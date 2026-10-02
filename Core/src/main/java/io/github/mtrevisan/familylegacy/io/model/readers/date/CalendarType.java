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


import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;


/**
 * Supported calendar systems and their conversion algorithms to Julian Day Number (JDN).
 */
public enum CalendarType implements CalendarConverterPlugin{

	GREGORIAN("gregorian"){
		@Override
		public long parseToJdn(final String dateExpression, final int baseContextAnchor){
			final GenealogicalDate date = UniversalDateConverter.parse(getCode(), dateExpression);
			final LocalDate localDate = date.isoDate();
			final int year = localDate.getYear();
			final int month = localDate.getMonthValue();
			final int day = localDate.getDayOfMonth();

			final long a = (14 - month) / 12;
			final long y = year + 4800L - a;
			final long m = month + 12L * a - 3;
			return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
		}
	},
	JULIAN("julian"){
		@Override
		public long parseToJdn(final String dateExpression, final int baseContextAnchor){
			final GenealogicalDate date = UniversalDateConverter.parse(getCode(), dateExpression);
			final LocalDate localDate = date.isoDate();
			final int year = localDate.getYear();
			final int month = localDate.getMonthValue();
			final int day = localDate.getDayOfMonth();

			final long a = (14 - month) / 12;
			final long y = year + 4800L - a;
			final long m = month + 12L * a - 3;
			return day + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083;
		}
	},
	REFORMED_JULIAN("reformed_julian"){
		@Override
		public long parseToJdn(final String dateExpression, final int baseContextAnchor){
			final GenealogicalDate date = UniversalDateConverter.parse(getCode(), dateExpression);
			final LocalDate localDate = date.isoDate();
			final int year = localDate.getYear();
			final int month = localDate.getMonthValue();
			final int day = localDate.getDayOfMonth();

			final long a = (14 - month) / 12;
			final long y = year + 4800L - a;
			final long m = month + 12L * a - 3;
			final long century = y / 100;
			final long leapCenturies = (century * 2 + 2) / 9;
			return day + (153 * m + 2) / 5 + 365 * y + y / 4 - century + leapCenturies - 32022;
		}
	},
	ISLAMIC("islamic"){
		@Override
		public long parseToJdn(final String dateExpression, final int baseContextAnchor){
			// FIXME Using astronomical calculations for the position of the sun and moon, the moon's illumination, and other factors, it is possible to determine the start of a lunar month with a fairly high degree of certainty
			final GenealogicalDate date = UniversalDateConverter.parse(getCode(), dateExpression);
			final LocalDate localDate = date.isoDate();
			final int year = localDate.getYear();
			final int month = localDate.getMonthValue();
			final int day = localDate.getDayOfMonth();

			return day
				+ (long)Math.ceil(29.5 * (month - 1))
				+ (year - 1L) * 354L
				+ (long)Math.floor((3 + 11. * year) / 30.)
				+ 1948440L - 1L;
		}
	},
	BUDDHIST("buddhist"){
		@Override
		public long parseToJdn(final String dateExpression, final int baseContextAnchor){
			final GenealogicalDate date = UniversalDateConverter.parse(getCode(), dateExpression);
			final LocalDate localDate = date.isoDate();
			final int year = localDate.getYear();
			final int month = localDate.getMonthValue();
			final int day = localDate.getDayOfMonth();

			// Solar Buddhist calendar uses Gregorian arithmetic with a +543 year offset (BE = CE + 543)
			return GREGORIAN.parseToJdn(day + StringUtils.SPACE + month + StringUtils.SPACE + (year - 543), baseContextAnchor);
		}
	},
	COPTIC("coptic"){
		@Override
		public long parseToJdn(final String dateExpression, final int baseContextAnchor){
			final GenealogicalDate date = UniversalDateConverter.parse(getCode(), dateExpression);
			final LocalDate localDate = date.isoDate();
			final int year = localDate.getYear();
			final int month = localDate.getMonthValue();
			final int day = localDate.getDayOfMonth();

			return 1825030L
				+ 365L * (year - 1L)
				+ (year / 4L)
				+ 30L * (month - 1L)
				+ (day - 1L);
		}
	},
	ETHIOPIAN("ethiopian"){
		@Override
		public long parseToJdn(final String dateExpression, final int baseContextAnchor){
			final GenealogicalDate date = UniversalDateConverter.parse(getCode(), dateExpression);
			final LocalDate localDate = date.isoDate();
			final int year = localDate.getYear();
			final int month = localDate.getMonthValue();
			final int day = localDate.getDayOfMonth();

			return 1724221L
				+ 365L * (year - 1L)
				+ (year / 4L)
				+ 30L * (month - 1L)
				+ (day - 1L);
		}
	},

