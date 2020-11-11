package io.github.mtrevisan.familylegacy.gedcom.transformations2;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class IndividualTransformationTest{

	@Test
	void to(){
		final GedcomNode note = GedcomNode.create("INDI")
			.withID("I1")
			.addChildValue("RESN", "RESTRICTION_NOTICE")
			.addChildValue("???", "<<PERSONAL_NAME_STRUCTURE>>")
			.addChildValue("SEX", "SEX_VALUE")
			.addChildValue("???", "<<INDIVIDUAL_EVENT_STRUCTURE>>")
			.addChildValue("???", "<<INDIVIDUAL_ATTRIBUTE_STRUCTURE>>")
			.addChildValue("???", "<<CHILD_TO_FAMILY_LINK>>")
			.addChildValue("???", "<<SPOUSE_TO_FAMILY_LINK>>")
			.addChildReference("SUBM", "SUBM1")
			.addChildValue("???", "<<ASSOCIATION_STRUCTURE>>")
			.addChildValue("???", "<<ASSOCIATION_STRUCTURE>>")
			.addChildValue("???", "<<NOTE_STRUCTURE>>")
			.addChildValue("???", "<<SOURCE_CITATION>>")
			.addChildValue("???", "<<MULTIMEDIA_LINK>>")
;
		final Gedcom origin = new Gedcom();
		origin.addNote(note);
		final Flef destination = new Flef();

		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}, {tag: REFN, value: USER_REFERENCE_NUMBER, children: [{tag: TYPE, value: USER_REFERENCE_TYPE}]}, {tag: RIN, value: AUTOMATED_RECORD_ID}, {tag: <SOURCE_CITATION>, value: SOURCE_DESCRIPTIVE_TITLE}, {tag: <CHANGE_DATE>, value: SOURCE_FILED_BY_ENTRY}]", origin.getNotes().get(0).toString());

		final Transformation<Gedcom, Flef> t = new NoteTransformation();
		t.to(origin, destination);

		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT", destination.getNotes().get(0).toString());
	}

	@Test
	void from(){
		final GedcomNode note = GedcomNode.create("INDIVIDUAL")
			.withID("I1")
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