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
package io.github.mtrevisan.familylegacy.v2.ui.components.grouptree;

import io.github.mtrevisan.familylegacy.v2.ui.components.biologicaltree.TreeLayout;
import io.github.mtrevisan.familylegacy.v2.ui.components.partners.PartnersPanel;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;


/**
 * Utility class for rendering connection lines in non-biological and group trees.
 */
public final class GroupIndividualTreeRenderer{

	private GroupIndividualTreeRenderer(){}


	/**
	 * Draws all connection lines for the group hierarchy tree.
	 *
	 * @param g2             the graphics context
	 * @param treeLayout     the orientation layout (VERTICAL or HORIZONTAL)
	 * @param rootNode       the root node of the tree
	 * @param nodeToPanelMap map associating tree nodes to their UI panels
	 * @param container      the parent container for coordinate conversion
	 */
	public static void drawTree(final Graphics2D g2, final TreeLayout treeLayout,
			final GroupTreeNode rootNode, final Map<GroupTreeNode, JPanel> nodeToPanelMap,
			final Component container){
		if(rootNode == null)
			return;

		drawGroupConnections(g2, treeLayout, rootNode, nodeToPanelMap, container);
	}

	/**
	 * Iteratively traverses the group tree nodes using BFS to draw connection lines
	 * between parents/super-groups and their members/children.
	 */
	private static void drawGroupConnections(final Graphics2D g2, final TreeLayout treeLayout,
			final GroupTreeNode rootNode, final Map<GroupTreeNode, JPanel> nodeToPanelMap,
			final Component container){
		final Queue<GroupTreeNode> queue = new ArrayDeque<>();
		queue.add(rootNode);
		while(!queue.isEmpty()){
			final GroupTreeNode currentNode = queue.poll();
			final JPanel currentPanel = nodeToPanelMap.get(currentNode);

			final List<GroupTreeNode> parents = currentNode.getParents();
			for(final GroupTreeNode parentNode : parents){
				final JPanel parentPanel = nodeToPanelMap.get(parentNode);
				if(parentPanel != null && currentPanel != null){
//					Point enter = currentPanel.getPaintingFatherEnterPoint();
//					enter = SwingUtilities.convertPoint(currentPanel, enter, container);
					connectParentToChild(g2, treeLayout, parentPanel, currentPanel, container);
				}
				queue.add(parentNode);
			}

			final List<GroupTreeNode> members = currentNode.getMembers();
			for(final GroupTreeNode memberNode : members){
				final JPanel memberPanel = nodeToPanelMap.get(memberNode);
				if(memberPanel != null && currentPanel != null){
//					Point enter = currentPanel.getPaintingMotherEnterPoint();
//					enter = SwingUtilities.convertPoint(currentPanel, enter, container);
					connectParentToChild(g2, treeLayout, currentPanel, memberPanel, container);
				}
				queue.add(memberNode);
			}
		}
	}

	/**
	 * Connects a parent/super-group panel to a child/member panel with orthogonal lines.
	 */
	private static void connectParentToChild(final Graphics2D g2, final TreeLayout treeLayout,
			final JPanel parentPanel, final JPanel childPanel, final Component container){
		Point parentExit = getPanelExitPoint(parentPanel, treeLayout);
		Point childEnter = getPanelEnterPoint(childPanel, treeLayout);

		parentExit = SwingUtilities.convertPoint(parentPanel, parentExit, container);
		childEnter = SwingUtilities.convertPoint(childPanel, childEnter, container);

		if(treeLayout == TreeLayout.VERTICAL){
			// Calculate midpoint Y for orthogonal step connection
			final int midY = (childEnter.y + parentExit.y + PartnersPanel.GROUP_EXITING_HEIGHT) / 2;

			// Line extending down from parent
			g2.drawLine(parentExit.x, parentExit.y,
				parentExit.x, midY);

			// Line entering into child top
			g2.drawLine(childEnter.x, childEnter.y,
				childEnter.x, midY);

			// Horizontal bus line connecting the two vertical segments
			g2.drawLine(parentExit.x, midY,
				childEnter.x, midY);
		}
		else{
			// Horizontal layout connection
			final int midX = (childEnter.x + parentExit.x) / 2;

			// Line extending right from parent
			g2.drawLine(parentExit.x, parentExit.y,
				midX, parentExit.y);

			// Vertical step line
			g2.drawLine(midX, parentExit.y,
				midX, childEnter.y);

			// Line entering into child left
			g2.drawLine(midX, childEnter.y,
				childEnter.x, childEnter.y);
		}
	}

	private static Point getPanelExitPoint(final JPanel panel, final TreeLayout treeLayout){
		if(panel instanceof PartnersPanel partnersPanel)
			return partnersPanel.getPaintingExitPoint();

		if(treeLayout == TreeLayout.VERTICAL)
			return new Point(panel.getWidth() / 2, panel.getHeight());
		else
			return new Point(panel.getWidth(), panel.getHeight() / 2);
	}

	private static Point getPanelEnterPoint(final JPanel panel, final TreeLayout treeLayout){
		if(panel instanceof PartnersPanel partnersPanel)
			return partnersPanel.getPaintingFatherEnterPoint();

		if(treeLayout == TreeLayout.VERTICAL)
			return new Point(panel.getWidth() / 2, 0);
		else
			return new Point(0, panel.getHeight() / 2);
	}

}
