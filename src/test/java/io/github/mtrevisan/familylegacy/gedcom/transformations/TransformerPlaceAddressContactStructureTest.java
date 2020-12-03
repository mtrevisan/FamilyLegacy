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


class TransformerPlaceAddressContactStructureTest{

	private final Transformer transformerTo = new Transformer(Protocol.FLEF);
	private final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);


	@Test
	void addressStructureTo(){
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
		final Flef destination = new Flef();
		transformerTo.placeAddressStructureTo(parent, destinationNode, destination);

		Assertions.assertEquals("children: [{tag: PLACE, ref: P1}]", destinationNode.toString());
		Assertions.assertEquals("id: P1, tag: PLACE, children: [{tag: ADDRESS, value: ADDRESS_LINE0 - ADDRESS_LINE1 - ADDRESS_LINE2 - ADDRESS_LINE3}, {tag: CITY, value: ADDRESS_CITY}, {tag: STATE, value: ADDRESS_STATE}, {tag: COUNTRY, value: ADDRESS_COUNTRY}]", destination.getPlaces().get(0).toString());
	}

	@Test
	void contactStructureTo(){
		final GedcomNode parent = transformerTo.createEmpty()
			.addChildValue("PHON", "PHONE_NUMBER")
			.addChildValue("EMAIL", "ADDRESS_EMAIL")
			.addChildValue("FAX", "ADDRESS_FAX")
			.addChildValue("WWW", "ADDRESS_WEB_PAGE");

		Assertions.assertEquals("children: [{tag: PHON, value: PHONE_NUMBER}, {tag: EMAIL, value: ADDRESS_EMAIL}, {tag: FAX, value: ADDRESS_FAX}, {tag: WWW, value: ADDRESS_WEB_PAGE}]", parent.toString());

		final GedcomNode destinationNode = transformerTo.createEmpty();
		transformerTo.contactStructureTo(parent, destinationNode);

		Assertions.assertEquals("children: [{tag: CONTACT, children: [{tag: PHONE, value: PHONE_NUMBER}, {tag: EMAIL, value: ADDRESS_EMAIL}, {tag: PHONE, value: ADDRESS_FAX, children: [{tag: TYPE, value: fax}]}, {tag: URL, value: ADDRESS_WEB_PAGE}]}]", destinationNode.toString());
	}

