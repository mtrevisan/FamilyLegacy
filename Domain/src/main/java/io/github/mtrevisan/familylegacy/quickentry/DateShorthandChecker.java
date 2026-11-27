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
package io.github.mtrevisan.familylegacy.quickentry;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.calendars.spi.DateInterpretation;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateShorthand;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


/**
 * Checks the full dates of a date typed in the shorthand against their calendar, as the date editor does when it is saved: the shorthand
 * reader only splits the expression, it is the calendar that knows whether {@code 31 February 1850} is a date, and whether a word can be
 * read in more than one way (the same word may be a season in one language and a month in another).
 */
public final class DateShorthandChecker{

	/** What is wrong with a full date. */
	public enum Kind{

		/** The calendar does not recognize it (it is not a date of that calendar: {@code 33 Foo 1850}, {@code 31 February 1850}). */
		UNRECOGNIZED,
		/** It can be read in more than one way: the user chooses which. */
		AMBIGUOUS

	}

	/**
	 * A problem of a full date.
	 *
	 * @param kind	What is wrong.
	 * @param date	The full date, as typed.
	 * @param reason	Why the calendar does not recognize it ({@link Kind#UNRECOGNIZED}), or empty.
	 * @param readings	The ways to read it ({@link Kind#AMBIGUOUS}), or empty.
	 */
	public record Problem(Kind kind, String date, String reason, List<DateInterpretation> readings){

		public Problem{
			readings = List.copyOf(readings);
		}

	}


	private DateShorthandChecker(){}


	/**
	 * @param shorthand	The date as the shorthand reader understood it.
	 * @return	The full dates it holds (a point has one, a bounded or spanning date has up to two), in order; the decades and the centuries
	 * 	are not full dates.
	 */
	public static List<String> fullDatesOf(final DateShorthand shorthand){
		final List<String> dates = new ArrayList<>(2);
		if(shorthand.first() != null && shorthand.first().fullDate() != null)
			dates.add(shorthand.first().fullDate());
		if(shorthand.second() != null && shorthand.second().fullDate() != null)
			dates.add(shorthand.second().fullDate());
		return dates;
	}

	/**
	 * @param fullDate	The full date, in the notation of the calendar.
	 * @param calendarCode	The code of the calendar of the date.
	 * @param locale	The language that the words of the date are read in first.
	 * @return	What is wrong with the date, {@code null} if the calendar recognizes it with a single reading (or cannot say: an unknown
	 * 	calendar, or one whose conversion needs a plugin that is missing).
	 */
	public static Problem check(final String fullDate, final String calendarCode, final Locale locale){
		final CalendarType calendar = CalendarType.fromCode(calendarCode);
		//a calendar unknown to the parsers cannot be checked
		if(!calendar.getCode().equalsIgnoreCase(calendarCode))
			return null;

		final List<DateInterpretation> readings = calendar.getInterpretations(fullDate, locale);
		if(!readings.isEmpty())
			return new Problem(Kind.AMBIGUOUS, fullDate, StringUtils.EMPTY, readings);

		try{
			calendar.parseToJdn(fullDate);

			return null;
		}
		catch(final UnsupportedOperationException uoe){
			//the date is recognized, but its conversion needs a missing plugin (e.g. the seasons need the Ephemeris)
			return null;
		}
		catch(final RuntimeException re){
			return new Problem(Kind.UNRECOGNIZED, fullDate, Objects.toString(re.getMessage(), StringUtils.EMPTY), List.of());
		}
	}

}
