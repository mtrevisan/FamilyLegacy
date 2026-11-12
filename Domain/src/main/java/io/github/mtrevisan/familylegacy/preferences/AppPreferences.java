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
package io.github.mtrevisan.familylegacy.preferences;


import io.github.mtrevisan.familylegacy.util.AtomicFileWriter;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;


/**
 * Persistent user preferences for the application's visual appearance.
 *
 * <p>Backed by a properties file stored under the user's home directory, so
 * the settings survive across runs and can be inspected or reset by hand
 * without touching the application.</p>
 *
 * <p>Only two preferences are currently persisted:</p>
 * <ul>
 *   <li>{@code ui.language} — the BCP 47 language tag of the UI locale
 *       (e.g. {@code "it"}, {@code "en-US"}, {@code "zh-CN"});</li>
 *   <li>{@code ui.font.family} — the family name of the UI font;</li>
 *   <li>{@code ui.font.size} — the point size of the UI font.</li>
 * </ul>
 *
 * <p>The class is a singleton and is thread-safe for reads; writes are
 * serialized by the underlying file system and can be triggered at any
 * time from the Event Dispatch Thread.</p>
 */
public final class AppPreferences{

	/** Directory under the user's home where the preferences file is stored. */
	private static final Path CONFIG_DIR =
		Path.of(System.getProperty("user.home"), ".familylegacy");

	/** Full path to the preferences file. */
	private static final Path CONFIG_FILE = CONFIG_DIR.resolve("preferences.properties");

	/* ----- Property keys --------------------------------------------------- */

	public static final String KEY_LANGUAGE = "ui.language";
	public static final String KEY_FONT_FAMILY = "ui.font.family";
	public static final String KEY_FONT_SIZE = "ui.font.size";
	public static final String KEY_PRIVACY_POLICY = "privacy.policy";
	// view preferences: how the views order children and partners (presentation only, never genealogical data)
	public static final String KEY_CHILDREN_ORDER = "view.order.children";
	public static final String KEY_PARTNER_ORDER = "view.order.partners";
	// view preferences: how the location of the media is shown, and the directory it is relative to (presentation only)
	public static final String KEY_MEDIA_PATH_DISPLAY = "view.media.path";
	public static final String KEY_MEDIA_BASE_DIRECTORY = "view.media.base_directory";
	// the Identity Assistant: from which similarity a pair is proposed, how many, and the scores of the confidence (see IdentitySettings)
	public static final String KEY_IDENTITY_THRESHOLD = "assistant.identity.threshold";
	public static final String KEY_IDENTITY_MAX_PAIRS = "assistant.identity.max_pairs";
	public static final String KEY_IDENTITY_HIGH_SCORE = "assistant.identity.high_score";
	public static final String KEY_IDENTITY_MEDIUM_SCORE = "assistant.identity.medium_score";
	public static final String KEY_IDENTITY_OTHER_PERCENT = "assistant.identity.other_percent";
	// the level of the interface: how much of FLEF the menus offer by default (presentation only, never genealogical data)
	public static final String KEY_UX_LEVEL = "ui.level";
	// whether the editors suggest advanced concepts when they could help (presentation only)
	public static final String KEY_SUGGESTIONS = "ui.suggestions";
	// whether the application looks for a newer release at startup, and when it last asked GitHub (epoch milliseconds)
	public static final String KEY_UPDATE_CHECK = "update.check";
	public static final String KEY_UPDATE_LAST = "update.last";
	// the findings of the Research Assistant ignored by the user, for each file (a presentation choice, never genealogical data)
	private static final String KEY_IGNORED_FINDINGS_PREFIX = "assistant.ignored.";

	/* ----- Defaults -------------------------------------------------------- */

	/** Default UI language tag when nothing is persisted. */
	private static final String DEFAULT_LANGUAGE = "en";

	/** Default UI font size, in points, when nothing is persisted. */
	private static final int DEFAULT_FONT_SIZE = 12;


	private static final AppPreferences INSTANCE = new AppPreferences();


	private final Properties properties = new Properties();


	private AppPreferences(){
		load();
	}


	public static AppPreferences getInstance(){
		return INSTANCE;
	}


	/* ======================================================================
	 *                          Accessors
	 * ====================================================================== */

	/** Returns the persisted UI language tag, or the default when unset. */
	public String languageTag(){
		return properties.getProperty(KEY_LANGUAGE, DEFAULT_LANGUAGE);
	}

	/** Returns the persisted UI font family, or {@code null} for the system default. */
	public String fontFamily(){
		final String v = properties.getProperty(KEY_FONT_FAMILY);
		return (StringUtils.isEmpty(v)? null: v);
	}

	/** Returns the persisted UI font size, or the default when unset. */
	public int fontSize(){
		final String v = properties.getProperty(KEY_FONT_SIZE);
		if(StringUtils.isEmpty(v))
			return DEFAULT_FONT_SIZE;
		try{
			return Integer.parseInt(v.trim());
		}
		catch(final NumberFormatException ignored){
			return DEFAULT_FONT_SIZE;
		}
	}


