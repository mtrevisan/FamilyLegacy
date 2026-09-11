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

/**
 * Directionality of a social edge in the Social Network views.
 * <p>
 * The FLEF protocol always records a relationship with an explicit
 * {@code subject} and {@code target}, but the semantics of the pair differ
 * by relationship type:
 * <ul>
 *   <li>asymmetric types (e.g. {@code group_member},
 *       {@code biological_child}, {@code part_of}) describe a role of the
 *       subject relative to the target, so the edge carries direction;</li>
 *   <li>symmetric types (e.g. {@code civil_spouse},
 *       {@code cohabiting_partner}, most {@code associate} instances)
 *       describe a mutual relationship in which the two roles are
 *       interchangeable, so the edge is undirected for rendering and
 *       navigation purposes.</li>
 * </ul>
 * The direction affects only the visual presentation and the navigation
 * semantics: the underlying FLEF record always preserves its
 * {@code subject} / {@code target} orientation.
 */
public enum SocialEdgeDirection{

	/**
	 * The edge has no direction: both endpoints play the same role. The
	 * renderer draws a plain line without arrow heads, and traversal is
	 * allowed in both directions.
	 */
	UNDIRECTED,

	/**
	 * The edge is directed from subject to target. The renderer draws an
	 * arrow head at the target end, and traversal is allowed only from
	 * subject to target.
	 */
	DIRECTED;


	/**
	 * Returns whether this direction admits traversal from the source to
	 * the target only.
	 *
	 * @return {@code true} for {@link #DIRECTED}
	 */
	public boolean isDirected(){
		return (this == DIRECTED);
	}

	/**
	 * Returns whether this direction admits traversal in both directions.
	 *
	 * @return {@code true} for {@link #UNDIRECTED}
	 */
	public boolean isUndirected(){
		return (this == UNDIRECTED);
	}

	/**
	 * Returns whether the given endpoint pair is reachable along this edge
	 * when starting from {@code fromSource}.
	 * <p>
	 * For {@link #UNDIRECTED} edges, both directions are always reachable.
	 * For {@link #DIRECTED} edges, only the forward direction is reachable.
	 *
	 * @param fromSource {@code true} if the traversal starts at the edge
	 *                   source; {@code false} if it starts at the edge target
	 * @return {@code true} if the traversal is allowed
	 */
	public boolean canTraverse(final boolean fromSource){
		return (this == UNDIRECTED || fromSource);
	}

}
