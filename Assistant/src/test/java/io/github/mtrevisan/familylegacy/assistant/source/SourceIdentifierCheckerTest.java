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


/** What the assistant says about the identifiers of a source, and of its repository citations. */
class SourceIdentifierCheckerTest{

	private static final String AUDIT = "\taudit.creation.date 2020-01-01T00:00:00Z\n";


	private static FLEFModel model(final String kind, final String body){
		return new FLEFParser().parse("repository R1 {\n\tname.value Archivio di Stato\n" + AUDIT + "}\n"
			+ "repository R2 {\n\tname.value FamilySearch\n" + AUDIT + "}\n"
			+ "source S1 {\n\ttitle.value Rossi Family History\n" + (kind != null? "\tkind " + kind + "\n": "") + body + AUDIT + "}\n");
	}

	private static String identifier(final String indent, final String scheme, final String value){
		return indent + "identifier {\n" + indent + "\tscheme " + scheme + "\n" + indent + "\tvalue " + value + "\n" + indent + "}\n";
	}

	private static String repository(final String id, final String identifiers, final String locator){
		return "\trepository {\n\t\trepository " + id + "\n" + identifiers + (locator != null? "\t\tlocator " + locator + "\n": "") + "\t}\n";
	}

	private static List<Diagnostic> findings(final String kind, final String body){
		return SourceIdentifierChecker.findings(model(kind, body), "S1");
	}

	private static List<Diagnostic> ofKind(final List<Diagnostic> findings, final String kind){
		return findings.stream()
			.filter(finding -> finding.kind().equals(kind))
			.toList();
	}


	@Test
	void nothingSelectedOrNotASourceGetsNoFinding(){
		final FLEFModel model = model("book", "");

		Assertions.assertEquals(List.of(), SourceIdentifierChecker.findings(model, null));
		Assertions.assertEquals(List.of(), SourceIdentifierChecker.findings(model, "X9"));
		Assertions.assertEquals(List.of(), SourceIdentifierChecker.findings(model, "R1"));
		Assertions.assertEquals(List.of(), SourceIdentifierChecker.findings(null, "S1"));
	}

	@Test
	void aWrongCheckDigitIsAWarningAboutTheSource(){
		final List<Diagnostic> findings = ofKind(findings(null, identifier("\t", "isbn", "9780199535560")), "source_identifier.invalid");

		Assertions.assertEquals(1, findings.size());
		final Diagnostic finding = findings.getFirst();
		Assertions.assertEquals(Diagnostic.Severity.WARNING, finding.severity());
		Assertions.assertEquals(List.of("S1"), finding.recordIds());
		Assertions.assertTrue(finding.explanation().contains("9780199535560"), finding.explanation());
		Assertions.assertNull(finding.action(), "it suggests, it does not act");
		Assertions.assertTrue(finding.isIgnorable());
	}

	@Test
	void anIdentifierOfARepositoryCitationIsCheckedToo(){
		final List<Diagnostic> findings = ofKind(findings(null, repository("R1", identifier("\t\t", "issn", "0378-5954"), null)),
			"source_identifier.invalid");

		Assertions.assertEquals(1, findings.size());
		Assertions.assertEquals(Diagnostic.Severity.WARNING, findings.getFirst().severity());
	}

	@Test
	void validIdentifiersAndTheOnesWithoutARuleAreNotReported(){
		final List<Diagnostic> findings = findings(null, identifier("\t", "isbn", "978-0-19-953556-9")
			+ identifier("\t", "fhl_film", "x") + repository("R1", identifier("\t\t", "call_number", "MS 45/12"), "Page 27"));

		Assertions.assertEquals(List.of(), ofKind(findings, "source_identifier.invalid"));
		Assertions.assertEquals(List.of(), ofKind(findings, "source_identifier.scheme_mismatch"));
		Assertions.assertEquals(List.of(), ofKind(findings, "source_identifier.locator_holds_identifier"));
		Assertions.assertEquals(List.of(), ofKind(findings, "source_identifier.repository_without_identifier"));
	}

