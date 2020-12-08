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


class NoteTransformationTest{

	private final Transformer transformerTo = new Transformer(Protocol.FLEF);
	private final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);


	@Test
	void to(){
		final GedcomNode note = transformerTo.create("NOTE")
			.withID("N1")
			.withValue("SUBMITTER_TEXT\\nSUBMITTER_TEXT")
			.addChild(transformerTo.create("REFN")
				.withValue("USER_REFERENCE_NUMBER")
				.addChildValue("TYPE", "USER_REFERENCE_TYPE")
			)
			.addChildValue("RIN", "AUTOMATED_RECORD_ID")
			.addChildReference("SOUR", "S1");
		final Gedcom origin = new Gedcom();
		origin.addNote(note);
		final Flef destination = new Flef();

		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXT\\nSUBMITTER_TEXT, children: [{tag: REFN, value: USER_REFERENCE_NUMBER, children: [{tag: TYPE, value: USER_REFERENCE_TYPE}]}, {tag: RIN, value: AUTOMATED_RECORD_ID}, {tag: SOUR, ref: S1}]", origin.getNotes().get(0).toString());

		final Transformation<Gedcom, Flef> t = new NoteTransformation();
		t.to(origin, destination);

		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXT\\nSUBMITTER_TEXT", destination.getNotes().get(0).toString());
	}

	@Test
	void from(){
		final GedcomNode note = transformerFrom.create("NOTE")
			.withID("N1")
			.withValue("SUBMITTER_TEXT")
			.addChildValue("RESTRICTION", "RESTRICTION_NOTICE");
		final Flef origin = new Flef();
		origin.addNote(note);
		final Gedcom destination = new Gedcom();

		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXT, children: [{tag: RESTRICTION, value: RESTRICTION_NOTICE}]", origin.getNotes().get(0).toString());

		final Transformation<Gedcom, Flef> t = new NoteTransformation();
		t.from(origin, destination);

		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXT", destination.getNotes().get(0).toString());
	}

}