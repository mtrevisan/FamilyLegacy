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
package io.github.mtrevisan.familylegacy.ui.assistant.context;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.ContextImpactReader;
import io.github.mtrevisan.familylegacy.io.model.readers.CulturalNormReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.HistoricEventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.ui.assistant.chronology.DateRange;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Reads from the model what the Context Assistant needs. The individuals hidden by the privacy settings are left out, and so are the
 * events that are about nobody else.
 */
final class ContextDataLoader{

	private static final String ROLE_PRINCIPAL = "principal";
	private static final String PLACE_OF = FLEFRecordHelper.composePath(PlaceHandler.TYPE, PlaceHandler.TYPE);
	private static final Set<String> TERRITORIAL = Set.of("administrative_part_of", "geographic_part_of");


	private ContextDataLoader(){}


	static ContextData load(final FLEFModel model){
		final Map<String, String> placeLabels = new HashMap<>();
		for(final FLEFRecord place : model.getRecordsByType(PlaceHandler.TYPE)){
			final String name = PlaceReader.extractPrimaryName(place);
			placeLabels.put(place.getId(), (name != null? name: place.getId()));
		}

		return new ContextData(events(model), contexts(model), impacts(model), ancestors(model), placeLabels);
	}


	private static List<ContextData.Event> events(final FLEFModel model){
		final Map<String, String> names = new LinkedHashMap<>();
		for(final FLEFRecord individual : model.getRecordsByType(IndividualHandler.TYPE))
			if(individual.getId() != null && !PrivacyService.isHidden(model, individual)){
				final String name = IndividualReader.extractPrimaryFullname(individual);
				names.put(individual.getId(), (StringUtils.isNotBlank(name)? name: individual.getId()));
			}

		final Map<String, List<String>> principals = new LinkedHashMap<>();
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationHandler.TYPE)){
			final String personId = armValue(FLEFRecordHelper.findChild(participation, EventParticipationReader.TAG_PARTICIPANT));
			final String eventId = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT);
			final String role = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_ROLE);
			if(personId != null && eventId != null && names.containsKey(personId) && (role == null || ROLE_PRINCIPAL.equals(role)))
				principals.computeIfAbsent(eventId, k -> new ArrayList<>()).add(personId);
		}

		final List<ContextData.Event> events = new ArrayList<>();
		for(final Map.Entry<String, List<String>> entry : principals.entrySet()){
			final FLEFRecord event = model.getRecordById(entry.getKey());
			if(!EventHandler.isOfType(event))
				continue;

			final String type = EventReader.extractType(event);
			final DateRange range = range(event, EventReader.TAG_DATE);
			final String who = String.join(", ", entry.getValue().stream().map(names::get).toList());
			final String what = I18N.t("enum.event.type." + type, String.valueOf(type));
			final boolean bounded = (range != null && range.min() != Long.MIN_VALUE && range.max() != Long.MAX_VALUE);
			events.add(new ContextData.Event(entry.getKey(), type, range, FLEFRecordHelper.getChildValue(event, PLACE_OF),
				I18N.tf("context.event.label", what, who)
					+ (bounded? " (" + CalendarType.jdnToProlepticGregorianLocalDate(range.min()).getYear() + ")": ""),
				entry.getValue()));
		}
		return events;
	}

	private static List<ContextData.Context> contexts(final FLEFModel model){
		final List<ContextData.Context> contexts = new ArrayList<>();
		for(final FLEFRecord record : model.getRecordsByType(HistoricEventHandler.TYPE)){
			final String title = HistoricEventReader.extractTitle(record);
			final String type = HistoricEventReader.extractType(record);
			contexts.add(new ContextData.Context(record.getId(), true, type, label(title, type, record.getId()),
				range(record, HistoricEventReader.TAG_DATE), FLEFRecordHelper.getChildValue(record, PLACE_OF)));
		}
		for(final FLEFRecord record : model.getRecordsByType(CulturalNormHandler.TYPE)){
			final String title = FLEFRecordHelper.getChildValue(record, CulturalNormReader.TAG_TITLE);
			final String type = FLEFRecordHelper.getChildValue(record, CulturalNormReader.TAG_TYPE);
			final DateRange from = range(record, CulturalNormReader.TAG_VALID_FROM);
			final DateRange to = range(record, CulturalNormReader.TAG_VALID_TO);
			final long min = (from != null? from.min(): Long.MIN_VALUE);
			final long max = (to != null? to.max(): Long.MAX_VALUE);
			final DateRange validity = ((from != null || to != null) && min <= max? new DateRange(min, max): null);
			contexts.add(new ContextData.Context(record.getId(), false, type, label(title, type, record.getId()), validity,
				FLEFRecordHelper.getChildValue(record, PLACE_OF)));
		}
		return contexts;
	}

	private static String label(final String title, final String type, final String id){
		if(StringUtils.isNotBlank(title))
			return title;
		return (StringUtils.isNotBlank(type)? type + " (" + id + ")": id);
	}

	private static List<ContextData.Impact> impacts(final FLEFModel model){
		final List<ContextData.Impact> impacts = new ArrayList<>();
		for(final FLEFRecord record : model.getRecordsByType(ContextImpactHandler.TYPE)){
			final String context = armValue(FLEFRecordHelper.findChild(record, ContextImpactReader.TAG_CONTEXT));
			final String target = armValue(FLEFRecordHelper.findChild(record, ContextImpactReader.TAG_TARGET));
			if(context != null && target != null)
				impacts.add(new ContextData.Impact(context, target));
		}
		return impacts;
	}

	/** The places that contain each place, through the relationships of territory, at any depth. */
	private static Map<String, Set<String>> ancestors(final FLEFModel model){
		final Map<String, Set<String>> parents = new HashMap<>();
		for(final FLEFRecord record : model.getRecordsByType(PlaceRelationshipHandler.TYPE)){
			if(!TERRITORIAL.contains(PlaceRelationshipReader.extractType(record)))
				continue;

			final String subject = FLEFRecordHelper.getChildValue(record,
				FLEFRecordHelper.composePath(PlaceRelationshipReader.TAG_SUBJECT, PlaceHandler.TYPE));
			final String target = FLEFRecordHelper.getChildValue(record,
				FLEFRecordHelper.composePath(PlaceRelationshipReader.TAG_TARGET, PlaceHandler.TYPE));
			if(subject != null && target != null)
				parents.computeIfAbsent(subject, k -> new HashSet<>()).add(target);
		}

		final Map<String, Set<String>> ancestors = new HashMap<>();
		for(final String place : parents.keySet()){
			final Set<String> seen = new HashSet<>();
			final List<String> queue = new ArrayList<>(parents.get(place));
			while(!queue.isEmpty()){
				final String next = queue.remove(queue.size() - 1);
				if(!next.equals(place) && seen.add(next))
					queue.addAll(parents.getOrDefault(next, Set.of()));
			}
			ancestors.put(place, seen);
		}
		return ancestors;
	}


	/** @return the days of the date held by a tag of a record, or {@code null} if it has none, or it cannot be read */
	private static DateRange range(final FLEFRecord record, final String tag){
		final List<FLEFRecord> dates = FLEFRecordHelper.extractStructures(record, tag);
		if(dates.isEmpty())
			return null;

		try{
			return DateRange.of(DateNormalizer.normalize(dates.getFirst()));
		}
		catch(final RuntimeException ignored){
			return null;
		}
	}

	/** @return the value of the alternative a "oneof" holds (an Xref), or {@code null} */
	private static String armValue(final FLEFRecord union){
		return (union == null || union.getChildren().isEmpty()? null: union.getChildren().getFirst().getValue());
	}

}
