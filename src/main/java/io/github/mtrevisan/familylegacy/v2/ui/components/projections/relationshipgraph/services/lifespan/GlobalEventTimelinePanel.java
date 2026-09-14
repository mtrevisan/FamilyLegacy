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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.relationshipgraph.services.lifespan;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.lifespan.EventIndex;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.NormalizedDate;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalAxis;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Global timeline of the events in the model.
 * <p>
 * <b>Layout.</b> The left column contains one row header per event type
 * present in the filtered data; the right area is the temporal content,
 * with one marker per event placed on its type's row at the X coordinate
 * corresponding to its date. The temporal axis is shared by every row, so
 * events of different types are directly comparable.
 * <p>
 * <b>Filtering.</b> The panel can be restricted to a subset of
 * participants through {@link #setParticipantFilter(Set)}. When a filter
 * is installed, only events whose participant list intersects the filter
 * are shown; the row set, the axis domain and the canvas size are all
 * recomputed accordingly. When the filter is {@code null} (the default),
 * all events are shown, which is the standalone behavior of the panel.
 * <p>
 * <b>Interaction.</b>
 * <ul>
 *   <li><b>Ctrl/Cmd + wheel</b> — zoom anchored at the cursor;</li>
 *   <li><b>plain wheel</b> — vertical scroll of the enclosing scroll pane;</li>
 *   <li><b>left drag</b> — horizontal pan of the visible time window;</li>
 *   <li><b>hover</b> — tooltip with type, date, place and participants of
 *       the marker under the cursor;</li>
 *   <li><b>single click</b> — select the marker;</li>
 *   <li><b>double click</b> — open the edit dialog for the underlying
 *       event record.</li>
 * </ul>
 * After an edit is saved, the panel rebuilds its internal index and
 * re-applies the current filter and view state.
 */
public class GlobalEventTimelinePanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 5281903752985610294L;


	/** Width of the row header column, in pixels. */
	private static final int HEADER_WIDTH = 160;
	/** Height of a single event-type row, in pixels. */
	private static final int ROW_HEIGHT = 28;
	/** Height of the axis strip, in pixels. */
	private static final int AXIS_HEIGHT = 30;
	/** Radius of an event marker, in pixels. */
	private static final int MARKER_RADIUS = 5;
	/** Dead zone for the drag, in pixels. */
	private static final int DRAG_DEAD_ZONE_PX = 3;
	/** Zoom multiplier applied to the visible span on each wheel rotation. */
	private static final double ZOOM_STEP = 1.15;

	private static final Font HEADER_FONT = new Font("Tahoma", Font.PLAIN, 12);
	private static final Font AXIS_FONT = new Font("Tahoma", Font.PLAIN, 11);

	private static final Color BACKGROUND = new Color(250, 249, 245);
	private static final Color HEADER_BG = new Color(244, 240, 232);
	private static final Color HEADER_BORDER = new Color(214, 208, 196);
	private static final Color AXIS_BG = new Color(248, 246, 240);
	private static final Color AXIS_LINE = new Color(140, 130, 110);
	private static final Color GRID = new Color(225, 220, 210);
	private static final Color LABEL = new Color(50, 40, 30);
	private static final Color SELECTED = new Color(220, 100, 60);

	private static final String[] MONTH_NAMES = {
		"Jan", "Feb", "Mar", "Apr", "May", "Jun",
		"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
	};


	private final FLEFModel model;

	/**
	 * Full event index over the model. Kept as a field so that a new
	 * filtered view can be rebuilt without re-reading the whole model,
	 * and so that an edit can trigger a rebuild of the index itself.
	 */
	private EventIndex index;

	/**
	 * Optional participant filter. When non-null, only events whose
	 * participant list contains at least one of these ids are shown.
	 * When null, no filter is applied.
	 */
	private Set<String> participantFilter;

	/**
	 * Events currently visible, i.e. the subset of {@link EventIndex}
	 * that passes {@link #participantFilter}. Recomputed by
	 * {@link #rebuildView()}.
	 */
	private List<EventIndex.EventDatum> visibleEvents = List.of();

	/**
	 * Event types present in {@link #visibleEvents}, sorted
	 * alphabetically. Determines the order of the row headers.
	 */
	private List<String> typeOrder = List.of();

	/**
	 * Maps an event type to its row index in the timeline. Derived from
	 * {@link #typeOrder}.
	 */
	private Map<String, Integer> typeRow = Map.of();

	/**
	 * Temporal axis. Rebuilt by {@link #rebuildView()} from the domain of
	 * the currently visible events.
	 */
	private TemporalAxis axis;

	private final Canvas canvas;
	private final JScrollPane scrollPane;

	/** Marker currently selected, or {@code null}. */
	private EventIndex.EventDatum selectedEvent;
	/** Marker currently under the cursor, or {@code null}. */
	private EventIndex.EventDatum hoveredEvent;

	/** Anchor for horizontal panning with the left mouse button. */
	private Point dragAnchor;


	/**
	 * Constructor.
	 *
	 * @param model the FLEF model (must not be {@code null})
	 */
	public GlobalEventTimelinePanel(final FLEFModel model){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");
		this.model = model;
		this.index = EventIndex.build(model);

		// Create the canvas before rebuilding the view, because rebuildView() calls revalidate() and repaint() on it
		this.canvas = new Canvas();
		this.scrollPane = new JScrollPane(canvas,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		this.scrollPane.setBorder(null);
		this.scrollPane.getVerticalScrollBar()
			.setUnitIncrement(16);

		setLayout(new BorderLayout());
		add(scrollPane, BorderLayout.CENTER);
		setPreferredSize(new Dimension(900, 500));

		ToolTipManager.sharedInstance()
			.registerComponent(canvas);
		installListeners();

		// Build the initial view (no filter)
		rebuildView();
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Restricts the timeline to events whose participants intersect the
	 * given set.
	 * <p>
	 * When {@code null}, all events are shown, which is the default
	 * behavior for standalone use of the panel.
	 * <p>
	 * After a call to this method the internal view is rebuilt from the
	 * full event index: the type rows, the axis domain, the canvas
	 * preferred size and the current selection are all recomputed so that
	 * the filtered view has exactly the space it needs and does not
	 * reference stale markers.
	 *
	 * @param participantIds the ids of the participants to keep, or
	 *                       {@code null} to disable the filter
	 */
	public void setParticipantFilter(final Set<String> participantIds){
		this.participantFilter = (participantIds != null? new HashSet<>(participantIds): null);

		rebuildView();
	}


	/* ======================================================================
	 *                          View rebuild
	 * ====================================================================== */

	/**
	 * Rebuilds the derived view state (visible events, type rows, axis
	 * domain, canvas preferred size) from the current event index and the
	 * current participant filter.
	 */
	private void rebuildView(){
		// Collect the events to display, applying the filter if present.
		final List<EventIndex.EventDatum> visible = new ArrayList<>();
		for(final EventIndex.EventDatum e : index.allEvents())
			if(matchesFilter(e))
				visible.add(e);
		this.visibleEvents = List.copyOf(visible);

		// Rebuild the type rows from the visible events.
		final Map<String, Integer> counts = new LinkedHashMap<>();
		for(final EventIndex.EventDatum e : visible)
			counts.merge(e.type(), 1, Integer::sum);
		final List<String> order = new ArrayList<>(counts.keySet());
		order.sort(String::compareToIgnoreCase);
		final Map<String, Integer> rowMap = new LinkedHashMap<>();
		for(int i = 0; i < order.size(); i ++)
			rowMap.put(order.get(i), i);
		this.typeOrder = List.copyOf(order);
		this.typeRow = Map.copyOf(rowMap);

		// Recompute the axis domain from the dated visible events
		NormalizedDate min = null;
		NormalizedDate max = null;
		for(final EventIndex.EventDatum e : visible){
			if(!e.hasDate())
				continue;
			if(min == null || e.date().compareTo(min) < 0)
				min = e.date();
			if(max == null || e.date().compareTo(max) > 0)
				max = e.date();
		}
		this.axis = new TemporalAxis(min, max);

		// Clear any stale selection or hover that points to an event
		// no longer visible.
		if(hoveredEvent != null && !visible.contains(hoveredEvent))
			hoveredEvent = null;
		if(selectedEvent != null && !visible.contains(selectedEvent))
			selectedEvent = null;

		if(canvas != null){
			canvas.revalidate();
			canvas.repaint();
		}
	}

	/**
	 * Returns whether the given event passes the current participant
	 * filter. When no filter is installed, every event passes.
	 */
	private boolean matchesFilter(final EventIndex.EventDatum event){
		if(participantFilter == null)
			return true;

		for(final EventIndex.Participant p : event.participants())
			if(participantFilter.contains(p.id()))
				return true;
		return false;
	}


	/* ======================================================================
	 *                          Interaction
	 * ====================================================================== */

	private void installListeners(){
		final MouseAdapter adapter = new MouseAdapter(){
			@Override
			public void mouseMoved(final MouseEvent e){
				final EventIndex.EventDatum hit = eventAt(e.getX(), e.getY());
				if(hit != hoveredEvent){
					hoveredEvent = hit;
					canvas.setCursor(hit != null
						? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
						: Cursor.getDefaultCursor());
					canvas.repaint();
				}
			}

			@Override
			public void mouseExited(final MouseEvent e){
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
				final int dy = e.getY() - dragAnchor.y;
				if(Math.abs(dx) < DRAG_DEAD_ZONE_PX && Math.abs(dy) < DRAG_DEAD_ZONE_PX)
					return;

				panHorizontally(dx);
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

				final EventIndex.EventDatum hit = eventAt(e.getX(), e.getY());
				if(hit == null){
					if(selectedEvent != null){
						selectedEvent = null;
						canvas.repaint();
					}

					return;
				}
				if(e.getClickCount() == 2)
					openEditDialog(hit);
				else{
					selectedEvent = (selectedEvent != null && selectedEvent.id().equals(hit.id())? null: hit);
					canvas.repaint();
				}
			}
		};
		canvas.addMouseListener(adapter);
		canvas.addMouseMotionListener(adapter);

		canvas.addMouseWheelListener(this::onMouseWheel);
	}

	/**
	 * Handles the mouse wheel.
	 * <p>
	 * With Ctrl/Cmd the wheel zooms the temporal axis anchored at the
	 * cursor. Without modifiers, the event is forwarded explicitly to the
	 * enclosing {@link JScrollPane}: Swing does not bubble wheel events
	 * from a component that has a wheel listener registered, so the
	 * forwarding must be done manually for vertical scrolling to keep
	 * working.
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
	 * Zooms the visible window in or out, keeping the date under the
	 * given X coordinate fixed.
	 */
	private void zoomAtCursor(final boolean zoomIn, final int cursorX){
		if(axis.isEmpty())
			return;

		final int contentX = cursorX - HEADER_WIDTH;
		if(contentX < 0)
			return;

		final long span = axis.visibleEndJdn() - axis.visibleStartJdn();
		if(zoomIn && span <= 2l)
			return;

		final long anchorJdn = axis.xToJdn(contentX);
		final double ratio = (double)(anchorJdn - axis.visibleStartJdn()) / (double)span;

		final long newSpan = Math.max(2l, (long)(span * (zoomIn? 1. / ZOOM_STEP: ZOOM_STEP)));
		final long newStart = anchorJdn - (long)(newSpan * ratio);
		axis.setVisibleRange(newStart, newStart + newSpan);
		canvas.repaint();
	}

	/**
	 * Pans the visible window horizontally by the given pixel amount. The
	 * amount is converted to days using the current visible span and the
	 * content width, so the pan is consistent at any zoom level.
	 */
	private void panHorizontally(final int dxPixels){
		if(axis.isEmpty())
			return;

		final int viewportWidth = axis.viewportWidth();
		if(viewportWidth <= 0)
			return;

		final long span = axis.visibleEndJdn() - axis.visibleStartJdn();
		final long deltaJdn = -(long)((double)dxPixels * span / viewportWidth);
		if(deltaJdn == 0l && dxPixels != 0)
			return;

		axis.pan(deltaJdn);
		canvas.repaint();
	}

	/**
	 * Returns the event marker under the given point, or {@code null}.
	 * Only events of the row that contains the point are considered, and
	 * the point must be inside the content area (not in the header).
	 */
	private EventIndex.EventDatum eventAt(final int x, final int y){
		final Rectangle contentBounds = new Rectangle(HEADER_WIDTH, AXIS_HEIGHT,
			canvas.getWidth() - HEADER_WIDTH, canvas.getHeight() - AXIS_HEIGHT);
		if(!contentBounds.contains(x, y))
			return null;

		final int row = (y - contentBounds.y) / ROW_HEIGHT;
		if(row < 0 || row >= typeOrder.size())
			return null;

		final String type = typeOrder.get(row);

		EventIndex.EventDatum nearest = null;
		int nearestDist = MARKER_RADIUS + 4;
		for(final EventIndex.EventDatum e : visibleEvents){
			if(!e.hasDate() || !type.equals(e.type()))
				continue;

			final int ex = contentBounds.x + axis.jdnToX(e.date().jdn());
			final int ey = contentBounds.y + row * ROW_HEIGHT + ROW_HEIGHT / 2;
			final int dist = (int)Math.hypot(ex - x, ey - y);
			if(dist < nearestDist){
				nearestDist = dist;
				nearest = e;
			}
		}
		return nearest;
	}


	/* ======================================================================
	 *                          Edit dialog
	 * ====================================================================== */

	private void openEditDialog(final EventIndex.EventDatum event){
		final FLEFRecord record = model.getRecordById(event.id());
		if(record == null)
			return;

		final Window owner = SwingUtilities.getWindowAncestor(this);
		final BaseRecordDialog dialog = EventHandler.getInstance()
			.createEditDialog(owner, model, record);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			// Rebuild the event index and re-apply the current view state (participant filter, zoom, selection) so that
			// the display reflects the updated event
			this.index = EventIndex.build(model);

			rebuildView();
		}
	}


	/* ======================================================================
	 *                          Canvas
	 * ====================================================================== */

	private final class Canvas extends JPanel{

		@Serial
		private static final long serialVersionUID = 4813729884621557321L;


		Canvas(){
			setBackground(BACKGROUND);
		}

		@Override
		public Dimension getPreferredSize(){
			final int contentHeight = Math.max(1, typeOrder.size()) * ROW_HEIGHT + 20;
			return new Dimension(900, AXIS_HEIGHT + contentHeight);
		}

		@Override
		public String getToolTipText(final MouseEvent event){
			final EventIndex.EventDatum hit = eventAt(event.getX(), event.getY());
			if(hit == null)
				return null;

			final StringBuilder sb = new StringBuilder("<html><b>")
				.append(escape(hit.type()))
				.append("</b>");
			if(hit.hasDate())
				sb.append(" — ").append(escape(formatDate(hit.date())));
			if(hit.hasPlace())
				sb.append("<br>Place: ").append(escape(hit.placeName()));
			if(!hit.participants().isEmpty()){
				sb.append("<br>Participants:");
				for(final EventIndex.Participant p : hit.participants())
					sb.append("<br>&nbsp;&nbsp;• ").append(escape(p.name()))
						.append(p.isIndividual()? StringUtils.EMPTY: " (group)");
			}
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

			final Rectangle headerBounds = new Rectangle(0, AXIS_HEIGHT, HEADER_WIDTH, height - AXIS_HEIGHT);
			final Rectangle contentBounds = new Rectangle(HEADER_WIDTH, AXIS_HEIGHT,
				width - HEADER_WIDTH, height - AXIS_HEIGHT);
			final Rectangle axisBounds = new Rectangle(HEADER_WIDTH, 0,
				width - HEADER_WIDTH, AXIS_HEIGHT);

			axis.setViewportWidth(contentBounds.width);

			paintAxis(g2, axisBounds, contentBounds);
			paintHeaders(g2, headerBounds);
			paintEvents(g2, contentBounds);
		}

		private void paintAxis(final Graphics2D g2, final Rectangle axisBounds, final Rectangle contentBounds){
			g2.setColor(AXIS_BG);
			g2.fillRect(axisBounds.x, axisBounds.y, axisBounds.width, axisBounds.height);

			g2.setColor(AXIS_LINE);
			g2.drawLine(axisBounds.x, axisBounds.y + axisBounds.height - 1,
				axisBounds.x + axisBounds.width - 1, axisBounds.y + axisBounds.height - 1);

			g2.setFont(AXIS_FONT);
			final FontMetrics fm = g2.getFontMetrics();
			for(final TemporalAxis.Tick tick : axis.computeTicks()){
				final int x = contentBounds.x + tick.x();
				g2.setColor(AXIS_LINE);
				g2.drawLine(x, axisBounds.y + axisBounds.height - 6, x, axisBounds.y + axisBounds.height);
				g2.setColor(LABEL);
				final int tw = fm.stringWidth(tick.label());
				g2.drawString(tick.label(), x - tw / 2, axisBounds.y + fm.getAscent() + 2);
			}
		}

		private void paintHeaders(final Graphics2D g2, final Rectangle headerBounds){
			g2.setColor(HEADER_BG);
			g2.fillRect(headerBounds.x, headerBounds.y, headerBounds.width, headerBounds.height);
			g2.setColor(HEADER_BORDER);
			g2.drawLine(headerBounds.x + headerBounds.width - 1, headerBounds.y,
				headerBounds.x + headerBounds.width - 1, headerBounds.y + headerBounds.height);
			g2.setFont(HEADER_FONT);

			for(int i = 0; i < typeOrder.size(); i ++){
				final String type = typeOrder.get(i);
				final int y = headerBounds.y + i * ROW_HEIGHT;
				g2.setColor(HEADER_BORDER);
				g2.drawLine(headerBounds.x, y + ROW_HEIGHT, headerBounds.x + headerBounds.width, y + ROW_HEIGHT);

				g2.setColor(typeColor(type));
				g2.fillRect(4, y + ROW_HEIGHT / 2 - 3, 6, 6);

				g2.setColor(LABEL);
				final FontMetrics fm = g2.getFontMetrics();
				g2.drawString(type, 16, y + (ROW_HEIGHT + fm.getAscent()) / 2 - 2);
			}
		}

		private void paintEvents(final Graphics2D g2, final Rectangle contentBounds){
			// Faint vertical grid lines aligned with the ticks
			g2.setColor(GRID);
			for(final TemporalAxis.Tick tick : axis.computeTicks()){
				final int x = contentBounds.x + tick.x();
				g2.drawLine(x, contentBounds.y, x, contentBounds.y + contentBounds.height);
			}

			for(final EventIndex.EventDatum event : visibleEvents){
				if(!event.hasDate())
					continue;
				final Integer row = typeRow.get(event.type());
				if(row == null)
					continue;
				final int x = contentBounds.x + axis.jdnToX(event.date().jdn());
				if(x < contentBounds.x || x > contentBounds.x + contentBounds.width)
					continue;
				final int y = contentBounds.y + row * ROW_HEIGHT + ROW_HEIGHT / 2;

				final boolean isSelected = (selectedEvent != null && selectedEvent.id().equals(event.id()));
				final boolean isHovered = (hoveredEvent != null && hoveredEvent.id().equals(event.id()));
				final Color base = isSelected? SELECTED: typeColor(event.type());
				final int r = MARKER_RADIUS + (isHovered || isSelected? 2: 0);

				g2.setColor(base);
				g2.fillOval(x - r, y - r, 2 * r, 2 * r);
				g2.setColor(Color.WHITE);
				g2.drawOval(x - r, y - r, 2 * r, 2 * r);
			}
		}
	}


	/* ======================================================================
	 *                          Palette and formatting
	 * ====================================================================== */

	private static Color typeColor(final String type){
		if(type == null)
			return new Color(120, 120, 120);

		final int h = Math.abs(type.hashCode());
		return Color.getHSBColor((h % 360) / 360f, 0.55f, 0.85f);
	}

	private static String formatDate(final NormalizedDate date){
		if(date == null)
			return "?";

		final int[] ymd = jdnToGregorian(date.jdn());
		return switch(date.precision()){
			case DAY -> ymd[2] + StringUtils.SPACE + MONTH_NAMES[ymd[1] - 1] + StringUtils.SPACE + ymd[0];
			case MONTH -> MONTH_NAMES[ymd[1] - 1] + StringUtils.SPACE + ymd[0];
			case YEAR -> Integer.toString(ymd[0]);
			case DECADE -> (ymd[0] / 10 * 10) + "s";
			case CENTURY -> (ymd[0] / 100 + 1) + "th century";
		};
	}

	private static String escape(final String s){
		if(s == null)
			return StringUtils.EMPTY;

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


	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final String content;
		try(final InputStream is = GlobalEventTimelinePanel.class.getResourceAsStream("/tests/TGMZ.flef")){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}
		final FLEFModel model = new FLEFParser().parse(content);

		SwingUtilities.invokeLater(() -> {
			final GlobalEventTimelinePanel panel = new GlobalEventTimelinePanel(model);
			// panel.setParticipantFilter(Set.of("I1", "I2", "I3"));

			final JFrame frame = new JFrame("Global Event Timeline");
			frame.add(panel);
			frame.setSize(1100, 700);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
