package io.github.mtrevisan.familylegacy.v2.gedcom.converters;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.AuditBuilder;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDGenerator;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.StructureParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.Map;


public class NoteConverter{

	private final FLEFModel model;
	private final Map<String, FLEFRecord> noteMap;
	private final Map<String, GEDCOMNode> noteRawMap;
	private final StructureParser structParser;

	public NoteConverter(FLEFModel model, Map<String, FLEFRecord> noteMap, Map<String, GEDCOMNode> noteRawMap){
		this.model = model;
		this.noteMap = noteMap;
		this.structParser = new StructureParser(null);
		this.noteRawMap = noteRawMap;
	}

	/**
	 * Converts a GEDCOM NOTE record into a FLEF note record.
	 *
	 * @param noteNode The GEDCOM NOTE node.
	 */
	public void convert(GEDCOMNode noteNode){
		String xref = noteNode.getXrefId();

		String id;
		if(xref != null){
			String cleaned = GEDCOMHelper.cleanId(xref);
			if(isValidIdFormat(cleaned)){
				id = cleaned;
			}
			else{
				id = IDGenerator.nextId("N");
			}
		}
		else{
			id = IDGenerator.nextId("N");
		}

		IDGenerator.registerExistingId(id);

		FLEFRecord note = FLEFRecord.createMainRecord(id, "note")
			.addChild(AuditBuilder.build(noteNode));
		noteMap.put(id, note);

		// ---- 1. NOTE value (including CONC/CONT) ----
		// The parser might already concatenate the value, but we ensure it.
		String fullText = getFullNoteText(noteNode);
		if(fullText != null && !fullText.isBlank()){
			note.addChild(FLEFRecord.createChildWithTagAndValue("text", fullText));
		}

		// ---- 2. SOURCE_CITATION (SOUR) ----
		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(noteNode, "SOUR")){
			FLEFRecord citation = structParser.parseSourceCitation(sourNode, model, noteRawMap);
			if(citation != null){
				note.addChild(citation);
			}
		}

//		// ---- 3. REFN (user reference number) with optional TYPE ----
//		for(GEDCOMNode refnNode : GEDCOMHelper.findChildren(noteNode, "REFN")){
//			if(refnNode.getValue() != null){
//				FLEFRecord refnChild = FLEFRecord.createChildWithTagAndValue("_refn", refnNode.getValue());
//				GEDCOMNode typeNode = GEDCOMHelper.findFirstChild(refnNode, "TYPE");
//				if(typeNode != null && typeNode.getValue() != null){
//					refnChild.addChild(FLEFRecord.createChildWithTagAndValue("type", typeNode.getValue()));
//				}
//				note.addChild(refnChild);
//			}
//		}

//		// ---- 4. RIN (automated record ID) ----
//		GEDCOMNode rinNode = GEDCOMHelper.findFirstChild(noteNode, "RIN");
//		if(rinNode != null && rinNode.getValue() != null){
//			note.addChild(FLEFRecord.createChildWithTagAndValue("_rin", rinNode.getValue()));
//		}

		// ---- 5. CHANGE_DATE (audit) ----
		note.addChild(AuditBuilder.build(noteNode));
	}

	/**
	 * Reconstructs the full note text from a GEDCOM NOTE node.
	 * Handles CONC and CONT children to concatenate lines correctly.
	 *
	 * @param noteNode the GEDCOM NOTE node
	 * @return the full text, or null if no text found
	 */
	private String getFullNoteText(GEDCOMNode noteNode){
		if(noteNode.getValue() != null && !noteNode.getValue().isBlank()){
			return noteNode.getValue();
		}
		// If no initial value, try to build from CONC/CONT children.
		// In many parsers, these are already concatenated into the value.
		// This is a fallback.
		StringBuilder sb = new StringBuilder();
		for(GEDCOMNode child : noteNode.getChildren()){
			String tag = child.getTag();
			if("CONC".equals(tag) || "CONT".equals(tag)){
				if(child.getValue() != null){
					if("CONT".equals(tag) && sb.length() > 0){
						sb.append('\n');
					}
					sb.append(child.getValue());
				}
			}
		}
		return sb.length() > 0? sb.toString(): null;
	}

	/**
	 * Checks whether the id already matches the FLEF format.
	 *
	 * @param id The identifier.
	 * @return Whether the identifier is valid.
	 */
	private boolean isValidIdFormat(String id){
		return id != null && id.matches("^[A-Za-z][A-Za-z0-9]*$");
	}

}
