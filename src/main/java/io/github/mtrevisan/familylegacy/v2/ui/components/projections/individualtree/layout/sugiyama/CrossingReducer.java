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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.sugiyama;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Crossing Reducer with Order Preservation (IVAPP 2022 principles).
 * Combines standard barycentric heuristic with native index preservation
 * to prevent unnecessary order flipping among non-connected or symmetric nodes.
 */
public class CrossingReducer{

	private static final int ITERATIONS = 8;

	// Alpha parameter controls order preservation weight (0.0 = pure barycenter, ~0.2-0.3 = strong order preservation)
	private static final double ORDER_PRESERVATION_ALPHA = 0.25;


	private CrossingReducer(){}


	public static void reduceCrossings(final List<List<Graph.Node>> layers){
		for(int pass = 0; pass < ITERATIONS; pass ++){
			// Top-Down sweep
			for(int l = 1; l < layers.size(); l ++)
				sortLayerWithOrderPreservation(layers.get(l), layers.get(l - 1), true);
			// Bottom-Up sweep
			for(int l = layers.size() - 2; l >= 0; l --)
				sortLayerWithOrderPreservation(layers.get(l), layers.get(l + 1), false);
		}
	}

	private static void sortLayerWithOrderPreservation(final List<Graph.Node> layer, final List<Graph.Node> refLayer,
			final boolean lookAtIncoming){
		// 1. Snapshot initial positions to preserve native sequence
		final int size = layer.size();
		final Map<Graph.Node, Integer> initialPositions = new HashMap<>(size);
		for(int i = 0; i < size; i ++)
			initialPositions.put(layer.get(i), i);

		final Map<Graph.Node, Integer> refLayerPositions = new HashMap<>(refLayer.size());
		for(int i = 0; i < refLayer.size(); i ++)
			refLayerPositions.put(refLayer.get(i), i);

		// 2. Sort using weighted key combining barycenter and original index
		layer.sort((n1, n2) -> {
			final double key1 = calculateOrderPreservingKey(n1, refLayerPositions, initialPositions.get(n1),
				lookAtIncoming);
			final double key2 = calculateOrderPreservingKey(n2, refLayerPositions, initialPositions.get(n2),
				lookAtIncoming);

			int cmp = Double.compare(key1, key2);
			if(cmp == 0)
				// Tie-breaking falls back strictly to initial relative order
				return Integer.compare(initialPositions.get(n1), initialPositions.get(n2));
			return cmp;
		});
	}

	private static double calculateOrderPreservingKey(final Graph.Node node,
			final Map<Graph.Node, Integer> refLayerPositions, final int originalIndex, final boolean lookAtIncoming){
		final List<Graph.Node> neighbors = (lookAtIncoming? node.getIncoming(): node.getOutgoing());

		// Nodes without edges in refLayer strictly retain their original relative index
		if(neighbors.isEmpty())
			return originalIndex;

		double sum = 0.;
		int count = 0;
		for(final Graph.Node neighbor : neighbors){
			final Integer index = refLayerPositions.get(neighbor);
			if(index != null){
				sum += index;
				count ++;
			}
		}

		if(count == 0)
			return originalIndex;

		final double barycenter = sum / count;

		// Weighted blend of barycenter and original sequence index
		return (1. - ORDER_PRESERVATION_ALPHA) * barycenter + ORDER_PRESERVATION_ALPHA * originalIndex;
	}

}
