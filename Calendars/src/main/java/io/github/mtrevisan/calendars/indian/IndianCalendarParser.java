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
		// Month 1: Chaitra (Starts on Vernal Equinox - March 22 / March 21 in leap years)
		MONTHS.put("चैत्र", 1);             // Native Devanagari script
		MONTHS.put("chaitra", 1);           // Classical Sanskrit transliteration
		MONTHS.put("caitra", 1);            // Standard ISO transliteration
		MONTHS.put("chithirai", 1);         // Tamil regional phonetic variant
		MONTHS.put("choitro", 1);           // Bengali regional phonetic variant

		// Month 2: Vaisakha
		MONTHS.put("वैशाख", 2);
		MONTHS.put("vaisakha", 2);
		MONTHS.put("vaishakha", 2);
		MONTHS.put("boishak", 2);           // Highly common Bengali/Commercial variant
		MONTHS.put("vaigasi", 2);           // Tamil regional alignment variant

		// Month 3: Jyaistha
		MONTHS.put("ज्येष्ठ", 3);
		MONTHS.put("jyaistha", 3);
		MONTHS.put("jyaist'ha", 3);
		MONTHS.put("jaishtha", 3);
		MONTHS.put("jeth", 3);              // Common Hindi vernacular short form

		// Month 4: Asadha
		MONTHS.put("आषाढ़", 4);
		MONTHS.put("asadha", 4);
		MONTHS.put("ashadha", 4);
		MONTHS.put("aashadh", 4);           // Phonetic long-vowel variant

		// Month 5: Shravana
		MONTHS.put("श्रावण", 5);
		MONTHS.put("shravana", 5);
		MONTHS.put("sravana", 5);
		MONTHS.put("sawan", 5);             // Vernacular Hindi/Prakrit form
		MONTHS.put("shrabon", 5);           // Bengali regional variant

		// Month 6: Bhadra / Bhadrapada
		MONTHS.put("भाद्रपद", 6);
		MONTHS.put("भाโด", 6);
		MONTHS.put("bhadra", 6);
		MONTHS.put("bhadrapada", 6);
		MONTHS.put("bhadoo", 6);            // Vernacular short form

		// Month 7: Asvina
		MONTHS.put("आश्विन", 7);
		MONTHS.put("asvina", 7);
		MONTHS.put("ashwin", 7);            // Most common colloquial spelling
		MONTHS.put("asvin", 7);

		// Month 8: Kartika
		MONTHS.put("कार्तिक", 8);
		MONTHS.put("kartika", 8);
		MONTHS.put("kartik", 8);            // Standard colloquial short form

		// Month 9: Agrahayana / Margashirsha
		MONTHS.put("मार्गशीर्ष", 9);
		MONTHS.put("अग्रहायण", 9);           // Alternate traditional Sanskrit name
		MONTHS.put("agrahayana", 9);
		MONTHS.put("margashirsha", 9);
		MONTHS.put("margsirsa", 9);
		MONTHS.put("margsir", 9);           // Common short form

		// Month 10: Pausa
		MONTHS.put("पौष", 10);
		MONTHS.put("pausa", 10);
		MONTHS.put("pousha", 10);
		MONTHS.put("poush", 10);
		MONTHS.put("pous", 10);

		// Month 11: Magha
		MONTHS.put("माघ", 11);
		MONTHS.put("magha", 11);
		MONTHS.put("magh", 11);

		// Month 12: Phalguna
		MONTHS.put("फाल्गुन", 12);
		MONTHS.put("phalguna", 12);
		MONTHS.put("falgun", 12);           // Highly common simplified phonetic format


		// 1. Vasanta (Spring Cycle - Centered around Vernal Equinox)
		MONTHS.put("वसन्त", RITU_VASANTA);    // Native Devanagari script
		MONTHS.put("vasanta", RITU_VASANTA);
		MONTHS.put("basant", RITU_VASANTA);

		// 2. Grishma (Summer Cycle - Centered around Estival Solstice)
		MONTHS.put("ग्रीष्म", RITU_GRISHMA);   // Native Devanagari script
		MONTHS.put("grishma", RITU_GRISHMA);

		// 3. Varsha (Monsoon Cycle - Aligned to mid/late Summer rainy transition parameters)
		MONTHS.put("वर्षा", RITU_VARSHA);     // Native Devanagari script
		MONTHS.put("varsha", RITU_VARSHA);

		// 4. Sharad (Autumn Cycle - Centered around Autumnal Equinox)
		MONTHS.put("शरद", RITU_SHARAD);     // Native Devanagari script
		MONTHS.put("sharad", RITU_SHARAD);

		// 5. Hemanta (Pre-Winter Cycle - Late Autumn / Early Winter transition)
		MONTHS.put("हेमन्त", RITU_HEMANTA);    // Native Devanagari script
		MONTHS.put("hemanta", RITU_HEMANTA);

		// 6. Shishira (Winter Cycle - Centered around Hibernal Solstice)
		MONTHS.put("शिशिर", RITU_SHISHIRA);    // Native Devanagari script
		MONTHS.put("shishira", RITU_SHISHIRA);
		MONTHS.put("sisira", RITU_SHISHIRA);
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
