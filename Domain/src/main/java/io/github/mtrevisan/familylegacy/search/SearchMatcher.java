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

package io.github.mtrevisan.familylegacy.search;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Word-level matcher and scorer for the record search.
 * <p>
 * The query is parsed into terms ({@link SearchQuery}: words, <code>"exact phrases"</code> and wildcards), and the target is split
 * into tokens. Every word is scored against the best-matching target token; the record is a candidate when at least one term matches,
 * and it is ranked by the number of matched terms first, then by relevance. Order-independent, so "gall bor" matches
 * "Bortolo Gallinaro" just like "bor gall".
 * <p>
 * The mode only decides which tokens are <b>admissible</b>. The similarity of an admissible pair is always the same fuzzy formula,
 * based on the Levenshtein distance normalised by the longer of the two tokens, so "bortolo" ranks above "bortolp" which ranks above
 * "bor" for the query "bortolo".
 * <p>
 * There are two ways to use it. {@link #score(String, String, Set)} judges a text alone (a combo box, a tree filter). For a whole
 * corpus the text is {@link #analyse(SearchQuery, List, Set) analysed} and the analyses are ranked together by {@link Bm25}, that
 * needs the statistics of the corpus; the analysis also remembers <b>where</b> every term was found.
 */
public final class SearchMatcher{

	/**
	 * Pre-compiled separator for target tokens. The pattern splits on any
	 * run of characters that are not Unicode letters or digits, so
	 * "Rossi-Bianchi" becomes two tokens and "de' Medici" keeps "de" and
	 * "medici" separate. Compiling once at class load avoids the cost of
	 * recompiling the regex on every call, which is what
	 * {@link String#split(String)} does internally.
	 */
	private static final Pattern TARGET_TOKEN_SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}]+");

	/** How many different pieces of text are remembered as the evidence of a term, at most. */
	private static final int MAX_EVIDENCE_PER_TERM = 3;


	private SearchMatcher(){}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Score of a match between a query and a display text.
	 *
	 * @param matchedTokens how many query terms found at least one match;
	 *                      used as the primary sort key
	 * @param score         sum of the per-term scores; used as the
	 *                      secondary sort key
	 * @param passes        whether the record is admitted at all; a record
	 *                      with no matched terms is not admitted, but a
	 *                      blank query admits everything
	 */
	public record MatchScore(int matchedTokens, double score, boolean passes){

		/** No match: the record is excluded from the results. */
		public static final MatchScore NONE = new MatchScore(0, 0., false);

		/** Blank query: the record passes, but contributes nothing to ranking. */
		public static final MatchScore ANY = new MatchScore(0, 0., true);

	}

	/**
	 * A piece of text that satisfied a term, and where it was.
	 *
	 * @param field	Which text of the record it was found in.
	 * @param text		The word (or the phrase) that matched, in lower case.
	 */
	public record Evidence(SearchField field, String text){}

	/**
	 * How a whole record fared against the terms of a query: for each term, how well it matched, how often, and where.
	 * It is filled one piece of text at a time by {@link #analyse(SearchQuery, List, Set)}.
	 */
	public static final class Analysis{

		private final SearchQuery query;
		private final double[] best;
		private final double[] frequency;
		private final List<List<Evidence>> evidence;
		private int length;


		private Analysis(final SearchQuery query){
			this.query = query;
			final int terms = query.terms().size();
			best = new double[terms];
			frequency = new double[terms];
			evidence = new ArrayList<>(terms);
			for(int i = 0; i < terms; i ++)
				evidence.add(new ArrayList<>(1));
		}


		public SearchQuery query(){
			return query;
		}

		/** @return	The number of words of the record (all its texts together): the length of the document for the ranking. */
		public int length(){
			return length;
		}

		/**
		 * The frequency of a term in the record: how many times it occurred, the occurrences of a word weighted by their similarity
		 * (an exact word counts 1, a typo less).
		 */
		public double frequency(final int term){
			return frequency[term];
		}

		/** @return	The similarity of the best match of a term, from 0 (not found) to 1 (exact). */
		public double best(final int term){
			return best[term];
		}

		/** @return	Where the term was found, empty when it was not. */
		public List<Evidence> evidence(final int term){
			return evidence.get(term);
		}

		/** @return	How many terms were found. */
		public int matchedTerms(){
			int count = 0;
			for(final double b : best)
				if(b > 0.)
					count ++;
			return count;
		}

		/** @return	The score as the plain matcher gives it: no corpus statistics, the similarities of the terms summed. */
		MatchScore toMatchScore(){
			int matched = 0;
			double total = 0.;
			for(final double b : best)
				if(b > 0.){
					matched ++;
					total += b;
				}
			return (matched > 0
				? new MatchScore(matched, total, true)
				: MatchScore.NONE);
		}

	}

	/**
	 * A text of a record, and which one it is.
	 *
	 * @param field	What the text is.
	 * @param text		The text itself.
	 */
	public record Segment(SearchField field, String text){}


	/**
	 * Scores the match between the query and the target.
	 * <p>
	 * The {@code modes} set decides <b>admission</b>: whether a pair
	 * of tokens is a candidate at all. The <b>score</b> is always the same
	 * fuzzy formula, so the ranking is independent of the modes and only
	 * the set of candidates changes.
	 * <p>
	 * The admission rules for a word are:
	 * <ul>
	 *   <li>empty set → plain substring match;</li>
	 *   <li>only {@link SearchMode#WHOLE_WORD} → exact token match;</li>
	 *   <li>only {@link SearchMode#FUZZY} → typo-tolerant substring
	 *       match;</li>
	 *   <li>both → typo-tolerant match restricted to whole tokens.</li>
	 * </ul>
	 * A phrase is always exact, and a wildcard is always a whole token (see {@link SearchQuery}).
	 *
	 * @param query  the user input; blank admits everything
	 * @param target the display text of the record; {@code null} never matches
	 * @param modes  the admission modes; may be {@code null} or empty
	 * @return the score, never {@code null}
	 */
	public static MatchScore score(final String query, final String target, final Set<SearchMode> modes){
		if(StringUtils.isEmpty(query))
			return MatchScore.ANY;

		final SearchQuery parsed = SearchQuery.parse(query);
		if(parsed.isEmpty())
			return MatchScore.ANY;
		if(target == null)
			return MatchScore.NONE;

		return analyse(parsed, List.of(new Segment(SearchField.DISPLAY_TEXT, target)), modes)
			.toMatchScore();
	}

	/**
	 * Analyses the texts of a record against the terms of a query.
	 *
	 * @param query		The parsed query.
	 * @param segments	The texts of the record, each with the field it belongs to.
	 * @param modes		The admission modes; may be {@code null} or empty.
	 * @return	How every term fared.
	 */
	public static Analysis analyse(final SearchQuery query, final List<Segment> segments, final Set<SearchMode> modes){
		final Analysis analysis = new Analysis(query);
		for(final Segment segment : segments)
			analyseSegment(analysis, segment, modes);
		return analysis;
	}

	private static void analyseSegment(final Analysis analysis, final Segment segment, final Set<SearchMode> modes){
		if(segment.text() == null)
			return;

		final String text = segment.text().toLowerCase(Locale.ROOT);
		final List<String> tokens = tokenize(text);
		analysis.length += tokens.size();

		final List<SearchQuery.Term> terms = analysis.query.terms();
		for(int i = 0; i < terms.size(); i ++){
			final SearchQuery.Term term = terms.get(i);
			switch(term.kind()){
				case WORD -> analyseWord(analysis, i, term.text(), tokens, segment.field(), modes);
				case WILDCARD -> analyseWildcard(analysis, i, term.text(), tokens, segment.field());
				case PHRASE -> analysePhrase(analysis, i, term.text(), text, segment.field(), modes);
			}
		}
	}

	private static List<String> tokenize(final String lowerCaseText){
		final List<String> tokens = new ArrayList<>();
		for(final String token : TARGET_TOKEN_SEPARATOR.split(lowerCaseText))
			//a leading separator gives an empty token, that is no word
			if(!token.isEmpty())
				tokens.add(token);
		return tokens;
	}

	private static void analyseWord(final Analysis analysis, final int term, final String word, final List<String> tokens,
			final SearchField field, final Set<SearchMode> modes){
		for(final String token : tokens){
			if(!admits(word, token, modes))
				continue;

			final double s = similarity(word, token);
			if(s > 0.)
				record(analysis, term, s, s, field, token);
		}
	}

	private static void analyseWildcard(final Analysis analysis, final int term, final String pattern, final List<String> tokens,
			final SearchField field){
		for(final String token : tokens)
			if(globMatches(pattern, token))
				record(analysis, term, 1., 1., field, token);
	}

	private static void analysePhrase(final Analysis analysis, final int term, final String phrase, final String text,
			final SearchField field, final Set<SearchMode> modes){
		final boolean wholeWord = (modes != null && modes.contains(SearchMode.WHOLE_WORD));
		int occurrences = 0;
		int from = 0;
		for(int at = text.indexOf(phrase, from); at >= 0; at = text.indexOf(phrase, from)){
			from = at + phrase.length();
			if(!wholeWord || isOnWordBoundaries(text, at, from))
				occurrences ++;
		}
		if(occurrences > 0)
			record(analysis, term, 1., occurrences, field, phrase);
	}

	/** @return	Whether the text between {@code start} (included) and {@code end} (excluded) is not the part of a longer word. */
	private static boolean isOnWordBoundaries(final String text, final int start, final int end){
		if(Character.isLetterOrDigit(text.charAt(start)) && start > 0 && Character.isLetterOrDigit(text.charAt(start - 1)))
			return false;
		return !(Character.isLetterOrDigit(text.charAt(end - 1)) && end < text.length() && Character.isLetterOrDigit(text.charAt(end)));
	}

	private static void record(final Analysis analysis, final int term, final double similarity, final double frequency,
			final SearchField field, final String found){
		if(similarity > analysis.best[term])
			analysis.best[term] = similarity;
		analysis.frequency[term] += frequency;

		final List<Evidence> evidence = analysis.evidence.get(term);
		if(evidence.size() < MAX_EVIDENCE_PER_TERM){
			final Evidence e = new Evidence(field, found);
			if(!evidence.contains(e))
				evidence.add(e);
		}
	}


	/* ======================================================================
	 *                          Wildcards
	 * ====================================================================== */

	/**
	 * Matches a token against a pattern whose wildcards are the markers of {@link SearchQuery}: no regular expression, just the
	 * classic two-pointer walk that remembers the last run to retry from.
	 */
	static boolean globMatches(final String pattern, final String token){
		final int patternLength = pattern.length();
		final int tokenLength = token.length();
		int p = 0;
		int t = 0;
		int lastRun = -1;
		int resume = 0;
		while(t < tokenLength){
			if(p < patternLength && pattern.charAt(p) == SearchQuery.ANY_RUN){
				lastRun = p ++;
				resume = t;
			}
			else if(p < patternLength && (pattern.charAt(p) == SearchQuery.ANY_ONE || pattern.charAt(p) == token.charAt(t))){
				p ++;
				t ++;
			}
			else if(lastRun >= 0){
				p = lastRun + 1;
				t = ++ resume;
			}
			else
				return false;
		}
		while(p < patternLength && pattern.charAt(p) == SearchQuery.ANY_RUN)
			p ++;
		return (p == patternLength);
	}


	/* ======================================================================
	 *                          Admission
	 * ====================================================================== */

	/**
	 * Admission: the modes decide whether the pair is a candidate at all.
	 * <p>
	 * The two flags are independent, so the four combinations are resolved
	 * in a cascade:
	 * <ul>
	 *   <li>whole-word only: exact token match, case-insensitive (both
	 *       inputs are already lower-cased by the caller);</li>
	 *   <li>whole-word + fuzzy: token match within the edit-distance
	 *       budget;</li>
	 *   <li>fuzzy only: substring match, or substring within the
	 *       edit-distance budget;</li>
	 *   <li>neither: plain substring match.</li>
	 * </ul>
	 * The result of this method is only <b>admission</b>: whether the pair
	 * is a candidate. The numeric score is computed separately by
	 * {@link #similarity(String, String)} and is independent of the modes,
	 * so the ranking stays consistent when the user toggles the flags.
	 *
	 * @param queryToken  the lower-cased query token
	 * @param targetToken the lower-cased target token
	 * @param modes       the admission modes; may be {@code null} or empty
	 * @return {@code true} when the target token is admitted for the query
	 *         token
	 */
	private static boolean admits(final String queryToken, final String targetToken,
		final Set<SearchMode> modes){
		final boolean fuzzy = (modes != null && modes.contains(SearchMode.FUZZY));
		final boolean wholeWord = (modes != null && modes.contains(SearchMode.WHOLE_WORD));

		if(wholeWord){
			if(fuzzy)
				return isWithinBudget(queryToken, targetToken);
			return targetToken.equals(queryToken);
		}

		if(fuzzy)
			return targetToken.contains(queryToken) || isWithinBudget(queryToken, targetToken);

		return targetToken.contains(queryToken);
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
