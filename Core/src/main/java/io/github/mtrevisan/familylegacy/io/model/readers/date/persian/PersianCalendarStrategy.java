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
package io.github.mtrevisan.familylegacy.io.model.readers.date.persian;

import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarConverterPlugin;
import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarType;
import io.github.mtrevisan.familylegacy.services.EphemerisEngine;
import io.github.mtrevisan.familylegacy.services.EphemerisEngineFactory;

import java.util.LinkedHashMap;
import java.util.Map;


public class PersianCalendarStrategy implements CalendarConverterPlugin{

	@Override
	public long parseToJdn(final String rawDate){
		// Expected native format: "۱ فروردین ۱۳۰۰" or "۱۰ رمضان ۱۲۶۸ هـ.ق"
		final PersianDateInput date = PersianCalendarParser.parse(rawDate);
		final int year = date.year();
		final int month = date.month();
		final int day = date.day();

		if(month < 0){
			// Step A: Determine an approximate baseline anchor for the year.
			// Solar Hijri Year + 622 gives the Gregorian year parameter space.
			final long baselineApproxJdn = 1948320L + (long)((year - 1) * 365.24219);
			// Middle of the year shift
			final double approximateMiddleJdUT = baselineApproxJdn + 182. - 0.5;

			// Step B: Map negative flag parameters directly onto true solar ecliptic target boundaries
			final double targetLongitude = CalendarType.getGeneralTargetLongitude(month);

			// Step C: Execute high-fidelity calculation to track the exact instant the Sun pierces this seasonal step
			final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
			final double exactSeasonJdUT = engine.findSolarLongitudeEvent(approximateMiddleJdUT, targetLongitude);

			return (long)Math.floor(exactSeasonJdUT + 0.5);
		}
		else
			// Standard numeric execution path using your existing arithmetic baseline formula for Persian dates
			// e.g., mapping back via your custom Persian JDN translation algorithms
			return toJdn(year, month, day);
	}

	private static long toJdn(final int year, final int month, final int day){
		// Persian epoch base year
		final long epBase = year - (year >= 0? 474: 473);

		// Year inside the 2820-year cycle
		final long epYear = 474 + Math.floorMod(epBase, 2820);

		// Days elapsed in previous months
		final long monthDays = (month <= 7
			? (month - 1) * 31L
			: (month - 1) * 30L + 6L);

		// Julian Day Number
		return day + monthDays + (epYear * 682 - 110) / 2816 + (epYear - 1) * 365
			+ Math.floorDiv(epBase, 2820) * 1_029_983L + 1_948_319L;
	}

	@Override
	public CalendarType.CalendarMetadata getMetadata(final String rawDate) {
		final PersianDateInput input = PersianCalendarParser.parse(rawDate);
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put("year", (input.year() != 0));
		components.put("month", (input.month() != 0));
		components.put("day", (input.day() != 0));

		final boolean isComplete = (input.year() != 0 && input.month() != 0 && input.day() != 0);
		return new CalendarType.CalendarMetadata(components, isComplete);
	}

}
