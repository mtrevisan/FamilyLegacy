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

import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;


/**
 * The rules of the Hebrew calendar: the dates of Rosh HaShanah against the reference implementation of ICU4J (79.1,
 * HebrewCalendar), the length of the years and of the months, and the days that do not exist.
 */
class HebrewCalendarRulesTest{

	private static final long UNIX_EPOCH_JDN = 2440588L;

	/** Rosh HaShanah (1 Tishrei) of the Hebrew years 5661 (1900) to 5861 (2100), as ISO dates (ICU4J HebrewCalendar). */
	private static final String[] ROSH_HASHANAH_5661_TO_5861 = {
		"1900-09-24", "1901-09-14", "1902-10-02", "1903-09-22", "1904-09-10", "1905-09-30", "1906-09-20", "1907-09-09",
		"1908-09-26", "1909-09-16", "1910-10-04", "1911-09-23", "1912-09-12", "1913-10-02", "1914-09-21", "1915-09-09",
		"1916-09-28", "1917-09-17", "1918-09-07", "1919-09-25", "1920-09-13", "1921-10-03", "1922-09-23", "1923-09-11",
		"1924-09-29", "1925-09-19", "1926-09-09", "1927-09-27", "1928-09-15", "1929-10-05", "1930-09-23", "1931-09-12",
		"1932-10-01", "1933-09-21", "1934-09-10", "1935-09-28", "1936-09-17", "1937-09-06", "1938-09-26", "1939-09-14",
		"1940-10-03", "1941-09-22", "1942-09-12", "1943-09-30", "1944-09-18", "1945-09-08", "1946-09-26", "1947-09-15",
		"1948-10-04", "1949-09-24", "1950-09-12", "1951-10-01", "1952-09-20", "1953-09-10", "1954-09-28", "1955-09-17",
		"1956-09-06", "1957-09-26", "1958-09-15", "1959-10-03", "1960-09-22", "1961-09-11", "1962-09-29", "1963-09-19",
		"1964-09-07", "1965-09-27", "1966-09-15", "1967-10-05", "1968-09-23", "1969-09-13", "1970-10-01", "1971-09-20",
		"1972-09-09", "1973-09-27", "1974-09-17", "1975-09-06", "1976-09-25", "1977-09-13", "1978-10-02", "1979-09-22",
		"1980-09-11", "1981-09-29", "1982-09-18", "1983-09-08", "1984-09-27", "1985-09-16", "1986-10-04", "1987-09-24",
		"1988-09-12", "1989-09-30", "1990-09-20", "1991-09-09", "1992-09-28", "1993-09-16", "1994-09-06", "1995-09-25",
		"1996-09-14", "1997-10-02", "1998-09-21", "1999-09-11", "2000-09-30", "2001-09-18", "2002-09-07", "2003-09-27",
		"2004-09-16", "2005-10-04", "2006-09-23", "2007-09-13", "2008-09-30", "2009-09-19", "2010-09-09", "2011-09-29",
		"2012-09-17", "2013-09-05", "2014-09-25", "2015-09-14", "2016-10-03", "2017-09-21", "2018-09-10", "2019-09-30",
		"2020-09-19", "2021-09-07", "2022-09-26", "2023-09-16", "2024-10-03", "2025-09-23", "2026-09-12", "2027-10-02",
		"2028-09-21", "2029-09-10", "2030-09-28", "2031-09-18", "2032-09-06", "2033-09-24", "2034-09-14", "2035-10-04",
		"2036-09-22", "2037-09-10", "2038-09-30", "2039-09-19", "2040-09-08", "2041-09-26", "2042-09-15", "2043-10-05",
		"2044-09-22", "2045-09-12", "2046-10-01", "2047-09-21", "2048-09-08", "2049-09-27", "2050-09-17", "2051-09-07",
		"2052-09-24", "2053-09-13", "2054-10-03", "2055-09-23", "2056-09-11", "2057-09-29", "2058-09-19", "2059-09-08",
		"2060-09-25", "2061-09-15", "2062-10-05", "2063-09-24", "2064-09-11", "2065-10-01", "2066-09-20", "2067-09-10",
		"2068-09-27", "2069-09-16", "2070-09-06", "2071-09-24", "2072-09-13", "2073-10-02", "2074-09-22", "2075-09-10",
		"2076-09-28", "2077-09-18", "2078-09-08", "2079-09-26", "2080-09-14", "2081-10-04", "2082-09-24", "2083-09-13",
		"2084-09-30", "2085-09-20", "2086-09-09", "2087-09-27", "2088-09-16", "2089-09-05", "2090-09-25", "2091-09-13",
		"2092-10-02", "2093-09-21", "2094-09-11", "2095-09-29", "2096-09-17", "2097-09-07", "2098-09-27", "2099-09-15",
		"2100-10-04"};


	private static LocalDate date(final long jdn){
		return LocalDate.ofEpochDay(jdn - UNIX_EPOCH_JDN);
	}

	private static long hebrew(final String text){
		return CalendarType.HEBREW.parseToJdn(text);
	}


	@Test
	void roshHashanahMatchesTheReferenceFrom1900To2100(){
		Assertions.assertEquals(201, ROSH_HASHANAH_5661_TO_5861.length);
		for(int i = 0; i < ROSH_HASHANAH_5661_TO_5861.length; i ++){
			final int year = 5661 + i;
			Assertions.assertEquals(LocalDate.parse(ROSH_HASHANAH_5661_TO_5861[i]), date(hebrew("1 Tishrei " + year)),
				"Rosh HaShanah " + year);
		}
	}

