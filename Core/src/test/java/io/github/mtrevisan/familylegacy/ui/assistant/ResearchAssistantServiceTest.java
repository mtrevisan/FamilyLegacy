package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.ui.components.HistoricalDateAssistantDialog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.ArrayList;
import java.util.List;


class ResearchAssistantServiceTest{

	/** An editor of a date that records which tool it was asked to open. */
	private static final class DateEditor extends JPanel implements HistoricalDateAssistantHost{

		private HistoricalDateAssistantDialog.Tool opened;

		@Override
		public void openHistoricalDateAssistant(final HistoricalDateAssistantDialog.Tool tool){
			opened = tool;
		}

	}

	private record StubAction(String id, AssistantCategory category, String title, String description, boolean applicable,
			int relevance, List<String> log) implements AssistantAction{

		@Override
		public boolean isApplicable(final AssistantContext context){
			return applicable;
		}

		@Override
		public int relevance(final AssistantContext context){
			return relevance;
		}

		@Override
		public void execute(final AssistantContext context){
			log.add(id);
		}

	}

	private static StubAction stub(final String id, final String title, final boolean applicable, final int relevance){
		return new StubAction(id, AssistantCategory.RESEARCH, title, "", applicable, relevance, new ArrayList<>());
	}

	private static AssistantContributor contributor(final List<AssistantAction> actions, final List<DiagnosticProvider> providers){
		return new AssistantContributor(){
			@Override
			public List<AssistantAction> actions(){
				return actions;
			}

			@Override
			public List<DiagnosticProvider> diagnosticProviders(){
				return providers;
			}
		};
	}

	private static List<String> ids(final List<ResearchAssistantService.RankedAction> ranked){
		return ranked.stream()
			.map(r -> r.action().id())
			.toList();
	}


	@Test
	void dateActionsAreOfferedInTheEditorOfADate(){
		final DateEditor editor = new DateEditor();
		final JTextField field = new JTextField();
		editor.add(field);
		final AssistantContext inEditor = new AssistantContext(null, field, null);
		final AssistantContext elsewhere = new AssistantContext(null, new JButton(), null);
		final ResearchAssistantService service = ResearchAssistantService.createDefault();

		final List<String> inEditorIds = ids(service.search("", inEditor));

		Assertions.assertEquals("dates.assistant", inEditorIds.getFirst());
		Assertions.assertTrue(inEditorIds.containsAll(List.of("dates.conversion", "dates.liturgical", "dates.sun", "dates.relative",
			"dates.event", "dates.age")));
		Assertions.assertTrue(ids(service.search("", elsewhere)).isEmpty());
		Assertions.assertTrue(ids(service.search("", new AssistantContext(null, null, null))).isEmpty());
	}

	@Test
	void dateActionsOpenTheAssistantOnTheirTool(){
		final DateEditor editor = new DateEditor();
		final JTextField field = new JTextField();
		editor.add(field);
		final AssistantContext context = new AssistantContext(null, field, null);
		final ResearchAssistantService service = ResearchAssistantService.createDefault();

		final AssistantAction age = service.search("", context).stream()
			.map(ResearchAssistantService.RankedAction::action)
			.filter(a -> "dates.age".equals(a.id()))
			.findFirst()
			.orElseThrow();
		Assertions.assertNull(editor.opened);

		age.execute(context);

		Assertions.assertEquals(HistoricalDateAssistantDialog.Tool.AGE, editor.opened);
	}

	@Test
	void searchFiltersAndRanks(){
		final ResearchAssistantService service = new ResearchAssistantService();
		service.register(contributor(List.of(
			stub("a", "Estimate Birth Year", true, 0),
			stub("b", "Find Possible Duplicates", true, 0),
			stub("c", "Hidden", false, 0),
			stub("d", "Date Estimator", true, 0)), List.of()));
		final AssistantContext context = new AssistantContext(null, null, null);

		Assertions.assertEquals(List.of("a", "b", "d"), ids(service.search("", context)).stream().sorted().toList());
		Assertions.assertEquals(List.of("b"), ids(service.search("dupl", context)));
		Assertions.assertEquals("d", ids(service.search("date est", context)).getFirst());
		Assertions.assertTrue(ids(service.search("zzz", context)).isEmpty());
	}

	@Test
	void relevanceOrdersTheActionsWhenNothingIsTyped(){
		final ResearchAssistantService service = new ResearchAssistantService();
		service.register(contributor(List.of(
			stub("low", "Alpha", true, 1),
			stub("high", "Zeta", true, 50),
			stub("none", "Beta", true, 0)), List.of()));

		Assertions.assertEquals(List.of("high", "low", "none"), ids(service.search("", new AssistantContext(null, null, null))));
	}

	@Test
	void categoryNameFiltersTheActions(){
		final ResearchAssistantService service = new ResearchAssistantService();
		service.register(contributor(List.of(
			new StubAction("p", AssistantCategory.PLACES, "Find Duplicate Places", "", true, 0, new ArrayList<>()),
			new StubAction("r", AssistantCategory.RESEARCH, "Find Gaps", "", true, 0, new ArrayList<>())), List.of()));
		final AssistantContext context = new AssistantContext(null, null, null);

		Assertions.assertEquals(List.of("p"), ids(service.search(AssistantCategory.PLACES.title(), context)));
	}

