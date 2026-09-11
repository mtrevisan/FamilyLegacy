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

import java.util.ArrayList;
import java.util.List;


/**
 * A connected sequence of {@link SocialEdgeRef} elements.
 * <p>
 * A path represents a chain of social relationships connecting two entities
 * through zero or more intermediate nodes. It is the return type of the
 * path finder and the model behind the highlighted route drawn by the
 * Social Network renderer.
 * <p>
 * The sequence is guaranteed to be a valid walk: consecutive edges share at
 * least one endpoint, as validated by the compact constructor. The direction
 * of each edge is preserved; the {@link #nodes()} method reconstructs the
 * ordered sequence of entities by traversing the edges from the first one.
 */
public record SocialPath(List<SocialEdgeRef> edges){

	/**
	 * Compact constructor with validation.
	 * <p>
	 * Validates that the edge list is non‑empty and that consecutive edges
	 * form a connected walk. Throws {@link IllegalArgumentException} when a
	 * discontinuity is detected, so that malformed paths produced by
	 * upstream code fail fast rather than silently producing incorrect
	 * renderings.
	 */
	public SocialPath{
		if(edges == null || edges.isEmpty())
			throw new IllegalArgumentException("Path must contain at least one edge");
		edges = List.copyOf(edges);

		for(int i = 1; i < edges.size(); i++){
			final SocialEdgeRef previous = edges.get(i - 1);
			final SocialEdgeRef current = edges.get(i);
			final boolean connected = (previous.involves(current.source())
				|| previous.involves(current.target()));
			if(!connected)
				throw new IllegalArgumentException("Path is not connected at index " + i);
		}
	}


	/**
	 * Creates a path from a list of edges.
	 *
	 * @param edges the edges (must not be {@code null} and must be non‑empty)
	 * @return a new path
	 */
	public static SocialPath of(final List<SocialEdgeRef> edges){
		return new SocialPath(edges);
	}


	/**
	 * Returns the number of edges in this path, i.e. the number of steps
	 * between the two endpoints.
	 *
	 * @return the path length, always positive
	 */
	public int length(){
		return edges.size();
	}

	/**
	 * Returns the number of intermediate nodes between the two endpoints.
	 * Zero for direct neighbours, one for a friend‑of‑a‑friend, and so on.
	 *
	 * @return the number of intermediate nodes, never negative
	 */
	public int intermediateNodeCount(){
		return Math.max(0, edges.size() - 1);
	}

	/**
	 * Returns the entity at the start of the path.
	 *
	 * @return the first endpoint
	 */
	public TemporalEntityRef start(){
		return edges.getFirst().source();
	}

	/**
	 * Returns the entity at the end of the path.
	 *
	 * @return the last endpoint
	 */
	public TemporalEntityRef end(){
		return edges.getLast().target();
	}

	/**
	 * Returns whether the given entity is one of the two endpoints of this
	 * path.
	 *
	 * @param entity the entity to test (must not be {@code null})
	 * @return {@code true} if the entity is the start or the end of the path
	 */
	public boolean connects(final TemporalEntityRef entity){
		return (start().equals(entity) || end().equals(entity));
	}

	/**
	 * Reconstructs the ordered sequence of entities along the path,
	 * traversing the edges from the first one. For a chain A‑B‑C the result
	 * is {@code [A, B, C]}.
	 * <p>
	 * The method assumes that the path is a walk in which each edge is
	 * traversed forward (from its source to its target) or backward. The
	 * reconstruction picks, for the first edge, the endpoint that is not
	 * shared with the second edge, so that the sequence is well defined for
	 * both directed and undirected edges.
	 *
	 * @return the ordered list of entities, of size {@code length() + 1}
	 */
	public List<TemporalEntityRef> nodes(){
		final List<TemporalEntityRef> result = new ArrayList<>(edges.size() + 1);

		final TemporalEntityRef first;
		if(edges.size() == 1)
			first = edges.getFirst().source();
		else{
			final SocialEdgeRef e0 = edges.getFirst();
			final SocialEdgeRef e1 = edges.get(1);
			// The shared endpoint between e0 and e1 is the one that must NOT
			// be the start of the path: pick the other endpoint of e0.
			final TemporalEntityRef shared = sharedEndpoint(e0, e1);
			first = (shared != null && e0.involves(shared)? e0.other(shared): e0.source());
		}
		result.add(first);

		TemporalEntityRef current = first;
		for(final SocialEdgeRef edge : edges){
			final TemporalEntityRef next = edge.other(current);
			if(next == null)
				// Defensive: should never happen because the compact
				// constructor validated connectivity.
				break;
			result.add(next);
			current = next;
		}
		return List.copyOf(result);
	}

	/**
	 * Returns whether the path is a direct edge between two entities.
	 *
	 * @return {@code true} if the path consists of exactly one edge
	 */
	public boolean isDirect(){
		return (edges.size() == 1);
	}


	private static TemporalEntityRef sharedEndpoint(final SocialEdgeRef a, final SocialEdgeRef b){
		if(a.source().equals(b.source()) || a.source().equals(b.target()))
			return a.source();
		if(a.target().equals(b.source()) || a.target().equals(b.target()))
			return a.target();
		return null;
	}

	@Override
	public String toString(){
		final List<TemporalEntityRef> nodes = nodes();
		final StringBuilder sb = new StringBuilder();
		for(int i = 0; i < nodes.size(); i++){
			if(i > 0)
				sb.append(" -> ");
			sb.append(nodes.get(i)
				.id());
		}
		return "SocialPath[" + length() + "]: " + sb;
	}

}
