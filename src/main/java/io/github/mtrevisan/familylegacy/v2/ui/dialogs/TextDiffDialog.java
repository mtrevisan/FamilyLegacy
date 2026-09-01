package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.ui.helpers.DiffUtils;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultHighlighter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.List;


/**
 * Dialog that compares two text versions side‑by‑side with difference highlighting
 * and synchronised scrolling. Allows the user to choose which version to keep.
 */
public class TextDiffDialog extends JDialog{

	private final JTextArea leftArea;
	private final JTextArea rightArea;
	private final JScrollPane leftScroll;
	private final JScrollPane rightScroll;

	private boolean accepted = false;
	private boolean keepRight = false; // true = keep right (after), false = keep left (before)

	public TextDiffDialog(Window owner, String title, String leftText, String rightText){
		super(owner, title, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		leftArea = createTextArea();
		rightArea = createTextArea();

		leftArea.setText(leftText);
		rightArea.setText(rightText);

		// Compute diff and apply highlights
		applyDiff(leftText, rightText);

		// Setup synchronized scrolling
		leftScroll = new JScrollPane(leftArea);
		rightScroll = new JScrollPane(rightArea);
		syncScrolling(leftScroll, rightScroll);

		// Layout
		JPanel centerPanel = new JPanel(new GridLayout(1, 2, 5, 5));
		centerPanel.add(leftScroll);
		centerPanel.add(rightScroll);

		// Buttons
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton acceptLeftBtn = new JButton("Revert to Previous");
		JButton acceptRightBtn = new JButton("Keep Current");
		JButton cancelBtn = new JButton("Cancel");

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
		add(centerPanel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);

		// Set a reasonable size
		setPreferredSize(new Dimension(900, 600));
		pack();
		setLocationRelativeTo(owner);
	}

	private JTextArea createTextArea(){
		JTextArea area = new JTextArea();
		area.setEditable(false);
		area.setFont(new Font("Monospaced", Font.PLAIN, 12));
		area.setTabSize(4);
		return area;
	}

	private void syncScrolling(JScrollPane left, JScrollPane right){
		AdjustmentListener listener = new AdjustmentListener(){
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e){
				if(e.getAdjustable() == left.getVerticalScrollBar()){
					right.getVerticalScrollBar().setValue(e.getValue());
				}
				else if(e.getAdjustable() == right.getVerticalScrollBar()){
					left.getVerticalScrollBar().setValue(e.getValue());
				}
			}
		};
		left.getVerticalScrollBar().addAdjustmentListener(listener);
		right.getVerticalScrollBar().addAdjustmentListener(listener);
		// Also sync horizontal scrolling
		left.getHorizontalScrollBar().addAdjustmentListener(listener);
		right.getHorizontalScrollBar().addAdjustmentListener(listener);
	}

