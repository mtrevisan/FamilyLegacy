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

import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.Cardinality;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FieldDefinition;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.ScalarType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.StructType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.TypeDefinition;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.UnionType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import java.util.ArrayList;
import java.util.List;


/**
 * Merges the blocks of the same singular field, as the instance syntax of the protocol requires: {@code a.b.c v} is the same as
 * {@code a { b { c v } } }, and blocks for the same singular field are merged, so that
 * <pre>
 *   audit.creation.date 2026-10-16T09:30:00+02:00
 *   audit.update {
 *     date 2026-10-17T09:30:00+02:00
 *   }
 * </pre>
 * gives a single {@code audit} holding both {@code creation} and {@code update}.
 * <p>
 * Telling a singular field from a repeated one takes the grammar, which the parser of instances does not have: like
 * {@link XrefPathIndex}, this index is initialized when the grammar is parsed. Until then {@link #mergeSingularBlocks} does
 * nothing. A singular leaf assigned twice is an error of the file and is left as it is, for the validator to report.
 */
public final class SingularFieldIndex{

	private static FLEFGrammar grammar;


	private SingularFieldIndex(){}


	public static synchronized void initialize(final FLEFGrammar grammar){
		if(grammar != null && SingularFieldIndex.grammar == null)
			SingularFieldIndex.grammar = grammar;
	}

	public static boolean isInitialized(){
		return (grammar != null);
	}

	/**
	 * Merges the blocks of the same singular field in a top-level record and in all its structures.
	 *
	 * @param record the record (or the header) as parsed
	 */
	public static void mergeSingularBlocks(final FLEFRecord record){
		final FLEFGrammar current = grammar;
		if(current == null || record == null)
			return;

		final TypeDefinition type = (record.getId() == null && isHeader(current, record)
			? current.getFileDefinition().headerField().type()
			: recordType(current, record.getTag()));
		if(resolve(current, type) instanceof StructType struct)
			mergeStruct(current, record, struct);
	}

	private static boolean isHeader(final FLEFGrammar grammar, final FLEFRecord record){
		return (grammar.getFileDefinition() != null && grammar.getFileDefinition().headerField() != null
			&& grammar.getFileDefinition().headerField().name().equals(record.getTag()));
	}

	private static TypeDefinition recordType(final FLEFGrammar grammar, final String kind){
		return (grammar.getType(XrefPathIndex.RECORD_UNION_NAME) instanceof UnionType records
			? records.getChoices().get(kind)
			: null);
	}

	private static void mergeStruct(final FLEFGrammar grammar, final FLEFRecord node, final StructType struct){
		for(final FieldDefinition field : struct.getFields()){
			final TypeDefinition fieldType = resolve(grammar, field.type());
			final boolean composite = (fieldType instanceof StructType || fieldType instanceof UnionType);
			final List<FLEFRecord> occurrences = children(node, field.name());
			if(composite && !isRepeated(field) && occurrences.size() > 1)
				mergeOccurrences(node, occurrences);

			for(final FLEFRecord occurrence : children(node, field.name()))
				if(fieldType instanceof StructType fieldStruct)
					mergeStruct(grammar, occurrence, fieldStruct);
				else if(fieldType instanceof UnionType union)
					for(final FLEFRecord arm : occurrence.getChildren())
						if(resolve(grammar, union.getChoices().get(arm.getTag())) instanceof StructType armStruct)
							mergeStruct(grammar, arm, armStruct);
		}
	}

	/**
	 * Moves the content of every occurrence without value into the first one; occurrences with a value are an error of the file and
	 * are kept.
	 */
	private static void mergeOccurrences(final FLEFRecord node, final List<FLEFRecord> occurrences){
		final FLEFRecord first = occurrences.getFirst();
		if(first.getValue() != null)
			return;

		for(int i = 1, size = occurrences.size(); i < size; i ++){
			final FLEFRecord other = occurrences.get(i);
			if(other.getValue() != null)
				continue;

			first.getChildren()
				.addAll(other.getChildren());
			node.getChildren()
				.removeIf(child -> child == other);
		}
	}

	private static boolean isRepeated(final FieldDefinition field){
		return (field.cardinality() == Cardinality.ZERO_OR_MORE || field.cardinality() == Cardinality.ONE_OR_MORE);
	}

	private static List<FLEFRecord> children(final FLEFRecord node, final String tag){
		final List<FLEFRecord> result = new ArrayList<>(0);
		for(final FLEFRecord child : node.getChildren())
			if(tag.equals(child.getTag()))
				result.add(child);
		return result;
	}

	private static TypeDefinition resolve(final FLEFGrammar grammar, TypeDefinition type){
		while(type instanceof ScalarType scalar && grammar.hasType(scalar.getName())){
			final TypeDefinition next = grammar.getType(scalar.getName());
			if(next == type)
				break;

			type = next;
		}
		return type;
	}

}
