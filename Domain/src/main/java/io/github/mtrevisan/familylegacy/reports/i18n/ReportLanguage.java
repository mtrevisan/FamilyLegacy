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
package io.github.mtrevisan.familylegacy.reports.i18n;

import io.github.mtrevisan.familylegacy.preferences.AvailableLanguages;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;


/**
 * A language a report can be written in.
 *
 * <p>The languages are not a closed list: dropping a file {@code report_messages_<language>[_<COUNTRY>].properties} in
 * {@code i18n/report} is enough to make the language available (see {@link #available()}), with the name shown in the user interface
 * taken from its {@value #KEY_LANGUAGE_NAME} property. The file without suffix is the English one and the fallback of every text a
 * language does not translate.</p>
 *
 * <p><b>RTL languages</b> (Arabic, Hebrew, Urdu, Persian): the {@code .properties} file is UTF-8, so the text is stored literally;
 * right-to-left rendering is a concern of the output format, not of the bundle.</p>
 */
public final class ReportLanguage{

	private static final String BUNDLE_DIRECTORY = "/i18n/report";
	private static final String BUNDLE_BASE_NAME = "report_messages";
	private static final String BUNDLE_PATH = BUNDLE_DIRECTORY + "/" + BUNDLE_BASE_NAME;
	private static final String KEY_LANGUAGE_NAME = "LANGUAGE_NAME";


	/** The language of the file without suffix: it is always there. */
	public static final ReportLanguage ENGLISH = new ReportLanguage(Locale.ENGLISH);


	private final Locale locale;
	private String displayName;


	private ReportLanguage(final Locale locale){
		this.locale = Objects.requireNonNull(locale, "locale");
	}


	public Locale locale(){
		return locale;
	}

	/**
	 * @return	The languages a report can be written in: English (the root bundle) and one for each
	 * 	{@code report_messages_<language>[_<COUNTRY>].properties} file found, English first and then sorted by name.
	 */
	public static List<ReportLanguage> available(){
		final List<ReportLanguage> languages = new ArrayList<>();
		for(final Locale found : AvailableLanguages.findLocales(BUNDLE_DIRECTORY, BUNDLE_BASE_NAME))
			if(!Locale.ENGLISH.equals(found))
				languages.add(of(found));
		languages.sort((a, b) -> {
			final int byName = a.displayName().compareToIgnoreCase(b.displayName());
			return (byName != 0? byName: a.locale.toLanguageTag().compareTo(b.locale.toLanguageTag()));
		});
		languages.addFirst(ENGLISH);
		return languages;
	}

	/** @return	{@code language} if a report can be written in it, English otherwise. */
	public static ReportLanguage orEnglish(final ReportLanguage language){
		return (available().contains(language)? language: ENGLISH);
	}

	/** @return	The language of the locale. */
	public static ReportLanguage of(final Locale locale){
		return (Locale.ENGLISH.equals(locale)? ENGLISH: new ReportLanguage(locale));
	}

	/**
	 * @param tag	A BCP 47 language tag, as the one of the user interface ({@code "it"}, {@code "en-US"}, {@code "zh-CN"}).
	 * @return	The available language of that tag, or the closest one with the same base language; English if there is none.
	 */
	public static ReportLanguage forLanguageTag(final String tag){
		return forLanguageTag(tag, available());
	}

	/** @see #forLanguageTag(String) */
	public static ReportLanguage forLanguageTag(final String tag, final List<ReportLanguage> candidates){
		if(tag == null || tag.isBlank())
			return ENGLISH;

		final Locale wanted = Locale.forLanguageTag(tag.strip());
		for(final ReportLanguage language : candidates)
			if(language.locale.equals(wanted))
				return language;
		for(final ReportLanguage language : candidates)
			if(language.locale.getLanguage().equals(wanted.getLanguage()))
				return language;
		return ENGLISH;
	}

	/** @return	The language with exactly that tag among the candidates, or {@code null}. */
	public static ReportLanguage withTag(final String tag, final List<ReportLanguage> candidates){
		if(tag != null)
			for(final ReportLanguage language : candidates)
				if(language.locale.toLanguageTag().equals(tag))
					return language;
		return null;
	}

	private String bundleFile(){
		if(Locale.ENGLISH.equals(locale))
			return BUNDLE_PATH + ".properties";

		final String country = locale.getCountry();
		return BUNDLE_PATH + "_" + locale.getLanguage() + (country.isEmpty()? "": "_" + country) + ".properties";
	}

	/** @return	The name of the language, written in the language itself: the {@code LANGUAGE_NAME} property of its bundle, if any. */
	public synchronized String displayName(){
		if(displayName == null){
			displayName = readName();
			if(displayName.isEmpty())
				displayName = StringUtils.capitalize(locale.getDisplayName(locale));
		}
		return displayName;
	}

	private String readName(){
		try(final InputStream stream = ReportLanguage.class.getResourceAsStream(bundleFile())){
			if(stream != null){
				final Properties properties = new Properties();
				properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
				return properties.getProperty(KEY_LANGUAGE_NAME, "").trim();
			}
		}
		catch(final IOException ignored){}
		return "";
	}

	@Override
	public boolean equals(final Object obj){
		return (this == obj || obj instanceof ReportLanguage other && locale.equals(other.locale));
	}

	@Override
	public int hashCode(){
		return locale.hashCode();
	}

	@Override
	public String toString(){
		return displayName();
	}

}
