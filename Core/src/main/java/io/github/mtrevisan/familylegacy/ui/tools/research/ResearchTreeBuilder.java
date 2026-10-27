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
package io.github.mtrevisan.familylegacy.ui.tools.research;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Builds the {@code question -> activity -> task -> conclusion} tree shown by {@link ResearchBoardDialog}.
 * <p>
 * Each research question is a root. Under it are:
 * <ul>
 *   <li>the activities that reference it (nested under their {@code parent_activity} when that is an activity of the same
 *       question), each with the tasks it created ({@code created_by});</li>
 *   <li>the tasks that reference the question but were not created by one of those activities;</li>
 *   <li>the conclusions that cite the question ({@code research}).</li>
 * </ul>
 * Activities, tasks, and conclusions that cannot be placed under any existing question are collected under a synthetic
 * {@link Kind#ORPHANS} node, so nothing is silently dropped. A record that references more than one question appears
 * under each of them.
 * <p>
 * The class holds no Swing state, so it can be tested on a plain model.
 */
public final class ResearchTreeBuilder{

	/** The kind of a tree node. */
	public enum Kind{
		QUESTION("Q"),
		ACTIVITY("A"),
		TASK("T"),
		CONCLUSION("C"),
		/** Synthetic parent of the records that reference no existing question. */
		ORPHANS("?");


		private final String symbol;


		Kind(final String symbol){
			this.symbol = symbol;
		}

		public String symbol(){
			return symbol;
		}
	}


	/** A node of the research tree. */
	public static final class Node{

		private final Kind kind;
		private final FLEFRecord record;
		private final String label;
		private final List<Node> children = new ArrayList<>();


		Node(final Kind kind, final FLEFRecord record, final String label){
			this.kind = kind;
			this.record = record;
			this.label = label;
		}

		public Kind kind(){
			return kind;
		}

		/** The underlying record, {@code null} for the synthetic {@link Kind#ORPHANS} node. */
		public FLEFRecord record(){
			return record;
		}

		public String label(){
			return label;
		}

		public List<Node> children(){
			return children;
		}

		/** Number of descendants of the given kind. */
		public int count(final Kind descendantKind){
			int count = 0;
			for(final Node child : children){
				if(child.kind == descendantKind)
					count ++;
				count += child.count(descendantKind);
			}
			return count;
		}

		/**
		 * Returns a copy of the subtree that keeps only the branches whose label (or a descendant's) contains the text. A
		 * node whose own label matches is kept with all its children.
		 *
		 * @param needle	The text to look for, case-insensitive.
		 * @return	The pruned copy, or {@code null} if nothing in the subtree matches.
		 */
		public Node filter(final String needle){
			if(StringUtils.isBlank(needle))
				return this;

			final String lowerNeedle = needle.trim()
				.toLowerCase(Locale.ROOT);
			return filterLowerCase(lowerNeedle);
		}

		private Node filterLowerCase(final String needle){
			if(label.toLowerCase(Locale.ROOT).contains(needle))
				return this;

			final Node copy = new Node(kind, record, label);
			for(final Node child : children){
				final Node filtered = child.filterLowerCase(needle);
				if(filtered != null)
					copy.children.add(filtered);
			}
			return (!copy.children.isEmpty()? copy: null);
		}

		@Override
		public String toString(){
			return label;
		}

	}


	private static final int MAX_LABEL_LENGTH = 90;


	private ResearchTreeBuilder(){}


	/**
	 * Builds the forest of research questions.
	 *
	 * @param model	The model to read from.
	 * @return	The root nodes: one per question, plus an {@link Kind#ORPHANS} node when some record is not reachable from a
	 * 	question.
	 */
	public static List<Node> build(final FLEFModel model){
		final List<FLEFRecord> questions = ResearchHelper.listQuestions(model);
		final List<FLEFRecord> activities = ResearchHelper.listActivities(model);
		final List<FLEFRecord> tasks = ResearchHelper.listTasks(model);
		final List<FLEFRecord> conclusions = ResearchHelper.listConclusions(model);

		final Set<String> questionIds = new HashSet<>();
		for(final FLEFRecord question : questions)
			questionIds.add(question.getId());

		final Map<String, List<FLEFRecord>> activitiesByQuestion = groupByReferences(activities, ResearchActivityReader.TAG_QUESTION);
		final Map<String, List<FLEFRecord>> tasksByQuestion = groupByReferences(tasks, ResearchTaskReader.TAG_QUESTION);
		final Map<String, List<FLEFRecord>> conclusionsByQuestion = groupByReferences(conclusions, ConclusionReader.TAG_RESEARCH);
		final Map<String, List<FLEFRecord>> tasksByActivity = groupByReferences(tasks, ResearchTaskReader.TAG_CREATED_BY);

		final Set<String> placed = new HashSet<>();
		final List<Node> roots = new ArrayList<>();
		for(final FLEFRecord question : questions){
			final Node questionNode = new Node(Kind.QUESTION, question, questionLabel(question));

			final List<FLEFRecord> questionActivities = activitiesByQuestion.getOrDefault(question.getId(), List.of());
			final Set<String> activityIds = new HashSet<>();
			for(final FLEFRecord activity : questionActivities)
				activityIds.add(activity.getId());

			addActivities(questionNode, questionActivities, activityIds, tasksByActivity, placed);

			for(final FLEFRecord task : tasksByQuestion.getOrDefault(question.getId(), List.of()))
				if(!createdByAny(task, activityIds)){
					questionNode.children.add(new Node(Kind.TASK, task, taskLabel(task)));
					placed.add(task.getId());
				}
			for(final FLEFRecord conclusion : conclusionsByQuestion.getOrDefault(question.getId(), List.of())){
				questionNode.children.add(new Node(Kind.CONCLUSION, conclusion, conclusionLabel(conclusion)));
				placed.add(conclusion.getId());
			}

			roots.add(withCounts(questionNode));
			placed.add(question.getId());
		}

		final Node orphans = new Node(Kind.ORPHANS, null, "(no question)");
		for(final FLEFRecord activity : activities)
			if(!placed.contains(activity.getId()))
				orphans.children.add(new Node(Kind.ACTIVITY, activity, activityLabel(activity)));
		for(final FLEFRecord task : tasks)
			if(!placed.contains(task.getId()))
				orphans.children.add(new Node(Kind.TASK, task, taskLabel(task)));
		for(final FLEFRecord conclusion : conclusions)
			if(!placed.contains(conclusion.getId()))
				orphans.children.add(new Node(Kind.CONCLUSION, conclusion, conclusionLabel(conclusion)));
		if(!orphans.children.isEmpty())
			roots.add(orphans);

		return roots;
	}

	/**
	 * Adds the activities under the parent node: those whose {@code parent_activity} is another activity of the same
	 * question are nested under it, the others are direct children. Activities that would end up unreachable (a cycle of
	 * parents) are added as direct children, so none is lost.
	 */
	private static void addActivities(final Node parent, final List<FLEFRecord> activities, final Set<String> activityIds,
			final Map<String, List<FLEFRecord>> tasksByActivity, final Set<String> placed){
		final Map<String, List<FLEFRecord>> childrenByParent = new HashMap<>();
		final List<FLEFRecord> roots = new ArrayList<>();
		for(final FLEFRecord activity : activities){
			final String parentId = FLEFRecordHelper.getChildValue(activity, ResearchActivityReader.TAG_PARENT_ACTIVITY);
			if(parentId != null && activityIds.contains(parentId) && !parentId.equals(activity.getId()))
				childrenByParent.computeIfAbsent(parentId, k -> new ArrayList<>())
					.add(activity);
			else
				roots.add(activity);
		}

		final Set<String> visited = new HashSet<>();
		for(final FLEFRecord root : roots)
			parent.children.add(buildActivityNode(root, childrenByParent, tasksByActivity, visited, placed));
		//activities caught in a parent cycle
		for(final FLEFRecord activity : activities)
			if(!visited.contains(activity.getId()))
				parent.children.add(buildActivityNode(activity, Map.of(), tasksByActivity, visited, placed));
	}

	private static Node buildActivityNode(final FLEFRecord activity, final Map<String, List<FLEFRecord>> childrenByParent,
			final Map<String, List<FLEFRecord>> tasksByActivity, final Set<String> visited, final Set<String> placed){
		visited.add(activity.getId());
		placed.add(activity.getId());

		final Node node = new Node(Kind.ACTIVITY, activity, activityLabel(activity));
		for(final FLEFRecord task : tasksByActivity.getOrDefault(activity.getId(), List.of())){
			node.children.add(new Node(Kind.TASK, task, taskLabel(task)));
			placed.add(task.getId());
		}
		for(final FLEFRecord child : childrenByParent.getOrDefault(activity.getId(), List.of()))
			if(!visited.contains(child.getId()))
				node.children.add(buildActivityNode(child, childrenByParent, tasksByActivity, visited, placed));
		return node;
	}

	private static boolean createdByAny(final FLEFRecord task, final Set<String> activityIds){
		for(final String activityId : references(task, ResearchTaskReader.TAG_CREATED_BY))
			if(activityIds.contains(activityId))
				return true;
		return false;
	}

	/** Returns the question node with the counts of its activities, tasks, and conclusions appended to the label. */
	private static Node withCounts(final Node questionNode){
		final Node node = new Node(questionNode.kind, questionNode.record,
			questionNode.label + " (" + questionNode.count(Kind.ACTIVITY) + "A " + questionNode.count(Kind.TASK) + "T "
				+ questionNode.count(Kind.CONCLUSION) + "C)");
		node.children.addAll(questionNode.children);
		return node;
	}


	/* ======================================================================
	 *                          Labels
	 * ====================================================================== */

	private static String questionLabel(final FLEFRecord question){
		return withDetails(ResearchHelper.questionTitle(question), ResearchHelper.status(question));
	}

	private static String activityLabel(final FLEFRecord activity){
		final String type = ResearchHelper.activityType(activity);
		final String action = ResearchHelper.action(activity);
		final String text = (StringUtils.isNotBlank(type)? type: "activity")
			+ (StringUtils.isNotBlank(action)? ": " + action: StringUtils.EMPTY);
		return withDetails(text, ResearchHelper.status(activity), ResearchHelper.activityResult(activity));
	}

	private static String taskLabel(final FLEFRecord task){
		final String description = ResearchHelper.taskDescription(task);
		return withDetails((StringUtils.isNotBlank(description)? description: task.getId()), ResearchHelper.status(task),
			ResearchHelper.taskPriority(task), ResearchHelper.firstTextValue(task, ResearchTaskReader.TAG_DUE_DATE));
	}

	private static String conclusionLabel(final FLEFRecord conclusion){
		final String issue = ResearchHelper.conclusionIssue(conclusion);
		return withDetails((StringUtils.isNotBlank(issue)? issue: conclusion.getId()), ResearchHelper.proofStatus(conclusion));
	}

	private static String withDetails(final String text, final String... details){
		final List<String> parts = new ArrayList<>();
		for(final String detail : details)
			if(StringUtils.isNotBlank(detail))
				parts.add(detail);
		final String abbreviated = StringUtils.abbreviate(StringUtils.normalizeSpace(text), MAX_LABEL_LENGTH);
		return (parts.isEmpty()? abbreviated: abbreviated + " [" + String.join(", ", parts) + "]");
	}


	/* ======================================================================
	 *                          References
	 * ====================================================================== */

	/** The ids referenced by the children with the given tag (the tag may be repeated). */
	public static Set<String> references(final FLEFRecord record, final String tag){
		final Set<String> ids = new LinkedHashSet<>();
		for(final FLEFRecord reference : FLEFRecordHelper.findChildren(record, tag)){
			String id = reference.getValue();
			if(StringUtils.isBlank(id)){
				final FLEFRecord only = reference.getTheOnlyChild();
				id = (only != null? only.getValue(): null);
			}
			if(StringUtils.isNotBlank(id))
				ids.add(id);
		}
		return ids;
	}

	private static Map<String, List<FLEFRecord>> groupByReferences(final List<FLEFRecord> records, final String tag){
		final Map<String, List<FLEFRecord>> result = new HashMap<>();
		for(final FLEFRecord record : records)
			for(final String id : references(record, tag))
				result.computeIfAbsent(id, k -> new ArrayList<>())
					.add(record);
		return result;
	}

}
