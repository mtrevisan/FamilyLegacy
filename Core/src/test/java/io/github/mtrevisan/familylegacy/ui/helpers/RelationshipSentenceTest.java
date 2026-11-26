package io.github.mtrevisan.familylegacy.ui.helpers;

import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class RelationshipSentenceTest{

	@Test
	void parentageIsReadFromTheParentSide(){
		//the record stores the child as the subject, the sentence is read the way people say it
		Assertions.assertEquals("Alice is the biological child of John",
			RelationshipSentence.of(RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, "Alice", "John", null));
		Assertions.assertEquals("Alice is the ward of John",
			RelationshipSentence.of(RelationshipReader.ENUM_TYPE_GUARDED_CHILD, "Alice", "John", ""));
	}

	@Test
	void everyTypeHasItsOwnWordingWithBothNames(){
		for(final String type : RelationshipReader.TYPES){
			final String sentence = RelationshipSentence.of(type, "Alice", "John", null);

			Assertions.assertNotNull(RelationshipSentence.KEY_PREFIX + type, sentence);
			Assertions.assertTrue(sentence.contains("Alice") && sentence.contains("John"), type + ": " + sentence);
			Assertions.assertFalse(sentence.startsWith(RelationshipSentence.KEY_PREFIX), type + " has no wording: " + sentence);
		}
	}

	@Test
	void theRoleIsToldOnlyWhereItBelongs(){
		Assertions.assertEquals("Alice is associated with John as godmother",
			RelationshipSentence.of(RelationshipReader.ENUM_TYPE_ASSOCIATE, "Alice", "John", " godmother "));
		Assertions.assertEquals("Alice is a member of Club as president",
			RelationshipSentence.of(RelationshipReader.ENUM_TYPE_GROUP_MEMBER, "Alice", "Club", "president"));
		Assertions.assertEquals("Alice is the biological child of John",
			RelationshipSentence.of(RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, "Alice", "John", "son"));
	}

	@Test
	void missingNamesAndTypeAreHandled(){
		Assertions.assertEquals("-- is a child of --", RelationshipSentence.of(RelationshipReader.ENUM_TYPE_CHILD, null, " ", null));
		Assertions.assertEquals("", RelationshipSentence.of(null, "Alice", "John", null));
	}

}
