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
package io.github.mtrevisan.familylegacy.v2.ui.helpers;

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
import java.util.Objects;


/**
 * Generic service to parse partial or full date strings into GenealogicalDate objects.
 * Supports standard date pattern formats as well as GEDCOM-style date strings.
 */
public final class UniversalDateConverter{

	private static final String[] GEDCOM_PREFIXES = {
		"ABT", "CAL", "EST", "BEFORE", "BEF", "AFTER", "AFT", "FROM", "TO", "BET", "AND", "INT"
	};


	private record PatternMatch(DateTimeFormatter formatter, ParsedGenealogicalDate.DatePrecision precision, boolean hasDay,
		boolean hasMonth, boolean hasYear){}


	private static final PatternMatch[] PATTERNS = new PatternMatch[]{
		createPattern("d MMMM uuuu", ParsedGenealogicalDate.DatePrecision.EXACT, true, true, true),
		createPattern("d MMM uuuu", ParsedGenealogicalDate.DatePrecision.EXACT, true, true, true),
		createPattern("d M uuuu", ParsedGenealogicalDate.DatePrecision.EXACT, true, true, true),
		createPattern("MMMM uuuu", ParsedGenealogicalDate.DatePrecision.YEAR_MONTH, false, true, true),
		createPattern("MMM uuuu", ParsedGenealogicalDate.DatePrecision.YEAR_MONTH, false, true, true),
		createPattern("M uuuu", ParsedGenealogicalDate.DatePrecision.YEAR_MONTH, false, true, true),
		createPattern("uuuu", ParsedGenealogicalDate.DatePrecision.YEAR_ONLY, false, false, true),
		createPattern("d MMMM uuu", ParsedGenealogicalDate.DatePrecision.EXACT, true, true, true),
		createPattern("d MMM uuu", ParsedGenealogicalDate.DatePrecision.EXACT, true, true, true),
		createPattern("d M uuu", ParsedGenealogicalDate.DatePrecision.EXACT, true, true, true),
		createPattern("MMMM uuu", ParsedGenealogicalDate.DatePrecision.YEAR_MONTH, false, true, true),
		createPattern("MMM uuu", ParsedGenealogicalDate.DatePrecision.YEAR_MONTH, false, true, true),
		createPattern("M uuu", ParsedGenealogicalDate.DatePrecision.YEAR_MONTH, false, true, true),
		createPattern("uuu", ParsedGenealogicalDate.DatePrecision.YEAR_ONLY, false, false, true),
		createPattern("d MMMM", ParsedGenealogicalDate.DatePrecision.MONTH_DAY, true, true, false),
		createPattern("d MMM", ParsedGenealogicalDate.DatePrecision.MONTH_DAY, true, true, false),
		createPattern("d M", ParsedGenealogicalDate.DatePrecision.MONTH_DAY, true, true, false)
	};

	private static PatternMatch createPattern(final String pattern,
		final ParsedGenealogicalDate.DatePrecision precision, final boolean hasDay, final boolean hasMonth,
		final boolean hasYear) {
		final DateTimeFormatter dtf = new DateTimeFormatterBuilder()
			.parseCaseInsensitive()
			.appendPattern(pattern)
			.parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
			.parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
			.parseDefaulting(ChronoField.YEAR, Year.now().getValue())
			.toFormatter(Locale.ENGLISH);
		return new PatternMatch(dtf, precision, hasDay, hasMonth, hasYear);
	}

