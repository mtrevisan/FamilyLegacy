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
package io.github.mtrevisan.familylegacy.io.merger;

import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FieldDefinition;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.StructType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.TypeDefinition;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.UnionType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * Canonical text of the assertion carried by a structure, used to recognize two structures that state the same thing.
 * <p>
 * Fields holding evidence and bookkeeping (citations, notes, audit, privacy, confidence) are left out at every depth: two
 * structures that differ only there state the same assertion, and merging them unites their evidence. Fields are compared
 * independently of their order, while the occurrences of a repeated field keep their order (e.g. the parts of a name).
 */
final class RecordSignature{

	private static final String TAG_AUDIT = "audit";


	private final GrammarSupport grammar;


	RecordSignature(final GrammarSupport grammar){
		this.grammar = grammar;
	}


	/**
	 * Signature of a top-level record, its id excluded.
	 */
	String ofRecord(final FLEFRecord record){
		return of(record, grammar.recordType(record.getTag()));
	}

	/**
	 * Signature of a structure of the given type ({@code null} when the type is unknown or is a leaf).
	 */
	String of(final FLEFRecord node, final TypeDefinition type){
		final StringBuilder sb = new StringBuilder();
		append(sb, node, grammar.resolve(type));
		return sb.toString();
	}

	private void append(final StringBuilder sb, final FLEFRecord node, final TypeDefinition type){
		final String value = (node.getValue() != null? node.getValue().strip(): null);
		if(value != null)
			sb.append('=')
				.append(value.length())
				.append(':')
				.append(value);

		// group by tag, keeping the order of the occurrences of the same tag
		final Map<String, List<FLEFRecord>> byTag = new TreeMap<>();
		for(final FLEFRecord child : node.getChildren())
			if(child.getTag() != null && !TAG_AUDIT.equals(child.getTag()))
				byTag.computeIfAbsent(child.getTag(), k -> new ArrayList<>())
					.add(child);
		if(byTag.isEmpty())
			return;

		sb.append('{');
		for(final Map.Entry<String, List<FLEFRecord>> entry : byTag.entrySet()){
			final String tag = entry.getKey();
			final TypeDefinition childType = childType(type, tag);
			if(childType == EVIDENCE)
				continue;

			for(final FLEFRecord child : entry.getValue()){
				sb.append(tag);
				append(sb, child, childType);
				sb.append(';');
			}
		}
		sb.append('}');
	}

	/** Marker for a field that must be left out of the signature. */
	private static final TypeDefinition EVIDENCE = new UnionType("evidence", Map.of());

	private TypeDefinition childType(final TypeDefinition parentType, final String tag){
		if(parentType instanceof StructType struct){
			final FieldDefinition field = struct.getField(tag);
			if(field == null)
				// unknown field: compared verbatim
				return null;
			if(grammar.isEvidenceField(field))
				return EVIDENCE;

			return grammar.resolve(field.type());
		}
		if(parentType instanceof UnionType union)
			return grammar.resolve(union.getChoices().get(tag));
		return null;
	}

}
