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
package io.github.mtrevisan.calendars.gregorian;

import io.github.mtrevisan.calendars.text.Ordinals;
import io.github.mtrevisan.calendars.text.TextNormalizer;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The indiction, the position of the year in a 15-year cycle, written next to the year in the notarial, papal, and
 * imperial documents (e.g. "anno Domini 1450, indictione tertia decima").
 * <p>
 * The indiction does not identify a date by itself: it is read and checked against the year, as an error check of the
 * transcription. The year 1 AD (counted from 1 January) has the indiction 4. The indiction changed on different days
 * depending on the style:
 * <ul>
 *   <li>Greek (Byzantine, used also in Venice and southern Italy): on 1 September;</li>
 *   <li>Bedan (Constantinian, imperial): on 24 September;</li>
 *   <li>Roman (pontifical): on 25 December or 1 January.</li>
 * </ul>
 * Since the style is not written in the document, a date from 1 September to 31 December is consistent with both the
 * indiction of its year and the following one.
 */
public final class Indiction{

	/** The keyword, followed by the number (one or two words, e.g. "XV", "3", "tertia", "tertia decima"). */
	private static final Pattern INDICTION_PATTERN = Pattern.compile(
		"(?iu)[,;]?\\s*\\b(?:indictione|indictionis|indictio|indizione|indiction|indict|ind)\\b\\.?\\s*([\\p{L}\\d]+)(?:\\s+([\\p{L}]+))?");

	private static final int CYCLE = 15;
	/** First month of the period that can belong to the following indiction (September). */
	private static final int FIRST_MONTH_OF_NEXT_INDICTION = 9;


	/**
	 * A date expression without its indiction.
	 *
	 * @param text      the date expression, with the indiction removed
	 * @param indiction the indiction (1 to 15), or 0 if missing
	 */
	public record Extraction(String text, int indiction){}


	private Indiction(){}


	/**
	 * Removes the indiction from a date expression.
	 *
	 * @param rawDate the date expression
	 * @return the expression without the indiction, and the indiction
	 * @throws IllegalArgumentException if the indiction is not a number from 1 to 15
	 */
	public static Extraction extract(final String rawDate){
		if(rawDate == null)
			return new Extraction(null, 0);

		final Matcher matcher = INDICTION_PATTERN.matcher(rawDate);
		if(!matcher.find())
			return new Extraction(rawDate, 0);

		final String first = TextNormalizer.normalize(matcher.group(1));
		final String second = (matcher.group(2) != null? TextNormalizer.normalize(matcher.group(2)): null);
		final String[] words = (second != null? new String[]{first, second}: new String[]{first});
		final int[] number = Ordinals.parse(words, 0);
		if(number == null || number[0] < 1 || number[0] > CYCLE)
			throw new IllegalArgumentException("Invalid indiction in: " + rawDate);

		// the second word belongs to the indiction only if it is part of the number
		final int end = (number[1] == 2? matcher.end(): matcher.end(1));
		final String text = rawDate.substring(0, matcher.start()) + StringUtils.SPACE + rawDate.substring(end);
		return new Extraction(StringUtils.normalizeSpace(text), number[0]);
	}

	/**
	 * Computes the indiction of a year, counted from 1 January.
	 *
	 * @param year the astronomical year (counted from 1 January)
	 * @return the indiction (1 to 15)
	 */
	public static int of(final int year){
		return Math.floorMod(year + 2, CYCLE) + 1;
	}

	/**
	 * Checks the indiction of a parsed date against its year.
	 *
	 * @param input      the parsed date
	 * @param commonYear the year of the date, counted from 1 January
	 * @throws IllegalArgumentException if the indiction is inconsistent with the date in every style
	 */
	public static void validate(final GregorianDateInput input, final int commonYear){
		validate(input.indiction(), commonYear, input.month());
	}

	/**
	 * Checks an indiction against a date.
	 *
	 * @param indiction  the indiction (1 to 15), or 0 if missing
	 * @param commonYear the year of the date, counted from 1 January
	 * @param month      the month of the date (1 to 12), or 0 (or a negative season flag) if missing
	 * @throws IllegalArgumentException if the indiction is inconsistent with the date in every style
	 */
	public static void validate(final int indiction, final int commonYear, final int month){
		if(indiction == 0)
			return;

		final int current = of(commonYear);
		final int next = current % CYCLE + 1;
		final boolean nextAllowed = (month <= 0 || month >= FIRST_MONTH_OF_NEXT_INDICTION);
		if(indiction != current && !(nextAllowed && indiction == next))
			throw new IllegalArgumentException("The indiction " + indiction + " is not consistent with the year "
				+ commonYear + " (indiction " + current + (nextAllowed? " or " + next: StringUtils.EMPTY) + ")");
	}

}
