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
package io.github.mtrevisan.familylegacy.io.model.readers.date.byzantine;

import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarConverterPlugin;
import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarType;

import java.util.LinkedHashMap;
import java.util.Map;


public class ByzantineCalendarStrategy implements CalendarConverterPlugin{

	@Override
	public long parseToJdn(final String rawDate){
		final ByzantineDateInput input = ByzantineCalendarParser.parse(rawDate);
		final int year = input.year();
		final int month = input.month();
		// a missing day stands for the first day of the month
		final int day = (input.day() > 0? input.day(): 1);

		final int julianYear;
		final int julianMonth;
		if(month <= 4){
			julianYear = year - 5509;
			// Sep=9, Oct=10, Nov=11, Dec=12
			julianMonth = month + 8;
		}
		else{
			julianYear = year - 5508;
			// Jan=1, Feb=2, ..., Aug=8
			julianMonth = month - 4;
		}

		// Adjust year boundary for the historical astronomical algorithm anchor
		final int y = (julianMonth <= 2? julianYear - 1: julianYear);
		final int m = (julianMonth <= 2? julianMonth + 12: julianMonth);

		// Standard Julian Calendar JDN conversion formula
		return Math.floorDiv(1461L * (y + 4716L), 4L)
			+ Math.floorDiv(153L * (m + 1), 5L)
			+ day - 1524L;
	}

	@Override
	public CalendarType.CalendarMetadata getMetadata(final String rawDate){
		final ByzantineDateInput input = ByzantineCalendarParser.parse(rawDate);
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put("year", (input.year() != 0));
		components.put("month", (input.month() != 0));
		components.put("day", (input.day() != 0));

		final boolean isComplete = (input.year() != 0 && input.month() != 0 && input.day() != 0);
		return new CalendarType.CalendarMetadata(components, isComplete);
	}

}
