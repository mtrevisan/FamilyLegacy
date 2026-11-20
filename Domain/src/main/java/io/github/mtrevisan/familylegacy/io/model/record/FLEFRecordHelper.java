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
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Utility methods for working with FLEFRecord objects.
 * Provides common operations for finding, adding, and updating child records.
 */
public final class FLEFRecordHelper{

	private static final Logger LOGGER = LoggerFactory.getLogger(FLEFRecordHelper.class);


	private static final String DOT = ".";


	private record Segment(String tag, int index, boolean wildcard){

		private static final String ANY = "*";

		static Segment parse(final String segment){
			final int startArrayIndex = segment.indexOf('[');
			if(startArrayIndex < 0)
				return new Segment(segment, -1, false);

			final String tag = segment.substring(0, startArrayIndex)
				.toLowerCase(Locale.ROOT);
			final int endArrayIndex = segment.indexOf(']', startArrayIndex);
			if(endArrayIndex < 0)
				return null;
			final String indexStr = segment.substring(startArrayIndex + 1, endArrayIndex)
				.trim();
			if(ANY.equals(indexStr))
				return new Segment(tag, -1, true);

			final int index;
			try{
				index = Integer.parseInt(indexStr);
			}
			catch(final NumberFormatException ignored){
				return null;
			}
			if(index < 0)
				return null;

			return new Segment(tag, index, false);
		}
	}


	private FLEFRecordHelper(){}


	public static String composePath(final String... segment){
		return String.join(DOT, segment);
	}


	/**
	 * Finds a child navigating through a path of tags separated by '.'.
	 *
	 * @param parent	The starting record.
	 * @param path	The dot‑separated tag path (e.g. "ROOT.RESTRICTION[2].CODE").
	 * @return	The matching record, or {@code null} if any tag in the path is not found.
	 */
	public static FLEFRecord findChild(final FLEFRecord parent, final String path){
		if(parent == null)
			return null;
		if(StringUtils.isEmpty(path)){
			final List<FLEFRecord> children = parent.getChildren();
			if(children.size() > 1)
				throw new IllegalArgumentException("Ambiguous reference: " + path);

			return (!children.isEmpty()? children.getFirst(): null);
		}

		//no list is built: the path is followed in place, stopping as soon as a second record shows that the reference is ambiguous
		final Matches matches = new Matches(null, true);
		walk(parent, path, 0, matches);
		if(matches.count > 1)
			throw new IllegalArgumentException("Ambiguous reference: " + path);

		return matches.first;
	}

	/**
	 * Counts the children that {@link #findChildren(FLEFRecord, String)} would find, without building the list.
	 *
	 * @param parent	The parent record.
	 * @param path	The dot-separated tag path.
	 * @return	The number of matching records.
	 */
	public static int countChildren(final FLEFRecord parent, final String path){
		if(parent == null)
			return 0;
		if(StringUtils.isEmpty(path))
			return parent.getChildren().size();

		final Matches matches = new Matches(null, false);
		walk(parent, path, 0, matches);
		return matches.count;
	}

	/**
	 * Finds all children with the given tag.
	 *
	 * @param parent	The parent record.
	 * @param paths	The array of dot‑separated tag paths to search for (e.g. "[GROUP, ROOT.RESTRICTION[2].CODE]").
	 * @return	A (unordered) list of matching child records.
	 */
	public static List<FLEFRecord> findChildren(final FLEFRecord parent, final String... paths){
		final List<FLEFRecord> result = new ArrayList<>();
		if(parent == null || paths == null)
			return result;

		for(int i = 0, size = paths.length; i < size; i ++){
			final String path = paths[i];

			result.addAll(findChildren(parent, path));
		}
		return result;
	}

	/**
	 * Finds all children with the given tag path, supporting multiple intermediate segments and wildcards ([*]).
	 * Optimized to avoid unnecessary list allocations at each loop iteration.
	 *
	 * @param parent The parent record.
	 * @param path   The dot-separated tag path to search for (e.g. "name[*].part.variant[*]").
	 * @return A list of matching child records.
	 */
	public static List<FLEFRecord> findChildren(final FLEFRecord parent, final String path){
		if(parent == null)
			return Collections.emptyList();
		if(StringUtils.isEmpty(path))
			return parent.getChildren();

		final Matches matches = new Matches(new ArrayList<>(2), false);
		walk(parent, path, 0, matches);
		return (matches.count > 0? matches.all: Collections.emptyList());
	}

