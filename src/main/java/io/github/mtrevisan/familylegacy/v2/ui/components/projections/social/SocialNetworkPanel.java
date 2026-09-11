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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.social;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeChangeListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.CalendarConverter;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.DatePrecision;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.NormalizedDate;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalEntityRef;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


/**
 * Main panel of the Social Network view.
 * <p>
 * Composes a {@link SocialNetworkToolbar} on top of a scrollable canvas
 * that renders the graph. The panel owns the service, the current graph,
 * the current layout and the interaction state (selected node, highlighted
 * path). It implements {@link TreeChangeListener} so that external model
 * mutations can trigger a refresh.
 * <p>
 * The canvas supports:
 * <ul>
 *   <li>horizontal and vertical scroll through the enclosing
 *       {@link JScrollPane};</li>
 *   <li>panning with left-button drag;</li>
 *   <li>zooming with Ctrl/Cmd + wheel, anchored at the cursor;</li>
 *   <li>node selection on click, edit dialog on double-click.</li>
 * </ul>
 * Zoom and pan are applied through an {@link AffineTransform} layered on
 * top of the layout coordinates, so the layout is never recomputed during
 * navigation.
 */
public final class SocialNetworkPanel extends JPanel implements TreeChangeListener{

	private static final long serialVersionUID = 7192849102849102943L;


	private static final double MIN_SCALE = 0.25;
	private static final double MAX_SCALE = 3.;
	private static final double ZOOM_STEP = 1.15;
	private static final int DRAG_DEAD_ZONE_PX = 3;


	private final FLEFModel model;
	private final SocialNetworkService service;
	private final SocialNetworkToolbar toolbar;

	private final JScrollPane scrollPane;
	private final NetworkCanvas canvas;

	private String focusId;
	private SocialFilters filters = SocialFilters.all(1);
	private SocialLayoutMode layoutMode = SocialLayoutMode.CONCENTRIC;

	private SocialGraph graph = SocialGraph.empty(SocialFilters.all(0));
	private SocialLayout layout = SocialLayout.compute(graph, SocialLayoutMode.CONCENTRIC);

	private TemporalEntityRef selectedEntity;
	private SocialPath highlightedPath;
	private Set<SocialEdgeRef> highlightedEdges = Collections.emptySet();

	// View transform.
	private double scale = 1.;
	private double offsetX = 0.;
	private double offsetY = 0.;

	// Drag state.
	private java.awt.Point dragAnchor;


