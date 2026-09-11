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
 * A single temporal assertion on a track of the General Temporal Projection.
 * <p>
 * An entry corresponds to one FLEF record that carries a date and belongs to
 * the row entity. Typical origins:
 * <ul>
 *   <li>an {@code EventRecord} in which the row entity participates (through
 *       an {@code EventParticipationRecord});</li>
 *   <li>an {@code IndividualAttributeRecord} or {@code GroupAttributeRecord}
 *       owned by the row entity and bounded by {@code valid_from} / {@code valid_to};</li>
 *   <li>a {@code ContextImpactRecord} whose target is the row entity or one
 *       of its entries (context track).</li>
 * </ul>
 * The {@code sourceRecord} field always points back to the originating FLEF
 * record so that the interaction handler can open the original data. The
 * {@code evidence} field carries the raw evidence qualifiers structure when
 * the entry has been assessed; it may be {@code null}.
 * <p>
 * The record is immutable. {@code label}, {@code type} and {@code role} are
 * normalized to empty strings when {@code null} is supplied.
 */
public record TemporalEntry(
	TemporalSpan span,
	String label,
	String type,
	String role,
	FLEFRecord sourceRecord,
	FLEFRecord evidence
){

	/**
	 * Compact constructor with normalization and validation.
	 */
	public TemporalEntry{
		if(span == null)
			throw new IllegalArgumentException("Span must not be null");
		if(label == null)
			label = StringUtils.EMPTY;
		if(type == null)
			type = StringUtils.EMPTY;
		if(role == null)
			role = StringUtils.EMPTY;
	}


	/**
	 * Returns whether this entry carries the given protocol type.
	 *
	 * @param candidate the protocol type to test (e.g. {@code "birth"},
	 *                  {@code "occupation"}); comparison is case‑insensitive
	 * @return {@code true} if the entry's type matches
	 */
	public boolean hasType(final String candidate){
		return (candidate != null && type.equalsIgnoreCase(candidate));
	}

	/**
	 * Returns whether this entry has a role (e.g. {@code "witness"},
	 * {@code "officiant"}).
	 *
	 * @return {@code true} if the role is non‑empty
	 */
	public boolean hasRole(){
		return !role.isEmpty();
	}

	/**
	 * Returns whether an evidence qualifier is attached to this entry.
	 *
	 * @return {@code true} if the evidence record is non‑null
	 */
	public boolean hasEvidence(){
		return (evidence != null);
	}

	@Override
	public String toString(){
		return (label.isEmpty()? type: label) + StringUtils.SPACE + span;
	}

}
