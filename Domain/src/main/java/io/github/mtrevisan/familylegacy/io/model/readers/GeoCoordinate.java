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
package io.github.mtrevisan.familylegacy.io.model.readers;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A point on the Earth, as written in the coordinates of a place.
 *
 * @param latitude    the latitude in degrees, positive to the north
 * @param longitude   the longitude in degrees, positive to the east
 * @param uncertainty how uncertain the point is (meters; 0 if not known)
 */
public record GeoCoordinate(double latitude, double longitude, int uncertainty){

	/** The uncertainty of a point that is written in the data (meters), as opposed to one that was found by a search. */
	public static final int UNCERTAINTY_DIRECT = 10;

	/**
	 * Matches a coordinate pair in any of these forms:
	 * <ul>
	 *   <li>ISO 6709 signed decimal: {@code +45.650556+12.213333/}</li>
	 *   <li>decimal with hemisphere letters: {@code N45.650556 E12.213333},
	 *       {@code 45.650556N 12.213333E}, {@code N45.650556E12.213333}</li>
	 *   <li>decimal with hemisphere after both: {@code 45.650556 N 12.213333 E}</li>
	 *   <li>plain signed/unsigned decimal: {@code 45.650556, 12.213333},
	 *       {@code -33.86, 151.20}</li>
	 *   <li>any of the above with an optional degree symbol after the numbers</li>
	 * </ul>
	 * Group layout:
	 * <pre>
	 *   1: N/S before latitude (optional)
	 *   2: latitude number
	 *   3: N/S after latitude  (optional)
	 *   4: E/W before longitude (optional)
	 *   5: longitude number
	 *   6: E/W after longitude  (optional)
	 * </pre>
	 */
	private static final Pattern DECIMAL_COORDINATES = Pattern.compile(
		"\\s*"
			+ "([NS])?\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*°?\\s*([NS])?"
			+ "[,\\s]*"
			+ "([EW])?\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*°?\\s*([EW])?"
			+ "\\s*/?\\s*",
		Pattern.CASE_INSENSITIVE
	);


	public static GeoCoordinate parse(final String raw){
		if(StringUtils.isEmpty(raw))
			return null;

		final Matcher m = DECIMAL_COORDINATES.matcher(raw.trim());
		if(!m.matches())
			return null;

		final String latHemi = firstNonEmpty(m.group(1), m.group(3));
		final String lonHemi = firstNonEmpty(m.group(4), m.group(6));

		try{
			double lat = Double.parseDouble(m.group(2));
			double lon = Double.parseDouble(m.group(5));

			// When a hemisphere letter is present, it overrides the
			// sign of the number: the value is taken as an absolute
			// magnitude and the letter determines the direction.
			if(latHemi != null){
				lat = Math.abs(lat);
				if("S".equalsIgnoreCase(latHemi))
					lat = -lat;
			}
			if(lonHemi != null){
				lon = Math.abs(lon);
				if("W".equalsIgnoreCase(lonHemi))
					lon = -lon;
			}

			if(lat < -90. || lat > 90. || lon < -180. || lon > 180.)
				return null;
			return new GeoCoordinate(lat, lon, UNCERTAINTY_DIRECT);
		}
		catch(final NumberFormatException ignored){
			return null;
		}
	}

	private static String firstNonEmpty(final String a, final String b){
		return (a != null && !a.isEmpty()? a: b);
	}
}
