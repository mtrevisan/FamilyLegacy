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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.GeoCoordinate;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;
import org.apache.commons.lang3.StringUtils;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Resolves spatial coordinates for place records via background Nominatim geocoding
 * with progressive left-truncation and disk cache mapping optimization.
 */
public final class PlaceCoordinateResolver{

	private static final Logger LOGGER = LoggerFactory.getLogger(PlaceCoordinateResolver.class);


	private static final String NOT_FOUND = "NOT_FOUND";
	private static final List<String> PART_OF_TYPES = new ArrayList<>(List.of(PlaceRelationshipReader.TYPES));

	/** How many places, at most, are added to a place to make its address (street, town, province, region, country, ...). */
	private static final int MAX_CONTAINERS = 8;

	// Estimated uncertainty radii in meters
	static final int UNCERTAINTY_DIRECT = GeoCoordinate.UNCERTAINTY_DIRECT;
	private static final int UNCERTAINTY_GEOCODED_FULL = 1_000;
	private static final int UNCERTAINTY_HIERARCHY = 15_000;

	private static final Pattern NOMINATIM_BOUNDINGBOX = Pattern.compile(
		"\"boundingbox\"\\s*:\\s*\\[\\s*\"([^\"]+)\"\\s*,\\s*\"([^\"]+)\"\\s*,\\s*\"([^\"]+)\"\\s*,\\s*\"([^\"]+)\"\\s*\\]", Pattern.DOTALL);

	private static final double EARTH_RADIUS_METERS = 6371000.0;

	private static final Pattern NOMINATIM_LATLON = Pattern.compile(
		"\"lat\"\\s*:\\s*\"([^\"]+)\".*?\"lon\"\\s*:\\s*\"([^\"]+)\"", Pattern.DOTALL);

	private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=";
	private static final String USER_AGENT = "FamilyLegacy/1.0 (genealogy research)";
	private static final long MIN_REQUEST_INTERVAL_MS = 1100;

	/**
	 * Outcome of a single Nominatim query.
	 * <p>
	 * Three states are possible:
	 * <ul>
	 *   <li>a coordinate: the query was answered and a place was found;</li>
	 *   <li>{@link #NOT_FOUND}: the query was answered, but Nominatim has
	 *       no matching place. This outcome is persisted to the disk cache,
	 *       so the same query is not re-issued on subsequent runs;</li>
	 *   <li>{@link #ERROR}: the query could not be answered (network
	 *       error, timeout, HTTP error). This outcome is <em>not</em>
	 *       persisted, because it is transient: the query is retried on
	 *       the next run.</li>
	 * </ul>
	 */
	private record NominatimOutcome(GeoCoordinate coordinate, boolean responded){

		static final NominatimOutcome ERROR = new NominatimOutcome(null, false);
		static final NominatimOutcome NOT_FOUND = new NominatimOutcome(null, true);

		static NominatimOutcome found(final GeoCoordinate c){
			return new NominatimOutcome(c, true);
		}
	}

	@FunctionalInterface
	public interface GeocodingProgressListener{
		void onProgress(int current, int total, String placeName);
	}

	public enum Source{
		DIRECT,
		HIERARCHY,
		GEOCODED
	}

	public record Resolved(GeoCoordinate coordinate, Source source, String matchedName){}


	private final FLEFModel model;
	private final Map<String, Resolved> cache = new HashMap<>();
	/** The place that contains each place (the street is in the town, the town in the province, ...). */
	private final Map<String, String> containerOf = new HashMap<>();
	private final Properties diskCache = new Properties();
	private final Path diskCacheFile;
	private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

	private long lastRequestMs = 0L;


	public PlaceCoordinateResolver(final FLEFModel model, final Path diskCacheFile){
		this.model = model;
		this.diskCacheFile = diskCacheFile;

		loadDiskCache();
		buildHierarchyCache();
	}


	public Resolved resolve(final String placeId){
		return (placeId != null? cache.get(placeId): null);
	}

	public int cachedCount(){
		return cache.size();
	}

