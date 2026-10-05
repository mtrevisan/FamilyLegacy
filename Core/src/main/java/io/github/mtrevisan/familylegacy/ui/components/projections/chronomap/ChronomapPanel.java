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

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.dialogs.GeocodingProgressDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.cache.FileBasedLocalCache;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.MouseInputListener;
import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Panel that shows a map of the places where the events of the visible
 * individuals happened, filtered by a zoomable timeline.
 *
 * <p>The panel can be used standalone (it owns its own timeline, which is
 * embedded in the bottom of the panel) or as part of a
 * {@link ChronomapWorkspace} (it receives a shared timeline and a shared
 * {@link WorkspaceSelection} from outside). In the latter case the
 * timeline is not embedded, because the workspace installs it below
 * both the map and the list.</p>
 *
 * <p><b>Historical maps and base map.</b> The modern OpenStreetMap layer
 * is the <em>base map</em> of the {@link JXMapViewer}: it is always
 * enabled and is not exposed as a layer. Historical maps are rendered as
 * semi-transparent overlays on top of the base map, via a
 * {@link MapWarperTileOverlayPainter} inserted at the bottom of the layer
 * stack (below images and markers). The overlay's
 * {@link MapWarperTileFactoryInfo} is built with the <em>same</em>
 * {@code totalMapZoom} as the OSM factory, so that world-pixel
 * coordinates coincide and the overlay lines up with the base map.</p>
 */
public class ChronomapPanel extends JPanel{

	private static final Logger LOGGER = LoggerFactory.getLogger(ChronomapPanel.class);

	// Global view constants
	private static final GeoPosition WORLD_CENTER = new GeoPosition(0., 0.);
	private static final int WORLD_ZOOM = 18;

	/** Radius in pixels within which a click is considered a marker hit. */
	private static final int MARKER_HIT_RADIUS_PX = 10;

	/** Opacity of the historical map overlay on top of the modern base map. */
	private static final float HISTORICAL_OVERLAY_ALPHA = 0.85f;


	private final ChronomapIndex index;
	private final ChronomapTimeline timeline;
	private final WorkspaceSelection selection;
	private final boolean ownsTimeline;

	private final JXMapViewer mapViewer = new JXMapViewer();

	private DefaultTileFactory osmTileFactory;
	private boolean autoSelectMapEnabled = true;
	private HistoricalMapMetaData activeHistoricalMap;
	/**
	 * Overlay currently displayed on top of the OSM base map. There is at
	 * most one active historical overlay at a time; switching to a
	 * different map replaces it entirely, because the tile factory (and
	 * therefore the MapWarper URL) is bound to the map identifier and
	 * cannot be reused across maps.
	 */
	private MapWarperTileOverlayPainter historicalMapOverlay;
	private final Map<Integer, DefaultTileFactory> mapWarperCache = new LinkedHashMap<>();

	//https://mapwarper.net/maps/98752
	private final List<HistoricalMapMetaData> availableHistoricalMaps = List.of(
		new HistoricalMapMetaData(98752, "Italy 1499", 2268561L, 2268922L,
			new GeoPosition(47.0925, 6.6261), new GeoPosition(35.49, 18.5194)),
		new HistoricalMapMetaData(70055, "Italy 1790", 2374845L, 2375208L,
			new GeoPosition(47.0925, 6.6261), new GeoPosition(35.49, 18.5194)),
		new HistoricalMapMetaData(50110, "Italy 1811", 2382514L, 2382878L,
			new GeoPosition(47.0925, 6.6261), new GeoPosition(35.49, 18.5194)),
		new HistoricalMapMetaData(109358, "Italy 1815", 2383977L, 2384339L,
			new GeoPosition(47.0925, 6.6261), new GeoPosition(35.49, 18.5194))
	);

	private final ChronomapLayerManager layerManager = new ChronomapLayerManager();
	private final ChronomapOverlayPainter markerLayer;
	private final ChronomapImageOverlayLayer imageLayer;

	private final JCheckBox showUncertaintyCheckBox = new JCheckBox("Show uncertainty", true);

	private final Map<String, JCheckBoxMenuItem> eventTypeItems = new LinkedHashMap<>();

	private record ProgressData(int current, int total, String placeName){}


	/* ======================================================================
	 *                          Factories
	 * ====================================================================== */

