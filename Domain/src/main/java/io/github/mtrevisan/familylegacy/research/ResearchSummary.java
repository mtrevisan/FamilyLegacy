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

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;


/**
 * What a list of research records says about each record besides its own text: the question an activity or a task belongs to, how many
 * activities, tasks and conclusions a question has, how many records a conclusion resolves. The lists of the Research menu show it on
 * each line, and the search finds a record by it.
 * <p>
 * The counts need the whole model, so they are indexed once and reused for a short time: describing a thousand questions in a row
 * costs one scan of the model. The model has no change notification, so the index expires after {@link #CACHE_MILLIS} and when the
 * number of records changes.
 */
public final class ResearchSummary{

	private static final long CACHE_MILLIS = 2_000L;
	private static final int MAX_TEXT_LENGTH = 80;


	private record Index(WeakReference<FLEFModel> model, int recordCount, long builtAt, Map<String, Integer> activities,
		Map<String, Integer> tasks, Map<String, Integer> conclusions){}


	private static volatile Index cache;


	private ResearchSummary(){}


	/** Drops the index, so that the next description reads the model again. */
	public static void invalidate(){
		cache = null;
	}

	/**
	 * @param record	A research activity or a research task.
	 * @param model	The model.
	 * @return	The title of the question the record belongs to, the identifier if the question is not in the model, {@code null} if the
	 * 	record belongs to none.
	 */
	public static String questionOf(final FLEFRecord record, final FLEFModel model){
		final String tag = (ResearchTaskReader.isOfType(record)? ResearchTaskReader.TAG_QUESTION: ResearchActivityReader.TAG_QUESTION);
		final String id = ResearchHelper.firstTextValue(record, tag);
		if(StringUtils.isBlank(id))
			return null;

		final FLEFRecord question = model.getRecordById(id);
		return (question != null? ResearchHelper.questionTitle(question): id);
	}

	/**
	 * @param question	A research question.
	 * @param model	The model.
	 * @return	How many activities, tasks and conclusions it has: "3 activities, 1 task, 0 conclusions".
	 */
	public static String counts(final FLEFRecord question, final FLEFModel model){
		final Index index = indexOf(model);
		final String id = question.getId();
		return I18N.tf("text.research.activities", index.activities().getOrDefault(id, 0)) + ", "
			+ I18N.tf("text.research.tasks", index.tasks().getOrDefault(id, 0)) + ", "
			+ I18N.tf("text.research.conclusions", index.conclusions().getOrDefault(id, 0));
	}

	/** @return	"resolves 2 records". */
	public static String resolves(final FLEFRecord conclusion){
		return I18N.tf("text.research.resolves", ResearchHelper.toConclusionRow(conclusion).resolvesCount());
	}

	/** @return	The text on one line, shortened: what is shown of a narrative or of a comment that can be long. */
	public static String shorten(final String text){
		return (StringUtils.isBlank(text)? null: StringUtils.abbreviate(StringUtils.normalizeSpace(text), "\u2026", MAX_TEXT_LENGTH));
	}


	private static Index indexOf(final FLEFModel model){
		final long now = System.currentTimeMillis();
		final Index current = cache;
		if(current != null && current.model().get() == model && current.recordCount() == model.getRecordCount()
				&& now - current.builtAt() < CACHE_MILLIS)
			return current;

		final Map<String, Integer> conclusions = new HashMap<>();
		for(final FLEFRecord conclusion : ResearchHelper.listConclusions(model))
			for(final FLEFRecord reference : FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESEARCH)){
				String questionId = reference.getValue();
				if(StringUtils.isBlank(questionId) && reference.getTheOnlyChild() != null)
					questionId = reference.getTheOnlyChild().getValue();
				if(StringUtils.isNotBlank(questionId))
					conclusions.merge(questionId, 1, Integer::sum);
			}

		final Index index = new Index(new WeakReference<>(model), model.getRecordCount(), now,
			ResearchHelper.countReferences(ResearchHelper.listActivities(model), ResearchActivityReader.TAG_QUESTION),
			ResearchHelper.countReferences(ResearchHelper.listTasks(model), ResearchTaskReader.TAG_QUESTION), conclusions);
		cache = index;
		return index;
	}

}
