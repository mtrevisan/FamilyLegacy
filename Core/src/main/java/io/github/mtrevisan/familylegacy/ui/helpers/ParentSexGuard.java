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
package io.github.mtrevisan.familylegacy.ui.helpers;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.individual.IndividualLabel;
import io.github.mtrevisan.familylegacy.io.model.readers.CoParentSex;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.record.RecordLookup;

import javax.swing.JOptionPane;
import java.awt.Component;


/**
 * Asks the user about two biological parents of the same sex (see {@link CoParentSex}): a certain clash is an error, a possible one
 * (a plain {@code child} link, that does not say whether the parentage is biological) is a warning the user can override.
 */
public final class ParentSexGuard{

	private ParentSexGuard(){}


	/**
	 * @param owner	The component the messages are shown over.
	 * @param clash	What {@link CoParentSex} found, {@code null} if nothing.
	 * @param parentId	The parent that is going to be linked (the other one, {@link CoParentSex.Clash#coParentId()}, is already there).
	 * @param model	Where the sex is looked up.
	 * @return	Whether to go on: {@code true} if there is no clash, or the user accepted the warning.
	 */
	public static boolean confirm(final Component owner, final CoParentSex.Clash clash, final String parentId, final RecordLookup model){
		if(clash == null)
			return true;

		final String parent = IndividualLabel.of(model, parentId) + " ("
			+ I18N.t("enum.individual.sex." + PreferredSex.rawOf(model.getRecordById(parentId), model)) + ")";
		final String coParent = IndividualLabel.of(model, clash.coParentId());
		final String child = IndividualLabel.of(model, clash.childId());
		if(clash.certain()){
			MessageDialogs.show(owner, I18N.tf("validation.parents_clash", coParent, child, parent),
				I18N.t("validation.title"), JOptionPane.ERROR_MESSAGE);

			return false;
		}

		return (MessageDialogs.confirm(owner, I18N.tf("validation.parents_warning", coParent, child, parent),
			I18N.t("validation.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION);
	}

}
