package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.addNode;


class NameTransformationTest{

	@Test
	void toFrom(){
		final GedcomNode header = composeGedcomName();
		final GedcomNode root = GedcomNode.createEmpty();
		root.addChild(header);

		Assertions.assertEquals("children: [{tag: HEAD, children: [{tag: SOUR, value: APPROVED_SYSTEM_ID, children: [{tag: VERS, value: VERSION_NUMBER}, {tag: NAME, value: NAME_OF_PRODUCT}, {tag: CORP, value: NAME_OF_BUSINESS, children: [{tag: ADDR, value: ADDRESS_LINE, children: [{tag: CONT, value: ADDRESS_LINE}, {tag: ADR1, value: ADDRESS_LINE1}, {tag: ADR2, value: ADDRESS_LINE2}, {tag: ADR3, value: ADDRESS_LINE3}, {tag: CITY, value: ADDRESS_CITY}, {tag: STAE, value: ADDRESS_STATE}, {tag: POST, value: ADDRESS_POSTAL_CODE}, {tag: CTRY, value: ADDRESS_COUNTRY}]}, {tag: PHON, value: PHONE_NUMBER1}, {tag: PHON, value: PHONE_NUMBER2}, {tag: EMAIL, value: ADDRESS_EMAIL1}, {tag: EMAIL, value: ADDRESS_EMAIL2}, {tag: FAX, value: ADDRESS_FAX1}, {tag: FAX, value: ADDRESS_FAX2}, {tag: WWW, value: ADDRESS_WEB_PAGE1}, {tag: WWW, value: ADDRESS_WEB_PAGE2}]}, {tag: DATA, value: NAME_OF_SOURCE_DATA, children: [{tag: DATE, value: PUBLICATION_DATE}, {tag: COPR, value: COPYRIGHT_SOURCE_DATA, children: [{tag: CONC, value: COPYRIGHT_SOURCE_DATA}]}]}]}, {tag: DEST, value: RECEIVING_SYSTEM_NAME}, {tag: DATE, value: TRANSMISSION_DATE, children: [{tag: TIME, value: TIME_VALUE}]}, {id: SUBM1, tag: SUBM}, {id: SUBN1, tag: SUBN}, {tag: FILE, value: FILE_NAME}, {tag: COPR, value: COPYRIGHT_GEDCOM_FILE}, {tag: GEDC, children: [{tag: VERS, value: VERSION_NUMBER}, {tag: FORM, value: LINEAGE-LINKED}]}, {tag: CHAR, value: CHARACTER_SET, children: [{tag: VERS, value: VERSION_NUMBER}]}, {tag: LANG, value: LANGUAGE_OF_TEXT_1}, {tag: LANG, value: LANGUAGE_OF_TEXT_2}, {tag: PLAC, children: [{tag: FORM, value: PLACE_HIERARCHY}]}, {tag: NOTE, value: GEDCOM_CONTENT_DESCRIPTION, children: [{tag: CONC, value: GEDCOM_CONTENT_DESCRIPTION}]}]}]", root.toString());

		final Transformation t = new HeaderTransformation();
		t.to(root);

		Assertions.assertEquals("children: [{tag: HEADER, children: [{tag: SOURCE, value: APPROVED_SYSTEM_ID, children: [{tag: VERSION, value: VERSION_NUMBER}, {tag: NAME, value: NAME_OF_PRODUCT}, {tag: CORPORATE, value: NAME_OF_BUSINESS, children: [{id: P1, tag: PLACE}]}]}, {id: SUBM1, tag: SUBMITTER}, {tag: COPYRIGHT, value: COPYRIGHT_GEDCOM_FILE}, {tag: CHARSET, value: CHARACTER_SET}, {tag: PROTOCOL_VERSION, value: 0.0.1}, {id: N1, tag: NOTE}]}, {id: N1, tag: NOTE, value: GEDCOM_CONTENT_DESCRIPTIONGEDCOM_CONTENT_DESCRIPTION}, {id: P1, tag: PLACE, children: [{tag: STREET, value: ADDRESS_LINE - ADDRESS_LINE1 - ADDRESS_LINE2 - ADDRESS_LINE3}, {tag: CITY, value: ADDRESS_CITY}, {tag: STATE, value: ADDRESS_STATE}, {tag: POSTAL_CODE, value: ADDRESS_POSTAL_CODE}, {tag: COUNTRY, value: ADDRESS_COUNTRY}, {tag: PHONE, value: PHONE_NUMBER1}, {tag: PHONE, value: PHONE_NUMBER2}, {tag: FAX, value: ADDRESS_FAX1}, {tag: FAX, value: ADDRESS_FAX2}, {tag: EMAIL, value: ADDRESS_EMAIL1}, {tag: EMAIL, value: ADDRESS_EMAIL2}, {tag: WWW, value: ADDRESS_WEB_PAGE1}, {tag: WWW, value: ADDRESS_WEB_PAGE2}]}]", root.toString());

		t.from(root);

		Assertions.assertEquals("children: [{tag: HEAD, children: [{tag: SOUR, value: APPROVED_SYSTEM_ID, children: [{tag: VERS, value: VERSION_NUMBER}, {tag: NAME, value: NAME_OF_PRODUCT}, {tag: CORP, value: NAME_OF_BUSINESS, children: [{tag: ADDR, value: ADDRESS_LINE - ADDRESS_LINE1 - ADDRESS_LINE2 - ADDRESS_LINE3, children: [{tag: CITY, value: ADDRESS_CITY}, {tag: STAE, value: ADDRESS_STATE}, {tag: POST, value: ADDRESS_POSTAL_CODE}, {tag: CTRY, value: ADDRESS_COUNTRY}]}, {tag: PHON, value: PHONE_NUMBER1}, {tag: PHON, value: PHONE_NUMBER2}, {tag: FAX, value: ADDRESS_FAX1}, {tag: FAX, value: ADDRESS_FAX2}, {tag: EMAIL, value: ADDRESS_EMAIL1}, {tag: EMAIL, value: ADDRESS_EMAIL2}, {tag: WWW, value: ADDRESS_WEB_PAGE1}, {tag: WWW, value: ADDRESS_WEB_PAGE2}]}]}, {id: SUBM1, tag: SUBM}, {tag: COPR, value: COPYRIGHT_GEDCOM_FILE}, {tag: CHAR, value: CHARACTER_SET}, {tag: GEDC, children: [{tag: VERS, value: 5.5.1}, {tag: FORM, value: LINEAGE-LINKED}]}, {tag: NOTE, value: GEDCOM_CONTENT_DESCRIPTIONGEDCOM_CONTENT_DESCRIPTION}]}]", root.toString());
	}

