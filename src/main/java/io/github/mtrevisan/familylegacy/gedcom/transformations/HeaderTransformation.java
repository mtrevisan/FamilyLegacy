package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
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
	public void from(final GedcomNode root, final Gedcom gedcom){
		final GedcomNode header = moveTag("HEAD", root, "HEADER");
		final GedcomNode headerSource = moveTag("SOUR", header, "SOURCE");
		moveTag("VERS", headerSource, "VERSION");
		final GedcomNode headerCorporate = moveTag("CORP", headerSource, "CORPORATE");
		final GedcomNode sourceCorporatePlace = extractSubStructure(header, "CORP");
		sourceCorporatePlace.setLevel(sourceCorporatePlace.getLevel() + 1);
		headerCorporate.addChild(sourceCorporatePlace);
		moveTag("SUBM", header, "SUBMITTER");
		moveTag("COPR", header, "COPYRIGHT");
		transferValue(header, "PROTOCOL_VERSION", header, "CHARSET", 1);
		final GedcomNode headerProtocolVersion = extractSubStructure(header, "PROTOCOL_VERSION");
		final GedcomNode headerGedcom = GedcomNode.create(1, "GEDC");
		final GedcomNode headerGedcomProtocolVersion = GedcomNode.create(2, "VERS");
		headerGedcomProtocolVersion.withValue(headerProtocolVersion.getValue());
		headerGedcom.addChild(headerGedcomProtocolVersion);
		addNode(headerGedcom, header);
		transferValue(header, "CHARSET", header, "CHAR", 1);
		splitNote(header, "NOTE");
	}

}
