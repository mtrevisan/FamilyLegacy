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


class TransformerPlaceAddressContactStructureTest{

	@Test
	void addressStructureToWithAddress(){
		final Transformer transformerTo = new Transformer(Protocol.FLEF);
		final GedcomNode parent = transformerTo.createEmpty()
			.addChild(transformerTo.createWithValue("ADDR", "ADDRESS_LINE0")
				.addChildValue("ADR1", "ADDRESS_LINE1")
				.addChildValue("ADR2", "ADDRESS_LINE2")
				.addChildValue("ADR3", "ADDRESS_LINE3")
				.addChildValue("CITY", "ADDRESS_CITY")
				.addChildValue("STAE", "ADDRESS_STATE")
				.addChildValue("POST", "ADDRESS_POSTAL_CODE")
				.addChildValue("CTRY", "ADDRESS_COUNTRY")
			);

		Assertions.assertEquals("children: [{tag: ADDR, value: ADDRESS_LINE0, children: [{tag: ADR1, value: ADDRESS_LINE1}, {tag: ADR2, value: ADDRESS_LINE2}, {tag: ADR3, value: ADDRESS_LINE3}, {tag: CITY, value: ADDRESS_CITY}, {tag: STAE, value: ADDRESS_STATE}, {tag: POST, value: ADDRESS_POSTAL_CODE}, {tag: CTRY, value: ADDRESS_COUNTRY}]}]", parent.toString());

		final GedcomNode destinationNode = transformerTo.createEmpty();
		final Gedcom origin = new Gedcom();
		final Flef destination = new Flef();
		transformerTo.placeAddressStructureTo(parent, destinationNode, origin, destination);

		Assertions.assertEquals("children: [{tag: PLACE, ref: P1}]", destinationNode.toString());
		Assertions.assertEquals("id: P1, tag: PLACE, children: [{tag: ADDRESS, value: ADDRESS_LINE0, ADDRESS_LINE1, ADDRESS_LINE2, ADDRESS_LINE3}]", destination.getPlaces().get(0).toString());
	}

	@Test
	void addressStructureToWithoutAddress(){
		final Transformer transformerTo = new Transformer(Protocol.FLEF);
		final GedcomNode parent = transformerTo.createEmpty()
			.addChild(transformerTo.create("ADDR")
				.addChildValue("CITY", "ADDRESS_CITY")
				.addChildValue("STAE", "ADDRESS_STATE")
				.addChildValue("POST", "ADDRESS_POSTAL_CODE")
				.addChildValue("CTRY", "ADDRESS_COUNTRY")
			);

		Assertions.assertEquals("children: [{tag: ADDR, children: [{tag: CITY, value: ADDRESS_CITY}, {tag: STAE, value: ADDRESS_STATE}, {tag: POST, value: ADDRESS_POSTAL_CODE}, {tag: CTRY, value: ADDRESS_COUNTRY}]}]", parent.toString());

		final GedcomNode destinationNode = transformerTo.createEmpty();
		final Gedcom origin = new Gedcom();
		final Flef destination = new Flef();
		transformerTo.placeAddressStructureTo(parent, destinationNode, origin, destination);

		Assertions.assertEquals("children: [{tag: PLACE, ref: P1}]", destinationNode.toString());
		Assertions.assertEquals("id: P1, tag: PLACE, children: [{tag: ADDRESS, value: ADDRESS_CITY, ADDRESS_STATE, ADDRESS_COUNTRY, children: [{tag: HIERARCHY, value: City, State, Country}]}]", destination.getPlaces().get(0).toString());
	}

	@Test
	void placeStructureTo(){
		final Transformer transformerTo = new Transformer(Protocol.FLEF);
		final GedcomNode parent = transformerTo.createEmpty()
			.addChild(transformerTo.createWithValue("PLAC", "PLACE_NAME")
				.addChildValue("FORM", "PLACE_HIERARCHY")
				.addChildValue("FONE", "PLACE_PHONETIC_VARIATION")
				.addChildValue("ROMN", "PLACE_ROMANIZED_VARIATION")
				.addChild(transformerTo.create("MAP")
					.addChildValue("LATI", "N45")
					.addChildValue("LONG", "W12")
				)
				.addChildReference("NOTE", "N1")
			);
		final GedcomNode note = transformerTo.createWithID("NOTE", "N1");

		Assertions.assertEquals("children: [{tag: PLAC, value: PLACE_NAME, children: [{tag: FORM, value: PLACE_HIERARCHY}, {tag: FONE, value: PLACE_PHONETIC_VARIATION}, {tag: ROMN, value: PLACE_ROMANIZED_VARIATION}, {tag: MAP, children: [{tag: LATI, value: N45}, {tag: LONG, value: W12}]}, {tag: NOTE, ref: N1}]}]", parent.toString());
		Assertions.assertEquals("id: N1, tag: NOTE", note.toString());

		final GedcomNode destinationNode = transformerTo.createEmpty();
		final Gedcom origin = new Gedcom();
		origin.addNote(note);
		final Flef destination = new Flef();
		transformerTo.placeAddressStructureTo(parent, destinationNode, origin, destination);

		Assertions.assertEquals("children: [{tag: PLACE, ref: P1}]", destinationNode.toString());
		Assertions.assertEquals("id: P1, tag: PLACE, children: [{tag: NAME, value: PLACE_NAME}, {tag: MAP, children: [{tag: LATITUDE, value: 45}, {tag: LONGITUDE, value: -12}]}]", destination.getPlaces().get(0).toString());
	}

