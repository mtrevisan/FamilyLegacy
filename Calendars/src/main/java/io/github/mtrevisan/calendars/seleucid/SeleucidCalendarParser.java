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
package io.github.mtrevisan.calendars.seleucid;

import io.github.mtrevisan.calendars.CalendarType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for Seleucid (Syro-Macedonian) calendar date expressions.
 * Supports Macedonian names, Syriac equivalents, and standard Latin translations.
 */
public final class SeleucidCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Month 1: Hyperberetaios / Tishrin Qadim (October)
		MONTHS.put("ὑπερβερεταῖος", 1);    // Native Macedonian Greek script
		MONTHS.put("ܬܫܪܝܢ ܩܕܝܡ", 1);        // Native Syriac script
		MONTHS.put("hyperberetaios", 1);
		MONTHS.put("hyperberetaos", 1);     // Common variant spelling
		MONTHS.put("tishrin qdim", 1);
		MONTHS.put("tishrin al-awwal", 1);  // Arabic-Syriac crossover variant
		MONTHS.put("tishri", 1);            // Jewish/Babylonian autumn lunar epoch alignment

		// Month 2: Dios / Tishrin Hray (November)
		MONTHS.put("δῖος", 2);
		MONTHS.put("ܬܫܪܝܢ ܐܚܪܝ", 2);
		MONTHS.put("dios", 2);
		MONTHS.put("dius", 2);              // Latinized epigraphic variant
		MONTHS.put("tishrin hray", 2);
		MONTHS.put("tishrin al-thani", 2);
		MONTHS.put("marcheshvan", 2);

		// Month 3: Apellaios / Kanun Qadim (December)
		MONTHS.put("ἀπελλαῖος", 3);
		MONTHS.put("ܟܢܘܢ ܩܕܝܡ", 3);
		MONTHS.put("apellaios", 3);
		MONTHS.put("apelleos", 3);
		MONTHS.put("kanun qdim", 3);
		MONTHS.put("kanun al-awwal", 3);
		MONTHS.put("kislev", 3);

		// Month 4: Audynaios / Kanun Hray (January)
		MONTHS.put("αὐδυναῖος", 4);
		MONTHS.put("ܟܢܘܢ ܐܚܪܝ", 4);
		MONTHS.put("audynaios", 4);
		MONTHS.put("audeneos", 4);
		MONTHS.put("audnaios", 4);
		MONTHS.put("kanun hray", 4);
		MONTHS.put("kanun al-thani", 4);
		MONTHS.put("tevet", 4);

		// Month 5: Peritios / Shbat (February)
		MONTHS.put("περίτιος", 5);
		MONTHS.put("ܫܒܛ", 5);
		MONTHS.put("peritios", 5);
		MONTHS.put("periteos", 5);
		MONTHS.put("shbat", 5);
		MONTHS.put("shebat", 5);
		MONTHS.put("shubat", 5);
		MONTHS.put("shevat", 5);

		// Month 6: Dystros / Adar (March)
		MONTHS.put("δύστρος", 6);
		MONTHS.put("ܐܕܪ", 6);
		MONTHS.put("dystros", 6);
		MONTHS.put("dystrus", 6);
		MONTHS.put("adar", 6);

		// Month 7: Xanthikos / Nisan (April) - The Spring Lunar Epoch Anchor
		MONTHS.put("ξανθικός", 7);
		MONTHS.put("ܢܝܣܢ", 7);
		MONTHS.put("xanthikos", 7);
		MONTHS.put("xanticus", 7);          // Latinized historical variant
		MONTHS.put("nisan", 7);
		MONTHS.put("nissan", 7);

		// Month 8: Artemisios / Iyar (May)
		MONTHS.put("ἀρτεμίσιος", 8);
		MONTHS.put("ܐܝܪ", 8);
		MONTHS.put("artemisios", 8);
		MONTHS.put("artemiseos", 8);
		MONTHS.put("iyar", 8);
		MONTHS.put("ijar", 8);

		// Month 9: Daisios / Haziran (June)
		MONTHS.put("δαίσιος", 9);
		MONTHS.put("ܚܙܝܪܢ", 9);
		MONTHS.put("daisios", 9);
		MONTHS.put("daiseos", 9);
		MONTHS.put("haziran", 9);

		// Month 10: Panemos / Tamuz (July)
		MONTHS.put("πάνεμος", 10);
		MONTHS.put("ܬܡܘܙ", 10);
		MONTHS.put("panemos", 10);
		MONTHS.put("panamusi", 10);         // Babylon-Semitic transcription variant
		MONTHS.put("tamuz", 10);
		MONTHS.put("tammuz", 10);

		// Month 11: Loios / Ab (August)
		MONTHS.put("λώιος", 11);
		MONTHS.put("ܐܒ", 11);
		MONTHS.put("loios", 11);
		MONTHS.put("loius", 11);
		MONTHS.put("ab", 11);
		MONTHS.put("av", 11);

		// Month 12: Gorpiaios / Aylul (September)
		MONTHS.put("γορπιαῖος", 12);
		MONTHS.put("ܐܝܠܘܠ", 12);
		MONTHS.put("gorpiaios", 12);
		MONTHS.put("gorpiose", 12);         // Kept original phonetic variant for compatibility
		MONTHS.put("aylul", 12);
		MONTHS.put("elul", 12);


		// Autumn
		MONTHS.put("hyperberetaios-season", CalendarType.SEASON_AUTUMN);
		MONTHS.put("tishrin-season", CalendarType.SEASON_AUTUMN);

		// Winter
		MONTHS.put("audynaios-season", CalendarType.SEASON_WINTER);
		MONTHS.put("kanun-season", CalendarType.SEASON_WINTER);

		// Spring
		MONTHS.put("xanthikos-season", CalendarType.SEASON_SPRING);
		MONTHS.put("nisan-season", CalendarType.SEASON_SPRING);

		// Summer
		MONTHS.put("panemos-season", CalendarType.SEASON_SUMMER);
		MONTHS.put("tammuz-season", CalendarType.SEASON_SUMMER);
	}

	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?([\\p{L}\\p{M}\\s-]+?)\\s+(\\d+)");


	private SeleucidCalendarParser(){}


	/**
	 * Parses a raw Seleucid text expression into a validated structural container.
	 *
	 * @param rawDate the date string (e.g., "15 Nisan 315" or "1 October 500")
	 * @return a configured SeleucidDateInput instance
	 * @throws IllegalArgumentException if the layout or parameters breach structural constraints
	 */
	public static SeleucidDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);

		// A year alone (e.g. the start of a decade or century) stands for the first day of the year
		if(StringUtils.isNumeric(normalized))
			return new SeleucidDateInput(Integer.parseInt(normalized), 0, 0);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Seleucid date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.trim();
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Seleucid month or seasonal descriptor: " + monthName);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 0);

		// Validation mapping onto Julian month limits
		if(dayGroup != null && month > 0 && (day < 1 || day > 31))
			throw new IllegalArgumentException("Invalid day count for the targeted Julian boundary.");

		return new SeleucidDateInput(year, month, day);
	}

}
