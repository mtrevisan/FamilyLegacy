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

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Layerer{

	private Layerer(){}


	public static List<List<Graph.Node>> assignLayers(final Graph graph,
			final Map<Graph.Node, List<Long>> ahnentafelMap){
		final Map<Graph.Node, Integer> heightMap = new HashMap<>();

		// 1. Identify leaf/subject nodes (nodes without outgoing edges in the ancestor DAG)
		final List<Graph.Node> leaves = new ArrayList<>();
		for(final Graph.Node node : graph.getNodes().values())
			if(node.getOutgoing().isEmpty())
				leaves.add(node);
		if(leaves.isEmpty() && !graph.getNodes().isEmpty())
			leaves.add(graph.getNodes().values().iterator().next());

		// 2. Compute height and Ahnentafel numbers upwards (Subject = 1, Father = 2k, Mother = 2k+1)
		long baseAhnentafel = 1l;
		for(final Graph.Node leaf : leaves){
			computeAncestorTopology(leaf, 0, baseAhnentafel, heightMap, ahnentafelMap, new HashSet<>());
			// Shift to avoid collision if multiple independent leaves exist
			baseAhnentafel <<= 10;
		}

		// 3. Invert heights so ancestors occupy top layers (layer 0) and descendants lower layers
		int minHeight = Integer.MAX_VALUE;
		int maxHeight = Integer.MIN_VALUE;
		for(final Graph.Node node : graph.getNodes().values()){
			final int height = heightMap.getOrDefault(node, 0);
			if(height < minHeight)
				minHeight = height;
			if(height > maxHeight)
				maxHeight = height;
		}

		// Map generation levels to positive 0-based layer indices:
		// Oldest ancestors (minHeight) -> Layer 0 (top row)
		// Root generation (maxHeight) -> Layer totalAncestorLayers (bottom row)
		final int totalAncestorLayers = maxHeight - minHeight;
		for(final Graph.Node node : graph.getNodes().values()){
			final TreeNode tn = node.getTreeNode();

			// Invert layer index: top ancestors get layer 0, subject gets highest layer index
			final int gen = (tn != null? tn.getGeneration(): -heightMap.getOrDefault(node, 0));
			final int assignedLayer = maxHeight - gen;
			node.setLayer(assignedLayer);
		}

		// 4. Group nodes into discrete layer lists
		final List<List<Graph.Node>> layers = new ArrayList<>();
		for(int i = 0; i < totalAncestorLayers; i ++)
			layers.add(new ArrayList<>());
		for(final Graph.Node node : graph.getNodes().values()){
			final int layer = node.getLayer() - 1;
			if(layer >= 0 && layer < layers.size())
				layers.get(layer)
					.add(node);
		}

		// 5. Absolute Ahnentafel ordering within each layer
		for(final List<Graph.Node> layer : layers)
			layer.sort(Comparator.comparingLong(n -> getMinAhnentafel(n, ahnentafelMap)));

		return layers;
	}

	private static void computeAncestorTopology(final Graph.Node current, final int currentHeight,
			final long currentAhnentafel, final Map<Graph.Node, Integer> heightMap,
			final Map<Graph.Node, List<Long>> ahnentafelMap, final Set<Graph.Node> visiting){
		if(current == null || visiting.contains(current))
			return;

		visiting.add(current);

		if(!heightMap.containsKey(current) || currentHeight > heightMap.get(current))
			heightMap.put(current, currentHeight);
		// Record Ahnentafel value (support multiple values for consanguineous nodes)
		final List<Long> ahnentafels = ahnentafelMap.computeIfAbsent(current, k -> new ArrayList<>());
		if(!ahnentafels.contains(currentAhnentafel))
			ahnentafels.add(currentAhnentafel);

		// Recursively navigate incoming edges and assign 2k to Father and 2k+1 to Mother
		final TreeNode treeNode = current.getTreeNode();
		for(final Graph.Node parent : current.getIncoming()){
			final TreeNode parentTreeNode = parent.getTreeNode();
			// Default father rule
			long nextAhnentafel = currentAhnentafel * 2l;

			if(treeNode != null && parentTreeNode != null && treeNode.getMother() == parentTreeNode)
				// Mother rule
				nextAhnentafel = currentAhnentafel * 2l + 1l;

			computeAncestorTopology(parent, currentHeight + 1, nextAhnentafel, heightMap, ahnentafelMap,
				visiting);
		}

		visiting.remove(current);
	}

	private static long getMinAhnentafel(final Graph.Node node, final Map<Graph.Node, List<Long>> ahnentafelMap){
		final List<Long> list = ahnentafelMap.get(node);
		if(list == null || list.isEmpty())
			return Long.MAX_VALUE;

		long min = Long.MAX_VALUE;
		for(final Long val : list)
			if(val < min)
				min = val;
		return min;
	}

}
