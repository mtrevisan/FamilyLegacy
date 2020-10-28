package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.addNode;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractPlace;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.mergeNote;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.splitNote;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.transferValue;


public class IndividualTransformation implements Transformation{

	@Override
	public void to(final GedcomNode root){
		final GedcomNode person = moveTag("INDIVIDUAL", root, "INDI");
		moveTag("RESTRICTION", person, "RESN");
		moveTag("ALIAS", person, "ALIA");
		moveTag("SUBMITTER", person, "SUBM");
		deleteTag(person, "ANCI");
		deleteTag(person, "DESI");
		deleteTag(person, "RFN");
		deleteTag(person, "AFN");
		deleteTag(person, "REFN");
		deleteTag(person, "RIN");
		deleteTag(person, "BAPL");
		deleteTag(person, "CONL");
		deleteTag(person, "ENDL");
		deleteTag(person, "SLGC");
		Transformation nameTransformation = new NameTransformation();
		nameTransformation.to(extractSubStructure(person, "NAME"));

/*
		+1 <<PERSONAL_NAME_STRUCTURE>>    {0:M}			+1 <<NAME_STRUCTURE>>    {0:M}
		+1 <<CHILD_TO_FAMILY_LINK>>    {0:M}				+1 <<CHILD_TO_FAMILY_LINK>>    {0:M}
		+1 <<SPOUSE_TO_FAMILY_LINK>>    {0:M}				+1 <<SPOUSE_TO_FAMILY_LINK>>    {0:M}
		+1 <<ASSOCIATION_STRUCTURE>>    {0:M}				+1 <<INDIVIDUAL_ASSOCIATION_STRUCTURE>>    {0:M}
		+1 <<NOTE_STRUCTURE>>    {0:M}						+2 NOTE @<XREF:NOTE>@    {0:M}
		+1 <<INDIVIDUAL_EVENT_STRUCTURE>>    {0:M}		+1 <<INDIVIDUAL_EVENT_STRUCTURE>>    {0:M}
		+1 <<INDIVIDUAL_ATTRIBUTE_STRUCTURE>>    {0:M}	+1 <<INDIVIDUAL_ATTRIBUTE_STRUCTURE>>    {0:M}
		+1 <<SOURCE_CITATION>>    {0:M}						+1 SOURCE @<XREF:SOURCE>@    {0:M}
		+1 <<MULTIMEDIA_LINK>>    {0:M}						+1 DOCUMENT @<XREF:DOCUMENT>@    {0:M}
		+2 TYPE <USER_REFERENCE_TYPE>    {0:1}
		+1 <<CHANGE_DATE>>    {0:1}							+1 <<CHANGE_DATE>>    {0:1}
*/

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
		final GedcomNode person = moveTag("INDI", root, "INDIVIDUAL");
		deleteTag(person, "GENDER");
		deleteTag(person, "SEXUAL_ORIENTATION");



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
