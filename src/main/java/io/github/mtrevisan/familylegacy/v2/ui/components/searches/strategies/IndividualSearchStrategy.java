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
	private static final String TAG_INDIVIDUAL = "individual";
	private static final String TAG_EVENT = "event";
	private static final String TAG_PARTICIPANT_INDIVIDUAL = TAG_PARTICIPANT + DOT + TAG_INDIVIDUAL;

	private static final String ENUM_TYPE_BIRTH = "birth";
	private static final String ENUM_TYPE_DEATH = "death";
	private static final String ENUM_SEX_MALE = "male";
	private static final String ENUM_SEX_FEMALE = "female";
	private static final String ENUM_SEX_UNKNOWN = "unknown";

	private static final double FUZZY_THRESHOLD = 0.05;

	private static final IndividualHandler HANDLER = IndividualHandler.getInstance();


	private String sex;
	private String eventType;
	private String eventDateFrom;
	private String calendarFrom;
	private String eventDateTo;
	private String calendarTo;
	private String eventLocation;
	private boolean fuzzy;
	private boolean wholeWord;

	private FLEFModel model;

	private final Map<String, Integer> birthYears = new HashMap<>();
	private final Map<String, Integer> deathYears = new HashMap<>();


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		sex = criteria.getFilterFor(IndividualFilterPanel.FILTER_KEY_SEX);
		eventType = criteria.getFilterFor(IndividualFilterPanel.FILTER_KEY_EVENT_TYPE);
		eventDateFrom = criteria.getFilterFor(IndividualFilterPanel.FILTER_KEY_EVENT_DATE_FROM);
		calendarFrom = criteria.getFilterFor(IndividualFilterPanel.FILTER_KEY_EVENT_CALENDAR_FROM);
		eventDateTo = criteria.getFilterFor(IndividualFilterPanel.FILTER_KEY_EVENT_DATE_TO);
		calendarTo = criteria.getFilterFor(IndividualFilterPanel.FILTER_KEY_EVENT_CALENDAR_TO);
		eventLocation = criteria.getFilterFor(IndividualFilterPanel.FILTER_KEY_EVENT_LOCATION);
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		this.model = model;

		SearchHelper.precomputeLifeBounds(birthYears, deathYears, model);

		final boolean hasEventFilters = (StringUtils.isNotEmpty(eventType) || StringUtils.isNotEmpty(eventDateFrom)
			|| StringUtils.isNotEmpty(eventDateTo) || StringUtils.isNotEmpty(eventLocation));

		return individual -> {
			if(StringUtils.isNotEmpty(sex)){
				final String recordSex = FLEFRecordHelper.getChildValue(individual, TAG_SEX);
				if(!ENUM_SEX_UNKNOWN.equalsIgnoreCase(recordSex) && !sex.equalsIgnoreCase(recordSex))
					return false;
			}

			if(hasEventFilters)
				return hasMatchingEvent(individual);

			return true;
		};
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
		if(StringUtils.isNotEmpty(eventDateFrom) || StringUtils.isNotEmpty(eventDateTo)){
			final FLEFRecord eventDateRecord = FLEFRecordHelper.findChild(event, TAG_DATE);
			final Integer fromYear = (StringUtils.isNotEmpty(eventDateFrom)
				? SearchHelper.extractYear(eventDateFrom, calendarFrom)
				: null);
			final Integer toYear = (StringUtils.isNotEmpty(eventDateTo)
				? SearchHelper.extractYear(eventDateTo, calendarTo)
				: null);
			if(!SearchHelper.isDateInRange(eventDateRecord, birthYear, deathYear, fromYear, toYear))
				return false;
		}

		// Location
		if(StringUtils.isNotEmpty(eventLocation)){
			final FLEFRecord placeCitation = FLEFRecordHelper.findChild(event, TAG_PLACE);
			if(placeCitation != null){
				final String placeId = placeCitation.getTheOnlyChild().getValue();
				final FLEFRecord placeRecord = model.getRecordById(placeId);
				final String place = PlaceHandler.getInstance()
					.getDisplayText(placeRecord, model);
				if(!TextSearchHelper.matchesText(place, eventLocation, fuzzy, wholeWord, FUZZY_THRESHOLD))
					return false;
			}
		}

		return true;
	}


	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String recordSex = FLEFRecordHelper.getChildValue(record, TAG_SEX);
		if(StringUtils.isNotEmpty(recordSex))
			baseDisplayText = switch(recordSex){
				case ENUM_SEX_MALE -> "[M] " + baseDisplayText;
				case ENUM_SEX_FEMALE -> "[F] " + baseDisplayText;
				default -> "[U] " + baseDisplayText;
			};

		String birthDate = null;
		String birthPlace = null;
		String deathDate = null;
		String deathPlace = null;

		final String individualId = record.getId();
		final List<FLEFRecord> participations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(final FLEFRecord participation : participations){
			final String participantRef = FLEFRecordHelper.getChildValue(participation, TAG_PARTICIPANT_INDIVIDUAL);
			if(!Objects.equals(participantRef, individualId))
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
