package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;
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

	// Estimated uncertainty radii in meters
	static final int UNCERTAINTY_DIRECT = 10;
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

	@FunctionalInterface
	public interface GeocodingProgressListener{
		void onProgress(int current, int total, String placeName);
	}

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
	 */
	public int geocodePlaces(final List<FLEFRecord> places, final GeocodingProgressListener progress,
			final Supplier<Boolean> isCancelled){
		int resolvedCount = 0;
		final int total = places.size();

		// Runtime cache for individual query attempts during the current session
		final Map<String, ChronomapIndex.GeoCoordinate> runtimeQueryCache = new HashMap<>();

		for(int i = 0; i < total; i ++){
			if(isCancelled != null && Boolean.TRUE.equals(isCancelled.get()))
				break;

			final FLEFRecord place = places.get(i);
			final String placeId = place.getId();
			final List<String> rawNames = PlaceReader.extractNames(place);
			final String primaryName = (!rawNames.isEmpty() ? rawNames.getFirst() : placeId);

			if(progress != null)
				progress.onProgress(i + 1, total, primaryName);

			ChronomapIndex.GeoCoordinate bestCoord = null;
			String bestMatchedQuery = null;

			// Generate progressive left-truncated variants for each name
			final List<String> queryCandidates = generateProgressiveCandidates(rawNames);

			for(final String query : queryCandidates){
				if(isCancelled != null && Boolean.TRUE.equals(isCancelled.get())){
					saveDiskCache();

					return resolvedCount;
				}

				ChronomapIndex.GeoCoordinate candidateCoord = null;

				// 1. Check persistent disk cache first
				final String cachedProp = diskCache.getProperty(query);
				if(cachedProp != null)
					candidateCoord = parseCoordinateString(cachedProp);
				else if(runtimeQueryCache.containsKey(query))
					// 2. Check in-memory runtime session cache
					candidateCoord = runtimeQueryCache.get(query);
				else{
					// 3. Query Nominatim API for new unvisited search string
					candidateCoord = geocodeNominatimSingle(query, UNCERTAINTY_GEOCODED_FULL);
					runtimeQueryCache.put(query, candidateCoord);
				}

				// Keep candidate if it yields a smaller uncertainty radius
				if(candidateCoord != null)
					if(bestCoord == null || candidateCoord.uncertainty() < bestCoord.uncertainty()){
						bestCoord = candidateCoord;
						bestMatchedQuery = query;
					}
			}

			// Store best resolved coordinate and map all original place names to disk cache
			if(bestCoord != null){
				cache.put(placeId, new Resolved(bestCoord, Source.GEOCODED, bestMatchedQuery));
				resolvedCount ++;

				final String coordValue = bestCoord.latitude() + "," + bestCoord.longitude() + "," + bestCoord.uncertainty();
				for(final String rawName : rawNames)
					diskCache.setProperty(rawName, coordValue);
				saveDiskCache();
			}
			else if(!cache.containsKey(placeId)){
				// Fallback: Direct FLEF coordinates
				final ChronomapIndex.GeoCoordinate directCoord = directCoordinates(place);
				if(directCoord != null){
					cache.put(placeId, new Resolved(directCoord, Source.DIRECT, null));

					resolvedCount ++;
				}
			}
		}

		return resolvedCount;
	}

	/**
	 * Generates a list of query variants by stripping leading comma-separated blocks.
	 * E.g., ["via Pasubio, Treviso, Italia"] -> ["via Pasubio, Treviso, Italia", "Treviso, Italia", "Italia"]
	 */
	private static List<String> generateProgressiveCandidates(final List<String> rawNames){
		final List<String> candidates = new ArrayList<>();
		final Set<String> seen = new HashSet<>();
		for(final String name : rawNames){
			String current = (name != null? name.trim(): "");
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

	List<FLEFRecord> extractMissingPlaces(){
		final List<FLEFRecord> places = model.getRecordsByType(PlaceHandler.TYPE);
		final List<FLEFRecord> unlocatedPlaces = new ArrayList<>();
		for(final FLEFRecord place : places){
			final String placeId = place.getId();
			if(placeId != null && !cache.containsKey(placeId)){
				final List<String> names = PlaceReader.extractNames(place);
				if(!names.isEmpty())
					unlocatedPlaces.add(place);
			}
		}
		return unlocatedPlaces;
	}

	List<FLEFRecord> extractAllPlaces(){
		return model.getRecordsByType(PlaceHandler.TYPE);
	}

	private ChronomapIndex.GeoCoordinate geocodeNominatimSingle(final String queryText, final int defaultUncertainty){
		enforceRateLimit();

		try(final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()){
			final String url = NOMINATIM_URL + URLEncoder.encode(queryText, StandardCharsets.UTF_8);
			final HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("User-Agent", USER_AGENT)
				.timeout(Duration.ofSeconds(10))
				.GET()
				.build();

			final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if(response.statusCode() != 200){
				LOGGER.warn("Nominatim returned HTTP {} for query '{}'", response.statusCode(), queryText);

				return null;
			}

			final String body = response.body();
			final Matcher mLatLon = NOMINATIM_LATLON.matcher(body);
			if(!mLatLon.find())
				return null;

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

			return new ChronomapIndex.GeoCoordinate(lat, lon, uncertainty);
		}
		catch(final IOException | InterruptedException | NumberFormatException e){
			if(e instanceof InterruptedException)
				Thread.currentThread().interrupt();

			LOGGER.debug("Geocoding failed for query '{}': {}", queryText, e.getMessage());
			return null;
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
		final List<FLEFRecord> places = model.getRecordsByType(PlaceHandler.TYPE);
		for(final FLEFRecord place : places){
			final String placeId = place.getId();
			if(placeId == null)
				continue;

			final ChronomapIndex.GeoCoordinate c = directCoordinates(place);
			if(c == null)
				continue;

			cache.put(placeId, new Resolved(c, Source.DIRECT, null));
			queue.add(placeId);
		}

		final Map<String, List<String>> childrenOf = new HashMap<>();
		final List<FLEFRecord> placeRelationships = model.getRecordsByType(PlaceRelationshipHandler.TYPE);
		for(final FLEFRecord placeRelationship : placeRelationships){
			final String type = PlaceRelationshipReader.extractType(placeRelationship);
			if(type == null || !PART_OF_TYPES.contains(type))
				continue;

			final String childId = extractPlaceRef(placeRelationship, PlaceRelationshipReader.TAG_SUBJECT);
			final String objectId = extractPlaceRef(placeRelationship, PlaceRelationshipReader.TAG_OBJECT);
			if(childId == null || objectId == null)
				continue;

			childrenOf.computeIfAbsent(objectId, k -> new ArrayList<>()).add(childId);
		}

		while(!queue.isEmpty()){
			final String parentId = queue.poll();
			final Resolved parent = cache.get(parentId);
			if(parent == null)
				continue;

			for(final String childId : childrenOf.getOrDefault(parentId, List.of())){
				if(cache.containsKey(childId))
					continue;

				// Assign inherited coordinate with hierarchy uncertainty radius
				final ChronomapIndex.GeoCoordinate inheritedCoord = new ChronomapIndex.GeoCoordinate(
					parent.coordinate().latitude(),
					parent.coordinate().longitude(),
					UNCERTAINTY_HIERARCHY
				);

				cache.put(childId, new Resolved(inheritedCoord, Source.HIERARCHY, parent.matchedName()));
				queue.add(childId);
			}
		}
	}

	private static ChronomapIndex.GeoCoordinate directCoordinates(final FLEFRecord place){
		final String coords = PlaceReader.extractCoordinates(place);
		if(coords == null)
			return null;

		final ChronomapIndex.GeoCoordinate parsed = ChronomapIndex.GeoCoordinate.parse(coords);
		return (parsed != null
			? new ChronomapIndex.GeoCoordinate(parsed.latitude(), parsed.longitude(), UNCERTAINTY_DIRECT)
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

	private static ChronomapIndex.GeoCoordinate parseCoordinateString(final String val){
		if(val == null || val.isEmpty() || NOT_FOUND.equals(val))
			return null;

		final String[] parts = val.split(",");
		if(parts.length < 2)
			return null;

		try{
			final double lat = Double.parseDouble(parts[0]);
			final double lon = Double.parseDouble(parts[1]);
			final int uncertainty = (parts.length >= 3
				? (int)Math.ceil(Double.parseDouble(parts[2]))
				: UNCERTAINTY_GEOCODED_FULL);
			return new ChronomapIndex.GeoCoordinate(lat, lon, uncertainty);
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
