package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class PersonalNameStructureTransformationTest{

	@Test
	void to(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("NAME")
					.withValue("NAME_PERSONAL")
					.addChild(GedcomNode.create("TYPE")
						.withValue("NAME_TYPE"))
					.addChild(GedcomNode.create("NPFX")
						.withValue("NAME_PIECE_PREFIX"))
					.addChild(GedcomNode.create("GIVN")
						.withValue("NAME_PIECE_GIVEN"))
					.addChild(GedcomNode.create("NICK")
						.withValue("NAME_PIECE_NICKNAME"))
					.addChild(GedcomNode.create("SPFX")
						.withValue("NAME_PIECE_SURNAME_PREFIX"))
					.addChild(GedcomNode.create("SURN")
						.withValue("NAME_PIECE_SURNAME"))
					.addChild(GedcomNode.create("NSFX")
						.withValue("NAME_PIECE_SUFFIX"))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))
					.addChild(GedcomNode.create("SOUR")
						.withID("S1"))
					.addChild(GedcomNode.create("FONE")
						.withValue("NAME_PHONETIC_VARIATION")
						.addChild(GedcomNode.create("TYPE")
							.withValue("PHONETIC_TYPE"))
						.addChild(GedcomNode.create("NPFX")
							.withValue("NAME_PIECE_PREFIX"))
						.addChild(GedcomNode.create("GIVN")
							.withValue("NAME_PIECE_GIVEN"))
						.addChild(GedcomNode.create("NICK")
							.withValue("NAME_PIECE_NICKNAME"))
						.addChild(GedcomNode.create("SPFX")
							.withValue("NAME_PIECE_SURNAME_PREFIX"))
						.addChild(GedcomNode.create("SURN")
							.withValue("NAME_PIECE_SURNAME"))
						.addChild(GedcomNode.create("NSFX")
							.withValue("NAME_PIECE_SUFFIX"))
						.addChild(GedcomNode.create("NOTE")
							.withID("N1"))
						.addChild(GedcomNode.create("SOUR")
							.withID("S1"))
					)
					.addChild(GedcomNode.create("ROMN")
						.withValue("NAME_ROMANIZED_VARIATION")
						.addChild(GedcomNode.create("TYPE")
							.withValue("ROMANIZED_TYPE"))
						.addChild(GedcomNode.create("NPFX")
							.withValue("NAME_PIECE_PREFIX"))
						.addChild(GedcomNode.create("GIVN")
							.withValue("NAME_PIECE_GIVEN"))
						.addChild(GedcomNode.create("NICK")
							.withValue("NAME_PIECE_NICKNAME"))
						.addChild(GedcomNode.create("SPFX")
							.withValue("NAME_PIECE_SURNAME_PREFIX"))
						.addChild(GedcomNode.create("SURN")
							.withValue("NAME_PIECE_SURNAME"))
						.addChild(GedcomNode.create("NSFX")
							.withValue("NAME_PIECE_SUFFIX"))
						.addChild(GedcomNode.create("NOTE")
							.withID("N1"))
						.addChild(GedcomNode.create("SOUR")
							.withID("S1"))
					)
				));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: NAME, value: NAME_PERSONAL, children: [{tag: TYPE, value: NAME_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {id: S1, tag: SOUR}, {tag: FONE, value: NAME_PHONETIC_VARIATION, children: [{tag: TYPE, value: PHONETIC_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {id: S1, tag: SOUR}]}, {tag: ROMN, value: NAME_ROMANIZED_VARIATION, children: [{tag: TYPE, value: ROMANIZED_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {id: S1, tag: SOUR}]}]}]}]", root.toString());

		final Transformation t = new PersonalNameStructureTransformation();
		t.to(extractSubStructure(root, "PARENT", "NAME"), root);

		Assertions.assertEquals("children: [{id: I1, tag: INDI, children: [{tag: NAME, children: [{tag: TYPE, value: NAME_TYPE}, {tag: NAME_PREFIX, value: NAME_PIECE_PREFIX}, {tag: NAME, value: NAME_PIECE_GIVEN}, {tag: NICKNAME, value: NAME_PIECE_NICKNAME}, {tag: SURNAME, value: NAME_PIECE_SURNAME_PREFIX NAME_PIECE_SURNAME}, {tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {id: N1, tag: NOTE}, {id: S1, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}, {tag: SOUR, value: SOURCE_DESCRIPTION, children: [{tag: CONC, value: SOURCE_DESCRIPTION}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}]}, {tag: NAME, children: [{tag: TYPE, value: PHONETIC_TYPE}, {tag: NAME_PREFIX, value: NAME_PIECE_PREFIX}, {tag: NAME, value: NAME_PIECE_GIVEN}, {tag: NICKNAME, value: NAME_PIECE_NICKNAME}, {tag: SURNAME, value: NAME_PIECE_SURNAME_PREFIX NAME_PIECE_SURNAME}, {tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {id: N2, tag: NOTE}, {id: S1, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}, {tag: SOUR, value: SOURCE_DESCRIPTION, children: [{tag: CONC, value: SOURCE_DESCRIPTION}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}]}, {tag: NAME, children: [{tag: TYPE, value: ROMANIZED_TYPE}, {tag: NAME_PREFIX, value: NAME_PIECE_PREFIX}, {tag: NAME, value: NAME_PIECE_GIVEN}, {tag: NICKNAME, value: NAME_PIECE_NICKNAME}, {tag: SURNAME, value: NAME_PIECE_SURNAME_PREFIX NAME_PIECE_SURNAME}, {tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {id: N3, tag: NOTE}, {id: S1, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}, {tag: SOUR, value: SOURCE_DESCRIPTION, children: [{tag: CONC, value: SOURCE_DESCRIPTION}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT1}, {id: N2, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT1}, {id: N3, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT1}]", root.toString());
	}


