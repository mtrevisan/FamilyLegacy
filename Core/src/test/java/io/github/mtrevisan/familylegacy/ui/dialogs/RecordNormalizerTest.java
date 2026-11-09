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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.merger.manual.MergeGrammar;
import io.github.mtrevisan.familylegacy.io.model.readers.AuditReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;


class RecordNormalizerTest{

	private static FLEFGrammar grammar;


	@BeforeAll
	static void loadGrammar(){
		grammar = MergeGrammar.instance();
	}


	private static List<String> tags(final FLEFRecord record){
		return record.getChildren().stream()
			.map(FLEFRecord::getTag)
			.toList();
	}


	@Test
	void childrenFollowTheOrderOfTheGrammar(){
		//the grammar declares: name, source, note, preferred_image, privacy, audit
		final FLEFRecord record = FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(AuditReader.TAG_AUDIT))
			.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_NOTE, "n"))
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_PRIVACY))
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME));

		Assertions.assertEquals(List.of("name", "note", "privacy", "audit"), tags(RecordNormalizer.normalize(record, grammar)));
	}

	@Test
	void unknownChildrenComeLastAndRepeatedChildrenAreSorted(){
		final FLEFRecord record = FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue("zzz", "1"))
			.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_NOTE, "b"))
			.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_NOTE, "a"))
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_PRIVACY));

		final FLEFRecord normalized = RecordNormalizer.normalize(record, grammar);

		Assertions.assertEquals(List.of("note", "note", "privacy", "zzz"), tags(normalized));
		Assertions.assertEquals("a", normalized.getChildren().get(0).getValue());
	}

	@Test
	void withoutGrammarTheChildrenAreSortedByTag(){
		final FLEFRecord record = FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME))
			.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_NOTE, "n"))
			.addChild(FLEFRecord.createChildWithTag(AuditReader.TAG_AUDIT));

		Assertions.assertEquals(List.of("audit", "name", "note"), tags(RecordNormalizer.normalize(record, null)));
	}

}