	/* ======================================================================
	 *                          Mutators
	 * ====================================================================== */

	/** Persists the UI language tag. */
	public void setLanguageTag(final String tag){
		put(KEY_LANGUAGE, Objects.requireNonNull(tag, "tag"));
	}

	/** Persists the UI font family. Passing {@code null} resets to system default. */
	public void setFontFamily(final String family){
		if(StringUtils.isEmpty(family))
			properties.remove(KEY_FONT_FAMILY);
		else
			properties.setProperty(KEY_FONT_FAMILY, family);
		save();
	}

	/** Returns the persisted name of the privacy policy, or {@code null} when unset (the privacy package knows what the names mean). */
	public String privacyPolicyName(){
		return properties.getProperty(KEY_PRIVACY_POLICY);
	}

	/** Persists the name of the privacy policy. */
	public void setPrivacyPolicyName(final String name){
		put(KEY_PRIVACY_POLICY, Objects.requireNonNull(name, "name"));
	}

	/** Returns the persisted level of the interface, or the default (basic) when unset or unknown. */
	public UxLevel uxLevel(){
		try{
			return UxLevel.valueOf(properties.getProperty(KEY_UX_LEVEL, UxLevel.BASIC.name()));
		}
		catch(final IllegalArgumentException ignored){
			return UxLevel.BASIC;
		}
	}

	/** Returns whether the editors suggest advanced concepts when they could help; on unless the user turned it off. */
	public boolean suggestionsEnabled(){
		return !"false".equals(properties.getProperty(KEY_SUGGESTIONS));
	}

	/** Persists whether the editors suggest advanced concepts (a view preference: it is not stored in the genealogy). */
	public void setSuggestionsEnabled(final boolean enabled){
		put(KEY_SUGGESTIONS, Boolean.toString(enabled));
	}

	/** Returns whether the application looks for a newer release at startup; on unless the user turned it off. */
	public boolean updateCheckEnabled(){
		return !"false".equals(properties.getProperty(KEY_UPDATE_CHECK));
	}

	/** Persists whether the application looks for a newer release at startup. */
	public void setUpdateCheckEnabled(final boolean enabled){
		put(KEY_UPDATE_CHECK, Boolean.toString(enabled));
	}

	/** Returns when GitHub was last asked for a newer release at startup (epoch milliseconds), 0 if never. */
	public long lastUpdateCheck(){
		try{
			return Long.parseLong(properties.getProperty(KEY_UPDATE_LAST, "0").trim());
		}
		catch(final NumberFormatException ignored){
			return 0;
		}
	}

	/** Persists when GitHub was last asked for a newer release at startup (epoch milliseconds). */
	public void setLastUpdateCheck(final long millis){
		put(KEY_UPDATE_LAST, Long.toString(millis));
	}

	/** Persists the level of the interface (a view preference: it is not stored in the genealogy). */
	public void setUxLevel(final UxLevel level){
		put(KEY_UX_LEVEL, Objects.requireNonNull(level, "level").name());
	}

	/** Returns the persisted display order of the children, or the default (by birth date) when unset or unknown. */
	public ChildrenOrder childrenOrder(){
		try{
			return ChildrenOrder.valueOf(properties.getProperty(KEY_CHILDREN_ORDER, ChildrenOrder.BIRTH_DATE.name()));
		}
		catch(final IllegalArgumentException ignored){
			return ChildrenOrder.BIRTH_DATE;
		}
	}

	/** Persists the display order of the children (a view preference: it is not stored in the genealogy). */
	public void setChildrenOrder(final ChildrenOrder order){
		put(KEY_CHILDREN_ORDER, Objects.requireNonNull(order, "order").name());
	}

	/** Returns the persisted display order of the partners, or the default (union chronology) when unset or unknown. */
	public PartnerOrder partnerOrder(){
		try{
			return PartnerOrder.valueOf(properties.getProperty(KEY_PARTNER_ORDER, PartnerOrder.UNION_CHRONOLOGY.name()));
		}
		catch(final IllegalArgumentException ignored){
			return PartnerOrder.UNION_CHRONOLOGY;
		}
	}

	/** Persists the display order of the partners (a view preference: it is not stored in the genealogy). */
	public void setPartnerOrder(final PartnerOrder order){
		put(KEY_PARTNER_ORDER, Objects.requireNonNull(order, "order").name());
	}

	/**
	 * @param scope	the digest of the file the findings are about
	 * @return	the digests of the findings the user ignored for the file
	 */
	public synchronized Set<String> ignoredFindings(final String scope){
		final String value = properties.getProperty(KEY_IGNORED_FINDINGS_PREFIX + scope, "");
		return (value.isEmpty()? Set.of(): Set.copyOf(decodeFindings(value)));
	}

	/** Persists the findings the user ignored for a file (a view preference: they are not stored in the genealogy). */
	public synchronized void setIgnoredFindings(final String scope, final Set<String> findings){
		put(KEY_IGNORED_FINDINGS_PREFIX + scope, encodeFindings(findings));
	}

