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
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;


/** The text of the narratives and of the validity of a record, as they are written in the reports. */
class ReportValidityTest{

	private static final ReportLabels LABELS = new ReportLabels(ReportLanguage.ENGLISH);


	@Test
	void aRecordThatStartsAndEndsOnTheSameDateHasOneDate(){
		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendValidity(rows, LABELS, "14 FEB 1953", "14 FEB 1953");

		Assertions.assertEquals(List.of("**" + LABELS.sections().date() + ":** 14 FEB 1953"), rows);
	}

	@Test
	void aPeriodHasItsStartAndItsEnd(){
		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendValidity(rows, LABELS, "1 JAN 1950", "1 JAN 1960");

		Assertions.assertEquals(List.of("**" + LABELS.sections().validFrom() + ":** 1 JAN 1950",
			"**" + LABELS.sections().validTo() + ":** 1 JAN 1960"), rows);
	}

	@Test
	void aMissingBoundIsLeftOut(){
		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendValidity(rows, LABELS, null, "1 JAN 1960");

		Assertions.assertEquals(List.of("**" + LABELS.sections().validTo() + ":** 1 JAN 1960"), rows);
	}

	@Test
	void theChildrenAreNamedAfterTheirKind(){
		Assertions.assertEquals("Antonia had 7 biological children: A, B.", LABELS.narrativeChildrenGroup("Antonia", 7, "biological children", "A, B"));
		Assertions.assertEquals("Antonia had one biological child: A.", LABELS.narrativeChildrenGroup("Antonia", 1, "biological child", "A"));
		Assertions.assertEquals("Antonia and Zamaria had 2 adopted children: A, B.",
			LABELS.narrativeChildrenGroupWith("Antonia", "Zamaria", 2, "adopted children", "A, B"));
	}

	@Test
	void theFragmentsOfAUnionAreSeparated(){
		Assertions.assertEquals("Dorato and Pastrelo family was established through marriage on 30 JAN 1657 in Sant'Angelo sul Sile.",
			LABELS.narrative().groupNarrativeUnion("Dorato and Pastrelo family", "marriage", "30 JAN 1657", "Sant'Angelo sul Sile"));
	}

	@Test
	void aLinkToAFileIsWrittenSoThatEveryViewerFollowsIt(){
		Assertions.assertEquals("../../x/My%20Photos/birth%20%28Pastrelo%29.jpg", ReportFormatters.linkTarget("../../x/My Photos/birth (Pastrelo).jpg"));
		Assertions.assertEquals("file:///C:/mauro/heritage/n%C3%A0me.jpg", ReportFormatters.linkTarget("C:\\mauro\\heritage\\nàme.jpg"));
		Assertions.assertEquals("https://host/a%20b.jpg", ReportFormatters.linkTarget("https://host/a b.jpg"));
	}

	@Test
	void aReportTellsTheNameOfAFileNotTheWholePathOfTheDisk(){
		Assertions.assertEquals("birth.jpg", ReportFormatters.fileNameOf("C:\\mauro\\heritage\\Photos\\birth.jpg"));
		Assertions.assertEquals("birth.jpg", ReportFormatters.fileNameOf("/home/x/birth.jpg"));
		Assertions.assertEquals("https://host/a/b.jpg", ReportFormatters.fileNameOf("https://host/a/b.jpg"));
		Assertions.assertNull(ReportFormatters.fileNameOf(" "));
	}

}
