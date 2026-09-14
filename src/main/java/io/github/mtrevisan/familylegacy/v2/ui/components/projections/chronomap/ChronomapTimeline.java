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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.chronomap;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.function.DoubleConsumer;


/**
 * Horizontal, zoomable timeline, without playback.
 * <p>
 * The domain is the global temporal range of the FLEF file, provided by
 * the enclosing panel. The visible window starts as the whole domain and
 * can be zoomed and panned by the user.
 * <ul>
 *   <li>Ctrl + wheel: zoom, anchored at the cursor;</li>
 *   <li>drag: pan the visible window;</li>
 *   <li>click on the axis: jump the playhead;</li>
 *   <li>double-click on the playhead: reset the zoom.</li>
 * </ul>
 */
public final class ChronomapTimeline extends JPanel{

	private static final Color BACKGROUND = new Color(240, 236, 228);
	private static final Color AXIS_LINE = new Color(120, 110, 95);
	private static final Color AXIS_LABEL = new Color(60, 55, 45);
	private static final Color PLAYHEAD = new Color(200, 60, 60);
	private static final Color RANGE_FILL = new Color(210, 200, 180);
	private static final int AXIS_HEIGHT = 44;
	private static final int PADDING = 12;
	private static final int DRAG_DEAD_ZONE_PX = 3;
	private static final double ZOOM_STEP = 1.3;
	private static final double PLAYHEAD_HIT_PX = 6.;

	private long domainMin = 0;
	private long domainMax = 1;
	private long visibleStart = 0;
	private long visibleEnd = 1;
	private double currentTime = 0;

	private Point dragAnchor;
	private boolean draggingPlayhead;

	private DoubleConsumer timeListener;


	public ChronomapTimeline(){
		setBackground(BACKGROUND);
		setOpaque(true);
		setPreferredSize(new Dimension(0, AXIS_HEIGHT));
		installListeners();
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	public void setDomain(final long minJdn, final long maxJdn){
		if(minJdn >= maxJdn)
			return;

		this.domainMin = minJdn;
		this.domainMax = maxJdn;
		this.visibleStart = minJdn;
		this.visibleEnd = maxJdn;
		this.currentTime = minJdn;

		repaint();
	}

	public void setCurrentTime(final double jdn){
		this.currentTime = Math.clamp(domainMax, domainMin, jdn);

		repaint();
	}

	public double getCurrentTime(){
		return currentTime;
	}

	public void withTimeListener(final DoubleConsumer listener){
		this.timeListener = listener;
	}

	public void resetZoom(){
		visibleStart = domainMin;
		visibleEnd = domainMax;

		repaint();
	}


	/* ======================================================================
	 *                          Painting
	 * ====================================================================== */

	@Override
	protected void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(!(g instanceof Graphics2D g2))
			return;

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		final int w = getWidth();
		final int h = getHeight();

		g2.setColor(BACKGROUND);
		g2.fillRect(0, 0, w, h);

		g2.setColor(RANGE_FILL);
		g2.fillRect(PADDING, h / 2 - 6, w - 2 * PADDING, 12);

		final int axisY = h / 2;
		g2.setColor(AXIS_LINE);
		g2.setStroke(new BasicStroke(1f));
		g2.drawLine(PADDING, axisY,
			w - PADDING, axisY);

		final long span = Math.max(1, visibleEnd - visibleStart);
		final long step = chooseStep(span, w);
		g2.setFont(getFont().deriveFont(10f));
		final FontMetrics fm = g2.getFontMetrics();
		for(long jdn = ((visibleStart / step) * step); jdn <= visibleEnd; jdn += step){
			final int x = jdnToX(jdn, w);
			if(x < PADDING || x > w - PADDING)
				continue;

			g2.setColor(AXIS_LINE);
			g2.drawLine(x, axisY - 4,
				x, axisY + 4);
			final String label = formatYear(jdn);
			final int tw = fm.stringWidth(label);
			g2.setColor(AXIS_LABEL);
			g2.drawString(label, x - tw / 2, axisY + 16);
		}

		final int phX = jdnToX((long)currentTime, w);
		g2.setColor(PLAYHEAD);
		g2.setStroke(new BasicStroke(2f));
		g2.drawLine(phX, 4,
			phX, h - 4);

		g2.setFont(getFont().deriveFont(10f).deriveFont(java.awt.Font.BOLD));
		final String timeLabel = formatYear((long)currentTime);
		final int tw = g2.getFontMetrics().stringWidth(timeLabel);
		g2.setColor(PLAYHEAD);
		g2.drawString(timeLabel, Math.max(PADDING, phX - tw / 2), 12);
	}


