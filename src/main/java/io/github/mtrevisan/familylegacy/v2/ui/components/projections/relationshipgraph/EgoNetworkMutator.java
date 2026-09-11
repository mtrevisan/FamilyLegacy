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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Handles structural modifications to the ego network graph, updating the underlying FLEFModel,
 * invalidating service indices, and notifying tree listeners.
 */
class EgoNetworkMutator{

	private static final Logger LOGGER = LoggerFactory.getLogger(EgoNetworkMutator.class);


	private static final String TAG_TYPE = "type";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";


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
	 * Removes an entity record and all associated relationships.
	 */
	public void removeEntity(final FLEFRecord record, final String currentEgoId){
		if(record == null)
			return;

		final String targetId = record.getId();
		final String newEgoId = targetId.equals(currentEgoId) ? null : currentEgoId;

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		final List<FLEFRecord> toRemove = new ArrayList<>();
		for(final FLEFRecord relationship : relationships){
			final String subjectId = relationship.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
			final String relTargetId = relationship.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);
			if(targetId.equals(subjectId) || targetId.equals(relTargetId))
				toRemove.add(relationship);
		}
		for(final FLEFRecord relationship : toRemove)
			model.removeRecord(relationship.getId());

		model.removeRecord(targetId);

		invalidateAndNotifyTreeChanged(newEgoId);
	}

	/**
	 * Unlinks a specific relationship between two entities.
	 */
	public void unlinkRelationship(final FLEFRecord sourceRecord, final FLEFRecord targetRecord,
			final String currentEgoId){
		if(sourceRecord == null || targetRecord == null)
			return;

		final String sourceId = sourceRecord.getId();
		final String sourceTag = sourceRecord.getTag();
		final String targetId = targetRecord.getId();
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
	private void removeRelationships(final List<String> relationshipIds){
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
