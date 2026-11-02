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
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 * The languages the application can be shown in, found by looking at the internationalization files that are actually available:
 * a file {@code <bundle>_<language>[_<COUNTRY>].properties} makes the language available, while the file without suffix is
 * the English one.
 */
final class AvailableLanguages{

	private static final Logger LOGGER = LoggerFactory.getLogger(AvailableLanguages.class);

	private static final String UI_DIRECTORY = "/i18n/ui";
	private static final String UI_BASE_NAME = "messages";
	private static final String KEY_LANGUAGE_NAME = "language.name";

	/** The directories (in the classpath) with the internationalization files, with the base name of the bundle in each one. */
	private static final Map<String, String> BUNDLES = Map.of(UI_DIRECTORY, UI_BASE_NAME);

	private static final String EXTENSION = ".properties";
	private static final char SEPARATOR = '_';


	private AvailableLanguages(){}


	/**
	 * The available languages: English first, then the others sorted by name.
	 *
	 * @return	The name of each language, written in the language itself, and its locale.
	 */
	static Map<String, Locale> find(){
		final Collection<String> fileNames = new ArrayList<>();
		for(final String directory : BUNDLES.keySet())
			fileNames.addAll(listFiles(directory));

		final List<String> baseNames = new ArrayList<>(BUNDLES.values());
		return toLanguages(fileNames, baseNames, AvailableLanguages::nameOf);
	}

	/**
	 * @param fileNames	The names of the files.
	 * @param baseNames	The base names of the bundles.
	 * @param nameOf	The name of a language, written in the language itself.
	 * @return	The languages for the given files (see {@link #find()}).
	 */
	static Map<String, Locale> toLanguages(final Collection<String> fileNames, final Collection<String> baseNames,
			final Function<Locale, String> nameOf){
		final TreeSet<Locale> locales = new TreeSet<>(Comparator.comparing(nameOf)
			.thenComparing(Locale::toLanguageTag));
		for(final String fileName : fileNames){
			final Locale locale = toLocale(fileName, baseNames);
			if(locale != null && !Locale.ENGLISH.equals(locale))
				locales.add(locale);
		}

		//the file without suffix is the English one
		final Map<String, Locale> languages = new LinkedHashMap<>();
		languages.put(nameOf.apply(Locale.ENGLISH), Locale.ENGLISH);
		for(final Locale locale : locales)
			languages.put(nameOf.apply(locale), locale);
		return languages;
	}

	/** @return	The locale of an internationalization file, or {@code null} if it has no suffix or is not such a file. */
	static Locale toLocale(final String fileName, final Collection<String> baseNames){
		if(!fileName.endsWith(EXTENSION))
			return null;

		final String name = Strings.CS.removeEnd(fileName, EXTENSION);
		for(final String baseName : baseNames){
			if(!name.startsWith(baseName + SEPARATOR))
				continue;

			final String[] parts = StringUtils.split(name.substring(baseName.length() + 1), SEPARATOR);
			if(parts.length == 1)
				return Locale.of(parts[0]);
			if(parts.length == 2)
				return Locale.of(parts[0], parts[1]);
		}
		return null;
	}

	/**
	 * The name of a language, written in the language itself: it is the {@value #KEY_LANGUAGE_NAME} property of the messages of
	 * that language, or, if missing, the name known by the JVM.
	 */
	private static String nameOf(final Locale locale){
		final String file = UI_DIRECTORY + "/" + UI_BASE_NAME + (!Locale.ENGLISH.equals(locale)
			? SEPARATOR + locale.getLanguage() + (!locale.getCountry().isEmpty()? SEPARATOR + locale.getCountry(): StringUtils.EMPTY)
			: StringUtils.EMPTY) + EXTENSION;
		try(final InputStream stream = AvailableLanguages.class.getResourceAsStream(file)){
			if(stream != null){
				final Properties properties = new Properties();
				properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
				final String name = properties.getProperty(KEY_LANGUAGE_NAME);
				if(StringUtils.isNotBlank(name))
					return name.trim();
			}
		}
		catch(final IOException ioe){
			LOGGER.warn("Cannot read the name of the language from {}: {}", file, ioe.toString());
		}
		return StringUtils.capitalize(locale.getDisplayName(locale));
	}


	private static List<String> listFiles(final String directory){
		final URL url = AvailableLanguages.class.getResource(directory);
		if(url == null)
			return List.of();

		final List<String> names = new ArrayList<>();
		try{
			if("jar".equals(url.getProtocol())){
				final JarURLConnection connection = (JarURLConnection)url.openConnection();
				connection.setUseCaches(false);
				final String prefix = Strings.CS.removeStart(directory, "/") + "/";
				try(final JarFile jar = connection.getJarFile()){
					final var entries = jar.entries();
					while(entries.hasMoreElements()){
						final JarEntry entry = entries.nextElement();
						final String entryName = entry.getName();
						if(!entry.isDirectory() && entryName.startsWith(prefix) && entryName.indexOf('/', prefix.length()) < 0)
							names.add(entryName.substring(prefix.length()));
					}
				}
			}
			else{
				final Path path = Path.of(URI.create(url.toString()));
				try(final DirectoryStream<Path> stream = Files.newDirectoryStream(path)){
					for(final Path file : stream)
						names.add(file.getFileName().toString());
				}
			}
		}
		catch(final IOException | RuntimeException e){
			LOGGER.warn("Cannot list the internationalization files in {}: {}", directory, e.toString());
		}
		return names;
	}

}
