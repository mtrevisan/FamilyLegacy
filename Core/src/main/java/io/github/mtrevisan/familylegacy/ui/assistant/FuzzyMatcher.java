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
package io.github.mtrevisan.familylegacy.ui.assistant;

import java.util.Locale;


/** A forgiving search: the characters of the query must appear in the text in the same order, not necessarily together. */
public final class FuzzyMatcher{

	/** The score of a text that does not match. */
	public static final int NO_MATCH = -1;

	private static final int MATCH = 10;
	private static final int CONSECUTIVE_BONUS = 20;
	private static final int WORD_START_BONUS = 15;
	private static final int GAP_PENALTY = 1;


	private FuzzyMatcher(){}


	/**
	 * Scores a text against a query: the higher, the better the match (consecutive characters and word starts count more,
	 * skipped characters count less).
	 *
	 * @param query the text typed by the user (case is ignored)
	 * @param text  the text to search in
	 * @return {@link #NO_MATCH} if the query does not match, a positive score otherwise ({@code 1} for an empty query)
	 */
	public static int score(final String query, final String text){
		if(query == null || query.isBlank())
			return 1;
		if(text == null)
			return NO_MATCH;

		final String q = query.strip().toLowerCase(Locale.ROOT);
		final String t = text.toLowerCase(Locale.ROOT);
		int score = 0;
		int previous = -2;
		int from = 0;
		for(int i = 0; i < q.length(); i ++){
			final char c = q.charAt(i);
			if(Character.isWhitespace(c))
				continue;

			final int index = t.indexOf(c, from);
			if(index < 0)
				return NO_MATCH;

			score += MATCH;
			if(index == previous + 1)
				score += CONSECUTIVE_BONUS;
			if(index == 0 || !Character.isLetterOrDigit(t.charAt(index - 1)))
				score += WORD_START_BONUS;
			if(previous >= 0)
				score -= Math.min(index - previous - 1, MATCH - 1) * GAP_PENALTY;
			previous = index;
			from = index + 1;
		}
		return Math.max(1, score);
	}

}