	/** The records found by {@link #walk(FLEFRecord, String, int, Matches)}. */
	private static final class Matches{

		private FLEFRecord first;
		private int count;
		/** All the records found, or {@code null} if only the first one and their number are needed. */
		private final List<FLEFRecord> all;
		/** Whether the walk stops as soon as a second record is found (the caller only needs to know that they are many). */
		private final boolean stopAtSecond;


		private Matches(final List<FLEFRecord> all, final boolean stopAtSecond){
			this.all = all;
			this.stopAtSecond = stopAtSecond;
		}

		private void add(final FLEFRecord record){
			if(count == 0)
				first = record;
			count ++;
			if(all != null)
				all.add(record);
		}

		private boolean isDone(){
			return (stopAtSecond && count > 1);
		}

	}

/**
 * Whether the tag is equal, ignoring the case, to the {@code length} characters of the path from {@code start} (the tag has the same
 * length). The tags are lowercase (the parser lowercases them) and so are almost all the paths, so the characters are compared as they
 * are, with the case folded by hand for the ASCII letters, and the slow comparison is left to what is not ASCII.
 */
private static boolean tagMatches(final String tag, final String path, final int start, final int length){
	for(int i = 0; i < length; i ++){
		final char a = tag.charAt(i);
		final char b = path.charAt(start + i);
		if(a == b)
			continue;
		if(a > 127 || b > 127)
			return tag.regionMatches(true, i, path, start + i, length - i);
		if(((a | 0x20) != (b | 0x20)) || !((a | 0x20) >= 'a' && (a | 0x20) <= 'z'))
			return false;
	}
	return true;
}

/** Case-insensitive equality of two tags ({@code null} is equal to nothing). */
	private static boolean tagEquals(final String tag, final String other){
		return (tag != null && tag.equalsIgnoreCase(other));
	}

	/**
	 * Follows a path of tags from a node, without splitting the path nor allocating the intermediate lists: the segments are read
	 * in place. The records the path leads to (the order is the same of a level-by-level visit) are given to {@code matches}.
	 * An invalid segment ("tag[", "tag[x]", "tag[-1]") matches nothing, and the empty segments ("a..b") are ignored, like in a split.
	 */
	private static void walk(final FLEFRecord node, final String path, final int from, final Matches matches){
		final int length = path.length();
		int start = from;
		while(start < length && path.charAt(start) == '.')
			start ++;
		if(start == length){
			matches.add(node);

			return;
		}

		int end = path.indexOf('.', start);
		if(end < 0)
			end = length;

		int tagEnd = end;
		int index = -1;
		boolean wildcard = false;
		final int bracket = path.indexOf('[', start);
		if(bracket >= 0 && bracket < end){
			final int close = path.indexOf(']', bracket);
			if(close < 0 || close >= end)
				return;

			tagEnd = bracket;
			final String indexText = path.substring(bracket + 1, close)
				.trim();
			if("*".equals(indexText))
				wildcard = true;
			else{
				try{
					index = Integer.parseInt(indexText);
				}
				catch(final NumberFormatException ignored){
					return;
				}
				if(index < 0)
					return;
			}
		}

		final int tagLength = tagEnd - start;
		final List<FLEFRecord> children = node.getChildren();
		int occurrence = 0;
		for(int i = 0, size = children.size(); i < size; i ++){
			final FLEFRecord child = children.get(i);
			final String childTag = child.getTag();
			if(childTag == null || childTag.length() != tagLength || !tagMatches(childTag, path, start, tagLength))
				continue;

			if(index >= 0 && !wildcard){
				//only the occurrence asked for (e.g. RESTRICTION[2])
				if(occurrence ++ == index){
					walk(child, path, end, matches);

					return;
				}
			}
			else{
				walk(child, path, end, matches);
				if(matches.isDone())
					return;
			}
		}
	}




	/*
	extract (xref):
		- `event: Xref<EventRecord>` | ParticipantField
	*/
	public static FLEFRecord extractRecordFromReference(final FLEFRecord record, final String referencePath,
			final RecordLookup model){
		final List<FLEFRecord> records = extractRecordsFromReference(record, referencePath, model);
		return (records.size() == 1? records.getFirst(): null);
	}

