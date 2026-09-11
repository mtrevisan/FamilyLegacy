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

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * The horizontal time axis of the General Temporal Projection.
 * <p>
 * The axis owns:
 * <ul>
 *   <li>a fixed <b>domain</b> — the overall temporal extent of the projection,
 *       computed from the model and immutable;</li>
 *   <li>a mutable <b>visible window</b> — the sub‑interval currently displayed,
 *       changed by panning and zooming;</li>
 *   <li>a mutable <b>viewport width</b> — the pixel width allocated to the axis
 *       by the panel, updated on resize;</li>
 *   <li>a mutable <b>zoom level</b> — controls the granularity of the ticks.</li>
 * </ul>
 * The axis exposes coordinate transforms between JDN and pixels, span‑to‑rectangle
 * conversion, and tick generation. It does not draw anything: rendering is the
 * responsibility of {@code TemporalAxisRenderer}.
 * <p>
 * Instances are mutable and not thread‑safe; all access must occur on the Swing
 * Event Dispatch Thread.
 */
public final class TemporalAxis{

	private static final int MAX_TICKS = 200;
	private static final long JDN_YEAR_2000 = 2451545L;
	private static final double APPROX_DAYS_PER_YEAR = 365.2425;
	private static final String[] MONTH_NAMES = {
		"Jan", "Feb", "Mar", "Apr", "May", "Jun",
		"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
	};


	private final boolean empty;
	private final long domainStartJdn;
	private final long domainEndJdn;

	private long visibleStartJdn;
	private long visibleEndJdn;
	private int viewportWidth;


	/**
	 * Constructs an axis over the given normalized domain.
	 *
	 * @param domainStart the earliest point of the projection; may be {@code null}
	 * @param domainEnd   the latest point of the projection; may be {@code null}
	 */
	public TemporalAxis(final NormalizedDate domainStart, final NormalizedDate domainEnd){
		if(domainStart == null || domainEnd == null){
			empty = true;
			domainStartJdn = 0L;
			domainEndJdn = 0L;
		}
		else{
			empty = false;
			domainStartJdn = domainStart.jdn();
			domainEndJdn = Math.max(domainStart.jdn() + 1, domainEnd.jdn());
		}
		visibleStartJdn = domainStartJdn;
		visibleEndJdn = domainEndJdn;
		viewportWidth = 0;
	}

	/**
	 * Constructs an axis from a projection model, using the model's domain.
	 *
	 * @param model the projection model (must not be {@code null})
	 */
	public TemporalAxis(final TemporalProjectionModel model){
		this(model.domainStart(), model.domainEnd());
	}


	/* ======================================================================
	 *                          Read‑only accessors
	 * ====================================================================== */

	public boolean isEmpty(){
		return empty;
	}

	public long domainStartJdn(){
		return domainStartJdn;
	}

	public long domainEndJdn(){
		return domainEndJdn;
	}

	public long visibleStartJdn(){
		return visibleStartJdn;
	}

	public long visibleEndJdn(){
		return visibleEndJdn;
	}

	/**
	 * Returns the effective zoom level, derived from the visible span. There
	 * is no explicit zoom state: the granularity always follows the data
	 * currently displayed.
	 */
	public TemporalZoomLevel zoomLevel(){
		return deriveZoomLevel();
	}

	/**
	 * Zooms in one step: the visible window is halved around its center.
	 * The tick granularity follows automatically via {@link #zoomLevel()}.
	 */
	public void zoomIn(){
		scaleVisibleWindow(0.5);
	}

	/**
	 * Zooms out one step: the visible window is doubled around its center.
	 * The tick granularity follows automatically via {@link #zoomLevel()}.
	 */
	public void zoomOut(){
		scaleVisibleWindow(2.);
	}

	/**
	 * Resets the visible window to the full domain.
	 */
	public void fitToDomain(){
		if(empty)
			return;
		visibleStartJdn = domainStartJdn;
		visibleEndJdn = domainEndJdn;
	}

