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
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.Assessment;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.Gap;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.Kind;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.Readiness;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.Stage;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.StageState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/** The GPS workflow: what is missing before the conclusion of a research question can be considered reasonably proven. */
class GpsWorkflowTest{

	private FLEFModel model;


	@BeforeEach
	void buildModel(){
		model = new FLEFModel();
		for(final String id : List.of("S1", "S2"))
			model.addRecord(FLEFRecord.createMainRecord(id, SourceReader.TYPE)
				.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, "Register " + id))));
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
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

	/** A citation that is located and assessed: an original source with direct evidence. */
	private static FLEFRecord citation(final String sourceId){
		return FLEFRecord.createChildWithTag(SourceCitationReader.TAG_SOURCE)
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, sourceId))
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, SourceCitationReader.STANCE_SUPPORTS))
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_LOCATOR, "page 12"))
			.addChild(FLEFRecord.createChildWithTag(SourceCitationReader.TAG_EVIDENCE)
				.addChild(FLEFRecord.createChildWithTagAndValue(EvidenceQualifiersReader.TAG_SOURCE_TYPE,
					EvidenceQualifiersReader.ENUM_SOURCE_TYPE_ORIGINAL))
				.addChild(FLEFRecord.createChildWithTagAndValue(EvidenceQualifiersReader.TAG_INFORMATION_TYPE,
					EvidenceQualifiersReader.INFORMATION_TYPE_PRIMARY))
				.addChild(FLEFRecord.createChildWithTagAndValue(EvidenceQualifiersReader.TAG_EVIDENCE_TYPE,
					EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_DIRECT)));
	}

	private FLEFRecord addQuestion(final String id, final String... targets){
		final FLEFRecord question = FLEFRecord.createMainRecord(id, ResearchQuestionReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_TITLE, "When was Anna born?"))
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_QUESTION, "When was Anna born?"))
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_STATUS, ResearchQuestionReader.ENUM_STATUS_OPEN));
		for(final String target : targets)
			question.addChild(FLEFRecord.createChildWithTag(ResearchQuestionReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(target.startsWith("I")? "individual": "event", target)));
		model.addRecord(question);
		return question;
	}

	private void addSearch(final String id, final String questionId, final String status){
		model.addRecord(FLEFRecord.createMainRecord(id, ResearchActivityReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchActivityReader.TAG_QUESTION, questionId))
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchActivityReader.TAG_ACTIVITY_TYPE, "search"))
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchActivityReader.TAG_STATUS, status))
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchActivityReader.TAG_ACTION, "Parish register 1850")));
	}

	private FLEFRecord addConclusion(final String id, final String questionId, final String status, final String narrative,
			final String preferredId, final String... resolves){
		final FLEFRecord conclusion = FLEFRecord.createMainRecord(id, ConclusionReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_ISSUE, "birth_date"));
		if(questionId != null)
			conclusion.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_RESEARCH, questionId));
		if(status != null)
			conclusion.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_PROOF_STATUS, status));
		if(narrative != null)
			conclusion.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_NARRATIVE, narrative));
		for(final String target : resolves)
			conclusion.addChild(FLEFRecord.createChildWithTag(ConclusionReader.TAG_RESOLVES)
				.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_RESOLVES, target)));
		if(preferredId != null)
			conclusion.addChild(FLEFRecord.createChildWithTag(ConclusionReader.TAG_PREFERRED)
				.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_PREFERRED, preferredId)));
		model.addRecord(conclusion);
		return conclusion;
	}

	private static Set<Kind> kinds(final Assessment assessment){
		return assessment.gaps().stream()
			.map(Gap::kind)
			.collect(Collectors.toSet());
	}


	@Test
	void aQuestionWithNothingYetNeedsEverythingFromTheStart(){
		final Assessment assessment = GpsWorkflow.assess(model, addQuestion("RS1"));

		Assertions.assertEquals(Set.of(Kind.NO_TARGET, Kind.NO_SEARCH, Kind.NO_CONCLUSION), kinds(assessment));
		Assertions.assertEquals(Readiness.NOT_STARTED, assessment.readiness());
		Assertions.assertEquals(Stage.QUESTION, assessment.currentStage());
		Assertions.assertEquals(StageState.OPEN, assessment.status(Stage.PROOF).state());
		Assertions.assertFalse(assessment.required().isEmpty());
	}

	@Test
	void evidenceThatIsNotCitedOrAssessedIsWhatIsMissingInTheEvidenceStage(){
		addBirth("E1", "P1");
		addBirth("E2", "P2", FLEFRecord.createChildWithTag(SourceCitationReader.TAG_SOURCE)
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, "S1")));
		final FLEFRecord question = addQuestion("RS1", "E1", "E2");
		addSearch("RA1", "RS1", ResearchActivityReader.ENUM_STATUS_COMPLETED);

		final Assessment assessment = GpsWorkflow.assess(model, question);
		final Set<Kind> kinds = kinds(assessment);

		Assertions.assertTrue(kinds.contains(Kind.UNCITED));
		Assertions.assertTrue(kinds.contains(Kind.NO_LOCATOR));
		Assertions.assertTrue(kinds.contains(Kind.UNASSESSED_EVIDENCE));
		Assertions.assertFalse(kinds.contains(Kind.NO_SEARCH));
		Assertions.assertEquals("E1", assessment.gaps().stream()
			.filter(gap -> gap.kind() == Kind.UNCITED)
			.findFirst()
			.orElseThrow()
			.subjectId());
		Assertions.assertEquals(Stage.EVIDENCE, assessment.currentStage());
	}

	@Test
	void twoCompetingBirthsWithNoConclusionAreAnUnresolvedConflictListedOnce(){
		addBirth("E1", "P1", citation("S1"));
		addBirth("E2", "P2", citation("S2"));
		final FLEFRecord question = addQuestion("RS1", "I1");
		addSearch("RA1", "RS1", ResearchActivityReader.ENUM_STATUS_COMPLETED);

		final Assessment assessment = GpsWorkflow.assess(model, question);

		Assertions.assertEquals(1, assessment.gaps().stream()
			.filter(gap -> gap.kind() == Kind.UNRESOLVED_CONFLICT)
			.count());
		Assertions.assertTrue(kinds(assessment).contains(Kind.NO_CONCLUSION));
		Assertions.assertEquals(StageState.OPEN, assessment.status(Stage.CONFLICTS).state());
		Assertions.assertFalse(assessment.isMet(GpsWorkflow.Element.CONFLICT_RESOLUTION));
	}

	@Test
	void aSoundConclusionLeavesNothingRequiredAndTheQuestionReasonablyProven(){
		addBirth("E1", "P1", citation("S1"), citation("S2"));
		addBirth("E2", "P2");
		final FLEFRecord question = addQuestion("RS1", "E1");
		question.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_CONCLUSION, "Born in 1850."));
		addSearch("RA1", "RS1", ResearchActivityReader.ENUM_STATUS_COMPLETED);
		addConclusion("CC1", "RS1", ConclusionReader.ENUM_PROOF_STATUS_PROVEN, "The register is the original and agrees with the baptism.",
			"E1", "E1", "E2");

		final Assessment assessment = GpsWorkflow.assess(model, question);

		Assertions.assertEquals(List.of(), assessment.required(), assessment.gaps().toString());
		Assertions.assertTrue(assessment.readiness() == Readiness.REASONABLY_PROVEN || assessment.readiness() == Readiness.COMPLETE);
		Assertions.assertNull(assessment.currentStage());
		Assertions.assertEquals(1, assessment.conclusions().size());
	}

	@Test
	void aConclusionThatTakesNoPositionAndHasNoNarrativeIsMissingTheReasoning(){
		addBirth("E1", "P1", citation("S1"));
		addBirth("E2", "P2", citation("S2"));
		final FLEFRecord question = addQuestion("RS1", "E1");
		addSearch("RA1", "RS1", ResearchActivityReader.ENUM_STATUS_COMPLETED);
		addConclusion("CC1", "RS1", null, null, null, "E1", "E2");

		final Set<Kind> kinds = kinds(GpsWorkflow.assess(model, question));

		Assertions.assertTrue(kinds.contains(Kind.OPEN_CONCLUSION));
		Assertions.assertTrue(kinds.contains(Kind.NO_NARRATIVE));
		Assertions.assertTrue(kinds.contains(Kind.NO_DECISION));
	}

	@Test
	void aSupportedConclusionIsReasonablyProvenButNotProven(){
		addBirth("E1", "P1", citation("S1"), citation("S2"));
		final FLEFRecord question = addQuestion("RS1", "E1");
		question.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_CONCLUSION, "Born in 1850."));
		addSearch("RA1", "RS1", ResearchActivityReader.ENUM_STATUS_COMPLETED);
		addConclusion("CC1", "RS1", ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED, "Two original sources agree.", "E1", "E1");

		final Assessment assessment = GpsWorkflow.assess(model, question);

		Assertions.assertTrue(kinds(assessment).contains(Kind.NOT_PROVEN));
		Assertions.assertEquals(List.of(), assessment.required(), assessment.gaps().toString());
		Assertions.assertEquals(Readiness.REASONABLY_PROVEN, assessment.readiness());
	}

	@Test
	void aQuestionMarkedResolvedTooSoonIsFlagged(){
		final FLEFRecord question = addQuestion("RS1");
		FLEFRecordHelper.updateChildValue(question, ResearchQuestionReader.TAG_STATUS, ResearchQuestionReader.ENUM_STATUS_RESOLVED);

		Assertions.assertTrue(kinds(GpsWorkflow.assess(model, question)).contains(Kind.RESOLVED_TOO_SOON));
	}

	@Test
	void aConclusionIsAssessedThroughTheQuestionItAnswers(){
		addBirth("E1", "P1", citation("S1"));
		addQuestion("RS1", "E1");
		final FLEFRecord conclusion = addConclusion("CC1", "RS1", ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED, "Clear.", "E1", "E1");
		final FLEFRecord orphan = addConclusion("CC9", null, null, null, null);

		final Assessment assessment = GpsWorkflow.assess(model, conclusion);

		Assertions.assertEquals(1, assessment.questions().size());
		Assertions.assertEquals(List.of("E1"), assessment.targets());
		Assertions.assertTrue(kinds(assessment).contains(Kind.NO_SEARCH));
		Assertions.assertTrue(kinds(GpsWorkflow.assess(model, orphan)).contains(Kind.NO_QUESTION));
	}

	@Test
	void theProofStatementTellsTheQuestionTheEvidenceTheConflictAndTheLimits(){
		addBirth("E1", "P1", citation("S1"));
		addBirth("E2", "P2", citation("S2"));
		final FLEFRecord question = addQuestion("RS1", "I1");
		addSearch("RA1", "RS1", ResearchActivityReader.ENUM_STATUS_COMPLETED);
		addConclusion("CC1", "RS1", ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED, "The first register is earlier.", "E1", "E1", "E2");

		final String statement = GpsProofStatement.render(model, GpsWorkflow.assess(model, question));

		Assertions.assertTrue(statement.contains("When was Anna born?"), statement);
		Assertions.assertTrue(statement.contains("Register S1"), statement);
		Assertions.assertTrue(statement.contains("The first register is earlier."), statement);
		Assertions.assertTrue(statement.contains("Parish register 1850"), statement);
	}

	@Test
	void theMissingListIsWordedForTheUser(){
		final Assessment assessment = GpsWorkflow.assess(model, addQuestion("RS1"));

		final String text = GpsText.missing(assessment);

		Assertions.assertTrue(text.contains("No conclusion has been made"), text);
		for(final Kind kind : Kind.values())
			Assertions.assertFalse(GpsText.gap(new Gap(kind, null, null, 0)).isBlank());
	}

}
