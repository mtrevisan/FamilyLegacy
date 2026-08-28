package io.github.mtrevisan.familylegacy.v2.gedcom.converters;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.AuditBuilder;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDGenerator;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDNormalizer;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.PlaceCache;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;


/**
 * Converts a GEDCOM SOURCE_RECORD (SOUR) into an FLEF SourceRecord.
 * Handles all fields defined in the GEDCOM 5.5.1 specification.
 */
public class SourceConverter {

	private final FLEFModel model;
	private final Map<String, GEDCOMNode> noteRawMap;
	private final Map<String, FLEFRecord> sourceMap;
	private final Map<String, GEDCOMNode> objeRawMap;
	private final Map<String, FLEFRecord> multimediaMap;

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
		Map<String, GEDCOMNode> noteRawMap,
		Map<String, GEDCOMNode> sourRawMap,
		Map<String, FLEFRecord> sourceMap,
		Map<String, GEDCOMNode> objeRawMap,
		Map<String, FLEFRecord> repositoryMap,
		Map<String, FLEFRecord> multimediaMap,
		PlaceCache placeCache) {
		this.model = model;
		this.noteRawMap = noteRawMap;
		this.sourceMap = sourceMap;
		this.objeRawMap = objeRawMap;
		this.multimediaMap = multimediaMap;
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

		FLEFRecord source = FLEFRecord.createMainRecord(cleanId, SourceHandler.TYPE);
		sourceMap.put(cleanId, source);

		// ---- 1. TITL (title) with CONC/CONT ----
		for (GEDCOMNode titlNode : GEDCOMHelper.findChildren(sourNode, "TITL")) {
			String fullTitle = GEDCOMHelper.extractFullText(titlNode);
			if (fullTitle != null && !fullTitle.isBlank()) {
				FLEFRecord titleRec = FLEFRecord.createChildWithTag("title")
					.addChild(FLEFRecord.createChildWithTagAndValue("value", fullTitle));
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

//		// ---- 5. DATA block (EVEN, AGNC, PLAC, NOTE) ----
//		GEDCOMNode dataNode = GEDCOMHelper.findFirstChild(sourNode, "DATA");
//		if (dataNode != null) {
//			// 5a. DATA/EVEN (events recorded) with optional DATE and PLAC
//			for (GEDCOMNode evenNode : GEDCOMHelper.findChildren(dataNode, "EVEN")) {
//				StringBuilder sb = new StringBuilder("Events recorded: ");
//				if (evenNode.getValue() != null) {
//					sb.append(evenNode.getValue());
//				}
//				GEDCOMNode evenDate = GEDCOMHelper.findFirstChild(evenNode, "DATE");
//				if (evenDate != null && evenDate.getValue() != null) {
//					sb.append(" (Date: ").append(evenDate.getValue()).append(")");
//				}
//				GEDCOMNode evenPlac = GEDCOMHelper.findFirstChild(evenNode, "PLAC");
//				if (evenPlac != null && evenPlac.getValue() != null) {
//					sb.append(" (Place: ").append(evenPlac.getValue()).append(")");
//				}
//				FLEFRecord note = structParser.createNoteStruct(sb.toString(), evenNode);
//				if (note != null) source.addChild(note);
//			}
//
//			// 5b. DATA/AGNC (responsible agency)
//			GEDCOMNode agncNode = GEDCOMHelper.findFirstChild(dataNode, "AGNC");
//			if (agncNode != null && agncNode.getValue() != null) {
//				source.addChild(FLEFRecord.createChildWithTagAndValue("_agency", agncNode.getValue()));
//			}
//
//			// 5c. DATA/PLAC (source jurisdiction place)
//			GEDCOMNode placNode = GEDCOMHelper.findFirstChild(dataNode, "PLAC");
//			if (placNode != null) {
//				FLEFRecord placeCitation = structParser.parsePlaceCitation(placNode);
//				if (placeCitation != null) source.addChild(placeCitation);
//			}
//
//			// 5d. DATA/NOTE (notes inside DATA)
//			for (GEDCOMNode dataNoteNode : GEDCOMHelper.findChildren(dataNode, "NOTE")) {
//				FLEFRecord noteStruct = structParser.parseNoteStruct(dataNoteNode);
//				if (noteStruct != null) source.addChild(noteStruct);
//			}
//		}

//		// ---- 6. ABBR (abbreviation) ----
//		GEDCOMNode abbrNode = GEDCOMHelper.findFirstChild(sourNode, "ABBR");
//		if (abbrNode != null && abbrNode.getValue() != null) {
//			String text = "Abbreviation: " + abbrNode.getValue();
//			FLEFRecord note = structParser.createNoteStruct(text, sourNode);
//			if (note != null) source.addChild(note);
//		}

		// ---- 7. TEXT (verbatim text from source) with CONC/CONT ----
		GEDCOMNode textNode = GEDCOMHelper.findFirstChild(sourNode, "TEXT");
		if (textNode != null) {
			String fullText = GEDCOMHelper.extractFullText(textNode);
			if (fullText != null && !fullText.isBlank()) {
				FLEFRecord note = FLEFRecord.createChildWithTag("note")
					.addChild(FLEFRecord.createChildWithTagAndValue("text", "Verbatim text: " + fullText))
					.addChild(AuditBuilder.build(sourNode));
				source.addChild(note);
			}
		}

		// ---- 8. REPO (repository citation) with CALN, MEDI, NOTE ----
		for (GEDCOMNode repoNode : GEDCOMHelper.findChildren(sourNode, "REPO")) {
			FLEFRecord repositoryCitation = FLEFRecord.createChildWithTag("repository");

			String repoNodeValue = repoNode.getValue();
			if(StringUtils.isNotEmpty(repoNodeValue)){
				String repositoryId = GEDCOMHelper.cleanId(repoNodeValue);
				FLEFRecord repository = FLEFRecord.createMainRecord(repositoryId, "repository")
					.addChild(FLEFRecord.createChildWithTag("name")
						.addChild(FLEFRecord.createChildWithTagAndValue("value", repositoryId))
					)
					.addChild(AuditBuilder.build(sourNode));

				model.addRecord(repository);

				repositoryCitation.addChild(FLEFRecord.createChildWithTagAndValue("repository", repositoryId));
			}

			// 8a. CALN (call number) with optional MEDI
			for (GEDCOMNode calnNode : GEDCOMHelper.findChildren(repoNode, "CALN")) {
				if (calnNode.getValue() != null) {
					GEDCOMHelper.transferValue(repositoryCitation, "locator", calnNode);

					GEDCOMNode mediNode = GEDCOMHelper.findFirstChild(calnNode, "MEDI");
					if (mediNode != null && mediNode.getValue() != null) {
						GEDCOMHelper.attachNote(repositoryCitation,
							mediNode, noteRawMap);
					}
				}
			}

			// 8b. NOTE inside REPO
			for (GEDCOMNode noteNode : GEDCOMHelper.findChildren(repoNode, "NOTE")) {
				GEDCOMHelper.attachNote(repositoryCitation,
					noteNode, noteRawMap);
			}

			if(!repositoryCitation.isEmpty()){
				source.addChild(repositoryCitation);
			}
		}

		// ---- Privacy (RESN) ----
		GEDCOMHelper.attachRestriction(source, sourNode);

		// ---- Notes (GEDCOM NOTE) – inline structs ----
		for (GEDCOMNode noteNode : GEDCOMHelper.findChildren(sourNode, "NOTE")) {
			GEDCOMHelper.attachNote(source,
				noteNode, noteRawMap);
		}

		// Multimedia (OBJE)
		for (GEDCOMNode multimediaLinkNode : GEDCOMHelper.findChildren(sourNode, "OBJE")) {
			GEDCOMHelper.attachMultimediaLink(source, model,
				multimediaLinkNode, objeRawMap);
		}

		// ---- Audit ----
		source.addChild(AuditBuilder.build(sourNode));
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
					objXref = GEDCOMHelper.cleanId(val);
				}
			}

			FLEFRecord docRecord = null;
			if(objXref != null){
				// Cerca il DocumentRecord nella mappa
				docRecord = multimediaMap.get(objXref);
				if(docRecord == null){
					// Non esiste: crea un placeholder minimo per non perdere il riferimento
					docRecord = FLEFRecord.createMainRecord(objXref, DocumentHandler.TYPE)
						.addChild(FLEFRecord.createChildWithTagAndValue("uri", "unknown"))
						.addChild(AuditBuilder.build(sourNode));
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
				FLEFRecord docRef = FLEFRecord.createChildWithTagAndValue("document", docRecord.getId());
				source.addChild(docRef);
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
		GEDCOMNode formNode = GEDCOMHelper.findFirstChild(objNode, "FORM");
		if (formNode != null && formNode.getValue() != null) {
			FLEFRecord note = FLEFRecord.createChildWithTag("note")
				.addChild(FLEFRecord.createChildWithTagAndValue("text", "Format: " + formNode.getValue()))
				.addChild(AuditBuilder.build(objNode));
			doc.addChild(note);
		}

//		// Exclude tags that are already used for specific purposes
//		Set<String> excludedTags = Set.of("_PRIMARY", "_CUTD", "_PUBL", "_CUT", "_PREF", "_DATE");
//		for (GEDCOMNode child : objNode.getChildren()) {
//			String tag = child.getTag();
//			if (tag.startsWith("_") && child.getValue() != null && !excludedTags.contains(tag)) {
//				FLEFRecord note = FLEFRecord.createChildWithTag("note")
//					.addChild(FLEFRecord.createChildWithTagAndValue("text", tag + ": " + child.getValue()))
//					.addChild(AuditBuilder.build(child));
//				doc.addChild(note);
//			}
//		}

		// Audit
		doc.addChild(AuditBuilder.build(objNode));
		return doc;
	}

}
