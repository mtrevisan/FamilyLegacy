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


/** What the bibliographies say of the sources that are derived from other sources (a transcription, a copy of it). */
class ReportDerivedSourcesTest{

	@TempDir
	Path directory;

	private FLEFModel model;


	/**
	 * S1 is a register, S2 a transcription of it (that disagrees on page 12), S3 a copy of the transcription. Anna cites only the copy.
	 */
	@BeforeEach
	void buildModel(){
		model = new FLEFModel();
		model.addRecord(source("S1", "Parish register"));
		model.addRecord(source("S2", "Transcription", derivedFrom("S1", "p. 12", SourceCitationReader.STANCE_CONTRADICTS)));
		model.addRecord(source("S3", "Copy", derivedFrom("S2", null, SourceCitationReader.STANCE_SUPPORTS)));
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, "Anna Rossi")))
			.addChild(FLEFRecord.createChildWithTag(SourceCitationReader.TAG_SOURCE)
				.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, "S3"))));
	}

	private static FLEFRecord source(final String id, final String title, final FLEFRecord... derivations){
		final FLEFRecord source = FLEFRecord.createMainRecord(id, SourceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, title)));
		for(final FLEFRecord derivation : derivations)
			source.addChild(derivation);
		return source;
	}

	private static FLEFRecord derivedFrom(final String originalId, final String locator, final String stance){
		final FLEFRecord citation = FLEFRecord.createChildWithTag(SourceReader.TAG_DERIVED_FROM)
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, originalId))
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, stance));
		if(locator != null)
			citation.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_LOCATOR, locator));
		return citation;
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


	@Test
	void aDerivedSourceTellsWhatItIsMadeFromAndTheOriginal() throws IOException{
		final String markdown = markdown(ReportType.SOURCE, "S3", ReportConfig.defaults());

		//the copy is made from the transcription, which is made from the register
		Assertions.assertTrue(markdown.contains("**Derived from:** Transcription"), markdown);
		Assertions.assertTrue(markdown.contains("**Original:** Parish register [S1]"), markdown);
	}

	@Test
	void aSourceTellsWhichSourcesAreMadeFromIt() throws IOException{
		final String markdown = markdown(ReportType.SOURCE, "S1", ReportConfig.defaults());

		Assertions.assertTrue(markdown.contains("Derived sources"), markdown);
		Assertions.assertTrue(markdown.contains("Transcription [S2] — p. 12 (Contradicts)"), markdown);
	}

	@Test
	void theLocatorAndTheDisagreementAreToldWhereTheDerivationIs() throws IOException{
		final String markdown = markdown(ReportType.SOURCE, "S2", ReportConfig.defaults());

		Assertions.assertTrue(markdown.contains("**Derived from:** Parish register [S1] — p. 12 (Contradicts)"), markdown);
		//the register is the only source above: it is not told again as the original
		Assertions.assertFalse(markdown.contains("**Original:**"), markdown);
	}

	@Test
	void theBibliographyOfAPersonListsTheOriginalsOfTheCitedSources() throws IOException{
		final String markdown = markdown(ReportType.INDIVIDUAL, "I1", ReportConfig.defaults());

		Assertions.assertTrue(markdown.contains("Copy"), markdown);
		Assertions.assertTrue(markdown.contains("Transcription"), markdown);
		Assertions.assertTrue(markdown.contains("Parish register"), markdown);
		Assertions.assertTrue(markdown.contains("Not cited directly"), markdown);
		Assertions.assertTrue(markdown.contains("## Parish register"), markdown);
	}

	@Test
	void theOriginalsAreLeftOutWhenTheOptionIsOff() throws IOException{
		final ReportConfig config = ReportConfig.defaults().with(ReportOption.INCLUDE_ORIGINAL_SOURCES, false);

		final String markdown = markdown(ReportType.INDIVIDUAL, "I1", config);

		Assertions.assertTrue(markdown.contains("Copy"), markdown);
		//the copy still tells its original in its details, but the original has no entry of its own
		Assertions.assertFalse(markdown.contains("## Parish register"), markdown);
		Assertions.assertFalse(markdown.contains("Not cited directly"), markdown);
	}

	@Test
	void theFullBibliographyHasEverySourceWithItsDerivation() throws IOException{
		final String markdown = markdown(ReportType.FULL_BIBLIOGRAPHY, null, ReportConfig.defaults());

		Assertions.assertTrue(markdown.contains("**Derived from:** Transcription"), markdown);
		Assertions.assertTrue(markdown.contains("Derived sources"), markdown);
	}

	@Test
	void theOptionIsOfferedOnlyWhereSourcesAreListed(){
		Assertions.assertEquals(io.github.mtrevisan.familylegacy.reports.OptionState.EDITABLE,
			ReportBuilder.optionState(ReportType.INDIVIDUAL, model.getRecordById("I1"), ReportOption.INCLUDE_ORIGINAL_SOURCES));
		//the full bibliography lists every source already
		Assertions.assertEquals(io.github.mtrevisan.familylegacy.reports.OptionState.HIDDEN,
			ReportBuilder.optionState(ReportType.FULL_BIBLIOGRAPHY, null, ReportOption.INCLUDE_ORIGINAL_SOURCES));
	}

}