	/*
	extract (xref):
		- `note*: Xref<NoteRecord>` | EntityListPanel
	*/
	public static List<FLEFRecord> extractRecordsFromReference(final FLEFRecord record, final String referencePath,
			final RecordLookup model){
		final List<FLEFRecord> references = FLEFRecordHelper.findChildren(record, referencePath);
		final List<FLEFRecord> referencedRecords = new ArrayList<>(references.size());
		for(int i = 0, size = references.size(); i < size; i ++){
			final FLEFRecord reference = references.get(i);

			final String referencedId = reference.getValue();
			if(StringUtils.isEmpty(referencedId)){
				System.err.println("No referenced ID found from reference " + reference + " from record " + record);

				continue;
			}

			final FLEFRecord referencedRecord = model.getRecordById(referencedId);
			if(referencedRecord != null && !referencedRecord.isEmpty())
				referencedRecords.add(referencedRecord);
		}
		return referencedRecords;
	}


	/*
	extract (struct):
		- `contact*: ContactStructure` | EntityListPanel | EntityListPanel.createForStructure
		- `extract*: ExtractStructure` | ExtractListPanel | EntityListPanel.createForStructure
		- `name*: NameStructure|PersonalNameStructure`, `name+: NameStructure`,
			`part+: PartStructure`, `title+: NameStructure` | EntityListPanel.createForStructure

	extract (oneof struct):
		- `variant*: TextValueVariant` | VariantListPanel
		- `single_date: SingleDate` | <VOID>
		- `value: DateValue` | <VOID>

	extract (plain):
		- `note*: Text` | ExtractListPanel | EntityListPanel.createForStructure
	*/
	public static List<FLEFRecord> extractStructures(final FLEFRecord record, final String path){
		return FLEFRecordHelper.findChildren(record, path);
	}


	/*
	extract (xref + struct):
		- `place?: PlaceCitation` | PlaceCitationField
	*/
	public static FLEFRecord extractStructureWithReference(final FLEFRecord record, final String path){
		final List<FLEFRecord> records = FLEFRecordHelper.findChildren(record, path);
		return (records.size() == 1? records.getFirst(): null);
	}

	/*
	extract (xref + struct):
		- `repository*: RepositoryCitation` | EntityCitationListPanel
		- `source*: SourceCitation` | EntityCitationListPanel
	*/
	public static List<FLEFRecord> extractStructuresWithReference(final FLEFRecord record, final String path){
		return FLEFRecordHelper.findChildren(record, path);
	}


	/*
	extract (oneof xref):
		- `identity: IdentityCandidate` | ParticipantField
		- `subject/target: RelationshipParticipant` | ParticipantField
		- `context: ContextSource` | ParticipantField
		- `target: ImpactTarget` | ParticipantField
		- `participant: EventParticipant` | ParticipantField
		- `target?: ResearchTarget` | ParticipantField
		- `preferred?: ConclusionTarget` | <VOID>
	*/
	public static FLEFRecord extractRecordFromOneOfReference(final FLEFRecord record, final String referencePath,
			final RecordLookup model){
		final List<FLEFRecord> records = extractRecordsFromOneOfReference(record, referencePath, model);
		return (records.size() == 1? records.getFirst(): null);
	}

	/*
	extract (oneof xref):
		- `resolves*: ConclusionTarget` | EntityListPanel.createForRecord
		- `target*: ResearchTarget` | EntityListPanel.createForRecord
	*/
	public static List<FLEFRecord> extractRecordsFromOneOfReference(final FLEFRecord record, final String referencePath,
			final RecordLookup model){
		final List<FLEFRecord> references = FLEFRecordHelper.findChildren(record, referencePath);
		final List<FLEFRecord> referencedRecords = new ArrayList<>(references.size());
		for(int i = 0, referenceCount = references.size(); i < referenceCount; i ++){
			FLEFRecord reference = references.get(i);

			final int size = reference.getChildren()
				.size();
			if(size == 0)
				continue;

			if(size > 1){
				LOGGER.debug("Record with more than one {}: {}", referencePath, record);

				continue;
			}

			reference = reference.getTheOnlyChild();

			final String referencedId = reference.getValue();
			if(StringUtils.isEmpty(referencedId)){
				LOGGER.debug("No referenced ID found from reference {} from record {}", reference, record);

				continue;
			}

			final FLEFRecord referencedRecord = model.getRecordById(referencedId);
			if(referencedRecord != null){
				final String referencedTag = reference.getTag();
				if(!Strings.CI.equals(referencedRecord.getTag(), referencedTag)){
					LOGGER.debug("Referenced tag differs from reference {} from record {}", reference, record);

					continue;
				}

				referencedRecords.add(referencedRecord);
			}
		}

		return referencedRecords;
	}



