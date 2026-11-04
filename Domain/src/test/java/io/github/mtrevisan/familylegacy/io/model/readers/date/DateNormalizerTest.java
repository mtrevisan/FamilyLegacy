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
package io.github.mtrevisan.familylegacy.io.model.readers.date;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class DateNormalizerTest{

	@Test
	void isoDate(){
		// 19 Aug 1886 Gregorian, the default calendar
		final NormalizedDate date = normalizePoint(fullDate(null, "1886-08-19"));

		Assertions.assertEquals(2410138L, date.jdn());
		Assertions.assertEquals(DatePrecision.DAY, date.precision());
	}

	@Test
	void reducedIsoDate(){
		// Aug 1886 Gregorian
		final NormalizedDate date = normalizePoint(fullDate(null, "1886-08"));

		Assertions.assertEquals(2410120L, date.jdn());
		Assertions.assertEquals(DatePrecision.MONTH, date.precision());
	}

	@Test
	void legacyDate(){
		// 19 Aug 1886 Gregorian, legacy FLEF notation
		final NormalizedDate date = normalizePoint(fullDate("gregorian", "19 AUG 1886"));

		Assertions.assertEquals(2410138L, date.jdn());
		Assertions.assertEquals(DatePrecision.DAY, date.precision());
	}

	@Test
	void legacyMonthAndYear(){
		// Aug 1886 Gregorian, legacy FLEF notation
		final NormalizedDate date = normalizePoint(fullDate("gregorian", "AUG 1886"));

		Assertions.assertEquals(2410120L, date.jdn());
		Assertions.assertEquals(DatePrecision.MONTH, date.precision());
	}

	@Test
	void yearAlone(){
		// 1886 Gregorian
		final NormalizedDate date = normalizePoint(fullDate("gregorian", "1886"));

		Assertions.assertEquals(2409908L, date.jdn());
		Assertions.assertEquals(DatePrecision.YEAR, date.precision());
	}

	@Test
	void beforeChrist(){
		// Ides of March, assassination of Julius Caesar (15 Mar 44 BC Julian)
		final NormalizedDate date = normalizePoint(fullDate("julian", "15 MAR 44 BC"));

		Assertions.assertEquals(1705426L, date.jdn());
		Assertions.assertEquals(DatePrecision.DAY, date.precision());
	}

	@Test
	void dateInTheNotationOfItsCalendar(){
		// Passover, 15 Nisan 5785 (13 Apr 2025 Gregorian)
		final NormalizedDate date = normalizePoint(fullDate("hebrew", "15 Nisan 5785"));

		Assertions.assertEquals(2460779L, date.jdn());
		Assertions.assertEquals(DatePrecision.DAY, date.precision());
		Assertions.assertEquals("hebrew", date.calendar());
	}

	@Test
	void monthInTheNotationOfItsCalendar(){
		// Nisan 5785 (from 30 Mar 2025 Gregorian)
		final NormalizedDate date = normalizePoint(fullDate("hebrew", "Nisan 5785"));

		Assertions.assertEquals(2460765L, date.jdn());
		Assertions.assertEquals(DatePrecision.MONTH, date.precision());
	}

	@Test
	void decade(){
		// The 1880s start on 1 Jan 1880 Gregorian
		final NormalizedDate date = normalizePoint(decade("gregorian", "1880"));

		Assertions.assertEquals(2407716L, date.jdn());
		Assertions.assertEquals(DatePrecision.DECADE, date.precision());
	}

	@Test
	void decadeInAnotherCalendar(){
		// The decade of 5780 starts on Rosh Hashanah 5780 (30 Sep 2019 Gregorian)
		final NormalizedDate date = normalizePoint(decade("hebrew", "5780"));

		Assertions.assertEquals(2458757L, date.jdn());
		Assertions.assertEquals(DatePrecision.DECADE, date.precision());
	}

	@Test
	void century(){
		// The 19th century starts on 1 Jan 1801 Gregorian
		final NormalizedDate date = normalizePoint(century("gregorian", "19"));

		Assertions.assertEquals(2378862L, date.jdn());
		Assertions.assertEquals(DatePrecision.CENTURY, date.precision());
	}

	@Test
	void unparsableDate(){
		Assertions.assertNull(DateNormalizer.normalize(fullDate("gregorian", "sometime in the past")));
	}

	@Test
	void dateNotInTheNotationOfItsCalendar(){
		Assertions.assertNull(DateNormalizer.normalize(fullDate("hebrew", "15 AUG 1886")));
	}


	private static NormalizedDate normalizePoint(final FLEFRecord dateStructure){
		final TemporalSpan span = DateNormalizer.normalize(dateStructure);

		Assertions.assertNotNull(span);
		Assertions.assertTrue(span.isPoint());
		return span.start();
	}

	private static FLEFRecord fullDate(final String calendar, final String fullDate){
		return dateStructure(calendar, FLEFRecord.createChildWithTagAndValue(DateReader.TAG_FULL_DATE, fullDate));
	}

	private static FLEFRecord decade(final String calendar, final String startYear){
		return dateStructure(calendar, FLEFRecord.createChildWithTagAndValue(DateReader.TAG_DECADE, startYear));
	}

	private static FLEFRecord century(final String calendar, final String ordinal){
		return dateStructure(calendar, FLEFRecord.createChildWithTag(DateReader.TAG_CENTURY)
			.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_ORDINAL, ordinal)));
	}

	/**
	 * Builds a {@code DateStructure} holding a {@code point} with the given {@code SingleDate} value.
	 */
	private static FLEFRecord dateStructure(final String calendar, final FLEFRecord singleDateValue){
		final FLEFRecord point = FLEFRecord.createChildWithTag(DateReader.TAG_POINT);
		if(calendar != null)
			point.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_CALENDAR, calendar));
		point.addChild(FLEFRecord.createChildWithTag(DateReader.TAG_VALUE)
			.addChild(singleDateValue));

		return FLEFRecord.createEmpty()
			.addChild(FLEFRecord.createChildWithTag(DateReader.TAG_VALUE)
				.addChild(point));
	}

}
