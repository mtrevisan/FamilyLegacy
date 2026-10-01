/**
 * Copyright (c) 2023 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.ephemeris.helpers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;


/**
 * <The Julian Date is the number for the preceding noon plus the fraction of the day (counting 86400 seconds) since that instant.
 *
 * <p>
 * A Julian Date number is defined as continuous number associated with the solar day and is zero at Greenwich mean noon on 1st of January
 * 4713 BC.
 * </p>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Julian_day">Julian day</a>
 * @see <a href="https://calendars.fandom.com/wiki/Julian_day_number">Julian day number</a>
 * @see <a href="https://www.aa.quae.nl/en/reken/juliaansedag.html">Astronomy answers - Julian Day Number</a>
 */
public final class JulianDate{

	private static final int[] F = {428, 459, 122, 153, 183, 214, 244, 275, 306, 336, 367, 397};

	/** Last day of the Julian calendar, that is October 4, 1582, after which 10 day were added [day]. */
	private static final double JULIAN_CALENDAR_END = 2_299_160.;
	private static final int GREGOR_XIII_REFORM_YEAR = 1582;

	public static final double MJD = 2_400_000.5;
	/** 1.5 Jan 2000 UT - Julian epoch. */
	static final double J2000 = 2_451_545.;

	public static final double CIVIL_SAECULUM = 36_525.;
	public static final double CIVIL_MILLENNIUM = CIVIL_SAECULUM * 10.;
	//[°/h]
	public static final double DEGREES_PER_HOUR = 15.;
	public static final double HOURS_PER_DAY = 24.;
	public static final double MINUTES_PER_HOUR = 60.;
	public static final double SECONDS_PER_HOUR = 3600.;
	public static final double MINUTES_PER_DAY = MINUTES_PER_HOUR * HOURS_PER_DAY;
	public static final double SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY;
	public static final double MILLISECONDS_PER_DAY = SECONDS_PER_DAY * 1000.;


	/**
	 * Calculate Julian Date at 0 UTC.
	 *
	 * @param date	The date.
	 * @return	The Julian Date [day].
	 */
	public static double of(final LocalDateTime date){
		final double jdNoon = of(date.toLocalDate());
		final double time = timeOf(date);
		return jdNoon + time;
	}

	public static double timeOf(final LocalDateTime date){
		return timeOf(date.toLocalTime());
	}

	public static double timeOf(final LocalTime time){
		return time.toSecondOfDay() / SECONDS_PER_DAY;
	}

	/**
	 * Calculate Julian Date at 0 UTC.
	 *
	 * @param date	The date.
	 * @return	The Julian Date [day].
	 */
	public static double of(final ZonedDateTime date){
		return of(LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC));
	}

	/**
	 * Calculate Julian Date at 0 UTC.
	 *
	 * @param date	The date.
	 * @return	The Julian Date [day].
	 */
	public static double of(final LocalDate date){
		return of(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
	}

	/**
	 * Calculate Julian Date at 0 UTC.
	 *
	 * @param year	The year.
	 * @param month	The month (1 is January).
	 * @param day	The day.
	 * @return	The Julian Date [day].
	 */
	public static double of(final int year, final int month, final int day){
		final int z = year + (month - 14) / 12;
		int jd =  (int)(365.25 * (z + 4716)) + F[month - 1] + day;
		if(jd > JULIAN_CALENDAR_END){
			//number of full centuries
			final int aa = z / 100;
			//days within the whole centuries (in the Julian Calendar) adding back days removed in the Gregorian Calendar
			jd += 2 - aa + (aa / 4);
		}
		return jd - 1524.5;
	}

	/**
	 * Calculated the Julian Century since JDE2451545 TT, that is J2000.0.
	 *
	 * @param jd	The Julian Date [day].
	 * @return	The Julian Century.
	 */
	public static double centuryJ2000Of(final double jd){
		return (jd - J2000) / CIVIL_SAECULUM;
	}

	/**
	 * Calculated the Julian Millennium since JDE2451545 TT, that is J2000.0.
	 *
	 * @param jd	The Julian Date [day].
	 * @return	The Julian Century.
	 */
	public static double millenniumJ2000Of(final double jd){
		return (jd - J2000) / CIVIL_MILLENNIUM;
	}

	/**
	 * Calculated the Modified Julian Date starting at midnight since JDE2451545, that is J2000.0.
	 *
	 * @param jd	The Julian Date [day].
	 * @return	The Modified Julian Date starting at midnight.
	 */
	public static double mjdOf(final double jd){
		return jd - MJD;
	}


	public static LocalDateTime dateTimeOf(final double jd){
		long b = (long)StrictMath.floor(jd + 0.5);
		final double timeOfDay = jd - b + 0.5;

		if(b > JULIAN_CALENDAR_END){
			final int aa = (int)StrictMath.floor((b - 1867216.25) / 36524.25);
			b += 1 + aa - (int)StrictMath.floor(aa * 0.25);
		}
		b += 1524;
		//year in a calendar whose years start on March 1
		int year = (int)StrictMath.floor((b - 122.1) / 365.25);
		b -=StrictMath.floor(365.25 * year);
		int month = (int)StrictMath.floor(b / 30.6) - 1;
		final int day = (int)(b - StrictMath.floor(30.6 * (month + 1)));
		month = ((month - 1) % 12) + 1;
		if(month > 2)
			year --;
		year -= 4715;

		final LocalDate date = LocalDate.of(year, month, day);
		final LocalTime time = LocalTime.ofSecondOfDay(Math.round(timeOfDay * SECONDS_PER_DAY));
		return LocalDateTime.of(date, time);
	}

	public static int getYear(final double jd){
		long b = (long)StrictMath.floor(jd + 0.5);

		if(b > JULIAN_CALENDAR_END){
			final int aa = (int)StrictMath.floor((b - 1867216.25) / 36524.25);
			b += 1 + aa - (int)StrictMath.floor(aa * 0.25);
		}
		b += 1524;
		//year in a calendar whose years start on March 1
		int year = (int)StrictMath.floor((b - 122.1) / 365.25);
		b -= StrictMath.floor(365.25 * year);
		final int month = (int)StrictMath.floor(b / 30.6) - 1;
		if((month - 1) % 12 > 1)
			year --;
		year -= 4715;

		return year;
	}


	public static boolean isLeapYear(final int year){
		return ((year & 0x03) == 0 && (year < GREGOR_XIII_REFORM_YEAR || (year % 100) != 0 || (year % 400) == 0));
	}

	public static double[] extractDateAndTime(final double jd){
		//calculate time of day [day]
		final double time = ((jd - 0.5) % 1);
		//calculate UT at 0h [day]
		final double date = (long)jd - 0.5;
		return new double[]{date, time};
	}

}
