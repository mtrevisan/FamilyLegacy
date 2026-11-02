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
package io.github.mtrevisan.familylegacy.io.model.record;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Describes what changed between two versions of a record: what was added (<code>+</code>), changed (<code>~</code>) and removed
 * (<code>-</code>), one line for each leaf field.
 * <p>
 * The {@code audit} structure of the record is not considered, as it is about the record itself.
 * </p>
 */
public final class RecordChangeSummary{

	private static final String TAG_AUDIT = "audit";
	private static final String ADDED = "+ ";
	private static final String CHANGED = "~ ";
	private static final String REMOVED = "- ";
	private static final String ARROW = " → ";
	private static final String ELLIPSIS = "…";

	private static final int MAX_LINES = 20;
	private static final int MAX_VALUE_LENGTH = 60;


	private RecordChangeSummary(){}


	/**
	 * Describes the changes.
	 *
	 * @param before	The record as it was.
	 * @param after	The record as it is.
	 * @return	One line for each change, at most {@value #MAX_LINES} (a last line tells how many more there are); empty if nothing
	 * 	changed.
	 */
	public static List<String> describe(final FLEFRecord before, final FLEFRecord after){
		final List<String> changes = new ArrayList<>();
		compare(StringUtils.EMPTY, before, after, changes);

		if(changes.size() <= MAX_LINES)
			return changes;

		final List<String> limited = new ArrayList<>(changes.subList(0, MAX_LINES));
		limited.add(ELLIPSIS + " +" + (changes.size() - MAX_LINES));
		return limited;
	}

	private static void compare(final String prefix, final FLEFRecord before, final FLEFRecord after, final List<String> changes){
		final Map<String, List<FLEFRecord>> beforeByTag = groupByTag(before, prefix.isEmpty());
		final Map<String, List<FLEFRecord>> afterByTag = groupByTag(after, prefix.isEmpty());

		final Set<String> tags = new LinkedHashSet<>(beforeByTag.keySet());
		tags.addAll(afterByTag.keySet());
		for(final String tag : tags){
			final List<FLEFRecord> oldChildren = beforeByTag.getOrDefault(tag, List.of());
			final List<FLEFRecord> newChildren = afterByTag.getOrDefault(tag, List.of());

			final boolean repeated = (Math.max(oldChildren.size(), newChildren.size()) > 1);
			for(int i = 0, count = Math.max(oldChildren.size(), newChildren.size()); i < count; i ++){
				final String path = prefix + tag + (repeated? "[" + i + "]": StringUtils.EMPTY);
				final FLEFRecord oldChild = (i < oldChildren.size()? oldChildren.get(i): null);
				final FLEFRecord newChild = (i < newChildren.size()? newChildren.get(i): null);

				if(oldChild == null)
					changes.add(ADDED + path + describeValue(newChild));
				else if(newChild == null)
					changes.add(REMOVED + path + describeValue(oldChild));
				else{
					if(!StringUtils.equals(oldChild.getValue(), newChild.getValue()))
						changes.add(CHANGED + path + ": " + abbreviate(oldChild.getValue()) + ARROW + abbreviate(newChild.getValue()));

					compare(path + ".", oldChild, newChild, changes);
				}
			}
		}
	}

	private static Map<String, List<FLEFRecord>> groupByTag(final FLEFRecord record, final boolean topLevel){
		final Map<String, List<FLEFRecord>> byTag = new LinkedHashMap<>();
		if(record == null)
			return byTag;

		for(final FLEFRecord child : record.getChildren())
			if(!topLevel || !TAG_AUDIT.equals(child.getTag()))
				byTag.computeIfAbsent(child.getTag(), k -> new ArrayList<>())
					.add(child);
		return byTag;
	}

	private static String describeValue(final FLEFRecord record){
		return (StringUtils.isNotBlank(record.getValue())? ": " + abbreviate(record.getValue()): StringUtils.EMPTY);
	}

	private static String abbreviate(final String value){
		return (value != null
			? StringUtils.abbreviate(StringUtils.normalizeSpace(value), ELLIPSIS, MAX_VALUE_LENGTH)
			: StringUtils.EMPTY);
	}

}
