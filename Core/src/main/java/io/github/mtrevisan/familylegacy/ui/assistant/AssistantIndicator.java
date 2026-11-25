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

import io.github.mtrevisan.familylegacy.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.assistant.FindingCounts;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.helpers.ShortcutRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
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
 *   <li>ignoring a finding, or showing it again, only recounts the findings already computed;</li>
 *   <li>the user can hide it (then it computes nothing) and limit it to some assistants (see {@link AssistantSettings}); its text is
 *   coloured by the most serious finding, and it blinks for a few seconds when a new error appears.</li>
 * </ul>
 */
public final class AssistantIndicator{

	private static final Logger LOGGER = LoggerFactory.getLogger(AssistantIndicator.class);

	/** How long after the last change of the model the findings are computed again. */
	static final int DEBOUNCE_MILLIS = 2_000;
	/** The least time between the end of a computation and the start of the next one. */
	static final int MIN_INTERVAL_MILLIS = 5_000;
	/** How long to wait to try again when a modal dialog is open. */
	static final int BUSY_RETRY_MILLIS = 2_000;
	/** The wait before the next computation, as a multiple of the time the last one took. */
	static final int BACKOFF_FACTOR = 4;

	private static final String CHECKING = I18N.t("dialog.ellipsis");
	/** The colours of the text of the indicator, for a light and a dark theme. */
	private static final Color ERROR_LIGHT = new Color(0xC6, 0x28, 0x28);
	private static final Color ERROR_DARK = new Color(0xFF, 0x6B, 0x6B);
	private static final Color WARNING_LIGHT = new Color(0xB2, 0x6A, 0x00);
	private static final Color WARNING_DARK = new Color(0xFF, 0xB7, 0x4D);
	/** How long the colour of the blinking stays, and how many times it is switched. */
	static final int BLINK_MILLIS = 450;
	static final int BLINK_CYCLES = 4;

	private final ResearchAssistantService service;
	private final Supplier<AssistantContext> contextSupplier;
	private final BooleanSupplier busy;
	private final Executor executor;
	/** Runs something on the event dispatch thread (the tests run it at once). */
	private final Consumer<Runnable> onEventThread;
	private final LongSupplier clock;

	/** How tall the bar of the progress is, at the bottom of the button. */
	private static final int BAR_HEIGHT = 3;
	/** The progress is told as thousandths: a finer step is not seen. */
	private static final int PROGRESS_STEPS = 1_000;

	private final JButton button = new JButton(){
		@Override
		protected void paintComponent(final Graphics g){
			super.paintComponent(g);
			//a thin bar along the bottom edge tells how far the computation is, while it runs and has something to tell
			if(running && progressSteps > 0){
				final Color color = UIManager.getColor("ProgressBar.foreground");
				g.setColor(color != null? color: severityColor(null));
				g.fillRect(0, getHeight() - BAR_HEIGHT, getWidth() * progressSteps / PROGRESS_STEPS, BAR_HEIGHT);
			}
		}
	};
	private final Timer timer;

