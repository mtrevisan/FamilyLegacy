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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


/**
 * The last steps of a merge: the identity hypotheses the merge accepted are deleted, the uncertain pairs become identity hypotheses
 * for the researcher, and the competing assertions the merge brought together are reported.
 */
final class ProtocolCleanup{

	private static final String HYPOTHESIS_ID_TEMPLATE = "IH";
	private static final String TAG_RATIONALE = "rationale";
	private static final String TAG_CONFIDENCE = "confidence";
	private static final String TAG_RESOLVES = "resolves";
	private static final String TAG_PREFERRED = "preferred";
	private static final Set<String> BIOLOGICAL_PARENT_TYPES = Set.of("child", "biological_child");
	private static final int MAX_BIOLOGICAL_PARENTS = 2;


	private final MergeSession session;
	private final ProtocolRules rules;
	private final double reviewThreshold;


	ProtocolCleanup(final MergeSession session, final ProtocolRules rules, final double reviewThreshold){
		this.session = session;
		this.rules = rules;
		this.reviewThreshold = reviewThreshold;
	}


	void run(final List<EntityLinker.Candidate> reviewCandidates){
		for(final FLEFRecord deleted : rules.deleteAcceptedHypotheses(session)){
			final String reason = (ProtocolRules.KIND_IDENTITY_HYPOTHESIS.equals(deleted.getTag())
				? "identity hypothesis accepted by the merge"
				: "depended on a deleted record");
			session.decide(new MergeReport.Decision(MergeReport.DecisionType.DELETED, deleted.getTag(),
				session.inputId(deleted.getId()), List.of(), Double.NaN, reason, List.of()));
		}
		rules.collapseDuplicateHypotheses(session, session.now)
			.forEach((absorbed, survivor) -> session.decide(new MergeReport.Decision(MergeReport.DecisionType.DUPLICATE_COLLAPSED,
				ProtocolRules.KIND_IDENTITY_HYPOTHESIS, survivor, List.of(session.inputId(absorbed)), Double.NaN,
				"same pair of candidates", List.of())));

		createHypotheses(reviewCandidates);
		warnCompetingAssertions();
		warnSelfRelationships();
	}

	/**
	 * Uncertain pairs are recorded as IdentityHypothesisRecords, the protocol's way of saying that two records may describe the same
	 * entity: the researcher accepts one by merging, or evaluates it with a ConclusionRecord.
	 */
	private void createHypotheses(final List<EntityLinker.Candidate> candidates){
		final Set<String> pairs = new HashSet<>();
		for(final FLEFRecord record : session.records(ProtocolRules.KIND_IDENTITY_HYPOTHESIS)){
			final String pair = ProtocolRules.unorderedPair(ProtocolRules.identities(record));
			if(pair != null)
				pairs.add(pair);
		}

		EntityMatcher matcher = null;
		for(final EntityLinker.Candidate candidate : candidates){
			final String idA = session.resolve(candidate.idA());
			final String idB = session.resolve(candidate.idB());
			if(idA == null || idB == null || idA.equals(idB))
				continue;
			// a merge does not judge the duplicates already inside the base: that is what deduplication is for
			if(session.isMergeMode() && !session.isIncoming(idA) && !session.isIncoming(idB))
				continue;
			if(pairs.contains(ProtocolRules.unorderedPair(List.of(idA, idB))))
				continue;

			// a candidate absorbed into another record is another pair: its score is taken again, on the records as merged
			MatchScore score = candidate.score();
			if(!idA.equals(candidate.idA()) || !idB.equals(candidate.idB())){
				if(matcher == null)
					matcher = new EntityMatcher(new ModelIndex(session.records(), session.references), session.signature,
						session.isMergeMode());
				score = matcher.compare(session.record(idA), session.record(idB));
				if(score.isVetoed() || score.probability() < reviewThreshold)
					continue;
			}
			pairs.add(ProtocolRules.unorderedPair(List.of(idA, idB)));

			final String id = session.newId(HYPOTHESIS_ID_TEMPLATE);
			final FLEFRecord hypothesis = FLEFRecord.createMainRecord(id, ProtocolRules.KIND_IDENTITY_HYPOTHESIS);
			hypothesis.addChild(FLEFRecord.createChildWithTag(ProtocolRules.TAG_IDENTITY)
				.addChild(FLEFRecord.createChildWithTagAndValue(candidate.kind(), idA)));
			hypothesis.addChild(FLEFRecord.createChildWithTag(ProtocolRules.TAG_IDENTITY)
				.addChild(FLEFRecord.createChildWithTagAndValue(candidate.kind(), idB)));
			hypothesis.addChild(FLEFRecord.createChildWithTagAndValue(TAG_RATIONALE,
				String.format(Locale.ROOT, "Proposed by the merge (match probability %.3f): %s", score.probability(),
					score.describe())));
			hypothesis.addChild(FLEFRecord.createChildWithTagAndValue(TAG_CONFIDENCE, confidence(score.probability())));
			hypothesis.addChild(FLEFRecord.createChildWithTag("audit")
				.addChild(FLEFRecord.createChildWithTag("creation")
					.addChild(FLEFRecord.createChildWithTagAndValue("date", session.now))));
			session.addRecord(hypothesis);

			session.decide(new MergeReport.Decision(MergeReport.DecisionType.HYPOTHESIS_CREATED, candidate.kind(), id,
				List.of(idA, idB), score.probability(), score.describe(), List.of()));
		}
	}

