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
