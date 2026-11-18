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
package io.github.mtrevisan.familylegacy.research;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


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

	private ResearchHelper(){}


	/* ======================================================================
	 *                          List accessors
	 * ====================================================================== */

	public static List<FLEFRecord> listQuestions(final FLEFModel model){
		return PrivacyService.visible(model, model.getRecordsByType(ResearchQuestionReader.TYPE));
	}

	public static List<FLEFRecord> listActivities(final FLEFModel model){
		return PrivacyService.visible(model, model.getRecordsByType(ResearchActivityReader.TYPE));
	}

	public static List<FLEFRecord> listTasks(final FLEFModel model){
		return PrivacyService.visible(model, model.getRecordsByType(ResearchTaskReader.TYPE));
	}

	public static List<FLEFRecord> listConclusions(final FLEFModel model){
		return PrivacyService.visible(model, model.getRecordsByType(ConclusionReader.TYPE));
	}

	public static List<FLEFRecord> listIdentityHypotheses(final FLEFModel model){
		return PrivacyService.visible(model, model.getRecordsByType(IdentityHypothesisReader.TYPE));
	}


	/* ======================================================================
	 *                          Field extraction
	 * ====================================================================== */

	/** First non-blank text among the direct children with the given tag. */
	public static String firstTextValue(final FLEFRecord record, final String tag){
		if(record == null)
			return null;
		//a tag may be repeated (an activity or a task can contribute to several questions): the first one with a text wins
		for(final FLEFRecord child : FLEFRecordHelper.findChildren(record, tag)){
			final String direct = child.getValue();
			if(direct != null && !direct.isBlank())
				return direct;
			final FLEFRecord onlyChild = child.getTheOnlyChild();
			if(onlyChild != null && onlyChild.getValue() != null && !onlyChild.getValue().isBlank())
				return onlyChild.getValue();
		}
		return null;
	}

	/** Title of a research question, or the id when missing. */
	public static String questionTitle(final FLEFRecord question){
		final String title = firstTextValue(question, ResearchQuestionReader.TAG_TITLE);
		return (title != null && !title.isBlank()? title: question.getId());
	}

	/** Question text, or {@code null}. */
	public static String questionText(final FLEFRecord question){
		return firstTextValue(question, ResearchQuestionReader.TAG_QUESTION);
	}

	/** Status of a research question, task, or activity. */
	public static String status(final FLEFRecord record){
		if(ResearchQuestionReader.isOfType(record))
			return firstTextValue(record, ResearchQuestionReader.TAG_STATUS);
		if(ResearchTaskReader.isOfType(record))
			return firstTextValue(record, ResearchTaskReader.TAG_STATUS);
		if(ResearchActivityReader.isOfType(record))
			return firstTextValue(record, ResearchActivityReader.TAG_STATUS);
		return null;
	}

	/** Type of a research activity. */
	public static String activityType(final FLEFRecord activity){
		return firstTextValue(activity, ResearchActivityReader.TAG_ACTIVITY_TYPE);
	}

	/** Action text of an activity. */
	public static String action(final FLEFRecord activity){
		return firstTextValue(activity, ResearchActivityReader.TAG_ACTION);
	}

	/** Result of an activity, or {@code null}. */
	public static String activityResult(final FLEFRecord activity){
		return firstTextValue(activity, ResearchActivityReader.TAG_RESULT);
	}

	/** Description of a task. */
	public static String taskDescription(final FLEFRecord task){
		return firstTextValue(task, ResearchTaskReader.TAG_DESCRIPTION);
	}

	/** Priority of a task. */
	public static String taskPriority(final FLEFRecord task){
		return firstTextValue(task, ResearchTaskReader.TAG_PRIORITY);
	}

	/** Issue resolved by a conclusion. */
	public static String conclusionIssue(final FLEFRecord conclusion){
		return firstTextValue(conclusion, ConclusionReader.TAG_ISSUE);
	}

	/** Proof status of a conclusion. */
	public static String proofStatus(final FLEFRecord conclusion){
		return firstTextValue(conclusion, ConclusionReader.TAG_PROOF_STATUS);
	}

	/** Comment of an identity hypothesis. */
	public static String identityComment(final FLEFRecord hypothesis){
		return firstTextValue(hypothesis, IdentityHypothesisReader.TAG_RATIONALE);
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
				result.computeIfAbsent(key, k -> new ArrayList<>())
					.add(r);
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
		//a record may reference several ids through a repeated tag (an activity can contribute to several questions): each one counts
		final Map<String, Integer> result = new LinkedHashMap<>();
		for(final FLEFRecord r : records){
			final Set<String> ids = new HashSet<>();
			for(final FLEFRecord child : FLEFRecordHelper.findChildren(r, tag)){
				String id = child.getValue();
				if(id == null || id.isBlank()){
					final FLEFRecord onlyChild = child.getTheOnlyChild();
					id = (onlyChild != null? onlyChild.getValue(): null);
				}
				if(id != null && !id.isBlank())
					ids.add(id);
			}
			for(final String id : ids)
				result.merge(id, 1, Integer::sum);
		}
		return result;
	}


	/* ======================================================================
	 *                          Row models
	 * ====================================================================== */

	/** Row for the conclusion table. */
	public record ConclusionRow(String id, String issue, String proofStatus, int resolvesCount, String narrative){}

	public static ConclusionRow toConclusionRow(final FLEFRecord conclusion){
		final int resolvesCount = conclusion.getChildren().stream()
			.filter(c -> ConclusionReader.TAG_RESOLVES.equalsIgnoreCase(c.getTag()))
			.mapToInt(c -> 1)
			.sum();
		return new ConclusionRow(
			conclusion.getId(),
			conclusionIssue(conclusion),
			proofStatus(conclusion),
			resolvesCount,
			firstTextValue(conclusion, ConclusionReader.TAG_NARRATIVE)
		);
	}

}
