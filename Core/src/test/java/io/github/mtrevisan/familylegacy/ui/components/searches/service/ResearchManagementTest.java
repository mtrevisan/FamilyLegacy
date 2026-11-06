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
package io.github.mtrevisan.familylegacy.ui.components.searches.service;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.research.ResearchSummary;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchTaskHandler;
import io.github.mtrevisan.familylegacy.ui.tools.research.ResearchToolRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;


/** The lists of the Research menu are the dialog of the other Manage commands, and each line says what the old tables did. */
class ResearchManagementTest{

	private FLEFModel model;


	@BeforeEach
	void buildModel(){
		ResearchSummary.invalidate();

		model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("RQ1", ResearchQuestionReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_TITLE, "Origins of the family"))
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_QUESTION, "Where was Anna born?"))
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_STATUS, ResearchQuestionReader.ENUM_STATUS_OPEN)));
		model.addRecord(FLEFRecord.createMainRecord("RQ2", ResearchQuestionReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_TITLE, "Another question")));
		model.addRecord(activity("RA1", "Searched the parish register", "RQ1"));
		model.addRecord(activity("RA2", "Searched the census", "RQ1"));
		model.addRecord(FLEFRecord.createMainRecord("RT1", ResearchTaskReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_DESCRIPTION, "Ask the archive"))
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_QUESTION, "RQ1")));
		model.addRecord(FLEFRecord.createMainRecord("C1", ConclusionReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_ISSUE, "Birthplace"))
			.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_NARRATIVE, "The register says Treviso, and the census agrees."))
			.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_RESEARCH, "RQ1"))
			.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_RESOLVES, "E1"))
			.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_RESOLVES, "E2")));
	}


	private static FLEFRecord activity(final String id, final String action, final String questionId){
		return FLEFRecord.createMainRecord(id, ResearchActivityReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchActivityReader.TAG_ACTION, action))
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_QUESTION, questionId));
	}

	private static void buttons(final Container container, final List<String> texts){
		for(final Component child : container.getComponents()){
			if(child instanceof AbstractButton button)
				texts.add(button.getText());
			if(child instanceof Container nested)
				buttons(nested, texts);
		}
	}


	@Test
	void aQuestionSaysHowManyActivitiesTasksAndConclusionsItHas(){
		final SearchService service = new SearchService(model);

		final String text = service.getDisplayText(model.getRecordById("RQ1"));

		Assertions.assertTrue(text.contains("2 activities, 1 task, 1 conclusion"), text);
		Assertions.assertTrue(service.getDisplayText(model.getRecordById("RQ2")).contains("0 activities, 0 tasks, 0 conclusions"));
	}

	@Test
	void anActivityAndATaskSayTheQuestionTheyBelongToAndTheSearchFindsThemByIt(){
		final SearchService service = new SearchService(model);

		Assertions.assertTrue(service.getDisplayText(model.getRecordById("RA1")).contains("Origins of the family"));
		Assertions.assertTrue(service.getDisplayText(model.getRecordById("RT1")).contains("Origins of the family"));
		//the text of the search is the one of the handler: the question is in it
		Assertions.assertTrue(ResearchActivityHandler.getInstance().getDisplayText(model.getRecordById("RA2"), model).contains("Origins"));
		Assertions.assertTrue(ResearchTaskHandler.getInstance().getDisplayText(model.getRecordById("RT1"), model).contains("Origins"));
	}

	@Test
	void aConclusionSaysWhatItResolvesAndItsNarrative(){
		final String text = new SearchService(model).getDisplayText(model.getRecordById("C1"));

		Assertions.assertTrue(text.contains("resolves 2 records"), text);
		Assertions.assertTrue(text.contains("The register says Treviso"), text);
	}

	@Test
	void theListsOfTheResearchMenuAreEnabledAlsoWhenEmptyAndHaveNoSelectButton() throws InvocationTargetException, InterruptedException{
		final List<ToolOperation> tools = new ArrayList<>();
		tools.addAll(ResearchToolRegistry.planningTools());
		tools.addAll(ResearchToolRegistry.analysisTools());
		final ToolContext empty = new ToolContext(new FLEFModel(), null, null, null);
		//questions, activities, tasks, conclusions, identity hypotheses: the first one can be created from the dialog
		final long manage = tools.stream()
			.filter(tool -> tool.getName().equals(I18N.t("menu.research.research.questions"))
				|| tool.getName().equals(I18N.t("menu.research.research.activities"))
				|| tool.getName().equals(I18N.t("menu.research.research.tasks"))
				|| tool.getName().equals(I18N.t("menu.research.conclusions"))
				|| tool.getName().equals(I18N.t("menu.research.identity.hypotheses")))
			.peek(tool -> Assertions.assertTrue(tool.isEnabled(empty), tool.getName()))
			.count();
		Assertions.assertEquals(5, manage);

		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final List<String> titles = new ArrayList<>();
		final List<String> texts = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			for(final Class<?> handler : List.of(ResearchQuestionHandler.class, ResearchActivityHandler.class, ResearchTaskHandler.class,
					ConclusionHandler.class, IdentityHypothesisHandler.class)){
				@SuppressWarnings("unchecked")
				final RecordSelectionDialog dialog = RecordSelectionDialog.createForManagement(null, new FLEFModel(),
					(Class<? extends io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler<?>>)handler);
				titles.add(dialog.getTitle());
				buttons(dialog, texts);
				dialog.dispose();
			}
		});

		Assertions.assertEquals(List.of("Manage Research Questions", "Manage Research Activities", "Manage Research Tasks",
			"Manage Conclusions", "Manage Identity Hypotheses"), titles);
		Assertions.assertFalse(texts.contains(I18N.t("button.select")), texts.toString());
		Assertions.assertTrue(texts.contains(I18N.t("button.close")), texts.toString());
	}

	@SuppressWarnings("unused")
	private static void unused(final Window window){}

}
