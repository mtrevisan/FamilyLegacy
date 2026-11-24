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

import io.github.mtrevisan.familylegacy.privacy.LivingPrivacy;
import io.github.mtrevisan.familylegacy.reports.ConflictConfidence;
import io.github.mtrevisan.familylegacy.reports.ConflictFilter;
import io.github.mtrevisan.familylegacy.reports.ConflictRecordType;
import io.github.mtrevisan.familylegacy.reports.GenerationOrder;
import io.github.mtrevisan.familylegacy.reports.GivenNameStyle;
import io.github.mtrevisan.familylegacy.reports.HypothesisFilter;
import io.github.mtrevisan.familylegacy.reports.HypothesisRecordType;
import io.github.mtrevisan.familylegacy.reports.MediaPlacement;
import io.github.mtrevisan.familylegacy.reports.NumberingSystem;
import io.github.mtrevisan.familylegacy.reports.PersonOrder;
import io.github.mtrevisan.familylegacy.reports.PortraitMode;
import io.github.mtrevisan.familylegacy.reports.ReportConfig;
import io.github.mtrevisan.familylegacy.reports.StoryPlacement;
import io.github.mtrevisan.familylegacy.reports.SurnamePeriod;
import io.github.mtrevisan.familylegacy.reports.SurnamePlaceLevel;
import io.github.mtrevisan.familylegacy.reports.TextPlacement;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportDateFormat;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import io.github.mtrevisan.reports.ImageAlignment;
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
			LivingPrivacy.HIDE,
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
			defaults.generations() + 3,
			defaults.firstGeneration() + 2,
			GenerationOrder.FARTHEST_FIRST,
			PersonOrder.BIRTH_DATE,
			ReportDateFormat.DAY_MONTH_YEAR,
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
			!defaults.includeOriginalSources(),
			!defaults.groupSourcesByKind(),
			!defaults.eventFullParticipants(),
			!defaults.coverageReport(),
			!defaults.timeline(),
			!defaults.indexIndividuals(),
			!defaults.indexPlaces(),
			!defaults.includeDescriptions(),
			!defaults.includePictures(),
			NumberingSystem.HENRY,
			!defaults.boldDirectAncestors(),
			!defaults.crossReferences(),
			GivenNameStyle.FIRST_AND_INITIALS,
			"n/a",
			!defaults.indexYears(),
			TextPlacement.SECTION,
			TextPlacement.LONG_IN_SECTION,
			defaults.longTextLines() + 1,
			!defaults.principalSourcesOnly(),
			!defaults.sourceLocator(),
			MediaPlacement.APPENDIX,
			defaults.imagesPerPage() + 2,
			defaults.maxImageMegapixels() + 3,
			defaults.thumbnailSize() + 250,
			PortraitMode.CROP,
			!defaults.portraitNameTitle(),
			ImageAlignment.WRAP_LEFT,
			ConflictFilter.RESOLVED,
			ConflictConfidence.HIGH,
			ConflictRecordType.NAME,
			HypothesisFilter.REJECTED,
			HypothesisRecordType.PLACE,
			SurnamePlaceLevel.REGION,
			SurnamePeriod.CENTURY,
			!defaults.surnameAllNames(),
			!defaults.surnameSimilar(),
			StoryPlacement.INLINE,
			!defaults.researchNotes());
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
