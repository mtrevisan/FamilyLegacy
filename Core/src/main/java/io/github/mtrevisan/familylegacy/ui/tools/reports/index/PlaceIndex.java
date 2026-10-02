package io.github.mtrevisan.familylegacy.ui.tools.reports.index;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;


/**
 * Index of every place-centric query.
 *
 * <p>Two distinct event-to-place relations are tracked:</p>
 * <ul>
 *   <li><b>events at a place</b> — events whose {@code place.place} field
 *       points to the place. This is the location where the event happened,
 *       and it is indexed by {@link #eventsAtPlace(String)}.</li>
 *   <li><b>events by a place</b> — events in which the place participates as
 *       an {@code event_participation} participant of kind {@code place}.
 *       This is the "the place took part in the event" relation, and it is
 *       indexed by {@link #eventsByPlace(String)}.</li>
 * </ul>
 *
 * <p>Place-to-place relationships are indexed in both directions: as
 * subject (the place's jurisdiction) and as target (the contained places).</p>
 */
public final class PlaceIndex{

	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_EVENT = "event";
	private static final String TAG_PLACE = "place";


	/** Events that happened at the place (via {@code event.place.place}). */
	private final Map<String, List<FLEFRecord>> placeToEventsMap = new HashMap<>();

	/** Attributes recorded at the place (via {@code attribute.place.place}). */
	private final Map<String, List<FLEFRecord>> placeToAttributesMap = new HashMap<>();

	/** Events in which the place participates (via {@code event_participation}). */
	private final Map<String, List<FLEFRecord>> placeToParticipatingEventsMap = new HashMap<>();

	/** Place-relationships whose subject is the place. */
	private final Map<String, List<FLEFRecord>> placeToSubjectRelationshipsMap = new HashMap<>();

	/** Place-relationships whose target is the place. */
	private final Map<String, List<FLEFRecord>> placeToTargetRelationshipsMap = new HashMap<>();


	public PlaceIndex(final FLEFModel model, final Predicate<FLEFRecord> filter){
		if(model == null)
			return;

		// ----- Events that happened at a place -------------------------
		final List<FLEFRecord> events = model.getRecordsByType(EventHandler.TYPE);
		for(final FLEFRecord event : events){
			if(!filter.test(event))
				continue;
			final String placeId = FLEFRecordHelper.getChildValue(event, "place.place");
			if(placeId != null)
				placeToEventsMap.computeIfAbsent(placeId, k -> new ArrayList<>()).add(event);
		}

		// ----- Events in which a place participates ---------------------
		// event_participation { participant { place P1 }; event E1; role ... }
		final List<FLEFRecord> eventParticipations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(final FLEFRecord eventParticipation : eventParticipations){
			if(!filter.test(eventParticipation))
				continue;

			final String placeId = extractPlaceParticipantId(eventParticipation);
			if(placeId == null)
				continue;

			final String eventId = FLEFRecordHelper.getChildValue(eventParticipation, TAG_EVENT);
			if(eventId == null)
				continue;

			final FLEFRecord event = model.getRecordById(eventId);
			if(event == null || !filter.test(event))
				continue;

			placeToParticipatingEventsMap
				.computeIfAbsent(placeId, k -> new ArrayList<>())
				.add(event);
		}

		// ----- Attributes recorded at a place --------------------------
		final List<FLEFRecord> individualAttributes = model.getRecordsByType(IndividualAttributeHandler.TYPE);
		for(final FLEFRecord individualAttribute : individualAttributes){
			if(!filter.test(individualAttribute))
				continue;
			final String placeId = FLEFRecordHelper.getChildValue(individualAttribute, "place.place");
			if(placeId != null)
				placeToAttributesMap.computeIfAbsent(placeId, k -> new ArrayList<>()).add(individualAttribute);
		}

		// ----- Place relationships (both directions) -------------------
		final List<FLEFRecord> placeRelationships = model.getRecordsByType(PlaceRelationshipHandler.TYPE);
		for(final FLEFRecord placeRelationship : placeRelationships){
			if(!filter.test(placeRelationship))
				continue;
			final String subjectPlace = FLEFRecordHelper.getChildValue(placeRelationship, "subject.place");
			final String targetPlace = FLEFRecordHelper.getChildValue(placeRelationship, "target.place");

			if(subjectPlace != null)
				placeToSubjectRelationshipsMap
					.computeIfAbsent(subjectPlace, k -> new ArrayList<>())
					.add(placeRelationship);
			if(targetPlace != null)
				placeToTargetRelationshipsMap
					.computeIfAbsent(targetPlace, k -> new ArrayList<>())
					.add(placeRelationship);
		}
	}


	/* ======================================================================
	 *                          Lookups
	 * ====================================================================== */

	/** Events that happened at the given place (via {@code event.place.place}). */
	public List<FLEFRecord> eventsAtPlace(final String placeId){
		if(placeId == null)
			return Collections.emptyList();
		return placeToEventsMap.getOrDefault(placeId, Collections.emptyList());
	}

	/**
	 * Events in which the given place participates as an
	 * {@code event_participation} participant of kind {@code place}.
	 */
	public List<FLEFRecord> eventsByPlace(final String placeId){
		if(placeId == null)
			return Collections.emptyList();
		return placeToParticipatingEventsMap.getOrDefault(placeId, Collections.emptyList());
	}

	/** Attributes recorded at the given place. */
	public List<FLEFRecord> attributesAtPlace(final String placeId){
		if(placeId == null)
			return Collections.emptyList();
		return placeToAttributesMap.getOrDefault(placeId, Collections.emptyList());
	}

	/** Place-relationships whose subject is the given place. */
	public List<FLEFRecord> placeRelationshipsAsSubject(final FLEFRecord place){
		if(place == null || place.getId() == null)
			return Collections.emptyList();
		return placeToSubjectRelationshipsMap.getOrDefault(place.getId(), Collections.emptyList());
	}

	/** Place-relationships whose target is the given place. */
	public List<FLEFRecord> placeRelationshipsAsTarget(final FLEFRecord place){
		if(place == null || place.getId() == null)
			return Collections.emptyList();
		return placeToTargetRelationshipsMap.getOrDefault(place.getId(), Collections.emptyList());
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	/**
	 * Extracts the referenced place ID from the {@code participant} oneof of
	 * an {@code event_participation} record, but only when the branch tag is
	 * {@code place}. Returns {@code null} for individual or group branches,
	 * or when the participant field is missing or malformed.
	 *
	 * <p>The oneof is checked by tag, not by name, so future extensions
	 * (e.g. a new branch) do not accidentally match.</p>
	 */
	private static String extractPlaceParticipantId(final FLEFRecord ep){
		final FLEFRecord participantField = FLEFRecordHelper.findChild(ep, TAG_PARTICIPANT);
		if(participantField == null)
			return null;

		final FLEFRecord ref = participantField.getTheOnlyChild();
		if(ref == null)
			return null;

		final String tag = ref.getTag();
		if(tag == null || !TAG_PLACE.equals(tag.toLowerCase(Locale.ROOT)))
			return null;

		return ref.getValue();
	}

}
