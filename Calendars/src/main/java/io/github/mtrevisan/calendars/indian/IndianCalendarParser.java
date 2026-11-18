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
package io.github.mtrevisan.calendars.indian;

import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;
import io.github.mtrevisan.calendars.text.ScriptText;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Parser for Indian National calendar date expressions using exact astronomical mapping flags.
 * Supports standard month names (the regional ones, Bengali, Tamil, Malayalam, have their own calendars, as their months start
 * at the sidereal sankranti instead of the equinox), and traditional Ritu markers.
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
		register("चैत्र", 1);						// Native Devanagari script
		register("chaitra", 1);				// Classical Sanskrit transliteration
		register("caitra", 1);				// Standard ISO transliteration
		
		// Month 2: Vaisakha
		register("वैशाख", 2);
		register("vaisakha", 2);
		register("vaishakha", 2);
		
		// Month 3: Jyaistha
		register("ज्येष्ठ", 3);
		register("jyaistha", 3);
		register("jyaist'ha", 3);
		register("jaishtha", 3);
		register("jeth", 3);					// Common Hindi vernacular short form

		// Month 4: Asadha
		register("आषाढ़", 4);
		register("asadha", 4);
		register("ashadha", 4);
		register("aashadh", 4);				// Phonetic long-vowel variant

		// Month 5: Shravana
		register("श्रावण", 5);
		register("shravana", 5);
		register("sravana", 5);
		register("sawan", 5);					// Vernacular Hindi/Prakrit form
	
		// Month 6: Bhadra / Bhadrapada
		register("भाद्रपद", 6);
		register("भाद्र", 6);
		register("bhadra", 6);
		register("bhadrapada", 6);
		register("bhadoo", 6);				// Vernacular short form

		// Month 7: Asvina
		register("आश्विन", 7);
		register("asvina", 7);
		register("ashwin", 7);				// Most common colloquial spelling
		register("asvin", 7);

		// Month 8: Kartika
		register("कार्तिक", 8);
		register("kartika", 8);
		register("kartik", 8);				// Standard colloquial short form

		// Month 9: Agrahayana / Margashirsha
		register("मार्गशीर्ष", 9);
		register("अग्रहायण", 9);				// Alternate traditional Sanskrit name
		register("agrahayana", 9);
		register("margashirsha", 9);
		register("margsirsa", 9);
		register("margsir", 9);				// Common short form

		// Month 10: Pausa
		register("पौष", 10);
		register("pausa", 10);
		register("pousha", 10);
		register("poush", 10);
		register("pous", 10);

		// Month 11: Magha
		register("माघ", 11);
		register("magha", 11);
		register("magh", 11);

		// Month 12: Phalguna
		register("फाल्गुन", 12);
		register("phalguna", 12);
		register("falgun", 12);				// Highly common simplified phonetic format


		// 1. Vasanta (Spring Cycle - Centered around Vernal Equinox)
		register("वसन्त", RITU_VASANTA);	// Native Devanagari script
		register("vasanta", RITU_VASANTA);
		register("basant", RITU_VASANTA);

		// 2. Grishma (Summer Cycle - Centered around Estival Solstice)
		register("ग्रीष्म", RITU_GRISHMA);		// Native Devanagari script
		register("grishma", RITU_GRISHMA);

		// 3. Varsha (Monsoon Cycle - Aligned to mid/late Summer rainy transition parameters)
		register("वर्षा", RITU_VARSHA);		// Native Devanagari script
		register("varsha", RITU_VARSHA);

		// 4. Sharad (Autumn Cycle - Centered around Autumnal Equinox)
		register("शरद", RITU_SHARAD);		// Native Devanagari script
		register("sharad", RITU_SHARAD);

		// 5. Hemanta (Pre-Winter Cycle - Late Autumn / Early Winter transition)
		register("हेमन्त", RITU_HEMANTA);	// Native Devanagari script
		register("hemanta", RITU_HEMANTA);

		// 6. Shishira (Winter Cycle - Centered around Hibernal Solstice)
		register("शिशिर", RITU_SHISHIRA);	// Native Devanagari script
		register("shishira", RITU_SHISHIRA);
		register("sisira", RITU_SHISHIRA);
	}

	private static void register(final String name, final int month){
		ScriptText.register(MONTHS, name, month);
	}


	private IndianCalendarParser(){}


	/**
	 * @return the normalized names of the months and of the Ritus known by the parser
	 */
	public static Set<String> knownNames(){
		return Collections.unmodifiableSet(MONTHS.keySet());
	}

	/**
	 * Parses a raw Indian calendar text expression into a validated structural container using exact flags.
	 *
	 * @param rawDate the string representing the calendar date (e.g., "Vasanta 1920" or "1 Chaitra 1879")
	 * @return the components
	 * @throws IllegalArgumentException if the text layout expression matches an invalid layout format, or a Ritu is given with a day
	 */
	public static YearMonthDay parse(final String rawDate){
		final ScriptText.Parsed parsed = ScriptText.parse(rawDate, "Indian", MONTHS, false);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = parsed.dayOrZero();
		if(parsed.day() != null && (day < 1 || day > 31))
			throw new IllegalArgumentException("Day variable breaches standard monthly boundaries.");

		return new YearMonthDay(parsed.year(), parsed.month(), day);
	}

}
