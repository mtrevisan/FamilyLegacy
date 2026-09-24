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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.repository;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;


/**
 * Centralized repository providing single-pass indexing and flyweight caching
 * for genealogical data shared across multiple application projections.
 */
public class GenealogyRepository{

	private static final String TAG_TYPE = "type";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";
	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_EVENT = "event";


	private final FLEFModel model;
	private final String[] relationshipAllowedTypes;
	private final Predicate<String> relationshipTypeFilter;

	// Global inverted indices
	private final Map<String, List<FLEFRecord>> individualToParentsMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> parentToChildrenMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> individualToEventMap = new HashMap<>();
	private final Set<String> individualsWithDescendantsSet = new HashSet<>();
	private final Map<String, List<String>> individualToRelationshipIdsMap = new HashMap<>();

	// Flyweight caches
	private final Map<String, IndividualData> individualDataCache = new HashMap<>();

	private boolean initialized;


	public GenealogyRepository(final String[] relationshipAllowedTypes, final FLEFModel model){
		this.model = model;
		this.relationshipAllowedTypes = relationshipAllowedTypes;

		relationshipTypeFilter = type ->
			ArrayUtils.contains(relationshipAllowedTypes, type.toLowerCase(Locale.ROOT));
	}


	/**
	 * Ensures global indices are populated in a single pass across the FLEFModel.
	 */
	public synchronized void ensureIndices(){
		if(initialized)
			return;

		// Index relationships
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(int i = 0, size = relationships.size(); i < size; i ++){
			final FLEFRecord relationship = relationships.get(i);
			indexRelationship(relationship);
		}

		// Index events
		final List<FLEFRecord> eventParticipations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(int i = 0, size = eventParticipations.size(); i < size; i ++){
			final FLEFRecord eventParticipation = eventParticipations.get(i);
			indexEventParticipation(eventParticipation);
		}

		initialized = true;
	}

	private void indexRelationship(final FLEFRecord relationship){
		final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
		if(type == null)
			return;

		final String subjectId = relationship.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
		final String targetId = relationship.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);
		if(subjectId == null || targetId == null)
			return;

		final String relationshipId = relationship.getId();
		if(relationshipId != null){
			individualToRelationshipIdsMap.computeIfAbsent(subjectId, k -> new ArrayList<>())
				.add(relationshipId);
			individualToRelationshipIdsMap.computeIfAbsent(targetId, k -> new ArrayList<>())
				.add(relationshipId);
		}

