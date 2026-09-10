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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree;

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsPanel;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;


/**
 * Utility class for rendering genealogical tree connections.
 */
public final class IndividualTreeRenderer{

	private IndividualTreeRenderer(){}


	/**
	 * Draws all connection lines for the genealogical tree.
	 *
	 * @param g2                 the graphics context
	 * @param rootNode           the root node of the tree
	 * @param nodeToPanelMap     map from nodes to their UI panels
	 * @param childrenPanel      the children panel
	 * @param container          the parent container for coordinate conversion
	 */
	public static void drawTree(final Graphics2D g2, final TreeLayout treeLayout, final AncestorNode rootNode,
			final Map<AncestorNode, PartnersPanel> nodeToPanelMap, final SiblingsPanel childrenPanel,
			final Component container){
		if(rootNode == null)
			return;

		drawTreeConnections(g2, treeLayout, rootNode, nodeToPanelMap, container);
		if(!childrenPanel.getSiblingBoxes().isEmpty())
			drawChildrenConnections(g2, treeLayout, rootNode, nodeToPanelMap, childrenPanel, container);
	}

	/**
	 * Iteratively traverses all tree nodes using a Queue (BFS) to draw connection lines between parents and children.
	 */
	private static void drawTreeConnections(final Graphics2D g2, final TreeLayout treeLayout,
			final AncestorNode rootNode, final Map<AncestorNode, PartnersPanel> nodeToPanelMap, final Component container){
		final Queue<AncestorNode> queue = new ArrayDeque<>();
		queue.add(rootNode);
		while(!queue.isEmpty()){
			final AncestorNode node = queue.poll();
			final PartnersPanel nodePanel = nodeToPanelMap.get(node);

			final AncestorNode father = node.getFather();
			if(father != null){
				final PartnersPanel fatherPanel = nodeToPanelMap.get(father);
				if(fatherPanel != null && nodePanel != null){
					Point enter = nodePanel.getPaintingFatherEnterPoint();
					enter = SwingUtilities.convertPoint(nodePanel, enter, container);
					connectParentToChild(g2, treeLayout, fatherPanel, enter, container);
				}
				queue.add(father);
			}

			final AncestorNode mother = node.getMother();
			if(mother != null){
				final PartnersPanel motherPanel = nodeToPanelMap.get(mother);
				if(motherPanel != null && nodePanel != null){
					Point enter = nodePanel.getPaintingMotherEnterPoint();
					enter = SwingUtilities.convertPoint(nodePanel, enter, container);
					connectParentToChild(g2, treeLayout, motherPanel, enter, container);
				}
				queue.add(mother);
			}
		}
	}

	/**
	 * Connects a parent/super-group panel to a child/member panel with orthogonal lines.
	 */
	private static void connectParentToChild(final Graphics2D g2, final TreeLayout treeLayout,
			final PartnersPanel parentGroupPanel, final Point childEnterPoint, final Component container){
		Point parentExit = parentGroupPanel.getPaintingExitPoint();
		parentExit = SwingUtilities.convertPoint(parentGroupPanel, parentExit, container);

		if(treeLayout == TreeLayout.VERTICAL){
			// Vertical line extending out from parent group
			final int midY = (childEnterPoint.y + parentExit.y + PartnersPanel.GROUP_EXITING_HEIGHT) / 2;
			g2.drawLine(parentExit.x, parentExit.y,
				parentExit.x, midY);

			// Vertical line entering into child panel
			g2.drawLine(childEnterPoint.x, childEnterPoint.y,
				childEnterPoint.x, midY);

			// Horizontal connecting line
			g2.drawLine(parentExit.x, midY,
				childEnterPoint.x, midY);
		}
		else{
			// Horizontal line extending leftwards/rightwards between child and parent
			g2.drawLine(parentExit.x, parentExit.y,
				childEnterPoint.x, parentExit.y);
			g2.drawLine(childEnterPoint.x, parentExit.y,
				childEnterPoint.x, childEnterPoint.y);
		}
	}

	private static void drawChildrenConnections(final Graphics2D g2, final TreeLayout treeLayout,
			final AncestorNode rootNode, final Map<AncestorNode, PartnersPanel> nodeToPanelMap,
			final SiblingsPanel childrenPanel, final Component container){
		if(childrenPanel == null)
			return;

		final PartnersPanel homePanel = nodeToPanelMap.get(rootNode);
		if(homePanel != null){
			Point homeExit = homePanel.getPaintingExitPoint();
			homeExit = SwingUtilities.convertPoint(homePanel, homeExit, container);
			if(treeLayout == TreeLayout.VERTICAL){
				final Point childrenRight = SwingUtilities.convertPoint(childrenPanel,
					new Point(0, 0), container);

				g2.drawLine(homeExit.x, homeExit.y,
					homeExit.x, childrenRight.y);
			}
			else{
				final Point[] enterPoints = childrenPanel.getPaintingEnterPoints();
				if(enterPoints.length > 0){
					final Point targetPoint = SwingUtilities.convertPoint(childrenPanel, enterPoints[0], container);
					g2.drawLine(homeExit.x, homeExit.y,
						targetPoint.x, homeExit.y);
				}
			}
		}
	}

}
