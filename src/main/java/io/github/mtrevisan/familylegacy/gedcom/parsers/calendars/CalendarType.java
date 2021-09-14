/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import java.util.ArrayList;
import java.util.List;


public enum CalendarType{

	GREGORIAN("Gregorian", CalendarParserBuilder.CALENDAR_GREGORIAN),
	JULIAN("Julian", CalendarParserBuilder.CALENDAR_JULIAN),
	FRENCH_REPUBLICAN("French Republican", CalendarParserBuilder.CALENDAR_FRENCH_REPUBLICAN),
	HEBREW("Hebrew", CalendarParserBuilder.CALENDAR_HEBREW);


	private final String description;
	private final String type;


	public static CalendarType createFromIndex(final int index){
		return values()[index];
	}

	CalendarType(final String description, final String type){
		this.description = description;
		this.type = type;
	}

	public static String[] getDescriptions(){
		final CalendarType[] values = values();
		final List<String> list = new ArrayList<>(values.length);
		for(final CalendarType calendarType : values)
			list.add(calendarType.description);
		return list.toArray(new String[0]);
	}

	public String getType(){
		return type;
	}

	public boolean isGregorianOrJulian(){
		return (this == GREGORIAN || this == JULIAN);
	}

}
