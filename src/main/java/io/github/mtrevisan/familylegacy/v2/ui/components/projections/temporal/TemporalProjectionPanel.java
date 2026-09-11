package io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeChangeListener;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;

import javax.swing.JFrame;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.Objects;


/*
Comportamento dell'interazione

Ctrl/Cmd + wheel → zoom ancorato al cursore.
Wheel senza modificatori → scroll verticale nativo.
Drag sinistro sul content → pan orizzontale.
Click su entry → selezione della riga.
Hover → tooltip con tipo, ruolo, span.
Toggle toolbar → refresh (ricostruzione del modello con il filtro).
 */


/**
 * Main panel of the General Temporal Projection.
 * <p>
 * Composes a {@link TemporalProjectionToolbar} on top of a
 * {@link JScrollPane} that hosts three synchronized canvases:
 * <ul>
 *   <li>the <b>content canvas</b> (viewport view) — draws bands, grid,
 *       entry bars, connection arcs and impact link connectors;</li>
 *   <li>the <b>header canvas</b> (row header view) — draws the row labels,
 *       kept horizontally aligned with the content canvas;</li>
 *   <li>the <b>axis strip canvas</b> (column header view) — draws the
 *       temporal axis, kept aligned with the content canvas.</li>
 * </ul>
 * Vertical scrolling is handled natively by the scroll pane; horizontal
 * navigation is handled by the {@link TemporalAxis} via the
 * {@link TemporalZoomController}, so there is no horizontal scrollbar.
 */
public final class TemporalProjectionPanel extends JPanel implements TreeChangeListener{

	@Serial
	private static final long serialVersionUID = 7192849102849102942L;


	/** Minimum height, in pixels, reserved for the content canvas. */
	private static final int MIN_CONTENT_HEIGHT = 100;


	private final FLEFModel flefModel;
	private final TemporalProjectionService service;
	private final TemporalProjectionLayout layout;
	private final TemporalProjectionToolbar toolbar;
	private final TemporalZoomController zoomController;

	private final ContentCanvas contentCanvas;
	private final HeaderCanvas headerCanvas;
	private final AxisStripCanvas axisStripCanvas;

	private TemporalProjectionModel model = TemporalProjectionModel.empty();
	private TemporalAxis axis = new TemporalAxis(null, null);

	private TemporalEntityRef selectedEntity;


