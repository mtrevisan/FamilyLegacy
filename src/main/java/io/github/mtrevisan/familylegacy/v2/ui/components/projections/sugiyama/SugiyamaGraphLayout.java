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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeService;
import org.apache.commons.lang3.StringUtils;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Runs the Sugiyama pipeline on an ancestor tree where the vertices are
 * individuals and exposes the result as absolute bounds.
 * <p>
 * Step 1 (hierarchy) is delegated to {@link SugiyamaHierarchy}. Step 2
 * (crossing reduction) is implicit in the path-based order chosen by the
 * hierarchy. Step 3 (X coordinate) is a two-direction iterative
 * refinement: each individual is pulled toward the midpoint of its
 * parents, then toward the midpoint of its children, and overlaps are
 * resolved by pushing right. Because the vertex is now an individual,
 * the couple is simply two adjacent vertices, and the midpoint rule
 * naturally centers each child between its father and mother.
 */
public final class SugiyamaGraphLayout{

	/** Width of an IndividualPanel in SECONDARY mode. */
	public static final int SECONDARY_WIDTH = (int)IndividualPanel.BOX_DIMENSION_SECONDARY.getWidth();
	/** Height of an IndividualPanel in SECONDARY mode. */
	public static final int SECONDARY_HEIGHT = (int)IndividualPanel.BOX_DIMENSION_SECONDARY.getHeight();
	/** Horizontal gap between two adjacent vertices in the same layer. */
	public static final int HORIZONTAL_GAP = 32;
	/** Vertical distance between two consecutive tracks inside a gap. */
	public static final int TRACK_SPACING = 24;
	/** Outer padding of the whole graph. */
	public static final int PADDING = 40;
	/** Width of a dummy node. */
	public static final int DUMMY_WIDTH = 1;
	/** Height of a dummy node. */
	public static final int DUMMY_HEIGHT = 1;
	/** Number of refinement passes. */
	private static final int REFINEMENT_ITERATIONS = 4;

	private static final String PIPE = "|";


	public record Result(
		String rootId,
		Map<String, Rectangle> nodeBounds,
		Set<String> dummyIds,
		List<SugiyamaHierarchy.Edge> edges,
		Map<String, IndividualData> dataById,
		Rectangle contentBounds){
	}


	private SugiyamaGraphLayout(){}


	public static Result compute(final FLEFModel model, final String rootId, final TreeService treeService,
			final int maxAncestors, final int maxDescendants, final boolean showPartner){
		// Step 1: hierarchy
		final SugiyamaHierarchy.Hierarchy h = SugiyamaHierarchy.build(model, rootId, treeService, maxAncestors,
			maxDescendants, showPartner);
		if(h.layers().isEmpty())
			return new Result(StringUtils.EMPTY, Map.of(), Set.of(), List.of(), Map.of(), new Rectangle());

		// Step 2: minimize crossings (constrained barycenter)
		SugiyamaCrossingReducer.reduce(h);

		// Step 3: X coordinates
		final Map<String, Double> xCoords = assignXCoordinates(h);

		// Track coloring per gap for the Y of each horizontal bar
		final Map<Integer, Integer> trackCountByGap = computeHorizontalTrackCounts(h, xCoords);

		// Layer Y: variable height, driven by the number of tracks and by the actual height of the boxes in each layer
		final int layerCount = h.layers()
			.size();
		final double[] layerTop = new double[layerCount];
		final int[] layerHeight = new int[layerCount];
		for(int l = 0; l < layerCount; l ++)
			layerHeight[l] = maxHeightInLayer(h.layers().get(l), h.dummyIds());
		layerTop[0] = PADDING;
		for(int l = 1; l < layerCount; l ++){
			final int t = trackCountByGap.getOrDefault(l - 1, 0);
			final double gapHeight = (t + 1) * (double)TRACK_SPACING;
			layerTop[l] = layerTop[l - 1] + layerHeight[l - 1] + gapHeight;
		}

		// X shift: bring the leftmost box's left edge to PADDING
		double minCx = Double.POSITIVE_INFINITY;
		double maxCx = Double.NEGATIVE_INFINITY;
		for(final Map.Entry<String, Double> entry : xCoords.entrySet()){
			final String id = entry.getKey();
			final double cx = entry.getValue();

			final int w = widthOf(id, h.dummyIds());
			minCx = Math.min(minCx, cx - w / 2.);
			maxCx = Math.max(maxCx, cx + w / 2.);
		}
		if(!Double.isFinite(minCx)){
			minCx = 0;
			maxCx = 0;
		}
		final double shift = PADDING - minCx;

		// Final bounds. Nodes are bottom-aligned within their layer
		final Map<String, Rectangle> bounds = new LinkedHashMap<>(xCoords.size());
		for(int l = 0; l < layerCount; l ++){
			final int top = (int)Math.round(layerTop[l]);
			final int rowH = layerHeight[l];
			final List<String> layer = h.layers().get(l);
			for(int i = 0; i < layer.size(); i ++){
				final String id = layer.get(i);

				final int w = widthOf(id, h.dummyIds());
				final int hh = heightOf(id, h.dummyIds());
				final int cx = (int)Math.round(xCoords.getOrDefault(id, 0.) + shift);
				// Bottom-align: smaller boxes sit lower in the layer
				final int y = top + (rowH - hh);
				bounds.put(id, new Rectangle(cx - w / 2, y, w, hh));
			}
		}

		final int contentW = (int)Math.ceil(maxCx - minCx) + 2 * PADDING;
		final int contentH = (int)Math.ceil(layerTop[layerCount - 1] + layerHeight[layerCount - 1])
			+ PADDING;

		return new Result(h.rootId(), bounds, h.dummyIds(), h.edges(), h.dataById(),
			new Rectangle(0, 0, contentW, contentH));
	}


