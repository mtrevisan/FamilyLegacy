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

import io.github.mtrevisan.familylegacy.v2.gedcom.utils.AuditBuilder;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsData;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;


/**
 * Handles structural modifications to the biological tree with targeted delta updates
 * on the shared GenealogyRepository.
 */
public class TreeMutator{

	private static final Logger LOGGER = LoggerFactory.getLogger(TreeMutator.class);


	private static final String TAG_TYPE = "type";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";
	private static final String TAG_SEX = "sex";


	private final Predicate<String> relationshipTypeFilter;
	private final FLEFModel model;
	private final TreeService treeService;
	private final TreeChangeListener listener;


	public TreeMutator(final TreeService treeService, final TreeChangeListener listener, final FLEFModel model){
		this.relationshipTypeFilter = treeService.getRelationshipTypeFilter();
		this.model = Objects.requireNonNull(model, "Model cannot be null");
		this.treeService = Objects.requireNonNull(treeService, "Tree service cannot be null");
		this.listener = listener;
	}


	public void navigateToRoot(final String newRootIndividualId){
		if(StringUtils.isEmpty(newRootIndividualId))
			return;

		if(!model.hasRecord(newRootIndividualId))
			return;

		notifyTreeChanged(newRootIndividualId);
	}

	public void addChildToParents(final String fatherId, final String motherId, final FLEFRecord newChild,
			final String fatherRelationshipType, final String motherRelationshipType){
		if(newChild == null)
			return;

		if(fatherId != null && fatherRelationshipType != null)
			createRelationship(newChild.getId(), fatherId, fatherRelationshipType);

		if(motherId != null && motherRelationshipType != null)
			createRelationship(newChild.getId(), motherId, motherRelationshipType);
	}

	private void createRelationship(final String subjectId, final String targetId, final String type){
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

		// Targeted Delta-Update in Repository
		treeService.getRepository().notifyRelationshipAdded(subjectId, targetId, relationship.getId());
	}

	public void addParentToChild(final List<String> childrenId, final FLEFRecord newParent,
			final List<String> relationshipTypes){
		if(newParent == null || childrenId == null || childrenId.isEmpty())
			return;

		if(relationshipTypes == null || relationshipTypes.size() != childrenId.size())
			throw new IllegalArgumentException("relationshipTypes must match childrenId size");

		final String parentSex = FLEFRecordHelper.getChildValue(newParent, TAG_SEX);
		if(parentSex != null){
			final List<FLEFRecord> toRemove = new ArrayList<>();
			final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
			for(final FLEFRecord relationship : relationships){
				final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
				if(!relationshipTypeFilter.test(type))
					continue;

				final String subjectId = relationship.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
				if(!childrenId.contains(subjectId))
					continue;

				final String targetId = relationship.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);
				if(targetId == null)
					continue;

				final FLEFRecord existingParent = model.getRecordById(targetId);
				if(existingParent == null)
					continue;

				final String existingParentSex = FLEFRecordHelper.getChildValue(existingParent, TAG_SEX);
				if(parentSex.equals(existingParentSex))
					toRemove.add(relationship);
			}

			for(final FLEFRecord relationship : toRemove){
				model.removeRecord(relationship.getId());
				treeService.getRepository().notifyRelationshipRemoved(relationship.getId());
			}
		}

		for(int i = 0, size = childrenId.size(); i < size; i ++){
			final String childId = childrenId.get(i);
			final String relationshipType = relationshipTypes.get(i);
			createRelationship(childId, newParent.getId(), relationshipType);
		}
	}

	public String removeIndividual(final FLEFRecord individual, final String currentRootId){
		if(individual == null)
			return currentRootId;

		final String targetId = individual.getId();
		String newRootId = currentRootId;
		if(targetId.equals(currentRootId)){
			newRootId = findFallbackRoot(targetId);
			if(newRootId == null){
				final Map<IndividualData, SiblingsData> childrenData = treeService.buildChildrenData(targetId);
				if(!childrenData.isEmpty()){
					final SiblingsData siblings = childrenData.values().iterator().next();
					if(!siblings.getSiblings().isEmpty())
						newRootId = siblings.getSiblings().getFirst().getId();
				}
			}
		}

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		final List<FLEFRecord> relationshipsToRemove = new ArrayList<>();
		for(final FLEFRecord relationship : relationships){
			final String subjectId = relationship.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
			final String relTargetId = relationship.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);
			if(targetId.equals(subjectId) || targetId.equals(relTargetId))
				relationshipsToRemove.add(relationship);
		}
		for(final FLEFRecord relationship : relationshipsToRemove){
			model.removeRecord(relationship.getId());
			treeService.getRepository().notifyRelationshipRemoved(relationship.getId());
		}

		model.removeRecord(individual.getId());
		treeService.getRepository().invalidateIndividual(individual.getId());

		invalidateAndNotifyTreeChanged(newRootId);
		return newRootId;
	}

	private String findFallbackRoot(final String individualId){
		final String parent = findParent(individualId);
		return (parent != null? parent: findChild(individualId));
	}

	private String findParent(final String individualId){
		for(final FLEFRecord relationship : model.getRecordsByType(RelationshipHandler.TYPE)){
			final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
			if(type == null || !relationshipTypeFilter.test(type))
				continue;

			final String subjectId = relationship.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
			if(!individualId.equals(subjectId))
				continue;

			final String parentId = relationship.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);
			if(parentId != null && model.hasRecord(parentId))
				return parentId;
		}
		return null;
	}

	private String findChild(final String individualId){
		for(final FLEFRecord relationship : model.getRecordsByType(RelationshipHandler.TYPE)){
			final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
			if(type == null || !relationshipTypeFilter.test(type))
				continue;

			final String targetId = relationship.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);
			if(!individualId.equals(targetId))
				continue;

			final String childId = relationship.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
			if(childId != null && model.hasRecord(childId))
				return childId;
		}
		return null;
	}

	public void removeRelationships(final List<String> relationshipIds){
		if(relationshipIds == null || relationshipIds.isEmpty())
			return;

		for(final String relationshipId : relationshipIds){
			model.removeRecord(relationshipId);
			treeService.getRepository().notifyRelationshipRemoved(relationshipId);
		}
	}

	private void notifyTreeChanged(final String rootIndividualId){
		if(listener != null)
			listener.onTreeStructureChanged(rootIndividualId);
	}

	public void invalidateAndNotifyTreeChanged(final String rootIndividualId){
		treeService.invalidateIndices();
		if(listener != null)
			listener.onTreeStructureChanged(rootIndividualId);
	}

}
