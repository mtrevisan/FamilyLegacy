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