	private static String confidence(final double probability){
		if(probability >= 0.85)
			return "high";
		return (probability >= 0.7? "medium": "low");
	}

	/**
	 * BIRTH and DEATH are single-valued, and an individual has at most two biological parents: when a merge brings together more
	 * than that, and no ConclusionRecord resolves them, the researcher has to.
	 */
	private void warnCompetingAssertions(){
		final Set<String> survivors = new LinkedHashSet<>();
		for(final MergeReport.Decision decision : session.decisions())
			if(decision.type() == MergeReport.DecisionType.AUTO_MERGED && ModelIndex.KIND_INDIVIDUAL.equals(decision.kind())){
				final String survivor = session.resolve(decision.recordId());
				if(survivor != null)
					survivors.add(survivor);
			}
		if(survivors.isEmpty())
			return;

		final ModelIndex index = new ModelIndex(session.records(), session.references);
		for(final String individualId : survivors){
			for(final String type : List.of("birth", "death")){
				final List<FLEFRecord> events = index.principalEvents(individualId, Set.of(type));
				if(events.size() < 2)
					continue;

				final Set<String> competing = new LinkedHashSet<>();
				for(final FLEFRecord event : events){
					competing.add(event.getId());
					for(final FLEFRecord participation : index.participationsOfEvent(event.getId()))
						competing.add(participation.getId());
				}
				if(!isResolved(competing))
					session.warn("Individual " + individualId + " has " + events.size() + StringUtils.SPACE + type + " events ("
						+ String.join(", ", events.stream().map(FLEFRecord::getId).toList())
						+ "): competing assertions to resolve with a ConclusionRecord");
			}

			final Set<String> parents = new LinkedHashSet<>();
			final Set<String> relationships = new LinkedHashSet<>();
			for(final FLEFRecord relationship : session.records(ModelIndex.KIND_RELATIONSHIP))
				if(ModelIndex.value(relationship, ModelIndex.TAG_TYPE) != null
						&& BIOLOGICAL_PARENT_TYPES.contains(ModelIndex.value(relationship, ModelIndex.TAG_TYPE))
						&& individualId.equals(ModelIndex.armValue(FieldMerger.first(relationship, "subject")))){
					parents.add(ModelIndex.armValue(FieldMerger.first(relationship, "target")));
					relationships.add(relationship.getId());
				}
			if(parents.size() > MAX_BIOLOGICAL_PARENTS && !isResolved(relationships))
				session.warn("Individual " + individualId + " has " + parents.size() + " possible biological parents ("
					+ String.join(", ", parents) + "): parentage is undetermined until resolved with ConclusionRecords");
		}
	}

	/**
	 * Whether a ConclusionRecord applies to the competing records: its RESOLVES contains at least two of them and its PREFERRED is
	 * one of them.
	 */
	private boolean isResolved(final Set<String> competing){
		for(final FLEFRecord conclusion : session.records(ProtocolRules.KIND_CONCLUSION)){
			int resolved = 0;
			for(final FLEFRecord target : FieldMerger.children(conclusion, TAG_RESOLVES))
				if(competing.contains(ModelIndex.armValue(target)))
					resolved ++;
			if(resolved >= 2 && competing.contains(ModelIndex.armValue(FieldMerger.first(conclusion, TAG_PREFERRED))))
				return true;
		}
		return false;
	}

	private void warnSelfRelationships(){
		final List<FLEFRecord> relationships = new ArrayList<>(session.records(ModelIndex.KIND_RELATIONSHIP));
		relationships.addAll(session.records(ModelIndex.KIND_PLACE_RELATIONSHIP));
		for(final FLEFRecord relationship : relationships){
			final String subject = ModelIndex.armValue(FieldMerger.first(relationship, "subject"));
			if(subject != null && subject.equals(ModelIndex.armValue(FieldMerger.first(relationship, "target"))))
				session.warn(relationship.getTag() + StringUtils.SPACE + relationship.getId() + " now relates " + subject
					+ " to itself: its two ends were merged");
		}
	}

}
