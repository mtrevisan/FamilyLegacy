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

import io.github.mtrevisan.familylegacy.assistant.gap.GapAnalysis;
import io.github.mtrevisan.familylegacy.assistant.task.TaskProposal;
import io.github.mtrevisan.familylegacy.assistant.task.TaskProposals;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.SortableTable;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * The gaps of the research in one place: what is missing (a birth, a death, a source, a piece of the chronology, an identity to settle),
 * and for each gap the task to create, the searches to make and the repositories that may keep the record. The researcher picks a kind
 * of gap, reads what to search and where, ticks the tasks to create and confirms: nothing is created until then, and cancelling (ESC)
 * leaves the file untouched.
 */
final class GapAnalysisDialog extends JDialog{

	/**
	 * A gap, with the task that would start to fill it ({@code null} if it already is a task, or does not ask for work).
	 *
	 * @param taskId the task created from this dialog ({@code null} if none was)
	 */
	record Row(GapAnalysis.Entry entry, TaskProposal task, boolean exists, String taskId){

		Row(final GapAnalysis.Entry entry, final TaskProposal task, final boolean exists){
			this(entry, task, exists, null);
		}

	}


	private static final String[] COLUMN_KEYS = {
		"assistant.gaps.column.create",
		"assistant.gaps.column.kind",
		"assistant.gaps.column.finding",
		"assistant.gaps.column.task"
	};
	private static final int COLUMN_CREATE = 0;
	private static final int[] COLUMN_WIDTHS = {60, 150, 560, 110};
	private static final String BULLET = "• ";


	private final FLEFModel model;
	private final Clock clock;

	private final GapTableModel tableModel = new GapTableModel();
	private final JTable table = new SortableTable(tableModel);
	private final JComboBox<Choice> filter = new JComboBox<>();
	private final JLabel status = new JLabel(I18N.t("assistant.gaps.looking"));
	private final JTextArea details = new JTextArea();
	private final JButton inboxButton = new JButton(I18N.t("assistant.tasks.open_inbox"));
	private final JButton createSelectedButton =new JButton(I18N.t("assistant.tasks.create_selected"));
	private final JButton createButton = new JButton(I18N.plural("assistant.tasks.create_ticked", 0));
	/** Shown while the gaps are being looked for. */
	private final JProgressBar progress = new JProgressBar();
	private final JButton openButton =new JButton(I18N.t("assistant.open_records"));
	private final JButton allButton =new JButton(I18N.t("button.select_all"));
	private final JButton noneButton = new JButton(I18N.t("assistant.tasks.select_none"));
	private transient List<Row> rows = List.of();
	private final transient Set<Row> checked = new HashSet<>();
	private boolean loading;
	private int created;


	/**
	 * @param owner the window the dialog belongs to
	 * @param model the model the tasks are added to
	 */
	GapAnalysisDialog(final Window owner, final FLEFModel model){
		this(owner, model, Clock.systemDefaultZone());
	}

	/**
	 * @param owner the window the dialog belongs to
	 * @param model the model the tasks are added to
	 * @param clock dates the creation of the tasks
	 */
	GapAnalysisDialog(final Window owner, final FLEFModel model, final Clock clock){
		super(owner, I18N.t("assistant.gaps.title"), ModalityType.APPLICATION_MODAL);

		this.model = model;
		this.clock = clock;

		setLayout(new BorderLayout(6, 6));
		add(createNorth(), BorderLayout.NORTH);
		add(createCenter(), BorderLayout.CENTER);
		add(createSouth(), BorderLayout.SOUTH);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setPreferredSize(new Dimension(1000, 620));
		ToolDialogs.installEscapeToClose(this);
		pack();
		setLocationRelativeTo(owner);

		updateButtons();
	}


