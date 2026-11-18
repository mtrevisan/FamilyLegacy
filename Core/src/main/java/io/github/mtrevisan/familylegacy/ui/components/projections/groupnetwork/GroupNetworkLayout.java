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
package io.github.mtrevisan.familylegacy.ui.components.projections.groupnetwork;

import io.github.mtrevisan.familylegacy.groups.GroupNetwork;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


/**
 * The layout of a {@link GroupNetwork}: levels from the top, the wholes above their parts.
 * <p>
 * It is not a generic graph layout: it uses what the network is. The level of a group is given by the {@code part_of} relationships (a
 * group is one level under the group it is part of, the lowest one if it is part of more than one); a group with no such relationship
 * takes the level of the group it is associated with that is nearer the center ({@code associate} does not make a hierarchy), and the
 * center without any starts at the top. A cycle of {@code part_of} (a data error) does not loop: the relationship that closes it is
 * ignored for the levels. Inside a level the groups are ordered under their parents, or by name; every level is centered.
 * <p>
 * The layout is deterministic: the same network always gives the same positions.
 */
public final class GroupNetworkLayout{

	public static final int NODE_WIDTH = 160;
	public static final int NODE_HEIGHT = 46;
	public static final int HORIZONTAL_GAP = 30;
	public static final int VERTICAL_GAP = 70;
	public static final int MARGIN = 20;


	/**
	 * The positions of the groups.
	 *
	 * @param bounds  the rectangle of each group, by identifier
	 * @param levels  the level of each group, by identifier (0 is the top)
	 * @param size    the size needed to show all, with the margins
	 */
	public record Layout(Map<String, Rectangle> bounds, Map<String, Integer> levels, Dimension size){

		public Layout{
			bounds = Map.copyOf(bounds);
			levels = Map.copyOf(levels);
		}

	}


	private GroupNetworkLayout(){}


	/**
	 * @param network	The network.
	 * @return	The layout of the network.
	 */
	public static Layout compute(final GroupNetwork network){
		if(network.isEmpty())
			return new Layout(Map.of(), Map.of(), new Dimension(2 * MARGIN, 2 * MARGIN));

		final Map<String, List<String>> parents = new HashMap<>();
		final Map<String, List<String>> children = new HashMap<>();
		final Map<String, List<String>> associates = new HashMap<>();
		for(final GroupNetwork.Edge edge : network.edges())
			if(edge.kind() == GroupNetwork.Kind.PART_OF){
				parents.computeIfAbsent(edge.firstId(), id -> new ArrayList<>())
					.add(edge.secondId());
				children.computeIfAbsent(edge.secondId(), id -> new ArrayList<>())
					.add(edge.firstId());
			}
			else{
				associates.computeIfAbsent(edge.firstId(), id -> new ArrayList<>())
					.add(edge.secondId());
				associates.computeIfAbsent(edge.secondId(), id -> new ArrayList<>())
					.add(edge.firstId());
			}

		final Map<String, String> labels = new HashMap<>();
		for(final GroupNetwork.Node node : network.nodes())
			labels.put(node.id(), node.label());

		final Map<String, Integer> levels = levels(network, parents, associates);

		final Map<Integer, List<String>> rows = new TreeMap<>();
		for(final GroupNetwork.Node node : network.nodes())
			rows.computeIfAbsent(levels.get(node.id()), level -> new ArrayList<>())
				.add(node.id());

		//the order inside a level: under the parents (their mean position in the level above), then by name
		final Map<String, Double> position = new HashMap<>();
		for(final Map.Entry<Integer, List<String>> row : rows.entrySet()){
			final List<String> ids = row.getValue();
			ids.sort(Comparator.<String>comparingDouble(id -> meanParentPosition(id, parents, position))
				.thenComparing(id -> labels.get(id), String.CASE_INSENSITIVE_ORDER)
				.thenComparing(Comparator.naturalOrder()));
			for(int i = 0; i < ids.size(); i ++)
				position.put(ids.get(i), (double)i);
		}

		int widest = 0;
		for(final List<String> ids : rows.values())
			widest = Math.max(widest, ids.size());
		final int contentWidth = widest * NODE_WIDTH + (widest - 1) * HORIZONTAL_GAP;

		final Map<String, Rectangle> bounds = new HashMap<>();
		int rowIndex = 0;
		for(final List<String> ids : rows.values()){
			final int rowWidth = ids.size() * NODE_WIDTH + (ids.size() - 1) * HORIZONTAL_GAP;
			final int left = MARGIN + (contentWidth - rowWidth) / 2;
			final int top = MARGIN + rowIndex * (NODE_HEIGHT + VERTICAL_GAP);
			for(int i = 0; i < ids.size(); i ++)
				bounds.put(ids.get(i), new Rectangle(left + i * (NODE_WIDTH + HORIZONTAL_GAP), top, NODE_WIDTH, NODE_HEIGHT));
			rowIndex ++;
		}

		final Dimension size = new Dimension(contentWidth + 2 * MARGIN, rows.size() * NODE_HEIGHT + (rows.size() - 1) * VERTICAL_GAP + 2 * MARGIN);
		return new Layout(bounds, levels, size);
	}


	/** The mean position, in its level, of the parents of a group already placed; 0 for a group with none (they are ordered by name). */
	private static double meanParentPosition(final String id, final Map<String, List<String>> parents, final Map<String, Double> position){
		double sum = 0.;
		int count = 0;
		for(final String parent : parents.getOrDefault(id, List.of())){
			final Double parentPosition = position.get(parent);
			if(parentPosition != null){
				sum += parentPosition;
				count ++;
			}
		}
		return (count > 0? sum / count: 0.);
	}

	private static Map<String, Integer> levels(final GroupNetwork network, final Map<String, List<String>> parents,
			final Map<String, List<String>> associates){
		final Map<String, Integer> levels = new HashMap<>();
		for(final GroupNetwork.Node node : network.nodes())
			level(node.id(), parents, levels, new HashSet<>());

		//a group with no part_of relationship takes the level of the associated group nearer the center
		final Set<String> inHierarchy = new HashSet<>();
		for(final Map.Entry<String, List<String>> entry : parents.entrySet()){
			inHierarchy.add(entry.getKey());
			inHierarchy.addAll(entry.getValue());
		}
		final Deque<String> queue = new ArrayDeque<>();
		final Set<String> reached = new HashSet<>();
		queue.add(network.centerId());
		reached.add(network.centerId());
		while(!queue.isEmpty()){
			final String id = queue.poll();
			for(final String associate : associates.getOrDefault(id, List.of())){
				if(!reached.add(associate))
					continue;

				if(!inHierarchy.contains(associate))
					levels.put(associate, levels.get(id));
				queue.add(associate);
			}
		}
		return levels;
	}

	/** The level of a group: one under its lowest parent; the groups on the path are the ones that close a cycle if met again. */
	private static int level(final String id, final Map<String, List<String>> parents, final Map<String, Integer> levels,
			final Set<String> path){
		final Integer known = levels.get(id);
		if(known != null)
			return known;

		path.add(id);
		int level = 0;
		for(final String parent : parents.getOrDefault(id, List.of()))
			if(!path.contains(parent))
				level = Math.max(level, level(parent, parents, levels, path) + 1);
		path.remove(id);

		levels.put(id, level);
		return level;
	}

}
