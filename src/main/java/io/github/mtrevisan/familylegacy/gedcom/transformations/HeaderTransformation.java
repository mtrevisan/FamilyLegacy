package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.addNode;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractNote;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractPlace;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.transferValues;


public class HeaderTransformation implements Transformation{

	private static final Collection<String> GEDCOM_ALLOWED_CHARSETS = new HashSet<>(Arrays.asList("ANSEL", "UTF-8", "UNICODE", "ASCII"));


	@Override
	public void to(final GedcomNode root){
		final GedcomNode header = moveTag("HEADER", root, "HEAD");
		final GedcomNode headerSource = moveTag("SOURCE", header, "SOUR");
		moveTag("VERSION", headerSource, "VERS");
		final GedcomNode headerCorporate = moveTag("CORPORATE", headerSource, "CORP");
		deleteTag(headerSource, "DATA");
		final GedcomNode sourceCorporatePlace = extractPlace(headerCorporate);
		sourceCorporatePlace.withID(Flef.getNextPlaceID(root.getChildrenWithTag("PLACE").size()));
		root.addChild(sourceCorporatePlace, 1);
		final GedcomNode sourceCorporatePlacePlaceholder = GedcomNode.create(3, "PLACE")
			.withID(sourceCorporatePlace.getID());
		addNode(sourceCorporatePlacePlaceholder, headerCorporate);
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
		moveTag("CHARSET", header, "CHAR");
		deleteTag(header, "CHARSET", "VERS");
		deleteTag(header, "LANG");
		deleteTag(header, "PLAC");
		final GedcomNode headerNote = extractNote(header)
			.withID(Flef.getNextNoteID(root.getChildrenWithTag("NOTE").size()));
		root.addChild(headerNote, 1);
		deleteTag(header, "NOTE");
		final GedcomNode headerNotePlaceholder = GedcomNode.create(1, "NOTE")
			.withID(headerNote.getID());
		addNode(headerNotePlaceholder, header);
	}

	@Override
	public void from(final GedcomNode root){
		final GedcomNode header = moveTag("HEAD", root, "HEADER");
		final GedcomNode headerSource = moveTag("SOUR", header, "SOURCE");
		moveTag("VERS", headerSource, "VERSION");
		final GedcomNode headerCorporate = moveTag("CORP", headerSource, "CORPORATE");
		final List<GedcomNode> headerCorporatePlaces = headerCorporate.getChildrenWithTag("PLACE");
		if(headerCorporatePlaces.size() == 1){
			final GedcomNode headerCorporatePlace = headerCorporatePlaces.get(0);
			final String headerCorporatePlaceID = headerCorporatePlace.getID();
			for(final GedcomNode child : root.getChildrenWithTag("PLACE"))
				if(headerCorporatePlaceID.equals(child.getID())){
					final GedcomNode addr = GedcomNode.create(3, "ADDR");
					List<GedcomNode> components = child.getChildrenWithTag("STREET");
					if(components.size() == 1)
						addr.withValue(components.get(0).getValue());
					components = child.getChildrenWithTag("CITY");
					if(components.size() == 1)
						addr.addChild(GedcomNode.create(4, "CITY")
							.withValue(components.get(0).getValue()));
					components = child.getChildrenWithTag("STATE");
					if(components.size() == 1)
						addr.addChild(GedcomNode.create(4, "STAE")
							.withValue(components.get(0).getValue()));
					components = child.getChildrenWithTag("POSTAL_CODE");
					if(components.size() == 1)
						addr.addChild(GedcomNode.create(4, "POST")
							.withValue(components.get(0).getValue()));
					components = child.getChildrenWithTag("COUNTRY");
					if(components.size() == 1)
						addr.addChild(GedcomNode.create(4, "CTRY")
							.withValue(components.get(0).getValue()));

					headerCorporate.removeChild(headerCorporatePlace);
					headerCorporate.addChild(addr);

					transferValues(child, "PHONE", headerCorporate, "PHON", 1);
					transferValues(child, "FAX", headerCorporate, "FAX", 1);
					transferValues(child, "EMAIL", headerCorporate, "EMAIL", 1);
					transferValues(child, "WWW", headerCorporate, "WWW", 1);

					root.removeChild(child);

					break;
				}
		}
		moveTag("SUBM", header, "SUBMITTER");
		moveTag("COPR", header, "COPYRIGHT");
		deleteTag(header, "PROTOCOL_VERSION");
		final GedcomNode gedcom = GedcomNode.create(1, "GEDC");
		gedcom.addChild(GedcomNode.create(2, "VERS")
			.withValue("5.5.1"));
		gedcom.addChild(GedcomNode.create(2, "FORM")
			.withValue("LINEAGE-LINKED"));
		addNode(gedcom, header);
		final GedcomNode headerCharset = moveTag("CHAR", header, "CHARSET");
		if(!GEDCOM_ALLOWED_CHARSETS.contains(headerCharset.getValue()))
			throw new IllegalArgumentException("Unallowed value for charset: " + headerCharset.getValue());
		final List<GedcomNode> headerNotes = header.getChildrenWithTag("NOTE");
		if(headerNotes.size() == 1){
			final GedcomNode headerNote = headerNotes.get(0);
			final String headerNoteID = headerNote.getID();
			for(final GedcomNode child : root.getChildrenWithTag("NOTE"))
				if(headerNoteID.equals(child.getID())){
					final GedcomNode addr = GedcomNode.create(1, "NOTE")
						.withValue(child.getValue());

					header.removeChild(headerNote);
					header.addChild(addr);
					root.removeChild(child);

					break;
				}
		}
	}

}
