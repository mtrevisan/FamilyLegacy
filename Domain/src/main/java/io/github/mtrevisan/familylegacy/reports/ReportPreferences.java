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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.preferences.AppPreferences;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import io.github.mtrevisan.reports.ReportLayout;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;


/**
 * What the user chose the last time a report was generated, to offer it again: the language of the document, the formats, the
 * options of each type of report, the font of the PDF and the folder where the files were written.
 * <p>
 * The privacy policy is not remembered: a report always starts from the safest one (see {@link ReportConfig#defaults()}), so that a
 * choice made once for a private report is not carried by mistake to a report that will be shared.
 * </p>
 */
public final class ReportPreferences{

	/** Where the preferences are kept. */
	public interface Store{

		/** @return	The value of the key, or {@code null} if there is none. */
		String get(String key);

		/** Sets the value of the key; a blank value removes it. */
		void put(String key, String value);

	}


	private static final String KEY_LANGUAGE = "reports.language";
	private static final String KEY_FORMATS = "reports.formats";
	private static final String KEY_FONT = "reports.font";
	private static final String KEY_DIRECTORY = "reports.directory";
	private static final String KEY_OPTIONS = "reports.options.";
	private static final String KEY_PRESETS = "reports.presets.";
	private static final String KEY_PRESET = "reports.preset.";
	private static final String KEY_LAYOUT = "reports.layout.";
	/** Separates the names of the presets, and the lines of the text of a cover (a character no name or line has). */
	private static final char NAME_SEPARATOR = '\u001E';
	private static final char LIST_SEPARATOR = ',';
	private static final char OPTION_SEPARATOR = ';';
	private static final char VALUE_SEPARATOR = '=';


	private final Store store;
	private final Supplier<String> interfaceLanguageTag;
	private final Supplier<List<ReportLanguage>> availableLanguages;


	/** @return	The preferences of the application, kept in its preferences file. */
	public static ReportPreferences application(){
		final AppPreferences preferences = AppPreferences.getInstance();
		return new ReportPreferences(new Store(){
			@Override
			public String get(final String key){
				return preferences.stringValue(key, null);
			}

			@Override
			public void put(final String key, final String value){
				preferences.setStringValue(key, value);
			}
		}, preferences::languageTag);
	}

	/**
	 * @param store	Where the preferences are kept.
	 * @param interfaceLanguageTag	The language tag of the user interface, the language of a document until the user chooses another.
	 */
	public ReportPreferences(final Store store, final Supplier<String> interfaceLanguageTag){
		this(store, interfaceLanguageTag, ReportLanguage::available);
	}

	/** @param availableLanguages	The languages a report can be written in. */
	public ReportPreferences(final Store store, final Supplier<String> interfaceLanguageTag,
			final Supplier<List<ReportLanguage>> availableLanguages){
		this.store = store;
		this.interfaceLanguageTag = interfaceLanguageTag;
		this.availableLanguages = availableLanguages;
	}


	/** @return	The language of a document when the user has not chosen one: the one of the user interface. */
	public ReportLanguage defaultLanguage(){
		return ReportLanguage.forLanguageTag(interfaceLanguageTag.get(), availableLanguages.get());
	}

	/** @return	The language the user chose the last time, or the default one. */
	public ReportLanguage language(){
		final ReportLanguage stored = ReportLanguage.withTag(store.get(KEY_LANGUAGE), availableLanguages.get());
		if(stored != null)
			return stored;
		return defaultLanguage();
	}

	public void setLanguage(final ReportLanguage language){
		store.put(KEY_LANGUAGE, language.locale().toLanguageTag());
	}


	/** @return	The ids of the formats chosen the last time; empty if none was chosen yet. */
	public List<String> formats(){
		final String value = store.get(KEY_FORMATS);
		return (value == null? List.of(): List.of(StringUtils.split(value, LIST_SEPARATOR)));
	}

