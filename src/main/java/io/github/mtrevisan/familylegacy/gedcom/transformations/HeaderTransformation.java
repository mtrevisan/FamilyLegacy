package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.*;


public class HeaderTransformation implements Transformation{

	@Override
	public void to(final GedcomNode root){
		final GedcomNode header = moveTag("HEADER", root, "HEAD");
		final GedcomNode headerSource = moveTag("SOURCE", header, "SOUR");
		moveTag("VERSION", headerSource, "VERS");
		moveTag("CORPORATE", headerSource, "CORP");
		final GedcomNode sourceCorporatePlace = extractPlace(headerSource, "CORPORATE");
		sourceCorporatePlace.withID(Flef.getNextPlaceID(root.getChildrenWithTag("PLACE").size()));
		root.addChild(sourceCorporatePlace, 1);
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
		final GedcomNode header = moveTag("HEAD", root, "HEADER");
		final GedcomNode headerSource = moveTag("SOUR", header, "SOURCE");
		moveTag("VERS", headerSource, "VERSION");
		final GedcomNode headerCorporate = moveTag("CORP", headerSource, "CORPORATE");
		final GedcomNode sourceCorporatePlace = extractSubStructure(headerCorporate, "PLACE");
		if(!sourceCorporatePlace.isEmpty()){
			GedcomNode place = null;
			final List<GedcomNode> places = root.getChildrenWithTag("PLACE");
			for(final GedcomNode p : places)
				if(p.getID().equals(sourceCorporatePlace.getID())){
					place = p;
					break;
				}
			if(place == null)
				throw new IllegalArgumentException("Cannot find place with ID " + sourceCorporatePlace.getID());

			place.removeID();
			deleteTag(headerCorporate, "PLACE");
			headerCorporate.addChild(place);
		}
		moveTag("SUBM", header, "SUBMITTER");
		moveTag("COPR", header, "COPYRIGHT");
		deleteTag(header, "PROTOCOL_VERSION");
		final GedcomNode headerGedcom = GedcomNode.create(1, "GEDC");
		final GedcomNode headerGedcomProtocolVersion = GedcomNode.create(2, "VERS");
		headerGedcomProtocolVersion.withValue("5.5.1");
		headerGedcom.addChild(headerGedcomProtocolVersion);
		addNode(headerGedcom, header);
		transferValue(header, "CHARSET", header, "CHAR", 1);
		splitNote(header, "NOTE");
	}

}
