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

import io.github.mtrevisan.ephemeris.AstronomicalEngineAdapter;
import io.github.mtrevisan.ephemeris.helpers.JulianDate;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.astronomy.SunlightPhase;
import io.github.mtrevisan.calendars.astronomy.Zenith;
import io.github.mtrevisan.calendars.astronomy.ZenithInterface;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;


/**
 * Horizon events compared with the U.S. Naval Observatory, Astronomical Applications Department, "Sun and Moon Data for
 * One Day" (times rounded to the minute; sunrise and sunset with the upper limb on the horizon and standard refraction,
 * civil twilight with the center of the Sun 6° below the horizon).
 *
 * @see <a href="https://aa.usno.navy.mil/data/RS_OneDay">USNO, Sun and Moon Data for One Day</a>
 */
class AstronomicalEngineAdapterTest{

	private static final EphemerisEngine ENGINE = new AstronomicalEngineAdapter();

	/** USNO times are rounded to the minute. */
	private static final double TOLERANCE_MINUTES = 1.;
	/** USNO times are rounded to the minute; the Moon horizon conventions may differ slightly. */
	private static final double MOON_TOLERANCE_MINUTES = 2.;


	@Test
	void moonLongitudeAtNewMoonEqualsSunLongitude(){
		// new moon of 11 January 2024, 11:57 UT
		final double newMoon = ENGINE.findNextNewMoon(2_460_315.5);
		final double moon = ENGINE.calculateApparentMoonLongitude(newMoon);
		final double sun = ENGINE.calculateApparentSunLongitude(newMoon);

		Assertions.assertEquals(0., Math.IEEEremainder(moon - sun, 2. * Math.PI), 1.e-6);
		// first quarter of 18 January 2024, 03:53 UT: the elongation is 90°
		final double firstQuarter = 2_460_327.5 + (3. + 53. / 60.) / 24.;
		final double elongation = Math.toDegrees(ENGINE.calculateApparentMoonLongitude(firstQuarter)
			- ENGINE.calculateApparentSunLongitude(firstQuarter));
		Assertions.assertEquals(90., ((elongation % 360.) + 360.) % 360., 0.1);
	}

	@Test
	void romeSummerSolstice(){
		final SunlightPhase phase = calculate(EphemerisEngine.CelestialBody.SUN, 2024, 6, 21, 0, 41.9, 12.5,
			Zenith.OFFICIAL);

		assertRegularDay(phase, "2024-06-21T03:35Z", "2024-06-21T11:12Z", "2024-06-21T18:49Z", TOLERANCE_MINUTES);
	}

	@Test
	void washingtonWinterSolstice(){
		final SunlightPhase phase = calculate(EphemerisEngine.CelestialBody.SUN, 2024, 12, 21, 0, 38.9, -77.,
			Zenith.OFFICIAL);

		assertRegularDay(phase, "2024-12-21T12:23Z", "2024-12-21T17:06Z", "2024-12-21T21:50Z", TOLERANCE_MINUTES);
	}

	@Test
	void romeEquationOfTimeMaximum(){
		// The equation of time is close to its maximum (+16 min): the transit is far from the mean solar noon
		final SunlightPhase phase = calculate(EphemerisEngine.CelestialBody.SUN, 2024, 11, 3, 0, 41.9, 12.5,
			Zenith.OFFICIAL);

		assertRegularDay(phase, "2024-11-03T05:45Z", "2024-11-03T10:54Z", "2024-11-03T16:01Z", TOLERANCE_MINUTES);
	}

	@Test
	void romeCivilTwilight(){
		final SunlightPhase phase = calculate(EphemerisEngine.CelestialBody.SUN, 2024, 6, 21, 0, 41.9, 12.5,
			Zenith.CIVIL);

		assertRegularDay(phase, "2024-06-21T03:01Z", "2024-06-21T11:12Z", "2024-06-21T19:23Z", TOLERANCE_MINUTES);
	}

	@Test
	void meccaLocalMidnight(){
		// Local civil day in Arabia Standard Time (UTC+3)
		final SunlightPhase phase = calculate(EphemerisEngine.CelestialBody.SUN, 2024, 3, 10, 3, 21.4225, 39.8262,
			Zenith.OFFICIAL);

		assertRegularDay(phase, "2024-03-10T06:34+03:00", "2024-03-10T12:31+03:00", "2024-03-10T18:28+03:00",
			TOLERANCE_MINUTES);
	}

	@Test
	void sydneyLocalMidnight(){
		// Local civil day in Australian Eastern Daylight Time (UTC+11)
		final SunlightPhase phase = calculate(EphemerisEngine.CelestialBody.SUN, 2024, 3, 20, 11, -33.87, 151.21,
			Zenith.OFFICIAL);

		assertRegularDay(phase, "2024-03-20T06:58+11:00", "2024-03-20T13:03+11:00", "2024-03-20T19:06+11:00",
			TOLERANCE_MINUTES);
	}

