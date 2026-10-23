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
package io.github.mtrevisan.calendars.bahai;

import io.github.mtrevisan.calendars.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngineFactory;
import io.github.mtrevisan.calendars.astronomy.SunlightPhase;
import io.github.mtrevisan.calendars.astronomy.Zenith;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.text.TextNormalizer;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Badíʿ (Bahá'í) calendar: 19 months of 19 days from Naw-Rúz (1 Bahá), with the intercalary days of Ayyám-i-Há before
 * the last month, ʻAláʼ, the month of the fast; the years (BE) are counted from Naw-Rúz 1844 (e.g. "3 Jamál 180 BE").
 * <ul>
 *   <li>Up to 171 BE, Naw-Rúz was fixed on 21 March (the Western practice), and Ayyám-i-Há lasted from 26 February to
 *   1 March.</li>
 *   <li>From 172 BE (2015), Naw-Rúz is the day on which the vernal equinox occurs before sunset in Ṭihrán (Universal
 *   House of Justice, 10 July 2014); for 172-221 BE the dates published by the Bahá'í World Centre are used, afterward
 *   they are computed with the same rule.</li>
 * </ul>
 * A Bahá'í day runs from sunset to sunset; a date is converted into the Gregorian day on which it mostly falls (e.g.
 * Naw-Rúz 172 is 21 March 2015, from sunset of 20 March).
 */
public class BahaiCalendarStrategy implements CalendarConverterPlugin{

	private static final String RESOURCE = "bahai-naw-ruz.csv";

	/** The difference between the Gregorian year of Naw-Rúz and the Bahá'í year. */
	private static final int ERA_OFFSET = 1843;
	/** The first year of the astronomical Naw-Rúz. */
	private static final int FIRST_ASTRONOMICAL_YEAR = 172;
	private static final int DAYS_IN_MONTH = 19;
	private static final int MONTHS_BEFORE_INTERCALARY_DAYS = 18;
	/** The month of the intercalary days (Ayyám-i-Há), between the 18th and the 19th month. */
	public static final int AYYAM_I_HA = 0;

