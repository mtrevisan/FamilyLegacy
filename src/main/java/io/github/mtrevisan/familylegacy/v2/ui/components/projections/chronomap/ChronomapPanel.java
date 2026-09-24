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

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.cache.FileBasedLocalCache;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.MouseInputListener;
import java.awt.BorderLayout;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * Panel that shows a map of the places where the events of the visible
 * individuals happened, filtered by a zoomable timeline.
 * <p>
 * The temporal domain spans the whole FLEF file, from the earliest to the
 * latest recorded date. There is no playback: the user scrubs the
 * timeline manually, and the markers interpolate their position between
 * consecutive events.
 * <p>
 * <b>Layers.</b> The overlay is composed of independent layers, each
 * one toggleable from the "Layers" menu: markers, trails, place
 * hierarchy, image overlays. The manager paints them back-to-front.
 * <p>
 * <b>Event filter.</b> The "Events" menu lists the distinct event types
 * in the model. Disabling a type hides both its marker and its
 * contribution to the trails.
 */
public class ChronomapPanel extends JPanel{

	private static final Logger LOGGER = LoggerFactory.getLogger(ChronomapPanel.class);


	@Serial
	private static final long serialVersionUID = 9038174928374910231L;


	/** Initial map center (Italy). */
	private static final GeoPosition INITIAL_CENTER = new GeoPosition(42.5, 12.5);
	/** Initial zoom level. */
	private static final int INITIAL_ZOOM = 4;


	private final ChronomapIndex index;
	private final PlaceCoordinateResolver placeResolver;

	private final JXMapViewer mapViewer = new JXMapViewer();
	private final ChronomapTimeline timeline = new ChronomapTimeline();

	private final ChronomapLayerManager layerManager = new ChronomapLayerManager();
	private final ChronomapOverlayPainter markerLayer;
	private final ChronomapTrailLayer trailLayer;
	private final ChronomapPlaceHierarchyLayer hierarchyLayer;
	private final ChronomapImageOverlayLayer imageLayer;

	/** Checkboxes of the "Events" menu, keyed by event type. */
	private final Map<String, JCheckBoxMenuItem> eventTypeItems = new LinkedHashMap<>();


	public static ChronomapPanel create(final FLEFModel model){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");

		// Disk cache for geocoded coordinates, so Nominatim is not queried
		// again on subsequent runs.
		final Path cacheFile = Path.of(System.getProperty("user.home"),
			".familylegacy", "geocoding.properties");
		final PlaceCoordinateResolver placeResolver = new PlaceCoordinateResolver(model, cacheFile);
		final ChronomapIndex index = new ChronomapIndex(model, placeResolver);
		return create(model, index, placeResolver);
	}

	public static ChronomapPanel create(final FLEFModel model, final ChronomapIndex index,
			final PlaceCoordinateResolver placeResolver){
		return new ChronomapPanel(model, index, placeResolver);
	}


