/**
 * Copyright (c) 2020 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class TransformerIndividualRecordTest{

	@Test
	void individualRecordTo(){
		final Transformer transformerTo = new Transformer(Protocol.FLEF);
		final GedcomNode individual = transformerTo.createWithID("INDI", "I1")
			.addChildValue("RESN", "RESTRICTION_NOTICE")
			.addChild(transformerTo.createWithValue("NAME", "name /surname/ name_suffix")
				.addChildValue("NPFX", "NAME_PIECE_PREFIX")
				.addChildValue("GIVN", "NAME_PIECE_GIVEN")
				.addChildValue("NICK", "NAME_PIECE_NICKNAME")
				.addChildValue("SPFX", "NAME_PIECE_SURNAME_PREFIX")
				.addChildValue("SURN", "NAME_PIECE_SURNAME")
				.addChildValue("NSFX", "NAME_PIECE_SUFFIX")
				.addChildReference("NOTE", "N1")
				.addChildReference("SOURCE", "S1")
			)
			.addChild(transformerTo.create("FONE")
				.addChildValue("TYPE", "PHONETIC_TYPE")
				.addChildValue("NPFX", "NAME_PIECE_PREFIX_FONE")
				.addChildValue("GIVN", "NAME_PIECE_GIVEN_FONE")
				.addChildValue("NICK", "NAME_PIECE_NICKNAME_FONE")
				.addChildValue("SPFX", "NAME_PIECE_SURNAME_PREFIX_FONE")
				.addChildValue("SURN", "NAME_PIECE_SURNAME_FONE")
				.addChildValue("NSFX", "NAME_PIECE_SUFFIX_FONE")
				.addChildReference("NOTE", "N2")
				.addChildReference("SOURCE", "S2")
			)
			.addChild(transformerTo.create("ROMN")
				.addChildValue("TYPE", "PHONETIC_TYPE")
				.addChildValue("NPFX", "NAME_PIECE_PREFIX_ROMN")
				.addChildValue("GIVN", "NAME_PIECE_GIVEN_ROMN")
				.addChildValue("NICK", "NAME_PIECE_NICKNAME_ROMN")
				.addChildValue("SPFX", "NAME_PIECE_SURNAME_PREFIX_ROMN")
				.addChildValue("SURN", "NAME_PIECE_SURNAME_ROMN")
				.addChildValue("NSFX", "NAME_PIECE_SUFFIX_ROMN")
				.addChildReference("NOTE", "N3")
				.addChildReference("SOURCE", "S3")
			)
			.addChildValue("SEX", "SEX_VALUE")
			.addChild(transformerTo.createWithValue("BIRTH", "Y")
				.addChildValue("DATE", "DATE_VALUE1")
				.addChildReference("FAMC", "F1")
			)
			.addChild(transformerTo.create("ADOP")
				.addChildValue("DATE", "DATE_VALUE2")
				.addChild(transformerTo.createWithReference("FAMC", "F1")
					.addChildValue("ADOP", "WIFE")
				)
			)
			.addChild(transformerTo.create("EVEN")
				.addChildValue("TYPE", "EVENT_OR_FACT_CLASSIFICATION")
				.addChildValue("DATE", "DATE_VALUE3")
			)
			.addChild(transformerTo.createWithValue("TITL", "NOBILITY_TYPE_TITLE")
				.addChildValue("TYPE", "EVENT_OR_FACT_CLASSIFICATION_titl")
				.addChildValue("DATE", "DATE_VALUE4")
			)
			.addChild(transformerTo.createWithValue("FACT", "EVENT_DESCRIPTOR_fact")
				.addChildValue("TYPE", "EVENT_OR_FACT_CLASSIFICATION_fact")
				.addChildValue("DATE", "DATE_VALUE5")
			)
			.addChild(transformerTo.createWithReference("FAMC", "F1")
				.addChildValue("PEDI", "WIFE")
				.addChildValue("STAT", "CHILD_LINKAGE_STATUS")
				.addChildReference("NOTE", "N4")
			)
			.addChild(transformerTo.createWithReference("FAMS", "F1")
				.addChildReference("NOTE", "N5")
			)
			.addChildReference("SUBM", "SUBM1")
			.addChild(transformerTo.createWithReference("ASSO", "I1")
				.addChildValue("TYPE", "INDI")
				.addChildReference("NOTE", "N6")
				.addChildReference("SOUR", "S1")
			)
			.addChild(transformerTo.createWithReference("ASSO", "F1")
				.addChildValue("TYPE", "FAM")
				.addChildReference("NOTE", "N7")
				.addChildReference("SOUR", "S1")
			)
			.addChild(transformerTo.createWithReference("ALIA", "I1"))
			.addChildReference("NOTE", "N8")
			.addChildReference("SOUR", "S1")
			.addChildReference("OBJE", "O1");
		final GedcomNode family = transformerTo.createWithID("FAM", "F1")
			.addChildReference("HUSB", "I1")
			.addChildReference("WIFE", "I2");
		final GedcomNode note1 = transformerTo.createWithID("NOTE", "N1");
		final GedcomNode source = transformerTo.createWithID("SOUR", "S1");
		final GedcomNode object = transformerTo.createWithID("OBJE", "O1");

		Assertions.assertEquals("id: I1, tag: INDI, children: [{tag: RESN, value: RESTRICTION_NOTICE}, {tag: NAME, value: name /surname/ name_suffix, children: [{tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {tag: NOTE, ref: N1}, {tag: SOURCE, ref: S1}]}, {tag: FONE, children: [{tag: TYPE, value: PHONETIC_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX_FONE}, {tag: GIVN, value: NAME_PIECE_GIVEN_FONE}, {tag: NICK, value: NAME_PIECE_NICKNAME_FONE}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX_FONE}, {tag: SURN, value: NAME_PIECE_SURNAME_FONE}, {tag: NSFX, value: NAME_PIECE_SUFFIX_FONE}, {tag: NOTE, ref: N2}, {tag: SOURCE, ref: S2}]}, {tag: ROMN, children: [{tag: TYPE, value: PHONETIC_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX_ROMN}, {tag: GIVN, value: NAME_PIECE_GIVEN_ROMN}, {tag: NICK, value: NAME_PIECE_NICKNAME_ROMN}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX_ROMN}, {tag: SURN, value: NAME_PIECE_SURNAME_ROMN}, {tag: NSFX, value: NAME_PIECE_SUFFIX_ROMN}, {tag: NOTE, ref: N3}, {tag: SOURCE, ref: S3}]}, {tag: SEX, value: SEX_VALUE}, {tag: BIRTH, value: Y, children: [{tag: DATE, value: DATE_VALUE1}, {tag: FAMC, ref: F1}]}, {tag: ADOP, children: [{tag: DATE, value: DATE_VALUE2}, {tag: FAMC, ref: F1, children: [{tag: ADOP, value: WIFE}]}]}, {tag: EVEN, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}, {tag: DATE, value: DATE_VALUE3}]}, {tag: TITL, value: NOBILITY_TYPE_TITLE, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION_titl}, {tag: DATE, value: DATE_VALUE4}]}, {tag: FACT, value: EVENT_DESCRIPTOR_fact, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION_fact}, {tag: DATE, value: DATE_VALUE5}]}, {tag: FAMC, ref: F1, children: [{tag: PEDI, value: WIFE}, {tag: STAT, value: CHILD_LINKAGE_STATUS}, {tag: NOTE, ref: N4}]}, {tag: FAMS, ref: F1, children: [{tag: NOTE, ref: N5}]}, {tag: SUBM, ref: SUBM1}, {tag: ASSO, ref: I1, children: [{tag: TYPE, value: INDI}, {tag: NOTE, ref: N6}, {tag: SOUR, ref: S1}]}, {tag: ASSO, ref: F1, children: [{tag: TYPE, value: FAM}, {tag: NOTE, ref: N7}, {tag: SOUR, ref: S1}]}, {tag: ALIA, ref: I1}, {tag: NOTE, ref: N8}, {tag: SOUR, ref: S1}, {tag: OBJE, ref: O1}]", individual.toString());
		Assertions.assertEquals("id: N1, tag: NOTE", note1.toString());

		final Gedcom origin = new Gedcom();
		origin.addIndividual(individual);
		origin.addFamily(family);
		origin.addNote(note1);
		origin.addNote(transformerTo.createWithIDValue("NOTE", "N1", "NOTE_1"));
		origin.addNote(transformerTo.createWithIDValue("NOTE", "N2", "NOTE_2"));
		origin.addNote(transformerTo.createWithIDValue("NOTE", "N3", "NOTE_3"));
		origin.addNote(transformerTo.createWithIDValue("NOTE", "N4", "NOTE_4"));
		origin.addNote(transformerTo.createWithIDValue("NOTE", "N5", "NOTE_5"));
		origin.addNote(transformerTo.createWithIDValue("NOTE", "N6", "NOTE_6"));
		origin.addNote(transformerTo.createWithIDValue("NOTE", "N7", "NOTE_7"));
		origin.addNote(transformerTo.createWithIDValue("NOTE", "N8", "NOTE_8"));
		origin.addSource(source);
		origin.addObject(object);
		final Flef destination = new Flef();
		transformerTo.individualRecordTo(individual, origin, destination);

		Assertions.assertEquals("id: I1, tag: INDIVIDUAL, children: [{tag: NAME, children: [{tag: TITLE, value: NAME_PIECE_PREFIX}, {tag: INDIVIDUAL_NAME, value: NAME_PIECE_GIVEN, children: [{tag: SUFFIX, value: NAME_PIECE_SUFFIX}]}, {tag: INDIVIDUAL_NICKNAME, value: NAME_PIECE_NICKNAME}, {tag: FAMILY_NAME, value: NAME_PIECE_SURNAME_PREFIX NAME_PIECE_SURNAME}]}, {tag: NOTE, ref: N1}, {tag: SEX, value: SEX_VALUE}, {tag: FAMILY_CHILD, ref: F1, children: [{tag: NOTE, ref: N2}]}, {tag: FAMILY_SPOUSE, ref: F1, children: [{tag: NOTE, ref: N3}]}, {tag: ASSOCIATION, ref: I1, children: [{tag: TYPE, value: individual}, {tag: NOTE, ref: N4}, {tag: SOURCE, ref: S1}]}, {tag: ASSOCIATION, ref: F1, children: [{tag: TYPE, value: family}, {tag: NOTE, ref: N5}, {tag: SOURCE, ref: S1}]}, {tag: ALIAS, ref: I1}, {tag: EVENT, ref: E1}, {tag: EVENT, ref: E2}, {tag: EVENT, ref: E3}, {tag: EVENT, ref: E4}, {tag: NOTE, ref: N6}, {tag: SOURCE, ref: S1}, {tag: SOURCE, ref: S2}, {tag: RESTRICTION, value: RESTRICTION_NOTICE}]", destination.getIndividuals().get(0).toString());
		Assertions.assertEquals("id: E1, tag: EVENT, children: [{tag: TYPE, value: ADOPTION}, {tag: FAMILY, ref: F1}, {tag: PARENT_PEDIGREE, ref: I2, children: [{tag: PEDIGREE, value: adopted}]}, {tag: DATE, value: DATE_VALUE2, children: [{tag: CALENDAR, ref: K1}]}]", destination.getEvents().get(0).toString());
		Assertions.assertEquals("id: E2, tag: EVENT, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}, {tag: DATE, value: DATE_VALUE3, children: [{tag: CALENDAR, ref: K1}]}]", destination.getEvents().get(1).toString());
		Assertions.assertEquals("id: E3, tag: EVENT, children: [{tag: TYPE, value: TITLE}, {tag: DESCRIPTION, value: NOBILITY_TYPE_TITLE}, {tag: DATE, value: DATE_VALUE4, children: [{tag: CALENDAR, ref: K1}]}]", destination.getEvents().get(2).toString());
		Assertions.assertEquals("id: E4, tag: EVENT, children: [{tag: TYPE, value: FACT}, {tag: DESCRIPTION, value: EVENT_DESCRIPTOR_fact}, {tag: DATE, value: DATE_VALUE5, children: [{tag: CALENDAR, ref: K1}]}]", destination.getEvents().get(3).toString());
		Assertions.assertEquals("id: K1, tag: CALENDAR", destination.getCalendars().get(0).toString());
	}

	@Test
	void individualRecordFrom(){
		final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);
		final GedcomNode individual = transformerFrom.createWithID("INDIVIDUAL", "I1")
			.addChild(transformerFrom.create("NAME")
				.addChildValue("TYPE", "NAME_TYPE")
				.addChildValue("LOCALE", "LOCALE_NAME")
				.addChild(transformerFrom.createWithValue("TITLE", "TITLE_PIECE")
					.addChild(transformerFrom.createWithValue("PHONETIC", "PHONETIC_SYSTEM")
						.addChildValue("VALUE", "PHONETIC_NAME_PIECE_title")
					)
					.addChild(transformerFrom.createWithValue("TRANSCRIPTION", "TRANSCRIPTION_SYSTEM")
						.addChildValue("TYPE", "TRANSCRIPTION_TYPE")
						.addChildValue("VALUE", "TRANSCRIPTION_NAME_PIECE_title")
					)
				)
				.addChild(transformerFrom.createWithValue("INDIVIDUAL_NAME", "INDIVIDUAL_NAME_PIECE")
					.addChild(transformerFrom.createWithValue("PHONETIC", "PHONETIC_SYSTEM")
						.addChildValue("VALUE", "PHONETIC_NAME_PIECE_individual_name")
					)
					.addChild(transformerFrom.createWithValue("TRANSCRIPTION", "TRANSCRIPTION_SYSTEM")
						.addChildValue("TYPE", "TRANSCRIPTION_TYPE")
						.addChildValue("VALUE", "TRANSCRIPTION_NAME_PIECE_individual_name")
					)
					.addChild(transformerFrom.createWithValue("SUFFIX", "INDIVIDUAL_NAME_PIECE_SUFFIX")
						.addChild(transformerFrom.createWithValue("PHONETIC", "PHONETIC_SYSTEM")
							.addChildValue("VALUE", "PHONETIC_NAME_PIECE_individual_name_suffix")
						)
						.addChild(transformerFrom.createWithValue("TRANSCRIPTION", "TRANSCRIPTION_SYSTEM")
							.addChildValue("TYPE", "TRANSCRIPTION_TYPE")
							.addChildValue("VALUE", "TRANSCRIPTION_NAME_PIECE_individual_name_suffix")
						)
					)
				)
				.addChild(transformerFrom.createWithValue("INDIVIDUAL_NICKNAME", "NICKNAME_NAME_PIECE")
					.addChild(transformerFrom.createWithValue("PHONETIC", "PHONETIC_SYSTEM")
						.addChildValue("VALUE", "PHONETIC_NAME_PIECE_nickname")
					)
					.addChild(transformerFrom.createWithValue("TRANSCRIPTION", "TRANSCRIPTION_SYSTEM")
						.addChildValue("TYPE", "TRANSCRIPTION_TYPE")
						.addChildValue("VALUE", "TRANSCRIPTION_NAME_PIECE_nickname")
					)
				)
				.addChild(transformerFrom.createWithValue("FAMILY_NAME", "FAMILY_NAME1")
					.addChild(transformerFrom.createWithValue("PHONETIC", "PHONETIC_SYSTEM")
						.addChildValue("VALUE", "PHONETIC_NAME_PIECE_surname1")
					)
					.addChild(transformerFrom.createWithValue("TRANSCRIPTION", "TRANSCRIPTION_SYSTEM")
						.addChildValue("TYPE", "TRANSCRIPTION_TYPE")
						.addChildValue("VALUE", "TRANSCRIPTION_NAME_PIECE_surname1")
					)
				)
				.addChild(transformerFrom.createWithValue("FAMILY_NAME", "FAMILY_NAME2")
					.addChild(transformerFrom.createWithValue("PHONETIC", "PHONETIC_SYSTEM")
						.addChildValue("VALUE", "PHONETIC_NAME_PIECE_surname2")
					)
					.addChild(transformerFrom.createWithValue("TRANSCRIPTION", "TRANSCRIPTION_SYSTEM")
						.addChildValue("TYPE", "TRANSCRIPTION_TYPE")
						.addChildValue("VALUE", "TRANSCRIPTION_NAME_PIECE_surname2")
					)
				)
				.addChild(transformerFrom.createWithValue("FAMILY_NICKNAME", "FAMILY_NICKNAME_PIECE")
					.addChild(transformerFrom.createWithValue("PHONETIC", "PHONETIC_SYSTEM")
						.addChildValue("VALUE", "PHONETIC_NAME_PIECE_family_nickname")
					)
					.addChild(transformerFrom.createWithValue("TRANSCRIPTION", "TRANSCRIPTION_SYSTEM")
						.addChildValue("TYPE", "TRANSCRIPTION_TYPE")
						.addChildValue("VALUE", "TRANSCRIPTION_NAME_PIECE_family_nickname")
					)
				)
				.addChildReference("CULTURAL_RULE", "C1")
				.addChildReference("NOTE", "N1")
				.addChildReference("SOURCE", "S1")
			)
			.addChildValue("SEX", "SEX_VALUE")
			.addChild(transformerFrom.createWithReference("ASSOCIATION", "I1")
				.addChildValue("TYPE", "individual")
				.addChildValue("RELATIONSHIP", "RELATION_IS_DESCRIPTOR1")
				.addChildReference("NOTE", "N1")
				.addChildReference("SOURCE", "S1")
			)
			.addChild(transformerFrom.createWithReference("ASSOCIATION", "F1")
				.addChildValue("TYPE", "family")
				.addChildValue("RELATIONSHIP", "RELATION_IS_DESCRIPTOR2")
				.addChildReference("NOTE", "N1")
				.addChildReference("SOURCE", "S1")
			)
			.addChild(transformerFrom.createWithReference("ALIAS", "I1")
				.addChildReference("NOTE", "N1")
			)
			.addChildReference("EVENT", "E1")
			.addChildReference("EVENT", "E2")
			.addChildReference("EVENT", "E3")
			.addChildReference("EVENT", "E4")
			.addChildReference("EVENT", "E5")
			.addChild(transformerFrom.createWithReference("GROUP", "G1")
				.addChildValue("ROLE", "ROLE_IN_GROUP")
				.addChildReference("NOTE", "N1")
				.addChildValue("CREDIBILITY", "CREDIBILITY_ASSESSMENT")
				.addChildValue("RESTRICTION", "RESTRICTION_NOTICE")
			)
			.addChildReference("CULTURAL_RULE", "C1")
			.addChildReference("NOTE", "N1")
			.addChildReference("SOURCE", "S1")
			.addChild(transformerFrom.createWithValue("PREFERRED_IMAGE", "IMAGE_FILE_REFERENCE")
				.addChildValue("CUTOUT", "CUTOUT_COORDINATES")
			)
			.addChildValue("RESTRICTION", "RESTRICTION_NOTICE");
		final GedcomNode event1 = transformerFrom.createWithID("EVENT", "E1")
			.addChildValue("TYPE", "BIRTH")
			.addChildValue("DESCRIPTION", "EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE1")
			.addChildReference("INDIVIDUAL", "I1")
			.addChildReference("FAMILY", "F1")
			.addChildValue("DATE", "ENTRY_RECORDING_DATE1")
			.addChildReference("PLACE", "P1")
			.addChildValue("AGENCY", "RESPONSIBLE_AGENCY1")
			.addChildValue("CAUSE", "CAUSE_OF_EVENT1")
			.addChildReference("NOTE", "N1")
			.addChildReference("SOURCE", "S1");
		final GedcomNode event2 = transformerFrom.createWithID("EVENT", "E2")
			.addChildValue("TYPE", "ADOPTION")
			.addChildValue("DESCRIPTION", "EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE2")
			.addChildReference("INDIVIDUAL", "I1")
			.addChildReference("FAMILY", "F1")
			.addChild(transformerFrom.createWithReference("PARENT_PEDIGREE", "I1")
				.addChildValue("PEDIGREE", "birth")
			)
			.addChild(transformerFrom.createWithReference("PARENT_PEDIGREE", "I2")
				.addChildValue("PEDIGREE", "adopted")
			)
			.addChildValue("DATE", "ENTRY_RECORDING_DATE2")
			.addChildValue("AGENCY", "RESPONSIBLE_AGENCY2")
			.addChildValue("CAUSE", "CAUSE_OF_EVENT2");
		final GedcomNode event3 = transformerFrom.createWithID("EVENT", "E3")
			.addChildValue("TYPE", "MARRIAGE")
			.addChildValue("DESCRIPTION", "EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE3")
			.addChildValue("DATE", "ENTRY_RECORDING_DATE3")
			.addChildValue("AGENCY", "RESPONSIBLE_AGENCY3")
			.addChildValue("CAUSE", "CAUSE_OF_EVENT3");
		final GedcomNode event4 = transformerFrom.createWithID("EVENT", "E4")
			.addChildValue("TYPE", "RESIDENCE")
			.addChildValue("DESCRIPTION", "EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE4")
			.addChildValue("DATE", "ENTRY_RECORDING_DATE4")
			.addChildValue("AGENCY", "RESPONSIBLE_AGENCY4")
			.addChildValue("CAUSE", "CAUSE_OF_EVENT4");
		final GedcomNode event5 = transformerFrom.createWithID("EVENT", "E5")
			.addChildValue("TYPE", "EVENT_DESCRIPTOR")
			.addChildValue("DESCRIPTION", "EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE5")
			.addChildValue("DATE", "ENTRY_RECORDING_DATE5")
			.addChildValue("AGENCY", "RESPONSIBLE_AGENCY5")
			.addChildValue("CAUSE", "CAUSE_OF_EVENT5");
		final GedcomNode source = transformerFrom.createWithID("SOURCE", "S1");
		final GedcomNode place = transformerFrom.createWithID("PLACE", "P1")
			.addChildValue("NAME", "PLACE_NAME");
		final GedcomNode note = transformerFrom.createWithID("NOTE", "N1");

		Assertions.assertEquals("id: I1, tag: INDIVIDUAL, children: [{tag: NAME, children: [{tag: TYPE, value: NAME_TYPE}, {tag: LOCALE, value: LOCALE_NAME}, {tag: TITLE, value: TITLE_PIECE, children: [{tag: PHONETIC, value: PHONETIC_SYSTEM, children: [{tag: VALUE, value: PHONETIC_NAME_PIECE_title}]}, {tag: TRANSCRIPTION, value: TRANSCRIPTION_SYSTEM, children: [{tag: TYPE, value: TRANSCRIPTION_TYPE}, {tag: VALUE, value: TRANSCRIPTION_NAME_PIECE_title}]}]}, {tag: INDIVIDUAL_NAME, value: INDIVIDUAL_NAME_PIECE, children: [{tag: PHONETIC, value: PHONETIC_SYSTEM, children: [{tag: VALUE, value: PHONETIC_NAME_PIECE_individual_name}]}, {tag: TRANSCRIPTION, value: TRANSCRIPTION_SYSTEM, children: [{tag: TYPE, value: TRANSCRIPTION_TYPE}, {tag: VALUE, value: TRANSCRIPTION_NAME_PIECE_individual_name}]}, {tag: SUFFIX, value: INDIVIDUAL_NAME_PIECE_SUFFIX, children: [{tag: PHONETIC, value: PHONETIC_SYSTEM, children: [{tag: VALUE, value: PHONETIC_NAME_PIECE_individual_name_suffix}]}, {tag: TRANSCRIPTION, value: TRANSCRIPTION_SYSTEM, children: [{tag: TYPE, value: TRANSCRIPTION_TYPE}, {tag: VALUE, value: TRANSCRIPTION_NAME_PIECE_individual_name_suffix}]}]}]}, {tag: INDIVIDUAL_NICKNAME, value: NICKNAME_NAME_PIECE, children: [{tag: PHONETIC, value: PHONETIC_SYSTEM, children: [{tag: VALUE, value: PHONETIC_NAME_PIECE_nickname}]}, {tag: TRANSCRIPTION, value: TRANSCRIPTION_SYSTEM, children: [{tag: TYPE, value: TRANSCRIPTION_TYPE}, {tag: VALUE, value: TRANSCRIPTION_NAME_PIECE_nickname}]}]}, {tag: FAMILY_NAME, value: FAMILY_NAME1, children: [{tag: PHONETIC, value: PHONETIC_SYSTEM, children: [{tag: VALUE, value: PHONETIC_NAME_PIECE_surname1}]}, {tag: TRANSCRIPTION, value: TRANSCRIPTION_SYSTEM, children: [{tag: TYPE, value: TRANSCRIPTION_TYPE}, {tag: VALUE, value: TRANSCRIPTION_NAME_PIECE_surname1}]}]}, {tag: FAMILY_NAME, value: FAMILY_NAME2, children: [{tag: PHONETIC, value: PHONETIC_SYSTEM, children: [{tag: VALUE, value: PHONETIC_NAME_PIECE_surname2}]}, {tag: TRANSCRIPTION, value: TRANSCRIPTION_SYSTEM, children: [{tag: TYPE, value: TRANSCRIPTION_TYPE}, {tag: VALUE, value: TRANSCRIPTION_NAME_PIECE_surname2}]}]}, {tag: FAMILY_NICKNAME, value: FAMILY_NICKNAME_PIECE, children: [{tag: PHONETIC, value: PHONETIC_SYSTEM, children: [{tag: VALUE, value: PHONETIC_NAME_PIECE_family_nickname}]}, {tag: TRANSCRIPTION, value: TRANSCRIPTION_SYSTEM, children: [{tag: TYPE, value: TRANSCRIPTION_TYPE}, {tag: VALUE, value: TRANSCRIPTION_NAME_PIECE_family_nickname}]}]}, {tag: CULTURAL_RULE, ref: C1}, {tag: NOTE, ref: N1}, {tag: SOURCE, ref: S1}]}, {tag: SEX, value: SEX_VALUE}, {tag: ASSOCIATION, ref: I1, children: [{tag: TYPE, value: individual}, {tag: RELATIONSHIP, value: RELATION_IS_DESCRIPTOR1}, {tag: NOTE, ref: N1}, {tag: SOURCE, ref: S1}]}, {tag: ASSOCIATION, ref: F1, children: [{tag: TYPE, value: family}, {tag: RELATIONSHIP, value: RELATION_IS_DESCRIPTOR2}, {tag: NOTE, ref: N1}, {tag: SOURCE, ref: S1}]}, {tag: ALIAS, ref: I1, children: [{tag: NOTE, ref: N1}]}, {tag: EVENT, ref: E1}, {tag: EVENT, ref: E2}, {tag: EVENT, ref: E3}, {tag: EVENT, ref: E4}, {tag: EVENT, ref: E5}, {tag: GROUP, ref: G1, children: [{tag: ROLE, value: ROLE_IN_GROUP}, {tag: NOTE, ref: N1}, {tag: CREDIBILITY, value: CREDIBILITY_ASSESSMENT}, {tag: RESTRICTION, value: RESTRICTION_NOTICE}]}, {tag: CULTURAL_RULE, ref: C1}, {tag: NOTE, ref: N1}, {tag: SOURCE, ref: S1}, {tag: PREFERRED_IMAGE, value: IMAGE_FILE_REFERENCE, children: [{tag: CUTOUT, value: CUTOUT_COORDINATES}]}, {tag: RESTRICTION, value: RESTRICTION_NOTICE}]", individual.toString());

		final Flef origin = new Flef();
		origin.addIndividual(individual);
		origin.addEvent(event1);
		origin.addEvent(event2);
		origin.addEvent(event3);
		origin.addEvent(event4);
		origin.addEvent(event5);
		origin.addSource(source);
		origin.addPlace(place);
		origin.addNote(note);
		final Gedcom destination = new Gedcom();
		destination.addSource(source);
		transformerFrom.individualRecordFrom(individual, origin, destination);

		Assertions.assertEquals("id: I1, tag: INDI, children: [{tag: NAME, value: INDIVIDUAL_NAME_PIECE /FAMILY_NAME1 FAMILY_NAME2/ INDIVIDUAL_NAME_PIECE_SUFFIX, children: [{tag: TYPE, value: NAME_TYPE}, {tag: NPFX, value: TITLE_PIECE}, {tag: GIVN, value: INDIVIDUAL_NAME_PIECE}, {tag: NSFX, value: INDIVIDUAL_NAME_PIECE_SUFFIX}, {tag: NICK, value: NICKNAME_NAME_PIECE}, {tag: SURN, value: FAMILY_NAME1 FAMILY_NAME2}, {tag: FONE, children: [{tag: NPFX, value: PHONETIC_NAME_PIECE_title}, {tag: GIVN, value: PHONETIC_NAME_PIECE_individual_name}, {tag: NICK, value: PHONETIC_NAME_PIECE_nickname}, {tag: SURN, value: PHONETIC_NAME_PIECE_surname1 PHONETIC_NAME_PIECE_surname2}]}, {tag: ROMN, children: [{tag: NPFX, value: TRANSCRIPTION_NAME_PIECE_title}, {tag: GIVN, value: TRANSCRIPTION_NAME_PIECE_individual_name}, {tag: NICK, value: TRANSCRIPTION_NAME_PIECE_nickname}, {tag: SURN, value: TRANSCRIPTION_NAME_PIECE_surname1 TRANSCRIPTION_NAME_PIECE_surname2}]}]}, {tag: NOTE, ref: N1}, {tag: SOUR, ref: S2}, {tag: SEX, value: SEX_VALUE}, {tag: ASSO, ref: I1, children: [{tag: TYPE, value: INDI}, {tag: RELA, value: RELATION_IS_DESCRIPTOR1}, {tag: NOTE, ref: N1}, {tag: SOUR, ref: S2}]}, {tag: ASSO, ref: F1, children: [{tag: TYPE, value: FAM}, {tag: RELA, value: RELATION_IS_DESCRIPTOR2}, {tag: NOTE, ref: N1}, {tag: SOUR, ref: S2}]}, {tag: ALIA, ref: I1}, {tag: BIRT, value: Y, children: [{tag: TYPE, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE1}, {tag: DATE, value: ENTRY_RECORDING_DATE1}, {tag: PLAC, value: PLACE_NAME}, {tag: AGNC, value: RESPONSIBLE_AGENCY1}, {tag: CAUS, value: CAUSE_OF_EVENT1}, {tag: NOTE, ref: N1}, {tag: SOUR, ref: S2}]}, {tag: ADOP, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE2, children: [{tag: DATE, value: ENTRY_RECORDING_DATE2}, {tag: AGNC, value: RESPONSIBLE_AGENCY2}, {tag: CAUS, value: CAUSE_OF_EVENT2}, {tag: FAMC, ref: F1, children: [{tag: ADOP, value: WIFE}]}]}, {tag: MARR, value: Y, children: [{tag: TYPE, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE3}, {tag: DATE, value: ENTRY_RECORDING_DATE3}, {tag: AGNC, value: RESPONSIBLE_AGENCY3}, {tag: CAUS, value: CAUSE_OF_EVENT3}]}, {tag: RESI, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE4, children: [{tag: DATE, value: ENTRY_RECORDING_DATE4}, {tag: AGNC, value: RESPONSIBLE_AGENCY4}, {tag: CAUS, value: CAUSE_OF_EVENT4}]}, {tag: EVEN, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE5, children: [{tag: TYPE, value: EVENT_DESCRIPTOR}, {tag: DATE, value: ENTRY_RECORDING_DATE5}, {tag: AGNC, value: RESPONSIBLE_AGENCY5}, {tag: CAUS, value: CAUSE_OF_EVENT5}]}, {tag: NOTE, ref: N1}, {tag: SOUR, ref: S2}, {tag: RESN, value: RESTRICTION_NOTICE}]", destination.getIndividuals().get(0).toString());
	}

}