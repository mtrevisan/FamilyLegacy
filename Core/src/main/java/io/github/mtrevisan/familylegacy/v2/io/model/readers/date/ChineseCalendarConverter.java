package io.github.mtrevisan.familylegacy.v2.io.model.readers.date;

import io.github.mtrevisan.familylegacy.v2.services.AstronomicalEngine;
import io.github.mtrevisan.familylegacy.v2.services.AstronomicalEngineFactory;


/**
 * Converter between the Chinese lunisolar calendar and the Gregorian
 * calendar.
 *
 * <p>The Chinese calendar used here is the <b>Shixian</b> (時憲曆) calendar
 * adopted in 1645 and still in use today. It is defined by the following
 * astronomical rules:</p>
 *
 * <ol>
 *   <li>Each lunar month begins on the day of the astronomical new moon
 *       (conjunction in apparent ecliptic longitude) as observed from the
 *       meridian of Beijing (UTC+8).</li>
 *   <li>The winter solstice (apparent solar longitude 270°) always falls
 *       in month 11 of the Chinese year.</li>
 *   <li>Chinese New Year (month 1) is the second new moon after the
 *       winter solstice, unless a leap month is inserted before it, in
 *       which case it is the third.</li>
 *   <li>If there are 13 lunar months between successive month-11's, the
 *       first month that does not contain a <em>zhongqi</em> (major solar
 *       term, a multiple of 30° of apparent solar longitude) is
 *       designated as a leap month, taking the same number as the month
 *       before it (e.g. 閏四月 "leap-4" is the extra month after
 *       month 4).</li>
 * </ol>
 *
 * <p><b>Range of validity.</b> The astronomical ephemerides used here
 * cover the interval from roughly 2000 BC to 3000 AD. For dates before
 * 1645 the historical Shixian rules did not apply, and for dates before
 * the Ming dynasty (1368–1644, the Datong calendar) the modern rule set
 * was not in use at all. The converter nonetheless produces a plausible
 * extrapolation for earlier dates; treat the result as an approximation
 * outside the Shixian period.</p>
 *
 * <p><b>References.</b> See Helmer Aslaksen, <i>The Mathematics of the
 * Chinese Calendar</i>, and the Chinese Academy of Sciences' Purple
 * Mountain Observatory publications on the Shixian calendar.</p>
 */
public final class ChineseCalendarConverter{

	/** Mean synodic month length [days]. */
	private static final double MEAN_SYNODIC_MONTH = 29.530588853;

	/**
	 * Apparent solar longitude that defines the winter solstice (Dongzhi, 冬至).
	 */
	private static final double WINTER_SOLSTICE_DEG = 270.;


	private ChineseCalendarConverter(){}

	/**
	 * Converts a Chinese calendar date to Julian Day Number (JDN).
	 *
	 * @param year  Chinese year
	 * @param month Chinese month (1-12)
	 * @param day   Chinese day (1-30)
	 * @return Corresponding JDN
	 */
	public static long toJdn(final int year, final int month, final int day){
		final AstronomicalEngine engine = AstronomicalEngineFactory.getEngine();
		if(!engine.isAvailable())
			throw new UnsupportedOperationException("Chinese calendar requires the optional Sunset astronomical plugin JAR in classpath.");

		// 1. Calculate the Winter Solstice of the previous Chinese astronomical year (December of Gregorian year - 1)
		final double utcOffset = getChinaUtcOffset(year);
		final double winterSolsticeJdn = engine.getSolarLongitudeJdn(year - 1, WINTER_SOLSTICE_DEG,
			utcOffset);

		// 2. Find the New Moon on or immediately preceding the Winter Solstice (Month 11)
		final long month11StartJdn = getNewMoonJdnOnOrBefore(engine, winterSolsticeJdn, utcOffset);

		// 3. Locate the New Moon corresponding to Chinese New Year (Month 1)
		// Month 12
		long newYearJdn = engine.getNextNewMoonJdn(month11StartJdn + 1., utcOffset);
		// Month 1
		newYearJdn = engine.getNextNewMoonJdn(newYearJdn + 1., utcOffset);

		// Check for an intercalary (leap) month inserted between Month 11 and Month 1
		if(newYearJdn - month11StartJdn > 60)
			newYearJdn = engine.getNextNewMoonJdn(newYearJdn + 1., utcOffset);

		// 4. Advance through lunar months to reach the target month
		long currentMonthStartJdn = newYearJdn;
		for(int m = 1; m < month; m ++)
			currentMonthStartJdn = engine.getNextNewMoonJdn(currentMonthStartJdn + 1., utcOffset);

		// Add 1-based day offset
		return currentMonthStartJdn + day - 1;
	}

	private static double getChinaUtcOffset(final int gregorianYear){
		if(gregorianYear >= 1929)
			// Standard UTC+8 (120° E) adopted in 1929
			return 8.;

		if(gregorianYear >= 1645)
			// Beijing Local Mean Time (116° 25' E = 7h 45m 40s) for Qing Dynasty Shixian calendar
			return 7.761111;

		// Before 1645: Nanjing / Historical Imperial Observatories (~UTC+7.9)
		return 7.9;
	}

	private static long getNewMoonJdnOnOrBefore(final AstronomicalEngine engine, final double jdn,
			final double utcOffset){
		final long targetLocalJdn = (long)Math.floor(jdn + utcOffset / 24.);

		// Start searching from one mean synodic month prior
		double searchJdn = jdn - MEAN_SYNODIC_MONTH;
		long nextNewMoon = engine.getNextNewMoonJdn(searchJdn, utcOffset);

		long candidate = nextNewMoon;
		while(nextNewMoon <= targetLocalJdn){
			candidate = nextNewMoon;
			nextNewMoon = engine.getNextNewMoonJdn(candidate + 1., utcOffset);
		}

		return candidate;
	}

}
