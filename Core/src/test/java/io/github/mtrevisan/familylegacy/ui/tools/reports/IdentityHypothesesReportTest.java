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
package io.github.mtrevisan.familylegacy.ui.tools.reports;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.LivingPrivacy;
import io.github.mtrevisan.familylegacy.reports.HypothesisFilter;
import io.github.mtrevisan.familylegacy.reports.HypothesisRecordType;
import io.github.mtrevisan.familylegacy.reports.ReportBuilder;
import io.github.mtrevisan.familylegacy.reports.ReportConfig;
import io.github.mtrevisan.familylegacy.reports.ReportOption;
import io.github.mtrevisan.familylegacy.reports.ReportProgressListener;
import io.github.mtrevisan.familylegacy.reports.ReportType;
import io.github.mtrevisan.reports.ReportDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** The Identity Hypotheses report: open, accepted and rejected hypotheses, with what is known of each. */
class IdentityHypothesesReportTest{

	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Anna /Rossi/
		0 @I2@ INDI
		1 NAME Anna /Rosi/
		0 @I3@ INDI
		1 NAME Dino /Bianchi/
		0 @I4@ INDI
		1 NAME Dino /Bianki/
		0 @I5@ INDI
		1 NAME Egidio /Verdi/
		0 @I6@ INDI
		1 NAME Egidio /Verdy/
		0 @S1@ SOUR
		1 TITL Parish register
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static void hypothesis(final FLEFModel model, final String id, final String left, final String right, final String confidence){
		final FLEFRecord hypothesis = FLEFRecord.createMainRecord(id, IdentityHypothesisReader.TYPE);
		hypothesis.addChild(FLEFRecord.createChildWithTag(IdentityHypothesisReader.TAG_IDENTITY)
			.addChild(FLEFRecord.createChildWithTagAndValue(IdentityHypothesisReader.TAG_IDENTITY_INDIVIDUAL, left)));
		hypothesis.addChild(FLEFRecord.createChildWithTag(IdentityHypothesisReader.TAG_IDENTITY)
			.addChild(FLEFRecord.createChildWithTagAndValue(IdentityHypothesisReader.TAG_IDENTITY_INDIVIDUAL, right)));
		hypothesis.addChild(FLEFRecord.createChildWithTagAndValue(IdentityHypothesisReader.TAG_RATIONALE, "same parents"));
		if(confidence != null)
			hypothesis.addChild(FLEFRecord.createChildWithTagAndValue(IdentityHypothesisReader.TAG_CONFIDENCE, confidence));
		model.addRecord(hypothesis);
	}

