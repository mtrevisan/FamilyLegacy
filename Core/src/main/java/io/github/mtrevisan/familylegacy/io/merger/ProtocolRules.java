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

import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.Cardinality;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FieldDefinition;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.StructType;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.TypeDefinition;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.UnionType;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The global rules of the protocol that follow a merge: deletion with cascade, and the fate of the identity hypotheses.
 * <ul>
 *   <li>Deletion: entries of repeated fields that reference the deleted record are removed, optional single fields are cleared,
 *     records whose required single field referenced it are deleted, and a ConclusionRecord whose RESOLVES becomes empty or whose
 *     PREFERRED was removed is deleted.</li>
 *   <li>Merge: the IdentityHypothesisRecord that proposed it (now pointing twice to the survivor) is deleted.</li>
 * </ul>
 */
final class ProtocolRules{

	static final String KIND_IDENTITY_HYPOTHESIS = "identity_hypothesis";
	static final String KIND_CONCLUSION = "conclusion";
	static final String TAG_IDENTITY = "identity";
	private static final String TAG_RESOLVES = "resolves";
	private static final String TAG_PREFERRED = "preferred";
	private static final String WILDCARD = "[*]";


	private final GrammarSupport grammar;
	private final ReferenceRewriter references;
	private final FieldMerger fields;


	ProtocolRules(final GrammarSupport grammar, final ReferenceRewriter references, final FieldMerger fields){
		this.grammar = grammar;
		this.references = references;
		this.fields = fields;
	}


	/**
	 * Deletes a record and cascades the deletion to the records that depend on it.
	 *
	 * @return the deleted records, in deletion order
	 */
	List<FLEFRecord> delete(final RecordStore store, final String id){
		final List<FLEFRecord> deleted = new ArrayList<>();
		final Deque<String> pending = new ArrayDeque<>();
		pending.add(id);
		while(!pending.isEmpty()){
			final String currentId = pending.poll();
			final FLEFRecord current = store.record(currentId);
			if(current == null)
				continue;

			store.delete(currentId);
			deleted.add(current);

			for(final FLEFRecord owner : new ArrayList<>(store.records()))
				if(removeReferences(owner, currentId, current.getTag()))
					pending.add(owner.getId());
		}
		return deleted;
	}

	/**
	 * Removes from {@code owner} the references to a deleted record.
	 *
	 * @return whether the owner must be deleted too
	 */
	private boolean removeReferences(final FLEFRecord owner, final String deletedId, final String deletedKind){
		boolean deleteOwner = false;
		for(final ReferenceRewriter.XrefPath xrefPath : references.pathsOf(owner.getTag())){
			if(!xrefPath.targetKind().equals(deletedKind))
				continue;

			final String[] segments = StringUtils.split(xrefPath.path(), '.');
			final List<List<FLEFRecord>> chains = new ArrayList<>();
			collectChains(owner, segments, 0, deletedId, new ArrayList<>(List.of(owner)), chains);
			if(chains.isEmpty())
				continue;

			final int removableDepth = removableDepth(owner.getTag(), segments);
			if(removableDepth < 0){
				deleteOwner = true;
				continue;
			}

			for(final List<FLEFRecord> chain : chains){
				final FLEFRecord parent = chain.get(removableDepth);
				final FLEFRecord node = chain.get(removableDepth + 1);
				parent.getChildren()
					.removeIf(child -> child == node);
			}

			final String topField = segment(segments[0]);
			final StructType type = grammar.recordType(owner.getTag());
			final FieldDefinition field = (type != null? type.getField(topField): null);
			if(field != null && field.cardinality().isRequired() && FieldMerger.first(owner, topField) == null)
				deleteOwner = true;
			if(KIND_CONCLUSION.equals(owner.getTag())
					&& (TAG_PREFERRED.equals(topField) || FieldMerger.first(owner, TAG_RESOLVES) == null))
				deleteOwner = true;
			if(KIND_IDENTITY_HYPOTHESIS.equals(owner.getTag()) && identities(owner).size() < 2)
				deleteOwner = true;
		}
		return deleteOwner;
	}

	private static void collectChains(final FLEFRecord node, final String[] segments, final int index, final String id,
			final List<FLEFRecord> chain, final List<List<FLEFRecord>> chains){
		final String tag = segment(segments[index]);
		for(final FLEFRecord child : node.getChildren()){
			if(!tag.equals(child.getTag()))
				continue;

			chain.add(child);
			if(index == segments.length - 1){
				if(id.equals(child.getValue()))
					chains.add(new ArrayList<>(chain));
			}
			else
				collectChains(child, segments, index + 1, id, chain, chains);
			chain.removeLast();
		}
	}

