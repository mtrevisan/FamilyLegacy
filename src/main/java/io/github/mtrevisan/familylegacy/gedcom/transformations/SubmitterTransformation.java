package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.StringJoiner;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


public class SubmitterTransformation implements Transformation<Gedcom, Flef>{

	private static final Collection<String> ADDRESS_TAGS = new HashSet<>(Arrays.asList("CONT", "ADR1", "ADR2", "ADR3"));


	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> submitters = origin.getSubmitters();
		for(final GedcomNode sbmitter : submitters)
			submitterRecordTo(sbmitter, destination);
	}

	private void submitterRecordTo(final GedcomNode submitter, final Flef destination){
		final GedcomNode name = extractSubStructure(submitter, "NAME");
		final GedcomNode destinationSource = GedcomNode.create("SOURCE")
			.withID(submitter.getID())
			.addChildValue("TITLE", name.getValue());
		addressStructureTo(submitter, destinationSource, destination);
		documentsTo(submitter, destinationSource, destination);
		final List<GedcomNode> preferredLanguages = submitter.getChildrenWithTag("LANG");
		final StringJoiner sj = new StringJoiner(", ");
		for(final GedcomNode preferredLanguage : preferredLanguages)
			sj.add(preferredLanguage.getValue());
		if(sj.length() > 0){
			final String noteID = destination.getNextNoteID();
			destination.addNote(GedcomNode.create("NOTE", noteID, "Preferred contact language(s): " + sj));
			destinationSource.addChildReference("NOTE", noteID);
		}
		notesTo(submitter, destinationSource, destination);

		destination.addSource(destinationSource);
	}

	private void addressStructureTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
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
			.addChildValue("ADDRESS", sj.toString())
			.addChildValue("CITY", extractSubStructure(address, "CITY").getValue())
			.addChildValue("STATE", extractSubStructure(address, "STAE").getValue())
			.addChildValue("COUNTRY", extractSubStructure(address, "CTRY").getValue());
		destinationNode.addChildReference("PLACE", destinationPlace.getID());

		destination.addPlace(destinationPlace);
	}

	private void documentsTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> documents = parent.getChildrenWithTag("OBJE");
		for(final GedcomNode document : documents){
			String documentID = document.getID();
			if(documentID == null){
				documentID = destination.getNextSourceID();

				final GedcomNode destinationDocument = GedcomNode.create("SOURCE")
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
					destinationDocument.addChild(GedcomNode.create("FILE")
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


	@Override
	public void from(final Flef origin, final Gedcom destination){}

}
