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
package io.github.mtrevisan.familylegacy.preferences;


/**
 * How much of FLEF the user interface shows by default (progressive disclosure).
 * <p>
 * <b>This is a presentation choice, not genealogical data.</b> The model is always complete and nothing is removed or disabled: the
 * level only decides what the menus and the views offer at first. Whatever is hidden stays reachable (the Research Assistant lists it).
 * The choice is kept in the user preferences, never in the records, and it is never exported.
 * </p>
 * The constants are ordered from the least to the most inclusive: a level shows everything the previous ones show.
 */
public enum UxLevel{

	/** People, groups, events, relationships, places, sources and media: a traditional genealogy application. The default. */
	BASIC,
	/** Also the research workflow: questions, activities, tasks and identity hypotheses. */
	STANDARD,
	/** Everything: also conclusions, cultural norms, context impacts and the evidence analysis. */
	ADVANCED;


	/**
	 * @param required	The lowest level a feature is shown at.
	 * @return	Whether a feature that needs {@code required} is shown at this level.
	 */
	public boolean includes(final UxLevel required){
		return (compareTo(required) >= 0);
	}

}
