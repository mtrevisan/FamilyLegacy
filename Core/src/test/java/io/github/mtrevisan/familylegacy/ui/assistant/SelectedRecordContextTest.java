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
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The record selected in the main window is part of the context of the assistant, and the findings about it come first. */
class SelectedRecordContextTest{

	private static Diagnostic finding(final String title, final String... recordIds){
		return new Diagnostic(Diagnostic.Severity.WARNING, AssistantCategory.DIAGNOSTICS, title, title, List.of(recordIds), null, title);
	}

	private static FLEFModel modelWith(final String id){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord(id, IndividualReader.TYPE));
		return model;
	}


	@Test
	void theSelectedRecordIsFoundInTheModel(){
		final FLEFModel model = modelWith("I1");

		Assertions.assertEquals("I1", new AssistantContext(null, null, model, null, null, "I1").selectedRecord().orElseThrow().getId());
		Assertions.assertTrue(new AssistantContext(null, null, model, null, null, "I2").selectedRecord().isEmpty(),
			"a record that is not there is not selected");
		Assertions.assertTrue(new AssistantContext(null, null, model).selectedRecord().isEmpty());
		Assertions.assertTrue(new AssistantContext(null, null, null, null, null, "I1").selectedRecord().isEmpty());
	}

	@Test
	void theSelectionSurvivesTheCopyForReading(){
		final FLEFModel model = modelWith("I1");
		final AssistantContext context = new AssistantContext(null, null, model, null, () -> modelWith("I1"), "I1");

		Assertions.assertEquals("I1", context.forReading().selectedRecordId());
	}

	@Test
	void theFindingsAboutTheRecordComeFirstInTheirOrder(){
		final Diagnostic a = finding("a", "I2");
		final Diagnostic b = finding("b", "I1", "I3");
		final Diagnostic c = finding("c", "I4");
		final Diagnostic d = finding("d", "I1");

		Assertions.assertEquals(List.of(b, d, a, c), ResearchAssistantService.aboutRecord(List.of(a, b, c, d), "I1", false));
	}

	@Test
	void onlyTheFindingsAboutTheRecordCanBeAsked(){
		final Diagnostic a = finding("a", "I2");
		final Diagnostic b = finding("b", "I1", "I3");

		Assertions.assertEquals(List.of(b), ResearchAssistantService.aboutRecord(List.of(a, b), "I1", true));
		Assertions.assertTrue(ResearchAssistantService.aboutRecord(List.of(a), "I1", true).isEmpty());
	}

}
