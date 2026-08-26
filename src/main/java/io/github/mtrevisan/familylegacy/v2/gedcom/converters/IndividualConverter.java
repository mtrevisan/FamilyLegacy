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

import java.util.List;
import java.util.Map;
import java.util.Set;


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
	private final Map<String, FLEFRecord> sourceMap;
	private final Map<String, FLEFRecord> multimediaMap;
	private final StructureParser structParser;

	private static final Set<String> EVENT_TAGS = Set.of(
		"BIRT", "DEAT", "BURI", "CREM", "ADOP", "BAPM", "BARM", "BASM", "BLES",
		"CHRA", "CONF", "FCOM", "ORDN", "NATU", "EMIG", "IMMI", "CENS", "PROB",
		"WILL", "GRAD", "RETI", "EVEN"
	);
	private static final Set<String> ATTR_TAGS = Set.of(
		"CAST", "DSCR", "EDUC", "IDNO", "NATI", "NCHI", "NMR", "OCCU", "PROP",
		"RELI", "RESI", "SSN", "TITL", "FACT"
	);

	/**
	 * Constructor.
	 *
	 * @param model          the FLEF model
	 * @param individualMap  map of individual IDs to FLEF records
	 * @param sourceMap      map of source IDs to FLEF records (for OBJE → SourceRecord)
	 * @param multimediaMap  map of document IDs to FLEF records (for OBJE → DocumentRecord)
	 * @param placeCache     cache for place records
	 */
	public IndividualConverter(FLEFModel model,
		Map<String, FLEFRecord> individualMap,
		Map<String, FLEFRecord> sourceMap,
		Map<String, FLEFRecord> multimediaMap,
		PlaceCache placeCache) {
		this.model = model;
		this.individualMap = individualMap;
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
		String xref = indiNode.getXrefId();
		String id;
		if (xref != null) {
			String cleaned = IDNormalizer.clean(xref);
			if (isValidIdFormat(cleaned)) {
				id = cleaned;
			} else {
				id = IDGenerator.nextId("I");
			}
		} else {
			id = IDGenerator.nextId("I");
		}
		IDGenerator.registerExistingId(id);

		FLEFRecord indi = FLEFRecord.createChildWithTag("individual");
		indi.setId(id);
		individualMap.put(id, indi);

		// ---- Names ----
		for (GEDCOMNode nameNode : structParser.findChildren(indiNode, "NAME")) {
			FLEFRecord nameRec = structParser.parsePersonalNameStructure(nameNode, model, xref, "individual");
			if (nameRec != null) indi.addChild(nameRec);
		}

		// ---- Sex ----
		GEDCOMNode sexNode = structParser.findFirstChild(indiNode, "SEX");
		if (sexNode != null && sexNode.getValue() != null) {
			String sex = GEDCOMMapper.mapSex(sexNode.getValue());
			indi.addChild(FLEFRecord.createChildWithTagAndValue("sex", sex));
		}

		// ---- Events ----
		for (String tag : EVENT_TAGS) {
			for (GEDCOMNode evtNode : structParser.findChildren(indiNode, tag)) {
				FLEFRecord evtRec = structParser.parseEvent(evtNode, tag);
				if (evtRec != null) indi.addChild(evtRec);
			}
		}

		// ---- Attributes ----
		for (String tag : ATTR_TAGS) {
			for (GEDCOMNode attrNode : structParser.findChildren(indiNode, tag)) {
				FLEFRecord attrRec = structParser.parseAttribute(attrNode, tag);
				if (attrRec != null) indi.addChild(attrRec);
			}
		}

		// ---- OBJE (multimedia) ----
		processObjNodes(indiNode, indi);

		// ---- Notes (GEDCOM NOTE) – inline structs ----
		for (GEDCOMNode noteNode : structParser.findChildren(indiNode, "NOTE")) {
			FLEFRecord noteStruct = structParser.parseNoteStruct(noteNode);
			if (noteStruct != null) indi.addChild(noteStruct);
		}

		// ---- Extra fields (REFN, RIN, ALIA, ASSO, ANCI, DESI) as inline notes ----
		addExtraFields(indiNode, indi);

		// ---- Sources (SOUR) ----
		for (GEDCOMNode sourNode : structParser.findChildren(indiNode, "SOUR")) {
			FLEFRecord sourCitation = structParser.parseSourceCitation(sourNode, model, xref, "individual");
			if (sourCitation != null) indi.addChild(sourCitation);
		}

		// ---- Privacy (RESN) ----
		GEDCOMNode resnNode = structParser.findFirstChild(indiNode, "RESN");
		if (resnNode != null && resnNode.getValue() != null) {
			String level = GEDCOMMapper.mapPrivacyLevel(resnNode.getValue());
			FLEFRecord privacy = FLEFRecord.createChildWithTag("privacy");
			privacy.addChild(FLEFRecord.createChildWithTagAndValue("level", level));
			indi.addChild(privacy);
		}

		// ---- Audit ----
		indi.addChild(structParser.createAudit(indiNode));
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
		List<GEDCOMNode> objNodes = structParser.findChildren(indiNode, "OBJE");
		if (objNodes.isEmpty()) return;

		// Find the primary OBJE (with _PRIMARY Y)
		GEDCOMNode preferredObj = null;
		for (GEDCOMNode obj : objNodes) {
			GEDCOMNode primaryNode = structParser.findFirstChild(obj, "_PRIMARY");
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
					GEDCOMNode cutdNode = structParser.findFirstChild(objNode, "_CUTD");
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
		GEDCOMNode fileNode = structParser.findFirstChild(objNode, "FILE");
		if (fileNode != null && fileNode.getValue() != null) {
			doc.addChild(FLEFRecord.createChildWithTagAndValue("file", fileNode.getValue()));
		}

		// TITL -> description
		GEDCOMNode titlNode = structParser.findFirstChild(objNode, "TITL");
		if (titlNode != null && titlNode.getValue() != null) {
			doc.addChild(FLEFRecord.createChildWithTagAndValue("description", titlNode.getValue()));
		}

		// FORM -> note (inline)
//		GEDCOMNode formNode = structParser.findFirstChild(objNode, "FORM");
//		if (formNode != null && formNode.getValue() != null) {
//			FLEFRecord note = FLEFRecord.createChildWithTag("note");
//			note.addChild(FLEFRecord.createChildWithTagAndValue("value", "Format: " + formNode.getValue()));
//			doc.addChild(note);
//		}

		// Exclude tags that are already used for specific purposes
		Set<String> excludedTags = Set.of("_PRIMARY", "_CUTD", "_PUBL", "_CUT", "_PREF", "_DATE");
		for (GEDCOMNode child : objNode.getChildren()) {
			String tag = child.getTag();
			if (tag.startsWith("_") && child.getValue() != null && !excludedTags.contains(tag)) {
				FLEFRecord note = FLEFRecord.createChildWithTag("note");
				note.addChild(FLEFRecord.createChildWithTagAndValue("value", tag + ": " + child.getValue()));
				doc.addChild(note);
			}
		}

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
		GEDCOMNode dateNode = structParser.findFirstChild(objNode, "_DATE");
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
							GEDCOMNode relaNode = structParser.findFirstChild(child, "RELA");
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

	// ------------------------------------------------------------------------
	// Utility
	// ------------------------------------------------------------------------

	/**
	 * Checks if the ID matches the FLEF format: letters followed by digits.
	 */
	private boolean isValidIdFormat(String id) {
		return id != null && id.matches("^[A-Za-z][A-Za-z0-9]*$");
	}

}
