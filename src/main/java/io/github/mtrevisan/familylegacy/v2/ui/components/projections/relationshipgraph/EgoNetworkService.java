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
 * <p>
 * To avoid repeated full scans of the model, the service builds two reverse
 * indices during {@link #ensureIndices()}:
 * <ul>
 *   <li>{@code relationshipsByEntityId} — for each entity id, the list of
 *       {@code relationship} records in which the entity appears as subject
 *       or target;</li>
 *   <li>{@code eventsByEntityId} — for each entity id, the list of
 *       {@code event} records in which the entity appears as a participant.</li>
 * </ul>
 * These indices let {@link #buildEgoNetwork(String)} answer in time
 * proportional to the number of relationships actually touching the ego,
 * instead of the total size of the model.
 */
class EgoNetworkService{

	private static final String TAG_TYPE = "type";
	private static final String TAG_ROLE = "role";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";
	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_EVENT = "event";
	private static final String TAG_INDIVIDUAL = "individual";
	private static final String TAG_GROUP = "group";

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

	private final Map<String, List<FLEFRecord>> relationshipsByEntityId = new HashMap<>();
	private final Map<String, List<FLEFRecord>> eventsByEntityId = new HashMap<>();
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

		final IndividualData egoData = IndividualData.create(egoRecord, eventsByEntityId, model);
		final EgoNode egoNode = new EgoNode(egoRecord, egoData);

		// Only iterate over the relationships that actually touch the ego
		for(final FLEFRecord relationship : relationshipsByEntityId.getOrDefault(egoId, List.of())){
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

	private void processEgoAsSubject(final EgoNode egoNode, final String type, final String role, final String targetId){
		final FLEFRecord targetRecord = model.getRecordById(targetId);
		if(targetRecord == null)
			return;

		if(isChildType(type))
			// Ego is child -> target is a parent
			getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.PARENT, targetRecord, type, role,
				false);
		else if(isPartnerType(type))
			// Ego is partner -> target is a partner
			getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.PARTNER, targetRecord, type, role,
				false);
		else if(ENUM_TYPE_GROUP_MEMBER.equals(type) || ENUM_TYPE_PART_OF.equals(type)){
			// group_member (Individual -> Group) and part_of (Group -> Group):
			// Ego is the member/sub-group, the target is the enclosing group
			if(isGroup(targetRecord))
				getOrAddRelatedGroup(egoNode, targetRecord, type, role, false);
			else
				getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.PARENT, targetRecord, type, role,
					false);
		}
		else if(ENUM_TYPE_ASSOCIATE.equals(type)){
			if(isGroup(targetRecord))
				getOrAddRelatedGroup(egoNode, targetRecord, type, role, false);
			else
				getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.ASSOCIATE, targetRecord, type, role,
					false);
		}
	}

	private void processEgoAsTarget(final EgoNode egoNode, final String type, final String role,
			final String subjectId){
		final FLEFRecord subjectRecord = model.getRecordById(subjectId);
		if(subjectRecord == null)
			return;

		if(isChildType(type))
			// Subject is child -> Ego is a parent
			getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.CHILD, subjectRecord, type, role,
				true);
		else if(isPartnerType(type))
			// Subject is partner -> Ego is a partner
			getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.PARTNER, subjectRecord, type, role,
				true);
		else if(ENUM_TYPE_GROUP_MEMBER.equals(type) || ENUM_TYPE_PART_OF.equals(type)){
			// group_member (Individual -> Group) and part_of (Group -> Group):
			// Ego is the group/super-group, the subject is the member/sub-group
			if(isGroup(subjectRecord))
				getOrAddRelatedGroup(egoNode, subjectRecord, type, role, true);
			else
				getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.CHILD, subjectRecord, type, role,
					true);
		}
		else if(ENUM_TYPE_ASSOCIATE.equals(type)){
			if(isGroup(subjectRecord))
				getOrAddRelatedGroup(egoNode, subjectRecord, type, role, true);
			else
				getOrAddRelatedIndividual(egoNode, EgoNode.RelationshipCategory.ASSOCIATE, subjectRecord, type, role,
					true);
		}
	}

	private static boolean isGroup(final FLEFRecord record){
		return (GroupHandler.TYPE.equalsIgnoreCase(record.getTag()));
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
			final IndividualData data = IndividualData.create(record, eventsByEntityId, model);
			targetNode = new EgoNode(record, data);
			egoNode.addRelatedNode(category, targetNode);
		}

		targetNode.addRelationInfo(type, role, isInverse);
	}

	private String extractParticipantId(final FLEFRecord relRecord, final String fieldTag){
		String refId = relRecord.extractReferencedId(fieldTag, TAG_INDIVIDUAL);
		if(refId == null)
			refId = relRecord.extractReferencedId(fieldTag, TAG_GROUP);
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

	/**
	 * Builds the reverse indices used by {@link #buildEgoNetwork(String)}.
	 * The method is idempotent and is invoked lazily the first time the
	 * network is built.
	 */
	private void ensureIndices(){
		if(indicesBuilt)
			return;

		// Index relationships by entity id.
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String subjectId = extractParticipantId(relationship, TAG_SUBJECT);
			final String targetId = extractParticipantId(relationship, TAG_TARGET);
			if(subjectId != null)
				relationshipsByEntityId.computeIfAbsent(subjectId, k -> new ArrayList<>())
					.add(relationship);
			if(targetId != null)
				relationshipsByEntityId.computeIfAbsent(targetId, k -> new ArrayList<>())
					.add(relationship);
		}

		// Index events by participant id. Only individuals and groups are
		// indexed, because places have their own dedicated event views.
		final List<FLEFRecord> eventParticipations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(final FLEFRecord ep : eventParticipations){
			final FLEFRecord participant = FLEFRecordHelper.findChild(ep, TAG_PARTICIPANT);
			if(participant == null)
				continue;

			final FLEFRecord indRef = participant.getTheOnlyChild();
			if(indRef == null || indRef.getValue() == null)
				continue;

			final String participantTag = indRef.getTag();
			if(participantTag == null)
				continue;

			if(!IndividualHandler.TYPE.equalsIgnoreCase(participantTag)
				&& !GroupHandler.TYPE.equalsIgnoreCase(participantTag))
				continue;

			final String participantId = indRef.getValue();
			final String eventId = FLEFRecordHelper.getChildValue(ep, TAG_EVENT);
			if(eventId == null)
				continue;

			final FLEFRecord event = model.getRecordById(eventId);
			if(event != null && EventHandler.TYPE.equalsIgnoreCase(event.getTag()))
				eventsByEntityId.computeIfAbsent(participantId, k -> new ArrayList<>())
					.add(event);
		}

		indicesBuilt = true;
	}

	/**
	 * Clears both reverse indices, forcing a rebuild on the next call to
	 * {@link #buildEgoNetwork(String)}.
	 */
	public void invalidateIndices(){
		relationshipsByEntityId.clear();
		eventsByEntityId.clear();
		indicesBuilt = false;
	}

}
