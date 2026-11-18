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
 * The FAN table of an individual: the people who appear with him in the documents, the strongest first, and what to look for next.
 *
 * @param subjectId   the identifier of the individual the table is about
 * @param entries     the people, the strongest link first (the relatives of the subject after the others)
 * @param suggestions where to search next, never applied by themselves
 */
public record FanResult(String subjectId, List<FanEntry> entries, List<FanSuggestion> suggestions){

	public FanResult{
		entries = List.copyOf(entries);
		suggestions = List.copyOf(suggestions);
	}


	/** @return the table of nobody */
	public static FanResult empty(){
		return new FanResult(null, List.of(), List.of());
	}


	/** @return whether nobody appears with the subject */
	public boolean isEmpty(){
		return entries.isEmpty();
	}

}
