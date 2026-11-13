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
package io.github.mtrevisan.familylegacy.ui.assistant.context;

import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class NormEstimateCheckerTest{

	private static FLEFRecord event(final String id, final String basis, final String normId){
		final FLEFRecord approximate = FLEFRecord.createChildWithTag(DateReader.TAG_APPROXIMATE);
		if(basis != null)
			approximate.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_BASIS, basis));
		if(normId != null)
			approximate.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_CULTURAL_NORM, normId));
		final FLEFRecord point = FLEFRecord.createChildWithTag("point");
		point.addChild(approximate);
		final FLEFRecord date = FLEFRecord.createChildWithTag(EventReader.TAG_DATE);
		date.addChild(point);

		final FLEFRecord event = FLEFRecord.createEmpty();
		event.setId(id);
		event.addChild(date);
		return event;
	}

	@Test
	void conventionalEstimateWithoutNormIsReported(){
		Assertions.assertEquals(1, NormEstimateChecker.findings(List.of(event("EV1", DateReader.ENUM_BASIS_CONVENTIONAL, null))).size());
	}

	@Test
	void conventionalEstimateWithNormIsNot(){
		Assertions.assertTrue(NormEstimateChecker.findings(List.of(event("EV1", DateReader.ENUM_BASIS_CONVENTIONAL, "CN1"))).isEmpty());
	}

	@Test
	void otherBasesAndExactDatesAreNot(){
		Assertions.assertTrue(NormEstimateChecker.findings(List.of(event("EV1", DateReader.ENUM_BASIS_STATED, null),
			event("EV2", DateReader.ENUM_BASIS_CALCULATED, null), event("EV3", null, null))).isEmpty());
	}

}
