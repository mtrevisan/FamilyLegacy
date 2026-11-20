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

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.LivingPrivacy;
import io.github.mtrevisan.familylegacy.privacy.PrivacyPolicy;
import io.github.mtrevisan.familylegacy.reports.ConflictConfidence;
import io.github.mtrevisan.familylegacy.reports.ConflictFilter;
import io.github.mtrevisan.familylegacy.reports.ConflictRecordType;
import io.github.mtrevisan.familylegacy.reports.HypothesisFilter;
import io.github.mtrevisan.familylegacy.reports.HypothesisRecordType;
import io.github.mtrevisan.familylegacy.reports.GivenNameStyle;
import io.github.mtrevisan.familylegacy.reports.MediaPlacement;
import io.github.mtrevisan.familylegacy.reports.NumberingSystem;
import io.github.mtrevisan.familylegacy.reports.OptionState;
import io.github.mtrevisan.familylegacy.reports.PersonOrder;
import io.github.mtrevisan.familylegacy.reports.GenerationOrder;
import io.github.mtrevisan.familylegacy.reports.PortraitMode;
import io.github.mtrevisan.reports.ImageAlignment;
import io.github.mtrevisan.familylegacy.reports.ReportBuilder;
import io.github.mtrevisan.familylegacy.reports.ReportConfig;
import io.github.mtrevisan.familylegacy.reports.ReportOption;
import io.github.mtrevisan.familylegacy.reports.ReportProgressListener;
import io.github.mtrevisan.familylegacy.reports.ReportType;
import io.github.mtrevisan.familylegacy.reports.TextPlacement;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportDateFormat;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import io.github.mtrevisan.familylegacy.reports.index.CollateralScope;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
 * The dialog of a report shows only the options that the report uses. What it hides must really change nothing, and what it forces
 * (an option always on) must not be changeable: otherwise the dialog would lie to the user.
 */
class ReportOptionStateTest{

	private static FLEFModel model;


	@BeforeAll
	static void load() throws IOException{
		model = new FLEFParser().parse(Path.of("src", "main", "resources", "tests", "test.flef"));
	}


	/** @return	The configuration with the option set to the value (the widest scope of the collaterals, two generations, or none). */
	private static ReportConfig with(final ReportConfig config, final ReportOption option, final boolean value){
		final CollateralScope[] scopes = CollateralScope.values();
		return config.with(option, switch(option){
			case COLLATERAL_SCOPE -> (value? scopes[scopes.length - 1]: CollateralScope.NONE);
			case GENERATIONS -> (value? 2: 0);
			//(a report that starts from a later generation than the ones the subject has would have no persons)
			case FIRST_GENERATION -> 0;
			case GENERATION_ORDER -> (value? GenerationOrder.FARTHEST_FIRST: GenerationOrder.NEAREST_FIRST);
			case PORTRAIT_ALIGNMENT -> (value? ImageAlignment.WRAP_RIGHT: ImageAlignment.DEFAULT);
			case PERSON_ORDER -> (value? PersonOrder.BIRTH_DATE: PersonOrder.ALPHABETICAL);
			case DATE_FORMAT -> (value? ReportDateFormat.DAY_MONTH_YEAR: ReportDateFormat.AS_RECORDED);
			case NUMBERING -> (value? NumberingSystem.AUTO: NumberingSystem.NONE);
			case GIVEN_NAMES -> (value? GivenNameStyle.FIRST_AND_INITIALS: GivenNameStyle.ALL);
			case UNKNOWN_NAME -> (value? "?": "");
			case NOTES_PLACEMENT, SOURCES_PLACEMENT -> (value? TextPlacement.SECTION: TextPlacement.INLINE);
			case LONG_TEXT_LINES -> (value? 1: 0);
			case MEDIA_PLACEMENT -> (value? MediaPlacement.INLINE: MediaPlacement.SECTION);
			case IMAGES_PER_PAGE, MAX_IMAGE_MEGAPIXELS -> (value? 2: 0);
			case THUMBNAIL_SIZE -> (value? 200: 0);
			case PORTRAIT_MODE -> (value? PortraitMode.CROP_WITHOUT_ORIGINAL: PortraitMode.WHOLE);
			case CONFLICT_FILTER -> (value? ConflictFilter.UNRESOLVED: ConflictFilter.ALL);
			case CONFLICT_CONFIDENCE -> (value? ConflictConfidence.LOW: ConflictConfidence.ALL);
			case CONFLICT_RECORD_TYPE -> (value? ConflictRecordType.EVENT: ConflictRecordType.ALL);
			case HYPOTHESIS_FILTER -> (value? HypothesisFilter.OPEN: HypothesisFilter.ALL);
			case HYPOTHESIS_RECORD_TYPE -> (value? HypothesisRecordType.INDIVIDUAL: HypothesisRecordType.ALL);
			default -> value;
		});
	}