	/**
	 * Geocodes places using progressive left-truncation. In-memory session cache stores
	 * individual query outcomes (including NOT_FOUND), whereas persistent disk storage maps
	 * all original place names to the final resolved coordinates with minimal uncertainty.
	 * <p>
	 * The caller is expected to pass only the places returned by
	 * {@link #extractMissingPlaces()}, so that the progress bar reflects
	 * actual network work rather than a pass over already-cached data.
	 */
	public int geocodePlaces(final List<FLEFRecord> places, final GeocodingProgressListener progress,
			final Supplier<Boolean> isCancelled){
		int resolvedCount = 0;
		final int total = places.size();

		// Runtime cache for individual query attempts during the current session
		final Map<String, NominatimOutcome> runtimeQueryCache = new HashMap<>();

		for(int i = 0; i < total; i ++){
			if(isCancelled != null && Boolean.TRUE.equals(isCancelled.get()))
				break;

			final FLEFRecord place = places.get(i);
			final String placeId = place.getId();
			final List<String> rawNames = PlaceReader.extractNames(place);
			//a place records only its own name: the address to look for is made of the names of the places that contain it too
			final List<String> addresses = fullAddresses(place);
			final String primaryName = (!addresses.isEmpty()? addresses.getFirst(): (!rawNames.isEmpty()? rawNames.getFirst(): placeId));

			if(progress != null)
				progress.onProgress(i + 1, total, primaryName);

			GeoCoordinate bestCoord = null;
			String bestMatchedQuery = null;

			// Generate progressive left-truncated variants for each name
			final List<String> queryCandidates = generateProgressiveCandidates(addresses);

			for(final String query : queryCandidates){
				if(isCancelled != null && Boolean.TRUE.equals(isCancelled.get())){
					saveDiskCache();

					return resolvedCount;
				}

				NominatimOutcome outcome;

				// 1. Check persistent disk cache first
				final String cachedProp = diskCache.getProperty(query);
				if(cachedProp != null)
					// Persisted outcome: either a coordinate or NOT_FOUND.
					outcome = (NOT_FOUND.equals(cachedProp)
						? NominatimOutcome.NOT_FOUND
						: NominatimOutcome.found(parseCoordinateString(cachedProp)));
				else if(runtimeQueryCache.containsKey(query))
					// 2. Check in-memory runtime session cache
					outcome = runtimeQueryCache.get(query);
				else{
					// 3. Query Nominatim API for new unvisited search string
					outcome = geocodeNominatimSingle(query, UNCERTAINTY_GEOCODED_FULL);
					runtimeQueryCache.put(query, outcome);

					// Persist the definitive "no result" outcome immediately,
					// so the next run will skip this query instead of
					// re-issuing the same request.
					if(outcome == NominatimOutcome.NOT_FOUND){
						diskCache.setProperty(query, NOT_FOUND);

						saveDiskCache();
					}
				}

				// Keep candidate if it yields a smaller uncertainty radius
				if(outcome.coordinate() != null)
					if(bestCoord == null || outcome.coordinate().uncertainty() < bestCoord.uncertainty()){
						bestCoord = outcome.coordinate();
						bestMatchedQuery = query;
					}
			}

			// Store best resolved coordinate and map all original place names to disk cache
			if(bestCoord != null){
				cache.put(placeId, new Resolved(bestCoord, Source.GEOCODED, bestMatchedQuery));
				resolvedCount ++;

				final String coordValue = bestCoord.latitude() + "," + bestCoord.longitude() + "," + bestCoord.uncertainty();
				for(final String address : addresses)
					diskCache.setProperty(address, coordValue);

				saveDiskCache();
			}
			else if(!cache.containsKey(placeId)){
				// Fallback: Direct FLEF coordinates
				final GeoCoordinate directCoord = directCoordinates(place);
				if(directCoord != null){
					cache.put(placeId, new Resolved(directCoord, Source.DIRECT, null));

					resolvedCount ++;
				}
			}
		}

		return resolvedCount;
	}

