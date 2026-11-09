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
package io.github.mtrevisan.familylegacy.ui.assistant.gap;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticActions;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.ResearchQuestionRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.ResearchTaskRecordDialog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


/** The Research Gap Assistant on a real model: what it finds, what it offers, and what a draft does. */
class GapAssistantTest{

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);

	/** Anna and Dino have no death (she is certainly dead, he probably), Anna and Carlo have a child and no marriage. */
	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Anna /Rossi/
		1 SEX F
		1 BIRT
		2 DATE 1 JAN 1890
		0 @I2@ INDI
		1 NAME Carlo /Bianchi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1885
		1 DEAT
		2 DATE 1 JAN 1950
		0 @I3@ INDI
		1 NAME Dino /Bianchi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1930
		1 FAMC @F1@
		0 @F1@ FAM
		1 HUSB @I2@
		1 WIFE @I1@
		1 CHIL @I3@
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static String id(final FLEFModel model, final String name){
		return model.getRecordsByType(IndividualReader.TYPE).stream()
			.filter(r -> IndividualReader.extractPrimaryFullname(r).contains(name))
			.findFirst()
			.orElseThrow()
			.getId();
	}

	private static FLEFRecord question(final String id, final String title, final String status){
		final FLEFRecord question = FLEFRecord.createMainRecord(id, ResearchQuestionReader.TYPE);
		question.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_TITLE, title));
		question.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_QUESTION, "What is " + title + "?"));
		question.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_STATUS, status));
		return question;
	}

	private static List<String> titles(final List<GapFinding> findings){
		return findings.stream()
			.map(f -> f.diagnostic().title())
			.toList();
	}

	private static List<GapFinding> findings(final FLEFModel model){
		return GapChecker.findings(GapDataLoader.load(model, TODAY));
	}


	@Test
	void readsWhatIsMissingFromAnImportedTree() throws IOException{
		final FLEFModel model = convert();

		final List<String> titles = titles(findings(model));

		Assertions.assertTrue(titles.stream().anyMatch(t -> t.equals("No death recorded: Anna Rossi [I1] (born 1890)")), titles.toString());
		Assertions.assertTrue(titles.stream().anyMatch(t -> t.equals("Probably dead, no death recorded: Dino Bianchi [I3] (born 1930)")),
			titles.toString());
		//Carlo has a death; the events of an import have no source
		Assertions.assertTrue(titles.stream().noneMatch(t -> t.contains("Carlo")), titles.toString());
		Assertions.assertTrue(titles.stream().anyMatch(t -> t.endsWith("vital event(s) without a source")), titles.toString());
	}

	@Test
	void aSourceOnTheParticipationOrTheIndividualBacksTheEvent() throws IOException{
		final FLEFModel model = convert();
		final String anna = id(model, "Anna");
		final String carlo = id(model, "Carlo");
		Assertions.assertTrue(GapDataLoader.load(model, TODAY).events().stream().allMatch(e -> e.sourceCount() == 0));

		//a citation on the individual
		model.getRecordById(anna)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TAG_SOURCE, "SC1"));
		//a citation on the participation of Carlo in his events
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationReader.TYPE))
			if(carlo.equals(FLEFRecordHelper.findChild(participation, EventParticipationReader.TAG_PARTICIPANT).getChildren().getFirst().getValue()))
				participation.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_SOURCE, "SC2"));

		final GapData data = GapDataLoader.load(model, TODAY);
		for(final GapData.Event event : data.events()){
			if(event.principals().contains(anna) || event.principals().contains(carlo))
				Assertions.assertTrue(event.sourceCount() > 0, event.toString());
			else
				Assertions.assertEquals(0, event.sourceCount(), event.toString());
		}
	}

	@Test
	void theChildrenOfACoupleWithoutAMarriage() throws IOException{
		final FLEFModel model = convert();
		//the import links the spouses of a family: the research knows no marriage when that link is not there
		final GapData data = GapDataLoader.load(model, TODAY);
		Assertions.assertFalse(data.parents().isEmpty(), "the child has parents");

		for(final FLEFRecord relationship : new ArrayList<>(model.getRecordsByType(RelationshipReader.TYPE)))
			if(FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_TYPE).endsWith(RelationshipReader.ENUM_TYPE_SPOUSE))
				model.removeRecord(relationship.getId());
		final List<GapFinding> found = findings(model).stream()
			.filter(f -> f.diagnostic().title().startsWith("Children without a known marriage"))
			.toList();

		Assertions.assertEquals(1, found.size());
		Assertions.assertEquals(GapDraft.Kind.QUESTION, found.getFirst().draft().kind());
		Assertions.assertEquals(2, found.getFirst().draft().records().size());
	}

	@Test
	void theResearchRecordsAreRead() throws IOException{
		final FLEFModel model = convert();
		model.addRecord(question("RQ1", "Parents of Dino", ResearchQuestionReader.ENUM_STATUS_OPEN));
		model.addRecord(question("RQ2", "Death of Anna", ResearchQuestionReader.ENUM_STATUS_OPEN));
		final FLEFRecord activity = FLEFRecord.createMainRecord("RA1", ResearchActivityReader.TYPE);
		activity.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_QUESTION, "RQ2"));
		activity.addChild(FLEFRecord.createChildWithTagAndValue(ResearchActivityReader.TAG_ACTIVITY_TYPE, "search"));
		activity.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_STATUS, ResearchTaskReader.ENUM_STATUS_COMPLETED));
		activity.addChild(FLEFRecord.createChildWithTagAndValue(ResearchActivityReader.TAG_ACTION, "Searched the parish register"));
		activity.addChild(FLEFRecord.createChildWithTagAndValue(ResearchActivityReader.TAG_RESULT, ResearchActivityReader.ENUM_RESULT_NEGATIVE));
		model.addRecord(activity);
		final FLEFRecord task = FLEFRecord.createMainRecord("RT1", ResearchTaskReader.TYPE);
		task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_DESCRIPTION, "Ask the diocese archive"));
		task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_QUESTION, "RQ2"));
		task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_STATUS, ResearchQuestionReader.ENUM_STATUS_OPEN));
		task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_DUE_DATE, "2020-01-01"));
		model.addRecord(task);

		final GapData data = GapDataLoader.load(model, TODAY);

		Assertions.assertEquals(2, data.questions().size());
		Assertions.assertEquals(List.of("RQ2"), data.activities().getFirst().questionIds());
		Assertions.assertEquals(ResearchActivityReader.ENUM_RESULT_NEGATIVE, data.activities().getFirst().result());
		Assertions.assertEquals(List.of("RQ2"), data.tasks().getFirst().questionIds());
		Assertions.assertNotNull(data.tasks().getFirst().due());

		final List<String> titles = titles(findings(model));
		//RQ1 has no activity; RQ2 has a negative search, and a task that is late
		Assertions.assertTrue(titles.contains("Research question with no activity: Parents of Dino"), titles.toString());
		Assertions.assertTrue(titles.stream().anyMatch(t -> t.startsWith("Search without a follow-up: Searched the parish register")),
			titles.toString());
		Assertions.assertTrue(titles.contains("Overdue task: Ask the diocese archive"), titles.toString());
		Assertions.assertTrue(titles.stream().noneMatch(t -> t.contains("Death of Anna") && t.startsWith("Research question")),
			titles.toString());
	}

	@Test
	void everyFindingCanBeIgnoredAndOpenedAndTheDraftsComeFirst() throws IOException{
		final FLEFModel model = convert();
		model.addRecord(question("RQ1", "Parents of Dino", ResearchQuestionReader.ENUM_STATUS_OPEN));

		final List<Diagnostic> found = new GapAssistantContributor().diagnosticProviders().getFirst()
			.diagnose(new AssistantContext(null, null, model));

		Assertions.assertFalse(found.isEmpty());
		for(final Diagnostic finding : found){
			Assertions.assertTrue(finding.isIgnorable(), finding.title());
			Assertions.assertFalse(DiagnosticActions.of(finding).isEmpty(), finding.title());
			Assertions.assertEquals(io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory.RESEARCH, finding.category());
			Assertions.assertNotNull(finding.action(), finding.title());
		}
		//a gap about a death starts a question, the one about a question without activities starts a task
		Assertions.assertEquals("Create Research Question Draft…", found.stream()
			.filter(f -> f.title().startsWith("No death recorded: Anna")).findFirst().orElseThrow().action().title());
		Assertions.assertEquals("Create Research Task Draft…", found.stream()
			.filter(f -> f.title().startsWith("Research question with no activity")).findFirst().orElseThrow().action().title());
	}

	@Test
	void theFindingsAreListedByUsefulness() throws IOException{
		final FLEFModel model = convert();
		model.addRecord(question("RQ1", "Parents of Dino", ResearchQuestionReader.ENUM_STATUS_OPEN));
		final List<Diagnostic> found = new GapAssistantContributor().diagnosticProviders().getFirst()
			.diagnose(new AssistantContext(null, null, model));

		final List<Double> ranks = found.stream().map(Diagnostic::rank).toList();

		Assertions.assertTrue(ranks.stream().distinct().count() > 1, ranks.toString());
		//the contributor gives the findings with their rank; the service lists them by it
		Assertions.assertTrue(found.stream().allMatch(f -> f.rank() >= 25 && f.rank() <= 100));
	}

	@Test
	void aQuestionDraftIsSavedOnlyIfTheResearcherSavesIt() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The editor needs a display");
		final FLEFModel model = convert();
		final FLEFRecord anna = model.getRecordById(id(model, "Anna"));
		final GapFinding finding = findings(model).stream()
			.filter(f -> f.diagnostic().title().startsWith("No death recorded: Anna")).findFirst().orElseThrow();

		//what the action does, but for showing the editor
		final ResearchQuestionRecordDialog dialog = ResearchQuestionRecordDialog.createNew(null, model)
			.withDraft(finding.draft().title(), finding.draft().text(), List.of(anna));

		Assertions.assertTrue(model.getRecordsByType(ResearchQuestionReader.TYPE).isEmpty());
		Assertions.assertFalse(dialog.isSaved());

		dialog.save();

		Assertions.assertTrue(dialog.isSaved());
		final List<FLEFRecord> questions = model.getRecordsByType(ResearchQuestionReader.TYPE);
		Assertions.assertEquals(1, questions.size());
		Assertions.assertEquals("Death of Anna Rossi", FLEFRecordHelper.getChildValue(questions.getFirst(), ResearchQuestionReader.TAG_TITLE));
		Assertions.assertEquals("When and where did Anna Rossi die?", FLEFRecordHelper.getChildValue(questions.getFirst(), ResearchQuestionReader.TAG_QUESTION));
		Assertions.assertEquals(1, FLEFRecordHelper.findChildren(questions.getFirst(), ResearchQuestionReader.TAG_TARGET).size());
		//the gap is not filled: it is a question to answer, and the individual still has no death
		Assertions.assertTrue(titles(findings(model)).contains("No death recorded: Anna Rossi [I1] (born 1890)"));
	}

	@Test
	void aTaskDraftIsSavedOnlyIfTheResearcherSavesIt() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The editor needs a display");
		final FLEFModel model = convert();
		final FLEFRecord question = question("RQ1", "Parents of Dino", ResearchQuestionReader.ENUM_STATUS_OPEN);
		model.addRecord(question);
		final GapFinding finding = findings(model).stream()
			.filter(f -> f.diagnostic().title().startsWith("Research question with no activity")).findFirst().orElseThrow();

		final ResearchTaskRecordDialog dialog = ResearchTaskRecordDialog.createNew(null, model)
			.withDraft(finding.draft().text(), finding.draft().priority(), List.of(question));

		Assertions.assertTrue(model.getRecordsByType(ResearchTaskReader.TYPE).isEmpty());

		dialog.save();

		Assertions.assertTrue(dialog.isSaved());
		final List<FLEFRecord> tasks = model.getRecordsByType(ResearchTaskReader.TYPE);
		Assertions.assertEquals(1, tasks.size());
		Assertions.assertTrue(FLEFRecordHelper.getChildValue(tasks.getFirst(), ResearchTaskReader.TAG_DESCRIPTION).contains("Parents of Dino"));
		Assertions.assertEquals(ResearchQuestionReader.ENUM_STATUS_OPEN, FLEFRecordHelper.getChildValue(tasks.getFirst(), ResearchTaskReader.TAG_STATUS));
		Assertions.assertEquals(ResearchTaskReader.ENUM_PRIORITY_NORMAL, FLEFRecordHelper.getChildValue(tasks.getFirst(), ResearchTaskReader.TAG_PRIORITY));
		Assertions.assertEquals("RQ1", FLEFRecordHelper.getChildValue(tasks.getFirst(), ResearchTaskReader.TAG_QUESTION));
	}

	@Test
	void cancellingADraftLeavesTheModelAsItWas() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The editor needs a display");
		final FLEFModel model = convert();
		final int records = model.getRecords().size();

		final ResearchQuestionRecordDialog dialog = ResearchQuestionRecordDialog.createNew(null, model)
			.withDraft("title", "question", List.of(model.getRecordById(id(model, "Anna"))));
		//ESC, which is Cancel
		final Object binding = dialog.getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
			.get(io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper.ESCAPE_STROKE);
		dialog.getRootPane().getActionMap().get(binding)
			.actionPerformed(new ActionEvent(dialog, ActionEvent.ACTION_PERFORMED, "escape"));

		Assertions.assertFalse(dialog.isSaved());
		Assertions.assertEquals(records, model.getRecords().size());
	}

	@Test
	void anEmptyModelHasNothingToReport() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader("0 HEAD\n1 CHAR UTF-8\n"));

		Assertions.assertTrue(findings(new GEDCOMToFLEFConverter().convert(roots)).isEmpty());
	}

}
