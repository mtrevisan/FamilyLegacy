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
package io.github.mtrevisan.calendars.assistant;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngineFactory;
import io.github.mtrevisan.calendars.astronomy.SunlightPhase;
import io.github.mtrevisan.calendars.astronomy.Zenith;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;


/** Sunrise and sunset of a day at a place, and what they mean for the date of an event expressed in hours from sunset. */
public final class SunTool{

	/** How the hours of a medieval source are counted. */
	public enum HourSystem{
		/** Hours of 60 minutes, as in a modern clock. */
		EQUAL("equal hours of 60 minutes"),
		/**
		 * Temporal hours: the time from sunrise to sunset (by day), or from sunset to the next sunrise (by night), is divided
		 * in 12 hours, whose length changes with the season and the latitude.
		 */
		UNEQUAL("unequal (temporal) hours: a twelfth of the day, or of the night");

		private final String description;

		HourSystem(final String description){
			this.description = description;
		}
	}


	private static final double SECONDS_PER_HOUR = 3600.;
	private static final double DEGREES_PER_HOUR = 15.;
	private static final int TEMPORAL_HOURS = 12;


	private SunTool(){}


	/**
	 * Computes the sun events of a day and the readings of the date of an event expressed in hours from sunset.
	 *
	 * @param calendarCode    the code of the calendar of the date
	 * @param rawDate         the date, in the notation of the calendar
	 * @param latitude        the latitude of the place, in degrees (North positive)
	 * @param longitude       the longitude of the place, in degrees (East positive)
	 * @param hourSystem      how the hours are counted (ignored if {@code hoursFromSunset} is {@code null})
	 * @param hoursFromSunset the time of the event with respect to the sunset (negative for before), or {@code null} if the
	 *                        event is not expressed from sunset
	 * @return the first suggestion is informative (sunrise and sunset), the following ones are the readings of the date
	 * @throws UnsupportedOperationException if the Ephemeris plugin is not installed
	 * @throws IllegalArgumentException      if the date cannot be parsed
	 * @throws IllegalStateException         if the Sun does not rise or set on the day (polar day or night) and the event
	 *                                       requires them
	 */
	public static List<DateSuggestion> analyze(final String calendarCode, final String rawDate, final double latitude,
			final double longitude, final HourSystem hourSystem, final Double hoursFromSunset){
		final CalendarType type = CalendarType.fromCode(calendarCode);
		final long jdn = type.parseToJdn(rawDate);

		final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
		if(!engine.isAvailable())
			throw new UnsupportedOperationException("Astronomical computation framework requires the Ephemeris JAR.");

		final SunlightPhase today = horizon(engine, jdn, latitude, longitude);

		final String place = "latitude " + latitude + "°, longitude " + longitude + "°";
		final List<String> assumptions = new ArrayList<>();
		assumptions.add("Local mean time of the place (historical dates predate the time zones): UTC + longitude / 15 hours");
		assumptions.add("Official sunrise and sunset (the upper limb of the Sun at the horizon), without the local relief");

		final List<String> steps = new ArrayList<>();
		steps.add("Read \"" + rawDate + "\" as a " + type.getCode() + " date: Julian Day Number " + jdn);
		final String meaning;
		ZonedDateTime sunrise = null;
		ZonedDateTime sunset = null;
		switch(today){
			case SunlightPhase.RegularDay day -> {
				sunrise = day.sunrise();
				sunset = day.sunset();
				final String times = "the Sun rises at " + localTime(sunrise, longitude) + " and sets at "
					+ localTime(sunset, longitude) + " (local mean time)";
				steps.add("Sunrise and sunset: " + times);
				meaning = "On " + rawDate + " at " + place + " " + times;
			}
			case SunlightPhase.AlwaysDay ignored -> meaning = "At " + place + " the Sun does not set on " + rawDate + " (polar day)";
			case SunlightPhase.AlwaysNight ignored -> meaning = "At " + place + " the Sun does not rise on " + rawDate + " (polar night)";
		}

		final List<DateSuggestion> result = new ArrayList<>();
		result.add(new DateSuggestion("Sunrise and sunset", meaning, assumptions, null, steps, null));
		if(hoursFromSunset == null)
			return result;

		if(sunrise == null || sunset == null)
			throw new IllegalStateException("The Sun does not both rise and set on " + rawDate + " at " + place);

		final ZonedDateTime event = eventInstant(engine, jdn, latitude, longitude, hourSystem, hoursFromSunset, sunrise, sunset,
			steps, assumptions);
		final String hours = formatHours(Math.abs(hoursFromSunset)) + (hoursFromSunset >= 0? " after": " before") + " sunset";
		final String when = hours + " (" + hourSystem.description + "), at " + localTime(event, longitude) + " local mean time";
		steps.add("Instant of the event: " + localTime(event, longitude) + " local mean time");

		result.addAll(dateReadings(calendarCode, rawDate, jdn, hoursFromSunset, event, sunset, longitude, when, steps, assumptions));
		return result;
	}


	private static SunlightPhase horizon(final EphemerisEngine engine, final long jdn, final double latitude,
			final double longitude){
		//the Julian Day Number starts at Greenwich noon: the local mean midnight is half a day before it, shifted by the longitude
		final double midnightUtJd = jdn - 0.5 - longitude / 360.;
		return engine.calculateHorizonEvent(EphemerisEngine.CelestialBody.SUN, midnightUtJd, latitude, longitude, Zenith.OFFICIAL);
	}

