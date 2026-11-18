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

import java.io.ByteArrayInputStream;
import java.io.IOException;


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

	@Test
	void quotesAndBackslashesInASingleLineValueAreWrittenAsTheyAreAndSurviveTheRoundTrip(){
		final String value = "He said \"hi\" and \"\"\"bye\"\"\" in C:\\temp\\new";
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", "individual")
			.addChild(FLEFRecord.createChildWithTagAndValue("note", value)));

		final String text = FLEFWriter.create().writeToString(model);

		//no text block, no escape: the value is read verbatim up to the end of the line
		Assertions.assertTrue(text.contains("note " + value + "\n"), text);
		Assertions.assertEquals(value, new FLEFParser().parse(text).getRecordById("I1").getChildren().getFirst().getValue());
	}

	@Test
	void escapesInAMultilineValueSurviveTheRoundTrip(){
		final String value = "C:\\temp\\new\nsay \"\"\"hi\"\"\"\n\\s";
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", "individual")
			.addChild(FLEFRecord.createChildWithTagAndValue("note", value)));

		final FLEFModel reread = new FLEFParser().parse(FLEFWriter.create().writeToString(model));

		Assertions.assertEquals(value, reread.getRecordById("I1").getChildren().getFirst().getValue());
	}

	@Test
	void aMultilineValueIsNotEscapedMoreThanNeeded(){
		final String value = "He said \"hi\" and \"\"ok\"\"\n  indented line\nlast \"quoted\"";
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", "individual")
			.addChild(FLEFRecord.createChildWithTagAndValue("note", value)));

		final String text = FLEFWriter.create().writeToString(model);

		Assertions.assertTrue(text.contains("He said \"hi\" and \"\"ok\"\"\n  indented line\nlast \"quoted\\\"\"\"\""), text);
		Assertions.assertEquals(value, new FLEFParser().parse(text).getRecordById("I1").getChildren().getFirst().getValue());
	}

	@Test
	void textBlocksFollowTheJavaRules(){
		final String text = "note \"\"\"\n    a  \n      b\\\n    c\\tx\\101\n    \"\"\"\n";

		final String value = new FLEFParser().parse("individual I1 {\n" + text + "}\n").getRecordById("I1").getChildren().getFirst()
			.getValue();

		//common indentation removed, trailing whitespace stripped, line continuation, escapes, octal
		Assertions.assertEquals("a\n  bc\txA\n", value);
	}

	@Test
	void whitespaceOnlyLinesAndTrailingWhitespaceSurviveTheRoundTrip(){
		for(final String value : new String[]{"a  \nb", "a\n   \nb", "   \n   ", "a \t", "  x  \n  y  "}){
			final FLEFModel model = new FLEFModel();
			model.addRecord(FLEFRecord.createMainRecord("I1", "individual")
				.addChild(FLEFRecord.createChildWithTagAndValue("note", value)));

			final FLEFModel reread = new FLEFParser().parse(FLEFWriter.create().writeToString(model));

			Assertions.assertEquals(value, reread.getRecordById("I1").getChildren().getFirst().getValue(), value);
		}
	}

	@Test
	void singleLineValuesThatLookLikeSyntaxSurviveTheRoundTrip(){
		final String[] values = {"", " ", "  leading", "trailing  ", "\tx", "ends with {", "{", "{ starts", "\"\"\"x", "a\n  indented", "  a\n  b",
			"\\s", "plain", "a\"\"\"\"\"\"b", "x\n  \"", "  \"\"\""};
		for(final boolean compact : new boolean[]{false, true}){
			final FLEFWriter writer = (compact? FLEFWriter.createCompact(): FLEFWriter.create());
			for(final String value : values){
				final FLEFModel model = new FLEFModel();
				model.addRecord(FLEFRecord.createMainRecord("I1", "individual")
					.addChild(FLEFRecord.createChildWithTagAndValue("note", value))
					.addChild(FLEFRecord.createChildWithTagAndValue("after", "next")));

				final FLEFModel reread = new FLEFParser().parse(writer.writeToString(model));

				final FLEFRecord record = reread.getRecordById("I1");
				Assertions.assertEquals(2, record.getChildren().size(), value);
				Assertions.assertEquals(value, record.getChildren().getFirst().getValue(), value);
				Assertions.assertEquals("next", record.getChildren().get(1).getValue(), value);
			}
		}
	}

	@Test
	void aValueWithAChildBlockSurvivesTheRoundTrip(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", "individual")
			.addChild(FLEFRecord.createChildWithTagAndValue("note", "text")
				.addChild(FLEFRecord.createChildWithTagAndValue("lang", "en"))));

		final FLEFModel reread = new FLEFParser().parse(FLEFWriter.create().writeToString(model));

		final FLEFRecord note = reread.getRecordById("I1").getChildren().getFirst();
		Assertions.assertEquals("text", note.getValue());
		Assertions.assertNull(note.getId());
		Assertions.assertEquals("en", note.getChildren().getFirst().getValue());
	}

	@Test
	void parsingAnEmptyStreamGivesAnEmptyModel() throws IOException{
		final FLEFModel model = new FLEFParser().parse(new ByteArrayInputStream(new byte[0]));

		Assertions.assertTrue(model.isEmpty());
	}

}
