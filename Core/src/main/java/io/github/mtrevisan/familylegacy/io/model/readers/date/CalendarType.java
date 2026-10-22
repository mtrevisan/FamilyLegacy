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
package io.github.mtrevisan.familylegacy.io.model.readers.date;

import io.github.mtrevisan.familylegacy.io.model.readers.date.armenian.ArmenianCalendarStrategy;
import io.github.mtrevisan.familylegacy.io.model.readers.date.buddhist.BuddhistCalendarStrategy;
import io.github.mtrevisan.familylegacy.io.model.readers.date.byzantine.ByzantineCalendarStrategy;
import io.github.mtrevisan.familylegacy.io.model.readers.date.chinese.ChineseCalendarStrategy;
import io.github.mtrevisan.familylegacy.io.model.readers.date.coptic.CopticCalendarStrategy;
import io.github.mtrevisan.familylegacy.io.model.readers.date.egyptian.EgyptianCalendarStrategy;
import io.github.mtrevisan.familylegacy.io.model.readers.date.ethiopian.EthiopianCalendarStrategy;
import io.github.mtrevisan.familylegacy.io.model.readers.date.frenchrepublican.FrenchRepublicanCalendarStrategy;
import io.github.mtrevisan.familylegacy.io.model.readers.date.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.familylegacy.io.model.readers.date.gregorian.JulianCalendarStrategy;
import io.github.mtrevisan.familylegacy.io.model.readers.date.gregorian.ReformedJulianCalendarStrategy;
import io.github.mtrevisan.familylegacy.io.model.readers.date.hebrew.HebrewCalendarStrategy;
import io.github.mtrevisan.familylegacy.io.model.readers.date.indian.IndianCalendarStrategy;
import io.github.mtrevisan.familylegacy.io.model.readers.date.islamic.IslamicCalendarStrategy;
import io.github.mtrevisan.familylegacy.io.model.readers.date.mayan.MayanCalendarStrategy;
import io.github.mtrevisan.familylegacy.io.model.readers.date.parsi.ParsiCalendarStrategy;
import io.github.mtrevisan.familylegacy.io.model.readers.date.persian.PersianCalendarStrategy;
import io.github.mtrevisan.familylegacy.io.model.readers.date.rumi.RumiCalendarStrategy;
import io.github.mtrevisan.familylegacy.io.model.readers.date.seleucid.SeleucidCalendarStrategy;
import io.github.mtrevisan.familylegacy.io.model.readers.date.sovieteternal.SovietEternalCalendarStrategy;
import io.github.mtrevisan.familylegacy.io.model.readers.date.venetan.VenetanCalendarStrategy;
import io.github.mtrevisan.familylegacy.services.EphemerisEngine;
import io.github.mtrevisan.familylegacy.services.EphemerisEngineFactory;
import io.github.mtrevisan.familylegacy.services.SunlightPhase;
import io.github.mtrevisan.familylegacy.services.ZenithInterface;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;


/**
 * Supported calendar systems and their conversion algorithms to Julian Day Number (JDN).
 */
public enum CalendarType implements CalendarConverterPlugin{

	GREGORIAN("gregorian", new GregorianCalendarStrategy()),
	VENETAN("venetan", new VenetanCalendarStrategy()),
	JULIAN("julian", new JulianCalendarStrategy()),
	REFORMED_JULIAN("reformed_julian", new ReformedJulianCalendarStrategy()),
	ISLAMIC("islamic", new IslamicCalendarStrategy()),
	BUDDHIST("buddhist", new BuddhistCalendarStrategy()),
	COPTIC("coptic", new CopticCalendarStrategy()),
	ETHIOPIAN("ethiopian", new EthiopianCalendarStrategy()),

	HEBREW("hebrew", new HebrewCalendarStrategy()),

	CHINESE("chinese", new ChineseCalendarStrategy()),
	INDIAN("indian", new IndianCalendarStrategy()),

	FRENCH_REPUBLICAN("french_republican", new FrenchRepublicanCalendarStrategy()),
	SOVIET_ETERNAL("soviet_eternal", new SovietEternalCalendarStrategy()),
	MAYAN("mayan", new MayanCalendarStrategy()),