	/**
	 * Derives a zoom level from the visible span, in days. Thresholds are
	 * calibrated so that the axis renders between 5 and 40 ticks at any level.
	 */
	private TemporalZoomLevel deriveZoomLevel(){
		final long span = Math.max(1L, visibleEndJdn - visibleStartJdn);
		if(span <= 90L)
			return TemporalZoomLevel.DAY;
		if(span <= 1_095L)
			return TemporalZoomLevel.MONTH;
		if(span <= 10_950L)
			return TemporalZoomLevel.YEAR;
		if(span <= 109_500L)
			return TemporalZoomLevel.DECADE;
		if(span <= 1_095_000L)
			return TemporalZoomLevel.CENTURY;
		return TemporalZoomLevel.MILLENNIUM;
	}

	public int viewportWidth(){
		return viewportWidth;
	}


	/* ======================================================================
	 *                          Viewport management
	 * ====================================================================== */

	/**
	 * Sets the pixel width allocated to the axis. Called by the panel on
	 * resize and during initial layout.
	 *
	 * @param width the width in pixels; values below 1 are clamped to 1
	 */
	public void setViewportWidth(final int width){
		viewportWidth = Math.max(1, width);
	}

	/**
	 * Sets the visible window directly. Bounds are normalized so that
	 * {@code start < end}; the window is not clamped to the domain.
	 *
	 * @param startJdn the new visible start (inclusive)
	 * @param endJdn   the new visible end (inclusive)
	 */
	public void setVisibleRange(final long startJdn, final long endJdn){
		if(endJdn <= startJdn)
			throw new IllegalArgumentException("endJdn must be strictly greater than startJdn");
		visibleStartJdn = startJdn;
		visibleEndJdn = endJdn;
	}

	/**
	 * Translates the visible window by the given amount, without changing its
	 * width.
	 *
	 * @param deltaJdn the offset in days
	 */
	public void pan(final long deltaJdn){
		if(empty)
			return;
		final long domainSpan = domainEndJdn - domainStartJdn;
		final long windowSpan = visibleEndJdn - visibleStartJdn;
		final long minStart = domainStartJdn - windowSpan;
		final long maxStart = domainEndJdn + windowSpan - windowSpan;
		long newStart = visibleStartJdn + deltaJdn;
		newStart = Math.max(minStart, Math.min(maxStart, newStart));
		visibleStartJdn = newStart;
		visibleEndJdn = newStart + windowSpan;
	}

	private void scaleVisibleWindow(final double factor){
		if(empty)
			return;
		final long center = (visibleStartJdn + visibleEndJdn) / 2L;
		final long halfSpan = Math.max(1L, (long)((visibleEndJdn - visibleStartJdn) * factor / 2.));
		visibleStartJdn = center - halfSpan;
		visibleEndJdn = center + halfSpan;
	}


	/* ======================================================================
	 *                          Coordinate transforms
	 * ====================================================================== */

	/**
	 * Converts a Julian Day Number to an X pixel coordinate relative to the
	 * left edge of the axis. Values outside the visible window produce
	 * out‑of‑range coordinates that callers are expected to clip.
	 *
	 * @param jdn the Julian Day Number
	 * @return the X coordinate in pixels
	 */
	public int jdnToX(final long jdn){
		final long span = visibleEndJdn - visibleStartJdn;
		if(span <= 0L || viewportWidth <= 0)
			return 0;
		final double ratio = (double)(jdn - visibleStartJdn) / (double)span;
		return (int)Math.round(ratio * viewportWidth);
	}

	/**
	 * Converts an X pixel coordinate to a Julian Day Number. The inverse of
	 * {@link #jdnToX(long)} modulo rounding.
	 *
	 * @param x the X coordinate in pixels
	 * @return the Julian Day Number at that pixel
	 */
	public long xToJdn(final int x){
		final long span = visibleEndJdn - visibleStartJdn;
		if(span <= 0L || viewportWidth <= 0)
			return visibleStartJdn;
		final double ratio = (double)x / (double)viewportWidth;
		return visibleStartJdn + Math.round(ratio * span);
	}

