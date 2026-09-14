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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.FLEFWriter;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.DiffUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.AdjustmentListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Dialog that compares two FLEFRecord objects side-by-side with
 * difference highlighting and synchronized scrolling. Allows the user
 * to choose which record to keep.
 * <p>
 * <b>Aligned rows.</b> Both text areas are rebuilt from the diff, so
 * each logical position occupies the same row on both sides. When a
 * line exists only on one side, the other side receives an empty
 * placeholder row at the same position, so line N on the left and line
 * N on the right always refer to the same content.
 * <p>
 * <b>Placeholder rendering.</b> Placeholder rows are drawn with a
 * diagonal hatch pattern, in the style of Beyond Compare, so a missing
 * line is visually distinct from a genuinely empty line in the source.
 * The hatch is painted by a custom {@link JTextArea} subclass after the
 * normal text rendering, and is clipped to the exact bounds of the
 * placeholder rows.
 */
public class RecordDiffDialog extends JDialog{

	private static final String PLACEHOLDER_NULL = "<null>";
	private static final String OPEN_PARENTHESIS = "(";
	private static final String CLOSE_PARENTHESIS = ")";

	private static final Color COLOR_DELETE = new Color(255, 214, 214);
	private static final Color COLOR_INSERT = new Color(214, 255, 220);
	private static final Color COLOR_MODIFIED = new Color(255, 244, 199);
	private static final Color COLOR_SELECTION = new Color(197, 220, 255);
	private static final Color COLOR_SELECTED_TEXT = Color.BLACK;

	/** Color of the diagonal hatch drawn on placeholder rows. */
	private static final Color COLOR_HATCH = new Color(140, 140, 140);
	/** Distance between two hatch lines, in pixels. */
	private static final int HATCH_SPACING = 9;

	private final DiffTextArea leftArea;
	private final DiffTextArea rightArea;

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
		super(owner, title, ModalityType.APPLICATION_MODAL);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		final FLEFWriter writer = FLEFWriter.createCompact();

		// Normalize both records before serializing them. Without this,
		// two records that carry the same information but list their
		// children in different orders would be shown as different: a
		// diff line would appear for every block whose position changed,
		// even though the content is unchanged.
		final String leftText = writer.writeToString(RecordNormalizer.normalize(before));
		final String rightText = writer.writeToString(RecordNormalizer.normalize(after));

		leftArea = createTextArea();
		rightArea = createTextArea();

		// Compute the diff and rebuild both text areas with aligned rows.
		applyDiff(leftText, rightText);

		// Setup synchronized scrolling.
		final JScrollPane leftScroll = new JScrollPane(leftArea);
		final JScrollPane rightScroll = new JScrollPane(rightArea);
		syncScrolling(leftScroll, rightScroll);

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

		final JPanel centerPanel = new JPanel(new GridLayout(1, 2, 5, 5));
		centerPanel.add(leftScroll);
		centerPanel.add(rightScroll);

		// Buttons.
		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		if(!readOnly){
			final JButton acceptLeftBtn = new JButton("Revert to Previous");
			final JButton acceptRightBtn = new JButton("Keep Current");
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
			buttonPanel.add(acceptLeftBtn);
			buttonPanel.add(acceptRightBtn);
		}
		final JButton cancelBtn = new JButton(readOnly? "Close": "Cancel");
		cancelBtn.addActionListener(e -> dispose());
		buttonPanel.add(cancelBtn);

		setLayout(new BorderLayout(10, 10));
		add(headerPanel, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);

