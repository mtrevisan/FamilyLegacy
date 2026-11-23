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
package io.github.mtrevisan.familylegacy.research;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.research.InferenceInspection.FindingKind;
import io.github.mtrevisan.familylegacy.research.InferenceInspection.Inspection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class InferenceInspectionTest{

	private static FLEFRecord source(final String id, final String title){
		final FLEFRecord source = FLEFRecord.createMainRecord(id, SourceReader.TYPE);
		source.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_TITLE, title));
		return source;
	}

	/** An event that carries one citation: original+primary+direct (weight 4) if {@code strong}, plain (weight 1) otherwise. */
	private static FLEFRecord event(final String id, final String confidence, final FLEFRecord... citations){
		final FLEFRecord event = FLEFRecord.createMainRecord(id, EventReader.TYPE);
		if(confidence != null)
			event.addChild(FLEFRecord.createChildWithTagAndValue("confidence", confidence));
		for(final FLEFRecord citation : citations)
			event.addChild(citation);
		return event;
	}

	private static FLEFRecord citation(final String sourceId, final boolean strong, final boolean contradicts){
		final FLEFRecord citation = FLEFRecord.createChildWithTag(SourceCitationReader.TAG_SOURCE);
		citation.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, sourceId));
		citation.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_LOCATOR, "p. 4"));
		if(contradicts)
			citation.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, SourceCitationReader.STANCE_CONTRADICTS));
		if(strong)
			citation.addChild(FLEFRecord.createChildWithTag(SourceCitationReader.TAG_EVIDENCE)
				.addChild(FLEFRecord.createChildWithTagAndValue(EvidenceQualifiersReader.TAG_SOURCE_TYPE, EvidenceQualifiersReader.ENUM_SOURCE_TYPE_ORIGINAL))
				.addChild(FLEFRecord.createChildWithTagAndValue(EvidenceQualifiersReader.TAG_INFORMATION_TYPE, EvidenceQualifiersReader.INFORMATION_TYPE_PRIMARY))
				.addChild(FLEFRecord.createChildWithTagAndValue(EvidenceQualifiersReader.TAG_EVIDENCE_TYPE, EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_DIRECT)));
		return citation;
	}

	private static FLEFRecord conclusion(final String status, final String narrative, final String preferred, final String question,
			final String... resolves){
		final FLEFRecord conclusion = FLEFRecord.createMainRecord("CC1", ConclusionReader.TYPE);
		conclusion.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_ISSUE, "birth_date"));
		if(status != null)
			conclusion.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_PROOF_STATUS, status));
		if(narrative != null)
			conclusion.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_NARRATIVE, narrative));
		for(final String id : resolves)
			conclusion.addChild(FLEFRecord.createChildWithTag(ConclusionReader.TAG_RESOLVES)
				.addChild(FLEFRecord.createChildWithTagAndValue("event", id)));
		if(preferred != null)
			conclusion.addChild(FLEFRecord.createChildWithTag(ConclusionReader.TAG_PREFERRED)
				.addChild(FLEFRecord.createChildWithTagAndValue("event", preferred)));
		if(question != null)
			conclusion.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_RESEARCH, question));
		return conclusion;
	}

	private static FLEFModel model(final FLEFRecord conclusion){
		final FLEFModel model = new FLEFModel();
		model.addRecord(source("S1", "Baptism register"));
		model.addRecord(source("S2", "Census"));
		final FLEFRecord question = FLEFRecord.createMainRecord("RQ1", ResearchQuestionReader.TYPE);
		question.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_TITLE, "When was John born?"));
		model.addRecord(question);
		model.addRecord(event("E1", "high", citation("S1", true, false)));
		model.addRecord(event("E2", "low", citation("S2", false, false), citation("S2", false, true)));
		model.addRecord(conclusion);
		return model;
	}

	private static List<FindingKind> kinds(final Inspection inspection){
		return inspection.findings().stream()
			.map(InferenceInspection.Finding::kind)
			.toList();
	}


	@Test
	void theEvidenceTheAlternativesAndTheConflictsAreRead(){
		final FLEFRecord conclusion = conclusion(ConclusionReader.ENUM_PROOF_STATUS_PROVEN, "The register is the original.", "E1", "RQ1", "E1", "E2");
		final Inspection inspection = InferenceInspection.inspect(model(conclusion), conclusion);

		Assertions.assertEquals("birth_date", inspection.issue());
		Assertions.assertEquals(List.of("When was John born?"), inspection.questions());
		Assertions.assertEquals("high", inspection.confidence());
		Assertions.assertEquals("E1", inspection.preferred().id());
		Assertions.assertEquals(4, inspection.preferred().weight());
		Assertions.assertEquals("Baptism register", inspection.preferred().citations().getFirst().source());
		Assertions.assertEquals("p. 4", inspection.preferred().citations().getFirst().locator());
		Assertions.assertEquals(List.of("E2"), inspection.alternatives().stream().map(InferenceInspection.Assertion::id).toList());
		//a supporting citation (+1) and a contradicting one (-1)
		Assertions.assertEquals(0, inspection.alternatives().getFirst().weight());
		Assertions.assertEquals(1, inspection.alternatives().getFirst().opposing().size());
		Assertions.assertEquals(List.of(FindingKind.RIVAL_SUPPORTED), kinds(inspection));
	}

	@Test
	void aWellFoundedConclusionHasNoCheckAgainstIt(){
		final FLEFRecord conclusion = conclusion(ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED, "Because.", "E1", "RQ1", "E1", "E3");
		final FLEFModel model = model(conclusion);
		model.addRecord(event("E3", null));

		Assertions.assertEquals(List.of(), kinds(InferenceInspection.inspect(model, conclusion)));
	}

	@Test
	void theReasoningThatDoesNotHoldIsFlagged(){
		//E2 is preferred but it is contradicted and outweighed by E1, there is no question and no narrative, and "proven" has no direct original source
		final FLEFRecord conclusion = conclusion(ConclusionReader.ENUM_PROOF_STATUS_PROVEN, null, "E2", null, "E1", "E2");
		final Inspection inspection = InferenceInspection.inspect(model(conclusion), conclusion);

		final List<FindingKind> kinds = kinds(inspection);
		Assertions.assertTrue(kinds.containsAll(List.of(FindingKind.NO_QUESTION, FindingKind.NO_NARRATIVE, FindingKind.PREFERRED_CONTRADICTED,
			FindingKind.PREFERRED_OUTWEIGHED, FindingKind.RIVAL_SUPPORTED, FindingKind.PROVEN_WITHOUT_DIRECT_ORIGINAL)), kinds.toString());
	}

	@Test
	void aStatusWithoutAPreferredAssertionAndMissingTargetsAreFlagged(){
		final FLEFRecord conclusion = conclusion(ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED, "Because.", null, "RQ1", "E1", "E404");
		final Inspection inspection = InferenceInspection.inspect(model(conclusion), conclusion);

		Assertions.assertNull(inspection.preferred());
		Assertions.assertNull(inspection.confidence());
		Assertions.assertTrue(kinds(inspection).containsAll(List.of(FindingKind.STATUS_WITHOUT_PREFERRED, FindingKind.UNRESOLVED_TARGET)));
		Assertions.assertFalse(inspection.assertions().getLast().known());
	}

	@Test
	void theTextAnswersWhyHowAndOnWhichEvidence(){
		final FLEFRecord conclusion = conclusion(ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED, "The register is the original.", "E1", "RQ1", "E1", "E2");
		final String text = InferenceInspectionText.render(InferenceInspection.inspect(model(conclusion), conclusion));

		Assertions.assertTrue(text.contains("When was John born?"), text);
		Assertions.assertTrue(text.contains("Baptism register, p. 4"), text);
		Assertions.assertTrue(text.contains("The register is the original."), text);
		Assertions.assertTrue(text.contains("[+4]"), text);
		Assertions.assertTrue(text.contains("[-1]"), text);
		Assertions.assertFalse(text.contains("text.inference"), text);
	}

}
