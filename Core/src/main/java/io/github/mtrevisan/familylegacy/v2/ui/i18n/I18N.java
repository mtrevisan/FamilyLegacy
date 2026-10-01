package io.github.mtrevisan.familylegacy.v2.ui.i18n;

import com.ibm.icu.text.MessageFormat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;


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

	/** Returns the localized string for {@code key} with {@link String#format} substitution. */
	public static String tf(final String key, final Object... args){
		return new MessageFormat(BUNDLE.getString(key), Locale.getDefault())
			.format(args);
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


	/* ======================================================================
	 *                          UTF-8 loader
	 * ====================================================================== */

	/**
	 * {@link ResourceBundle.Control} that loads {@code .properties} files
	 * as UTF-8 instead of ISO-8859-1. Needed because the standard loader
	 * mangles every non-ASCII character unless it is escaped as
	 * {@code \\uXXXX}.
	 */
	private static final class Utf8Control extends ResourceBundle.Control{
		@Override
		public ResourceBundle newBundle(final String baseName, final Locale locale, final String format,
				final ClassLoader loader, final boolean reload) throws IOException, IllegalAccessException,
				InstantiationException{
			if(!"java.properties".equals(format))
				return super.newBundle(baseName, locale, format, loader, reload);

			final String bundleName = toBundleName(baseName, locale);
			final String resourceName = toResourceName(bundleName, "properties");
			try(final InputStream is = loader.getResourceAsStream(resourceName)){
				if(is == null)
					return null;

				try(final Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)){
					return new PropertyResourceBundle(reader);
				}
			}
		}
	}

}
