package io.github.mtrevisan.familylegacy.v2.gedcom.converters;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.AuditBuilder;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDGenerator;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.PlaceCache;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.StructureParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.DocumentHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;


/**
 * Converts a GEDCOM MULTIMEDIA_RECORD (OBJE) into an FLEF DocumentRecord.
 * Handles all fields defined in the GEDCOM 5.5.1 specification.
 */
public class MultimediaConverter{

	private final FLEFModel model;
	private final Map<String, FLEFRecord> multimediaMap;
	private final Map<String, GEDCOMNode> noteRawMap;
	private final StructureParser structParser;

	/**
	 * Constructor.
	 *
	 * @param model         the FLEF model
	 * @param multimediaMap map of document IDs to FLEF records
	 * @param placeCache    cache for place records (may be null if not needed)
	 */
	public MultimediaConverter(FLEFModel model, Map<String, FLEFRecord> multimediaMap, PlaceCache placeCache, Map<String, GEDCOMNode> noteRawMap){
		this.model = model;
		this.multimediaMap = multimediaMap;
		this.structParser = new StructureParser(placeCache);
		this.noteRawMap = noteRawMap;
	}

	/**
	 * Converts a top-level GEDCOM OBJE node into an FLEF DocumentRecord.
	 *
	 * @param objNode the GEDCOM node with tag "OBJE"
	 */
	public void convert(GEDCOMNode objNode){
		if(objNode == null) return;

		// Extract Xref – either from getXrefId() or from the value if it's a reference
		String objXref = objNode.getXrefId();
		if(objXref == null && objNode.getValue() != null){
			String val = objNode.getValue().trim();
			if(val.startsWith("@") && val.endsWith("@")){
				objXref = GEDCOMHelper.cleanId(val);
			}
		}

		// ---- 2. Determine if this is a reference (no children) or a full record ----
		boolean isReference = (objXref != null && objNode.getChildren().isEmpty()
			&& (objNode.getValue() != null && objNode.getValue().startsWith("@")));

		if(isReference){
			// This is a pointer to an OBJE record. Do NOT create a new DocumentRecord.
			// If the referenced record does not exist yet, we create a minimal placeholder
			// so that the reference can be resolved later.
			if(!multimediaMap.containsKey(objXref)){
				FLEFRecord doc = FLEFRecord.createMainRecord(objXref, DocumentHandler.TYPE)
					// Add a placeholder file to satisfy the protocol requirement.
					// This will be replaced when the actual OBJE record is converted.
					.addChild(FLEFRecord.createChildWithTagAndValue("uri", "unknown"))
					.addChild(AuditBuilder.build(objNode));
				multimediaMap.put(objXref, doc);
				model.addRecord(doc);
			}
			return;
		}

		// ---- 3. Determine ID ----
		String id;
		if(objXref != null && isValidIdFormat(objXref)){
			id = objXref;
		}
		else{
			id = IDGenerator.nextId(DocumentHandler.ID_PREFIX);
		}

		// If the ID is already in the map, skip (already converted)
		FLEFRecord doc;
		if(multimediaMap.containsKey(id)){
			// Record già esistente (ad esempio un placeholder) – lo sovrascriviamo
			doc = multimediaMap.get(id);
			// Pulisce i vecchi figli per evitare duplicati
			doc.getChildren().clear();
			// L'ID è già impostato
		}
		else{
			doc = FLEFRecord.createMainRecord(id, DocumentHandler.TYPE);
			multimediaMap.put(id, doc);
		}

		// ---- 4. Create DocumentRecord ----
		doc.setId(id);
		multimediaMap.put(id, doc);

		// ---- 1. TITL directly under OBJE ----
		GEDCOMNode objTitlNode = GEDCOMHelper.findFirstChild(objNode, "TITL");
		if(objTitlNode != null){
			String fullTitle = GEDCOMHelper.extractFullText(objTitlNode);
			if(StringUtils.isNotEmpty(fullTitle)){
				doc.addChild(FLEFRecord.createChildWithTagAndValue("description", fullTitle));
			}
		}

		// ---- 5. FILE (1:M) – take the first occurrence ----
		List<GEDCOMNode> fileNodes = GEDCOMHelper.findChildren(objNode, "FILE");
		if(!fileNodes.isEmpty()){
			GEDCOMNode fileNode = fileNodes.get(0);
			String fileVal = (fileNode.getValue() != null)? fileNode.getValue().trim(): "";
			if(!fileVal.isEmpty()){
				doc.addChild(FLEFRecord.createChildWithTagAndValue("uri", fileVal));
			}
			else{
				// File node exists but has no value – add placeholder
				doc.addChild(FLEFRecord.createChildWithTagAndValue("uri", "unknown"));
			}

			// ---- 5a. FILE/FORM ----
			GEDCOMNode formNode = GEDCOMHelper.findFirstChild(fileNode, "FORM");
			if(formNode != null && formNode.getValue() != null){
				String format = formNode.getValue().trim();
				if(StringUtils.isNotEmpty(format)){
					FLEFRecord note = FLEFRecord.createChildWithTag("note")
						.addChild(FLEFRecord.createChildWithTagAndValue("text", "Format: " + format))
						.addChild(AuditBuilder.build(objNode));
					doc.addChild(note);
				}
				// ---- 5b. FILE/FORM/TYPE ----
				GEDCOMNode typeNode = GEDCOMHelper.findFirstChild(formNode, "TYPE");
				if(typeNode != null && typeNode.getValue() != null){
					FLEFRecord note = FLEFRecord.createChildWithTag("note")
						.addChild(FLEFRecord.createChildWithTagAndValue("text", "Media type: " + typeNode.getValue()))
						.addChild(AuditBuilder.build(objNode));
					doc.addChild(note);
				}
			}

			// FILE/TITL (optional)
			GEDCOMNode fileTitlNode = GEDCOMHelper.findFirstChild(fileNode, "TITL");
			if(fileTitlNode != null){
				String fileTitle = GEDCOMHelper.extractFullText(fileTitlNode);
				if(StringUtils.isNotEmpty(fileTitle)){
					// If we already have a description from OBJE/TITL, add as note; else use as description.
					if(FLEFRecordHelper.findChild(doc, "description") != null){
						FLEFRecord note = FLEFRecord.createChildWithTag("note")
							.addChild(FLEFRecord.createChildWithTagAndValue("text", "File title: " + fileTitle))
							.addChild(AuditBuilder.build(objNode));
						doc.addChild(note);
					}
					else{
						doc.addChild(FLEFRecord.createChildWithTagAndValue("description", fileTitle));
					}
				}
			}
		}
		else{
			// No FILE child – this should not happen for a valid OBJE record,
			// but we add a placeholder to satisfy the protocol.
			doc.addChild(FLEFRecord.createChildWithTagAndValue("uri", "unknown"));
		}

//		// ---- 2. REFN (0:M) – user reference number with optional TYPE ----
//		for(GEDCOMNode refnNode : GEDCOMHelper.findChildren(objNode, "REFN")){
//			if(refnNode.getValue() != null){
//				FLEFRecord refnChild = FLEFRecord.createChildWithTagAndValue("_refn", refnNode.getValue());
//				GEDCOMNode typeNode = GEDCOMHelper.findFirstChild(refnNode, "TYPE");
//				if(typeNode != null && typeNode.getValue() != null){
//					refnChild.addChild(FLEFRecord.createChildWithTagAndValue("type", typeNode.getValue()));
//				}
//				doc.addChild(refnChild);
//			}
//		}

//		// ---- 3. RIN (0:1) – automated record ID ----
//		GEDCOMNode rinNode = GEDCOMHelper.findFirstChild(objNode, "RIN");
//		if(rinNode != null && rinNode.getValue() != null){
//			doc.addChild(FLEFRecord.createChildWithTagAndValue("_rin", rinNode.getValue()));
//		}

		// ---- 4. NOTE_STRUCTURE (0:M) – notes (inline or via xref) ----
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(objNode, "NOTE")){
			FLEFRecord noteStruct = structParser.parseNoteStruct(noteNode);
			if(noteStruct != null){
				doc.addChild(noteStruct);
			}
		}