	@Test
	void duplicateIdentifiersAreRejected(){
		final ResearchAssistantService service = new ResearchAssistantService();
		service.register(contributor(List.of(stub("x", "One", true, 0)), List.of()));

		Assertions.assertThrows(IllegalArgumentException.class,
			() -> service.register(contributor(List.of(stub("x", "Two", true, 0)), List.of())));
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> service.register(contributor(List.of(stub("y", "Two", true, 0), stub("y", "Three", true, 0)), List.of())));
	}

	@Test
	void diagnosticsAreCollectedAndSortedBySeverity(){
		final ResearchAssistantService service = new ResearchAssistantService();
		service.register(contributor(List.of(), List.of(
			context -> List.of(new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.CONTEXT, "Possible norm", "why"),
				new Diagnostic(Diagnostic.Severity.ERROR, AssistantCategory.DIAGNOSTICS, "Child born before parent", "why")),
			context -> List.of(new Diagnostic(Diagnostic.Severity.WARNING, AssistantCategory.CONCLUSIONS, "Multiple births", "why")))));

		final List<Diagnostic> diagnostics = service.diagnose(new AssistantContext(null, null, null));

		Assertions.assertEquals(List.of("Child born before parent", "Multiple births", "Possible norm"),
			diagnostics.stream().map(Diagnostic::title).toList());
		Assertions.assertEquals(List.of("Multiple births"),
			ResearchAssistantService.filter("multi", diagnostics).stream().map(Diagnostic::title).toList());
		Assertions.assertEquals(3, ResearchAssistantService.filter("", diagnostics).size());
	}

	@Test
	void theFindingsOfTheSameSeverityAreListedByRank(){
		final ResearchAssistantService service = new ResearchAssistantService();
		service.register(contributor(List.of(), List.of(context -> List.of(
			ranked(Diagnostic.Severity.INFORMATION, "Alpha", 62.), ranked(Diagnostic.Severity.WARNING, "Zeta", 10.),
			ranked(Diagnostic.Severity.INFORMATION, "Beta", 74.), ranked(Diagnostic.Severity.INFORMATION, "Gamma", 74.),
			ranked(Diagnostic.Severity.ERROR, "Omega", 0.)))));

		final List<Diagnostic> findings = service.diagnose(new AssistantContext(null, null, null));

		//the most serious first, whatever the rank; within a severity the higher rank first, then by title
		Assertions.assertEquals(List.of("Omega", "Zeta", "Beta", "Gamma", "Alpha"), findings.stream().map(Diagnostic::title).toList());
	}

	private static Diagnostic ranked(final Diagnostic.Severity severity, final String title, final double rank){
		return new Diagnostic(severity, AssistantCategory.IDENTITY, title, "why").withRank(rank);
	}

	@Test
	void aFindingOffersItsActionsThenTheOpeningOfItsRecords(){
		final DiagnosticAction first = action("first");
		final DiagnosticAction second = action("second");
		final Diagnostic finding = new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.IDENTITY, "t", "e",
			List.of("I1", "I2"), first, "key").withAlternatives(List.of(second));

		final List<DiagnosticAction> actions = DiagnosticActions.of(finding);

		//Enter, Ctrl+Enter, Ctrl+Shift+Enter
		Assertions.assertEquals(3, actions.size());
		Assertions.assertSame(first, actions.get(0));
		Assertions.assertSame(second, actions.get(1));
		Assertions.assertEquals("Open the Records…", actions.get(2).title());
		//a finding that keeps its alternatives when it is given another action
		Assertions.assertEquals(List.of(second), finding.withAction(second).alternatives());
		Assertions.assertEquals(finding.key(), finding.withRank(5.).key());
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
	void aFailingDiagnosticProviderIsSkipped(){
		final ResearchAssistantService service = new ResearchAssistantService();
		service.register(contributor(List.of(), List.of(
			context -> {
				throw new IllegalStateException("bug in an assistant");
			},
			context -> List.of(new Diagnostic(Diagnostic.Severity.WARNING, AssistantCategory.RESEARCH, "Missing death", "why")))));

		final List<Diagnostic> diagnostics = service.diagnose(new AssistantContext(null, null, null));

		Assertions.assertEquals(1, diagnostics.size());
	}

	@Test
	void focusedAncestorFindsTheClosestEditor(){
		final DateEditor outer = new DateEditor();
		final DateEditor inner = new DateEditor();
		final JTextField field = new JTextField();
		inner.add(field);
		outer.add(inner);

		Assertions.assertSame(inner, new AssistantContext(null, field, null).focusedAncestor(HistoricalDateAssistantHost.class)
			.orElseThrow());
		Assertions.assertTrue(new AssistantContext(null, null, null).focusedAncestor(HistoricalDateAssistantHost.class).isEmpty());
	}

}
