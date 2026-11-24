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
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The records that a list of an editor shows are put in the order the user gave them, in the places they have in the file. */
class ReorderRecordsTest{

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("IA1", IndividualAttributeReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("IA2", IndividualAttributeReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("I2", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("IA3", IndividualAttributeReader.TYPE));
		return model;
	}

	private static List<String> ids(final List<FLEFRecord> records){
		return records.stream()
			.map(FLEFRecord::getId)
			.toList();
	}


	@Test
	void theRecordsTakeTheNewOrderInTheirPlacesAndTheOthersStay(){
		final FLEFModel model = model();

		model.reorderRecords(List.of(model.getRecordById("IA3"), model.getRecordById("IA1"), model.getRecordById("IA2")));

		Assertions.assertEquals(List.of("IA3", "I1", "IA1", "I2", "IA2"), ids(model.getRecords()));
		Assertions.assertEquals(List.of("IA3", "IA1", "IA2"), ids(model.getRecordsByType(IndividualAttributeReader.TYPE)));
		Assertions.assertEquals(List.of("I1", "I2"), ids(model.getRecordsByType(IndividualReader.TYPE)));
	}

	@Test
	void recordsOfSeveralTypesKeepTheirTypeOrderToo(){
		final FLEFModel model = model();

		model.reorderRecords(List.of(model.getRecordById("I2"), model.getRecordById("IA2"), model.getRecordById("I1")));

		Assertions.assertEquals(List.of("IA1", "I2", "IA2", "I1", "IA3"), ids(model.getRecords()));
		Assertions.assertEquals(List.of("I2", "I1"), ids(model.getRecordsByType(IndividualReader.TYPE)));
		Assertions.assertEquals(List.of("IA1", "IA2", "IA3"), ids(model.getRecordsByType(IndividualAttributeReader.TYPE)));
	}

	@Test
	void aRecordThatIsNotOfTheModelOrIsGivenTwiceChangesNothing(){
		final FLEFModel model = model();
		final List<String> before = ids(model.getRecords());

		model.reorderRecords(List.of(model.getRecordById("IA3"), FLEFRecord.createMainRecord("IA9", IndividualAttributeReader.TYPE)));
		model.reorderRecords(List.of(model.getRecordById("IA3"), model.getRecordById("IA3")));
		model.reorderRecords(List.of(model.getRecordById("IA3")));
		model.reorderRecords(null);

		Assertions.assertEquals(before, ids(model.getRecords()));
	}

	@Test
	void theSameOrderChangesNothing(){
		final FLEFModel model = model();
		final long stamp = model.structureStamp();

		model.reorderRecords(List.of(model.getRecordById("IA1"), model.getRecordById("IA2"), model.getRecordById("IA3")));

		Assertions.assertEquals(stamp, model.structureStamp());
	}

}