	PERSIAN("persian", new PersianCalendarStrategy()),
	PARSI("parsi", new ParsiCalendarStrategy()),
	BYZANTINE("byzantine", new ByzantineCalendarStrategy()),
	EGYPTIAN("egyptian", new EgyptianCalendarStrategy()),
	SELEUCID("seleucid", new SeleucidCalendarStrategy()),
	ARMENIAN("armenian", new ArmenianCalendarStrategy()),
	RUMI("rumi", new RumiCalendarStrategy());


	private final String code;
	private final CalendarConverterPlugin strategy;


	CalendarType(final String code, final CalendarConverterPlugin strategy){
		this.code = code;
		this.strategy = strategy;
	}


	public String getCode(){
		return code;
	}

	public static CalendarType fromCode(final String code){
		for(final CalendarType type : values())
			if(type.code.equalsIgnoreCase(code))
				return type;

//		throw new IllegalArgumentException("Unsupported calendar: " + code);
//
//		LOGGER.warn("Unsupported calendar: {}, default to 'gregorian'", code);

		return GREGORIAN;
	}

	@Override
	public long parseToJdn(final String rawDate){
		return strategy.parseToJdn(rawDate);
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		return strategy.getMetadata(rawDate);
	}

	public record CalendarMetadata(Map<String, Boolean> nativeComponents, boolean isComplete){
		public CalendarMetadata{
			nativeComponents = Collections.unmodifiableMap(nativeComponents);
		}
	}

	/**
	 * Julian Day Number of 1970-01-01 in the proleptic Gregorian calendar,
	 * i.e. the JDN that corresponds to {@link LocalDate#ofEpochDay(long)}
	 * day {@code 0}. Used to convert a JDN into a {@link LocalDate}.
	 */
	private static final long UNIX_EPOCH_JDN = 2440588L;

	/**
	 * Converts an absolute Julian Day Number (JDN) into a local proleptic Gregorian LocalDate.
	 * This serves as the standardized internal translation layer for the genealogy framework.
	 *
	 * @param jdn the absolute Julian Day Number to convert
	 * @return the corresponding proleptic Gregorian {@link LocalDate}
	 */
	public static LocalDate jdnToProlepticGregorianLocalDate(final long jdn){
		return LocalDate.ofEpochDay(jdn - UNIX_EPOCH_JDN);
	}


	public static final int SEASON_AUTUMN = -1;
	public static final int SEASON_WINTER = -2;
	public static final int SEASON_SPRING = -3;
	public static final int SEASON_SUMMER = -4;

	public static double getGeneralTargetLongitude(final int month){
		return switch(month){
			case SEASON_AUTUMN -> Math.PI;
			case SEASON_WINTER -> 1.5 * Math.PI;
			case SEASON_SPRING -> 0.;
			case SEASON_SUMMER -> 0.5 * Math.PI;
			default -> 0.;
		};
	}


	/**
	 * Computes the precise localized sunlight phase tracking boundaries for any historical date.
	 *
	 * @param calendarCode the native calendar tracking system code
	 * @param rawDate      the uncleaned date expression text string
	 * @param latitude     observer geodetic latitude in degrees
	 * @param longitude    observer geodetic longitude in degrees
	 * @param zenith       the targeted zenith boundary (e.g. OFFICIAL, CIVIL twilight)
	 * @return a localized polymorphic {@link SunlightPhase} container instance
	 */
	public static SunlightPhase getSunlightPhase(final String calendarCode, final String rawDate,
			final double latitude, final double longitude, final ZenithInterface zenith){
		final CalendarType type = CalendarType.fromCode(calendarCode);
		final long jdn = type.parseToJdn(rawDate);

		// Align absolute Julian Day Number to Greenwich Midnight UT
		final double midnightUtJd = jdn - 0.5;

		final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
		if(!engine.isAvailable())
			throw new UnsupportedOperationException("Astronomical computation framework requires the Ephemeris JAR.");

		// Execute the proxy calculation call out to the plugin JAR
		return engine.calculateHorizonEvent(EphemerisEngine.CelestialBody.SUN, midnightUtJd, latitude, longitude, zenith);
	}

}
