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
package io.github.mtrevisan.familylegacy.ui.components.searches;

import java.util.Locale;


/**
 * Word-level matcher and scorer for the record search.
 * <p>
 * Both the query and the target are split into tokens. Every query token
 * is scored against the best-matching target token; the record is a
 * candidate when at least one query token matches, and it is ranked by
 * the number of matched tokens first, then by the sum of the per-token
 * scores. Order-independent, so "gall bor" matches "Bortolo Gallinaro"
 * just like "bor gall".
 * <p>
 * The mode only decides which tokens are <b>admissible</b>. The score of
 * an admissible pair is always the same fuzzy formula, based on the
 * Levenshtein distance normalised by the longer of the two tokens, so
 * "bortolo" ranks above "bortolp" which ranks above "bor" for the query
 * "bortolo".
 */
public final class SearchMatcher{

	private SearchMatcher(){}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Score of a match between a query and a display text.
	 *
	 * @param matchedTokens how many query tokens found at least one match;
	 *                      used as the primary sort key
	 * @param score         sum of the per-token scores; used as the
	 *                      secondary sort key
	 * @param passes        whether the record is admitted at all; a record
	 *                      with no matched tokens is not admitted, but a
	 *                      blank query admits everything
	 */
	public record MatchScore(int matchedTokens, double score, boolean passes){

		/** No match: the record is excluded from the results. */
		public static final MatchScore NONE = new MatchScore(0, 0., false);

		/** Blank query: the record passes, but contributes nothing to ranking. */
		public static final MatchScore ANY = new MatchScore(0, 0., true);

	}


	/**
	 * Scores the match between the query and the target.
	 * <p>
	 * The mode decides <b>admission</b>: whether a pair of tokens is a
	 * candidate at all. The <b>score</b> is always the same fuzzy
	 * formula, so the ranking is independent of the mode and only the
	 * set of candidates changes.
	 *
	 * @param query  the user input; blank admits everything
	 * @param target the display text of the record; {@code null} never matches
	 * @param mode   the admission mode; {@code null} defaults to {@link SearchMode#FUZZY}
	 * @return the score, never {@code null}
	 */
	public static MatchScore score(final String query, final String target, final SearchMode mode){
		if(query == null || query.isBlank())
			return MatchScore.ANY;
		if(target == null)
			return MatchScore.NONE;

		final SearchMode effectiveMode = (mode != null? mode: SearchMode.FUZZY);

		final String[] queryTokens = query.toLowerCase(Locale.ROOT)
			.trim()
			.split("\\s+");
		final String[] targetTokens = target.toLowerCase(Locale.ROOT)
			// Split on anything that is not a letter or a digit, so
			// "Rossi-Bianchi" becomes two tokens and "de' Medici" keeps
			// "de" and "medici" separate. \\p{L} and \\p{N} are
			// Unicode-aware, so accented letters are preserved.
			.split("[^\\p{L}\\p{N}]+");

		int matchedTokens = 0;
		double totalScore = 0.;
		for(final String queryToken : queryTokens){
			double best = 0.;
			for(final String targetToken : targetTokens){
				if(!admits(queryToken, targetToken, effectiveMode))
					continue;

				final double s = similarity(queryToken, targetToken);
				if(s > best)
					best = s;
			}
			if(best > 0.){
				matchedTokens ++;
				totalScore += best;
			}
		}

		return (matchedTokens > 0
			? new MatchScore(matchedTokens, totalScore, true)
			: MatchScore.NONE);
	}


	/* ======================================================================
	 *                          Admission
	 * ====================================================================== */

	/**
	 * Admission: the mode decides whether the pair is a candidate at all.
	 */
	private static boolean admits(final String queryToken, final String targetToken, final SearchMode mode){
		return switch(mode){
			case WHOLE_WORD -> targetToken.equals(queryToken);
			case FUZZY -> (targetToken.contains(queryToken) || isWithinBudget(queryToken, targetToken));
		};
	}


	/* ======================================================================
	 *                          Similarity (always fuzzy)
	 * ====================================================================== */

	/**
	 * Similarity of two tokens, based on the Levenshtein distance
	 * normalised by the longer of the two. Always the same formula,
	 * independent of the mode.
	 * <ul>
	 *   <li>exact match → 1;</li>
	 *   <li>one-character difference on a seven-character word → 0.86;</li>
	 *   <li>prefix "bor" on "bortolo" → 0.57;</li>
	 *   <li>completely different tokens → 0.</li>
	 * </ul>
	 */
	private static double similarity(final String queryToken, final String targetToken){
		if(queryToken.equals(targetToken))
			return 1.;

		final int maxLen = Math.max(queryToken.length(), targetToken.length());
		if(maxLen == 0)
			return 0.;

		final int distance = levenshtein(queryToken, targetToken, maxLen);
		return Math.max(0., 1. - (double)distance / maxLen);
	}

	private static boolean isWithinBudget(final String queryToken, final String targetToken){
		final int budget = Math.max(1, queryToken.length() / 3);
		if(Math.abs(queryToken.length() - targetToken.length()) > budget)
			return false;

		return levenshtein(queryToken, targetToken, budget) <= budget;
	}


	/* ======================================================================
	 *                          Levenshtein
	 * ====================================================================== */

	/**
	 * Levenshtein edit distance, with early exit when the running minimum
	 * already exceeds the budget. This keeps the fuzzy match cheap on
	 * long target tokens.
	 *
	 * @param a      the first string
	 * @param b      the second string
	 * @param budget the maximum distance of interest; the exact value
	 *               above the budget is not computed, {@code budget + 1}
	 *               is returned instead
	 * @return the edit distance, or {@code budget + 1} when it exceeds
	 * the budget
	 */
	private static int levenshtein(final String a, final String b, final int budget){
		final int n = a.length();
		final int m = b.length();
		int[] prev = new int[m + 1];
		int[] curr = new int[m + 1];

		for(int j = 0; j <= m; j ++)
			prev[j] = j;

		for(int i = 1; i <= n; i ++){
			curr[0] = i;
			int rowMin = curr[0];

			for(int j = 1; j <= m; j ++){
				final int cost = (a.charAt(i - 1) == b.charAt(j - 1)? 0: 1);
				curr[j] = Math.min(
					Math.min(curr[j - 1] + 1, prev[j] + 1),
					prev[j - 1] + cost);
				if(curr[j] < rowMin)
					rowMin = curr[j];
			}

			if(rowMin > budget)
				return budget + 1;

			final int[] tmp = prev;
			prev = curr;
			curr = tmp;
		}
		return prev[m];
	}

}
