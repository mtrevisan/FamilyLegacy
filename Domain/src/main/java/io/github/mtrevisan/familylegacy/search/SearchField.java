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

package io.github.mtrevisan.familylegacy.search;


/**
 * The texts of a record that a query is matched against: where a term can be found.
 */
public enum SearchField{

	/** The text shown for the record (for an individual, the first name and the identifier). */
	DISPLAY_TEXT,

	/** The other names of a record that has several (the other names of a person, the titles of a source, ...). */
	OTHER_NAMES,

	/** The names of the places that contain a place. */
	CONTAINING_PLACES,

	/** All the names of the members of a group. */
	MEMBER_NAMES,

	/** The notes (the stories excepted) and the citations of the record (only with {@link SearchMode#NOTES_AND_CITATIONS}). */
	NOTES_AND_CITATIONS,

	/** The stories of the record (only with {@link SearchMode#STORIES}). */
	STORIES,

	/** The free tags of the record. */
	TAGS

}
