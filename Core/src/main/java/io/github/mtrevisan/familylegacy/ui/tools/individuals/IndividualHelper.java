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
package io.github.mtrevisan.familylegacy.ui.tools.individuals;

import io.github.mtrevisan.familylegacy.gedcom.utils.AuditBuilder;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ContextImpactReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Shared helpers for the tools in the {@code individuals} package.
 * <p>
 * The class centralizes three concerns that would otherwise be
 * duplicated across every tool:
 * <ul>
 *   <li>reading individual fields (name, sex, birth/death);</li>
 *   <li>creating, re-pointing, and deleting relationships;</li>
 *   <li>computing the set of relationships that involve an individual,
 *       so that a delete or a merge can clean up before mutating the
 *       model.</li>
 * </ul>
 */
public final class IndividualHelper{

	private IndividualHelper(){}


	public static List<FLEFRecord> listAllIndividuals(final FLEFModel model){
		return model.getRecordsByType(IndividualHandler.TYPE);
	}

	public static Map<String, FLEFRecord> indexIndividualsById(final FLEFModel model){
		final Map<String, FLEFRecord> result = new LinkedHashMap<>();
		for(final FLEFRecord individual : listAllIndividuals(model))
			if(individual.getId() != null)
				result.put(individual.getId(), individual);
		return result;
	}


	/* ======================================================================
	 *                          Relationships
	 * ====================================================================== */

