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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.social;

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalSpan;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Set;


/**
 * Draws the edges of the Social Network views.
 * <p>
 * Edges are drawn between the centers of the two node cards, using the
 * category color provided by {@link SocialNodeRenderer#colorForCategory}.
 * The stroke style encodes the {@link TemporalSpan#status()}:
 * <ul>
 *   <li>{@code active} → solid line</li>
 *   <li>{@code ended} → dashed line</li>
 *   <li>{@code unknown} → dotted line</li>
 * </ul>
 * Directed edges ({@link SocialEdgeDirection#DIRECTED}) receive a small
 * arrow head at the target end. Highlighted edges (typically the result of
 * a path query) are drawn with full opacity and a thicker stroke; all other
 * edges are drawn with reduced opacity so that the highlighted path stands
 * out without being obscured.
 */
public final class SocialEdgeRenderer{

	/** Base stroke width of a regular edge. */
	private static final float REGULAR_THICKNESS = 1.2f;

	/** Base stroke width of a highlighted edge. */
	private static final float HIGHLIGHT_THICKNESS = 2.4f;

	/** Opacity applied to non-highlighted edges when a highlight set is present. */
	private static final int DIMMED_ALPHA = 60;

	/** Full opacity applied to edges when no highlight set is present. */
	private static final int FULL_ALPHA = 200;

	/** Size of the arrow head, in pixels. */
	private static final int ARROW_SIZE = 8;

	/** Distance, in pixels, by which an edge is shortened at each end to avoid overlapping the node cards. */
	private static final int EDGE_INSET = 6;


	private SocialEdgeRenderer(){
	}


	/**
	 * Draws all edges of the graph.
	 *
	 * @param g           the graphics context
	 * @param graph       the graph providing the edges
	 * @param nodeBounds  the map of node bounds provided by
	 *                    {@link SocialLayout#nodeBounds()}
	 * @param highlighted the set of edges to draw in full opacity; may be
	 *                    {@code null} or empty, in which case all edges are
	 *                    drawn at full opacity
	 */
	public static void drawEdges(final Graphics2D g, final SocialGraph graph,
		final java.util.Map<SocialNodeRef, Rectangle> nodeBounds,
		final Set<SocialEdgeRef> highlighted){
		if(g == null || graph == null || nodeBounds == null || graph.isEmpty())
			return;

		final boolean hasHighlight = (highlighted != null && !highlighted.isEmpty());

		for(final SocialEdgeRef edge : graph.edges()){
			final Rectangle sourceBounds = nodeBounds.get(graph.findNode(edge.source()
				.id()));
			final Rectangle targetBounds = nodeBounds.get(graph.findNode(edge.target()
				.id()));
			if(sourceBounds == null || targetBounds == null)
				continue;

			final boolean isHighlighted = (highlighted != null && highlighted.contains(edge));
			drawEdge(g, edge, sourceBounds, targetBounds, isHighlighted, hasHighlight);
		}
	}

	/**
	 * Draws the edges contained in the given path with a dedicated
	 * highlight stroke, regardless of the highlight set. Useful when a path
	 * is selected and must be shown on top of everything.
	 *
	 * @param g          the graphics context
	 * @param path       the path to draw; may be {@code null}
	 * @param nodeBounds the map of node bounds
	 */
	public static void drawPathHighlight(final Graphics2D g, final SocialPath path,
		final java.util.Map<SocialNodeRef, Rectangle> nodeBounds){
		if(g == null || path == null || nodeBounds == null)
			return;

		final Stroke originalStroke = g.getStroke();
		try{
			g.setStroke(new BasicStroke(HIGHLIGHT_THICKNESS + 0.6f, BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND));
			g.setColor(new Color(220, 100, 60, 220));

			for(final SocialEdgeRef edge : path.edges()){
				final Rectangle sourceBounds = findBounds(nodeBounds, edge.source()
					.id());
				final Rectangle targetBounds = findBounds(nodeBounds, edge.target()
					.id());
				if(sourceBounds == null || targetBounds == null)
					continue;

				final Point2D source = trimTo(sourceBounds, centerOf(targetBounds));
				final Point2D target = trimTo(targetBounds, centerOf(sourceBounds));
				g.draw(new Line2D.Double(source, target));
			}
		}
		finally{
			g.setStroke(originalStroke);
		}
	}


	/* ======================================================================
	 *                          Single edge
	 * ====================================================================== */

