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
package io.github.mtrevisan.familylegacy.assistant.source;

import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** What the assistant says about a source depends on its kind. */
class SourceKindCheckerTest{

	private static final String AUDIT = "\taudit.creation.date 2020-01-01T00:00:00Z\n";


	private static FLEFModel model(final String kind, final String extra){
		return new FLEFParser().parse("source S1 {\n\ttitle.value Register\n" + (kind != null? "\tkind " + kind + "\n": "") + extra + AUDIT + "}\n"
			+ "source S2 {\n\ttitle.value Original\n" + AUDIT + "}\n");
	}


	@Test
	void aSourceWithoutAKindGetsNoFinding(){
		Assertions.assertEquals(List.of(), SourceKindChecker.findings(model(null, ""), "S1"));
	}

	@Test
	void aFreeTextKindGetsNoFinding(){
		Assertions.assertEquals(List.of(), SourceKindChecker.findings(model("land_survey", ""), "S1"));
	}

	@Test
	void nothingSelectedOrNotASourceGetsNoFinding(){
		final FLEFModel model = model("census", "");

		Assertions.assertEquals(List.of(), SourceKindChecker.findings(model, null));
		Assertions.assertEquals(List.of(), SourceKindChecker.findings(model, "X9"));
		Assertions.assertEquals(List.of(), SourceKindChecker.findings(null, "S1"));
	}

	@Test
	void everyAdvisedKindHasItsTextAndIsAboutTheSource(){
		for(final String kind : SourceKindChecker.ADVISED){
			final List<Diagnostic> findings = SourceKindChecker.findings(model(kind, ""), "S1");

			Assertions.assertEquals(1, findings.size(), kind);
			final Diagnostic finding = findings.getFirst();
			Assertions.assertEquals(List.of("S1"), finding.recordIds(), kind);
			Assertions.assertEquals("source_kind." + kind, finding.kind(), kind);
			Assertions.assertNotNull(I18N.t("source_kind.advice." + kind, null), kind);
			Assertions.assertNull(finding.action(), "it suggests, it does not act");
			Assertions.assertTrue(finding.isIgnorable());
		}
	}

	@Test
	void theAdviceFollowsTheKind(){
		Assertions.assertTrue(SourceKindChecker.findings(model("church_register", ""), "S1").getFirst().explanation().contains("baptisms"));
		Assertions.assertTrue(SourceKindChecker.findings(model("census", ""), "S1").getFirst().explanation().contains("household"));
		Assertions.assertTrue(SourceKindChecker.findings(model("military_record", ""), "S1").getFirst().explanation().contains("service timeline"));
		Assertions.assertTrue(SourceKindChecker.findings(model("cemetery_record", ""), "S1").getFirst().explanation().contains("burial"));
		final String school = SourceKindChecker.findings(model("school_record", ""), "S1").getFirst().explanation();
		Assertions.assertTrue(school.contains("education") && school.contains("residence") && school.contains("age")
			&& school.contains("parents or guardians"), school);
	}

	@Test
	void aCompilationIsToBeTracedBackToItsOriginalsUnlessItSaysWhereFromItComes(){
		Assertions.assertTrue(SourceKindChecker.findings(model("compilation", ""), "S1").getFirst().explanation()
			.contains("underlying primary sources"));

		final FLEFModel derived = model("compilation", "\tderived_from.source S2\n");
		Assertions.assertEquals(List.of(), SourceKindChecker.findings(derived, "S1"));
	}

}
