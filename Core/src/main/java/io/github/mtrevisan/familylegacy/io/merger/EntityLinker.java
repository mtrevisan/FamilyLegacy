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

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Finds the records of one kind that describe the same entity, and merges them.
 * <p>
 * Records are compared only within blocks (see {@link EntityMatcher#blockingKeys(FLEFRecord)}). Pairs above the automatic threshold
 * are clustered, strongest first; pairs between the review and the automatic threshold, and the strong pairs that could not be
 * clustered, become candidates for an IdentityHypothesisRecord.
 * <p>
 * Clustering never chains weak links: two clusters are joined only if every pair across them is at least a review candidate and
 * none is excluded. When merging two models, a cluster holds at most one record of each model: duplicates inside the base are not
 * the purpose of a merge, and are reported as hypotheses instead.
 */
final class EntityLinker{

	/** Blocks larger than this are not compared, to keep the comparison from becoming quadratic in the size of the file. */
	private static final int MAX_BLOCK_SIZE = 1_000;


	/**
	 * A pair worth a decision.
	 */
	record Candidate(String kind, String idA, String idB, MatchScore score){}


	private final MergeSession session;
	private final double autoThreshold;
	private final double reviewThreshold;

	/** By pair: a later round replaces the candidate of an earlier one, scored on less merged records. */
	private final Map<String, Candidate> reviewCandidates = new LinkedHashMap<>();


	EntityLinker(final MergeSession session, final double autoThreshold, final double reviewThreshold){
		this.session = session;
		this.autoThreshold = autoThreshold;
		this.reviewThreshold = reviewThreshold;
	}


	List<Candidate> reviewCandidates(){
		return new ArrayList<>(reviewCandidates.values());
	}

	/**
	 * @return the number of merges
	 */
	int link(final String kind){
		final List<FLEFRecord> records = session.records(kind);
		if(records.size() < 2)
			return 0;

		final ModelIndex index = new ModelIndex(session.records(), session.references);
		final EntityMatcher matcher = new EntityMatcher(index, session.signature, session.isMergeMode());
		final Map<String, MatchScore> scores = new HashMap<>();

		final List<Candidate> candidates = findCandidates(kind, records, matcher, scores);
		candidates.sort(Comparator.comparingDouble((Candidate candidate) -> -candidate.score().probability())
			.thenComparingInt(candidate -> session.fileOrder(candidate.idA()))
			.thenComparingInt(candidate -> session.fileOrder(candidate.idB())));

		final Map<String, List<String>> clusterOf = new HashMap<>();
		for(final Candidate candidate : candidates){
			if(candidate.score().probability() < autoThreshold){
				reviewCandidates.put(pairKey(candidate.idA(), candidate.idB()), candidate);
				continue;
			}

			final List<String> clusterA = clusterOf.computeIfAbsent(candidate.idA(), id -> new ArrayList<>(List.of(id)));
			final List<String> clusterB = clusterOf.computeIfAbsent(candidate.idB(), id -> new ArrayList<>(List.of(id)));
			if(clusterA == clusterB)
				continue;

			if(canJoin(clusterA, clusterB, matcher, scores)){
				clusterA.addAll(clusterB);
				for(final String id : clusterB)
					clusterOf.put(id, clusterA);
			}
			else
				reviewCandidates.put(pairKey(candidate.idA(), candidate.idB()), candidate);
		}

		return mergeClusters(kind, clusterOf, index, matcher, scores);
	}

	private List<Candidate> findCandidates(final String kind, final List<FLEFRecord> records, final EntityMatcher matcher,
			final Map<String, MatchScore> scores){
		final Map<String, List<FLEFRecord>> blocks = new LinkedHashMap<>();
		for(final FLEFRecord record : records)
			for(final String key : matcher.blockingKeys(record))
				blocks.computeIfAbsent(key, k -> new ArrayList<>())
					.add(record);

		final List<Candidate> candidates = new ArrayList<>();
		final Set<String> compared = new HashSet<>();
		for(final Map.Entry<String, List<FLEFRecord>> block : blocks.entrySet()){
			final List<FLEFRecord> members = block.getValue();
			if(members.size() > MAX_BLOCK_SIZE){
				session.warn("Too many " + kind + " records share the blocking key '" + block.getKey() + "' (" + members.size()
					+ "): they were not compared with each other");
				continue;
			}

			for(int i = 0, size = members.size(); i < size; i ++)
				for(int j = i + 1; j < size; j ++){
					final FLEFRecord a = members.get(i);
					final FLEFRecord b = members.get(j);
					// a merge aligns the two models: records of the same model are not compared with each other
					if(session.isMergeMode() && session.isIncoming(a.getId()) == session.isIncoming(b.getId()))
						continue;
					if(!compared.add(pairKey(a.getId(), b.getId())))
						continue;

					final MatchScore score = score(a.getId(), b.getId(), matcher, scores);
					if(!score.isVetoed() && score.probability() >= reviewThreshold)
						candidates.add(new Candidate(kind, a.getId(), b.getId(), score));
				}
		}
		return candidates;
	}

	private boolean canJoin(final List<String> clusterA, final List<String> clusterB, final EntityMatcher matcher,
			final Map<String, MatchScore> scores){
		if(session.isMergeMode()){
			int incoming = 0;
			for(final String id : clusterA)
				if(session.isIncoming(id))
					incoming ++;
			for(final String id : clusterB)
				if(session.isIncoming(id))
					incoming ++;
			final int base = clusterA.size() + clusterB.size() - incoming;
			return (incoming <= 1 && base <= 1);
		}

		for(final String idA : clusterA)
			for(final String idB : clusterB){
				final MatchScore score = score(idA, idB, matcher, scores);
				if(score.isVetoed() || score.probability() < reviewThreshold)
					return false;
			}
		return true;
	}

	private int mergeClusters(final String kind, final Map<String, List<String>> clusterOf, final ModelIndex index,
			final EntityMatcher matcher, final Map<String, MatchScore> scores){
		// the scores are taken before any field is merged, so that every merge is justified by the records as they were
		final List<String[]> merges = new ArrayList<>();
		final Set<List<String>> done = Collections.newSetFromMap(new IdentityHashMap<>());
		for(final List<String> cluster : clusterOf.values()){
			if(cluster.size() < 2 || !done.add(cluster))
				continue;

			cluster.sort(Comparator.comparingInt(session::fileOrder));
			final String survivorId = chooseSurvivor(cluster, index);
			for(final String memberId : cluster)
				if(!memberId.equals(survivorId)){
					merges.add(new String[]{survivorId, memberId});
					// cached for the audit comment
					score(survivorId, memberId, matcher, scores);
				}
		}
		merges.sort(Comparator.comparingInt((String[] merge) -> session.fileOrder(merge[0]))
			.thenComparingInt(merge -> session.fileOrder(merge[1])));

		final Map<String, String> absorbed = new LinkedHashMap<>();
		for(int i = 0, size = merges.size(); i < size; i ++){
			final String survivorId = merges.get(i)[0];
			final String absorbedId = merges.get(i)[1];
			final MatchScore score = score(survivorId, absorbedId, matcher, scores);
			final FLEFRecord survivor = session.record(survivorId);

			final List<String> conflicts = new ArrayList<>();
			session.fields.mergeRecord(survivor, session.record(absorbedId), conflicts);
			FieldMerger.addAuditUpdate(survivor, session.now, "Merged " + session.label(absorbedId)
				+ String.format(Locale.ROOT, " (match probability %.3f: %s)", score.probability(), score.describe()));
			session.decide(new MergeReport.Decision(MergeReport.DecisionType.AUTO_MERGED, kind, survivorId,
				List.of(session.inputId(absorbedId)), score.probability(), score.describe(), conflicts));
			absorbed.put(absorbedId, survivorId);
		}
		session.absorb(absorbed);
		return absorbed.size();
	}

	/**
	 * When merging two models the base record survives, so that the ids of the base never change. When deduplicating, the
	 * survivor is the most referenced record (fewer references to redirect), then the oldest, then the first in the file.
	 */
	private String chooseSurvivor(final List<String> cluster, final ModelIndex index){
		if(session.isMergeMode()){
			for(final String id : cluster)
				if(!session.isIncoming(id))
					return id;
			return cluster.getFirst();
		}

		return cluster.stream()
			.min(Comparator.comparingInt((String id) -> -index.referenceCount(id))
				.thenComparing(id -> creationDate(session.record(id)))
				.thenComparingInt(session::fileOrder))
			.orElseThrow();
	}

	private static String creationDate(final FLEFRecord record){
		final FLEFRecord audit = FieldMerger.first(record, "audit");
		final FLEFRecord creation = (audit != null? FieldMerger.first(audit, "creation"): null);
		final String date = (creation != null? ModelIndex.value(creation, "date"): null);
		// a record without creation date sorts last
		return (date != null? date: "~");
	}

	private MatchScore score(final String idA, final String idB, final EntityMatcher matcher, final Map<String, MatchScore> scores){
		return scores.computeIfAbsent(pairKey(idA, idB),
			key -> matcher.compare(session.record(idA), session.record(idB)));
	}

	private static String pairKey(final String idA, final String idB){
		return (idA.compareTo(idB) <= 0? idA + '\u0000' + idB: idB + '\u0000' + idA);
	}

}
