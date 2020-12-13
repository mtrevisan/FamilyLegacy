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


class TransformerSourceRepositoryCitationTest{

	@Test
	void sourceRepositoryCitationTo(){
		final Transformer transformerTo = new Transformer(Protocol.FLEF);
		final GedcomNode parent = transformerTo.createEmpty()
			.addChild(transformerTo.createWithReference("REPO", "@R1@")
				.addChildReference("NOTE", "@N1@")
				.addChild(transformerTo.create("CALN")
					.withValue("SOURCE_CALL_NUMBER")
					.addChildValue("MEDI", "SOURCE_MEDIA_TYPE")
				)
			);
		final GedcomNode repository = transformerTo.createWithID("REPO", "@R1@");
		final GedcomNode note = transformerTo.createWithID("NOTE", "@N1@");

		Assertions.assertEquals("children: [{tag: REPO, ref: @R1@, children: [{tag: NOTE, ref: @N1@}, {tag: CALN, value: SOURCE_CALL_NUMBER, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]", parent.toString());
		Assertions.assertEquals("id: @R1@, tag: REPO", repository.toString());
		Assertions.assertEquals("id: @N1@, tag: NOTE", note.toString());

		final GedcomNode destinationNode = transformerTo.createEmpty();
		final Flef destination = new Flef();
		destination.addRepository(repository);
		destination.addNote(note);
		transformerTo.sourceRepositoryCitationTo(parent, destinationNode, destination);

		Assertions.assertEquals("children: [{tag: REPOSITORY, ref: @R1@, children: [{tag: LOCATION, value: SOURCE_CALL_NUMBER}, {tag: NOTE, ref: @N1@}]}]", destinationNode.toString());
	}

	@Test
	void sourceRepositoryCitationFrom(){
		final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);
		final GedcomNode parent = transformerFrom.createEmpty()
			.addChild(transformerFrom.createWithReference("REPOSITORY", "@R1@")
				.addChildValue("LOCATION", "REPOSITORY_LOCATION_TEXT")
				.addChildReference("NOTE", "@N1@")
			);

		Assertions.assertEquals("children: [{tag: REPOSITORY, ref: @R1@, children: [{tag: LOCATION, value: REPOSITORY_LOCATION_TEXT}, {tag: NOTE, ref: @N1@}]}]", parent.toString());

		final GedcomNode destinationNode = transformerFrom.createEmpty();
		transformerFrom.sourceRepositoryCitationFrom(parent, destinationNode);

		Assertions.assertEquals("children: [{tag: REPO, ref: @R1@, children: [{tag: CALN, value: REPOSITORY_LOCATION_TEXT}, {tag: NOTE, ref: @N1@}]}]", destinationNode.toString());
	}

}