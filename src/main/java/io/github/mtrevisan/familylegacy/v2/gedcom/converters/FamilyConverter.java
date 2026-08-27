package io.github.mtrevisan.familylegacy.v2.gedcom.converters;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDGenerator;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDNormalizer;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.PlaceCache;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.ReferenceResolver;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.StructureParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Converts GEDCOM FAM (family) records into FLEF GroupRecord,
 * RelationshipRecord (spouse and parent-child), EventRecord, and EventParticipationRecord.
 * <p>
 * Handles:
 * <ul>
 *   <li>Group name → name</li>
 *   <li>Spouse relationship → RelationshipRecord</li>
 *   <li>Parent-child relationships → RelationshipRecord</li>
 *   <li>Family events (MARR, DIV, etc.) → EventRecord + EventParticipationRecord</li>
 *   <li>OBJE → DocumentRecord + preferred_image (if _PRIMARY Y) or SourceRecord (otherwise)</li>
 *   <li>Notes (NOTE) → inline note structs</li>
 *   <li>Extra fields (NCHI, RESN, etc.) → inline note structs or privacy</li>
 *   <li>Sources → SourceCitation</li>
 *   <li>Audit → AuditStructure</li>
 * </ul>
 */
public class FamilyConverter{

	private final FLEFModel model;
	private final Map<String, FLEFRecord> familyMap;
	private final Map<String, FLEFRecord> individualMap;
	private final Map<String, FLEFRecord> sourceMap;
	private final Map<String, FLEFRecord> multimediaMap;
	private final Map<String, GEDCOMNode> noteRawMap;
	private final StructureParser structParser;
	private final ReferenceResolver resolver;

	private final List<FamilyLink> familyLinks = new ArrayList<>();

	private static class FamilyLink{
		String familyId;
		String husbandId;
		String wifeId;
		List<String> childrenIds = new ArrayList<>();
		List<GEDCOMNode> events = new ArrayList<>();
		List<GEDCOMNode> objNodes = new ArrayList<>(); // OBJE children of the family
		List<GEDCOMNode> noteNodes = new ArrayList<>();
		List<GEDCOMNode> sourNodes = new ArrayList<>();
		GEDCOMNode famNode; // full node for later reference
	}

	/**
	 * Constructor.
	 *
	 * @param model         the FLEF model
	 * @param familyMap     map of family IDs to FLEF records
	 * @param individualMap map of individual IDs to FLEF records
	 * @param sourceMap     map of source IDs to FLEF records (for OBJE → SourceRecord)
	 * @param multimediaMap map of document IDs to FLEF records (for OBJE → DocumentRecord)
	 * @param placeCache    cache for place records
	 * @param resolver      reference resolver for creating relationships/events
	 */
	public FamilyConverter(FLEFModel model,
		Map<String, FLEFRecord> familyMap,
		Map<String, FLEFRecord> individualMap,
		Map<String, FLEFRecord> sourceMap,
		Map<String, FLEFRecord> multimediaMap,
		Map<String, GEDCOMNode> noteRawMap,
		PlaceCache placeCache,
		ReferenceResolver resolver){
		this.model = model;
		this.familyMap = familyMap;
		this.individualMap = individualMap;
		this.sourceMap = sourceMap;
		this.multimediaMap = multimediaMap;
		this.noteRawMap = noteRawMap;
		this.structParser = new StructureParser(placeCache);
		this.resolver = resolver;
	}

