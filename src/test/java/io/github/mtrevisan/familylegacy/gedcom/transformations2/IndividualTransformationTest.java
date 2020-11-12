package io.github.mtrevisan.familylegacy.gedcom.transformations2;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class IndividualTransformationTest{

	@Test
	void to(){
		final GedcomNode individual = GedcomNode.create("INDI")
			.withID("I1")
			.addChildValue("RESN", "RESTRICTION_NOTICE")
			.addChild(GedcomNode.create("NAME")
				.withValue("NAME_PERSONAL")
				.addChildValue("TYPE", "NAME_TYPE")
				.addChildValue("NPFX", "NAME_PIECE_PREFIX1")
				.addChildValue("GIVN", "NAME_PIECE_GIVEN1")
				.addChildValue("NICK", "NAME_PIECE_NICKNAME1")
				.addChildValue("SPFX", "NAME_PIECE_SURNAME_PREFIX1")
				.addChildValue("SURN", "NAME_PIECE_SURNAME1")
				.addChildValue("NSFX", "NAME_PIECE_SUFFIX1")
				.addChildReference("NOTE", "N-10")
				.addChildValue("NOTE", "SUBMITTER_TEXT10")
				.addChild(GedcomNode.create("SOUR")
					.addChildValue("TEXT", "TEXT_FROM_SOURCE1")
					.addChildReference("OBJE", "D-1")
					.addChild(GedcomNode.create("OBJE")
						.addChildValue("TITL", "DESCRIPTIVE_TITLE7")
						.addChild(GedcomNode.create("FORM")
							.withValue("MULTIMEDIA_FORMAT7")
							.addChildValue("MEDI", "SOURCE_MEDIA_TYPE7")
						)
						.addChildValue("FILE", "MULTIMEDIA_FILE_REFN7")
					)
					.addChildReference("NOTE", "N-12")
					.addChildValue("NOTE", "SUBMITTER_TEXT12")
					.addChildValue("QUAY", "CERTAINTY_ASSESSMENT12")
				)
				.addChild(GedcomNode.create("FONE")
					.withValue("NAME_PHONETIC_VARIATION")
					.addChildValue("TYPE", "PHONETIC_TYPE")
					.addChildValue("NPFX", "NAME_PIECE_PREFIX2")
					.addChildValue("GIVN", "NAME_PIECE_GIVEN2")
					.addChildValue("NICK", "NAME_PIECE_NICKNAME2")
					.addChildValue("SPFX", "NAME_PIECE_SURNAME_PREFIX2")
					.addChildValue("SURN", "NAME_PIECE_SURNAME2")
					.addChildValue("NSFX", "NAME_PIECE_SUFFIX2")
					.addChildReference("NOTE", "N-9")
					.addChildValue("NOTE", "SUBMITTER_TEXT9")
					.addChild(GedcomNode.create("SOUR")
						.addChildValue("TEXT", "TEXT_FROM_SOURCE2")
						.addChildReference("OBJE", "D-2")
						.addChild(GedcomNode.create("OBJE")
							.addChildValue("TITL", "DESCRIPTIVE_TITLE6")
							.addChild(GedcomNode.create("FORM")
								.withValue("MULTIMEDIA_FORMAT6")
								.addChildValue("MEDI", "SOURCE_MEDIA_TYPE6")
							)
							.addChildValue("FILE", "MULTIMEDIA_FILE_REFN6")
						)
						.addChildReference("NOTE", "N-12")
						.addChildValue("NOTE", "SUBMITTER_TEXT12")
						.addChildValue("QUAY", "CERTAINTY_ASSESSMENT12")
					)
				)
				.addChild(GedcomNode.create("ROMN")
					.withValue("NAME_ROMANIZED_VARIATION")
					.addChildValue("TYPE", "ROMANIZED_TYPE")
					.addChildValue("NPFX", "NAME_PIECE_PREFIX3")
					.addChildValue("GIVN", "NAME_PIECE_GIVEN3")
					.addChildValue("NICK", "NAME_PIECE_NICKNAME3")
					.addChildValue("SPFX", "NAME_PIECE_SURNAME_PREFIX3")
					.addChildValue("SURN", "NAME_PIECE_SURNAME3")
					.addChildValue("NSFX", "NAME_PIECE_SUFFIX3")
					.addChildReference("NOTE", "N-8")
					.addChildValue("NOTE", "SUBMITTER_TEXT8")
					.addChild(GedcomNode.create("SOUR")
						.addChildValue("TEXT", "TEXT_FROM_SOURCE3")
						.addChildReference("OBJE", "D-3")
						.addChild(GedcomNode.create("OBJE")
							.addChildValue("TITL", "DESCRIPTIVE_TITLE5")
							.addChild(GedcomNode.create("FORM")
								.withValue("MULTIMEDIA_FORMAT5")
								.addChildValue("MEDI", "SOURCE_MEDIA_TYPE5")
							)
							.addChildValue("FILE", "MULTIMEDIA_FILE_REFN5")
						)
						.addChildReference("NOTE", "N-11")
						.addChildValue("NOTE", "SUBMITTER_TEXT11")
						.addChildValue("QUAY", "CERTAINTY_ASSESSMENT11")
					)
				)
			)
			.addChildValue("SEX", "SEX_VALUE")
			.addChild(GedcomNode.create("BIRT")
				.addChildValue("TYPE", "EVENT_OR_FACT_CLASSIFICATION1")
				.addChildValue("DATE", "DATE_VALUE1")
				.addChild(GedcomNode.create("PLAC")
					.addChildValue("FORM", "PLACE_HIERARCHY1")
					.addChild(GedcomNode.create("FONE")
						.withValue("PLACE_PHONETIC_VARIATION1")
						.addChildValue("TYPE", "PHONETIC_TYPE")
					)
					.addChild(GedcomNode.create("ROMN")
						.withValue("PLACE_ROMANIZED_VARIATION")
						.addChildValue("TYPE", "ROMANIZED_TYPE")
					)
					.addChild(GedcomNode.create("MAP")
						.addChildValue("LATI", "PLACE_LATITUDE1")
						.addChildValue("LONG", "PLACE_LONGITUDE1")
					)
					.addChildReference("NOTE", "N-12")
				)
				.addChild(GedcomNode.create("ADDR")
					.withValue("ADDRESS_LINE1")
					.addChildValue("CONT", "ADDRESS_LINE1")
					.addChildValue("ADR1", "ADDRESS_LINE11")
					.addChildValue("ADR2", "ADDRESS_LINE12")
					.addChildValue("ADR3", "ADDRESS_LINE13")
					.addChildValue("CITY", "ADDRESS_CITY1")
					.addChildValue("STAE", "ADDRESS_STATE1")
					.addChildValue("POST", "ADDRESS_POSTAL_CODE1")
					.addChildValue("CTRY", "ADDRESS_COUNTRY1")
				)
				.addChildValue("PHON", "PHONE_NUMBER1")
				.addChildValue("EMAIL", "ADDRESS_EMAIL1")
				.addChildValue("FAX", "ADDRESS_FAX1")
				.addChildValue("WWW", "ADDRESS_WEB_PAGE1")
				.addChildValue("AGNC", "RESPONSIBLE_AGENCY1")
				.addChildValue("RELI", "RELIGIOUS_AFFILIATION1")
				.addChildValue("CAUS", "CAUSE_OF_EVENT1")
				.addChildValue("RESN", "RESTRICTION_NOTICE1")
				.addChildReference("NOTE", "N-12")
				.addChildReference("SOUR", "S-4")
				.addChildReference("OBJE", "D-4")
				.addChildValue("AGE", "AGE_AT_EVENT")
				.addChildReference("FAMC", "F3")
			)
			.addChild(GedcomNode.create("DEAT")
				.withValue("Y")
				.addChildValue("TYPE", "EVENT_OR_FACT_CLASSIFICATION2")
				.addChildValue("DATE", "DATE_VALUE2")
				.addChild(GedcomNode.create("PLAC")
					.addChildValue("FORM", "PLACE_HIERARCHY1")
					.addChild(GedcomNode.create("FONE")
						.withValue("PLACE_PHONETIC_VARIATION1")
						.addChildValue("TYPE", "PHONETIC_TYPE")
					)
					.addChild(GedcomNode.create("ROMN")
						.withValue("PLACE_ROMANIZED_VARIATION")
						.addChildValue("TYPE", "ROMANIZED_TYPE")
					)
					.addChild(GedcomNode.create("MAP")
						.addChildValue("LATI", "PLACE_LATITUDE1")
						.addChildValue("LONG", "PLACE_LONGITUDE1")
					)
					.addChildReference("NOTE", "N-12")
				)
				.addChild(GedcomNode.create("ADDR")
					.withValue("ADDRESS_LINE2")
					.addChildValue("CONT", "ADDRESS_LINE2")
					.addChildValue("ADR1", "ADDRESS_LINE21")
					.addChildValue("ADR2", "ADDRESS_LINE22")
					.addChildValue("ADR3", "ADDRESS_LINE23")
					.addChildValue("CITY", "ADDRESS_CITY2")
					.addChildValue("STAE", "ADDRESS_STATE2")
					.addChildValue("POST", "ADDRESS_POSTAL_CODE2")
					.addChildValue("CTRY", "ADDRESS_COUNTRY2")
				)
				.addChildValue("PHON", "PHONE_NUMBER2")
				.addChildValue("EMAIL", "ADDRESS_EMAIL2")
				.addChildValue("FAX", "ADDRESS_FAX2")
				.addChildValue("WWW", "ADDRESS_WEB_PAGE2")
				.addChildValue("AGNC", "RESPONSIBLE_AGENCY2")
				.addChildValue("RELI", "RELIGIOUS_AFFILIATION2")
				.addChildValue("CAUS", "CAUSE_OF_EVENT2")
				.addChildValue("RESN", "RESTRICTION_NOTICE2")
				.addChildReference("NOTE", "N-13")
				.addChildReference("SOUR", "S-5")
				.addChildReference("OBJE", "D-5")
				.addChildValue("AGE", "AGE_AT_EVENT")
			)
			.addChild(GedcomNode.create("BURI")
				.addChildValue("TYPE", "EVENT_OR_FACT_CLASSIFICATION3")
				.addChildValue("DATE", "DATE_VALUE3")
				.addChild(GedcomNode.create("PLAC")
					.addChildValue("FORM", "PLACE_HIERARCHY1")
					.addChild(GedcomNode.create("FONE")
						.withValue("PLACE_PHONETIC_VARIATION1")
						.addChildValue("TYPE", "PHONETIC_TYPE")
					)
					.addChild(GedcomNode.create("ROMN")
						.withValue("PLACE_ROMANIZED_VARIATION")
						.addChildValue("TYPE", "ROMANIZED_TYPE")
					)
					.addChild(GedcomNode.create("MAP")
						.addChildValue("LATI", "PLACE_LATITUDE1")
						.addChildValue("LONG", "PLACE_LONGITUDE1")
					)
					.addChildReference("NOTE", "N-12")
				)
				.addChild(GedcomNode.create("ADDR")
					.withValue("ADDRESS_LINE3")
					.addChildValue("CONT", "ADDRESS_LINE3")
					.addChildValue("ADR1", "ADDRESS_LINE31")
					.addChildValue("ADR2", "ADDRESS_LINE32")
					.addChildValue("ADR3", "ADDRESS_LINE33")
					.addChildValue("CITY", "ADDRESS_CITY3")
					.addChildValue("STAE", "ADDRESS_STATE3")
					.addChildValue("POST", "ADDRESS_POSTAL_CODE3")
					.addChildValue("CTRY", "ADDRESS_COUNTRY3")
				)
				.addChildValue("PHON", "PHONE_NUMBER3")
				.addChildValue("EMAIL", "ADDRESS_EMAIL3")
				.addChildValue("FAX", "ADDRESS_FAX3")
				.addChildValue("WWW", "ADDRESS_WEB_PAGE3")
				.addChildValue("AGNC", "RESPONSIBLE_AGENCY3")
				.addChildValue("RELI", "RELIGIOUS_AFFILIATION3")
				.addChildValue("CAUS", "CAUSE_OF_EVENT3")
				.addChildValue("RESN", "RESTRICTION_NOTICE3")
				.addChildReference("NOTE", "N-13")
				.addChildReference("SOUR", "S-6")
				.addChildReference("OBJE", "D-6")
				.addChildValue("AGE", "AGE_AT_EVENT")
			)
			.addChild(GedcomNode.create("ADOP")
				.addChildValue("TYPE", "EVENT_OR_FACT_CLASSIFICATION4")
				.addChildValue("DATE", "DATE_VALUE4")
				.addChild(GedcomNode.create("PLAC")
					.addChildValue("FORM", "PLACE_HIERARCHY1")
					.addChild(GedcomNode.create("FONE")
						.withValue("PLACE_PHONETIC_VARIATION1")
						.addChildValue("TYPE", "PHONETIC_TYPE")
					)
					.addChild(GedcomNode.create("ROMN")
						.withValue("PLACE_ROMANIZED_VARIATION")
						.addChildValue("TYPE", "ROMANIZED_TYPE")
					)
					.addChild(GedcomNode.create("MAP")
						.addChildValue("LATI", "PLACE_LATITUDE1")
						.addChildValue("LONG", "PLACE_LONGITUDE1")
					)
					.addChildReference("NOTE", "N-12")
				)
				.addChild(GedcomNode.create("ADDR")
					.withValue("ADDRESS_LINE4")
					.addChildValue("CONT", "ADDRESS_LINE4")
					.addChildValue("ADR1", "ADDRESS_LINE41")
					.addChildValue("ADR2", "ADDRESS_LINE42")
					.addChildValue("ADR3", "ADDRESS_LINE43")
					.addChildValue("CITY", "ADDRESS_CITY4")
					.addChildValue("STAE", "ADDRESS_STATE4")
					.addChildValue("POST", "ADDRESS_POSTAL_CODE4")
					.addChildValue("CTRY", "ADDRESS_COUNTRY4")
				)
				.addChildValue("PHON", "PHONE_NUMBER4")
				.addChildValue("EMAIL", "ADDRESS_EMAIL4")
				.addChildValue("FAX", "ADDRESS_FAX4")
				.addChildValue("WWW", "ADDRESS_WEB_PAGE4")
				.addChildValue("AGNC", "RESPONSIBLE_AGENCY4")
				.addChildValue("RELI", "RELIGIOUS_AFFILIATION4")
				.addChildValue("CAUS", "CAUSE_OF_EVENT4")
				.addChildValue("RESN", "RESTRICTION_NOTICE4")
				.addChildReference("NOTE", "N-12")
				.addChildReference("SOUR", "S-7")
				.addChildReference("OBJE", "D-7")
				.addChildValue("AGE", "AGE_AT_EVENT")
				.addChild(GedcomNode.create("FAMC")
					.withID("F4")
					.addChildValue("ADOP", "ADOPTED_BY_WHICH_PARENT")
				)
			)
			.addChild(GedcomNode.create("DSCR")
				.withValue("PHYSICAL_DESCRIPTION")
				.addChildValue("TYPE", "EVENT_OR_FACT_CLASSIFICATION5")
				.addChildValue("DATE", "DATE_VALUE5")
				.addChild(GedcomNode.create("PLAC")
					.addChildValue("FORM", "PLACE_HIERARCHY1")
					.addChild(GedcomNode.create("FONE")
						.withValue("PLACE_PHONETIC_VARIATION1")
						.addChildValue("TYPE", "PHONETIC_TYPE")
					)
					.addChild(GedcomNode.create("ROMN")
						.withValue("PLACE_ROMANIZED_VARIATION")
						.addChildValue("TYPE", "ROMANIZED_TYPE")
					)
					.addChild(GedcomNode.create("MAP")
						.addChildValue("LATI", "PLACE_LATITUDE1")
						.addChildValue("LONG", "PLACE_LONGITUDE1")
					)
					.addChildReference("NOTE", "N-12")
				)
				.addChild(GedcomNode.create("ADDR")
					.withValue("ADDRESS_LINE5")
					.addChildValue("CONT", "ADDRESS_LINE5")
					.addChildValue("ADR1", "ADDRESS_LINE51")
					.addChildValue("ADR2", "ADDRESS_LINE52")
					.addChildValue("ADR3", "ADDRESS_LINE53")
					.addChildValue("CITY", "ADDRESS_CITY5")
					.addChildValue("STAE", "ADDRESS_STATE5")
					.addChildValue("POST", "ADDRESS_POSTAL_CODE5")
					.addChildValue("CTRY", "ADDRESS_COUNTRY5")
				)
				.addChildValue("PHON", "PHONE_NUMBER5")
				.addChildValue("EMAIL", "ADDRESS_EMAIL5")
				.addChildValue("FAX", "ADDRESS_FAX5")
				.addChildValue("WWW", "ADDRESS_WEB_PAGE5")
				.addChildValue("AGNC", "RESPONSIBLE_AGENCY5")
				.addChildValue("RELI", "RELIGIOUS_AFFILIATION5")
				.addChildValue("CAUS", "CAUSE_OF_EVENT5")
				.addChildValue("RESN", "RESTRICTION_NOTICE5")
				.addChildReference("NOTE", "N-12")
				.addChildReference("SOUR", "S-8")
				.addChildReference("OBJE", "D-8")
				.addChildValue("AGE", "AGE_AT_EVENT")
			)
			.addChild(GedcomNode.create("RESI")
				.addChildValue("TYPE", "EVENT_OR_FACT_CLASSIFICATION6")
				.addChildValue("DATE", "DATE_VALUE6")
			)
			.addChild(GedcomNode.create("FAMC")
				.withValue("F2")
				.addChildValue("PEDI", "PEDIGREE_LINKAGE_TYPE")
				.addChildValue("STAT", "CHILD_LINKAGE_STATUS")
				.addChildReference("NOTE", "N-8")
				.addChildValue("NOTE", "SUBMITTER_TEXT8")
			)
			.addChild(GedcomNode.create("FAMS")
				.withValue("F1")
				.addChildReference("NOTE", "N-7")
				.addChildValue("NOTE", "SUBMITTER_TEXT7")
			)
			.addChildReference("SUBM", "SUBM1")
			.addChild(GedcomNode.create("ASSO")
				.withID("I4")
				.addChildValue("RELA", "RELATION_IS_DESCRIPTOR")
				.addChild(GedcomNode.create("SOUR")
					.withID("S-1")
					.addChildValue("PAGE", "WHERE_WITHIN_SOURCE")
					.addChild(GedcomNode.create("EVEN")
						.withValue("EVENT_TYPE_CITED_FROM")
						.addChildValue("ROLE", "ROLE_IN_EVENT")
					)
					.addChild(GedcomNode.create("DATA")
						.addChildValue("DATE", "ENTRY_RECORDING_DATE")
						.addChildValue("TEXT", "TEXT_FROM_SOURCE")
					)
					.addChildReference("OBJE", "D-9")
					.addChild(GedcomNode.create("OBJE")
						.addChildValue("TITL", "DESCRIPTIVE_TITLE3")
						.addChild(GedcomNode.create("FORM")
							.withValue("MULTIMEDIA_FORMAT3")
							.addChildValue("MEDI", "SOURCE_MEDIA_TYPE3")
						)
						.addChildValue("FILE", "MULTIMEDIA_FILE_REFN3")
					)
					.addChildReference("NOTE", "N-5")
					.addChildValue("NOTE", "SUBMITTER_TEXT5")
					.addChildValue("QUAY", "CERTAINTY_ASSESSMENT5")
				)
				.addChild(GedcomNode.create("SOUR")
					.addChildValue("TEXT", "TEXT_FROM_SOURCE9")
					.addChildReference("OBJE", "D-10")
					.addChild(GedcomNode.create("OBJE")
						.addChildValue("TITL", "DESCRIPTIVE_TITLE4")
						.addChild(GedcomNode.create("FORM")
							.withValue("MULTIMEDIA_FORMAT4")
							.addChildValue("MEDI", "SOURCE_MEDIA_TYPE4")
						)
						.addChildValue("FILE", "MULTIMEDIA_FILE_REFN4")
					)
					.addChildReference("NOTE", "N-6")
					.addChildValue("NOTE", "SUBMITTER_TEXT6")
					.addChildValue("QUAY", "CERTAINTY_ASSESSMENT6")
				)
				.addChildReference("NOTE", "N-4")
				.addChildValue("NOTE", "SUBMITTER_TEXT4")
			)
			.addChildReference("ALIA", "I2")
			.addChildReference("ALIA", "I3")
			.addChildReference("NOTE", "N-3")
			.addChildValue("NOTE", "SUBMITTER_TEXT3")
			.addChild(GedcomNode.create("SOUR")
				.withID("S-2")
				.addChildValue("PAGE", "WHERE_WITHIN_SOURCE")
				.addChild(GedcomNode.create("EVEN")
					.withValue("EVENT_TYPE_CITED_FROM")
					.addChildValue("ROLE", "ROLE_IN_EVENT")
				)
				.addChild(GedcomNode.create("DATA")
					.addChildValue("DATE", "ENTRY_RECORDING_DATE")
					.addChildValue("TEXT", "TEXT_FROM_SOURCE")
				)
				.addChildReference("OBJE", "D-11")
				.addChild(GedcomNode.create("OBJE")
					.addChildValue("TITL", "DESCRIPTIVE_TITLE2")
					.addChild(GedcomNode.create("FORM")
						.withValue("MULTIMEDIA_FORMAT2")
						.addChildValue("MEDI", "SOURCE_MEDIA_TYPE2")
					)
					.addChildValue("FILE", "MULTIMEDIA_FILE_REFN2")
				)
				.addChildReference("NOTE", "N-1")
				.addChildValue("NOTE", "SUBMITTER_TEXT1")
				.addChildValue("QUAY", "CERTAINTY_ASSESSMENT-2")
			)
			.addChild(GedcomNode.create("SOUR")
				.addChildValue("TEXT", "TEXT_FROM_SOURCE10")
				.addChildReference("OBJE", "D-12")
				.addChild(GedcomNode.create("OBJE")
					.addChildValue("TITL", "DESCRIPTIVE_TITLE3")
					.addChild(GedcomNode.create("FORM")
						.withValue("MULTIMEDIA_FORMAT3")
						.addChildValue("MEDI", "SOURCE_MEDIA_TYPE3")
					)
					.addChildValue("FILE", "MULTIMEDIA_FILE_REFN3")
				)
				.addChildReference("NOTE", "N-2")
				.addChildValue("NOTE", "SUBMITTER_TEXT2")
				.addChildValue("QUAY", "CERTAINTY_ASSESSMENT-12")
			)
			.addChildReference("OBJE", "D-13")
			.addChild(GedcomNode.create("OBJE")
				.addChildValue("TITL", "DESCRIPTIVE_TITLE1")
				.addChild(GedcomNode.create("FORM")
					.withValue("MULTIMEDIA_FORMAT1")
					.addChildValue("MEDI", "SOURCE_MEDIA_TYPE1")
				)
				.addChildValue("FILE", "MULTIMEDIA_FILE_REFN1")
			);
		final Gedcom origin = new Gedcom();
		origin.addIndividual(individual);
		final Flef destination = new Flef();

		Assertions.assertEquals("id: I1, tag: INDI, children: [{tag: RESN, value: RESTRICTION_NOTICE}, {tag: NAME, value: NAME_PERSONAL, children: [{tag: TYPE, value: NAME_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX1}, {tag: GIVN, value: NAME_PIECE_GIVEN1}, {tag: NICK, value: NAME_PIECE_NICKNAME1}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX1}, {tag: SURN, value: NAME_PIECE_SURNAME1}, {tag: NSFX, value: NAME_PIECE_SUFFIX1}, {id: N-10, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT10}, {tag: SOUR, children: [{tag: TEXT, value: TEXT_FROM_SOURCE1}, {id: D-1, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE7}, {tag: FORM, value: MULTIMEDIA_FORMAT7, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE7}]}, {tag: FILE, value: MULTIMEDIA_FILE_REFN7}]}, {id: N-12, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT12}, {tag: QUAY, value: CERTAINTY_ASSESSMENT12}]}, {tag: FONE, value: NAME_PHONETIC_VARIATION, children: [{tag: TYPE, value: PHONETIC_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX2}, {tag: GIVN, value: NAME_PIECE_GIVEN2}, {tag: NICK, value: NAME_PIECE_NICKNAME2}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX2}, {tag: SURN, value: NAME_PIECE_SURNAME2}, {tag: NSFX, value: NAME_PIECE_SUFFIX2}, {id: N-9, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT9}, {tag: SOUR, children: [{tag: TEXT, value: TEXT_FROM_SOURCE2}, {id: D-2, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE6}, {tag: FORM, value: MULTIMEDIA_FORMAT6, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE6}]}, {tag: FILE, value: MULTIMEDIA_FILE_REFN6}]}, {id: N-12, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT12}, {tag: QUAY, value: CERTAINTY_ASSESSMENT12}]}]}, {tag: ROMN, value: NAME_ROMANIZED_VARIATION, children: [{tag: TYPE, value: ROMANIZED_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX3}, {tag: GIVN, value: NAME_PIECE_GIVEN3}, {tag: NICK, value: NAME_PIECE_NICKNAME3}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX3}, {tag: SURN, value: NAME_PIECE_SURNAME3}, {tag: NSFX, value: NAME_PIECE_SUFFIX3}, {id: N-8, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT8}, {tag: SOUR, children: [{tag: TEXT, value: TEXT_FROM_SOURCE3}, {id: D-3, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE5}, {tag: FORM, value: MULTIMEDIA_FORMAT5, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE5}]}, {tag: FILE, value: MULTIMEDIA_FILE_REFN5}]}, {id: N-11, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT11}, {tag: QUAY, value: CERTAINTY_ASSESSMENT11}]}]}]}, {tag: SEX, value: SEX_VALUE}, {tag: BIRT, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION1}, {tag: DATE, value: DATE_VALUE1}, {tag: PLAC, children: [{tag: FORM, value: PLACE_HIERARCHY1}, {tag: FONE, value: PLACE_PHONETIC_VARIATION1, children: [{tag: TYPE, value: PHONETIC_TYPE}]}, {tag: ROMN, value: PLACE_ROMANIZED_VARIATION, children: [{tag: TYPE, value: ROMANIZED_TYPE}]}, {tag: MAP, children: [{tag: LATI, value: PLACE_LATITUDE1}, {tag: LONG, value: PLACE_LONGITUDE1}]}, {id: N-12, tag: NOTE}]}, {tag: ADDR, value: ADDRESS_LINE1, children: [{tag: CONT, value: ADDRESS_LINE1}, {tag: ADR1, value: ADDRESS_LINE11}, {tag: ADR2, value: ADDRESS_LINE12}, {tag: ADR3, value: ADDRESS_LINE13}, {tag: CITY, value: ADDRESS_CITY1}, {tag: STAE, value: ADDRESS_STATE1}, {tag: POST, value: ADDRESS_POSTAL_CODE1}, {tag: CTRY, value: ADDRESS_COUNTRY1}]}, {tag: PHON, value: PHONE_NUMBER1}, {tag: EMAIL, value: ADDRESS_EMAIL1}, {tag: FAX, value: ADDRESS_FAX1}, {tag: WWW, value: ADDRESS_WEB_PAGE1}, {tag: AGNC, value: RESPONSIBLE_AGENCY1}, {tag: RELI, value: RELIGIOUS_AFFILIATION1}, {tag: CAUS, value: CAUSE_OF_EVENT1}, {tag: RESN, value: RESTRICTION_NOTICE1}, {id: N-12, tag: NOTE}, {id: S-4, tag: SOUR}, {id: D-4, tag: OBJE}, {tag: AGE, value: AGE_AT_EVENT}, {id: F3, tag: FAMC}]}, {tag: DEAT, value: Y, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION2}, {tag: DATE, value: DATE_VALUE2}, {tag: PLAC, children: [{tag: FORM, value: PLACE_HIERARCHY1}, {tag: FONE, value: PLACE_PHONETIC_VARIATION1, children: [{tag: TYPE, value: PHONETIC_TYPE}]}, {tag: ROMN, value: PLACE_ROMANIZED_VARIATION, children: [{tag: TYPE, value: ROMANIZED_TYPE}]}, {tag: MAP, children: [{tag: LATI, value: PLACE_LATITUDE1}, {tag: LONG, value: PLACE_LONGITUDE1}]}, {id: N-12, tag: NOTE}]}, {tag: ADDR, value: ADDRESS_LINE2, children: [{tag: CONT, value: ADDRESS_LINE2}, {tag: ADR1, value: ADDRESS_LINE21}, {tag: ADR2, value: ADDRESS_LINE22}, {tag: ADR3, value: ADDRESS_LINE23}, {tag: CITY, value: ADDRESS_CITY2}, {tag: STAE, value: ADDRESS_STATE2}, {tag: POST, value: ADDRESS_POSTAL_CODE2}, {tag: CTRY, value: ADDRESS_COUNTRY2}]}, {tag: PHON, value: PHONE_NUMBER2}, {tag: EMAIL, value: ADDRESS_EMAIL2}, {tag: FAX, value: ADDRESS_FAX2}, {tag: WWW, value: ADDRESS_WEB_PAGE2}, {tag: AGNC, value: RESPONSIBLE_AGENCY2}, {tag: RELI, value: RELIGIOUS_AFFILIATION2}, {tag: CAUS, value: CAUSE_OF_EVENT2}, {tag: RESN, value: RESTRICTION_NOTICE2}, {id: N-13, tag: NOTE}, {id: S-5, tag: SOUR}, {id: D-5, tag: OBJE}, {tag: AGE, value: AGE_AT_EVENT}]}, {tag: BURI, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION3}, {tag: DATE, value: DATE_VALUE3}, {tag: PLAC, children: [{tag: FORM, value: PLACE_HIERARCHY1}, {tag: FONE, value: PLACE_PHONETIC_VARIATION1, children: [{tag: TYPE, value: PHONETIC_TYPE}]}, {tag: ROMN, value: PLACE_ROMANIZED_VARIATION, children: [{tag: TYPE, value: ROMANIZED_TYPE}]}, {tag: MAP, children: [{tag: LATI, value: PLACE_LATITUDE1}, {tag: LONG, value: PLACE_LONGITUDE1}]}, {id: N-12, tag: NOTE}]}, {tag: ADDR, value: ADDRESS_LINE3, children: [{tag: CONT, value: ADDRESS_LINE3}, {tag: ADR1, value: ADDRESS_LINE31}, {tag: ADR2, value: ADDRESS_LINE32}, {tag: ADR3, value: ADDRESS_LINE33}, {tag: CITY, value: ADDRESS_CITY3}, {tag: STAE, value: ADDRESS_STATE3}, {tag: POST, value: ADDRESS_POSTAL_CODE3}, {tag: CTRY, value: ADDRESS_COUNTRY3}]}, {tag: PHON, value: PHONE_NUMBER3}, {tag: EMAIL, value: ADDRESS_EMAIL3}, {tag: FAX, value: ADDRESS_FAX3}, {tag: WWW, value: ADDRESS_WEB_PAGE3}, {tag: AGNC, value: RESPONSIBLE_AGENCY3}, {tag: RELI, value: RELIGIOUS_AFFILIATION3}, {tag: CAUS, value: CAUSE_OF_EVENT3}, {tag: RESN, value: RESTRICTION_NOTICE3}, {id: N-13, tag: NOTE}, {id: S-6, tag: SOUR}, {id: D-6, tag: OBJE}, {tag: AGE, value: AGE_AT_EVENT}]}, {tag: ADOP, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION4}, {tag: DATE, value: DATE_VALUE4}, {tag: PLAC, children: [{tag: FORM, value: PLACE_HIERARCHY1}, {tag: FONE, value: PLACE_PHONETIC_VARIATION1, children: [{tag: TYPE, value: PHONETIC_TYPE}]}, {tag: ROMN, value: PLACE_ROMANIZED_VARIATION, children: [{tag: TYPE, value: ROMANIZED_TYPE}]}, {tag: MAP, children: [{tag: LATI, value: PLACE_LATITUDE1}, {tag: LONG, value: PLACE_LONGITUDE1}]}, {id: N-12, tag: NOTE}]}, {tag: ADDR, value: ADDRESS_LINE4, children: [{tag: CONT, value: ADDRESS_LINE4}, {tag: ADR1, value: ADDRESS_LINE41}, {tag: ADR2, value: ADDRESS_LINE42}, {tag: ADR3, value: ADDRESS_LINE43}, {tag: CITY, value: ADDRESS_CITY4}, {tag: STAE, value: ADDRESS_STATE4}, {tag: POST, value: ADDRESS_POSTAL_CODE4}, {tag: CTRY, value: ADDRESS_COUNTRY4}]}, {tag: PHON, value: PHONE_NUMBER4}, {tag: EMAIL, value: ADDRESS_EMAIL4}, {tag: FAX, value: ADDRESS_FAX4}, {tag: WWW, value: ADDRESS_WEB_PAGE4}, {tag: AGNC, value: RESPONSIBLE_AGENCY4}, {tag: RELI, value: RELIGIOUS_AFFILIATION4}, {tag: CAUS, value: CAUSE_OF_EVENT4}, {tag: RESN, value: RESTRICTION_NOTICE4}, {id: N-12, tag: NOTE}, {id: S-7, tag: SOUR}, {id: D-7, tag: OBJE}, {tag: AGE, value: AGE_AT_EVENT}, {id: F4, tag: FAMC, children: [{tag: ADOP, value: ADOPTED_BY_WHICH_PARENT}]}]}, {tag: DSCR, value: PHYSICAL_DESCRIPTION, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION5}, {tag: DATE, value: DATE_VALUE5}, {tag: PLAC, children: [{tag: FORM, value: PLACE_HIERARCHY1}, {tag: FONE, value: PLACE_PHONETIC_VARIATION1, children: [{tag: TYPE, value: PHONETIC_TYPE}]}, {tag: ROMN, value: PLACE_ROMANIZED_VARIATION, children: [{tag: TYPE, value: ROMANIZED_TYPE}]}, {tag: MAP, children: [{tag: LATI, value: PLACE_LATITUDE1}, {tag: LONG, value: PLACE_LONGITUDE1}]}, {id: N-12, tag: NOTE}]}, {tag: ADDR, value: ADDRESS_LINE5, children: [{tag: CONT, value: ADDRESS_LINE5}, {tag: ADR1, value: ADDRESS_LINE51}, {tag: ADR2, value: ADDRESS_LINE52}, {tag: ADR3, value: ADDRESS_LINE53}, {tag: CITY, value: ADDRESS_CITY5}, {tag: STAE, value: ADDRESS_STATE5}, {tag: POST, value: ADDRESS_POSTAL_CODE5}, {tag: CTRY, value: ADDRESS_COUNTRY5}]}, {tag: PHON, value: PHONE_NUMBER5}, {tag: EMAIL, value: ADDRESS_EMAIL5}, {tag: FAX, value: ADDRESS_FAX5}, {tag: WWW, value: ADDRESS_WEB_PAGE5}, {tag: AGNC, value: RESPONSIBLE_AGENCY5}, {tag: RELI, value: RELIGIOUS_AFFILIATION5}, {tag: CAUS, value: CAUSE_OF_EVENT5}, {tag: RESN, value: RESTRICTION_NOTICE5}, {id: N-12, tag: NOTE}, {id: S-8, tag: SOUR}, {id: D-8, tag: OBJE}, {tag: AGE, value: AGE_AT_EVENT}]}, {tag: RESI, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION6}, {tag: DATE, value: DATE_VALUE6}]}, {tag: FAMC, value: F2, children: [{tag: PEDI, value: PEDIGREE_LINKAGE_TYPE}, {tag: STAT, value: CHILD_LINKAGE_STATUS}, {id: N-8, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT8}]}, {tag: FAMS, value: F1, children: [{id: N-7, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT7}]}, {id: SUBM1, tag: SUBM}, {id: I4, tag: ASSO, children: [{tag: RELA, value: RELATION_IS_DESCRIPTOR}, {id: S-1, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE}]}, {id: D-9, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE3}, {tag: FORM, value: MULTIMEDIA_FORMAT3, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE3}]}, {tag: FILE, value: MULTIMEDIA_FILE_REFN3}]}, {id: N-5, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT5}, {tag: QUAY, value: CERTAINTY_ASSESSMENT5}]}, {tag: SOUR, children: [{tag: TEXT, value: TEXT_FROM_SOURCE9}, {id: D-10, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE4}, {tag: FORM, value: MULTIMEDIA_FORMAT4, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE4}]}, {tag: FILE, value: MULTIMEDIA_FILE_REFN4}]}, {id: N-6, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT6}, {tag: QUAY, value: CERTAINTY_ASSESSMENT6}]}, {id: N-4, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT4}]}, {id: I2, tag: ALIA}, {id: I3, tag: ALIA}, {id: N-3, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT3}, {id: S-2, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE}]}, {id: D-11, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE2}, {tag: FORM, value: MULTIMEDIA_FORMAT2, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE2}]}, {tag: FILE, value: MULTIMEDIA_FILE_REFN2}]}, {id: N-1, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT1}, {tag: QUAY, value: CERTAINTY_ASSESSMENT-2}]}, {tag: SOUR, children: [{tag: TEXT, value: TEXT_FROM_SOURCE10}, {id: D-12, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE3}, {tag: FORM, value: MULTIMEDIA_FORMAT3, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE3}]}, {tag: FILE, value: MULTIMEDIA_FILE_REFN3}]}, {id: N-2, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT2}, {tag: QUAY, value: CERTAINTY_ASSESSMENT-12}]}, {id: D-13, tag: OBJE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE1}, {tag: FORM, value: MULTIMEDIA_FORMAT1, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE1}]}, {tag: FILE, value: MULTIMEDIA_FILE_REFN1}]}]", origin.getIndividuals().get(0).toString());

		final Transformation<Gedcom, Flef> t = new IndividualTransformation();
		t.to(origin, destination);

		Assertions.assertEquals("id: I1, tag: INDIVIDUAL, children: [{tag: NAME, children: [{tag: TYPE, value: NAME_TYPE}, {tag: TITLE, value: NAME_PIECE_PREFIX1}, {tag: PERSONAL_NAME, value: NAME_PIECE_GIVEN1, children: [{tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX1}]}, {tag: INDIVIDUAL_NICKNAME, value: NAME_PIECE_NICKNAME1}, {tag: FAMILY_NAME, value: NAME_PIECE_SURNAME_PREFIX1 NAME_PIECE_SURNAME1}, {id: N-10, tag: NOTE}, {id: N1, tag: NOTE}, {id: S1, tag: SOURCE, children: [{tag: CREDIBILITY, value: CERTAINTY_ASSESSMENT12}]}, {tag: PHONETIC, children: [{tag: TYPE, value: PHONETIC_TYPE}, {tag: TITLE, value: NAME_PIECE_PREFIX2}, {tag: PERSONAL_NAME, value: NAME_PIECE_GIVEN2, children: [{tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX2}]}, {tag: INDIVIDUAL_NICKNAME, value: NAME_PIECE_NICKNAME2}, {tag: FAMILY_NAME, value: NAME_PIECE_SURNAME_PREFIX2 NAME_PIECE_SURNAME2}, {id: N-9, tag: NOTE}, {id: N4, tag: NOTE}, {id: S3, tag: SOURCE, children: [{tag: CREDIBILITY, value: CERTAINTY_ASSESSMENT12}]}]}, {tag: TRANSCRIPTION, children: [{tag: TYPE, value: ROMANIZED_TYPE}, {tag: TITLE, value: NAME_PIECE_PREFIX3}, {tag: PERSONAL_NAME, value: NAME_PIECE_GIVEN3, children: [{tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX3}]}, {tag: INDIVIDUAL_NICKNAME, value: NAME_PIECE_NICKNAME3}, {tag: FAMILY_NAME, value: NAME_PIECE_SURNAME_PREFIX3 NAME_PIECE_SURNAME3}, {id: N-8, tag: NOTE}, {id: N7, tag: NOTE}, {id: S5, tag: SOURCE, children: [{tag: CREDIBILITY, value: CERTAINTY_ASSESSMENT11}]}]}]}, {tag: SEX, value: SEX_VALUE}, {tag: FAMILY_CHILD, children: [{tag: PEDIGREE, value: PEDIGREE_LINKAGE_TYPE}, {tag: CERTAINTY, value: CHILD_LINKAGE_STATUS}, {id: N-8, tag: NOTE}, {id: N10, tag: NOTE}]}, {tag: FAMILY_SPOUSE, children: [{id: N-7, tag: NOTE}, {id: N11, tag: NOTE}]}, {id: I4, tag: ASSOCIATION, children: [{tag: RELATIONSHIP, value: RELATION_IS_DESCRIPTOR}, {id: N-4, tag: NOTE}, {id: N12, tag: NOTE}, {id: S-1, tag: SOURCE, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: ROLE, value: ROLE_IN_EVENT}, {tag: CREDIBILITY, value: CERTAINTY_ASSESSMENT5}]}, {id: S9, tag: SOURCE, children: [{tag: CREDIBILITY, value: CERTAINTY_ASSESSMENT6}]}]}, {tag: EVENT, value: BIRTH, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION1}, {tag: DATE, value: DATE_VALUE1}, {id: P1, tag: PLACE}, {tag: AGENCY, value: RESPONSIBLE_AGENCY1}, {tag: CAUSE, value: CAUSE_OF_EVENT1}, {id: N-12, tag: NOTE}, {id: S-4, tag: SOURCE}, {id: D-4, tag: SOURCE}, {tag: RESTRICTION, value: RESTRICTION_NOTICE1}, {id: F3, tag: FAMILY_CHILD}]}, {tag: EVENT, value: ADOPTION, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION4}, {tag: DATE, value: DATE_VALUE4}, {id: P2, tag: PLACE}, {tag: AGENCY, value: RESPONSIBLE_AGENCY4}, {tag: CAUSE, value: CAUSE_OF_EVENT4}, {id: N-12, tag: NOTE}, {id: S-7, tag: SOURCE}, {id: D-7, tag: SOURCE}, {tag: RESTRICTION, value: RESTRICTION_NOTICE4}, {id: F4, tag: FAMILY_CHILD, children: [{tag: ADOPTED_BY, value: ADOPTED_BY_WHICH_PARENT}]}]}, {tag: EVENT, value: DEATH, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION2}, {tag: DATE, value: DATE_VALUE2}, {id: P3, tag: PLACE}, {tag: AGENCY, value: RESPONSIBLE_AGENCY2}, {tag: CAUSE, value: CAUSE_OF_EVENT2}, {id: N-13, tag: NOTE}, {id: S-5, tag: SOURCE}, {id: D-5, tag: SOURCE}, {tag: RESTRICTION, value: RESTRICTION_NOTICE2}]}, {tag: EVENT, value: BURIAL, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION3}, {tag: DATE, value: DATE_VALUE3}, {id: P4, tag: PLACE}, {tag: AGENCY, value: RESPONSIBLE_AGENCY3}, {tag: CAUSE, value: CAUSE_OF_EVENT3}, {id: N-13, tag: NOTE}, {id: S-6, tag: SOURCE}, {id: D-6, tag: SOURCE}, {tag: RESTRICTION, value: RESTRICTION_NOTICE3}]}, {tag: ATTRIBUTE, value: PHYSICAL_DESCRIPTION, children: [{tag: VALUE, value: PHYSICAL_DESCRIPTION}, {tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION5}, {id: P5, tag: PLACE}, {tag: AGENCY, value: RESPONSIBLE_AGENCY5}, {tag: CAUSE, value: CAUSE_OF_EVENT5}, {tag: RESTRICTION, value: RESTRICTION_NOTICE5}, {id: N-12, tag: NOTE}, {id: S-8, tag: SOURCE}, {id: D-8, tag: SOURCE}]}, {tag: ATTRIBUTE, value: RESIDENCE, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION6}, {id: P6, tag: PLACE}]}, {id: N-3, tag: NOTE}, {id: N22, tag: NOTE}, {id: S-2, tag: SOURCE, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: ROLE, value: ROLE_IN_EVENT}, {tag: CREDIBILITY, value: CERTAINTY_ASSESSMENT-2}]}, {id: S18, tag: SOURCE, children: [{tag: CREDIBILITY, value: CERTAINTY_ASSESSMENT-12}]}, {id: D-13, tag: SOURCE}, {id: S20, tag: SOURCE}, {tag: RESTRICTION, value: RESTRICTION_NOTICE}]", destination.getIndividuals().get(0).toString());
	}

	@Test
	void from(){
		final GedcomNode individual = GedcomNode.create("INDIVIDUAL")
			.withID("I1")
			.addChild(GedcomNode.create("NAME")
				.addChildValue("TYPE", "NAME_TYPE")
				.addChildValue("LOCALE", "en-US")
				.addChildValue("TITLE", "TITLE_PIECE1")
				.addChild(GedcomNode.create("PERSONAL_NAME")
					.withValue("NAME_PIECE1")
					.addChildValue("NAME_SUFFIX", "NAME_PIECE_SUFFIX1")
				)
				.addChildValue("INDIVIDUAL_NICKNAME", "NAME_PIECE_NICKNAME1")
				.addChildValue("FAMILY_NAME", "SURNAME_PIECE1")
				.addChildValue("FAMILY_NICKNAME", "SURNAME_PIECE_NICKNAME1")
				.addChild(GedcomNode.create("PHONETIC")
					.addChildValue("TITLE", "TITLE_PIECE2")
					.addChild(GedcomNode.create("PERSONAL_NAME")
						.withValue("NAME_PIECE2")
						.addChildValue("NAME_SUFFIX", "NAME_PIECE_SUFFIX2")
					)
					.addChildValue("INDIVIDUAL_NICKNAME", "NAME_PIECE_NICKNAME2")
					.addChildValue("FAMILY_NAME", "SURNAME_PIECE2")
					.addChildValue("FAMILY_NICKNAME", "SURNAME_PIECE_NICKNAME2")
				)
				.addChild(GedcomNode.create("TRANSCRIPTION")
					.withValue("TRANSCRIPTION_TYPE")
					.addChildValue("SYSTEM", "TRANSCRIPTION_SYSTEM")
					.addChildValue("TITLE", "TITLE_PIECE3")
					.addChild(GedcomNode.create("PERSONAL_NAME")
						.withValue("NAME_PIECE3")
						.addChildValue("NAME_SUFFIX", "NAME_PIECE_SUFFIX3")
					)
					.addChildValue("INDIVIDUAL_NICKNAME", "NAME_PIECE_NICKNAME3")
					.addChildValue("FAMILY_NAME", "SURNAME_PIECE3")
					.addChildValue("FAMILY_NICKNAME", "SURNAME_PIECE_NICKNAME3")
				)
				.addChildReference("NOTE", "N1")
				.addChildReference("SOURCE", "S1")
			)
			.addChildValue("SEX", "SEX_VALUE")
			.addChild(GedcomNode.create("FAMILY_CHILD")
				.addChildValue("PEDIGREE", "PEDIGREE_LINKAGE_TYPE")
				.addChildValue("CERTAINTY", "CERTAINTY")
				.addChildValue("CREDIBILITY", "CREDIBILITY_ASSESSMENT")
				.addChildReference("NOTE", "N1")
			)
			.addChild(GedcomNode.create("FAMILY_SPOUSE")
				.addChildReference("NOTE", "N2")
			)
			.addChild(GedcomNode.create("ASSOCIATION")
				.withID("I1")
				.addChildValue("TYPE", "ASSOCIATION_TYPE")
				.addChildValue("RELATIONSHIP", "RELATION_IS_DESCRIPTOR")
				.addChildReference("NOTE", "N1")
				.addChildReference("SOURCE", "S1")
			)
			.addChild(GedcomNode.create("ALIAS")
				.withID("I1")
				.addChildReference("NOTE", "N2")
			)
			.addChild(GedcomNode.create("EVENT")
				.withValue("BIRTH")
				.addChildValue("TYPE", "EVENT_OR_FACT_CLASSIFICATION_BIRTH")
				.addChild(GedcomNode.create("DATE")
					.withValue("DATE_TIME_VALUE")
					.addChildValue("CALENDAR", "CALENDAR_TYPE")
					.addChildValue("CREDIBILITY", "CREDIBILITY_ASSESSMENT")
				)
				.addChild(GedcomNode.create("PLACE")
					.withID("P1")
					.addChildValue("CERTAINTY", "CERTAINTY_ASSESSMENT")
					.addChildValue("CREDIBILITY", "CREDIBILITY_ASSESSMENT")
				)
				.addChildValue("AGENCY", "RESPONSIBLE_AGENCY")
				.addChild(GedcomNode.create("CAUSE")
					.withValue("CAUSE_OF_EVENT")
					.addChildValue("CERTAINTY", "CERTAINTY_ASSESSMENT")
					.addChildValue("CREDIBILITY", "CREDIBILITY_ASSESSMENT")
				)
				.addChildReference("NOTE", "N1")
				.addChildReference("SOURCE", "S1")
				.addChildValue("CERTAINTY", "CERTAINTY_ASSESSMENT")
				.addChildValue("CREDIBILITY", "CREDIBILITY_ASSESSMENT")
				.addChildValue("RESTRICTION", "RESTRICTION_NOTICE")
			)
			.addChild(GedcomNode.create("EVENT")
				.withValue("ADOPTION")
				.addChildValue("TYPE", "EVENT_OR_FACT_CLASSIFICATION_ADOPTION")
				.addChild(GedcomNode.create("FAMILY_CHILD")
					.withID("F1")
					.addChildValue("ADOPTED_BY", "ADOPTED_BY_WHICH_PARENT")
				)
			)
			.addChild(GedcomNode.create("EVENT")
				.withValue("DEATH")
				.addChildValue("TYPE", "EVENT_OR_FACT_CLASSIFICATION_DEATH")
			)
			.addChild(GedcomNode.create("ATTRIBUTE")
				.withValue("CASTE")
				.addChildValue("VALUE", "CASTE_NAME")
				.addChildValue("???", "EVENT_DETAIL")
			)
			.addChild(GedcomNode.create("ATTRIBUTE")
				.withValue("PHYSICAL_DESCRIPTION")
				.addChildValue("VALUE", "PHYSICAL_DESCRIPTION")
				.addChild(GedcomNode.create("KEY")
					.withValue("KEY_DESCRIPTION1")
					.addChildValue("VALUE", "VALUE_DESCRIPTION1")
				)
				.addChild(GedcomNode.create("KEY")
					.withValue("KEY_DESCRIPTION2")
					.addChildValue("VALUE", "VALUE_DESCRIPTION2")
				)
				.addChildValue("???", "EVENT_DETAIL")
			)
			.addChild(GedcomNode.create("ATTRIBUTE")
				.withValue("RESIDENCE")
				.addChildValue("???", "EVENT_DETAIL")
			)
			.addChildReference("NOTE", "N2")
			.addChildValue("SOURCE", "S1")
			.addChildValue("RESTRICTION", "RESTRICTION_NOTICE")
		;
		final Flef origin = new Flef();
		origin.addIndividual(individual);
		origin.addNote(GedcomNode.create("NOTE", "N1", "SUBMITTER_TEXT1"));
		origin.addNote(GedcomNode.create("NOTE", "N2", "SUBMITTER_TEXT2"));
		origin.addSource(GedcomNode.create("SOURCE", "S1", null));
		origin.addPlace(GedcomNode.create("PLACE", "P1", null));
		final Gedcom destination = new Gedcom();

		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: RESTRICTION, value: RESTRICTION_NOTICE}]", origin.getIndividuals().get(0).toString());

		final Transformation<Gedcom, Flef> t = new IndividualTransformation();
		t.from(origin, destination);

		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXT", destination.getIndividuals().get(0).toString());
	}

}