	private JPanel createNorth(){
		filter.addActionListener(e -> {
			if(!loading)
				showRows();
		});

		final JPanel north = new JPanel(new BorderLayout());
		north.setBorder(BorderFactory.createEmptyBorder(6, 8, 0, 8));
		north.add(new JLabel(I18N.t("assistant.gaps.hint")), BorderLayout.NORTH);
		final JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
		line.add(new JLabel(I18N.t("assistant.gaps.filter")));
		line.add(filter);
		north.add(line, BorderLayout.SOUTH);
		return north;
	}

	private JSplitPane createCenter(){
		table.setAutoCreateRowSorter(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(22);
		table.setFillsViewportHeight(true);
		table.getTableHeader().setReorderingAllowed(false);
		for(int column = 0; column < COLUMN_WIDTHS.length; column ++)
			table.getColumnModel().getColumn(column).setPreferredWidth(COLUMN_WIDTHS[column]);
		table.getSelectionModel().addListSelectionListener(e -> {
			if(!e.getValueIsAdjusting()){
				showDetails();
				updateButtons();
			}
		});
		//a double click opens the records the gap is about (a click on the box of the task only ticks it)
		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent e){
				//on the press, not on the click: a click is lost if the mouse moves a little between press and release
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && table.columnAtPoint(e.getPoint()) != COLUMN_CREATE)
					openSelected();
			}
		});

		details.setEditable(false);
		details.setLineWrap(true);
		details.setWrapStyleWord(true);
		details.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		final JScrollPane detailScroll = new JScrollPane(details);
		detailScroll.setBorder(BorderFactory.createTitledBorder(I18N.t("border.details")));

		final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(table), detailScroll);
		split.setResizeWeight(0.5);
		return split;
	}

	private JPanel createSouth(){
		allButton.addActionListener(e -> selectAll(true));
		noneButton.addActionListener(e -> selectAll(false));
		openButton.addActionListener(e -> openSelected());
		createSelectedButton.addActionListener(e -> createSelected());
		createButton.addActionListener(e -> create());
		inboxButton.addActionListener(e -> TaskInbox.open(this, model));
		getRootPane().setDefaultButton(createSelectedButton);

		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
		buttons.add(inboxButton);
		buttons.add(openButton);
		buttons.add(allButton);
		buttons.add(noneButton);
		buttons.add(createButton);
		buttons.add(createSelectedButton);

		final JPanel south = new JPanel(new BorderLayout(8, 0));
		south.setBorder(BorderFactory.createEmptyBorder(0, 8, 2, 4));
		progress.setIndeterminate(true);
		final JPanel statusLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		statusLine.add(status);
		statusLine.add(progress);
		south.add(statusLine, BorderLayout.WEST);
		south.add(buttons, BorderLayout.EAST);
		return south;
	}


	/**
	 * Shows the gaps (call it on the event dispatch thread, when the findings have been looked at). The tasks that can be created are
	 * ticked at first.
	 *
	 * @param rows the gaps, in the order they are listed
	 */
	void setRows(final List<Row> rows){
		progress.setVisible(false);
		this.rows = List.copyOf(rows);
		//nothing is ticked at first: the researcher chooses the tasks to create
		checked.clear();

		loading = true;
		filter.removeAllItems();
		filter.addItem(new Choice(null, I18N.t("assistant.gaps.all") + " (" + rows.size() + ")"));
		for(final GapAnalysis.Gap gap : GapAnalysis.Gap.values()){
			final long count = rows.stream()
				.filter(row -> row.entry().gap() == gap)
				.count();
			if(count > 0)
				filter.addItem(new Choice(gap, gap.label() + " (" + count + ")"));
		}
		loading = false;

		status.setText(rows.isEmpty()? I18N.t("assistant.gaps.none"): I18N.plural("assistant.gaps.found", rows.size()));
		showRows();
	}

	/** Tells that the gaps could not be looked at. */
	void setFailed(){
		progress.setVisible(false);
		status.setText(I18N.t("assistant.gaps.failed"));
		updateButtons();
	}

	/** Shows the gaps of the kind that is chosen. */
	private void showRows(){
		final Choice choice = (Choice)filter.getSelectedItem();
		final GapAnalysis.Gap gap = (choice != null? choice.gap(): null);
		tableModel.setRows(rows.stream()
			.filter(row -> gap == null || row.entry().gap() == gap)
			.toList());
		if(tableModel.getRowCount() > 0)
			table.getSelectionModel().setSelectionInterval(0, 0);
		else
			showDetails();
		updateButtons();
	}

	private void selectAll(final boolean value){
		for(final Row row : tableModel.shown)
			if(row.task() != null){
				if(value)
					checked.add(row);
				else
					checked.remove(row);
			}
		tableModel.fireTableDataChanged();
		updateButtons();
	}

	/** Opens the records of the selected gap (the editor of the record, or a list to open them one after the other). */
	void openSelected(){
		final int index = selectedIndex();
		if(index < 0 || index >= tableModel.shown.size())
			return;

		//the task, if the gap became one here; otherwise the records the gap is about
		final Row row = tableModel.shown.get(index);
		OpenRecordsAction.openAll(this, model, (row.taskId() != null? List.of(row.taskId()): row.entry().finding().recordIds()));
		table.repaint();
	}

	private void updateButtons(){
		final boolean any = (tableModel.getRowCount() > 0);
		openButton.setEnabled(any && table.getSelectedRow() >= 0);
		allButton.setEnabled(any);
		noneButton.setEnabled(any);
		createButton.setEnabled(!checked.isEmpty());
		createButton.setText(I18N.plural("assistant.tasks.create_ticked", checked.size()));
		final int index = selectedIndex();
		createSelectedButton.setEnabled(index >= 0 && index < tableModel.shown.size() && tableModel.shown.get(index).task() != null);
		inboxButton.setEnabled(created > 0);
	}

	/** @return the index (in the list of the rows shown) of the selected gap, or -1 */
	private int selectedIndex(){
		final int viewRow = table.getSelectedRow();
		return (viewRow >= 0? table.convertRowIndexToModel(viewRow): -1);
	}

	/** Shows why the gap, the task, the searches and the repositories of the selected row. */
	private void showDetails(){
		final int index = selectedIndex();
		if(index < 0 || index >= tableModel.shown.size()){
			details.setText(StringUtils.EMPTY);
			return;
		}

		details.setText(describe(tableModel.shown.get(index)));
		details.setCaretPosition(0);
	}

	/** @return what the gap says, the task proposed, the searches and the repositories, in plain text */
	static String describe(final Row row){
		final GapAnalysis.Entry entry = row.entry();
		final StringBuilder sb = new StringBuilder();
		section(sb, I18N.t("assistant.gaps.detail.why"), List.of(entry.finding().explanation()), false);
		if(row.task() != null)
			section(sb, I18N.t("assistant.gaps.detail.task"), List.of(row.task().description()), false);
		section(sb, I18N.t("assistant.gaps.detail.searches"), entry.searches(), true);
		section(sb, I18N.t("assistant.gaps.detail.repositories"), entry.repositories(), true);
		return sb.toString().strip();
	}

	private static void section(final StringBuilder sb, final String title, final List<String> lines, final boolean bullets){
		sb.append(title).append(":\n");
		if(lines.isEmpty())
			sb.append(I18N.t("assistant.gaps.detail.none")).append('\n');
		for(final String line : lines)
			sb.append(bullets? BULLET: StringUtils.EMPTY).append(line).append('\n');
		sb.append('\n');
	}

	/** Creates the task of the selected gap; the gap stays in the list, marked as already a task, and the dialog stays open. */
	void createSelected(){
		final int viewRow = table.getSelectedRow();
		final int index = selectedIndex();
		if(index < 0 || index >= tableModel.shown.size())
			return;

		final Row row = tableModel.shown.get(index);
		if(row.task() == null)
			return;

		markCreated(row, TaskProposals.create(model, row.task(), clock).getId());
		updateButtons();
		//the next row as the researcher sees it
		if(viewRow + 1 < table.getRowCount())
			table.getSelectionModel().setSelectionInterval(viewRow + 1, viewRow + 1);
	}

	/** Creates the tasks that are ticked; the gaps stay in the list, marked as already a task, and the dialog stays open. */
	void create(){
		for(final Row row : List.copyOf(rows))
			if(row.task() != null && checked.contains(row))
				markCreated(row, TaskProposals.create(model, row.task(), clock).getId());
		updateButtons();
	}

	/** The gap now is a task: it is not ticked, and says which task it is. */
	private void markCreated(final Row row, final String taskId){
		created ++;
		final Row done = new Row(row.entry(), null, true, taskId);
		checked.remove(row);
		final List<Row> all = new ArrayList<>(rows);
		all.replaceAll(candidate -> (candidate == row? done: candidate));
		rows = List.copyOf(all);
		final int shownIndex = tableModel.shown.indexOf(row);
		if(shownIndex >= 0){
			tableModel.shown.set(shownIndex, done);
			tableModel.fireTableRowsUpdated(shownIndex, shownIndex);
		}
		if(selectedIndex() == shownIndex)
			showDetails();
	}

	/** @return how many tasks the researcher created */
	int created(){
		return created;
	}

	/** @return the gaps listed (all of them, whatever the filter says) */
	List<Row> rows(){
		return List.copyOf(rows);
	}

	/** Ticks or unticks the task of a gap. */
	void setChecked(final Row row, final boolean value){
		if(row.task() == null)
			return;

		if(value)
			checked.add(row);
		else
			checked.remove(row);
		tableModel.fireTableDataChanged();
		updateButtons();
	}

	/** Chooses the kind of gap to show ({@code null} for all). */
	void showOnly(final GapAnalysis.Gap gap){
		for(int i = 0; i < filter.getItemCount(); i ++)
			if(filter.getItemAt(i).gap() == gap){
				filter.setSelectedIndex(i);
				return;
			}
	}

	/** @return the gaps now shown in the table */
	List<Row> shown(){
		return List.copyOf(tableModel.shown);
	}


	/** An item of the filter. */
	private record Choice(GapAnalysis.Gap gap, String label){

		@Override
		public String toString(){
			return label;
		}

	}


	/** The rows of the table: one gap each, with a box to tick the task. */
	private final class GapTableModel extends AbstractTableModel{

		private transient List<Row> shown = new ArrayList<>();


		void setRows(final List<Row> rows){
			shown = new ArrayList<>(rows);
			fireTableDataChanged();
		}

		@Override
		public int getRowCount(){
			return shown.size();
		}

		@Override
		public int getColumnCount(){
			return COLUMN_KEYS.length;
		}

		@Override
		public String getColumnName(final int column){
			return I18N.t(COLUMN_KEYS[column]);
		}

		@Override
		public Class<?> getColumnClass(final int column){
			return (column == COLUMN_CREATE? Boolean.class: String.class);
		}

		@Override
		public boolean isCellEditable(final int row, final int column){
			return (column == COLUMN_CREATE && shown.get(row).task() != null);
		}

		@Override
		public Object getValueAt(final int index, final int column){
			final Row row = shown.get(index);
			return switch(column){
				case COLUMN_CREATE -> checked.contains(row);
				case 1 -> row.entry().gap().label();
				case 2 -> row.entry().finding().title();
				default -> (row.taskId() != null
					? I18N.tf("assistant.tasks.status.created", row.taskId())
					: I18N.t(row.task() != null? "assistant.gaps.task.new": (row.exists()? "assistant.gaps.task.exists": "assistant.gaps.task.none")));
			};
		}

		@Override
		public void setValueAt(final Object value, final int index, final int column){
			if(column == COLUMN_CREATE)
				setChecked(shown.get(index), (Boolean)value);
		}

	}

}
