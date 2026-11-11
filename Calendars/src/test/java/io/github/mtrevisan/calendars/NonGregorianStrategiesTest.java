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

import io.github.mtrevisan.calendars.astronomy.Zenith;
import io.github.mtrevisan.calendars.bahai.BahaiCalendarStrategy;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.nepali.NepaliCalendarStrategy;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;


/** Day-of-month validation, season anchors and tables of the non-Gregorian calendars. */
class NonGregorianStrategiesTest{

	private static void assertBetween(final long first, final long last, final long actual){
		Assertions.assertTrue(actual >= first && actual <= last, "Expected between " + first + " and " + last + " but was " + actual);
	}

	private static long gregorian(final int year, final int month, final int day){
		return GregorianCalendarStrategy.toJdn(year, month, day);
	}


	@Test
	void persianEsfandHasThirtyDaysOnlyInALeapYear(){
		//1403 is a leap year (1 Farvardin 1404 is 21 March 2025, 1403 started on 20 March 2024: 366 days)
		Assertions.assertEquals(CalendarType.PERSIAN.parseToJdn("1 Farvardin 1404") - 1, CalendarType.PERSIAN.parseToJdn("30 Esfand 1403"));
		Assertions.assertEquals(CalendarType.PERSIAN.parseToJdn("1 Farvardin 1405") - 1, CalendarType.PERSIAN.parseToJdn("29 Esfand 1404"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.PERSIAN.parseToJdn("30 Esfand 1404"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.PERSIAN.parseToJdn("32 Farvardin 1404"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.PERSIAN.parseToJdn("31 Mehr 1404"));
	}

	@Test
	void persianSeasonsAreAnchoredOnTheGregorianMonth(){
		//the Persian year 1403 started in March 2024: the four seasons fall in 2024
		assertBetween(gregorian(2024, 3, 19), gregorian(2024, 3, 21), CalendarType.PERSIAN.parseToJdn("Bahar 1403"));
		assertBetween(gregorian(2024, 6, 20), gregorian(2024, 6, 21), CalendarType.PERSIAN.parseToJdn("Tabestan 1403"));
		assertBetween(gregorian(2024, 9, 22), gregorian(2024, 9, 23), CalendarType.PERSIAN.parseToJdn("Paeez 1403"));
		assertBetween(gregorian(2024, 12, 20), gregorian(2024, 12, 21), CalendarType.PERSIAN.parseToJdn("Zemestan 1403"));
	}

	@Test
	void parsiSeasonsFallInsideTheYear(){
		//the Fasli year 1394 YZ starts on 21 March 2024
		assertBetween(gregorian(2024, 3, 21), gregorian(2024, 3, 21) + 365, CalendarType.PARSI.parseToJdn("Vahar 1394 Fasli"));
		assertBetween(gregorian(2024, 12, 20), gregorian(2024, 12, 21), CalendarType.PARSI.parseToJdn("Zayan 1394 Fasli"));
		assertBetween(gregorian(2024, 6, 20), gregorian(2024, 6, 21), CalendarType.PARSI.parseToJdn("Hamin 1394 Fasli"));
		assertBetween(gregorian(2024, 9, 22), gregorian(2024, 9, 23), CalendarType.PARSI.parseToJdn("Padiz 1394 Fasli"));

		//the Shahenshahi and Kadmi years drift through the seasons: the event falls inside the year, whatever its number
		for(final String reckoning : new String[]{"", " Kadmi"})
			for(final String season : new String[]{"Vahar", "Hamin", "Padiz", "Zayan"}){
				final long start = CalendarType.PARSI.parseToJdn("1 Fravardin 1394" + reckoning);
				final long season1394 = CalendarType.PARSI.parseToJdn(season + " 1394" + reckoning);
				assertBetween(start, start + 364, season1394);
			}
	}

	@Test
	void indianMonthLengths(){
		//the Saka year 1946 starts in 2024, a leap year: Chaitra has 31 days, and starts on 21 March
		Assertions.assertEquals(gregorian(2024, 3, 21) + 30, CalendarType.INDIAN.parseToJdn("31 Chaitra 1946"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.INDIAN.parseToJdn("31 Chaitra 1947"));
		Assertions.assertEquals(gregorian(2025, 3, 22) + 30 + 30, CalendarType.INDIAN.parseToJdn("31 Vaisakha 1947"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.INDIAN.parseToJdn("31 Kartika 1946"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.INDIAN.parseToJdn("31 Phalguna 1946"));
		Assertions.assertEquals(CalendarType.INDIAN.parseToJdn("1 Chaitra 1947") - 1, CalendarType.INDIAN.parseToJdn("30 Phalguna 1946"));
	}

	/** The Ritus start every 60 degrees of tropical longitude, two solar months each. */
	@Test
	void rituStartEvery60Degrees(){
		final long vasanta = CalendarType.INDIAN.parseToJdn("Vasanta 1946");
		final long grishma = CalendarType.INDIAN.parseToJdn("Grishma 1946");
		final long varsha = CalendarType.INDIAN.parseToJdn("Varsha 1946");
		final long sharad = CalendarType.INDIAN.parseToJdn("Sharad 1946");
		final long hemanta = CalendarType.INDIAN.parseToJdn("Hemanta 1946");
		final long shishira = CalendarType.INDIAN.parseToJdn("Shishira 1946");

		assertBetween(gregorian(2024, 3, 19), gregorian(2024, 3, 21), vasanta);
		assertBetween(gregorian(2024, 5, 19), gregorian(2024, 5, 21), grishma);
		assertBetween(gregorian(2024, 7, 20), gregorian(2024, 7, 22), varsha);
		assertBetween(gregorian(2024, 9, 21), gregorian(2024, 9, 23), sharad);
		assertBetween(gregorian(2024, 11, 20), gregorian(2024, 11, 22), hemanta);
		assertBetween(gregorian(2025, 1, 19), gregorian(2025, 1, 21), shishira);
		Assertions.assertTrue(vasanta < grishma && grishma < varsha && varsha < sharad && sharad < hemanta && hemanta < shishira);
		//every Ritu lasts about two months
		for(final long[] pair : new long[][]{{vasanta, grishma}, {grishma, varsha}, {varsha, sharad}, {sharad, hemanta}, {hemanta, shishira}})
			assertBetween(58, 64, pair[1] - pair[0]);
	}

	@Test
	void buddhistRejectsImpossibleDays(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.BUDDHIST.parseToJdn("31 กุมภาพันธ์ 2567"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.BUDDHIST.parseToJdn("30 กุมภาพันธ์ 2567"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.BUDDHIST.parseToJdn("29 กุมภาพันธ์ 2566"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.BUDDHIST.parseToJdn("31 เมษายน 2567"));
		Assertions.assertEquals(gregorian(2024, 2, 29), CalendarType.BUDDHIST.parseToJdn("29 กุมภาพันธ์ 2567"));
		Assertions.assertEquals(gregorian(2024, 1, 1), CalendarType.BUDDHIST.parseToJdn("1 ม.ค. 2567"));
		Assertions.assertEquals(gregorian(2024, 3, 31), CalendarType.BUDDHIST.parseToJdn("31 มี.ค. 2567"));
	}

	@Test
	void mayanEpochIs11August3114BCEProlepticGregorian(){
		Assertions.assertEquals(584283L, CalendarType.MAYAN.parseToJdn("0.0.0.0.0"));
		Assertions.assertEquals(584283L, gregorian(-3113, 8, 11));
		//13.0.0.0.0 is 21 December 2012
		Assertions.assertEquals(gregorian(2012, 12, 21), CalendarType.MAYAN.parseToJdn("13.0.0.0.0"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.MAYAN.parseToJdn("13.0.0.18.0"));
	}

	@Test
	void officialSunriseIsTheUpperLimbOnTheApparentHorizon(){
		Assertions.assertEquals(Math.toRadians(-50. / 60.), Zenith.OFFICIAL.getElevation(), 1e-12);
		Assertions.assertEquals(Math.toRadians(-6.), Zenith.CIVIL.getElevation(), 1e-12);
	}


	private static String[] readTable(final Class<?> owner, final String resource) throws IOException{
		try(final InputStream is = owner.getResourceAsStream(resource)){
			Assertions.assertNotNull(is, resource);
			return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines().toArray(String[]::new);
		}
	}

	@Test
	void everyBikramSambatYearHas365Or366Days() throws IOException{
		int rows = 0;
		for(final String line : readTable(NepaliCalendarStrategy.class, "bikram-sambat.csv")){
			if(line.isBlank() || line.startsWith("#") || line.startsWith("year"))
				continue;

			final String[] fields = StringUtils.split(line, ';');
			int days = 0;
			for(int i = 1; i <= 12; i ++)
				days += Integer.parseInt(fields[i]);
			Assertions.assertTrue(days == 365 || days == 366, "The year " + fields[0] + " BS has " + days + " days");
			rows ++;
		}
		Assertions.assertEquals(131, rows);
	}

	@Test
	void bikramSambatIsAnchoredToThePublishedNewYears() throws IOException{
		//the accumulation of the month lengths from 1970 BS agrees with every published 1 Baisakh
		Assertions.assertEquals(java.util.List.of(), NepaliCalendarStrategy.anchorMismatches());

		int anchors = 0;
		for(final String line : readTable(NepaliCalendarStrategy.class, "bikram-sambat-new-years.csv")){
			if(line.isBlank() || line.startsWith("#") || line.startsWith("year"))
				continue;

			final String[] fields = StringUtils.split(line, ';');
			final LocalDate date = LocalDate.parse(fields[1]);
			Assertions.assertEquals(gregorian(date.getYear(), date.getMonthValue(), date.getDayOfMonth()),
				CalendarType.NEPALI.parseToJdn("1 Baisakh " + fields[0]), fields[0]);
			anchors ++;
		}
		Assertions.assertEquals(19, anchors);

		//2096 BS has a valid length
		Assertions.assertEquals(CalendarType.NEPALI.parseToJdn("1 Baisakh 2097") - CalendarType.NEPALI.parseToJdn("1 Baisakh 2096"), 366L);
	}

	@Test
	void bahaiAyyamIHaFromTheTableAndTheNawRuzCrossCheck(){
		for(int year = 172; year <= 220; year ++){
			final long ayyamIHa = CalendarType.BAHAI.parseToJdn("1 Ayyam-i-Ha " + year + " BE");
			final long ala = CalendarType.BAHAI.parseToJdn("1 Ala " + year + " BE");
			final long expected = BahaiCalendarStrategy.nawRuzJdn(year + 1) - BahaiCalendarStrategy.nawRuzJdn(year) - 19 * 19;
			Assertions.assertEquals(expected, ala - ayyamIHa, "Ayyam-i-Ha of " + year + " BE");
		}
		//221 BE is the last year of the table: its Ayyam-i-Ha comes from the table, not from the Naw-Ruz of 222 BE
		Assertions.assertEquals(4L, CalendarType.BAHAI.parseToJdn("1 Ala 221 BE") - CalendarType.BAHAI.parseToJdn("1 Ayyam-i-Ha 221 BE"));
		Assertions.assertEquals(5L, CalendarType.BAHAI.parseToJdn("1 Ala 174 BE") - CalendarType.BAHAI.parseToJdn("1 Ayyam-i-Ha 174 BE"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.BAHAI.parseToJdn("5 Ayyam-i-Ha 221 BE"));
		Assertions.assertEquals(CalendarType.BAHAI.parseToJdn("1 Ala 174 BE") + 18,
			CalendarType.BAHAI.parseToJdn("19 Ala 174 BE"));
	}

}
