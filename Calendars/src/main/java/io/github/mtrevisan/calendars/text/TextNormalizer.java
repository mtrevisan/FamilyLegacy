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

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;


/**
 * Normalization of the free text of the historical date expressions written in Latin-script languages.
 */
public final class TextNormalizer{

	private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
	/** Apostrophes, punctuation, and the ordinal indicators (e.g. "1ª", "3°"). */
	private static final Pattern PUNCTUATION = Pattern.compile("[\\p{Punct}’‘“”«»ºª°·–—ʻʼʿʾ]");


	private TextNormalizer(){}


	/**
	 * Lowercases the text, removes the diacritics, and replaces punctuation with spaces.
	 *
	 * @param text the text
	 * @return the normalized text, with single spaces between the words
	 */
	public static String normalize(final String text){
		if(text == null)
			return StringUtils.EMPTY;

		String normalized = Normalizer.normalize(asciiDigits(text).toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
		normalized = COMBINING_MARKS.matcher(normalized).replaceAll(StringUtils.EMPTY);
		// the ligatures of the Latin texts
		normalized = normalized
			.replace("æ", "ae")
			.replace("œ", "oe")
			.replace("ſ", "s")
			.replace("ß", "ss");
		normalized = PUNCTUATION.matcher(normalized).replaceAll(StringUtils.SPACE);
		return StringUtils.normalizeSpace(normalized);
	}

	/**
	 * Replaces the decimal digits of every script (e.g. Devanagari "२०८०", Bengali "১৪৩১") with the ASCII ones.
	 *
	 * @param text the text
	 * @return the text with ASCII digits
	 */
	public static String asciiDigits(final String text){
		final StringBuilder sb = new StringBuilder(text.length());
		for(int i = 0; i < text.length(); i ++){
			final char c = text.charAt(i);
			sb.append(c > 0x7F && Character.isDigit(c)? (char)('0' + Character.digit(c, 10)): c);
		}
		return sb.toString();
	}

	/**
	 * Normalizes and splits the text into words.
	 *
	 * @param text the text
	 * @return the words
	 */
	public static String[] tokenize(final String text){
		return StringUtils.split(normalize(text));
	}

}
