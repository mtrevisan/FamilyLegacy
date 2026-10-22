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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class PersianCalendarConverterTest{

	@Test
	void islamicRevolution(){
		// Victory of the Islamic Revolution (11 Feb 1979 Gregorian)
		final long jdn = CalendarType.PERSIAN.parseToJdn("22 Bahman 1357");

		Assertions.assertEquals(2443916L, jdn);
	}

	@Test
	void islamicRevolutionNative(){
		// Victory of the Islamic Revolution, Persian script and digits (11 Feb 1979 Gregorian)
		final long jdn = CalendarType.PERSIAN.parseToJdn("۲۲ بهمن ۱۳۵۷");

		Assertions.assertEquals(2443916L, jdn);
	}

	@Test
	void nowruz1403(){
		// Nowruz, equinox at 06:36 Tehran time, before noon (20 Mar 2024 Gregorian)
		final long jdn = CalendarType.PERSIAN.parseToJdn("1 Farvardin 1403");

		Assertions.assertEquals(2460390L, jdn);
	}

	@Test
	void nowruz1404(){
		// Nowruz, equinox at 12:31 Tehran time, after noon, so 1403 has 30 Esfand (21 Mar 2025 Gregorian)
		final long jdn = CalendarType.PERSIAN.parseToJdn("1 Farvardin 1404");

		Assertions.assertEquals(2460756L, jdn);
	}

	@Test
	void nowruz1399(){
		// Nowruz, equinox at 07:19 Tehran time, before noon (20 Mar 2020 Gregorian)
		final long jdn = CalendarType.PERSIAN.parseToJdn("1 Farvardin 1399");

		Assertions.assertEquals(2458929L, jdn);
	}

	@Test
	void nowruz1400(){
		// Nowruz, equinox at 13:07 Tehran time, after noon, so 1399 has 30 Esfand (21 Mar 2021 Gregorian)
		final long jdn = CalendarType.PERSIAN.parseToJdn("1 Farvardin 1400");

		Assertions.assertEquals(2459295L, jdn);
	}

	@Test
	void abanNative(){
		// Persian script (22 Oct 2024 Gregorian)
		final long jdn = CalendarType.PERSIAN.parseToJdn("1 آبان 1403");

		Assertions.assertEquals(2460606L, jdn);
	}

}
