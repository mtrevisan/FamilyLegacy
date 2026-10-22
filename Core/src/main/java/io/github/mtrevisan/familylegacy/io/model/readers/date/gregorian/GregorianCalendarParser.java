package io.github.mtrevisan.familylegacy.io.model.readers.date.gregorian;

import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarType;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * High-fidelity polyglot parser for standard solar calendars (Gregorian, Julian, Reformed Julian).
 * Dynamically resolves month names (full or abbreviated) across all system locales and intercepts seasonal text markers.
 */
public final class GregorianCalendarParser{

	private static final Map<String, Integer> SEASONS = new HashMap<>();
	static{
		// AUTOMATIC GLOBAL SEASON REGISTRATION
		// Sweep across all available system locales on startup and dynamically extract
		// localized strings for quarters/seasons to completely avoid manual code maintenance.
		for(final Locale locale : Locale.getAvailableLocales()){
			try{
				// 1. Extract Quarter-of-Year localized textual names from java.time (mapping onto astronomical seasons)
				// Q1 = Spring, Q2 = Summer, Q3 = Autumn, Q4 = Winter (Northern Civil/Astronomical baselines)
				final DateTimeFormatter quarterFormatterLong = new DateTimeFormatterBuilder()
					.appendPattern("qqqq")
					.toFormatter(locale);
				final DateTimeFormatter quarterFormatterShort = new DateTimeFormatterBuilder()
					.appendPattern("qqq")
					.toFormatter(locale);

				// Sample dummy date elements to extract localized strings safely
				registerSeasonString(quarterFormatterLong.format(LocalDate.of(2000, 3, 21)), CalendarType.SEASON_SPRING);
				registerSeasonString(quarterFormatterShort.format(LocalDate.of(2000, 3, 21)), CalendarType.SEASON_SPRING);

				registerSeasonString(quarterFormatterLong.format(LocalDate.of(2000, 6, 21)), CalendarType.SEASON_SUMMER);
				registerSeasonString(quarterFormatterShort.format(LocalDate.of(2000, 6, 21)), CalendarType.SEASON_SUMMER);

				registerSeasonString(quarterFormatterLong.format(LocalDate.of(2000, 9, 21)), CalendarType.SEASON_AUTUMN);
				registerSeasonString(quarterFormatterShort.format(LocalDate.of(2000, 9, 21)), CalendarType.SEASON_AUTUMN);

				registerSeasonString(quarterFormatterLong.format(LocalDate.of(2000, 12, 21)), CalendarType.SEASON_WINTER);
				registerSeasonString(quarterFormatterShort.format(LocalDate.of(2000, 12, 21)), CalendarType.SEASON_WINTER);
			}
			catch(Exception ignored){
			}
		}

		// Hardcoded fallback anchors for core legacy and shorthand symbols
		SEASONS.put("aut", CalendarType.SEASON_AUTUMN);
		SEASONS.put("win", CalendarType.SEASON_WINTER);
		SEASONS.put("spr", CalendarType.SEASON_SUMMER);
		SEASONS.put("sum", CalendarType.SEASON_SUMMER);
	}

	private static void registerSeasonString(final String text, final int flag){
		if(StringUtils.isNotBlank(text))
			SEASONS.put(text.toLowerCase(Locale.ROOT).trim(), flag);
	}

	// Robust, exception-free Unicode pattern supporting optional day prefix and localized alphabets
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?([\\p{L}\\p{M}\\s-]+?)\\s+(\\d+)");


	private GregorianCalendarParser(){}


	/**
	 * Parses a raw Gregorian text expression into a validated structural container using exact flags.
	 * Dynamically resolves month names (full or abbreviated) across all system locales.
	 *
	 * @param rawDate the string representing the Gregorian date (e.g., "15 Gennaio 1953", "Aug 2026", "Estate 1953")
	 * @return a configured GregorianDateInput instance
	 * @throws IllegalArgumentException if the text layout expression matches an invalid layout format or unknown month
	 */
	public static GregorianDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Gregorian date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.trim();
		final int year = Integer.parseInt(matcher.group(3));

		// 1. Check if the token maps straight to a registered seasonal marker
		final String lowerMonthName = monthName.toLowerCase(Locale.ROOT);
		if(SEASONS.containsKey(lowerMonthName)){
			final int seasonFlag = SEASONS.get(lowerMonthName);
			final int day = (dayGroup != null) ? Integer.parseInt(dayGroup) : 1;
			return new GregorianDateInput(year, seasonFlag, day);
		}

		// 2. Resolve multi-lingual full or abbreviated months using internal Java internationalization tables
		final int month = resolvePolyglotMonth(monthName);

		// Operational fallback for standalone seasonal records
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 1);

		if(month > 0 && (day < 1 || day > 31))
			throw new IllegalArgumentException("Day variable breaches standard monthly boundaries.");

		return new GregorianDateInput(year, month, day);
	}

	/**
	 * Iterates over available network locales to map full or abbreviated text back to its calendar numeric month index.
	 */
	private static int resolvePolyglotMonth(final String token){
		for(final Locale locale : Locale.getAvailableLocales()){
			final int month = parseMonthWithLocale(token, locale);
			if(month != -1)
				return month;
		}

		throw new IllegalArgumentException("Unknown Gregorian month or seasonal descriptor across global locale parameters: " + token);
	}

	private static int parseMonthWithLocale(final String token, final Locale locale){
		try{
			// Build a dynamic parser instance configured to match full (MMMM), short (MMM), or numeric (M) textual components
			final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
				.parseCaseInsensitive()
				.appendPattern("[MMMM][MMM][M]")
				.toFormatter(locale);

			return formatter.parse(token)
				.get(ChronoField.MONTH_OF_YEAR);
		}
		catch(Exception e){
			return -1;
		}
	}

}
