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

import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Utility methods for text-based search (fuzzy, whole-word,
 * diacritics-insensitive).
 */
public final class TextSearchHelper{

	/**
	 * Pre-compiled separator for display-text tokens, used by the
	 * whole-word branch of {@link #matchesText(String, String, Set, double)}.
	 * The pattern splits on any run of characters that are not Unicode
	 * letters or digits, matching the tokenization used by
	 * {@link SearchMatcher}. Compiling once at class load avoids the cost
	 * of recompiling the regex on every call.
	 */
	private static final Pattern DISPLAY_TOKEN_SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}]+");

	/**
	 * Pre-compiled pattern for combining diacritical marks, used by
	 * {@link #normalize(String)}. Pre-compiling avoids the cost of
	 * recompiling the regex on every call to {@code normalize}, which is
	 * invoked at least twice per record during a search.
	 */
	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");


	private TextSearchHelper(){}


	/**
	 * Normalizes text by removing diacritics, expanding common
	 * abbreviations, and converting to lower case.
	 *
	 * @param text the input text
	 * @return the normalized text
	 */
	public static String normalize(final String text){
		if(StringUtils.isEmpty(text))
			return text;

		final String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
		final String withoutMarks = DIACRITICS.matcher(decomposed)
			.replaceAll(StringUtils.EMPTY);
		return withoutMarks.toLowerCase();
	}

	/**
	 * Returns a set of character trigrams for the given text.
	 * The text is padded with two spaces at both ends.
	 */
	public static Set<String> getTrigrams(final String text){
		final Set<String> trigrams = new HashSet<>();
		final String padded = "  " + text + "  ";
		for(int i = 0, length = padded.length() - 2; i < length; i ++)
			trigrams.add(padded.substring(i, i + 3));
		return trigrams;
	}

	/**
	 * Computes the Jaccard similarity between two sets.
	 */
	public static double jaccardSimilarity(final Set<String> set1, final Set<String> set2){
		if(set1.isEmpty() && set2.isEmpty())
			return 1.;

		final Set<String> intersection = new HashSet<>(set1);
		intersection.retainAll(set2);
		final Set<String> union = new HashSet<>(set1);
		union.addAll(set2);
		return (double)intersection.size() / union.size();
	}

	/**
	 * Checks whether the display text matches the search text according
	 * to the given modes.
	 * <p>
	 * The two flags are independent, so the four combinations are
	 * resolved in a cascade:
	 * <ul>
	 *   <li>whole-word only: the search text must equal one complete
	 *       token of the display text, case- and diacritics-insensitive;</li>
	 *   <li>whole-word + fuzzy: any token pair must have a trigram
	 *       similarity of at least {@code threshold};</li>
	 *   <li>fuzzy only: the whole normalized strings must have a trigram
	 *       similarity of at least {@code threshold};</li>
	 *   <li>neither: plain substring match on the normalized strings.</li>
	 * </ul>
	 *
	 * @param displayText the text to search in (e.g., record display text)
	 * @param searchText  the search query
	 * @param modes       the admission modes; may be {@code null} or empty
	 * @param threshold   the similarity threshold for fuzzy matching;
	 *                    ignored when the fuzzy flag is not set
	 * @return {@code true} when the display text matches the search text
	 */
	public static boolean matchesText(final String displayText, final String searchText,
		final Set<SearchMode> modes, final double threshold){
		if(StringUtils.isEmpty(searchText))
			return true;

		final boolean fuzzy = (modes != null && modes.contains(SearchMode.FUZZY));
		final boolean wholeWord = (modes != null && modes.contains(SearchMode.WHOLE_WORD));

		final String normalizedSearch = normalize(searchText);
		final String normalizedDisplay = normalize(displayText);

		if(wholeWord){
			// Token-level comparison: the search text is split into words,
			// the display text is split into words, and the match succeeds
			// when any pair of tokens is equal (exact) or sufficiently
			// similar (fuzzy).
			//
			// Using explicit token comparison instead of a
			// Pattern.compile("\\b" + quote + "\\b") avoids recompiling
			// the regex on every call, and avoids the trap of Java's
			// \b not being Unicode-aware by default.
			final String[] searchTokens = StringUtils.split(normalizedSearch);
			final String[] displayTokens = DISPLAY_TOKEN_SEPARATOR.split(normalizedDisplay);

			for(final String searchToken : searchTokens)
				for(final String displayToken : displayTokens){
					if(fuzzy){
						final double sim = jaccardSimilarity(
							getTrigrams(searchToken), getTrigrams(displayToken));
						if(sim >= threshold)
							return true;
					}
					else if(searchToken.equals(displayToken))
						return true;
				}
			return false;
		}

		// Non-whole-word: the search text may appear anywhere in the
		// display text.
		if(fuzzy){
			final double sim = jaccardSimilarity(
				getTrigrams(normalizedSearch), getTrigrams(normalizedDisplay));
			return sim >= threshold;
		}
		return normalizedDisplay.contains(normalizedSearch);
	}

}