	@Test
	void roshHashanah5750IsThe30thOfSeptember1989(){
		//the year the old molad constant gave 2 October
		Assertions.assertEquals(LocalDate.of(1989, 9, 30), date(hebrew("1 Tishrei 5750")));
	}

	@Test
	void yearLengthsAndWeekdaysOfRoshHashanah(){
		final Set<Long> lengths = Set.of(353L, 354L, 355L, 383L, 384L, 385L);
		for(int year = 1; year <= 6000; year ++){
			final long start = hebrew("1 Tishrei " + year);
			final long length = hebrew("1 Tishrei " + (year + 1)) - start;
			Assertions.assertTrue(lengths.contains(length), "year " + year + " has " + length + " days");

			final boolean leap = (7 * year + 1) % 19 < 7;
			Assertions.assertEquals(leap, length > 380, "year " + year);

			final DayOfWeek weekday = date(start).getDayOfWeek();
			Assertions.assertTrue(weekday != DayOfWeek.SUNDAY && weekday != DayOfWeek.WEDNESDAY && weekday != DayOfWeek.FRIDAY,
				"Rosh HaShanah " + year + " is a " + weekday);
		}
	}

	@Test
	void monthsFillTheYear(){
		//Elul (the last month, 29 days) ends the day before the next Rosh HaShanah, in common and leap years
		for(final int year : new int[]{5784, 5785, 5786, 5787, 5788, 5790}){
			Assertions.assertEquals(hebrew("1 Tishrei " + (year + 1)) - 1, hebrew("29 Elul " + year), "year " + year);
			Assertions.assertThrows(IllegalArgumentException.class, () -> hebrew("30 Elul " + year));
		}
	}

	@Test
	void variableMonthsFollowTheLengthOfTheYear(){
		//5784 is deficient (383 days): Cheshvan and Kislev have 29 days
		Assertions.assertThrows(IllegalArgumentException.class, () -> hebrew("30 Cheshvan 5784"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> hebrew("30 Kislev 5784"));
		Assertions.assertEquals(hebrew("1 Kislev 5784") - 1, hebrew("29 Cheshvan 5784"));
		//5785 is complete (355 days): both have 30 days
		Assertions.assertEquals(hebrew("1 Kislev 5785") - 1, hebrew("30 Cheshvan 5785"));
		Assertions.assertEquals(hebrew("1 Tevet 5785") - 1, hebrew("30 Kislev 5785"));
		//5786 is regular (354 days): Cheshvan has 29 and Kislev 30
		Assertions.assertThrows(IllegalArgumentException.class, () -> hebrew("30 Cheshvan 5786"));
		Assertions.assertEquals(hebrew("1 Tevet 5786") - 1, hebrew("30 Kislev 5786"));
	}

	@Test
	void monthsOf29Days(){
		for(final String month : new String[]{"Tevet", "Iyar", "Tammuz", "Elul"})
			Assertions.assertThrows(IllegalArgumentException.class, () -> hebrew("30 " + month + " 5785"), month);
		for(final String month : new String[]{"Tishrei", "Shevat", "Nisan", "Sivan", "Av"})
			Assertions.assertDoesNotThrow(() -> hebrew("30 " + month + " 5785"), month);
		Assertions.assertThrows(IllegalArgumentException.class, () -> hebrew("31 Tishrei 5785"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> hebrew("0 Tishrei 5785"));
	}

	@Test
	void adar(){
		//a common year has a single Adar of 29 days
		Assertions.assertThrows(IllegalArgumentException.class, () -> hebrew("30 Adar 5785"));
		Assertions.assertEquals(LocalDate.of(2025, 3, 14), date(hebrew("14 Adar 5785")));
		//a leap year: Adar I has 30 days, Adar II 29, and the bare "Adar" is the second one (Purim, 24 March 2024)
		Assertions.assertEquals(LocalDate.of(2024, 2, 23), date(hebrew("14 Adar I 5784")));
		Assertions.assertDoesNotThrow(() -> hebrew("30 Adar I 5784"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> hebrew("30 Adar II 5784"));
		Assertions.assertEquals(LocalDate.of(2024, 3, 24), date(hebrew("14 Adar II 5784")));
		Assertions.assertEquals(LocalDate.of(2024, 3, 24), date(hebrew("14 Adar 5784")));
		Assertions.assertEquals(LocalDate.of(2024, 3, 24), date(hebrew("14 ve'adar 5784")));
		Assertions.assertEquals(LocalDate.of(2024, 3, 24), date(hebrew("14 Ve’Adar 5784")));
		//there is no second Adar in a common year
		Assertions.assertThrows(IllegalArgumentException.class, () -> hebrew("14 Adar II 5785"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> hebrew("14 ve'adar 5785"));
	}

	@Test
	void notValidDatesAreRejected(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> hebrew(null));
		Assertions.assertThrows(IllegalArgumentException.class, () -> hebrew(""));
		Assertions.assertThrows(IllegalArgumentException.class, () -> hebrew("   "));
		Assertions.assertThrows(IllegalArgumentException.class, () -> hebrew("1 Tishrei 0"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> hebrew("0"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> hebrew("1 Tishrei 99999999999"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> hebrew("1 Foo 5785"));
	}

	@Test
	void aSeasonWithADayIsAnError(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> hebrew("5 Tekufat Tevet 5784"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> hebrew("5 horef 5784"));
	}

	@Test
	void seasonsAreAnchoredOnTheGregorianSeason(){
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2023, 9, 23), hebrew("Tekufat Tishrei 5784"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2023, 12, 22), hebrew("Tekufat Tevet 5784"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2024, 3, 20), hebrew("Tekufat Nisan 5784"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2024, 6, 20), hebrew("Tekufat Tammuz 5784"));
	}

}
