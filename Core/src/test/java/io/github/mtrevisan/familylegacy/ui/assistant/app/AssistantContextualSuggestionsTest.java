package io.github.mtrevisan.familylegacy.ui.assistant.app;

import io.github.mtrevisan.familylegacy.preferences.UxLevel;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticAction;
import io.github.mtrevisan.familylegacy.ui.assistant.gap.CreateResearchDraftAction;
import io.github.mtrevisan.familylegacy.ui.assistant.gap.GapDraft;
import io.github.mtrevisan.familylegacy.ui.features.Suggestion;
import io.github.mtrevisan.familylegacy.ui.features.SuggestionKind;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


class AssistantContextualSuggestionsTest{

	private static final AssistantContext CONTEXT = new AssistantContext(null, null, null);

	private static Diagnostic finding(final Diagnostic.Severity severity, final String kind, final String recordId,
			final DiagnosticAction action){
		return new Diagnostic(severity, AssistantCategory.CONCLUSIONS, "title " + kind, "explanation " + kind, List.of(recordId), action,
			kind + "|" + recordId + "|");
	}

	private static Diagnostic finding(final String kind, final String recordId, final DiagnosticAction action){
		return finding(Diagnostic.Severity.WARNING, kind, recordId, action);
	}

	private static DiagnosticAction action(final AtomicInteger runs, final int slot){
		return new DiagnosticAction(){
			@Override
			public String title(){
				return "draft";
			}

			@Override
			public int keySlot(){
				return slot;
			}

			@Override
			public void execute(final AssistantContext context){
				runs.incrementAndGet();
			}
		};
	}

	private static DiagnosticAction action(final AtomicInteger runs){
		return action(runs, 0);
	}

	private static CreateResearchDraftAction gap(final GapDraft.Kind kind){
		return new CreateResearchDraftAction(new GapDraft(kind, "t", "x", List.of(), "normal"));
	}

	private static List<SuggestionKind> kinds(final List<Diagnostic> findings, final UxLevel level){
		return AssistantContextualSuggestions.suggestions(findings, "R1", CONTEXT, level).stream()
			.map(Suggestion::kind)
			.toList();
	}

	@Test
	void findingsAboutTheRecordBecomeSuggestionsAndOnlyTheirActionRuns(){
		final AtomicInteger runs = new AtomicInteger();
		final List<Suggestion> suggestions = AssistantContextualSuggestions.suggestions(List.of(
			finding("conclusion.conflicting_events", "R1", action(runs)),
			finding("identity.possible_duplicate", "OTHER", action(runs)),
			finding("gap.no_death_certain", "R1", gap(GapDraft.Kind.QUESTION))), "R1", CONTEXT, UxLevel.ADVANCED);

		Assertions.assertEquals(List.of(SuggestionKind.CONCLUSION, SuggestionKind.RESEARCH_GAP),
			suggestions.stream().map(Suggestion::kind).toList());
		Assertions.assertEquals(0, runs.get(), "building the suggestions must not run anything");
		suggestions.getFirst().action().run();
		Assertions.assertEquals(1, runs.get());
	}

	@Test
	void aFindingWithoutActionOrOfAnotherKindIsNotSuggested(){
		Assertions.assertTrue(kinds(List.of(finding("conclusion.conflicting_events", "R1", null),
			finding("chronology.child_before_parent", "R1", action(new AtomicInteger()))), UxLevel.ADVANCED).isEmpty());
	}

	@Test
	void onlyTheConflictsThatAConclusionSettlesAreSuggested(){
		final DiagnosticAction run = action(new AtomicInteger());
		Assertions.assertTrue(kinds(List.of(finding("conclusion.own_ancestor", "R1", run),
			finding("conclusion.same_sex_parents", "R1", run)), UxLevel.ADVANCED).isEmpty());
		Assertions.assertEquals(List.of(SuggestionKind.CONCLUSION), kinds(List.of(finding("conclusion.multiple_events", "R1", run)),
			UxLevel.ADVANCED));
		//an open identity hypothesis is not a conflict of assertions: it has its own words
		Assertions.assertEquals(List.of(SuggestionKind.OPEN_HYPOTHESIS), kinds(List.of(finding("conclusion.unresolved_identity", "R1", run)),
			UxLevel.ADVANCED));
	}

