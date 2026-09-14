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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceRelationshipHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Resolves the coordinates of a place, in this order:
 * <ol>
 *   <li>direct {@code place.map.coordinates};</li>
 *   <li>inherited from a parent place via a {@code place_relationship}
 *       of type {@code administrative_part_of} (or any of the other
 *       part-of types), walking up the hierarchy;</li>
 *   <li>geocoded via Nominatim, with a local disk cache.</li>
 * </ol>
 * The first two steps are offline and run at construction time. The
 * third step is opt-in and runs in the background.
 */
public final class PlaceCoordinateResolver{

	private static final Logger LOGGER = LoggerFactory.getLogger(PlaceCoordinateResolver.class);


	private static final String TAG_TYPE = "type";
	private static final String TAG_MAP = "map";
	private static final String TAG_COORDINATES = "coordinates";
	private static final String TAG_NAME = "name";
	private static final String TAG_VALUE = "value";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";

	/** Relationship types that mean "subject is part of target". */
	private static final List<String> PART_OF_TYPES = List.of(
		"administrative_part_of", "geographic_part_of",
		"ecclesiastical_part_of", "judicial_part_of", "cadastral_part_of");

	/** Nominatim response: {@code [{"lat":"45.65","lon":"12.21",...}]}. */
	private static final Pattern NOMINATIM_LATLON = Pattern.compile(
		"\"lat\"\\s*:\\s*\"([^\"]+)\".*?\"lon\"\\s*:\\s*\"([^\"]+)\"", Pattern.DOTALL);

	private static final String NOMINATIM_URL =
		"https://nominatim.openstreetmap.org/search?format=json&limit=1&q=";
	private static final String USER_AGENT =
		"FamilyLegacy/1.0 (genealogy research)";

	/** Nominatim usage policy: max 1 request per second. */
	private static final long MIN_REQUEST_INTERVAL_MS = 1100;


	public enum Source{
		DIRECT,
		HIERARCHY,
		GEOCODED
	}

	public record Resolved(ChronomapIndex.GeoCoordinate coordinate, Source source, String matchedName){}


	private final FLEFModel model;
	private final Map<String, Resolved> cache = new HashMap<>();
	private final Properties diskCache = new Properties();
	private final Path diskCacheFile;