//	@Test
	void from(){
//		final GedcomNode root = GedcomNode.createEmpty()
//			.addChild(composeGedcomNameFrom());
//
//		Assertions.assertEquals("children: [{id: I1, tag: INDIVIDUAL, children: [{tag: NAME, children: [{tag: TYPE, value: NAME_TYPE}, {tag: LOCALE, value: en_US}, {tag: NAME, value: NAME_PIECE}, {tag: NAME_PREFIX, value: NAME_PIECE_PREFIX}, {tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX}, {tag: NICKNAME, value: NAME_PIECE_NICKNAME}, {tag: SURNAME, value: SURNAME_PIECE1}, {tag: SURNAME, value: SURNAME_PIECE2}, {tag: FAMILY_NICKNAME, value: SURNAME_PIECE_NICKNAME}, {id: N1, tag: NOTE}, {id: S1, tag: SOURCE, children: [{tag: EVENT, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE}, {tag: PAGE, value: WHERE_WITHIN_SOURCE}, {id: D1, tag: DOCUMENT}, {id: N2, tag: NOTE}, {tag: CREDIBILITY, value: CREDIBILITY_ASSESSMENT}]}]}]}]", root.toString());
//
//		final Transformation t = new NameTransformation();
//		final GedcomNode individual = extractSubStructure(root, "INDIVIDUAL");
//		final List<GedcomNode> names = individual.getChildrenWithTag("NAME");
//		for(final GedcomNode name : names)
//			t.from(name, root);
//
//		Assertions.assertEquals("children: [{id: I1, tag: INDIVIDUAL, children: [{tag: NAME, value: NAME_PIECE /SURNAME_PIECE1 SURNAME_PIECE2/, children: [{tag: TYPE, value: NAME_TYPE}, {tag: GIVN, value: NAME_PIECE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {id: N1, tag: NOTE}, {id: S1, tag: SOUR, children: [{tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: PAGE, value: WHERE_WITHIN_SOURCE}, {id: D1, tag: OBJE}, {id: N2, tag: NOTE}, {tag: QUAY, value: CREDIBILITY_ASSESSMENT}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE}]}]}, {tag: SURN, value: SURNAME_PIECE1 SURNAME_PIECE2}]}]}]", root.toString());
	}

}