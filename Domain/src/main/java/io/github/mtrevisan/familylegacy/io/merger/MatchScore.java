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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


/**
 * The outcome of comparing two records, in the Fellegi-Sunter model.
 * <p>
 * Every compared feature adds its weight, the base-2 logarithm of how much more often that outcome is seen between two records of
 * the same entity than between two records of different entities: an agreement adds a positive weight, a disagreement a negative
 * one, and a feature that is missing on either side adds nothing (missing data is neither evidence for nor against the identity).
 * The total weight, shifted by the prior of the record kind, becomes a probability through the logistic function.
 * <p>
 * A veto marks a pair that cannot be the same entity whatever the other features say (e.g. opposite sexes, or a birth after the
 * other's death).
 *
 * @param weight      the total weight
 * @param probability the match probability, {@code 0} when vetoed
 * @param features    the features that contributed
 * @param weights     the weight of each feature (the text of the evidence is built only when it is asked for: a comparison is made
 *                    for every pair of records of a block, and all of them are kept)
 * @param veto        why the pair cannot match, or {@code null}
 */
record MatchScore(double weight, double probability, String[] features, double[] weights, String veto){

	boolean isVetoed(){
		return (veto != null);
	}

	/** The features that contributed, each with its weight. */
	List<String> evidence(){
		final List<String> evidence = new ArrayList<>(features.length);
		for(int i = 0; i < features.length; i ++)
			evidence.add(String.format(Locale.ROOT, "%s (%+.1f)", features[i], weights[i]));
		return evidence;
	}

	/**
	 * @param agreements	Whether to give the features that speak for the identity (a weight of zero or more) or those against it.
	 * @return	The features of one kind, each with its weight.
	 */
	List<String> evidence(final boolean agreements){
		final List<String> evidence = new ArrayList<>(features.length);
		for(int i = 0; i < features.length; i ++)
			if((weights[i] >= 0.) == agreements)
				evidence.add(String.format(Locale.ROOT, "%s (%+.1f)", features[i], weights[i]));
		return evidence;
	}

	String describe(){
		final String evidence = String.join("; ", evidence());
		return (isVetoed()? "excluded: " + veto + (evidence.isEmpty()? StringUtils.EMPTY: " (" + evidence + ")"): evidence);
	}


	/**
	 * Accumulates the features of a comparison.
	 */
	static final class Builder{

		private final double priorOffset;
		private double weight;
		private String[] features = new String[6];
		private double[] weights = new double[6];
		private int count;
		private String veto;


		/**
		 * @param priorOffset the weight at which a pair of this kind has even odds of being a match
		 */
		Builder(final double priorOffset){
			this.priorOffset = priorOffset;
		}


		Builder add(final double featureWeight, final String feature){
			weight += featureWeight;
			if(count == features.length){
				features = Arrays.copyOf(features, count * 2);
				weights = Arrays.copyOf(weights, count * 2);
			}
			features[count] = feature;
			weights[count ++] = featureWeight;
			return this;
		}

		Builder veto(final String reason){
			if(veto == null)
				veto = reason;
			return this;
		}

		boolean isVetoed(){
			return (veto != null);
		}

		MatchScore build(){
			final double probability = (veto != null? 0.: 1. / (1. + Math.pow(2., priorOffset - weight)));
			return new MatchScore(weight, probability, Arrays.copyOf(features, count), Arrays.copyOf(weights, count), veto);
		}

	}

}
