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

import io.github.mtrevisan.familylegacy.i18n.I18N;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.cache.FileBasedLocalCache;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.function.DoubleSupplier;


/**
 * The historical maps shown over the modern map of a {@link JXMapViewer}: which ones depict the time and the place being looked at,
 * their overlays in the layer stack, and the menu to choose them by hand.
 * <p>
 * It is shared by the chronomap and by the pickers of a place, so that every map behaves the same way. The overlays are built with the
 * same {@code totalMapZoom} as the modern base map, so that world-pixel coordinates coincide and the overlay lines up with it.
 */
public final class HistoricalMapOverlays{

	/** The maps of an area are searched only when the zoom is this one or a closer one (the lower the zoom, the closer the view). */
	private static final int HISTORICAL_SEARCH_ZOOM = 12;
	/** Opacity of the historical map overlay on top of the modern base map. */
	private static final float HISTORICAL_OVERLAY_ALPHA = 0.85f;
	/**
	 * At most this number of overlays is shown at once, the smallest (most detailed) ones: every overlay has its own tile factory and
	 * requests tiles from a rate-limited service, so showing all the maps of a crowded area would slow the whole application down.
	 * The others can still be chosen from the menu.
	 */
	private static final int MAX_AUTOMATIC_OVERLAYS = 4;
	private static final int BASE_MAP_THREADS = 8;
	private static final int HISTORICAL_MAP_THREADS = 2;


	private final JXMapViewer mapViewer;
	private final ChronomapLayerManager layerManager;
	private final CancellableTileFactory baseFactory;
	private final int totalMapZoom;
	private final DoubleSupplier currentTime;

	private boolean autoSelectMapEnabled = true;

	/**
	 * Overlays currently displayed, ordered from the largest to the smallest. The first element is painted first (bottom of the
	 * stack), the last element is painted last (top of the stack): the smaller, more detailed maps cover the larger, coarser ones in
	 * their overlap region.
	 */
	private final List<MapWarperTileOverlayPainter> historicalMapOverlays = new ArrayList<>();
	/** Metadata of the maps currently shown, parallel to {@link #historicalMapOverlays}. */
	private final List<HistoricalMapMetaData> activeHistoricalMaps = new ArrayList<>();
	private final Map<Integer, CancellableTileFactory> mapWarperCache = new LinkedHashMap<>();
	private final HistoricalMapCatalog historicalMapCatalog = SharedCatalog.INSTANCE;


	/**
	 * The catalog is loaded from the disk once and shared by every map of the application: opening a map must not parse the file again,
	 * nor start another thread and another connection pool.
	 */
	private static final class SharedCatalog{

		private static final HistoricalMapCatalog INSTANCE = new HistoricalMapCatalog(
			Path.of(System.getProperty("user.home"), ".familylegacy", "historical-maps.properties"));

	}


	/**
	 * @param mapViewer     the viewer the overlays are shown on
	 * @param layerManager  the stack the overlays are inserted at the bottom of
	 * @param baseFactory   the tile factory of the base map: its requests are cancelled, too, when the view moves on
	 * @param currentTime   the day (Julian day number) the maps must depict
	 */
	public HistoricalMapOverlays(final JXMapViewer mapViewer, final ChronomapLayerManager layerManager,
			final CancellableTileFactory baseFactory, final DoubleSupplier currentTime){
		this.mapViewer = mapViewer;
		this.layerManager = layerManager;
		this.baseFactory = baseFactory;
		this.totalMapZoom = baseFactory.getInfo()
			.getTotalMapZoom();
		this.currentTime = currentTime;

		//the tiles not requested yet belong to a view that is gone: the ones still on the screen are requested again by the next paint
		mapViewer.addPropertyChangeListener("center", evt -> viewChanged());
		mapViewer.addPropertyChangeListener("zoom", evt -> viewChanged());
	}

	private void viewChanged(){
		cancelPendingTiles();

		update();
	}

	private void cancelPendingTiles(){
		baseFactory.cancelPending();
		for(final CancellableTileFactory factory : mapWarperCache.values())
			factory.cancelPending();
	}


	/** Every tile factory has its own pool of threads and its cache of tiles: they are released with the window. */
	public void dispose(){
		for(final CancellableTileFactory factory : mapWarperCache.values())
			factory.dispose();
	}

	/** Creates a tile factory that caches the tiles on disk, in a folder named after the factory. */
	public static CancellableTileFactory createTileFactory(final TileFactoryInfo info){
		return createTileFactory(info, BASE_MAP_THREADS);
	}

	private static CancellableTileFactory createTileFactory(final TileFactoryInfo info, final int threads){
		final CancellableTileFactory factory = new CancellableTileFactory(info, threads);
		factory.setUserAgent("FamilyLegacy/1.0 (genealogy research)");

		final File cacheDir = new File(System.getProperty("user.home")
			+ File.separator + ".familylegacy" + File.separator + "tiles" + File.separator + info.getName().toLowerCase());
		factory.setLocalCache(new FileBasedLocalCache(cacheDir, false));
		return factory;
	}


	/** The time changed: the overlays follow it, and the maps are chosen again. */
	public void setCurrentTime(final double jdn){
		cancelPendingTiles();

		for(final MapWarperTileOverlayPainter overlay : historicalMapOverlays)
			overlay.setCurrentTime(jdn);

		update();
	}

