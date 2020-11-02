package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class EndOfFileTransformationTest{

	@Test
	void toAndFrom(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("TRLR"));

		Assertions.assertEquals("children: [{tag: TRLR}]", root.toString());

		final Transformation t = new EndOfFileTransformation();
		t.to(extractSubStructure(root, "TRLR"), root);

		Assertions.assertEquals("children: [{tag: EOF}]", root.toString());

		t.from(extractSubStructure(root, "EOF"), root);

		Assertions.assertEquals("children: [{tag: TRLR}]", root.toString());
	}

}