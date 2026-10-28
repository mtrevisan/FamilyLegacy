package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.assistant.chronology.ChronologyAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.conclusion.ConclusionAssistantContributor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


class IgnoredFindingsTest{

	private static final String FILE = "C:\\research\\rossi.flef";

	/** A storage in memory that records what it is asked to keep. */
	private static final class Memory implements IgnoredFindings.Storage{

		private final Map<String, Set<String>> kept = new HashMap<>();
		private int saves;

		@Override
		public Set<String> load(final String scope){
			return kept.getOrDefault(scope, Set.of());
		}

		@Override
		public void save(final String scope, final Set<String> keys){
			kept.put(scope, new HashSet<>(keys));
			saves ++;
		}

	}

	private static Diagnostic finding(final String title, final String key){
		return new Diagnostic(Diagnostic.Severity.WARNING, AssistantCategory.DIAGNOSTICS, title, "why", List.of("I1"), null, key);
	}


	@Test
	void aFindingIsIgnoredAndShownAgain(){
		final IgnoredFindings ignored = new IgnoredFindings(new Memory());
		final Diagnostic finding = finding("Child born before the parent", "chronology.child_before_parent|I1,I2|a~b");

		Assertions.assertFalse(ignored.isIgnored(FILE, finding));

		ignored.ignore(FILE, finding);
		Assertions.assertTrue(ignored.isIgnored(FILE, finding));

		ignored.restore(FILE, finding);
		Assertions.assertFalse(ignored.isIgnored(FILE, finding));
	}

	@Test
	void theListenersAreTold(){
		final IgnoredFindings ignored = new IgnoredFindings(new Memory());
		final java.util.concurrent.atomic.AtomicInteger told = new java.util.concurrent.atomic.AtomicInteger();
		ignored.addListener(told::incrementAndGet);
		final Diagnostic finding = finding("Told", "kind|I1|x");

		ignored.ignore(FILE, finding);
		Assertions.assertEquals(1, told.get());
		ignored.restore(FILE, finding);
		Assertions.assertEquals(2, told.get());
		//a file never saved is told too
		ignored.ignore(null, finding);
		Assertions.assertEquals(3, told.get());
		//nothing changes for a finding that cannot be ignored, or that is not ignored
		ignored.ignore(FILE, finding("No key", null));
		ignored.restore(FILE, finding("Not ignored", "kind|I9|y"));
		Assertions.assertEquals(3, told.get());
	}

	@Test
	void aFindingWithoutAKeyCannotBeIgnored(){
		final Memory memory = new Memory();
		final IgnoredFindings ignored = new IgnoredFindings(memory);
		final Diagnostic finding = finding("No key", null);

		Assertions.assertFalse(finding.isIgnorable());
		ignored.ignore(FILE, finding);

		Assertions.assertFalse(ignored.isIgnored(FILE, finding));
		Assertions.assertEquals(0, memory.saves);
	}

	@Test
	void aFindingThatChangesComesBack(){
		final IgnoredFindings ignored = new IgnoredFindings(new Memory());
		ignored.ignore(FILE, finding("Child born before the parent", "kind|I1,I2|born 1840~parent 1850"));

		//the date of the parent was corrected but the finding is still there: it is another finding
		Assertions.assertFalse(ignored.isIgnored(FILE, finding("Child born before the parent", "kind|I1,I2|born 1840~parent 1842")));
		//the same finding with another title (another language) is still the ignored one
		Assertions.assertTrue(ignored.isIgnored(FILE, finding("Figlio nato prima del genitore", "kind|I1,I2|born 1840~parent 1850")));
	}

	@Test
	void theFilesDoNotShareTheirIgnoredFindings(){
		final IgnoredFindings ignored = new IgnoredFindings(new Memory());
		final Diagnostic finding = finding("Same finding", "kind|I1|x");

		ignored.ignore(FILE, finding);

		Assertions.assertTrue(ignored.isIgnored(FILE, finding));
		Assertions.assertFalse(ignored.isIgnored("C:\\research\\bianchi.flef", finding));
	}

	@Test
	void theChoiceIsKeptBetweenSessions(){
		final Memory memory = new Memory();
		final Diagnostic finding = finding("Kept", "kind|I1|x");
		new IgnoredFindings(memory).ignore(FILE, finding);

		//the application is started again: the same preferences, a new object
		final IgnoredFindings next = new IgnoredFindings(memory);

		Assertions.assertTrue(next.isIgnored(FILE, finding));
		next.restore(FILE, finding);
		Assertions.assertFalse(new IgnoredFindings(memory).isIgnored(FILE, finding));
	}

