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
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.AdjustmentListener;
import java.util.ArrayList;
import java.util.List;


/**
 * Dialog that compares two FLEFRecord objects side‑by‑side with difference highlighting
 * and synchronized scrolling. Allows the user to choose which record to keep.
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


	private final JTextArea leftArea;
	private final JTextArea rightArea;

	private boolean accepted;
	// true = keep right (after), false = keep left (before)
	private boolean keepRight;


	/**
	 * Constructs a RecordDiffDialog comparing two records.
	 *
	 * @param owner     the parent window
	 * @param title     dialog title
	 * @param before    the "before" record
	 * @param after     the "after" record
	 */
	public RecordDiffDialog(final Window owner, final String title, final FLEFRecord before, final FLEFRecord after){
		super(owner, title, ModalityType.APPLICATION_MODAL);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		final FLEFWriter writer = FLEFWriter.createCompact();

		final String leftText = writer.writeToString(before);
		final String rightText = writer.writeToString(after);

		leftArea = createTextArea();
		rightArea = createTextArea();

		leftArea.setText(leftText);
		rightArea.setText(rightText);

		// Compute diff and apply highlights
		applyDiff(leftText, rightText);

		// Setup synchronized scrolling
		final JScrollPane leftScroll = new JScrollPane(leftArea);
		final JScrollPane rightScroll = new JScrollPane(rightArea);
		syncScrolling(leftScroll, rightScroll);

		// Create header labels showing record info
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

		// Layout
		final JPanel headerPanel = new JPanel(new GridLayout(1, 2, 5, 5));
		headerPanel.add(leftHeader);
		headerPanel.add(rightHeader);

		final JPanel centerPanel = new JPanel(new GridLayout(1, 2, 5, 5));
		centerPanel.add(leftScroll);
		centerPanel.add(rightScroll);

		// Buttons
		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		final JButton acceptLeftBtn = new JButton("Revert to Previous");
		final JButton acceptRightBtn = new JButton("Keep Current");
		final JButton cancelBtn = new JButton("Cancel");

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
		cancelBtn.addActionListener(e -> dispose());

		buttonPanel.add(acceptLeftBtn);
		buttonPanel.add(acceptRightBtn);
		buttonPanel.add(cancelBtn);

		setLayout(new BorderLayout(10, 10));
		add(headerPanel, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);


		setPreferredSize(new Dimension(900, 600));

		pack();

		setLocationRelativeTo(owner);
	}

	private JTextArea createTextArea(){
		final JTextArea area = new JTextArea();
		area.setEditable(false);
		area.setFont(new Font("Monospaced", Font.PLAIN, 12));
		area.setTabSize(4);

		// The default look-and-feel typically renders selected text in white,
		// assuming a dark selection background. That clashes with our pastel
		// diff highlights (DELETE/INSERT/MODIFIED), which are light colors:
		// when a highlight and the selection overlap, Swing may keep painting
		// the selection's text color even where the visible background ends
		// up being the lighter diff highlight instead of the selection color,
		// making the text unreadable. Forcing a dark, fixed selected-text
		// color and a light, pastel-consistent selection color keeps the
		// text legible regardless of which background layer is visible.
		area.setSelectionColor(COLOR_SELECTION);
		area.setSelectedTextColor(COLOR_SELECTED_TEXT);

		return area;
	}

	private void syncScrolling(final JScrollPane left, final JScrollPane right){
		final AdjustmentListener listener = e -> {
			if(e.getAdjustable() == left.getVerticalScrollBar())
				right.getVerticalScrollBar().setValue(e.getValue());
			else if(e.getAdjustable() == right.getVerticalScrollBar())
				left.getVerticalScrollBar().setValue(e.getValue());
		};
		left.getVerticalScrollBar().addAdjustmentListener(listener);
		right.getVerticalScrollBar().addAdjustmentListener(listener);
		// Also sync horizontal scrolling
		left.getHorizontalScrollBar().addAdjustmentListener(listener);
		right.getHorizontalScrollBar().addAdjustmentListener(listener);
	}

	/**
	 * Computes the line-based diff between the two texts and highlights each
	 * text area accordingly:
	 * - DELETE: line present only on the left, highlighted in pastel red.
	 * - INSERT: line present only on the right, highlighted in pastel green.
	 * - MODIFIED: line present on both sides but changed, highlighted in
	 *   pastel yellow on both the left and the right.
	 * EQUAL lines are left untouched.
	 */
	private void applyDiff(final String leftText, final String rightText){
		// Split into lines
		final String[] leftLines = StringUtils.split(leftText, StringUtils.LF, -1);
		final String[] rightLines = StringUtils.split(rightText,StringUtils.LF, -1);

		// Compute diff
		final List<DiffUtils.DiffEntry> diffs = DiffUtils.computeDiff(
			List.of(leftLines),
			List.of(rightLines)
		);

		final DefaultHighlighter leftHighlighter = (DefaultHighlighter)leftArea.getHighlighter();
		final DefaultHighlighter rightHighlighter = (DefaultHighlighter)rightArea.getHighlighter();

		// Clear any existing highlights first.
		leftHighlighter.removeAllHighlights();
		rightHighlighter.removeAllHighlights();

		int leftLineIdx = 0;
		int rightLineIdx = 0;
		final List<Integer> leftOffsets = computeLineOffsets(leftText);
		final List<Integer> rightOffsets = computeLineOffsets(rightText);

		// Iterate over diff entries
		for(final DiffUtils.DiffEntry entry : diffs){
			switch(entry.operation()){
				case EQUAL -> {
					// Both lines are equal; no highlight.
					leftLineIdx ++;
					rightLineIdx ++;
				}

				case DELETE -> {
					// Left has a line that right doesn't have.
					highlightLine(leftHighlighter, leftOffsets, leftText, leftLineIdx, COLOR_DELETE);
					leftLineIdx ++;
				}

				case INSERT -> {
					// Right has a line that left doesn't have.
					highlightLine(rightHighlighter, rightOffsets, rightText, rightLineIdx, COLOR_INSERT);
					rightLineIdx ++;
				}

				case MODIFIED -> {
					// The line exists on both sides but its content changed;
					// mark both the old and the new line in pastel yellow.
					highlightLine(leftHighlighter, leftOffsets, leftText, leftLineIdx, COLOR_MODIFIED);
					highlightLine(rightHighlighter, rightOffsets, rightText, rightLineIdx, COLOR_MODIFIED);
					leftLineIdx ++;
					rightLineIdx ++;
				}
			}
		}
	}

	/**
	 * Highlights the line at {@code lineIdx} within {@code text}, using the
	 * precomputed {@code offsets} to resolve the line's start/end position.
	 */
	private void highlightLine(final DefaultHighlighter highlighter, final List<Integer> offsets, final String text,
			final int lineIdx, final Color color){
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

	private void addHighlight(final DefaultHighlighter highlighter, final int start, final int end, final Color color){
		try{
			highlighter.addHighlight(start, end, new DefaultHighlighter.DefaultHighlightPainter(color));
		}
		catch(final BadLocationException ignored){}
	}


	/**
	 * Returns whether the user accepted (clicked one of the accept buttons).
	 */
	public boolean isAccepted(){
		return accepted;
	}

	/**
	 * Returns whether the user chose the right (after) record.
	 * If false, the left (before) record was chosen.
	 */
	public boolean isKeepRight(){
		return keepRight;
	}

	/**
	 * Returns the record selected by the user.
	 *
	 * @param before the "before" record
	 * @param after  the "after" record
	 * @return the chosen record, or null if the dialog was canceled
	 */
	public FLEFRecord getSelectedRecord(final FLEFRecord before, final FLEFRecord after){
		if(!accepted)
			return null;

		return (keepRight? after: before);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		SwingUtilities.invokeLater(() -> {
			final JFrame frame = new JFrame("Test Record Diff");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			// Create two dummy records
			final FLEFRecord before = FLEFRecord.createChildWithTag("individual");
			before.setId("I1");
			before.addChild(FLEFRecord.createChildWithTagAndValue("name", "John Doe"));
			before.addChild(FLEFRecord.createChildWithTagAndValue("sex", "male"));
			before.addChild(FLEFRecord.createChildWithTagAndValue("birth", "2000-01-01"));

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
			else{
				System.out.println("Cancelled");
			}
		});
	}

}
