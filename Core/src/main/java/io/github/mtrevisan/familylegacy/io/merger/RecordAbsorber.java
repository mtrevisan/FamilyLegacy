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

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Applies to a live model a merge decided by the researcher, following the Merge rule of the protocol.
 * <p>
 * The caller decides the fields of the survivor (possibly with {@link #mergeField}, which applies the same rules as the automatic
 * merge), then calls {@link #absorb}: every reference to the absorbed record, and every {@code flef:} link to it, is redirected to the
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
		Objects.requireNonNull(grammar, "Grammar cannot be null");

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

		final List<String> collapsedIds = new ArrayList<>();
		new AssertionDeduplicator(store, references, signature, fields, rules, now, survivorPolicy(survivorId, collapsedIds))
			.run();

		final List<String> deletedIds = new ArrayList<>();
		for(final FLEFRecord deleted : rules.deleteAcceptedHypotheses(store))
			deletedIds.add(deleted.getId());
		collapsedIds.addAll(rules.collapseDuplicateHypotheses(store, now).keySet());

		return new Result(redirected, collapsedIds, deletedIds);
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
