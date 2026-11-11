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
package io.github.mtrevisan.calendars.gregorian;

import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;
import io.github.mtrevisan.calendars.spi.DateInterpretation;

import java.util.List;
import java.util.Locale;


/**
 * Base of the calendars whose dates are written as the Gregorian ones (day, month name or number or season, year) and
 * are read by {@link GregorianCalendarParser}: it provides the metadata and the readings of the ambiguous expressions
 * once, and the subclasses provide only the conversion.
 * <p>
 * The expression is parsed by {@link #parseInput(String)}, which a calendar overrides to accept more (e.g. the Swedish
 * 30 February) or to remove its own qualifiers (see {@link #readableText(String)}).
 */
public abstract class GregorianStyleCalendarStrategy implements CalendarConverterPlugin{

	/**
	 * Parses the date expression.
	 *
	 * @param rawDate the date expression
	 * @return the parsed date
	 * @throws IllegalArgumentException if the expression is not a valid date
	 */
	protected GregorianDateInput parseInput(final String rawDate){
		return GregorianCalendarParser.parse(rawDate);
	}

	/**
	 * @param rawDate the date expression
	 * @return the expression without the parts that {@link GregorianCalendarParser} does not know (e.g. "avant Pâques")
	 */
	protected String readableText(final String rawDate){
		return rawDate;
	}

	@Override
	public List<DateInterpretation> getInterpretations(final String rawDate, final Locale locale){
		return GregorianCalendarParser.interpretations(rawDate, readableText(rawDate), locale);
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final GregorianDateInput input = parseInput(rawDate);
		// the parser always requires the year, which can be the astronomical year 0 (1 BC)
		return CalendarMetadata.createFromYearMonthDay(true, input.month(), input.day());
	}

}
