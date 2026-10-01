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
package io.github.mtrevisan.familylegacy.v2.io.model.readers.date;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.ChineseCalendar;
import com.ibm.icu.util.HebrewCalendar;
import com.ibm.icu.util.IndianCalendar;
import com.ibm.icu.util.TimeZone;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.threeten.extra.chrono.CopticChronology;
import org.threeten.extra.chrono.EthiopicChronology;
import org.threeten.extra.chrono.JulianChronology;

import java.text.ParsePosition;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.HijrahChronology;
import java.time.chrono.IsoChronology;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;


/**
 * Generic service to parse partial or full date strings into GenealogicalDate objects.
 * Supports standard date pattern formats as well as GEDCOM-style date strings.
 *
 * <p>Every calendar declared in {@link CalendarType} is converted to a
 * proleptic Gregorian date. Three conversion strategies are used, chosen
 * per calendar:</p>
 *
 * <ol>
 *   <li><b>JSR-310 chronology</b> — for calendars with a built-in
 *       {@link Chronology} implementation (Gregorian, Julian, Islamic,
 *       Buddhist, Coptic, Ethiopic). The chronology handles leap-year
 *       rules natively.</li>
 *   <li><b>ICU4J calendar</b> — for calendars whose leap rules are not
 *       purely arithmetic (Hebrew, Chinese, Indian National). ICU4J ships
 *       the full tables and the astronomical computation.</li>
 *   <li><b>Arithmetic JDN</b> — for the remaining calendars, whose only
 *       conversion rule is the one declared in
 *       {@link CalendarType#toJdn(int, int, int)}. The enum produces a
 *       Julian Day Number and this class converts it back to a
 *       proleptic Gregorian date.</li>
 * </ol>
 */
public final class UniversalDateConverter{

	private static final String[] GEDCOM_PREFIXES = {
		"ABT", "CAL", "EST", "BEFORE", "BEF", "AFTER", "AFT", "FROM", "TO", "BET", "AND", "INT"
	};

	/**
	 * Julian Day Number of 1970-01-01 in the proleptic Gregorian calendar,
	 * i.e. the JDN that corresponds to {@link LocalDate#ofEpochDay(long)}
	 * day {@code 0}. Used to convert a JDN into a {@link LocalDate}.
	 */
	private static final long UNIX_EPOCH_JDN = 2440588L;


	private record PatternMatch(DateTimeFormatter formatter, GenealogicalDate.DatePrecision precision, boolean hasDay,
		boolean hasMonth, boolean hasYear){}


	private static final PatternMatch[] PATTERNS = new PatternMatch[]{
		createPattern("d MMMM uuuu", GenealogicalDate.DatePrecision.EXACT, true, true, true),
		createPattern("d MMM uuuu", GenealogicalDate.DatePrecision.EXACT, true, true, true),
		createPattern("d M uuuu", GenealogicalDate.DatePrecision.EXACT, true, true, true),
		createPattern("MMMM uuuu", GenealogicalDate.DatePrecision.YEAR_MONTH, false, true, true),
		createPattern("MMM uuuu", GenealogicalDate.DatePrecision.YEAR_MONTH, false, true, true),
		createPattern("M uuuu", GenealogicalDate.DatePrecision.YEAR_MONTH, false, true, true),
		createPattern("uuuu", GenealogicalDate.DatePrecision.YEAR_ONLY, false, false, true),
		createPattern("d MMMM uuu", GenealogicalDate.DatePrecision.EXACT, true, true, true),
		createPattern("d MMM uuu", GenealogicalDate.DatePrecision.EXACT, true, true, true),
		createPattern("d M uuu", GenealogicalDate.DatePrecision.EXACT, true, true, true),
		createPattern("MMMM uuu", GenealogicalDate.DatePrecision.YEAR_MONTH, false, true, true),
		createPattern("MMM uuu", GenealogicalDate.DatePrecision.YEAR_MONTH, false, true, true),
		createPattern("M uuu", GenealogicalDate.DatePrecision.YEAR_MONTH, false, true, true),
		createPattern("uuu", GenealogicalDate.DatePrecision.YEAR_ONLY, false, false, true),
		createPattern("d MMMM", GenealogicalDate.DatePrecision.MONTH_DAY, true, true, false),
		createPattern("d MMM", GenealogicalDate.DatePrecision.MONTH_DAY, true, true, false),
		createPattern("d M", GenealogicalDate.DatePrecision.MONTH_DAY, true, true, false)
	};

	private static PatternMatch createPattern(final String pattern,
			final GenealogicalDate.DatePrecision precision, final boolean hasDay, final boolean hasMonth,
			final boolean hasYear){
		final DateTimeFormatter dtf = new DateTimeFormatterBuilder()
			.parseCaseInsensitive()
			.appendPattern(pattern)
			.parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
			.parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
			.parseDefaulting(ChronoField.YEAR, Year.now().getValue())
			.toFormatter(Locale.ENGLISH);
		return new PatternMatch(dtf, precision, hasDay, hasMonth, hasYear);
	}


