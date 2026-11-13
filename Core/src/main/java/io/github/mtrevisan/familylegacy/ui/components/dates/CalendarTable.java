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

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.readers.date.JulianDay;
import org.apache.commons.lang3.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.chrono.HijrahDate;
import java.time.chrono.JapaneseDate;
import java.time.chrono.MinguoDate;
import java.time.chrono.ThaiBuddhistDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * The same day written in the calendars of the table of the Calendar Conversion tool, through its Julian Day Number.
 * <p>
 * The Gregorian and the Julian calendars are the proleptic ones: the reform of 1582 is not applied, so the Gregorian date of a day
 * before 15 October 1582 is the one the calendar would have given if it had existed. The Hijrah is the Umm al-Qura one (from 1882
 * to 2174), the Japanese one is written in the years of the era from 1873 (before that the months were lunisolar), the Minguo one
 * counts the years from 1912. The Thai Buddhist year began on 1 April until 1940, as the {@code Calendars} module reads it.
 * </p>
 * The dates of these calendars agree, on every day of the range they are written for, with what the {@code Calendars} module reads
 * (see {@code CalendarTableTest}).
 */
final class CalendarTable{

	/** The first day with the Thai Buddhist year starting on 1 January (2484 BE). */
	private static final LocalDate THAI_JANUARY_NEW_YEAR = LocalDate.of(1941, 1, 1);
	private static final int THAI_OFFSET = 543;
	private static final int APRIL = 4;

	/** The calendars of the table, in the order they are shown, each with the code of the calendar of the sources it is the same as. */
	enum Calendar{

		GREGORIAN("gregorian"),
		JULIAN("julian"),
		HIJRAH("islamic"),
		MINGUO("minguo"),
		THAI_BUDDHIST("buddhist"),
		JAPANESE("japanese");


		private final String code;


		Calendar(final String code){
			this.code = code;
		}

		/** @return	The name of the calendar, for the user. */
		String label(){
			return I18N.t("label.calendar." + name().toLowerCase(Locale.ROOT));
		}

	}


	private CalendarTable(){}


	/**
	 * @param jdn	The Julian Day Number of the day.
	 * @param sourceCode	The code of the calendar the date was entered in: its row is marked with an asterisk.
	 * @return	The rows of the table: a calendar and the date for each of them, then the Julian Day Number and the day of the week.
	 */
	static List<Object[]> rows(final long jdn, final String sourceCode){
		final List<Object[]> rows = new ArrayList<>();
		for(final Calendar calendar : Calendar.values()){
			String date;
			try{
				date = write(calendar, jdn);
			}
			catch(final IllegalArgumentException iae){
				//a calendar that cannot write this day does not hide the others
				date = iae.getMessage();
			}
			rows.add(new Object[]{calendar.label() + (calendar.code.equals(sourceCode)? " *": StringUtils.EMPTY), date});
		}
		rows.add(new Object[]{I18N.t("label.julian_day_number"), Long.toString(jdn)});
		rows.add(new Object[]{I18N.t("label.weekday"), weekday(jdn)});
		return rows;
	}

	/**
	 * @param calendar	The calendar to write the date in.
	 * @param jdn	The Julian Day Number of the day.
	 * @return	The date, as {@code YYYY-MM-DD} for the Gregorian and the Julian calendars and as the JDK writes it for the others.
	 * @throws IllegalArgumentException	If the day cannot be represented in the calendar.
	 */
	static String write(final Calendar calendar, final long jdn){
		if(calendar == Calendar.JULIAN){
			final int[] ymd = julianFields(jdn);
			return String.format("%04d-%02d-%02d", ymd[0], ymd[1], ymd[2]);
		}

		final LocalDate iso = toLocalDate(jdn);
		try{
			return switch(calendar){
				case GREGORIAN -> iso.toString();
				case HIJRAH -> HijrahDate.from(iso)
					.toString();
				case MINGUO -> MinguoDate.from(iso)
					.toString();
				case THAI_BUDDHIST -> thaiBuddhist(iso);
				case JAPANESE -> JapaneseDate.from(iso)
					.toString();
				case JULIAN -> throw new IllegalStateException();
			};
		}
		catch(final RuntimeException ignored){
			throw new IllegalArgumentException(I18N.tf("text.calendar.out_of_range", calendar.label()));
		}
	}

	/** @return	The name of the day of the week of the day, in the language of the user. */
	static String weekday(final long jdn){
		final DayOfWeek day = toLocalDate(jdn)
			.getDayOfWeek();
		return day.getDisplayName(TextStyle.FULL, Locale.getDefault());
	}


	/** The JDK counts the Thai year from 1 January always: before 1941 the year began on 1 April, so January to March belong to the one before. */
	private static String thaiBuddhist(final LocalDate iso){
		if(!iso.isBefore(THAI_JANUARY_NEW_YEAR))
			return ThaiBuddhistDate.from(iso)
				.toString();

		final int year = iso.getYear() + THAI_OFFSET - (iso.getMonthValue() < APRIL? 1: 0);
		return String.format("ThaiBuddhist BE %d-%02d-%02d", year, iso.getMonthValue(), iso.getDayOfMonth())
			+ StringUtils.SPACE + I18N.t("text.calendar.thai_april_year");
	}

	private static LocalDate toLocalDate(final long jdn){
		final int[] ymd = JulianDay.toGregorian(jdn);
		try{
			return LocalDate.of(ymd[0], ymd[1], ymd[2]);
		}
		catch(final RuntimeException ignored){
			throw new IllegalArgumentException(I18N.tf("text.calendar.out_of_range", Calendar.GREGORIAN.label()));
		}
	}

	/** @return	The year, month and day of the proleptic Julian calendar. */
	private static int[] julianFields(final long jdn){
		final long c = jdn + 32082;
		final long d = (4 * c + 3) / 1461;
		final long e = c - (1461 * d) / 4;
		final long m = (5 * e + 2) / 153;
		final int day = (int)(e - (153 * m + 2) / 5 + 1);
		final int month = (int)(m + 3 - 12 * (m / 10));
		final int year = (int)(d - 4800 + m / 10);
		return new int[]{year, month, day};
	}

}
