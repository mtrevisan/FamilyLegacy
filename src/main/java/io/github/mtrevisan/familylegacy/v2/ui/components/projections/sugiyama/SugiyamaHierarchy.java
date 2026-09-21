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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.sugiyama;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeService;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsData;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Step 1 of the Sugiyama framework.
 * <p>
 * Every vertex is an individual. The layer of a vertex is its ancestor
 * generation: the root is at depth 0, its parents at depth 1, its
 * grandparents at depth 2, and so on. Edges go from parent to child in
 * the direction root-to-ancestor, i.e. a parent is always one layer
 * above its child in the drawing.
 * <p>
 * Each vertex is given a <b>path signature</b>: the sequence of
 * {@code F}/{@code M}/{@code P}/{@code C} steps that leads from the root
 * to the vertex. The signature determines the left-to-right order
 * within a layer.
 * <p>
 * <b>Hourglass extension.</b> When {@code showPartner} is {@code true},
 * the graph also includes partners and descendants of the root. The
 * expansion runs to a fixed point: every time a new node is discovered
 * (going down or going up), the missing directions are walked. This is
 * necessary because a partner discovered by descending may have its own
 * ancestors that are not reachable from the root by ascending.
 */
public final class SugiyamaHierarchy{

	private static final String SUFFIX_FEMALE = "F";
	private static final String SUFFIX_MALE = "M";
	/** Prefix used for the path signature of a partner. */
	private static final String PARTNER_PREFIX = "P";
	/** Prefix used for the path signature of a child. */
	private static final String CHILD_PREFIX = "C";
	private static final String DUMMY_ID_PREFIX = "__d";
	private static final String ABOUT = "~";

	private static final String TAG_SEX = "sex";

	private static final String ENUM_SEX_MALE = "male";

	/** Default descendant depth when the caller does not specify one. */
	private static final int DEFAULT_MAX_DESCENDANTS = 3;
	/** Safety bound on the fixed-point iteration. */
	private static final int MAX_EXPANSION_PASSES = 6;

	public record Edge(String fromId, String toId){}

	public record Hierarchy(
		String rootId,
		List<List<String>> layers,
		Set<String> dummyIds,
		List<Edge> edges,
		Map<String, List<String>> parentsOf,
		Map<String, List<String>> childrenOf,
		Map<String, IndividualData> dataById,
		Map<String, String> pathById){

		Map<String, String> buildParentMap(){
			final Map<String, String> parentOf = new HashMap<>(parentsOf.size());
			final Map<String, Set<String>> partnersOf = new HashMap<>(parentsOf.size());

			for(final List<String> parents : parentsOf.values()){
				final int size = parents.size();
				if(size < 2)
					continue;

				for(int i = 0; i < size; i ++){
					final String a = parents.get(i);
					for(int j = i + 1; j < size; j ++){
						final String b = parents.get(j);
						if(!a.equals(b)){
							partnersOf.computeIfAbsent(a, k -> new LinkedHashSet<>()).add(b);
							partnersOf.computeIfAbsent(b, k -> new LinkedHashSet<>()).add(a);
						}
					}
				}
			}
			for(final Map.Entry<String, Set<String>> e : partnersOf.entrySet())
				parentOf.putIfAbsent(e.getKey(), e.getValue().iterator().next());
			return parentOf;
		}
	}


	private SugiyamaHierarchy(){}


	/**
	 * Builds the individual hierarchy from the given root, using the
	 * default descendant depth.
	 */
	public static Hierarchy build(final FLEFModel model, final String rootId, final TreeService treeService,
		final int maxAncestors, final boolean showPartner){
		return build(model, rootId, treeService, maxAncestors, DEFAULT_MAX_DESCENDANTS, showPartner);
	}

