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
package io.github.mtrevisan.familylegacy.quality;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;


class RecordReferencesTest{

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("P1", "place"));
		model.addRecord(FLEFRecord.createMainRecord("P2", "place"));
		model.addRecord(FLEFRecord.createMainRecord("E1", "event")
			.addChild(FLEFRecord.createChildWithTag("place")
				.addChild(FLEFRecord.createChildWithTagAndValue("place", "P1"))));
		model.addRecord(FLEFRecord.createMainRecord("PR1", "place_relationship")
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(FLEFRecord.createChildWithTagAndValue("place", "P2")))
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(FLEFRecord.createChildWithTagAndValue("place", "P1"))));
		return model;
	}


	@Test
	void theRecordsThatPointToARecordAreFoundWithTheFieldThatPointsToIt(){
		final List<RecordReferences.Reference> references = RecordReferences.to(model(), "P1");

		Assertions.assertEquals(List.of("E1", "PR1"), references.stream().map(reference -> reference.owner().getId()).toList());
		Assertions.assertEquals(List.of("place.place", "target.place"), references.stream().map(RecordReferences.Reference::path).toList());
	}

	@Test
	void aRecordNobodyPointsToHasNoReferences(){
		Assertions.assertEquals(1, RecordReferences.to(model(), "P2").size());
		Assertions.assertTrue(RecordReferences.to(model(), "P3").isEmpty());
		Assertions.assertTrue(RecordReferences.to(null, "P1").isEmpty());
	}

	@Test
	void severalRecordsAreAskedInOnePass(){
		final Map<String, List<RecordReferences.Reference>> references = RecordReferences.to(model(), List.of("P1", "P2", "P9"));

		Assertions.assertEquals(List.of("P1", "P2", "P9"), List.copyOf(references.keySet()));
		Assertions.assertEquals(2, references.get("P1").size());
		Assertions.assertEquals(1, references.get("P2").size());
		Assertions.assertTrue(references.get("P9").isEmpty());
	}

}
