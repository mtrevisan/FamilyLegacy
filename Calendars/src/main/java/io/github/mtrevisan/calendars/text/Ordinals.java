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
package io.github.mtrevisan.calendars.text;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser of the (normalized, see {@link TextNormalizer}) cardinal and ordinal numbers of the historical documents:
 * Arabic numerals with or without an ordinal suffix ("3", "3rd", "3a", "3o"), Roman numerals ("iii", "xiv"), and the
 * Italian, Latin, and English ordinal words ("terza", "tertio", "third", "decima quarta", "twenty-first").
 */
public final class Ordinals{

	private static final Pattern ARABIC = Pattern.compile("(\\d{1,4})(?:st|nd|rd|th|a|o|e|i|er|re|eme|e)?");
	/**
	 * Roman numerals up to 89 (regnal years, indictions, ordinals); "c", "d", and "m" are left out, so that common words
	 * like "di" or "mi" are not taken for numbers.
	 */
	private static final Pattern ROMAN = Pattern.compile("(?:xc|xl|l?x{0,3})(?:ix|iv|v?i{0,3})");

	/** Ordinal words (and the stems of the Latin ones), by value. */
	private static final Map<String, Integer> WORDS = new HashMap<>();
	/** Latin ordinal stems: the inflected ending is removed before the lookup. */
	private static final Map<String, Integer> LATIN_STEMS = new HashMap<>();
	private static final Pattern LATIN_ENDING = Pattern.compile("(?:us|um|a|o|i|ae|am|is|os|as|e)$");

	static{
		final String[] italian = {"primo", "secondo", "terzo", "quarto", "quinto", "sesto", "settimo", "ottavo", "nono",
			"decimo", "undicesimo", "dodicesimo", "tredicesimo", "quattordicesimo", "quindicesimo", "sedicesimo",
			"diciassettesimo", "diciottesimo", "diciannovesimo", "ventesimo", "ventunesimo", "ventiduesimo", "ventitreesimo",
			"ventiquattresimo", "venticinquesimo", "ventiseiesimo", "ventisettesimo", "ventottesimo", "ventinovesimo",
			"trentesimo", "trentunesimo"};
		for(int i = 0; i < italian.length; i ++){
			final String masculine = italian[i];
			WORDS.put(masculine, i + 1);
			WORDS.put(masculine.substring(0, masculine.length() - 1) + "a", i + 1);
		}

		final String[] english = {"first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth",
			"tenth", "eleventh", "twelfth", "thirteenth", "fourteenth", "fifteenth", "sixteenth", "seventeenth",
			"eighteenth", "nineteenth", "twentieth"};
		for(int i = 0; i < english.length; i ++)
			WORDS.put(english[i], i + 1);
		WORDS.put("thirtieth", 30);
		for(int i = 0; i < 9; i ++){
			WORDS.put("twenty " + english[i], 21 + i);
			WORDS.put("twenty" + english[i], 21 + i);
		}
		WORDS.put("thirty first", 31);
		WORDS.put("thirtyfirst", 31);

		// cardinal numbers (e.g. "tre giorni dopo", "three days before", "tribus diebus post")
		final String[][] cardinals = {
			{"uno", "un", "una", "one", "unus", "unum", "une"},
			{"due", "two", "duo", "duobus", "deux"},
			{"tre", "three", "tres", "tribus", "trois"},
			{"quattro", "four", "quattuor", "quatuor", "quatre"},
			{"cinque", "five", "quinque", "cinq"},
			{"sei", "six", "sex"},
			{"sette", "seven", "septem", "sept"},
			{"otto", "eight", "octo", "huit"},
			{"nove", "nine", "novem", "neuf"},
			{"dieci", "ten", "decem", "dix"}
		};
		for(int i = 0; i < cardinals.length; i ++)
			for(final String cardinal : cardinals[i])
				WORDS.put(cardinal, i + 1);

		final String[] latin = {"prim", "secund", "terti", "quart", "quint", "sext", "septim", "octav", "non", "decim",
			"undecim", "duodecim"};
		for(int i = 0; i < latin.length; i ++)
			LATIN_STEMS.put(latin[i], i + 1);
		LATIN_STEMS.put("duodevicesim", 18);
		LATIN_STEMS.put("undevicesim", 19);
		LATIN_STEMS.put("vicesim", 20);
		LATIN_STEMS.put("vigesim", 20);
		LATIN_STEMS.put("duodetricesim", 28);
		LATIN_STEMS.put("undetricesim", 29);
		LATIN_STEMS.put("tricesim", 30);
		LATIN_STEMS.put("trigesim", 30);
	}


	private Ordinals(){}


	/**
	 * Parses a single (normalized) word as a number.
	 *
	 * @param word the word
	 * @return the value, or {@code -1} if the word is not a number
	 */
	public static int parse(final String word){
		if(StringUtils.isBlank(word))
			return -1;

		final Matcher arabic = ARABIC.matcher(word);
		if(arabic.matches())
			return Integer.parseInt(arabic.group(1));

		final Integer value = WORDS.get(word);
		if(value != null)
			return value;

		final int latin = parseLatin(word);
		if(latin > 0)
			return latin;

		return parseRoman(word);
	}

	/**
	 * Parses a number written with one or two words (e.g. "decima quarta", "quarta decima", "twenty first"),
	 * starting at the given position.
	 *
	 * @param words the words
	 * @param index the position of the first word
	 * @return the value and the number of words used, or {@code null} if there is no number at the position
	 */
	public static int[] parse(final String[] words, final int index){
		if(index >= words.length)
			return null;

		if(index + 1 < words.length){
			final Integer compound = WORDS.get(words[index] + " " + words[index + 1]);
			if(compound != null)
				return new int[]{compound, 2};

			// Latin compound ordinals: "decimus quartus", "quartus decimus", "vicesimus primus"
			final int first = parseLatin(words[index]);
			final int second = parseLatin(words[index + 1]);
			if(first > 0 && second > 0 && (first >= 10 && second < 10 || first < 10 && second == 10))
				return new int[]{first + second, 2};
		}

		final int value = parse(words[index]);
		return (value > 0? new int[]{value, 1}: null);
	}

	/**
	 * Parses a Roman numeral.
	 *
	 * @param word the (lowercase) numeral
	 * @return the value, or {@code -1} if the word is not a Roman numeral
	 */
	public static int parseRoman(final String word){
		// a lone "l" is the elided article (e.g. "l'ottava"), not 50
		if(StringUtils.isBlank(word) || word.equals("l") || !ROMAN.matcher(word).matches())
			return -1;

		int total = 0;
		int previous = 0;
		for(int i = word.length() - 1; i >= 0; i --){
			final int value = switch(word.charAt(i)){
				case 'i' -> 1;
				case 'v' -> 5;
				case 'x' -> 10;
				case 'l' -> 50;
				default -> 0;
			};
			total += (value < previous? -value: value);
			previous = Math.max(previous, value);
		}
		return total;
	}

	private static int parseLatin(final String word){
		final Integer exact = LATIN_STEMS.get(word);
		if(exact != null)
			return exact;

		final Matcher ending = LATIN_ENDING.matcher(word);
		if(!ending.find())
			return -1;

		final Integer value = LATIN_STEMS.get(word.substring(0, ending.start()));
		return (value != null? value: -1);
	}

}
