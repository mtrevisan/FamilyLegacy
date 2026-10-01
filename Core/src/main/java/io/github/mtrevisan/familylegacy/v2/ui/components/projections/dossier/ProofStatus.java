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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.dossier;

import java.awt.Color;
import java.util.Locale;


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
	PROVEN("proven", new Color(80, 160, 80)),

	/** Preponderance of evidence, but not absolute certainty. */
	SUPPORTED("supported", new Color(70, 120, 200)),

	/** The evidence is conflicting, none prevails. */
	CONFLICTING("conflicting evidence", new Color(200, 170, 60)),

	/** Proven false. */
	DISPROVEN("disproven", new Color(200, 80, 80));


	private final String displayName;
	private final Color color;


	ProofStatus(final String displayName, final Color color){
		this.displayName = displayName;
		this.color = color;
	}


	/** Human-readable label, used in tooltips. */
	public String displayName(){
		return displayName;
	}

	/** Color of the indicator dot. */
	public Color color(){
		return color;
	}

	/**
	 * Parses the FLEF enum value of {@code proof_status}. Returns
	 * {@code null} for an unknown or empty value.
	 *
	 * @param raw the raw value, may be {@code null}
	 * @return the matching status, or {@code null}
	 */
	public static ProofStatus fromString(final String raw){
		if(raw == null)
			return null;

		return switch(raw.toLowerCase(Locale.ROOT)){
			case "proven" -> PROVEN;
			case "supported" -> SUPPORTED;
			case "conflicting_evidence" -> CONFLICTING;
			case "disproven" -> DISPROVEN;
			default -> null;
		};
	}

}
