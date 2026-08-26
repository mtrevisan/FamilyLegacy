package io.github.mtrevisan.familylegacy.v2.gedcom.converters;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDGenerator;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDNormalizer;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.StructureParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.Map;


/**
 * Converts GEDCOM NOTE records to FLEF NoteRecord.
 * <p>
 * Handles:
 * - NOTE text -> value
 * - SOUR -> source
 * - CHAN -> audit
 * - REFN/RIN -> inline notes
 */
public class NoteConverter{

	private final FLEFModel model;
	private final Map<String, FLEFRecord> noteMap;
	private final StructureParser structParser;

	public NoteConverter(FLEFModel model, Map<String, FLEFRecord> noteMap){
		this.model = model;
		this.noteMap = noteMap;
		this.structParser = new StructureParser(null);
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
			String cleaned = IDNormalizer.clean(xref);

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

		FLEFRecord note = FLEFRecord.createChildWithTag("note");
		note.setId(id);

		noteMap.put(id, note);

		// NOTE value (including CONC/CONT already reconstructed by parser)
		if(noteNode.getValue() != null && !noteNode.getValue().isBlank()){
			note.addChild(FLEFRecord.createChildWithTagAndValue(
				"value",
				noteNode.getValue()
			));
		}

		// SOUR
		for(GEDCOMNode sourNode : structParser.findChildren(noteNode, "SOUR")){
			FLEFRecord citation = structParser.parseSourceCitation(sourNode, model);
			if(citation != null){
				note.addChild(citation);
			}
		}

		// REFN
		for(GEDCOMNode refnNode : structParser.findChildren(noteNode, "REFN")){
			if(refnNode.getValue() != null){
				FLEFRecord extraNote = structParser.createNoteStruct(
					"REFN: " + refnNode.getValue(),
					refnNode
				);

				if(extraNote != null){
					note.addChild(extraNote);
				}
			}
		}

//		// RIN
//		GEDCOMNode rinNode = structParser.findFirstChild(noteNode, "RIN");
//		if(rinNode != null && rinNode.getValue() != null){
//			FLEFRecord extraNote = structParser.createNoteStruct(
//				"RIN: " + rinNode.getValue(),
//				rinNode
//			);
//
//			if(extraNote != null){
//				note.addChild(extraNote);
//			}
//		}

		// Audit
		note.addChild(structParser.createAudit(noteNode));
	}

	/**
	 * Checks whether the id already matches the FLEF format.
	 *
	 * @param id The identifier.
	 * @return Whether the identifier is valid.
	 */
	private boolean isValidIdFormat(String id){
		return (id != null
			&& id.matches("^[A-Za-z][A-Za-z0-9]*$"));
	}

}
