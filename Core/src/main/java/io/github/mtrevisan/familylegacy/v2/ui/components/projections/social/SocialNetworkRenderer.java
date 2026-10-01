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

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalEntityRef;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Set;


/**
 * Orchestrates the rendering of the Social Network view.
 * <p>
 * The draw order is fixed and encodes the visual layering:
 * <ol>
 *   <li>background;</li>
 *   <li>edges at their regular opacity;</li>
 *   <li>node cards;</li>
 *   <li>highlighted path (if any) drawn on top of everything else;</li>
 *   <li>selection overlay on the selected node.</li>
 * </ol>
 * The renderer is stateless: it reads the graph, the layout and the current
 * selection on every repaint, so scroll, selection changes and re-layouts
 * are reflected immediately without caching.
 */
public final class SocialNetworkRenderer{

	/** Background color of the canvas. */
	public static final Color COLOR_BACKGROUND = new Color(250, 249, 245);


	private SocialNetworkRenderer(){
	}


	/**
	 * Draws the whole social network.
	 *
	 * @param g                the graphics context
	 * @param graph            the graph to draw (must not be {@code null})
	 * @param layout           the computed layout (must not be {@code null})
	 * @param contentBounds    the visible rectangle of the canvas
	 * @param selectedEntity   the currently selected node entity, or {@code null}
	 * @param highlightedEdges the set of edges to draw in full opacity; may
	 *                         be {@code null} or empty
	 * @param highlightedPath  the path to draw on top; may be {@code null}
	 */
	public static void draw(final Graphics2D g, final SocialGraph graph, final SocialLayout layout,
		final Rectangle contentBounds, final TemporalEntityRef selectedEntity,
		final Set<SocialEdgeRef> highlightedEdges, final SocialPath highlightedPath){
		if(g == null || graph == null || layout == null || contentBounds == null)
			return;

		// 1. Background.
		g.setColor(COLOR_BACKGROUND);
		g.fillRect(contentBounds.x, contentBounds.y, contentBounds.width, contentBounds.height);

		if(graph.isEmpty())
			return;

		// 2. Edges.
		SocialEdgeRenderer.drawEdges(g, graph, layout.nodeBounds(), highlightedEdges);

		// 3. Node cards.
		for(final SocialNodeRef node : graph.nodes()){
			final Rectangle bounds = layout.boundsOf(node);
			if(bounds == null)
				continue;
			final boolean selected = (selectedEntity != null && selectedEntity.equals(node.entity()));
			SocialNodeRenderer.drawNode(g, node, bounds, selected);
		}

		// 4. Path highlight on top of the nodes.
		if(highlightedPath != null)
			SocialEdgeRenderer.drawPathHighlight(g, highlightedPath, layout.nodeBounds());
	}

}
