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

import io.github.mtrevisan.familylegacy.importer.gramps.GrampsXMLToFLEFConverter;
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
import java.nio.charset.StandardCharsets;
import java.util.List;


/**
 * The call number of a repository reference of Gramps is an identifier of the repository citation; the attributes of a source that name
 * an identifier (ISBN, DOI, ...) are identifiers of the source; every other attribute stays a note.
 */
class GrampsSourceIdentifierTest{

	private static final String XML_HEADER = "<database><header><created date=\"2025-03-18\" version=\"6.0.0\"/></header>";
	private static final String XML_FOOTER = "</database>";

	private static FLEFRecord source(final String attributes, final String repositoryReference) throws IOException{
		final String body = "<repositories><repository handle=\"_r\" id=\"R1\" change=\"1\"><rname>Archive</rname></repository></repositories>"
			+ "<sources><source handle=\"_s\" id=\"S1\" change=\"1\"><stitle>T</stitle>" + attributes + repositoryReference
			+ "</source></sources>";
		final FLEFModel model = GrampsXMLToFLEFConverter.convert((XML_HEADER + body + XML_FOOTER).getBytes(StandardCharsets.UTF_8),
			(percent, message) -> {});
		return model.getRecordById("S1");
	}


	@Test
	void aCallNumberIsAnIdentifierOfTheRepositoryCitation() throws IOException{
		final FLEFRecord source = source("", "<reporef hlink=\"_r\" callno=\"MS 45/12\" medium=\"Book\"/>");

		final FLEFRecord citation = FLEFRecordHelper.findChild(source, SourceReader.TAG_REPOSITORY);
		Assertions.assertEquals(List.of("MS 45/12"), IdentifierReader.extractValues(citation, IdentifierReader.ENUM_SCHEME_CALL_NUMBER));
		Assertions.assertNull(FLEFRecordHelper.getChildValue(citation, RepositoryCitationReader.TAG_LOCATOR), "it is not a position");
		Assertions.assertTrue(SourceReader.extractIdentifiers(source).isEmpty());
	}

	@Test
	void aReferenceWithoutACallNumberHasNoIdentifier() throws IOException{
		final FLEFRecord source = source("", "<reporef hlink=\"_r\" callno=\"  \" medium=\"Book\"/>");

		Assertions.assertTrue(RepositoryCitationReader.extractIdentifiers(FLEFRecordHelper.findChild(source, SourceReader.TAG_REPOSITORY))
			.isEmpty());
	}

	@Test
	void theAttributesThatNameAnIdentifierAreIdentifiersOfTheSource() throws IOException{
		final FLEFRecord source = source("<srcattribute type=\"ISBN\" value=\"978-0-19-953556-9\"/>"
			+ "<srcattribute type=\"DOI\" value=\"10.1000/182\"/>"
			+ "<srcattribute type=\"FHL film number\" value=\"1234567\"/>"
			+ "<srcattribute type=\"Shelf mark\" value=\"X 9\"/>", "");

		Assertions.assertEquals(List.of("978-0-19-953556-9"), IdentifierReader.extractValues(source, "isbn"));
		Assertions.assertEquals(List.of("10.1000/182"), IdentifierReader.extractValues(source, "doi"));
		Assertions.assertEquals(List.of("1234567"), IdentifierReader.extractValues(source, "fhl_film"));
		Assertions.assertEquals(4, SourceReader.extractIdentifiers(source).size());
		Assertions.assertTrue(FLEFRecordHelper.findChildren(source, NoteReader.TAG_NOTE).isEmpty(), "nothing is left as a note");
	}

	@Test
	void anAttributeThatIsNotAnIdentifierStaysANote() throws IOException{
		final FLEFRecord source = source("<srcattribute type=\"Shelf color\" value=\"red\"/>"
			+ "<srcattribute type=\"ISBN\" value=\"978-0-19-953556-9\"/>", "");

		Assertions.assertEquals(1, SourceReader.extractIdentifiers(source).size());
		Assertions.assertEquals(1, FLEFRecordHelper.findChildren(source, NoteReader.TAG_NOTE).size());
	}

	@Test
	void aRepeatedIdentifierIsWrittenOnce() throws IOException{
		final FLEFRecord source = source("<srcattribute type=\"ISBN\" value=\"0-306-40615-2\"/>"
			+ "<srcattribute type=\"ISBN-10\" value=\"0306406152\"/>", "");

		Assertions.assertEquals(1, SourceReader.extractIdentifiers(source).size());
	}

}
