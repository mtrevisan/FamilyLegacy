package io.github.mtrevisan.familylegacy.ui.assistant.conclusion;

import io.github.mtrevisan.familylegacy.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** The checks on a real model: what is read from the records, and what is found. */
class ConclusionDataLoaderTest{

	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Anna /Rossi/
		1 SEX F
		1 BIRT
		2 DATE 1 JAN 1850
		1 BIRT
		2 DATE 1 JAN 1860
		0 @I2@ INDI
		1 NAME Dino /Rossi/
		1 SEX M
		1 FAMC @F1@
		1 FAMC @F2@
		0 @I3@ INDI
		1 NAME Egidio /Bianchi/
		1 SEX M
		0 @I4@ INDI
		1 NAME Fede /Verdi/
		1 SEX F
		0 @I5@ INDI
		1 NAME Gino /Neri/
		1 SEX M
		0 @I6@ INDI
		1 NAME Livio /Gialli/
		1 SEX M
		1 FAMC @F4@
		0 @I7@ INDI
		1 NAME Mario /Gialli/
		1 SEX M
		1 FAMC @F3@
		0 @F1@ FAM
		1 HUSB @I3@
		1 WIFE @I4@
		1 CHIL @I2@
		0 @F2@ FAM
		1 HUSB @I5@
		1 WIFE @I4@
		1 CHIL @I2@
		0 @F3@ FAM
		1 HUSB @I6@
		1 CHIL @I7@
		0 @F4@ FAM
		1 HUSB @I7@
		1 CHIL @I6@
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static List<String> titles(final FLEFModel model){
		return ConclusionChecker.check(ConclusionDataLoader.load(model)).stream()
			.map(Diagnostic::title)
			.toList();
	}


	@Test
	void findsTheConflictsOfAModel() throws IOException{
		final List<String> titles = titles(convert());

		Assertions.assertTrue(titles.stream().anyMatch(t -> t.startsWith("Conflicting birth dates: Anna")), titles.toString());
		Assertions.assertTrue(titles.stream().anyMatch(t -> t.startsWith("More than two biological parents: Dino")),
			titles.toString());
		Assertions.assertTrue(titles.stream().anyMatch(t -> t.startsWith("Own ancestor")), titles.toString());
	}

	@Test
	void aConclusionRecordSettlesAConflict() throws IOException{
		final FLEFModel model = convert();
		final List<ConclusionData.PrincipalEvent> births = ConclusionDataLoader.load(model).events().stream()
			.filter(e -> "birth".equals(e.type()))
			.toList();
		Assertions.assertEquals(2, births.size());

		final FLEFRecord conclusion = FLEFRecord.createMainRecord("C1", "conclusion");
		conclusion.addChild(FLEFRecord.createChildWithTagAndValue("issue", "birth_date"));
		conclusion.addChild(FLEFRecord.createChildWithTagAndValue("proof_status", "supported"));
		for(final ConclusionData.PrincipalEvent birth : births)
			conclusion.addChild(FLEFRecord.createChildWithTag("resolves")
				.addChild(FLEFRecord.createChildWithTagAndValue("event", birth.eventId())));
		conclusion.addChild(FLEFRecord.createChildWithTag("preferred")
			.addChild(FLEFRecord.createChildWithTagAndValue("event", births.getFirst().eventId())));
		model.addRecord(conclusion);

		final List<String> titles = titles(model);
		Assertions.assertTrue(titles.stream().noneMatch(t -> t.contains("birth dates")), titles.toString());
		//the other conflicts are still there
		Assertions.assertTrue(titles.stream().anyMatch(t -> t.startsWith("Own ancestor")), titles.toString());
		Assertions.assertEquals(1, ConclusionDataLoader.load(model).conclusions().size());
		Assertions.assertEquals(births.getFirst().eventId(), ConclusionDataLoader.load(model).conclusions().getFirst().preferred());
	}

	@Test
	void everyFindingOfTheAssistantHasTheActionThatStartsTheDraft() throws IOException{
		final FLEFModel model = convert();

		final List<Diagnostic> diagnostics = new ConclusionAssistantContributor().diagnosticProviders().getFirst()
			.diagnose(new AssistantContext(null, null, model));

		Assertions.assertFalse(diagnostics.isEmpty());
		for(final Diagnostic diagnostic : diagnostics){
			Assertions.assertNotNull(diagnostic.action(), diagnostic.title());
			Assertions.assertEquals("Create Conclusion Draft…", diagnostic.action().title());
		}
		//without a model there is nothing to report
		Assertions.assertTrue(new ConclusionAssistantContributor().diagnosticProviders().getFirst()
			.diagnose(new AssistantContext(null, null, null)).isEmpty());
	}

	@Test
	void theRecordsOfADraftAreInTheModel() throws IOException{
		final FLEFModel model = convert();

		final List<ConclusionFinding> findings = ConclusionChecker.findings(ConclusionDataLoader.load(model));

		Assertions.assertFalse(findings.isEmpty());
		for(final ConclusionFinding finding : findings)
			for(final String id : finding.draft().resolves())
				Assertions.assertNotNull(model.getRecordById(id), id + " of " + finding.diagnostic().title());
	}

	@Test
	void anEmptyModelHasNothingToReport() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader("0 HEAD\n1 CHAR UTF-8\n"));

		Assertions.assertTrue(titles(new GEDCOMToFLEFConverter().convert(roots)).isEmpty());
	}

}
