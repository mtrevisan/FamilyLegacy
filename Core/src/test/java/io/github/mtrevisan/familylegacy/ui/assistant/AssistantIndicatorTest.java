package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.assistant.chronology.ChronologyAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.conclusion.ConclusionAssistantContributor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


class AssistantIndicatorTest{

	private static final String FILE = "C:\\research\\rossi.flef";

	/** Anna has two births (a conflict), and her child is born before her (a chronological error). */
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

	private static ResearchAssistantService service(final AtomicInteger computations, final AtomicLong clock, final long duration){
		final ResearchAssistantService service = new ResearchAssistantService();
		service.register(new ChronologyAssistantContributor());
		service.register(new ConclusionAssistantContributor());
		service.register(new AssistantContributor(){
			@Override
			public List<AssistantAction> actions(){
				return List.of();
			}

			@Override
			public List<DiagnosticProvider> diagnosticProviders(){
				return List.of(context -> {
					computations.incrementAndGet();
					clock.addAndGet(duration);
					return List.of();
				});
			}
		});
		return service;
	}

	private static Diagnostic finding(final Diagnostic.Severity severity, final String key){
		return new Diagnostic(severity, AssistantCategory.DIAGNOSTICS, key, "why", List.of("I1"), null, key);
	}


	@Test
	void theCountsTellTheFindingsOfEachSeverity(){
		final FindingCounts counts = FindingCounts.of(List.of(finding(Diagnostic.Severity.ERROR, "a"),
			finding(Diagnostic.Severity.WARNING, "b"), finding(Diagnostic.Severity.WARNING, "c"),
			finding(Diagnostic.Severity.INFORMATION, "d")));

		Assertions.assertEquals(new FindingCounts(1, 2, 1), counts);
		Assertions.assertEquals(4, counts.total());
		Assertions.assertFalse(counts.isClean());
		Assertions.assertEquals("✖ 1  ⚠ 2  ℹ 1", counts.label());
		Assertions.assertEquals("1 error(s), 2 warning(s), 1 note(s)", counts.summary());
	}

	@Test
	void onlyTheCountsThatAreNotZeroAreShown(){
		Assertions.assertEquals("⚠ 3", new FindingCounts(0, 3, 0).label());
		Assertions.assertEquals("✖ 2  ℹ 5", new FindingCounts(2, 0, 5).label());
		Assertions.assertEquals("✓", new FindingCounts(0, 0, 0).label());
		Assertions.assertTrue(FindingCounts.of(List.of()).isClean());
		Assertions.assertEquals("no findings", new FindingCounts(0, 0, 0).summary());
	}

	@Test
	void theIndicatorSaysItIsCheckingUntilTheFirstComputationEnds() throws IOException{
		final FLEFModel model = convert();
		final Deque<Runnable> pending = new ArrayDeque<>();
		final AssistantIndicator indicator = new AssistantIndicator(service(new AtomicInteger(), new AtomicLong(), 0),
			() -> new AssistantContext(null, null, model, FILE), () -> false, pending::add, Runnable::run, System::currentTimeMillis);

		Assertions.assertEquals("Assistant …", indicator.text());
		Assertions.assertNull(indicator.counts());
		Assertions.assertTrue(indicator.tooltip().contains("checking"), indicator.tooltip());

		indicator.onTimer();
		//the computation is running, away from the event dispatch thread
		Assertions.assertEquals(1, pending.size());
		Assertions.assertNull(indicator.counts());

		pending.poll().run();
		Assertions.assertNotNull(indicator.counts());
		Assertions.assertFalse(indicator.counts().isClean());
	}

	@Test
	void theCountsAreThoseOfTheFindingsOfTheModel() throws IOException{
		final FLEFModel model = convert();
		final ResearchAssistantService service = service(new AtomicInteger(), new AtomicLong(), 0);
		final List<Diagnostic> expected = service.diagnose(new AssistantContext(null, null, model, FILE));
		final AssistantIndicator indicator = new AssistantIndicator(service, () -> new AssistantContext(null, null, model, FILE),
			() -> false, Runnable::run, Runnable::run, System::currentTimeMillis);

		indicator.onTimer();

		Assertions.assertEquals(FindingCounts.of(expected), indicator.counts());
		Assertions.assertTrue(indicator.counts().errors() >= 1, indicator.counts().toString());
		Assertions.assertTrue(indicator.counts().warnings() >= 1, indicator.counts().toString());
		Assertions.assertTrue(indicator.text().startsWith("Assistant "), indicator.text());
		Assertions.assertTrue(indicator.text().contains(indicator.counts().label()), indicator.text());
		Assertions.assertTrue(indicator.tooltip().contains(indicator.counts().summary()), indicator.tooltip());
	}

