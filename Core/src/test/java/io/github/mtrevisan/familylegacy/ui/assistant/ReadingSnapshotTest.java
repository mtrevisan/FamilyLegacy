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

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.ModelHistory;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.assistant.app.DefaultResearchAssistant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;


/**
 * The findings are computed away from the event dispatch thread on a copy of the model taken before leaving it, so the user can go on
 * editing: the copy never changes, and the actions keep acting on the model of the file.
 */
class ReadingSnapshotTest{

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

	private static List<String> identities(final List<Diagnostic> findings){
		return findings.stream()
			.map(ResearchAssistantService::identity)
			.toList();
	}

	/** What the user does meanwhile: they delete everything, in place, as the editors do. */
	private static void editEverything(final FLEFModel model){
		for(final FLEFRecord record : new ArrayList<>(model.getRecords()))
			model.removeRecord(record.getId());
	}


	@Test
	void theFindingsAreTheOnesOfTheModelWhenTheCopyWasTaken() throws IOException{
		final FLEFModel model = convert();
		final ModelHistory history = new ModelHistory(model);
		final ResearchAssistantService service = DefaultResearchAssistant.createDefault(IgnoredFindings.inMemory());
		final AssistantContext context = new AssistantContext(null, null, model, FILE, history::readOnlyView);
		final List<String> expected = identities(service.diagnose(new AssistantContext(null, null, model, FILE)));
		Assertions.assertFalse(expected.isEmpty());

		//taken on the event dispatch thread; then the user edits, while the findings are computed away from it
		final AssistantContext reading = context.forReading();
		editEverything(model);
		final List<String> found = identities(service.diagnose(reading));

		Assertions.assertEquals(expected, found, "the edit that came after does not reach what is being read");
		Assertions.assertTrue(identities(service.diagnose(context.forReading())).isEmpty(), "a copy taken now sees the edit");
	}

	@Test
	void theActionsKeepTheModelOfTheFile() throws IOException{
		final FLEFModel model = convert();
		final ModelHistory history = new ModelHistory(model);
		final AssistantContext context = new AssistantContext(null, null, model, FILE, history::readOnlyView);

		final AssistantContext reading = context.forReading();

		Assertions.assertSame(model, context.model(), "the context the actions get is not changed");
		Assertions.assertNotSame(model, reading.model(), "what is read is a copy");
		Assertions.assertEquals(FILE, reading.scope(), "the findings the user ignored are kept for the same file");
		Assertions.assertEquals(model.getRecordCount(), reading.model().getRecordCount());
	}

	@Test
	void aContextWithoutACopyIsReadAsItIs() throws IOException{
		final FLEFModel model = convert();
		final AssistantContext context = new AssistantContext(null, null, model, FILE);

		Assertions.assertSame(context, context.forReading());
		final AssistantContext withoutModel = new AssistantContext(null, null, null, FILE, () -> model);
		Assertions.assertSame(withoutModel, withoutModel.forReading(), "no model, nothing to copy");
	}

	@Test
	void theCopyIsTakenOnlyOnce() throws IOException{
		final FLEFModel model = convert();
		final ModelHistory history = new ModelHistory(model);
		final AssistantContext reading = new AssistantContext(null, null, model, FILE, history::readOnlyView).forReading();

		Assertions.assertSame(reading, reading.forReading(), "a context for reading is not copied again");
	}

	@Test
	void theCopyIsSharedWhileTheModelDoesNotChange() throws IOException{
		final FLEFModel model = convert();
		final ModelHistory history = new ModelHistory(model);
		final AssistantContext context = new AssistantContext(null, null, model, FILE, history::readOnlyView);

		Assertions.assertSame(context.forReading().model(), context.forReading().model(),
			"the indicator and the palette read the same copy, which costs once");
	}


	/** A service whose only assistant counts how many times it is asked for findings. */
	private static ResearchAssistantService countingService(final java.util.concurrent.atomic.AtomicInteger calls){
		final ResearchAssistantService service = new ResearchAssistantService(IgnoredFindings.inMemory());
		service.register(new AssistantContributor(){
			@Override
			public List<AssistantAction> actions(){
				return List.of();
			}

			@Override
			public List<DiagnosticProvider> diagnosticProviders(){
				return List.of(context -> {
					calls.incrementAndGet();
					return List.of();
				});
			}
		});
		return service;
	}

	@Test
	void theFindingsOfTheSameCopyAreComputedOnce() throws IOException{
		final FLEFModel model = convert();
		final ModelHistory history = new ModelHistory(model);
		final java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
		final ResearchAssistantService service = countingService(calls);
		final AssistantContext context = new AssistantContext(null, null, model, FILE, history::readOnlyView);

		//the indicator computes, then the palette is opened: it asks for the findings of the same copy
		final List<Diagnostic> first = service.diagnose(context.forReading());
		final List<Diagnostic> second = service.diagnose(context.forReading());

		Assertions.assertEquals(1, calls.get());
		Assertions.assertSame(first, second);
	}

	@Test
	void theFindingsAreComputedAgainWhenTheModelChanged() throws IOException{
		final FLEFModel model = convert();
		final ModelHistory history = new ModelHistory(model);
		final java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
		final ResearchAssistantService service = countingService(calls);
		final AssistantContext context = new AssistantContext(null, null, model, FILE, history::readOnlyView);

		service.diagnose(context.forReading());
		editEverything(model);
		service.diagnose(context.forReading());

		Assertions.assertEquals(2, calls.get());
	}

	@Test
	void aContextThatReadsTheModelAsItIsIsNeverKept() throws IOException{
		final FLEFModel model = convert();
		final java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
		final ResearchAssistantService service = countingService(calls);
		final AssistantContext context = new AssistantContext(null, null, model, FILE);

		service.diagnose(context);
		service.diagnose(context);

		Assertions.assertEquals(2, calls.get(), "the model may have been edited in place between the two");
	}

}
