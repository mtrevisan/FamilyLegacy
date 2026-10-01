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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections;

import java.awt.Rectangle;
import java.util.Map;


/**
 * Geometric helper for directional keyboard navigation on graph-style
 * views (ancestor tree, Sugiyama graph, ego network).
 * <p>
 * The three views are not grids, so "the node above" cannot be resolved
 * from an index. Instead, the position of every visible node is read
 * from its screen bounds, and the closest node in the requested
 * direction is selected by a weighted score: the distance along the
 * direction is the primary term, and the perpendicular distance is the
 * secondary term, weighted twice as much. The weighting keeps the
 * navigation on the same column (or row) whenever possible, so pressing
 * {@code Up} from a child reaches its parent rather than a far cousin.
 * <p>
 * All the coordinates are read from screen bounds, so the returned node
 * is the one the user actually sees closest in the pressed direction.
 */
public final class SpatialNavigation{

	/** The four cardinal directions. */
	public enum Direction{
		UP,
		DOWN,
		LEFT,
		RIGHT
	}


	private SpatialNavigation(){
	}


	/**
	 * Returns the id of the node that lies closest to the given node in
	 * the requested direction, or {@code null} when no node exists in
	 * that direction.
	 * <p>
	 * Nodes are compared by the center of their bounds. A node is a
	 * candidate only when its center is strictly beyond the current
	 * center along the requested axis; the other axis contributes only
	 * to the score. The primary term is the distance along the axis,
	 * the secondary term is the perpendicular distance weighted by
	 * {@link #PERPENDICULAR_WEIGHT}.
	 *
	 * @param boundsById the visible nodes, keyed by id
	 * @param currentId  the id of the current node
	 * @param direction  the direction to move
	 * @return the id of the next node, or {@code null}
	 */
	public static String next(final Map<String, Rectangle> boundsById, final String currentId,
			final Direction direction){
		if(boundsById == null || boundsById.isEmpty() || currentId == null || direction == null)
			return null;

		final Rectangle current = boundsById.get(currentId);
		if(current == null)
			return null;

		final double cx = current.getCenterX();
		final double cy = current.getCenterY();

		String best = null;
		double bestScore = Double.POSITIVE_INFINITY;

		for(final Map.Entry<String, Rectangle> entry : boundsById.entrySet()){
			final String id = entry.getKey();
			if(id.equals(currentId))
				continue;

			final Rectangle r = entry.getValue();
			final double rx = r.getCenterX();
			final double ry = r.getCenterY();
			final double dx = rx - cx;
			final double dy = ry - cy;

			final double primary;
			final double secondary;
			switch(direction){
				case UP -> {
					if(dy >= -1.)
						continue;
					primary = -dy;
					secondary = Math.abs(dx);
				}
				case DOWN -> {
					if(dy <= 1.)
						continue;
					primary = dy;
					secondary = Math.abs(dx);
				}
				case LEFT -> {
					if(dx >= -1.)
						continue;
					primary = -dx;
					secondary = Math.abs(dy);
				}
				case RIGHT -> {
					if(dx <= 1.)
						continue;
					primary = dx;
					secondary = Math.abs(dy);
				}
				default -> {
					continue;
				}
			}

			final double score = primary + secondary * PERPENDICULAR_WEIGHT;
			if(score < bestScore){
				bestScore = score;
				best = id;
			}
		}

		return best;
	}


	/** Weight applied to the perpendicular distance in the scoring function. */
	private static final double PERPENDICULAR_WEIGHT = 2.;

}