	/**
	 * Finds all children that match any of the given dot‑separated tag paths.
	 * The result preserves the global order of the tree (pre‑order traversal).
	 *
	 * @param parent The root record from which to start the search.
	 * @param paths  An array of dot‑separated tag paths (e.g. {"ROOT.A.CODE", "ROOT.B.VALUE"}).
	 * @return A list of matching child records, in global tree order, without duplicates.
	 */
	public static List<FLEFRecord> findChildrenOrdered(final FLEFRecord parent, final String... paths){
		final List<FLEFRecord> result = new ArrayList<>();
		if(parent == null || paths == null || paths.length == 0)
			return result;

		// Build map: targetParent → set of expected tags
		//(by identity: records that are equal are still different nodes of the tree)
		final Map<FLEFRecord, Set<String>> targetMap = new IdentityHashMap<>();
		for(int i = 0, size = paths.length; i < size; i ++){
			final String path = paths[i];

			if(StringUtils.isEmpty(path))
				continue;

			final String[] segments = StringUtils.split(path, '.');
			final FLEFRecord targetParent = navigateToParent(parent, segments);
			if(targetParent == null)
				continue;

			final Segment seg = Segment.parse(segments[segments.length - 1]);
			if(seg == null)
				continue;

			targetMap.computeIfAbsent(targetParent, k -> new HashSet<>())
				.add(seg.tag);
		}

		if(targetMap.isEmpty())
			return result;

		// Iterative pre‑order traversal (stack), tracking the parent of each node
		final Set<FLEFRecord> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		final Deque<AbstractMap.SimpleEntry<FLEFRecord, FLEFRecord>> stack = new ArrayDeque<>();
		stack.push(new AbstractMap.SimpleEntry<>(parent, null));
		while(!stack.isEmpty()){
			final AbstractMap.SimpleEntry<FLEFRecord, FLEFRecord> entry = stack.pop();
			final FLEFRecord node = entry.getKey();
			final FLEFRecord nodeParent = entry.getValue();

			if(visited.contains(node))
				continue;

			visited.add(node);

			// Check if this node is a direct child of a target parent and has a matching tag
			if(nodeParent != null && targetMap.containsKey(nodeParent)){
				final Set<String> expectedTags = targetMap.get(nodeParent);
				if(expectedTags.contains(node.getTag()))
					result.add(node);
			}

			// Push children in reverse order so that the first child is processed first (LIFO)
			final List<FLEFRecord> children = node.getChildren();
			for(int i = children.size() - 1; i >= 0; i --)
				stack.push(new AbstractMap.SimpleEntry<>(children.get(i), node));
		}

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
			return null;

		final StringBuilder sb = new StringBuilder();
		for(int i = 0, size = children.size(); i < size; i ++){
			final String value = children.get(i).getValue();
			if(StringUtils.isEmpty(value))
				continue;

			if(!sb.isEmpty())
				sb.append(',');
			sb.append(value);
		}
		return sb.toString();
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

		final FLEFRecord existing = getOrCreateTargetNode(parent, path);
		assert existing != null : "Cannot update the value of a non-existent record";
		assert findChildren(parent, path).size() == 1 : "Cannot update the value of a non-single record";
		existing.setValue(value);
	}

	/**
	 * Adds a single child with the given tag and value, if the value is not empty.
	 * The path specifies the full tag path for the new child (including its tag).
	 * The parent container is automatically created if it does not exist.
	 *
	 * @param parent The parent record.
	 * @param path   The dot‑separated path to the new child (e.g. "RESOLVES.CONCLUSION").
	 * @param value  The value to set (ignored if {@code null} or empty).
	 */
	public static void addChildValue(final FLEFRecord parent, final String path, final String value){
		if(StringUtils.isEmpty(value) || parent == null)
			return;

		final String[] segments = StringUtils.split(path, '.');
		// The last segment is the new child's tag (with optional index)
		final Segment lastSegment = Segment.parse(segments[segments.length - 1]);
		if(lastSegment == null)
			return;
		final String childTag = lastSegment.tag();

		// Parent path is everything except the last segment
		final String parentPath = (segments.length > 1
			? StringUtils.join(segments, '.', 0, segments.length - 1)
			: StringUtils.EMPTY);

		// Get or create the parent container
		final FLEFRecord container = (StringUtils.isEmpty(parentPath)
			? parent
			: getOrCreateTargetNode(parent, parentPath));
		if(container == null)
			return;

		// Create the new child and add it to the container
		final FLEFRecord child = FLEFRecord.createChildWithTagAndValue(childTag, value);
		container.addChild(child);
	}

