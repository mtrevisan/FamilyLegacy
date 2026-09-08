package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ParsedGenealogicalDate;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.UniversalDateConverter;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;
import java.util.function.Predicate;


/* TODO */
/**
 * Search strategy for ResearchActivity records.
 * Supports filtering by activity type, date range, and location.
 */
public class ResearchActivitySearchStrategy implements SearchStrategy{

	private static final String DOT = ".";

	private static final String TAG_TYPE = "type";
	private static final String TAG_DATE = "date";
	private static final String TAG_PLACE = "place";
	private static final String TAG_VALUE = "value";
	private static final String TAG_POINT = "point";
	private static final String TAG_BOUNDED = "bounded";
	private static final String TAG_NOT_BEFORE = "not_before";
	private static final String TAG_NOT_AFTER = "not_after";
	private static final String TAG_SPANNING = "spanning";
	private static final String TAG_FROM = "from";
	private static final String TAG_TO = "to";
	private static final String TAG_FULL_DATE = "full_date";
	private static final String TAG_CALENDAR = "calendar";
	private static final String TAG_DECADE = "decade";
	private static final String TAG_START_YEAR = "start_year";
	private static final String TAG_CENTURY = "century";
	private static final String TAG_ORDINAL = "ordinal";
	private static final String TAG_PART = "part";
	private static final String TAG_ORIGINAL_TEXT = "original_text";

	private static final String TAG_PLACE_PLACE = TAG_PLACE + DOT + TAG_PLACE;
	private static final String TAG_DATE_ORIGINAL_TEXT = TAG_DATE + DOT + TAG_ORIGINAL_TEXT;
	private static final String TAG_DATE_VALUE_BOUNDED_NOT_BEFORE = TAG_DATE + DOT + TAG_VALUE + DOT + TAG_BOUNDED + DOT + TAG_NOT_BEFORE;
	private static final String TAG_DATE_VALUE_BOUNDED_NOT_AFTER = TAG_DATE + DOT + TAG_VALUE + DOT + TAG_BOUNDED + DOT + TAG_NOT_AFTER;
	private static final String TAG_DATE_VALUE_SPANNING_FROM = TAG_DATE + DOT + TAG_VALUE + DOT + TAG_SPANNING + DOT + TAG_FROM;
	private static final String TAG_DATE_VALUE_SPANNING_TO = TAG_DATE + DOT + TAG_VALUE + DOT + TAG_SPANNING + DOT + TAG_TO;
	private static final String TAG_FULL_DATE_VALUE = TAG_FULL_DATE + DOT + TAG_VALUE;
	private static final String TAG_DECADE_START_YEAR = TAG_DECADE + DOT + TAG_START_YEAR;
	private static final String TAG_CENTURY_ORDINAL = TAG_CENTURY + DOT + TAG_ORDINAL;
	private static final String TAG_CENTURY_PART = TAG_CENTURY + DOT + TAG_PART;

	private static final double FUZZY_THRESHOLD = 0.05;

	private static final ResearchActivityHandler HANDLER = ResearchActivityHandler.getInstance();

