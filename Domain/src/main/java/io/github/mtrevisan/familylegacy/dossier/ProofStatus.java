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
package io.github.mtrevisan.familylegacy.dossier;

import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;


/**
 * Proof status of an assertion, as declared by a
 * {@code ConclusionRecord}.
 * <p>
 * The status is attached to every record that a conclusion resolves,
 * so the dossier can show at a glance whether a specific assertion has
 * been formally evaluated by the researcher and with what outcome.
 * <p>
 * The color is used by the panel to draw a small dot on the left of
 * the entry row. The status is independent from the evidence badge
 * ({@code orig/prim/dir}), which describes the source rather than the
 * researcher's verdict.
 */
public enum ProofStatus{

	/** Proven according to the Genealogical Proof Standard. */
	PROVEN(ConclusionReader.ENUM_PROOF_STATUS_PROVEN),

	/** Preponderance of evidence, but not absolute certainty. */
	SUPPORTED(ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED),

	/** The evidence is conflicting, none prevails. */
	CONFLICTING(ConclusionReader.ENUM_PROOF_STATUS_CONFLICTING_EVIDENCE),

	/** Proven false. */
	DISPROVEN(ConclusionReader.ENUM_PROOF_STATUS_DISPROVEN);


	private final String code;


	ProofStatus(final String code){
		this.code = code;
	}


	public String code(){
		return code;
	}

	/**
	 * @param code	The proof status as written in the file (for example {@code conflicting_evidence}), case-insensitive.
	 * @return	The status, or {@code null} if the code is missing or unknown.
	 */
	public static ProofStatus fromCode(final String code){
		if(code == null)
			return null;

		for(final ProofStatus status : values())
			if(status.code.equalsIgnoreCase(code))
				return status;
		return null;
	}
}
