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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;


/**
 * Comparison of names and titles, as levels of agreement rather than as raw distances.
 * <p>
 * Texts are compared after removing diacritics, case, and punctuation, so that {@code Nicolò} and {@code NICOLO'} agree.
 * <p>
 * The comparisons run for every pair of records of a block, so they read the words of the texts in place, without splitting them,
 * and the texts that are only ASCII are normalized without the regular expressions.
 */
public final class TextSimilarity{

	/**
	 * Level of agreement between two texts, from the strongest.
	 */
	public enum Agreement{
		/** Equal after normalization. */
		EXACT,
		/** The words of one are among the words of the other, or are their initials (e.g. {@code Giovanni} and {@code Giovanni Battista}, {@code G. B.}). */
		SUBSET,
		/** Spelling variants (Jaro-Winkler similarity of at least {@value #SIMILARITY_THRESHOLD}). */
		SIMILAR,
		DIFFERENT;

		public boolean isAtLeast(final Agreement other){
			return (ordinal() <= other.ordinal());
		}
	}


	private static final double SIMILARITY_THRESHOLD = 0.92;

	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
	private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{L}\\p{N}]+");

	private static final JaroWinklerSimilarity JARO_WINKLER = new JaroWinklerSimilarity();


	private TextSimilarity(){}


	public static String normalize(final String text){
		if(text == null)
			return StringUtils.EMPTY;

		final String ascii = normalizeAscii(text);
		if(ascii != null)
			return ascii;

		final String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
		final String withoutMarks = DIACRITICS.matcher(decomposed).replaceAll(StringUtils.EMPTY);
		return NON_ALPHANUMERIC.matcher(withoutMarks.toLowerCase(Locale.ROOT))
			.replaceAll(StringUtils.SPACE)
			.strip();
	}

	/**
	 * Normalizes a text that is only ASCII (no diacritics to remove): lowercase letters and digits, the runs of anything else
	 * turned into a single space, and no space at the ends. The text itself is returned if it is already so.
	 *
	 * @return the normalized text, or {@code null} if the text has a character that is not ASCII
	 */
	private static String normalizeAscii(final String text){
		final int length = text.length();
		boolean normalized = true;
		for(int i = 0; i < length; i ++){
			final char c = text.charAt(i);
			if(c > 127)
				return null;

			final boolean alphanumeric = (c >= 'a' && c <= 'z' || c >= '0' && c <= '9');
			if(!alphanumeric && !(c == ' ' && i > 0 && i < length - 1 && text.charAt(i - 1) != ' ' && text.charAt(i + 1) != ' '))
				normalized = false;
		}
		if(normalized)
			return text;

		final StringBuilder sb = new StringBuilder(length);
		boolean separator = false;
		for(int i = 0; i < length; i ++){
			char c = text.charAt(i);
			if(c >= 'A' && c <= 'Z')
				c += 'a' - 'A';
			if(c >= 'a' && c <= 'z' || c >= '0' && c <= '9'){
				if(separator && !sb.isEmpty())
					sb.append(' ');
				separator = false;
				sb.append(c);
			}
			else
				separator = true;
		}
		return sb.toString();
	}

	/**
	 * Compares two already normalized texts.
	 *
	 * @return the agreement, or {@code null} if either text is empty (nothing to compare)
	 */
	public static Agreement compareNormalized(final String a, final String b){
		if(a.isEmpty() || b.isEmpty())
			return null;
		if(a.equals(b))
			return Agreement.EXACT;

		// numbers are dates, volumes, and pages: titles that differ in them name different documents
		if(numbersDiffer(a, b))
			return Agreement.DIFFERENT;
		if(isContained(a, b) || isContained(b, a))
			return Agreement.SUBSET;

		return (jaroWinkler(a, b) >= SIMILARITY_THRESHOLD? Agreement.SIMILAR: Agreement.DIFFERENT);
	}

	/** Texts longer than this are given to the library (the matches of the shorter ones are kept in the bits of a long). */
	private static final int MAX_BITS = Long.SIZE;
	/** From this Jaro similarity the common prefix raises the Jaro-Winkler one. */
	private static final double BOOST_THRESHOLD = 0.7;
	private static final double SCALING_FACTOR = 0.1;
	private static final int MAX_PREFIX = 4;

	/**
	 * Jaro-Winkler similarity, the same that {@link JaroWinklerSimilarity} gives, without its arrays: for the names (short texts)
	 * the characters that match are kept in two bit sets. It is called for every pair of records that has to be compared.
	 */
	static double jaroWinkler(final String left, final String right){
		if(left.equals(right))
			return 1.;

		final String max;
		final String min;
		if(left.length() > right.length()){
			max = left;
			min = right;
		}
		else{
			max = right;
			min = left;
		}
		final int maxLength = max.length();
		final int minLength = min.length();
		if(maxLength > MAX_BITS)
			return JARO_WINKLER.apply(left, right);

		final int range = Math.max(maxLength / 2 - 1, 0);
		//the characters of the longer text already matched, and the positions of the shorter one that matched
		long usedInMax = 0L;
		long matchedInMin = 0L;
		int matches = 0;
		for(int mi = 0; mi < minLength; mi ++){
			final char c = min.charAt(mi);
			for(int xi = Math.max(mi - range, 0), end = Math.min(mi + range + 1, maxLength); xi < end; xi ++)
				if((usedInMax & 1L << xi) == 0L && c == max.charAt(xi)){
					usedInMax |= 1L << xi;
					matchedInMin |= 1L << mi;
					matches ++;

					break;
				}
		}
		if(matches == 0)
			return 0.;

		//the matched characters, in order of appearance, that differ in the two texts are half transpositions
		int halfTranspositions = 0;
		long remainingInMin = matchedInMin;
		long remainingInMax = usedInMax;
		while(remainingInMin != 0L){
			final int mi = Long.numberOfTrailingZeros(remainingInMin);
			final int xi = Long.numberOfTrailingZeros(remainingInMax);
			if(min.charAt(mi) != max.charAt(xi))
				halfTranspositions ++;
			remainingInMin &= remainingInMin - 1;
			remainingInMax &= remainingInMax - 1;
		}

		int prefix = 0;
		for(int i = 0, limit = Math.min(MAX_PREFIX, minLength); i < limit; i ++){
			if(left.charAt(i) != right.charAt(i))
				break;

			prefix ++;
		}

		final double m = matches;
		final double jaro = (m / left.length() + m / right.length() + (m - (double)halfTranspositions / 2) / m) / 3;
		return (jaro < BOOST_THRESHOLD? jaro: jaro + SCALING_FACTOR * prefix * (1. - jaro));
	}

	/**
	 * Compares two already normalized place names. Unlike personal names, a name with more words is another place
	 * ({@code San Martino} and {@code San Martino di Lupari}, {@code Pordenone} and {@code Prata di Pordenone}): only abbreviations
	 * and initials ({@code S. Martino}) partially agree.
	 *
	 * @return the agreement, or {@code null} if either text is empty
	 */
	public static Agreement comparePlaceNormalized(final String a, final String b){
		final Agreement agreement = compareNormalized(a, b);
		if(agreement != Agreement.SUBSET && agreement != Agreement.SIMILAR)
			return agreement;

		return (countWords(a) == countWords(b)? agreement: Agreement.DIFFERENT);
	}

	/**
	 * Best agreement between any place name of the first group and any of the second.
	 *
	 * @return the agreement, or {@code null} if either group has no name
	 */
	static Agreement bestPlace(final Collection<String> groupA, final Collection<String> groupB){
		Agreement best = null;
		for(final String a : groupA)
			for(final String b : groupB){
				final Agreement agreement = comparePlaceNormalized(a, b);
				if(agreement != null && (best == null || agreement.isAtLeast(best))){
					best = agreement;
					if(best == Agreement.EXACT)
						return best;
				}
			}
		return best;
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
	 * Normalizes every text, dropping the empty ones and the repeated ones.
	 */
	static List<String> normalizeAll(final Collection<String> texts){
		if(texts.isEmpty())
			return List.of();

		final List<String> normalized = new ArrayList<>(texts.size());
		for(final String text : texts){
			final String normalizedText = normalize(text);
			//the lists are short: looking for a repetition in the list costs less than building a set
			if(!normalizedText.isEmpty() && !normalized.contains(normalizedText))
				normalized.add(normalizedText);
		}
		return Collections.unmodifiableList(normalized);
	}

	/**
	 * First characters of a normalized text, spaces excluded, for blocking.
	 */
	static String prefix(final String normalized, final int length){
		final int size = normalized.length();
		//the end of the first characters that are not spaces
		int end = 0;
		int count = 0;
		boolean spaces = false;
		while(end < size && count < length){
			if(normalized.charAt(end) == ' ')
				spaces = true;
			else
				count ++;
			end ++;
		}
		if(!spaces)
			return normalized.substring(0, end);

		final char[] compact = new char[count];
		int written = 0;
		for(int i = 0; i < end; i ++){
			final char c = normalized.charAt(i);
			if(c != ' ')
				compact[written ++] = c;
		}
		return new String(compact);
	}


	/* ======================================================================
	 *                          Words, read in place
	 * ====================================================================== */

	/** @return the start of the first word at or after {@code from} (the words are separated by whitespace), or {@code -1} */
	private static int wordStart(final String text, final int from){
		for(int i = from, length = text.length(); i < length; i ++)
			if(!Character.isWhitespace(text.charAt(i)))
				return i;
		return -1;
	}

	/** @return the end (exclusive) of the word that starts at {@code start} */
	private static int wordEnd(final String text, final int start){
		int end = start;
		final int length = text.length();
		while(end < length && !Character.isWhitespace(text.charAt(end)))
			end ++;
		return end;
	}

	private static int countWords(final String text){
		int count = 0;
		int start = wordStart(text, 0);
		while(start >= 0){
			count ++;
			start = wordStart(text, wordEnd(text, start));
		}
		return count;
	}

	/** @return the start of the first numeric word at or after {@code from}, or {@code -1} */
	private static int numberStart(final String text, final int from){
		int start = wordStart(text, from);
		while(start >= 0){
			final int end = wordEnd(text, start);
			if(isNumeric(text, start, end))
				return start;

			start = wordStart(text, end);
		}
		return -1;
	}

	private static boolean isNumeric(final String text, final int start, final int end){
		for(int i = start; i < end; i ++)
			if(!Character.isDigit(text.charAt(i)))
				return false;
		return (end > start);
	}

	/**
	 * Whether both texts have numbers, and they are not the same, in the same order.
	 */
	private static boolean numbersDiffer(final String a, final String b){
		int startA = numberStart(a, 0);
		int startB = numberStart(b, 0);
		if(startA < 0 || startB < 0)
			return false;

		while(startA >= 0 && startB >= 0){
			final int endA = wordEnd(a, startA);
			final int endB = wordEnd(b, startB);
			if(endA - startA != endB - startB || !a.regionMatches(startA, b, startB, endA - startA))
				return true;

			startA = numberStart(a, endA);
			startB = numberStart(b, endB);
		}
		//the same numbers, unless one text has more of them
		return (startA >= 0 || startB >= 0);
	}

	/** Whether every word of {@code inner} is a word of {@code outer} or the initial of one, in order. */
	private static boolean isContained(final String inner, final String outer){
		int outerPosition = 0;
		for(int innerStart = wordStart(inner, 0); innerStart >= 0; innerStart = wordStart(inner, wordEnd(inner, innerStart))){
			final int innerEnd = wordEnd(inner, innerStart);
			final int innerLength = innerEnd - innerStart;

			boolean found = false;
			for(int outerStart = wordStart(outer, outerPosition); outerStart >= 0; outerStart = wordStart(outer, outerPosition)){
				final int outerEnd = wordEnd(outer, outerStart);
				outerPosition = outerEnd;
				if(innerLength == outerEnd - outerStart && inner.regionMatches(innerStart, outer, outerStart, innerLength)
						|| innerLength == 1 && outer.charAt(outerStart) == inner.charAt(innerStart)){
					found = true;

					break;
				}
			}
			if(!found)
				return false;
		}
		return true;
	}

}