	/**
	 * Creates a standalone panel with its own timeline and no shared
	 * selection. The timeline is embedded at the bottom of the panel.
	 */
	public static ChronomapPanel create(final FLEFModel model){
		final Path cacheFile = Path.of(System.getProperty("user.home"), ".familylegacy", "geocoding.properties");
		final PlaceCoordinateResolver placeResolver = new PlaceCoordinateResolver(model, cacheFile);
		final ChronomapIndex index = new ChronomapIndex(model, placeResolver);
		return create(model, index, placeResolver);
	}

	/**
	 * Creates a standalone panel using an externally supplied index and
	 * resolver. The timeline is created internally.
	 */
	public static ChronomapPanel create(final FLEFModel model, final ChronomapIndex index,
		final PlaceCoordinateResolver placeResolver){
		return new ChronomapPanel(model, index, placeResolver, null, null);
	}

	/**
	 * Creates a panel that shares the given timeline and selection with
	 * other views. The timeline is not embedded, because the enclosing
	 * workspace installs it below the split pane.
	 */
	public static ChronomapPanel create(final FLEFModel model, final ChronomapIndex index,
		final PlaceCoordinateResolver placeResolver, final ChronomapTimeline timeline,
		final WorkspaceSelection selection){
		return new ChronomapPanel(model, index, placeResolver, timeline, selection);
	}


	private ChronomapPanel(final FLEFModel model, final ChronomapIndex index,
		final PlaceCoordinateResolver placeResolver, final ChronomapTimeline timeline,
		final WorkspaceSelection selection){
		this.index = index;
		this.markerLayer = new ChronomapOverlayPainter(model, index);
		this.imageLayer = new ChronomapImageOverlayLayer();

		// A null timeline means "standalone": the panel creates and owns
		// its own, and embeds it in its layout. A non-null timeline is
		// shared with the workspace, which is responsible for embedding it.
		this.ownsTimeline = (timeline == null);
		this.timeline = (timeline != null? timeline: new ChronomapTimeline());
		this.selection = selection;

		// Painting order (bottom-to-top):
		//   1. historical map overlay (inserted later at index 0)
		//   2. image overlays
		//   3. markers (with labels and trails)
		// The modern base map is the viewer's tile factory, always below
		// all layers.
		layerManager.addLayer(imageLayer);
		layerManager.addLayer(markerLayer);

		buildUI();
		configureTileFactory();
		configureInteraction();
		wireSelection();

		this.timeline.withTimeListener(jdn -> {
			markerLayer.setCurrentTime(jdn);

			if(historicalMapOverlay != null)
				historicalMapOverlay.setCurrentTime(jdn);

			// Automatically switch historical overlay on time change
			autoSelectBaseMap(jdn);

			mapViewer.repaint();
		});

		final long[] range = index.computeGlobalDateRange();
		if(range != null && range[0] < range[1])
			// setDomain automatically defaults to range[1] (most recent date)
			// and notifies timeListener to sync markerLayer and overlay.
			this.timeline.setDomain(range[0], range[1]);

		showUncertaintyCheckBox.addActionListener(e -> {
			markerLayer.setShowUncertainty(showUncertaintyCheckBox.isSelected());
			mapViewer.repaint();
		});

		startGeocodingWorker(placeResolver);
	}


	/* ======================================================================
	 *                          Selection wiring
	 * ====================================================================== */

