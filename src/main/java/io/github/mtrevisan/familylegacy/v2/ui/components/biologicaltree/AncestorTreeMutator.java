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
	private static final String TAG_SEX = "sex";

	private static final String ENUM_TYPE_CHILD = "child";
	private static final String ENUM_TYPE_PARTNER = "partner";


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
	 * Adds a child to parent individuals by creating parent-child relationship records.
	 *
	 * @param fatherId    the ID of the male parent
	 * @param motherId  the ID of the female parent
	 * @param newChild           the child record to link
	 */
	public void addChildToParents(final String fatherId, final String motherId, final FLEFRecord newChild){
		if(newChild == null)
			return;

		// Create relationship record for father if present
		if(fatherId != null){
			final FLEFRecord relationship = FLEFRecord.createMainRecord(RelationshipHandler.TYPE, model)
				.addChild(FLEFRecord.createChildWithTagAndValue(TAG_TYPE, ENUM_TYPE_CHILD))
				.addChild(FLEFRecord.createChildWithTag(TAG_SUBJECT)
					.addChild(FLEFRecord.createChildWithTagAndValue(IndividualHandler.TYPE, newChild.getId()))
				)
				.addChild(FLEFRecord.createChildWithTag(TAG_TARGET)
					.addChild(FLEFRecord.createChildWithTagAndValue(IndividualHandler.TYPE, fatherId))
				);
			model.addRecord(relationship);
		}

		// Create relationship record for mother if present
		if(motherId != null){
			final FLEFRecord relationship = FLEFRecord.createMainRecord(RelationshipHandler.TYPE, model)
				.addChild(FLEFRecord.createChildWithTagAndValue(TAG_TYPE, ENUM_TYPE_CHILD))
				.addChild(FLEFRecord.createChildWithTag(TAG_SUBJECT)
					.addChild(FLEFRecord.createChildWithTagAndValue(IndividualHandler.TYPE, newChild.getId()))
				)
				.addChild(FLEFRecord.createChildWithTag(TAG_TARGET)
					.addChild(FLEFRecord.createChildWithTagAndValue(IndividualHandler.TYPE, motherId))
				);
			model.addRecord(relationship);
		}
	}

	/**
	 * Adds or links a parent to a target child individual.
	 * If a parent of the same sex already exists, it is replaced.
	 *
	 * @param childId       the ID of the child
	 * @param newParent     the parent record to add (must have sex defined)
	 */
	public void addParentToChild(final String childId, final FLEFRecord newParent){
		if(newParent == null)
			return;

		// Get the target individual record
		final FLEFRecord child = model.getRecordById(childId);
		if(child == null)
			throw new IllegalArgumentException("Child individual not found: " + childId);

		// Ensure parent exists in model
		if(!model.hasRecord(newParent.getId()))
			model.addRecord(newParent);

		// Get parent sex from the record
		final String parentSex = FLEFRecordHelper.getChildValue(newParent, TAG_SEX);
		if(parentSex != null){
			// Find and remove existing parent of the same sex
			final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
			final List<FLEFRecord> toRemove = new ArrayList<>();
			for(final FLEFRecord relationship : relationships){
				final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
				if(!ENUM_TYPE_CHILD.equals(type))
					continue;

				final String subjectId = relationship.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
				if(!childId.equals(subjectId))
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
			for(final FLEFRecord relationship : toRemove)
				model.removeRecord(relationship.getId());
		}

		// Create new child relationship
		createRelationship(child, newParent, ENUM_TYPE_CHILD);
	}

	/**
	 * Adds or links a partner to a target individual.
	 * Creates a spouse/partner relationship between the target and the new partner.
	 *
	 * @param partnerId     the ID of the individual to whom the partner will be added
	 * @param newPartner    the partner record (may already exist in the model)
	 */
	public void addPartnerToIndividual(final String partnerId, final FLEFRecord newPartner){
		if(newPartner == null)
			return;

		// Get the target individual record
		final FLEFRecord target = model.getRecordById(partnerId);
		if(target == null)
			throw new IllegalArgumentException("Target individual not found: " + partnerId);

		// Create spouse relationships (bidirectional)
		createRelationship(target, newPartner, ENUM_TYPE_PARTNER);
		createRelationship(newPartner, target, ENUM_TYPE_PARTNER);
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

		// Determine fallback root if removing current root
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

		// Remove all relationships associated with this individual
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

		// Remove individual record itself
		model.removeRecord(individual.getId());

		// Invalidate service cache & notify UI
		invalidateAndNotifyTreeChanged(newRootId);
	}

	/**
	 * Removes a list of relationship records by their IDs and refreshes the tree.
	 *
	 * @param relationshipIds list of relationship record IDs to remove
	 */
	public void removeRelationships(final List<String> relationshipIds){
		if(relationshipIds == null || relationshipIds.isEmpty())
			return;

		for(final String relationshipId : relationshipIds)
			model.removeRecord(relationshipId);
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
			.addChild(FLEFRecord.createChildWithTagAndValue(TAG_TYPE, type))
			.addChild(FLEFRecord.createChildWithTag(TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualHandler.TYPE, subject.getId()))
			)
			.addChild(FLEFRecord.createChildWithTag(TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualHandler.TYPE, target.getId()))
			)
			.addChild(AuditBuilder.build());

		model.addRecord(relationship);
	}

	private void notifyTreeChanged(final String rootIndividualId){
		LOGGER.debug("Notify root changes to {}", rootIndividualId);

		if(listener != null)
			listener.onTreeStructureChanged(rootIndividualId);
	}

	void invalidateAndNotifyTreeChanged(final String rootIndividualId){
		LOGGER.debug("Invalidate & Notify root changes to {}", rootIndividualId);

		treeService.invalidateIndices();

		if(listener != null)
			listener.onTreeStructureChanged(rootIndividualId);
	}

}
