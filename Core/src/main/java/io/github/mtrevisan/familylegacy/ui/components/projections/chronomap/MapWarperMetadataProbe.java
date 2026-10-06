package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Standalone probe that queries the MapWarper API for the metadata of a
 * set of historical maps, and prints:
 * <ul>
 *   <li>the raw JSON returned by the API, so the available fields can
 *       be inspected;</li>
 *   <li>the extracted {@code date_depicted} field, if present;</li>
 *   <li>the year extracted from {@code date_depicted} or, as a
 *       fallback, from the title;</li>
 *   <li>the place extracted from the title or, as a fallback, from the
 *       center of the {@code bbox}.</li>
 * </ul>
 * Run it with:
 * <pre>
 *   javac --add-modules java.net.http MapWarperMetadataProbe.java
 *   java  --add-modules java.net.http io.github.mtrevisan.familylegacy.debug.MapWarperMetadataProbe
 * </pre>
 */
final class MapWarperMetadataProbe{

	private static final String API_URL = "https://mapwarper.net/api/v1/maps/%d.json";
	private static final String USER_AGENT = "FamilyLegacy/1.0 (genealogy research)";

	/**
	 * Matches a year range first, then a single year.
	 * <p>
	 * Recognised formats:
	 * <ul>
	 *   <li>{@code "1923"} → {@code [1923, 1923]}</li>
	 *   <li>{@code "1930-1950"}, {@code "1930–1950"}, {@code "1930—1950"},
	 *       {@code "1930/1950"}, {@code "1930 to 1950"} → {@code [1930, 1950]}</li>
	 * </ul>
	 * Years are limited to 1000-2099 to avoid false positives.
	 */
	private static final Pattern YEAR_RANGE = Pattern.compile(
		"\\b(1[0-9]{3}|20[0-9]{2})\\s*(?:-|–|—|/|\\s+to\\s+)\\s*(1[0-9]{3}|20[0-9]{2})\\b");

	private static final Pattern YEAR_SINGLE = Pattern.compile(
		"(1[0-9]{3}|20[0-9]{2})\\b");


	private MapWarperMetadataProbe(){}


	public static void main(final String[] args) throws IOException, InterruptedException{
		final HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();

		final int startID = 694;
//		for(final int mapId : MAP_IDS){
		for(int mapId = startID; mapId < startID + 1; mapId ++){
//			System.out.println("============================================================");
//			System.out.println("Map id: " + mapId);
//			System.out.println("============================================================");

			final String json = fetchMapJson(client, mapId);
			if(json == null){
				System.out.println("map id " + mapId + ": (no response)");
//				System.out.println("Raw JSON:");
//				System.out.println(json);
//				System.out.println();

				continue;
			}

			// Print the raw JSON so the user can see every available
			// field, including the ones not parsed below.
//			System.out.println("Raw JSON:");
//			System.out.println(json);
//			System.out.println();

			// Extract the fields we care about.
			final String title = extractString(json, "title");
			final String dateDepicted = extractString(json, "date_depicted");
//			final String description = extractString(json, "description");
			final String bbox = extractBbox(json);
			final String[] coords = StringUtils.split(bbox, ',');

//			System.out.println("title          = " + title);
//			System.out.println("date_depicted  = " + dateDepicted);
//			System.out.println("description    = " + description);
//			System.out.println("bbox           = " + bbox);
//			System.out.println();

			// Year: prefer date_depicted, fall back to the title.
			final Integer[] years = extractYears(dateDepicted, title);
			if(years == null){
				System.out.println("map id " + mapId + ": (no year), title: " + title);
//				System.out.println("Raw JSON:");
//				System.out.println(json);
//				System.out.println();

				continue;
			}
//			System.out.println("year           = " + (year != null? year: "(not found)"));
//			System.out.println();

			final String place = extractPlace(title, bbox);
			System.out.println("new HistoricalMapMetaData(" + mapId
				+ ", \"" + place + " [" + (Objects.equals(years[0], years[1])? years[0]: years[0] + "-" + years[1]) + "]"
				+ "\", MapWarperMetadataProbe.firstDayOfYear(" + years[0] + "), MapWarperMetadataProbe.lastDayOfYear(" + years[1] + "),"
				+ "\n\tnew GeoPosition(" + coords[3] + ", " + coords[0]
				+ "), new GeoPosition(" + coords[1] + ", " + coords[2] + ")),");
		}
	}

