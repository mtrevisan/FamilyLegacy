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
package io.github.mtrevisan.familylegacy.ui.tools.dates;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.dates.HistoricalDateAssistantDialog;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.tools.events.EventToolRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


/** Where the date tools are in the menus, and how the Historical Date Assistant opens from them. */
class DateToolsTest{

	private static void collect(final Container container, final List<String> buttons, final List<JTable> tables){
		for(final Component child : container.getComponents()){
			if(child instanceof AbstractButton button)
				buttons.add(button.getText());
			if(child instanceof JTable table)
				tables.add(table);
			if(child instanceof Container nested)
				collect(nested, buttons, tables);
		}
	}

	@Test
	void theHistoricalDateAssistantHasTheCalendarTableAndNoApplyWithoutADate() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");

		final List<String> assistantButtons = new ArrayList<>();
		final List<JTable> assistantTables = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			final HistoricalDateAssistantDialog assistant = new HistoricalDateAssistantDialog(null,
				HistoricalDateAssistantDialog.Tool.CONVERSION, null, null, new FLEFModel(), null);
			collect(assistant, assistantButtons, assistantTables);
			assistant.dispose();
		});

		//the table of the calendars is in the conversion tool, with the button to copy it
		Assertions.assertEquals(1, assistantTables.size());
		Assertions.assertTrue(assistantButtons.contains(I18N.t("button.copy_all")), assistantButtons.toString());
		//opened from a menu there is no date to apply a suggestion to
		Assertions.assertFalse(assistantButtons.contains(I18N.t("dialog.date_assistant.apply")), assistantButtons.toString());
		Assertions.assertTrue(assistantButtons.contains(I18N.t("dialog.date_assistant.copy")), assistantButtons.toString());
	}

	@Test
	void theDateToolsAreInTheToolsMenuAndNotInTheEventMenu(){
		final List<String> dateTools = DateToolRegistry.tools().stream()
			.map(ToolOperation::getName)
			.toList();
		final List<String> eventTools = Stream.of(EventToolRegistry.primaryTools(), EventToolRegistry.referenceTools(),
				EventToolRegistry.analysisTools())
			.flatMap(List::stream)
			.map(ToolOperation::getName)
			.toList();

		Assertions.assertEquals(1, dateTools.size());
		Assertions.assertTrue(dateTools.stream().noneMatch(eventTools::contains), dateTools + " / " + eventTools);
		Assertions.assertTrue(dateTools.stream().noneMatch(String::isBlank));
	}

}
