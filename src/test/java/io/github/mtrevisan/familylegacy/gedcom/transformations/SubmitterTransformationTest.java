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
			.addChildValue("ADDR", "ADDRESS_LINE")
			.addChildReference("OBJE", "D1")
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

		Assertions.assertEquals("id: SUBM1, tag: SUBM, children: [{tag: NAME, value: SUBMITTER_NAME}, {tag: ADDR, value: ADDRESS_LINE}, {id: D1, tag: OBJE}, {tag: LANG, value: LANGUAGE_PREFERENCE1}, {tag: LANG, value: LANGUAGE_PREFERENCE2}, {tag: RFN, value: SUBMITTER_REGISTERED_RFN}, {tag: RIN, value: AUTOMATED_RECORD_ID}, {id: N1, tag: NOTE}, {tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE}]}]", origin.getSubmitters().get(0).toString());

		final Transformation<Gedcom, Flef> t = new SubmitterTransformation();
		t.to(origin, destination);

		Assertions.assertEquals("id: SUBM1, tag: SOURCE, children: [{tag: TITLE, value: SUBMITTER_NAME}, {id: P1, tag: PLACE}, {id: D1, tag: SOURCE}, {id: N1, tag: NOTE}, {id: N1, tag: NOTE}]", destination.getSources().get(0).toString());
	}

}