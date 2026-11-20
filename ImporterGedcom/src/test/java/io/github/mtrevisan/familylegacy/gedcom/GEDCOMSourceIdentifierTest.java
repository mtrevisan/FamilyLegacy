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

import io.github.mtrevisan.familylegacy.importer.ImportReport;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentifierReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/**
 * The call number (CALN) of a repository is an identifier of the repository citation, the reference number (REFN) of a source is an
 * identifier of the source; the locator, the position inside the source, is left to the page, the folio, the frame.
 */
class GEDCOMSourceIdentifierTest{

	private static FLEFModel convert(final String body) throws IOException{
		final String gedcom = "0 HEAD\n1 CHAR UTF-8\n0 @R1@ REPO\n1 NAME Archive\n" + body + "0 TRLR\n";
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(gedcom));
		return new GEDCOMToFLEFConverter().convert(roots, (percent, message) -> {}, new ImportReport());
	}

	private static FLEFRecord citationOf(final FLEFModel model){
		return FLEFRecordHelper.findChild(model.getRecordById("S1"), SourceReader.TAG_REPOSITORY);
	}


	@Test
	void aCallNumberIsAnIdentifierOfTheRepositoryCitation() throws IOException{
		final FLEFModel model = convert("""
			0 @S1@ SOUR
			1 TITL Register
			1 REPO @R1@
			2 CALN MS 45/12
			3 MEDI Book
			""");

		final FLEFRecord citation = citationOf(model);
		final List<FLEFRecord> identifiers = RepositoryCitationReader.extractIdentifiers(citation);
		Assertions.assertEquals(1, identifiers.size());
		Assertions.assertEquals(IdentifierReader.ENUM_SCHEME_CALL_NUMBER, IdentifierReader.extractScheme(identifiers.getFirst()));
		Assertions.assertEquals("MS 45/12", IdentifierReader.extractValue(identifiers.getFirst()));
		Assertions.assertNull(FLEFRecordHelper.getChildValue(citation, RepositoryCitationReader.TAG_LOCATOR), "it is not a position");
		Assertions.assertTrue(SourceReader.extractIdentifiers(model.getRecordById("S1")).isEmpty(), "it does not identify the source itself");
	}

	@Test
	void severalCallNumbersAreSeveralIdentifiersAndARepeatedOneIsWrittenOnce() throws IOException{
		final FLEFModel model = convert("""
			0 @S1@ SOUR
			1 TITL Register
			1 REPO @R1@
			2 CALN MS 45/12
			2 CALN 929.2 ROS
			2 CALN MS 45/12
			""");

		Assertions.assertEquals(List.of("MS 45/12", "929.2 ROS"),
			IdentifierReader.extractValues(citationOf(model), IdentifierReader.ENUM_SCHEME_CALL_NUMBER));
	}

	@Test
	void aBlankCallNumberIsNotAnIdentifier() throws IOException{
		final FLEFModel model = convert("""
			0 @S1@ SOUR
			1 TITL Register
			1 REPO @R1@
			2 CALN
			3 MEDI Book
			""");

		Assertions.assertTrue(RepositoryCitationReader.extractIdentifiers(citationOf(model)).isEmpty());
	}

	@Test
	void theReferenceNumberOfASourceTakesItsSchemeFromItsType() throws IOException{
		final FLEFModel model = convert("""
			0 @S1@ SOUR
			1 TITL Rossi Family History
			1 REFN 978-0-19-953556-9
			2 TYPE ISBN
			1 REFN 10.1000/182
			2 TYPE DOI
			1 REFN 1234567
			2 TYPE FHL film number
			""");

		final FLEFRecord source = model.getRecordById("S1");
		Assertions.assertEquals(List.of("978-0-19-953556-9"), IdentifierReader.extractValues(source, "isbn"));
		Assertions.assertEquals(List.of("10.1000/182"), IdentifierReader.extractValues(source, "doi"));
		Assertions.assertEquals(List.of("1234567"), IdentifierReader.extractValues(source, "fhl_film"));
		Assertions.assertTrue(FLEFRecordHelper.findChildren(source, NoteReader.TAG_NOTE).isEmpty(), "nothing is left as a note");
	}

	@Test
	void aReferenceNumberWithoutATypeTakesItsSchemeFromWhatItLooksLike() throws IOException{
		final FLEFModel model = convert("""
			0 @S1@ SOUR
			1 TITL Rossi Family History
			1 REFN 978-0-19-953556-9
			1 REFN https://example.org/rossi
			""");

		final FLEFRecord source = model.getRecordById("S1");
		Assertions.assertEquals(List.of("978-0-19-953556-9"), IdentifierReader.extractValues(source, "isbn"));
		Assertions.assertEquals(List.of("https://example.org/rossi"), IdentifierReader.extractValues(source, "url"));
	}

	@Test
	void aSchemeThatCannotBeMappedSafelyIsKeptAsACustomScheme() throws IOException{
		final FLEFModel model = convert("""
			0 @S1@ SOUR
			1 TITL Register
			1 REFN S-77
			1 REFN 0042
			2 TYPE Barcode
			""");

		final List<FLEFRecord> identifiers = SourceReader.extractIdentifiers(model.getRecordById("S1"));
		Assertions.assertEquals(2, identifiers.size());
		//no type, and not a recognizable value: the GEDCOM tag
		Assertions.assertEquals("REFN", IdentifierReader.extractScheme(identifiers.get(0)));
		Assertions.assertEquals("S-77", IdentifierReader.extractValue(identifiers.get(0)));
		//a type that is not known: the type as written
		Assertions.assertEquals("Barcode", IdentifierReader.extractScheme(identifiers.get(1)));
		Assertions.assertEquals("0042", IdentifierReader.extractValue(identifiers.get(1)));
	}

	@Test
	void aReferenceNumberWithMoreThanItsTypeStaysANoteSoThatNothingIsLost() throws IOException{
		final FLEFModel model = convert("""
			0 @S1@ SOUR
			1 TITL Register
			1 REFN 123
			2 TYPE user
			2 NOTE assigned by the parish
			""");

		final FLEFRecord source = model.getRecordById("S1");
		Assertions.assertTrue(SourceReader.extractIdentifiers(source).isEmpty());
		Assertions.assertFalse(FLEFRecordHelper.findChildren(source, NoteReader.TAG_NOTE).isEmpty());
	}

}