	@Test
	void onlyDigestsAreKept(){
		final Memory memory = new Memory();
		new IgnoredFindings(memory).ignore(FILE, finding("Secret", "chronology.x|I1|Anna Rossi~born 1 JAN 1850"));

		//neither the path of the file nor the names and dates of the people are written in the preferences
		Assertions.assertEquals(1, memory.kept.size());
		final String scope = memory.kept.keySet().iterator().next();
		final String key = memory.kept.get(scope).iterator().next();
		Assertions.assertTrue(scope.matches("[0-9a-f]{16}"), scope);
		Assertions.assertTrue(key.matches("[0-9a-f]{16}"), key);
	}

	@Test
	void aFileThatWasNeverSavedKeepsThemForTheSessionOnly(){
		final Memory memory = new Memory();
		final IgnoredFindings ignored = new IgnoredFindings(memory);
		final Diagnostic finding = finding("Unsaved", "kind|I1|x");

		ignored.ignore(null, finding);

		Assertions.assertTrue(ignored.isIgnored(null, finding));
		Assertions.assertEquals(0, memory.saves);
		Assertions.assertFalse(new IgnoredFindings(memory).isIgnored(null, finding));
		ignored.restore(null, finding);
		Assertions.assertFalse(ignored.isIgnored(null, finding));
	}

	@Test
	void theFindingsAreSplitKeepingTheirOrder(){
		final IgnoredFindings ignored = new IgnoredFindings(new Memory());
		final Diagnostic first = finding("A", "k|1");
		final Diagnostic second = finding("B", "k|2");
		final Diagnostic third = finding("C", "k|3");
		final Diagnostic unkeyed = finding("D", null);
		ignored.ignore(FILE, second);

		final IgnoredFindings.Split split = ignored.split(FILE, List.of(first, second, third, unkeyed));

		Assertions.assertEquals(List.of(first, third, unkeyed), split.active());
		Assertions.assertEquals(List.of(second), split.ignored());
	}

	@Test
	void theKeyDoesNotDependOnTheOrderOfTheRecords(){
		Assertions.assertEquals(DiagnosticKeys.of("kind", List.of("I2", "I1", "E1"), "a", 3),
			DiagnosticKeys.of("kind", List.of("E1", "I1", "I2"), "a", 3));
		Assertions.assertNotEquals(DiagnosticKeys.of("kind", List.of("I1"), "a"), DiagnosticKeys.of("kind", List.of("I1"), "b"));
		Assertions.assertNotEquals(DiagnosticKeys.of("kind", List.of("I1"), "a"), DiagnosticKeys.of("other", List.of("I1"), "a"));
		Assertions.assertNotEquals(DiagnosticKeys.of("kind", List.of("I1"), "a"), DiagnosticKeys.of("kind", List.of("I1", "I2"), "a"));
	}

	@Test
	void theServiceKeepsTheIgnoredFindingsApartFromThePreferencesOfTheUser(){
		//a service built by a test keeps them in memory: it never writes the preferences of whoever runs the tests
		final ResearchAssistantService first = new ResearchAssistantService();
		final ResearchAssistantService second = new ResearchAssistantService();
		final Diagnostic finding = finding("Only here", "kind|I1|x");

		first.ignoredFindings().ignore(FILE, finding);

		Assertions.assertTrue(first.ignoredFindings().isIgnored(FILE, finding));
		Assertions.assertFalse(second.ignoredFindings().isIgnored(FILE, finding));
	}

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

	private static List<Diagnostic> findings(final FLEFModel model){
		final AssistantContext context = new AssistantContext(null, null, model, FILE);
		final List<Diagnostic> findings = new ArrayList<>();
		for(final AssistantContributor contributor : List.of(new ChronologyAssistantContributor(),
				new ConclusionAssistantContributor()))
			contributor.diagnosticProviders().forEach(provider -> findings.addAll(provider.diagnose(context)));
		return findings;
	}

	@Test
	void everyFindingOfTheAssistantsCanBeIgnored() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		final FLEFModel model = new GEDCOMToFLEFConverter().convert(roots);
		final List<Diagnostic> found = findings(model);
		Assertions.assertTrue(found.size() >= 2, found.toString());

		for(final Diagnostic finding : found)
			Assertions.assertTrue(finding.isIgnorable(), finding.title());
		//the keys tell the findings apart
		Assertions.assertEquals(found.size(), found.stream().map(Diagnostic::key).distinct().count());
		//the findings of a model are the same at every opening: what was ignored stays ignored
		Assertions.assertEquals(found.stream().map(Diagnostic::key).toList(), findings(model).stream().map(Diagnostic::key).toList());

		final IgnoredFindings ignored = new IgnoredFindings(new Memory());
		ignored.ignore(FILE, found.getFirst());
		final IgnoredFindings.Split split = ignored.split(FILE, findings(model));
		Assertions.assertEquals(1, split.ignored().size());
		Assertions.assertEquals(found.size() - 1, split.active().size());
		//a finding keeps its actions when it is shown again after being ignored
		Assertions.assertEquals(found.getFirst().action() != null, split.ignored().getFirst().action() != null);
	}

}
