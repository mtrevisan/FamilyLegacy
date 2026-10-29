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
package io.github.mtrevisan.familylegacy.ui.preferences;


/**
 * How the children of a parent or couple are ordered by the views.
 * <p>
 * <b>This is a presentation choice, not genealogical data.</b> Genealogical records carry evidence and conclusions; the order in
 * which they are shown is a matter of the view, and different users may legitimately prefer different orders for the same file.
 * The choice is kept in the user preferences (see {@link OrderPreferences}), never in the records, and it is never exported.
 */
public enum ChildrenOrder{

	/** Oldest to youngest, by the preferred birth assertion; the children without a known birth come last. The default. */
	BIRTH_DATE,
	/** By the preferred display name; the children without a name come last. */
	ALPHABETICAL,
	/** The order returned by the model, without re-sorting. */
	EXISTING

}
