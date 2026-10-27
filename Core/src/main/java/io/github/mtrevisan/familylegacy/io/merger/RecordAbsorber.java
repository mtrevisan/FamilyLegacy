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

import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FieldDefinition;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.StructType;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * Applies to a live model a merge decided by the researcher, following the Merge rule of the protocol.
 * <p>
 * {@link #candidates} proposes the records that may be the same entity as a given one. The caller decides the fields of the
 * survivor (possibly with {@link #mergeFields} or {@link #mergeField}, which apply the same rules as the automatic merge), then
 * calls {@link #absorb}: every reference to the absorbed record, and every {@code flef:} link to it, is redirected to the
 * survivor, the absorbed record is removed, the assertions that became identical are collapsed, the identity hypothesis that
 * proposed the merge is deleted with its cascade, and the survivor gains an {@code audit.update} naming the absorbed record.
 */
public final class RecordAbsorber{

	/**
	 * How a field of a record takes part in a merge.
	 */
	public enum FieldRole{
		/** Repeated field: the occurrences of the two records are united. */
		REPEATED,
		/** Singular field: the record can hold only one of the two values. */
		SINGULAR,
		/** The audit of the survivor, never taken from the absorbed record. */
		AUDIT,
		/** Privacy: the most restrictive of the two is kept. */
		PRIVACY,
		/** A field unknown to the grammar: preserved. */
		UNKNOWN
	}

	/**
	 * @param referencesRedirected how many references and links now point to the survivor
	 * @param collapsedIds         the association records that, after the merge, stated the same assertion as another one and were
	 *                             collapsed into it (their citations and notes are kept)
	 * @param deletedIds           the records deleted by the merge (the accepted hypothesis and its cascade)
	 */
	public record Result(int referencesRedirected, List<String> collapsedIds, List<String> deletedIds){}


	private static final String TAG_RATIONALE = "rationale";
	private static final String TAG_ISSUE = "issue";
	private static final String TAG_PROOF_STATUS = "proof_status";
	private static final String TAG_NARRATIVE = "narrative";
	private static final String TAG_RESOLVES = "resolves";
	private static final String TAG_AUDIT = "audit";
	private static final String TAG_CREATION = "creation";
	private static final String TAG_DATE = "date";
	private static final String ISSUE_IDENTITY = "identity";


	private final GrammarSupport grammar;
	private final ReferenceRewriter references;
	private final RecordSignature signature;
	private final FieldMerger fields;
	private final ProtocolRules rules;
	private final Clock clock;


	public RecordAbsorber(final FLEFGrammar grammar){
		this(grammar, Clock.systemDefaultZone());
	}

	public RecordAbsorber(final FLEFGrammar grammar, final Clock clock){
		this.grammar = new GrammarSupport(grammar);
		references = new ReferenceRewriter(this.grammar);
		signature = new RecordSignature(this.grammar);
		fields = new FieldMerger(this.grammar, signature);
		rules = new ProtocolRules(this.grammar, references, fields);
		this.clock = clock;
	}


	public FieldRole fieldRole(final String kind, final String tag){
		final FieldDefinition field = field(kind, tag);
		if(field == null)
			return FieldRole.UNKNOWN;

		final String typeName = grammar.declaredTypeName(field);
		if(GrammarSupport.TYPE_AUDIT.equals(typeName))
			return FieldRole.AUDIT;
		if(GrammarSupport.TYPE_PRIVACY.equals(typeName))
			return FieldRole.PRIVACY;
		return (GrammarSupport.isRepeated(field)? FieldRole.REPEATED: FieldRole.SINGULAR);
	}

	/**
	 * Whether two lists of occurrences of a field state the same things, regardless of order and of their evidence and audit.
	 */
	public boolean sameContent(final String kind, final String tag, final List<FLEFRecord> occurrencesA,
			final List<FLEFRecord> occurrencesB){
		if(occurrencesA.size() != occurrencesB.size())
			return false;

		final FieldDefinition field = field(kind, tag);
		final Map<String, Integer> counts = new HashMap<>();
		for(final FLEFRecord occurrence : occurrencesA)
			counts.merge(signature.of(occurrence, field != null? field.type(): null), 1, Integer::sum);
		for(final FLEFRecord occurrence : occurrencesB){
			final String key = signature.of(occurrence, field != null? field.type(): null);
			final int count = counts.getOrDefault(key, 0);
			if(count == 0)
				return false;
			counts.put(key, count - 1);
		}
		return true;
	}

	/**
	 * Merges one field of the absorbed record into the survivor with the rules of the automatic merge.
	 *
	 * @return the values of the absorbed record that could not be kept
	 */
	public List<String> mergeField(final FLEFRecord survivor, final FLEFRecord absorbed, final String tag){
		final List<String> conflicts = new ArrayList<>();
		final StructType type = grammar.recordType(survivor.getTag());
		if(type == null || type.getField(tag) == null){
			if(FieldMerger.first(survivor, tag) == null)
				for(final FLEFRecord child : FieldMerger.children(absorbed, tag))
					FieldMerger.insertChild(survivor, FieldMerger.copy(child));
			return conflicts;
		}

		fields.mergeField(survivor, absorbed, type.getField(tag), survivor.getTag(), conflicts);
		return conflicts;
	}

	/**
	 * Completes the merge of {@code absorbedId} into {@code survivorId}, whose fields are already final.
	 *
	 * @param comment the evidence that justified the merge, for the audit of the survivor; may be {@code null}
	 * @throws IllegalArgumentException if the ids are equal, a record is missing, or the records are of different kinds
	 */
	public Result absorb(final FLEFModel model, final String absorbedId, final String survivorId, final String comment){
		final FLEFRecord absorbed = model.getRecordById(absorbedId);
		final FLEFRecord survivor = model.getRecordById(survivorId);
		if(absorbed == null || survivor == null)
			throw new IllegalArgumentException("Record does not exist: " + (absorbed == null? absorbedId: survivorId));
		if(absorbedId.equals(survivorId))
			throw new IllegalArgumentException("A record cannot be merged with itself: " + absorbedId);
		if(!Objects.equals(absorbed.getTag(), survivor.getTag()))
			throw new IllegalArgumentException("Records of different kinds cannot be merged: " + absorbed.getTag() + " vs "
				+ survivor.getTag());

		final String now = FLEFMerger.now(clock);
		FieldMerger.addAuditUpdate(survivor, now, "Merged record " + absorbedId
			+ (comment != null && !comment.isBlank()? ": " + comment: ""));

		final RecordStore store = RecordStore.of(model, references);
		final int redirected = store.absorb(Map.of(absorbedId, survivorId));

		final List<String> collapsedIds = new ArrayList<>(collapseDuplicates(store, survivorId, now));

		final List<String> deletedIds = new ArrayList<>();
		for(final FLEFRecord deleted : rules.deleteAcceptedHypotheses(store))
			deletedIds.add(deleted.getId());
		collapsedIds.addAll(rules.collapseDuplicateHypotheses(store, now).keySet());

		return new Result(redirected, collapsedIds, deletedIds);
	}

	/**
	 * Merges every field of the absorbed record into the survivor with the rules of the automatic merge: repeated fields are united,
	 * a singular field is taken only when the survivor lacks it, the most restrictive privacy is kept, the audit stays the
	 * survivor's.
	 *
	 * @return the values of the absorbed record that could not be kept
	 */
	public List<String> mergeFields(final FLEFRecord survivor, final FLEFRecord absorbed){
		final List<String> conflicts = new ArrayList<>();
		fields.mergeRecord(survivor, absorbed, conflicts);
		return conflicts;
	}

	/**
	 * Collapses the association records that state the same assertion and involve the given record (e.g. a place twice part of the
	 * same province), uniting their citations and notes and redirecting the references to the collapsed ones.
	 *
	 * @return the ids of the collapsed records
	 */
	public List<String> collapseDuplicates(final FLEFModel model, final String recordId){
		return collapseDuplicates(RecordStore.of(model, references), recordId, FLEFMerger.now(clock));
	}

	private List<String> collapseDuplicates(final RecordStore store, final String recordId, final String now){
		final List<String> collapsedIds = new ArrayList<>();
		new AssertionDeduplicator(store, references, signature, fields, rules, now, survivorPolicy(recordId, collapsedIds))
			.run();
		return collapsedIds;
	}

	/**
	 * Whether records of a kind can be the subject of an identity hypothesis (and so be judged the same or different entities).
	 */
	public static boolean isIdentityCandidate(final String kind){
		return EntityMatcher.MATCHABLE_KINDS.contains(kind);
	}

	/**
	 * @param hypothesisId the identity hypothesis of the pair (created, or the one already in the model)
	 * @param conclusionId the conclusion that disproves it
	 */
	public record Rejection(String hypothesisId, String conclusionId){}

	/**
	 * Records that the researcher judged two records to be different entities: the identity hypothesis of the pair (created when
	 * missing) is resolved by a ConclusionRecord whose PROOF_STATUS is {@code disproven}. Neither {@link #candidates} nor the
	 * automatic merge propose the pair again, until a more recent conclusion on the hypothesis says otherwise.
	 *
	 * @param rationale the evidence that made them look alike, for the hypothesis; may be {@code null}
	 * @param narrative why they were judged different, for the conclusion; may be {@code null}
	 * @throws IllegalArgumentException if a record is missing, the records are of different kinds, or their kind cannot be the
	 *                                  subject of an identity hypothesis
	 */
	public Rejection rejectIdentity(final FLEFModel model, final String idA, final String idB, final String rationale,
			final String narrative){
		final FLEFRecord a = model.getRecordById(idA);
		final FLEFRecord b = model.getRecordById(idB);
		if(a == null || b == null)
			throw new IllegalArgumentException("Record does not exist: " + (a == null? idA: idB));
		if(idA.equals(idB) || !Objects.equals(a.getTag(), b.getTag()) || !EntityMatcher.MATCHABLE_KINDS.contains(a.getTag()))
			throw new IllegalArgumentException("No identity hypothesis can relate " + idA + " and " + idB);

		final String now = FLEFMerger.now(clock);
		final String pair = ProtocolRules.unorderedPair(List.of(idA, idB));
		FLEFRecord hypothesis = null;
		for(final FLEFRecord existing : model.getRecordsByType(ProtocolRules.KIND_IDENTITY_HYPOTHESIS))
			if(pair.equals(ProtocolRules.unorderedPair(ProtocolRules.identities(existing)))){
				hypothesis = existing;
				break;
			}
		if(hypothesis == null){
			hypothesis = newRecord(model, ProtocolRules.KIND_IDENTITY_HYPOTHESIS, IdentityHypothesisHandler.ID_PREFIX);
			hypothesis.addChild(FLEFRecord.createChildWithTag(ProtocolRules.TAG_IDENTITY)
				.addChild(FLEFRecord.createChildWithTagAndValue(a.getTag(), idA)));
			hypothesis.addChild(FLEFRecord.createChildWithTag(ProtocolRules.TAG_IDENTITY)
				.addChild(FLEFRecord.createChildWithTagAndValue(b.getTag(), idB)));
			if(rationale != null && !rationale.isBlank())
				hypothesis.addChild(FLEFRecord.createChildWithTagAndValue(TAG_RATIONALE, rationale));
			hypothesis.addChild(audit(now));
			model.addRecord(hypothesis);
		}

		// a disproven conclusion prefers none of what it resolves
		final FLEFRecord conclusion = newRecord(model, ProtocolRules.KIND_CONCLUSION, ConclusionHandler.ID_PREFIX);
		conclusion.addChild(FLEFRecord.createChildWithTagAndValue(TAG_ISSUE, ISSUE_IDENTITY));
		conclusion.addChild(FLEFRecord.createChildWithTagAndValue(TAG_PROOF_STATUS, ModelIndex.PROOF_STATUS_DISPROVEN));
		if(narrative != null && !narrative.isBlank())
			conclusion.addChild(FLEFRecord.createChildWithTagAndValue(TAG_NARRATIVE, narrative));
		conclusion.addChild(FLEFRecord.createChildWithTag(TAG_RESOLVES)
			.addChild(FLEFRecord.createChildWithTagAndValue(ProtocolRules.KIND_IDENTITY_HYPOTHESIS, hypothesis.getId())));
		conclusion.addChild(audit(now));
		model.addRecord(conclusion);

		return new Rejection(hypothesis.getId(), conclusion.getId());
	}

	private static FLEFRecord newRecord(final FLEFModel model, final String kind, final String prefix){
		FLEFRecord record;
		do{
			record = FLEFRecord.createMainRecord(kind, prefix, model);
		}while(model.hasRecord(record.getId()));
		return record;
	}

	private static FLEFRecord audit(final String now){
		return FLEFRecord.createChildWithTag(TAG_AUDIT)
			.addChild(FLEFRecord.createChildWithTag(TAG_CREATION)
				.addChild(FLEFRecord.createChildWithTagAndValue(TAG_DATE, now)));
	}

	/**
	 * A record that may describe the same entity as another one.
	 *
	 * @param recordId    the candidate
	 * @param probability the match probability
	 * @param rationale   the evidence for and against the identity
	 */
	public record Candidate(String recordId, double probability, String rationale){}

	/**
	 * The records of the model that may describe the same entity as the given one, most likely first, with the comparison of the
	 * automatic merge (see {@link FLEFMerger}). Pairs that cannot be the same entity (e.g. places linked by succession) are left out.
	 *
	 * @param recordId        the record to find duplicates of; only the kinds that can be the subject of an identity hypothesis
	 *                        have candidates
	 * @param minProbability the probability below which a candidate is left out
	 */
	public List<Candidate> candidates(final FLEFModel model, final String recordId, final double minProbability){
		final FLEFRecord record = model.getRecordById(recordId);
		if(record == null || !EntityMatcher.MATCHABLE_KINDS.contains(record.getTag()))
			return List.of();

		final EntityMatcher matcher = new EntityMatcher(new ModelIndex(model.getRecords(), references), signature, false);
		final Set<String> keys = matcher.blockingKeys(record);
		final List<Candidate> candidates = new ArrayList<>();
		for(final FLEFRecord other : model.getRecordsByType(record.getTag())){
			if(other == record || Collections.disjoint(keys, matcher.blockingKeys(other)))
				continue;

			final MatchScore score = matcher.compare(record, other);
			if(!score.isVetoed() && score.probability() >= minProbability)
				candidates.add(new Candidate(other.getId(), score.probability(), score.describe()));
		}
		candidates.sort(Comparator.comparingDouble(Candidate::probability).reversed());
		return candidates;
	}

	/**
	 * Collapses only the duplicates the merge created: the groups of identical assertions that involve the survivor. Other
	 * duplicates of the model are left as the researcher made them.
	 */
	private AssertionDeduplicator.Policy survivorPolicy(final String survivorId, final List<String> collapsedIds){
		return new AssertionDeduplicator.Policy(){
			@Override
			public List<FLEFRecord> select(final List<FLEFRecord> group, final String participants){
				for(final String participant : participants.split(","))
					if(participant.endsWith(":" + survivorId))
						return group;

				final boolean[] involved = {false};
				for(final FLEFRecord record : group)
					references.forEachReference(record, (owner, parent, node, path, targetKind) -> {
						if(survivorId.equals(node.getValue()))
							involved[0] = true;
					});
				return (involved[0]? group: List.of());
			}

			@Override
			public String label(final String id){
				return "record " + id;
			}

			@Override
			public void collapsed(final String kind, final FLEFRecord survivor, final String absorbedId,
					final List<String> conflicts){
				collapsedIds.add(absorbedId);
			}
		};
	}

	private FieldDefinition field(final String kind, final String tag){
		final StructType type = grammar.recordType(kind);
		return (type != null? type.getField(tag): null);
	}

}
