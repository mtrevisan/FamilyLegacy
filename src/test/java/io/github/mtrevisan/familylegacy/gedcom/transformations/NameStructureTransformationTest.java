package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class NameStructureTransformationTest{

	@Test
	void to(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(composeGedcomNameTo());

		Assertions.assertEquals("children: [{id: I1, tag: INDI, children: [{tag: NAME, value: NAME_PERSONAL /SURNAME_PERSONAL/, children: [{tag: TYPE, value: NAME_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT1}]}, {id: S1, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}, {tag: SOUR, value: SOURCE_DESCRIPTION, children: [{tag: CONC, value: SOURCE_DESCRIPTION}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}]}, {tag: FONE, value: NAME_PHONETIC_VARIATION1, children: [{tag: TYPE, value: PHONETIC_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT1}]}, {id: S1, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}, {tag: SOUR, value: SOURCE_DESCRIPTION, children: [{tag: CONC, value: SOURCE_DESCRIPTION}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}]}, {tag: ROMN, value: NAME_ROMANIZED_VARIATION1, children: [{tag: TYPE, value: ROMANIZED_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT1}]}, {id: S1, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}, {tag: SOUR, value: SOURCE_DESCRIPTION, children: [{tag: CONC, value: SOURCE_DESCRIPTION}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}]}]}]", root.toString());

		final Transformation t = new NameTransformation();
		t.to(extractSubStructure(root, "INDI", "NAME"), root);
		GedcomNode node = extractSubStructure(root, "INDI", "FONE");
		node.withTag("NAME");
		t.to(node, root);
		node = extractSubStructure(root, "INDI", "ROMN");
		node.withTag("NAME");
		t.to(node, root);

		Assertions.assertEquals("children: [{id: I1, tag: INDI, children: [{tag: NAME, children: [{tag: TYPE, value: NAME_TYPE}, {tag: NAME_PREFIX, value: NAME_PIECE_PREFIX}, {tag: NAME, value: NAME_PIECE_GIVEN}, {tag: NICKNAME, value: NAME_PIECE_NICKNAME}, {tag: SURNAME, value: NAME_PIECE_SURNAME_PREFIX NAME_PIECE_SURNAME}, {tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {id: N1, tag: NOTE}, {id: S1, tag: SOUR}, {id: S2, tag: SOUR, value: SOURCE_DESCRIPTION}]}, {tag: NAME, children: [{tag: TYPE, value: PHONETIC_TYPE}, {tag: NAME_PREFIX, value: NAME_PIECE_PREFIX}, {tag: NAME, value: NAME_PIECE_GIVEN}, {tag: NICKNAME, value: NAME_PIECE_NICKNAME}, {tag: SURNAME, value: NAME_PIECE_SURNAME_PREFIX NAME_PIECE_SURNAME}, {tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {id: N7, tag: NOTE}, {id: S1, tag: SOUR}, {id: S4, tag: SOUR, value: SOURCE_DESCRIPTION}]}, {tag: NAME, children: [{tag: TYPE, value: ROMANIZED_TYPE}, {tag: NAME_PREFIX, value: NAME_PIECE_PREFIX}, {tag: NAME, value: NAME_PIECE_GIVEN}, {tag: NICKNAME, value: NAME_PIECE_NICKNAME}, {tag: SURNAME, value: NAME_PIECE_SURNAME_PREFIX NAME_PIECE_SURNAME}, {tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {id: N13, tag: NOTE}, {id: S1, tag: SOUR}, {id: S6, tag: SOUR, value: SOURCE_DESCRIPTION}]}]}, {id: N18, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT}, {id: D6, tag: DOCUMENT, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N17, tag: NOTE, value: TEXT_FROM_SOURCETEXT_FROM_SOURCE}, {id: N16, tag: NOTE, value: SOURCE_DESCRIPTIONSOURCE_DESCRIPTION}, {id: N15, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT}, {id: D5, tag: DOCUMENT, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORMAT}]}]}, {id: N14, tag: NOTE, value: TEXT_FROM_SOURCETEXT_FROM_SOURCE}, {id: N12, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT}, {id: D4, tag: DOCUMENT, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N11, tag: NOTE, value: TEXT_FROM_SOURCETEXT_FROM_SOURCE}, {id: N10, tag: NOTE, value: SOURCE_DESCRIPTIONSOURCE_DESCRIPTION}, {id: N9, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT}, {id: D3, tag: DOCUMENT, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORMAT}]}]}, {id: N8, tag: NOTE, value: TEXT_FROM_SOURCETEXT_FROM_SOURCE}, {id: N6, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT}, {id: D2, tag: DOCUMENT, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N5, tag: NOTE, value: TEXT_FROM_SOURCETEXT_FROM_SOURCE}, {id: N4, tag: NOTE, value: SOURCE_DESCRIPTIONSOURCE_DESCRIPTION}, {id: N3, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT}, {id: D1, tag: DOCUMENT, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORMAT}]}]}, {id: N2, tag: NOTE, value: TEXT_FROM_SOURCETEXT_FROM_SOURCE}, {id: N1, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT1}, {tag: SOURCE, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVENT, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATE, value: ENTRY_RECORDING_DATE}, {id: N2, tag: NOTE}, {id: D1, tag: DOCUMENT}, {id: D1, tag: DOCUMENT}, {id: N2, tag: NOTE}, {id: N3, tag: NOTE}, {tag: CERTAINTY, value: CERTAINTY_ASSESSMENT}]}, {id: S2, tag: SOURCE, children: [{id: N4, tag: NOTE}, {id: N5, tag: NOTE}, {id: D1, tag: DOCUMENT}, {id: D2, tag: DOCUMENT}, {id: N2, tag: NOTE}, {id: N6, tag: NOTE}, {tag: CERTAINTY, value: CERTAINTY_ASSESSMENT}]}, {id: N7, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT1}, {tag: SOURCE, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVENT, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATE, value: ENTRY_RECORDING_DATE}, {id: N8, tag: NOTE}, {id: D1, tag: DOCUMENT}, {id: D3, tag: DOCUMENT}, {id: N2, tag: NOTE}, {id: N9, tag: NOTE}, {tag: CERTAINTY, value: CERTAINTY_ASSESSMENT}]}, {id: S4, tag: SOURCE, children: [{id: N10, tag: NOTE}, {id: N11, tag: NOTE}, {id: D1, tag: DOCUMENT}, {id: D4, tag: DOCUMENT}, {id: N2, tag: NOTE}, {id: N12, tag: NOTE}, {tag: CERTAINTY, value: CERTAINTY_ASSESSMENT}]}, {id: N13, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT1}, {tag: SOURCE, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVENT, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATE, value: ENTRY_RECORDING_DATE}, {id: N14, tag: NOTE}, {id: D1, tag: DOCUMENT}, {id: D5, tag: DOCUMENT}, {id: N2, tag: NOTE}, {id: N15, tag: NOTE}, {tag: CERTAINTY, value: CERTAINTY_ASSESSMENT}]}, {id: S6, tag: SOURCE, children: [{id: N16, tag: NOTE}, {id: N17, tag: NOTE}, {id: D1, tag: DOCUMENT}, {id: D6, tag: DOCUMENT}, {id: N2, tag: NOTE}, {id: N18, tag: NOTE}, {tag: CERTAINTY, value: CERTAINTY_ASSESSMENT}]}]", root.toString());
	}

	private GedcomNode composeGedcomNameTo(){
		final GedcomNode parent = GedcomNode.create("INDI")
			.withID("I1");

		final GedcomNode name = GedcomNode.create("NAME")
			.withValue("NAME_PERSONAL /SURNAME_PERSONAL/")
			.addChild(GedcomNode.create("TYPE")
				.withValue("NAME_TYPE"));
		createPersonalNamePiecesTo(name);
		parent.addChild(name);

		final GedcomNode phone = GedcomNode.create("FONE")
			.withValue("NAME_PHONETIC_VARIATION1")
			.addChild(GedcomNode.create("TYPE")
				.withValue("PHONETIC_TYPE"));
		createPersonalNamePiecesTo(phone);
		parent.addChild(phone);

		final GedcomNode romanized = GedcomNode.create("ROMN")
			.withValue("NAME_ROMANIZED_VARIATION1")
			.addChild(GedcomNode.create("TYPE")
				.withValue("ROMANIZED_TYPE"));
		createPersonalNamePiecesTo(romanized);
		parent.addChild(romanized);

		return parent;
	}

	private void createPersonalNamePiecesTo(final GedcomNode name){
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


	@Test
	void from(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(composeGedcomNameFrom());

		Assertions.assertEquals("children: [{id: I1, tag: INDIVIDUAL, children: [{tag: NAME, children: [{tag: TYPE, value: NAME_TYPE}, {tag: LOCALE, value: en_US}, {tag: NAME, value: NAME_PIECE}, {tag: NAME_PREFIX, value: NAME_PIECE_PREFIX}, {tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX}, {tag: NICKNAME, value: NAME_PIECE_NICKNAME}, {tag: SURNAME, value: SURNAME_PIECE1}, {tag: SURNAME, value: SURNAME_PIECE2}, {tag: FAMILY_NICKNAME, value: SURNAME_PIECE_NICKNAME}, {id: N1, tag: NOTE}, {id: N1, tag: NOTE}, {id: S1, tag: SOUR}, {id: S2, tag: SOUR, value: SOURCE_DESCRIPTION}]}, {tag: NAME, children: [{tag: TYPE, value: PHONETIC_TYPE}, {tag: NAME_PREFIX, value: NAME_PIECE_PREFIX}, {tag: NAME, value: NAME_PIECE_GIVEN}, {tag: NICKNAME, value: NAME_PIECE_NICKNAME}, {tag: SURNAME, value: NAME_PIECE_SURNAME_PREFIX NAME_PIECE_SURNAME}, {tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {id: N7, tag: NOTE}, {id: S1, tag: SOUR}, {id: S4, tag: SOUR, value: SOURCE_DESCRIPTION}]}, {tag: NAME, children: [{tag: TYPE, value: ROMANIZED_TYPE}, {tag: NAME_PREFIX, value: NAME_PIECE_PREFIX}, {tag: NAME, value: NAME_PIECE_GIVEN}, {tag: NICKNAME, value: NAME_PIECE_NICKNAME}, {tag: SURNAME, value: NAME_PIECE_SURNAME_PREFIX NAME_PIECE_SURNAME}, {tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {id: N13, tag: NOTE}, {id: S1, tag: SOUR}, {id: S6, tag: SOUR, value: SOURCE_DESCRIPTION}]}]}, {id: N18, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT}, {id: D6, tag: DOCUMENT, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N17, tag: NOTE, value: TEXT_FROM_SOURCETEXT_FROM_SOURCE}, {id: N16, tag: NOTE, value: SOURCE_DESCRIPTIONSOURCE_DESCRIPTION}, {id: N15, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT}, {id: D5, tag: DOCUMENT, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORMAT}]}]}, {id: N14, tag: NOTE, value: TEXT_FROM_SOURCETEXT_FROM_SOURCE}, {id: N12, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT}, {id: D4, tag: DOCUMENT, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N11, tag: NOTE, value: TEXT_FROM_SOURCETEXT_FROM_SOURCE}, {id: N10, tag: NOTE, value: SOURCE_DESCRIPTIONSOURCE_DESCRIPTION}, {id: N9, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT}, {id: D3, tag: DOCUMENT, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORMAT}]}]}, {id: N8, tag: NOTE, value: TEXT_FROM_SOURCETEXT_FROM_SOURCE}, {id: N6, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT}, {id: D2, tag: DOCUMENT, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N5, tag: NOTE, value: TEXT_FROM_SOURCETEXT_FROM_SOURCE}, {id: N4, tag: NOTE, value: SOURCE_DESCRIPTIONSOURCE_DESCRIPTION}, {id: N3, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT}, {id: D1, tag: DOCUMENT, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORMAT}]}]}, {id: N2, tag: NOTE, value: TEXT_FROM_SOURCETEXT_FROM_SOURCE}, {id: N1, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT1}, {tag: SOURCE, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVENT, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATE, value: ENTRY_RECORDING_DATE}, {id: N2, tag: NOTE}, {id: D1, tag: DOCUMENT}, {id: D1, tag: DOCUMENT}, {id: N2, tag: NOTE}, {id: N3, tag: NOTE}, {tag: CERTAINTY, value: CERTAINTY_ASSESSMENT}]}, {id: S2, tag: SOURCE, children: [{id: N4, tag: NOTE}, {id: N5, tag: NOTE}, {id: D1, tag: DOCUMENT}, {id: D2, tag: DOCUMENT}, {id: N2, tag: NOTE}, {id: N6, tag: NOTE}, {tag: CERTAINTY, value: CERTAINTY_ASSESSMENT}]}, {id: N7, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT1}, {tag: SOURCE, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVENT, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATE, value: ENTRY_RECORDING_DATE}, {id: N8, tag: NOTE}, {id: D1, tag: DOCUMENT}, {id: D3, tag: DOCUMENT}, {id: N2, tag: NOTE}, {id: N9, tag: NOTE}, {tag: CERTAINTY, value: CERTAINTY_ASSESSMENT}]}, {id: S4, tag: SOURCE, children: [{id: N10, tag: NOTE}, {id: N11, tag: NOTE}, {id: D1, tag: DOCUMENT}, {id: D4, tag: DOCUMENT}, {id: N2, tag: NOTE}, {id: N12, tag: NOTE}, {tag: CERTAINTY, value: CERTAINTY_ASSESSMENT}]}, {id: N13, tag: NOTE, value: SUBMITTER_TEXTSUBMITTER_TEXT1}, {tag: SOURCE, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVENT, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATE, value: ENTRY_RECORDING_DATE}, {id: N14, tag: NOTE}, {id: D1, tag: DOCUMENT}, {id: D5, tag: DOCUMENT}, {id: N2, tag: NOTE}, {id: N15, tag: NOTE}, {tag: CERTAINTY, value: CERTAINTY_ASSESSMENT}]}, {id: S6, tag: SOURCE, children: [{id: N16, tag: NOTE}, {id: N17, tag: NOTE}, {id: D1, tag: DOCUMENT}, {id: D6, tag: DOCUMENT}, {id: N2, tag: NOTE}, {id: N18, tag: NOTE}, {tag: CERTAINTY, value: CERTAINTY_ASSESSMENT}]}]", root.toString());
		Assertions.assertEquals("children: [{id: I1, tag: INDIVIDUAL, children: [{tag: NAME, children: [{tag: TYPE, value: NAME_TYPE}, {tag: LOCALE, value: en_US}, {tag: NAME, value: NAME_PIECE}, {tag: NAME_PREFIX, value: NAME_PIECE_PREFIX}, {tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX}, {tag: NICKNAME, value: NAME_PIECE_NICKNAME}, {tag: SURNAME, value: SURNAME_PIECE1}, {tag: SURNAME, value: SURNAME_PIECE2}, {tag: FAMILY_NICKNAME, value: SURNAME_PIECE_NICKNAME}, {id: N1, tag: NOTE}, {id: SOURCE_ID, tag: SOURCE, children: [{tag: EVENT, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE}, {tag: PAGE, value: WHERE_WITHIN_SOURCE}, {id: D1, tag: DOCUMENT}, {id: N2, tag: NOTE}, {tag: CREDIBILITY, value: CREDIBILITY_ASSESSMENT}]}]}]}]", root.toString());

		final Transformation t = new NameTransformation();
		final GedcomNode individual = extractSubStructure(root, "INDI");
		final List<GedcomNode> names = individual.getChildrenWithTag("NAME");
		for(final GedcomNode name : names)
			t.from(name, root);

		Assertions.assertEquals("children: [{id: I1, tag: INDI, children: [{tag: NAME, value: NAME_PERSONAL /SURNAME_PERSONAL/, children: [{tag: TYPE, value: NAME_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT1}]}, {id: S1, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}, {tag: SOUR, value: SOURCE_DESCRIPTION, children: [{tag: CONC, value: SOURCE_DESCRIPTION}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}]}, {tag: NAME, value: NAME_PHONETIC_VARIATION1, children: [{tag: TYPE, value: PHONETIC_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT1}]}, {id: S1, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}, {tag: SOUR, value: SOURCE_DESCRIPTION, children: [{tag: CONC, value: SOURCE_DESCRIPTION}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}]}, {tag: ROMN, value: NAME_ROMANIZED_VARIATION1, children: [{tag: TYPE, value: ROMANIZED_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT1}]}, {id: S1, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}, {tag: SOUR, value: SOURCE_DESCRIPTION, children: [{tag: CONC, value: SOURCE_DESCRIPTION}, {tag: TEXT, value: TEXT_FROM_SOURCE, children: [{tag: CONC, value: TEXT_FROM_SOURCE}]}, {id: D1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {id: N2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: CONC, value: SUBMITTER_TEXT}]}, {tag: QUAY, value: CERTAINTY_ASSESSMENT}]}]}]}]", root.toString());
	}

	private GedcomNode composeGedcomNameFrom(){
		final GedcomNode parent = GedcomNode.create("INDIVIDUAL")
			.withID("I1");

		final GedcomNode name = GedcomNode.create("NAME")
			.addChild(GedcomNode.create("TYPE")
				.withValue("NAME_TYPE"))
			.addChild(GedcomNode.create("LOCALE")
				.withValue("en_US"))
			.addChild(GedcomNode.create("NAME")
				.withValue("NAME_PIECE"))
			.addChild(GedcomNode.create("NAME_PREFIX")
				.withValue("NAME_PIECE_PREFIX"))
			.addChild(GedcomNode.create("NAME_SUFFIX")
				.withValue("NAME_PIECE_SUFFIX"))
			.addChild(GedcomNode.create("NICKNAME")
				.withValue("NAME_PIECE_NICKNAME"))
			.addChild(GedcomNode.create("SURNAME")
				.withValue("SURNAME_PIECE1"))
			.addChild(GedcomNode.create("SURNAME")
				.withValue("SURNAME_PIECE2"))
			.addChild(GedcomNode.create("FAMILY_NICKNAME")
				.withValue("SURNAME_PIECE_NICKNAME"))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1"))
			.addChild(GedcomNode.create("SOURCE")
				.withID("SOURCE_ID")
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
					.withID("D1"))
				.addChild(GedcomNode.create("NOTE")
					.withID("N2"))
				.addChild(GedcomNode.create("CREDIBILITY")
					.withValue("CREDIBILITY_ASSESSMENT")));
		parent.addChild(name);

		return parent;
	}

}