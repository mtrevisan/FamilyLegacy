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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Pre-computed indices over the FLEF model, used by the General Temporal
 * Projection to avoid repeated full scans of {@code model.getRecordsByType}.
 * <p>
 * The indices are built once per projection build. They map an entity id to
 * the list of records that reference it, so that extraction for a given row
 * is O(k) where k is the number of records actually touching that row,
 * instead of O(N·M).
 * <p>
 * All map values are immutable lists. The class is immutable and thread-safe.
 */
public final class TemporalIndices{

	// Participant / target tag names inside the FLEF structures.
	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_TARGET = "target";
	private static final String TAG_INDIVIDUAL = "individual";
	private static final String TAG_GROUP = "group";


	private final Map<String, List<FLEFRecord>> eventParticipationsByParticipantId;
	private final Map<String, List<FLEFRecord>> individualAttributesByOwnerId;
	private final Map<String, List<FLEFRecord>> groupAttributesByOwnerId;
	private final Map<String, List<FLEFRecord>> contextImpactsByTargetId;
	private final Map<String, List<FLEFRecord>> historicEventsByPlaceId;
	private final Map<String, List<FLEFRecord>> culturalNormsByPlaceId;

	private final List<FLEFRecord> allRelationships;
	private final List<FLEFRecord> allPlaceRelationships;
	private final List<FLEFRecord> allContextImpacts;
	private final List<FLEFRecord> allHistoricEvents;
	private final List<FLEFRecord> allCulturalNorms;


	private TemporalIndices(final Map<String, List<FLEFRecord>> eventParticipationsByParticipantId,
		final Map<String, List<FLEFRecord>> individualAttributesByOwnerId,
		final Map<String, List<FLEFRecord>> groupAttributesByOwnerId,
		final Map<String, List<FLEFRecord>> contextImpactsByTargetId,
		final Map<String, List<FLEFRecord>> historicEventsByPlaceId,
		final Map<String, List<FLEFRecord>> culturalNormsByPlaceId,
		final List<FLEFRecord> allRelationships,
		final List<FLEFRecord> allPlaceRelationships,
		final List<FLEFRecord> allContextImpacts,
		final List<FLEFRecord> allHistoricEvents,
		final List<FLEFRecord> allCulturalNorms){
		this.eventParticipationsByParticipantId = eventParticipationsByParticipantId;
		this.individualAttributesByOwnerId = individualAttributesByOwnerId;
		this.groupAttributesByOwnerId = groupAttributesByOwnerId;
		this.contextImpactsByTargetId = contextImpactsByTargetId;
		this.historicEventsByPlaceId = historicEventsByPlaceId;
		this.culturalNormsByPlaceId = culturalNormsByPlaceId;
		this.allRelationships = allRelationships;
		this.allPlaceRelationships = allPlaceRelationships;
		this.allContextImpacts = allContextImpacts;
		this.allHistoricEvents = allHistoricEvents;
		this.allCulturalNorms = allCulturalNorms;
	}


	/**
	 * Builds all indices from the given model in a single pass per record
	 * type. This is the only place where {@code model.getRecordsByType} is
	 * invoked for the projection.
	 *
	 * @param model the FLEF model (must not be {@code null})
	 * @return the populated indices
	 */
	public static TemporalIndices build(final FLEFModel model){
		// 1. Fetch each record type exactly once.
		final List<FLEFRecord> eventParticipations = model.getRecordsByType(EventParticipationHandler.TYPE);
		final List<FLEFRecord> individualAttributes = model.getRecordsByType(IndividualAttributeHandler.TYPE);
		final List<FLEFRecord> groupAttributes = model.getRecordsByType(GroupAttributeHandler.TYPE);
		final List<FLEFRecord> contextImpacts = model.getRecordsByType(ContextImpactHandler.TYPE);
		final List<FLEFRecord> historicEvents = model.getRecordsByType(HistoricEventHandler.TYPE);
		final List<FLEFRecord> culturalNorms = model.getRecordsByType(CulturalNormHandler.TYPE);
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		final List<FLEFRecord> placeRelationships = model.getRecordsByType(PlaceRelationshipHandler.TYPE);

		// 2. Build per-id maps.
		final Map<String, List<FLEFRecord>> participationsByParticipantId =
			indexByReference(eventParticipations, TAG_PARTICIPANT);

		final Map<String, List<FLEFRecord>> individualAttributesByOwnerId =
			indexByReference(individualAttributes, TAG_INDIVIDUAL);

		final Map<String, List<FLEFRecord>> groupAttributesByOwnerId =
			indexByReference(groupAttributes, TAG_GROUP);

		final Map<String, List<FLEFRecord>> contextImpactsByTargetId =
			indexByReference(contextImpacts, TAG_TARGET);

		final Map<String, List<FLEFRecord>> historicEventsByPlaceId =
			indexByPlaceCitation(historicEvents);

		final Map<String, List<FLEFRecord>> culturalNormsByPlaceId =
			indexByPlaceCitation(culturalNorms);

		return new TemporalIndices(
			participationsByParticipantId,
			individualAttributesByOwnerId,
			groupAttributesByOwnerId,
			contextImpactsByTargetId,
			historicEventsByPlaceId,
			culturalNormsByPlaceId,
			relationships,
			placeRelationships,
			contextImpacts,
			historicEvents,
			culturalNorms);
	}


