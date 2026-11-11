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
import io.github.mtrevisan.calendars.gregorian.CalendarReform;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;


/** Sunrise and sunset of a day at a place, and what they mean for the date of an event expressed in hours from sunset. */
public final class SunTool{

	/** How the hours of a medieval source are counted. */
	public enum HourSystem{
		/** Hours of 60 minutes, as in a modern clock. */
		EQUAL("date_assistant.hour_system.equal"),
		/**
		 * Temporal hours: the time from sunrise to sunset (by day), or from sunset to the next sunrise (by night), is divided
		 * in 12 hours, whose length changes with the season and the latitude.
		 */
		UNEQUAL("date_assistant.hour_system.unequal");

		private final String descriptionKey;

		HourSystem(final String descriptionKey){
			this.descriptionKey = descriptionKey;
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

		final Message place = Message.of("date_assistant.place", String.valueOf(latitude), String.valueOf(longitude));
		final List<Message> assumptions = new ArrayList<>();
		assumptions.add(Message.of("date_assistant.assumption.local_mean_time"));
		assumptions.add(Message.of("date_assistant.assumption.official_sun"));

		final List<Message> steps = new ArrayList<>();
		steps.add(Message.of("date_assistant.step.read", rawDate, Message.calendar(type.getCode()), jdn));
		final Message meaning;
		ZonedDateTime sunrise = null;
		ZonedDateTime sunset = null;
		switch(today){
			case SunlightPhase.RegularDay day -> {
				sunrise = day.sunrise();
				sunset = day.sunset();
				final Message times = Message.of("date_assistant.sun.times", localTime(sunrise, longitude), localTime(sunset, longitude));
				steps.add(Message.of("date_assistant.step.sun_times", times));
				meaning = Message.of("date_assistant.meaning.sun_regular", rawDate, place, times);
			}
			case SunlightPhase.AlwaysDay ignored -> meaning = Message.of("date_assistant.meaning.polar_day", place, rawDate);
			case SunlightPhase.AlwaysNight ignored -> meaning = Message.of("date_assistant.meaning.polar_night", place, rawDate);
		}

		final List<DateSuggestion> result = new ArrayList<>();
		result.add(new DateSuggestion(Message.of("date_assistant.title.sun"), meaning, assumptions, null, steps, null));
		if(hoursFromSunset == null)
			return result;

		if(sunrise == null || sunset == null)
			throw new AssistantStateException("date_assistant.error.sun_no_events", rawDate, place);

		final ZonedDateTime event = eventInstant(engine, jdn, latitude, longitude, hourSystem, hoursFromSunset, sunrise, sunset,
			steps, assumptions);
		final Message hours = Message.of("date_assistant.hours", Math.abs(hoursFromSunset));
		final Message when = Message.of(hoursFromSunset >= 0? "date_assistant.when.after": "date_assistant.when.before", hours,
			Message.of(hourSystem.descriptionKey), localTime(event, longitude));
		steps.add(Message.of("date_assistant.step.event_instant", localTime(event, longitude)));

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
			final ZonedDateTime sunset, final List<Message> steps, final List<Message> assumptions){
		if(hourSystem == HourSystem.EQUAL){
			assumptions.add(Message.of("date_assistant.assumption.equal_hours"));
			return sunset.plus(Math.round(hoursFromSunset * SECONDS_PER_HOUR), ChronoUnit.SECONDS);
		}

		//a temporal hour is a twelfth of the night (after the sunset) or of the day (before it)
		final Duration span;
		if(hoursFromSunset >= 0){
			if(!(horizon(engine, jdn + 1, latitude, longitude) instanceof SunlightPhase.RegularDay next))
				throw new AssistantStateException("date_assistant.error.sun_no_next_day");

			span = Duration.between(sunset, next.sunrise());
			steps.add(Message.of("date_assistant.step.night", localTime(sunset, longitude), localTime(next.sunrise(), longitude),
				duration(span)));
			assumptions.add(Message.of("date_assistant.assumption.temporal_night"));
		}
		else{
			span = Duration.between(sunrise, sunset);
			steps.add(Message.of("date_assistant.step.day", localTime(sunrise, longitude), localTime(sunset, longitude), duration(span)));
			assumptions.add(Message.of("date_assistant.assumption.temporal_day"));
		}
		final long hourMillis = span.toMillis() / TEMPORAL_HOURS;
		steps.add(Message.of("date_assistant.step.temporal_hour", duration(Duration.ofMillis(hourMillis))));
		return sunset.plus(Math.round(hoursFromSunset * hourMillis), ChronoUnit.MILLIS);
	}