	HEBREW("hebrew"){
		@Override
		public long parseToJdn(final String dateExpression, final int baseContextAnchor){
			// FIXME In the traditional Hebrew calendar, days start at sunset
			final GenealogicalDate date = UniversalDateConverter.parse(getCode(), dateExpression);
			final LocalDate localDate = date.isoDate();
			final int year = localDate.getYear();
			final int month = localDate.getMonthValue();
			final int day = localDate.getDayOfMonth();

			final long roshHaShanah = hebrewNewYear(year);
			return roshHaShanah + daysBeforeMonth(year, month) + (day - 1);
		}

		/**
		 * Determines if a Hebrew year is a leap year (13 months) in the 19-year Metonic cycle.
		 */
		private boolean isLeapYear(final int year){
			return ((1 + 7 * (year % 19)) % 19 < 7);
		}

		/**
		 * Computes the Julian Day Number for Rosh HaShanah (1 Tishrei) of a given Hebrew year.
		 * Applies the four Dehiyyot postponement rules.
		 */
		private long hebrewNewYear(final int year){
			final long monthsElapsed = (235L * year - 234L) / 19L;
			final long partsElapsed = 204L + 793L * (monthsElapsed % 1080L);
			final long hoursElapsed = 11L + 12L * monthsElapsed + 793L * (monthsElapsed / 1080L) + partsElapsed / 1080L;
			final long day = 1L + 29L * monthsElapsed + hoursElapsed / 24L;
			final long parts = (hoursElapsed % 24L) * 1080L + (partsElapsed % 1080L);

			long roshHaShanah = day;

			// Rule 1: Dehiyyat Molad Zaken (postpone if Molad occurs at or after 18:00)
			if(parts >= 19440L)
				roshHaShanah ++;

			// Rule 2: Dehiyyat ADU (postpone if new year falls on Sunday, Wednesday, or Friday)
			final long dayOfWeek = roshHaShanah % 7L;
			if(dayOfWeek == 0 || dayOfWeek == 3 || dayOfWeek == 5)
				roshHaShanah ++;

			// Re-check day of week after Rule 1 or Rule 2 adjustment
			final long newDayOfWeek = roshHaShanah % 7L;

			// Rule 3: Dehiyyat GATRAD (postpone in a non-leap year if Molad is on Tuesday at/after 9h 204p)
			if(newDayOfWeek == 2 && !isLeapYear(year) && parts >= 9924L)
				roshHaShanah += 2L;

			// Rule 4: Dehiyyat BETUAGOT (postpone in a year following a leap year if Molad is on Monday at/after 15h 589p)
			if(newDayOfWeek == 1 && isLeapYear(year - 1) && parts >= 16789L)
				roshHaShanah ++;

			// Epoch offset to convert Hebrew calendar day count to JDN (347997)
			return roshHaShanah + 347997L;
		}

		/**
		 * Computes the cumulative elapsed days in a Hebrew year before the given month.
		 */
		private int daysBeforeMonth(final int year, final int month){
			final long thisNY = hebrewNewYear(year);
			final long nextNY = hebrewNewYear(year + 1);
			final int yearLength = (int)(nextNY - thisNY);

			final boolean longHeshvan = (yearLength == 355 || yearLength == 385);
			final boolean shortKislev = (yearLength == 353 || yearLength == 383);
			final boolean leap = isLeapYear(year);

			int days = 0;
			// Hebrew months in civil order starting from Tishrei (month 1)
			for(int m = 1; m < month; m ++){
				days += switch(m){
					case 1 -> 30; // Tishrei
					case 2 -> (longHeshvan? 30: 29); // Cheshvan
					case 3 -> (shortKislev? 29: 30); // Kislev
					case 4 -> 29; // Tevet
					case 5 -> 30; // Shevat
					case 6 -> (leap? 30: 29); // Adar I (or Adar in regular years)
					case 7 -> (leap? 29: 30); // Adar II (or Nisan in regular years)
					case 8 -> 29; // Iyar
					case 9 -> 30; // Sivan
					case 10 -> 29; // Tammuz
					case 11 -> 30; // Av
					case 12 -> 29; // Elul
					default -> 30;
				};
			}
			return days;
		}
	},
	CHINESE("chinese"){
		/**
		 * Chinese Lunisolar Calendar.
		 * Uses Sunset astronomical engine for precise solar longitude (Zhongqi)
		 * and lunar conjunctions at Beijing mean time (UTC+8).
		 */
		@Override
		public long parseToJdn(final String dateExpression, final int baseContextAnchor){
			// Expected native format: "庚申年 正月 初一" or "庚申年 闰四月 廿二"
			// 1. Leverage your string parser to break down the native text
			final ChineseDateInput date = ChineseCalendarParser.parse(dateExpression, baseContextAnchor);
			final int year = date.getYear();
			final int month = date.getMonth();
			final boolean leapMonth = date.isLeapMonth();
			final int day = date.getDay();

			final double jdB = ChineseCalendarAstronomicalEngine.toJulianDateUT(year, month, leapMonth, day);

			return (long)Math.floor(jdB + 0.5);
		}
	},
	INDIAN("indian"){
		/**
		 * Indian National Calendar (Saka).
		 * Epoch: Saka 0 = 78 CE.
		 * Year starts on 1 Chaitra (March 21 in leap years, March 22 in common years).
		 */
		@Override
		public long parseToJdn(final String dateExpression, final int baseContextAnchor){
			final GenealogicalDate date = UniversalDateConverter.parse(getCode(), dateExpression);
			final LocalDate localDate = date.isoDate();
			final int year = localDate.getYear();
			final int month = localDate.getMonthValue();
			final int day = localDate.getDayOfMonth();

			final int gregorianYear = year + 78;
			final boolean isLeap = isGregorianLeapYear(gregorianYear);

			// Compute JDN of 1 Chaitra (start of the Saka year)
			final int chaitraDay = (isLeap? 21: 22);
			final long startOfYearJdn = GREGORIAN.parseToJdn(
				chaitraDay + StringUtils.SPACE + 3 + StringUtils.SPACE + gregorianYear, baseContextAnchor);

			// Days elapsed in preceding months
			int elapsedDays = 0;
			for(int m = 1; m < month; m ++)
				elapsedDays += switch(m){
					// Chaitra
					case 1 -> (isLeap? 31: 30);
					// Vaishakha to Bhadra
					case 2, 3, 4, 5, 6 -> 31;
					// Ashvina to Phalguna
					case 7, 8, 9, 10, 11, 12 -> 30;
					default -> 30;
				};

			return startOfYearJdn + elapsedDays + (day - 1);
		}

		private boolean isGregorianLeapYear(final int year){
			return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
		}
	},

