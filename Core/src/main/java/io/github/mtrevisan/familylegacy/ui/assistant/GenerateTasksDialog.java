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

import io.github.mtrevisan.familylegacy.assistant.task.TaskProposal;
import io.github.mtrevisan.familylegacy.assistant.task.TaskProposals;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.SortableTable;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;


/**
 * The tasks the Research Assistant proposes from its findings, to review before they exist: the researcher ticks the ones to create (all
 * are ticked at first), reads why each one is proposed, and confirms. Nothing is created until then, and cancelling (ESC) leaves the file
 * untouched.
 */
final class GenerateTasksDialog extends JDialog{

	private static final String[] COLUMN_KEYS = {
		"assistant.tasks.column.create",
		"assistant.tasks.column.topic",
		"assistant.tasks.column.priority",
		"assistant.tasks.column.task",
		"assistant.tasks.column.about",
		"assistant.tasks.column.due",
		"assistant.tasks.column.status"
	};
	private static final int COLUMN_CREATE = 0;
	private static final int COLUMN_STATUS = 6;
	private static final int[] COLUMN_WIDTHS = {60, 130, 70, 400, 220, 90, 110};


	private final FLEFModel model;
	private final Clock clock;

	private final ProposalTableModel tableModel = new ProposalTableModel();
	private final JTable table = new SortableTable(tableModel);
	private final JLabel status = new JLabel(I18N.t("assistant.tasks.looking"));
	private final JTextArea rationale = new JTextArea();
	private final JButton inboxButton = new JButton(I18N.t("assistant.tasks.open_inbox"));
	private final JButton createSelectedButton =new JButton(I18N.t("assistant.tasks.create_selected"));
	private final JButton createButton = new JButton(I18N.plural("assistant.tasks.create_ticked", 0));
	private final JButton allButton = new JButton(I18N.t("button.select_all"));
	private final JButton noneButton = new JButton(I18N.t("assistant.tasks.select_none"));
	private int created;


	/**
	 * @param owner the window the dialog belongs to
	 * @param model the model the tasks are added to
	 */
	GenerateTasksDialog(final Window owner, final FLEFModel model){
		this(owner, model, Clock.systemDefaultZone());
	}

	/**
	 * @param owner the window the dialog belongs to
	 * @param model the model the tasks are added to
	 * @param clock dates the creation of the tasks
	 */
	GenerateTasksDialog(final Window owner, final FLEFModel model, final Clock clock){
		super(owner, I18N.t("assistant.tasks.title"), ModalityType.APPLICATION_MODAL);

		this.model = model;
		this.clock = clock;

		setLayout(new BorderLayout(6, 6));
		add(createNorth(), BorderLayout.NORTH);
		add(createCenter(), BorderLayout.CENTER);
		add(createSouth(), BorderLayout.SOUTH);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setPreferredSize(new Dimension(1000, 560));
		ToolDialogs.installEscapeToClose(this);
		pack();
		setLocationRelativeTo(owner);

		updateButtons();
	}


