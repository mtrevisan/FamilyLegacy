package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class FamilyRecordTransformationTest{

	@Test
	void to(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("FAM")
					.withID("FAM1")
					.addChild(GedcomNode.create("RESN")
						.withValue("RESTRICTION_NOTICE"))
					.addChild(GedcomNode.create("MARR")
						.addChild(GedcomNode.create("TYPE")
							.withValue("EVENT_OR_FACT_CLASSIFICATION")))
					.addChild(GedcomNode.create("HUSB")
						.withValue("I1"))
					.addChild(GedcomNode.create("WIFE")
						.withValue("I2"))
					.addChild(GedcomNode.create("CHIL")
						.withValue("I3"))
					.addChild(GedcomNode.create("CHIL")
						.withValue("I4"))
					.addChild(GedcomNode.create("NCHIL")
						.withValue("2"))
					.addChild(GedcomNode.create("SUBM")
						.withID("SUBM1"))
					.addChild(GedcomNode.create("SLGS")
						.addChild(GedcomNode.create("DATE")
							.withValue("DATE_LDS_ORD")))
					.addChild(GedcomNode.create("REFN")
						.withValue("USER_REFERENCE_NUMBER")
						.addChild(GedcomNode.create("TYPE")
							.withValue("USER_REFERENCE_TYPE")))
					.addChild(GedcomNode.create("RIN")
						.withValue("AUTOMATED_RECORD_ID"))
					.addChild(GedcomNode.create("CHAN")
						.addChild(GedcomNode.create("DATE")
							.withValue("CHANGE_DATE")))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))
					.addChild(GedcomNode.create("SOUR")
						.withID("S1"))
					.addChild(GedcomNode.create("OBJE")
						.withID("D1")))
				.addChild(GedcomNode.create("NOTE")
					.withID("N1")
					.withValue("SUBMITTER_TEXT")));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: FAM1, tag: FAM, children: [{tag: RESN, value: RESTRICTION_NOTICE}, {tag: MARR, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: HUSB, value: I1}, {tag: WIFE, value: I2}, {tag: CHIL, value: I3}, {tag: CHIL, value: I4}, {tag: NCHIL, value: 2}, {id: SUBM1, tag: SUBM}, {tag: SLGS, children: [{tag: DATE, value: DATE_LDS_ORD}]}, {tag: REFN, value: USER_REFERENCE_NUMBER, children: [{tag: TYPE, value: USER_REFERENCE_TYPE}]}, {tag: RIN, value: AUTOMATED_RECORD_ID}, {tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE}]}, {id: N1, tag: NOTE}, {id: S1, tag: SOUR}, {id: D1, tag: OBJE}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]}]", root.toString());

		final Transformation t = new FamilyRecordTransformation();
		t.to(extractSubStructure(root, "PARENT", "FAM"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: FAM1, tag: FAMILY, children: [{tag: RESTRICTION, value: RESTRICTION_NOTICE}, {tag: EVENT, value: MARRIAGE, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: SPOUSE1, value: I1}, {tag: SPOUSE2, value: I2}, {tag: CHILD, value: I3}, {tag: CHILD, value: I4}, {tag: NCHIL, value: 2}, {id: SUBM1, tag: SUBMITTER}, {tag: SLGS, children: [{tag: DATE, value: DATE_LDS_ORD}]}, {tag: _REFN, value: USER_REFERENCE_NUMBER, children: [{tag: TYPE, value: USER_REFERENCE_TYPE}]}, {tag: _RIN, value: AUTOMATED_RECORD_ID}, {tag: CHANGE, children: [{tag: DATE, value: CHANGE_DATE}]}, {id: N1, tag: NOTE}, {id: S1, tag: SOURCE}, {id: D1, tag: DOCUMENT}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]}]", root.toString());
	}

	@Test
	void from(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("FAMILY")
					.withID("FAM1")
					.addChild(GedcomNode.create("SPOUSE1")
						.withValue("I1"))
					.addChild(GedcomNode.create("SPOUSE2")
						.withValue("I2"))
					.addChild(GedcomNode.create("CHILD")
						.withValue("I3"))
					.addChild(GedcomNode.create("CHILD")
						.withValue("I4"))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))
					.addChild(GedcomNode.create("SOURCE")
						.withID("S1"))
					.addChild(GedcomNode.create("DOCUMENT")
						.withID("D1"))
					.addChild(GedcomNode.create("EVENT")
						.withValue("MARRIAGE")
						.addChild(GedcomNode.create("TYPE")
							.withValue("EVENT_OR_FACT_CLASSIFICATION")))
					.addChild(GedcomNode.create("SUBMITTER")
						.withID("SUBM1"))
					.addChild(GedcomNode.create("RESTRICTION")
						.withValue("RESTRICTION_NOTICE"))
					.addChild(GedcomNode.create("CHANGE")
						.addChild(GedcomNode.create("DATE")
							.withValue("CHANGE_DATE"))))
				.addChild(GedcomNode.create("NOTE")
					.withID("N1")
					.withValue("SUBMITTER_TEXT")));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: FAM1, tag: FAMILY, children: [{tag: SPOUSE1, value: I1}, {tag: SPOUSE2, value: I2}, {tag: CHILD, value: I3}, {tag: CHILD, value: I4}, {id: N1, tag: NOTE}, {id: S1, tag: SOURCE}, {id: D1, tag: DOCUMENT}, {tag: EVENT, value: MARRIAGE, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {id: SUBM1, tag: SUBMITTER}, {tag: RESTRICTION, value: RESTRICTION_NOTICE}, {tag: CHANGE, children: [{tag: DATE, value: CHANGE_DATE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]}]", root.toString());

		final Transformation t = new FamilyRecordTransformation();
		t.from(extractSubStructure(root, "PARENT", "FAMILY"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: FAM1, tag: FAM, children: [{tag: HUSB, value: I1}, {tag: WIFE, value: I2}, {tag: CHIL, value: I3}, {tag: CHIL, value: I4}, {id: N1, tag: NOTE}, {id: S1, tag: SOUR}, {id: D1, tag: OBJE}, {tag: MARR, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {id: SUBM1, tag: SUBM}, {tag: RESN, value: RESTRICTION_NOTICE}, {tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]}]", root.toString());
	}

}