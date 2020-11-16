package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.Protocol;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class HeaderTransformationTest{

	private final Transformer transformerTo = new Transformer(Protocol.FLEF);
	private final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);


	@Test
	void to(){
		final GedcomNode header = transformerTo.create("HEAD")
			.addChild(transformerTo.create("SOUR")
				.withValue("APPROVED_SYSTEM_ID")
				.addChildValue("VERS", "SOURCE_VERSION_NUMBER")
				.addChildValue("NAME", "NAME_OF_PRODUCT")
				.addChild(transformerTo.create("CORP")
					.withValue("NAME_OF_BUSINESS")
					.addChild(transformerTo.create("ADDR")
						.withValue("ADDRESS_LINE")
						.addChildValue("CONT", "ADDRESS_LINE")
						.addChildValue("ADR1", "ADDRESS_LINE1")
						.addChildValue("ADR2", "ADDRESS_LINE2")
						.addChildValue("ADR3", "ADDRESS_LINE3")
						.addChildValue("CITY", "ADDRESS_CITY")
						.addChildValue("STAE", "ADDRESS_STATE")
						.addChildValue("POST", "ADDRESS_POSTAL_CODE")
						.addChildValue("CTRY", "ADDRESS_COUNTRY")
					)
					.addChildValue("PHON", "PHONE_NUMBER1")
					.addChildValue("PHON", "PHONE_NUMBER2")
					.addChildValue("EMAIL", "ADDRESS_EMAIL1")
					.addChildValue("EMAIL", "ADDRESS_EMAIL2")
					.addChildValue("FAX", "ADDRESS_FAX1")
					.addChildValue("FAX", "ADDRESS_FAX2")
					.addChildValue("WWW", "ADDRESS_WEB_PAGE1")
					.addChildValue("WWW", "ADDRESS_WEB_PAGE2")
				)
				.addChild(transformerTo.create("DATA")
					.withValue("NAME_OF_SOURCE_DATA")
					.addChildValue("DATE", "PUBLICATION_DATE")
					.addChild(transformerTo.create("COPR")
						.withValue("COPYRIGHT_SOURCE_DATA")
						.addChildValue("CONC", "COPYRIGHT_SOURCE_DATA")
					)
				)
			)
			.addChildValue("DEST", "RECEIVING_SYSTEM_NAME")
			.addChild(transformerTo.create("DATE")
				.withValue("TRANSMISSION_DATE")
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
			.addChild(transformerTo.create("CHAR")
				.withValue("UTF-8")
				.addChildValue("VERS", "CHARSET_VERSION_NUMBER")
			)
			.addChildValue("LANG", "LANGUAGE_OF_TEXT_1")
			.addChildValue("LANG", "LANGUAGE_OF_TEXT_2")
			.addChild(transformerTo.create("PLAC")
				.addChildValue("FORM", "PLACE_HIERARCHY")
			)
			.addChild(transformerTo.create("NOTE")
				.withValue("GEDCOM_CONTENT_DESCRIPTION")
				.addChildValue("CONC", "GEDCOM_CONTENT_DESCRIPTION")
			);
		final Gedcom origin = new Gedcom();
		origin.setHeader(header);
		final Flef destination = new Flef();

		Assertions.assertEquals("tag: HEAD, children: [{tag: SOUR, value: APPROVED_SYSTEM_ID, children: [{tag: VERS, value: SOURCE_VERSION_NUMBER}, {tag: NAME, value: NAME_OF_PRODUCT}, {tag: CORP, value: NAME_OF_BUSINESS, children: [{tag: ADDR, value: ADDRESS_LINE, children: [{tag: CONT, value: ADDRESS_LINE}, {tag: ADR1, value: ADDRESS_LINE1}, {tag: ADR2, value: ADDRESS_LINE2}, {tag: ADR3, value: ADDRESS_LINE3}, {tag: CITY, value: ADDRESS_CITY}, {tag: STAE, value: ADDRESS_STATE}, {tag: POST, value: ADDRESS_POSTAL_CODE}, {tag: CTRY, value: ADDRESS_COUNTRY}]}, {tag: PHON, value: PHONE_NUMBER1}, {tag: PHON, value: PHONE_NUMBER2}, {tag: EMAIL, value: ADDRESS_EMAIL1}, {tag: EMAIL, value: ADDRESS_EMAIL2}, {tag: FAX, value: ADDRESS_FAX1}, {tag: FAX, value: ADDRESS_FAX2}, {tag: WWW, value: ADDRESS_WEB_PAGE1}, {tag: WWW, value: ADDRESS_WEB_PAGE2}]}, {tag: DATA, value: NAME_OF_SOURCE_DATA, children: [{tag: DATE, value: PUBLICATION_DATE}, {tag: COPR, value: COPYRIGHT_SOURCE_DATA, children: [{tag: CONC, value: COPYRIGHT_SOURCE_DATA}]}]}]}, {tag: DEST, value: RECEIVING_SYSTEM_NAME}, {tag: DATE, value: TRANSMISSION_DATE, children: [{tag: TIME, value: TIME_VALUE}]}, {id: SUBM1, tag: SUBM}, {id: SUBN1, tag: SUBN}, {tag: FILE, value: FILE_NAME}, {tag: COPR, value: COPYRIGHT_GEDCOM_FILE}, {tag: GEDC, children: [{tag: VERS, value: GEDCOM_VERSION_NUMBER}, {tag: FORM, value: LINEAGE-LINKED}]}, {tag: CHAR, value: UTF-8, children: [{tag: VERS, value: CHARSET_VERSION_NUMBER}]}, {tag: LANG, value: LANGUAGE_OF_TEXT_1}, {tag: LANG, value: LANGUAGE_OF_TEXT_2}, {tag: PLAC, children: [{tag: FORM, value: PLACE_HIERARCHY}]}, {tag: NOTE, value: GEDCOM_CONTENT_DESCRIPTION, children: [{tag: CONC, value: GEDCOM_CONTENT_DESCRIPTION}]}]", origin.getHeader().toString());

		final Transformation<Gedcom, Flef> t = new HeaderTransformation();
		t.to(origin, destination);

		Assertions.assertEquals("tag: HEADER, children: [{tag: PROTOCOL, value: FLEF, children: [{tag: NAME, value: Family LEgacy Format}, {tag: VERSION, value: 0.0.2}]}, {tag: SOURCE, value: APPROVED_SYSTEM_ID, children: [{tag: NAME, value: NAME_OF_PRODUCT}, {tag: VERSION, value: SOURCE_VERSION_NUMBER}, {tag: CORPORATE, value: NAME_OF_BUSINESS}]}, {tag: DATE, value: TRANSMISSION_DATE TIME_VALUE}, {tag: DEFAULT_CALENDAR, value: GREGORIAN}, {tag: DEFAULT_LOCALE, value: en-US}]", destination.getHeader().toString());
	}


	@Test
	void from() throws GedcomGrammarParseException{
		final GedcomNode header = transformerFrom.create("HEADER")
			.addChild(transformerFrom.create("SOURCE")
				.withValue("APPROVED_SYSTEM_ID")
				.addChildValue("NAME", "NAME_OF_PRODUCT")
				.addChildValue("VERSION", "VERSION_NUMBER")
				.addChildValue("CORPORATE", "NAME_OF_BUSINESS"))
			.addChild(transformerFrom.create("PROTOCOL")
				.withValue("PROTOCOL_NAME")
				.addChildValue("NAME", "NAME_OF_PROTOCOL")
				.addChildValue("VERSION", "VERSION_NUMBER")
			)
			.addChildValue("DATE", "CREATION_DATE")
			.addChildValue("DEFAULT_CALENDAR", "CALENDAR_TYPE")
			.addChildValue("DEFAULT_LOCALE", "en-US")
			.addChildValue("COPYRIGHT", "COPYRIGHT_SOURCE_DATA")
			.addChildReference("SUBMITTER", "SUBM1")
			.addChildValue("NOTE", "GEDCOM_CONTENT_DESCRIPTION");
		final Flef origin = new Flef();
		origin.setHeader(header);
		final Gedcom destination = new Gedcom();

		Assertions.assertEquals("tag: HEADER, children: [{tag: SOURCE, value: APPROVED_SYSTEM_ID, children: [{tag: NAME, value: NAME_OF_PRODUCT}, {tag: VERSION, value: VERSION_NUMBER}, {tag: CORPORATE, value: NAME_OF_BUSINESS}]}, {tag: PROTOCOL, value: PROTOCOL_NAME, children: [{tag: NAME, value: NAME_OF_PROTOCOL}, {tag: VERSION, value: VERSION_NUMBER}]}, {tag: DATE, value: CREATION_DATE}, {tag: DEFAULT_CALENDAR, value: CALENDAR_TYPE}, {tag: DEFAULT_LOCALE, value: en-US}, {tag: COPYRIGHT, value: COPYRIGHT_SOURCE_DATA}, {id: SUBM1, tag: SUBMITTER}, {tag: NOTE, value: GEDCOM_CONTENT_DESCRIPTION}]", origin.getHeader().toString());

		final Transformation<Gedcom, Flef> t = new HeaderTransformation();
		t.from(origin, destination);

		Assertions.assertEquals("tag: HEAD, children: [{tag: SOUR, value: APPROVED_SYSTEM_ID, children: [{tag: VERS, value: VERSION_NUMBER}, {tag: NAME, value: NAME_OF_PRODUCT}, {tag: CORP, value: NAME_OF_BUSINESS}]}, {tag: DATE, value: CREATION_DATE}, {tag: GEDC, children: [{tag: VERS, value: 5.5.1}, {tag: FORM, value: LINEAGE-LINKED}]}, {tag: CHAR, value: UTF-8}, {tag: LANG, value: English}]", destination.getHeader().toString());
	}

}