	/* ======================================================================
	 *                          Entry point
	 * ====================================================================== */

	public static GenealogicalDate parse(final String calendarCode, String rawDate){
		if(rawDate == null || rawDate.isEmpty())
			throw new IllegalArgumentException("Date string is required");

		// Strip embedded calendar escape tags (e.g. @#DGREGORIAN@)
		if(rawDate.startsWith("@#") && rawDate.contains("@")){
			rawDate = StringUtils.substringAfter(rawDate, "@")
				.trim();
			if(rawDate.startsWith("#"))
				rawDate = StringUtils.substringAfter(rawDate, "@")
					.trim();
		}

		boolean isApproximate = false;
		for(final String prefix : GEDCOM_PREFIXES)
			if(Strings.CI.startsWith(rawDate, prefix)){
				isApproximate = true;
				rawDate = StringUtils.stripStart(rawDate.substring(prefix.length()), null);

				break;
			}

		// Replace standard GEDCOM / date delimiters ('/', '.', '-') with spaces
		rawDate = StringUtils.replaceChars(rawDate, "/.-", "   ");
		final String cleanedDate = StringUtils.normalizeSpace(rawDate);
		final ParsePosition pos = new ParsePosition(0);
		final CalendarType type = CalendarType.fromCode(calendarCode);
		for(final PatternMatch pm : PATTERNS){
			try{
				pos.setIndex(0);
				pos.setErrorIndex(-1);
				final TemporalAccessor accessor = pm.formatter()
					.parse(cleanedDate, pos);
				// Accept match only if parsing succeeded AND consumed the entire string
				if(pos.getErrorIndex() != -1 || pos.getIndex() != cleanedDate.length())
					continue;

				final LocalDate resultIso = switch(type){
					/* ----- JSR-310 chronologies (arithmetic leap rules) ----- */
					case GREGORIAN -> parseJsr310(IsoChronology.INSTANCE, accessor, pm);
					case JULIAN -> parseJsr310(JulianChronology.INSTANCE, accessor, pm);
					case ISLAMIC -> parseJsr310(HijrahChronology.INSTANCE, accessor, pm);
					case BUDDHIST -> parseJsr310(ThaiBuddhistChronology.INSTANCE, accessor, pm);
					case COPTIC -> parseJsr310(CopticChronology.INSTANCE, accessor, pm);
					case ETHIOPIAN -> parseJsr310(EthiopicChronology.INSTANCE, accessor, pm);

					/* ----- ICU4J calendars (non-arithmetic rules) ----------- */
					case HEBREW -> parseIcu4j(new HebrewCalendar(), accessor, pm);
					case CHINESE -> parseIcu4j(new ChineseCalendar(), accessor, pm);
					case INDIAN -> parseIcu4j(new IndianCalendar(), accessor, pm);

					/* ----- Calendars with a bespoke conversion --------------- */
					case FRENCH_REPUBLICAN -> parseFrenchRepublican(accessor, pm);
					case SOVIET_ETERNAL -> parseSovietEternal(accessor, pm);
					case MAYAN -> parseMayanLongCount(cleanedDate);

					/* ----- Calendars defined only by an arithmetic JDN ------
					 * These calendars have no JSR-310 or ICU4J analogue, so
					 * the conversion is delegated to
					 * {@link CalendarType#toJdn(int, int, int)} and the
					 * resulting JDN is turned into a proleptic Gregorian
					 * date. This covers the calendars that previously
					 * returned {@code null} from the switch. */
					case REFORMED_JULIAN, PERSIAN, PARSI, BYZANTINE,
						  EGYPTIAN, SELEUCID, ARMENIAN, RUMI
						-> parseViaJdn(type, accessor, pm);
				};

				return new GenealogicalDate(resultIso, pm.precision(), isApproximate, rawDate, type);
			}
			catch(final Exception ignored){}
		}

		throw new IllegalArgumentException("Unable to parse date '" + rawDate + "' for calendar " + calendarCode);
	}


	/* ======================================================================
	 *                          JSR-310 chronologies
	 * ====================================================================== */

	/**
	 * Converts a date expressed in a JSR-310 chronology into a proleptic
	 * Gregorian {@link LocalDate}. The chronology handles the leap-year
	 * rules.
	 */
	private static LocalDate parseJsr310(final Chronology chrono, final TemporalAccessor accessor,
			final PatternMatch pm){
		final int year = (pm.hasYear()? accessor.get(ChronoField.YEAR): Year.now().getValue());
		final int month = (pm.hasMonth()? accessor.get(ChronoField.MONTH_OF_YEAR): 1);
		final int day = (pm.hasDay()? accessor.get(ChronoField.DAY_OF_MONTH): 1);

		final ChronoLocalDate cld = chrono.date(year, month, day);
		return LocalDate.from(cld);
	}


	/* ======================================================================
	 *                          ICU4J calendars
	 * ====================================================================== */

