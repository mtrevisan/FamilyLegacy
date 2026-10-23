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

import io.github.mtrevisan.calendars.armenian.ArmenianCalendarStrategy;
import io.github.mtrevisan.calendars.bahai.BahaiCalendarStrategy;
import io.github.mtrevisan.calendars.berber.BerberCalendarStrategy;
import io.github.mtrevisan.calendars.buddhist.BuddhistCalendarStrategy;
import io.github.mtrevisan.calendars.byzantine.ByzantineCalendarStrategy;
import io.github.mtrevisan.calendars.chinese.ChineseCalendarStrategy;
import io.github.mtrevisan.calendars.coptic.CopticCalendarStrategy;
import io.github.mtrevisan.calendars.eastasian.EraOffsetCalendarStrategy;
import io.github.mtrevisan.calendars.eastasian.JapaneseCalendarStrategy;
import io.github.mtrevisan.calendars.eastasian.KoreanCalendarStrategy;
import io.github.mtrevisan.calendars.eastasian.VietnameseCalendarStrategy;
import io.github.mtrevisan.calendars.egyptian.EgyptianCalendarStrategy;
import io.github.mtrevisan.calendars.ethiopian.EthiopianCalendarStrategy;
import io.github.mtrevisan.calendars.frenchrepublican.FrenchRepublicanCalendarStrategy;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.gregorian.JulianCalendarStrategy;
import io.github.mtrevisan.calendars.gregorian.ReformedJulianCalendarStrategy;
import io.github.mtrevisan.calendars.hebrew.HebrewCalendarStrategy;
import io.github.mtrevisan.calendars.indian.BangladeshCalendarStrategy;
import io.github.mtrevisan.calendars.indian.HinduSolarCalendarStrategy;
import io.github.mtrevisan.calendars.indian.IndianCalendarStrategy;
import io.github.mtrevisan.calendars.indian.VikramSamvatCalendarStrategy;
import io.github.mtrevisan.calendars.nepali.NepaliCalendarStrategy;
import io.github.mtrevisan.calendars.islamic.IslamicCalendarStrategy;
import io.github.mtrevisan.calendars.liturgical.LiturgicalCalendarStrategy;
import io.github.mtrevisan.calendars.mayan.MayanCalendarStrategy;
import io.github.mtrevisan.calendars.parsi.ParsiCalendarStrategy;
import io.github.mtrevisan.calendars.persian.PersianCalendarStrategy;
import io.github.mtrevisan.calendars.quaker.QuakerCalendarStrategy;
import io.github.mtrevisan.calendars.regnal.RegnalCalendarStrategy;
import io.github.mtrevisan.calendars.rumi.RumiCalendarStrategy;
import io.github.mtrevisan.calendars.seleucid.SeleucidCalendarStrategy;
import io.github.mtrevisan.calendars.sovieteternal.SovietEternalCalendarStrategy;
import io.github.mtrevisan.calendars.swedish.SwedishCalendarStrategy;
import io.github.mtrevisan.calendars.venetan.VenetanCalendarStrategy;
import io.github.mtrevisan.calendars.yearstart.EasterStyleCalendarStrategy;
import io.github.mtrevisan.calendars.yearstart.YearStartStyleCalendarStrategy;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngineFactory;
import io.github.mtrevisan.calendars.astronomy.SunlightPhase;
import io.github.mtrevisan.calendars.astronomy.ZenithInterface;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;


/**
 * Supported calendar systems and their conversion algorithms to Julian Day Number (JDN).
 * TODO altri calendari da aggiungere (se non già presenti): juche, korean, nengo, thai, vietnamese, Senmyō
 */
public enum CalendarType implements CalendarConverterPlugin{

	GREGORIAN("gregorian", new GregorianCalendarStrategy()),
	VENETAN("venetan", new VenetanCalendarStrategy()),
	FLORENTINE("florentine", YearStartStyleCalendarStrategy.FLORENTINE),
	PISAN("pisan", YearStartStyleCalendarStrategy.PISAN),
	NATIVITY("nativity", YearStartStyleCalendarStrategy.NATIVITY),
	BYZANTINE_STYLE("byzantine_style", YearStartStyleCalendarStrategy.BYZANTINE_STYLE),
	EASTER_STYLE("easter_style", new EasterStyleCalendarStrategy()),
	JULIAN("julian", new JulianCalendarStrategy()),
	REFORMED_JULIAN("reformed_julian", new ReformedJulianCalendarStrategy()),
	SWEDISH("swedish", new SwedishCalendarStrategy()),
	QUAKER("quaker", new QuakerCalendarStrategy()),
	LITURGICAL("liturgical", LiturgicalCalendarStrategy.WESTERN),
	LITURGICAL_JULIAN("liturgical_julian", LiturgicalCalendarStrategy.JULIAN),
	ENGLISH_REGNAL("english_regnal", RegnalCalendarStrategy.ENGLISH),
	PONTIFICAL("pontifical", RegnalCalendarStrategy.PONTIFICAL),
	ISLAMIC("islamic", new IslamicCalendarStrategy()),
	BUDDHIST("buddhist", new BuddhistCalendarStrategy()),
	COPTIC("coptic", new CopticCalendarStrategy()),
	ETHIOPIAN("ethiopian", new EthiopianCalendarStrategy()),

	HEBREW("hebrew", new HebrewCalendarStrategy()),

	CHINESE("chinese", new ChineseCalendarStrategy()),
	JAPANESE("japanese", new JapaneseCalendarStrategy()),
	MINGUO("minguo", EraOffsetCalendarStrategy.MINGUO),
	DANGI("dangi", EraOffsetCalendarStrategy.DANGI),
	JUCHE("juche", EraOffsetCalendarStrategy.JUCHE),
	KOREAN("korean", new KoreanCalendarStrategy()),
	VIETNAMESE("vietnamese", new VietnameseCalendarStrategy()),
	INDIAN("indian", new IndianCalendarStrategy()),
	BENGALI("bengali", HinduSolarCalendarStrategy.BENGALI),
	BENGALI_BANGLADESH("bengali_bangladesh", new BangladeshCalendarStrategy()),
	TAMIL("tamil", HinduSolarCalendarStrategy.TAMIL),
	MALAYALAM("malayalam", HinduSolarCalendarStrategy.MALAYALAM),
	NEPALI("nepali", new NepaliCalendarStrategy()),
	VIKRAM_SAMVAT("vikram_samvat", new VikramSamvatCalendarStrategy()),

	FRENCH_REPUBLICAN("french_republican", new FrenchRepublicanCalendarStrategy()),
	SOVIET_ETERNAL("soviet_eternal", new SovietEternalCalendarStrategy()),
	MAYAN("mayan", new MayanCalendarStrategy()),

	PERSIAN("persian", new PersianCalendarStrategy()),
	PARSI("parsi", new ParsiCalendarStrategy()),
	BYZANTINE("byzantine", new ByzantineCalendarStrategy()),
	EGYPTIAN("egyptian", new EgyptianCalendarStrategy()),
	SELEUCID("seleucid", new SeleucidCalendarStrategy()),
	ARMENIAN("armenian", new ArmenianCalendarStrategy()),
	RUMI("rumi", new RumiCalendarStrategy()),
	BAHAI("bahai", new BahaiCalendarStrategy()),
	BERBER("berber", new BerberCalendarStrategy());


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
		final CalendarType[] values = values();
		for(final CalendarType type : values)
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


	// TODO write test
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