	private static String text(final ReportDocument document){
		return document.title() + document.sections();
	}

	/** The records that can be the subject of a report of the type (the dialog takes a selected one). */
	private static List<FLEFRecord> subjects(final ReportType type){
		return switch(type){
			case GROUP -> model.getRecordsByType(GroupReader.TYPE);
			case PLACE -> model.getRecordsByType(PlaceReader.TYPE);
			case SOURCE -> model.getRecordsByType(SourceReader.TYPE);
			case RESEARCH_QUESTION -> model.getRecordsByType(ResearchQuestionReader.TYPE);
			//(about the whole file: any record will do)
			case FULL_BIBLIOGRAPHY, FAMILY_BOOK, TO_VERIFY, CONFLICTS, SURNAMES, IDENTITY_HYPOTHESES -> model.getRecords().subList(0, 1);
			case BIBLIOGRAPHY, RESEARCH_PROGRESS -> model.getRecords();
			default -> model.getRecordsByType(IndividualReader.TYPE);
		};
	}

	private static List<String> headings(final ReportType type, final String id, final ReportConfig config){
		return ReportBuilder.build(type, model, model.getRecordById(id), config, ReportProgressListener.NOOP).sections().stream()
			.filter(ReportSection.Heading.class::isInstance)
			.map(section -> ((ReportSection.Heading)section).text())
			.toList();
	}


	@Test
	void anOptionThatIsNotShownChangesNothing(){
		final List<String> lies = new ArrayList<>();
		final ReportConfig on = ReportConfig.uniform(true, ReportLanguage.ENGLISH, PrivacyPolicy.SHOW_ALL);
		final ReportConfig off = ReportConfig.uniform(false, ReportLanguage.ENGLISH, PrivacyPolicy.SHOW_ALL);

		for(final ReportType type : ReportType.values())
			for(final FLEFRecord root : subjects(type))
				for(final ReportOption option : ReportOption.values()){
					if(ReportBuilder.optionState(type, root, option) == OptionState.EDITABLE)
						continue;

					for(final ReportConfig base : List.of(on, off)){
						final String before = text(ReportBuilder.build(type, model, root, with(base, option, true),
							ReportProgressListener.NOOP));
						final String after = text(ReportBuilder.build(type, model, root, with(base, option, false),
							ReportProgressListener.NOOP));
						if(!before.equals(after))
							lies.add(type + " of " + root.getId() + ": " + option + " is " + ReportBuilder.optionState(type, root, option)
								+ " but changes the report");
					}
				}

		Assertions.assertEquals(List.of(), lies);
	}

