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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


/**
 * An assistant that says which kinds of record its findings depend on is not asked again while the records of those kinds are the same
 * ones (the copies of the model that the history gives share the records that did not change), and the time of each assistant is told.
 */
class IncrementalFindingsTest{

	private static final String FILE = "C:\\research\\rossi.flef";

	private static FLEFRecord record(final String id, final String type, final String value){
		return FLEFRecord.createMainRecord(id, type)
			.addChild(FLEFRecord.createChildWithTagAndValue("note", value));
	}

	/** A copy of the model with these records (the same instances: what the history does with a record that did not change). */
	private static FLEFModel copy(final FLEFRecord... records){
		final FLEFModel model = new FLEFModel();
		for(final FLEFRecord record : records)
			model.addRecord(record);
		return model;
	}

	private static AssistantContext reading(final FLEFModel copy){
		return new AssistantContext(null, null, copy, FILE, () -> copy, null).forReading();
	}

	private static AssistantContributor assistant(final AtomicInteger asked, final Set<String> dependsOn,
			final AtomicReference<Object> environment){
		return new AssistantContributor(){
			@Override
			public List<AssistantAction> actions(){
				return List.of();
			}

			@Override
			public List<DiagnosticProvider> diagnosticProviders(){
				return List.of(new DiagnosticProvider(){
					@Override
					public List<Diagnostic> diagnose(final AssistantContext context){
						asked.incrementAndGet();
						return List.of(new Diagnostic(Diagnostic.Severity.WARNING, AssistantCategory.RESEARCH, "Found",
							"why " + context.model().getRecordsByType("individual").size()));
					}

					@Override
					public Set<String> dependsOn(){
						return dependsOn;
					}

					@Override
					public Object environment(){
						return environment.get();
					}
				});
			}

			@Override
			public String id(){
				return "gap";
			}
		};
	}


	@Test
	void theFindingsAreKeptWhileTheRecordsTheyDependOnAreTheSameOnes(){
		final ResearchAssistantService service = new ResearchAssistantService();
		final AtomicInteger asked = new AtomicInteger();
		service.register(assistant(asked, Set.of("individual"), new AtomicReference<>()));
		final FLEFRecord anna = record("I1", "individual", "Anna");
		final FLEFRecord place = record("P1", "place", "Treviso");

		final List<Diagnostic> first = service.diagnose(reading(copy(anna, place)));
		//a place was edited: it is another copy of the model, in which the individual is the very same record
		final List<Diagnostic> second = service.diagnose(reading(copy(anna, record("P1", "place", "Padova"))));

		Assertions.assertEquals(1, asked.get());
		Assertions.assertEquals(first, second);
	}

	@Test
	void theFindingsAreComputedAgainWhenARecordTheyDependOnChanged(){
		final ResearchAssistantService service = new ResearchAssistantService();
		final AtomicInteger asked = new AtomicInteger();
		service.register(assistant(asked, Set.of("individual"), new AtomicReference<>()));
		final FLEFRecord anna = record("I1", "individual", "Anna");

		service.diagnose(reading(copy(anna)));
		//the individual was edited (another record with the same id), then another one was added, then one was removed
		service.diagnose(reading(copy(record("I1", "individual", "Anna Rossi"))));
		Assertions.assertEquals(2, asked.get());
		service.diagnose(reading(copy(record("I1", "individual", "Anna Rossi"), record("I2", "individual", "Bruno"))));
		Assertions.assertEquals(3, asked.get());
		service.diagnose(reading(copy()));
		Assertions.assertEquals(4, asked.get());
	}

	@Test
	void anAssistantThatDoesNotSayWhatItDependsOnIsAlwaysAsked(){
		final ResearchAssistantService service = new ResearchAssistantService();
		final AtomicInteger asked = new AtomicInteger();
		service.register(assistant(asked, null, new AtomicReference<>()));
		final FLEFRecord anna = record("I1", "individual", "Anna");

		service.diagnose(reading(copy(anna)));
		service.diagnose(reading(copy(anna)));

		Assertions.assertEquals(2, asked.get());
	}

	@Test
	void whatIsNotACopyThatNobodyChangesIsNeverKept(){
		final ResearchAssistantService service = new ResearchAssistantService();
		final AtomicInteger asked = new AtomicInteger();
		service.register(assistant(asked, Set.of("individual"), new AtomicReference<>()));
		final FLEFModel model = copy(record("I1", "individual", "Anna"));

		//the model of the file, that the user may be editing: not a copy
		service.diagnose(new AssistantContext(null, null, model, FILE));
		service.diagnose(new AssistantContext(null, null, model, FILE));

		Assertions.assertEquals(2, asked.get());
	}

	@Test
	void aChangeOfWhatElseTheFindingsDependOnComputesThemAgain(){
		final ResearchAssistantService service = new ResearchAssistantService();
		final AtomicInteger asked = new AtomicInteger();
		final AtomicReference<Object> environment = new AtomicReference<>("threshold 60");
		service.register(assistant(asked, Set.of("individual"), environment));
		final FLEFRecord anna = record("I1", "individual", "Anna");

		service.diagnose(reading(copy(anna)));
		service.diagnose(reading(copy(anna, record("P1", "place", "Treviso"))));
		Assertions.assertEquals(1, asked.get());

		environment.set("threshold 70");
		service.diagnose(reading(copy(anna)));
		Assertions.assertEquals(2, asked.get());
	}

	@Test
	void theTimeOfEachAssistantIsToldAfterItWasAsked(){
		final ResearchAssistantService service = new ResearchAssistantService();
		service.register(assistant(new AtomicInteger(), Set.of("individual"), new AtomicReference<>()));
		Assertions.assertTrue(service.lastDurationsMillis().isEmpty());

		service.diagnose(reading(copy(record("I1", "individual", "Anna"))));

		Assertions.assertTrue(service.lastDurationsMillis().containsKey("gap"));
		Assertions.assertTrue(service.lastDurationsMillis().get("gap") >= 0L);
	}

	@Test
	void theRecordsOfTheKindsAreTheSameOnesOnlyIfEveryOneIs(){
		final FLEFRecord anna = record("I1", "individual", "Anna");
		final FLEFRecord bruno = record("I2", "individual", "Bruno");
		final FLEFModel before = copy(anna, bruno);

		Assertions.assertTrue(ResearchAssistantService.sameRecords(before, before, Set.of("individual")));
		Assertions.assertTrue(ResearchAssistantService.sameRecords(before, copy(anna, bruno), Set.of("individual", "place")));
		Assertions.assertFalse(ResearchAssistantService.sameRecords(before, copy(anna, record("I2", "individual", "Bruno")),
			Set.of("individual")));
		Assertions.assertFalse(ResearchAssistantService.sameRecords(before, copy(anna), Set.of("individual")));
		//the same content in another record is not "the same": it is computed again, never wrongly kept
		Assertions.assertFalse(ResearchAssistantService.sameRecords(copy(anna), copy(record("I1", "individual", "Anna")), Set.of("individual")));
	}

}
