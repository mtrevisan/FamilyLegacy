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
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


/** The kind of a Gramps source: from a type attribute, a tag or the medium of its repository, and nothing else is guessed. */
class GrampsSourceKindTest{

	private static final String XML_HEADER = "<database><header><created date=\"2025-03-18\" version=\"6.0.0\"/></header>";
	private static final String XML_FOOTER = "</database>";

	private static FLEFModel convertXml(final String body) throws IOException{
		return GrampsXMLToFLEFConverter.convert((XML_HEADER + body + XML_FOOTER).getBytes(StandardCharsets.UTF_8), (percent, message) -> {});
	}

	private static FLEFRecord source(final String attributes, final String tags, final String repositoryMedium) throws IOException{
		final FLEFModel model = convertXml("<tags><tag handle=\"_t\" name=\"Census\" color=\"#ff0000\" priority=\"1\" change=\"1\"/></tags>"
			+ "<repositories><repository handle=\"_r\" id=\"R1\" change=\"1\"><rname>Archive</rname></repository></repositories>"
			+ "<sources><source handle=\"_s\" id=\"S1\" change=\"1\"><stitle>T</stitle>" + attributes + tags
			+ (repositoryMedium != null? "<reporef hlink=\"_r\" medium=\"" + repositoryMedium + "\"/>": "") + "</source></sources>");
		return model.getRecordById("S1");
	}


	@Test
	void aTypeAttributeIsTheKind() throws IOException{
		final FLEFRecord source = source("<srcattribute type=\"Type\" value=\"Church Register\"/>", "", null);

		Assertions.assertEquals(SourceReader.ENUM_KIND_CHURCH_REGISTER, SourceReader.extractKind(source));
	}

	@Test
	void aTagThatSaysAKindIsTheKind() throws IOException{
		final FLEFRecord source = source("", "<tagref hlink=\"_t\"/>", null);

		Assertions.assertEquals(SourceReader.ENUM_KIND_CENSUS, SourceReader.extractKind(source));
	}

	@Test
	void theMediumIsTheKindOnlyWhenItSaysWhatTheSourceIs() throws IOException{
		Assertions.assertEquals(SourceReader.ENUM_KIND_NEWSPAPER, SourceReader.extractKind(source("", "", "Newspaper")));
		Assertions.assertEquals("newspaper", SourceReader.extractMediaType(source("", "", "Newspaper")));
		//a fiche is a physical form: nothing is guessed about what is on it
		final FLEFRecord fiche = source("", "", "Fiche");
		Assertions.assertNull(SourceReader.extractKind(fiche));
		Assertions.assertEquals("fiche", SourceReader.extractMediaType(fiche));
	}

}
