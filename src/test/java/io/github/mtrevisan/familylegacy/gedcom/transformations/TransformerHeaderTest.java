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


class TransformerHeaderTest{

	@Test
	void headerTo(){
		final Transformer transformerTo = new Transformer(Protocol.FLEF);
		final GedcomNode header = transformerTo.create("HEAD")
			.addChild(transformerTo.createWithValue("SOUR", "APPROVED_SYSTEM_ID")
				.addChildValue("VERS", "SOURCE_VERSION_NUMBER")
				.addChildValue("NAME", "NAME_OF_PRODUCT")
				.addChild(transformerTo.createWithValue("CORP", "NAME_OF_BUSINESS")
					.addChild(transformerTo.createWithValue("ADDR", "ADDRESS_LINE")
						.addChildValue("CONT", "ADDRESS_LINE")
						.addChildValue("ADR1", "ADDRESS_LINE1")
						.addChildValue("ADR2", "ADDRESS_LINE2")
						.addChildValue("ADR3", "ADDRESS_LINE3")
						.addChildValue("CITY", "ADDRESS_CITY")
						.addChildValue("STAE", "ADDRESS_STATE")
						.addChildValue("POST", "ADDRESS_POSTAL_CODE")
						.addChildValue("CTRY", "ADDRESS_COUNTRY")
					)
					.addChildValue("PHON", "00123456789")
					.addChildValue("PHON", "00123456780")
					.addChildValue("EMAIL", "address1@mail.com")
					.addChildValue("EMAIL", "address2@mail.com")
					.addChildValue("FAX", "00987654321")
					.addChildValue("FAX", "00987654322")
					.addChildValue("WWW", "http://www.webpage1.com")
					.addChildValue("WWW", "http://www.webpage2.com")
				)
				.addChild(transformerTo.createWithValue("DATA", "NAME_OF_SOURCE_DATA")
					.addChildValue("DATE", "PUBLICATION_DATE")
					.addChild(transformerTo.createWithValue("COPR", "COPYRIGHT_SOURCE_DATA")
						.addChildValue("CONC", "COPYRIGHT_SOURCE_DATA")
					)
				)
			)
			.addChildValue("DEST", "RECEIVING_SYSTEM_NAME")
			.addChild(transformerTo.createWithValue("DATE", "TRANSMISSION_DATE")
				.addChildValue("TIME", "TIME_VALUE")
			)
			.addChildReference("SUBM", "SUBM1")
			.addChildReference("SUBN", "SUBN1")
			.addChildValue("FILE", "FILE_NAME")
			.addChildValue("COPR", "COPYRIGHT_GEDCOM_FILE")
			.addChild(transformerTo.create("GEDC")
				.addChildValue("VERS", "GEDCOM_VERSION_NUMBER")
				.addChildValue("FORM", "LINEAGE-LINKED")
			)
			.addChild(transformerTo.createWithValue("CHAR", "UTF-8")
				.addChildValue("VERS", "CHARSET_VERSION_NUMBER")
			)
			.addChildValue("LANG", "English")
			.addChildValue("LANG", "French")
			.addChild(transformerTo.create("PLAC")
				.addChildValue("FORM", "PLACE_HIERARCHY")
			)
			.addChildReference("NOTE", "N1")
			.addChild(transformerTo.createWithValue("NOTE", "GEDCOM_CONTENT_DESCRIPTION")
				.addChildValue("CONC", "GEDCOM_CONTENT_DESCRIPTION")
			);
		final GedcomNode submitter = transformerTo.createWithID("SUBM", "SUBM1")
			.addChildValue("NAME", "SUBMITTER_NAME")
			.addChild(transformerTo.createWithValue("ADDR", "ADDRESS_LINE")
				.addChildValue("CONT", "ADDRESS_LINE1")
				.addChildValue("ADR1", "ADDRESS_LINE2")
				.addChildValue("CITY", "ADDRESS_CITY")
				.addChildValue("STAE", "ADDRESS_STATE")
				.addChildValue("CTRY", "ADDRESS_COUNTRY")
			)
			.addChildValue("PHON", "00123456789")
			.addChildValue("EMAIL", "address@mail.com")
			.addChildValue("FAX", "00987654321")
			.addChildValue("WWW", "http://www.webpage.com")
			.addChildValue("LANG", "LANGUAGE_PREFERENCE");
		final GedcomNode note = transformerTo.createWithIDValue("NOTE", "N1", "SUBMITTER_TEXT1");

		Assertions.assertEquals("tag: HEAD, children: [{tag: SOUR, value: APPROVED_SYSTEM_ID, children: [{tag: VERS, value: SOURCE_VERSION_NUMBER}, {tag: NAME, value: NAME_OF_PRODUCT}, {tag: CORP, value: NAME_OF_BUSINESS, children: [{tag: ADDR, value: ADDRESS_LINE, children: [{tag: CONT, value: ADDRESS_LINE}, {tag: ADR1, value: ADDRESS_LINE1}, {tag: ADR2, value: ADDRESS_LINE2}, {tag: ADR3, value: ADDRESS_LINE3}, {tag: CITY, value: ADDRESS_CITY}, {tag: STAE, value: ADDRESS_STATE}, {tag: POST, value: ADDRESS_POSTAL_CODE}, {tag: CTRY, value: ADDRESS_COUNTRY}]}, {tag: PHON, value: 00123456789}, {tag: PHON, value: 00123456780}, {tag: EMAIL, value: address1@mail.com}, {tag: EMAIL, value: address2@mail.com}, {tag: FAX, value: 00987654321}, {tag: FAX, value: 00987654322}, {tag: WWW, value: http://www.webpage1.com}, {tag: WWW, value: http://www.webpage2.com}]}, {tag: DATA, value: NAME_OF_SOURCE_DATA, children: [{tag: DATE, value: PUBLICATION_DATE}, {tag: COPR, value: COPYRIGHT_SOURCE_DATA, children: [{tag: CONC, value: COPYRIGHT_SOURCE_DATA}]}]}]}, {tag: DEST, value: RECEIVING_SYSTEM_NAME}, {tag: DATE, value: TRANSMISSION_DATE, children: [{tag: TIME, value: TIME_VALUE}]}, {tag: SUBM, ref: SUBM1}, {tag: SUBN, ref: SUBN1}, {tag: FILE, value: FILE_NAME}, {tag: COPR, value: COPYRIGHT_GEDCOM_FILE}, {tag: GEDC, children: [{tag: VERS, value: GEDCOM_VERSION_NUMBER}, {tag: FORM, value: LINEAGE-LINKED}]}, {tag: CHAR, value: UTF-8, children: [{tag: VERS, value: CHARSET_VERSION_NUMBER}]}, {tag: LANG, value: English}, {tag: LANG, value: French}, {tag: PLAC, children: [{tag: FORM, value: PLACE_HIERARCHY}]}, {tag: NOTE, ref: N1}, {tag: NOTE, value: GEDCOM_CONTENT_DESCRIPTION, children: [{tag: CONC, value: GEDCOM_CONTENT_DESCRIPTION}]}]", header.toString());
		Assertions.assertEquals("id: SUBM1, tag: SUBM, children: [{tag: NAME, value: SUBMITTER_NAME}, {tag: ADDR, value: ADDRESS_LINE, children: [{tag: CONT, value: ADDRESS_LINE1}, {tag: ADR1, value: ADDRESS_LINE2}, {tag: CITY, value: ADDRESS_CITY}, {tag: STAE, value: ADDRESS_STATE}, {tag: CTRY, value: ADDRESS_COUNTRY}]}, {tag: PHON, value: 00123456789}, {tag: EMAIL, value: address@mail.com}, {tag: FAX, value: 00987654321}, {tag: WWW, value: http://www.webpage.com}, {tag: LANG, value: LANGUAGE_PREFERENCE}]", submitter.toString());

		final Gedcom origin = new Gedcom();
		origin.addSubmitter(submitter);
		origin.addNote(note);
		final Flef destination = new Flef();
		transformerTo.headerTo(header, origin, destination);

		Assertions.assertEquals("tag: HEADER, children: [{tag: PROTOCOL, value: FLEF, children: [{tag: NAME, value: Family LEgacy Format}, {tag: VERSION, value: 0.0.6}]}, {tag: SOURCE, value: APPROVED_SYSTEM_ID, children: [{tag: NAME, value: NAME_OF_PRODUCT}, {tag: VERSION, value: SOURCE_VERSION_NUMBER}, {tag: CORPORATE, value: NAME_OF_BUSINESS}]}, {tag: DATE, value: TRANSMISSION_DATE TIME_VALUE}, {tag: COPYRIGHT, value: COPYRIGHT_GEDCOM_FILE}, {tag: SUBMITTER, children: [{tag: NAME, value: SUBMITTER_NAME}, {tag: PLACE, children: [{tag: ADDRESS, value: ADDRESS_LINE, ADDRESS_LINE1, ADDRESS_LINE2}]}, {tag: CONTACT, value: 00123456789}, {tag: CONTACT, value: address@mail.com}, {tag: CONTACT, value: 00987654321, children: [{tag: TYPE, value: fax}]}, {tag: CONTACT, value: http://www.webpage.com}]}, {tag: NOTE, value: SUBMITTER_TEXT1}, {tag: NOTE, value: GEDCOM_CONTENT_DESCRIPTION}]", destination.getHeader().toString());
	}

