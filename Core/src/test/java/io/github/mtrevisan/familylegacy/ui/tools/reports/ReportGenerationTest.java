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
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.PrivacyPolicy;
import io.github.mtrevisan.familylegacy.privacy.LivingPrivacy;
import io.github.mtrevisan.familylegacy.reports.ReportBuilder;
import io.github.mtrevisan.familylegacy.reports.ReportConfig;
import io.github.mtrevisan.familylegacy.reports.ReportProgressListener;
import io.github.mtrevisan.familylegacy.reports.ReportType;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import io.github.mtrevisan.familylegacy.reports.renderers.ReportRenderers;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportRenderer;
import io.github.mtrevisan.reports.ReportSection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Every report of the Reports menu is generated on the sample file (which has several names per individual, groups, sources, research
 * data, places), with any record as the subject, with the options all on and all off, and written in every format. The menu may
 * disable a command, but nothing must fail when it is run anyway.
 */
class ReportGenerationTest{

	private static FLEFModel model;


	@BeforeAll
	static void load() throws IOException{
		model = new FLEFParser().parse(Path.of("src", "main", "resources", "tests", "test.flef"));
	}


	private static ReportConfig config(final boolean value, final ReportLanguage language, final PrivacyPolicy privacy){
		return ReportConfig.uniform(value, language, privacy);
	}

	private static ReportDocument build(final ReportType type, final String id, final ReportConfig config){
		return ReportBuilder.build(type, model, model.getRecordById(id), config, ReportProgressListener.NOOP);
	}

	/** A record the report of the type can be about. */
	private static String rootOf(final ReportType type){
		return switch(type){
			case GROUP -> "G1";
			case PLACE -> "P1";
			case SOURCE -> "S1";
			case RESEARCH_QUESTION -> "RQ1";
			default -> "I1";
		};
	}

	private static List<String> headings(final ReportDocument document){
		return document.sections().stream()
			.filter(ReportSection.Heading.class::isInstance)
			.map(section -> ((ReportSection.Heading)section).text())
			.toList();
	}


	@Test
	void everyReportIsGeneratedForEveryRecordWithAnyOptions(){
		final Map<String, ReportConfig> configs = Map.of("defaults", ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW),
			"all on", config(true, ReportLanguage.ENGLISH, PrivacyPolicy.SHOW_ALL),
			"all off", config(false, ReportLanguage.ENGLISH, PrivacyPolicy.HIDE_CONFIDENTIAL));
		final List<String> failures = new ArrayList<>();

		for(final FLEFRecord root : model.getRecords())
			for(final ReportType type : ReportType.values())
				for(final Map.Entry<String, ReportConfig> entry : configs.entrySet())
					try{
						Assertions.assertFalse(build(type, root.getId(), entry.getValue()).sections().isEmpty());
					}
					catch(final RuntimeException e){
						failures.add(type + " of " + root.getId() + " (" + entry.getKey() + "): " + e);
					}

