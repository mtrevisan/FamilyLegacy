/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * ... MIT license header ...
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.sugiyama;

import org.apache.commons.lang3.StringUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


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
	public static final Color BACKGROUND = new Color(242, 238, 228);
	public static final Color EDGE_COLOR = new Color(90, 90, 100, 200);

	private static final String PIPE = "|";


	private SugiyamaEdgeRouter(){}


	public static void drawEdges(final Graphics2D g2, final SugiyamaGraphLayout.Result layout){
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(EDGE_COLOR);
		g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		// Group edges by child signature (parent set)
		final Map<String, List<SugiyamaHierarchy.Edge>> byChild = new LinkedHashMap<>();
		for(final SugiyamaHierarchy.Edge e : layout.edges())
			byChild.computeIfAbsent(e.toId(), k -> new ArrayList<>()).add(e);

		final Map<String, List<String>> childrenBySig = new LinkedHashMap<>();
		for(final Map.Entry<String, List<SugiyamaHierarchy.Edge>> entry : byChild.entrySet()){
			final List<String> parentIds = new ArrayList<>();
			for(final SugiyamaHierarchy.Edge e : entry.getValue())
				parentIds.add(e.fromId());
			parentIds.sort(String::compareTo);
			childrenBySig.computeIfAbsent(String.join(PIPE, parentIds), k -> new ArrayList<>())
				.add(entry.getKey());
		}

		for(final Map.Entry<String, List<String>> group : childrenBySig.entrySet()){
			final String[] parentIds = StringUtils.split(group.getKey(), PIPE);
			final List<String> childIds = group.getValue();

			// Collect the parent exits and the child entries.
			final List<Rectangle> parentBoxes = new ArrayList<>(parentIds.length);
			for(final String pid : parentIds){
				final Rectangle r = layout.nodeBounds().get(pid);
				if(r != null && !layout.dummyIds().contains(pid))
					parentBoxes.add(r);
			}
			final List<Rectangle> childBoxes = new ArrayList<>(childIds.size());
			for(final String cid : childIds){
				final Rectangle r = layout.nodeBounds().get(cid);
				if(r != null && !layout.dummyIds().contains(cid))
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
			for(final Rectangle r : parentBoxes){
				minCx = Math.min(minCx, r.x + r.width / 2);
				maxCx = Math.max(maxCx, r.x + r.width / 2);
				topY = Math.min(topY, r.y + r.height);
			}
			for(final Rectangle r : childBoxes){
				minCx = Math.min(minCx, r.x + r.width / 2);
				maxCx = Math.max(maxCx, r.x + r.width / 2);
				botY = Math.max(botY, r.y);
			}
			final int barY = (topY + botY) / 2;

			// Horizontal bar
			g2.drawLine(minCx, barY,
				maxCx, barY);
			// Vertical from each parent down to the bar
			for(final Rectangle r : parentBoxes){
				final int cx = r.x + r.width / 2;
				final int cy = r.y + r.height;
				if(cy < barY)
					g2.drawLine(cx, cy,
						cx, barY);
			}
			// Vertical from the bar down to each child
			for(final Rectangle r : childBoxes){
				final int cx = r.x + r.width / 2;
				final int cy = r.y;
				if(barY < cy)
					g2.drawLine(cx, barY,
						cx, cy);
			}
		}
	}

	private static void drawDummyChain(final Graphics2D g2, final String[] parentIds, final List<String> childIds,
			final SugiyamaGraphLayout.Result layout){
		// Chain through dummies: draw the polyline along the vertical between consecutive dummy centers
		final List<String> chain = new ArrayList<>();
		chain.addAll(List.of(parentIds));
		chain.addAll(childIds);
		int prevX = Integer.MIN_VALUE, prevY = Integer.MIN_VALUE;
		for(final String id : chain){
			final Rectangle r = layout.nodeBounds().get(id);
			if(r == null)
				continue;

			final int cx = r.x + r.width / 2;
			final int cy = r.y + r.height / 2;
			if(prevX != Integer.MIN_VALUE)
				g2.drawLine(prevX, prevY,
					cx, cy);

			prevX = cx;
			prevY = cy;
		}
	}

}