	public static ParsedGenealogicalDate parse(final String calendarCode, final String rawDate){
		final CalendarType type = CalendarType.fromCode(calendarCode);

		String workingDate = Objects.requireNonNull(rawDate, "rawDate cannot be null")
			.trim();
		if(workingDate.isEmpty())
			throw new IllegalArgumentException("Date string is required");

		// Strip embedded calendar escape tags (e.g. @#DGREGORIAN@)
		if(workingDate.startsWith("@#") && workingDate.contains("@")){
			workingDate = StringUtils.substringAfter(workingDate, "@")
				.trim();
			if(workingDate.startsWith("#"))
				workingDate = StringUtils.substringAfter(workingDate, "@")
					.trim();
		}

		boolean isApproximate = false;
		for(final String prefix : GEDCOM_PREFIXES)
			if(Strings.CI.startsWith(workingDate, prefix)){
				isApproximate = true;
				workingDate = StringUtils.stripStart(workingDate.substring(prefix.length()), null);

				break;
			}

		// Replace standard GEDCOM / date delimiters ('/', '.', '-') with spaces
		workingDate = StringUtils.replaceChars(workingDate, "/.-", "   ");
		final String cleanedDate = StringUtils.normalizeSpace(workingDate);
		final ParsePosition pos = new ParsePosition(0);
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
					case GREGORIAN -> parseJsr310(IsoChronology.INSTANCE, accessor, pm);
					case JULIAN -> parseJsr310(JulianChronology.INSTANCE, accessor, pm);
					case ISLAMIC -> parseJsr310(HijrahChronology.INSTANCE, accessor, pm);
					case BUDDHIST -> parseJsr310(ThaiBuddhistChronology.INSTANCE, accessor, pm);
					case COPTIC -> parseJsr310(CopticChronology.INSTANCE, accessor, pm);
					case ETHIOPIAN -> parseJsr310(EthiopicChronology.INSTANCE, accessor, pm);

					case HEBREW -> parseIcu4j(new HebrewCalendar(), accessor, pm);
					case CHINESE -> parseIcu4j(new ChineseCalendar(), accessor, pm);
					case INDIAN -> parseIcu4j(new IndianCalendar(), accessor, pm);

					case FRENCH_REPUBLICAN -> parseFrenchRepublican(accessor, pm);
					case SOVIET_ETERNAL -> parseSovietEternal(accessor, pm);
					case MAYAN -> parseMayanLongCount(cleanedDate);
				};

				return new ParsedGenealogicalDate(resultIso, pm.precision(), isApproximate, rawDate, type);
			}
			catch(final Exception ignored){}
		}

		throw new IllegalArgumentException("Unable to parse date '" + rawDate + "' for calendar " + calendarCode);
	}


	private static LocalDate parseJsr310(final Chronology chrono, final TemporalAccessor accessor,
			final PatternMatch pm){
		final int year = (pm.hasYear()? accessor.get(ChronoField.YEAR): Year.now().getValue());
		final int month = (pm.hasMonth()? accessor.get(ChronoField.MONTH_OF_YEAR): 1);
		final int day = (pm.hasDay()? accessor.get(ChronoField.DAY_OF_MONTH): 1);

		final ChronoLocalDate cld = chrono.date(year, month, day);
		return LocalDate.from(cld);
	}

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

	private static LocalDate parseFrenchRepublican(final TemporalAccessor accessor, final PatternMatch pm){
		final int day = (pm.hasDay()? accessor.get(ChronoField.DAY_OF_MONTH): 1);
		final int month = (pm.hasMonth()? accessor.get(ChronoField.MONTH_OF_YEAR): 1);
		final int year = (pm.hasYear()? accessor.get(ChronoField.YEAR): 1);

		final LocalDate epoch = LocalDate.of(1792, 9, 22);
		final long daysToAdd = (year - 1) * 365l + (year / 4) + (month - 1) * 30l + (day - 1);
		return epoch.plusDays(daysToAdd);
	}

	private static LocalDate parseSovietEternal(final TemporalAccessor accessor, final PatternMatch pm){
		// Soviet Revolutionary Calendar (1929-1940): 12 months of 30 days each + 5/6 holidays without a month.
		// The months are numbered according to the Julian/Gregorian calendar.
		return parseJsr310(IsoChronology.INSTANCE, accessor, pm);
	}

	private static LocalDate parseMayanLongCount(final String input){
		// Mayan format: Baktun.Katun.Tun.Uinal.Kin (e.g. 13.0.0.0.0)
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