	/**
	 * Converts a temporal span into a rectangle on the axis, given a vertical
	 * position and height. Open bounds are extended to the edges of the
	 * visible window; the result is clipped to {@code [0, viewportWidth]}.
	 *
	 * @param span   the span to convert (must not be {@code null})
	 * @param y      the vertical position of the rectangle
	 * @param height the height of the rectangle; values below 1 are clamped to 1
	 * @return the rectangle, or {@code null} if the span lies entirely outside
	 * the visible window
	 */
	public Rectangle spanToRect(final TemporalSpan span, final int y, final int height){
		if(span == null)
			return null;

		long startJdn;
		long endJdn;
		if(span.isPoint()){
			startJdn = span.start().jdn();
			endJdn = startJdn + span.start().precisionSpanInDays() - 1L;
		}
		else{
			startJdn = (span.start() != null? span.start().jdn(): Long.MIN_VALUE);
			endJdn = (span.end() != null? span.end().jdn(): Long.MAX_VALUE);
		}

		if(startJdn == Long.MIN_VALUE)
			startJdn = visibleStartJdn;
		if(endJdn == Long.MAX_VALUE)
			endJdn = visibleEndJdn;

		final int x1 = jdnToX(startJdn);
		final int x2 = jdnToX(endJdn);

		final int clampedX1 = Math.max(0, x1);
		final int clampedX2 = Math.min(viewportWidth, Math.max(clampedX1 + 1, x2));
		if(clampedX2 <= 0 || clampedX1 >= viewportWidth)
			return null;

		final int width = Math.max(1, clampedX2 - clampedX1);
		final int h = Math.max(1, height);
		return new Rectangle(clampedX1, y, width, h);
	}


	/* ======================================================================
	 *                          Tick generation
	 * ====================================================================== */

	/**
	 * A tick mark on the temporal axis.
	 *
	 * @param jdn   the Julian Day Number of the tick
	 * @param x     the X pixel coordinate of the tick
	 * @param label the display label (e.g. {@code "1750"}, {@code "Jan 1750"})
	 * @param major whether the tick should be drawn more prominently
	 */
	public record Tick(long jdn, int x, String label, boolean major){
	}


	/**
	 * Computes the tick list for the current zoom level and visible window.
	 * The result is never {@code null}; it may be empty when the axis is
	 * empty or the viewport has no width.
	 *
	 * @return an immutable list of ticks ordered by JDN
	 */
	public List<Tick> computeTicks(){
		if(empty || viewportWidth <= 0)
			return List.of();

		final List<Tick> ticks = new ArrayList<>();
		switch(zoomLevel()){
			case MILLENNIUM -> addYearTicks(ticks, 1000);
			case CENTURY -> addYearTicks(ticks, 100);
			case DECADE -> addYearTicks(ticks, 10);
			case YEAR -> addYearTicks(ticks, 1);
			case MONTH -> addMonthTicks(ticks);
			case DAY -> addDayTicks(ticks);
		}
		return Collections.unmodifiableList(ticks);
	}

	private void addYearTicks(final List<Tick> ticks, final int intervalYears){
		final int startYear = jdnToGregorianYear(visibleStartJdn);
		final int endYear = jdnToGregorianYear(visibleEndJdn);

		// Round down to the nearest multiple of the interval (handles negative years).
		final int alignedStart = Math.floorDiv(startYear, intervalYears) * intervalYears;

		int count = 0;
		for(int year = alignedStart; year <= endYear + intervalYears && count < MAX_TICKS; year += intervalYears){
			final long tickJdn = CalendarConverter.gregorianToJdn(year, 1, 1);
			if(tickJdn < visibleStartJdn)
				continue;
			if(tickJdn > visibleEndJdn)
				break;
			ticks.add(new Tick(tickJdn, jdnToX(tickJdn), formatYear(year), true));
			count++;
		}
	}

	private void addMonthTicks(final List<Tick> ticks){
		final long spanDays = visibleEndJdn - visibleStartJdn;
		int intervalMonths = 1;
		// Guard against overflow and against a span that overflows the
		// interval computation. 200 ticks are enough for any zoom level.
		while(intervalMonths < 1_000_000 && spanDays / 30 / intervalMonths > MAX_TICKS)
			intervalMonths *= 2;

		final int[] gregorian = jdnToGregorian(visibleStartJdn);
		int year = gregorian[0];
		int month = gregorian[1];
		// Defensive: never start from an out-of-range month.
		if(month < 1 || month > 12)
			month = 1;

		int count = 0;
		while(count < MAX_TICKS){
			if(month < 1 || month > 12)
				break;

			final long tickJdn = CalendarConverter.gregorianToJdn(year, month, 1);
			if(tickJdn > visibleEndJdn)
				break;
			if(tickJdn >= visibleStartJdn){
				ticks.add(new Tick(tickJdn, jdnToX(tickJdn), formatMonth(year, month), true));
				count ++;
			}
			month += intervalMonths;
			while(month > 12){
				month -= 12;
				year ++;
			}
		}
	}

