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
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.relationshipgraph.EgoNetworkService;
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
 */
public class EgoNetworkMutator extends AbstractProjectionMutator{

	private static final Logger LOGGER = LoggerFactory.getLogger(EgoNetworkMutator.class);

	private final EgoNetworkService networkService;


	public EgoNetworkMutator(final FLEFModel model, final EgoNetworkService networkService,
			final TreeChangeListener listener){
		super(model, listener);

		this.networkService = Objects.requireNonNull(networkService, "Network service cannot be null");
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
		for(int i = 0, size = relationships.size(); i < size; i ++){
			final FLEFRecord relationship = relationships.get(i);

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

	@Override
	protected void onRelationshipAdded(final String subjectId, final String targetId, final String relationshipId){
		// Delta update on repository
		networkService.getRepository()
			.notifyRelationshipAdded(subjectId, targetId, relationshipId);
	}

	@Override
	protected void onRelationshipRemoved(final String relationshipId){
		networkService.getRepository()
			.notifyRelationshipRemoved(relationshipId);
	}

	@Override
	protected void onEntityRemoved(final String entityId){
		networkService.getRepository()
			.invalidateIndividual(entityId);
	}

	@Override
	protected String getFallbackFocusId(final String targetId, final String currentFocusId){
		return (Objects.equals(targetId, currentFocusId)? null: currentFocusId);
	}

	@Override
	public void invalidateAndNotifyTreeChanged(final String egoId){
		LOGGER.debug("Invalidate & Notify ego changes to {}", egoId);

		networkService.invalidateIndices();

		if(listener != null)
			listener.onTreeStructureChanged(egoId);
	}

}
