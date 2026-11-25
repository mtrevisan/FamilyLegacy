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
package io.github.mtrevisan.familylegacy.i18n;

import com.ibm.icu.text.MessageFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * Central accessor for the UI's localized strings.
 *
 * <p>Translations live in {@code i18n/ui/messages.properties} and its
 * locale-specific siblings (e.g. {@code messages_it.properties}). The
 * loader reads them as UTF-8, so any script can be stored literally
 * without {@code \\uXXXX} escapes.</p>
 *
 * <p>Plural- and gender-sensitive messages use ICU4J
 * {@link MessageFormat}, so languages with more than two plural forms
 * (Slavic, Arabic) and languages with grammatical gender (French, Russian)
 * are handled correctly. A message that does not depend on the count
 * should use {@link #t(String)}; one that does should use
 * {@link #plural(String, int, Object...)}.</p>
 *
 * <p><b>Runtime changes are not supported.</b> The bundle is loaded once
 * at class initialization with the default locale captured at that
 * moment. Changing the locale at runtime requires restarting the JVM,
 * which is exactly what the application does when the user picks a new
 * language.</p>
 */
public final class I18N{

	private static final String BUNDLE_BASE = "i18n.ui.messages";

	/** The bundle, resolved once against the current default locale. */
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_BASE, Locale.getDefault());


	/** The bundles of the other languages (the ones of the reports), by locale. */
	private static final ConcurrentMap<Locale, ResourceBundle> BUNDLES = new ConcurrentHashMap<>(4);


	private I18N(){}


	/* ======================================================================
	 *                          Simple lookups
	 * ====================================================================== */

	/** Returns the localized string for {@code key}. Throws when the key is missing. */
	public static String t(final String key){
		return BUNDLE.getString(key);
	}

	/** Returns the localized string for {@code key}, or {@code fallback} when missing. */
	public static String t(final String key, final String fallback){
		try{
			return BUNDLE.getString(key);
		}
		catch(final MissingResourceException ignored){
			return fallback;
		}
	}

	/**
	 * Returns the string of {@code key} in the given language, that may not be the one of the user interface (a report can be written
	 * in a language other than the interface's), or {@code fallback} when missing. A language without a bundle gets the root
	 * (English) one, not the one of the default locale of the JVM.
	 */
	public static String t(final Locale locale, final String key, final String fallback){
		final ResourceBundle bundle = BUNDLES.computeIfAbsent(locale, I18N::loadBundle);
		try{
			return bundle.getString(key);
		}
		catch(final MissingResourceException ignored){
			return fallback;
		}
	}

	private static ResourceBundle loadBundle(final Locale locale){
		final ResourceBundle candidate = ResourceBundle.getBundle(BUNDLE_BASE, locale);
		return (candidate.getLocale().getLanguage().equals(locale.getLanguage())
			? candidate
			: ResourceBundle.getBundle(BUNDLE_BASE, Locale.ROOT));
	}

	/** Returns the localized string for {@code key} with {@link String#format} substitution. */
	public static String tf(final String key, final Object... args){
		try{
			return new MessageFormat(BUNDLE.getString(key), Locale.getDefault())
				.format(args);
		}
		catch(final MissingResourceException ignored){
			return key;
		}
	}

	/**
	 * Returns every distinct printable character used by the messages of the current language (in code point
	 * order) that belongs to the main script of the bundle (the script most letters are written in, e.g. Latin) or to
	 * no script in particular (digits, punctuation, symbols, combining marks), i.e. the characters a font must be
	 * able to draw to render the user interface.
	 * <p>Letters of other scripts are left out on purpose: they appear as occasional native-script labels (e.g. a
	 * Japanese or Arabic name next to its transliteration) that no single font covers together with the main script,
	 * and the Java runtime draws them with a fallback font anyway. Requiring them would exclude every real font.</p>
	 */
	public static String allCharacters(){
		final List<String> values = values();
		final TreeSet<Integer> codePoints = new TreeSet<>();
		for(final String value : values)
			value.codePoints()
				.filter(cp -> !Character.isISOControl(cp) && !Character.isWhitespace(cp))
				.forEach(codePoints::add);
		return mainScriptCharacters(codePoints, countLetters(values));
	}

	private static List<String> values(){
		final List<String> values = new ArrayList<>();
		final Enumeration<String> keys = BUNDLE.getKeys();
		while(keys.hasMoreElements())
			values.add(BUNDLE.getString(keys.nextElement()));
		return values;
	}

	private static Map<Character.UnicodeScript, Integer> countLetters(final List<String> values){
		final Map<Character.UnicodeScript, Integer> letters = new EnumMap<>(Character.UnicodeScript.class);
		for(final String value : values)
			value.codePoints()
				.filter(Character::isLetter)
				.forEach(cp -> letters.merge(Character.UnicodeScript.of(cp), 1, Integer::sum));
		return letters;
	}

	/**
	 * Keeps the characters of the most frequent script among the letters, plus the script-neutral ones.
	 *
	 * @param codePoints	The distinct characters.
	 * @param letters	The number of letters per script.
	 */
	static String mainScriptCharacters(final Collection<Integer> codePoints,
			final Map<Character.UnicodeScript, Integer> letters){
		final Character.UnicodeScript main = letters.entrySet().stream()
			.max(Map.Entry.comparingByValue())
			.map(Map.Entry::getKey)
			.orElse(Character.UnicodeScript.LATIN);
		final StringBuilder sb = new StringBuilder();
		for(final int cp : codePoints){
			final Character.UnicodeScript script = Character.UnicodeScript.of(cp);
			if(script == main || script == Character.UnicodeScript.COMMON || script == Character.UnicodeScript.INHERITED)
				sb.appendCodePoint(cp);
		}
		return sb.toString();
	}

	/**
	 * Returns the localized text of {@code key} formatted as the label of a field. The separator is part of the
	 * translation ({@code format.label}), because it depends on the language (e.g., {@code "Nom :"} in French).
	 */
	public static String l(final String key){
		return al(t(key));
	}

	/** Formats an already localized text as the label of a field (see {@link #l(String)}). */
	public static String al(final String text){
		return tf("format.label", text);
	}


	/* ======================================================================
	 *                          ICU formatting
	 * ====================================================================== */

	/**
	 * Formats a plural-sensitive message using ICU4J. The pattern must use
	 * the CLDR plural syntax, e.g.
	 * {@code "{0,plural,one{# file}other{# files}}"}. The count is bound to
	 * argument {@code 0}; any additional arguments follow.
	 */
	public static String plural(final String key, final int count, final Object... extra){
		final String pattern = BUNDLE.getString(key);
		final Object[] args = new Object[extra.length + 1];
		args[0] = count;
		System.arraycopy(extra, 0, args, 1, extra.length);
		return new MessageFormat(pattern, Locale.getDefault())
			.format(args);
	}

	/** Same as {@link #plural(String, int, Object...)} but takes an explicit pattern. */
	public static String formatIcu(final String pattern, final Object... args){
		return new MessageFormat(pattern, Locale.getDefault())
			.format(args);
	}

}