	/**
	 * The names of the place, each one completed with the names of the places that contain it: "via Mattielli 3" is looked
	 * for as "via Mattielli 3, Vicenza, VI, Italia".
	 * <p>
	 * A place records one thing only (a street, linked to the town, linked to the province, ...), and a search for the bare name
	 * of a street, or of a town, finds the wrong place (or none).
	 *
	 * @param place	The place.
	 * @return	The addresses, one for each name of the place; an address that contains a question mark is uncertain, so it is
	 * 	left out (and a place with no address left is not geocoded).
	 */
	List<String> fullAddresses(final FLEFRecord place){
		final List<String> containers = containerNames(place.getId());

		final List<String> addresses = new ArrayList<>();
		for(final String name : PlaceReader.extractNames(place)){
			final String address = compose(name, containers);
			if(address.indexOf('?') < 0 && !addresses.contains(address))
				addresses.add(address);
		}
		return addresses;
	}

	/** @return	The names of the places that contain the place, from the nearest to the farthest (the ones the privacy hides excluded). */
	private List<String> containerNames(final String placeId){
		final List<String> names = new ArrayList<>();
		if(placeId == null)
			return names;

		//the data may contain a cycle
		final Set<String> visited = new HashSet<>();
		visited.add(placeId);
		String container = containerOf.get(placeId);
		while(container != null && names.size() < MAX_CONTAINERS && visited.add(container)){
			final FLEFRecord record = model.getRecordById(container);
			//the name of a place the privacy hides must not be sent to the geocoding service
			if(record == null || PrivacyService.visible(model, List.of(record)).isEmpty())
				break;

			final String name = PlaceReader.extractPrimaryName(record);
			if(name != null)
				names.add(name);
			container = containerOf.get(container);
		}
		return names;
	}

	/**
	 * @param name	The name of a place (it may already be made of several parts, separated by commas).
	 * @param containers	The names of the places that contain it, from the nearest to the farthest.
	 * @return	The name followed by the names of the containers, without repeating a part that the name already has.
	 */
	static String compose(final String name, final List<String> containers){
		final List<String> parts = new ArrayList<>();
		final Set<String> inName = new HashSet<>();
		for(final String part : StringUtils.split(name, ',')){
			final String normalized = StringUtils.normalizeSpace(part);
			if(!normalized.isEmpty()){
				parts.add(normalized);
				inName.add(normalized.toLowerCase(Locale.ROOT));
			}
		}
		for(final String container : containers)
			for(final String part : StringUtils.split(container, ',')){
				final String normalized = StringUtils.normalizeSpace(part);
				if(!normalized.isEmpty() && !inName.contains(normalized.toLowerCase(Locale.ROOT)))
					parts.add(normalized);
			}
		return String.join(", ", parts);
	}

	/**
	 * Generates a list of query variants by stripping leading comma-separated blocks.
	 * E.g., ["via Pasubio, Treviso, Italia"] -> ["via Pasubio, Treviso, Italia", "Treviso, Italia", "Italia"]
	 */
	private static List<String> generateProgressiveCandidates(final List<String> rawNames){
		final List<String> candidates = new ArrayList<>();
		final Set<String> seen = new HashSet<>();
		for(final String name : rawNames){
			String current = (name != null? name.trim(): StringUtils.EMPTY);
			while(!current.isEmpty()){
				if(seen.add(current))
					candidates.add(current);
				final int commaIdx = current.indexOf(',');
				if(commaIdx >= 0)
					current = current.substring(commaIdx + 1).trim();
				else
					break;
			}
		}
		return candidates;
	}

