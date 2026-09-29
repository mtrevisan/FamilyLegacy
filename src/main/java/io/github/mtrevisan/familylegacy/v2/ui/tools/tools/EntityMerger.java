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
package io.github.mtrevisan.familylegacy.v2.ui.tools.tools;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.individuals.IndividualHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * Re-points and cleans the records around an individual merge, and
 * finally removes the source record.
 * <p>
 * The merge proceeds by re-pointing, not by copying:
 * <ol>
 *   <li>every relationship whose subject or target is the source is
 *       re-pointed to the target; relationships that would become
 *       self-loops are removed, and duplicated edges are collapsed;</li>
 *   <li>every event participation whose participant is the source is
 *       re-pointed to the target, with duplicate (event, role) pairs
 *       collapsed;</li>
 *   <li>every attribute record whose individual is the source is
 *       re-pointed to the target, with duplicate (type, value) pairs
 *       collapsed;</li>
 *   <li>the source record is deleted, together with any relationship
 *       that still refers to it (dangling edges from the previous
 *       steps).</li>
 * </ol>
 * Re-pointing is preferable to copying because it avoids creating
 * duplicate events, duplicate citations, and duplicate memberships that
 * would then have to be de-duplicated by hand. The source's own sources,
 * notes, and audit blocks are not carried over: the caller is expected
 * to warn the user before invoking the merge.
 */
public final class EntityMerger{

	private static final String TAG_SUBJECT = IndividualHelper.TAG_SUBJECT;
	private static final String TAG_OBJECT = IndividualHelper.TAG_OBJECT;
	private static final String TYPE_INDIVIDUAL = IndividualHelper.TYPE_INDIVIDUAL;
	private static final String TAG_TYPE = IndividualHelper.TAG_TYPE;

	private static final String TAG_EVENT = "event";
	private static final String TAG_ROLE = "role";
	private static final String TAG_PARTICIPANT = "participant";

	private static final String TAG_INDIVIDUAL = "individual";
	private static final String TAG_VALUE = "value";


	/**
	 * Summary of what a merge changed.
	 *
	 * @param relationshipsMoved  relationships re-pointed to the target
	 * @param participationsMoved event participations re-pointed to the target
	 * @param attributesMoved     attributes re-pointed to the target
	 */
	public record MergeResult(int relationshipsMoved, int participationsMoved, int attributesMoved){}


	private final FLEFModel model;


	public EntityMerger(final FLEFModel model){
		this.model = Objects.requireNonNull(model, "Model must not be null");
	}


	/**
	 * Merges the source individual into the target individual.
	 * <p>
	 * The source record is deleted at the end of the operation. The
	 * caller is responsible for warning the user about the data that
	 * will not be carried over (the source's own sources, notes, and
	 * audit blocks) and for invalidating any cache that depends on the
	 * model after this call returns.
	 *
	 * @param sourceId the id of the record to be deleted; must exist
	 * @param targetId the id of the record to keep; must exist and be
	 *                 different from {@code sourceId}
	 * @return a summary of the changes
	 * @throws IllegalArgumentException if the ids are null, equal, or
	 *                                  not present in the model
	 */
	public MergeResult merge(final String sourceId, final String targetId){
		if(StringUtils.isEmpty(sourceId) || StringUtils.isEmpty(targetId))
			throw new IllegalArgumentException("Source and target ids must not be empty");
		if(sourceId.equals(targetId))
			throw new IllegalArgumentException("Source and target must be different individuals");
		if(!model.hasRecord(sourceId))
			throw new IllegalArgumentException("Source individual does not exist: " + sourceId);
		if(!model.hasRecord(targetId))
			throw new IllegalArgumentException("Target individual does not exist: " + targetId);

		final int relationshipsMoved = repointRelationships(sourceId, targetId);
		final int participationsMoved = repointEventParticipations(sourceId, targetId);
		final int attributesMoved = repointAttributeRecords(sourceId, targetId);

		// Remove any relationship that still refers to the source, then
		// the source itself. This also cleans up the relationships that
		// could not be re-pointed because they had already been removed
		// as duplicates or self-loops.
		removeRemainingRelationships(sourceId);
		model.removeRecord(sourceId);

		return new MergeResult(relationshipsMoved, participationsMoved, attributesMoved);
	}


	/* ======================================================================
	 *                          Relationships
	 * ====================================================================== */

