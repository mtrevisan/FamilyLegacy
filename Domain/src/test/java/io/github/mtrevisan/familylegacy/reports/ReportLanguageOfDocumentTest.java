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

import io.github.mtrevisan.familylegacy.reports.i18n.ReportLabels;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;


/** The language of a report reaches the document: the tag and the title of the table of contents. */
class ReportLanguageOfDocumentTest{

	@Test
	void theTitleOfTheContentsIsTranslatedForEachLanguage(){
		Assertions.assertEquals("Contents", new ReportLabels(ReportLanguage.ENGLISH).sections().contents());
		Assertions.assertEquals("Contents", new ReportLabels(ReportLanguage.of(Locale.forLanguageTag("it"))).sections().contents());
		Assertions.assertEquals("Contents", new ReportLabels(ReportLanguage.of(Locale.forLanguageTag("fr"))).sections().contents());
		Assertions.assertEquals("Contents", new ReportLabels(ReportLanguage.of(Locale.forLanguageTag("zh-CN"))).sections().contents());
	}

	@Test
	void theTagOfALanguageIsTheOneOfItsLocale(){
		Assertions.assertEquals("it", ReportLanguage.of(Locale.forLanguageTag("it")).locale().toLanguageTag());
		Assertions.assertEquals("zh-CN", ReportLanguage.of(Locale.forLanguageTag("zh-CN")).locale().toLanguageTag());
	}

}
