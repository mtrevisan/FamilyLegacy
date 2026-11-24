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
package io.github.mtrevisan.familylegacy.sources;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


/** Why do we believe this: the chain from an assertion to its sources, the competitors, the conclusions and the verdict. */
class EvidenceTraceTest{

	private FLEFModel model;


	@BeforeEach
	void buildModel(){
		model = new FLEFModel();
		for(final String id : List.of("S1", "S2"))
			model.addRecord(FLEFRecord.createMainRecord(id, SourceReader.TYPE)
				.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, "Register " + id))));
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		//E1 and E2 are two births of I1: E1 is cited by S1 (supports) and S2 (contradicts), E2 by S1 and S2 (both support)
		addBirth("E1", "P1", citation("S1", SourceCitationReader.STANCE_SUPPORTS), citation("S2", SourceCitationReader.STANCE_CONTRADICTS));
		addBirth("E2", "P2", citation("S1", SourceCitationReader.STANCE_SUPPORTS), citation("S2", SourceCitationReader.STANCE_SUPPORTS));
		addBirth("E3", "P3");
	}

	private void addBirth(final String eventId, final String participationId, final FLEFRecord... citations){
		final FLEFRecord event = FLEFRecord.createMainRecord(eventId, EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, EventReader.ENUM_TYPE_BIRTH));
		for(final FLEFRecord citation : citations)
			event.addChild(citation);
		model.addRecord(event);
		model.addRecord(FLEFRecord.createMainRecord(participationId, EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, "I1")))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, eventId)));
	}

	private static FLEFRecord citation(final String sourceId, final String stance){
		return FLEFRecord.createChildWithTag(SourceCitationReader.TAG_SOURCE)
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, sourceId))
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, stance));
	}

	private void addConclusion(final String status, final String preferredId){
		final FLEFRecord conclusion = FLEFRecord.createMainRecord("CC1", ConclusionReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_ISSUE, "birth_date"))
			.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_PROOF_STATUS, status))
			.addChild(FLEFRecord.createChildWithTag(ConclusionReader.TAG_RESOLVES)
				.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_RESOLVES, "E1")))
			.addChild(FLEFRecord.createChildWithTag(ConclusionReader.TAG_RESOLVES)
				.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_RESOLVES, "E2")));
		if(preferredId != null)
			conclusion.addChild(FLEFRecord.createChildWithTag(ConclusionReader.TAG_PREFERRED)
				.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_PREFERRED, preferredId)));
		model.addRecord(conclusion);
	}


	@Test
	void theChainGoesFromTheAssertionToTheSourcesWithTheirStance(){
		final EvidenceTrace trace = EvidenceTrace.of(model, "E1");

		Assertions.assertEquals(2, trace.subject().links().size());
		Assertions.assertEquals("Register S1", trace.subject().links().getFirst().sourceTitle());
		Assertions.assertEquals(1, trace.subject().supporting());
		Assertions.assertEquals(1, trace.subject().contradicting());
		Assertions.assertEquals(0, trace.subject().weight());
	}

	@Test
	void theOtherBirthsOfThePersonCompete(){
		final EvidenceTrace trace = EvidenceTrace.of(model, "E1");

		Assertions.assertEquals(List.of("E2", "E3"), trace.alternatives().stream()
			.map(alternative -> alternative.record().getId())
			.toList());
		//E2 weighs more than E1 and there is no conclusion: E1 leaves the conflict open, E2 is the best supported
		Assertions.assertEquals(EvidenceTrace.Verdict.OPEN, trace.verdict());
		Assertions.assertEquals(EvidenceTrace.Verdict.BEST_SUPPORTED, EvidenceTrace.of(model, "E2").verdict());
	}

	@Test
	void aConclusionThatPrefersOneSettlesTheConflictAndMarksItPreferred(){
		addConclusion(ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED, "E2");

		final EvidenceTrace trace = EvidenceTrace.of(model, "E1");

		Assertions.assertEquals(EvidenceTrace.Verdict.SETTLED, trace.verdict());
		Assertions.assertEquals(1, trace.conclusions().size());
		Assertions.assertTrue(trace.conclusions().getFirst().settles());
		Assertions.assertEquals("E2", trace.conclusions().getFirst().preferredId());
		Assertions.assertFalse(trace.subject().preferred());
		Assertions.assertTrue(trace.alternatives().getFirst().preferred());
	}

	@Test
	void aConclusionWithoutAPositionIsADraft(){
		addConclusion(ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED, null);

		final EvidenceTrace trace = EvidenceTrace.of(model, "E2");

		Assertions.assertFalse(trace.conclusions().getFirst().settles());
		Assertions.assertEquals(EvidenceTrace.Verdict.BEST_SUPPORTED, trace.verdict());
	}

	@Test
	void anUncitedAssertionIsUndocumentedAndTheReasoningSaysIt(){
		final EvidenceTrace trace = EvidenceTrace.of(model, "E3");

		Assertions.assertEquals(EvidenceTrace.Verdict.UNDOCUMENTED, trace.verdict());
		Assertions.assertEquals("uncited", trace.reasoning().getFirst().key());
	}

	@Test
	void anAssertionWithNothingCompetingIsUncontested(){
		model.addRecord(FLEFRecord.createMainRecord("E9", EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, EventReader.ENUM_TYPE_BURIAL))
			.addChild(citation("S1", SourceCitationReader.STANCE_SUPPORTS)));

		final EvidenceTrace trace = EvidenceTrace.of(model, "E9");

		Assertions.assertTrue(trace.alternatives().isEmpty());
		Assertions.assertEquals(EvidenceTrace.Verdict.UNCONTESTED, trace.verdict());
	}

	@Test
	void theTraceOfAnIndividualHasTheirFactsAndASourceHasNone(){
		final List<EvidenceTrace> traces = EvidenceTrace.ofIndividual(model, "I1");

		Assertions.assertEquals(List.of("I1", "E1", "E2", "E3"), traces.stream()
			.map(trace -> trace.subject().record().getId())
			.toList());
		Assertions.assertNull(EvidenceTrace.of(model, "S1"));
		Assertions.assertNull(EvidenceTrace.of(model, "missing"));
	}

}
