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

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.search.SearchMode;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.helpers.SavedFileNotice;
import io.github.mtrevisan.familylegacy.ui.helpers.ShortcutRegistry;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;


/**
 * The palette of the Research Assistant: a search field over two sections, the actions the user can run and the diagnostics
 * about the research. It is driven from the keyboard: type to search (the category names work as filters too), Up and Down to
 * move, Enter to run, Esc to close.
 */
final class ResearchAssistantPalette extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(ResearchAssistantPalette.class);

	private sealed interface Row permits Header, ActionRow, DiagnosticRow{}

	/**
	 * A heading: the ones of the severities and of the kinds of finding can be collapsed (they have an {@code id}), the other ones are
	 * only a title.
	 */
	private record Header(String title, String id, boolean open) implements Row{

		Header(final String title){
			this(title, null, false);
		}

		boolean isCollapsible(){
			return (id != null);
		}

	}

	private record ActionRow(AssistantAction action) implements Row{}

	private record DiagnosticRow(Diagnostic diagnostic) implements Row{}

	/**
	 * Where the palette was, to open it again in the same place after the user acted on a finding.
	 *
	 * @param query             what was typed
	 * @param showIgnored       whether it showed the ignored findings
	 * @param selectedIdentity  the identity of the selected finding (see {@link ResearchAssistantService#identity(Diagnostic)})
	 * @param selectedIndex     the position of the selected row
	 * @param toggled           the headings the user opened or closed (see {@link #isOpen(String)})
	 */
	record State(String query, Set<SearchMode> modes, boolean showIgnored, String selectedIdentity, int selectedIndex,
			Set<String> toggled,
			boolean selectedOnly){}


	/** The width of the text of the detail, in pixels. */
	private static final int DETAIL_WIDTH = 480;
	/** What the border and the insets of the content take from its width. */
	private static final int DETAIL_MARGIN = 34;
	private static final String ACTION_PREVIOUS = "previous";
	private static final String ACTION_NEXT = "next";
	private static final String ACTION_RUN = "run";
	/** The steps of the bar of the progress: finer than a percent, so that a long provider is seen moving. */
	private static final int PROGRESS_STEPS = 1_000;
	private static final String ACTION_RUN_OTHER = "runOther";
	private static final String ACTION_RUN_THIRD = "runThird";
	private static final String ACTION_RUN_FOURTH = "runFourth";
	private static final String ACTION_IGNORE = "ignore";
	private static final String ACTION_TOGGLE_IGNORED = "toggleIgnored";
	private static final String ACTION_WHERE_USED = "whereUsed";
	private static final String ACTION_EXPORT = "export";
	/** The start of the address of a link to a record, in the detail. */
	private static final String RECORD_LINK = "record:";
	private static final String ACTION_CLOSE = "close";


	private final ResearchAssistantService service;
	private final AssistantContext context;

	private final JTextField searchField = new JTextField();
	//the same modes of the record selection dialog (they are not in the tab order: the keyboard stays in the search field)
	private final JCheckBox wholeWordCheckBox = new JCheckBox(I18N.t("search.mode.whole_word"), false);
	private final JCheckBox fuzzyCheckBox = new JCheckBox(I18N.t("search.mode.fuzzy"), false);
	/** Limits the findings to the ones about the record selected in the main window (only there when there is one). */
	private final JCheckBox selectedOnlyCheckBox = new JCheckBox();
	private final DefaultListModel<Row> rowsModel = new DefaultListModel<>();
	private final JList<Row> rowsList = new JList<>(rowsModel);
	/** The tallest detail shown so far: the detail never gets shorter, so that the list does not change size while it is scrolled. */
	private int detailHeight;
	/** The text of the detail (without the markup that wraps it), and the width of the label when it was last wrapped. */
	private String detailBody;
	private int detailLabelWidth;
	/** The detail is selectable, to copy it: it is a read-only HTML pane (a label cannot be selected). */
	private final JEditorPane detailLabel = new JEditorPane("text/html", StringUtils.SPACE){
		@Override
		public void setBounds(final int x, final int y, final int width, final int height){
			super.setBounds(x, y, width, height);
			//the width is what it is now: the text wraps again at it
			if(width != detailLabelWidth)
				SwingUtilities.invokeLater(ResearchAssistantPalette.this::applyDetail);
		}

		@Override
		public Dimension getPreferredSize(){
			//the height of the text at the width the pane has (the pane wraps by itself); the width never drives the window
			final int width = (getWidth() > 0? getWidth(): DETAIL_WIDTH);
			final Dimension current = getSize();
			setSize(width, Short.MAX_VALUE);
			final Dimension size = super.getPreferredSize();
			setSize(current);
			detailHeight = Math.max(detailHeight, size.height);
			return new Dimension(100, detailHeight);
		}
	};
	private final JLabel statusLabel = new JLabel(StringUtils.SPACE);
	/** Shown while the findings are being computed. */
	private final JProgressBar loadingBar = new JProgressBar();

	/** The findings, once computed away from the event dispatch thread (and {@code null} until then). */
	private volatile List<Diagnostic> diagnostics;
	/** Whether the palette shows the findings the user ignored, instead of the ones to look at. */
	private boolean showIgnored;
	/** The headings that are not as they start: a severity starts open, a kind of finding starts collapsed. */
	private final Set<String> toggled = new HashSet<>();
	/** What happened to the findings since the user acted on one (kept until the palette is closed), or empty. */
	private String notice = "";


	ResearchAssistantPalette(final ResearchAssistantService service, final AssistantContext context){
		this(service, context, null, null);
	}

	/**
	 * The palette opened again after the user acted on a finding: the findings are the ones just computed, and the search, the view
	 * and the selection are the ones the user left.
	 *
	 * @param recalculation the findings computed again, with how many are resolved and how many are new ({@code null} if the palette
	 *                      is opened for the first time)
	 * @param state         where the palette was ({@code null} if the palette is opened for the first time)
	 */
	ResearchAssistantPalette(final ResearchAssistantService service, final AssistantContext context,
			final ResearchAssistantService.Recalculation recalculation, final State state){
		super(context.owner(), I18N.t("assistant.title"), ModalityType.MODELESS);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		//clicking outside the palette closes it (as for a popup): the window loses the focus
		addWindowFocusListener(new WindowAdapter(){
			@Override
			public void windowLostFocus(final WindowEvent e){
				if(e.getOppositeWindow() != null && e.getOppositeWindow().getOwner() == ResearchAssistantPalette.this)
					return;

				dispose();
			}
		});

		this.service = service;
		this.context = context;

		initComponents();
		if(recalculation != null){
			diagnostics = recalculation.findings();
			notice = I18N.tf("assistant.recalculated", recalculation.resolved(), recalculation.added());
			showIgnored = state.showIgnored();
			//what the user had opened stays so, and the group of the finding they acted on is open, to show it
			toggled.addAll(state.toggled());
			for(final Diagnostic d : diagnostics)
				if(ResearchAssistantService.identity(d).equals(state.selectedIdentity()) && !isOpen(kindId(d)))
					toggled.add(kindId(d));
			searchField.setText(state.query());
			wholeWordCheckBox.setSelected(state.modes().contains(SearchMode.WHOLE_WORD));
			fuzzyCheckBox.setSelected(state.modes().contains(SearchMode.FUZZY));
			selectedOnlyCheckBox.setSelected(state.selectedOnly());
			refresh();
			selectFinding(state);
		}
		else{
			refresh();
			loadDiagnostics();
		}

		pack();
		setLocationRelativeTo(context.owner());
	}


	private void initComponents(){
		setUndecorated(true);

		searchField.putClientProperty("JTextField.placeholderText", I18N.t("assistant.search.placeholder"));
		searchField.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void insertUpdate(final DocumentEvent e){
				refresh();
			}

			@Override
			public void removeUpdate(final DocumentEvent e){
				refresh();
			}

			@Override
			public void changedUpdate(final DocumentEvent e){
				refresh();
			}
		});
		bindKeys();
		//ESC closes the palette wherever the focus is in it, not only in the search field
		getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW);

		rowsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		rowsList.setFocusable(false);
		rowsList.setVisibleRowCount(12);
		//the list always has the same height, whatever it holds (the detail is what makes the window taller)
		rowsList.setFixedCellHeight(getFontMetrics(rowsList.getFont()).getHeight() + 8);
		detailLabel.setEditable(false);
		//the records of a finding are links that open them
		detailLabel.addHyperlinkListener(event -> {
			//the hand over a link
			if(event.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ENTERED)
				detailLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			else if(event.getEventType() == javax.swing.event.HyperlinkEvent.EventType.EXITED)
				detailLabel.setCursor(Cursor.getDefaultCursor());
			if(event.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED && event.getDescription() != null
					&& event.getDescription().startsWith(RECORD_LINK))
				openRecord(event.getDescription().substring(RECORD_LINK.length()));
		});
		detailLabel.setOpaque(false);
		detailLabel.setBorder(null);
		detailLabel.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		detailLabel.setFont(UIManager.getFont("Label.font"));
		detailLabel.setForeground(UIManager.getColor("Label.foreground"));
		//the text wraps, so the detail does not hold the window wide
		detailLabel.setMinimumSize(new Dimension(100, 0));
		rowsList.setCellRenderer((list, value, index, selected, focus) -> render(list, value, selected));
		rowsList.addListSelectionListener(e -> showDetail());
		rowsList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				//a double click runs what is under it, or opens or closes a heading (a single click only selects it)
				if(e.getClickCount() == 2)
					runSelected(0);
			}
		});

		final JPanel content = new JPanel(new MigLayout("fill,wrap 1,ins 8", "[grow,fill,500::]", "[]3[]5[grow,fill]5[]5[]15[]"));
		content.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")),
			BorderFactory.createEmptyBorder(4, 4, 4, 4)));
		content.add(searchField);
		final JPanel modes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		modes.setOpaque(false);
		for(final JCheckBox mode : List.of(wholeWordCheckBox, fuzzyCheckBox)){
			mode.setFocusable(false);
			mode.addActionListener(e -> refresh());
			modes.add(mode);
		}
		final String selectedId = context.selectedRecord()
			.map(FLEFRecord::getId)
			.orElse(null);
		if(selectedId != null){
			selectedOnlyCheckBox.setText(I18N.tf("assistant.selected_only", RecordDisplay.describe(context.selectedRecord().orElseThrow(),
				context.model())));
			selectedOnlyCheckBox.setFocusable(false);
			//opened from an editor, only the findings about its record (the user can ask for all of them)
			selectedOnlyCheckBox.setSelected(context.window() instanceof RecordDialog);
			selectedOnlyCheckBox.addActionListener(e -> refresh());
			modes.add(selectedOnlyCheckBox);
		}
		content.add(modes);
		content.add(new JScrollPane(rowsList));
		content.add(detailLabel);
		content.add(statusLabel);
		//the bar says how far the computation is, and the status line the percentage: the findings appear when it is full
		loadingBar.setMaximum(PROGRESS_STEPS);
		loadingBar.setIndeterminate(true);
		content.add(loadingBar, "h 8!");
		setContentPane(content);
		//the palette has no title bar: it is moved by dragging it from anywhere that is not the list or the search field
		for(final JComponent draggable : List.of(content, modes, statusLabel, loadingBar))
			makeDraggable(draggable);
	}

	/**
	 * Lets the window be moved by dragging a component, and resized by dragging its border (the {@code content} panel is the border,
	 * the other components only move the window).
	 */
	private void makeDraggable(final JComponent component){
		final boolean border = (component == getContentPane());
		final MouseAdapter drag = new MouseAdapter(){
			private Point pressedAt;
			private int edges;

			private int edgesAt(final MouseEvent e){
				if(!border)
					return 0;

				final int margin = 8;
				final Point p = e.getPoint();
				final int w = component.getWidth();
				final int h = component.getHeight();
				return (p.x < margin? 1: 0) | (p.x >= w - margin? 2: 0) | (p.y < margin? 4: 0) | (p.y >= h - margin? 8: 0);
			}

			@Override
			public void mouseMoved(final MouseEvent e){
				final int at = edgesAt(e);
				component.setCursor(Cursor.getPredefinedCursor(switch(at){
					case 1 -> Cursor.W_RESIZE_CURSOR;
					case 2 -> Cursor.E_RESIZE_CURSOR;
					case 4 -> Cursor.N_RESIZE_CURSOR;
					case 8 -> Cursor.S_RESIZE_CURSOR;
					case 5 -> Cursor.NW_RESIZE_CURSOR;
					case 6 -> Cursor.NE_RESIZE_CURSOR;
					case 9 -> Cursor.SW_RESIZE_CURSOR;
					case 10 -> Cursor.SE_RESIZE_CURSOR;
					default -> Cursor.DEFAULT_CURSOR;
				}));
			}

			@Override
			public void mousePressed(final MouseEvent e){
				pressedAt = e.getLocationOnScreen();
				edges = edgesAt(e);
			}

			@Override
			public void mouseDragged(final MouseEvent e){
				if(pressedAt == null)
					return;

				final Point now = e.getLocationOnScreen();
				final int dx = now.x - pressedAt.x;
				final int dy = now.y - pressedAt.y;
				if(edges == 0)
					setLocation(getX() + dx, getY() + dy);
				else{
					final Dimension min = getMinimumSize();
					int x = getX();
					int y = getY();
					int width = getWidth();
					int height = getHeight();
					if((edges & 1) != 0){
						final int newWidth = Math.max(min.width, width - dx);
						x += width - newWidth;
						width = newWidth;
					}
					if((edges & 2) != 0)
						width = Math.max(min.width, width + dx);
					if((edges & 4) != 0){
						final int newHeight = Math.max(min.height, height - dy);
						y += height - newHeight;
						height = newHeight;
					}
					if((edges & 8) != 0)
						height = Math.max(min.height, height + dy);
					setBounds(x, y, width, height);
					validate();
				}
				pressedAt = now;
			}
		};
		component.addMouseListener(drag);
		component.addMouseMotionListener(drag);
	}

	private void bindKeys(){
		final var inputMap = searchField.getInputMap(JTextField.WHEN_FOCUSED);
		final var actionMap = searchField.getActionMap();
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), ACTION_PREVIOUS);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), ACTION_NEXT);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), ACTION_RUN);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), ACTION_RUN_OTHER);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK), ACTION_RUN_THIRD);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.ALT_DOWN_MASK), ACTION_RUN_FOURTH);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), ACTION_IGNORE);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
			ACTION_TOGGLE_IGNORED);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), ACTION_WHERE_USED);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), ACTION_EXPORT);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ACTION_CLOSE);
		actionMap.put(ACTION_IGNORE, new SimpleAction(this::toggleIgnoreSelected));
		actionMap.put(ACTION_TOGGLE_IGNORED, new SimpleAction(this::toggleIgnoredView));
		actionMap.put(ACTION_WHERE_USED, new SimpleAction(this::showWhereUsed));
		actionMap.put(ACTION_EXPORT, new SimpleAction(this::exportFindings));
		actionMap.put(ACTION_PREVIOUS, new SimpleAction(() -> move(-1)));
		actionMap.put(ACTION_NEXT, new SimpleAction(() -> move(1)));
		actionMap.put(ACTION_RUN, new SimpleAction(() -> runSelected(0)));
		actionMap.put(ACTION_RUN_OTHER, new SimpleAction(() -> runSelected(1)));
		actionMap.put(ACTION_RUN_THIRD, new SimpleAction(() -> runSelected(2)));
		actionMap.put(ACTION_RUN_FOURTH, new SimpleAction(() -> runSelected(3)));
		actionMap.put(ACTION_CLOSE, new SimpleAction(this::dispose));
	}

	private static final class SimpleAction extends javax.swing.AbstractAction{

		private final Runnable runnable;

		SimpleAction(final Runnable runnable){
			this.runnable = runnable;
		}

		@Override
		public void actionPerformed(final ActionEvent e){
			runnable.run();
		}

	}


	/** Computes the findings away from the event dispatch thread: a provider can inspect the whole model. */
	private void loadDiagnostics(){
		//a copy of the model, taken here on the event dispatch thread: the actions keep the context with the model itself
		final AssistantContext reading = context.forReading();
		CompletableFuture
			.supplyAsync(() -> FindingLevels.visible(service.diagnose(reading, this::showProgress)))
			.exceptionally(e -> {
				LOGGER.warn("Cannot compute the findings: {}", e.toString());
				return List.of();
			})
			.thenAccept(found -> SwingUtilities.invokeLater(() -> {
				diagnostics = found;
				refresh();
			}));
	}

	/** Shows how far the computation is (it is told from the thread that computes, so it hands over to the event dispatch thread). */
	private void showProgress(final double fraction){
		final int step = (int)Math.round(Math.max(0., Math.min(1., fraction)) * PROGRESS_STEPS);
		SwingUtilities.invokeLater(() -> {
			//the findings may have arrived while this was waiting
			if(diagnostics != null)
				return;

			loadingBar.setIndeterminate(false);
			//never back: the updates are handed over in order, but one that is late must not move the bar back
			if(step > loadingBar.getValue())
				loadingBar.setValue(step);
			statusLabel.setText(I18N.tf("assistant.loading.progress", loadingBar.getValue() / (PROGRESS_STEPS / 100)));
		});
	}

	private Set<SearchMode> searchModes(){
		final Set<SearchMode> modes = EnumSet.noneOf(SearchMode.class);
		if(fuzzyCheckBox.isSelected())
			modes.add(SearchMode.FUZZY);
		if(wholeWordCheckBox.isSelected())
			modes.add(SearchMode.WHOLE_WORD);
		return modes;
	}

	private void refresh(){
		final String query = searchField.getText();
		rowsModel.clear();

		//the commands are not about findings: they are not listed with the ignored ones
		final List<ResearchAssistantService.RankedAction> actions = (showIgnored? List.of(): service.search(query, searchModes(), context));
		if(!actions.isEmpty()){
			rowsModel.addElement(new Header(I18N.t("assistant.section.actions")));
			actions.forEach(a -> rowsModel.addElement(new ActionRow(a.action())));
		}
		final List<Diagnostic> found = diagnostics;
		final IgnoredFindings.Split split = (found != null
			? service.ignoredFindings().split(context.scope(), found)
			: new IgnoredFindings.Split(List.of(), List.of()));
		//most severe first (the sort is stable): within the same severity the ones about the selected record come first
		final List<Diagnostic> matching = aboutSelected(ResearchAssistantService.filter(query, searchModes(),
				(showIgnored? split.ignored(): split.active()))).stream()
			.sorted(Comparator.comparing(Diagnostic::severity))
			.toList();
		if(!matching.isEmpty())
			if(showIgnored){
				rowsModel.addElement(new Header(I18N.t("assistant.section.ignored")));
				matching.forEach(d -> rowsModel.addElement(new DiagnosticRow(d)));
			}
			else
				//errors, then warnings, then information, each with its own heading
				for(final Diagnostic.Severity severity : Diagnostic.Severity.values()){
					final List<Diagnostic> ofSeverity = matching.stream()
						.filter(d -> d.severity() == severity)
						.toList();
					if(ofSeverity.isEmpty())
						continue;

					final String severityId = severityId(severity);
					final boolean severityOpen = isOpen(severityId);
					rowsModel.addElement(new Header(I18N.tf(switch(severity){
						case ERROR -> "assistant.section.errors";
						case WARNING -> "assistant.section.warnings";
						case INFORMATION -> "assistant.section.information";
					}, ofSeverity.size()), severityId, severityOpen));
					if(!severityOpen)
						continue;

					//and in each, the findings of the same kind under their own heading (the kinds in the order of the findings, so that
					//the ones about the selected record come first)
					final Map<String, List<Diagnostic>> ofKind = new LinkedHashMap<>();
					for(final Diagnostic d : ofSeverity)
						ofKind.computeIfAbsent(d.kind(), k -> new ArrayList<>())
							.add(d);
					for(final List<Diagnostic> group : ofKind.values()){
						final String kindId = kindId(group.getFirst());
						final boolean kindOpen = isOpen(kindId);
						rowsModel.addElement(new Header(kindLabel(group.getFirst()) + " (" + group.size() + ")", kindId, kindOpen));
						if(kindOpen)
							group.forEach(d -> rowsModel.addElement(new DiagnosticRow(d)));
					}
				}

		final String ignoredStatus = (showIgnored
			? I18N.tf("assistant.ignored.showing", shortcut("I"))
			: (split.ignored().isEmpty()? "": I18N.tf("assistant.ignored.count", split.ignored().size(), shortcut("I"))));
		final String exportStatus = (found == null || found.isEmpty()? "": I18N.tf("assistant.export.hint", shortcut("E")));
		final String status = String.join(" · ", Stream.of(notice, ignoredStatus, exportStatus)
			.filter(part -> !part.isEmpty())
			.toList());
		final boolean loading = (found == null);
		loadingBar.setVisible(loading);
		statusLabel.setText(loading? I18N.t("assistant.loading"): (status.isEmpty()? StringUtils.SPACE: status));
		if(loading && rowsModel.isEmpty())
			detailLabel.setText(null);
		else if(rowsModel.isEmpty())
			detailLabel.setText(I18N.t(showIgnored? "assistant.ignored.none": "assistant.no_results"));
		else
			move(0);
	}

	/**
	 * The findings about the record selected in the main window come first (the order is otherwise kept), or are the only ones if the
	 * user asked for that. Without a selection nothing changes.
	 */
	private List<Diagnostic> aboutSelected(final List<Diagnostic> findings){
		final String selectedId = context.selectedRecordId();
		if(selectedId == null || context.selectedRecord().isEmpty())
			return findings;

		return ResearchAssistantService.aboutRecord(findings, selectedId, selectedOnlyCheckBox.isSelected());
	}

	/**
	 * @return the name of the kind of a finding, for the heading of its group: the one the messages give to the kind
	 * ({@code assistant.kind.<kind>}), or the words of the kind after its category
	 */
	static String kindLabel(final Diagnostic diagnostic){
		final String kind = diagnostic.kind();
		final int dot = kind.indexOf('.');
		final String words = (dot >= 0? kind.substring(dot + 1): kind).replace('.', ' ').replace('_', ' ');
		return I18N.t("assistant.kind." + kind, diagnostic.category().title() + ": " + words);
	}

	/** Shows the findings the user ignored, or the ones to look at. */
	private void toggleIgnoredView(){
		showIgnored = !showIgnored;
		refresh();
	}

	/**
	 * Ignores the selected finding, or shows it again if it is one of the ignored. The choice is kept in the user preferences (never in
	 * the records), and the finding comes back by itself if the data it reports changes.
	 */
	private void toggleIgnoreSelected(){
		if(!(rowsList.getSelectedValue() instanceof DiagnosticRow row) || !row.diagnostic().isIgnorable())
			return;

		final int index = rowsList.getSelectedIndex();
		if(showIgnored)
			service.ignoredFindings().restore(context.scope(), row.diagnostic());
		else
			service.ignoredFindings().ignore(context.scope(), row.diagnostic());
		refresh();
		selectNear(index);
	}

	/**
	 * Selects the finding the user had selected when they acted on it, if it is still there; otherwise the one that took its place (the
	 * next one, or the last if it was the last).
	 */
	private void selectFinding(final State state){
		for(int i = 0; i < rowsModel.size(); i ++)
			if(rowsModel.get(i) instanceof DiagnosticRow row
					&& ResearchAssistantService.identity(row.diagnostic()).equals(state.selectedIdentity())){
				rowsList.setSelectedIndex(i);
				rowsList.ensureIndexIsVisible(i);
				return;
			}

		selectNear(state.selectedIndex());
	}

	/** Selects the row at an index, or the nearest one that is not a title (the one before if there is none after). */
	private void selectNear(final int index){
		final int size = rowsModel.size();
		for(int candidate = Math.min(index, size - 1); candidate >= 0; candidate --)
			if(!(rowsModel.get(candidate) instanceof Header header && !header.isCollapsible())){
				rowsList.setSelectedIndex(candidate);
				rowsList.ensureIndexIsVisible(candidate);
				return;
			}
	}

	/**
	 * Moves the selection by a row, skipping the titles (the headings that can be collapsed are rows of their own).
	 *
	 * @param delta {@code -1} for the previous row, {@code 1} for the next one, {@code 0} to select the first one
	 */
	private void move(final int delta){
		final int size = rowsModel.size();
		final int step = (delta < 0? -1: 1);
		int index = rowsList.getSelectedIndex();
		if(delta == 0 || index < 0)
			index = (step < 0? size: -1);
		do{
			index += step;
		}while(index >= 0 && index < size && rowsModel.get(index) instanceof Header header && !header.isCollapsible());

		if(index >= 0 && index < size){
			rowsList.setSelectedIndex(index);
			rowsList.ensureIndexIsVisible(index);
		}
	}

	private void showDetail(){
		final Row row = rowsList.getSelectedValue();
		detailBody = switch(row){
			case ActionRow a -> escape(a.action().description());
			case DiagnosticRow d -> escape(d.diagnostic().explanation()) + recordsOf(d.diagnostic()) + keyHints(d.diagnostic());
			case null, default -> null;
		};
		applyDetail();
	}

	/**
	 * Sets the text of the detail so that it wraps at the width the label really has (the user can resize the window), and makes the
	 * window taller if the text needs it.
	 */
	private void applyDetail(){
		
		detailLabelWidth = detailLabel.getWidth();
		detailLabel.setText(detailBody == null? StringUtils.SPACE: "<html><body>" + detailBody + "</body></html>");
		growToFitDetail();
	}

	/** A long detail makes the palette taller: the list keeps its room. The palette never gets smaller while it is open. */
	private void growToFitDetail(){
		if(!isDisplayable())
			return;

		final int needed = getContentPane().getPreferredSize().height;
		if(needed <= getHeight())
			return;

		//only the height changes: the width is the user's (the preferred one follows the longest row of the list)
		setSize(getWidth(), needed);
		//it stays inside the screen
		final Rectangle screen = getGraphicsConfiguration().getBounds();
		if(getY() + getHeight() > screen.y + screen.height)
			setLocation(getX(), Math.max(screen.y, screen.y + screen.height - getHeight()));
		revalidate();
	}


	private static String severityId(final Diagnostic.Severity severity){
		return "s:" + severity.name();
	}

	private static String kindId(final Diagnostic diagnostic){
		return "k:" + diagnostic.severity().name() + ":" + diagnostic.kind();
	}

	/**
	 * Whether a heading shows what is under it: a severity starts open, a kind of finding starts collapsed, and the user can change
	 * either. While something is searched everything is open, so that what matches is never hidden.
	 */
	private boolean isOpen(final String id){
		return (!searchField.getText().isBlank() || id.startsWith("s:") != toggled.contains(id));
	}

	/** Opens or closes the selected heading (nothing happens during a search, when everything is open). */
	void toggleSelected(){
		if(!(rowsList.getSelectedValue() instanceof Header header) || !header.isCollapsible() || !searchField.getText().isBlank())
			return;

		//the list stays where it is: it is rebuilt, but the user keeps the view and the heading they chose, and only what is under it
		//opens or closes
		final JViewport viewport = (JViewport)SwingUtilities.getAncestorOfClass(JViewport.class, rowsList);
		final Point position = (viewport != null? viewport.getViewPosition(): null);

		if(!toggled.remove(header.id()))
			toggled.add(header.id());
		refresh();
		for(int i = 0; i < rowsModel.size(); i ++)
			if(rowsModel.get(i) instanceof Header h && header.id().equals(h.id())){
				rowsList.setSelectedIndex(i);
				break;
			}
		if(viewport != null){
			viewport.setViewPosition(position);
			//the list is laid out again after the change: the position is set again when it is
			SwingUtilities.invokeLater(() -> viewport.setViewPosition(position));
		}
	}

	/** @return the rows as text, to look at what the palette lists: a heading is {@code [id open title]}, a finding its title */
	List<String> rows(){
		final List<String> rows = new ArrayList<>();
		for(final Object row : Collections.list(rowsModel.elements()))
			rows.add(switch(row){
				case Header h -> "[" + h.id() + (h.open()? " open ": " closed ") + h.title() + "]";
				case DiagnosticRow d -> d.diagnostic().title();
				case ActionRow a -> a.action().title();
				default -> row.toString();
			});
		return rows;
	}

	/** @return the text of the detail of the selected row (HTML), to look at what the palette says about it */
	String detail(){
		return detailBody;
	}

	void select(final int index){
		rowsList.setSelectedIndex(index);
	}

	/** @return where the palette is now (the query, the view and the selection), to come back to it after an action */
	private State currentState(){
		return new State(searchField.getText(), searchModes(), showIgnored, switch(rowsList.getSelectedValue()){
			case DiagnosticRow d -> ResearchAssistantService.identity(d.diagnostic());
			case null, default -> null;
		}, rowsList.getSelectedIndex(), Set.copyOf(toggled), selectedOnlyCheckBox.isSelected());
	}

	/**
	 * Saves all the findings (the ones to look at, then the ones the user ignored, marked as such) as a CSV file, to work on them outside
	 * the application. The palette stays open. Nothing is changed in the research.
	 */
	private void exportFindings(){
		final List<Diagnostic> found = diagnostics;
		if(found == null || found.isEmpty())
			return;

		final IgnoredFindings.Split split = service.ignoredFindings().split(context.scope(), found);
		final List<FindingsExport.Entry> entries = new ArrayList<>(found.size());
		split.active().forEach(d -> entries.add(new FindingsExport.Entry(d, false)));
		split.ignored().forEach(d -> entries.add(new FindingsExport.Entry(d, true)));

		final JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(I18N.t("assistant.export"));
		chooser.setSelectedFile(new File("assistant-findings.csv"));
		if(chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return;

		File file = chooser.getSelectedFile();
		if(!file.getName().toLowerCase(Locale.ROOT).endsWith(".csv"))
			file = new File(file.getParentFile(), file.getName() + ".csv");
		if(file.exists() && JOptionPane.showConfirmDialog(this, I18N.tf("assistant.export.overwrite", file.getName()),
				I18N.t("assistant.export"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)
			return;

		//the file is written away from the event dispatch thread; the outcome is reported on it
		final File target = file;
		CompletableFuture.runAsync(() -> {
			try{
				FindingsExport.write(target.toPath(), entries);
			}
			catch(final IOException ioe){
				throw new UncheckedIOException(ioe);
			}
		}).whenComplete((ignored, error) -> SwingUtilities.invokeLater(() -> {
			if(error == null)
				SavedFileNotice.show(this, I18N.t("assistant.export"), I18N.tf("assistant.export.done", entries.size(), target.getName()), target);
			else{
				final Throwable cause = (error.getCause() != null? error.getCause(): error);
				LOGGER.warn("Cannot export the findings: {}", cause.toString());
				JOptionPane.showMessageDialog(this, I18N.t("assistant.export.error") + "\n" + cause.getMessage(), I18N.t("assistant.export"),
					JOptionPane.ERROR_MESSAGE);
			}
		}));
	}

	/**
	 * Shows where the records of the selected finding are used (the records that point to them). The palette closes first, as for the
	 * actions of a finding, and comes back if what the user did in the meantime changed the findings.
	 */
	private void showWhereUsed(){
		if(!(rowsList.getSelectedValue() instanceof DiagnosticRow row) || row.diagnostic().recordIds().isEmpty())
			return;

		final List<Diagnostic> before = diagnostics;
		final State state = currentState();
		final DiagnosticAction action = new WhereUsedAction(row.diagnostic().recordIds());
		dispose();
		SwingUtilities.invokeLater(() -> {
			action.execute(context);
			service.reopenIfChanged(context, before, state);
		});
	}

	/**
	 * Runs an action of the selected row: the first one with Enter, the second one (if any) with Ctrl+Enter, the third one with
	 * Ctrl+Shift+Enter, the fourth one with Ctrl+Alt+Enter. A command has only the first one, a finding has the ones of
	 * {@link DiagnosticActions}. A heading that can be collapsed opens or closes instead.
	 */
	private void runSelected(final int index){
		//a heading that can be collapsed opens or closes
		if(rowsList.getSelectedValue() instanceof Header header && header.isCollapsible()){
			toggleSelected();
			return;
		}

		final List<Diagnostic> before = diagnostics;
		final State state = currentState();
		final Runnable run = switch(rowsList.getSelectedValue()){
			case ActionRow a when index == 0 -> () -> a.action().execute(context);
			case DiagnosticRow d -> {
				final List<DiagnosticAction> actions = DiagnosticActions.of(d.diagnostic());
				//the editor the action opens is modal: when it returns the user is done, and the findings may have changed
				final DiagnosticAction chosen = DiagnosticActions.forSlot(actions, index);
				yield (chosen != null
					? () -> {
						chosen.execute(context);
						service.reopenIfChanged(context, before, state);
					}
					: null);
			}
			case null, default -> null;
		};
		if(run == null)
			return;

		dispose();
		//the palette is closed first, so that the action can open its own dialog over the editor
		SwingUtilities.invokeLater(run);
	}


	private static JLabel render(final JList<? extends Row> list, final Row row, final boolean selected){
		final JLabel label = new JLabel();
		label.setOpaque(true);
		label.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
		label.setBackground(selected? list.getSelectionBackground(): list.getBackground());
		label.setForeground(selected? list.getSelectionForeground(): list.getForeground());
		switch(row){
			case Header h -> {
				//a heading that can be collapsed says what a click does; a kind of finding is indented under its severity
				label.setText((h.isCollapsible()? (h.open()? "▾ ": "▸ "): "") + (h.id() != null && h.id().startsWith("k:")? "   ": "")
					+ h.title());
				label.setFont(label.getFont().deriveFont(Font.BOLD));
				//the headings of the severities (errors, warnings, information) are in the full color of the text, the ones under them in
				//the muted one, so that the two levels are told apart at a glance; a selected heading keeps the color of the selection
				if(!selected)
					label.setForeground(UIManager.getColor(h.id() != null && h.id().startsWith("s:")
						? "Label.foreground": "Label.disabledForeground"));
			}
			case ActionRow a -> label.setText(a.action().title() + "  ·  " + a.action().category().title());
			case DiagnosticRow d -> label.setText(severitySymbol(d.diagnostic().severity()) + StringUtils.SPACE + d.diagnostic().title());
		}
		return label;
	}

	/**
	 * The records of a finding, each with its type, what it is about and its identifier, so that every record the finding is about can be
	 * told apart, whether or not the text of the finding names it.
	 *
	 * @return the list, in HTML, with a line before it (empty if the finding is not about records)
	 */
	private String recordsOf(final Diagnostic diagnostic){
		if(diagnostic.recordIds().isEmpty())
			return "";

		final StringBuilder records = new StringBuilder("<br><br><b>").append(escape(I18N.t("assistant.records"))).append("</b>");
		for(final String id : diagnostic.recordIds()){
			final FLEFRecord record = (context.model() != null? context.model().getRecordById(id): null);
			final String text = escape(record != null? OpenRecordsAction.describe(context.model(), record): id);
			//a record that has an editor is a link: it opens
			final boolean editable = (record != null && !OpenRecordsAction.records(context.model(), List.of(id)).isEmpty());
			records.append("<br>• ").append(editable? "<a href=\"" + RECORD_LINK + escape(id) + "\">" + text + "</a>": text);
		}
		return records.toString();
	}

	/** Opens the editor of a record of the detail (the palette closes first, as for the actions, and comes back if something changed). */
	private void openRecord(final String id){
		final FLEFModel model = context.model();
		if(model == null)
			return;

		final List<FLEFRecord> records = OpenRecordsAction.records(model, List.of(id));
		if(records.isEmpty())
			return;

		final List<Diagnostic> before = diagnostics;
		final State state = currentState();
		dispose();
		SwingUtilities.invokeLater(() -> {
			OpenRecordsAction.open(context.owner(), model, records.getFirst());
			service.reopenIfChanged(context, before, state);
		});
	}

	/**
	 * The keys of a finding: Enter for the first action, Ctrl+Enter (Cmd+Enter on a Mac) for the second one, Ctrl+D to ignore it (or to
	 * show it again, if it is one of the ignored).
	 */
	private String keyHints(final Diagnostic diagnostic){
		final List<DiagnosticAction> actions = DiagnosticActions.of(diagnostic);
		final String[] keys = {"Enter", shortcut("Enter"), shortcut("Shift+Enter"), shortcut("Alt+Enter")};
		final StringBuilder hints = new StringBuilder();
		for(final DiagnosticAction action : actions){
			//the key of an action never changes; Enter also runs the first one when no action has it
			final int slot = action.keySlot();
			final String key = (slot != 0 && action == actions.getFirst() && DiagnosticActions.forSlot(actions, 0) == action
				? keys[0] + ", " + keys[slot]: keys[slot]);
			hints.append("<br><b>").append(escape(I18N.tf("assistant.diagnostic.key", key, action.title()))).append("</b>");
		}
		if(!diagnostic.recordIds().isEmpty())
			hints.append("<br><b>").append(escape(I18N.tf("assistant.diagnostic.key", shortcut("U"), I18N.t("assistant.where_used")))).append("</b>");
		if(diagnostic.isIgnorable())
			hints.append("<br><b>").append(escape(I18N.tf("assistant.diagnostic.key", shortcut("D"),
				I18N.t(showIgnored? "assistant.restore": "assistant.ignore")))).append("</b>");
		//an empty line separates the keys from the description
		return (hints.isEmpty()? "": "<br>" + hints);
	}

	/** @return a key with the modifier of the shortcuts of the platform (Ctrl, Cmd on a Mac) */
	private static String shortcut(final String key){
		return (ShortcutRegistry.IS_MAC? "⌘": "Ctrl+") + key;
	}

	private static String severitySymbol(final Diagnostic.Severity severity){
		return switch(severity){
			case ERROR -> "✖";
			case WARNING -> "⚠";
			case INFORMATION -> "ℹ";
		};
	}

	private static String escape(final String text){
		if(text == null)
			return "";
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

}