		setPreferredSize(new Dimension(900, 600));
		pack();
		setLocationRelativeTo(owner);
	}


	/**
	 * Opens the dialog in read-only comparison mode: no accept button is
	 * shown, the diff is highlighted, and the dialog is closed only with
	 * the Close button or the window close action.
	 */
	public static void showComparison(final Window owner, final String title,
		final FLEFRecord left, final FLEFRecord right){
		final RecordDiffDialog dialog = new RecordDiffDialog(owner, title, left, right, true);
		dialog.setVisible(true);
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
	 *                          Diff application
	 * ====================================================================== */

	/**
	 * Computes the line-based diff between the two texts and rebuilds
	 * both text areas so that they are visually aligned.
	 * <p>
	 * For each diff entry, one row is emitted on the left and one on the
	 * right. When a line exists only on one side, the other side
	 * receives an empty placeholder row at the same position, marked so
	 * that the renderer draws a diagonal hatch on it. In this way the
	 * two text areas always have the same number of rows, line N on the
	 * left and line N on the right refer to the same logical position,
	 * and a missing line is visually distinct from an empty line in the
	 * source.
	 */
	private void applyDiff(final String leftText, final String rightText){
		final String[] leftLines = splitLines(leftText);
		final String[] rightLines = splitLines(rightText);

		final List<DiffUtils.DiffEntry> diffs = DiffUtils.computeDiff(
			List.of(leftLines),
			List.of(rightLines));

		final List<StyledLine> leftStyled = new ArrayList<>();
		final List<StyledLine> rightStyled = new ArrayList<>();

		for(final DiffUtils.DiffEntry entry : diffs){
			switch(entry.operation()){
				case EQUAL -> {
					leftStyled.add(new StyledLine(nullToEmpty(entry.leftLine()), null, false));
					rightStyled.add(new StyledLine(nullToEmpty(entry.rightLine()), null, false));
				}
				case DELETE -> {
					leftStyled.add(new StyledLine(nullToEmpty(entry.leftLine()), COLOR_DELETE, false));
					rightStyled.add(new StyledLine("", COLOR_DELETE, true));
				}
				case INSERT -> {
					leftStyled.add(new StyledLine("", COLOR_INSERT, true));
					rightStyled.add(new StyledLine(nullToEmpty(entry.rightLine()), COLOR_INSERT, false));
				}
				case MODIFIED -> {
					leftStyled.add(new StyledLine(nullToEmpty(entry.leftLine()), COLOR_MODIFIED, false));
					rightStyled.add(new StyledLine(nullToEmpty(entry.rightLine()), COLOR_MODIFIED, false));
				}
			}
		}

		final String alignedLeftText = joinLines(leftStyled);
		final String alignedRightText = joinLines(rightStyled);
		leftArea.setText(alignedLeftText);
		rightArea.setText(alignedRightText);

		final DefaultHighlighter leftHighlighter = (DefaultHighlighter)leftArea.getHighlighter();
		final DefaultHighlighter rightHighlighter = (DefaultHighlighter)rightArea.getHighlighter();
		leftHighlighter.removeAllHighlights();
		rightHighlighter.removeAllHighlights();

		final List<Integer> leftOffsets = computeLineOffsets(alignedLeftText);
		final List<Integer> rightOffsets = computeLineOffsets(alignedRightText);

		// Collect the placeholder row indices, so the custom text area
		// can draw the hatch on them.
		final Set<Integer> leftPlaceholders = new HashSet<>();
		final Set<Integer> rightPlaceholders = new HashSet<>();

		for(int i = 0; i < leftStyled.size(); i++){
			final Color leftColor = leftStyled.get(i).color();
			if(leftColor != null)
				highlightLine(leftHighlighter, leftOffsets, alignedLeftText, i, leftColor);
			if(leftStyled.get(i).placeholder())
				leftPlaceholders.add(i);

			final Color rightColor = rightStyled.get(i).color();
			if(rightColor != null)
				highlightLine(rightHighlighter, rightOffsets, alignedRightText, i, rightColor);
			if(rightStyled.get(i).placeholder())
				rightPlaceholders.add(i);
		}

		leftArea.setPlaceholderLines(leftPlaceholders);
		rightArea.setPlaceholderLines(rightPlaceholders);

		// Both areas start showing the first line.
		leftArea.setCaretPosition(0);
		rightArea.setCaretPosition(0);
	}


	/* ======================================================================
	 *                          Alignment helpers
	 * ====================================================================== */

	/** A line of the aligned content, with its highlight color and placeholder flag. */
	private record StyledLine(String text, Color color, boolean placeholder){}

	/**
	 * Splits a text into lines. A single trailing empty element,
	 * produced by {@code StringUtils.split} when the text ends with a
	 * newline, is dropped: it is not a real line, only an artifact of
	 * the split.
	 */
	private static String[] splitLines(final String text){
		final String[] raw = StringUtils.split(text, StringUtils.LF, -1);
		if(raw.length > 0 && raw[raw.length - 1].isEmpty())
			return java.util.Arrays.copyOf(raw, raw.length - 1);
		return raw;
	}

	/**
	 * Joins the styled lines into a single string, separated by a
	 * newline and terminated by a newline. The trailing newline is
	 * required so that {@link #computeLineOffsets(String)} produces
	 * exactly {@code lines.size() + 1} offsets.
	 */
	private static String joinLines(final List<StyledLine> lines){
		final StringBuilder sb = new StringBuilder();
		for(int i = 0; i < lines.size(); i++){
			if(i > 0)
				sb.append('\n');
			sb.append(lines.get(i).text());
		}
		sb.append('\n');
		return sb.toString();
	}

	private static String nullToEmpty(final String s){
		return (s != null? s: "");
	}

	private void highlightLine(final DefaultHighlighter highlighter, final List<Integer> offsets,
		final String text, final int lineIdx, final Color color){
		final int start = offsets.get(lineIdx);
		final int end = (lineIdx + 1 < offsets.size())
			? offsets.get(lineIdx + 1)
			: text.length();
		addHighlight(highlighter, start, end, color);
	}

	private List<Integer> computeLineOffsets(final String text){
		final List<Integer> offsets = new ArrayList<>();
		offsets.add(0);
		int pos = 0;
		while(pos < text.length()){
			final int next = text.indexOf('\n', pos);
			if(next == -1)
				break;

			pos = next + 1;
			offsets.add(pos);
		}
		// If text doesn't end with newline, we add a final offset to the end.
		if(!text.isEmpty() && !text.endsWith(StringUtils.LF))
			offsets.add(text.length());
		return offsets;
	}

	private void addHighlight(final DefaultHighlighter highlighter, final int start, final int end,
		final Color color){
		try{
			highlighter.addHighlight(start, end, new DefaultHighlighter.DefaultHighlightPainter(color));
		}
		catch(final BadLocationException ignored){
		}
	}


	/* ======================================================================
	 *                          Custom text area
	 * ====================================================================== */

	/**
	 * A {@link JTextArea} that draws a diagonal hatch over the rows
	 * marked as placeholders, in the style of Beyond Compare.
	 * <p>
	 * Consecutive placeholder rows are merged into a single block, and the
	 * hatch is drawn across the whole block with a single continuous
	 * pattern. Without this grouping, each row would restart the pattern at
	 * the same X offset, producing a visible discontinuity at every row
	 * boundary.
	 * <p>
	 * The hatch is painted after the normal text rendering, is clipped to
	 * the exact bounds of each block, and does not affect the text content
	 * or the selection.
	 */
	private static final class DiffTextArea extends JTextArea{

		private final Set<Integer> placeholderLines = new HashSet<>();

		DiffTextArea(){
			super();
		}

		void setPlaceholderLines(final Set<Integer> lines){
			this.placeholderLines.clear();
			if(lines != null)
				this.placeholderLines.addAll(lines);
			repaint();
		}

		@Override
		protected void paintComponent(final Graphics g){
			super.paintComponent(g);

			if(placeholderLines.isEmpty())
				return;

			final Graphics2D g2 = (Graphics2D)g.create();
			try{
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g2.setColor(COLOR_HATCH);
				g2.setStroke(new BasicStroke(1f));

				final int insetsLeft = getInsets().left;
				final int visibleWidth = getWidth() - insetsLeft - getInsets().right;

				for(final int[] range : placeholderRanges()){
					drawHatchOverRange(g2, range[0], range[1], insetsLeft, visibleWidth);
				}
			}
			finally{
				g2.dispose();
			}
		}

		/**
		 * Groups the placeholder line indices into maximal ranges of
		 * consecutive lines. Each range is returned as
		 * {@code {startLine, endLineExclusive}}.
		 */
		private List<int[]> placeholderRanges(){
			final List<Integer> sorted = new ArrayList<>(placeholderLines);
			java.util.Collections.sort(sorted);

			final List<int[]> ranges = new ArrayList<>();
			int start = -1;
			int prev = -2;
			for(final int idx : sorted){
				if(idx == prev + 1){
					prev = idx;
					continue;
				}
				if(start >= 0)
					ranges.add(new int[]{start, prev + 1});
				start = idx;
				prev = idx;
			}
			if(start >= 0)
				ranges.add(new int[]{start, prev + 1});
			return ranges;
		}

		/**
		 * Draws a single continuous hatch over the block of lines from
		 * {@code startLine} (inclusive) to {@code endLineExclusive}
		 * (exclusive).
		 * <p>
		 * The block is measured with {@link #modelToView2D(int)}, so the
		 * hatch follows the exact geometry produced by the look-and-feel.
		 * The pattern is computed once over the whole block: the offset
		 * between consecutive diagonals is {@link #HATCH_SPACING}, and each
		 * diagonal spans the full block height, so the pattern is continuous
		 * across row boundaries.
		 */
		private void drawHatchOverRange(final Graphics2D g2, final int startLine,
			final int endLineExclusive, final int insetsLeft, final int visibleWidth){
			if(startLine < 0 || endLineExclusive > getLineCount())
				return;

			try{
				final int startOffset = getLineStartOffset(startLine);
				final int endOffset = getLineStartOffset(endLineExclusive - 1);

				final Rectangle2D startRect = modelToView2D(startOffset);
				final Rectangle2D endRect = modelToView2D(endOffset);
				if(startRect == null || endRect == null)
					return;

				final int y = (int)startRect.getY();
				final int yBottom = (int)(endRect.getY() + endRect.getHeight());
				final int h = yBottom - y;
				if(h <= 0)
					return;
				if(yBottom < 0 || y > getHeight())
					return;

				final java.awt.Shape oldClip = g2.getClip();
				g2.clipRect(insetsLeft, y, visibleWidth, h);

				// One single loop over the whole block. The diagonals go from
				// bottom-left to top-right: (x, y + h) -> (x + h, y). The X
				// start is offset by h to the left, so the top-left corner of
				// the block is fully covered.
				final int xStart = insetsLeft - h;
				final int xEnd = insetsLeft + visibleWidth + h;
				for(int x = xStart; x < xEnd; x += HATCH_SPACING)
					g2.drawLine(x, y + h, x + h, y);

				g2.setClip(oldClip);
			}
			catch(final BadLocationException ignored){
				// The model changed between the collection of the indices and
				// the paint. Skip the block silently.
			}
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

	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){
		}

		SwingUtilities.invokeLater(() -> {
			final JFrame frame = new JFrame("Test Record Diff");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			final FLEFRecord before = FLEFRecord.createChildWithTag("individual");
			before.setId("I1");
			before.addChild(FLEFRecord.createChildWithTagAndValue("name", "John Doe"));
			before.addChild(FLEFRecord.createChildWithTagAndValue("sex", "male"));
			before.addChild(FLEFRecord.createChildWithTagAndValue("birth", "2000-01-01"));
			before.addChild(FLEFRecord.createChildWithTagAndValue("ill", "2010-01-01"));

			final FLEFRecord after = FLEFRecord.createChildWithTag("individual");
			after.setId("I1");
			after.addChild(FLEFRecord.createChildWithTagAndValue("name", "John Doe"));
			after.addChild(FLEFRecord.createChildWithTagAndValue("sex", "male"));
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
