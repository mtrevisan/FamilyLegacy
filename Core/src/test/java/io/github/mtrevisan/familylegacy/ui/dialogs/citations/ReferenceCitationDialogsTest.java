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
package io.github.mtrevisan.familylegacy.ui.dialogs.citations;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;


/** The place of a place citation and the repository of a repository citation are references chosen with the field, and saved as such. */
class ReferenceCitationDialogsTest{

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("P1", PlaceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_NAME_VALUE, "Treviso"))));
		model.addRecord(FLEFRecord.createMainRecord("R1", RepositoryReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(RepositoryReader.TAG_NAME, "Archivio di Stato")));
		return model;
	}


	@Test
	void thePlaceOfAPlaceCitationIsLoadedAndSavedOnce() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final FLEFModel model = model();
		final FLEFRecord citation = FLEFRecord.createEmpty()
			.addChild(FLEFRecord.createChildWithTagAndValue(PlaceHandler.TYPE, "P1"));

		SwingUtilities.invokeAndWait(() -> {
			final PlaceCitationDialog dialog = PlaceCitationDialog.createEdit(null, model, citation);
			Assertions.assertTrue(dialog.validData());
			dialog.saveData();
			dialog.dispose();
		});

		//the place is saved, and only once
		Assertions.assertEquals(1, FLEFRecordHelper.findChildren(citation, PlaceHandler.TYPE).size());
		Assertions.assertEquals("P1", FLEFRecordHelper.getChildValue(citation, PlaceHandler.TYPE));
	}

	@Test
	void theRepositoryOfARepositoryCitationIsLoadedAndSavedOnce() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final FLEFModel model = model();
		final FLEFRecord citation = FLEFRecord.createEmpty()
			.addChild(FLEFRecord.createChildWithTagAndValue(RepositoryCitationReader.TAG_REPOSITORY, "R1"));

		SwingUtilities.invokeAndWait(() -> {
			final RepositoryCitationDialog dialog = RepositoryCitationDialog.createEdit(null, model, citation);
			Assertions.assertTrue(dialog.validData());
			dialog.saveData();
			dialog.dispose();
		});

		Assertions.assertEquals(1, FLEFRecordHelper.findChildren(citation, RepositoryCitationReader.TAG_REPOSITORY).size());
		Assertions.assertEquals("R1", FLEFRecordHelper.getChildValue(citation, RepositoryCitationReader.TAG_REPOSITORY));
	}

}
