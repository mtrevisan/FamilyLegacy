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

	@Test
	void repositoryRecordTo(){
		final Transformer transformerTo = new Transformer(Protocol.FLEF);
		final GedcomNode repository = transformerTo.createWithID("REPO", "R1")
			.addChildValue("NAME", "NAME_OF_REPOSITORY")
			.addChildValue("ADDR", "ADDRESS_LINE")
			.addChildReference("NOTE", "N1");
		final GedcomNode note = transformerTo.createWithID("NOTE", "N1");

		Assertions.assertEquals("id: R1, tag: REPO, children: [{tag: NAME, value: NAME_OF_REPOSITORY}, {tag: ADDR, value: ADDRESS_LINE}, {tag: NOTE, ref: N1}]", repository.toString());
		Assertions.assertEquals("id: N1, tag: NOTE", note.toString());

		final Gedcom origin = new Gedcom();
		origin.addNote(note);
		final Flef destination = new Flef();
		transformerTo.repositoryRecordTo(repository, origin, destination);

		Assertions.assertEquals("id: R1, tag: REPOSITORY, children: [{tag: NAME, value: NAME_OF_REPOSITORY}, {tag: PLACE, ref: P1}, {tag: NOTE, ref: N1}]", destination.getRepositories().get(0).toString());
		Assertions.assertEquals("id: P1, tag: PLACE, children: [{tag: ADDRESS, value: ADDRESS_LINE}]", destination.getPlaces().get(0).toString());
	}

	@Test
	void repositoryRecordFrom(){
		final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);
		final GedcomNode repository = transformerFrom.createWithID("REPOSITORY", "R1")
			.addChildValue("NAME", "NAME_OF_REPOSITORY")
			.addChildReference("INDIVIDUAL", "I1")
			.addChildReference("PLACE", "P1")
			.addChildValue("PHONE", "PHONE_NUMBER")
			.addChildReference("NOTE", "N1");
		final GedcomNode place = transformerFrom.createWithID("PLACE", "P1")
			.addChildValue("NAME", "PLACE_NAME")
			.addChild(transformerFrom.create("ADDRESS")
				.addChildValue("COUNTRY", "ADDRESS_COUNTRY")
			);
		final GedcomNode note = transformerFrom.createWithID("NOTE", "N1");

		Assertions.assertEquals("id: R1, tag: REPOSITORY, children: [{tag: NAME, value: NAME_OF_REPOSITORY}, {tag: INDIVIDUAL, ref: I1}, {tag: PLACE, ref: P1}, {tag: PHONE, value: PHONE_NUMBER}, {tag: NOTE, ref: N1}]", repository.toString());

		final Flef origin = new Flef();
		origin.addRepository(repository);
		origin.addPlace(place);
		origin.addNote(note);
		final Gedcom destination = new Gedcom();
		transformerFrom.repositoryRecordFrom(repository, origin, destination);

		Assertions.assertEquals("id: R1, tag: REPO, children: [{tag: NAME, value: NAME_OF_REPOSITORY}, {tag: ADDR, children: [{tag: CTRY, value: ADDRESS_COUNTRY}]}, {tag: NOTE, ref: N1}]", destination.getRepositories().get(0).toString());
	}

}