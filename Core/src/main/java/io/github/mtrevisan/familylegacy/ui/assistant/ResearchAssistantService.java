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
package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.dialogs.help.ShortcutRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;


/**
 * The Research Assistant: a context-sensitive command palette, opened from anywhere with Ctrl+Shift+A, that offers the
 * genealogical tools and the diagnostics relevant to what the user is doing.
 * <p>
 * It is a research aid and never an authority: it generates drafts, proposes fixes, creates suggestions and opens dedicated
 * workflows, but all genealogical decisions remain under the control of the researcher. No action or diagnostic may merge
 * records, create conclusions, modify assertions or resolve conflicts automatically.
 * </p>
 * <p>
 * The assistants plug in through {@link AssistantContributor}: they are registered with {@link #register} by the code that assembles the
 * application (the package {@code app}), so that this class does not depend on the assistants and a new assistant never needs
 * a change in this class.
 * </p>
 */
public final class ResearchAssistantService{

	private static final Logger LOGGER = LoggerFactory.getLogger(ResearchAssistantService.class);

	/** An action with how well it matches what the user typed. */
	public record RankedAction(AssistantAction action, int score){}


	private final List<AssistantAction> actions = new CopyOnWriteArrayList<>();
	private final List<DiagnosticProvider> diagnosticProviders = new CopyOnWriteArrayList<>();

	private final IgnoredFindings ignoredFindings;

	private Supplier<FLEFModel> modelSupplier = () -> null;
	private Supplier<String> scopeSupplier = () -> null;
	private boolean installed;


	/** A service that keeps the ignored findings in memory only: the tests must not write the preferences of the user. */
	ResearchAssistantService(){
		this(IgnoredFindings.inMemory());
	}

	public ResearchAssistantService(final IgnoredFindings ignoredFindings){
		this.ignoredFindings = ignoredFindings;
	}

	/** @return the findings the user ignored */
	public IgnoredFindings ignoredFindings(){
		return ignoredFindings;
	}


	/**
	 * Adds the actions and the diagnostics of an assistant.
	 *
	 * @throws IllegalArgumentException if an action has the identifier of one already registered
	 */
	public void register(final AssistantContributor contributor){
		final Set<String> ids = new HashSet<>();
		actions.forEach(a -> ids.add(a.id()));
		for(final AssistantAction action : contributor.actions())
			if(!ids.add(action.id()))
				throw new IllegalArgumentException("Duplicate assistant action: " + action.id());

		actions.addAll(contributor.actions());
		diagnosticProviders.addAll(contributor.diagnosticProviders());
	}

	/**
	 * Lists the actions usable in a context that match a query, the best first.
	 * <p>
	 * An empty query lists all of them, ordered by relevance in the context, then by category and title. Otherwise the order is
	 * by how well the title, the category or the description match, with the relevance as a tie-breaker.
	 * </p>
	 *
	 * @param query   the text typed by the user
	 * @param context where the user is
	 */
	public List<RankedAction> search(final String query, final AssistantContext context){
		final List<RankedAction> result = new ArrayList<>();
		for(final AssistantAction action : actions){
			if(!action.isApplicable(context))
				continue;

			final int score = score(query, action.title(), action.category(), action.description());
			if(score != FuzzyMatcher.NO_MATCH)
				result.add(new RankedAction(action, score));
		}
		final Comparator<RankedAction> byRelevance = Comparator.comparingInt((RankedAction r) -> r.action().relevance(context))
			.reversed();
		final Comparator<RankedAction> byScore = Comparator.comparingInt(RankedAction::score).reversed();
		final Comparator<RankedAction> byName = Comparator.comparing((RankedAction r) -> r.action().category())
			.thenComparing(r -> r.action().title());
		result.sort((query == null || query.isBlank()
			? byRelevance.thenComparing(byName)
			: byScore.thenComparing(byRelevance).thenComparing(byName)));
		return result;
	}

	/**
	 * Collects the diagnostics of every provider. A provider that fails is skipped: a bug in one assistant must not take the
	 * others down.
	 * <p>
	 * It can be slow: call it away from the event dispatch thread.
	 * </p>
	 *
	 * @param context where the user is
	 * @return the findings, the most serious first
	 */
	public List<Diagnostic> diagnose(final AssistantContext context){
		final List<Diagnostic> result = new ArrayList<>();
		for(final DiagnosticProvider provider : diagnosticProviders){
			try{
				result.addAll(provider.diagnose(context));
			}
			catch(final RuntimeException re){
				LOGGER.warn("Diagnostic provider {} failed", provider.getClass().getName(), re);
			}
		}
		//the most serious first; among the same severity, the ones that matter more (e.g. the more likely duplicates), then by title
		result.sort(Comparator.comparing(Diagnostic::severity).thenComparing(Diagnostic::rank, Comparator.reverseOrder())
			.thenComparing(Diagnostic::title));
		return result;
	}

	/** @return the diagnostics that match a query (all of them if the query is empty) */
	public static List<Diagnostic> filter(final String query, final List<Diagnostic> diagnostics){
		return diagnostics.stream()
			.filter(d -> score(query, d.title(), d.category(), d.explanation()) != FuzzyMatcher.NO_MATCH)
			.toList();
	}


