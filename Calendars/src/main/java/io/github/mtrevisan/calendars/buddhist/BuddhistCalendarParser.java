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
package io.github.mtrevisan.calendars.buddhist;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.text.TextNormalizer;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for Buddhist calendar date expressions using exact astronomical mapping flags.
 * Supports standard month names, regional scripts, and localized seasonal markers.
 */
public final class BuddhistCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Month 1: January
		MONTHS.put("มกราคม", 1);
		MONTHS.put("ม.ค.", 1);               // Official Thai abbreviation
		MONTHS.put("mokarakhom", 1);        // Correct RTGS spelling
		MONTHS.put("makarakhom", 1);        // Common phonetic variant

		// Month 2: February
		MONTHS.put("กุมภาพันธ์", 2);
		MONTHS.put("ก.พ.", 2);               // Official Thai abbreviation
		MONTHS.put("kumphaphan", 2);        // Correct RTGS spelling

		// Month 3: March
		MONTHS.put("มีนาคม", 3);
		MONTHS.put("มี.ค.", 3);               // Official Thai abbreviation
		MONTHS.put("minakhom", 3);          // Correct RTGS spelling

		// Month 4: April
		MONTHS.put("เมษายน", 4);
		MONTHS.put("เม.ย.", 4);               // Official Thai abbreviation
		MONTHS.put("mesayon", 4);           // Correct RTGS spelling

		// Month 5: May
		MONTHS.put("พฤษภาคม", 5);
		MONTHS.put("พ.ค.", 5);               // Official Thai abbreviation
		MONTHS.put("phruetsaphakhom", 5);   // Correct RTGS spelling
		MONTHS.put("phrasaphakhom", 5);     // Common variant
		MONTHS.put("phrutsubhakhom", 5);    // Common variant

		// Month 6: June
		MONTHS.put("มิถุนายน", 6);
		MONTHS.put("มิ.ย.", 6);               // Official Thai abbreviation
		MONTHS.put("mithunayon", 6);        // Correct RTGS spelling

		// Month 7: July
		MONTHS.put("กรกฎาคม", 7);
		MONTHS.put("ก.ค.", 7);               // Official Thai abbreviation
		MONTHS.put("korakadahkom", 7);       // Correct RTGS spelling
		MONTHS.put("karakhadakhom", 7);     // Phonetic variant

		// Month 8: August
		MONTHS.put("สิงหาคม", 8);
		MONTHS.put("ส.ค.", 8);               // Official Thai abbreviation
		MONTHS.put("singhakhom", 8);        // Correct RTGS spelling

		// Month 9: September
		MONTHS.put("กันยายน", 9);
		MONTHS.put("ก.ย.", 9);               // Official Thai abbreviation
		MONTHS.put("kanyayon", 9);          // Correct RTGS spelling

		// Month 10: October
		MONTHS.put("ตุลาคม", 10);
		MONTHS.put("ต.ค.", 10);              // Official Thai abbreviation
		MONTHS.put("tulakhom", 10);         // Correct RTGS spelling

		// Month 11: November
		MONTHS.put("พฤศจิกายน", 11);
		MONTHS.put("พ.ย.", 11);              // Official Thai abbreviation
		MONTHS.put("phruetsachikayon", 11); // Correct RTGS spelling
		MONTHS.put("phrachikayon", 11);     // Colloquial short form

		// Month 12: December
		MONTHS.put("ธันวาคม", 12);
		MONTHS.put("ธ.ค.", 12);              // Official Thai abbreviation
		MONTHS.put("thanwakhom", 12);       // Correct RTGS spelling

		// Thai Seasons Alignment
		// Winter / Cold Cycle
		MONTHS.put("ฤดูหนาว", CalendarSeason.SEASON_WINTER);   // Common formal form
		MONTHS.put("หน้าหนาว", CalendarSeason.SEASON_WINTER);   // Colloquial spoken form
		MONTHS.put("เหมันต์", CalendarSeason.SEASON_WINTER);    // Literary Sanskrit form (Hemanta)
		MONTHS.put("เหมันตฤดู", CalendarSeason.SEASON_WINTER);   // Complete literary form
		MONTHS.put("hemanta", CalendarSeason.SEASON_WINTER);
		MONTHS.put("hemanta-rue-du", CalendarSeason.SEASON_WINTER);
		MONTHS.put("ru-du-nao", CalendarSeason.SEASON_WINTER);   // Correct RTGS transliteration
		MONTHS.put("ru-du-naow", CalendarSeason.SEASON_WINTER);  // Common phonetic variant
		MONTHS.put("rudunao", CalendarSeason.SEASON_WINTER);
		MONTHS.put("nha-nao", CalendarSeason.SEASON_WINTER);
		MONTHS.put("nhanao", CalendarSeason.SEASON_WINTER);

		// Summer / Hot Cycle
		MONTHS.put("ฤดูร้อน", CalendarSeason.SEASON_SUMMER);    // Common formal form
		MONTHS.put("หน้าร้อน", CalendarSeason.SEASON_SUMMER);    // Colloquial spoken form
		MONTHS.put("คิมหันต์", CalendarSeason.SEASON_SUMMER);    // Literary Sanskrit form (Kimhana)
		MONTHS.put("คิมหันตฤดู", CalendarSeason.SEASON_SUMMER);   // Complete literary form
		MONTHS.put("kimmhana", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("kimhant", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("ru-du-ron", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("ruduron", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("nha-ron", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("nharon", CalendarSeason.SEASON_SUMMER);

		// Spring / Vernal Equinox Cross-alignment
		MONTHS.put("ฤดูใบไม้ผลิ", CalendarSeason.SEASON_SPRING); // Common form ("Sprouting leaves")
		MONTHS.put("วสันต์", CalendarSeason.SEASON_SPRING);       // Literary Sanskrit form (Vasanta)
		MONTHS.put("วสันตฤดู", CalendarSeason.SEASON_SPRING);     // Complete literary form
		MONTHS.put("vasanta", CalendarSeason.SEASON_SPRING);
		MONTHS.put("wasan", CalendarSeason.SEASON_SPRING);
		MONTHS.put("wasanta", CalendarSeason.SEASON_SPRING);
		MONTHS.put("ru-du-bai-mai-phli", CalendarSeason.SEASON_SPRING);
		MONTHS.put("rudubaimaiphli", CalendarSeason.SEASON_SPRING);

		// Autumn / Autumnal Equinox Cross-alignment
		MONTHS.put("ฤดูใบไม้ร่วง", CalendarSeason.SEASON_AUTUMN); // Common form ("Falling leaves")
		MONTHS.put("สารท", CalendarSeason.SEASON_AUTUMN);        // Literary Sanskrit form (Sarada)
		MONTHS.put("ศารทฤดู", CalendarSeason.SEASON_AUTUMN);      // Complete literary form
		MONTHS.put("osen", CalendarSeason.SEASON_AUTUMN);         // Cross-point alignment variant
		MONTHS.put("sarada", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("sarat", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("ru-du-bai-mai-ruang", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("rudubaimairuang", CalendarSeason.SEASON_AUTUMN);
	}

	private static final Pattern RATTANAKOSIN_MARKER = Pattern.compile("(?i)ร\\.?\\s?ศ\\.?|(?<![\\p{L}])r\\.?\\s?s\\.?(?![\\p{L}])");
	private static final Pattern BUDDHIST_MARKER = Pattern.compile("(?i)พ\\.?\\s?ศ\\.?|(?<![\\p{L}])b\\.?\\s?e\\.?(?![\\p{L}])");

	// Robust, exception-free Unicode pattern supporting optional day prefix and localized alphabets
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?([\\p{L}\\p{M}\\s-]+?)\\s+(\\d+)");


	private BuddhistCalendarParser(){}


	/**
	 * Parses a raw Buddhist text expression into a validated structural container using exact flags.
	 *
	 * @param rawDate the string representing the calendar date (e.g., "Kimmhana 2500")
	 * @return a configured BuddhistDateInput instance
	 * @throws IllegalArgumentException if the text layout expression matches an invalid layout format
	 */
	public static BuddhistDateInput parse(final String rawDate){
		// Thai digits, and the era markers: Buddhist (พ.ศ., BE) or Rattanakosin (ร.ศ., RS)
		String text = TextNormalizer.asciiDigits(rawDate);
		final boolean rattanakosin = RATTANAKOSIN_MARKER.matcher(text).find();
		text = RATTANAKOSIN_MARKER.matcher(text)
			.replaceAll(StringUtils.SPACE);
		text = BUDDHIST_MARKER.matcher(text)
			.replaceAll(StringUtils.SPACE);
		final String normalized = StringUtils.normalizeSpace(text);

		// A year alone (e.g. the start of a decade or century) stands for the first day of the year
		if(StringUtils.isNumeric(normalized))
			return new BuddhistDateInput(Integer.parseInt(normalized), 0, 0, rattanakosin);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Buddhist date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.trim();
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Buddhist month or seasonal descriptor: " + monthName);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 0);

		if(dayGroup != null && month > 0 && (day < 1 || day > 31))
			throw new IllegalArgumentException("Day variable breaches standard monthly boundaries.");

		return new BuddhistDateInput(year, month, day, rattanakosin);
	}

}
