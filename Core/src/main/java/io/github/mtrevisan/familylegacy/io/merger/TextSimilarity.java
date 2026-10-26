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
package io.github.mtrevisan.familylegacy.io.merger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;


/**
 * Comparison of names and titles, as levels of agreement rather than as raw distances.
 * <p>
 * Texts are compared after removing diacritics, case, and punctuation, so that {@code Nicolò} and {@code NICOLO'} agree.
 */
final class TextSimilarity{

	/**
	 * Level of agreement between two texts, from the strongest.
	 */
	enum Agreement{
		/** Equal after normalization. */
		EXACT,
		/** The words of one are among the words of the other, or are their initials (e.g. {@code Giovanni} and {@code Giovanni Battista}, {@code G. B.}). */
		SUBSET,
		/** Spelling variants (Jaro-Winkler similarity of at least {@value #SIMILARITY_THRESHOLD}). */
		SIMILAR,
		DIFFERENT;

		boolean isAtLeast(final Agreement other){
			return (ordinal() <= other.ordinal());
		}
	}


	private static final double SIMILARITY_THRESHOLD = 0.92;

	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
	private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{L}\\p{N}]+");

	private static final JaroWinklerSimilarity JARO_WINKLER = new JaroWinklerSimilarity();


	private TextSimilarity(){}


	static String normalize(final String text){
		if(text == null)
			return StringUtils.EMPTY;

		final String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
		final String withoutMarks = DIACRITICS.matcher(decomposed).replaceAll(StringUtils.EMPTY);
		return NON_ALPHANUMERIC.matcher(withoutMarks.toLowerCase(Locale.ROOT))
			.replaceAll(StringUtils.SPACE)
			.strip();
	}

	/**
	 * Compares two already normalized texts.
	 *
	 * @return the agreement, or {@code null} if either text is empty (nothing to compare)
	 */
	static Agreement compareNormalized(final String a, final String b){
		if(a.isEmpty() || b.isEmpty())
			return null;
		if(a.equals(b))
			return Agreement.EXACT;

		final String[] wordsA = StringUtils.split(a);
		final String[] wordsB = StringUtils.split(b);
		// numbers are dates, volumes, and pages: titles that differ in them name different documents
		final List<String> numbersA = numbers(wordsA);
		final List<String> numbersB = numbers(wordsB);
		if(!numbersA.isEmpty() && !numbersB.isEmpty() && !numbersA.equals(numbersB))
			return Agreement.DIFFERENT;
		if(isContained(wordsA, wordsB) || isContained(wordsB, wordsA))
			return Agreement.SUBSET;

		return (JARO_WINKLER.apply(a, b) >= SIMILARITY_THRESHOLD? Agreement.SIMILAR: Agreement.DIFFERENT);
	}

	/**
	 * Best agreement between any text of the first group and any of the second.
	 *
	 * @return the agreement, or {@code null} if either group has no text
	 */
	static Agreement best(final Collection<String> groupA, final Collection<String> groupB){
		Agreement best = null;
		for(final String a : groupA)
			for(final String b : groupB){
				final Agreement agreement = compareNormalized(a, b);
				if(agreement != null && (best == null || agreement.isAtLeast(best))){
					best = agreement;
					if(best == Agreement.EXACT)
						return best;
				}
			}
		return best;
	}

	/**
	 * Normalizes every text, dropping the empty ones.
	 */
	static List<String> normalizeAll(final Collection<String> texts){
		return texts.stream()
			.map(TextSimilarity::normalize)
			.filter(text -> !text.isEmpty())
			.distinct()
			.toList();
	}

	/**
	 * First characters of a normalized text, spaces excluded, for blocking.
	 */
	static String prefix(final String normalized, final int length){
		final String compact = StringUtils.remove(normalized, ' ');
		return compact.substring(0, Math.min(length, compact.length()));
	}

	private static List<String> numbers(final String[] words){
		final List<String> numbers = new ArrayList<>(0);
		for(final String word : words)
			if(StringUtils.isNumeric(word))
				numbers.add(word);
		return numbers;
	}

	/** Whether every word of {@code inner} is a word of {@code outer} or the initial of one, in order. */
	private static boolean isContained(final String[] inner, final String[] outer){
		if(inner.length > outer.length)
			return false;

		int j = 0;
		for(final String word : inner){
			while(j < outer.length && !matchesWord(word, outer[j]))
				j ++;
			if(j == outer.length)
				return false;

			j ++;
		}
		return true;
	}

	private static boolean matchesWord(final String word, final String other){
		return (word.equals(other) || word.length() == 1 && other.charAt(0) == word.charAt(0));
	}

}
