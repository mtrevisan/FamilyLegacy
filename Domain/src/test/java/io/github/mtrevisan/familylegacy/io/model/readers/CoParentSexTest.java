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
package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.importer.ImportReasons;
import io.github.mtrevisan.familylegacy.importer.ImportReport;
import io.github.mtrevisan.familylegacy.importer.SameSexParentsCheck;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;


/** The biological parents of a child cannot have the same sex; a civil union has no such constraint. */
class CoParentSexTest{

	private FLEFModel model;


	@BeforeEach
	void setUp(){
		model = new FLEFModel();
		for(final String id : new String[]{"I1", "I2", "I3"})
			model.addRecord(FLEFRecord.createMainRecord(id, IndividualReader.TYPE));
		addSex("IA1", "I2", "female");
	}


	private void addSex(final String id, final String individual, final String value){
		model.addRecord(FLEFRecord.createMainRecord(id, IndividualAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, individual))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_SEX))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, value)));
	}

	private void addRelationship(final String id, final String type, final String subject, final String target){
		model.addRecord(FLEFRecord.createMainRecord(id, RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, type))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE, subject)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE, target))));
	}


	private void bothParents(final String type1, final String type2){
		addRelationship("R1", type1, "I3", "I1");
		addRelationship("R2", type2, "I3", "I2");
	}


	@Test
	void sameSexAsTheCoParentOfABiologicalChildClashesForCertain(){
		bothParents(RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);

		final CoParentSex.Clash clash = CoParentSex.findClashingCoParent("I1", SexType.FEMALE, model);
		Assertions.assertEquals("I2", clash.coParentId());
		Assertions.assertTrue(clash.certain());
	}

	@Test
	void aPlainChildLinkIsOnlyAPossibleClash(){
		bothParents(RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, RelationshipReader.ENUM_TYPE_CHILD);

		final CoParentSex.Clash clash = CoParentSex.findClashingCoParent("I1", SexType.FEMALE, model);
		Assertions.assertEquals("I2", clash.coParentId());
		Assertions.assertFalse(clash.certain());
	}

	@Test
	void theOppositeSexDoesNotClash(){
		bothParents(RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);

		Assertions.assertNull(CoParentSex.findClashingCoParent("I1", SexType.MALE, model));
		Assertions.assertNull(CoParentSex.findClashingCoParent("I1", SexType.UNKNOWN, model));
	}

	@Test
	void aNonBiologicalLinkDoesNotClash(){
		bothParents(RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD);

		Assertions.assertNull(CoParentSex.findClashingCoParent("I1", SexType.FEMALE, model));
	}

	@Test
	void aCivilUnionWithoutCommonChildrenDoesNotClash(){
		addRelationship("R1", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, "I1", "I2");

		Assertions.assertNull(CoParentSex.findClashingCoParent("I1", SexType.FEMALE, model));
	}

	@Test
	void aNewBiologicalLinkToASameSexParentClashes(){
		addRelationship("R2", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, "I3", "I2");
		addSex("IA2", "I1", "female");

		Assertions.assertTrue(CoParentSex.findClashingParent("I3", "I1", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, null, model)
			.certain());
		Assertions.assertFalse(CoParentSex.findClashingParent("I3", "I1", RelationshipReader.ENUM_TYPE_CHILD, null, model).certain());
		Assertions.assertNull(CoParentSex.findClashingParent("I3", "I1", RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD, null, model));
	}

	@Test
	void theLinkBeingEditedDoesNotCount(){
		addRelationship("R2", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, "I3", "I2");
		addSex("IA2", "I1", "female");

		Assertions.assertNull(CoParentSex.findClashingParent("I3", "I1", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, "R2", model));
	}

	@Test
	void twoParentsLinkedTogetherClashWhenOfTheSameSex(){
		addSex("IA2", "I1", "female");

		final CoParentSex.Clash certain = CoParentSex.findClashBetween("I3", "I1", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, "I2",
			RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, model);
		Assertions.assertTrue(certain.certain());
		Assertions.assertFalse(CoParentSex.findClashBetween("I3", "I1", RelationshipReader.ENUM_TYPE_CHILD, "I2",
			RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, model).certain());
		Assertions.assertNull(CoParentSex.findClashBetween("I3", "I1", RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD, "I2",
			RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, model));
	}

	@Test
	void theWholeModelIsScanned(){
		bothParents(RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);
		Assertions.assertTrue(CoParentSex.findAll(model).isEmpty());

		addSex("IA2", "I1", "female");
		final List<CoParentSex.Clash> clashes = CoParentSex.findAll(model);
		Assertions.assertEquals(1, clashes.size());
		Assertions.assertTrue(clashes.getFirst().certain());
		Assertions.assertEquals("I3", clashes.getFirst().childId());
	}

	@Test
	void theImportReportsThem(){
		bothParents(RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, RelationshipReader.ENUM_TYPE_CHILD);
		addSex("IA2", "I1", "female");

		final ImportReport report = new ImportReport();
		SameSexParentsCheck.report(model, report);

		Assertions.assertEquals(Map.of(ImportReasons.SAME_SEX_POSSIBLE_PARENTS, 1), report.countsByReason());
	}

}
