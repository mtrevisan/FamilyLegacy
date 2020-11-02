package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class NoteStructureTransformationTest{

	@Test
	void to1(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(composeNoteStructureTo1()))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1")
				.withValue("SUBMITTER_TEXT"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: N1, tag: NOTE}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());

		final Transformation t = new NoteStructureTransformation();
		t.to(extractSubStructure(root, "PARENT", "NOTE"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: N1, tag: NOTE}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());
	}

	@Test
	void from1(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(composeNoteStructureFrom1()))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1")
				.withValue("SUBMITTER_TEXT"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: N1, tag: NOTE}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());

		final Transformation t = new NoteStructureTransformation();
		t.from(extractSubStructure(root, "PARENT", "NOTE"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: N1, tag: NOTE}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());
	}

	static GedcomNode composeNoteStructureTo1(){
		return GedcomNode.create("NOTE")
			.withID("N1");
	}

	static GedcomNode composeNoteStructureFrom1(){
		return GedcomNode.create("NOTE")
			.withID("N1");
	}

	@Test
	void to2(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(composeNoteStructureTo2()));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: NOTE, children: [{tag: CONC, value: SUBMITTER_TEXT}]}]}]", root.toString());

		final Transformation t = new NoteStructureTransformation();
		t.to(extractSubStructure(root, "PARENT", "NOTE"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: N1, tag: NOTE}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());
	}

	@Test
	void from2(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(composeNoteStructureFrom2()));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: NOTE, value: SUBMITTER_TEXT}]}]", root.toString());

		final Transformation t = new NoteStructureTransformation();
		t.to(extractSubStructure(root, "PARENT", "NOTE"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: N1, tag: NOTE}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());
	}

	static GedcomNode composeNoteStructureTo2(){
		return GedcomNode.create("NOTE")
			.addChild(GedcomNode.create("CONC")
				.withValue("SUBMITTER_TEXT"));
	}

	static GedcomNode composeNoteStructureFrom2(){
		return GedcomNode.create("NOTE")
			.withValue("SUBMITTER_TEXT");
	}

	@Test
	void to3(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(composeNoteStructureTo3()));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}]}]", root.toString());

		final Transformation t = new NoteStructureTransformation();
		t.to(extractSubStructure(root, "PARENT", "NOTE"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: N1, tag: NOTE}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT}]", root.toString());
	}

	@Test
	void from3(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(composeNoteStructureFrom3()));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: NOTE, value: SUBMITTER_TEXT}]}]", root.toString());

		final Transformation t = new NoteStructureTransformation();
		t.to(extractSubStructure(root, "PARENT", "NOTE"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: N1, tag: NOTE}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());
	}

	static GedcomNode composeNoteStructureTo3(){
		return GedcomNode.create("NOTE")
			.withValue("SUBMITTER_TEXT")
			.addChild(GedcomNode.create("CONC")
				.withValue("SUBMITTER_TEXT"));
	}

	static GedcomNode composeNoteStructureFrom3(){
		return GedcomNode.create("NOTE")
			.withValue("SUBMITTER_TEXT");
	}

}