		Assertions.assertEquals(List.of(), failures);
	}

	@Test
	void everyPrivacyPolicyIsGenerated(){
		for(final PrivacyPolicy policy : PrivacyPolicy.values())
			Assertions.assertDoesNotThrow(() -> build(ReportType.INDIVIDUAL, "I1", config(true, ReportLanguage.ENGLISH, policy)), policy.name());
	}

	@Test
	void everyLanguageIsGenerated(){
		for(final ReportLanguage language : ReportLanguage.available())
			Assertions.assertDoesNotThrow(() -> build(ReportType.INDIVIDUAL, "I1", config(true, language, PrivacyPolicy.SHOW_ALL)),
				language.toString());
	}

	@Test
	void everyReportIsWrittenInEveryFormat(@TempDir final Path directory) throws IOException{
		for(final ReportType type : ReportType.values()){
			final ReportDocument document = build(type, rootOf(type), ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW));

			for(final ReportRenderer renderer : ReportRenderers.available()){
				final Path target = directory.resolve(type.name() + renderer.extension());

				renderer.render(document, target);

				final long size = (Files.isDirectory(target)? Files.list(target).count(): Files.size(target));
				Assertions.assertTrue(size > 0, type + " as " + renderer.id());
			}
		}
	}


	@Test
	void theIndividualSummaryTellsTheLifeOfTheIndividual(){
		final List<String> headings = headings(build(ReportType.INDIVIDUAL, "I1", ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW)));

		Assertions.assertTrue(headings.contains("Introduction"), headings.toString());
		Assertions.assertTrue(headings.stream().anyMatch(heading -> heading.startsWith("Life of")), headings.toString());
		Assertions.assertTrue(headings.contains("Paternal Ancestry"), headings.toString());
		Assertions.assertTrue(headings.contains("Descendants"), headings.toString());
		Assertions.assertFalse(headings.contains("Direct Relations"), headings.toString());
	}

	@Test
	void theRelationshipReportHasTheRelationsAndNotTheLines(){
		final List<String> headings = headings(build(ReportType.RELATIONSHIP, "I1", ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW)));

		Assertions.assertTrue(headings.contains("Direct Relations"), headings.toString());
		Assertions.assertTrue(headings.contains("Indirect Relations"), headings.toString());
		Assertions.assertFalse(headings.contains("Paternal Ancestry"), headings.toString());
		Assertions.assertFalse(headings.contains("Descendants"), headings.toString());
	}

	@Test
	void theAncestorAndDescendantReportsHaveTheirOwnTitle(){
		Assertions.assertTrue(build(ReportType.ANCESTOR, "I1", ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW)).title().startsWith("Ancestors of"));
		Assertions.assertTrue(build(ReportType.DESCENDANT, "I1", ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW)).title().startsWith("Descendants of"));
	}

	@Test
	void theGroupSheetIsAboutTheGroup(){
		final List<String> headings = headings(build(ReportType.GROUP, "G1", ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW)));

		Assertions.assertTrue(headings.contains("Members"), headings.toString());
	}

	@Test
	void theBibliographyListsTheSourcesAndTheResearchReportTheResearch(){
		final List<String> bibliography = headings(build(ReportType.BIBLIOGRAPHY, "I1", ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW)));
		final List<String> research = headings(build(ReportType.RESEARCH_PROGRESS, "I1", ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW)));

		Assertions.assertTrue(bibliography.contains("Sources"), bibliography.toString());
		Assertions.assertTrue(research.contains("Research"), research.toString());
		Assertions.assertTrue(research.contains("Coverage report"), research.toString());
	}

	@Test
	void thePlaceReportIsAboutThePlace(){
		final ReportDocument document = build(ReportType.PLACE, "P1", ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW));
		final List<String> headings = headings(document);

		Assertions.assertTrue(headings.stream().anyMatch(heading -> heading.startsWith("Place: a certain place")), headings.toString());
		Assertions.assertTrue(headings.contains("Historical jurisdictions"), headings.toString());
		Assertions.assertTrue(headings.contains("Contained places"), headings.toString());
		Assertions.assertTrue(headings.contains("Events at this place"), headings.toString());
		Assertions.assertFalse(headings.contains("Introduction"), headings.toString());
	}

	@Test
	void theSourceReportIsAboutTheSource(){
		final List<String> headings = headings(build(ReportType.SOURCE, "S1", ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW)));

		Assertions.assertTrue(headings.stream().anyMatch(heading -> heading.startsWith("Source: src title")), headings.toString());
		Assertions.assertTrue(headings.contains("Repository"), headings.toString());
		Assertions.assertTrue(headings.contains("Cited by"), headings.toString());
	}

	@Test
	void theResearchQuestionReportIsAboutTheQuestion(){
		final List<String> headings = headings(build(ReportType.RESEARCH_QUESTION, "RQ1", ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW)));

		Assertions.assertTrue(headings.stream().anyMatch(heading -> heading.startsWith("Research question: rq title")), headings.toString());
		for(final String heading : List.of("Targets", "Activities", "Tasks", "Conclusions"))
			Assertions.assertTrue(headings.contains(heading), heading + " in " + headings);
	}

}
