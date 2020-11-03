package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class PlaceAddressStructureTransformation implements Transformation{

	private static final Transformation NOTE_STRUCTURE_TRANSFORMATION = new NoteStructureTransformation();

	private static final Collection<String> ADDRESS_TAGS = new HashSet<>(Arrays.asList("CONT", "ADR1", "ADR2", "ADR3"));


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		//place structure:
		GedcomNode mapNode = null;
		List<GedcomNode> noteNodes = null;
		final GedcomNode place = extractSubStructure(node, "PLAC");
		if(!place.isEmpty()){
			moveTag("_FORM", place, "FORM");
			final GedcomNode phoneticNode = extractSubStructure(place, "FONE");
			if(!phoneticNode.isEmpty()){
				phoneticNode.withTag("_FONE");
				moveTag("_TYPE", phoneticNode, "TYPE");
			}
			final GedcomNode romanizedNode = extractSubStructure(place, "ROMN");
			if(!romanizedNode.isEmpty()){
				romanizedNode.withTag("_ROMN");
				moveTag("_TYPE", romanizedNode, "TYPE");
			}
			mapNode = extractSubStructure(place, "MAP");
			if(!mapNode.isEmpty()){
				place.removeChild(mapNode);

				moveTag("LATITUDE", mapNode, "LATI");
				moveTag("LONGITUDE", mapNode, "LONG");
			}
			noteNodes = place.getChildrenWithTag("NOTE");
			for(final GedcomNode noteNode : noteNodes){
				place.removeChild(noteNode);

				NOTE_STRUCTURE_TRANSFORMATION.to(noteNode, root);
			}

			node.removeChild(place);
		}


		//address structure:
		final GedcomNode addr = extractSubStructure(node, "ADDR");
		if(!addr.isEmpty()){
			addr.withTag("ADDRESS");
			final StringJoiner address = new StringJoiner(" - ");
			final String addrConcatenated = addr.getValueConcatenated();
			if(addrConcatenated != null)
				address.add(addrConcatenated);
			else if(place.getValue() != null)
				address.add(place.getValue());
			final Iterator<GedcomNode> itr = addr.getChildren().iterator();
			while(itr.hasNext()){
				final GedcomNode child = itr.next();
				if(ADDRESS_TAGS.contains(child.getTag())){
					final String value = child.getValue();
					if(value != null)
						address.add(value);

					itr.remove();
				}
			}
			if(address.length() > 0)
				addr.withValue(address.toString());
			moveTag("STATE", addr, "STAE");
			moveTag("POSTAL_CODE", addr, "POST");
			moveTag("COUNTRY", addr, "CTRY");
			final List<GedcomNode> phones = node.getChildrenWithTag("PHON");
			for(final GedcomNode phone : phones){
				node.removeChild(phone);
				addr.addChild(phone.withTag("PHONE"));
			}
			final List<GedcomNode> faxes = node.getChildrenWithTag("FAX");
			for(final GedcomNode fax : faxes){
				node.removeChild(fax);
				addr.addChild(fax.withTag("PHONE")
					.addChild(GedcomNode.create("TYPE")
						.withValue("FAX")));
			}
			final List<GedcomNode> emails = node.getChildrenWithTag("EMAIL");
			for(final GedcomNode email : emails){
				node.removeChild(email);
				addr.addChild(email);
			}
			final List<GedcomNode> wwws = node.getChildrenWithTag("WWW");
			for(final GedcomNode www : wwws){
				node.removeChild(www);
				addr.addChild(www);
			}
			if(mapNode != null)
				addr.addChild(mapNode);
			if(noteNodes != null)
				addr.withChildren(noteNodes);
		}
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		//place structure:
		final GedcomNode addr = extractSubStructure(node, "ADDRESS");
		if(!addr.isEmpty()){
			moveTag("FORM", addr, "PLACE_NAME");
			final GedcomNode phoneticNode = extractSubStructure(addr, "_FONE");
			if(!phoneticNode.isEmpty()){
				phoneticNode.withTag("FONE");
				moveTag("TYPE", phoneticNode, "_TYPE");
			}
			final GedcomNode romanizedNode = extractSubStructure(addr, "_ROMN");
			if(!romanizedNode.isEmpty()){
				romanizedNode.withTag("ROMN");
				moveTag("TYPE", romanizedNode, "_TYPE");
			}
			final GedcomNode place = GedcomNode.create("PLAC");
			final GedcomNode mapNode = extractSubStructure(addr, "MAP");
			if(!mapNode.isEmpty()){
				moveTag("LATI", mapNode, "LATITUDE");
				moveTag("LONG", mapNode, "LONGITUDE");
				place.addChild(mapNode);
				addr.removeChild(mapNode);
			}
			final List<GedcomNode> notes = addr.getChildrenWithTag("NOTE");
			final Iterator<GedcomNode> itr = notes.iterator();
			while(itr.hasNext()){
				final GedcomNode note = itr.next();
				NOTE_STRUCTURE_TRANSFORMATION.from(note, root);
				place.addChild(note);
				itr.remove();
			}
			if(place.hasChildren())
				node.addChild(place);
		}


		//address structure:
		addr.withTag("ADDR");
		addr.withValueConcatenated(addr.getValue());
		moveTag("_LOCALE", addr, "LOCALE");
		moveTag("_COMPANY", addr, "COMPANY");
		moveTag("_APARTMENT", addr, "APARTMENT");
		moveTag("_NUMBER", addr, "NUMBER");
		final GedcomNode street = extractSubStructure(addr, "STREET");
		if(!street.isEmpty()){
			street.withTag("_STREET");
			moveTag("_TYPE", street, "TYPE");
		}
		moveTag("_DISTRICT", addr, "DISTRICT");
		moveTag("_TOWN", addr, "TOWN");
		moveTag("_CITY", addr, "CITY");
		moveTag("_COUNTY", addr, "COUNTY");
		moveTag("STAE", addr, "STATE");
		moveTag("POST", addr, "POSTAL_CODE");
		moveTag("CTRY", addr, "COUNTRY");
		final GedcomNode phon = extractSubStructure(addr, "PHONE");
		if(!phon.isEmpty()){
			addr.removeChild(phon);
			moveTag("_TYPE", phon, "TYPE");
			node.addChild(phon.withTag("PHON"));
		}
		final GedcomNode email = extractSubStructure(addr, "EMAIL");
		if(!email.isEmpty()){
			addr.removeChild(email);
			moveTag("_TYPE", email, "TYPE");
			node.addChild(email);
		}
		final GedcomNode www = extractSubStructure(addr, "WWW");
		if(!www.isEmpty()){
			addr.removeChild(www);
			moveTag("_TYPE", www, "TYPE");
			node.addChild(www);
		}
		moveTag("_NOTE", addr, "NOTE");
		moveTag("_SUBMITTER", addr, "SUBMITTER");
		moveTag("_RESTRICTION", addr, "RESTRICTION");
		final GedcomNode change = extractSubStructure(addr, "CHANGE");
		if(!change.isEmpty()){
			change.withTag("_CHANGE");
			final GedcomNode changeDate = extractSubStructure(change, "DATE");
			if(!changeDate.isEmpty()){
				changeDate.withTag("_DATE");
				moveTag("_TIME", changeDate, "TIME");
			}
		}
	}

}