	/**
	 * Index of the deepest segment of the path whose field is optional or repeated, so that its occurrence can be removed;
	 * {@code -1} if every field along the path is required.
	 */
	private int removableDepth(final String kind, final String[] segments){
		TypeDefinition type = grammar.recordType(kind);
		int depth = -1;
		for(int i = 0; i < segments.length; i ++){
			final String tag = segment(segments[i]);
			if(type instanceof StructType struct){
				final FieldDefinition field = struct.getField(tag);
				if(field == null)
					break;

				if(field.cardinality() != Cardinality.REQUIRED)
					depth = i;
				type = grammar.resolve(field.type());
			}
			else if(type instanceof UnionType union)
				// an arm is the whole value of its union: it is not removable by itself
				type = grammar.resolve(union.getChoices().get(tag));
			else
				break;
		}
		return depth;
	}

	private static String segment(final String segment){
		return StringUtils.removeEnd(segment, WILDCARD);
	}


	/**
	 * The ids a hypothesis proposes as the same entity.
	 */
	static List<String> identities(final FLEFRecord hypothesis){
		final List<String> ids = new ArrayList<>(2);
		for(final FLEFRecord identity : FieldMerger.children(hypothesis, TAG_IDENTITY)){
			final String id = ModelIndex.armValue(identity);
			if(id != null)
				ids.add(id);
		}
		return ids;
	}

	/**
	 * Deletes, with cascade, the identity hypotheses whose candidates have become the same record: the merge accepted them.
	 *
	 * @return the deleted records
	 */
	List<FLEFRecord> deleteAcceptedHypotheses(final RecordStore store){
		final List<String> accepted = new ArrayList<>();
		for(final FLEFRecord record : store.records())
			if(KIND_IDENTITY_HYPOTHESIS.equals(record.getTag())){
				final List<String> ids = identities(record);
				if(ids.size() == 2 && ids.get(0).equals(ids.get(1)))
					accepted.add(record.getId());
			}

		final List<FLEFRecord> deleted = new ArrayList<>();
		for(final String id : accepted)
			deleted.addAll(delete(store, id));
		return deleted;
	}

	/**
	 * Identity hypotheses should be unique per unordered pair: after a merge two hypotheses can propose the same pair, and are
	 * merged into the first.
	 *
	 * @return absorbed id to survivor id, already applied to the store
	 */
	Map<String, String> collapseDuplicateHypotheses(final RecordStore store, final String now){
		final Map<String, FLEFRecord> byPair = new HashMap<>();
		final Map<String, String> merges = new LinkedHashMap<>();
		for(final FLEFRecord record : store.records()){
			if(!KIND_IDENTITY_HYPOTHESIS.equals(record.getTag()))
				continue;

			final String pair = unorderedPair(identities(record));
			if(pair == null)
				continue;

			final FLEFRecord survivor = byPair.putIfAbsent(pair, record);
			if(survivor != null){
				fields.mergeRecord(survivor, record, new ArrayList<>());
				FieldMerger.addAuditUpdate(survivor, now, "Merged duplicate hypothesis " + record.getId());
				merges.put(record.getId(), survivor.getId());
			}
		}
		store.absorb(merges);
		return merges;
	}

	static String unorderedPair(final List<String> ids){
		if(ids.size() != 2)
			return null;
		return (ids.get(0).compareTo(ids.get(1)) <= 0? ids.get(0) + '\u0000' + ids.get(1): ids.get(1) + '\u0000' + ids.get(0));
	}

	/**
	 * After a merge, the RESOLVES of a conclusion can list the same record twice: the duplicates are removed.
	 *
	 * @return the ids of the conclusions that changed
	 */
	Set<String> dedupeConclusionTargets(final RecordStore store){
		final Set<String> changed = new LinkedHashSet<>();
		for(final FLEFRecord record : store.records()){
			if(!KIND_CONCLUSION.equals(record.getTag()))
				continue;

			final Set<String> seen = new LinkedHashSet<>();
			final boolean removed = record.getChildren()
				.removeIf(child -> TAG_RESOLVES.equals(child.getTag())
					&& !seen.add(ModelIndex.armTag(child) + ':' + ModelIndex.armValue(child)));
			if(removed)
				changed.add(record.getId());
		}
		return changed;
	}

}
