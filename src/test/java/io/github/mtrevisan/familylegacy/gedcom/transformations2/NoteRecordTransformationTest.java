package io.github.mtrevisan.familylegacy.gedcom.transformations2;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class NoteRecordTransformationTest{

	@Test
	void to(){
		final GedcomNode note = GedcomNode.create("NOTE")
			.withID("N1")
			.withValueConcatenated("SUBMITTER_TEXT\\nSUBMITTER_TEXT")
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

		Assertions.assertEquals("id: S1, tag: SOUR, children: [{tag: DATA, children: [{tag: EVEN, value: EVENTS_RECORDED1, children: [{tag: DATE, value: DATE_PERIOD}, {tag: PLAC, value: SOURCE_JURISDICTION_PLACE}]}, {tag: EVEN, value: EVENTS_RECORDED2}]}, {tag: AUTH, value: SOURCE_ORIGINATOR}, {tag: TITL, value: SOURCE_DESCRIPTIVE_TITLE}, {tag: ABBR, value: SOURCE_FILED_BY_ENTRY}, {tag: PUBL, value: SOURCE_PUBLICATION_FACTS}, {tag: TEXT, value: TEXT_FROM_SOURCE}, {id: R1, tag: REPO}, {tag: REPO, children: [{id: N2, tag: NOTE}, {tag: CALN, value: SOURCE_CALL_NUMBER}]}, {tag: REFN, value: USER_REFERENCE_NUMBER, children: [{tag: TYPE, value: USER_REFERENCE_TYPE}]}, {tag: RIN, value: AUTOMATED_RECORD_ID}, {tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE}]}, {id: N1, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FORM, value: MULTIMEDIA_FORMAT, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}]}]", origin.getSources().get(0).toString());
		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXT", origin.getNotes().get(0).toString());

		final Transformation<Gedcom, Flef> t = new SourceRecordTransformation();
		t.to(origin, destination);

		Assertions.assertEquals("id: S1, tag: SOURCE, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORMAT, value: MULTIMEDIA_FORMAT}, {tag: MEDIA, value: SOURCE_MEDIA_TYPE}]}]", destination.getSources().get(0).toString());
		Assertions.assertEquals("id: S1, tag: SOURCE, children: [{tag: TITLE, value: SOURCE_DESCRIPTIVE_TITLE}, {tag: EVENT, value: EVENTS_RECORDED1}, {tag: EVENT, value: EVENTS_RECORDED2}, {tag: DATE, value: DATE_PERIOD}, {tag: TEXT, value: TEXT_FROM_SOURCE}, {id: N1, tag: NOTE}, {id: D1, tag: SOURCE}, {id: S1, tag: SOURCE}, {id: N1, tag: NOTE}, {id: N2, tag: NOTE}, {id: R1, tag: REPOSITORY}, {id: R1, tag: REPOSITORY, children: [{id: N2, tag: NOTE}]}]", destination.getSources().get(1).toString());
		Assertions.assertEquals("id: N1, tag: NOTE, value: SOURCE_ORIGINATOR, SOURCE_PUBLICATION_FACTS", destination.getNotes().get(0).toString());
		Assertions.assertEquals("id: N2, tag: NOTE, value: SUBMITTER_TEXT", destination.getNotes().get(1).toString());
		Assertions.assertEquals("id: R1, tag: REPOSITORY, children: [{id: N2, tag: NOTE}]", destination.getRepositories().get(0).toString());
	}

	@Test
	void from(){
		final GedcomNode note = GedcomNode.create("NOTE")
			.withID("N1")
			.withValue("SUBMITTER_TEXT")
			.addChild(GedcomNode.create("RESTRICTION")
				.withValue("RESTRICTION_NOTICE"));
		final Flef origin = new Flef();
		origin.addNote(note);
		final Gedcom destination = new Gedcom();

		Assertions.assertEquals("id: S1, tag: SOURCE, children: [{tag: TYPE, value: DIGITAL_ARCHIVE}, {tag: TITLE, value: SOURCE_DESCRIPTIVE_TITLE}, {tag: EVENT, value: EVENTS_RECORDED1}, {tag: EVENT, value: EVENTS_RECORDED2}, {tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE}, {id: N1, tag: NOTE}, {id: R1, tag: REPOSITORY, children: [{tag: REPOSITORY_LOCATION, value: REPOSITORY_LOCATION_TEXT}, {id: N2, tag: NOTE}]}, {tag: FILE, value: DOCUMENT_FILE_REFERENCE, children: [{tag: FORMAT, value: DOCUMENT_FORMAT}, {tag: MEDIA, value: SOURCE_MEDIA_TYPE}, {tag: CUT, value: CUT_COORDINATES}]}, {tag: URL, value: ADDRESS_WEB_PAGE}, {tag: RESTRICTION, value: RESTRICTION_NOTICE}]", origin.getSources().get(0).toString());

		final Transformation<Gedcom, Flef> t = new SourceRecordTransformation();
		t.from(origin, destination);

		Assertions.assertEquals("id: S1, tag: SOUR, children: [{tag: DATA, children: [{tag: EVEN, value: EVENTS_RECORDED1, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}]}, {tag: EVEN, value: EVENTS_RECORDED2, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}]}]}, {tag: TITL, value: SOURCE_DESCRIPTIVE_TITLE}, {tag: TEXT, value: TEXT_FROM_SOURCE}, {id: R1, tag: REPO}, {id: N1, tag: NOTE}, {tag: OBJE, children: [{tag: FORM, value: DOCUMENT_FORMAT, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}, {tag: FILE, value: DOCUMENT_FILE_REFERENCE}, {tag: CUT, value: Y}, {tag: _CUTD, value: CUT_COORDINATES}]}]", destination.getSources().get(0).toString());
	}

}