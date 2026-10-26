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
package io.github.mtrevisan.calendars.indian;

import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;


/**
 * The sidereal solar calendars of India: each month begins when the Sun enters a sidereal sign (sankranti, with the
 * Lahiri ayanamsa), on the first civil day whose critical time follows the sankranti (Dershowitz and Reingold,
 * "Indian Calendrical Calculations", §4.2).
 * <ul>
 *   <li>{@link #BENGALI}: West Bengal, year from Mesha (1 Baishakh), Bengali era (Gregorian year − 593), critical time at
 *   the midnight that starts the civil day (the special rules for a sankranti around midnight are not modeled);</li>
 *   <li>{@link #TAMIL}: Tamil Nadu, year from Mesha (1 Chithirai), Saka era (Gregorian year − 78), critical time at
 *   sunset (Chennai);</li>
 *   <li>{@link #MALAYALAM}: Kerala, year from Simha (1 Chingam), Kollam era (Gregorian year − 824), critical time at 3/5 of
 *   the daytime (Thiruvananthapuram).</li>
 * </ul>
 * The modern (drik) panchangs are reproduced; the traditional ones computed with the Surya Siddhanta may differ by some
 * days (Dershowitz and Reingold).
 */
public class HinduSolarCalendarStrategy implements CalendarConverterPlugin{

	/** The time of the civil day at which the sidereal sign of the Sun is evaluated. */
	private enum CriticalTime{
		/** Midnight at the start of the civil day. */
		MIDNIGHT,
		/** Sunset. */
		SUNSET,
		/** Sunrise plus 3/5 of the daytime. */
		AFTERNOON
	}

	/** West Bengal (Bengali era, from 1 Baishakh). */
	public static final HinduSolarCalendarStrategy BENGALI = new HinduSolarCalendarStrategy("Bengali", 0, 593,
		CriticalTime.MIDNIGHT, 22.5726, 88.3639, new IndianMonthNames()
		.register(1, "Boishakh", "Baishakh", "Baisakh", "Vaishakh", "বৈশাখ")
		.register(2, "Joishtho", "Jyoishtho", "Jaishtha", "Jyeshtha", "Jaistha", "জ্যৈষ্ঠ")
		.register(3, "Asharh", "Ashar", "Asadh", "Ashadh", "আষাঢ়", "আষাঢ")
		.register(4, "Srabon", "Shrabon", "Shraban", "Sravan", "Shravan", "শ্রাবণ")
		.register(5, "Bhadro", "Bhadra", "ভাদ্র")
		.register(6, "Ashshin", "Ashwin", "Ashin", "Aswin", "আশ্বিন")
		.register(7, "Kartik", "Kartick", "কার্তিক")
		.register(8, "Ogrohayon", "Agrahayan", "Agrahayon", "Ograhayon", "অগ্রহায়ণ")
		.register(9, "Poush", "Pous", "Paush", "পৌষ")
		.register(10, "Magh", "মাঘ")
		.register(11, "Falgun", "Phalgun", "Falgoon", "ফাল্গুন")
		.register(12, "Choitro", "Chaitra", "Choitra", "চৈত্র"));

	/** Tamil Nadu (Saka era, from 1 Chithirai). */
	public static final HinduSolarCalendarStrategy TAMIL = new HinduSolarCalendarStrategy("Tamil", 0, 78,
		CriticalTime.SUNSET, 13.0827, 80.2707, new IndianMonthNames()
		.register(1, "Chithirai", "Chittirai", "Chitirai", "சித்திரை")
		.register(2, "Vaikasi", "Vaigasi", "Vaikaasi", "வைகாசி")
		.register(3, "Aani", "Ani", "ஆனி")
		.register(4, "Aadi", "Adi", "ஆடி")
		.register(5, "Aavani", "Avani", "ஆவணி")
		.register(6, "Purattasi", "Puratasi", "புரட்டாசி")
		.register(7, "Aippasi", "Ippasi", "ஐப்பசி")
		.register(8, "Karthigai", "Karthikai", "Kartikai", "கார்த்திகை")
		.register(9, "Margazhi", "Margali", "Marghazhi", "மார்கழி")
		.register(10, "Thai", "Tai", "தை")
		.register(11, "Maasi", "Masi", "மாசி")
		.register(12, "Panguni", "Pankuni", "பங்குனி"));

