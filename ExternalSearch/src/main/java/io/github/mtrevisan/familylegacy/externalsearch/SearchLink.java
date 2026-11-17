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

package io.github.mtrevisan.familylegacy.externalsearch;

import io.github.mtrevisan.familylegacy.i18n.I18N;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;


/**
 * The search of a person on a site: the address to open, or why it cannot be opened.
 *
 * @param site	The site.
 * @param relevance	How likely the site is to have the person.
 * @param status	Whether the search can be opened.
 * @param url	The address to open (with the credentials), {@code null} if the template is not valid.
 * @param displayUrl	The address to show or to keep, without the credentials, {@code null} if the template is not valid.
 * @param missingCredentials	The credentials the template needs that the user has not given.
 * @param problem	Why the template is not valid, {@code null} if it is.
 */
public record SearchLink(SearchSite site, Relevance relevance, Status status, String url, String displayUrl, List<String> missingCredentials,
		String problem){

	/** How long a life is taken to be at most, to tell the years of a person from only its birth or only its death. */
	static final int LONGEST_LIFE = 100;


	/** Whether a search can be opened. */
	public enum Status{
		/** It can be opened. */
		READY,
		/** The template needs credentials the user has not given. */
		MISSING_CREDENTIALS,
		/** The template asks for a name, and no part of the name of the person is known. */
		NO_NAME,
		/** The template is not valid. */
		INVALID_TEMPLATE;

		/** @return	The (localized) name. */
		public String label(){
			return I18N.t("external_search.status." + name().toLowerCase(Locale.ROOT));
		}
	}


	public SearchLink{
		missingCredentials = List.copyOf(missingCredentials);
	}


	/** @return	Whether the search can be opened. */
	public boolean isReady(){
		return (status == Status.READY);
	}


	/**
	 * @param sites	The sites.
	 * @param subject	The values of the person.
	 * @param credentials	The credentials of the user.
	 * @return	The search on each site, the likely sites first, then by kind (records first) and name.
	 */
	public static List<SearchLink> forSites(final List<SearchSite> sites, final SearchSubject subject, final CredentialStore credentials){
		final Set<String> countries = new LinkedHashSet<>();
		if(subject.has(Placeholder.BIRTH_COUNTRY))
			countries.add(subject.value(Placeholder.BIRTH_COUNTRY));
		if(subject.has(Placeholder.DEATH_COUNTRY))
			countries.add(subject.value(Placeholder.DEATH_COUNTRY));
		final Integer from = firstYear(subject);
		final Integer to = lastYear(subject);

		final List<SearchLink> links = new ArrayList<>(sites.size());
		for(final SearchSite site : sites)
			links.add(forSite(site, subject, credentials, site.coverage().relevanceFor(countries, from, to)));

		final Collator collator = Collator.getInstance();
		links.sort(Comparator.comparing(SearchLink::relevance)
			.thenComparing(link -> link.site().kind())
			.thenComparing(link -> link.site().name(), collator));
		return links;
	}

	private static SearchLink forSite(final SearchSite site, final SearchSubject subject, final CredentialStore credentials,
			final Relevance relevance){
		final UrlTemplate template;
		try{
			template = site.template();
		}
		catch(final UrlTemplate.TemplateException e){
			return new SearchLink(site, relevance, Status.INVALID_TEMPLATE, null, null, List.of(), e.describe());
		}

		final String url = template.expand(subject, name -> credentials.get(site.id(), name).orElse(null));
		final String displayUrl = template.expandForDisplay(subject);
		final List<String> missing = credentials.missing(site);
		Status status = Status.READY;
		if(!missing.isEmpty())
			status = Status.MISSING_CREDENTIALS;
		else if(template.placeholders().stream().anyMatch(Placeholder::isName) && !subject.hasName())
			status = Status.NO_NAME;
		return new SearchLink(site, relevance, status, url, displayUrl, missing, null);
	}

	/** @return	The first year the person may have been alive, {@code null} if nothing is known. */
	static Integer firstYear(final SearchSubject subject){
		final OptionalInt birth = first(subject, Placeholder.BIRTH_YEAR_FROM, Placeholder.BIRTH_YEAR);
		if(birth.isPresent())
			return birth.getAsInt();

		final OptionalInt death = first(subject, Placeholder.DEATH_YEAR_FROM, Placeholder.DEATH_YEAR);
		return (death.isPresent()? death.getAsInt() - LONGEST_LIFE: null);
	}

	/** @return	The last year the person may have been alive, {@code null} if nothing is known. */
	static Integer lastYear(final SearchSubject subject){
		final OptionalInt death = first(subject, Placeholder.DEATH_YEAR_TO, Placeholder.DEATH_YEAR);
		if(death.isPresent())
			return death.getAsInt();

		final OptionalInt birth = first(subject, Placeholder.BIRTH_YEAR_TO, Placeholder.BIRTH_YEAR);
		return (birth.isPresent()? birth.getAsInt() + LONGEST_LIFE: null);
	}

	private static OptionalInt first(final SearchSubject subject, final Placeholder preferred, final Placeholder fallback){
		final OptionalInt value = subject.year(preferred);
		return (value.isPresent()? value: subject.year(fallback));
	}

}
