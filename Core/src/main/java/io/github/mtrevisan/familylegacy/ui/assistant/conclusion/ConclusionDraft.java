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
package io.github.mtrevisan.familylegacy.ui.assistant.conclusion;

import java.util.List;


/**
 * What the Conclusion Assistant prepares for a conflict: the start of a ConclusionRecord, for the researcher to complete.
 * <p>
 * It deliberately judges nothing: it has no preferred assertion and no proof status, so a draft saved as it is does not settle the
 * conflict (see {@link ConclusionChecker#isResolved}). The candidate preferred assertion, if any, is only named in the narrative.
 * </p>
 *
 * @param issue     the issue to resolve (e.g. {@code birth_date}, {@code parentage}, {@code national_id})
 * @param resolves  the identifiers of the competing records
 * @param narrative the description of the conflict, as the first text of the reasoning
 */
record ConclusionDraft(String issue, List<String> resolves, String narrative){}
