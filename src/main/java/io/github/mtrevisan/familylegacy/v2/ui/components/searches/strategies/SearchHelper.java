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
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ParsedGenealogicalDate;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.UniversalDateConverter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;


public class SearchHelper{

	private static final String TAG_DATE = "date";
	private static final String TAG_PLACE = "place";
	private static final String TAG_VALUE = "value";
	private static final String TAG_CALENDAR = "calendar";
	private static final String TAG_POINT = "point";
	private static final String TAG_BOUNDED = "bounded";
	private static final String TAG_NOT_BEFORE = "not_before";
	private static final String TAG_NOT_AFTER = "not_after";
	private static final String TAG_SPANNING = "spanning";
	private static final String TAG_FROM = "from";
	private static final String TAG_TO = "to";
	private static final String TAG_FULL_DATE = "full_date";
	private static final String TAG_DECADE = "decade";
	private static final String TAG_START_YEAR = "start_year";
	private static final String TAG_CENTURY = "century";
	private static final String TAG_ORDINAL = "ordinal";
	private static final String TAG_PART = "part";
	private static final String TAG_NAME = "name";
	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_TYPE = "type";

	private static final String ENUM_TYPE_BIRTH = "birth";
	private static final String ENUM_TYPE_DEATH = "death";
	private static final String ENUM_PART_FIRST_QUARTER = "first_quarter";
	private static final String ENUM_PART_SECOND_QUARTER = "second_quarter";
	private static final String ENUM_PART_THIRD_QUARTER = "third_quarter";
	private static final String ENUM_PART_FOURTH_QUARTER = "fourth_quarter";
	private static final String ENUM_PART_FIRST_HALF = "first_half";
	private static final String ENUM_PART_SECOND_HALF = "second_half";
	private static final String ENUM_PART_EARLY = "early";
	private static final String ENUM_PART_MID = "mid";
	private static final String ENUM_PART_LATE = "late";


	private SearchHelper(){}


	static String joinNonNull(final String delimiter, final String first, final String second){
		if(first != null && second != null)
			return first + delimiter + second;
		return (first != null? first: second);
	}


	static void precomputeLifeBounds(final Map<String, Integer> birthYears, final Map<String, Integer> deathYears,
			final FLEFModel model){
		birthYears.clear();
		deathYears.clear();

		final List<FLEFRecord> participations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(final FLEFRecord participation : participations){
			final String participantId = participation.extractReferencedId(TAG_PARTICIPANT, IndividualHandler.TYPE);
			if(participantId == null)
				continue;

			final String eventId = FLEFRecordHelper.getChildValue(participation, EventHandler.TYPE);
			if(eventId == null)
				continue;

			final FLEFRecord event = model.getRecordById(eventId);
			if(event == null)
				continue;

			final String type = FLEFRecordHelper.getChildValue(event, TAG_TYPE);
			if(type == null)
				continue;

			if(ENUM_TYPE_BIRTH.equalsIgnoreCase(type) && !birthYears.containsKey(participantId)){
				final FLEFRecord eventDateRecord = FLEFRecordHelper.findChild(event, TAG_DATE);
				if(eventDateRecord != null){
					final Integer[] range = SearchHelper.extractYearRangeFromDateStructure(eventDateRecord);
					if(range != null && range[0] != Integer.MIN_VALUE)
						birthYears.put(participantId, range[0]);
				}
			}
			else if(ENUM_TYPE_DEATH.equalsIgnoreCase(type) && !deathYears.containsKey(participantId)){
				final FLEFRecord eventDateRecord = FLEFRecordHelper.findChild(event, TAG_DATE);
				if(eventDateRecord != null){
					final Integer[] range = SearchHelper.extractYearRangeFromDateStructure(eventDateRecord);
					if(range != null && range[1] != Integer.MAX_VALUE)
						deathYears.put(participantId, range[1]);
				}
			}
		}
	}


	public static boolean matchesName(final FLEFRecord place, final String name, final boolean fuzzy,
			final boolean wholeWord, final double fuzzyThreshold){
		if(StringUtils.isEmpty(name))
			return true;

		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(place, TAG_NAME);
		boolean matched = false;
		for(final FLEFRecord nameStruct : names){
			final String nameValue = FLEFRecordHelper.getChildValue(nameStruct, TAG_VALUE);
			if(TextSearchHelper.matchesText(nameValue, name, fuzzy, wholeWord, fuzzyThreshold)){
				matched = true;

				break;
			}
		}
		return matched;
	}

