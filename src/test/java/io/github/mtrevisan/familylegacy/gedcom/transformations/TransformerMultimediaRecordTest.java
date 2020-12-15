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


class TransformerMultimediaRecordTest{

	@Test
	void multimediaRecordTo(){
		final Transformer transformerTo = new Transformer(Protocol.FLEF);
		final GedcomNode object = transformerTo.createWithID("OBJE", "M1")
			.addChild(transformerTo.createWithValue("FILE", "MULTIMEDIA_FILE_REFN")
				.addChild(transformerTo.createWithValue("FORM", "MULTIMEDIA_FORMAT")
					.addChildValue("TYPE", "SOURCE_MEDIA_TYPE")
				)
				.addChildValue("TITL", "DESCRIPTIVE_TITLE")
			)
			.addChild(transformerTo.createWithValue("REFN", "USER_REFERENCE_NUMBER")
				.addChildValue("TYPE", "USER_REFERENCE_TYPE")
			)
			.addChildValue("RIN", "AUTOMATED_RECORD_ID")
			.addChildReference("NOTE", "N1")
			.addChildReference("SOUR", "S1");

		final Gedcom origin = new Gedcom();
		origin.addObject(object);
		origin.addNote(transformerTo.createWithIDValue("NOTE", "N1", "SUBMITTER_TEXT"));
		origin.addNote(transformerTo.createWithID("SOUR", "S1"));
		final Flef destination = new Flef();
		transformerTo.multimediaRecordTo(object, destination);

		Assertions.assertEquals("id: S1, tag: SOURCE, children: [{tag: MEDIA_TYPE, value: SOURCE_MEDIA_TYPE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: DESCRIPTION, value: DESCRIPTIVE_TITLE}]}, {tag: NOTE, ref: N1}]", destination.getSources().get(0).toString());
	}

}