	@Test
	void onlyAStrongDuplicateIsSuggested(){
		final DiagnosticAction run = action(new AtomicInteger());
		Assertions.assertTrue(kinds(List.of(finding(Diagnostic.Severity.INFORMATION, "identity.possible_duplicate", "R1", run)),
			UxLevel.STANDARD).isEmpty());
		Assertions.assertEquals(List.of(SuggestionKind.IDENTITY_HYPOTHESIS), kinds(List.of(finding("identity.possible_duplicate", "R1", run)),
			UxLevel.STANDARD));
	}

	@Test
	void aGapSaysWhatItOpensAndTheCommonGapsAreLeftToTheAssistant(){
		Assertions.assertEquals(List.of(SuggestionKind.RESEARCH_TASK),
			kinds(List.of(finding("gap.question_without_activities", "R1", gap(GapDraft.Kind.TASK))), UxLevel.STANDARD));
		Assertions.assertEquals(List.of(SuggestionKind.RESEARCH_GAP),
			kinds(List.of(finding("gap.no_birth", "R1", gap(GapDraft.Kind.QUESTION))), UxLevel.STANDARD));
		Assertions.assertTrue(kinds(List.of(finding("gap.unsourced_vital_event", "R1", gap(GapDraft.Kind.TASK)),
			finding("gap.no_death_probable", "R1", gap(GapDraft.Kind.QUESTION))), UxLevel.STANDARD).isEmpty());
	}

	@Test
	void aBasicUserIsOfferedTheComparisonNotTheHypothesis(){
		final AtomicInteger hypothesis = new AtomicInteger();
		final AtomicInteger comparison = new AtomicInteger();
		final Diagnostic duplicate = finding("identity.possible_duplicate", "R1", action(hypothesis))
			.withAlternatives(List.of(action(comparison, 1)));

		AssistantContextualSuggestions.suggestions(List.of(duplicate), "R1", CONTEXT, UxLevel.BASIC).getFirst().action().run();
		Assertions.assertEquals(0, hypothesis.get());
		Assertions.assertEquals(1, comparison.get());

		AssistantContextualSuggestions.suggestions(List.of(duplicate), "R1", CONTEXT, UxLevel.STANDARD).getFirst().action().run();
		Assertions.assertEquals(1, hypothesis.get());
	}

	@Test
	void theWordsFollowTheLevelAndABasicUserIsShownOneAtATime(){
		final List<Diagnostic> findings = List.of(
			finding("identity.possible_duplicate", "R1", action(new AtomicInteger())),
			finding("conclusion.conflicting_events", "R1", action(new AtomicInteger())));

		final List<Suggestion> basic = AssistantContextualSuggestions.suggestions(findings, "R1", CONTEXT, UxLevel.BASIC);
		Assertions.assertEquals(1, basic.size());
		Assertions.assertEquals(SuggestionKind.IDENTITY_HYPOTHESIS.message(UxLevel.BASIC), basic.getFirst().message());
		Assertions.assertNull(basic.getFirst().explanation(), "a basic user is not told FLEF terms");
		Assertions.assertFalse(basic.getFirst().message().contains("Hypothesis"));
		Assertions.assertFalse(SuggestionKind.IDENTITY_HYPOTHESIS.actionLabel(UxLevel.BASIC).contains("Hypothesis"));

		final List<Suggestion> standard = AssistantContextualSuggestions.suggestions(findings, "R1", CONTEXT, UxLevel.STANDARD);
		Assertions.assertEquals(2, standard.size());
		Assertions.assertTrue(SuggestionKind.IDENTITY_HYPOTHESIS.actionLabel(UxLevel.STANDARD).contains("Identity Hypothesis"));
		Assertions.assertNotNull(standard.getFirst().explanation());
	}

	@Test
	void everyKindHasWordsForEveryLevel(){
		for(final SuggestionKind kind : SuggestionKind.values()){
			for(final UxLevel level : UxLevel.values()){
				Assertions.assertFalse(kind.message(level).startsWith("suggestion."), kind + StringUtils.SPACE + level);
				Assertions.assertFalse(kind.actionLabel(level).startsWith("suggestion."), kind + StringUtils.SPACE + level);
			}
			Assertions.assertFalse(kind.hint().startsWith("suggestion."), kind.name());
		}
	}

}
