package io.github.mtrevisan.familylegacy.v2.gedcom.converters;

import io.github.mtrevisan.familylegacy.v2.gedcom.Deduplicator;
import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.AuditBuilder;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDGenerator;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDNormalizer;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.PlaceCache;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.StructureParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Converts GEDCOM FAM (family) records into FLEF GroupRecord,
 * RelationshipRecord (spouse and parent-child), EventRecord, EventParticipationRecord,
 * and GroupAttributeRecord (for RESI, NCHI, etc.).
 * <p>
 * Handles:
 * <ul>
 *   <li>Group name → name</li>
 *   <li>Spouse relationship → RelationshipRecord</li>
 *   <li>Parent-child relationships → RelationshipRecord</li>
 *   <li>Family events (MARR, DIV, ENGA, CENS, etc.) → EventRecord + EventParticipationRecord</li>
 *   <li>Family attributes (RESI) → GroupAttributeRecord</li>
 *   <li>OBJE → DocumentRecord + preferred_image (if _PRIMARY Y) or SourceRecord (otherwise)</li>
 *   <li>Notes (NOTE) → inline note structs</li>
 *   <li>Extra fields (NCHI, RESN, etc.) → inline note structs or privacy</li>
 *   <li>Sources → SourceCitation</li>
 *   <li>Audit → AuditStructure</li>
 * </ul>
 *
 * <p><b>Improvements over previous version:</b>
 * <ul>
 *   <li>Separate actual events (MARR, DIV, etc.) from attributes (RESI) – RESI now becomes GroupAttributeRecord.</li>
 *   <li>Use GEDCOMMapper to map GEDCOM event/attribute tags to FLEF standard types.</li>
 *   <li>Always attach the family Group as a participant for all events (role "family") to ensure context.</li>
 *   <li>Handle EVEN with TYPE sub-tag for custom event classification.</li>
 *   <li>Correctly set event type using mapped values.</li>
 * </ul>
 */
public class FamilyConverter{

	private final FLEFModel model;
	private final Map<String, FLEFRecord> familyMap;
	private final Map<String, FLEFRecord> individualMap;
	private final Map<String, FLEFRecord> sourceMap;
	private final Map<String, FLEFRecord> multimediaMap;
	private final Map<String, GEDCOMNode> noteRawMap;
	private final Map<String, GEDCOMNode> sourRawMap;
	private final Map<String, GEDCOMNode> objeRawMap;
	private final StructureParser structParser;

	private final List<FamilyLink> familyLinks = new ArrayList<>();

	private static class FamilyLink{
		String familyId;
		String husbandId;
		String wifeId;
		List<String> childrenIds = new ArrayList<>();
		// Actual events (MARR, DIV, ENGA, CENS, DIVF, EVEN, etc.)
		List<GEDCOMNode> eventNodes = new ArrayList<>();
		// Attributes (RESI)
		List<GEDCOMNode> attributeNodes = new ArrayList<>();
		List<GEDCOMNode> objNodes = new ArrayList<>();
		List<GEDCOMNode> noteNodes = new ArrayList<>();
		List<GEDCOMNode> sourNodes = new ArrayList<>();
		GEDCOMNode famNode;
	}

	public FamilyConverter(FLEFModel model,
		Map<String, FLEFRecord> familyMap,
		Map<String, FLEFRecord> individualMap,
		Map<String, FLEFRecord> sourceMap,
		Map<String, FLEFRecord> multimediaMap,
		Map<String, GEDCOMNode> noteRawMap,
		Map<String, GEDCOMNode> sourRawMap,
		Map<String, GEDCOMNode> objeRawMap,
		PlaceCache placeCache){
		this.model = model;
		this.familyMap = familyMap;
		this.individualMap = individualMap;
		this.sourceMap = sourceMap;
		this.multimediaMap = multimediaMap;
		this.noteRawMap = noteRawMap;
		this.sourRawMap = sourRawMap;
		this.objeRawMap = objeRawMap;
		this.structParser = new StructureParser(placeCache);
	}

