package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.addNode;


class HeaderTransformationTest{

	@Test
	void to(){
		final GedcomNode header = composeGedcomHeader();
		final GedcomNode root = GedcomNode.createEmpty();
		root.addChild(header);

		System.out.println(root);

		Transformation t = new HeaderTransformation();
		t.to(root);

		System.out.println(root);
	}

	private GedcomNode composeGedcomHeader(){
		final GedcomNode header = GedcomNode.create(0, "HEAD");

		final GedcomNode source = GedcomNode.create(1, "SOUR")
			.withValue("APPROVED_SYSTEM_ID");
		source.addChild(GedcomNode.create(2, "VERS")
			.withValue("VERSION_NUMBER"));
		source.addChild(GedcomNode.create(2, "NAME")
			.withValue("NAME_OF_PRODUCT"));
		final GedcomNode sourceCorporate = GedcomNode.create(2, "CORP")
			.withValue("NAME_OF_BUSINESS");
		final GedcomNode sourceCorporateAddress = GedcomNode.create(3, "ADDR")
			.withValue("ADDRESS_LINE");
		sourceCorporateAddress.addChild(GedcomNode.create(4, "CONT")
			.withValue("ADDRESS_LINE"));
		sourceCorporateAddress.addChild(GedcomNode.create(4, "ADR1")
			.withValue("ADDRESS_LINE1"));
		sourceCorporateAddress.addChild(GedcomNode.create(4, "ADR2")
			.withValue("ADDRESS_LINE2"));
		sourceCorporateAddress.addChild(GedcomNode.create(4, "ADR3")
			.withValue("ADDRESS_LINE3"));
		sourceCorporateAddress.addChild(GedcomNode.create(4, "CITY")
			.withValue("ADDRESS_CITY"));
		sourceCorporateAddress.addChild(GedcomNode.create(4, "STAE")
			.withValue("ADDRESS_STATE"));
		sourceCorporateAddress.addChild(GedcomNode.create(4, "POST")
			.withValue("ADDRESS_POSTAL_CODE"));
		sourceCorporateAddress.addChild(GedcomNode.create(4, "CTRY")
			.withValue("ADDRESS_COUNTRY"));
		sourceCorporate.addChild(sourceCorporateAddress);
		sourceCorporate.addChild(GedcomNode.create(3, "PHON")
			.withValue("PHONE_NUMBER"));
		sourceCorporate.addChild(GedcomNode.create(3, "PHON")
			.withValue("PHONE_NUMBER"));
		sourceCorporate.addChild(GedcomNode.create(3, "EMAIL")
			.withValue("ADDRESS_EMAIL"));
		sourceCorporate.addChild(GedcomNode.create(3, "EMAIL")
			.withValue("ADDRESS_EMAIL"));
		sourceCorporate.addChild(GedcomNode.create(3, "FAX")
			.withValue("ADDRESS_FAX"));
		sourceCorporate.addChild(GedcomNode.create(3, "FAX")
			.withValue("ADDRESS_FAX"));
		sourceCorporate.addChild(GedcomNode.create(3, "WWW")
			.withValue("ADDRESS_WEB_PAGE"));
		sourceCorporate.addChild(GedcomNode.create(3, "WWW")
			.withValue("ADDRESS_WEB_PAGE"));
		source.addChild(sourceCorporate);
		final GedcomNode sourceData = GedcomNode.create(2, "DATA")
			.withValue("NAME_OF_SOURCE_DATA");
		sourceData.addChild(GedcomNode.create(3, "DATE")
			.withValue("PUBLICATION_DATE"));
		final GedcomNode sourceDataCopyright = GedcomNode.create(3, "COPR")
			.withValue("COPYRIGHT_SOURCE_DATA");
		sourceDataCopyright.addChild(GedcomNode.create(4, "CONC")
			.withValue("COPYRIGHT_SOURCE_DATA"));
		sourceData.addChild(sourceDataCopyright);
		source.addChild(sourceData);
		final GedcomNode destination = GedcomNode.create(1, "DEST")
			.withValue("RECEIVING_SYSTEM_NAME");
		final GedcomNode date = GedcomNode.create(1, "DATE")
			.withValue("TRANSMISSION_DATE");
		final GedcomNode dateTime = GedcomNode.create(1, "TIME")
			.withValue("TIME_VALUE");
		date.addChild(dateTime);
		final GedcomNode submitter = GedcomNode.create(1, "SUBM")
			.withID("SUBM1");
		final GedcomNode submission = GedcomNode.create(1, "SUBN")
			.withID("SUBN1");
		final GedcomNode file = GedcomNode.create(1, "FILE")
			.withValue("FILE_NAME");
		final GedcomNode copyright = GedcomNode.create(1, "COPR")
			.withValue("COPYRIGHT_GEDCOM_FILE");
		final GedcomNode gedcom = GedcomNode.create(1, "GEDC");
		gedcom.addChild(GedcomNode.create(2, "VERS")
			.withValue("VERSION_NUMBER"));
		gedcom.addChild(GedcomNode.create(2, "FORM")
			.withValue("GEDCOM_FORM"));
		final GedcomNode charset = GedcomNode.create(1, "CHAR")
			.withValue("CHARACTER_SET");
		charset.addChild(GedcomNode.create(2, "VERS")
			.withValue("VERSION_NUMBER"));
		final GedcomNode language1 = GedcomNode.create(1, "LANG")
			.withValue("LANGUAGE_OF_TEXT_1");
		final GedcomNode language2 = GedcomNode.create(1, "LANG")
			.withValue("LANGUAGE_OF_TEXT_2");
		final GedcomNode place = GedcomNode.create(1, "PLAC");
		place.addChild(GedcomNode.create(2, "FORM")
			.withValue("PLACE_HIERARCHY"));
		final GedcomNode note = GedcomNode.create(1, "NOTE")
			.withValue("GEDCOM_CONTENT_DESCRIPTION");
		note.addChild(GedcomNode.create(2, "CONC")
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