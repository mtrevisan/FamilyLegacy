package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.*;


public class HeaderTransformation implements Transformation{

	@Override
	public void to(final GedcomNode root, final Flef flef){
		final GedcomNode header = moveTag("HEADER", root, "HEAD");
		final GedcomNode headerSource = moveTag("SOURCE", header, "SOUR");
		moveTag("VERSION", headerSource, "VERS");
		moveTag("CORPORATE", headerSource, "CORP");
		final GedcomNode sourceCorporatePlace = extractPlace(headerSource, "CORPORATE");
		flef.addPlace(sourceCorporatePlace);
		final GedcomNode sourceCorporatePlacePlaceholder = GedcomNode.create(3, "PLACE")
			.withID(sourceCorporatePlace.getID());
		addNode(sourceCorporatePlacePlaceholder, headerSource, "CORPORATE");
		deleteTag(header, "DEST");
		deleteTag(header, "DATE");
		moveTag("SUBMITTER", header, "SUBM");
		deleteTag(header, "SUBN");
		deleteTag(header, "FILE");
		moveTag("COPYRIGHT", header, "COPR");
		final GedcomNode protocolVersion = GedcomNode.create(1, "PROTOCOL_VERSION")
			.withValue("0.0.1");
		addNode(protocolVersion, header);
		deleteTag(header, "GEDC");
		transferValue(header, "CHAR", header, "CHARSET", 1);
		deleteTag(header, "LANG");
		deleteTag(header, "PLACE");
		mergeNote(header, "NOTE");
	}

	@Override
	public void from(final GedcomNode root){
		moveTag("HEAD", root, "HEADER");
//		final Map<String, Object> source = (Map<String, Object>)getStructure(root, "HEAD", "SOURCE");
//		final Map<String, Object> submitter = (Map<String, Object>)getStructure(root, "HEAD", "SUBMITTER");
//		final Map<String, Object> copyright = (Map<String, Object>)getStructure(root, "HEAD", "COPYRIGHT");
//		final Map<String, Object> protocolVersion = (Map<String, Object>)getStructure(root, "HEAD", "PROTOCOL_VERSION");
//		final Map<String, Object> charset = (Map<String, Object>)getStructure(root, "HEAD", "CHARSET");
//		final Map<String, Object> note = (Map<String, Object>)getStructure(root, "HEAD", "NOTE");

		//remove place
		deleteTag(root, "CHANGE");
	}

}
