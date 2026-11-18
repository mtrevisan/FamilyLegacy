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
package io.github.mtrevisan.familylegacy.ui.components.dates;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** The table of the Calendar Conversion tool: what it writes is what the Calendars module reads back, on every day it writes. */
class CalendarTableTest{

	private static final Pattern DATE = Pattern.compile("(?:(\\p{L}+) )?(\\d+)-(\\d+)-(\\d+)");
	private static final String[] ISLAMIC_MONTHS = {"muharram", "safar", "rabi al-awwal", "rabi al-thani", "jumada al-awwal",
		"jumada al-thani", "rajab", "shaban", "ramadan", "shawwal", "dhu al-qadah", "dhu al-hijjah"};
	private static final String[] THAI_MONTHS = {"มกราคม", "กุมภาพันธ์",
		"มีนาคม", "เมษายน", "พฤษภาคม",
		"มิถุนายน", "กรกฎาคม", "สิงหาคม",
		"กันยายน", "ตุลาคม", "พฤศจิกายน",
		"ธันวาคม"};


	private static long jdn(final LocalDate date){
		return CalendarType.GREGORIAN.parseToJdn(date.getDayOfMonth() + " " + date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
			+ " " + date.getYear());
	}

	/**
	 * Writes the days of a range in a calendar of the table and reads each of them back with the Calendars module: it must be the same
	 * day.
	 */
	private static void assertRoundTrip(final CalendarTable.Calendar calendar, final CalendarType type, final LocalDate from,
			final LocalDate to, final int step, final BiFunction<String, int[], String> notation){
		int checked = 0;
		for(LocalDate day = from; day.isBefore(to); day = day.plusDays(step)){
			final long jdn = jdn(day);
			final String written = CalendarTable.write(calendar, jdn);
			final Matcher matcher = DATE.matcher(written);
			Assertions.assertTrue(matcher.find(), written);
			final int[] fields = {Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4))};
			final String text = notation.apply(matcher.group(1), fields);
			Assertions.assertEquals(jdn, type.parseToJdn(text), day + " is written " + written + ", read as " + text);
			checked ++;
		}
		Assertions.assertTrue(checked > 100);
	}


	@Test
	void theGregorianAndTheJulianCalendarsAreTheProlepticOnes(){
		final long jdn = jdn(LocalDate.of(1582, 10, 15));

		//the day after the 4th of October 1582 of the Julian calendar is the 15th of October of the Gregorian one
		Assertions.assertEquals("1582-10-15", CalendarTable.write(CalendarTable.Calendar.GREGORIAN, jdn));
		Assertions.assertEquals("1582-10-05", CalendarTable.write(CalendarTable.Calendar.JULIAN, jdn));
		//the J2000 epoch
		Assertions.assertEquals("2000-01-01", CalendarTable.write(CalendarTable.Calendar.GREGORIAN, 2_451_545L));
	}

	@Test
	void theTableHasACalendarPerRowThenTheJulianDayNumberAndTheWeekday(){
		final long jdn = jdn(LocalDate.of(1979, 11, 21));

		final List<Object[]> rows = CalendarTable.rows(jdn, "islamic");

		Assertions.assertEquals(CalendarTable.Calendar.values().length + 2, rows.size());
		for(final Object[] row : rows)
			Assertions.assertFalse(String.valueOf(row[1]).isBlank(), String.valueOf(row[0]));
		//the calendar the date was entered in is marked
		Assertions.assertTrue(((String)rows.get(CalendarTable.Calendar.HIJRAH.ordinal())[0]).endsWith("*"));
		Assertions.assertFalse(((String)rows.get(CalendarTable.Calendar.JULIAN.ordinal())[0]).endsWith("*"));
		Assertions.assertEquals(Long.toString(jdn), rows.get(rows.size() - 2)[1]);
		Assertions.assertEquals(DayOfWeek.WEDNESDAY.getDisplayName(TextStyle.FULL, Locale.getDefault()), rows.getLast()[1]);
	}

	@Test
	void theWeekdayIsInTheLanguageOfTheUser(){
		Assertions.assertEquals(DayOfWeek.SATURDAY.getDisplayName(TextStyle.FULL, Locale.getDefault()), CalendarTable.weekday(2_451_545L));
	}

	@Test
	void aDayOutOfTheRangeOfACalendarIsSaidAndDoesNotHideTheOthers(){
		//the Japanese years start from 1873
		final List<Object[]> rows = CalendarTable.rows(jdn(LocalDate.of(1800, 1, 1)), "gregorian");

		Assertions.assertEquals("1800-01-01", rows.get(CalendarTable.Calendar.GREGORIAN.ordinal())[1]);
		Assertions.assertEquals(I18N.tf("text.calendar.out_of_range", CalendarTable.Calendar.JAPANESE.label()),
			rows.get(CalendarTable.Calendar.JAPANESE.ordinal())[1]);
	}


	@Test
	void theHijrahIsTheUmmAlQuraOfTheCalendarsModule(){
		assertRoundTrip(CalendarTable.Calendar.HIJRAH, CalendarType.ISLAMIC, LocalDate.of(1900, 1, 1), LocalDate.of(2070, 1, 1), 3,
			(era, f) -> f[2] + " " + ISLAMIC_MONTHS[f[1] - 1] + " " + f[0]);
	}

	@Test
	void theMinguoYearsAreTheOnesOfTheCalendarsModule(){
		assertRoundTrip(CalendarTable.Calendar.MINGUO, CalendarType.MINGUO, LocalDate.of(1912, 1, 1), LocalDate.of(2100, 1, 1), 5,
			(era, f) -> f[0] + "." + f[1] + "." + f[2]);
	}

	@Test
	void theJapaneseYearsAreTheOnesOfTheCalendarsModule(){
		assertRoundTrip(CalendarTable.Calendar.JAPANESE, CalendarType.JAPANESE, LocalDate.of(1873, 1, 1), LocalDate.of(2100, 1, 1), 5,
			(era, f) -> era + " " + f[0] + "." + f[1] + "." + f[2]);
	}

	@Test
	void theThaiBuddhistYearStartsOnTheFirstOfAprilUntil1940(){
		final BiFunction<String, int[], String> notation = (era, f) -> f[2] + " " + THAI_MONTHS[f[1] - 1] + " " + f[0];

		assertRoundTrip(CalendarTable.Calendar.THAI_BUDDHIST, CalendarType.BUDDHIST, LocalDate.of(1941, 1, 1), LocalDate.of(2100, 1, 1), 5,
			notation);
		//before 1941 the JDK would write January to March in the year after: the table writes them as the Calendars module reads them
		assertRoundTrip(CalendarTable.Calendar.THAI_BUDDHIST, CalendarType.BUDDHIST, LocalDate.of(1900, 1, 1), LocalDate.of(1940, 12, 31), 7,
			notation);
		Assertions.assertTrue(CalendarTable.write(CalendarTable.Calendar.THAI_BUDDHIST, jdn(LocalDate.of(1940, 1, 1))).contains("BE 2482-01-01"));
		Assertions.assertTrue(CalendarTable.write(CalendarTable.Calendar.THAI_BUDDHIST, jdn(LocalDate.of(1941, 1, 1))).contains("BE 2484-01-01"));
	}

}