	@Test
	void theRelationshipReportAlwaysHasTheRelationsAndNeverTheLines(){
		final FLEFRecord root = model.getRecordById("I1");

		Assertions.assertEquals(OptionState.ALWAYS_ON, ReportBuilder.optionState(ReportType.RELATIONSHIP, root, ReportOption.DIRECT_RELATIONS));
		Assertions.assertEquals(OptionState.ALWAYS_ON, ReportBuilder.optionState(ReportType.RELATIONSHIP, root, ReportOption.INDIRECT_RELATIONS));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.RELATIONSHIP, root, ReportOption.PATERNAL_ANCESTRY));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.RELATIONSHIP, root, ReportOption.DESCENDANTS));
		Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(ReportType.RELATIONSHIP, root, ReportOption.INTRODUCTION));
	}

	@Test
	void theIndividualSummaryLetsTheUserChooseTheNarrativeButNotTheGroupSheet(){
		final FLEFRecord root = model.getRecordById("I1");

		for(final ReportOption option : List.of(ReportOption.INTRODUCTION, ReportOption.PATERNAL_ANCESTRY, ReportOption.DESCENDANTS,
				ReportOption.DIRECT_RELATIONS, ReportOption.TIMELINE, ReportOption.COVERAGE_REPORT, ReportOption.INDEX_PLACES))
			Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(ReportType.INDIVIDUAL, root, option), option.name());
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.INDIVIDUAL, root, ReportOption.GROUP_MEMBERS));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.INDIVIDUAL, root, ReportOption.COLLATERAL_SCOPE));
	}

	@Test
	void theGroupSheetHasNoAncestryAndTheGroupOptions(){
		final FLEFRecord group = model.getRecordById("G1");

		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.GROUP, group, ReportOption.PATERNAL_ANCESTRY));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.GROUP, group, ReportOption.DIRECT_RELATIONS));
		Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(ReportType.GROUP, group, ReportOption.GROUP_MEMBERS));
	}

	@Test
	void theAncestorAndDescendantReportsHaveTheirOwnOptions(){
		final FLEFRecord root = model.getRecordById("I1");

		for(final ReportType type : List.of(ReportType.ANCESTOR, ReportType.DESCENDANT)){
			Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(type, root, ReportOption.COLLATERAL_SCOPE), type.name());
			Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(type, root, ReportOption.SOURCES), type.name());
			Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(type, root, ReportOption.INDEX_PLACES), type.name());
			Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(type, root, ReportOption.INDEX_INDIVIDUALS), type.name());
			Assertions.assertEquals(OptionState.ALWAYS_ON, ReportBuilder.optionState(type, root, ReportOption.INTRODUCTION), type.name());
			Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(type, root, ReportOption.TIMELINE), type.name());
			Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(type, root, ReportOption.COVERAGE_REPORT), type.name());
			Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(type, root, ReportOption.DESCENDANTS), type.name());
		}
	}

	@Test
	void theBibliographyAlwaysHasTheSourcesAndTheResearchReportTheResearch(){
		final FLEFRecord root = model.getRecordById("I1");

		Assertions.assertEquals(OptionState.ALWAYS_ON, ReportBuilder.optionState(ReportType.BIBLIOGRAPHY, root, ReportOption.SOURCES));
		Assertions.assertEquals(OptionState.ALWAYS_ON, ReportBuilder.optionState(ReportType.BIBLIOGRAPHY, root, ReportOption.REPOSITORIES));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.BIBLIOGRAPHY, root, ReportOption.TIMELINE));
		Assertions.assertEquals(OptionState.ALWAYS_ON, ReportBuilder.optionState(ReportType.RESEARCH_PROGRESS, root, ReportOption.COVERAGE_REPORT));
		Assertions.assertEquals(OptionState.ALWAYS_ON, ReportBuilder.optionState(ReportType.RESEARCH_PROGRESS, root, ReportOption.CONTEXT_AND_RESEARCH));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.RESEARCH_PROGRESS, root, ReportOption.MEDIA));
	}

	@Test
	void aReportOnASourceOrAPlaceHasNeitherPersonNorGroupOptions(){
		final FLEFRecord source = model.getRecordById("S1");

		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.RESEARCH_PROGRESS, source, ReportOption.INTRODUCTION));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.RESEARCH_PROGRESS, source, ReportOption.GROUP_MEMBERS));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.RESEARCH_PROGRESS, source, ReportOption.DESCENDANTS));
	}


	@Test
	void theAncestorAndDescendantReportsHaveWhatTheirPersonsCite(){
		for(final ReportType type : List.of(ReportType.ANCESTOR, ReportType.DESCENDANT)){
			final List<String> on = headings(type, "I1", ReportConfig.uniform(true, ReportLanguage.ENGLISH, PrivacyPolicy.SHOW_ALL));
			final List<String> off = headings(type, "I1", ReportConfig.uniform(false, ReportLanguage.ENGLISH, PrivacyPolicy.SHOW_ALL));

			for(final String heading : List.of("Sources", "Media", "Index of Places", "Index of Individuals"))
				Assertions.assertTrue(on.contains(heading), type + " with the option on has no " + heading + ": " + on);
			Assertions.assertTrue(on.contains("Places"), type + ": " + on);
			for(final String heading : List.of("Sources", "Media", "Index of Places", "Index of Individuals"))
				Assertions.assertFalse(off.contains(heading), type + " with the option off has " + heading + ": " + off);
		}
	}

	@Test
	void theSourcesOfAnAncestorReportAreThoseOfAllThePersons(){
		final ReportConfig config = ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW);
		final long ofTheRoot = headings(ReportType.INDIVIDUAL, "I1", config).stream().filter("src title [S1]"::equals).count();
		final long ofTheLine = headings(ReportType.ANCESTOR, "I1", config).stream().filter("src title [S1]"::equals).count();

		Assertions.assertEquals(1, ofTheRoot);
		Assertions.assertEquals(1, ofTheLine);
	}

	@Test
	void theReportsOnAPlaceASourceOrAQuestionHaveNeitherPersonNorGroupOptions(){
		for(final ReportType type : List.of(ReportType.PLACE, ReportType.SOURCE, ReportType.RESEARCH_QUESTION)){
			final FLEFRecord root = subjects(type).getFirst();

			for(final ReportOption option : List.of(ReportOption.INTRODUCTION, ReportOption.PATERNAL_ANCESTRY, ReportOption.DESCENDANTS,
					ReportOption.DIRECT_RELATIONS, ReportOption.GROUP_MEMBERS, ReportOption.COLLATERAL_SCOPE, ReportOption.GENERATIONS,
					ReportOption.FIRST_GENERATION, ReportOption.GENERATION_ORDER))
				Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(type, root, option), type + StringUtils.SPACE + option);
			for(final ReportOption option : List.of(ReportOption.NOTES, ReportOption.SOURCES, ReportOption.MEDIA, ReportOption.INDEX_PLACES))
				Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(type, root, option), type + StringUtils.SPACE + option);
		}
	}

}
