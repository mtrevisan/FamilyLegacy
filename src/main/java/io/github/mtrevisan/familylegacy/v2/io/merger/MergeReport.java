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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Detailed report of a merge or deduplication operation.
 */
public class MergeReport{

	public enum DecisionType{
		AUTO_MERGED,      // automatically merged into a single record
		MANUAL_REVIEW,    // similarity is moderate; user should decide
		REJECTED,         // similarity too low; kept separate
		KEPT_AS_IS        // no conflict or single record
	}


	/**
	 * A single decision made for a group of records.
	 */
	public static class Decision{
		private final DecisionType type;
		private final Set<String> recordIds;
		private final String mergedId;
		private final double score;
		private final List<String> conflicts;
		private final String reason;

		public Decision(final DecisionType type, final Set<String> recordIds, final String mergedId, final double score,
				final List<String> conflicts, final String reason){
			this.type = type;
			this.recordIds = Collections.unmodifiableSet(recordIds);
			this.mergedId = mergedId;
			this.score = score;
			this.conflicts = (conflicts != null? Collections.unmodifiableList(conflicts): List.of());
			this.reason = reason != null? reason: "";
		}

		public DecisionType getType(){
			return type;
		}

		public Set<String> getRecordIds(){
			return recordIds;
		}

		public String getMergedId(){
			return mergedId;
		}

		public double getScore(){
			return score;
		}

		public List<String> getConflicts(){
			return conflicts;
		}

		public String getReason(){
			return reason;
		}
	}


	private final FLEFModel mergedModel;
	private final List<Decision> decisions;
	private final Map<String, String> idMapping;
	private final Map<String, Set<String>> idToCluster;


	public MergeReport(final FLEFModel mergedModel, final List<Decision> decisions, final Map<String, String> idMapping,
			final Map<String, Set<String>> idToCluster){
		this.mergedModel = mergedModel;
		this.decisions = Collections.unmodifiableList(decisions);
		this.idMapping = Collections.unmodifiableMap(idMapping);
		this.idToCluster = Collections.unmodifiableMap(idToCluster);
	}


	public FLEFModel getMergedModel(){
		return mergedModel;
	}

	public List<Decision> getDecisions(){
		return decisions;
	}

	public Map<String, String> getIdMapping(){
		return idMapping;
	}

	public Map<String, Set<String>> getIdToCluster(){
		return idToCluster;
	}

	/**
	 * Returns decisions that require human review (MANUAL_REVIEW type).
	 */
	public List<Decision> getDecisionsNeedingReview(){
		return decisions.stream()
			.filter(d -> d.getType() == DecisionType.MANUAL_REVIEW)
			.toList();
	}

}
