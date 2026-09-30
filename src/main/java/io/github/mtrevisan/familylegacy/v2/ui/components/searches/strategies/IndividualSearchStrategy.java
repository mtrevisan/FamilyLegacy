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
import io.github.mtrevisan.familylegacy.v2.io.model.readers.DateReader;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchMode;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * Search strategy for Individual records.
 * Supports event‑based filters: sex, eventType, dateFrom, dateTo, locationContains.
 */
public class IndividualSearchStrategy implements SearchStrategy{

	static final String KEY_DATE_FROM = EventReader.TAG_DATE + "_" + DateReader.TAG_FROM;
	static final String KEY_CALENDAR_FROM = DateReader.TAG_CALENDAR + "_" + DateReader.TAG_FROM;
	static final String KEY_DATE_TO = EventReader.TAG_DATE + "_" + DateReader.TAG_TO;
	static final String KEY_CALENDAR_TO = DateReader.TAG_CALENDAR + "_" + DateReader.TAG_TO;

	private static final String TAG_PARTICIPANT_INDIVIDUAL = FLEFRecordHelper.composePath(EventParticipationReader.TAG_PARTICIPANT, IndividualHandler.TYPE);


	private static final String SEX_ABBREVIATION_MALE = "[" + I18N.t("dialog.individual.sex.abbreviation.male") + "]";
	private static final String SEX_ABBREVIATION_FEMALE = "[" + I18N.t("dialog.individual.sex.abbreviation.female") + "]";
	private static final String SEX_ABBREVIATION_UNKNOWN = "[" + I18N.t("dialog.individual.sex.abbreviation.unknown") + "]";

	private static final IndividualHandler HANDLER = IndividualHandler.getInstance();


	private String sex;
	private String eventType;
	private String eventDateFrom;
	private String calendarFrom;
	private String eventDateTo;
	private String calendarTo;
	private String eventPlace;
	private SearchMode mode;

	private FLEFModel model;

	private final Map<String, Integer> birthYears = new HashMap<>();
	private final Map<String, Integer> deathYears = new HashMap<>();
	private final Map<String, List<FLEFRecord>> eventsByIndividual = new HashMap<>();


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		sex = criteria.getFilterFor(IndividualReader.TAG_SEX);
		eventType = criteria.getFilterFor(EventReader.TAG_TYPE);
		eventDateFrom = criteria.getFilterFor(KEY_DATE_FROM);
		calendarFrom = criteria.getFilterFor(KEY_CALENDAR_FROM);
		eventDateTo = criteria.getFilterFor(KEY_DATE_TO);
		calendarTo = criteria.getFilterFor(KEY_CALENDAR_TO);
		eventPlace = criteria.getFilterFor(EventReader.TAG_PLACE);
		mode = criteria.mode();

		this.model = model;

		SearchHelper.precomputeLifeBounds(birthYears, deathYears, model);
		final List<FLEFRecord> participations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(final FLEFRecord participation : participations){
			final String participantRef = FLEFRecordHelper.getChildValue(participation, TAG_PARTICIPANT_INDIVIDUAL);
			final String eventRef = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT);
			if(participantRef != null && eventRef != null){
				final FLEFRecord event = model.getRecordById(eventRef);
				if(event != null)
					eventsByIndividual.computeIfAbsent(participantRef, k -> new ArrayList<>())
						.add(event);
			}
		}

		final boolean hasEventFilters = (StringUtils.isNotEmpty(eventType) || StringUtils.isNotEmpty(eventDateFrom)
			|| StringUtils.isNotEmpty(eventDateTo) || StringUtils.isNotEmpty(eventPlace));

		return individual -> {
			if(StringUtils.isNotEmpty(sex)){
				final String recordSex = IndividualReader.extractRawSex(individual);
				if(!IndividualReader.isSexUnknown(recordSex) && !Strings.CI.equals(sex, recordSex))
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

		final List<FLEFRecord> events = eventsByIndividual.getOrDefault(individualId, List.of());
		for(final FLEFRecord event : events)
			if(matchesEvent(event, birthYear, deathYear))
				return true;
		return false;
	}

	private boolean matchesEvent(final FLEFRecord event, final Integer birthYear, final Integer deathYear){
		// Event type
		if(StringUtils.isNotEmpty(eventType)){
			final String type = EventReader.extractType(event);
			if(!Strings.CI.equals(eventType, type))
				return false;
		}

		// Date range
		if(StringUtils.isNotEmpty(eventDateFrom) || StringUtils.isNotEmpty(eventDateTo)){
			final FLEFRecord dateRecord = EventReader.extractDate(event);
			final Integer fromYear = (StringUtils.isNotEmpty(eventDateFrom)
				? SearchHelper.extractYear(eventDateFrom, calendarFrom)
				: null);
			final Integer toYear = (StringUtils.isNotEmpty(eventDateTo)
				? SearchHelper.extractYear(eventDateTo, calendarTo)
				: null);
			if(!SearchHelper.isDateInRange(dateRecord, birthYear, deathYear, fromYear, toYear))
				return false;
		}

		// Place
		if(StringUtils.isNotEmpty(eventPlace)){
			final FLEFRecord placeCitation = FLEFRecordHelper.findChild(event, PlaceHandler.TYPE);
			if(placeCitation != null){
				final String placeId = placeCitation.getTheOnlyChild().getValue();
				final FLEFRecord placeRecord = model.getRecordById(placeId);
				final String place = PlaceHandler.getInstance()
					.getDisplayText(placeRecord, model);
				if(!SearchHelper.matches(place, eventPlace, mode))
					return false;
			}
		}

		return true;
	}


	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		String baseDisplayText = HANDLER.getDisplayText(record, model);
		final String recordSex = IndividualReader.extractRawSex(record);
		if(StringUtils.isNotEmpty(recordSex)){
			if(IndividualReader.isSexMale(recordSex))
				baseDisplayText = SEX_ABBREVIATION_MALE + StringUtils.SPACE + baseDisplayText;
			else if(IndividualReader.isSexFemale(recordSex))
				baseDisplayText = SEX_ABBREVIATION_FEMALE + StringUtils.SPACE + baseDisplayText;
			else
				baseDisplayText = SEX_ABBREVIATION_UNKNOWN + StringUtils.SPACE + baseDisplayText;
		}

		String birthDate = null;
		String birthPlace = null;
		String deathDate = null;
		String deathPlace = null;

		final List<FLEFRecord> events = eventsByIndividual.getOrDefault(record.getId(), List.of());
		for(final FLEFRecord event : events){
			final String eventType = EventReader.extractType(event);
			if(EventReader.isTypeBirth(eventType)){
				if(birthDate == null)
					birthDate = FLEFRecordHelper.extractDate(event);
				if(birthPlace == null)
					birthPlace = FLEFRecordHelper.extractPlace(event, model);
			}
			else if(EventReader.isTypeDeath(eventType)){
				if(deathDate == null)
					deathDate = FLEFRecordHelper.extractDate(event);
				if(deathPlace == null)
					deathPlace = FLEFRecordHelper.extractPlace(event, model);
			}

			if(birthDate != null && birthPlace != null && deathDate != null && deathPlace != null)
				break;
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
