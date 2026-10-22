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

import io.github.mtrevisan.ephemeris.helpers.DeltaT;
import io.github.mtrevisan.ephemeris.helpers.JulianDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Function;


/**
 * Moonset times compared with the U.S. Naval Observatory, Astronomical Applications Department, "Sun and Moon Data for
 * One Day" (times in UT, rounded to the minute; upper limb on the horizon, with refraction and topocentric parallax).
 *
 * @see <a href="https://aa.usno.navy.mil/data/RS_OneDay">USNO, Sun and Moon Data for One Day</a>
 */
class MoonHorizonEventsTest{

	/** UT to Julian centuries of TT, using ΔT. */
	private static final Function<Double, Double> TT_CENTURIES = jdUT -> JulianDate.centuryJ2000Of(
		jdUT + DeltaT.deltaTSecondsFromJd(jdUT) / JulianDate.SECONDS_PER_DAY);

	/** USNO times are rounded to the minute; the Moon horizon conventions may differ slightly. */
	private static final double TOLERANCE_MINUTES = 2.;


	@Test
	void romeMorningMoonset(){
		assertMoonset(2024, 6, 21, 41.9, 12.5, "02:27");
	}

	@Test
	void washingtonAfternoonMoonset(){
		assertMoonset(2024, 12, 21, 38.9, -77., "16:39");
	}

	@Test
	void sydneyMoonset(){
		assertMoonset(2024, 3, 20, -33.87, 151.21, "15:52");
	}

	@Test
	void beijing1900(){
		assertMoonset(1900, 1, 31, 39.9, 116.4, "10:02");
	}

	@Test
	void arcticMoonset(){
		assertMoonset(2024, 12, 21, 69.65, 18.96, "11:10");
	}

	@Test
	void moonContinuouslyBelowHorizon(){
		// USNO: "Object continuously below the Horizon"
		final double moonset = MoonHorizonEvents.calculateMoonset(JulianDate.of(2024, 6, 21), 69.65, 18.96,
			TT_CENTURIES);

		Assertions.assertTrue(Double.isNaN(moonset));
	}


	private static void assertMoonset(final int year, final int month, final int day, final double latitude,
			final double longitude, final String expectedUT){
		final double midnightUtJd = JulianDate.of(year, month, day);

		final double moonset = MoonHorizonEvents.calculateMoonset(midnightUtJd, latitude, longitude, TT_CENTURIES);

		final String[] hm = expectedUT.split(":");
		final double expectedMinutes = Integer.parseInt(hm[0]) * 60. + Integer.parseInt(hm[1]);
		Assertions.assertEquals(expectedMinutes, (moonset - midnightUtJd) * JulianDate.MINUTES_PER_DAY,
			TOLERANCE_MINUTES);
	}

}
