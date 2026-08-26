package io.github.mtrevisan.familylegacy.v2.gedcom.converters;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDGenerator;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDNormalizer;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.PlaceCache;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.StructureParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Converts GEDCOM SOUR records to FLEF SourceRecord.
 * <p>
 * Handles:
 * <ul>
 *   <li>Title (TITL) → title+ NameStructure</li>
 *   <li>Author (AUTH) → author</li>
 *   <li>Publisher (PUBL) → publisher</li>
 *   <li>Date (DATE) → date</li>
 *   <li>_DATE (extension) → date (if no DATE present)</li>
 *   <li>Place (DATA/PLAC) → place: PlaceCitation</li>
 *   <li>Media type (MEDI) → media_type</li>
 *   <li>Repository (REPO) → repository citation</li>
 *   <li>OBJE → DocumentRecord + document reference</li>
 *   <li>Abbreviation (ABBR) → note (inline)</li>
 *   <li>Verbatim text (TEXT) → note (inline)</li>
 *   <li>Notes (NOTE) → inline note structs</li>
 *   <li>Extra fields (RIN, REFN) → inline note structs</li>
 *   <li>Privacy (RESN) → PrivacyStructure</li>
 *   <li>Audit (CHAN) → AuditStructure</li>
 * </ul>
 */
public class SourceConverter{

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
	 * @param multimediaMap map of document IDs to FLEF records (for OBJE → DocumentRecord)
	 * @param placeCache    cache for place records
	 */
	public SourceConverter(FLEFModel model,
		Map<String, FLEFRecord> sourceMap,
		Map<String, FLEFRecord> repositoryMap,
		Map<String, FLEFRecord> multimediaMap,
		PlaceCache placeCache){
		this.model = model;
		this.sourceMap = sourceMap;
		this.repositoryMap = repositoryMap;
		this.multimediaMap = multimediaMap;
		this.structParser = new StructureParser(placeCache);
	}

