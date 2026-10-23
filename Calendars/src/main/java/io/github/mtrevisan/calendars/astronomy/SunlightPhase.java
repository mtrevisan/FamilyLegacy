/**
 * Copyright (c) 2021 Mauro Trevisan
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
package io.github.mtrevisan.calendars.astronomy;

import java.time.ZonedDateTime;


/** Result types for sunlight phases calculations. */
public sealed interface SunlightPhase{

	ZonedDateTime transit();


	/**
	 * Result type for a day with sunrise and sunset.
	 * <p>
	 * An event that does not happen during the day is {@code null}: this happens about once a month for each event of
	 * the Moon (that rises, transits and sets about 50 minutes later each day), and for the Sun close to the polar day
	 * or night.
	 * </p>
	 *
	 * @param sunrise	Time of sunrise (or of the rising of the body).
	 * @param transit	Time of transit (culmination), i.e. when the sun is closest to the zenith.
	 * @param sunset	Time of sunset (or of the setting of the body).
	 */
	record RegularDay(ZonedDateTime sunrise, ZonedDateTime transit, ZonedDateTime sunset) implements SunlightPhase{}

	/**
	 * A day on which the sun is above the horizon all the time (polar day).
	 *
	 * @param transit	Time of transit (culmination), i.e. when the sun is closest to the zenith.
	 */
	record AlwaysDay(ZonedDateTime transit) implements SunlightPhase{}

	/**
	 * A day on which the sun is below the horizon all the time (polar night).
	 *
	 * @param transit	Time of transit (culmination), i.e. when the sun is closest to the zenith.
	 */
	record AlwaysNight(ZonedDateTime transit) implements SunlightPhase{}

}
