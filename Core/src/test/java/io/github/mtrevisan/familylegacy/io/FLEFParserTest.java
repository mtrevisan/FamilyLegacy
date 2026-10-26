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

import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;


class FLEFParserTest{

	private static final String HEADER = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		""";

	private static final String INDIVIDUAL = """
		individual I1 {
			name {
				part {
					type given
					value Mario
				}
			}
			name {
				type alias
				part {
					type given
					value Mariolino
				}
			}
			audit.creation.date 2026-10-16T09:30:00+02:00
			audit.update {
				date 2026-10-17T09:30:00+02:00
				comment fixed name
			}
		}
		""";


	@BeforeAll
	static void loadGrammar() throws IOException{
		// the grammar tells the parser which fields are singular
		try(final InputStream is = FLEFParserTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			FLEFGrammarParser.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
		}
	}


	@Test
	void recordsAreTopLevelBlocks(){
		final FLEFModel model = new FLEFParser().parse(HEADER + INDIVIDUAL);

		Assertions.assertEquals(1, model.getRecordCount());
		Assertions.assertEquals("individual", model.getRecordById("I1").getTag());
		Assertions.assertEquals("0.2.0", FLEFRecordHelper.getChildValue(model.getHeader(), "protocol.version"));
	}

	@Test
	void theLegacyRecordsBlockIsStillRead(){
		final FLEFModel model = new FLEFParser().parse(HEADER + "records {\n" + INDIVIDUAL + "}\n");

		Assertions.assertEquals(1, model.getRecordCount());
		Assertions.assertNotNull(model.getRecordById("I1"));
	}

	@Test
	void aByteOrderMarkIsIgnored(){
		final FLEFModel model = new FLEFParser().parse('﻿' + HEADER + INDIVIDUAL);

		Assertions.assertNotNull(model.getRecordById("I1"));
	}

	@Test
	void blocksOfTheSameSingularFieldAreMerged(){
		final FLEFRecord individual = new FLEFParser().parse(HEADER + INDIVIDUAL)
			.getRecordById("I1");

		// AUDIT is singular: the dotted line and the block are the same audit
		Assertions.assertEquals(1, FLEFRecordHelper.findChildren(individual, "audit").size());
		Assertions.assertEquals("2026-10-16T09:30:00+02:00", FLEFRecordHelper.getChildValue(individual, "audit.creation.date"));
		Assertions.assertEquals("fixed name", FLEFRecordHelper.getChildValue(individual, "audit.update.comment"));
		// NAME is repeated: each block is a name
		Assertions.assertEquals(2, FLEFRecordHelper.findChildren(individual, "name").size());
	}

	@Test
	void theWriterFollowsTheInstanceSyntax() throws IOException{
		final FLEFModel model = new FLEFParser().parse(HEADER + INDIVIDUAL);

		final StringWriter out = new StringWriter();
		FLEFWriter.createCompact()
			.write(model, out);
		final String text = out.toString();

		Assertions.assertFalse(text.contains("records"), text);
		Assertions.assertTrue(text.contains("\nindividual I1 {"), text);
		Assertions.assertEquals(model, new FLEFParser().parse(text));
	}

}
