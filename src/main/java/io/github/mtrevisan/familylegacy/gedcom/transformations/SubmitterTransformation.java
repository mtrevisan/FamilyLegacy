package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.Protocol;

import java.util.List;
import java.util.StringJoiner;


public class SubmitterTransformation implements Transformation<Gedcom, Flef>{

	private final Transformer transformerTo = new Transformer(Protocol.FLEF);


	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> submitters = origin.getSubmitters();
		for(final GedcomNode submitter : submitters)
			submitterRecordTo(submitter, destination);
	}

	private void submitterRecordTo(final GedcomNode submitter, final Flef destination){
		final GedcomNode name = transformerTo.extractSubStructure(submitter, "NAME");
		final GedcomNode destinationSource = transformerTo.create("SOURCE")
			.withID(submitter.getID())
			.addChildValue("TITLE", name.getValue());
		transformerTo.addressStructureTo(submitter, destinationSource, destination);
		transformerTo.documentTo(submitter, destinationSource, destination);
		final List<GedcomNode> preferredLanguages = submitter.getChildrenWithTag("LANG");
		final StringJoiner sj = new StringJoiner(", ");
		for(final GedcomNode preferredLanguage : preferredLanguages)
			sj.add(preferredLanguage.getValue());
		if(sj.length() > 0){
			final String noteID = destination.getNextNoteID();
			destination.addNote(transformerTo.create("NOTE", noteID, "Preferred contact language(s): " + sj));
			destinationSource.addChildReference("NOTE", noteID);
		}
		transformerTo.noteTo(submitter, destinationSource, destination);

		destination.addSource(destinationSource);
	}


	@Override
	public void from(final Flef origin, final Gedcom destination){}

}
