package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class EventDetailTransformationTest{

	@Test
	void to(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("TYPE")
					.withValue("EVENT_OR_FACT_CLASSIFICATION"))
				.addChild(GedcomNode.create("DATE")
					.withValue("DATE_VALUE"))
				.addChild(GedcomNode.create("PLAC")
					.withValue("PLACE_NAME"))
				.addChild(GedcomNode.create("ADDR")
					.withValue("ADDRESS_LINE"))
				.addChild(GedcomNode.create("AGNC")
					.withValue("RESPONSIBLE_AGENCY"))
				.addChild(GedcomNode.create("RELI")
					.withValue("RELIGIOUS_AFFILIATION"))
				.addChild(GedcomNode.create("CAUS")
					.withValue("CAUSE_OF_EVENT"))
				.addChild(GedcomNode.create("RESN")
					.withValue("RESTRICTION_NOTICE"))
				.addChild(GedcomNode.create("NOTE")
					.withID("N1"))
				.addChild(GedcomNode.create("SOUR")
					.withID("S1"))
				.addChild(GedcomNode.create("OBJE")
					.withID("D1"))
			);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}, {tag: DATE, value: DATE_VALUE}, {tag: PLAC, value: PLACE_NAME}, {tag: ADDR, value: ADDRESS_LINE}, {tag: AGNC, value: RESPONSIBLE_AGENCY}, {tag: RELI, value: RELIGIOUS_AFFILIATION}, {tag: CAUS, value: CAUSE_OF_EVENT}, {tag: RESN, value: RESTRICTION_NOTICE}, {id: N1, tag: NOTE}, {id: S1, tag: SOUR}, {id: D1, tag: OBJE}]}]", root.toString());

		final Transformation t = new EventDetailTransformation();
		t.to(extractSubStructure(root, "PARENT", "NOTE"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: N1, tag: NOTE}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());
	}

	@Test
	void from(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("NOTE")
					.withID("N1")))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1")
				.withValue("SUBMITTER_TEXT"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: N1, tag: NOTE}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());

		final Transformation t = new NoteStructureTransformation();
		t.from(extractSubStructure(root, "PARENT", "NOTE"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: N1, tag: NOTE}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());
	}

}