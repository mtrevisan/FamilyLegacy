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
package io.github.mtrevisan.familylegacy.v2.io.merger;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.HashMap;
import java.util.Map;


/**
 * Probabilistic scorer based on the Fellegi‑Sünther model.
 * It computes a match score by combining field‑wise similarities with configurable weights.
 * The score is a weighted average of the individual field similarities.
 */
public class FellegiSuenterScorer{

	private final Map<String, Double> weights;
	private final double threshold;


	/**
	 * Constructs a scorer with given field weights and a threshold.
	 *
	 * @param weights   field name -> weight (sum should be 1.0)
	 * @param threshold minimum score to consider a match (0..1)
	 */
	public FellegiSuenterScorer(final Map<String, Double> weights, final double threshold){
		this.weights = new HashMap<>(weights);
		this.threshold = threshold;
	}


	/**
	 * Computes the match probability between two records using
	 * the field similarities and weights.
	 *
	 * @param r1 first record
	 * @param r2 second record
	 * @return the match score (0..1)
	 */
	public double computeScore(final FLEFRecord r1, final FLEFRecord r2){
		final double nameSim = SimilarityMetrics.computeNameSimilarity(r1, r2);
		final double placeSim = SimilarityMetrics.computePlaceSimilarity(r1, r2);
		final double dateSim = SimilarityMetrics.computeDateSimilarity(r1, r2);
		final double structSim = SimilarityMetrics.structuralSimilarity(r1, r2);

		final double wName = weights.getOrDefault("name", 0.35);
		final double wPlace = weights.getOrDefault("place", 0.20);
		final double wDate = weights.getOrDefault("date", 0.15);
		final double wStruct = weights.getOrDefault("structural", 0.30);

		return nameSim * wName +
			placeSim * wPlace +
			dateSim * wDate +
			structSim * wStruct;
	}

	/**
	 * Returns whether the score indicates a match (above threshold).
	 */
	public boolean isMatch(final FLEFRecord r1, final FLEFRecord r2){
		return (computeScore(r1, r2) >= threshold);
	}

	/**
	 * Returns the default scorer with sensible weights for FLEF records.
	 */
	public static FellegiSuenterScorer defaultScorer(){
		final Map<String, Double> weights = new HashMap<>();
		weights.put("name", 0.35);
		weights.put("place", 0.20);
		weights.put("date", 0.15);
		weights.put("structural", 0.30);
		return new FellegiSuenterScorer(weights, 0.75);
	}

}
