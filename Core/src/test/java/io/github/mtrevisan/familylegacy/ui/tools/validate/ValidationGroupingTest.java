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
package io.github.mtrevisan.familylegacy.ui.tools.validate;

import io.github.mtrevisan.familylegacy.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


/** The report is grouped by the kind of problem: the groups, their order, the filter by severity, and closing a group. */
class ValidationGroupingTest{

	private static FLEFValidator.ValidationError error(final String message, final String recordId, final String kind){
		return new FLEFValidator.ValidationError(message, recordId, FLEFValidator.Severity.ERROR, kind);
	}

	private static FLEFValidator.ValidationError warning(final String message, final String recordId, final String kind){
		return new FLEFValidator.ValidationError(message, recordId, FLEFValidator.Severity.WARNING, kind);
	}

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		for(final String id : List.of("I1", "I2", "I3"))
			model.addRecord(FLEFRecord.createMainRecord(id, IndividualReader.TYPE));
		return model;
	}

	private static List<String> kinds(final List<ValidationReportDialog.Group> groups){
		return groups.stream().map(ValidationReportDialog.Group::kind).toList();
	}


	@Test
	void theProblemsAreGroupedByKindAndTheGroupsWithErrorsComeFirst(){
		final List<FLEFValidator.ValidationError> errors = List.of(
			warning("m1", "I1", "media_missing"), warning("m2", "I1", "media_missing"), warning("m3", "I2", "media_missing"),
			warning("n1", "I3", "no_name"),
			error("c1", "I1", "cycle"),
			error("p1", "I1", "parents"), error("p2", "I2", "parents"));

		final List<ValidationReportDialog.Group> groups = ValidationReportDialog.groupsOf(errors, model());

		//the groups with errors (the smaller before the larger), then the ones with warnings only (the same)
		Assertions.assertEquals(List.of("cycle", "parents", "no_name", "media_missing"), kinds(groups));
		Assertions.assertEquals(3, groups.getLast().errors().size());
		Assertions.assertEquals("Media files not found", groups.getLast().label());
	}

	@Test
	void aGroupWithErrorsAndWarningsIsAnErrorGroup(){
		final List<FLEFValidator.ValidationError> errors = List.of(
			warning("w", "I1", "a_kind"), error("e", "I2", "a_kind"), warning("n1", "I3", "no_name"));

		final List<ValidationReportDialog.Group> groups = ValidationReportDialog.groupsOf(errors, model());

		Assertions.assertEquals(List.of("a_kind", "no_name"), kinds(groups));
		Assertions.assertTrue(groups.getFirst().hasErrors());
		Assertions.assertFalse(groups.getLast().hasErrors());
		//a kind that has no text of its own is called by its key
		Assertions.assertEquals("a_kind", groups.getFirst().label());
	}

	@Test
	void whatTheGrammarFindsIsGroupedByTheTypeOfTheRecordOrByTheFile(){
		final List<FLEFValidator.ValidationError> errors = List.of(
			new FLEFValidator.ValidationError("x", "I1"), new FLEFValidator.ValidationError("y", "I2"),
			new FLEFValidator.ValidationError("z", null), new FLEFValidator.ValidationError("w", "I404"));

		final List<ValidationReportDialog.Group> groups = ValidationReportDialog.groupsOf(errors, model());

		//the two of individuals, and the two that are about no record that exists, are one group each (the same size: by name)
		Assertions.assertEquals(List.of("grammar.individual", "grammar"), kinds(groups));
		Assertions.assertEquals(2, groups.getFirst().errors().size());
		Assertions.assertEquals("Grammar: Individual", groups.getFirst().label());
		Assertions.assertEquals("Grammar: the file as a whole", groups.getLast().label());
	}

	@Test
	void noProblemsHaveNoGroups(){
		Assertions.assertTrue(ValidationReportDialog.groupsOf(List.of(), model()).isEmpty());
	}


	private static JTable table(final Container container){
		for(final Component child : container.getComponents()){
			if(child instanceof JTable table)
				return table;
			if(child instanceof Container nested){
				final JTable found = table(nested);
				if(found != null)
					return found;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static JComboBox<Object> combo(final Container container){
		for(final Component child : container.getComponents()){
			if(child instanceof JComboBox<?> combo)
				return (JComboBox<Object>)combo;
			if(child instanceof Container nested){
				final JComboBox<Object> found = combo(nested);
				if(found != null)
					return found;
			}
		}
		return null;
	}

	private static ValidationReportDialog dialog(final List<FLEFValidator.ValidationError> errors) throws Exception{
		final FLEFModel model = model();
		final AtomicReference<ValidationReportDialog> dialog = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> dialog.set(new ValidationReportDialog(null, model, errors, () -> model, view -> List.of())));
		return dialog.get();
	}

	@Test
	void theLinesAreTheGroupsAndTheirProblemsAndTheFilterShowsTheErrorsOrTheWarningsOnly() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final ValidationReportDialog dialog = dialog(List.of(error("c1", "I1", "cycle"), warning("n1", "I3", "no_name"),
			warning("n2", "I2", "no_name")));
		final JTable table = table(dialog);
		final JComboBox<Object> filter = combo(dialog);
		final List<Integer> rows = new ArrayList<>();

		SwingUtilities.invokeAndWait(() -> {
			//the two groups are closed: only their lines
			rows.add(table.getRowCount());
			//open both: cycle (1 line + 1 problem), no_name (1 line + 2 problems)
			click(table, 1);
			click(table, 0);
			rows.add(table.getRowCount());
			filter.setSelectedIndex(1);
			rows.add(table.getRowCount());
			filter.setSelectedIndex(2);
			rows.add(table.getRowCount());
			filter.setSelectedIndex(0);
			rows.add(table.getRowCount());
			dialog.dispose();
		});

		//the groups that were opened stay open when the filter changes
		Assertions.assertEquals(List.of(2, 5, 2, 3, 5), rows);
	}

	@Test
	void theGroupsStartClosedAndAClickOnTheLineOfAGroupOpensItAndAnotherClosesIt() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final ValidationReportDialog dialog = dialog(List.of(error("c1", "I1", "cycle"), warning("n1", "I3", "no_name"),
			warning("n2", "I2", "no_name")));
		final JTable table = table(dialog);
		final List<Integer> rows = new ArrayList<>();
		final List<String> texts = new ArrayList<>();

		SwingUtilities.invokeAndWait(() -> {
			//the groups start closed: the line of each, with the arrow of a closed one and the number of its problems
			rows.add(table.getRowCount());
			texts.add(String.valueOf(table.getValueAt(1, 1)));
			//the second group (no_name, the larger): a click opens it, another closes it
			click(table, 1);
			rows.add(table.getRowCount());
			texts.add(String.valueOf(table.getValueAt(1, 1)));
			click(table, 1);
			rows.add(table.getRowCount());
			texts.add(String.valueOf(table.getValueAt(1, 1)));
			dialog.dispose();
		});

		Assertions.assertEquals(List.of(2, 4, 2), rows);
		//the arrow tells whether it is closed or open, and the count is of the problems of the group
		Assertions.assertTrue(texts.get(0).startsWith("\u25B6") && texts.get(0).contains("(2)"), texts.get(0));
		Assertions.assertTrue(texts.get(1).startsWith("\u25BC") && texts.get(1).contains("(2)"), texts.get(1));
		Assertions.assertTrue(texts.get(2).startsWith("\u25B6"), texts.get(2));
	}

	private static void click(final JTable table, final int row){
		final Rectangle cell = table.getCellRect(row, 1, true);
		final Point point = new Point(cell.x + 5, cell.y + cell.height / 2);
		table.dispatchEvent(new MouseEvent(table, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, point.x, point.y, 1, false,
			MouseEvent.BUTTON1));
	}

}
