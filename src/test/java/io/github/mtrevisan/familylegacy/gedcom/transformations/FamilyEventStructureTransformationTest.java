package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class FamilyEventStructureTransformationTest{

	@Test
	void to(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("BIRT")
					.withValue("Y")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION"))
					.addChild(GedcomNode.create("FAMC")
						.withID("F1")))
				.addChild(GedcomNode.create("CHR")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION"))
					.addChild(GedcomNode.create("FAMC")
						.withID("F1")))
				.addChild(GedcomNode.create("DEAT")
					.withValue("Y")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("BURI")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("CREM")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("ADOP")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
					.addChild(GedcomNode.create("FAMC")
						.withID("F1")
						.addChild(GedcomNode.create("ADOP")
							.withValue("ADOPTED_BY_WHICH_PARENT")))
				.addChild(GedcomNode.create("BAPM")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("BARM")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("BASM")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("BLES")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("CHRA")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("CONF")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("FCOM")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("ORDN")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("NATU")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("EMIG")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("IMMI")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("CENS")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("PROB")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("WILL")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("GRAD")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("RETI")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("EVEN")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
			);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: BIRT, value: Y, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}, {id: F1, tag: FAMC}]}, {tag: CHR, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}, {id: F1, tag: FAMC}]}, {tag: DEAT, value: Y, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: BURI, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: CREM, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: ADOP, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {id: F1, tag: FAMC, children: [{tag: ADOP, value: ADOPTED_BY_WHICH_PARENT}]}, {tag: BAPM, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: BARM, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: BASM, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: BLES, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: CHRA, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: CONF, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: FCOM, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: ORDN, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: NATU, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EMIG, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: IMMI, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: CENS, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: PROB, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: WILL, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: GRAD, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: RETI, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVEN, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}]}]", root.toString());

		final Transformation t = new IndividualEventStructureTransformation();
		t.to(extractSubStructure(root, "PARENT"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: EVENT, value: BIRTH, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}, {id: F1, tag: FAMILY_CHILD}]}, {tag: EVENT, value: _CHR, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}, {id: F1, tag: FAMILY_CHILD}]}, {tag: EVENT, value: DEATH, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: BURIAL, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: CREMATION, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: ADOPTION, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {id: F1, tag: FAMC, children: [{tag: ADOP, value: ADOPTED_BY_WHICH_PARENT}]}, {tag: EVENT, value: _BAPM, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: _BARM, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: _BASM, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: _BLES, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: _CHRA, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: _CONF, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: _FCOM, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: _ORDNM, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: NATURALIZATION, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: EMIGRATION, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: IMMIGRATION, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: CENSUS, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: PROBATE, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: WILL, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: GRADUATION, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: RETIREMENT, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}]}]", root.toString());
	}

	@Test
	void from(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("EVENT")
					.withValue("BIRTH")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION"))
					.addChild(GedcomNode.create("FAMILY_CHILD")
						.withID("F1")))
				.addChild(GedcomNode.create("EVENT")
					.withValue("ADOPTION")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION"))
					.addChild(GedcomNode.create("FAMILY_CHILD")
						.withID("F1")
						.addChild(GedcomNode.create("ADOPTEE")
							.withValue("ADOPTED_BY_WHICH_PARENT"))))
				.addChild(GedcomNode.create("EVENT")
					.withValue("DEATH")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("EVENT")
					.withValue("BURIAL")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("EVENT")
					.withValue("CREMATION")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("EVENT")
					.withValue("NATURALIZATION")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("EVENT")
					.withValue("EMIGRATION")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("EVENT")
					.withValue("IMMIGRATION")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("EVENT")
					.withValue("CENSUS")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("EVENT")
					.withValue("PROBATE")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("EVENT")
					.withValue("WILL")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("EVENT")
					.withValue("GRADUATION")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("EVENT")
					.withValue("RETIREMENT")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("EVENT")
					.withValue("EVENT_DESCRIPTOR")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
				.addChild(GedcomNode.create("EVENT")
					.addChild(GedcomNode.create("TYPE")
						.withValue("EVENT_OR_FACT_CLASSIFICATION")))
			);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: EVENT, value: BIRTH, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}, {id: F1, tag: FAMILY_CHILD}]}, {tag: EVENT, value: ADOPTION, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}, {id: F1, tag: FAMILY_CHILD, children: [{tag: ADOPTEE, value: ADOPTED_BY_WHICH_PARENT}]}]}, {tag: EVENT, value: DEATH, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: BURIAL, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: CREMATION, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: NATURALIZATION, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: EMIGRATION, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: IMMIGRATION, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: CENSUS, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: PROBATE, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: WILL, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: GRADUATION, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: RETIREMENT, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, value: EVENT_DESCRIPTOR, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVENT, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}]}]", root.toString());

		final Transformation t = new IndividualEventStructureTransformation();
		t.from(extractSubStructure(root, "PARENT"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: BIRT, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}, {id: F1, tag: FAMILY_CHILD}]}, {tag: ADOP, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}, {id: F1, tag: FAMC, children: [{tag: ADOPTEE, value: ADOPTED_BY_WHICH_PARENT}]}]}, {tag: DEAT, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: BURI, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: CREM, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: NATU, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EMIG, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: IMMI, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: CENS, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: PROB, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: WILL, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: GRAD, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: RETI, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVEN, value: EVENT_DESCRIPTOR, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}, {tag: EVEN, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}]}]}]", root.toString());
	}

}