	/**
	 * Re-points every relationship that has the source as an endpoint to
	 * the target. Relationships that would become self-loops are removed,
	 * and duplicated edges are collapsed so the model does not end up
	 * with the same edge twice.
	 *
	 * @return the number of relationships that were re-pointed
	 */
	private int repointRelationships(final String sourceId, final String targetId){
		final Set<String> existingEdges = collectExistingEdges(sourceId);

		final List<String> toRemove = new ArrayList<>();
		int repointed = 0;
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String subjectId = relationship.extractReferencedId(RelationshipHandler.TAG_SUBJECT, IndividualHandler.TYPE);
			final String objectIdOfRel = relationship.extractReferencedId(RelationshipHandler.TAG_OBJECT, IndividualHandler.TYPE);
			final String type = FLEFRecordHelper.getChildValue(relationship, RelationshipHandler.TAG_TYPE);
			if(type == null)
				continue;

			final boolean sourceIsSubject = sourceId.equals(subjectId);
			final boolean sourceIsTarget = sourceId.equals(objectIdOfRel);
			if(!sourceIsSubject && !sourceIsTarget)
				continue;

			final String newSubject = (sourceIsSubject? targetId: subjectId);
			final String newTarget = (sourceIsTarget? targetId: objectIdOfRel);

			// Self-loop after re-point: the edge disappears.
			if(newSubject != null && newSubject.equals(newTarget)){
				toRemove.add(relationship.getId());

				continue;
			}

			// Duplicate edge after re-point: the existing one wins.
			final String edgeKey = edgeKey(newSubject, newTarget, type);
			if(existingEdges.contains(edgeKey)){
				toRemove.add(relationship.getId());

				continue;
			}

			// Re-point.
			if(sourceIsSubject)
				IndividualHelper.setRelationshipEndpoint(relationship, TAG_SUBJECT, newSubject);
			if(sourceIsTarget)
				IndividualHelper.setRelationshipEndpoint(relationship, TAG_OBJECT, newTarget);
			existingEdges.add(edgeKey);
			repointed ++;
		}

		for(final String id : toRemove)
			model.removeRecord(id);

