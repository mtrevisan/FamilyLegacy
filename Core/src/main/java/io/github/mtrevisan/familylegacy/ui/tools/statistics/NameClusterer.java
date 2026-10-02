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
package io.github.mtrevisan.familylegacy.ui.tools.statistics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Groups surnames or given names that are plausible variants of the
 * same name, using a trigram similarity measure.
 * <p>
 * Each name is decomposed into the set of its overlapping three-character
 * substrings, padded at both ends with two spaces so that prefixes and
 * suffixes contribute their own trigrams. Two names are considered
 * variants when the Dice coefficient of their trigram sets is at least
 * {@value #MIN_SIMILARITY}.
 * <p>
 * The trigram approach is the same technique used by PostgreSQL's
 * {@code pg_trgm} extension and by Lucene's fuzzy matching. It is more
 * tolerant than a prefix-based comparison (it does not require the
 * difference to be in the suffix) and more discriminating than plain
 * Levenshtein (two names with the same edit distance can have very
 * different trigram overlap).
 */
final class NameClusterer{


	/**
	 * Minimum Dice similarity for two names to be considered variants.
	 * 0.6 is a good balance for Italian names: it merges the common
	 * variations (doubled letters, final vowels, z/s alternation)
	 * without merging names that only share a short prefix.
	 */
	private static final double MIN_SIMILARITY = 0.71;

	/**
	 * Names shorter than this length use a direct comparison instead of
	 * trigrams: with two or three characters there are too few trigrams
	 * to make the measure meaningful.
	 */
	private static final int MIN_LENGTH_FOR_TRIGRAMS = 3;

	/** Maximum edit distance for names shorter than the trigram threshold. */
	private static final int MAX_EDITS_FOR_SHORT_NAMES = 1;


	private NameClusterer(){}


	/**
	 * Clusters the given name counts.
	 *
	 * @param counts a map from name (lowercase) to occurrence count
	 * @return the groups, sorted by descending total count
	 */
	static List<Statistics.NameGroup> cluster(final Map<String, Integer> counts){
		// Sort by descending frequency, then alphabetically, so the
		// most common spelling becomes the canonical form of its group
		// and the ordering is deterministic.
		final List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
		sorted.sort((a, b) -> {
			final int byCount = Integer.compare(b.getValue(), a.getValue());
			return (byCount != 0? byCount: a.getKey()
				.compareTo(b.getKey()));
		});

		final List<Bucket> buckets = new ArrayList<>();
		for(final Map.Entry<String, Integer> e : sorted){
			final String name = e.getKey();
			final int count = e.getValue();

			Bucket target = null;
			for(final Bucket b : buckets)
				if(areVariants(b.canonical, name)){
					target = b;
					break;
				}

			if(target == null){
				final Bucket b = new Bucket(name);
				b.variants.add(name);
				b.count = count;
				buckets.add(b);
			}
			else{
				if(!target.variants.contains(name))
					target.variants.add(name);
				target.count += count;
			}
		}

		buckets.sort((a, b) -> Integer.compare(b.count, a.count));

		final List<Statistics.NameGroup> out = new ArrayList<>(buckets.size());
		for(final Bucket b : buckets)
			out.add(new Statistics.NameGroup(b.canonical, List.copyOf(b.variants), b.count));
		return out;
	}


	/* ======================================================================
	 *                          Variant check
	 * ====================================================================== */

	private static boolean areVariants(final String a, final String b){
		if(a == null || b == null || a.isEmpty() || b.isEmpty())
			return false;
		if(a.equals(b))
			return true;

		// Short names: trigrams are too coarse, fall back to edit distance.
		if(a.length() < MIN_LENGTH_FOR_TRIGRAMS || b.length() < MIN_LENGTH_FOR_TRIGRAMS)
			return levenshtein(a, b) <= MAX_EDITS_FOR_SHORT_NAMES;

		return trigramSimilarity(a, b) >= MIN_SIMILARITY;
	}


	/* ======================================================================
	 *                          Trigram similarity
	 * ====================================================================== */

	/**
	 * Returns the set of overlapping three-character substrings of the
	 * given name, with the name padded at both ends by two spaces. The
	 * padding makes the beginning and the end of the name contribute
	 * their own trigrams, so a name that only matches another in the
	 * middle does not score artificially high.
	 */
	private static Set<String> trigrams(final String name){
		final String padded = "  " + name + "  ";
		final Set<String> out = new HashSet<>(padded.length());
		for(int i = 0; i <= padded.length() - 3; i++)
			out.add(padded.substring(i, i + 3));
		return out;
	}

	/**
	 * Dice coefficient of the two trigram sets: {@code 2 * |A ∩ B| / (|A| + |B|)}.
	 * The result is a value in {@code [0, 1]}, where 1 means the two
	 * names have identical trigram sets.
	 */
	private static double trigramSimilarity(final String a, final String b){
		final Set<String> ta = trigrams(a);
		final Set<String> tb = trigrams(b);
		if(ta.isEmpty() || tb.isEmpty())
			return 0;

		// Iterate on the smaller set for efficiency.
		final Set<String> smaller = (ta.size() <= tb.size()? ta: tb);
		final Set<String> larger = (ta.size() <= tb.size()? tb: ta);
		int common = 0;
		for(final String t : smaller)
			if(larger.contains(t))
				common++;

		return 2.0 * common / (ta.size() + tb.size());
	}


	/* ======================================================================
	 *                          Edit distance (short names)
	 * ====================================================================== */

	private static int levenshtein(final String a, final String b){
		final int n = a.length();
		final int m = b.length();
		final int[] prev = new int[m + 1];
		final int[] curr = new int[m + 1];
		for(int j = 0; j <= m; j++)
			prev[j] = j;
		for(int i = 1; i <= n; i++){
			curr[0] = i;
			for(int j = 1; j <= m; j++){
				final int cost = (a.charAt(i - 1) == b.charAt(j - 1)? 0: 1);
				curr[j] = Math.min(
					Math.min(curr[j - 1] + 1, prev[j] + 1),
					prev[j - 1] + cost);
			}
			System.arraycopy(curr, 0, prev, 0, m + 1);
		}
		return prev[m];
	}


	private static final class Bucket{
		final String canonical;
		final List<String> variants = new ArrayList<>();
		int count;

		Bucket(final String canonical){
			this.canonical = canonical;
		}
	}

}
