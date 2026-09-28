package io.github.mtrevisan.familylegacy.v2.ui.tools.reports.index;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

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

	public KinshipResolver(final PedigreeIndex pedigreeIndex, final RelationshipIndex relationshipIndex){
		this.pedigreeIndex = pedigreeIndex;
		this.relationshipIndex = relationshipIndex;
	}

	public String relationshipTypeFrom(final FLEFRecord parent, final FLEFRecord child){
		for(final PedigreeIndex.ParentEdge edge : pedigreeIndex.parentEdgesOf(child)){
			if(edge.parent().getId().equals(parent.getId())){
				return edge.relationshipType();
			}
		}
		return "biological_child";
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
			case "biological_child" -> 10;
			case "adoptive_child" -> 20;
			case "foster_child" -> 30;
			case "guarded_child" -> 40;
			case "step_child" -> 50;
			default -> 60;
		};
	}

	public List<FLEFRecord> paternalAncestors(final FLEFRecord root){
		final List<FLEFRecord> out = new ArrayList<>();
		final List<FLEFRecord> parents = pedigreeIndex.parentsOf(root);
		if(!parents.isEmpty())
			collectAncestors(parents.getFirst(), out, new HashSet<>());
		return out;
	}

	public List<FLEFRecord> maternalAncestors(final FLEFRecord root){
		final List<FLEFRecord> out = new ArrayList<>();
		final List<FLEFRecord> parents = pedigreeIndex.parentsOf(root);
		if(parents.size() > 1)
			collectAncestors(parents.get(1), out, new HashSet<>());
		return out;
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
