package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class SubmitterTransformationTest{

	@Test
	void to(){
		final GedcomNode submitter = GedcomNode.create("SUBM")
			.withID("SUBM1")
			.addChildValue("NAME", "SUBMITTER_NAME")
			.addChildValue("???", "ADDRESS_STRUCTURE")
			.addChildValue("???", "MULTIMEDIA_LINK")
			.addChildValue("LANG", "LANGUAGE_PREFERENCE1")
			.addChildValue("LANG", "LANGUAGE_PREFERENCE2")
			.addChildValue("RFN", "SUBMITTER_REGISTERED_RFN")
			.addChildValue("RIN", "AUTOMATED_RECORD_ID")
			.addChildReference("NOTE", "N1")
			.addChild(GedcomNode.create("CHAN")
				.addChildValue("DATE", "CHANGE_DATE")
			);
		final Gedcom origin = new Gedcom();
		origin.addSubmitter(submitter);
		final Flef destination = new Flef();

		Assertions.assertEquals("id: S1, tag: SOUR, children: [{tag: DATA, children: [{tag: EVEN, value: EVENTS_RECORDED1, children: [{tag: DATE, value: DATE_PERIOD}, {tag: PLAC, value: SOURCE_JURISDICTION_PLACE}]}, {tag: EVEN, value: EVENTS_RECORDED2}]}, {tag: AUTH, value: SOURCE_ORIGINATOR}, {tag: TITL, value: SOURCE_DESCRIPTIVE_TITLE}, {tag: ABBR, value: SOURCE_FILED_BY_ENTRY}, {tag: PUBL, value: SOURCE_PUBLICATION_FACTS}, {tag: TEXT, value: TEXT_FROM_SOURCE}, {id: R1, tag: REPO}, {tag: REPO, children: [{id: N2, tag: NOTE}, {tag: CALN, value: SOURCE_CALL_NUMBER}]}, {tag: REFN, value: USER_REFERENCE_NUMBER, children: [{tag: TYPE, value: USER_REFERENCE_TYPE}]}, {tag: RIN, value: AUTOMATED_RECORD_ID}, {tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE}]}, {id: N1, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FORM, value: MULTIMEDIA_FORMAT, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}]}]", origin.getSubmitters().get(0).toString());

		final Transformation<Gedcom, Flef> t = new SubmitterTransformation();
		t.to(origin, destination);

		Assertions.assertEquals("id: S1, tag: SOURCE, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORMAT, value: MULTIMEDIA_FORMAT}, {tag: MEDIA, value: SOURCE_MEDIA_TYPE}]}]", destination.getSubmitters().get(0).toString());
	}

}