package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNodeBuilder;
import io.github.mtrevisan.familylegacy.gedcom.Protocol;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;


final class Transformer{

	private static final Collection<String> ADDRESS_TAGS = new HashSet<>(Arrays.asList("CONT", "ADR1", "ADR2", "ADR3"));


	private final Protocol protocol;


	Transformer(final Protocol protocol){
		this.protocol = protocol;
	}


	public GedcomNode create(final String tag){
		return GedcomNodeBuilder.create(protocol, tag);
	}

	public GedcomNode create(final String tag, final String id, final String value){
		return GedcomNodeBuilder.create(protocol, tag, id, value);
	}

	String joinIfNotNull(final String separator, final String... components){
		final StringJoiner sj = new StringJoiner(separator);
		for(final String component : components)
			if(component != null)
				sj.add(component);
		return (sj.length() > 0? sj.toString(): null);
	}

	GedcomNode extractSubStructure(final GedcomNode context, final String... tags){
		GedcomNode current = context;
		for(final String tag : tags){
			final List<GedcomNode> childrenWithTag = current.getChildrenWithTag(tag);
			if(childrenWithTag.size() != 1)
				return GedcomNodeBuilder.createEmpty(protocol);

			current = childrenWithTag.get(0);
		}
		return current;
	}


	void eventTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination,
			final String tagFrom, final String valueTo){
		final List<GedcomNode> events = individual.getChildrenWithTag(tagFrom);
		for(final GedcomNode event : events){
			final GedcomNode destinationEvent = createEventTo(valueTo, event, destination);
			destinationNode.addChild(destinationEvent);
		}
	}

	void documentTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> documents = parent.getChildrenWithTag("OBJE");
		for(final GedcomNode document : documents){
			String documentID = document.getID();
			if(documentID == null){
				documentID = destination.getNextSourceID();

				final GedcomNode destinationDocument = GedcomNodeBuilder.create(protocol, "SOURCE")
					.withID(documentID);
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
					destinationDocument.addChild(GedcomNodeBuilder.create(protocol, "FILE")
						.withValue(documentFile)
						.addChildValue("FORMAT", documentFormat)
						.addChildValue("MEDIA", documentMedia)
						.addChildValue("CUT", documentCut)
					);
				destination.addSource(destinationDocument);
			}
			destinationNode.addChildReference("SOURCE", documentID);
		}
	}

	void sourceCitationTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> sourceCitations = parent.getChildrenWithTag("SOUR");
		for(final GedcomNode sourceCitation : sourceCitations){
			String sourceCitationID = sourceCitation.getID();
			if(sourceCitationID == null){
				sourceCitationID = destination.getNextSourceID();

				//create source:
				final GedcomNode note = GedcomNodeBuilder.create(protocol, "NOTE", destination.getNextNoteID(), sourceCitation.getValue());
				destination.addNote(note);
				final GedcomNode destinationSource = GedcomNodeBuilder.create(protocol, "SOURCE")
					.withID(sourceCitationID)
					.addChildValue("EXTRACT", extractSubStructure(sourceCitation, "TEXT")
						.getValue())
					.addChildReference("NOTE", note.getID());
				documentTo(sourceCitation, destinationSource, destination);
				noteTo(sourceCitation, destinationSource, destination);
				destination.addSource(destinationSource);

				//add source citation
				destinationNode.addChild(GedcomNodeBuilder.create(protocol, "SOURCE")
					.withID(sourceCitationID)
					.addChildValue("CREDIBILITY", extractSubStructure(sourceCitation, "QUAY")
						.getValue()));
			}
			else{
				//create source:
				final GedcomNode note = GedcomNodeBuilder.create(protocol, "NOTE", destination.getNextNoteID(), sourceCitation.getValue());
				destination.addNote(note);
				final GedcomNode eventNode = extractSubStructure(sourceCitation, "EVEN");
				final GedcomNode data = extractSubStructure(sourceCitation, "DATA");
				final GedcomNode destinationSource = GedcomNodeBuilder.create(protocol, "SOURCE")
					.withID(sourceCitationID)
					.addChildValue("EVENT", eventNode.getValue())
					.addChildValue("DATE", extractSubStructure(data, "DATE")
						.getValue());
				final List<GedcomNode> texts = data.getChildrenWithTag( "EXTRACT");
				for(final GedcomNode text : texts)
					destinationSource.addChildValue("TEXT", text
						.getValue());
				destinationSource.addChildReference("NOTE", note.getID());
				documentTo(sourceCitation, destinationSource, destination);
				noteTo(sourceCitation, destinationSource, destination);
				destination.addSource(destinationSource);

				//add source citation
				destinationNode.addChild(GedcomNodeBuilder.create(protocol, "SOURCE")
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

	void addressStructureTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final GedcomNode address = extractSubStructure(parent, "ADDR");
		final String addressValue = extractAddressValue(address);

		final GedcomNode destinationPlace = GedcomNodeBuilder.create(protocol, "PLACE")
			.withID(destination.getNextPlaceID())
			.addChildValue("ADDRESS", addressValue)
			.addChildValue("CITY", extractSubStructure(address, "CITY")
				.getValue())
			.addChildValue("STATE", extractSubStructure(address, "STAE")
				.getValue())
			.addChildValue("COUNTRY", extractSubStructure(address, "CTRY")
				.getValue());
		destinationNode.addChildReference("PLACE", destinationPlace.getID());

		destination.addPlace(destinationPlace);
	}

	private GedcomNode createEventTo(final String valueTo, final GedcomNode event, final Flef destination){
		final GedcomNode destinationEvent = GedcomNodeBuilder.create(protocol, "EVENT")
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
		noteTo(event, destinationEvent, destination);
		sourceCitationTo(event, destinationEvent, destination);
		documentTo(event, destinationEvent, destination);
		final GedcomNode familyChild = extractSubStructure(event, "FAMC");
		destinationEvent.addChildValue("RESTRICTION", extractSubStructure(event, "RESN")
			.getValue())
			.addChild(GedcomNodeBuilder.create(protocol, "FAMILY_CHILD")
				.withID(familyChild.getID())
				.addChildValue("ADOPTED_BY", extractSubStructure(familyChild, "ADOP")
					.getValue())
			);
		return destinationEvent;
	}

	void placeAddressStructureTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final GedcomNode address = extractSubStructure(parent, "ADDR");
		final String addressValue = extractAddressValue(address);

		final GedcomNode place = extractSubStructure(parent, "PLAC");
		final GedcomNode map = extractSubStructure(place, "MAP");
		final GedcomNode destinationPlace = GedcomNodeBuilder.create(protocol, "PLACE")
			.withID(destination.getNextPlaceID())
			.withValue(place.getValue())
			.addChildValue("ADDRESS", addressValue)
			.addChildValue("CITY", extractSubStructure(address, "CITY").getValue())
			.addChildValue("STATE", extractSubStructure(address, "STAE").getValue())
			.addChildValue("COUNTRY", extractSubStructure(address, "CTRY").getValue())
			.addChild(GedcomNodeBuilder.create(protocol, "MAP")
				.addChildValue("LATITUDE", extractSubStructure(map, "LATI")
					.getValue())
				.addChildValue("LONGITUDE", extractSubStructure(map, "LONG")
					.getValue())
			);
		noteTo(place, destinationPlace, destination);
		destinationNode.addChildReference("PLACE", destinationPlace.getID());

		destination.addPlace(destinationPlace);
	}

	private String extractAddressValue(final GedcomNode address){
		final StringJoiner sj = new StringJoiner(" - ");
		final String wholeAddress = address.getValue();
		if(wholeAddress != null)
			sj.add(wholeAddress);
		for(final GedcomNode child : address.getChildren())
			if(ADDRESS_TAGS.contains(child.getTag())){
				final String value = child.getValue();
				if(value != null)
					sj.add(value);
			}
		return (sj.length() > 0? sj.toString(): null);
	}

	void noteTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes){
			String noteID = note.getID();
			if(noteID == null){
				noteID = destination.getNextNoteID();

				destination.addNote(GedcomNodeBuilder.create(protocol, "NOTE", noteID, note.getValue()));
			}
			destinationNode.addChildReference("NOTE", noteID);
		}
	}


	void eventFrom(final Iterable<GedcomNode> events, final GedcomNode destinationNode, final Flef origin, final String valueFrom,
			final String tagTo) throws GedcomGrammarParseException{
		final Iterator<GedcomNode> itr = events.iterator();
		while(itr.hasNext()){
			final GedcomNode event = itr.next();
			if("@EVENT@".equals(valueFrom) || valueFrom.equals(event.getValue())){
				final GedcomNode destinationEvent = createEventFrom(tagTo, event, origin);
				destinationNode.addChild(destinationEvent);

				itr.remove();
			}
		}
	}

	void documentFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> files = parent.getChildrenWithTag("FILE");
		for(final GedcomNode file : files){
			final String format = extractSubStructure(file, "FORMAT")
				.getValue();
			final String media = extractSubStructure(file, "MEDIA")
				.getValue();
			final GedcomNode destinationObject = GedcomNodeBuilder.create(protocol, "OBJE")
				.addChild(GedcomNodeBuilder.create(protocol, "FORM")
					.withValue(format)
					.addChildValue("MEDI", media)
				)
				.addChildValue("FILE", file.getValue());
			final GedcomNode cut = extractSubStructure(file, "CUT");
			if(!cut.isEmpty())
				destinationObject.addChildValue("CUT", "Y")
					.addChildValue("_CUTD", cut.getValue());
			destinationNode.addChild(destinationObject);
		}
	}

	void sourceCitationFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> sourceCitations = parent.getChildrenWithTag("SOURCE");
		for(final GedcomNode sourceCitation : sourceCitations){
			//create source:
			final GedcomNode destinationSource = GedcomNodeBuilder.create(protocol, "SOUR")
				.withID(sourceCitation.getID())
				.addChildValue("PAGE", extractSubStructure(sourceCitation, "PAGE")
					.getValue())
				.addChild(GedcomNodeBuilder.create(protocol, "EVEN")
					.addChildValue("ROLE", extractSubStructure(sourceCitation, "ROLE")
						.getValue())
				)
				.addChildValue("QUAY", extractSubStructure(sourceCitation, "CREDIBILITY")
					.getValue());
			noteFrom(sourceCitation, destinationSource);
			destinationNode.addChild(destinationSource);
		}
	}

	private GedcomNode createEventFrom(final String tagTo, final GedcomNode event, final Flef origin)
			throws GedcomGrammarParseException{
		final GedcomNode destinationEvent = GedcomNodeBuilder.create(protocol, tagTo)
			.withValue("EVENT".equals(tagTo)? event.getValue(): null)
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
			.addChild(GedcomNodeBuilder.create(protocol, "FAMC")
				.withID(familyChild.getID())
				.addChildValue("ADOP", extractSubStructure(familyChild, "ADOPTED_BY")
					.getValue())
			);
		noteFrom(event, destinationEvent);
		sourceCitationFrom(event, destinationEvent);
		return destinationEvent;
	}

	void addressStructureFrom(final GedcomNode parent, final GedcomNode destinationNode, final Flef origin)
		throws GedcomGrammarParseException{
		final GedcomNode place = extractSubStructure(parent, "PLACE");
		if(!place.isEmpty()){
			final GedcomNode placeRecord = origin.getPlace(place.getID());
			if(placeRecord == null)
				throw GedcomGrammarParseException.create("Place with ID {} not found", place.getID());

			final GedcomNode address = extractSubStructure(placeRecord, "ADDRESS");
			destinationNode.addChild(GedcomNodeBuilder.create(protocol, "ADDR")
				.withValue(placeRecord.getValue())
				.addChildValue("CITY", extractSubStructure(address, "CITY").getValue())
				.addChildValue("STAE", extractSubStructure(address, "STATE").getValue())
				.addChildValue("CTRY", extractSubStructure(address, "COUNTRY").getValue()));
		}
	}

	void placeStructureFrom(final GedcomNode parent, final GedcomNode destinationNode, final Flef origin)
			throws GedcomGrammarParseException{
		final GedcomNode place = extractSubStructure(parent, "PLACE");
		if(!place.isEmpty()){
			final GedcomNode placeRecord = origin.getPlace(place.getID());
			if(placeRecord == null)
				throw GedcomGrammarParseException.create("Place with ID {} not found", place.getID());

			final GedcomNode map = extractSubStructure(placeRecord, "MAP");

			final GedcomNode destinationPlace = GedcomNodeBuilder.create(protocol, "PLAC")
				.withValue(extractSubStructure(placeRecord, "NAME")
					.getValue())
				.addChild(GedcomNodeBuilder.create(protocol, "MAP")
					.addChildValue("LATI", extractSubStructure(map, "LATI")
						.getValue())
					.addChildValue("LONG", extractSubStructure(map, "LONG")
						.getValue())
				);
			noteFrom(place, destinationPlace);
			destinationNode.addChild(destinationPlace);
		}
	}

	void noteFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			destinationNode.addChildReference("NOTE", note.getID());
	}

}