	/**
	 * Joins the ids with a comma, escaping a backslash and a comma inside an id with a backslash, so that every id round-trips.
	 * The old format (plain comma-separated, no escapes) is read unchanged by {@link #decodeFindings(String)}.
	 */
	static String encodeFindings(final Set<String> findings){
		final StringBuilder sb = new StringBuilder();
		for(final String finding : new TreeSet<>(findings)){
			if(!sb.isEmpty())
				sb.append(',');
			for(int i = 0; i < finding.length(); i ++){
				final char c = finding.charAt(i);
				if(c == '\\' || c == ',')
					sb.append('\\');
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/** Splits the text written by {@link #encodeFindings(Set)} (or by the old unescaped format) into the ids; empty ids are dropped. */
	static List<String> decodeFindings(final String value){
		final List<String> result = new ArrayList<>();
		final StringBuilder current = new StringBuilder();
		for(int i = 0; i < value.length(); i ++){
			final char c = value.charAt(i);
			if(c == '\\' && i + 1 < value.length())
				current.append(value.charAt(++ i));
			else if(c == ','){
				if(!current.isEmpty())
					result.add(current.toString());
				current.setLength(0);
			}
			else
				current.append(c);
		}
		if(!current.isEmpty())
			result.add(current.toString());
		return result;
	}

	/** Returns how the location of the media is shown, or the default (the whole location) when unset or unknown. */
	public MediaPathDisplay mediaPathDisplay(){
		try{
			return MediaPathDisplay.valueOf(properties.getProperty(KEY_MEDIA_PATH_DISPLAY, MediaPathDisplay.FULL.name()));
		}
		catch(final IllegalArgumentException ignored){
			return MediaPathDisplay.FULL;
		}
	}

	/** Persists how the location of the media is shown (a view preference: it is not stored in the genealogy). */
	public void setMediaPathDisplay(final MediaPathDisplay display){
		put(KEY_MEDIA_PATH_DISPLAY, Objects.requireNonNull(display, "display").name());
	}

	/** Returns the base directory the media paths are shown relative to, or {@code null} when unset. */
	public String mediaBaseDirectory(){
		final String v = properties.getProperty(KEY_MEDIA_BASE_DIRECTORY);
		return (StringUtils.isBlank(v)? null: v);
	}

	/** Persists the base directory the media paths are shown relative to; {@code null} clears it. */
	public void setMediaBaseDirectory(final String directory){
		put(KEY_MEDIA_BASE_DIRECTORY, directory);
	}

	/**
	 * @param key	The key of a whole number.
	 * @param defaultValue	The value when nothing is persisted or it is not a number.
	 * @return	The persisted number, or the default.
	 */
	public synchronized int intValue(final String key, final int defaultValue){
		final String v = properties.getProperty(key);
		if(StringUtils.isBlank(v))
			return defaultValue;
		try{
			return Integer.parseInt(v.trim());
		}
		catch(final NumberFormatException ignored){
			return defaultValue;
		}
	}

	/** Persists a whole number. */
	public synchronized void setIntValue(final String key, final int value){
		put(key, Integer.toString(value));
	}

	/** Persists the UI font size, in points. */
	public void setFontSize(final int size){
		put(KEY_FONT_SIZE, Integer.toString(size));
	}

	/** Returns the persisted text of a key, or {@code defaultValue} when unset. */
	public synchronized String stringValue(final String key, final String defaultValue){
		return properties.getProperty(key, defaultValue);
	}

	/** Persists the text of a key; an empty text removes the key. */
	public synchronized void setStringValue(final String key, final String value){
		put(key, value);
	}

	/** Returns the file path of the preferences file (may not exist yet). */
	public static Path filePath(){
		return CONFIG_FILE;
	}


	/* ======================================================================
	 *                          Storage
	 * ====================================================================== */

	private void put(final String key, final String value){
		if(StringUtils.isEmpty(value))
			properties.remove(key);
		else
			properties.setProperty(key, value);
		save();
	}


	private void load(){
		if(!Files.exists(CONFIG_FILE))
			return;

		try(final InputStream in = Files.newInputStream(CONFIG_FILE)){
			properties.load(in);
		}
		catch(final IOException ignored){
			// A corrupted or unreadable file is treated as "no preferences":
			// the application starts with defaults. The user can delete the
			// file to reset everything.
		}
	}


	private synchronized void save(){
		try{
			Files.createDirectories(CONFIG_DIR);
			//written to a temporary file and moved in place, so that a crash cannot leave a truncated file
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			properties.store(out, "Family Legacy -- user preferences");
			AtomicFileWriter.write(CONFIG_FILE, out.toString(StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1);
		}
		catch(final IOException ignored){
			// If writing fails (read-only home, disk full), the preference
			// applies to the current session but is not persisted. The
			// application keeps working; only the persistence is lost.
		}
	}

}