	private void addDayTicks(final List<Tick> ticks){
		final long span = visibleEndJdn - visibleStartJdn;
		if(span <= 0L)
			return;

		// Cast safely: clamp the interval so it cannot overflow to 0 or a
		// negative value if the visible span degenerates.
		final long rawInterval = Math.max(1L, span / MAX_TICKS);
		final int intervalDays = niceDayInterval((rawInterval > Integer.MAX_VALUE
			? Integer.MAX_VALUE
			: (int)rawInterval));

		long start = visibleStartJdn;
		if(intervalDays >= 7L)
			start -= Math.floorMod(start, 7L);

		int count = 0;
		for(long j = start; j <= visibleEndJdn && count < MAX_TICKS; j += intervalDays){
			if(j < visibleStartJdn)
				continue;

			final int[] gregorian = jdnToGregorian(j);
			// Defensive: skip ticks whose reverse Gregorian conversion falls
			// outside the valid month range.
			if(gregorian[1] < 1 || gregorian[1] > 12)
				continue;

			ticks.add(new Tick(j, jdnToX(j), formatDay(gregorian), true));
			count ++;
		}
	}

	private static int niceDayInterval(final int raw){
		if(raw <= 1)
			return 1;
		if(raw <= 2)
			return 2;
		if(raw <= 5)
			return 5;
		if(raw <= 7)
			return 7;
		if(raw <= 14)
			return 14;
		if(raw <= 30)
			return 30;
		if(raw <= 90)
			return 90;
		if(raw <= 180)
			return 180;
		if(raw <= 365)
			return 365;
		return raw;
	}

	private static String formatYear(final int year){
		return Integer.toString(year);
	}

	/**
	 * Formats a year/month pair as a label. If the month is outside the valid
	 * 1..12 range (which can happen only if the axis is fed an out-of-range
	 * Julian Day Number), the year alone is returned.
	 */
	private static String formatMonth(final int year, final int month){
		if(month < 1 || month > 12)
			return Integer.toString(year);
		return MONTH_NAMES[month - 1] + StringUtils.SPACE + year;
	}

	/**
	 * Formats a {@code [year, month, day]} triple as a day label. If the month
	 * is outside the valid 1..12 range, the year alone is returned.
	 */
	private static String formatDay(final int[] gregorian){
		final int month = gregorian[1];
		if(month < 1 || month > 12)
			return Integer.toString(gregorian[0]);
		return gregorian[2] + StringUtils.SPACE + MONTH_NAMES[month - 1];
	}


	/* ======================================================================
	 *                          Calendar helpers
	 * ====================================================================== */

	/**
	 * Reverse Gregorian conversion (Fliegel–Van Flandern), returning the
	 * proleptic Gregorian year of the given Julian Day Number.
	 */
	private static int jdnToGregorianYear(final long jdn){
		return jdnToGregorian(jdn)[0];
	}

	/**
	 * Reverse Gregorian conversion (Fliegel–Van Flandern), returning
	 * {@code [year, month, day]}.
	 */
	private static int[] jdnToGregorian(final long jdn){
		final long a = jdn + 32044L;
		final long b = (4L * a + 3L) / 146097L;
		final long c = a - (146097L * b) / 4L;
		final long d = (4L * c + 3L) / 1461L;
		final long e = c - (1461L * d) / 4L;
		final long m = (5L * e + 2L) / 153L;
		final int day = (int)(e - (153L * m + 2L) / 5L + 1L);
		final int month = (int)(m + 3L - 12L * (m / 10L));
		final int year = (int)(100L * b + d - 4800L + m / 10L);
		return new int[]{year, month, day};
	}

}
