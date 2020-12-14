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


class FamilyTransformationTest{

	private final Transformer transformerTo = new Transformer(Protocol.FLEF);
	private final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);


	@Test
	void to(){
		final GedcomNode family = transformerTo.create("FAM")
			.withID("F1")
			.addChildValue("RESN", "RESTRICTION_NOTICE")
			.addChild(transformerTo.create("MARR")
				.withValue("Y")
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
			.addChild(transformerTo.create("EVEN")
				.withValue("EVENT_DESCRIPTOR")
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
			.addChildValue("NCHI", "COUNT_OF_CHILDREN")
			.addChildReference("SUBM", "SUBM1")
			.addChild(transformerTo.create("REFN")
				.withValue("USER_REFERENCE_NUMBER")
				.addChildValue("TYPE", "USER_REFERENCE_TYPE")
			)
			.addChildValue("RIN", "AUTOMATED_RECORD_ID")
			.addChild(transformerTo.create("CHAN")
				.addChildValue("DATE", "CHANGE_DATE")
			)
			.addChildReference("NOTE", "N1")
			.addChildReference("SOUR", "S1")
			.addChildReference("OBJE", "D1");
		final Gedcom origin = new Gedcom();
		origin.addFamily(family);
		final Flef destination = new Flef();

		Assertions.assertEquals("id: F1, tag: FAM, children: [{tag: RESN, value: RESTRICTION_NOTICE}, {tag: MARR, value: Y, children: [{tag: HUSB, children: [{tag: AGE, value: AGE_AT_EVENT11}]}, {tag: WIFE, children: [{tag: AGE, value: AGE_AT_EVENT12}]}, {tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION1}]}, {tag: RESI, children: [{tag: HUSB, children: [{tag: AGE, value: AGE_AT_EVENT21}]}, {tag: WIFE, children: [{tag: AGE, value: AGE_AT_EVENT22}]}, {tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION2}]}, {tag: EVEN, value: EVENT_DESCRIPTOR, children: [{tag: HUSB, children: [{tag: AGE, value: AGE_AT_EVENT31}]}, {tag: WIFE, children: [{tag: AGE, value: AGE_AT_EVENT32}]}, {tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION3}]}, {tag: HUSB, ref: I1}, {tag: WIFE, ref: I2}, {tag: CHIL, ref: I3}, {tag: NCHI, value: COUNT_OF_CHILDREN}, {tag: SUBM, ref: SUBM1}, {tag: REFN, value: USER_REFERENCE_NUMBER, children: [{tag: TYPE, value: USER_REFERENCE_TYPE}]}, {tag: RIN, value: AUTOMATED_RECORD_ID}, {tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE}]}, {tag: NOTE, ref: N1}, {tag: SOUR, ref: S1}, {tag: OBJE, ref: D1}]", origin.getFamilies().get(0).toString());

		final Transformation<Gedcom, Flef> t = new FamilyTransformation();
		t.to(origin, destination);

		Assertions.assertEquals("id: F1, tag: FAMILY, children: [{tag: SPOUSE1, ref: I1}, {tag: SPOUSE2, ref: I2}, {tag: CHILD, ref: I3}, {tag: NOTE, ref: N1}, {tag: SOURCE, ref: S1}, {tag: SOURCE, ref: D1}, {tag: EVENT, value: MARRIAGE, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION1}]}, {tag: EVENT, value: RESIDENCE, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION2}]}, {tag: EVENT, value: EVENT_DESCRIPTOR, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION3}]}, {tag: RESTRICTION, value: RESTRICTION_NOTICE}]", destination.getFamilies().get(0).toString());
	}

	@Test
	void from(){
		final GedcomNode family = transformerFrom.create("FAMILY")
			.withID("F1")
			.addChildValue("TYPE", "FAMILY_TYPE")
			.addChildReference("SPOUSE1", "I1")
			.addChildReference("SPOUSE2", "I2")
			.addChildReference("CHILD", "I3")
			.addChild(transformerFrom.create("GROUP")
				.withXRef("G1")
				.addChildValue("ROLE", "ROLE_IN_GROUP")
				.addChildReference("NOTE", "N1")
				.addChildValue("CREDIBILITY", "CREDIBILITY_ASSESSMENT")
				.addChildValue("RESTRICTION", "RESTRICTION_NOTICE")
			)
			.addChildReference("CULTURAL_RULE", "A1")
			.addChildReference("NOTE", "N2")
			.addChildReference("SOUR", "S2")
			.addChild(transformerFrom.create("EVENT")
				.withValue("MARRIAGE")
				.addChildValue("TYPE", "EVENT_OR_FACT_CLASSIFICATION1")
			)
			.addChild(transformerFrom.create("EVENT")
				.withValue("EVENT_DESCRIPTOR")
				.addChildValue("TYPE", "EVENT_OR_FACT_CLASSIFICATION2")
			)
			.addChildValue("RESTRICTION", "RESTRICTION_NOTICE");
		final Flef origin = new Flef();
		origin.addFamily(family);
		final Gedcom destination = new Gedcom();

		Assertions.assertEquals("id: F1, tag: FAMILY, children: [{tag: TYPE, value: FAMILY_TYPE}, {tag: SPOUSE1, ref: I1}, {tag: SPOUSE2, ref: I2}, {tag: CHILD, ref: I3}, {tag: GROUP, ref: G1, children: [{tag: ROLE, value: ROLE_IN_GROUP}, {tag: NOTE, ref: N1}, {tag: CREDIBILITY, value: CREDIBILITY_ASSESSMENT}, {tag: RESTRICTION, value: RESTRICTION_NOTICE}]}, {tag: CULTURAL_RULE, ref: A1}, {tag: NOTE, ref: N2}, {tag: SOUR, ref: S2}, {tag: EVENT, value: MARRIAGE, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION1}]}, {tag: EVENT, value: EVENT_DESCRIPTOR, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION2}]}, {tag: RESTRICTION, value: RESTRICTION_NOTICE}]", origin.getFamilies().get(0).toString());

		final Transformation<Gedcom, Flef> t = new FamilyTransformation();
		t.from(origin, destination);

		Assertions.assertEquals("id: F1, tag: FAM, children: [{tag: RESN, value: RESTRICTION_NOTICE}, {tag: MARR, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION1}]}, {tag: EVEN, children: [{tag: TYPE, value: EVENT_OR_FACT_CLASSIFICATION2}]}, {tag: CHIL, ref: I3}, {tag: NOTE, ref: N2}]", destination.getFamilies().get(0).toString());
	}

}