	/**
	 * First pass: collect family data and create the GroupRecord.
	 */
	public void collect(GEDCOMNode famNode){
		String famXref = famNode.getXrefId();
		if(famXref == null) return;

		String cleanFamId = IDNormalizer.clean(famXref);
		IDGenerator.registerExistingId(cleanFamId);

		// Create GroupRecord
		FLEFRecord group = FLEFRecord.createChildWithTag("group");
		group.setId(cleanFamId);
		group.addChild(FLEFRecord.createChildWithTagAndValue("type", "family"));
		familyMap.put(cleanFamId, group);

		// Create a link object for later resolution
		FamilyLink link = new FamilyLink();
		link.familyId = cleanFamId;
		link.famNode = famNode;

		// Husband and wife
		GEDCOMNode husbNode = GEDCOMHelper.findFirstChild(famNode, "HUSB");
		if(husbNode != null && husbNode.getValue() != null){
			link.husbandId = IDNormalizer.clean(husbNode.getValue());
		}
		GEDCOMNode wifeNode = GEDCOMHelper.findFirstChild(famNode, "WIFE");
		if(wifeNode != null && wifeNode.getValue() != null){
			link.wifeId = IDNormalizer.clean(wifeNode.getValue());
		}

		// Children
		for(GEDCOMNode chilNode : GEDCOMHelper.findChildren(famNode, "CHIL")){
			if(chilNode.getValue() != null){
				link.childrenIds.add(IDNormalizer.clean(chilNode.getValue()));
			}
		}

		// ---- Family events (aggiunto CENS e DIVF) ----
		Set<String> famEventTags = Set.of("MARR", "DIV", "ANUL", "ENGA", "MARB", "MARC", "MARL", "MARS", "RESI", "EVEN", "CENS", "DIVF");
		for (String tag : famEventTags) {
			link.events.addAll(GEDCOMHelper.findChildren(famNode, tag));
		}

		// Notes
		link.noteNodes.addAll(GEDCOMHelper.findChildren(famNode, "NOTE"));

		// Sources
		link.sourNodes.addAll(GEDCOMHelper.findChildren(famNode, "SOUR"));

		// Multimedia (OBJE)
		link.objNodes.addAll(GEDCOMHelper.findChildren(famNode, "OBJE"));

		// NCHI (number of children) – store as inline note only if different from actual count
		GEDCOMNode nchiNode = GEDCOMHelper.findFirstChild(famNode, "NCHI");
		if(nchiNode != null && nchiNode.getValue() != null){
			int reportedCount;
			try{
				reportedCount = Integer.parseInt(nchiNode.getValue().trim());
			}
			catch(NumberFormatException e){
				reportedCount = -1;
			}
			if(reportedCount >= 0){
				int actualCount = link.childrenIds.size();
				if(reportedCount != actualCount){
					String text = "Number of children: " + nchiNode.getValue();
					FLEFRecord note = structParser.createNoteStruct(text, famNode);
					if(note != null) group.addChild(note);
				}
			}
		}

		// Restriction notice (RESN) -> privacy
		GEDCOMNode resnNode = GEDCOMHelper.findFirstChild(famNode, "RESN");
		if(resnNode != null && resnNode.getValue() != null){
			String level = GEDCOMMapper.mapPrivacyLevel(resnNode.getValue());
			FLEFRecord privacy = FLEFRecord.createChildWithTag("privacy");
			privacy.addChild(FLEFRecord.createChildWithTagAndValue("level", level));
			group.addChild(privacy);
		}

		// Audit
		group.addChild(structParser.createAudit(famNode));

		// Add all sources and notes directly to the group (they are not link-specific)
		for(GEDCOMNode sourNode : link.sourNodes){
			FLEFRecord sourCitation = structParser.parseSourceCitation(sourNode, model, noteRawMap/*, famXref, "group"*/);
			if(sourCitation != null) group.addChild(sourCitation);
		}
		for(GEDCOMNode noteNode : link.noteNodes){
			FLEFRecord noteStruct = structParser.parseNoteStruct(noteNode);
			if(noteStruct != null) group.addChild(noteStruct);
		}

		// Store the link for the second pass
		familyLinks.add(link);
	}

