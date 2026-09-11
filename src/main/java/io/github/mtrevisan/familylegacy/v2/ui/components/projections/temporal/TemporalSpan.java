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
 * The extension of time occupied by a single assertion in the General
 * Temporal Projection.
 * <p>
 * Mirrors the three alternatives of the FLEF {@code DateValue} union:
 * <ul>
 *   <li>{@link TemporalSpanKind#POINT} — a single point; {@code start} is
 *       required, {@code end} must be {@code null}. Reduced‑precision points
 *       (decade, century) are still {@code POINT} but are rendered as shaded
 *       bars.</li>
 *   <li>{@link TemporalSpanKind#BOUNDED} — the date is unknown but is known
 *       to fall within an interval. At least one of {@code start} (not before)
 *       or {@code end} (not after) must be non‑null. The interval expresses
 *       <i>uncertainty</i>, not duration.</li>
 *   <li>{@link TemporalSpanKind#SPANNING} — the fact itself extends across
 *       the interval. At least one of {@code start} (from) or {@code end} (to)
 *       must be non‑null. The interval expresses <i>actual duration</i>.</li>
 * </ul>
 * The {@code status} field is independent of the dates and mirrors the FLEF
 * relationship status: {@link #STATUS_ACTIVE}, {@link #STATUS_ENDED},
 * {@link #STATUS_UNKNOWN}. It is meaningful for relationships and is
 * normally {@link #STATUS_UNKNOWN} for events and attributes.
 */
public record TemporalSpan(TemporalSpanKind kind, NormalizedDate start, NormalizedDate end, String status){

	public static final String STATUS_ACTIVE = "active";
	public static final String STATUS_ENDED = "ended";
	public static final String STATUS_UNKNOWN = "unknown";


	/**
	 * Compact constructor with validation.
	 */
	public TemporalSpan{
		if(kind == null)
			throw new IllegalArgumentException("Span kind must not be null");
		if(status == null || status.isBlank())
			throw new IllegalArgumentException("Status must not be null or blank");
		if(!STATUS_ACTIVE.equals(status) && !STATUS_ENDED.equals(status) && !STATUS_UNKNOWN.equals(status))
			throw new IllegalArgumentException("Unknown status: " + status);

		switch(kind){
			case POINT -> {
				if(start == null)
					throw new IllegalArgumentException("A POINT span requires a non-null start");
				if(end != null)
					throw new IllegalArgumentException("A POINT span must not have an end");
			}
			case BOUNDED, SPANNING -> {
				if(start == null && end == null)
					throw new IllegalArgumentException("A " + kind + " span requires at least one of start or end");
				if(start != null && end != null && start.compareTo(end) > 0)
					throw new IllegalArgumentException("Start (" + start.jdn() + ") must not be greater than end ("
						+ end.jdn() + ")");
			}
		}
	}


	/**
	 * Creates a {@link TemporalSpanKind#POINT} span.
	 *
	 * @param date   the single point in time (must not be {@code null})
	 * @param status the relationship status
	 * @return a point span
	 */
	public static TemporalSpan point(final NormalizedDate date, final String status){
		return new TemporalSpan(TemporalSpanKind.POINT, date, null, status);
	}

	/**
	 * Creates a {@link TemporalSpanKind#POINT} span with
	 * {@link #STATUS_UNKNOWN}.
	 *
	 * @param date the single point in time
	 * @return a point span
	 */
	public static TemporalSpan point(final NormalizedDate date){
		return point(date, STATUS_UNKNOWN);
	}

	/**
	 * Creates a {@link TemporalSpanKind#BOUNDED} span. At least one bound must
	 * be non‑null; a null bound represents an open‑ended limit.
	 *
	 * @param notBefore the lower bound; may be {@code null}
	 * @param notAfter  the upper bound; may be {@code null}
	 * @param status    the relationship status
	 * @return a bounded span
	 */
	public static TemporalSpan bounded(final NormalizedDate notBefore, final NormalizedDate notAfter,
		final String status){
		return new TemporalSpan(TemporalSpanKind.BOUNDED, notBefore, notAfter, status);
	}

	/**
	 * Creates a {@link TemporalSpanKind#BOUNDED} span with
	 * {@link #STATUS_UNKNOWN}.
	 */
	public static TemporalSpan bounded(final NormalizedDate notBefore, final NormalizedDate notAfter){
		return bounded(notBefore, notAfter, STATUS_UNKNOWN);
	}

	/**
	 * Creates a {@link TemporalSpanKind#SPANNING} span. At least one bound
	 * must be non‑null; a null bound represents an open‑ended limit.
	 *
	 * @param from   the start of the interval; may be {@code null}
	 * @param to     the end of the interval; may be {@code null}
	 * @param status the relationship status
	 * @return a spanning span
	 */
	public static TemporalSpan spanning(final NormalizedDate from, final NormalizedDate to, final String status){
		return new TemporalSpan(TemporalSpanKind.SPANNING, from, to, status);
	}

	/**
	 * Creates a {@link TemporalSpanKind#SPANNING} span with
	 * {@link #STATUS_UNKNOWN}.
	 */
	public static TemporalSpan spanning(final NormalizedDate from, final NormalizedDate to){
		return spanning(from, to, STATUS_UNKNOWN);
	}

	/**
	 * Creates an unbounded span covering the entire temporal domain. Used by
	 * views that must represent an enduring relationship or attribute whose
	 * validity is unknown, without dropping it from the view.
	 * <p>
	 * The span is of kind {@link TemporalSpanKind#SPANNING} with both bounds
	 * set to the extreme values {@link Long#MIN_VALUE} and
	 * {@link Long#MAX_VALUE}. The temporal axis and the renderers already
	 * handle these sentinel values as "extend to the visible edge", so an
	 * unbounded span is drawn as a bar that reaches both edges of the current
	 * viewport.
	 * <p>
	 * The {@link #contains(long)} method always returns {@code true}, and any
	 * temporal window in {@code SocialFilters} accepts the span because its
	 * {@link #minJdn()} and {@link #maxJdn()} cover the whole numeric range.
	 *
	 * @param status the relationship status
	 * @return an unbounded span
	 */
	public static TemporalSpan unbounded(final String status){
		return new TemporalSpan(TemporalSpanKind.SPANNING,
			NormalizedDate.exact(Long.MIN_VALUE, DatePrecision.YEAR),
			NormalizedDate.exact(Long.MAX_VALUE, DatePrecision.YEAR),
			status);
	}

	/**
	 * Creates an unbounded span with {@link #STATUS_UNKNOWN}.
	 *
	 * @return an unbounded span with unknown status
	 */
	public static TemporalSpan unbounded(){
		return unbounded(STATUS_UNKNOWN);
	}


	/**
	 * Returns whether this span represents a single point in time.
	 *
	 * @return {@code true} for {@link TemporalSpanKind#POINT}
	 */
	public boolean isPoint(){
		return (kind == TemporalSpanKind.POINT);
	}

	/**
	 * Returns whether this span has no lower bound. Only interval spans can
	 * be open‑started; point spans are never open.
	 *
	 * @return {@code true} if the span is an interval with a null start
	 */
	public boolean isOpenStarted(){
		return (!isPoint() && start == null);
	}

	/**
	 * Returns whether this span has no upper bound. Only interval spans can
	 * be open‑ended; point spans are never open.
	 *
	 * @return {@code true} if the span is an interval with a null end
	 */
	public boolean isOpenEnded(){
		return (!isPoint() && end == null);
	}

	/**
	 * Returns the effective end of this span. For a point span, the effective
	 * end is the start; for interval spans it is the {@code end} field, which
	 * may be {@code null} (open‑ended).
	 *
	 * @return the effective end, or {@code null} for an open‑ended interval
	 */
	public NormalizedDate effectiveEnd(){
		return (isPoint()? start: end);
	}

	/**
	 * Returns whether either bound of this span is approximate.
	 *
	 * @return {@code true} if at least one of the bounds is approximate
	 */
	public boolean isApproximate(){
		return (start != null && start.approximate()) || (end != null && end.approximate());
	}

	/**
	 * Returns the smallest JDN covered by this span, or {@link Long#MIN_VALUE}
	 * if the span is open‑started.
	 *
	 * @return the minimum JDN
	 */
	public long minJdn(){
		return (start != null? start.jdn(): Long.MIN_VALUE);
	}

	/**
	 * Returns the largest JDN covered by this span, or {@link Long#MAX_VALUE}
	 * if the span is open‑ended.
	 *
	 * @return the maximum JDN
	 */
	public long maxJdn(){
		if(isPoint())
			return start.jdn();
		return (end != null? end.jdn(): Long.MAX_VALUE);
	}

	/**
	 * Returns whether the given JDN falls within this span.
	 * <p>
	 * For a point span with reduced precision (decade, century), the JDN is
	 * considered to fall within the span if it lies anywhere inside the
	 * interval represented by that precision. For interval spans, open bounds
	 * are treated as ±infinity.
	 *
	 * @param jdn the Julian Day Number to test
	 * @return {@code true} if the JDN is contained in this span
	 */
	public boolean contains(final long jdn){
		if(isPoint()){
			final long startJdn = start.jdn();
			if(jdn < startJdn)
				return false;
			return (jdn - startJdn) < start.precisionSpanInDays();
		}
		if(start != null && jdn < start.jdn())
			return false;
		if(end != null && jdn > end.jdn())
			return false;
		return true;
	}


	@Override
	public String toString(){
		if(isPoint())
			return "POINT [" + status + "]: " + start;
		return kind + " [" + status + "]: " + (start != null? start: "(open)")
			+ " .. " + (end != null? end: "(open)");
	}

}
