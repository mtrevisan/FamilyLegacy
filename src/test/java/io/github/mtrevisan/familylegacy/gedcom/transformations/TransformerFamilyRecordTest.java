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


class TransformerFamilyRecordTest{

	@Test
	void familyRecordTo(){
		final Transformer transformerTo = new Transformer(Protocol.FLEF);
		final GedcomNode family = transformerTo.createWithID("FAM", "F1")
			.addChildValue("RESN", "RESTRICTION_NOTICE")
			.addChild(transformerTo.createWithValue("MARR", "Y")
				.addChild(transformerTo.create("HUSB")
					.addChildValue("AGE", "AGE_AT_EVENT11")
				)
				.addChild(transformerTo.create("WIFE")
					.addChildValue("AGE", "AGE_AT_EVENT12")
				)
				.addChildValue("TYPE", "EVENT_OR_FACT_CLASSIFICATION1")
			)
			.addChild(transformerTo.create("RESI")
				.addChild(transformerTo.create("HUSB")
					.addChildValue("AGE", "AGE_AT_EVENT21")
				)
				.addChild(transformerTo.create("WIFE")
					.addChildValue("AGE", "AGE_AT_EVENT22")
				)
				.addChildValue("TYPE", "EVENT_OR_FACT_CLASSIFICATION2")
			)
			.addChild(transformerTo.createWithValue("EVEN", "EVENT_DESCRIPTOR")
				.addChild(transformerTo.create("HUSB")
					.addChildValue("AGE", "AGE_AT_EVENT31")
				)
				.addChild(transformerTo.create("WIFE")
					.addChildValue("AGE", "AGE_AT_EVENT32")
				)
				.addChildValue("TYPE", "EVENT_OR_FACT_CLASSIFICATION3")
			)
			.addChildReference("HUSB", "I1")
			.addChildReference("WIFE", "I2")
			.addChildReference("CHIL", "I3")
			.addChildValue("NCHI", "5")
			.addChildReference("SUBM", "SUBM1")
			.addChild(transformerTo.createWithValue("REFN", "USER_REFERENCE_NUMBER")
				.addChildValue("TYPE", "USER_REFERENCE_TYPE")
			)
			.addChildValue("RIN", "AUTOMATED_RECORD_ID")
			.addChildReference("NOTE", "N1")
			.addChildReference("SOUR", "S1")
			.addChildReference("OBJE", "O1");
		final GedcomNode note1 = transformerTo.createWithIDValue("NOTE", "N1", "NOTE_1");
		final GedcomNode source = transformerTo.createWithID("SOUR", "S1");
		final GedcomNode object = transformerTo.createWithID("OBJE", "O1");

		Assertions.assertEquals("id: F1, tag: FAM, children: [{tag: RESN, value: RESTRICTION_NOTICE}, {tag: MARR, value: Y, children: [{tag: HUSB, children: [{tag: AGE, value: AGE_AT_EVENT11}]}, {tag: WIFE, children: [{tag: AGE, value: AGE_AT_EVENT12}]}, {tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION1}]}, {tag: RESI, children: [{tag: HUSB, children: [{tag: AGE, value: AGE_AT_EVENT21}]}, {tag: WIFE, children: [{tag: AGE, value: AGE_AT_EVENT22}]}, {tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION2}]}, {tag: EVEN, value: EVENT_DESCRIPTOR, children: [{tag: HUSB, children: [{tag: AGE, value: AGE_AT_EVENT31}]}, {tag: WIFE, children: [{tag: AGE, value: AGE_AT_EVENT32}]}, {tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION3}]}, {tag: HUSB, ref: I1}, {tag: WIFE, ref: I2}, {tag: CHIL, ref: I3}, {tag: NCHI, value: 5}, {tag: SUBM, ref: SUBM1}, {tag: REFN, value: USER_REFERENCE_NUMBER, children: [{tag: TYPE, value: USER_REFERENCE_TYPE}]}, {tag: RIN, value: AUTOMATED_RECORD_ID}, {tag: NOTE, ref: N1}, {tag: SOUR, ref: S1}, {tag: OBJE, ref: O1}]", family.toString());
		Assertions.assertEquals("id: N1, tag: NOTE, value: NOTE_1", note1.toString());

		final Gedcom origin = new Gedcom();
		origin.addFamily(family);
		origin.addNote(note1);
		origin.addSource(source);
		origin.addObject(object);
		final Flef destination = new Flef();
		transformerTo.noteRecordTo(note1, destination);
		transformerTo.familyRecordTo(family, origin, destination);

		Assertions.assertEquals("id: F1, tag: FAMILY, children: [{tag: PARTNER1, ref: I1}, {tag: PARTNER2, ref: I2}, {tag: CHILD, ref: I3}, {tag: EVENT, ref: E1}, {tag: EVENT, ref: E2}, {tag: EVENT, ref: E3}, {tag: EVENT, ref: E4}, {tag: NOTE, ref: N2}, {tag: SOURCE, ref: S1}, {tag: SOURCE, ref: S2}, {tag: CREDIBILITY, value: RESTRICTION_NOTICE}]", destination.getFamilies().get(0).toString());
		Assertions.assertEquals("id: E1, tag: EVENT, children: [{tag: TYPE, value: MARRIAGE}, {tag: DESCRIPTION, value: EVENT_OR_FACT_CLASSIFICATION1}, {tag: FAMILY, ref: F1}]", destination.getEvents().get(0).toString());
		Assertions.assertEquals("id: E2, tag: EVENT, children: [{tag: TYPE, value: RESIDENCE}, {tag: FAMILY, ref: F1}]", destination.getEvents().get(1).toString());
		Assertions.assertEquals("id: E3, tag: EVENT, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION3}, {tag: DESCRIPTION, value: EVENT_DESCRIPTOR}, {tag: FAMILY, ref: F1}]", destination.getEvents().get(2).toString());
		Assertions.assertEquals("id: E4, tag: EVENT, children: [{tag: TYPE, value: CHILDREN_COUNT}, {tag: DESCRIPTION, value: 5}, {tag: FAMILY, ref: F1}]", destination.getEvents().get(3).toString());
	}

