package io.github.mtrevisan.familylegacy.v2.ui.helpers;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ChineseCalendar;
import com.ibm.icu.util.HebrewCalendar;
import com.ibm.icu.util.IndianCalendar;
import org.threeten.extra.chrono.CopticChronology;
import org.threeten.extra.chrono.EthiopicChronology;
import org.threeten.extra.chrono.JulianChronology;

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
 * Service to parse partial or full date strings into GenealogicalDate objects.
 */
public final class UniversalDateConverter{

	private record PatternMatch(String pattern, ParsedGenealogicalDate.DatePrecision precision, boolean hasDay,
		boolean hasMonth, boolean hasYear){}


	private static final PatternMatch[] PATTERNS = new PatternMatch[]{
		new PatternMatch("d MMMM uuuu", ParsedGenealogicalDate.DatePrecision.EXACT, true, true, true),
		new PatternMatch("d MMM uuuu", ParsedGenealogicalDate.DatePrecision.EXACT, true, true, true),
		new PatternMatch("d M uuuu", ParsedGenealogicalDate.DatePrecision.EXACT, true, true, true),
		new PatternMatch("MMMM uuuu", ParsedGenealogicalDate.DatePrecision.YEAR_MONTH, false, true, true),
		new PatternMatch("MMM uuuu", ParsedGenealogicalDate.DatePrecision.YEAR_MONTH, false, true, true),
		new PatternMatch("M uuuu", ParsedGenealogicalDate.DatePrecision.YEAR_MONTH, false, true, true),
		new PatternMatch("d MMMM", ParsedGenealogicalDate.DatePrecision.MONTH_DAY, true, true, false),
		new PatternMatch("d MMM", ParsedGenealogicalDate.DatePrecision.MONTH_DAY, true, true, false),
		new PatternMatch("d M", ParsedGenealogicalDate.DatePrecision.MONTH_DAY, true, true, false),
		new PatternMatch("uuuu", ParsedGenealogicalDate.DatePrecision.YEAR_ONLY, false, false, true)
	};

	public static ParsedGenealogicalDate parse(final String calendarCode, final String rawDate){
		final CalendarType type = CalendarType.fromCode(calendarCode);
		final String cleanedDate = rawDate.trim()
			.replaceAll("[/.\\-]", " ");

		for(final PatternMatch pm : PATTERNS){
			try{
				final LocalDate resultIso = switch(type){
					case GREGORIAN -> parseJsr310(IsoChronology.INSTANCE, cleanedDate, pm);
					case JULIAN -> parseJsr310(JulianChronology.INSTANCE, cleanedDate, pm);
					case ISLAMIC -> parseJsr310(HijrahChronology.INSTANCE, cleanedDate, pm);
					case BUDDHIST -> parseJsr310(ThaiBuddhistChronology.INSTANCE, cleanedDate, pm);
					case COPTIC -> parseJsr310(CopticChronology.INSTANCE, cleanedDate, pm);
					case ETHIOPIAN -> parseJsr310(EthiopicChronology.INSTANCE, cleanedDate, pm);

					case HEBREW -> parseIcu4j(new HebrewCalendar(), cleanedDate, pm);
					case CHINESE -> parseIcu4j(new ChineseCalendar(), cleanedDate, pm);
					case INDIAN -> parseIcu4j(new IndianCalendar(), cleanedDate, pm);

					case FRENCH_REPUBLICAN -> parseFrenchRepublican(cleanedDate, pm);
					case SOVIET_ETERNAL -> parseSovietEternal(cleanedDate, pm);
					case MAYAN -> parseMayanLongCount(cleanedDate);
				};

				return new ParsedGenealogicalDate(resultIso, pm.precision(), rawDate, type);
			}
			catch(Exception ignored){}
		}

		throw new IllegalArgumentException("Unable to parse date '" + rawDate + "' for calendar " + calendarCode);
	}


