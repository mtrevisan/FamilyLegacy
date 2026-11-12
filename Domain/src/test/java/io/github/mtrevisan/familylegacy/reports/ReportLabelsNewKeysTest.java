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

import io.github.mtrevisan.familylegacy.reports.i18n.ReportLabels;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;


/** The words that were once written in English inside the report sections. */
class ReportLabelsNewKeysTest{

	@Test
	void collateralKinshipIsComposedByTheLanguage(){
		Assertions.assertEquals("uncle of John", new ReportLabels(ReportLanguage.ENGLISH).sections().collateralOf("uncle", "John"));
		Assertions.assertEquals("zio of Gianni", new ReportLabels(ReportLanguage.of(Locale.forLanguageTag("it"))).sections().collateralOf("zio", "Gianni"));
	}

	@Test
	void collateralKinshipUsesBothArgumentsInEveryLanguage(){
		for(final ReportLanguage language : ReportLanguage.available()){
			final String composed = new ReportLabels(language).sections().collateralOf("KINSHIP", "PERSON");

			Assertions.assertTrue(composed.contains("KINSHIP") && composed.contains("PERSON"), language + ": " + composed);
		}
	}

	@Test
	void eventWithoutTypeIsNamedByTheLanguage(){
		final ReportLabels italian = new ReportLabels(ReportLanguage.of(Locale.forLanguageTag("it")));
		Assertions.assertEquals("Event", italian.sections().eventGeneric());
		Assertions.assertEquals("event", italian.sections().eventGenericLower());
	}

	@Test
	void decadeAndCenturyComeFromTheDateLabels(){
		final ReportLabels italian = new ReportLabels(ReportLanguage.of(Locale.forLanguageTag("it")));
		Assertions.assertEquals("15th century", String.format(italian.dates().dateCentury(), "15"));
	}

}
