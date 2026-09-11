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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Immutable snapshot of a social network centred on a focus entity.
 * <p>
 * A graph is produced by the social network service and consumed by the
 * layout, renderer, path finder and interaction layers. It contains:
 * <ul>
 *   <li>{@code center} — the focus node, or {@code null} for an empty graph
 *       (e.g. when the requested entity does not exist);</li>
 *   <li>{@code nodes} — all nodes reachable from the center within
 *       {@code maxDegree} edges, ordered by degree then id;</li>
 *   <li>{@code edges} — all accepted edges between those nodes, ordered by
 *       source id, target id, then relationship type;</li>
 *   <li>{@code maxDegree} — the distance bound used to build the graph;</li>
 *   <li>{@code filters} — the filter configuration that produced the graph.</li>
 * </ul>
 * <p>
 * For performance, two derived indices are pre‑computed once at
 * construction: a map from entity id to node, and a map from entity id to
 * the list of incident edges. These maps are exposed through lookup methods
 * that the path finder, the interaction handler and the layout use
 * repeatedly. Because the class holds derived state in addition to its
 * value components, it is implemented as a regular final class rather than
 * a record; the class is nonetheless fully immutable.
 * <p>
 * Invariants enforced at construction:
 * <ul>
 *   <li>if {@code center} is {@code null}, then {@code nodes} and
 *       {@code edges} must be empty;</li>
 *   <li>if {@code center} is non‑null, it must appear in {@code nodes}
 *       with degree zero;</li>
 *   <li>every endpoint of every edge must appear in {@code nodes};</li>
 *   <li>every node's degree must not exceed {@code maxDegree}.</li>
 * </ul>
 */
public final class SocialGraph{

	private final SocialNodeRef center;
	private final List<SocialNodeRef> nodes;
	private final List<SocialEdgeRef> edges;
	private final int maxDegree;
	private final SocialFilters filters;

	// Derived indices, built once at construction.
	private final Map<String, SocialNodeRef> nodesById;
	private final Map<String, List<SocialEdgeRef>> edgesByNodeId;


	private SocialGraph(final SocialNodeRef center, final List<SocialNodeRef> nodes,
		final List<SocialEdgeRef> edges, final int maxDegree, final SocialFilters filters){
		this.center = center;
		this.nodes = nodes;
		this.edges = edges;
		this.maxDegree = maxDegree;
		this.filters = filters;
		this.nodesById = buildNodesById(nodes);
		this.edgesByNodeId = buildEdgesByNodeId(edges);
	}


	/* ======================================================================
	 *                          Factories
	 * ====================================================================== */

	/**
	 * Creates a social graph and validates its internal consistency.
	 *
	 * @param center    the focus node, or {@code null} for an empty graph
	 * @param nodes     the nodes; may be {@code null}
	 * @param edges     the edges; may be {@code null}
	 * @param maxDegree the distance bound; must be non‑negative
	 * @param filters   the filter configuration; must not be {@code null}
	 * @return a new social graph
	 */
	public static SocialGraph of(final SocialNodeRef center, final List<SocialNodeRef> nodes,
		final List<SocialEdgeRef> edges, final int maxDegree, final SocialFilters filters){
		if(filters == null)
			throw new IllegalArgumentException("Filters must not be null");
		if(maxDegree < 0)
			throw new IllegalArgumentException("Max degree must not be negative");

		final List<SocialNodeRef> sortedNodes = sortNodes(nodes);
		final List<SocialEdgeRef> sortedEdges = sortEdges(edges);

		validate(center, sortedNodes, sortedEdges, maxDegree);

		return new SocialGraph(center, sortedNodes, sortedEdges, maxDegree, filters);
	}

	/**
	 * Returns an empty graph with no nodes, no edges and no center. Useful
	 * as a placeholder when the requested focus entity does not exist.
	 *
	 * @param filters the filter configuration (must not be {@code null})
	 * @return an empty graph
	 */
	public static SocialGraph empty(final SocialFilters filters){
		if(filters == null)
			throw new IllegalArgumentException("Filters must not be null");
		return new SocialGraph(null, List.of(), List.of(), 0, filters);
	}


	/* ======================================================================
	 *                          Basic accessors
	 * ====================================================================== */

	/**
	 * Returns the focus node, or {@code null} for an empty graph.
	 */
	public SocialNodeRef center(){
		return center;
	}

	/**
	 * Returns the immutable, ordered list of nodes.
	 */
	public List<SocialNodeRef> nodes(){
		return nodes;
	}

	/**
	 * Returns the immutable, ordered list of edges.
	 */
	public List<SocialEdgeRef> edges(){
		return edges;
	}

	/**
	 * Returns the distance bound used to build the graph.
	 */
	public int maxDegree(){
		return maxDegree;
	}

	/**
	 * Returns the filter configuration that produced the graph.
	 */
	public SocialFilters filters(){
		return filters;
	}


