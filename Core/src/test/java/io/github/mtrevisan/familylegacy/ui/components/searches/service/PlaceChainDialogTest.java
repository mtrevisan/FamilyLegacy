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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;


/** A place is created inside the hierarchy with one line. */
class PlaceChainDialogTest{

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		place(model, "P1", "Veneto");
		place(model, "P2", "Treviso");
		model.addRecord(FLEFRecord.createMainRecord("R1", PlaceRelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_PLACE, "P2")))
			.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_PLACE, "P1")))
			.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_TYPE,
				PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF)));
		return model;
	}

	private static void place(final FLEFModel model, final String id, final String name){
		model.addRecord(FLEFRecord.createMainRecord(id, PlaceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_NAME_VALUE, name))));
	}

	private static boolean partOf(final FLEFModel model, final String subjectId, final String targetId){
		for(final FLEFRecord relationship : model.getRecordsByType(PlaceRelationshipReader.TYPE))
			if(subjectId.equals(relationship.extractReferencedId(PlaceRelationshipReader.TAG_SUBJECT, PlaceReader.TYPE))
					&& targetId.equals(relationship.extractReferencedId(PlaceRelationshipReader.TAG_TARGET, PlaceReader.TYPE)))
				return true;
		return false;
	}


	@Test
	void aLineCreatesTheNewPlaceInsideTheOnesOfTheFile() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final FLEFModel model = model();
		final PlaceChainDialog[] dialog = new PlaceChainDialog[1];

		SwingUtilities.invokeAndWait(() -> {
			dialog[0] = PlaceChainDialog.create(null, model, null);
			Assertions.assertFalse(dialog[0].okButton.isEnabled(), "nothing is written: nothing to create");

			dialog[0].chainField.setText("Borgo, Treviso, Veneto");
			//the largest first: Veneto and Treviso are in the file, Borgo is new
			Assertions.assertEquals(3, dialog[0].levels().size());
			Assertions.assertFalse(dialog[0].levels().get(0).isNew());
			Assertions.assertFalse(dialog[0].levels().get(1).isNew());
			Assertions.assertTrue(dialog[0].levels().get(2).isNew());
			Assertions.assertTrue(dialog[0].okButton.isEnabled());

			dialog[0].okButton.doClick();
		});

		final PlaceChainDialog.Result result = dialog[0].result();
		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.openEditor());
		//a place was created, and it is part of Treviso
		Assertions.assertTrue(partOf(model, result.placeId(), "P2"));
		Assertions.assertEquals(3 + 1 + 1, model.getRecordCount());
	}

	@Test
	void nothingIsCreatedUntilOkAndAPlaceThatIsThereIsNotCreatedAgain() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final FLEFModel model = model();
		final PlaceChainDialog[] dialog = new PlaceChainDialog[1];

		SwingUtilities.invokeAndWait(() -> {
			dialog[0] = PlaceChainDialog.create(null, model, null);
			dialog[0].chainField.setText("Borgo, Villa, Veneto");
			Assertions.assertEquals(3, model.getRecordCount());

			dialog[0].chainField.setText("Treviso, Veneto");
			dialog[0].okButton.doClick();
		});

		Assertions.assertEquals("P2", dialog[0].result().placeId());
		Assertions.assertEquals(3, model.getRecordCount());
	}

	@Test
	void aLineCanBeInsideAPlaceThatIsGiven() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final FLEFModel model = model();
		final PlaceChainDialog[] dialog = new PlaceChainDialog[1];

		SwingUtilities.invokeAndWait(() -> {
			dialog[0] = PlaceChainDialog.create(null, model, model.getRecordById("P2"));
			dialog[0].chainField.setText("Borgo");
			dialog[0].okButton.doClick();
		});

		Assertions.assertTrue(partOf(model, dialog[0].result().placeId(), "P2"));
	}

}
