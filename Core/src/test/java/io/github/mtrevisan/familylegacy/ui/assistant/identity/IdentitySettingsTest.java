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
package io.github.mtrevisan.familylegacy.ui.assistant.identity;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.quality.DuplicateFinderService;
import io.github.mtrevisan.familylegacy.quality.IdentitySettings;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** What the user can decide about the Identity Assistant: the settings are kept in range, and they change what is proposed. */
class IdentitySettingsTest{

	/** Mario twice, with the same name, birth and death (the dates of the second one a year later): 65 points. */
	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Mario /Rossi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1850
		1 DEAT
		2 DATE 1 JAN 1920
		0 @I2@ INDI
		1 NAME Mario /Rossi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1851
		1 DEAT
		2 DATE 1 JAN 1921
		""";


	private static FLEFModel convert() throws IOException{
		return new GEDCOMToFLEFConverter().convert(new GEDCOMParser().parse(new StringReader(GEDCOM)));
	}

	private static List<Diagnostic> found(final FLEFModel model, final IdentitySettings settings){
		return new IdentityAssistantContributor(() -> settings).diagnosticProviders().getFirst()
			.diagnose(new AssistantContext(null, null, model));
	}


	@Test
	void everyValueIsBroughtIntoItsRange(){
		final IdentitySettings low = new IdentitySettings(-5, 0, 0, 0, 0);
		final IdentitySettings high = new IdentitySettings(10_000, 10_000, 10_000, 10_000, 1_000);

		Assertions.assertEquals(IdentitySettings.MIN_THRESHOLD, low.threshold());
		Assertions.assertEquals(IdentitySettings.MIN_MAX_PAIRS, low.maxPairs());
		Assertions.assertEquals(IdentitySettings.MIN_OTHER_PERCENT, low.otherPercent());
		Assertions.assertEquals(IdentitySettings.MAX_THRESHOLD, high.threshold());
		Assertions.assertEquals(IdentitySettings.MAX_MAX_PAIRS, high.maxPairs());
		Assertions.assertEquals(IdentitySettings.MAX_OTHER_PERCENT, high.otherPercent());
	}

	@Test
	void theMediumScoreIsNeverAboveTheHighOne(){
		final IdentitySettings settings = new IdentitySettings(60, 100, 70, 90, 70);

		Assertions.assertEquals(70, settings.highScore());
		Assertions.assertEquals(70, settings.mediumScore());
		Assertions.assertEquals("high", settings.confidence(70));
		Assertions.assertEquals("low", settings.confidence(69));
	}

	@Test
	void theDefaultsAreTheConventionsOfTheAssistant(){
		final IdentitySettings defaults = IdentitySettings.DEFAULTS;

		Assertions.assertEquals(60, defaults.threshold());
		Assertions.assertEquals(100, defaults.maxPairs());
		Assertions.assertEquals("high", defaults.confidence(90));
		Assertions.assertEquals("medium", defaults.confidence(89));
		Assertions.assertEquals("medium", defaults.confidence(70));
		Assertions.assertEquals("low", defaults.confidence(69));
		Assertions.assertEquals(0.7, defaults.otherProbability(), 1e-12);
	}

	@Test
	void theThresholdDecidesWhichPairsAreProposed() throws IOException{
		final FLEFModel model = convert();

		//the pair has 65 points
		Assertions.assertEquals(1, found(model, new IdentitySettings(65, 100, 90, 70, 70)).size());
		Assertions.assertEquals(0, found(model, new IdentitySettings(66, 100, 90, 70, 70)).size());
	}

	@Test
	void theScoresOfTheConfidenceDecideTheSeverityAndTheConfidenceOfTheDraft() throws IOException{
		final FLEFModel model = convert();
		final List<DuplicateFinderService.Candidate> candidates = DuplicateFinderService.find(model, new DuplicateFinderService.Options());

		final IdentityFinding byDefault = IdentityChecker.findings(candidates, IdentitySettings.DEFAULTS).getFirst();
		final IdentityFinding lenient = IdentityChecker.findings(candidates, new IdentitySettings(60, 100, 65, 50, 70)).getFirst();

		//65 points: under the default high score (information, low confidence), at the high score of the lenient user (a warning)
		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, byDefault.diagnostic().severity());
		Assertions.assertEquals("low", byDefault.draft().confidence());
		Assertions.assertEquals(Diagnostic.Severity.WARNING, lenient.diagnostic().severity());
		Assertions.assertEquals("high", lenient.draft().confidence());
	}

}
