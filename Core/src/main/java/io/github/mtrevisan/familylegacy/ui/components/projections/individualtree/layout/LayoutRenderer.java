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
package io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.layout;

import io.github.mtrevisan.familylegacy.tree.TreeNode;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.TreeLayout;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.PartnersPanel;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.SiblingsPanel;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Point;
import java.awt.geom.Path2D;
import java.util.Map;


public final class LayoutRenderer{

	/** The length of a dash and of a gap (pixels). */
	private static final double DASH_LENGTH = 2.;
	/** The distance of each of two side by side connections from the common axis (pixels). */
	private static final int PARALLEL_OFFSET = 2;

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
		final TreeNode mother = node.getMother();
		final PartnersPanel fatherPanel = (father != null? nodeToPanelMap.get(father): null);
		final PartnersPanel motherPanel = (mother != null? nodeToPanelMap.get(mother): null);

		//NOTE: the line drawn here for a parent goes from the panel of that parent to the panel of the node: it is the line of the
		//parent to ITS OWN parents, so the kind of the line (biological or not) is the one of the links of the parent, not of the node
		if(father != null){
			if(isDrawn(fatherPanel) && isDrawn(nodePanel) && nodePanel.getFatherPanel().isVisible())
				connectToParents(path, treeLayout, father,
					SwingUtilities.convertPoint(fatherPanel, fatherPanel.getPaintingExitPoint(), container),
					SwingUtilities.convertPoint(nodePanel, nodePanel.getPaintingFatherEnterPoint(), container));
			buildTreeConnections(path, treeLayout, father, nodeToPanelMap, container);
		}

		if(mother != null){
			if(isDrawn(motherPanel) && isDrawn(nodePanel) && nodePanel.getMotherPanel().isVisible())
				connectToParents(path, treeLayout, mother,
					SwingUtilities.convertPoint(motherPanel, motherPanel.getPaintingExitPoint(), container),
					SwingUtilities.convertPoint(nodePanel, nodePanel.getPaintingMotherEnterPoint(), container));
			buildTreeConnections(path, treeLayout, mother, nodeToPanelMap, container);
		}
	}

	/** A line goes only to boxes that are on the screen: no box (an ancestor that is not shown, a partner that is not there), no segment. */
	private static boolean isDrawn(final PartnersPanel panel){
		return (panel != null && panel.getParent() != null && panel.isVisible() && panel.getWidth() > 0 && panel.getHeight() > 0);
	}

	/**
	 * Draws the line between an individual and the couple of its parents.
	 * <p>
	 * A line is dashed if the individual is not a biological child of the parent it stands for. When the two parents are of a
	 * different kind (one biological, one not) the two lines run side by side: the father on the left (dashed if not biological), the
	 * mother on the right; when they are of the same kind there is only one line.
	 */
	private static void connectToParents(final Path2D path, final TreeLayout treeLayout, final TreeNode individual,
			final Point exit, final Point enter){
		final boolean hasFather = (individual.getFather() != null);
		final boolean hasMother = (individual.getMother() != null);
		final boolean fatherNonBiological = (hasFather && individual.isFatherNonBiological());
		final boolean motherNonBiological = (hasMother && individual.isMotherNonBiological());
		if(!hasFather || !hasMother || fatherNonBiological == motherNonBiological){
			connectParentToChild(path, treeLayout, exit, enter, fatherNonBiological || motherNonBiological);
			return;
		}

		final int dx = (treeLayout == TreeLayout.VERTICAL? PARALLEL_OFFSET: 0);
		final int dy = (treeLayout == TreeLayout.VERTICAL? 0: PARALLEL_OFFSET);
		connectParentToChild(path, treeLayout, new Point(exit.x - dx, exit.y - dy), new Point(enter.x - dx, enter.y - dy),
			fatherNonBiological);
		connectParentToChild(path, treeLayout, new Point(exit.x + dx, exit.y + dy), new Point(enter.x + dx, enter.y + dy),
			motherNonBiological);
	}

	private static void connectParentToChild(final Path2D path, final TreeLayout treeLayout,
			final Point parentExitPoint, final Point childEnterPoint, final boolean dashed){
		final Point[] points;
		if(treeLayout == TreeLayout.VERTICAL){
			final int midY = (childEnterPoint.y + parentExitPoint.y + PartnersPanel.GROUP_EXITING_HEIGHT) >> 1;

			points = new Point[]{parentExitPoint, new Point(parentExitPoint.x, midY), new Point(childEnterPoint.x, midY),
				childEnterPoint};
		}
		else{
			final int midX = (childEnterPoint.x + parentExitPoint.x - PartnersPanel.GROUP_EXITING_HEIGHT) >> 1;

			points = new Point[]{parentExitPoint, new Point(midX, parentExitPoint.y), new Point(midX, childEnterPoint.y),
				childEnterPoint};
		}

		path.moveTo(points[0].x, points[0].y);
		if(dashed)
			//a connection that is not by birth is dashed, as in the other views of the tree
			appendDashed(path, points);
		else
			for(int i = 1; i < points.length; i ++)
				path.lineTo(points[i].x, points[i].y);
	}

	/** Adds the polyline as a sequence of short segments (the same pattern as {@code ConnectionStrokes.CONNECTION_STROKE_ADOPTED}). */
	private static void appendDashed(final Path2D path, final Point[] points){
		boolean on = true;
		double remaining = DASH_LENGTH;
		for(int i = 1; i < points.length; i ++){
			final double dx = points[i].x - points[i - 1].x;
			final double dy = points[i].y - points[i - 1].y;
			final double length = Math.hypot(dx, dy);
			if(length == 0.)
				continue;

			double position = 0.;
			while(position < length){
				final double step = Math.min(remaining, length - position);
				position += step;
				remaining -= step;

				final double x = points[i - 1].x + dx * position / length;
				final double y = points[i - 1].y + dy * position / length;
				if(on)
					path.lineTo(x, y);
				else
					path.moveTo(x, y);

				if(remaining <= 0.){
					on = !on;
					remaining = DASH_LENGTH;
				}
			}
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
