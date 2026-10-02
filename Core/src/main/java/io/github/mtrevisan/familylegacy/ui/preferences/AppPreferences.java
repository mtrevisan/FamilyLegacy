package io.github.mtrevisan.familylegacy.ui.preferences;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;


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
		return (v == null || v.isBlank()? null: v);
	}

	/** Returns the persisted UI font size, or the default when unset. */
	public int fontSize(){
		final String v = properties.getProperty(KEY_FONT_SIZE);
		if(v == null || v.isBlank())
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
		if(family == null || family.isBlank())
			properties.remove(KEY_FONT_FAMILY);
		else
			properties.setProperty(KEY_FONT_FAMILY, family);
		save();
	}

	/** Persists the UI font size, in points. */
	public void setFontSize(final int size){
		put(KEY_FONT_SIZE, Integer.toString(size));
	}

	/** Returns the file path of the preferences file (may not exist yet). */
	public static Path filePath(){
		return CONFIG_FILE;
	}


	/* ======================================================================
	 *                          Storage
	 * ====================================================================== */

	private void put(final String key, final String value){
		if(value == null || value.isBlank())
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


	private void save(){
		try{
			Files.createDirectories(CONFIG_DIR);
			try(OutputStream out = Files.newOutputStream(CONFIG_FILE)){
				properties.store(out, "Family Legacy — user preferences");
			}
		}
		catch(final IOException ignored){
			// If writing fails (read-only home, disk full), the preference
			// applies to the current session but is not persisted. The
			// application keeps working; only the persistence is lost.
		}
	}

}