	/** The instant of an event at a number of hours from the sunset. */
	private static ZonedDateTime eventInstant(final EphemerisEngine engine, final long jdn, final double latitude,
			final double longitude, final HourSystem hourSystem, final double hoursFromSunset, final ZonedDateTime sunrise,
			final ZonedDateTime sunset, final List<String> steps, final List<String> assumptions){
		if(hourSystem == HourSystem.EQUAL){
			assumptions.add("Hours of 60 minutes, counted from the sunset");
			return sunset.plus(Math.round(hoursFromSunset * SECONDS_PER_HOUR), ChronoUnit.SECONDS);
		}

		//a temporal hour is a twelfth of the night (after the sunset) or of the day (before it)
		final Duration span;
		if(hoursFromSunset >= 0){
			if(!(horizon(engine, jdn + 1, latitude, longitude) instanceof SunlightPhase.RegularDay next))
				throw new IllegalStateException("The Sun does not rise and set on the day after: the night has no temporal hours");

			span = Duration.between(sunset, next.sunrise());
			steps.add("Night: from sunset (" + localTime(sunset, longitude) + ") to the next sunrise ("
				+ localTime(next.sunrise(), longitude) + "), " + formatDuration(span));
			assumptions.add("The temporal hours of the night are a twelfth of the time from sunset to the next sunrise");
		}
		else{
			span = Duration.between(sunrise, sunset);
			steps.add("Day: from sunrise (" + localTime(sunrise, longitude) + ") to sunset (" + localTime(sunset, longitude) + "), "
				+ formatDuration(span));
			assumptions.add("The temporal hours of the day are a twelfth of the time from sunrise to sunset");
		}
		final long hourMillis = span.toMillis() / TEMPORAL_HOURS;
		steps.add("Temporal hour: " + formatDuration(Duration.ofMillis(hourMillis)));
		return sunset.plus(Math.round(hoursFromSunset * hourMillis), ChronoUnit.MILLIS);
	}

	private static List<DateSuggestion> dateReadings(final String calendarCode, final String rawDate, final long jdn,
			final double hoursFromSunset, final ZonedDateTime event, final ZonedDateTime sunset, final double longitude,
			final String when, final List<String> steps, final List<String> assumptions){
		//the civil day changes at the local midnight, that may fall between the sunset and the event
		final long shift = ChronoUnit.DAYS.between(localDate(sunset, longitude), localDate(event, longitude));

		final List<DateSuggestion> readings = new ArrayList<>();
		readings.add(reading(shift == 0? "Day starts at midnight": "Day starts at midnight (after midnight)",
			rawDate + ", " + when, calendarCode, rawDate, jdn, shift, steps, assumptions,
			shift == 0
				? "The civil day starts at midnight (modern convention): the event is still on " + rawDate
				: "The civil day starts at midnight (modern convention): the event falls after the local midnight, in the following day"));
		if(hoursFromSunset > 0)
			readings.add(reading("Day starts at sunset", "The day after " + rawDate + ", " + when, calendarCode, rawDate, jdn, 1,
				steps, assumptions, "The day starts at sunset (Hebrew and Islamic reckoning, Italian hours, and many liturgical "
					+ "sources): the evening and the night belong to the following day"));
		return readings;
	}

	private static DateSuggestion reading(final String title, final String meaning, final String calendarCode,
			final String rawDate, final long jdn, final long shift, final List<String> steps, final List<String> assumptions,
			final String assumption){
		final List<String> allAssumptions = new ArrayList<>(assumptions);
		allAssumptions.add(assumption);
		final List<String> allSteps = new ArrayList<>(steps);
		if(shift == 0)
			return new DateSuggestion(title, meaning, allAssumptions, calendarCode, allSteps, rawDate);

		final String code = (CivilDates.GREGORIAN.equals(calendarCode)? CivilDates.GREGORIAN: CivilDates.JULIAN);
		final String date = CivilDates.format(jdn + shift, code);
		allSteps.add("Julian Day Number " + jdn + " + " + shift + " = " + (jdn + shift) + ", written in the " + code
			+ " calendar: " + date);
		if(!code.equals(calendarCode))
			allAssumptions.add("The date is written in the " + code + " calendar: change it if the source follows another one");
		return new DateSuggestion(title, meaning, allAssumptions, code, allSteps, date);
	}


	private static java.time.LocalDate localDate(final ZonedDateTime utc, final double longitude){
		return shiftToLocal(utc, longitude).toLocalDate();
	}

	private static java.time.LocalDateTime shiftToLocal(final ZonedDateTime utc, final double longitude){
		return utc.withZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime().plusSeconds(Math.round(longitude / DEGREES_PER_HOUR * SECONDS_PER_HOUR));
	}

	private static String localTime(final ZonedDateTime utc, final double longitude){
		if(utc == null)
			return "none";

		final java.time.LocalDateTime local = shiftToLocal(utc, longitude);
		return String.format("%02d:%02d", local.getHour(), local.getMinute());
	}

	private static String formatDuration(final Duration duration){
		return duration.toHours() + " h " + duration.toMinutesPart() + " min";
	}

	private static String formatHours(final double hours){
		return (hours == Math.rint(hours)? String.valueOf((long)hours): String.valueOf(hours)) + (hours == 1.? " hour": " hours");
	}

}
