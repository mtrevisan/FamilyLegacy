package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.addNode;


class HeaderTransformationTest{

	@Test
	void toFrom(){
		final GedcomNode header = composeGedcomHeader();
		final GedcomNode root = GedcomNode.createEmpty();
		root.addChild(header);

		Assertions.assertEquals("children: [{tag: HEAD, children: [{tag: SOUR, value: APPROVED_SYSTEM_ID, children: [{tag: VERS, value: VERSION_NUMBER}, {tag: NAME, value: NAME_OF_PRODUCT}, {tag: CORP, value: NAME_OF_BUSINESS, children: [{tag: ADDR, value: ADDRESS_LINE, children: [{tag: CONT, value: ADDRESS_LINE}, {tag: ADR1, value: ADDRESS_LINE1}, {tag: ADR2, value: ADDRESS_LINE2}, {tag: ADR3, value: ADDRESS_LINE3}, {tag: CITY, value: ADDRESS_CITY}, {tag: STAE, value: ADDRESS_STATE}, {tag: POST, value: ADDRESS_POSTAL_CODE}, {tag: CTRY, value: ADDRESS_COUNTRY}]}, {tag: PHON, value: PHONE_NUMBER1}, {tag: PHON, value: PHONE_NUMBER2}, {tag: EMAIL, value: ADDRESS_EMAIL1}, {tag: EMAIL, value: ADDRESS_EMAIL2}, {tag: FAX, value: ADDRESS_FAX1}, {tag: FAX, value: ADDRESS_FAX2}, {tag: WWW, value: ADDRESS_WEB_PAGE1}, {tag: WWW, value: ADDRESS_WEB_PAGE2}]}, {tag: DATA, value: NAME_OF_SOURCE_DATA, children: [{tag: DATE, value: PUBLICATION_DATE}, {tag: COPR, value: COPYRIGHT_SOURCE_DATA, children: [{tag: CONC, value: COPYRIGHT_SOURCE_DATA}]}]}]}, {tag: DEST, value: RECEIVING_SYSTEM_NAME}, {tag: DATE, value: TRANSMISSION_DATE, children: [{tag: TIME, value: TIME_VALUE}]}, {id: SUBM1, tag: SUBM}, {id: SUBN1, tag: SUBN}, {tag: FILE, value: FILE_NAME}, {tag: COPR, value: COPYRIGHT_GEDCOM_FILE}, {tag: GEDC, children: [{tag: VERS, value: VERSION_NUMBER}, {tag: FORM, value: LINEAGE-LINKED}]}, {tag: CHAR, value: UTF-8, children: [{tag: VERS, value: VERSION_NUMBER}]}, {tag: LANG, value: LANGUAGE_OF_TEXT_1}, {tag: LANG, value: LANGUAGE_OF_TEXT_2}, {tag: PLAC, children: [{tag: FORM, value: PLACE_HIERARCHY}]}, {tag: NOTE, value: GEDCOM_CONTENT_DESCRIPTION, children: [{tag: CONC, value: GEDCOM_CONTENT_DESCRIPTION}]}]}]", root.toString());

		final Transformation t = new HeaderTransformation();
		t.to(root);

		Assertions.assertEquals("children: [{tag: HEADER, children: [{tag: SOURCE, value: APPROVED_SYSTEM_ID, children: [{tag: VERSION, value: VERSION_NUMBER}, {tag: NAME, value: NAME_OF_PRODUCT}, {tag: CORPORATE, value: NAME_OF_BUSINESS, children: [{id: P1, tag: PLACE}]}]}, {id: SUBM1, tag: SUBMITTER}, {tag: COPYRIGHT, value: COPYRIGHT_GEDCOM_FILE}, {tag: CHARSET, value: UTF-8}, {tag: PROTOCOL_VERSION, value: 0.0.1}, {id: N1, tag: NOTE}]}, {id: N1, tag: NOTE, value: GEDCOM_CONTENT_DESCRIPTIONGEDCOM_CONTENT_DESCRIPTION}, {id: P1, tag: PLACE, children: [{tag: STREET, value: ADDRESS_LINE - ADDRESS_LINE1 - ADDRESS_LINE2 - ADDRESS_LINE3}, {tag: CITY, value: ADDRESS_CITY}, {tag: STATE, value: ADDRESS_STATE}, {tag: POSTAL_CODE, value: ADDRESS_POSTAL_CODE}, {tag: COUNTRY, value: ADDRESS_COUNTRY}, {tag: PHONE, value: PHONE_NUMBER1}, {tag: PHONE, value: PHONE_NUMBER2}, {tag: FAX, value: ADDRESS_FAX1}, {tag: FAX, value: ADDRESS_FAX2}, {tag: EMAIL, value: ADDRESS_EMAIL1}, {tag: EMAIL, value: ADDRESS_EMAIL2}, {tag: WWW, value: ADDRESS_WEB_PAGE1}, {tag: WWW, value: ADDRESS_WEB_PAGE2}]}]", root.toString());

		t.from(root);

		Assertions.assertEquals("children: [{tag: HEAD, children: [{tag: SOUR, value: APPROVED_SYSTEM_ID, children: [{tag: VERS, value: VERSION_NUMBER}, {tag: NAME, value: NAME_OF_PRODUCT}, {tag: CORP, value: NAME_OF_BUSINESS, children: [{tag: ADDR, value: ADDRESS_LINE - ADDRESS_LINE1 - ADDRESS_LINE2 - ADDRESS_LINE3, children: [{tag: CITY, value: ADDRESS_CITY}, {tag: STAE, value: ADDRESS_STATE}, {tag: POST, value: ADDRESS_POSTAL_CODE}, {tag: CTRY, value: ADDRESS_COUNTRY}]}, {tag: PHON, value: PHONE_NUMBER1}, {tag: PHON, value: PHONE_NUMBER2}, {tag: FAX, value: ADDRESS_FAX1}, {tag: FAX, value: ADDRESS_FAX2}, {tag: EMAIL, value: ADDRESS_EMAIL1}, {tag: EMAIL, value: ADDRESS_EMAIL2}, {tag: WWW, value: ADDRESS_WEB_PAGE1}, {tag: WWW, value: ADDRESS_WEB_PAGE2}]}]}, {id: SUBM1, tag: SUBM}, {tag: COPR, value: COPYRIGHT_GEDCOM_FILE}, {tag: CHAR, value: UTF-8}, {tag: GEDC, children: [{tag: VERS, value: 5.5.1}, {tag: FORM, value: LINEAGE-LINKED}]}, {tag: NOTE, value: GEDCOM_CONTENT_DESCRIPTIONGEDCOM_CONTENT_DESCRIPTION}]}]", root.toString());
	}

