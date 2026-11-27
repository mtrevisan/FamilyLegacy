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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class LayoutRenderer{

	/** The length of a dash and of a gap (pixels). */
	private static final double DASH_LENGTH = 2.;
	/** The distance of each of two side by side connections from the common axis (pixels). */
	private static final int PARALLEL_OFFSET = 2;

	/** The distance between two tracks of overlapping runs (pixels). */
	private static final int TRACK_SPACING = 8;
	/** Runs closer than this (pixels) are considered overlapping. */
	private static final int TRACK_MARGIN = 3;
	/** The free space kept between the outermost track and the boxes (pixels). */
	private static final int TRACK_EDGE_MARGIN = 3;

	private LayoutRenderer(){}


	/**
	 * Builds a cached {@link Path2D} containing all connection lines for the tree.
	 */
	public static Path2D buildTreePath(final TreeLayout treeLayout, final TreeNode rootNode,
			final Map<TreeNode, PartnersPanel> nodeToPanelMap, final SiblingsPanel childrenPanel, final JPanel canvas){
		final Path2D path = new Path2D.Double();
		if(rootNode == null || nodeToPanelMap.isEmpty())
			return path;

		final List<Connection> connections = new ArrayList<>();
		buildTreeConnections(connections, treeLayout, rootNode, nodeToPanelMap, canvas);
		assignTracks(connections, treeLayout);
		for(final Connection connection : connections)
			connectToParents(path, treeLayout, connection.individual, connection.exit, connection.enter, connection.shift);
		if(childrenPanel != null && !childrenPanel.getSiblingBoxes().isEmpty()){
			final PartnersPanel homePanel = nodeToPanelMap.get(rootNode);
			if(homePanel != null)
				buildChildrenConnections(path, treeLayout, homePanel, childrenPanel, canvas);
		}

		return path;
	}

	private static void buildTreeConnections(final List<Connection> connections, final TreeLayout treeLayout, final TreeNode node,
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
				connections.add(new Connection(father,
					SwingUtilities.convertPoint(fatherPanel, fatherPanel.getPaintingExitPoint(), container),
					SwingUtilities.convertPoint(nodePanel, nodePanel.getPaintingFatherEnterPoint(), container)));
			buildTreeConnections(connections, treeLayout, father, nodeToPanelMap, container);
		}

		if(mother != null){
			if(isDrawn(motherPanel) && isDrawn(nodePanel) && nodePanel.getMotherPanel().isVisible())
				connections.add(new Connection(mother,
					SwingUtilities.convertPoint(motherPanel, motherPanel.getPaintingExitPoint(), container),
					SwingUtilities.convertPoint(nodePanel, nodePanel.getPaintingMotherEnterPoint(), container)));
			buildTreeConnections(connections, treeLayout, mother, nodeToPanelMap, container);
		}
	}

	/** A line from the panel of a parent to the panel of the child; {@code shift} moves its middle run away from the default. */
	private static final class Connection{
		private final TreeNode individual;
		private final Point exit;
		private final Point enter;
		private int shift;

		private Connection(final TreeNode individual, final Point exit, final Point enter){
			this.individual = individual;
			this.exit = exit;
			this.enter = enter;
		}
	}

	/**
	 * Gives each group of lines leaving the same box a track of its own, so that the runs of two different groups never lie on
	 * the same line: otherwise it is impossible to tell whose child is who (typical with an ancestor couple seen twice).
	 * <p>
	 * The lines are grouped by the layers they join and by the exit point (all the lines of a couple are one bus and share a
	 * track); two buses whose extents overlap get different tracks (interval coloring), placed around the default position.
	 */
	private static void assignTracks(final List<Connection> connections, final TreeLayout treeLayout){
		final boolean vertical = (treeLayout == TreeLayout.VERTICAL);
		final Map<String, Map<String, List<Connection>>> byGap = new LinkedHashMap<>();
		for(final Connection connection : connections){
			final String gapKey = (vertical? connection.exit.y + "/" + connection.enter.y: connection.exit.x + "/" + connection.enter.x);
			final String busKey = connection.exit.x + "/" + connection.exit.y;
			byGap.computeIfAbsent(gapKey, k -> new LinkedHashMap<>())
				.computeIfAbsent(busKey, k -> new ArrayList<>())
				.add(connection);
		}

		for(final Map<String, List<Connection>> buses : byGap.values()){
			if(buses.size() < 2)
				continue;

			final List<List<Connection>> busList = new ArrayList<>(buses.values());
			busList.sort(Comparator.comparingInt(bus -> extent(bus, vertical)[0]));

			final List<Integer> trackEnds = new ArrayList<>();
			final int[] busTrack = new int[busList.size()];
			for(int b = 0; b < busList.size(); b ++){
				final int[] extent = extent(busList.get(b), vertical);
				int track = 0;
				while(track < trackEnds.size() && trackEnds.get(track) + TRACK_MARGIN >= extent[0])
					track ++;
				if(track == trackEnds.size())
					trackEnds.add(extent[1]);
				else
					trackEnds.set(track, extent[1]);
				busTrack[b] = track;
			}

			final Connection sample = busList.get(0).get(0);
			final int room = Math.abs(vertical? sample.enter.y - sample.exit.y: sample.enter.x - sample.exit.x);
			final int half = Math.max(0, (room - PartnersPanel.GROUP_EXITING_HEIGHT) / 2 - TRACK_EDGE_MARGIN);
			final int spacing = (trackEnds.size() > 1? Math.max(1, Math.min(TRACK_SPACING, half * 2 / (trackEnds.size() - 1))): 0);
			for(int b = 0; b < busList.size(); b ++){
				//track 0 in the middle, then alternating below and above: 0, +1, -1, +2, -2...
				final int slot = ((busTrack[b] & 1) == 1? (busTrack[b] + 1) / 2: -busTrack[b] / 2);
				for(final Connection connection : busList.get(b))
					connection.shift = slot * spacing;
			}
		}
	}

	/** Returns {min, max} of the coordinate along which the run of the bus extends. */
	private static int[] extent(final List<Connection> bus, final boolean vertical){
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for(final Connection connection : bus){
			final int a = (vertical? connection.exit.x: connection.exit.y);
			final int b = (vertical? connection.enter.x: connection.enter.y);
			min = Math.min(min, Math.min(a, b));
			max = Math.max(max, Math.max(a, b));
		}
		return new int[]{min, max};
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
			final Point exit, final Point enter, final int shift){
		final boolean hasFather = (individual.getFather() != null);
		final boolean hasMother = (individual.getMother() != null);
		final boolean fatherNonBiological = (hasFather && individual.isFatherNonBiological());
		final boolean motherNonBiological = (hasMother && individual.isMotherNonBiological());
		if(!hasFather || !hasMother || fatherNonBiological == motherNonBiological){
			connectParentToChild(path, treeLayout, exit, enter, fatherNonBiological || motherNonBiological, shift);
			return;
		}

		final int dx = (treeLayout == TreeLayout.VERTICAL? PARALLEL_OFFSET: 0);
		final int dy = (treeLayout == TreeLayout.VERTICAL? 0: PARALLEL_OFFSET);
		connectParentToChild(path, treeLayout, new Point(exit.x - dx, exit.y - dy), new Point(enter.x - dx, enter.y - dy),
			fatherNonBiological, shift);
		connectParentToChild(path, treeLayout, new Point(exit.x + dx, exit.y + dy), new Point(enter.x + dx, enter.y + dy),
			motherNonBiological, shift);
	}

	private static void connectParentToChild(final Path2D path, final TreeLayout treeLayout,
			final Point parentExitPoint, final Point childEnterPoint, final boolean dashed, final int shift){
		final Point[] points;
		if(treeLayout == TreeLayout.VERTICAL){
			final int midY = (childEnterPoint.y + parentExitPoint.y + PartnersPanel.GROUP_EXITING_HEIGHT) / 2 + shift;

			points = new Point[]{parentExitPoint, new Point(parentExitPoint.x, midY), new Point(childEnterPoint.x, midY),
				childEnterPoint};
		}
		else{
			final int midX = (childEnterPoint.x + parentExitPoint.x - PartnersPanel.GROUP_EXITING_HEIGHT) / 2 + shift;

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
				//the lines of the boxes and the one of the parents are rounded each on its own: when the line of the parents
				//would run a pixel off the line of the child in front of it, it takes the line of that child, so that they join
				int y = homeExit.y;
				for(final Point enterPoint : enterPoints){
					final int childY = SwingUtilities.convertPoint(childrenPanel, enterPoint, container).y;
					if(Math.abs(childY - homeExit.y) == 1)
						y = childY;
				}
				path.moveTo(homeExit.x, y);
				path.lineTo(targetPoint.x, y);
			}
		}
	}

}