	/* ======================================================================
	 *                          Size helpers
	 * ====================================================================== */

	private static int widthOf(final String id, final Set<String> dummyIds){
		return (dummyIds.contains(id)? DUMMY_WIDTH: SECONDARY_WIDTH);
	}

	private static int heightOf(final String id, final Set<String> dummyIds){
		return (dummyIds.contains(id)? DUMMY_HEIGHT: SECONDARY_HEIGHT);
	}

	/** Returns the height of the tallest box in the layer. */
	private static int maxHeightInLayer(final List<String> layer, final Set<String> dummyIds){
		int max = 0;
		for(int i = 0; i < layer.size(); i ++){
			final String id = layer.get(i);

			max = Math.max(max, heightOf(id, dummyIds));
		}
		return max;
	}


	/* ======================================================================
	 *                          Step 3: X coordinates
	 * ====================================================================== */

	/**
	 * Assigns the X coordinate (center) of every vertex.
	 * <p>
	 * Layers are processed top-down. For each layer below the first, the
	 * nodes are re-sorted by the average X of their parents, so that
	 * siblings stay clustered under their couple and children of different
	 * couples do not interleave. Without this re-sort, the layer would keep
	 * the path-signature order chosen by the hierarchy, which has no
	 * relationship with the horizontal position of the parents and mixes
	 * unrelated families.
	 * <p>
	 * After the initial top-down placement, a barycenter refinement pulls
	 * each node toward the midpoint of its parents. The refinement respects
	 * the order established during the placement, so the clustering is
	 * preserved.
	 */
	private static Map<String, Double> assignXCoordinates(final SugiyamaHierarchy.Hierarchy h){
		final Map<String, Double> x = new HashMap<>();

		// Compute the widest layer, used to center every layer.
		int maxUnits = 0;
		final List<List<String>> layers = h.layers();
		for(int l = 0; l < layers.size(); l ++){
			final List<String> layer = layers.get(l);

			int u = 0;
			for(int i = 0; i < layer.size(); i ++){
				u += widthOf(layer.get(i), h.dummyIds());
				if(i < layer.size() - 1)
					u += HORIZONTAL_GAP;
			}
			maxUnits = Math.max(maxUnits, u);
		}

		// Layer 0: no parents to sort by, keep the path order.
		placeEvenly(h.layers().get(0), x, maxUnits, h);

		// Layers 1..N: sort by average parent X, then place evenly.
		for(int l = 1; l < layers.size(); l++){
			final List<String> layer = layers.get(l);

			// Average X of the parents of each node. Nodes with no parent
			// X available (orphans, partners whose parents are not in the
			// graph) get Double.MAX_VALUE and go to the end.
			final Map<String, Double> parentAvgX = new HashMap<>(layer.size());
			for(int i = 0; i < layer.size(); i ++){
				final String id = layer.get(i);

				final List<String> parents = h.parentsOf().getOrDefault(id, List.of());
				double sum = 0;
				int n = 0;
				for(int j = 0; j < parents.size(); j ++){
					final String p = parents.get(j);

					final Double px = x.get(p);
					if(px != null){
						sum += px;
						n ++;
					}
				}
				parentAvgX.put(id, (n > 0? sum / n: Double.MAX_VALUE));
			}

			layer.sort(Comparator
				.comparingDouble((String id) -> parentAvgX.getOrDefault(id, Double.MAX_VALUE))
				.thenComparing(id -> id));

			placeEvenly(layer, x, maxUnits, h);
		}

		// Barycenter refinement, respects the order established above.
		for(int iter = 0; iter < REFINEMENT_ITERATIONS; iter ++)
			for(int l = 1, size = layers.size(); l < size; l ++)
				placeLayer(layers.get(l), h.parentsOf(), x, h.dummyIds());

		return x;
	}

