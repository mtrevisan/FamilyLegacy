package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class ChangeDateTransformationTest{

	@Test
	void to(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("CHAN")
					.addChild(GedcomNode.create("DATE")
						.withValue("CHANGE_DATE")
						.addChild(GedcomNode.create("TIME")
							.withValue("TIME_VALUE")))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1")
				.withValue("SUBMITTER_TEXT"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE, children: [{tag: TIME, value: TIME_VALUE}]}, {id: N1, tag: NOTE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());

		final Transformation t = new ChangeDateTransformation();
		t.to(extractSubStructure(root, "PARENT", "CHAN"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: CHANGE, children: [{tag: DATE, value: CHANGE_DATE, children: [{tag: TIME, value: TIME_VALUE}]}, {id: N1, tag: NOTE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());
	}

	@Test
	void from(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("CHANGE")
					.addChild(GedcomNode.create("DATE")
						.withValue("CHANGE_DATE")
						.addChild(GedcomNode.create("TIME")
							.withValue("TIME_VALUE")))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1")
				.withValue("SUBMITTER_TEXT"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: CHANGE, children: [{tag: DATE, value: CHANGE_DATE, children: [{tag: TIME, value: TIME_VALUE}]}, {id: N1, tag: NOTE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());

		final Transformation t = new ChangeDateTransformation();
		t.from(extractSubStructure(root, "PARENT", "CHANGE"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE, children: [{tag: TIME, value: TIME_VALUE}]}, {id: N1, tag: NOTE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());
	}

}