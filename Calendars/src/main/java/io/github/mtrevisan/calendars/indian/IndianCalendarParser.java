package io.github.mtrevisan.calendars.indian;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for Indian National calendar date expressions using exact astronomical mapping flags.
 * Supports standard month names, regional variants, and traditional Ritu markers.
 */
public final class IndianCalendarParser{

	public static final int RITU_SHARAD = -1;
	public static final int RITU_SHISHIRA = -2;
	public static final int RITU_VASANTA = -3;
	public static final int RITU_GRISHMA = -4;
	public static final int RITU_VARSHA = -5;
	public static final int RITU_HEMANTA = -6;


	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Standard Saka Months (Sanskrit transliterations)
		MONTHS.put("chaitra", 1);
		MONTHS.put("caitra", 1);
		MONTHS.put("vaisakha", 2);
		MONTHS.put("vaishakha", 2);
		MONTHS.put("jyaistha", 3);
		MONTHS.put("jyaist'ha", 3);
		MONTHS.put("jaishtha", 3);
		MONTHS.put("asadha", 4);
		MONTHS.put("ashadha", 4);
		MONTHS.put("shravana", 5);
		MONTHS.put("sravana", 5);
		MONTHS.put("sawan", 5);
		MONTHS.put("bhadra", 6);
		MONTHS.put("bhadrapada", 6);
		MONTHS.put("asvina", 7);
		MONTHS.put("asvina", 7);
		MONTHS.put("ashwin", 7);
		MONTHS.put("kartika", 8);
		MONTHS.put("kartik", 8);
		MONTHS.put("agrahayana", 9);
		MONTHS.put("margashirsha", 9);
		MONTHS.put("margsirsa", 9);
		MONTHS.put("pausa", 10);
		MONTHS.put("pousha", 10);
		MONTHS.put("poush", 10);
		MONTHS.put("magha", 11);
		MONTHS.put("magh", 11);
		MONTHS.put("phalguna", 12);
		MONTHS.put("falgun", 12);

		// Sharad (Autumn -> Centered near the Autumnal Equinox)
		MONTHS.put("sharad", RITU_SHARAD);
		// Shishira (Winter -> Centered near the Hibernal Solstice)
		MONTHS.put("shishira", RITU_SHISHIRA);
		MONTHS.put("sisira", RITU_SHISHIRA);
		// Vasanta (Spring -> Centered near the Vernal Equinox)
		MONTHS.put("vasanta", RITU_VASANTA);
		MONTHS.put("basant", RITU_VASANTA);
		// Grishma (Summer -> Centered near the Estival Solstice)
		MONTHS.put("grishma", RITU_GRISHMA);
		// Varsha (Monsoon) -> Maps to mid-summer/rainy transition point
		MONTHS.put("varsha", RITU_VARSHA);
		// Hemanta (Pre-Winter) -> Maps to late autumn transition
		MONTHS.put("hemanta", RITU_HEMANTA);
	}

	// Robust, exception-free Unicode pattern supporting optional day prefix and localized alphabets
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?([\\p{L}\\p{M}\\s-]+?)\\s+(\\d+)");


	private IndianCalendarParser(){}


	/**
	 * Parses a raw Indian calendar text expression into a validated structural container using exact flags.
	 *
	 * @param rawDate the string representing the calendar date (e.g., "Vasanta 1920" or "1 Chaitra 1879")
	 * @return a configured IndianDateInput instance
	 * @throws IllegalArgumentException if the text layout expression matches an invalid layout format
	 */
	public static IndianDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);

		// A year alone (e.g. the start of a decade or century) stands for the first day of the year
		if(StringUtils.isNumeric(normalized))
			return new IndianDateInput(Integer.parseInt(normalized), 0, 0);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Indian calendar date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.trim();
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Indian month or Ritu descriptor: " + monthName);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 0);

		if(dayGroup != null && month > 0 && (day < 1 || day > 31))
			throw new IllegalArgumentException("Day variable breaches standard monthly boundaries.");

		return new IndianDateInput(year, month, day);
	}

}