	public PlaceCoordinateResolver(final FLEFModel model, final Path diskCacheFile){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");

		this.model = model;
		this.diskCacheFile = diskCacheFile;

		loadDiskCache();
		buildHierarchyCache();
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	public Resolved resolve(final String placeId){
		return (placeId != null? cache.get(placeId): null);
	}

	public int cachedCount(){
		return cache.size();
	}

	/**
	 * Geocodes every place that still has no coordinates. Blocking: the
	 * caller is responsible for running it on a background thread.
	 * <p>
	 * Results are cached on disk, so subsequent runs do not hit the
	 * network for places that have already been resolved.
	 *
	 * @param progress callback invoked with the place id for each new
	 *                 resolution; may be {@code null}
	 * @return the number of newly resolved places
	 */
	public int geocodeMissingPlaces(final Consumer<String> progress){
		int resolved = 0;
		long lastRequestMs = 0l;

		for(final FLEFRecord place : model.getRecordsByType(PlaceHandler.TYPE)){
			final String placeId = place.getId();
			if(placeId == null || cache.containsKey(placeId))
				continue;

			final String name = primaryName(place);
			if(name == null || name.isBlank())
				continue;

			// Disk cache hit: no network, no rate limit
			final ChronomapIndex.GeoCoordinate fromDisk = lookupDiskCache(name);
			if(fromDisk != null){
				cache.put(placeId, new Resolved(fromDisk, Source.GEOCODED, name));
				resolved ++;
				if(progress != null)
					progress.accept(placeId);

				continue;
			}

			// Rate limit Nominatim.
			final long now = System.currentTimeMillis();
			final long wait = MIN_REQUEST_INTERVAL_MS - (now - lastRequestMs);
			if(wait > 0){
				try{
					Thread.sleep(wait);
				}
				catch(final InterruptedException ie){
					Thread.currentThread().interrupt();
					return resolved;
				}
			}
			lastRequestMs = System.currentTimeMillis();

			final ChronomapIndex.GeoCoordinate c = geocode(name);
			if(c == null)
				continue;

			diskCache.setProperty(name, c.latitude() + "," + c.longitude());
			cache.put(placeId, new Resolved(c, Source.GEOCODED, name));
			resolved ++;
			if(progress != null)
				progress.accept(placeId);
		}
		saveDiskCache();
		return resolved;
	}


	/* ======================================================================
	 *                          Hierarchy
	 * ====================================================================== */

	/**
	 * Builds the coordinate cache by walking the place hierarchy:
	 * <ol>
	 *   <li>every place with direct coordinates is cached as {@link Source#DIRECT};</li>
	 *   <li>BFS over the {@code part_of} graph, so every descendant of a
	 *       located place inherits its coordinates as {@link Source#HIERARCHY}.</li>
	 * </ol>
	 * The BFS uses a visited set, so malformed cycles in the place graph
	 * do not cause infinite loops.
	 */
	private void buildHierarchyCache(){
		// 1. Direct coordinates
		final Deque<String> queue = new ArrayDeque<>();
		for(final FLEFRecord place : model.getRecordsByType(PlaceHandler.TYPE)){
			final String placeId = place.getId();
			if(placeId == null)
				continue;

			final ChronomapIndex.GeoCoordinate c = directCoordinates(place);
			if(c == null)
				continue;

			cache.put(placeId, new Resolved(c, Source.DIRECT, null));
			queue.add(placeId);
		}

		// 2. Build the child adjacency: parentId -> [childId, ...]
		final Map<String, List<String>> childrenOf = new HashMap<>();
		for(final FLEFRecord rel : model.getRecordsByType(PlaceRelationshipHandler.TYPE)){
			final String type = FLEFRecordHelper.getChildValue(rel, TAG_TYPE);
			if(type == null || !PART_OF_TYPES.contains(type))
				continue;

			final String childId = extractPlaceRef(rel, TAG_SUBJECT);
			final String parentId = extractPlaceRef(rel, TAG_TARGET);
			if(childId == null || parentId == null)
				continue;

			childrenOf.computeIfAbsent(parentId, k -> new ArrayList<>()).add(childId);
		}

		// 3. BFS from located places to their descendants
		while(!queue.isEmpty()){
			final String parentId = queue.poll();
			final Resolved parent = cache.get(parentId);
			if(parent == null)
				continue;

			for(final String childId : childrenOf.getOrDefault(parentId, List.of())){
				if(cache.containsKey(childId))
					continue;

				cache.put(childId, new Resolved(parent.coordinate(), Source.HIERARCHY, parent.matchedName()));
				queue.add(childId);
			}
		}

		LOGGER.debug("Place coordinate cache: {} entries ({} direct, {} inherited)",
			cache.size(),
			cache.values().stream().filter(r -> r.source() == Source.DIRECT).count(),
			cache.values().stream().filter(r -> r.source() == Source.HIERARCHY).count());
	}

	private static ChronomapIndex.GeoCoordinate directCoordinates(final FLEFRecord place){
		final FLEFRecord mapStruct = FLEFRecordHelper.findChild(place, TAG_MAP);
		if(mapStruct == null)
			return null;

		final String coords = FLEFRecordHelper.getChildValue(mapStruct, TAG_COORDINATES);
		return ChronomapIndex.GeoCoordinate.parse(coords);
	}

	private static String extractPlaceRef(final FLEFRecord rel, final String fieldTag){
		final FLEFRecord field = FLEFRecordHelper.findChild(rel, fieldTag);
		if(field == null)
			return null;

		final FLEFRecord placeRef = FLEFRecordHelper.findChild(field, PlaceHandler.TYPE);
		if(placeRef != null && placeRef.getValue() != null)
			return placeRef.getValue();

		// Try one level deeper.
		final FLEFRecord inner = field.getTheOnlyChild();
		if(inner != null){
			final FLEFRecord placeInner = FLEFRecordHelper.findChild(inner, PlaceHandler.TYPE);
			if(placeInner != null && placeInner.getValue() != null)
				return placeInner.getValue();
		}
		return null;
	}


	/* ======================================================================
	 *                          Geocoding
	 * ====================================================================== */

	private static String primaryName(final FLEFRecord place){
		for(final FLEFRecord nameStruct : FLEFRecordHelper.findChildren(place, TAG_NAME)){
			final String v = FLEFRecordHelper.getChildValue(nameStruct, TAG_VALUE);
			if(v != null && !v.isBlank())
				return v;
		}
		return null;
	}

	private ChronomapIndex.GeoCoordinate lookupDiskCache(final String name){
		final String s = diskCache.getProperty(name);
		if(s == null)
			return null;

		final int comma = s.indexOf(',');
		if(comma < 0)
			return null;

		try{
			return new ChronomapIndex.GeoCoordinate(
				Double.parseDouble(s.substring(0, comma)),
				Double.parseDouble(s.substring(comma + 1)));
		}
		catch(final NumberFormatException ignored){
			return null;
		}
	}

	private static ChronomapIndex.GeoCoordinate geocode(final String placeName){
		try{
			final String url = NOMINATIM_URL + URLEncoder.encode(placeName, StandardCharsets.UTF_8);
			final HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(5))
				.build();
			final HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("User-Agent", USER_AGENT)
				.timeout(Duration.ofSeconds(10))
				.GET()
				.build();

			final HttpResponse<String> response = client.send(request,
				HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if(response.statusCode() != 200){
				LOGGER.warn("Nominatim returned {} for '{}'", response.statusCode(), placeName);

				return null;
			}

			final Matcher m = NOMINATIM_LATLON.matcher(response.body());
			if(!m.find())
				return null;

			final double lat = Double.parseDouble(m.group(1));
			final double lon = Double.parseDouble(m.group(2));
			return new ChronomapIndex.GeoCoordinate(lat, lon);
		}
		catch(final IOException | InterruptedException | NumberFormatException e){
			if(e instanceof InterruptedException)
				Thread.currentThread().interrupt();

			LOGGER.debug("Geocoding failed for '{}': {}", placeName, e.getMessage());

			return null;
		}
	}


	/* ======================================================================
	 *                          Disk cache
	 * ====================================================================== */

	private void loadDiskCache(){
		if(diskCacheFile == null || !Files.exists(diskCacheFile))
			return;

		try(final InputStream is = Files.newInputStream(diskCacheFile)){
			diskCache.load(is);
		}
		catch(final IOException e){
			LOGGER.debug("Could not load geocoding cache: {}", e.getMessage());
		}
	}

	private void saveDiskCache(){
		if(diskCacheFile == null)
			return;

		try{
			Files.createDirectories(diskCacheFile.getParent());
			try(final OutputStream os = Files.newOutputStream(diskCacheFile)){
				diskCache.store(os, "FamilyLegacy geocoding cache");
			}
		}
		catch(final IOException e){
			LOGGER.debug("Could not save geocoding cache: {}", e.getMessage());
		}
	}

}
