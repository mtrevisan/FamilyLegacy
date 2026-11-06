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
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.ui.tools.context.ContextBoardDialog;
import io.github.mtrevisan.familylegacy.ui.tools.events.EventToolRegistry;
import io.github.mtrevisan.familylegacy.ui.tools.events.TimelineViewDialog;
import io.github.mtrevisan.familylegacy.ui.tools.research.ResearchToolRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;


/** The dialogs "Manage ..." manage the records, they do not pick one: there is no Select button, and the dialog is closed with Close. */
class ManagementDialogTest{

	private static List<String> buttons(final Container container){
		final List<String> texts = new ArrayList<>();
		for(final Component child : container.getComponents()){
			if(child instanceof AbstractButton button)
				texts.add(button.getText());
			if(child instanceof Container nested)
				texts.addAll(buttons(nested));
		}
		return texts;
	}

	private static List<String> buttonsOf(final boolean manage) throws InvocationTargetException, InterruptedException{
		final List<String> texts = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			final RecordSelectionDialog dialog = (manage
				? RecordSelectionDialog.createForManagement(null, new FLEFModel(), EventHandler.class)
				: RecordSelectionDialog.create(null, new FLEFModel(), (record, handler) -> {}, SourceHandler.class));
			texts.addAll(buttons(dialog));
			dialog.dispose();
		});
		return texts;
	}


	@Test
	void theManagementDialogHasNoSelectButtonAndIsClosedWithClose() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");

		final List<String> texts = buttonsOf(true);

		Assertions.assertFalse(texts.contains(I18N.t("button.select")), texts.toString());
		Assertions.assertFalse(texts.contains(I18N.t("button.cancel")), texts.toString());
		Assertions.assertTrue(texts.contains(I18N.t("button.close")), texts.toString());
		Assertions.assertTrue(texts.contains(I18N.t("button.new")), texts.toString());
		Assertions.assertTrue(texts.contains(I18N.t("button.edit")), texts.toString());
		Assertions.assertTrue(texts.contains(I18N.t("button.delete")), texts.toString());
	}

	@Test
	void theParticipationsCanBeManagedAndSearched() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");

		Assertions.assertTrue(SearchService.searchableHandlers().contains(EventParticipationHandler.class));
		Assertions.assertNotNull(FilterPanelFactory.createPanel(HandlerRegistry.getHandler(EventParticipationHandler.class), criteria -> {}));

		final List<String> texts = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			final RecordSelectionDialog dialog = RecordSelectionDialog.createForManagement(null, new FLEFModel(),
				EventParticipationHandler.class);
			texts.addAll(buttons(dialog));
			dialog.dispose();
		});
		Assertions.assertFalse(texts.contains(I18N.t("button.select")), texts.toString());
		Assertions.assertTrue(texts.contains(I18N.t("button.close")), texts.toString());
	}

	@Test
	void theTimelineOfTheSelectedIndividualShowsNamesAndPlacesAndNotIdentifiers() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");

		final FLEFModel model = new FLEFParser().parse("""
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
			place P1 {
				name {
					value Treviso
				}
			}
			""");
		model.addRecord(FLEFRecord.createMainRecord("E1", EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, EventReader.ENUM_TYPE_BIRTH))
			.addChild(FLEFRecord.createChildWithTag(EventReader.TAG_PLACE).addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_PLACE, "P1"))));
		model.addRecord(FLEFRecord.createMainRecord("E2", EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, EventReader.ENUM_TYPE_DEATH)));
		model.addRecord(FLEFRecord.createMainRecord("EP1", EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT).addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, "I1")))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, "E1")));

		final List<JTable> tables = new ArrayList<>();
		final List<String> participant = new ArrayList<>();
		final List<String> row = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			final TimelineViewDialog dialog = new TimelineViewDialog(new ToolContext(model, "I1", null, null));
			collect(dialog, new ArrayList<>(), tables);
			final JTable table = tables.getFirst();
			participant.add(String.valueOf(table.getRowCount()));
			for(int column = 0; column < table.getColumnCount(); column ++)
				row.add(String.valueOf(table.getValueAt(0, column)));
			dialog.dispose();
		});

		//the selected individual is the participant filter: only its event is listed
		Assertions.assertEquals("1", participant.getFirst());
		Assertions.assertEquals(EventReader.ENUM_TYPE_BIRTH, row.get(1));
		Assertions.assertEquals("Treviso", row.get(2));
		Assertions.assertTrue(row.get(3).contains("Anna"), row.toString());
	}

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

	private static void texts(final Container container, final List<String> areas){
		for(final Component child : container.getComponents()){
			if(child instanceof JTextArea area)
				areas.add(area.getText());
			if(child instanceof Container nested)
				texts(nested, areas);
		}
	}

	@Test
	void theContextBoardIsInTheResearchMenuAndExplainsItselfWhenEmpty() throws InvocationTargetException, InterruptedException{
		final List<String> research = new ArrayList<>();
		ResearchToolRegistry.contextTools().forEach(tool -> research.add(tool.getName()));
		final List<String> event = new ArrayList<>();
		for(final List<ToolOperation> tools : List.of(EventToolRegistry.primaryTools(), EventToolRegistry.referenceTools(),
				EventToolRegistry.analysisTools()))
			tools.forEach(tool -> event.add(tool.getName()));
		Assertions.assertEquals(List.of(I18N.t("menu.research.context_board")), research);
		Assertions.assertFalse(event.contains(research.getFirst()), event.toString());

		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");

		final List<String> buttons = new ArrayList<>();
		final List<String> areas = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			final ContextBoardDialog dialog = new ContextBoardDialog(new ToolContext(new FLEFModel(), null, null, null));
			buttons.addAll(buttons(dialog));
			texts(dialog, areas);
			dialog.dispose();
		});
		Assertions.assertTrue(buttons.contains(I18N.t("button.delete")), buttons.toString());
		Assertions.assertTrue(areas.contains(I18N.t("text.context.empty")), areas.toString());
	}

	@Test
	void theSelectionDialogKeepsTheSelectButton() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");

		final List<String> texts = buttonsOf(false);

		Assertions.assertTrue(texts.contains(I18N.t("button.select")), texts.toString());
		Assertions.assertTrue(texts.contains(I18N.t("button.cancel")), texts.toString());
		Assertions.assertFalse(texts.contains(I18N.t("button.close")), texts.toString());
	}

}
