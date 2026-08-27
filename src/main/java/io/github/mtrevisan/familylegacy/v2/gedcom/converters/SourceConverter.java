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
 * Converts a GEDCOM SOURCE_RECORD (SOUR) into an FLEF SourceRecord.
 * Handles all fields defined in the GEDCOM 5.5.1 specification.
 */
public class SourceConverter {

	private final FLEFModel model;
	private final Map<String, FLEFRecord> sourceMap;
	private final Map<String, FLEFRecord> repositoryMap;
	private final Map<String, FLEFRecord> multimediaMap;
	private final StructureParser structParser;

	/**
	 * Constructor.
	 *
	 * @param model         the FLEF model
	 * @param sourceMap     map of source IDs to FLEF records
	 * @param repositoryMap map of repository IDs to FLEF records
	 * @param multimediaMap map of document IDs to FLEF records
	 * @param placeCache    cache for place records
	 */
	public SourceConverter(FLEFModel model,
		Map<String, FLEFRecord> sourceMap,
		Map<String, FLEFRecord> repositoryMap,
		Map<String, FLEFRecord> multimediaMap,
		PlaceCache placeCache) {
		this.model = model;
		this.sourceMap = sourceMap;
		this.repositoryMap = repositoryMap;
		this.multimediaMap = multimediaMap;
		this.structParser = new StructureParser(placeCache);
	}

