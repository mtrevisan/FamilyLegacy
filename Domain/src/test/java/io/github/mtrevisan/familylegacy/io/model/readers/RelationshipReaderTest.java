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

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The types of relationship that the editor offers for two records. */
class RelationshipReaderTest{

	private static FLEFRecord individual(final String id){
		return FLEFRecord.createMainRecord(id, IndividualReader.TYPE);
	}

	private static FLEFRecord group(final String id){
		return FLEFRecord.createMainRecord(id, GroupReader.TYPE);
	}


	@Test
	void twoIndividualsCanBeLinkedByEveryTypeOfTheGrammarThatFitsThem(){
		final List<String> types = List.of(RelationshipReader.getValidTypes(individual("I1"), individual("I2")));

		//the generic types too: an import writes them, and a relationship that is not offered cannot be saved again
		Assertions.assertTrue(types.containsAll(List.of(RelationshipReader.ENUM_TYPE_CHILD, RelationshipReader.ENUM_TYPE_SPOUSE)), types.toString());
		Assertions.assertTrue(types.containsAll(List.of(RelationshipReader.INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES)));
		Assertions.assertTrue(types.containsAll(List.of(RelationshipReader.INDIVIDUAL_TO_INDIVIDUAL_SOCIAL_TYPES)));
		Assertions.assertFalse(types.contains(RelationshipReader.ENUM_TYPE_GROUP_MEMBER));
		Assertions.assertFalse(types.contains(RelationshipReader.ENUM_TYPE_PART_OF));
	}

	@Test
	void anIndividualAndAGroupCanOnlyBeMembersOrAssociates(){
		Assertions.assertEquals(List.of("group_member", "associate"),
			List.of(RelationshipReader.getValidTypes(individual("I1"), group("G1"))));
	}

	@Test
	void twoGroupsCanBePartOfOrAssociates(){
		Assertions.assertEquals(List.of("part_of", "associate"), List.of(RelationshipReader.getValidTypes(group("G1"), group("G2"))));
	}

	@Test
	void aGroupIsNotRelatedToAnIndividualAndNothingIsOfferedWithoutBothRecords(){
		Assertions.assertEquals(0, RelationshipReader.getValidTypes(group("G1"), individual("I1")).length);
		Assertions.assertEquals(0, RelationshipReader.getValidTypes(null, individual("I1")).length);
		Assertions.assertEquals(0, RelationshipReader.getValidTypes(individual("I1"), null).length);
	}

}
