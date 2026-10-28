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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.AlternationType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FieldDefinition;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.ScalarType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.StructType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.TypeDefinition;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.UnionType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.tools.tools.merge.MergeGrammar;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/**
 * Produces a normalized copy of a {@link FLEFRecord} whose children are
 * recursively sorted, so that two records that carry the same
 * information but list their children in different orders compare equal
 * in a diff.
 * <p>
 * The children are ordered as the fields are declared in the grammar (see {@link #normalize(FLEFRecord, FLEFGrammar)}); the
 * children with the same tag, or all of them when the grammar does not know the record, are ordered by {@code (tag,
 * canonicalKey)}, where the canonical key
 * is a stable string built from the record's tag, value, id, and
 * recursively sorted children. Two records with the same content in
 * different child orders produce the same canonical key.
 * <p>
 * <b>Order-significant containers.</b> The {@code name} block of a
 * person ({@code PersonalNameStructure}) has order-significant
 * children: the sequence of {@code part} elements is what defines the
 * full name. The normalizer preserves the order of {@code name}'s
 * children, so a name written as {@code part given, part family} is not
 * considered equal to the same name written as {@code part family, part
 * given}. All other containers have their children sorted.
 */
public final class RecordNormalizer{

	/**
	 * Tags whose children must keep their original order. The protocol
	 * declares the sequence of {@code part} elements of a personal name
	 * as significant, so a {@code name} block is the one container that
	 * must not be sorted.
	 */
	private static final String ORDER_SIGNIFICANT_TAG = "name";

	private static final int MAX_RESOLVE_DEPTH = 8;


	private RecordNormalizer(){}


	/**
	 * Returns a normalized copy of the given record. The original is
	 * never modified.
	 *
	 * @param record the record to normalize; may be {@code null}
	 * @return a new record with children recursively sorted, or
	 *         {@code null} if the input was {@code null}
	 */
	public static FLEFRecord normalize(final FLEFRecord record){
		return normalize(record, MergeGrammar.instance());
	}

	/**
	 * Same as {@link #normalize(FLEFRecord)}, but the children are ordered as the fields are declared in the grammar (children
	 * whose field is unknown to the grammar come last, sorted by tag), instead of alphabetically.
	 *
	 * @param record	The record to normalize; may be {@code null}.
	 * @param grammar	The grammar defining the order of the fields; may be {@code null}, in which case the children are sorted by
	 * 	tag.
	 * @return	A new record with children recursively sorted, or {@code null} if the input was {@code null}.
	 */
	public static FLEFRecord normalize(final FLEFRecord record, final FLEFGrammar grammar){
		if(record == null)
			return null;

		return normalize(record, grammar, rootType(record, grammar));
	}

	private static FLEFRecord normalize(final FLEFRecord record, final FLEFGrammar grammar, final TypeDefinition type){
		final FLEFRecord copy = copyShell(record);
		final List<FLEFRecord> children = new ArrayList<>(record.getChildren());

		final boolean orderSignificant = ORDER_SIGNIFICANT_TAG.equals(record.getTag());
		final Comparator<FLEFRecord> byTag = Comparator.comparing(RecordNormalizer::tagOf);
		final Comparator<FLEFRecord> order;
		if(type instanceof StructType structType){
			final Comparator<FLEFRecord> byField = Comparator.comparingInt(child -> fieldIndex(structType, child.getTag()));
			order = (orderSignificant? byField: byField.thenComparing(byTag).thenComparing(RecordNormalizer::canonicalKey));
		}
		else
			order = (orderSignificant? null: byTag.thenComparing(RecordNormalizer::canonicalKey));
		if(order != null)
			children.sort(order);

		for(final FLEFRecord child : children)
			copy.addChild(normalize(child, grammar, childType(type, child, grammar)));
		return copy;
	}

