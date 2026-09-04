package io.github.mtrevisan.familylegacy.v2.ui.components.biologicaltree;

import io.github.mtrevisan.familylegacy.v2.gedcom.utils.AuditBuilder;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.siblings.SiblingsData;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Handles structural modifications to the biological tree, updating the underlying FLEFModel,
 * invalidating service indices, and notifying tree listeners.
 */
public class AncestorTreeMutator{

	private static final Logger LOGGER = LoggerFactory.getLogger(AncestorTreeMutator.class);


	private static final String TAG_TYPE = "type";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";

	private static final String ENUM_TYPE_CHILD = "child";
	private static final String ENUM_TYPE_SPOUSE = "spouse";

	private static final String RELATIONSHIP_TYPE = "relationship";
	private static final String INDIVIDUAL_TYPE = "individual";


	private final FLEFModel model;
	private final BiologicalTreeService treeService;
	private final BiologicalTreeChangeListener listener;


	public AncestorTreeMutator(final FLEFModel model, final BiologicalTreeService treeService,
		final BiologicalTreeChangeListener listener){
		this.model = Objects.requireNonNull(model, "Model cannot be null");
		this.treeService = Objects.requireNonNull(treeService, "Tree service cannot be null");
		this.listener = listener;
	}


	/**
	 * Navigates to a new root individual node.
	 */
	public void navigateToRoot(final String newRootIndividualId){
		if(StringUtils.isEmpty(newRootIndividualId))
			return;

		if(!model.hasRecord(newRootIndividualId))
			return;

		notifyTreeChanged(newRootIndividualId);
	}

	/**
	 * Handles the post-editing process for an individual record,
	 * invalidating service caches and refreshing the UI tree structure.
	 *
	 * @param individual    the edited individual record
	 * @param currentRootId the active root ID to maintain view focus
	 */
	public void editIndividual(final FLEFRecord individual, final String currentRootId){
		if(individual == null)
			return;

		invalidateAndNotifyTreeChanged(currentRootId);
	}

	/**
	 * Removes an individual record and all associated relationship records from the model.
	 *
	 * @param individual    the record to remove
	 * @param currentRootId the current active root ID
	 */
	public void removeIndividual(final FLEFRecord individual, final String currentRootId){
		if(individual == null)
			return;

		final String targetId = individual.getId();

		// 1. Determine fallback root if removing current root
		String newRootId = currentRootId;
		if(targetId.equals(currentRootId)){
			final Map<IndividualData, SiblingsData> childrenData = treeService.buildChildrenData(targetId);
			if(!childrenData.isEmpty()){
				final SiblingsData siblings = childrenData.values().iterator().next();
				if(!siblings.getSiblings().isEmpty())
					newRootId = siblings.getSiblings()
						.getFirst()
						.getIndividualId();
			}
			else
				newRootId = null;
		}

		// 2. Remove all relationships associated with this individual
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		final List<FLEFRecord> relationshipsToRemove = new ArrayList<>();
		for(final FLEFRecord relationship : relationships){
			final String subjectId = relationship.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
			final String relTargetId = relationship.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);
			if(targetId.equals(subjectId) || targetId.equals(relTargetId))
				relationshipsToRemove.add(relationship);
		}
		for(final FLEFRecord relationship : relationshipsToRemove)
			model.removeRecord(relationship.getId());

		// 3. Remove individual record itself
		model.removeRecord(individual.getId());