	/**
	 * Adds a single child to the parent container specified by the path.
	 * The path specifies the full tag path for the new child (including its tag).
	 * The parent container is automatically created if it does not exist.
	 *
	 * @param parent The parent record.
	 * @param path   The dot‑separated path to the new child (e.g. "RESOLVES.CONCLUSION").
	 * @param child  The child record to add (ignored if {@code null} or empty).
	 */
	public static void addChild(final FLEFRecord parent, final String path, final FLEFRecord child){
		if(child == null || child.isEmpty() || parent == null)
			return;

		// Split path into parent path and child tag (child tag is only used for the parent container)
		final int lastDot = path.lastIndexOf('.');
		final String parentPath = (lastDot >= 0? path.substring(0, lastDot): StringUtils.EMPTY);

		// Get or create the parent container
		final FLEFRecord container = (StringUtils.isEmpty(parentPath)
			? parent
			: getOrCreateTargetNode(parent, parentPath));
		if(container == null)
			return;

		// Add the child to the container
		container.addChild(child);
	}

	/**
	 * Navigates the given path, creating intermediate and target nodes as necessary.
	 */
	public static FLEFRecord getOrCreateTargetNode(final FLEFRecord parent, final String path){
		if(parent == null)
			return null;
		if(StringUtils.isEmpty(path))
			return parent;

		//the segments are read in place (the nodes are created along the way, up to an invalid segment)
		final Cursor cursor = new Cursor(path);
		FLEFRecord current = parent;
		while(cursor.next()){
			if(!cursor.valid)
				return null;

			current = getNthChildOrCreate(current, cursor);
		}
		return current;
	}

	/**
	 * Remove a child navigating through a path of tags separated by '.'.
	 *
	 * @param parent	The starting record.
	 * @param path	The dot‑separated tag path (e.g. "ROOT.RESTRICTION[2].CODE").
	 * @return The deleted child, or {@code null} if any tag in the path is not found.
	 */
	public static FLEFRecord removeChild(final FLEFRecord parent, final String path){
		if(parent == null || StringUtils.isEmpty(path))
			return null;

		final Cursor cursor = new Cursor(path);
		FLEFRecord current = parent;
		while(cursor.next()){
			if(!cursor.valid)
				return null;

			final FLEFRecord child = getNthChild(current, cursor);
			if(child == null)
				return null;

			if(cursor.isLast()){
				current.getChildren()
					.removeIf(sibling -> sibling == child);

				return child;
			}
			current = child;
		}
		return null;
	}