	/* ======================================================================
	 *                          Derived queries
	 * ====================================================================== */

	/**
	 * Returns whether the graph contains no nodes.
	 *
	 * @return {@code true} if the graph is empty
	 */
	public boolean isEmpty(){
		return nodes.isEmpty();
	}

	/**
	 * Returns the number of nodes.
	 *
	 * @return the node count
	 */
	public int nodeCount(){
		return nodes.size();
	}

	/**
	 * Returns the number of edges.
	 *
	 * @return the edge count
	 */
	public int edgeCount(){
		return edges.size();
	}

	/**
	 * Returns the node with the given id, or {@code null} if not present.
	 *
	 * @param id the entity id
	 * @return the matching node, or {@code null}
	 */
	public SocialNodeRef findNode(final String id){
		return (id != null? nodesById.get(id): null);
	}

	/**
	 * Returns whether a node with the given id exists.
	 *
	 * @param id the entity id
	 * @return {@code true} if the node exists
	 */
	public boolean hasNode(final String id){
		return (id != null && nodesById.containsKey(id));
	}

	/**
	 * Returns whether the graph contains the given entity.
	 *
	 * @param entity the entity to test
	 * @return {@code true} if the entity is present as a node
	 */
	public boolean hasEntity(final TemporalEntityRef entity){
		return (entity != null && nodesById.containsKey(entity.id()));
	}

	/**
	 * Returns the degree of the given entity, or {@code -1} if it is not
	 * part of the graph.
	 *
	 * @param id the entity id
	 * @return the degree, or {@code -1}
	 */
	public int degreeOf(final String id){
		final SocialNodeRef node = findNode(id);
		return (node != null? node.degree(): -1);
	}

	/**
	 * Returns the immutable list of edges incident to the given entity, or
	 * an empty list if the entity is not part of the graph.
	 *
	 * @param id the entity id
	 * @return the incident edges, never {@code null}
	 */
	public List<SocialEdgeRef> edgesOf(final String id){
		if(id == null)
			return List.of();
		return edgesByNodeId.getOrDefault(id, List.of());
	}

	/**
	 * Returns the immutable set of neighbours of the given entity, i.e.
	 * the entities reachable through a single edge. Returns an empty set if
	 * the entity is not part of the graph.
	 *
	 * @param id the entity id
	 * @return the neighbour entities, never {@code null}
	 */
	public Set<TemporalEntityRef> neighboursOf(final String id){
		final List<SocialEdgeRef> incident = edgesOf(id);
		if(incident.isEmpty())
			return Set.of();

		final TemporalEntityRef self = nodesById.get(id)
			.entity();
		final Set<TemporalEntityRef> result = new LinkedHashSet<>(incident.size());
		for(final SocialEdgeRef edge : incident){
			final TemporalEntityRef other = edge.other(self);
			if(other != null)
				result.add(other);
		}
		return Collections.unmodifiableSet(result);
	}

	/**
	 * Returns all edges connecting the two given entities, in either
	 * direction. Returns an empty list when the two entities are not
	 * directly connected.
	 *
	 * @param idA the first entity id
	 * @param idB the second entity id
	 * @return the connecting edges, never {@code null}
	 */
	public List<SocialEdgeRef> edgesBetween(final String idA, final String idB){
		if(idA == null || idB == null || idA.equals(idB))
			return List.of();

		final List<SocialEdgeRef> incident = edgesOf(idA);
		if(incident.isEmpty())
			return List.of();

		final List<SocialEdgeRef> result = new ArrayList<>();
		for(final SocialEdgeRef edge : incident){
			final String otherId = edge.other(nodesById.get(idA)
					.entity())
				.id();
			if(idB.equals(otherId))
				result.add(edge);
		}
		return Collections.unmodifiableList(result);
	}

	/**
	 * Returns the number of edges between the two given entities.
	 *
	 * @param idA the first entity id
	 * @param idB the second entity id
	 * @return the edge count, zero if they are not directly connected
	 */
	public int connectionCount(final String idA, final String idB){
		return edgesBetween(idA, idB).size();
	}

	/**
	 * Returns the set of distinct categories represented among the edges
	 * incident to the given entity.
	 *
	 * @param id the entity id
	 * @return the incident categories, never {@code null}
	 */
	public Set<SocialRelationCategory> categoriesOf(final String id){
		final List<SocialEdgeRef> incident = edgesOf(id);
		if(incident.isEmpty())
			return Set.of();

		final Set<SocialRelationCategory> result = java.util.EnumSet.noneOf(SocialRelationCategory.class);
		for(final SocialEdgeRef edge : incident)
			result.add(edge.category());
		return Collections.unmodifiableSet(result);
	}

