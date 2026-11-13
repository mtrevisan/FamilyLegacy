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
package io.github.mtrevisan.familylegacy.ui.tools.maintenance.merge;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.merger.manual.MergeDecision;
import io.github.mtrevisan.familylegacy.io.merger.manual.MergeField;
import io.github.mtrevisan.familylegacy.io.merger.manual.MergeGrammar;
import io.github.mtrevisan.familylegacy.io.merger.manual.MergePlan;
import io.github.mtrevisan.familylegacy.io.merger.manual.TagSuggester;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.components.comparison.DiffKind;
import io.github.mtrevisan.familylegacy.ui.components.comparison.DiffPainters;
import io.github.mtrevisan.familylegacy.ui.components.comparison.RecordAlignment;
import io.github.mtrevisan.familylegacy.ui.components.comparison.RecordAlignment.Rows;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;
import static io.github.mtrevisan.familylegacy.ui.components.comparison.RecordAlignment.alignBlocks;
import static io.github.mtrevisan.familylegacy.ui.components.comparison.RecordAlignment.displayOrder;
import static io.github.mtrevisan.familylegacy.ui.components.comparison.RecordAlignment.isDelimiter;
import static io.github.mtrevisan.familylegacy.ui.components.comparison.RecordAlignment.linesOf;
import static io.github.mtrevisan.familylegacy.ui.components.comparison.RecordAlignment.pairBlocks;
import static io.github.mtrevisan.familylegacy.ui.components.comparison.RecordAlignment.similarity;
import static io.github.mtrevisan.familylegacy.ui.components.comparison.RecordAlignment.sideBySide;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * The review of a merge, of the <b>whole record</b>, before anything is written, in the manner of a three-way file merge tool: the
 * <b>source</b> (to be deleted) on the left, the <b>result</b> in the middle, the <b>target</b> (to be kept) on the right.
 * <p>
 * The record is a sequence of <em>sections</em>, each the same place in the three columns, coloured by where it comes from:
 * <ul>
 *   <li><b>from the source</b>: only the source has it;</li>
 *   <li><b>from the target</b>: only the target has it;</li>
 *   <li><b>conflict</b>: both have something there and it is not the same, for example two different names, or two values of a
 *       field that holds one;</li>
 *   <li>the same in both: white, and hidden if only the differences are wanted.</li>
 * </ul>
 * The columns are aligned a sub-record at a time: a block of one column, from the line that opens it to the one that closes it,
 * faces <b>at most one</b> block of another column (and so is never interrupted by a second one), and where a column has no line it has
 * an empty one. The colours are <b>per line</b>, as in a file comparison: inside a section only the lines that differ are coloured, not the ones
 * that are the same, nor the lines that only open or close a group (the lines <code>name &#123;</code> and <code>&#125;</code>). The middle column is what the kept record
 * will have, as FLEF text. It starts from the choices already made and the suggestions of the rules, and can be edited freely, or
 * filled from either side with the arrows, from both, or emptied.
 * <p>
 * The buttons (and the keys <em>F7</em>/<em>F8</em>, with Shift for the conflicts only) go to the previous and the next
 * difference or conflict, and the strip beside the scroll bar shows where they all are. A conflict stays red until it is
 * decided, by taking a side or by editing it.
 * <p>
 * A field left as it was proposed keeps following the automatic rules of the merge (the citations and notes of a statement that the
 * two records make alike are united, for example); a field with a section that was changed is written as it is in the middle
 * column. On <em>Merge</em> the middle column is read, and the whole file with the result in place of the target is validated: if it
 * does not, the dialog stays open and says why. The decisions are written into the {@link MergePlan} that was given, which the
 * caller then applies; nothing in the file changes here.
 */
public final class MergeRecordReviewDialog extends JDialog{

	private static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 11);

	private static final Color COLOR_FROM_SOURCE = DiffPainters.COLOR_FROM_SOURCE;
	private static final Color COLOR_FROM_TARGET = DiffPainters.COLOR_FROM_TARGET;
	private static final Color COLOR_CONFLICT = DiffPainters.COLOR_CONFLICT;
	/** A different record where the other side has another one: stronger than a different text. */
	private static final Color COLOR_REFERENCE = DiffPainters.COLOR_REFERENCE;
	private static final Color COLOR_EDITED = DiffPainters.COLOR_EDITED;
	private static final Color COLOR_ABSENT = DiffPainters.COLOR_ABSENT;
	private static final Color COLOR_BORDER = new Color(0xD0D0D0);
	private static final Color COLOR_CURRENT = new Color(0x1565C0);
	private static final Color COLOR_MUTED = DiffPainters.COLOR_MUTED;
	private static final Color COLOR_SAME_HEADER = new Color(0xEEEEEE);

	private static final int BUTTON_COLUMN_WIDTH = 22;
	private static final int OVERVIEW_WIDTH = 14;
	private static final int MIN_ROWS = 4;
	private static final int BUTTON_HEIGHT = 20;
	/** The columns of the buttons beside the texts: the ones of the section, then the ones of its blocks. */
	private static final int LEFT_GUTTER_COLUMNS = 2;
	private static final int RIGHT_GUTTER_COLUMNS = 3;
	private static final int MAX_ERRORS_SHOWN = 10;
	/** The spaces of a level of indentation, for the keys of the editor of the result. */
	private static final int INDENT_SIZE = 2;
	/** How long after the last change the result is checked, in milliseconds. */
	private static final int VALIDITY_DELAY = 600;
	private static final Color COLOR_VALID = new Color(0x2E7D32);
	private static final Color COLOR_INVALID = new Color(0xC62828);

	private static final String COLUMNS = "[grow,fill,sg col][" + (LEFT_GUTTER_COLUMNS * BUTTON_COLUMN_WIDTH)
		+ "!][grow,fill,sg col][" + (RIGHT_GUTTER_COLUMNS * BUTTON_COLUMN_WIDTH) + "!][grow,fill,sg col]";


	/**
	 * The text without its empty lines, each line ended by a line feed: what a column says, apart from the empty lines that keep the
	 * three columns aligned.
	 */
	private static String logical(final String text){
		final StringBuilder sb = new StringBuilder();
		if(text != null)
			for(final String line : StringUtils.split(text, '\n'))
				if(!line.isBlank())
					sb.append(line).append(StringUtils.LF);
		return sb.toString();
	}

	/** The lines of the text that are not empty. */
	private static String[] logicalLines(final String text){
		return linesOf(logical(text));
	}

	/** The buttons beside the texts: the ones of the section in the first column, the ones of its blocks, each at its first line, after. */
	private static final class Gutter extends JPanel{

		private final Dimension size;


		Gutter(final int columns, final int minimumHeight){
			super(null);

			setOpaque(false);
			size = new Dimension(columns * BUTTON_COLUMN_WIDTH, minimumHeight);
		}

		@Override
		public Dimension getPreferredSize(){
			return size;
		}

		@Override
		public Dimension getMinimumSize(){
			return size;
		}

		void place(final JButton button, final int column, final int y, final int height){
			button.setBounds(column * BUTTON_COLUMN_WIDTH, y, BUTTON_COLUMN_WIDTH, height);
			add(button);
		}

	}

	/** The three columns, one row for each line, and what each line of the sides is. */
	private record Placed(List<String> source, List<String> result, List<String> target, List<DiffKind> sourceKind, List<DiffKind> targetKind,
			List<int[]> spans){

		String text(final List<String> column){
			final StringBuilder sb = new StringBuilder();
			for(final String line : column){
				if(line != null)
					sb.append(line);
				sb.append(StringUtils.LF);
			}
			return sb.toString();
		}

	}

	/** The blocks of the lines: each is a line, or a line that opens a group, up to the one that closes it. */
	private static List<List<String>> blocksOf(final String[] lines){
		final List<List<String>> blocks = new ArrayList<>();
		List<String> block = new ArrayList<>();
		int depth = 0;
		for(final String line : lines){
			block.add(line);
			final String stripped = line.strip();
			if(stripped.equals("}"))
				depth = Math.max(0, depth - 1);
			else if(stripped.endsWith("{"))
				depth ++;
			if(depth == 0){
				blocks.add(block);
				block = new ArrayList<>();
			}
		}
		if(!block.isEmpty())
			blocks.add(block);
		return blocks;
	}

	/** How many lines of the source are a reference to a record that is not the one the target has there: for the tests. */
	static int referenceConflicts(final FLEFRecord source, final FLEFRecord target, final FLEFModel model){
		final Rows rows = new Rows();
		alignBlocks(source, target, 0, rows, model);
		int count = 0;
		for(final DiffKind kind : rows.sourceKind)
			if(kind == DiffKind.REFERENCE)
				count ++;
		return count;
	}

	/** The three columns of a section as the dialog shows them (source, result, target), a line for each row, or {@code null} where it is empty. */
	static List<List<String>> alignedColumns(final FLEFRecord source, final FLEFRecord target, final String[] resultLines,
			final FLEFModel model){
		final Rows rows = new Rows();
		if(source != null && target != null)
			alignBlocks(source, target, 0, rows, model);
		else
			sideBySide(source, target, 0, rows);
		final Placed placed = place(rows, source, target, "individual", model, resultLines);
		return List.of(placed.source(), placed.result(), placed.target());
	}

	/** The block of lines as a record, or {@code null} if the text is not one (it is being written). */
	private static FLEFRecord parseBlock(final String recordTag, final List<String> block){
		try{
			final List<FLEFRecord> records = MergeFragments.parse(recordTag, String.join(StringUtils.LF, block));
			return (records.size() == 1? records.get(0): null);
		}
		catch(final IOException | RuntimeException ignored){
			return null;
		}
	}

	/**
	 * The lines of a block of the result in the order the sides are shown in, if it can be read and is like one of them: what is
	 * written below the sides has the sub-records in the same order as the ones above, even if it is not what was written.
	 */
	private static List<String> inDisplayOrder(final List<String> block, final FLEFRecord sourceRecord, final FLEFRecord targetRecord,
			final String recordTag, final FLEFModel model){
		final FLEFRecord record = parseBlock(recordTag, block);
		if(record == null)
			return block;

		final int againstTarget = (targetRecord != null? Math.max(0, similarity(record, targetRecord, model)): 0);
		final int againstSource = (sourceRecord != null? Math.max(0, similarity(record, sourceRecord, model)): 0);
		if(againstTarget == 0 && againstSource == 0)
			return block;

		final Rows aligned = new Rows();
		alignBlocks(record, (againstTarget >= againstSource? targetRecord: sourceRecord), 0, aligned, model);
		final List<String> lines = new ArrayList<>();
		for(final String line : aligned.source)
			if(line != null)
				lines.add(line);
		return lines;
	}

	/**
	 * Puts the result against the rows of the sides, a block at a time, so that a block of the result faces at most one block of each
	 * side, at every depth, as the sides do between them:
	 * <ul>
	 *   <li>a block that is the same as the one of the target, or of the source, goes line by line against it;</li>
	 *   <li>any other, if it can be read, is aligned with the one of the two it is most like, as the sides are aligned (the lines
	 *       that it has and the other has not are on rows of their own, inserted in the columns of the sides, that stay as they are);</li>
	 *   <li>the one that cannot, and a second one on the same side, go below all the rows of the sides, facing empty lines.</li>
	 * </ul>
	 */
	private static Placed place(final Rows rows, final FLEFRecord sourceRecord, final FLEFRecord targetRecord, final String recordTag,
			final FLEFModel model, final String[] resultLines){
		final List<Integer> sourceRows = new ArrayList<>();
		final List<String> sourceBlock = new ArrayList<>();
		final List<Integer> targetRows = new ArrayList<>();
		final List<String> targetBlock = new ArrayList<>();
		for(int row = 0; row < rows.size(); row ++){
			if(rows.source.get(row) != null){
				sourceRows.add(row);
				sourceBlock.add(rows.source.get(row));
			}
			if(rows.target.get(row) != null){
				targetRows.add(row);
				targetBlock.add(rows.target.get(row));
			}
		}

		final List<String> result = new ArrayList<>();
		for(int row = 0; row < rows.size(); row ++)
			result.add(null);
		//the lines of the result that go on a row of their own, after the row of the key (-1 is before the first)
		final Map<Integer, List<String>> inserted = new HashMap<>();
		final List<String> below = new ArrayList<>();
		boolean sourceUsed = false;
		boolean targetUsed = false;
		int last = -1;
		for(final List<String> block : blocksOf(resultLines)){
			final boolean targetFree = (!targetUsed && !targetRows.isEmpty() && targetRows.get(0) > last);
			final boolean sourceFree = (!sourceUsed && !sourceRows.isEmpty() && sourceRows.get(0) > last);
			if(targetFree && block.equals(targetBlock)){
				for(int k = 0; k < block.size(); k ++)
					result.set(targetRows.get(k), block.get(k));
				targetUsed = true;
				last = targetRows.get(targetRows.size() - 1);
				continue;
			}
			if(sourceFree && block.equals(sourceBlock)){
				for(int k = 0; k < block.size(); k ++)
					result.set(sourceRows.get(k), block.get(k));
				sourceUsed = true;
				last = sourceRows.get(sourceRows.size() - 1);
				continue;
			}

			//what was written: it faces the side it is most like
			final FLEFRecord record = ((targetFree || sourceFree)? parseBlock(recordTag, block): null);
			final int againstTarget = (record != null && targetFree? Math.max(0, similarity(record, targetRecord, model)): 0);
			final int againstSource = (record != null && sourceFree? Math.max(0, similarity(record, sourceRecord, model)): 0);
			if(againstTarget == 0 && againstSource == 0){
				below.addAll(inDisplayOrder(block, sourceRecord, targetRecord, recordTag, model));
				continue;
			}

			final boolean useTarget = (againstTarget >= againstSource);
			final List<Integer> baseRows = (useTarget? targetRows: sourceRows);
			final Rows aligned = new Rows();
			alignBlocks(record, (useTarget? targetRecord: sourceRecord), 0, aligned, model);
			int base = 0;
			int anchor = baseRows.get(0) - 1;
			for(int k = 0; k < aligned.size(); k ++){
				final String resultLine = aligned.source.get(k);
				if(aligned.target.get(k) != null){
					anchor = baseRows.get(base ++);
					if(resultLine != null)
						result.set(anchor, resultLine);
				}
				else if(resultLine != null)
					inserted.computeIfAbsent(anchor, key -> new ArrayList<>()).add(resultLine);
			}
			if(useTarget)
				targetUsed = true;
			else
				sourceUsed = true;
			last = baseRows.get(baseRows.size() - 1);
		}

		final List<String> source = new ArrayList<>();
		final List<String> target = new ArrayList<>();
		final List<String> column = new ArrayList<>();
		final List<DiffKind> sourceKind = new ArrayList<>();
		final List<DiffKind> targetKind = new ArrayList<>();
		final int[] finalRow = new int[rows.size()];
		for(int row = -1; row < rows.size(); row ++){
			if(row >= 0){
				finalRow[row] = source.size();
				source.add(rows.source.get(row));
				target.add(rows.target.get(row));
				column.add(result.get(row));
				sourceKind.add(rows.sourceKind.get(row));
				targetKind.add(rows.targetKind.get(row));
			}
			for(final String line : inserted.getOrDefault(row, List.of())){
				source.add(null);
				target.add(null);
				column.add(line);
				sourceKind.add(DiffKind.FILLER);
				targetKind.add(DiffKind.FILLER);
			}
		}
		for(final String line : below){
			source.add(null);
			target.add(null);
			column.add(line);
			sourceKind.add(DiffKind.FILLER);
			targetKind.add(DiffKind.FILLER);
		}
		//the rows that were inserted move the blocks down
		final List<int[]> spans = new ArrayList<>();
		for(final int[] span : rows.spans)
			spans.add(new int[]{finalRow[span[0]], finalRow[span[1]], span[2]});
		return new Placed(source, column, target, sourceKind, targetKind, spans);
	}

	/** How the application names a record: through the handler of its kind. */
	private String displayNameOf(final FLEFRecord record){
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(record.getTag());
		return (handler != null? handler.getDisplayText(record, model): null);
	}

	private String nameOfReference(final String line){
		return DiffPainters.nameOfReference(line, model, this::displayNameOf);
	}

	private FLEFRecord referencedBy(final String line){
		return DiffPainters.referencedBy(line, model);
	}

	private TagSuggester suggesterFor(final String fieldTag){
		return suggesters.computeIfAbsent(fieldTag, tag -> new TagSuggester(plan.target().getTag(), tag));
	}

	/** Opens the editor of the record that the line under a double click cites, and paints the names again if it was changed. */
	private void openReferenceAt(final JTextArea area, final MouseEvent event){
		if(event.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(event))
			return;

		final int offset = area.viewToModel2D(event.getPoint());
		if(offset < 0)
			return;

		try{
			final int line = area.getLineOfOffset(offset);
			final String text = area.getText(area.getLineStartOffset(line), area.getLineEndOffset(line) - area.getLineStartOffset(line));
			final FLEFRecord referenced = referencedBy(text);
			final RecordTypeHandler<?> handler = (referenced != null? HandlerRegistry.getHandler(referenced.getTag()): null);
			if(handler == null)
				return;

			final RecordDialog dialog = handler.createEditDialog(this, model, referenced);
			dialog.setVisible(true);
			if(dialog.isSaved())
				for(final Section section : sections)
					section.realign();
		}
		catch(final BadLocationException ignored){
			//the line is gone: nothing to open
		}
	}

	private enum Status{
		COLLISION("dialog.merge.review.status.collision"),
		SOURCE_ONLY("dialog.merge.review.status.source_only"),
		TARGET_ONLY("dialog.merge.review.status.target_only"),
		SAME("dialog.merge.review.status.same");


		private final String key;


		Status(final String key){
			this.key = key;
		}

		String label(){
			return I18N.t(key);
		}
	}


	/** A field of the record: a header and its sections. */
	private final class FieldRow{

		private final MergeField field;
		private final Status status;
		private final JLabel header;
		private final List<Section> sections = new ArrayList<>();


		FieldRow(final MergeField field, final Status status){
			this.field = field;
			this.status = status;

			header = new JLabel(field.tag() + "  —  " + status.label());
			header.setFont(header.getFont().deriveFont(Font.BOLD));
			header.setOpaque(true);
			header.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
			header.setBackground(status == Status.SAME? COLOR_SAME_HEADER: new Color(0xE3E8F0));
			if(status == Status.SAME)
				header.setForeground(COLOR_MUTED);
		}

		/** What the result of the field starts as, the sections one after the other. */
		String initialText(){
			return join(sections.stream().map(section -> section.initialLogical).toList());
		}

		/** What the result of the field is now. */
		String resultText(){
			return join(sections.stream().map(section -> section.resultArea.getText()).toList());
		}

		boolean isEdited(){
			return !resultText().equals(initialText());
		}

		private static String join(final List<String> texts){
			final StringBuilder sb = new StringBuilder();
			for(final String text : texts)
				sb.append(logical(text));
			return sb.toString();
		}

	}


	/** The same place in the three columns. */
	private final class Section{

		private final FieldRow row;
		private final DiffKind kind;
		/** The text the result starts with: a field whose sections all still have theirs is left to the rules of the merge. */
		private final String initialText;
		private final JTextArea sourceArea;
		private final JTextArea resultArea;
		private final JTextArea targetArea;
		private final Gutter leftGutter;
		private final Gutter rightGutter;
		/** The buttons of the blocks that differ, rebuilt each time the columns are aligned. */
		private final List<JButton> blockButtons = new ArrayList<>();
		/** The three columns as they are shown now, to act on a block. */
		private Placed placed;
		/** The text of the result when the window opened, with the empty lines of the alignment: what the reset goes back to. */
		private String initialShown;
		/** Whether the user decided it, taking a side or editing it: only the conflicts care. */
		private boolean resolved;
		/** The text of the lines of the two sides that are a conflict, and of the ones only one side has. */
		private final Set<String> conflictLines = new HashSet<>();
		/** The lines of the conflicts that are a reference to a record that is not the same as the other side's. */
		private final Set<String> referenceLines = new HashSet<>();
		private final Set<String> sourceOnlyLines = new HashSet<>();
		private final Set<String> targetOnlyLines = new HashSet<>();
		private final String[] initialLines;
		/** What the result starts as, in the order it is shown: a field whose sections all still have it is left to the rules. */
		private String initialLogical;
		/** The text of each side, as it is (the columns show it with empty lines in it, to be aligned), and its lines. */
		private final String sourceText;
		private final String targetText;
		private final String[] sourceLines;
		/** The two sides, aligned one against the other: a block faces at most one of the other. */
		private final Rows rows = new Rows();
		private final FLEFRecord sourceRecord;
		private final FLEFRecord targetRecord;


		Section(final FieldRow row, final DiffKind kind, final FLEFRecord source, final FLEFRecord target, final String initialText){
			this.row = row;
			this.kind = kind;
			this.initialText = initialText;
			sourceRecord = source;
			targetRecord = target;

			sourceText = (source != null? MergeFragments.serialize(source): StringUtils.EMPTY);
			targetText = (target != null? MergeFragments.serialize(target): StringUtils.EMPTY);
			sourceArea = createArea(sourceText, false);
			resultArea = createArea(initialText, true);
			targetArea = createArea(targetText, false);
			resultArea.setToolTipText(I18N.t("dialog.merge.review.result.tooltip"));
			//the tags the field allows, as the user types them, and the editor of a record that a line cites with a double click
			final TagSuggester suggester = suggesterFor(row.field.tag());
			new AutocompleteController(resultArea, MergeRecordReviewDialog.this, suggester, INDENT_SIZE, JTextArea::getText, area -> 0);
			for(final JTextArea area : List.of(sourceArea, resultArea, targetArea))
				area.addMouseListener(new MouseAdapter(){
					@Override
					public void mouseClicked(final MouseEvent e){
						openReferenceAt(area, e);
					}
				});
			//the colour is of the line, not of the section: only the side that has nothing is coloured as a whole
			sourceArea.setBackground(source != null? Color.WHITE: COLOR_ABSENT);
			targetArea.setBackground(target != null? Color.WHITE: COLOR_ABSENT);
			initialLines = linesOf(initialText);
			initialLogical = logical(initialText);

			sourceLines = linesOf(sourceText);
			if(source != null && target != null)
				alignBlocks(source, target, 0, rows, model);
			else
				sideBySide(source, target, 0, rows);
			for(int line = 0; line < rows.size(); line ++){
				final DiffKind sourceKind = rows.sourceKind.get(line);
				if(sourceKind == DiffKind.CONFLICT || sourceKind == DiffKind.REFERENCE){
					conflictLines.add(rows.source.get(line));
					if(sourceKind == DiffKind.REFERENCE)
						referenceLines.add(rows.source.get(line));
				}
				else if(sourceKind == DiffKind.FROM_SOURCE)
					sourceOnlyLines.add(rows.source.get(line));
				final DiffKind targetKind = rows.targetKind.get(line);
				if(targetKind == DiffKind.CONFLICT || targetKind == DiffKind.REFERENCE){
					conflictLines.add(rows.target.get(line));
					if(targetKind == DiffKind.REFERENCE)
						referenceLines.add(rows.target.get(line));
				}
				else if(targetKind == DiffKind.FROM_TARGET)
					targetOnlyLines.add(rows.target.get(line));
			}
			realign();
			//the order of the screen is not the one of the file: what is shown at first is what the result starts as
			initialLogical = logical(resultArea.getText());
			initialShown = resultArea.getText();

			final JButton useSource = createButton("→", I18N.t("tooltip.merge.use_source"));
			useSource.setEnabled(source != null);
			useSource.addActionListener(e -> take(sourceText));
			final JButton both = createButton("⊕", I18N.t("tooltip.merge.use_both"));
			both.setEnabled(source != null && target != null && kind == DiffKind.CONFLICT);
			both.addActionListener(e -> take(targetText + sourceText));
			final JButton useTarget = createButton("←", I18N.t("tooltip.merge.use_target"));
			useTarget.setEnabled(target != null);
			useTarget.addActionListener(e -> take(targetText));
			final JButton clear = createButton("✕", I18N.t("tooltip.merge.remove_row"));
			clear.addActionListener(e -> take(StringUtils.EMPTY));

			final JButton reset = createButton("↺", I18N.t("tooltip.merge.reset"));
			reset.addActionListener(e -> reset());

			leftGutter = new Gutter(LEFT_GUTTER_COLUMNS, 3 * BUTTON_HEIGHT);
			leftGutter.place(useSource, 0, 0, BUTTON_HEIGHT);
			leftGutter.place(both, 0, BUTTON_HEIGHT, BUTTON_HEIGHT);
			leftGutter.place(reset, 0, 2 * BUTTON_HEIGHT, BUTTON_HEIGHT);
			rightGutter = new Gutter(RIGHT_GUTTER_COLUMNS, 2 * BUTTON_HEIGHT);
			rightGutter.place(useTarget, 0, 0, BUTTON_HEIGHT);
			rightGutter.place(clear, 0, BUTTON_HEIGHT, BUTTON_HEIGHT);
			rebuildBlockButtons();

			resultArea.getDocument().addDocumentListener(new DocumentListener(){
				@Override
				public void insertUpdate(final DocumentEvent e){
					onResultEdited();
				}

				@Override
				public void removeUpdate(final DocumentEvent e){
					onResultEdited();
				}

				@Override
				public void changedUpdate(final DocumentEvent e){ /* plain text */ }
			});
			//what the user wrote is aligned with the sides when it is left, not while it is being typed
			resultArea.addFocusListener(new FocusAdapter(){
				@Override
				public void focusLost(final FocusEvent e){
					SwingUtilities.invokeLater(Section.this::realign);
				}
			});
			syncRows();
			paintResult();
		}

		/** Takes this text as the result, and counts as the user's decision even if it is what the result already was. */
		private void take(final String text){
			resultArea.setText(text);
			realign();
			resultArea.setCaretPosition(0);
			resolved = true;
			paintResult();
			onStateChanged();
		}

		/** Goes back to what the result was when the window opened, and the conflict is to be decided again. */
		private void reset(){
			resultArea.setText(initialShown);
			realign();
			resultArea.setCaretPosition(0);
			resolved = false;
			paintResult();
			onStateChanged();
		}

		/**
		 * Takes a block of one side, or none: the lines of the result on the rows of the block are replaced by the ones the side has
		 * on them (empty lines if it has none, so that the block of a side that lacks it is removed), and nothing else changes.
		 *
		 * @param side 0 for the source, 1 for the target, 2 for none
		 */
		private void takeBlock(final int[] span, final int side){
			final List<String> result = new ArrayList<>(placed.result());
			for(int line = span[0]; line <= span[1]; line ++)
				result.set(line, (side == 0? placed.source().get(line): (side == 1? placed.target().get(line): null)));
			resultArea.setText(placed.text(result));
			realign();
			resultArea.setCaretPosition(0);
			resolved = true;
			paintResult();
			onStateChanged();
		}

		private static boolean hasLines(final List<String> column, final int[] span){
			for(int line = span[0]; line <= span[1]; line ++)
				if(column.get(line) != null)
					return true;
			return false;
		}

		/** The buttons of the blocks that differ, each at the first line of its block: take it from the source, from the target, or drop it. */
		private void rebuildBlockButtons(){
			for(final JButton button : blockButtons){
				leftGutter.remove(button);
				rightGutter.remove(button);
			}
			blockButtons.clear();
			if(placed == null)
				return;

			final int lineHeight = sourceArea.getFontMetrics(sourceArea.getFont()).getHeight();
			final int top = sourceArea.getInsets().top;
			final int height = Math.min(BUTTON_HEIGHT, lineHeight);
			for(final int[] span : placed.spans()){
				final int y = top + span[0] * lineHeight;
				final JButton fromSource = createButton("→", I18N.t("tooltip.merge.block.use_source"));
				fromSource.setEnabled(hasLines(placed.source(), span));
				fromSource.addActionListener(e -> takeBlock(span, 0));
				leftGutter.place(fromSource, 1, y, height);
				final JButton fromTarget = createButton("←", I18N.t("tooltip.merge.block.use_target"));
				fromTarget.setEnabled(hasLines(placed.target(), span));
				fromTarget.addActionListener(e -> takeBlock(span, 1));
				rightGutter.place(fromTarget, 1, y, height);
				final JButton remove = createButton("✕", I18N.t("tooltip.merge.block.remove"));
				remove.setEnabled(hasLines(placed.result(), span));
				remove.addActionListener(e -> takeBlock(span, 2));
				rightGutter.place(remove, 2, y, height);
				blockButtons.add(fromSource);
				blockButtons.add(fromTarget);
				blockButtons.add(remove);
			}
			leftGutter.revalidate();
			leftGutter.repaint();
			rightGutter.revalidate();
			rightGutter.repaint();
		}

		/** Whether the result is not what it started as: the empty lines that keep the columns aligned say nothing. */
		boolean isEdited(){
			return !logical(resultArea.getText()).equals(initialLogical);
		}

		/**
		 * Shows the three columns, one row for each line: a block of a column faces at most one block of another, and an empty line is
		 * where a column has none. The text of the result is not changed, but for the empty lines.
		 */
		private void realign(){
			placed = place(rows, sourceRecord, targetRecord, plan.target().getTag(), model, logicalLines(resultArea.getText()));

			sourceArea.setText(placed.text(placed.source()));
			targetArea.setText(placed.text(placed.target()));
			final String result = placed.text(placed.result());
			if(!result.equals(resultArea.getText()))
				resultArea.setText(result);
			sourceArea.setCaretPosition(0);
			targetArea.setCaretPosition(0);
			DiffPainters.paint(sourceArea, placed.sourceKind().toArray(new DiffKind[0]), MergeRecordReviewDialog.this::nameOfReference);
			DiffPainters.paint(targetArea, placed.targetKind().toArray(new DiffKind[0]), MergeRecordReviewDialog.this::nameOfReference);
			syncRows();
			paintResult();
			if(leftGutter != null)
				rebuildBlockButtons();
		}

		boolean isConflict(){
			return (kind == DiffKind.CONFLICT);
		}

		/** A difference is a section that is not the same in the two records. */
		boolean isDifference(){
			return (kind != DiffKind.SAME);
		}

		private void onResultEdited(){
			if(isEdited())
				resolved = true;
			syncRows();
			paintResult();
			onStateChanged();
		}

		/**
		 * Colours the lines of the result: one that is not in what the result started as is edited; of the others, one that is a
		 * conflict of the two sides is red until the section is decided, then it has the colour of the side it came from.
		 */
		private void paintResult(){
			final String[] lines = linesOf(resultArea.getText());
			final Map<String, Integer> initial = new HashMap<>();
			for(final String line : initialLines)
				initial.merge(line, 1, Integer::sum);

			final DiffKind[] marks = new DiffKind[lines.length];
			for(int i = 0; i < lines.length; i ++){
				final String line = lines[i];
				if(line.isBlank() || isDelimiter(line))
					continue;

				final Integer left = initial.get(line);
				if(left == null || left == 0)
					marks[i] = DiffKind.EDITED;
				else{
					initial.put(line, left - 1);
					if(conflictLines.contains(line))
						marks[i] = (!resolved? (referenceLines.contains(line)? DiffKind.REFERENCE: DiffKind.CONFLICT)
							: (sourceOnlyLines.contains(line) || isFromSource(line)
							? DiffKind.FROM_SOURCE
							: DiffKind.FROM_TARGET));
					else if(sourceOnlyLines.contains(line))
						marks[i] = DiffKind.FROM_SOURCE;
					else if(targetOnlyLines.contains(line))
						marks[i] = DiffKind.FROM_TARGET;
				}
			}
			DiffPainters.paint(resultArea, marks, MergeRecordReviewDialog.this::nameOfReference);
		}

		/** Whether the line is one of the source's, as opposed to the target's. */
		private boolean isFromSource(final String line){
			for(final String sourceLine : sourceLines)
				if(sourceLine.equals(line))
					return true;
			return false;
		}

		/** The three texts of a section have the same height, so that the lines stay aligned whoever grows it. */
		private void syncRows(){
			final int lines = Math.max(MIN_ROWS, Math.max(countLines(sourceArea.getText()),
				Math.max(countLines(resultArea.getText()), countLines(targetArea.getText()))));
			if(sourceArea.getRows() == lines)
				return;

			sourceArea.setRows(lines);
			resultArea.setRows(lines);
			targetArea.setRows(lines);
			grid.revalidate();
			grid.repaint();
		}

		void setCurrent(final boolean current){
			final var border = BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(current? COLOR_CURRENT: COLOR_BORDER, current? 2: 1),
				BorderFactory.createEmptyBorder(current? 3: 4, current? 5: 6, current? 3: 4, current? 5: 6));
			sourceArea.setBorder(border);
			resultArea.setBorder(border);
			targetArea.setBorder(border);
		}

		void addTo(final JPanel grid){
			grid.add(sourceArea, "growx,aligny top");
			grid.add(leftGutter, "growy,aligny top");
			grid.add(resultArea, "growx,aligny top");
			grid.add(rightGutter, "growy,aligny top");
			grid.add(targetArea, "growx,aligny top");
		}

		void setVisible(final boolean visible){
			sourceArea.setVisible(visible);
			leftGutter.setVisible(visible);
			resultArea.setVisible(visible);
			rightGutter.setVisible(visible);
			targetArea.setVisible(visible);
		}

		/** The place of the section in the grid, to scroll to and to mark in the overview. */
		Rectangle bounds(){
			return sourceArea.getBounds().union(resultArea.getBounds()).union(targetArea.getBounds());
		}

	}


	private final MergePlan plan;
	private final FLEFModel model;
	private final FLEFGrammar grammar;
	private final List<FieldRow> rows = new ArrayList<>();
	/** The suggesters of the tags a field allows, one for each field: each reads the grammar when it is made. */
	private final Map<String, TagSuggester> suggesters = new HashMap<>();
	/** Every section of the record, in the order they are shown. */
	private final List<Section> sections = new ArrayList<>();
	private final JPanel grid = new JPanel(new MigLayout("ins 6,fillx,wrap 5,hidemode 3", COLUMNS));
	private final JCheckBox onlyDifferences = new JCheckBox(I18N.t("dialog.merge.review.only_differences"), true);
	private final JLabel summary = new JLabel(StringUtils.SPACE);
	private final JButton previousConflict = new JButton("▲ " + I18N.t("dialog.merge.review.nav.previous_conflict"));
	private final JButton nextConflict = new JButton("▼ " + I18N.t("dialog.merge.review.nav.next_conflict"));
	private final JButton previousDifference = new JButton("▲ " + I18N.t("dialog.merge.review.nav.previous_difference"));
	private final JButton nextDifference = new JButton("▼ " + I18N.t("dialog.merge.review.nav.next_difference"));
	private final Overview overview = new Overview();
	private JScrollPane scroll;

	private Section current;
	private boolean confirmed;
	/** The window has just opened: the view stays at the first line until the user moves it, whatever the layout does meanwhile. */
	private boolean opening = true;
	/** Says, a moment after the last change, whether the result is valid: the same check that Merge makes, without waiting for it. */
	private final JLabel validity = new JLabel(StringUtils.SPACE);
	private final Timer validityTimer = new Timer(VALIDITY_DELAY, e -> checkValidity());
	private int validityGeneration;
	/** The texts that were checked last: a check of the same texts says nothing new, and is not made. */
	private Map<FieldRow, String> checkedTexts = Map.of();


	/**
	 * @param owner  the window the dialog is on
	 * @param plan   the plan of all the fields of the two records, whose decisions the dialog completes
	 * @param model  the file, to validate the result in
	 * @param source the name of the record that is deleted, for the title
	 * @param target the name of the record that is kept, for the title
	 */
	public MergeRecordReviewDialog(final Window owner, final MergePlan plan, final FLEFModel model, final String source,
			final String target){
		super(owner, I18N.tf("dialog.merge.review.title", source, target), ModalityType.APPLICATION_MODAL);

		this.plan = plan;
		this.model = model;
		grammar = MergeGrammar.instance();

		ToolDialogs.installEscapeToClose(this);

		for(final MergeField field : plan.fields()){
			final FieldRow row = new FieldRow(field, statusOf(field));
			buildSections(row);
			rows.add(row);
		}
		for(final FieldRow row : rows){
			grid.add(row.header, "span 5,growx,gaptop 8");
			for(final Section section : row.sections){
				section.addTo(grid);
				section.setCurrent(false);
				sections.add(section);
			}
		}
		applyFilter();
		onlyDifferences.addActionListener(e -> applyFilter());
		grid.addComponentListener(new ComponentAdapter(){
			@Override
			public void componentResized(final ComponentEvent e){
				overview.repaint();
			}
		});

		scroll = new JScrollPane(grid, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.getVerticalScrollBar().setUnitIncrement(GUIHelper.SCROLLBAR_UNIT_INCREMENT);
		scroll.getHorizontalScrollBar().setUnitIncrement(GUIHelper.SCROLLBAR_UNIT_INCREMENT);
		scroll.setColumnHeaderView(createColumnTitles());

		final JPanel center = new JPanel(new BorderLayout());
		center.add(scroll, BorderLayout.CENTER);
		center.add(overview, BorderLayout.EAST);

		setLayout(new BorderLayout(0, 6));
		add(createTop(), BorderLayout.NORTH);
		add(center, BorderLayout.CENTER);
		add(createBottom(), BorderLayout.SOUTH);

		installKeys();
		updateSummary();
		updateNavigation();

		setPreferredSize(new Dimension(1200, 700));
		pack();
		setLocationRelativeTo(owner);

		//the view starts at the first line of the record, not at a conflict or at the end: the buttons go to the conflicts
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
			public void mouseWheelMoved(final java.awt.event.MouseWheelEvent e){
				opening = false;
			}
		};
		scroll.addMouseWheelListener(userScrolls);
		scroll.getVerticalScrollBar().addMouseListener(userScrolls);
		scroll.getHorizontalScrollBar().addMouseListener(userScrolls);
		overview.addMouseListener(userScrolls);
		addWindowListener(new WindowAdapter(){
			@Override
			public void windowOpened(final WindowEvent e){
				SwingUtilities.invokeLater(() -> showTop());
			}
		});
	}


	/** Whether the user confirmed the review: the decisions are in the plan, ready to be applied. */
	public boolean isConfirmed(){
		return confirmed;
	}


	/* ======================================================================
	 *                          Construction
	 * ====================================================================== */

	private Status statusOf(final MergeField field){
		if(plan.collisions().contains(field))
			return Status.COLLISION;
		if(field.targetInstances().isEmpty())
			return Status.SOURCE_ONLY;
		if(field.sourceInstances().isEmpty())
			return Status.TARGET_ONLY;

		return Status.SAME;
	}

	/**
	 * Aligns the occurrences of the two records, as lines of a file: the ones that are the same are one section; between two of them,
	 * what only the source has and what only the target has are paired, one against the other, as conflicts (what is left over is
	 * from one side only).
	 */
	private void buildSections(final FieldRow row){
		final MergeField field = row.field;
		final List<FLEFRecord> source = field.sourceInstances();
		final List<FLEFRecord> target = field.targetInstances();

		//what the result starts with: a composition made elsewhere is kept, otherwise it is what the decision says
		final Map<String, Integer> composed = new HashMap<>();
		if(field.isComposed())
			for(final FLEFRecord instance : field.resultInstances())
				composed.merge(MergeFragments.serialize(instance), 1, Integer::sum);
		final Defaults defaults = new Defaults(field, composed);

		//the occurrences are shown in the order of the target, each beside the one it is the same thing as, wherever it was
		for(final int[] step : displayOrder(pairBlocks(source, target, model, true, tag -> !field.isRepeated()), target.size())){
			final FLEFRecord sourceInstance = (step[0] >= 0? source.get(step[0]): null);
			final FLEFRecord targetInstance = (step[1] >= 0? target.get(step[1]): null);
			if(sourceInstance != null && targetInstance != null){
				if(MergeFragments.serialize(sourceInstance).equals(MergeFragments.serialize(targetInstance)))
					row.sections.add(new Section(row, DiffKind.SAME, sourceInstance, targetInstance, defaults.same(targetInstance)));
				else
					row.sections.add(new Section(row, DiffKind.CONFLICT, sourceInstance, targetInstance,
						defaults.conflict(sourceInstance, targetInstance)));
			}
			else if(sourceInstance != null)
				row.sections.add(new Section(row, DiffKind.FROM_SOURCE, sourceInstance, null, defaults.onlySource(sourceInstance)));
			else
				row.sections.add(new Section(row, DiffKind.FROM_TARGET, null, targetInstance, defaults.onlyTarget(targetInstance)));
		}

		//a part of the composition that matches no occurrence of either side was written by the user
		for(final String text : defaults.leftOver())
			row.sections.add(new Section(row, DiffKind.EDITED, null, null, text));
	}

	/** The text each section of a field starts with, from the decision taken on the field so far. */
	private static final class Defaults{

		private final MergeField field;
		private final Map<String, Integer> composed;


		Defaults(final MergeField field, final Map<String, Integer> composed){
			this.field = field;
			this.composed = composed;
		}

		private boolean takesSource(){
			return (field.chosen() != MergeDecision.KEEP_TARGET);
		}

		private boolean takesTarget(){
			return (field.chosen() != MergeDecision.KEEP_SOURCE);
		}

		/** Whether the composition has this occurrence: the composition is used up as the occurrences claim their part of it. */
		private boolean composedHas(final FLEFRecord instance){
			final String text = MergeFragments.serialize(instance);
			final Integer count = composed.get(text);
			if(count == null || count == 0)
				return false;

			composed.put(text, count - 1);
			return true;
		}

		String same(final FLEFRecord target){
			if(field.isComposed())
				return (composedHas(target)? MergeFragments.serialize(target): StringUtils.EMPTY);

			return MergeFragments.serialize(target);
		}

		String onlySource(final FLEFRecord source){
			if(field.isComposed())
				return (composedHas(source)? MergeFragments.serialize(source): StringUtils.EMPTY);

			return (takesSource()? MergeFragments.serialize(source): StringUtils.EMPTY);
		}

		String onlyTarget(final FLEFRecord target){
			if(field.isComposed())
				return (composedHas(target)? MergeFragments.serialize(target): StringUtils.EMPTY);

			return (takesTarget()? MergeFragments.serialize(target): StringUtils.EMPTY);
		}

		String conflict(final FLEFRecord source, final FLEFRecord target){
			final StringBuilder sb = new StringBuilder();
			if(field.isComposed()){
				if(composedHas(target))
					sb.append(MergeFragments.serialize(target));
				if(composedHas(source))
					sb.append(MergeFragments.serialize(source));
				return sb.toString();
			}

			if(takesTarget())
				sb.append(MergeFragments.serialize(target));
			if(takesSource())
				sb.append(MergeFragments.serialize(source));
			return sb.toString();
		}

		/** What the composition has that no occurrence of either side claimed. */
		List<String> leftOver(){
			final List<String> left = new ArrayList<>();
			for(final Map.Entry<String, Integer> entry : composed.entrySet())
				for(int k = 0; k < entry.getValue(); k ++)
					left.add(entry.getKey());
			return left;
		}

	}

	private JPanel createColumnTitles(){
		final JPanel titles = new JPanel(new MigLayout("ins 6 6 0 6,fillx,wrap 5", COLUMNS));
		titles.add(createTitle(I18N.t("dialog.merge.field.source")), "growx");
		titles.add(new JLabel());
		titles.add(createTitle(I18N.t("dialog.merge.field.result")), "growx");
		titles.add(new JLabel());
		titles.add(createTitle(I18N.t("dialog.merge.field.target")), "growx");
		return titles;
	}

	private static JLabel createTitle(final String text){
		final JLabel title = new JLabel(text);
		title.setFont(title.getFont().deriveFont(Font.BOLD));
		return title;
	}

	/** Takes every conflict that is not decided yet from one side. */
	private void takeConflicts(final boolean fromSource){
		for(final Section section : sections)
			if(section.isConflict() && !section.resolved)
				section.take(fromSource? section.sourceText: section.targetText);
	}

	private JPanel createTop(){
		final JPanel top = new JPanel(new MigLayout("ins 8 8 0 8,fillx,wrap 1", "[grow,fill]"));

		final JLabel hint = new JLabel(I18N.t("dialog.merge.review.hint"));
		hint.setForeground(COLOR_MUTED);
		top.add(hint);

		final JPanel tools = new JPanel(new MigLayout("ins 0,fillx", "[]6[]12[]6[]push[]12[]"));
		previousConflict.setToolTipText(I18N.t("dialog.merge.review.nav.previous_conflict.tooltip"));
		previousConflict.addActionListener(e -> goToPrevious(true));
		tools.add(previousConflict);
		nextConflict.setToolTipText(I18N.t("dialog.merge.review.nav.next_conflict.tooltip"));
		nextConflict.addActionListener(e -> goToNext(true));
		tools.add(nextConflict);
		previousDifference.setToolTipText(I18N.t("dialog.merge.review.nav.previous_difference.tooltip"));
		previousDifference.addActionListener(e -> goToPrevious(false));
		tools.add(previousDifference);
		nextDifference.setToolTipText(I18N.t("dialog.merge.review.nav.next_difference.tooltip"));
		nextDifference.addActionListener(e -> goToNext(false));
		tools.add(nextDifference);
		tools.add(createLegend());
		tools.add(onlyDifferences);
		top.add(tools);

		final JPanel decisions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		final JButton allSource = new JButton(I18N.t("dialog.merge.review.take_all_source"));
		allSource.setToolTipText(I18N.t("dialog.merge.review.take_all_source.tooltip"));
		allSource.addActionListener(e -> takeConflicts(true));
		decisions.add(allSource);
		final JButton allTarget = new JButton(I18N.t("dialog.merge.review.take_all_target"));
		allTarget.setToolTipText(I18N.t("dialog.merge.review.take_all_target.tooltip"));
		allTarget.addActionListener(e -> takeConflicts(false));
		decisions.add(allTarget);
		top.add(decisions);

		return top;
	}

	private static JPanel createLegend(){
		final JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		legend.add(createChip(I18N.t("dialog.merge.review.legend.source"), COLOR_FROM_SOURCE));
		legend.add(createChip(I18N.t("dialog.merge.review.legend.target"), COLOR_FROM_TARGET));
		legend.add(createChip(I18N.t("dialog.merge.review.legend.conflict"), COLOR_CONFLICT));
		legend.add(createChip(I18N.t("dialog.merge.review.legend.reference"), COLOR_REFERENCE));
		legend.add(createChip(I18N.t("dialog.merge.review.legend.edited"), COLOR_EDITED));
		return legend;
	}

	private static JLabel createChip(final String text, final Color color){
		final JLabel chip = new JLabel(text);
		chip.setOpaque(true);
		chip.setBackground(color);
		chip.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(COLOR_BORDER),
			BorderFactory.createEmptyBorder(1, 6, 1, 6)));
		return chip;
	}

	private JPanel createBottom(){
		final JPanel bottom = new JPanel(new BorderLayout());
		bottom.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

		summary.setForeground(COLOR_MUTED);
		bottom.add(summary, BorderLayout.CENTER);

		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
		buttons.add(validity);
		//there is no button to give up: ESC closes the dialog, as every dialog, and nothing has been written
		final JButton merge = new JButton(I18N.t("button.merge"));
		merge.addActionListener(e -> onMerge());
		buttons.add(merge);
		getRootPane().setDefaultButton(merge);
		bottom.add(buttons, BorderLayout.EAST);

		return bottom;
	}

	private void installKeys(){
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), "next-difference", () -> goToNext(false));
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), "previous-difference", () -> goToPrevious(false));
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_F8, KeyEvent.SHIFT_DOWN_MASK), "next-conflict", () -> goToNext(true));
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_F7, KeyEvent.SHIFT_DOWN_MASK), "previous-conflict", () -> goToPrevious(true));
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

	private void applyFilter(){
		final boolean onlyDifferent = onlyDifferences.isSelected();
		for(final FieldRow row : rows){
			boolean any = false;
			for(final Section section : row.sections){
				final boolean visible = (!onlyDifferent || section.isDifference());
				section.setVisible(visible);
				any |= visible;
			}
			row.header.setVisible(any);
		}

		grid.revalidate();
		grid.repaint();
		overview.repaint();
	}


	/* ======================================================================
	 *                          Navigation
	 * ====================================================================== */

	/** Called whenever a section is decided or edited. */
	private void onStateChanged(){
		updateSummary();
		overview.repaint();
		validityTimer.restart();
	}

	/**
	 * What is wrong with the result as it is now: the text of the fields that were changed that cannot be read, or that a field with a
	 * single value has twice, or what the validation of the whole file says. Empty if it is valid. It runs outside the event dispatch
	 * thread: the texts are read before.
	 */
	private List<String> problemsOf(final Map<FieldRow, String> texts){
		final String recordTag = plan.target().getTag();
		final List<String> problems = new ArrayList<>();
		final Map<FieldRow, List<FLEFRecord>> edited = new LinkedHashMap<>();
		for(final Map.Entry<FieldRow, String> entry : texts.entrySet())
			try{
				final List<FLEFRecord> instances = MergeFragments.parse(recordTag, entry.getValue());
				if(!entry.getKey().field.isRepeated() && instances.size() > 1)
					problems.add(I18N.tf("dialog.merge.review.single", entry.getKey().field.tag()));
				edited.put(entry.getKey(), instances);
			}
			catch(final IOException | RuntimeException ex){
				problems.add(entry.getKey().field.tag() + ": " + (ex.getMessage() != null? ex.getMessage(): ex.toString()));
			}
		if(problems.isEmpty())
			problems.addAll(validate(edited));
		return problems;
	}

	/** Says what the bar says, only if it is not already what it says: a text that is written again blinks. */
	private void say(final String text, final Color color, final String tooltip){
		if(!text.equals(validity.getText()))
			validity.setText(text);
		if(!color.equals(validity.getForeground()))
			validity.setForeground(color);
		if(!Objects.equals(tooltip, validity.getToolTipText()))
			validity.setToolTipText(tooltip);
	}

	/**
	 * Checks the result in the background and says it in the bar: the check of the whole file can take a moment on a large one.
	 * <p>
	 * The bar is steady: while a check runs it keeps what it said (it says it is checking only if it had nothing to say), and what a
	 * check finds replaces it only if it is not the same; the texts that were already checked are not checked again.
	 */
	private void checkValidity(){
		final Map<FieldRow, String> texts = new LinkedHashMap<>();
		for(final FieldRow row : rows)
			if(row.isEdited())
				texts.put(row, row.resultText());
		if(texts.equals(checkedTexts))
			return;

		checkedTexts = texts;
		final int generation = ++ validityGeneration;
		if(texts.isEmpty()){
			say(StringUtils.SPACE, COLOR_MUTED, null);
			return;
		}

		if(validity.getText().isBlank())
			say(I18N.t("dialog.merge.review.validity.checking"), COLOR_MUTED, null);
		new SwingWorker<List<String>, Void>(){
			@Override
			protected List<String> doInBackground(){
				return problemsOf(texts);
			}

			@Override
			protected void done(){
				//a newer check replaces this one, and a closed window has nothing to say
				if(generation != validityGeneration || !isDisplayable())
					return;

				try{
					final List<String> problems = get();
					if(problems.isEmpty())
						say("✓ " + I18N.t("dialog.merge.review.validity.ok"), COLOR_VALID, null);
					else
						say("✗ " + I18N.tf("dialog.merge.review.validity.problems", problems.size()), COLOR_INVALID,
							"<html>" + problems.stream()
								.limit(MAX_ERRORS_SHOWN)
								.map(StringEscapeUtils::escapeHtml4)
								.collect(Collectors.joining("<br>")) + "</html>");
				}
				catch(final Exception ignored){
					say(StringUtils.SPACE, COLOR_MUTED, null);
				}
			}
		}.execute();
	}

	@Override
	public void dispose(){
		validityTimer.stop();
		validityGeneration ++;

		super.dispose();
	}

	private void updateSummary(){
		int conflicts = 0;
		int undecided = 0;
		int differences = 0;
		for(final Section section : sections){
			if(section.isDifference())
				differences ++;
			if(section.isConflict()){
				conflicts ++;
				if(!section.resolved)
					undecided ++;
			}
		}
		summary.setText(I18N.tf("dialog.merge.review.summary", conflicts, undecided, differences));
	}

	private void updateNavigation(){
		final int from = (current != null? sections.indexOf(current): -1);
		previousConflict.setEnabled(find(from < 0? sections.size(): from, -1, true) != null);
		nextConflict.setEnabled(find(from, 1, true) != null);
		previousDifference.setEnabled(find(from < 0? sections.size(): from, -1, false) != null);
		nextDifference.setEnabled(find(from, 1, false) != null);
	}

	/** The next section, from {@code from} exclusive, that is a conflict (or any difference) in the direction {@code step}. */
	private Section find(final int from, final int step, final boolean conflictsOnly){
		for(int i = from + step; i >= 0 && i < sections.size(); i += step){
			final Section section = sections.get(i);
			if(conflictsOnly? section.isConflict(): section.isDifference())
				return section;
		}
		return null;
	}

	/** Shows the record from its first line: the three columns, the scroll bars and the carets at the top. */
	private void keepAtTop(){
		//the user is writing in a text, or went somewhere with the buttons: the view follows them
		if(opening && !(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JTextArea))
			showTop();
	}

	void showTop(){
		for(final Section section : sections){
			section.sourceArea.setCaretPosition(0);
			section.resultArea.setCaretPosition(0);
			section.targetArea.setCaretPosition(0);
		}
		scroll.getVerticalScrollBar().setValue(0);
		scroll.getHorizontalScrollBar().setValue(0);
	}

	/** Goes to the first conflict, or to the first difference if there is no conflict. */
	void goToFirst(){
		Section first = find(-1, 1, true);
		if(first == null)
			first = find(-1, 1, false);
		if(first != null)
			goTo(first);
	}

	void goToNext(final boolean conflictsOnly){
		final Section next = find(current != null? sections.indexOf(current): -1, 1, conflictsOnly);
		if(next != null)
			goTo(next);
	}

	void goToPrevious(final boolean conflictsOnly){
		final Section previous = find(current != null? sections.indexOf(current): sections.size(), -1, conflictsOnly);
		if(previous != null)
			goTo(previous);
	}

	private void goTo(final Section section){
		opening = false;
		if(current != null)
			current.setCurrent(false);
		current = section;
		section.setCurrent(true);
		updateNavigation();
		overview.repaint();
		SwingUtilities.invokeLater(() -> scrollTo(section));
	}

	/** Shows the section in the middle of the view, if it does not fit it, from its top. */
	private void scrollTo(final Section section){
		final Rectangle bounds = section.bounds();
		if(bounds.isEmpty())
			return;

		final int viewHeight = scroll.getViewport().getHeight();
		final Rectangle target = new Rectangle(bounds);
		target.y = Math.max(0, bounds.y - Math.max(0, (viewHeight - bounds.height) / 2));
		target.height = Math.max(bounds.height, viewHeight);
		grid.scrollRectToVisible(target);
	}

	/** The number of the conflict the dialog is on, from 1, or 0 if it is not on one: for the tests and the status. */
	int currentConflictNumber(){
		if(current == null || !current.isConflict())
			return 0;

		int number = 0;
		for(final Section section : sections){
			if(section.isConflict())
				number ++;
			if(section == current)
				return number;
		}
		return 0;
	}

	/** What the result of the section the dialog is on says now, empty if it is on none: for the tests. */
	String currentResultText(){
		return (current != null? current.resultArea.getText(): StringUtils.EMPTY);
	}

	/** Where the vertical scroll bar is: for the tests. */
	int verticalScrollPosition(){
		return scroll.getVerticalScrollBar().getValue();
	}

	/** What the bar of the validity says now: for the tests. */
	String validityText(){
		return validity.getText();
	}

	/** Checks the result now, without waiting for the pause after the last change: for the tests. */
	void checkValidityNow(){
		checkValidity();
	}

	/** Writes a text as the result of a section, as if it was typed: for the tests. */
	void writeResult(final int section, final String text){
		sections.get(section).resultArea.setText(text);
	}

	/** How many blocks of the section differ (each has its own buttons): for the tests. */
	int blockCount(final int section){
		return sections.get(section).placed.spans().size();
	}

	/** Takes a block of a section from a side (0 the source, 1 the target, 2 none): for the tests. */
	void takeBlock(final int section, final int block, final int side){
		final Section target = sections.get(section);
		target.takeBlock(target.placed.spans().get(block), side);
	}

	/** The text of the result of a section, without the empty lines of the alignment: for the tests. */
	String resultText(final int section){
		return logical(sections.get(section).resultArea.getText());
	}

	/** Takes every conflict not decided yet from a side: for the tests. */
	void takeAllConflicts(final boolean fromSource){
		takeConflicts(fromSource);
	}

	/** How many conflicts the record has: for the tests. */
	int conflictCount(){
		return (int)sections.stream().filter(Section::isConflict).count();
	}

	/** How many conflicts are still to be decided: for the tests. */
	int undecidedConflictCount(){
		return (int)sections.stream().filter(section -> section.isConflict() && !section.resolved).count();
	}


	/** The strip beside the scroll bar: where the differences are in the whole record, and the one the dialog is on. */
	private final class Overview extends JComponent{

		Overview(){
			setPreferredSize(new Dimension(OVERVIEW_WIDTH, 0));
			setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, COLOR_BORDER));
			addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent e){
					final Section hit = sectionAt(e.getY());
					if(hit != null)
						goTo(hit);
				}
			});
		}

		private int yOf(final Section section){
			final int total = grid.getHeight();
			if(total <= 0)
				return 0;

			final Rectangle bounds = section.bounds();
			return (int)((long)bounds.y * getHeight() / total);
		}

		private Section sectionAt(final int y){
			Section best = null;
			int bestDistance = Integer.MAX_VALUE;
			for(final Section section : sections){
				if(!section.isDifference())
					continue;

				final int distance = Math.abs(yOf(section) - y);
				if(distance < bestDistance){
					bestDistance = distance;
					best = section;
				}
			}
			return (bestDistance <= 12? best: null);
		}

		@Override
		protected void paintComponent(final Graphics g){
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
			for(final Section section : sections){
				if(!section.isDifference())
					continue;

				final Rectangle bounds = section.bounds();
				if(bounds.isEmpty())
					continue;

				final int total = Math.max(1, grid.getHeight());
				final int y = yOf(section);
				final int height = Math.max(3, (int)((long)bounds.height * getHeight() / total));
				g.setColor(switch(section.kind){
					case FROM_SOURCE -> COLOR_FROM_SOURCE.darker();
					case FROM_TARGET -> COLOR_FROM_TARGET.darker();
					case CONFLICT -> (section.resolved? COLOR_EDITED.darker(): COLOR_CONFLICT.darker());
					default -> COLOR_EDITED.darker();
				});
				g.fillRect(2, y, getWidth() - 4, height);
				if(section == current){
					g.setColor(COLOR_CURRENT);
					g.drawRect(1, y - 1, getWidth() - 3, height + 1);
				}
			}
		}

	}


	/* ======================================================================
	 *                          Merge
	 * ====================================================================== */

	/**
	 * Reads the result column, validates the whole file with it and, when it holds, writes it into the plan.
	 * <p>
	 * Only the fields the user changed are read back: the others have no text to be wrong, and keep what the rules of the merge give.
	 */
	private void onMerge(){
		final String recordTag = plan.target().getTag();

		final Map<FieldRow, List<FLEFRecord>> edited = new LinkedHashMap<>();
		for(final FieldRow row : rows){
			if(!row.isEdited())
				continue;

			try{
				final List<FLEFRecord> instances = MergeFragments.parse(recordTag, row.resultText());
				if(!row.field.isRepeated() && instances.size() > 1){
					showError(I18N.t("dialog.merge.field.invalid_result"), I18N.tf("dialog.merge.review.single", row.field.tag()));
					return;
				}
				edited.put(row, instances);
			}
			catch(final IOException | RuntimeException ex){
				showError(I18N.t("dialog.merge.field.invalid_syntax"),
					row.field.tag() + ": " + (ex.getMessage() != null? ex.getMessage(): ex.toString()));
				return;
			}
		}

		final List<String> errors = validate(edited);
		if(!errors.isEmpty()){
			showValidationErrors(errors);
			return;
		}

		for(final Map.Entry<FieldRow, List<FLEFRecord>> entry : edited.entrySet())
			entry.getKey().field.setResultInstances(entry.getValue());
		confirmed = true;
		dispose();
	}

	/**
	 * Validates the whole file with the target replaced by a copy of itself that has the result of each field in place of the
	 * field: the schema, the references to the rest of the file and the business rules are checked in their real context.
	 */
	private List<String> validate(final Map<FieldRow, List<FLEFRecord>> edited){
		if(grammar == null)
			return List.of();

		final FLEFRecord target = plan.target();
		final FLEFRecord modified = FLEFRecord.createChildWithTag(target.getTag());
		if(target.getId() != null)
			modified.setId(target.getId());
		if(target.getValue() != null)
			modified.setValue(target.getValue());

		//the audit and the privacy are not fields of the plan: they stay as they are
		final Set<String> fieldTags = plan.fields().stream()
			.map(MergeField::tag)
			.collect(Collectors.toSet());
		for(final FLEFRecord child : target.getChildren())
			if(!fieldTags.contains(child.getTag()))
				modified.addChild(child);
		for(final FieldRow row : rows)
			for(final FLEFRecord instance : edited.getOrDefault(row, initialInstances(row.field)))
				modified.addChild(instance);

		final FLEFModel copy = new FLEFModel();
		if(model != null && model.getHeader() != null && model.getHeader().hasData())
			copy.setHeader(model.getHeader());
		if(model != null){
			for(final FLEFRecord record : model.getRecords())
				copy.addRecord(Objects.equals(record.getId(), target.getId())? modified: record);
		}
		else
			copy.addRecord(modified);

		return new FLEFValidator(grammar).validateAll(copy);
	}

	/**
	 * What a field has after the choices made so far: the composition if there is one, otherwise what the decision says, the
	 * target's occurrences and the source's that the target does not already say for both.
	 */
	private static List<FLEFRecord> initialInstances(final MergeField field){
		if(field.isComposed())
			return field.resultInstances();

		return switch(field.chosen()){
			case KEEP_TARGET -> field.targetInstances();
			case KEEP_SOURCE -> field.sourceInstances();
			case KEEP_BOTH -> {
				final List<FLEFRecord> both = new ArrayList<>(field.targetInstances());
				final Set<String> known = field.targetInstances().stream()
					.map(MergeFragments::serialize)
					.collect(Collectors.toSet());
				for(final FLEFRecord instance : field.sourceInstances())
					if(known.add(MergeFragments.serialize(instance)))
						both.add(instance);
				yield both;
			}
		};
	}

	private void showValidationErrors(final List<String> errors){
		final StringBuilder message = new StringBuilder(I18N.l("dialog.merge.field.invalid_schema"))
			.append(StringUtils.LF)
			.append(StringUtils.LF);
		final int shown = Math.min(errors.size(), MAX_ERRORS_SHOWN);
		for(int i = 0; i < shown; i ++)
			message.append("  • ")
				.append(errors.get(i))
				.append(StringUtils.LF);
		if(errors.size() > shown)
			message.append(I18N.tf("dialog.merge.field.more_errors", errors.size() - shown))
				.append(StringUtils.LF);

		showError(I18N.t("dialog.merge.field.invalid_result"), message.toString());
	}

	private void showError(final String title, final String body){
		JOptionPane.showMessageDialog(this, body, title, JOptionPane.ERROR_MESSAGE);
	}


	/* ======================================================================
	 *                          UI helpers
	 * ====================================================================== */

	private static JTextArea createArea(final String text, final boolean editable){
		final JTextArea area = new JTextArea(text){
			/** Only the text that is being written scrolls the record to the caret: the others stay where the record is. */
			@Override
			public void scrollRectToVisible(final Rectangle rectangle){
				if(isFocusOwner())
					super.scrollRectToVisible(rectangle);
			}
		};
		area.setEditable(editable);
		area.setLineWrap(false);
		area.setOpaque(true);
		area.setFont(MONO);
		area.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(COLOR_BORDER),
			BorderFactory.createEmptyBorder(4, 6, 4, 6)));
		area.setColumns(24);
		area.setRows(MIN_ROWS);
		//the text is read from its first line: a caret at the end would make the view follow it there
		area.setCaretPosition(0);
		return area;
	}

	private static JButton createButton(final String text, final String tooltip){
		final JButton button = new JButton(text);
		button.setToolTipText(tooltip);
		button.setFont(MONO.deriveFont(Font.BOLD));
		button.setMargin(new Insets(0, 2, 0, 2));
		button.setFocusable(false);
		button.setPreferredSize(new Dimension(BUTTON_COLUMN_WIDTH, 20));
		return button;
	}

	private static int countLines(final String text){
		if(StringUtils.isEmpty(text))
			return 0;

		int lines = 1;
		for(int i = 0; i < text.length(); i ++)
			if(text.charAt(i) == '\n')
				lines ++;
		return lines;
	}

}