	@Test
	void headerFrom(){
		final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);
		final GedcomNode header = transformerFrom.create("HEADER")
			.addChild(transformerFrom.createWithValue("PROTOCOL", "PROTOCOL_NAME")
				.addChildValue("NAME", "NAME_OF_PROTOCOL")
				.addChildValue("VERSION", "VERSION_NUMBER")
			)
			.addChild(transformerFrom.createWithValue("SOURCE", "APPROVED_SYSTEM_ID")
				.addChildValue("NAME", "NAME_OF_PRODUCT")
				.addChildValue("VERSION", "VERSION_NUMBER")
				.addChildValue("CORPORATE", "NAME_OF_BUSINESS"))
			.addChildValue("DATE", "CREATION_DATE")
			.addChildValue("COPYRIGHT", "COPYRIGHT_SOURCE_DATA")
			.addChild(transformerFrom.create("SUBMITTER")
				.addChildValue("NAME", "SUBMITTER_NAME")
				.addChild(transformerFrom.create("PLACE")
					.addChildValue("ADDRESS", "ADDRESS_LINE")
					.addChildValue("CITY", "ADDRESS_CITY")
					.addChildValue("STATE", "ADDRESS_STATE")
					.addChildValue("COUNTRY", "ADDRESS_COUNTRY")
				)
				.addChild(transformerFrom.createWithValue("CONTACT", "00123456789"))
				.addChild(transformerFrom.createWithValue("CONTACT", "00987654321")
					.addChildValue("TYPE", "fax"))
				.addChild(transformerFrom.createWithValue("CONTACT", "address@mail.com"))
				.addChild(transformerFrom.createWithValue("CONTACT", "http://www.webpage.com"))
			)
			.addChildValue("NOTE", "GEDCOM_CONTENT_DESCRIPTION");