	public static boolean matchesDate(final FLEFRecord event, final String date, final String calendar){
		if(StringUtils.isEmpty(date))
			return true;

		final FLEFRecord dateRecord = FLEFRecordHelper.findChild(event, TAG_DATE);
		final Integer year = SearchHelper.extractYear(date, calendar);
		return SearchHelper.isDateInRange(dateRecord, null, null, year, year);
	}

	public static boolean matchesPlace(final FLEFRecord event, final String targetPlace, final FLEFModel model,
		final boolean fuzzy, final boolean wholeWord, final double fuzzyThreshold){
		if(StringUtils.isEmpty(targetPlace))
			return true;

		final FLEFRecord placeCitation = FLEFRecordHelper.findChild(event, TAG_PLACE);
		if(placeCitation == null)
			return true;

		final String placeId = placeCitation.getTheOnlyChild().getValue();
		final FLEFRecord placeRecord = model.getRecordById(placeId);
		final String place = PlaceHandler.getInstance()
			.getDisplayText(placeRecord, model);
		return TextSearchHelper.matchesText(place, targetPlace, fuzzy, wholeWord, fuzzyThreshold);
	}

	static boolean isDateInRange(final FLEFRecord dateRecord, final Integer birthYear, final Integer deathYear,
			final Integer fromYear, final Integer toYear){
		if(dateRecord == null)
			return false;

		// Extract lower and upper year boundaries of the event date structure
		final Integer[] eventYearRange = extractYearRangeFromDateStructure(dateRecord);
		if(eventYearRange == null)
			return false;

		int eventMinYear = eventYearRange[0];
		int eventMaxYear = eventYearRange[1];
		// Clamp unbounded lower limit to birth year
		if(eventMinYear == Integer.MIN_VALUE && birthYear != null)
			eventMinYear = birthYear;
		// Clamp unbounded upper limit to death year
		if(eventMaxYear == Integer.MAX_VALUE && deathYear != null)
			eventMaxYear = deathYear;

		// Evaluate dateFrom filter boundary
		if(fromYear != null && (eventMaxYear == Integer.MAX_VALUE || eventMaxYear < fromYear))
			return false;

		// Evaluate dateTo filter boundary
		if(toYear != null && (eventMinYear == Integer.MIN_VALUE || eventMinYear > toYear))
			return false;

		return true;
	}

	/**
	 * Extracts [minYear, maxYear] from a DateStructure record protocol.
	 * Returns null if no valid year boundary could be determined.
	 */
	static Integer[] extractYearRangeFromDateStructure(final FLEFRecord dateRecord){
		final FLEFRecord valueRecord = FLEFRecordHelper.findChild(dateRecord, TAG_VALUE);
		final FLEFRecord target = (valueRecord != null? valueRecord: dateRecord);

		// 1. SingleDate point
		final FLEFRecord pointRecord = FLEFRecordHelper.findChild(target, TAG_POINT);
		if(pointRecord != null)
			return extractYearRangeFromSingleDate(pointRecord);

		// 2. BoundedDate (not_before / not_after)
		final FLEFRecord boundedRecord = FLEFRecordHelper.findChild(target, TAG_BOUNDED);
		if(boundedRecord != null){
			final FLEFRecord notBefore = FLEFRecordHelper.findChild(boundedRecord, TAG_NOT_BEFORE);
			final FLEFRecord notAfter = FLEFRecordHelper.findChild(boundedRecord, TAG_NOT_AFTER);
			return extractYearRangeFromBoundPair(notBefore, notAfter);
		}

		// 3. SpanningDate (from / to)
		final FLEFRecord spanningRecord = FLEFRecordHelper.findChild(target, TAG_SPANNING);
		if(spanningRecord != null){
			final FLEFRecord fromRecord = FLEFRecordHelper.findChild(spanningRecord, TAG_FROM);
			final FLEFRecord toRecord = FLEFRecordHelper.findChild(spanningRecord, TAG_TO);
			return extractYearRangeFromBoundPair(fromRecord, toRecord);
		}

		// Direct SingleDate fallback
		return extractYearRangeFromSingleDate(target);
	}

