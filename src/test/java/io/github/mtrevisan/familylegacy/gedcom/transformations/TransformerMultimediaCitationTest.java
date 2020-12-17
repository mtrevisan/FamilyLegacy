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


class TransformerMultimediaCitationTest{

	@Test
	void multimediaCitationToXRef(){
		final Transformer transformerTo = new Transformer(Protocol.FLEF);
		final GedcomNode parent = transformerTo.createEmpty()
			.addChild(transformerTo.createWithReference("OBJE", "M1"));
		final GedcomNode object = transformerTo.createWithID("OBJE", "M1")
			.addChildValue("FILE", "MULTIMEDIA_FILE_REFN");

		final GedcomNode destinationNode = transformerTo.createEmpty();
		final Gedcom origin = new Gedcom();
		origin.addObject(object);
		final Flef destination = new Flef();
		transformerTo.multimediaCitationTo(parent, destinationNode, origin, destination);

		Assertions.assertEquals("children: [{tag: SOURCE, ref: S1}]", destinationNode.toString());
	}

	@Test
	void multimediaCitationToNoXRef(){
		final Transformer transformerTo = new Transformer(Protocol.FLEF);
		final GedcomNode parent = transformerTo.createEmpty()
			.addChild(transformerTo.create("OBJE")
				.addChildValue("TITL", "DESCRIPTIVE_TITLE")
				.addChild(transformerTo.createWithValue("FORM", "MULTIMEDIA_FORMAT")
					.addChild(transformerTo.createWithValue("MEDI", "SOURCE_MEDIA_TYPE"))
				)
				.addChildValue("FILE", "MULTIMEDIA_FILE_REFN")
				.addChildValue("_CUTD", "CUT_COORDINATES")
				.addChildValue("_PREF", "PREFERRED_MEDIA")
			);

		Assertions.assertEquals("children: [{tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FORM, value: MULTIMEDIA_FORMAT, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: _CUTD, value: CUT_COORDINATES}, {tag: _PREF, value: PREFERRED_MEDIA}]}]", parent.toString());

		final GedcomNode destinationNode = transformerTo.createEmpty();
		final Gedcom origin = new Gedcom();
		final Flef destination = new Flef();
		transformerTo.multimediaCitationTo(parent, destinationNode, origin, destination);

		Assertions.assertEquals("children: [{tag: SOURCE, ref: S1, children: [{tag: CUTOUT, value: CUT_COORDINATES}]}]", destinationNode.toString());
	}

}