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
package io.github.mtrevisan.familylegacy.ui.assistant.app;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.preferences.AppPreferences;
import io.github.mtrevisan.familylegacy.preferences.UxLevel;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticAction;
import io.github.mtrevisan.familylegacy.ui.assistant.ResearchAssistantService;
import io.github.mtrevisan.familylegacy.ui.assistant.context.CreateCulturalNormDraftAction;
import io.github.mtrevisan.familylegacy.ui.assistant.gap.CreateResearchDraftAction;
import io.github.mtrevisan.familylegacy.ui.assistant.gap.GapDraft;
import io.github.mtrevisan.familylegacy.ui.features.ContextualSuggestions;
import io.github.mtrevisan.familylegacy.ui.features.FeatureVisibilityService;
import io.github.mtrevisan.familylegacy.ui.features.Suggestion;
import io.github.mtrevisan.familylegacy.ui.features.SuggestionKind;

import javax.swing.SwingUtilities;
import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


/**
 * The suggestions of the editors, taken from the findings of the Research Assistant: a finding about the record the editor shows, that
 * proposes an advanced concept (a conclusion, an identity hypothesis, a cultural norm, a context impact, a research question), becomes a
 * suggestion with the action of the finding, worded for the level of the user. The findings the user ignored are not suggested, at most
 * one suggestion for each concept is shown, and a basic user is shown one at a time.
 */
public final class AssistantContextualSuggestions implements ContextualSuggestions{

	/** The key of the action that compares two records side by side (see {@code DiagnosticAction#keySlot()}). */
	private static final int COMPARE_SLOT = 1;
	/** The findings about an age that a custom of the time may explain (the minimum age of marriage, the generational interval). */
	private static final Set<String> AGE_FINDINGS = Set.of("chronology.marriage_too_young", "chronology.parent_too_young",
		"chronology.parent_too_old");
	/** The conflicts of the research that a conclusion settles. */
	private static final Set<String> CONFLICTS = Set.of("conclusion.conflicting_events", "conclusion.multiple_events",
		"conclusion.conflicting_attributes", "conclusion.conflicting_relationships", "conclusion.too_many_parents");
	/** The gaps that almost every individual of an imported file has (no source, a death that is merely probable): not worth a hint. */
	private static final Set<String> COMMON_GAPS = Set.of("gap.unsourced_vital_event", "gap.no_death_probable",
		"gap.no_death_probable_estimated", "gap.no_death_probable_relatives");

	/** How many suggestions an editor shows at most: it is a hint, not a list (one for a basic user). */
	static final int MAX_SUGGESTIONS = 2;

	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
		final Thread thread = new Thread(runnable, "contextual-suggestions");
		thread.setDaemon(true);
		return thread;
	});


	@Override
	public void suggest(final Window owner, final FLEFModel model, final String recordId,
			final Consumer<List<Suggestion>> callback){
		if(!AppPreferences.getInstance().suggestionsEnabled())
			return;

		final ResearchAssistantService service = DefaultResearchAssistant.getInstance();
		//the context that acts (on the model of the file) and the one that reads (on a copy nobody changes, taken here on this thread)
		final AssistantContext context = service.contextFor(owner, model);
		if(context == null || recordId == null)
			return;

		final AssistantContext reading = context.forReading();
		final UxLevel level = FeatureVisibilityService.shared().level();
		EXECUTOR.execute(() -> {
			try{
				final List<Diagnostic> findings = service.ignoredFindings()
					.split(context.scope(), service.diagnose(reading))
					.active();
				final List<Suggestion> suggestions = suggestions(findings, recordId, context, level);
				if(!suggestions.isEmpty())
					SwingUtilities.invokeLater(() -> callback.accept(suggestions));
			}
			catch(final RuntimeException ignored){
				//a hint that cannot be computed is a hint that is not shown: the editor is not affected
			}
		});
	}

	/**
	 * @param findings the findings, the most serious first
	 * @param recordId the record the editor shows
	 * @param context  where the user is, to run the actions
	 * @param level    the level of the user
	 * @return the suggestions about the record: the first finding of each concept, at most {@link #MAX_SUGGESTIONS} (one for a basic user)
	 */
	static List<Suggestion> suggestions(final List<Diagnostic> findings, final String recordId, final AssistantContext context,
			final UxLevel level){
		final Map<SuggestionKind, Suggestion> byKind = new LinkedHashMap<>();
		for(final Diagnostic finding : findings){
			final SuggestionKind kind = kindOf(finding);
			if(kind == null || byKind.containsKey(kind) || !finding.recordIds().contains(recordId))
				continue;

			final DiagnosticAction action = actionFor(kind, finding, level);
			byKind.put(kind, Suggestion.forLevel(kind, level, finding.explanation(), () -> action.execute(context),
				() -> DefaultResearchAssistant.getInstance().ignoredFindings().ignore(context.scope(), finding)));
		}
		final int max = (level == UxLevel.BASIC? 1: MAX_SUGGESTIONS);
		return new ArrayList<>(byKind.values()).subList(0, Math.min(max, byKind.size()));
	}

	/**
	 * What the suggestion runs. A basic user is not sent to an identity hypothesis (a FLEF record): the first thing is to look at the two
	 * records side by side, that creates nothing.
	 */
	private static DiagnosticAction actionFor(final SuggestionKind kind, final Diagnostic finding, final UxLevel level){
		if(kind == SuggestionKind.IDENTITY_HYPOTHESIS && level == UxLevel.BASIC)
			return finding.alternatives().stream()
				.filter(alternative -> alternative.keySlot() == COMPARE_SLOT)
				.findFirst()
				.orElse(finding.action());
		if(kind == SuggestionKind.AGE_NORM)
			return new CreateCulturalNormDraftAction();
		return finding.action();
	}

	/**
	 * @return the advanced concept a finding proposes, or {@code null} if it proposes none, has no action to start it, or is too common or
	 * 	too weak to interrupt the editor (it is still in the Research Assistant).
	 */
	static SuggestionKind kindOf(final Diagnostic finding){
		final String kind = finding.kind();
		//the chronology only reports: the draft of the norm is what the suggestion adds
		if(AGE_FINDINGS.contains(kind))
			return SuggestionKind.AGE_NORM;
		if(finding.action() == null)
			return null;

		//only the conflicts that a conclusion can settle: a person that is their own ancestor is a mistake to fix, and parents of the same
		//sex are not a conflict
		if(CONFLICTS.contains(kind))
			return SuggestionKind.CONCLUSION;
		if(kind.equals("conclusion.unresolved_identity"))
			return SuggestionKind.OPEN_HYPOTHESIS;
		//only the strong candidates (the weak ones are in the assistant, where they can be ignored)
		if(kind.equals("identity.possible_duplicate"))
			return (finding.severity() == Diagnostic.Severity.WARNING? SuggestionKind.IDENTITY_HYPOTHESIS: null);
		if(kind.equals("context.norm_estimate"))
			return SuggestionKind.CULTURAL_NORM;
		if(kind.equals("context.candidate"))
			return SuggestionKind.CONTEXT_IMPACT;
		if(kind.equals("place.jurisdiction_changed"))
			return SuggestionKind.JURISDICTION_CHANGE;
		if(kind.equals("context.cluster"))
			return SuggestionKind.HISTORIC_EVENT;
		if(kind.startsWith("gap.") && !COMMON_GAPS.contains(kind) && finding.action() instanceof CreateResearchDraftAction draft)
			return (draft.draft().kind() == GapDraft.Kind.QUESTION? SuggestionKind.RESEARCH_GAP: SuggestionKind.RESEARCH_TASK);
		return null;
	}

}
