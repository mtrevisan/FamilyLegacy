package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringJoiner;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteMultipleTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.transferValues;


public class HeaderTransformation implements Transformation{

	private static final Collection<String> ALLOWED_CHARSETS = new HashSet<>(Arrays.asList("ANSEL", "UTF-8", "UNICODE", "ASCII"));
	private static final Collection<String> ADDRESS_TAGS = new HashSet<>(Arrays.asList("ADDR", "CONT", "ADR1", "ADR2", "ADR3"));


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		node.withTag("HEADER");
		final GedcomNode source = moveTag("SOURCE", node, "SOUR");
		moveTag("VERSION", source, "VERS");
		final GedcomNode corporate = moveTag("CORPORATE", source, "CORP");
		if(!corporate.isEmpty()){
			final GedcomNode corporatePlace = extractPlaceStructure(corporate);
			corporatePlace.withID(Flef.getNextPlaceID(root.getChildrenWithTag("PLACE").size()));
			root.addChild(corporatePlace, 1);
			corporate.addChild(GedcomNode.create("PLACE")
				.withID(corporatePlace.getID()));
		}
		deleteTag(source, "DATA");
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
		deleteMultipleTag(node, "LANG");
		deleteTag(node, "PLAC");
		final GedcomNode note = extractSubStructure(node, "NOTE");
		if(!note.isEmpty()){
			//create a note in the root:
			note.withID(Flef.getNextNoteID(root.getChildrenWithTag("NOTE").size()));
			root.addChild(GedcomNode.create("NOTE")
				.withID(note.getID())
				.withValue(note.extractValueConcatenated()));
			note.removeValue();
		}
	}

	private GedcomNode extractPlaceStructure(final GedcomNode context, final String... tags){
		final GedcomNode parentContext = extractSubStructure(context, tags);
		final GedcomNode placeContext = extractSubStructure(parentContext, "ADDR");

		GedcomNode place = GedcomNode.createEmpty();
		if(!placeContext.isEmpty()){
			final StringJoiner street = new StringJoiner(" - ");
			String value = placeContext.getValue();
			final Iterator<GedcomNode> itr = placeContext.getChildren().iterator();
			while(itr.hasNext()){
				final GedcomNode child = itr.next();
				if(ADDRESS_TAGS.contains(child.getTag())){
					final String component = child.getValue();
					if(component != null && ! component.isEmpty())
						street.add(component);

					itr.remove();
				}
			}
			if(street.length() > 0)
				value = street.toString();

			place = GedcomNode.create("PLACE");
			if(value != null && !value.isEmpty())
				place.addChild(GedcomNode.create("STREET")
					.withValue(value));
			transferValues(placeContext, "CITY", place, "CITY");
			transferValues(placeContext, "STAE", place, "STATE");
			transferValues(placeContext, "POST", place, "POSTAL_CODE");
			transferValues(placeContext, "CTRY", place, "COUNTRY");

			transferValues(parentContext, "PHON", place, "PHONE");
			transferValues(parentContext, "FAX", place, "FAX");
			transferValues(parentContext, "EMAIL", place, "EMAIL");
			transferValues(parentContext, "WWW", place, "WWW");

			parentContext.removeChild(placeContext);
		}
		return place;
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
		if(!ALLOWED_CHARSETS.contains(headerCharset.getValue()))
			throw new IllegalArgumentException("Unallowed value for charset: " + headerCharset.getValue());
		final GedcomNode headerNote = extractSubStructure(node, "NOTE");
		if(!headerNote.isEmpty()){
			final GedcomNode child = root.getChildWithIDAndTag(headerNote.getID(), "NOTE");
			if(child.isEmpty())
				throw new IllegalArgumentException("Cannot find NOTE with ID " + headerNote.getID());

			headerNote.removeID();
			headerNote.withValueConcatenated(child.getValue());
		}
	}

}
