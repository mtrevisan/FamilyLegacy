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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.lifespan;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.events.EventIndex;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.NormalizedDate;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalAxis;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;


/**
 * Horizontal strip of lifespans, one row per individual.
 * <p>
 * The strip shares a single temporal axis across all rows, so that the
 * lifetimes of different people are directly comparable: the user sees at
 * a glance who was alive at the same time, whose life overlapped whose,
 * and where the events of each life are concentrated.
 * <p>
 * Each row contains:
 * <ul>
 *   <li>the individual's name, on the left;</li>
 *   <li>a bar spanning from the earliest to the latest dated event of
 *       that individual;</li>
 *   <li>one marker per dated event, placed at its date and colored by
 *       event type.</li>
 * </ul>
 * The temporal extent of the axis is computed from the union of all the
 * events shown, and it changes automatically when the set of individuals
 * changes.
 * <p>
 * Interaction:
 * <ul>
 *   <li><b>hover</b> — tooltip with the event type, date and place;</li>
 *   <li><b>double-click on a marker</b> — opens the edit dialog for the
 *       underlying event record; the strip rebuilds its index and
 *       refreshes after a successful edit.</li>
 * </ul>
 * The strip is meant to be embedded as a collapsible panel at the bottom
 * of a view. It never modifies the model directly.
 */
