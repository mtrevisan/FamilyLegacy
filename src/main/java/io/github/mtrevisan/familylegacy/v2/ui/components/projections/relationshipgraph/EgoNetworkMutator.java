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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.relationshipgraph;

import io.github.mtrevisan.familylegacy.v2.gedcom.utils.AuditBuilder;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeChangeListener;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Handles structural modifications to the ego network graph, updating the
 * underlying {@link FLEFModel}, invalidating service indices, and notifying
 * tree listeners.
 * <p>
 * Besides creating and removing relationships, the mutator is responsible
 * for keeping the model free of dangling references when an entity is
 * removed: every association record that references the removed entity is
 * deleted together with it, so that the resulting model remains
 * consistent.
 */
class EgoNetworkMutator{

	private static final Logger LOGGER = LoggerFactory.getLogger(EgoNetworkMutator.class);


	private static final String TAG_TYPE = "type";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";
	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_INDIVIDUAL = "individual";
	private static final String TAG_GROUP = "group";


	private final FLEFModel model;
	private final EgoNetworkService networkService;
	private final TreeChangeListener listener;


	public EgoNetworkMutator(final FLEFModel model, final EgoNetworkService networkService,
			final TreeChangeListener listener){
		this.model = Objects.requireNonNull(model, "Model cannot be null");
		this.networkService = Objects.requireNonNull(networkService, "Network service cannot be null");
		this.listener = listener;
	}


	/**
	 * Navigates to a new ego entity node.
	 */
	public void navigateToEgo(final String newEgoId){
		if(StringUtils.isEmpty(newEgoId) || !model.hasRecord(newEgoId))
			return;

		notifyTreeChanged(newEgoId);
	}

