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
package io.github.mtrevisan.calendars.mayan;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;
import io.github.mtrevisan.calendars.spi.SeasonSolver;


public class MayanCalendarStrategy implements CalendarConverterPlugin{

	/**
	 * Converts a Mayan Long Count date (expressed as year = Baktun, month = Katun, day = Tun)
	 * into a Julian Day Number (JDN).
	 * <p>
	 * Note on Correlation Constants:
	 * <ul>
	 *   <li><b>584283</b>: Standard GMT (Goodman-Martínez-Thompson) correlation; epoch 11 Aug 3114 BCE (proleptic
	 *   Gregorian, which is 6 Sep 3114 BCE in the proleptic Julian calendar).</li>
	 *   <li><b>584285</b>: Astronomical GMT correlation (2-day shift for lunar/eclipse alignment); epoch 13 Aug 3114 BCE
	 *   (proleptic Gregorian).</li>
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
			// the kin holds the Gregorian year; the anchor is the first day of the month of the station in that year, as the
			// Gregorian parser does: the nearest solstice or equinox to the middle of the year would take the winter solstice of
			// the year before
			final long anchorJdn = GregorianCalendarStrategy.toJdn(kin, CalendarSeason.gregorianSeasonMonth(baktun), 1);
			return SeasonSolver.jdnNear(anchorJdn, baktun, "Mayan");
		}
		else{
			// STANDARD LINEAR LONG COUNT ARITHMETIC PATHWAY
			// Total days elapsed = Baktun*144000 + Katun*7200 + Tun*360 + Uinal*20 + Kin
			final long totalDays = baktun * 144000L
				+ katun * 7200L
				+ tun * 360L
				+ uinal * 20L
				+ kin;

			// Mayan Era GMT Correlation Anchor (11 August 3114 BCE proleptic Gregorian = JDN 584283L)
			return totalDays + 584283L;
		}
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		return CalendarMetadata.createRawDate(rawDate);
	}

}
