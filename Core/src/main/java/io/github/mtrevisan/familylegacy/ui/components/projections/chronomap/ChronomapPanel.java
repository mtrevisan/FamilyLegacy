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
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.MouseInputListener;
import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Window;
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
 */
public class ChronomapPanel extends JPanel{

	private static final Logger LOGGER = LoggerFactory.getLogger(ChronomapPanel.class);

	// Global view constants
	private static final GeoPosition WORLD_CENTER = new GeoPosition(0., 0.);
	private static final int WORLD_ZOOM = 18;

	private final ChronomapIndex index;

	private final JXMapViewer mapViewer = new JXMapViewer();
	private final ChronomapTimeline timeline = new ChronomapTimeline();

	private DefaultTileFactory osmTileFactory;
	private boolean autoSelectMapEnabled = true;
	private HistoricalMapMetaData activeHistoricalMap = null;
	/**
	 * Automatically selects the best matching historical map based on current timeline position and map viewport center.
	 * Reverts to modern OpenStreetMap if no historical map matches.
	 */
	private MapWarperTileOverlayPainter historicalMapOverlay;
	private final Map<Integer, DefaultTileFactory> mapWarperCache = new LinkedHashMap<>();
	//https://mapwarper.net/maps/98752
	private final List<HistoricalMapMetaData> availableHistoricalMaps = List.of(
		new HistoricalMapMetaData(98752, "Italy 1499", 2268561L, 2268922L,
			new GeoPosition(47.0925, 6.6261), new GeoPosition(35.49, 18.5194)),
		new HistoricalMapMetaData(70055, "Italy 1790", 2375208L, 2374845L,
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


	public static ChronomapPanel create(final FLEFModel model){
		final Path cacheFile = Path.of(System.getProperty("user.home"), ".familylegacy", "geocoding.properties");
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
		this.index = index;
		this.markerLayer = new ChronomapOverlayPainter(model, index);
		this.imageLayer = new ChronomapImageOverlayLayer();

		layerManager.addLayer(markerLayer);
		layerManager.addLayer(imageLayer);

		buildUI();
		configureTileFactory();
		configureInteraction();

		timeline.withTimeListener(jdn -> {
			markerLayer.setCurrentTime(jdn);
			// Automatically switch tile factory on time change
			autoSelectBaseMap();
			mapViewer.repaint();
		});

		final long[] range = index.computeGlobalDateRange();
		if(range != null && range[0] < range[1])
			// setDomain automatically defaults to range[1] (most recent date)
			// and notifies timeListener to sync markerLayer and trailLayer.
			timeline.setDomain(range[0], range[1]);

		showUncertaintyCheckBox.addActionListener(e -> {
			markerLayer.setShowUncertainty(showUncertaintyCheckBox.isSelected());
			mapViewer.repaint();
		});

		startGeocodingWorker(placeResolver);
	}

	private void startGeocodingWorker(final PlaceCoordinateResolver placeResolver){
		final SwingWorker<Integer, ProgressData> worker = new SwingWorker<>(){
			private GeocodingProgressDialog dialog;

			@Override
			protected Integer doInBackground(){
//				final List<FLEFRecord> places = placeResolver.extractMissingPlaces();
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

		final JPanel south = new JPanel(new BorderLayout());
		south.add(timeline, BorderLayout.CENTER);

		final JPanel toolbar = new JPanel();
		final JButton resetButton = new JButton("Reset view");
		resetButton.addActionListener(e -> resetView());
		toolbar.add(resetButton);

		toolbar.add(createBaseMapButton());
		toolbar.add(createLayersButton());
		toolbar.add(createEventsButton());
		toolbar.add(showUncertaintyCheckBox);

		add(mapViewer, BorderLayout.CENTER);
		add(south, BorderLayout.SOUTH);
		add(toolbar, BorderLayout.NORTH);
	}

	private void autoSelectBaseMap(){
		if(!autoSelectMapEnabled)
			return;

		final double currentJdn = timeline.getCurrentTime();
		final GeoPosition currentCenter = mapViewer.getCenterPosition();

		final HistoricalMapMetaData matchingMap = availableHistoricalMaps.stream()
			.filter(map -> map.isMatching(currentJdn, currentCenter))
			.findFirst()
			.orElse(null);

		if(matchingMap != null){
			if(activeHistoricalMap != matchingMap){
				activeHistoricalMap = matchingMap;
				final DefaultTileFactory factory = getOrCreateMapWarperFactory(matchingMap.mapId());

				if(historicalMapOverlay == null){
					historicalMapOverlay = new MapWarperTileOverlayPainter(matchingMap, factory);
					layerManager.addLayer(historicalMapOverlay);
				}
				else{
					historicalMapOverlay.setMetaData(matchingMap);
					historicalMapOverlay.setVisible(true);
				}
			}
		}
		else if(activeHistoricalMap != null){
			activeHistoricalMap = null;
			if(historicalMapOverlay != null)
				historicalMapOverlay.setVisible(false);
		}
		mapViewer.repaint();
	}

	private void updateBaseMapForTime(final double currentJdn){
		// Revert to modern OpenStreetMap if the active historical map is no longer valid for the selected date
		if(activeHistoricalMap != null && !activeHistoricalMap.isAvailableAt(currentJdn)){
			switchTileFactory(osmTileFactory);
			activeHistoricalMap = null;
		}
	}

	private void switchTileFactory(final DefaultTileFactory newFactory){
		if(mapViewer.getTileFactory() == newFactory)
			return;

		// Preserve current viewport position and zoom level during factory switch
		final GeoPosition currentCenter = mapViewer.getCenterPosition();
		final int currentZoom = mapViewer.getZoom();

		mapViewer.setTileFactory(newFactory);

		if(currentCenter != null){
			mapViewer.setAddressLocation(currentCenter);
			mapViewer.setZoom(currentZoom);
		}
		mapViewer.repaint();
	}

	private JButton createBaseMapButton(){
		final JButton button = new JButton("Base Map");
		final JPopupMenu menu = new JPopupMenu();

		button.addActionListener(e -> {
			menu.removeAll();

			// Auto-selection option
			final JCheckBoxMenuItem autoItem = new JCheckBoxMenuItem("Auto Select Historical Map", autoSelectMapEnabled);
			autoItem.addActionListener(ev -> {
				autoSelectMapEnabled = autoItem.isSelected();
				if(autoSelectMapEnabled)
					autoSelectBaseMap();
			});
			menu.add(autoItem);
			menu.addSeparator();

			// Manual overrides
			final JMenuItem osmItem = new JMenuItem("OpenStreetMap (Modern)");
			osmItem.addActionListener(ev -> {
				autoSelectMapEnabled = false;
				activeHistoricalMap = null;
				switchTileFactory(osmTileFactory);
			});
			menu.add(osmItem);

			final double currentJdn = timeline.getCurrentTime();
			final GeoPosition center = mapViewer.getCenterPosition();

			for(final HistoricalMapMetaData map : availableHistoricalMaps){
				if(map.isMatching(currentJdn, center)){
					final JMenuItem mapItem = new JMenuItem(map.name());
					mapItem.addActionListener(ev -> {
						autoSelectMapEnabled = false;
						activeHistoricalMap = map;
						switchTileFactory(getOrCreateMapWarperFactory(map.mapId()));
					});
					menu.add(mapItem);
				}
			}

			menu.show(button, 0, button.getHeight());
		});

		return button;
	}

	private DefaultTileFactory getOrCreateMapWarperFactory(final int mapId) {
		return mapWarperCache.computeIfAbsent(mapId, id ->
			createTileFactory(new MapWarperTileFactoryInfo(id))
		);
	}

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

		setEnabledEventTypes(enabled.size() == eventTypeItems.size() ? null : enabled);
	}

	private void configureTileFactory(){
		// 1. OpenStreetMap Tile Factory (Modern)
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

		// Trigger auto map selection when panning or zooming stops/changes
		mapViewer.addPropertyChangeListener("center", evt -> autoSelectBaseMap());
	}


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
//			panel.setTrailIndividuals(List.of("I1"));

			final JFrame frame = new JFrame("Chronomap");
			frame.add(panel, BorderLayout.CENTER);
			frame.setSize(1000, 720);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