	/**
	 * Builds the individual hierarchy from the given root.
	 *
	 * @param model          the FLEF model
	 * @param rootId         the focus individual id
	 * @param treeService    the service used to look up parents and children
	 * @param maxAncestors   the maximum number of ancestor generations
	 * @param maxDescendants the maximum number of descendant generations (ignored when {@code showPartner} is
	 * 	{@code false})
	 * @param showPartner    if {@code true}, partners and descendants are included in the graph
	 * @return the hierarchy
	 */
	public static Hierarchy build(final FLEFModel model, final String rootId, final TreeService treeService,
		final int maxAncestors, final int maxDescendants, final boolean showPartner){
		if(model == null || rootId == null || treeService == null)
			return new Hierarchy(StringUtils.EMPTY, List.of(), Set.of(), List.of(), Map.of(), Map.of(), Map.of(),
				Map.of());

		final Map<String, List<FLEFRecord>> eventMap = treeService.getEventMap();
		final Map<String, IndividualData> dataById = new LinkedHashMap<>();
		final Map<String, String> pathById = new HashMap<>();
		final Map<String, Integer> depthById = new HashMap<>();
		final Set<String> collected = new LinkedHashSet<>();

		// Seed the root
		collected.add(rootId);
		depthById.put(rootId, 0);
		pathById.put(rootId, StringUtils.EMPTY);
		loadData(model, eventMap, rootId, dataById);

		// First pass: walk ancestors of the root.
		bfsUpwards(model, rootId, treeService, maxAncestors, eventMap,
			collected, depthById, pathById, dataById);

		// Hourglass expansion. The graph is grown to a fixed point:
		// descending from every known node discovers partners and
		// children; each newly discovered node is then walked upwards
		// to collect its own ancestors. This is what makes Davide
		// (father of b, where b is partner of the root a) appear in
		// the graph even though he is not reachable from the root by
		// ascending alone.
		final List<Edge> extraEdges = new ArrayList<>();
		if(showPartner){
			for(int pass = 0; pass < MAX_EXPANSION_PASSES; pass ++){
				final int beforeSize = collected.size();
				final Set<String> before = new LinkedHashSet<>(collected);

				// Descend from every known node, up to maxDescendants.
				extraEdges.addAll(expandDescendants(treeService, maxDescendants,
					collected, depthById, pathById, dataById));

				// For each newly discovered node, walk up its ancestors.
				final Set<String> newlyDiscovered = new LinkedHashSet<>(collected);
				newlyDiscovered.removeAll(before);
				for(final String newId : newlyDiscovered)
					bfsUpwards(model, newId, treeService, maxAncestors, eventMap,
						collected, depthById, pathById, dataById);

				if(collected.size() == beforeSize)
					break;
			}
		}

		// Assign layers. The formula "maxDepth - depth" places ancestors
		// above the root, the root at layer maxDepth, and descendants
		// below (layer maxDepth + |depth|).
		int maxDepth = 0;
		for(final int d : depthById.values())
			maxDepth = Math.max(maxDepth, d);
		final Map<String, Integer> layerOf = new HashMap<>(depthById.size());
		for(final Map.Entry<String, Integer> e : depthById.entrySet())
			layerOf.put(e.getKey(), maxDepth - e.getValue());

		// Parent-child edges. The tree service is the primary source, but
		// it is sex-based and may omit one of the two parents when the
		// couple has unknown or same sex. The extra edges recovered from
		// the partner resolution are merged on top.
		final List<Edge> edges = new ArrayList<>();
		final Set<Long> edgeKeys = new HashSet<>(); // Fast long hash key for edges instead of String concatenation
		final Map<String, List<String>> parentsOf = new HashMap<>(collected.size());
		final Map<String, List<String>> childrenOf = new HashMap<>(collected.size());

		for(final String id : collected){
			parentsOf.put(id, new ArrayList<>());
			childrenOf.put(id, new ArrayList<>());
		}

		for(final String id : collected){
			for(final FLEFRecord parent : treeService.getParents(id)){
				final String pid = parent.getId();
				if(pid == null || !collected.contains(pid))
					continue;

				final long key = (((long)pid.hashCode()) << 32) | (id.hashCode() & 0xFFFFFFFFL);
				if(!edgeKeys.add(key))
					continue;

				edges.add(new Edge(pid, id));
				parentsOf.get(id).add(pid);
				childrenOf.get(pid).add(id);
			}
		}

		for(final Edge e : extraEdges){
			if(!collected.contains(e.fromId()) || !collected.contains(e.toId()))
				continue;

			final long key = (((long)e.fromId().hashCode()) << 32) | (e.toId().hashCode() & 0xFFFFFFFFL);
			if(!edgeKeys.add(key))
				continue;

			edges.add(e);
			parentsOf.get(e.toId()).add(e.fromId());
			childrenOf.get(e.fromId()).add(e.toId());
		}

		// Normalization: dummy nodes for edges that span more than one layer
		final Set<String> dummyIds = new HashSet<>();
		final List<Edge> augEdges = new ArrayList<>();
		final Map<String, List<String>> augParents = new HashMap<>(collected.size());
		final Map<String, List<String>> augChildren = new HashMap<>(collected.size());
		final Map<String, Integer> augLayer = new HashMap<>(collected.size());

		for(final String id : collected){
			augParents.put(id, new ArrayList<>());
			augChildren.put(id, new ArrayList<>());
			augLayer.put(id, layerOf.getOrDefault(id, 0));
		}

		int dummyCounter = 0;
		for(final Edge e : edges){
			final int lp = layerOf.getOrDefault(e.fromId(), 0);
			final int lc = layerOf.getOrDefault(e.toId(), 0);
			if(lc <= lp)
				continue;

			String prev = e.fromId();
			for(int l = lp + 1; l < lc; l++){
				final String dummyId = DUMMY_ID_PREFIX + (dummyCounter++);
				dummyIds.add(dummyId);
				augParents.put(dummyId, new ArrayList<>());
				augChildren.put(dummyId, new ArrayList<>());
				augLayer.put(dummyId, l);
				augEdges.add(new Edge(prev, dummyId));
				augChildren.get(prev).add(dummyId);
				augParents.get(dummyId).add(prev);
				prev = dummyId;
			}
			augEdges.add(new Edge(prev, e.toId()));
			augChildren.get(prev).add(e.toId());
			augParents.get(e.toId()).add(prev);
		}

		// Group nodes by layer, sorted by path.
		int maxLayer = 0;
		for(final int l : augLayer.values())
			maxLayer = Math.max(maxLayer, l);

		final List<List<String>> layers = new ArrayList<>(maxLayer + 1);
		for(int i = 0; i <= maxLayer; i ++)
			layers.add(new ArrayList<>());

		for(final Map.Entry<String, Integer> e : augLayer.entrySet())
			layers.get(e.getValue()).add(e.getKey());

		for(final List<String> layer : layers)
			layer.sort(Comparator
				.comparing((String id) -> pathById.getOrDefault(id, ABOUT))
				.thenComparing(id -> id));

		return new Hierarchy(rootId, layers, dummyIds, augEdges,
			augParents, augChildren, dataById, pathById);
	}


