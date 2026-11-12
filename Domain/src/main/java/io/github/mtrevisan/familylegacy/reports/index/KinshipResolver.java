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
package io.github.mtrevisan.familylegacy.reports.index;

import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.RecordLookup;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


/**
 * Encapsulates rank, priority, parent relationship evaluation logic,
 * ancestor tree traversal, and indirect relation resolution.
 */
public final class KinshipResolver{

	public enum IndirectKind{
		SPOUSE,
		ASSOCIATE
	}

	public record IndirectRelation(FLEFRecord individual, FLEFRecord via, IndirectKind kind){
	}

	private final PedigreeIndex pedigreeIndex;
	private final RelationshipIndex relationshipIndex;
	private final RecordLookup model;

	public KinshipResolver(final PedigreeIndex pedigreeIndex, final RelationshipIndex relationshipIndex, final RecordLookup model){
		this.pedigreeIndex = pedigreeIndex;
		this.relationshipIndex = relationshipIndex;
		this.model = model;
	}

	public String relationshipTypeFrom(final FLEFRecord parent, final FLEFRecord child){
		for(final PedigreeIndex.ParentEdge edge : pedigreeIndex.parentEdgesOf(child)){
			if(edge.parent().getId().equals(parent.getId())){
				return edge.relationshipType();
			}
		}
		return RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD;
	}

	public FLEFRecord otherParentOf(final FLEFRecord child, final FLEFRecord person){
		FLEFRecord fallback = null;
		FLEFRecord best = null;
		int bestRank = Integer.MAX_VALUE;

		for(final PedigreeIndex.ParentEdge edge : pedigreeIndex.parentEdgesOf(child)){
			final FLEFRecord candidate = edge.parent();
			if(candidate.getId().equals(person.getId()))
				continue;

			if(fallback == null) fallback = candidate;
			final int rank = rankOf(edge.relationshipType());
			if(rank < bestRank){
				bestRank = rank;
				best = candidate;
			}
		}
		return (best != null? best: fallback);
	}

	public static int rankOf(final String relationshipType){
		if(relationshipType == null)
			return 90;
		return switch(relationshipType.toLowerCase(Locale.ROOT)){
			case RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, RelationshipReader.ENUM_TYPE_CHILD -> 10;
			case RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD -> 20;
			case RelationshipReader.ENUM_TYPE_FOSTER_CHILD -> 30;
			case RelationshipReader.ENUM_TYPE_GUARDED_CHILD -> 40;
			case RelationshipReader.ENUM_TYPE_STEP_CHILD -> 50;
			default -> 60;
		};
	}

	/** @return	The father of the root and all his ancestors (empty if the father cannot be determined, see {@link #parentsBySex}). */
	public List<FLEFRecord> paternalAncestors(final FLEFRecord root){
		final List<FLEFRecord> out = new ArrayList<>();
		final FLEFRecord father = parentsBySex(root)[0];
		if(father != null)
			collectAncestors(father, out, new HashSet<>());
		return out;
	}

	/** @return	The mother of the root and all her ancestors (empty if the mother cannot be determined, see {@link #parentsBySex}). */
	public List<FLEFRecord> maternalAncestors(final FLEFRecord root){
		final List<FLEFRecord> out = new ArrayList<>();
		final FLEFRecord mother = parentsBySex(root)[1];
		if(mother != null)
			collectAncestors(mother, out, new HashSet<>());
		return out;
	}

	/**
	 * Tells the father from the mother by the preferred sex of the parents, never by the order of the relationships in the file.
	 * <ul>
	 *   <li>The father is the male parent, the mother the female one; if there are several of the same sex, the one with the
	 *   closest kind of link wins (see {@link #rankOf(String)}: biological before adoptive, foster, ...), then the first in the
	 *   order of the model.</li>
	 *   <li>A parent of unknown sex is never guessed: it is assigned only to the side that the other parent of known sex leaves
	 *   free, and only if it is the single parent of unknown sex; otherwise (no parent of known sex, or several unknown ones) it is
	 *   assigned to neither side.</li>
	 * </ul>
	 *
	 * @return	An array of two elements: the father and the mother, each one {@code null} if it cannot be determined.
	 */
	private FLEFRecord[] parentsBySex(final FLEFRecord root){
		FLEFRecord father = null;
		FLEFRecord mother = null;
		int fatherRank = Integer.MAX_VALUE;
		int motherRank = Integer.MAX_VALUE;
		final List<FLEFRecord> unknown = new ArrayList<>(2);
		for(final PedigreeIndex.ParentEdge edge : pedigreeIndex.parentEdgesOf(root)){
			final FLEFRecord parent = edge.parent();
			final SexType sex = PreferredSex.of(parent, model);
			final int rank = rankOf(edge.relationshipType());
			if(sex == SexType.MALE){
				if(rank < fatherRank){
					father = parent;
					fatherRank = rank;
				}
			}
			else if(sex == SexType.FEMALE){
				if(rank < motherRank){
					mother = parent;
					motherRank = rank;
				}
			}
			else
				unknown.add(parent);
		}
		if(unknown.size() == 1){
			if(father == null && mother != null)
				father = unknown.getFirst();
			else if(mother == null && father != null)
				mother = unknown.getFirst();
		}
		return new FLEFRecord[]{father, mother};
	}

	private void collectAncestors(final FLEFRecord start, final List<FLEFRecord> out, final Set<String> seen){
		if(!seen.add(start.getId()))
			return;
		out.add(start);
		for(final FLEFRecord p : pedigreeIndex.parentsOf(start))
			collectAncestors(p, out, seen);
	}

	public List<FLEFRecord> reachable(final FLEFRecord start){
		final List<FLEFRecord> out = new ArrayList<>();
		final Set<String> seen = new HashSet<>();
		final Deque<FLEFRecord> stack = new ArrayDeque<>();
		stack.push(start);
		while(!stack.isEmpty()){
			final FLEFRecord cur = stack.pop();
			if(!seen.add(cur.getId()))
				continue;
			out.add(cur);
			for(final FLEFRecord c : pedigreeIndex.childrenOf(cur)) stack.push(c);
		}
		return out;
	}

	public List<IndirectRelation> indirectRelationsDetailed(final FLEFRecord root){
		final List<FLEFRecord> direct = reachable(root);
		final Set<String> seen = new HashSet<>();
		for(final FLEFRecord r : direct) seen.add(r.getId());
		final List<IndirectRelation> out = new ArrayList<>();
		for(final FLEFRecord r : direct){
			for(final FLEFRecord s : relationshipIndex.spousesOf(r))
				if(seen.add(s.getId())) out.add(new IndirectRelation(s, r, IndirectKind.SPOUSE));
			for(final FLEFRecord a : relationshipIndex.associatesOf(r))
				if(seen.add(a.getId())) out.add(new IndirectRelation(a, r, IndirectKind.ASSOCIATE));
		}
		return out;
	}
}
