package io.github.mtrevisan.familylegacy.gedcom.transformations2;

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
				.addChild(GedcomNode.create("TYPE")
					.withValue("USER_REFERENCE_TYPE"))
			)
			.addChild(GedcomNode.create("RIN")
				.withValue("AUTOMATED_RECORD_ID"))
			.addChild(GedcomNode.create("<SOURCE_CITATION>")
				.withValue("SOURCE_DESCRIPTIVE_TITLE"))
			.addChild(GedcomNode.create("<CHANGE_DATE>")
				.withValue("SOURCE_FILED_BY_ENTRY"));
		final Gedcom origin = new Gedcom();
		origin.addNote(note);
		final Flef destination = new Flef();

		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}, {tag: REFN, value: USER_REFERENCE_NUMBER, children: [{tag: TYPE, value: USER_REFERENCE_TYPE}]}, {tag: RIN, value: AUTOMATED_RECORD_ID}, {tag: <SOURCE_CITATION>, value: SOURCE_DESCRIPTIVE_TITLE}, {tag: <CHANGE_DATE>, value: SOURCE_FILED_BY_ENTRY}]", origin.getNotes().get(0).toString());

		final Transformation<Gedcom, Flef> t = new NoteTransformation();
		t.to(origin, destination);

		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT", destination.getNotes().get(0).toString());
	}

	@Test
	void from() throws GedcomGrammarParseException{
		final GedcomNode note = GedcomNode.create("NOTE")
			.withID("N1")
			.withValue("SUBMITTER_TEXT")
			.addChild(GedcomNode.create("RESTRICTION")
				.withValue("RESTRICTION_NOTICE"));
		final Flef origin = new Flef();
		origin.addNote(note);
		final Gedcom destination = new Gedcom();

		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: RESTRICTION, value: RESTRICTION_NOTICE}]", origin.getNotes().get(0).toString());

		final Transformation<Gedcom, Flef> t = new NoteTransformation();
		t.from(origin, destination);

		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXT", destination.getNotes().get(0).toString());
	}

}