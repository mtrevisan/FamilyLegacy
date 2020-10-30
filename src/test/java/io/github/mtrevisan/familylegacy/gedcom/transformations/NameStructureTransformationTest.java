package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class NameStructureTransformationTest{

	@Test
	void toAndFrom(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(composeGedcomName());

		Assertions.assertEquals("children: [{id: I1, tag: INDI, children: [{tag: NAME, value: NAME_PERSONAL /SURNAME_PERSONAL/, children: [{tag: TYPE, value: NAME_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT1}]}, {id: S1, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}, {tag: SOUR, value: SOURCE_DESCRIPTION, children: [{tag: CONC, value: SOURCE_DESCRIPTION}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}]}, {tag: FONE, value: NAME_PHONETIC_VARIATION1, children: [{tag: TYPE, value: PHONETIC_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT1}]}, {id: S1, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}, {tag: SOUR, value: SOURCE_DESCRIPTION, children: [{tag: CONC, value: SOURCE_DESCRIPTION}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}]}, {tag: ROMN, value: NAME_ROMANIZED_VARIATION1, children: [{tag: TYPE, value: ROMANIZED_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT1}]}, {id: S1, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}, {tag: SOUR, value: SOURCE_DESCRIPTION, children: [{tag: CONC, value: SOURCE_DESCRIPTION}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}]}]}]", root.toString());

		final Transformation t = new NameTransformation();
		t.to(extractSubStructure(root, "INDI", "NAME"), root);

		Assertions.assertEquals("children: [{id: I1, tag: INDI, children: [{tag: NAME, value: NAME_PERSONAL /SURNAME_PERSONAL/, children: [{tag: TYPE, value: NAME_TYPE}, {tag: NAME_PREFIX, value: NAME_PIECE_PREFIX}, {tag: NAME, value: NAME_PIECE_GIVEN}, {tag: NAME, value: NAME_PIECE_NICKNAME}, {tag: SURNAME, value: NAME_PIECE_SURNAME_PREFIX NAME_PIECE_SURNAME}, {tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT1}]}, {id: S1, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVENT, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}]}, {id: D1, tag: DOCUMENT}, {id: D1, tag: DOCUMENT, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORMAT}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}, {tag: SOUR, value: SOURCE_DESCRIPTION, children: [{tag: CONC, value: SOURCE_DESCRIPTION}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}, {id: D1, tag: DOCUMENT}, {id: D2, tag: DOCUMENT, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}, {id: N1, tag: NOTE}, {id: S1, tag: SOURCE}, {id: S2, tag: SOURCE}]}, {id: S2, tag: SOURCE, children: [{id: N3, tag: NOTE}, {id: N4, tag: NOTE}, {id: D1, tag: DOCUMENT}, {id: D2, tag: NOTE}]}, {id: D2, tag: DOCUMENT, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N4, tag: NOTE, value: TEXT_FROM_SOURCETEXT_FROM_SOURCE}, {id: N3, tag: NOTE, value: SOURCE_DESCRIPTIONSOURCE_DESCRIPTION}, {id: S1, tag: SOURCE, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVENT, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATE, value: ENTRY_RECORDING_DATE}, {id: N2, tag: NOTE}, {id: D1, tag: DOCUMENT}, {id: D1, tag: NOTE}]}, {id: D1, tag: DOCUMENT, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORMAT}]}]}, {id: N2, tag: NOTE, value: TEXT_FROM_SOURCETEXT_FROM_SOURCE}, {id: N1, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT1}, {tag: FONE, value: NAME_PHONETIC_VARIATION1, children: [{tag: TYPE, value: PHONETIC_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT1}]}, {id: S1, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}, {tag: SOUR, value: SOURCE_DESCRIPTION, children: [{tag: CONC, value: SOURCE_DESCRIPTION}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}]}, {tag: ROMN, value: NAME_ROMANIZED_VARIATION1, children: [{tag: TYPE, value: ROMANIZED_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT1}]}, {id: S1, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}, {tag: SOUR, value: SOURCE_DESCRIPTION, children: [{tag: CONC, value: SOURCE_DESCRIPTION}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}]}, {tag: NAME, children: [{tag: TYPE, value: INDIVIDUAL_NICKNAME}, {tag: NAME, value: NAME_PIECE_NICKNAME}]}]}]", root.toString());

		t.from(extractSubStructure(root, "INDI", "NAME"), root);

		Assertions.assertEquals("children: [{id: I1, tag: INDI, children: [{tag: NAME, value: NAME_PERSONAL /SURNAME_PERSONAL/, children: [{tag: TYPE, value: NAME_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT1}]}, {id: S1, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}, {tag: SOUR, value: SOURCE_DESCRIPTION, children: [{tag: CONC, value: SOURCE_DESCRIPTION}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}]}, {tag: FONE, value: NAME_PHONETIC_VARIATION1, children: [{tag: TYPE, value: PHONETIC_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT1}]}, {id: S1, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}, {tag: SOUR, value: SOURCE_DESCRIPTION, children: [{tag: CONC, value: SOURCE_DESCRIPTION}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}]}, {tag: ROMN, value: NAME_ROMANIZED_VARIATION1, children: [{tag: TYPE, value: ROMANIZED_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT1}]}, {id: S1, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}, {tag: SOUR, value: SOURCE_DESCRIPTION, children: [{tag: CONC, value: SOURCE_DESCRIPTION}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}]}]}]", root.toString());
	}

	private GedcomNode composeGedcomName(){
		final GedcomNode parent = GedcomNode.create("INDI")
			.withID("I1");

		final GedcomNode name = GedcomNode.create("NAME")
			.withValue("NAME_PERSONAL /SURNAME_PERSONAL/")
			.addChild(GedcomNode.create("TYPE")
				.withValue("NAME_TYPE"));
		createPersonalNamePieces(name);
		parent.addChild(name);

		final GedcomNode phone = GedcomNode.create("FONE")
			.withValue("NAME_PHONETIC_VARIATION1")
			.addChild(GedcomNode.create("TYPE")
				.withValue("PHONETIC_TYPE"));
		createPersonalNamePieces(phone);
		parent.addChild(phone);

		final GedcomNode romanized = GedcomNode.create("ROMN")
			.withValue("NAME_ROMANIZED_VARIATION1")
			.addChild(GedcomNode.create("TYPE")
				.withValue("ROMANIZED_TYPE"));
		createPersonalNamePieces(romanized);
		parent.addChild(romanized);

		return parent;
	}

	private void createPersonalNamePieces(final GedcomNode name){
		name.addChild(GedcomNode.create("NPFX")
			.withValue("NAME_PIECE_PREFIX"));
		name.addChild(GedcomNode.create("GIVN")
			.withValue("NAME_PIECE_GIVEN"));
		name.addChild(GedcomNode.create("NICK")
			.withValue("NAME_PIECE_NICKNAME"));
		name.addChild(GedcomNode.create("SPFX")
			.withValue("NAME_PIECE_SURNAME_PREFIX"));
		name.addChild(GedcomNode.create("SURN")
			.withValue("NAME_PIECE_SURNAME"));
		name.addChild(GedcomNode.create("NSFX")
			.withValue("NAME_PIECE_SUFFIX"));
		name.addChild(GedcomNode.create("NOTE")
			.withID("N1"));
		name.addChild(GedcomNode.create("NOTE")
			.withValue("SUBMITTER_TEXT")
			.addChild(GedcomNode.create("CONC")
				.withValue("SUBMITTER_TEXT1")));
		name.addChild(GedcomNode.create("SOUR")
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
			.addChild(GedcomNode.create("OBJE")
				.addChild(GedcomNode.create("TITL")
					.withValue("DESCRIPTIVE_TITLE"))
				.addChild(GedcomNode.create("FILE")
					.withValue("MULTIMEDIA_FILE_REFN")
					.addChild(GedcomNode.create("FORM")
						.addChild(GedcomNode.create("MEDI")
							.withValue("SOURCE_MEDIA_TYPE")))))
			.addChild(GedcomNode.create("NOTE")
				.withID("N2"))
			.addChild(GedcomNode.create("NOTE")
				.withValue("SUBMITTER_TEXT")
				.addChild(GedcomNode.create("CONC")
					.withValue("SUBMITTER_TEXT")))
			.addChild(GedcomNode.create("QUAY")
				.withValue("CERTAINTY_ASSESSMENT")));
		name.addChild(GedcomNode.create("SOUR")
			.withValue("SOURCE_DESCRIPTION")
			.addChild(GedcomNode.create("CONC")
				.withValue("SOURCE_DESCRIPTION"))
			.addChild(GedcomNode.create("TEXT")
				.withValue("TEXT_FROM_SOURCE")
				.addChild(GedcomNode.create("CONC")
					.withValue("TEXT_FROM_SOURCE")))
			.addChild(GedcomNode.create("OBJE")
				.withID("D1"))
			.addChild(GedcomNode.create("OBJE")
				.addChild(GedcomNode.create("TITL")
					.withValue("DESCRIPTIVE_TITLE"))
				.addChild(GedcomNode.create("FILE")
					.withValue("MULTIMEDIA_FILE_REFN"))
					.addChild(GedcomNode.create("FORM")
						.addChild(GedcomNode.create("MEDI")
							.withValue("SOURCE_MEDIA_TYPE"))))
			.addChild(GedcomNode.create("NOTE")
				.withID("N2"))
			.addChild(GedcomNode.create("NOTE")
				.withValue("SUBMITTER_TEXT")
				.addChild(GedcomNode.create("CONC")
					.withValue("SUBMITTER_TEXT")))
			.addChild(GedcomNode.create("QUAY")
				.withValue("CERTAINTY_ASSESSMENT")));
	}

}