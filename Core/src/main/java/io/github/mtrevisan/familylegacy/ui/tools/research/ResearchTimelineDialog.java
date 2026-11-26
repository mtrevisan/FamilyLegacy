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
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.research.OpenTasks;
import io.github.mtrevisan.familylegacy.research.ResearchTimeline;
import io.github.mtrevisan.familylegacy.sources.SourceHelper;
import io.github.mtrevisan.familylegacy.ui.bindings.ComboBoxSearch;
import io.github.mtrevisan.familylegacy.ui.components.dates.DatePicker;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.SortableTable;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * The timeline of the research (see {@link ResearchTimeline}): what was done, in chronological order, with filters by individual, family,
 * source, repository and date range. A double-click (or "Open record") opens the record the entry is about; the timeline is made again
 * after it has been edited.
 */
public final class ResearchTimelineDialog extends JDialog{

	private static final int DATE_FIELD_COLUMNS = 9;

	/** An item of a filter list: the record, and what the user reads. */
	private record Choice(String id, String label){

		@Override
		public String toString(){
			return label;
		}

	}


	private final ToolContext context;
	private final TimelineTableModel tableModel = new TimelineTableModel();
	private final JTable table = new SortableTable(tableModel);
	private final JComboBox<Choice> individuals = new JComboBox<>();
	private final JComboBox<Choice> families = new JComboBox<>();
	private final JComboBox<Choice> sources = new JComboBox<>();
	private final JComboBox<Choice> repositories = new JComboBox<>();
	private final JTextField from = new JTextField(DATE_FIELD_COLUMNS);
	private final JTextField to = new JTextField(DATE_FIELD_COLUMNS);
	private final JLabel statusLabel = new JLabel(StringUtils.SPACE);
	private List<ResearchTimeline.Entry> all = List.of();
	private boolean loading;


	public ResearchTimelineDialog(final ToolContext context){
		super(context.owner(), I18N.t("dialog.research_timeline.title"), ModalityType.APPLICATION_MODAL);

		this.context = context;

		setLayout(new BorderLayout(6, 6));
		add(createFilters(), BorderLayout.NORTH);
		add(createTable(), BorderLayout.CENTER);
		add(createFooter(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(1200, 650));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());

		reload();
	}


	private JPanel createFilters(){
		//a grid: the labels and the fields of the rows are aligned
		final JPanel filters = new JPanel(new GridBagLayout());
		filters.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

		addFilter(filters, 0, 0, "dialog.research_timeline.individual", individuals);
		addFilter(filters, 0, 2, "dialog.research_timeline.family", families);
		addFilter(filters, 1, 0, "dialog.research_timeline.source", sources);
		addFilter(filters, 1, 2, "dialog.research_timeline.repository", repositories);
		addField(filters, 2, 0, "dialog.research_timeline.from", DatePicker.withPicker(dateField(from)));
		addField(filters, 2, 2, "dialog.research_timeline.to", DatePicker.withPicker(dateField(to)));
		return filters;
	}

	private void addFilter(final JPanel panel, final int row, final int column, final String labelKey, final JComboBox<Choice> combo){
		combo.setPrototypeDisplayValue(new Choice(null, "M".repeat(28)));
		ComboBoxSearch.searchable(combo);
		combo.addActionListener(e -> applyFilter());
		addField(panel, row, column, labelKey, combo);
	}

	private static void addField(final JPanel panel, final int row, final int column, final String labelKey, final JComponent field){
		final GridBagConstraints label = new GridBagConstraints();
		label.gridx = column;
		label.gridy = row;
		label.anchor = GridBagConstraints.WEST;
		label.insets = new Insets(3, 6, 3, 4);
		panel.add(new JLabel(I18N.l(labelKey)), label);

		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = column + 1;
		constraints.gridy = row;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.fill = (field instanceof JComboBox? GridBagConstraints.HORIZONTAL: GridBagConstraints.NONE);
		constraints.weightx = (field instanceof JComboBox? 1.: 0.);
		constraints.insets = new Insets(3, 0, 3, 6);
		panel.add(field, constraints);
	}

