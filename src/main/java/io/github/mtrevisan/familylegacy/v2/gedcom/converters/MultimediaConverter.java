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


public class MultimediaConverter{

	private final FLEFModel model;
	private final Map<String, FLEFRecord> multimediaMap;
	private final StructureParser structParser;

	/**
	 * Constructor.
	 *
	 * @param model         the FLEF model
	 * @param multimediaMap map of document IDs to FLEF records
	 * @param placeCache    cache for place records
	 */
	public MultimediaConverter(FLEFModel model, Map<String, FLEFRecord> multimediaMap, PlaceCache placeCache){
		this.model = model;
		this.multimediaMap = multimediaMap;
		this.structParser = new StructureParser(placeCache);
	}

	/**
	 * Converts a top-level GEDCOM OBJE record into an FLEF DocumentRecord.
	 *
	 * @param objNode the GEDCOM node with the tag "OBJE"
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
		else{
			// ---- BLOB fallback -> file / raw_data ----
//			GEDCOMNode blobNode = structParser.findFirstChild(objNode, "BLOB");
//			if(blobNode != null){
//				String blobData = extractContinuationData(blobNode);
//				if(!blobData.isEmpty()){
//					doc.addChild(FLEFRecord.createChildWithTagAndValue("file", blobData));
//				}
//			}
			//deprecated
			multimediaMap.remove(id);
			return;
		}

		// ---- TITL -> description ----
		GEDCOMNode titlNode = structParser.findFirstChild(objNode, "TITL");
		if(titlNode != null && titlNode.getValue() != null){
			doc.addChild(FLEFRecord.createChildWithTagAndValue("description", titlNode.getValue()));
		}

		// ---- NOTE -> note ----
		for(GEDCOMNode child : objNode.getChildren()){
			if("NOTE".equals(child.getTag())){
				FLEFRecord note = structParser.createNoteStruct(extractContinuationData(child), child);
				if(note != null){
					doc.addChild(note);
				}
			}
		}

		// Exclude tags that are already used for specific purposes
		Set<String> excludedTags = Set.of("PRIMARY", "CUTD", "PUBL", "CUT", "PREF", "DATE");
		for(GEDCOMNode child : objNode.getChildren()){
			String tag = child.getTag();
			if(tag.startsWith("_") && child.getValue() != null && !excludedTags.contains(tag)){
				String text = tag + ": " + child.getValue();
				FLEFRecord note = structParser.createNoteStruct(text, child);
				if(note != null){
					doc.addChild(note);
				}
			}
		}

		// ---- Audit ----
		doc.addChild(structParser.createAudit(objNode));

		// ---- Add to model ----
		model.addRecord(doc);
	}

	/**
	 * Concatenates initial node value and subsequent CONT lines.
	 */
	private String extractContinuationData(GEDCOMNode node){
		StringBuilder builder = new StringBuilder();
		if(node.getValue() != null){
			builder.append(node.getValue());
		}
		for(GEDCOMNode child : node.getChildren()){
			if("CONT".equals(child.getTag()) && child.getValue() != null){
				builder.append(child.getValue());
			}
		}
		return builder.toString()
			.replace("\n", "");
	}

	private boolean isValidIdFormat(String id){
		return id != null && id.matches("^[A-Za-z][A-Za-z0-9]*$");
	}

}
