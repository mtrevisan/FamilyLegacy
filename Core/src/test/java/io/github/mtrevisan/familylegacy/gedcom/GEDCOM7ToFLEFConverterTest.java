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

import io.github.mtrevisan.familylegacy.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


class GEDCOM7ToFLEFConverterTest{

	private static final String GEDCOM = """
		0 HEAD
		1 GEDC
		2 VERS 7.0
		1 SOUR FamilyApp
		2 NAME Family App
		1 DATE 14 OCT 2026
		2 TIME 20:51:22Z
		1 SUBM @U1@
		1 SNOTE @N2@
		0 @N1@ SNOTE A shared note
		1 MIME text/html
		1 LANG en
		1 TRAN Una nota condivisa
		2 LANG it
		0 @N2@ SNOTE File scope
		0 @U1@ SUBM
		1 NAME John Doe
		1 EMAIL john@example.com
		0 @M1@ OBJE
		1 FILE media/register.jpg
		2 FORM image/jpeg
		3 MEDI PHOTO
		2 TITL Register page
		2 TRAN media/register-thumb.jpg
		3 FORM image/jpeg
		1 RESN PRIVACY
		0 @S1@ SOUR
		1 TITL Baptism register
		1 REPO @VOID@
		2 CALN 12/A
		1 OBJE @M1@
		0 @I1@ INDI
		1 NAME Giovanni /Rossi/
		2 TYPE OTHER
		3 PHRASE Baptismal name
		2 TRAN John /Red/
		3 LANG en
		1 SEX M
		1 BIRT
		2 DATE JULIAN 1 MAR 1700
		2 PLAC Venezia, Italia
		3 LANG it
		3 TRAN Venice, Italy
		4 LANG en
		2 ASSO @I3@
		3 ROLE WITN
		2 ASSO @I2@
		3 ROLE FATH
		2 SOUR @S1@
		3 PAGE Folio 12
		3 DATA
		4 TEXT Baptizatus est
		5 LANG la
		3 OBJE @M1@
		4 CROP
		5 TOP 10
		5 LEFT 20
		5 HEIGHT 100
		5 WIDTH 200
		2 SOUR @VOID@
		3 PAGE Family tradition
		1 DEAT
		2 DATE 1760
		3 PHRASE In the spring of 1760
		1 INIL
		2 DATE 1990
		1 NO MARR
		2 DATE FROM 1720 TO 1760
		2 SOUR @S1@
		3 PAGE Folio 30
		1 ASSO @I3@
		2 ROLE GODP
		1 ASSO @VOID@
		2 PHRASE Mr Stockdale
		2 ROLE FRIEND
		1 SNOTE @N1@
		1 NOTE @@home
		1 FAMC @F1@
		2 PEDI BIRTH
		1 CREA
		2 DATE 1 JAN 2020
		1 CHAN
		2 DATE 2 FEB 2021
		3 TIME 10:00:00Z
		0 @I2@ INDI
		1 NAME Pietro /Rossi/
		1 SEX M
		1 BIRT
		2 PLAC Venezia, Italia
		3 TRAN Venice, Italy
		4 LANG en
		1 FAMS @F1@
		0 @I3@ INDI
		1 NAME Marco /Bianchi/
		1 SEX X
		1 NOTE Mail a@@b
		0 @F1@ FAM
		1 HUSB @I2@
		1 WIFE @VOID@
		2 PHRASE Unknown mother
		1 CHIL @I1@
		1 FACT Catholic
		2 TYPE Religion
		1 ASSO @I3@
		2 ROLE FRIEND
		0 TRLR
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static List<FLEFRecord> recordsWith(final FLEFModel model, final String type, final String path, final String value){
		return model.getRecordsByType(type).stream()
			.filter(record -> value.equals(FLEFRecordHelper.getChildValue(record, path)))
			.toList();
	}

	/** The first event of a type (the birth of I1 comes before that of I2). */
	private static FLEFRecord eventOfType(final FLEFModel model, final String type){
		return recordsWith(model, "event", "type", type).getFirst();
	}

	private static FLEFRecord noteWithTitle(final FLEFRecord record, final String title){
		return FLEFRecordHelper.findChildren(record, "note").stream()
			.filter(note -> title.equals(FLEFRecordHelper.getChildValue(note, "title")))
			.findFirst()
			.orElse(null);
	}

	private static boolean hasNoteWithText(final FLEFRecord record, final String text){
		return FLEFRecordHelper.findChildren(record, "note").stream()
			.anyMatch(note -> text.equals(FLEFRecordHelper.getChildValue(note, "text")));
	}


	@Test
	void validAgainstTheSchema() throws IOException, URISyntaxException{
		final FLEFModel model = convert();

		final Path grammarPath = Path.of(GEDCOM7ToFLEFConverterTest.class.getResource("/gedg/flef_0.2.0.gedg").toURI());
		final FLEFGrammar grammar = FLEFGrammarParser.parse(grammarPath);
		final FLEFValidator validator = new FLEFValidator(grammar);
		Assertions.assertEquals(List.of(), validator.validateSchema(model));
		Assertions.assertEquals(List.of(), validator.validateIntegrity(model));
	}

	@Test
	void readAsUTF8() throws IOException{
		final byte[] bytes = GEDCOM.replace("Giovanni", "Nuño").getBytes(StandardCharsets.UTF_8);

		final String content;
		try(BufferedReader reader = GEDCOMHelper.getBufferedReader(new ByteArrayInputStream(bytes))){
			content = reader.lines()
				.collect(Collectors.joining("\n"));
		}
		Assertions.assertTrue(content.contains("1 NAME Nuño /Rossi/"));
	}

	@Test
	void header() throws IOException{
		final FLEFRecord header = convert().getHeader();

		Assertions.assertEquals("2026-10-14T20:51:22Z", FLEFRecordHelper.getChildValue(header, "date"));
		Assertions.assertEquals("File scope", FLEFRecordHelper.getChildValue(header, "scope"));
		Assertions.assertEquals("john@example.com", FLEFRecordHelper.getChildValue(header, "submitter.contact.value"));
	}

	@Test
	void escapes() throws IOException{
		final FLEFModel model = convert();

		// only a leading '@' is doubled
		Assertions.assertTrue(hasNoteWithText(model.getRecordById("I1"), "@home"));
		Assertions.assertTrue(hasNoteWithText(model.getRecordById("I3"), "Mail a@@b"));
	}

	@Test
	void sharedNote() throws IOException{
		final FLEFRecord note = FLEFRecordHelper.findChildren(convert().getRecordById("I1"), "note").stream()
			.filter(n -> "A shared note".equals(FLEFRecordHelper.getChildValue(n, "text")))
			.findFirst()
			.orElseThrow();

		Assertions.assertEquals("text/html", FLEFRecordHelper.getChildValue(note, "mime"));
		Assertions.assertEquals("en", FLEFRecordHelper.getChildValue(note, "locale"));
		Assertions.assertEquals("Una nota condivisa", FLEFRecordHelper.getChildValue(note, "translation.text"));
		Assertions.assertEquals("it", FLEFRecordHelper.getChildValue(note, "translation.locale"));
	}

	@Test
	void names() throws IOException{
		final FLEFRecord name = FLEFRecordHelper.findChild(convert().getRecordById("I1"), "name");

		// TYPE OTHER takes its PHRASE
		Assertions.assertEquals("baptismal_name", FLEFRecordHelper.getChildValue(name, "type"));
		final List<FLEFRecord> parts = FLEFRecordHelper.findChildren(name, "part");
		Assertions.assertEquals("John", FLEFRecordHelper.getChildValue(parts.get(0), "variant.transcription.value"));
		Assertions.assertEquals("anglicized", FLEFRecordHelper.getChildValue(parts.get(0), "variant.transcription.type"));
		Assertions.assertEquals("Red", FLEFRecordHelper.getChildValue(parts.get(1), "variant.transcription.value"));
	}

	@Test
	void dates() throws IOException{
		final FLEFModel model = convert();

		final FLEFRecord birth = eventOfType(model, "birth");
		Assertions.assertEquals("1 MAR 1700", FLEFRecordHelper.getChildValue(birth, "date.value.point.value.full_date"));
		Assertions.assertEquals("julian", FLEFRecordHelper.getChildValue(birth, "date.value.point.calendar"));
		final FLEFRecord death = eventOfType(model, "death");
		Assertions.assertEquals("1760", FLEFRecordHelper.getChildValue(death, "date.value.point.value.full_date"));
		Assertions.assertEquals("In the spring of 1760", FLEFRecordHelper.getChildValue(death, "date.original_text"));
		Assertions.assertNotNull(eventOfType(model, "lds_initiatory"));
	}

	@Test
	void repeatableNormalization() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		GEDCOM7Normalizer.normalize(roots);
		GEDCOM7Normalizer.normalize(roots);

		final GEDCOMNode individual = roots.stream()
			.filter(root -> "I1".equals(root.getXrefId()))
			.findFirst()
			.orElseThrow();
		Assertions.assertEquals("INT 1760 (In the spring of 1760)",
			GEDCOMHelper.findFirstValue(GEDCOMHelper.findFirstChild(individual, "DEAT"), "DATE"));
		Assertions.assertEquals("@#DJULIAN@ 1 MAR 1700",
			GEDCOMHelper.findFirstValue(GEDCOMHelper.findFirstChild(individual, "BIRT"), "DATE"));
	}

	@Test
	void places() throws IOException{
		final FLEFModel model = convert();

		final FLEFRecord venice = model.getRecordsByType("place").stream()
			.filter(place -> "Venezia".equals(FLEFRecordHelper.getChildValue(place, "name[0].value")))
			.findFirst()
			.orElseThrow();
		Assertions.assertEquals("it", FLEFRecordHelper.getChildValue(venice, "name[0].locale"));
		// the place is cited twice, with the same translation
		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(venice, "name");
		Assertions.assertEquals(2, names.size());
		final FLEFRecord translated = names.getLast();
		Assertions.assertEquals("Venice", FLEFRecordHelper.getChildValue(translated, "value"));
		Assertions.assertEquals("translated", FLEFRecordHelper.getChildValue(translated, "type"));
		Assertions.assertEquals("en", FLEFRecordHelper.getChildValue(translated, "locale"));
	}

	@Test
	void citations() throws IOException{
		final FLEFModel model = convert();

		final List<FLEFRecord> citations = FLEFRecordHelper.findChildren(eventOfType(model, "birth"), "source");
		final FLEFRecord citation = citations.getFirst();
		Assertions.assertEquals("S1", FLEFRecordHelper.getChildValue(citation, "source"));
		Assertions.assertEquals("Folio 12", FLEFRecordHelper.getChildValue(citation, "locator"));
		Assertions.assertEquals("la", FLEFRecordHelper.getChildValue(citation, "extract.locale"));
		final FLEFRecord crop = FLEFRecordHelper.findChildren(citation, "extract").stream()
			.map(extract -> FLEFRecordHelper.findChild(extract, "media_part.crop"))
			.filter(Objects::nonNull)
			.findFirst()
			.orElseThrow();
		Assertions.assertEquals(List.of("20", "10", "200", "100"), List.of(FLEFRecordHelper.getChildValue(crop, "x"),
			FLEFRecordHelper.getChildValue(crop, "y"), FLEFRecordHelper.getChildValue(crop, "width"),
			FLEFRecordHelper.getChildValue(crop, "height")));

		// a citation of an unrecorded source
		final FLEFRecord voidCitation = citations.get(1);
		Assertions.assertEquals("Family tradition", FLEFRecordHelper.getChildValue(voidCitation, "locator"));
		Assertions.assertEquals("Unspecified source", FLEFRecordHelper.getChildValue(
			model.getRecordById(FLEFRecordHelper.getChildValue(voidCitation, "source")), "title.value"));

		// a call number in an unrecorded repository
		final FLEFRecord source = model.getRecordById("S1");
		Assertions.assertNull(FLEFRecordHelper.findChild(source, "repository"));
		Assertions.assertTrue(hasNoteWithText(source, "Call number in an unrecorded repository: 12/A"));
	}

	@Test
	void media() throws IOException{
		final FLEFRecord media = convert().getRecordById("M1");

		Assertions.assertEquals("image", FLEFRecordHelper.getChildValue(media, "kind"));
		Assertions.assertEquals("restricted", FLEFRecordHelper.getChildValue(media, "privacy.level"));
		Assertions.assertEquals("media/register-thumb.jpg (image/jpeg)",
			FLEFRecordHelper.getChildValue(noteWithTitle(media, "Alternative file"), "text"));
	}

	@Test
	void associates() throws IOException{
		final FLEFModel model = convert();

		final String birthId = eventOfType(model, "birth").getId();
		final List<FLEFRecord> participations = recordsWith(model, "event_participation", "event", birthId);
		final FLEFRecord witness = participations.stream()
			.filter(participation -> "I3".equals(FLEFRecordHelper.getChildValue(participation, "participant.individual")))
			.findFirst()
			.orElseThrow();
		Assertions.assertEquals("witness", FLEFRecordHelper.getChildValue(witness, "role"));
		// a kinship is not a role in the event
		final FLEFRecord father = participations.stream()
			.filter(participation -> "I2".equals(FLEFRecordHelper.getChildValue(participation, "participant.individual")))
			.findFirst()
			.orElseThrow();
		Assertions.assertNull(FLEFRecordHelper.getChildValue(father, "role"));
		Assertions.assertEquals("Father", FLEFRecordHelper.getChildValue(noteWithTitle(father, "Role"), "text"));

		final List<FLEFRecord> associations = recordsWith(model, "relationship", "type", "associate");
		Assertions.assertTrue(associations.stream()
			.anyMatch(association -> "I1".equals(FLEFRecordHelper.getChildValue(association, "target.individual"))
				&& "Godparent".equals(FLEFRecordHelper.getChildValue(association, "role"))));
		Assertions.assertTrue(associations.stream()
			.anyMatch(association -> "G1".equals(FLEFRecordHelper.getChildValue(association, "target.group"))
				&& "Friend".equals(FLEFRecordHelper.getChildValue(association, "role"))));
		// an unrecorded associate
		Assertions.assertTrue(hasNoteWithText(model.getRecordById("I1"), "Associate: Mr Stockdale (Friend)"));
	}

	@Test
	void nonEvent() throws IOException{
		final FLEFRecord note = noteWithTitle(convert().getRecordById("I1"), "Did not happen");

		Assertions.assertEquals("marriage (FROM 1720 TO 1760)", FLEFRecordHelper.getChildValue(note, "text"));
		Assertions.assertEquals("Folio 30", FLEFRecordHelper.getChildValue(note, "source.locator"));
	}

	@Test
	void family() throws IOException{
		final FLEFModel model = convert();

		// the pointer to nothing (the wife) is dropped
		Assertions.assertEquals(List.of("biological_child"), recordsWith(model, "relationship", "subject.individual", "I1").stream()
			.filter(relationship -> "I2".equals(FLEFRecordHelper.getChildValue(relationship, "target.individual")))
			.map(relationship -> FLEFRecordHelper.getChildValue(relationship, "type"))
			.toList());
		final FLEFRecord religion = recordsWith(model, "group_attribute", "type", "religion").getFirst();
		Assertions.assertEquals("Catholic", FLEFRecordHelper.getChildValue(religion, "value"));
	}

	@Test
	void audit() throws IOException{
		final FLEFRecord audit = FLEFRecordHelper.findChild(convert().getRecordById("I1"), "audit");

		Assertions.assertEquals("2020-01-01T00:00:00Z", FLEFRecordHelper.getChildValue(audit, "creation.date"));
		Assertions.assertEquals("2021-02-02T10:00:00Z", FLEFRecordHelper.getChildValue(audit, "update.date"));
	}

}
