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
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;


/** The editor of a record lists the research tasks that name it in their {@code related}. */
class TasksAboutRecordTest{

	private static FLEFRecord task(final String id, final String relatedTag, final String relatedId){
		return FLEFRecord.createMainRecord(id, ResearchTaskReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_DESCRIPTION, "Task " + id))
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_STATUS, "open"))
			.addChild(FLEFRecord.createChildWithTag(ResearchTaskReader.TAG_RELATED)
				.addChild(FLEFRecord.createChildWithTagAndValue(relatedTag, relatedId)));
	}


	@Test
	void theEditorOfAnIndividualAndOfAPlaceListTheirTasks() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = new FLEFModel();
		//a record with no children is a new one: its lists are not loaded
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, "Antonia"))));
		model.addRecord(FLEFRecord.createMainRecord("P1", PlaceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_NAME, "Treviso")));
		model.addRecord(task("T1", "individual", "I1"));
		model.addRecord(task("T2", "individual", "I1"));
		model.addRecord(task("T3", "place", "P1"));
		final List<List<String>> listed = new ArrayList<>();

		SwingUtilities.invokeAndWait(() -> {
			for(final BaseRecordDialog dialog : List.of(
					(BaseRecordDialog)IndividualHandler.getInstance().createEditDialog(null, model, model.getRecordById("I1")),
					(BaseRecordDialog)PlaceHandler.getInstance().createEditDialog(null, model, model.getRecordById("P1")))){
				final EntityListPanel panel = (EntityListPanel)dialog.components.getPanel(PanelKey.RESEARCH_TASK_ON_RELATED);
				listed.add(panel.getItems().stream()
					.map(FLEFRecord::getId)
					.toList());
				dialog.dispose();
			}
		});

		Assertions.assertEquals(List.of(List.of("T1", "T2"), List.of("T3")), listed);
	}

}
