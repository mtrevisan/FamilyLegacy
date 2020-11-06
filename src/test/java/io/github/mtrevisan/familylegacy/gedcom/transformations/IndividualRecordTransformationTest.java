package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class IndividualRecordTransformationTest{

	@Test
	void to(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("INDI")
					.withID("I1")
					.addChild(GedcomNode.create("RESN")
						.withValue("RESTRICTION_NOTICE"))
					.addChild(GedcomNode.create("NAME")
						.withValue("NAME_PERSONAL"))
					.addChild(GedcomNode.create("SEX")
						.withValue("SEX_VALUE"))
					.addChild(GedcomNode.create("BIRT"))
					.addChild(GedcomNode.create("DEAT"))
					.addChild(GedcomNode.create("NATI"))
					.addChild(GedcomNode.create("NCHI"))
					.addChild(GedcomNode.create("BAPL"))
					.addChild(GedcomNode.create("ENDL"))
					.addChild(GedcomNode.create("FAMC")
						.withID("F1"))
					.addChild(GedcomNode.create("FAMC")
						.withID("F2"))
					.addChild(GedcomNode.create("FAMS")
						.withID("F3"))
					.addChild(GedcomNode.create("FAMS")
						.withID("F4"))
					.addChild(GedcomNode.create("SUBM")
						.withID("SUBM1"))
					.addChild(GedcomNode.create("SUBM")
						.withID("SUBM2"))
					.addChild(GedcomNode.create("ASSO")
						.withID("ASS1"))
					.addChild(GedcomNode.create("ASSO")
						.withID("ASS2"))
					.addChild(GedcomNode.create("ALIA")
						.withID("ALI1"))
					.addChild(GedcomNode.create("ALIA")
						.withID("ALI2"))
					.addChild(GedcomNode.create("ANCI")
						.withID("SUBM1"))
					.addChild(GedcomNode.create("ANCI")
						.withID("SUBM2"))
					.addChild(GedcomNode.create("DESI")
						.withID("SUBM1"))
					.addChild(GedcomNode.create("DESI")
						.withID("SUBM2"))
					.addChild(GedcomNode.create("RFN")
						.withValue("PERMANENT_RECORD_FILE_NUMBER"))
					.addChild(GedcomNode.create("AFN")
						.withValue("ANCESTRAL_FILE_NUMBER"))
					.addChild(GedcomNode.create("REFN")
						.withValue("USER_REFERENCE_NUMBER1")
						.addChild(GedcomNode.create("TYPE")
							.withValue("USER_REFERENCE_TYPE1")))
					.addChild(GedcomNode.create("REFN")
						.withValue("USER_REFERENCE_NUMBER2")
						.addChild(GedcomNode.create("TYPE")
							.withValue("USER_REFERENCE_TYPE2")))
					.addChild(GedcomNode.create("RIN")
						.withID("AUTOMATED_RECORD_ID"))
					.addChild(GedcomNode.create("CHAN")
						.addChild(GedcomNode.create("DATE")
							.withValue("CHANGE_DATE")))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))
					.addChild(GedcomNode.create("SOUR")
						.withID("S1"))
					.addChild(GedcomNode.create("OBJE")
						.withID("D1")))
				.addChild(GedcomNode.create("NOTE")
					.withID("N1")
					.withValue("SUBMITTER_TEXT")));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: I1, tag: INDI, children: [{tag: RESN, value: RESTRICTION_NOTICE}, {tag: NAME, value: NAME_PERSONAL}, {tag: SEX, value: SEX_VALUE}, {tag: BIRT}, {tag: DEAT}, {tag: NATI}, {tag: NCHI}, {tag: BAPL}, {tag: ENDL}, {id: F1, tag: FAMC}, {id: F2, tag: FAMC}, {id: F3, tag: FAMS}, {id: F4, tag: FAMS}, {id: SUBM1, tag: SUBM}, {id: SUBM2, tag: SUBM}, {id: ASS1, tag: ASSO}, {id: ASS2, tag: ASSO}, {id: ALI1, tag: ALIA}, {id: ALI2, tag: ALIA}, {id: SUBM1, tag: ANCI}, {id: SUBM2, tag: ANCI}, {id: SUBM1, tag: DESI}, {id: SUBM2, tag: DESI}, {tag: RFN, value: PERMANENT_RECORD_FILE_NUMBER}, {tag: AFN, value: ANCESTRAL_FILE_NUMBER}, {tag: REFN, value: USER_REFERENCE_NUMBER1, children: [{tag: TYPE, value: USER_REFERENCE_TYPE1}]}, {tag: REFN, value: USER_REFERENCE_NUMBER2, children: [{tag: TYPE, value: USER_REFERENCE_TYPE2}]}, {id: AUTOMATED_RECORD_ID, tag: RIN}, {tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE}]}, {id: N1, tag: NOTE}, {id: S1, tag: SOUR}, {id: D1, tag: OBJE}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]}]", root.toString());

		final Transformation t = new IndividualRecordTransformation();
		t.to(extractSubStructure(root, "PARENT", "INDI"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: I1, tag: INDIVIDUAL, children: [{tag: RESTRICTION, value: RESTRICTION_NOTICE}, {tag: NAME, children: [{tag: PERSONAL_NAME, value: NAME_PERSONAL}]}, {tag: SEX, value: SEX_VALUE}, {tag: EVENT, value: BIRTH}, {tag: EVENT, value: DEATH}, {tag: ATTRIBUTE, value: ORIGIN}, {tag: ATTRIBUTE, value: CHILDREN_COUNT}, {id: F1, tag: FAMILY_CHILD}, {id: F2, tag: FAMILY_CHILD}, {id: F3, tag: FAMILY_SPOUSE}, {id: F4, tag: FAMILY_SPOUSE}, {id: SUBM1, tag: SUBM}, {id: SUBM2, tag: SUBM}, {id: ASS1, tag: ASSOCIATION, children: [{tag: TYPE, value: INDIVIDUAL}]}, {id: ASS2, tag: ASSOCIATION, children: [{tag: TYPE, value: INDIVIDUAL}]}, {id: ALI1, tag: ALIAS}, {id: ALI2, tag: ALIAS}, {tag: SUBMITTER, value: USER_REFERENCE_NUMBER1, children: [{tag: TYPE, value: USER_REFERENCE_TYPE1}]}, {tag: SUBMITTER, value: USER_REFERENCE_NUMBER2, children: [{tag: TYPE, value: USER_REFERENCE_TYPE2}]}, {id: AUTOMATED_RECORD_ID, tag: _RIN}, {tag: CHANGE, children: [{tag: DATE, value: CHANGE_DATE}]}, {id: N1, tag: NOTE}, {id: S1, tag: SOURCE}, {id: D1, tag: DOCUMENT}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]}]", root.toString());
	}

	@Test
	void from(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("INDIVIDUAL")
					.withID("I1")
					.addChild(GedcomNode.create("NAME")
						.addChild(GedcomNode.create("PERSONAL_NAME")
							.withValue("NAME_PIECE")))
					.addChild(GedcomNode.create("NAME")
						.withValue("NAME_PERSONAL"))
					.addChild(GedcomNode.create("SEX")
						.withValue("SEX_VALUE"))
					.addChild(GedcomNode.create("BIRT"))
					.addChild(GedcomNode.create("DEAT"))
					.addChild(GedcomNode.create("NATI"))
					.addChild(GedcomNode.create("NCHI"))
					.addChild(GedcomNode.create("BAPL"))
					.addChild(GedcomNode.create("ENDL"))
					.addChild(GedcomNode.create("FAMC")
						.withID("F1"))
					.addChild(GedcomNode.create("FAMC")
						.withID("F2"))
					.addChild(GedcomNode.create("FAMS")
						.withID("F3"))
					.addChild(GedcomNode.create("FAMS")
						.withID("F4"))
					.addChild(GedcomNode.create("SUBM")
						.withID("SUBM1"))
					.addChild(GedcomNode.create("SUBM")
						.withID("SUBM2"))
					.addChild(GedcomNode.create("ASSO")
						.withID("ASS1"))
					.addChild(GedcomNode.create("ASSO")
						.withID("ASS2"))
					.addChild(GedcomNode.create("ALIA")
						.withID("ALI1"))
					.addChild(GedcomNode.create("ALIA")
						.withID("ALI2"))
					.addChild(GedcomNode.create("ANCI")
						.withID("SUBM1"))
					.addChild(GedcomNode.create("ANCI")
						.withID("SUBM2"))
					.addChild(GedcomNode.create("DESI")
						.withID("SUBM1"))
					.addChild(GedcomNode.create("DESI")
						.withID("SUBM2"))
					.addChild(GedcomNode.create("RFN")
						.withValue("PERMANENT_RECORD_FILE_NUMBER"))
					.addChild(GedcomNode.create("AFN")
						.withValue("ANCESTRAL_FILE_NUMBER"))
					.addChild(GedcomNode.create("REFN")
						.withValue("USER_REFERENCE_NUMBER1")
						.addChild(GedcomNode.create("TYPE")
							.withValue("USER_REFERENCE_TYPE1")))
					.addChild(GedcomNode.create("REFN")
						.withValue("USER_REFERENCE_NUMBER2")
						.addChild(GedcomNode.create("TYPE")
							.withValue("USER_REFERENCE_TYPE2")))
					.addChild(GedcomNode.create("RIN")
						.withID("AUTOMATED_RECORD_ID"))
					.addChild(GedcomNode.create("CHAN")
						.addChild(GedcomNode.create("DATE")
							.withValue("CHANGE_DATE")))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))
					.addChild(GedcomNode.create("SOUR")
						.withID("S1"))
					.addChild(GedcomNode.create("OBJE")
						.withID("D1")))
				.addChild(GedcomNode.create("NOTE")
					.withID("N1")
					.withValue("SUBMITTER_TEXT")));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: I1, tag: INDIVIDUAL, children: [{tag: NAME, children: [{tag: PERSONAL_NAME, value: NAME_PIECE}]}, {tag: NAME, value: NAME_PERSONAL}, {tag: SEX, value: SEX_VALUE}, {tag: BIRT}, {tag: DEAT}, {tag: NATI}, {tag: NCHI}, {tag: BAPL}, {tag: ENDL}, {id: F1, tag: FAMC}, {id: F2, tag: FAMC}, {id: F3, tag: FAMS}, {id: F4, tag: FAMS}, {id: SUBM1, tag: SUBM}, {id: SUBM2, tag: SUBM}, {id: ASS1, tag: ASSO}, {id: ASS2, tag: ASSO}, {id: ALI1, tag: ALIA}, {id: ALI2, tag: ALIA}, {id: SUBM1, tag: ANCI}, {id: SUBM2, tag: ANCI}, {id: SUBM1, tag: DESI}, {id: SUBM2, tag: DESI}, {tag: RFN, value: PERMANENT_RECORD_FILE_NUMBER}, {tag: AFN, value: ANCESTRAL_FILE_NUMBER}, {tag: REFN, value: USER_REFERENCE_NUMBER1, children: [{tag: TYPE, value: USER_REFERENCE_TYPE1}]}, {tag: REFN, value: USER_REFERENCE_NUMBER2, children: [{tag: TYPE, value: USER_REFERENCE_TYPE2}]}, {id: AUTOMATED_RECORD_ID, tag: RIN}, {tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE}]}, {id: N1, tag: NOTE}, {id: S1, tag: SOUR}, {id: D1, tag: OBJE}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]}]", root.toString());

		final Transformation t = new IndividualRecordTransformation();
		t.from(extractSubStructure(root, "PARENT", "INDIVIDUAL"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: I1, tag: INDI, children: [{tag: NAME, value: NAME_PIECE, children: [{tag: GIVN, value: NAME_PIECE}]}, {tag: NAME, value: NAME_PERSONAL}, {tag: SEX, value: SEX_VALUE}, {tag: BIRT}, {tag: DEAT}, {tag: NATI}, {tag: NCHI}, {tag: BAPL}, {tag: ENDL}, {id: F1, tag: FAMC}, {id: F2, tag: FAMC}, {id: F3, tag: FAMS}, {id: F4, tag: FAMS}, {id: SUBM1, tag: SUBM}, {id: SUBM2, tag: SUBM}, {id: ASS1, tag: ASSO}, {id: ASS2, tag: ASSO}, {id: ALI1, tag: ALIA}, {id: ALI2, tag: ALIA}, {id: SUBM1, tag: ANCI}, {id: SUBM2, tag: ANCI}, {id: SUBM1, tag: DESI}, {id: SUBM2, tag: DESI}, {tag: RFN, value: PERMANENT_RECORD_FILE_NUMBER}, {tag: AFN, value: ANCESTRAL_FILE_NUMBER}, {tag: REFN, value: USER_REFERENCE_NUMBER1, children: [{tag: TYPE, value: USER_REFERENCE_TYPE1}]}, {tag: REFN, value: USER_REFERENCE_NUMBER2, children: [{tag: TYPE, value: USER_REFERENCE_TYPE2}]}, {id: AUTOMATED_RECORD_ID, tag: RIN}, {tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE}]}, {id: N1, tag: NOTE}, {id: S1, tag: SOUR}, {id: D1, tag: OBJE}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]}]", root.toString());
	}

}