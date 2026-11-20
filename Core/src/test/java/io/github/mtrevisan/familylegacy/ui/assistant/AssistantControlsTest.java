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
package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.assistant.AssistantAction;
import io.github.mtrevisan.familylegacy.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.assistant.AssistantContributor;
import io.github.mtrevisan.familylegacy.assistant.AssistantSettings;
import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.assistant.DiagnosticKeys;
import io.github.mtrevisan.familylegacy.assistant.DiagnosticProvider;
import io.github.mtrevisan.familylegacy.assistant.IgnoredFindings;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.Component;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


/**
 * What the user can decide about the Research Assistant: which assistants look for findings (for the palette and for the indicator),
 * whether the indicator is shown, whether the palette opens again, how a finding is ignored or put aside, and how the assistant tells
 * that one of its assistants failed.
 */
class AssistantControlsTest{

	private static final String FILE = "C:\\research\\rossi.flef";

	private static Diagnostic finding(final String kind, final String data, final List<String> records){
		return new Diagnostic(Diagnostic.Severity.WARNING, AssistantCategory.RESEARCH, kind + " " + data, "why", records, null,
			DiagnosticKeys.of(kind, records, data));
	}

	/** An assistant with a name that gives, every time it is asked, the findings it was given. */
	private static AssistantContributor assistant(final String id, final AtomicInteger asked, final List<Diagnostic> findings){
		return new AssistantContributor(){
			@Override
			public List<AssistantAction> actions(){
				return List.of();
			}

			@Override
			public List<DiagnosticProvider> diagnosticProviders(){
				return List.of(context -> {
					asked.incrementAndGet();
					return findings;
				});
			}

			@Override
			public String id(){
				return id;
			}
		};
	}


	@Test
	void theSettingsHaveTheDefaultsAndKeepTheChoices(){
		final Map<String, String> kept = new HashMap<>();
		final AssistantSettings.Storage storage = new AssistantSettings.Storage(){
			@Override
			public String get(final String key, final String defaultValue){
				return kept.getOrDefault(key, defaultValue);
			}

			@Override
			public void put(final String key, final String value){
				kept.put(key, value);
			}
		};
		final AssistantSettings settings = new AssistantSettings(storage);

		Assertions.assertTrue(settings.isIndicatorVisible());
		Assertions.assertTrue(settings.isBlinkOnNewError());
		Assertions.assertTrue(settings.isReopenAfterAction());
		Assertions.assertTrue(settings.isEnabled("gap"));
		Assertions.assertTrue(settings.isEnabledForIndicator("gap"));

		settings.setIndicatorVisible(false);
		settings.setReopenAfterAction(false);
		settings.setEnabled("gap", false);
		settings.setEnabledForIndicator("place", false);

		//another instance on the same storage (the next run of the application) sees the same choices
		final AssistantSettings again = new AssistantSettings(storage);
		Assertions.assertFalse(again.isIndicatorVisible());
		Assertions.assertFalse(again.isReopenAfterAction());
		Assertions.assertTrue(again.isBlinkOnNewError());
		Assertions.assertFalse(again.isEnabled("gap"));
		Assertions.assertFalse(again.isEnabledForIndicator("gap"));
		Assertions.assertTrue(again.isEnabled("place"));
		Assertions.assertFalse(again.isEnabledForIndicator("place"));
		Assertions.assertEquals(Set.of("place"), again.indicatorDisabled());

		again.setEnabled("gap", true);
		Assertions.assertTrue(again.isEnabled("gap"));
	}

	@Test
	void theListenersAreToldOnlyWhenASettingChanges(){
		final AssistantSettings settings = AssistantSettings.inMemory();
		final AtomicInteger told = new AtomicInteger();
		settings.addListener(told::incrementAndGet);

		settings.setIndicatorVisible(false);
		settings.setIndicatorVisible(false);
		settings.setEnabled("gap", false);
		settings.setEnabled("gap", false);

		Assertions.assertEquals(2, told.get());
	}

	@Test
	void anAssistantSwitchedOffIsNotAskedForItsFindings(){
		final ResearchAssistantService service = new ResearchAssistantService();
		final AtomicInteger gap = new AtomicInteger();
		final AtomicInteger place = new AtomicInteger();
		service.register(assistant("gap", gap, List.of(finding("gap.x", "a", List.of("I1")))));
		service.register(assistant("place", place, List.of(finding("place.x", "b", List.of("P1")))));
		final AssistantContext context = new SwingAssistantContext(null, null, new FLEFModel());

		Assertions.assertEquals(2, service.diagnose(context).size());

		service.settings().setEnabled("gap", false);
		final List<Diagnostic> found = service.diagnose(context);

		Assertions.assertEquals(1, found.size());
		Assertions.assertEquals("place.x", found.getFirst().kind());
		Assertions.assertEquals(1, gap.get());
		Assertions.assertEquals(2, place.get());
	}