	@Test
	void meccaMoonsetAfterSunsetOnRamadan1445ConjunctionDay(){
		// New moon on 10 Mar 2024 at 09:00 UT: the Moon set after the Sun in Mecca, so (Umm al-Qura) 1 Ramadan 1445 was
		// 11 Mar 2024
		final SunlightPhase sun = calculate(EphemerisEngine.CelestialBody.SUN, 2024, 3, 10, 0, 21.4225, 39.8262,
			Zenith.OFFICIAL);
		final SunlightPhase moon = calculate(EphemerisEngine.CelestialBody.MOON, 2024, 3, 10, 0, 21.4225, 39.8262,
			Zenith.OFFICIAL);

		assertRegularDay(sun, "2024-03-10T03:34Z", "2024-03-10T09:31Z", "2024-03-10T15:28Z", TOLERANCE_MINUTES);
		final ZonedDateTime moonset = Assertions.assertInstanceOf(SunlightPhase.RegularDay.class, moon).sunset();
		assertTime("2024-03-10T15:41Z", moonset, MOON_TOLERANCE_MINUTES);
		Assertions.assertTrue(moonset.isAfter(Assertions.assertInstanceOf(SunlightPhase.RegularDay.class, sun).sunset()));
	}

	@Test
	void romeMoonset(){
		final SunlightPhase phase = calculate(EphemerisEngine.CelestialBody.MOON, 2024, 6, 21, 0, 41.9, 12.5,
			Zenith.OFFICIAL);

		assertTime("2024-06-21T02:27Z", Assertions.assertInstanceOf(SunlightPhase.RegularDay.class, phase).sunset(),
			MOON_TOLERANCE_MINUTES);
	}

	@Test
	void washingtonMoon(){
		final SunlightPhase phase = calculate(EphemerisEngine.CelestialBody.MOON, 2024, 12, 21, 0, 38.9, -77.,
			Zenith.OFFICIAL);

		assertRegularDay(phase, "2024-12-21T03:26Z", "2024-12-21T10:08Z", "2024-12-21T16:39Z", MOON_TOLERANCE_MINUTES);
	}

	@Test
	void meccaMoonLocalMidnight(){
		// Local civil day in Arabia Standard Time (UTC+3)
		final SunlightPhase phase = calculate(EphemerisEngine.CelestialBody.MOON, 2024, 3, 10, 3, 21.4225, 39.8262,
			Zenith.OFFICIAL);

		assertRegularDay(phase, "2024-03-10T06:36+03:00", "2024-03-10T12:36+03:00", "2024-03-10T18:41+03:00",
			MOON_TOLERANCE_MINUTES);
	}

	@Test
	void tromsoMoonContinuouslyBelowHorizon(){
		// USNO: "Object continuously below the Horizon"
		final SunlightPhase phase = calculate(EphemerisEngine.CelestialBody.MOON, 2024, 6, 21, 0, 69.65, 18.96,
			Zenith.OFFICIAL);

		Assertions.assertInstanceOf(SunlightPhase.AlwaysNight.class, phase);
	}

	@Test
	void tromsoPolarNight(){
		// USNO: "Object continuously below the Horizon"
		final SunlightPhase phase = calculate(EphemerisEngine.CelestialBody.SUN, 2024, 12, 21, 0, 69.65, 18.96,
			Zenith.OFFICIAL);

		Assertions.assertInstanceOf(SunlightPhase.AlwaysNight.class, phase);
	}

	@Test
	void tromsoMidnightSun(){
		// USNO: "Object continuously above the Horizon"
		final SunlightPhase phase = calculate(EphemerisEngine.CelestialBody.SUN, 2024, 6, 21, 0, 69.65, 18.96,
			Zenith.OFFICIAL);

		Assertions.assertInstanceOf(SunlightPhase.AlwaysDay.class, phase);
	}

	@Test
	void tromsoCivilTwilightDuringPolarNight(){
		// The Sun does not rise, but it is less than 6° below the horizon around noon
		final SunlightPhase phase = calculate(EphemerisEngine.CelestialBody.SUN, 2024, 12, 21, 0, 69.65, 18.96,
			Zenith.CIVIL);

		final SunlightPhase.RegularDay day = Assertions.assertInstanceOf(SunlightPhase.RegularDay.class, phase);
		assertTime("2024-12-21T08:32Z", day.sunrise(), TOLERANCE_MINUTES);
		assertTime("2024-12-21T12:53Z", day.sunset(), TOLERANCE_MINUTES);
	}


	/**
	 * @param utcOffsetHours the offset of the local civil day from UT [h]
	 */
	private static SunlightPhase calculate(final EphemerisEngine.CelestialBody body, final int year, final int month,
			final int day, final int utcOffsetHours, final double latitude, final double longitude,
			final ZenithInterface zenith){
		final double localMidnightUtJd = JulianDate.of(year, month, day) - utcOffsetHours / JulianDate.HOURS_PER_DAY;
		return ENGINE.calculateHorizonEvent(body, localMidnightUtJd, latitude, longitude, zenith);
	}

	private static void assertRegularDay(final SunlightPhase phase, final String expectedRise,
			final String expectedTransit, final String expectedSet, final double toleranceMinutes){
		final SunlightPhase.RegularDay day = Assertions.assertInstanceOf(SunlightPhase.RegularDay.class, phase);
		assertTime(expectedRise, day.sunrise(), toleranceMinutes);
		assertTime(expectedTransit, day.transit(), toleranceMinutes);
		assertTime(expectedSet, day.sunset(), toleranceMinutes);
	}

	private static void assertTime(final String expected, final ZonedDateTime actual, final double toleranceMinutes){
		final OffsetDateTime expectedTime = OffsetDateTime.parse(expected);
		final double differenceMinutes = Duration.between(expectedTime.toInstant(), actual.toInstant()).toMillis() / 60_000.;
		Assertions.assertEquals(0., differenceMinutes, toleranceMinutes,
			() -> "expected " + expected + " but was " + actual);
	}

}
