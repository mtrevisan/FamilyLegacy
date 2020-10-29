package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractPlaceStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
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
		final GedcomNode sourceCorporatePlace = extractPlaceStructure(headerCorporate);
		sourceCorporatePlace.withID(Flef.getNextPlaceID(root.getChildrenWithTag("PLACE").size()));
		root.addChild(sourceCorporatePlace, 1);
		headerCorporate.addChild(GedcomNode.create("PLACE")
			.withID(sourceCorporatePlace.getID()));
		deleteTag(header, "DEST");
		deleteTag(header, "DATE");
		moveTag("SUBMITTER", header, "SUBM");
		deleteTag(header, "SUBN");
		deleteTag(header, "FILE");
		moveTag("COPYRIGHT", header, "COPR");
		header.addChild(GedcomNode.create("PROTOCOL_VERSION")
			.withValue("0.0.1"));
		deleteTag(header, "GEDC");
		moveTag("CHARSET", header, "CHAR");
		deleteTag(header, "CHARSET", "VERS");
		deleteTag(header, "LANG");
		deleteTag(header, "PLAC");
		final GedcomNode noteContext = extractSubStructure(header, "NOTE");
		if(!noteContext.isEmpty()){
			final GedcomNode headerNote = GedcomNode.create("NOTE")
				.withID(Flef.getNextNoteID(root.getChildrenWithTag("NOTE").size()))
				.withValue(noteContext.getValueConcatenated());
			root.addChild(headerNote, 1);
			header.removeChild(noteContext);
			noteContext.clear();
			noteContext.withID(headerNote.getID());
			header.addChild(noteContext);
		}
	}

	@Override
	public void from(final GedcomNode root){
		final GedcomNode header = moveTag("HEAD", root, "HEADER");
		final GedcomNode headerSource = moveTag("SOUR", header, "SOURCE");
		moveTag("VERS", headerSource, "VERSION");
		final GedcomNode headerCorporate = moveTag("CORP", headerSource, "CORPORATE");
		final GedcomNode headerCorporatePlace = extractSubStructure(headerCorporate, "PLACE");
		if(!headerCorporatePlace.isEmpty()){
			final String headerCorporatePlaceID = headerCorporatePlace.getID();
			for(final GedcomNode child : root.getChildrenWithTag("PLACE"))
				if(headerCorporatePlaceID.equals(child.getID())){
					final GedcomNode addr = GedcomNode.create("ADDR");
					GedcomNode component = extractSubStructure(child, "STREET");
					if(!component.isEmpty())
						addr.withValue(component.getValue());
					component = extractSubStructure(child, "CITY");
					if(!component.isEmpty())
						addr.addChild(component);
					component = extractSubStructure(child, "STATE");
					if(!component.isEmpty())
						addr.addChild(component.withTag("STAE"));
					component = extractSubStructure(child, "POSTAL_CODE");
					if(!component.isEmpty())
						addr.addChild(component.withTag("POST"));
					component = extractSubStructure(child, "COUNTRY");
					if(!component.isEmpty())
						addr.addChild(component.withTag("CTRY"));

					headerCorporate.removeChild(headerCorporatePlace);
					headerCorporate.addChild(addr);

					transferValues(child, "PHONE", headerCorporate, "PHON");
					transferValues(child, "FAX", headerCorporate, "FAX");
					transferValues(child, "EMAIL", headerCorporate, "EMAIL");
					transferValues(child, "WWW", headerCorporate, "WWW");

					root.removeChild(child);

					break;
				}
		}
		moveTag("SUBM", header, "SUBMITTER");
		moveTag("COPR", header, "COPYRIGHT");
		deleteTag(header, "PROTOCOL_VERSION");
		header.addChild(GedcomNode.create("GEDC")
			.addChild(GedcomNode.create("VERS")
				.withValue("5.5.1"))
			.addChild(GedcomNode.create("FORM")
				.withValue("LINEAGE-LINKED")));
		final GedcomNode headerCharset = moveTag("CHAR", header, "CHARSET");
		if(!GEDCOM_ALLOWED_CHARSETS.contains(headerCharset.getValue()))
			throw new IllegalArgumentException("Unallowed value for charset: " + headerCharset.getValue());
		final GedcomNode headerNote = extractSubStructure(header, "NOTE");
		if(!headerNote.isEmpty()){
			final GedcomNode child = root.getChildWithIDAndTag(headerNote.getID(), "NOTE");
			if(!child.isEmpty()){
				header.removeChild(headerNote);
				header.addChild(GedcomNode.create("NOTE")
					.withValue(child.getValue()));
				root.removeChild(child);
			}
		}
	}

}
