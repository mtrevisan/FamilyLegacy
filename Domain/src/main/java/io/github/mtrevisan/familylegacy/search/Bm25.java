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

import java.util.List;


/**
 * The BM25 relevance of a set of records for a query.
 * <p>
 * For every term of the query, the contribution to a record is
 * <pre>
 *   idf(term) · f·(k1 + 1) / (f + k1·(1 − b + b·length / averageLength))
 * </pre>
 * where {@code f} is the frequency of the term in the record (see {@link SearchMatcher.Analysis#frequency(int)}), {@code length} the
 * number of its words and {@code averageLength} the mean of the corpus. The inverse document frequency is
 * {@code ln(1 + (N − n + 0.5) / (n + 0.5))}, with {@code N} the records of the corpus and {@code n} those where the term was found
 * (it is never negative, so a term found everywhere weighs little instead of subtracting).
 * <p>
 * The effect is that a rare word (a surname) weighs more than a common one (a given name, or a word that is in every title), a term
 * repeated in the record saturates instead of growing without bound, and a short record (a name) beats a long one (a note)
 * for the same match.
 * <p>
 * The corpus is the one passed in: it should hold <b>all</b> the records that could have been found, including those that matched
 * nothing, otherwise every term looks common.
 */
public final class Bm25{

	/** Saturation of the frequency of a term. */
	public static final double K1 = 1.2;
	/** How much the length of the record counts: 0 not at all, 1 fully. */
	public static final double B = 0.75;


	private Bm25(){}


	/**
	 * @param corpus	The analyses of every record of the corpus, all of the same query.
	 * @return	The relevance of each record, in the same order; 0 for a record where no term was found.
	 */
	public static double[] score(final List<SearchMatcher.Analysis> corpus){
		final int size = corpus.size();
		final double[] scores = new double[size];
		if(size == 0)
			return scores;

		final int terms = corpus.getFirst().query().terms().size();
		final int[] documentFrequency = new int[terms];
		double totalLength = 0.;
		for(final SearchMatcher.Analysis analysis : corpus){
			totalLength += analysis.length();
			for(int t = 0; t < terms; t ++)
				if(analysis.frequency(t) > 0.)
					documentFrequency[t] ++;
		}
		final double averageLength = Math.max(1., totalLength / size);

		final double[] idf = new double[terms];
		for(int t = 0; t < terms; t ++)
			idf[t] = Math.log(1. + (size - documentFrequency[t] + 0.5) / (documentFrequency[t] + 0.5));

		for(int i = 0; i < size; i ++){
			final SearchMatcher.Analysis analysis = corpus.get(i);
			final double norm = K1 * (1. - B + B * analysis.length() / averageLength);
			double sum = 0.;
			for(int t = 0; t < terms; t ++){
				final double f = analysis.frequency(t);
				if(f > 0.)
					sum += idf[t] * f * (K1 + 1.) / (f + norm);
			}
			scores[i] = sum;
		}
		return scores;
	}

}
