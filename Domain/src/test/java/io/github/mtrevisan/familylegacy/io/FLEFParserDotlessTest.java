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
package io.github.mtrevisan.familylegacy.io;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;


/** A file with no dot in its records is read in time linear in its size (the search for the dot of a tag stops at the end of the tag). */
class FLEFParserDotlessTest{

	@Test
	void manyRecordsWithoutDotsAreParsedQuickly(){
		final int count = 30_000;
		final StringBuilder text = new StringBuilder("header {\n\tprotocol {\n\t\tname Family LEgacy Format\n\t\tversion 0\n\t}\n}\n");
		for(int i = 0; i < count; i ++)
			text.append("place P").append(i).append(" {\n\tname {\n\t\tvalue Place").append(i).append("\n\t}\n}\n");

		final FLEFModel[] model = new FLEFModel[1];
		Assertions.assertTimeoutPreemptively(Duration.ofSeconds(10), () -> model[0] = new FLEFParser().parse(text.toString()));

		Assertions.assertEquals(count, model[0].getRecordsByType("place").size());
	}

}