	/**
	 * Converts a GEDCOM SOUR node into an FLEF SourceRecord.
	 *
	 * @param sourNode the GEDCOM node with tag "SOUR"
	 */
	public void convert(GEDCOMNode sourNode) {
		String xref = sourNode.getXrefId();
		if (xref == null) return;

		String cleanId = IDNormalizer.clean(xref);
		IDGenerator.registerExistingId(cleanId);

		FLEFRecord source = FLEFRecord.createChildWithTag("source");
		source.setId(cleanId);
		sourceMap.put(cleanId, source);

		// ---- 1. TITL (title) with CONC/CONT ----
		for (GEDCOMNode titlNode : GEDCOMHelper.findChildren(sourNode, "TITL")) {
			String fullTitle = GEDCOMHelper.extractFullText(titlNode);
			if (fullTitle != null && !fullTitle.isBlank()) {
				FLEFRecord titleRec = FLEFRecord.createChildWithTag("title");
				titleRec.addChild(FLEFRecord.createChildWithTagAndValue("value", fullTitle));
				source.addChild(titleRec);
			}
		}

		// ---- 2. AUTH (author) with CONC/CONT ----
		GEDCOMNode authNode = GEDCOMHelper.findFirstChild(sourNode, "AUTH");
		if (authNode != null) {
			String fullAuthor = GEDCOMHelper.extractFullText(authNode);
			if (fullAuthor != null && !fullAuthor.isBlank()) {
				source.addChild(FLEFRecord.createChildWithTagAndValue("author", fullAuthor));
			}
		}

		// ---- 3. PUBL (publication facts) with CONC/CONT ----
		GEDCOMNode publNode = GEDCOMHelper.findFirstChild(sourNode, "PUBL");
		if (publNode != null) {
			String fullPubl = GEDCOMHelper.extractFullText(publNode);
			if (fullPubl != null && !fullPubl.isBlank()) {
				source.addChild(FLEFRecord.createChildWithTagAndValue("publisher", fullPubl));
			}
		}

		// ---- 4. DATE (source creation date) ----
		GEDCOMNode dateNode = GEDCOMHelper.findFirstChild(sourNode, "DATE");
		if (dateNode != null) {
			FLEFRecord dateStruct = structParser.parseDateStructure(dateNode);
			if (dateStruct != null) source.addChild(dateStruct);
		}

		// ---- 5. DATA block (EVEN, AGNC, PLAC, NOTE) ----
		GEDCOMNode dataNode = GEDCOMHelper.findFirstChild(sourNode, "DATA");
		if (dataNode != null) {
			// 5a. DATA/EVEN (events recorded) with optional DATE and PLAC
			for (GEDCOMNode evenNode : GEDCOMHelper.findChildren(dataNode, "EVEN")) {
				StringBuilder sb = new StringBuilder("Events recorded: ");
				if (evenNode.getValue() != null) {
					sb.append(evenNode.getValue());
				}
				GEDCOMNode evenDate = GEDCOMHelper.findFirstChild(evenNode, "DATE");
				if (evenDate != null && evenDate.getValue() != null) {
					sb.append(" (Date: ").append(evenDate.getValue()).append(")");
				}
				GEDCOMNode evenPlac = GEDCOMHelper.findFirstChild(evenNode, "PLAC");
				if (evenPlac != null && evenPlac.getValue() != null) {
					sb.append(" (Place: ").append(evenPlac.getValue()).append(")");
				}
				FLEFRecord note = structParser.createNoteStruct(sb.toString(), evenNode);
				if (note != null) source.addChild(note);
			}

//			// 5b. DATA/AGNC (responsible agency)
//			GEDCOMNode agncNode = GEDCOMHelper.findFirstChild(dataNode, "AGNC");
//			if (agncNode != null && agncNode.getValue() != null) {
//				source.addChild(FLEFRecord.createChildWithTagAndValue("_agency", agncNode.getValue()));
//			}

			// 5c. DATA/PLAC (source jurisdiction place)
			GEDCOMNode placNode = GEDCOMHelper.findFirstChild(dataNode, "PLAC");
			if (placNode != null) {
				FLEFRecord placeCitation = structParser.parsePlaceCitation(placNode);
				if (placeCitation != null) source.addChild(placeCitation);
			}

			// 5d. DATA/NOTE (notes inside DATA)
			for (GEDCOMNode dataNoteNode : GEDCOMHelper.findChildren(dataNode, "NOTE")) {
				FLEFRecord noteStruct = structParser.parseNoteStruct(dataNoteNode);
				if (noteStruct != null) source.addChild(noteStruct);
			}
		}

		// ---- 6. ABBR (abbreviation) ----
		GEDCOMNode abbrNode = GEDCOMHelper.findFirstChild(sourNode, "ABBR");
		if (abbrNode != null && abbrNode.getValue() != null) {
			String text = "Abbreviation: " + abbrNode.getValue();
			FLEFRecord note = structParser.createNoteStruct(text, sourNode);
			if (note != null) source.addChild(note);
		}

		// ---- 7. TEXT (verbatim text from source) with CONC/CONT ----
		GEDCOMNode textNode = GEDCOMHelper.findFirstChild(sourNode, "TEXT");
		if (textNode != null) {
			String fullText = GEDCOMHelper.extractFullText(textNode);
			if (fullText != null && !fullText.isBlank()) {
				FLEFRecord note = FLEFRecord.createChildWithTag("note");
				note.addChild(FLEFRecord.createChildWithTagAndValue("value", "Verbatim text: " + fullText));
				source.addChild(note);
			}
		}

		// ---- 8. REPO (repository citation) with CALN, MEDI, NOTE ----
		for (GEDCOMNode repoNode : GEDCOMHelper.findChildren(sourNode, "REPO")) {
			FLEFRecord repoCitation = structParser.parseRepositoryCitation(repoNode, repositoryMap);
			if (repoCitation == null) continue;

//			// 8a. CALN (call number) with optional MEDI
//			for (GEDCOMNode calnNode : GEDCOMHelper.findChildren(repoNode, "CALN")) {
//				if (calnNode.getValue() != null) {
//					FLEFRecord caln = FLEFRecord.createChildWithTagAndValue("_call_number", calnNode.getValue());
//					GEDCOMNode mediNode = GEDCOMHelper.findFirstChild(calnNode, "MEDI");
//					if (mediNode != null && mediNode.getValue() != null) {
//						caln.addChild(FLEFRecord.createChildWithTagAndValue("_media_type", mediNode.getValue()));
//					}
//					repoCitation.addChild(caln);
//				}
//			}

			// 8b. NOTE inside REPO
			for (GEDCOMNode repoNoteNode : GEDCOMHelper.findChildren(repoNode, "NOTE")) {
				FLEFRecord noteStruct = structParser.parseNoteStruct(repoNoteNode);
				if (noteStruct != null) repoCitation.addChild(noteStruct);
			}

			source.addChild(repoCitation);
		}

		// ---- 9. OBJE → DocumentRecord + document reference ----
		processObjNodes(sourNode, source);

//		// ---- 10. REFN (user reference number) with TYPE ----
//		for (GEDCOMNode refnNode : GEDCOMHelper.findChildren(sourNode, "REFN")) {
//			if (refnNode.getValue() != null) {
//				FLEFRecord refnChild = FLEFRecord.createChildWithTagAndValue("_refn", refnNode.getValue());
//				GEDCOMNode typeNode = GEDCOMHelper.findFirstChild(refnNode, "TYPE");
//				if (typeNode != null && typeNode.getValue() != null) {
//					refnChild.addChild(FLEFRecord.createChildWithTagAndValue("type", typeNode.getValue()));
//				}
//				source.addChild(refnChild);
//			}
//		}

//		// ---- 11. RIN (automated record ID) ----
//		GEDCOMNode rinNode = GEDCOMHelper.findFirstChild(sourNode, "RIN");
//		if (rinNode != null && rinNode.getValue() != null) {
//			source.addChild(FLEFRecord.createChildWithTagAndValue("_rin", rinNode.getValue()));
//		}

		// ---- 12. RESN (restriction notice) ----
		GEDCOMNode resnNode = GEDCOMHelper.findFirstChild(sourNode, "RESN");
		if (resnNode != null && resnNode.getValue() != null) {
			String level = GEDCOMMapper.mapPrivacyLevel(resnNode.getValue());
			FLEFRecord privacy = FLEFRecord.createChildWithTag("privacy");
			privacy.addChild(FLEFRecord.createChildWithTagAndValue("level", level));
			source.addChild(privacy);
		}

		// ---- 13. General NOTE structures ----
		for (GEDCOMNode noteNode : GEDCOMHelper.findChildren(sourNode, "NOTE")) {
			FLEFRecord noteStruct = structParser.parseNoteStruct(noteNode);
			if (noteStruct != null) source.addChild(noteStruct);
		}

		// ---- 14. CHANGE_DATE (audit) ----
		source.addChild(structParser.createAudit(sourNode));
	}

