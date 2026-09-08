package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
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
	private static final String TAG_PLACE = "place";

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
			if(StringUtils.isNotEmpty(date)){
				final FLEFRecord dateRecord = FLEFRecordHelper.findChild(historicEvent, TAG_DATE);
				final Integer year = (StringUtils.isNotEmpty(date)
					? SearchHelper.extractYear(date, calendar)
					: null);
				if(!SearchHelper.isDateInRange(dateRecord, null, null, year, year))
					return false;
			}

			// Place filter
			if(StringUtils.isNotEmpty(place)){
				final FLEFRecord placeCitation = FLEFRecordHelper.findChild(historicEvent, TAG_PLACE);
				if(placeCitation != null){
					final String placeId = placeCitation.getTheOnlyChild().getValue();
					final FLEFRecord placeRecord = model.getRecordById(placeId);
					final String place = PlaceHandler.getInstance()
						.getDisplayText(placeRecord, model);
					if(!TextSearchHelper.matchesText(place, place, fuzzy, wholeWord, FUZZY_THRESHOLD))
						return false;
				}
			}

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String title = FLEFRecordHelper.getChildValue(record, TAG_TITLE);
		final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
		final FLEFRecord dateRecord = FLEFRecordHelper.findChild(record, TAG_DATE);
		final String date = SearchHelper.extractDate(dateRecord);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(title))
			details.add(title);
		if(StringUtils.isNotEmpty(type))
			details.add("Type: " + type);
		if(StringUtils.isNotEmpty(date))
			details.add(date);

		return baseDisplayText + details;
	}

}
