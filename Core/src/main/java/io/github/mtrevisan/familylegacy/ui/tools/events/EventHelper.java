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
package io.github.mtrevisan.familylegacy.ui.tools.events;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.DateReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceCitationReader;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Shared extraction utilities for {@code EventRecord} and
 * {@code EventParticipationRecord}.
 * <p>
 * The methods here are the single point where event fields are read
 * from the model. Every tool in the {@code events} package goes through
 * this class, so a change to the FLEF structure only needs to be
 * reflected here.
 * <p>
 * <b>Event type vocabulary.</b> The protocol declares a list of event
 * types (birth, death, marriage, divorce, …) but allows custom types.
 * {@link EventReader#TYPES} lists the declared ones, in the order
 * the protocol presents them, so that the tools can offer a stable
 * grouping. Custom types found in the data are always included when
 * enumerating the types actually in use.
 */
public final class EventHelper{

	private EventHelper(){}


	public static List<FLEFRecord> listAllEvents(final FLEFModel model){
		return model.getRecordsByType(EventHandler.TYPE);
	}

	public static List<FLEFRecord> listAllParticipations(final FLEFModel model){
		return model.getRecordsByType(EventParticipationHandler.TYPE);
	}

	/** Returns the type of the event, or {@code null} when missing. */
	public static String eventType(final FLEFRecord event){
		return (event != null? EventReader.extractType(event): null);
	}

	/** Returns the description of the event, or {@code null} when missing. */
	public static String eventDescription(final FLEFRecord event){
		return (event != null? EventReader.extractDescription(event): null);
	}

	/**
	 * Returns the raw value of the {@code DateStructure} of the event,
	 * or {@code null} when the date is missing.
	 * <p>
	 * The value is used as-is, without parsing: a genealogical date can
	 * be a single point, a bounded interval, or a spanning interval, and
	 * the tools in this package do not need to distinguish them.
	 */
	public static String eventDateRaw(final FLEFRecord event){
		if(event == null)
			return null;

		final FLEFRecord dateStruct = FLEFRecordHelper.findChild(event, EventReader.TAG_DATE);
		if(dateStruct == null)
			return null;

		final String value = FLEFRecordHelper.getChildValue(dateStruct, DateReader.TAG_VALUE);
		if(value != null)
			return value;
		final FLEFRecord onlyChild = dateStruct.getTheOnlyChild();
		return (onlyChild != null? onlyChild.getValue(): null);
	}

	/**
	 * Attempts to interpret the event date as a {@link LocalDate}.
	 * Returns {@code null} when the value is missing, unparseable, or
	 * reduced-precision (only a year, only a year-month, …).
	 * <p>
	 * The method is intentionally conservative: it only accepts the
	 * {@code YYYY-MM-DD} form, which is what the protocol uses for a
	 * complete date. Partial dates are ignored by the timeline and by
	 * the calculators, because they cannot be placed on a precise
	 * timeline.
	 */
	public static LocalDate eventDate(final FLEFRecord event){
		final String raw = eventDateRaw(event);
		if(raw == null)
			return null;
		final String s = raw.trim();
		if(s.length() != 10)
			return null;
		try{
			return LocalDate.parse(s);
		}
		catch(final Exception ignored){
			return null;
		}
	}

	/** Returns the referenced place id of the event, or {@code null}. */
	public static String eventPlaceId(final FLEFRecord event){
		if(event == null)
			return null;

		final FLEFRecord placeCitation = FLEFRecordHelper.findChild(event, EventReader.TAG_PLACE);
		if(placeCitation == null)
			return null;

		return FLEFRecordHelper.getChildValue(placeCitation, PlaceCitationReader.TAG_PLACE);
	}


	/* ======================================================================
	 *                          Participants
	 * ====================================================================== */

	/** A single participation of an entity in an event. */
	public record Participation(
		String participationId,
		String eventId,
		String participantId,
		String participantType,
		String role){}

	/**
	 * Extracts the participant reference from a participation record.
	 * The reference is nested: {@code participant} contains a
	 * {@code oneof} block ({@code individual}, {@code group}, or
	 * {@code place}) whose only child holds the id.
	 */
	public static Participation toParticipation(final FLEFRecord eventParticipation){
		final String participationId = eventParticipation.getId();
		final String eventId = FLEFRecordHelper.getChildValue(eventParticipation, EventParticipationReader.TAG_EVENT);
		final String role = FLEFRecordHelper.getChildValue(eventParticipation, EventParticipationReader.TAG_ROLE);

		final FLEFRecord participantBlock = FLEFRecordHelper.findChild(eventParticipation, EventParticipationReader.TAG_PARTICIPANT);
		if(participantBlock == null)
			return new Participation(participationId, eventId, null, null, role);

		// The participant block contains a single nested tag whose name
		// is the entity type (individual, group, place) and whose only
		// child is the referenced id.
		final FLEFRecord entityRef = participantBlock.getTheOnlyChild();
		if(entityRef == null)
			return new Participation(participationId, eventId, null, null, role);
		final String participantType = entityRef.getTag();
		final FLEFRecord idRef = entityRef.getTheOnlyChild();
		final String participantId = (idRef != null? idRef.getValue(): entityRef.getValue());
		return new Participation(participationId, eventId, participantId, participantType, role);
	}

	/**
	 * Returns every participation of the given event, in stable order.
	 */
	public static List<Participation> participantsOf(final FLEFRecord event, final FLEFModel model){
		final List<Participation> result = new ArrayList<>();
		if(event == null || event.getId() == null)
			return result;
		final List<FLEFRecord> eventParticipations = listAllParticipations(model);
		for(final FLEFRecord eventParticipation : eventParticipations){
			final Participation p = toParticipation(eventParticipation);
			if(event.getId().equals(p.eventId()))
				result.add(p);
		}
		return result;
	}

	/**
	 * Counts the participations per event, keyed by event id.
	 */
	public static Map<String, Integer> countParticipantsPerEvent(final FLEFModel model){
		final Map<String, Integer> counts = new LinkedHashMap<>();
		final List<FLEFRecord> eventParticipations = listAllParticipations(model);
		for(final FLEFRecord eventParticipation : eventParticipations){
			final Participation p = toParticipation(eventParticipation);
			if(p.eventId() != null)
				counts.merge(p.eventId(), 1, Integer::sum);
		}
		return counts;
	}

	/**
	 * Counts the participations per participant, keyed by participant id.
	 */
	public static Map<String, Integer> countEventsPerParticipant(final FLEFModel model){
		final Map<String, Integer> counts = new LinkedHashMap<>();
		final List<FLEFRecord> eventParticipations = listAllParticipations(model);
		for(final FLEFRecord eventParticipation : eventParticipations){
			final Participation p = toParticipation(eventParticipation);
			if(p.participantId() != null)
				counts.merge(p.participantId(), 1, Integer::sum);
		}
		return counts;
	}


	/* ======================================================================
	 *                          Row models
	 * ====================================================================== */

	/** Row for the event management table. */
	public record EventRow(
		String id,
		String type,
		String date,
		String placeName,
		int participantCount,
		String description){}

	/**
	 * Builds the participation index once, keyed by event id.
	 * <p>
	 * This is the O(P) building block that every bulk consumer should use
	 * instead of {@link #participantsOf(FLEFRecord, FLEFModel)}, which is
	 * O(P) per call and becomes O(E × P) when used in a loop over events.
	 */
	public static Map<String, List<Participation>> participationsByEvent(final FLEFModel model){
		final Map<String, List<Participation>> result = new LinkedHashMap<>();
		final List<FLEFRecord> eventParticipations = listAllParticipations(model);
		for(final FLEFRecord eventParticipation : eventParticipations){
			final Participation p = toParticipation(eventParticipation);
			if(p.eventId() != null)
				result.computeIfAbsent(p.eventId(), k -> new ArrayList<>()).add(p);
		}
		return result;
	}

	/**
	 * Builds a display row for the management table.
	 * <p>
	 * The method takes precomputed indexes instead of the model, so it can
	 * be called in a tight loop over every event without re-scanning the
	 * whole participation list on each call.
	 */
	public static EventRow toRow(final FLEFRecord event,
			final Map<String, FLEFRecord> placesById,
			final Map<String, List<Participation>> participationsByEvent){
		final String id = event.getId();
		final String type = eventType(event);
		final String date = eventDateRaw(event);
		final String description = eventDescription(event);

		final String placeId = eventPlaceId(event);
		String placeName = null;
		if(placeId != null && placesById.containsKey(placeId))
			placeName = io.github.mtrevisan.familylegacy.ui.tools.places.PlaceHelper
				.displayName(placesById.get(placeId));
		else if(placeId != null)
			placeName = placeId;

		final int participants = participationsByEvent
			.getOrDefault(id, List.of()).size();

		return new EventRow(id, type, date, placeName, participants, description);
	}

}
