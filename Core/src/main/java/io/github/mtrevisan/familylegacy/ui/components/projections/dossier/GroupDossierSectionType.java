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
package io.github.mtrevisan.familylegacy.ui.components.projections.dossier;

import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import java.util.Locale;


/**
 * The sections that compose a group dossier.
 * <p>
 * Mirrors {@code DossierSectionType} for individuals, but with sections
 * that are meaningful for a group entity: members, subgroups, attributes,
 * events, sources, context, research.
 */
public enum GroupDossierSectionType{

	/** Names, type, image. */
	IDENTITY,

	/** Individuals that are members of the group through group_member. */
	MEMBERS,

	/** Groups linked through part_of (both subgroups and supergroups). */
	SUBGROUPS,

	/** {@code GroupAttributeRecord} entries owned by the group. */
	ATTRIBUTES,

	/** Events in which the group participated. */
	EVENTS,

	/** Source citations attached to the group and its records. */
	SOURCES,

	/** Contextual factors affecting the group. */
	CONTEXT,

	/** Research questions and conclusions about the group. */
	RESEARCH,

	/** Free-text notes attached to the group. */
	NOTES;


	/**
	 * Returns the label used as a section header in the panel.
	 *
	 * @return the display label, never {@code null}
	 */
	public String getDisplayLabel(){
		return I18N.t("enum.group_dossier.section." + name().toLowerCase(Locale.ROOT));
	}

}
