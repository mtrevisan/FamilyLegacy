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
 * Categories of temporal track that can be attached to a row in the
 * General Temporal Projection.
 * <p>
 * A track groups entries that share the same origin in the FLEF protocol:
 * <ul>
 *   <li>{@link #EVENT} — {@code EventRecord} instances in which the row
 *       entity participates through {@code EventParticipationRecord}.</li>
 *   <li>{@link #ATTRIBUTE} — {@code IndividualAttributeRecord} and
 *       {@code GroupAttributeRecord} instances owned by the row entity.</li>
 *   <li>{@link #CONTEXT} — {@code ContextImpactRecord} instances whose
 *       target is the row entity (or one of its entries).</li>
 * </ul>
 * Tracks are ordered on the row by {@link #getSortOrder()}.
 */
public enum TemporalTrackType{

	/** Events the row entity participated in. */
	EVENT(0),

	/** Enduring attributes owned by the row entity. */
	ATTRIBUTE(1),

	/** Contextual influences affecting the row entity. */
	CONTEXT(2);


	private final int sortOrder;


	TemporalTrackType(final int sortOrder){
		this.sortOrder = sortOrder;
	}


	/**
	 * Returns the display order of this track within a row. Lower values are
	 * rendered first (top‑most inside the row).
	 *
	 * @return the sort order, non‑negative
	 */
	public int getSortOrder(){
		return sortOrder;
	}

}
