package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeChangeListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Main panel of the geographic projection.
 * <p>
 * Composes a {@link GeoMapToolbar} on top of a custom canvas. The canvas
 * draws the map through {@link GeoMapRenderer}, handles hit-testing and
 * tooltips through {@link GeoMapInteractionHandler}, and delegates pan and
 * zoom to {@link GeoMapController}.
 */
public final class GeoMapPanel extends JPanel implements TreeChangeListener{

	private static final long serialVersionUID = 7192849102849102944L;


	private final FLEFModel flefModel;
	private final GeoMapService service;
	private final GeoMapToolbar toolbar;
	private final GeoMapController controller;

	private final MapCanvas canvas;

	private GeoFilters filters = GeoFilters.all();
	private GeoMapModel model = GeoMapModel.empty();
	private GeoMapViewport viewport = new GeoMapViewport(GeoProjectionType.MERCATOR);
	private GeoLayout layout = GeoLayout.compute(model, viewport.createProjection());

	private GeoPlaceRef selectedPlace;
	private GeoEventRef selectedEvent;


	public GeoMapPanel(final FLEFModel flefModel){
		this.flefModel = flefModel;
		this.service = new GeoMapService(flefModel);
		this.toolbar = new GeoMapToolbar(new ToolbarListener());
		this.controller = new GeoMapController(() -> viewport).withListener(this::onViewportChanged);

		this.canvas = new MapCanvas();

		installInteractions();

		setLayout(new BorderLayout());
		add(toolbar, BorderLayout.NORTH);
		add(canvas, BorderLayout.CENTER);
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	public void loadMap(){
		if(!SwingUtilities.isEventDispatchThread()){
			SwingUtilities.invokeLater(this::loadMap);
			return;
		}

		model = service.build(filters);

		if(model.hasBounds())
			viewport.fitTo(model.bounds(), 0.5);

		recomputeLayout();
		updateFocusLabel();
		canvas.repaint();
	}

	public void refreshMap(){
		service.invalidate();
		loadMap();
	}

	public void setFocus(final String entityId){
		filters = filters.withFocus(entityId);
		refreshMap();
	}

	@Override
	public void onTreeStructureChanged(final String rootEntityId){
		SwingUtilities.invokeLater(this::refreshMap);
	}


	/* ======================================================================
	 *                          Composition
	 * ====================================================================== */

	private void installInteractions(){
		controller.installOn(canvas);

		final MouseAdapter adapter = new MouseAdapter(){
			@Override public void mouseClicked(final MouseEvent e){
				if(!SwingUtilities.isLeftMouseButton(e))
					return;
				final Object hit = GeoMapInteractionHandler.hitTest(e.getPoint(), layout);
				if(hit instanceof GeoPlaceRef place){
					selectedPlace = place;
					selectedEvent = null;
					canvas.repaint();
					if(e.getClickCount() == 2)
						openEditDialog(place.entity()
							.record());
				}
				else if(hit instanceof GeoEventRef event){
					selectedEvent = event;
					selectedPlace = null;
					canvas.repaint();
					if(e.getClickCount() == 2)
						openEditDialog(event.eventRecord());
				}
				else{
					selectedPlace = null;
					selectedEvent = null;
					canvas.repaint();
				}
			}

			@Override public void mouseMoved(final MouseEvent e){
				canvas.setToolTipText(buildTooltip(e.getPoint()));
			}

			@Override public void mouseExited(final MouseEvent e){
				canvas.setToolTipText(null);
			}
		};
		canvas.addMouseListener(adapter);
		canvas.addMouseMotionListener(adapter);

		canvas.addComponentListener(new ComponentAdapter(){
			@Override public void componentResized(final ComponentEvent e){
				viewport.setPixelSize(canvas.getWidth(), canvas.getHeight());
				recomputeLayout();
				canvas.repaint();
			}
		});
	}

	private String buildTooltip(final Point point){
		final Object hit = GeoMapInteractionHandler.hitTest(point, layout);
		return GeoMapInteractionHandler.buildTooltip(hit);
	}

	private void onViewportChanged(){
		recomputeLayout();
		canvas.repaint();
	}

	private void recomputeLayout(){
		viewport.setPixelSize(canvas.getWidth(), canvas.getHeight());
		final GeoProjection projection = viewport.createProjection();
		layout = GeoLayout.compute(model, projection);
	}

	private void updateFocusLabel(){
		if(!filters.hasFocus()){
			toolbar.setFocusLabel("(none)");
			return;
		}
		final FLEFRecord record = flefModel.getRecordById(filters.focusEntityId());
		if(record == null){
			toolbar.setFocusLabel("(none)");
			return;
		}
		final String label = (record.getId() != null? record.getId(): "?");
		toolbar.setFocusLabel(label);
	}


	/* ======================================================================
	 *                          Edit dialog
	 * ====================================================================== */

	private void openEditDialog(final FLEFRecord record){
		if(record == null)
			return;
		final RecordTypeHandler<?> handler = handlerForTag(record.getTag());
		if(handler == null)
			return;

		final Window parent = SwingUtilities.getWindowAncestor(this);
		final BaseRecordDialog dialog = handler.createEditDialog(parent, flefModel, record);
		dialog.setVisible(true);
		if(dialog.isSaved())
			refreshMap();
	}

	private static RecordTypeHandler<?> handlerForTag(final String tag){
		if(tag == null)
			return null;
		return switch(tag.toLowerCase()){
			case "individual" -> IndividualHandler.getInstance();
			case "group"      -> GroupHandler.getInstance();
			case "place"      -> PlaceHandler.getInstance();
			default            -> null;
		};
	}


	/* ======================================================================
	 *                          Toolbar listener
	 * ====================================================================== */

	private final class ToolbarListener implements GeoMapToolbar.Listener{
		@Override public void onProjectionTypeChanged(final GeoProjectionType type){
			if(type == null)
				return;
			viewport.setProjectionType(type);
			recomputeLayout();
			canvas.repaint();
		}

		@Override public void onZoomIn(){
			viewport.zoomIn();
			onViewportChanged();
		}

		@Override public void onZoomOut(){
			viewport.zoomOut();
			onViewportChanged();
		}

		@Override public void onFitRequested(){
			viewport.fitTo(model.bounds(), 0.5);
			onViewportChanged();
		}

		@Override public void onPickFocus(){
			pickFocus();
		}

		@Override public void onFiltersChanged(){
			// The individual show-* checkboxes only affect the rendering,
			// not the model, so no rebuild is needed: just repaint.
			canvas.repaint();
		}
	}


	private void pickFocus(){
		final FLEFRecord[] result = {null};
		final Window parent = SwingUtilities.getWindowAncestor(this);
		@SuppressWarnings("unchecked")
		final RecordSelectionDialog dialog = RecordSelectionDialog.createWithAllowRecordCreation(
			parent, flefModel,
			(record, handler) -> result[0] = record,
			IndividualHandler.class);
		dialog.setVisible(true);
		if(result[0] != null)
			setFocus(result[0].getId());
	}


	/* ======================================================================
	 *                          Canvas
	 * ====================================================================== */

	private final class MapCanvas extends JPanel{
		MapCanvas(){
			setBackground(GeoMapRenderer.COLOR_BACKGROUND);
			setToolTipText(null);
		}

		@Override protected void paintComponent(final Graphics g){
			super.paintComponent(g);
			if(!(g instanceof Graphics2D g2))
				return;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

			final Rectangle bounds = new Rectangle(0, 0, getWidth(), getHeight());
			final GeoProjection projection = viewport.createProjection();

			if(toolbar.isShowGraticule())
				GeoGraticuleRenderer.draw(g2, projection, bounds);

			final GeoMapModel effective = filterModel();
			GeoMapRenderer.draw(g2, effective, layout, projection, bounds,
				selectedPlace, selectedEvent, viewport.zoomLevel());
		}

		@Override public String getToolTipText(final MouseEvent event){
			return buildTooltip(event.getPoint());
		}
	}


	private GeoMapModel filterModel(){
		if(toolbar.isShowPlaces() && toolbar.isShowEvents() && toolbar.isShowRoutes())
			return model;

		final java.util.List<GeoPlaceRef> places = (toolbar.isShowPlaces()
			? model.places(): java.util.List.of());
		final java.util.List<GeoEventRef> events = (toolbar.isShowEvents()
			? model.events(): java.util.List.of());
		final java.util.List<GeoMigrationRoute> routes = (toolbar.isShowRoutes()
			? model.routes(): java.util.List.of());
		return new GeoMapModel(places, events, routes, model.bounds(), model.snapshotYear());
	}

	// Reserved for the future graticule toggle.
	static boolean isGraticuleEnabled(final GeoMapToolbar toolbar){
		return toolbar.isShowGraticule();
	}

	// Reserved for direct programmatic panning.
	void panView(final int dx, final int dy){
		controller.panBy(dx, dy);
	}

	// Reserved for tests.
	GeoLayout currentLayout(){
		return layout;
	}

	// Reserved for tests.
	GeoMapViewport currentViewport(){
		return viewport;
	}

	// Reserved for future selection sync with other views.
	Point2D screenPointOfPlace(final GeoPlaceRef place){
		return GeoMapInteractionHandler.screenPointOf(place, layout);
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
		try(final InputStream is = GeoMapPanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}
		final FLEFModel model = new FLEFParser().parse(content);

		SwingUtilities.invokeLater(() -> {
			final GeoMapPanel panel = new GeoMapPanel(model);
			panel.loadMap();

			final JFrame frame = new JFrame("Spatial Map");
			frame.setLayout(new BorderLayout());
			frame.add(panel, BorderLayout.CENTER);
			frame.setSize(1200, 800);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