	/* ======================================================================
	 *                          Interaction
	 * ====================================================================== */

	private void installListeners(){
		final MouseAdapter adapter = new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent e){
				if(!SwingUtilities.isLeftMouseButton(e))
					return;

				draggingPlayhead = (Math.abs(e.getX() - jdnToX((long)currentTime, getWidth())) <= PLAYHEAD_HIT_PX);
				if(!draggingPlayhead){
					movePlayhead(e.getX());

					draggingPlayhead = true;
				}
				dragAnchor = e.getPoint();
			}

			@Override
			public void mouseDragged(final MouseEvent e){
				if(dragAnchor == null)
					return;

				final int dx = e.getX() - dragAnchor.x;
				if(Math.abs(dx) < DRAG_DEAD_ZONE_PX)
					return;

				if(draggingPlayhead)
					movePlayhead(e.getX());
				else
					pan(dx);
				dragAnchor = e.getPoint();
			}

			@Override
			public void mouseReleased(final MouseEvent e){
				dragAnchor = null;
				draggingPlayhead = false;
			}

			@Override
			public void mouseClicked(final MouseEvent e){
				if(!SwingUtilities.isLeftMouseButton(e))
					return;

				if(e.getClickCount() == 2 && Math.abs(e.getX() - jdnToX((long)currentTime, getWidth())) <= PLAYHEAD_HIT_PX)
					resetZoom();
			}
		};
		addMouseListener(adapter);
		addMouseMotionListener(adapter);

		addMouseWheelListener(this::onMouseWheel);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	private void onMouseWheel(final MouseWheelEvent e){
		if(e.isControlDown() || e.isMetaDown()){
			e.consume();
			final int rot = e.getWheelRotation();
			if(rot != 0)
				zoomAtCursor(rot < 0, e.getX());
		}
	}

	private void zoomAtCursor(final boolean zoomIn, final int cursorX){
		final int w = getWidth();
		if(w <= 2 * PADDING)
			return;

		final long span = visibleEnd - visibleStart;
		if(zoomIn && span <= 10)
			return;

		final long anchor = xToJdn(cursorX, w);
		final double ratio = (double)(anchor - visibleStart) / span;
		final long newSpan = Math.max(10, (long)(span * (zoomIn? 1. / ZOOM_STEP: ZOOM_STEP)));
		final long newStart = anchor - (long)(newSpan * ratio);
		visibleStart = Math.max(domainMin, newStart);
		visibleEnd = Math.min(domainMax, visibleStart + newSpan);
		visibleStart = Math.max(domainMin, visibleEnd - newSpan);

		repaint();
	}

	private void pan(final int dxPixels){
		final int w = getWidth();
		if(w <= 2 * PADDING)
			return;

		final long span = visibleEnd - visibleStart;
		final long delta = -(long)((double)dxPixels * span / (w - 2 * PADDING));
		if(delta == 0)
			return;

		visibleStart = Math.max(domainMin, visibleStart + delta);
		visibleEnd = Math.min(domainMax, visibleStart + span);
		visibleStart = Math.max(domainMin, visibleEnd - span);

		repaint();
	}

	private void movePlayhead(final int x){
		final int w = getWidth();
		if(w <= 2 * PADDING)
			return;

		final long jdn = xToJdn(x, w);
		currentTime = Math.clamp(domainMax, domainMin, jdn);

		repaint();

		if(timeListener != null)
			timeListener.accept(currentTime);
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private int jdnToX(final long jdn, final int width){
		final long span = Math.max(1, visibleEnd - visibleStart);
		final double f = (double)(jdn - visibleStart) / span;
		return (int)Math.round(PADDING + f * (width - 2 * PADDING));
	}

	private long xToJdn(final int x, final int width){
		final long span = Math.max(1, visibleEnd - visibleStart);
		final double f = (double)(x - PADDING) / (width - 2 * PADDING);
		return visibleStart + (long)(f * span);
	}

	private static long chooseStep(final long span, final int width){
		final int approxTicks = Math.max(2, width / 90);
		final long step = Math.max(1, span / approxTicks);
		long base = 1;
		while(base * 10 <= step)
			base *= 10;
		final long[] nice = {1, 2, 5, 10};
		for(final long n : nice)
			if(base * n >= step)
				return base * n;
		return base * 10;
	}

	private static String formatYear(final long jdn){
		return Integer.toString(jdnToGregorian(jdn)[0]);
	}

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
