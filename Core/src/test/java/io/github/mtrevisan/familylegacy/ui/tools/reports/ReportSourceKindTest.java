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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.LivingPrivacy;
import io.github.mtrevisan.familylegacy.reports.OptionState;
import io.github.mtrevisan.familylegacy.reports.ReportBuilder;
import io.github.mtrevisan.familylegacy.reports.ReportConfig;
import io.github.mtrevisan.familylegacy.reports.ReportOption;
import io.github.mtrevisan.familylegacy.reports.ReportProgressListener;
import io.github.mtrevisan.familylegacy.reports.ReportType;
import io.github.mtrevisan.familylegacy.reports.renderers.ReportRenderers;
import io.github.mtrevisan.reports.ReportDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


/** The sources in the reports can be grouped by their kind: those of the vocabulary in its order, then the custom ones, then the rest. */
class ReportSourceKindTest{

	@TempDir
	Path directory;

	private FLEFModel model;


	/** Anna cites a census, a church register, a source of a kind of the user and a source without a kind. */
	@BeforeEach
	void buildModel(){
		model = new FLEFModel();
		model.addRecord(source("S1", "Alpha census", SourceReader.ENUM_KIND_CENSUS));
		model.addRecord(source("S2", "Beta register", SourceReader.ENUM_KIND_CHURCH_REGISTER));
		model.addRecord(source("S3", "Gamma survey", "land_survey"));
		model.addRecord(source("S4", "Delta unknown", null));
		final FLEFRecord anna = FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, "Anna Rossi")));
		for(final String id : new String[]{"S1", "S2", "S3", "S4"})
			anna.addChild(FLEFRecord.createChildWithTag(SourceCitationReader.TAG_SOURCE)
				.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, id)));
		model.addRecord(anna);
	}

	private static FLEFRecord source(final String id, final String title, final String kind){
		final FLEFRecord source = FLEFRecord.createMainRecord(id, SourceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, title)));
		if(kind != null)
			source.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_KIND, kind));
		return source;
	}

	private String markdown(final ReportType type, final String id, final ReportConfig config) throws IOException{
		final ReportDocument document = ReportBuilder.build(type, model, (id != null? model.getRecordById(id): null), config,
			ReportProgressListener.NOOP);
		final Path target = directory.resolve(type.name() + "-" + id + ".md");
		ReportRenderers.available().stream()
			.filter(renderer -> "markdown".equals(renderer.id()))
			.findFirst()
			.orElseThrow()
			.render(document, target);
		return Files.readString(target, StandardCharsets.UTF_8);
	}

	private static ReportConfig config(final boolean grouped){
		return ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW).with(ReportOption.GROUP_SOURCES_BY_KIND, grouped);
	}


	@Test
	void theKindOfASourceIsShownAndTheListIsAlphabeticalByDefault() throws IOException{
		final String markdown = markdown(ReportType.INDIVIDUAL, "I1", config(false));

		Assertions.assertTrue(markdown.contains("**Kind:**"), markdown);
		Assertions.assertTrue(markdown.indexOf("## Alpha census") < markdown.indexOf("## Beta register"), markdown);
		Assertions.assertFalse(markdown.contains("Sources of unspecified kind"), markdown);
	}

	@Test
	void theSourcesAreGroupedByKindInTheOrderOfTheVocabularyThenTheCustomOnesThenTheRest() throws IOException{
		final String markdown = markdown(ReportType.INDIVIDUAL, "I1", config(true));

		final int churchRegister = markdown.indexOf("## church register");
		final int census = markdown.indexOf("## census");
		final int custom = markdown.indexOf("## land survey");
		final int unspecified = markdown.indexOf("## Sources of unspecified kind");
		Assertions.assertTrue(churchRegister >= 0 && churchRegister < census, markdown);
		Assertions.assertTrue(census < custom && custom < unspecified, markdown);
		//each source is under its kind
		Assertions.assertTrue(churchRegister < markdown.indexOf("### Beta register") && markdown.indexOf("### Beta register") < census, markdown);
		Assertions.assertTrue(unspecified < markdown.indexOf("### Delta unknown"), markdown);
	}

	@Test
	void theFullBibliographyIsGroupedByKindToo() throws IOException{
		final String markdown = markdown(ReportType.FULL_BIBLIOGRAPHY, null, config(true));

		Assertions.assertTrue(markdown.indexOf("## church register") < markdown.indexOf("## census"), markdown);
		Assertions.assertTrue(markdown.indexOf("## census") < markdown.indexOf("### Source: Alpha census"), markdown);
		Assertions.assertTrue(markdown.indexOf("## Sources of unspecified kind") < markdown.indexOf("### Source: Delta unknown"), markdown);
	}

	@Test
	void theOptionIsOfferedOnlyWhereSourcesAreListed(){
		Assertions.assertEquals(OptionState.EDITABLE,
			ReportBuilder.optionState(ReportType.INDIVIDUAL, model.getRecordById("I1"), ReportOption.GROUP_SOURCES_BY_KIND));
		Assertions.assertEquals(OptionState.EDITABLE,
			ReportBuilder.optionState(ReportType.FULL_BIBLIOGRAPHY, null, ReportOption.GROUP_SOURCES_BY_KIND));
		//a source report is about a single source
		Assertions.assertEquals(OptionState.HIDDEN,
			ReportBuilder.optionState(ReportType.SOURCE, model.getRecordById("S1"), ReportOption.GROUP_SOURCES_BY_KIND));
	}

}
