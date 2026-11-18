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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.comparison.DiffKind;
import io.github.mtrevisan.familylegacy.ui.components.comparison.DiffPainters;
import io.github.mtrevisan.familylegacy.ui.components.comparison.RecordAlignment;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Dialog that compares two FLEFRecord objects side-by-side with difference highlighting and synchronized scrolling. Allows the user
 * to choose which record to keep.
 * <p>
 * It compares the two records as the review of a merge does ({@link RecordAlignment}): the columns are aligned <b>a sub-record at a
 * time</b>, so a block of one column faces at most one block of the other and is never interrupted by a second one; the sub-records
 * are shown in the same order in both columns, whatever order they had; blocks that cite the same record face each other, and ones
 * that cite different records do not; a reference to another record is a stronger difference than another text. The colours are of
 * the line ({@link DiffPainters}), the lines that cite a record say, after their text, the name of the record, and the buttons (and
 * the keys <em>F7</em> and <em>F8</em>) go to the previous and the next difference.
 * <p>
 * Where a column has no line it has an empty one, in grey, so that line N on the left and line N on the right always refer to the
 * same place. The window opens on the first line, and stays there until the user moves it.
 */
public class RecordDiffDialog extends JDialog{

	private static final String PLACEHOLDER_NULL = "<null>";
	private static final String OPEN_PARENTHESIS = "(";
	private static final String CLOSE_PARENTHESIS = ")";

	private static final Color COLOR_SELECTION = new Color(197, 220, 255);
	private static final Color COLOR_SELECTED_TEXT = Color.BLACK;
	/** The frame of the difference the window is on. */
	private static final Color COLOR_CURRENT = new Color(0x1565C0);

	/** What an identifier of a record looks like: letters and a number (I12, IH3, S1). Only the ones that are records of the model are links. */
	private static final Pattern IDENTIFIER = Pattern.compile("\\b[A-Za-z]{1,4}\\d+\\b");
	/** The color of the underline of a link. */
	private static final Color COLOR_LINK = new Color(0x0645AD);

	private final DiffTextArea leftArea;
	private final DiffTextArea rightArea;
	private final JScrollPane leftScroll;
	private final JScrollPane rightScroll;
	private final JButton previousButton = new JButton("\u25B2 " + I18N.t("dialog.record_diff.nav.previous"));
	private final JButton nextButton = new JButton("\u25BC " + I18N.t("dialog.record_diff.nav.next"));
	private final JLabel summary = new JLabel(StringUtils.SPACE);

	/** The model the references are records of ({@code null}: there are no links). */
	private final FLEFModel model;
	private final FLEFRecord before;
	private final FLEFRecord after;

	/** The places where the two records differ, as the first row and the last: one after the other. */
	private List<int[]> differences = List.of();
	private int current = -1;
	/** The window has just opened: the view stays at the first line until the user moves it, whatever the layout does meanwhile. */
	private boolean opening = true;

	private boolean accepted;
	// true = keep right (after), false = keep left (before)
	private boolean keepRight;


	/**
	 * Constructs a RecordDiffDialog in interactive mode: the user can
	 * choose one of the two records to keep.
	 */
	public RecordDiffDialog(final Window owner, final String title, final FLEFRecord before,
		final FLEFRecord after){
		this(owner, title, before, after, false);
	}

	/**
	 * Constructs a RecordDiffDialog.
	 *
	 * @param readOnly when {@code true}, the accept buttons are hidden
	 *                 and the dialog only shows the diff; the returned
	 *                 record is always {@code null}
	 */
	public RecordDiffDialog(final Window owner, final String title, final FLEFRecord before,
		final FLEFRecord after, final boolean readOnly){
		this(owner, title, before, after, readOnly, null);
	}

