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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.events;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.NormalizedDate;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Network of events: nodes are events, edges connect events that share at
 * least one participant or one place.
 * <p>
 * <b>Hairball avoidance.</b> A naive co-occurrence graph produces an
 * unusable "hairball" when a single hub (a parish, a notary, a repeated
 * witness) connects hundreds of events, generating O(n²) edges. To keep
 * the graph readable, a shared element (participant or place) only
 * contributes edges when it appears in at most
 * {@link #MAX_SHARED_EVENTS} events. Common hubs are therefore treated as
 * background context, not as connections, which is the standard practice
 * in co-occurrence network analysis.
 * <p>
 * Layout is circular: events are ordered chronologically around a circle,
 * so temporally close events are visually close and cluster structure is
 * immediately readable.
 * <p>
 * Interaction:
 * <ul>
 *   <li><b>Ctrl/Cmd + wheel</b> — zoom anchored at the cursor;</li>
 *   <li><b>left drag</b> — pan;</li>
 *   <li><b>hover</b> — tooltip with type, date, place and participants;</li>
 *   <li><b>single click</b> — select the node;</li>
 *   <li><b>double click</b> — open the edit dialog for the underlying
 *       event record.</li>
 * </ul>
 * After an edit is saved, the graph is rebuilt from the model.
 */
@Deprecated
public class EventNetworkPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 6248190384971276481L;


	/**
	 * A shared element (participant or place) that appears in more than
	 * this number of events is considered a hub and does not generate
	 * edges. Tuned empirically: 10 keeps the graph readable while
	 * preserving the meaningful connections.
	 */
	private static final int MAX_SHARED_EVENTS = 10;

	/** Maximum number of events shown. Beyond this, the graph is truncated to the earliest ones. */
	private static final int MAX_EVENTS = 1_000;

	private static final int NODE_RADIUS = 8;
	private static final int LABEL_MARGIN = 5;
	private static final int PADDING = 60;
	private static final int LABEL_THRESHOLD = 80;
	private static final int DRAG_DEAD_ZONE_PX = 3;
	private static final double MIN_SCALE = 0.5;
	private static final double MAX_SCALE = 8.0;
	private static final double ZOOM_STEP = 1.15;

	private static final Color BACKGROUND = new Color(250, 249, 245);
	private static final Color NODE = new Color(70, 120, 190);
	private static final Color NODE_BORDER = new Color(30, 60, 120);
	private static final Color NODE_SELECTED = new Color(220, 100, 60);
	private static final Color EDGE_PARTICIPANT = new Color(70, 120, 190, 130);
	private static final Color EDGE_PLACE = new Color(210, 130, 40, 130);
	private static final Color LABEL = new Color(40, 40, 40);

	private static final Font LABEL_FONT = new Font("Tahoma", Font.PLAIN, 10);
	private static final Font HOVER_FONT = new Font("Tahoma", Font.BOLD, 11);


	private final FLEFModel model;

	private List<EventIndex.EventDatum> events;
	private Map<String, Point2D> positions;
	private Map<String, Integer> indexById;
	private List<Edge> edges;

	private EventIndex.EventDatum hovered;
	private EventIndex.EventDatum selected;

	// View transform.
	private double scale = 1.0;
	private double offsetX = 0.0;
	private double offsetY = 0.0;

	// Drag state.
	private Point dragAnchor;

	private final Canvas canvas;


	/** An edge between two events, with the reason for the connection. */
	private record Edge(int fromIndex, int toIndex, boolean byParticipant, boolean byPlace){
	}


	public EventNetworkPanel(final FLEFModel model){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");
		this.model = model;

		buildGraph();

		this.canvas = new Canvas();
		setLayout(new java.awt.BorderLayout());
		add(canvas, java.awt.BorderLayout.CENTER);
		setPreferredSize(new Dimension(900, 720));

		installListeners();
	}


	/* ======================================================================
	 *                          Graph construction
	 * ====================================================================== */

	private void buildGraph(){
		final EventIndex index = EventIndex.build(model);

		// Keep only dated events, ordered chronologically. If there are
		// more than MAX_EVENTS, keep the earliest ones: they are the most
		// informative for the network structure.
		List<EventIndex.EventDatum> dated = new ArrayList<>();
		for(final EventIndex.EventDatum e : index.allEvents())
			if(e.hasDate())
				dated.add(e);
		dated.sort(java.util.Comparator.comparing(EventIndex.EventDatum::date));
		if(dated.size() > MAX_EVENTS)
			dated = new ArrayList<>(dated.subList(0, MAX_EVENTS));

		this.events = List.copyOf(dated);
		this.indexById = new HashMap<>();
		for(int i = 0; i < events.size(); i++)
			indexById.put(events.get(i).id(), i);
		this.positions = computeCircularPositions(events);
		this.edges = buildEdges(events);
	}


	private static Map<String, Point2D> computeCircularPositions(final List<EventIndex.EventDatum> events){
		final Map<String, Point2D> result = new HashMap<>();
		final int n = events.size();
		if(n == 0)
			return result;
		for(int i = 0; i < n; i++){
			final double angle = -Math.PI / 2.0 + 2.0 * Math.PI * i / n;
			result.put(events.get(i).id(),
				new Point2D.Double(Math.cos(angle), Math.sin(angle)));
		}
		return result;
	}

	/**
	 * Builds the edge list using a hub-filtered co-occurrence approach.
	 * <p>
	 * For each shared element (participant or place), the events that
	 * carry it are grouped together. If the group size exceeds
	 * {@link #MAX_SHARED_EVENTS}, the element is treated as a hub and
	 * contributes no edges. Otherwise, all pairs within the group are
	 * emitted, merging duplicate pairs so that a pair of events sharing
	 * both a participant and a place produces a single edge.
	 */
	private List<Edge> buildEdges(final List<EventIndex.EventDatum> events){
		// Pair key: a single long encoding the two event indices in the
		// order (min, max). Values: bitmask (1 = participant, 2 = place).
		final Map<Long, Integer> pairMask = new HashMap<>();

		// Index events by participant.
		final Map<String, List<Integer>> eventsByParticipant = new HashMap<>();
		for(int i = 0; i < events.size(); i++)
			for(final EventIndex.Participant p : events.get(i).participants())
				eventsByParticipant.computeIfAbsent(p.id(), k -> new ArrayList<>()).add(i);

		// Index events by place.
		final Map<String, List<Integer>> eventsByPlace = new HashMap<>();
		for(int i = 0; i < events.size(); i++)
			if(events.get(i).hasPlace())
				eventsByPlace.computeIfAbsent(events.get(i).placeId(), k -> new ArrayList<>()).add(i);

		// Emit pairs for non-hub groups.
		for(final List<Integer> group : eventsByParticipant.values())
			if(group.size() <= MAX_SHARED_EVENTS)
				emitPairs(group, 1, pairMask);
		for(final List<Integer> group : eventsByPlace.values())
			if(group.size() <= MAX_SHARED_EVENTS)
				emitPairs(group, 2, pairMask);

		// Convert the pair mask to edges.
		final List<Edge> result = new ArrayList<>(pairMask.size());
		for(final Map.Entry<Long, Integer> entry : pairMask.entrySet()){
			final long key = entry.getKey();
			final int from = (int)(key >>> 32);
			final int to = (int)(key & 0xFFFFFFFFL);
			final int mask = entry.getValue();
			result.add(new Edge(from, to, (mask & 1) != 0, (mask & 2) != 0));
		}
		return result;
	}

	private static void emitPairs(final List<Integer> group, final int bit, final Map<Long, Integer> pairMask){
		for(int i = 0; i < group.size(); i++)
			for(int j = i + 1; j < group.size(); j++){
				final int a = Math.min(group.get(i), group.get(j));
				final int b = Math.max(group.get(i), group.get(j));
				final long key = ((long)a << 32) | (b & 0xFFFFFFFFL);
				pairMask.merge(key, bit, (existing, incoming) -> existing | incoming);
			}
	}


	/* ======================================================================
	 *                          Interaction
	 * ====================================================================== */

	private void installListeners(){
		final MouseAdapter adapter = new MouseAdapter(){
			@Override
			public void mouseMoved(final MouseEvent e){
				final EventIndex.EventDatum hit = nodeAt(e.getPoint());
				if(hit != hovered){
					hovered = hit;
					canvas.setCursor(hit != null
						? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
						: Cursor.getDefaultCursor());
					canvas.repaint();
				}
			}

			@Override
			public void mouseExited(final MouseEvent e){
				hovered = null;
				canvas.setCursor(Cursor.getDefaultCursor());
				canvas.repaint();
			}

			@Override
			public void mousePressed(final MouseEvent e){
				if(javax.swing.SwingUtilities.isLeftMouseButton(e))
					dragAnchor = e.getPoint();
			}

			@Override
			public void mouseDragged(final MouseEvent e){
				if(dragAnchor == null || !javax.swing.SwingUtilities.isLeftMouseButton(e))
					return;
				final int dx = e.getX() - dragAnchor.x;
				final int dy = e.getY() - dragAnchor.y;
				if(Math.abs(dx) < DRAG_DEAD_ZONE_PX && Math.abs(dy) < DRAG_DEAD_ZONE_PX)
					return;
				offsetX += dx;
				offsetY += dy;
				dragAnchor = e.getPoint();
				canvas.repaint();
			}

			@Override
			public void mouseReleased(final MouseEvent e){
				dragAnchor = null;
			}

			@Override
			public void mouseClicked(final MouseEvent e){
				if(!javax.swing.SwingUtilities.isLeftMouseButton(e))
					return;
				final EventIndex.EventDatum hit = nodeAt(e.getPoint());
				if(hit == null){
					if(selected != null){
						selected = null;
						canvas.repaint();
					}
					return;
				}
				if(e.getClickCount() == 2)
					openEditDialog(hit);
				else{
					selected = (selected != null && selected.id().equals(hit.id())? null: hit);
					canvas.repaint();
				}
			}
		};
		canvas.addMouseListener(adapter);
		canvas.addMouseMotionListener(adapter);
		canvas.addMouseWheelListener(this::onMouseWheel);
	}

	private void onMouseWheel(final MouseWheelEvent e){
		if(!e.isControlDown() && !e.isMetaDown())
			return;
		e.consume();
		final int rotations = e.getWheelRotation();
		if(rotations == 0)
			return;
		final double factor = (rotations < 0? ZOOM_STEP: 1.0 / ZOOM_STEP);
		final double newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale * factor));
		if(newScale == scale)
			return;

		final int cx = e.getX();
		final int cy = e.getY();
		final double layoutX = (cx - offsetX) / scale;
		final double layoutY = (cy - offsetY) / scale;
		scale = newScale;
		offsetX = cx - layoutX * scale;
		offsetY = cy - layoutY * scale;
		canvas.repaint();
	}

	/**
	 * Returns the event whose node is under the given screen point, or
	 * {@code null}. The point is transformed back into layout space so
	 * that hit testing is independent of zoom and pan.
	 */
	private EventIndex.EventDatum nodeAt(final Point screenPoint){
		if(events.isEmpty())
			return null;

		final double layoutX = (screenPoint.x - offsetX) / scale;
		final double layoutY = (screenPoint.y - offsetY) / scale;
		final double centerX = canvas.getWidth() / 2.0;
		final double centerY = canvas.getHeight() / 2.0;
		final int size = Math.min(canvas.getWidth(), canvas.getHeight()) - 2 * PADDING;
		final double radius = size / 2.0;
		if(radius <= 0)
			return null;

		EventIndex.EventDatum best = null;
		double bestDist = (NODE_RADIUS + 4) * (NODE_RADIUS + 4);
		for(final EventIndex.EventDatum e : events){
			final Point2D p = positions.get(e.id());
			if(p == null)
				continue;
			final double x = centerX + p.getX() * radius;
			final double y = centerY + p.getY() * radius;
			final double dx = layoutX - x;
			final double dy = layoutY - y;
			final double d2 = dx * dx + dy * dy;
			if(d2 < bestDist){
				bestDist = d2;
				best = e;
			}
		}
		return best;
	}

	private void openEditDialog(final EventIndex.EventDatum event){
		final FLEFRecord record = model.getRecordById(event.id());
		if(record == null)
			return;

		final Window owner = SwingUtilities.getWindowAncestor(this);
		final BaseRecordDialog dialog = EventHandler.getInstance()
			.createEditDialog(owner, model, record);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			buildGraph();
			hovered = null;
			selected = null;
			offsetX = 0.0;
			offsetY = 0.0;
			scale = 1.0;
			canvas.repaint();
		}
	}


	/* ======================================================================
	 *                          Canvas
	 * ====================================================================== */

	private final class Canvas extends JPanel{

		@java.io.Serial
		private static final long serialVersionUID = 7305102956193742918L;


		Canvas(){
			setBackground(BACKGROUND);
		}

		@Override
		public String getToolTipText(final MouseEvent event){
			final EventIndex.EventDatum hit = nodeAt(event.getPoint());
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
						.append(p.isIndividual()? "": " (group)");
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
			g2.setColor(BACKGROUND);
			g2.fillRect(0, 0, getWidth(), getHeight());

			if(events.isEmpty())
				return;

			final int size = Math.min(getWidth(), getHeight()) - 2 * PADDING;
			final double centerX = getWidth() / 2.0;
			final double centerY = getHeight() / 2.0;
			final double radius = size / 2.0;

			// Apply the view transform.
			g2.translate(offsetX, offsetY);
			g2.scale(scale, scale);

			// Edges.
			drawEdges(g2, centerX, centerY, radius);

			// Nodes.
			drawNodes(g2, centerX, centerY, radius);
		}

		private void drawEdges(final Graphics2D g2, final double centerX, final double centerY,
			final double radius){
			for(final Edge edge : edges){
				final EventIndex.EventDatum a = events.get(edge.fromIndex());
				final EventIndex.EventDatum b = events.get(edge.toIndex());
				final Point2D pa = positions.get(a.id());
				final Point2D pb = positions.get(b.id());
				if(pa == null || pb == null)
					continue;
				final double ax = centerX + pa.getX() * radius;
				final double ay = centerY + pa.getY() * radius;
				final double bx = centerX + pb.getX() * radius;
				final double by = centerY + pb.getY() * radius;

				if(edge.byParticipant()){
					g2.setColor(EDGE_PARTICIPANT);
					g2.setStroke(new BasicStroke(1f));
					g2.draw(new Line2D.Double(ax, ay, bx, by));
				}
				if(edge.byPlace()){
					g2.setColor(EDGE_PLACE);
					g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
						10f, new float[]{4f, 4f}, 0f));
					g2.draw(new Line2D.Double(ax, ay, bx, by));
				}
			}
		}

		private void drawNodes(final Graphics2D g2, final double centerX, final double centerY,
			final double radius){
			final boolean drawLabels = (events.size() <= LABEL_THRESHOLD) || (scale >= 2.5);
			g2.setFont(LABEL_FONT);

			for(final EventIndex.EventDatum e : events){
				final Point2D p = positions.get(e.id());
				if(p == null)
					continue;
				final double x = centerX + p.getX() * radius;
				final double y = centerY + p.getY() * radius;

				final boolean isSel = (selected != null && selected.id().equals(e.id()));
				final boolean isHov = (hovered != null && hovered.id().equals(e.id()));
				final int r = NODE_RADIUS + (isSel || isHov? 3: 0);

				g2.setColor(isSel? NODE_SELECTED: NODE);
				g2.fillOval((int)(x - r), (int)(y - r), 2 * r, 2 * r);
				g2.setColor(NODE_BORDER);
				g2.setStroke(new BasicStroke(1.2f));
				g2.drawOval((int)(x - r), (int)(y - r), 2 * r, 2 * r);

				// Labels only when the graph is small enough or the user
				// has zoomed in. The hovered or selected node always shows
				// its label.
				if(drawLabels || isSel || isHov){
					final Font f = (isSel || isHov? HOVER_FONT: LABEL_FONT);
					g2.setFont(f);
					final FontMetrics fm = g2.getFontMetrics();
					final String label = shortLabel(e);
					final double angle = Math.atan2(y - centerY, x - centerX);
					final double lx = x + (r + LABEL_MARGIN) * Math.cos(angle);
					final double ly = y + (r + LABEL_MARGIN) * Math.sin(angle);
					final int tw = fm.stringWidth(label);
					g2.setColor(LABEL);
					g2.drawString(label, (int)(lx - tw / 2.0), (int)(ly + fm.getAscent() / 2.0));
				}
			}
		}
	}


	/* ======================================================================
	 *                          Formatting
	 * ====================================================================== */

	private static String shortLabel(final EventIndex.EventDatum e){
		final String type = e.type();
		final String date = (e.hasDate()? Integer.toString(jdnYear(e.date().jdn())): "?");
		return type + " " + date;
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
			case CENTURY -> (ymd[0] / 100 + 1) + "th century";
		};
	}

	private static String escape(final String s){
		if(s == null)
			return "";
		return s.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}

	private static int jdnYear(final long jdn){
		final long a = jdn + 32044L;
		final long b = (4L * a + 3L) / 146097L;
		final long c = a - (146097L * b) / 4L;
		final long d = (4L * c + 3L) / 1461L;
		final long e = c - (1461L * d) / 4L;
		final long m = (5L * e + 2L) / 153L;
		return (int)(100L * b + d - 4800L + m / 10L);
	}

	private static final String[] MONTH_NAMES = {
		"Jan", "Feb", "Mar", "Apr", "May", "Jun",
		"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
	};

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


	/* ======================================================================
	 *                          Bootstrap
	 * ====================================================================== */

	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final String content;
		try(final InputStream is = EventNetworkPanel.class.getResourceAsStream("/tests/TGMZ.flef")){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}
		final FLEFModel model = new FLEFParser().parse(content);

		SwingUtilities.invokeLater(() -> {
			final EventNetworkPanel panel = new EventNetworkPanel(model);
			final JFrame frame = new JFrame("Event Network");
			frame.add(panel);
			frame.setSize(1000, 800);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
