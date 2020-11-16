package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;
import java.util.StringJoiner;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.addressStructureTo;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.documentTo;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.noteTo;


public class SubmitterTransformation implements Transformation<Gedcom, Flef>{


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
		documentTo(submitter, destinationSource, destination);
		final List<GedcomNode> preferredLanguages = submitter.getChildrenWithTag("LANG");
		final StringJoiner sj = new StringJoiner(", ");
		for(final GedcomNode preferredLanguage : preferredLanguages)
			sj.add(preferredLanguage.getValue());
		if(sj.length() > 0){
			final String noteID = destination.getNextNoteID();
			destination.addNote(GedcomNode.create("NOTE", noteID, "Preferred contact language(s): " + sj));
			destinationSource.addChildReference("NOTE", noteID);
		}
		noteTo(submitter, destinationSource, destination);

		destination.addSource(destinationSource);
	}


	@Override
	public void from(final Flef origin, final Gedcom destination){}

}
