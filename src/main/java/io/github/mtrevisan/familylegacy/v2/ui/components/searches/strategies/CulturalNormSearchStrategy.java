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
import io.github.mtrevisan.familylegacy.v2.io.model.readers.CulturalNormReader;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.DateReader;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchMode;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * Search strategy for CulturalNorm records.
 * Supports filtering by title, rule type, place, and validity date range.
 */
public class CulturalNormSearchStrategy implements SearchStrategy{

	static final String KEY_CALENDAR_FROM = DateReader.TAG_CALENDAR + "_" + DateReader.TAG_FROM;
	static final String KEY_CALENDAR_TO = DateReader.TAG_CALENDAR + "_" + DateReader.TAG_TO;


	private static final double FUZZY_THRESHOLD = 0.05;


	private static final CulturalNormHandler HANDLER = CulturalNormHandler.getInstance();

	private String title;
	private String type;
	private String place;
	private String validFrom;
	private String calendarFrom;
	private String validTo;
	private String calendarTo;
	private SearchMode mode;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		title = criteria.getFilterFor(CulturalNormReader.TAG_TITLE);
		type = criteria.getFilterFor(CulturalNormReader.TAG_TYPE);
		place = criteria.getFilterFor(CulturalNormReader.TAG_PLACE);
		validFrom = criteria.getFilterFor(CulturalNormReader.TAG_VALID_FROM);
		calendarFrom = criteria.getFilterFor(KEY_CALENDAR_FROM);
		validTo = criteria.getFilterFor(CulturalNormReader.TAG_VALID_TO);
		calendarTo = criteria.getFilterFor(KEY_CALENDAR_TO);
		mode = criteria.mode();

		return culturalNorm -> {
			// Title filter
			if(StringUtils.isNotEmpty(title)){
				final String recordTitle = CulturalNormReader.extractTitle(culturalNorm);
				if(!SearchHelper.matches(recordTitle, title, mode))
					return false;
			}

			// Rule Type filter
			if(StringUtils.isNotEmpty(type)){
				final String typeRecord = CulturalNormReader.extractType(culturalNorm);
				if(!type.equalsIgnoreCase(typeRecord))
					return false;
			}

			// Place filter
			if(!SearchHelper.matchesPlace(culturalNorm, place, model, mode, FUZZY_THRESHOLD))
				return false;

			// Date range
			if(StringUtils.isNotEmpty(validFrom) || StringUtils.isNotEmpty(validTo)){
				final FLEFRecord validFromRecord = FLEFRecordHelper.findChild(culturalNorm, CulturalNormReader.TAG_VALID_FROM);
				final FLEFRecord validToRecord = FLEFRecordHelper.findChild(culturalNorm, CulturalNormReader.TAG_VALID_TO);
				final Integer fromYear = (StringUtils.isNotEmpty(validFrom)
					? SearchHelper.extractYear(validFrom, calendarFrom)
					: null);
				final Integer toYear = (StringUtils.isNotEmpty(validTo)
					? SearchHelper.extractYear(validTo, calendarTo)
					: null);
				if(fromYear != null
						&& !SearchHelper.isDateInRange(validFromRecord, null, null, fromYear, null))
					return false;
				if(toYear != null
						&& !SearchHelper.isDateInRange(validToRecord, null, null, null, toYear))
					return false;
			}

			return true;
		};
	}


	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String title = CulturalNormReader.extractTitle(record);
		final String type = CulturalNormReader.extractType(record);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(title))
			details.add(title);
		if(StringUtils.isNotEmpty(type))
			details.add(I18N.t("dialog.cultural.norm.rule.type") + ": " + type);

		return baseDisplayText + details;
	}

}
