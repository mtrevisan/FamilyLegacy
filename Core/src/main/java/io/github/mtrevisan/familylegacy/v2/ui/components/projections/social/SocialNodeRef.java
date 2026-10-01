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

import java.util.Objects;


/**
 * A single node in the Social Network views.
 * <p>
 * A node wraps a {@link TemporalEntityRef} (the underlying individual,
 * group or place) and enriches it with two projection‑specific pieces of
 * information:
 * <ul>
 *   <li>{@code degree} — the distance, in edges, from the focus entity
 *       used to build the network. The focus itself has degree 0, its
 *       direct contacts have degree 1, and so on. The degree drives the
 *       concentric layout and is used by the path finder as an upper
 *       bound.</li>
 *   <li>{@code primaryCategory} — the {@link SocialRelationCategory} most
 *       represented among the edges incident to this node. It provides a
 *       quick visual hint of the dominant role of the node in the network
 *       (e.g. a node whose contacts are mostly religious witnesses is
 *       marked as {@code RELIGIOUS}). It is a presentation‑layer summary:
 *       the actual categories remain available on each individual edge.</li>
 * </ul>
 * Equality and hash code are based solely on the wrapped entity, so that a
 * node and its underlying {@code TemporalEntityRef} can be compared across
 * different network reconstructions.
 */
public record SocialNodeRef(TemporalEntityRef entity, int degree, SocialRelationCategory primaryCategory){

	/**
	 * Compact constructor with validation.
	 */
	public SocialNodeRef{
		if(entity == null)
			throw new IllegalArgumentException("Entity must not be null");
		if(degree < 0)
			throw new IllegalArgumentException("Degree must not be negative");
		if(primaryCategory == null)
			throw new IllegalArgumentException("Primary category must not be null");
	}


	/**
	 * Returns the id of the underlying entity, for convenience.
	 *
	 * @return the entity id, never {@code null}
	 */
	public String id(){
		return entity.id();
	}

	/**
	 * Returns whether this node is the focus of the network.
	 *
	 * @return {@code true} if the degree is zero
	 */
	public boolean isCenter(){
		return (degree == 0);
	}

	/**
	 * Returns whether this node belongs to the given ring.
	 *
	 * @param ring the ring index to test
	 * @return {@code true} if the degree equals the ring index
	 */
	public boolean isOnRing(final int ring){
		return (degree == ring);
	}

	/**
	 * Returns a copy of this node with the given primary category. Useful
	 * when the dominant category of a node is recomputed after a filter
	 * change.
	 *
	 * @param category the new primary category (must not be {@code null})
	 * @return a new node
	 */
	public SocialNodeRef withPrimaryCategory(final SocialRelationCategory category){
		return new SocialNodeRef(entity, degree, category);
	}


	/**
	 * Equality is based on the underlying entity only.
	 */
	@Override
	public boolean equals(final Object other){
		if(this == other)
			return true;
		if(!(other instanceof SocialNodeRef node))
			return false;
		return entity.equals(node.entity);
	}

	/**
	 * Hash code is based on the underlying entity only.
	 */
	@Override
	public int hashCode(){
		return Objects.hashCode(entity);
	}

	@Override
	public String toString(){
		return entity.displayLabel() + " (degree " + degree + ", " + primaryCategory + ")";
	}

}
