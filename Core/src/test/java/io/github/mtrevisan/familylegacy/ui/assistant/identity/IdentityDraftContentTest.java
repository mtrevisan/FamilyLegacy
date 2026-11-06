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

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.quality.DuplicateFinderService;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.IdentityHypothesisRecordDialog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** What the draft of an identity hypothesis carries: the shared sources, the reasons feature by feature, and the narrative of a "different people". */
class IdentityDraftContentTest{

	/** Mario Rossi twice, both citing the same source, with different death years that are not within the tolerance. */
	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @S1@ SOUR
		1 TITL Parish register
		0 @I1@ INDI
		1 NAME Mario /Rossi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1850
		1 DEAT
		2 DATE 1 JAN 1900
		1 SOUR @S1@
		0 @I2@ INDI
		1 NAME Mario /Rossi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1851
		1 DEAT
		2 DATE 1 JAN 1920
		1 SOUR @S1@
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static IdentityFinding finding(final FLEFModel model){
		return IdentityChecker.findings(DuplicateFinderService.find(model, new DuplicateFinderService.Options())).getFirst();
	}


	@Test
	void theDraftCarriesTheSharedSourcesAndTheReasonsOneForEachFeature() throws IOException{
		final FLEFModel model = convert();

		final IdentityDraft draft = finding(model).draft();

		Assertions.assertEquals(1, draft.sharedSourceIds().size());
		Assertions.assertEquals("Parish register", io.github.mtrevisan.familylegacy.sources.SourceHelper
			.sourceTitle(model.getRecordById(draft.sharedSourceIds().getFirst())));
		Assertions.assertTrue(draft.similarities().stream().anyMatch(line -> line.startsWith("Name: ") && line.endsWith("(+40)")),
			draft.similarities().toString());
		Assertions.assertTrue(draft.similarities().stream().anyMatch(line -> line.startsWith("Birth year: ")), draft.similarities().toString());
	}

	@Test
	void theNarrativeOfTheDisprovingConclusionListsWhyTheyLookedAlikeAndWhatTellsThemApart() throws IOException{
		final FLEFModel model = convert();
		final IdentityFinding finding = finding(model);
		final FLEFRecord left = model.getRecordById(finding.draft().leftId());
		final FLEFRecord right = model.getRecordById(finding.draft().rightId());

		final String narrative = new RecordDifferentAction(finding.draft()).narrative(model, left, right);

		Assertions.assertTrue(narrative.contains("are different people"), narrative);
		Assertions.assertTrue(narrative.contains("\n- Name: "), narrative);
		Assertions.assertTrue(narrative.contains("They looked alike in:"), narrative);
		Assertions.assertTrue(narrative.contains("What tells them apart:"), narrative);
	}

	@Test
	void aNarrativeWithNothingToListSaysSo(){
		final IdentityDraft draft = new IdentityDraft("I1", "I2", "r", ConfidenceReader.ENUM_LOW, List.of(), List.of(), List.of());

		final String narrative = new RecordDifferentAction(draft).narrative(new FLEFModel(),
			FLEFRecord.createMainRecord("I1", IndividualReader.TYPE), FLEFRecord.createMainRecord("I2", IndividualReader.TYPE));

		Assertions.assertEquals(2, narrative.split("nothing recorded", -1).length - 1, narrative);
	}

	@Test
	void theSharedSourcesAreInTheDraftOfTheHypothesisAsSupportingCitations() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The editor needs a display");
		final FLEFModel model = convert();
		final IdentityDraft draft = finding(model).draft();

		final IdentityHypothesisRecordDialog dialog = IdentityHypothesisRecordDialog.createNew(null, model)
			.withCandidates(model.getRecordById(draft.leftId()), model.getRecordById(draft.rightId()), draft.rationale(), ConfidenceReader.ENUM_LOW)
			.withSources(draft.sharedSourceIds());
		//the draft is in the editor only
		Assertions.assertTrue(model.getRecordsByType(IdentityHypothesisReader.TYPE).isEmpty());
		dialog.save();

		final FLEFRecord hypothesis = model.getRecordsByType(IdentityHypothesisReader.TYPE).getFirst();
		final List<FLEFRecord> citations = FLEFRecordHelper.findChildren(hypothesis, SourceCitationReader.TAG_SOURCE);
		Assertions.assertEquals(1, citations.size());
		Assertions.assertEquals(draft.sharedSourceIds().getFirst(), FLEFRecordHelper.getChildValue(citations.getFirst(), SourceCitationReader.TAG_SOURCE));
		Assertions.assertEquals(SourceCitationReader.STANCE_SUPPORTS, FLEFRecordHelper.getChildValue(citations.getFirst(), SourceCitationReader.TAG_STANCE));
	}

	@Test
	void aSourceThatDoesNotExistIsNotAdded(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The editor needs a display");
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("I2", IndividualReader.TYPE));

		final IdentityHypothesisRecordDialog dialog = IdentityHypothesisRecordDialog.createNew(null, model)
			.withCandidates(model.getRecordById("I1"), model.getRecordById("I2"), "r", ConfidenceReader.ENUM_LOW)
			.withSources(List.of("S404"));
		dialog.save();

		Assertions.assertTrue(FLEFRecordHelper.findChildren(model.getRecordsByType(IdentityHypothesisReader.TYPE).getFirst(), SourceCitationReader.TAG_SOURCE).isEmpty());
	}

}