public final class MultiLifespanStripPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 4281904752198471039L;


	/** Height of a single row, in pixels. */
	private static final int ROW_HEIGHT = 18;
	/** Width of the name column, in pixels. */
	private static final int NAME_WIDTH = 160;
	/** Height of the axis, in pixels. */
	private static final int AXIS_HEIGHT = 20;
	/** Radius of an event marker, in pixels. */
	private static final int MARKER_RADIUS = 3;
	/** Left and right padding of the strip, in pixels. */
	private static final int PADDING = 8;
	/** Height of the bar representing a lifespan, in pixels. */
	private static final int BAR_HEIGHT = 4;

	/** Dead zone for the drag, in pixels. */
	private static final int DRAG_DEAD_ZONE_PX = 3;
	/** Zoom step multiplier applied to the visible span on each wheel rotation. */
	private static final double ZOOM_STEP = 1.15;

	private static final Font NAME_FONT = new Font("Tahoma", Font.PLAIN, 11);
	private static final Font AXIS_FONT = new Font("Tahoma", Font.PLAIN, 10);
	private static final Font EMPTY_FONT = new Font("Tahoma", Font.ITALIC, 12);

	private static final Color BACKGROUND = new Color(248, 246, 240);
	private static final Color NAME_COLOR = new Color(50, 40, 30);
	private static final Color AXIS_BACKGROUND = new Color(242, 240, 234);
	private static final Color AXIS_LINE = new Color(140, 130, 110);
	private static final Color AXIS_LABEL = new Color(80, 70, 50);
	private static final Color GRID = new Color(228, 225, 218);
	private static final Color BAR_COLOR = new Color(180, 178, 170, 200);
	private static final Color BAR_BORDER = new Color(120, 118, 110);
	private static final Color EMPTY_MESSAGE = new Color(140, 130, 120);
	private static final Color ROW_HIGHLIGHT = new Color(240, 236, 226);

	private static final String[] MONTH_NAMES = {
		"Jan", "Feb", "Mar", "Apr", "May", "Jun",
		"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
	};


	/**
	 * One individual represented in the strip, with the resolved name and
	 * the list of dated events, sorted chronologically.
	 *
	 * @param id     the individual id
	 * @param name   the display name
	 * @param events the dated events of this individual
	 * @param minJdn the JDN of the earliest event
	 * @param maxJdn the JDN of the latest event
	 */
	private record Row(String id, String name, List<EventIndex.EventDatum> events,
							 long minJdn, long maxJdn){
	}


	private final FLEFModel model;

	/**
	 * Event index over the whole model. Rebuilt on demand after an edit,
	 * so it is not final.
	 */
	private EventIndex eventIndex;

	private final StripCanvas canvas;
	private final JScrollPane scrollPane;

	private TemporalAxis axis;
	private List<Row> rows = List.of();
	private List<String> currentIds = List.of();

	private int hoveredRowIndex = -1;
	private EventIndex.EventDatum hoveredEvent;

	/** Anchor for horizontal panning with the left mouse button. */
	private Point dragAnchor;

	/**
	 * Constructor.
	 *
	 * @param model the FLEF model (must not be {@code null})
	 */
	public MultiLifespanStripPanel(final FLEFModel model){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");

		this.model = model;
		this.eventIndex = EventIndex.build(model);
		this.axis = new TemporalAxis((NormalizedDate)null, (NormalizedDate)null);

		this.canvas = new StripCanvas();
		this.scrollPane = new JScrollPane(canvas,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		this.scrollPane.setBorder(null);
		this.scrollPane.getVerticalScrollBar()
			.setUnitIncrement(ROW_HEIGHT);

		setLayout(new BorderLayout());
		add(scrollPane, BorderLayout.CENTER);

		// Default height: enough for about 8 rows plus the axis. The
		// caller can override via setPreferredSize.
		setPreferredSize(new Dimension(0, AXIS_HEIGHT + 8 * ROW_HEIGHT + 16));
		setMinimumSize(new Dimension(0, AXIS_HEIGHT + 2 * ROW_HEIGHT + 16));

		ToolTipManager.sharedInstance()
			.registerComponent(canvas);
		installListeners();
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Sets the individuals to display. Duplicates are silently ignored,
	 * and individuals with no dated events are excluded from the strip
	 * entirely, because they would produce empty rows.
	 *
	 * @param ids the individual ids (may be {@code null}, in which case
	 *            the strip is emptied)
	 */
	public void setIndividuals(final Collection<String> ids){
		this.currentIds = (ids != null? List.copyOf(new LinkedHashSet<>(ids)): List.of());

		final List<Row> newRows = new ArrayList<>(currentIds.size());
		NormalizedDate min = null;
		NormalizedDate max = null;

		for(final String id : currentIds){
			final List<EventIndex.EventDatum> dated = new ArrayList<>();
			long rowMin = Long.MAX_VALUE;
			long rowMax = Long.MIN_VALUE;
			for(final EventIndex.EventDatum e : eventIndex.eventsOf(id)){
				if(!e.hasDate())
					continue;
				dated.add(e);
				rowMin = Math.min(rowMin, e.date()
					.jdn());
				rowMax = Math.max(rowMax, e.date()
					.jdn());
				if(min == null || e.date()
					.compareTo(min) < 0)
					min = e.date();
				if(max == null || e.date()
					.compareTo(max) > 0)
					max = e.date();
			}
			if(dated.isEmpty())
				continue;
			dated.sort(Comparator.comparing(EventIndex.EventDatum::date));

			final String name = resolveName(id);
			newRows.add(new Row(id, name, dated, rowMin, rowMax));
		}

		// Sort by earliest date, then by name, to give a stable display
		// order that matches the chronological flow.
		newRows.sort(Comparator
			.comparingLong(Row::minJdn)
			.thenComparing(Row::name, String.CASE_INSENSITIVE_ORDER));

		this.rows = Collections.unmodifiableList(newRows);
		this.axis = new TemporalAxis(min, max);
		this.hoveredRowIndex = -1;
		this.hoveredEvent = null;

		canvas.revalidate();
		canvas.repaint();
	}


	/* ======================================================================
	 *                          Interaction
	 * ====================================================================== */

	private void installListeners(){
		final MouseAdapter adapter = new MouseAdapter(){
			@Override
			public void mouseMoved(final MouseEvent e){
				final int row = rowAt(e.getY());
				final EventIndex.EventDatum ev = (row >= 0? eventAt(row, e.getX()): null);
				if(row != hoveredRowIndex || ev != hoveredEvent){
					hoveredRowIndex = row;
					hoveredEvent = ev;
					canvas.setCursor(ev != null
						? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
						: Cursor.getDefaultCursor());
					canvas.repaint();
				}
			}

			@Override
			public void mouseExited(final MouseEvent e){
				hoveredRowIndex = -1;
				hoveredEvent = null;
				canvas.setCursor(Cursor.getDefaultCursor());
				canvas.repaint();
			}

			@Override
			public void mousePressed(final MouseEvent e){
				if(SwingUtilities.isLeftMouseButton(e))
					dragAnchor = e.getPoint();
			}

			@Override
			public void mouseDragged(final MouseEvent e){
				if(dragAnchor == null || !SwingUtilities.isLeftMouseButton(e))
					return;
				final int dx = e.getX() - dragAnchor.x;
				if(Math.abs(dx) < DRAG_DEAD_ZONE_PX)
					return;
				panByPixels(dx);
				dragAnchor = e.getPoint();
			}

			@Override
			public void mouseReleased(final MouseEvent e){
				dragAnchor = null;
			}

			@Override
			public void mouseClicked(final MouseEvent e){
				if(!SwingUtilities.isLeftMouseButton(e))
					return;
				final int row = rowAt(e.getY());
				if(row < 0){
					// Click on the axis area: reset the zoom to the full
					// domain, providing a natural "zoom out all the way"
					// gesture.
					resetZoom();
					return;
				}
				if(e.getClickCount() != 2)
					return;
				final EventIndex.EventDatum hit = eventAt(row, e.getX());
				if(hit != null)
					openEventEditDialog(hit);
			}
		};
		canvas.addMouseListener(adapter);
		canvas.addMouseMotionListener(adapter);

		canvas.addMouseWheelListener(this::onMouseWheel);
	}

	/**
	 * Handles the mouse wheel.
	 * <p>
	 * With Ctrl/Cmd the wheel zooms the temporal axis anchored at the cursor.
	 * Without modifiers, the event is forwarded explicitly to the enclosing
	 * {@link JScrollPane}: Swing does not bubble wheel events from a
	 * component that has a wheel listener registered, so the forwarding must
	 * be done manually for vertical scrolling to keep working.
	 *
	 * @param e the wheel event
	 */
	private void onMouseWheel(final MouseWheelEvent e){
		if(e.isControlDown() || e.isMetaDown()){
			e.consume();

			final int rotations = e.getWheelRotation();
			if(rotations != 0)
				zoomAtCursor(rotations < 0, e.getX());

			return;
		}

		final JScrollBar bar = scrollPane.getVerticalScrollBar();
		if(bar == null || !bar.isVisible())
			return;

		final int direction = (e.getWheelRotation() < 0? -1: 1);
		final int increment = (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL
			? bar.getUnitIncrement(direction) * e.getUnitsToScroll()
			: bar.getBlockIncrement(direction) * e.getWheelRotation());
		bar.setValue(bar.getValue() + increment);
		e.consume();
	}

	/**
	 * Zooms the visible window in or out, keeping the date under the given
	 * X coordinate fixed.
	 *
	 * @param zoomIn  {@code true} to zoom in, {@code false} to zoom out
	 * @param cursorX the X coordinate of the anchor point, relative to the
	 *                whole strip
	 */
	private void zoomAtCursor(final boolean zoomIn, final int cursorX){
		if(axis.isEmpty())
			return;

		// Translate the cursor X into the content coordinate system (the
		// axis works in content pixels, not in strip pixels).
		final int contentX = cursorX - PADDING - NAME_WIDTH;
		if(contentX < 0)
			return;

		final long span = axis.visibleEndJdn() - axis.visibleStartJdn();
		if(zoomIn && span <= 2L)
			return;

		final long anchorJdn = axis.xToJdn(contentX);
		final double ratio = (double)(anchorJdn - axis.visibleStartJdn()) / (double)span;

		final long newSpan = Math.max(2L, (long)(span * (zoomIn? 1.0 / ZOOM_STEP: ZOOM_STEP)));
		final long newStart = anchorJdn - (long)(newSpan * ratio);
		axis.setVisibleRange(newStart, newStart + newSpan);

		canvas.repaint();
	}

	/**
	 * Pans the visible window horizontally by the given pixel amount. The
	 * amount is converted to days using the current visible span and the
	 * content width, so the pan is consistent at any zoom level.
	 *
	 * @param dxPixels the horizontal displacement in pixels
	 */
	private void panByPixels(final int dxPixels){
		if(axis.isEmpty())
			return;
		final int contentWidth = Math.max(1, canvas.getWidth() - 2 * PADDING - NAME_WIDTH);
		final long span = axis.visibleEndJdn() - axis.visibleStartJdn();
		if(contentWidth <= 0 || span <= 0L)
			return;

		final long deltaJdn = -(long)((double)dxPixels * span / contentWidth);
		if(deltaJdn == 0L && dxPixels != 0)
			return;
		axis.pan(deltaJdn);
		canvas.repaint();
	}

	/**
	 * Resets the visible window to the full domain, discarding any zoom or
	 * pan the user has applied.
	 */
	private void resetZoom(){
		if(axis.isEmpty())
			return;
		axis.fitToDomain();
		canvas.repaint();
	}

	/**
	 * Returns the index of the row that contains the given Y coordinate,
	 * or {@code -1} if the coordinate is outside the content area.
	 */
	private int rowAt(final int y){
		if(y < AXIS_HEIGHT)
			return -1;
		final int row = (y - AXIS_HEIGHT) / ROW_HEIGHT;
		return (row >= 0 && row < rows.size()? row: -1);
	}

	/**
	 * Returns the event marker under the given point, or {@code null}.
	 */
	private EventIndex.EventDatum eventAt(final int rowIndex, final int x){
		if(rowIndex < 0 || rowIndex >= rows.size())
			return null;
		final Row row = rows.get(rowIndex);
		final int contentX = x - PADDING - NAME_WIDTH;
		if(contentX < 0)
			return null;

		final int contentWidth = Math.max(1, canvas.getWidth() - 2 * PADDING - NAME_WIDTH);
		axis.setViewportWidth(contentWidth);

		EventIndex.EventDatum best = null;
		int bestDist = MARKER_RADIUS + 4;
		for(final EventIndex.EventDatum e : row.events()){
			final int ex = axis.jdnToX(e.date()
				.jdn());
			final int d = Math.abs(ex - contentX);
			if(d < bestDist){
				bestDist = d;
				best = e;
			}
		}
		return best;
	}


	/* ======================================================================
	 *                          Edit
	 * ====================================================================== */

	/**
	 * Opens the edit dialog for the given event and, when the edit is
	 * saved, rebuilds the internal index and re-applies the current
	 * individuals so that the strip reflects the updated data.
	 *
	 * @param event the event to edit (must not be {@code null})
	 */
	private void openEventEditDialog(final EventIndex.EventDatum event){
		final FLEFRecord record = model.getRecordById(event.id());
		if(record == null)
			return;

		final Window owner = SwingUtilities.getWindowAncestor(this);
		final BaseRecordDialog dialog = EventHandler.getInstance()
			.createEditDialog(owner, model, record);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			this.eventIndex = EventIndex.build(model);
			setIndividuals(currentIds);
		}
	}


	/* ======================================================================
	 *                          Canvas
	 * ====================================================================== */

	private final class StripCanvas extends JPanel{

		@java.io.Serial
		private static final long serialVersionUID = 8492037491820374821L;


		StripCanvas(){
			setBackground(BACKGROUND);
			setOpaque(true);
		}

		@Override
		public Dimension getPreferredSize(){
			final int rowsHeight = Math.max(1, rows.size()) * ROW_HEIGHT;
			return new Dimension(800, AXIS_HEIGHT + rowsHeight + 12);
		}

		@Override
		public String getToolTipText(final MouseEvent event){
			final int row = rowAt(event.getY());
			if(row < 0){
				// Empty axis area: help the user discover the zoom gestures.
				return "<html>Ctrl + wheel: zoom<br>"
					+ "Drag: pan<br>"
					+ "Click: reset zoom</html>";
			}
			final EventIndex.EventDatum e = eventAt(row, event.getX());
			if(e == null)
				return null;

			final StringBuilder sb = new StringBuilder("<html><b>")
				.append(escape(rows.get(row).name()))
				.append("</b><br>")
				.append(escape(e.type()));
			if(e.hasDate())
				sb.append(" — ").append(escape(formatDate(e.date())));
			if(e.hasPlace())
				sb.append("<br>Place: ").append(escape(e.placeName()));
			sb.append("<br><i>Double-click to edit</i></html>");
			return sb.toString();
		}

		@Override
		protected void paintComponent(final Graphics g){
			super.paintComponent(g);
			if(!(g instanceof Graphics2D g2))
				return;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			final int width = getWidth();
			final int height = getHeight();
			g2.setColor(BACKGROUND);
			g2.fillRect(0, 0, width, height);

			if(rows.isEmpty()){
				paintEmptyMessage(g2, width, height);
				return;
			}

			final Rectangle contentBounds = new Rectangle(PADDING + NAME_WIDTH, 0,
				Math.max(1, width - 2 * PADDING - NAME_WIDTH), height);
			axis.setViewportWidth(contentBounds.width);

			paintAxis(g2, contentBounds);
			paintRows(g2, contentBounds);
		}

		private void paintEmptyMessage(final Graphics2D g2, final int width, final int height){
			g2.setFont(EMPTY_FONT);
			g2.setColor(EMPTY_MESSAGE);
			final FontMetrics fm = g2.getFontMetrics();
			final String msg = "No dated events for the individuals in this view.";
			final int tw = fm.stringWidth(msg);
			g2.drawString(msg, (width - tw) / 2, height / 2 + fm.getAscent() / 2);
		}

		private void paintAxis(final Graphics2D g2, final Rectangle contentBounds){
			g2.setColor(AXIS_BACKGROUND);
			g2.fillRect(0, 0, getWidth(), AXIS_HEIGHT);
			g2.setColor(AXIS_LINE);
			g2.drawLine(contentBounds.x, AXIS_HEIGHT - 1,
				contentBounds.x + contentBounds.width, AXIS_HEIGHT - 1);

			g2.setFont(AXIS_FONT);
			final FontMetrics fm = g2.getFontMetrics();
			for(final TemporalAxis.Tick tick : axis.computeTicks()){
				final int x = contentBounds.x + tick.x();
				if(x < contentBounds.x || x > contentBounds.x + contentBounds.width)
					continue;
				g2.setColor(AXIS_LINE);
				g2.drawLine(x, AXIS_HEIGHT - 5, x, AXIS_HEIGHT);
				g2.setColor(AXIS_LABEL);
				final int tw = fm.stringWidth(tick.label());
				g2.drawString(tick.label(), x - tw / 2, fm.getAscent() + 1);
			}
		}

		private void paintRows(final Graphics2D g2, final Rectangle contentBounds){
			final int barYOffset = (ROW_HEIGHT - BAR_HEIGHT) / 2;

			// Save the current clip so that we can restrict the drawing of bars
			// and markers to the content area. Without this clip, when the zoom
			// moves the visible window the bars would extend into the name
			// column, overlapping the labels.
			final java.awt.Shape originalClip = g2.getClip();

			for(int i = 0; i < rows.size(); i++){
				final Row row = rows.get(i);
				final int y = AXIS_HEIGHT + i * ROW_HEIGHT;
				final boolean hovered = (i == hoveredRowIndex);

				// Row background: drawn across the full width, so it also covers
				// the name column when the row is hovered.
				if(hovered){
					g2.setColor(ROW_HIGHLIGHT);
					g2.fillRect(0, y, getWidth(), ROW_HEIGHT);
				}

				// Name: drawn outside the content clip.
				g2.setClip(originalClip);
				g2.setFont(NAME_FONT);
				g2.setColor(NAME_COLOR);
				final FontMetrics fm = g2.getFontMetrics();
				final String name = truncate(fm, row.name(), NAME_WIDTH - 10);
				g2.drawString(name, PADDING, y + (ROW_HEIGHT + fm.getAscent()) / 2 - 1);

				// From here on, restrict the drawing to the content area.
				final Rectangle rowContentClip = new Rectangle(
					contentBounds.x, y, contentBounds.width, ROW_HEIGHT);
				g2.setClip(rowContentClip);

				// Faint vertical grid on the content area.
				g2.setColor(GRID);
				g2.setStroke(new BasicStroke(1f));
				for(final TemporalAxis.Tick tick : axis.computeTicks()){
					final int x = contentBounds.x + tick.x();
					g2.drawLine(x, y, x, y + ROW_HEIGHT);
				}

				// Lifespan bar.
				final int barX1 = contentBounds.x + axis.jdnToX(row.minJdn());
				final int barX2 = contentBounds.x + axis.jdnToX(row.maxJdn());
				final int barY = y + barYOffset;
				final int barWidth = Math.max(2, barX2 - barX1);
				g2.setColor(BAR_COLOR);
				g2.fillRoundRect(barX1, barY, barWidth, BAR_HEIGHT, BAR_HEIGHT, BAR_HEIGHT);
				g2.setColor(BAR_BORDER);
				g2.drawRoundRect(barX1, barY, barWidth, BAR_HEIGHT, BAR_HEIGHT, BAR_HEIGHT);

				// Event markers.
				final int centerY = y + ROW_HEIGHT / 2;
				for(final EventIndex.EventDatum e : row.events()){
					final int ex = contentBounds.x + axis.jdnToX(e.date().jdn());
					final boolean isHovered = (hoveredEvent != null && hoveredEvent.id().equals(e.id()));
					final int r = MARKER_RADIUS + (isHovered? 2: 0);
					g2.setColor(typeColor(e.type()));
					g2.fillOval(ex - r, centerY - r, 2 * r, 2 * r);
					g2.setColor(Color.WHITE);
					g2.drawOval(ex - r, centerY - r, 2 * r, 2 * r);
				}
			}

			// Restore the original clip when done.
			g2.setClip(originalClip);
		}
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private String resolveName(final String id){
		final FLEFRecord record = model.getRecordById(id);
		if(record == null)
			return id;
		try{
			final String text = IndividualHandler.getInstance()
				.getDisplayText(record, model);
			return (text != null && !text.isBlank()? text: id);
		}
		catch(final RuntimeException ignored){
			return id;
		}
	}

	private static Color typeColor(final String type){
		if(type == null)
			return new Color(120, 120, 120);
		final int h = Math.abs(type.hashCode());
		return Color.getHSBColor((h % 360) / 360f, 0.55f, 0.80f);
	}

	private static String formatDate(final NormalizedDate date){
		if(date == null)
			return "?";
		final int[] ymd = jdnToGregorian(date.jdn());
		return switch(date.precision()){
			case DAY -> ymd[2] + " " + MONTH_NAMES[ymd[1] - 1] + " " + ymd[0];
			case MONTH -> MONTH_NAMES[ymd[1] - 1] + " " + ymd[0];
			case YEAR -> Integer.toString(ymd[0]);
			case DECADE -> (ymd[0] / 10 * 10) + "s";
			case CENTURY -> (ymd[0] / 100 + 1) + "th c.";
		};
	}

	private static String truncate(final FontMetrics fm, final String text, final int maxWidth){
		if(text == null)
			return "";
		if(fm.stringWidth(text) <= maxWidth)
			return text;
		String current = text;
		while(current.length() > 1 && fm.stringWidth(current + "…") > maxWidth)
			current = current.substring(0, current.length() - 1);
		return current + "…";
	}

	private static String escape(final String s){
		if(s == null)
			return "";
		return s.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
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
