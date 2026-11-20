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
package io.github.mtrevisan.familylegacy.quality;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** An identifier that fails the check of its scheme is a warning of the validation, never an error. */
class IdentifierConsistencyTest{

	private static final String AUDIT = "\taudit.creation.date 2020-01-01T00:00:00Z\n";


	private static List<ConsistencyService.Issue> issuesOf(final String sourceBody){
		final FLEFModel model = new FLEFParser().parse("repository R1 {\n\tname.value Archive\n" + AUDIT + "}\n"
			+ "source S1 {\n\ttitle.value Rossi Family History\n" + sourceBody + AUDIT + "}\n");
		return ConsistencyService.check(model).issues().stream()
			.filter(issue -> issue.kind().endsWith("identifier"))
			.toList();
	}

	private static String identifier(final String indent, final String scheme, final String value){
		return indent + "identifier {\n" + indent + "\tscheme " + scheme + "\n" + indent + "\tvalue " + value + "\n" + indent + "}\n";
	}


	@Test
	void validIdentifiersSayNothing(){
		Assertions.assertEquals(List.of(), issuesOf(identifier("\t", "isbn", "978-0-19-953556-9")
			+ identifier("\t", "issn", "0378-5955") + identifier("\t", "url", "https://example.org/rossi")
			+ identifier("\t", "doi", "10.1000/182")));
	}

	@Test
	void theSchemesWithoutARuleSayNothing(){
		Assertions.assertEquals(List.of(), issuesOf(identifier("\t", "fhl_film", "not a number at all")
			+ identifier("\t", "Barcode", "???") + "\trepository {\n\t\trepository R1\n" + identifier("\t\t", "call_number", "MS 45/12")
			+ identifier("\t\t", "dewey", "929.2 ROS") + "\t}\n"));
	}

	@Test
	void aWrongIsbnIsAWarningOfTheSource(){
		final List<ConsistencyService.Issue> issues = issuesOf(identifier("\t", "isbn", "9780199535560"));

		Assertions.assertEquals(1, issues.size());
		final ConsistencyService.Issue issue = issues.getFirst();
		Assertions.assertEquals(ConsistencyService.Severity.WARNING, issue.severity());
		Assertions.assertEquals("S1", issue.recordId());
		Assertions.assertEquals("invalid_identifier", issue.kind());
		Assertions.assertTrue(issue.message().contains("9780199535560"), issue.message());
	}

	@Test
	void aWrongIssnAndAMalformedUrlAreWarnings(){
		final List<ConsistencyService.Issue> issues = issuesOf(identifier("\t", "issn", "0378-5954")
			+ identifier("\t", "url", "not a url"));

		Assertions.assertEquals(2, issues.size());
		for(final ConsistencyService.Issue issue : issues)
			Assertions.assertEquals(ConsistencyService.Severity.WARNING, issue.severity());
	}

	@Test
	void anIdentifierOfARepositoryCitationIsReportedWithTheRepository(){
		final List<ConsistencyService.Issue> issues = issuesOf("\trepository {\n\t\trepository R1\n"
			+ identifier("\t\t", "isbn", "123") + "\t}\n");

		Assertions.assertEquals(1, issues.size());
		Assertions.assertEquals("invalid_repository_identifier", issues.getFirst().kind());
		Assertions.assertEquals(ConsistencyService.Severity.WARNING, issues.getFirst().severity());
		Assertions.assertTrue(issues.getFirst().message().contains("R1"), issues.getFirst().message());
	}

	@Test
	void theSchemeIsReadIgnoringTheCase(){
		Assertions.assertEquals(1, issuesOf(identifier("\t", "ISBN", "9780199535560")).size());
	}

}
