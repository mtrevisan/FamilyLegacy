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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.sugiyama;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Step 2 of the Sugiyama framework: minimize edge crossings by refining
 * the layer order.
 * <p>
 * The initial order comes from the path signature assigned in Step 1
 * ({@code F} before {@code M}, recursively), which for a standard
 * ancestor tree already produces zero crossings. This reducer is a
 * refinement for the cases where the tree is not a pure tree: endogamy,
 * remarriage, multiple connections between branches.
 * <p>
 * <b>Constrained barycenter.</b> The layer is partitioned into
 * <b>blocks</b>: each block is either a couple (two spouses on the same
 * layer) or a singleton. The internal order of a block is fixed
 * (father left, mother right), so the reducer never separates a couple
 * and never swaps the two spouses. Blocks are sorted by the barycenter
 * of their members, and the candidate order is accepted only if it
 * strictly reduces the number of crossings.
 * <p>
 * This is what preserves the natural reading order of a pedigree while
 * still minimizing crossings when the tree structure allows it.
 */
public final class SugiyamaCrossingReducer{

	private static final int ITERATIONS = 8;


	private SugiyamaCrossingReducer(){}


	/**
	 * Refines the layer order in place.
	 *
	 * @param hierarchy the hierarchy built by {@link SugiyamaHierarchy}
	 */
	public static void reduce(final SugiyamaHierarchy.Hierarchy hierarchy){
		final List<List<String>> layers = hierarchy.layers();
		final Map<String, List<String>> parentsOf = hierarchy.parentsOf();
		final Map<String, List<String>> childrenOf = hierarchy.childrenOf();
		// Build the parent map for the crossing reducer
		final Map<String, String> parentOf = hierarchy.buildParentMap();

		for(int iter = 0; iter < ITERATIONS; iter ++){
			boolean changed = false;
			for(int l = 1; l < layers.size(); l ++)
				changed |= refineLayer(layers, l, parentsOf, parentOf);
			for(int l = layers.size() - 2; l >= 0; l --)
				changed |= refineLayer(layers, l, childrenOf, parentOf);
			if(!changed)
				break;
		}
	}


	/* ======================================================================
	 *                          Layer refinement
	 * ====================================================================== */

	/**
	 * Refines one layer: the current order is compared with a
	 * barycenter-based candidate, and the candidate is accepted only
	 * if it reduces crossings. Blocks (couples) keep their internal
	 * order fixed.
	 */
	private static boolean refineLayer(final List<List<String>> layers, final int l,
			final Map<String, List<String>> neighborMap, final Map<String, String> spouseOf){
		final List<String> layer = layers.get(l);
		if(layer.size() <= 1)
			return false;

		final List<String> reference = lookupReferenceLayer(layers, l, neighborMap);
		if(reference.isEmpty())
			return false;

		final Map<String, Integer> refIdx = new HashMap<>(reference.size());
		for(int i = 0; i < reference.size(); i ++)
			refIdx.put(reference.get(i), i);

		// Build the blocks (couple or singleton) in the current order
		final List<Block> blocks = buildBlocks(layer, spouseOf);

		// Barycenter of each block: average of its members' barycenters
		for(final Block b : blocks){
			double sum = 0;
			int n = 0;
			for(int m = 0; m < b.members.size(); m ++){
				final String id = b.members.get(m);
				final List<String> neighbors = neighborMap.get(id);
				if(neighbors != null && !neighbors.isEmpty()){
					double s = 0;
					int k = 0;
					for(int idx = 0; idx < neighbors.size(); idx ++){
						final Integer i = refIdx.get(neighbors.get(idx));
						if(i != null){
							s += i;
							k ++;
						}
					}
					if(k > 0){
						sum += s / k;
						n ++;
					}
				}
			}
			b.barycenter = (n > 0? sum / n: null);
		}

		final Map<String, Integer> blockPosition = new HashMap<>(blocks.size());
		for(int i = 0; i < blocks.size(); i ++)
			blockPosition.put(blocks.get(i).members.getFirst(), i);

		final List<Block> candidate = new ArrayList<>(blocks);
		candidate.sort(Comparator
			.comparingDouble((Block b) -> (b.barycenter == null? Double.MAX_VALUE: b.barycenter))
			.thenComparingInt(b -> blockPosition.getOrDefault(b.members.getFirst(), 0)));

		// Build the candidate order
		final List<String> candidateOrder = new ArrayList<>(layer.size());
		for(final Block b : candidate)
			candidateOrder.addAll(b.members);

		// Accept only if strictly fewer crossings
		final int currentCrossings = countCrossings(layer, neighborMap, refIdx);
		final int candidateCrossings = countCrossings(candidateOrder, neighborMap, refIdx);
		if(candidateCrossings < currentCrossings){
			layer.clear();
			layer.addAll(candidateOrder);

			return true;
		}

		return false;
	}