	private JTextField dateField(final JTextField field){
		field.setToolTipText(I18N.t("dialog.research_timeline.date_hint"));
		field.getDocument()
			.addDocumentListener(new DocumentListener(){
				@Override
				public void insertUpdate(final DocumentEvent e){
					applyFilter();
				}

				@Override
				public void removeUpdate(final DocumentEvent e){
					applyFilter();
				}

				@Override
				public void changedUpdate(final DocumentEvent e){
					applyFilter();
				}
			});
		return field;
	}

	private JScrollPane createTable(){
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(22);
		//(the order of the model is the chronological one: it is what the table shows until a column is clicked, and the sort is stable,
		//so that equal instants keep the order of the timeline)
		table.setAutoCreateRowSorter(true);
		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2)
					openSelected();
			}
		});
		final TableColumnModel columnModel = table.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(150);
		columnModel.getColumn(1).setPreferredWidth(170);
		columnModel.getColumn(2).setPreferredWidth(80);
		columnModel.getColumn(3).setPreferredWidth(350);
		columnModel.getColumn(4).setPreferredWidth(400);

		final JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createTitledBorder(I18N.t("border.research_timeline")));
		return scroll;
	}

	private JPanel createFooter(){
		return GUIHelper.createStatusFooter(statusLabel, ToolDialogs.createCopyButton(tableModel::toText, table));
	}


	/** Reads the timeline again from the model, and fills the lists of the filters (keeping the choices that still exist). */
	private void reload(){
		final FLEFModel model = context.model();
		all = ResearchTimeline.build(model);

		loading = true;
		try{
			refill(individuals, choices(model, all, ResearchTimeline.Entry::involved, ResearchTimelineDialog::isIndividual));
			refill(families, choices(model, all, ResearchTimeline.Entry::involved, ResearchTimelineDialog::isFamily));
			refill(sources, choices(model, all, ResearchTimeline.Entry::sourceIds, record -> true));
			refill(repositories, choices(model, all, ResearchTimeline.Entry::repositoryIds, record -> true));
		}
		finally{
			loading = false;
		}
		applyFilter();
	}

	private static boolean isIndividual(final FLEFRecord record){
		return IndividualReader.isOfType(record);
	}

	private static boolean isFamily(final FLEFRecord record){
		return (GroupReader.isOfType(record) || RelationshipReader.isOfType(record) && RelationshipReader.isTypePartner(RelationshipReader.extractType(record)));
	}

	/** The records the entries are about, by what the user reads. */
	private static List<Choice> choices(final FLEFModel model, final List<ResearchTimeline.Entry> entries,
			final Function<ResearchTimeline.Entry, Set<String>> idsOf,
			final Predicate<FLEFRecord> accepts){
		final Map<String, Choice> byId = new LinkedHashMap<>();
		for(final ResearchTimeline.Entry entry : entries)
			for(final String id : idsOf.apply(entry)){
				if(byId.containsKey(id))
					continue;

				final FLEFRecord record = model.getRecordById(id);
				if(record != null && accepts.test(record))
					byId.put(id, new Choice(id, labelOf(model, record)));
			}

		final List<Choice> choices = new ArrayList<>(byId.values());
		choices.sort(Comparator.comparing(Choice::label, String.CASE_INSENSITIVE_ORDER)
			.thenComparing(Choice::id));
		return choices;
	}

	private static String labelOf(final FLEFModel model, final FLEFRecord record){
		if(IndividualReader.isOfType(record))
			return IndividualReader.displayText(record);
		if(GroupReader.isOfType(record))
			return StringUtils.defaultIfBlank(GroupReader.displayName(record), record.getId());
		if(RelationshipReader.isOfType(record))
			return name(model, FLEFRecordHelper.getChildValue(record, RelationshipReader.TAG_SUBJECT_INDIVIDUAL)) + " & "
				+ name(model, FLEFRecordHelper.getChildValue(record, RelationshipReader.TAG_TARGET_INDIVIDUAL));
		if(SourceReader.isOfType(record))
			return SourceHelper.sourceTitle(record);
		return SourceHelper.repositoryName(record);
	}

	private static String name(final FLEFModel model, final String individualId){
		final FLEFRecord individual = (individualId != null? model.getRecordById(individualId): null);
		return (individual != null? IndividualReader.displayText(individual): StringUtils.defaultString(individualId));
	}

	private void refill(final JComboBox<Choice> combo, final List<Choice> choices){
		final Choice selected = (Choice)combo.getSelectedItem();
		combo.removeAllItems();
		combo.addItem(new Choice(null, I18N.t("dialog.research_timeline.all")));
		choices.forEach(combo::addItem);
		if(selected != null && selected.id() != null)
			for(final Choice choice : choices)
				if(choice.id().equals(selected.id()))
					combo.setSelectedItem(choice);
	}

	private void applyFilter(){
		if(loading)
			return;

		final ResearchTimeline.Filter filter = new ResearchTimeline.Filter(idOf(individuals), idOf(families), idOf(sources),
			idOf(repositories), OpenTasks.parseDue(from.getText()), OpenTasks.parseDue(to.getText()));
		tableModel.setRows(ResearchTimeline.filter(all, filter));
		statusLabel.setText(I18N.tf("text.research_timeline.entries", tableModel.getRowCount()));
	}

	private static String idOf(final JComboBox<Choice> combo){
		final Choice choice = (Choice)combo.getSelectedItem();
		return (choice != null? choice.id(): null);
	}

	private void openSelected(){
		final int selected = table.getSelectedRow();
		if(selected < 0)
			return;

		final ResearchTimeline.Entry entry = tableModel.getRow(table.convertRowIndexToModel(selected));
		final FLEFRecord record = context.model().getRecordById(entry.recordId());
		if(record == null)
			return;

		ToolDialogs.openRecordEditor(this, context.model(), record);

		reload();
	}


	private static final class TimelineTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {I18N.t("column.date"), I18N.t("column.event"), I18N.t("column.id"), I18N.t("column.title"),
			I18N.t("column.description")};

		private final List<ResearchTimeline.Entry> rows = new ArrayList<>();


		void setRows(final List<ResearchTimeline.Entry> rows){
			this.rows.clear();
			this.rows.addAll(rows);
			fireTableDataChanged();
		}

		ResearchTimeline.Entry getRow(final int index){
			return rows.get(index);
		}

		String toText(){
			final StringBuilder text = new StringBuilder();
			for(int row = 0; row < rows.size(); row ++){
				for(int column = 0; column < COLUMNS.length; column ++){
					if(column > 0)
						text.append('\t');
					text.append(getValueAt(row, column));
				}
				text.append(System.lineSeparator());
			}
			return text.toString();
		}

		@Override public int getRowCount(){ return rows.size(); }
		@Override public int getColumnCount(){ return COLUMNS.length; }
		@Override public String getColumnName(final int column){ return COLUMNS[column]; }

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex){
			final ResearchTimeline.Entry entry = rows.get(rowIndex);
			return switch(columnIndex){
				case 0 -> StringUtils.defaultString(dateOf(entry));
				case 1 -> I18N.t("enum.research_timeline.kind." + entry.kind().name());
				case 2 -> entry.recordId();
				case 3 -> entry.title();
				case 4 -> entry.detail();
				default -> StringUtils.EMPTY;
			};
		}

		/** @return	The day, followed by the time if the audit has it (the seconds are of no use to who reads). */
		private static String dateOf(final ResearchTimeline.Entry entry){
			final LocalDate day = entry.day();
			if(day == null)
				return entry.when();

			final String when = entry.when();
			final String time = (when.length() > 10? StringUtils.left(StringUtils.substring(when, 11), 5): StringUtils.EMPTY);
			return (time.isEmpty() || "00:00".equals(time)? day.toString(): day + " " + time);
		}
	}

}
