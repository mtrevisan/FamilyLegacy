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
package io.github.mtrevisan.familylegacy.assistant.place;

import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.assistant.identity.IdentityDraft;

import java.util.List;


/**
 * A finding of the Place Assistant, with what can be done about it.
 *
 * @param diagnostic the finding
 * @param draft      the start of a relationship between places to propose, or {@code null}
 * @param compare    the two places to compare side by side, or an empty list
 * @param identity   for two possible duplicates, the start of the identity hypothesis that would record the possibility, or {@code null}
 */
public record PlaceFinding(Diagnostic diagnostic, PlaceDraft draft, List<String> compare, IdentityDraft identity){

	/** A finding with no identity draft. */
	public PlaceFinding(final Diagnostic diagnostic, final PlaceDraft draft, final List<String> compare){
		this(diagnostic, draft, compare, null);
	}

}