		return repointed;
	}

	/**
	 * Collects the {@code subject|target|type} keys of every relationship
	 * that does not involve the source. Used to detect duplicates when
	 * re-pointing.
	 */
	private Set<String> collectExistingEdges(final String sourceId){
		final Set<String> edges = new LinkedHashSet<>();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String subjectId = relationship.extractReferencedId(RelationshipHandler.TAG_SUBJECT, IndividualHandler.TYPE);
			final String objectId = relationship.extractReferencedId(RelationshipHandler.TAG_OBJECT, IndividualHandler.TYPE);
			final String type = FLEFRecordHelper.getChildValue(relationship, RelationshipHandler.TAG_TYPE);
			if(subjectId != null && objectId != null && type != null
					&& !sourceId.equals(subjectId) && !sourceId.equals(objectId))
				edges.add(edgeKey(subjectId, objectId, type));
		}
		return edges;
	}

	private static String edgeKey(final String subject, final String target, final String type){
		return (subject != null? subject: "?")
			+ "|" + (target != null? target: "?")
			+ "|" + type;
	}

	/** Removes every remaining relationship that refers to the source. */
	private void removeRemainingRelationships(final String sourceId){
		final List<String> remaining = IndividualHelper.relationshipIdsForIndividual(sourceId, model);
		for(final String id : remaining)
			model.removeRecord(id);
	}


	/* ======================================================================
	 *                          Event participations
	 * ====================================================================== */

	/**
	 * Re-points every {@code event_participation} whose participant is
	 * the source to the target. Duplicates are collapsed: when the
	 * target already participates in the same event with the same role,
	 * the source's participation is deleted instead of re-pointed.
	 *
	 * @return the number of participations that were re-pointed
	 */
	private int repointEventParticipations(final String sourceId, final String targetId){
		final Set<String> existingParticipations = collectExistingParticipations(sourceId);

		final List<String> toRemove = new ArrayList<>();
		int repointed = 0;
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationHandler.TYPE)){
			final String participantId = participantId(participation);
			if(!sourceId.equals(participantId))
				continue;

			final String eventId = FLEFRecordHelper.getChildValue(participation, TAG_EVENT);
			if(eventId == null){
				toRemove.add(participation.getId());

				continue;
			}

			final String role = FLEFRecordHelper.getChildValue(participation, TAG_ROLE);
			final String key = participantKey(targetId, eventId, role);
			if(existingParticipations.contains(key)){
				toRemove.add(participation.getId());

				continue;
			}

			setParticipantId(participation, targetId);
			existingParticipations.add(key);
			repointed ++;
		}

		for(final String id : toRemove)
			model.removeRecord(id);

		return repointed;
	}

	private Set<String> collectExistingParticipations(final String sourceId){
		final Set<String> keys = new LinkedHashSet<>();
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationHandler.TYPE)){
			final String participantId = participantId(participation);
			final String eventId = FLEFRecordHelper.getChildValue(participation, TAG_EVENT);
			if(participantId != null && eventId != null && !sourceId.equals(participantId)){
				final String role = FLEFRecordHelper.getChildValue(participation, TAG_ROLE);
				keys.add(participantKey(participantId, eventId, role));
			}
		}
		return keys;
	}

	private static String participantKey(final String participant, final String eventId, final String role){
		return participant + "|" + eventId + "|" + (role != null? role: StringUtils.EMPTY);
	}

	/**
	 * Extracts the individual id of an event participation, following the
	 * {@code participant → individual → value} path. Returns {@code null}
	 * when the participation is malformed.
	 */
	private static String participantId(final FLEFRecord participation){
		final FLEFRecord participantBlock = FLEFRecordHelper.findChild(participation, TAG_PARTICIPANT);
		if(participantBlock == null)
			return null;

		final FLEFRecord oneOf = participantBlock.getTheOnlyChild();
		if(oneOf == null || !TYPE_INDIVIDUAL.equalsIgnoreCase(oneOf.getTag()))
			return null;

		final FLEFRecord reference = oneOf.getTheOnlyChild();
		return (reference != null? reference.getValue(): oneOf.getValue());
	}

	/**
	 * Rewrites the individual id of an event participation, following the
	 * same path as {@link #participantId(FLEFRecord)}.
	 */
	private static void setParticipantId(final FLEFRecord participation, final String newId){
		final FLEFRecord participantBlock = FLEFRecordHelper.findChild(participation, TAG_PARTICIPANT);
		if(participantBlock == null)
			return;

		final FLEFRecord oneOf = participantBlock.getTheOnlyChild();
		if(oneOf == null)
			return;

		final FLEFRecord reference = oneOf.getTheOnlyChild();
		if(reference != null)
			reference.setValue(newId);
		else
			oneOf.setValue(newId);
	}


	/* ======================================================================
	 *                          Attributes
	 * ====================================================================== */

	/**
	 * Re-points every {@code individual_attribute} whose individual is
	 * the source to the target. Duplicates are collapsed by
	 * {@code (type, value)}.
	 *
	 * @return the number of attributes that were re-pointed
	 */
	private int repointAttributeRecords(final String sourceId, final String targetId){
		final Set<String> existingAttributes = collectExistingAttributes(sourceId);

		final List<String> toRemove = new ArrayList<>();
		int repointed = 0;
		for(final FLEFRecord attribute : model.getRecordsByType(IndividualAttributeHandler.TYPE)){
			final String individualId = FLEFRecordHelper.getChildValue(attribute, TAG_INDIVIDUAL);
			if(!sourceId.equals(individualId))
				continue;

			final String type = FLEFRecordHelper.getChildValue(attribute, TAG_TYPE);
			final String value = FLEFRecordHelper.getChildValue(attribute, TAG_VALUE);
			final String key = attributeKey(targetId, type, value);
			if(existingAttributes.contains(key)){
				toRemove.add(attribute.getId());

				continue;
			}

			setAttributeIndividualId(attribute, targetId);
			existingAttributes.add(key);
			repointed ++;
		}

		for(final String id : toRemove)
			model.removeRecord(id);

		return repointed;
	}

	private Set<String> collectExistingAttributes(final String sourceId){
		final Set<String> keys = new LinkedHashSet<>();
		for(final FLEFRecord attribute : model.getRecordsByType(IndividualAttributeHandler.TYPE)){
			final String individualId = FLEFRecordHelper.getChildValue(attribute, TAG_INDIVIDUAL);
			if(individualId == null || sourceId.equals(individualId))
				continue;

			keys.add(attributeKey(individualId,
				FLEFRecordHelper.getChildValue(attribute, TAG_TYPE),
				FLEFRecordHelper.getChildValue(attribute, TAG_VALUE)));
		}
		return keys;
	}

	private static String attributeKey(final String individualId, final String type, final String value){
		return individualId
			+ "|" + (type != null? type: StringUtils.EMPTY)
			+ "|" + (value != null? value: StringUtils.EMPTY);
	}

	/**
	 * Rewrites the individual id of an individual attribute, following
	 * the {@code individual → value} path.
	 */
	private static void setAttributeIndividualId(final FLEFRecord attribute, final String newId){
		final FLEFRecord individual = FLEFRecordHelper.findChild(attribute, TAG_INDIVIDUAL);
		if(individual == null)
			return;

		final FLEFRecord reference = individual.getTheOnlyChild();
		if(reference != null)
			reference.setValue(newId);
		else
			individual.setValue(newId);
	}

}