	/* ======================================================================
	 *                          Upward walk
	 * ====================================================================== */

	/**
	 * Walks the ancestors of {@code startId}, adding every individual
	 * not yet collected. Depth is increased by one per generation, and
	 * the walk stops at {@code maxAncestors}.
	 * <p>
	 * Nodes already in {@code collected} are not re-enqueued, so the
	 * method is safe to call from multiple starting points: the union
	 * of all the walks converges to the full ancestor closure of the
	 * starting set.
	 */
	private static void bfsUpwards(final FLEFModel model, final String startId, final TreeService treeService,
			final int maxAncestors, final Map<String, List<FLEFRecord>> eventMap,
			final Set<String> collected, final Map<String, Integer> depthById,
			final Map<String, String> pathById, final Map<String, IndividualData> dataById){
		final Deque<String> queue = new ArrayDeque<>();
		queue.add(startId);

		while(!queue.isEmpty()){
			final String id = queue.poll();
			final int depth = depthById.getOrDefault(id, 0);

			loadData(model, eventMap, id, dataById);

			if(depth >= maxAncestors)
				continue;

			final String path = pathById.getOrDefault(id, StringUtils.EMPTY);
			for(final FLEFRecord parent : treeService.getParents(id)){
				final String pid = parent.getId();
				if(pid == null)
					continue;

				final String suffix = (isMale(parent)? SUFFIX_FEMALE: SUFFIX_MALE);
				pathById.putIfAbsent(pid, path + suffix);
				if(collected.add(pid)){
					depthById.put(pid, depth + 1);
					queue.add(pid);
				}
			}
		}
	}


