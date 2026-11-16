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
package io.github.mtrevisan.updater;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * An optional part of the application that can be downloaded (a set of fonts, a plugin), as the catalog describes it.
 *
 * @param id	What identifies the component (letters, digits, {@code .}, {@code _} and {@code -}).
 * @param names	The name by language tag (at least one).
 * @param descriptions	The description by language tag (may be empty).
 * @param target	Where it is installed: one of {@link ComponentCatalog#TARGETS}.
 * @param release	The tag of the release that holds the file, or {@code latest}; {@code null} if the file comes from {@code url}.
 * @param asset	The name of the file in the release; {@code null} if the file comes from {@code url}.
 * @param url	Where to download the file from (HTTPS); {@code null} if the file is an asset of a release.
 * @param sha256	The SHA-256 of the file, only for {@code url} (an asset of a release has it from GitHub); {@code null} otherwise.
 * @param size	The size in bytes of the file, if the catalog says it, or -1.
 * @param minVersion	The oldest version of the application that can use it, or {@code null}.
 * @param files	The files that tell that the component is installed (may be empty).
 */
public record Component(String id, Map<String, String> names, Map<String, String> descriptions, String target, String release, String asset,
		URI url, String sha256, long size, String minVersion, List<String> files){

	/** The release whose files are the ones of the latest published version of the application. */
	public static final String LATEST = "latest";


	/** @return	The name in the language, or in English, or the first available, or the id. */
	public String name(final Locale locale){
		final String name = localized(names, locale);
		return (name != null? name: id);
	}

	/** @return	The description in the language, or in English, or the first available, or an empty text. */
	public String description(final Locale locale){
		final String description = localized(descriptions, locale);
		return (description != null? description: "");
	}

	private static String localized(final Map<String, String> texts, final Locale locale){
		if(texts.isEmpty())
			return null;

		final String language = locale.getLanguage();
		String text = texts.get(locale.toLanguageTag());
		if(text == null)
			text = texts.get(language);
		if(text == null)
			text = texts.get("en");
		return (text != null? text: texts.values().iterator().next());
	}

}
