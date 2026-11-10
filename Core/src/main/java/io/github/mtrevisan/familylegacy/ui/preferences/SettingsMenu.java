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
package io.github.mtrevisan.familylegacy.ui.preferences;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.preferences.AppPreferences;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import java.awt.Desktop;
import java.awt.Window;
import java.nio.file.Path;


/**
 * Builds the "Settings" menu: the preferences of the user that are not about how the application looks (those are in the Appearance menu).
 */
public final class SettingsMenu{

	private SettingsMenu(){
	}


	/**
	 * Creates a "Settings" menu ready to be added to a menu bar.
	 *
	 * @param owner the window that will own any dialog shown by the menu; may be {@code null}
	 * @return the menu, never {@code null}
	 */
	public static JMenu create(final Window owner){
		final JMenu menu = new JMenu(I18N.t("menu.settings"));
		menu.add(createIdentityItem(owner));
		menu.add(new JSeparator());
		menu.add(createOpenPreferencesItem(owner));
		return menu;
	}


	private static JMenuItem createIdentityItem(final Window owner){
		final JMenuItem item = new JMenuItem(I18N.t("menu.settings.identity"));
		item.addActionListener(a -> IdentitySettingsDialog.show(owner));
		return item;
	}

	private static JMenuItem createOpenPreferencesItem(final Window owner){
		final JMenuItem item = new JMenuItem(I18N.t("menu.item.open_preferences_folder"));
		item.addActionListener(a -> onOpenPreferences(owner));
		return item;
	}

	private static void onOpenPreferences(final Window owner){
		try{
			final Path folder = AppPreferences.filePath().getParent();
			if(!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)){
				//the folder cannot be opened from here: at least the user is told where it is
				JOptionPane.showMessageDialog(owner, I18N.tf("message.preferences_folder.unsupported", folder),
					I18N.t("error.title"), JOptionPane.INFORMATION_MESSAGE);

				return;
			}

			Desktop.getDesktop().open(folder.toFile());
		}
		catch(final Exception ex){
			JOptionPane.showMessageDialog(owner,
				I18N.t("message.preferences_folder.open_failed") + ":\n" + ex.getMessage(),
				I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);
		}
	}

}
