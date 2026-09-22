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
package io.github.mtrevisan.familylegacy.v2.ui.tools.research;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchTaskHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Shared extraction utilities and precomputed indexes for the
 * research-related records: {@code ResearchQuestionRecord},
 * {@code ResearchActivityRecord}, {@code ResearchTaskRecord},
 * {@code ConclusionRecord}, and {@code IdentityHypothesisRecord}.
 * <p>
 * The methods here are the single point where research fields are read
 * from the model. Every tool in the {@code research} package goes
 * through this class.
 * <p>
 * <b>Precomputed indexes.</b> The record types in this package are
 * cross-referenced (a task points to a question, an activity points to a
 * question and to a parent activity, a conclusion points to a question).
 * Every dialog that iterates over one type while resolving references to
 * another type must build the reference index once and reuse it, rather
 * than calling a lookup method inside the loop. The methods in this
 * class follow that pattern: they return maps that are cheap to build
 * and O(1) to query.
 */
public final class ResearchHelper{

	public static final String TYPE_QUESTION = "research_question";
	public static final String TYPE_ACTIVITY = "research_activity";
	public static final String TYPE_TASK = "research_task";
	public static final String TYPE_CONCLUSION = "conclusion";
	public static final String TYPE_IDENTITY = "identity_hypothesis";

	public static final String TAG_TITLE = "title";
	public static final String TAG_QUESTION = "question";
	public static final String TAG_STATUS = "status";
	public static final String TAG_RATIONALE = "rationale";
	public static final String TAG_CONCLUSION = "conclusion";
	public static final String TAG_CONFIDENCE = "conclusion_confidence";
	public static final String TAG_CLOSED_DATE = "closed_date";

	public static final String TAG_ACTIVITY_TYPE = "activity_type";
	public static final String TAG_ACTION = "action";
	public static final String TAG_TARGET = "target";
	public static final String TAG_RESULT = "result";
	public static final String TAG_OBSERVATION = "observation";
	public static final String TAG_PARENT_ACTIVITY = "parent_activity";

	public static final String TAG_DESCRIPTION = "description";
	public static final String TAG_CREATED_BY = "created_by";
	public static final String TAG_PRIORITY = "priority";
	public static final String TAG_DUE_DATE = "due_date";
	public static final String TAG_OUTCOME = "outcome";

	public static final String TAG_ISSUE = "issue";
	public static final String TAG_PROOF_STATUS = "proof_status";
	public static final String TAG_NARRATIVE = "narrative";
	public static final String TAG_RESOLVES = "resolves";
	public static final String TAG_PREFERRED = "preferred";
	public static final String TAG_RESEARCH = "research";

	public static final String TAG_IDENTITY = "identity";
	public static final String TAG_COMMENT = "comment";
	public static final String TAG_EVIDENCE = "evidence";
	public static final String TAG_SOURCE_TYPE = "source_type";
	public static final String TAG_INFORMATION_TYPE = "information_type";
	public static final String TAG_EVIDENCE_TYPE = "evidence_type";


	private ResearchHelper(){
	}


	/* ======================================================================
	 *                          List accessors
	 * ====================================================================== */

	public static List<FLEFRecord> listQuestions(final FLEFModel model){
		return model.getRecordsByType(ResearchQuestionHandler.TYPE);
	}

	public static List<FLEFRecord> listActivities(final FLEFModel model){
		return model.getRecordsByType(ResearchActivityHandler.TYPE);
	}

	public static List<FLEFRecord> listTasks(final FLEFModel model){
		return model.getRecordsByType(ResearchTaskHandler.TYPE);
	}

	public static List<FLEFRecord> listConclusions(final FLEFModel model){
		return model.getRecordsByType(ConclusionHandler.TYPE);
	}

	public static List<FLEFRecord> listIdentityHypotheses(final FLEFModel model){
		return model.getRecordsByType(IdentityHypothesisHandler.TYPE);
	}


	/* ======================================================================
	 *                          Field extraction
	 * ====================================================================== */

