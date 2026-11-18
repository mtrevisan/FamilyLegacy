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
package io.github.mtrevisan.calendars.spi;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


public record CalendarMetadata(Map<String, Boolean> nativeComponents, boolean isComplete){

	public static final String COMPONENT_YEAR = "year";
	public static final String COMPONENT_MONTH = "month";
	public static final String COMPONENT_DAY = "day";


	/**
	 * Builds the metadata of a date whose missing year is written as 0 (for the calendars that have no year 0). The
	 * calendars that have the year 0 (e.g. the astronomical year of the Gregorian one) must use
	 * {@link #createFromYearMonthDay(boolean, int, int)}.
	 */
	public static CalendarMetadata createFromYearMonthDay(final int year, final int month, final int day){
		return createFromYearMonthDay(year != 0, month, day);
	}

	/**
	 * Builds the metadata of a date whose year is known to be present or not, independently of its value (the year 0 is a
	 * valid astronomical year, 1 BC, and must not be taken for a missing one).
	 *
	 * @param yearPresent whether the date has the year
	 * @param month       the month (0 if missing)
	 * @param day         the day (0 if missing)
	 * @return the metadata
	 */
	public static CalendarMetadata createFromYearMonthDay(final boolean yearPresent, final int month, final int day){
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put(COMPONENT_YEAR, yearPresent);
		components.put(COMPONENT_MONTH, (month != 0));
		components.put(COMPONENT_DAY, (day != 0));

		final boolean isComplete = (yearPresent && month != 0 && day != 0);
		return new CalendarMetadata(components, isComplete);
	}

	public static CalendarMetadata createFromMonthDay(final int month, final int day){
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put(COMPONENT_YEAR, true);
		components.put(COMPONENT_MONTH, (month != 0));
		components.put(COMPONENT_DAY, (day != 0));

		final boolean isComplete = (month != 0 && day != 0);
		return new CalendarMetadata(components, isComplete);
	}

	public static CalendarMetadata createFromMonthMinusOneDay(final int month, final int day){
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put(COMPONENT_YEAR, true);
		components.put(COMPONENT_MONTH, (month != -1));
		components.put(COMPONENT_DAY, (day != 0));

		final boolean isComplete = (month != -1 && day != 0);
		return new CalendarMetadata(components, isComplete);
	}

	public static CalendarMetadata createTrue(){
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put(COMPONENT_YEAR, true);
		components.put(COMPONENT_MONTH, true);
		components.put(COMPONENT_DAY, true);

		return new CalendarMetadata(components, true);
	}

	public static CalendarMetadata createRawDate(final String rawDate){
		final boolean isComplete = (StringUtils.countMatches(rawDate, '.') == 4);
		return new CalendarMetadata(Collections.emptyMap(), isComplete);
	}

	public static CalendarMetadata createLunisolar(final int month, final int tithi){
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put(CalendarMetadata.COMPONENT_YEAR, true);
		components.put(CalendarMetadata.COMPONENT_MONTH, (month != -1));
		components.put(CalendarMetadata.COMPONENT_DAY, (tithi != 0));

		return new CalendarMetadata(components, (month != -1 && tithi != 0));
	}


	public CalendarMetadata{
		// defensive copy, keeping the order of the components
		nativeComponents = Collections.unmodifiableMap(new LinkedHashMap<>(nativeComponents));
	}

}
