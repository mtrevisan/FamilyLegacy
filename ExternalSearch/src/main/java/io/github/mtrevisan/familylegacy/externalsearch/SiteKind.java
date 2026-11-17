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

package io.github.mtrevisan.familylegacy.externalsearch;

import io.github.mtrevisan.familylegacy.i18n.I18N;

import java.util.Locale;


/**
 * What a site holds. It says how far what is found there is evidence: a tree compiled by other researchers is only a lead, an index
 * is derived from a register, an image of the register is the original, a catalogue tells where a source is.
 */
public enum SiteKind{

	/** Indexes and images of registers (civil, parish, census, military, emigration). */
	RECORDS,
	/** Family trees compiled by other researchers, collaborative or not: leads, not evidence. */
	FAMILY_TREES,
	/** Catalogues and inventories of archives: where a source is, rarely what it says. */
	ARCHIVES,
	/** Digitized newspapers. */
	NEWSPAPERS,
	/** Books and other digitized publications. */
	LIBRARIES,
	/** Graves and burial records. */
	CEMETERIES,
	/** Anything else. */
	OTHER;


	/**
	 * @param name	The name (case does not matter).
	 * @return	The kind, {@link #OTHER} if unknown.
	 */
	public static SiteKind of(final String name){
		if(name != null)
			for(final SiteKind kind : values())
				if(kind.name().equalsIgnoreCase(name.strip()))
					return kind;
		return OTHER;
	}

	/** @return	The (localized) name. */
	public String label(){
		return I18N.t("external_search.kind." + name().toLowerCase(Locale.ROOT));
	}

}
