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
