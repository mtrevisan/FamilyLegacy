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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.relationshipgraph;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Utility class for rendering orthogonal connection lines between the
 * central Ego node and satellite nodes.
 * <p>
 * The stroke style encodes the aggregate relationship status:
 * <ul>
 *   <li>{@code active} — solid line;</li>
 *   <li>{@code ended} — dashed line;</li>
 *   <li>{@code unknown} or mixed — dotted line.</li>
 * </ul>
 * When a satellite is reached through several relations with different
 * statuses, the "strongest" one wins: active beats ended, ended beats
 * unknown.
 */
final class EgoNetworkRenderer{

	private static final Stroke STROKE_ACTIVE = new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke STROKE_ENDED = new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
		10f, new float[]{6f, 4f}, 0f);
	private static final Stroke STROKE_UNKNOWN = new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
		10f, new float[]{2f, 4f}, 0f);


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
				final Stroke stroke = pickStroke(node.getRelationsWithEgo());
				drawOrthogonalLine(g2, centerPoint, targetPoint, stroke);
			}
		}
	}

	private static void connectGroupRecords(final Graphics2D g2, final Point centerPoint,
			final EgoNode rootEgoNode, final Map<FLEFRecord, JPanel> groupToPanelMap, final Component container){
		for(final FLEFRecord groupRecord : rootEgoNode.getGroupRecords()){
			final JPanel targetPanel = groupToPanelMap.get(groupRecord);
			if(targetPanel != null){
				final Point targetPoint = getPanelCenter(targetPanel, container);
				final Stroke stroke = pickStroke(rootEgoNode.getGroupRelationInfo(groupRecord));
				drawOrthogonalLine(g2, centerPoint, targetPoint, stroke);
			}
		}
	}

	/**
	 * Aggregates the statuses of a list of relations and returns the
	 * corresponding stroke.
	 */
	private static Stroke pickStroke(final List<EgoNode.RelationInfo> relations){
		if(relations == null || relations.isEmpty())
			return STROKE_UNKNOWN;

		boolean hasActive = false;
		boolean hasEnded = false;
		for(final EgoNode.RelationInfo info : relations){
			final String status = info.status();
			if(status == null)
				continue;
			if("active".equalsIgnoreCase(status))
				hasActive = true;
			else if("ended".equalsIgnoreCase(status))
				hasEnded = true;
		}
		if(hasActive)
			return STROKE_ACTIVE;
		if(hasEnded)
			return STROKE_ENDED;
		return STROKE_UNKNOWN;
	}

	private static Point getPanelCenter(final JPanel panel, final Component container){
		final Point localCenter = new Point(panel.getWidth() / 2, panel.getHeight() / 2);
		return SwingUtilities.convertPoint(panel, localCenter, container);
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
