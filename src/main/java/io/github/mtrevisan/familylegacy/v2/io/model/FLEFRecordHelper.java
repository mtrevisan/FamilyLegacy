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
package io.github.mtrevisan.familylegacy.v2.io.model;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * Utility methods for working with FLEFRecord objects.
 * Provides common operations for finding, adding, and updating child records.
 */
public final class FLEFRecordHelper{

	private static final Pattern PATH_SEGMENT = Pattern.compile("([^.\\[]+)(?:\\[(\\d+)\\])?");


	private record Segment(String tag, int index){
		static Segment parse(final String segment){
			final Matcher matcher = PATH_SEGMENT.matcher(segment);
			if(!matcher.matches())
				return null;

			final String tag = matcher.group(1);
			final int index = (matcher.group(2) == null? 0: Integer.parseInt(matcher.group(2)));
			return new Segment(tag, index);
		}
	}


	private FLEFRecordHelper(){}


	/**
	 * Finds a child navigating through a path of tags separated by '.'.
	 *
	 * @param parent	The starting record.
	 * @param path	The dot‑separated tag path (e.g. "ROOT.RESTRICTION[2].CODE").
	 * @return	The matching record, or {@code null} if any tag in the path is not found.
	 */
	public static FLEFRecord findChild(final FLEFRecord parent, final String path){
		if(parent == null || StringUtils.isEmpty(path))
			return parent;

		FLEFRecord current = parent;
		for(final String segment : path.split("\\.")){
			if(current == null)
				break;

			final Segment seg = Segment.parse(segment);
			if(seg == null)
				return null;

			current = getNthChild(current, seg);
		}
		return current;
	}

	/**
	 * Finds all children with the given tag.
	 *
	 * @param parent	The parent record.
	 * @param path	The dot‑separated tag path to search for (e.g. "ROOT.RESTRICTION[2].CODE").
	 * @return	A list of matching child records.
	 */
	public static List<FLEFRecord> findChildren(final FLEFRecord parent, final String path){
		final List<FLEFRecord> result = new ArrayList<>();
		if(parent == null|| StringUtils.isEmpty(path))
			return result;

		final String[] segments = path.split("\\.");
		final FLEFRecord targetParent = navigateToParent(parent, segments);
		if(targetParent == null)
			return result;

		final Segment seg = Segment.parse(segments[segments.length - 1]);
		if(seg == null)
			return result;

		for(final FLEFRecord child : targetParent.getChildren())
			if(seg.tag.equals(child.getTag()))
				result.add(child);
		return result;
	}

	/**
	 * Retrieves the value ({@code String}) of the node identified by the path.
	 * If the last node has a value, it is returned; otherwise {@code null}.
	 *
	 * @param parent	The parent record.
	 * @param path	The dot‑separated tag path (e.g. "ROOT.RESTRICTION[2].CODE").
	 * @return	The value as a {@code String}, or {@code null} if not found or no value.
	 */
	public static String getChildValue(final FLEFRecord parent, final String path){
		final FLEFRecord child = findChild(parent, path);
		return (child != null? child.getValue(): null);
	}

	/**
	 * Collects values of all children with the given tag as a comma-separated string.
	 *
	 * @param parent	The parent record.
	 * @param path	The dot‑separated tag path to search for (e.g. "ROOT.RESTRICTION[2].CODE").
	 * @return	A comma-separated string of values, or empty string if none found.
	 */
	public static String getChildValuesAsString(final FLEFRecord parent, final String path){
		final List<FLEFRecord> children = findChildren(parent, path);
		if(children.isEmpty())
			return StringUtils.EMPTY;

		return children.stream()
			.map(FLEFRecord::getValue)
			.filter(StringUtils::isNotEmpty)
			.collect(Collectors.joining(","));
	}

	/**
	 * Updates or creates a child with the given tag and value.
	 * If the value is {@code null} or empty, the child is removed.
	 *
	 * @param parent	The parent record.
	 * @param path	The dot‑separated tag path to update (e.g. "ROOT.RESTRICTION[2].CODE").
	 * @param value	The new value ({@code null} or empty to remove).
	 */
	public static void updateChildValue(final FLEFRecord parent, final String path, final String value){
		if(parent == null)
			return;

		if(StringUtils.isEmpty(value)){
			removeChild(parent, path);

			return;
		}

		final FLEFRecord existing = findChild(parent, path);
		if(existing != null)
			existing.setValue(value);
		else
			addChild(parent, path, value);
	}

