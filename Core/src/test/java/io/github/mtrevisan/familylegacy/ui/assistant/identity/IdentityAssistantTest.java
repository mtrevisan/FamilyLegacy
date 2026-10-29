package io.github.mtrevisan.familylegacy.ui.assistant.identity;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticActions;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.IdentityHypothesisRecordDialog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** The Identity Assistant on a real model: the pairs it finds, what it offers, and what a draft does. */
class IdentityAssistantTest{

	/** Mario Rossi is in the file twice (an import of two trees), Maria and Luigi are different people. */
	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Mario /Rossi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1850
		1 DEAT
		2 DATE 1 JAN 1920
		0 @I2@ INDI
		1 NAME Mario /Rossi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1851
		1 DEAT
		2 DATE 1 JAN 1921
		0 @I3@ INDI
		1 NAME Maria /Rossi/
		1 SEX F
		1 BIRT
		2 DATE 1 JAN 1850
		1 DEAT
		2 DATE 1 JAN 1920
		0 @I4@ INDI
		1 NAME Luigi /Verdi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1700
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static List<Diagnostic> found(final FLEFModel model){
		return new IdentityAssistantContributor().diagnosticProviders().getFirst()
			.diagnose(new AssistantContext(null, null, model));
	}

	private static String id(final FLEFModel model, final String name, final int nth){
		return model.getRecordsByType("individual").stream()
			.filter(r -> IndividualReader.extractPrimaryFullname(r).contains(name))
			.skip(nth)
			.findFirst()
			.orElseThrow()
			.getId();
	}


	@Test
	void theDatesOfTheEventsCountInTheSimilarity() throws IOException{
		//a regression: the duplicate finder did not read the years of the dates of the protocol, and scored the name only
		final List<io.github.mtrevisan.familylegacy.ui.tools.tools.DuplicateFinderService.Candidate> candidates
			= io.github.mtrevisan.familylegacy.ui.tools.tools.DuplicateFinderService.find(convert(),
				new io.github.mtrevisan.familylegacy.ui.tools.tools.DuplicateFinderService.Options());

		Assertions.assertEquals(1, candidates.size());
		final var candidate = candidates.getFirst();
		Assertions.assertEquals(1850, candidate.left().birthYear());
		Assertions.assertEquals(1920, candidate.left().deathYear());
		Assertions.assertEquals(1851, candidate.right().birthYear());
		Assertions.assertEquals(1921, candidate.right().deathYear());
		Assertions.assertTrue(candidate.matches().stream().anyMatch(m -> "Birth year".equals(m.feature())), candidate.matches().toString());
		Assertions.assertTrue(candidate.matches().stream().anyMatch(m -> "Death year".equals(m.feature())), candidate.matches().toString());
		//the name (40), the birth within the tolerance (15) and the death (10)
		Assertions.assertEquals(65, candidate.score());
	}

	@Test
	void theSamePersonTwiceIsProposedAndTheOthersAreNot() throws IOException{
		final FLEFModel model = convert();

		final List<Diagnostic> findings = found(model);

		Assertions.assertEquals(1, findings.size(), findings.stream().map(Diagnostic::title).toList().toString());
		final Diagnostic finding = findings.getFirst();
		Assertions.assertEquals(AssistantCategory.IDENTITY, finding.category());
		Assertions.assertEquals(List.of(id(model, "Mario", 0), id(model, "Mario", 1)).stream().sorted().toList(),
			finding.recordIds().stream().sorted().toList());
		//Maria has the same dates and surname but not the sex: a hard filter of the finder
		Assertions.assertFalse(finding.recordIds().contains(id(model, "Maria", 0)));
		Assertions.assertTrue(finding.isIgnorable());
		Assertions.assertTrue(finding.explanation().contains("Name"), finding.explanation());
	}

	@Test
	void everyFindingOffersTheDraftTheComparisonAndTheRecords() throws IOException{
		final Diagnostic finding = found(convert()).getFirst();

		final List<io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticAction> actions = DiagnosticActions.of(finding);

		//Enter, Ctrl+Enter, Ctrl+Shift+Enter
		Assertions.assertEquals(List.of("Create Identity Hypothesis Draft…", "Compare Side by Side…", "Open the Records…"),
			actions.stream().map(io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticAction::title).toList());
	}

