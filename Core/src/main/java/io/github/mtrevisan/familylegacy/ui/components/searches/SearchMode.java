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
package io.github.mtrevisan.familylegacy.ui.components.searches;


/**
 * Search modifiers.
 * <p>
 * The modifiers are orthogonal: a query can be fuzzy, whole-word, both,
 * or neither. Enumerating every combination would require 2ⁿ constants
 * and would not scale when a new modifier is added, so the combinations
 * are represented as a bitmask packed into a single {@code int}: each
 * enum constant carries its own bit, and the caller combines them with
 * the bitwise OR.
 * <p>
 * Usage:
 * <pre>
 *   int mode = SearchMode.FUZZY.bit() | SearchMode.WHOLE_WORD.bit();
 *   if(SearchMode.isSet(mode, SearchMode.FUZZY)){ ... }
 * </pre>
 */
public enum SearchMode{

	/**
	 * Whole-word matching: a query token matches only when it equals a
	 * complete target token, not when it appears inside one.
	 */
	WHOLE_WORD,

	/**
	 * Typo-tolerant matching: a query token matches a target token when
	 * they are within a small edit distance, in addition to the exact
	 * and substring cases.
	 */
	FUZZY

}