	private static LocalDate parseJsr310(final Chronology chrono, final String input, final PatternMatch pm){
		final DateTimeFormatter dtf = new DateTimeFormatterBuilder()
			.parseCaseInsensitive()
			.appendPattern(pm.pattern())
			.parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
			.parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
			.parseDefaulting(ChronoField.YEAR, Year.now().getValue())
			.toFormatter(Locale.ENGLISH);

		final ChronoLocalDate cld = chrono.date(dtf.parse(input));
		return LocalDate.from(cld);
	}

	private static LocalDate parseIcu4j(final Calendar cal, final String input, final PatternMatch pm){
		final DateTimeFormatter dtf = new DateTimeFormatterBuilder()
			.parseCaseInsensitive()
			.appendPattern(pm.pattern())
			.parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
			.parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
			.parseDefaulting(ChronoField.YEAR, Year.now().getValue())
			.toFormatter(Locale.ENGLISH);

		final TemporalAccessor accessor = dtf.parse(input);
		final int day = (pm.hasDay()? accessor.get(ChronoField.DAY_OF_MONTH): 1);
		final int month = (pm.hasMonth()? accessor.get(ChronoField.MONTH_OF_YEAR) - 1: 0);
		final int year = (pm.hasYear()? accessor.get(ChronoField.YEAR): Year.now().getValue());

		cal.clear();
		cal.setTimeZone(TimeZone.getTimeZone("UTC"));
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DAY_OF_MONTH, day);

		final long millis = cal.getTimeInMillis();
		return Instant.ofEpochMilli(millis)
			.atZone(ZoneOffset.UTC)
			.toLocalDate();
	}

	private static LocalDate parseFrenchRepublican(final String input, final PatternMatch pm){
		final DateTimeFormatter dtf = new DateTimeFormatterBuilder()
			.parseCaseInsensitive()
			.appendPattern(pm.pattern())
			.parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
			.parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
			.parseDefaulting(ChronoField.YEAR, 1)
			.toFormatter(Locale.ENGLISH);

		final TemporalAccessor accessor = dtf.parse(input);
		final int day = (pm.hasDay()? accessor.get(ChronoField.DAY_OF_MONTH): 1);
		final int month = (pm.hasMonth()? accessor.get(ChronoField.MONTH_OF_YEAR): 1);
		final int year = (pm.hasYear()? accessor.get(ChronoField.YEAR): 1);

		final LocalDate epoch = LocalDate.of(1792, 9, 22);
		final long daysToAdd = (year - 1) * 365l + (year / 4) + (month - 1) * 30l + (day - 1);
		return epoch.plusDays(daysToAdd);
	}

	private static LocalDate parseSovietEternal(final String input, final PatternMatch pm){
		// Soviet Revolutionary Calendar (1929-1940): 12 months of 30 days each + 5/6 holidays without a month.
		// The months are numbered according to the Julian/Gregorian calendar.
		return parseJsr310(IsoChronology.INSTANCE, input, pm);
	}

	private static LocalDate parseMayanLongCount(String input){
		// Mayan format: Baktun.Katun.Tun.Uinal.Kin (es. 13.0.0.0.0)
		String[] parts = input.split("\\s+");
		if(parts.length < 5){
			throw new IllegalArgumentException("Invalid Mayan date. Requested format: 'Baktun Katun Tun Uinal Kin'");
		}
		long baktun = Long.parseLong(parts[0]);
		long katun = Long.parseLong(parts[1]);
		long tun = Long.parseLong(parts[2]);
		long uinal = Long.parseLong(parts[3]);
		long kin = Long.parseLong(parts[4]);

		long totalDays = baktun * 144000 + katun * 7200 + tun * 360 + uinal * 20 + kin;

		// Mayan Era GMT Correlation (11 August 3114 BC proleptic Gregorian = JDN 584283)
		LocalDate mayanEpoch = LocalDate.of(-3113, 8, 11);
		return mayanEpoch.plusDays(totalDays);
	}

}
