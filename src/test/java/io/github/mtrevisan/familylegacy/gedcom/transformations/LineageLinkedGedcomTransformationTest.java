package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class LineageLinkedGedcomTransformationTest{

	@Test
	void to(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("HEAD")
				.addChild(GedcomNode.create("SOUR"))
				.addChild(GedcomNode.create("SUBM")
					.withID("SUBM1"))
				.addChild(GedcomNode.create("GEDC")
					.addChild(GedcomNode.create("VERS")
						.withValue("VERSION_NUMBER"))
					.addChild(GedcomNode.create("FORM")
						.withValue("GEDCOM_FORM"))
					)
				.addChild(GedcomNode.create("CHAR")
					.withID("CHARACTER_SET"))
			)
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
			.addChild(GedcomNode.create("TRLR"));

		Assertions.assertEquals("children: [{tag: HEAD, children: [{tag: SOUR}, {id: SUBM1, tag: SUBM}, {tag: GEDC, children: [{tag: VERS, value: VERSION_NUMBER}, {tag: FORM, value: GEDCOM_FORM}]}, {id: CHARACTER_SET, tag: CHAR}]}, {id: F1, tag: FAM}, {id: I1, tag: INDI}, {id: D1, tag: OBJE}, {id: N1, tag: NOTE}, {id: R1, tag: REPO}, {id: S1, tag: SOUR}, {id: SUBM1, tag: SUBM}, {tag: TRLR}]", root.toString());

		final Transformation t = new LineageLinkedGedcomTransformation();
		t.to(root, root);

		Assertions.assertEquals("children: [{tag: HEADER, children: [{tag: SOURCE}, {id: SUBM1, tag: SUBMITTER}, {id: CHARACTER_SET, tag: CHARSET}, {tag: PROTOCOL_VERSION, value: 0.0.1}]}, {id: F1, tag: FAMILY}, {id: I1, tag: INDIVIDUAL}, {id: D1, tag: DOCUMENT}, {id: N1, tag: NOTE}, {id: R1, tag: REPOSITORY}, {id: S1, tag: SOURCE}, {id: SUBM1, tag: SUBMITTER}, {tag: EOF}]", root.toString());
	}

	@Test
	void from(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("HEADER")
				.addChild(GedcomNode.create("SOURCE")
					.withValue("APPROVED_SYSTEM_ID"))
				.addChild(GedcomNode.create("SUBMITTER")
					.withID("SUBM1"))
				.addChild(GedcomNode.create("CHARSET")
					.withValue("UTF-8"))
			)
			.addChild(GedcomNode.create("INDIVIDUAL")
				.withID("I1"))
			.addChild(GedcomNode.create("FAMILY")
				.withID("F1"))
			.addChild(GedcomNode.create("PLACE")
				.withID("P1")
				.addChild(GedcomNode.create("NAME")
					.withValue("PLACE_NAME")))
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
			.addChild(GedcomNode.create("EOF"));

		Assertions.assertEquals("children: [{tag: HEADER, children: [{tag: SOURCE, value: APPROVED_SYSTEM_ID}, {id: SUBM1, tag: SUBMITTER}, {tag: CHARSET, value: UTF-8}]}, {id: I1, tag: INDIVIDUAL}, {id: F1, tag: FAMILY}, {id: P1, tag: PLACE, children: [{tag: NAME, value: PLACE_NAME}]}, {id: D1, tag: DOCUMENT}, {id: N1, tag: NOTE}, {id: R1, tag: REPOSITORY}, {id: S1, tag: SOURCE}, {id: SUBM1, tag: SUBMITTER}, {tag: EOF}]", root.toString());

		final Transformation t = new LineageLinkedGedcomTransformation();
		t.from(root, root);

		Assertions.assertEquals("children: [{tag: HEAD, children: [{tag: SOUR, value: APPROVED_SYSTEM_ID}, {id: SUBM1, tag: SUBM}, {tag: CHAR, value: UTF-8}, {tag: GEDC, children: [{tag: VERS, value: 5.5.1}, {tag: FORM, value: LINEAGE-LINKED}]}]}, {id: I1, tag: INDI}, {id: F1, tag: FAM}, {id: D1, tag: OBJE}, {id: N1, tag: NOTE}, {id: R1, tag: REPO}, {id: S1, tag: SOUR}, {id: SUBM1, tag: SUBM}, {tag: TRLR}]", root.toString());
	}

}