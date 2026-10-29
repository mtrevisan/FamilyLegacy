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

import io.github.mtrevisan.familylegacy.ui.dialogs.help.ShortcutRegistry;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;


/**
 * The indicator of the Research Assistant in the main window: how many errors, warnings and notes it has found (the ones the user
 * ignored are not counted), and a click opens the palette. It is the way to know that the assistant has something to say without
 * opening it.
 * <p>
 * It keeps itself up to date without getting in the way:
 * </p>
 * <ul>
 *   <li>the findings are computed away from the event dispatch thread, and only when the model changed: a change is waited for to
 *   settle ({@link #DEBOUNCE_MILLIS} after the last one), and it is not computed while a modal dialog is open (the user may be
 *   editing a record);</li>
 *   <li>one computation at a time, and a change that comes meanwhile asks for another one afterward;</li>
 *   <li>a computation that took long is not repeated at once: the wait before the next one is a multiple of how long it took (at
 *   least {@link #MIN_INTERVAL_MILLIS}), so on a big file the assistant does not keep the processor busy;</li>
 *   <li>ignoring a finding, or showing it again, only recounts the findings already computed.</li>
 * </ul>
 */
public final class AssistantIndicator{

	/** How long after the last change of the model the findings are computed again. */
	static final int DEBOUNCE_MILLIS = 2_000;
	/** The least time between the end of a computation and the start of the next one. */
	static final int MIN_INTERVAL_MILLIS = 5_000;
	/** How long to wait to try again when a modal dialog is open. */
	static final int BUSY_RETRY_MILLIS = 2_000;
	/** The wait before the next computation, as a multiple of the time the last one took. */
	static final int BACKOFF_FACTOR = 4;

	private static final String CHECKING = "…";

	private final ResearchAssistantService service;
	private final Supplier<AssistantContext> contextSupplier;
	private final BooleanSupplier busy;
	private final Executor executor;
	/** Runs something on the event dispatch thread (the tests run it at once). */
	private final Consumer<Runnable> onEventThread;
	private final LongSupplier clock;

	private final JButton button = new JButton();
	private final Timer timer;

	/** The findings, as computed the last time (and {@code null} until the first computation ends). */
	private List<Diagnostic> findings;
	private boolean running;
	/** Whether the model changed after the computation that is running started. */
	private boolean dirty;
	/** The earliest time (in milliseconds of the clock) the next computation may start. */
	private long nextAllowed;


	/**
	 * @param service         the Research Assistant
	 * @param contextSupplier where the user is: the window, the model and the file being edited (called on the event dispatch thread)
	 * @param busy            whether the user is busy in a modal dialog, so that the findings are not computed now
	 */
	public AssistantIndicator(final ResearchAssistantService service, final Supplier<AssistantContext> contextSupplier,
			final BooleanSupplier busy){
		this(service, contextSupplier, busy, newExecutor(), SwingUtilities::invokeLater, System::currentTimeMillis);
	}

	AssistantIndicator(final ResearchAssistantService service, final Supplier<AssistantContext> contextSupplier,
			final BooleanSupplier busy, final Executor executor, final Consumer<Runnable> onEventThread, final LongSupplier clock){
		this.service = service;
		this.contextSupplier = contextSupplier;
		this.busy = busy;
		this.executor = executor;
		this.onEventThread = onEventThread;
		this.clock = clock;

		timer = new Timer(DEBOUNCE_MILLIS, e -> onTimer());
		timer.setRepeats(false);

		button.setFocusable(false);
		button.addActionListener(e -> service.open());
		//ignoring a finding does not need the findings again, only to count them again
		service.ignoredFindings().addListener(() -> onEventThread.accept(this::recount));
		update();
	}


	/** @return the component to put in the toolbar */
	public JComponent component(){
		return button;
	}

	/** The model changed: the findings will be computed again when it settles. Call it on the event dispatch thread. */
	public void refreshSoon(){
		dirty = true;
		schedule(delayUntilNext());
	}

	/** @return how long to wait before computing the findings again: the model has to settle, and a long computation is not repeated at once */
	long delayUntilNext(){
		return Math.max(DEBOUNCE_MILLIS, nextAllowed - clock.getAsLong());
	}

	/** Counts again the findings already computed (a finding was ignored, or shown again). */
	void recount(){
		update();
	}

	/** @return the counts of the findings to look at, or {@code null} if they were not computed yet */
	FindingCounts counts(){
		return currentCounts();
	}

	String text(){
		return button.getText();
	}

	String tooltip(){
		return button.getToolTipText();
	}

	/** Starts the computation if it is time (the tests call it instead of waiting for the timer). */
	void onTimer(){
		if(busy.getAsBoolean()){
			schedule(BUSY_RETRY_MILLIS);
			return;
		}
		if(running)
			//when the computation ends it asks for another one, if the model changed
			return;

		final AssistantContext context = contextSupplier.get();
		if(context == null || context.model() == null){
			findings = List.of();
			update();
			return;
		}

		running = true;
		dirty = false;
		update();
		executor.execute(() -> {
			final long start = clock.getAsLong();
			final List<Diagnostic> found = service.diagnose(context);
			final long elapsed = clock.getAsLong() - start;
			onEventThread.accept(() -> finish(found, elapsed));
		});
	}


	private void finish(final List<Diagnostic> found, final long elapsedMillis){
		findings = found;
		running = false;
		nextAllowed = clock.getAsLong() + Math.max(MIN_INTERVAL_MILLIS, BACKOFF_FACTOR * elapsedMillis);
		update();
		if(dirty)
			refreshSoon();
	}

	private void schedule(final long delayMillis){
		timer.setInitialDelay((int)Math.min(Integer.MAX_VALUE, delayMillis));
		timer.restart();
	}

	private FindingCounts currentCounts(){
		if(findings == null)
			return null;

		final AssistantContext context = contextSupplier.get();
		final String scope = (context != null? context.scope(): null);
		return FindingCounts.of(service.ignoredFindings().split(scope, findings).active());
	}

	private void update(){
		final FindingCounts counts = currentCounts();
		final String label = I18N.t("assistant.indicator.label");
		final String shortcut = ShortcutRegistry.TOOLS_RESEARCH_ASSISTANT.displayKeys();
		if(counts == null){
			button.setText(label + StringUtils.SPACE + CHECKING);
			button.setToolTipText(I18N.tf("assistant.indicator.tooltip", shortcut, I18N.t("assistant.indicator.checking")));
			return;
		}

		button.setText(label + StringUtils.SPACE + counts.label() + (running? StringUtils.SPACE + CHECKING: StringUtils.EMPTY));
		button.setToolTipText(I18N.tf("assistant.indicator.tooltip", shortcut,
			counts.summary() + (running? " (" + I18N.t("assistant.indicator.checking") + ")": StringUtils.EMPTY)));
	}

	private static ExecutorService newExecutor(){
		return Executors.newSingleThreadExecutor(runnable -> {
			final Thread thread = new Thread(runnable, "research-assistant-indicator");
			thread.setDaemon(true);
			thread.setPriority(Thread.MIN_PRIORITY);
			return thread;
		});
	}

}
