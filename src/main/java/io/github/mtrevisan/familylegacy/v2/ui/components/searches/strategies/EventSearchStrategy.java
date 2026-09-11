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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * Search strategy for Event records.
 * Supports filtering by event type, description, date, location, agency, and cause reason.
 */
public class EventSearchStrategy implements SearchStrategy{

	private static final String DOT = ".";

	private static final String TAG_TYPE = "type";
	private static final String TAG_DESCRIPTION = "description";
	private static final String TAG_DATE = "date";
	private static final String TAG_AGENCY = "agency";
	private static final String TAG_CAUSE = "cause";
	private static final String TAG_REASON = "reason";
	private static final String TAG_CAUSE_REASON = TAG_CAUSE + DOT + TAG_REASON;

	private static final double FUZZY_THRESHOLD = 0.05;


	private static final EventHandler HANDLER = EventHandler.getInstance();


	private String eventType;
	private String description;
	private String date;
	private String calendar;
	private String location;
	private String agency;
	private String causeReason;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		eventType = criteria.getFilterFor(EventFilterPanel.FILTER_KEY_EVENT_TYPE);
		description = criteria.getFilterFor(EventFilterPanel.FILTER_KEY_DESCRIPTION);
		date = criteria.getFilterFor(EventFilterPanel.FILTER_KEY_DATE);
		calendar = criteria.getFilterFor(EventFilterPanel.FILTER_KEY_CALENDAR);
		location = criteria.getFilterFor(EventFilterPanel.FILTER_KEY_LOCATION);
		agency = criteria.getFilterFor(EventFilterPanel.FILTER_KEY_AGENCY);
		causeReason = criteria.getFilterFor(EventFilterPanel.FILTER_KEY_CAUSE_REASON);
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return event -> {
			// Event Type filter
			if(StringUtils.isNotEmpty(eventType)){
				final String type = FLEFRecordHelper.getChildValue(event, TAG_TYPE);
				if(!eventType.equalsIgnoreCase(type))
					return false;
			}

			// Description filter
			if(StringUtils.isNotEmpty(description)){
				final String description = FLEFRecordHelper.getChildValue(event, TAG_DESCRIPTION);
				if(!TextSearchHelper.matchesText(description, this.description, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}

			// Date filter
			if(!SearchHelper.matchesDate(event, date, calendar))
				return false;

			// Location filter
			if(!SearchHelper.matchesPlace(event, location, model, fuzzy, wholeWord, FUZZY_THRESHOLD))
				return false;

			// Agency filter
			if(StringUtils.isNotEmpty(agency)){
				final String agency = FLEFRecordHelper.getChildValue(event, TAG_AGENCY);
				if(!TextSearchHelper.matchesText(agency, this.agency, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}

			// Cause Reason filter
			if(StringUtils.isNotEmpty(causeReason)){
				final String reason = FLEFRecordHelper.getChildValue(event, TAG_CAUSE_REASON);
				if(!TextSearchHelper.matchesText(reason, causeReason, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
		final FLEFRecord dateRecord = FLEFRecordHelper.findChild(record, TAG_DATE);
		final String date = FLEFRecordHelper.extractDate(dateRecord);
		final String place = FLEFRecordHelper.extractPlace(record, model);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(type))
			details.add("Type: " + type);
		if(StringUtils.isNotEmpty(date))
			details.add(date);
		if(StringUtils.isNotEmpty(place))
			details.add(place);

		return baseDisplayText + details;
	}

}
