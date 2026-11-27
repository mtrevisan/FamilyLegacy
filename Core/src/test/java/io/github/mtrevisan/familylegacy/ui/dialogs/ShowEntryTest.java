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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.dossier.EditorTab;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.ui.components.quickentry.TabLink;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;


/** The dossier opens the editor of a record on a tab, with the item of the row selected. */
class ShowEntryTest{

	private static void collectLists(final Container container, final List<EntityListPanel> lists){
		for(final Component component : container.getComponents())
			if(component instanceof EntityListPanel list)
				lists.add(list);
			else if(component instanceof Container child)
				collectLists(child, lists);
	}


	@Test
	void theSexRowShowsTheAttributesTabWithTheSexAttributeSelected() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = new FLEFModel();
		final FLEFRecord individual = FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, "Antonia")));
		model.addRecord(individual);
		final FLEFRecord occupation = FLEFRecord.createMainRecord("IA1", IndividualAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, "I1"))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_OCCUPATION))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, "farmer"));
		final FLEFRecord sex = FLEFRecord.createMainRecord("IA2", IndividualAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, "I1"))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_SEX))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, "female"));
		model.addRecord(occupation);
		model.addRecord(sex);

		final String[] shownTab = new String[1];
		final FLEFRecord[] selected = new FLEFRecord[1];
		final List<Throwable> failures = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			try{
				final RecordDialog dialog = IndividualHandler.getInstance()
					.createEditDialog(null, model, individual);
				dialog.showEntry(EditorTab.ATTRIBUTES, sex);

				final Timer closer = new Timer(500, e -> {
					try{
						final BaseRecordDialog base = (BaseRecordDialog)dialog;
						final JPanel tab = (JPanel)base.tabbedPane.getSelectedComponent();
						shownTab[0] = (String)tab.getClientProperty(BaseRecordDialog.PANEL_PROPERTY_NAME);
						final List<EntityListPanel> lists = new ArrayList<>();
						collectLists(tab, lists);
						selected[0] = lists.getFirst().getSelectedItem();
					}
					catch(final Throwable t){
						failures.add(t);
					}
					for(final Window window : Window.getWindows())
						if(window.isShowing())
							window.dispose();
				});
				closer.setRepeats(false);
				closer.start();
				dialog.setVisible(true);
			}
			catch(final Throwable t){
				failures.add(t);
			}
		});

		Assertions.assertTrue(failures.isEmpty(), failures.toString());
		Assertions.assertEquals(EditorTab.ATTRIBUTES, shownTab[0]);
		Assertions.assertNotNull(selected[0]);
		Assertions.assertEquals("IA2", selected[0].getId());
	}

	@Test
	void theNoteAboutOtherBirthsLeadsToTheirParticipation() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = new FLEFModel();
		final FLEFRecord individual = FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, "Antonia")));
		model.addRecord(individual);
		for(final String n : new String[]{"1", "2"}){
			model.addRecord(FLEFRecord.createMainRecord("E" + n, EventReader.TYPE)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, EventReader.ENUM_TYPE_BIRTH)));
			model.addRecord(FLEFRecord.createMainRecord("EP" + n, EventParticipationReader.TYPE)
				.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
					.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, "I1")))
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, "E" + n)));
		}

		final String[] shownTab = new String[1];
		final FLEFRecord[] selected = new FLEFRecord[1];
		final List<Throwable> failures = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			try{
				final RecordDialog dialog = IndividualHandler.getInstance()
					.createEditDialog(null, model, individual);

				final Timer reader = new Timer(1200, e -> {
					try{
						final BaseRecordDialog base = (BaseRecordDialog)dialog;
						final JPanel tab = (JPanel)base.tabbedPane.getSelectedComponent();
						shownTab[0] = (String)tab.getClientProperty(BaseRecordDialog.PANEL_PROPERTY_NAME);
						final List<EntityListPanel> lists = new ArrayList<>();
						collectLists(tab, lists);
						selected[0] = lists.getFirst().getSelectedItem();
					}
					catch(final Throwable t){
						failures.add(t);
					}
					for(final Window window : Window.getWindows())
						if(window.isShowing())
							window.dispose();
				});
				reader.setRepeats(false);
				final Timer clicker = new Timer(500, e -> {
					try{
						final TabLink link = findLink((BaseRecordDialog)dialog);
						link.dispatchEvent(new MouseEvent(link, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 1, 1, 1, false));
					}
					catch(final Throwable t){
						failures.add(t);
					}
					reader.start();
				});
				clicker.setRepeats(false);
				clicker.start();
				dialog.setVisible(true);
			}
			catch(final Throwable t){
				failures.add(t);
			}
		});

		Assertions.assertTrue(failures.isEmpty(), failures.toString());
		Assertions.assertEquals(EditorTab.PARTICIPATIONS, shownTab[0]);
		Assertions.assertNotNull(selected[0]);
		Assertions.assertEquals("EP2", selected[0].getId());
	}

	private static TabLink findLink(final Container container){
		for(final Component component : container.getComponents()){
			if(component instanceof TabLink link && link.isVisible())
				return link;
			if(component instanceof Container child){
				final TabLink found = findLink(child);
				if(found != null)
					return found;
			}
		}
		return null;
	}

}
