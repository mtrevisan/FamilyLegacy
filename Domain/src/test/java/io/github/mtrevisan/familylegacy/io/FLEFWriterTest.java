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
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/** What the writer writes, the parser reads back. */
class FLEFWriterTest{

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		//a record with an id and nothing else is still a block
		model.addRecord(FLEFRecord.createMainRecord("I1", "individual"));
		model.addRecord(FLEFRecord.createMainRecord("I2", "individual")
			.addChild(FLEFRecord.createChildWithTagAndValue("note", "first\r\nsecond\rthird")));
		return model;
	}

	private static void assertRoundTrip(final FLEFWriter writer){
		final String text = writer.writeToString(model());
		Assertions.assertFalse(text.startsWith("\n"), text);

		final FLEFModel reread = new FLEFParser().parse(text);

		Assertions.assertEquals(2, reread.getRecordCount());
		Assertions.assertTrue(reread.getRecordById("I1").getChildren().isEmpty());
		Assertions.assertEquals("first\nsecond\nthird", reread.getRecordById("I2").getChildren().getFirst().getValue());
	}


	@Test
	void anEmptyRecordWithAnIdIsClosed(){
		final String text = FLEFWriter.create().writeToString(FLEFRecord.createMainRecord("I1", "individual"));

		Assertions.assertEquals("\tindividual I1 {\n\t}\n".replace("\t", ""), text.replace("\t", ""));
	}

	@Test
	void expandedRoundTrip(){
		assertRoundTrip(FLEFWriter.create());
	}

	@Test
	void compactRoundTrip(){
		assertRoundTrip(FLEFWriter.createCompact());
	}

}