	/**
	 * Constructs a RecordDiffDialog whose references are links.
	 *
	 * @param readOnly when {@code true}, the accept buttons are hidden and the dialog only shows the diff
	 * @param model    the model the records are in: every identifier in the text that is a record of it is a link that opens the
	 *                 editor of that record; {@code null} for no links
	 */
	public RecordDiffDialog(final Window owner, final String title, final FLEFRecord before,
			final FLEFRecord after, final boolean readOnly, final FLEFModel model){
		super(owner, title, ModalityType.APPLICATION_MODAL);

		GUIHelper.installEscapeToClose(this);

		this.model = model;
		this.before = before;
		this.after = after;

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		leftArea = createTextArea();
		rightArea = createTextArea();
		leftScroll = new JScrollPane(leftArea);
		rightScroll = new JScrollPane(rightArea);

		// Align the two records and fill both text areas.
		showDiff();
		enableLinks(leftArea);
		enableLinks(rightArea);

		// Setup synchronized scrolling.
		syncScrolling(leftScroll, rightScroll);
		keepAtTopWhileOpening();

		// Header labels showing record info.
		final String leftLabel = (before != null
			? before.getTag() + StringUtils.SPACE + OPEN_PARENTHESIS + before.getId() + CLOSE_PARENTHESIS
			: PLACEHOLDER_NULL);
		final String rightLabel = (after != null
			? after.getTag() + StringUtils.SPACE + OPEN_PARENTHESIS + after.getId() + CLOSE_PARENTHESIS
			: PLACEHOLDER_NULL);

		final JLabel leftHeader = new JLabel(leftLabel, SwingConstants.CENTER);
		final JLabel rightHeader = new JLabel(rightLabel, SwingConstants.CENTER);
		leftHeader.setFont(leftHeader.getFont().deriveFont(Font.BOLD));
		rightHeader.setFont(rightHeader.getFont().deriveFont(Font.BOLD));

		final JPanel headerPanel = new JPanel(new GridLayout(1, 2, 5, 5));
		headerPanel.add(leftHeader);
		headerPanel.add(rightHeader);

		final JPanel top = new JPanel(new BorderLayout(0, 4));
		top.setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 6));
		top.add(createToolbar(), BorderLayout.NORTH);
		top.add(headerPanel, BorderLayout.CENTER);

		final JPanel centerPanel = new JPanel(new GridLayout(1, 2, 5, 5));
		centerPanel.add(leftScroll);
		centerPanel.add(rightScroll);

		// Buttons.
		final List<JButton> accept = new ArrayList<>();
		if(!readOnly){
			final JButton acceptLeftBtn = new JButton(I18N.t("button.revert_to_previous"));
			final JButton acceptRightBtn = new JButton(I18N.t("button.keep_current"));
			acceptLeftBtn.addActionListener(e -> {
				accepted = true;
				keepRight = false;
				dispose();
			});
			acceptRightBtn.addActionListener(e -> {
				accepted = true;
				keepRight = true;
				dispose();
			});
			accept.add(acceptLeftBtn);
			accept.add(acceptRightBtn);
		}
		setLayout(new BorderLayout(10, 10));
		add(top, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
		//how many differences there are, and which one the window is on, at the left as in every window with a table
		summary.setForeground(DiffPainters.COLOR_MUTED);
		add(GUIHelper.createStatusFooter(summary, accept.toArray(new JButton[0])), BorderLayout.SOUTH);

		installKeys();
		updateNavigation();

		setPreferredSize(new Dimension(900, 600));
		pack();
		setLocationRelativeTo(owner);

		//the first line is the one that is shown, not a difference: the buttons go to the differences
		addWindowListener(new WindowAdapter(){
			@Override
			public void windowOpened(final WindowEvent e){
				SwingUtilities.invokeLater(() -> showTop());
			}
		});
	}


	/**
	 * Opens the dialog in read-only comparison mode, with the references as links.
	 *
	 * @param model	The model the records are in: every identifier that is a record of it opens its editor.
	 */
	public static void showComparison(final Window owner, final String title, final FLEFRecord left, final FLEFRecord right,
			final FLEFModel model){
		new RecordDiffDialog(owner, title, left, right, true, model)
			.setVisible(true);
	}

	/**
	 * Opens the dialog in read-only comparison mode: no accept button is
	 * shown, the diff is highlighted, and the dialog is closed only with
	 * the ESC key or the window close action.
	 */
	public static void showComparison(final Window owner, final String title,
		final FLEFRecord left, final FLEFRecord right){
		final RecordDiffDialog dialog = new RecordDiffDialog(owner, title, left, right, true);
		dialog.setVisible(true);
	}


	/* ======================================================================
	 *                          Toolbar and keys
	 * ====================================================================== */

	private JPanel createToolbar(){
		final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

		previousButton.setToolTipText(I18N.t("dialog.record_diff.nav.previous.tooltip"));
		previousButton.addActionListener(e -> goToPrevious());
		toolbar.add(previousButton);
		nextButton.setToolTipText(I18N.t("dialog.record_diff.nav.next.tooltip"));
		nextButton.addActionListener(e -> goToNext());
		toolbar.add(nextButton);
		toolbar.add(createChip(I18N.t("dialog.record_diff.legend.left"), DiffPainters.COLOR_FROM_SOURCE));
		toolbar.add(createChip(I18N.t("dialog.record_diff.legend.right"), DiffPainters.COLOR_FROM_TARGET));
		toolbar.add(createChip(I18N.t("dialog.record_diff.legend.different"), DiffPainters.COLOR_CONFLICT));
		toolbar.add(createChip(I18N.t("dialog.record_diff.legend.reference"), DiffPainters.COLOR_REFERENCE));
		return toolbar;
	}

	private static JLabel createChip(final String text, final Color color){
		final JLabel chip = new JLabel(text);
		chip.setOpaque(true);
		chip.setBackground(color);
		chip.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xD0D0D0)),
			BorderFactory.createEmptyBorder(1, 6, 1, 6)));
		return chip;
	}

	private void installKeys(){
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), "next-difference", this::goToNext);
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), "previous-difference", this::goToPrevious);
	}

	private void bind(final KeyStroke key, final String name, final Runnable action){
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
		getRootPane().getActionMap().put(name, new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent e){
				action.run();
			}
		});
	}


	/* ======================================================================
	 *                          Differences
	 * ====================================================================== */

	/** Whether a line is a difference: it only one side has, or that is not the same in the two, as opposed to the shape of the text. */
	private static boolean isDifference(final DiffKind kind){
		return (kind == DiffKind.FROM_SOURCE || kind == DiffKind.FROM_TARGET || kind == DiffKind.CONFLICT || kind == DiffKind.REFERENCE);
	}

	/** The places where the two columns differ: the rows that are a difference, one after the other, are one place. */
	private static List<int[]> differencesOf(final RecordAlignment.Rows rows){
		final List<int[]> found = new ArrayList<>();
		int start = -1;
		for(int row = 0; row <= rows.size(); row ++){
			final boolean different = (row < rows.size()
				&& (isDifference(rows.sourceKind.get(row)) || isDifference(rows.targetKind.get(row))));
			if(different && start < 0)
				start = row;
			else if(!different && start >= 0){
				found.add(new int[]{start, row - 1});
				start = -1;
			}
		}
		return found;
	}

	private void updateNavigation(){
		nextButton.setEnabled(current < differences.size() - 1);
		previousButton.setEnabled(!differences.isEmpty() && current != 0);
		summary.setText(current >= 0
			? I18N.tf("dialog.record_diff.position", current + 1, differences.size())
			: I18N.tf("dialog.record_diff.summary", differences.size()));
	}

	void goToNext(){
		if(current < differences.size() - 1)
			goTo(current + 1);
	}

	void goToPrevious(){
		if(!differences.isEmpty() && current != 0)
			goTo(current < 0? differences.size() - 1: current - 1);
	}

	/** Shows the difference a third of the way down the window, framed, in both columns. */
	private void goTo(final int index){
		opening = false;
		current = index;
		final int[] rows = differences.get(index);
		leftArea.setCurrentRows(rows);
		rightArea.setCurrentRows(rows);
		updateNavigation();
		SwingUtilities.invokeLater(() -> {
			try{
				final Rectangle2D top = leftArea.modelToView2D(leftArea.getLineStartOffset(rows[0]));
				if(top != null)
					leftScroll.getVerticalScrollBar().setValue(Math.max(0, (int)top.getY() - leftScroll.getViewport().getHeight() / 3));
			}
			catch(final BadLocationException ignored){
				//the rows are the ones of the text just written: it cannot happen
			}
		});
	}

	/** The index of the difference the window is on, from 0, or -1: for the tests. */
	int currentDifference(){
		return current;
	}

	/** How many places the two records differ in: for the tests. */
	int differenceCount(){
		return differences.size();
	}

	/** The text of the left and of the right column, with the empty lines of the alignment: for the tests. */
	String leftText(){
		return leftArea.getText();
	}

	String rightText(){
		return rightArea.getText();
	}

	/** Where the vertical scroll bar is: for the tests. */
	int verticalScrollPosition(){
		return leftScroll.getVerticalScrollBar().getValue();
	}

	/** Shows the record from its first line: the scroll bars and the carets at the top. */
	void showTop(){
		leftArea.setCaretPosition(0);
		rightArea.setCaretPosition(0);
		leftScroll.getVerticalScrollBar().setValue(0);
		leftScroll.getHorizontalScrollBar().setValue(0);
		rightScroll.getVerticalScrollBar().setValue(0);
		rightScroll.getHorizontalScrollBar().setValue(0);
	}

	/** The view stays at the first line until the user moves it (the wheel, a scroll bar, the buttons), whatever the layout does. */
	private void keepAtTopWhileOpening(){
		for(final JScrollPane scroll : List.of(leftScroll, rightScroll)){
			scroll.getViewport().addChangeListener(e -> {
				if(opening && scroll.getVerticalScrollBar().getValue() != 0)
					SwingUtilities.invokeLater(this::keepAtTop);
			});
			final MouseAdapter userScrolls = new MouseAdapter(){
				@Override
				public void mousePressed(final MouseEvent e){
					opening = false;
				}

				@Override
				public void mouseWheelMoved(final MouseWheelEvent e){
					opening = false;
				}
			};
			scroll.addMouseWheelListener(userScrolls);
			scroll.getVerticalScrollBar().addMouseListener(userScrolls);
			scroll.getHorizontalScrollBar().addMouseListener(userScrolls);
		}
	}

	private void keepAtTop(){
		//the user is on the text (to select it, to follow a link): the view follows them
		if(opening && !(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JTextArea))
			showTop();
	}


	/* ======================================================================
	 *                          Links
	 * ====================================================================== */

	/** Aligns the two records and writes them in the two text areas, coloured and with the names of the records they cite. */
	private void showDiff(){
		final RecordAlignment.Rows rows = RecordAlignment.align(before, after, model);
		display(leftArea, rows.source, rows.sourceKind);
		display(rightArea, rows.target, rows.targetKind);
		findLinks(leftArea);
		findLinks(rightArea);

		differences = differencesOf(rows);
		current = -1;
		leftArea.setCurrentRows(null);
		rightArea.setCurrentRows(null);
		updateNavigation();
	}

	private void display(final JTextArea area, final List<String> lines, final List<DiffKind> kinds){
		final StringBuilder text = new StringBuilder();
		for(final String line : lines){
			if(line != null)
				text.append(line);
			text.append(StringUtils.LF);
		}
		area.setText(text.toString());
		//the text is read from its first line: a caret at the end would make the view follow it there
		area.setCaretPosition(0);
		DiffPainters.paint(area, kinds.toArray(new DiffKind[0]), line -> DiffPainters.nameOfReference(line, model, this::displayNameOf));
	}

	/** How the application names a record: through the handler of its kind. */
	private String displayNameOf(final FLEFRecord record){
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(record.getTag());
		return (handler != null? handler.getDisplayText(record, model): null);
	}

	/**
	 * @param text	A text of a record.
	 * @param model	The model, {@code null} for none.
	 * @return	The places of the text that are an identifier of a record of the model: {start, end (excluded)}, in order; the identifier is
	 * 	the text between them.
	 */
	static List<int[]> linkRanges(final String text, final FLEFModel model){
		final List<int[]> ranges = new ArrayList<>();
		if(model == null)
			return ranges;

		final Matcher matcher = IDENTIFIER.matcher(text);
		while(matcher.find())
			if(model.hasRecord(matcher.group()))
				ranges.add(new int[]{matcher.start(), matcher.end()});
		return ranges;
	}

	private void findLinks(final DiffTextArea area){
		area.setLinks(linkRanges(area.getText(), model));
	}

	private void enableLinks(final DiffTextArea area){
		if(model == null)
			return;

		area.setToolTipText(I18N.t("dialog.record_diff.links.tooltip"));
		area.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(SwingUtilities.isLeftMouseButton(e))
					openLinkAt(area, e.getPoint());
			}
		});
		area.addMouseMotionListener(new MouseAdapter(){
			@Override
			public void mouseMoved(final MouseEvent e){
				area.setCursor(area.recordIdAt(e.getPoint()) != null? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
					: Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
			}
		});
	}

	/** Opens the editor of the record that the text at the point refers to, then compares the records again: they may have changed. */
	private void openLinkAt(final DiffTextArea area, final Point point){
		final String recordId = area.recordIdAt(point);
		if(recordId == null)
			return;

		final var record = model.getRecordById(recordId);
		final RecordTypeHandler<?> handler = (record != null? HandlerRegistry.getHandler(record.getTag()): null);
		if(handler == null)
			return;

		handler.createEditDialog(this, model, record)
			.setVisible(true);
		showDiff();
	}


	/* ======================================================================
	 *                          Text area construction
	 * ====================================================================== */

	private DiffTextArea createTextArea(){
		final DiffTextArea area = new DiffTextArea();
		area.setEditable(false);
		area.setFont(new Font("Monospaced", Font.PLAIN, 12));
		area.setTabSize(4);

		// The default look-and-feel typically renders selected text in
		// white, assuming a dark selection background. That clashes with
		// our pastel diff highlights, so we force a dark, fixed
		// selected-text color and a light selection color.
		area.setSelectionColor(COLOR_SELECTION);
		area.setSelectedTextColor(COLOR_SELECTED_TEXT);

		return area;
	}

	private void syncScrolling(final JScrollPane left, final JScrollPane right){
		final JScrollBar leftVerticalScrollBar = left.getVerticalScrollBar();
		final JScrollBar leftHorizontalScrollBar = left.getHorizontalScrollBar();
		final JScrollBar rightVerticalScrollBar = right.getVerticalScrollBar();
		final JScrollBar rightHorizontalScrollBar = right.getHorizontalScrollBar();
		final AdjustmentListener listener = e -> {
			if(e.getAdjustable() == leftVerticalScrollBar)
				rightVerticalScrollBar.setValue(e.getValue());
			else if(e.getAdjustable() == rightVerticalScrollBar)
				leftVerticalScrollBar.setValue(e.getValue());

			if(e.getAdjustable() == leftHorizontalScrollBar)
				rightHorizontalScrollBar.setValue(e.getValue());
			else if(e.getAdjustable() == rightHorizontalScrollBar)
				leftHorizontalScrollBar.setValue(e.getValue());
		};
		leftVerticalScrollBar.addAdjustmentListener(listener);
		rightVerticalScrollBar.addAdjustmentListener(listener);
		leftHorizontalScrollBar.addAdjustmentListener(listener);
		rightHorizontalScrollBar.addAdjustmentListener(listener);
	}


	/* ======================================================================
	 *                          Custom text area
	 * ====================================================================== */

	/**
	 * A {@link JTextArea} that underlines the links and frames the rows of the difference the window is on. It scrolls the record to its
	 * caret only while it has the focus (the user is on it): the program moving the caret does not move the view.
	 */
	private static final class DiffTextArea extends JTextArea{

		/** The links: {start, end (excluded)} in the text. */
		private List<int[]> links = List.of();
		/** The first and the last row of the difference the window is on, {@code null} for none. */
		private int[] currentRows;

		DiffTextArea(){
			super();
		}

		void setLinks(final List<int[]> links){
			this.links = List.copyOf(links);
			repaint();
		}

		void setCurrentRows(final int[] rows){
			currentRows = rows;
			repaint();
		}

		@Override
		public void scrollRectToVisible(final Rectangle rectangle){
			if(isFocusOwner())
				super.scrollRectToVisible(rectangle);
		}

		/** @return	The identifier of the record that the text at the point is a link to; {@code null} if it is not a link. */
		String recordIdAt(final Point point){
			final int offset = viewToModel2D(point);
			for(final int[] link : links)
				if(offset >= link[0] && offset < link[1])
					return getText().substring(link[0], link[1]);
			return null;
		}

		/** Underlines the links in the color of a link: a text area has one color for all its text. */
		private void paintLinks(final Graphics g){
			if(links.isEmpty())
				return;

			final Graphics2D g2 = (Graphics2D)g.create();
			try{
				g2.setColor(COLOR_LINK);
				for(final int[] link : links){
					final Rectangle2D start = modelToView2D(link[0]);
					final Rectangle2D end = modelToView2D(link[1]);
					if(start != null && end != null && start.getY() == end.getY()){
						final int y = (int)start.getMaxY() - 2;
						g2.drawLine((int)start.getX(), y, (int)end.getX(), y);
					}
				}
			}
			catch(final BadLocationException ignored){
				//the links are the ones of the text just written: it cannot happen
			}
			finally{
				g2.dispose();
			}
		}

		/** Frames the rows of the difference the window is on. */
		private void paintCurrent(final Graphics g){
			if(currentRows == null)
				return;

			final Graphics2D g2 = (Graphics2D)g.create();
			try{
				final Rectangle2D first = modelToView2D(getLineStartOffset(currentRows[0]));
				final Rectangle2D last = modelToView2D(getLineStartOffset(currentRows[1]));
				if(first == null || last == null)
					return;

				g2.setColor(COLOR_CURRENT);
				g2.drawRect(0, (int)first.getY(), getWidth() - 1, (int)(last.getMaxY() - first.getY()) - 1);
				g2.drawRect(1, (int)first.getY() + 1, getWidth() - 3, (int)(last.getMaxY() - first.getY()) - 3);
			}
			catch(final BadLocationException ignored){
				//the rows are the ones of the text just written: it cannot happen
			}
			finally{
				g2.dispose();
			}
		}

		@Override
		protected void paintComponent(final Graphics g){
			super.paintComponent(g);

			paintLinks(g);
			paintCurrent(g);
		}

	}


	/* ======================================================================
	 *                          Accessors
	 * ====================================================================== */

	/** Returns whether the user accepted (clicked one of the accept buttons). */
	public boolean isAccepted(){
		return accepted;
	}

	/** Returns whether the user chose the right (after) record. */
	public boolean isKeepRight(){
		return keepRight;
	}

	/**
	 * Returns the record selected by the user.
	 *
	 * @param before the "before" record
	 * @param after  the "after" record
	 * @return the chosen record, or {@code null} if the dialog was canceled
	 */
	public FLEFRecord getSelectedRecord(final FLEFRecord before, final FLEFRecord after){
		if(!accepted)
			return null;

		return (keepRight? after: before);
	}


	/* ======================================================================
	 *                          Bootstrap
	 * ====================================================================== */

	static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		SwingUtilities.invokeLater(() -> {
			final JFrame frame = new JFrame("Test Record Diff");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			final FLEFRecord before = FLEFRecord.createChildWithTag(IndividualReader.TYPE);
			before.setId("I1");
			before.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TAG_NAME, "John Doe"));
			before.addChild(FLEFRecord.createChildWithTagAndValue("birth", "2000-01-01"));
			before.addChild(FLEFRecord.createChildWithTagAndValue("ill", "2010-01-01"));

			final FLEFRecord after = FLEFRecord.createChildWithTag(IndividualReader.TYPE);
			after.setId("I1");
			after.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TAG_NAME, "John Doe"));
			after.addChild(FLEFRecord.createChildWithTagAndValue("birth", "2000-01-02"));
			after.addChild(FLEFRecord.createChildWithTagAndValue("death", "2024-12-31"));

			final RecordDiffDialog dialog = new RecordDiffDialog(frame, "Compare Records", before, after);
			dialog.setVisible(true);

			if(dialog.isAccepted()){
				final FLEFRecord selected = dialog.getSelectedRecord(before, after);
				System.out.println("Selected: " + selected);
			}
			else
				System.out.println("Cancelled");
		});
	}

}
