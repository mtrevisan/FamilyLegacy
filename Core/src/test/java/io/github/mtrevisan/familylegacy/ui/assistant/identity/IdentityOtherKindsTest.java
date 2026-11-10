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
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.quality.IdentitySettings;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticAction;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticActions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** The Identity Assistant for the kinds of record other than individuals: the same source twice, the same repository twice. */
class IdentityOtherKindsTest{

	/** The same source twice (an import of two copies), and a different source; the import merges the repeated repository. */
	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @R1@ REPO
		1 NAME State Archive of Treviso
		0 @R2@ REPO
		1 NAME State Archive of Treviso
		0 @S1@ SOUR
		1 TITL Parish register of baptisms of Treviso 1850
		1 AUTH Diocese of Treviso
		1 REPO @R1@
		0 @S2@ SOUR
		1 TITL Parish register of baptisms of Treviso 1850
		1 AUTH Diocese of Treviso
		1 REPO @R2@
		0 @S3@ SOUR
		1 TITL Census of Padua 1881
		1 AUTH Kingdom of Italy
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		final FLEFModel model = new GEDCOMToFLEFConverter().convert(roots);
		//the import merges the repositories that are the same one, so the second copy is the one that somebody typed again by hand
		final FLEFRecord survivor = model.getRecordsByType(RepositoryReader.TYPE).getFirst();
		final FLEFRecord copy = FLEFRecord.createMainRecord("R99", RepositoryReader.TYPE);
		survivor.deepCopyTo(copy);
		copy.setId("R99");
		model.addRecord(copy);
		return model;
	}

	private static List<Diagnostic> found(final FLEFModel model){
		return new IdentityAssistantContributor(() -> IdentitySettings.DEFAULTS).diagnosticProviders().get(1)
			.diagnose(new AssistantContext(null, null, model));
	}


	@Test
	void theSameSourceAndTheSameRepositoryTwiceAreProposedAndTheOthersAreNot() throws IOException{
		final FLEFModel model = convert();

		final List<Diagnostic> findings = found(model);

		Assertions.assertEquals(2, findings.size(), findings.stream().map(Diagnostic::title).toList().toString());
		for(final Diagnostic finding : findings){
			Assertions.assertEquals(AssistantCategory.IDENTITY, finding.category());
			Assertions.assertTrue(finding.isIgnorable());
			Assertions.assertEquals(2, finding.recordIds().size());
			for(final String id : finding.recordIds())
				Assertions.assertFalse(model.getRecordById(id).getTag().equals("source")
					&& model.getRecordById(id).toString().contains("Padua"), finding.title());
		}
		Assertions.assertTrue(findings.stream().anyMatch(finding -> finding.title().startsWith("Possible duplicate")), findings.toString());
	}

	@Test
	void everyFindingOffersTheDraftTheComparisonTheDifferentRecordsAndTheRecords() throws IOException{
		final Diagnostic finding = found(convert()).getFirst();

		final List<DiagnosticAction> actions = DiagnosticActions.of(finding);

		Assertions.assertEquals(List.of("Create Identity Hypothesis Draft…", "Compare Side by Side…", "Record as Different Records…",
			"Open the Records…"), actions.stream().map(DiagnosticAction::title).toList());
	}

	@Test
	void aPairWithAnIdentityHypothesisIsNotProposedAgain() throws IOException{
		final FLEFModel model = convert();
		final Diagnostic finding = found(model).getFirst();

		final FLEFRecord hypothesis = FLEFRecord.createMainRecord("IH1", IdentityHypothesisReader.TYPE);
		for(final String id : finding.recordIds())
			hypothesis.addChild(FLEFRecord.createChildWithTag(IdentityHypothesisReader.TAG_IDENTITY)
				.addChild(FLEFRecord.createChildWithTagAndValue(model.getRecordById(id).getTag(), id)));
		model.addRecord(hypothesis);

		Assertions.assertEquals(1, found(model).size());
		Assertions.assertFalse(found(model).getFirst().recordIds().equals(finding.recordIds()));
	}

	@Test
	void aPairThatAConclusionHasDisprovenIsNotProposedAgain() throws IOException{
		final FLEFModel model = convert();
		final Diagnostic finding = found(model).getFirst();
		final FLEFRecord hypothesis = FLEFRecord.createMainRecord("IH1", IdentityHypothesisReader.TYPE);
		for(final String id : finding.recordIds())
			hypothesis.addChild(FLEFRecord.createChildWithTag(IdentityHypothesisReader.TAG_IDENTITY)
				.addChild(FLEFRecord.createChildWithTagAndValue(model.getRecordById(id).getTag(), id)));
		model.addRecord(hypothesis);
		model.addRecord(FLEFRecord.createMainRecord("C1", ConclusionReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_ISSUE, "identity"))
			.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_PROOF_STATUS, ConclusionReader.ENUM_PROOF_STATUS_DISPROVEN))
			.addChild(FLEFRecord.createChildWithTag(ConclusionReader.TAG_RESOLVES)
				.addChild(FLEFRecord.createChildWithTagAndValue(IdentityHypothesisReader.TYPE, "IH1"))));

		Assertions.assertEquals(1, found(model).size());
	}

	@Test
	void aModelWithoutOtherKindsHasNothing(){
		Assertions.assertTrue(found(new FLEFModel()).isEmpty());
		Assertions.assertTrue(found(null).isEmpty());
	}

}
