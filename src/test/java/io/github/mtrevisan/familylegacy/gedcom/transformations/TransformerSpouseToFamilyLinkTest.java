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


class TransformerSpouseToFamilyLinkTest{

	private final Transformer transformerTo = new Transformer(Protocol.FLEF);
	private final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);


	@Test
	void spouseToFamilyLinkTo(){
		final GedcomNode parent = transformerTo.createEmpty()
			.addChild(transformerTo.createWithReference("FAMS", "@F1@")
				.addChildReference("NOTE", "@N1@")
			);
		final GedcomNode family = transformerTo.createWithID("FAMILY", "@F1@");
		final GedcomNode note = transformerFrom.createWithID("NOTE", "@N1@");

		Assertions.assertEquals("children: [{tag: FAMS, ref: @F1@, children: [{tag: NOTE, ref: @N1@}]}]", parent.toString());
		Assertions.assertEquals("id: @F1@, tag: FAMILY", family.toString());
		Assertions.assertEquals("id: @N1@, tag: NOTE", note.toString());

		final GedcomNode destinationNode = transformerTo.createEmpty();
		final Flef destination = new Flef();
		destination.addFamily(family);
		destination.addNote(note);
		transformerTo.spouseToFamilyLinkTo(parent, destinationNode, destination);

		Assertions.assertEquals("children: [{tag: FAMILY_SPOUSE, ref: @F1@, children: [{tag: NOTE, ref: @N1@}]}]", destinationNode.toString());
	}

	@Test
	void spouseToFamilyLinkFrom(){
		final GedcomNode parent = transformerFrom.createEmpty()
			.addChild(transformerTo.createWithReference("FAMILY_SPOUSE", "@F1@")
				.addChildReference("NOTE", "@N1@")
			);

		Assertions.assertEquals("children: [{tag: FAMILY_SPOUSE, ref: @F1@, children: [{tag: NOTE, ref: @N1@}]}]", parent.toString());

		final GedcomNode destinationNode = transformerFrom.createEmpty();
		transformerFrom.spouseToFamilyLinkFrom(parent, destinationNode);

		Assertions.assertEquals("children: [{tag: FAMS, ref: @F1@, children: [{tag: NOTE, ref: @N1@}]}]", destinationNode.toString());
	}

}