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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Service responsible for extracting and building an {@link EgoNode} network
 * from the FLEF model, covering both biological and non-biological relationships.
 */
class EgoNetworkService{

	private static final String TAG_TYPE = "type";
	private static final String TAG_ROLE = "role";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";
	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_EVENT = "event";

	private static final String ENUM_TYPE_BIOLOGICAL_CHILD = "biological_child";
	private static final String ENUM_TYPE_ADOPTIVE_CHILD = "adoptive_child";
	private static final String ENUM_TYPE_FOSTER_CHILD = "foster_child";
	private static final String ENUM_TYPE_GUARDED_CHILD = "guarded_child";
	private static final String ENUM_TYPE_STEP_CHILD = "step_child";
	private static final String ENUM_TYPE_CIVIL_SPOUSE = "civil_spouse";
	private static final String ENUM_TYPE_RELIGIOUS_SPOUSE = "religious_spouse";
	private static final String ENUM_TYPE_CUSTOMARY_SPOUSE = "customary_spouse";
	private static final String ENUM_TYPE_COHABITING_PARTNER = "cohabiting_partner";
	private static final String ENUM_TYPE_ENGAGED_PARTNER = "engaged_partner";
	private static final String ENUM_TYPE_GROUP_MEMBER = "group_member";
	private static final String ENUM_TYPE_ASSOCIATE = "associate";
	private static final String ENUM_TYPE_PART_OF = "part_of";


	private final FLEFModel model;

	private final Map<String, List<FLEFRecord>> individualToEventMap = new HashMap<>();
	private boolean indicesBuilt;


	public EgoNetworkService(final FLEFModel model){
		this.model = model;
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

		ensureIndices();

		final IndividualData egoData = IndividualData.create(egoRecord, individualToEventMap, model);
		final EgoNode egoNode = new EgoNode(egoRecord, egoData);

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
			final String role = FLEFRecordHelper.getChildValue(relationship, TAG_ROLE);
			if(type == null)
				continue;

			final String subjectId = extractParticipantId(relationship, TAG_SUBJECT);
			final String targetId = extractParticipantId(relationship, TAG_TARGET);
			if(subjectId == null || targetId == null)
				continue;

			// Process relationships where Ego is the Subject
			if(egoId.equals(subjectId))
				processEgoAsSubject(egoNode, type, role, targetId);

			// Process relationships where Ego is the Target
			if(egoId.equals(targetId))
				processEgoAsTarget(egoNode, type, role, subjectId);
		}

		return egoNode;
	}

	private void processEgoAsSubject(final EgoNode egoNode, final String type, final String role,
			final String targetId){
		final FLEFRecord targetRecord = model.getRecordById(targetId);
		if(targetRecord == null)
			return;

		if(isChildType(type))
			// Ego is child -> Target is a parent
			getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.PARENT, targetRecord, type, role, false);
		else if(isPartnerType(type))
			// Ego is partner -> Target is a partner
			getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.PARTNER, targetRecord, type, role, false);
		else if(ENUM_TYPE_GROUP_MEMBER.equals(type) || ENUM_TYPE_PART_OF.equals(type)){
			// Ego is member/sub-group -> Target is parent Group
			if(GroupHandler.TYPE.equalsIgnoreCase(targetRecord.getTag()))
				getOrAddRelatedGroup(egoNode, targetRecord, type, role, false);
			else
				getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.PARENT, targetRecord, type, role, false);
		}
		else if(ENUM_TYPE_ASSOCIATE.equals(type)){
			if(GroupHandler.TYPE.equalsIgnoreCase(targetRecord.getTag()))
				getOrAddRelatedGroup(egoNode, targetRecord, type, role, false);
			else
				getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.ASSOCIATE, targetRecord, type, role, false);
		}
	}

	private void processEgoAsTarget(final EgoNode egoNode, final String type, final String role,
			final String subjectId){
		final FLEFRecord subjectRecord = model.getRecordById(subjectId);
		if(subjectRecord == null)
			return;

		if(isChildType(type))
			// Subject is child -> Ego is a parent
			getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.CHILD, subjectRecord, type, role, true);
		else if(isPartnerType(type))
			// Subject is partner -> Ego is a partner
			getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.PARTNER, subjectRecord, type, role, true);
		else if(ENUM_TYPE_PART_OF.equals(type)){
			// Subject is sub-group/member -> Ego is super-group
			if(GroupHandler.TYPE.equalsIgnoreCase(subjectRecord.getTag()))
				getOrAddRelatedGroup(egoNode, subjectRecord, type, role, true);
			else
				getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.CHILD, subjectRecord, type, role, true);
		}
		else if(ENUM_TYPE_ASSOCIATE.equals(type)){
			if(!GroupHandler.TYPE.equalsIgnoreCase(subjectRecord.getTag()))
				getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.ASSOCIATE, subjectRecord, type, role, true);
		}
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
			final IndividualData data = IndividualData.create(record, individualToEventMap, model);
			targetNode = new EgoNode(record, data);
			egoNode.addRelatedNode(category, targetNode);
		}

		targetNode.addRelationInfo(type, role, isInverse);
	}

	private String extractParticipantId(final FLEFRecord relRecord, final String fieldTag){
		String refId = relRecord.extractReferencedId(fieldTag, IndividualHandler.TYPE);
		if(refId == null)
			refId = relRecord.extractReferencedId(fieldTag, GroupHandler.TYPE);
		return refId;
	}

	private boolean isChildType(final String type){
		return (ENUM_TYPE_BIOLOGICAL_CHILD.equals(type)
			|| ENUM_TYPE_ADOPTIVE_CHILD.equals(type)
			|| ENUM_TYPE_FOSTER_CHILD.equals(type)
			|| ENUM_TYPE_GUARDED_CHILD.equals(type)
			|| ENUM_TYPE_STEP_CHILD.equals(type));
	}

	private boolean isPartnerType(final String type){
		return (ENUM_TYPE_CIVIL_SPOUSE.equals(type)
			|| ENUM_TYPE_RELIGIOUS_SPOUSE.equals(type)
			|| ENUM_TYPE_CUSTOMARY_SPOUSE.equals(type)
			|| ENUM_TYPE_COHABITING_PARTNER.equals(type)
			|| ENUM_TYPE_ENGAGED_PARTNER.equals(type));
	}

	private void ensureIndices(){
		if(indicesBuilt)
			return;

		final List<FLEFRecord> eventParticipations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(final FLEFRecord ep : eventParticipations){
			final FLEFRecord participant = FLEFRecordHelper.findChild(ep, TAG_PARTICIPANT);
			if(participant == null)
				continue;
			final FLEFRecord indRef = participant.getTheOnlyChild();
			if(indRef == null || indRef.getValue() == null)
				continue;

			final String indId = indRef.getValue();
			final String eventId = FLEFRecordHelper.getChildValue(ep, TAG_EVENT);
			if(eventId == null)
				continue;

			final FLEFRecord event = model.getRecordById(eventId);
			if(event != null && EventHandler.TYPE.equalsIgnoreCase(event.getTag()))
				individualToEventMap.computeIfAbsent(indId, k -> new ArrayList<>()).add(event);
		}

		indicesBuilt = true;
	}

	public void invalidateIndices(){
		individualToEventMap.clear();
		indicesBuilt = false;
	}

}