	private static List<DateSuggestion> dateReadings(final String calendarCode, final String rawDate, final long jdn,
			final double hoursFromSunset, final ZonedDateTime event, final ZonedDateTime sunset, final double longitude,
			final Message when, final List<Message> steps, final List<Message> assumptions){
		//the civil day changes at the local midnight, that may fall between the sunset and the event
		final long shift = ChronoUnit.DAYS.between(localDate(sunset, longitude), localDate(event, longitude));

		final List<DateSuggestion> readings = new ArrayList<>();
		readings.add(reading(Message.of(shift == 0? "date_assistant.title.midnight": "date_assistant.title.midnight_after"),
			Message.of("date_assistant.meaning.reading", rawDate, when), calendarCode, rawDate, jdn, shift, steps, assumptions,
			Message.of(shift == 0? "date_assistant.assumption.midnight_same_day": "date_assistant.assumption.midnight_next_day", rawDate)));
		if(hoursFromSunset > 0)
			readings.add(reading(Message.of("date_assistant.title.sunset"), Message.of("date_assistant.meaning.reading_next", rawDate, when),
				calendarCode, rawDate, jdn, 1, steps, assumptions, Message.of("date_assistant.assumption.sunset_day")));
		return readings;
	}

	private static DateSuggestion reading(final Message title, final Message meaning, final String calendarCode,
			final String rawDate, final long jdn, final long shift, final List<Message> steps, final List<Message> assumptions,
			final Message assumption){
		final List<Message> allAssumptions = new ArrayList<>(assumptions);
		allAssumptions.add(assumption);
		final List<Message> allSteps = new ArrayList<>(steps);
		if(shift == 0)
			return new DateSuggestion(title, meaning, allAssumptions, calendarCode, allSteps, rawDate);

		//a civil source keeps its calendar, any other one is written in the calendar in force at the date (as in the conversion)
		final boolean civil = (CivilDates.GREGORIAN.equals(calendarCode) || CivilDates.JULIAN.equals(calendarCode));
		final String code = (civil? calendarCode: CivilDates.calendarInForce(jdn + shift, CalendarReform.ROME));
		final String date = CivilDates.format(jdn + shift, code);
		allSteps.add(Message.of("date_assistant.step.shift", jdn, shift, jdn + shift, Message.calendar(code), date));
		if(!code.equals(calendarCode))
			allAssumptions.add(Message.of("date_assistant.assumption.written_in_force", Message.calendar(code)));
		return new DateSuggestion(title, meaning, allAssumptions, code, allSteps, date);
	}


	private static LocalDate localDate(final ZonedDateTime utc, final double longitude){
		return shiftToLocal(utc, longitude).toLocalDate();
	}

	private static LocalDateTime shiftToLocal(final ZonedDateTime utc, final double longitude){
		return utc.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime().plusSeconds(Math.round(longitude / DEGREES_PER_HOUR * SECONDS_PER_HOUR));
	}

	private static String localTime(final ZonedDateTime utc, final double longitude){
		if(utc == null)
			return "-";

		final LocalDateTime local = shiftToLocal(utc, longitude);
		return String.format("%02d:%02d", local.getHour(), local.getMinute());
	}

	private static Message duration(final Duration duration){
		return Message.of("date_assistant.duration", duration.toHours(), duration.toMinutesPart());
	}

}
