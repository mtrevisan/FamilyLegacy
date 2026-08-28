package io.github.mtrevisan.familylegacy.v2.gedcom.converters;

import io.github.mtrevisan.familylegacy.v2.gedcom.Deduplicator;
import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.AuditBuilder;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDGenerator;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.PlaceCache;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.StructureParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;


/**
 * Converts GEDCOM INDI records to FLEF IndividualRecord.
 * <p>
 * Handles:
 * <ul>
 *   <li>Names (NAME) → PersonalNameStructure</li>
 *   <li>Sex (SEX) → sex</li>
 *   <li>Events (BIRT, DEAT, etc.) → EventRecord</li>
 *   <li>Attributes (CAST, RESI, OCCU, etc.) → IndividualAttributeRecord</li>
 *   <li>OBJE → DocumentRecord + preferred_image (if _PRIMARY Y) or SourceRecord (otherwise)</li>
 *   <li>Notes (NOTE) → inline note structs</li>
 *   <li>Extra fields (REFN, RIN, ALIA, ASSO, etc.) → inline note structs</li>
 *   <li>Sources (SOUR) → SourceCitation</li>
 *   <li>Privacy (RESN) → PrivacyStructure</li>
 *   <li>Audit (CHAN) → AuditStructure</li>
 * </ul>
 */
public class IndividualConverter {

	private final FLEFModel model;
	private final Map<String, FLEFRecord> individualMap;
	private final Map<String, GEDCOMNode> noteRawMap;
	private final Map<String, GEDCOMNode> sourRawMap;
	private final Map<String, GEDCOMNode> objeRawMap;
	private final Map<String, FLEFRecord> sourceMap;
	private final Map<String, FLEFRecord> multimediaMap;
	private final StructureParser structParser;


	/**
	 * Constructor.
	 *
	 * @param model          the FLEF model
	 * @param individualMap  map of individual IDs to FLEF records
	 * @param sourceMap      map of source IDs to FLEF records (for OBJE → SourceRecord)
	 * @param multimediaMap  map of document IDs to FLEF records (for OBJE → DocumentRecord)
	 * @param placeCache     cache for place records
	 */
	public IndividualConverter(FLEFModel model, Map<String, FLEFRecord> individualMap, Map<String, GEDCOMNode> noteRawMap,
			Map<String, GEDCOMNode> sourRawMap, Map<String, GEDCOMNode> objeRawMap, Map<String, FLEFRecord> sourceMap, Map<String, FLEFRecord> multimediaMap, PlaceCache placeCache) {
		this.model = model;
		this.individualMap = individualMap;
		this.noteRawMap = noteRawMap;
		this.sourRawMap = sourRawMap;
		this.objeRawMap = objeRawMap;
		this.sourceMap = sourceMap;
		this.multimediaMap = multimediaMap;
		this.structParser = new StructureParser(placeCache);
	}

	/**
	 * Converts a GEDCOM INDI node into an FLEF IndividualRecord.
	 *
	 * @param indiNode the GEDCOM node with tag "INDI"
	 */
	public void convert(GEDCOMNode indiNode, List<GEDCOMNode> roots) {
		String id = GEDCOMHelper.extractId(indiNode, IndividualHandler.ID_PREFIX);
		FLEFRecord individual = FLEFRecord.createMainRecord(id, IndividualHandler.TYPE);
		individualMap.put(id, individual);

		// ---- Names ----
		for (GEDCOMNode nameNode : GEDCOMHelper.findChildren(indiNode, "NAME")) {
			GEDCOMHelper.attachPersonalNameStructure(individual, nameNode, model, noteRawMap, sourRawMap, objeRawMap);
		}

		// ---- Sex ----
		GEDCOMNode sexNode = GEDCOMHelper.findFirstChild(indiNode, "SEX");
		if (sexNode != null && sexNode.getValue() != null) {
			individual.addChild(FLEFRecord.createChildWithTagAndValue("sex", GEDCOMMapper.mapSex(sexNode.getValue())));
		}

		// ---- Events ----
		for (String tag : GEDCOMMapper.EVENT_MAP.keySet()) {
			for (GEDCOMNode evtNode : GEDCOMHelper.findChildren(indiNode, tag)) {
				GEDCOMHelper.attachIndividualEvent(individual, model, evtNode, noteRawMap, sourRawMap, objeRawMap, roots);
			}
		}

		// ---- Attributes ----
		for (String tag : GEDCOMMapper.ATTRIBUTE_MAP.keySet()) {
			for (GEDCOMNode attrNode : GEDCOMHelper.findChildren(indiNode, tag)) {
				GEDCOMHelper.attachIndividualAttribute(individual, model, attrNode, noteRawMap, sourRawMap, objeRawMap, roots);
			}
		}

		// ---- Sources (SOUR) ----
		for (GEDCOMNode sourNode : GEDCOMHelper.findChildren(indiNode, "SOUR")) {
			GEDCOMHelper.attachSource(individual, model,
				sourNode, noteRawMap, sourRawMap, objeRawMap);
		}

		// ---- Notes (GEDCOM NOTE) – inline structs ----
		for (GEDCOMNode noteNode : GEDCOMHelper.findChildren(indiNode, "NOTE")) {
			GEDCOMHelper.attachNote(individual,
				noteNode, noteRawMap);
		}

		// Multimedia (OBJE)
		for (GEDCOMNode multimediaLinkNode : GEDCOMHelper.findChildren(indiNode, "OBJE")) {
			GEDCOMHelper.attachMultimediaLink(individual, model,
				multimediaLinkNode, objeRawMap);
		}

		// ---- FAMC (child to family link) ----
		GEDCOMHelper.attachFamilyOfOrigin(individual, model,
			indiNode, noteRawMap, roots);

		// ---- FAMS (spouse to family link) ----
		GEDCOMHelper.attachSpouseToFamily(individual, model,
			indiNode, noteRawMap);

		// ---- Extra fields (REFN, RIN, ALIA, ASSO, ANCI, DESI) as inline notes ----
		addExtraFields(indiNode);

		// ---- Privacy (RESN) ----
		GEDCOMHelper.attachRestriction(individual, indiNode);

		// ---- Audit ----
		individual.addChild(AuditBuilder.build(indiNode));

		model.addRecord(individual);
	}