	/**
	 * Returns only the places that still require a network geocoding call.
	 * <p>
	 * A place is considered "missing" when:
	 * <ul>
	 *   <li>its id is not in the in-memory cache (direct coordinates,
	 *       hierarchy inheritance, or a geocoding result already
	 *       warmed in this session);</li>
	 *   <li>none of its progressive name candidates is present in the
	 *       on-disk geocoding cache.</li>
	 * </ul>
	 * When a place's name is found in the disk cache, the in-memory cache
	 * is warmed immediately as a side effect, so the rest of the
	 * application (index, markers) sees the resolved coordinate without
	 * waiting for the worker. Places warmed this way are not returned.
	 * <p>
	 * This is the method that should be passed to
	 * {@link #geocodePlaces(List, GeocodingProgressListener, Supplier)}:
	 * when it returns an empty list, the caller can skip the worker (and
	 * its progress dialog) entirely.
	 *
	 * @return the places that need a real Nominatim lookup; never {@code null}
	 */
	List<FLEFRecord> extractMissingPlaces(){
		final List<FLEFRecord> unlocatedPlaces = new ArrayList<>();
		int warmed = 0;

		final List<FLEFRecord> places = PrivacyService.visible(model, model.getRecordsByType(PlaceHandler.TYPE));
		for(final FLEFRecord place : places){
			final Lookup lookup = lookup(place);
			if(lookup == null)
				continue;

			if(lookup.resolved() != null){
				// Warm the in-memory cache so the index sees the place
				// immediately, without waiting for the worker. This does
				// not require any network call.
				cache.put(place.getId(), lookup.resolved());
				warmed ++;
			}
			else
				unlocatedPlaces.add(place);
		}

		if(warmed > 0)
			LOGGER.debug("Warmed {} places from the on-disk geocoding cache", warmed);
		LOGGER.debug("Places needing network geocoding: {}", unlocatedPlaces.size());

		return unlocatedPlaces;
	}

	/**
	 * Like {@link #extractMissingPlaces()}, but it only tells whether there is something left to geocode, and it does not touch
	 * the in-memory cache.
	 */
	boolean hasMissingPlaces(){
		final List<FLEFRecord> places = PrivacyService.visible(model, model.getRecordsByType(PlaceHandler.TYPE));
		for(final FLEFRecord place : places){
			final Lookup lookup = lookup(place);
			if(lookup != null && lookup.resolved() == null)
				return true;
		}
		return false;
	}

	/** What the caches say about a place: a coordinate (resolved) or nothing (still to be searched). */
	private record Lookup(Resolved resolved){}

	/**
	 * @param place	The place.
	 * @return	{@code null} if there is nothing to do (the place is already located, has no usable address, or was searched
	 * 	without any result), a lookup with the coordinate if the on-disk cache has it, a lookup without it if a real search is needed.
	 */
	private Lookup lookup(final FLEFRecord place){
		final String placeId = place.getId();
		if(placeId == null)
			return null;
		//a place that only inherits the position of the place that contains it (a street at the position of its town) is still to be looked for
		final Resolved known = cache.get(placeId);
		if(known != null && known.source() != Source.HIERARCHY)
			return null;

		if(PlaceReader.extractNames(place).isEmpty())
			return null;

		final List<String> addresses = fullAddresses(place);
		final List<String> candidates = generateProgressiveCandidates(addresses);
		//an uncertain place (with a question mark) is not geocoded
		if(candidates.isEmpty())
			return null;

		GeoCoordinate bestCoord = null;
		String bestName = null;
		int unknownCandidates = 0;
		int notFoundCandidates = 0;
		for(final String candidate : candidates){
			final String cachedProp = diskCache.getProperty(candidate);
			if(cachedProp == null){
				unknownCandidates ++;

				continue;
			}
			if(NOT_FOUND.equals(cachedProp)){
				notFoundCandidates ++;

				continue;
			}
			//only the complete address says that the place itself was searched (the coordinate is stored once the search is over):
			//a coordinate found for a shorter address (the town of a street) says nothing about the place
			if(!addresses.contains(candidate))
				continue;

			final GeoCoordinate coord = parseCoordinateString(cachedProp);
			if(coord != null)
				if(bestCoord == null || coord.uncertainty() < bestCoord.uncertainty()){
					bestCoord = coord;
					bestName = candidate;
				}
		}

		if(bestCoord != null)
			return new Lookup(new Resolved(bestCoord, Source.GEOCODED, bestName));

		// No coordinate in the disk cache.
		// If every candidate is marked NOT_FOUND, the place has been
		// resolved before and Nominatim had nothing for it: skip it so
		// the worker is not scheduled on every panel creation.
		if(unknownCandidates == 0 && notFoundCandidates > 0)
			return null;

		// Some candidates are still unknown: a real lookup is needed.
		return new Lookup(null);
	}

