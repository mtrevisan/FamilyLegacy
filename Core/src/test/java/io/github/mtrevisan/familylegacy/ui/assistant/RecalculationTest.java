package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.assistant.app.DefaultResearchAssistant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** The findings computed again after the user edited the records: what is resolved and what is new. */
class RecalculationTest{

	private static final String FILE = "C:\\research\\rossi.flef";

	/** Anna has two births with different dates (a conflict), and Bruno is born before her, though he is her child. */
	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Anna /Rossi/
		1 SEX F
		1 BIRT
		2 DATE 1 JAN 1850
		1 BIRT
		2 DATE 1 JAN 1860
		0 @I2@ INDI
		1 NAME Bruno /Rossi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1840
		1 FAMC @F1@
		0 @F1@ FAM
		1 WIFE @I1@
		1 CHIL @I2@
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static AssistantContext context(final FLEFModel model){
		return new AssistantContext(null, null, model, FILE);
	}

	/** A conclusion that resolves the events of the birth of Anna, preferring the first. */
	private static FLEFRecord conclusionOnTheBirths(final List<Diagnostic> findings){
		final Diagnostic conflict = findings.stream()
			.filter(f -> f.title().startsWith("Conflicting birth dates"))
			.findFirst()
			.orElseThrow();
		final List<String> events = conflict.recordIds().stream()
			.filter(id -> id.startsWith("E"))
			.toList();
		Assertions.assertEquals(2, events.size(), conflict.recordIds().toString());

		final FLEFRecord conclusion = FLEFRecord.createMainRecord("C1", "conclusion");
		conclusion.addChild(FLEFRecord.createChildWithTagAndValue("issue", "birth_date"));
		conclusion.addChild(FLEFRecord.createChildWithTagAndValue("proof_status", "supported"));
		for(final String event : events)
			conclusion.addChild(FLEFRecord.createChildWithTag("resolves")
				.addChild(FLEFRecord.createChildWithTagAndValue("event", event)));
		conclusion.addChild(FLEFRecord.createChildWithTag("preferred")
			.addChild(FLEFRecord.createChildWithTagAndValue("event", events.getFirst())));
		return conclusion;
	}


	@Test
	void nothingChangedIfNothingWasEdited() throws IOException{
		final FLEFModel model = convert();
		final ResearchAssistantService service = DefaultResearchAssistant.createDefault(IgnoredFindings.inMemory());
		final List<Diagnostic> before = service.diagnose(context(model));
		Assertions.assertFalse(before.isEmpty());

		final ResearchAssistantService.Recalculation recalculation = service.recalculate(context(model), before);

		Assertions.assertFalse(recalculation.isChanged());
		Assertions.assertEquals(0, recalculation.resolved());
		Assertions.assertEquals(0, recalculation.added());
		Assertions.assertEquals(before.size(), recalculation.findings().size());
	}

	@Test
	void aFindingResolvedByAConclusionIsGone() throws IOException{
		final FLEFModel model = convert();
		final ResearchAssistantService service = DefaultResearchAssistant.createDefault(IgnoredFindings.inMemory());
		final List<Diagnostic> before = service.diagnose(context(model));

		//the researcher writes the conclusion (what the draft starts) and saves it
		model.addRecord(conclusionOnTheBirths(before));
		final ResearchAssistantService.Recalculation recalculation = service.recalculate(context(model), before);

		Assertions.assertTrue(recalculation.isChanged());
		Assertions.assertEquals(1, recalculation.resolved());
		Assertions.assertEquals(0, recalculation.added());
		Assertions.assertEquals(before.size() - 1, recalculation.findings().size());
		Assertions.assertTrue(recalculation.findings().stream().noneMatch(f -> f.title().startsWith("Conflicting birth dates")));
		//the other findings are the same ones, so what the user ignored about them stays ignored
		final List<String> kept = before.stream()
			.filter(f -> !f.title().startsWith("Conflicting birth dates"))
			.map(ResearchAssistantService::identity)
			.toList();
		Assertions.assertEquals(kept, recalculation.findings().stream().map(ResearchAssistantService::identity).toList());
	}

	@Test
	void aFindingThatReturnsIsNew() throws IOException{
		final FLEFModel model = convert();
		final ResearchAssistantService service = DefaultResearchAssistant.createDefault(IgnoredFindings.inMemory());
		final List<Diagnostic> conflicted = service.diagnose(context(model));
		model.addRecord(conclusionOnTheBirths(conflicted));
		final List<Diagnostic> settled = service.diagnose(context(model));

		//the conclusion is deleted: the conflict is open again
		model.removeRecord("C1");
		final ResearchAssistantService.Recalculation recalculation = service.recalculate(context(model), settled);

		Assertions.assertTrue(recalculation.isChanged());
		Assertions.assertEquals(0, recalculation.resolved());
		Assertions.assertEquals(1, recalculation.added());
		Assertions.assertEquals(conflicted.size(), recalculation.findings().size());
	}

	@Test
	void theSameFindingsInAnotherOrderAreNotAChange() throws IOException{
		final FLEFModel model = convert();
		final ResearchAssistantService service = DefaultResearchAssistant.createDefault(IgnoredFindings.inMemory());
		final List<Diagnostic> before = service.diagnose(context(model));

		final List<Diagnostic> reversed = new java.util.ArrayList<>(before);
		java.util.Collections.reverse(reversed);

		Assertions.assertFalse(service.recalculate(context(model), reversed).isChanged());
	}

	@Test
	void aFindingWithoutAKeyIsTheSameWhileItsTitleAndRecordsAre(){
		final Diagnostic first = new Diagnostic(Diagnostic.Severity.WARNING, AssistantCategory.DIAGNOSTICS, "Title", "why",
			List.of("I1"));
		final Diagnostic again = new Diagnostic(Diagnostic.Severity.WARNING, AssistantCategory.DIAGNOSTICS, "Title", "another why",
			List.of("I1"));
		final Diagnostic other = new Diagnostic(Diagnostic.Severity.WARNING, AssistantCategory.DIAGNOSTICS, "Title", "why",
			List.of("I2"));

		Assertions.assertEquals(ResearchAssistantService.identity(first), ResearchAssistantService.identity(again));
		Assertions.assertNotEquals(ResearchAssistantService.identity(first), ResearchAssistantService.identity(other));
	}

	@Test
	void aProviderThatFailsDoesNotBreakTheRecalculation(){
		final ResearchAssistantService service = new ResearchAssistantService();
		service.register(new AssistantContributor(){
			@Override
			public List<AssistantAction> actions(){
				return List.of();
			}

			@Override
			public List<DiagnosticProvider> diagnosticProviders(){
				return List.of(context -> {
					throw new IllegalStateException("the model was being edited");
				});
			}
		});

		final ResearchAssistantService.Recalculation recalculation = service.recalculate(context(null), List.of());

		Assertions.assertFalse(recalculation.isChanged());
		Assertions.assertTrue(recalculation.findings().isEmpty());
	}

}
