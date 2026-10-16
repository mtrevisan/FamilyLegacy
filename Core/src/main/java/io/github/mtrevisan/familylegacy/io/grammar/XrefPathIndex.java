/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.io.grammar;

import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.AlternationType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.Cardinality;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FieldDefinition;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.ReferenceType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.ScalarType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.StructType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.TypeDefinition;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.UnionType;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public final class XrefPathIndex{

	public static final String RECORD_UNION_NAME = "Record";
	private static final String STAR_INDEX = "[*]";

	// Maps the target record type (e.g., "IndividualRecord") to a set of full paths leading to it
	private static final Map<String, Set<String>> XREF_PATHS = new HashMap<>();


	private static boolean initialized;


	private XrefPathIndex(){}


	public static boolean isInitialized(){
		return initialized;
	}

	/**
	 * Extracts and caches all Xref paths starting exclusively from the main top-level records
	 * defined within the "Record" union type of the grammar.
	 */
	public static synchronized void initialize(final FLEFGrammar grammar){
		if(grammar == null || initialized)
			return;

		// Locate the "Record" union definition
		final TypeDefinition recordTypeDef = grammar.getType(RECORD_UNION_NAME);
		if(recordTypeDef instanceof UnionType recordUnion){
			// Iterate over each alternative/choice inside the Record union (e.g., individual, place, event, etc.)
			for(final Map.Entry<String, TypeDefinition> choice : recordUnion.getChoices().entrySet()){
				final String recordWrapperName = choice.getKey();
				TypeDefinition choiceType = choice.getValue();

				// Resolve scalar type aliases if any
				while(choiceType instanceof ScalarType scalar && grammar.hasType(scalar.getName()))
					choiceType = grammar.getType(scalar.getName());

				// Collect paths starting from this record type wrapper
				collectPaths(choiceType, grammar, recordWrapperName);
			}
		}

		initialized = true;
	}

	/**
	 * Returns all full Xref paths that point to the specified target record type
	 * (e.g., "IndividualRecord").
	 *
	 * @param targetRecordType the target record type name (e.g. "IndividualRecord")
	 * @return a set of matching full Xref paths
	 */
	public static Set<String> getPaths(final String targetRecordType){
		if(StringUtils.isEmpty(targetRecordType))
			return Collections.emptySet();

		return XREF_PATHS.getOrDefault(targetRecordType, Collections.emptySet());
	}

	private static void collectPaths(TypeDefinition typeDef, final FLEFGrammar grammar, final String currentPath){
		if(typeDef == null)
			return;

		// Resolve scalar type aliases
		while(typeDef instanceof ScalarType scalar && grammar.hasType(scalar.getName()))
			typeDef = grammar.getType(scalar.getName());

		// If the terminal type is a ReferenceType, index the path by its target record type
		if(typeDef instanceof ReferenceType refType){
			if(!currentPath.isEmpty()){
				final String targetTypeName = refType.getTargetTypeName();
				XREF_PATHS.computeIfAbsent(targetTypeName, k -> new HashSet<>())
					.add(currentPath);
			}
		}
		else if(typeDef instanceof StructType structType){
			for(final FieldDefinition fieldDef : structType.getFields()){
				final String fieldName = fieldDef.name();
				// Check if the field cardinality allows multiple items (* or +)
				final boolean isMultiple = (fieldDef.cardinality() == Cardinality.ZERO_OR_MORE
					|| fieldDef.cardinality() == Cardinality.ONE_OR_MORE);
				final String segment = fieldName + (isMultiple? STAR_INDEX: StringUtils.EMPTY);

				final String nextPath = (currentPath.isEmpty()
					? segment
					: FLEFRecordHelper.composePath(currentPath, segment));
				collectPaths(fieldDef.type(), grammar, nextPath);
			}
		}
		else if(typeDef instanceof UnionType unionType){
			for(final Map.Entry<String, TypeDefinition> choice : unionType.getChoices().entrySet()){
				final String choiceName = choice.getKey();
				final String nextPath = (currentPath.isEmpty()
					? choiceName
					: FLEFRecordHelper.composePath(currentPath, choiceName));
				collectPaths(choice.getValue(), grammar, nextPath);
			}
		}
		else if(typeDef instanceof AlternationType altType){
			for(final TypeDefinition alt : altType.getAlternatives())
				collectPaths(alt, grammar, currentPath);
		}
	}

}
