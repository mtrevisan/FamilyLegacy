package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class SubmitterRecordTransformationTest{

	@Test
	void to(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("SUBM")
					.withID("SUBM1")
					.addChild(GedcomNode.create("NAME")
						.withValue("SUBMITTER_NAME"))
					.addChild(GedcomNode.create("ADDR")
						.withValue("ADDRESS_LINE"))
					.addChild(GedcomNode.create("OBJE")
						.withID("D1"))
					.addChild(GedcomNode.create("LANG")
						.withID("LANGUAGE_PREFERENCE1"))
					.addChild(GedcomNode.create("LANG")
						.withID("LANGUAGE_PREFERENCE2"))
					.addChild(GedcomNode.create("LANG")
						.withID("LANGUAGE_PREFERENCE3"))
					.addChild(GedcomNode.create("RFN")
						.withID("SUBMITTER_REGISTERED_RFN"))
					.addChild(GedcomNode.create("RIN")
						.withID("AUTOMATED_RECORD_ID"))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))
					.addChild(GedcomNode.create("CHAN")
						.addChild(GedcomNode.create("DATE")
							.withValue("CHANGE_DATE")))
				)
			);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: SUBM1, tag: SUBM, children: [{tag: NAME, value: SUBMITTER_NAME}, {tag: ADDR, value: ADDRESS_LINE}, {id: D1, tag: OBJE}, {id: LANGUAGE_PREFERENCE1, tag: LANG}, {id: LANGUAGE_PREFERENCE2, tag: LANG}, {id: LANGUAGE_PREFERENCE3, tag: LANG}, {id: SUBMITTER_REGISTERED_RFN, tag: RFN}, {id: AUTOMATED_RECORD_ID, tag: RIN}, {id: N1, tag: NOTE}, {tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE}]}]}]}]", root.toString());

		final Transformation t = new SubmitterRecordTransformation();
		t.to(extractSubStructure(root, "PARENT", "SUBM"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: SUBM1, tag: SUBMITTER, children: [{tag: NAME, value: SUBMITTER_NAME}, {id: D1, tag: DOCUMENT}, {id: LANGUAGE_PREFERENCE1, tag: LANGUAGE}, {id: LANGUAGE_PREFERENCE2, tag: LANGUAGE}, {id: LANGUAGE_PREFERENCE3, tag: LANGUAGE}, {id: SUBMITTER_REGISTERED_RFN, tag: _RFN}, {id: AUTOMATED_RECORD_ID, tag: _RIN}, {id: N1, tag: NOTE}, {tag: CHANGE, children: [{tag: DATE, value: CHANGE_DATE}]}, {id: P1, tag: PLACE}]}]}, {id: P1, tag: PLACE, children: [{tag: ADDRESS, value: ADDRESS_LINE}]}]", root.toString());
	}


	@Test
	void from(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("SUBMITTER")
					.withID("SUBM1")
					.addChild(GedcomNode.create("NAME")
						.withValue("SUBMITTER_NAME"))
					.addChild(GedcomNode.create("PLACE")
						.withID("P1"))
					.addChild(GedcomNode.create("DOCUMENT")
						.withID("D1"))
					.addChild(GedcomNode.create("LANGUAGE")
						.withValue("LANGUAGE_PREFERENCE1"))
					.addChild(GedcomNode.create("LANGUAGE")
						.withValue("LANGUAGE_PREFERENCE2"))
					.addChild(GedcomNode.create("LANGUAGE")
						.withValue("LANGUAGE_PREFERENCE3"))
					.addChild(GedcomNode.create("LANGUAGE")
						.withValue("LANGUAGE_PREFERENCE4"))
					.addChild(GedcomNode.create("INDIVIDUAL")
						.withValue("I1"))
					.addChild(GedcomNode.create("NOTE")
						.withValue("N1"))
					.addChild(GedcomNode.create("CHANGE")
						.addChild(GedcomNode.create("DATE")
							.withValue("CHANGE_DATE")
							.addChild(GedcomNode.create("TIME")
								.withValue("TIME_VALUE")))))
			)
			.addChild(GedcomNode.create("PLACE")
				.withID("P1")
				.addChild(GedcomNode.create("NAME")
					.withValue("PLACE_NAME")));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: SUBM1, tag: SUBMITTER, children: [{tag: NAME, value: SUBMITTER_NAME}, {id: P1, tag: PLACE}, {id: D1, tag: DOCUMENT}, {tag: LANGUAGE, value: LANGUAGE_PREFERENCE1}, {tag: LANGUAGE, value: LANGUAGE_PREFERENCE2}, {tag: LANGUAGE, value: LANGUAGE_PREFERENCE3}, {tag: LANGUAGE, value: LANGUAGE_PREFERENCE4}, {tag: INDIVIDUAL, value: I1}, {tag: NOTE, value: N1}, {tag: CHANGE, children: [{tag: DATE, value: CHANGE_DATE, children: [{tag: TIME, value: TIME_VALUE}]}]}]}]}, {id: P1, tag: PLACE, children: [{tag: NAME, value: PLACE_NAME}]}]", root.toString());

		final Transformation t = new SubmitterRecordTransformation();
		t.from(extractSubStructure(root, "PARENT", "SUBMITTER"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: SUBM1, tag: SUBM, children: [{tag: NAME, value: SUBMITTER_NAME}, {tag: ADDR, children: [{tag: NAME, value: PLACE_NAME}]}, {id: D1, tag: DOCUMENT}, {tag: LANG, value: LANGUAGE_PREFERENCE1}, {tag: LANG, value: LANGUAGE_PREFERENCE2}, {tag: LANG, value: LANGUAGE_PREFERENCE3}, {tag: _LANGUAGE, value: LANGUAGE_PREFERENCE4}, {tag: _INDIVIDUAL, value: I1}, {tag: NOTE, value: N1}, {tag: CHANGE, children: [{tag: DATE, value: CHANGE_DATE, children: [{tag: TIME, value: TIME_VALUE}]}]}]}]}, {id: P1, tag: PLACE, children: [{tag: NAME, value: PLACE_NAME}]}]", root.toString());
	}

}