	private ChronomapPanel(final FLEFModel model, final ChronomapIndex index,
			final PlaceCoordinateResolver placeResolver){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");

		this.placeResolver = placeResolver;
		this.index = index;
		this.markerLayer = new ChronomapOverlayPainter(model, index);
		this.trailLayer = new ChronomapTrailLayer(model, index);
		this.hierarchyLayer = new ChronomapPlaceHierarchyLayer(model, placeResolver);
		this.imageLayer = new ChronomapImageOverlayLayer();

		// Insertion order determines painting order: hierarchy first
		// (background), trails next, markers on top, image overlays last.
		layerManager.addLayer(hierarchyLayer);
		layerManager.addLayer(trailLayer);
		layerManager.addLayer(markerLayer);
		layerManager.addLayer(imageLayer);

		buildUI();
		configureTileFactory();
		configureInteraction();

		final long[] range = index.computeGlobalDateRange();
		if(range != null && range[0] < range[1]){
			timeline.setDomain(range[0], range[1]);
			timeline.setCurrentTime(range[0]);
			markerLayer.setCurrentTime(range[0]);
			trailLayer.setCurrentTime(range[0]);
		}

		timeline.withTimeListener(jdn -> {
			markerLayer.setCurrentTime(jdn);
			trailLayer.setCurrentTime(jdn);
			mapViewer.repaint();
		});

		// Background geocoding: resolves the places that the hierarchy walk
		// could not locate. Runs off the EDT; the index is rebuilt when done.
		new SwingWorker<Integer, String>(){
			@Override
			protected Integer doInBackground(){
				return placeResolver.geocodeMissingPlaces(this::publish);
			}

			@Override
			protected void done(){
				try{
					final int resolved = get();
					if(resolved > 0){
						index.rebuild();
						hierarchyLayer.rebuild();
						mapViewer.repaint();
					}
				}
				catch(final Exception e){
					LOGGER.debug("Geocoding worker failed", e);
				}
			}
		}.execute();
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/** Sets the individuals whose markers are drawn on the map. */
	public void setIndividuals(final Collection<String> ids){
		final List<String> visible = (ids != null? List.copyOf(ids): List.of());
		markerLayer.setVisibleIndividuals(visible);
		mapViewer.repaint();

		resetView();
	}

	/**
	 * Sets the individuals whose trails are drawn on the map. Usually a
	 * single individual, but multiple trails are supported and each is
	 * drawn in its own color.
	 */
	public void setTrailIndividuals(final Collection<String> ids){
		final List<String> trail = (ids != null? List.copyOf(ids): List.of());
		trailLayer.setTrailIndividuals(trail);
		mapViewer.repaint();
	}

	/**
	 * Enables only the given event types. A {@code null} or empty set
	 * means "all event types enabled".
	 */
	public void setEnabledEventTypes(final Set<String> types){
		final Set<String> enabled = (types == null || types.isEmpty()? null: Set.copyOf(types));
		markerLayer.setEnabledEventTypes(enabled);
		trailLayer.setEnabledEventTypes(enabled);
		mapViewer.repaint();
	}

	/** Adds a georeferenced raster overlay (old map, scanned register). */
	public void addImageOverlay(final Image image, final GeoPosition northWest,
		final GeoPosition southEast, final float alpha){
		imageLayer.addOverlay(image, northWest, southEast, alpha);
		mapViewer.repaint();
	}

	/** Removes all raster overlays. */
	public void clearImageOverlays(){
		imageLayer.clearOverlays();
		mapViewer.repaint();
	}

	/** Returns the layers, in painting order, for external toggling. */
	public List<ChronomapLayer> layers(){
		return layerManager.layers();
	}

	/** Enables or disables a layer without going through the UI. */
	public void setLayerVisible(final ChronomapLayer layer, final boolean visible){
		layerManager.setVisible(layer, visible);
		mapViewer.repaint();
	}

	public void resetView(){
		final double now = timeline.getCurrentTime();
		final Set<GeoPosition> points = new LinkedHashSet<>();
		for(final String id : markerLayer.getVisibleIndividuals()){
			final List<ChronomapIndex.GeoAnchor> anchors = index.anchorsOf(id);
			if(anchors.isEmpty())
				continue;
			final ChronomapIndex.GeoCoordinate pos =
				ChronomapOverlayPainter.interpolate(anchors, now);
			if(pos == null)
				continue;
			points.add(new GeoPosition(pos.latitude(), pos.longitude()));
		}

		SwingUtilities.invokeLater(() -> {
			if(points.isEmpty()){
				mapViewer.setAddressLocation(INITIAL_CENTER);
				mapViewer.setZoom(INITIAL_ZOOM);
			}
			else if(points.size() == 1){
				mapViewer.setAddressLocation(points.iterator().next());
				mapViewer.setZoom(8);
			}
			else
				mapViewer.zoomToBestFit(points, 0.7);

			timeline.resetZoom();
		});
	}


	/* ======================================================================
	 *                          UI
	 * ====================================================================== */

	private void buildUI(){
		setLayout(new BorderLayout());

		final JPanel south = new JPanel(new BorderLayout());
		south.add(timeline, BorderLayout.CENTER);

		final JPanel toolbar = new JPanel();
		final JButton resetButton = new JButton("Reset view");
		resetButton.addActionListener(e -> resetView());
		toolbar.add(resetButton);

		toolbar.add(createLayersButton());
		toolbar.add(createEventsButton());

		add(mapViewer, BorderLayout.CENTER);
		add(south, BorderLayout.SOUTH);
		add(toolbar, BorderLayout.NORTH);
	}

	/**
	 * Builds the "Layers" menu button. Each layer is a checkbox that
	 * toggles its visibility on the map.
	 */
	private JButton createLayersButton(){
		final JButton button = new JButton("Layers");
		final JPopupMenu menu = new JPopupMenu();

		for(final ChronomapLayer layer : layerManager.layers()){
			final JCheckBoxMenuItem item = new JCheckBoxMenuItem(layer.getName(), layer.isVisible());
			item.addActionListener(e -> {
				layer.setVisible(item.isSelected());
				mapViewer.repaint();
			});
			menu.add(item);
		}

		button.addActionListener(e -> menu.show(button, 0, button.getHeight()));
		return button;
	}

	/**
	 * Builds the "Events" menu button. Each distinct event type in the
	 * model is a checkbox; all are enabled by default. Toggling one
	 * updates the marker and trail filters.
	 */
	private JButton createEventsButton(){
		final JButton button = new JButton("Events");
		final JPopupMenu menu = new JPopupMenu();

		eventTypeItems.clear();
		for(final String type : index.eventTypes()){
			final JCheckBoxMenuItem item = new JCheckBoxMenuItem(type, true);
			item.addActionListener(e -> applyEventFilterFromUi());
			eventTypeItems.put(type, item);
			menu.add(item);
		}

		if(eventTypeItems.isEmpty()){
			final JCheckBoxMenuItem empty = new JCheckBoxMenuItem("(no events)");
			empty.setEnabled(false);
			menu.add(empty);
		}

		button.addActionListener(e -> menu.show(button, 0, button.getHeight()));
		return button;
	}

	private void applyEventFilterFromUi(){
		final Set<String> enabled = new LinkedHashSet<>();
		for(final Map.Entry<String, JCheckBoxMenuItem> e : eventTypeItems.entrySet())
			if(e.getValue().isSelected())
				enabled.add(e.getKey());

		// All selected => no filter. This avoids filtering out the
		// attributes on a full selection.
		setEnabledEventTypes(enabled.size() == eventTypeItems.size()? null: enabled);
	}

	private void configureTileFactory(){
		final OSMTileFactoryInfo info = new OSMTileFactoryInfo(
			"OpenStreetMap", "https://tile.openstreetmap.org");
		final DefaultTileFactory tileFactory = new DefaultTileFactory(info);
		tileFactory.setThreadPoolSize(8);
		tileFactory.setUserAgent("FamilyLegacy/1.0 (genealogy research)");

		final File cacheDir = new File(System.getProperty("user.home")
			+ File.separator + ".familylegacy" + File.separator + "tiles");
		tileFactory.setLocalCache(new FileBasedLocalCache(cacheDir, false));

		mapViewer.setTileFactory(tileFactory);
		mapViewer.setAddressLocation(INITIAL_CENTER);
		mapViewer.setZoom(INITIAL_ZOOM);
		mapViewer.setOverlayPainter(layerManager);
	}

	private void configureInteraction(){
		final MouseInputListener pan = new PanMouseInputListener(mapViewer);
		mapViewer.addMouseListener(pan);
		mapViewer.addMouseMotionListener(pan);
		mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));
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

		final String content;
		try(final InputStream is = ChronomapPanel.class.getResourceAsStream("/tests/TGMZ.flef")){
			content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
		final FLEFModel model = new FLEFParser().parse(content);

		SwingUtilities.invokeLater(() -> {
			final ChronomapPanel panel = create(model);
			panel.setIndividuals(model.getRecordsByType(IndividualHandler.TYPE)
				.stream()
				.map(FLEFRecord::getId)
				.toList());
			panel.setTrailIndividuals(List.of("I1"));

			final JFrame frame = new JFrame("Chronomap");
			frame.add(panel, BorderLayout.CENTER);
			frame.setSize(1000, 720);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