	@Test
	void aDoiWrittenAsAUrlIsRecognized(){
		final List<Diagnostic> findings = ofKind(findings(null, identifier("\t", "url", "https://doi.org/10.1000/182")),
			"source_identifier.scheme_mismatch");

		Assertions.assertEquals(1, findings.size());
		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, findings.getFirst().severity());
		Assertions.assertTrue(findings.getFirst().title().contains("DOI"), findings.getFirst().title());
	}

	@Test
	void anIsbnWithACustomSchemeIsRecognizedAndAnIdentifierOfTheRightSchemeIsNot(){
		Assertions.assertEquals(1, ofKind(findings(null, identifier("\t", "ISBN-13", "978-0-19-953556-9")), "source_identifier.scheme_mismatch")
			.size());
		Assertions.assertEquals(0, ofKind(findings(null, identifier("\t", "ISBN", "978-0-19-953556-9")), "source_identifier.scheme_mismatch")
			.size());
		Assertions.assertEquals(0, ofKind(findings(null, identifier("\t", "doi", "10.1000/182")), "source_identifier.scheme_mismatch")
			.size());
	}

	@Test
	void aLocatorThatHoldsAShelfMarkOrACallNumberIsToBeAnIdentifier(){
		final List<Diagnostic> findings = ofKind(findings(null, repository("R1", identifier("\t\t", "dewey", "929.2 ROS"),
			"Shelf Mark: 12, Call number: MS 45/12, Frame: 344")), "source_identifier.locator_holds_identifier");

		Assertions.assertEquals(1, findings.size());
		final String text = findings.getFirst().explanation();
		Assertions.assertTrue(text.contains("Shelf Mark: 12") && text.contains("Call number: MS 45/12"), text);
		Assertions.assertFalse(text.contains("Frame: 344,"), "the frame is a position: it stays a locator");
	}

	@Test
	void aLocatorThatIsOnlyAPositionIsLeftAlone(){
		Assertions.assertEquals(List.of(), ofKind(findings(null, repository("R1", identifier("\t\t", "call_number", "MS 1"),
			"Volume: 2, Page: 143, Entry: 17, Folio: 8r, Frame: 213")), "source_identifier.locator_holds_identifier"));
	}

	@Test
	void aRepositoryCitationWithoutAnIdentifierIsToBeCompleted(){
		final List<Diagnostic> findings = ofKind(findings(null, repository("R1", "", "Page 27")), "source_identifier.repository_without_identifier");

		Assertions.assertEquals(1, findings.size());
		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, findings.getFirst().severity());
		Assertions.assertTrue(findings.getFirst().title().contains("Archivio di Stato"), findings.getFirst().title());
	}

	@Test
	void aPublishedKindWithoutAnyIdentifierIsToBeIdentified(){
		for(final String kind : List.of("book", "periodical", "compilation")){
			final List<Diagnostic> findings = ofKind(findings(kind, ""), "source_identifier.missing");

			Assertions.assertEquals(1, findings.size(), kind);
			Assertions.assertTrue(findings.getFirst().explanation().contains("ISBN"), findings.getFirst().explanation());
		}
		final List<Diagnostic> website = ofKind(findings("website", ""), "source_identifier.missing");
		Assertions.assertEquals(1, website.size());
		Assertions.assertTrue(website.getFirst().explanation().contains("URL"), website.getFirst().explanation());
	}

	@Test
	void aSourceThatHasAnIdentifierOfItsOwnOrInItsRepositoryIsIdentified(){
		Assertions.assertEquals(List.of(), ofKind(findings("book", identifier("\t", "isbn", "978-0-19-953556-9")), "source_identifier.missing"));
		Assertions.assertEquals(List.of(), ofKind(findings("book", repository("R1", identifier("\t\t", "call_number", "MS 1"), null)),
			"source_identifier.missing"));
		//other kinds are not published works
		Assertions.assertEquals(List.of(), ofKind(findings("church_register", ""), "source_identifier.missing"));
		Assertions.assertEquals(List.of(), ofKind(findings(null, ""), "source_identifier.missing"));
	}

	@Test
	void aMicrofilmOfFamilySearchIsExplainedAndIsToBeRecordedWhenMissing(){
		final List<Diagnostic> film = ofKind(findings("church_register", identifier("\t", "fhl_film", "1234567")), "source_identifier.film");
		Assertions.assertEquals(1, film.size());
		Assertions.assertTrue(film.getFirst().title().contains("1234567"), film.getFirst().title());
		Assertions.assertTrue(film.getFirst().explanation().contains("FamilySearch Catalog"), film.getFirst().explanation());

		//a register kept at FamilySearch, with no film or digital folder: the number is asked
		Assertions.assertEquals(1, ofKind(findings("church_register", repository("R2", "", null)), "source_identifier.fhl_missing").size());
		Assertions.assertEquals(0, ofKind(findings("church_register", repository("R1", "", null)), "source_identifier.fhl_missing").size());
		Assertions.assertEquals(0, ofKind(findings("church_register", repository("R2", identifier("\t\t", "fhl_film", "1"), null)),
			"source_identifier.fhl_missing").size());
		Assertions.assertEquals(0, ofKind(findings("church_register", identifier("\t", "fhl_digital_folder", "1") + repository("R2", "", null)),
			"source_identifier.fhl_missing").size());
		Assertions.assertEquals(0, ofKind(findings("book", repository("R2", "", null)), "source_identifier.fhl_missing").size());
	}

	@Test
	void aFilmInTheSourceAndInItsCitationIsExplainedOnce(){
		final List<Diagnostic> film = ofKind(findings(null, identifier("\t", "fhl_film", "1234567")
			+ repository("R2", identifier("\t\t", "fhl_film", "1234567"), null)), "source_identifier.film");

		Assertions.assertEquals(1, film.size());
	}

	@Test
	void everyFindingHasItsMessagesAndAKeyThatChangesWithTheData(){
		final List<Diagnostic> all = findings("church_register", identifier("\t", "isbn", "9780199535560")
			+ identifier("\t", "url", "https://doi.org/10.1000/182") + identifier("\t", "fhl_film", "1234567")
			+ repository("R2", "", "Shelf Mark: 12, Page 3"));

		Assertions.assertFalse(all.isEmpty());
		for(final Diagnostic finding : all){
			Assertions.assertTrue(finding.kind().startsWith("source_identifier."), finding.kind());
			Assertions.assertNotNull(I18N.t(finding.kind() + ".title", null), finding.kind());
			Assertions.assertNotNull(I18N.t("assistant.kind." + finding.kind(), null), finding.kind());
			Assertions.assertFalse(finding.title().contains("{") || finding.explanation().contains("{"), finding.title());
		}
		final String before = ofKind(findings(null, identifier("\t", "isbn", "9780199535560")), "source_identifier.invalid").getFirst().key();
		final String after = ofKind(findings(null, identifier("\t", "isbn", "9780199535561")), "source_identifier.invalid").getFirst().key();
		Assertions.assertNotEquals(before, after);
	}

}