	/**
	 * Makes the Research Assistant available from every window of the application with Ctrl+Shift+A. The editors of the dates
	 * live in modal dialogs that have no menu bar, so the shortcut cannot be a menu accelerator: it is handled before any
	 * component sees the key.
	 *
	 * @param modelSupplier gives the model being edited, when the assistant is opened
	 * @param scopeSupplier gives what identifies the file being edited (its path), or {@code null} if it was never saved: the findings
	 *                      the user ignores are kept for each file
	 */
	public synchronized void install(final Supplier<FLEFModel> modelSupplier, final Supplier<String> scopeSupplier){
		this.modelSupplier = modelSupplier;
		this.scopeSupplier = scopeSupplier;
		if(installed)
			return;

		installed = true;
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(event -> {
			if(event.getID() != KeyEvent.KEY_PRESSED
					|| !ShortcutRegistry.TOOLS_RESEARCH_ASSISTANT.keyStroke().equals(KeyStroke.getKeyStrokeForEvent(event)))
				return false;

			SwingUtilities.invokeLater(this::open);
			return true;
		});
	}

	/** Opens the palette on what the user is doing now. */
	public void open(){
		final KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		final Component focusOwner = focusManager.getFocusOwner();
		final Window window = (focusManager.getActiveWindow() != null
			? focusManager.getActiveWindow()
			: (focusOwner != null? SwingUtilities.getWindowAncestor(focusOwner): null));
		open(new AssistantContext(window, focusOwner, modelSupplier.get(), scopeSupplier.get()));
	}

	public void open(final AssistantContext context){
		new ResearchAssistantPalette(this, context).setVisible(true);
	}


	/**
	 * What changed in the findings between two computations, by their {@link #identity(Diagnostic) identity}.
	 *
	 * @param findings the findings now
	 * @param resolved how many of the previous findings are gone (fixed, or resolved by a conclusion)
	 * @param added    how many findings are new
	 */
	public record Recalculation(List<Diagnostic> findings, int resolved, int added){

		/** @return whether the findings are not the same as before */
		public boolean isChanged(){
			return (resolved > 0 || added > 0);
		}

	}

	/**
	 * Computes the findings again and compares them with the previous ones: after an edit the findings that were about it may be
	 * gone, and new ones may have appeared.
	 * <p>
	 * It can be slow (see {@link #diagnose(AssistantContext)}): call it away from the event dispatch thread.
	 * </p>
	 *
	 * @param context where the user is
	 * @param before  the findings the user was looking at
	 */
	public Recalculation recalculate(final AssistantContext context, final List<Diagnostic> before){
		final List<Diagnostic> after = diagnose(context);
		final Set<String> previous = identities(before);
		final Set<String> current = identities(after);
		final int resolved = (int)previous.stream().filter(id -> !current.contains(id)).count();
		final int added = (int)current.stream().filter(id -> !previous.contains(id)).count();
		return new Recalculation(after, resolved, added);
	}

	/**
	 * After the user ran an action on a finding (and finished with the editor it opened), computes the findings again, away from the
	 * event dispatch thread, and if they changed opens the palette again where it was: with the same search, the same view (findings or
	 * ignored ones) and the next finding selected, and a notice of how many are resolved and how many are new. If nothing changed the
	 * user is left to what they were doing.
	 *
	 * @param context where the user was
	 * @param before  the findings the user was looking at
	 * @param state   where the palette was
	 */
	void reopenIfChanged(final AssistantContext context, final List<Diagnostic> before,
			final ResearchAssistantPalette.State state){
		if(before == null)
			return;

		CompletableFuture
			.supplyAsync(() -> recalculate(context, before))
			.thenAccept(recalculation -> {
				if(recalculation.isChanged())
					SwingUtilities.invokeLater(() -> new ResearchAssistantPalette(this, context, recalculation, state).setVisible(true));
			});
	}

	/**
	 * What identifies a finding to tell whether it is the same after the data was edited: its key (kind, records and the data it
	 * reports), or, for a finding without a key, its title and records.
	 */
	static String identity(final Diagnostic diagnostic){
		return (diagnostic.key() != null? diagnostic.key(): diagnostic.title() + "|" + diagnostic.recordIds());
	}

	private static Set<String> identities(final List<Diagnostic> diagnostics){
		final Set<String> identities = new HashSet<>();
		diagnostics.forEach(d -> identities.add(identity(d)));
		return identities;
	}


	private static int score(final String query, final String title, final AssistantCategory category, final String description){
		final int titleScore = FuzzyMatcher.score(query, title);
		final int categoryScore = FuzzyMatcher.score(query, category.title());
		final int descriptionScore = FuzzyMatcher.score(query, description);
		if(titleScore == FuzzyMatcher.NO_MATCH && categoryScore == FuzzyMatcher.NO_MATCH
				&& descriptionScore == FuzzyMatcher.NO_MATCH)
			return FuzzyMatcher.NO_MATCH;

		return Math.max(2 * titleScore, Math.max(categoryScore, descriptionScore / 2));
	}

}