	public void setFormats(final Collection<String> ids){
		store.put(KEY_FORMATS, String.join(String.valueOf(LIST_SEPARATOR), ids));
	}


	/** @return	The font chosen for the PDF the last time, or {@code null} if none was chosen (the renderer picks one). */
	public String font(){
		final String value = store.get(KEY_FONT);
		return (value == null || value.isBlank()? null: value);
	}

	/** @param font	The font chosen for the PDF; {@code null} or blank for the one the renderer picks. */
	public void setFont(final String font){
		store.put(KEY_FONT, font);
	}


	/** @return	The folder where the files were written the last time, or {@code null}. */
	public String directory(){
		return store.get(KEY_DIRECTORY);
	}

	public void setDirectory(final String directory){
		store.put(KEY_DIRECTORY, directory);
	}


	/**
	 * @param type	The type of the report.
	 * @param defaults	The configuration of an option that was not remembered (and the privacy policy and language, that never are).
	 * @return	The defaults with the options remembered for the type of report.
	 */
	public ReportConfig options(final ReportType type, final ReportConfig defaults){
		return withStored(store.get(optionsKey(type)), defaults);
	}

	/** @return	The defaults with the options of the serialized value (a damaged entry is skipped). */
	private static ReportConfig withStored(final String value, final ReportConfig defaults){
		if(value == null)
			return defaults;

		ReportConfig config = defaults;
		for(final String entry : StringUtils.split(value, OPTION_SEPARATOR)){
			final int separator = entry.indexOf(VALUE_SEPARATOR);
			if(separator <= 0)
				continue;

			final ReportOption option = optionNamed(entry.substring(0, separator));
			if(option == null)
				continue;
			final Object parsed = parse(option.valueIn(defaults), entry.substring(separator + 1));
			if(parsed != null)
				config = config.with(option, parsed);
		}
		return config;
	}

	/**
	 * @param type	The type of the report.
	 * @param config	The configuration the user chose.
	 * @param options	The options to remember: the ones the user could choose.
	 */
	public void setOptions(final ReportType type, final ReportConfig config, final Collection<ReportOption> options){
		store.put(optionsKey(type), serialize(config, options));
	}

	private static String serialize(final ReportConfig config, final Collection<ReportOption> options){
		final List<String> entries = new ArrayList<>(options.size());
		for(final ReportOption option : options)
			entries.add(option.name() + VALUE_SEPARATOR + option.valueIn(config));
		return String.join(String.valueOf(OPTION_SEPARATOR), entries);
	}


	/* ----- Presets: named sets of options of a type of report ------------- */

	/** @return	The names of the presets saved for the type of report, in alphabetical order. */
	public List<String> presetNames(final ReportType type){
		final String value = store.get(KEY_PRESETS + typeKey(type));
		if(value == null || value.isBlank())
			return List.of();

		final List<String> names = new ArrayList<>(List.of(StringUtils.split(value, NAME_SEPARATOR)));
		names.sort(String.CASE_INSENSITIVE_ORDER);
		return names;
	}

	/**
	 * @param type	The type of the report.
	 * @param name	The name of a preset.
	 * @param base	The configuration the options of the preset are put on (so that what a preset does not keep, as the privacy policy, stays).
	 * @return	The base with the options of the preset; the base itself if there is no such preset.
	 */
	public ReportConfig preset(final ReportType type, final String name, final ReportConfig base){
		return withStored(store.get(presetKey(type, name)), base);
	}

	/**
	 * Saves the options as a preset, replacing the one with the same name.
	 *
	 * @param type	The type of the report.
	 * @param name	The name of the preset; a blank one saves nothing.
	 * @param config	The configuration the user chose.
	 * @param options	The options to keep: the ones the user could choose.
	 */
	public void savePreset(final ReportType type, final String name, final ReportConfig config, final Collection<ReportOption> options){
		final String clean = cleanName(name);
		if(clean.isEmpty())
			return;

		store.put(presetKey(type, clean), serialize(config, options));
		final List<String> names = new ArrayList<>(presetNames(type));
		if(names.stream().noneMatch(clean::equalsIgnoreCase)){
			names.add(clean);
			store.put(KEY_PRESETS + typeKey(type), String.join(String.valueOf(NAME_SEPARATOR), names));
		}
	}