	// ------------------------------------------------------------------------
	// OBJE handling
	// ------------------------------------------------------------------------

	/**
	 * Processes all OBJE nodes under the source:
	 * - Creates a DocumentRecord for each OBJE.
	 * - Adds a document reference to the source.
	 */
	private void processObjNodes(GEDCOMNode sourNode, FLEFRecord source){
		List<GEDCOMNode> objNodes = GEDCOMHelper.findChildren(sourNode, "OBJE");
		for(GEDCOMNode objNode : objNodes){
			// Estrai l'Xref dal nodo (getXrefId() o dal valore)
			String objXref = objNode.getXrefId();
			if(objXref == null && objNode.getValue() != null){
				String val = objNode.getValue().trim();
				if(val.startsWith("@") && val.endsWith("@")){
					objXref = IDNormalizer.clean(val);
				}
			}

			FLEFRecord docRecord = null;
			if(objXref != null){
				// Cerca il DocumentRecord nella mappa
				docRecord = multimediaMap.get(objXref);
				if(docRecord == null){
					// Non esiste: crea un placeholder minimo per non perdere il riferimento
					docRecord = FLEFRecord.createChildWithTag("document");
					docRecord.setId(objXref);
					docRecord.addChild(FLEFRecord.createChildWithTagAndValue("file", "unknown"));
					docRecord.addChild(structParser.createAudit(objNode));
					multimediaMap.put(objXref, docRecord);
					model.addRecord(docRecord);
				}
			}
			else{
				// OBJE inline (con FILE) – crea un DocumentRecord dal nodo
				docRecord = createDocumentRecord(objNode);
				if(docRecord != null){
					model.addRecord(docRecord);
					multimediaMap.put(docRecord.getId(), docRecord);
				}
			}

			if(docRecord != null){
				// Aggiungi il riferimento document al SourceRecord
				FLEFRecord docRef = FLEFRecord.createChildWithTag("document");
				docRef.setValue(docRecord.getId());
				source.addChild(docRef);
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
		GEDCOMNode formNode = GEDCOMHelper.findFirstChild(objNode, "FORM");
		if (formNode != null && formNode.getValue() != null) {
			FLEFRecord note = FLEFRecord.createChildWithTag("note");
			note.addChild(FLEFRecord.createChildWithTagAndValue("value", "Format: " + formNode.getValue()));
			doc.addChild(note);
		}

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

}
