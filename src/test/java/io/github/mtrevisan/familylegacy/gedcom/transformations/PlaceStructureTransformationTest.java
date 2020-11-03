package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class PlaceStructureTransformationTest{

	@Test
	void to(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("PLAC")
					.withValue("PLACE_NAME")
					.addChild(GedcomNode.create("FORM")
						.withValue("PLACE_HIERARCHY"))
					.addChild(GedcomNode.create("FONE")
						.withValue("PLACE_PHONETIC_VARIATION")
						.addChild(GedcomNode.create("TYPE")
							.withValue("PHONETIC_TYPE")))
					.addChild(GedcomNode.create("ROMN")
						.withValue("PLACE_ROMANIZED_VARIATION")
						.addChild(GedcomNode.create("TYPE")
							.withValue("ROMANIZED_TYPE")))
					.addChild(GedcomNode.create("MAP")
						.addChild(GedcomNode.create("LATI")
							.withValue("PLACE_LATITUDE"))
						.addChild(GedcomNode.create("LONG")
							.withValue("PLACE_LONGITUDE")))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1")))
				.addChild(GedcomNode.create("NOTE")
					.withID("N1")
					.withValue("SUBMITTER_TEXT")));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: PLAC, value: PLACE_NAME, children: [{tag: FORM, value: PLACE_HIERARCHY}, {tag: FONE, value: PLACE_PHONETIC_VARIATION, children: [{tag: TYPE, value: PHONETIC_TYPE}]}, {tag: ROMN, value: PLACE_ROMANIZED_VARIATION, children: [{tag: TYPE, value: ROMANIZED_TYPE}]}, {tag: MAP, children: [{tag: LATI, value: PLACE_LATITUDE}, {tag: LONG, value: PLACE_LONGITUDE}]}, {id: N1, tag: NOTE}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]}]", root.toString());

		final Transformation t = new PlaceStructureTransformation();
		t.to(extractSubStructure(root, "PARENT", "PLAC"), root);
		deleteTag(extractSubStructure(root, "PARENT"), "PLAC");

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: N1, tag: NOTE, value: SUBMITTER_TEXT}]}, {tag: !PLACE_STRUCTURE, children: [{tag: PLAC, value: PLACE_NAME, children: [{tag: PLACE_NAME, value: PLACE_HIERARCHY}, {tag: _FONE, value: PLACE_PHONETIC_VARIATION, children: [{tag: _TYPE, value: PHONETIC_TYPE}]}, {tag: _ROMN, value: PLACE_ROMANIZED_VARIATION, children: [{tag: _TYPE, value: ROMANIZED_TYPE}]}, {tag: MAP, children: [{tag: LATITUDE, value: PLACE_LATITUDE}, {tag: LONGITUDE, value: PLACE_LONGITUDE}]}, {id: N1, tag: NOTE}]}]}]", root.toString());
	}

}