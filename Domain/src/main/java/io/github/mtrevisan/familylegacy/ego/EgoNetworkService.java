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
package io.github.mtrevisan.familylegacy.ego;

import io.github.mtrevisan.familylegacy.consanguinity.AncestryGraph;
import io.github.mtrevisan.familylegacy.consanguinity.ConsanguineousUnionDetector;
import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityCalculator;
import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityReport;
import io.github.mtrevisan.familylegacy.individual.IndividualSummary;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.tree.GenealogyRepository;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Service responsible for extracting and building an {@link EgoNode} network
 * from the FLEF model, covering both biological and non-biological relationships.
 */
public class EgoNetworkService{

	private final FLEFModel model;
	private final GenealogyRepository repository;

	private final Map<String, List<FLEFRecord>> relationshipsByEntityId = new HashMap<>();
	private boolean relationshipsIndexed;
	/** The couples of each individual and the calculator of the ancestry, built on first use and dropped on changes. */
	private Map<String, List<ConsanguineousUnionDetector.Couple>> couplesByIndividual;
	private ConsanguinityCalculator calculator;


	public EgoNetworkService(final GenealogyRepository repository, final FLEFModel model){
		this.repository = repository;
		this.model = model;
	}


	public GenealogyRepository getRepository(){
		return repository;
	}

	/**
	 * Builds an {@link EgoNode} for the specified ego entity ID, collecting all
	 * directly connected parents, partners, children, groups, and associates.
	 *
	 * @param egoId the record ID of the central entity
	 * @return the populated EgoNode, or {@code null} if the record is not found
	 */
	public EgoNode buildEgoNetwork(final String egoId){
		if(StringUtils.isEmpty(egoId))
			return null;

		final FLEFRecord egoRecord = model.getRecordById(egoId);
		if(egoRecord == null)
			return null;

		repository.ensureIndices();
		ensureRelationshipsIndex();

		final IndividualSummary egoData = repository.getIndividualData(egoRecord);
		final EgoNode egoNode = new EgoNode(egoRecord, egoData);

		final List<FLEFRecord> rels = relationshipsByEntityId.get(egoId);
		if(rels != null){
			for(int i = 0, size = rels.size(); i < size; i ++){
				final FLEFRecord relationship = rels.get(i);

				final String type = RelationshipReader.extractType(relationship);
				final String role = RelationshipReader.extractRole(relationship);
				if(type == null)
					continue;

				final String subjectId = extractParticipantId(relationship, RelationshipReader.TAG_SUBJECT);
				final String objectId = extractParticipantId(relationship, RelationshipReader.TAG_TARGET);
				if(subjectId == null || objectId == null)
					continue;

				if(egoId.equals(subjectId))
					processEgoAsSubject(egoNode, type, role, objectId);

				if(egoId.equals(objectId))
					processEgoAsObject(egoNode, type, role, subjectId);
			}
		}

		markConsanguineousUnions(egoNode);

		return egoNode;
	}

	/** Marks the partners of the ego that are related to the ego (the badge on the edge is drawn from this). */
	private void markConsanguineousUnions(final EgoNode egoNode){
		final Set<EgoNode> partners = egoNode.getRelatedNodes(EgoNode.RelationshipCategory.PARTNER);
		if(partners.isEmpty())
			return;

		//only the couples of the ego are looked at, never the whole file: the report of the file can take minutes on a big one
		if(couplesByIndividual == null){
			final AncestryGraph ancestry = AncestryGraph.of(model);
			calculator = new ConsanguinityCalculator(ancestry);
			couplesByIndividual = ConsanguineousUnionDetector.couplesByIndividual(ConsanguineousUnionDetector.couples(model, ancestry));
		}

		final String egoId = egoNode.getEgoId();
		final Map<String, ConsanguinityReport.ConsanguineousUnion> unions = new HashMap<>();
		for(final ConsanguinityReport.ConsanguineousUnion union : ConsanguineousUnionDetector.unionsOf(calculator,
				couplesByIndividual.getOrDefault(egoId, List.of()), id -> ConsanguineousUnionDetector.nameOf(model, id),
				ConsanguineousUnionDetector.DEFAULT_MINIMUM_KINSHIP))
			unions.put(unionKey(union.firstId(), union.secondId()), union);
		for(final EgoNode partner : partners)
			partner.setConsanguineousUnion(unions.get(unionKey(egoId, partner.getEgoId())));
	}

	private static String unionKey(final String a, final String b){
		return (a.compareTo(b) <= 0? a + '\u0000' + b: b + '\u0000' + a);
	}

	private static String normalizeStatus(final String raw){
		if(raw == null)
			return RelationshipReader.ENUM_STATUS_UNKNOWN;

		return switch(raw.toLowerCase(Locale.ROOT)){
			case RelationshipReader.ENUM_STATUS_ACTIVE -> RelationshipReader.ENUM_STATUS_ACTIVE;
			case RelationshipReader.ENUM_STATUS_ENDED -> RelationshipReader.ENUM_STATUS_ENDED;
			default -> RelationshipReader.ENUM_STATUS_UNKNOWN;
		};
	}

