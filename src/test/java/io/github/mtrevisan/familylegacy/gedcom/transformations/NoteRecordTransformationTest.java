package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class NoteRecordTransformationTest{

	@Test
	void to1(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("NOTE")
					.withID("N1")
					.withValue("SUBMITTER_TEXT")
					.addChild(GedcomNode.create("CONC")
							.withValue("SUBMITTER_TEXT"))
					.addChild(GedcomNode.create("REFN")
						.withValue("USER_REFERENCE_NUMBER")
						.addChild(GedcomNode.create("TYPE")
							.withValue("USER_REFERENCE_TYPE")))
					.addChild(GedcomNode.create("RIN")
						.withValue("AUTOMATED_RECORD_ID"))
					.addChild(GedcomNode.create("SOUR")
						.withValue("S1"))
					.addChild(GedcomNode.create("CHAN")
						.addChild(GedcomNode.create("DATE")
							.withValue("CHANGE_DATE")))))
			.addChild(GedcomNode.create("SOUR")
				.withID("S1"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: N1, tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}, {tag: REFN, value: USER_REFERENCE_NUMBER, children: [{tag: TYPE, value: USER_REFERENCE_TYPE}]}, {tag: RIN, value: AUTOMATED_RECORD_ID}, {tag: SOUR, value: S1}, {tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE}]}]}]}, {id: S1, tag: SOUR}]", root.toString());

		final Transformation t = new NoteRecordTransformation();
		t.to(extractSubStructure(root, "PARENT", "NOTE"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: N1, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT, children: [{tag: SUBMITTER, value: USER_REFERENCE_NUMBER, children: [{tag: _TYPE, value: USER_REFERENCE_TYPE}]}, {tag: _RIN, value: AUTOMATED_RECORD_ID}, {tag: SOURCE, value: S1}, {tag: CHANGE, children: [{tag: DATE, value: CHANGE_DATE}]}]}]}, {id: S1, tag: SOUR}]", root.toString());
	}

	@Test
	void from1(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("NOTE")
					.withValue("SUBMITTER_TEXT")
					.addChild(GedcomNode.create("SOURCE")
						.withID("S1"))
						.addChild(GedcomNode.create("SUBMITTER")
							.withID("SUBM1"))
						.addChild(GedcomNode.create("RESTRICTION")
							.withValue("RESTRICTION_NOTICE"))
					.addChild(GedcomNode.create("CHANGE")
						.addChild(GedcomNode.create("DATE")
							.withValue("CHANGE_DATE")))
					))
			.addChild(GedcomNode.create("SOURCE")
				.withID("S1"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: NOTE, value: SUBMITTER_TEXT, children: [{id: S1, tag: SOURCE}, {id: SUBM1, tag: SUBMITTER}, {tag: RESTRICTION, value: RESTRICTION_NOTICE}, {tag: CHANGE, children: [{tag: DATE, value: CHANGE_DATE}]}]}]}, {id: S1, tag: SOURCE}]", root.toString());

		final Transformation t = new NoteRecordTransformation();
		t.from(extractSubStructure(root, "PARENT", "NOTE"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: NOTE, value: SUBMITTER_TEXT, children: [{id: S1, tag: SOUR}, {id: SUBM1, tag: REFN}, {tag: _RESTRICTION, value: RESTRICTION_NOTICE}, {tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE}]}]}]}, {id: S1, tag: SOURCE}]", root.toString());
	}

}