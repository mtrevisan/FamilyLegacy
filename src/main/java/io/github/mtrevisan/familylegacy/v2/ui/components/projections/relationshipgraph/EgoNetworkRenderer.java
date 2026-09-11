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
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Map;
import java.util.Set;


/**
 * Utility class for rendering orthogonal connection lines between the central Ego node
 * and satellite nodes (Parents, Partners, Children, Groups, Associates).
 */
final class EgoNetworkRenderer{

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

		// 1. Connect to Parent nodes
		connectCategoryNodes(g2, centerPoint, rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.PARENT),
			nodeToPanelMap, container);

		// 2. Connect to Partner nodes
		connectCategoryNodes(g2, centerPoint, rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.PARTNER),
			nodeToPanelMap, container);

		// 3. Connect to Child nodes
		connectCategoryNodes(g2, centerPoint, rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.CHILD),
			nodeToPanelMap, container);

		// 4. Connect to Associate nodes
		connectCategoryNodes(g2, centerPoint, rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.ASSOCIATE),
			nodeToPanelMap, container);

		// 5. Connect to Group cards
		connectGroupRecords(g2, centerPoint, rootEgoNode.getGroupRecords(), groupToPanelMap, container);
	}

	private static void connectCategoryNodes(final Graphics2D g2, final Point centerPoint, final Set<EgoNode> nodes,
			final Map<EgoNode, JPanel> nodeToPanelMap, final Component container){
		for(final EgoNode node : nodes){
			final JPanel targetPanel = nodeToPanelMap.get(node);
			if(targetPanel != null){
				final Point targetPoint = getPanelCenter(targetPanel, container);
				drawOrthogonalLine(g2, centerPoint, targetPoint);
			}
		}
	}

	private static void connectGroupRecords(final Graphics2D g2, final Point centerPoint, final Set<FLEFRecord> groups,
			final Map<FLEFRecord, JPanel> groupToPanelMap, final Component container){
		for(final FLEFRecord groupRecord : groups){
			final JPanel targetPanel = groupToPanelMap.get(groupRecord);
			if(targetPanel != null){
				final Point targetPoint = getPanelCenter(targetPanel, container);
				drawOrthogonalLine(g2, centerPoint, targetPoint);
			}
		}
	}

	/**
	 * Computes the center coordinates of a Swing component relative to the specified container.
	 */
	private static Point getPanelCenter(final JPanel panel, final Component container){
		final Point localCenter = new Point(panel.getWidth() / 2, panel.getHeight() / 2);
		return SwingUtilities.convertPoint(panel, localCenter, container);
	}

	/**
	 * Draws an orthogonal stepped line connecting two points.
	 */
	private static void drawOrthogonalLine(final Graphics2D g2, final Point start, final Point end){
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

}
