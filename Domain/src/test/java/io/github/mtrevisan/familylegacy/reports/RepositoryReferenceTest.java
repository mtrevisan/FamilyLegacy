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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLabels;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/** The reports say where a source is found in a repository: the identifiers of the citation first, then the locator. */
class RepositoryReferenceTest{

	private static final ReportLabels LABELS = new ReportLabels(ReportLanguage.ENGLISH);


	private static FLEFRecord citation(final String body){
		final FLEFModel model = new FLEFParser().parse("source S1 {\n\ttitle.value Register\n\trepository {\n\t\trepository R1\n" + body
			+ "\t}\n\taudit.creation.date 2020-01-01T00:00:00Z\n}\n");
		return FLEFRecordHelper.findChild(model.getRecordById("S1"), SourceReader.TAG_REPOSITORY);
	}


	@Test
	void theIdentifiersComeBeforeTheLocatorAndHaveTheNameOfTheirScheme(){
		final String text = ReportFormatters.repositoryReference(citation("\t\tidentifier {\n\t\t\tscheme call_number\n\t\t\tvalue MS 45/12\n\t\t}\n"
			+ "\t\tidentifier {\n\t\t\tscheme dewey\n\t\t\tvalue 929.2 ROS\n\t\t}\n\t\tlocator Page 27\n"), LABELS);

		Assertions.assertEquals("call number: MS 45/12; Dewey: 929.2 ROS; Page 27", text);
	}

	@Test
	void aSchemeOfTheUserIsWrittenAsItIs(){
		Assertions.assertEquals("Barcode: 0042", ReportFormatters.repositoryReference(
			citation("\t\tidentifier {\n\t\t\tscheme Barcode\n\t\t\tvalue 0042\n\t\t}\n"), LABELS));
	}

	@Test
	void aCitationWithOnlyALocatorOrNothingIsStillSaid(){
		Assertions.assertEquals("Page 27", ReportFormatters.repositoryReference(citation("\t\tlocator Page 27\n"), LABELS));
		Assertions.assertNull(ReportFormatters.repositoryReference(citation(""), LABELS));
	}

}