	private void applyDiff(String leftText, String rightText){
		// Split into lines
		String[] leftLines = leftText.split("\n", -1);
		String[] rightLines = rightText.split("\n", -1);

		// Compute diff
		List<DiffUtils.DiffEntry> diffs = DiffUtils.computeDiff(
			List.of(leftLines),
			List.of(rightLines)
		);

		// Build the new text for each side (with placeholders for alignment)
		// We'll also collect line positions for highlighting.
		// Since we want to keep the original content, we'll just highlight the lines.
		// We'll use a Highlighter to mark the lines that are different.

		// First, we need to know which line numbers differ.
		// We'll iterate over the diff entries and for each line index in left/right,
		// we'll add a highlight.

		// We'll use the DefaultHighlighter and set a custom HighlightPainter.
		// We'll highlight the entire line range.

		// For simplicity, we'll just use the first line of each side and highlight the lines
		// that are not equal. Since we have the diff entries, we can apply highlights.

		// We'll create a custom Highlighter for each text area.
		// We'll define colors:
		// - DELETE (only in left): red
		// - INSERT (only in right): green
		// - MODIFIED (both present but different): yellow on both sides

		DefaultHighlighter leftHighlighter = (DefaultHighlighter)leftArea.getHighlighter();
		DefaultHighlighter rightHighlighter = (DefaultHighlighter)rightArea.getHighlighter();

		// We'll clear any existing highlights first.
		leftHighlighter.removeAllHighlights();
		rightHighlighter.removeAllHighlights();

		// We need to map line numbers to offsets.
		// We'll compute the offset for each line start and end.
		// We'll do this by iterating over the lines and using a position counter.

		// We'll compute offsets for left and right.
		int leftOffset = 0;
		int rightOffset = 0;
		// We'll also need the line boundaries, so we'll store them.
		// We'll use the diff entries to iterate.

		// We'll create a list of line ranges for each side.
		// For each diff entry, we'll highlight the corresponding line(s).

		// We'll use a simple algorithm: for each diff entry, we highlight the lines in
		// the left (if DELETE) and right (if INSERT) and both (if EQUAL but with different content? Actually EQUAL means same content, so no highlight).

		// For MODIFIED lines (where both sides have lines but content differs), we need to
		// detect them. The diff algorithm treats them as DELETE + INSERT. So we can pair
		// consecutive DELETE and INSERT to represent a modification. We'll handle that.

		// We'll just highlight each line that is not equal. For DELETE, highlight left.
		// For INSERT, highlight right.
		// For MODIFIED, we highlight both (with a different color).

		// To simplify, we'll use the standard Myers diff output: it gives a sequence of
		// DELETE, INSERT, EQUAL. We'll process them sequentially.

		// We'll store the line index in left and right.
		int leftLineIdx = 0;
		int rightLineIdx = 0;
		// We'll precompute the offsets for each line.
		List<Integer> leftOffsets = computeLineOffsets(leftText);
		List<Integer> rightOffsets = computeLineOffsets(rightText);

		// Iterate over diff entries
		for(DiffUtils.DiffEntry entry : diffs){
			switch(entry.operation){
				case EQUAL -> {
					// Both lines are equal; no highlight.
					leftLineIdx++;
					rightLineIdx++;
				}
				case DELETE -> {
					// Left has a line that right doesn't have.
					// Highlight this line in left (red)
					int start = leftOffsets.get(leftLineIdx);
					int end = (leftLineIdx + 1 < leftOffsets.size())? leftOffsets.get(leftLineIdx + 1): leftText.length();
					addHighlight(leftHighlighter, start, end, Color.RED);
					leftLineIdx++;
				}
				case INSERT -> {
					// Right has a line that left doesn't have.
					// Highlight this line in right (green)
					int start = rightOffsets.get(rightLineIdx);
					int end = (rightLineIdx + 1 < rightOffsets.size())? rightOffsets.get(rightLineIdx + 1): rightText.length();
					addHighlight(rightHighlighter, start, end, Color.GREEN);
					rightLineIdx++;
				}
			}
		}

		// For modified lines (where both sides have a line but they differ), we could
		// detect if after a DELETE there is an INSERT. We'll pair them.
		// But for simplicity, we'll just use the above approach; it will show the deleted
		// line in red on left and inserted line in green on right.
		// If we want to show them as modified (yellow), we need to pair them.
		// We'll implement a simple pairing: if a DELETE is immediately followed by an INSERT
		// of the same line count, we treat it as a modification and use yellow for both.

		// We'll leave it as red/green for simplicity. The user can see the difference.
	}

	private List<Integer> computeLineOffsets(String text){
		List<Integer> offsets = new java.util.ArrayList<>();
		offsets.add(0);
		int pos = 0;
		while(pos < text.length()){
			int next = text.indexOf('\n', pos);
			if(next == -1) break;
			pos = next + 1;
			offsets.add(pos);
		}
		// If text doesn't end with newline, we add a final offset to the end.
		if(text.length() > 0 && !text.endsWith("\n")){
			offsets.add(text.length());
		}
		return offsets;
	}

	private void addHighlight(DefaultHighlighter highlighter, int start, int end, Color color){
		try{
			highlighter.addHighlight(start, end, new DefaultHighlighter.DefaultHighlightPainter(color));
		}
		catch(javax.swing.text.BadLocationException e){
			// ignore
		}
	}

	public boolean isAccepted(){
		return accepted;
	}

	public boolean isKeepRight(){
		return keepRight;
	}

	public String getSelectedText(){
		return keepRight? rightArea.getText(): leftArea.getText();
	}

	// ------------------------------------------------------------------------
	// Usage example
	// ------------------------------------------------------------------------

	public static void main(String[] args){
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Diff");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			JButton showDiff = new JButton("Show Diff");
			showDiff.addActionListener(e -> {
				String before = "Line 1\nLine 2\nLine 3\nLine 4\nLine 5";
				String after = "Line 1\nLine 2 modified\nLine 3\nLine 4 new\nLine 5\nLine 6";
				TextDiffDialog dialog = new TextDiffDialog(frame, "Compare Changes", before, after);
				dialog.setVisible(true);
				if(dialog.isAccepted()){
					String result = dialog.getSelectedText();
					System.out.println("Selected: " + (dialog.isKeepRight()? "AFTER": "BEFORE"));
					System.out.println(result);
				}
				else{
					System.out.println("Cancelled");
				}
			});

			frame.add(showDiff);
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

	/*
	String beforeText = getStateBeforeSave();
String afterText = getStateAfterSave();

TextDiffDialog dialog = new TextDiffDialog(mainFrame, "Undo Changes", beforeText, afterText);
dialog.setVisible(true);

if (dialog.isAccepted()) {
    String finalText = dialog.getSelectedText();
    if (dialog.isKeepRight()) {
        // Keep the after state (current)
    } else {
        // Revert to before state
        setState(finalText);
    }
}
	 */

}
