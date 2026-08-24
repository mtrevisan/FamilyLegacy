package io.github.mtrevisan.familylegacy.v2.gedcom.utils;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.time.LocalDate;
import java.util.Map;


/**
 * Crea i record di relazione (RelationshipRecord) e partecipazione (EventParticipationRecord)
 * una volta che tutti gli individui sono stati risolti.
 */
public class ReferenceResolver{

	private final FLEFModel model;
	private final Map<String, FLEFRecord> individualMap;
	private final Map<String, FLEFRecord> familyMap;

	public ReferenceResolver(FLEFModel model, Map<String, FLEFRecord> individualMap,
		Map<String, FLEFRecord> familyMap){
		this.model = model;
		this.individualMap = individualMap;
		this.familyMap = familyMap;
	}

	public void createParentChild(String childId, String parentId){
		String relId = IDGenerator.nextId("RL");
		FLEFRecord rel = FLEFRecord.createChildWithTag("relationship");
		rel.setId(relId);
		// subject: child
		FLEFRecord subject = FLEFRecord.createChildWithTag("subject");
		FLEFRecord subjInd = FLEFRecord.createChildWithTag("individual");
		subjInd.setValue(childId);
		subject.addChild(subjInd);
		rel.addChild(subject);
		// object: parent
		FLEFRecord object = FLEFRecord.createChildWithTag("object");
		FLEFRecord objInd = FLEFRecord.createChildWithTag("individual");
		objInd.setValue(parentId);
		object.addChild(objInd);
		rel.addChild(object);
		// type: biological_child (default) – could be overridden by adoption etc.
		rel.addChild(FLEFRecord.createChildWithTagAndValue("type", "biological_child"));
		// Status: active
		rel.addChild(FLEFRecord.createChildWithTagAndValue("status", "active"));

		rel.addChild(FLEFRecord.createChildWithTag("audit")
			.addChild(FLEFRecord.createChildWithTag("creation")
				.addChild(FLEFRecord.createChildWithTagAndValue("date", LocalDate.now().toString()))
			)
		);
		model.addRecord(rel);
	}

	public void createEventParticipation(String eventId, String entityId, String entityType, String role){
		String epId = IDGenerator.nextId("EP");
		FLEFRecord part = FLEFRecord.createChildWithTag("event_participation");
		part.setId(epId);
		// participant
		FLEFRecord participant = FLEFRecord.createChildWithTag("participant");
		FLEFRecord sub = FLEFRecord.createChildWithTag(entityType);
		sub.setValue(entityId);
		participant.addChild(sub);
		part.addChild(participant);
		// event
		FLEFRecord eventRef = FLEFRecord.createChildWithTag("event");
		eventRef.setValue(eventId);
		part.addChild(eventRef);
		// role (mappato)
		if(role != null){
			String mappedRole = GEDCOMMapper.mapRole(role);
			if(mappedRole != null){
				part.addChild(FLEFRecord.createChildWithTagAndValue("role", mappedRole));
			}
		}

		part.addChild(FLEFRecord.createChildWithTag("audit")
			.addChild(FLEFRecord.createChildWithTag("creation")
				.addChild(FLEFRecord.createChildWithTagAndValue("date", LocalDate.now().toString()))
			)
		);
		model.addRecord(part);
	}

}
