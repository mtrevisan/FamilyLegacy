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
package io.github.mtrevisan.familylegacy.ui.components.projections.relationshipgraph;

import io.github.mtrevisan.familylegacy.ego.EgoNode;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.Map;
import java.util.Set;


/**
 * Utility class for rendering orthogonal connection lines between the
 * central Ego node and satellite nodes.
 * <p>
 * Every line is dotted: FLEF does not record whether a relationship is still active, so the node carries no status that
 * could choose another style.
 * <p>
 * The edge to a partner that is related to the ego (see {@link EgoNode#getConsanguineousUnion()}) carries a badge "C" in the middle.
 * It is a possibility to look into, as in the consanguinity report: the explanation is in the tooltip of the partner.
 */
final class EgoNetworkRenderer{

	private static final Stroke STROKE_UNKNOWN = new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
		10f, new float[]{2f, 4f}, 0f);


	private static final int BADGE_RADIUS = 8;
	private static final Stroke BADGE_STROKE = new BasicStroke(1.2f);
	private static final Color BADGE_FILL = new Color(0xFFC107);
	private static final Color BADGE_BORDER = new Color(0x5D4037);
	private static final String BADGE_TEXT = "C";


	private EgoNetworkRenderer(){}


	/**
	 * Draws connection lines between the central Ego panel and all connected satellite panels.
	 *
	 * @param g2              the graphics context
	 * @param rootEgoNode     the root EgoNode containing relational structure
	 * @param nodeToPanelMap  map associating EgoNode objects to their UI JPanel
	 * @param groupToPanelMap map associating Group FLEFRecords to their UI JPanel
	 * @param container       the parent container for coordinate conversion
	 */
	public static void drawNetworkLines(final Graphics2D g2, final EgoNode rootEgoNode,
			final Map<EgoNode, JPanel> nodeToPanelMap, final Map<FLEFRecord, JPanel> groupToPanelMap,
			final Component container){
		if(rootEgoNode == null)
			return;

		final JPanel centerPanel = nodeToPanelMap.get(rootEgoNode);
		if(centerPanel == null)
			return;

		final Point centerPoint = getPanelCenter(centerPanel, container);

		// Connect to Parent nodes
		connectCategoryNodes(g2, centerPoint, rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.PARENT),
			nodeToPanelMap, container);

		// Connect to Partner nodes
		connectCategoryNodes(g2, centerPoint, rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.PARTNER),
			nodeToPanelMap, container);

		// Connect to Child nodes
		connectCategoryNodes(g2, centerPoint, rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.CHILD),
			nodeToPanelMap, container);

		// Connect to Associate nodes
		connectCategoryNodes(g2, centerPoint, rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.ASSOCIATE),
			nodeToPanelMap, container);

		// Connect to Group cards
		connectGroupRecords(g2, centerPoint, rootEgoNode, groupToPanelMap, container);
	}

	private static void connectCategoryNodes(final Graphics2D g2, final Point centerPoint, final Set<EgoNode> nodes,
			final Map<EgoNode, JPanel> nodeToPanelMap, final Component container){
		for(final EgoNode node : nodes){
			final JPanel targetPanel = nodeToPanelMap.get(node);
			if(targetPanel != null){
				final Point targetPoint = getPanelCenter(targetPanel, container);
				drawOrthogonalLine(g2, centerPoint, targetPoint, STROKE_UNKNOWN);
				//only partners carry a union mark
				if(node.getConsanguineousUnion() != null)
					drawConsanguinityBadge(g2, lineMidpoint(centerPoint, targetPoint));
			}
		}
	}

	private static void connectGroupRecords(final Graphics2D g2, final Point centerPoint,
			final EgoNode rootEgoNode, final Map<FLEFRecord, JPanel> groupToPanelMap, final Component container){
		for(final FLEFRecord groupRecord : rootEgoNode.getGroupRecords()){
			final JPanel targetPanel = groupToPanelMap.get(groupRecord);
			if(targetPanel != null){
				final Point targetPoint = getPanelCenter(targetPanel, container);
				drawOrthogonalLine(g2, centerPoint, targetPoint, STROKE_UNKNOWN);
			}
		}
	}

	private static Point getPanelCenter(final JPanel panel, final Component container){
		final Point localCenter = new Point(panel.getWidth() / 2, panel.getHeight() / 2);
		return SwingUtilities.convertPoint(panel, localCenter, container);
	}

	/** @return	The middle of the line drawn by {@link #drawOrthogonalLine}: on its vertical segment. */
	private static Point lineMidpoint(final Point start, final Point end){
		return new Point((start.x + end.x) / 2, (start.y + end.y) / 2);
	}

	/** Draws the badge that signals a consanguineous union: a small amber disc with a "C". */
	private static void drawConsanguinityBadge(final Graphics2D g2, final Point center){
		final Object antialiasing = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		final Font originalFont = g2.getFont();
		final Color originalColor = g2.getColor();
		final Stroke originalStroke = g2.getStroke();
		try{
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			final int x = center.x - BADGE_RADIUS;
			final int y = center.y - BADGE_RADIUS;
			final int diameter = 2 * BADGE_RADIUS;

			g2.setStroke(BADGE_STROKE);
			g2.setColor(BADGE_FILL);
			g2.fillOval(x, y, diameter, diameter);
			g2.setColor(BADGE_BORDER);
			g2.drawOval(x, y, diameter, diameter);

			g2.setFont(originalFont.deriveFont(Font.BOLD, BADGE_RADIUS + 1f));
			final FontMetrics metrics = g2.getFontMetrics();
			g2.drawString(BADGE_TEXT, center.x - metrics.stringWidth(BADGE_TEXT) / 2,
				center.y + (metrics.getAscent() - metrics.getDescent()) / 2);
		}
		finally{
			g2.setStroke(originalStroke);
			g2.setColor(originalColor);
			g2.setFont(originalFont);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				(antialiasing != null? antialiasing: RenderingHints.VALUE_ANTIALIAS_DEFAULT));
		}
	}

	/**
	 * Draws an orthogonal stepped line connecting two points.
	 * <p>
	 * When the two points share the same X or Y coordinate, the intermediate
	 * segments degenerate to zero length; in that case a straight line is
	 * drawn directly to avoid rendering artifacts produced by some
	 * platform-specific stroke implementations on zero-length segments.
	 */
	private static void drawOrthogonalLine(final Graphics2D g2, final Point start, final Point end, final Stroke stroke){
		final Stroke original = g2.getStroke();
		try{
			g2.setStroke(stroke);
			if(start.x == end.x || start.y == end.y){
				g2.drawLine(start.x, start.y,
					end.x, end.y);

				return;
			}

			final int midX = (start.x + end.x) / 2;

			// Horizontal segment to midpoint X
			g2.drawLine(start.x, start.y,
				midX, start.y);

			// Vertical segment to end Y
			g2.drawLine(midX, start.y,
				midX, end.y);

			// Horizontal segment to end X
			g2.drawLine(midX, end.y,
				end.x, end.y);
		}
		finally{
			g2.setStroke(original);
		}
	}

}