	FRENCH_REPUBLICAN("french_republican"){
		@Override
		public long parseToJdn(final String dateExpression, final int baseContextAnchor){
			// FIXME to test
			final GenealogicalDate date = UniversalDateConverter.parse(getCode(), dateExpression);
			final LocalDate localDate = date.isoDate();
			final int year = localDate.getYear();
			final int month = localDate.getMonthValue();
			final int day = localDate.getDayOfMonth();

			// Arithmetic French Republican calendar (Romme method: leap years every 4 years starting Year 3)
			// Epoch: 22 September 1792 Gregorian (JDN 2375839)
			final long yearOffset = year - 1L;
			final long leapYears = (yearOffset + 1L) / 4L;
			return 2375839L + 365L * yearOffset + leapYears + 30L * (month - 1L) + (day - 1L);
		}
	},
	SOVIET_ETERNAL("soviet_eternal"){
		/**
		 * Soviet Revolutionary Calendar (1929–1940).
		 * <p>
		 * The solar dates and leap year rules were identical to the Gregorian calendar.
		 * The 5 (or 6) national "intercalary/holiday days" outside the standard 5-day cycle
		 * map directly onto the corresponding solar days of the Gregorian system.
		 */
		@Override
		public long parseToJdn(final String dateExpression, final int baseContextAnchor){
			// FIXME to test
			final GenealogicalDate date = UniversalDateConverter.parse(getCode(), dateExpression);
			final LocalDate localDate = date.isoDate();
			final int year = localDate.getYear();
			final int month = localDate.getMonthValue();
			final int day = localDate.getDayOfMonth();

			if(year < 1929 || year > 1940)
				throw new IllegalArgumentException("Soviet revolutionary calendar was only active between 1929 and 1940.");

			return GREGORIAN.parseToJdn(day + StringUtils.SPACE + month + StringUtils.SPACE + year,
				baseContextAnchor);
		}
	},
	MAYAN("mayan"){
		/**
		 * Converts a Mayan Long Count date (expressed as year = Baktun, month = Katun, day = Tun)
		 * into a Julian Day Number (JDN).
		 * <p>
		 * Note on Correlation Constants:
		 * <ul>
		 *   <li><b>584283</b>: Standard GMT (Goodman-Martínez-Thompson) correlation; epoch 11 Aug 3114 BCE (proleptic Julian).</li>
		 *   <li><b>584285</b>: Astronomical GMT correlation (2-day shift for lunar/eclipse alignment); epoch 13 Aug 3114 BCE.</li>
		 *   <li><b>489384</b>: Spinden correlation (historical alternative shift).</li>
		 * </ul>
		 */
		@Override
		public long parseToJdn(final String dateExpression, final int baseContextAnchor){
			// Expected format: "13.0.13.17.13" (Baktun.Katun.Tun.Uinal.Kin)
			final String[] tokens = StringUtils.split(dateExpression.replaceAll("\\s+.*", StringUtils.EMPTY),
				'.');
			if(tokens.length < 5)
				throw new IllegalArgumentException("Invalid Mayan Long Count layout structure.");

			final long baktun = Long.parseLong(tokens[0]);
			final long katun = Long.parseLong(tokens[1]);
			final long tun = Long.parseLong(tokens[2]);
			final long uinal = Long.parseLong(tokens[3]);
			final long kin = Long.parseLong(tokens[4]);

			// Standard GMT correlation constant
			final long gmtCorrelation = 584283L;

			// Long Count calculation in days (Kin): 1 Baktun = 144,000 days, 1 Katun = 7,200 days, 1 Tun = 360 days
			final long totalKin = baktun * 144000L + katun * 7200L + tun * 360L + uinal * 20L + kin;

			return gmtCorrelation + totalKin;
		}
	},