	private String activityType;
	private String dateFrom;
	private String calendarFrom;
	private String dateTo;
	private String calendarTo;
	private String locationContains;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		activityType = criteria.getFilterFor("activityType");
		dateFrom = criteria.getFilterFor("dateFrom");
		calendarFrom = criteria.getFilterFor("calendarFrom");
		dateTo = criteria.getFilterFor("dateTo");
		calendarTo = criteria.getFilterFor("calendarTo");
		locationContains = criteria.getFilterFor("locationContains");
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return activity -> {
			// Activity type filter
			if(StringUtils.isNotEmpty(activityType)){
				final String recordType = FLEFRecordHelper.getChildValue(activity, TAG_TYPE);
				if(!activityType.equalsIgnoreCase(recordType)){
					return false;
				}
			}

			// Date range filter
			if(StringUtils.isNotEmpty(dateFrom) || StringUtils.isNotEmpty(dateTo)){
				final FLEFRecord dateRecord = FLEFRecordHelper.findChild(activity, TAG_DATE);
				if(!isDateInRange(dateRecord)){
					return false;
				}
			}

			// Location filter
			if(StringUtils.isNotEmpty(locationContains)){
				final String placeRef = FLEFRecordHelper.getChildValue(activity, TAG_PLACE_PLACE);
				if(placeRef != null){
					final FLEFRecord placeRecord = model.getRecordById(placeRef);
					if(placeRecord != null){
						final String placeDisplayText = PlaceHandler.getInstance().getDisplayText(placeRecord, model);
						if(!TextSearchHelper.matchesText(placeDisplayText, locationContains, fuzzy, wholeWord, FUZZY_THRESHOLD)){
							return false;
						}
					}
					else{
						return false;
					}
				}
				else{
					return false;
				}
			}

			return true;
		};
	}

	private boolean isDateInRange(final FLEFRecord dateRecord){
		if(dateRecord == null){
			return false;
		}

		final Integer[] yearRange = extractYearRangeFromDateStructure(dateRecord);
		if(yearRange == null){
			return false;
		}

		final int activityMinYear = yearRange[0];
		final int activityMaxYear = yearRange[1];

		if(StringUtils.isNotEmpty(dateFrom)){
			final Integer fromYear = extractYear(dateFrom, calendarFrom);
			if(fromYear != null && (activityMaxYear == Integer.MAX_VALUE || activityMaxYear < fromYear)){
				return false;
			}
		}

		if(StringUtils.isNotEmpty(dateTo)){
			final Integer toYear = extractYear(dateTo, calendarTo);
			if(toYear != null && (activityMinYear == Integer.MIN_VALUE || activityMinYear > toYear)){
				return false;
			}
		}

		return true;
	}

	private Integer[] extractYearRangeFromDateStructure(final FLEFRecord dateRecord){
		final FLEFRecord valueRecord = FLEFRecordHelper.findChild(dateRecord, TAG_VALUE);
		final FLEFRecord target = (valueRecord != null ? valueRecord : dateRecord);

		final FLEFRecord pointRecord = FLEFRecordHelper.findChild(target, TAG_POINT);
		if(pointRecord != null){
			return extractYearRangeFromSingleDate(pointRecord);
		}

		final FLEFRecord boundedRecord = FLEFRecordHelper.findChild(target, TAG_BOUNDED);
		if(boundedRecord != null){
			final FLEFRecord notBefore = FLEFRecordHelper.findChild(boundedRecord, TAG_NOT_BEFORE);
			final FLEFRecord notAfter = FLEFRecordHelper.findChild(boundedRecord, TAG_NOT_AFTER);

			final Integer[] minRange = (notBefore != null ? extractYearRangeFromSingleDate(notBefore) : null);
			final Integer[] maxRange = (notAfter != null ? extractYearRangeFromSingleDate(notAfter) : null);

			final int min = (minRange != null ? minRange[0] : Integer.MIN_VALUE);
			final int max = (maxRange != null ? maxRange[1] : Integer.MAX_VALUE);

			return (min != Integer.MIN_VALUE || max != Integer.MAX_VALUE ? new Integer[]{min, max} : null);
		}

		final FLEFRecord spanningRecord = FLEFRecordHelper.findChild(target, TAG_SPANNING);
		if(spanningRecord != null){
			final FLEFRecord fromRecord = FLEFRecordHelper.findChild(spanningRecord, TAG_FROM);
			final FLEFRecord toRecord = FLEFRecordHelper.findChild(spanningRecord, TAG_TO);

			final Integer[] minRange = (fromRecord != null ? extractYearRangeFromSingleDate(fromRecord) : null);
			final Integer[] maxRange = (toRecord != null ? extractYearRangeFromSingleDate(toRecord) : null);

			final int min = (minRange != null ? minRange[0] : Integer.MIN_VALUE);
			final int max = (maxRange != null ? maxRange[1] : Integer.MAX_VALUE);

			return (min != Integer.MIN_VALUE || max != Integer.MAX_VALUE ? new Integer[]{min, max} : null);
		}

		return extractYearRangeFromSingleDate(target);
	}

	private Integer[] extractYearRangeFromSingleDate(final FLEFRecord singleDateRecord){
		if(singleDateRecord == null){
			return null;
		}

		final FLEFRecord fullDateRecord = FLEFRecordHelper.findChild(singleDateRecord, TAG_FULL_DATE);
		if(fullDateRecord != null){
			final String dateVal = FLEFRecordHelper.getChildValue(fullDateRecord, TAG_VALUE);
			final String calendar = FLEFRecordHelper.getChildValue(fullDateRecord, TAG_CALENDAR);
			final Integer year = extractYear(dateVal, calendar);
			return (year != null ? new Integer[]{year, year} : null);
		}

		final FLEFRecord decadeRecord = FLEFRecordHelper.findChild(singleDateRecord, TAG_DECADE);
		if(decadeRecord != null){
			final String startYearStr = FLEFRecordHelper.getChildValue(decadeRecord, TAG_START_YEAR);
			if(StringUtils.isNumeric(startYearStr)){
				final int startYear = Integer.parseInt(startYearStr);
				return new Integer[]{startYear, startYear + 9};
			}
		}

		final FLEFRecord centuryRecord = FLEFRecordHelper.findChild(singleDateRecord, TAG_CENTURY);
		if(centuryRecord != null){
			final String ordinalStr = FLEFRecordHelper.getChildValue(centuryRecord, TAG_ORDINAL);
			if(StringUtils.isNumeric(ordinalStr)){
				final int ordinal = Integer.parseInt(ordinalStr);
				final int startYear = (ordinal - 1) * 100 + 1;
				final int endYear = ordinal * 100;
				return new Integer[]{startYear, endYear};
			}
		}

		return null;
	}

	private Integer extractYear(final String dateStr, final String calendarStr){
		if(StringUtils.isEmpty(dateStr)){
			return null;
		}

		final ParsedGenealogicalDate parsedDate = UniversalDateConverter.parse(calendarStr, dateStr);
		return (parsedDate.isoDate() != null ? parsedDate.isoDate().getYear() : null);
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
		final String date = SearchHelper.extractDate(record);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(type)){
			details.add(type);
		}
		if(StringUtils.isNotEmpty(date)){
			details.add("📅 " + date);
		}

		return baseDisplayText + details;
	}

}