		// ---- 5. SOURCE_CITATION (0:M) – source citations ----
		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(objNode, "SOUR")){
			FLEFRecord sourCitation = structParser.parseSourceCitation(sourNode, model, noteRawMap);
			if(sourCitation != null){
				doc.addChild(sourCitation);
			}
		}

//		// ---- 6. Custom extension tags (_XXX) – stored as inline notes ----
//		Set<String> excludedTags = Set.of("_PRIMARY", "_CUTD", "_PUBL", "_CUT", "_PREF", "_DATE");
//		for(GEDCOMNode child : objNode.getChildren()){
//			String tag = child.getTag();
//			if(tag.startsWith("_") && child.getValue() != null && !excludedTags.contains(tag)){
//				String text = tag + ": " + child.getValue();
//				FLEFRecord note = structParser.createNoteStruct(text, child);
//				if(note != null){
//					doc.addChild(note);
//				}
//			}
//		}

		// ---- 7. CHANGE_DATE (audit) ----
		doc.addChild(AuditBuilder.build(objNode));

		// ---- 8. Add to model ----
		model.addRecord(doc);
	}

	/**
	 * Checks if the given string is a valid FLEF ID format: letters followed by digits.
	 *
	 * @param id the ID to check
	 * @return true if the format is valid
	 */
	private boolean isValidIdFormat(String id){
		return id != null && id.matches("^[A-Za-z][A-Za-z0-9]*$");
	}

}
