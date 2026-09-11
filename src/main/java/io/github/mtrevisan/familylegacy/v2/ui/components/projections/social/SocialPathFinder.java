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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Finds the shortest social path between two entities of a {@link SocialGraph}.
 * <p>
 * The path finder performs a breadth‑first search over the graph, starting
 * from the source entity and stopping as soon as the target entity is
 * reached. Traversal respects the {@link SocialEdgeDirection} of each edge:
 * undirected edges can be crossed in both directions, directed edges only
 * from subject to target.
 * <p>
 * The class is stateless and thread‑safe: it exposes only static methods
 * that operate on the immutable {@code SocialGraph} value.
 */
public final class SocialPathFinder{

	/**
	 * Maximum number of edges explored before the search is abandoned.
	 * Prevents pathological cases where the graph is very large and the
	 * target is unreachable. The value is generous enough for real social
	 * networks produced by the projection (typically a few hundred nodes).
	 */
	private static final int MAX_EXPLORED_EDGES = 100_000;


	private SocialPathFinder(){}


	/**
	 * Finds the shortest path between two entities in the given graph.
	 *
	 * @param graph  the graph to search (must not be {@code null})
	 * @param fromId the source entity id (must not be {@code null})
	 * @param toId   the target entity id (must not be {@code null})
	 * @return the shortest path, or {@code null} if either endpoint is not
	 * part of the graph or no path exists
	 */
	public static SocialPath findPath(final SocialGraph graph, final String fromId, final String toId){
		if(graph == null || fromId == null || toId == null)
			return null;
		if(fromId.equals(toId))
			return null;
		if(!graph.hasNode(fromId) || !graph.hasNode(toId))
			return null;

		final Map<String, SocialEdgeRef> previousEdge = new HashMap<>();
		final Map<String, String> previousNode = new HashMap<>();
		final Set<String> visited = new HashSet<>();
		final Deque<String> queue = new ArrayDeque<>();
		visited.add(fromId);
		queue.add(fromId);
		int exploredEdges = 0;
		while(!queue.isEmpty() && exploredEdges < MAX_EXPLORED_EDGES){
			final String currentId = queue.poll();
			if(currentId.equals(toId))
				break;

			final TemporalEntityRef currentEntity = graph.findNode(currentId)
				.entity();
			for(final SocialEdgeRef edge : graph.edgesOf(currentId)){
				exploredEdges ++;
				if(exploredEdges >= MAX_EXPLORED_EDGES)
					break;

				if(!edge.canTraverseFrom(currentEntity))
					continue;

				final TemporalEntityRef next = edge.other(currentEntity);
				if(next == null)
					continue;
				final String nextId = next.id();
				if(visited.contains(nextId))
					continue;

				visited.add(nextId);
				previousNode.put(nextId, currentId);
				previousEdge.put(nextId, edge);
				queue.add(nextId);
			}
		}

		if(!visited.contains(toId))
			return null;

		return reconstructPath(previousEdge, previousNode, fromId, toId);
	}

	/**
	 * Returns the shortest path length between two entities, or {@code -1}
	 * if no path exists. Equivalent to
	 * {@code findPath(graph, fromId, toId).length()} but avoids
	 * reconstructing the path.
	 *
	 * @param graph  the graph to search
	 * @param fromId the source entity id
	 * @param toId   the target entity id
	 * @return the path length, or {@code -1}
	 */
	public static int findPathLength(final SocialGraph graph, final String fromId, final String toId){
		final SocialPath path = findPath(graph, fromId, toId);
		return (path != null? path.length(): -1);
	}

	/**
	 * Returns whether two entities are connected in the given graph.
	 *
	 * @param graph  the graph to search
	 * @param fromId the source entity id
	 * @param toId   the target entity id
	 * @return {@code true} if a path exists
	 */
	public static boolean areConnected(final SocialGraph graph, final String fromId, final String toId){
		return (findPath(graph, fromId, toId) != null);
	}

	/**
	 * Returns the set of all entities reachable from the given entity
	 * within the given number of hops, including the start entity.
	 *
	 * @param graph   the graph to search
	 * @param fromId  the source entity id
	 * @param maxHops the maximum number of edges to cross
	 * @return the reachable entity ids, never {@code null}
	 */
	public static Set<String> reachableWithin(final SocialGraph graph, final String fromId, final int maxHops){
		if(graph == null || !graph.hasNode(fromId) || maxHops < 0)
			return Set.of();

		final Set<String> visited = new HashSet<>();
		final Deque<String> current = new ArrayDeque<>();
		final Deque<String> next = new ArrayDeque<>();
		visited.add(fromId);
		current.add(fromId);
		for(int hop = 0; hop < maxHops && !current.isEmpty(); hop ++){
			while(!current.isEmpty()){
				final String currentId = current.poll();
				final TemporalEntityRef currentEntity = graph.findNode(currentId)
					.entity();
				for(final SocialEdgeRef edge : graph.edgesOf(currentId)){
					if(!edge.canTraverseFrom(currentEntity))
						continue;
					final TemporalEntityRef other = edge.other(currentEntity);
					if(other == null)
						continue;
					final String otherId = other.id();
					if(visited.add(otherId))
						next.add(otherId);
				}
			}
			current.addAll(next);
			next.clear();
		}

		return Collections.unmodifiableSet(visited);
	}


	/* ======================================================================
	 *                          Reconstruction
	 * ====================================================================== */

	private static SocialPath reconstructPath(final Map<String, SocialEdgeRef> previousEdge,
			final Map<String, String> previousNode, final String fromId, final String toId){
		final List<SocialEdgeRef> edges = new ArrayList<>();
		String current = toId;
		while(!current.equals(fromId)){
			final SocialEdgeRef edge = previousEdge.get(current);
			final String previous = previousNode.get(current);
			if(edge == null || previous == null)
				return null;

			edges.addFirst(edge);
			current = previous;
		}
		return SocialPath.of(edges);
	}

}
