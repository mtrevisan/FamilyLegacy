package io.github.mtrevisan.familylegacy.v2.ui.preferences;

import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18n;
import org.apache.commons.lang3.Strings;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import java.awt.Font;
import java.awt.Window;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;


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

	/** The languages offered in the Language submenu. */
	private static final Map<String, Locale> LANGUAGES = new LinkedHashMap<>();

	static{
		// The languages exposed in the UI. Add more here as translations
		// become available. Order is the order in the menu.
		LANGUAGES.put("English", Locale.ENGLISH);
		LANGUAGES.put("Italiano", Locale.ITALIAN);
		LANGUAGES.put("Deutsch", Locale.GERMAN);
		LANGUAGES.put("Français", Locale.FRENCH);
		LANGUAGES.put("Español", Locale.of("es"));
		LANGUAGES.put("Português", Locale.of("pt"));
		LANGUAGES.put("Nederlands", Locale.of("nl"));
		LANGUAGES.put("Polski", Locale.of("pl"));
		LANGUAGES.put("Čeština", Locale.of("cs"));
		LANGUAGES.put("Magyar", Locale.of("hu"));
		LANGUAGES.put("Română", Locale.of("ro"));
		LANGUAGES.put("Русский", Locale.of("ru"));
		LANGUAGES.put("Українська", Locale.of("uk"));
		LANGUAGES.put("Ελληνικά", Locale.of("el"));
		LANGUAGES.put("العربية", Locale.of("ar"));
		LANGUAGES.put("עברית", Locale.of("he"));
		LANGUAGES.put("Türkçe", Locale.of("tr"));
		LANGUAGES.put("فارسی", Locale.of("fa"));
		LANGUAGES.put("हिन्दी", Locale.of("hi"));
		LANGUAGES.put("বাংলা", Locale.of("bn"));
		LANGUAGES.put("اردو", Locale.of("ur"));
		LANGUAGES.put("தமிழ்", Locale.of("ta"));
		LANGUAGES.put("తెలుగు", Locale.of("te"));
		LANGUAGES.put("मराठी", Locale.of("mr"));
		LANGUAGES.put("ગુજરાતી", Locale.of("gu"));
		LANGUAGES.put("ਪੰਜਾਬੀ", Locale.of("pa"));
		LANGUAGES.put("Bahasa Indonesia", Locale.of("id"));
		LANGUAGES.put("Tiếng Việt", Locale.of("vi"));
		LANGUAGES.put("ไทย", Locale.of("th"));
		LANGUAGES.put("Tagalog", Locale.of("tl"));
		LANGUAGES.put("简体中文", Locale.of("zh", "CN"));
		LANGUAGES.put("繁體中文", Locale.of("zh", "TW"));
		LANGUAGES.put("日本語", Locale.of("ja"));
		LANGUAGES.put("한국어", Locale.of("ko"));
		LANGUAGES.put("Kiswahili", Locale.of("sw"));
		LANGUAGES.put("Hausa", Locale.of("ha"));
	}


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
	 * @return the menu, never {@code null}
	 */
	public static JMenu create(final Window owner){
		final JMenu menu = new JMenu(I18n.t("menu.appearance"));
		menu.add(createLanguageMenu(owner));
		menu.add(createFontMenu(owner));
		menu.add(new JSeparator());
		menu.add(createOpenPreferencesItem(owner));
		return menu;
	}


	/* ======================================================================
	 *                          Language submenu
	 * ====================================================================== */

	private static JMenu createLanguageMenu(final Window owner){
		final JMenu language = new JMenu(I18n.t("menu.appearance.language"));
		final Locale current = AppearanceManager.savedLocale();
		final ButtonGroup group = new ButtonGroup();

		for(final Map.Entry<String, Locale> e : LANGUAGES.entrySet()){
			final Locale locale = e.getValue();
			final JRadioButtonMenuItem item =
				new JRadioButtonMenuItem(e.getKey(), sameLanguage(locale, current));
			item.addActionListener(a -> onLanguageSelected(owner, locale));
			group.add(item);
			language.add(item);
		}
		return language;
	}

	private static void onLanguageSelected(final Window owner, final Locale locale){
		final Locale current = AppearanceManager.savedLocale();
		if(sameLanguage(locale, current))
			return;

		// Persist first, so the new process picks it up on startup.
		AppearanceManager.applyLanguage(locale);

		// The locale change only takes full effect after restarting, because
		// some labels are baked into components at construction time.
		ApplicationRestarter.restart(owner,
			"The interface language has been changed to "
				+ locale.getDisplayLanguage(locale) + ".");
	}

	private static boolean sameLanguage(final Locale a, final Locale b){
		return (Strings.CI.equals(a.getLanguage(), b.getLanguage())
			&& Strings.CI.equals(a.getCountry(), b.getCountry()));
	}


	/* ======================================================================
	 *                          Font submenu
	 * ====================================================================== */

	private static JMenu createFontMenu(final Window owner){
		final JMenu fontMenu = new JMenu(I18n.t("menu.appearance.font"));
		final ButtonGroup group = new ButtonGroup();

		// "Default" restores the L&F font.
		final Font saved = AppearanceManager.savedFont();
		final JRadioButtonMenuItem defaultItem =
			new JRadioButtonMenuItem("Default", saved == null);
		defaultItem.addActionListener(a -> AppearanceManager.applyFont(null));
		group.add(defaultItem);
		fontMenu.add(defaultItem);

		fontMenu.add(new JSeparator());

		final String savedFamily = (saved != null? saved.getFamily(): null);
		final String[] families = AppearanceManager.availableFontFamilies();

		for(final String family : families){
			final Font preview = new Font(family, Font.PLAIN, 12);
			final JRadioButtonMenuItem item =
				new JRadioButtonMenuItem(family, family.equalsIgnoreCase(savedFamily));
			// Preview the font by setting it on the menu item itself.
			item.setFont(preview);
			item.addActionListener(a -> AppearanceManager.applyFont(preview));
			group.add(item);
			fontMenu.add(item);
		}
		return fontMenu;
	}


	/* ======================================================================
	 *                          Open preferences item
	 * ====================================================================== */

	private static JMenuItem createOpenPreferencesItem(final Window owner){
		final JMenuItem item = new JMenuItem("Open preferences file…");
		item.addActionListener(a -> onOpenPreferences(owner));
		return item;
	}

	private static void onOpenPreferences(final Window owner){
		try{
			final java.nio.file.Path path = AppPreferences.filePath();
			if(java.awt.Desktop.isDesktopSupported())
				java.awt.Desktop.getDesktop().open(path.getParent().toFile());
		}
		catch(final Exception ex){
			JOptionPane.showMessageDialog(owner,
				"Could not open the preferences folder:\n" + ex.getMessage(),
				"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

}