	private static Integer[] extractYearRangeFromBoundPair(final FLEFRecord lowerRecord, final FLEFRecord upperRecord){
		final Integer[] minRange = (lowerRecord != null? extractYearRangeFromSingleDate(lowerRecord): null);
		final Integer[] maxRange = (upperRecord != null? extractYearRangeFromSingleDate(upperRecord): null);

		final int min = (minRange != null? minRange[0]: Integer.MIN_VALUE);
		final int max = (maxRange != null? maxRange[1]: Integer.MAX_VALUE);

		return (min != Integer.MIN_VALUE || max != Integer.MAX_VALUE? new Integer[]{min, max}: null);
	}

	/**
	 * Extracts [minYear, maxYear] from a SingleDate variant (full_date, decade, century).
	 */
	private static Integer[] extractYearRangeFromSingleDate(final FLEFRecord singleDateRecord){
		if(singleDateRecord == null)
			return null;

		// Variant A: full_date
		final FLEFRecord fullDateRecord = FLEFRecordHelper.findChild(singleDateRecord, TAG_FULL_DATE);
		if(fullDateRecord != null){
			final String date = FLEFRecordHelper.getChildValue(fullDateRecord, TAG_VALUE);
			final String calendar = FLEFRecordHelper.getChildValue(fullDateRecord, TAG_CALENDAR);
			final Integer year = extractYear(date, calendar);
			return (year != null? new Integer[]{year, year}: null);
		}

		// Variant B: decade
		final FLEFRecord decadeRecord = FLEFRecordHelper.findChild(singleDateRecord, TAG_DECADE);
		if(decadeRecord != null){
			final String startYearStr = FLEFRecordHelper.getChildValue(decadeRecord, TAG_START_YEAR);
			if(StringUtils.isNumeric(startYearStr)){
				final int startYear = Integer.parseInt(startYearStr);
				return new Integer[]{startYear, startYear + 9};
			}
		}

		// Variant C: century
		final FLEFRecord centuryRecord = FLEFRecordHelper.findChild(singleDateRecord, TAG_CENTURY);
		if(centuryRecord != null){
			final String ordinalStr = FLEFRecordHelper.getChildValue(centuryRecord, TAG_ORDINAL);
			if(StringUtils.isNumeric(ordinalStr)){
				final int ordinal = Integer.parseInt(ordinalStr);
				final int startYear = (ordinal - 1) * 100 + 1;
				final int endYear = ordinal * 100;
				final String part = FLEFRecordHelper.getChildValue(centuryRecord, TAG_PART);

				return calculateCenturyPartRange(startYear, endYear, part);
			}
		}

		return null;
	}

	/**
	 * Resolves century subdivisions (CenturyPart enum) into specific year boundaries.
	 */
	private static Integer[] calculateCenturyPartRange(final int startYear, final int endYear, final String part){
		if(StringUtils.isEmpty(part))
			return new Integer[]{startYear, endYear};

		return switch(part.toLowerCase()){
			case ENUM_PART_FIRST_QUARTER -> new Integer[]{startYear, startYear + 24};
			case ENUM_PART_SECOND_QUARTER -> new Integer[]{startYear + 25, startYear + 49};
			case ENUM_PART_THIRD_QUARTER -> new Integer[]{startYear + 50, startYear + 74};
			case ENUM_PART_FOURTH_QUARTER -> new Integer[]{startYear + 75, endYear};
			case ENUM_PART_FIRST_HALF -> new Integer[]{startYear, startYear + 49};
			case ENUM_PART_SECOND_HALF -> new Integer[]{startYear + 50, endYear};
			case ENUM_PART_EARLY -> new Integer[]{startYear, startYear + 32};
			case ENUM_PART_MID -> new Integer[]{startYear + 33, startYear + 66};
			case ENUM_PART_LATE -> new Integer[]{startYear + 67, endYear};
			default -> new Integer[]{startYear, endYear};
		};
	}

	static Integer extractYear(final String dateStr, final String calendarStr){
		if(StringUtils.isEmpty(dateStr))
			return null;

		final ParsedGenealogicalDate parsedDate = UniversalDateConverter.parse(calendarStr, dateStr);
		return (parsedDate.isoDate() != null? parsedDate.isoDate().getYear(): null);
	}

}
