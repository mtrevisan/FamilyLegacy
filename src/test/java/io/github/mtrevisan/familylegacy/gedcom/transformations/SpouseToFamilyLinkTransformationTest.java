package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class SpouseToFamilyLinkTransformationTest{

	@Test
	void to(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("FAMS")
					.withID("F1")
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))
					.addChild(GedcomNode.create("NOTE")
						.withID("N2"))))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1")
				.withValue("SUBMITTER_TEXT1"))
			.addChild(GedcomNode.create("NOTE")
				.withID("N2")
				.withValue("SUBMITTER_TEXT2"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: F1, tag: FAMS, children: [{id: N1, tag: NOTE}, {id: N2, tag: NOTE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT1}, {id: N2, tag: NOTE, value: SUBMITTER_TEXT2}]", root.toString());

		final Transformation t = new SpouseToFamilyLinkTransformation();
		t.to(extractSubStructure(root, "PARENT", "FAMS"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: F1, tag: FAMILY_SPOUSE, children: [{id: N1, tag: NOTE}, {id: N2, tag: NOTE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT1}, {id: N2, tag: NOTE, value: SUBMITTER_TEXT2}]", root.toString());
	}


	@Test
	void from(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("FAMILY_SPOUSE")
					.withID("F1")
					.addChild(NoteStructureTransformationTest.composeNoteStructureFrom1())))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1")
				.withValue("SUBMITTER_TEXT"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: F1, tag: FAMILY_SPOUSE, children: [{id: N1, tag: NOTE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());

		final Transformation t = new SpouseToFamilyLinkTransformation();
		final GedcomNode individual = extractSubStructure(root, "INDIVIDUAL");
		final List<GedcomNode> names = individual.getChildrenWithTag("NAME");
		for(final GedcomNode name : names)
			t.from(name, root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: F1, tag: FAMILY_SPOUSE, children: [{id: N1, tag: NOTE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());
	}

}