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
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.Cardinality;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;


class FLEFGrammarFileDefinitionTest{

	@Test
	void fileAsAPlainListOfRecords(){
		final FLEFGrammar grammar = FLEFGrammarParser.parse("""
			file F {
			  Record+
			}
			Record = oneof {
			  header: Header
			  note: NoteRecord
			}
			record Header {
			  version: string
			}
			record NoteRecord {
			  text: string
			}
			""");

		Assertions.assertNull(grammar.getFileDefinition().headerField());
		Assertions.assertEquals("Record", grammar.getFileDefinition().recordsField().type().getName());
		Assertions.assertEquals(Cardinality.ONE_OR_MORE, grammar.getFileDefinition().recordsField().cardinality());
		Assertions.assertEquals("Header", grammar.getHeaderType().getName());
	}

	@Test
	void legacyFileWithHeaderAndRecordsFields(){
		final FLEFGrammar grammar = FLEFGrammarParser.parse("""
			file F {
			  header: Header
			  records*: Record
			}
			Record = oneof {
			  note: NoteRecord
			}
			record Header {
			  version: string
			}
			record NoteRecord {
			  text: string
			}
			""");

		Assertions.assertNotNull(grammar.getFileDefinition().headerField());
		Assertions.assertEquals("Header", grammar.getHeaderType().getName());
	}

	@Test
	void aFileDefinedAsAHeaderFollowedByRecords(){
		final FLEFGrammar grammar = FLEFGrammarParser.parse("""
			F := Header Record*
			Record = oneof {
			  note: NoteRecord
			}
			record Header {
			  version: string
			}
			record NoteRecord {
			  text: string
			}
			""");

		Assertions.assertEquals("F", grammar.getFileDefinition().name());
		Assertions.assertEquals("Header", grammar.getFileDefinition().headerField().type().getName());
		Assertions.assertEquals(Cardinality.REQUIRED, grammar.getFileDefinition().headerField().cardinality());
		Assertions.assertEquals("Record", grammar.getFileDefinition().recordsField().type().getName());
		Assertions.assertEquals(Cardinality.ZERO_OR_MORE, grammar.getFileDefinition().recordsField().cardinality());
		Assertions.assertEquals("Header", grammar.getHeaderType().getName());
	}

	@Test
	void theProtocolGrammarIsAHeaderFollowedByRecords() throws IOException{
		try(final InputStream stream = getClass().getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			final FLEFGrammar grammar = FLEFGrammarParser.parse(stream);

			Assertions.assertEquals("FamilyLegacyFile", grammar.getFileDefinition().name());
			Assertions.assertEquals("HeaderRecord", grammar.getHeaderType().getName());
			Assertions.assertEquals("Record", grammar.getFileDefinition().recordsField().type().getName());
			Assertions.assertEquals(Cardinality.ZERO_OR_MORE, grammar.getFileDefinition().recordsField().cardinality());
			Assertions.assertNotNull(grammar.getType("HeaderRecord"));
		}
	}

}
