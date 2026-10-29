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

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Searches the georeferenced maps hosted on MapWarper, through its public API.
 * <p>
 * The API answers a JSON:API document, in which every map has its title, its bounding box, its status and (sometimes) the year it
 * depicts. A map is usable only if it is already warped and if a year can be found, either in {@code date_depicted} or in the title.
 */
final class MapWarperSearch{

	/** The maps of a page of results. */
	record Page(List<HistoricalMapMetaData> maps, int totalPages){}


	private static final String API_URL = "https://mapwarper.net/api/v1/maps?format=json&operation=intersects&field=title"
		+ "&per_page=%d&page=%d&bbox=%s&query=%s";
	private static final String USER_AGENT = "FamilyLegacy/1.0 (genealogy research)";
	private static final String STATUS_WARPED = "warped";
	/** Some titles are a whole description. */
	private static final int MAX_NAME_LENGTH = 80;

	private static final Pattern MAP_START = Pattern.compile("\"id\":\"(\\d+)\",\"type\":\"maps\",\"attributes\":\\{");
	private static final String RELATIONSHIPS_KEY = "\"relationships\"";
	private static final Pattern TOTAL_PAGES = Pattern.compile("\"total_pages\":(\\d+)");

	/**
	 * Matches a year range first, then a single year ("1930-1950", "1930/1950", "1930 to 1950").
	 * The years are limited to 1000-2099 to avoid false positives.
	 */
	private static final Pattern YEAR_RANGE = Pattern.compile(
		"\\b(1[0-9]{3}|20[0-9]{2})\\s*(?:-|–|—|/|\\s+to\\s+)\\s*(1[0-9]{3}|20[0-9]{2})\\b");
	private static final Pattern YEAR_SINGLE = Pattern.compile("(1[0-9]{3}|20[0-9]{2})\\b");


	private MapWarperSearch(){}


	/**
	 * @param client	The HTTP client.
	 * @param minLongitude	The west limit of the area.
	 * @param minLatitude	The south limit of the area.
	 * @param maxLongitude	The east limit of the area.
	 * @param maxLatitude	The north limit of the area.
	 * @param titlePrefix	The text the title has to contain (a century, like {@code 18}: the dates are in the title more often than
	 * 	in the {@code date_depicted} field).
	 * @param pageSize	The number of maps in a page.
	 * @param page	The number of the page (the first one is {@code 1}).
	 * @return	The maps that intersect the area, that are warped, and that have a year.
	 * @throws IOException	If the service cannot be reached or does not answer.
	 */
	static Page fetch(final HttpClient client, final double minLongitude, final double minLatitude, final double maxLongitude,
			final double maxLatitude, final String titlePrefix, final int pageSize, final int page) throws IOException, InterruptedException{
		final String bbox = URLEncoder.encode(minLongitude + "," + minLatitude + "," + maxLongitude + "," + maxLatitude,
			StandardCharsets.UTF_8);
		final String url = String.format(API_URL, pageSize, page, bbox, URLEncoder.encode(titlePrefix, StandardCharsets.UTF_8));
		final HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.header("User-Agent", USER_AGENT)
			.timeout(Duration.ofSeconds(20))
			.GET()
			.build();

		final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if(response.statusCode() != 200)
			throw new IOException("HTTP " + response.statusCode() + " for " + url);

		return parse(response.body());
	}

	/**
	 * Reads a page of results.
	 * <p>
	 * It is not a general purpose JSON parser: it only reads the few fields of the attributes of every map.
	 *
	 * @param json	The answer of the service.
	 * @return	The usable maps of the page, and the number of pages of the whole search.
	 */
	static Page parse(final String json){
		final List<HistoricalMapMetaData> maps = new ArrayList<>();

		final Matcher starts = MAP_START.matcher(json);
		final List<int[]> bounds = new ArrayList<>();
		final List<Integer> ids = new ArrayList<>();
		while(starts.find()){
			if(!bounds.isEmpty())
				bounds.getLast()[1] = starts.start();
			bounds.add(new int[]{starts.end(), json.length()});
			ids.add(Integer.parseInt(starts.group(1)));
		}
		for(int i = 0; i < ids.size(); i ++){
			final String attributes = attributesOf(json.substring(bounds.get(i)[0], bounds.get(i)[1]));

			final HistoricalMapMetaData map = toMap(ids.get(i), attributes);
			if(map != null)
				maps.add(map);
		}

		final Matcher totalPages = TOTAL_PAGES.matcher(json);
		return new Page(maps, (totalPages.find()? Integer.parseInt(totalPages.group(1)): 1));
	}

	/** The attributes end where the relationships of the map begin: the rest is about other objects. */
	private static String attributesOf(final String segment){
		final int end = segment.indexOf(RELATIONSHIPS_KEY);
		return (end >= 0? segment.substring(0, end): segment);
	}

