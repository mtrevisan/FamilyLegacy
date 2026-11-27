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
package io.github.mtrevisan.familylegacy.ui.shell;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.tools.files.FileMenuController;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.Color;


/**
 * Where the main window tells whether what is in memory is on the file: <em>Saved</em>, <em>Saving...</em> or <em>Not saved</em>. As
 * every change is written by itself, the user has no other way to know it.
 */
final class SaveStateView extends JLabel{

	SaveStateView(){
		setBorder(new EmptyBorder(0, 10, 0, 10));
		show(FileMenuController.SaveState.NONE);
	}


	/** Shows the state (nothing for a document that has no file and no changes). */
	void show(final FileMenuController.SaveState state){
		switch(state){
			case SAVED -> show("status.save.saved", UIManager.getColor("Label.disabledForeground"));
			case SAVING -> show("status.save.saving", UIManager.getColor("Label.disabledForeground"));
			case NOT_SAVED -> show("status.save.not_saved", new Color(0xB00020));
			default -> {
				setText(StringUtils.EMPTY);
				setToolTipText(null);
				setVisible(false);
			}
		}
		revalidate();
	}

	private void show(final String key, final Color color){
		setForeground(color);
		setText(I18N.t(key));
		setToolTipText(I18N.t(key + ".tooltip"));
		setVisible(true);
	}

}
