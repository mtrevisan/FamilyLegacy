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
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class TransformerMultimediaCitationTest{

	private final Transformer transformerTo = new Transformer(Protocol.FLEF);
	private final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);


	@Test
	void multimediaCitationToXRef(){
		final GedcomNode parent = transformerTo.createEmpty()
			.addChild(transformerTo.createWithReference("OBJE", "@M1@"));

		final GedcomNode destinationNode = transformerTo.createEmpty();
		final Flef destination = new Flef();
		transformerTo.multimediaCitationTo(parent, destinationNode, destination);

		Assertions.assertEquals("children: [{tag: MULTIMEDIA, ref: @M1@}]", destinationNode.toString());
	}

	@Test
	void multimediaCitationToNoXRef(){
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
		final Flef destination = new Flef();
		transformerTo.multimediaCitationTo(parent, destinationNode, destination);

		Assertions.assertEquals("children: [{tag: MULTIMEDIA, ref: M1, children: [{tag: CUTOUT, value: CUT_COORDINATES}]}]", destinationNode.toString());
		Assertions.assertEquals("id: M1, tag: MULTIMEDIA, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: MEDIA_TYPE, value: SOURCE_MEDIA_TYPE}]}]", destination.getMultimedias().get(0).toString());
	}

	@Test
	void multimediaCitationFrom(){
		final GedcomNode parent = transformerFrom.createEmpty()
			.addChild(transformerFrom.createWithReference("MULTIMEDIA", "@M1@")
				.addChildValue("CUTOUT", "CUTOUT_COORDINATES")
				.addChildReference("NOTE", "@N1@")
				.addChildValue("CREDIBILITY", "CREDIBILITY_ASSESSMENT")
			);
		final GedcomNode multimedia = transformerFrom.createWithID("MULTIMEDIA", "@M1@")
			.addChildValue("TITLE", "DOCUMENT_TITLE")
			.addChild(transformerFrom.create("FILE")
				.withValue("DOCUMENT_FILE_REFERENCE")
				.addChildValue("MEDIA_TYPE", "SOURCE_MEDIA_TYPE")
			);
		final GedcomNode note = transformerFrom.createWithID("NOTE", "@N1@");

		Assertions.assertEquals("children: [{tag: MULTIMEDIA, ref: @M1@, children: [{tag: CUTOUT, value: CUTOUT_COORDINATES}, {tag: NOTE, ref: @N1@}, {tag: CREDIBILITY, value: CREDIBILITY_ASSESSMENT}]}]", parent.toString());
		Assertions.assertEquals("id: @N1@, tag: NOTE", note.toString());

		final GedcomNode destinationNode = transformerFrom.createEmpty();
		final Flef destination = new Flef();
		destination.addMultimedia(multimedia);
		destination.addNote(note);
		transformerFrom.multimediaCitationFrom(parent, destinationNode, destination);

		Assertions.assertEquals("children: [{tag: OBJE, children: [{tag: TITL, value: DOCUMENT_TITLE}, {tag: FILE, value: DOCUMENT_FILE_REFERENCE}, {tag: FORM, children: [{tag: TITL, value: SOURCE_MEDIA_TYPE}]}, {tag: _CUTD, value: CUTOUT_COORDINATES}]}]", destinationNode.toString());
	}

}