		// 4. Invalidate service cache & notify UI
		invalidateAndNotifyTreeChanged(newRootId);
	}

	/**
	 * Unlinks an individual from their parent relationships (removes 'child' relationships where subject is child).
	 *
	 * @param child         the child record to unlink
	 * @param currentRootId the active root ID
	 */
	public void unlinkFromParents(final FLEFRecord child, final String currentRootId){
		if(child == null)
			return;

		final String childId = child.getId();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		final List<FLEFRecord> toRemove = new ArrayList<>();
		for(final FLEFRecord relationship : relationships){
			final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
			final String subjectId = relationship.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);

			if(type != null && type.endsWith(ENUM_TYPE_CHILD) && childId.equals(subjectId))
				toRemove.add(relationship);
		}

		for(final FLEFRecord rel : toRemove)
			model.removeRecord(rel.getId());

		invalidateAndNotifyTreeChanged(currentRootId);
	}

	/**
	 * Unlinks an individual from their partner/spouse.
	 *
	 * @param individual    the individual record to unlink
	 * @param currentRootId the active root ID to maintain view focus
	 */
	public void unlinkFromPartner(final FLEFRecord individual, final String currentRootId){
		if(individual == null)
			return;

		final String targetId = individual.getId();

		// 1. Remove partner/marriage relationships involving this individual
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		final List<FLEFRecord> toRemove = new ArrayList<>();
		for(final FLEFRecord rel : relationships){
			final String type = FLEFRecordHelper.getChildValue(rel, TAG_TYPE);
			final String subjectId = rel.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
			final String targetRefId = rel.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);

			if(type != null && type.endsWith(ENUM_TYPE_SPOUSE)
					&& (targetId.equals(subjectId) || targetId.equals(targetRefId)))
				toRemove.add(rel);
		}

		for(final FLEFRecord rel : toRemove)
			model.removeRecord(rel.getId());

		// 2. Invalidate cache & notify UI
		invalidateAndNotifyTreeChanged(currentRootId);
	}


	/**
	 * Adds a child to parent individuals by creating parent-child relationship records.
	 *
	 * @param fatherId    the ID of the male parent
	 * @param motherId  the ID of the female parent
	 * @param newChild           the child record to link
	 * @param currentRootId   the active root ID
	 */
	public void addChildToParents(final String fatherId, final String motherId, final FLEFRecord newChild,
			final String currentRootId){
		if(newChild == null)
			return;

		// 1. Ensure child record exists in model
		if(model.getRecordById(newChild.getId()) == null)
			model.addRecord(newChild);

		// 2. Create relationship record for father if present
		if(fatherId != null){
			final FLEFRecord relationship = FLEFRecord.createChildWithTag(RelationshipHandler.TYPE)
				.addChild(FLEFRecord.createChildWithTagAndValue(TAG_TYPE, ENUM_TYPE_CHILD))
				.addChild(FLEFRecord.createChildWithTag(TAG_SUBJECT)
					.addChild(FLEFRecord.createChildWithTagAndValue(IndividualHandler.TYPE, newChild.getId()))
				)
				.addChild(FLEFRecord.createChildWithTag(TAG_TARGET)
					.addChild(FLEFRecord.createChildWithTagAndValue(IndividualHandler.TYPE, fatherId))
				);
			model.addRecord(relationship);
		}

		// 3. Create relationship record for mother if present
		if(motherId != null){
			final FLEFRecord relationship = FLEFRecord.createChildWithTag(RelationshipHandler.TYPE)
				.addChild(FLEFRecord.createChildWithTagAndValue(TAG_TYPE, ENUM_TYPE_CHILD))
				.addChild(FLEFRecord.createChildWithTag(TAG_SUBJECT)
					.addChild(FLEFRecord.createChildWithTagAndValue(IndividualHandler.TYPE, newChild.getId()))
				)
				.addChild(FLEFRecord.createChildWithTag(TAG_TARGET)
					.addChild(FLEFRecord.createChildWithTagAndValue(IndividualHandler.TYPE, motherId))
				);
			model.addRecord(relationship);
		}

		// 4. Refresh service state
		invalidateAndNotifyTreeChanged(currentRootId);
	}


	/**
	 * Adds or links a partner to a target individual.
	 * Creates a spouse/partner relationship between the target and the new partner.
	 *
	 * @param partnerId     the ID of the individual to whom the partner will be added
	 * @param newPartner    the partner record (may already exist in the model)
	 * @param currentRootId the current root individual ID (used to refresh the tree)
	 */
	public void addPartnerToIndividual(final String partnerId, final FLEFRecord newPartner, final String currentRootId){
		if(newPartner == null)
			return;

		// Get the target individual record
		final FLEFRecord target = model.getRecordById(partnerId);
		if(target == null)
			throw new IllegalArgumentException("Target individual not found: " + partnerId);

		// Create spouse relationships (bidirectional)
		createRelationship(target, newPartner, "spouse");
		createRelationship(newPartner, target, "spouse");

		// Invalidate cached indices in the tree service and notify the listener to refresh the tree, keeping the
		// current root
		invalidateAndNotifyTreeChanged(currentRootId);
	}

	/**
	 * Helper method to create a relationship record of the given type.
	 * Assumes the model uses a record structure with "relationship" tag and
	 * children: "type", "subject", "target" (each referencing an individual).
	 *
	 * @param subject the individual that is the subject of the relationship
	 * @param target  the individual that is the target of the relationship
	 * @param type    the type of relationship (e.g., "child", "spouse", "parent")
	 */
	private void createRelationship(final FLEFRecord subject, final FLEFRecord target, final String type){
		final FLEFRecord relationship = FLEFRecord.createMainRecord(RelationshipHandler.TYPE, model)
			.addChild(FLEFRecord.createChildWithTagAndValue("type", type))
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", subject.getId()))
			)
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", target.getId()))
			)
			.addChild(AuditBuilder.build());

		model.addRecord(relationship);
	}


	/**
	 * Adds or links a parent to a target child individual.
	 */
	public void addParentToChild(final String childId, final FLEFRecord newParent, final String currentRootId){
//		if(parentRecord == null || childId == null)
//			return;
//
//		if(model.getRecordById(parentRecord.getId()) == null)
//			model.addRecord(parentRecord);
//
//		final String sex = IndividualHandler.getInstance()
//			.getSex(parentRecord, model);
//		if("female".equals(sex))
//			addChildToParents(null, parentRecord.getId(), model.getRecordById(childId), currentRootId);
//		else
//			addChildToParents(parentRecord.getId(), null, model.getRecordById(childId), currentRootId);
	}


	/**
	 * Unlinks an individual from their previous relations and connects them to new parents or partner.
	 *
	 * @param sourceRecord  the individual being moved
	 * @param fatherId      the new father ID (can be null)
	 * @param motherId      the new mother ID (can be null)
	 * @param partnerId     the new partner ID (can be null)
	 * @param currentRootId the active root ID
	 */
	public void moveAndLinkIndividual(final FLEFRecord sourceRecord, final String fatherId,
			final String motherId, final String partnerId, final String currentRootId){
//		if(sourceRecord == null)
//			return;
//
//		// 1. Unlink from current parents and partner
//		unlinkFromParents(sourceRecord, null);
//		unlinkFromPartner(sourceRecord, null);
//
//		// 2. Link to new parents if specified
//		if(fatherId != null || motherId != null){
//			addChildToParents(fatherId, motherId, sourceRecord, null);
//		}
//
//		// 3. Link to new partner if specified
//		if(partnerId != null){
//			addPartnerRelationship(sourceRecord.getId(), partnerId);
//		}
//
//		// 4. Invalidate and refresh UI once
//		invalidateAndNotifyTreeChanged(currentRootId);
	}


	/* TODO UNTESTED */
	/**
	 * Case 2: Moves a parent node from a source child to a target child.
	 */
	public boolean moveAncestorSubtree(final AncestorNode sourceChild, final boolean sourceIsFather,
			final AncestorNode targetChild, final boolean targetAsFather, final String currentRootId){
		if(sourceChild == null || targetChild == null)
			return false;

		final AncestorNode movingNode = sourceIsFather ? sourceChild.getFather() : sourceChild.getMother();
		if(movingNode == null)
			return false;

		final String childIdSource = sourceChild.getIndividualId();
		final String childIdTarget = targetChild.getIndividualId();
		final String movingParentId = movingNode.getIndividualId();

		if(childIdSource != null && movingParentId != null)
			removeRelationshipRecord(childIdSource, movingParentId);
		if(childIdTarget != null && movingParentId != null)
			addOrUpdateRelationshipRecord(childIdTarget, movingParentId);

		invalidateAndNotifyTreeChanged(currentRootId);

		return true;
	}

	/* TODO UNTESTED */
	/**
	 * Case 3: Swaps biological parent roles under the specified child node.
	 */
	public boolean swapParentsRoles(final AncestorNode childNode, final String currentRootId){
		if(childNode == null)
			return false;

		final AncestorNode father = childNode.getFather();
		final AncestorNode mother = childNode.getMother();

		if(father == null || mother == null)
			return false;

		final String childId = childNode.getIndividualId();
		final String fatherId = father.getIndividualId();
		final String motherId = mother.getIndividualId();

		if(childId != null){
			removeRelationshipRecord(childId, fatherId);
			removeRelationshipRecord(childId, motherId);

			// Re-assign in swapped positions
			addOrUpdateRelationshipRecord(childId, motherId);
			addOrUpdateRelationshipRecord(childId, fatherId);
		}

		invalidateAndNotifyTreeChanged(currentRootId);

		return true;
	}

	/* TODO UNTESTED */
	/**
	 * Case 4: Reverses direct parent-child relationship (child becomes parent of parent).
	 */
	public boolean invertParentChildRelationship(final AncestorNode parentNode, final boolean isFather,
			final String currentRootId){
		if(parentNode == null)
			return false;

		final AncestorNode childNode = (isFather? parentNode.getFather(): parentNode.getMother());
		if(childNode == null)
			return false;

		final String parentId = parentNode.getIndividualId();
		final String childId = childNode.getIndividualId();

		if(parentId != null && childId != null){
			removeRelationshipRecord(parentId, childId);
			addOrUpdateRelationshipRecord(parentId, childId);
		}

		invalidateAndNotifyTreeChanged(currentRootId);

		return true;
	}


	// --------------------------------------------------------------------------------
	// Helpers
	// --------------------------------------------------------------------------------

	private void removeRelationshipRecord(final String childId, final String parentId){
		final List<FLEFRecord> relationships = new ArrayList<>(model.getRecordsByType(RELATIONSHIP_TYPE));
		for(final FLEFRecord relationship : relationships){
			final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
			if(type != null && type.endsWith(ENUM_TYPE_CHILD)){
				final String subjectId = relationship.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
				final String targetId = relationship.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);

				if(Objects.equals(subjectId, childId) && Objects.equals(targetId, parentId))
					model.removeRecord(relationship.getId());
			}
		}
	}

	private void addOrUpdateRelationshipRecord(final String childId, final String parentId){
		removeRelationshipRecord(childId, parentId);

		final FLEFRecord relationshipRecord = FLEFRecord.createMainRecord(null, RELATIONSHIP_TYPE);
		FLEFRecordHelper.addChildValue(relationshipRecord, TAG_TYPE, ENUM_TYPE_CHILD);

		final FLEFRecord subjectRecord = FLEFRecord.createChildWithTag(TAG_SUBJECT);
		FLEFRecordHelper.addChildValue(subjectRecord, INDIVIDUAL_TYPE, childId);
		relationshipRecord.addChild(subjectRecord);

		final FLEFRecord targetRecord = FLEFRecord.createChildWithTag(TAG_TARGET);
		FLEFRecordHelper.addChildValue(targetRecord, INDIVIDUAL_TYPE, parentId);
		relationshipRecord.addChild(targetRecord);

		model.addRecord(relationshipRecord);
	}

	private void notifyTreeChanged(final String rootIndividualId){
		LOGGER.debug("Notify root changes to {}", rootIndividualId);

		if(listener != null)
			listener.onTreeStructureChanged(rootIndividualId);
	}

	private void invalidateAndNotifyTreeChanged(final String rootIndividualId){
		LOGGER.debug("Invalidate & Notify root changes to {}", rootIndividualId);

		treeService.invalidateIndices();

		if(listener != null)
			listener.onTreeStructureChanged(rootIndividualId);
	}

}
