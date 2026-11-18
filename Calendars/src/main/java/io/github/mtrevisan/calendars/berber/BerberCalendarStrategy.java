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
package io.github.mtrevisan.calendars.berber;

import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.gregorian.JulianCalendarStrategy;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;
import io.github.mtrevisan.calendars.text.Ordinals;
import io.github.mtrevisan.calendars.text.TextNormalizer;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;


/**
 * Amazigh (Berber) agricultural calendar: the Julian calendar, with the years counted from 950 BC (the accession of
 * Shoshenq I, a convention introduced by the Académie berbère in the 1960s-1980s), so that 1 Yennayer 2976 is the Julian
 * 1 January 2026 (Gregorian 14 January 2026), e.g. "9 Tubeṛ 2976" (22 October 2026).
 * <p>
 * Month names are accepted in Kabyle, Tachelhit (Shilha), Riffian, Ayt Hdidou, and in the neologisms of the Académie
 * berbère. The fixed civil holidays (12 January in Algeria, 14 January in Morocco) are Gregorian conventions and are not
 * modeled here. Sources: en/fr.wikipedia "Berber calendar"/"Calendrier berbère", "Yennayer".
 */
public class BerberCalendarStrategy implements CalendarConverterPlugin{

	/** Difference between the Amazigh and the Julian year numbers. */
	private static final int ERA_OFFSET = 950;

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		registerMonth(1, "yennayer", "ennayer", "nnayer", "innayr", "innayer", "yannayer", "yenyur");
		registerMonth(2, "furar", "fuṛar", "brayr", "xubrayer", "kubrayer", "fubrayer", "yebrayer", "fabrayer", "sinyur");
		registerMonth(3, "meɣres", "meghres", "maghres", "marṣ", "mars", "mares", "marsu", "krayur");
		registerMonth(4, "yebrir", "brir", "ibrir", "ibril", "yabril", "kuẓyur");
		registerMonth(5, "mayyu", "maggu", "mayyuh", "mayu", "may", "semyur");
		registerMonth(6, "yunyu", "yunyuh", "bulɛanṣart", "seḍyur");
		registerMonth(7, "yulyuz", "yulyu", "izɣ", "sayur");
		registerMonth(8, "ɣuct", "ghuct", "ghusht", "ucht", "tamyur");
		registerMonth(9, "ctembeṛ", "shtember", "ctember", "cutambir", "cutanbir", "cutembir", "shutembir", "satambir",
			"tẓayur");
		registerMonth(10, "tubeṛ", "ktubeṛ", "kṭubr", "kṭuber", "ctuber", "ktuber", "ktobir", "mrayur");
		registerMonth(11, "wambeṛ", "nunembeṛ", "nuwambir", "duwanbir", "atlaɣ", "nwambir", "nvambir", "yamrayur");
		registerMonth(12, "dujembeṛ", "bujembeṛ", "duǧembeṛ", "buǧembeṛ", "dujambir", "dujanbir", "dujmbir", "dujembir",
			"meggyur");
	}

	private static void registerMonth(final int month, final String... names){
		for(final String name : names)
			MONTHS.put(normalize(name), month);
	}

	private static String normalize(final String text){
		return TextNormalizer.normalize(text)
			.replace("ɣ", "gh")
			.replace("ɛ", "e");
	}


	@Override
	public long parseToJdn(final String rawDate){
		final int[] date = parse(rawDate);
		final int julianYear = date[0] - ERA_OFFSET;
		final int month = (date[1] != 0? date[1]: 1);
		final int day = (date[2] != 0? date[2]: 1);

		final int monthLength = GregorianCalendarStrategy.monthLength(month, JulianCalendarStrategy.isLeapYear(julianYear));
		if(day < 1 || day > monthLength)
			throw new IllegalArgumentException("Day " + day + " does not exist in month " + month + ": " + rawDate);

		return JulianCalendarStrategy.toJdn(julianYear, month, day);
	}

	/**
	 * Parses "year", "month year", or "day month year", where the month is a name or a number from 1 to 12 and the day
	 * can have an ordinal suffix ("5th", "1st"). With two words the first one is always the month (so "5 2976" is the
	 * month 5, Mayyu, of 2976, and a day alone is never accepted).
	 *
	 * @return the year, the month (0 if missing), and the day (0 if missing)
	 */
	private static int[] parse(final String rawDate){
		final String[] words = StringUtils.split(normalize(rawDate));
		if(words == null || words.length == 0 || words.length > 3)
			throw new IllegalArgumentException("Invalid Amazigh date: " + rawDate);

		final String yearWord = words[words.length - 1];
		if(!StringUtils.isNumeric(yearWord))
			throw new IllegalArgumentException("Missing year in the Amazigh date: " + rawDate);
		if(yearWord.length() > MAX_YEAR_DIGITS)
			throw new IllegalArgumentException("Year too large in the Amazigh date: " + rawDate);
		final int year = Integer.parseInt(yearWord);

		int month = 0;
		int day = 0;
		if(words.length >= 2){
			final String monthWord = words[words.length - 2];
			final Integer named = MONTHS.get(monthWord);
			if(named != null)
				month = named;
			else{
				final int number = (StringUtils.isNumeric(monthWord) && monthWord.length() <= 2? Integer.parseInt(monthWord): 0);
				if(number < 1 || number > 12)
					throw new IllegalArgumentException("Unknown Amazigh month '" + monthWord + "': " + rawDate);
				month = number;
			}
		}
		if(words.length == 3){
			day = Ordinals.parse(words[0]);
			if(day < 1)
				throw new IllegalArgumentException("Invalid day in the Amazigh date: " + rawDate);
		}
		return new int[]{year, month, day};
	}

	/** Maximum number of digits of a year. */
	private static final int MAX_YEAR_DIGITS = 6;

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final int[] date = parse(rawDate);
		return CalendarMetadata.createFromMonthDay(date[1], date[2]);
	}

}
