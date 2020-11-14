package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.StringJoiner;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


public class FamilyTransformation implements Transformation<Gedcom, Flef>{

	private static final Collection<String> ADDRESS_TAGS = new HashSet<>(Arrays.asList("CONT", "ADR1", "ADR2", "ADR3"));


	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> families = origin.getFamilies();
		for(final GedcomNode family : families)
			familyRecordTo(family, destination);
	}

	private void familyRecordTo(final GedcomNode family, final Flef destination){
		final GedcomNode destinationFamily = GedcomNode.create("FAMILY")
			.withID(family.getID())
			.addChildReference("SPOUSE1", extractSubStructure(family, "HUSB")
				.getID())
			.addChildReference("SPOUSE2", extractSubStructure(family, "WIFE")
				.getID());
		final List<GedcomNode> children = family.getChildrenWithTag("CHIL");
		for(final GedcomNode child : children)
			destinationFamily.addChildReference("CHILD", child.getID());
		notesTo(family, destinationFamily, destination);
		sourceCitationTo(family, destinationFamily, destination);
		documentsTo(family, destinationFamily, destination);
		eventTo(family, destinationFamily, destination, "ANUL", "ANNULMENT");
		eventTo(family, destinationFamily, destination, "CENS", "CENSUS");
		eventTo(family, destinationFamily, destination, "DIV", "DIVORCE");
		eventTo(family, destinationFamily, destination, "DIVF", "DIVORCE_FILED");
		eventTo(family, destinationFamily, destination, "ENGA", "ENGAGEMENT");
		eventTo(family, destinationFamily, destination, "MARB", "MARRIAGE_BANN");
		eventTo(family, destinationFamily, destination, "MARC", "MARRIAGE_CONTRACT");
		eventTo(family, destinationFamily, destination, "MARR", "MARRIAGE");
		eventTo(family, destinationFamily, destination, "MARL", "MARRIAGE_LICENCE");
		eventTo(family, destinationFamily, destination, "MARS", "MARRIAGE_SETTLEMENT");
		eventTo(family, destinationFamily, destination, "RESI", "RESIDENCE");
		eventTo(family, destinationFamily, destination, "EVEN", "EVENT");
		destinationFamily.addChildValue("RESTRICTION", extractSubStructure(family, "RESN")
			.getValue());

		destination.addFamily(destinationFamily);
	}

	private void notesTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes){
			String noteID = note.getID();
			if(noteID == null){
				noteID = destination.getNextNoteID();

				destination.addNote(GedcomNode.create("NOTE", noteID, note.getValue()));
			}
			destinationNode.addChildReference("NOTE", noteID);
		}
	}

	private void sourceCitationTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> sourceCitations = parent.getChildrenWithTag("SOUR");
		for(final GedcomNode sourceCitation : sourceCitations){
			String sourceCitationID = sourceCitation.getID();
			if(sourceCitationID == null){
				sourceCitationID = destination.getNextSourceID();

				//create source:
				final GedcomNode note = GedcomNode.create("NOTE")
					.withID(destination.getNextNoteID())
					.withValue(sourceCitation.getValue());
				destination.addNote(note);
				final GedcomNode destinationSource = GedcomNode.create("SOURCE")
					.withID(sourceCitationID)
					.addChildValue("EXTRACT", extractSubStructure(sourceCitation, "TEXT")
						.getValue())
					.addChildReference("NOTE", note.getID());
				documentsTo(sourceCitation, destinationSource, destination);
				notesTo(sourceCitation, destinationSource, destination);
				destination.addSource(destinationSource);

				//add source citation
				destinationNode.addChild(GedcomNode.create("SOURCE")
					.withID(sourceCitationID)
					.addChildValue("CREDIBILITY", extractSubStructure(sourceCitation, "QUAY")
						.getValue()));
			}
			else{
				//create source:
				final GedcomNode note = GedcomNode.create("NOTE")
					.withID(destination.getNextNoteID())
					.withValue(sourceCitation.getValue());
				destination.addNote(note);
				final GedcomNode eventNode = extractSubStructure(sourceCitation, "EVEN");
				final GedcomNode data = extractSubStructure(sourceCitation, "DATA");
				final GedcomNode destinationSource = GedcomNode.create("SOURCE")
					.withID(sourceCitationID)
					.addChildValue("EVENT", eventNode.getValue())
					.addChildValue("DATE", extractSubStructure(data, "DATE")
						.getValue());
				final List<GedcomNode> texts = data.getChildrenWithTag( "EXTRACT");
				for(final GedcomNode text : texts)
					destinationSource.addChildValue("TEXT", text
						.getValue());
				destinationSource.addChildReference("NOTE", note.getID());
				documentsTo(sourceCitation, destinationSource, destination);
				notesTo(sourceCitation, destinationSource, destination);
				destination.addSource(destinationSource);

				//add source citation
				destinationNode.addChild(GedcomNode.create("SOURCE")
					.withID(sourceCitationID)
					.addChildValue("PAGE", extractSubStructure(sourceCitation, "PAGE")
						.getValue())
					.addChildValue("ROLE", extractSubStructure(eventNode, "ROLE")
						.getValue())
					.addChildValue("CREDIBILITY", extractSubStructure(sourceCitation, "QUAY")
						.getValue()));
			}
		}
	}

	private void documentsTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> documents = parent.getChildrenWithTag("OBJE");
		for(final GedcomNode document : documents){
			String documentID = document.getID();
			if(documentID == null){
				documentID = destination.getNextSourceID();

				final GedcomNode destinationDocument = GedcomNode.create("SOURCE");
				final String documentTitle = extractSubStructure(document, "TITL")
					.getValue();
				final String documentFormat = extractSubStructure(document, "FORM")
					.getValue();
				final String documentMedia = extractSubStructure(document, "FORM", "MEDI")
					.getValue();
				final String documentFile = extractSubStructure(document, "FILE")
					.getValue();
				final String documentCut = extractSubStructure(document, "_CUTD")
					.getValue();

				destinationDocument.addChildValue("TITLE", documentTitle);
				if(documentFormat != null || documentMedia != null)
					destinationDocument.addChild(GedcomNode.create("FILE")
						.withValue(documentFile)
						.addChildValue("FORMAT", documentFormat)
						.addChildValue("MEDIA", documentMedia)
						.addChildValue("CUT", documentCut)
					);
				destination.addSource(destinationDocument
					.withID(documentID));
			}
			destinationNode.addChildReference("SOURCE", documentID);
		}
	}

	private void eventTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination,
		final String tagFrom, final String valueTo){
		final List<GedcomNode> events = individual.getChildrenWithTag(tagFrom);
		for(final GedcomNode event : events){
			final GedcomNode destinationEvent = createEventTo(valueTo, event, destination);
			destinationNode.addChild(destinationEvent);
		}
	}

	private GedcomNode createEventTo(final String valueTo, final GedcomNode event, final Flef destination){
		final GedcomNode destinationEvent = GedcomNode.create("EVENT")
			.withValue("EVENT".equals(valueTo)? event.getValue(): valueTo)
			.addChildValue("TYPE", extractSubStructure(event, "TYPE")
				.getValue())
			.addChildValue("DATE", extractSubStructure(event, "DATE")
				.getValue());
		placeAddressStructureTo(event, destinationEvent, destination);
		destinationEvent.addChildValue("AGENCY", extractSubStructure(event, "AGNC")
			.getValue())
			.addChildValue("CAUSE", extractSubStructure(event, "CAUS")
				.getValue());
		notesTo(event, destinationEvent, destination);
		sourceCitationTo(event, destinationEvent, destination);
		documentsTo(event, destinationEvent, destination);
		final GedcomNode familyChild = extractSubStructure(event, "FAMC");
		destinationEvent.addChildValue("RESTRICTION", extractSubStructure(event, "RESN")
			.getValue())
			.addChild(GedcomNode.create("FAMILY_CHILD")
				.withID(familyChild.getID())
				.addChildValue("ADOPTED_BY", extractSubStructure(familyChild, "ADOP")
					.getValue())
			);
		return destinationEvent;
	}

	private void placeAddressStructureTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final GedcomNode place = extractSubStructure(parent, "PLAC");
		final GedcomNode map = extractSubStructure(place, "MAP");
		final GedcomNode address = extractSubStructure(parent, "ADDR");
		final StringJoiner sj = new StringJoiner(" - ");
		final String wholeAddress = address.extractValueConcatenated();
		if(wholeAddress != null)
			sj.add(wholeAddress);
		for(final GedcomNode child : address.getChildren())
			if(ADDRESS_TAGS.contains(child.getTag())){
				final String value = child.getValue();
				if(value != null)
					sj.add(value);
			}

		final GedcomNode destinationPlace = GedcomNode.create("PLACE")
			.withID(destination.getNextPlaceID())
			.withValue(place.getValue())
			.addChildValue("ADDRESS", sj.toString())
			.addChildValue("CITY", extractSubStructure(address, "CITY").getValue())
			.addChildValue("STATE", extractSubStructure(address, "STAE").getValue())
			.addChildValue("COUNTRY", extractSubStructure(address, "CTRY").getValue())
			.addChild(GedcomNode.create("MAP")
				.addChildValue("LATITUDE", extractSubStructure(map, "LATI")
					.getValue())
				.addChildValue("LONGITUDE", extractSubStructure(map, "LONG")
					.getValue())
			);
		notesTo(place, destinationPlace, destination);
		destinationNode.addChildReference("PLACE", destinationPlace.getID());

		destination.addPlace(destinationPlace);
	}


	@Override
	public void from(final Flef origin, final Gedcom destination) throws GedcomGrammarParseException{
		final List<GedcomNode> families = origin.getFamilies();
		for(final GedcomNode family : families)
			familyRecordFrom(family, origin, destination);
	}

	private void familyRecordFrom(final GedcomNode family, final Flef origin, final Gedcom destination) throws GedcomGrammarParseException{
		final GedcomNode destinationFamily = GedcomNode.create("FAM")
			.withID(family.getID())
			.addChildValue("RESN", extractSubStructure(family, "RESTRICTION")
				.getValue());
		final List<GedcomNode> events = family.getChildrenWithTag("EVENT");
		eventFrom(events, destinationFamily, origin, "ANNULMENT", "ANUL");
		eventFrom(events, destinationFamily, origin, "CENSUS", "CENS");
		eventFrom(events, destinationFamily, origin, "DIVORCE", "DIV");
		eventFrom(events, destinationFamily, origin, "DIVORCE_FILED", "DIVF");
		eventFrom(events, destinationFamily, origin, "ENGAGEMENT", "ENGA");
		eventFrom(events, destinationFamily, origin, "MARRIAGE_BANN", "MARB");
		eventFrom(events, destinationFamily, origin, "MARRIAGE_CONTRACT", "MARC");
		eventFrom(events, destinationFamily, origin, "MARRIAGE", "MARR");
		eventFrom(events, destinationFamily, origin, "MARRIAGE_LICENCE", "MARL");
		eventFrom(events, destinationFamily, origin, "MARRIAGE_SETTLEMENT", "MARS");
		eventFrom(events, destinationFamily, origin, "RESIDENCE", "RESI");
		eventFrom(events, destinationFamily, origin, "EVENT", "EVEN");
		destinationFamily
			.addChildValue("HUSB", extractSubStructure(family, "SPOUSE1")
				.getValue())
			.addChildValue("WIFE", extractSubStructure(family, "SPOUSE2")
				.getValue());
		final List<GedcomNode> children = family.getChildrenWithTag("CHILD");
		for(final GedcomNode child : children)
			destinationFamily.addChildReference("CHIL", child.getID());
		notesFrom(family, destinationFamily);
		sourceCitationFrom(family, destinationFamily);

		destination.addFamily(destinationFamily);
	}

	private void eventFrom(final List<GedcomNode> events, final GedcomNode destinationNode, final Flef origin, final String valueFrom,
		final String tagTo) throws GedcomGrammarParseException{
		for(final GedcomNode event : events)
			if(valueFrom.equals(event.getValue())){
				final GedcomNode destinationEvent = createEventFrom(tagTo, event, origin);
				destinationNode.addChild(destinationEvent);
			}
	}

	private GedcomNode createEventFrom(final String tagTo, final GedcomNode event, final Flef origin) throws GedcomGrammarParseException{
		final GedcomNode destinationEvent = GedcomNode.create(tagTo)
			.addChildValue("TYPE", extractSubStructure(event, "TYPE")
				.getValue())
			.addChildValue("DATE", extractSubStructure(event, "DATE")
				.getValue());
		placeStructureFrom(event, destinationEvent, origin);
		addressStructureFrom(event, destinationEvent, origin);
		destinationEvent.addChildValue("AGNC", extractSubStructure(event, "AGENCY")
			.getValue())
			.addChildValue("CAUS", extractSubStructure(event, "CAUSE")
				.getValue());
		final GedcomNode familyChild = extractSubStructure(event, "FAMILY_CHILD");
		destinationEvent.addChildValue("RESN", extractSubStructure(event, "RESTRICTION")
			.getValue())
			.addChild(GedcomNode.create("FAMC")
				.withID(familyChild.getID())
				.addChildValue("ADOP", extractSubStructure(familyChild, "ADOPTED_BY")
					.getValue())
			);
		notesFrom(event, destinationEvent);
		sourceCitationFrom(event, destinationEvent);
		return destinationEvent;
	}

	private void placeStructureFrom(final GedcomNode parent, final GedcomNode destinationNode, final Flef origin)
		throws GedcomGrammarParseException{
		final GedcomNode place = extractSubStructure(parent, "PLACE");
		if(!place.isEmpty()){
			final GedcomNode placeRecord = origin.getPlace(place.getID());
			if(placeRecord == null)
				throw GedcomGrammarParseException.create("Place with ID {} not found", place.getID());

			final GedcomNode map = extractSubStructure(placeRecord, "MAP");

			final GedcomNode destinationPlace = GedcomNode.create("PLAC")
				.withValue(extractSubStructure(placeRecord, "NAME")
					.getValue())
				.addChild(GedcomNode.create("MAP")
					.addChildValue("LATI", extractSubStructure(map, "LATI")
						.getValue())
					.addChildValue("LONG", extractSubStructure(map, "LONG")
						.getValue())
				);
			notesFrom(place, destinationPlace);
			destinationNode.addChild(destinationPlace);
		}
	}

	private void addressStructureFrom(final GedcomNode parent, final GedcomNode destinationNode, final Flef origin) throws GedcomGrammarParseException{
		final GedcomNode place = extractSubStructure(parent, "PLACE");
		if(!place.isEmpty()){
			final GedcomNode placeRecord = origin.getPlace(place.getID());
			if(placeRecord == null)
				throw GedcomGrammarParseException.create("Place with ID {} not found", place.getID());

			final GedcomNode address = extractSubStructure(placeRecord, "ADDRESS");
			destinationNode.addChild(GedcomNode.create("ADDR")
				.withValue(placeRecord.getValue())
				.addChildValue("CITY", extractSubStructure(address, "CITY").getValue())
				.addChildValue("STAE", extractSubStructure(address, "STATE").getValue())
				.addChildValue("CTRY", extractSubStructure(address, "COUNTRY").getValue()));
		}
	}

	private void notesFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			destinationNode.addChildReference("NOTE", note.getID());
	}

	private void sourceCitationFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> sourceCitations = parent.getChildrenWithTag("SOURCE");
		for(final GedcomNode sourceCitation : sourceCitations){
			//create source:
			final GedcomNode destinationSource = GedcomNode.create("SOUR")
				.withID(sourceCitation.getID())
				.addChildValue("PAGE", extractSubStructure(sourceCitation, "PAGE")
					.getValue())
				.addChild(GedcomNode.create("EVEN")
					.addChildValue("ROLE", extractSubStructure(sourceCitation, "ROLE")
						.getValue())
				)
				.addChildValue("QUAY", extractSubStructure(sourceCitation, "CREDIBILITY")
					.getValue());
			notesFrom(sourceCitation, destinationSource);
			destinationNode.addChild(destinationSource);
		}
	}

}
