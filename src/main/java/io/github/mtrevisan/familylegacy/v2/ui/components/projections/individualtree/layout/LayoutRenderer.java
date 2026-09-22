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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout;

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeNode;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsPanel;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Point;
import java.awt.geom.Path2D;
import java.util.Map;


public final class LayoutRenderer{

	private LayoutRenderer(){}


	/**
	 * Builds a cached {@link Path2D} containing all connection lines for the tree.
	 */
	public static Path2D buildTreePath(final TreeLayout treeLayout, final TreeNode rootNode,
			final Map<TreeNode, PartnersPanel> nodeToPanelMap, final SiblingsPanel childrenPanel, final JPanel canvas){
		final Path2D path = new Path2D.Double();
		if(rootNode == null || nodeToPanelMap.isEmpty())
			return path;

		buildTreeConnections(path, treeLayout, rootNode, nodeToPanelMap, canvas);
		if(childrenPanel != null && !childrenPanel.getSiblingBoxes().isEmpty()){
			final PartnersPanel homePanel = nodeToPanelMap.get(rootNode);
			if(homePanel != null)
				buildChildrenConnections(path, treeLayout, homePanel, childrenPanel, canvas);
		}

		return path;
	}

	private static void buildTreeConnections(final Path2D path, final TreeLayout treeLayout, final TreeNode node,
			final Map<TreeNode, PartnersPanel> nodeToPanelMap, final Component container){
		if(node == null)
			return;

		final PartnersPanel nodePanel = nodeToPanelMap.get(node);

		final TreeNode father = node.getFather();
		if(father != null){
			final PartnersPanel fatherPanel = nodeToPanelMap.get(father);
			if(fatherPanel != null && nodePanel != null){
				final Point enter = SwingUtilities.convertPoint(nodePanel, nodePanel.getPaintingFatherEnterPoint(), container);
				connectParentToChild(path, treeLayout, fatherPanel, enter, container);
			}
			buildTreeConnections(path, treeLayout, father, nodeToPanelMap, container);
		}

		final TreeNode mother = node.getMother();
		if(mother != null){
			final PartnersPanel motherPanel = nodeToPanelMap.get(mother);
			if(motherPanel != null && nodePanel != null){
				final Point enter = SwingUtilities.convertPoint(nodePanel, nodePanel.getPaintingMotherEnterPoint(), container);
				connectParentToChild(path, treeLayout, motherPanel, enter, container);
			}
			buildTreeConnections(path, treeLayout, mother, nodeToPanelMap, container);
		}
	}

	private static void connectParentToChild(final Path2D path, final TreeLayout treeLayout,
			final PartnersPanel parentGroupPanel, final Point childEnterPoint, final Component container){
		Point parentExit = parentGroupPanel.getPaintingExitPoint();
		parentExit = SwingUtilities.convertPoint(parentGroupPanel, parentExit, container);

		if(treeLayout == TreeLayout.VERTICAL){
			final int midY = (childEnterPoint.y + parentExit.y + PartnersPanel.GROUP_EXITING_HEIGHT) >> 1;

			path.moveTo(parentExit.x, parentExit.y);
			path.lineTo(parentExit.x, midY);
			path.lineTo(childEnterPoint.x, midY);
			path.lineTo(childEnterPoint.x, childEnterPoint.y);
		}
		else{
			path.moveTo(parentExit.x, parentExit.y);
			path.lineTo(childEnterPoint.x, parentExit.y);
			path.lineTo(childEnterPoint.x, childEnterPoint.y);
		}
	}

	private static void buildChildrenConnections(final Path2D path, final TreeLayout treeLayout,
			final PartnersPanel homePanel, final SiblingsPanel childrenPanel, final Component container){
		Point homeExit = homePanel.getPaintingExitPoint();
		homeExit = SwingUtilities.convertPoint(homePanel, homeExit, container);

		if(treeLayout == TreeLayout.VERTICAL){
			final Point childrenRight = SwingUtilities.convertPoint(childrenPanel, new Point(0, 1), container);
			path.moveTo(homeExit.x, homeExit.y);
			path.lineTo(homeExit.x, childrenRight.y);
		}
		else{
			final Point[] enterPoints = childrenPanel.getPaintingEnterPoints();
			if(enterPoints.length > 0){
				final Point targetPoint = SwingUtilities.convertPoint(childrenPanel, enterPoints[0], container);
				path.moveTo(homeExit.x, homeExit.y);
				path.lineTo(targetPoint.x, homeExit.y);
			}
		}
	}

}