//	@Test
//	void placeStructureTo(){
//		final GedcomNode parent = transformerTo.createEmpty()
//			.addChild(transformerTo.createWithReference("REPO", "@R1@")
//				.addChildReference("NOTE", "@N1@")
//				.addChild(transformerTo.create("CALN")
//					.withValue("SOURCE_CALL_NUMBER")
//					.addChildValue("MEDI", "SOURCE_MEDIA_TYPE")
//				)
//			);
//		final GedcomNode repository = transformerTo.createWithID("REPO", "@R1@");
//		final GedcomNode note = transformerFrom.createWithID("NOTE", "@N1@");
//
//		Assertions.assertEquals("children: [{tag: REPO, ref: @R1@, children: [{tag: NOTE, ref: @N1@}, {tag: CALN, value: SOURCE_CALL_NUMBER, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]", parent.toString());
//		Assertions.assertEquals("id: @R1@, tag: REPO", repository.toString());
//		Assertions.assertEquals("id: @N1@, tag: NOTE", note.toString());
//
//		final GedcomNode destinationNode = transformerTo.createEmpty();
//		final Flef destination = new Flef();
//		destination.addRepository(repository);
//		destination.addNote(note);
//		t.placeAddressStructureTo(parent, destinationNode, destination);
//
//		Assertions.assertEquals("children: [{tag: REPOSITORY, ref: @R1@, children: [{tag: LOCATION, value: SOURCE_CALL_NUMBER}, {tag: NOTE, ref: @N1@}]}]", destinationNode.toString());
//	}
//
//	@Test
//	void placeAddressStructureTo(){
//		final GedcomNode parent = transformerTo.createEmpty()
//			.addChild(transformerTo.createWithReference("REPO", "@R1@")
//				.addChildReference("NOTE", "@N1@")
//				.addChild(transformerTo.create("CALN")
//					.withValue("SOURCE_CALL_NUMBER")
//					.addChildValue("MEDI", "SOURCE_MEDIA_TYPE")
//				)
//			);
//		final GedcomNode repository = transformerTo.createWithID("REPO", "@R1@");
//		final GedcomNode note = transformerFrom.createWithID("NOTE", "@N1@");
//
//		Assertions.assertEquals("children: [{tag: REPO, ref: @R1@, children: [{tag: NOTE, ref: @N1@}, {tag: CALN, value: SOURCE_CALL_NUMBER, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]", parent.toString());
//		Assertions.assertEquals("id: @R1@, tag: REPO", repository.toString());
//		Assertions.assertEquals("id: @N1@, tag: NOTE", note.toString());
//
//		final GedcomNode destinationNode = transformerTo.createEmpty();
//		final Flef destination = new Flef();
//		destination.addRepository(repository);
//		destination.addNote(note);
//		t.placeAddressStructureTo(parent, destinationNode, destination);
//
//		Assertions.assertEquals("children: [{tag: REPOSITORY, ref: @R1@, children: [{tag: LOCATION, value: SOURCE_CALL_NUMBER}, {tag: NOTE, ref: @N1@}]}]", destinationNode.toString());
//	}

	@Test
	void addressStructureFrom(){
		final GedcomNode parent = transformerFrom.createEmpty()
			.addChild(transformerFrom.createWithReference("PLACE", "@P1@"));

		Assertions.assertEquals("children: [{tag: PLACE, ref: @P1@}]", parent.toString());

		final GedcomNode destinationNode = transformerFrom.createEmpty();
		final Flef origin = new Flef();
		origin.addPlace(transformerTo.createWithID("PLACE", "@P1@")
			.addChild(transformerTo.create("ADDRESS")
				.withValue("ADDRESS_LINE")
				.addChildValue("CITY", "ADDRESS_CITY")
				.addChildValue("STATE", "ADDRESS_STATE")
				.addChildValue("COUNTRY", "ADDRESS_COUNTRY")
			)
			.addChildReference("NOTE", "N1")
		);
		origin.addNote(transformerTo.createWithIDValue("NOTE", "N1", "SUBMITTER_TEXT"));
		transformerFrom.addressStructureFrom(parent, destinationNode, origin);

		Assertions.assertEquals("children: [{tag: ADDR, value: ADDRESS_LINE, children: [{tag: CITY, value: ADDRESS_CITY}, {tag: STAE, value: ADDRESS_STATE}, {tag: CTRY, value: ADDRESS_COUNTRY}]}]", destinationNode.toString());
		Assertions.assertEquals("id: @P1@, tag: PLACE, children: [{tag: ADDRESS, value: ADDRESS_LINE, children: [{tag: CITY, value: ADDRESS_CITY}, {tag: STATE, value: ADDRESS_STATE}, {tag: COUNTRY, value: ADDRESS_COUNTRY}]}, {tag: NOTE, ref: N1}]", origin.getPlaces().get(0).toString());
		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXT", origin.getNotes().get(0).toString());
	}

	@Test
	void contactStructureFrom(){
		final GedcomNode parent = transformerFrom.createEmpty()
			.addChild(transformerFrom.create("CONTACT")
				.addChildValue("PHONE", "PHONE_NUMBER")
				.addChild(transformerFrom.create("PHONE")
					.withValue("FAX_NUMBER")
					.addChildValue("TYPE", "fax")
				)
				.addChildValue("EMAIL", "EMAIL_ADDRESS")
				.addChildValue("URL", "URL_ADDRESS")
			);

		Assertions.assertEquals("children: [{tag: CONTACT, children: [{tag: PHONE, value: PHONE_NUMBER}, {tag: PHONE, value: FAX_NUMBER, children: [{tag: TYPE, value: fax}]}, {tag: EMAIL, value: EMAIL_ADDRESS}, {tag: URL, value: URL_ADDRESS}]}]", parent.toString());

		final GedcomNode destinationNode = transformerFrom.createEmpty();
		transformerFrom.contactStructureFrom(parent, destinationNode);

		Assertions.assertEquals("children: [{tag: PHONE, value: PHONE_NUMBER}, {tag: EMAIL, value: EMAIL_ADDRESS}, {tag: FAX, value: FAX_NUMBER}, {tag: WWW, value: URL_ADDRESS}]", destinationNode.toString());
	}

}