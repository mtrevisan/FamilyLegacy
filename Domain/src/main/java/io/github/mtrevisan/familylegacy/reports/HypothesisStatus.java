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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;


/**
 * Where an identity hypothesis stands. The hypothesis itself has no status: it is told by the most recent conclusion that evaluates it
 * and gives a proof status.
 */
public enum HypothesisStatus{

	/** No conclusion evaluates it, or the evaluation leaves it open (conflicting evidence, no proof status). */
	OPEN,
	/** The most recent evaluation supports or proves it. */
	ACCEPTED,
	/** The most recent evaluation disproves it. */
	REJECTED;


	/** @param proofStatus	The proof status of the deciding conclusion ({@code null} if there is none). */
	public static HypothesisStatus of(final String proofStatus){
		if(ConclusionReader.ENUM_PROOF_STATUS_DISPROVEN.equals(proofStatus))
			return REJECTED;
		if(ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED.equals(proofStatus) || ConclusionReader.ENUM_PROOF_STATUS_PROVEN.equals(proofStatus))
			return ACCEPTED;
		return OPEN;
	}

}
