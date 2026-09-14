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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.lifespan;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.DateNormalizer;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.NormalizedDate;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalSpan;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Pre-computed, immutable index of all events in a FLEF model.
 * <p>
 * Exposes:
 * <ul>
 *   <li>the flat list of all events, ordered by date;</li>
 *   <li>a lookup by event type;</li>
 *   <li>the list of events per participant (individual or group);</li>
 *   <li>the list of events per place.</li>
 * </ul>
 * Each event is exposed as an {@link EventDatum} record with the resolved
 * date, place and participants, so that the views do not need to touch the
 * model again.
 */
public final class EventIndex{

	private static final String TAG_TYPE = "type";
	private static final String TAG_DATE = "date";
	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_EVENT = "event";
	private static final String TAG_NAME = "name";
	private static final String TAG_VALUE = "value";


	/** A participant of an event, resolved to a display name. */
	public record Participant(String id, String name, boolean isIndividual){
	}

	/** A single event, resolved. */
	public record EventDatum(String id, String type, String description, NormalizedDate date,
									 String placeId, String placeName, List<Participant> participants){
		public boolean hasDate(){
			return date != null;
		}

		public boolean hasPlace(){
			return placeId != null;
		}
	}


	private final List<EventDatum> allEvents;
	private final Map<String, List<EventDatum>> eventsByType;
	private final Map<String, List<EventDatum>> eventsByParticipantId;
	private final Map<String, List<EventDatum>> eventsByPlaceId;
	private final Map<String, EventDatum> eventsById;


	private EventIndex(final List<EventDatum> allEvents, final Map<String, List<EventDatum>> eventsByType,
		final Map<String, List<EventDatum>> eventsByParticipantId,
		final Map<String, List<EventDatum>> eventsByPlaceId,
		final Map<String, EventDatum> eventsById){
		this.allEvents = allEvents;
		this.eventsByType = eventsByType;
		this.eventsByParticipantId = eventsByParticipantId;
		this.eventsByPlaceId = eventsByPlaceId;
		this.eventsById = eventsById;
	}


	public static EventIndex build(final FLEFModel model){
		final DateNormalizer normalizer = new DateNormalizer();

		// Collect participations grouped by event.
		final Map<String, List<FLEFRecord>> participationsByEvent = new LinkedHashMap<>();
		for(final FLEFRecord p : model.getRecordsByType(EventParticipationHandler.TYPE)){
			final String eventId = FLEFRecordHelper.getChildValue(p, TAG_EVENT);
			if(eventId == null)
				continue;
			participationsByEvent.computeIfAbsent(eventId, k -> new ArrayList<>()).add(p);
		}

		// Build the event datum for each event.
		final List<EventDatum> events = new ArrayList<>();
		final Map<String, EventDatum> byId = new HashMap<>();
		for(final FLEFRecord event : model.getRecordsByType(EventHandler.TYPE)){
			final String id = event.getId();
			if(id == null)
				continue;

			final String type = FLEFRecordHelper.getChildValue(event, TAG_TYPE);
			final String description = FLEFRecordHelper.getChildValue(event, "description");

			// Date.
			final FLEFRecord dateStructure = FLEFRecordHelper.findChild(event, TAG_DATE);
			final TemporalSpan span = normalizer.normalize(dateStructure);
			final NormalizedDate date = (span != null? span.start(): null);

			// Place.
			final FLEFRecord placeCitation = FLEFRecordHelper.findChild(event, PlaceHandler.TYPE);
			String placeId = null;
			String placeName = null;
			if(placeCitation != null){
				final FLEFRecord ref = FLEFRecordHelper.findChild(placeCitation, PlaceHandler.TYPE);
				if(ref != null && ref.getValue() != null){
					placeId = ref.getValue();
					placeName = resolvePlaceName(model, placeId);
				}
			}

			// Participants.
			final List<Participant> participants = new ArrayList<>();
			for(final FLEFRecord p : participationsByEvent.getOrDefault(id, List.of())){
				final Participant participant = resolveParticipant(model, p);
				if(participant != null)
					participants.add(participant);
			}

			final EventDatum datum = new EventDatum(id, type != null? type: "unknown",
				description != null? description: StringUtils.EMPTY, date, placeId, placeName, participants);
			events.add(datum);
			byId.put(id, datum);
		}

		// Sort chronologically: dated first, then by JDN; undated last.
		events.sort(Comparator
			.comparing((EventDatum e) -> (e.hasDate()? 0: 1))
			.thenComparing(e -> (e.hasDate()? e.date(): null),
				Comparator.nullsLast(Comparator.naturalOrder()))
			.thenComparing(EventDatum::id));

		// Build the lookup indices.
		final Map<String, List<EventDatum>> byType = new HashMap<>();
		final Map<String, List<EventDatum>> byParticipant = new HashMap<>();
		final Map<String, List<EventDatum>> byPlace = new HashMap<>();
		for(final EventDatum e : events){
			byType.computeIfAbsent(e.type(), k -> new ArrayList<>()).add(e);
			for(final Participant p : e.participants())
				byParticipant.computeIfAbsent(p.id(), k -> new ArrayList<>()).add(e);
			if(e.hasPlace())
				byPlace.computeIfAbsent(e.placeId(), k -> new ArrayList<>()).add(e);
		}

		return new EventIndex(events, byType, byParticipant, byPlace, byId);
	}


	/* ======================================================================
	 *                          Accessors
	 * ====================================================================== */

	public List<EventDatum> allEvents(){
		return allEvents;
	}

	public Map<String, EventDatum> eventsById(){
		return eventsById;
	}

	public List<EventDatum> eventsOfType(final String type){
		return eventsByType.getOrDefault(type, List.of());
	}

	public List<EventDatum> eventsOf(final String participantId){
		return eventsByParticipantId.getOrDefault(participantId, List.of());
	}

	public List<EventDatum> eventsAt(final String placeId){
		return eventsByPlaceId.getOrDefault(placeId, List.of());
	}

	public Map<String, List<EventDatum>> eventsByType(){
		return eventsByType;
	}

	public boolean isEmpty(){
		return allEvents.isEmpty();
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private static Participant resolveParticipant(final FLEFModel model, final FLEFRecord participation){
		final FLEFRecord field = FLEFRecordHelper.findChild(participation, TAG_PARTICIPANT);
		if(field == null)
			return null;
		final FLEFRecord ref = field.getTheOnlyChild();
		if(ref == null || ref.getValue() == null)
			return null;

		final String id = ref.getValue();
		final String tag = ref.getTag();
		final FLEFRecord record = model.getRecordById(id);
		if(record == null)
			return null;

		final boolean isIndividual = IndividualHandler.TYPE.equalsIgnoreCase(tag);
		final String name = (isIndividual
			? IndividualHandler.getInstance().getDisplayText(record, model)
			: GroupHandler.getInstance().getDisplayText(record, model));
		return new Participant(id, name != null? name: id, isIndividual);
	}

	private static String resolvePlaceName(final FLEFModel model, final String placeId){
		final FLEFRecord place = model.getRecordById(placeId);
		if(place == null)
			return placeId;
		final FLEFRecord name = FLEFRecordHelper.findChild(place, TAG_NAME);
		if(name != null){
			final String v = FLEFRecordHelper.getChildValue(name, TAG_VALUE);
			if(v != null && !v.isBlank())
				return v;
		}
		return (place.getId() != null? place.getId(): placeId);
	}

	// Reserved for future expansions.
	static String placeHandlerType(){
		return PlaceHandler.TYPE;
	}

}