	@Test
	void theIndicatorCanBeLimitedToSomeAssistantsWhileThePaletteKeepsThemAll(){
		final ResearchAssistantService service = new ResearchAssistantService();
		service.register(assistant("gap", new AtomicInteger(), List.of(finding("gap.x", "a", List.of("I1")))));
		service.register(assistant("place", new AtomicInteger(), List.of(finding("place.x", "b", List.of("P1")))));
		service.settings().setEnabledForIndicator("gap", false);
		final AssistantContext context = new SwingAssistantContext(null, null, new FLEFModel());

		Assertions.assertEquals(2, service.diagnose(context, fraction -> {}, ResearchAssistantService.Audience.PALETTE).size());
		final List<Diagnostic> forIndicator = service.diagnose(context, fraction -> {}, ResearchAssistantService.Audience.INDICATOR);

		Assertions.assertEquals(1, forIndicator.size());
		Assertions.assertEquals("place.x", forIndicator.getFirst().kind());
		//an assistant switched off for the palette is off for the indicator too
		service.settings().setEnabledForIndicator("gap", true);
		service.settings().setEnabled("gap", false);
		Assertions.assertEquals(1, service.diagnose(context, fraction -> {}, ResearchAssistantService.Audience.INDICATOR).size());
	}

	@Test
	void theComputationOfTheIndicatorIsNotSharedWithAPaletteThatAsksForOtherAssistants(){
		final ResearchAssistantService service = new ResearchAssistantService();
		service.register(assistant("gap", new AtomicInteger(), List.of(finding("gap.x", "a", List.of("I1")))));
		service.register(assistant("place", new AtomicInteger(), List.of(finding("place.x", "b", List.of("P1")))));
		service.settings().setEnabledForIndicator("gap", false);
		//the same copy of the model: what the palette and the indicator ask for
		final FLEFModel copy = new FLEFModel();
		final AssistantContext reading = new SwingAssistantContext(null, null, copy, FILE, () -> copy, null).forReading();

		final List<Diagnostic> indicator = service.diagnose(reading, fraction -> {}, ResearchAssistantService.Audience.INDICATOR);
		final List<Diagnostic> palette = service.diagnose(reading, fraction -> {}, ResearchAssistantService.Audience.PALETTE);

		Assertions.assertEquals(1, indicator.size());
		Assertions.assertEquals(2, palette.size());
	}

	@Test
	void anAssistantThatFailsIsReportedAndTheOthersGoOn(){
		final ResearchAssistantService service = new ResearchAssistantService();
		service.register(new AssistantContributor(){
			@Override
			public List<AssistantAction> actions(){
				return List.of();
			}

			@Override
			public List<DiagnosticProvider> diagnosticProviders(){
				return List.of(context -> {
					throw new IllegalStateException("bug in an assistant");
				});
			}

			@Override
			public String id(){
				return "broken";
			}

			@Override
			public String name(){
				return "Broken Assistant";
			}
		});
		service.register(assistant("place", new AtomicInteger(), List.of(finding("place.x", "b", List.of("P1")))));

		final List<Diagnostic> found = service.diagnose(new SwingAssistantContext(null, null, new FLEFModel()));

		Assertions.assertEquals(2, found.size());
		final Diagnostic failure = found.stream()
			.filter(d -> ResearchAssistantService.KIND_FAILED.equals(d.kind()))
			.findFirst()
			.orElseThrow();
		Assertions.assertEquals(Diagnostic.Severity.WARNING, failure.severity());
		Assertions.assertTrue(failure.title().contains("Broken Assistant"), failure.title());
		Assertions.assertTrue(failure.explanation().contains("log"), failure.explanation());
	}

	@Test
	void theAssistantsThatCanBeSwitchedOffAreTheOnesWithFindings(){
		final ResearchAssistantService service = new ResearchAssistantService();
		service.register(assistant("gap", new AtomicInteger(), List.of()));
		service.register(new AssistantContributor(){
			@Override
			public List<AssistantAction> actions(){
				return List.of();
			}

			@Override
			public String id(){
				return "tools";
			}
		});

		Assertions.assertEquals(List.of("gap"), service.assistantsWithFindings().stream().map(AssistantContributor::id).toList());
	}

	@Test
	void theIdOfAnAssistantIsTheNameOfItsClassWithoutTheSuffix(){
		final class ChronologyAssistantContributor implements AssistantContributor{

			@Override
			public List<AssistantAction> actions(){
				return List.of();
			}

		}

		Assertions.assertEquals("chronology", new ChronologyAssistantContributor().id());
		Assertions.assertEquals("Chronology Assistant", new ChronologyAssistantContributor().name());
	}

