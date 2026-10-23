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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;


class CalendarTypeTest{

	@Test
	void fromCode(){
		Assertions.assertEquals(CalendarType.HEBREW, CalendarType.fromCode("hebrew"));
		Assertions.assertEquals(CalendarType.FRENCH_REPUBLICAN, CalendarType.fromCode("French_Republican"));
	}

	@Test
	void fromUnknownCodeDefaultsToGregorian(){
		Assertions.assertEquals(CalendarType.GREGORIAN, CalendarType.fromCode("klingon"));
	}

	@Test
	void jdnToProlepticGregorianLocalDate(){
		// J2000.0 epoch day
		Assertions.assertEquals(LocalDate.of(2000, 1, 1), CalendarType.jdnToProlepticGregorianLocalDate(2451545L));
		// Unix epoch
		Assertions.assertEquals(LocalDate.of(1970, 1, 1), CalendarType.jdnToProlepticGregorianLocalDate(2440588L));
	}

	@Test
	void monthAndYearIsIncomplete(){
		// calendar, month and year, first day of the same month
		final Object[][] dates = {
			{CalendarType.GREGORIAN, "August 1953", "1 August 1953"},
			{CalendarType.JULIAN, "August 1953", "1 August 1953"},
			{CalendarType.REFORMED_JULIAN, "August 1953", "1 August 1953"},
			{CalendarType.VENETAN, "aprile 1500", "1 aprile 1500"},
			{CalendarType.ISLAMIC, "ramadan 1445", "1 ramadan 1445"},
			{CalendarType.BUDDHIST, "mithunayon 2475", "1 mithunayon 2475"},
			{CalendarType.COPTIC, "Thout 1741", "1 Thout 1741"},
			{CalendarType.ETHIOPIAN, "Meskerem 2017", "1 Meskerem 2017"},
			{CalendarType.HEBREW, "Nisan 5785", "1 Nisan 5785"},
			{CalendarType.INDIAN, "Chaitra 1946", "1 Chaitra 1946"},
			{CalendarType.FRENCH_REPUBLICAN, "Brumaire 8", "1 Brumaire 8"},
			{CalendarType.SOVIET_ETERNAL, "ноябрь 1929", "1 ноябрь 1929"},
			{CalendarType.PERSIAN, "Farvardin 1403", "1 Farvardin 1403"},
			{CalendarType.PARSI, "Fravardin 1394", "1 Fravardin 1394"},
			{CalendarType.BYZANTINE, "September 7533", "1 September 7533"},
			{CalendarType.EGYPTIAN, "Thoth 425", "1 Thoth 425"},
			{CalendarType.SELEUCID, "Nisan 1000", "1 Nisan 1000"},
			{CalendarType.ARMENIAN, "Navasard 2", "1 Navasard 2"},
			{CalendarType.RUMI, "mart 1300", "1 mart 1300"}
		};

		Assertions.assertAll(Arrays.stream(dates)
			.map(date -> () -> {
				final CalendarType type = (CalendarType)date[0];
				final String monthAndYear = (String)date[1];
				final String firstDayOfMonth = (String)date[2];
				final CalendarType.CalendarMetadata metadata = type.getMetadata(monthAndYear);

				Assertions.assertTrue(metadata.nativeComponents().get("year"), type + ": year");
				Assertions.assertTrue(metadata.nativeComponents().get("month"), type + ": month");
				Assertions.assertFalse(metadata.nativeComponents().get("day"), type + ": day");
				Assertions.assertFalse(metadata.isComplete(), type + ": month and year is complete");
				Assertions.assertTrue(type.getMetadata(firstDayOfMonth).isComplete(), type + ": full date is incomplete");
				// without the day, the date stands for the first day of the month
				Assertions.assertEquals(type.parseToJdn(firstDayOfMonth), type.parseToJdn(monthAndYear), type + ": JDN");
			}));
	}

	@Test
	void seasonIsIncomplete(){
		final Object[][] seasons = {
			{CalendarType.GREGORIAN, "spr 2024"},
			{CalendarType.CHINESE, "立春 2024年 甲辰年"},
			{CalendarType.HEBREW, "Tekufat Nisan 5784"},
			{CalendarType.PARSI, "Hamin 1394"}
		};

		Assertions.assertAll(Arrays.stream(seasons)
			.map(season -> () -> {
				final CalendarType type = (CalendarType)season[0];
				final CalendarType.CalendarMetadata metadata = type.getMetadata((String)season[1]);

				Assertions.assertFalse(metadata.nativeComponents().get("day"), type + ": day");
				Assertions.assertFalse(metadata.isComplete(), type + ": season is complete");
			}));
	}

}