	@Test
	void familyRecordFrom(){
		final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);
		final GedcomNode family = transformerFrom.createWithID("FAMILY", "F1")
			.addChildValue("TYPE", "marriage")
			.addChildReference("PARTNER1", "I1")
			.addChildReference("PARTNER2", "I2")
			.addChildReference("CHILD", "I3")
			.addChildReference("EVENT", "E1")
			.addChildReference("EVENT", "E2")
			.addChildReference("EVENT", "E3")
			.addChildReference("EVENT", "E4")
			.addChildReference("EVENT", "E5")
			.addChildReference("GROUP", "G1")
			.addChildReference("CULTURAL_RULE", "C1")
			.addChildReference("NOTE", "N1")
			.addChildReference("SOURCE", "S1")
			.addChild(transformerFrom.createWithValue("PREFERRED_IMAGE", "IMAGE_FILE_REFERENCE")
				.addChildValue("CROP", "CROP_COORDINATES")
			)
			.addChildValue("RESTRICTION", "RESTRICTION_NOTICE");
		final GedcomNode event1 = transformerFrom.createWithID("EVENT", "E1")
			.addChildValue("TYPE", "BIRTH")
			.addChildValue("DESCRIPTION", "EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE1")
			.addChildReference("INDIVIDUAL", "I3")
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
			.addChildReference("INDIVIDUAL", "I3")
			.addChildReference("FAMILY", "F1")
			.addChildValue("DATE", "ENTRY_RECORDING_DATE2")
			.addChildValue("AGENCY", "RESPONSIBLE_AGENCY2")
			.addChildValue("CAUSE", "CAUSE_OF_EVENT2");
		final GedcomNode event3 = transformerFrom.createWithID("EVENT", "E3")
			.addChildValue("TYPE", "RESIDENCE")
			.addChildValue("DESCRIPTION", "EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE3")
			.addChildValue("DATE", "ENTRY_RECORDING_DATE3")
			.addChildValue("AGENCY", "RESPONSIBLE_AGENCY3")
			.addChildValue("CAUSE", "CAUSE_OF_EVENT3");
		final GedcomNode event4 = transformerFrom.createWithID("EVENT", "E4")
			.addChildValue("TYPE", "EVENT_DESCRIPTOR")
			.addChildValue("DESCRIPTION", "EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE4")
			.addChildValue("DATE", "ENTRY_RECORDING_DATE4")
			.addChildValue("AGENCY", "RESPONSIBLE_AGENCY4")
			.addChildValue("CAUSE", "CAUSE_OF_EVENT4");
		final GedcomNode event5 = transformerFrom.createWithID("EVENT", "E5")
			.addChildValue("TYPE", "custom")
			.addChildValue("DESCRIPTION", "EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE5")
			.addChildValue("DATE", "ENTRY_RECORDING_DATE5")
			.addChildValue("AGENCY", "RESPONSIBLE_AGENCY5")
			.addChildValue("CAUSE", "CAUSE_OF_EVENT5");
		final GedcomNode source = transformerFrom.createWithID("SOURCE", "S1");
		final GedcomNode place = transformerFrom.createWithID("PLACE", "P1")
			.addChildValue("NAME", "PLACE_NAME");
		final GedcomNode note = transformerFrom.createWithIDValue("NOTE", "N1", "NOTE 1");