	/**
	 * Adds a single child with the given tag and value, if value is not empty.
	 *
	 * @param parent	The parent record.
	 * @param path	The dot‑separated tag path for the new child (e.g. "ROOT.RESTRICTION[2].CODE").
	 * @param value	The value to set (ignored if {@code null} or empty).
	 */
	public static void addChild(final FLEFRecord parent, final String path, final String value){
		if(parent == null || StringUtils.isEmpty(path) || StringUtils.isEmpty(value))
			return;

		final String[] segments = path.split("\\.");
		final FLEFRecord targetParent = navigateToParentAndCreate(parent, segments);
		if(targetParent == null)
			return;

		final Segment seg = Segment.parse(segments[segments.length - 1]);
		if(seg == null)
			return;

		FLEFRecord target = getNthChild(targetParent, seg);
		// Create missing occurrences up to the requested index
		if(target == null){
			final long existingCount = targetParent.getChildren().stream()
				.filter(c -> seg.tag().equals(c.getTag()))
				.count();

			for(long i = existingCount; i <= seg.index(); i ++){
				target = FLEFRecord.createChild(seg.tag());
				targetParent.addChild(target);
			}
		}

		target.setValue(value);
	}

	/**
	 * Remove a child navigating through a path of tags separated by '.'.
	 *
	 * @param parent The starting record.
	 * @param path	The dot‑separated tag path (e.g. "ROOT.RESTRICTION[2].CODE").
	 * @return The deleted child, or {@code null} if any tag in the path is not found.
	 */
	public static FLEFRecord removeChild(final FLEFRecord parent, final String path){
		if(parent == null || StringUtils.isEmpty(path))
			return null;

		final String[] segments = path.split("\\.");
		final FLEFRecord targetParent = navigateToParent(parent, segments);
		if(targetParent == null)
			return null;

		final Segment seg = Segment.parse(segments[segments.length - 1]);
		if(seg == null)
			return null;

		final FLEFRecord target = getNthChild(targetParent, seg);
		if(target != null){
			targetParent.getChildren()
				.remove(target);

			return target;
		}
		return null;
	}

	public static boolean removeChildren(final FLEFRecord parent, final String... paths){
		boolean result = true;
		for(final String path : paths)
			result &= removeChildren(parent, path);
		return result;
	}

	/**
	 * Removes all children with the given tag.
	 *
	 * @param parent	The parent record.
	 * @param path	The dot‑separated tag path to remove (e.g. "ROOT.RESTRICTION[2].CODE").
	 */
	public static boolean removeChildren(final FLEFRecord parent, final String path){
		if(parent == null || StringUtils.isEmpty(path))
			return false;

		final String[] segments = path.split("\\.");
		final Matcher matcher = PATH_SEGMENT.matcher(segments[segments.length - 1]);
		if(!matcher.matches())
			return false;

		if(matcher.group(2) != null){
			removeChild(parent, path);
			return true;
		}

		final FLEFRecord targetParent = navigateToParent(parent, segments);
		if(targetParent != null){
			final String tag = matcher.group(1);
			targetParent.getChildren().removeIf(child -> tag.equals(child.getTag()));
		}
		return true;
	}

	/**
	 * Removes all children.
	 *
	 * @param parent	The parent record.
	 */
	public static void removeAllChildren(final FLEFRecord parent){
		if(parent != null && parent.getChildren() != null)
			parent.getChildren()
				.clear();
	}

	private static FLEFRecord getNthChild(FLEFRecord current, final Segment segment){
		if(segment.index == 0)
			// find the first child with that tag
			current = current.findChild(segment.tag);
		else{
			int occurrence = 0;
			FLEFRecord found = null;
			for(final FLEFRecord child : current.getChildren())
				if(segment.tag.equals(child.getTag())){
					if(occurrence == segment.index){
						found = child;

						break;
					}
					occurrence ++;
				}
			current = found;
		}
		return current;
	}

	private static FLEFRecord getNthChildOrCreate(FLEFRecord current, final Segment segment){
		int occurrence = 0;
		FLEFRecord found = null;
		for(final FLEFRecord child : current.getChildren()){
			if(segment.tag.equals(child.getTag())){
				if(occurrence == segment.index){
					found = child;

					break;
				}

				occurrence ++;
			}
		}

		if(found == null){
			for(int i = occurrence; i <= segment.index; i ++){
				found = FLEFRecord.createChild(segment.tag);
				current.addChild(found);
			}
		}

		return found;
	}

	private static FLEFRecord navigateToParent(final FLEFRecord root, final String[] segments){
		FLEFRecord current = root;
		for(int i = 0; i < segments.length - 1; i ++){
			if(current == null)
				return null;

			final Segment seg = Segment.parse(segments[i]);
			if(seg == null)
				return null;

			current = getNthChild(current, seg);
		}
		return current;
	}

	private static FLEFRecord navigateToParentAndCreate(final FLEFRecord parent, final String[] segments){
		FLEFRecord current = parent;
		for(int i = 0; i < segments.length - 1; i ++){
			if(current == null)
				return null;

			final Segment seg = Segment.parse(segments[i]);
			if(seg == null)
				return null;

			current = getNthChildOrCreate(current, seg);
		}
		return current;
	}

}
