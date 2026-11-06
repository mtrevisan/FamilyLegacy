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

import io.github.mtrevisan.familylegacy.importer.ImportIssue;
import io.github.mtrevisan.familylegacy.importer.ImportReasons;
import io.github.mtrevisan.familylegacy.importer.ImportReport;
import io.github.mtrevisan.familylegacy.importer.PendingFile;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.HeaderReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Set;


/**
 * The report of the important information that a GEDCOM import discards, and the data that is kept as notes instead of being
 * discarded.
 */
class GEDCOMImportReportTest{

	private static final Set<String> REASONS = Set.of(ImportReasons.IDENTIFIER, ImportReasons.CONTACT_DATA, ImportReasons.PLACE_DETAIL,
		ImportReasons.NOTE_DETAIL, ImportReasons.MEDIA_DETAIL, ImportReasons.NAME_DETAIL, ImportReasons.DATE_DETAIL,
		ImportReasons.SOURCE_DETAIL, ImportReasons.UNMAPPED_TAG, ImportReasons.OTHER);


	private record Result(FLEFModel model, ImportReport report){}


	private static ImportReport convert(final String body) throws IOException{
		return convert(body, "");
	}

	private static ImportReport convert(final String body, final String headerLines) throws IOException{
		return convertModel(body, headerLines).report();
	}

	private static Result convertModel(final String body) throws IOException{
		return convertModel(body, "");
	}

