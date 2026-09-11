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

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Computes the positions of the nodes of a {@link SocialGraph}.
 * <p>
 * Two deterministic layouts are supported, selected through
 * {@link SocialLayoutMode}:
 * <ul>
 *   <li><b>{@link SocialLayoutMode#CONCENTRIC}</b> — nodes are placed on
 *       concentric rings around the focus. The distance between two nodes
 *       in the drawing is proportional to their graph distance from the
 *       focus, so the degree of separation is immediately readable.</li>
 *   <li><b>{@link SocialLayoutMode#RADIAL_TREE}</b> — nodes are placed as
 *       a radial tree rooted at the focus. Each node's angular sector is
 *       proportional to the size of its BFS subtree, so deep branches
 *       receive more angular space than shallow ones. Cross-edges (cycles,
 *       multiple parents) are not part of the tree and do not influence
 *       the layout; the renderer draws them on top.</li>
 * </ul>
 * The layout is <b>deterministic</b>: for the same graph and the same
 * mode, the produced positions are always identical. Ties between nodes
 * with the same BFS parent are broken by entity id, so screenshots and
 * tests are reproducible.
 * <p>
 * All coordinates are in a coordinate system where the focus is placed at
 * the origin during computation and then translated so that the whole
 * graph fits into a positive rectangle. The translation is exposed through
 * {@link #contentBounds()}, which the panel uses to size its canvas.
 * Instances are immutable.
 */
public final class SocialLayout{

	/** Width, in pixels, of a regular node's bounding box. */
	public static final int NODE_WIDTH = 180;

	/** Height, in pixels, of a regular node's bounding box. */
	public static final int NODE_HEIGHT = 60;

	/** Width, in pixels, of the focus node's bounding box. */
	public static final int CENTER_NODE_WIDTH = 220;

	/** Height, in pixels, of the focus node's bounding box. */
	public static final int CENTER_NODE_HEIGHT = 80;

	/** Minimum gap, in pixels, between two consecutive rings. */
	public static final int MIN_RING_STEP = 140;

	/** Minimum gap, in pixels, between two nodes on the same ring. */
	public static final int MIN_NODE_GAP = 40;

	/** Padding, in pixels, added around the whole graph. */
	public static final int CONTENT_PADDING = 80;

	/** Minimum radius, in pixels, of the first ring. */
	public static final int MIN_FIRST_RING_RADIUS = 200;

	private static final double TWO_PI = 2. * Math.PI;


	private final Map<SocialNodeRef, Rectangle> nodeBounds;
	private final Rectangle contentBounds;
	private final SocialLayoutMode mode;


	private SocialLayout(final Map<SocialNodeRef, Rectangle> nodeBounds, final Rectangle contentBounds,
		final SocialLayoutMode mode){
		this.nodeBounds = Collections.unmodifiableMap(nodeBounds);
		this.contentBounds = contentBounds;
		this.mode = mode;
	}


	/* ======================================================================
	 *                          Factories
	 * ====================================================================== */

	/**
	 * Computes the layout of the given graph with the given mode.
	 *
	 * @param graph the graph to lay out (must not be {@code null})
	 * @param mode  the layout mode (must not be {@code null})
	 * @return the computed layout
	 */
	public static SocialLayout compute(final SocialGraph graph, final SocialLayoutMode mode){
		if(graph == null)
			throw new IllegalArgumentException("Graph must not be null");
		if(mode == null)
			throw new IllegalArgumentException("Mode must not be null");

		if(graph.isEmpty())
			return new SocialLayout(Map.of(), new Rectangle(0, 0, 0, 0), mode);

		return switch(mode){
			case CONCENTRIC -> computeConcentric(graph);
			case RADIAL_TREE -> computeRadialTree(graph);
		};
	}


	/* ======================================================================
	 *                          Accessors
	 * ====================================================================== */

	/**
	 * Returns the immutable bounding box of every node, keyed by node.
	 *
	 * @return the node bounds, never {@code null}
	 */
	public Map<SocialNodeRef, Rectangle> nodeBounds(){
		return nodeBounds;
	}

	/**
	 * Returns the bounding box that encloses every node plus the content
	 * padding. The panel sizes its canvas to this rectangle.
	 *
	 * @return the content bounds, never {@code null}
	 */
	public Rectangle contentBounds(){
		return contentBounds;
	}

	/**
	 * Returns the layout mode used to compute this layout.
	 *
	 * @return the mode
	 */
	public SocialLayoutMode mode(){
		return mode;
	}

	/**
	 * Returns the bounding box of the given node, or {@code null} if the
	 * node is not part of the layout.
	 *
	 * @param node the node
	 * @return the bounds, or {@code null}
	 */
	public Rectangle boundsOf(final SocialNodeRef node){
		return (node != null? nodeBounds.get(node): null);
	}

	/**
	 * Returns the center of the given node, or {@code null} if the node is
	 * not part of the layout.
	 *
	 * @param node the node
	 * @return the center point, or {@code null}
	 */
	public Point2D centerOf(final SocialNodeRef node){
		final Rectangle bounds = boundsOf(node);
		if(bounds == null)
			return null;
		return new Point2D.Double(
			bounds.x + bounds.width / 2.,
			bounds.y + bounds.height / 2.);
	}

	/**
	 * Returns the id of the node whose bounding box contains the given
	 * point, or {@code null} if no node is hit.
	 *
	 * @param x the X coordinate
	 * @param y the Y coordinate
	 * @return the hit node, or {@code null}
	 */
	public SocialNodeRef hitTest(final int x, final int y){
		for(final Map.Entry<SocialNodeRef, Rectangle> entry : nodeBounds.entrySet())
			if(entry.getValue()
				.contains(x, y))
				return entry.getKey();
		return null;
	}

	/**
	 * Returns whether the layout contains no nodes.
	 *
	 * @return {@code true} if {@link #nodeBounds()} is empty
	 */
	public boolean isEmpty(){
		return nodeBounds.isEmpty();
	}


	/* ======================================================================
	 *                          Concentric layout
	 * ====================================================================== */

	private static SocialLayout computeConcentric(final SocialGraph graph){
		final SocialNodeRef center = graph.center();
		final Map<SocialNodeRef, Point2D> positions = new LinkedHashMap<>();

		// Place the focus at the origin.
		positions.put(center, new Point2D.Double(0., 0.));

		// Reconstruct the BFS parent of each node so that children of the
		// same parent remain adjacent on their ring.
		final Map<SocialNodeRef, SocialNodeRef> parent = bfsParents(graph);
		final Map<SocialNodeRef, List<SocialNodeRef>> childrenByParent = groupByParent(parent, graph);

		// Group nodes by ring.
		final Map<Integer, List<SocialNodeRef>> ringToNodes = new LinkedHashMap<>();
		for(final SocialNodeRef node : graph.nodes())
			if(!node.isCenter())
				ringToNodes.computeIfAbsent(node.degree(), k -> new ArrayList<>())
					.add(node);

		// Sort each ring's nodes by the position of their parent in the
		// previous ring, then by id.
		final Map<SocialNodeRef, Integer> parentIndexInRing = new HashMap<>();
		parentIndexInRing.put(center, 0);

		double previousRadius = 0.;
		int ringIndex = 0;
		for(final Map.Entry<Integer, List<SocialNodeRef>> entry : ringToNodes.entrySet()){
			final List<SocialNodeRef> ringNodes = entry.getValue();
			ringIndex++;

			// Sort children so that those with the same parent stay together.
			ringNodes.sort(Comparator
				.comparingInt((SocialNodeRef n) -> {
					final SocialNodeRef p = parent.get(n);
					final Integer idx = parentIndexInRing.get(p);
					return (idx != null? idx: Integer.MAX_VALUE);
				})
				.thenComparing(n -> n.entity()
					.id()));

			// Compute the minimum radius that avoids overlap and respects
			// the incremental step.
			final double circumference = (double)ringNodes.size() * (NODE_WIDTH + MIN_NODE_GAP);
			final double requiredRadius = circumference / TWO_PI;
			final double incrementalRadius = previousRadius + MIN_RING_STEP;
			final double radius = Math.max(
				ringIndex == 1? MIN_FIRST_RING_RADIUS: incrementalRadius,
				Math.max(requiredRadius, incrementalRadius));

			// Distribute the nodes uniformly.
			final double angleStep = TWO_PI / ringNodes.size();
			// Start from the top (12 o'clock) and go clockwise.
			final double startAngle = -Math.PI / 2.;
			for(int i = 0; i < ringNodes.size(); i++){
				final SocialNodeRef node = ringNodes.get(i);
				final double angle = startAngle + i * angleStep;
				final double x = radius * Math.cos(angle);
				final double y = radius * Math.sin(angle);
				positions.put(node, new Point2D.Double(x, y));
				parentIndexInRing.put(node, i);
			}

			previousRadius = radius;
		}

		return buildLayout(positions, center);
	}


	/* ======================================================================
	 *                          Radial tree layout
	 * ====================================================================== */

	private static SocialLayout computeRadialTree(final SocialGraph graph){
		final SocialNodeRef center = graph.center();
		final Map<SocialNodeRef, Point2D> positions = new LinkedHashMap<>();
		positions.put(center, new Point2D.Double(0., 0.));

		final Map<SocialNodeRef, SocialNodeRef> parent = bfsParents(graph);
		final Map<SocialNodeRef, List<SocialNodeRef>> childrenByParent = groupByParent(parent, graph);

		// Compute subtree weights (number of nodes in the subtree rooted at
		// each node, including the node itself).
		final Map<SocialNodeRef, Integer> subtreeWeight = computeSubtreeWeights(center, childrenByParent);

		// Recursively assign angular sectors.
		final double rootWeight = subtreeWeight.getOrDefault(center, 1);
		assignRadial(center, 0., -Math.PI / 2., TWO_PI, (int)rootWeight, childrenByParent, subtreeWeight, positions);

		return buildLayout(positions, center);
	}

	private static void assignRadial(final SocialNodeRef node, final double parentRadius, final double startAngle,
		final double sector, final int weight, final Map<SocialNodeRef, List<SocialNodeRef>> childrenByParent,
		final Map<SocialNodeRef, Integer> subtreeWeight, final Map<SocialNodeRef, Point2D> positions){
		final List<SocialNodeRef> children = childrenByParent.getOrDefault(node, List.of());
		if(children.isEmpty())
			return;

		final double childRadius = parentRadius + MIN_RING_STEP;
		double angle = startAngle;

		for(final SocialNodeRef child : children){
			final int childWeight = subtreeWeight.getOrDefault(child, 1);
			final double childSector = sector * ((double)childWeight / (double)weight);
			final double midAngle = angle + childSector / 2.;
			final double x = childRadius * Math.cos(midAngle);
			final double y = childRadius * Math.sin(midAngle);
			positions.put(child, new Point2D.Double(x, y));

			assignRadial(child, childRadius, angle, childSector, childWeight, childrenByParent, subtreeWeight,
				positions);

			angle += childSector;
		}
	}


	/* ======================================================================
	 *                          Shared helpers
	 * ====================================================================== */

	/**
	 * Reconstructs the BFS parent of every node starting from the focus,
	 * traversing the graph respecting edge direction.
	 */
	private static Map<SocialNodeRef, SocialNodeRef> bfsParents(final SocialGraph graph){
		final Map<SocialNodeRef, SocialNodeRef> parent = new HashMap<>();
		final Map<String, SocialNodeRef> visitedById = new HashMap<>();
		final Deque<SocialNodeRef> queue = new ArrayDeque<>();

		final SocialNodeRef center = graph.center();
		visitedById.put(center.id(), center);
		queue.add(center);

		while(!queue.isEmpty()){
			final SocialNodeRef current = queue.poll();
			final TemporalEntityRef currentEntity = current.entity();
			for(final SocialEdgeRef edge : graph.edgesOf(current.id())){
				if(!edge.canTraverseFrom(currentEntity))
					continue;
				final TemporalEntityRef otherEntity = edge.other(currentEntity);
				if(otherEntity == null)
					continue;
				final String otherId = otherEntity.id();
				if(visitedById.containsKey(otherId))
					continue;
				final SocialNodeRef other = graph.findNode(otherId);
				if(other == null)
					continue;
				visitedById.put(otherId, other);
				parent.put(other, current);
				queue.add(other);
			}
		}
		return parent;
	}

	private static Map<SocialNodeRef, List<SocialNodeRef>> groupByParent(final Map<SocialNodeRef, SocialNodeRef> parent,
		final SocialGraph graph){
		final Map<SocialNodeRef, List<SocialNodeRef>> result = new HashMap<>();
		for(final SocialNodeRef node : graph.nodes()){
			final SocialNodeRef p = parent.get(node);
			if(p == null)
				continue;
			result.computeIfAbsent(p, k -> new ArrayList<>())
				.add(node);
		}
		// Sort each child list by id for determinism.
		for(final List<SocialNodeRef> children : result.values())
			children.sort(Comparator.comparing(n -> n.entity()
				.id()));
		return result;
	}

	private static Map<SocialNodeRef, Integer> computeSubtreeWeights(final SocialNodeRef root,
		final Map<SocialNodeRef, List<SocialNodeRef>> childrenByParent){
		final Map<SocialNodeRef, Integer> weights = new HashMap<>();
		computeWeight(root, childrenByParent, weights);
		return weights;
	}

	private static int computeWeight(final SocialNodeRef node,
		final Map<SocialNodeRef, List<SocialNodeRef>> childrenByParent, final Map<SocialNodeRef, Integer> weights){
		final List<SocialNodeRef> children = childrenByParent.getOrDefault(node, List.of());
		int weight = 1;
		for(final SocialNodeRef child : children)
			weight += computeWeight(child, childrenByParent, weights);
		weights.put(node, weight);
		return weight;
	}


	/* ======================================================================
	 *                          Bounding box computation
	 * ====================================================================== */

	/**
	 * Translates the computed positions (which are centered around the
	 * origin) into a positive coordinate system, computes the bounding box
	 * of every node, and assembles the final layout.
	 */
	private static SocialLayout buildLayout(final Map<SocialNodeRef, Point2D> positions, final SocialNodeRef center){
		// Compute raw bounds centered at the origin.
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for(final Map.Entry<SocialNodeRef, Point2D> entry : positions.entrySet()){
			final boolean isCenter = entry.getKey()
				.equals(center);
			final double halfWidth = (isCenter? CENTER_NODE_WIDTH: NODE_WIDTH) / 2.;
			final double halfHeight = (isCenter? CENTER_NODE_HEIGHT: NODE_HEIGHT) / 2.;
			final Point2D p = entry.getValue();
			minX = Math.min(minX, p.getX() - halfWidth);
			minY = Math.min(minY, p.getY() - halfHeight);
			maxX = Math.max(maxX, p.getX() + halfWidth);
			maxY = Math.max(maxY, p.getY() + halfHeight);
		}

		// Translate so that the top-left of the raw bounding box is at
		// (CONTENT_PADDING, CONTENT_PADDING).
		final double dx = CONTENT_PADDING - minX;
		final double dy = CONTENT_PADDING - minY;

		final Map<SocialNodeRef, Rectangle> bounds = new LinkedHashMap<>();
		for(final Map.Entry<SocialNodeRef, Point2D> entry : positions.entrySet()){
			final boolean isCenter = entry.getKey()
				.equals(center);
			final int width = (isCenter? CENTER_NODE_WIDTH: NODE_WIDTH);
			final int height = (isCenter? CENTER_NODE_HEIGHT: NODE_HEIGHT);
			final Point2D p = entry.getValue();
			final int x = (int)Math.round(p.getX() + dx - width / 2.);
			final int y = (int)Math.round(p.getY() + dy - height / 2.);
			bounds.put(entry.getKey(), new Rectangle(x, y, width, height));
		}

		final int contentWidth = (int)Math.ceil(maxX - minX) + 2 * CONTENT_PADDING;
		final int contentHeight = (int)Math.ceil(maxY - minY) + 2 * CONTENT_PADDING;
		final Rectangle contentBounds = new Rectangle(0, 0, Math.max(1, contentWidth),
			Math.max(1, contentHeight));

		return new SocialLayout(bounds, contentBounds, SocialLayoutMode.CONCENTRIC);
	}

}