	private GedcomNode composeGedcomName(){
		final GedcomNode parent = GedcomNode.createEmpty();

		final GedcomNode name = GedcomNode.create(1, "NAME")
			.withValue("NAME_PERSONAL /SURNAME_PERSONAL/");
		name.addChild(GedcomNode.create(1, "TYPE")
			.withValue("NAME_TYPE"));
		name.addChild(GedcomNode.create(1, "NPFX")
			.withValue("NAME_PIECE_PREFIX"));
		name.addChild(GedcomNode.create(1, "GIVN")
			.withValue("NAME_PIECE_GIVEN"));
		name.addChild(GedcomNode.create(1, "NICK")
			.withValue("NAME_PIECE_NICKNAME"));
		name.addChild(GedcomNode.create(1, "SPFX")
			.withValue("NAME_PIECE_SURNAME_PREFIX"));
		name.addChild(GedcomNode.create(1, "SURN")
			.withValue("NAME_PIECE_SURNAME"));
		name.addChild(GedcomNode.create(1, "NSFX")
			.withValue("NAME_PIECE_SUFFIX"));
		name.addChild(GedcomNode.create(1, "NOTE")
			.withID("N1"));
		final GedcomNode note = GedcomNode.create(1, "NOTE")
			.withValue("SUBMITTER_TEXT");
		note.addChild(GedcomNode.create(2, "CONC")
			.withValue("SUBMITTER_TEXT1"));
		name.addChild(note);
		GedcomNode source = GedcomNode.create(1, "SOUR")
			.withID("S1");
		source.addChild(GedcomNode.create(2, "PAGE")
			.withValue("WHERE_WITHIN_SOURCE"));
		source.addChild(GedcomNode.create(2, "EVEN")
			.withValue("EVENT_TYPE_CITED_FROM"));
		source.addChild(GedcomNode.create(2, "ROLE")
			.withValue("ROLE_IN_EVENT"));
		GedcomNode sourceData = GedcomNode.create(2, "DATA");
		sourceData.addChild(GedcomNode.create(3, "DATE")
			.withValue("ENTRY_RECORDING_DATE"));
		GedcomNode sourceDataText = GedcomNode.create(2, "TEXT")
			.withValue("TEXT_FROM_SOURCE");
		sourceDataText.addChild(GedcomNode.create(3, "CONC")
			.withValue("TEXT_FROM_SOURCE"));
		sourceData.addChild(sourceDataText);
		source.addChild(sourceData);
//TODO		+1 <<MULTIMEDIA_LINK>>    {0:M}	/* A list of MULTIMEDIA_LINK() objects */
//TODO		+1 <<NOTE_STRUCTURE>>    {0:M}	/* A list of NOTE_STRUCTURE() objects. */
		source.addChild(GedcomNode.create(2, "QUAY")
			.withValue("CERTAINTY_ASSESSMENT"));
		name.addChild(source);

		source = GedcomNode.create(1, "SOUR")
			.withValue("SOURCE_DESCRIPTION");
		source.addChild(GedcomNode.create(2, "CONC")
			.withValue("SOURCE_DESCRIPTION"));
		GedcomNode sourceText = GedcomNode.create(2, "TEXT")
			.withValue("TEXT_FROM_SOURCE");
		sourceText.addChild(GedcomNode.create(3, "CONC")
			.withValue("TEXT_FROM_SOURCE"));
		source.addChild(sourceText);
//TODO		+1 <<MULTIMEDIA_LINK>>    {0:M}	/* A list of MULTIMEDIA_LINK() objects */
//TODO		+1 <<NOTE_STRUCTURE>>    {0:M}	/* A list of NOTE_STRUCTURE() objects. */
		source.addChild(GedcomNode.create(2, "QUAY")
			.withValue("CERTAINTY_ASSESSMENT"));

		GedcomNode phone = GedcomNode.create(1, "FONE")
			.withValue("NAME_PHONETIC_VARIATION1");
		//TODO			+2 <<PERSONAL_NAME_PIECES>>    {0:1}	/* A list of PERSONAL_NAME_PIECES() objects giving the components of the phonetic name variations.
		phone.addChild(GedcomNode.create(2, "TYPE"));
		name.addChild(phone);
		phone = GedcomNode.create(1, "FONE")
			.withValue("NAME_PHONETIC_VARIATION1");
		//TODO			+2 <<PERSONAL_NAME_PIECES>>    {0:1}	/* A list of PERSONAL_NAME_PIECES() objects giving the components of the phonetic name variations.
		phone.addChild(GedcomNode.create(2, "TYPE"));
		name.addChild(phone);

		GedcomNode romanized = GedcomNode.create(1, "FONE")
			.withValue("NAME_ROMANIZED_VARIATION");
		//TODO			+2 <<PERSONAL_NAME_PIECES>>    {0:1}	/* A list of PERSONAL_NAME_PIECES() objects giving the components of the phonetic name variations.
		romanized.addChild(GedcomNode.create(2, "TYPE"));
		name.addChild(romanized);
		romanized = GedcomNode.create(1, "FONE")
			.withValue("NAME_ROMANIZED_VARIATION");
		//TODO			+2 <<PERSONAL_NAME_PIECES>>    {0:1}	/* A list of PERSONAL_NAME_PIECES() objects giving the components of the phonetic name variations.
		romanized.addChild(GedcomNode.create(2, "TYPE"));
		name.addChild(romanized);
		name.addChild(source);

		addNode(name, parent);

		return parent;
	}

}