	/** @return	The type defining the given top-level record (or header), or {@code null} if unknown. */
	private static TypeDefinition rootType(final FLEFRecord record, final FLEFGrammar grammar){
		if(grammar == null || grammar.getFileDefinition() == null)
			return null;

		final FieldDefinition recordsField = grammar.getFileDefinition()
			.recordsField();
		if(recordsField != null
				&& resolve(grammar.getType(recordsField.type().getName()), grammar) instanceof UnionType union
				&& union.getChoices().containsKey(record.getTag()))
			return resolve(union.getChoices().get(record.getTag()), grammar);

		final FieldDefinition headerField = grammar.getFileDefinition()
			.headerField();
		if(headerField != null && Strings.CI.equals(headerField.name(), record.getTag()))
			return resolve(grammar.getType(headerField.type().getName()), grammar);
		return null;
	}

	private static TypeDefinition childType(final TypeDefinition type, final FLEFRecord child, final FLEFGrammar grammar){
		if(grammar == null)
			return null;

		if(type instanceof StructType structType){
			final FieldDefinition field = structType.getField(child.getTag());
			return (field != null? resolve(field.type(), grammar): null);
		}
		if(type instanceof UnionType union)
			return resolve(union.getChoices().get(child.getTag()), grammar);
		return null;
	}

	/** Dereferences named types and picks the first structured alternative of an alternation. */
	private static TypeDefinition resolve(final TypeDefinition type, final FLEFGrammar grammar){
		TypeDefinition current = type;
		for(int depth = 0; depth < MAX_RESOLVE_DEPTH && current != null; depth ++){
			if(current instanceof ScalarType scalar && grammar.hasType(scalar.getName()))
				current = grammar.getType(scalar.getName());
			else if(current instanceof AlternationType alternation){
				TypeDefinition structured = null;
				for(final TypeDefinition alternative : alternation.getAlternatives()){
					final TypeDefinition resolved = resolve(alternative, grammar);
					if(resolved instanceof StructType || resolved instanceof UnionType){
						structured = resolved;
						break;
					}
				}
				return structured;
			}
			else
				break;
		}
		return current;
	}

	private static int fieldIndex(final StructType type, final String tag){
		final List<FieldDefinition> fields = type.getFields();
		for(int i = 0, size = fields.size(); i < size; i ++)
			if(Strings.CI.equals(fields.get(i).name(), tag))
				return i;
		return Integer.MAX_VALUE;
	}

	private static String tagOf(final FLEFRecord record){
		return (record.getTag() != null? record.getTag(): StringUtils.EMPTY);
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	/**
	 * Creates an empty record with the same tag, value, and id as the
	 * source, but with no children. The caller adds the sorted children.
	 */
	private static FLEFRecord copyShell(final FLEFRecord record){
		final FLEFRecord copy;
		if(record.getValue() != null)
			copy = FLEFRecord.createChildWithTagAndValue(record.getTag(), record.getValue());
		else
			copy = FLEFRecord.createChildWithTag(record.getTag());
		if(record.getId() != null)
			copy.setId(record.getId());
		return copy;
	}

	/**
	 * Builds a canonical, order-independent key for the given record.
	 * The key is used as a sort key, not as a display string: its only
	 * requirement is that two records with the same content in different
	 * child orders produce the same key.
	 */
	private static String canonicalKey(final FLEFRecord record){
		final StringBuilder sb = new StringBuilder();
		sb.append(record.getTag() != null? record.getTag(): StringUtils.EMPTY)
			.append('=')
			.append(record.getValue() != null? record.getValue(): StringUtils.EMPTY)
			.append('#')
			.append(record.getId() != null? record.getId(): StringUtils.EMPTY)
			.append('[');

		final List<FLEFRecord> children = new ArrayList<>(record.getChildren());
		if(!ORDER_SIGNIFICANT_TAG.equals(record.getTag()))
			children.sort(Comparator
				.comparing((FLEFRecord r) -> r.getTag() != null? r.getTag(): StringUtils.EMPTY)
				.thenComparing(RecordNormalizer::canonicalKey));

		for(final FLEFRecord child : children)
			sb.append('{')
				.append(canonicalKey(child))
				.append('}');
		sb.append(']');
		return sb.toString();
	}

}
