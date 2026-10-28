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
package io.github.mtrevisan.calendars.assistant;

import java.util.List;


/**
 * A candidate reading of a date, proposed to the researcher together with the reasoning that produced it.
 * <p>
 * A suggestion is never applied on its own: the user decides whether to use it.
 * </p>
 *
 * @param title          a short label of the candidate (e.g. "Julian calendar")
 * @param meaning        the interpreted meaning of the input
 * @param assumptions    the assumptions the result relies on
 * @param calendarCode   the code of the calendar of {@code normalizedDate} (see {@code CalendarType#getCode()})
 * @param steps          the conversion steps, in order
 * @param normalizedDate the resulting date, in the notation of the calendar, or {@code null} if the candidate is only
 *                       informative or a range
 * @param notBefore      the earliest possible date of a range, in the notation of the calendar, or {@code null}
 * @param notAfter       the latest possible date of a range, in the notation of the calendar, or {@code null}
 */
public record DateSuggestion(String title, String meaning, List<String> assumptions, String calendarCode, List<String> steps,
		String normalizedDate, String notBefore, String notAfter){

	public DateSuggestion{
		assumptions = List.copyOf(assumptions);
		steps = List.copyOf(steps);
	}

	/** A suggestion that is a single date (or only informative if {@code normalizedDate} is {@code null}). */
	public DateSuggestion(final String title, final String meaning, final List<String> assumptions, final String calendarCode,
			final List<String> steps, final String normalizedDate){
		this(title, meaning, assumptions, calendarCode, steps, normalizedDate, null, null);
	}


	/** @return whether the suggestion carries a range (a bounded date) */
	public boolean isBounded(){
		return (normalizedDate == null && (notBefore != null || notAfter != null));
	}

	/** @return whether the suggestion carries a date or a range that can be applied to a date structure */
	public boolean isApplicable(){
		return (normalizedDate != null || isBounded());
	}

	/** @return the explanation as plain text, suitable to be copied into notes */
	public String explanation(){
		final StringBuilder sb = new StringBuilder();
		sb.append(title).append('\n');
		sb.append("Interpreted meaning: ").append(meaning).append('\n');
		if(!assumptions.isEmpty()){
			sb.append("Assumptions:\n");
			assumptions.forEach(a -> sb.append("  - ").append(a).append('\n'));
		}
		if(calendarCode != null)
			sb.append("Calendar: ").append(calendarCode).append('\n');
		if(!steps.isEmpty()){
			sb.append("Conversion steps:\n");
			for(int i = 0; i < steps.size(); i ++)
				sb.append("  ").append(i + 1).append(". ").append(steps.get(i)).append('\n');
		}
		if(normalizedDate != null)
			sb.append("Normalized date: ").append(normalizedDate).append('\n');
		if(notBefore != null)
			sb.append("Not before: ").append(notBefore).append('\n');
		if(notAfter != null)
			sb.append("Not after: ").append(notAfter).append('\n');
		return sb.toString();
	}

}
