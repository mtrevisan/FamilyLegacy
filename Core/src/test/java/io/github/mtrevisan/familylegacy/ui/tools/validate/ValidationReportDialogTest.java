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

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/** The report shows what was found, by the name of the record, without validating again; "Re-run" validates in the background. */
class ValidationReportDialogTest{

	private static final String FLEF = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		individual I1 {
			name {
				part {
					type given
					value Anna
				}
			}
		}
		""";


	private static void collect(final Container container, final List<JLabel> labels, final List<JTable> tables, final List<AbstractButton> buttons){
		for(final Component child : container.getComponents()){
			if(child instanceof JLabel label)
				labels.add(label);
			if(child instanceof JTable table)
				tables.add(table);
			if(child instanceof AbstractButton button)
				buttons.add(button);
			if(child instanceof Container nested)
				collect(nested, labels, tables, buttons);
		}
	}


	private static void click(final JTable table, final int row){
		final java.awt.Rectangle cell = table.getCellRect(row, 1, true);
		table.dispatchEvent(new java.awt.event.MouseEvent(table, java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
			cell.x + 5, cell.y + cell.height / 2, 1, false, java.awt.event.MouseEvent.BUTTON1));
	}


	@Test
	void theReportShowsWhatWasFoundWithoutValidatingAgainAndTheRecordByName() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = new FLEFParser().parse(FLEF);
		final AtomicInteger validations = new AtomicInteger();
		final List<String> summary = new ArrayList<>();
		final List<String> cells = new ArrayList<>();

		SwingUtilities.invokeAndWait(() -> {
			final ValidationReportDialog dialog = new ValidationReportDialog(null, model,
				List.of(new FLEFValidator.ValidationError("The birth is after the death", "I1"),
					new FLEFValidator.ValidationError("A problem of the file", null)),
				() -> model, view -> {
					validations.incrementAndGet();
					return List.of();
				});
			final List<JLabel> labels = new ArrayList<>();
			final List<JTable> tables = new ArrayList<>();
			collect(dialog, labels, tables, new ArrayList<>());
			labels.forEach(label -> summary.add(label.getText()));
			final JTable table = tables.getFirst();
			//the groups start closed: open both (the one of the file is the second, then the first one moves down by one)
			click(table, 1);
			click(table, 0);
			//the lines are: the group of the individual, its problem, the group of the file, its problem
			cells.add(String.valueOf(table.getValueAt(1, 2)));
			cells.add(String.valueOf(table.getValueAt(3, 2)));
			dialog.dispose();
		});

		//what was found is what is shown: nothing was validated to open the dialog
		Assertions.assertEquals(0, validations.get());
		Assertions.assertTrue(summary.contains(I18N.tf("text.validate.issues", 2)), summary.toString());
		Assertions.assertTrue(cells.get(0).contains("Anna"), cells.toString());
		Assertions.assertEquals("—", cells.get(1));
	}

	@Test
	void reRunValidatesInTheBackgroundOnACopyOfTheModel() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = new FLEFParser().parse(FLEF);
		final FLEFModel copy = new FLEFParser().parse(FLEF);
		final List<FLEFModel> validated = new ArrayList<>();
		final List<Thread> threads = new ArrayList<>();
		final ValidationReportDialog[] dialog = new ValidationReportDialog[1];
		final List<AbstractButton> buttons = new ArrayList<>();
		final List<JLabel> labels = new ArrayList<>();

		SwingUtilities.invokeAndWait(() -> {
			dialog[0] = new ValidationReportDialog(null, model, List.of(new FLEFValidator.ValidationError("x", "I1")), () -> copy, view -> {
				validated.add(view);
				threads.add(Thread.currentThread());
				return List.of();
			});
			collect(dialog[0], labels, new ArrayList<>(), buttons);
			buttons.stream()
				.filter(button -> I18N.t("button.rerun").equals(button.getText()))
				.findFirst()
				.orElseThrow()
				.doClick();
		});

		final long deadline = System.currentTimeMillis() + 20_000;
		while(validated.isEmpty() && System.currentTimeMillis() < deadline)
			Thread.sleep(50);
		final String[] text = new String[1];
		//the label changes when the worker is done, which is after the validation was called: wait for it, do not assume
		do{
			SwingUtilities.invokeAndWait(() -> text[0] = labels.stream()
				.map(JLabel::getText)
				.filter(label -> label != null && label.contains("valid"))
				.findFirst()
				.orElse(""));
			if(text[0].isEmpty())
				Thread.sleep(50);
		}while(text[0].isEmpty() && System.currentTimeMillis() < deadline);
		SwingUtilities.invokeAndWait(() -> dialog[0].dispose());

		Assertions.assertEquals(1, validated.size());
		Assertions.assertSame(copy, validated.getFirst(), "it is the copy that is read, not the model of the user");
		Assertions.assertNotEquals(Thread.currentThread(), threads.getFirst());
		Assertions.assertFalse(SwingUtilities.isEventDispatchThread());
		Assertions.assertEquals(I18N.t("text.validate.valid"), text[0]);
	}

}
