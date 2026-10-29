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
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.file.Path;
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
			.map(relationship -> FLEFRecordHelper.getChildValue(relationship, "type"))
			.toList();
	}

	private static List<FLEFRecord> relationships(final FLEFModel model, final String type){
		return model.getRecordsByType("relationship").stream()
			.filter(relationship -> type.equals(FLEFRecordHelper.getChildValue(relationship, "type")))
			.toList();
	}

	private static FLEFRecord eventOfType(final FLEFModel model, final String type){
		return model.getRecordsByType("event").stream()
			.filter(event -> type.equals(FLEFRecordHelper.getChildValue(event, "type")))
			.findFirst()
			.orElse(null);
	}

	private static FLEFRecord attributeOfType(final FLEFModel model, final String type){
		return model.getRecordsByType("individual_attribute").stream()
			.filter(attribute -> type.equals(FLEFRecordHelper.getChildValue(attribute, "type")))
			.findFirst()
			.orElse(null);
	}


	@Test
	void validAgainstTheSchema() throws IOException, URISyntaxException{
		final FLEFModel model = convert();

		final Path grammarPath = Path.of(GEDCOMToFLEFConverterTest.class.getResource("/gedg/flef_0.2.0.gedg").toURI());
		final FLEFGrammar grammar = FLEFGrammarParser.parse(grammarPath);
		final FLEFValidator validator = new FLEFValidator(grammar);
		Assertions.assertEquals(List.of(), validator.validateSchema(model));
		Assertions.assertEquals(List.of(), validator.validateIntegrity(model));
	}

	@Test
	void header() throws IOException{
		final FLEFRecord header = convert().getHeader();

		Assertions.assertEquals("Family LEgacy Format", FLEFRecordHelper.getChildValue(header, "protocol.name"));
		Assertions.assertEquals("The Complete Genealogy Builder", FLEFRecordHelper.getChildValue(header, "source.name"));
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
		final FLEFRecord name = FLEFRecordHelper.findChildren(individual, "name").getFirst();

		final List<FLEFRecord> parts = FLEFRecordHelper.findChildren(name, "part");
		Assertions.assertEquals(List.of("prefix", "given", "nickname", "family"), parts.stream()
			.map(part -> FLEFRecordHelper.getChildValue(part, "type"))
			.toList());
		Assertions.assertEquals("de Trevisan", FLEFRecordHelper.getChildValue(parts.get(3), "value"));
		Assertions.assertEquals("Milio", FLEFRecordHelper.getChildValue(parts.get(2), "value"));
		Assertions.assertEquals("/ˈmi.ljo/", FLEFRecordHelper.getChildValue(parts.get(2), "variant.phonetic.value"));
		// the citation of the name belongs to the name
		Assertions.assertEquals("S1", FLEFRecordHelper.getChildValue(name, "source.source"));
		Assertions.assertTrue(FLEFRecordHelper.findChildren(individual, "source").stream()
			.noneMatch(citation -> "#30".equals(FLEFRecordHelper.getChildValue(citation, "locator"))));
	}

	@Test
	void factsThatAreNames() throws IOException{
		final FLEFRecord individual = convert().getRecordById("I1");

		final FLEFRecord familyNickname = FLEFRecordHelper.findChildren(individual, "name").stream()
			.filter(name -> "family_nickname".equals(FLEFRecordHelper.getChildValue(name, "part[0].type")))
			.findFirst()
			.orElseThrow();
		Assertions.assertEquals("Dorato", FLEFRecordHelper.getChildValue(familyNickname, "part.value"));
		Assertions.assertEquals("/do.ˈra.to/", FLEFRecordHelper.getChildValue(familyNickname, "part.variant.phonetic.value"));

		final FLEFRecord alias = FLEFRecordHelper.findChildren(individual, "name").stream()
			.filter(name -> "alias".equals(FLEFRecordHelper.getChildValue(name, "type")))
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
		Assertions.assertTrue(model.getRecordsByType("event_participation").stream()
			.allMatch(participation -> "event".equals(model.getRecordById(
				FLEFRecordHelper.getChildValue(participation, "event")).getTag())));
	}

	@Test
	void sourceCitation() throws IOException{
		final FLEFModel model = convert();
		final FLEFRecord christening = eventOfType(model, "christening");

		final List<FLEFRecord> citations = FLEFRecordHelper.findChildren(christening, "source");
		final FLEFRecord citation = citations.getFirst();
		Assertions.assertEquals("S1", FLEFRecordHelper.getChildValue(citation, "source"));
		Assertions.assertEquals("#31, Date: 21 NOV 1926", FLEFRecordHelper.getChildValue(citation, "locator"));
		Assertions.assertEquals("Baptized", FLEFRecordHelper.getChildValue(citation, "extract.text"));
		Assertions.assertEquals("A citation note", FLEFRecordHelper.getChildValue(citation, "extract.remark"));
		Assertions.assertEquals("primary", FLEFRecordHelper.getChildValue(citation, "evidence.information_type"));

		// the citation data does not change the source record
		final FLEFRecord source = model.getRecordById("S1");
		Assertions.assertNull(FLEFRecordHelper.findChild(source, "date"));
		Assertions.assertNull(FLEFRecordHelper.findChild(source, "evidence"));

		// a source description is the title of an unrecorded source
		final FLEFRecord bible = model.getRecordById(FLEFRecordHelper.getChildValue(citations.get(1), "source"));
		Assertions.assertEquals("A family bible", FLEFRecordHelper.getChildValue(bible, "title.value"));
	}

	@Test
	void sourceAndRepository() throws IOException{
		final FLEFModel model = convert();

		final FLEFRecord source = model.getRecordById("S1");
		Assertions.assertEquals(List.of("Baptism register", "BR"), FLEFRecordHelper.findChildren(source, "title").stream()
			.map(title -> FLEFRecordHelper.getChildValue(title, "value"))
			.toList());
		Assertions.assertEquals("abbreviated", FLEFRecordHelper.getChildValue(source, "title[1].type"));
		Assertions.assertEquals("Parish priest", FLEFRecordHelper.getChildValue(source, "author.text"));
		Assertions.assertEquals("book", FLEFRecordHelper.getChildValue(source, "media_type"));
		Assertions.assertEquals("R1", FLEFRecordHelper.getChildValue(source, "repository.repository"));
		Assertions.assertEquals("12/A", FLEFRecordHelper.getChildValue(source, "repository.locator"));
		// OBJE.SOUR: the media belongs to the source
		Assertions.assertEquals("M1", FLEFRecordHelper.getChildValue(source, "media"));

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
		Assertions.assertEquals("media", media.getTag());
		Assertions.assertEquals("photos/register.jpg", FLEFRecordHelper.getChildValue(media, "uri"));
		Assertions.assertEquals("image", FLEFRecordHelper.getChildValue(media, "kind"));
		Assertions.assertEquals("Register page", FLEFRecordHelper.getChildValue(media, "description"));
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
		final FLEFRecord birth = eventOfType(model, "birth");
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
		Assertions.assertTrue(model.getRecordsByType("event_participation").stream()
			.anyMatch(participation -> marriage.getId().equals(FLEFRecordHelper.getChildValue(participation, "event"))
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
	void association() throws IOException{
		final FLEFModel model = convert();

		// "I1 ASSO @I3@ RELA Godfather": I3 is the godfather of I1
		final FLEFRecord association = relationships(model, "associate").getFirst();
		Assertions.assertEquals("I3", FLEFRecordHelper.getChildValue(association, "subject.individual"));
		Assertions.assertEquals("I1", FLEFRecordHelper.getChildValue(association, "target.individual"));
		Assertions.assertEquals("Godfather", FLEFRecordHelper.getChildValue(association, "role"));
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

}
