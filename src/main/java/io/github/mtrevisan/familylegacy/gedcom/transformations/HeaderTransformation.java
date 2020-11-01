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
	public void to(final GedcomNode node, final GedcomNode root){
		node.withTag("HEADER");
		final GedcomNode headerSource = moveTag("SOURCE", node, "SOUR");
		moveTag("VERSION", headerSource, "VERS");
		final GedcomNode headerCorporate = moveTag("CORPORATE", headerSource, "CORP");
		deleteTag(headerSource, "DATA");
		final GedcomNode sourceCorporatePlace = extractPlaceStructure(headerCorporate);
		sourceCorporatePlace.withID(Flef.getNextPlaceID(root.getChildrenWithTag("PLACE").size()));
		root.addChild(sourceCorporatePlace, 1);
		headerCorporate.addChild(GedcomNode.create("PLACE")
			.withID(sourceCorporatePlace.getID()));
		deleteTag(node, "DEST");
		deleteTag(node, "DATE");
		moveTag("SUBMITTER", node, "SUBM");
		deleteTag(node, "SUBN");
		deleteTag(node, "FILE");
		moveTag("COPYRIGHT", node, "COPR");
		node.addChild(GedcomNode.create("PROTOCOL_VERSION")
			.withValue("0.0.1"));
		deleteTag(node, "GEDC");
		moveTag("CHARSET", node, "CHAR");
		deleteTag(node, "CHARSET", "VERS");
		deleteTag(node, "LANG");
		deleteTag(node, "PLAC");
		final GedcomNode noteContext = extractSubStructure(node, "NOTE");
		TransformationHelper.transferNote(noteContext, root);
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		node.withTag("HEAD");
		final GedcomNode headerSource = moveTag("SOUR", node, "SOURCE");
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
		moveTag("SUBM", node, "SUBMITTER");
		moveTag("COPR", node, "COPYRIGHT");
		deleteTag(node, "PROTOCOL_VERSION");
		node.addChild(GedcomNode.create("GEDC")
			.addChild(GedcomNode.create("VERS")
				.withValue("5.5.1"))
			.addChild(GedcomNode.create("FORM")
				.withValue("LINEAGE-LINKED")));
		final GedcomNode headerCharset = moveTag("CHAR", node, "CHARSET");
		if(!GEDCOM_ALLOWED_CHARSETS.contains(headerCharset.getValue()))
			throw new IllegalArgumentException("Unallowed value for charset: " + headerCharset.getValue());
		final GedcomNode headerNote = extractSubStructure(node, "NOTE");
		if(!headerNote.isEmpty()){
			final GedcomNode child = root.getChildWithIDAndTag(headerNote.getID(), "NOTE");
			if(!child.isEmpty()){
				headerNote.removeID();
				headerNote.withValueConcatenated(child.getValue());
			}
		}
	}

}