	@Test
	void aPairWithAnIdentityHypothesisIsNotProposedAgain() throws IOException{
		final FLEFModel model = convert();
		Assertions.assertEquals(1, found(model).size());

		final FLEFRecord hypothesis = FLEFRecord.createMainRecord("IH1", "identity_hypothesis");
		for(final String id : List.of(id(model, "Mario", 0), id(model, "Mario", 1)))
			hypothesis.addChild(FLEFRecord.createChildWithTag("identity")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", id)));
		model.addRecord(hypothesis);

		Assertions.assertTrue(found(model).isEmpty());
	}

	@Test
	void aDraftIsSavedOnlyIfTheResearcherSavesIt() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The editor needs a display");
		final FLEFModel model = convert();
		final IdentityFinding finding = IdentityChecker.findings(io.github.mtrevisan.familylegacy.ui.tools.tools.DuplicateFinderService
			.find(model, new io.github.mtrevisan.familylegacy.ui.tools.tools.DuplicateFinderService.Options())).getFirst();
		final FLEFRecord first = model.getRecordById(finding.draft().leftId());
		final FLEFRecord second = model.getRecordById(finding.draft().rightId());

		//what the action does, but for showing the editor
		final IdentityHypothesisRecordDialog dialog = IdentityHypothesisRecordDialog.createNew(null, model)
			.withCandidates(first, second, finding.draft().rationale(), finding.draft().confidence());

		//the draft is in the editor only
		Assertions.assertTrue(model.getRecordsByType("identity_hypothesis").isEmpty());
		Assertions.assertFalse(dialog.isSaved());

		//the researcher confirms it as it is
		dialog.save();

		Assertions.assertTrue(dialog.isSaved());
		final List<FLEFRecord> hypotheses = model.getRecordsByType("identity_hypothesis");
		Assertions.assertEquals(1, hypotheses.size());
		final FLEFRecord hypothesis = hypotheses.getFirst();
		Assertions.assertEquals(2, FLEFRecordHelper.findChildren(hypothesis, "identity").size());
		Assertions.assertTrue(FLEFRecordHelper.getChildValue(hypothesis, "rationale")
			.startsWith("Proposed by the Identity Assistant"));
		Assertions.assertEquals(finding.draft().confidence(), FLEFRecordHelper.getChildValue(hypothesis, "confidence"));
		//no record was merged or removed: the two individuals are still there
		Assertions.assertEquals(4, model.getRecordsByType("individual").size());
		//the possibility is recorded: the pair is not proposed again, and what the finding was is resolved
		Assertions.assertTrue(found(model).isEmpty());
	}

	@Test
	void cancellingTheDraftLeavesTheModelAsItWas() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The editor needs a display");
		final FLEFModel model = convert();
		final int records = model.getRecords().size();
		final FLEFRecord first = model.getRecordById(id(model, "Mario", 0));
		final FLEFRecord second = model.getRecordById(id(model, "Mario", 1));

		final IdentityHypothesisRecordDialog dialog = IdentityHypothesisRecordDialog.createNew(null, model)
			.withCandidates(first, second, "rationale", "low");
		//ESC, which is Cancel
		final Object binding = dialog.getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
			.get(io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper.ESCAPE_STROKE);
		dialog.getRootPane().getActionMap().get(binding)
			.actionPerformed(new java.awt.event.ActionEvent(dialog, java.awt.event.ActionEvent.ACTION_PERFORMED, "escape"));

		Assertions.assertFalse(dialog.isSaved());
		Assertions.assertEquals(records, model.getRecords().size());
	}

	@Test
	void anEmptyModelHasNothingToReport() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader("0 HEAD\n1 CHAR UTF-8\n"));

		Assertions.assertTrue(found(new GEDCOMToFLEFConverter().convert(roots)).isEmpty());
	}

}
