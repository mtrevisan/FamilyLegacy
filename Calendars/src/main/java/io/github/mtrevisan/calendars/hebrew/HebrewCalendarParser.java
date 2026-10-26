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
package io.github.mtrevisan.calendars.hebrew;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for Hebrew calendar date expressions using exact astronomical mapping flags.
 * Supports standard month names, leap variants, and traditional Tekufah seasonal markers.
 */
public final class HebrewCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Month 1: Tishrei (Autumn Equinox Month)
		MONTHS.put("תשרי", 1);
		MONTHS.put("tishrei", 1);
		MONTHS.put("tishri", 1);
		MONTHS.put("tischri", 1);		// German-Jewish phonetic variant

		// Month 2: Cheshvan / Marcheshvan
		MONTHS.put("חשון", 2);
		MONTHS.put("מרחשון", 2);		// Traditional formal full spelling
		MONTHS.put("cheshvan", 2);
		MONTHS.put("heshvan", 2);
		MONTHS.put("marcheshvan", 2);
		MONTHS.put("marheshvan", 2);

		// Month 3: Kislev
		MONTHS.put("כסלו", 3);
		MONTHS.put("kislev", 3);
		MONTHS.put("chizlev", 3);		// Archaic phonetic variant
		MONTHS.put("chislew", 3);		// Historical German-Jewish spelling

		// Month 4: Tevet (Winter Solstice Month)
		MONTHS.put("טבת", 4);
		MONTHS.put("tevet", 4);
		MONTHS.put("tebeth", 4);

		// Month 5: Shevat
		MONTHS.put("שבט", 5);
		MONTHS.put("shevat", 5);
		MONTHS.put("shebat", 5);
		MONTHS.put("schewat", 5);

		// Month 6: Adar (Standard/Non-Leap Year) or Adar I (Intercalary Leap Month)
		MONTHS.put("אדר", 6);
		MONTHS.put("אדר א", 6);			// Native script for Adar Aleph
		MONTHS.put("אדר ראשון", 6);	// Full native string ("First Adar")
		MONTHS.put("adar", 6);
		MONTHS.put("adar i", 6);
		MONTHS.put("adar aleph", 6);
		MONTHS.put("adar rishon", 6);

		// Month 7: Adar II (Only exists in 13-month Leap Years)
		MONTHS.put("אדר ב", 7);			// Native script for Adar Bet
		MONTHS.put("אדר שני", 7);		// Full native string ("Second Adar")
		MONTHS.put("adar ii", 7);
		MONTHS.put("adar bet", 7);
		MONTHS.put("adar sheni", 7);
		MONTHS.put("ve'adar", 7);		// Classical prefix notation ("And Adar")
		MONTHS.put("veadar", 7);

		// Month 8: Nisan (Spring Equinox Month / Ecclesiastical Month 1)
		MONTHS.put("ניסן", 8);
		MONTHS.put("nisan", 8);
		MONTHS.put("nissan", 8);

		// Month 9: Iyar
		MONTHS.put("אייר", 9);
		MONTHS.put("iyar", 9);
		MONTHS.put("ijar", 9);
		MONTHS.put("iyyar", 9);

		// Month 10: Sivan
		MONTHS.put("סיון", 10);
		MONTHS.put("sivan", 10);
		MONTHS.put("siwan", 10);

		// Month 11: Tammuz (Summer Solstice Month)
		MONTHS.put("תמוז", 11);
		MONTHS.put("tammuz", 11);
		MONTHS.put("tamuz", 11);

		// Month 12: Av
		MONTHS.put("אב", 12);
		MONTHS.put("av", 12);
		MONTHS.put("ab", 12);
		MONTHS.put("menachem av", 12);	// Comforting traditional title suffix
		MONTHS.put("menachem-ab", 12);

		// Month 13: Elul
		MONTHS.put("אלול", 13);
		MONTHS.put("elul", 13);


		// =============================================================================
		// HEBREW SEASONS & TEKUFOT (Astronomical Halakhic Turnings)
		// =============================================================================

		// Autumn / Autumnal Equinox (Tekufat Tishrei)
		MONTHS.put("תקופת תשרי", CalendarSeason.SEASON_AUTUMN);		// Native script
		MONTHS.put("tekufat tishrei", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("tekufat tishri", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("stav", CalendarSeason.SEASON_AUTUMN);				// Modern Hebrew word for Autumn (סתיו)
		MONTHS.put("סתיו", CalendarSeason.SEASON_AUTUMN);

		// Winter / Hibernal Solstice (Tekufat Tevet)
		MONTHS.put("תקופת טבת", CalendarSeason.SEASON_WINTER);		// Native script
		MONTHS.put("tekufat tevet", CalendarSeason.SEASON_WINTER);
		MONTHS.put("tekufat tebeth", CalendarSeason.SEASON_WINTER);
		MONTHS.put("horef", CalendarSeason.SEASON_WINTER);			// Modern Hebrew word for Winter (חורף)
		MONTHS.put("חורף", CalendarSeason.SEASON_WINTER);

		// Spring / Vernal Equinox (Tekufat Nisan)
		MONTHS.put("תקופת ניסן", CalendarSeason.SEASON_SPRING);		// Native script
		MONTHS.put("tekufat nisan", CalendarSeason.SEASON_SPRING);
		MONTHS.put("tekufat nissan", CalendarSeason.SEASON_SPRING);
		MONTHS.put("aviv", CalendarSeason.SEASON_SPRING);				// Modern Hebrew word for Spring (אביב)
		MONTHS.put("אביב", CalendarSeason.SEASON_SPRING);

		// Summer / Estival Solstice (Tekufat Tammuz)
		MONTHS.put("תקופת תמוז", CalendarSeason.SEASON_SUMMER);		// Native script
		MONTHS.put("tekufat tammuz", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("tekufat tamuz", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("kayitz", CalendarSeason.SEASON_SUMMER);			// Modern Hebrew word for Summer (קיץ)
		MONTHS.put("קיץ", CalendarSeason.SEASON_SUMMER);
	}

	// Robust, exception-free Unicode pattern supporting optional day prefix and localized alphabets
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?([\\p{L}\\p{M}\\s-]+?)\\s+(\\d+)");


	private HebrewCalendarParser(){}


	/**
	 * Parses a raw Hebrew text expression into a validated structural container using exact flags.
	 *
	 * @param rawDate the string representing the calendar date (e.g., "Tekufat Nisan 5786")
	 * @return a configured HebrewDateInput instance
	 * @throws IllegalArgumentException if the text layout expression matches an invalid layout format
	 */
	public static HebrewDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);

		// A year alone (e.g. the start of a decade or century) stands for the first day of the year
		if(StringUtils.isNumeric(normalized))
			return new HebrewDateInput(Integer.parseInt(normalized), 0, 0);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Hebrew date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.trim();
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Hebrew month or Tekufah descriptor: " + monthName);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 0);

		if(dayGroup != null && month > 0 && (day < 1 || day > 30))
			throw new IllegalArgumentException("Day variable breaches standard monthly boundaries.");

		return new HebrewDateInput(year, month, day);
	}

}
