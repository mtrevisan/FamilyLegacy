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

import io.github.mtrevisan.calendars.gregorian.CalendarReform;
import io.github.mtrevisan.calendars.gregorian.CalendarReforms;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.gregorian.JulianCalendarStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class CalendarReformsTest{

	@Test
	void everyRegisteredReformIsConsistent(){
		for(final String jurisdiction : CalendarReforms.jurisdictions()){
			final CalendarReform reform = CalendarReforms.resolve(jurisdiction).orElseThrow();
			final long lastJulian = JulianCalendarStrategy.toJdn(reform.lastJulianYear(), reform.lastJulianMonth(),
				reform.lastJulianDay());
			final long firstGregorian = GregorianCalendarStrategy.toJdn(reform.firstGregorianYear(),
				reform.firstGregorianMonth(), reform.firstGregorianDay());
			Assertions.assertEquals(lastJulian + 1, firstGregorian, jurisdiction);
		}
	}

	@Test
	void mostSpecificWins(){
		Assertions.assertEquals(CalendarReform.ROME_1583, CalendarReforms.resolve("Austria/Tyrol").orElseThrow());
		Assertions.assertEquals(new CalendarReform(1583, 12, 14, 1583, 12, 25),
			CalendarReforms.resolve("austria/carinthia").orElseThrow());
		Assertions.assertEquals(CalendarReform.NETHERLANDS_GELDERLAND,
			CalendarReforms.resolve("Netherlands/Gelderland").orElseThrow());
		Assertions.assertEquals(new CalendarReform(1583, 2, 10, 1583, 2, 21),
			CalendarReforms.resolve("Belgium/Liege").orElseThrow());
	}

	@Test
	void switzerland(){
		Assertions.assertTrue(CalendarReforms.resolve("Switzerland").isEmpty());
		Assertions.assertEquals(new CalendarReform(1584, 1, 11, 1584, 1, 22), CalendarReforms.resolve("Switzerland/Luzern").orElseThrow());
		Assertions.assertEquals(CalendarReforms.resolve("Switzerland/Protestant"), CalendarReforms.resolve("Switzerland/Zürich"));
		Assertions.assertEquals(CalendarReforms.resolve("Switzerland/Protestant"), CalendarReforms.resolve("Switzerland/Zurich"));
		// the commune falls back to its canton group when it has no entry of its own
		Assertions.assertEquals(new CalendarReform(1811, 12, 25, 1812, 1, 7),
			CalendarReforms.resolve("Switzerland/Graubünden/Schiers").orElseThrow());
		Assertions.assertTrue(CalendarReforms.resolve("Switzerland/Graubünden/Chur").isEmpty());
	}

	@Test
	void unknownSubdivisionFallsBackToTheCountry(){
		Assertions.assertEquals(CalendarReform.FRANCE, CalendarReforms.resolve("France/Alsace").orElseThrow());
	}

	@Test
	void countriesWithoutNationalDateHaveNoFallback(){
		Assertions.assertTrue(CalendarReforms.resolve("Austria").isEmpty());
		Assertions.assertTrue(CalendarReforms.resolve("Netherlands").isEmpty());
		Assertions.assertTrue(CalendarReforms.resolve("Belgium").isEmpty());
		Assertions.assertTrue(CalendarReforms.resolve("Nowhere").isEmpty());
		Assertions.assertTrue(CalendarReforms.resolve(null).isEmpty());
		Assertions.assertTrue(CalendarReforms.subdivisions("Austria").contains("Austria/Tyrol"));
	}

}
