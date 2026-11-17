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
package io.github.mtrevisan.familylegacy.social;

import java.util.List;


/**
 * A hint of where to search next, drawn from a FAN table. It is only a suggestion: nothing is ever changed because of it.
 *
 * @param kind      what the hint is
 * @param personIds the people it is about: for {@link Kind#POSSIBLE_COUPLE} the two that might be a couple, for
 *                  {@link Kind#RECURRING_WITHOUT_KINSHIP} the one that recurs
 * @param eventIds  the events it is drawn from
 */
public record FanSuggestion(Kind kind, List<String> personIds, List<String> eventIds){

	public FanSuggestion{
		personIds = List.copyOf(personIds);
		eventIds = List.copyOf(eventIds);
	}


	/** What a suggestion is. */
	public enum Kind{

		/** Two people with the same role in the same event of the subject (for example the two godparents of a child), not known to be related: they might be a couple. */
		POSSIBLE_COUPLE,

		/** A person who recurs in the events of the subject, with no known kinship to him: a candidate relative or fellow villager. */
		RECURRING_WITHOUT_KINSHIP

	}

}
