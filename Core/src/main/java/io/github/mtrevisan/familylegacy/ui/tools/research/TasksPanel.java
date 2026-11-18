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
package io.github.mtrevisan.familylegacy.ui.tools.research;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.research.OpenTasks;
import io.github.mtrevisan.familylegacy.research.ResearchHelper;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.ResearchTaskRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchTaskHandler;
import org.apache.commons.lang3.StringUtils;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Supplier;


/**
 * The panel of the main window with the research tasks still to be done in the whole file (see {@link OpenTasks}): the earliest due
 * date first, the overdue ones in red.
 * <p>
 * A double click (or Enter) opens the editor of the task; the context menu changes its status without opening it, or opens its
 * question. <em>Only the selected record</em> keeps the tasks of the questions about the record selected in the view. The panel is
 * built again when the model changes ({@link #refresh()}) and when the selection changes ({@link #selectionChanged()}).
 * </p>
 */
public final class TasksPanel extends JPanel{

	private static final String ENUM_STATUS_PREFIX = "enum.research_task.status.";
	private static final String ENUM_PRIORITY_PREFIX = "enum.research_task.priority.";
	private static final String EDIT_ACTION = "edit-task";

	private static final int COLUMN_DUE = 0;
	private static final int COLUMN_DESCRIPTION = 3;
	private static final int COLUMN_QUESTION = 4;
	private static final int COLUMN_TARGET = 5;
	private static final String[] COLUMN_KEYS = {
		"panel.tasks.column.due",
		"panel.tasks.column.priority",
		"panel.tasks.column.status",
		"panel.tasks.column.description",
		"panel.tasks.column.question",
		"panel.tasks.column.target"
	};
	private static final int[] COLUMN_WIDTHS = {90, 70, 90, 320, 220, 200};

	private static final int PREFERRED_HEIGHT = 180;


	private final transient Supplier<FLEFModel> model;
	private final transient Supplier<String> selection;
	private final transient Supplier<LocalDate> today;
	private transient Runnable afterChange = () -> {};
	private transient Runnable onHide = () -> {};

	private final TaskTableModel tableModel = new TaskTableModel();
	private final JTable table = new JTable(tableModel);
	private final JLabel summary = new JLabel();
	private final JCheckBox onlySelection = new JCheckBox(I18N.t("panel.tasks.only_selection"));
	private final JButton newTask = new JButton(I18N.t("panel.tasks.new"));
	/** Opens the editor of a new task, of the given question ({@code null}: of none); the tests replace it. */
	private transient BiConsumer<FLEFModel, FLEFRecord> newTaskOpener = this::openNewTask;


	/**
	 * @param model	The model of the window (it changes when another file is opened).
	 * @param selection	The id of the record selected in the view, or {@code null}.
	 */
	public TasksPanel(final Supplier<FLEFModel> model, final Supplier<String> selection){
		this(model, selection, LocalDate::now);
	}

	/** @param today	What day it is (the tests fix it). */
	TasksPanel(final Supplier<FLEFModel> model, final Supplier<String> selection, final Supplier<LocalDate> today){
		super(new BorderLayout());

		this.model = Objects.requireNonNull(model, "model");
		this.selection = Objects.requireNonNull(selection, "selection");
		this.today = Objects.requireNonNull(today, "today");

		add(createHeader(), BorderLayout.NORTH);
		add(createTable(), BorderLayout.CENTER);
		setPreferredSize(new Dimension(0, PREFERRED_HEIGHT));
		setMinimumSize(new Dimension(0, 0));

		refresh();
	}


	/** @param afterChange	What to do after the panel changed the model (a task saved or its status changed). */
	public void setAfterChange(final Runnable afterChange){
		this.afterChange = Objects.requireNonNull(afterChange, "afterChange");
	}

	/** @param onHide	What the close button of the panel does. */
	public void setOnHide(final Runnable onHide){
		this.onHide = Objects.requireNonNull(onHide, "onHide");
	}


