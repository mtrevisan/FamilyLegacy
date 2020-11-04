package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class RepositoryRecordTransformationTest{

	@Test
	void to1(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("REPO")
					.withID("R1")
					.addChild(GedcomNode.create("NAME")
						.withValue("NAME_OF_REPOSITORY"))
					.addChild(GedcomNode.create("ADDR")
						.withValue("ADDRESS_LINE"))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))
					.addChild(GedcomNode.create("REFN")
						.withValue("USER_REFERENCE_NUMBER")
						.addChild(GedcomNode.create("TYPE")
							.withValue("USER_REFERENCE_TYPE")))
					.addChild(GedcomNode.create("RIN")
						.withValue("AUTOMATED_RECORD_ID"))
					.addChild(GedcomNode.create("CHAN")
						.addChild(GedcomNode.create("DATE")
							.withValue("CHANGE_DATE")))));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: R1, tag: REPO, children: [{tag: NAME, value: NAME_OF_REPOSITORY}, {tag: ADDR, value: ADDRESS_LINE}, {id: N1, tag: NOTE}, {tag: REFN, value: USER_REFERENCE_NUMBER, children: [{tag: TYPE, value: USER_REFERENCE_TYPE}]}, {tag: RIN, value: AUTOMATED_RECORD_ID}, {tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE}]}]}]}]", root.toString());

		final Transformation t = new RepositoryRecordTransformation();
		t.to(extractSubStructure(root, "PARENT", "REPO"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: R1, tag: REPOSITORY, children: [{tag: NAME, value: NAME_OF_REPOSITORY}, {id: N1, tag: NOTE}, {tag: _REFN, value: USER_REFERENCE_NUMBER, children: [{tag: _TYPE, value: USER_REFERENCE_TYPE}]}, {tag: _RIN, value: AUTOMATED_RECORD_ID}, {tag: CHANGE, children: [{tag: DATE, value: CHANGE_DATE}]}, {id: P1, tag: PLACE}]}]}, {id: P1, tag: PLACE, children: [{tag: ADDRESS, value: ADDRESS_LINE}]}]", root.toString());
	}

	@Test
	void from1(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("REPOSITORY")
					.withID("R1")
					.addChild(GedcomNode.create("NAME")
						.withValue("NAME_OF_REPOSITORY"))
					.addChild(GedcomNode.create("PLACE")
						.withID("P1"))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))
					.addChild(GedcomNode.create("SUBMITTER")
						.withID("SUBM1"))
					.addChild(GedcomNode.create("RESTRICTION")
						.withID("RESTRICTION_NOTICE"))
					.addChild(GedcomNode.create("CHANGE")
						.addChild(GedcomNode.create("DATE")
							.withValue("CHANGE_DATE")))
					))
			.addChild(GedcomNode.create("PLACE")
				.withID("P1")
				.addChild(GedcomNode.create("NAME")
					.withValue("PLACE_NAME")));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: R1, tag: REPOSITORY, children: [{tag: NAME, value: NAME_OF_REPOSITORY}, {id: P1, tag: PLACE}, {id: N1, tag: NOTE}, {id: SUBM1, tag: SUBMITTER}, {id: RESTRICTION_NOTICE, tag: RESTRICTION}, {tag: CHANGE, children: [{tag: DATE, value: CHANGE_DATE}]}]}]}, {id: P1, tag: PLACE, children: [{tag: NAME, value: PLACE_NAME}]}]", root.toString());

		final Transformation t = new RepositoryRecordTransformation();
		t.from(extractSubStructure(root, "PARENT", "REPOSITORY"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: R1, tag: REPO, children: [{tag: NAME, value: NAME_OF_REPOSITORY}, {tag: ADDR, children: [{tag: NAME, value: PLACE_NAME}]}, {id: N1, tag: NOTE}, {id: SUBM1, tag: _SUBMITTER}, {id: RESTRICTION_NOTICE, tag: _RESTRICTION}, {tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE}]}]}]}, {id: P1, tag: PLACE, children: [{tag: NAME, value: PLACE_NAME}]}]", root.toString());
	}

}