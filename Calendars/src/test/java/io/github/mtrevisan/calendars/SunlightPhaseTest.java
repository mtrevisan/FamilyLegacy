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
package io.github.mtrevisan.calendars;

import io.github.mtrevisan.calendars.astronomy.SunlightPhase;
import io.github.mtrevisan.calendars.astronomy.Zenith;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;


/**
 * Reference times from the U.S. Naval Observatory ("Sun and Moon Data for One Day"), as in the tests of the Ephemeris
 * module.
 */
class SunlightPhaseTest{

	private static final double TOLERANCE_MINUTES = 1.;

	private static final double ROME_LATITUDE = 41.9;
	private static final double ROME_LONGITUDE = 12.5;
	private static final double TROMSO_LATITUDE = 69.65;
	private static final double TROMSO_LONGITUDE = 18.96;


	@Test
	void romeSummerSolstice(){
		final SunlightPhase phase = CalendarType.getSunlightPhase("gregorian", "21 June 2024", ROME_LATITUDE,
			ROME_LONGITUDE, Zenith.OFFICIAL);

		assertRegularDay(phase, "2024-06-21T03:35Z", "2024-06-21T11:12Z", "2024-06-21T18:49Z");
	}

	@Test
	void romeCivilTwilight(){
		final SunlightPhase phase = CalendarType.getSunlightPhase("gregorian", "21 June 2024", ROME_LATITUDE,
			ROME_LONGITUDE, Zenith.CIVIL);

		assertRegularDay(phase, "2024-06-21T03:01Z", "2024-06-21T11:12Z", "2024-06-21T19:23Z");
	}

	@Test
	void sameDayInAnotherCalendar(){
		// 21 June 2024 (Gregorian) is 8 June 2024 (Julian) and 15 Sivan 5784
		final SunlightPhase gregorian = CalendarType.getSunlightPhase("gregorian", "21 June 2024", ROME_LATITUDE,
			ROME_LONGITUDE, Zenith.OFFICIAL);

		Assertions.assertEquals(gregorian, CalendarType.getSunlightPhase("julian", "8 June 2024", ROME_LATITUDE,
			ROME_LONGITUDE, Zenith.OFFICIAL));
		Assertions.assertEquals(gregorian, CalendarType.getSunlightPhase("hebrew", "15 Sivan 5784", ROME_LATITUDE,
			ROME_LONGITUDE, Zenith.OFFICIAL));
	}

	@Test
	void calendarCodeIsCaseInsensitive(){
		Assertions.assertEquals(
			CalendarType.getSunlightPhase("gregorian", "21 June 2024", ROME_LATITUDE, ROME_LONGITUDE, Zenith.OFFICIAL),
			CalendarType.getSunlightPhase("GREGORIAN", "21 June 2024", ROME_LATITUDE, ROME_LONGITUDE, Zenith.OFFICIAL));
	}

	@Test
	void washingtonWinterSolstice(){
		// West of Greenwich: the local day starts at 05:08 UT
		final SunlightPhase phase = CalendarType.getSunlightPhase("gregorian", "21 December 2024", 38.9, -77.,
			Zenith.OFFICIAL);

		assertRegularDay(phase, "2024-12-21T12:23Z", "2024-12-21T17:06Z", "2024-12-21T21:50Z");
	}

	@Test
	void sydneyEventsBelongToTheLocalDay(){
		// Far East of Greenwich: the sunrise (06:58 AEDT) happens on the previous day in UT
		final SunlightPhase phase = CalendarType.getSunlightPhase("gregorian", "20 March 2024", -33.87, 151.21,
			Zenith.OFFICIAL);

		assertRegularDay(phase, "2024-03-20T06:58+11:00", "2024-03-20T13:03+11:00", "2024-03-20T19:06+11:00");
	}

	@Test
	void tromsoMidnightSun(){
		final SunlightPhase phase = CalendarType.getSunlightPhase("gregorian", "21 June 2024", TROMSO_LATITUDE,
			TROMSO_LONGITUDE, Zenith.OFFICIAL);

		Assertions.assertInstanceOf(SunlightPhase.AlwaysDay.class, phase);
	}

	@Test
	void tromsoPolarNight(){
		final SunlightPhase phase = CalendarType.getSunlightPhase("gregorian", "21 December 2024", TROMSO_LATITUDE,
			TROMSO_LONGITUDE, Zenith.OFFICIAL);

		Assertions.assertInstanceOf(SunlightPhase.AlwaysNight.class, phase);
	}

	@Test
	void tromsoCivilTwilightDuringPolarNight(){
		// The Sun does not rise, but it is less than 6° below the horizon around noon
		final SunlightPhase phase = CalendarType.getSunlightPhase("gregorian", "21 December 2024", TROMSO_LATITUDE,
			TROMSO_LONGITUDE, Zenith.CIVIL);

		final SunlightPhase.RegularDay day = Assertions.assertInstanceOf(SunlightPhase.RegularDay.class, phase);
		assertTime("2024-12-21T08:32Z", day.sunrise());
		assertTime("2024-12-21T12:53Z", day.sunset());
	}

	@Test
	void invalidDate(){
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarType.getSunlightPhase("gregorian", "31 February 2024", ROME_LATITUDE, ROME_LONGITUDE,
				Zenith.OFFICIAL));
	}


	private static void assertRegularDay(final SunlightPhase phase, final String expectedRise,
			final String expectedTransit, final String expectedSet){
		final SunlightPhase.RegularDay day = Assertions.assertInstanceOf(SunlightPhase.RegularDay.class, phase);
		assertTime(expectedRise, day.sunrise());
		assertTime(expectedTransit, day.transit());
		assertTime(expectedSet, day.sunset());
	}

	private static void assertTime(final String expected, final ZonedDateTime actual){
		final OffsetDateTime expectedTime = OffsetDateTime.parse(expected);
		final double differenceMinutes = Duration.between(expectedTime.toInstant(), actual.toInstant()).toMillis() / 60_000.;
		Assertions.assertEquals(0., differenceMinutes, TOLERANCE_MINUTES, () -> "expected " + expected + " but was " + actual);
	}

}
