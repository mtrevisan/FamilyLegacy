package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


//TODO
class IndividualEventStructureTransformationTest{

	@Test
	void to(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("AGE")
					.withValue("AGE_AT_EVENT"))
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

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: AGE, value: AGE_AT_EVENT}, {tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}, {tag: DATE, value: DATE_VALUE}, {tag: PLAC, value: PLACE_NAME}, {tag: ADDR, value: ADDRESS_LINE}, {tag: AGNC, value: RESPONSIBLE_AGENCY}, {tag: RELI, value: RELIGIOUS_AFFILIATION}, {tag: CAUS, value: CAUSE_OF_EVENT}, {tag: RESN, value: RESTRICTION_NOTICE}, {id: N1, tag: NOTE}, {id: S1, tag: SOUR}, {id: D1, tag: OBJE}]}]", root.toString());

		final Transformation t = new IndividualEventDetailTransformation();
		t.to(extractSubStructure(root, "PARENT"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: _AGE, value: AGE_AT_EVENT}, {tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION, children: [{tag: DATE, value: DATE_VALUE}, {tag: AGENCY, value: RESPONSIBLE_AGENCY}, {tag: CAUSE, value: CAUSE_OF_EVENT}, {tag: RESTRICTION, value: RESTRICTION_NOTICE}]}, {id: N1, tag: NOTE}, {id: S1, tag: SOURCE}, {id: D1, tag: DOCUMENT}, {id: P1, tag: PLACE}]}, {id: P1, tag: PLACE, children: [{tag: ADDRESS, value: ADDRESS_LINE}]}]", root.toString());
	}

	@Test
	void from(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("TYPE")
					.withValue("EVENT_OR_FACT_CLASSIFICATION")
					.addChild(GedcomNode.create("DATE")
						.withValue("DATE_TIME_VALUE")
						.addChild(GedcomNode.create("CREDIBILITY")
							.withValue("CREDIBILITY_ASSESSMENT")))
					.addChild(GedcomNode.create("PLACE")
						.withID("P1")
						.addChild(GedcomNode.create("CREDIBILITY")
							.withValue("CREDIBILITY_ASSESSMENT")))
					.addChild(GedcomNode.create("AGENCY")
						.withValue("RESPONSIBLE_AGENCY"))
					.addChild(GedcomNode.create("CAUSE")
						.withValue("CAUSE_OF_EVENT")
						.addChild(GedcomNode.create("CREDIBILITY")
							.withValue("CREDIBILITY_ASSESSMENT")))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))
					.addChild(GedcomNode.create("SOURCE")
						.withID("S1"))
					.addChild(GedcomNode.create("CREDIBILITY")
						.withValue("CREDIBILITY_ASSESSMENT"))
					.addChild(GedcomNode.create("DOCUMENT")
						.withID("D1"))
					.addChild(GedcomNode.create("RESTRICTION")
						.withID("RESTRICTION_NOTICE"))
				)
			)
			.addChild(GedcomNode.create("NOTE")
				.withID("N1"))
			.addChild(GedcomNode.create("PLACE")
				.withID("P1"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION, children: [{tag: DATE, value: DATE_TIME_VALUE, children: [{tag: CREDIBILITY, value: CREDIBILITY_ASSESSMENT}]}, {id: P1, tag: PLACE, children: [{tag: CREDIBILITY, value: CREDIBILITY_ASSESSMENT}]}, {tag: AGENCY, value: RESPONSIBLE_AGENCY}, {tag: CAUSE, value: CAUSE_OF_EVENT, children: [{tag: CREDIBILITY, value: CREDIBILITY_ASSESSMENT}]}, {id: N1, tag: NOTE}, {id: S1, tag: SOURCE}, {tag: CREDIBILITY, value: CREDIBILITY_ASSESSMENT}, {id: D1, tag: DOCUMENT}, {id: RESTRICTION_NOTICE, tag: RESTRICTION}]}]}, {id: N1, tag: NOTE}, {id: P1, tag: PLACE}]", root.toString());

		final Transformation t = new IndividualEventDetailTransformation();
		t.from(extractSubStructure(root, "PARENT"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION, children: [{tag: ADDR, children: [{tag: _CREDIBILITY, value: CREDIBILITY_ASSESSMENT}]}, {id: N1, tag: NOTE}, {id: S1, tag: SOUR}, {tag: _CREDIBILITY, value: CREDIBILITY_ASSESSMENT}, {id: D1, tag: OBJE}]}, {tag: DATE, value: DATE_TIME_VALUE, children: [{tag: _CREDIBILITY, value: CREDIBILITY_ASSESSMENT}]}, {tag: AGNC, value: RESPONSIBLE_AGENCY}, {tag: CAUS, value: CAUSE_OF_EVENT, children: [{tag: _CREDIBILITY, value: CREDIBILITY_ASSESSMENT}]}, {id: RESTRICTION_NOTICE, tag: RESN}]}, {id: N1, tag: NOTE}, {id: P1, tag: PLACE}]", root.toString());
	}

}