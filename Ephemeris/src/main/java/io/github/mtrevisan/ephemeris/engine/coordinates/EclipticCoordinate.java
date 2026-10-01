/**
 * Copyright (c) 2023 Mauro Trevisan
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
package io.github.mtrevisan.ephemeris.engine.coordinates;

import io.github.mtrevisan.ephemeris.helpers.StringHelper;

import java.text.DecimalFormat;


/**
 * (Polar) Ecliptic coordinates.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Ecliptic_coordinate_system">Ecliptic coordinate system</>
 */
public final class EclipticCoordinate{

	//[rad]
	private final double latitude;
	//[rad]
	private final double longitude;
	//[AU]
	private final double distance;


	/**
	 * Creates a new instance with the given parameters.
	 *
	 * @param latitude	The latitude [rad].
	 * @param longitude	The longitude [rad].
	 * @param distance	The distance [AU].
	 * @return	An instance.
	 */
	public static EclipticCoordinate create(final double latitude, final double longitude, final double distance){
		return new EclipticCoordinate(latitude, longitude, distance);
	}


	public EclipticCoordinate(final double latitude, final double longitude, final double distance){
		if(Double.isNaN(latitude) || Double.isInfinite(latitude))
			throw new IllegalArgumentException("Not finite latitude: " + latitude);
		if(Double.isNaN(longitude) || Double.isInfinite(longitude))
			throw new IllegalArgumentException("Not finite longitude: " + longitude);
		if(Double.isNaN(distance) || Double.isInfinite(distance))
			throw new IllegalArgumentException("Not finite distance: " + distance);

		this.latitude = latitude;
		this.longitude = longitude;
		this.distance = distance;
	}


	/**
	 * The latitude.
	 *
	 * @return	The latitude [rad].
	 */
	public double getLatitude(){
		return latitude;
	}

	/**
	 * The longitude.
	 *
	 * @return	The longitude [rad].
	 */
	public double getLongitude(){
		return longitude;
	}

	/**
	 * The distance.
	 *
	 * @return	The distance [AU].
	 */
	public double getDistance(){
		return distance;
	}

	@Override
	public String toString(){
		final DecimalFormat df = StringHelper.decimalFormat(2);
		return "EclipticCoordinate{"
			+ "b: " + StringHelper.degreeToDegMinSecString(StrictMath.toDegrees(latitude), 2)
			+ ", l: " + StringHelper.degreeToDegMinSecString(StrictMath.toDegrees(longitude), 2)
			+ ", r: " + df.format(distance)
			+ '}';
	}

}
