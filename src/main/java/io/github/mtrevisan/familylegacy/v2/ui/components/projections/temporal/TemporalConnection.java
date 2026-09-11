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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import org.apache.commons.lang3.StringUtils;


/**
 * A temporal arc between two rows of the General Temporal Projection.
 * <p>
 * A connection represents an enduring association from the FLEF protocol
 * ({@code RelationshipRecord} or {@code PlaceRelationshipRecord}) whose
 * lifespan is drawn on the temporal axis as a connector between the source
 * and target rows.
 * <p>
 * The direction is preserved: {@code sourceRow} is the FLEF subject and
 * {@code targetRow} is the FLEF target, so that asymmetric relationship
 * types ({@code biological_child}, {@code group_member}, {@code part_of},
 * {@code administrative_part_of}, …) are rendered with the correct
 * orientation. The {@code type} field is the symmetric participant pair,
 * used by the renderer to choose the visual style.
 * <p>
 * The record is immutable. {@code role} is normalized to an empty string
 * when {@code null} is supplied.
 */
public record TemporalConnection(
	TemporalConnectionType type,
	TemporalEntityRef sourceRow,
	TemporalEntityRef targetRow,
	String relationshipType,
	TemporalSpan span,
	FLEFRecord sourceRecord,
	String role
){

	/**
	 * Compact constructor with validation and normalization.
	 */
	public TemporalConnection{
		if(type == null)
			throw new IllegalArgumentException("Connection type must not be null");
		if(sourceRow == null)
			throw new IllegalArgumentException("Source row must not be null");
		if(targetRow == null)
			throw new IllegalArgumentException("Target row must not be null");
		if(!sourceRow.isRowEntity())
			throw new IllegalArgumentException("Source must be a row entity, got: " + sourceRow.type());
		if(!targetRow.isRowEntity())
			throw new IllegalArgumentException("Target must be a row entity, got: " + targetRow.type());
		if(span == null)
			throw new IllegalArgumentException("Span must not be null");
		if(relationshipType == null || relationshipType.isBlank())
			throw new IllegalArgumentException("Relationship type must not be null or blank");
		if(role == null)
			role = StringUtils.EMPTY;

		// The declared type must be consistent with the participant kinds.
		final TemporalConnectionType expected = TemporalConnectionType.of(sourceRow.type(), targetRow.type());
		if(type != expected)
			throw new IllegalArgumentException("Declared type " + type + " does not match participant kinds ("
				+ sourceRow.type() + ", " + targetRow.type() + "), expected " + expected);
	}


	/**
	 * Returns whether the connection is open‑ended, i.e. its span has no
	 * upper bound. Such connections are rendered as half‑open arcs on the
	 * temporal axis.
	 *
	 * @return {@code true} if the span is open‑ended
	 */
	public boolean isOpenEnded(){
		return span.isOpenEnded();
	}

	/**
	 * Returns whether the connection has a role (e.g. {@code "president"},
	 * {@code "witness"}).
	 *
	 * @return {@code true} if the role is non‑empty
	 */
	public boolean hasRole(){
		return !role.isEmpty();
	}

	/**
	 * Returns whether the given entity participates in this connection,
	 * either as source or as target.
	 *
	 * @param entity the entity to test (must not be {@code null})
	 * @return {@code true} if the entity is one of the two endpoints
	 */
	public boolean involves(final TemporalEntityRef entity){
		return (sourceRow.equals(entity) || targetRow.equals(entity));
	}

	@Override
	public String toString(){
		return relationshipType + " (" + sourceRow.id() + " -> " + targetRow.id() + ") " + span;
	}

}
