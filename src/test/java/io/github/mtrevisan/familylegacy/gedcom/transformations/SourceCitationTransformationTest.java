package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class SourceCitationTransformationTest{

	@Test
	void to1(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("SOUR")
					.withID("S1")
					.addChild(GedcomNode.create("PAGE")
						.withValue("WHERE_WITHIN_SOURCE"))
					.addChild(GedcomNode.create("EVEN")
						.withValue("EVENT_TYPE_CITED_FROM")
						.addChild(GedcomNode.create("ROLE")
							.withValue("ROLE_IN_EVENT")))
					.addChild(GedcomNode.create("DATA")
						.addChild(GedcomNode.create("DATE")
							.withValue("ENTRY_RECORDING_DATE"))
						.addChild(GedcomNode.create("TEXT")
							.withValue("TEXT_FROM_SOURCE")
							.addChild(GedcomNode.create("CONC")
								.withValue("TEXT_FROM_SOURCE"))))
					.addChild(GedcomNode.create("OBJE")
						.withID("D1"))
					.addChild(GedcomNode.create("NOTE")
						.withID("N2"))
					.addChild(GedcomNode.create("QUAY")
						.withValue("CERTAINTY_ASSESSMENT"))));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: S1, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}]}, {id: D1, tag: OBJE}, {id: N2, tag: NOTE}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}]}]", root.toString());

		final Transformation t = new SourceCitationTransformation();
		t.to(extractSubStructure(root, "PARENT", "SOUR"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: S1, tag: SOURCE, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVENT, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {id: D1, tag: DOCUMENT}, {id: N2, tag: NOTE}, {tag: CREDIBILITY, value: CERTAINTY_ASSESSMENT}, {tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCETEXT_FROM_SOURCE}]}]}]", root.toString());
	}

	@Test
	void to2(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("SOUR")
					.withValue("SOURCE_DESCRIPTION")
					.addChild(GedcomNode.create("CONC")
						.withValue("SOURCE_DESCRIPTION"))
					.addChild(GedcomNode.create("TEXT")
						.withValue("TEXT_FROM_SOURCE")
						.addChild(GedcomNode.create("CONC")
							.withValue("TEXT_FROM_SOURCE")))
					.addChild(GedcomNode.create("OBJE")
						.withID("D1"))
					.addChild(GedcomNode.create("NOTE")
						.withID("N2"))
					.addChild(GedcomNode.create("QUAY")
						.withValue("CERTAINTY_ASSESSMENT"))));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: SOUR, value: SOURCE_DESCRIPTION, children: [{tag: CONC, value: SOURCE_DESCRIPTION}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}, {id: D1, tag: OBJE}, {id: N2, tag: NOTE}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}]}]", root.toString());

		final Transformation t = new SourceCitationTransformation();
		t.to(extractSubStructure(root, "PARENT", "SOUR"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: SOURCE, value: SOURCE_DESCRIPTIONSOURCE_DESCRIPTION, children: [{tag: TEXT, value: TEXT_FROM_SOURCETEXT_FROM_SOURCE}, {id: D1, tag: DOCUMENT}, {id: N2, tag: NOTE}, {tag: CREDIBILITY, value: CERTAINTY_ASSESSMENT}, {tag: TEXT, value: TEXT_FROM_SOURCETEXT_FROM_SOURCE}]}]}]", root.toString());
	}


	@Test
	void from(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("SOURCE")
					.withID("S1")
					.addChild(GedcomNode.create("EVENT")
						.withValue("EVENT_TYPE_CITED_FROM")
						.addChild(GedcomNode.create("ROLE")
							.withValue("ROLE_IN_EVENT")))
					.addChild(GedcomNode.create("DATE")
						.withValue("ENTRY_RECORDING_DATE"))
					.addChild(GedcomNode.create("TEXT")
						.withValue("TEXT_FROM_SOURCE"))
					.addChild(GedcomNode.create("PAGE")
						.withValue("WHERE_WITHIN_SOURCE"))
					.addChild(GedcomNode.create("DOCUMENT")
						.withValue("D1"))
					.addChild(GedcomNode.create("NOTE")
						.withValue("N1"))
					.addChild(GedcomNode.create("CREDIBILITY")
						.withValue("CREDIBILITY_ASSESSMENT"))));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: S1, tag: SOURCE, children: [{tag: EVENT, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE}, {tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: DOCUMENT, value: D1}, {tag: NOTE, value: N1}, {tag: CREDIBILITY, value: CREDIBILITY_ASSESSMENT}]}]}]", root.toString());

		final Transformation t = new SourceCitationTransformation();
		t.from(extractSubStructure(root, "PARENT", "SOURCE"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: S1, tag: SOUR, children: [{tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: OBJE, value: D1}, {tag: NOTE, value: N1}, {tag: QUAY, value: CREDIBILITY_ASSESSMENT}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE}]}]}]}]", root.toString());
	}

}