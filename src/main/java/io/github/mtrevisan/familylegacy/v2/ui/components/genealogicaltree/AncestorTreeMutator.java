package io.github.mtrevisan.familylegacy.v2.ui.components.genealogicaltree;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.biologicalparents.BiologicalParentsData;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;


/**
 * Utility class handling all structural movement operations for {@link AncestorNode} trees in memory with automatic
 * bidirectional synchronization to {@link FLEFModel}.
 */
public final class AncestorTreeMutator{

	private static final String TAG_TYPE = "type";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";
	private static final String TAG_SEX = "sex";

	private static final String ENUM_TYPE_CHILD = "child";
	private static final String ENUM_SEX_MALE = "male";
	private static final String ENUM_SEX_FEMALE = "female";


	private AncestorTreeMutator(){}


	/**
	 * Case 1: Move a parent node (and its entire upper ancestor subtree)
	 * from a source child node to a target child node as father or mother.
	 */
	public static boolean moveAncestorSubtree(final AncestorNode sourceChild, final boolean sourceIsFather,
			final AncestorNode targetChild, final boolean targetAsFather, final FLEFModel model){
		if(sourceChild == null || targetChild == null || model == null)
			return false;

		final AncestorNode movingNode = sourceIsFather ? sourceChild.getFather() : sourceChild.getMother();
		if(movingNode == null)
			return false;

		// Prevent cycle creation: targetChild cannot be inside movingNode's subtree
		if(isDescendantOf(targetChild, movingNode))
			return false;

		final String childIdSource = getIndividualId(sourceChild);
		final String childIdTarget = getIndividualId(targetChild);
		final String movingParentId = getIndividualId(movingNode);

		// 1. Update FLEFModel records
		if(childIdSource != null && movingParentId != null)
			removeRelationshipRecord(childIdSource, movingParentId, model);
		if(childIdTarget != null && movingParentId != null)
			addOrUpdateRelationshipRecord(childIdTarget, movingParentId, model);

		// Detach from source
		if(sourceIsFather)
			sourceChild.setFather(null);
		else
			sourceChild.setMother(null);

		// Attach to target
		if(targetAsFather)
			targetChild.setFather(movingNode);
		else
			targetChild.setMother(movingNode);

		recalculateGenerationsIterative(movingNode, targetChild.getGeneration() + 1);
		updateBiologicalParentsData(sourceChild, model);
		updateBiologicalParentsData(targetChild, model);

		return true;
	}

	/**
	 * Case 2: Swap biological parent roles (Father <-> Mother) under the same child node.
	 */
	public static boolean swapParentsRoles(final AncestorNode childNode, final FLEFModel model){
		if(childNode == null || model == null)
			return false;

		final AncestorNode currentFather = childNode.getFather();
		final AncestorNode currentMother = childNode.getMother();

		final String childId = getIndividualId(childNode);
		final String fatherId = getIndividualId(currentFather);
		final String motherId = getIndividualId(currentMother);

		// Update model genders/roles if relationships exist
		if(childId != null){
			if(fatherId != null)
				addOrUpdateRelationshipRecord(childId, fatherId, model);
			if(motherId != null)
				addOrUpdateRelationshipRecord(childId, motherId, model);
		}

		childNode.setFather(currentMother);
		childNode.setMother(currentFather);

		updateBiologicalParentsData(childNode, model);
		return true;
	}

	/**
	 * Case 3: Promote an ancestor node to become the new Root Node (Root Swap).
	 * Re-roots the tree layout around the specified ancestor.
	 *
	 * @return the new root AncestorNode
	 */
	public static AncestorNode promoteToRoot(final AncestorNode rootNode, final AncestorNode newRootTarget,
			final FLEFModel model){
		if(rootNode == null || newRootTarget == null)
			return rootNode;

		// Verify that target exists within the current tree
		if(findNodeById(rootNode, getIndividualId(newRootTarget)) == null)
			return rootNode;

		// Recalculate generations relative to new root (generation 0)
		recalculateGenerationsIterative(newRootTarget, 0);
		updateBiologicalParentsData(newRootTarget, model);

		return newRootTarget;
	}

	/**
	 * Case 4: Reverse hierarchy relationship (Parent-Child Swap).
	 * Makes a child node the parent of its direct parent node.
	 */
	public static boolean invertParentChildRelationship(final AncestorNode parentNode, final boolean isFather,
			final FLEFModel model){
		if(parentNode == null || model == null)
			return false;

		final AncestorNode childNode = isFather ? parentNode.getFather() : parentNode.getMother();
		if(childNode == null)
			return false;

		final String parentId = getIndividualId(parentNode);
		final String childId = getIndividualId(childNode);
		if(parentId != null && childId != null){
			// Remove old parent -> child relation
			removeRelationshipRecord(parentId, childId, model);
			// Invert relation: old child becomes new parent of old parent
			addOrUpdateRelationshipRecord(parentId, childId, model);
		}

		// Detach child from parent
		if(isFather)
			parentNode.setFather(null);
		else
			parentNode.setMother(null);

		// Make parent node an ancestor (father) of childNode
		childNode.setFather(parentNode);

		recalculateGenerationsIterative(childNode, parentNode.getGeneration());
		recalculateGenerationsIterative(parentNode, childNode.getGeneration() + 1);

		updateBiologicalParentsData(parentNode, model);
		updateBiologicalParentsData(childNode, model);

		return true;
	}


