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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * Search strategy for CulturalNorm records.
 * Supports filtering by title, rule type, location, and validity date range.
 */
public class CulturalNormSearchStrategy implements SearchStrategy{

	private static final String TAG_TITLE = "title";
	private static final String TAG_RULE_TYPE = "rule_type";
	private static final String TAG_DATE = "date";

	private static final double FUZZY_THRESHOLD = 0.05;


	private static final CulturalNormHandler HANDLER = CulturalNormHandler.getInstance();

	private String title;
	private String ruleType;
	private String place;
	private String validFrom;
	private String calendarFrom;
	private String validTo;
	private String calendarTo;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		title = criteria.getFilterFor(CulturalNormFilterPanel.FILTER_KEY_TITLE);
		ruleType = criteria.getFilterFor(CulturalNormFilterPanel.FILTER_KEY_RULE_TYPE);
		place = criteria.getFilterFor(CulturalNormFilterPanel.FILTER_KEY_PLACE);
		validFrom = criteria.getFilterFor(CulturalNormFilterPanel.FILTER_KEY_VALID_FROM);
		calendarFrom = criteria.getFilterFor(CulturalNormFilterPanel.FILTER_KEY_CALENDAR_FROM);
		validTo = criteria.getFilterFor(CulturalNormFilterPanel.FILTER_KEY_VALID_TO);
		calendarTo = criteria.getFilterFor(CulturalNormFilterPanel.FILTER_KEY_CALENDAR_FROM);
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return culturalNorm -> {
			// Title filter
			if(StringUtils.isNotEmpty(title)){
				final String title = FLEFRecordHelper.getChildValue(culturalNorm, TAG_TITLE);
				if(!TextSearchHelper.matchesText(title, this.title, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}

			// Rule Type filter
			if(StringUtils.isNotEmpty(ruleType)){
				final String recordRuleType = FLEFRecordHelper.getChildValue(culturalNorm, TAG_RULE_TYPE);
				if(!ruleType.equalsIgnoreCase(recordRuleType))
					return false;
			}

			// Place filter
			if(!SearchHelper.matchesPlace(culturalNorm, place, model, fuzzy, wholeWord, FUZZY_THRESHOLD))
				return false;

			// Date range
			if(StringUtils.isNotEmpty(validFrom) || StringUtils.isNotEmpty(validTo)){
				final FLEFRecord validRecord = FLEFRecordHelper.findChild(culturalNorm, TAG_DATE);
				final Integer fromYear = (StringUtils.isNotEmpty(validFrom)
					? SearchHelper.extractYear(validFrom, calendarFrom)
					: null);
				final Integer toYear = (StringUtils.isNotEmpty(validTo)
					? SearchHelper.extractYear(validTo, calendarTo)
					: null);
				if(!SearchHelper.isDateInRange(validRecord, null, null, fromYear, toYear))
					return false;
			}

			return true;
		};
	}


	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String title = FLEFRecordHelper.getChildValue(record, TAG_TITLE);
		final String ruleType = FLEFRecordHelper.getChildValue(record, TAG_RULE_TYPE);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(title))
			details.add(title);
		if(StringUtils.isNotEmpty(ruleType))
			details.add("Type: " + ruleType);

		return baseDisplayText + details;
	}

}
