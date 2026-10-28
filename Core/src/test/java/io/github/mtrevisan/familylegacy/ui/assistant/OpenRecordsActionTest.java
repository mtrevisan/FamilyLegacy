package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.assistant.chronology.ChronologyAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.conclusion.ConclusionAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;


class OpenRecordsActionTest{

	/** Bruno (1840) is a child of Anna (1850) and has two births. */
	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Anna /Rossi/
		1 SEX F
		1 BIRT
		2 DATE 1 JAN 1850
		0 @I2@ INDI
		1 NAME Bruno /Rossi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1840
		1 BIRT
		2 DATE 1 JAN 1845
		1 FAMC @F1@
		0 @F1@ FAM
		1 WIFE @I1@
		1 CHIL @I2@
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static Diagnostic diagnostic(final DiagnosticAction action, final List<String> recordIds){
		return new Diagnostic(Diagnostic.Severity.WARNING, AssistantCategory.DIAGNOSTICS, "title", "explanation", recordIds, action);
	}

	private static DiagnosticAction action(final String title){
		return new DiagnosticAction(){
			@Override
			public String title(){
				return title;
			}

			@Override
			public void execute(final AssistantContext context){}
		};
	}


	@Test
	void theActionsOfAFinding(){
		final DiagnosticAction draft = action("draft");

		//the action the finding carries comes first (Enter), then the opening of the records (Ctrl+Enter)
		final List<DiagnosticAction> both = DiagnosticActions.of(diagnostic(draft, List.of("I1")));
		Assertions.assertEquals(2, both.size());
		Assertions.assertSame(draft, both.getFirst());
		Assertions.assertEquals("Open the Records…", both.get(1).title());

		//without an action of its own, the opening of the records is the first one
		final List<DiagnosticAction> onlyRecords = DiagnosticActions.of(diagnostic(null, List.of("I1")));
		Assertions.assertEquals(1, onlyRecords.size());
		Assertions.assertEquals("Open the Records…", onlyRecords.getFirst().title());

		//a finding about nothing in particular has nothing to open
		Assertions.assertEquals(List.of(draft), DiagnosticActions.of(diagnostic(draft, List.of())));
		Assertions.assertTrue(DiagnosticActions.of(diagnostic(null, List.of())).isEmpty());
	}

	@Test
	void everyFindingOfTheAssistantsCanOpenItsRecords() throws IOException{
		final FLEFModel model = convert();
		final AssistantContext context = new AssistantContext(null, null, model);
		final List<Diagnostic> findings = new ArrayList<>();
		for(final AssistantContributor contributor : List.of(new ChronologyAssistantContributor(),
				new ConclusionAssistantContributor()))
			contributor.diagnosticProviders().forEach(provider -> findings.addAll(provider.diagnose(context)));

		Assertions.assertFalse(findings.isEmpty());
		for(final Diagnostic finding : findings){
			Assertions.assertFalse(DiagnosticActions.of(finding).isEmpty(), finding.title());
			//the records of the finding are the ones of the model, with an editor
			final List<FLEFRecord> records = OpenRecordsAction.records(model, finding.recordIds());
			Assertions.assertFalse(records.isEmpty(), finding.title());
			Assertions.assertEquals(finding.recordIds().size(), records.size(), finding.title() + " " + finding.recordIds());
		}
		//the conclusion findings keep the draft as the first action
		Assertions.assertTrue(findings.stream().anyMatch(f -> f.action() != null
			&& DiagnosticActions.of(f).getFirst() == f.action()));
	}

	@Test
	void theEditorOfEveryRecordOfAFindingCanBeBuilt() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The editors need a display");
		final FLEFModel model = convert();
		final AssistantContext context = new AssistantContext(null, null, model);
		final List<String> types = new ArrayList<>();
		for(final AssistantContributor contributor : List.of(new ChronologyAssistantContributor(),
				new ConclusionAssistantContributor()))
			for(final DiagnosticProvider provider : contributor.diagnosticProviders())
				for(final Diagnostic finding : provider.diagnose(context))
					for(final FLEFRecord record : OpenRecordsAction.records(model, finding.recordIds())){
						//what the action does, but for showing the editor
						Assertions.assertNotNull(HandlerRegistry.getHandler(record.getTag())
							.createEditDialog(null, model, record), record.getId());
						types.add(record.getTag());
					}

		//individuals, events and relationships
		Assertions.assertTrue(types.contains("relationship") && types.contains("individual") && types.contains("event"), types.toString());
	}

	@Test
	void recordsThatAreGoneAreLeftOut() throws IOException{
		final FLEFModel model = convert();
		final String individual = model.getRecordsByType("individual").getFirst().getId();

		final List<FLEFRecord> records = OpenRecordsAction.records(model, List.of("NOT-THERE", individual, individual));

		Assertions.assertEquals(1, records.size());
		Assertions.assertEquals(individual, records.getFirst().getId());
		Assertions.assertTrue(OpenRecordsAction.records(model, List.of("NOT-THERE")).isEmpty());
	}

	@Test
	void aRecordIsListedByItsTypeItsNameAndItsIdentifier() throws IOException{
		final FLEFModel model = convert();
		final FLEFRecord anna = model.getRecordsByType("individual").stream()
			.filter(r -> OpenRecordsAction.describe(model, r).contains("Anna"))
			.findFirst()
			.orElseThrow();

		final String description = OpenRecordsAction.describe(model, anna);

		Assertions.assertTrue(description.contains("[" + anna.getId() + "]"), description);
		Assertions.assertTrue(description.startsWith("Individual") || description.toLowerCase().startsWith("individual"), description);
	}

	@Test
	void theChooserOpensTheSelectedRecordAndStays() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The list needs a display");
		final FLEFModel model = convert();
		final List<FLEFRecord> records = model.getRecordsByType("individual");
		final List<FLEFRecord> opened = new ArrayList<>();

		final RecordChooserDialog chooser = new RecordChooserDialog(null, model, records,
			(owner, m, record) -> opened.add(record));
		Assertions.assertEquals(records, chooser.records());

		//the first record is selected
		chooser.openSelected();
		chooser.select(1);
		chooser.openSelected();
		chooser.openSelected();

		Assertions.assertEquals(List.of(records.get(0), records.get(1), records.get(1)), opened);
		//nothing is changed by opening
		Assertions.assertEquals(records.size(), model.getRecordsByType("individual").size());
	}

}
