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

import org.apache.commons.lang3.StringUtils;
import org.jxmapviewer.viewer.GeoPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * The historical maps (served by MapWarper) that the chronomap can overlay on the base map.
 * <p>
 * The catalog is not a fixed list: the maps of an area are searched on the service when the area is displayed for the first time, and
 * remembered on disk, so that the area is searched again only after a month, and the maps are still available without a network.
 * Without a network only the maps whose tiles were downloaded before are offered ({@link #hasLocalTiles(int)}), because the others
 * could not be drawn.
 */
final class HistoricalMapCatalog{

	private static final Logger LOGGER = LoggerFactory.getLogger(HistoricalMapCatalog.class);

	/** The world is divided in cells of this size, and every cell is searched once. */
	private static final double CELL_DEGREES = 4.;
	private static final long CELL_VALIDITY_MS = Duration.ofDays(30).toMillis();
	/** A cell that could not be searched (no network) is not tried again before this time. */
	private static final long RETRY_AFTER_MS = Duration.ofMinutes(5).toMillis();
	private static final long REQUEST_INTERVAL_MS = 1100;
	private static final int PAGE_SIZE = 100;
	private static final int MAX_PAGES = 5;
	/** The centuries searched (the years are in the title of the maps more often than in their date field). */
	private static final List<String> TITLE_PREFIXES = List.of("14", "15", "16", "17", "18", "19");

	private static final String KEY_MAP = "map.";
	private static final String KEY_CELL = "cell.";
	private static final char SEPARATOR = '\t';


	private final Path file;
	private final Properties store = new Properties();
	private final Map<Integer, HistoricalMapMetaData> maps = new ConcurrentHashMap<>();
	private volatile List<HistoricalMapMetaData> snapshot = List.of();
	/** The cells being searched. */
	private final Set<String> searching = ConcurrentHashMap.newKeySet();
	private final Map<String, Long> failedAt = new ConcurrentHashMap<>();
	private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
	private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
		final Thread thread = new Thread(runnable, "historical-map-catalog");
		thread.setDaemon(true);
		return thread;
	});
	/** Whether the last search could not reach the service. */
	private volatile boolean offline;


	/** @param file	Where the maps found are remembered ({@code null} to not remember them). */
	HistoricalMapCatalog(final Path file){
		this.file = file;

		load();
	}


	/** @return	Every map known so far (the ones that were found for the areas displayed). */
	List<HistoricalMapMetaData> maps(){
		return snapshot;
	}

	/** @return	Whether the service could not be reached the last time, so that only the maps with local tiles can be shown. */
	boolean isOffline(){
		return offline;
	}

	/**
	 * Whether some tiles of the map were already downloaded.
	 *
	 * @param mapId	The identifier of the map.
	 */
	static boolean hasLocalTiles(final int mapId){
		final Path directory = Path.of(System.getProperty("user.home"), ".familylegacy", "tiles", "mapwarper-" + mapId);
		if(!Files.isDirectory(directory))
			return false;

		try(final var content = Files.list(directory)){
			return content.findAny().isPresent();
		}
		catch(final IOException ignored){
			return false;
		}
	}

	/**
	 * Makes sure that the maps of the area around the position are known: if the area was not searched yet (or too long ago), the
	 * search is started in the background.
	 *
	 * @param center	The position displayed.
	 * @param onChange	What to do (in the UI thread) when a search is over, or has found something new.
	 */
	void requestArea(final GeoPosition center, final Runnable onChange){
		if(center == null)
			return;

		final int cellX = (int)Math.floor((center.getLongitude() + 180.) / CELL_DEGREES);
		final int cellY = (int)Math.floor((center.getLatitude() + 90.) / CELL_DEGREES);
		final String cell = cellX + "." + cellY;

		final long now = System.currentTimeMillis();
		if(now - searchedAt(cell) < CELL_VALIDITY_MS || now - failedAt.getOrDefault(cell, 0L) < RETRY_AFTER_MS)
			return;
		if(!searching.add(cell))
			return;

		executor.execute(() -> {
			try{
				search(cell, cellX, cellY, onChange);
			}
			finally{
				searching.remove(cell);
			}
		});
	}

	private void search(final String cell, final int cellX, final int cellY, final Runnable onChange){
		final double minLongitude = cellX * CELL_DEGREES - 180.;
		final double minLatitude = cellY * CELL_DEGREES - 90.;
		try{
			boolean first = true;
			for(final String prefix : TITLE_PREFIXES){
				int totalPages = 1;
				for(int page = 1; page <= Math.min(totalPages, MAX_PAGES); page ++){
					if(!first)
						Thread.sleep(REQUEST_INTERVAL_MS);
					first = false;

					final MapWarperSearch.Page result = MapWarperSearch.fetch(httpClient, minLongitude, minLatitude,
						minLongitude + CELL_DEGREES, minLatitude + CELL_DEGREES, prefix, PAGE_SIZE, page);
					totalPages = result.totalPages();

					if(add(result.maps()))
						notifyChange(onChange);
				}
			}

			store.setProperty(KEY_CELL + cell, Long.toString(System.currentTimeMillis()));
			save();
			offline = false;
			failedAt.remove(cell);

			LOGGER.debug("Historical maps of the cell {} searched: {} maps known", cell, maps.size());
		}
		catch(final IOException ioe){
			offline = true;
			failedAt.put(cell, System.currentTimeMillis());

			LOGGER.debug("Historical maps of the cell {} not searched: {}", cell, ioe.getMessage());
		}
		catch(final InterruptedException ie){
			Thread.currentThread().interrupt();
		}
		//the search is over (or failed): the caller can use what it has, whether it is offline or not
		notifyChange(onChange);
	}

	private static void notifyChange(final Runnable onChange){
		if(onChange != null)
			SwingUtilities.invokeLater(onChange);
	}

	/** @return	Whether at least one map is new. */
	private boolean add(final List<HistoricalMapMetaData> found){
		boolean added = false;
		for(final HistoricalMapMetaData map : found)
			if(maps.put(map.mapId(), map) == null){
				store.setProperty(KEY_MAP + map.mapId(), serialize(map));

				added = true;
			}
		if(added){
			snapshot = List.copyOf(maps.values());

			save();
		}
		return added;
	}

	private long searchedAt(final String cell){
		final String value = store.getProperty(KEY_CELL + cell);
		if(value == null)
			return 0L;

		try{
			return Long.parseLong(value);
		}
		catch(final NumberFormatException ignored){
			return 0L;
		}
	}


	private static String serialize(final HistoricalMapMetaData map){
		return StringUtils.replaceChars(map.name(), SEPARATOR, ' ') + SEPARATOR + map.startJdn() + SEPARATOR + map.endJdn()
			+ SEPARATOR + map.northWest().getLatitude() + SEPARATOR + map.northWest().getLongitude()
			+ SEPARATOR + map.southEast().getLatitude() + SEPARATOR + map.southEast().getLongitude();
	}

	private static HistoricalMapMetaData deserialize(final int mapId, final String value){
		final String[] parts = StringUtils.splitPreserveAllTokens(value, SEPARATOR);
		if(parts.length != 7)
			return null;

		try{
			return new HistoricalMapMetaData(mapId, parts[0], Long.parseLong(parts[1]), Long.parseLong(parts[2]),
				new GeoPosition(Double.parseDouble(parts[3]), Double.parseDouble(parts[4])),
				new GeoPosition(Double.parseDouble(parts[5]), Double.parseDouble(parts[6])));
		}
		catch(final NumberFormatException ignored){
			return null;
		}
	}

	private void load(){
		if(file == null || !Files.isRegularFile(file))
			return;

		try(final InputStream is = Files.newInputStream(file)){
			store.load(is);
		}
		catch(final IOException ioe){
			LOGGER.debug("Could not load the historical maps: {}", ioe.getMessage());

			return;
		}

		for(final String key : store.stringPropertyNames())
			if(key.startsWith(KEY_MAP))
				try{
					final int mapId = Integer.parseInt(key.substring(KEY_MAP.length()));
					final HistoricalMapMetaData map = deserialize(mapId, store.getProperty(key));
					if(map != null)
						maps.put(mapId, map);
				}
				catch(final NumberFormatException ignored){
					//not a map
				}
		snapshot = List.copyOf(maps.values());
	}

	private synchronized void save(){
		if(file == null)
			return;

		try{
			Files.createDirectories(file.getParent());
			try(final OutputStream os = Files.newOutputStream(file)){
				store.store(os, "FamilyLegacy historical maps");
			}
		}
		catch(final IOException ioe){
			LOGGER.debug("Could not save the historical maps: {}", ioe.getMessage());
		}
	}

}