	/**
	 * Ṭihrán (35° 41′ N, 51° 25′ E, the coordinates of the almanacs), the reference for the vernal equinox.
	 * <p>
	 * With the standard sunset, these coordinates reproduce all the 50 years of the table of the Bahá'í World Centre;
	 * the closest case is 183 BE (2026), when the equinox (14:45:53 UTC) followed the sunset by about 3 seconds.
	 */
	private static final double TEHRAN_LATITUDE = 35. + 41. / 60.;
	private static final double TEHRAN_LONGITUDE = 51. + 25. / 60.;
	/** Iran Standard Time, the civil day of Ṭihrán [hours]. */
	private static final double TEHRAN_UTC_OFFSET = 3.5;
	private static final double UNIX_EPOCH_JD = 2_440_587.5;
	private static final double MILLISECONDS_PER_DAY = 86_400_000.;

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		registerMonth(1, "Bahá", "Splendour", "Splendor");
		registerMonth(2, "Jalál", "Glory");
		registerMonth(3, "Jamál", "Beauty");
		registerMonth(4, "ʻAẓamat", "Grandeur");
		registerMonth(5, "Núr", "Light");
		registerMonth(6, "Raḥmat", "Mercy");
		registerMonth(7, "Kalimát", "Words");
		registerMonth(8, "Kamál", "Perfection");
		registerMonth(9, "Asmáʼ", "Names");
		registerMonth(10, "ʻIzzat", "Might");
		registerMonth(11, "Mas͟híyyat", "Mashíyyat", "Mashiyat", "Will");
		registerMonth(12, "ʻIlm", "Knowledge");
		registerMonth(13, "Qudrat", "Power");
		registerMonth(14, "Qawl", "Speech");
		registerMonth(15, "Masáʼil", "Questions");
		registerMonth(16, "S͟haraf", "Sharaf", "Honour", "Honor");
		registerMonth(17, "Sulṭán", "Sovereignty");
		registerMonth(18, "Mulk", "Dominion");
		registerMonth(19, "ʻAláʼ", "Loftiness");
		registerMonth(AYYAM_I_HA, "Ayyám-i-Há", "Ayyamiha", "Intercalary Days");
	}

	/** Official Naw-Rúz (Julian Day Number) of 172-221 BE. */
	private static final Map<Integer, Long> OFFICIAL_NAW_RUZ = loadOfficialNawRuz();
	private static final Map<Integer, Long> COMPUTED_NAW_RUZ = new ConcurrentHashMap<>();


	private static void registerMonth(final int month, final String... names){
		for(final String name : names)
			MONTHS.put(TextNormalizer.normalize(name), month);
	}


	@Override
	public long parseToJdn(final String rawDate){
		final int[] date = parse(rawDate);
		final int year = date[0];
		final int month = date[1];
		final int day = date[2];
		final long nawRuz = nawRuzJdn(year);
		if(month == -1)
			return nawRuz;

		final int intercalaryDays = (int)(nawRuzJdn(year + 1) - nawRuz) - (MONTHS_BEFORE_INTERCALARY_DAYS + 1) * DAYS_IN_MONTH;
		final int dayOfMonth = (day != 0? day: 1);
		final int monthLength = (month == AYYAM_I_HA? intercalaryDays: DAYS_IN_MONTH);
		if(dayOfMonth < 1 || dayOfMonth > monthLength)
			throw new IllegalArgumentException("The day " + dayOfMonth + " does not exist in "
				+ (month == AYYAM_I_HA? "Ayyám-i-Há of " + year + " (" + intercalaryDays + " days)": "a Bahá'í month") + ": "
				+ rawDate);

		final long monthStart;
		if(month == AYYAM_I_HA)
			monthStart = nawRuz + MONTHS_BEFORE_INTERCALARY_DAYS * DAYS_IN_MONTH;
		else if(month <= MONTHS_BEFORE_INTERCALARY_DAYS)
			monthStart = nawRuz + (long)(month - 1) * DAYS_IN_MONTH;
		else
			monthStart = nawRuz + MONTHS_BEFORE_INTERCALARY_DAYS * DAYS_IN_MONTH + intercalaryDays;
		return monthStart + dayOfMonth - 1;
	}

	/**
	 * Computes the day of Naw-Rúz (1 Bahá) of a year.
	 *
	 * @param year the Bahá'í year (BE)
	 * @return the Julian Day Number of the Gregorian day of Naw-Rúz
	 */
	public static long nawRuzJdn(final int year){
		if(year < FIRST_ASTRONOMICAL_YEAR)
			return GregorianCalendarStrategy.toJdn(year + ERA_OFFSET, 3, 21);

		final Long official = OFFICIAL_NAW_RUZ.get(year);
		return (official != null? official: computeNawRuzJdn(year));
	}

	/**
	 * Computes the day of Naw-Rúz with the rule of 172 BE: the day on which the vernal equinox occurs before sunset in
	 * Ṭihrán.
	 *
	 * @param year the Bahá'í year (BE)
	 * @return the Julian Day Number of the Gregorian day of Naw-Rúz
	 */
	public static long computeNawRuzJdn(final int year){
		return COMPUTED_NAW_RUZ.computeIfAbsent(year, y -> {
			final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
			if(!engine.isAvailable())
				throw new UnsupportedOperationException("The Bahá'í calendar after 221 BE requires the Ephemeris plugin.");

			final double approximateEquinox = GregorianCalendarStrategy.toJdn(y + ERA_OFFSET, 3, 20) - 0.5;
			final double equinox = engine.findSolarLongitudeEvent(approximateEquinox, 0.);
			// the civil day of Ṭihrán on which the equinox occurs, and its sunset
			final long equinoxDay = (long)Math.floor(equinox + TEHRAN_UTC_OFFSET / 24. + 0.5);
			final double localMidnightUT = equinoxDay - 0.5 - TEHRAN_UTC_OFFSET / 24.;
			final SunlightPhase phase = engine.calculateHorizonEvent(EphemerisEngine.CelestialBody.SUN, localMidnightUT,
				TEHRAN_LATITUDE, TEHRAN_LONGITUDE, Zenith.OFFICIAL);
			if(!(phase instanceof SunlightPhase.RegularDay(var sunrise, var transit, var sunset)) || sunset == null)
				throw new IllegalStateException("No sunset in Ṭihrán on the day of the vernal equinox of " + y + " BE");

			final double sunsetJd = sunset.toInstant().toEpochMilli() / MILLISECONDS_PER_DAY + UNIX_EPOCH_JD;
			return (equinox < sunsetJd? equinoxDay: equinoxDay + 1);
		});
	}

	/**
	 * @return the year, the month (1 to 19, {@link #AYYAM_I_HA}, or -1 if missing), and the day (0 if missing)
	 */
	private static int[] parse(final String rawDate){
		final List<String> words = new ArrayList<>(List.of(TextNormalizer.tokenize(rawDate)));
		// the era marker ("BE", "B.E.", "Badí'")
		words.removeIf(word -> word.equals("be") || word.equals("badi") || word.equals("b") || word.equals("e"));
		if(words.isEmpty() || !StringUtils.isNumeric(words.getLast()))
			throw new IllegalArgumentException("Missing year in the Bahá'í date: " + rawDate);

		final int year = Integer.parseInt(words.removeLast());
		if(words.isEmpty())
			return new int[]{year, -1, 0};

		int day = 0;
		if(StringUtils.isNumeric(words.getFirst()))
			day = Integer.parseInt(words.removeFirst());
		final String monthName = String.join(" ", words);
		Integer month = MONTHS.get(monthName);
		if(month == null)
			month = MONTHS.get(monthName.replace(" ", ""));
		if(month == null && StringUtils.isNumeric(monthName)){
			final int number = Integer.parseInt(monthName);
			month = (number >= 1 && number <= DAYS_IN_MONTH? number: null);
		}
		if(month == null)
			throw new IllegalArgumentException("Unknown Bahá'í month '" + monthName + "': " + rawDate);

		return new int[]{year, month, day};
	}

	private static Map<Integer, Long> loadOfficialNawRuz(){
		final Map<Integer, Long> nawRuz = new HashMap<>();
		try(final InputStream is = BahaiCalendarStrategy.class.getResourceAsStream(RESOURCE)){
			if(is == null)
				throw new IllegalStateException("Missing resource " + RESOURCE);

			final BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			String line;
			while((line = reader.readLine()) != null){
				if(line.isBlank() || line.startsWith("#"))
					continue;

				final String[] fields = line.split(";");
				final LocalDate date = LocalDate.parse(fields[1]);
				nawRuz.put(Integer.parseInt(fields[0]),
					GregorianCalendarStrategy.toJdn(date.getYear(), date.getMonthValue(), date.getDayOfMonth()));
			}
		}
		catch(final IOException ioe){
			throw new UncheckedIOException(ioe);
		}
		return nawRuz;
	}

	@Override
	public CalendarType.CalendarMetadata getMetadata(final String rawDate){
		final int[] date = parse(rawDate);
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put("year", true);
		components.put("month", (date[1] != -1));
		components.put("day", (date[2] != 0));
		return new CalendarType.CalendarMetadata(components, (date[1] != -1 && date[2] != 0));
	}

}
