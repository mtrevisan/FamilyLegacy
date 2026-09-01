package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.DiffUtils;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
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
import java.util.function.Function;


/**
 * Dialog that compares two FLEFRecord objects side‑by‑side with difference highlighting
 * and synchronised scrolling. Allows the user to choose which record to keep.
 */
public class RecordDiffDialog extends JDialog{

	private final JTextArea leftArea;
	private final JTextArea rightArea;
	private final JScrollPane leftScroll;
	private final JScrollPane rightScroll;

	private boolean accepted = false;
	private boolean keepRight = false; // true = keep right (after), false = keep left (before)

	/**
	 * Constructs a RecordDiffDialog comparing two records.
	 *
	 * @param owner     the parent window
	 * @param title     dialog title
	 * @param before    the "before" record
	 * @param after     the "after" record
	 * @param formatter a function that converts a FLEFRecord to a string representation
	 */
	public RecordDiffDialog(Window owner, String title,
		FLEFRecord before, FLEFRecord after,
		Function<FLEFRecord, String> formatter){
		super(owner, title, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		// If formatter is null, use toString() as fallback
		if(formatter == null){
			formatter = record -> record != null? record.toString(): "null";
		}

		String leftText = formatter.apply(before);
		String rightText = formatter.apply(after);

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

		// Create header labels showing record info
		String leftLabel = (before != null)? before.getTag() + " (" + before.getId() + ")": "null";
		String rightLabel = (after != null)? after.getTag() + " (" + after.getId() + ")": "null";

		JLabel leftHeader = new JLabel(leftLabel, SwingConstants.CENTER);
		JLabel rightHeader = new JLabel(rightLabel, SwingConstants.CENTER);
		leftHeader.setFont(leftHeader.getFont().deriveFont(Font.BOLD));
		rightHeader.setFont(rightHeader.getFont().deriveFont(Font.BOLD));

		// Layout
		JPanel headerPanel = new JPanel(new GridLayout(1, 2, 5, 5));
		headerPanel.add(leftHeader);
		headerPanel.add(rightHeader);

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
		add(headerPanel, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);

		setPreferredSize(new Dimension(900, 600));
		pack();
		setLocationRelativeTo(owner);
	}

	/**
	 * Convenience constructor using toString() as formatter.
	 */
	public RecordDiffDialog(Window owner, String title, FLEFRecord before, FLEFRecord after){
		this(owner, title, before, after, null);
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

		DefaultHighlighter leftHighlighter = (DefaultHighlighter)leftArea.getHighlighter();
		DefaultHighlighter rightHighlighter = (DefaultHighlighter)rightArea.getHighlighter();
		leftHighlighter.removeAllHighlights();
		rightHighlighter.removeAllHighlights();

		int leftLineIdx = 0;
		int rightLineIdx = 0;
		List<Integer> leftOffsets = computeLineOffsets(leftText);
		List<Integer> rightOffsets = computeLineOffsets(rightText);

		for(DiffUtils.DiffEntry entry : diffs){
			switch(entry.operation){
				case EQUAL -> {
					leftLineIdx++;
					rightLineIdx++;
				}
				case DELETE -> {
					int start = leftOffsets.get(leftLineIdx);
					int end = (leftLineIdx + 1 < leftOffsets.size())? leftOffsets.get(leftLineIdx + 1): leftText.length();
					addHighlight(leftHighlighter, start, end, Color.RED);
					leftLineIdx++;
				}
				case INSERT -> {
					int start = rightOffsets.get(rightLineIdx);
					int end = (rightLineIdx + 1 < rightOffsets.size())? rightOffsets.get(rightLineIdx + 1): rightText.length();
					addHighlight(rightHighlighter, start, end, Color.GREEN);
					rightLineIdx++;
				}
			}
		}
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

	// ------------------------------------------------------------------------
	// Result getters
	// ------------------------------------------------------------------------

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
	 * @return the chosen record, or null if the dialog was cancelled
	 */
	public FLEFRecord getSelectedRecord(FLEFRecord before, FLEFRecord after){
		if(!accepted) return null;
		return keepRight? after: before;
	}

	// ------------------------------------------------------------------------
	// Usage example
	// ------------------------------------------------------------------------

	public static void main(String[] args){
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Record Diff");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			// Create two dummy records
			FLEFRecord before = FLEFRecord.createChildWithTag("individual");
			before.setId("I1");
			before.addChild(FLEFRecord.createChildWithTagAndValue("name", "John Doe"));
			before.addChild(FLEFRecord.createChildWithTagAndValue("sex", "male"));
			before.addChild(FLEFRecord.createChildWithTagAndValue("birth", "2000-01-01"));

			FLEFRecord after = FLEFRecord.createChildWithTag("individual");
			after.setId("I1");
			after.addChild(FLEFRecord.createChildWithTagAndValue("name", "John Doe"));
			after.addChild(FLEFRecord.createChildWithTagAndValue("sex", "male"));
			after.addChild(FLEFRecord.createChildWithTagAndValue("birth", "2000-01-01"));
			after.addChild(FLEFRecord.createChildWithTagAndValue("death", "2024-12-31"));

			JButton showDiff = new JButton("Show Diff");
			showDiff.addActionListener(e -> {
				RecordDiffDialog dialog = new RecordDiffDialog(frame, "Compare Records", before, after);
				dialog.setVisible(true);
				if(dialog.isAccepted()){
					FLEFRecord selected = dialog.getSelectedRecord(before, after);
					System.out.println("Selected: " + selected);
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

}
