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
package io.github.mtrevisan.familylegacy.v2.ui.helpers;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;


public final class LocaleHelper{

	private static List<String> cachedLocaleCodes;


	private LocaleHelper(){}


	/**
	 * Returns an immutable list of sorted BCP 47 language tags available in the JVM,
	 * prepended with an empty string for empty selection.
	 */
	public static synchronized List<String> getAvailableLanguageTags(){
		if(cachedLocaleCodes == null){
			// Extract BCP 47 language tags (e.g. "en-US - English (United States)")
			final TreeSet<String> tags = new TreeSet<>(Comparator.naturalOrder());
			for(final Locale locale : Locale.getAvailableLocales()){
				final String tag = locale.toLanguageTag();
				if(!tag.equals("und") && !tag.isBlank()){
					final String displayName = locale.getDisplayName(Locale.ENGLISH);
					if(!displayName.isBlank())
						tags.add(tag + " - " + displayName);
					else
						tags.add(tag);
				}
			}
			final List<String> list = new ArrayList<>(tags.size() + 1);
			list.add(StringUtils.EMPTY);
			list.addAll(tags);

			cachedLocaleCodes = Collections.unmodifiableList(list);
		}
		return cachedLocaleCodes;
	}

}
