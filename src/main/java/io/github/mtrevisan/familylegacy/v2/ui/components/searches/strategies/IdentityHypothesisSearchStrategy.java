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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * Search strategy for IdentityHypothesis records.
 * Supports filtering by target candidate records and hypothesis comments.
 */
public class IdentityHypothesisSearchStrategy implements SearchStrategy{

	private static final String TAG_IDENTITY = "identity";
	private static final String TAG_COMMENT = "comment";

	private static final double FUZZY_THRESHOLD = 0.05;


	private static final IdentityHypothesisHandler HANDLER = IdentityHypothesisHandler.getInstance();


	private String candidate;
	private String comment;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		candidate = criteria.getFilterFor(IdentityHypothesisFilterPanel.FILTER_KEY_CANDIDATE);
		comment = criteria.getFilterFor(IdentityHypothesisFilterPanel.FILTER_KEY_COMMENT);
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return hypothesis -> {
			// Candidate filter (checks resolved display text for candidate records)
			if(StringUtils.isNotEmpty(candidate)){
				final List<FLEFRecord> candidates = FLEFRecordHelper.findChildren(hypothesis, TAG_IDENTITY);
				boolean matched = false;
				for(final FLEFRecord candidate : candidates){
					final String targetRef = candidate.getValue();
					if(targetRef != null){
						final FLEFRecord targetRecord = model.getRecordById(targetRef);
						if(targetRecord != null){
							final String candidateDisplayText = HandlerRegistry.getHandler(targetRecord.getTag())
								.getDisplayText(targetRecord, model);
							if(TextSearchHelper.matchesText(candidateDisplayText, this.candidate, fuzzy, wholeWord, FUZZY_THRESHOLD)){
								matched = true;

								break;
							}
						}
					}
				}
				if(!matched)
					return false;
			}

			// Comment filter
			if(StringUtils.isNotEmpty(comment)){
				final String comment = FLEFRecordHelper.getChildValue(hypothesis, TAG_COMMENT);
				if(!TextSearchHelper.matchesText(comment, this.comment, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String comment = FLEFRecordHelper.getChildValue(record, TAG_COMMENT);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(comment))
			details.add(comment);

		return baseDisplayText + details;
	}

}
