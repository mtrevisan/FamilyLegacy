package io.github.mtrevisan.calendars.ethiopian;

import io.github.mtrevisan.calendars.CalendarType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for Ethiopian calendar date expressions using exact astronomical mapping flags.
 * Supports standard month names, transliterations, and native Amharic seasonal markers.
 */
public final class EthiopianCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Month 1: Mäskeräm (New Year / Enkutatash)
		MONTHS.put("መስከረም", 1);             // Native Fidäl script
		MONTHS.put("mäskeräm", 1);          // Scientific linguistic transliteration
		MONTHS.put("meskerem", 1);          // Standard ASCII variant
		MONTHS.put("maskaram", 1);          // Common phonetic variation

		// Month 2: Ṭəqəmt
		MONTHS.put("ጥቅምት", 2);
		MONTHS.put("ṭəqəmt", 2);
		MONTHS.put("tekemt", 2);
		MONTHS.put("tseqemt", 2);           // Common phonetic rendering of the explosive T (ጠ)
		MONTHS.put("tiqimt", 2);            // Alternate vowel notation

		// Month 3: ḫədar
		MONTHS.put("ህዳር", 3);
		MONTHS.put("ḫədar", 3);
		MONTHS.put("hedar", 3);
		MONTHS.put("hidar", 3);             // Most frequent English/Amharic media spelling

		// Month 4: Taḫśaś
		MONTHS.put("ታኅሣሥ", 4);
		MONTHS.put("taḫśaś", 4);
		MONTHS.put("tahasas", 4);
		MONTHS.put("tahsas", 4);            // Standard plain text variant

		// Month 5: Ṭərr
		MONTHS.put("ጥር", 5);
		MONTHS.put("ṭərr", 5);
		MONTHS.put("ter", 5);
		MONTHS.put("tir", 5);               // Alternate vowel notation

		// Month 6: Yäkatit
		MONTHS.put("የካቲት", 6);
		MONTHS.put("yäkatit", 6);
		MONTHS.put("yekatit", 6);
		MONTHS.put("yakatit", 6);

		// Month 7: Mäggabit
		MONTHS.put("መጋቢት", 7);
		MONTHS.put("maggabit", 7);
		MONTHS.put("megabit", 7);
		MONTHS.put("magabit", 7);

		// Month 8: Miyazya
		MONTHS.put("ሚያዝያ", 8);
		MONTHS.put("miyazya", 8);
		MONTHS.put("miyazia", 8);
		MONTHS.put("miazia", 8);

		// Month 9: Gənboṭ
		MONTHS.put("ግንቦት", 9);
		MONTHS.put("gənboṭ", 9);
		MONTHS.put("genbot", 9);
		MONTHS.put("ginbot", 9);            // Highly frequent alternate spelling

		// Month 10: Sänē
		MONTHS.put("ሰኔ", 10);
		MONTHS.put("sänē", 10);
		MONTHS.put("sene", 10);
		MONTHS.put("sanie", 10);

		// Month 11: Ḥamle
		MONTHS.put("ሐምሌ", 11);
		MONTHS.put("ḥamle", 11);
		MONTHS.put("hamle", 11);
		MONTHS.put("hamlie", 11);

		// Month 12: Nähase
		MONTHS.put("ነሐሴ", 12);
		MONTHS.put("nähase", 12);
		MONTHS.put("nehase", 12);
		MONTHS.put("nahase", 12);

		// Month 13: Pagumēn (Intercalary Month / 5 or 6 Epagomenal Days)
		MONTHS.put("ጳጉሜን", 13);
		MONTHS.put("pagumēn", 13);
		MONTHS.put("pagumen", 13);
		MONTHS.put("pagume", 13);           // Truncated spoken form


		// =============================================================================
		// ETHIOPIAN SEASONS ALIGNMENT (Traditional Agricultural & Literary Grid)
		// =============================================================================

		// Season 1: Bega (The Long Dry Season - Aligned to Winter Cross-grid)
		MONTHS.put("በጋ", CalendarType.SEASON_WINTER);
		MONTHS.put("bega", CalendarType.SEASON_WINTER);

		// Season 2: Belg (The Short Rainy Season / Spring Transition)
		MONTHS.put("በልግ", CalendarType.SEASON_SPRING);
		MONTHS.put("belg", CalendarType.SEASON_SPRING);

		// Season 3: Kremt (The Heavy Monsoon Rainy Season - Aligned to Summer Cross-grid)
		MONTHS.put("ክረምት", CalendarType.SEASON_SUMMER);
		MONTHS.put("keremt", CalendarType.SEASON_SUMMER);
		MONTHS.put("kremt", CalendarType.SEASON_SUMMER);

		// Season 4: Tseday (The Blossom Season / Post-Rain Autumn - FIXED and mapped to Autumn)
		MONTHS.put("ጸደይ", CalendarType.SEASON_AUTUMN);     // Native Amharic script for Autumn bloom
		MONTHS.put("tseday", CalendarType.SEASON_AUTUMN);    // Correct astronomical alignment
		MONTHS.put("ceday", CalendarType.SEASON_AUTUMN);     // Linguistic ISO variant
	}

	// Robust, exception-free Unicode pattern supporting optional day prefix and world alphabets
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?([\\p{L}\\p{M}\\s-]+?)\\s+(\\d+)");


	private EthiopianCalendarParser(){}


	/**
	 * Parses a raw Ethiopian text string into a validated structural container using exact flags.
	 *
	 * @param rawDate the string representing the calendar date (e.g., "Keremt 2015")
	 * @return a configured EthiopianDateInput instance
	 * @throws IllegalArgumentException if the text layout expression matches an invalid layout format
	 */
	public static EthiopianDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);

		// A year alone (e.g. the start of a decade or century) stands for the first day of the year
		if(StringUtils.isNumeric(normalized))
			return new EthiopianDateInput(Integer.parseInt(normalized), 0, 0);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Ethiopian date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.trim();
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Ethiopian month or seasonal descriptor: " + monthName);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 0);

		if(dayGroup != null && month > 0 && month <= 12 && (day < 1 || day > 30))
			throw new IllegalArgumentException("Standard Ethiopian months consist strictly of 30 days.");
		if(dayGroup != null && month == 13 && (day < 1 || day > 6))
			throw new IllegalArgumentException("Pagumēn intercalary days are bound between 1 and 6 days.");

		return new EthiopianDateInput(year, month, day);
	}

}
