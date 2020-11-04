package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class AssociationStructureTransformationTest{

	@Test
	void to(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("ASSO")
					.withID("I1")
					.addChild(GedcomNode.create("RELA")
						.withValue("RELATION_IS_DESCRIPTOR"))
					.addChild(GedcomNode.create("SOUR")
						.withID("S1"))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))
			))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1")
				.withValue("SUBMITTER_TEXT"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: I1, tag: ASSO, children: [{tag: RELA, value: RELATION_IS_DESCRIPTOR}, {id: S1, tag: SOUR}, {id: N1, tag: NOTE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());

		final Transformation t = new AssociationStructureTransformation();
		t.to(extractSubStructure(root, "PARENT", "ASSO"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: I1, tag: ASSOCIATION, children: [{tag: RELATIONSHIP, value: RELATION_IS_DESCRIPTOR}, {id: S1, tag: SOURCE}, {id: N1, tag: NOTE}, {tag: TYPE, value: INDIVIDUAL}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());
	}

	@Test
	void from1(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("ASSOCIATION")
					.withID("A1")
					.addChild(GedcomNode.create("TYPE")
						.withValue("INDIVIDUAL"))
					.addChild(GedcomNode.create("RELATIONSHIP")
						.withValue("RELATION_IS_DESCRIPTOR"))
					.addChild(GedcomNode.create("SOUR")
						.withID("S1"))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))
				))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1")
				.withValue("SUBMITTER_TEXT"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: A1, tag: ASSOCIATION, children: [{tag: TYPE, value: INDIVIDUAL}, {tag: RELATIONSHIP, value: RELATION_IS_DESCRIPTOR}, {id: S1, tag: SOUR}, {id: N1, tag: NOTE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());

		final Transformation t = new AssociationStructureTransformation();
		t.from(extractSubStructure(root, "PARENT", "ASSOCIATION"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: A1, tag: ASSO, children: [{tag: RELA, value: RELATION_IS_DESCRIPTOR}, {id: S1, tag: SOUR}, {id: N1, tag: NOTE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());
	}

	@Test
	void from2(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("ASSOCIATION")
					.withID("A1")
					.addChild(GedcomNode.create("TYPE")
						.withValue("ASSOCIATION_TYPE"))
					.addChild(GedcomNode.create("RELATIONSHIP")
						.withValue("RELATION_IS_DESCRIPTOR"))
					.addChild(GedcomNode.create("SOURCE")
						.withID("S1"))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))
				))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1")
				.withValue("SUBMITTER_TEXT"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: A1, tag: ASSOCIATION, children: [{tag: TYPE, value: ASSOCIATION_TYPE}, {tag: RELATIONSHIP, value: RELATION_IS_DESCRIPTOR}, {id: S1, tag: SOURCE}, {id: N1, tag: NOTE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());

		final Transformation t = new AssociationStructureTransformation();
		t.from(extractSubStructure(root, "PARENT", "ASSOCIATION"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: A1, tag: _ASSOCIATION, children: [{tag: _TYPE, value: ASSOCIATION_TYPE}, {tag: _RELATIONSHIP, value: RELATION_IS_DESCRIPTOR}, {id: S1, tag: _SOURCE}, {id: N1, tag: _NOTE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());
	}

}