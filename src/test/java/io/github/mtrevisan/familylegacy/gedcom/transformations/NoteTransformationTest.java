package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class NoteTransformationTest{

	@Test
	void to(){
		final GedcomNode note = GedcomNode.create("NOTE")
			.withID("N1")
			.withValue("SUBMITTER_TEXT\\nSUBMITTER_TEXT")
			.addChild(GedcomNode.create("REFN")
				.withValue("USER_REFERENCE_NUMBER")
				.addChildValue("TYPE", "USER_REFERENCE_TYPE")
			)
			.addChildValue("RIN", "AUTOMATED_RECORD_ID")
			.addChildReference("SOUR", "S1");
		final Gedcom origin = new Gedcom();
		origin.addNote(note);
		final Flef destination = new Flef();

		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}, {tag: REFN, value: USER_REFERENCE_NUMBER, children: [{tag: TYPE, value: USER_REFERENCE_TYPE}]}, {tag: RIN, value: AUTOMATED_RECORD_ID}, {id: S1, tag: SOUR}]", origin.getNotes().get(0).toString());

		final Transformation<Gedcom, Flef> t = new NoteTransformation();
		t.to(origin, destination);

		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT", destination.getNotes().get(0).toString());
	}

	@Test
	void from() throws GedcomGrammarParseException{
		final GedcomNode note = GedcomNode.create("NOTE")
			.withID("N1")
			.withValue("SUBMITTER_TEXT")
			.addChildValue("RESTRICTION", "RESTRICTION_NOTICE");
		final Flef origin = new Flef();
		origin.addNote(note);
		final Gedcom destination = new Gedcom();

		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: RESTRICTION, value: RESTRICTION_NOTICE}]", origin.getNotes().get(0).toString());

		final Transformation<Gedcom, Flef> t = new NoteTransformation();
		t.from(origin, destination);

		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXT", destination.getNotes().get(0).toString());
	}

}