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
