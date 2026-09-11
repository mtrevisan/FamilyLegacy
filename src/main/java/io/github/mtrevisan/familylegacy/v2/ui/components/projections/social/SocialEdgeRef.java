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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalEntityRef;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalSpan;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;


/**
 * A single social edge in the Social Network views.
 * <p>
 * An edge represents an enduring non‑parental relationship between two
 * entities. It is a projection of either a {@code RelationshipRecord}
 * (types {@code associate}, {@code group_member}, {@code part_of}) or an
 * {@code EventParticipationRecord} that expresses a relational role
 * (e.g. {@code witness}, {@code officiant}, {@code executor}).
 * <p>
 * The edge preserves the FLEF orientation: {@code source} is the
 * {@code subject} of the underlying record and {@code target} is its
 * {@code target}. The {@link SocialEdgeDirection} field records whether the
 * edge should be rendered and traversed as directed or undirected.
 * <p>
 * Equality and hash code are based solely on the underlying FLEF record id,
 * so that two edges representing the same relationship compare equal
 * regardless of the rendering context in which they were produced.
 */
public record SocialEdgeRef(
	TemporalEntityRef source,
	TemporalEntityRef target,
	String relationshipType,
	String role,
	SocialRelationCategory category,
	SocialEdgeDirection direction,
	TemporalSpan span,
	FLEFRecord sourceRecord
){

	/**
	 * Compact constructor with validation and normalization.
	 */
	public SocialEdgeRef{
		if(source == null)
			throw new IllegalArgumentException("Source must not be null");
		if(target == null)
			throw new IllegalArgumentException("Target must not be null");
		if(source.equals(target))
			throw new IllegalArgumentException("Source and target must be distinct entities");
		if(relationshipType == null || relationshipType.isBlank())
			throw new IllegalArgumentException("Relationship type must not be null or blank");
		if(category == null)
			throw new IllegalArgumentException("Category must not be null");
		if(direction == null)
			throw new IllegalArgumentException("Direction must not be null");
		if(span == null)
			throw new IllegalArgumentException("Span must not be null");
		if(sourceRecord == null || sourceRecord.getId() == null)
			throw new IllegalArgumentException("Source record must not be null and must have an id");
		if(role == null)
			role = StringUtils.EMPTY;
	}


	/**
	 * Returns whether the edge has an explicit role (e.g. {@code "president"},
	 * {@code "witness"}).
	 *
	 * @return {@code true} if the role is non‑empty
	 */
	public boolean hasRole(){
		return !role.isEmpty();
	}

	/**
	 * Returns whether the edge is directed.
	 *
	 * @return {@code true} for {@link SocialEdgeDirection#DIRECTED}
	 */
	public boolean isDirected(){
		return direction.isDirected();
	}

	/**
	 * Returns whether the given entity is one of the two endpoints.
	 *
	 * @param entity the entity to test (must not be {@code null})
	 * @return {@code true} if the entity is the source or the target
	 */
	public boolean involves(final TemporalEntityRef entity){
		return (source.equals(entity) || target.equals(entity));
	}

	/**
	 * Returns the opposite endpoint relative to the given entity, or
	 * {@code null} if the entity is not one of the endpoints.
	 *
	 * @param entity the entity whose opposite endpoint is requested
	 * @return the opposite endpoint, or {@code null}
	 */
	public TemporalEntityRef other(final TemporalEntityRef entity){
		if(source.equals(entity))
			return target;
		if(target.equals(entity))
			return source;
		return null;
	}

	/**
	 * Returns whether traversal of this edge is allowed when starting from
	 * the given entity.
	 *
	 * @param from the starting entity (must be one of the endpoints)
	 * @return {@code true} if traversal is permitted in that direction
	 */
	public boolean canTraverseFrom(final TemporalEntityRef from){
		return direction.canTraverse(source.equals(from));
	}


	/**
	 * Equality is based on the underlying FLEF record id only, so that two
	 * edges derived from the same record compare equal.
	 */
	@Override
	public boolean equals(final Object other){
		if(this == other)
			return true;
		if(!(other instanceof SocialEdgeRef edge))
			return false;
		return Objects.equals(sourceRecord.getId(), edge.sourceRecord.getId());
	}

	/**
	 * Hash code is based on the underlying FLEF record id only.
	 */
	@Override
	public int hashCode(){
		return Objects.hashCode(sourceRecord.getId());
	}

	@Override
	public String toString(){
		return relationshipType + " (" + source.id() + (isDirected()? " -> ": " -- ")
			+ target.id() + ")" + (hasRole()? " [" + role + "]": StringUtils.EMPTY);
	}

}
