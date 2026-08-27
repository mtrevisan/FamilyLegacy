package io.github.mtrevisan.familylegacy.v2.gedcom.converters;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDGenerator;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDNormalizer;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.PlaceCache;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.StructureParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.Map;


/**
 * Converts GEDCOM SUBM (submitter) records into FLEF submitter structures.
 * <p>
 * Submitters are used in the header, not as top‑level records.
 * The resulting structure is stored in the submitterMap for later inclusion
 * in the Header by HeaderConverter.
 * <p>
 * Handles:
 * <ul>
 *   <li>Name (NAME) → ContactStructure.name</li>
 *   <li>Address (ADDR) → ContactStructure (if present)</li>
 *   <li>Language (LANG) → note (inline)</li>
 *   <li>Registered numbers (RFN, RIN) → notes (inline)</li>
 * </ul>
 */
public class SubmitterConverter{

	private final FLEFModel model;
	private final Map<String, FLEFRecord> submitterMap;
	private final StructureParser structParser;

	/**
	 * Constructor.
	 *
	 * @param model        the FLEF model
	 * @param submitterMap map of submitter IDs to FLEF submitter structures
	 * @param placeCache   cache for place records (used by StructureParser for addresses)
	 */
	public SubmitterConverter(FLEFModel model,
		Map<String, FLEFRecord> submitterMap,
		PlaceCache placeCache){
		this.model = model;
		this.submitterMap = submitterMap;
		this.structParser = new StructureParser(placeCache);
	}

	/**
	 * Converts a GEDCOM SUBM node into an FLEF submitter structure.
	 *
	 * @param subNode the GEDCOM node with the tag "SUBM"
	 */
	public void convert(GEDCOMNode subNode){
		String xref = subNode.getXrefId();
		if(xref == null) return;

		String cleanId = IDNormalizer.clean(xref);
		IDGenerator.registerExistingId(cleanId);

		// Create the submitter structure (this will be used in the header)
		FLEFRecord submitter = FLEFRecord.createChildWithTag("submitter");
		submitter.setId(cleanId);
		submitterMap.put(cleanId, submitter);

		// ---- Name (NAME) -> ContactStructure ----
		GEDCOMNode nameNode = GEDCOMHelper.findFirstChild(subNode, "NAME");
		if(nameNode != null && nameNode.getValue() != null){
			FLEFRecord contact = FLEFRecord.createChildWithTag("contact");
			// Name goes inside the contact structure
			FLEFRecord nameStruct = FLEFRecord.createChildWithTag("name");
			nameStruct.addChild(FLEFRecord.createChildWithTagAndValue("value", nameNode.getValue()));
			contact.addChild(nameStruct);
			submitter.addChild(contact);
		}

		// ---- Address (ADDR) -> ContactStructure ----
		GEDCOMNode addrNode = GEDCOMHelper.findFirstChild(subNode, "ADDR");
		if(addrNode != null){
			FLEFRecord contact = structParser.parseAddressToContact(addrNode, subNode);
			if(contact != null) submitter.addChild(contact);
		}

		// ---- Language (LANG) -> inline note ----
		GEDCOMNode langNode = GEDCOMHelper.findFirstChild(subNode, "LANG");
		if(langNode != null && langNode.getValue() != null){
			FLEFRecord note = structParser.createNoteStruct("Language: " + langNode.getValue(), subNode);
			if (note != null) submitter.addChild(note);
		}

		// ---- Extra fields (RFN, RIN) as inline notes ----
		for(GEDCOMNode child : subNode.getChildren()){
			String tag = child.getTag();
			if(tag.equals("RFN") /*|| tag.equals("RIN")*/){
				if(child.getValue() != null){
					FLEFRecord note = FLEFRecord.createChildWithTag("note");
					note.addChild(FLEFRecord.createChildWithTagAndValue("value", tag + ": " + child.getValue()));
					submitter.addChild(note);
				}
			}
		}

		// Note: submitter does NOT have an audit field in the FLEF grammar.
		// Audit is only for records, not for the submitter structure.
	}

}