	/**
	 * Creates a relationship between two entities in the network.
	 *
	 * @param subjectId the subject entity id
	 * @param targetId  the target entity id
	 * @param type      the FLEF relationship type
	 */
	public void createRelationship(final String subjectId, final String targetId, final String type){
		if(StringUtils.isEmpty(subjectId) || StringUtils.isEmpty(targetId) || StringUtils.isEmpty(type))
			return;

		final FLEFRecord relationship = FLEFRecord.createMainRecord(RelationshipHandler.TYPE,
				RelationshipHandler.ID_PREFIX, model)
			.addChild(FLEFRecord.createChildWithTagAndValue(TAG_TYPE, type))
			.addChild(FLEFRecord.createChildWithTag(TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualHandler.TYPE, subjectId))
			)
			.addChild(FLEFRecord.createChildWithTag(TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualHandler.TYPE, targetId))
			)
			.addChild(AuditBuilder.build());

		model.addRecord(relationship);
	}

	/**
	 * Removes an entity record together with every record that references it.
	 * <p>
	 * The cleanup covers:
	 * <ul>
	 *   <li>{@code relationship} records where the entity appears as
	 *       subject or target;</li>
	 *   <li>{@code event_participation} records where the entity is the
	 *       participant;</li>
	 *   <li>{@code individual_attribute} records (for individual entities)
	 *       whose owner is the entity;</li>
	 *   <li>{@code group_attribute} records (for group entities) whose
	 *       owner is the entity.</li>
	 * </ul>
	 *
	 * @param record       the entity to remove
	 * @param currentEgoId the current ego id (used to decide the fallback)
	 */
	public void removeEntity(final FLEFRecord record, final String currentEgoId){
		if(record == null)
			return;

		final String targetId = record.getId();
		if(targetId == null)
			return;

		final boolean removingCurrentEgo = Objects.equals(targetId, currentEgoId);
		final String newEgoId = (removingCurrentEgo? null: currentEgoId);

		removeRelationshipsInvolving(targetId);
		removeEventParticipationsInvolving(targetId);
		removeAttributesInvolving(targetId);

		model.removeRecord(targetId);

		invalidateAndNotifyTreeChanged(newEgoId);
	}

	/**
	 * Removes every {@code relationship} record that references the given
	 * entity as subject or target.
	 */
	private void removeRelationshipsInvolving(final String targetId){
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		final List<String> toRemove = new ArrayList<>();
		for(final FLEFRecord relationship : relationships){
			final String subjectId = relationship.extractReferencedId(TAG_SUBJECT, TAG_INDIVIDUAL);
			final String groupSubjectId = relationship.extractReferencedId(TAG_SUBJECT, TAG_GROUP);
			final String targetSubjectId = (subjectId != null? subjectId: groupSubjectId);

			final String relTargetId = relationship.extractReferencedId(TAG_TARGET, TAG_INDIVIDUAL);
			final String groupTargetId = relationship.extractReferencedId(TAG_TARGET, TAG_GROUP);
			final String targetTargetId = (relTargetId != null? relTargetId: groupTargetId);

			if(targetId.equals(targetSubjectId) || targetId.equals(targetTargetId))
				toRemove.add(relationship.getId());
		}
		for(final String id : toRemove)
			model.removeRecord(id);
	}

	/**
	 * Removes every {@code event_participation} record whose participant
	 * references the given entity.
	 */
	private void removeEventParticipationsInvolving(final String targetId){
		final List<FLEFRecord> participations = model.getRecordsByType(EventParticipationHandler.TYPE);
		final List<String> toRemove = new ArrayList<>();
		for(final FLEFRecord participation : participations){
			final FLEFRecord participantField = FLEFRecordHelper.findChild(participation, TAG_PARTICIPANT);
			if(participantField == null)
				continue;

			final FLEFRecord ref = participantField.getTheOnlyChild();
			if(ref == null || ref.getValue() == null)
				continue;

			if(targetId.equals(ref.getValue()))
				toRemove.add(participation.getId());
		}
		for(final String id : toRemove)
			model.removeRecord(id);
	}

	/**
	 * Removes every attribute record ({@code individual_attribute} or
	 * {@code group_attribute}) whose owner references the given entity.
	 */
	private void removeAttributesInvolving(final String targetId){
		removeAttributesInvolving(targetId, IndividualAttributeHandler.TYPE, TAG_INDIVIDUAL);
		removeAttributesInvolving(targetId, GroupAttributeHandler.TYPE, TAG_GROUP);
	}

	private void removeAttributesInvolving(final String targetId, final String recordType, final String ownerTag){
		final List<FLEFRecord> attributes = model.getRecordsByType(recordType);
		final List<String> toRemove = new ArrayList<>();
		for(final FLEFRecord attribute : attributes){
			final FLEFRecord ownerField = FLEFRecordHelper.findChild(attribute, ownerTag);
			if(ownerField == null)
				continue;

			final FLEFRecord ref = ownerField.getTheOnlyChild();
			if(ref == null || ref.getValue() == null)
				continue;

			if(targetId.equals(ref.getValue()))
				toRemove.add(attribute.getId());
		}
		for(final String id : toRemove)
			model.removeRecord(id);
	}

	/**
	 * Unlinks a specific relationship between two entities.
	 */
	public void unlinkRelationship(final FLEFRecord sourceRecord, final FLEFRecord targetRecord,
			final String currentEgoId){
		if(sourceRecord == null || targetRecord == null)
			return;

		final String sourceId = sourceRecord.getId();
		final String targetId = targetRecord.getId();
		if(sourceId == null || targetId == null)
			return;

		final String sourceTag = sourceRecord.getTag();
		final String targetTag = targetRecord.getTag();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		final List<String> toRemove = new ArrayList<>();
		for(final FLEFRecord relationship : relationships){
			final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
			if(type == null)
				continue;

			final String subjectId = relationship.extractReferencedId(TAG_SUBJECT, sourceTag);
			final String relTargetId = relationship.extractReferencedId(TAG_TARGET, targetTag);
			if((sourceId.equals(subjectId) && targetId.equals(relTargetId))
					|| (targetId.equals(subjectId) && sourceId.equals(relTargetId)))
				toRemove.add(relationship.getId());
		}

		removeRelationships(toRemove);
		invalidateAndNotifyTreeChanged(currentEgoId);
	}

	/**
	 * Removes a list of relationship records by their IDs and refreshes the view.
	 */
	void removeRelationships(final List<String> relationshipIds){
		if(relationshipIds == null || relationshipIds.isEmpty())
			return;

		for(final String relationshipId : relationshipIds)
			model.removeRecord(relationshipId);
	}

	private void notifyTreeChanged(final String egoId){
		LOGGER.debug("Notify ego changes to {}", egoId);

		if(listener != null)
			listener.onTreeStructureChanged(egoId);
	}

	public void invalidateAndNotifyTreeChanged(final String egoId){
		LOGGER.debug("Invalidate & Notify ego changes to {}", egoId);

		networkService.invalidateIndices();

		if(listener != null)
			listener.onTreeStructureChanged(egoId);
	}

}