	private void processEgoAsSubject(final EgoNode egoNode, final String type, final String role, final String targetId){
		final FLEFRecord targetRecord = model.getRecordById(targetId);
		if(targetRecord == null)
			return;

		if(RelationshipReader.isTypeChild(type))
			getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.PARENT, targetRecord, type, role, false);
		else if(RelationshipReader.isTypePartner(type))
			getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.PARTNER, targetRecord, type, role, false);
		else if(RelationshipReader.isTypeGroupMember(type) || RelationshipReader.isTypePartOf(type)){
			// group_member (Individual -> Group) and part_of (Group -> Group):
			// Ego is the member/sub-group, the target is the enclosing group
			if(isGroup(targetRecord))
				getOrAddRelatedGroup(egoNode, targetRecord, type, role, false);
			else
				getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.PARENT, targetRecord, type, role, false);
		}
		else if(RelationshipReader.isTypeAssociate(type)){
			if(isGroup(targetRecord))
				getOrAddRelatedGroup(egoNode, targetRecord, type, role, false);
			else
				getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.ASSOCIATE, targetRecord, type, role, false);
		}
	}

	private void processEgoAsObject(final EgoNode egoNode, final String type, final String role, final String subjectId){
		final FLEFRecord subjectRecord = model.getRecordById(subjectId);
		if(subjectRecord == null)
			return;

		if(RelationshipReader.isTypeChild(type))
			getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.CHILD, subjectRecord, type, role, true);
		else if(RelationshipReader.isTypePartner(type))
			getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.PARTNER, subjectRecord, type, role, true);
		else if(RelationshipReader.isTypeGroupMember(type) || RelationshipReader.isTypePartOf(type)){
			// group_member (Individual -> Group) and part_of (Group -> Group):
			// Ego is the group/super-group, the subject is the member/sub-group
			if(isGroup(subjectRecord))
				getOrAddRelatedGroup(egoNode, subjectRecord, type, role, true);
			else
				getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.CHILD, subjectRecord, type, role, true);
		}
		else if(RelationshipReader.isTypeAssociate(type)){
			if(isGroup(subjectRecord))
				getOrAddRelatedGroup(egoNode, subjectRecord, type, role, true);
			else
				getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.ASSOCIATE, subjectRecord, type, role, true);
		}
	}

	private static boolean isGroup(final FLEFRecord record){
		return GroupReader.isOfType(record);
	}

	private static void getOrAddRelatedGroup(final EgoNode egoNode, final FLEFRecord record, final String type,
			final String role, final boolean isInverse){
		egoNode.addGroupRecord(record, type, role, isInverse);
	}

	private void getOrAddRelatedIndividual(final EgoNode egoNode, final EgoNode.RelationshipCategory category,
			final FLEFRecord record, final String type, final String role, final boolean isInverse){
		EgoNode targetNode = null;
		for(final EgoNode existingNode : egoNode.getRelatedNodes(category))
			if(record.getId().equals(existingNode.getEgoId())){
				targetNode = existingNode;

				break;
			}

		if(targetNode == null){
			final IndividualSummary data = repository.getIndividualData(record);
			targetNode = new EgoNode(record, data);
			egoNode.addRelatedNode(category, targetNode);
		}

		targetNode.addRelationInfo(type, role, isInverse);
	}

	private String extractParticipantId(final FLEFRecord relRecord, final String fieldTag){
		String refId = relRecord.extractReferencedId(fieldTag, IndividualReader.TYPE);
		if(refId == null)
			refId = relRecord.extractReferencedId(fieldTag, GroupReader.TYPE);
		return refId;
	}

	/**
	 * Builds the reverse indices used by {@link #buildEgoNetwork(String)}.
	 * The method is idempotent and is invoked lazily the first time the
	 * network is built.
	 */
	private void ensureRelationshipsIndex(){
		if(relationshipsIndexed)
			return;

		// Index relationships by entity id.
		final List<FLEFRecord> relationships = PrivacyService.visible(model, model.getRecordsByType(RelationshipReader.TYPE));
		for(final FLEFRecord relationship : relationships){
			final String subjectId = extractParticipantId(relationship, RelationshipReader.TAG_SUBJECT);
			final String objectId = extractParticipantId(relationship, RelationshipReader.TAG_TARGET);
			if(subjectId != null)
				relationshipsByEntityId.computeIfAbsent(subjectId, k -> new ArrayList<>())
					.add(relationship);
			if(objectId != null)
				relationshipsByEntityId.computeIfAbsent(objectId, k -> new ArrayList<>())
					.add(relationship);
		}

		relationshipsIndexed = true;
	}

	/**
	 * Clears both reverse indices, forcing a rebuild on the next call to
	 * {@link #buildEgoNetwork(String)}.
	 */
	public void invalidateIndices(){
		repository.invalidateIndices();
		relationshipsByEntityId.clear();
		relationshipsIndexed = false;
		couplesByIndividual = null;
		calculator = null;
	}

}
