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
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/** A relationship between places is read as a sentence, not as a subject and a target. */
class PlaceRelationshipCellRendererTest{

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		for(final String[] place : new String[][]{{"P1", "Veneto"}, {"P2", "Treviso"}})
			model.addRecord(FLEFRecord.createMainRecord(place[0], PlaceReader.TYPE)
				.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_NAME)
					.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_NAME_VALUE, place[1]))));
		return model;
	}

	private static FLEFRecord relationship(final FLEFModel model){
		final FLEFRecord record = FLEFRecord.createMainRecord("R1", PlaceRelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_PLACE, "P2")))
			.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_PLACE, "P1")))
			.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_TYPE,
				PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF));
		model.addRecord(record);
		return record;
	}


	@Test
	void fromThePlaceThatIsPartOfAnotherOne(){
		final FLEFModel model = model();

		final String sentence = new PlaceRelationshipCellRenderer(model, true).sentence(relationship(model));

		Assertions.assertTrue(sentence.startsWith("administrative part of Veneto"), sentence);
		Assertions.assertFalse(sentence.contains("Subject") || sentence.contains("Target"), sentence);
	}

	@Test
	void fromThePlaceThatContainsIt(){
		final FLEFModel model = model();

		final String sentence = new PlaceRelationshipCellRenderer(model, false).sentence(relationship(model));

		Assertions.assertTrue(sentence.startsWith("Treviso"), sentence);
		Assertions.assertTrue(sentence.contains("administrative part of this place"), sentence);
	}

}