	// ------------------------------------------------------------------------
	// OBJE handling
	// ------------------------------------------------------------------------

	/**
	 * Processes all OBJE nodes attached to the individual:
	 * - Creates a DocumentRecord for each.
	 * - If _PRIMARY Y, creates preferred_image on the individual.
	 * - Otherwise, creates a SourceRecord and a SourceCitation for the individual.
	 */
	private void processObjNodes(GEDCOMNode indiNode, FLEFRecord indi) {
		List<GEDCOMNode> objNodes = GEDCOMHelper.findChildren(indiNode, "OBJE");
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

				// check for duplicates before adding
				Deduplicator.getDeduplicatedRecordId(model, document);

				multimediaMap.put(document.getId(), document);
			}

			// 2. If primary: create preferred_image
			if (objNode == preferredObj) {
				String fileUri = FLEFRecordHelper.getChildValue(document, "file");
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
					indi.addChild(prefImg);
				}
			} else {
				// 3. Non-primary: create a SourceRecord and link it to the individual
				FLEFRecord source = createSourceRecordFromDocument(document, objNode);

				// check for duplicates before adding
				Deduplicator.getDeduplicatedRecordId(model, source);

				sourceMap.put(source.getId(), source);

				// Create SourceCitation for the individual
				FLEFRecord sourceCitation = FLEFRecord.createChildWithTag("source");
				FLEFRecord sourceRef = FLEFRecord.createChildWithTag("source");
				sourceRef.setValue(source.getId());
				sourceCitation.addChild(sourceRef);
				indi.addChild(sourceCitation);
			}
		}
	}

	/**
	 * Creates a DocumentRecord from a GEDCOM OBJE node.
	 * Excludes tags that are already used for specific purposes.
	 */
	private FLEFRecord createDocumentRecord(GEDCOMNode objNode) {
		FLEFRecord doc = FLEFRecord.createMainRecord(IDGenerator.nextId(DocumentHandler.ID_PREFIX), DocumentHandler.TYPE);

		// FILE -> file
		GEDCOMNode fileNode = GEDCOMHelper.findFirstChild(objNode, "FILE");
		if (fileNode != null && fileNode.getValue() != null) {
			doc.addChild(FLEFRecord.createChildWithTagAndValue("uri", fileNode.getValue()));
		}

		// TITL -> description
		GEDCOMNode titlNode = GEDCOMHelper.findFirstChild(objNode, "TITL");
		if (titlNode != null && titlNode.getValue() != null) {
			doc.addChild(FLEFRecord.createChildWithTagAndValue("description", titlNode.getValue()));
		}

		// FORM -> note (inline)
//		GEDCOMNode formNode = GEDCOMHelper.findFirstChild(objNode, "FORM");
//		if (formNode != null && formNode.getValue() != null) {
//			FLEFRecord note = FLEFRecord.createChildWithTag("note")
//				.addChild(FLEFRecord.createChildWithTagAndValue("text", "Format: " + formNode.getValue()))
//				.addChild(AuditBuilder.build(objNode));
//			doc.addChild(note);
//		}

//		// Exclude tags that are already used for specific purposes
//		Set<String> excludedTags = Set.of("_PRIMARY", "_CUTD", "_PUBL", "_CUT", "_PREF", "_DATE");
//		for (GEDCOMNode child : objNode.getChildren()) {
//			String tag = child.getTag();
//			if (tag.startsWith("_") && child.getValue() != null && !excludedTags.contains(tag)) {
//				FLEFRecord note = FLEFRecord.createChildWithTag("note")
//					.addChild(FLEFRecord.createChildWithTagAndValue("text", tag + ": " + child.getValue()));
//					.addChild(AuditBuilder.build(child));
//				doc.addChild(note);
//			}
//		}

		// Audit
		doc.addChild(AuditBuilder.build(objNode));
		return doc;
	}

	/**
	 * Creates a SourceRecord that references a DocumentRecord.
	 */
	private FLEFRecord createSourceRecordFromDocument(FLEFRecord docRecord, GEDCOMNode objNode) {
		String id = IDGenerator.nextId(SourceHandler.ID_PREFIX);
		FLEFRecord source = FLEFRecord.createChildWithTag("source");
		source.setId(id);

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
	// Extra fields as inline notes
	// ------------------------------------------------------------------------

	/**
	 * Converts GEDCOM extra fields (REFN, RIN, ALIA, ASSO, ANCI, DESI) into inline note structs.
	 */
	private void addExtraFields(GEDCOMNode node){
		for(GEDCOMNode child : node.getChildren()){
			String tag = child.getTag();
			if(child.getValue() != null){
				switch(tag){
					case "ALIA" -> {
						FLEFRecord identityHypothesis = FLEFRecord.createMainRecord(IDGenerator.nextId(IdentityHypothesisHandler.ID_PREFIX), IdentityHypothesisHandler.TYPE)
							.addChild(FLEFRecord.createChildWithTag("identity")
								.addChild(FLEFRecord.createChildWithTagAndValue("individual", node.getXrefId()))
							)
							.addChild(FLEFRecord.createChildWithTag("identity")
								.addChild(FLEFRecord.createChildWithTagAndValue("individual", GEDCOMHelper.cleanId(child.getValue())))
							)
							.addChild(AuditBuilder.build(node));

						// check for duplicates before adding
						Deduplicator.getDeduplicatedRecordId(model, identityHypothesis);
					}
					case "ASSO" -> {
//						GEDCOMNode typeNode = GEDCOMHelper.findFirstChild(child, "TYPE");
//						if("FAM".equals(typeNode.getValue())){}
//						else if("INDI".equals(typeNode.getValue())){}
						GEDCOMNode relaNode = GEDCOMHelper.findFirstChild(child, "RELA");
						String relation = (relaNode != null && StringUtils.isNotEmpty(relaNode.getValue())? relaNode.getValue(): "unknown");

						FLEFRecord relationship = FLEFRecord.createMainRecord(IDGenerator.nextId(RelationshipHandler.ID_PREFIX), RelationshipHandler.TYPE)
							// subject: child
							.addChild(FLEFRecord.createChildWithTag("subject")
								.addChild(FLEFRecord.createChildWithTagAndValue("individual", node.getXrefId()))
							)
							// target: group
							.addChild(FLEFRecord.createChildWithTag("target")
								.addChild(FLEFRecord.createChildWithTagAndValue("individual", GEDCOMHelper.cleanId(child.getValue())))
							)
							.addChild(FLEFRecord.createChildWithTagAndValue("type", relation));
						relationship.addChild(AuditBuilder.build(node));

						// ---- Sources (SOUR) ----
						for (GEDCOMNode sourNode : GEDCOMHelper.findChildren(child, "SOUR")) {
							GEDCOMHelper.attachSource(relationship, model,
								sourNode, noteRawMap, sourRawMap, objeRawMap);
						}

						// ---- Notes (GEDCOM NOTE) – inline structs ----
						for (GEDCOMNode noteNode : GEDCOMHelper.findChildren(child, "NOTE")) {
							GEDCOMHelper.attachNote(relationship,
								noteNode, noteRawMap);
						}

						// check for duplicates before adding
						Deduplicator.getDeduplicatedRecordId(model, relationship);
					}
//					case "ANCI" -> text = "Ancestor interest: " + child.getValue();
//					case "DESI" -> text = "Descendant interest: " + child.getValue();
				}
			}
		}
	}

}
