package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class RecordTransformationTest{

	@Test
	void to(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("FAM")
					.withID("F1"))
				.addChild(GedcomNode.create("INDI")
					.withID("I1"))
				.addChild(GedcomNode.create("OBJE")
					.withID("D1"))
				.addChild(GedcomNode.create("NOTE")
					.withID("N1"))
				.addChild(GedcomNode.create("REPO")
					.withID("R1"))
				.addChild(GedcomNode.create("SOUR")
					.withID("S1"))
				.addChild(GedcomNode.create("SUBM")
					.withID("SUBM1"))
			);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: F1, tag: FAM}, {id: I1, tag: INDI}, {id: D1, tag: OBJE}, {id: N1, tag: NOTE}, {id: R1, tag: REPO}, {id: S1, tag: SOUR}, {id: SUBM1, tag: SUBM}]}]", root.toString());

		final Transformation t = new RecordTransformation();
		final GedcomNode parent = extractSubStructure(root, "PARENT");
		for(final GedcomNode child : parent.getChildren())
			t.to(child, root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: F1, tag: FAMILY}, {id: I1, tag: INDIVIDUAL}, {id: D1, tag: DOCUMENT}, {id: N1, tag: NOTE}, {id: R1, tag: REPOSITORY}, {id: S1, tag: SOURCE}, {id: SUBM1, tag: SUBMITTER}]}]", root.toString());
	}

	@Test
	void from(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("INDIVIDUAL")
					.withID("I1"))
				.addChild(GedcomNode.create("FAMILY")
					.withID("F1"))
				.addChild(GedcomNode.create("PLACE")
					.withID("P1"))
				.addChild(GedcomNode.create("DOCUMENT")
					.withID("D1"))
				.addChild(GedcomNode.create("NOTE")
					.withID("N1"))
				.addChild(GedcomNode.create("REPOSITORY")
					.withID("R1"))
				.addChild(GedcomNode.create("SOURCE")
					.withID("S1"))
				.addChild(GedcomNode.create("SUBMITTER")
					.withID("SUBM1"))
			)
			.addChild(GedcomNode.create("PLACE")
				.withID("P1")
				.addChild(GedcomNode.create("NAME")
					.withValue("PLACE_NAME"))
			);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: I1, tag: INDIVIDUAL}, {id: F1, tag: FAMILY}, {id: P1, tag: PLACE}, {id: D1, tag: DOCUMENT}, {id: N1, tag: NOTE}, {id: R1, tag: REPOSITORY}, {id: S1, tag: SOURCE}, {id: SUBM1, tag: SUBMITTER}]}, {id: P1, tag: PLACE, children: [{tag: NAME, value: PLACE_NAME}]}]", root.toString());

		final Transformation t = new RecordTransformation();
		final GedcomNode parent = extractSubStructure(root, "PARENT");
		for(final GedcomNode child : parent.getChildren())
			t.from(child, root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: I1, tag: INDI}, {id: F1, tag: FAM}, {id: P1, tag: PLACE}, {id: D1, tag: OBJE}, {id: N1, tag: NOTE}, {id: R1, tag: REPO}, {id: S1, tag: SOUR}, {id: SUBM1, tag: SUBM}]}, {id: P1, tag: PLACE, children: [{tag: NAME, value: PLACE_NAME}]}]", root.toString());
	}

}