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
package io.github.mtrevisan.familylegacy.ui.tools.sources;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.sources.SourceUsage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/** Add Citation really adds the citation, and the analysis and the lists see it. */
class SourceToolsTest{

	private FLEFModel model;
	private FLEFRecord individual;
	private FLEFRecord source;


	@BeforeEach
	void buildModel(){
		SourceUsage.invalidate();

		model = new FLEFModel();
		individual = FLEFRecord.createMainRecord("I1", IndividualReader.TYPE);
		source = FLEFRecord.createMainRecord("S1", SourceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_TITLE, "Parish register"));
		model.addRecord(individual);
		model.addRecord(source);
	}


	@Test
	void theAnalysisSeesTheUnusedSourceAndTheUncitedIndividual(){
		final CitationAnalysisTool.Analysis analysis = CitationAnalysisTool.analyze(model);

		Assertions.assertEquals(1, analysis.unusedSources().size());
		Assertions.assertEquals(1, analysis.uncited().size());
		Assertions.assertEquals("I1", analysis.uncited().getFirst().getId());
	}

	@Test
	void addingACitationKeepsTheLocatorAndTheStanceAndMakesBothListsClean(){
		AddCitationDialog.addCitation(individual, source, "  p. 12  ", SourceCitationReader.STANCE_CONTRADICTS);

		final FLEFRecord citation = FLEFRecordHelper.findChild(individual, SourceCitationReader.TAG_SOURCE);
		Assertions.assertEquals("S1", FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_SOURCE));
		Assertions.assertEquals("p. 12", FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_LOCATOR));
		Assertions.assertEquals(SourceCitationReader.STANCE_CONTRADICTS, FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_STANCE));
		final CitationAnalysisTool.Analysis analysis = CitationAnalysisTool.analyze(model);
		Assertions.assertTrue(analysis.unusedSources().isEmpty());
		Assertions.assertTrue(analysis.uncited().isEmpty());
	}

	@Test
	void aBlankLocatorIsNotWritten(){
		AddCitationDialog.addCitation(individual, source, "   ", SourceCitationReader.STANCE_SUPPORTS);

		Assertions.assertNull(FLEFRecordHelper.getChildValue(FLEFRecordHelper.findChild(individual, SourceCitationReader.TAG_SOURCE), "locator"));
	}

	@Test
	void aSourceSaysHowManyCitationsItHas(){
		Assertions.assertEquals("0 citations", SourceUsage.citations(source, model));

		AddCitationDialog.addCitation(individual, source, null, SourceCitationReader.STANCE_SUPPORTS);
		SourceUsage.invalidate();

		Assertions.assertEquals("1 citation", SourceUsage.citations(source, model));
	}

}
