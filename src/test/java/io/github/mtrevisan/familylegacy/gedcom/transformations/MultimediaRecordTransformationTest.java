package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class MultimediaRecordTransformationTest{

	@Test
	void to(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("OBJE")
					.withID("D1")
					.addChild(GedcomNode.create("FILE")
						.withValue("MULTIMEDIA_FILE_REFN")
						.addChild(GedcomNode.create("FORM")
							.withValue("MULTIMEDIA_FORMAT")
							.addChild(GedcomNode.create("TYPE")
								.withValue("SOURCE_MEDIA_TYPE")))
						.addChild(GedcomNode.create("TITL")
							.withValue("DESCRIPTIVE_TITLE")))
					.addChild(GedcomNode.create("REFN")
						.withValue("USER_REFERENCE_NUMBER")
						.addChild(GedcomNode.create("TYPE")
							.withValue("USER_REFERENCE_TYPE")))
					.addChild(GedcomNode.create("RIN")
						.withValue("AUTOMATED_RECORD_ID"))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))
					.addChild(GedcomNode.create("SOUR")
						.withID("S1"))
					.addChild(GedcomNode.create("CHAN")
						.addChild(GedcomNode.create("DATE")
							.withValue("CHANGE_DATE"))
							.addChild(GedcomNode.create("TIME")
								.withValue("TIME_VALUE")))
				))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1")
				.withValue("SUBMITTER_TEXT"))
			.addChild(GedcomNode.create("SOUR")
				.withID("S1"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: D1, tag: OBJE, children: [{tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORM, value: MULTIMEDIA_FORMAT, children: [{tag: TYPE, value: SOURCE_MEDIA_TYPE}]}, {tag: TITL, value: DESCRIPTIVE_TITLE}]}, {tag: REFN, value: USER_REFERENCE_NUMBER, children: [{tag: TYPE, value: USER_REFERENCE_TYPE}]}, {tag: RIN, value: AUTOMATED_RECORD_ID}, {id: N1, tag: NOTE}, {id: S1, tag: SOUR}, {tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE}, {tag: TIME, value: TIME_VALUE}]}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}, {id: S1, tag: SOUR}]", root.toString());

		final Transformation t = new MultimediaRecordTransformation();
		t.to(extractSubStructure(root, "PARENT", "OBJE"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: D1, tag: DOCUMENT, children: [{tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORMAT, value: MULTIMEDIA_FORMAT}, {tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: TYPE, value: SOURCE_MEDIA_TYPE}]}, {tag: _REFN, value: USER_REFERENCE_NUMBER, children: [{tag: TYPE, value: USER_REFERENCE_TYPE}]}, {tag: _RIN, value: AUTOMATED_RECORD_ID}, {id: N1, tag: NOTE}, {id: S1, tag: SOURCE}, {tag: CHANGE, children: [{tag: DATE, value: CHANGE_DATE}, {tag: TIME, value: TIME_VALUE}]}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}, {id: S1, tag: SOUR}]", root.toString());
	}

	@Test
	void from(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("DOCUMENT")
					.withID("D1")
					.addChild(GedcomNode.create("TITLE")
						.withValue("DESCRIPTIVE_TITLE"))
					.addChild(GedcomNode.create("FILE")
						.withValue("DOCUMENT_FILE_REFERENCE")
						.addChild(GedcomNode.create("TITLE")
							.withValue("FILE_DESCRIPTIVE_TITLE"))
						.addChild(GedcomNode.create("FORMAT")
							.withValue("DOCUMENT_FORMAT"))
						.addChild(GedcomNode.create("TYPE")
							.withValue("SOURCE_MEDIA_TYPE"))
						.addChild(GedcomNode.create("CUT")
							.withValue("CUT_COORDINATES")))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))
					.addChild(GedcomNode.create("SOURCE")
						.withID("S1"))
					.addChild(GedcomNode.create("SUBMITTER")
						.withID("SUBM1"))
					.addChild(GedcomNode.create("RESTRICTION")
						.withID("RESTRICTION_NOTICE"))
					.addChild(GedcomNode.create("CHANGE")
						.addChild(GedcomNode.create("DATE")
							.withValue("CHANGE_DATE")
							.addChild(GedcomNode.create("TIME")
								.withValue("TIME_VALUE"))))
				))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1")
				.withValue("SUBMITTER_TEXT"))
			.addChild(GedcomNode.create("SOURCE")
				.withID("S1"))
			.addChild(GedcomNode.create("SUBMITTER")
				.withID("SUBM1"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: D1, tag: DOCUMENT, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: DOCUMENT_FILE_REFERENCE, children: [{tag: TITLE, value: FILE_DESCRIPTIVE_TITLE}, {tag: FORMAT, value: DOCUMENT_FORMAT}, {tag: TYPE, value: SOURCE_MEDIA_TYPE}, {tag: CUT, value: CUT_COORDINATES}]}, {id: N1, tag: NOTE}, {id: S1, tag: SOURCE}, {id: SUBM1, tag: SUBMITTER}, {id: RESTRICTION_NOTICE, tag: RESTRICTION}, {tag: CHANGE, children: [{tag: DATE, value: CHANGE_DATE, children: [{tag: TIME, value: TIME_VALUE}]}]}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}, {id: S1, tag: SOURCE}, {id: SUBM1, tag: SUBMITTER}]", root.toString());

		final Transformation t = new MultimediaRecordTransformation();
		t.from(extractSubStructure(root, "PARENT", "DOCUMENT"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: D1, tag: OBJE, children: [{tag: _TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: DOCUMENT_FILE_REFERENCE, children: [{tag: TITL, value: FILE_DESCRIPTIVE_TITLE}, {tag: FORM, value: DOCUMENT_FORMAT, children: [{tag: TYPE, value: SOURCE_MEDIA_TYPE}]}, {tag: _CUT, value: CUT_COORDINATES}]}, {id: N1, tag: NOTE}, {id: S1, tag: SOUR}, {id: SUBM1, tag: _SUBMITTER}, {id: RESTRICTION_NOTICE, tag: _RESN}, {tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE, children: [{tag: TIME, value: TIME_VALUE}]}]}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}, {id: S1, tag: SOURCE}, {id: SUBM1, tag: SUBMITTER}]", root.toString());
	}

}