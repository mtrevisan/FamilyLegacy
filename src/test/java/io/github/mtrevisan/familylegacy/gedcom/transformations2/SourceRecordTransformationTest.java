package io.github.mtrevisan.familylegacy.gedcom.transformations2;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class SourceRecordTransformationTest{

	@Test
	void to(){
		final GedcomNode source = GedcomNode.create("SOUR")
			.withID("S1")
			.addChild(GedcomNode.create("DATA")
				.addChild(GedcomNode.create("EVEN")
					.withValue("EVENTS_RECORDED1")
					.addChild(GedcomNode.create("DATE")
						.withValue("DATE_PERIOD"))
					.addChild(GedcomNode.create("PLAC")
						.withValue("SOURCE_JURISDICTION_PLACE"))
				)
				.addChild(GedcomNode.create("EVEN")
					.withValue("EVENTS_RECORDED2"))
			)
			.addChild(GedcomNode.create("AUTH")
				.withValue("SOURCE_ORIGINATOR"))
			.addChild(GedcomNode.create("TITL")
				.withValue("SOURCE_DESCRIPTIVE_TITLE"))
			.addChild(GedcomNode.create("ABBR")
				.withValue("SOURCE_FILED_BY_ENTRY"))
			.addChild(GedcomNode.create("PUBL")
				.withValue("SOURCE_PUBLICATION_FACTS"))
			.addChild(GedcomNode.create("TEXT")
				.withValue("TEXT_FROM_SOURCE"))
			.addChild(GedcomNode.create("REPO")
				.withID("R1"))
			.addChild(GedcomNode.create("REPO")
				.addChild(GedcomNode.create("NOTE")
					.withID("N2"))
				.addChild(GedcomNode.create("CALN")
					.withValue("SOURCE_CALL_NUMBER"))
			)
			.addChild(GedcomNode.create("REFN")
				.withValue("USER_REFERENCE_NUMBER")
				.addChild(GedcomNode.create("TYPE")
					.withValue("USER_REFERENCE_TYPE"))
			)
			.addChild(GedcomNode.create("RIN")
				.withValue("AUTOMATED_RECORD_ID"))
			.addChild(GedcomNode.create("CHAN")
				.addChild(GedcomNode.create("DATE")
					.withValue("CHANGE_DATE")))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1"))
			.addChild(GedcomNode.create("NOTE")
				.withValue("SUBMITTER_TEXT"))
			.addChild(GedcomNode.create("OBJE")
				.withID("D1"))
			.addChild(GedcomNode.create("OBJE")
				.addChild(GedcomNode.create("TITL")
					.withValue("DESCRIPTIVE_TITLE"))
				.addChild(GedcomNode.create("FORM")
					.withValue("MULTIMEDIA_FORMAT")
					.addChild(GedcomNode.create("MEDI")
						.withValue("SOURCE_MEDIA_TYPE"))
				)
				.addChild(GedcomNode.create("FILE")
					.withValue("MULTIMEDIA_FILE_REFN"))
			);
		final GedcomNode note = GedcomNode.create("NOTE", "N1", "SUBMITTER_TEXT");
		final Gedcom origin = new Gedcom();
		origin.addSource(source);
		origin.addNote(note);
		final Flef destination = new Flef();

		Assertions.assertEquals("id: S1, tag: SOUR, children: [{tag: DATA, children: [{tag: EVEN, value: EVENTS_RECORDED1, children: [{tag: DATE, value: DATE_PERIOD}, {tag: PLAC, value: SOURCE_JURISDICTION_PLACE}]}, {tag: EVEN, value: EVENTS_RECORDED2}]}, {tag: AUTH, value: SOURCE_ORIGINATOR}, {tag: TITL, value: SOURCE_DESCRIPTIVE_TITLE}, {tag: ABBR, value: SOURCE_FILED_BY_ENTRY}, {tag: PUBL, value: SOURCE_PUBLICATION_FACTS}, {tag: TEXT, value: TEXT_FROM_SOURCE}, {id: R1, tag: REPO}, {tag: REPO, children: [{id: N2, tag: NOTE}, {tag: CALN, value: SOURCE_CALL_NUMBER}]}, {tag: REFN, value: USER_REFERENCE_NUMBER, children: [{tag: TYPE, value: USER_REFERENCE_TYPE}]}, {tag: RIN, value: AUTOMATED_RECORD_ID}, {tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE}]}, {id: N1, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FORM, value: MULTIMEDIA_FORMAT, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}]}]", origin.getSources().get(0).toString());

		final Transformation t = new SourceRecordTransformation();
		t.to(origin, destination);

		Assertions.assertEquals("id: S1, tag: SOURCE, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORMAT, value: MULTIMEDIA_FORMAT}, {tag: MEDIA, value: SOURCE_MEDIA_TYPE}]}]", destination.getSources().get(0).toString());
		Assertions.assertEquals("id: S1, tag: SOURCE, children: [{tag: TITLE, value: SOURCE_DESCRIPTIVE_TITLE}, {tag: EVENT, value: EVENTS_RECORDED1}, {tag: EVENT, value: EVENTS_RECORDED2}, {tag: DATE, value: DATE_PERIOD}, {tag: TEXT, value: TEXT_FROM_SOURCE}, {id: N1, tag: NOTE}, {id: D1, tag: SOURCE}, {id: S1, tag: SOURCE}, {id: N1, tag: NOTE}, {id: N2, tag: NOTE}, {id: R1, tag: REPOSITORY}, {id: R1, tag: REPOSITORY, children: [{id: N2, tag: NOTE}]}]", destination.getSources().get(1).toString());
		Assertions.assertEquals("id: N1, tag: NOTE, value: SOURCE_ORIGINATOR, SOURCE_PUBLICATION_FACTS", destination.getNotes().get(0).toString());
		Assertions.assertEquals("id: N2, tag: NOTE, value: SUBMITTER_TEXT", destination.getNotes().get(1).toString());
		Assertions.assertEquals("id: R1, tag: REPOSITORY, children: [{id: N2, tag: NOTE}]", destination.getRepositories().get(0).toString());
	}

//	@Test
//	void from(){
//		final GedcomNode root = GedcomNode.createEmpty()
//			.addChild(GedcomNode.create("PARENT")
//				.addChild(GedcomNode.create("SOURCE")
//					.withID("S1")
//					.addChild(GedcomNode.create("TITLE")
//						.withValue("SOURCE_DESCRIPTIVE_TITLE"))
//					.addChild(GedcomNode.create("AUTHOR")
//						.withValue("SOURCE_ORIGINATOR"))
//					.addChild(GedcomNode.create("PUBLICATION")
//						.withValue("SOURCE_PUBLICATION_FACTS"))
//					.addChild(GedcomNode.create("PUBLICATION_DATE")
//						.withValue("DATE_VALUE"))
//					.addChild(GedcomNode.create("LOCATION")
//						.withValue("LOCATION_TEXT"))
//					.addChild(GedcomNode.create("TEXT")
//						.withValue("TEXT_FROM_SOURCE"))
//					.addChild(GedcomNode.create("DOCUMENT")
//						.withID("D1"))
//					.addChild(GedcomNode.create("NOTE")
//						.withID("N1"))
//					.addChild(GedcomNode.create("RESTRICTION")
//						.withValue("RESTRICTION_NOTICE"))
//					.addChild(GedcomNode.create("CHANGE")
//						.addChild(GedcomNode.create("DATE")
//							.withValue("CHANGE_DATE"))))
//				.addChild(GedcomNode.create("NOTE")
//					.withID("N1")
//					.withValue("SUBMITTER_TEXT")));
//
//		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: S1, tag: SOURCE, children: [{tag: TITLE, value: SOURCE_DESCRIPTIVE_TITLE}, {tag: AUTHOR, value: SOURCE_ORIGINATOR}, {tag: PUBLICATION, value: SOURCE_PUBLICATION_FACTS}, {tag: PUBLICATION_DATE, value: DATE_VALUE}, {tag: LOCATION, value: LOCATION_TEXT}, {tag: TEXT, value: TEXT_FROM_SOURCE}, {id: D1, tag: DOCUMENT}, {id: N1, tag: NOTE}, {tag: RESTRICTION, value: RESTRICTION_NOTICE}, {tag: CHANGE, children: [{tag: DATE, value: CHANGE_DATE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]}]", root.toString());
//
//		final Transformation t = new SourceRecordTransformation();
//		t.from(extractSubStructure(root, "PARENT", "SOURCE"), root);
//
//		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: S1, tag: SOUR, children: [{tag: TITL, value: SOURCE_DESCRIPTIVE_TITLE}, {tag: AUTH, value: SOURCE_ORIGINATOR}, {tag: PUBL, value: SOURCE_PUBLICATION_FACTS}, {tag: _PUBLICATION_DATE, value: DATE_VALUE}, {tag: _LOCATION, value: LOCATION_TEXT}, {tag: TEXT, value: TEXT_FROM_SOURCE}, {id: D1, tag: OBJE}, {id: N1, tag: NOTE}, {tag: _RESTRICTION, value: RESTRICTION_NOTICE}, {tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]}]", root.toString());
//	}

}