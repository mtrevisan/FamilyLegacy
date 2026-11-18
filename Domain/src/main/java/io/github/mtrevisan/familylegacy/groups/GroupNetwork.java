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
package io.github.mtrevisan.familylegacy.groups;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import java.util.List;


/**
 * The network of the groups around a group (view B of the group views): the groups as nodes, joined by the {@code part_of} (a group
 * is part of another) and {@code associate} (two groups are associated) relationships.
 * <p>
 * The network is always a neighborhood of a group, never the whole file: the groups at most {@code maxDepth} relationships away from the
 * center, as it is built by {@link GroupNetworkBuilder}. It can be restricted to a year: the relationships and memberships that were not
 * in effect in that year are left out.
 *
 * @param centerId  the identifier of the group at the center
 * @param maxDepth  how many relationships away from the center a group can be
 * @param year      the year the network is restricted to, {@code null} for any
 * @param nodes     the groups, the center first, then by name
 * @param edges     the relationships between the groups
 */
public record GroupNetwork(String centerId, int maxDepth, Integer year, List<Node> nodes, List<Edge> edges){

	/** The kind of a relationship between two groups. */
	public enum Kind{
		/** The first group is part of the second one (a hierarchy: the second is above). */
		PART_OF,
		/** The two groups are associated (no hierarchy). */
		ASSOCIATE
	}


	/**
	 * A group of the network.
	 *
	 * @param id       the identifier of the group
	 * @param label    the name of the group
	 * @param record   the record of the group
	 * @param members  the individuals that are members of the group (in the year, if the network is restricted to one; with those of
	 *                 its subgroups if the network was built so); the memberships with no date count
	 * @param center   whether it is the group at the center
	 */
	public record Node(String id, String label, FLEFRecord record, int members, boolean center){}


	/**
	 * A relationship between two groups. For a {@link Kind#PART_OF} the first group is the part, the second the whole.
	 *
	 * @param kind           the kind of relationship
	 * @param firstId        the group that is the subject of the relationship
	 * @param secondId       the group that is its target
	 * @param relationship   the record of the relationship
	 * @param role           the role of the relationship (can be {@code null})
	 * @param dates          the dates of the relationship as text, empty if it has none
	 */
	public record Edge(Kind kind, String firstId, String secondId, FLEFRecord relationship, String role, String dates){}


	public GroupNetwork{
		nodes = List.copyOf(nodes);
		edges = List.copyOf(edges);
	}


	/** @return	The empty network: there is no group at the center. */
	public static GroupNetwork empty(){
		return new GroupNetwork(null, 0, null, List.of(), List.of());
	}

	/** @return	Whether there is no group. */
	public boolean isEmpty(){
		return nodes.isEmpty();
	}

}
