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

import java.util.List;
import java.util.Optional;
import java.util.Set;


/**
 * The sites that come with the application, with the address of their public search page (no API: the search is opened in the
 * browser, where the user may be logged in).
 * <p>
 * The credentials declared here are not used by the addresses: they are the keys of the APIs of the sites (for a later search inside
 * the application), which the user can already keep with the site.
 */
public final class BuiltInSites{

	private static final String API_KEY = "api_key";
	private static final String APP_KEY = "app_key";
	private static final String APP_ID = "app_id";

	private record Definition(String id, String name, String template, SiteKind kind, Set<String> countries, Integer from, Integer to,
		List<String> credentials, boolean loginRequired){}


	/* The parameters were checked on 2026-11-17 against the live pages, the documentation of the sites, or code that is maintained and
	 * builds the same addresses (WikiTree Sourcer, the WebSearch gramplet of Gramps). Not every value a site can take is used: a filter
	 * on a value that is empty, or that the site applies to the date of the item and not to the person, hides results instead of
	 * finding them. */
	private static final List<Definition> DEFINITIONS = List.of(
		//the results need a free account
		new Definition("familysearch-records", "FamilySearch (records)",
			"https://www.familysearch.org/search/record/results?q.givenName={name_given}&q.surname={surname}"
				+ "&q.birthLikeDate.from={birth_year_from}&q.birthLikeDate.to={birth_year_to}&q.birthLikePlace={birth_place}"
				+ "&q.deathLikeDate.from={death_year_from}&q.deathLikeDate.to={death_year_to}&q.deathLikePlace={death_place}"
				+ "&q.fatherGivenName={father_given}&q.fatherSurname={father_surname}&q.motherGivenName={mother_given}"
				+ "&q.motherSurname={mother_surname}&q.spouseGivenName={spouse_given}&q.spouseSurname={spouse_surname}",
			SiteKind.RECORDS, Set.of(), null, null, List.of(APP_KEY), true),
		new Definition("familysearch-tree", "FamilySearch (Family Tree)",
			"https://www.familysearch.org/search/tree/results?q.givenName={name_given}&q.surname={surname}"
				+ "&q.birthLikeDate.from={birth_year_from}&q.birthLikeDate.to={birth_year_to}&q.birthLikePlace={birth_place}"
				+ "&q.deathLikeDate.from={death_year_from}&q.deathLikeDate.to={death_year_to}&q.deathLikePlace={death_place}",
			SiteKind.FAMILY_TREES, Set.of(), null, null, List.of(APP_KEY), true),
		//"_" separates the given names from the surname, and the year from the place
		new Definition("ancestry", "Ancestry",
			"https://www.ancestry.com/search/?name={name_given}_{surname}&birth={birth_year}_{birth_place}&death={death_year}",
			SiteKind.RECORDS, Set.of(), null, null, List.of(), false),
		//a space inside a value should be %2F3 for this site: a name of several words is searched as written, less precisely
		new Definition("myheritage", "MyHeritage",
			"https://www.myheritage.com/research?s=1&formId=master&formMode=1&useTranslation=1&exactSearch=&p=1&action=query"
				+ "&view_mode=card&qname=Name+fn.{name_given}+fnmo.1+ln.{surname}+lnmsrs.false",
			SiteKind.RECORDS, Set.of(), null, null, List.of(), false),
		//one period only: the birth
		new Definition("geneanet", "Geneanet",
			"https://www.geneanet.org/fonds/individus/?go=1&nom={surname}&prenom={name_given}&place__0__={birth_place}"
				+ "&type_periode=birth_between&from={birth_year_from}&to={birth_year_to}",
			SiteKind.FAMILY_TREES, Set.of(), null, null, List.of(), false),
		//the search page of WikiTree is a form, and WikiTree Plus (which takes an address) blocks whole ranges of addresses of ordinary
		//users: the index of the surname is a plain page of the site
		new Definition("wikitree", "WikiTree",
			"https://www.wikitree.com/genealogy/{surname}",
			SiteKind.FAMILY_TREES, Set.of(), null, null, List.of(APP_ID), false),
		new Definition("openarch", "Open Archives",
			"https://www.openarchieven.nl/search.php?lang=en&name={name_given}%20{surname}",
			SiteKind.RECORDS, Set.of("NL", "BE", "FR", "DE"), null, null, List.of(), false),
		//the name index covers only some archives, and many records have no date
		new Definition("antenati", "Antenati",
			"https://antenati.cultura.gov.it/search-nominative/?cognome={surname}&nome={name_given}"
				+ "&anno_nascita_inizio={birth_year_from}&anno_nascita_fine={birth_year_to}",
			SiteKind.RECORDS, Set.of("IT"), 1800, 1945, List.of(), false),
		new Definition("gedbas", "GEDBAS",
			"https://gedbas.genealogy.net/search/simple?firstname={name_given}&lastname={surname}",
			SiteKind.FAMILY_TREES, Set.of("DE", "AT", "CH", "PL", "CZ"), null, null, List.of(), false),
		new Definition("compgen-meta", "CompGen Metasearch",
			"https://meta.genealogy.net/search?lastname={surname}&placename={birth_place}",
			SiteKind.RECORDS, Set.of("DE", "AT", "CH", "PL", "CZ"), null, null, List.of(), false),
		new Definition("francearchives", "FranceArchives (names)",
			"https://francearchives.gouv.fr/fr/basedenoms?es_names={surname}&es_forenames={name_given}",
			SiteKind.ARCHIVES, Set.of("FR"), null, null, List.of(), false),
		new Definition("pares", "PARES",
			"https://pares.cultura.gob.es/ParesBusquedas20/catalogo/find?nm=&texto={name_given}%20{surname}",
			SiteKind.ARCHIVES, Set.of("ES"), null, null, List.of(), false),
		//the birth year within 3 years either way; the place of a grave is not the place of birth
		new Definition("findagrave", "Find a Grave",
			"https://www.findagrave.com/memorial/search?firstname={name_given}&lastname={surname}&birthyear={birth_year}"
				+ "&birthyearfilter=3&deathyear={death_year}&deathyearfilter=3",
			SiteKind.CEMETERIES, Set.of(), null, null, List.of(), false),
		new Definition("europeana", "Europeana",
			"https://www.europeana.eu/en/search?query=%22{name_given}%20{surname}%22",
			SiteKind.LIBRARIES, Set.of(), null, null, List.of(API_KEY), false),
		new Definition("nara", "National Archives Catalog (USA)",
			"https://catalog.archives.gov/search?q=%22{name_given}%20{surname}%22",
			SiteKind.ARCHIVES, Set.of("US"), null, null, List.of(API_KEY), false),
		new Definition("chronicling-america", "Chronicling America",
			"https://www.loc.gov/collections/chronicling-america/?dl=page&searchType=advanced&ops=PHRASE&qs={name_given}%20{surname}",
			SiteKind.NEWSPAPERS, Set.of("US"), 1756, 1963, List.of(), false),
		new Definition("trove", "Trove",
			"https://trove.nla.gov.au/search/category/newspapers?keyword=%22{name_given}%20{surname}%22",
			SiteKind.NEWSPAPERS, Set.of("AU"), null, null, List.of(API_KEY), false),
		new Definition("internet-archive", "Internet Archive",
			"https://archive.org/search?query=%22{name_given}%20{surname}%22",
			SiteKind.LIBRARIES, Set.of(), null, null, List.of(), false),
		new Definition("dpla", "Digital Public Library of America",
			"https://dp.la/search?q=%22{name_given}%20{surname}%22",
			SiteKind.LIBRARIES, Set.of("US"), null, null, List.of(API_KEY), false)
	);


	private BuiltInSites(){}


	/** @return	The built-in sites, with their description in the language of the user. */
	public static List<SearchSite> all(){
		return DEFINITIONS.stream()
			.map(BuiltInSites::toSite)
			.toList();
	}

	/**
	 * @param id	The identifier of a site.
	 * @return	The built-in site with that identifier, empty if none.
	 */
	public static Optional<SearchSite> find(final String id){
		for(final Definition definition : DEFINITIONS)
			if(definition.id().equals(id))
				return Optional.of(toSite(definition));
		return Optional.empty();
	}

	private static SearchSite toSite(final Definition definition){
		return new SearchSite(definition.id(), definition.name(), definition.template(), definition.kind(),
			new SiteCoverage(definition.countries(), definition.from(), definition.to()), definition.credentials(),
			I18N.t("external_search.site." + definition.id(), ""), definition.loginRequired(), true);
	}

}