	private GedcomNode composeGedcomHeader(){
		final GedcomNode header = GedcomNode.create("HEAD");

		final GedcomNode source = GedcomNode.create("SOUR")
			.withValue("APPROVED_SYSTEM_ID");
		source.addChild(GedcomNode.create("VERS")
			.withValue("VERSION_NUMBER"));
		source.addChild(GedcomNode.create("NAME")
			.withValue("NAME_OF_PRODUCT"));
		final GedcomNode sourceCorporate = GedcomNode.create("CORP")
			.withValue("NAME_OF_BUSINESS");
		final GedcomNode sourceCorporateAddress = GedcomNode.create("ADDR")
			.withValue("ADDRESS_LINE");
		sourceCorporateAddress.addChild(GedcomNode.create("CONT")
			.withValue("ADDRESS_LINE"));
		sourceCorporateAddress.addChild(GedcomNode.create("ADR1")
			.withValue("ADDRESS_LINE1"));
		sourceCorporateAddress.addChild(GedcomNode.create("ADR2")
			.withValue("ADDRESS_LINE2"));
		sourceCorporateAddress.addChild(GedcomNode.create("ADR3")
			.withValue("ADDRESS_LINE3"));
		sourceCorporateAddress.addChild(GedcomNode.create("CITY")
			.withValue("ADDRESS_CITY"));
		sourceCorporateAddress.addChild(GedcomNode.create("STAE")
			.withValue("ADDRESS_STATE"));
		sourceCorporateAddress.addChild(GedcomNode.create("POST")
			.withValue("ADDRESS_POSTAL_CODE"));
		sourceCorporateAddress.addChild(GedcomNode.create("CTRY")
			.withValue("ADDRESS_COUNTRY"));
		sourceCorporate.addChild(sourceCorporateAddress);
		sourceCorporate.addChild(GedcomNode.create("PHON")
			.withValue("PHONE_NUMBER1"));
		sourceCorporate.addChild(GedcomNode.create("PHON")
			.withValue("PHONE_NUMBER2"));
		sourceCorporate.addChild(GedcomNode.create("EMAIL")
			.withValue("ADDRESS_EMAIL1"));
		sourceCorporate.addChild(GedcomNode.create("EMAIL")
			.withValue("ADDRESS_EMAIL2"));
		sourceCorporate.addChild(GedcomNode.create("FAX")
			.withValue("ADDRESS_FAX1"));
		sourceCorporate.addChild(GedcomNode.create("FAX")
			.withValue("ADDRESS_FAX2"));
		sourceCorporate.addChild(GedcomNode.create("WWW")
			.withValue("ADDRESS_WEB_PAGE1"));
		sourceCorporate.addChild(GedcomNode.create("WWW")
			.withValue("ADDRESS_WEB_PAGE2"));
		source.addChild(sourceCorporate);
		final GedcomNode sourceData = GedcomNode.create("DATA")
			.withValue("NAME_OF_SOURCE_DATA");
		sourceData.addChild(GedcomNode.create("DATE")
			.withValue("PUBLICATION_DATE"));
		final GedcomNode sourceDataCopyright = GedcomNode.create("COPR")
			.withValue("COPYRIGHT_SOURCE_DATA");
		sourceDataCopyright.addChild(GedcomNode.create("CONC")
			.withValue("COPYRIGHT_SOURCE_DATA"));
		sourceData.addChild(sourceDataCopyright);
		source.addChild(sourceData);
		final GedcomNode destination = GedcomNode.create("DEST")
			.withValue("RECEIVING_SYSTEM_NAME");
		final GedcomNode date = GedcomNode.create("DATE")
			.withValue("TRANSMISSION_DATE");
		final GedcomNode dateTime = GedcomNode.create("TIME")
			.withValue("TIME_VALUE");
		date.addChild(dateTime);
		final GedcomNode submitter = GedcomNode.create("SUBM")
			.withID("SUBM1");
		final GedcomNode submission = GedcomNode.create("SUBN")
			.withID("SUBN1");
		final GedcomNode file = GedcomNode.create("FILE")
			.withValue("FILE_NAME");
		final GedcomNode copyright = GedcomNode.create("COPR")
			.withValue("COPYRIGHT_GEDCOM_FILE");
		final GedcomNode gedcom = GedcomNode.create("GEDC");
		gedcom.addChild(GedcomNode.create("VERS")
			.withValue("VERSION_NUMBER"));
		gedcom.addChild(GedcomNode.create("FORM")
			.withValue("LINEAGE-LINKED"));
		final GedcomNode charset = GedcomNode.create("CHAR")
			.withValue("UTF-8");
		charset.addChild(GedcomNode.create("VERS")
			.withValue("VERSION_NUMBER"));
		final GedcomNode language1 = GedcomNode.create("LANG")
			.withValue("LANGUAGE_OF_TEXT_1");
		final GedcomNode language2 = GedcomNode.create("LANG")
			.withValue("LANGUAGE_OF_TEXT_2");
		final GedcomNode place = GedcomNode.create("PLAC");
		place.addChild(GedcomNode.create("FORM")
			.withValue("PLACE_HIERARCHY"));
		final GedcomNode note = GedcomNode.create("NOTE")
			.withValue("GEDCOM_CONTENT_DESCRIPTION");
		note.addChild(GedcomNode.create("CONC")
			.withValue("GEDCOM_CONTENT_DESCRIPTION"));

		addNode(source, header);
		addNode(destination, header);
		addNode(date, header);
		addNode(submitter, header);
		addNode(submission, header);
		addNode(file, header);
		addNode(copyright, header);
		addNode(gedcom, header);
		addNode(charset, header);
		addNode(language1, header);
		addNode(language2, header);
		addNode(place, header);
		addNode(note, header);

		return header;
	}

}