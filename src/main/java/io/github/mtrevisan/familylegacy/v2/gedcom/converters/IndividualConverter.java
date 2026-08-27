package io.github.mtrevisan.familylegacy.v2.gedcom.converters;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDGenerator;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDNormalizer;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.PlaceCache;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.StructureParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;

import java.util.List;
import java.util.Locale;
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
	public void convert(GEDCOMNode indiNode) {
		String id = GEDCOMHelper.extractId(indiNode, IndividualHandler.ID_PREFIX);
		FLEFRecord individual = FLEFRecord.createMainRecord(id, "individual");
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

		//TODO here
		// ---- Events ----
		for (String tag : GEDCOMMapper.EVENT_MAP.keySet()) {
			for (GEDCOMNode evtNode : GEDCOMHelper.findChildren(indiNode, tag)) {
				GEDCOMHelper.attachIndividualEvent(individual, model, evtNode, noteRawMap, sourRawMap, objeRawMap);
			}
		}

		// ---- Attributes ----
		for (String tag : GEDCOMMapper.ATTRIBUTE_MAP.keySet()) {
			for (GEDCOMNode attrNode : GEDCOMHelper.findChildren(indiNode, tag)) {
				FLEFRecord attrRec = structParser.parseAttribute(attrNode, tag, noteRawMap);
				if (attrRec != null) individual.addChild(attrRec);
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

		// ---- Extra fields (REFN, RIN, ALIA, ASSO, ANCI, DESI) as inline notes ----
		addExtraFields(indiNode, individual);

		// ---- FAMC (child to family link) ----
		for (GEDCOMNode famcNode : GEDCOMHelper.findChildren(indiNode, "FAMC")) {
			if (famcNode.getValue() != null) {
				StringBuilder sb = new StringBuilder("Child of family: ").append(famcNode.getValue());
				GEDCOMNode pediNode = GEDCOMHelper.findFirstChild(famcNode, "PEDI");
				if (pediNode != null && pediNode.getValue() != null) {
					sb.append(" (Pedigree: ").append(pediNode.getValue()).append(")");
				}
				GEDCOMNode statNode = GEDCOMHelper.findFirstChild(famcNode, "STAT");
				if (statNode != null && statNode.getValue() != null) {
					sb.append(" (Status: ").append(statNode.getValue()).append(")");
				}
				FLEFRecord note = structParser.createNoteStruct(sb.toString(), famcNode);
				if (note != null) individual.addChild(note);
			}
		}

		// ---- FAMS (spouse to family link) ----
		for (GEDCOMNode famsNode : GEDCOMHelper.findChildren(indiNode, "FAMS")) {
			if (famsNode.getValue() != null) {
				String text = "Spouse in family: " + famsNode.getValue();
				FLEFRecord note = structParser.createNoteStruct(text, famsNode);
				if (note != null) individual.addChild(note);
			}
		}

//		// ---- SUBM (submitter) ----
//		for (GEDCOMNode subNode : GEDCOMHelper.findChildren(indiNode, "SUBM")) {
//			if (subNode.getValue() != null) {
//				indi.addChild(FLEFRecord.createChildWithTagAndValue("_submitter_ref", subNode.getValue()));
//			}
//		}

		// ---- Privacy (RESN) ----
		GEDCOMHelper.attachRestriction(individual, indiNode);

		// ---- Audit ----
		individual.addChild(structParser.createAudit(indiNode));

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
			FLEFRecord docRecord = null;
			String objXref = objNode.getXrefId();
			if (objXref != null) {
				String cleanId = IDNormalizer.clean(objXref);
				docRecord = multimediaMap.get(cleanId);
			}
			if (docRecord == null) {
				docRecord = createDocumentRecord(objNode);
				model.addRecord(docRecord);
				multimediaMap.put(docRecord.getId(), docRecord);
			}

			// 2. If primary: create preferred_image
			if (objNode == preferredObj) {
				String fileUri = FLEFRecordHelper.getChildValue(docRecord, "file");
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
				FLEFRecord sourceRecord = createSourceRecordFromDocument(docRecord, objNode);
				model.addRecord(sourceRecord);
				sourceMap.put(sourceRecord.getId(), sourceRecord);

				// Create SourceCitation for the individual
				FLEFRecord sourceCitation = FLEFRecord.createChildWithTag("source");
				FLEFRecord sourceRef = FLEFRecord.createChildWithTag("source");
				sourceRef.setValue(sourceRecord.getId());
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
		String id = IDGenerator.nextId("D");
		FLEFRecord doc = FLEFRecord.createChildWithTag("document");
		doc.setId(id);

		// FILE -> file
		GEDCOMNode fileNode = GEDCOMHelper.findFirstChild(objNode, "FILE");
		if (fileNode != null && fileNode.getValue() != null) {
			doc.addChild(FLEFRecord.createChildWithTagAndValue("file", fileNode.getValue()));
		}

		// TITL -> description
		GEDCOMNode titlNode = GEDCOMHelper.findFirstChild(objNode, "TITL");
		if (titlNode != null && titlNode.getValue() != null) {
			doc.addChild(FLEFRecord.createChildWithTagAndValue("description", titlNode.getValue()));
		}

		// FORM -> note (inline)
//		GEDCOMNode formNode = GEDCOMHelper.findFirstChild(objNode, "FORM");
//		if (formNode != null && formNode.getValue() != null) {
//			FLEFRecord note = FLEFRecord.createChildWithTag("note");
//			note.addChild(FLEFRecord.createChildWithTagAndValue("value", "Format: " + formNode.getValue()));
//			doc.addChild(note);
//		}

//		// Exclude tags that are already used for specific purposes
//		Set<String> excludedTags = Set.of("_PRIMARY", "_CUTD", "_PUBL", "_CUT", "_PREF", "_DATE");
//		for (GEDCOMNode child : objNode.getChildren()) {
//			String tag = child.getTag();
//			if (tag.startsWith("_") && child.getValue() != null && !excludedTags.contains(tag)) {
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
	private FLEFRecord createSourceRecordFromDocument(FLEFRecord docRecord, GEDCOMNode objNode) {
		String id = IDGenerator.nextId("S");
		FLEFRecord source = FLEFRecord.createChildWithTag("source");
		source.setId(id);

		// Title: use document description or default
		String docDesc = FLEFRecordHelper.getChildValue(docRecord, "description");
		if (docDesc == null || docDesc.isEmpty()) {
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
		if (dateNode != null && dateNode.getValue() != null) {
			GEDCOMNode syntheticDate = new GEDCOMNode(dateNode.getLevel(), "DATE", dateNode.getValue());
			FLEFRecord dateStruct = structParser.parseDateStructure(syntheticDate);
			if (dateStruct != null) source.addChild(dateStruct);
		}

		// Audit
		source.addChild(structParser.createAudit(objNode));
		return source;
	}

	// ------------------------------------------------------------------------
	// Extra fields as inline notes
	// ------------------------------------------------------------------------

	/**
	 * Converts GEDCOM extra fields (REFN, RIN, ALIA, ASSO, ANCI, DESI) into inline note structs.
	 */
	private void addExtraFields(GEDCOMNode node, FLEFRecord target){
		for(GEDCOMNode child : node.getChildren()){
			String tag = child.getTag();
			if(tag.equals("REFN") /*|| tag.equals("RIN")*/ || tag.equals("RFN") || tag.equals("AFN") ||
				tag.equals("ALIA") || tag.equals("ASSO") || tag.equals("ANCI") || tag.equals("DESI")){
				if(child.getValue() != null){
					String text = tag + ": " + child.getValue();
					switch(tag){
						case "ALIA" -> text = "Alias: " + IDNormalizer.clean(child.getValue());
						case "ASSO" -> {
							String assocId = IDNormalizer.clean(child.getValue());
							StringBuilder sb = new StringBuilder("Associated with: ").append(assocId);
							GEDCOMNode relaNode = GEDCOMHelper.findFirstChild(child, "RELA");
							if(relaNode != null && relaNode.getValue() != null){
								sb.append(" (Relation: ").append(relaNode.getValue()).append(')');
							}
							text = sb.toString();
						}
						case "ANCI" -> text = "Ancestor interest: " + child.getValue();
						case "DESI" -> text = "Descendant interest: " + child.getValue();
					}
					// Usa createNoteStruct per aggiungere audit
					FLEFRecord note = structParser.createNoteStruct(text, child);
					if(note != null) target.addChild(note);
				}
			}
		}
	}

}