	/** The findings, as computed the last time (and {@code null} until the first computation ends). */
	private List<Diagnostic> findings;
	private boolean running;
	/** The words of the button, symbols included. */
	private String shownText = "";
	/** How far the running computation is, in thousandths (event dispatch thread only). */
	private int progressSteps;
	/** Whether the model changed after the computation that is running started. */
	private boolean dirty;
	/** The earliest time (in milliseconds of the clock) the next computation may start. */
	private long nextAllowed;
	/** The errors counted the last time (negative until the first computation ends), to see when a new one appears. */
	private int shownErrors = -1;
	private Timer blinkTimer;
	private int blinkSteps;


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
		//the user may hide the indicator, or limit it to some assistants: in the second case the findings are not the same any more
		service.settings().addListener(() -> onEventThread.accept(this::settingsChanged));
		button.setVisible(service.settings().isIndicatorVisible());
		update();
	}

	/**
	 * The settings of the assistant changed: the indicator is shown or hidden (while it is hidden it computes nothing), and the findings
	 * are computed again, because the assistants it asks may be others.
	 */
	void settingsChanged(){
		final boolean visible = service.settings().isIndicatorVisible();
		button.setVisible(visible);
		if(visible)
			refreshSoon();
	}

	/** @return whether the indicator is shown in the toolbar */
	boolean isVisible(){
		return button.isVisible();
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

	/** @return	What the button says, symbols included (it shows them as images). */
	String text(){
		return shownText;
	}

	String tooltip(){
		return button.getToolTipText();
	}

	/** Starts the computation if it is time (the tests call it instead of waiting for the timer). */
	void onTimer(){
		//a hidden indicator counts nothing: it saves the time of the assistants (the palette computes its findings when it opens)
		if(!service.settings().isIndicatorVisible())
			return;

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
		progressSteps = 0;
		update();
		//a copy of the model as it is now, taken here on the event dispatch thread: the user may go on editing while it is read
		final AssistantContext reading = context.forReading();
		//the progress is handed to the event dispatch thread only when it moved by a step: a computation may tell it thousands of times
		final AtomicInteger told = new AtomicInteger();
		final DoubleConsumer progress = fraction -> {
			final int steps = (int)Math.round(Math.max(0., Math.min(1., fraction)) * PROGRESS_STEPS);
			if(told.getAndSet(steps) != steps)
				onEventThread.accept(() -> showProgress(steps));
		};
		executor.execute(() -> {
			final long start = clock.getAsLong();
			final List<Diagnostic> found;
			try{
				found = FindingLevels.visible(service.diagnose(reading, progress, ResearchAssistantService.Audience.INDICATOR));
			}
			catch(final RuntimeException re){
				//without this the indicator would stay "checking" for ever
				LOGGER.warn("Cannot compute the findings: {}", re.toString());
				LOGGER.debug("Findings failure", re);
				onEventThread.accept(() -> finish(findings != null? findings: List.of(), MIN_INTERVAL_MILLIS));
				return;
			}
			final long elapsed = clock.getAsLong() - start;
			onEventThread.accept(() -> finish(found, elapsed));
		});
	}


	private void finish(final List<Diagnostic> found, final long elapsedMillis){
		findings = found;
		running = false;
		progressSteps = 0;
		nextAllowed = clock.getAsLong() + Math.max(MIN_INTERVAL_MILLIS, BACKOFF_FACTOR * elapsedMillis);
		final int errorsBefore = shownErrors;
		update();
		//an error that was not there before: the user is told, once, not at the first computation (it is not news, it is what the file has)
		if(errorsBefore >= 0 && shownErrors > errorsBefore && service.settings().isBlinkOnNewError() && button.isShowing())
			startBlink();
		if(dirty)
			refreshSoon();
	}

	/** @param steps how far the running computation is, in thousandths */
	private void showProgress(final int steps){
		//a late report of a computation that is over does not draw a bar
		if(!running || steps == progressSteps)
			return;

		progressSteps = steps;
		button.repaint();
	}

	/** @return how far the running computation is, from 0 to 1 (0 when none is running) */
	double progress(){
		return (running? (double)progressSteps / PROGRESS_STEPS: 0.);
	}

	/** @return whether the button is blinking, to tell the user that a new error appeared */
	boolean isBlinking(){
		return (blinkTimer != null && blinkTimer.isRunning());
	}

	/** Makes the button blink for a few seconds (the colour of the error, and back); it stops by itself. */
	void startBlink(){
		stopBlink();
		blinkSteps = 0;
		blinkTimer = new Timer(BLINK_MILLIS, e -> {
			blinkSteps ++;
			if(blinkSteps >= 2 * BLINK_CYCLES)
				stopBlink();
			else
				button.setForeground(blinkSteps % 2 == 1? neutralColor(): severityColor(currentCounts()));
		});
		blinkTimer.start();
		button.setForeground(neutralColor());
	}

	void stopBlink(){
		if(blinkTimer != null){
			blinkTimer.stop();
			blinkTimer = null;
		}
		button.setForeground(severityColor(currentCounts()));
	}

	/**
	 * @return the colour of the text of the indicator: the one of the most serious finding there is (an error is red, a warning is amber;
	 * a note, or nothing, leaves the colour of the buttons), so that it can be seen at a glance
	 */
	static Color severityColor(final FindingCounts counts){
		if(counts == null || (counts.errors() == 0 && counts.warnings() == 0))
			return neutralColor();

		final boolean dark = isDark();
		if(counts.errors() > 0)
			return (dark? ERROR_DARK: ERROR_LIGHT);
		return (dark? WARNING_DARK: WARNING_LIGHT);
	}

	private static Color neutralColor(){
		final Color color = UIManager.getColor("Button.foreground");
		return (color != null? color: Color.BLACK);
	}

	/** @return whether the theme is a dark one (by the background of the buttons) */
	private static boolean isDark(){
		final Color background = UIManager.getColor("Button.background");
		return (background != null && (0.299 * background.getRed() + 0.587 * background.getGreen() + 0.114 * background.getBlue()) < 128.);
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
			shownText = I18N.tf("assistant.indicator.text", label, CHECKING);
			button.setIcon(null);
			button.setText(shownText);
			button.getAccessibleContext().setAccessibleName(shownText);
			button.setToolTipText(I18N.tf("assistant.indicator.tooltip", shortcut, I18N.t("assistant.indicator.checking")));
			return;
		}

		shownErrors = counts.errors();
		//while it blinks the colour is the one of the blinking
		if(!isBlinking())
			button.setForeground(severityColor(counts));
		shownText = (running? I18N.tf("assistant.indicator.text.running", label, counts.label(), CHECKING)
			: I18N.tf("assistant.indicator.text", label, counts.label()));
		//the symbols of the counts are images: a font may not have them
		button.setText(label);
		button.setIcon(new CountsIcon(button, counts, (running? CHECKING: null)));
		button.setHorizontalTextPosition(SwingConstants.LEFT);
		button.setIconTextGap(6);
		button.getAccessibleContext().setAccessibleName(shownText);
		button.setToolTipText(I18N.tf("assistant.indicator.tooltip", shortcut,
			(running? I18N.tf("assistant.indicator.summary.running", counts.summary(), I18N.t("assistant.indicator.checking")): counts.summary())));
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
