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

import org.apache.commons.lang3.StringUtils;


/**
 * A link between a {@link TemporalContextBand} and a target element of the
 * projection (row, entry or connection).
 * <p>
 * Mirrors the FLEF {@code ContextImpactRecord}: it makes explicit which
 * contextual factor is believed to influence, explain, constrain, motivate
 * or cause a specific genealogical element. The renderer uses these links to
 * draw a faint connector from the band to the affected element, and the
 * interaction handler uses them to show the rationale and the impact type on
 * hover.
 * <p>
 * The {@code impactType} field mirrors the FLEF enum
 * ({@code explains}, {@code influences}, {@code constrains}, {@code motivates},
 * {@code causes}); the {@code rationale} field carries the optional
 * human‑readable explanation.
 * <p>
 * The record is immutable. {@code impactType} and {@code rationale} are
 * normalized to empty strings when {@code null} is supplied.
 */
public record ContextImpactLink(
	TemporalContextBand band,
	TemporalProjectionRef targetRef,
	String impactType,
	String rationale
){

	/** The FLEF {@code ContextImpactRecord.impact_type} values. */
	public static final String IMPACT_EXPLAINS = "explains";
	public static final String IMPACT_INFLUENCES = "influences";
	public static final String IMPACT_CONSTRAINS = "constrains";
	public static final String IMPACT_MOTIVATES = "motivates";
	public static final String IMPACT_CAUSES = "causes";


	/**
	 * Compact constructor with validation and normalization.
	 */
	public ContextImpactLink{
		if(band == null)
			throw new IllegalArgumentException("Band must not be null");
		if(targetRef == null)
			throw new IllegalArgumentException("Target reference must not be null");
		if(impactType == null)
			impactType = StringUtils.EMPTY;
		if(rationale == null)
			rationale = StringUtils.EMPTY;
	}


	/**
	 * Returns whether this link carries a recognized FLEF impact type.
	 *
	 * @return {@code true} if the impact type is one of the five standard values
	 */
	public boolean hasStandardImpactType(){
		return (IMPACT_EXPLAINS.equals(impactType)
			|| IMPACT_INFLUENCES.equals(impactType)
			|| IMPACT_CONSTRAINS.equals(impactType)
			|| IMPACT_MOTIVATES.equals(impactType)
			|| IMPACT_CAUSES.equals(impactType));
	}

	/**
	 * Returns whether the rationale text is present.
	 *
	 * @return {@code true} if the rationale is non‑empty
	 */
	public boolean hasRationale(){
		return !rationale.isEmpty();
	}

	@Override
	public String toString(){
		return impactType + ": " + band.label() + " -> " + targetRef.anchorEntity();
	}

}