	/**
	 * Converts a GEDCOM SOUR node into an FLEF SourceRecord.
	 *
	 * @param sourNode the GEDCOM node with the tag "SOUR"
	 */
	public void convert(GEDCOMNode sourNode){
		String xref = sourNode.getXrefId();
		if(xref == null) return;

		String cleanId = IDNormalizer.clean(xref);
		IDGenerator.registerExistingId(cleanId);

		FLEFRecord source = FLEFRecord.createChildWithTag("source");
		source.setId(cleanId);
		sourceMap.put(cleanId, source);

		// ---- Title (TITL) ----
		for(GEDCOMNode titlNode : structParser.findChildren(sourNode, "TITL")){
			FLEFRecord titleRec = structParser.parseNameStructure(titlNode, "title");
			if(titleRec != null) source.addChild(titleRec);
		}

		// ---- Author (AUTH) ----
		GEDCOMNode authNode = structParser.findFirstChild(sourNode, "AUTH");
		if(authNode != null && authNode.getValue() != null){
			source.addChild(FLEFRecord.createChildWithTagAndValue("author", authNode.getValue()));
		}

		// ---- Publisher (PUBL) ----
		GEDCOMNode publNode = structParser.findFirstChild(sourNode, "PUBL");
		if(publNode != null && publNode.getValue() != null){
			source.addChild(FLEFRecord.createChildWithTagAndValue("publisher", publNode.getValue()));
		}

		// ---- Date (GEDCOM DATE) ----
		GEDCOMNode dateNode = structParser.findFirstChild(sourNode, "DATE");
		if(dateNode != null){
			FLEFRecord dateStruct = structParser.parseDateStructure(dateNode);
			if(dateStruct != null) source.addChild(dateStruct);
		}
		else{
			// ---- _DATE (extension) – promote to source date if no DATE ----
			for(GEDCOMNode objNode : structParser.findChildren(sourNode, "OBJE")){
				GEDCOMNode extDateNode = structParser.findFirstChild(objNode, "_DATE");
				if(extDateNode != null && extDateNode.getValue() != null){
					GEDCOMNode syntheticDate = new GEDCOMNode(extDateNode.getLevel(), "DATE", extDateNode.getValue());
					FLEFRecord dateStruct = structParser.parseDateStructure(syntheticDate);
					if(dateStruct != null){
						source.addChild(dateStruct);
						break; // use only the first _DATE found
					}
				}
			}
		}

		// ---- Place (DATA/PLAC) ----
		GEDCOMNode dataNode = structParser.findFirstChild(sourNode, "DATA");
		if(dataNode != null){
			GEDCOMNode placNode = structParser.findFirstChild(dataNode, "PLAC");
			if(placNode != null){
				FLEFRecord placeCitation = structParser.parsePlaceCitation(placNode);
				if(placeCitation != null) source.addChild(placeCitation);
			}
		}

		// ---- Media type (MEDI) ----
		GEDCOMNode mediaNode = structParser.findFirstChild(sourNode, "MEDI");
		if(mediaNode != null && mediaNode.getValue() != null){
			String media = GEDCOMMapper.mapMediaType(mediaNode.getValue());
			if(media != null){
				source.addChild(FLEFRecord.createChildWithTagAndValue("media_type", media));
			}
		}

		// ---- Repository (REPO) ----
		for(GEDCOMNode repoNode : structParser.findChildren(sourNode, "REPO")){
			FLEFRecord repoCitation = structParser.parseRepositoryCitation(repoNode, repositoryMap);
			if(repoCitation != null) source.addChild(repoCitation);
		}

		// ---- OBJE → DocumentRecord + document reference ----
		processObjNodes(sourNode, source);

		// ---- Abbreviation (ABBR) → inline note ----
		GEDCOMNode abbrNode = structParser.findFirstChild(sourNode, "ABBR");
		if(abbrNode != null && abbrNode.getValue() != null){
			String text = "Abbreviation: " + abbrNode.getValue();
			FLEFRecord note = structParser.createNoteStruct(text, sourNode);
			if(note != null) source.addChild(note);
		}

// ---- Verbatim text (TEXT) → inline note ----
		GEDCOMNode textNode = structParser.findFirstChild(sourNode, "TEXT");
		if(textNode != null && textNode.getValue() != null){
			String text = "Verbatim text: " + textNode.getValue();
			FLEFRecord note = structParser.createNoteStruct(text, sourNode);
			if(note != null) source.addChild(note);
		}

		// ---- Notes (GEDCOM NOTE) – inline structs ----
		for(GEDCOMNode noteNode : structParser.findChildren(sourNode, "NOTE")){
			FLEFRecord noteStruct = structParser.parseNoteStruct(noteNode);
			if(noteStruct != null) source.addChild(noteStruct);
		}

		// ---- Extra fields (RIN, REFN) as inline notes ----
		for(GEDCOMNode child : sourNode.getChildren()){
			String tag = child.getTag();
			if(/*tag.equals("RIN") ||*/ tag.equals("REFN")){
				if(child.getValue() != null){
					String text = tag + ": " + child.getValue();
					FLEFRecord note = structParser.createNoteStruct(text, child);
					if(note != null) source.addChild(note);
				}
			}
		}

		// ---- Privacy (RESN) ----
		GEDCOMNode resnNode = structParser.findFirstChild(sourNode, "RESN");
		if(resnNode != null && resnNode.getValue() != null){
			String level = GEDCOMMapper.mapPrivacyLevel(resnNode.getValue());
			FLEFRecord privacy = FLEFRecord.createChildWithTag("privacy");
			privacy.addChild(FLEFRecord.createChildWithTagAndValue("level", level));
			source.addChild(privacy);
		}

		// ---- Audit (required) ----
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
		List<GEDCOMNode> objNodes = structParser.findChildren(sourNode, "OBJE");
		for(GEDCOMNode objNode : objNodes){
			// 1. Get or create DocumentRecord
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

			// 2. Add document reference to the source
			FLEFRecord docRef = FLEFRecord.createChildWithTag("document");
			docRef.setValue(docRecord.getId());
			source.addChild(docRef);
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
		GEDCOMNode fileNode = structParser.findFirstChild(objNode, "FILE");
		if(fileNode != null && fileNode.getValue() != null){
			doc.addChild(FLEFRecord.createChildWithTagAndValue("file", fileNode.getValue()));
		}

		// TITL -> description
		GEDCOMNode titlNode = structParser.findFirstChild(objNode, "TITL");
		if(titlNode != null && titlNode.getValue() != null){
			doc.addChild(FLEFRecord.createChildWithTagAndValue("description", titlNode.getValue()));
		}

//		// FORM -> note (inline)
//		GEDCOMNode formNode = structParser.findFirstChild(objNode, "FORM");
//		if(formNode != null && formNode.getValue() != null){
//			FLEFRecord note = FLEFRecord.createChildWithTag("note");
//			note.addChild(FLEFRecord.createChildWithTagAndValue("value", "Format: " + formNode.getValue()));
//			doc.addChild(note);
//		}

		// Exclude tags that are already used for specific purposes
		Set<String> excludedTags = Set.of("_PRIMARY", "_CUTD", "_PUBL", "_CUT", "_PREF", "_DATE");
		for(GEDCOMNode child : objNode.getChildren()){
			String tag = child.getTag();
			if(tag.startsWith("_") && child.getValue() != null && !excludedTags.contains(tag)){
				FLEFRecord note = FLEFRecord.createChildWithTag("note");
				note.addChild(FLEFRecord.createChildWithTagAndValue("value", tag + ": " + child.getValue()));
				doc.addChild(note);
			}
		}

		// Audit
		doc.addChild(structParser.createAudit(objNode));
		return doc;
	}

}
