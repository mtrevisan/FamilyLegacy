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
package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import io.github.mtrevisan.familylegacy.ui.tools.events.CalendarConverterDialog;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


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
	private static final double PLAYHEAD_HIT_PX = 8.;
	private static final double DAYS_PER_YEAR = 365.2425;
	private static final double DAYS_PER_MONTH = DAYS_PER_YEAR / 12.;

	private long domainMin = 0;
	private long domainMax = 1;
	private long visibleStart = 0;
	private long visibleEnd = 1;
	private double currentTime = 0;

	private Point dragAnchor;
	private boolean draggingPlayhead;
	private boolean wasDragged;

	private final List<Consumer<Double>> timeListeners = new ArrayList<>();


	public ChronomapTimeline(){
		setBackground(BACKGROUND);
		setOpaque(true);
		setPreferredSize(new Dimension(0, AXIS_HEIGHT));
		setFocusable(true);

		installListeners();
		configureKeyBindings();
		configureGlobalKeyDispatcher();
	}

	public void setDomain(final long minJdn, final long maxJdn){
		if(minJdn >= maxJdn)
			return;

		this.domainMin = minJdn;
		this.domainMax = maxJdn;
		this.visibleStart = minJdn;
		this.visibleEnd = maxJdn;
		this.currentTime = maxJdn;

		repaint();

		for(final Consumer<Double> listener : timeListeners)
			listener.accept(currentTime);
	}

	public void setCurrentTime(final double jdn){
		final double newTime = Math.clamp(jdn, domainMin, domainMax);
		if(Double.compare(newTime, currentTime) != 0){
			this.currentTime = newTime;

			repaint();

			for(final Consumer<Double> listener : timeListeners)
				listener.accept(currentTime);
		}
	}

	public double getCurrentTime(){
		return currentTime;
	}

	public ChronomapTimeline withTimeListener(final Consumer<Double> listener){
		if(listener != null)
			this.timeListeners.add(listener);

		return this;
	}

	public void resetZoom(){
		visibleStart = domainMin;
		visibleEnd = domainMax;
		repaint();
	}

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
		g2.drawLine(PADDING, axisY, w - PADDING, axisY);

		final long span = Math.max(1, visibleEnd - visibleStart);
		final long step = chooseStep(span, w);
		g2.setFont(getFont().deriveFont(10f));
		final FontMetrics fm = g2.getFontMetrics();

		final long startTick = (visibleStart / step) * step;
		for(long jdn = startTick; jdn <= visibleEnd; jdn += step){
			final int x = jdnToX(jdn, w);
			if(x < PADDING || x > w - PADDING)
				continue;

			g2.setColor(AXIS_LINE);
			g2.drawLine(x, axisY - 4, x, axisY + 4);
			final String label = formatYear(jdn);
			final int tw = fm.stringWidth(label);
			g2.setColor(AXIS_LABEL);
			g2.drawString(label, x - tw / 2, axisY + 16);
		}

		final int phX = jdnToX((long)currentTime, w);
		g2.setColor(PLAYHEAD);
		g2.setStroke(new BasicStroke(2f));
		g2.drawLine(phX, 4, phX, h - 4);

		g2.setFont(getFont().deriveFont(10f).deriveFont(Font.BOLD));
		final String timeLabel = formatYear((long)currentTime);
		final int tw = g2.getFontMetrics().stringWidth(timeLabel);
		g2.setColor(PLAYHEAD);
		g2.drawString(timeLabel, Math.max(PADDING, phX - tw / 2), 12);
	}

	private void configureKeyBindings(){
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "stepLeft");
		getActionMap().put("stepLeft", new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent e){
				stepTime(-DAYS_PER_MONTH);
			}
		});

		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "stepRight");
		getActionMap().put("stepRight", new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent e){
				stepTime(DAYS_PER_MONTH);
			}
		});

		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_DOWN_MASK), "stepLeftYear");
		getActionMap().put("stepLeftYear", new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent e){
				stepTime(-DAYS_PER_YEAR);
			}
		});

		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_DOWN_MASK), "stepRightYear");
		getActionMap().put("stepRightYear", new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent e){
				stepTime(DAYS_PER_YEAR);
			}
		});
	}

	private void configureGlobalKeyDispatcher(){
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
			if(e.getID() != KeyEvent.KEY_PRESSED || !isShowing())
				return false;

			final int keyCode = e.getKeyCode();
			final boolean isShift = e.isShiftDown();

			if(keyCode == KeyEvent.VK_LEFT){
				stepTime(isShift? -DAYS_PER_YEAR: -DAYS_PER_MONTH);
				return true;
			}
			else if(keyCode == KeyEvent.VK_RIGHT){
				stepTime(isShift? DAYS_PER_YEAR: DAYS_PER_MONTH);
				return true;
			}
			return false;
		});
	}

	private void stepTime(final double deltaJdn){
		final double newTime = Math.clamp(currentTime + deltaJdn, domainMin, domainMax);
		if(Double.compare(newTime, currentTime) != 0){
			currentTime = newTime;

			repaint();

			for(final Consumer<Double> listener : timeListeners)
				listener.accept(currentTime);
		}
	}

	private void installListeners(){
		final MouseAdapter adapter = new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent e){
				if(!SwingUtilities.isLeftMouseButton(e))
					return;

				requestFocusInWindow();

				final int currentX = jdnToX((long)currentTime, getWidth());
				draggingPlayhead = (Math.abs(e.getX() - currentX) <= PLAYHEAD_HIT_PX);
				wasDragged = false;
				dragAnchor = e.getPoint();

				// Direct click on track immediately jumps playhead to cursor position
				if(!draggingPlayhead){
					movePlayhead(e.getX());
					draggingPlayhead = true;
				}
			}

			@Override
			public void mouseDragged(final MouseEvent e){
				if(dragAnchor == null)
					return;

				final int dx = e.getX() - dragAnchor.x;
				if(Math.abs(dx) < DRAG_DEAD_ZONE_PX)
					return;

				wasDragged = true;

				if(draggingPlayhead){
					movePlayhead(e.getX());
				}
				else{
					pan(dx);
				}
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

				if(!wasDragged && e.getClickCount() == 1){
					movePlayhead(e.getX());
				}
				else if(e.getClickCount() == 2 && Math.abs(e.getX() - jdnToX((long)currentTime, getWidth())) <= PLAYHEAD_HIT_PX){
					resetZoom();
				}
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

		long newStart = visibleStart + delta;
		long newEnd = visibleEnd + delta;

		if(newStart < domainMin){
			newStart = domainMin;
			newEnd = Math.min(domainMax, newStart + span);
		}
		else if(newEnd > domainMax){
			newEnd = domainMax;
			newStart = Math.max(domainMin, newEnd - span);
		}

		visibleStart = newStart;
		visibleEnd = newEnd;

		repaint();
	}

	private void movePlayhead(final int x){
		final int w = getWidth();
		if(w <= 2 * PADDING)
			return;

		final long jdn = xToJdn(x, w);
		currentTime = Math.clamp(jdn, domainMin, domainMax);

		repaint();

		for(final Consumer<Double> listener : timeListeners)
			listener.accept(currentTime);
	}

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

	private static long chooseStep(final long spanDays, final int widthPixels){
		final double spanYears = spanDays / DAYS_PER_YEAR;
		final double pxPerYear = widthPixels / Math.max(1.e-3, spanYears);

		if(pxPerYear > 200) return (long)Math.max(1, Math.round(DAYS_PER_MONTH));
		if(pxPerYear > 50) return (long)Math.round(DAYS_PER_YEAR);
		if(pxPerYear > 10) return (long)Math.round(5 * DAYS_PER_YEAR);
		if(pxPerYear > 2) return (long)Math.round(10 * DAYS_PER_YEAR);
		if(pxPerYear > 0.5) return (long)Math.round(50 * DAYS_PER_YEAR);
		return (long)Math.round(100 * DAYS_PER_YEAR);
	}

	private static String formatYear(final long jdn){
		return Integer.toString(CalendarConverterDialog.jdnToGregorian(jdn)[0]);
	}

}
