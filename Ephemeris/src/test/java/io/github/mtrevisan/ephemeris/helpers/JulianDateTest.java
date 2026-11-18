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
package io.github.mtrevisan.ephemeris.helpers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;


class JulianDateTest{

	@Test
	void julianCalendarBeforeTheReform(){
		Assertions.assertEquals(2299159.5, JulianDate.of(1582, 10, 4), 1.e-9);
		Assertions.assertEquals(2299160.5, JulianDate.of(1582, 10, 15), 1.e-9);
		//one day before 1580-01-01 Julian is 1579-12-31
		Assertions.assertEquals(JulianDate.of(1580, 1, 1) - 1., JulianDate.of(1579, 12, 31), 1.e-9);
		Assertions.assertEquals(2299159.5 - 277. - 731., JulianDate.of(1580, 1, 1) + 0., 1.e-9 + 2.);
	}

	@Test
	void j2000(){
		Assertions.assertEquals(2451544.5, JulianDate.of(2000, 1, 1), 1.e-9);
	}

	@Test
	void dateTimeRollsOverInsteadOfThrowing(){
		Assertions.assertEquals(LocalDateTime.of(2000, 1, 2, 0, 0), JulianDate.dateTimeOf(2451544.5 + 0.999999999));
	}

	@Test
	void extractDateAndTime(){
		final double[] first = JulianDate.extractDateAndTime(2451544.7);
		Assertions.assertEquals(2451544.5, first[0], 1.e-9);
		Assertions.assertEquals(0.2, first[1], 1.e-9);
		final double[] second = JulianDate.extractDateAndTime(2451545.3);
		Assertions.assertEquals(2451544.5, second[0], 1.e-9);
		Assertions.assertEquals(0.8, second[1], 1.e-9);
	}

}
