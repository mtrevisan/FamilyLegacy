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
package io.github.mtrevisan.familylegacy.io.model.readers.date.mayan;

import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarConverterPlugin;
import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarType;
import io.github.mtrevisan.familylegacy.services.EphemerisEngine;
import io.github.mtrevisan.familylegacy.services.EphemerisEngineFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;


public class MayanCalendarStrategy implements CalendarConverterPlugin{

	/**
	 * Converts a Mayan Long Count date (expressed as year = Baktun, month = Katun, day = Tun)
	 * into a Julian Day Number (JDN).
	 * <p>
	 * Note on Correlation Constants:
	 * <ul>
	 *   <li><b>584283</b>: Standard GMT (Goodman-Martínez-Thompson) correlation; epoch 11 Aug 3114 BCE (proleptic Julian).</li>
	 *   <li><b>584285</b>: Astronomical GMT correlation (2-day shift for lunar/eclipse alignment); epoch 13 Aug 3114 BCE.</li>
	 *   <li><b>489384</b>: Spinden correlation (historical alternative shift).</li>
	 * </ul>
	 */
	@Override
	public long parseToJdn(final String rawDate){
		// 1. Unpack components using the specialized Unicode Mayan text parser
		final MayanDateInput input = MayanCalendarParser.parse(rawDate);
		final int baktun = input.baktun();
		final int katun = input.katun();
		final int tun = input.tun();
		final int uinal = input.uinal();
		final int kin = input.kin();

		// 2. Evaluate tracking branch
		if(baktun < 0){
			// Step A: Approximate a baseline JDN anchor for the center of the targeted Gregorian year.
			// Mayan kin holds the year value. Gregorian 1970-01-01 = JDN 2440588L.
			// We calculate June 1st of that target year as our search anchor guess.
			final int targetYear = kin;

			// Establish a simple leap-safe baseline calculation to anchor our initial search window
			final long baselineApproxJdn = 1721426L + (long)((targetYear - 1) * 365.2425) + 152L;
			final double approximateMiddleJdUT = baselineApproxJdn - 0.5;

			// Step B: Map negative flag parameters directly onto true solar ecliptic target boundaries
			// Matching standard alignments: Autumn (-1) = 𝜋, Winter (-2) = 3𝜋/2, Spring (-3) = 0, Summer (-4) = 𝜋/2
			final double targetLongitude = CalendarType.getGeneralTargetLongitude(baktun);

			// Step C: Trigger exact calculation to find the point where the Sun enters this station
			final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
			final double exactSeasonJdUT = engine.findSolarLongitudeEvent(approximateMiddleJdUT, targetLongitude);

			return (long)Math.floor(exactSeasonJdUT + 0.5);
		}
		else{
			// STANDARD LINEAR LONG COUNT ARITHMETIC PATHWAY
			// Total days elapsed = Baktun*144000 + Katun*7200 + Tun*360 + Uinal*20 + Kin
			final long totalDays = baktun * 144000L
				+ katun * 7200L
				+ tun * 360L
				+ uinal * 20L
				+ kin;

			// Mayan Era GMT Correlation Anchor (11 August 3114 BC proleptic Gregorian = JDN 584283L)
			return totalDays + 584283L;
		}
	}

	@Override
	public CalendarType.CalendarMetadata getMetadata(final String rawDate) {
		final boolean isComplete = (StringUtils.countMatches(rawDate, '.') == 4);
		return new CalendarType.CalendarMetadata(Collections.emptyMap(), isComplete);
	}

}
