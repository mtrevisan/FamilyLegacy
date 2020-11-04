package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class ChildToFamilyLinkTransformationTest{

	@Test
	void to(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("FAMC")
					.withID("F1")
					.addChild(GedcomNode.create("PEDI")
						.withValue("PEDIGREE_LINKAGE_TYPE"))
					.addChild(GedcomNode.create("STAT")
						.withValue("CHILD_LINKAGE_STATUS"))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))
				))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1")
				.withValue("SUBMITTER_TEXT1"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: F1, tag: FAMC, children: [{tag: PEDI, value: PEDIGREE_LINKAGE_TYPE}, {tag: STAT, value: CHILD_LINKAGE_STATUS}, {id: N1, tag: NOTE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT1}]", root.toString());

		final Transformation t = new ChildToFamilyLinkTransformation();
		t.to(extractSubStructure(root, "PARENT", "FAMC"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: F1, tag: FAMILY_CHILD, children: [{tag: PEDIGREE, value: PEDIGREE_LINKAGE_TYPE}, {tag: STATUS, value: CHILD_LINKAGE_STATUS}, {id: N1, tag: NOTE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT1}]", root.toString());
	}


	@Test
	void from(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("FAMILY_CHILD")
					.withID("F1")
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1")
				.withValue("SUBMITTER_TEXT"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: F1, tag: FAMILY_CHILD, children: [{id: N1, tag: NOTE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());

		final Transformation t = new ChildToFamilyLinkTransformation();
		t.from(extractSubStructure(root, "PARENT", "FAMILY_CHILD"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: F1, tag: FAMC, children: [{id: N1, tag: NOTE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());
	}

}