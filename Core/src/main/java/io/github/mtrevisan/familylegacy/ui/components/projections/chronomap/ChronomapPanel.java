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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ChronomapPanel extends JPanel{

	private static final Logger LOGGER = LoggerFactory.getLogger(ChronomapPanel.class);


	private static final GeoPosition WORLD_CENTER = new GeoPosition(0., 0.);
	private static final int WORLD_ZOOM = 18;

	private static final int MARKER_HIT_RADIUS_PX = 10;

	private static final float HISTORICAL_OVERLAY_ALPHA = 0.85f;


	private final ChronomapIndex index;
	private final ChronomapTimeline timeline;
	private final WorkspaceSelection selection;
	private final boolean ownsTimeline;

	private final JXMapViewer mapViewer = new JXMapViewer();

	private DefaultTileFactory osmTileFactory;
	private boolean autoSelectMapEnabled = true;

	/**
	 * Overlays currently displayed, ordered from the largest to the
	 * smallest. The first element is painted first (bottom of the stack),
	 * the last element is painted last (top of the stack). This ordering
	 * makes the smaller, more detailed maps cover the larger, coarser
	 * ones in their overlap region.
	 */
	private final List<MapWarperTileOverlayPainter> historicalMapOverlays = new ArrayList<>();
	/** Metadata of the maps currently shown, parallel to {@link #historicalMapOverlays}. */
	private final List<HistoricalMapMetaData> activeHistoricalMaps = new ArrayList<>();
	private final Map<Integer, DefaultTileFactory> mapWarperCache = new LinkedHashMap<>();

	// https://mapwarper.net/maps/98752
	// TODO see MapWarperMetadataProbe
	private final List<HistoricalMapMetaData> availableHistoricalMaps = List.of(
		new HistoricalMapMetaData(98752, "Italy [1499]", 2268559L, 2268923L,
			new GeoPosition(47.8777742, 4.2787858), new GeoPosition(34.7527998, 19.1181981)),
		new HistoricalMapMetaData(40917, "East Europe [1570]", firstDayOfYear(1570), lastDayOfYear(1570),
			new GeoPosition(57.4950594973751, 8.055127588116198), new GeoPosition(33.46653425876917, 35.841168995341484)),
		new HistoricalMapMetaData(40364, "Europe, the peace of Westphalia [1648]", firstDayOfYear(1648), lastDayOfYear(1648),
			new GeoPosition(62.93794, -22.0289902), new GeoPosition(30.1449439, 54.4634365)),
		new HistoricalMapMetaData(86282, "Europe [1696]", firstDayOfYear(1696), lastDayOfYear(1696),
			new GeoPosition(76.2465603, -37.1024572), new GeoPosition(27.7860639, 91.9596861)),
		new HistoricalMapMetaData(70055, "Italy [1790]", 2374845L, 2375209L,
			new GeoPosition(49.2560582, 4.77068), new GeoPosition(32.2506154, 19.9735354)),
		new HistoricalMapMetaData(101098, "Europe [1810]", firstDayOfYear(1810), lastDayOfYear(1810),
			new GeoPosition(74.336858, -34.6566512), new GeoPosition(24.3812222, 87.5786717)),
		new HistoricalMapMetaData(50110, "Italy [1811]", 2382514L, 2382878L,
			new GeoPosition(48.6998699, 3.8993105), new GeoPosition(33.2088633, 23.9187322)),
		new HistoricalMapMetaData(46121, "Europe map [1815]", firstDayOfYear(1815), lastDayOfYear(1815),
			new GeoPosition(60.4193475, -13.6826846), new GeoPosition(32.8045958, 39.8144521)),
		new HistoricalMapMetaData(109358, "Italy [1815]", 2383975L, 2384339L,
			new GeoPosition(47.2788843, 3.7518328), new GeoPosition(34.9267374, 19.0513314)),
		new HistoricalMapMetaData(46122, "Europe [1854]", firstDayOfYear(1854), lastDayOfYear(1854),
			new GeoPosition(73.1984149, -50.6104437), new GeoPosition(21.9213243, 78.0662862)),
		new HistoricalMapMetaData(694, "Central Europe [1875]", firstDayOfYear(1875), lastDayOfYear(1875),
			new GeoPosition(58.82915948331553, -12.87125040612257), new GeoPosition(31.472393302609195, 42.76694731645283)),
		new HistoricalMapMetaData(98610, "Europe Train Map [1889]", firstDayOfYear(1889), lastDayOfYear(1889),
			new GeoPosition(67.1616677, -30.5444337), new GeoPosition(22.6591603, 67.9727088)),
		new HistoricalMapMetaData(54313, "Europe [1895]", firstDayOfYear(1895), lastDayOfYear(1895),
			new GeoPosition(75.4558424, -36.6280992), new GeoPosition(25.6867332, 85.789368)),
		new HistoricalMapMetaData(13354, "Europe [1910]", firstDayOfYear(1910), lastDayOfYear(1910),
			new GeoPosition(76.117162, -50.0199415), new GeoPosition(18.4774785, 87.5562268)),
		new HistoricalMapMetaData(52583, "East Central Europe [1910]", firstDayOfYear(1910), lastDayOfYear(1910),
			new GeoPosition(58.2223856, 9.2425931), new GeoPosition(33.2128363, 33.0980444)),
		new HistoricalMapMetaData(23789, "Europe [1914]", firstDayOfYear(1914), lastDayOfYear(1914),
			new GeoPosition(57.4161474, -8.5438205), new GeoPosition(39.0943806, 34.872125)),
		new HistoricalMapMetaData(91403, "Map of Europe sep [193807]", firstDayOfYear(1938) + 181, firstDayOfYear(1938) + 211,
			new GeoPosition(76.3769155, -20.9594392), new GeoPosition(31.9105175, 53.3787187)),
		new HistoricalMapMetaData(65941, "Europe [1939]", firstDayOfYear(1939), lastDayOfYear(1939),
			new GeoPosition(58.8626453, -20.9326147), new GeoPosition(25.7711045, 50.9206112)),
		new HistoricalMapMetaData(103854, "Europe [1955]", firstDayOfYear(1955), lastDayOfYear(1955),
			new GeoPosition(84.0118525, -79.7691404), new GeoPosition(9.8722811, 121.202397)),
		new HistoricalMapMetaData(39802, "Europe [1955]", firstDayOfYear(1955), lastDayOfYear(1955),
			new GeoPosition(83.3444685, -91.3676302), new GeoPosition(8.4669641, 112.0641015))
	);

	private final ChronomapLayerManager layerManager = new ChronomapLayerManager();
	private final ChronomapOverlayPainter markerLayer;
	private final ChronomapImageOverlayLayer imageLayer;

	private final JCheckBox showUncertaintyCheckBox = new JCheckBox("Show uncertainty", true);

	private final Map<String, JCheckBoxMenuItem> eventTypeItems = new LinkedHashMap<>();

	private record ProgressData(int current, int total, String placeName){}


	/* ======================================================================
	 *                          JDN helpers
	 * ====================================================================== */

	/**
	 * Julian Day Number of January 1 of the given Gregorian year.
	 * Gregorian proleptic calendar; all divisions are integer divisions.
	 */
	static long firstDayOfYear(final int year){
		return gregorianToJdn(year, 1, 1);
	}

	/**
	 * Julian Day Number of December 31 of the given Gregorian year.
	 * Gregorian proleptic calendar; all divisions are integer divisions.
	 */
	static long lastDayOfYear(final int year){
		return gregorianToJdn(year, 12, 31);
	}

	private static long gregorianToJdn(final int year, final int month, final int day){
		final int a = (14 - month) / 12;
		final int y = year + 4800 - a;
		final int m = month + 12 * a - 3;
		return day + (153L * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045L;
	}


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
			for(final MapWarperTileOverlayPainter overlay : historicalMapOverlays)
				overlay.setCurrentTime(jdn);
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

	/**
	 * Starts the background geocoding worker.
	 * <p>
	 * The worker is started only when there is actual network work to do.
	 * The list of places to geocode is computed by
	 * {@link PlaceCoordinateResolver#extractMissingPlaces()}, which
	 * checks both the in-memory cache and the on-disk cache. When every
	 * place is already resolved (a typical second run on the same file),
	 * the method returns without showing the progress dialog or
	 * scheduling any background task.
	 */
	private void startGeocodingWorker(final PlaceCoordinateResolver placeResolver){
		final List<FLEFRecord> placesToGeocode = placeResolver.extractMissingPlaces();
		if(placesToGeocode.isEmpty()){
			LOGGER.debug("Geocoding skipped: all places are already cached");

			return;
		}

		LOGGER.debug("Geocoding {} places not present in the cache", placesToGeocode.size());

		final SwingWorker<Integer, ProgressData> worker = new SwingWorker<>(){
			private GeocodingProgressDialog dialog;

			@Override
			protected Integer doInBackground(){
				return placeResolver.geocodePlaces(
					placesToGeocode,
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
	 * Chooses the historical map overlays to display based on the current
	 * timeline position and map viewport center. When
	 * {@link #autoSelectMapEnabled} is on, this is called automatically on
	 * time changes and viewport panning.
	 * <p>
	 * All matching maps are shown simultaneously as a mosaic. They are
	 * sorted by area descending, so the smaller, more detailed maps are
	 * painted on top of the larger, coarser ones.
	 */
	private void autoSelectBaseMap(final double currentJdn){
		if(!autoSelectMapEnabled){
			// Manual mode: keep only the maps that are still valid at the
			// current time. Nothing else changes.
			final List<HistoricalMapMetaData> stillValid = activeHistoricalMaps.stream()
				.filter(map -> map.isAvailableAt(currentJdn))
				.toList();
			if(stillValid.size() != activeHistoricalMaps.size())
				setHistoricalOverlays(stillValid);

			return;
		}

		final GeoPosition currentCenter = mapViewer.getCenterPosition();

		final List<HistoricalMapMetaData> matching = availableHistoricalMaps.stream()
			.filter(map -> map.isMatching(currentJdn, currentCenter))
			.sorted(Comparator.comparingDouble(ChronomapPanel::areaOf).reversed())
			.toList();

		setHistoricalOverlays(matching);
	}

	/**
	 * Computes the area of the bounding box of the given historical map,
	 * expressed in square degrees. Only used to compare maps against each
	 * other, so absolute units are irrelevant.
	 */
	private static double areaOf(final HistoricalMapMetaData map){
		final GeoPosition nw = map.northWest();
		final GeoPosition se = map.southEast();
		final double latSpan = Math.abs(nw.getLatitude() - se.getLatitude());
		final double lonSpan = Math.abs(nw.getLongitude() - se.getLongitude());
		return latSpan * lonSpan;
	}

	/**
	 * Replaces the current set of historical overlays with the given list.
	 * The list must already be sorted by area descending. Maps already
	 * active are kept as-is, maps no longer active are removed, and new
	 * maps are created and inserted at the bottom of the layer stack.
	 * <p>
	 * The method is a no-op when the new set is identical to the current
	 * one, so that panning does not cause flicker.
	 */
	private void setHistoricalOverlays(final List<HistoricalMapMetaData> maps){
		// Fast path: same set, same order.
		if(activeHistoricalMaps.equals(maps))
			return;

		// 1. Remove overlays that are no longer needed.
		final Iterator<MapWarperTileOverlayPainter> it = historicalMapOverlays.iterator();
		while(it.hasNext()){
			final MapWarperTileOverlayPainter overlay = it.next();
			final boolean stillNeeded = maps.stream()
				.anyMatch(m -> m.mapId() == overlay.mapId());
			if(!stillNeeded){
				layerManager.removeLayer(overlay);
				it.remove();
			}
		}

		// 2. Insert the new maps at the bottom of the stack, largest first.
		// addLayer(0, ...) pushes everything else up, so iterating the list
		// in reverse order leaves the smallest map on top of the stack and
		// the largest just above the base map.
		for(int i = maps.size() - 1; i >= 0; i --){
			final HistoricalMapMetaData map = maps.get(i);
			final boolean alreadyActive = historicalMapOverlays.stream()
				.anyMatch(o -> o.mapId() == map.mapId());
			if(alreadyActive)
				continue;

			final DefaultTileFactory factory = getOrCreateMapWarperFactory(map.mapId());
			final MapWarperTileOverlayPainter overlay = new MapWarperTileOverlayPainter(map, factory);
			overlay.setAlpha(HISTORICAL_OVERLAY_ALPHA);
			overlay.setCurrentTime(timeline.getCurrentTime());
			layerManager.addLayer(0, overlay);

			historicalMapOverlays.add(overlay);
		}

		// 3. Rebuild the metadata list from the overlay order so the two
		// stay in sync. The overlay list is ordered bottom-to-top, i.e.
		// largest to smallest, which matches the sort applied by
		// autoSelectBaseMap.
		activeHistoricalMaps.clear();
		for(final MapWarperTileOverlayPainter overlay : historicalMapOverlays)
			activeHistoricalMaps.add(overlay.metadata());

		mapViewer.repaint();
	}


	private JButton createHistoricalMapButton(){
		final JButton button = new JButton("Historical Map");
		final JPopupMenu menu = new JPopupMenu();

		button.addActionListener(e -> {
			menu.removeAll();

			// Option 1: auto-selection toggle.
			final JCheckBoxMenuItem autoItem = new JCheckBoxMenuItem("Auto Select Historical Map", autoSelectMapEnabled);
			autoItem.addActionListener(ev -> {
				autoSelectMapEnabled = autoItem.isSelected();
				if(autoSelectMapEnabled)
					autoSelectBaseMap(timeline.getCurrentTime());
			});
			menu.add(autoItem);
			menu.addSeparator();

			// Option 2: manual "None" (modern map only).
			final boolean noneActive = (!autoSelectMapEnabled && activeHistoricalMaps.isEmpty());
			final JRadioButtonMenuItem noneItem = new JRadioButtonMenuItem("None (modern map only)", noneActive);
			noneItem.addActionListener(ev -> {
				autoSelectMapEnabled = false;
				setHistoricalOverlays(List.of());
			});
			menu.add(noneItem);

			// Option 3: manual override for available historical maps.
			// Selecting one disables auto-selection and replaces the whole
			// overlay set with just that map.
			for(final HistoricalMapMetaData map : availableHistoricalMaps){
				final boolean isCurrent = !autoSelectMapEnabled
					&& activeHistoricalMaps.size() == 1
					&& activeHistoricalMaps.getFirst().mapId() == map.mapId();
				final JRadioButtonMenuItem mapItem = new JRadioButtonMenuItem(map.name(), isCurrent);
				mapItem.addActionListener(ev -> {
					autoSelectMapEnabled = false;
					setHistoricalOverlays(List.of(map));
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
			final int totalMapZoom = osmTileFactory.getInfo()
				.getTotalMapZoom();
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
		final int totalMapZoom = osmTileFactory.getInfo()
			.getTotalMapZoom();
		markerLayer.setTotalMapZoom(totalMapZoom);


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
