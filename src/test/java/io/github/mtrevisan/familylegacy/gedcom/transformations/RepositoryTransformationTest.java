package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class RepositoryTransformationTest{

	@Test
	void to(){
		final GedcomNode repository = GedcomNode.create("REPO")
			.withID("R1")
			.addChildValue("NAME", "NAME_OF_REPOSITORY")
			.addChild(GedcomNode.create("ADDR")
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
			.addChildReference("NOTE", "N1")
			.addChildValue("NOTE", "SUBMITTER_TEXT");
		final GedcomNode note = GedcomNode.create("NOTE", "N1", "SUBMITTER_TEXT");
		final Gedcom origin = new Gedcom();
		origin.addRepository(repository);
		origin.addNote(note);
		final Flef destination = new Flef();

		Assertions.assertEquals("id: R1, tag: REPO, children: [{tag: NAME, value: NAME_OF_REPOSITORY}, {tag: ADDR, value: ADDRESS_LINE, children: [{tag: CONT, value: ADDRESS_LINE}, {tag: ADR1, value: ADDRESS_LINE1}, {tag: ADR2, value: ADDRESS_LINE2}, {tag: ADR3, value: ADDRESS_LINE3}, {tag: CITY, value: ADDRESS_CITY}, {tag: STAE, value: ADDRESS_STATE}, {tag: POST, value: ADDRESS_POSTAL_CODE}, {tag: CTRY, value: ADDRESS_COUNTRY}]}, {tag: PHON, value: PHONE_NUMBER1}, {tag: PHON, value: PHONE_NUMBER2}, {tag: EMAIL, value: ADDRESS_EMAIL1}, {tag: EMAIL, value: ADDRESS_EMAIL2}, {tag: FAX, value: ADDRESS_FAX1}, {tag: FAX, value: ADDRESS_FAX2}, {tag: WWW, value: ADDRESS_WEB_PAGE1}, {tag: WWW, value: ADDRESS_WEB_PAGE2}, {id: N1, tag: NOTE}, {tag: NOTE, value: SUBMITTER_TEXT}]", origin.getRepositories().get(0).toString());
		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXT", origin.getNotes().get(0).toString());

		final Transformation<Gedcom, Flef> t = new RepositoryTransformation();
		t.to(origin, destination);

		Assertions.assertEquals("id: R1, tag: REPOSITORY, children: [{tag: NAME, value: NAME_OF_REPOSITORY}, {id: P1, tag: PLACE}, {tag: CONTACT, children: [{tag: PHONE, value: PHONE_NUMBER1}, {tag: PHONE, value: PHONE_NUMBER2}, {tag: EMAIL, value: ADDRESS_EMAIL1}, {tag: EMAIL, value: ADDRESS_EMAIL2}, {tag: PHONE, value: ADDRESS_FAX1, children: [{tag: TYPE, value: fax}]}, {tag: PHONE, value: ADDRESS_FAX2, children: [{tag: TYPE, value: fax}]}, {tag: URL, value: ADDRESS_WEB_PAGE1}, {tag: URL, value: ADDRESS_WEB_PAGE2}]}, {id: N1, tag: NOTE}, {id: N1, tag: NOTE}]", destination.getRepositories().get(0).toString());
		Assertions.assertEquals("id: P1, tag: PLACE, children: [{tag: ADDRESS, value: ADDRESS_LINE, children: [{tag: CONC, value: ADDRESS_LINE - ADDRESS_LINE1 - ADDRESS_LINE2 - ADDRESS_LINE3}]}, {tag: CITY, value: ADDRESS_CITY}, {tag: STATE, value: ADDRESS_STATE}, {tag: COUNTRY, value: ADDRESS_COUNTRY}]", destination.getPlaces().get(0).toString());
		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXT", destination.getNotes().get(0).toString());
	}

	@Test
	void from() throws GedcomGrammarParseException{
		final GedcomNode repository = GedcomNode.create("REPOSITORY")
			.withID("R1")
			.addChildValue("NAME", "NAME_OF_REPOSITORY")
			.addChildReference("INDIVIDUAL", "I1")
			.addChildReference("PLACE", "P1")
			.addChild(GedcomNode.create("CONTACT")
				.addChild(GedcomNode.create("PHONE")
					.withValue("PHONE_NUMBER")
					.addChildValue("TYPE", "personal")
					.addChildValue("CALLED_ID", "CALLED_ID_VALUE1")
					.addChildReference("NOTE", "N1")
					.addChildValue("RESTRICTION", "private")
				)
				.addChild(GedcomNode.create("EMAIL")
					.withValue("ADDRESS_EMAIL")
					.addChildValue("CALLED_ID", "CALLED_ID_VALUE2")
					.addChildReference("NOTE", "N1")
					.addChildValue("RESTRICTION", "private")
				)
				.addChild(GedcomNode.create("URL")
					.withValue("ADDRESS_WEB_PAGE")
					.addChildValue("TYPE", "blog")
					.addChildReference("NOTE", "N1")
				)
			)
			.addChildReference("NOTE", "N1");
		final GedcomNode place = GedcomNode.create("PLACE")
			.withID("P1")
			.addChild(GedcomNode.create("ADDRESS")
				.addChildValue("CITY", "ADDRESS_CITY")
				.addChildValue("STATE", "ADDRESS_STATE")
			);
		final Flef origin = new Flef();
		origin.addRepository(repository);
		origin.addPlace(place);
		final Gedcom destination = new Gedcom();

		Assertions.assertEquals("id: R1, tag: REPOSITORY, children: [{tag: NAME, value: NAME_OF_REPOSITORY}, {id: I1, tag: INDIVIDUAL}, {id: P1, tag: PLACE}, {tag: CONTACT, children: [{tag: PHONE, value: PHONE_NUMBER, children: [{tag: TYPE, value: personal}, {tag: CALLED_ID, value: CALLED_ID_VALUE1}, {id: N1, tag: NOTE}, {tag: RESTRICTION, value: private}]}, {tag: EMAIL, value: ADDRESS_EMAIL, children: [{tag: CALLED_ID, value: CALLED_ID_VALUE2}, {id: N1, tag: NOTE}, {tag: RESTRICTION, value: private}]}, {tag: URL, value: ADDRESS_WEB_PAGE, children: [{tag: TYPE, value: blog}, {id: N1, tag: NOTE}]}]}, {id: N1, tag: NOTE}]", origin.getRepositories().get(0).toString());
		Assertions.assertEquals("id: P1, tag: PLACE, children: [{tag: ADDRESS, children: [{tag: CITY, value: ADDRESS_CITY}, {tag: STATE, value: ADDRESS_STATE}]}]", origin.getPlaces().get(0).toString());

		final Transformation<Gedcom, Flef> t = new RepositoryTransformation();
		t.from(origin, destination);

		Assertions.assertEquals("id: R1, tag: REPO, children: [{tag: NAME, value: NAME_OF_REPOSITORY}, {tag: ADDR, children: [{tag: CITY, value: ADDRESS_CITY}, {tag: STAE, value: ADDRESS_STATE}]}, {tag: PHONE, value: PHONE_NUMBER}, {tag: EMAIL, value: ADDRESS_EMAIL}, {tag: WWW, value: ADDRESS_WEB_PAGE}, {id: N1, tag: NOTE}]", destination.getRepositories().get(0).toString());
	}

}