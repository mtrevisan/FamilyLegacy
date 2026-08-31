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
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Complete FLEF merger with blocking, Fellegi‑Sünther scoring, clustering, and trust‑based merging.
 * Supports both:
 * <ul>
 *   <li>{@link #merge(FLEFModel, FLEFModel)} – merge two distinct models</li>
 *   <li>{@link #deduplicate(FLEFModel)} – deduplicate records inside a single model</li>
 * </ul>
 */
public class FLEFMerger{

	private static final String TAG_PIPE = "|";


	private final FellegiSuenterScorer scorer;
	private final double autoThreshold;
	private final double reviewThreshold;


	/**
	 * Constructs a merger with a custom scorer and thresholds.
	 *
	 * @param scorer          the Fellegi‑Sünther scorer
	 * @param autoThreshold   score above which automatic merge is performed
	 * @param reviewThreshold score between reviewThreshold and autoThreshold triggers manual review
	 */
	public FLEFMerger(final FellegiSuenterScorer scorer, final double autoThreshold, final double reviewThreshold){
		this.scorer = scorer;
		this.autoThreshold = autoThreshold;
		this.reviewThreshold = reviewThreshold;
	}


	/**
	 * Returns a merger with default settings.
	 */
	public static FLEFMerger defaultMerger(){
		return new FLEFMerger(FellegiSuenterScorer.defaultScorer(), 0.85, 0.60);
	}

	/**
	 * Merges two FLEF models into one, resolving duplicate entities.
	 *
	 * @param model1 first model
	 * @param model2 second model
	 * @return a MergeReport containing the merged model and all decisions
	 */
	public MergeReport merge(final FLEFModel model1, final FLEFModel model2){
		if(model1 == null && model2 == null) return null;
		if(model1 == null) return singleModelResult(model2);
		if(model2 == null) return singleModelResult(model1);

		List<FLEFRecord> allRecords = new ArrayList<>();
		allRecords.addAll(model1.getRecords());
		allRecords.addAll(model2.getRecords());

		return processRecords(allRecords, model1.getHeader(), model2.getHeader());
	}

	// ------------------------------------------------------------------------
	// Deduplicate a single model
	// ------------------------------------------------------------------------

	/**
	 * Deduplicates records inside a single model.
	 *
	 * @param model the model to deduplicate
	 * @return a MergeReport with the deduplicated model and decisions
	 */
	public MergeReport deduplicate(FLEFModel model){
		if(model == null)
			return null;

		if(model.getRecords().isEmpty())
			return new MergeReport(model, Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());

		final List<FLEFRecord> allRecords = new ArrayList<>(model.getRecords());
		return processRecords(allRecords, model.getHeader(), null);
	}

	// ------------------------------------------------------------------------
	// Core processing (shared by merge and deduplicate)
	// ------------------------------------------------------------------------

	// Each cluster is represented by: { leader, members, sumScores, count }
	// We'll use a custom class to hold the cluster data.
	static class ClusterData{
		final FLEFRecord leader;
		final Set<FLEFRecord> members = new HashSet<>();
		double sumScores;
		int count; // number of members (excluding the leader? we'll include leader with score 1.0?)

		ClusterData(final FLEFRecord leader){
			this.leader = leader;

			members.add(leader);
			// leader has perfect similarity with itself (1.0)
			sumScores = 1.;
			count = 1;
		}

		double getAverageScore(){
			return (count > 0? sumScores / count: 0.);
		}

		void addMember(final FLEFRecord record, final double score){
			members.add(record);
			sumScores += score;
			count ++;
		}
	}

	/**
	 * Processes a list of records (from one or two models) by blocking, scoring, clustering and merging.
	 * Optimized for large datasets using caching and leader‑based clustering.
	 *
	 * @param allRecords all records to process
	 * @param header1    header from the first model (or null)
	 * @param header2    header from the second model (or null)
	 * @return the final MergeReport
	 */
	private MergeReport processRecords(final List<FLEFRecord> allRecords, final FLEFRecord header1,
			final FLEFRecord header2){
		// Global cache for similarity scores
		final Map<String, Double> simCache = new HashMap<>();

		// Step 1: Blocking
		final Map<String, List<FLEFRecord>> blocks = blockRecords(allRecords);

		// Step 2: Leader‑based clustering with incremental average
		final List<ClusterData> clusters = new ArrayList<>();
		for(final List<FLEFRecord> block : blocks.values()){
			if(block.isEmpty())
				continue;

			if(block.size() == 1){
				// single record cluster
				clusters.add(new ClusterData(block.getFirst()));

				continue;
			}

			// For each block, we'll process records one by one
			for(final FLEFRecord rec : block){
				// Check against existing clusters
				boolean placed = false;
				// Try to place in an existing cluster
				for(final ClusterData cluster : clusters){
					// Compare with the cluster's leader (cached)
					final double score = getCachedScore(rec, cluster.leader, simCache);
					if(score >= autoThreshold){
						cluster.addMember(rec, score);
						placed = true;

						break;
					}
				}
				if(!placed)
					// Create a new cluster with this record as leader
					clusters.add(new ClusterData(rec));
			}
		}

		// Step 3: Process each cluster (merge, review, reject)
		final List<MergeReport.Decision> decisions = new ArrayList<>();
		final Map<String, String> idMapping = new LinkedHashMap<>();
		final Map<String, Set<String>> idToCluster = new LinkedHashMap<>();
		final List<FLEFRecord> mergedRecords = new ArrayList<>();

		for(final ClusterData clusterData : clusters){
			final Set<FLEFRecord> members = clusterData.members;
			if(members.isEmpty())
				continue;

			final List<FLEFRecord> list = new ArrayList<>(members);
			if(list.size() == 1){
				mergedRecords.add(deepCopy(list.getFirst()));

				continue;
			}

			// Use the incremental average (which is the average of scores against the leader)
			// This approximates the pairwise average and is much faster.
			final double avgScore = clusterData.getAverageScore();

			// Decision logic
			if(avgScore >= autoThreshold){
				// Auto-merge
				final Map<FLEFRecord, Double> trustScores = new HashMap<>();
				for(final FLEFRecord r : list)
					trustScores.put(r, TrustScorer.score(r));
				final FLEFRecord merged = RecordClusterMerger.merge(list, trustScores);
				final String chosenId = list.stream()
					.filter(r -> r.getId() != null)
					.findFirst()
					.map(FLEFRecord::getId)
					.orElse(null);
				if(chosenId != null){
					merged.setId(chosenId);
					for(final FLEFRecord r : list)
						if(r.getId() != null && !r.getId().equals(chosenId))
							idMapping.put(r.getId(), chosenId);
				}
				mergedRecords.add(merged);
				// record decision
				final Set<String> ids = list.stream()
					.map(FLEFRecord::getId)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
				decisions.add(new MergeReport.Decision(
					MergeReport.DecisionType.AUTO_MERGED,
					ids,
					chosenId,
					avgScore,
					Collections.emptyList(),
					"Auto-merged " + list.size() + " records (score=" + avgScore + ")"));
			}
			else if(avgScore >= reviewThreshold){
				// Manual review: keep separate
				for(final FLEFRecord rec : list)
					mergedRecords.add(deepCopy(rec));
				final Set<String> ids = list.stream()
					.map(FLEFRecord::getId)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
				decisions.add(new MergeReport.Decision(
					MergeReport.DecisionType.MANUAL_REVIEW,
					ids,
					null,
					avgScore,
					Collections.singletonList("Moderate similarity; manual review recommended"),
					"Manual review needed (score=" + avgScore + ")"));
			}
			else{
				// Reject: keep separate
				for(final FLEFRecord rec : list)
					mergedRecords.add(deepCopy(rec));
				final Set<String> ids = list.stream()
					.map(FLEFRecord::getId)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
				decisions.add(new MergeReport.Decision(
					MergeReport.DecisionType.REJECTED,
					ids,
					null,
					avgScore,
					Collections.emptyList(),
					"Low similarity, kept separate (score=" + avgScore + ")"));
			}
		}

		// Step 4: Build merged model
		final FLEFModel mergedModel = new FLEFModel();
		// Merge headers if both are provided
		final FLEFRecord mergedHeader = mergeHeaders(header1, header2);
		if(mergedHeader != null)
			mergedModel.setHeader(mergedHeader);
		else if(header1 != null)
			mergedModel.setHeader(deepCopy(header1));
		else if(header2 != null)
			mergedModel.setHeader(deepCopy(header2));
		for(final FLEFRecord rec : mergedRecords)
			mergedModel.addRecord(rec);

		// Step 5: Update references
		if(!idMapping.isEmpty())
			updateReferences(mergedModel, idMapping);

		// Step 6: Build idToCluster map
		for(final ClusterData clusterData : clusters){
			final Set<String> ids = clusterData.members.stream()
				.map(FLEFRecord::getId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
			for(final String id : ids)
				idToCluster.put(id, ids);
		}

		return new MergeReport(mergedModel, decisions, idMapping, idToCluster);
	}

	private double getCachedScore(final FLEFRecord a, final FLEFRecord b, final Map<String, Double> cache){
		// Use a stable key: e.g., concatenation of record IDs or hashes
		// If IDs are null, fallback to system identity hash codes
		final String key = getPairKey(a, b);
		return cache.computeIfAbsent(key, k -> scorer.computeScore(a, b));
	}

	private static String getPairKey(final FLEFRecord a, final FLEFRecord b){
		final String id1 = (a.getId() != null? a.getId(): Integer.toHexString(System.identityHashCode(a)));
		final String id2 = (b.getId() != null? b.getId(): Integer.toHexString(System.identityHashCode(b)));
		// Ensure order‑independent key
		return (id1.compareTo(id2) <= 0? id1 + TAG_PIPE + id2: id2 + TAG_PIPE + id1);
	}

	// ---------- Helpers ----------

	private Map<String, List<FLEFRecord>> blockRecords(final List<FLEFRecord> records){
		final Map<String, List<FLEFRecord>> blocks = new LinkedHashMap<>();
		for(final FLEFRecord rec : records){
			final String key = blockingKey(rec);
			blocks.computeIfAbsent(key, k -> new ArrayList<>())
				.add(rec);
		}
		return blocks;
	}

	private String blockingKey(final FLEFRecord rec){
		// Use first letter of given name + first 3 of family name + birth year
		final String given = SimilarityMetrics.extractNamePart(rec, "given");
		final String family = SimilarityMetrics.extractNamePart(rec, "family");
		final String year = extractBirthYear(rec);
		String key = "";
		if(!given.isEmpty())
			key += given.charAt(0);
		if(!family.isEmpty())
			key += family.substring(0, Math.min(3, family.length()));
		if(year != null && year.length() >= 4)
			key += year.substring(0, 4);
		if(key.isEmpty())
			key = "UNKNOWN_" + rec.getTag();
		return key.toLowerCase();
	}

	private String extractBirthYear(final FLEFRecord rec){
		// search for event of type birth and extract date year
		for(FLEFRecord child : rec.getChildren())
			if("event".equalsIgnoreCase(child.getTag())){
				final String type = FLEFRecordHelper.getChildValue(child, "type");
				if("birth".equalsIgnoreCase(type)){
					final FLEFRecord date = FLEFRecordHelper.findChild(child, "date");
					if(date != null){
						final String val = FLEFRecordHelper.getChildValue(date, "value.point.full_date.value");
						if(val != null && val.matches("\\d{4}-\\d{2}-\\d{2}"))
							return val.substring(0, 4);

						if(val != null && val.matches("\\d{4}"))
							return val;
					}
				}
			}
		return null;
	}

	private MergeReport singleModelResult(final FLEFModel model){
		final List<MergeReport.Decision> decisions = new ArrayList<>();
		for(final FLEFRecord rec : model.getRecords()){
			decisions.add(new MergeReport.Decision(
				MergeReport.DecisionType.KEPT_AS_IS,
				rec.getId() != null? Set.of(rec.getId()): Set.of(),
				rec.getId(),
				1.0,
				Collections.emptyList(),
				"Single model, kept as is"));
		}
		return new MergeReport(model, decisions, Collections.emptyMap(), Collections.emptyMap());
	}

	private FLEFRecord deepCopy(final FLEFRecord rec){
		if(rec == null)
			return null;

		final FLEFRecord copy = FLEFRecord.createChildWithTag(rec.getTag());
		rec.deepCopyTo(copy);
		return copy;
	}

	private FLEFRecord mergeHeaders(final FLEFRecord h1, final FLEFRecord h2){
		if(h1 == null && h2 == null)
			return null;
		if(h1 == null)
			return deepCopy(h2);
		if(h2 == null)
			return deepCopy(h1);

		final FLEFRecord merged = FLEFRecord.createChildWithTag("header");
		for(final FLEFRecord child : h1.getChildren())
			merged.addChild(deepCopy(child));
		for(final FLEFRecord child : h2.getChildren()){
			final String tag = child.getTag();
			if("contact".equalsIgnoreCase(tag))
				merged.addChild(deepCopy(child));
			else{
				FLEFRecordHelper.removeChildren(merged, tag);
				merged.addChild(deepCopy(child));
			}
		}
		return merged;
	}

	private void updateReferences(final FLEFModel model, final Map<String, String> mapping){
		final Deque<FLEFRecord> stack = new ArrayDeque<>();
		if(model.getHeader() != null)
			stack.push(model.getHeader());
		for(final FLEFRecord rec : model.getRecords())
			stack.push(rec);
		while(!stack.isEmpty()){
			final FLEFRecord rec = stack.pop();
			final String id = rec.getValue();
			if(id != null && model.hasRecord(id)){
				final String newId = mapping.get(id);
				if(newId != null && !newId.equals(id))
					rec.setValue(newId);
			}
			for(final FLEFRecord child : rec.getChildren())
				stack.push(child);
		}
	}


//	public static void main(final String[] args){
//		// ----- Merging two models -----
//		FLEFMerger merger = FLEFMerger.defaultMerger();
//		MergeReport report = merger.merge(modelA, modelB);
//		FLEFModel merged = report.getMergedModel();
//
//		// Show auto‑merged decisions
//		report.getDecisions().stream()
//			.filter(d -> d.getType() == MergeReport.DecisionType.AUTO_MERGED)
//			.forEach(System.out::println);
//
//		// List clusters needing manual review
//		report.getDecisionsNeedingReview().forEach(d -> {
//			System.out.println("Review these IDs: " + d.getRecordIds());
//		});
//
//		// ----- Deduplicating a single model -----
//		MergeReport dedupReport = merger.deduplicate(singleModel);
//		FLEFModel dedupModel = dedupReport.getMergedModel();
//	}

}

