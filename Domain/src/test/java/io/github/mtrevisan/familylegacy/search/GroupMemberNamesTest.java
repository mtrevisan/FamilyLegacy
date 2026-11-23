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
package io.github.mtrevisan.familylegacy.search;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


/**
 * A group is found by all the names of all its members.
 */
class GroupMemberNamesTest{

	private FLEFModel model;


	@BeforeEach
	void buildModel(){
		model = new FLEFModel();
		model.addRecord(group("G1"));
		model.addRecord(group("G2"));
		//the father has two names
		model.addRecord(individual("I1", "Mario Rossi", "Marino Rosso"));
		model.addRecord(individual("I2", "Anna Bianchi"));
		model.addRecord(individual("I3", "Carlo Verdi"));
		model.addRecord(membership("R1", "I1", "G1", RelationshipReader.ENUM_TYPE_GROUP_MEMBER));
		model.addRecord(membership("R2", "I2", "G1", RelationshipReader.ENUM_TYPE_GROUP_MEMBER));
		model.addRecord(membership("R3", "I3", "G2", RelationshipReader.ENUM_TYPE_GROUP_MEMBER));
		//not a membership
		model.addRecord(membership("R4", "I3", "G1", RelationshipReader.ENUM_TYPE_ASSOCIATE));
	}


	@Test
	void everyNameOfEveryMember(){
		final List<String> names = new GroupMemberNames(model).names(model.getRecordById("G1"));

		Assertions.assertEquals(3, names.size());
		Assertions.assertTrue(names.stream().anyMatch(name -> name.contains("Mario")));
		Assertions.assertTrue(names.stream().anyMatch(name -> name.contains("Marino")));
		Assertions.assertTrue(names.stream().anyMatch(name -> name.contains("Anna")));
	}

	@Test
	void onlyMembersOfThatGroup(){
		final List<String> names = new GroupMemberNames(model).names(model.getRecordById("G2"));

		Assertions.assertEquals(1, names.size());
		Assertions.assertTrue(names.getFirst().contains("Carlo"));
		Assertions.assertFalse(names.stream().anyMatch(name -> name.contains("Anna")));
	}

	@Test
	void groupWithoutMembersOrUnknownIsEmpty(){
		model.addRecord(group("G3"));
		final GroupMemberNames memberNames = new GroupMemberNames(model);

		Assertions.assertTrue(memberNames.names(model.getRecordById("G3")).isEmpty());
		Assertions.assertTrue(memberNames.names(group("missing")).isEmpty());
		Assertions.assertTrue(memberNames.names(null).isEmpty());
	}

	@Test
	void familyIsFoundByTheParentsAndTheChildrenOfItsMembers(){
		//the family has the child as a member; the child is the biological child of Mario Rossi and Anna Bianchi
		model.addRecord(group("G4", GroupReader.ENUM_TYPE_FAMILY));
		model.addRecord(individual("I4", "Luca Rossi"));
		model.addRecord(individual("I5", "Piero Neri"));
		model.addRecord(membership("R5", "I4", "G4", RelationshipReader.ENUM_TYPE_GROUP_MEMBER));
		model.addRecord(childOf("R6", "I4", "I1", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
		model.addRecord(childOf("R7", "I4", "I2", RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD));
		//a child of the member
		model.addRecord(childOf("R8", "I5", "I4", RelationshipReader.ENUM_TYPE_CHILD));

		final List<String> names = new GroupMemberNames(model).names(model.getRecordById("G4"));

		//Luca, the two names of Mario, Anna, Piero
		Assertions.assertEquals(5, names.size());
		Assertions.assertTrue(names.stream().anyMatch(name -> name.contains("Marino")));
		Assertions.assertTrue(names.stream().anyMatch(name -> name.contains("Anna")));
		Assertions.assertTrue(names.stream().anyMatch(name -> name.contains("Piero")));
	}

	@Test
	void membersWithTheirRoleButNotTheirRelatives(){
		model.addRecord(group("G4", GroupReader.ENUM_TYPE_FAMILY));
		model.addRecord(individual("I4", "Luca Rossi"));
		model.addRecord(membership("R5", "I4", "G4", RelationshipReader.ENUM_TYPE_GROUP_MEMBER)
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_ROLE, "son")));
		model.addRecord(membership("R6", "I2", "G4", RelationshipReader.ENUM_TYPE_GROUP_MEMBER));
		model.addRecord(childOf("R7", "I4", "I1", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));

		final List<GroupMemberNames.Member> members = new GroupMemberNames(model).members(model.getRecordById("G4"));

		Assertions.assertEquals(2, members.size());
		Assertions.assertEquals("son", members.get(0).role());
		Assertions.assertTrue(members.get(0).name().contains("Luca"));
		Assertions.assertNull(members.get(1).role());
		Assertions.assertTrue(members.get(1).name().contains("Anna"));
	}

	@Test
	void otherGroupsAreNotFoundByTheRelativesOfTheirMembers(){
		model.addRecord(group("G5", GroupReader.ENUM_TYPE_CLUB));
		model.addRecord(individual("I4", "Luca Rossi"));
		model.addRecord(membership("R5", "I4", "G5", RelationshipReader.ENUM_TYPE_GROUP_MEMBER));
		model.addRecord(childOf("R6", "I4", "I1", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));

		final List<String> names = new GroupMemberNames(model).names(model.getRecordById("G5"));

		Assertions.assertEquals(1, names.size());
		Assertions.assertTrue(names.getFirst().contains("Luca"));
	}


	private static FLEFRecord group(final String id){
		return group(id, null);
	}

	private static FLEFRecord group(final String id, final String type){
		final FLEFRecord group = FLEFRecord.createMainRecord(id, GroupReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(GroupReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue("value", "Some group " + id)));
		if(type != null)
			group.addChild(FLEFRecord.createChildWithTagAndValue(GroupReader.TAG_TYPE, type));
		return group;
	}

	private static FLEFRecord individual(final String id, final String... names){
		final FLEFRecord individual = FLEFRecord.createMainRecord(id, IndividualReader.TYPE);
		for(final String name : names){
			final String given = StringUtils.substringBefore(name, " ");
			final String family = StringUtils.substringAfter(name, " ");
			individual.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_GIVEN))
					.addChild(FLEFRecord.createChildWithTagAndValue("value", given)))
				.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_FAMILY))
					.addChild(FLEFRecord.createChildWithTagAndValue("value", family))));
		}
		return individual;
	}

	private static FLEFRecord membership(final String id, final String individualId, final String groupId, final String type){
		return FLEFRecord.createMainRecord(id, RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, type))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE, individualId)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(GroupReader.TYPE, groupId)));
	}

	private static FLEFRecord childOf(final String id, final String childId, final String parentId, final String type){
		return FLEFRecord.createMainRecord(id, RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, type))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE, childId)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE, parentId)));
	}

}
