package io.github.mtrevisan.familylegacy.v2.ui.components.biologicaltree;

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
					newRootId = siblings.getSiblings().getFirst().getIndividualId();
			}
			else
				newRootId = null;
		}

		// 2. Remove all relationships associated with this individual
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		final List<FLEFRecord> toRemove = new ArrayList<>();
		for(final FLEFRecord rel : relationships){
			final String subjectId = extractReferencedId(rel, TAG_SUBJECT);
			final String relTargetId = extractReferencedId(rel, TAG_TARGET);
			if(targetId.equals(subjectId) || targetId.equals(relTargetId))
				toRemove.add(rel);
		}
		for(final FLEFRecord rel : toRemove)
			model.removeRecord(rel.getId());

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
		for(final FLEFRecord rel : relationships){
			final String type = FLEFRecordHelper.getChildValue(rel, TAG_TYPE);
			final String subjectId = extractReferencedId(rel, TAG_SUBJECT);

			if(type != null && type.endsWith(ENUM_TYPE_CHILD) && childId.equals(subjectId))
				toRemove.add(rel);
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
			final String subjectId = extractReferencedId(rel, TAG_SUBJECT);
			final String targetRefId = extractReferencedId(rel, TAG_TARGET);

			if(type != null && !type.endsWith(ENUM_TYPE_CHILD)
					&& (targetId.equals(subjectId) || targetId.equals(targetRefId)))
				toRemove.add(rel);
		}

		for(final FLEFRecord rel : toRemove)
			model.removeRecord(rel.getId());

		// 2. Invalidate cache & notify UI
		invalidateAndNotifyTreeChanged(currentRootId);
	}

	/**
	 * Adds a child to a parent individual by creating a new parent-child relationship record.
	 *
	 * @param parentId      the ID of the parent
	 * @param child         the child record to link
	 * @param currentRootId the active root ID
	 */
	public void addChildToIndividual(final String parentId, final FLEFRecord child, final String currentRootId){
		if(parentId == null || child == null)
			return;

		// 1. Ensure child record exists in model
		if(model.getRecordById(child.getId()) == null)
			model.addRecord(child);

		// 2. Create relationship record: Subject = Child, Target = Parent, Type = child
		final FLEFRecord relRecord = FLEFRecord.createChildWithTag(RelationshipHandler.TYPE);

		final FLEFRecord typeRecord = FLEFRecord.createChildWithTagAndValue(TAG_TYPE, ENUM_TYPE_CHILD);
		relRecord.addChild(typeRecord);

		final FLEFRecord subjectRecord = FLEFRecord.createChildWithTag(TAG_SUBJECT);
		final FLEFRecord subjectRef = FLEFRecord.createChildWithTagAndValue(IndividualHandler.TYPE, child.getId());
		subjectRecord.addChild(subjectRef);
		relRecord.addChild(subjectRecord);

		final FLEFRecord targetRecord = FLEFRecord.createChildWithTag(TAG_TARGET);
		final FLEFRecord targetRef = FLEFRecord.createChildWithTagAndValue(IndividualHandler.TYPE, parentId);
		targetRecord.addChild(targetRef);
		relRecord.addChild(targetRecord);

		model.addRecord(relRecord);

		// 3. Refresh service state
		invalidateAndNotifyTreeChanged(currentRootId);
	}


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

		final String childIdSource = getIndividualId(sourceChild);
		final String childIdTarget = getIndividualId(targetChild);
		final String movingParentId = getIndividualId(movingNode);

		if(childIdSource != null && movingParentId != null)
			removeRelationshipRecord(childIdSource, movingParentId);
		if(childIdTarget != null && movingParentId != null)
			addOrUpdateRelationshipRecord(childIdTarget, movingParentId);

		invalidateAndNotifyTreeChanged(currentRootId);

		return true;
	}

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

		final String childId = getIndividualId(childNode);
		final String fatherId = getIndividualId(father);
		final String motherId = getIndividualId(mother);

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

		final String parentId = getIndividualId(parentNode);
		final String childId = getIndividualId(childNode);

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
		for(final FLEFRecord rel : relationships){
			final String type = FLEFRecordHelper.getChildValue(rel, TAG_TYPE);
			if(type != null && type.endsWith(ENUM_TYPE_CHILD)){
				final String subjectId = extractReferencedId(rel, TAG_SUBJECT);
				final String targetId = extractReferencedId(rel, TAG_TARGET);

				if(Objects.equals(subjectId, childId) && Objects.equals(targetId, parentId))
					model.removeRecord(rel.getId());
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

	private String extractReferencedId(final FLEFRecord record, final String fieldTag){
		final FLEFRecord field = FLEFRecordHelper.findChild(record, fieldTag);
		if(field == null)
			return null;

		final FLEFRecord ref = field.getTheOnlyChild();
		return (ref != null? ref.getValue(): null);
	}

	static String getIndividualId(final AncestorNode node){
		if(node == null)
			return null;

		if(node.getIndividual() != null)
			return node.getIndividual()
				.getId();

		final IndividualData individualData = node.getIndividualData();
		return (individualData != null? individualData.getIndividualId(): null);
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
