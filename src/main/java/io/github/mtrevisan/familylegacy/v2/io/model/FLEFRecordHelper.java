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

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Utility methods for working with FLEFRecord objects.
 * Provides common operations for finding, adding, and updating child records.
 */
public final class FLEFRecordHelper{

	private static final String TAG_VOID = "VOID";


	private record Segment(String tag, int index){
		static Segment parse(final String segment){
			final int startArrayIndex = segment.indexOf('[');
			if(startArrayIndex < 0)
				return new Segment(segment, 0);

			final String tag = segment.substring(0, startArrayIndex);
			final int endArrayIndex = segment.indexOf(']', startArrayIndex);
			final int index;
			try{
				index = Integer.parseInt(segment.substring(startArrayIndex + 1, endArrayIndex));
			}
			catch(final NumberFormatException ignored){
				return null;
			}
			if(index < 0)
				return null;

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
		final String[] segments = StringUtils.split(path, '.');
		for(final String segment : segments){
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
	 * @param paths	The array of dot‑separated tag paths to search for (e.g. "[GROUP, ROOT.RESTRICTION[2].CODE]").
	 * @return	A (unordered) list of matching child records.
	 */
	public static List<FLEFRecord> findChildren(final FLEFRecord parent, final String... paths){
		final List<FLEFRecord> result = new ArrayList<>();
		if(parent == null || paths == null)
			return result;

		for(final String path : paths)
			result.addAll(findChildren(parent, path));
		return result;
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
		if(parent == null)
			return result;
		if(StringUtils.isEmpty(path))
			return parent.getChildren();

		final String[] segments = StringUtils.split(path, '.');
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


	/*
	extract (xref):
		- `event: Xref<EventRecord>` | ParticipantField
	*/
	public static FLEFRecord extractRecordFromReference(final FLEFRecord record, final String referencePath,
			final FLEFModel model){
		final List<FLEFRecord> records = extractRecordsFromReference(record, referencePath, model);
		return (!records.isEmpty()? records.getFirst(): null);
	}

	/*
	extract (xref):
		- `note*: Xref<NoteRecord>` | EntityListPanel
	*/
	public static List<FLEFRecord> extractRecordsFromReference(final FLEFRecord record, final String referencePath,
			final FLEFModel model){
		final List<FLEFRecord> references = FLEFRecordHelper.findChildren(record, referencePath);
		final List<FLEFRecord> referencedRecords = new ArrayList<>(references.size());
		for(final FLEFRecord reference : references){
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
		- `contact*: ContactStructure` | EntityReferenceListPanel | EntityReferenceListPanel.createForStructure
		- `extract*: ExtractStructure` | ExtractListPanel | EntityReferenceListPanel.createForStructure
		- `name*: ClassifiedNameStructure|PersonalNameStructure`, `name+: ClassifiedNameStructure`,
			`part+: PartStructure`, `title+: NameStructure` | EntityReferenceListPanel.createForStructure

	extract (oneof struct):
		- `variant*: TextValueVariant` | VariantListPanel
		- `single_date: SingleDate` | <VOID>
		- `value: DateValue` | <VOID>

	extract (plain):
		- `note*: Text` | ExtractListPanel | EntityReferenceListPanel.createForStructure
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
		return (!records.isEmpty()? records.getFirst(): null);
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
		- `subject/candidate: IdentityCandidate` | ParticipantField
		- `object/subject: RelationshipParticipant` | ParticipantField
		- `context: ContextSource` | ParticipantField
		- `target: ImpactTarget` | ParticipantField
		- `participant: EventParticipant` | ParticipantField
		- `target?: ResearchTarget` | ParticipantField
		- `preferred?: ConclusionTarget` | <VOID>
	*/
	public static FLEFRecord extractRecordFromOneOfReference(final FLEFRecord record, final String referencePath,
			final FLEFModel model){
		final List<FLEFRecord> records = extractRecordsFromOneOfReference(record, referencePath, model);
		return (!records.isEmpty()? records.getFirst(): null);
	}

	/*
	extract (oneof xref):
		- `resolves*: ConclusionTarget` | EntityReferenceListPanel.createForRecord
		- `target*: ResearchTarget` | EntityReferenceListPanel.createForRecord
	*/
	public static List<FLEFRecord> extractRecordsFromOneOfReference(final FLEFRecord record, final String referencePath,
			final FLEFModel model){
		final List<FLEFRecord> references = FLEFRecordHelper.findChildren(record, referencePath);
		final List<FLEFRecord> referencedRecords = new ArrayList<>(references.size());
		for(FLEFRecord reference : references){
			final int size = reference.getChildren()
				.size();
			if(size == 0)
				continue;
			if(size > 1){
				System.err.println("Record with more than one " + referencePath + ": " + record);

				continue;
			}

			reference = reference.getTheOnlyChild();

			final String referencedTag = reference.getTag();
			if(TAG_VOID.equals(referencedTag))
				continue;

			final String referencedId = reference.getValue();
			if(StringUtils.isEmpty(referencedId)){
				System.err.println("No referenced ID found from reference " + reference + " from record " + record);

				continue;
			}

			final FLEFRecord referencedRecord = model.getRecordById(referencedId);
			if(referencedRecord != null && !referencedRecord.getTag().equals(referencedTag)){
				System.err.println("Referenced tag differs from reference " + reference + " from record " + record);

				continue;
			}

			referencedRecords.add(referencedRecord);
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

		// 1. Build map: targetParent → set of expected tags
		final Map<FLEFRecord, Set<String>> targetMap = new HashMap<>();
		for(final String path : paths){
			if(StringUtils.isEmpty(path))
				continue;

			final String[] segments = StringUtils.split(path, '.');
			final FLEFRecord targetParent = navigateToParent(parent, segments);
			if(targetParent == null)
				continue;

			final Segment seg = Segment.parse(segments[segments.length - 1]);
			if(seg == null)
				continue;

			targetMap.computeIfAbsent(targetParent, k -> new HashSet<>()).add(seg.tag);
		}

		if(targetMap.isEmpty())
			return result;

		// 2. Iterative pre‑order traversal (stack), tracking the parent of each node
		final Set<FLEFRecord> visited = new HashSet<>();
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

		final FLEFRecord existing = getOrCreateTargetNode(parent, path);
		assert existing != null : "Cannot update the value of a non-existent record";
		assert findChildren(parent, path).size() != 1 : "Cannot update the value of a non-single record";
		existing.setValue(value);
	}

	/**
	 * Adds a single child with the given tag and value, if value is not empty.
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

		final String[] segments = StringUtils.split(path, '.');
		final FLEFRecord targetParent = navigateToParentAndCreate(parent, segments);
		if(targetParent == null)
			return null;

		final Segment seg = Segment.parse(segments[segments.length - 1]);
		if(seg == null)
			return null;

		FLEFRecord target = getNthChild(targetParent, seg);
		// Create missing occurrences up to the requested index
		if(target == null){
			final long existingCount = targetParent.countChildrenWithTag(seg.tag());
			for(long i = existingCount; i <= seg.index(); i ++){
				target = FLEFRecord.createChildWithTag(seg.tag());
				targetParent.forceAddChild(target);
			}
		}

		return target;
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

		final String[] segments = StringUtils.split(path, '.');
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

		final String[] segments = StringUtils.split(path, '.');
		final Segment lastSegment = Segment.parse(segments[segments.length - 1]);
		if(lastSegment == null)
			return false;

		if(lastSegment.index >= 0){
			removeChild(parent, path);

			return true;
		}

		final FLEFRecord targetParent = navigateToParent(parent, segments);
		if(targetParent != null)
			targetParent.getChildren()
				.removeIf(child -> lastSegment.tag.equals(child.getTag()));

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
		final List<FLEFRecord> children = current.getChildren();
		if(segment.index == 0){
			// find the first child with that tag
			current = null;
			for(final FLEFRecord child : children)
				if(segment.tag.equals(child.getTag()))
					current = child;
		}
		else{
			int occurrence = 0;
			FLEFRecord found = null;
			for(final FLEFRecord child : children)
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
				found = FLEFRecord.createChildWithTag(segment.tag);
				current.forceAddChild(found);
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
