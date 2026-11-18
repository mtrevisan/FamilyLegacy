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
import io.github.mtrevisan.calendars.text.ScriptText;
import io.github.mtrevisan.calendars.text.TextNormalizer;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Parser for Buddhist calendar date expressions using exact astronomical mapping flags.
 * Supports standard month names, regional scripts, and localized seasonal markers.
 */
public final class BuddhistCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Month 1: January
		register("มกราคม", 1);
		register("ม.ค.", 1);               // Official Thai abbreviation
		register("mokarakhom", 1);        // Correct RTGS spelling
		register("makarakhom", 1);        // Common phonetic variant

		// Month 2: February
		register("กุมภาพันธ์", 2);
		register("ก.พ.", 2);               // Official Thai abbreviation
		register("kumphaphan", 2);        // Correct RTGS spelling

		// Month 3: March
		register("มีนาคม", 3);
		register("มี.ค.", 3);               // Official Thai abbreviation
		register("minakhom", 3);          // Correct RTGS spelling

		// Month 4: April
		register("เมษายน", 4);
		register("เม.ย.", 4);               // Official Thai abbreviation
		register("mesayon", 4);           // Correct RTGS spelling

		// Month 5: May
		register("พฤษภาคม", 5);
		register("พ.ค.", 5);               // Official Thai abbreviation
		register("phruetsaphakhom", 5);   // Correct RTGS spelling
		register("phrasaphakhom", 5);     // Common variant
		register("phrutsubhakhom", 5);    // Common variant

		// Month 6: June
		register("มิถุนายน", 6);
		register("มิ.ย.", 6);               // Official Thai abbreviation
		register("mithunayon", 6);        // Correct RTGS spelling

		// Month 7: July
		register("กรกฎาคม", 7);
		register("ก.ค.", 7);               // Official Thai abbreviation
		register("korakadahkom", 7);       // Correct RTGS spelling
		register("karakhadakhom", 7);     // Phonetic variant

		// Month 8: August
		register("สิงหาคม", 8);
		register("ส.ค.", 8);               // Official Thai abbreviation
		register("singhakhom", 8);        // Correct RTGS spelling

		// Month 9: September
		register("กันยายน", 9);
		register("ก.ย.", 9);               // Official Thai abbreviation
		register("kanyayon", 9);          // Correct RTGS spelling

		// Month 10: October
		register("ตุลาคม", 10);
		register("ต.ค.", 10);              // Official Thai abbreviation
		register("tulakhom", 10);         // Correct RTGS spelling

		// Month 11: November
		register("พฤศจิกายน", 11);
		register("พ.ย.", 11);              // Official Thai abbreviation
		register("phruetsachikayon", 11); // Correct RTGS spelling
		register("phrachikayon", 11);     // Colloquial short form

		// Month 12: December
		register("ธันวาคม", 12);
		register("ธ.ค.", 12);              // Official Thai abbreviation
		register("thanwakhom", 12);       // Correct RTGS spelling

		// Thai Seasons Alignment
		// Winter / Cold Cycle
		register("ฤดูหนาว", CalendarSeason.SEASON_WINTER);   // Common formal form
		register("หน้าหนาว", CalendarSeason.SEASON_WINTER);   // Colloquial spoken form
		register("เหมันต์", CalendarSeason.SEASON_WINTER);    // Literary Sanskrit form (Hemanta)
		register("เหมันตฤดู", CalendarSeason.SEASON_WINTER);   // Complete literary form
		register("hemanta", CalendarSeason.SEASON_WINTER);
		register("hemanta-rue-du", CalendarSeason.SEASON_WINTER);
		register("ru-du-nao", CalendarSeason.SEASON_WINTER);   // Correct RTGS transliteration
		register("ru-du-naow", CalendarSeason.SEASON_WINTER);  // Common phonetic variant
		register("rudunao", CalendarSeason.SEASON_WINTER);
		register("nha-nao", CalendarSeason.SEASON_WINTER);
		register("nhanao", CalendarSeason.SEASON_WINTER);

		// Summer / Hot Cycle
		register("ฤดูร้อน", CalendarSeason.SEASON_SUMMER);    // Common formal form
		register("หน้าร้อน", CalendarSeason.SEASON_SUMMER);    // Colloquial spoken form
		register("คิมหันต์", CalendarSeason.SEASON_SUMMER);    // Literary Sanskrit form (Kimhana)
		register("คิมหันตฤดู", CalendarSeason.SEASON_SUMMER);   // Complete literary form
		register("kimmhana", CalendarSeason.SEASON_SUMMER);
		register("kimhant", CalendarSeason.SEASON_SUMMER);
		register("ru-du-ron", CalendarSeason.SEASON_SUMMER);
		register("ruduron", CalendarSeason.SEASON_SUMMER);
		register("nha-ron", CalendarSeason.SEASON_SUMMER);
		register("nharon", CalendarSeason.SEASON_SUMMER);

		// Spring / Vernal Equinox Cross-alignment
		register("ฤดูใบไม้ผลิ", CalendarSeason.SEASON_SPRING); // Common form ("Sprouting leaves")
		register("วสันต์", CalendarSeason.SEASON_SPRING);       // Literary Sanskrit form (Vasanta)
		register("วสันตฤดู", CalendarSeason.SEASON_SPRING);     // Complete literary form
		register("vasanta", CalendarSeason.SEASON_SPRING);
		register("wasan", CalendarSeason.SEASON_SPRING);
		register("wasanta", CalendarSeason.SEASON_SPRING);
		register("ru-du-bai-mai-phli", CalendarSeason.SEASON_SPRING);
		register("rudubaimaiphli", CalendarSeason.SEASON_SPRING);

		// Autumn / Autumnal Equinox Cross-alignment
		register("ฤดูใบไม้ร่วง", CalendarSeason.SEASON_AUTUMN); // Common form ("Falling leaves")
		register("สารท", CalendarSeason.SEASON_AUTUMN);        // Literary Sanskrit form (Sarada)
		register("ศารทฤดู", CalendarSeason.SEASON_AUTUMN);      // Complete literary form
		register("osen", CalendarSeason.SEASON_AUTUMN);         // Cross-point alignment variant
		register("sarada", CalendarSeason.SEASON_AUTUMN);
		register("sarat", CalendarSeason.SEASON_AUTUMN);
		register("ru-du-bai-mai-ruang", CalendarSeason.SEASON_AUTUMN);
		register("rudubaimairuang", CalendarSeason.SEASON_AUTUMN);
	}

	private static final Pattern RATTANAKOSIN_MARKER = Pattern.compile("(?i)ร\\.?\\s?ศ\\.?|(?<![\\p{L}])r\\.?\\s?s\\.?(?![\\p{L}])");
	private static final Pattern BUDDHIST_MARKER = Pattern.compile("(?i)พ\\.?\\s?ศ\\.?|(?<![\\p{L}])b\\.?\\s?e\\.?(?![\\p{L}])");

	private static void register(final String name, final int month){
		ScriptText.register(MONTHS, name, month);
	}


	private BuddhistCalendarParser(){}


	/**
	 * @return the normalized names of the months and of the seasons known by the parser
	 */
	public static Set<String> knownNames(){
		return Collections.unmodifiableSet(MONTHS.keySet());
	}

	/**
	 * Parses a raw Buddhist text expression into a validated structural container using exact flags.
	 *
	 * @param rawDate the string representing the calendar date (e.g., "Kimmhana 2500")
	 * @return a configured BuddhistDateInput instance
	 * @throws IllegalArgumentException if the text layout expression matches an invalid layout format, or a season is given with a day
	 */
	public static BuddhistDateInput parse(final String rawDate){
		// Thai digits, and the era markers: Buddhist (พ.ศ., BE) or Rattanakosin (ร.ศ., RS)
		String text = TextNormalizer.asciiDigits(rawDate);
		final boolean rattanakosin = RATTANAKOSIN_MARKER.matcher(text).find();
		text = RATTANAKOSIN_MARKER.matcher(text)
			.replaceAll(StringUtils.SPACE);
		text = BUDDHIST_MARKER.matcher(text)
			.replaceAll(StringUtils.SPACE);
		final ScriptText.Parsed parsed = ScriptText.parseNormalized(ScriptText.normalize(text), rawDate, "Buddhist", MONTHS, false);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = parsed.dayOrZero();
		if(parsed.day() != null && (day < 1 || day > 31))
			throw new IllegalArgumentException("Day variable breaches standard monthly boundaries.");

		return new BuddhistDateInput(parsed.year(), parsed.month(), day, rattanakosin);
	}

}
