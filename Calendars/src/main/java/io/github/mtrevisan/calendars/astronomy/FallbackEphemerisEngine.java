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
package io.github.mtrevisan.calendars.astronomy;


public final class FallbackEphemerisEngine implements EphemerisEngine{

	@Override
	public boolean isAvailable(){
		return false;
	}


	@Override
	public double findNextNewMoon(final double approximateJdUT){
		throw new UnsupportedOperationException("Ephemeris engine plugin not installed.");
	}

	@Override
	public double calculateApparentMoonLongitude(double jdUT){
		throw new UnsupportedOperationException("Ephemeris engine plugin not installed.");
	}


	@Override
	public double findSolarLongitudeEvent(final double approximateJdUT, final double targetLongitude){
		throw new UnsupportedOperationException("Ephemeris engine plugin not installed.");
	}

	@Override
	public double calculateApparentSunLongitude(final double jdUT){
		throw new UnsupportedOperationException("Ephemeris engine plugin not installed.");
	}


	@Override
	public SunlightPhase calculateHorizonEvent(final CelestialBody body, final double midnightUtJd,
			final double latitude, final double longitude, final ZenithInterface zenith){
		throw new UnsupportedOperationException("Ephemeris engine plugin not installed.");
	}

}