		Assertions.assertEquals("id: F1, tag: FAMILY, children: [{tag: TYPE, value: marriage}, {tag: PARTNER1, ref: I1}, {tag: PARTNER2, ref: I2}, {tag: CHILD, ref: I3}, {tag: EVENT, ref: E1}, {tag: EVENT, ref: E2}, {tag: EVENT, ref: E3}, {tag: EVENT, ref: E4}, {tag: EVENT, ref: E5}, {tag: GROUP, ref: G1}, {tag: CULTURAL_RULE, ref: C1}, {tag: NOTE, ref: N1}, {tag: SOURCE, ref: S1}, {tag: PREFERRED_IMAGE, value: IMAGE_FILE_REFERENCE, children: [{tag: CROP, value: CROP_COORDINATES}]}, {tag: RESTRICTION, value: RESTRICTION_NOTICE}]", family.toString());

		final Flef origin = new Flef();
		origin.addFamily(family);
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
		destination.addNote(note);
		transformerFrom.familyRecordFrom(family, origin, destination);

		Assertions.assertEquals("id: F1, tag: FAM, children: [{tag: HUSB, ref: I1}, {tag: WIFE, ref: I2}, {tag: CHIL, ref: I3}, {tag: BIRT, value: Y, children: [{tag: TYPE, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE1}, {tag: DATE, value: ENTRY_RECORDING_DATE1}, {tag: PLAC, value: PLACE_NAME}, {tag: AGNC, value: RESPONSIBLE_AGENCY1}, {tag: CAUS, value: CAUSE_OF_EVENT1}, {tag: NOTE, ref: N1}, {tag: SOUR, ref: S2}]}, {tag: ADOP, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE2, children: [{tag: DATE, value: ENTRY_RECORDING_DATE2}, {tag: AGNC, value: RESPONSIBLE_AGENCY2}, {tag: CAUS, value: CAUSE_OF_EVENT2}]}, {tag: RESI, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE3, children: [{tag: DATE, value: ENTRY_RECORDING_DATE3}, {tag: AGNC, value: RESPONSIBLE_AGENCY3}, {tag: CAUS, value: CAUSE_OF_EVENT3}]}, {tag: EVEN, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE4, children: [{tag: TYPE, value: EVENT_DESCRIPTOR}, {tag: DATE, value: ENTRY_RECORDING_DATE4}, {tag: AGNC, value: RESPONSIBLE_AGENCY4}, {tag: CAUS, value: CAUSE_OF_EVENT4}]}, {tag: EVEN, value: EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE5, children: [{tag: TYPE, value: custom}, {tag: DATE, value: ENTRY_RECORDING_DATE5}, {tag: AGNC, value: RESPONSIBLE_AGENCY5}, {tag: CAUS, value: CAUSE_OF_EVENT5}]}, {tag: NOTE, ref: N1}, {tag: SOUR, ref: S2}]", destination.getFamilies().get(0).toString());
	}

}