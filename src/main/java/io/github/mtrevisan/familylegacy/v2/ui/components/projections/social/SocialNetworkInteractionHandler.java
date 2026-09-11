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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.List;


/**
 * Static helpers for hit-testing and tooltip generation in the Social
 * Network view.
 * <p>
 * The handler is stateless: the caller passes the graph and the layout on
 * every query. Hit testing is delegated to the layout, which owns the
 * node bounding boxes. Tooltip generation formats the node metadata
 * (name, degree, primary category) and, when the node is not the focus,
 * the list of relationships that connect it to the focus or to its
 * neighbours.
 */
public final class SocialNetworkInteractionHandler{

	private SocialNetworkInteractionHandler(){
	}


	/**
	 * Returns the node under the given point, or {@code null} if no node
	 * is hit. Coordinates must be expressed in the layout coordinate
	 * system, not in screen coordinates.
	 *
	 * @param x      the X coordinate in layout space
	 * @param y      the Y coordinate in layout space
	 * @param layout the layout (must not be {@code null})
	 * @return the hit node, or {@code null}
	 */
	public static SocialNodeRef hitTest(final int x, final int y, final SocialLayout layout){
		if(layout == null || layout.isEmpty())
			return null;
		return layout.hitTest(x, y);
	}

	/**
	 * Builds an HTML tooltip for the given node.
	 *
	 * @param graph the graph the node belongs to (must not be {@code null})
	 * @param node  the node (must not be {@code null})
	 * @return the tooltip text, or {@code null} if no text is available
	 */
	public static String buildTooltip(final SocialGraph graph, final SocialNodeRef node){
		if(graph == null || node == null)
			return null;

		final StringBuilder sb = new StringBuilder("<html>");
		sb.append("<b>").append(escape(node.entity()
			.displayLabel())).append("</b>");
		sb.append("<br>Degree: ").append(node.degree());
		sb.append("<br>Primary role: ").append(escape(node.primaryCategory()
			.getDisplayLabel()));

		final List<SocialEdgeRef> incident = graph.edgesOf(node.id());
		if(!incident.isEmpty()){
			sb.append("<br><i>").append(incident.size())
				.append(incident.size() == 1? " relationship": " relationships")
				.append("</i>");
		}

		if(!node.isCenter())
			sb.append("<br><i>Double-click to edit, right-click for actions</i>");

		return sb.append("</html>").toString();
	}

	/**
	 * Builds an HTML tooltip for an edge, used when the user hovers over a
	 * connector. The edge is identified by the two endpoints and the
	 * relationship record id.
	 *
	 * @param edge the edge (must not be {@code null})
	 * @return the tooltip text
	 */
	public static String buildEdgeTooltip(final SocialEdgeRef edge){
		if(edge == null)
			return null;

		final StringBuilder sb = new StringBuilder("<html>");
		sb.append("<b>").append(escape(edge.relationshipType())).append("</b>");
		if(edge.hasRole())
			sb.append("<br>Role: ").append(escape(edge.role()));
		sb.append("<br>Category: ").append(escape(edge.category()
			.getDisplayLabel()));
		sb.append("<br>Status: ").append(escape(edge.span()
			.status()));
		if(edge.isDirected())
			sb.append("<br>Direction: subject → target");
		return sb.append("</html>").toString();
	}

	/**
	 * Returns the FLEF record backing the given node.
	 *
	 * @param node the node (must not be {@code null})
	 * @return the backing record, never {@code null}
	 */
	public static FLEFRecord sourceRecordOf(final SocialNodeRef node){
		return node.entity()
			.record();
	}


	private static String escape(final String text){
		if(text == null)
			return StringUtils.EMPTY;
		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}

}
