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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Outcome of a merge or of a deduplication: the resulting model and every decision taken to build it.
 *
 * @param mergedModel the resulting model; the input models are never modified
 * @param decisions   the decisions, in the order they were taken
 * @param idMapping   for {@link FLEFMerger#merge(FLEFModel, FLEFModel) merge}: every id of the incoming model to the id of the record
 *                    that represents it in the merged model (records deleted by the merge are missing); for
 *                    {@link FLEFMerger#deduplicate(FLEFModel) deduplicate}: every absorbed id to the id of its survivor
 * @param warnings    situations that the merge left for the researcher to resolve (e.g. competing single-valued assertions)
 */
public record MergeReport(FLEFModel mergedModel, List<Decision> decisions, Map<String, String> idMapping, List<String> warnings){

	public enum DecisionType{
		/** Two records judged to describe the same entity were merged (see Merge in the protocol). */
		AUTO_MERGED,
		/** Two association records stating the same assertion were collapsed into one, uniting their evidence. */
		DUPLICATE_COLLAPSED,
		/** Two records may describe the same entity: an IdentityHypothesisRecord was created for the researcher to evaluate. */
		HYPOTHESIS_CREATED,
		/** A record was deleted by the cascade that follows a merge (see Deletion in the protocol). */
		DELETED
	}


	/**
	 * A single decision.
	 * <p>
	 * Ids of records that no longer exist (absorbed or deleted records) are given as they were in their input model.
	 *
	 * @param type        the kind of decision
	 * @param kind        the record kind involved (e.g. {@code individual})
	 * @param recordId    the surviving record (merges), the created hypothesis (hypotheses), or the deleted record (deletions)
	 * @param relatedIds  the absorbed record (merges), the two candidate records in the merged model (hypotheses), none otherwise
	 * @param probability the match probability, {@link Double#NaN} when not applicable
	 * @param rationale   the evidence that justified the decision
	 * @param conflicts   values of the absorbed record that could not be kept in the survivor
	 */
	public record Decision(DecisionType type, String kind, String recordId, List<String> relatedIds, double probability,
			String rationale, List<String> conflicts){

		public Decision{
			relatedIds = List.copyOf(relatedIds);
			conflicts = List.copyOf(conflicts);
		}

	}


	public MergeReport{
		decisions = List.copyOf(decisions);
		idMapping = Collections.unmodifiableMap(new LinkedHashMap<>(idMapping));
		warnings = List.copyOf(warnings);
	}


	public List<Decision> decisions(final DecisionType type){
		return decisions.stream()
			.filter(decision -> decision.type() == type)
			.toList();
	}

	/**
	 * Decisions that require the researcher: the identity hypotheses to evaluate.
	 */
	public List<Decision> decisionsNeedingReview(){
		return decisions(DecisionType.HYPOTHESIS_CREATED);
	}

}