	/** Kerala (Kollam era, from 1 Chingam). */
	public static final HinduSolarCalendarStrategy MALAYALAM = new HinduSolarCalendarStrategy("Malayalam", 4, 824,
		CriticalTime.AFTERNOON, 8.5241, 76.9366, new IndianMonthNames()
		.register(1, "Chingam", "Chinnam", "ചിങ്ങം")
		.register(2, "Kanni", "Kanni", "കന്നി")
		.register(3, "Thulam", "Tulam", "തുലാം")
		.register(4, "Vrischikam", "Vrishchikam", "Vrichikam", "വൃശ്ചികം")
		.register(5, "Dhanu", "ധനു")
		.register(6, "Makaram", "മകരം")
		.register(7, "Kumbham", "കുംഭം")
		.register(8, "Meenam", "Minam", "മീനം")
		.register(9, "Medam", "Metam", "മേടം")
		.register(10, "Edavam", "Itavam", "ഇടവം")
		.register(11, "Mithunam", "മിഥുനം")
		.register(12, "Karkidakam", "Karkatakam", "Karkkidakam", "കർക്കടകം", "കര്‍ക്കടകം"));

	/** Mean length of a solar month [days]. */
	private static final double MEAN_SOLAR_MONTH = 365.25636 / 12.;
	private static final double AFTERNOON_FRACTION = 3. / 5.;


	private final String name;
	/** The sign (0 = Mesha) of the first month. */
	private final int firstSign;
	/** The difference between the Gregorian year in which the year starts and the year number. */
	private final int eraOffset;
	private final CriticalTime criticalTime;
	private final double latitude;
	private final double longitude;
	private final IndianMonthNames monthNames;


	private HinduSolarCalendarStrategy(final String name, final int firstSign, final int eraOffset,
			final CriticalTime criticalTime, final double latitude, final double longitude, final IndianMonthNames monthNames){
		this.name = name;
		this.firstSign = firstSign;
		this.eraOffset = eraOffset;
		this.criticalTime = criticalTime;
		this.latitude = latitude;
		this.longitude = longitude;
		this.monthNames = monthNames;
	}


	@Override
	public long parseToJdn(final String rawDate){
		final IndianMonthNames.SolarDate date = monthNames.parse(rawDate, name);
		final int month = date.monthOrFirst();
		final int day = date.dayOrFirst();

		final long monthStart = monthStartJdn(date.year(), month);
		final long nextMonthStart = monthStartJdn(date.year(), month + 1);
		if(day < 1 || monthStart + day - 1 >= nextMonthStart)
			throw new IllegalArgumentException("The month " + month + " of the " + name + " year " + date.year() + " has "
				+ (nextMonthStart - monthStart) + " days: " + rawDate);
		return monthStart + day - 1;
	}

	/**
	 * @param year  the year
	 * @param month the month (1 to 13, the 13th being the first month of the next year)
	 * @return the Julian Day Number of the first day of the month
	 */
	long monthStartJdn(final int year, final int month){
		final int sign = (firstSign + month - 1) % 12;
		// Mesha sankranti is around 14 April
		final double approximate = GregorianCalendarStrategy.toJdn(year + eraOffset, 4, 14)
			+ (firstSign + month - 1) * MEAN_SOLAR_MONTH;
		final double sankranti = HinduAstronomy.sankranti(approximate, sign);

		final long day = HinduAstronomy.civilDay(sankranti);
		return (criticalTime(day) >= sankranti? day: day + 1);
	}

	private double criticalTime(final long day){
		return switch(criticalTime){
			case MIDNIGHT -> day - 0.5 - HinduAstronomy.IST_OFFSET;
			case SUNSET -> HinduAstronomy.sunriseSunset(day, latitude, longitude)[1];
			case AFTERNOON -> {
				final double[] sun = HinduAstronomy.sunriseSunset(day, latitude, longitude);
				yield sun[0] + AFTERNOON_FRACTION * (sun[1] - sun[0]);
			}
		};
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final IndianMonthNames.SolarDate input = monthNames.parse(rawDate, name);
		return CalendarMetadata.createFromMonthDay(input.month(), input.day());
	}

}