	private static void drawEdge(final Graphics2D g, final SocialEdgeRef edge, final Rectangle sourceBounds,
		final Rectangle targetBounds, final boolean highlighted, final boolean hasHighlight){
		final Color baseColor = SocialNodeRenderer.colorForCategory(edge.category());
		final int alpha = (highlighted? FULL_ALPHA: (hasHighlight? DIMMED_ALPHA: FULL_ALPHA));
		final Color color = withAlpha(baseColor, alpha);

		final Point2D sourceCenter = centerOf(sourceBounds);
		final Point2D targetCenter = centerOf(targetBounds);
		final Point2D source = trimTo(sourceBounds, targetCenter);
		final Point2D target = trimTo(targetBounds, sourceCenter);

		final Stroke originalStroke = g.getStroke();
		try{
			g.setStroke(strokeFor(edge, highlighted));
			g.setColor(color);
			g.draw(new Line2D.Double(source, target));

			if(edge.isDirected()){
				g.setColor(color);
				drawArrowHead(g, source, target);
			}
		}
		finally{
			g.setStroke(originalStroke);
		}
	}

	private static Stroke strokeFor(final SocialEdgeRef edge, final boolean highlighted){
		final float thickness = (highlighted? HIGHLIGHT_THICKNESS: REGULAR_THICKNESS);
		return switch(edge.span()
			.status()){
			case TemporalSpan.STATUS_ACTIVE -> new BasicStroke(thickness, BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND);
			case TemporalSpan.STATUS_ENDED -> new BasicStroke(thickness, BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_ROUND, 10f, new float[]{6f, 4f}, 0f);
			default -> new BasicStroke(thickness, BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_ROUND, 10f, new float[]{2f, 4f}, 0f);
		};
	}


	/* ======================================================================
	 *                          Geometry helpers
	 * ====================================================================== */

	private static Point2D centerOf(final Rectangle bounds){
		return new Point2D.Double(
			bounds.x + bounds.width / 2.,
			bounds.y + bounds.height / 2.);
	}

	/**
	 * Returns the point where the segment from {@code from} to the center
	 * of {@code bounds} intersects the border of {@code bounds}, inset by
	 * {@link #EDGE_INSET} pixels so that the edge does not touch the card.
	 */
	private static Point2D trimTo(final Rectangle bounds, final Point2D from){
		final Point2D center = centerOf(bounds);
		final double dx = center.getX() - from.getX();
		final double dy = center.getY() - from.getY();
		final double distance = Math.hypot(dx, dy);
		if(distance < 1.)
			return center;

		// Approximate intersection with the bounding box: walk from the
		// center toward the other point until we exit the rectangle.
		final double ux = dx / distance;
		final double uy = dy / distance;

		final double halfWidth = bounds.width / 2.;
		final double halfHeight = bounds.height / 2.;

		final double tx = (Math.abs(ux) > 1e-9? halfWidth / Math.abs(ux): Double.POSITIVE_INFINITY);
		final double ty = (Math.abs(uy) > 1e-9? halfHeight / Math.abs(uy): Double.POSITIVE_INFINITY);
		final double t = Math.min(tx, ty);

		return new Point2D.Double(
			center.getX() - ux * (t + EDGE_INSET),
			center.getY() - uy * (t + EDGE_INSET));
	}

	private static void drawArrowHead(final Graphics2D g, final Point2D from, final Point2D to){
		final double dx = to.getX() - from.getX();
		final double dy = to.getY() - from.getY();
		final double distance = Math.hypot(dx, dy);
		if(distance < 1.)
			return;
		final double ux = dx / distance;
		final double uy = dy / distance;

		// Arrow head base at the target end.
		final double tipX = to.getX();
		final double tipY = to.getY();
		final double backX = tipX - ux * ARROW_SIZE;
		final double backY = tipY - uy * ARROW_SIZE;
		final double perpX = -uy;
		final double perpY = ux;

		g.draw(new Line2D.Double(tipX, tipY, backX + perpX * ARROW_SIZE * 0.5, backY + perpY * ARROW_SIZE * 0.5));
		g.draw(new Line2D.Double(tipX, tipY, backX - perpX * ARROW_SIZE * 0.5, backY - perpY * ARROW_SIZE * 0.5));
	}

	private static Color withAlpha(final Color color, final int alpha){
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	private static Rectangle findBounds(final java.util.Map<SocialNodeRef, Rectangle> nodeBounds, final String id){
		for(final java.util.Map.Entry<SocialNodeRef, Rectangle> entry : nodeBounds.entrySet())
			if(entry.getKey()
				.id()
				.equals(id))
				return entry.getValue();
		return null;
	}

}