	public SocialNetworkPanel(final FLEFModel model){
		this.model = model;
		this.service = new SocialNetworkService(model);
		this.toolbar = new SocialNetworkToolbar(new ToolbarListener());

		this.canvas = new NetworkCanvas();
		this.scrollPane = buildScrollPane();

		installInteractions();

		setLayout(new BorderLayout());
		add(toolbar, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Sets the focus entity and rebuilds the network around it.
	 *
	 * @param entityId the id of the entity to use as focus; may be
	 *                 {@code null} to clear the view
	 */
	public void setFocus(final String entityId){
		this.focusId = entityId;
		rebuildNetwork();
	}

	/**
	 * Rebuilds the network after an external change to the FLEF model.
	 */
	public void refreshNetwork(){
		service.invalidate();
		rebuildNetwork();
	}


	@Override
	public void onTreeStructureChanged(final String rootEntityId){
		SwingUtilities.invokeLater(this::refreshNetwork);
	}


	/* ======================================================================
	 *                          Composition
	 * ====================================================================== */

	private JScrollPane buildScrollPane(){
		final JScrollPane pane = new JScrollPane(canvas,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		pane.getViewport()
			.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
		pane.getVerticalScrollBar()
			.setUnitIncrement(16);
		pane.getHorizontalScrollBar()
			.setUnitIncrement(16);
		return pane;
	}

	private void installInteractions(){
		final MouseAdapter adapter = new MouseAdapter(){
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
				// Pan by adjusting the scroll position through the transform.
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
				if(!SwingUtilities.isLeftMouseButton(e))
					return;
				final SocialNodeRef node = hitTest(e);
				if(node == null){
					selectedEntity = null;
					canvas.repaint();
					return;
				}
				selectedEntity = node.entity();
				canvas.repaint();
				if(e.getClickCount() == 2)
					openEditDialog(node);
			}

			@Override
			public void mouseMoved(final MouseEvent e){
				canvas.setToolTipText(buildTooltip(e));
			}

			@Override
			public void mouseExited(final MouseEvent e){
				canvas.setToolTipText(null);
			}
		};
		canvas.addMouseListener(adapter);
		canvas.addMouseMotionListener(adapter);

		canvas.addMouseWheelListener(this::onMouseWheel);

		canvas.addComponentListener(new ComponentAdapter(){
			@Override
			public void componentResized(final ComponentEvent e){
				// The canvas size follows the layout and the transform, so
				// resizing the viewport only repaints.
				canvas.repaint();
			}
		});
	}

	private void onMouseWheel(final MouseWheelEvent e){
		if(!e.isControlDown() && !e.isMetaDown())
			return;
		e.consume();

		final int rotations = e.getWheelRotation();
		if(rotations == 0)
			return;

		final double factor = (rotations < 0? ZOOM_STEP: 1. / ZOOM_STEP);
		final double newScale = Math.clamp(scale * factor, MIN_SCALE, MAX_SCALE);
		if(newScale == scale)
			return;

		// Anchor the zoom at the cursor.
		final int cx = e.getX();
		final int cy = e.getY();
		final double layoutX = (cx - offsetX) / scale;
		final double layoutY = (cy - offsetY) / scale;

		scale = newScale;
		offsetX = cx - layoutX * scale;
		offsetY = cy - layoutY * scale;
		canvas.repaint();
	}

	private SocialNodeRef hitTest(final MouseEvent e){
		if(layout.isEmpty())
			return null;
		final int layoutX = (int)Math.round((e.getX() - offsetX) / scale);
		final int layoutY = (int)Math.round((e.getY() - offsetY) / scale);
		return SocialNetworkInteractionHandler.hitTest(layoutX, layoutY, layout);
	}

	private String buildTooltip(final MouseEvent e){
		final SocialNodeRef node = hitTest(e);
		return (node != null? SocialNetworkInteractionHandler.buildTooltip(graph, node): null);
	}


	/* ======================================================================
	 *                          Network rebuild
	 * ====================================================================== */

	private void rebuildNetwork(){
		if(!SwingUtilities.isEventDispatchThread()){
			SwingUtilities.invokeLater(this::rebuildNetwork);
			return;
		}

		graph = (focusId != null? service.build(focusId, filters): SocialGraph.empty(filters));
		layout = SocialLayout.compute(graph, layoutMode);

		// Drop any highlight that no longer belongs to the graph.
		highlightedPath = null;
		highlightedEdges = Collections.emptySet();

		// Keep the toolbar in sync with the current filter state.
		if(filters.hasTemporalWindow())
			toolbar.setTemporalWindow(
				filters.minDate() != null? fromNormalizedDate(filters.minDate()): null,
				filters.maxDate() != null? fromNormalizedDate(filters.maxDate()): null);
		else
			toolbar.clearTemporalWindow();

		updateCanvasSize();
		updateFocusLabel();
		canvas.repaint();
	}

	/**
	 * Converts a {@link NormalizedDate} back to a {@link Date} for display in
	 * the toolbar. The conversion uses the Gregorian calendar and truncates to
	 * day granularity.
	 *
	 * @param normalized the normalized date (must not be {@code null})
	 * @return the equivalent {@link Date}
	 */
	private static Date fromNormalizedDate(final NormalizedDate normalized){
		// Reverse Gregorian conversion (Fliegel-Van Flandern).
		final long jdn = normalized.jdn();
		final long a = jdn + 32044L;
		final long b = (4L * a + 3L) / 146097L;
		final long c = a - (146097L * b) / 4L;
		final long d = (4L * c + 3L) / 1461L;
		final long e = c - (1461L * d) / 4L;
		final long m = (5L * e + 2L) / 153L;
		final int day = (int)(e - (153L * m + 2L) / 5L + 1L);
		final int month = (int)(m + 3L - 12L * (m / 10L));
		final int year = (int)(100L * b + d - 4800L + m / 10L);

		final Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(year, month - 1, day);
		return cal.getTime();
	}

	private void updateCanvasSize(){
		final Rectangle content = layout.contentBounds();
		final int w = Math.max(1, (int)Math.ceil(content.width * scale));
		final int h = Math.max(1, (int)Math.ceil(content.height * scale));
		canvas.setPreferredSize(new Dimension(w, h));
		canvas.revalidate();
	}

	private void updateFocusLabel(){
		if(graph.isEmpty() || graph.center() == null){
			toolbar.setFocusLabel("(none)");
			return;
		}
		toolbar.setFocusLabel(graph.center()
			.entity()
			.displayLabel());
	}

	private void fitToView(){
		scale = 1.;
		offsetX = 0.;
		offsetY = 0.;
		updateCanvasSize();
		canvas.repaint();
	}


	/* ======================================================================
	 *                          Edit dialog
	 * ====================================================================== */

	private void openEditDialog(final SocialNodeRef node){
		if(node == null)
			return;
		final FLEFRecord record = node.entity()
			.record();
		if(record == null)
			return;
		final RecordTypeHandler<?> handler = handlerForTag(record.getTag());
		if(handler == null)
			return;

		final Window parent = SwingUtilities.getWindowAncestor(this);
		final BaseRecordDialog dialog = handler.createEditDialog(parent, model, record);
		dialog.setVisible(true);
		if(dialog.isSaved())
			refreshNetwork();
	}

	private static RecordTypeHandler<?> handlerForTag(final String tag){
		if(tag == null)
			return null;
		return switch(tag.toLowerCase()){
			case "individual" -> IndividualHandler.getInstance();
			case "group" -> GroupHandler.getInstance();
			default -> null;
		};
	}


	/* ======================================================================
	 *                          Toolbar listener
	 * ====================================================================== */

	private final class ToolbarListener implements SocialNetworkToolbar.Listener{
		@Override
		public void onPickFocus(){
			pickFocus();
		}

		@Override
		public void onMaxDegreeChanged(final int maxDegree){
			filters = filters.withMaxDegree(maxDegree);
			rebuildNetwork();
		}

		@Override
		public void onLayoutModeChanged(final SocialLayoutMode mode){
			if(mode == null || mode == layoutMode)
				return;
			layoutMode = mode;
			layout = SocialLayout.compute(graph, layoutMode);
			updateCanvasSize();
			canvas.repaint();
		}

		@Override
		public void onIncludeInactiveChanged(final boolean value){
			filters = filters.withIncludeInactive(value);
			rebuildNetwork();
		}

		@Override
		public void onIncludeEmptyRoleChanged(final boolean value){
			filters = filters.withIncludeEmptyRole(value);
			rebuildNetwork();
		}

		@Override
		public void onCategoriesRequested(){
			openCategoriesDialog();
		}

		@Override
		public void onPathFinderRequested(){
			openPathFinderDialog();
		}

		@Override
		public void onFitRequested(){
			fitToView();
		}

		@Override
		public void onTemporalWindowChanged(final boolean enabled, final Date from, final Date to){
			if(!enabled){
				filters = filters.withTemporalWindow(null, null);
				rebuildNetwork();
				return;
			}

			final NormalizedDate minDate = (from != null? toNormalizedDate(from): null);
			final NormalizedDate maxDate = (to != null? toNormalizedDate(to): null);
			filters = filters.withTemporalWindow(minDate, maxDate);
			rebuildNetwork();
		}
	}

	/**
	 * Converts a {@link Date} to a {@link NormalizedDate} in the Gregorian
	 * calendar, at {@link DatePrecision#DAY} granularity.
	 * <p>
	 * The conversion uses {@link Calendar} to extract the local year, month
	 * and day, then relies on {@link CalendarConverter} to compute the Julian
	 * Day Number. The time component is discarded: the temporal filter
	 * operates at day granularity.
	 */
	private static NormalizedDate toNormalizedDate(final Date date){
		final Calendar c = Calendar.getInstance();
		c.setTime(date);
		final int year = c.get(Calendar.YEAR);
		final int month = c.get(Calendar.MONTH) + 1;
		final int day = c.get(Calendar.DAY_OF_MONTH);
		final long jdn = CalendarConverter.gregorianToJdn(year, month, day);
		return NormalizedDate.exact(jdn, DatePrecision.DAY);
	}

	/* ======================================================================
	 *                          Dialog helpers
	 * ====================================================================== */

	private void pickFocus(){
		final FLEFRecord[] result = {null};
		final Window parent = SwingUtilities.getWindowAncestor(this);
		@SuppressWarnings("unchecked") final RecordSelectionDialog dialog = RecordSelectionDialog.createWithAllowRecordCreation(
			parent, model,
			(record, handler) -> result[0] = record,
			IndividualHandler.class);
		dialog.setVisible(true);
		if(result[0] != null)
			setFocus(result[0].getId());
	}

	private void openCategoriesDialog(){
		final Window parent = SwingUtilities.getWindowAncestor(this);
		final Set<SocialRelationCategory> current = new HashSet<>(filters.categories());
		final Set<SocialRelationCategory> picked = SocialCategorySelectionDialog.show(parent, current);
		if(picked == null)
			return;
		filters = filters.withCategories(picked.isEmpty()? null: picked);
		rebuildNetwork();
	}

	private void openPathFinderDialog(){
		final Window parent = SwingUtilities.getWindowAncestor(this);
		final TemporalEntityRef entityFrom = (graph.center() != null? graph.center().entity(): null);
		final TemporalEntityRef entityTo = selectedEntity;
		final FLEFRecord from = (entityFrom != null? model.getRecordById(entityFrom.id()): null);
		final FLEFRecord to = (entityTo != null? model.getRecordById(entityTo.id()): null);
		final SocialNetworkPathFinderDialog dialog = new SocialNetworkPathFinderDialog(parent, model, from, to);
		dialog.setVisible(true);

		if(!dialog.isAccepted())
			return;

		final String fromId = dialog.getFromEntityId();
		final String toId = dialog.getToEntityId();

		final SocialPath path = SocialPathFinder.findPath(graph, fromId, toId);
		if(path == null){
			highlightedPath = null;
			highlightedEdges = Collections.emptySet();
			JOptionPane.showMessageDialog(this,
				"No social path found between the chosen entities.",
				"Path not found", JOptionPane.INFORMATION_MESSAGE);
		}
		else{
			highlightedPath = path;
			highlightedEdges = new HashSet<>(path.edges());
		}
		canvas.repaint();
	}


	/* ======================================================================
	 *                          Canvas
	 * ====================================================================== */

	private final class NetworkCanvas extends JPanel{
		NetworkCanvas(){
			setBackground(SocialNetworkRenderer.COLOR_BACKGROUND);
			setToolTipText(null);
		}

		@Override
		protected void paintComponent(final Graphics g){
			super.paintComponent(g);
			if(!(g instanceof Graphics2D g2))
				return;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

			final AffineTransform original = g2.getTransform();
			try{
				g2.translate(offsetX, offsetY);
				g2.scale(scale, scale);

				final Rectangle content = layout.contentBounds();
				final Rectangle scaled = new Rectangle(content.x, content.y, content.width, content.height);
				SocialNetworkRenderer.draw(g2, graph, layout, scaled, selectedEntity, highlightedEdges,
					highlightedPath);
			}
			finally{
				g2.setTransform(original);
			}
		}

		@Override
		public String getToolTipText(final MouseEvent event){
			return buildTooltip(event);
		}
	}


	/* ======================================================================
	 *                          Bootstrap
	 * ====================================================================== */

	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){
		}

		final String modelUri = "/tests/TGMZ.flef";
		final String content;
		try(final InputStream is = SocialNetworkPanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFModel model = new FLEFParser().parse(content);

		SwingUtilities.invokeLater(() -> {
			final SocialNetworkPanel panel = new SocialNetworkPanel(model);
			panel.setFocus("I1");

			final JFrame frame = new JFrame("Social Network");
			frame.setLayout(new BorderLayout());
			frame.add(panel, BorderLayout.CENTER);
			frame.setSize(1200, 760);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
