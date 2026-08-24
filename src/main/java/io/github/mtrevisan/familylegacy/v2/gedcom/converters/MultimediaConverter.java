package io.github.mtrevisan.familylegacy.v2.gedcom.converters;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDGenerator;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDNormalizer;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.PlaceCache;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.StructureParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.Map;
import java.util.Set;


/**
 * Converts top‑level GEDCOM OBJE records (multimedia) into FLEF DocumentRecord.
 * <p>
 * Handles:
 * <ul>
 *   <li>FILE → file</li>
 *   <li>TITL → description</li>
 *   <li>FORM → note (inline)</li>
 *   <li>Other underscore tags (_DATE, etc.) → notes (inline)</li>
 *   <li>Audit → AuditStructure</li>
 * </ul>
 * <p>
 * Notes are now inline structs (not global NoteRecord references).
 */
public class MultimediaConverter{

	private final FLEFModel model;
	private final Map<String, FLEFRecord> multimediaMap;
	private final StructureParser structParser;

	/**
	 * Constructor.
	 *
	 * @param model         the FLEF model
	 * @param multimediaMap map of document IDs to FLEF records
	 * @param placeCache    cache for place records (not used for OBJE, but required for StructureParser)
	 */
	public MultimediaConverter(FLEFModel model,
		Map<String, FLEFRecord> multimediaMap,
		PlaceCache placeCache){
		this.model = model;
		this.multimediaMap = multimediaMap;
		this.structParser = new StructureParser(placeCache);
	}

	/**
	 * Converts a top‑level GEDCOM OBJE node into an FLEF DocumentRecord.
	 *
	 * @param objNode the GEDCOM node with tag "OBJE"
	 */
	public void convert(GEDCOMNode objNode){
		String xref = objNode.getXrefId();
		String id;
		if(xref != null){
			String cleaned = IDNormalizer.clean(xref);
			if(isValidIdFormat(cleaned)){
				id = cleaned;
			}
			else{
				id = IDGenerator.nextId("D");
			}
		}
		else{
			id = IDGenerator.nextId("D");
		}
		IDGenerator.registerExistingId(id);

		FLEFRecord doc = FLEFRecord.createChildWithTag("document");
		doc.setId(id);
		multimediaMap.put(id, doc);

		// ---- FILE -> file ----
		GEDCOMNode fileNode = structParser.findFirstChild(objNode, "FILE");
		if(fileNode != null && fileNode.getValue() != null){
			doc.addChild(FLEFRecord.createChildWithTagAndValue("file", fileNode.getValue()));
		}

		// ---- TITL -> description ----
		GEDCOMNode titlNode = structParser.findFirstChild(objNode, "TITL");
		if(titlNode != null && titlNode.getValue() != null){
			doc.addChild(FLEFRecord.createChildWithTagAndValue("description", titlNode.getValue()));
		}

//		// FORM -> inline note with audit
//		GEDCOMNode formNode = structParser.findFirstChild(objNode, "FORM");
//		if(formNode != null && formNode.getValue() != null){
//			String text = "Format: " + formNode.getValue();
//			FLEFRecord note = structParser.createNoteStruct(text, objNode);
//			if(note != null) doc.addChild(note);
//		}

		// Exclude tags that are already used for specific purposes
		Set<String> excludedTags = Set.of("_PRIMARY", "_CUTD", "_PUBL", "_CUT", "_PREF", "_DATE");
		for(GEDCOMNode child : objNode.getChildren()){
			String tag = child.getTag();
			if(tag.startsWith("_") && child.getValue() != null && !excludedTags.contains(tag)){
				String text = tag + ": " + child.getValue();
				FLEFRecord note = structParser.createNoteStruct(text, child);
				if(note != null) doc.addChild(note);
			}
		}

		// ---- Audit ----
		doc.addChild(structParser.createAudit(objNode));

		// ---- Add to model ----
		model.addRecord(doc);
	}

	/**
	 * Checks if the ID matches the FLEF format: letters followed by digits.
	 */
	private boolean isValidIdFormat(String id){
		return id != null && id.matches("^[A-Za-z][A-Za-z0-9]*$");
	}

}
