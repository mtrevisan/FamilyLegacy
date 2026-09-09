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
package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * Search strategy for ResearchQuestion records.
 * Supports filtering by title, question text, status, and conclusion confidence.
 */
public class ResearchQuestionSearchStrategy implements SearchStrategy{

	private static final String TAG_TITLE = "title";
	private static final String TAG_QUESTION = "question";
	private static final String TAG_STATUS = "status";
	private static final String TAG_CONFIDENCE = "conclusion_confidence";

	private static final double FUZZY_THRESHOLD = 0.05;

	private static final ResearchQuestionHandler HANDLER = ResearchQuestionHandler.getInstance();

	private String title;
	private String question;
	private String status;
	private String confidence;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		title = criteria.getFilterFor(ResearchQuestionFilterPanel.FILTER_KEY_TITLE);
		question = criteria.getFilterFor(ResearchQuestionFilterPanel.FILTER_KEY_QUESTION);
		status = criteria.getFilterFor(ResearchQuestionFilterPanel.FILTER_KEY_STATUS);
		confidence = criteria.getFilterFor(ResearchQuestionFilterPanel.FILTER_KEY_CONFIDENCE);
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return question -> {
			// Title filter
			if(StringUtils.isNotEmpty(title)){
				final String title = FLEFRecordHelper.getChildValue(question, TAG_TITLE);
				if(!TextSearchHelper.matchesText(title, this.title, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}

			// Question text filter
			if(StringUtils.isNotEmpty(this.question)){
				final String questionText = FLEFRecordHelper.getChildValue(question, TAG_QUESTION);
				if(!TextSearchHelper.matchesText(questionText, this.question, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}

			// Status filter
			if(StringUtils.isNotEmpty(status)){
				final String recordStatus = FLEFRecordHelper.getChildValue(question, TAG_STATUS);
				if(!status.equalsIgnoreCase(recordStatus))
					return false;
			}

			// Conclusion confidence filter
			if(StringUtils.isNotEmpty(confidence)){
				final String recordConfidence = FLEFRecordHelper.getChildValue(question, TAG_CONFIDENCE);
				if(!confidence.equalsIgnoreCase(recordConfidence))
					return false;
			}

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String status = FLEFRecordHelper.getChildValue(record, TAG_STATUS);
		final String confidence = FLEFRecordHelper.getChildValue(record, TAG_CONFIDENCE);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(status))
			details.add("Status: " + status);
		if(StringUtils.isNotEmpty(confidence))
			details.add("Confidence: " + confidence);

		return baseDisplayText + details;
	}

}