	/** First non-blank text among the direct children with the given tag. */
	public static String firstTextValue(final FLEFRecord record, final String tag){
		if(record == null)
			return null;
		final String direct = FLEFRecordHelper.getChildValue(record, tag);
		if(direct != null && !direct.isBlank())
			return direct;
		final FLEFRecord child = FLEFRecordHelper.findChild(record, tag);
		if(child == null)
			return null;
		final FLEFRecord onlyChild = child.getTheOnlyChild();
		return (onlyChild != null? onlyChild.getValue(): null);
	}

	/** Title of a research question, or the id when missing. */
	public static String questionTitle(final FLEFRecord question){
		final String title = firstTextValue(question, TAG_TITLE);
		return (title != null && !title.isBlank()? title: question.getId());
	}

	/** Question text, or {@code null}. */
	public static String questionText(final FLEFRecord question){
		return firstTextValue(question, TAG_QUESTION);
	}

	/** Status of a research question, task, or activity. */
	public static String status(final FLEFRecord record){
		return firstTextValue(record, TAG_STATUS);
	}

	/** Type of a research activity. */
	public static String activityType(final FLEFRecord activity){
		return firstTextValue(activity, TAG_ACTIVITY_TYPE);
	}

	/** Action text of an activity. */
	public static String action(final FLEFRecord activity){
		return firstTextValue(activity, TAG_ACTION);
	}

	/** Result of an activity, or {@code null}. */
	public static String activityResult(final FLEFRecord activity){
		return firstTextValue(activity, TAG_RESULT);
	}

	/** Description of a task. */
	public static String taskDescription(final FLEFRecord task){
		return firstTextValue(task, TAG_DESCRIPTION);
	}

	/** Priority of a task. */
	public static String taskPriority(final FLEFRecord task){
		return firstTextValue(task, TAG_PRIORITY);
	}

	/** Issue resolved by a conclusion. */
	public static String conclusionIssue(final FLEFRecord conclusion){
		return firstTextValue(conclusion, TAG_ISSUE);
	}

	/** Proof status of a conclusion. */
	public static String proofStatus(final FLEFRecord conclusion){
		return firstTextValue(conclusion, TAG_PROOF_STATUS);
	}

	/** Comment of an identity hypothesis. */
	public static String identityComment(final FLEFRecord hypothesis){
		return firstTextValue(hypothesis, TAG_COMMENT);
	}


	/* ======================================================================
	 *                          Precomputed indexes
	 * ====================================================================== */

	/**
	 * Groups a list of records by the value of a given single-valued
	 * child tag. Records whose tag is missing or blank are skipped.
	 */
	public static Map<String, List<FLEFRecord>> groupByTag(
		final List<FLEFRecord> records, final String tag){
		final Map<String, List<FLEFRecord>> result = new LinkedHashMap<>();
		for(final FLEFRecord r : records){
			final String key = firstTextValue(r, tag);
			if(key != null && !key.isBlank())
				result.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
		}
		return result;
	}

	/**
	 * Counts the occurrences of a tag's values across a list of records.
	 * Useful for report tools that show distributions.
	 */
	public static Map<String, Integer> countByTag(
		final List<FLEFRecord> records, final String tag){
		final Map<String, Integer> result = new LinkedHashMap<>();
		for(final FLEFRecord r : records){
			final String value = firstTextValue(r, tag);
			if(value != null && !value.isBlank())
				result.merge(value, 1, Integer::sum);
		}
		return result;
	}

	/**
	 * Counts how many records reference a given id through a tag. The
	 * result is keyed by the referenced id.
	 */
	public static Map<String, Integer> countReferences(
		final List<FLEFRecord> records, final String tag){
		final Map<String, Integer> result = new LinkedHashMap<>();
		for(final FLEFRecord r : records){
			final String ref = firstTextValue(r, tag);
			if(ref != null && !ref.isBlank())
				result.merge(ref, 1, Integer::sum);
		}
		return result;
	}


	/* ======================================================================
	 *                          Row models
	 * ====================================================================== */

