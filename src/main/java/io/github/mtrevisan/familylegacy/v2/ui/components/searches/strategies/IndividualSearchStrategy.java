package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ParsedGenealogicalDate;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.UniversalDateConverter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * Search strategy for Individual records.
 * Supports event‑based filters: sex, eventType, dateFrom, dateTo, locationContains.
 */
public class IndividualSearchStrategy implements SearchStrategy{

	private static final String DOT = ".";

	private static final String TAG_SEX = "sex";
	private static final String TAG_TYPE = "type";
	private static final String TAG_DATE = "date";
	private static final String TAG_PLACE = "place";
	private static final String TAG_PARTICIPANT = "participant";
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
	private static final String TAG_INDIVIDUAL = "individual";
	private static final String TAG_EVENT = "event";
	private static final String TAG_PARTICIPANT_INDIVIDUAL = TAG_PARTICIPANT + DOT + TAG_INDIVIDUAL;

	private static final String ENUM_PART_FIRST_QUARTER = "first_quarter";
	private static final String ENUM_PART_SECOND_QUARTER = "second_quarter";
	private static final String ENUM_PART_THIRD_QUARTER = "third_quarter";
	private static final String ENUM_PART_FOURTH_QUARTER = "fourth_quarter";
	private static final String ENUM_PART_FIRST_HALF = "first_half";
	private static final String ENUM_PART_SECOND_HALF = "second_half";
	private static final String ENUM_PART_EARLY = "early";
	private static final String ENUM_PART_MID = "mid";
	private static final String ENUM_PART_LATE = "late";
	private static final String ENUM_TYPE_BIRTH = "birth";
	private static final String ENUM_TYPE_DEATH = "death";

	private static final double FUZZY_THRESHOLD = 0.05;

	private static final IndividualHandler HANDLER = IndividualHandler.getInstance();


	private String sex;
	private String eventType;
	private String dateFrom;
	private String calendarFrom;
	private String dateTo;
	private String calendarTo;
	private String locationContains;
	private boolean fuzzy;
	private boolean wholeWord;

	private FLEFModel model;

	private final Map<String, Integer> birthYears = new HashMap<>();
	private final Map<String, Integer> deathYears = new HashMap<>();


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		sex = criteria.getFilterFor("sex");
		eventType = criteria.getFilterFor("eventType");
		dateFrom = criteria.getFilterFor("dateFrom");
		calendarFrom = criteria.getFilterFor("calendarFrom");
		dateTo = criteria.getFilterFor("dateTo");
		calendarTo = criteria.getFilterFor("calendarTo");
		locationContains = criteria.getFilterFor("locationContains");
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		this.model = model;

		precomputeLifeBounds();

		final boolean hasEventFilters = (StringUtils.isNotEmpty(eventType) || StringUtils.isNotEmpty(dateFrom)
			|| StringUtils.isNotEmpty(dateTo) || StringUtils.isNotEmpty(locationContains));