	private static HistoricalMapMetaData toMap(final int id, final String attributes){
		if(!STATUS_WARPED.equals(stringField(attributes, "status")))
			return null;

		final String title = stringField(attributes, "title");
		final int[] years = years(stringField(attributes, "date_depicted"), title);
		final GeoPosition[] corners = corners(stringField(attributes, "bbox"));
		if(years == null || corners == null)
			return null;

		return new HistoricalMapMetaData(id, nameOf(title, years), firstDayOfYear(years[0]), lastDayOfYear(years[1]),
			corners[0], corners[1]);
	}

	private static String nameOf(final String title, final int[] years){
		final String period = (years[0] == years[1]? Integer.toString(years[0]): years[0] + "-" + years[1]);
		final String name = StringUtils.abbreviate(StringUtils.normalizeSpace(title), MAX_NAME_LENGTH);
		if(StringUtils.isEmpty(name))
			return period;

		//the year is not repeated if the title already says it
		return (name.contains(Integer.toString(years[0]))? name: name + " [" + period + "]");
	}

	/**
	 * The year (or the period) depicted: {@code date_depicted} first, then the title.
	 *
	 * @return	The first and the last year, or {@code null} if there is no year.
	 */
	static int[] years(final String dateDepicted, final String title){
		final int[] years = parseYears(dateDepicted);
		return (years != null? years: parseYears(title));
	}

	private static int[] parseYears(final String text){
		if(StringUtils.isEmpty(text))
			return null;

		final Matcher range = YEAR_RANGE.matcher(text);
		if(range.find()){
			final int start = Integer.parseInt(range.group(1));
			final int end = Integer.parseInt(range.group(2));
			return new int[]{Math.min(start, end), Math.max(start, end)};
		}

		final Matcher single = YEAR_SINGLE.matcher(text);
		if(single.find()){
			final int year = Integer.parseInt(single.group(1));
			return new int[]{year, year};
		}
		return null;
	}

	/**
	 * @param bbox	The bounding box, as {@code minLongitude,minLatitude,maxLongitude,maxLatitude}.
	 * @return	The north-west and the south-east corners, or {@code null} if the box is missing or not valid.
	 */
	static GeoPosition[] corners(final String bbox){
		if(StringUtils.isBlank(bbox))
			return null;

		final String[] parts = StringUtils.split(bbox, ',');
		if(parts.length != 4)
			return null;

		try{
			final double minLongitude = Double.parseDouble(parts[0].trim());
			final double minLatitude = Double.parseDouble(parts[1].trim());
			final double maxLongitude = Double.parseDouble(parts[2].trim());
			final double maxLatitude = Double.parseDouble(parts[3].trim());
			if(minLatitude < -90. || maxLatitude > 90. || minLongitude < -180. || maxLongitude > 180.
					|| minLatitude >= maxLatitude || minLongitude >= maxLongitude)
				return null;

			return new GeoPosition[]{new GeoPosition(maxLatitude, minLongitude), new GeoPosition(minLatitude, maxLongitude)};
		}
		catch(final NumberFormatException ignored){
			return null;
		}
	}

	/** @return	The value of a string field of the attributes ({@code null} if missing), with the escapes resolved. */
	private static String stringField(final String attributes, final String key){
		final Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\":\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(attributes);
		return (matcher.find()? unescape(matcher.group(1)): null);
	}

	private static String unescape(final String text){
		if(text.indexOf('\\') < 0)
			return text;

		final StringBuilder sb = new StringBuilder(text.length());
		for(int i = 0; i < text.length(); i ++){
			final char c = text.charAt(i);
			if(c != '\\' || i + 1 == text.length()){
				sb.append(c);

				continue;
			}

			final char next = text.charAt(++ i);
			switch(next){
				case 'n', 'r', 't' -> sb.append(' ');
				case 'u' -> {
					if(i + 4 < text.length()){
						try{
							sb.append((char)Integer.parseInt(text.substring(i + 1, i + 5), 16));
							i += 4;
						}
						catch(final NumberFormatException ignored){
							sb.append(next);
						}
					}
					else
						sb.append(next);
				}
				default -> sb.append(next);
			}
		}
		return sb.toString();
	}


	/** Julian Day Number of January 1 of the given Gregorian year. */
	static long firstDayOfYear(final int year){
		return gregorianToJdn(year, 1, 1);
	}

	/** Julian Day Number of December 31 of the given Gregorian year. */
	static long lastDayOfYear(final int year){
		return gregorianToJdn(year, 12, 31);
	}

	private static long gregorianToJdn(final int year, final int month, final int day){
		final int a = (14 - month) / 12;
		final int y = year + 4800 - a;
		final int m = month + 12 * a - 3;
		return day + (153L * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045L;
	}

}
