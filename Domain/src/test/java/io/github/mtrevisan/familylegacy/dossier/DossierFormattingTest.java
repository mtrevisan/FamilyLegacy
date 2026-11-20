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
package io.github.mtrevisan.familylegacy.dossier;

import io.github.mtrevisan.familylegacy.io.model.readers.date.DatePrecision;
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class DossierFormattingTest{

	private static long jdn(final int year){
		return java.time.LocalDate.of(year, 6, 15).toEpochDay() + 2_440_588l;
	}

	private static NormalizedDate year(final int year){
		return NormalizedDate.exact(jdn(year), DatePrecision.YEAR);
	}

	private static NormalizedDate aboutYear(final int year){
		return NormalizedDate.approximated(jdn(year), DatePrecision.YEAR, "gregorian", "stated", null);
	}


	@Test
	void anExactDateHasNoQualifier(){
		Assertions.assertEquals("1850", DossierFormatting.formatSpan(TemporalSpan.point(year(1850))));
	}

	@Test
	void anApproximateDateIsAbout(){
		Assertions.assertEquals("about 1850", DossierFormatting.formatSpan(TemporalSpan.point(aboutYear(1850))));
	}

	@Test
	void aCalculatedDateIsTold(){
		final NormalizedDate calculated = NormalizedDate.approximated(jdn(1850), DatePrecision.YEAR,
			"gregorian", "calculated", null);
		Assertions.assertEquals("calculated 1850", DossierFormatting.formatSpan(TemporalSpan.point(calculated)));
	}

	@Test
	void aBoundedDateIsBeforeAfterOrBetween(){
		Assertions.assertEquals("before 1850", DossierFormatting.formatSpan(TemporalSpan.bounded(null, year(1850))));
		Assertions.assertEquals("after 1850", DossierFormatting.formatSpan(TemporalSpan.bounded(year(1850), null)));
		Assertions.assertEquals("between 1850 and 1860", DossierFormatting.formatSpan(TemporalSpan.bounded(year(1850), year(1860))));
	}

	@Test
	void aSpanningDateIsFromTo(){
		Assertions.assertEquals("from 1850 to 1860", DossierFormatting.formatSpan(TemporalSpan.spanning(year(1850), year(1860))));
	}

}
