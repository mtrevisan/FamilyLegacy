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
import io.github.mtrevisan.familylegacy.io.model.readers.ExtractReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** The text of a source (SOUR.TEXT) is a verbatim extract of the source, and a citation that quotes the same text references it. */
class GEDCOMSourceTextTest{

	private static FLEFModel convert(final String body) throws IOException{
		final String gedcom = "0 HEAD\n1 CHAR UTF-8\n" + body + "0 TRLR\n";
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(gedcom));
		return new GEDCOMToFLEFConverter().convert(roots, (percent, message) -> {}, new ImportReport());
	}


	@Test
	void theTextOfASourceIsAnExtractOfIt() throws IOException{
		final FLEFModel model = convert("""
			0 @S1@ SOUR
			1 TITL Baptism register
			1 TEXT Anno domini 1723
			2 CONT die 3 martii
			0 @I1@ INDI
			1 NAME Mario /Rossi/
			1 SOUR @S1@
			2 PAGE p. 12
			2 DATA
			3 TEXT Anno domini 1723
			4 CONT die 3 martii
			""");

		final List<FLEFRecord> extracts = model.getRecordsByType(ExtractReader.TYPE);
		Assertions.assertEquals(1, extracts.size(), extracts.toString());
		final FLEFRecord extract = extracts.getFirst();
		Assertions.assertEquals("S1", ExtractReader.extractSource(extract));
		Assertions.assertEquals("Anno domini 1723\ndie 3 martii", ExtractReader.extractText(extract));
		Assertions.assertEquals(ExtractReader.ENUM_TYPE_VERBATIM, ExtractReader.extractType(extract));

		//no note keeps the text any more
		Assertions.assertTrue(FLEFRecordHelper.findChildren(model.getRecordById("S1"), NoteReader.TAG_NOTE).isEmpty());
		//the citation that quotes the same text references the extract of the source
		final FLEFRecord citation = FLEFRecordHelper.findChild(model.getRecordById("I1"), SourceCitationReader.TAG_SOURCE);
		Assertions.assertEquals(List.of(extract), ExtractReader.extractsOf(citation, model));
	}


	@Test
	void aNoteThatIsADateIsTheDateTheSourceWasCreated() throws IOException{
		final FLEFModel model = convert("""
			0 @S1@ SOUR
			1 TITL Baptism register
			1 NOTE 15 gennaio 1853
			0 @S2@ SOUR
			1 TITL Census
			1 DATA
			2 NOTE April 3, 1881
			0 @S3@ SOUR
			1 TITL Letters
			1 NOTE Kept by the parish priest
			""");

		final FLEFRecord register = model.getRecordById("S1");
		Assertions.assertEquals("15 JAN 1853", FLEFRecordHelper.getChildValue(register, "date.value.point.value.full_date"));
		Assertions.assertEquals("gregorian", FLEFRecordHelper.getChildValue(register, "date.value.point.calendar"));
		Assertions.assertTrue(FLEFRecordHelper.findChildren(register, NoteReader.TAG_NOTE).isEmpty());

		final FLEFRecord census = model.getRecordById("S2");
		Assertions.assertEquals("3 APR 1881", FLEFRecordHelper.getChildValue(census, "date.value.point.value.full_date"));
		Assertions.assertTrue(FLEFRecordHelper.findChildren(census, NoteReader.TAG_NOTE).isEmpty());

		//a note that is not a date stays a note
		final FLEFRecord letters = model.getRecordById("S3");
		Assertions.assertNull(FLEFRecordHelper.findChild(letters, "date"));
		Assertions.assertEquals(1, FLEFRecordHelper.findChildren(letters, NoteReader.TAG_NOTE).size());
	}

}