		return individual -> {
			if(StringUtils.isNotEmpty(sex)){
				final String recordSex = FLEFRecordHelper.getChildValue(individual, TAG_SEX);
				if(!sex.equalsIgnoreCase(recordSex))
					return false;
			}

			if(hasEventFilters)
				return hasMatchingEvent(individual);

			return true;
		};
	}

	private void precomputeLifeBounds(){
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
					final Integer[] range = extractYearRangeFromDateStructure(eventDateRecord);
					if(range != null && range[0] != Integer.MIN_VALUE)
						birthYears.put(participantId, range[0]);
				}
			}
			else if(ENUM_TYPE_DEATH.equalsIgnoreCase(type) && !deathYears.containsKey(participantId)){
				final FLEFRecord eventDateRecord = FLEFRecordHelper.findChild(event, TAG_DATE);
				if(eventDateRecord != null){
					final Integer[] range = extractYearRangeFromDateStructure(eventDateRecord);
					if(range != null && range[1] != Integer.MAX_VALUE)
						deathYears.put(participantId, range[1]);
				}
			}
		}
	}

	private boolean hasMatchingEvent(final FLEFRecord individual){
		final String individualId = individual.getId();

		final Integer birthYear = birthYears.get(individualId);
		final Integer deathYear = deathYears.get(individualId);

		// Iterate over event participations where this individual is the participant
		final List<FLEFRecord> participations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(final FLEFRecord participation : participations){
			final String participantId = participation.extractReferencedId(TAG_PARTICIPANT, IndividualHandler.TYPE);
			if(!individual.getId().equals(participantId))
				continue;

			final String eventId = FLEFRecordHelper.getChildValue(participation, EventHandler.TYPE);
			if(eventId == null)
				continue;

			final FLEFRecord event = model.getRecordById(eventId);
			if(event == null)
				continue;

			if(matchesEvent(event, birthYear, deathYear))
				return true;
		}
		return false;
	}

	private boolean matchesEvent(final FLEFRecord event, final Integer birthYear, final Integer deathYear){
		// Event type
		if(StringUtils.isNotEmpty(eventType)){
			final String type = FLEFRecordHelper.getChildValue(event, TAG_TYPE);
			if(!eventType.equalsIgnoreCase(type))
				return false;
		}

		// Date range
		if(StringUtils.isNotEmpty(dateFrom) || StringUtils.isNotEmpty(dateTo)){
			final FLEFRecord eventDateRecord = FLEFRecordHelper.findChild(event, TAG_DATE);
			if(!isDateInRange(eventDateRecord, birthYear, deathYear))
				return false;
		}

		// Location
		if(StringUtils.isNotEmpty(locationContains)){
			final FLEFRecord placeCitation = FLEFRecordHelper.findChild(event, TAG_PLACE);
			if(placeCitation != null){
				final String placeId = placeCitation.getTheOnlyChild().getValue();
				final FLEFRecord placeRecord = model.getRecordById(placeId);
				final String place = PlaceHandler.getInstance().getDisplayText(placeRecord, model);
				if(!TextSearchHelper.matchesText(place, locationContains, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}
		}

		return true;
	}

	private boolean isDateInRange(final FLEFRecord dateRecord, final Integer birthYear, final Integer deathYear){
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
		if(StringUtils.isNotEmpty(dateFrom)){
			final Integer fromYear = extractYear(dateFrom, calendarFrom);
			if(fromYear != null && (eventMaxYear == Integer.MAX_VALUE || eventMaxYear < fromYear))
				return false;
		}

		// Evaluate dateTo filter boundary
		if(StringUtils.isNotEmpty(dateTo)){
			final Integer toYear = extractYear(dateTo, calendarTo);
			if(toYear != null && (eventMinYear == Integer.MIN_VALUE || eventMinYear > toYear))
				return false;
		}

		return true;
	}

	/**
	 * Extracts [minYear, maxYear] from a DateStructure record protocol.
	 * Returns null if no valid year boundary could be determined.
	 */
	private Integer[] extractYearRangeFromDateStructure(final FLEFRecord dateRecord){
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

			final Integer[] minRange = (notBefore != null? extractYearRangeFromSingleDate(notBefore): null);
			final Integer[] maxRange = (notAfter != null? extractYearRangeFromSingleDate(notAfter): null);

			final int min = (minRange != null? minRange[0]: Integer.MIN_VALUE);
			final int max = (maxRange != null? maxRange[1]: Integer.MAX_VALUE);

			return (min != Integer.MIN_VALUE || max != Integer.MAX_VALUE? new Integer[]{min, max}: null);
		}

		// 3. SpanningDate (from / to)
		final FLEFRecord spanningRecord = FLEFRecordHelper.findChild(target, TAG_SPANNING);
		if(spanningRecord != null){
			final FLEFRecord fromRecord = FLEFRecordHelper.findChild(spanningRecord, TAG_FROM);
			final FLEFRecord toRecord = FLEFRecordHelper.findChild(spanningRecord, TAG_TO);

			final Integer[] minRange = (fromRecord != null? extractYearRangeFromSingleDate(fromRecord): null);
			final Integer[] maxRange = (toRecord != null? extractYearRangeFromSingleDate(toRecord): null);

			final int min = (minRange != null? minRange[0]: Integer.MIN_VALUE);
			final int max = (maxRange != null? maxRange[1]: Integer.MAX_VALUE);

			return (min != Integer.MIN_VALUE || max != Integer.MAX_VALUE? new Integer[]{min, max}: null);
		}

		// Direct SingleDate fallback
		return extractYearRangeFromSingleDate(target);
	}

	/**
	 * Extracts [minYear, maxYear] from a SingleDate variant (full_date, decade, century).
	 */
	private Integer[] extractYearRangeFromSingleDate(final FLEFRecord singleDateRecord){
		if(singleDateRecord == null)
			return null;

		// Variant A: full_date
		final FLEFRecord fullDateRecord = FLEFRecordHelper.findChild(singleDateRecord, TAG_FULL_DATE);
		if(fullDateRecord != null){
			final String dateVal = FLEFRecordHelper.getChildValue(fullDateRecord, TAG_VALUE);
			final String calendar = FLEFRecordHelper.getChildValue(fullDateRecord, TAG_CALENDAR);
			final Integer year = extractYear(dateVal, calendar);
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
	private Integer[] calculateCenturyPartRange(final int startYear, final int endYear, final String part){
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

	private Integer extractYear(final String dateStr, final String calendarStr){
		if(StringUtils.isEmpty(dateStr))
			return null;

		final ParsedGenealogicalDate parsedDate = UniversalDateConverter.parse(calendarStr, dateStr);
		return (parsedDate.isoDate() != null? parsedDate.isoDate().getYear(): null);
	}


	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		String birthDate = null;
		String birthPlace = null;
		String deathDate = null;
		String deathPlace = null;

		final String indId = record.getId();
		final List<FLEFRecord> participations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(final FLEFRecord participation : participations){
			final String participantRef = FLEFRecordHelper.getChildValue(participation, TAG_PARTICIPANT_INDIVIDUAL);
			if(!Objects.equals(participantRef, indId))
				continue;

			final String eventRef = FLEFRecordHelper.getChildValue(participation, TAG_EVENT);
			final FLEFRecord event = model.getRecordById(eventRef);
			if(event == null)
				continue;

			final String eventType = FLEFRecordHelper.getChildValue(event, TAG_TYPE);
			if(ENUM_TYPE_BIRTH.equalsIgnoreCase(eventType)){
				if(birthDate == null)
					birthDate = SearchHelper.extractDate(event);
				if(birthPlace == null)
					birthPlace = SearchHelper.extractPlace(event, model);
			}
			else if(ENUM_TYPE_DEATH.equalsIgnoreCase(eventType)){
				if(deathDate == null)
					deathDate = SearchHelper.extractDate(event);
				if(deathPlace == null)
					deathPlace = SearchHelper.extractPlace(event, model);
			}
		}

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(birthDate != null || birthPlace != null)
			details.add("★ " + SearchHelper.joinNonNull(" - ", birthDate, birthPlace));
		if(deathDate != null || deathPlace != null)
			details.add("● " + SearchHelper.joinNonNull(" - ", deathDate, deathPlace));

		return baseDisplayText + details;
	}

}
