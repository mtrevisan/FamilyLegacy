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
package io.github.mtrevisan.familylegacy.individual;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;


class IndividualSummaryTest{

	@Test
	void parentsAreFoundEvenIfThePartnerAndTheChildrenComeFirst(){
		final FLEFModel model = new FLEFModel();
		for(final String id : new String[]{"I1", "I2", "I3", "I4"})
			model.addRecord(FLEFRecord.createMainRecord(id, IndividualReader.TYPE));
		model.addRecord(relationship("R1", RelationshipReader.ENUM_TYPE_SPOUSE, "I1", "I2"));
		model.addRecord(relationship("R2", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, "I3", "I1"));
		model.addRecord(relationship("R3", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, "I1", "I4"));

		final IndividualSummary summary = IndividualSummary.create(model.getRecordById("I1"), Map.of(), model);

		Assertions.assertTrue(summary.hasPartner());
		Assertions.assertTrue(summary.hasChildren());
		Assertions.assertTrue(summary.hasParents());
	}


	private static FLEFRecord relationship(final String id, final String type, final String subject, final String target){
		return FLEFRecord.createMainRecord(id, RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, type))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE, subject)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE, target)));
	}

}
