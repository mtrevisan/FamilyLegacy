package io.github.mtrevisan.familylegacy.v2.gedcom.converters;

import io.github.mtrevisan.familylegacy.v2.gedcom.Deduplicator;
import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.AuditBuilder;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDGenerator;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDNormalizer;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.PlaceCache;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.StructureParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
public class FamilyConverter {

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

	private static class FamilyLink {
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
	 */
	public FamilyConverter(FLEFModel model,
		Map<String, FLEFRecord> familyMap,
		Map<String, FLEFRecord> individualMap,
		Map<String, FLEFRecord> sourceMap,
		Map<String, FLEFRecord> multimediaMap,
		Map<String, GEDCOMNode> noteRawMap,
		Map<String, GEDCOMNode> sourRawMap,
		Map<String, GEDCOMNode> objeRawMap,
		PlaceCache placeCache) {
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
	 * First pass: collect family data and create the GroupRecord.
	 */
	public void collect(GEDCOMNode famNode) {
		String famXref = famNode.getXrefId();
		if (famXref == null) return;

		String cleanFamId = IDNormalizer.clean(famXref);
		IDGenerator.registerExistingId(cleanFamId);

		// Create GroupRecord
		FLEFRecord group = FLEFRecord.createMainRecord(cleanFamId, GroupHandler.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue("type", "family"));
		familyMap.put(cleanFamId, group);

		// Create a link object for later resolution
		FamilyLink link = new FamilyLink();
		link.familyId = cleanFamId;
		link.famNode = famNode;

		// Husband and wife
		GEDCOMNode husbNode = GEDCOMHelper.findFirstChild(famNode, "HUSB");
		if (husbNode != null && husbNode.getValue() != null) {
			link.husbandId = GEDCOMHelper.cleanId(husbNode.getValue());
		}
		GEDCOMNode wifeNode = GEDCOMHelper.findFirstChild(famNode, "WIFE");
		if (wifeNode != null && wifeNode.getValue() != null) {
			link.wifeId = GEDCOMHelper.cleanId(wifeNode.getValue());
		}

		// Children
		for (GEDCOMNode chilNode : GEDCOMHelper.findChildren(famNode, "CHIL")) {
			if (chilNode.getValue() != null) {
				link.childrenIds.add(GEDCOMHelper.cleanId(chilNode.getValue()));
			}
		}

		// Family events
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

		// NCHI (number of children) – store as GroupAttributeRecord if valid
		GEDCOMNode nchiNode = GEDCOMHelper.findFirstChild(famNode, "NCHI");
		if (nchiNode != null && nchiNode.getValue() != null) {
			int reportedCount;
			try {
				reportedCount = Integer.parseInt(nchiNode.getValue().trim());
			} catch (NumberFormatException e) {
				reportedCount = -1;
			}
			if (reportedCount >= 0) {
				int actualCount = link.childrenIds.size();
				if (reportedCount != actualCount) {
					FLEFRecord groupAttribute = FLEFRecord.createMainRecord(IDGenerator.nextId(GroupAttributeHandler.ID_PREFIX), GroupAttributeHandler.TYPE)
						.addChild(AuditBuilder.build(famNode))
						.addChild(FLEFRecord.createChildWithTagAndValue("group", group.getId()))
						.addChild(FLEFRecord.createChildWithTagAndValue("type", "children_count"))
						.addChild(FLEFRecord.createChildWithTagAndValue("value", String.valueOf(reportedCount)));

					Deduplicator.getDeduplicatedRecordId(model, groupAttribute);
				}
			}
		}

		// Sources (SOUR)
		for (GEDCOMNode sourNode : GEDCOMHelper.findChildren(famNode, "SOUR")) {
			GEDCOMHelper.attachSource(group, model, sourNode, noteRawMap, sourRawMap, objeRawMap);
		}

		// Notes (GEDCOM NOTE) – inline structs
		for (GEDCOMNode noteNode : GEDCOMHelper.findChildren(famNode, "NOTE")) {
			GEDCOMHelper.attachNote(group, noteNode, noteRawMap);
		}

		// Multimedia (OBJE)
		for (GEDCOMNode multimediaLinkNode : GEDCOMHelper.findChildren(famNode, "OBJE")) {
			GEDCOMHelper.attachMultimediaLink(group, model, multimediaLinkNode, objeRawMap);
		}

		// Restriction notice (RESN) -> privacy
		GEDCOMHelper.attachRestriction(group, famNode);

		// Audit
		group.addChild(AuditBuilder.build(famNode));

		// Add all sources and notes directly to the group
		for (GEDCOMNode sourNode : link.sourNodes) {
			FLEFRecord sourCitation = structParser.parseSourceCitation(sourNode, model, noteRawMap);
			if (sourCitation != null) group.addChild(sourCitation);
		}
		for (GEDCOMNode noteNode : link.noteNodes) {
			FLEFRecord noteStruct = structParser.parseNoteStruct(noteNode);
			if (noteStruct != null) group.addChild(noteStruct);
		}

		// Store the link for the second pass
		familyLinks.add(link);
	}

	/**
	 * Second pass: resolve all links, create relationships, events, and OBJE handling.
	 */
	public void resolveLinks(List<GEDCOMNode> roots) {
		for (FamilyLink link : familyLinks) {
			FLEFRecord group = familyMap.get(link.familyId);
			if (group == null) continue;

			// ---- Set group name ----
			String husbandName = getDisplayName(link.husbandId);
			String wifeName = getDisplayName(link.wifeId);
			String groupName = "Family of " + husbandName + (wifeName.isEmpty() ? StringUtils.EMPTY : " and " + wifeName);
			if (!groupName.equals("Family of ")) {
				FLEFRecord nameRec = FLEFRecord.createChildWithTag("name")
					.addChild(FLEFRecord.createChildWithTagAndValue("value", groupName));
				group.addChild(nameRec);
			}

			// ---- Spouse relationship (Inter-individual) ----
			if (link.husbandId != null && link.wifeId != null) {
				FLEFRecord relationship = FLEFRecord.createMainRecord(IDGenerator.nextId(RelationshipHandler.ID_PREFIX), RelationshipHandler.TYPE);
				// subject: husband
				relationship.addChild(FLEFRecord.createChildWithTag("subject")
					.addChild(FLEFRecord.createChildWithTagAndValue("individual", link.husbandId)));
				// target: wife
				relationship.addChild(FLEFRecord.createChildWithTag("target")
					.addChild(FLEFRecord.createChildWithTagAndValue("individual", link.wifeId)));

				String marriageType = null;
				for (GEDCOMNode evt : link.events) {
					if ("MARR".equals(evt.getTag())) {
						for (GEDCOMNode subEvt : evt.getChildren()) {
							if ("TYPE".equals(subEvt.getTag())) {
								marriageType = subEvt.getValue();
								break;
							}
						}
					}
				}
				relationship.addChild(FLEFRecord.createChildWithTagAndValue("type", (marriageType == null ? "civil_spouse" : marriageType + "_spouse")));

				// Status: if there is a DIV event, set ended
				boolean hasDivorce = link.events.stream().anyMatch(e -> "DIV".equals(e.getTag()));
				relationship.addChild(FLEFRecord.createChildWithTagAndValue("status", hasDivorce ? "ended" : "active"));

				// Add date from MARR event
				for (GEDCOMNode evt : link.events) {
					if ("MARR".equals(evt.getTag())) {
						FLEFRecord dateStruct = structParser.parseDateStructure(evt);
						if (dateStruct != null) {
							dateStruct.setTag("valid_from");
							relationship.addChild(dateStruct);
						}
						break;
					}
				}
				// Audit for relationship
				relationship.addChild(AuditBuilder.build(link.famNode));

				Deduplicator.getDeduplicatedRecordId(model, relationship);
			}

			// ---- Parent-child relationships ----
			for (String childId : link.childrenIds) {
				if (individualMap.containsKey(childId)) {
					if (link.husbandId != null) {
						createParentChild(childId, link.husbandId);
					}
					if (link.wifeId != null) {
						createParentChild(childId, link.wifeId);
					}
				}
			}

			// ---- Family events as discrete EventRecords & EventParticipationRecords ----
			for (GEDCOMNode evtNode : link.events) {
				String eventFlefId = GEDCOMHelper.createAndAddEventRecord(
					evtNode, roots, noteRawMap, sourRawMap, objeRawMap, model
				);

				if (eventFlefId != null) {
					// Attach Group to Event
					if (link.husbandId == null && link.wifeId == null){
						GEDCOMHelper.attachEventParticipation(roots, eventFlefId, "group", group.getId(), "family", model);
					}

					// Attach Husband to Event
					if (link.husbandId != null) {
						GEDCOMHelper.attachEventParticipation(roots, eventFlefId, "individual", link.husbandId, "husband", model);
					}

					// Attach Wife to Event
					if (link.wifeId != null) {
						GEDCOMHelper.attachEventParticipation(roots, eventFlefId, "individual", link.wifeId, "wife", model);
					}
				}
			}

			// ---- Process OBJE nodes ----
			processObjNodes(link, group);
		}
	}

	private void createParentChild(String childId, String parentId) {
		FLEFRecord relationship = FLEFRecord.createMainRecord(IDGenerator.nextId(RelationshipHandler.ID_PREFIX), RelationshipHandler.TYPE)
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", childId))
			)
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", parentId))
			)
			.addChild(FLEFRecord.createChildWithTagAndValue("type", "biological_child"))
			.addChild(FLEFRecord.createChildWithTagAndValue("status", "active"))
			.addChild(FLEFRecord.createChildWithTag("audit")
				.addChild(FLEFRecord.createChildWithTag("creation")
					.addChild(FLEFRecord.createChildWithTagAndValue("date", DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.DAYS))))
					.addChild(FLEFRecord.createChildWithTagAndValue("comment", "From GEDCOM conversion"))
				)
			);

		Deduplicator.getDeduplicatedRecordId(model, relationship);
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
	private void processObjNodes(FamilyLink link, FLEFRecord group) {
		List<GEDCOMNode> objNodes = link.objNodes;
		if (objNodes.isEmpty()) return;

		// Find the primary OBJE (with _PRIMARY Y)
		GEDCOMNode preferredObj = null;
		for (GEDCOMNode obj : objNodes) {
			GEDCOMNode primaryNode = GEDCOMHelper.findFirstChild(obj, "_PRIMARY");
			if (primaryNode != null && "Y".equalsIgnoreCase(primaryNode.getValue())) {
				preferredObj = obj;
				break;
			}
		}

		for (GEDCOMNode objNode : objNodes) {
			// 1. Create or retrieve DocumentRecord
			FLEFRecord document = null;
			String objXref = objNode.getXrefId();
			if (objXref != null) {
				String cleanId = GEDCOMHelper.cleanId(objXref);
				document = multimediaMap.get(cleanId);
			}
			if (document == null) {
				document = createDocumentRecord(objNode);
				Deduplicator.getDeduplicatedRecordId(model, document);
				multimediaMap.put(document.getId(), document);
			}

			// 2. If primary: create preferred_image
			if (objNode == preferredObj) {
				String fileUri = FLEFRecordHelper.getChildValue(document, "uri");
				if (fileUri != null && !fileUri.isEmpty()) {
					FLEFRecord prefImg = FLEFRecord.createChildWithTag("preferred_image");
					prefImg.addChild(FLEFRecord.createChildWithTagAndValue("uri", fileUri));

					// Crop from _CUTD
					GEDCOMNode cutdNode = GEDCOMHelper.findFirstChild(objNode, "_CUTD");
					if (cutdNode != null && cutdNode.getValue() != null) {
						String[] parts = cutdNode.getValue().split(" ");
						if (parts.length == 4) {
							try {
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
							} catch (NumberFormatException ignored) {
							}
						}
					}
					group.addChild(prefImg);
				}
			} else {
				// 3. Non-primary: create a SourceRecord and link it to the group
				FLEFRecord source = createSourceRecordFromDocument(document, objNode);
				Deduplicator.getDeduplicatedRecordId(model, source);
				sourceMap.put(source.getId(), source);

				// Create SourceCitation for the group
				FLEFRecord sourceCitation = FLEFRecord.createChildWithTag("source");
				FLEFRecord sourceRef = FLEFRecord.createChildWithTag("source");
				sourceRef.setValue(source.getId());
				sourceCitation.addChild(sourceRef);
				group.addChild(sourceCitation);
			}
		}
	}

	/**
	 * Creates a DocumentRecord from a GEDCOM OBJE node.
	 */
	private FLEFRecord createDocumentRecord(GEDCOMNode objNode) {
		FLEFRecord document = FLEFRecord.createMainRecord(IDGenerator.nextId(DocumentHandler.ID_PREFIX), DocumentHandler.TYPE);

		// FILE -> uri
		GEDCOMNode fileNode = GEDCOMHelper.findFirstChild(objNode, "FILE");
		if (fileNode != null && fileNode.getValue() != null) {
			document.addChild(FLEFRecord.createChildWithTagAndValue("uri", fileNode.getValue()));
		}

		// TITL -> description
		GEDCOMNode titlNode = GEDCOMHelper.findFirstChild(objNode, "TITL");
		if (titlNode != null && titlNode.getValue() != null) {
			document.addChild(FLEFRecord.createChildWithTagAndValue("description", titlNode.getValue()));
		}

		// Audit
		document.addChild(AuditBuilder.build(objNode));
		return document;
	}

	/**
	 * Creates a SourceRecord that references a DocumentRecord.
	 */
	private FLEFRecord createSourceRecordFromDocument(FLEFRecord docRecord, GEDCOMNode objNode) {
		FLEFRecord source = FLEFRecord.createMainRecord(IDGenerator.nextId(SourceHandler.ID_PREFIX), SourceHandler.TYPE);

		// Title: use document description or default
		String docDesc = FLEFRecordHelper.getChildValue(docRecord, "description");
		if (docDesc == null || docDesc.isEmpty()) {
			docDesc = "Image";
		}
		FLEFRecord titleRec = FLEFRecord.createChildWithTag("title")
			.addChild(FLEFRecord.createChildWithTagAndValue("value", docDesc));
		source.addChild(titleRec);

		// Reference to the DocumentRecord
		FLEFRecord docRef = FLEFRecord.createChildWithTagAndValue("document", docRecord.getId());
		source.addChild(docRef);

		// Date from _DATE (if present)
		GEDCOMNode dateNode = GEDCOMHelper.findFirstChild(objNode, "_DATE");
		if (dateNode != null && dateNode.getValue() != null) {
			GEDCOMNode syntheticDate = new GEDCOMNode(dateNode.getLevel(), "DATE", dateNode.getValue());
			FLEFRecord dateStruct = structParser.parseDateStructure(syntheticDate);
			if (dateStruct != null) source.addChild(dateStruct);
		}

		// Audit
		source.addChild(AuditBuilder.build(objNode));
		return source;
	}

	// ------------------------------------------------------------------------
	// Utility
	// ------------------------------------------------------------------------

	/**
	 * Helper to extract a display name from an individual ID.
	 */
	private String getDisplayName(String indiId) {
		if (indiId == null) return StringUtils.EMPTY;
		FLEFRecord indi = individualMap.get(indiId);
		if (indi == null) return StringUtils.EMPTY;

		for (FLEFRecord name : indi.getChildren()) {
			if ("name".equals(name.getTag())) {
				for (FLEFRecord part : name.getChildren()) {
					if ("part".equals(part.getTag())) {
						String type = FLEFRecordHelper.getChildValue(part, "type");
						if ("given".equals(type)) {
							String val = FLEFRecordHelper.getChildValue(part, "value");
							if (val != null) return val;
						}
					}
				}
			}
		}
		return "Unknown";
	}
}