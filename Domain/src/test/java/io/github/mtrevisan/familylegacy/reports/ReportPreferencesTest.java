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

import io.github.mtrevisan.familylegacy.privacy.PrivacyPolicy;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import io.github.mtrevisan.familylegacy.reports.index.CollateralScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;


/** What the user chose for a report is offered again, and a damaged preference never breaks a report. */
class ReportPreferencesTest{

	/** The languages with a bundle, as if their files were there. */
	private static final List<ReportLanguage> AVAILABLE = List.of(ReportLanguage.ENGLISH, ReportLanguage.of(Locale.forLanguageTag("en-GB")),
		ReportLanguage.of(Locale.forLanguageTag("en-US")), ReportLanguage.of(Locale.forLanguageTag("it")),
		ReportLanguage.of(Locale.forLanguageTag("de")));

	private final Map<String, String> values = new HashMap<>();
	private String interfaceLanguage = "it";


	private ReportPreferences preferences(){
		return new ReportPreferences(new ReportPreferences.Store(){
			@Override
			public String get(final String key){
				return values.get(key);
			}

			@Override
			public void put(final String key, final String value){
				if(value == null || value.isBlank())
					values.remove(key);
				else
					values.put(key, value);
			}
		}, () -> interfaceLanguage, () -> AVAILABLE);
	}


	@Test
	void theLanguageIsTheOneOfTheInterfaceUntilTheUserChoosesAnother(){
		final ReportPreferences preferences = preferences();

		Assertions.assertEquals(ReportLanguage.of(Locale.forLanguageTag("it")), preferences.language());
		Assertions.assertEquals(ReportLanguage.of(Locale.forLanguageTag("it")), preferences.defaultLanguage());

		preferences.setLanguage(ReportLanguage.of(Locale.forLanguageTag("de")));

		Assertions.assertEquals(ReportLanguage.of(Locale.forLanguageTag("de")), preferences().language());
		Assertions.assertEquals(ReportLanguage.of(Locale.forLanguageTag("it")), preferences().defaultLanguage());
	}

	@Test
	void theFontOfThePdfIsRememberedAndAutomaticByDefault(){
		Assertions.assertNull(preferences().font());

		preferences().setFont("Segoe UI");
		Assertions.assertEquals("Segoe UI", preferences().font());

		//back to the automatic one
		preferences().setFont(null);
		Assertions.assertNull(preferences().font());
	}

	@Test
	void anUnknownLanguageFallsBackToTheInterfaceOne(){
		values.put("reports.language", "KLINGON");

		Assertions.assertEquals(ReportLanguage.of(Locale.forLanguageTag("it")), preferences().language());
	}

	@Test
	void theLanguageOfAnInterfaceTagIsFound(){
		Assertions.assertEquals(ReportLanguage.ENGLISH, ReportLanguage.forLanguageTag("en", AVAILABLE));
		Assertions.assertEquals(ReportLanguage.of(Locale.forLanguageTag("en-US")), ReportLanguage.forLanguageTag("en-US", AVAILABLE));
		Assertions.assertEquals(ReportLanguage.of(Locale.forLanguageTag("en-GB")), ReportLanguage.forLanguageTag("en-GB", AVAILABLE));
		Assertions.assertEquals(ReportLanguage.of(Locale.forLanguageTag("it")), ReportLanguage.forLanguageTag("it", AVAILABLE));
		Assertions.assertEquals(ReportLanguage.of(Locale.forLanguageTag("it")), ReportLanguage.forLanguageTag("it-CH", AVAILABLE));
		Assertions.assertEquals(ReportLanguage.ENGLISH, ReportLanguage.forLanguageTag("tlh", AVAILABLE));
		Assertions.assertEquals(ReportLanguage.ENGLISH, ReportLanguage.forLanguageTag("", AVAILABLE));
		Assertions.assertEquals(ReportLanguage.ENGLISH, ReportLanguage.forLanguageTag(null, AVAILABLE));
	}

	@Test
	void everyInterfaceLanguageOfTheApplicationHasAReportLanguage(){
		for(final ReportLanguage language : ReportLanguage.available())
			Assertions.assertEquals(language.locale().getLanguage(), ReportLanguage.forLanguageTag(language.locale().toLanguageTag(), AVAILABLE)
				.locale().getLanguage(), language.toString());
	}

