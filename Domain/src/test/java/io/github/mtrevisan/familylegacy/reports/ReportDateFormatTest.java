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

import io.github.mtrevisan.familylegacy.reports.i18n.ReportDateFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;


/** The way the dates of a report are written: only a full Gregorian ISO date is rewritten, anything else is left as it is. */
class ReportDateFormatTest{

	@Test
	void asRecordedChangesNothing(){
		Assertions.assertEquals("1888-05-01", ReportDateFormat.AS_RECORDED.format("1888-05-01", Locale.ENGLISH));
	}

	@Test
	void aFullDateIsWrittenWithTheMonthInTheLanguage(){
		Assertions.assertEquals("1 May 1888", ReportDateFormat.DAY_MONTH_YEAR.format("1888-05-01", Locale.ENGLISH));
		Assertions.assertEquals("May 1, 1888", ReportDateFormat.MONTH_DAY_YEAR.format("1888-05-01", Locale.ENGLISH));
		Assertions.assertEquals("1 maggio 1888", ReportDateFormat.DAY_MONTH_YEAR.format("1888-05-01", Locale.ITALIAN));
		Assertions.assertEquals("1888", ReportDateFormat.YEAR_ONLY.format("1888-05-01", Locale.ENGLISH));
	}

	@Test
	void aMonthAndAYearAreWrittenWithoutTheDay(){
		Assertions.assertEquals("May 1888", ReportDateFormat.DAY_MONTH_YEAR.format("1888-05", Locale.ENGLISH));
		Assertions.assertEquals("1888", ReportDateFormat.DAY_MONTH_YEAR.format("1888", Locale.ENGLISH));
		Assertions.assertEquals("1888", ReportDateFormat.YEAR_ONLY.format("1888-05", Locale.ENGLISH));
	}

	@Test
	void whatIsNotAnIsoDateStaysAsItIs(){
		for(final String text : new String[]{"1 MAY 1888", "about 1888", "1888-13-45", "0000-01-01", "", "12"})
			Assertions.assertEquals(text, ReportDateFormat.DAY_MONTH_YEAR.format(text, Locale.ENGLISH), text);
		Assertions.assertNull(ReportDateFormat.DAY_MONTH_YEAR.format(null, Locale.ENGLISH));
	}

}