	private static Result convertModel(final String body, final String headerLines) throws IOException{
		final String gedcom = "0 HEAD\n1 CHAR UTF-8\n" + headerLines + body + "0 TRLR\n";
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(gedcom));
		final ImportReport report = new ImportReport();
		final FLEFModel model = new GEDCOMToFLEFConverter().convert(roots, (percent, message) -> {}, report);
		return new Result(model, report);
	}

	private static List<ImportIssue> issues(final ImportReport report, final String reason){
		return report.issues().stream()
			.filter(issue -> reason.equals(issue.reasonKey()))
			.toList();
	}

	private static void assertReported(final ImportReport report, final String reason, final String itemStart, final String where){
		Assertions.assertTrue(issues(report, reason).stream()
				.anyMatch(issue -> issue.item().startsWith(itemStart) && issue.where().equals(where)),
			"expected " + reason + " " + itemStart + " @ " + where + " but got " + report.issues());
	}

	/** The text of the note of a record with the given title, {@code null} if none. */
	private static String noteText(final FLEFRecord record, final String title){
		for(final FLEFRecord note : FLEFRecordHelper.findChildren(record, NoteReader.TAG_NOTE))
			if(title.equals(FLEFRecordHelper.getChildValue(note, NoteReader.TAG_TITLE)))
				return FLEFRecordHelper.getChildValue(note, NoteReader.TAG_TEXT);
		return null;
	}

	private static void assertNote(final FLEFRecord record, final String title, final String text){
		Assertions.assertEquals(text, noteText(record, title), "note " + title + " of " + record);
	}

	private static FLEFRecord only(final FLEFModel model, final String type){
		final List<FLEFRecord> records = model.getRecordsByType(type);
		Assertions.assertEquals(1, records.size(), type + ": " + records);
		return records.getFirst();
	}

	private static FLEFRecord placeNamed(final FLEFModel model, final String name){
		return model.getRecordsByType(PlaceReader.TYPE).stream()
			//the first name is the primary one (the others are translations)
			.filter(place -> name.equals(FLEFRecordHelper.getChildValue(FLEFRecordHelper.findChildren(place, PlaceReader.TAG_NAME).getFirst(),
				NameReader.TAG_VALUE)))
			.findFirst()
			.orElseThrow();
	}

	private static FLEFRecord eventOfType(final FLEFModel model, final String type){
		return model.getRecordsByType(EventReader.TYPE).stream()
			.filter(event -> type.equals(FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE)))
			.findFirst()
			.orElseThrow();
	}


	@Test
	void identifiersAreKeptAsNotes() throws IOException{
		final Result result = convertModel("""
			0 @S1@ SOUR
			1 TITL Register
			1 REFN S-77
			0 @R1@ REPO
			1 NAME Archive
			1 RFN R-1
			0 @O1@ OBJE
			1 FILE photo.jpg
			2 FORM jpg
			1 UID 11-22
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 REFN 123
			1 REFN 456
			2 TYPE user
			1 _UID ABCDEF
			1 UID 7777
			1 AFN 1AB-2CD
			1 RFN rfn:5
			1 _FSFTID KNWB-123
			1 EXID Q42
			2 TYPE http://wikidata.org
			1 RIN 99
			1 BIRT
			2 DATE 1900
			2 _UID EVENT-UID
			2 PLAC Treviso
			3 EXID Q123
			3 EXID 77
			4 TYPE geonames
			0 @F1@ FAM
			1 REFN FAM-1
			""");

		final FLEFModel model = result.model();
		final FLEFRecord individual = model.getRecordById("I1");
		assertNote(individual, "REFN", "REFN: 123");
		assertNote(individual, "REFN (user)", "REFN: 456");
		assertNote(individual, "_UID", "_UID: ABCDEF");
		assertNote(individual, "UID", "UID: 7777");
		assertNote(individual, "AFN", "AFN: 1AB-2CD");
		assertNote(individual, "RFN", "RFN: rfn:5");
		assertNote(individual, "_FSFTID", "_FSFTID: KNWB-123");
		assertNote(individual, "EXID", "EXID: Q42 (http://wikidata.org)");
		//the record identification number is technical
		Assertions.assertEquals(8, FLEFRecordHelper.findChildren(individual, NoteReader.TAG_NOTE).size());
		assertNote(model.getRecordById("S1"), "REFN", "REFN: S-77");
		assertNote(only(model, RepositoryReader.TYPE), "RFN", "RFN: R-1");
		assertNote(only(model, MediaReader.TYPE), "UID", "UID: 11-22");
		assertNote(only(model, GroupReader.TYPE), "REFN", "REFN: FAM-1");
		assertNote(eventOfType(model, EventReader.ENUM_TYPE_BIRTH), "_UID", "_UID: EVENT-UID");
		assertNote(placeNamed(model, "Treviso"), "EXID", "EXID: Q123");

		Assertions.assertTrue(issues(result.report(), ImportReasons.IDENTIFIER).isEmpty(), result.report().issues().toString());
		Assertions.assertTrue(result.report().isEmpty(), result.report().issues().toString());
	}

	@Test
	void theExternalIdOfAPlaceIsKeptOnThePlace() throws IOException{
		final Result result = convertModel("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 BIRT
			2 PLAC Treviso
			3 EXID 3165185
			4 TYPE https://geonames.org
			1 DEAT
			2 PLAC Treviso
			3 EXID 3165185
			4 TYPE https://geonames.org
			""");

		final FLEFRecord place = placeNamed(result.model(), "Treviso");
		assertNote(place, "EXID", "EXID: 3165185 (https://geonames.org)");
		//the same note is not duplicated when the place is reused
		Assertions.assertEquals(1, FLEFRecordHelper.findChildren(place, NoteReader.TAG_NOTE).size());
		Assertions.assertTrue(result.report().isEmpty(), result.report().issues().toString());
	}

	@Test
	void identifiersOfANoteRecordAreStillReported() throws IOException{
		final ImportReport report = convert("""
			0 @N1@ NOTE A shared note
			1 REFN 5
			""");

		assertReported(report, ImportReasons.IDENTIFIER, "REFN 5", "N1 REFN");
	}

	@Test
	void contactDataOfAnEventAddressIsKeptOnThePlace() throws IOException{
		final Result result = convertModel("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 RESI
			2 ADDR Via Roma 1
			3 CITY Treviso
			3 POST 31100
			2 PHON 0422 1234
			2 EMAIL anna@example.org
			2 FAX 0422 9999
			2 WWW http://example.org
			1 OCCU Baker
			2 PHON 111
			""");

		final FLEFRecord place = placeNamed(result.model(), "Via Roma 1");
		assertNote(place, "Contact", "Postal code: 31100\nPhone: 0422 1234\nE-mail: anna@example.org\nFax: 0422 9999\nWeb: http://example.org");
		//no address: the contact data stays on the attribute
		final FLEFRecord occupation = result.model().getRecordsByType(IndividualAttributeReader.TYPE).stream()
			.filter(attribute -> IndividualAttributeReader.ENUM_TYPE_OCCUPATION.equals(FLEFRecordHelper.getChildValue(attribute,
				IndividualAttributeReader.TAG_TYPE)))
			.findFirst()
			.orElseThrow();
		assertNote(occupation, "Contact", "Phone: 111");
		Assertions.assertTrue(issues(result.report(), ImportReasons.CONTACT_DATA).isEmpty(), result.report().issues().toString());
		Assertions.assertTrue(result.report().isEmpty(), result.report().issues().toString());
	}

	@Test
	void contactsOfARepositoryAreKept() throws IOException{
		final ImportReport report = convert("""
			0 @R1@ REPO
			1 NAME Archive
			1 ADDR Via Roma 1
			2 POST 31100
			1 PHON 0422 1234
			1 WWW http://example.org
			""");

		Assertions.assertTrue(report.isEmpty(), report.issues().toString());
	}

	@Test
	void placeDetailsAreKept() throws IOException{
		final Result result = convertModel("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 BIRT
			2 DATE 1900
			2 PLAC Treviso
			3 MAP
			4 LATI N45.66
			4 LONG E12.24
			3 TRAN Trevise, France
			4 LANG fr
			2 PLAC Padova
			1 DEAT
			2 DATE 1980
			2 PLAC Treviso
			3 MAP
			4 LATI N46.00
			4 LONG E13.00
			1 BURI
			2 PLAC Treviso
			3 MAP
			4 LATI N46.00
			4 LONG E13.00
			""");

		final FLEFModel model = result.model();
		assertNote(eventOfType(model, EventReader.ENUM_TYPE_BIRTH), "Other place", "Other place: Padova");
		final FLEFRecord place = placeNamed(model, "Treviso");
		assertNote(place, "Translation", "Translation (fr): Trevise, France");
		//once for each distinct value
		assertNote(place, "Other coordinates", "Other coordinates: N46.00, E13.00");
		Assertions.assertEquals(2, FLEFRecordHelper.findChildren(place, NoteReader.TAG_NOTE).size());
		Assertions.assertTrue(issues(result.report(), ImportReasons.PLACE_DETAIL).isEmpty(), result.report().issues().toString());
		Assertions.assertTrue(result.report().isEmpty(), result.report().issues().toString());
	}

	@Test
	void anUndecodableBlobAndACropAreReported() throws IOException{
		final ImportReport report = convert("""
			0 @O1@ OBJE
			1 BLOB
			2 CONT .HM.......
			0 @O2@ OBJE
			1 FILE photo.jpg
			2 FORM jpg
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 OBJE @O2@
			2 CROP
			3 LEFT 5
			""");

		assertReported(report, ImportReasons.MEDIA_DETAIL, "BLOB", "O1 BLOB");
		assertReported(report, ImportReasons.MEDIA_DETAIL, "CROP", "I1 OBJE.CROP");
	}

	@Test
	void aBlobBecomesAPendingFile() throws IOException{
		final Result result = convertModel("""
			0 @O1@ OBJE
			1 FORM jpeg
			1 BLOB
			2 CONT .k6/
			2 CONT .k6/
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 OBJE
			2 FORM bmp
			2 BLOB
			3 CONT /.k6
			""");

		final List<PendingFile> pending = result.report().pendingFiles();
		Assertions.assertEquals(2, pending.size(), pending.toString());
		final PendingFile top = pending.getFirst();
		Assertions.assertEquals("O1", top.mediaId());
		Assertions.assertEquals("O1.jpg", top.suggestedName());
		Assertions.assertArrayEquals(new byte[]{1, 2, 3, 1, 2, 3}, top.data());
		final PendingFile embedded = pending.get(1);
		Assertions.assertTrue(embedded.suggestedName().endsWith(".bmp"), embedded.suggestedName());
		for(final PendingFile file : pending){
			final FLEFRecord media = result.model().getRecordById(file.mediaId());
			Assertions.assertNotNull(media, file.toString());
			//the media keeps a placeholder until the application writes the file
			Assertions.assertEquals("unknown", FLEFRecordHelper.getChildValue(media, MediaReader.TAG_URI));
		}
		Assertions.assertEquals(2, result.model().getRecordsByType(MediaReader.TYPE).size());
		Assertions.assertTrue(result.report().isEmpty(), result.report().issues().toString());
	}

	@Test
	void nameTypeIsKeptAsANote() throws IOException{
		final Result result = convertModel("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			2 TYPE one two three four five six
			2 _FOO bar
			""");

		final FLEFRecord name = FLEFRecordHelper.findChild(result.model().getRecordById("I1"), IndividualReader.TAG_NAME);
		assertNote(name, "Name type", "one two three four five six");
		assertNote(name, "GEDCOM _FOO", "bar");
		Assertions.assertNull(FLEFRecordHelper.getChildValue(name, NameReader.TAG_TYPE));
		Assertions.assertTrue(result.report().isEmpty(), result.report().issues().toString());
	}

	@Test
	void sexXIsKeptAsANote() throws IOException{
		final Result result = convertModel("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 SEX X
			""");

		final FLEFRecord individual = result.model().getRecordById("I1");
		assertNote(individual, "Sex", "Sex: X (neither male nor female)");
		Assertions.assertEquals("unknown", FLEFRecordHelper.getChildValue(individual, IndividualReader.TAG_SEX));
		Assertions.assertTrue(result.report().isEmpty(), result.report().issues().toString());
	}

	@Test
	void ldsStatusDateIsKept() throws IOException{
		final Result result = convertModel("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 BAPL
			2 DATE 1 JAN 1950
			2 TEMP SLAKE
			2 STAT COMPLETED
			3 DATE 5 FEB 1951
			""");

		final FLEFRecord ordinance = only(result.model(), EventReader.TYPE);
		assertNote(ordinance, "Status date", "Status date: 5 FEB 1951");
		Assertions.assertTrue(result.report().isEmpty(), result.report().issues().toString());
	}

	@Test
	void submitterNoteGoesToTheRemark() throws IOException{
		final Result result = convertModel("""
			0 @U1@ SUBM
			1 NAME Mario
			1 LANG English
			1 NOTE About me
			2 CONT second line
			1 OBJE
			2 FILE me.jpg
			""", "1 SUBM @U1@\n");

		final String remark = FLEFRecordHelper.getChildValue(result.model().getHeader(), HeaderReader.TAG_SUBMITTER_REMARK);
		Assertions.assertEquals("Mario\nLanguage: English\nAbout me\nsecond line", remark);
		//the multimedia of a submitter have no place in the header
		assertReported(result.report(), ImportReasons.MEDIA_DETAIL, "OBJE", "U1 OBJE");
		Assertions.assertEquals(1, result.report().issues().size(), result.report().issues().toString());
	}

	@Test
	void everyEntryOfTheEventsRecordedIsKept() throws IOException{
		final Result result = convertModel("""
			0 @S1@ SOUR
			1 TITL Register
			1 DATA
			2 EVEN BIRT, CHR
			3 DATE FROM 1800 TO 1850
			3 PLAC Treviso
			2 EVEN DEAT
			3 DATE FROM 1810 TO 1860
			3 PLAC Padova
			2 EVEN MARR
			3 DATE 1830
			""");

		final String text = noteText(result.model().getRecordById("S1"), "Events recorded");
		Assertions.assertEquals("BIRT, CHR, FROM 1800 TO 1850, Treviso\nDEAT, FROM 1810 TO 1860, Padova\nMARR, 1830", text);
		Assertions.assertTrue(result.report().isEmpty(), result.report().issues().toString());
	}

	@Test
	void sourceIdentifier() throws IOException{
		final Result result = convertModel("""
			0 @S1@ SOUR
			1 TITL Register
			1 REFN X1
			2 TYPE user
			""");

		assertNote(result.model().getRecordById("S1"), "REFN (user)", "REFN: X1");
		Assertions.assertTrue(result.report().isEmpty(), result.report().issues().toString());
	}

	@Test
	void otherDiscards() throws IOException{
		final ImportReport report = convert("""
			0 @U1@ SUBM
			1 NAME Mario
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 FAMC @F1@
			0 @I2@ INDI
			1 NAME Luigi /Rossi/
			1 FAMS @F1@
			0 @F1@ FAM
			1 HUSB @I2@
			""", "1 LANG English\n");

		//the default language of the file has no field in FLEF
		assertReported(report, ImportReasons.OTHER, "LANG English", "HEAD LANG");
		assertReported(report, ImportReasons.OTHER, "FAMC @G1@", "I1 FAMC");
		Assertions.assertEquals(2, report.issues().size(), report.issues().toString());
	}

	@Test
	void unmappedTagsAreKeptAsNotes() throws IOException{
		final Result result = convertModel("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 _FOO bar
			2 _SUB sub value
			3 _DEEP deeper
			1 BIRT
			2 DATE 1900
			2 _BAR baz
			0 @F1@ FAM
			1 _QUX 1
			""");

		final FLEFModel model = result.model();
		assertNote(model.getRecordById("I1"), "GEDCOM _FOO", "bar\n_SUB sub value\n  _DEEP deeper");
		assertNote(eventOfType(model, EventReader.ENUM_TYPE_BIRTH), "GEDCOM _BAR", "baz");
		assertNote(only(model, GroupReader.TYPE), "GEDCOM _QUX", "1");
		Assertions.assertTrue(issues(result.report(), ImportReasons.UNMAPPED_TAG).isEmpty(), result.report().issues().toString());
		Assertions.assertTrue(result.report().isEmpty(), result.report().issues().toString());
	}

	@Test
	void unmappedTagsOfANoteRecordAreStillReported() throws IOException{
		final ImportReport report = convert("""
			0 @N1@ NOTE A shared note
			1 _FOO bar
			""");

		assertReported(report, ImportReasons.UNMAPPED_TAG, "_FOO bar", "N1 _FOO");
	}

	@Test
	void technicalTagsAreNotReportedNorKept() throws IOException{
		final Result result = convertModel("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 RIN 5
			1 ANCI @I1@
			1 DESI @I1@
			1 _CRE 2020
			1 BIRT
			2 DATE 1900
			2 SDATE 1901
			0 SUBN
			1 FAMF File
			""");

		Assertions.assertTrue(result.report().isEmpty(), result.report().issues().toString());
		Assertions.assertTrue(FLEFRecordHelper.findChildren(result.model().getRecordById("I1"), NoteReader.TAG_NOTE).isEmpty());
	}

	@Test
	void cleanFileHasAnEmptyReport() throws IOException{
		final ImportReport report = convert("""
			0 @S1@ SOUR
			1 TITL Register
			1 AUTH Priest
			0 @R1@ REPO
			1 NAME Archive
			0 @N1@ NOTE A shared note
			1 CONT second line
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 SEX F
			1 BIRT
			2 DATE 5 MAR 1900
			2 PLAC Treviso, Italia
			2 SOUR @S1@
			3 PAGE 4
			1 NOTE @N1@
			1 FAMS @F1@
			0 @I2@ INDI
			1 NAME Luigi /Rossi/
			1 SEX M
			1 FAMS @F1@
			0 @I3@ INDI
			1 NAME Piero /Rossi/
			1 FAMC @F1@
			0 @F1@ FAM
			1 HUSB @I2@
			1 WIFE @I1@
			1 CHIL @I3@
			1 MARR
			2 DATE 1925
			""");

		Assertions.assertTrue(report.isEmpty(), report.issues().toString());
	}

	@Test
	void theReportDoesNotLeakBetweenConversions() throws IOException{
		final ImportReport first = convert("""
			0 @N1@ NOTE A shared note
			1 REFN 1
			""");
		final ImportReport second = convert("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			""");

		Assertions.assertEquals(1, first.issues().size());
		Assertions.assertTrue(second.isEmpty());
		for(final ImportIssue issue : first.issues())
			Assertions.assertTrue(REASONS.contains(issue.reasonKey()));
	}

}