	@Test
	void theFormatsAndTheFolderAreRemembered(){
		final ReportPreferences preferences = preferences();
		Assertions.assertEquals(List.of(), preferences.formats());
		Assertions.assertNull(preferences.directory());

		preferences.setFormats(List.of("markdown", "pdf"));
		preferences.setDirectory("C:\\reports");

		Assertions.assertEquals(List.of("markdown", "pdf"), preferences().formats());
		Assertions.assertEquals("C:\\reports", preferences().directory());
	}

	@Test
	void theOptionsAreRememberedForEachTypeOfReport(){
		final ReportPreferences preferences = preferences();
		final ReportConfig defaults = ReportConfig.defaults();
		final ReportConfig chosen = defaults.with(ReportOption.TIMELINE, true)
			.with(ReportOption.INDEX_PLACES, false)
			.with(ReportOption.COLLATERAL_SCOPE, CollateralScope.COUSINS)
			.with(ReportOption.GENERATIONS, 4)
			.with(ReportOption.PERSON_ORDER, PersonOrder.BIRTH_DATE);

		preferences.setOptions(ReportType.ANCESTOR, chosen, List.of(ReportOption.TIMELINE, ReportOption.INDEX_PLACES,
			ReportOption.COLLATERAL_SCOPE, ReportOption.GENERATIONS, ReportOption.PERSON_ORDER));

		final ReportConfig ancestors = preferences().options(ReportType.ANCESTOR, defaults);
		Assertions.assertTrue(ancestors.timeline());
		Assertions.assertFalse(ancestors.indexPlaces());
		Assertions.assertEquals(CollateralScope.COUSINS, ancestors.collateralScope());
		Assertions.assertEquals(4, ancestors.generations());
		Assertions.assertEquals(PersonOrder.BIRTH_DATE, ancestors.personOrder());
		//another report does not see them
		Assertions.assertEquals(defaults, preferences().options(ReportType.DESCENDANT, defaults));
	}

	@Test
	void onlyTheOptionsGivenAreRemembered(){
		final ReportPreferences preferences = preferences();
		final ReportConfig defaults = ReportConfig.defaults();

		preferences.setOptions(ReportType.INDIVIDUAL, defaults.with(ReportOption.TIMELINE, true).with(ReportOption.NOTES, false),
			List.of(ReportOption.TIMELINE));

		final ReportConfig read = preferences().options(ReportType.INDIVIDUAL, defaults);
		Assertions.assertTrue(read.timeline());
		Assertions.assertEquals(defaults.notes(), read.notes());
	}

	@Test
	void thePrivacyPolicyAndTheLanguageAreNeverTakenFromTheOptions(){
		final ReportPreferences preferences = preferences();
		final ReportConfig everything = ReportConfig.uniform(true, ReportLanguage.of(Locale.forLanguageTag("de")), PrivacyPolicy.SHOW_ALL);

		preferences.setOptions(ReportType.INDIVIDUAL, everything, List.of(ReportOption.values()));

		final ReportConfig read = preferences().options(ReportType.INDIVIDUAL, ReportConfig.defaults());
		Assertions.assertEquals(ReportConfig.defaults().privacyPolicy(), read.privacyPolicy());
		Assertions.assertEquals(ReportConfig.defaults().language(), read.language());
		Assertions.assertTrue(read.timeline());
	}

	@Test
	void aDamagedPreferenceIsIgnored(){
		values.put("reports.options.individual", "TIMELINE=maybe;NOTES;=true;NO_SUCH_OPTION=true;GENERATIONS=many;COLLATERAL_SCOPE=everyone;"
			+ "INDEX_PLACES=false");
		final ReportConfig defaults = ReportConfig.defaults();

		final ReportConfig read = preferences().options(ReportType.INDIVIDUAL, defaults);

		Assertions.assertEquals(defaults.timeline(), read.timeline());
		Assertions.assertEquals(defaults.generations(), read.generations());
		Assertions.assertEquals(defaults.collateralScope(), read.collateralScope());
		Assertions.assertFalse(read.indexPlaces());
	}

	@Test
	void aNegativeNumberOfGenerationsMeansAll(){
		Assertions.assertEquals(0, ReportConfig.defaults().with(ReportOption.GENERATIONS, -3).generations());
	}

}