	/**
	 * Wires the shared selection so that:
	 * <ul>
	 *   <li>a selection made elsewhere highlights the marker on the map;</li>
	 *   <li>a single click on a marker selects the individual in the
	 *       workspace, which in turn highlights the row in the list.</li>
	 * </ul>
	 * Does nothing when no shared selection has been provided.
	 */
	private void wireSelection(){
		if(selection == null)
			return;

		// Selection made outside: highlight the marker.
		selection.addListener(id -> {
			markerLayer.setSelectedId(id);
			mapViewer.repaint();
		});

		// Click on the map: hit-test markers and select the closest one.
		mapViewer.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(!SwingUtilities.isLeftMouseButton(e))
					return;
				if(e.getClickCount() != 1)
					return;

				final String hit = findMarkerAt(e.getX(), e.getY());
				if(hit != null)
					selection.select(hit);
			}
		});
	}


	/**
	 * Returns the id of the closest visible marker within
	 * {@link #MARKER_HIT_RADIUS_PX} pixels of the given point, or
	 * {@code null} when no marker is near enough.
	 */
	private String findMarkerAt(final int x, final int y){
		final double now = timeline.getCurrentTime();
		String bestId = null;
		double bestDistance = MARKER_HIT_RADIUS_PX;

		for(final String id : markerLayer.getVisibleIndividuals()){
			final List<ChronomapIndex.GeoAnchor> anchors = index.anchorsOf(id);
			if(anchors.isEmpty())
				continue;

			final ChronomapOverlayPainter.InterpolatedPosition state =
				ChronomapOverlayPainter.interpolatePosition(mapViewer, anchors, now);
			if(state == null)
				continue;

			final ChronomapIndex.GeoCoordinate pos = state.coordinate();
			final Point2D p = mapViewer.convertGeoPositionToPoint(
				new GeoPosition(pos.latitude(), pos.longitude()));
			final double d = p.distance(x, y);
			if(d < bestDistance){
				bestDistance = d;
				bestId = id;
			}
		}
		return bestId;
	}


	/* ======================================================================
	 *                          Geocoding worker
	 * ====================================================================== */

	private void startGeocodingWorker(final PlaceCoordinateResolver placeResolver){
		final SwingWorker<Integer, ProgressData> worker = new SwingWorker<>(){
			private GeocodingProgressDialog dialog;

			@Override
			protected Integer doInBackground(){
				final List<FLEFRecord> places = placeResolver.extractAllPlaces();
				return placeResolver.geocodePlaces(
					places,
					(current, total, placeName) -> publish(new ProgressData(current, total, placeName)),
					this::isCancelled
				);
			}

			@Override
			protected void process(final List<ProgressData> chunks){
				if(isCancelled())
					return;

				final ProgressData latest = chunks.getLast();
				if(dialog == null){
					final Window windowOwner = SwingUtilities.getWindowAncestor(ChronomapPanel.this);
					dialog = new GeocodingProgressDialog(
						windowOwner,
						I18N.t("dialog.geocoding.title"),
						() -> cancel(true)
					);
					dialog.setVisible(true);
				}
				dialog.updateProgress(latest.current(), latest.total(), latest.placeName());
			}

			@Override
			protected void done(){
				if(dialog != null)
					dialog.dispose();

				try{
					final int resolved = get();
					if(resolved > 0){
						index.rebuild();
						mapViewer.repaint();
					}
				}
				catch(final Exception ignored){
					LOGGER.debug("Geocoding worker cancelled or interrupted");
				}
			}
		};

		worker.execute();
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	public void setIndividuals(final Collection<String> ids){
		final List<String> visible = (ids != null? List.copyOf(ids): List.of());
		markerLayer.setVisibleIndividuals(visible);
		mapViewer.repaint();

		resetView();
	}

	public void setEnabledEventTypes(final Set<String> types){
		final Set<String> enabled = (types == null || types.isEmpty()? null: Set.copyOf(types));
		markerLayer.setEnabledEventTypes(enabled);
		mapViewer.repaint();
	}

	public void addImageOverlay(final Image image, final GeoPosition northWest, final GeoPosition southEast,
		final float alpha){
		imageLayer.addOverlay(image, northWest, southEast, alpha);
		mapViewer.repaint();
	}

	public void clearImageOverlays(){
		imageLayer.clearOverlays();
		mapViewer.repaint();
	}

	public List<ChronomapLayer> layers(){
		return layerManager.layers();
	}

	public void setLayerVisible(final ChronomapLayer layer, final boolean visible){
		layerManager.setVisible(layer, visible);
		mapViewer.repaint();
	}

	public void resetView(){
		final double now = timeline.getCurrentTime();
		final Set<GeoPosition> points = new LinkedHashSet<>();

		// Collect current position at 'now' for all visible individuals
		for(final String id : markerLayer.getVisibleIndividuals()){
			final List<ChronomapIndex.GeoAnchor> anchors = index.anchorsOf(id);
			if(anchors.isEmpty())
				continue;

			final ChronomapOverlayPainter.InterpolatedPosition state =
				ChronomapOverlayPainter.interpolatePosition(null, anchors, now);
			if(state == null)
				continue;

			final ChronomapIndex.GeoCoordinate pos = state.coordinate();
			points.add(new GeoPosition(pos.latitude(), pos.longitude()));
		}

		SwingUtilities.invokeLater(() -> {
			if(points.isEmpty()){
				mapViewer.setAddressLocation(WORLD_CENTER);
				mapViewer.setZoom(WORLD_ZOOM);
			}
			else if(points.size() == 1){
				mapViewer.setAddressLocation(points.iterator().next());
				mapViewer.setZoom(8);
			}
			else
				// Zoom to best fit to encompass all current markers at instant 'now'
				mapViewer.zoomToBestFit(points, 0.85);

			timeline.resetZoom();
		});
	}


	/* ======================================================================
	 *                          UI
	 * ====================================================================== */

	private void buildUI(){
		setLayout(new BorderLayout());

		// Timeline is embedded only when this panel owns it. In the
		// workspace the timeline is installed by the workspace itself,
		// below both the map and the list.
		if(ownsTimeline){
			final JPanel south = new JPanel(new BorderLayout());
			south.add(timeline, BorderLayout.CENTER);
			add(south, BorderLayout.SOUTH);
		}

		final JPanel toolbar = new JPanel();
		final JButton resetButton = new JButton("Reset view");
		resetButton.addActionListener(e -> resetView());
		toolbar.add(resetButton);

		toolbar.add(createHistoricalMapButton());
		toolbar.add(createLayersButton());
		toolbar.add(createEventsButton());
		toolbar.add(showUncertaintyCheckBox);

		add(mapViewer, BorderLayout.CENTER);
		add(toolbar, BorderLayout.NORTH);
	}


	/**
	 * Chooses the historical map overlay to display based on the current
	 * timeline position and map viewport center. When
	 * {@link #autoSelectMapEnabled} is on, this is called automatically on
	 * time changes and viewport panning. Manual selection from the
	 * toolbar menu temporarily disables auto-selection.
	 * <p>
	 * The base map is never switched: the historical map is always shown
	 * as an overlay on top of the modern OpenStreetMap base.
	 */
	private void autoSelectBaseMap(final double currentJdn){
		if(!autoSelectMapEnabled){
			if(activeHistoricalMap != null && !activeHistoricalMap.isAvailableAt(currentJdn))
				hideHistoricalOverlay();

			return;
		}

		final GeoPosition currentCenter = mapViewer.getCenterPosition();

		final HistoricalMapMetaData matchingMap = availableHistoricalMaps.stream()
			.filter(map -> map.isMatching(currentJdn, currentCenter))
			.findFirst()
			.orElse(null);

		if(matchingMap != null)
			showHistoricalOverlay(matchingMap);
		else
			hideHistoricalOverlay();
	}

	/**
	 * Ensures the given historical map is shown as an overlay on top of
	 * the modern base map. A different tile factory is created for each
	 * map identifier and reused across invocations, because
	 * {@link MapWarperTileFactoryInfo} is bound to a single map id.
	 */
	private void showHistoricalOverlay(final HistoricalMapMetaData map){
		if(activeHistoricalMap != null && activeHistoricalMap.mapId() == map.mapId() && historicalMapOverlay != null){
			historicalMapOverlay.setCurrentTime(timeline.getCurrentTime());

			return;
		}

		// Remove any previously installed overlay: it belongs to a
		// different MapWarper map and cannot be reused.
		if(historicalMapOverlay != null){
			layerManager.removeLayer(historicalMapOverlay);
			historicalMapOverlay = null;
		}

		final DefaultTileFactory factory = getOrCreateMapWarperFactory(map.mapId());
		historicalMapOverlay = new MapWarperTileOverlayPainter(map, factory);
		historicalMapOverlay.setAlpha(HISTORICAL_OVERLAY_ALPHA);
		historicalMapOverlay.setCurrentTime(timeline.getCurrentTime());
		layerManager.addLayer(0, historicalMapOverlay);

		activeHistoricalMap = map;
		mapViewer.repaint();
	}

	private void hideHistoricalOverlay(){
		if(historicalMapOverlay != null){
			layerManager.removeLayer(historicalMapOverlay);
			historicalMapOverlay = null;
		}
		activeHistoricalMap = null;
		mapViewer.repaint();
	}


	private JButton createHistoricalMapButton(){
		final JButton button = new JButton("Historical Map");
		final JPopupMenu menu = new JPopupMenu();

		button.addActionListener(e -> {
			menu.removeAll();

			// Option 1: Auto-selection toggle
			final JCheckBoxMenuItem autoItem = new JCheckBoxMenuItem("Auto Select Historical Map", autoSelectMapEnabled);
			autoItem.addActionListener(ev -> {
				autoSelectMapEnabled = autoItem.isSelected();
				if(autoSelectMapEnabled){
					autoSelectBaseMap(timeline.getCurrentTime());
				}
			});
			menu.add(autoItem);
			menu.addSeparator();

			// Option 2: Manual "None" (modern map only)
			final JRadioButtonMenuItem noneItem = new JRadioButtonMenuItem("None (modern map only)", !autoSelectMapEnabled && activeHistoricalMap == null);
			noneItem.addActionListener(ev -> {
				autoSelectMapEnabled = false;
				hideHistoricalOverlay();
			});
			menu.add(noneItem);

			// Option 3: Manual override for available historical maps
			for(final HistoricalMapMetaData map : availableHistoricalMaps){
				final boolean isCurrent = !autoSelectMapEnabled && activeHistoricalMap != null && activeHistoricalMap.mapId() == map.mapId();
				final JRadioButtonMenuItem mapItem = new JRadioButtonMenuItem(map.name(), isCurrent);
				mapItem.addActionListener(ev -> {
					autoSelectMapEnabled = false;
					showHistoricalOverlay(map);
				});
				menu.add(mapItem);
			}

			menu.show(button, 0, button.getHeight());
		});

		return button;
	}

	/**
	 * Returns the MapWarper tile factory for the given map identifier,
	 * creating it on first use. The factory is built with the same
	 * {@code totalMapZoom} as the OSM base map factory, so that
	 * world-pixel coordinates coincide and the overlay aligns with the
	 * modern base map.
	 */
	private DefaultTileFactory getOrCreateMapWarperFactory(final int mapId){
		return mapWarperCache.computeIfAbsent(mapId, id -> {
			final int totalMapZoom = osmTileFactory.getInfo().getTotalMapZoom();
			final MapWarperTileFactoryInfo info = new MapWarperTileFactoryInfo(id, totalMapZoom);
			return createTileFactory(info);
		});
	}

	private JButton createLayersButton(){
		final JButton button = new JButton("Layers");
		final JPopupMenu menu = new JPopupMenu();

		button.addActionListener(e -> {
			menu.removeAll();
			for(final ChronomapLayer layer : layerManager.layers()){
				final JCheckBoxMenuItem item = new JCheckBoxMenuItem(layer.getName(), layer.isVisible());
				item.addActionListener(ev -> {
					layer.setVisible(item.isSelected());
					mapViewer.repaint();
				});
				menu.add(item);
			}
			menu.show(button, 0, button.getHeight());
		});

		return button;
	}

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

		setEnabledEventTypes(enabled.size() == eventTypeItems.size()? null: enabled);
	}

	private void configureTileFactory(){
		// OpenStreetMap is the permanent base map: it is never replaced by
		// a historical map, which is instead drawn as an overlay on top.
		final OSMTileFactoryInfo osmInfo = new OSMTileFactoryInfo("OpenStreetMap", "https://tile.openstreetmap.org");
		osmTileFactory = createTileFactory(osmInfo);

		mapViewer.setTileFactory(osmTileFactory);
		mapViewer.setAddressLocation(WORLD_CENTER);
		mapViewer.setZoom(WORLD_ZOOM);
		mapViewer.setOverlayPainter(layerManager);
	}

	private static DefaultTileFactory createTileFactory(final TileFactoryInfo info){
		final DefaultTileFactory factory = new DefaultTileFactory(info);
		factory.setThreadPoolSize(8);
		factory.setUserAgent("FamilyLegacy/1.0 (genealogy research)");

		final File cacheDir = new File(System.getProperty("user.home")
			+ File.separator + ".familylegacy" + File.separator + "tiles" + File.separator + info.getName().toLowerCase());
		factory.setLocalCache(new FileBasedLocalCache(cacheDir, false));
		return factory;
	}

	private void configureInteraction(){
		final MouseInputListener pan = new PanMouseInputListener(mapViewer);
		mapViewer.addMouseListener(pan);
		mapViewer.addMouseMotionListener(pan);
		mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));

		// Trigger auto map selection passing current timeline JDN when viewport center changes
		mapViewer.addPropertyChangeListener("center", evt -> autoSelectBaseMap(timeline.getCurrentTime()));
	}


	/* ======================================================================
	 *                          Standalone demo
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

			final JFrame frame = new JFrame("Chronomap");
			frame.add(panel, BorderLayout.CENTER);
			frame.setSize(1000, 720);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
