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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HistoricEventHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * Search strategy for HistoricEvent records.
 * Supports filtering by type, title, date, and location.
 */
public class HistoricEventSearchStrategy implements SearchStrategy{

	private static final String TAG_TYPE = "type";
	private static final String TAG_TITLE = "title";
	private static final String TAG_DATE = "date";

	private static final double FUZZY_THRESHOLD = 0.05;


	private static final HistoricEventHandler HANDLER = HistoricEventHandler.getInstance();

	private String type;
	private String title;
	private String date;
	private String calendar;
	private String place;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		type = criteria.getFilterFor(HistoricEventFilterPanel.FILTER_KEY_TYPE);
		title = criteria.getFilterFor(HistoricEventFilterPanel.FILTER_KEY_TITLE);
		date = criteria.getFilterFor(HistoricEventFilterPanel.FILTER_KEY_DATE);
		calendar = criteria.getFilterFor(HistoricEventFilterPanel.FILTER_KEY_CALENDAR);
		place = criteria.getFilterFor(HistoricEventFilterPanel.FILTER_KEY_PLACE);
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return historicEvent -> {
			// Type filter
			if(StringUtils.isNotEmpty(type)){
				final String recordType = FLEFRecordHelper.getChildValue(historicEvent, TAG_TYPE);
				if(!type.equalsIgnoreCase(recordType))
					return false;
			}

			// Title filter
			if(StringUtils.isNotEmpty(title)){
				final String title = FLEFRecordHelper.getChildValue(historicEvent, TAG_TITLE);
				if(!TextSearchHelper.matchesText(title, this.title, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}

			// Date filter
			if(!SearchHelper.matchesDate(historicEvent, date, calendar))
				return false;

			// Place filter
			if(!SearchHelper.matchesPlace(historicEvent, place, model, fuzzy, wholeWord, FUZZY_THRESHOLD))
				return false;

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String title = FLEFRecordHelper.getChildValue(record, TAG_TITLE);
		final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
		final FLEFRecord dateRecord = FLEFRecordHelper.findChild(record, TAG_DATE);
		final String date = FLEFRecordHelper.extractDate(dateRecord);
		final String place = FLEFRecordHelper.extractPlace(record, model);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(title))
			details.add(title);
		if(StringUtils.isNotEmpty(type))
			details.add("Type: " + type);
		if(StringUtils.isNotEmpty(date))
			details.add(date);
		if(StringUtils.isNotEmpty(place))
			details.add(" (" + place + ")");

		return baseDisplayText + details;
	}

}