	@Test
	void ignoringAFindingRecountsWithoutComputingAgain() throws IOException{
		final FLEFModel model = convert();
		final AtomicInteger computations = new AtomicInteger();
		final ResearchAssistantService service = service(computations, new AtomicLong(), 0);
		final AssistantIndicator indicator = new AssistantIndicator(service, () -> new AssistantContext(null, null, model, FILE),
			() -> false, Runnable::run, Runnable::run, System::currentTimeMillis);
		indicator.onTimer();
		final int total = indicator.counts().total();
		Assertions.assertEquals(1, computations.get());
		final Diagnostic first = service.diagnose(new AssistantContext(null, null, model, FILE)).getFirst();
		computations.set(0);

		service.ignoredFindings().ignore(FILE, first);

		Assertions.assertEquals(total - 1, indicator.counts().total());
		Assertions.assertEquals(0, computations.get(), "the findings are not computed again to count them");

		service.ignoredFindings().restore(FILE, first);
		Assertions.assertEquals(total, indicator.counts().total());
	}

	@Test
	void nothingIsComputedWhileAModalDialogIsOpen() throws IOException{
		final FLEFModel model = convert();
		final AtomicInteger computations = new AtomicInteger();
		final AtomicBoolean busy = new AtomicBoolean(true);
		final AssistantIndicator indicator = new AssistantIndicator(service(computations, new AtomicLong(), 0),
			() -> new AssistantContext(null, null, model, FILE), busy::get, Runnable::run, Runnable::run, System::currentTimeMillis);

		indicator.onTimer();
		Assertions.assertEquals(0, computations.get());
		Assertions.assertNull(indicator.counts());

		//the dialog is closed: the next try computes
		busy.set(false);
		indicator.onTimer();
		Assertions.assertEquals(1, computations.get());
		Assertions.assertNotNull(indicator.counts());
	}

	@Test
	void oneComputationAtATimeAndAChangeMeanwhileAsksForAnother() throws IOException{
		final FLEFModel model = convert();
		final AtomicInteger computations = new AtomicInteger();
		final Deque<Runnable> pending = new ArrayDeque<>();
		final AssistantIndicator indicator = new AssistantIndicator(service(computations, new AtomicLong(), 0),
			() -> new AssistantContext(null, null, model, FILE), () -> false, pending::add, Runnable::run,
			System::currentTimeMillis);

		indicator.onTimer();
		//the timer fires again (a change settled) while the computation is still running: no second computation starts
		indicator.refreshSoon();
		indicator.onTimer();
		Assertions.assertEquals(1, pending.size());

		pending.poll().run();
		Assertions.assertEquals(1, computations.get());
		//the change that came meanwhile has its own computation
		indicator.onTimer();
		Assertions.assertEquals(1, pending.size());
		pending.poll().run();
		Assertions.assertEquals(2, computations.get());
	}

	@Test
	void aLongComputationIsNotRepeatedAtOnce() throws IOException{
		final FLEFModel model = convert();
		final AtomicLong clock = new AtomicLong(1_000_000);
		//a computation that takes ten seconds (a big file)
		final AssistantIndicator indicator = new AssistantIndicator(service(new AtomicInteger(), clock, 10_000),
			() -> new AssistantContext(null, null, model, FILE), () -> false, Runnable::run, Runnable::run, clock::get);
		Assertions.assertEquals(AssistantIndicator.DEBOUNCE_MILLIS, indicator.delayUntilNext());

		indicator.onTimer();

		//the wait is a multiple of how long it took, not the usual two seconds
		Assertions.assertEquals(AssistantIndicator.BACKOFF_FACTOR * 10_000L, indicator.delayUntilNext());
		clock.addAndGet(AssistantIndicator.BACKOFF_FACTOR * 10_000L);
		Assertions.assertEquals(AssistantIndicator.DEBOUNCE_MILLIS, indicator.delayUntilNext());
	}

	@Test
	void aQuickComputationWaitsTheMinimumInterval() throws IOException{
		final FLEFModel model = convert();
		final AtomicLong clock = new AtomicLong(1_000_000);
		final AssistantIndicator indicator = new AssistantIndicator(service(new AtomicInteger(), clock, 50),
			() -> new AssistantContext(null, null, model, FILE), () -> false, Runnable::run, Runnable::run, clock::get);

		indicator.onTimer();

		Assertions.assertEquals(AssistantIndicator.MIN_INTERVAL_MILLIS, indicator.delayUntilNext());
	}

	@Test
	void withoutAModelThereIsNothingToLookAt(){
		final AssistantIndicator indicator = new AssistantIndicator(new ResearchAssistantService(),
			() -> new AssistantContext(null, null, null, null), () -> false, Runnable::run, Runnable::run, System::currentTimeMillis);

		indicator.onTimer();

		Assertions.assertTrue(indicator.counts().isClean());
		Assertions.assertTrue(indicator.text().contains("✓"), indicator.text());
	}

	@Test
	void theIndicatorIsAComponentForTheToolbar(){
		final AssistantIndicator indicator = new AssistantIndicator(new ResearchAssistantService(),
			() -> new AssistantContext(null, null, null, null), () -> false, Runnable::run, Runnable::run, System::currentTimeMillis);

		Assertions.assertNotNull(indicator.component());
		//the shortcut is in the tooltip, to be learned
		Assertions.assertTrue(indicator.tooltip().contains("Ctrl+Shift+A"), indicator.tooltip());
		final List<Object> listeners = new ArrayList<>(List.of(((javax.swing.JButton)indicator.component()).getActionListeners()));
		Assertions.assertEquals(1, listeners.size());
	}
}