	// --------------------------------------------------------------------------------
	// FLEFModel Synchronization Helpers
	// --------------------------------------------------------------------------------

	private static void removeRelationshipRecord(final String childId, final String parentId, final FLEFModel model){
		final List<FLEFRecord> relationships = new ArrayList<>(model.getRecordsByType(RelationshipHandler.TYPE));
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

	//TODO ask for relationship type?
	private static void addOrUpdateRelationshipRecord(final String childId, final String parentId,
			final FLEFModel model){
		// Remove pre-existing relationship if present
		removeRelationshipRecord(childId, parentId, model);

		// Build new relationship record within model
		final FLEFRecord relationshipRecord = FLEFRecord.createChildWithTag(RelationshipHandler.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(TAG_TYPE, ENUM_TYPE_CHILD))
			.addChild(FLEFRecord.createChildWithTag(TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualHandler.TYPE, childId))
			)
			.addChild(FLEFRecord.createChildWithTag(TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualHandler.TYPE, parentId))
			);

		model.addRecord(relationshipRecord);
	}

	private static String extractReferencedId(final FLEFRecord record, final String fieldTag){
		final FLEFRecord field = FLEFRecordHelper.findChild(record, fieldTag);
		if(field == null)
			return null;

		final FLEFRecord ref = field.getTheOnlyChild();
		return (ref != null? ref.getValue(): null);
	}


	// --------------------------------------------------------------------------------
	// Tree Traversal & Structural Helpers
	// --------------------------------------------------------------------------------

	/**
	 * Finds a node in the tree matching the target individual Record ID using BFS iteration.
	 */
	public static AncestorNode findNodeById(final AncestorNode rootNode, final String individualId){
		if(rootNode == null || individualId == null)
			return null;

		final Queue<AncestorNode> queue = new ArrayDeque<>();
		queue.add(rootNode);
		while(!queue.isEmpty()){
			final AncestorNode current = queue.poll();
			if(Objects.equals(getIndividualId(current), individualId))
				return current;

			if(current.getFather() != null)
				queue.add(current.getFather());
			if(current.getMother() != null)
				queue.add(current.getMother());
		}
		return null;
	}

	/**
	 * Iteratively recalculates generation levels for a subtree without recursion.
	 */
	public static void recalculateGenerationsIterative(final AncestorNode startNode, final int startGeneration){
		if(startNode == null)
			return;

		final Queue<AncestorNode> nodeQueue = new ArrayDeque<>();
		final Queue<Integer> genQueue = new ArrayDeque<>();
		nodeQueue.add(startNode);
		genQueue.add(startGeneration);
		while(!nodeQueue.isEmpty()){
			final AncestorNode current = nodeQueue.poll();
			@SuppressWarnings("DataFlowIssue")
			final int currentGen = genQueue.poll();

			current.setGeneration(currentGen);

			final int nextGen = currentGen + 1;
			if(current.getFather() != null){
				nodeQueue.add(current.getFather());
				genQueue.add(nextGen);
			}
			if(current.getMother() != null){
				nodeQueue.add(current.getMother());
				genQueue.add(nextGen);
			}
		}
	}

	/**
	 * Checks if candidateNode is contained within ancestorSubtree to prevent cyclic graph dependencies.
	 */
	public static boolean isDescendantOf(final AncestorNode candidateNode, final AncestorNode ancestorSubtree){
		if(candidateNode == null || ancestorSubtree == null)
			return false;

		final Set<AncestorNode> visited = new HashSet<>();
		final Queue<AncestorNode> queue = new ArrayDeque<>();
		queue.add(ancestorSubtree);
		while(!queue.isEmpty()){
			final AncestorNode current = queue.poll();
			if(current == candidateNode)
				return true;

			if(visited.add(current)){
				if(current.getFather() != null)
					queue.add(current.getFather());
				if(current.getMother() != null)
					queue.add(current.getMother());
			}
		}
		return false;
	}

	private static String getIndividualId(final AncestorNode node){
		return (node != null && node.getIndividual() != null
			? node.getIndividual().getId()
			: null);
	}

	private static void updateBiologicalParentsData(final AncestorNode node, final FLEFModel model){
		if(node != null && model != null){
			final BiologicalParentsData data = BiologicalParentsData.create(null, model);
			node.setBiologicalParentsData(data);
		}
	}

}
