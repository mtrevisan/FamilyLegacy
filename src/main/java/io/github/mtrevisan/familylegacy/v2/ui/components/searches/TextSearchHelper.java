package io.github.mtrevisan.familylegacy.v2.ui.components.searches;

import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Utility methods for text‑based search (fuzzy, whole‑word, diacritics‑insensitive).
 */
public final class TextSearchHelper{

	private TextSearchHelper(){}


	/**
	 * Normalizes text by removing diacritics, expanding common abbreviations,
	 * and converting to lower case.
	 *
	 * @param text the input text
	 * @return the normalized text
	 */
	public static String normalize(final String text){
		if(StringUtils.isEmpty(text))
			return text;

		String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
		normalized = normalized.replaceAll("\\p{M}", "");

		// Expand abbreviations
		normalized = normalized.replaceAll("\\bSt\\b", "Street");
		normalized = normalized.replaceAll("\\bAve\\b", "Avenue");
		// Add more as needed.

		return normalized.toLowerCase();
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
	 * Checks whether the display text matches the search text according to the given mode.
	 *
	 * @param displayText the text to search in (e.g., record display text)
	 * @param searchText  the search query
	 * @param fuzzy       if true, uses trigram similarity (Jaccard)
	 * @param wholeWord   if true, uses whole‑word regex match
	 * @param threshold   the similarity threshold for fuzzy matching (ignored if fuzzy is false)
	 * @return true if the text matches
	 */
	public static boolean matchesText(final String displayText, final String searchText, final boolean fuzzy,
			final boolean wholeWord, final double threshold){
		if(StringUtils.isEmpty(searchText))
			return true;

		final String normalizedSearch = normalize(searchText);
		final String normalizedDisplay = normalize(displayText);

		if(wholeWord){
			final Pattern pattern = Pattern.compile("\\b" + Pattern.quote(normalizedSearch) + "\\b");
			return pattern.matcher(normalizedDisplay).find();
		}
		else if(fuzzy){
			final Set<String> searchTrigrams = getTrigrams(normalizedSearch);
			final Set<String> displayTrigrams = getTrigrams(normalizedDisplay);
			final double similarity = jaccardSimilarity(searchTrigrams, displayTrigrams);
			return similarity >= threshold;
		}
		else
			return normalizedDisplay.contains(normalizedSearch);
	}

}