	/* ======================================================================
	 *                          Blocks
	 * ====================================================================== */

	private static final class Block{
		final List<String> members = new ArrayList<>(2);
		Double barycenter;
	}

	/**
	 * Walks the layer and groups each individual with their spouse (if
	 * the spouse is on the same layer and not yet grouped). The internal
	 * order of a block is the order in which the members appear in the
	 * layer, which is the order chosen by the path signature: the
	 * father (F) comes before the mother (M).
	 */
	private static List<Block> buildBlocks(final List<String> layer, final Map<String, String> partnerOf){
		final Set<String> assigned = new HashSet<>(layer.size());
		final List<Block> blocks = new ArrayList<>(layer.size());
		for(final String id : layer){
			if(assigned.contains(id))
				continue;

			final Block b = new Block();
			b.members.add(id);
			assigned.add(id);
			if(partnerOf != null){
				final String sp = partnerOf.get(id);
				if(sp != null && layer.contains(sp) && !assigned.contains(sp)){
					// The spouse must appear AFTER the current individual
					// in the layer order (father before mother). If it
					// appears before, we still add it but after, because
					// the path signature guarantees F before M.
					b.members.add(sp);
					assigned.add(sp);
				}
			}
			blocks.add(b);
		}
		return blocks;
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private static List<String> lookupReferenceLayer(final List<List<String>> layers, final int l,
			final Map<String, List<String>> neighborMap){
		if(l > 0){
			final List<String> currentLayer = layers.get(l);
			final List<String> prevLayer = layers.get(l - 1);
			for(final String id : currentLayer){
				final List<String> nbs = neighborMap.get(id);
				if(nbs != null)
					for(final String nb : nbs)
						if(prevLayer.contains(nb))
							return prevLayer;
			}
		}
		if(l + 1 < layers.size()){
			final List<String> currentLayer = layers.get(l);
			final List<String> nextLayer = layers.get(l + 1);
			for(final String id : currentLayer){
				final List<String> nbs = neighborMap.get(id);
				if(nbs != null)
					for(final String nb : nbs)
						if(nextLayer.contains(nb))
							return nextLayer;
			}
		}
		return List.of();
	}

	/**
	 * Counts the crossings between the given layer and the reference
	 * layer. Two edges cross when the order of their sources in the
	 * layer is opposite to the order of their targets in the reference.
	 */
	private static int countCrossings(final List<String> layer, final Map<String, List<String>> neighborMap,
			final Map<String, Integer> refIdx){
		int crossings = 0;
		final int layerSize = layer.size();
		final int[][] neighborIndices = new int[layerSize][];

		for(int i = 0; i < layerSize; i ++){
			final String u = layer.get(i);

			final List<String> nbs = neighborMap.get(u);
			if(nbs == null || nbs.isEmpty()){
				neighborIndices[i] = new int[0];

				continue;
			}

			final List<Integer> temp = new ArrayList<>(nbs.size());
			for(final String nb : nbs){
				final Integer idx = refIdx.get(nb);
				if(idx != null)
					temp.add(idx);
			}
			final int[] arr = new int[temp.size()];
			for(int k = 0; k < temp.size(); k ++)
				arr[k] = temp.get(k);
			neighborIndices[i] = arr;
		}

		for(int i = 0; i < layerSize; i ++){
			final int[] uNeighbors = neighborIndices[i];
			if(uNeighbors.length == 0)
				continue;

			for(int j = i + 1; j < layerSize; j ++){
				final int[] vNeighbors = neighborIndices[j];
				for(final int vIdx : vNeighbors)
					for(final int uIdx : uNeighbors)
						if(uIdx > vIdx)
							crossings ++;
			}
		}
		return crossings;
	}

}
