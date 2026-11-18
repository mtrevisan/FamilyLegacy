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
package io.github.mtrevisan.familylegacy.research;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;


class ResearchHelperTest{

	@Test
	void everyQuestionOfARecordIsCounted(){
		final FLEFRecord first = FLEFRecord.createMainRecord("A1", "RESEARCH_ACTIVITY")
			.addChild(FLEFRecord.createChildWithTagAndValue("question", "Q1"))
			.addChild(FLEFRecord.createChildWithTagAndValue("question", "Q2"));
		final FLEFRecord second = FLEFRecord.createMainRecord("A2", "RESEARCH_ACTIVITY")
			.addChild(FLEFRecord.createChildWithTagAndValue("question", "Q1"))
			.addChild(FLEFRecord.createChildWithTagAndValue("question", "Q1"));

		final Map<String, Integer> counts = ResearchHelper.countReferences(List.of(first, second), "question");

		Assertions.assertEquals(2, counts.get("Q1"));
		Assertions.assertEquals(1, counts.get("Q2"));
	}

}
