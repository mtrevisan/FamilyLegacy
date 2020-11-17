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


class SubmitterTransformationTest{

	private final Transformer transformerTo = new Transformer(Protocol.FLEF);


	@Test
	void to(){
		final GedcomNode submitter = transformerTo.create("SUBM")
			.withID("SUBM1")
			.addChildValue("NAME", "SUBMITTER_NAME")
			.addChildValue("ADDR", "ADDRESS_LINE")
			.addChildReference("OBJE", "D1")
			.addChildValue("LANG", "LANGUAGE_PREFERENCE1")
			.addChildValue("LANG", "LANGUAGE_PREFERENCE2")
			.addChildValue("RFN", "SUBMITTER_REGISTERED_RFN")
			.addChildValue("RIN", "AUTOMATED_RECORD_ID")
			.addChildReference("NOTE", "N1")
			.addChild(transformerTo.create("CHAN")
				.addChildValue("DATE", "CHANGE_DATE")
			);
		final Gedcom origin = new Gedcom();
		origin.addSubmitter(submitter);
		final Flef destination = new Flef();

		Assertions.assertEquals("id: SUBM1, tag: SUBM, children: [{tag: NAME, value: SUBMITTER_NAME}, {tag: ADDR, value: ADDRESS_LINE}, {id: D1, tag: OBJE}, {tag: LANG, value: LANGUAGE_PREFERENCE1}, {tag: LANG, value: LANGUAGE_PREFERENCE2}, {tag: RFN, value: SUBMITTER_REGISTERED_RFN}, {tag: RIN, value: AUTOMATED_RECORD_ID}, {id: N1, tag: NOTE}, {tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE}]}]", origin.getSubmitters().get(0).toString());

		final Transformation<Gedcom, Flef> t = new SubmitterTransformation();
		t.to(origin, destination);

		Assertions.assertEquals("id: SUBM1, tag: SOURCE, children: [{tag: TITLE, value: SUBMITTER_NAME}, {id: P1, tag: PLACE}, {id: D1, tag: SOURCE}, {id: N1, tag: NOTE}, {id: N1, tag: NOTE}]", destination.getSources().get(0).toString());
	}

}