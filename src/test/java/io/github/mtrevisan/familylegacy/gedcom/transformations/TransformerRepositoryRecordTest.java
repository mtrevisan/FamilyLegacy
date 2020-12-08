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


class TransformerRepositoryRecordTest{

	private final Transformer transformerTo = new Transformer(Protocol.FLEF);
	private final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);


	@Test
	void repositoryRecordTo(){
		final GedcomNode parent = transformerTo.createEmpty()
			.addChild(transformerTo.createWithID("REPO", "@R1@"))
			.addChildValue("NAME", "NAME_OF_REPOSITORY")
			.addChildValue("ADDR", "ADDRESS_LINE")
			.addChildReference("NOTE", "@N1@");
		final GedcomNode note = transformerTo.createWithID("NOTE", "@N1@");

		Assertions.assertEquals("children: [{id: @R1@, tag: REPO}, {tag: NAME, value: NAME_OF_REPOSITORY}, {tag: ADDR, value: ADDRESS_LINE}, {tag: NOTE, ref: @N1@}]", parent.toString());
		Assertions.assertEquals("id: @N1@, tag: NOTE", note.toString());

		final GedcomNode destinationNode = transformerTo.createEmpty();
		final Flef destination = new Flef();
		transformerTo.repositoryRecordTo(parent, destinationNode, destination);

		Assertions.assertEquals("children: [{id: @R1@, tag: REPOSITORY, children: [{tag: PLACE, ref: P1}, {tag: NOTE, ref: @N1@}]}]", destinationNode.toString());
		Assertions.assertEquals("id: P1, tag: PLACE, children: [{tag: ADDRESS, value: ADDRESS_LINE}]", destination.getPlaces().get(0).toString());
	}

	@Test
	void repositoryRecordFrom(){
		final GedcomNode parent = transformerFrom.createEmpty()
			.addChild(transformerFrom.createWithID("REPOSITORY", "@R1@")
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
		transformerFrom.repositoryRecordFrom(parent, destinationNode, origin);

		Assertions.assertEquals("children: [{id: @R1@, tag: REPO, children: [{tag: NAME, value: NAME_OF_REPOSITORY}]}]", destinationNode.toString());
	}

}