	public void deletePreset(final ReportType type, final String name){
		store.put(presetKey(type, name), StringUtils.EMPTY);
		final List<String> names = new ArrayList<>(presetNames(type));
		if(names.removeIf(name::equalsIgnoreCase))
			store.put(KEY_PRESETS + typeKey(type), String.join(String.valueOf(NAME_SEPARATOR), names));
	}

	/** @return	The name without the surrounding blanks, and without the character that separates the names. */
	private static String cleanName(final String name){
		return (name != null? StringUtils.remove(name, NAME_SEPARATOR).strip(): StringUtils.EMPTY);
	}

	private static String presetKey(final ReportType type, final String name){
		return KEY_PRESET + typeKey(type) + "." + cleanName(name).toLowerCase(Locale.ROOT);
	}


	/* ----- Layout of the pages (shared by every type of report) ----------- */

	/** @return	The layout of the pages the user chose the last time, or the default one. */
	public ReportLayout layout(){
		ReportLayout.PaperSize paper = ReportLayout.DEFAULT.paper();
		final String paperName = store.get(KEY_LAYOUT + "paper");
		for(final ReportLayout.PaperSize candidate : ReportLayout.PaperSize.values())
			if(candidate.name().equals(paperName))
				paper = candidate;

		int margin = 0;
		try{
			final String value = store.get(KEY_LAYOUT + "margin");
			if(value != null)
				margin = Integer.parseInt(value.strip());
		}
		catch(final NumberFormatException ignored){
			//a damaged value is the default one
		}

		final String headings = store.get(KEY_LAYOUT + "headings");
		return new ReportLayout(paper, margin, !"false".equalsIgnoreCase(headings), StringUtils.defaultString(store.get(KEY_LAYOUT + "footnote")),
			StringUtils.replaceChars(StringUtils.defaultString(store.get(KEY_LAYOUT + "cover")), NAME_SEPARATOR, '\n'));
	}

	public void setLayout(final ReportLayout layout){
		store.put(KEY_LAYOUT + "paper", layout.paper().name());
		store.put(KEY_LAYOUT + "margin", String.valueOf(layout.marginMm()));
		store.put(KEY_LAYOUT + "headings", String.valueOf(layout.pageHeadings()));
		store.put(KEY_LAYOUT + "footnote", layout.footnote());
		store.put(KEY_LAYOUT + "cover", StringUtils.replaceChars(StringUtils.remove(layout.coverText(), '\r'), '\n', NAME_SEPARATOR));
	}


	private static String optionsKey(final ReportType type){
		return KEY_OPTIONS + typeKey(type);
	}

	private static String typeKey(final ReportType type){
		return type.name().toLowerCase(Locale.ROOT);
	}

	private static ReportOption optionNamed(final String name){
		for(final ReportOption option : ReportOption.values())
			if(option.name().equals(name))
				return option;
		return null;
	}

	/** @return	The value of the text as the kind of the example, or {@code null} if it is not one (a damaged file is not fatal). */
	private static Object parse(final Object example, final String text){
		if(example instanceof Boolean){
			if("true".equalsIgnoreCase(text))
				return Boolean.TRUE;
			if("false".equalsIgnoreCase(text))
				return Boolean.FALSE;
			return null;
		}
		if(example instanceof Integer){
			try{
				return Integer.valueOf(text.strip());
			}
			catch(final NumberFormatException e){
				return null;
			}
		}
		if(example instanceof Enum<?> constant)
			for(final Object candidate : constant.getDeclaringClass().getEnumConstants())
				if(((Enum<?>)candidate).name().equals(text))
					return candidate;
		return null;
	}

}
