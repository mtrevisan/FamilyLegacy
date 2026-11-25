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
package io.github.mtrevisan.familylegacy.ui.tools.research;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;


/** The GPS workflow dialog, built on a model with a research question: the stages, the elements and what is missing are listed. */
class GpsWorkflowDialogTest{

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("RS1", ResearchQuestionReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_TITLE, "When was Anna born?"))
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_QUESTION, "When was Anna born?"))
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_STATUS, ResearchQuestionReader.ENUM_STATUS_OPEN)));
		return model;
	}

	private static <T extends Component> void find(final Container container, final Class<T> type, final List<T> found){
		for(final Component child : container.getComponents()){
			if(type.isInstance(child))
				found.add(type.cast(child));
			if(child instanceof Container nested)
				find(nested, type, found);
		}
	}


	@Test
	void theStagesTheElementsAndWhatIsMissingAreListed() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = model();

		final int[] rows = new int[4];
		SwingUtilities.invokeAndWait(() -> {
			final GpsWorkflowDialog dialog = new GpsWorkflowDialog(null, model, null);
			final List<JTable> tables = new ArrayList<>();
			find(dialog, JTable.class, tables);
			Assertions.assertEquals(3, tables.size());
			rows[0] = tables.get(0).getRowCount();
			rows[1] = tables.get(1).getRowCount();
			rows[2] = tables.get(2).getRowCount();
			final List<JComboBox> combos = new ArrayList<>();
			find(dialog, JComboBox.class, combos);
			rows[3] = combos.getFirst().getItemCount();
			dialog.dispose();
		});

		Assertions.assertEquals(5, rows[0]);
		Assertions.assertEquals(5, rows[1]);
		//no target, no search, no conclusion
		Assertions.assertEquals(3, rows[2]);
		Assertions.assertEquals(1, rows[3]);
	}

	@Test
	void aFileWithoutQuestionsHasNothingToAssess() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");

		final int[] rows = new int[1];
		SwingUtilities.invokeAndWait(() -> {
			final GpsWorkflowDialog dialog = new GpsWorkflowDialog(null, new FLEFModel(), null);
			final List<JTable> tables = new ArrayList<>();
			find(dialog, JTable.class, tables);
			rows[0] = tables.get(2).getRowCount();
			dialog.dispose();
		});

		Assertions.assertEquals(0, rows[0]);
	}

	@Test
	void aConclusionThatIsOpenedFromAnswersAsTheSubject() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = model();
		final FLEFRecord conclusion = FLEFRecord.createMainRecord("CC1", ConclusionReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_ISSUE, "birth_date"));
		model.addRecord(conclusion);

		final Object[] selected = new Object[1];
		SwingUtilities.invokeAndWait(() -> {
			final GpsWorkflowDialog dialog = new GpsWorkflowDialog(null, model, conclusion);
			final List<JComboBox> combos = new ArrayList<>();
			find(dialog, JComboBox.class, combos);
			selected[0] = combos.getFirst().getSelectedItem();
			dialog.dispose();
		});

		Assertions.assertSame(conclusion, selected[0]);
	}

	@Test
	void theProofStatementIsWrittenFromTheModelAndNothingIsSavedUntilAsked() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = model();
		final FLEFRecord question = model.getRecordById("RS1");

		SwingUtilities.invokeAndWait(() -> {
			final GpsProofStatementDialog dialog = new GpsProofStatementDialog(null, model, question);
			final List<javax.swing.JTextArea> areas = new ArrayList<>();
			find(dialog, javax.swing.JTextArea.class, areas);
			Assertions.assertTrue(areas.getFirst().getText().contains("When was Anna born?"));
			dialog.dispose();
		});

		Assertions.assertEquals(0, FLEFRecordHelper.findChildren(question, NoteReader.TAG_NOTE).size());
	}

}
