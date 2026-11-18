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
package io.github.mtrevisan.familylegacy.io.model;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class FLEFRecordCopyTest{

	@Test
	void deepCopyKeepsEqualSiblingsAsDistinctRecords(){
		final FLEFRecord original = FLEFRecord.createMainRecord("I1", "individual");
		final FLEFRecord first = FLEFRecord.createChildWithTagAndValue("note", "same");
		final FLEFRecord second = FLEFRecord.createChildWithTagAndValue("note", "same");
		original.addChild(first);
		original.addChild(second);
		first.addChild(FLEFRecord.createChildWithTagAndValue("text", "a"));
		second.addChild(FLEFRecord.createChildWithTagAndValue("text", "a"));

		final FLEFRecord copy = FLEFRecord.createEmpty();
		original.deepCopyTo(copy);

		Assertions.assertEquals(2, copy.getChildren().size());
		Assertions.assertNotSame(copy.getChildren().get(0), copy.getChildren().get(1));
		Assertions.assertEquals(1, copy.getChildren().get(0).getChildren().size());
		Assertions.assertEquals(1, copy.getChildren().get(1).getChildren().size());
	}

	@Test
	void findChildrenOrderedKeepsEqualSiblings(){
		final FLEFRecord parent = FLEFRecord.createMainRecord("I1", "individual");
		parent.addChild(FLEFRecord.createChildWithTagAndValue("note", "same"));
		parent.addChild(FLEFRecord.createChildWithTagAndValue("note", "same"));

		Assertions.assertEquals(2, FLEFRecordHelper.findChildrenOrdered(parent, "note").size());
	}

	@Test
	void anUnclosedIndexMatchesNothing(){
		final FLEFRecord parent = FLEFRecord.createMainRecord("I1", "individual");
		parent.addChild(FLEFRecord.createChildWithTagAndValue("note", "x"));

		Assertions.assertTrue(FLEFRecordHelper.findChildren(parent, "note[1").isEmpty());
	}

	@Test
	void childrenWithDifferentMultiplicitiesAreNotEqual(){
		final FLEFRecord a = FLEFRecord.createChildWithTag("x");
		a.addChild(FLEFRecord.createChildWithTagAndValue("p", "1"));
		a.addChild(FLEFRecord.createChildWithTagAndValue("p", "1"));
		a.addChild(FLEFRecord.createChildWithTagAndValue("q", "2"));
		final FLEFRecord b = FLEFRecord.createChildWithTag("x");
		b.addChild(FLEFRecord.createChildWithTagAndValue("p", "1"));
		b.addChild(FLEFRecord.createChildWithTagAndValue("q", "2"));
		b.addChild(FLEFRecord.createChildWithTagAndValue("q", "2"));

		Assertions.assertNotEquals(a, b);
	}

}