	/**
	 * Returns the subset of nodes belonging to the given ring (degree).
	 *
	 * @param ring the ring index
	 * @return the nodes on that ring, never {@code null}
	 */
	public List<SocialNodeRef> nodesOnRing(final int ring){
		if(ring < 0 || ring > maxDegree)
			return List.of();

		final List<SocialNodeRef> result = new ArrayList<>();
		for(final SocialNodeRef node : nodes)
			if(node.isOnRing(ring))
				result.add(node);
		return Collections.unmodifiableList(result);
	}

	/**
	 * Returns the number of distinct rings present in the graph, i.e. one
	 * plus the maximum degree observed among the nodes.
	 *
	 * @return the ring count, zero for an empty graph
	 */
	public int ringCount(){
		if(nodes.isEmpty())
			return 0;
		int maxObserved = 0;
		for(final SocialNodeRef node : nodes)
			if(node.degree() > maxObserved)
				maxObserved = node.degree();
		return maxObserved + 1;
	}


	/* ======================================================================
	 *                          Internal construction
	 * ====================================================================== */

	private static List<SocialNodeRef> sortNodes(final List<SocialNodeRef> input){
		if(input == null || input.isEmpty())
			return List.of();
		final List<SocialNodeRef> copy = new ArrayList<>(input);
		copy.sort(Comparator
			.comparingInt(SocialNodeRef::degree)
			.thenComparing(node -> node.entity()
				.id()));
		return Collections.unmodifiableList(copy);
	}

	private static List<SocialEdgeRef> sortEdges(final List<SocialEdgeRef> input){
		if(input == null || input.isEmpty())
			return List.of();
		final List<SocialEdgeRef> copy = new ArrayList<>(input);
		copy.sort(Comparator
			.comparing((SocialEdgeRef edge) -> edge.source()
				.id())
			.thenComparing(edge -> edge.target()
				.id())
			.thenComparing(SocialEdgeRef::relationshipType));
		return Collections.unmodifiableList(copy);
	}

	private static Map<String, SocialNodeRef> buildNodesById(final List<SocialNodeRef> nodes){
		final Map<String, SocialNodeRef> result = new HashMap<>(nodes.size());
		for(final SocialNodeRef node : nodes){
			final SocialNodeRef previous = result.put(node.id(), node);
			if(previous != null)
				throw new IllegalArgumentException("Duplicate node id in graph: " + node.id());
		}
		return Collections.unmodifiableMap(result);
	}

	private static Map<String, List<SocialEdgeRef>> buildEdgesByNodeId(final List<SocialEdgeRef> edges){
		final Map<String, List<SocialEdgeRef>> result = new HashMap<>();
		for(final SocialEdgeRef edge : edges){
			result.computeIfAbsent(edge.source()
					.id(), k -> new ArrayList<>())
				.add(edge);
			// Avoid double registration when the two endpoints share the same
			// id: cannot happen because SocialEdgeRef rejects self-loops.
			result.computeIfAbsent(edge.target()
					.id(), k -> new ArrayList<>())
				.add(edge);
		}
		// Freeze the inner lists.
		final Map<String, List<SocialEdgeRef>> frozen = new HashMap<>(result.size());
		for(final Map.Entry<String, List<SocialEdgeRef>> entry : result.entrySet())
			frozen.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
		return Collections.unmodifiableMap(frozen);
	}

	private static void validate(final SocialNodeRef center, final List<SocialNodeRef> nodes,
		final List<SocialEdgeRef> edges, final int maxDegree){
		if(center == null){
			if(!nodes.isEmpty())
				throw new IllegalArgumentException("A graph with a null center must have no nodes");
			if(!edges.isEmpty())
				throw new IllegalArgumentException("A graph with a null center must have no edges");
			return;
		}

		if(!center.isCenter())
			throw new IllegalArgumentException("Center node must have degree zero, got: " + center.degree());

		final Set<String> nodeIds = new java.util.HashSet<>(nodes.size());
		for(final SocialNodeRef node : nodes){
			nodeIds.add(node.id());
			if(node.degree() > maxDegree)
				throw new IllegalArgumentException("Node " + node.id()
					+ " has degree " + node.degree() + " exceeding maxDegree " + maxDegree);
		}

		if(!nodeIds.contains(center.id()))
			throw new IllegalArgumentException("Center node " + center.id() + " is not part of the node list");

		for(final SocialEdgeRef edge : edges){
			if(!nodeIds.contains(edge.source()
				.id()))
				throw new IllegalArgumentException("Edge endpoint not in graph: " + edge.source()
					.id());
			if(!nodeIds.contains(edge.target()
				.id()))
				throw new IllegalArgumentException("Edge endpoint not in graph: " + edge.target()
					.id());
		}
	}


	@Override
	public String toString(){
		final String centerLabel = (center != null? center.entity()
			.displayLabel(): "(empty)");
		return "SocialGraph[center=" + centerLabel
			+ ", nodes=" + nodes.size()
			+ ", edges=" + edges.size()
			+ ", maxDegree=" + maxDegree
			+ "]";
	}

}
