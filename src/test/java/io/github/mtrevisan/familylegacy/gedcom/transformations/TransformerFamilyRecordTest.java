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

	private final Transformer transformerTo = new Transformer(Protocol.FLEF);
	private final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);


	@Test
	void familyRecordTo(){
		final GedcomNode parent = transformerTo.createEmpty()
			.addChild(transformerTo.create("FAM")
				.withID("@F1@")
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

		Assertions.assertEquals("children: [{id: @F1@, tag: FAM, children: [{tag: RESN, value: RESTRICTION_NOTICE}, {tag: MARR, value: Y, children: [{tag: HUSB, children: [{tag: AGE, value: AGE_AT_EVENT11}]}, {tag: WIFE, children: [{tag: AGE, value: AGE_AT_EVENT12}]}, {tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION1}]}, {tag: RESI, children: [{tag: HUSB, children: [{tag: AGE, value: AGE_AT_EVENT21}]}, {tag: WIFE, children: [{tag: AGE, value: AGE_AT_EVENT22}]}, {tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION2}]}, {tag: EVEN, value: EVENT_DESCRIPTOR, children: [{tag: HUSB, children: [{tag: AGE, value: AGE_AT_EVENT31}]}, {tag: WIFE, children: [{tag: AGE, value: AGE_AT_EVENT32}]}, {tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION3}]}, {tag: HUSB, ref: @I1@}, {tag: WIFE, ref: @I2@}, {tag: CHIL, ref: @I3@}, {tag: NCHI, value: 5}, {tag: SUBM, value: @SUBM1@}, {tag: REFN, value: USER_REFERENCE_NUMBER, children: [{tag: TYPE, value: USER_REFERENCE_TYPE}]}, {tag: RIN, value: AUTOMATED_RECORD_ID}, {tag: NOTE, ref: @N1@}, {tag: SOUR, ref: @S1@}, {tag: OBJE, ref: @O1@}]}]", parent.toString());
		Assertions.assertEquals("id: @N1@, tag: NOTE", note.toString());

		final Gedcom origin = new Gedcom();
		origin.addNote(note);
		origin.addSource(source);
		origin.addObject(object);
		final Flef destination = new Flef();
		transformerTo.familyRecordTo(parent, origin, destination);

		Assertions.assertEquals("id: @F1@, tag: FAMILY, children: [{tag: SPOUSE1, value: @I1@}, {tag: SPOUSE2, value: @I2@}, {tag: CHILD, ref: @I3@}, {tag: EVENT, children: [{tag: TYPE, value: MARRIAGE}, {tag: DESCRIPTION, value: EVENT_OR_FACT_CLASSIFICATION1}, {tag: DATE, children: [{tag: CALENDAR, value: gregorian}]}]}, {tag: EVENT, children: [{tag: TYPE, value: RESIDENCE}, {tag: DESCRIPTION, value: EVENT_OR_FACT_CLASSIFICATION2}, {tag: DATE, children: [{tag: CALENDAR, value: gregorian}]}]}, {tag: EVENT, children: [{tag: TYPE, value: EVENT_DESCRIPTOR}, {tag: DESCRIPTION, value: EVENT_OR_FACT_CLASSIFICATION3}, {tag: DATE, children: [{tag: CALENDAR, value: gregorian}]}]}, {tag: NOTE, ref: @N1@}, {tag: SOURCE, ref: @S1@}, {tag: NOTE, ref: @N1@}]", destination.getFamilies().get(0).toString());
	}

	@Test
	void familyRecordFrom(){
		final GedcomNode parent = transformerFrom.createEmpty()
			.addChild(transformerFrom.createWithID("FAMILY", "@F1@")
				.addChildValue("NAME", "NAME_OF_REPOSITORY")
				.addChildValue("INDIVIDUAL", "@I1@")
				.addChildValue("PLACE", "@P1@")
				.addChildValue("PHONE", "PHONE_NUMBER")
				.addChildReference("NOTE", "@N1@")
			);
		final GedcomNode place = transformerTo.createWithID("PLACE", "@P1@")
			.addChildValue("NAME", "PLACE_NAME");
		final GedcomNode note = transformerTo.createWithID("NOTE", "@N1@");

		Assertions.assertEquals("children: [{id: @R1@, tag: REPOSITORY, children: [{tag: NAME, value: NAME_OF_REPOSITORY}, {tag: INDIVIDUAL, value: @I1@}, {tag: PLACE, value: @P1@}, {tag: PHONE, value: PHONE_NUMBER}, {tag: NOTE, ref: @N1@}]}]", parent.toString());

		final GedcomNode destinationNode = transformerFrom.createEmpty();
		final Flef origin = new Flef();
		origin.addPlace(place);
		origin.addNote(note);
		final Gedcom destination = new Gedcom();
		transformerFrom.familyRecordFrom(parent, destinationNode, origin, destination);

		Assertions.assertEquals("children: [{id: @R1@, tag: REPO, children: [{tag: NAME, value: NAME_OF_REPOSITORY}]}]", destinationNode.toString());
	}

}