	@Test
	void theRecordOfAFindingThatTheWindowCanShowIsTheFirstIndividual(){
		final ResearchAssistantService service = new ResearchAssistantService();
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("P1", "place"));
		model.addRecord(FLEFRecord.createMainRecord("I7", "individual"));
		model.addRecord(FLEFRecord.createMainRecord("I8", "individual"));

		//the window does not say how to show a record: nothing can be shown
		Assertions.assertNull(service.viewableRecord(model, List.of("P1", "I7")));

		final List<String> shown = new ArrayList<>();
		service.setRecordViewer(shown::add);
		Assertions.assertEquals("I7", service.viewableRecord(model, List.of("P1", "I7", "I8")));
		Assertions.assertNull(service.viewableRecord(model, List.of("P1")));
		Assertions.assertNull(service.viewableRecord(model, List.of("X9")));

		service.showRecord("I7");
		Assertions.assertEquals(List.of("I7"), shown);
	}


	@Test
	void everyFindingOfAKindCanBeIgnoredAtOnce(){
		final IgnoredFindings ignored = IgnoredFindings.inMemory();
		final Diagnostic first = finding("chronology.event_after_death", "a", List.of("I1"));
		final Diagnostic second = finding("chronology.event_after_death", "b", List.of("I2"));
		final Diagnostic other = finding("chronology.death_before_birth", "c", List.of("I3"));

		ignored.ignoreKind(FILE, first);

		Assertions.assertTrue(ignored.isIgnored(FILE, first));
		Assertions.assertTrue(ignored.isIgnored(FILE, second));
		Assertions.assertFalse(ignored.isIgnored(FILE, other));
		Assertions.assertTrue(ignored.isIgnoredByRule(FILE, second));
		Assertions.assertEquals(List.of(other), ignored.split(FILE, List.of(first, second, other)).active());
	}

	@Test
	void everyFindingAboutARecordCanBeIgnoredAtOnce(){
		final IgnoredFindings ignored = IgnoredFindings.inMemory();
		final Diagnostic about = finding("chronology.event_after_death", "a", List.of("I1", "E1"));
		final Diagnostic alsoAbout = finding("gap.no_death", "b", List.of("I1"));
		final Diagnostic notAbout = finding("gap.no_death", "c", List.of("I2"));

		ignored.ignoreRecord(FILE, "I1");

		Assertions.assertTrue(ignored.isIgnored(FILE, about));
		Assertions.assertTrue(ignored.isIgnored(FILE, alsoAbout));
		Assertions.assertFalse(ignored.isIgnored(FILE, notAbout));
	}

	@Test
	void aFindingPutAsideComesBackOnTheDay(){
		final AtomicReference<LocalDate> today = new AtomicReference<>(LocalDate.of(2026, 11, 18));
		final IgnoredFindings ignored = new IgnoredFindings(new MemoryStorage(), today::get);
		final Diagnostic finding = finding("gap.no_death", "a", List.of("I1"));
		final Diagnostic other = finding("gap.no_death", "b", List.of("I2"));

		ignored.snooze(FILE, finding, LocalDate.of(2026, 11, 25));

		Assertions.assertTrue(ignored.isIgnored(FILE, finding));
		Assertions.assertFalse(ignored.isIgnored(FILE, other));
		today.set(LocalDate.of(2026, 11, 24));
		Assertions.assertTrue(ignored.isIgnored(FILE, finding));
		today.set(LocalDate.of(2026, 11, 25));
		Assertions.assertFalse(ignored.isIgnored(FILE, finding));

		//a finding put aside can be shown again before the day
		today.set(LocalDate.of(2026, 11, 20));
		Assertions.assertTrue(ignored.isIgnored(FILE, finding));
		ignored.restore(FILE, finding);
		Assertions.assertFalse(ignored.isIgnored(FILE, finding));
	}

	@Test
	void showingAFindingAgainAlsoLiftsTheRulesThatCoverIt(){
		final IgnoredFindings ignored = IgnoredFindings.inMemory();
		final Diagnostic finding = finding("gap.no_death", "a", List.of("I1"));
		final Diagnostic sameKind = finding("gap.no_death", "b", List.of("I2"));
		ignored.ignoreKind(FILE, finding);
		ignored.ignoreRecord(FILE, "I1");

		ignored.restore(FILE, finding);

		Assertions.assertFalse(ignored.isIgnored(FILE, finding));
		Assertions.assertFalse(ignored.isIgnored(FILE, sameKind));
		Assertions.assertEquals(0, ignored.count(FILE));
	}

	@Test
	void aFindingWithoutAKeyIsNeverIgnoredByARule(){
		final IgnoredFindings ignored = IgnoredFindings.inMemory();
		final Diagnostic keyless = new Diagnostic(Diagnostic.Severity.WARNING, AssistantCategory.RESEARCH, "t", "why", List.of("I1"));

		ignored.ignoreKind(FILE, keyless);
		ignored.ignoreRecord(FILE, "I1");

		Assertions.assertFalse(ignored.isIgnored(FILE, keyless));
	}

	@Test
	void whatIsIgnoredCanBeSavedAndAddedToAnotherFile(){
		final IgnoredFindings ignored = IgnoredFindings.inMemory();
		final Diagnostic single = finding("gap.no_death", "a", List.of("I1"));
		final Diagnostic snoozed = finding("gap.no_birth", "b", List.of("I2"));
		ignored.ignore(FILE, single);
		ignored.ignoreKind(FILE, finding("chronology.event_after_death", "x", List.of("I3")));
		ignored.ignoreRecord(FILE, "I9");
		ignored.snooze(FILE, snoozed, LocalDate.now().plusDays(30));
		final String text = ignored.export(FILE);

		//the same file after being renamed: it has nothing, then it gets what the old one had
		final String renamed = "C:\\research\\rossi-copy.flef";
		Assertions.assertFalse(ignored.isIgnored(renamed, single));
		Assertions.assertEquals(4, ignored.importFrom(renamed, text));

		Assertions.assertTrue(ignored.isIgnored(renamed, single));
		Assertions.assertTrue(ignored.isIgnored(renamed, snoozed));
		Assertions.assertTrue(ignored.isIgnored(renamed, finding("chronology.event_after_death", "other", List.of("I4"))));
		Assertions.assertTrue(ignored.isIgnored(renamed, finding("gap.no_death", "z", List.of("I9"))));
		//what is there already is not counted twice
		Assertions.assertEquals(0, ignored.importFrom(renamed, text));
		//the file has only digests: no name, no date, no identifier of a record
		Assertions.assertFalse(text.contains("I1") || text.contains("gap"), text);
	}

	@Test
	void aTextThatIsNotAFileOfIgnoredFindingsIsRefusedAndItsBadLinesAreSkipped(){
		final IgnoredFindings ignored = IgnoredFindings.inMemory();

		Assertions.assertThrows(IllegalArgumentException.class, () -> ignored.importFrom(FILE, "hello\nworld"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> ignored.importFrom(FILE, ""));

		final int added = ignored.importFrom(FILE, IgnoredFindings.EXPORT_HEADER + "\n0123456789abcdef\nnot an entry\nk:zzzz\nz:nodate:0123456789abcdef\n");
		Assertions.assertEquals(1, added);
		Assertions.assertEquals(1, ignored.count(FILE));
	}


	@Test
	void theSettingsDialogKeepsEveryChoiceAsSoonAsItIsMade(){
		final ResearchAssistantService service = new ResearchAssistantService();
		service.register(assistant("gap", new AtomicInteger(), List.of()));
		service.register(assistant("place", new AtomicInteger(), List.of()));

		final JPanel panel = ResearchAssistantSettingsDialog.panel(service);
		final List<JCheckBox> boxes = new ArrayList<>();
		for(final Component component : panel.getComponents())
			if(component instanceof JCheckBox box)
				boxes.add(box);
		//three flags, then two columns for each assistant
		Assertions.assertEquals(3 + 2 * 2 + 1, boxes.size());

		boxes.get(0).doClick();
		boxes.get(2).doClick();
		Assertions.assertFalse(service.settings().isIndicatorVisible());
		Assertions.assertFalse(service.settings().isReopenAfterAction());

		//the palette column of the first assistant, then its indicator column: it cannot be chosen when the palette does not show it
		final JCheckBox gapPalette = boxes.get(3);
		final JCheckBox gapIndicator = boxes.get(4);
		Assertions.assertTrue(gapIndicator.isEnabled());
		gapPalette.doClick();
		Assertions.assertFalse(service.settings().isEnabled("gap"));
		Assertions.assertFalse(gapIndicator.isEnabled());
		gapPalette.doClick();
		gapIndicator.doClick();
		Assertions.assertTrue(service.settings().isEnabled("gap"));
		Assertions.assertFalse(service.settings().isEnabledForIndicator("gap"));
	}


	/** A storage that keeps everything in memory. */
	private static final class MemoryStorage implements IgnoredFindings.Storage{

		private final Map<String, Set<String>> kept = new HashMap<>();

		@Override
		public Set<String> load(final String scope){
			return kept.getOrDefault(scope, Set.of());
		}

		@Override
		public void save(final String scope, final Set<String> keys){
			kept.put(scope, Set.copyOf(keys));
		}

	}

}
