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

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


class BuiltInSitesTest{

	@Test
	void everyBuiltInSiteHasAValidTemplateAndAUniqueIdentifier() throws Exception{
		final List<SearchSite> sites = BuiltInSites.all();
		Assertions.assertFalse(sites.isEmpty());

		final Set<String> ids = new HashSet<>();
		final SearchSubject subject = SearchSubject.EMPTY
			.with(Placeholder.NAME_GIVEN, "Giovanni Battista")
			.with(Placeholder.SURNAME, "D'Àlvise")
			.with(Placeholder.BIRTH_PLACE, "Sant'Ambrogio di Fiorenza")
			.with(Placeholder.BIRTH_YEAR, "1805");
		for(final SearchSite site : sites){
			Assertions.assertTrue(ids.add(site.id()), "repeated identifier " + site.id());
			Assertions.assertTrue(site.builtIn());
			Assertions.assertFalse(site.description().isBlank(), "no description for " + site.id());
			final UrlTemplate template = site.template();
			Assertions.assertTrue(template.text().startsWith("https://"), site.id() + " is not https");
			Assertions.assertTrue(template.placeholders().stream().anyMatch(Placeholder::isName), site.id() + " does not search a name");
			//the built-in addresses use no credential: the declared ones are for the APIs
			Assertions.assertTrue(template.credentials().isEmpty(), site.id());
			Assertions.assertNotNull(URI.create(template.expand(subject, name -> null)).getHost(), site.id());
		}
	}

	@Test
	void aBuiltInSiteIsFoundByItsIdentifier(){
		Assertions.assertEquals("Antenati", BuiltInSites.find("antenati").orElseThrow().name());
		Assertions.assertTrue(BuiltInSites.find("nothing").isEmpty());
		//FamilySearch shows its results only to a logged-in user, Ancestry shows the list to anybody
		Assertions.assertTrue(BuiltInSites.find("familysearch-records").orElseThrow().loginRequired());
		Assertions.assertFalse(BuiltInSites.find("ancestry").orElseThrow().loginRequired());
	}

}
