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
package io.github.mtrevisan.familylegacy.ui.tools.dates;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.readers.date.JulianDay;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.chrono.HijrahDate;
import java.time.chrono.JapaneseDate;
import java.time.chrono.MinguoDate;
import java.time.chrono.ThaiBuddhistDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;


/**
 * The conversion of a date between the calendars of the Calendar Converter, through the Julian Day Number: every conversion
 * goes through it. The Gregorian and the Julian calendars are the proleptic ones: the reform of 1582 is not applied, so the
 * Gregorian date of a day before 15 October 1582 is the one the calendar would have given if it had existed.
 */
final class CalendarConversions{

	/** The calendars of the converter, in the order they are shown. */
	enum Calendar{

		GREGORIAN,
		JULIAN,
		HIJRAH,
		MINGUO,
		THAI_BUDDHIST,
		JAPANESE;


		/** @return	The name of the calendar, for the user. */
		String label(){
			return I18N.t("label.calendar." + name().toLowerCase(Locale.ROOT));
		}

		@Override
		public String toString(){
			return label();
		}

	}


	private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;


	private CalendarConversions(){}


	/**
	 * @param calendar	The calendar of the date.
	 * @param year	The year.
	 * @param month	The month.
	 * @param day	The day.
	 * @return	The Julian Day Number of the day.
	 * @throws IllegalArgumentException	If the date does not exist in the calendar.
	 */
	static long toJdn(final Calendar calendar, final int year, final int month, final int day){
		return switch(calendar){
			case GREGORIAN -> {
				try{
					LocalDate.of(year, month, day);
				}
				catch(final Exception ignored){
					throw invalid(calendar);
				}
				yield gregorianToJdn(year, month, day);
			}
			case JULIAN -> {
				final long jdn = julianToJdn(year, month, day);
				//a date that does not exist (30 February) does not come back unchanged
				final int[] back = jdnToJulian(jdn);
				if(month < 1 || month > 12 || back[0] != year || back[1] != month || back[2] != day)
					throw invalid(calendar);
				yield jdn;
			}
			case HIJRAH -> fromChrono(calendar, () -> LocalDate.from(HijrahDate.of(year, month, day)));
			case MINGUO -> fromChrono(calendar, () -> LocalDate.from(MinguoDate.of(year, month, day)));
			case THAI_BUDDHIST -> fromChrono(calendar, () -> LocalDate.from(ThaiBuddhistDate.of(year, month, day)));
			case JAPANESE -> fromChrono(calendar, () -> LocalDate.from(JapaneseDate.of(year, month, day)));
		};
	}

	/**
	 * @param calendar	The calendar to write the date in.
	 * @param jdn	The Julian Day Number of the day.
	 * @return	The date, as {@code YYYY-MM-DD} for the Gregorian and the Julian calendars and as the JDK writes it for the others.
	 * @throws IllegalArgumentException	If the day cannot be represented in the calendar.
	 */
	static String fromJdn(final Calendar calendar, final long jdn){
		if(calendar == Calendar.JULIAN){
			final int[] ymd = jdnToJulian(jdn);
			return String.format("%04d-%02d-%02d", ymd[0], ymd[1], ymd[2]);
		}

		final LocalDate iso = toLocalDate(jdn);
		try{
			return switch(calendar){
				case GREGORIAN -> iso.format(ISO);
				case HIJRAH -> HijrahDate.from(iso).toString();
				case MINGUO -> MinguoDate.from(iso).toString();
				case THAI_BUDDHIST -> ThaiBuddhistDate.from(iso).toString();
				case JAPANESE -> JapaneseDate.from(iso).toString();
				case JULIAN -> throw new IllegalStateException();
			};
		}
		catch(final RuntimeException ignored){
			throw outOfRange(calendar);
		}
	}

	/** @return	The name of the day of the week of the day, in the language of the user. */
	static String weekday(final long jdn){
		final DayOfWeek day = toLocalDate(jdn)
			.getDayOfWeek();
		return day.getDisplayName(TextStyle.FULL, Locale.getDefault());
	}


	private static LocalDate toLocalDate(final long jdn){
		final int[] ymd = JulianDay.toGregorian(jdn);
		try{
			return LocalDate.of(ymd[0], ymd[1], ymd[2]);
		}
		catch(final RuntimeException ignored){
			throw outOfRange(Calendar.GREGORIAN);
		}
	}

	private static long fromChrono(final Calendar calendar, final java.util.function.Supplier<LocalDate> date){
		final LocalDate iso;
		try{
			iso = date.get();
		}
		catch(final RuntimeException ignored){
			throw invalid(calendar);
		}
		return gregorianToJdn(iso.getYear(), iso.getMonthValue(), iso.getDayOfMonth());
	}

	private static IllegalArgumentException invalid(final Calendar calendar){
		return new IllegalArgumentException(I18N.tf("text.calendar.invalid_date", calendar.label()));
	}

	private static IllegalArgumentException outOfRange(final Calendar calendar){
		return new IllegalArgumentException(I18N.tf("text.calendar.out_of_range", calendar.label()));
	}


	/** The Julian Day Number of a date of the proleptic Gregorian calendar (Meeus). */
	static long gregorianToJdn(final int year, final int month, final int day){
		final int a = (14 - month) / 12;
		final int y = year + 4800 - a;
		final int m = month + 12 * a - 3;
		return day + (153L * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045L;
	}

	/** The Julian Day Number of a date of the proleptic Julian calendar. */
	static long julianToJdn(final int year, final int month, final int day){
		final int a = (14 - month) / 12;
		final int y = year + 4800 - a;
		final int m = month + 12 * a - 3;
		return day + (153L * m + 2) / 5 + 365L * y + y / 4 - 32083L;
	}

	/** @return	The year, month and day of the proleptic Julian calendar. */
	static int[] jdnToJulian(final long jdn){
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