	public TemporalProjectionPanel(final FLEFModel flefModel){
		this.flefModel = flefModel;
		this.service = new TemporalProjectionService(flefModel);
		this.layout = new TemporalProjectionLayout();
		this.toolbar = new TemporalProjectionToolbar(new ToolbarListener());
		this.zoomController = new TemporalZoomController(() -> axis).withListener(this::onAxisChanged);

		this.contentCanvas = new ContentCanvas();
		this.headerCanvas = new HeaderCanvas();
		this.axisStripCanvas = new AxisStripCanvas();

		JScrollPane scrollPane = buildScrollPane();

		installInteractions();

		setLayout(new BorderLayout());
		add(toolbar, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Builds the projection from the current FLEF model and rebuilds the UI.
	 */
	public void loadProjection(){
		if(!SwingUtilities.isEventDispatchThread()){
			SwingUtilities.invokeLater(this::loadProjection);
			return;
		}

		model = service.build();

		// Determine the entity filter from the toolbar.
		final TemporalProjectionToolbar.EntityFilter filter = toolbar.getEntityFilter();
		if(filter != TemporalProjectionToolbar.EntityFilter.ALL){
			// Rebuild with the entity filter applied.
			model = service.build(entity -> matchesFilter(entity, filter));
		}

		layout.compute(model);

		// Recreate the axis with the new domain, preserving the zoom level
		// and the visible window when possible.
		if(model.hasDomain()){
			final TemporalAxis previous = axis;
			axis = new TemporalAxis(model.domainStart(), model.domainEnd());
			if(previous.viewportWidth() > 0)
				axis.setViewportWidth(previous.viewportWidth());
			axis.fitToDomain();
		}
		else
			axis = new TemporalAxis(null, null);

		updatePreferredSizes();
		revalidate();
		repaint();
	}

	/**
	 * Refreshes the projection after the underlying model has changed.
	 */
	public void refreshProjection(){
		service.invalidate();
		loadProjection();
	}

	/**
	 * Returns the currently selected row entity, or {@code null}.
	 */
	public TemporalEntityRef getSelectedEntity(){
		return selectedEntity;
	}


	/* ======================================================================
	 *                          Composition
	 * ====================================================================== */

	private JScrollPane buildScrollPane(){
		final JScrollPane pane = new JScrollPane(contentCanvas,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		pane.setRowHeaderView(headerCanvas);
		pane.setColumnHeaderView(axisStripCanvas);
		pane.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, new JPanel());
		pane.getVerticalScrollBar()
			.setUnitIncrement(16);
		pane.getViewport()
			.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
		return pane;
	}

	private void installInteractions(){
		zoomController.installOn(contentCanvas);

		final MouseAdapter interactionAdapter = new MouseAdapter(){
			@Override
			public void mouseMoved(final MouseEvent e){
				updateTooltip(e);
			}

			@Override
			public void mouseExited(final MouseEvent e){
				contentCanvas.setToolTipText(null);
			}

			@Override
			public void mouseClicked(final MouseEvent e){
				if(!SwingUtilities.isLeftMouseButton(e))
					return;

				final TemporalProjectionRef ref = hitTest(e);
				if(ref == null){
					if(selectedEntity != null){
						selectedEntity = null;
						repaint();
					}
					return;
				}

				// Update selection based on the hit element.
				if(ref instanceof TemporalProjectionRef.EntryRef entryRef){
					selectedEntity = entryRef.rowEntity();
					repaint();
				}
				else if(ref instanceof TemporalProjectionRef.RowRef rowRef){
					selectedEntity = rowRef.entity();
					repaint();
				}

				// Double-click opens the edit dialog of the underlying record.
				if(e.getClickCount() == 2)
					openEditDialogFor(ref);
			}
		};
		contentCanvas.addMouseListener(interactionAdapter);
		contentCanvas.addMouseMotionListener(interactionAdapter);

		contentCanvas.addComponentListener(new ComponentAdapter(){
			@Override
			public void componentResized(final ComponentEvent e){
				axis.setViewportWidth(contentCanvas.getWidth());
				repaint();
			}
		});

		headerCanvas.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(!SwingUtilities.isLeftMouseButton(e))
					return;
				final TemporalEntityRef entity = findRowEntityAt(e.getY());
				if(entity == null)
					return;

				if(e.getClickCount() == 2)
					openEditDialogFor(new TemporalProjectionRef.RowRef(entity));
				else{
					selectedEntity = entity;
					repaint();
				}
			}
		});
	}

	/**
	 * Returns the entity whose row contains the given Y coordinate, or
	 * {@code null} if no row is hit. Coordinates are relative to the header
	 * canvas, which shares the same vertical coordinate system as the content
	 * canvas (both are inside the same scroll pane).
	 */
	private TemporalEntityRef findRowEntityAt(final int y){
		for(final TemporalProjectionLayout.RowLayout rowLayout : layout.rows())
			if(y >= rowLayout.y() && y < rowLayout.bottom())
				return rowLayout.entity();
		return null;
	}

	/**
	 * Opens the edit dialog appropriate for the given entity, and refreshes
	 * the projection when the dialog is saved.
	 */
	/**
	 * Opens the edit dialog appropriate for the record referenced by the
	 * given projection element. The record is resolved from the entry or
	 * connection carried by the reference; if no handler matches the record
	 * tag, the call is a no-op.
	 */
	private void openEditDialogFor(final TemporalProjectionRef ref){
		final FLEFRecord record = sourceRecordOf(ref);
		if(record == null)
			return;

		final RecordTypeHandler<?> handler = handlerForTag(record.getTag());
		if(handler == null)
			return;

		final Window parent = SwingUtilities.getWindowAncestor(this);
		final BaseRecordDialog dialog = handler.createEditDialog(parent, flefModel, record);
		dialog.setVisible(true);

		if(dialog.isSaved())
			refreshProjection();
	}

	/**
	 * Resolves the FLEF record backing the given projection element.
	 */
	private static FLEFRecord sourceRecordOf(final TemporalProjectionRef ref){
		if(ref instanceof TemporalProjectionRef.EntryRef entryRef)
			return entryRef.entry().sourceRecord();
		if(ref instanceof TemporalProjectionRef.ConnectionRef connectionRef)
			return connectionRef.connection().sourceRecord();
		if(ref instanceof TemporalProjectionRef.RowRef rowRef)
			return rowRef.entity().record();
		return null;
	}

	/**
	 * Returns the singleton handler for the given row entity type, or
	 * {@code null} if the type cannot be edited.
	 */
	private static RecordTypeHandler<?> handlerFor(final TemporalEntityType type){
		return switch(type){
			case INDIVIDUAL -> IndividualHandler.getInstance();
			case GROUP -> GroupHandler.getInstance();
			case PLACE -> PlaceHandler.getInstance();
			default -> null;
		};
	}

	/**
	 * Maps a FLEF record tag to the corresponding singleton handler, or
	 * {@code null} if the record type is not editable through this view.
	 */
	private static RecordTypeHandler<?> handlerForTag(final String tag){
		if(tag == null)
			return null;
		return HandlerRegistry.getHandler(tag);
	}

	private void updateTooltip(final MouseEvent e){
		final TemporalProjectionRef ref = hitTest(e);
		contentCanvas.setToolTipText(TemporalInteractionHandler.buildTooltip(ref));
	}

	private TemporalProjectionRef hitTest(final MouseEvent e){
		final Rectangle contentBounds = new Rectangle(0, 0, contentCanvas.getWidth(),
			contentCanvas.getHeight());
		return TemporalInteractionHandler.hitTest(e.getX(), e.getY(), model, layout, axis,
			contentBounds);
	}


	/* ======================================================================
	 *                          Layout
	 * ====================================================================== */

	private void updatePreferredSizes(){
		final int contentHeight = Math.max(MIN_CONTENT_HEIGHT, layout.totalHeight());
		contentCanvas.setPreferredSize(new Dimension(500, contentHeight));
		headerCanvas.setPreferredSize(new Dimension(TemporalProjectionLayout.ROW_HEADER_WIDTH,
			contentHeight));
		axisStripCanvas.setPreferredSize(new Dimension(500,
			TemporalProjectionRenderer.AXIS_STRIP_HEIGHT));
	}

	private void onAxisChanged(){
		contentCanvas.repaint();
		axisStripCanvas.repaint();
	}

	private static boolean matchesFilter(final TemporalEntityRef entity,
		final TemporalProjectionToolbar.EntityFilter filter){
		return switch(filter){
			case ALL -> true;
			case INDIVIDUALS -> entity.type() == TemporalEntityType.INDIVIDUAL;
			case GROUPS -> entity.type() == TemporalEntityType.GROUP;
			case PLACES -> entity.type() == TemporalEntityType.PLACE;
		};
	}


	/* ======================================================================
	 *                          TreeChangeListener
	 * ====================================================================== */

	@Override
	public void onTreeStructureChanged(final String rootIndividualId){
		SwingUtilities.invokeLater(this::refreshProjection);
	}


	/* ======================================================================
	 *                          Toolbar listener
	 * ====================================================================== */

	private final class ToolbarListener implements TemporalProjectionToolbar.Listener{
		@Override public void onZoomIn(){
			axis.zoomIn();
			onAxisChanged();
		}

		@Override public void onZoomOut(){
			axis.zoomOut();
			onAxisChanged();
		}

		@Override public void onFitToDomain(){
			axis.fitToDomain();
			onAxisChanged();
		}

		@Override public void onFiltersChanged(){
			refreshProjection();
		}
	}


	/* ======================================================================
	 *                          Canvases
	 * ====================================================================== */

	/** Viewport view: draws bands, grid, entries, connections and impacts. */
	private final class ContentCanvas extends JPanel{
		ContentCanvas(){
			setBackground(TemporalProjectionRenderer.COLOR_BACKGROUND);
			setToolTipText(null);
		}

		@Override
		protected void paintComponent(final Graphics g){
			super.paintComponent(g);
			if(!(g instanceof Graphics2D g2))
				return;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			final Rectangle bounds = new Rectangle(0, 0, getWidth(), getHeight());

			// Bands (background)
			if(toolbar.isShowBands())
				TemporalContextRenderer.drawBands(g2, model.bands(), axis, bounds);

			// Vertical grid
			TemporalAxisRenderer.drawVerticalGrid(g2, axis, bounds);

			// Selection background for the selected row (behind the entries)
			if(selectedEntity != null){
				final TemporalProjectionLayout.RowLayout selectedRow = layout.findRow(selectedEntity);
				if(selectedRow != null)
					TemporalRowRenderer.drawRowSelectionBackground(g2, selectedRow, bounds, true);
			}

			// Entries per row
			for(final TemporalProjectionLayout.RowLayout rowLayout : layout.rows()){
				final TemporalRow row = findRow(rowLayout.entity());
				if(row == null || row.collapsed())
					continue;
				final boolean selected = (selectedEntity != null
					&& selectedEntity.equals(rowLayout.entity()));
				TemporalRowRenderer.drawRowTracks(g2, row, rowLayout, axis, bounds, selected);
			}

			// Connections
			if(toolbar.isShowConnections())
				TemporalConnectionRenderer.drawConnections(g2, model.connections(), layout, axis, bounds);

			// Impact links
			if(toolbar.isShowImpactLinks())
				TemporalContextRenderer.drawImpactLinks(g2, model.impactLinks(), layout, axis, bounds);
		}

		@Override
		public String getToolTipText(final MouseEvent event){
			final TemporalProjectionRef ref = hitTest(event);
			return TemporalInteractionHandler.buildTooltip(ref);
		}
	}

	/** Row header view: draws the row labels and header backgrounds. */
	private final class HeaderCanvas extends JPanel{
		HeaderCanvas(){
			setBackground(TemporalRowRenderer.COLOR_HEADER_BACKGROUND);
		}

		@Override
		public String getToolTipText(final MouseEvent event){
			return (findRowEntityAt(event.getY()) != null? "Double-click to edit": null);
		}

		@Override
		protected void paintComponent(final Graphics g){
			super.paintComponent(g);
			if(!(g instanceof Graphics2D g2))
				return;

			final Rectangle bounds = new Rectangle(0, 0, getWidth(), getHeight());
			for(final TemporalProjectionLayout.RowLayout rowLayout : layout.rows()){
				final TemporalRow row = findRow(rowLayout.entity());
				if(row == null)
					continue;
				final boolean selected = (selectedEntity != null
					&& selectedEntity.equals(rowLayout.entity()));
				TemporalRowRenderer.drawRowHeader(g2, row, rowLayout, bounds, selected);
			}
		}
	}

	/** Column header view: draws the axis strip across the temporal content width. */
	private final class AxisStripCanvas extends JPanel{
		AxisStripCanvas(){
			setBackground(TemporalAxisRenderer.COLOR_STRIP_BACKGROUND);
		}

		@Override
		protected void paintComponent(final Graphics g){
			super.paintComponent(g);
			if(!(g instanceof Graphics2D g2))
				return;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			final Rectangle bounds = new Rectangle(0, 0, getWidth(), getHeight());
			TemporalAxisRenderer.drawAxisStrip(g2, axis, bounds, bounds);
		}
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private TemporalRow findRow(final TemporalEntityRef entity){
		for(final TemporalRow row : model.rows())
			if(row.entity().equals(entity))
				return row;
		return null;
	}


	/* ======================================================================
	 *                          Bootstrap
	 * ====================================================================== */

	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final String modelUri = "/tests/TGMZ.flef";

		final String content;
		try(final InputStream is = TemporalProjectionPanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFModel model = new FLEFParser().parse(content);

		SwingUtilities.invokeLater(() -> {
			final TemporalProjectionPanel panel = new TemporalProjectionPanel(model);
			panel.loadProjection();

			final JFrame frame = new JFrame("General Temporal Projection");
			frame.setLayout(new BorderLayout());
			frame.add(panel, BorderLayout.CENTER);
			frame.setSize(1200, 700);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
