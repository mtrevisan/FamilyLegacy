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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * Search strategy for Conclusion records.
 * Supports filtering by issue, proof status, narrative text, and linked research questions.
 */
public class ConclusionSearchStrategy implements SearchStrategy{

	private static final String TAG_ISSUE = "issue";
	private static final String TAG_PROOF_STATUS = "proof_status";
	private static final String TAG_NARRATIVE = "narrative";
	private static final String TAG_RESEARCH = "research";

	private static final double FUZZY_THRESHOLD = 0.05;

	private static final ConclusionHandler HANDLER = ConclusionHandler.getInstance();

	private String issue;
	private String proofStatus;
	private String narrative;
	private String researchQuestion;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		issue = criteria.getFilterFor(ConclusionFilterPanel.FILTER_KEY_ISSUE);
		proofStatus = criteria.getFilterFor(ConclusionFilterPanel.FILTER_KEY_PROOF_STATUS);
		narrative = criteria.getFilterFor(ConclusionFilterPanel.FILTER_KEY_NARRATIVE);
		researchQuestion = criteria.getFilterFor(ConclusionFilterPanel.FILTER_KEY_RESEARCH_QUESTION);
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return conclusion -> {
			// Issue filter
			if(StringUtils.isNotEmpty(issue)){
				final String recordIssue = FLEFRecordHelper.getChildValue(conclusion, TAG_ISSUE);
				if(!TextSearchHelper.matchesText(recordIssue, issue, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}

			// Proof Status filter
			if(StringUtils.isNotEmpty(proofStatus)){
				final String recordStatus = FLEFRecordHelper.getChildValue(conclusion, TAG_PROOF_STATUS);
				if(!proofStatus.equalsIgnoreCase(recordStatus))
					return false;
			}

			// Narrative filter
			if(StringUtils.isNotEmpty(narrative)){
				final String recordNarrative = FLEFRecordHelper.getChildValue(conclusion, TAG_NARRATIVE);
				if(!TextSearchHelper.matchesText(recordNarrative, narrative, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}

			// Linked Research Questions filter
			if(StringUtils.isNotEmpty(researchQuestion)){
				final List<FLEFRecord> researchRefs = FLEFRecordHelper.findChildren(conclusion, TAG_RESEARCH);
				boolean matched = false;
				for(final FLEFRecord researchRef : researchRefs){
					final String questionRef = researchRef.getValue();
					if(questionRef != null)
						continue;

					final FLEFRecord questionRecord = model.getRecordById(questionRef);
					if(questionRecord == null)
						continue;

					final String questionDisplayText = ResearchQuestionHandler.getInstance()
						.getDisplayText(questionRecord, model);
					if(TextSearchHelper.matchesText(questionDisplayText, researchQuestion, fuzzy, wholeWord,
							FUZZY_THRESHOLD)){
						matched = true;

						break;
					}
				}
				if(!matched)
					return false;
			}

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String issue = FLEFRecordHelper.getChildValue(record, TAG_ISSUE);
		final String status = FLEFRecordHelper.getChildValue(record, TAG_PROOF_STATUS);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(issue))
			details.add("Issue: " + issue);
		if(StringUtils.isNotEmpty(status))
			details.add("Status: " + status);

		return baseDisplayText + details;
	}

}