	/* ======================================================================
	 *                          Index builders
	 * ====================================================================== */

	/**
	 * Indexes a list of records by the id found inside their
	 * {@code <tag>.{individual|group|place}} reference. The record whose
	 * referenced id is null or missing is skipped.
	 */
	private static Map<String, List<FLEFRecord>> indexByReference(final List<FLEFRecord> records,
		final String referenceTag){
		final Map<String, List<FLEFRecord>> result = new HashMap<>();
		for(final FLEFRecord record : records){
			final FLEFRecord ref = FLEFRecordHelper.findChild(record, referenceTag);
			if(ref == null)
				continue;
			final FLEFRecord inner = ref.getTheOnlyChild();
			if(inner == null || inner.getValue() == null)
				continue;
			result.computeIfAbsent(inner.getValue(), k -> new ArrayList<>())
				.add(record);
		}
		return result;
	}

	/**
	 * Indexes records carrying a {@code place.place} citation by the id of
	 * the referenced place.
	 */
	private static Map<String, List<FLEFRecord>> indexByPlaceCitation(final List<FLEFRecord> records){
		final Map<String, List<FLEFRecord>> result = new HashMap<>();
		for(final FLEFRecord record : records){
			final FLEFRecord citation = FLEFRecordHelper.findChild(record, PlaceHandler.TYPE);
			if(citation == null)
				continue;
			final FLEFRecord ref = FLEFRecordHelper.findChild(citation, PlaceHandler.TYPE);
			if(ref == null || ref.getValue() == null)
				continue;
			result.computeIfAbsent(ref.getValue(), k -> new ArrayList<>())
				.add(record);
		}
		return result;
	}


	/* ======================================================================
	 *                          Accessors
	 * ====================================================================== */

	public List<FLEFRecord> eventParticipationsFor(final String participantId){
		return eventParticipationsByParticipantId.getOrDefault(participantId, List.of());
	}

	public List<FLEFRecord> individualAttributesFor(final String ownerId){
		return individualAttributesByOwnerId.getOrDefault(ownerId, List.of());
	}

	public List<FLEFRecord> groupAttributesFor(final String ownerId){
		return groupAttributesByOwnerId.getOrDefault(ownerId, List.of());
	}

	public List<FLEFRecord> contextImpactsFor(final String targetId){
		return contextImpactsByTargetId.getOrDefault(targetId, List.of());
	}

	public List<FLEFRecord> historicEventsFor(final String placeId){
		return historicEventsByPlaceId.getOrDefault(placeId, List.of());
	}

	public List<FLEFRecord> culturalNormsFor(final String placeId){
		return culturalNormsByPlaceId.getOrDefault(placeId, List.of());
	}

	public List<FLEFRecord> allRelationships(){
		return allRelationships;
	}

	public List<FLEFRecord> allPlaceRelationships(){
		return allPlaceRelationships;
	}

	public List<FLEFRecord> allContextImpacts(){
		return allContextImpacts;
	}

	public List<FLEFRecord> allHistoricEvents(){
		return allHistoricEvents;
	}

	public List<FLEFRecord> allCulturalNorms(){
		return allCulturalNorms;
	}

}
