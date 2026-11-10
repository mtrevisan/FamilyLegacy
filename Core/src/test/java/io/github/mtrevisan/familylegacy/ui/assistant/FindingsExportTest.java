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
package io.github.mtrevisan.familylegacy.ui.assistant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


/** The findings as a table: a line each, the fields that need it quoted, the ignored ones marked. */
class FindingsExportTest{

	private static Diagnostic finding(final Diagnostic.Severity severity, final String title, final String explanation,
			final List<String> records, final String key){
		return new Diagnostic(severity, AssistantCategory.PLACES, title, explanation, records, null, key);
	}


	@Test
	void everyFindingIsALineWithItsKindAndRecords(){
		final String csv = FindingsExport.toCsv(List.of(
			new FindingsExport.Entry(finding(Diagnostic.Severity.WARNING, "Possible duplicate Place: A and B", "why", List.of("P1", "P2"),
				"place.possible_duplicate|P1,P2|x"), false),
			new FindingsExport.Entry(finding(Diagnostic.Severity.ERROR, "Self", "why", List.of("P3"), "place.self_relation|P3|"), true)));

		final String[] lines = csv.split("\r\n");
		Assertions.assertEquals(3, lines.length, csv);
		Assertions.assertEquals("Severity;Category;Kind;Title;Explanation;Records;Ignored", lines[0]);
		Assertions.assertEquals("warning;Places;place.possible_duplicate;Possible duplicate Place: A and B;why;P1 P2;", lines[1]);
		Assertions.assertEquals("error;Places;place.self_relation;Self;why;P3;yes", lines[2]);
	}

	@Test
	void aFieldWithTheSeparatorAQuoteOrALineBreakIsQuoted(){
		final String csv = FindingsExport.toCsv(List.of(new FindingsExport.Entry(
			finding(Diagnostic.Severity.INFORMATION, "Treviso; Italia", "the \"city\"\nof Treviso", List.of("P1"), "k|P1|"), false)));

		Assertions.assertTrue(csv.contains("\"Treviso; Italia\";\"the \"\"city\"\"\nof Treviso\";P1"), csv);
	}

	@Test
	void theFileIsUtf8WithAByteOrderMarkSoThatASpreadsheetReadsTheAccents(@TempDir final Path directory) throws IOException{
		final Path file = directory.resolve("findings.csv");

		FindingsExport.write(file, List.of(new FindingsExport.Entry(
			finding(Diagnostic.Severity.INFORMATION, "Pieve di Sant'Angelo - Città", "why", List.of("P1"), "k|P1|"), false)));

		final byte[] bytes = Files.readAllBytes(file);
		Assertions.assertEquals((byte)0xEF, bytes[0]);
		Assertions.assertEquals((byte)0xBB, bytes[1]);
		Assertions.assertEquals((byte)0xBF, bytes[2]);
		Assertions.assertTrue(new String(bytes, StandardCharsets.UTF_8).contains("Città"));
	}

}
