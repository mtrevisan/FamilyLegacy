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
package io.github.mtrevisan.familylegacy.v2.ui.tools.tools.merge;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;


/**
 * BeyondCompare-style merge view for a single field.
 * <p>
 * The dialog is a three-column comparison between the field as it
 * appears on the two records:
 * <ul>
 *   <li><b>Source</b> (left): the field on the record that will be
 *       deleted;</li>
 *   <li><b>Result</b> (center): the field that will end up on the
 *       surviving record, pre-filled with the complete merge and
 *       freely editable, with IntelliJ-style tag autocomplete;</li>
 *   <li><b>Target</b> (right): the field on the record that will be
 *       kept.</li>
 * </ul>
 * Every instance of the field is a row. Instances are aligned by their
 * serialized form: identical instances land on the same row
 * ({@code COMMON}), source-only instances on their own row
 * ({@code SOURCE_ONLY}), target-only instances on their own row
 * ({@code TARGET_ONLY}). A thin horizontal line separates consecutive
 * rows.
 * <p>
 * The result column is pre-filled with the complete merge: for
 * {@code COMMON} and {@code TARGET_ONLY} rows the target content is
 * used, for {@code SOURCE_ONLY} rows the source content is used. The
 * user's job is to remove what does not belong, not to build the
 * result from scratch. The three buttons per row are:
 * <ul>
 *   <li>{@code →}: replace the result of this row with the source
 *       content;</li>
 *   <li>{@code ←}: replace the result of this row with the target
 *       content;</li>
 *   <li>{@code ✕}: remove the row from the result.</li>
 * </ul>
 * <p>
 * <b>Autocomplete.</b> Each result area has an
 * {@link AutocompleteController} attached. It opens a popup with the
 * tags allowed at the caret position, or with the values of an enum
 * when the caret is at a value position. Block tags are inserted with
 * their braces, a two-space indent, and the mandatory children
 * pre-filled. The controller resolves the context against the whole
 * column, so a value typed in one area is visible to the resolution of
 * the areas below it.
 * <p>
 * <b>Highlighting.</b> A {@link TagHighlighter} marks unknown tags
 * with a red wavy underline, missing values with an amber background
 * wash, and invalid values with a red wavy underline. The highlighter
 * also shows a format hint on hover for the value kinds that are not
 * obvious (dates, durations, coordinates, ...).
 * <p>
 * <b>Validation.</b> On OK, the result column is parsed as a FLEF
 * fragment, then a copy of the whole model is built with the target
 * record replaced by the same record carrying the parsed fragment in
 * place of the field, and the copy is validated with
 * {@link FLEFValidator#validateAll}. This means the validator sees the
 * full file, not just the fragment: cardinality, cross-references and
 * business rules are all checked against the real context.
 * <p>
 * The three columns live in three independent scroll panes: each
 * column has its own horizontal scrollbar, but the vertical scroll is
 * kept in sync so the rows stay aligned while scrolling. Row heights
 * are recomputed on every edit of the result, so growing a row in the
 * result column grows it in the source and target columns too.
 */
