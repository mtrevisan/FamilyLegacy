package io.github.mtrevisan.familylegacy.consanguinity;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.reports.index.RelationIndex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** Father and mother told by the sex (not by the order of the file), and blood kinship on the biological links only. */
class KinshipParentsTest{

	private static void person(final FLEFModel model, final String id, final String sex){
		model.addRecord(FLEFRecord.createMainRecord(id, IndividualReader.TYPE));
		if(sex != null)
			model.addRecord(FLEFRecord.createMainRecord("S" + id, IndividualAttributeReader.TYPE)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, id))
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_SEX))
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, sex)));
	}

	private static void link(final FLEFModel model, final String id, final String childId, final String parentId, final String type){
		model.addRecord(FLEFRecord.createMainRecord(id, RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_INDIVIDUAL, childId)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_INDIVIDUAL, parentId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, type)));
	}

	private static List<String> ids(final List<FLEFRecord> records){
		return records.stream()
			.map(FLEFRecord::getId)
			.toList();
	}


	@Test
	void fatherAndMotherDoNotDependOnTheOrderOfTheRelationships(){
		final FLEFModel model = new FLEFModel();
		person(model, "C", IndividualReader.ENUM_SEX_MALE);
		person(model, "F", IndividualReader.ENUM_SEX_MALE);
		person(model, "M", IndividualReader.ENUM_SEX_FEMALE);
		person(model, "GF", IndividualReader.ENUM_SEX_MALE);
		//the mother comes first in the file
		link(model, "R1", "C", "M", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);
		link(model, "R2", "C", "F", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);
		link(model, "R3", "F", "GF", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);

		final RelationIndex index = RelationIndex.build(model);
		final FLEFRecord root = model.getRecordById("C");

		Assertions.assertEquals(List.of("F", "GF"), ids(index.paternalAncestors(root)));
		Assertions.assertEquals(List.of("M"), ids(index.maternalAncestors(root)));
	}

	@Test
	void aParentOfUnknownSexTakesTheSideLeftFreeByTheKnownOne(){
		final FLEFModel model = new FLEFModel();
		person(model, "C", null);
		person(model, "X", null);
		person(model, "M", IndividualReader.ENUM_SEX_FEMALE);
		link(model, "R1", "C", "M", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);
		link(model, "R2", "C", "X", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);

		final RelationIndex index = RelationIndex.build(model);
		final FLEFRecord root = model.getRecordById("C");

		Assertions.assertEquals(List.of("X"), ids(index.paternalAncestors(root)));
		Assertions.assertEquals(List.of("M"), ids(index.maternalAncestors(root)));
	}

	@Test
	void parentsOfUnknownSexAreNotGuessed(){
		final FLEFModel model = new FLEFModel();
		person(model, "C", null);
		person(model, "X", null);
		person(model, "Y", null);
		link(model, "R1", "C", "X", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);
		link(model, "R2", "C", "Y", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);

		final RelationIndex index = RelationIndex.build(model);
		final FLEFRecord root = model.getRecordById("C");

		Assertions.assertTrue(index.paternalAncestors(root).isEmpty());
		Assertions.assertTrue(index.maternalAncestors(root).isEmpty());
	}

	@Test
	void bloodKinshipIgnoresAdoptiveLinksWhileLegalKinshipKeepsThem(){
		final FLEFModel model = new FLEFModel();
		person(model, "A", IndividualReader.ENUM_SEX_MALE);
		person(model, "B", IndividualReader.ENUM_SEX_MALE);
		person(model, "P", IndividualReader.ENUM_SEX_MALE);
		link(model, "R1", "P", "A", RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD);
		link(model, "R2", "P", "B", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);

		final KinshipCalculator blood = KinshipCalculator.forModel(model);
		final KinshipCalculator legal = KinshipCalculator.forModelWithLegalParents(model);

		Assertions.assertEquals("unrelated", blood.shortTerm("A", "P"));
		Assertions.assertEquals("father", blood.shortTerm("B", "P"));
		Assertions.assertEquals("father", legal.shortTerm("A", "P"));
		Assertions.assertEquals(List.of("B"), AncestryGraph.of(model).parentsOf("P"));
	}

	@Test
	void theGenericChildLinkCountsAsBiologicalInBothDefinitions(){
		final FLEFModel model = new FLEFModel();
		person(model, "A", IndividualReader.ENUM_SEX_MALE);
		person(model, "P", IndividualReader.ENUM_SEX_MALE);
		link(model, "R1", "P", "A", RelationshipReader.ENUM_TYPE_CHILD);

		Assertions.assertEquals("father", KinshipCalculator.forModel(model).shortTerm("A", "P"));
		Assertions.assertEquals(List.of("A"), AncestryGraph.of(model).parentsOf("P"));
	}

}