	/**
	 * Gregorian Julian Day Number for the given date.
	 * <p>
	 * Standard algorithm: all divisions are integer divisions.
	 *
	 * @param year  the Gregorian year (can be negative for BCE)
	 * @param month the month, 1-12
	 * @param day   the day of the month, 1-31
	 * @return the Julian Day Number
	 */
	public static long toJdn(final int year, final int month, final int day){
		final int a = (14 - month) / 12;
		final int y = year + 4800 - a;
		final int m = month + 12 * a - 3;
		return day + (153L * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045L;
	}

	/**
	 * JDN of January 1 of the given year.
	 *
	 * @param year the Gregorian year
	 * @return the JDN of {@code year-01-01}
	 */
	public static long firstDayOfYear(final int year){
		return toJdn(year, 1, 1);
	}

	/**
	 * JDN of December 31 of the given year.
	 *
	 * @param year the Gregorian year
	 * @return the JDN of {@code year-12-31}
	 */
	public static long lastDayOfYear(final int year){
		return toJdn(year, 12, 31);
	}


	/* ======================================================================
	 *                          HTTP
	 * ====================================================================== */

	private static String fetchMapJson(final HttpClient client, final int mapId)
		throws IOException, InterruptedException{
		final String url = String.format(API_URL, mapId);
		final HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.header("User-Agent", USER_AGENT)
			.timeout(Duration.ofSeconds(10))
			.GET()
			.build();

		final HttpResponse<String> response = client.send(request,
			HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if(response.statusCode() != 200){
			System.err.println("HTTP " + response.statusCode() + " for " + url);

			return null;
		}
		return response.body();
	}


	/* ======================================================================
	 *                          JSON parsing
	 * ====================================================================== */

	/**
	 * Extracts a top-level string field from a flat JSON object. This is
	 * deliberately naive: it is only meant for the MapWarper metadata
	 * response, which is a flat JSON object with string or numeric
	 * values, and it does not need to be a general-purpose parser.
	 * Nested objects are not handled.
	 */
	private static String extractString(final String json, final String key){
		final Pattern p = Pattern.compile(
			"\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"",
			Pattern.DOTALL);
		final Matcher m = p.matcher(json);
		return (m.find()? unescape(m.group(1)): null);
	}

	/**
	 * Extracts the {@code bbox} field. Depending on the API version, the
	 * bbox can be a string in the form {@code "minLon,minLat,maxLon,maxLat"}
	 * or an array of four numbers. Both are handled: the raw substring is
	 * returned, trimmed of brackets and quotes.
	 */
	private static String extractBbox(final String json){
		final Pattern asString = Pattern.compile("\"bbox\"\\s*:\\s*\"([^\"]+)\"", Pattern.DOTALL);
		final Matcher ms = asString.matcher(json);
		if(ms.find())
			return ms.group(1).trim();

		final Pattern asArray = Pattern.compile("\"bbox\"\\s*:\\s*\\[([^\\]]+)\\]", Pattern.DOTALL);
		final Matcher ma = asArray.matcher(json);
		if(ma.find()){
			// Normalise "[a, b, c, d]" to "a,b,c,d".
			return ma.group(1)
				.replaceAll("\\s+", "")
				.replace("\"", "");
		}
		return null;
	}

	private static String unescape(final String s){
		return s.replace("\\\"", "\"")
			.replace("\\n", "\n")
			.replace("\\t", "\t")
			.replace("\\\\", "\\");
	}


	/* ======================================================================
	 *                          Extraction
	 * ====================================================================== */

	/**
	 * Extracts the temporal range expressed in {@code dateDepicted} or, as
	 * a fallback, in the title.
	 * <p>
	 * The result is always a two-element array:
	 * <ul>
	 *   <li>{@code [0]} is the start year;</li>
	 *   <li>{@code [1]} is the end year.</li>
	 * </ul>
	 * When only one year is present, the same value is stored in both
	 * positions. When a range is present, the two bounds are used in the
	 * order they appear. Returns {@code null} when neither source contains
	 * a parseable year.
	 *
	 * @param dateDepicted the {@code date_depicted} field, may be {@code null}
	 * @param title        the map title, may be {@code null}
	 * @return {@code [startYear, endYear]}, or {@code null}
	 */
	private static Integer[] extractYears(final String dateDepicted, final String title){
		Integer[] years = parseYears(dateDepicted);
		if(years != null)
			return years;
		return parseYears(title);
	}

	/**
	 * Parses a single string into a {@code [startYear, endYear]} pair.
	 * Returns {@code null} when no year is present.
	 */
	private static Integer[] parseYears(final String text){
		if(text == null || text.isBlank())
			return null;

		// 1. Try a range first, so that "1930-1950" is captured as a whole.
		final Matcher rangeMatcher = YEAR_RANGE.matcher(text);
		if(rangeMatcher.find()){
			final int start = Integer.parseInt(rangeMatcher.group(1));
			final int end = Integer.parseInt(rangeMatcher.group(2));
			return new Integer[]{start, end};
		}

		// 2. Fall back to a single year, repeated in both positions.
		final Matcher singleMatcher = YEAR_SINGLE.matcher(text);
		if(singleMatcher.find()){
			final int year = Integer.parseInt(singleMatcher.group(1));
			return new Integer[]{year, year};
		}

		return null;
	}


	/**
	 * Extracts the place from the title by removing every year or year range
	 * and any surrounding punctuation and whitespace.
	 * <p>
	 * The place is therefore "everything else" in the title:
	 * <ul>
	 *   <li>{@code "Italy 1790"} → {@code "Italy"}</li>
	 *   <li>{@code "Italy 1930-1950"} → {@code "Italy"}</li>
	 *   <li>{@code "Washington DC Streetcars in 1888"} → {@code "Washington DC Streetcars in"}</li>
	 *   <li>{@code "Northern Italy, 1815"} → {@code "Northern Italy"}</li>
	 *   <li>{@code "Map of France (1790)"} → {@code "Map of France"}</li>
	 * </ul>
	 * When the title is {@code null}, empty, or contains only the year, the
	 * method falls back to the center of the {@code bbox}.
	 *
	 * @param title the map title, may be {@code null}
	 * @param bbox  the bounding box in {@code "minLon,minLat,maxLon,maxLat"}
	 *              format, may be {@code null}
	 * @return the place label, or {@code null}
	 */
	private static String extractPlace(final String title, final String bbox){
		final String fromTitle = extractPlaceFromTitle(title);
		if(fromTitle != null)
			return fromTitle;
		return extractPlaceFromBbox(bbox);
	}

	/**
	 * Removes every year or year range from the title and cleans up the
	 * surrounding punctuation and whitespace.
	 * <p>
	 * Uses {@code replaceAll} rather than {@code replaceFirst}: a title can
	 * contain more than one year (for instance a range plus a reference
	 * year), and all of them must be stripped. Uses {@code \p{Z}} in
	 * addition to {@code \s} to handle Unicode whitespace such as the
	 * non-breaking space (U+00A0), which is common in titles scraped from
	 * the web and is not matched by {@code \s} in Java's default mode.
	 */
	private static String extractPlaceFromTitle(final String title){
		if(title == null || title.isBlank())
			return null;

		// Remove every year range, then every single year. The order
		// matters: removing ranges first prevents "1930-1950" from being
		// reduced to "-1950" by the single-year pattern.
		String cleaned = YEAR_RANGE.matcher(title).replaceAll(" ");
		cleaned = YEAR_SINGLE.matcher(cleaned).replaceAll(" ");

		// Collapse any run of whitespace (including Unicode spaces) into a
		// single regular space.
		cleaned = cleaned.replaceAll("[\\s\\p{Z}]+", " ");

		// Strip leading and trailing punctuation, including spaces and
		// dashes. This handles titles like "(1790) Italy" → "Italy" and
		// "Italy, 1790," → "Italy".
		cleaned = cleaned.replaceAll("^[\\p{Punct}\\s]+", "");
		cleaned = cleaned.replaceAll("[\\p{Punct}\\s]+$", "");

		return (cleaned.isEmpty()? null: cleaned);
	}

	/**
	 * Center of the bounding box, formatted as {@code "lat, lon"}.
	 * The MapWarper bbox format is {@code "minLon,minLat,maxLon,maxLat"}.
	 */
	private static String extractPlaceFromBbox(final String bbox){
		if(bbox == null || bbox.isBlank())
			return null;

		final String[] parts = bbox.split(",");
		if(parts.length != 4)
			return null;

		try{
			final double minLon = Double.parseDouble(parts[0].trim());
			final double minLat = Double.parseDouble(parts[1].trim());
			final double maxLon = Double.parseDouble(parts[2].trim());
			final double maxLat = Double.parseDouble(parts[3].trim());
			final double centerLat = (minLat + maxLat) / 2.;
			final double centerLon = (minLon + maxLon) / 2.;
			return String.format("%.4f, %.4f", centerLat, centerLon);
		}
		catch(final NumberFormatException ignored){
			return null;
		}
	}

}
