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


import io.github.mtrevisan.familylegacy.io.model.readers.date.armenian.ArmenianCalendarParser;
import io.github.mtrevisan.familylegacy.io.model.readers.date.armenian.ArmenianDateInput;
import io.github.mtrevisan.familylegacy.io.model.readers.date.byzantine.ByzantineCalendarParser;
import io.github.mtrevisan.familylegacy.io.model.readers.date.byzantine.ByzantineDateInput;
import io.github.mtrevisan.familylegacy.io.model.readers.date.chinese.ChineseCalendarAstronomicalEngine;
import io.github.mtrevisan.familylegacy.io.model.readers.date.chinese.ChineseCalendarParser;
import io.github.mtrevisan.familylegacy.io.model.readers.date.chinese.ChineseDateInput;
import io.github.mtrevisan.familylegacy.io.model.readers.date.egyptian.EgyptianCalendarParser;
import io.github.mtrevisan.familylegacy.io.model.readers.date.egyptian.EgyptianDateInput;
import io.github.mtrevisan.familylegacy.io.model.readers.date.islamic.IslamicCalendarAstronomicalEngine;
import io.github.mtrevisan.familylegacy.io.model.readers.date.parsi.ParsiCalendarParser;
import io.github.mtrevisan.familylegacy.io.model.readers.date.parsi.ParsiDateInput;
import io.github.mtrevisan.familylegacy.io.model.readers.date.persian.PersianCalendarParser;
import io.github.mtrevisan.familylegacy.io.model.readers.date.persian.PersianDateInput;
import io.github.mtrevisan.familylegacy.io.model.readers.date.rumi.RumiCalendarParser;
import io.github.mtrevisan.familylegacy.io.model.readers.date.rumi.RumiDateInput;
import io.github.mtrevisan.familylegacy.io.model.readers.date.seleucid.SeleucidCalendarParser;
import io.github.mtrevisan.familylegacy.io.model.readers.date.seleucid.SeleucidDateInput;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;


/**
 * Supported calendar systems and their conversion algorithms to Julian Day Number (JDN).
 */
public enum CalendarType implements CalendarConverterPlugin{

