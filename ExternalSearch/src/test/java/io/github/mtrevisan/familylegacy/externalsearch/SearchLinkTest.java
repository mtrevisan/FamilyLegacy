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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


class SearchLinkTest{

	private static SearchSite site(final String id, final String template, final SiteKind kind, final String countries){
		return new SearchSite(id, id, template, kind, SiteCoverage.parse(countries, null, null), List.of(), null, false, false);
	}

	private static SearchSubject italian(){
		final Map<Placeholder, String> values = new EnumMap<>(Placeholder.class);
		values.put(Placeholder.NAME_GIVEN, "Giovanni");
		values.put(Placeholder.SURNAME, "Rossi");
		values.put(Placeholder.BIRTH_YEAR, "1805");
		values.put(Placeholder.BIRTH_COUNTRY, "IT");
		return SearchSubject.of(values);
	}


	@Test
	void theLikelySitesComeFirstThenTheRecords(){
		final List<SearchSite> sites = List.of(
			site("france", "https://fr.example.org/?n={surname}", SiteKind.RECORDS, "FR"),
			site("world-trees", "https://trees.example.org/?n={surname}", SiteKind.FAMILY_TREES, null),
			site("world-records", "https://records.example.org/?n={surname}", SiteKind.RECORDS, null),
			site("italy", "https://it.example.org/?n={surname}", SiteKind.RECORDS, "IT"));

		final List<SearchLink> links = SearchLink.forSites(sites, italian(), CredentialStore.inMemory());

		Assertions.assertEquals(List.of("italy", "world-records", "world-trees", "france"),
			links.stream().map(link -> link.site().id()).toList());
		Assertions.assertEquals(List.of(Relevance.LIKELY, Relevance.POSSIBLE, Relevance.POSSIBLE, Relevance.UNLIKELY),
			links.stream().map(SearchLink::relevance).toList());
		Assertions.assertEquals("https://it.example.org/?n=Rossi", links.getFirst().url());
	}

	@Test
	void aSearchSaysWhyItCannotBeOpened() throws IOException{
		final CredentialStore credentials = CredentialStore.inMemory();
		final SearchSite keyed = site("keyed", "https://api.example.org/?n={surname}&k={credential:api_key}", SiteKind.RECORDS, null);
		final SearchSite broken = site("broken", "https://example.org/{nickname}", SiteKind.RECORDS, null);
		final SearchSite named = site("named", "https://example.org/?n={surname}", SiteKind.RECORDS, null);
		final SearchSite home = site("home", "https://example.org/", SiteKind.RECORDS, null);

		final List<SearchLink> withoutName = SearchLink.forSites(List.of(keyed, broken, named, home),
			SearchSubject.EMPTY.with(Placeholder.BIRTH_YEAR, "1805"), credentials);
		Assertions.assertEquals(SearchLink.Status.MISSING_CREDENTIALS, status(withoutName, "keyed"));
		Assertions.assertEquals(List.of("api_key"), link(withoutName, "keyed").missingCredentials());
		Assertions.assertEquals(SearchLink.Status.INVALID_TEMPLATE, status(withoutName, "broken"));
		Assertions.assertNotNull(link(withoutName, "broken").problem());
		Assertions.assertEquals(SearchLink.Status.NO_NAME, status(withoutName, "named"));
		//an address that asks for nothing can always be opened
		Assertions.assertEquals(SearchLink.Status.READY, status(withoutName, "home"));

		credentials.put("keyed", "api_key", "s3cret");
		final SearchLink ready = link(SearchLink.forSites(List.of(keyed), italian(), credentials), "keyed");
		Assertions.assertTrue(ready.isReady());
		Assertions.assertEquals("https://api.example.org/?n=Rossi&k=s3cret", ready.url());
		Assertions.assertFalse(ready.displayUrl().contains("s3cret"));
	}

	@Test
	void theYearsOfALifeComeFromWhatIsKnown(){
		Assertions.assertEquals(1803, SearchLink.firstYear(italian().with(Placeholder.BIRTH_YEAR_FROM, "1803")));
		Assertions.assertEquals(1805 + SearchLink.LONGEST_LIFE, SearchLink.lastYear(italian()));
		final SearchSubject onlyDeath = SearchSubject.EMPTY.with(Placeholder.DEATH_YEAR, "1870");
		Assertions.assertEquals(1870 - SearchLink.LONGEST_LIFE, SearchLink.firstYear(onlyDeath));
		Assertions.assertEquals(1870, SearchLink.lastYear(onlyDeath));
		Assertions.assertNull(SearchLink.firstYear(SearchSubject.EMPTY));
	}

	private static SearchLink link(final List<SearchLink> links, final String id){
		return links.stream().filter(link -> link.site().id().equals(id)).findFirst().orElseThrow();
	}

	private static SearchLink.Status status(final List<SearchLink> links, final String id){
		return link(links, id).status();
	}

}
