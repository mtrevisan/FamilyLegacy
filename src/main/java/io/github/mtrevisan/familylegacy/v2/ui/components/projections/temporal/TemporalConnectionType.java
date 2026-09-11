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

/**
 * Nature of a temporal connection (arc) between two rows of the
 * General Temporal Projection.
 * <p>
 * The type is <i>symmetric</i>: it describes the pair of participant
 * kinds involved, not the direction of the underlying FLEF relationship.
 * The direction is preserved separately in the {@code TemporalConnection}
 * record through its source and target rows.
 * <p>
 * The four constants correspond to the participant pairs allowed by the
 * FLEF {@code RelationshipRecord} and {@code PlaceRelationshipRecord}
 * types:
 * <ul>
 *   <li>individual ↔ individual — child, spouse, associate</li>
 *   <li>individual ↔ group — {@code group_member}, {@code associate}</li>
 *   <li>group ↔ group — {@code part_of}, {@code associate}</li>
 *   <li>place ↔ place — {@code administrative_part_of}, etc.</li>
 * </ul>
 */
public enum TemporalConnectionType{

	/** Both endpoints are individuals. */
	INDIVIDUAL_INDIVIDUAL,

	/** One endpoint is an individual, the other is a group. */
	INDIVIDUAL_GROUP,

	/** Both endpoints are groups. */
	GROUP_GROUP,

	/** Both endpoints are places. */
	PLACE_PLACE;


	/**
	 * Resolves the connection type from the two endpoint entity types.
	 * <p>
	 * The resolution is symmetric: {@code of(A, B)} and {@code of(B, A)}
	 * return the same value. Only pairs allowed by the FLEF protocol are
	 * accepted.
	 *
	 * @param a the first endpoint type (must not be {@code null})
	 * @param b the second endpoint type (must not be {@code null})
	 * @return the matching connection type
	 * @throws IllegalArgumentException if the pair does not correspond to any
	 *                                  relationship allowed by the protocol, or if either endpoint is a
	 *                                  context entity
	 */
	public static TemporalConnectionType of(final TemporalEntityType a, final TemporalEntityType b){
		if(a == null || b == null)
			throw new IllegalArgumentException("Endpoint types must not be null");
		if(a.isContextEntity() || b.isContextEntity())
			throw new IllegalArgumentException("Context entities cannot participate in temporal connections: " + a + ", " + b);

		if(a == TemporalEntityType.INDIVIDUAL && b == TemporalEntityType.INDIVIDUAL)
			return INDIVIDUAL_INDIVIDUAL;
		if(a == TemporalEntityType.GROUP && b == TemporalEntityType.GROUP)
			return GROUP_GROUP;
		if(a == TemporalEntityType.PLACE && b == TemporalEntityType.PLACE)
			return PLACE_PLACE;
		if((a == TemporalEntityType.INDIVIDUAL && b == TemporalEntityType.GROUP)
			|| (a == TemporalEntityType.GROUP && b == TemporalEntityType.INDIVIDUAL))
			return INDIVIDUAL_GROUP;

		throw new IllegalArgumentException("Unsupported participant pair: " + a + ", " + b);
	}

	/**
	 * Returns whether this connection type involves at least one individual.
	 *
	 * @return {@code true} for {@link #INDIVIDUAL_INDIVIDUAL} and {@link #INDIVIDUAL_GROUP}
	 */
	public boolean involvesIndividual(){
		return (this == INDIVIDUAL_INDIVIDUAL || this == INDIVIDUAL_GROUP);
	}

	/**
	 * Returns whether this connection type involves at least one group.
	 *
	 * @return {@code true} for {@link #GROUP_GROUP} and {@link #INDIVIDUAL_GROUP}
	 */
	public boolean involvesGroup(){
		return (this == GROUP_GROUP || this == INDIVIDUAL_GROUP);
	}

}
