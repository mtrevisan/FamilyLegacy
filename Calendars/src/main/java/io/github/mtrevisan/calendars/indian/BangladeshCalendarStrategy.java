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

import io.github.mtrevisan.calendars.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * The revised Bengali calendar (Bangabda) of Bangladesh: a fixed solar calendar whose year starts on 14 April
 * (1 Boishakh), with the year counted as the Gregorian one minus 593 (e.g. "1 Boishakh 1431" is 14 April 2024).
 * <ul>
 *   <li>From 1426 (2019) the months from Boishakh to Ashshin have 31 days, the others 30, except Falgun with 29 days, or
 *   30 in the years whose Falgun contains 29 February (Bangla Academy revision, effective from 16 October 2019).</li>
 *   <li>Up to 1425 (the 1966 Shahidullah reform, adopted in 1987, applied here also to the earlier years) the months
 *   from Boishakh to Bhadro have 31 days, the others 30, and Falgun 31 days in a leap year.</li>
 * </ul>
 * Sources: Bangla Academy, as quoted by The Daily Star (13 February 2020); en.wikipedia "Bengali calendar".
 */
public class BangladeshCalendarStrategy implements CalendarConverterPlugin{

	/** The difference between the Gregorian and the Bengali year numbers, at 1 Boishakh. */
	private static final int ERA_OFFSET = 593;
	/** The first year of the 2019 revision. */
	private static final int REVISION_2019_YEAR = 1426;
	private static final int FALGUN = 11;

	private static final IndianMonthNames MONTH_NAMES = new IndianMonthNames()
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
		.register(12, "Choitro", "Chaitra", "Choitra", "চৈত্র");


	@Override
	public long parseToJdn(final String rawDate){
		final IndianMonthNames.SolarDate date = MONTH_NAMES.parse(rawDate, "Bengali");
		final int year = date.year();
		final int month = date.monthOrFirst();
		final int day = date.dayOrFirst();

		final int length = monthLength(year, month);
		if(day < 1 || day > length)
			throw new IllegalArgumentException("The month " + month + " of " + year + " has " + length + " days: " + rawDate);

		long jdn = GregorianCalendarStrategy.toJdn(year + ERA_OFFSET, 4, 14);
		for(int m = 1; m < month; m ++)
			jdn += monthLength(year, m);
		return jdn + day - 1;
	}

	private static int monthLength(final int year, final int month){
		// the Falgun of the year B falls in the Gregorian year B + 594
		final int gregorianYear = year + ERA_OFFSET + 1;
		final boolean leap = (gregorianYear % 4 == 0 && (gregorianYear % 100 != 0 || gregorianYear % 400 == 0));
		if(year >= REVISION_2019_YEAR){
			if(month <= 6)
				return 31;
			return (month == FALGUN? (leap? 30: 29): 30);
		}
		if(month <= 5)
			return 31;
		return (month == FALGUN && leap? 31: 30);
	}

	@Override
	public CalendarType.CalendarMetadata getMetadata(final String rawDate){
		final IndianMonthNames.SolarDate date = MONTH_NAMES.parse(rawDate, "Bengali");
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put("year", true);
		components.put("month", (date.month() != 0));
		components.put("day", (date.day() != 0));
		return new CalendarType.CalendarMetadata(components, (date.month() != 0 && date.day() != 0));
	}

}
