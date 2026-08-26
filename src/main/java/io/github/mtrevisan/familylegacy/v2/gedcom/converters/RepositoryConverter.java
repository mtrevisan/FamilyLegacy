package io.github.mtrevisan.familylegacy.v2.gedcom.converters;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDGenerator;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDNormalizer;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.PlaceCache;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.StructureParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.Map;
import java.util.Set;


/**
 * Converts GEDCOM REPO records to FLEF RepositoryRecord.
 * <p>
 * Handles:
 * <ul>
 *   <li>Name (NAME) → name+: ClassifiedNameStructure</li>
 *   <li>Place (PLAC) → place: PlaceCitation</li>
 *   <li>Address (ADDR) → contact: ContactStructure</li>
 *   <li>Date (DATE) → date</li>
 *   <li>_DATE (extension) → date (if no DATE present)</li>
 *   <li>OBJE → DocumentRecord + document reference</li>
 *   <li>Notes (NOTE) → inline NoteStructure (with audit)</li>
 *   <li>Extra fields (RIN, REFN) → inline NoteStructure (with audit)</li>
 *   <li>Privacy (RESN) → PrivacyStructure</li>
 *   <li>Audit (CHAN) → AuditStructure</li>
 * </ul>
 */
public class RepositoryConverter{

	private final FLEFModel model;
	private final Map<String, FLEFRecord> repositoryMap;
	private final Map<String, FLEFRecord> multimediaMap;
	private final StructureParser structParser;

	/**
	 * Constructor.
	 *
	 * @param model         the FLEF model
	 * @param repositoryMap map of repository IDs to FLEF records
	 * @param multimediaMap map of document IDs to FLEF records (for OBJE → DocumentRecord)
	 * @param placeCache    cache for place records
	 */
	public RepositoryConverter(FLEFModel model,
		Map<String, FLEFRecord> repositoryMap,
		Map<String, FLEFRecord> multimediaMap,
		PlaceCache placeCache){
		this.model = model;
		this.repositoryMap = repositoryMap;
		this.multimediaMap = multimediaMap;
		this.structParser = new StructureParser(placeCache);
	}

	/**
	 * Converts a GEDCOM REPO node into an FLEF RepositoryRecord.
	 *
	 * @param repoNode the GEDCOM node with the tag "REPO"
	 */
	public void convert(GEDCOMNode repoNode){
		String xref = repoNode.getXrefId();
		if(xref == null) return;

		String cleanId = IDNormalizer.clean(xref);
		IDGenerator.registerExistingId(cleanId);

		FLEFRecord repo = FLEFRecord.createChildWithTag("repository");
		repo.setId(cleanId);
		repositoryMap.put(cleanId, repo);

		// ---- 1. Name (name+: ClassifiedNameStructure) ----
		GEDCOMNode nameNode = structParser.findFirstChild(repoNode, "NAME");
		if(nameNode != null && nameNode.getValue() != null){
			FLEFRecord classifiedName = FLEFRecord.createChildWithTag("name");
			FLEFRecord nameStruct = structParser.parseNameStructure(nameNode, "text");
			if(nameStruct != null){
				classifiedName.addChild(nameStruct);
				// Optional type
				GEDCOMNode typeNode = structParser.findFirstChild(nameNode, "TYPE");
				if(typeNode != null && typeNode.getValue() != null){
					classifiedName.addChild(
						FLEFRecord.createChildWithTagAndValue("type", typeNode.getValue())
					);
				}
				repo.addChild(classifiedName);
			}
		}

		// ---- 2. Place (PLAC -> place: PlaceCitation) ----
		GEDCOMNode placNode = structParser.findFirstChild(repoNode, "PLAC");
		if(placNode != null){
			FLEFRecord placeCitation = structParser.parsePlaceCitation(placNode);
			if(placeCitation != null) repo.addChild(placeCitation);
		}

		// ---- 3. Address (ADDR -> contact: ContactStructure) ----
		GEDCOMNode addrNode = structParser.findFirstChild(repoNode, "ADDR");
		if(addrNode != null){
			FLEFRecord contact = structParser.parseAddressToContact(addrNode, repoNode);
			if(contact != null) repo.addChild(contact);
		}

		// ---- 4. Date (GEDCOM DATE) ----
		GEDCOMNode dateNode = structParser.findFirstChild(repoNode, "DATE");
		if(dateNode != null){
			FLEFRecord dateStruct = structParser.parseDateStructure(dateNode);
			if(dateStruct != null) repo.addChild(dateStruct);
		}
		else{
			// ---- 5. _DATE (extension) – promote to repository date if no DATE ----
			for(GEDCOMNode objNode : structParser.findChildren(repoNode, "OBJE")){
				GEDCOMNode extDateNode = structParser.findFirstChild(objNode, "_DATE");
				if(extDateNode != null && extDateNode.getValue() != null){
					GEDCOMNode syntheticDate = new GEDCOMNode(extDateNode.getLevel(), "DATE", extDateNode.getValue());
					FLEFRecord dateStruct = structParser.parseDateStructure(syntheticDate);
					if(dateStruct != null){
						repo.addChild(dateStruct);
						break; // use only the first _DATE found
					}
				}
			}
		}

		// ---- 6. Multimedia (OBJE) – create DocumentRecord and add reference ----
		for(GEDCOMNode objNode : structParser.findChildren(repoNode, "OBJE")){
			FLEFRecord docRecord = null;
			String objXref = objNode.getXrefId();
			if(objXref != null){
				String cleanObjId = IDNormalizer.clean(objXref);
				docRecord = multimediaMap.get(cleanObjId);
			}
			if(docRecord == null){
				docRecord = createDocumentRecord(objNode);
				model.addRecord(docRecord);
				multimediaMap.put(docRecord.getId(), docRecord);
			}
			FLEFRecord docRef = FLEFRecord.createChildWithTag("document");
			docRef.setValue(docRecord.getId());
			repo.addChild(docRef);
		}

		// ---- 7. Notes (GEDCOM NOTE) – inline NoteStructure with audit ----
		for(GEDCOMNode noteNode : structParser.findChildren(repoNode, "NOTE")){
			FLEFRecord noteStruct = structParser.parseNoteStruct(noteNode);
			if(noteStruct != null) repo.addChild(noteStruct);
		}

		// ---- 8. Extra fields (RIN, REFN) as inline notes with audit ----
		for(GEDCOMNode child : repoNode.getChildren()){
			String tag = child.getTag();
			if(/*tag.equals("RIN") ||*/ tag.equals("REFN")){
				if(child.getValue() != null){
					String text = tag + ": " + child.getValue();
					FLEFRecord note = structParser.createNoteStruct(text, child);
					if(note != null) repo.addChild(note);
				}
			}
		}

		// ---- 9. Privacy (RESN) ----
		GEDCOMNode resnNode = structParser.findFirstChild(repoNode, "RESN");
		if(resnNode != null && resnNode.getValue() != null){
			String level = GEDCOMMapper.mapPrivacyLevel(resnNode.getValue());
			FLEFRecord privacy = FLEFRecord.createChildWithTag("privacy");
			privacy.addChild(FLEFRecord.createChildWithTagAndValue("level", level));
			repo.addChild(privacy);
		}

		// ---- 10. Audit (required) ----
		repo.addChild(structParser.createAudit(repoNode));
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

		// Audit (required)
		doc.addChild(structParser.createAudit(objNode));
		return doc;
	}

}