	/**
	 * Chooses the historical map overlays to display based on the current time and map viewport center. When the auto-selection is
	 * on, this must be called on time changes and viewport panning.
	 * <p>
	 * All matching maps are shown simultaneously as a mosaic. They are sorted by area descending, so the smaller, more detailed maps
	 * are painted on top of the larger, coarser ones.
	 */
	public void update(){
		final double jdn = currentTime.getAsDouble();
		if(!autoSelectMapEnabled){
			//manual mode: keep only the maps that are still valid at the current time
			final List<HistoricalMapMetaData> stillValid = activeHistoricalMaps.stream()
				.filter(map -> map.isAvailableAt(jdn))
				.toList();
			if(stillValid.size() != activeHistoricalMaps.size())
				setHistoricalOverlays(stillValid);

			return;
		}

		final GeoPosition currentCenter = mapViewer.getCenterPosition();
		//the maps of the area are searched only when the area is looked at closely, not at the zoom of the whole world
		if(mapViewer.getZoom() <= HISTORICAL_SEARCH_ZOOM)
			historicalMapCatalog.requestArea(currentCenter, this::update);

		final List<HistoricalMapMetaData> maps = mapsAt(jdn, currentCenter);
		//sorted from the largest to the smallest: the smallest ones are kept
		setHistoricalOverlays(maps.subList(Math.max(0, maps.size() - MAX_AUTOMATIC_OVERLAYS), maps.size()));
	}

	/**
	 * The maps that depict the time and the place, from the largest to the smallest. Without a network, only the maps that have
	 * some tiles already downloaded.
	 */
	private List<HistoricalMapMetaData> mapsAt(final double currentJdn, final GeoPosition center){
		final boolean offline = historicalMapCatalog.isOffline();
		return historicalMapCatalog.maps().stream()
			.filter(map -> map.isMatching(currentJdn, center))
			.filter(map -> !offline || HistoricalMapCatalog.hasLocalTiles(map.mapId()))
			.sorted(Comparator.comparingDouble(HistoricalMapOverlays::areaOf).reversed())
			.toList();
	}

	/** The area of the bounding box of the map, in square degrees: only used to compare maps against each other. */
	private static double areaOf(final HistoricalMapMetaData map){
		final GeoPosition nw = map.northWest();
		final GeoPosition se = map.southEast();
		final double latSpan = Math.abs(nw.getLatitude() - se.getLatitude());
		final double lonSpan = Math.abs(nw.getLongitude() - se.getLongitude());
		return latSpan * lonSpan;
	}

	/**
	 * Replaces the current set of historical overlays with the given list, already sorted by area descending. Maps already active are
	 * kept as-is, maps no longer active are removed, and new maps are created and inserted at the bottom of the layer stack.
	 * <p>
	 * It does nothing when the new set is identical to the current one, so that panning does not cause flicker.
	 */
	private void setHistoricalOverlays(final List<HistoricalMapMetaData> maps){
		if(activeHistoricalMaps.equals(maps))
			return;

		//remove the overlays that are no longer needed
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

		//insert the new maps at the bottom of the stack, largest first: addLayer(0, ...) pushes everything else up, so iterating in
		//reverse order leaves the smallest map on top of the stack and the largest just above the base map
		for(int i = maps.size() - 1; i >= 0; i --){
			final HistoricalMapMetaData map = maps.get(i);
			final boolean alreadyActive = historicalMapOverlays.stream()
				.anyMatch(o -> o.mapId() == map.mapId());
			if(alreadyActive)
				continue;

			final MapWarperTileOverlayPainter overlay = new MapWarperTileOverlayPainter(map, getOrCreateMapWarperFactory(map.mapId()));
			overlay.setAlpha(HISTORICAL_OVERLAY_ALPHA);
			overlay.setCurrentTime(currentTime.getAsDouble());
			layerManager.addLayer(0, overlay);

			historicalMapOverlays.add(overlay);
		}

		//rebuild the metadata list from the overlay order so the two stay in sync
		activeHistoricalMaps.clear();
		for(final MapWarperTileOverlayPainter overlay : historicalMapOverlays)
			activeHistoricalMaps.add(overlay.metadata());

		mapViewer.repaint();
	}

	/** The factory is built on first use, with the same {@code totalMapZoom} as the base map, so that the overlay aligns with it. */
	private CancellableTileFactory getOrCreateMapWarperFactory(final int mapId){
		return mapWarperCache.computeIfAbsent(mapId, id -> createTileFactory(new MapWarperTileFactoryInfo(id, totalMapZoom),
			HISTORICAL_MAP_THREADS));
	}


	/** The button with the menu to choose the historical maps: automatically, none, or one of those of the time and place. */
	public JButton createButton(){
		final JButton button = new JButton(I18N.t("button.historical_map"));
		final JPopupMenu menu = new JPopupMenu();

		button.addActionListener(e -> {
			menu.removeAll();

			final JCheckBoxMenuItem autoItem = new JCheckBoxMenuItem(I18N.t("checkbox.map_auto_select"), autoSelectMapEnabled);
			autoItem.addActionListener(ev -> {
				autoSelectMapEnabled = autoItem.isSelected();
				if(autoSelectMapEnabled)
					update();
			});
			menu.add(autoItem);
			menu.addSeparator();

			//manual "None" (modern map only)
			final boolean noneActive = (!autoSelectMapEnabled && activeHistoricalMaps.isEmpty());
			final JRadioButtonMenuItem noneItem = new JRadioButtonMenuItem(I18N.t("checkbox.map_none"), noneActive);
			noneItem.addActionListener(ev -> {
				autoSelectMapEnabled = false;
				setHistoricalOverlays(List.of());
			});
			menu.add(noneItem);

			//manual override: selecting a map disables the auto-selection and replaces the whole set with just that map
			//only the maps of the time and place displayed: the catalog holds all the maps ever found
			for(final HistoricalMapMetaData map : mapsAt(currentTime.getAsDouble(), mapViewer.getCenterPosition())){
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

}