public final class MergeFieldComparisonDialog extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(MergeFieldComparisonDialog.class);


	private static final Color COLOR_COMMON = new Color(0xFFFFFF);
	private static final Color COLOR_SOURCE_ONLY = new Color(0xE8F5E9);
	private static final Color COLOR_TARGET_ONLY = new Color(0xFFEBEE);
	private static final Color COLOR_RESULT_INCLUDED = new Color(0xE8F5E9);
	private static final Color COLOR_RESULT_EXCLUDED = new Color(0xEEEEEE);
	private static final Color COLOR_GUTTER = new Color(0xFAFAFA);
	private static final Color COLOR_SEPARATOR = new Color(0xB0B0B0);

	private static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 11);

	/** Minimum number of visible text lines per row. */
	private static final int MIN_ROWS = 3;

	/** Width of the column that hosts an action button. */
	private static final int BUTTON_COLUMN_WIDTH = 22;

	/** Maximum number of validation errors shown in the error dialog. */
	private static final int MAX_ERRORS_SHOWN = 10;

	/** Number of spaces used to indent a nested block. */
	private static final int INDENT_SIZE = 2;


	private enum Status{ COMMON, SOURCE_ONLY, TARGET_ONLY }

	private enum Choice{ NONE, INCLUDE_SOURCE, INCLUDE_TARGET }


	/* ======================================================================
	 *                          Model
	 * ====================================================================== */

	private static final class MergeLine{

		final FLEFRecord source;
		final FLEFRecord target;
		final Status status;
		Choice choice;

		MergeLine(final FLEFRecord source, final FLEFRecord target, final Status status,
			final Choice initialChoice){
			this.source = source;
			this.target = target;
			this.status = status;
			this.choice = initialChoice;
		}

		FLEFRecord result(){
			return switch(choice){
				case INCLUDE_SOURCE -> source;
				case INCLUDE_TARGET -> target;
				case NONE -> null;
			};
		}

	}


	/* ======================================================================
	 *                          Grammar holder
	 * ====================================================================== */

	/**
	 * Lazy holder of the grammar used to validate the merge result and
	 * to drive the autocomplete.
	 * <p>
	 * The grammar file is fixed and never changes at runtime, so it is
	 * parsed once and shared across every dialog instance. A missing or
	 * unreadable grammar disables schema validation and lets the
	 * autocomplete fall back to the hardcoded table in
	 * {@link TagSuggester}, rather than breaking the dialog.
	 */
	private static final class GrammarHolder{

		private static final FLEFGrammar INSTANCE = load();

		private static FLEFGrammar load(){
			try{
				final Path path = Paths.get("src/main/resources/gedg/flef_0.1.2.gedg");
				return FLEFGrammarParser.parse(path);
			}
			catch(final Exception ex){
				LOGGER.warn("Unable to load FLEF grammar; autocomplete falls back to "
					+ "the hardcoded table, schema validation is skipped", ex);
				return null;
			}
		}

		static FLEFGrammar instance(){
			return INSTANCE;
		}

		private GrammarHolder(){}

	}


	/* ======================================================================
	 *                          Row widgets
	 * ====================================================================== */

	private final class RowWidgets{

		private final MergeLine line;
		private final JTextArea sourceArea;
		private final JTextArea resultArea;
		private final JTextArea targetArea;
		private final JButton leftArrow;
		private final JButton rightArrow;
		private final JButton clearButton;
		private int rowCount;


		RowWidgets(final MergeLine line, final int rowCount){
			this.line = line;
			this.rowCount = rowCount;

			sourceArea = createArea(textOf(line.source), rowCount, false);
			sourceArea.setBackground(sourceColor());
			resultArea = createArea(textOf(line.result()), rowCount, true);
			targetArea = createArea(textOf(line.target), rowCount, false);
			targetArea.setBackground(targetColor());

			leftArrow = createActionButton("\u2192",
				"Replace the result of this row with the source content");
			rightArrow = createActionButton("\u2190",
				"Replace the result of this row with the target content");
			clearButton = createActionButton("\u2715",
				"Remove this row from the result");

			resultArea.getDocument().addDocumentListener(new DocumentListener(){
				@Override public void insertUpdate(final DocumentEvent e){ onResultEdited(); }
				@Override public void removeUpdate(final DocumentEvent e){ onResultEdited(); }
				@Override public void changedUpdate(final DocumentEvent e){ /* plain text */ }
			});

			refresh();
		}

		JTextArea sourceArea(){ return sourceArea; }
		JTextArea resultArea(){ return resultArea; }
		JTextArea targetArea(){ return targetArea; }
		JButton leftArrow(){ return leftArrow; }
		JButton rightArrow(){ return rightArrow; }
		JButton clearButton(){ return clearButton; }

		void useSource(){
			line.choice = Choice.INCLUDE_SOURCE;
			refresh();
		}

		void useTarget(){
			line.choice = Choice.INCLUDE_TARGET;
			refresh();
		}

		void clearResult(){
			line.choice = Choice.NONE;
			resultArea.setText("");
			refresh();
		}

		void refresh(){
			final String resultText = textOf(line.result());
			if(!resultText.equals(resultArea.getText()))
				resultArea.setText(resultText);
			resultArea.setBackground(line.result() != null
				? COLOR_RESULT_INCLUDED
				: COLOR_RESULT_EXCLUDED);

			leftArrow.setEnabled(line.source != null);
			rightArrow.setEnabled(line.target != null);
			clearButton.setEnabled(!resultArea.getText().isBlank());
		}

		private void onResultEdited(){
			clearButton.setEnabled(!resultArea.getText().isBlank());
			syncRowHeight();
		}

		private void syncRowHeight(){
			final int s = countLines(sourceArea.getText());
			final int r = countLines(resultArea.getText());
			final int t = countLines(targetArea.getText());
			final int max = Math.max(MIN_ROWS, Math.max(Math.max(s, r), t));
			if(max == rowCount)
				return;
			rowCount = max;

			sourceArea.setRows(max);
			resultArea.setRows(max);
			targetArea.setRows(max);

			revalidateColumns();
		}

		private Color sourceColor(){
			return switch(line.status){
				case COMMON -> COLOR_COMMON;
				case SOURCE_ONLY -> COLOR_SOURCE_ONLY;
				case TARGET_ONLY -> COLOR_GUTTER;
			};
		}

		private Color targetColor(){
			return switch(line.status){
				case COMMON -> COLOR_COMMON;
				case SOURCE_ONLY -> COLOR_GUTTER;
				case TARGET_ONLY -> COLOR_TARGET_ONLY;
			};
		}

	}


	/* ======================================================================
	 *                          Fields
	 * ====================================================================== */

	private final MergeField field;
	private final FLEFRecord targetRecord;
	private final FLEFModel model;
	private final FLEFGrammar grammar;
	private final TagSuggester suggester;

	private final List<MergeLine> lines = new ArrayList<>();
	private final List<RowWidgets> rows = new ArrayList<>();

	private final JPanel sourceColumn;
	private final JPanel resultColumn;
	private final JPanel targetColumn;

	private final JScrollPane sourceScroll;
	private final JScrollPane resultScroll;
	private final JScrollPane targetScroll;


	/* ======================================================================
	 *                          Construction
	 * ====================================================================== */

	public MergeFieldComparisonDialog(final Window owner, final MergeField field,
		final FLEFRecord targetRecord, final FLEFModel model){
		super(owner, "Merge field: " + field.tag(), ModalityType.APPLICATION_MODAL);

		this.field = Objects.requireNonNull(field, "field must not be null");
		this.targetRecord = targetRecord;
		this.model = model;
		this.grammar = GrammarHolder.instance();

		final String ownerTag = (targetRecord != null? targetRecord.getTag(): null);
		this.suggester = new TagSuggester(ownerTag, field.tag());

		buildLines(field);

		final int[] rowCounts = computeRowCounts();

		sourceColumn = buildSourceColumn(rowCounts);
		resultColumn = buildResultColumn();
		targetColumn = buildTargetColumn();

		sourceScroll = createScrollPane(sourceColumn, "Source (deleted)");
		resultScroll = createScrollPane(resultColumn, "Result (kept)");
		targetScroll = createScrollPane(targetColumn, "Target (existing)");

		syncVerticalScroll(sourceScroll, resultScroll, targetScroll);

		final JPanel columns = new JPanel(new MigLayout("ins 0,fill",
			"[grow,fill]4[grow,fill]4[grow,fill]", "[]"));
		columns.add(sourceScroll, "grow");
		columns.add(resultScroll, "grow");
		columns.add(targetScroll, "grow");

		setLayout(new BorderLayout(0, 8));
		add(columns, BorderLayout.CENTER);
		add(buildButtons(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(1100, 600));
		pack();
		setLocationRelativeTo(owner);

		installEditorSupport();
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	public List<FLEFRecord> getResultInstances(){
		final List<FLEFRecord> out = new ArrayList<>();
		for(final MergeLine line : lines){
			final FLEFRecord result = line.result();
			if(result != null)
				out.add(result);
		}
		return out;
	}


	/* ======================================================================
	 *                          Editor support
	 * ====================================================================== */

	/**
	 * Installs the autocomplete controller and the highlighter on every
	 * result area of the column.
	 * <p>
	 * Both need to see the whole column, not just one area: the field's
	 * structure is split across the areas, one row per area, so the
	 * enclosing blocks at the caret can only be determined by
	 * concatenating every area in order. Two functions are built here,
	 * both derived from the same concatenation:
	 * <ul>
	 *   <li>{@code textFor(area)} returns the whole column text;</li>
	 *   <li>{@code offsetFor(area)} returns the offset of the given
	 *       area inside that text.</li>
	 * </ul>
	 * The two functions use the same concatenation rule (append a
	 * newline after each area that does not already end with one), so
	 * the offset computed by one matches the position the other
	 * produces.
	 */
	private void installEditorSupport(){
		final List<JTextArea> resultAreas = new ArrayList<>();
		for(final RowWidgets rw : rows)
			resultAreas.add(rw.resultArea());

		final Function<JTextArea, String> textFor = area -> {
			final StringBuilder sb = new StringBuilder();
			for(final JTextArea a : resultAreas){
				final String text = a.getText();
				sb.append(text);
				if(!text.endsWith("\n"))
					sb.append('\n');
			}
			return sb.toString();
		};

		final Function<JTextArea, Integer> offsetFor = area -> {
			int offset = 0;
			for(final JTextArea a : resultAreas){
				if(a == area)
					return offset;
				final String text = a.getText();
				offset += text.length();
				if(!text.endsWith("\n"))
					offset ++;
			}
			return offset;
		};

		for(final JTextArea area : resultAreas)
			new AutocompleteController(area, this, suggester, INDENT_SIZE, textFor, offsetFor);

		TagHighlighter.install(resultAreas, suggester, textFor, offsetFor);
		installReferenceEditing(resultAreas, textFor, offsetFor);
	}

	/**
	 * Installs the double-click handler that opens the edit dialog for a
	 * referenced record. A field whose type is {@code Xref<...>} accepts
	 * as value the id of another record; double-clicking on the value
	 * opens the edit dialog of that record, as if the user had clicked
	 * the record in the tree.
	 * <p>
	 * The handler is installed on every result area: the reference can be
	 * on any row, and the row it belongs to is resolved from the whole
	 * column text, so the surrounding block context is always correct.
	 *
	 * @param areas     the text areas of the result column
	 * @param textFor   returns the whole column text, given one area
	 * @param offsetFor returns the offset of the given area inside that
	 *                  text
	 */
	private void installReferenceEditing(final List<JTextArea> areas,
		final Function<JTextArea, String> textFor,
		final Function<JTextArea, Integer> offsetFor){
		for(final JTextArea area : areas){
			area.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent e){
					if(e.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(e))
						return;

					final int offsetInArea = area.viewToModel2D(e.getPoint());
					if(offsetInArea < 0)
						return;

					final int fullOffset = offsetFor.apply(area) + offsetInArea;
					final TagSuggester.ReferenceHit hit =
						suggester.referenceAt(textFor.apply(area), fullOffset);
					if(hit != null)
						openReferenceEditor(hit);
				}
			});
		}
	}

	/**
	 * Opens the edit dialog for a referenced record. Does nothing when the
	 * referenced id is not present in the model, or when no handler is
	 * registered for the referenced record tag.
	 */
	private void openReferenceEditor(final TagSuggester.ReferenceHit hit){
		final FLEFRecord record = model.getRecordById(hit.recordId());
		if(record == null){
			LOGGER.debug("Reference target '{}' not found in the model",
				hit.recordId());
			return;
		}

		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(hit.recordTag());
		if(handler == null){
			LOGGER.debug("No handler registered for record tag '{}'",
				hit.recordTag());
			return;
		}

		final BaseRecordDialog dialog = handler.createEditDialog(this, model, record);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			// The record changed: the display of the reference might not
			// change (we show the id, not the display text), but the
			// validation of the whole model depends on the record, so we
			// refresh the highlight to reflect any new inconsistency.
			refreshHighlights();
		}
	}

	/**
	 * Re-runs the highlighter on the whole column. Called after a
	 * referenced record has been edited, so any change to the model is
	 * reflected in the highlights (for example, a reference that was
	 * dangling and is now resolved).
	 */
	private void refreshHighlights(){
		final List<JTextArea> resultAreas = new ArrayList<>();
		for(final RowWidgets rw : rows)
			resultAreas.add(rw.resultArea());
		for(final JTextArea area : resultAreas)
			area.repaint();
	}


	/* ======================================================================
	 *                          Column construction
	 * ====================================================================== */

	private JPanel buildSourceColumn(final int[] rowCounts){
		final JPanel panel = new JPanel(new MigLayout("ins 0,wrap 1,fillx", "[grow,fill]", "[]0"));

		for(int i = 0; i < lines.size(); i ++){
			final RowWidgets rw = new RowWidgets(lines.get(i), rowCounts[i]);
			rows.add(rw);
			rw.leftArrow().addActionListener(e -> rw.useSource());

			final JPanel row = new JPanel(new MigLayout("ins 0,fillx",
				"[grow,fill]0[" + BUTTON_COLUMN_WIDTH + "!]", "[]"));
			if(i > 0)
				row.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_SEPARATOR));
			row.add(rw.sourceArea(), "growx,aligny top");
			row.add(rw.leftArrow(), "aligny top");
			panel.add(row, "growx");
		}

		return panel;
	}

	private JPanel buildResultColumn(){
		final JPanel panel = new JPanel(new MigLayout("ins 0,wrap 1,fillx", "[grow,fill]", "[]0"));

		for(int i = 0; i < rows.size(); i ++){
			final RowWidgets rw = rows.get(i);
			rw.clearButton().addActionListener(e -> rw.clearResult());

			final JPanel row = new JPanel(new MigLayout("ins 0,fillx",
				"[grow,fill]0[" + BUTTON_COLUMN_WIDTH + "!]", "[]"));
			if(i > 0)
				row.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_SEPARATOR));
			row.add(rw.resultArea(), "growx,aligny top");
			row.add(rw.clearButton(), "aligny top");
			panel.add(row, "growx");
		}

		return panel;
	}

	private JPanel buildTargetColumn(){
		final JPanel panel = new JPanel(new MigLayout("ins 0,wrap 1,fillx", "[grow,fill]", "[]0"));

		for(int i = 0; i < rows.size(); i ++){
			final RowWidgets rw = rows.get(i);
			rw.rightArrow().addActionListener(e -> rw.useTarget());

			final JPanel row = new JPanel(new MigLayout("ins 0,fillx",
				"[" + BUTTON_COLUMN_WIDTH + "!]0[grow,fill]", "[]"));
			if(i > 0)
				row.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_SEPARATOR));
			row.add(rw.rightArrow(), "aligny top");
			row.add(rw.targetArea(), "growx,aligny top");
			panel.add(row, "growx");
		}

		return panel;
	}

	private static JScrollPane createScrollPane(final JPanel content, final String title){
		final JScrollPane scroll = new JScrollPane(content,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setBorder(BorderFactory.createTitledBorder(title));
		scroll.getVerticalScrollBar()
			.setUnitIncrement(16);
		scroll.getHorizontalScrollBar()
			.setUnitIncrement(16);
		return scroll;
	}

	private void revalidateColumns(){
		sourceColumn.revalidate();
		resultColumn.revalidate();
		targetColumn.revalidate();
		sourceColumn.repaint();
		resultColumn.repaint();
		targetColumn.repaint();
	}


	/* ======================================================================
	 *                          Scroll sync
	 * ====================================================================== */

	private static void syncVerticalScroll(final JScrollPane... panes){
		final boolean[] syncing = {false};

		final AdjustmentListener listener = (AdjustmentEvent e) -> {
			if(syncing[0])
				return;

			syncing[0] = true;
			try{
				final int value = e.getValue();
				for(final JScrollPane pane : panes){
					final JScrollBar bar = pane.getVerticalScrollBar();
					if(bar != null && bar.getValue() != value)
						bar.setValue(value);
				}
			}
			finally{
				syncing[0] = false;
			}
		};

		for(final JScrollPane pane : panes){
			final JScrollBar bar = pane.getVerticalScrollBar();
			if(bar != null)
				bar.addAdjustmentListener(listener);
		}
	}


	/* ======================================================================
	 *                          Diff construction
	 * ====================================================================== */

	private void buildLines(final MergeField field){
		final List<FLEFRecord> target = field.targetInstances();
		final List<FLEFRecord> source = field.sourceInstances();

		final int m = source.size();
		final int n = target.size();
		final String[] srcSer = new String[m];
		final String[] tgtSer = new String[n];
		for(int i = 0; i < m; i ++)
			srcSer[i] = serialize(source.get(i));
		for(int j = 0; j < n; j ++)
			tgtSer[j] = serialize(target.get(j));

		final int[][] dp = new int[m + 1][n + 1];
		for(int i = m - 1; i >= 0; i --)
			for(int j = n - 1; j >= 0; j --)
				dp[i][j] = srcSer[i].equals(tgtSer[j])
					? dp[i + 1][j + 1] + 1
					: Math.max(dp[i + 1][j], dp[i][j + 1]);

		int i = 0, j = 0;
		while(i < m && j < n){
			if(srcSer[i].equals(tgtSer[j])){
				lines.add(new MergeLine(source.get(i), target.get(j),
					Status.COMMON, Choice.INCLUDE_TARGET));
				i ++;
				j ++;
			}
			else if(dp[i + 1][j] >= dp[i][j + 1]){
				lines.add(new MergeLine(source.get(i), null,
					Status.SOURCE_ONLY, Choice.INCLUDE_SOURCE));
				i ++;
			}
			else{
				lines.add(new MergeLine(null, target.get(j),
					Status.TARGET_ONLY, Choice.INCLUDE_TARGET));
				j ++;
			}
		}
		while(i < m){
			lines.add(new MergeLine(source.get(i), null,
				Status.SOURCE_ONLY, Choice.INCLUDE_SOURCE));
			i ++;
		}
		while(j < n){
			lines.add(new MergeLine(null, target.get(j),
				Status.TARGET_ONLY, Choice.INCLUDE_TARGET));
			j ++;
		}
	}

	private int[] computeRowCounts(){
		final int[] counts = new int[lines.size()];
		for(int i = 0; i < lines.size(); i ++){
			final MergeLine line = lines.get(i);
			final int s = countLines(textOf(line.source));
			final int r = countLines(textOf(line.result()));
			final int t = countLines(textOf(line.target));
			counts[i] = Math.max(MIN_ROWS, Math.max(Math.max(s, r), t));
		}
		return counts;
	}

	private static int countLines(final String text){
		if(text == null || text.isEmpty())
			return 0;
		int n = 1;
		for(int i = 0; i < text.length(); i ++)
			if(text.charAt(i) == '\n')
				n ++;
		return n;
	}


	/* ======================================================================
	 *                          Buttons
	 * ====================================================================== */

	private JPanel buildButtons(){
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

		final JButton ok = new JButton("OK");
		ok.addActionListener(e -> onOk());
		buttons.add(ok);

		final JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> dispose());
		buttons.add(cancel);

		return buttons;
	}


	/* ======================================================================
	 *                          Validation
	 * ====================================================================== */

	/**
	 * Validates the edited result column and, when it parses and
	 * validates, writes it into the {@link MergeField}.
	 * <p>
	 * The validation is two-step:
	 * <ol>
	 *   <li>the result text is parsed as a FLEF fragment; a syntax
	 *       error stops the process and keeps the dialog open;</li>
	 *   <li>a full copy of the model is built with the target record
	 *       replaced by the same record carrying the parsed fragment in
	 *       place of the field, and the copy is validated with
	 *       {@link FLEFValidator#validateAll}: this checks the schema,
	 *       the cross-references against the rest of the model, and the
	 *       business rules, so the user cannot commit a change that
	 *       would break the file.</li>
	 * </ol>
	 */
	private void onOk(){
		final String resultText = collectResultText();

		final List<FLEFRecord> instances;
		try{
			instances = parseResultText(resultText);
		}
		catch(final IOException | RuntimeException ex){
			showError("Invalid FLEF syntax",
				(ex.getMessage() != null? ex.getMessage(): ex.toString()));
			return;
		}

		final List<String> errors = validateWholeModel(instances);
		if(!errors.isEmpty()){
			showValidationErrors(errors);
			return;
		}

		field.setResultInstances(instances);
		dispose();
	}

	private String collectResultText(){
		final StringBuilder sb = new StringBuilder();
		for(final RowWidgets rw : rows){
			final String text = rw.resultArea().getText();
			if(text != null && !text.isBlank())
				sb.append(text).append('\n');
		}
		return sb.toString();
	}

	/**
	 * Parses the result column as a list of field instances.
	 * <p>
	 * The fragment is wrapped in a synthetic record of the same type as
	 * the target (e.g. {@code individual}), with a placeholder id. The
	 * parser sees a document that looks like a real file:
	 * <pre>
	 *   records {
	 *     individual PARSER_TMP {
	 *       name { ... }
	 *       name { ... }
	 *     }
	 *   }
	 * </pre>
	 * The synthetic record's children are the field instances. This avoids
	 * relying on the parser accepting the field tag as a top-level record,
	 * and it makes the wrapper symmetric with the one used for validation.
	 * If the user has broken the structure (e.g. typed a {@code part { ... }}
	 * without its enclosing {@code name { ... }}), the wrong children are
	 * returned and the schema validation will reject them, pointing at the
	 * mistake.
	 *
	 * @param text the text of the result column
	 * @return the field instances, never {@code null}
	 * @throws IOException on a syntax error in the fragment
	 */
	private List<FLEFRecord> parseResultText(final String text) throws IOException{
		if(text == null || text.isBlank())
			return List.of();

		final String recordTag = (targetRecord != null && targetRecord.getTag() != null
			? targetRecord.getTag()
			: field.tag());
		final String syntheticId = "PARSER_TMP";

		final String wrapped = "records {\n"
			+ recordTag + " " + syntheticId + " {\n"
			+ text + "\n"
			+ "}\n"
			+ "}\n";

		final FLEFModel model = new FLEFParser()
			.parse(wrapped);
		final FLEFRecord synthetic = model.getRecordById(syntheticId);
		if(synthetic == null)
			return List.of();

		return new ArrayList<>(synthetic.getChildren());
	}

	/**
	 * Validates the whole model with the target record replaced by the
	 * same record carrying the edited field.
	 * <p>
	 * The model passed to the validator is a copy: every record is
	 * referenced as-is except the target, which is rebuilt from scratch
	 * with the tag, id, value and children copied explicitly, and the
	 * field being edited swapped for the parsed instances. This means
	 * the validator sees the full record (e.g. {@code individual I1})
	 * with the modified field inside it, plus every other record of
	 * the file, so cardinality, cross-references and business rules are
	 * all checked against the real context.
	 */
	private List<String> validateWholeModel(final List<FLEFRecord> instances){
		if(grammar == null)
			return List.of();

		final FLEFModel copy = new FLEFModel();
		if(model != null && model.getHeader() != null && model.getHeader().hasData())
			copy.setHeader(model.getHeader());

		if(model != null){
			final String targetId = (targetRecord != null? targetRecord.getId(): null);
			for(final FLEFRecord record : model.getRecords()){
				if(targetId != null && Objects.equals(record.getId(), targetId))
					copy.addRecord(buildModifiedTarget(record, instances));
				else
					copy.addRecord(record);
			}
		}
		else if(targetRecord != null)
			copy.addRecord(buildModifiedTarget(targetRecord, instances));

		return new FLEFValidator(grammar).validateAll(copy);
	}

	/**
	 * Builds a shallow copy of the target with the field replaced by
	 * the parsed instances. The target's identity (tag, id, value) is
	 * copied explicitly, because {@code deepCopyTo} is not guaranteed
	 * to reproduce it. Children are referenced as-is; only the field
	 * being edited is omitted and replaced.
	 */
	private FLEFRecord buildModifiedTarget(final FLEFRecord target, final List<FLEFRecord> instances){
		final FLEFRecord result = FLEFRecord.createChildWithTag(target.getTag());
		if(target.getId() != null)
			result.setId(target.getId());
		if(target.getValue() != null)
			result.setValue(target.getValue());

		for(final FLEFRecord child : target.getChildren()){
			if(Objects.equals(child.getTag(), field.tag()))
				continue;
			result.addChild(child);
		}

		for(final FLEFRecord instance : instances)
			result.addChild(instance);

		return result;
	}

	private void showValidationErrors(final List<String> errors){
		final StringBuilder message = new StringBuilder(
			"The result field does not validate against the FLEF schema:\n\n");
		final int shown = Math.min(errors.size(), MAX_ERRORS_SHOWN);
		for(int i = 0; i < shown; i ++)
			message.append("  \u2022 ").append(errors.get(i)).append('\n');
		if(errors.size() > shown)
			message.append("  \u2026 and ").append(errors.size() - shown).append(" more\n");

		JOptionPane.showMessageDialog(this, message.toString(),
			"Invalid result", JOptionPane.ERROR_MESSAGE);
	}

	private void showError(final String title, final String body){
		JOptionPane.showMessageDialog(this, body, title, JOptionPane.ERROR_MESSAGE);
	}


	/* ======================================================================
	 *                          UI helpers
	 * ====================================================================== */

	private static JTextArea createArea(final String text, final int rowCount,
		final boolean editable){
		final JTextArea area = new JTextArea(text);
		area.setEditable(editable);
		area.setLineWrap(false);
		area.setOpaque(true);
		area.setFont(MONO);
		area.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
		area.setRows(rowCount);
		area.setColumns(30);
		return area;
	}

	private static JButton createActionButton(final String text, final String tooltip){
		final JButton button = new JButton(text);
		button.setToolTipText(tooltip);
		button.setFont(MONO.deriveFont(Font.BOLD));
		button.setMargin(new Insets(0, 2, 0, 2));
		button.setFocusable(false);
		button.setPreferredSize(new Dimension(BUTTON_COLUMN_WIDTH, 20));
		return button;
	}


	/* ======================================================================
	 *                          Serialization
	 * ====================================================================== */

	private static String textOf(final FLEFRecord record){
		return (record != null? serialize(record): "");
	}

	private static String serialize(final FLEFRecord record){
		final StringBuilder sb = new StringBuilder();
		serializeInto(record, 0, sb);
		return sb.toString();
	}

	private static void serializeInto(final FLEFRecord record, final int indent, final StringBuilder sb){
		final String prefix = "  ".repeat(indent);
		sb.append(prefix)
			.append(record.getTag() != null? record.getTag(): "?");
		if(record.getId() != null)
			sb.append(' ').append(record.getId());
		if(record.getValue() != null)
			sb.append(' ').append(record.getValue());
		if(record.hasChildren()){
			sb.append(" {\n");
			for(final FLEFRecord child : record.getChildren())
				serializeInto(child, indent + 1, sb);
			sb.append(prefix).append("}\n");
		}
		else
			sb.append('\n');
	}

}