	private JPanel createNorth(){
		final JPanel north = new JPanel(new BorderLayout());
		north.setBorder(BorderFactory.createEmptyBorder(6, 8, 0, 8));
		north.add(new JLabel(I18N.t("assistant.tasks.hint")), BorderLayout.NORTH);
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
				showRationale();
				updateButtons();
			}
		});
		//a double click opens the records the task is about (a click on the box only ticks it)
		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent e){
				//on the press, not on the click: a click is lost if the mouse moves a little between press and release
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && table.columnAtPoint(e.getPoint()) != COLUMN_CREATE)
					openSelected();
			}
		});

		rationale.setEditable(false);
		rationale.setLineWrap(true);
		rationale.setWrapStyleWord(true);
		rationale.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		final JScrollPane rationaleScroll = new JScrollPane(rationale);
		rationaleScroll.setBorder(BorderFactory.createTitledBorder(I18N.t("assistant.tasks.rationale")));

		final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(table), rationaleScroll);
		split.setResizeWeight(0.75);
		return split;
	}

	private JPanel createSouth(){
		allButton.addActionListener(e -> tableModel.selectAll(true));
		noneButton.addActionListener(e -> tableModel.selectAll(false));
		createSelectedButton.addActionListener(e -> createSelected());
		createButton.addActionListener(e -> create());
		inboxButton.addActionListener(e -> TaskInbox.open(this, model));
		getRootPane().setDefaultButton(createSelectedButton);

		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
		buttons.add(inboxButton);
		buttons.add(allButton);
		buttons.add(noneButton);
		buttons.add(createButton);
		buttons.add(createSelectedButton);

		final JPanel south = new JPanel(new BorderLayout(8, 0));
		south.setBorder(BorderFactory.createEmptyBorder(0, 8, 2, 4));
		south.add(status, BorderLayout.WEST);
		south.add(buttons, BorderLayout.EAST);
		return south;
	}


	/**
	 * Shows the proposals (call it on the event dispatch thread, when the findings have been looked at).
	 *
	 * @param proposals the tasks proposed
	 */
	void setProposals(final List<TaskProposal> proposals){
		tableModel.setProposals(proposals, model);
		status.setText(proposals.isEmpty()
			? I18N.t("assistant.tasks.none")
			: I18N.plural("assistant.tasks.found", proposals.size()));
		if(!proposals.isEmpty())
			table.getSelectionModel().setSelectionInterval(0, 0);
		updateButtons();
	}

	/** Tells that the findings could not be looked at. */
	void setFailed(){
		status.setText(I18N.t("assistant.tasks.failed"));
		updateButtons();
	}

	private void updateButtons(){
		final boolean any = (tableModel.getRowCount() > 0);
		allButton.setEnabled(any);
		noneButton.setEnabled(any);
		final int ticked = tableModel.checked();
		createButton.setEnabled(ticked > 0);
		createButton.setText(I18N.plural("assistant.tasks.create_ticked", ticked));
		final int selected = selectedIndex();
		createSelectedButton.setEnabled(selected >= 0 && tableModel.taskIds.get(selected) == null);
		inboxButton.setEnabled(created > 0);
	}

	/** @return the index in the model (not in the view, that may be sorted) of the selected row, or -1 */
	private int selectedIndex(){
		final int viewRow = table.getSelectedRow();
		final int row = (viewRow >= 0? table.convertRowIndexToModel(viewRow): -1);
		return (row >= 0 && row < tableModel.proposals.size()? row: -1);
	}

	private void showRationale(){
		final int row = selectedIndex();
		rationale.setText(row >= 0? tableModel.proposals.get(row).rationale(): StringUtils.EMPTY);
		rationale.setCaretPosition(0);
	}

	/** Creates the task of the selected row, which stays in the list marked as created; the dialog stays, with the next row selected. */
	void createSelected(){
		final int viewRow = table.getSelectedRow();
		final int row = selectedIndex();
		if(row < 0 || tableModel.taskIds.get(row) != null)
			return;

		tableModel.create(row);
		afterCreating();
		//the next row as the researcher sees it
		if(viewRow + 1 < table.getRowCount())
			table.getSelectionModel().setSelectionInterval(viewRow + 1, viewRow + 1);
	}

	/** Creates the tasks that are ticked, which stay in the list marked as created; the dialog stays. */
	void create(){
		boolean any = false;
		for(int row = 0; row < tableModel.proposals.size(); row ++)
			if(tableModel.checked.get(row) && tableModel.taskIds.get(row) == null){
				tableModel.create(row);
				any = true;
			}
		if(any)
			afterCreating();
	}

	/** Shows what is left to create and what was created, and where to find it. */
	private void afterCreating(){
		final int left = tableModel.pending();
		status.setText((left > 0? I18N.plural("assistant.tasks.found", left) + " · ": StringUtils.EMPTY)
			+ I18N.plural(left > 0? "assistant.tasks.created_so_far": "assistant.tasks.all_created", created));
		showRationale();
		updateButtons();
	}

	/** Opens the task of the selected row (the one created, if it was), or else the records the proposal is about. */
	void openSelected(){
		final int row = selectedIndex();
		if(row < 0)
			return;

		final String taskId = tableModel.taskIds.get(row);
		OpenRecordsAction.openAll(this, model, (taskId != null? List.of(taskId): tableModel.proposals.get(row).relatedIds()));
		table.repaint();
	}

	/** @return the identifiers of the tasks created, in the order of the rows */
	List<String> taskIds(){
		return tableModel.taskIds.stream()
			.filter(Objects::nonNull)
			.toList();
	}

	/** @return how many proposals are still to create */
	int pending(){
		return tableModel.pending();
	}

	/** @return how many tasks the researcher created */
	int created(){
		return created;
	}

	/** @return the tasks proposed, in the order they are listed */
	List<TaskProposal> proposals(){
		return List.copyOf(tableModel.proposals);
	}

	/** Ticks or unticks a task of the list. */
	void setChecked(final int row, final boolean checked){
		tableModel.setValueAt(checked, row, COLUMN_CREATE);
	}


	/** The rows of the table: one proposal each, with a box to tick. */
	private final class ProposalTableModel extends AbstractTableModel{

		private transient List<TaskProposal> proposals = new ArrayList<>();
		private transient List<String[]> texts = new ArrayList<>();
		private final transient List<Boolean> checked = new ArrayList<>();


		/** The task each row became ({@code null} while it is only a proposal). */
		private final transient List<String> taskIds = new ArrayList<>();


		/** Creates the task of the row: the row stays, unticked, and says which task it became. */
		void create(final int row){
			final FLEFRecord task = TaskProposals.create(model, proposals.get(row), clock);
			taskIds.set(row, task.getId());
			checked.set(row, Boolean.FALSE);
			created ++;
			fireTableRowsUpdated(row, row);
		}

		int pending(){
			return (int)taskIds.stream()
				.filter(Objects::isNull)
				.count();
		}

		void setProposals(final List<TaskProposal> proposals, final FLEFModel model){
			this.proposals = new ArrayList<>(proposals);
			final List<String[]> rows = new ArrayList<>(proposals.size());
			checked.clear();
			taskIds.clear();
			for(final TaskProposal proposal : proposals){
				final StringJoiner about = new StringJoiner("; ");
				for(final String id : proposal.relatedIds()){
					final FLEFRecord record = model.getRecordById(id);
					about.add(record != null? RecordDisplay.describe(record, model): id);
				}
				rows.add(new String[]{
					proposal.topic().label(),
					I18N.t("enum.research_task.priority." + proposal.priority(), proposal.priority()),
					proposal.description(),
					about.toString(),
					StringUtils.defaultString(proposal.dueDate())
				});
				//nothing is ticked at first: with thousands of proposals the researcher chooses, and does not have to untick
				checked.add(Boolean.FALSE);
				taskIds.add(null);
			}
			texts = rows;
			fireTableDataChanged();
		}

		void selectAll(final boolean value){
			for(int i = 0; i < checked.size(); i ++)
				checked.set(i, value && taskIds.get(i) == null);
			fireTableDataChanged();
			updateButtons();
		}

		int checked(){
			return (int)checked.stream()
				.filter(Boolean::booleanValue)
				.count();
		}

		@Override
		public int getRowCount(){
			return proposals.size();
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
			return (column == COLUMN_CREATE && taskIds.get(row) == null);
		}

		@Override
		public Object getValueAt(final int row, final int column){
			if(column == COLUMN_CREATE)
				return checked.get(row);
			if(column == COLUMN_STATUS)
				return (taskIds.get(row) != null? I18N.tf("assistant.tasks.status.created", taskIds.get(row)): I18N.t("assistant.tasks.status.new"));
			return texts.get(row)[column - 1];
		}

		@Override
		public void setValueAt(final Object value, final int row, final int column){
			if(column == COLUMN_CREATE && taskIds.get(row) == null){
				checked.set(row, (Boolean)value);
				fireTableCellUpdated(row, column);
				updateButtons();
			}
		}

	}

}
