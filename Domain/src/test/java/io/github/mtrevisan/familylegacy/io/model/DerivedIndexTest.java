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
package io.github.mtrevisan.familylegacy.io.model;

import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/** The index of the sex attributes kept by the model follows the changes of the model. */
class DerivedIndexTest{

	private static FLEFRecord sex(final String id, final String individual, final String value){
		return FLEFRecord.createMainRecord(id, IndividualAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, individual))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_SEX))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, value));
	}


	@Test
	void anAddedOrRemovedAttributeIsSeen(){
		final FLEFModel model = new FLEFModel();
		final FLEFRecord person = FLEFRecord.createMainRecord("I1", IndividualReader.TYPE);
		model.addRecord(person);
		Assertions.assertEquals(SexType.UNKNOWN, PreferredSex.of(person, model));

		model.addRecord(sex("A1", "I1", IndividualReader.ENUM_SEX_FEMALE));
		Assertions.assertEquals(SexType.FEMALE, PreferredSex.of(person, model));

		model.removeRecord("A1");
		Assertions.assertEquals(SexType.UNKNOWN, PreferredSex.of(person, model));
	}

	@Test
	void anAttributeEditedInPlaceIsSeenAtTheCheckpoint(){
		final FLEFModel model = new FLEFModel();
		final FLEFRecord person = FLEFRecord.createMainRecord("I1", IndividualReader.TYPE);
		model.addRecord(person);
		final FLEFRecord attribute = sex("A1", "I2", IndividualReader.ENUM_SEX_MALE);
		model.addRecord(attribute);
		final ModelHistory history = new ModelHistory(model);
		Assertions.assertEquals(SexType.UNKNOWN, PreferredSex.of(person, model));

		//the attribute now belongs to I1: only the checkpoint of the history can tell
		attribute.getChildren().stream()
			.filter(child -> IndividualAttributeReader.TAG_INDIVIDUAL.equals(child.getTag()))
			.forEach(child -> child.setValue("I1"));
		history.checkpoint();

		Assertions.assertEquals(SexType.MALE, PreferredSex.of(person, model));

		history.undo();
		Assertions.assertEquals(SexType.UNKNOWN, PreferredSex.of(person, model));
	}

}
