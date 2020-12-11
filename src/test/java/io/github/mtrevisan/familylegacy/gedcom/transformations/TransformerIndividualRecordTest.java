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

	private final Transformer transformerTo = new Transformer(Protocol.FLEF);
	private final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);


	@Test
	void individualRecordTo(){
		final GedcomNode parent = transformerTo.createEmpty()
			.addChild(transformerTo.createWithID("INDI", "@I1@")
				.addChildValue("RESN", "RESTRICTION_NOTICE")
				.addChild(transformerTo.createWithValue("NAME", "name /surname/ name_suffix")
					.addChildValue("NPFX", "NAME_PIECE_PREFIX")
					.addChildValue("GIVN", "NAME_PIECE_GIVEN")
					.addChildValue("NICK", "NAME_PIECE_NICKNAME")
					.addChildValue("SPFX", "NAME_PIECE_SURNAME_PREFIX")
					.addChildValue("SURN", "NAME_PIECE_SURNAME")
					.addChildValue("NSFX", "NAME_PIECE_SUFFIX")
					.addChildReference("NOTE", "@N1@")
					.addChildReference("SOURCE", "@S1@")
				)
				.addChild(transformerTo.create("FONE")
					.addChildValue("TYPE", "PHONETIC_TYPE")
					.addChildValue("NPFX", "NAME_PIECE_PREFIX_FONE")
					.addChildValue("GIVN", "NAME_PIECE_GIVEN_FONE")
					.addChildValue("NICK", "NAME_PIECE_NICKNAME_FONE")
					.addChildValue("SPFX", "NAME_PIECE_SURNAME_PREFIX_FONE")
					.addChildValue("SURN", "NAME_PIECE_SURNAME_FONE")
					.addChildValue("NSFX", "NAME_PIECE_SUFFIX_FONE")
					.addChildReference("NOTE", "@N2@")
					.addChildReference("SOURCE", "@S2@")
				)
				.addChild(transformerTo.create("ROMN")
					.addChildValue("TYPE", "PHONETIC_TYPE")
					.addChildValue("NPFX", "NAME_PIECE_PREFIX_ROMN")
					.addChildValue("GIVN", "NAME_PIECE_GIVEN_ROMN")
					.addChildValue("NICK", "NAME_PIECE_NICKNAME_ROMN")
					.addChildValue("SPFX", "NAME_PIECE_SURNAME_PREFIX_ROMN")
					.addChildValue("SURN", "NAME_PIECE_SURNAME_ROMN")
					.addChildValue("NSFX", "NAME_PIECE_SUFFIX_ROMN")
					.addChildReference("NOTE", "@N3@")
					.addChildReference("SOURCE", "@S3@")
				)
				.addChildValue("SEX", "SEX_VALUE")
				.addChild(transformerFrom.createWithValue("BIRTH", "Y")
					.addChildValue("DATE", "DATE_VALUE1")
					.addChildReference("FAMC", "@F1@")
				)
				.addChild(transformerFrom.create("ADOP")
					.addChildValue("DATE", "DATE_VALUE2")
					.addChild(transformerFrom.create("FAMC")
						.withXRef("@F1@")
						.addChildValue("ADOP", "ADOPTED_BY_WHICH_PARENT")
					)
				)
				.addChild(transformerFrom.create("EVEN")
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
				.addChild(transformerTo.createWithReference("FAMC", "@F1@")
					.addChildValue("PEDI", "PEDIGREE_LINKAGE_TYPE")
					.addChildValue("STAT", "CHILD_LINKAGE_STATUS")
					.addChildReference("NOTE", "@N4@")
				)
				.addChild(transformerTo.createWithReference("FAMS", "@F1@")
					.addChildReference("NOTE", "@N5@")
				)
				.addChildReference("SUBM", "@SUBM1@")
				.addChild(transformerTo.createWithReference("ASSO", "@I1@")
					.addChildReference("TYPE", "INDI")
					.addChildReference("NOTE", "@N6@")
					.addChildReference("SOUR", "@S1@")
				)
				.addChild(transformerTo.createWithReference("ASSO", "@F1@")
					.addChildReference("TYPE", "FAM")
					.addChildReference("NOTE", "@N7@")
					.addChildReference("SOUR", "@S1@")
				)
				.addChild(transformerTo.createWithReference("ALIA", "@I1@"))
				.addChildReference("NOTE", "@N8@")
				.addChildReference("SOUR", "@S1@")
				.addChildReference("OBJE", "@O1@")
			);
		final GedcomNode note = transformerTo.createWithID("NOTE", "@N1@");
		final GedcomNode source = transformerTo.createWithID("SOUR", "@S1@");
		final GedcomNode object = transformerTo.createWithID("OBJE", "@O1@");

		Assertions.assertEquals("children: [{id: @I1@, tag: INDI, children: [{tag: RESN, value: RESTRICTION_NOTICE}, {tag: NAME, value: name /surname/ name_suffix, children: [{tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {tag: NOTE, ref: @N1@}, {tag: SOURCE, ref: @S1@}]}, {tag: FONE, children: [{tag: TYPE, value: PHONETIC_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX_FONE}, {tag: GIVN, value: NAME_PIECE_GIVEN_FONE}, {tag: NICK, value: NAME_PIECE_NICKNAME_FONE}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX_FONE}, {tag: SURN, value: NAME_PIECE_SURNAME_FONE}, {tag: NSFX, value: NAME_PIECE_SUFFIX_FONE}, {tag: NOTE, ref: @N2@}, {tag: SOURCE, ref: @S2@}]}, {tag: ROMN, children: [{tag: TYPE, value: PHONETIC_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX_ROMN}, {tag: GIVN, value: NAME_PIECE_GIVEN_ROMN}, {tag: NICK, value: NAME_PIECE_NICKNAME_ROMN}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX_ROMN}, {tag: SURN, value: NAME_PIECE_SURNAME_ROMN}, {tag: NSFX, value: NAME_PIECE_SUFFIX_ROMN}, {tag: NOTE, ref: @N3@}, {tag: SOURCE, ref: @S3@}]}, {tag: SEX, value: SEX_VALUE}, {tag: BIRTH, value: Y, children: [{tag: DATE, value: DATE_VALUE1}, {tag: FAMC, ref: @F1@}]}, {tag: ADOP, children: [{tag: DATE, value: DATE_VALUE2}, {tag: FAMC, ref: @F1@, children: [{tag: ADOP, value: ADOPTED_BY_WHICH_PARENT}]}]}, {tag: EVEN, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}, {tag: DATE, value: DATE_VALUE3}]}, {tag: TITL, value: NOBILITY_TYPE_TITLE, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION_titl}, {tag: DATE, value: DATE_VALUE4}]}, {tag: FACT, value: EVENT_DESCRIPTOR_fact, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION_fact}, {tag: DATE, value: DATE_VALUE5}]}, {tag: FAMC, ref: @F1@, children: [{tag: PEDI, value: PEDIGREE_LINKAGE_TYPE}, {tag: STAT, value: CHILD_LINKAGE_STATUS}, {tag: NOTE, ref: @N4@}]}, {tag: FAMS, ref: @F1@, children: [{tag: NOTE, ref: @N5@}]}, {tag: SUBM, ref: @SUBM1@}, {tag: ASSO, ref: @I1@, children: [{tag: TYPE, ref: INDI}, {tag: NOTE, ref: @N6@}, {tag: SOUR, ref: @S1@}]}, {tag: ASSO, ref: @F1@, children: [{tag: TYPE, ref: FAM}, {tag: NOTE, ref: @N7@}, {tag: SOUR, ref: @S1@}]}, {tag: ALIA, ref: @I1@}, {tag: NOTE, ref: @N8@}, {tag: SOUR, ref: @S1@}, {tag: OBJE, ref: @O1@}]}]", parent.toString());
		Assertions.assertEquals("id: @N1@, tag: NOTE", note.toString());

		final Gedcom origin = new Gedcom();
		origin.addNote(note);
		origin.addSource(source);
		origin.addObject(object);
		final Flef destination = new Flef();
		transformerTo.individualRecordTo(parent, origin, destination);

		Assertions.assertEquals("id: @I1@, tag: INDIVIDUAL, children: [{tag: NAME, children: [{tag: TITLE, value: NAME_PIECE_PREFIX, children: [{tag: PERSONAL_NAME, value: NAME_PIECE_GIVEN, children: [{tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX}, {tag: INDIVIDUAL_NICKNAME, value: NAME_PIECE_NICKNAME}, {tag: FAMILY_NAME, value: NAME_PIECE_SURNAME_PREFIX NAME_PIECE_SURNAME}]}]}]}, {tag: NOTE, ref: @N1@}, {tag: SEX, value: SEX_VALUE}, {tag: FAMILY_CHILD, ref: @F1@, children: [{tag: NOTE, ref: @N4@}]}, {tag: FAMILY_SPOUSE, ref: @F1@, children: [{tag: NOTE, ref: @N5@}]}, {tag: ASSOCIATION, ref: @I1@, children: [{tag: NOTE, ref: @N6@}, {tag: SOURCE, ref: @S1@}]}, {tag: ASSOCIATION, ref: @F1@, children: [{tag: NOTE, ref: @N7@}, {tag: SOURCE, ref: @S1@}]}, {tag: ALIAS, ref: @I1@}, {tag: EVENT, children: [{tag: TYPE, value: ADOPTION}, {tag: FAMILY_CHILD, ref: @F1@, children: [{tag: ADOPTED_BY, value: ADOPTED_BY_WHICH_PARENT}]}, {tag: DATE, value: DATE_VALUE2, children: [{tag: CALENDAR, value: gregorian}]}]}, {tag: EVENT, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION}, {tag: DATE, value: DATE_VALUE3, children: [{tag: CALENDAR, value: gregorian}]}]}, {tag: EVENT, children: [{tag: TYPE, value: TITLE}, {tag: DESCRIPTION, value: NOBILITY_TYPE_TITLE}, {tag: DATE, value: DATE_VALUE4, children: [{tag: CALENDAR, value: gregorian}]}]}, {tag: EVENT, children: [{tag: TYPE, value: FACT}, {tag: DESCRIPTION, value: EVENT_DESCRIPTOR_fact}, {tag: DATE, value: DATE_VALUE5, children: [{tag: CALENDAR, value: gregorian}]}]}, {tag: NOTE, ref: @N8@}, {tag: SOURCE, ref: @S1@}, {tag: NOTE, ref: @N8@}, {tag: RESTRICTION, value: RESTRICTION_NOTICE}]", destination.getFamilies().get(0).toString());
	}

	@Test
	void individualRecordFrom(){
		//TODO
		final GedcomNode parent = transformerFrom.createEmpty()
			.addChild(transformerFrom.createWithID("INDIVIDUAL", "@I1@")
				.addChildValue("TYPE", "marriage")
				.addChildReference("SPOUSE1", "@I1@")
				.addChildReference("SPOUSE2", "@I2@")
				.addChildReference("CHILD", "@I3@")
				.addChild(transformerFrom.create("EVENT")
					.addChildValue("TYPE", "BIRTH")
					.addChildValue("DESCRIPTION", "EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE1")
					.addChildReference("FAMILY_CHILD", "@F1@")
					.addChildValue("DATE", "ENTRY_RECORDING_DATE1")
					.addChildReference("PLACE", "@P1@")
					.addChildValue("AGENCY", "RESPONSIBLE_AGENCY1")
					.addChildValue("CAUSE", "CAUSE_OF_EVENT1")
					.addChildReference("NOTE", "@N1@")
					.addChildReference("SOURCE", "@S1@")
				)
				.addChild(transformerFrom.create("EVENT")
					.addChildValue("TYPE", "ADOPTION")
					.addChildValue("DESCRIPTION", "EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE2")
					.addChild(transformerFrom.create("FAMILY_CHILD")
						.withXRef("@F1@")
						.addChildValue("ADOPTED_BY", "ADOPTED_BY_WHICH_PARENT")
					)
					.addChildValue("DATE", "ENTRY_RECORDING_DATE2")
					.addChildValue("AGENCY", "RESPONSIBLE_AGENCY2")
					.addChildValue("CAUSE", "CAUSE_OF_EVENT2")
				)
				.addChild(transformerFrom.create("EVENT")
					.addChildValue("TYPE", "MARRIAGE")
					.addChildValue("DESCRIPTION", "EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE3")
					.addChildValue("DATE", "ENTRY_RECORDING_DATE3")
					.addChildValue("AGENCY", "RESPONSIBLE_AGENCY3")
					.addChildValue("CAUSE", "CAUSE_OF_EVENT3")
				)
				.addChild(transformerFrom.create("EVENT")
					.addChildValue("TYPE", "RESIDENCE")
					.addChildValue("DESCRIPTION", "EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE4")
					.addChildValue("DATE", "ENTRY_RECORDING_DATE4")
					.addChildValue("AGENCY", "RESPONSIBLE_AGENCY4")
					.addChildValue("CAUSE", "CAUSE_OF_EVENT4")
				)
				.addChild(transformerFrom.create("EVENT")
					.addChildValue("TYPE", "EVENT_DESCRIPTOR")
					.addChildValue("DESCRIPTION", "EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE5")
					.addChildValue("DATE", "ENTRY_RECORDING_DATE5")
					.addChildValue("AGENCY", "RESPONSIBLE_AGENCY5")
					.addChildValue("CAUSE", "CAUSE_OF_EVENT5")
				)
				.addChildReference("GROUP", "@G1@")
				.addChildReference("CULTURAL_RULE", "@C1@")
				.addChildReference("NOTE", "@N1@")
				.addChildReference("SOURCE", "@S1@")
				.addChild(transformerFrom.create("PREFERRED_IMAGE")
					.withValue("IMAGE_FILE_REFERENCE")
					.addChildValue("CUTOUT", "CUTOUT_COORDINATES")
				)
				.addChildValue("RESTRICTION", "RESTRICTION_NOTICE")
			);
		final GedcomNode source = transformerTo.createWithID("SOURCE", "@S1@");
		final GedcomNode place = transformerTo.createWithID("PLACE", "@P1@")
			.addChildValue("NAME", "PLACE_NAME");
		final GedcomNode note = transformerTo.createWithID("NOTE", "@N1@");

		Assertions.assertEquals("children: [{id: @F1@, tag: FAMILY, children: [{tag: TYPE, value: marriage}, {tag: SPOUSE1, ref: @I1@}, {tag: SPOUSE2, ref: @I2@}, {tag: CHILD, ref: @I3@}, {tag: EVENT, children: [{tag: TYPE, value: BIRTH}, {tag: DESCRIPTION, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE1}, {tag: FAMILY_CHILD, ref: @F1@}, {tag: DATE, value: ENTRY_RECORDING_DATE1}, {tag: PLACE, ref: @P1@}, {tag: AGENCY, value: RESPONSIBLE_AGENCY1}, {tag: CAUSE, value: CAUSE_OF_EVENT1}, {tag: NOTE, ref: @N1@}, {tag: SOURCE, ref: @S1@}]}, {tag: EVENT, children: [{tag: TYPE, value: ADOPTION}, {tag: DESCRIPTION, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE2}, {tag: FAMILY_CHILD, ref: @F1@, children: [{tag: ADOPTED_BY, value: ADOPTED_BY_WHICH_PARENT}]}, {tag: DATE, value: ENTRY_RECORDING_DATE2}, {tag: AGENCY, value: RESPONSIBLE_AGENCY2}, {tag: CAUSE, value: CAUSE_OF_EVENT2}]}, {tag: EVENT, children: [{tag: TYPE, value: MARRIAGE}, {tag: DESCRIPTION, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE3}, {tag: DATE, value: ENTRY_RECORDING_DATE3}, {tag: AGENCY, value: RESPONSIBLE_AGENCY3}, {tag: CAUSE, value: CAUSE_OF_EVENT3}]}, {tag: EVENT, children: [{tag: TYPE, value: RESIDENCE}, {tag: DESCRIPTION, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE4}, {tag: DATE, value: ENTRY_RECORDING_DATE4}, {tag: AGENCY, value: RESPONSIBLE_AGENCY4}, {tag: CAUSE, value: CAUSE_OF_EVENT4}]}, {tag: EVENT, children: [{tag: TYPE, value: EVENT_DESCRIPTOR}, {tag: DESCRIPTION, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE5}, {tag: DATE, value: ENTRY_RECORDING_DATE5}, {tag: AGENCY, value: RESPONSIBLE_AGENCY5}, {tag: CAUSE, value: CAUSE_OF_EVENT5}]}, {tag: GROUP, ref: @G1@}, {tag: CULTURAL_RULE, ref: @C1@}, {tag: NOTE, ref: @N1@}, {tag: SOURCE, ref: @S1@}, {tag: PREFERRED_IMAGE, value: IMAGE_FILE_REFERENCE, children: [{tag: CUTOUT, value: CUTOUT_COORDINATES}]}, {tag: RESTRICTION, value: RESTRICTION_NOTICE}]}]", parent.toString());

		final Flef origin = new Flef();
		origin.addSource(source);
		origin.addPlace(place);
		origin.addNote(note);
		final Gedcom destination = new Gedcom();
		transformerFrom.individualRecordFrom(parent, origin, destination);

		Assertions.assertEquals("id: @F1@, tag: FAM, children: [{tag: HUSB, value: @I1@}, {tag: WIFE, value: @I2@}, {tag: CHIL, ref: @I3@}, {tag: EVEN, value: BIRTH, children: [{tag: TYPE, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE1}, {tag: DATE, value: ENTRY_RECORDING_DATE1}, {tag: PLAC, value: PLACE_NAME}, {tag: AGNC, value: RESPONSIBLE_AGENCY1}, {tag: CAUS, value: CAUSE_OF_EVENT1}, {tag: FAMC, ref: @F1@}, {tag: NOTE, ref: @N1@}, {tag: SOUR, ref: @S1@}]}, {tag: EVEN, value: ADOPTION, children: [{tag: TYPE, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE2}, {tag: DATE, value: ENTRY_RECORDING_DATE2}, {tag: AGNC, value: RESPONSIBLE_AGENCY2}, {tag: CAUS, value: CAUSE_OF_EVENT2}, {tag: FAMC, ref: @F1@, children: [{tag: ADOP, value: ADOPTED_BY_WHICH_PARENT}]}]}, {tag: MARR, children: [{tag: TYPE, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE3}, {tag: DATE, value: ENTRY_RECORDING_DATE3}, {tag: AGNC, value: RESPONSIBLE_AGENCY3}, {tag: CAUS, value: CAUSE_OF_EVENT3}]}, {tag: RESI, children: [{tag: TYPE, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE4}, {tag: DATE, value: ENTRY_RECORDING_DATE4}, {tag: AGNC, value: RESPONSIBLE_AGENCY4}, {tag: CAUS, value: CAUSE_OF_EVENT4}]}, {tag: EVEN, value: EVENT_DESCRIPTOR, children: [{tag: TYPE, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE5}, {tag: DATE, value: ENTRY_RECORDING_DATE5}, {tag: AGNC, value: RESPONSIBLE_AGENCY5}, {tag: CAUS, value: CAUSE_OF_EVENT5}]}, {tag: NOTE, ref: @N1@}, {tag: SOUR, ref: @S1@}, {tag: SOUR, ref: @S1@}]", destination.getFamilies().get(0).toString());
	}

}