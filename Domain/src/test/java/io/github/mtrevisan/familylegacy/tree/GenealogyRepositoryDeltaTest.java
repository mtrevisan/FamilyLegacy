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
package io.github.mtrevisan.familylegacy.tree;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/**
 * The incremental update of the indices after a relationship is added.
 */
class GenealogyRepositoryDeltaTest{

	private static final String[] CHILD_TYPES = {RelationshipReader.ENUM_TYPE_CHILD, RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD};


	@Test
	void aPartnerIsNotAParent(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("I2", IndividualReader.TYPE));
		final GenealogyRepository repository = new GenealogyRepository(CHILD_TYPES, model);
		repository.ensureIndices();

		model.addRecord(relationship("R1", RelationshipReader.ENUM_TYPE_SPOUSE, "I1", "I2"));
		repository.notifyRelationshipAdded("I1", "I2", "R1");

		Assertions.assertTrue(repository.getParents("I1").isEmpty());
		Assertions.assertTrue(repository.getChildren("I2").isEmpty());
		Assertions.assertEquals(List.of("I2"), repository.getPartnerIds("I1"));
	}

	@Test
	void aChildLinkIsApplied(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("I2", IndividualReader.TYPE));
		final GenealogyRepository repository = new GenealogyRepository(CHILD_TYPES, model);
		repository.ensureIndices();

		model.addRecord(relationship("R1", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, "I1", "I2"));
		repository.notifyRelationshipAdded("I1", "I2", "R1");

		Assertions.assertEquals(1, repository.getParents("I1").size());
		Assertions.assertEquals(RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, repository.getParentLinkType("I1", "I2"));
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
