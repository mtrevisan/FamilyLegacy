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
package io.github.mtrevisan.familylegacy.ui.components.dates;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;


/**
 * The button that runs a tool of the assistant is disabled while its result is on display: it is enabled again when the input
 * changes or another tool is chosen, and the result is cleared in the second case.
 */
class AssistantRunButtonTest{

	private static <T extends Component> List<T> find(final Container container, final Class<T> type){
		final List<T> result = new ArrayList<>();
		for(final Component component : container.getComponents()){
			if(type.isInstance(component))
				result.add(type.cast(component));
			if(component instanceof Container child)
				result.addAll(find(child, type));
		}
		return result;
	}

	private static JList<?> suggestions(final HistoricalDateAssistantDialog dialog){
		final JTabbedPane tabs = find(dialog, JTabbedPane.class).get(0);
		//the list of the suggestions is the one outside the tools (the combo boxes can have their own)
		return find(dialog, JList.class).stream()
			.filter(list -> !SwingUtilities.isDescendingFrom(list, tabs))
			.findFirst().orElseThrow();
	}


	@Test
	void runIsDisabledUntilTheInputChanges() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");

		SwingUtilities.invokeAndWait(() -> {
			final HistoricalDateAssistantDialog dialog = new HistoricalDateAssistantDialog(null,
				HistoricalDateAssistantDialog.Tool.VERIFY, "gregorian", "12 January 1400", null, null);
			dialog.setVisible(false);
			final JTabbedPane tabs = find(dialog, JTabbedPane.class).get(0);
			final JButton run = findYearsButton(tabs);
			Assertions.assertTrue(run.isEnabled());

			run.doClick();
			Assertions.assertFalse(run.isEnabled());
			Assertions.assertFalse(suggestions(dialog).getModel().getSize() == 0);

			//the input changes: the tool can run again
			final JSpinner indiction = find((Container)tabs.getSelectedComponent(), JSpinner.class).get(0);
			indiction.setValue(2);
			Assertions.assertTrue(run.isEnabled());

			//another tab: enabled, and the output is gone
			run.doClick();
			Assertions.assertFalse(run.isEnabled());
			tabs.setSelectedIndex((tabs.getSelectedIndex() + 1) % tabs.getTabCount());
			Assertions.assertTrue(run.isEnabled());
			Assertions.assertEquals(0, suggestions(dialog).getModel().getSize());
			dialog.dispose();
		});
	}

	private static JButton findYearsButton(final JTabbedPane tabs){
		final List<JButton> buttons = find((Container)tabs.getSelectedComponent(), JButton.class).stream()
			.filter(button -> I18N.t("dialog.date_assistant.run").equals(button.getText()))
			.toList();
		return buttons.get(0);
	}

}