	private JComponent createHeader(){
		final JLabel title = new JLabel(I18N.t("panel.tasks.title"));
		title.setFont(title.getFont().deriveFont(Font.BOLD));

		onlySelection.setToolTipText(I18N.t("panel.tasks.only_selection.tooltip"));
		onlySelection.setFocusable(false);
		onlySelection.addActionListener(e -> refresh());

		newTask.setToolTipText(I18N.t("panel.tasks.new.tooltip"));
		newTask.setFocusable(false);
		newTask.addActionListener(e -> createTask());

		final JButton hide = new JButton("×");
		hide.setToolTipText(I18N.t("panel.tasks.hide.tooltip"));
		hide.setBorderPainted(false);
		hide.setContentAreaFilled(false);
		hide.setFocusable(false);
		hide.addActionListener(e -> onHide.run());

		final JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
		header.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 2));
		header.add(title);
		header.add(Box.createHorizontalStrut(10));
		header.add(summary);
		header.add(Box.createHorizontalGlue());
		header.add(onlySelection);
		header.add(Box.createHorizontalStrut(6));
		header.add(newTask);
		header.add(Box.createHorizontalStrut(2));
		header.add(hide);
		return header;
	}

	private JComponent createTable(){
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoCreateRowSorter(false);
		table.setFillsViewportHeight(true);
		table.getTableHeader().setReorderingAllowed(false);
		for(int column = 0; column < COLUMN_WIDTHS.length; column ++)
			table.getColumnModel().getColumn(column).setPreferredWidth(COLUMN_WIDTHS[column]);
		table.setDefaultRenderer(Object.class, new TaskRenderer());

		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
					editTask(selectedTask());
			}

			@Override
			public void mousePressed(final MouseEvent e){
				showPopup(e);
			}

			@Override
			public void mouseReleased(final MouseEvent e){
				showPopup(e);
			}
		});
		table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
			.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), EDIT_ACTION);
		table.getActionMap()
			.put(EDIT_ACTION, new AbstractAction(){
				@Override
				public void actionPerformed(final ActionEvent e){
					editTask(selectedTask());
				}
			});

		final JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor()));
		return scroll;
	}

	private static Color borderColor(){
		final Color color = UIManager.getColor("Separator.foreground");
		return (color != null? color: Color.GRAY);
	}


	/** Builds the list again from the model (call it on the event dispatch thread), keeping the selected task selected. */
	public void refresh(){
		final OpenTasks.Task selected = selectedTask();
		final String selectedId = (selected != null? selected.record().getId(): null);

		final FLEFModel current = model.get();
		final List<OpenTasks.Task> all = (current != null? OpenTasks.list(current): List.of());
		final String selectionId = selection.get();
		final List<OpenTasks.Task> shown;
		if(onlySelection.isSelected()){
			shown = new ArrayList<>();
			for(final OpenTasks.Task task : all)
				if(task.concerns(selectionId))
					shown.add(task);
		}
		else
			shown = all;
		tableModel.setTasks(shown, current);

		updateSummary(shown, selectionId);

		if(selectedId != null)
			for(int row = 0; row < shown.size(); row ++)
				if(selectedId.equals(shown.get(row).record().getId())){
					table.getSelectionModel().setSelectionInterval(row, row);
					break;
				}
	}

	/** Tells the panel that the record selected in the view changed (the list changes only if it shows the tasks of the selection). */
	public void selectionChanged(){
		if(onlySelection.isSelected())
			refresh();
	}

	private void updateSummary(final List<OpenTasks.Task> shown, final String selectionId){
		final LocalDate day = today.get();
		int overdue = 0;
		for(final OpenTasks.Task task : shown)
			if(task.isOverdue(day))
				overdue ++;

		final StringJoiner text = new StringJoiner(", ");
		text.add(I18N.plural("panel.tasks.summary", shown.size()));
		if(overdue > 0)
			text.add(I18N.plural("panel.tasks.summary.overdue", overdue));
		if(onlySelection.isSelected() && selectionId == null)
			text.add(I18N.t("panel.tasks.no_selection"));
		summary.setText(text.toString());
		summary.setForeground(overdue > 0? ToolDialogs.ERROR_COLOR: UIManager.getColor("Label.foreground"));
	}

	/** @return	The tasks listed, in their order. */
	List<OpenTasks.Task> shownTasks(){
		return List.copyOf(tableModel.tasks);
	}

	/** @return	The text of the summary next to the title. */
	String summaryText(){
		return summary.getText();
	}

	/** Keeps only the tasks of the questions about the selected record, or shows them all. */
	void setOnlySelection(final boolean only){
		onlySelection.setSelected(only);
		refresh();
	}

	private OpenTasks.Task selectedTask(){
		final int row = table.getSelectedRow();
		return (row >= 0 && row < tableModel.tasks.size()? tableModel.tasks.get(row): null);
	}


	/* ======================================================================
	 *                          Editing
	 * ====================================================================== */

	private void showPopup(final MouseEvent e){
		if(!e.isPopupTrigger())
			return;

		final int row = table.rowAtPoint(e.getPoint());
		if(row < 0)
			return;

		table.getSelectionModel().setSelectionInterval(row, row);
		final OpenTasks.Task task = selectedTask();
		if(task != null)
			createPopup(task).show(table, e.getX(), e.getY());
	}

	/** @return	The context menu of a task: edit it, change its status, open its questions. */
	JPopupMenu createPopup(final OpenTasks.Task task){
		final JPopupMenu menu = new JPopupMenu();

		final JMenuItem edit = new JMenuItem(I18N.t("panel.tasks.menu.edit"));
		edit.addActionListener(e -> editTask(task));
		menu.add(edit);

		menu.addSeparator();
		final String current = StringUtils.defaultIfBlank(task.status(), ResearchTaskReader.ENUM_STATUS_OPEN);
		for(final String status : ResearchTaskReader.STATUSES){
			final JMenuItem item = new JMenuItem(I18N.tf("panel.tasks.menu.mark", statusLabel(status)));
			item.setEnabled(!status.equalsIgnoreCase(current));
			item.addActionListener(e -> changeStatus(task, status));
			menu.add(item);
		}

		if(!task.questions().isEmpty()){
			menu.addSeparator();
			for(final FLEFRecord question : task.questions()){
				final JMenuItem item = new JMenuItem(I18N.tf("panel.tasks.menu.edit_question", ResearchHelper.questionTitle(question)));
				item.addActionListener(e -> editQuestion(question));
				menu.add(item);
			}
		}
		return menu;
	}

	/** Changes the status of a task, as the context menu does. */
	void changeStatus(final OpenTasks.Task task, final String status){
		if(OpenTasks.changeStatus(task.record(), status))
			changed();
	}

	private void editTask(final OpenTasks.Task task){
		if(task != null)
			show(ResearchTaskHandler.getInstance()
				.createEditDialog(owner(), model.get(), task.record()));
	}

	private void editQuestion(final FLEFRecord question){
		show(ResearchQuestionHandler.getInstance()
			.createEditDialog(owner(), model.get(), question));
	}

	/**
	 * Creates a task, of the research question about the record selected in the view when there is one: with a single question under
	 * way it is filled in, with several the user picks one (or none) from a menu under the button.
	 */
	void createTask(){
		final FLEFModel current = model.get();
		if(current == null)
			return;

		final List<FLEFRecord> questions = OpenTasks.questionsAbout(current, selection.get());
		if(questions.size() <= 1)
			newTaskOpener.accept(current, (questions.isEmpty()? null: questions.getFirst()));
		else
			createQuestionChooser(current, questions)
				.show(newTask, 0, newTask.getHeight());
	}

	/** @return	The menu that asks which question a new task serves: one item for each question, then one for none. */
	JPopupMenu createQuestionChooser(final FLEFModel current, final List<FLEFRecord> questions){
		final JPopupMenu menu = new JPopupMenu();
		for(final FLEFRecord question : questions){
			final JMenuItem item = new JMenuItem(I18N.tf("panel.tasks.new.of_question", ResearchHelper.questionTitle(question)));
			item.addActionListener(e -> newTaskOpener.accept(current, question));
			menu.add(item);
		}
		menu.addSeparator();
		final JMenuItem none = new JMenuItem(I18N.t("panel.tasks.new.without_question"));
		none.addActionListener(e -> newTaskOpener.accept(current, null));
		menu.add(none);
		return menu;
	}

	private void openNewTask(final FLEFModel current, final FLEFRecord question){
		final ResearchTaskRecordDialog dialog = ResearchTaskRecordDialog.createNew(owner(), current);
		if(question != null)
			dialog.withQuestion(question);
		show(dialog);
	}

	/** Replaces what opens the editor of a new task (the tests cannot show a modal dialog). */
	void setNewTaskOpener(final BiConsumer<FLEFModel, FLEFRecord> newTaskOpener){
		this.newTaskOpener = Objects.requireNonNull(newTaskOpener, "newTaskOpener");
	}

	private void show(final RecordDialog dialog){
		if(dialog == null)
			return;

		dialog.setVisible(true);
		if(dialog.isSaved())
			changed();
	}

	private void changed(){
		afterChange.run();
		refresh();
	}

	private Window owner(){
		return SwingUtilities.getWindowAncestor(this);
	}


	private static String statusLabel(final String status){
		return (StringUtils.isBlank(status)? StringUtils.EMPTY: I18N.t(ENUM_STATUS_PREFIX + status, status));
	}

	private static String priorityLabel(final String priority){
		return (StringUtils.isBlank(priority)? StringUtils.EMPTY: I18N.t(ENUM_PRIORITY_PREFIX + priority, priority));
	}


	/** The rows of the table: one task each, the texts computed once. */
	private static final class TaskTableModel extends AbstractTableModel{

		private transient List<OpenTasks.Task> tasks = List.of();
		private transient List<String[]> rows = List.of();


		void setTasks(final List<OpenTasks.Task> tasks, final FLEFModel model){
			this.tasks = List.copyOf(tasks);
			final List<String[]> texts = new ArrayList<>(tasks.size());
			for(final OpenTasks.Task task : tasks)
				texts.add(texts(task, model));
			rows = texts;
			fireTableDataChanged();
		}

		private static String[] texts(final OpenTasks.Task task, final FLEFModel model){
			final StringJoiner questions = new StringJoiner("; ");
			for(final FLEFRecord question : task.questions())
				questions.add(ResearchHelper.questionTitle(question));

			final StringJoiner targets = new StringJoiner("; ");
			for(final String targetId : task.targetIds()){
				final FLEFRecord target = (model != null? model.getRecordById(targetId): null);
				targets.add(target != null? RecordDisplay.describe(target, model): targetId);
			}

			return new String[]{
				(task.due() != null? task.due().toString(): StringUtils.EMPTY),
				priorityLabel(task.priority()),
				statusLabel(StringUtils.defaultIfBlank(task.status(), ResearchTaskReader.ENUM_STATUS_OPEN)),
				StringUtils.defaultIfBlank(task.description(), task.record().getId()),
				questions.toString(),
				targets.toString()
			};
		}

		@Override
		public int getRowCount(){
			return rows.size();
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
		public Object getValueAt(final int row, final int column){
			return rows.get(row)[column];
		}

	}


	/** Writes the due date of an overdue task in red, and shows the whole text of a cell as its tooltip. */
	private final class TaskRenderer extends DefaultTableCellRenderer{

		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
				final boolean hasFocus, final int row, final int column){
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			final OpenTasks.Task task = (row < tableModel.tasks.size()? tableModel.tasks.get(row): null);
			final boolean overdue = (task != null && task.isOverdue(today.get()));
			setForeground(isSelected? table.getSelectionForeground(): (overdue? ToolDialogs.ERROR_COLOR: table.getForeground()));
			setFont(table.getFont().deriveFont(overdue && column == COLUMN_DUE? Font.BOLD: Font.PLAIN));

			final String text = (value != null? value.toString(): StringUtils.EMPTY);
			if(overdue && column == COLUMN_DUE)
				setToolTipText(I18N.tf("panel.tasks.overdue.tooltip", text));
			else
				setToolTipText(StringUtils.isNotBlank(text) && (column == COLUMN_DESCRIPTION || column == COLUMN_QUESTION
					|| column == COLUMN_TARGET)? text: null);
			return this;
		}

	}

}
