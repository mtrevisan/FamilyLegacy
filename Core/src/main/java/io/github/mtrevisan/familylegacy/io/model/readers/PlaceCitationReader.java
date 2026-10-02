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
package io.github.mtrevisan.familylegacy.io.model.readers;


/**
 * Handler for PLACE CITATION records.
 * <p>
 * Structure:
 * <pre>
 * // Reference to a PLACE record together with location-specific citations and evidence qualifiers.
 * // Implementations SHOULD prevent cyclic expansion of citation metadata (PlaceCitation > SourceCitation > SourceRecord > PlaceCitation).
 * struct PlaceCitation {
 *   place: Xref&lt;PlaceRecord&gt;
 *   original_text?: Text   // The place name exactly as recorded in the source. This value MAY coexist with PLACE when the source wording is preserved alongside a normalized place identification.
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 * }
 * </pre>
 */
public final class PlaceCitationReader{

	public static final String TAG_PLACE = "place";
	public static final String TAG_ORIGINAL_TEXT = "original_text";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_EVIDENCE = "evidence";


	private PlaceCitationReader(){}

}