	/**
	 * Converts a date expressed in an ICU4J calendar into a proleptic
	 * Gregorian {@link LocalDate}. ICU4J uses months 0-11 internally, so
	 * the parsed 1-based month is decremented before use.
	 */
	private static LocalDate parseIcu4j(final Calendar cal, final TemporalAccessor accessor, final PatternMatch pm){
		final int day = (pm.hasDay()? accessor.get(ChronoField.DAY_OF_MONTH): 1);
		final int month = (pm.hasMonth()? accessor.get(ChronoField.MONTH_OF_YEAR) - 1: 0);
		final int year = (pm.hasYear()? accessor.get(ChronoField.YEAR): Year.now().getValue());

		cal.clear();
		cal.setTimeZone(TimeZone.getTimeZone("UTC"));
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DAY_OF_MONTH, day);

		return Instant.ofEpochMilli(cal.getTimeInMillis())
			.atZone(ZoneOffset.UTC)
			.toLocalDate();
	}


	/* ======================================================================
	 *                          Arithmetic JDN calendars
	 * ====================================================================== */

	/**
	 * Converts a date expressed in any calendar whose conversion rule is
	 * declared arithmetically in {@link CalendarType#toJdn(int, int, int)}.
	 *
	 * <p>The year, month and day are extracted from the accessor and passed
	 * to the enum, which returns a Julian Day Number. The JDN is then
	 * converted to a proleptic Gregorian date by subtracting the JDN of the
	 * Unix epoch (1970-01-01) and using the result as the epoch day count of
	 * {@link LocalDate#ofEpochDay(long)}.</p>
	 *
	 * <p>Calendars handled here: Revised Julian, Persian (Solar Hijri),
	 * Parsi (Zoroastrian), Byzantine, Egyptian, Seleucid, Armenian and
	 * Rumi. The first three have their own leap-year cycles; the last five
	 * are anchored to the Julian calendar but with a different epoch or
	 * year-counting rule.</p>
	 */
	private static LocalDate parseViaJdn(final CalendarType type, final TemporalAccessor accessor,
			final PatternMatch pm){
		final int year = (pm.hasYear()? accessor.get(ChronoField.YEAR): Year.now().getValue());
		final int month = (pm.hasMonth()? accessor.get(ChronoField.MONTH_OF_YEAR): 1);
		final int day = (pm.hasDay()? accessor.get(ChronoField.DAY_OF_MONTH): 1);

		final long jdn = type.toJdn(year, month, day);
		return LocalDate.ofEpochDay(jdn - UNIX_EPOCH_JDN);
	}


	/* ======================================================================
	 *                          Bespoke conversions
	 * ====================================================================== */

	/**
	 * French Republican calendar, arithmetic Romme variant: 12 months of
	 * 30 days + 5 or 6 complementary days, with leap years every 4 years
	 * starting from year 3. The epoch is 22 September 1792 (JDN 2375839).
	 */
	private static LocalDate parseFrenchRepublican(final TemporalAccessor accessor, final PatternMatch pm){
		final int day = (pm.hasDay()? accessor.get(ChronoField.DAY_OF_MONTH): 1);
		final int month = (pm.hasMonth()? accessor.get(ChronoField.MONTH_OF_YEAR): 1);
		final int year = (pm.hasYear()? accessor.get(ChronoField.YEAR): 1);

		final LocalDate epoch = LocalDate.of(1792, 9, 22);
		final long daysToAdd = (year - 1) * 365L + (year / 4) + (month - 1) * 30L + (day - 1);
		return epoch.plusDays(daysToAdd);
	}

	/**
	 * Soviet Revolutionary Calendar (1929-1940): 12 months of 30 days each
	 * plus 5 or 6 holiday days without a month. The solar dates follow the
	 * Gregorian calendar, so the conversion is a pass-through.
	 */
	private static LocalDate parseSovietEternal(final TemporalAccessor accessor, final PatternMatch pm){
		return parseJsr310(IsoChronology.INSTANCE, accessor, pm);
	}

	/**
	 * Mayan Long Count: Baktun.Katun.Tun.Uinal.Kin (e.g. 13.0.0.0.0).
	 * Uses the standard GMT correlation (JDN 584283 for the epoch
	 * 11 August 3114 BC proleptic Gregorian).
	 */
	private static LocalDate parseMayanLongCount(final String input){
		final String[] parts = StringUtils.split(input, ' ');
		if(parts.length < 5)
			throw new IllegalArgumentException("Invalid Mayan date. Requested format: 'Baktun Katun Tun Uinal Kin'");

		final long baktun = Long.parseLong(parts[0]);
		final long katun = Long.parseLong(parts[1]);
		final long tun = Long.parseLong(parts[2]);
		final long uinal = Long.parseLong(parts[3]);
		final long kin = Long.parseLong(parts[4]);

		final long totalDays = baktun * 144000 + katun * 7200 + tun * 360 + uinal * 20 + kin;

		// Mayan Era GMT Correlation (11 August 3114 BC proleptic Gregorian = JDN 584283)
		final LocalDate mayanEpoch = LocalDate.of(-3113, 8, 11);
		return mayanEpoch.plusDays(totalDays);
	}

}