	private NominatimOutcome geocodeNominatimSingle(final String queryText, final int defaultUncertainty){
		enforceRateLimit();

		try{
			final String url = NOMINATIM_URL + URLEncoder.encode(queryText, StandardCharsets.UTF_8);
			final HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("User-Agent", USER_AGENT)
				.timeout(Duration.ofSeconds(10))
				.GET()
				.build();

			final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if(response.statusCode() != 200){
				LOGGER.warn("Nominatim returned HTTP {} for query '{}'", response.statusCode(), queryText);

				// HTTP-level failure: treat as transient, do not persist.
				return NominatimOutcome.ERROR;
			}

			final String body = response.body();
			final Matcher mLatLon = NOMINATIM_LATLON.matcher(body);
			if(!mLatLon.find()){
				// The query was answered, but Nominatim has no matching place.
				// This is a definitive outcome and can be cached.
				return NominatimOutcome.NOT_FOUND;
			}

			final double lat = Double.parseDouble(mLatLon.group(1));
			final double lon = Double.parseDouble(mLatLon.group(2));

			// Extract boundingbox and calculate uncertainty radius
			int uncertainty = defaultUncertainty;
			final Matcher mBbox = NOMINATIM_BOUNDINGBOX.matcher(body);
			if(mBbox.find()){
				try{
					final double minLat = Double.parseDouble(mBbox.group(1));
					final double maxLat = Double.parseDouble(mBbox.group(2));
					final double minLon = Double.parseDouble(mBbox.group(3));
					final double maxLon = Double.parseDouble(mBbox.group(4));

					// Calculate distance from center to bounding box corner (approximate radius)
					uncertainty = (int)Math.ceil(calculateHaversineDistance(lat, lon, maxLat, maxLon));
				}
				catch(final NumberFormatException ignored){}
			}

			return NominatimOutcome.found(new GeoCoordinate(lat, lon, uncertainty));
		}
		catch(final InterruptedException ie){
			Thread.currentThread().interrupt();

			LOGGER.debug("Geocoding interrupted for query '{}'", queryText);

			return NominatimOutcome.ERROR;
		}
		catch(final IOException | NumberFormatException e){
			LOGGER.debug("Geocoding failed for query '{}': {}", queryText, e.getMessage());

			// Network-level failure: transient, do not persist.
			return NominatimOutcome.ERROR;
		}
	}

	private static double calculateHaversineDistance(final double lat1, final double lon1, final double lat2,
			final double lon2){
		final double dLat = Math.toRadians(lat2 - lat1);
		final double dLon = Math.toRadians(lon2 - lon1);
		final double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
			+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
			* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return EARTH_RADIUS_METERS * c;
	}

	private void enforceRateLimit(){
		final long now = System.currentTimeMillis();
		final long wait = MIN_REQUEST_INTERVAL_MS - (now - lastRequestMs);
		if(wait > 0){
			try{
				Thread.sleep(wait);
			}
			catch(final InterruptedException ie){
				Thread.currentThread().interrupt();
			}
		}
		lastRequestMs = System.currentTimeMillis();
	}

