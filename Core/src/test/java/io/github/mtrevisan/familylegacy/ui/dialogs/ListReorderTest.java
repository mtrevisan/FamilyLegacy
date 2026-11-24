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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.preferences.UxLevel;
import io.github.mtrevisan.familylegacy.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.ui.features.FeatureVisibilityService;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;


/** The user can arrange the items of any list of an editor, and the order is the one the items have in the file. */
class ListReorderTest{

	@BeforeAll
	static void standardLevel(){
		//(the level is a preference of the user: the tests say the one they need)
		BaseRecordDialog.useLevelForStories(FeatureVisibilityService.inMemory(UxLevel.STANDARD));
	}

	@AfterAll
	static void userLevel(){
		BaseRecordDialog.useLevelForStories(null);
	}


	private static FLEFRecord attribute(final String id, final String value){
		return FLEFRecord.createMainRecord(id, IndividualAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, "I1"))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_OCCUPATION))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, value));
	}

	private static FLEFRecord note(final String text){
		return FLEFRecord.createChildWithTag(NoteReader.TAG_NOTE)
			.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TEXT, text));
	}

	private static List<String> values(final List<FLEFRecord> records){
		return records.stream()
			.map(record -> FLEFRecordHelper.getChildValue(record, IndividualAttributeReader.TAG_VALUE))
			.toList();
	}


	@Test
	void theRecordsOfAListAreSavedInTheOrderTheUserGaveThem() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = new FLEFModel();
		final FLEFRecord individual = FLEFRecord.createMainRecord("I1", IndividualReader.TYPE);
		model.addRecord(individual);
		model.addRecord(attribute("IA1", "baker"));
		model.addRecord(attribute("IA2", "farmer"));
		model.addRecord(attribute("IA3", "mayor"));

		final List<String> before = new ArrayList<>();
		final List<Throwable> failures = new ArrayList<>();
		final Object[] reorderable = new Object[1];
		SwingUtilities.invokeAndWait(() -> {
			try{
				final BaseRecordDialog dialog = (BaseRecordDialog)IndividualHandler.getInstance()
					.createEditDialog(null, model, individual);
				final EntityListPanel attributes = (EntityListPanel)dialog.components.getPanel(PanelKey.INDIVIDUAL_ATTRIBUTE);
				before.addAll(values(attributes.getItems()));
				reorderable[0] = attributes.getList().getClientProperty(GUIHelper.PROPERTY_REORDERABLE);

				//the user moves the last one up twice (Ctrl + Up, or the menu)
				attributes.getList().setSelectedIndex(2);
				GUIHelper.moveSelectedItemUp(attributes.getList(), (javax.swing.DefaultListModel<FLEFRecord>)attributes.getList().getModel());
				GUIHelper.moveSelectedItemUp(attributes.getList(), (javax.swing.DefaultListModel<FLEFRecord>)attributes.getList().getModel());

				dialog.components.save(individual);
				dialog.dispose();
			}
			catch(final Throwable t){
				failures.add(t);
			}
		});

		Assertions.assertTrue(failures.isEmpty(), failures.toString());
		Assertions.assertEquals(Boolean.TRUE, reorderable[0]);
		Assertions.assertEquals(List.of("baker", "farmer", "mayor"), before);
		Assertions.assertEquals(List.of("mayor", "baker", "farmer"), values(model.getRecordsByType(IndividualAttributeReader.TYPE)));
	}

	@Test
	void theNotesOfAListAreSavedInTheOrderTheUserGaveThem() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = new FLEFModel();
		final FLEFRecord individual = FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(note("first"))
			.addChild(note("second"))
			.addChild(note("third"));
		model.addRecord(individual);

		final List<String> saved = new ArrayList<>();
		final List<Throwable> failures = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			try{
				final BaseRecordDialog dialog = (BaseRecordDialog)IndividualHandler.getInstance()
					.createEditDialog(null, model, individual);
				final EntityListPanel notes = (EntityListPanel)dialog.components.getPanel(PanelKey.NOTE_EXCEPT_STORIES);

				notes.getList().setSelectedIndex(0);
				GUIHelper.moveSelectedItemDown(notes.getList(), (javax.swing.DefaultListModel<FLEFRecord>)notes.getList().getModel());
				GUIHelper.moveSelectedItemDown(notes.getList(), (javax.swing.DefaultListModel<FLEFRecord>)notes.getList().getModel());

				dialog.components.save(individual);
				//(the editor puts the record back as it was if it is closed without saving: the record is read before)
				FLEFRecordHelper.findChildren(individual, NoteReader.TAG_NOTE)
					.forEach(note -> saved.add(NoteReader.extractText(note)));
				dialog.dispose();
			}
			catch(final Throwable t){
				failures.add(t);
			}
		});

		Assertions.assertTrue(failures.isEmpty(), failures.toString());
		Assertions.assertEquals(List.of("second", "third", "first"), saved);
	}

}
