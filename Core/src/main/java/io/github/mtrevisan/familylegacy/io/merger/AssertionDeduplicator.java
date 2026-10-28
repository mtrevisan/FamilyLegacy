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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;


/**
 * Collapses the association records that, once the entities are merged, state exactly the same assertion.
 * <p>
 * After merging the two copies of a person, the two copies' birth events, parent relationships, and attributes of that person refer
 * to the same records: when they also agree on every asserted value they are the same assertion stated twice, and are collapsed
 * uniting their citations and notes. Assertions that differ in any value (e.g. two birth dates) are competing assertions: they are
 * all preserved, to be resolved by a ConclusionRecord.
 * <p>
 * Two events are the same only if they also have the same principals (twins have distinct birth events with the same date and
 * place). The kinds are processed in dependency order, since collapsing events makes their participations identical.
 */
final class AssertionDeduplicator{

	private static final List<List<String>> STAGES = List.of(
		List.of(ModelIndex.KIND_RELATIONSHIP, "individual_attribute", "group_attribute", ModelIndex.KIND_PLACE_RELATIONSHIP),
		List.of(ModelIndex.KIND_EVENT),
		List.of(ModelIndex.KIND_EVENT_PARTICIPATION),
		List.of("context_impact"),
		List.of(ProtocolRules.KIND_CONCLUSION)
	);


	/**
	 * Decides which records of a group of identical assertions are collapsed, and is told of every collapse.
	 */
	interface Policy{
		/**
		 * @param group        records stating the same assertion, in file order
		 * @param participants for events, the principals (or participants) as {@code kind:id} separated by commas; otherwise empty
		 * @return the survivor followed by the records to collapse into it, or an empty list to leave the group alone
		 */
		List<FLEFRecord> select(List<FLEFRecord> group, String participants);

		/** How a record is named in the audit of its survivor. */
		String label(String id);

		void collapsed(String kind, FLEFRecord survivor, String absorbedId, List<String> conflicts);
	}


	private final RecordStore store;
	private final ReferenceRewriter references;
	private final RecordSignature signature;
	private final FieldMerger fields;
	private final ProtocolRules rules;
	private final String now;
	private final Policy policy;


	AssertionDeduplicator(final RecordStore store, final ReferenceRewriter references, final RecordSignature signature,
			final FieldMerger fields, final ProtocolRules rules, final String now, final Policy policy){
		this.store = store;
		this.references = references;
		this.signature = signature;
		this.fields = fields;
		this.rules = rules;
		this.now = now;
		this.policy = policy;
	}


	/**
	 * The policy of a merge session: when merging two models only the incoming duplicates are collapsed, into the base record
	 * (duplicates already inside the base are not the business of a merge); when deduplicating, all of them.
	 */
	static Policy sessionPolicy(final MergeSession session){
		return new Policy(){
			@Override
			public List<FLEFRecord> select(final List<FLEFRecord> group, final String participants){
				if(!session.isMergeMode())
					return group;

				FLEFRecord survivor = null;
				for(final FLEFRecord record : group)
					if(!session.isIncoming(record.getId())){
						survivor = record;
						break;
					}
				final List<FLEFRecord> selected = new ArrayList<>();
				for(final FLEFRecord record : group)
					if(session.isIncoming(record.getId()))
						selected.add(record);
				if(survivor != null)
					selected.addFirst(survivor);
				return (selected.size() > 1? selected: List.of());
			}

			@Override
			public String label(final String id){
				return session.label(id);
			}

			@Override
			public void collapsed(final String kind, final FLEFRecord survivor, final String absorbedId,
					final List<String> conflicts){
				session.decide(new MergeReport.Decision(MergeReport.DecisionType.DUPLICATE_COLLAPSED, kind, survivor.getId(),
					List.of(session.inputId(absorbedId)), Double.NaN, "same assertion", conflicts));
			}
		};
	}


	void run(){
		for(final List<String> stage : STAGES){
			if(stage.contains(ProtocolRules.KIND_CONCLUSION))
				rules.dedupeConclusionTargets(store);
			final ModelIndex index = (stage.contains(ModelIndex.KIND_EVENT)
				? new ModelIndex(store.records(), references)
				: null);

			final Map<String, String> merges = new LinkedHashMap<>();
			for(final String kind : stage){
				final Map<String, List<FLEFRecord>> groups = new LinkedHashMap<>();
				final Map<String, String> participantsOf = new LinkedHashMap<>();
				for(final FLEFRecord record : store.records()){
					if(!kind.equals(record.getTag()))
						continue;

					final String participants = (index != null? participants(record.getId(), index): "");
					final String key = signature.ofRecord(record) + "|" + participants;
					groups.computeIfAbsent(key, k -> new ArrayList<>())
						.add(record);
					participantsOf.put(key, participants);
				}

				for(final Map.Entry<String, List<FLEFRecord>> group : groups.entrySet())
					if(group.getValue().size() > 1)
						collapse(kind, policy.select(group.getValue(), participantsOf.get(group.getKey())), merges);
			}
			store.absorb(merges);
		}
	}

	/**
	 * The principals of an event, or all its participants when it has no principal.
	 */
	private static String participants(final String eventId, final ModelIndex index){
		final TreeSet<String> principals = new TreeSet<>();
		final TreeSet<String> all = new TreeSet<>();
		for(final FLEFRecord participation : index.participationsOfEvent(eventId)){
			final FLEFRecord participant = FieldMerger.first(participation, "participant");
			final String who = ModelIndex.armTag(participant) + ":" + ModelIndex.armValue(participant);
			final String role = ModelIndex.value(participation, ModelIndex.TAG_ROLE);
			if(role == null || ModelIndex.ROLE_PRINCIPAL.equals(role))
				principals.add(who);
			all.add(who + "/" + role);
		}
		return String.join(",", principals.isEmpty()? all: principals);
	}

	private void collapse(final String kind, final List<FLEFRecord> selected, final Map<String, String> merges){
		if(selected.size() < 2)
			return;

		final FLEFRecord survivor = selected.getFirst();
		for(final FLEFRecord record : selected.subList(1, selected.size())){
			final List<String> conflicts = new ArrayList<>();
			fields.mergeRecord(survivor, record, conflicts);
			FieldMerger.addAuditUpdate(survivor, now, "Merged duplicate " + policy.label(record.getId()));
			policy.collapsed(kind, survivor, record.getId(), conflicts);
			merges.put(record.getId(), survivor.getId());
		}
	}

}