	/**
	 * Second pass: resolve all links, create relationships, events, and OBJE handling.
	 */
	public void resolveLinks(){
		for(FamilyLink link : familyLinks){
			FLEFRecord group = familyMap.get(link.familyId);
			if(group == null) continue;

			// ---- Set group name ----
			String husbandName = getDisplayName(link.husbandId);
			String wifeName = getDisplayName(link.wifeId);
			String groupName = "Family of " + husbandName + (wifeName.isEmpty()? "": " and " + wifeName);
			if(!groupName.equals("Family of ")){
				FLEFRecord nameRec = FLEFRecord.createChildWithTag("name");
				FLEFRecord textRec = FLEFRecord.createChildWithTag("text");
				textRec.addChild(FLEFRecord.createChildWithTagAndValue("value", groupName));
				nameRec.addChild(textRec);
				group.addChild(nameRec);
			}

			// ---- OBJE handling (preferred_image + SourceRecord for others) ----
			processObjNodes(link, group);

			// ---- Spouse relationship ----
			if(link.husbandId != null && link.wifeId != null){
				FLEFRecord spouseRel = FLEFRecord.createChildWithTag("relationship");
				spouseRel.setId(IDGenerator.nextId("RL"));
				// subject: husband
				FLEFRecord subject = FLEFRecord.createChildWithTag("subject");
				FLEFRecord subjInd = FLEFRecord.createChildWithTag("individual");
				subjInd.setValue(link.husbandId);
				subject.addChild(subjInd);
				spouseRel.addChild(subject);
				// object: wife
				FLEFRecord object = FLEFRecord.createChildWithTag("object");
				FLEFRecord objInd = FLEFRecord.createChildWithTag("individual");
				objInd.setValue(link.wifeId);
				object.addChild(objInd);
				spouseRel.addChild(object);
				spouseRel.addChild(FLEFRecord.createChildWithTagAndValue("type", "civil_spouse"));
				// Status: if there is a DIV event, set ended
				boolean hasDivorce = link.events.stream().anyMatch(e -> "DIV".equals(e.getTag()));
				spouseRel.addChild(FLEFRecord.createChildWithTagAndValue("status", hasDivorce? "ended": "active"));
				// Add date from MARR event
				for(GEDCOMNode evt : link.events){
					if("MARR".equals(evt.getTag())){
						FLEFRecord dateStruct = structParser.parseDateStructure(evt);
						if(dateStruct != null) spouseRel.addChild(dateStruct);
						break;
					}
				}
				// Audit for relationship
				spouseRel.addChild(structParser.createAudit(link.famNode));
				model.addRecord(spouseRel);
			}

			// ---- Parent-child relationships ----
			for(String childId : link.childrenIds){
				if(individualMap.containsKey(childId)){
					if(link.husbandId != null){
						resolver.createParentChild(childId, link.husbandId);
					}
					if(link.wifeId != null){
						resolver.createParentChild(childId, link.wifeId);
					}
				}
			}

			// ---- Family events as EventRecords ----
			for(GEDCOMNode evtNode : link.events){
				FLEFRecord evtRec = structParser.parseEvent(evtNode, evtNode.getTag(), noteRawMap);
				if(evtRec != null){
					String evtId = IDGenerator.nextId("E");
					evtRec.setId(evtId);
					model.addRecord(evtRec);
					// Link participants
					if(link.husbandId != null){
						resolver.createEventParticipation(evtId, link.husbandId, "individual", "spouse");
					}
					if(link.wifeId != null){
						resolver.createEventParticipation(evtId, link.wifeId, "individual", "spouse");
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------------
	// OBJE handling
	// ------------------------------------------------------------------------

	/**
	 * Processes all OBJE nodes attached to the family:
	 * - Creates a DocumentRecord for each.
	 * - If _PRIMARY Y, creates preferred_image on the group.
	 * - Otherwise, creates a SourceRecord and a SourceCitation for the group.
	 */
	private void processObjNodes(FamilyLink link, FLEFRecord group){
		List<GEDCOMNode> objNodes = link.objNodes;
		if(objNodes.isEmpty()) return;

		// Find the primary OBJE (with _PRIMARY Y)
		GEDCOMNode preferredObj = null;
		for(GEDCOMNode obj : objNodes){
			GEDCOMNode primaryNode = GEDCOMHelper.findFirstChild(obj, "_PRIMARY");
			if(primaryNode != null && "Y".equalsIgnoreCase(primaryNode.getValue())){
				preferredObj = obj;
				break;
			}
		}

		for(GEDCOMNode objNode : objNodes){
			// 1. Create or retrieve DocumentRecord
			FLEFRecord docRecord = null;
			String objXref = objNode.getXrefId();
			if(objXref != null){
				String cleanId = IDNormalizer.clean(objXref);
				docRecord = multimediaMap.get(cleanId);
			}
			if(docRecord == null){
				docRecord = createDocumentRecord(objNode);
				model.addRecord(docRecord);
				multimediaMap.put(docRecord.getId(), docRecord);
			}

			// 2. If primary: create preferred_image
			if(objNode == preferredObj){
				String fileUri = FLEFRecordHelper.getChildValue(docRecord, "file");
				if(fileUri != null && !fileUri.isEmpty()){
					FLEFRecord prefImg = FLEFRecord.createChildWithTag("preferred_image");
					prefImg.addChild(FLEFRecord.createChildWithTagAndValue("uri", fileUri));

					// Crop from _CUTD
					GEDCOMNode cutdNode = GEDCOMHelper.findFirstChild(objNode, "_CUTD");
					if(cutdNode != null && cutdNode.getValue() != null){
						String[] parts = cutdNode.getValue().split(" ");
						if(parts.length == 4){
							try{
								int x = Integer.parseInt(parts[0]);
								int y = Integer.parseInt(parts[1]);
								int w = Integer.parseInt(parts[2]);
								int h = Integer.parseInt(parts[3]);
								FLEFRecord crop = FLEFRecord.createChildWithTag("crop");
								crop.addChild(FLEFRecord.createChildWithTagAndValue("x", String.valueOf(x)));
								crop.addChild(FLEFRecord.createChildWithTagAndValue("y", String.valueOf(y)));
								crop.addChild(FLEFRecord.createChildWithTagAndValue("width", String.valueOf(w)));
								crop.addChild(FLEFRecord.createChildWithTagAndValue("height", String.valueOf(h)));
								prefImg.addChild(crop);
							}
							catch(NumberFormatException ignored){
							}
						}
					}
					group.addChild(prefImg);
				}
			}
			else{
				// 3. Non-primary: create a SourceRecord and link it to the group
				FLEFRecord sourceRecord = createSourceRecordFromDocument(docRecord, objNode);
				model.addRecord(sourceRecord);
				sourceMap.put(sourceRecord.getId(), sourceRecord);

				// Create SourceCitation for the group
				FLEFRecord sourceCitation = FLEFRecord.createChildWithTag("source");
				FLEFRecord sourceRef = FLEFRecord.createChildWithTag("source");
				sourceRef.setValue(sourceRecord.getId());
				sourceCitation.addChild(sourceRef);
				group.addChild(sourceCitation);
			}
		}
	}

	/**
	 * Creates a DocumentRecord from a GEDCOM OBJE node.
	 * Excludes tags that are already used for specific purposes.
	 */
	private FLEFRecord createDocumentRecord(GEDCOMNode objNode){
		String id = IDGenerator.nextId("D");
		FLEFRecord doc = FLEFRecord.createChildWithTag("document");
		doc.setId(id);

		// FILE -> file
		GEDCOMNode fileNode = GEDCOMHelper.findFirstChild(objNode, "FILE");
		if(fileNode != null && fileNode.getValue() != null){
			doc.addChild(FLEFRecord.createChildWithTagAndValue("file", fileNode.getValue()));
		}

		// TITL -> description
		GEDCOMNode titlNode = GEDCOMHelper.findFirstChild(objNode, "TITL");
		if(titlNode != null && titlNode.getValue() != null){
			doc.addChild(FLEFRecord.createChildWithTagAndValue("description", titlNode.getValue()));
		}

//		// FORM -> note (inline)
//		GEDCOMNode formNode = GEDCOMHelper.findFirstChild(objNode, "FORM");
//		if(formNode != null && formNode.getValue() != null){
//			FLEFRecord note = FLEFRecord.createChildWithTag("note");
//			note.addChild(FLEFRecord.createChildWithTagAndValue("value", "Format: " + formNode.getValue()));
//			doc.addChild(note);
//		}

//		// Exclude tags that are already used for specific purposes
//		Set<String> excludedTags = Set.of("_PRIMARY", "_CUTD", "_PUBL", "_CUT", "_PREF", "_DATE");
//		for(GEDCOMNode child : objNode.getChildren()){
//			String tag = child.getTag();
//			if(tag.startsWith("_") && child.getValue() != null && !excludedTags.contains(tag)){
//				FLEFRecord note = FLEFRecord.createChildWithTag("note");
//				note.addChild(FLEFRecord.createChildWithTagAndValue("value", tag + ": " + child.getValue()));
//				doc.addChild(note);
//			}
//		}

		// Audit
		doc.addChild(structParser.createAudit(objNode));
		return doc;
	}

	/**
	 * Creates a SourceRecord that references a DocumentRecord.
	 */
	private FLEFRecord createSourceRecordFromDocument(FLEFRecord docRecord, GEDCOMNode objNode){
		String id = IDGenerator.nextId("S");
		FLEFRecord source = FLEFRecord.createChildWithTag("source");
		source.setId(id);

		// Title: use document description or default
		String docDesc = FLEFRecordHelper.getChildValue(docRecord, "description");
		if(docDesc == null || docDesc.isEmpty()){
			docDesc = "Image";
		}
		FLEFRecord titleRec = FLEFRecord.createChildWithTag("title");
		titleRec.addChild(FLEFRecord.createChildWithTagAndValue("value", docDesc));
		source.addChild(titleRec);

		// Reference to the DocumentRecord
		FLEFRecord docRef = FLEFRecord.createChildWithTag("document");
		docRef.setValue(docRecord.getId());
		source.addChild(docRef);

		// Date from _DATE (if present)
		GEDCOMNode dateNode = GEDCOMHelper.findFirstChild(objNode, "_DATE");
		if(dateNode != null && dateNode.getValue() != null){
			GEDCOMNode syntheticDate = new GEDCOMNode(dateNode.getLevel(), "DATE", dateNode.getValue());
			FLEFRecord dateStruct = structParser.parseDateStructure(syntheticDate);
			if(dateStruct != null) source.addChild(dateStruct);
		}

		// Audit
		source.addChild(structParser.createAudit(objNode));
		return source;
	}

	// ------------------------------------------------------------------------
	// Utility
	// ------------------------------------------------------------------------

	/**
	 * Helper to extract a display name from an individual ID.
	 */
	private String getDisplayName(String indiId){
		if(indiId == null) return "";
		FLEFRecord indi = individualMap.get(indiId);
		if(indi == null) return "";
		// Extract first given name from "name" structure
		for(FLEFRecord name : indi.getChildren()){
			if("name".equals(name.getTag())){
				for(FLEFRecord part : name.getChildren()){
					if("part".equals(part.getTag())){
						String type = FLEFRecordHelper.getChildValue(part, "type");
						if("given".equals(type)){
							String val = FLEFRecordHelper.getChildValue(part, "value");
							if(val != null) return val;
						}
					}
				}
			}
		}
		return "Unknown";
	}

}
