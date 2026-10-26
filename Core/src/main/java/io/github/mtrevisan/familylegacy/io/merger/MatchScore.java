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

import java.util.ArrayList;
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
 * @param evidence    the features that contributed, with their weight
 * @param veto        why the pair cannot match, or {@code null}
 */
record MatchScore(double weight, double probability, List<String> evidence, String veto){

	boolean isVetoed(){
		return (veto != null);
	}

	String describe(){
		final String features = String.join("; ", evidence);
		return (isVetoed()? "excluded: " + veto + (features.isEmpty()? "": " (" + features + ")"): features);
	}


	/**
	 * Accumulates the features of a comparison.
	 */
	static final class Builder{

		private final double priorOffset;
		private double weight;
		private final List<String> evidence = new ArrayList<>();
		private String veto;


		/**
		 * @param priorOffset the weight at which a pair of this kind has even odds of being a match
		 */
		Builder(final double priorOffset){
			this.priorOffset = priorOffset;
		}


		Builder add(final double featureWeight, final String feature){
			weight += featureWeight;
			evidence.add(String.format(Locale.ROOT, "%s (%+.1f)", feature, featureWeight));
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
			return new MatchScore(weight, probability, List.copyOf(evidence), veto);
		}

	}

}
