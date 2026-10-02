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
package io.github.mtrevisan.familylegacy.ui.components.projections.repository;

import io.github.mtrevisan.familylegacy.gedcom.utils.AuditBuilder;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Base implementation providing common FLEFModel mutation operations for projection mutators.
 */
public abstract class AbstractProjectionMutator implements ProjectionMutator{

	protected final FLEFModel model;
	protected final TreeChangeListener listener;


	protected AbstractProjectionMutator(final FLEFModel model, final TreeChangeListener listener){
		this.model = Objects.requireNonNull(model, "Model cannot be null");
		this.listener = listener;
	}


	@Override
	public void createRelationship(final String subjectId, final String targetId, final String type){
		if(StringUtils.isEmpty(subjectId) || StringUtils.isEmpty(targetId) || StringUtils.isEmpty(type))
			return;

		final FLEFRecord relationship = FLEFRecord.createMainRecord(RelationshipHandler.TYPE,
				RelationshipHandler.ID_PREFIX, model)
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, type))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualHandler.TYPE, subjectId))
			)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_OBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualHandler.TYPE, targetId))
			)
			.addChild(AuditBuilder.build());

		model.addRecord(relationship);

		onRelationshipAdded(subjectId, targetId, relationship.getId());
	}

	@Override
	public void removeRelationships(final List<String> relationshipIds){
		if(relationshipIds == null || relationshipIds.isEmpty())
			return;

		for(int i = 0, size = relationshipIds.size(); i < size; i ++){
			final String relationshipId = relationshipIds.get(i);

			model.removeRecord(relationshipId);

			onRelationshipRemoved(relationshipId);
		}

		invalidateCaches();
	}

	/**
	 * Invalidates every cache that depends on the relationship graph.
	 * Subclasses override to clear their local caches in addition to the
	 * shared repository.
	 */
	protected void invalidateCaches(){
		// Default: nothing. Subclasses with local caches must override.
	}

	@Override
	public void removeEntity(final FLEFRecord record, final String currentFocusId){
		if(record == null)
			return;

		final String targetId = record.getId();
		if(targetId == null)
			return;

		final boolean removingCurrentFocus = Objects.equals(targetId, currentFocusId);
		final String newFocusId = (removingCurrentFocus? getFallbackFocusId(targetId, currentFocusId): currentFocusId);

		removeRelationshipsInvolving(targetId);
		removeEventParticipationsInvolving(targetId);
		removeAttributesInvolving(targetId);

		model.removeRecord(targetId);
		onEntityRemoved(targetId);

		invalidateAndNotifyTreeChanged(newFocusId);
	}

	/**
	 * Removes every {@code relationship} record that references the given entity as subject or target.
	 */
	protected void removeRelationshipsInvolving(final String targetId){
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		final List<String> toRemove = new ArrayList<>();
		for(int i = 0, size = relationships.size(); i < size; i ++){
			final FLEFRecord relationship = relationships.get(i);

			final String subjectId = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualHandler.TYPE);
			final String groupSubjectId = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, GroupHandler.TYPE);
			final String targetSubjectId = (subjectId != null? subjectId: groupSubjectId);

			final String relationshipObjectId = relationship.extractReferencedId(RelationshipReader.TAG_OBJECT, IndividualHandler.TYPE);
			final String groupObjectId = relationship.extractReferencedId(RelationshipReader.TAG_OBJECT, GroupHandler.TYPE);
			final String targetObjectId = (relationshipObjectId != null? relationshipObjectId: groupObjectId);

			if(targetId.equals(targetSubjectId) || targetId.equals(targetObjectId))
				toRemove.add(relationship.getId());
		}
		removeRelationships(toRemove);
	}

	/**
	 * Removes every {@code event_participation} record whose participant references the given entity.
	 */
	protected void removeEventParticipationsInvolving(final String targetId){
		final List<FLEFRecord> participations = model.getRecordsByType(EventParticipationHandler.TYPE);
		final List<String> toRemove = new ArrayList<>();
		for(int i = 0, size = participations.size(); i < size; i ++){
			final FLEFRecord participation = participations.get(i);

			final FLEFRecord participantField = FLEFRecordHelper.findChild(participation, EventParticipationReader.TAG_PARTICIPANT);
			if(participantField == null)
				continue;

			final FLEFRecord ref = participantField.getTheOnlyChild();
			if(ref == null || ref.getValue() == null)
				continue;

			if(targetId.equals(ref.getValue()))
				toRemove.add(participation.getId());
		}
		for(int i = 0, size = toRemove.size(); i < size; i ++)
			model.removeRecord(toRemove.get(i));
	}

	/**
	 * Removes every attribute record ({@code individual_attribute} or {@code group_attribute})
	 * whose owner references the given entity.
	 */
	protected void removeAttributesInvolving(final String targetId){
		removeAttributesInvolving(targetId, IndividualAttributeHandler.TYPE, IndividualHandler.TYPE);
		removeAttributesInvolving(targetId, GroupAttributeHandler.TYPE, GroupHandler.TYPE);
	}

	private void removeAttributesInvolving(final String targetId, final String recordType, final String ownerTag){
		final List<FLEFRecord> attributes = model.getRecordsByType(recordType);
		final List<String> toRemove = new ArrayList<>();
		for(int i = 0, size = attributes.size(); i < size; i ++){
			final FLEFRecord attribute = attributes.get(i);

			final FLEFRecord ownerField = FLEFRecordHelper.findChild(attribute, ownerTag);
			if(ownerField == null)
				continue;

			final FLEFRecord ref = ownerField.getTheOnlyChild();
			if(ref == null || ref.getValue() == null)
				continue;

			if(targetId.equals(ref.getValue()))
				toRemove.add(attribute.getId());
		}
		for(int i = 0, size = toRemove.size(); i < size; i ++)
			model.removeRecord(toRemove.get(i));
	}

	protected void notifyTreeChanged(final String focusId){
		if(listener != null)
			listener.onTreeStructureChanged(focusId);
	}

	// Callback hooks for concrete repository notifications
	protected abstract void onRelationshipAdded(String subjectId, String targetId, String relationshipId);

	protected abstract void onRelationshipRemoved(String relationshipId);

	protected abstract void onEntityRemoved(String entityId);

	protected abstract String getFallbackFocusId(String targetId, String currentFocusId);

}
