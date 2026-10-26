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
package io.github.mtrevisan.calendars;

import io.github.mtrevisan.calendars.spi.CalendarMetadata;
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
			{CalendarType.FLORENTINE, "gennaio 1500", "1 gennaio 1500"},
			{CalendarType.PISAN, "aprile 1500", "1 aprile 1500"},
			{CalendarType.NATIVITY, "dicembre 1500", "1 dicembre 1500"},
			{CalendarType.BYZANTINE_STYLE, "ottobre 1500", "1 ottobre 1500"},
			{CalendarType.SWEDISH, "August 1705", "1 August 1705"},
			{CalendarType.QUAKER, "3rd month 1705", "1st day 3rd month 1705"},
			{CalendarType.JAPANESE, "昭和20年8月", "昭和20年8月1日"},
			{CalendarType.MINGUO, "民國50年3月", "民國50年3月1日"},
			{CalendarType.DANGI, "단기 4290년 3월", "단기 4290년 3월 1일"},
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
			{CalendarType.RUMI, "mart 1300", "1 mart 1300"},
			{CalendarType.JUCHE, "주체112년 4월", "주체112년 4월 1일"},
			{CalendarType.BENGALI, "Srabon 1431", "1 Srabon 1431"},
			{CalendarType.BENGALI_BANGLADESH, "Srabon 1431", "1 Srabon 1431"},
			{CalendarType.TAMIL, "Aadi 1946", "1 Aadi 1946"},
			{CalendarType.MALAYALAM, "Thulam 1200", "1 Thulam 1200"},
			{CalendarType.NEPALI, "Shrawan 2081", "1 Shrawan 2081"},
			{CalendarType.VIKRAM_SAMVAT, "VS 2081 Kartika", "VS 2081 Kartika Krishna 1"},
			{CalendarType.BAHAI, "Jalál 183", "1 Jalál 183"},
			{CalendarType.BERBER, "Furar 2976", "1 Furar 2976"}
		};

		Assertions.assertAll(Arrays.stream(dates)
			.map(date -> () -> {
				final CalendarType type = (CalendarType)date[0];
				final String monthAndYear = (String)date[1];
				final String firstDayOfMonth = (String)date[2];
				final CalendarMetadata metadata = type.getMetadata(monthAndYear);

				Assertions.assertTrue(metadata.nativeComponents().get(CalendarMetadata.COMPONENT_YEAR), type + ": year");
				Assertions.assertTrue(metadata.nativeComponents().get("month"), type + ": month");
				Assertions.assertFalse(metadata.nativeComponents().get("day"), type + ": day");
				Assertions.assertFalse(metadata.isComplete(), type + ": month and year is complete");
				Assertions.assertTrue(type.getMetadata(firstDayOfMonth).isComplete(), type + ": full date is incomplete");
				// without the day, the date stands for the first day of the month
				Assertions.assertEquals(type.parseToJdn(firstDayOfMonth), type.parseToJdn(monthAndYear), type + ": JDN");
			}));
	}

	@Test
	void yearAloneIsStartOfYear(){
		// calendar, year alone, first day of the same year
		final Object[][] dates = {
			{CalendarType.GREGORIAN, "1953", "1 January 1953"},
			{CalendarType.JULIAN, "1953", "1 January 1953"},
			{CalendarType.REFORMED_JULIAN, "1953", "1 January 1953"},
			{CalendarType.VENETAN, "1500", "1 marzo 1500"},
			{CalendarType.FLORENTINE, "1500", "25 marzo 1500"},
			{CalendarType.PISAN, "1500", "25 marzo 1500"},
			{CalendarType.NATIVITY, "1500", "25 dicembre 1500"},
			{CalendarType.BYZANTINE_STYLE, "1500", "1 settembre 1500"},
			{CalendarType.SWEDISH, "1705", "1 January 1705"},
			{CalendarType.QUAKER, "1705", "25th day 1st month 1705"},
			{CalendarType.JAPANESE, "昭和20年", "昭和20年1月1日"},
			{CalendarType.MINGUO, "民國50年", "民國50年1月1日"},
			{CalendarType.DANGI, "단기 4290년", "단기 4290년 1월 1일"},
			{CalendarType.ISLAMIC, "1446", "1 muharram 1446"},
			{CalendarType.BUDDHIST, "2567", "1 มกราคม 2567"},
			{CalendarType.COPTIC, "1741", "1 Thout 1741"},
			{CalendarType.ETHIOPIAN, "2017", "1 Meskerem 2017"},
			{CalendarType.HEBREW, "5785", "1 Tishrei 5785"},
			{CalendarType.INDIAN, "1946", "1 Chaitra 1946"},
			{CalendarType.FRENCH_REPUBLICAN, "8", "1 Vendémiaire 8"},
			{CalendarType.SOVIET_ETERNAL, "1930", "1 январь 1930"},
			{CalendarType.PERSIAN, "1403", "1 Farvardin 1403"},
			{CalendarType.PARSI, "1394", "1 Fravardin 1394"},
			{CalendarType.BYZANTINE, "7533", "1 September 7533"},
			{CalendarType.EGYPTIAN, "425", "1 Thoth 425"},
			{CalendarType.SELEUCID, "1000", "1 Hyperberetaios 1000"},
			{CalendarType.ARMENIAN, "2", "1 Navasard 2"},
			{CalendarType.RUMI, "1300", "1 mart 1300"},
			{CalendarType.EASTER_STYLE, "1400", "18 avril 1400"},
			{CalendarType.JUCHE, "주체112년", "주체112년 1월 1일"},
			{CalendarType.BENGALI, "1431", "1 Baishakh 1431"},
			{CalendarType.BENGALI_BANGLADESH, "1431", "1 Boishakh 1431"},
			{CalendarType.TAMIL, "1946", "1 Chithirai 1946"},
			{CalendarType.MALAYALAM, "1200", "1 Chingam 1200"},
			{CalendarType.NEPALI, "2080", "1 Baisakh 2080"},
			{CalendarType.VIKRAM_SAMVAT, "VS 2081", "VS 2081 Chaitra Shukla 1"},
			{CalendarType.BAHAI, "183", "1 Bahá 183"},
			{CalendarType.BERBER, "2976", "1 Yennayer 2976"}
		};

		Assertions.assertAll(Arrays.stream(dates)
			.map(date -> () -> {
				final CalendarType type = (CalendarType)date[0];
				final String year = (String)date[1];
				final CalendarMetadata metadata = type.getMetadata(year);

				Assertions.assertTrue(metadata.nativeComponents().get(CalendarMetadata.COMPONENT_YEAR), type + ": year");
				Assertions.assertFalse(metadata.nativeComponents().get("month"), type + ": month");
				Assertions.assertFalse(metadata.nativeComponents().get("day"), type + ": day");
				Assertions.assertEquals(type.parseToJdn((String)date[2]), type.parseToJdn(year), type + ": JDN");
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
				final CalendarMetadata metadata = type.getMetadata((String)season[1]);

				Assertions.assertFalse(metadata.nativeComponents().get("day"), type + ": day");
				Assertions.assertFalse(metadata.isComplete(), type + ": season is complete");
			}));
	}

}
