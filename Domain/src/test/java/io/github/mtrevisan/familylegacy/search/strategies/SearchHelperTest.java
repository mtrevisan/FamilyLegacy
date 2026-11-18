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
package io.github.mtrevisan.familylegacy.search.strategies;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class SearchHelperTest{

	@Test
	void theYearOfTheDateFilterIsComparedWithTheYearsOfTheEvent(){
		final FLEFModel model = new FLEFParser().parse("""
			header {
				date 2026-01-01T00:00:00Z
			}
			event E1 {
				type birth
				date.value.point {
					value.full_date 1850-05-01
					calendar gregorian
				}
				audit.creation.date 2026-01-01T00:00:00Z
			}
			""");
		final FLEFRecord event = model.getRecordById("E1");

		Assertions.assertTrue(SearchHelper.matchesDate(event, "1850-05-01", "gregorian"));
		Assertions.assertTrue(SearchHelper.matchesDate(event, "1850-12-31", null));
		Assertions.assertFalse(SearchHelper.matchesDate(event, "1851-01-01", "gregorian"));
		Assertions.assertTrue(SearchHelper.matchesDate(event, "", "gregorian"));
	}

	@Test
	void anEmptyOrUnknownCalendarDoesNotBreakTheYear(){
		Assertions.assertDoesNotThrow(() -> SearchHelper.extractYear("1850-05-01", ""));
		Assertions.assertNull(SearchHelper.extractYear("1850-05-01", "no_such_calendar"));
	}

}