	/** Row for the research question table. */
	public record QuestionRow(String id, String title, String status, int activityCount, int taskCount,
		int conclusionCount){}

	/** Row for the research activity table. */
	public record ActivityRow(String id, String questionId, String questionTitle, String activityType, String status,
		String action, String result){}

	/** Row for the research task table. */
	public record TaskRow(String id, String questionId, String description, String status, String priority,
		String dueDate){}

	/** Row for the conclusion table. */
	public record ConclusionRow(String id, String issue, String proofStatus, int resolvesCount, String narrative){}

	/** Row for the identity hypothesis table. */
	public record IdentityRow(String id, String firstCandidate, String secondCandidate, String comment){}


	public static QuestionRow toQuestionRow(final FLEFRecord question,
		final Map<String, Integer> activityCountByQuestion,
		final Map<String, Integer> taskCountByQuestion,
		final Map<String, Integer> conclusionCountByQuestion){
		final String id = question.getId();
		return new QuestionRow(
			id,
			questionTitle(question),
			status(question),
			activityCountByQuestion.getOrDefault(id, 0),
			taskCountByQuestion.getOrDefault(id, 0),
			conclusionCountByQuestion.getOrDefault(id, 0)
		);
	}

	public static ActivityRow toActivityRow(final FLEFRecord activity,
		final Map<String, FLEFRecord> questionsById){
		final String questionId = firstTextValue(activity, TAG_QUESTION);
		final FLEFRecord q = (questionId != null? questionsById.get(questionId): null);
		return new ActivityRow(
			activity.getId(),
			questionId,
			q != null? questionTitle(q): null,
			activityType(activity),
			status(activity),
			action(activity),
			activityResult(activity)
		);
	}

	public static TaskRow toTaskRow(final FLEFRecord task,
		final Map<String, FLEFRecord> questionsById){
		final String questionId = firstTextValue(task, TAG_QUESTION);
		return new TaskRow(
			task.getId(),
			questionId,
			taskDescription(task),
			status(task),
			taskPriority(task),
			firstTextValue(task, TAG_DUE_DATE)
		);
	}

	public static ConclusionRow toConclusionRow(final FLEFRecord conclusion){
		final int resolvesCount = conclusion.getChildren().stream()
			.filter(c -> TAG_RESOLVES.equalsIgnoreCase(c.getTag()))
			.mapToInt(c -> 1)
			.sum();
		return new ConclusionRow(
			conclusion.getId(),
			conclusionIssue(conclusion),
			proofStatus(conclusion),
			resolvesCount,
			firstTextValue(conclusion, TAG_NARRATIVE)
		);
	}

	/**
	 * Builds a row for an identity hypothesis. The two candidates are
	 * the two {@code identity} children; each contains a oneof block
	 * whose only child is the referenced id.
	 */
	public static IdentityRow toIdentityRow(final FLEFRecord hypothesis, final FLEFModel model){
		final List<String> candidates = new ArrayList<>();
		for(final FLEFRecord child : hypothesis.getChildren()){
			if(!TAG_IDENTITY.equalsIgnoreCase(child.getTag()))
				continue;
			final FLEFRecord oneof = child.getTheOnlyChild();
			if(oneof == null)
				continue;
			final FLEFRecord ref = oneof.getTheOnlyChild();
			final String id = (ref != null? ref.getValue(): oneof.getValue());
			if(id != null)
				candidates.add(id);
		}
		final String first = (candidates.size() > 0? candidates.get(0): StringUtils.EMPTY);
		final String second = (candidates.size() > 1? candidates.get(1): StringUtils.EMPTY);
		final String firstLabel = (StringUtils.isNotEmpty(first)? IndividualHandler.getInstance().getDisplayText(model.getRecordById(first), model): first);
		final String secondLabel = (StringUtils.isNotEmpty(second)? IndividualHandler.getInstance().getDisplayText(model.getRecordById(second), model): first);
		return new IdentityRow(hypothesis.getId(), firstLabel, secondLabel, identityComment(hypothesis));
	}

}
