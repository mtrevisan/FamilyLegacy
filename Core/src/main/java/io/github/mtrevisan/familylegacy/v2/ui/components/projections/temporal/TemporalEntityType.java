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
 * Kinds of FLEF entities that can appear in the General Temporal Projection.
 * <p>
 * The projection draws two families of entities:
 * <ul>
 *   <li><b>row entities</b> — entities that own a horizontal lifeline in the projection
 *       (individuals, groups, places). Each row carries a set of temporal tracks.</li>
 *   <li><b>context entities</b> — entities that do not own a row but that may appear as
 *       background bands or contextual overlays (historic events, cultural norms).</li>
 * </ul>
 * The two families are disjoint and are queried through {@link #isRowEntity()} and
 * {@link #isContextEntity()}.
 */
public enum TemporalEntityType{

	/** A FLEF {@code IndividualRecord}. Row entity. */
	INDIVIDUAL,

	/** A FLEF {@code GroupRecord}. Row entity. */
	GROUP,

	/** A FLEF {@code PlaceRecord}. Row entity. */
	PLACE,

	/** A FLEF {@code HistoricEventRecord}. Context entity. */
	HISTORIC_EVENT,

	/** A FLEF {@code CulturalNormRecord}. Context entity. */
	CULTURAL_NORM;


	/**
	 * Returns whether this entity type can own a row in the projection.
	 *
	 * @return {@code true} for {@link #INDIVIDUAL}, {@link #GROUP}, {@link #PLACE};
	 * {@code false} for context entities
	 */
	public boolean isRowEntity(){
		return (this == INDIVIDUAL || this == GROUP || this == PLACE);
	}

	/**
	 * Returns whether this entity type can appear as a background band or
	 * contextual overlay rather than as a row.
	 *
	 * @return {@code true} for {@link #HISTORIC_EVENT}, {@link #CULTURAL_NORM};
	 * {@code false} for row entities
	 */
	public boolean isContextEntity(){
		return (this == HISTORIC_EVENT || this == CULTURAL_NORM);
	}

}
