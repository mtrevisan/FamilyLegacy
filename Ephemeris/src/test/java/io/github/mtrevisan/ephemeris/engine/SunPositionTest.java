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
package io.github.mtrevisan.ephemeris.engine;

import io.github.mtrevisan.ephemeris.engine.coordinates.EclipticCoordinates;
import io.github.mtrevisan.ephemeris.helpers.JulianDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class SunPositionTest{

	@Test
	void test(){
		final double t = JulianDate.millenniumJ2000Of(2444269.5);
		final EclipticCoordinates coord = SunPosition.embTrueHeliocentricPosition(t);

		Assertions.assertEquals(3.182657571886453E-5, coord.getLatitude(), 0.000_000_1);
		Assertions.assertEquals(2.278426072988786, coord.getLongitude(), 0.000_000_1);
		Assertions.assertEquals(0.9851322735478121, coord.getDistance(), 0.000_000_1);
	}

}