		Assertions.assertEquals("tag: HEADER, children: [{tag: PROTOCOL, value: PROTOCOL_NAME, children: [{tag: NAME, value: NAME_OF_PROTOCOL}, {tag: VERSION, value: VERSION_NUMBER}]}, {tag: SOURCE, value: APPROVED_SYSTEM_ID, children: [{tag: NAME, value: NAME_OF_PRODUCT}, {tag: VERSION, value: VERSION_NUMBER}, {tag: CORPORATE, value: NAME_OF_BUSINESS}]}, {tag: DATE, value: CREATION_DATE}, {tag: COPYRIGHT, value: COPYRIGHT_SOURCE_DATA}, {tag: SUBMITTER, children: [{tag: NAME, value: SUBMITTER_NAME}, {tag: PLACE, children: [{tag: ADDRESS, value: ADDRESS_LINE}, {tag: CITY, value: ADDRESS_CITY}, {tag: STATE, value: ADDRESS_STATE}, {tag: COUNTRY, value: ADDRESS_COUNTRY}]}, {tag: CONTACT, value: 00123456789}, {tag: CONTACT, value: 00987654321, children: [{tag: TYPE, value: fax}]}, {tag: CONTACT, value: address@mail.com}, {tag: CONTACT, value: http://www.webpage.com}]}, {tag: NOTE, value: GEDCOM_CONTENT_DESCRIPTION}]", header.toString());

		final Gedcom destination = new Gedcom();
		transformerFrom.headerFrom(header, destination);

		Assertions.assertEquals("tag: HEAD, children: [{tag: SOUR, value: APPROVED_SYSTEM_ID, children: [{tag: NAME, value: NAME_OF_PRODUCT}, {tag: VERS, value: VERSION_NUMBER}, {tag: CORP, value: NAME_OF_BUSINESS}]}, {tag: DATE, value: CREATION_DATE}, {tag: SUBM, ref: SUBM1}, {tag: GEDC, children: [{tag: VERS, value: 5.5.1}, {tag: FORM, value: LINEAGE-LINKED}]}, {tag: CHAR, value: UTF-8}]", destination.getHeader().toString());
		Assertions.assertEquals("id: SUBM1, tag: SUBM, children: [{tag: NAME, value: SUBMITTER_NAME}, {tag: ADDR, value: ADDRESS_LINE}, {tag: PHON, value: 00123456789}, {tag: EMAIL, value: address@mail.com}, {tag: FAX, value: 00987654321}, {tag: WWW, value: http://www.webpage.com}]", destination.getSubmitters().get(0).toString());
	}

}