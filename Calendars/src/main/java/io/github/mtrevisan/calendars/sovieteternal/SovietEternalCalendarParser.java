/**
*
*/
package io.github.mtrevisan.calendars.sovieteternal;

import io.github.mtrevisan.calendars.CalendarType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for Soviet Eternal calendar date expressions using exact astronomical mapping flags.
 * Supports canonical month names, administrative holiday markers, and native seasonal terms.
 */
public final class SovietEternalCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Month 1: January (Январь)
		MONTHS.put("январь", 1);           // Native Cyrillic script
		MONTHS.put("yanvar", 1);            // Standard Romanization
		MONTHS.put("januar", 1);            // Alternate European phonetic variant

		// Month 2: February (Февраль)
		MONTHS.put("февраль", 2);
		MONTHS.put("fevral", 2);
		MONTHS.put("fevral'", 2);           // Scientific transliteration showing soft sign

		// Month 3: March (Март)
		MONTHS.put("март", 3);
		MONTHS.put("mart", 3);

		// Month 4: April (Апрель)
		MONTHS.put("апрель", 4);
		MONTHS.put("aprel", 4);
		MONTHS.put("aprel'", 4);

		// Month 5: May (Май)
		MONTHS.put("май", 5);
		MONTHS.put("may", 5);
		MONTHS.put("maj", 5);

		// Month 6: June (Июнь)
		MONTHS.put("июнь", 6);
		MONTHS.put("iyun", 6);
		MONTHS.put("ijun", 6);

		// Month 7: July (Июль)
		MONTHS.put("июль", 7);
		MONTHS.put("iyul", 7);
		MONTHS.put("ijul", 7);

		// Month 8: August (Август)
		MONTHS.put("август", 8);
		MONTHS.put("avgust", 8);

		// Month 9: September (Сентябрь)
		MONTHS.put("сентябрь", 9);
		MONTHS.put("sentyabr", 9);
		MONTHS.put("sentjabr", 9);

		// Month 10: October (Октябрь)
		MONTHS.put("октябрь", 10);
		MONTHS.put("oktyabr", 10);
		MONTHS.put("oktjabr", 10);

		// Month 11: November (Ноябрь)
		MONTHS.put("ноябрь", 11);
		MONTHS.put("noyabr", 11);
		MONTHS.put("nojabr", 11);

		// Month 12: December (Декабрь)
		MONTHS.put("декабрь", 12);
		MONTHS.put("dekabr", 12);
		MONTHS.put("dekabr'", 12);

		// --- INTERCALARY STANDALONE REVOLUTIONARY HOLIDAYS ---
		// Month 13: Collective and individual legal name allocations for non-month days
		MONTHS.put("revolutionary-holidays", 13);
		MONTHS.put("holiday-days", 13);
		MONTHS.put("день ленина", 13);       // Lenin Day (Observed January 22nd)
		MONTHS.put("lenin day", 13);
		MONTHS.put("дни труда", 13);         // International Labor Days (Observed May 1st & 2nd)
		MONTHS.put("labor days", 13);
		MONTHS.put("индустриальные дни", 13); // Industry Days (Observed November 7th & 8th)
		MONTHS.put("industry days", 13);
		MONTHS.put("високосный день", 13);   // Intercalary Leap Day (Extra Revolutionary Holiday)

		// --- SOVIET PRODUCTION WEEK COLOR SHIFT TOKENS ---
		// These tokens are routed to index 0 to flag continuous shift tracking layers
		MONTHS.put("желтый", 0);            // Yellow Shift (Жёлтый)
		MONTHS.put("yellow", 0);
		MONTHS.put("розовый", 0);           // Pink Shift
		MONTHS.put("pink", 0);
		MONTHS.put("красный", 0);           // Red Shift
		MONTHS.put("red", 0);
		MONTHS.put("фиолетовый", 0);        // Purple Shift
		MONTHS.put("purple", 0);
		MONTHS.put("зеленый", 0);           // Green Shift (Зелёный)
		MONTHS.put("green", 0);


		// Autumn
		MONTHS.put("осень", CalendarType.SEASON_AUTUMN);
		MONTHS.put("осен", CalendarType.SEASON_AUTUMN);            // Truncated phonetic token
		MONTHS.put("osen", CalendarType.SEASON_AUTUMN);

		// Winter
		MONTHS.put("зима", CalendarType.SEASON_WINTER);
		MONTHS.put("zima", CalendarType.SEASON_WINTER);

		// Spring
		MONTHS.put("весна", CalendarType.SEASON_SPRING);
		MONTHS.put("vesna", CalendarType.SEASON_SPRING);

		// Summer
		MONTHS.put("лето", CalendarType.SEASON_SUMMER);
		MONTHS.put("leto", CalendarType.SEASON_SUMMER);
	}

	// Robust, exception-free Unicode pattern supporting optional day prefix and world alphabets
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?([\\p{L}\\p{M}\\s-]+?)\\s+(\\d+)");


	private SovietEternalCalendarParser(){}


	/**
	 * Parses a raw Soviet string expression into a validated architectural container using exact flags.
	 *
	 * @param rawDate the string representing the Soviet Revolutionary date
	 * @return a configured SovietDateInput instance
	 * @throws IllegalArgumentException if the expression layout is invalid
	 */
	public static SovietEternalDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);

		// A year alone (e.g. the start of a decade or century) stands for the first day of the year
		if(StringUtils.isNumeric(normalized))
			return new SovietEternalDateInput(Integer.parseInt(normalized), 0, 0);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches()){
			throw new IllegalArgumentException("Invalid Soviet Eternal date format: " + rawDate);
		}

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.trim();
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Soviet month or seasonal descriptor: " + monthName);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 0);

		if(dayGroup != null && month > 0 && (day < 1 || day > 31)){
			throw new IllegalArgumentException("Day variable breaches monthly operational limits.");
		}

		return new SovietEternalDateInput(year, month, day);
	}

}
