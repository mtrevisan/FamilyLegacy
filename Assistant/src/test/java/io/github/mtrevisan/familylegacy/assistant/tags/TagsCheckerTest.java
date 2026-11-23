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
package io.github.mtrevisan.familylegacy.assistant.tags;

import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The assistant tells the records that share a tag with the one the user is looking at. */
class TagsCheckerTest{

	private static final String AUDIT = "\taudit.creation.date 2020-01-01T00:00:00Z\n";


	private static FLEFModel model(){
		final FLEFModel model = new FLEFParser().parse("source S1 {\n\ttitle.value Register\n" + AUDIT + "}\n"
			+ "source S2 {\n\ttitle.value Original\n" + AUDIT + "}\n"
			+ "source S3 {\n\ttitle.value Another\n" + AUDIT + "}\n");
		model.tags().set("S1", List.of("Brick Wall", "To Verify"));
		model.tags().set("S2", List.of("brick wall"));
		model.tags().set("S3", List.of("Immigration"));
		return model;
	}


	@Test
	void aFindingForEachTagThatOtherRecordsCarry(){
		final List<Diagnostic> findings = TagsChecker.findings(model(), "S1");

		Assertions.assertEquals(1, findings.size());
		Assertions.assertEquals(List.of("S2"), findings.getFirst().recordIds());
		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, findings.getFirst().severity());
		Assertions.assertEquals(TagsChecker.KIND, findings.getFirst().kind());
	}

	@Test
	void nothingSelectedOrNotTaggedGetsNoFinding(){
		final FLEFModel model = model();

		Assertions.assertEquals(List.of(), TagsChecker.findings(model, null));
		Assertions.assertEquals(List.of(), TagsChecker.findings(model, "X9"));
		Assertions.assertEquals(List.of(), TagsChecker.findings(null, "S1"));
		Assertions.assertEquals(List.of(), TagsChecker.findings(model, "S3"));
	}

	@Test
	void aTagOfARecordThatIsNotThereIsNotListed(){
		final FLEFModel model = model();
		model.tags().add("X9", "Brick Wall");

		Assertions.assertEquals(List.of("S2"), TagsChecker.findings(model, "S1").getFirst().recordIds());
	}

}
