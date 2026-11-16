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
import io.github.mtrevisan.familylegacy.io.model.readers.AuditReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


/**
 * Canonical text of the assertion carried by a structure, used to recognize two structures that state the same thing.
 * <p>
 * Fields holding evidence and bookkeeping (citations, notes, audit, privacy, confidence) are left out at every depth: two
 * structures that differ only there state the same assertion, and merging them unites their evidence. Fields are compared
 * independently of their order, while the occurrences of a repeated field keep their order (e.g. the parts of a name).
 */
final class RecordSignature{

	private static final String TAG_AUDIT = AuditReader.TAG_AUDIT;


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

		// group by tag, keeping the order of the occurrences of the same tag: a stable sort by tag, on the children that count
		final List<FLEFRecord> children = node.getChildren();
		int counted = 0;
		for(int i = 0, size = children.size(); i < size; i ++){
			final String tag = children.get(i).getTag();
			if(tag != null && !TAG_AUDIT.equals(tag))
				counted ++;
		}
		if(counted == 0)
			return;

		final FLEFRecord[] ordered = new FLEFRecord[counted];
		int written = 0;
		for(int i = 0, size = children.size(); i < size; i ++){
			final FLEFRecord child = children.get(i);
			if(child.getTag() != null && !TAG_AUDIT.equals(child.getTag()))
				ordered[written ++] = child;
		}
		if(counted > 1)
			Arrays.sort(ordered, BY_TAG);

		//the evidence is left out, and a structure that has nothing else states what one with no children does
		final int mark = sb.length();
		sb.append('{');
		boolean any = false;
		String tag = null;
		TypeDefinition childType = null;
		for(final FLEFRecord child : ordered){
			if(!child.getTag().equals(tag)){
				tag = child.getTag();
				childType = childType(type, tag);
			}
			if(childType == EVIDENCE)
				continue;

			sb.append(tag);
			append(sb, child, childType);
			sb.append(';');
			any = true;
		}
		if(any)
			sb.append('}');
		else
			sb.setLength(mark);
	}

	private static final Comparator<FLEFRecord> BY_TAG = Comparator.comparing(FLEFRecord::getTag);

	/** Marker for a field that must be left out of the signature. */
	private static final TypeDefinition EVIDENCE = new UnionType(EvidenceQualifiersReader.TAG_EVIDENCE, Map.of());

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