	/* ======================================================================
	 *                          Downward walk
	 * ====================================================================== */

	/**
	 * Expands the graph downward from every node that is already in
	 * {@code collected}, up to {@code maxDescendants} generations from
	 * the root. For every child discovered, the other parent (if any) is
	 * added as a partner at the same layer, and the
	 * {@code partner -> child} edge is recorded so that the layout can
	 * center the child between both parents.
	 *
	 * @return the {@code partner -> child} edges discovered during the
	 * expansion
	 */
	private static List<Edge> expandDescendants(final TreeService treeService, final int maxDescendants,
		final Set<String> collected, final Map<String, Integer> depthById,
		final Map<String, String> pathById, final Map<String, IndividualData> dataById){
		final List<Edge> extraEdges = new ArrayList<>();
		if(maxDescendants <= 0)
			return extraEdges;

		// Snapshot of the nodes to expand: we do not want to expand
		// nodes discovered during the same call, because they will be
		// expanded on the next pass.
		final List<String> nodes = new ArrayList<>(collected);
		final Map<String, Integer> nextPartnerIdxByParent = new HashMap<>();
		final Map<String, Integer> nextChildIdxByParent = new HashMap<>();

		for(final String parentId : nodes){
			final int parentDepth = depthById.getOrDefault(parentId, 0);
			// Only descend from the root and from its descendants. Ancestors
			// (depth > 0) are leaves at the top of the graph: descending from
			// them would pull in their other children (siblings of the root's
			// ancestors), which are unrelated to the current focus.
			if(parentDepth > 0 || -parentDepth >= maxDescendants)
				continue;

			final Map<IndividualData, SiblingsData> childrenByPartner = treeService.buildChildrenData(parentId);
			if(childrenByPartner.isEmpty())
				continue;

			final String parentPath = pathById.getOrDefault(parentId, StringUtils.EMPTY);
			int partnerIdx = nextPartnerIdxByParent.getOrDefault(parentId, 0);
			int childIdx = nextChildIdxByParent.getOrDefault(parentId, 0);

			for(final Map.Entry<IndividualData, SiblingsData> entry : childrenByPartner.entrySet()){
				final IndividualData partnerData = entry.getKey();
				final SiblingsData childrenData = entry.getValue();
				final String partnerId = (partnerData != null? partnerData.getId(): null);

				if(partnerId != null && collected.add(partnerId)){
					depthById.put(partnerId, parentDepth);
					pathById.putIfAbsent(partnerId, parentPath + PARTNER_PREFIX + (partnerIdx++));
					dataById.putIfAbsent(partnerId, partnerData);
				}

				if(childrenData == null)
					continue;

				for(final IndividualData childData : childrenData.getSiblings()){
					final String childId = childData.getId();
					if(childId == null)
						continue;

					if(collected.add(childId)){
						depthById.put(childId, parentDepth - 1);
						pathById.putIfAbsent(childId, parentPath + CHILD_PREFIX + (childIdx++));
						dataById.putIfAbsent(childId, childData);
					}

					if(partnerId != null)
						extraEdges.add(new Edge(partnerId, childId));
				}
			}

			nextPartnerIdxByParent.put(parentId, partnerIdx);
			nextChildIdxByParent.put(parentId, childIdx);
		}
		return extraEdges;
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private static void loadData(final FLEFModel model, final Map<String, List<FLEFRecord>> eventMap,
		final String id, final Map<String, IndividualData> dataById){
		if(dataById.containsKey(id))
			return;

		final FLEFRecord record = model.getRecordById(id);
		if(record != null)
			dataById.put(id, IndividualData.create(record, eventMap, model));
	}

	private static boolean isMale(final FLEFRecord record){
		if(record == null)
			return false;

		final String raw = FLEFRecordHelper.getChildValue(record, TAG_SEX);
		return ENUM_SEX_MALE.equalsIgnoreCase(raw);
	}

}
