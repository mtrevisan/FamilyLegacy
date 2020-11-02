package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class MultimediaLinkTransformationTest{

	@Test
	void to1(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("OBJE")
					.withID("D1")
				));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: D1, tag: OBJE}]}]", root.toString());

		final Transformation t = new MultimediaLinkTransformation();
		t.to(extractSubStructure(root, "PARENT", "OBJE"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: D1, tag: DOCUMENT}]}]", root.toString());
	}

	@Test
	void to2(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("OBJE")
					.addChild(GedcomNode.create("TITL")
						.withValue("DESCRIPTIVE_TITLE"))
					.addChild(GedcomNode.create("FORM")
						.withValue("MULTIMEDIA_FORMAT")
						.addChild(GedcomNode.create("MEDI")
							.withValue("SOURCE_MEDIA_TYPE")))
					.addChild(GedcomNode.create("FILE")
						.withValue("MULTIMEDIA_FILE_REFN"))));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FORM, value: MULTIMEDIA_FORMAT, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}]}]}]", root.toString());

		final Transformation t = new MultimediaLinkTransformation();
		t.to(extractSubStructure(root, "PARENT", "OBJE"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: D1, tag: DOCUMENT}]}, {id: D1, tag: DOCUMENT, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORMAT, value: MULTIMEDIA_FORMAT, children: [{tag: _MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}]", root.toString());
	}

	@Test
	void from(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("DOCUMENT")
					.withID("D1")
				));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: D1, tag: DOCUMENT}]}]", root.toString());

		final Transformation t = new MultimediaLinkTransformation();
		t.from(extractSubStructure(root, "PARENT", "DOCUMENT"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: D1, tag: OBJE}]}]", root.toString());
	}

}