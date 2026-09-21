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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Draws the edges of the Sugiyama graph as orthogonal polylines.
 * <p>
 * Every edge goes from the bottom-center of the source panel to the
 * top-center of the target panel. Children that share the same parents
 * share a single horizontal bar, with one drop per child: this merges
 * the descent from a couple into one visual line, as in a pedigree.
 */
public final class SugiyamaEdgeRouter{

	/**
	 * Background color of the Sugiyama canvas. Matches the background of the
	 * ancestor tree so that the two views look visually consistent when
	 * swapped through the projection switcher.
	 */
	public static final Color BACKGROUND_COLOR = new Color(242, 238, 228);
	public static final Color EDGE_COLOR = new Color(90, 90, 100, 200);

	// OPTIMIZATION: Static stroke to avoid garbage generation on each render cycle
	private static final Stroke EDGE_STROKE = new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	private SugiyamaEdgeRouter(){}

	// Light-weight key for group mapping without String joining/splitting
	private record ParentGroupKey(List<String> parentIds){}

	public static void drawEdges(final Graphics2D g2, final SugiyamaGraphLayout.Result layout){
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(EDGE_COLOR);
		g2.setStroke(EDGE_STROKE);

		// Group edges by child signature (parent set)
		final Map<String, List<SugiyamaHierarchy.Edge>> byChild = new LinkedHashMap<>();
		final List<SugiyamaHierarchy.Edge> edges = layout.edges();
		for(int i = 0; i < edges.size(); i ++){
			final SugiyamaHierarchy.Edge e = edges.get(i);
			byChild.computeIfAbsent(e.toId(), k -> new ArrayList<>()).add(e);
		}

		final Map<ParentGroupKey, List<String>> childrenBySig = new LinkedHashMap<>();
		for(final Map.Entry<String, List<SugiyamaHierarchy.Edge>> entry : byChild.entrySet()){
			final List<String> parentIds = new ArrayList<>();
			final List<SugiyamaHierarchy.Edge> edgeList = entry.getValue();
			for(int i = 0; i < edgeList.size(); i ++)
				parentIds.add(edgeList.get(i).fromId());

			Collections.sort(parentIds);
			childrenBySig.computeIfAbsent(new ParentGroupKey(parentIds), k -> new ArrayList<>())
				.add(entry.getKey());
		}

		final Map<String, Rectangle> nodeBounds = layout.nodeBounds();
		final Set<String> dummyIds = layout.dummyIds();
		for(final Map.Entry<ParentGroupKey, List<String>> group : childrenBySig.entrySet()){
			final List<String> parentIds = group.getKey().parentIds();
			final List<String> childIds = group.getValue();

			final List<Rectangle> parentBoxes = new ArrayList<>(parentIds.size());
			for(int i = 0; i < parentIds.size(); i ++){
				final String pid = parentIds.get(i);
				final Rectangle r = nodeBounds.get(pid);
				if(r != null && !dummyIds.contains(pid))
					parentBoxes.add(r);
			}

			final List<Rectangle> childBoxes = new ArrayList<>(childIds.size());
			for(int i = 0; i < childIds.size(); i ++){
				final String cid = childIds.get(i);
				final Rectangle r = nodeBounds.get(cid);
				if(r != null && !dummyIds.contains(cid))
					childBoxes.add(r);
			}

			if(parentBoxes.isEmpty() || childBoxes.isEmpty()){
				// Dummy-only chain: draw a simple vertical drop between
				// the top of the child and the bottom of the parent,
				// using the bounds directly.
				drawDummyChain(g2, parentIds, childIds, layout);

				continue;
			}

			// Bar Y: midway between the bottom of the parents and the top of the children
			int topY = Integer.MAX_VALUE;
			int botY = Integer.MIN_VALUE;
			int minCx = Integer.MAX_VALUE;
			int maxCx = Integer.MIN_VALUE;

			for(int i = 0; i < parentBoxes.size(); i ++){
				final Rectangle r = parentBoxes.get(i);
				final int cx = r.x + r.width / 2;
				minCx = Math.min(minCx, cx);
				maxCx = Math.max(maxCx, cx);
				topY = Math.min(topY, r.y + r.height);
			}
			for(int i = 0; i < childBoxes.size(); i ++){
				final Rectangle r = childBoxes.get(i);
				final int cx = r.x + r.width / 2;
				minCx = Math.min(minCx, cx);
				maxCx = Math.max(maxCx, cx);
				botY = Math.max(botY, r.y);
			}
			final int barY = (topY + botY) / 2;

			g2.drawLine(minCx, barY, maxCx, barY);

			for(int i = 0; i < parentBoxes.size(); i ++){
				final Rectangle r = parentBoxes.get(i);
				final int cx = r.x + r.width / 2;
				final int cy = r.y + r.height;
				if(cy < barY)
					g2.drawLine(cx, cy, cx, barY);
			}

			for(int i = 0; i < childBoxes.size(); i ++){
				final Rectangle r = childBoxes.get(i);
				final int cx = r.x + r.width / 2;
				final int cy = r.y;
				if(barY < cy)
					g2.drawLine(cx, barY, cx, cy);
			}
		}
	}

	private static void drawDummyChain(final Graphics2D g2, final List<String> parentIds, final List<String> childIds,
			final SugiyamaGraphLayout.Result layout){
		final Map<String, Rectangle> nodeBounds = layout.nodeBounds();
		int prevX = Integer.MIN_VALUE;
		int prevY = Integer.MIN_VALUE;
		for(int i = 0; i < parentIds.size(); i ++){
			final Rectangle r = nodeBounds.get(parentIds.get(i));
			if(r == null)
				continue;

			final int cx = r.x + r.width / 2;
			final int cy = r.y + r.height / 2;
			if(prevX != Integer.MIN_VALUE)
				g2.drawLine(prevX, prevY, cx, cy);

			prevX = cx;
			prevY = cy;
		}
		for(int i = 0; i < childIds.size(); i ++){
			final Rectangle r = nodeBounds.get(childIds.get(i));
			if(r == null)
				continue;

			final int cx = r.x + r.width / 2;
			final int cy = r.y + r.height / 2;
			if(prevX != Integer.MIN_VALUE)
				g2.drawLine(prevX, prevY, cx, cy);

			prevX = cx;
			prevY = cy;
		}
	}

}