	/**
	 * Places the nodes of a layer at even intervals, centered on the
	 * widest layer.
	 */
	private static void placeEvenly(final List<String> layer, final Map<String, Double> x, final int maxUnits,
			final SugiyamaHierarchy.Hierarchy h){
		int row = 0;
		for(int i = 0; i < layer.size(); i ++){
			row += widthOf(layer.get(i), h.dummyIds());
			if(i < layer.size() - 1)
				row += HORIZONTAL_GAP;
		}
		double cx = (maxUnits - row) / 2.;
		for(int i = 0; i < layer.size(); i ++){
			final String id = layer.get(i);

			final double w = widthOf(id, h.dummyIds());
			x.put(id, cx + w / 2.);
			cx += w + HORIZONTAL_GAP;
		}
	}

	private static void placeLayer(final List<String> layer, final Map<String, List<String>> neighborMap,
			final Map<String, Double> x, final Set<String> dummyIds){
		if(layer.size() <= 1)
			return;

		final Map<String, Double> targets = new HashMap<>(layer.size());
		for(int i = 0; i < layer.size(); i ++){
			final String id = layer.get(i);

			final List<String> neighbors = neighborMap.getOrDefault(id, List.of());
			double sum = 0;
			int n = 0;
			for(int j = 0; j < neighbors.size(); j ++){
				final String nb = neighbors.get(j);

				final Double nx = x.get(nb);
				if(nx != null){
					sum += nx;
					n ++;
				}
			}
			targets.put(id, (n > 0? sum / n: x.getOrDefault(id, 0.)));
		}

		double cursor = Double.NEGATIVE_INFINITY;
		for(int i = 0; i < layer.size(); i ++){
			final String id = layer.get(i);

			final double w = widthOf(id, dummyIds);
			final double halfW = w / 2.;
			final double target = targets.getOrDefault(id, 0.);
			final double cx = Math.max(target, cursor + halfW);
			x.put(id, cx);
			// Advance past the box right edge PLUS the gap, so the next
			// box in the same layer does not touch this one.
			cursor = cx + halfW + HORIZONTAL_GAP;
		}
	}


	/* ======================================================================
	 *                          Track coloring
	 * ====================================================================== */

	private record ParentGroupKey(List<String> sortedParents){}

	/**
	 * For every gap, count how many horizontal tracks are needed to
	 * route the sibling groups without overlaps.
	 */
	private static Map<Integer, Integer> computeHorizontalTrackCounts(final SugiyamaHierarchy.Hierarchy h,
			final Map<String, Double> xCoords){
		final Map<Integer, Integer> counts = new HashMap<>();
		final List<List<String>> layers = h.layers();
		for(int l = 0; l + 1 < layers.size(); l ++){
			// Group children of layer l+1 by their parent set
			final Map<ParentGroupKey, List<String>> childrenBySig = new LinkedHashMap<>();
			final List<String> nextLayer = layers.get(l + 1);
			for(int i = 0; i < nextLayer.size(); i ++){
				final String childId = nextLayer.get(i);

				final List<String> parents = h.parentsOf().getOrDefault(childId, List.of());
				if(parents.isEmpty())
					continue;

				final List<String> sorted = new ArrayList<>(parents);
				Collections.sort(sorted);
				childrenBySig.computeIfAbsent(new ParentGroupKey(sorted), k -> new ArrayList<>())
					.add(childId);
			}

			// Compute the horizontal span of each group
			final List<int[]> spans = new ArrayList<>();
			for(final Map.Entry<ParentGroupKey, List<String>> entry : childrenBySig.entrySet()){
				final List<String> sig = entry.getKey()
					.sortedParents();
				final List<String> children = entry.getValue();

				// Collapse: one parent, one child, aligned
				if(sig.size() == 1 && children.size() == 1){
					final double px = xCoords.getOrDefault(sig.getFirst(), 0.);
					final double cx = xCoords.getOrDefault(children.getFirst(), 0.);
					if(Math.abs(px - cx) < 0.5)
						continue;
				}
				int minC = Integer.MAX_VALUE, maxC = Integer.MIN_VALUE;
				for(int i = 0; i < sig.size(); i ++){
					final String p = sig.get(i);

					final int pc = (int)Math.round(xCoords.getOrDefault(p, 0.));
					minC = Math.min(minC, pc);
					maxC = Math.max(maxC, pc);
				}
				for(int i = 0; i < children.size(); i ++){
					final String c = children.get(i);

					final int cc = (int)Math.round(xCoords.getOrDefault(c, 0.));
					minC = Math.min(minC, cc);
					maxC = Math.max(maxC, cc);
				}
				spans.add(new int[]{minC, maxC});
			}

			// Greedy interval coloring
			spans.sort(Comparator
				.comparingInt((int[] s) -> s[0])
				.thenComparingInt(s -> -s[1]));

			final List<Integer> rightEnd = new ArrayList<>();
			for(int i = 0; i < spans.size(); i ++){
				final int[] span = spans.get(i);

				int chosen = -1;
				for(int j = 0; j < rightEnd.size(); j ++)
					if(rightEnd.get(j) < span[0]){
						chosen = j;
						rightEnd.set(j, span[1]);

						break;
					}
				if(chosen < 0)
					rightEnd.add(span[1]);
			}
			counts.put(l, rightEnd.size());
		}
		return counts;
	}

}