	/**
	 * Creates a relationship record of the given type, with the given
	 * subject and target, both individuals. The relationship is added to
	 * the model and returned.
	 * <p>
	 * The convention is the one used throughout the model: the subject
	 * is the endpoint described by the type relative to the target.
	 * For a parent-child relationship, the subject is the child and the
	 * target is the parent.
	 */
	public static FLEFRecord createRelationship(final String subjectId, final String targetId, final String type,
			final String idPrefix, final FLEFModel model){
		final FLEFRecord relationship = FLEFRecord.createMainRecord(RelationshipHandler.TYPE, idPrefix, model)
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, type))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualHandler.TYPE, subjectId))
			)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualHandler.TYPE, targetId))
			)
			.addChild(AuditBuilder.build());
		model.addRecord(relationship);
		return relationship;
	}

	/**
	 * Returns the ids of every relationship in which the given
	 * individual appears as subject or target.
	 */
	public static Map<Class<? extends RecordTypeHandler<?>>, List<FLEFRecord>> relationshipIds(final String individualId, final FLEFModel model){
		final Map<Class<? extends RecordTypeHandler<?>>, List<FLEFRecord>> result = new HashMap<>();
		if(individualId == null)
			return result;

		final List<FLEFRecord> individualAttributes = model.getRecordsByType(IndividualAttributeHandler.TYPE);
		for(final FLEFRecord individualAttribute : individualAttributes){
			final String individualRecordId = individualAttribute.extractReferencedId(IndividualAttributeReader.TAG_INDIVIDUAL, IndividualHandler.TYPE);
			if(individualId.equals(individualRecordId))
				result.computeIfAbsent(IndividualAttributeHandler.class, k -> new ArrayList<>())
					.add(individualAttribute);
		}

		final List<FLEFRecord> eventParticipations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(final FLEFRecord eventParticipation : eventParticipations){
			final String participant = eventParticipation.extractReferencedId(EventParticipationReader.TAG_PARTICIPANT, IndividualHandler.TYPE);
			if(individualId.equals(participant))
				result.computeIfAbsent(EventParticipationHandler.class, k -> new ArrayList<>())
					.add(eventParticipation);
		}

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String subjectId = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualHandler.TYPE);
			final String objectId = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualHandler.TYPE);
			if(individualId.equals(subjectId) || individualId.equals(objectId))
				result.computeIfAbsent(RelationshipHandler.class, k -> new ArrayList<>())
					.add(relationship);
		}

		final List<FLEFRecord> repositories = model.getRecordsByType(RepositoryHandler.TYPE);
		for(final FLEFRecord repository : repositories){
			final List<FLEFRecord> custodian = FLEFRecordHelper.findChildren(repository, RepositoryReader.TAG_CUSTODIAN, IndividualHandler.TYPE);
			if(!custodian.isEmpty() && individualId.equals(custodian.getFirst().getId()))
				result.computeIfAbsent(RepositoryHandler.class, k -> new ArrayList<>())
					.add(repository);
		}

		final List<FLEFRecord> contextImpacts = model.getRecordsByType(ContextImpactHandler.TYPE);
		for(final FLEFRecord contextImpact : contextImpacts){
			final String target = contextImpact.extractReferencedId(ContextImpactReader.TAG_TARGET, IndividualHandler.TYPE);
			if(individualId.equals(target))
				result.computeIfAbsent(ContextImpactHandler.class, k -> new ArrayList<>())
					.add(contextImpact);
		}

		final List<FLEFRecord> researchQuestions = model.getRecordsByType(ResearchQuestionHandler.TYPE);
		for(final FLEFRecord researchQuestion : researchQuestions){
			final List<FLEFRecord> targets = FLEFRecordHelper.findChildren(researchQuestion, ResearchQuestionReader.TAG_TARGET, IndividualHandler.TYPE);
			for(final FLEFRecord target : targets)
				if(individualId.equals(target.getId())){
					result.computeIfAbsent(ResearchQuestionHandler.class, k -> new ArrayList<>())
						.add(researchQuestion);

					break;
				}
		}

		final List<FLEFRecord> conclusions = model.getRecordsByType(ConclusionHandler.TYPE);
		for(final FLEFRecord conclusion : conclusions){
			final List<FLEFRecord> resolves = FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESOLVES, IndividualHandler.TYPE);
			for(final FLEFRecord resolve : resolves)
				if(individualId.equals(resolve.getId())){
					result.computeIfAbsent(ConclusionHandler.class, k -> new ArrayList<>())
						.add(conclusion);

					break;
				}
		}

		final List<FLEFRecord> identityHypotheses = model.getRecordsByType(IdentityHypothesisHandler.TYPE);
		for(final FLEFRecord identityHypothesis : identityHypotheses){
			final List<FLEFRecord> identities = FLEFRecordHelper.findChildren(identityHypothesis, IdentityHypothesisReader.TAG_IDENTITY, IndividualHandler.TYPE);
			for(final FLEFRecord identity : identities)
				if(individualId.equals(identity.getId())){
					result.computeIfAbsent(IdentityHypothesisHandler.class, k -> new ArrayList<>())
						.add(identityHypothesis);

					break;
				}
		}

		return result;
	}

	/**
	 * Returns the ids of the parents of the given individual, using the
	 * biological-child relationship only. The result is ordered: father
	 * first when present, then mother.
	 */
	public static List<String> biologicalParentIds(final String individualId, final FLEFModel model){
		final List<String> fathers = new ArrayList<>();
		final List<String> mothers = new ArrayList<>();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = RelationshipReader.extractType(relationship);
			if(!RelationshipReader.isTypeBiologicalChild(type))
				continue;

			final String child = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualHandler.TYPE);
			final String parent = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualHandler.TYPE);
			if(!individualId.equals(child) || parent == null)
				continue;

			final FLEFRecord parentRecord = model.getRecordById(parent);
			final String rawSex = IndividualReader.extractRawSex(parentRecord);
			if(IndividualReader.isSexMale(rawSex))
				fathers.add(parent);
			else if(IndividualReader.isSexFemale(rawSex))
				mothers.add(parent);
			else
				fathers.add(parent);
		}
		final List<String> result = new ArrayList<>();
		result.addAll(fathers);
		result.addAll(mothers);
		return result;
	}

	/**
	 * Re-points an existing relationship to a new endpoint. Used by the
	 * merge operation to move relationships without recreating them.
	 *
	 * @param rel        the relationship to modify
	 * @param wrapperTag {@code subject} or {@code target}
	 * @param newId      the new individual id
	 */
	public static void setRelationshipEndpoint(final FLEFRecord rel, final String wrapperTag, final String newId){
		final FLEFRecord wrapper = FLEFRecordHelper.findChild(rel, wrapperTag);
		if(wrapper == null)
			return;
		final FLEFRecord entityBlock = wrapper.getTheOnlyChild();
		if(entityBlock == null)
			return;
		final FLEFRecord idRef = entityBlock.getTheOnlyChild();
		if(idRef != null)
			idRef.setValue(newId);
		else
			entityBlock.setValue(newId);
	}


	/* ======================================================================
	 *                          Cleanup before delete
	 * ====================================================================== */

	/**
	 * Deletes the given individual and all the relationships that
	 * involve it, in a single operation. The relationships are removed
	 * first, so the model is never left with a dangling reference.
	 *
	 * @param individualId the individual to delete
	 * @param model        the model
	 */
	public static void deleteIndividual(final String individualId, final FLEFModel model){
		if(individualId == null)
			return;

		final Map<Class<? extends RecordTypeHandler<?>>, List<FLEFRecord>> relationshipIds = relationshipIds(individualId, model);
		for(final Map.Entry<Class<? extends RecordTypeHandler<?>>, List<FLEFRecord>> entry : relationshipIds.entrySet())
			for(final FLEFRecord record : entry.getValue())
				model.removeRecord(record.getId());

		model.removeRecord(individualId);
	}


	/**
	 * Returns the set of IDs of all parents of the given individual (across all child relationship types).
	 */
	public static Set<String> parentIds(final String individualId, final FLEFModel model){
		final Set<String> result = new LinkedHashSet<>();
		if(individualId == null)
			return result;

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = RelationshipReader.extractType(relationship);
			if(!RelationshipReader.isTypeChild(type))
				continue;

			final String child = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualHandler.TYPE);
			final String parent = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualHandler.TYPE);
			if(individualId.equals(child) && parent != null)
				result.add(parent);
		}
		return result;
	}

	/**
	 * Returns the set of IDs of all associate/partner individuals linked to the given individual.
	 */
	public static Set<String> associateIds(final String individualId, final FLEFModel model){
		final Set<String> result = new LinkedHashSet<>();
		if(individualId == null)
			return result;

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = RelationshipReader.extractType(relationship);
			if(type == null || RelationshipReader.isTypeChild(type)
					|| RelationshipReader.ENUM_TYPE_GROUP_MEMBER.equalsIgnoreCase(type))
				continue;

			final String subject = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualHandler.TYPE);
			final String object = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualHandler.TYPE);
			if(individualId.equals(subject) && object != null)
				result.add(object);
			else if(individualId.equals(object) && subject != null)
				result.add(subject);
		}
		return result;
	}

	/**
	 * Returns the set of IDs of all groups in which the given individual is a member.
	 */
	public static Set<String> groupIds(final String individualId, final FLEFModel model){
		final Set<String> result = new LinkedHashSet<>();
		if(individualId == null)
			return result;

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = RelationshipReader.extractType(relationship);
			if(!RelationshipReader.isTypeGroupMember(type))
				continue;

			final String subject = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualHandler.TYPE);
			final String objectGroup = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, GroupHandler.TYPE);
			if(individualId.equals(subject) && objectGroup != null)
				result.add(objectGroup);
		}
		return result;
	}

	/**
	 * Returns the set of IDs of all children of the given individual (across all child relationship types).
	 */
	public static Set<String> childIds(final String individualId, final FLEFModel model){
		final Set<String> result = new LinkedHashSet<>();
		if(individualId == null)
			return result;

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = RelationshipReader.extractType(relationship);
			if(!RelationshipReader.isTypeChild(type))
				continue;

			final String child = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualHandler.TYPE);
			final String parent = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualHandler.TYPE);
			if(individualId.equals(parent) && child != null)
				result.add(child);
		}
		return result;
	}


	/* ======================================================================
	 *                          Row models
	 * ====================================================================== */

	/** Row used by the merge dialog to preview the two records. */
	public record MergePreview(String id, String name, String sex, int parentCount, int childCount, int spouseCount,
		int eventCount){}

	/**
	 * Builds a compact preview of an individual, used by the merge
	 * dialog to show what is about to be merged.
	 */
	public static MergePreview buildMergePreview(final String individualId, final FLEFModel model){
		final FLEFRecord individual = model.getRecordById(individualId);
		if(individual == null)
			return new MergePreview(individualId, individualId, null, 0, 0, 0,
				0);

		final Set<String> parents = new LinkedHashSet<>(biologicalParentIds(individualId, model));
		final Set<String> children = new LinkedHashSet<>();
		final Set<String> spouses = new LinkedHashSet<>();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = RelationshipReader.extractType(relationship);
			if(type == null)
				continue;

			final String subject = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualHandler.TYPE);
			final String object = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualHandler.TYPE);
			if(RelationshipReader.isTypeBiologicalChild(type)){
				if(individualId.equals(object) && subject != null)
					children.add(subject);
			}
			else if(RelationshipReader.isTypePartner(type)){
				if(individualId.equals(subject) && object != null)
					spouses.add(object);
				else if(individualId.equals(object) && subject != null)
					spouses.add(subject);
			}
		}

		int eventCount = 0;
		final List<FLEFRecord> participations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(final FLEFRecord participation : participations){
			final FLEFRecord participantBlock = FLEFRecordHelper.findChild(participation, "participant");
			if(participantBlock == null)
				continue;

			final FLEFRecord oneof = participantBlock.getTheOnlyChild();
			if(!IndividualHandler.isOfType(oneof))
				continue;

			final FLEFRecord ref = oneof.getTheOnlyChild();
			final String id = (ref != null? ref.getValue(): oneof.getValue());
			if(individualId.equals(id))
				eventCount ++;
		}

		final String rawSex = IndividualReader.extractRawSex(individual);
		final String displayName = IndividualReader.extractPrimaryFullname(individual);
		return new MergePreview(individualId, displayName, rawSex, parents.size(), children.size(),
			spouses.size(), eventCount);
	}

}
