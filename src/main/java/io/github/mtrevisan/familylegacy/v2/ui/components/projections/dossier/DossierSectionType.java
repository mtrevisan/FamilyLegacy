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


/**
 * The sections that compose an individual dossier.
 * <p>
 * Each section corresponds to a family of FLEF records that are linked
 * to the focus individual either directly or indirectly.
 * <p>
 * The order of the constants is the order in which the sections are
 * rendered in the panel.
 */
public enum DossierSectionType{

	/** Names, sex, vital dates, photo, privacy. */
	IDENTITY("Identity"),

	/** All events in which the individual participated. */
	EVENTS("Events"),

	/** Enduring attributes owned by the individual. */
	ATTRIBUTES("Attributes"),

	/** All relationships involving the individual. */
	RELATIONSHIPS("Relationships"),

	/** Source citations attached to the individual and its records. */
	SOURCES("Sources"),

	/** Contextual factors (historic events, cultural norms) affecting the individual. */
	CONTEXT("Context"),

	/** Identity hypotheses that include the individual as a candidate. */
	IDENTITY_HYPOTHESES("Identity hypotheses"),

	/** Research questions and conclusions about the individual. */
	RESEARCH("Research"),

	/** Free-text notes attached to the individual and its records. */
	NOTES("Notes");


	private final String displayLabel;


	DossierSectionType(final String displayLabel){
		this.displayLabel = displayLabel;
	}


	/**
	 * Returns the label used as a section header in the panel.
	 *
	 * @return the display label, never {@code null}
	 */
	public String getDisplayLabel(){
		return displayLabel;
	}

}