	public static boolean removeChildren(final FLEFRecord parent, final String... paths){
		boolean result = true;
		for(int i = 0, size = paths.length; i < size; i ++){
			final String path = paths[i];

			result &= removeChildren(parent, path);
		}
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

		final Cursor cursor = new Cursor(path);
		//null once a segment before the last one is invalid or not found: nothing is removed, but the result depends on the last one
		FLEFRecord current = parent;
		while(cursor.next()){
			if(!cursor.isLast()){
				current = (current != null && cursor.valid? getNthChild(current, cursor): null);

				continue;
			}

			if(!cursor.valid)
				return false;

			if(cursor.index >= 0 || cursor.wildcard){
				removeChild(parent, path);

				return true;
			}

			if(current != null){
				current.getChildren()
					.removeIf(child -> cursor.tagMatches(child.getTag()));
			}
			return true;
		}
		return false;
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

	/**
	 * Reads the segments of a path of tags in place, one at a time, without splitting the path nor allocating a string for each
	 * segment. The empty segments ("a..b") are ignored, like in a split.
	 */
	private static final class Cursor{

		private final String path;
		private int position;
		private int tagStart;
		private int tagEnd;
		/** The occurrence asked for (e.g. RESTRICTION[2]), or {@code -1}. */
		private int index;
		private boolean wildcard;
		/** Whether the segment is valid ("tag[", "tag[x]", and "tag[-1]" are not). */
		private boolean valid;


		private Cursor(final String path){
			this.path = path;
		}

		/** @return whether there is a next segment, that is then the current one */
		private boolean next(){
			final int length = path.length();
			while(position < length && path.charAt(position) == '.')
				position ++;
			if(position >= length)
				return false;

			final int start = position;
			int end = path.indexOf('.', start);
			if(end < 0)
				end = length;
			position = end;

			tagStart = start;
			tagEnd = end;
			index = -1;
			wildcard = false;
			valid = true;
			final int bracket = path.indexOf('[', start);
			if(bracket >= 0 && bracket < end){
				final int close = path.indexOf(']', bracket);
				if(close < 0 || close >= end){
					valid = false;

					return true;
				}

				tagEnd = bracket;
				final String indexText = path.substring(bracket + 1, close)
					.trim();
				if("*".equals(indexText))
					wildcard = true;
				else
					try{
						index = Integer.parseInt(indexText);
						if(index < 0)
							valid = false;
					}
					catch(final NumberFormatException ignored){
						valid = false;
					}
			}
			return true;
		}

		/** @return whether the current segment is the last one */
		private boolean isLast(){
			int next = position;
			while(next < path.length() && path.charAt(next) == '.')
				next ++;
			return (next >= path.length());
		}

		private boolean tagMatches(final String tag){
			final int length = tagEnd - tagStart;
			return (tag != null && tag.length() == length && tag.regionMatches(true, 0, path, tagStart, length));
		}

		private String tag(){
			return path.substring(tagStart, tagEnd);
		}

	}

	private static FLEFRecord getNthChild(final FLEFRecord current, final Cursor cursor){
		final List<FLEFRecord> children = current.getChildren();
		int occurrence = 0;
		for(int i = 0, size = children.size(); i < size; i ++){
			final FLEFRecord child = children.get(i);
			if(!cursor.tagMatches(child.getTag()))
				continue;

			//the first child with the tag, or the occurrence asked for
			if(cursor.index == -1 || occurrence == cursor.index)
				return child;

			occurrence ++;
		}
		return null;
	}

	private static FLEFRecord getNthChildOrCreate(final FLEFRecord current, final Cursor cursor){
		final int targetIndex = Math.max(0, cursor.index);
		int occurrence = 0;
		FLEFRecord found = null;
		final List<FLEFRecord> children = current.getChildren();
		for(int i = 0, size = children.size(); i < size; i ++){
			final FLEFRecord child = children.get(i);
			if(!cursor.tagMatches(child.getTag()))
				continue;

			if(occurrence == targetIndex){
				found = child;

				break;
			}

			occurrence ++;
		}

		if(found == null){
			final String tag = cursor.tag();
			for(int i = occurrence; i <= targetIndex; i ++){
				found = FLEFRecord.createChildWithTag(tag);
				current.forceAddChild(found);
			}
		}
		return found;
	}

	private static FLEFRecord getNthChild(FLEFRecord current, final Segment segment){
		final List<FLEFRecord> children = current.getChildren();
		final int size = children.size();
		if(segment.index == -1){
			// find the first child with that tag
			for(int i = 0; i < size; i ++){
				final FLEFRecord child = children.get(i);

				if(tagEquals(child.getTag(), segment.tag))
					return child;
			}
		}
		else{
			int occurrence = 0;
			for(int i = 0; i < size; i ++){
				final FLEFRecord child = children.get(i);

				if(tagEquals(child.getTag(), segment.tag)){
					if(occurrence == segment.index)
						return child;

					occurrence ++;
				}
			}
		}
		return null;
	}

	private static FLEFRecord getNthChildOrCreate(FLEFRecord current, final Segment segment){
		final int targetIndex = Math.max(0, segment.index);
		int occurrence = 0;
		FLEFRecord found = null;
		final List<FLEFRecord> children = current.getChildren();
		for(int i = 0, size = children.size(); i < size; i ++){
			final FLEFRecord child = children.get(i);

			if(!tagEquals(child.getTag(), segment.tag))
				continue;

			if(occurrence == targetIndex){
				found = child;

				break;
			}

			occurrence ++;
		}

		if(found == null)
			for(int i = occurrence; i <= targetIndex; i ++){
				found = FLEFRecord.createChildWithTag(segment.tag);
				current.forceAddChild(found);
			}

		return found;
	}

	private static FLEFRecord navigateToParent(final FLEFRecord root, final String[] segments){
		FLEFRecord current = root;
		for(int i = 0, length = segments.length - 1; i < length; i ++){
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
		for(int i = 0, length = segments.length - 1; i < length; i ++){
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
