/**
 * Copyright (c) 2026 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.gedcom;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.util.SoftwareInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;


class GEDCOMToFLEFConverterTest{

	private static final String GEDCOM = """
		0 HEAD
		1 SOUR TCGB
		2 NAME The Complete Genealogy Builder
		2 VERS 1.0.0.0
		1 DATE 14 OCT 2026
		2 TIME 20:51:22
		1 SUBM @U1@
		1 CHAR UTF-8
		0 @U1@ SUBM
		1 NAME John Doe
		1 ADDR Main street 1
		1 EMAIL john@@example.com
		0 @R1@ REPO
		1 NAME Parish archive
		1 PHON 555-1234
		1 NOTE An archive
		0 @M1@ OBJE
		1 FILE photos/register.jpg
		2 FORM jpg
		2 TITL Register page
		1 _DATE 5 MAY 1900
		1 SOUR @S1@
		0 @S1@ SOUR
		1 TITL Baptism register
		1 ABBR BR
		1 AUTH Parish priest
		1 REPO @R1@
		2 CALN 12/A
		3 MEDI Book
		0 @I1@ INDI
		1 NAME Emilio /Trevisan/
		2 NPFX Dr.
		2 GIVN Emilio
		2 NICK Milio /ˈmi.ljo/
		2 SPFX de
		2 SURN Trevisan
		2 SOUR @S1@
		3 PAGE #30
		1 SEX M
		1 BIRT Y
		2 DATE 19 NOV 1926
		2 PLAC Treviso, Treviso, Italia
		3 MAP
		4 LATI N45.666
		4 LONG E12.245
		1 CHR
		2 DATE 21 NOV 1926
		2 PLAC Treviso, Italia
		2 SOUR @S1@
		3 PAGE #31
		3 DATA
		4 DATE 21 NOV 1926
		4 TEXT Baptized
		3 QUAY 3
		3 NOTE A citation note
		2 SOUR A family bible
		1 EVEN Dorato /do.ˈra.to/
		2 TYPE Family nickname
		1 EVEN
		2 TYPE Family Address
		2 DATE BEF 2 APR 1956
		2 ADDR
		3 ADR1 via Roma 1
		3 CITY Treviso
		1 EVEN
		2 TYPE Military Service
		2 DATE FROM 1945 TO 1946
		1 OCCU
		2 TYPE stuccatore
		1 OBJE
		2 FILE photos/emilio.jpg
		2 FORM jpg
		2 _PRIMARY Y
		2 _CUTD 1 2 30 40
		1 OBJE @M1@
		1 FAMC @F2@
		2 PEDI adopted
		1 FAMS @F1@
		1 ASSO @I3@
		2 RELA Godfather
		1 ALIA Milio /Dorato/
		1 NOTE First line,
		2 SOUR @S1@
		2 CONC  continued
		0 @I2@ INDI
		1 NAME Maria /Rossi/
		1 SEX F
		1 FAMS @F1@
		0 @I3@ INDI
		1 NAME Giovanni /Bianchi/
		1 SEX M
		1 FAMS @F2@
		0 @I4@ INDI
		1 NAME Anna /Verdi/
		1 SEX F
		1 FAMS @F2@
		0 @F1@ FAM
		1 HUSB @I1@
		1 WIFE @I2@
		1 MARR
		2 TYPE religious
		2 DATE 14 FEB 1953
		2 HUSB
		3 AGE 26y
		1 DIV
		2 DATE 1970
		0 @F2@ FAM
		1 HUSB @I3@
		1 WIFE @I4@
		1 CHIL @I1@
		0 TRLR
		""";


	/** A child born to I1 and I2, adopted by I3, the new husband of its mother. */
	private static final String GEDCOM_ADOPTION_BY_STEPFATHER = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Father /Biological/
		1 FAMS @F1@
		0 @I2@ INDI
		1 NAME Mother /Biological/
		1 FAMS @F1@
		1 FAMS @F2@
		0 @I3@ INDI
		1 NAME Father /Adoptive/
		1 FAMS @F2@
		0 @I4@ INDI
		1 NAME Child /Biological/
		1 FAMC @F1@
		2 PEDI birth
		1 FAMC @F2@
		2 PEDI adopted
		1 ADOP Y
		2 FAMC @F2@
		3 ADOP BOTH
		0 @F1@ FAM
		1 HUSB @I1@
		1 WIFE @I2@
		1 CHIL @I4@
		0 @F2@ FAM
		1 HUSB @I3@
		1 WIFE @I2@
		1 CHIL @I4@
		0 TRLR
		""";


	/** An adoption written twice: by the family of the child (FAMC, PEDI adopted) and by an association of the adoptive father. */
	private static final String GEDCOM_ADOPTION_WRITTEN_TWICE = """
		0 HEAD
		1 CHAR UTF-8
		0 @I2@ INDI
		1 NAME Mother /Biological/
		1 FAMS @F2@
		0 @I3@ INDI
		1 NAME Father /Adoptive/
		1 FAMS @F2@
		1 ASSO @I4@
		2 RELA adoptive child
		2 NOTE Told by the neighbours
		0 @I4@ INDI
		1 NAME Child /Adopted/
		1 FAMC @F2@
		2 PEDI adopted
		0 @F2@ FAM
		1 HUSB @I3@
		1 WIFE @I2@
		1 CHIL @I4@
		0 TRLR
		""";


	/** I1 and I2 are friends, each says it of the other; I3 is the guardian of I4, who is the ward of I3. */
	private static final String GEDCOM_ASSOCIATIONS_FROM_BOTH_SIDES = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME One /First/
		1 ASSO @I2@
		2 RELA Friend
		0 @I2@ INDI
		1 NAME Two /Second/
		1 ASSO @I1@
		2 RELA friend
		2 NOTE Also told by the second one
		0 @I3@ INDI
		1 NAME Three /Third/
		1 ASSO @I4@
		2 RELA Ward
		0 @I4@ INDI
		1 NAME Four /Fourth/
		1 ASSO @I3@
		2 RELA Guardian
		0 TRLR
		""";


	private static FLEFModel convert() throws IOException{
		return convert(GEDCOM);
	}

	private static FLEFModel convert(final String gedcom) throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(gedcom));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	/** The types of the relationships of a child with a parent. */
	private static List<String> childTypes(final FLEFModel model, final String childId, final String parentId){
		return model.getRecordsByType("relationship").stream()
			.filter(relationship -> childId.equals(FLEFRecordHelper.getChildValue(relationship, "subject.individual"))
				&& parentId.equals(FLEFRecordHelper.getChildValue(relationship, "target.individual")))
			.map(relationship -> FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_TYPE))
			.toList();
	}

	private static List<FLEFRecord> relationships(final FLEFModel model, final String type){
		return model.getRecordsByType("relationship").stream()
			.filter(relationship -> type.equals(FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_TYPE)))
			.toList();
	}

	private static FLEFRecord eventOfType(final FLEFModel model, final String type){
		return model.getRecordsByType(EventReader.TYPE).stream()
			.filter(event -> type.equals(FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE)))
			.findFirst()
			.orElse(null);
	}

	private static FLEFRecord attributeOfType(final FLEFModel model, final String type){
		return model.getRecordsByType("individual_attribute").stream()
			.filter(attribute -> type.equals(FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_TYPE)))
			.findFirst()
			.orElse(null);
	}


	@Test
	void validAgainstTheSchema() throws IOException{
		final FLEFModel model = convert();

		//the grammar is in a jar (Domain): it is read as a stream, a path into a jar cannot be built without opening its file system
		final FLEFGrammar grammar;
		try(final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("gedg/flef_0.2.0.gedg")){
			grammar = FLEFGrammarParser.parse(stream);
		}
		final FLEFValidator validator = new FLEFValidator(grammar);
		Assertions.assertEquals(List.of(), validator.validateSchema(model));
		Assertions.assertEquals(List.of(), validator.validateIntegrity(model));
	}

	@Test
	void header() throws IOException{
		final FLEFRecord header = convert().getHeader();

		Assertions.assertEquals("Family LEgacy Format", FLEFRecordHelper.getChildValue(header, "protocol.name"));
		Assertions.assertEquals("Family Legacy", FLEFRecordHelper.getChildValue(header, "source.name"), "the software that writes the file is this one");
		Assertions.assertEquals(SoftwareInfo.version(), FLEFRecordHelper.getChildValue(header, "source.version"));
		Assertions.assertEquals("Mauro Trevisan", FLEFRecordHelper.getChildValue(header, "source.organization"));
		Assertions.assertEquals("2026-10-14T20:51:22Z", FLEFRecordHelper.getChildValue(header, "date"));
		final List<FLEFRecord> contacts = FLEFRecordHelper.findChildren(header, "submitter.contact");
		Assertions.assertEquals(List.of("Main street 1", "john@example.com"), contacts.stream()
			.map(contact -> FLEFRecordHelper.getChildValue(contact, "value"))
			.toList());
		Assertions.assertEquals("John Doe", FLEFRecordHelper.getChildValue(contacts.getFirst(), "name.value"));
	}

	@Test
	void namePartsInDisplayOrder() throws IOException{
		final FLEFRecord individual = convert().getRecordById("I1");
		final FLEFRecord name = FLEFRecordHelper.findChildren(individual, IndividualReader.TAG_NAME).getFirst();

		final List<FLEFRecord> parts = FLEFRecordHelper.findChildren(name, "part");
		Assertions.assertEquals(List.of("prefix", "given", "nickname", "family_prefix", "family"), parts.stream()
			.map(part -> FLEFRecordHelper.getChildValue(part, NameReader.TAG_PART_TYPE))
			.toList());
		Assertions.assertEquals("de", FLEFRecordHelper.getChildValue(parts.get(3), "value"));
		Assertions.assertEquals("Trevisan", FLEFRecordHelper.getChildValue(parts.get(4), "value"));
		Assertions.assertEquals("Milio", FLEFRecordHelper.getChildValue(parts.get(2), "value"));
		Assertions.assertEquals("/ˈmi.ljo/", FLEFRecordHelper.getChildValue(parts.get(2), "variant.phonetic.value"));
		// the citation of the name belongs to the name
		Assertions.assertEquals("S1", FLEFRecordHelper.getChildValue(name, "source.source"));
		Assertions.assertTrue(FLEFRecordHelper.findChildren(individual, SourceCitationReader.TAG_SOURCE).stream()
			.noneMatch(citation -> "#30".equals(FLEFRecordHelper.getChildValue(citation, "locator"))));
	}

	@Test
	void factsThatAreNames() throws IOException{
		final FLEFRecord individual = convert().getRecordById("I1");

		final FLEFRecord familyNickname = FLEFRecordHelper.findChildren(individual, IndividualReader.TAG_NAME).stream()
			.filter(name -> "family_nickname".equals(FLEFRecordHelper.getChildValue(name, "part[0].type")))
			.findFirst()
			.orElseThrow();
		Assertions.assertEquals("Dorato", FLEFRecordHelper.getChildValue(familyNickname, "part.value"));
		Assertions.assertEquals("/do.ˈra.to/", FLEFRecordHelper.getChildValue(familyNickname, "part.variant.phonetic.value"));

		final FLEFRecord alias = FLEFRecordHelper.findChildren(individual, IndividualReader.TAG_NAME).stream()
			.filter(name -> "alias".equals(FLEFRecordHelper.getChildValue(name, NameReader.TAG_TYPE)))
			.findFirst()
			.orElseThrow();
		Assertions.assertEquals("Dorato", FLEFRecordHelper.getChildValue(alias, "part[1].value"));
	}

	@Test
	void eventsAndAttributes() throws IOException{
		final FLEFModel model = convert();

		Assertions.assertNotNull(eventOfType(model, "christening"));
		final FLEFRecord military = eventOfType(model, "military_service");
		Assertions.assertEquals("1946", FLEFRecordHelper.getChildValue(military, "date.value.spanning.to.value.full_date"));

		final FLEFRecord residence = attributeOfType(model, "residence");
		Assertions.assertEquals("2 APR 1956", FLEFRecordHelper.getChildValue(residence,
			"valid_from.value.bounded.not_after.value.full_date"));
		Assertions.assertNotNull(FLEFRecordHelper.getChildValue(residence, "place.place"));

		final FLEFRecord occupation = attributeOfType(model, "occupation");
		Assertions.assertEquals("stuccatore", FLEFRecordHelper.getChildValue(occupation, "value"));

		// an attribute is not an event: nobody participates in it
		Assertions.assertTrue(model.getRecordsByType(EventParticipationReader.TYPE).stream()
			.allMatch(participation -> EventReader.TYPE.equalsIgnoreCase(model.getRecordById(
				FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT)).getTag())));
	}

	@Test
	void sourceCitation() throws IOException{
		final FLEFModel model = convert();
		final FLEFRecord christening = eventOfType(model, "christening");

		final List<FLEFRecord> citations = FLEFRecordHelper.findChildren(christening, SourceCitationReader.TAG_SOURCE);
		final FLEFRecord citation = citations.getFirst();
		Assertions.assertEquals("S1", FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_SOURCE));
		Assertions.assertEquals("#31", FLEFRecordHelper.getChildValue(citation, "locator"));
		Assertions.assertEquals("Baptized", FLEFRecordHelper.getChildValue(citation, "extract.text"));
		Assertions.assertEquals("A citation note", FLEFRecordHelper.getChildValue(citation, "extract.remark"));
		Assertions.assertEquals("primary", FLEFRecordHelper.getChildValue(citation, "evidence.information_type"));

		// the entry date of the citation is the creation date of the source (and no note is created for it)
		final FLEFRecord source = model.getRecordById("S1");
		Assertions.assertNotNull(FLEFRecordHelper.findChild(source, "date"));
		Assertions.assertNull(FLEFRecordHelper.findChild(christening, "note"));
		Assertions.assertNull(FLEFRecordHelper.findChild(source, "evidence"));

		// a source description is the title of an unrecorded source
		final FLEFRecord bible = model.getRecordById(FLEFRecordHelper.getChildValue(citations.get(1), SourceCitationReader.TAG_SOURCE));
		Assertions.assertEquals("A family bible", FLEFRecordHelper.getChildValue(bible, "title.value"));
	}

	@Test
	void sourceAndRepository() throws IOException{
		final FLEFModel model = convert();

		final FLEFRecord source = model.getRecordById("S1");
		Assertions.assertEquals(List.of("Baptism register", "BR"), FLEFRecordHelper.findChildren(source, SourceReader.TAG_TITLE).stream()
			.map(title -> FLEFRecordHelper.getChildValue(title, "value"))
			.toList());
		Assertions.assertEquals("abbreviated", FLEFRecordHelper.getChildValue(source, "title[1].type"));
		Assertions.assertEquals("Parish priest", FLEFRecordHelper.getChildValue(source, "author.text"));
		Assertions.assertEquals("book", FLEFRecordHelper.getChildValue(source, "media_type"));
		Assertions.assertEquals("R1", FLEFRecordHelper.getChildValue(source, "repository.repository"));
		Assertions.assertEquals("12/A", FLEFRecordHelper.getChildValue(source, "repository.locator"));
		// OBJE.SOUR: the media belongs to the source
		Assertions.assertEquals("M1", FLEFRecordHelper.getChildValue(source, SourceCitationReader.TAG_MEDIA));

		final FLEFRecord repository = model.getRecordById("R1");
		Assertions.assertEquals("555-1234", FLEFRecordHelper.getChildValue(repository, "contact.value"));
		Assertions.assertEquals("phone", FLEFRecordHelper.getChildValue(repository, "contact.type"));
		Assertions.assertEquals("An archive", FLEFRecordHelper.getChildValue(repository, "note.text"));
	}

	@Test
	void multimedia() throws IOException{
		final FLEFModel model = convert();
		final FLEFRecord individual = model.getRecordById("I1");

		final FLEFRecord photo = model.getRecordById(FLEFRecordHelper.getChildValue(individual, "preferred_image.media"));
		Assertions.assertEquals("photos/emilio.jpg", FLEFRecordHelper.getChildValue(photo, "uri"));
		Assertions.assertEquals("image", FLEFRecordHelper.getChildValue(photo, "kind"));
		Assertions.assertEquals("30", FLEFRecordHelper.getChildValue(individual, "preferred_image.crop.width"));

		final FLEFRecord media = model.getRecordById("M1");
		Assertions.assertTrue(MediaReader.TYPE.equalsIgnoreCase(media.getTag()));
		Assertions.assertEquals("photos/register.jpg", FLEFRecordHelper.getChildValue(media, "uri"));
		Assertions.assertEquals("image", FLEFRecordHelper.getChildValue(media, "kind"));
		Assertions.assertEquals("Register page", FLEFRecordHelper.getChildValue(media, "description"));
		Assertions.assertEquals("5 MAY 1900", FLEFRecordHelper.getChildValue(media, "date.value.point.value.full_date"));
	}

	@Test
	void sameFileTwiceIsOneMedia() throws IOException{
		final FLEFModel model = convert("""
			0 HEAD
			1 CHAR UTF-8
			0 @M1@ OBJE
			1 FILE photos/a.jpg
			1 _DATE 13 FEB 2011
			0 @M2@ OBJE
			1 FILE photos/a.jpg
			1 SOUR @S1@
			0 @S1@ SOUR
			1 TITL Register
			0 TRLR
			""");

		Assertions.assertNotNull(model.getRecordById("M1"));
		Assertions.assertFalse(model.hasRecord("M2"));
		Assertions.assertEquals("13 FEB 2011", FLEFRecordHelper.getChildValue(model.getRecordById("M1"), "date.value.point.value.full_date"));
		Assertions.assertEquals("M1", FLEFRecordHelper.getChildValue(model.getRecordById("S1"), SourceReader.TAG_MEDIA));
	}

	@Test
	void sameFileInLinksWithDifferentDataIsOneMedia() throws IOException{
		final FLEFModel model = convert("""
			0 HEAD
			1 CHAR UTF-8
			0 @I1@ INDI
			1 NAME Emilio /Trevisan/
			1 OBJE
			2 FILE photos/a.jpg
			2 _DATE 13 FEB 2011
			1 OBJE
			2 FILE photos/a.jpg
			0 TRLR
			""");

		final List<FLEFRecord> media = model.getRecordsByType(MediaReader.TYPE);
		Assertions.assertEquals(1, media.size());
		Assertions.assertEquals("13 FEB 2011", FLEFRecordHelper.getChildValue(media.getFirst(), "date.value.point.value.full_date"));
	}

	@Test
	void sourcesWithTheSameTitleAndMediaAreOneSource() throws IOException{
		final FLEFModel model = convert("""
			0 HEAD
			1 CHAR UTF-8
			0 @M1@ OBJE
			1 FILE photos/a.jpg
			0 @S1@ SOUR
			1 TITL naturalization
			1 DATA
			2 EVEN BIRT
			3 DATE 3 AUG 1931
			1 OBJE @M1@
			0 @S2@ SOUR
			1 TITL naturalization
			1 OBJE @M1@
			0 @S3@ SOUR
			1 TITL naturalization
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 SOUR @S2@
			2 PAGE 3
			0 TRLR
			""");

		Assertions.assertNotNull(model.getRecordById("S1"));
		Assertions.assertFalse(model.hasRecord("S2"));
		//a source without media is not the same source: there is nothing that says so
		Assertions.assertTrue(model.hasRecord("S3"));
		//the citations of the merged source cite the survivor
		Assertions.assertEquals(2, model.getRecordsByType("source").size());
		Assertions.assertEquals("S1", FLEFRecordHelper.getChildValue(model.getRecordById("I1"), "assertion.source.source") != null
			? FLEFRecordHelper.getChildValue(model.getRecordById("I1"), "assertion.source.source")
			: FLEFRecordHelper.getChildValue(model.getRecordById("I1"), "source.source"));
	}

	@Test
	void anAssociationNamedAsAnEventIsAWitnessOfThatEvent() throws IOException{
		final FLEFModel model = convert("""
			0 HEAD
			1 CHAR UTF-8
			0 @I1@ INDI
			1 NAME Argentino /Mazutti/
			1 ASSO @I2@
			2 RELA birth
			1 ASSO @I2@
			2 RELA godfather
			1 ASSO @I3@
			2 RELA birth
			0 @I2@ INDI
			1 NAME Zelia /Masutti/
			1 BIRT
			2 DATE 1 JAN 1900
			0 @I3@ INDI
			1 NAME Anna /Rossi/
			0 TRLR
			""");

		final List<FLEFRecord> witnesses = model.getRecordsByType("event_participation").stream()
			.filter(p -> "witness".equals(FLEFRecordHelper.getChildValue(p, "role")))
			.toList();
		//the birth of Zelia has a witness: Argentino (and there is no association for it)
		Assertions.assertEquals(1, witnesses.size());
		Assertions.assertEquals("I1", FLEFRecordHelper.getChildValue(witnesses.getFirst(), "participant.individual"));
		final List<FLEFRecord> associations = model.getRecordsByType("relationship");
		//godfather stays an association, and so does the birth of Anna, who has no birth recorded
		Assertions.assertEquals(2, associations.size());
	}

	@Test
	void theSpouseOfAParentIsAStepParentOfTheChildrenOfAnotherFamily() throws IOException{
		final FLEFModel model = convert("""
			0 HEAD
			1 CHAR UTF-8
			0 @I1@ INDI
			1 NAME Lorenzo /Masutto/
			1 FAMS @F1@
			1 FAMS @F2@
			0 @I2@ INDI
			1 NAME Anna /Rossi/
			1 FAMS @F1@
			0 @I3@ INDI
			1 NAME Zuanne /Masutto/
			1 BIRT
			2 DATE 1700
			1 FAMC @F1@
			0 @I4@ INDI
			1 NAME Pasqua /Piazarola/
			1 FAMS @F2@
			0 @I5@ INDI
			1 NAME Late /Masutto/
			1 BIRT
			2 DATE 1750
			1 FAMC @F1@
			0 @F1@ FAM
			1 HUSB @I1@
			1 WIFE @I2@
			1 CHIL @I3@
			1 CHIL @I5@
			0 @F2@ FAM
			1 HUSB @I1@
			1 WIFE @I4@
			1 MARR
			2 DATE 1720
			0 TRLR
			""");

		final List<FLEFRecord> steps = model.getRecordsByType("relationship").stream()
			.filter(r -> "step_child".equals(FLEFRecordHelper.getChildValue(r, "type")))
			.toList();
		//Zuanne (born before the marriage) is a step child of Pasqua; the one born after is not derived, as she may be the mother
		Assertions.assertEquals(1, steps.size());
		Assertions.assertEquals("I3", FLEFRecordHelper.getChildValue(steps.getFirst(), "subject.individual"));
		Assertions.assertEquals("I4", FLEFRecordHelper.getChildValue(steps.getFirst(), "target.individual"));
	}

	@Test
	void mediaKind(){
		Assertions.assertEquals("image", GEDCOMMapper.mapMediaKind("photo", "pdf", "scan.pdf"));
		Assertions.assertEquals("audio", GEDCOMMapper.mapMediaKind(null, "wav", null));
		Assertions.assertEquals("video", GEDCOMMapper.mapMediaKind(null, "video/mp4", null));
		Assertions.assertEquals("document", GEDCOMMapper.mapMediaKind("book", null, "C:\\scans\\register.pdf"));
		Assertions.assertEquals("image", GEDCOMMapper.mapMediaKind(null, null, "C:\\photos\\Trevisan Emilio.JPG"));
		Assertions.assertEquals("document", GEDCOMMapper.mapMediaKind(null, null, null));
	}

	@Test
	void places() throws IOException{
		final FLEFModel model = convert();
		final FLEFRecord birth = eventOfType(model, EventReader.ENUM_TYPE_BIRTH);
		final FLEFRecord christening = eventOfType(model, "christening");

		// the town of Treviso and the province of Treviso are different places
		final String town = FLEFRecordHelper.getChildValue(birth, "place.place");
		final String province = FLEFRecordHelper.getChildValue(christening, "place.place");
		Assertions.assertNotEquals(town, province);
		Assertions.assertTrue(model.getRecordsByType("place_relationship").stream()
			.anyMatch(relationship -> town.equals(FLEFRecordHelper.getChildValue(relationship, "subject.place"))
				&& province.equals(FLEFRecordHelper.getChildValue(relationship, "target.place"))));

		Assertions.assertEquals("+45.666+012.245/", FLEFRecordHelper.getChildValue(model.getRecordById(town),
			"location.coordinates"));
	}

	@Test
	void family() throws IOException{
		final FLEFModel model = convert();

		final FLEFRecord spouses = relationships(model, "religious_spouse").getFirst();
		Assertions.assertEquals("14 FEB 1953", FLEFRecordHelper.getChildValue(spouses, "valid_from.value.point.value.full_date"));
		Assertions.assertEquals("1970", FLEFRecordHelper.getChildValue(spouses, "valid_to.value.point.value.full_date"));
		Assertions.assertNull(FLEFRecordHelper.findChild(spouses, "status"));

		// FAMC.PEDI gives the nature of the relationship to the parents
		final List<FLEFRecord> adoptive = relationships(model, "adoptive_child");
		Assertions.assertEquals(2, adoptive.size());
		Assertions.assertTrue(relationships(model, "biological_child").isEmpty());

		final FLEFRecord marriage = eventOfType(model, "marriage");
		Assertions.assertTrue(model.getRecordsByType(EventParticipationReader.TYPE).stream()
			.anyMatch(participation -> marriage.getId().equals(FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT))
				&& "I1".equals(FLEFRecordHelper.getChildValue(participation, "participant.individual"))
				&& "26y".equals(FLEFRecordHelper.getChildValue(participation, "note.text"))));
	}

	@Test
	void adoptionByTheNewHusbandOfTheMother() throws IOException{
		final FLEFModel model = convert(GEDCOM_ADOPTION_BY_STEPFATHER);

		Assertions.assertEquals(List.of("biological_child"), childTypes(model, "I4", "I1"));
		// the mother is in the adoptive family too, but she stays the mother by birth
		Assertions.assertEquals(List.of("biological_child"), childTypes(model, "I4", "I2"));
		Assertions.assertEquals(List.of("adoptive_child"), childTypes(model, "I4", "I3"));
	}

	@Test
	void theSameAdoptionWrittenTwiceIsOneRelationshipAndKeepsWhatBothSay() throws IOException{
		final FLEFModel model = convert(GEDCOM_ADOPTION_WRITTEN_TWICE);

		Assertions.assertEquals(List.of("adoptive_child"), childTypes(model, "I4", "I3"));
		final FLEFRecord adoption = model.getRecordsByType("relationship").stream()
			.filter(relationship -> "I4".equals(FLEFRecordHelper.getChildValue(relationship, "subject.individual"))
				&& "I3".equals(FLEFRecordHelper.getChildValue(relationship, "target.individual")))
			.findFirst()
			.orElseThrow();
		//the note of the association is not lost
		Assertions.assertEquals("Told by the neighbours", FLEFRecordHelper.getChildValue(adoption, "note.text"));
		//and the mother of that family is written once too
		Assertions.assertEquals(List.of("adoptive_child"), childTypes(model, "I4", "I2"));
	}

	@Test
	void theSameAssociationWrittenFromBothSidesIsOneAndTwoRolesAreTwo() throws IOException{
		final FLEFModel model = convert(GEDCOM_ASSOCIATIONS_FROM_BOTH_SIDES);

		final List<FLEFRecord> associates = relationships(model, "associate");
		//the friends are one relationship, the guardian and the ward two
		Assertions.assertEquals(3, associates.size());
		final List<FLEFRecord> friends = associates.stream()
			.filter(relationship -> "friend".equalsIgnoreCase(FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_ROLE)))
			.toList();
		Assertions.assertEquals(1, friends.size());
		//and it keeps what the second one added
		Assertions.assertEquals("Also told by the second one", FLEFRecordHelper.getChildValue(friends.getFirst(), "note.text"));
	}

	@Test
	void association() throws IOException{
		final FLEFModel model = convert();

		// "I1 ASSO @I3@ RELA Godfather": I3 is the godfather of I1
		final FLEFRecord association = relationships(model, "associate").getFirst();
		Assertions.assertEquals("I3", FLEFRecordHelper.getChildValue(association, "subject.individual"));
		Assertions.assertEquals("I1", FLEFRecordHelper.getChildValue(association, "target.individual"));
		Assertions.assertEquals("Godfather", FLEFRecordHelper.getChildValue(association, EventParticipationReader.TAG_ROLE));
	}

	@Test
	void continuationOfTheSuperior() throws IOException{
		final FLEFRecord individual = convert().getRecordById("I1");

		Assertions.assertEquals("First line, continued", FLEFRecordHelper.getChildValue(individual, "note.text"));
	}

	@Test
	void repeatableConversion() throws IOException{
		final int records = convert().getRecordCount();

		Assertions.assertEquals(records, convert().getRecordCount());
	}


	/** Two couples; the sources of the first are titled by their kind, cited by both spouses (the way some programs write them). */
	private static final String GEDCOM_MARRIAGE_KINDS = """
		0 HEAD
		1 CHAR UTF-8
		0 @S1@ SOUR
		1 TITL marriage Rossi Mario + Bianchi Anna 18500112 religious
		0 @S2@ SOUR
		1 TITL marriage Verdi Luigi + Neri Maria 19100203 civil
		0 @S3@ SOUR
		1 TITL marriage Gialli Paolo + Blu Rosa 19200304 religious
		0 @I1@ INDI
		1 NAME Mario /Rossi/
		1 SOUR @S1@
		2 EVEN MARR
		0 @I2@ INDI
		1 NAME Anna /Bianchi/
		1 SOUR @S1@
		2 EVEN MARR
		0 @I3@ INDI
		1 NAME Luigi /Verdi/
		1 SOUR @S2@
		2 EVEN MARR
		0 @I4@ INDI
		1 NAME Maria /Neri/
		1 SOUR @S2@
		2 EVEN MARR
		0 @I5@ INDI
		1 NAME Paolo /Gialli/
		1 SOUR @S3@
		2 EVEN MARR
		0 @I6@ INDI
		1 NAME Rosa /Blu/
		0 @I7@ INDI
		1 NAME Carlo /Verdi/
		0 @I8@ INDI
		1 NAME Elena /Rosa/
		0 @F1@ FAM
		1 HUSB @I1@
		1 WIFE @I2@
		1 MARR Y
		0 @F2@ FAM
		1 HUSB @I3@
		1 WIFE @I4@
		1 MARR Y
		0 @F3@ FAM
		1 HUSB @I5@
		1 WIFE @I6@
		1 MARR Y
		0 @F4@ FAM
		1 HUSB @I7@
		1 WIFE @I8@
		1 MARR Y
		2 TYPE Family
		0 @F5@ FAM
		1 HUSB @I1@
		1 WIFE @I8@
		1 MARR Y
		2 NOTE Matrimonio religioso celebrato nella pieve
		0 TRLR
		""";

	@Test
	void theKindOfAMarriageComesFromWhatTheFileSaysExplicitly() throws IOException{
		final FLEFModel model = convert(GEDCOM_MARRIAGE_KINDS);

		//a source of the marriage cited by both spouses, titled by its kind
		Assertions.assertEquals(1, relationships(model, "religious_spouse").stream()
			.filter(r -> "I1".equals(FLEFRecordHelper.getChildValue(r, "subject.individual"))
				&& "I2".equals(FLEFRecordHelper.getChildValue(r, "target.individual"))).count());
		Assertions.assertEquals(1, relationships(model, "civil_spouse").size());
		//a source cited by one spouse only is not enough
		Assertions.assertTrue(relationships(model, "religious_spouse").stream()
			.noneMatch(r -> "I5".equals(FLEFRecordHelper.getChildValue(r, "subject.individual"))));
		//a note that says it
		Assertions.assertTrue(relationships(model, "religious_spouse").stream()
			.anyMatch(r -> "I8".equals(FLEFRecordHelper.getChildValue(r, "target.individual"))));
	}

	@Test
	void aMarriageWhoseKindIsNotKnownIsGenericWithoutANoteToRevise() throws IOException{
		final FLEFModel model = convert(GEDCOM_MARRIAGE_KINDS);

		final List<FLEFRecord> generic = relationships(model, "spouse");
		Assertions.assertEquals(2, generic.size());
		//the type the file gave to the marriage is kept as a note, nothing says "to be revised"
		Assertions.assertTrue(generic.stream().noneMatch(r -> String.valueOf(FLEFRecordHelper.getChildValue(r, "note.text")).contains("TO BE REVISED")));
		Assertions.assertTrue(generic.stream().anyMatch(r -> "Type of the marriage in the file: Family".equals(FLEFRecordHelper.getChildValue(r, "note.text"))));
	}

}
