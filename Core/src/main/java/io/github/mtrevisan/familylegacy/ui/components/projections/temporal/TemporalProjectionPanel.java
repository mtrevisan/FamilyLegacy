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
package io.github.mtrevisan.familylegacy.ui.components.projections.temporal;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntityRef;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntityType;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionLayout;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionModel;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionRef;
import io.github.mtrevisan.familylegacy.temporal.TemporalRow;
import io.github.mtrevisan.familylegacy.tree.TreeChangeListener;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;


/*
Comportamento dell'interazione

Ctrl/Cmd + wheel → zoom ancorato al cursore.
Wheel senza modificatori → scroll verticale nativo.
Drag sinistro sul content → pan orizzontale.
Click su entry → selezione della riga.
Hover → tooltip con tipo, ruolo, span.
 */


/**
 * Timeline panel shared by the group views (group timeline, career of an individual, timeline of the jurisdictions): it draws the
 * {@link TemporalProjectionModel} given by {@link #withModelSource(Supplier)}.
 * <p>
 * Composes a {@link TemporalProjectionToolbar} on top of a
 * {@link JScrollPane} that hosts three synchronized canvases:
 * <ul>
 *   <li>the <b>content canvas</b> (viewport view) — draws bands, grid
 *       and entry bars;</li>
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

	/** Minimum height, in pixels, reserved for the content canvas. */
	private static final int MIN_CONTENT_HEIGHT = 100;


	private final FLEFModel flefModel;
	private final TemporalProjectionLayout layout;
	private final TemporalProjectionToolbar toolbar;
	private final TemporalZoomController zoomController;

	private final ContentCanvas contentCanvas;
	private final HeaderCanvas headerCanvas;
	private final AxisStripCanvas axisStripCanvas;

	/** Gives the model to show. */
	private Supplier<TemporalProjectionModel> modelSource = TemporalProjectionModel::empty;
	private TemporalProjectionModel model = TemporalProjectionModel.empty();
	/** The rows of the model by entity (looked for at every paint, for every row: a list made it quadratic). */
	private Map<TemporalEntityRef, TemporalRow> rowsByEntity = Map.of();
	private TemporalAxis axis = new TemporalAxis(null, null);

	private TemporalEntityRef selectedEntity;
	/** Told when an individual or a group is selected; {@code null} when nobody listens. */
	private Consumer<String> selectionCallback;


	public TemporalProjectionPanel(final FLEFModel flefModel){
		this.flefModel = flefModel;
		this.layout = new TemporalProjectionLayout();
		this.toolbar = new TemporalProjectionToolbar(new ToolbarListener());
		this.zoomController = new TemporalZoomController(() -> axis)
			.withListener(this::onAxisChanged);

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
	 * Asks the model source for the model to show and rebuilds the UI.
	 */
	public void loadProjection(){
		if(!SwingUtilities.isEventDispatchThread()){
			SwingUtilities.invokeLater(this::loadProjection);
			return;
		}

		model = modelSource.get();

		final Map<TemporalEntityRef, TemporalRow> index = new HashMap<>(model.rows().size() * 2);
		for(final TemporalRow row : model.rows())
			index.putIfAbsent(row.entity(), row);
		rowsByEntity = index;

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
	 * Makes the panel show the model given by the supplier. The supplier is called at every {@link #loadProjection()}, so that a refresh
	 * shows the current data.
	 *
	 * @param source	The supplier of the model to show, or {@code null} for an empty panel.
	 * @return	This panel.
	 */
	public TemporalProjectionPanel withModelSource(final Supplier<TemporalProjectionModel> source){
		modelSource = (source != null? source: TemporalProjectionModel::empty);

		return this;
	}

	/**
	 * Refreshes the projection after the underlying model has changed.
	 */
	public void refreshProjection(){
		loadProjection();
	}

	/**
	 * Returns the currently selected row entity, or {@code null}.
	 */
	public TemporalEntityRef getSelectedEntity(){
		return selectedEntity;
	}

	/**
	 * Registers a callback invoked with the id of an individual or a group when its row, or one of its entries, is selected.
	 *
	 * @param callback	The callback, {@code null} to remove it.
	 */
	public TemporalProjectionPanel withSelectionCallback(final Consumer<String> callback){
		selectionCallback = callback;

		return this;
	}

	private void select(final TemporalEntityRef entity){
		selectedEntity = entity;

		if(entity != null && selectionCallback != null
				&& (entity.type() == TemporalEntityType.INDIVIDUAL || entity.type() == TemporalEntityType.GROUP))
			selectionCallback.accept(entity.id());
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
			.setUnitIncrement(GUIHelper.SCROLLBAR_UNIT_INCREMENT);
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
					select(entryRef.rowEntity());

					repaint();
				}
				else if(ref instanceof TemporalProjectionRef.RowRef rowRef){
					select(rowRef.entity());

					repaint();
				}
				else if(ref instanceof TemporalProjectionRef.BandRef && selectedEntity != null){
					//the background is not a row
					selectedEntity = null;

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
					select(entity);

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
	 * Opens the edit dialog appropriate for the record referenced by the
	 * given projection element. The record is resolved from the entry, row
	 * or band carried by the reference; if no handler matches the record
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
		final RecordDialog dialog = handler.createEditDialog(parent, flefModel, record);
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
		if(ref instanceof TemporalProjectionRef.RowRef rowRef)
			return rowRef.entity().record();
		if(ref instanceof TemporalProjectionRef.BandRef bandRef)
			return bandRef.band().entity().record();
		return null;
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
		return TemporalInteractionHandler.hitTest(e.getX(), e.getY(), model, layout, axis, contentBounds);
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
	}


	/* ======================================================================
	 *                          Canvases
	 * ====================================================================== */

	/** Viewport view: draws bands, grid and entries. */
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
			TemporalContextRenderer.drawBands(g2, model.bands(), axis, bounds);

			// Vertical grid
			TemporalAxisRenderer.drawVerticalGrid(g2, axis, bounds);

			// Selection background for the selected row (behind the entries)
			if(selectedEntity != null){
				final TemporalProjectionLayout.RowLayout selectedRow = layout.findRow(selectedEntity);
				if(selectedRow != null)
					TemporalRowRenderer.drawRowSelectionBackground(g2, selectedRow, bounds, true);
			}

			// Entries per row: only the rows that the clip shows (a scroll repaints the viewport at every step)
			final Rectangle clip = g2.getClipBounds();
			for(final TemporalProjectionLayout.RowLayout rowLayout : layout.rows()){
				if(clip != null && (rowLayout.bottom() < clip.y || rowLayout.y() > clip.y + clip.height))
					continue;

				final TemporalRow row = findRow(rowLayout.entity());
				if(row == null || row.collapsed())
					continue;
				final boolean selected = (selectedEntity != null
					&& selectedEntity.equals(rowLayout.entity()));
				TemporalRowRenderer.drawRowTracks(g2, row, rowLayout, axis, bounds, selected);
			}
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
			return (findRowEntityAt(event.getY()) != null? I18N.t("text.tip.edit_double"): null);
		}

		@Override
		protected void paintComponent(final Graphics g){
			super.paintComponent(g);
			if(!(g instanceof Graphics2D g2))
				return;

			final Rectangle bounds = new Rectangle(0, 0, getWidth(), getHeight());
			final Rectangle clip = g2.getClipBounds();
			for(final TemporalProjectionLayout.RowLayout rowLayout : layout.rows()){
				if(clip != null && (rowLayout.bottom() < clip.y || rowLayout.y() > clip.y + clip.height))
					continue;

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
		return rowsByEntity.get(entity);
	}


}
