package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


/** PLACE_STRUCTURE() + ADDRESS_STRUCTURE() */
class PlaceAndAddressStructureTransformationTest{

	@Test
	void to(){
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
						.withID("N1"))
				))
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
				.withValue("ADDRESS_WEB_PAGE2"))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1")
				.withValue("SUBMITTER_TEXT"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: PLAC, value: PLACE_NAME, children: [{tag: FORM, value: PLACE_HIERARCHY}, {tag: FONE, value: PLACE_PHONETIC_VARIATION, children: [{tag: TYPE, value: PHONETIC_TYPE}]}, {tag: ROMN, value: PLACE_ROMANIZED_VARIATION, children: [{tag: TYPE, value: ROMANIZED_TYPE}]}, {tag: MAP, children: [{tag: LATI, value: PLACE_LATITUDE}, {tag: LONG, value: PLACE_LONGITUDE}]}, {id: N1, tag: NOTE}]}]}, {tag: ADDR, value: ADDRESS_LINE, children: [{tag: CONT, value: ADDRESS_LINE}, {tag: ADR1, value: ADDRESS_LINE1}, {tag: ADR2, value: ADDRESS_LINE2}, {tag: ADR3, value: ADDRESS_LINE3}, {tag: CITY, value: ADDRESS_CITY}, {tag: STAE, value: ADDRESS_STATE}, {tag: POST, value: ADDRESS_POSTAL_CODE}, {tag: CTRY, value: ADDRESS_COUNTRY}]}, {tag: PHON, value: PHONE_NUMBER1}, {tag: PHON, value: PHONE_NUMBER2}, {tag: EMAIL, value: ADDRESS_EMAIL1}, {tag: EMAIL, value: ADDRESS_EMAIL2}, {tag: FAX, value: ADDRESS_FAX1}, {tag: FAX, value: ADDRESS_FAX2}, {tag: WWW, value: ADDRESS_WEB_PAGE1}, {tag: WWW, value: ADDRESS_WEB_PAGE2}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());

		final Transformation t = new PlaceAndAddressStructureTransformation();
		t.to(extractSubStructure(root, "PARENT", "CHAN"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: CHANGE, children: [{tag: DATE, value: CHANGE_DATE, children: [{tag: TIME, value: TIME_VALUE}]}, {id: N1, tag: NOTE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());
	}

	@Test
	void from(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("PLACE")
					.addChild(GedcomNode.create("DATA")
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
						.addChild(GedcomNode.create("DATE")
							.withValue("DATE_VALUE"))
						.addChild(GedcomNode.create("NOTE")
							.withValue("N1"))
						.addChild(GedcomNode.create("SUBMITTER")
							.withValue("SUBM1"))
						.addChild(GedcomNode.create("RESTRICTION")
							.withValue("RESTRICTION_NOTICE"))
						.withValue("CHANGE_DATE")
						.addChild(GedcomNode.create("TIME")
							.withValue("TIME_VALUE")))))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1")
				.withValue("SUBMITTER_TEXT"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: PLACE, children: [{tag: DATA, value: CHANGE_DATE, children: [{tag: LOCALE, value: en_US}, {tag: COMPANY, value: COMPANY_NAME}, {tag: APARTMENT, value: ADDRESS_APARTMENT}, {tag: NUMBER, value: ADDRESS_NUMBER}, {tag: STREET, value: ADDRESS_STREET, children: [{tag: TYPE, value: ADDRESS_STREET_TYPE}]}, {tag: DISTRICT, value: ADDRESS_DISTRICT}, {tag: TOWN, value: ADDRESS_TOWN}, {tag: CITY, value: ADDRESS_CITY}, {tag: COUNTY, value: ADDRESS_COUNTY}, {tag: STATE, value: ADDRESS_STATE}, {tag: POSTAL_CODE, value: ADDRESS_POSTAL_CODE}, {tag: COUNTRY, value: ADDRESS_COUNTRY}, {tag: MAP, children: [{tag: LATITUDE, value: PLACE_LATITUDE}, {tag: LONGITUDE, value: PLACE_LONGITUDE}]}, {tag: PHONE, value: PHONE_NUMBER, children: [{tag: TYPE, value: PHONE_NUMBER_TYPE}]}, {tag: EMAIL, value: ADDRESS_EMAIL, children: [{tag: TYPE, value: ADDRESS_EMAIL_TYPE}]}, {tag: WWW, value: ADDRESS_WEB_PAGE, children: [{tag: TYPE, value: ADDRESS_WEB_PAGE_TYPE}]}, {tag: DATE, value: DATE_VALUE}, {tag: NOTE, value: N1}, {tag: SUBMITTER, value: SUBM1}, {tag: RESTRICTION, value: RESTRICTION_NOTICE}, {tag: TIME, value: TIME_VALUE}]}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());

		final Transformation t = new PlaceAndAddressStructureTransformation();
		t.from(extractSubStructure(root, "PARENT", "CHANGE"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE, children: [{tag: TIME, value: TIME_VALUE}]}, {id: N1, tag: NOTE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());
	}

}