	private void buildHierarchyCache(){
		final Deque<String> queue = new ArrayDeque<>();
		final List<FLEFRecord> places = PrivacyService.visible(model, model.getRecordsByType(PlaceHandler.TYPE));
		for(final FLEFRecord place : places){
			final String placeId = place.getId();
			if(placeId == null)
				continue;

			final GeoCoordinate c = directCoordinates(place);
			if(c == null)
				continue;

			cache.put(placeId, new Resolved(c, Source.DIRECT, null));
			queue.add(placeId);
		}

		final Map<String, List<String>> childrenOf = new HashMap<>();
		final Map<String, String> administrativeContainerOf = new HashMap<>();
		final Map<String, String> geographicContainerOf = new HashMap<>();
		final List<FLEFRecord> placeRelationships = PrivacyService.visible(model, model.getRecordsByType(PlaceRelationshipHandler.TYPE));
		for(final FLEFRecord placeRelationship : placeRelationships){
			final String type = PlaceRelationshipReader.extractType(placeRelationship);
			if(type == null || !PART_OF_TYPES.contains(type))
				continue;

			final String childId = extractPlaceRef(placeRelationship, PlaceRelationshipReader.TAG_SUBJECT);
			final String objectId = extractPlaceRef(placeRelationship, PlaceRelationshipReader.TAG_TARGET);
			if(childId == null || objectId == null)
				continue;

			if(PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF.equals(type))
				administrativeContainerOf.putIfAbsent(childId, objectId);
			else if("geographic_part_of".equals(type))
				geographicContainerOf.putIfAbsent(childId, objectId);

			childrenOf.computeIfAbsent(objectId, k -> new ArrayList<>())
				.add(childId);
		}

		//only the containment makes an address (not the ecclesiastical or judicial ones, nor the history of the place), and the administrative one first
		containerOf.putAll(geographicContainerOf);
		containerOf.putAll(administrativeContainerOf);

		while(!queue.isEmpty()){
			final String parentId = queue.poll();
			final Resolved parent = cache.get(parentId);
			if(parent == null)
				continue;

			for(final String childId : childrenOf.getOrDefault(parentId, List.of())){
				if(cache.containsKey(childId))
					continue;

				// Assign inherited coordinate with hierarchy uncertainty radius
				final GeoCoordinate inheritedCoord = new GeoCoordinate(
					parent.coordinate().latitude(),
					parent.coordinate().longitude(),
					UNCERTAINTY_HIERARCHY
				);

				cache.put(childId, new Resolved(inheritedCoord, Source.HIERARCHY, parent.matchedName()));
				queue.add(childId);
			}
		}
	}

	private static GeoCoordinate directCoordinates(final FLEFRecord place){
		final String coords = PlaceReader.extractCoordinates(place);
		if(coords == null)
			return null;

		final GeoCoordinate parsed = GeoCoordinate.parse(coords);
		return (parsed != null
			? new GeoCoordinate(parsed.latitude(), parsed.longitude(), UNCERTAINTY_DIRECT)
			: null);
	}

	private static String extractPlaceRef(final FLEFRecord rel, final String fieldTag){
		final FLEFRecord field = FLEFRecordHelper.findChild(rel, fieldTag);
		if(field == null)
			return null;

		final FLEFRecord placeRef = FLEFRecordHelper.findChild(field, PlaceHandler.TYPE);
		if(placeRef != null && placeRef.getValue() != null)
			return placeRef.getValue();

		final FLEFRecord inner = field.getTheOnlyChild();
		if(inner != null){
			final FLEFRecord placeInner = FLEFRecordHelper.findChild(inner, PlaceHandler.TYPE);
			if(placeInner != null && placeInner.getValue() != null)
				return placeInner.getValue();
		}
		return null;
	}

	private static GeoCoordinate parseCoordinateString(final String val){
		if(StringUtils.isEmpty(val) || NOT_FOUND.equals(val))
			return null;

		final String[] parts = StringUtils.split(val, ',');
		if(parts.length < 2)
			return null;

		try{
			final double lat = Double.parseDouble(parts[0]);
			final double lon = Double.parseDouble(parts[1]);
			final int uncertainty = (parts.length >= 3
				? (int)Math.ceil(Double.parseDouble(parts[2]))
				: UNCERTAINTY_GEOCODED_FULL);
			return new GeoCoordinate(lat, lon, uncertainty);
		}
		catch(final NumberFormatException ignored){
			return null;
		}
	}

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

	private synchronized void saveDiskCache(){
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
