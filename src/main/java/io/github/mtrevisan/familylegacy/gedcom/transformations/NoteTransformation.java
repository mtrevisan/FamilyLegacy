package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;


public class NoteTransformation extends Transformation<Gedcom, Flef>{

	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> notes = origin.getNotes();
		for(final GedcomNode note : notes)
			noteRecordTo(note, destination);
	}

	private void noteRecordTo(final GedcomNode note, final Flef destination){
		final GedcomNode destinationNote = transformerTo.create("NOTE", note.getID(), note.getValue());

		destination.addNote(destinationNote);
	}


	@Override
	public void from(final Flef origin, final Gedcom destination){
		final List<GedcomNode> notes = origin.getNotes();
		for(final GedcomNode note : notes)
			noteRecordFrom(note, destination);
	}

	private void noteRecordFrom(final GedcomNode note, final Gedcom destination){
		final GedcomNode destinationNote = transformerFrom.create("NOTE", note.getID(), note.getValue());

		destination.addNote(destinationNote);
	}

}
