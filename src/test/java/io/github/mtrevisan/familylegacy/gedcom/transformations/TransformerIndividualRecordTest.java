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
				//TODO
				.addChild(transformerTo.createWithValue("FACT", "EVENT_DESCRIPTOR_fact")
					.addChildValue("TYPE", "EVENT_OR_FACT_CLASSIFICATION3")
				)
				.addChild(transformerTo.createWithValue("EVEN", "EVENT_DESCRIPTOR_event")
					.addChild(transformerTo.create("HUSB")
						.addChildValue("AGE", "AGE_AT_EVENT31")
					)
					.addChild(transformerTo.create("WIFE")
						.addChildValue("AGE", "AGE_AT_EVENT32")
					)
					.addChildValue("TYPE", "EVENT_OR_FACT_CLASSIFICATION3")
				)
				.addChildReference("HUSB", "@I1@")
				.addChildReference("WIFE", "@I2@")
				.addChildReference("CHIL", "@I3@")
				.addChildValue("NCHI", "5")
				.addChildValue("SUBM", "@SUBM1@")
				.addChild(transformerTo.create("REFN")
					.withValue("USER_REFERENCE_NUMBER")
					.addChildValue("TYPE", "USER_REFERENCE_TYPE")
				)
				.addChildValue("RIN", "AUTOMATED_RECORD_ID")
				.addChildReference("NOTE", "@N1@")
				.addChildReference("SOUR", "@S1@")
				.addChildReference("OBJE", "@O1@")
			);
		final GedcomNode note = transformerTo.createWithID("NOTE", "@N1@");
		final GedcomNode source = transformerTo.createWithID("SOUR", "@S1@");
		final GedcomNode object = transformerTo.createWithID("OBJE", "@O1@");

		Assertions.assertEquals("children: [{id: @I1@, tag: INDI, children: [{tag: RESN, value: RESTRICTION_NOTICE}, {tag: MARR, value: Y, children: [{tag: HUSB, children: [{tag: AGE, value: AGE_AT_EVENT11}]}, {tag: WIFE, children: [{tag: AGE, value: AGE_AT_EVENT12}]}, {tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION1}]}, {tag: RESI, children: [{tag: HUSB, children: [{tag: AGE, value: AGE_AT_EVENT21}]}, {tag: WIFE, children: [{tag: AGE, value: AGE_AT_EVENT22}]}, {tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION2}]}, {tag: FACT, value: EVENT_DESCRIPTOR_fact, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION3}]}, {tag: EVEN, value: EVENT_DESCRIPTOR_event, children: [{tag: HUSB, children: [{tag: AGE, value: AGE_AT_EVENT31}]}, {tag: WIFE, children: [{tag: AGE, value: AGE_AT_EVENT32}]}, {tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION3}]}, {tag: HUSB, ref: @I1@}, {tag: WIFE, ref: @I2@}, {tag: CHIL, ref: @I3@}, {tag: NCHI, value: 5}, {tag: SUBM, value: @SUBM1@}, {tag: REFN, value: USER_REFERENCE_NUMBER, children: [{tag: TYPE, value: USER_REFERENCE_TYPE}]}, {tag: RIN, value: AUTOMATED_RECORD_ID}, {tag: NOTE, ref: @N1@}, {tag: SOUR, ref: @S1@}, {tag: OBJE, ref: @O1@}]}]", parent.toString());
		Assertions.assertEquals("id: @N1@, tag: NOTE", note.toString());

		final Gedcom origin = new Gedcom();
		origin.addNote(note);
		origin.addSource(source);
		origin.addObject(object);
		final Flef destination = new Flef();
		transformerTo.individualRecordTo(parent, origin, destination);

		Assertions.assertEquals("id: @I1@, tag: INDIVIDUAL, children: [{tag: EVENT, children: [{tag: TYPE, value: MARRIAGE}]}, {tag: EVENT, children: [{tag: TYPE, value: RESIDENCE}]}, {tag: EVENT, children: [{tag: TYPE, value: FACT}, {tag: DESCRIPTION, value: EVENT_DESCRIPTOR_fact}]}, {tag: EVENT, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION3}, {tag: DESCRIPTION, value: EVENT_DESCRIPTOR_event}]}, {tag: EVENT, children: [{tag: TYPE, value: CHILDREN_COUNT}, {tag: DESCRIPTION, value: 5}]}, {tag: NOTE, ref: @N1@}, {tag: SOURCE, ref: @S1@}, {tag: NOTE, ref: @N1@}]", destination.getFamilies().get(0).toString());
	}

	//TODO
//	@Test
	void individualRecordFrom(){
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
//		transformerFrom.individualRecordFrom(parent, origin, destination);
//
//		Assertions.assertEquals("id: @F1@, tag: FAM, children: [{tag: HUSB, value: @I1@}, {tag: WIFE, value: @I2@}, {tag: CHIL, ref: @I3@}, {tag: EVEN, value: BIRTH, children: [{tag: TYPE, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE1}, {tag: DATE, value: ENTRY_RECORDING_DATE1}, {tag: PLAC, value: PLACE_NAME}, {tag: AGNC, value: RESPONSIBLE_AGENCY1}, {tag: CAUS, value: CAUSE_OF_EVENT1}, {tag: FAMC, ref: @F1@}, {tag: NOTE, ref: @N1@}, {tag: SOUR, ref: @S1@}]}, {tag: EVEN, value: ADOPTION, children: [{tag: TYPE, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE2}, {tag: DATE, value: ENTRY_RECORDING_DATE2}, {tag: AGNC, value: RESPONSIBLE_AGENCY2}, {tag: CAUS, value: CAUSE_OF_EVENT2}, {tag: FAMC, ref: @F1@, children: [{tag: ADOP, value: ADOPTED_BY_WHICH_PARENT}]}]}, {tag: MARR, children: [{tag: TYPE, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE3}, {tag: DATE, value: ENTRY_RECORDING_DATE3}, {tag: AGNC, value: RESPONSIBLE_AGENCY3}, {tag: CAUS, value: CAUSE_OF_EVENT3}]}, {tag: RESI, children: [{tag: TYPE, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE4}, {tag: DATE, value: ENTRY_RECORDING_DATE4}, {tag: AGNC, value: RESPONSIBLE_AGENCY4}, {tag: CAUS, value: CAUSE_OF_EVENT4}]}, {tag: EVEN, value: EVENT_DESCRIPTOR, children: [{tag: TYPE, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE5}, {tag: DATE, value: ENTRY_RECORDING_DATE5}, {tag: AGNC, value: RESPONSIBLE_AGENCY5}, {tag: CAUS, value: CAUSE_OF_EVENT5}]}, {tag: NOTE, ref: @N1@}, {tag: SOUR, ref: @S1@}, {tag: SOUR, ref: @S1@}]", destination.getFamilies().get(0).toString());
	}

}