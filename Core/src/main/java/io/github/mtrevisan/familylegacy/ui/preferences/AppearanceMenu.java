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
import io.github.mtrevisan.familylegacy.preferences.AvailableLanguages;
import io.github.mtrevisan.familylegacy.preferences.MediaPathDisplay;
import org.apache.commons.lang3.Strings;

import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.Desktop;
import java.awt.Window;
import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;


/**
 * Builds the menu bar entry that lets the user choose the application's UI
 * language and font. Both choices are persisted across runs via
 * {@link AppPreferences}.
 *
 * <p>The menu contains two submenus:</p>
 * <ul>
 *   <li><b>Language</b> — a radio group of the languages offered by the
 *       application; picking one applies it immediately (with a prompt to
 *       restart, since some labels are set at construction time) and
 *       persists the choice;</li>
 *   <li><b>Font</b> — a list of every font installed on the system, plus a
 *       "Default" entry to restore the look-and-feel font. Each entry is
 *       rendered in its own font so the user can preview it.</li>
 * </ul>
 */
public final class AppearanceMenu{

	/** The languages offered in the Language submenu: the ones for which there are internationalization files. */
	private static final Map<String, Locale> LANGUAGES = AvailableLanguages.find();


	private AppearanceMenu(){
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Creates an "Appearance" menu ready to be added to a {@link JMenuBar}.
	 *
	 * @param owner the window that will own any dialog shown by the menu
	 *              (e.g. the restart prompt); may be {@code null}
	 * @param saveBeforeRestart writes what the document has still to write, before the application is closed to be started again: {@code false}
	 *                          if it could not be done and the user gave up
	 * @param openFile gives the file of the document, that the application opens again when it starts; {@code null} if it has none
	 * @return the menu, never {@code null}
	 */
	public static JMenu create(final Window owner, final BooleanSupplier saveBeforeRestart, final Supplier<File> openFile){
		final JMenu menu = new JMenu(I18N.t("menu.appearance"));
		menu.add(createLanguageMenu(owner, saveBeforeRestart, openFile));
		menu.add(createFontMenu(owner));
		menu.add(createMediaPathsMenu(owner));
		menu.add(createIdentityItem(owner));
		menu.add(new JSeparator());
		menu.add(createOpenPreferencesItem(owner));
		return menu;
	}


	/* ======================================================================
	 *                          Language submenu
	 * ====================================================================== */

	private static JMenu createLanguageMenu(final Window owner, final BooleanSupplier saveBeforeRestart, final Supplier<File> openFile){
		final JMenu language = new JMenu(I18N.t("menu.appearance.language"));
		final Locale current = AppearanceManager.savedLocale();
		final ButtonGroup group = new ButtonGroup();
		final Map<Locale, JRadioButtonMenuItem> items = new LinkedHashMap<>();

		for(final Map.Entry<String, Locale> e : LANGUAGES.entrySet()){
			final Locale locale = e.getValue();
			final JRadioButtonMenuItem item = new JRadioButtonMenuItem(e.getKey(), sameLanguage(locale, current));
			item.addActionListener(a -> onLanguageSelected(owner, locale, saveBeforeRestart, openFile));
			group.add(item);
			language.add(item);
			items.put(locale, item);
		}

		//the choice is the one in use each time the submenu is opened (a change that was given up leaves the old one)
		language.addMenuListener(new MenuListener(){
			@Override
			public void menuSelected(final MenuEvent e){
				final Locale saved = AppearanceManager.savedLocale();
				items.forEach((locale, item) -> item.setSelected(sameLanguage(locale, saved)));
			}

			@Override
			public void menuDeselected(final MenuEvent e){}

			@Override
			public void menuCanceled(final MenuEvent e){}
		});
		return language;
	}

	private static void onLanguageSelected(final Window owner, final Locale locale, final BooleanSupplier saveBeforeRestart,
			final Supplier<File> openFile){
		final Locale current = AppearanceManager.savedLocale();
		if(sameLanguage(locale, current))
			return;

		//the application is closed to be started again: what the document has still to write is written before, and if that cannot be done
		//the language is left as it is
		if(!saveBeforeRestart.getAsBoolean())
			return;

		// Persist first, so the new process picks it up on startup.
		AppearanceManager.applyLanguage(locale);

		// The locale change only takes full effect after restarting, because
		// some labels are baked into components at construction time. The document that is open is opened again.
		ApplicationRestarter.restart(owner, I18N.tf("dialog.restart.language_changed", locale.getDisplayLanguage(locale)), openFile.get());
	}

	private static boolean sameLanguage(final Locale a, final Locale b){
		return (Strings.CI.equals(a.getLanguage(), b.getLanguage())
			&& Strings.CI.equals(a.getCountry(), b.getCountry()));
	}


	/* ======================================================================
	 *                          Font submenu
	 * ====================================================================== */

	/* ======================================================================
	 *                          Media paths submenu
	 * ====================================================================== */

	private static JMenu createMediaPathsMenu(final Window owner){
		final JMenu menu = new JMenu(I18N.t("menu.appearance.media_paths"));
		final MediaPathDisplay current = AppPreferences.getInstance().mediaPathDisplay();
		final ButtonGroup group = new ButtonGroup();

		addMediaPathItem(menu, group, owner, MediaPathDisplay.FULL, "menu.appearance.media_paths.full", current);
		addMediaPathItem(menu, group, owner, MediaPathDisplay.FILE_NAME, "menu.appearance.media_paths.file_name", current);
		addMediaPathItem(menu, group, owner, MediaPathDisplay.RELATIVE_TO_BASE, "menu.appearance.media_paths.relative", current);

		menu.add(new JSeparator());
		final JMenuItem baseDirectory = new JMenuItem(I18N.t("menu.appearance.media_paths.base_directory"));
		baseDirectory.addActionListener(a -> chooseMediaBaseDirectory(owner));
		menu.add(baseDirectory);
		final JMenuItem fileDirectory = new JMenuItem(I18N.t("menu.appearance.media_paths.file_directory"));
		fileDirectory.addActionListener(a -> AppPreferences.getInstance().setMediaBaseDirectory(null));
		menu.add(fileDirectory);

		//tell which directory is in use, every time the submenu is opened
		menu.addMenuListener(new MenuListener(){
			@Override
			public void menuSelected(final MenuEvent e){
				final String chosen = AppPreferences.getInstance().mediaBaseDirectory();
				final String inUse = MediaPathDisplay.baseDirectory();
				baseDirectory.setToolTipText(inUse != null
					? I18N.tf((chosen != null? "menu.appearance.media_paths.in_use.chosen": "menu.appearance.media_paths.in_use.file"),
					inUse)
					: I18N.t("menu.appearance.media_paths.in_use.none"));
				fileDirectory.setEnabled(chosen != null);
			}

			@Override
			public void menuDeselected(final MenuEvent e){}

			@Override
			public void menuCanceled(final MenuEvent e){}
		});
		return menu;
	}

	private static void addMediaPathItem(final JMenu menu, final ButtonGroup group, final Window owner,
			final MediaPathDisplay display, final String key, final MediaPathDisplay current){
		final JRadioButtonMenuItem item = new JRadioButtonMenuItem(I18N.t(key), display == current);
		item.addActionListener(a -> {
			AppPreferences.getInstance().setMediaPathDisplay(display);
			//the paths are relative to something: ask for it only if there is no open file to take it from
			if(display == MediaPathDisplay.RELATIVE_TO_BASE && MediaPathDisplay.baseDirectory() == null)
				chooseMediaBaseDirectory(owner);
		});
		group.add(item);
		menu.add(item);
	}

	private static void chooseMediaBaseDirectory(final Window owner){
		final JFileChooser chooser = new JFileChooser(MediaPathDisplay.baseDirectory());
		chooser.setDialogTitle(I18N.t("menu.appearance.media_paths.base_directory.title"));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if(chooser.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION)
			AppPreferences.getInstance().setMediaBaseDirectory(chooser.getSelectedFile().getAbsolutePath());
	}


	private static JMenuItem createIdentityItem(final Window owner){
		final JMenuItem item = new JMenuItem(I18N.t("menu.appearance.identity"));
		item.addActionListener(a -> IdentitySettingsDialog.show(owner));
		return item;
	}


	private static JMenuItem createFontMenu(final Window owner){
		final JMenuItem item = new JMenuItem(I18N.t("menu.appearance.font.dialog"));
		item.addActionListener(a -> FontChooserDialog.show(owner));
		return item;
	}


	/* ======================================================================
	 *                          Open preferences item
	 * ====================================================================== */

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
