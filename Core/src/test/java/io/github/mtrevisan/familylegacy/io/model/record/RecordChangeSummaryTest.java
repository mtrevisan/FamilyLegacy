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
package io.github.mtrevisan.familylegacy.io.model.record;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class RecordChangeSummaryTest{

	private static FLEFRecord record(final String... tagValuePairs){
		final FLEFRecord record = FLEFRecord.createMainRecord("I1", "individual");
		for(int i = 0; i < tagValuePairs.length; i += 2)
			record.addChild(FLEFRecord.createChildWithTagAndValue(tagValuePairs[i], tagValuePairs[i + 1]));
		return record;
	}


	@Test
	void nothingChanged(){
		Assertions.assertTrue(RecordChangeSummary.describe(record("sex", "male"), record("sex", "male")).isEmpty());
	}

	@Test
	void addedChangedAndRemoved(){
		final FLEFRecord before = record("sex", "male", "note", "old");
		final FLEFRecord after = record("sex", "female", "occupation", "smith");

		final List<String> changes = RecordChangeSummary.describe(before, after);

		Assertions.assertEquals(List.of("~ sex: male → female", "- note: old", "+ occupation: smith"), changes);
	}

	@Test
	void repeatedFieldsAreIndexed(){
		final FLEFRecord before = record("note", "a");
		final FLEFRecord after = record("note", "a", "note", "b");

		Assertions.assertEquals(List.of("+ note[1]: b"), RecordChangeSummary.describe(before, after));
	}

	@Test
	void nestedFieldsUseTheFullPath(){
		final FLEFRecord before = record();
		before.addChild(FLEFRecord.createChildWithTag("name")
			.addChild(FLEFRecord.createChildWithTagAndValue("given", "John")));
		final FLEFRecord after = record();
		after.addChild(FLEFRecord.createChildWithTag("name")
			.addChild(FLEFRecord.createChildWithTagAndValue("given", "Jon")));

		Assertions.assertEquals(List.of("~ name.given: John → Jon"), RecordChangeSummary.describe(before, after));
	}

	@Test
	void theAuditIsIgnored(){
		final FLEFRecord before = record("sex", "male");
		final FLEFRecord after = record("sex", "male");
		after.addChild(FLEFRecord.createChildWithTag("audit")
			.addChild(FLEFRecord.createChildWithTagAndValue("comment", "x")));

		Assertions.assertTrue(RecordChangeSummary.describe(before, after).isEmpty());
	}

	@Test
	void tooManyChangesAreTruncated(){
		final FLEFRecord after = record();
		for(int i = 0; i < 25; i ++)
			after.addChild(FLEFRecord.createChildWithTagAndValue("t" + i, "v"));

		final List<String> changes = RecordChangeSummary.describe(record(), after);

		Assertions.assertEquals(21, changes.size());
		Assertions.assertEquals("… +5", changes.getLast());
	}

}
