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

import io.github.mtrevisan.familylegacy.assistant.AssistantAction;
import io.github.mtrevisan.familylegacy.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.assistant.AssistantContributor;
import io.github.mtrevisan.familylegacy.assistant.AssistantSettings;
import io.github.mtrevisan.familylegacy.assistant.AssistantStatistics;
import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.assistant.DiagnosticKeys;
import io.github.mtrevisan.familylegacy.assistant.DiagnosticProvider;
import io.github.mtrevisan.familylegacy.assistant.FuzzyMatcher;
import io.github.mtrevisan.familylegacy.assistant.IgnoredFindings;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.PrivacyPolicy;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.search.SearchMatcher;
import io.github.mtrevisan.familylegacy.search.SearchMode;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.helpers.ShortcutRegistry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;


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
	private final List<Registered> diagnosticProviders = new CopyOnWriteArrayList<>();

	private final IgnoredFindings ignoredFindings;
	private final AssistantSettings settings;
	private final AssistantStatistics statistics;
	/** How long each assistant took the last time (see {@link #lastDurationsMillis()}); by identifier. */
	private final Map<String, Long> durations = new ConcurrentHashMap<>();
	/** From how long a computation of all the assistants is written in the log, even if the log is not in debug. */
	private static final long SLOW_MILLIS = 2_000L;
	/** Shows a record in the main window (installed by the window; {@code null} if it cannot). */
	private volatile Consumer<String> recordViewer;
	/** The last computation made on a copy of the model (see {@link #diagnose(AssistantContext, DoubleConsumer)}); guarded by this. */
	private Computation computation;

	/** The base of the score of a text that matches by words: far above the ones of the abbreviations, that come after. */
	private static final int WORD_MATCH = 100_000;

	private Supplier<FLEFModel> modelSupplier = () -> null;
	private Supplier<FLEFModel> snapshotSupplier;
	private Supplier<String> scopeSupplier = () -> null;
	private Supplier<String> selectionSupplier = () -> null;
	private boolean installed;


	/** A service that keeps the ignored findings in memory only: the tests must not write the preferences of the user. */
	ResearchAssistantService(){
		this(IgnoredFindings.inMemory());
	}

	public ResearchAssistantService(final IgnoredFindings ignoredFindings){
		this(ignoredFindings, AssistantSettings.inMemory());
	}

	public ResearchAssistantService(final IgnoredFindings ignoredFindings, final AssistantSettings settings){
		this(ignoredFindings, settings, AssistantStatistics.inMemory(settings::isStatisticsEnabled, settings::tuning));
	}

	public ResearchAssistantService(final IgnoredFindings ignoredFindings, final AssistantSettings settings,
			final AssistantStatistics statistics){
		this.ignoredFindings = ignoredFindings;
		this.settings = settings;
		this.statistics = statistics;
	}

	/** @return the counts of what the user does with the findings (collected only if the user turned it on) */
	public AssistantStatistics statistics(){
		return statistics;
	}

	/** @return the findings the user ignored */
	public IgnoredFindings ignoredFindings(){
		return ignoredFindings;
	}

	/** @return what the user decided about the assistant itself */
	public AssistantSettings settings(){
		return settings;
	}

	/** An assistant that looks for findings, and one of its sources. */
	private record Registered(AssistantContributor contributor, DiagnosticProvider provider){}

	/** Who is asking for the findings: the palette, or the indicator of the main window (that the user can limit to some assistants). */
	public enum Audience{
		PALETTE,
		INDICATOR
	}

	/** @return the assistants that look for findings (the ones the user can switch off), in the order they were registered */
	public List<AssistantContributor> assistantsWithFindings(){
		return diagnosticProviders.stream()
			.map(Registered::contributor)
			.distinct()
			.toList();
	}

	/** @return the sources of findings that are switched on for the audience */
	private List<Registered> providersFor(final Audience audience){
		return diagnosticProviders.stream()
			.filter(registered -> (audience == Audience.INDICATOR
				? settings.isEnabledForIndicator(registered.contributor().id())
				: settings.isEnabled(registered.contributor().id())))
			.toList();
	}

	/**
	 * Tells the assistant how to show a record in the main window (the window does: it selects the individual in its view).
	 *
	 * @param viewer shows the record whose identifier it is given, on the event dispatch thread; {@code null} if it cannot
	 */
	public void setRecordViewer(final Consumer<String> viewer){
		recordViewer = viewer;
	}

	/**
	 * @param model     the model
	 * @param recordIds the records of a finding
	 * @return the first of them that the main window can show (an individual), or {@code null} if none can, or if the window cannot
	 */
	public String viewableRecord(final FLEFModel model, final List<String> recordIds){
		if(recordViewer == null || model == null)
			return null;

		for(final String id : recordIds)
			if(IndividualReader.isOfType(model.getRecordById(id)))
				return id;
		return null;
	}

	/** Shows the record in the main window, if the window can (call it on the event dispatch thread). */
	public void showRecord(final String recordId){
		final Consumer<String> viewer = recordViewer;
		if(viewer != null && recordId != null)
			viewer.accept(recordId);
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
		for(final DiagnosticProvider provider : contributor.diagnosticProviders())
			diagnosticProviders.add(new Registered(contributor, provider));
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
		return search(query, EnumSet.noneOf(SearchMode.class), context);
	}

	/**
	 * The same, matching as the record selection dialog does: word by word, and narrowed to whole words or widened to typos by the
	 * modes. A query that is not a word of the text can still be an abbreviation of it (hda: Historical Date Assistant) when no mode is
	 * chosen, ranked after the words.
	 *
	 * @param modes the modes of the search
	 */
	public List<RankedAction> search(final String query, final Set<SearchMode> modes, final AssistantContext context){
		final List<RankedAction> result = new ArrayList<>();
		for(final AssistantAction action : actions){
			if(!action.isApplicable(context))
				continue;

			final int score = score(query, modes, action.title(), action.category(), action.description());
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
	 * It can be slow: call it away from the event dispatch thread, with the context given by {@link AssistantContext#forReading()}
	 * (taken on that thread before leaving it): the user may be editing the model meanwhile.
	 * </p>
	 *
	 * @param context where the user is
	 * @return the findings, the most serious first
	 */
	public List<Diagnostic> diagnose(final AssistantContext context){
		return diagnose(context, fraction -> {});
	}

	/**
	 * The same, telling how far it got, so that the user knows when the findings will be there: every provider is an equal share of the
	 * work, and a provider that can tell how far it is (the one of the identity is the slowest) fills its share little by little. The
	 * progress never goes back. It is called from the thread that computes, so a listener that touches Swing has to hand over to the
	 * event dispatch thread.
	 *
	 * @param context where the user is
	 * @param progress told, from time to time, the share of the work done (0 to 1)
	 * @return the findings, the most serious first
	 */
	public List<Diagnostic> diagnose(final AssistantContext context, final DoubleConsumer progress){
		return diagnose(context, progress, Audience.PALETTE);
	}

	/**
	 * The same, for the audience that asks: the palette gets the findings of every assistant the user did not switch off, the indicator
	 * only of the ones the user did not exclude from it either.
	 *
	 * @param audience who asks for the findings
	 */
	public List<Diagnostic> diagnose(final AssistantContext context, final DoubleConsumer progress, final Audience audience){
		final List<Registered> providers = providersFor(audience);
		//what is computed on a copy of the model that nobody changes is true as long as the copy is the same one: the indicator of the
		//window and the palette ask for the findings of the same copy (the history gives the same one until the model changes), so the
		//palette does not compute them again, and if the indicator is still computing it waits for it instead of starting over (only if
		//they ask for the same assistants: the user can limit the indicator to some of them)
		if(!context.isReadingCopy())
			return compute(context, providers, progress);

		final Computation mine;
		final boolean owner;
		synchronized(this){
			owner = (computation == null || computation.model != context.model() || !computation.providers.equals(providers));
			if(owner)
				computation = new Computation(context.model(), providers);
			mine = computation;
		}
		mine.listeners.add(progress);
		if(!owner){
			progress.accept(mine.progress);
			try{
				return mine.future.join();
			}
			catch(final CompletionException ce){
				//the one that was computing failed: this one computes for itself
				return compute(context, providers, progress);
			}
		}

		try{
			final List<Diagnostic> result = List.copyOf(compute(context, providers, fraction -> {
				mine.progress = fraction;
				for(final DoubleConsumer listener : mine.listeners)
					listener.accept(fraction);
			}));
			mine.future.complete(result);
			return result;
		}
		catch(final RuntimeException | Error e){
			synchronized(this){
				if(computation == mine)
					computation = null;
			}
			mine.future.completeExceptionally(e);
			throw e;
		}
	}

	/** The findings being computed (or just computed) on a copy of the model, for whoever asks for the same copy. */
	private static final class Computation{

		private final FLEFModel model;
		/** The sources of findings it asked: another set is another computation. */
		private final List<Registered> providers;
		private final CompletableFuture<List<Diagnostic>> future = new CompletableFuture<>();
		private final List<DoubleConsumer> listeners = new CopyOnWriteArrayList<>();
		private volatile double progress;


		private Computation(final FLEFModel model, final List<Registered> providers){
			this.model = model;
			this.providers = providers;
		}

	}

	private List<Diagnostic> compute(final AssistantContext context, final List<Registered> providers, final DoubleConsumer progress){
		final List<Diagnostic> result = new ArrayList<>();
		final Map<String, Long> spent = new LinkedHashMap<>();
		final int count = providers.size();
		int done = 0;
		progress.accept(0.);
		for(final Registered registered : providers){
			final int index = done;
			final long start = System.nanoTime();
			try{
				final List<Diagnostic> kept = keptFindings(registered.provider(), context);
				if(kept != null)
					result.addAll(kept);
				else{
					final List<Diagnostic> computed = registered.provider().diagnose(context,
						fraction -> progress.accept((index + Math.max(0., Math.min(1., fraction))) / count));
					result.addAll(computed);
					keep(registered.provider(), context, computed);
				}
			}
			catch(final RuntimeException re){
				LOGGER.warn("Diagnostic provider {} failed: {}", registered.provider().getClass().getName(), re.toString());
				LOGGER.debug("Diagnostic provider failure", re);
				keptFindings.remove(registered.provider());
				//a broken assistant must not be mistaken for an assistant with nothing to say: the user is told, and the log has the cause
				result.add(failure(registered.contributor()));
			}
			spent.merge(registered.contributor().id(), (System.nanoTime() - start) / 1_000_000L, Long::sum);
			done ++;
			progress.accept((double)done / count);
		}
		recordDurations(spent);
		//the most serious first; among the same severity, the ones of the same category and kind together; among the same kind, the ones that matter
		//more (e.g. the more likely duplicates), then by title
		result.sort(Comparator.comparing(Diagnostic::severity).thenComparing(Diagnostic::category).thenComparing(Diagnostic::kind)
			.thenComparing(Diagnostic::rank, Comparator.reverseOrder())
			.thenComparing(Diagnostic::title));
		return result;
	}

	/** What a provider found the last time, and what it was found from (see {@link DiagnosticProvider#dependsOn()}). */
	private record Kept(FLEFModel model, Object environment, LocalDate day, List<Diagnostic> findings){}

	/** The findings the providers that say what they depend on gave the last time, on a copy of the model that nobody changes. */
	private final Map<DiagnosticProvider, Kept> keptFindings = new ConcurrentHashMap<>();

	/** @return whether the findings of a provider can be kept and given again: the model is a copy that nobody changes, and what it
	 * says it depends on is only the records */
	private static boolean isKeepable(final DiagnosticProvider provider, final AssistantContext context){
		//what is hidden by the privacy depends on the relatives of a record, not only on the kinds a provider names
		return (context.isReadingCopy() && provider.dependsOn() != null && PrivacyService.policy() == PrivacyPolicy.SHOW_ALL);
	}

	/** @return the findings of the last time if none of the records they depend on changed since, otherwise {@code null} */
	private List<Diagnostic> keptFindings(final DiagnosticProvider provider, final AssistantContext context){
		if(!isKeepable(provider, context))
			return null;

		final Kept kept = keptFindings.get(provider);
		if(kept == null || !Objects.equals(kept.environment(), provider.environment()) || !LocalDate.now().equals(kept.day())
				|| !sameRecords(kept.model(), context.model(), provider.dependsOn()))
			return null;

		return kept.findings();
	}

	private void keep(final DiagnosticProvider provider, final AssistantContext context, final List<Diagnostic> findings){
		if(isKeepable(provider, context))
			keptFindings.put(provider, new Kept(context.model(), provider.environment(), LocalDate.now(), List.copyOf(findings)));
		else
			keptFindings.remove(provider);
	}

	/**
	 * @return whether the records of the kinds are the very same ones in the two copies of the model: the copies that the history gives
	 * share the records that did not change, and nobody changes them, so the same record is the same content (the other way round is
	 * not true: the same content may be another record, and then the findings are computed again)
	 */
	static boolean sameRecords(final FLEFModel before, final FLEFModel after, final Set<String> kinds){
		if(before == after)
			return true;

		for(final String kind : kinds){
			final List<FLEFRecord> previous = before.getRecordsByType(kind);
			final List<FLEFRecord> current = after.getRecordsByType(kind);
			if(previous.size() != current.size())
				return false;

			for(int i = 0, size = previous.size(); i < size; i ++)
				if(previous.get(i) != current.get(i))
					return false;
		}
		return true;
	}

	/**
	 * @return how long each assistant took, in milliseconds, the last time it was asked for its findings (by the identifier of the
	 * assistant): it tells which one is slow on a big file. An assistant that was not asked since the start has no entry.
	 */
	public Map<String, Long> lastDurationsMillis(){
		return Map.copyOf(durations);
	}

	private void recordDurations(final Map<String, Long> spent){
		durations.putAll(spent);
		if(LOGGER.isDebugEnabled())
			LOGGER.debug("Time of the assistants (ms): {}", spent);
		final long total = spent.values().stream().mapToLong(Long::longValue).sum();
		//a computation that is long is worth knowing about, with the assistant that took the most
		if(total >= SLOW_MILLIS)
			LOGGER.info("The assistants took {} ms: {}", total, spent);
	}

	/** The kind of the finding that says that an assistant failed. */
	static final String KIND_FAILED = "assistant.provider_failed";

	/** @return the finding that tells the user an assistant could not check the model (the cause is in the log) */
	private static Diagnostic failure(final AssistantContributor contributor){
		return new Diagnostic(Diagnostic.Severity.WARNING, AssistantCategory.DIAGNOSTICS,
			I18N.tf("assistant.provider_failed.title", contributor.name()),
			I18N.tf("assistant.provider_failed.explanation", contributor.name()), List.of(), null,
			DiagnosticKeys.of(KIND_FAILED, List.of(), contributor.id()));
	}

	/** @return the diagnostics that match a query (all of them if the query is empty) */
	public static List<Diagnostic> filter(final String query, final List<Diagnostic> diagnostics){
		return filter(query, EnumSet.noneOf(SearchMode.class), diagnostics);
	}

	/**
	 * @param findings   the findings
	 * @param recordId   the record the user selected
	 * @param onlyAbout  whether to keep only the findings that are about the record
	 * @return	The findings about the record first, in their order, then (unless only those are wanted) the others in theirs.
	 */
	public static List<Diagnostic> aboutRecord(final List<Diagnostic> findings, final String recordId, final boolean onlyAbout){
		final List<Diagnostic> about = findings.stream()
			.filter(d -> d.recordIds().contains(recordId))
			.toList();
		if(onlyAbout)
			return about;

		final List<Diagnostic> ordered = new ArrayList<>(about);
		findings.stream()
			.filter(d -> !d.recordIds().contains(recordId))
			.forEach(ordered::add);
		return ordered;
	}

	/** The same, with the modes of the search (see {@link #search(String, Set, AssistantContext)}). */
	public static List<Diagnostic> filter(final String query, final Set<SearchMode> modes, final List<Diagnostic> diagnostics){
		return diagnostics.stream()
			.filter(d -> score(query, modes, d.title(), d.category(), d.explanation()) != FuzzyMatcher.NO_MATCH)
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
	public void install(final Supplier<FLEFModel> modelSupplier, final Supplier<String> scopeSupplier){
		install(modelSupplier, null, scopeSupplier);
	}

	/**
	 * As {@link #install(Supplier, Supplier)}, with the copy of the model the findings are computed on.
	 *
	 * @param snapshotSupplier gives a copy of the model as it is now that nobody changes (called on the event dispatch thread, see
	 *                         {@link AssistantContext#forReading()}), or {@code null} to compute the findings on the model itself
	 */
	public void install(final Supplier<FLEFModel> modelSupplier, final Supplier<FLEFModel> snapshotSupplier,
			final Supplier<String> scopeSupplier){
		install(modelSupplier, snapshotSupplier, scopeSupplier, null);
	}

	/**
	 * As {@link #install(Supplier, Supplier, Supplier)}, with what tells which record the user has selected in the main window.
	 *
	 * @param selectionSupplier gives the identifier of the selected record (called on the event dispatch thread), or {@code null} if
	 *                          nothing is selected
	 */
	public synchronized void install(final Supplier<FLEFModel> modelSupplier, final Supplier<FLEFModel> snapshotSupplier,
			final Supplier<String> scopeSupplier, final Supplier<String> selectionSupplier){
		this.modelSupplier = modelSupplier;
		this.snapshotSupplier = snapshotSupplier;
		this.scopeSupplier = scopeSupplier;
		this.selectionSupplier = (selectionSupplier != null? selectionSupplier: () -> null);
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

	/**
	 * @param owner the window the suggestions of an editor open their dialogs from
	 * @param model the model the editor shows
	 * @return the context of an editor, to compute findings on a copy of the model and to act on the model of the file, or {@code null}
	 * 	if the assistant is not installed (nothing is computed on a model that the user may be editing)
	 */
	public synchronized AssistantContext contextFor(final Window owner, final FLEFModel model){
		if(snapshotSupplier == null || model == null)
			return null;

		return new SwingAssistantContext(owner, null, model, scopeSupplier.get(), snapshotSupplier, null);
	}

	/** Opens the palette on what the user is doing now. */
	public void open(){
		final KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		final Component focusOwner = focusManager.getFocusOwner();
		final Window window = (focusManager.getActiveWindow() != null
			? focusManager.getActiveWindow()
			: (focusOwner != null? SwingUtilities.getWindowAncestor(focusOwner): null));
		//only a window of the main window has a selection: from an editor the selection is not what the user is working on...
		final boolean inEditor = (window instanceof Dialog);
		//...but the record the editor shows is what the user is working on: the findings are about it
		final String selected = (window instanceof RecordDialog editor && editor.getRecord() != null
			? editor.getRecord().getId()
			: (inEditor? null: selectionSupplier.get()));
		open(new SwingAssistantContext(window, focusOwner, modelSupplier.get(), scopeSupplier.get(), snapshotSupplier, selected));
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
		return recalculate(context, before, findings -> findings);
	}

	/** As {@link #recalculate(AssistantContext, List)}, listing only the findings that {@code listed} keeps (the ones the user sees). */
	private Recalculation recalculate(final AssistantContext context, final List<Diagnostic> before,
			final UnaryOperator<List<Diagnostic>> listed){
		final List<Diagnostic> after = listed.apply(diagnose(context));
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
		//the user may not want the palette to come back (see the settings)
		if(before == null || !settings.isReopenAfterAction())
			return;

		//the copy of the model is taken now, on the event dispatch thread and after the edit, and read away from it
		final AssistantContext reading = context.forReading();
		CompletableFuture
			.supplyAsync(() -> recalculate(reading, before, FindingLevels::visible))
			.exceptionally(e -> {
				LOGGER.warn("Cannot recalculate the findings: {}", e.toString());
				return null;
			})
			.thenAccept(recalculation -> {
				if(recalculation == null)
					return;

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


	private static int score(final String query, final Set<SearchMode> modes, final String title, final AssistantCategory category,
			final String description){
		//the words of the query can be spread over the title, the category and the description
		final int words = best(wordScore(query, modes, title), wordScore(query, modes, category.title()),
			wordScore(query, modes, description), wordScore(query, modes, title + StringUtils.SPACE + category.title() + StringUtils.SPACE
				+ description));
		if(words != FuzzyMatcher.NO_MATCH || !modes.isEmpty())
			return words;

		//not a word of the text: the characters in order, as a command palette does (hda -> Historical Date Assistant); not with the
		//modes, that ask for a stricter or a more tolerant match of the words
		return best(FuzzyMatcher.score(query, title), FuzzyMatcher.score(query, category.title()), FuzzyMatcher.score(query, description),
			FuzzyMatcher.NO_MATCH);
	}

	/** The best of the scores of the title, the category, the description and the whole text; the title counts the double, the description the half. */
	private static int best(final int title, final int category, final int description, final int all){
		int best = FuzzyMatcher.NO_MATCH;
		if(title != FuzzyMatcher.NO_MATCH)
			best = Math.max(best, 2 * title);
		if(category != FuzzyMatcher.NO_MATCH)
			best = Math.max(best, category);
		if(description != FuzzyMatcher.NO_MATCH)
			best = Math.max(best, Math.max(1, description / 2));
		if(all != FuzzyMatcher.NO_MATCH)
			best = Math.max(best, all);
		return best;
	}

	/**
	 * How well the words of the query match a text, as the record selection dialog does ({@link SearchMatcher}): the more words matched,
	 * the better, then the quality of the match; {@link FuzzyMatcher#NO_MATCH} if none does. A blank query matches with {@code 1}.
	 */
	private static int wordScore(final String query, final Set<SearchMode> modes, final String text){
		final SearchMatcher.MatchScore match = SearchMatcher.score(query, text, modes);
		if(!match.passes())
			return FuzzyMatcher.NO_MATCH;

		return 1 + WORD_MATCH * match.matchedTokens() + (int)Math.round(match.score() * 100.);
	}

}