	GREGORIAN("gregorian"){
		@Override
		public long parseToJdn(final String dateExpression){
			final GenealogicalDate date = UniversalDateConverter.parse(getCode(), dateExpression);
			final LocalDate localDate = date.isoDate();
			final int year = localDate.getYear();
			final int month = localDate.getMonthValue();
			final int day = localDate.getDayOfMonth();

			return gregorianDateToJdn(year, month, day);
		}
	},
	JULIAN("julian"){
		@Override
		public long parseToJdn(final String dateExpression){
			final GenealogicalDate date = UniversalDateConverter.parse(getCode(), dateExpression);
			final LocalDate localDate = date.isoDate();
			final int year = localDate.getYear();
			final int month = localDate.getMonthValue();
			final int day = localDate.getDayOfMonth();

			final long a = (14 - month) / 12;
			final long y = year + 4800L - a;
			final long m = month + 12L * a - 3;
			return day
				+ (153 * m + 2) / 5
				+ 365 * y + y / 4
				- 32083;
		}
	},
	REFORMED_JULIAN("reformed_julian"){
		@Override
		public long parseToJdn(final String dateExpression){
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
			return day
				+ (153 * m + 2) / 5
				+ 365 * y + y / 4
				- century + leapCenturies
				- 32022;
		}
	},
	ISLAMIC("islamic"){
		@Override
		public long parseToJdn(final String dateExpression){
			final GenealogicalDate date = UniversalDateConverter.parse(getCode(), dateExpression);
			final LocalDate localDate = date.isoDate();
			final int year = localDate.getYear();
			final int month = localDate.getMonthValue();
			final int day = localDate.getDayOfMonth();

			return IslamicCalendarAstronomicalEngine.toJdn(year, month, day);
		}
	},
	BUDDHIST("buddhist"){
		@Override
		public long parseToJdn(final String dateExpression){
			final GenealogicalDate date = UniversalDateConverter.parse(getCode(), dateExpression);
			final LocalDate localDate = date.isoDate();
			final int year = localDate.getYear();
			final int month = localDate.getMonthValue();
			final int day = localDate.getDayOfMonth();

			// Solar Buddhist calendar uses Gregorian arithmetic with a +543 year offset (BE = CE + 543)
			return gregorianDateToJdn(year - 543, month, day);
		}
	},
	COPTIC("coptic"){
		@Override
		public long parseToJdn(final String dateExpression){
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
		public long parseToJdn(final String dateExpression){
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
		public long parseToJdn(final String dateExpression){
			// Note: Traditional Hebrew days start at sunset. This structural JDN anchors to the civil day portion.
			final GenealogicalDate date = UniversalDateConverter.parse(getCode(), dateExpression);
			final LocalDate localDate = date.isoDate();
			final int year = localDate.getYear();
			final int month = localDate.getMonthValue();
			final int day = localDate.getDayOfMonth();

			return hebrewNewYear(year) + daysBeforeMonth(year, month) + (day - 1);
		}

		private boolean isLeapYear(final int year){
			return ((7 * year + 1) % 19 < 7);
		}

		/**
		 * Computes the Julian Day Number for Rosh HaShanah (1 Tishrei) of a given Hebrew year.
		 * Strictly applies the historical Dehiyyot postponement rules in the correct mathematical sequence.
		 */
		private long hebrewNewYear(final int year){
			// 1. Calculate the elapsed months and parts since the epoch
			final long monthsElapsed = (235L * year - 234L) / 19L;
			final long partsElapsed = 204L + 793L * (monthsElapsed % 1080L);
			final long hoursElapsed = 11L + 12L * monthsElapsed + 793L * (monthsElapsed / 1080L) + partsElapsed / 1080L;

			// Dynamic day calculation from the raw astronomical Molad calculation
			long day = 1L + 29L * monthsElapsed + hoursElapsed / 24L;
			final long parts = (hoursElapsed % 24L) * 1080L + (partsElapsed % 1080L);

			// Convert to absolute JDN coordinates BEFORE evaluating the day of the week to fix the remainder bug
			long roshHaShanahJdn = day + 347997L;

			// Rule 1: Dehiyyat Molad Zaken (Postpone if Molad occurs at or after noon / 18 hours = 19440 parts)
			if(parts >= 19440L)
				roshHaShanahJdn ++;

			// 2. Identify the absolute day of the week (JDN % 7: 0 = Monday, 1 = Tuesday, ..., 5 = Saturday, 6 = Sunday)
			long dayOfWeek = roshHaShanahJdn % 7L;

			// Rule 3: Dehiyyat GATRAD
			// Postpone if it's a common year, Molad falls on Tuesday (dayOfWeek == 1), and parts >= 9h 204p (9924 parts)
			if(dayOfWeek == 1L && !isLeapYear(year) && parts >= 9924L)
				roshHaShanahJdn += 2L;
			// Rule 4: Dehiyyat BETUAGOT
			// Postpone if it follows a leap year, Molad falls on Monday (dayOfWeek == 0), and parts >= 15h 589p (16789 parts)
			else if(dayOfWeek == 0L && isLeapYear(year - 1) && parts >= 16789L)
				roshHaShanahJdn ++;

			// Re-evaluate day of the week for the final absolute check against prohibited days
			dayOfWeek = roshHaShanahJdn % 7L;

			// Rule 2: Dehiyyat ADU
			// Rosh HaShanah cannot fall on Sunday, Wednesday, or Friday.
			// Mapping JDN % 7: Sunday = 6, Wednesday = 2, Friday = 4
			if(dayOfWeek == 6L || dayOfWeek == 2L || dayOfWeek == 4L)
				roshHaShanahJdn ++;

			return roshHaShanahJdn;
		}

		/**
		 * Computes the cumulative elapsed days in a Hebrew year before the given month.
		 * Dynamically adjusts for deficient, regular, and complete year lengths.
		 */
		private int daysBeforeMonth(final int year, final int month){
			// Resolve the absolute length of the current Hebrew year
			final long thisNY = hebrewNewYear(year);
			final long nextNY = hebrewNewYear(year + 1);
			final int yearLength = (int)(nextNY - thisNY);

			final boolean isLeap = isLeapYear(year);

			// Detect custom year configurations (Heshvan and Kislev dynamic lengths)
			// Deficient (353/383 days), Regular (354/384 days), Complete (355/385 days)
			final boolean longCheshvan = (yearLength == 355 || yearLength == 385);
			final boolean shortKislev = (yearLength == 353 || yearLength == 383);

			int days = 0;
			for(int m = 1; m < month; m ++){
				days += switch(m){
					// Tishrei
					case 1 -> 30;
					// Cheshvan
					case 2 -> (longCheshvan? 30: 29);
					// Kislev
					case 3 -> (shortKislev? 29: 30);
					// Tevet
					case 4 -> 29;
					// Shevat
					case 5 -> 30;
					// In a leap year, Month 6 is Adar I (30 days). In a common year, it is Adar (29 days).
					case 6 -> (isLeap? 30: 29);
					// In a leap year, Month 7 is Adar II (29 days). In a common year, it is Nisan (30 days).
					case 7 -> (isLeap? 29: 30);
					// In a leap year, Month 8 is Nisan (30 days). In a common year, it is Iyar (29 days).
					case 8 -> (isLeap? 30: 29);
					// In a leap year, Month 9 is Iyar (29 days). In a common year, it is Sivan (30 days).
					case 9 -> (isLeap? 29: 30);
					// In a leap year, Month 10 is Sivan (30 days). In a common year, it is Tammuz (29 days).
					case 10 -> (isLeap? 30: 29);
					// In a leap year, Month 11 is Tammuz (29 days). In a common year, it is Av (30 days).
					case 11 -> (isLeap? 29: 30);
					// In a leap year, Month 12 is Av (30 days). In a common year, it is Elul (29 days).
					case 12 -> (isLeap? 30: 29);
					// Elul in a leap year (Month 13)
					case 13 -> 29;
					default -> 0;
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
		public long parseToJdn(final String dateExpression){
			// Expected native format: "光绪二十五年 庚子年 正月 初一" or "2020年 庚子年 正月 初一"
			final ChineseDateInput date = ChineseCalendarParser.parse(dateExpression);
			final int year = date.getYear();
			final int month = date.getMonth();
			final boolean leapMonth = date.isLeapMonth();
			final int day = date.getDay();

			return ChineseCalendarAstronomicalEngine.toJdn(year, month, leapMonth, day);
		}
	},
	INDIAN("indian"){
		/**
		 * Indian National Calendar (Saka Samvat).
		 * Epoch: Saka 0 = 78 CE (Gregorian).
		 * Year starts on 1 Chaitra (March 21 in Gregorian leap years, March 22 in common years).
		 */
		@Override
		public long parseToJdn(final String dateExpression){
			// 1. Parse the string into components.
			// Assuming UniversalDateConverter.parse maps fields to the structural Indian components.
			final GenealogicalDate date = UniversalDateConverter.parse(getCode(), dateExpression);
			final LocalDate localDate = date.isoDate();
			final int sakaYear = localDate.getYear();
			final int sakaMonth = localDate.getMonthValue();
			final int sakaDay = localDate.getDayOfMonth();

			// The year Chaitra starts in the Gregorian year: Saka + 78
			final int gregorianYear = sakaYear + 78;

			// The Indian National Calendar is leap if the corresponding Gregorian year is leap
			final boolean isLeap = isGregorianLeapYear(gregorianYear);

			// 2. Compute the exact JDN of 1 Chaitra for this specific year
			final int chaitraStartDay = (isLeap? 21: 22);

			// Anchor the JDN to the target Gregorian March entry point
			final long startOfYearJdn = GREGORIAN.parseToJdn(
				chaitraStartDay + StringUtils.SPACE + 3 + StringUtils.SPACE + gregorianYear);

			// 3. Accumulate elapsed days in preceding Indian months
			int elapsedDays = 0;
			for(int m = 1; m < sakaMonth; m ++){
				elapsedDays += switch(m){
					// Chaitra has 31 days in a leap year, 30 days in a common year
					case 1 -> (isLeap? 31: 30);
					// Vaisakha, Jyaistha, Asadha, Shravana, Bhadra always have 31 days
					case 2, 3, 4, 5, 6 -> 31;
					// Asvina, Kartika, Agrahayana, Pausa, Magha, Phalguna always have 30 days
					default -> 30;
				};
			}

			// 4. Return total JDN adding elapsed months and days offset
			return startOfYearJdn + elapsedDays + (sakaDay - 1);
		}

		private boolean isGregorianLeapYear(final int year){
			return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
		}
	},

	FRENCH_REPUBLICAN("french_republican"){
		@Override
		public long parseToJdn(final String dateExpression){
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
		public long parseToJdn(final String dateExpression){
			// FIXME to test
			final GenealogicalDate date = UniversalDateConverter.parse(getCode(), dateExpression);
			final LocalDate localDate = date.isoDate();
			final int year = localDate.getYear();
			final int month = localDate.getMonthValue();
			final int day = localDate.getDayOfMonth();

			if(year < 1929 || year > 1940)
				throw new IllegalArgumentException("Soviet revolutionary calendar was only active between 1929 and 1940.");

			return GREGORIAN.parseToJdn(day + StringUtils.SPACE + month + StringUtils.SPACE + year);
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
		public long parseToJdn(final String dateExpression){
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
		public long parseToJdn(final String dateExpression){
			// Expected native format: "۱ فروردین ۱۳۰۰" or "۱۰ رمضان ۱۲۶۸ هـ.ق"
			final PersianDateInput date = PersianCalendarParser.parse(dateExpression);
			final int year = date.getYear();
			final int month = date.getMonth();
			final int day = date.getDay();

			// Persian epoch base year
			final long epBase = year - (year >= 0? 474: 473);

			// Year inside the 2820-year cycle
			final long epYear = 474 + Math.floorMod(epBase, 2820);

			// Days elapsed in previous months
			final long monthDays = (month <= 7
				? (month - 1) * 31L
				: (month - 1) * 30L + 6L);

			// Julian Day Number
			return day + monthDays + (epYear * 682 - 110) / 2816 + (epYear - 1) * 365
				+ Math.floorDiv(epBase, 2820) * 1_029_983L + 1_948_319L;
		}
	},
	PARSI("parsi"){
		@Override
		public long parseToJdn(final String dateExpression){
			// Parse the raw string expression into a structured input object using the specialized parser
			final ParsiDateInput input = ParsiCalendarParser.parse(dateExpression);
			final int year = input.year();
			final int month = input.month();
			final int day = input.day();

			// Compute and return the final Julian Day Number using the non-intercalating linear system
			return 1952063L + 365L * (year - 1L) + 30L * (month - 1L) + (day - 1L);
		}
	},
	BYZANTINE("byzantine"){
		@Override
		public long parseToJdn(final String dateExpression){
			final ByzantineDateInput input = ByzantineCalendarParser.parse(dateExpression);
			final int year = input.year();
			final int month = input.month();
			final int day = input.day();

			final int julianYear;
			final int julianMonth;
			if(month <= 4){
				julianYear = year - 5509;
				// Sep=9, Oct=10, Nov=11, Dec=12
				julianMonth = month + 8;
			}
			else{
				julianYear = year - 5508;
				// Jan=1, Feb=2, ..., Aug=8
				julianMonth = month - 4;
			}

			// Adjust year boundary for the historical astronomical algorithm anchor
			final int y = (julianMonth <= 2? julianYear - 1: julianYear);
			final int m = (julianMonth <= 2? julianMonth + 12: julianMonth);

			// Standard Julian Calendar JDN conversion formula
			return Math.floorDiv(1461L * (y + 4716L), 4L)
				+ Math.floorDiv(153L * (m + 1), 5L)
				+ day - 1524L;
		}
	},
	EGYPTIAN("egyptian"){
		@Override
		public long parseToJdn(final String dateExpression){
			final EgyptianDateInput input = EgyptianCalendarParser.parse(dateExpression);
			final int year = input.year();
			final int month = input.month();
			final int day = input.day();

			// 1703643: JDN of the Era of Nabonassar epoch (26 February 747 BC Julian)
			return 1703643L + (year - 1L) * 365L + (month - 1L) * 30L + (day - 1L);
		}
	},
	SELEUCID("seleucid"){
		@Override
		public long parseToJdn(final String dateExpression){
			final SeleucidDateInput input = SeleucidCalendarParser.parse(dateExpression);
			final int year = input.year();
			final int month = input.month();
			final int day = input.day();

			// Convert Seleucid year and month to standard Julian calendar coordinates
			// October (1) to December (3) -> Julian year = Seleucid year - 312
			// January (4) to September (12) -> Julian year = Seleucid year - 311
			final int julianYear;
			final int julianMonth;

			if(month <= 3){
				julianYear = year - 312;
				// Oct=10, Nov=11, Dec=12
				julianMonth = month + 9;
			}
			else{
				julianYear = year - 311;
				// Jan=1, Feb=2, ..., Sep=9
				julianMonth = month - 3;
			}

			// Standard Julian Calendar JDN conversion logic (Meeus framework)
			final int y = (julianMonth <= 2? julianYear - 1: julianYear);
			final int m = (julianMonth <= 2? julianMonth + 12: julianMonth);

			return Math.floorDiv(1461L * (y + 4716L), 4L)
				+ Math.floorDiv(153L * (m + 1), 5L)
				+ day - 1524L;
		}
	},
	ARMENIAN("armenian"){
		@Override
		public long parseToJdn(final String dateExpression){
			final ArmenianDateInput input = ArmenianCalendarParser.parse(dateExpression);
			final int year = input.year();
			final int month = input.month();
			final int day = input.day();

			// 1922868: JDN of the Great Armenian Era epoch (11 July 552 CE Julian)
			return 1922868L + 365L * (year - 1L) + 30L * (month - 1L) + (day - 1L);
		}
	},
	RUMI("rumi"){
		@Override
		public long parseToJdn(final String dateExpression){
			// Isolate components via the adaptive Rumi parser
			final RumiDateInput input = RumiCalendarParser.parse(dateExpression);
			final int year = input.year();
			final int month = input.month();
			final int day = input.day();

			// Determine if the target date is after the Gregorian alignment reform (March 1, 1333 Rumi)
			// Before reform: Year started in March (Month 1 = March, 12 = February)
			// After reform: Year aligned to January (Month 1 = January, 12 = December)
			if(year < 1332 || year == 1332 && month <= 12){
				// PRE-REFORM LOGIC: Pure Julian Calendar tracking with March start (Epoch 584 year difference)
				// Mapping Rumi month back to Julian month placement
				// March=1->3, Dec=10->12
				int julianMonth = month + 2;
				int julianYear = year + 584;
				// January and February belong to the next Julian year
				if(month > 10){
					// Jan=11->1, Feb=12->2
					julianMonth = month - 10;
					julianYear ++;
				}

				final int y = (julianMonth <= 2? julianYear - 1: julianYear);
				final int m = (julianMonth <= 2? julianMonth + 12: julianMonth);

				return Math.floorDiv(1461L * (y + 4716L), 4L)
					+ Math.floorDiv(153L * (m + 1), 5L)
					+ day - 1524L;
			}
			else{
				// POST-REFORM LOGIC: Completely mapped onto Gregorian rules. Year difference is strictly 584 years.
				final int gregorianYear = year + 584;

				final int y = (month <= 2? gregorianYear - 1: gregorianYear);
				final int m = (month <= 2? month + 12: month);

				return Math.floorDiv(48525L * y, 400L)
					- Math.floorDiv(y, 100L)
					+ Math.floorDiv(153L * (m + 1), 5L)
					+ day + 1721423L;
			}
		}
	};

	private static long gregorianDateToJdn(final int year, final int month, final int day){
		final long a = (14 - month) / 12;
		final long y = year + 4800L - a;
		final long m = month + 12L * a - 3;
		return day
			+ (153 * m + 2) / 5
			+ 365 * y + y / 4 - y / 100 + y / 400
			- 32045;
	}


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
	public long parseToJdn(final String dateExpression){
		throw new UnsupportedOperationException("Calendar not supported by the arithmetic converter: " + code
			+ ". Extend CalendarType to add it.");
	}

}
