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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/**
 * Produces a normalized copy of a {@link FLEFRecord} whose children are
 * recursively sorted, so that two records that carry the same
 * information but list their children in different orders compare equal
 * in a diff.
 * <p>
 * The sort key is {@code (tag, canonicalKey)}, where the canonical key
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


	private RecordNormalizer(){
	}


	/**
	 * Returns a normalized copy of the given record. The original is
	 * never modified.
	 *
	 * @param record the record to normalize; may be {@code null}
	 * @return a new record with children recursively sorted, or
	 *         {@code null} if the input was {@code null}
	 */
	public static FLEFRecord normalize(final FLEFRecord record){
		if(record == null)
			return null;

		final FLEFRecord copy = copyShell(record);
		final List<FLEFRecord> children = new ArrayList<>(record.getChildren());
		if(!ORDER_SIGNIFICANT_TAG.equals(record.getTag()))
			children.sort(Comparator
				.comparing((FLEFRecord r) -> r.getTag() != null? r.getTag(): StringUtils.EMPTY)
				.thenComparing(RecordNormalizer::canonicalKey));
		for(final FLEFRecord child : children)
			copy.addChild(normalize(child));
		return copy;
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
		sb.append(record.getTag() != null? record.getTag(): StringUtils.EMPTY);
		sb.append('=');
		sb.append(record.getValue() != null? record.getValue(): StringUtils.EMPTY);
		sb.append('#');
		sb.append(record.getId() != null? record.getId(): StringUtils.EMPTY);
		sb.append('[');

		final List<FLEFRecord> children = new ArrayList<>(record.getChildren());
		if(!ORDER_SIGNIFICANT_TAG.equals(record.getTag()))
			children.sort(Comparator
				.comparing((FLEFRecord r) -> r.getTag() != null? r.getTag(): StringUtils.EMPTY)
				.thenComparing(RecordNormalizer::canonicalKey));

		for(final FLEFRecord child : children){
			sb.append('{');
			sb.append(canonicalKey(child));
			sb.append('}');
		}
		sb.append(']');
		return sb.toString();
	}

}