	PERSIAN("persian"){
		@Override
		public long parseToJdn(final String dateExpression, final int baseContextAnchor){
			// TODO
//			final long epYear = year - (year >= 0? 474: 473);
//			final long cycle = epYear / 2820;
//			final long cYear = epYear % 2820;
//			final long aux = (cYear < 1029? cYear: cYear - 2820);
//			final long yCycle = (aux >= 0? aux: aux + 2820);
//			final long yIndex = yCycle + 474;
//
//			final long daysInMonths = (month <= 7? (month - 1) * 31L: (month - 1) * 30L + 6);
//			return day + daysInMonths + (yIndex * 682 - 110) / 2816 + (yIndex - 1) * 365 + cycle * 1029983 + 1948320;
			return -1l;
		}
	},
	PARSI("parsi"){
		@Override
		public long parseToJdn(final String dateExpression, final int baseContextAnchor){
			// TODO
//			return 1952063L + 365L * (year - 1L) + 30L * (month - 1L) + (day - 1L);
			return -1l;
		}
	},
	BYZANTINE("byzantine"){
		@Override
		public long parseToJdn(final String dateExpression, final int baseContextAnchor){
			// TODO
			// Adjust year: Byzantine year starts Sep 1; years before Sep belong to (year - 5509), Sep onwards to (year - 5508)
//			final int julianYear = (month >= 9? year - 5508: year - 5509);
//			return JULIAN.toJdn(julianYear, month, day);
			return -1l;
		}
	},
	EGYPTIAN("egyptian"){
		@Override
		public long parseToJdn(final String dateExpression, final int baseContextAnchor){
			// TODO
//			return 1448638L + 365L * (year - 1L) + 30L * (month - 1L) + (day - 1L);
			return -1l;
		}
	},
	SELEUCID("seleucid"){
		@Override
		public long parseToJdn(final String dateExpression, final int baseContextAnchor){
			// TODO
//			final int julianYear = year - 311;
//			return JULIAN.toJdn(julianYear, month, day);
			return -1l;
		}
	},
	ARMENIAN("armenian"){
		@Override
		public long parseToJdn(final String dateExpression, final int baseContextAnchor){
			// TODO
//			return 1922868L + 365L * (year - 1L) + 30L * (month - 1L) + (day - 1L);
			return -1l;
		}
	},
	RUMI("rumi"){
		@Override
		public long parseToJdn(final String dateExpression, final int baseContextAnchor){
			// TODO
//			final int julianYear = year + 584;
//			return JULIAN.toJdn(julianYear, month, day);
			return -1l;
		}
	};


	private final String code;


	CalendarType(final String code){
		this.code = code;
	}


	public String getCode(){
		return code;
	}

	public static CalendarType fromCode(final String code){
		for(final CalendarType type : values())
			if(type.code.equalsIgnoreCase(code))
				return type;

//		throw new IllegalArgumentException("Unsupported calendar: " + code);
//
//		LOGGER.warn("Unsupported calendar: {}, default to 'gregorian'", code);

		return GREGORIAN;
	}

	/**
	 * Converts a calendar-specific date to its Julian Day Number.
	 *
	 * @param dateExpression  the date in the given calendar
	 * @return the Julian Day Number
	 * @throws UnsupportedOperationException if the calendar is not arithmetic
	 */
	public long parseToJdn(final String dateExpression, final int baseContextAnchor){
		throw new UnsupportedOperationException("Calendar not supported by the arithmetic converter: " + code
			+ ". Extend CalendarType to add it.");
	}

}