	@Test
	void contactStructureTo(){
		final Transformer transformerTo = new Transformer(Protocol.FLEF);
		final GedcomNode parent = transformerTo.createEmpty()
			.addChildValue("PHON", "00123456789")
			.addChildValue("EMAIL", "address@mail.com")
			.addChildValue("FAX", "00987654321")
			.addChildValue("WWW", "http://www.webpage.com");

		Assertions.assertEquals("children: [{tag: PHON, value: 00123456789}, {tag: EMAIL, value: address@mail.com}, {tag: FAX, value: 00987654321}, {tag: WWW, value: http://www.webpage.com}]", parent.toString());

		final GedcomNode destinationNode = transformerTo.createEmpty();
		transformerTo.contactStructureTo(parent, destinationNode);

		Assertions.assertEquals("children: [{tag: CONTACT, value: 00123456789}, {tag: CONTACT, value: address@mail.com}, {tag: CONTACT, value: 00987654321, children: [{tag: TYPE, value: fax}]}, {tag: CONTACT, value: http://www.webpage.com}]", destinationNode.toString());
	}

	@Test
	void addressStructureFrom(){
		final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);
		final GedcomNode parent = transformerFrom.createEmpty()
			.addChild(transformerFrom.createWithReference("PLACE", "P1"));

		Assertions.assertEquals("children: [{tag: PLACE, ref: P1}]", parent.toString());

		final GedcomNode destinationNode = transformerFrom.createEmpty();
		final Flef origin = new Flef();
		origin.addPlace(transformerFrom.createWithID("PLACE", "P1")
			.addChild(transformerFrom.createWithValue("ADDRESS", "ADDRESS_CITY, ADDRESS_STATE, ADDRESS_COUNTRY")
				.addChildValue("HIERARCHY", "City, State, Country")
			)
			.addChildReference("NOTE", "N1")
		);
		origin.addNote(transformerFrom.createWithIDValue("NOTE", "N1", "SUBMITTER_TEXT"));
		transformerFrom.addressStructureFrom(parent, destinationNode, origin);

		Assertions.assertEquals("children: [{tag: ADDR, value: ADDRESS_CITY, ADDRESS_STATE, ADDRESS_COUNTRY}]", destinationNode.toString());
		Assertions.assertEquals("id: P1, tag: PLACE, children: [{tag: ADDRESS, value: ADDRESS_CITY, ADDRESS_STATE, ADDRESS_COUNTRY, children: [{tag: HIERARCHY, value: City, State, Country}]}, {tag: NOTE, ref: N1}]", origin.getPlaces().get(0).toString());
		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXT", origin.getNotes().get(0).toString());
	}

	@Test
	void placeStructureFrom(){
		final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);
		final GedcomNode parent = transformerFrom.createEmpty()
			.addChild(transformerFrom.createWithReference("PLACE", "P1"));

		Assertions.assertEquals("children: [{tag: PLACE, ref: P1}]", parent.toString());

		final GedcomNode destinationNode = transformerFrom.createEmpty();
		final Flef origin = new Flef();
		origin.addPlace(transformerFrom.createWithID("PLACE", "P1")
			.addChildValue("NAME", "PLACE_NAME")
			.addChild(transformerFrom.create("MAP")
				.addChildValue("LATITUDE", "45")
				.addChildValue("LONGITUDE", "-12")
			)
			.addChildReference("NOTE", "N1")
		);
		origin.addNote(transformerFrom.createWithIDValue("NOTE", "N1", "SUBMITTER_TEXT"));
		transformerFrom.placeStructureFrom(parent, destinationNode, origin);

		Assertions.assertEquals("children: [{tag: PLAC, value: PLACE_NAME, children: [{tag: MAP, children: [{tag: LATI, value: N45}, {tag: LONG, value: W12}]}]}]", destinationNode.toString());
		Assertions.assertEquals("id: P1, tag: PLACE, children: [{tag: NAME, value: PLACE_NAME}, {tag: MAP, children: [{tag: LATITUDE, value: 45}, {tag: LONGITUDE, value: -12}]}, {tag: NOTE, ref: N1}]", origin.getPlaces().get(0).toString());
		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXT", origin.getNotes().get(0).toString());
	}

	@Test
	void contactStructureFrom(){
		final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);
		final GedcomNode parent = transformerFrom.createEmpty()
			.addChild(transformerFrom.createWithValue("CONTACT", "00123456789"))
			.addChild(transformerFrom.createWithValue("CONTACT", "00987654321")
					.addChildValue("TYPE", "fax")
				)
			.addChild(transformerFrom.createWithValue("CONTACT", "address@mail.com"))
			.addChild(transformerFrom.createWithValue("CONTACT", "http://www.webpage.com"));

		Assertions.assertEquals("children: [{tag: CONTACT, value: 00123456789}, {tag: CONTACT, value: 00987654321, children: [{tag: TYPE, value: fax}]}, {tag: CONTACT, value: address@mail.com}, {tag: CONTACT, value: http://www.webpage.com}]", parent.toString());

		final GedcomNode destinationNode = transformerFrom.createEmpty();
		transformerFrom.contactStructureFrom(parent, destinationNode);

		Assertions.assertEquals("children: [{tag: PHON, value: 00123456789}, {tag: EMAIL, value: address@mail.com}, {tag: FAX, value: 00987654321}, {tag: WWW, value: http://www.webpage.com}]", destinationNode.toString());
	}

}