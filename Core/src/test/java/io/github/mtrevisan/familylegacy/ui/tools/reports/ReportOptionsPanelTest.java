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
package io.github.mtrevisan.familylegacy.ui.tools.reports;

import io.github.mtrevisan.familylegacy.ui.tools.reports.i18n.ReportLanguage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Each check box of the options panel must reach the option that carries its name: the options are booleans, so a wrong order of the
 * arguments of {@link ReportConfig} compiles and silently swaps the options.
 */
class ReportOptionsPanelTest{

	private static ReportConfig everyOptionFlipped(final ReportConfig defaults){
		return new ReportConfig(
			defaults.language(),
			defaults.privacyPolicy(),
			!defaults.showPrivacyDetails(),
			!defaults.audit(),
			!defaults.header(),
			!defaults.introduction(),
			!defaults.paternalAncestry(),
			!defaults.maternalAncestry(),
			!defaults.descendants(),
			!defaults.directRelations(),
			!defaults.indirectRelations(),
			defaults.collateralScope(),
			!defaults.notes(),
			!defaults.sources(),
			!defaults.media(),
			!defaults.evidence(),
			!defaults.contextAndResearch(),
			!defaults.culturalNorms(),
			!defaults.groups(),
			!defaults.groupMembers(),
			!defaults.groupSubgroups(),
			!defaults.groupAttributes(),
			!defaults.historicEvents(),
			!defaults.repositories(),
			!defaults.eventFullParticipants(),
			!defaults.coverageReport(),
			!defaults.timeline(),
			!defaults.indexIndividuals(),
			!defaults.indexPlaces(),
			!defaults.includeDescriptions(),
			!defaults.includePictures());
	}


	@Test
	void theDefaultsSurviveTheRoundTripThroughThePanel(){
		final ReportConfig defaults = ReportConfig.defaults();

		final ReportConfig read = new ReportOptionsPanel(defaults).toConfig(ReportLanguage.ENGLISH);

		Assertions.assertEquals(defaults, read);
	}

	@Test
	void everyOptionReachesItsOwnField(){
		final ReportConfig flipped = everyOptionFlipped(ReportConfig.defaults());

		final ReportConfig read = new ReportOptionsPanel(flipped).toConfig(ReportLanguage.ENGLISH);

		Assertions.assertEquals(flipped, read);
	}

	@Test
	void theChronologicalTimelineIsOffAndThePicturesAreOnByDefault(){
		final ReportConfig defaults = ReportConfig.defaults();

		Assertions.assertFalse(defaults.timeline());
		Assertions.assertTrue(defaults.includePictures());
		Assertions.assertTrue(defaults.indexIndividuals());
		Assertions.assertTrue(defaults.indexPlaces());
	}

}
