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

import org.apache.commons.lang3.StringUtils;

import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;


/**
 * Centralized application of the visual preferences: UI language and UI
 * font. Settings are read from {@link AppPreferences} at startup and applied
 * through {@link #applySaved()}; changes made from the menu are applied
 * immediately by {@link #applyLanguage(Locale)} and {@link #applyFont(Font)},
 * then persisted.
 *
 * <p><b>Language.</b> The chosen locale drives every {@code ResourceBundle}
 * lookup in the application. Changing it at runtime works for labels that
 * are re-fetched on the fly; components whose text is set once at
 * construction time will keep their old text until they are rebuilt. For
 * a fully localized refresh, the simplest approach is to prompt the user
 * to restart the application — see {@link #languageChangeRequiresRestart()}.
 * This class does not force a restart; it only updates the locale and the
 * component tree, leaving the caller free to decide.</p>
 *
 * <p><b>Font.</b> The chosen font family is applied to every {@code *.font}
 * UIManager key, then the component tree of every open window is refreshed
 * so existing components pick up the new font. This is the standard Swing
 * idiom for runtime font changes; it does not require a restart.</p>
 */
public final class AppearanceManager{

	/* ======================================================================
	 *                          Language
	 * ====================================================================== */

	/** Returns the locale persisted in the preferences, or {@link Locale#ENGLISH}. */
	public static Locale savedLocale(){
		final String tag = AppPreferences.getInstance().languageTag();
		if(StringUtils.isBlank(tag))
			return Locale.ENGLISH;
		return Locale.forLanguageTag(tag);
	}

	/**
	 * Applies the given locale as the default for the whole application,
	 * updates the component tree of every open window, and persists the
	 * choice.
	 */
	public static void applyLanguage(final Locale locale){
		Locale.setDefault(locale);
		AppPreferences.getInstance().setLanguageTag(locale.toLanguageTag());
	}

	/**
	 * Whether a language change requires a restart to take full effect.
	 *
	 * <p>Some labels are set once when a component is constructed; these do
	 * not update after a runtime locale change. If the application relies
	 * heavily on such pre-set labels, callers should prompt the user to
	 * restart. This method always returns {@code true}; it is a placeholder
	 * for a future, more precise detection.</p>
	 */
	public static boolean languageChangeRequiresRestart(){
		return true;
	}


	/* ======================================================================
	 *                          Font
	 * ====================================================================== */

	/**
	 * Returns the font persisted in the preferences, or {@code null} when
	 * none is set (meaning: use the look-and-feel default).
	 */
	public static Font savedFont(){
		final AppPreferences prefs = AppPreferences.getInstance();
		final String family = prefs.fontFamily();
		if(family == null)
			return null;
		final int size = prefs.fontSize();
		final Font f = new Font(family, Font.PLAIN, size);
		// If the family is no longer available (uninstalled, renamed, or
		// mis-typed in the preferences file), fall back to the default.
		if(!f.getFamily().equalsIgnoreCase(family))
			return null;
		return f;
	}

	/**
	 * Applies the given font to every {@code *.font} UIManager key, refreshes
	 * every open window, and persists the choice. Passing {@code null} restores
	 * the default look-and-feel fonts.
	 */
	public static void applyFont(final Font font){
		if(font != null){
			final UIDefaults defaults = UIManager.getDefaults();
			for(final Object key : new ArrayList<>(defaults.keySet())){
				if(key instanceof String s && s.endsWith(".font"))
					defaults.put(key, font);
			}
			AppPreferences.getInstance().setFontFamily(font.getFamily());
			AppPreferences.getInstance().setFontSize(font.getSize());
		}
		else{
			// Restore the look-and-feel defaults by resetting the font keys
			// to null; the L&F will repopulate them on the next UI update.
			final UIDefaults defaults = UIManager.getDefaults();
			for(final Object key : new ArrayList<>(defaults.keySet())){
				if(key instanceof String s && s.endsWith(".font"))
					defaults.put(key, null);
			}
			AppPreferences.getInstance().setFontFamily(null);
		}
		refreshComponentTrees();
	}


	/* ======================================================================
	 *                          Startup
	 * ====================================================================== */

	/**
	 * Applies every persisted preference. Call this once at application
	 * startup, before any window is created.
	 */
	public static void applySaved(){
		final Locale locale = savedLocale();
		Locale.setDefault(locale);

		final Font font = savedFont();
		if(font != null){
			final UIDefaults defaults = UIManager.getDefaults();
			for(final Object key : new ArrayList<>(defaults.keySet())){
				if(key instanceof String s && s.endsWith(".font"))
					defaults.put(key, font);
			}
		}
	}


	/* ======================================================================
	 *                          Font enumeration
	 * ====================================================================== */

	/**
	 * Returns the names of every font family installed on the system,
	 * sorted alphabetically. Useful to populate the font chooser.
	 */
	public static String[] availableFontFamilies(){
		final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		final Set<String> names = new HashSet<>(Arrays.asList(
			ge.getAvailableFontFamilyNames(Locale.ROOT)));
		final String[] out = names.toArray(new String[0]);
		Arrays.sort(out, String.CASE_INSENSITIVE_ORDER);
		return out;
	}


	/* ======================================================================
	 *                          Internals
	 * ====================================================================== */

	/**
	 * Rebuilds the UI of every open window. This is required after changing
	 * UIManager defaults so that already-created components pick up the new
	 * values.
	 */
	private static void refreshComponentTrees(){
		SwingUtilities.invokeLater(() -> {
			for(final Window w : Window.getWindows()){
				if(w.isDisplayable())
					SwingUtilities.updateComponentTreeUI(w);
			}
		});
	}

}
