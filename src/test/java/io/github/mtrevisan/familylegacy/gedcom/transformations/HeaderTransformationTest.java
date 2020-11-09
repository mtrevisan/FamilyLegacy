package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class HeaderTransformationTest{

	@Test
	void to(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("HEAD")
				.addChild(GedcomNode.create("SOUR")
					.withValue("APPROVED_SYSTEM_ID")
					.addChild(GedcomNode.create("VERS")
						.withValue("VERSION_NUMBER"))
					.addChild(GedcomNode.create("NAME")
						.withValue("NAME_OF_PRODUCT"))
					.addChild(GedcomNode.create("CORP")
						.withValue("NAME_OF_BUSINESS")
						.addChild(GedcomNode.create("ADDR")
							.withValue("ADDRESS_LINE")
							.addChild(GedcomNode.create("CONT")
								.withValue("ADDRESS_LINE"))
							.addChild(GedcomNode.create("ADR1")
								.withValue("ADDRESS_LINE1"))
							.addChild(GedcomNode.create("ADR2")
								.withValue("ADDRESS_LINE2"))
							.addChild(GedcomNode.create("ADR3")
								.withValue("ADDRESS_LINE3"))
							.addChild(GedcomNode.create("CITY")
								.withValue("ADDRESS_CITY"))
							.addChild(GedcomNode.create("STAE")
								.withValue("ADDRESS_STATE"))
							.addChild(GedcomNode.create("POST")
								.withValue("ADDRESS_POSTAL_CODE"))
							.addChild(GedcomNode.create("CTRY")
								.withValue("ADDRESS_COUNTRY")))
						.addChild(GedcomNode.create("PHON")
							.withValue("PHONE_NUMBER1"))
						.addChild(GedcomNode.create("PHON")
							.withValue("PHONE_NUMBER2"))
						.addChild(GedcomNode.create("EMAIL")
							.withValue("ADDRESS_EMAIL1"))
						.addChild(GedcomNode.create("EMAIL")
							.withValue("ADDRESS_EMAIL2"))
						.addChild(GedcomNode.create("FAX")
							.withValue("ADDRESS_FAX1"))
						.addChild(GedcomNode.create("FAX")
							.withValue("ADDRESS_FAX2"))
						.addChild(GedcomNode.create("WWW")
							.withValue("ADDRESS_WEB_PAGE1"))
						.addChild(GedcomNode.create("WWW")
							.withValue("ADDRESS_WEB_PAGE2")))
					.addChild(GedcomNode.create("DATA")
						.withValue("NAME_OF_SOURCE_DATA")
						.addChild(GedcomNode.create("DATE")
							.withValue("PUBLICATION_DATE"))
						.addChild(GedcomNode.create("COPR")
							.withValue("COPYRIGHT_SOURCE_DATA")
							.addChild(GedcomNode.create("CONC")
								.withValue("COPYRIGHT_SOURCE_DATA")))))
				.addChild(GedcomNode.create("DEST")
					.withValue("RECEIVING_SYSTEM_NAME"))
				.addChild(GedcomNode.create("DATE")
					.withValue("TRANSMISSION_DATE")
					.addChild(GedcomNode.create("TIME")
						.withValue("TIME_VALUE")))
				.addChild(GedcomNode.create("SUBM")
					.withID("SUBM1"))
				.addChild(GedcomNode.create("SUBN")
					.withID("SUBN1"))
				.addChild(GedcomNode.create("FILE")
					.withValue("FILE_NAME"))
				.addChild(GedcomNode.create("COPR")
					.withValue("COPYRIGHT_GEDCOM_FILE"))
				.addChild(GedcomNode.create("GEDC")
					.addChild(GedcomNode.create("VERS")
						.withValue("VERSION_NUMBER"))
					.addChild(GedcomNode.create("FORM")
						.withValue("LINEAGE-LINKED")))
				.addChild(GedcomNode.create("CHAR")
					.withValue("UTF-8")
					.addChild(GedcomNode.create("VERS")
						.withValue("VERSION_NUMBER")))
				.addChild(GedcomNode.create("LANG")
					.withValue("LANGUAGE_OF_TEXT_1"))
				.addChild(GedcomNode.create("LANG")
					.withValue("LANGUAGE_OF_TEXT_2"))
				.addChild(GedcomNode.create("PLAC")
					.addChild(GedcomNode.create("FORM")
						.withValue("PLACE_HIERARCHY")))
				.addChild(GedcomNode.create("NOTE")
					.withValue("GEDCOM_CONTENT_DESCRIPTION")
					.addChild(GedcomNode.create("CONC")
						.withValue("GEDCOM_CONTENT_DESCRIPTION")))
		);

		Assertions.assertEquals("children: [{tag: HEAD, children: [{tag: SOUR, value: APPROVED_SYSTEM_ID, children: [{tag: VERS, value: VERSION_NUMBER}, {tag: NAME, value: NAME_OF_PRODUCT}, {tag: CORP, value: NAME_OF_BUSINESS, children: [{tag: ADDR, value: ADDRESS_LINE, children: [{tag: CONT, value: ADDRESS_LINE}, {tag: ADR1, value: ADDRESS_LINE1}, {tag: ADR2, value: ADDRESS_LINE2}, {tag: ADR3, value: ADDRESS_LINE3}, {tag: CITY, value: ADDRESS_CITY}, {tag: STAE, value: ADDRESS_STATE}, {tag: POST, value: ADDRESS_POSTAL_CODE}, {tag: CTRY, value: ADDRESS_COUNTRY}]}, {tag: PHON, value: PHONE_NUMBER1}, {tag: PHON, value: PHONE_NUMBER2}, {tag: EMAIL, value: ADDRESS_EMAIL1}, {tag: EMAIL, value: ADDRESS_EMAIL2}, {tag: FAX, value: ADDRESS_FAX1}, {tag: FAX, value: ADDRESS_FAX2}, {tag: WWW, value: ADDRESS_WEB_PAGE1}, {tag: WWW, value: ADDRESS_WEB_PAGE2}]}, {tag: DATA, value: NAME_OF_SOURCE_DATA, children: [{tag: DATE, value: PUBLICATION_DATE}, {tag: COPR, value: COPYRIGHT_SOURCE_DATA, children: [{tag: CONC, value: COPYRIGHT_SOURCE_DATA}]}]}]}, {tag: DEST, value: RECEIVING_SYSTEM_NAME}, {tag: DATE, value: TRANSMISSION_DATE, children: [{tag: TIME, value: TIME_VALUE}]}, {id: SUBM1, tag: SUBM}, {id: SUBN1, tag: SUBN}, {tag: FILE, value: FILE_NAME}, {tag: COPR, value: COPYRIGHT_GEDCOM_FILE}, {tag: GEDC, children: [{tag: VERS, value: VERSION_NUMBER}, {tag: FORM, value: LINEAGE-LINKED}]}, {tag: CHAR, value: UTF-8, children: [{tag: VERS, value: VERSION_NUMBER}]}, {tag: LANG, value: LANGUAGE_OF_TEXT_1}, {tag: LANG, value: LANGUAGE_OF_TEXT_2}, {tag: PLAC, children: [{tag: FORM, value: PLACE_HIERARCHY}]}, {tag: NOTE, value: GEDCOM_CONTENT_DESCRIPTION, children: [{tag: CONC, value: GEDCOM_CONTENT_DESCRIPTION}]}]}]", root.toString());

		final Transformation t = new HeaderTransformation();
		t.to(extractSubStructure(root, "HEAD"), root);

		Assertions.assertEquals("children: [{tag: HEADER, children: [{tag: SOURCE, value: APPROVED_SYSTEM_ID, children: [{tag: VERSION, value: VERSION_NUMBER}, {tag: NAME, value: NAME_OF_PRODUCT}, {tag: CORPORATE, value: NAME_OF_BUSINESS, children: [{id: P1, tag: PLACE}]}]}, {id: SUBM1, tag: SUBMITTER}, {tag: COPYRIGHT, value: COPYRIGHT_GEDCOM_FILE}, {tag: CHARSET, value: UTF-8}, {id: N1, tag: NOTE}, {tag: PROTOCOL_VERSION, value: 0.0.1}]}, {id: P1, tag: PLACE, children: [{tag: STREET, value: ADDRESS_LINE - ADDRESS_LINE1 - ADDRESS_LINE2 - ADDRESS_LINE3}, {tag: CITY, value: ADDRESS_CITY}, {tag: STATE, value: ADDRESS_STATE}, {tag: POSTAL_CODE, value: ADDRESS_POSTAL_CODE}, {tag: COUNTRY, value: ADDRESS_COUNTRY}, {tag: PHONE, value: PHONE_NUMBER1}, {tag: PHONE, value: PHONE_NUMBER2}, {tag: FAX, value: ADDRESS_FAX1}, {tag: FAX, value: ADDRESS_FAX2}, {tag: EMAIL, value: ADDRESS_EMAIL1}, {tag: EMAIL, value: ADDRESS_EMAIL2}, {tag: WWW, value: ADDRESS_WEB_PAGE1}, {tag: WWW, value: ADDRESS_WEB_PAGE2}]}, {id: N1, tag: NOTE, value: GEDCOM_CONTENT_DESCRIPTIONGEDCOM_CONTENT_DESCRIPTION}]", root.toString());
	}


	@Test
	void from(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("NOTE")
				.withID("N1")
				.withValue("GEDCOM_CONTENT_DESCRIPTION"))
			.addChild(GedcomNode.create("PLACE")
				.withID("P1")
				.addChild(GedcomNode.create("STREET")
					.withValue("ADDRESS_LINE"))
				.addChild(GedcomNode.create("CITY")
					.withValue("ADDRESS_CITY"))
				.addChild(GedcomNode.create("STATE")
					.withValue("ADDRESS_STATE"))
				.addChild(GedcomNode.create("POSTAL_CODE")
					.withValue("ADDRESS_POSTAL_CODE"))
				.addChild(GedcomNode.create("COUNTRY")
					.withValue("ADDRESS_COUNTRY"))
				.addChild(GedcomNode.create("PHONE")
					.withValue("PHONE_NUMBER1"))
				.addChild(GedcomNode.create("PHONE")
					.withValue("PHONE_NUMBER2"))
				.addChild(GedcomNode.create("FAX")
					.withValue("ADDRESS_FAX1"))
				.addChild(GedcomNode.create("FAX")
					.withValue("ADDRESS_FAX2"))
				.addChild(GedcomNode.create("EMAIL")
					.withValue("ADDRESS_EMAIL1"))
				.addChild(GedcomNode.create("EMAIL")
					.withValue("ADDRESS_EMAIL2"))
				.addChild(GedcomNode.create("WWW")
					.withValue("ADDRESS_WEB_PAGE1"))
				.addChild(GedcomNode.create("WWW")
					.withValue("ADDRESS_WEB_PAGE2")))
			.addChild(GedcomNode.create("HEADER")
				.addChild(GedcomNode.create("SOURCE")
					.withValue("APPROVED_SYSTEM_ID")
					.addChild(GedcomNode.create("VERSION")
						.withValue("VERSION_NUMBER"))
					.addChild(GedcomNode.create("NAME")
						.withValue("NAME_OF_PRODUCT"))
					.addChild(GedcomNode.create("CORPORATE")
						.withValue("NAME_OF_BUSINESS")
						.addChild(GedcomNode.create("PLACE")
							.withID("P1"))))
				.addChild(GedcomNode.create("SUBMITTER")
					.withID("SUBM1"))
				.addChild(GedcomNode.create("COPYRIGHT")
					.withValue("COPYRIGHT_GEDCOM_FILE"))
				.addChild(GedcomNode.create("PROTOCOL_VERSION")
					.withValue("VERSION_NUMBER"))
				.addChild(GedcomNode.create("CHARSET")
					.withValue("UTF-8"))
				.addChild(GedcomNode.create("NOTE")
					.withID("N1")));

		Assertions.assertEquals("children: [{id: N1, tag: NOTE, value: GEDCOM_CONTENT_DESCRIPTION}, {id: P1, tag: PLACE, children: [{tag: STREET, value: ADDRESS_LINE}, {tag: CITY, value: ADDRESS_CITY}, {tag: STATE, value: ADDRESS_STATE}, {tag: POSTAL_CODE, value: ADDRESS_POSTAL_CODE}, {tag: COUNTRY, value: ADDRESS_COUNTRY}, {tag: PHONE, value: PHONE_NUMBER1}, {tag: PHONE, value: PHONE_NUMBER2}, {tag: FAX, value: ADDRESS_FAX1}, {tag: FAX, value: ADDRESS_FAX2}, {tag: EMAIL, value: ADDRESS_EMAIL1}, {tag: EMAIL, value: ADDRESS_EMAIL2}, {tag: WWW, value: ADDRESS_WEB_PAGE1}, {tag: WWW, value: ADDRESS_WEB_PAGE2}]}, {tag: HEADER, children: [{tag: SOURCE, value: APPROVED_SYSTEM_ID, children: [{tag: VERSION, value: VERSION_NUMBER}, {tag: NAME, value: NAME_OF_PRODUCT}, {tag: CORPORATE, value: NAME_OF_BUSINESS, children: [{id: P1, tag: PLACE}]}]}, {id: SUBM1, tag: SUBMITTER}, {tag: COPYRIGHT, value: COPYRIGHT_GEDCOM_FILE}, {tag: PROTOCOL_VERSION, value: VERSION_NUMBER}, {tag: CHARSET, value: UTF-8}, {id: N1, tag: NOTE}]}]", root.toString());

		final Transformation t = new HeaderTransformation();
		t.from(extractSubStructure(root, "HEADER"), root);

		Assertions.assertEquals("children: [{id: N1, tag: NOTE, value: GEDCOM_CONTENT_DESCRIPTION}, {tag: HEAD, children: [{tag: SOUR, value: APPROVED_SYSTEM_ID, children: [{tag: VERS, value: VERSION_NUMBER}, {tag: NAME, value: NAME_OF_PRODUCT}, {tag: CORP, value: NAME_OF_BUSINESS, children: [{tag: ADDR, value: ADDRESS_LINE, children: [{tag: CITY, value: ADDRESS_CITY}, {tag: STAE, value: ADDRESS_STATE}, {tag: POST, value: ADDRESS_POSTAL_CODE}, {tag: CTRY, value: ADDRESS_COUNTRY}]}, {tag: PHON, value: PHONE_NUMBER1}, {tag: PHON, value: PHONE_NUMBER2}, {tag: FAX, value: ADDRESS_FAX1}, {tag: FAX, value: ADDRESS_FAX2}, {tag: EMAIL, value: ADDRESS_EMAIL1}, {tag: EMAIL, value: ADDRESS_EMAIL2}, {tag: WWW, value: ADDRESS_WEB_PAGE1}, {tag: WWW, value: ADDRESS_WEB_PAGE2}]}]}, {id: SUBM1, tag: SUBM}, {tag: COPR, value: COPYRIGHT_GEDCOM_FILE}, {tag: CHAR, value: UTF-8}, {tag: NOTE, value: GEDCOM_CONTENT_DESCRIPTION}, {tag: GEDC, children: [{tag: VERS, value: 5.5.1}, {tag: FORM, value: LINEAGE-LINKED}]}]}]", root.toString());
	}

}