	private static void conclude(final FLEFModel model, final String id, final String hypothesisId, final String proofStatus){
		final FLEFRecord conclusion = FLEFRecord.createMainRecord(id, ConclusionReader.TYPE);
		conclusion.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_ISSUE, "identity"));
		conclusion.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_PROOF_STATUS, proofStatus));
		conclusion.addChild(FLEFRecord.createChildWithTag(ConclusionReader.TAG_RESOLVES)
			.addChild(FLEFRecord.createChildWithTagAndValue(IdentityHypothesisReader.TYPE.toLowerCase(), hypothesisId)));
		model.addRecord(conclusion);
	}

	/** An open, an accepted and a rejected hypothesis. */
	private static FLEFModel threeHypotheses() throws IOException{
		final FLEFModel model = convert();
		hypothesis(model, "IH1", "I1", "I2", "high");
		hypothesis(model, "IH2", "I3", "I4", "low");
		hypothesis(model, "IH3", "I5", "I6", null);
		conclude(model, "C1", "IH2", ConclusionReader.ENUM_PROOF_STATUS_PROVEN);
		conclude(model, "C2", "IH3", ConclusionReader.ENUM_PROOF_STATUS_DISPROVEN);
		return model;
	}

	private static String text(final FLEFModel model, final ReportConfig base){
		final ReportConfig config = base.withLivingPrivacy(LivingPrivacy.SHOW);
		final ReportDocument document = ReportBuilder.build(ReportType.IDENTITY_HYPOTHESES, model, null, config, ReportProgressListener.NOOP);
		return document.title() + "\n" + document.sections();
	}


	@Test
	void everyHypothesisIsListedUnderItsState() throws IOException{
		final String text = text(threeHypotheses(), ReportConfig.defaults());

		Assertions.assertTrue(text.startsWith("Identity Hypotheses\n"), text);
		for(final String expected : List.of("Open hypotheses (1)", "Accepted hypotheses (1)", "Rejected hypotheses (1)", "same parents",
				"Anna Rossi", "Dino Bianchi", "Egidio Verdi", "high", "not stated", "Supporting evidence", "Contradicting evidence",
				"proven", "disproven", "[C1]", "[C2]"))
			Assertions.assertTrue(text.contains(expected), expected + " is missing in " + text);
		Assertions.assertTrue(text.indexOf("Open hypotheses") < text.indexOf("Accepted hypotheses"), text);
		Assertions.assertTrue(text.indexOf("Accepted hypotheses") < text.indexOf("Rejected hypotheses"), text);
	}

	@Test
	void conflictingEvidenceLeavesTheHypothesisOpen() throws IOException{
		final FLEFModel model = convert();
		hypothesis(model, "IH1", "I1", "I2", null);
		conclude(model, "C1", "IH1", ConclusionReader.ENUM_PROOF_STATUS_CONFLICTING_EVIDENCE);

		final String text = text(model, ReportConfig.defaults());

		Assertions.assertTrue(text.contains("Open hypotheses (1)"), text);
		Assertions.assertFalse(text.contains("Accepted hypotheses"), text);
	}

	@Test
	void theFilterByStateKeepsOnlyThatState() throws IOException{
		final FLEFModel model = threeHypotheses();

		final String open = text(model, ReportConfig.defaults().with(ReportOption.HYPOTHESIS_FILTER, HypothesisFilter.OPEN));
		final String rejected = text(model, ReportConfig.defaults().with(ReportOption.HYPOTHESIS_FILTER, HypothesisFilter.REJECTED));

		Assertions.assertTrue(open.contains("Anna Rossi") && !open.contains("Dino Bianchi") && !open.contains("Egidio Verdi"), open);
		Assertions.assertTrue(open.contains("Only the open hypotheses are listed."), open);
		Assertions.assertTrue(rejected.contains("Egidio Verdi") && !rejected.contains("Anna Rossi"), rejected);
	}

	@Test
	void theFilterByKindOfRecordKeepsOnlyThatKind() throws IOException{
		final FLEFModel model = threeHypotheses();

		final String places = text(model, ReportConfig.defaults().with(ReportOption.HYPOTHESIS_RECORD_TYPE, HypothesisRecordType.PLACE));
		final String individuals = text(model, ReportConfig.defaults().with(ReportOption.HYPOTHESIS_RECORD_TYPE, HypothesisRecordType.INDIVIDUAL));

		Assertions.assertTrue(places.contains("No hypothesis has the state or the kind of record chosen for this report."), places);
		Assertions.assertTrue(individuals.contains("Anna Rossi") && individuals.contains("Only the possible duplicate individuals are listed."), individuals);
	}

	@Test
	void theCitationsAreSupportingOrContradicting() throws IOException{
		final FLEFModel model = convert();
		hypothesis(model, "IH1", "I1", "I2", null);
		model.getRecordById("IH1")
			.addChild(FLEFRecord.createChildWithTag(IdentityHypothesisReader.TAG_SOURCE)
				.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, "S1"))
				.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, SourceCitationReader.STANCE_CONTRADICTS)));

		final String text = text(model, ReportConfig.defaults());

		Assertions.assertTrue(text.contains("Supporting evidence:** none"), text);
		Assertions.assertFalse(text.contains("Contradicting evidence:** none"), text);
	}

	@Test
	void aFileWithoutHypothesesSaysSo() throws IOException{
		final String text = text(convert(), ReportConfig.defaults());

		Assertions.assertTrue(text.contains("No identity hypothesis is on file."), text);
	}

}
