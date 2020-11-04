package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class PlaceRecordTransformationTest{

	@Test
	void placeTo(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("PLAC")
					.withValue("PLACE_NAME")
					.addChild(GedcomNode.create("FORM")
						.withValue("PLACE_HIERARCHY"))
					.addChild(GedcomNode.create("FONE")
						.withValue("PLACE_PHONETIC_VARIATION")
						.addChild(GedcomNode.create("TYPE")
							.withValue("PHONETIC_TYPE")))
					.addChild(GedcomNode.create("ROMN")
						.withValue("PLACE_ROMANIZED_VARIATION")
						.addChild(GedcomNode.create("TYPE")
							.withValue("ROMANIZED_TYPE")))
					.addChild(GedcomNode.create("MAP")
						.addChild(GedcomNode.create("LATI")
							.withValue("PLACE_LATITUDE"))
						.addChild(GedcomNode.create("LONG")
							.withValue("PLACE_LONGITUDE")))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1")))
				.addChild(GedcomNode.create("NOTE")
					.withID("N1")
					.withValue("SUBMITTER_TEXT")));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: PLAC, value: PLACE_NAME, children: [{tag: FORM, value: PLACE_HIERARCHY}, {tag: FONE, value: PLACE_PHONETIC_VARIATION, children: [{tag: TYPE, value: PHONETIC_TYPE}]}, {tag: ROMN, value: PLACE_ROMANIZED_VARIATION, children: [{tag: TYPE, value: ROMANIZED_TYPE}]}, {tag: MAP, children: [{tag: LATI, value: PLACE_LATITUDE}, {tag: LONG, value: PLACE_LONGITUDE}]}, {id: N1, tag: NOTE}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]}]", root.toString());

		final Transformation t = new PlaceRecordTransformation();
		t.to(extractSubStructure(root, "PARENT"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: N1, tag: NOTE, value: SUBMITTER_TEXT}, {id: P1, tag: PLACE}]}, {id: P1, tag: PLACE, children: [{tag: ADDRESS, value: PLACE_NAME, children: [{id: N1, tag: NOTE}]}]}]", root.toString());
	}


	@Test
	void addressTo(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("ADDR")
					.withValue("ADDRESS_LINE")
					.addChild(GedcomNode.create("CONC")
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
					.withValue("PHONE_NUMBER"))
				.addChild(GedcomNode.create("EMAIL")
					.withValue("ADDRESS_EMAIL"))
				.addChild(GedcomNode.create("FAX")
					.withValue("ADDRESS_FAX"))
				.addChild(GedcomNode.create("WWW")
					.withValue("ADDRESS_WEB_PAGE")));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: ADDR, value: ADDRESS_LINE, children: [{tag: CONC, value: ADDRESS_LINE}, {tag: ADR1, value: ADDRESS_LINE1}, {tag: ADR2, value: ADDRESS_LINE2}, {tag: ADR3, value: ADDRESS_LINE3}, {tag: CITY, value: ADDRESS_CITY}, {tag: STAE, value: ADDRESS_STATE}, {tag: POST, value: ADDRESS_POSTAL_CODE}, {tag: CTRY, value: ADDRESS_COUNTRY}]}, {tag: PHON, value: PHONE_NUMBER}, {tag: EMAIL, value: ADDRESS_EMAIL}, {tag: FAX, value: ADDRESS_FAX}, {tag: WWW, value: ADDRESS_WEB_PAGE}]}]", root.toString());

		final Transformation t = new PlaceRecordTransformation();
		t.to(extractSubStructure(root, "PARENT"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: P1, tag: PLACE}]}, {id: P1, tag: PLACE, children: [{tag: ADDRESS, value: ADDRESS_LINEADDRESS_LINE - ADDRESS_LINE1 - ADDRESS_LINE2 - ADDRESS_LINE3, children: [{tag: CITY, value: ADDRESS_CITY}, {tag: STATE, value: ADDRESS_STATE}, {tag: POSTAL_CODE, value: ADDRESS_POSTAL_CODE}, {tag: COUNTRY, value: ADDRESS_COUNTRY}, {tag: PHONE, value: PHONE_NUMBER}, {tag: PHONE, value: ADDRESS_FAX, children: [{tag: TYPE, value: FAX}]}, {tag: EMAIL, value: ADDRESS_EMAIL}, {tag: WWW, value: ADDRESS_WEB_PAGE}]}]}]", root.toString());
	}

	@Test
	void addressFrom(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("PLACE")
					.withID("P1")))
			.addChild(GedcomNode.create("PLACE")
				.withID("P1"))
				.addChild(GedcomNode.create("ADDRESS")
					.withValue("ADDRESS_LINE")
					.addChild(GedcomNode.create("LOCALE")
						.withValue("en_US"))
					.addChild(GedcomNode.create("COMPANY")
						.withValue("COMPANY_NAME"))
					.addChild(GedcomNode.create("APARTMENT")
						.withValue("ADDRESS_APARTMENT"))
					.addChild(GedcomNode.create("NUMBER")
						.withValue("ADDRESS_NUMBER"))
					.addChild(GedcomNode.create("STREET")
						.withValue("ADDRESS_STREET")
						.addChild(GedcomNode.create("TYPE")
							.withValue("ADDRESS_STREET_TYPE")))
					.addChild(GedcomNode.create("DISTRICT")
						.withValue("ADDRESS_DISTRICT"))
					.addChild(GedcomNode.create("TOWN")
						.withValue("ADDRESS_TOWN"))
					.addChild(GedcomNode.create("CITY")
						.withValue("ADDRESS_CITY"))
					.addChild(GedcomNode.create("STATE")
						.withValue("ADDRESS_STATE"))
					.addChild(GedcomNode.create("POSTAL_CODE")
						.withValue("ADDRESS_POSTAL_CODE"))
					.addChild(GedcomNode.create("MAP")
						.addChild(GedcomNode.create("LATITUDE")
							.withValue("PLACE_LATITUDE"))
						.addChild(GedcomNode.create("LONGITUDE")
							.withValue("PLACE_LONGITUDE")))
					.addChild(GedcomNode.create("PHONE")
						.withValue("PHONE_NUMBER")
						.addChild(GedcomNode.create("TYPE")
							.withValue("PHONE_NUMBER_TYPE")))
					.addChild(GedcomNode.create("EMAIL")
						.withValue("ADDRESS_EMAIL")
						.addChild(GedcomNode.create("TYPE")
							.withValue("ADDRESS_EMAIL_TYPE")))
					.addChild(GedcomNode.create("WWW")
						.withValue("ADDRESS_WEB_PAGE")
						.addChild(GedcomNode.create("TYPE")
							.withValue("ADDRESS_WEB_PAGE_TYPE")))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))
					.addChild(GedcomNode.create("SUBMITTER")
						.withID("SUBM1"))
					.addChild(GedcomNode.create("RESTRICTION")
						.withValue("RESTRICTION_NOTICE"))
					.addChild(GedcomNode.create("CHANGE")
						.addChild(GedcomNode.create("DATE")
							.withValue("CHANGE_DATE")
							.addChild(GedcomNode.create("TIME")
								.withValue("TIME_VALUE")))
						.addChild(GedcomNode.create("NOTE")
							.withID("N2"))));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: ADDRESS, value: ADDRESS_LINE, children: [{tag: LOCALE, value: en_US}, {tag: COMPANY, value: COMPANY_NAME}, {tag: APARTMENT, value: ADDRESS_APARTMENT}, {tag: NUMBER, value: ADDRESS_NUMBER}, {tag: STREET, value: ADDRESS_STREET, children: [{tag: TYPE, value: ADDRESS_STREET_TYPE}]}, {tag: DISTRICT, value: ADDRESS_DISTRICT}, {tag: TOWN, value: ADDRESS_TOWN}, {tag: CITY, value: ADDRESS_CITY}, {tag: STATE, value: ADDRESS_STATE}, {tag: POSTAL_CODE, value: ADDRESS_POSTAL_CODE}, {tag: MAP, children: [{tag: LATITUDE, value: PLACE_LATITUDE}, {tag: LONGITUDE, value: PLACE_LONGITUDE}]}, {tag: PHONE, value: PHONE_NUMBER, children: [{tag: TYPE, value: PHONE_NUMBER_TYPE}]}, {tag: EMAIL, value: ADDRESS_EMAIL, children: [{tag: TYPE, value: ADDRESS_EMAIL_TYPE}]}, {tag: WWW, value: ADDRESS_WEB_PAGE, children: [{tag: TYPE, value: ADDRESS_WEB_PAGE_TYPE}]}, {id: N1, tag: NOTE}, {id: SUBM1, tag: SUBMITTER}, {tag: RESTRICTION, value: RESTRICTION_NOTICE}, {tag: CHANGE, children: [{tag: DATE, value: CHANGE_DATE, children: [{tag: TIME, value: TIME_VALUE}]}, {id: N2, tag: NOTE}]}]}]}]", root.toString());

		final Transformation t = new PlaceRecordTransformation();
		t.from(extractSubStructure(root, "PARENT"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: ADDR, value: ADDRESS_LINE, children: [{tag: _LOCALE, value: en_US}, {tag: _COMPANY, value: COMPANY_NAME}, {tag: _APARTMENT, value: ADDRESS_APARTMENT}, {tag: _NUMBER, value: ADDRESS_NUMBER}, {tag: _STREET, value: ADDRESS_STREET, children: [{tag: _TYPE, value: ADDRESS_STREET_TYPE}]}, {tag: _DISTRICT, value: ADDRESS_DISTRICT}, {tag: _TOWN, value: ADDRESS_TOWN}, {tag: _CITY, value: ADDRESS_CITY}, {tag: STAE, value: ADDRESS_STATE}, {tag: POST, value: ADDRESS_POSTAL_CODE}, {id: N1, tag: _NOTE}, {id: SUBM1, tag: _SUBMITTER}, {tag: _RESTRICTION, value: RESTRICTION_NOTICE}, {tag: _CHANGE, children: [{tag: _DATE, value: CHANGE_DATE, children: [{tag: _TIME, value: TIME_VALUE}]}, {id: N2, tag: NOTE}]}]}, {tag: PLAC, children: [{tag: MAP, children: [{tag: LATI, value: PLACE_LATITUDE}, {tag: LONG, value: PLACE_LONGITUDE}]}, {id: N1, tag: _NOTE}]}, {tag: PHON, value: PHONE_NUMBER, children: [{tag: _TYPE, value: PHONE_NUMBER_TYPE}]}, {tag: EMAIL, value: ADDRESS_EMAIL, children: [{tag: _TYPE, value: ADDRESS_EMAIL_TYPE}]}, {tag: WWW, value: ADDRESS_WEB_PAGE, children: [{tag: _TYPE, value: ADDRESS_WEB_PAGE_TYPE}]}]}]", root.toString());
	}


	@Test
	void placeAddressTo(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("PLAC")
					.withValue("PLACE_NAME")
					.addChild(GedcomNode.create("FORM")
						.withValue("PLACE_HIERARCHY"))
					.addChild(GedcomNode.create("FONE")
						.withValue("PLACE_PHONETIC_VARIATION")
						.addChild(GedcomNode.create("TYPE")
							.withValue("PHONETIC_TYPE")))
					.addChild(GedcomNode.create("ROMN")
						.withValue("PLACE_ROMANIZED_VARIATION")
						.addChild(GedcomNode.create("TYPE")
							.withValue("ROMANIZED_TYPE")))
					.addChild(GedcomNode.create("MAP")
						.addChild(GedcomNode.create("LATI")
							.withValue("PLACE_LATITUDE"))
						.addChild(GedcomNode.create("LONG")
							.withValue("PLACE_LONGITUDE")))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1")))
				.addChild(GedcomNode.create("ADDR")
					.withValue("ADDRESS_LINE")
					.addChild(GedcomNode.create("CONC")
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
					.withValue("ADDRESS_WEB_PAGE2"))
				.addChild(GedcomNode.create("NOTE")
					.withID("N1")
					.withValue("SUBMITTER_TEXT")));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: PLAC, value: PLACE_NAME, children: [{tag: FORM, value: PLACE_HIERARCHY}, {tag: FONE, value: PLACE_PHONETIC_VARIATION, children: [{tag: TYPE, value: PHONETIC_TYPE}]}, {tag: ROMN, value: PLACE_ROMANIZED_VARIATION, children: [{tag: TYPE, value: ROMANIZED_TYPE}]}, {tag: MAP, children: [{tag: LATI, value: PLACE_LATITUDE}, {tag: LONG, value: PLACE_LONGITUDE}]}, {id: N1, tag: NOTE}]}, {tag: ADDR, value: ADDRESS_LINE, children: [{tag: CONC, value: ADDRESS_LINE}, {tag: ADR1, value: ADDRESS_LINE1}, {tag: ADR2, value: ADDRESS_LINE2}, {tag: ADR3, value: ADDRESS_LINE3}, {tag: CITY, value: ADDRESS_CITY}, {tag: STAE, value: ADDRESS_STATE}, {tag: POST, value: ADDRESS_POSTAL_CODE}, {tag: CTRY, value: ADDRESS_COUNTRY}]}, {tag: PHON, value: PHONE_NUMBER1}, {tag: PHON, value: PHONE_NUMBER2}, {tag: EMAIL, value: ADDRESS_EMAIL1}, {tag: EMAIL, value: ADDRESS_EMAIL2}, {tag: FAX, value: ADDRESS_FAX1}, {tag: FAX, value: ADDRESS_FAX2}, {tag: WWW, value: ADDRESS_WEB_PAGE1}, {tag: WWW, value: ADDRESS_WEB_PAGE2}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]}]", root.toString());

		final Transformation t = new PlaceRecordTransformation();
		t.to(extractSubStructure(root, "PARENT"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: N1, tag: NOTE, value: SUBMITTER_TEXT}, {id: P1, tag: PLACE}]}, {id: P1, tag: PLACE, children: [{tag: ADDRESS, value: ADDRESS_LINEADDRESS_LINE - ADDRESS_LINE1 - ADDRESS_LINE2 - ADDRESS_LINE3, children: [{tag: CITY, value: ADDRESS_CITY}, {tag: STATE, value: ADDRESS_STATE}, {tag: POSTAL_CODE, value: ADDRESS_POSTAL_CODE}, {tag: COUNTRY, value: ADDRESS_COUNTRY}, {tag: PHONE, value: PHONE_NUMBER1}, {tag: PHONE, value: PHONE_NUMBER2}, {tag: PHONE, value: ADDRESS_FAX1, children: [{tag: TYPE, value: FAX}]}, {tag: PHONE, value: ADDRESS_FAX2, children: [{tag: TYPE, value: FAX}]}, {tag: EMAIL, value: ADDRESS_EMAIL1}, {tag: EMAIL, value: ADDRESS_EMAIL2}, {tag: WWW, value: ADDRESS_WEB_PAGE1}, {tag: WWW, value: ADDRESS_WEB_PAGE2}, {tag: MAP, children: [{tag: LATITUDE, value: PLACE_LATITUDE}, {tag: LONGITUDE, value: PLACE_LONGITUDE}]}, {id: N1, tag: NOTE}]}]}]", root.toString());
	}

	@Test
	void placeAddressFrom(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("ADDRESS")
					.addChild(GedcomNode.create("LOCALE")
						.withValue("en_US"))
					.addChild(GedcomNode.create("COMPANY")
						.withValue("COMPANY_NAME"))
					.addChild(GedcomNode.create("APARTMENT")
						.withValue("ADDRESS_APARTMENT"))
					.addChild(GedcomNode.create("NUMBER")
						.withValue("ADDRESS_NUMBER"))
					.addChild(GedcomNode.create("STREET")
						.withValue("ADDRESS_STREET")
						.addChild(GedcomNode.create("TYPE")
							.withValue("ADDRESS_STREET_TYPE")))
					.addChild(GedcomNode.create("DISTRICT")
						.withValue("ADDRESS_DISTRICT"))
					.addChild(GedcomNode.create("TOWN")
						.withValue("ADDRESS_TOWN"))
					.addChild(GedcomNode.create("CITY")
						.withValue("ADDRESS_CITY"))
					.addChild(GedcomNode.create("COUNTY")
						.withValue("ADDRESS_COUNTY"))
					.addChild(GedcomNode.create("STATE")
						.withValue("ADDRESS_STATE"))
					.addChild(GedcomNode.create("POSTAL_CODE")
						.withValue("ADDRESS_POSTAL_CODE"))
					.addChild(GedcomNode.create("COUNTRY")
						.withValue("ADDRESS_COUNTRY"))
					.addChild(GedcomNode.create("MAP")
						.addChild(GedcomNode.create("LATITUDE")
								.withValue("PLACE_LATITUDE"))
							.addChild(GedcomNode.create("LONGITUDE")
								.withValue("PLACE_LONGITUDE")))
					.addChild(GedcomNode.create("PHONE")
						.withValue("PHONE_NUMBER")
						.addChild(GedcomNode.create("TYPE")
							.withValue("PHONE_NUMBER_TYPE")))
					.addChild(GedcomNode.create("EMAIL")
						.withValue("ADDRESS_EMAIL")
						.addChild(GedcomNode.create("TYPE")
							.withValue("ADDRESS_EMAIL_TYPE")))
					.addChild(GedcomNode.create("WWW")
						.withValue("ADDRESS_WEB_PAGE")
						.addChild(GedcomNode.create("TYPE")
							.withValue("ADDRESS_WEB_PAGE_TYPE")))
					.addChild(GedcomNode.create("NOTE")
						.withValue("N1"))
					.addChild(GedcomNode.create("SUBMITTER")
						.withValue("SUBM1"))
					.addChild(GedcomNode.create("RESTRICTION")
						.withValue("RESTRICTION_NOTICE"))
					.withValue("CHANGE_DATE")))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1")
				.withValue("SUBMITTER_TEXT"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: ADDRESS, value: CHANGE_DATE, children: [{tag: LOCALE, value: en_US}, {tag: COMPANY, value: COMPANY_NAME}, {tag: APARTMENT, value: ADDRESS_APARTMENT}, {tag: NUMBER, value: ADDRESS_NUMBER}, {tag: STREET, value: ADDRESS_STREET, children: [{tag: TYPE, value: ADDRESS_STREET_TYPE}]}, {tag: DISTRICT, value: ADDRESS_DISTRICT}, {tag: TOWN, value: ADDRESS_TOWN}, {tag: CITY, value: ADDRESS_CITY}, {tag: COUNTY, value: ADDRESS_COUNTY}, {tag: STATE, value: ADDRESS_STATE}, {tag: POSTAL_CODE, value: ADDRESS_POSTAL_CODE}, {tag: COUNTRY, value: ADDRESS_COUNTRY}, {tag: MAP, children: [{tag: LATITUDE, value: PLACE_LATITUDE}, {tag: LONGITUDE, value: PLACE_LONGITUDE}]}, {tag: PHONE, value: PHONE_NUMBER, children: [{tag: TYPE, value: PHONE_NUMBER_TYPE}]}, {tag: EMAIL, value: ADDRESS_EMAIL, children: [{tag: TYPE, value: ADDRESS_EMAIL_TYPE}]}, {tag: WWW, value: ADDRESS_WEB_PAGE, children: [{tag: TYPE, value: ADDRESS_WEB_PAGE_TYPE}]}, {tag: NOTE, value: N1}, {tag: SUBMITTER, value: SUBM1}, {tag: RESTRICTION, value: RESTRICTION_NOTICE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());

		final Transformation t = new PlaceRecordTransformation();
		t.from(extractSubStructure(root, "PARENT"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: ADDR, value: CHANGE_DATE, children: [{tag: _LOCALE, value: en_US}, {tag: _COMPANY, value: COMPANY_NAME}, {tag: _APARTMENT, value: ADDRESS_APARTMENT}, {tag: _NUMBER, value: ADDRESS_NUMBER}, {tag: _STREET, value: ADDRESS_STREET, children: [{tag: _TYPE, value: ADDRESS_STREET_TYPE}]}, {tag: _DISTRICT, value: ADDRESS_DISTRICT}, {tag: _TOWN, value: ADDRESS_TOWN}, {tag: _CITY, value: ADDRESS_CITY}, {tag: _COUNTY, value: ADDRESS_COUNTY}, {tag: STAE, value: ADDRESS_STATE}, {tag: POST, value: ADDRESS_POSTAL_CODE}, {tag: CTRY, value: ADDRESS_COUNTRY}, {tag: _NOTE, value: N1}, {tag: _SUBMITTER, value: SUBM1}, {tag: _RESTRICTION, value: RESTRICTION_NOTICE}]}, {tag: PLAC, children: [{tag: MAP, children: [{tag: LATI, value: PLACE_LATITUDE}, {tag: LONG, value: PLACE_LONGITUDE}]}, {tag: _NOTE, value: N1}]}, {tag: PHON, value: PHONE_NUMBER, children: [{tag: _TYPE, value: PHONE_NUMBER_TYPE}]}, {tag: EMAIL, value: ADDRESS_EMAIL, children: [{tag: _TYPE, value: ADDRESS_EMAIL_TYPE}]}, {tag: WWW, value: ADDRESS_WEB_PAGE, children: [{tag: _TYPE, value: ADDRESS_WEB_PAGE_TYPE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());
	}

}