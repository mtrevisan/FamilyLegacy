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
 * A background band in the General Temporal Projection.
 * <p>
 * A band is a temporal interval extracted from a context entity
 * ({@code HistoricEventRecord} or {@code CulturalNormRecord}) that is
 * rendered as a shaded region behind the row lifelines, to give historical,
 * cultural or legal context to the events and relationships drawn on top.
 * <p>
 * Bands do not participate in relationship arcs: they are strictly
 * background. The {@code kind} field mirrors the protocol type
 * (e.g. {@code "war"}, {@code "epidemic"}, {@code "marriage_practice"}).
 * The {@code place} field, when present, restricts the geographic scope of
 * the band and may be used by the renderer to apply a distinct style or to
 * hide the band when the projection is filtered by place.
 * <p>
 * The record is immutable. {@code label} and {@code kind} are normalized to
 * empty strings when {@code null} is supplied.
 */
public record TemporalContextBand(
	TemporalEntityRef entity,
	TemporalSpan span,
	String label,
	String kind,
	FLEFRecord place
){

	/**
	 * Compact constructor with validation.
	 */
	public TemporalContextBand{
		if(entity == null)
			throw new IllegalArgumentException("Band entity must not be null");
		if(!entity.isContextEntity())
			throw new IllegalArgumentException("A band can only be anchored to a context entity, got: " + entity.type());
		if(span == null)
			throw new IllegalArgumentException("Span must not be null");
		if(label == null)
			label = StringUtils.EMPTY;
		if(kind == null)
			kind = StringUtils.EMPTY;
	}


	/**
	 * Returns whether this band is geographically scoped.
	 *
	 * @return {@code true} if a place record is attached
	 */
	public boolean hasPlace(){
		return (place != null);
	}

	/**
	 * Returns whether the band covers the given Julian Day Number.
	 *
	 * @param jdn the Julian Day Number to test
	 * @return {@code true} if the JDN is inside the band's span
	 */
	public boolean covers(final long jdn){
		return span.contains(jdn);
	}

	@Override
	public String toString(){
		return (label.isEmpty()? kind: label) + StringUtils.SPACE + span;
	}

}