	/**
	 * First pass: collect family data, create the GroupRecord,
	 * and separate event nodes from attribute nodes.
	 */
	public void collect(GEDCOMNode famNode){
		String famXref = famNode.getXrefId();
		if(famXref == null) return;

		String cleanFamId = IDNormalizer.clean(famXref);
		IDGenerator.registerExistingId(cleanFamId);

		// Create GroupRecord
		FLEFRecord group = FLEFRecord.createMainRecord(cleanFamId, GroupHandler.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue("type", "family"));
		familyMap.put(cleanFamId, group);

		FamilyLink link = new FamilyLink();
		link.familyId = cleanFamId;
		link.famNode = famNode;

		// Husband and wife
		GEDCOMNode husbNode = GEDCOMHelper.findFirstChild(famNode, "HUSB");
		if(husbNode != null && husbNode.getValue() != null){
			link.husbandId = GEDCOMHelper.cleanId(husbNode.getValue());
		}
		GEDCOMNode wifeNode = GEDCOMHelper.findFirstChild(famNode, "WIFE");
		if(wifeNode != null && wifeNode.getValue() != null){
			link.wifeId = GEDCOMHelper.cleanId(wifeNode.getValue());
		}

		// Children
		for(GEDCOMNode chilNode : GEDCOMHelper.findChildren(famNode, "CHIL")){
			if(chilNode.getValue() != null){
				link.childrenIds.add(GEDCOMHelper.cleanId(chilNode.getValue()));
			}
		}

		// Family events: collect event tags (excluding RESI which is an attribute)
		Set<String> eventTags = new HashSet<>(Set.of(
			"MARR", "DIV", "ANUL", "ENGA", "MARB", "MARC",
			"MARL", "MARS", "CENS", "DIVF", "EVEN"
		));
		// Family attributes: RESI (residence)
		Set<String> attributeTags = Set.of("RESI");

		for(String tag : eventTags){
			link.eventNodes.addAll(GEDCOMHelper.findChildren(famNode, tag));
		}
		for(String tag : attributeTags){
			link.attributeNodes.addAll(GEDCOMHelper.findChildren(famNode, tag));
		}

		// Notes
		link.noteNodes.addAll(GEDCOMHelper.findChildren(famNode, "NOTE"));

		// Sources
		link.sourNodes.addAll(GEDCOMHelper.findChildren(famNode, "SOUR"));

		// Multimedia (OBJE)
		link.objNodes.addAll(GEDCOMHelper.findChildren(famNode, "OBJE"));

		// NCHI (number of children) – store as GroupAttributeRecord if valid
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
					FLEFRecord groupAttribute = FLEFRecord.createMainRecord(
							IDGenerator.nextId(GroupAttributeHandler.ID_PREFIX), GroupAttributeHandler.TYPE)
						.addChild(AuditBuilder.build(famNode))
						.addChild(FLEFRecord.createChildWithTagAndValue("group", group.getId()))
						.addChild(FLEFRecord.createChildWithTagAndValue("type", "children_count"))
						.addChild(FLEFRecord.createChildWithTagAndValue("value", String.valueOf(reportedCount)));
					Deduplicator.getDeduplicatedRecordId(model, groupAttribute);
				}
			}
		}

		// Sources (SOUR) – attach directly to group
		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(famNode, "SOUR")){
			GEDCOMHelper.attachSource(group, model, sourNode, noteRawMap, sourRawMap, objeRawMap);
		}

		// Notes – inline structs
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(famNode, "NOTE")){
			GEDCOMHelper.attachNote(group, noteNode, noteRawMap);
		}

		// Multimedia (OBJE) – attach directly to group
		for(GEDCOMNode multimediaLinkNode : GEDCOMHelper.findChildren(famNode, "OBJE")){
			GEDCOMHelper.attachMultimediaLink(group, model, multimediaLinkNode, objeRawMap);
		}

		// Restriction notice -> privacy
		GEDCOMHelper.attachRestriction(group, famNode);

		// Audit
		group.addChild(AuditBuilder.build(famNode));

		// Store the link for the second pass
		familyLinks.add(link);
	}

	/**
	 * Second pass: resolve all links, create relationships, events, attributes, and OBJE handling.
	 */
	public void resolveLinks(List<GEDCOMNode> roots){
		for(FamilyLink link : familyLinks){
			FLEFRecord group = familyMap.get(link.familyId);
			if(group == null) continue;

			// ---- Set group name ----
			String husbandName = getDisplayName(link.husbandId);
			String wifeName = getDisplayName(link.wifeId);
			String groupName = "Family of " + husbandName + (wifeName.isEmpty()? "": " and " + wifeName);
			if(!groupName.equals("Family of ")){
				FLEFRecord nameRec = FLEFRecord.createChildWithTag("name")
					.addChild(FLEFRecord.createChildWithTagAndValue("value", groupName));
				group.addChild(nameRec);
			}

			// ---- Spouse relationship (Inter-individual) ----
			if(link.husbandId != null && link.wifeId != null){
				FLEFRecord relationship = FLEFRecord.createMainRecord(
					IDGenerator.nextId(RelationshipHandler.ID_PREFIX), RelationshipHandler.TYPE);
				relationship.addChild(FLEFRecord.createChildWithTag("subject")
					.addChild(FLEFRecord.createChildWithTagAndValue("individual", link.husbandId)));
				relationship.addChild(FLEFRecord.createChildWithTag("target")
					.addChild(FLEFRecord.createChildWithTagAndValue("individual", link.wifeId)));

				// Determine spouse type from MARR.TYPE if present
				String marriageType = null;
				for(GEDCOMNode evt : link.eventNodes){
					if("MARR".equals(evt.getTag())){
						GEDCOMNode typeNode = GEDCOMHelper.findFirstChild(evt, "TYPE");
						if(typeNode != null && typeNode.getValue() != null){
							marriageType = typeNode.getValue().trim();
						}
						break;
					}
				}
				String typeValue = (marriageType == null)? "civil_spouse": marriageType + "_spouse";
				relationship.addChild(FLEFRecord.createChildWithTagAndValue("type", typeValue));

				// Status: if there is a DIV event, set ended
				boolean hasDivorce = link.eventNodes.stream().anyMatch(e -> "DIV".equals(e.getTag()));
				relationship.addChild(FLEFRecord.createChildWithTagAndValue("status", hasDivorce? "ended": "active"));

				// Add date from MARR event (valid_from)
				for(GEDCOMNode evt : link.eventNodes){
					if("MARR".equals(evt.getTag())){
						FLEFRecord dateStruct = structParser.parseDateStructure(evt);
						if(dateStruct != null){
							dateStruct.setTag("valid_from");
							relationship.addChild(dateStruct);
						}
						break;
					}
				}
				relationship.addChild(AuditBuilder.build(link.famNode));
				Deduplicator.getDeduplicatedRecordId(model, relationship);
			}

			// ---- Parent-child relationships ----
			for(String childId : link.childrenIds){
				if(individualMap.containsKey(childId)){
					if(link.husbandId != null){
						createParentChild(childId, link.husbandId);
					}
					if(link.wifeId != null){
						createParentChild(childId, link.wifeId);
					}
				}
			}

			// ---- Process actual family events (MARR, DIV, ENGA, etc.) ----
			for(GEDCOMNode evtNode : link.eventNodes){
				String eventFlefId = createAndAddEventRecord(
					evtNode, noteRawMap, sourRawMap, objeRawMap, model
				);
				if(eventFlefId != null){
					// Attach Group as participant (role "family")
					GEDCOMHelper.attachEventParticipation(
						roots, eventFlefId, "group", group.getId(), "family", model
					);
					// Attach Husband and Wife if present
					if(link.husbandId != null){
						GEDCOMHelper.attachEventParticipation(
							roots, eventFlefId, "individual", link.husbandId, "husband", model
						);
					}
					if(link.wifeId != null){
						GEDCOMHelper.attachEventParticipation(
							roots, eventFlefId, "individual", link.wifeId, "wife", model
						);
					}
				}
			}

			// ---- Process family attributes (RESI) ----
			for(GEDCOMNode attrNode : link.attributeNodes){
				createGroupAttribute(attrNode, group, link.husbandId, link.wifeId, model);
			}

			// ---- Process OBJE nodes ----
			processObjNodes(link, group);
		}
	}

	// ------------------------------------------------------------------------
	// Helper methods for creating records
	// ------------------------------------------------------------------------

	private void createParentChild(String childId, String parentId){
		FLEFRecord relationship = FLEFRecord.createMainRecord(
				IDGenerator.nextId(RelationshipHandler.ID_PREFIX), RelationshipHandler.TYPE)
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", childId)))
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", parentId)))
			.addChild(FLEFRecord.createChildWithTagAndValue("type", "biological_child"))
			.addChild(FLEFRecord.createChildWithTagAndValue("status", "active"))
			.addChild(FLEFRecord.createChildWithTag("audit")
				.addChild(FLEFRecord.createChildWithTag("creation")
					.addChild(FLEFRecord.createChildWithTagAndValue("date",
						DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.DAYS))))
					.addChild(FLEFRecord.createChildWithTagAndValue("comment", "From GEDCOM conversion"))));
		Deduplicator.getDeduplicatedRecordId(model, relationship);
	}

	/**
	 * Creates an EventRecord from a GEDCOM event node, maps its type using GEDCOMMapper,
	 * adds sources, notes, date, place, and audit.
	 */
	private String createAndAddEventRecord(GEDCOMNode eventNode,
		Map<String, GEDCOMNode> noteRawMap,
		Map<String, GEDCOMNode> sourRawMap,
		Map<String, GEDCOMNode> objeRawMap,
		FLEFModel model){
		if(eventNode == null) return null;

		String eventFlefId = IDGenerator.nextId(EventHandler.ID_PREFIX);
		FLEFRecord eventRecord = FLEFRecord.createMainRecord(eventFlefId, EventHandler.TYPE);

		// Determine FLEF event type using GEDCOMMapper.
		// If tag is EVEN, try to get TYPE child for custom classification.
		String gedcomTag = eventNode.getTag();
		String customType = null;
		if("EVEN".equals(gedcomTag) || "MARR".equals(gedcomTag)){
			GEDCOMNode typeNode = GEDCOMHelper.findFirstChild(eventNode, "TYPE");
			if(typeNode != null && typeNode.getValue() != null){
				customType = typeNode.getValue().trim();
			}
		}
		String flefType = GEDCOMMapper.mapEvent(gedcomTag, (customType != null? customType: gedcomTag));
		eventRecord.addChild(FLEFRecord.createChildWithTagAndValue("type",
			("civil".equals(customType) || "religious".equals(customType)? customType + "_": "")
				+ flefType));

		// Date
		GEDCOMNode dateNode = GEDCOMHelper.findFirstChild(eventNode, "DATE");
		if(dateNode != null && dateNode.getValue() != null){
			GEDCOMHelper.attachDate(eventRecord, "date", GEDCOMHelper.getDateTime(dateNode));
		}

		// Place
		GEDCOMNode placNode = GEDCOMHelper.findFirstChild(eventNode, "PLAC");
		GEDCOMNode addrNode = GEDCOMHelper.findFirstChild(eventNode, "ADDR");
		if(placNode != null && placNode.getValue() != null){
			GEDCOMHelper.attachPlaceCitation(eventRecord, model, placNode, addrNode, noteRawMap);
		}

		// Agency (if any)
		GEDCOMNode agncNode = GEDCOMHelper.findFirstChild(eventNode, "AGNC");
		if(agncNode != null && agncNode.getValue() != null){
			eventRecord.addChild(FLEFRecord.createChildWithTagAndValue("agency", agncNode.getValue()));
		}

		// Cause (if any)
		GEDCOMNode causNode = GEDCOMHelper.findFirstChild(eventNode, "CAUS");
		if(causNode != null && causNode.getValue() != null){
			FLEFRecord cause = FLEFRecord.createChildWithTag("cause")
				.addChild(FLEFRecord.createChildWithTagAndValue("reason", causNode.getValue()));
			eventRecord.addChild(cause);
		}

		// Description (for generic EVEN or if value is not Y/N)
		String eventValue = eventNode.getValue();
		if(StringUtils.isNotEmpty(eventValue) && !"Y".equalsIgnoreCase(eventValue) && !"N".equalsIgnoreCase(eventValue)){
			eventRecord.addChild(FLEFRecord.createChildWithTagAndValue("description", eventValue));
		}

		// Notes
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(eventNode, "NOTE")){
			GEDCOMHelper.attachNote(eventRecord, noteNode, noteRawMap);
		}

		// Sources
		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(eventNode, "SOUR")){
			GEDCOMHelper.attachSource(eventRecord, model, sourNode, noteRawMap, sourRawMap, objeRawMap);
		}

		// Multimedia
		for(GEDCOMNode multimediaLinkNode : GEDCOMHelper.findChildren(eventNode, "OBJE")){
			GEDCOMHelper.attachMultimediaLink(eventRecord, model, multimediaLinkNode, objeRawMap);
		}

		// Restriction notice
		GEDCOMHelper.attachRestriction(eventRecord, eventNode);

		// Audit
		eventRecord.addChild(AuditBuilder.build(eventNode));

		model.addRecord(eventRecord);
		return eventFlefId;
	}

	/**
	 * Creates a GroupAttributeRecord for a family attribute (e.g., RESI).
	 * Uses GEDCOMMapper to map the attribute type.
	 */
	private void createGroupAttribute(GEDCOMNode attrNode,
		FLEFRecord group,
		String husbandId,
		String wifeId,
		FLEFModel model){
		if(attrNode == null) return;

		String gedcomTag = attrNode.getTag();
		// Map to FLEF attribute type (e.g., RESI -> residence)
		String flefType = GEDCOMMapper.mapAttribute(gedcomTag, gedcomTag);

		FLEFRecord groupAttribute = FLEFRecord.createMainRecord(
				IDGenerator.nextId(GroupAttributeHandler.ID_PREFIX), GroupAttributeHandler.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue("group", group.getId()))
			.addChild(FLEFRecord.createChildWithTagAndValue("type", flefType));

		// Value (if present)
		String value = attrNode.getValue();
		if(StringUtils.isNotEmpty(value) && !"Y".equalsIgnoreCase(value) && !"N".equalsIgnoreCase(value)){
			groupAttribute.addChild(FLEFRecord.createChildWithTagAndValue("value", value));
		}

		// Date (valid_from / valid_to) – we treat as valid_from if single date
		GEDCOMNode dateNode = GEDCOMHelper.findFirstChild(attrNode, "DATE");
		if(dateNode != null && dateNode.getValue() != null){
			GEDCOMHelper.attachDate(groupAttribute, "valid_from", GEDCOMHelper.getDateTime(dateNode));
			// If there is a range, we could also set valid_to; but GEDCOM DATE for RESI is usually single.
		}

		// Place
		GEDCOMNode placNode = GEDCOMHelper.findFirstChild(attrNode, "PLAC");
		GEDCOMNode addrNode = GEDCOMHelper.findFirstChild(attrNode, "ADDR");
		if(placNode != null && placNode.getValue() != null){
			GEDCOMHelper.attachPlaceCitation(groupAttribute, model, placNode, addrNode, noteRawMap);
		}

		// Notes
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(attrNode, "NOTE")){
			GEDCOMHelper.attachNote(groupAttribute, noteNode, noteRawMap);
		}

		// Sources
		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(attrNode, "SOUR")){
			GEDCOMHelper.attachSource(groupAttribute, model, sourNode, noteRawMap, sourRawMap, objeRawMap);
		}

		// Multimedia
		for(GEDCOMNode multimediaLinkNode : GEDCOMHelper.findChildren(attrNode, "OBJE")){
			GEDCOMHelper.attachMultimediaLink(groupAttribute, model, multimediaLinkNode, objeRawMap);
		}

		// Restriction
		GEDCOMHelper.attachRestriction(groupAttribute, attrNode);

		// Audit
		groupAttribute.addChild(AuditBuilder.build(attrNode));

		// Deduplicate and add
		Deduplicator.getDeduplicatedRecordId(model, groupAttribute);
	}

	// ------------------------------------------------------------------------
	// OBJE handling (unchanged but kept for completeness)
	// ------------------------------------------------------------------------

	private void processObjNodes(FamilyLink link, FLEFRecord group){
		List<GEDCOMNode> objNodes = link.objNodes;
		if(objNodes.isEmpty()) return;

		GEDCOMNode preferredObj = null;
		for(GEDCOMNode obj : objNodes){
			GEDCOMNode primaryNode = GEDCOMHelper.findFirstChild(obj, "_PRIMARY");
			if(primaryNode != null && "Y".equalsIgnoreCase(primaryNode.getValue())){
				preferredObj = obj;
				break;
			}
		}
		if(preferredObj == null){
			for(GEDCOMNode obj : objNodes){
				GEDCOMNode prefNode = GEDCOMHelper.findFirstChild(obj, "_PREF");
				if(prefNode != null && "Y".equalsIgnoreCase(prefNode.getValue())){
					preferredObj = obj;
					break;
				}
			}
		}

		for(GEDCOMNode objNode : objNodes){
			FLEFRecord document = null;
			String objXref = objNode.getXrefId();
			if(objXref != null){
				String cleanId = GEDCOMHelper.cleanId(objXref);
				document = multimediaMap.get(cleanId);
			}
			if(document == null){
				document = createDocumentRecord(objNode);
				Deduplicator.getDeduplicatedRecordId(model, document);
				multimediaMap.put(document.getId(), document);
			}

			if(objNode == preferredObj){
				String fileUri = FLEFRecordHelper.getChildValue(document, "uri");
				if(fileUri != null && !fileUri.isEmpty()){
					FLEFRecord prefImg = FLEFRecord.createChildWithTag("preferred_image");
					prefImg.addChild(FLEFRecord.createChildWithTagAndValue("uri", fileUri));
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
				FLEFRecord source = createSourceRecordFromDocument(document, objNode);
				Deduplicator.getDeduplicatedRecordId(model, source);
				sourceMap.put(source.getId(), source);

				FLEFRecord sourceCitation = FLEFRecord.createChildWithTag("source");
				sourceCitation.addChild(FLEFRecord.createChildWithTagAndValue("source", source.getId()));
				group.addChild(sourceCitation);
			}
		}
	}

	private FLEFRecord createDocumentRecord(GEDCOMNode objNode){
		FLEFRecord document = FLEFRecord.createMainRecord(
			IDGenerator.nextId(DocumentHandler.ID_PREFIX), DocumentHandler.TYPE);
		GEDCOMNode fileNode = GEDCOMHelper.findFirstChild(objNode, "FILE");
		if(fileNode != null && fileNode.getValue() != null){
			document.addChild(FLEFRecord.createChildWithTagAndValue("uri", fileNode.getValue()));
		}
		GEDCOMNode titlNode = GEDCOMHelper.findFirstChild(objNode, "TITL");
		if(titlNode != null && titlNode.getValue() != null){
			document.addChild(FLEFRecord.createChildWithTagAndValue("description", titlNode.getValue()));
		}
		document.addChild(AuditBuilder.build(objNode));
		return document;
	}

	private FLEFRecord createSourceRecordFromDocument(FLEFRecord docRecord, GEDCOMNode objNode){
		FLEFRecord source = FLEFRecord.createMainRecord(
			IDGenerator.nextId(SourceHandler.ID_PREFIX), SourceHandler.TYPE);
		String docDesc = FLEFRecordHelper.getChildValue(docRecord, "description");
		if(docDesc == null || docDesc.isEmpty()) docDesc = "Image";
		FLEFRecord titleRec = FLEFRecord.createChildWithTag("title")
			.addChild(FLEFRecord.createChildWithTagAndValue("value", docDesc));
		source.addChild(titleRec);
		source.addChild(FLEFRecord.createChildWithTagAndValue("document", docRecord.getId()));
		GEDCOMNode dateNode = GEDCOMHelper.findFirstChild(objNode, "_DATE");
		if(dateNode != null && dateNode.getValue() != null){
			GEDCOMNode syntheticDate = new GEDCOMNode(dateNode.getLevel(), "DATE", dateNode.getValue());
			FLEFRecord dateStruct = structParser.parseDateStructure(syntheticDate);
			if(dateStruct != null) source.addChild(dateStruct);
		}
		source.addChild(AuditBuilder.build(objNode));
		return source;
	}

	// ------------------------------------------------------------------------
	// Utility methods
	// ------------------------------------------------------------------------

	private String getDisplayName(String indiId){
		if(indiId == null) return "";
		FLEFRecord indi = individualMap.get(indiId);
		if(indi == null) return "";
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