		if(relationshipTypeFilter.test(type)){
			final FLEFRecord child = model.getRecordById(subjectId);
			final FLEFRecord parent = model.getRecordById(targetId);
			if(parent != null)
				individualToParentsMap.computeIfAbsent(subjectId, k -> new ArrayList<>())
					.add(parent);
			if(child != null){
				parentToChildrenMap.computeIfAbsent(targetId, k -> new ArrayList<>())
					.add(child);
				individualsWithDescendantsSet.add(targetId);
			}
		}
	}

	private void indexEventParticipation(final FLEFRecord eventParticipation){
		final FLEFRecord participant = FLEFRecordHelper.findChild(eventParticipation, TAG_PARTICIPANT);
		if(participant == null)
			return;

		final FLEFRecord individualRef = participant.getTheOnlyChild();
		if(individualRef == null)
			return;

		final String individualId = individualRef.getValue();
		if(individualId == null)
			return;

		final String eventId = FLEFRecordHelper.getChildValue(eventParticipation, TAG_EVENT);
		if(eventId == null)
			return;

		final FLEFRecord event = model.getRecordById(eventId);
		if(event != null && EventHandler.TYPE.equalsIgnoreCase(event.getTag()))
			individualToEventMap.computeIfAbsent(individualId, k -> new ArrayList<>())
				.add(event);
	}

	/**
	 * Flyweight retriever for IndividualData instances.
	 */
	public IndividualData getIndividualData(final String individualId){
		if(individualId == null)
			return null;

		ensureIndices();

		return individualDataCache.computeIfAbsent(individualId, id -> {
			final FLEFRecord record = model.getRecordById(id);
			return (record != null? IndividualData.create(record, individualToEventMap, model): null);
		});
	}

	public IndividualData getIndividualData(final FLEFRecord individual){
		if(individual == null || individual.getId() == null)
			return null;

		ensureIndices();

		return individualDataCache.computeIfAbsent(individual.getId(),
			id -> IndividualData.create(individual, individualToEventMap, model));
	}

	public List<FLEFRecord> getParents(final String individualId){
		ensureIndices();

		final List<FLEFRecord> parents = individualToParentsMap.get(individualId);
		return (parents != null && !parents.isEmpty()? new ArrayList<>(parents): Collections.emptyList());
	}

	public List<FLEFRecord> getChildren(final String parentId){
		ensureIndices();

		final List<FLEFRecord> children = parentToChildrenMap.get(parentId);
		return (children != null && !children.isEmpty()? new ArrayList<>(children): Collections.emptyList());
	}

	public List<String> getRelationshipIdsForIndividual(final String individualId){
		if(individualId == null)
			return Collections.emptyList();

		ensureIndices();

		final List<String> ids = individualToRelationshipIdsMap.get(individualId);
		return (ids != null? new ArrayList<>(ids): Collections.emptyList());
	}

	public boolean hasDescendants(final String individualId){
		ensureIndices();

		return individualsWithDescendantsSet.contains(individualId);
	}

	/* ======================================================================
	 *                       Delta-Update Invalidation
	 * ====================================================================== */

	/**
	 * Selective Delta-Update when a relationship is added.
	 */
	public synchronized void notifyRelationshipAdded(final String subjectId, final String targetId,
			final String relationshipId){
		if(!initialized)
			return;

		if(relationshipId != null){
			if(subjectId != null)
				individualToRelationshipIdsMap.computeIfAbsent(subjectId, k -> new ArrayList<>())
					.add(relationshipId);
			if(targetId != null)
				individualToRelationshipIdsMap.computeIfAbsent(targetId, k -> new ArrayList<>())
					.add(relationshipId);
		}

		if(subjectId != null && targetId != null){
			final FLEFRecord parent = model.getRecordById(targetId);
			final FLEFRecord child = model.getRecordById(subjectId);
			if(parent != null)
				individualToParentsMap.computeIfAbsent(subjectId, k -> new ArrayList<>())
					.add(parent);
			if(child != null){
				parentToChildrenMap.computeIfAbsent(targetId, k -> new ArrayList<>())
					.add(child);
				individualsWithDescendantsSet.add(targetId);
			}
		}

		invalidateIndividual(subjectId);
		invalidateIndividual(targetId);
	}

	/**
	 * Selective Delta-Update when a relationship is removed.
	 */
	public synchronized void notifyRelationshipRemoved(final String relationshipId){
		if(!initialized)
			return;

		// Perform full re-index of relationships only if necessary
		invalidateIndices();
	}

	/**
	 * Invalidates a specific individual entry in the flyweight cache.
	 */
	public void invalidateIndividual(final String individualId){
		if(individualId != null)
			individualDataCache.remove(individualId);
	}

	/**
	 * Full cache invalidation (triggered on structural model reload).
	 */
	public synchronized void invalidateIndices(){
		individualToParentsMap.clear();
		parentToChildrenMap.clear();
		individualToEventMap.clear();
		individualsWithDescendantsSet.clear();
		individualToRelationshipIdsMap.clear();
		individualDataCache.clear();
		initialized = false;
	}

	public String[] getRelationshipAllowedTypes(){
		return relationshipAllowedTypes;
	}

	public Predicate<String> getRelationshipTypeFilter(){
		return relationshipTypeFilter;
	}

}
