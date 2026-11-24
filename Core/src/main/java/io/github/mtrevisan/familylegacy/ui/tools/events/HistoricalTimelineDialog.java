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
package io.github.mtrevisan.familylegacy.ui.tools.events;

import io.github.mtrevisan.familylegacy.groups.LifespanComparisonBuilder;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.temporal.HistoricalScope;
import io.github.mtrevisan.familylegacy.temporal.HistoricalTimeline;
import io.github.mtrevisan.familylegacy.temporal.Lifetime;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionModel;
import io.github.mtrevisan.familylegacy.temporal.TemporalSvgWriter;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalProjectionPanel;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.SavedFileNotice;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Dialog that puts the life of individuals against history, in two tabs:
 * <ul>
 *   <li><b>Life and history</b>: the events and attributes of one individual and, in the same list, the historic events and the cultural
 *   norms of its time, with the age of the individual at each date (see {@link HistoricalTimeline}); above the list, the strip of its
 *   life against the historic context of its places. The historic events can be limited by how far from its places they happened (see
 *   {@link HistoricalScope}); the ones that a context impact links to the individual are always there.</li>
 *   <li><b>Lifespan comparison</b>: several individuals on a shared axis, with their lives, their events and the historic context of their
 *   places (see {@link LifespanComparisonBuilder}).</li>
 * </ul>
 * Both views can be saved as an SVG image.
 */
public final class HistoricalTimelineDialog extends JDialog{

	private static final Color CONTEXT_ROW = new Color(250, 244, 232);
	private static final Color LINKED_ROW = new Color(246, 232, 204);

	private final FLEFModel model;

	//the tab of one individual
	private final JTextField personField = new JTextField(StringUtils.EMPTY, 28);
	private final Map<HistoricalScope, JCheckBox> scopeBoxes = new EnumMap<>(HistoricalScope.class);
	private final JCheckBox normsBox = new JCheckBox(I18N.t("dialog.historical_timeline.cultural_norms"), true);
	private final JCheckBox attributesBox = new JCheckBox(I18N.t("dialog.historical_timeline.attributes"), true);
	private final TimelineTableModel tableModel = new TimelineTableModel();
	private final JTable table = new JTable(tableModel);
	private final JLabel statusLabel = new JLabel(StringUtils.SPACE);
	private final TemporalProjectionPanel personStrip;
	private TemporalProjectionModel personModel = TemporalProjectionModel.empty();
	private String personId;

	//the tab of the comparison
	private final DefaultListModel<IndividualItem> individuals = new DefaultListModel<>();
	private final JList<IndividualItem> individualList = new JList<>(individuals);
	private final JCheckBox keyEventsBox = new JCheckBox(I18N.t("dialog.historical_timeline.key_events"), false);
	private final JCheckBox comparisonAttributesBox = new JCheckBox(I18N.t("dialog.historical_timeline.attributes"), false);
	private final JCheckBox contextBox = new JCheckBox(I18N.t("dialog.historical_timeline.historic_context"), true);
	private final JCheckBox placeAwareBox = new JCheckBox(I18N.t("dialog.historical_timeline.place_aware"), true);
	private final TemporalProjectionPanel comparisonPanel;
	private TemporalProjectionModel comparisonModel = TemporalProjectionModel.empty();


	/**
	 * @param context	The context of the tool.
	 * @param individualId	The individual to start from, {@code null} for none.
	 */
	public HistoricalTimelineDialog(final ToolContext context, final String individualId){
		super(context.owner(), I18N.t("dialog.historical_timeline.title"), ModalityType.APPLICATION_MODAL);

		model = context.model();

		personStrip = new TemporalProjectionPanel(model)
			.withModelSource(() -> {
				personModel = (personId != null
					? LifespanComparisonBuilder.build(model, List.of(personId), new LifespanComparisonBuilder.Options(false,
						attributesBox.isSelected(), true, !scopeBoxes.get(HistoricalScope.ELSEWHERE).isSelected()))
					: TemporalProjectionModel.empty());
				return personModel;
			});
		comparisonPanel = new TemporalProjectionPanel(model)
			.withModelSource(() -> {
				comparisonModel = LifespanComparisonBuilder.build(model, individualIds(), comparisonOptions());
				return comparisonModel;
			});

		final JTabbedPane tabs = new JTabbedPane();
		GUIHelper.addTab(tabs, I18N.t("dialog.historical_timeline.tab.person"), "timeline_person", createPersonTab());
		GUIHelper.addTab(tabs, I18N.t("dialog.historical_timeline.tab.comparison"), "timeline_comparison", createComparisonTab());

		setLayout(new BorderLayout(6, 6));
		add(tabs, BorderLayout.CENTER);

		setPreferredSize(new Dimension(1200, 740));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());

		final FLEFRecord individual = (individualId != null? model.getRecordById(individualId): null);
		selectPerson(IndividualHandler.isOfType(individual)? individual: null);
		if(personId != null)
			individuals.addElement(new IndividualItem(personId, RecordDisplay.describe(individual, model)));
		reloadPerson();
		reloadComparison();
	}


	/* ======================================================================
	 *                          Tab of one individual
	 * ====================================================================== */

	private JPanel createPersonTab(){
		final JPanel who = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
		who.add(new JLabel(I18N.l("dialog.historical_timeline.individual")));
		personField.setEditable(false);
		who.add(personField);
		final JButton choose = new JButton(I18N.t("button.choose"));
		choose.addActionListener(e -> {
			final FLEFRecord chosen = chooseIndividual();
			if(chosen != null){
				selectPerson(chosen);

				reloadPerson();
			}
		});
		who.add(choose);

		final JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
		final JLabel show = new JLabel(I18N.l("dialog.historical_timeline.scope"));
		show.setToolTipText(I18N.t("dialog.historical_timeline.scope.tooltip"));
		filters.add(show);
		final Set<HistoricalScope> initial = HistoricalTimeline.Options.defaults()
			.scopes();
		for(final HistoricalScope scope : HistoricalScope.values()){
			final JCheckBox box = new JCheckBox(I18N.t(scope.key()), initial.contains(scope));
			box.addActionListener(e -> reloadPerson());
			scopeBoxes.put(scope, box);
			filters.add(box);
		}
		normsBox.addActionListener(e -> reloadPerson());
		attributesBox.addActionListener(e -> reloadPerson());
		filters.add(normsBox);
		filters.add(attributesBox);

		final JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.add(who);
		top.add(filters);

		final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, personStrip, createTable());
		split.setResizeWeight(0.4);
		split.setBorder(null);
		personStrip.setPreferredSize(new Dimension(900, 260));

		final JButton saveSvg = new JButton(I18N.t("button.save_svg"));
		saveSvg.addActionListener(e -> saveSvg(personModel, "historical-timeline.svg"));

		final JPanel tab = new JPanel(new BorderLayout(6, 6));
		tab.add(top, BorderLayout.NORTH);
		tab.add(split, BorderLayout.CENTER);
		tab.add(GUIHelper.createStatusFooter(statusLabel, ToolDialogs.createCopyButton(tableModel::toText), saveSvg),
			BorderLayout.SOUTH);
		return tab;
	}

	private JScrollPane createTable(){
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(22);
		table.setAutoCreateRowSorter(true);
		final TableColumnModel columns = table.getColumnModel();
		final int[] widths = {150, 50, 110, 260, 220, 220, 90, 90};
		for(int i = 0; i < widths.length; i ++)
			columns.getColumn(i)
				.setPreferredWidth(widths[i]);
		table.setDefaultRenderer(Object.class, new RowRenderer());
		table.setDefaultRenderer(DateCell.class, new RowRenderer());
		table.setDefaultRenderer(Integer.class, new RowRenderer());

		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
					openSelected();
			}
		});

		final JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createTitledBorder(I18N.t("border.timeline")));
		return scroll;
	}

	private HistoricalTimeline.Options personOptions(){
		final Set<HistoricalScope> scopes = EnumSet.noneOf(HistoricalScope.class);
		scopeBoxes.forEach((scope, box) -> {
			if(box.isSelected())
				scopes.add(scope);
		});
		return new HistoricalTimeline.Options(scopes, normsBox.isSelected(), attributesBox.isSelected());
	}

	private void selectPerson(final FLEFRecord individual){
		personId = (individual != null? individual.getId(): null);
		personField.setText(individual != null? RecordDisplay.describe(individual, model): StringUtils.EMPTY);
	}

	private void reloadPerson(){
		final HistoricalTimeline.Result result = (personId != null
			? HistoricalTimeline.build(model, personId, personOptions())
			: HistoricalTimeline.Result.empty());

		final List<TimelineRow> rows = new ArrayList<>(result.items().size());
		for(final HistoricalTimeline.Item item : result.items())
			rows.add(new TimelineRow(item,
				new DateCell(item.date(), item.span().minJdn() != Long.MIN_VALUE? item.span().minJdn(): item.span().maxJdn())));
		tableModel.setRows(rows);

		final String count = I18N.tf("text.historical.status", rows.size());
		final Lifetime lifetime = result.lifetime();
		if(personId == null)
			statusLabel.setText(StringUtils.SPACE);
		else if(!lifetime.isKnown())
			statusLabel.setText(I18N.t("text.historical.no_dates"));
		else
			statusLabel.setText(count + " · " + I18N.tf((lifetime.startEstimated() || lifetime.endEstimated()
				? "text.historical.lifetime.estimated": "text.historical.lifetime"), yearOf(lifetime.start()), yearOf(lifetime.end())));

		personStrip.loadProjection();
	}

	private static int yearOf(final long jdn){
		return LocalDate.ofEpochDay(jdn - 2_440_588L)
			.getYear();
	}

	private void openSelected(){
		final int viewRow = table.getSelectedRow();
		if(viewRow < 0)
			return;

		final FLEFRecord record = tableModel.rows.get(table.convertRowIndexToModel(viewRow))
			.item()
			.record();
		if(record == null)
			return;

		ToolDialogs.openRecordEditor(this, model, record);

		reloadPerson();
		reloadComparison();
	}


	/* ======================================================================
	 *                          Tab of the comparison
	 * ====================================================================== */

	private JPanel createComparisonTab(){
		individualList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		individualList.setVisibleRowCount(4);
		final JScrollPane listScroll = new JScrollPane(individualList);
		listScroll.setPreferredSize(new Dimension(380, 90));

		final JButton add = new JButton(I18N.t("dialog.historical_timeline.add"));
		add.addActionListener(e -> {
			final FLEFRecord chosen = chooseIndividual();
			if(chosen == null)
				return;

			for(int i = 0; i < individuals.size(); i ++)
				if(individuals.get(i).id().equals(chosen.getId()))
					return;

			individuals.addElement(new IndividualItem(chosen.getId(), RecordDisplay.describe(chosen, model)));
			reloadComparison();
		});
		final JButton remove = new JButton(I18N.t("dialog.historical_timeline.remove"));
		remove.addActionListener(e -> {
			final int[] selected = individualList.getSelectedIndices();
			for(int i = selected.length - 1; i >= 0; i --)
				individuals.remove(selected[i]);
			reloadComparison();
		});
		final JButton clear = new JButton(I18N.t("button.clear"));
		clear.addActionListener(e -> {
			individuals.clear();
			reloadComparison();
		});

		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
		buttons.add(add);
		buttons.add(remove);
		buttons.add(clear);

		final JPanel chosenPanel = new JPanel(new BorderLayout(4, 4));
		chosenPanel.setBorder(BorderFactory.createTitledBorder(I18N.t("dialog.historical_timeline.individuals")));
		chosenPanel.add(listScroll, BorderLayout.CENTER);
		chosenPanel.add(buttons, BorderLayout.SOUTH);

		placeAwareBox.setToolTipText(I18N.t("dialog.historical_timeline.place_aware.tooltip"));
		for(final JCheckBox box : new JCheckBox[]{keyEventsBox, comparisonAttributesBox, contextBox, placeAwareBox})
			box.addActionListener(e -> reloadComparison());
		final JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
		options.add(keyEventsBox);
		options.add(comparisonAttributesBox);
		options.add(contextBox);
		options.add(placeAwareBox);

		final JPanel top = new JPanel(new BorderLayout());
		top.add(chosenPanel, BorderLayout.CENTER);
		top.add(options, BorderLayout.SOUTH);

		final JButton saveSvg = new JButton(I18N.t("button.save_svg"));
		saveSvg.addActionListener(e -> saveSvg(comparisonModel, "lifespan-comparison.svg"));

		final JPanel tab = new JPanel(new BorderLayout(6, 6));
		tab.add(top, BorderLayout.NORTH);
		tab.add(comparisonPanel, BorderLayout.CENTER);
		tab.add(GUIHelper.createStatusFooter(new JLabel(StringUtils.SPACE), saveSvg), BorderLayout.SOUTH);
		return tab;
	}

	private List<String> individualIds(){
		final List<String> ids = new ArrayList<>(individuals.size());
		for(int i = 0; i < individuals.size(); i ++)
			ids.add(individuals.get(i)
				.id());
		return ids;
	}

	private LifespanComparisonBuilder.Options comparisonOptions(){
		return new LifespanComparisonBuilder.Options(keyEventsBox.isSelected(), comparisonAttributesBox.isSelected(), contextBox.isSelected(),
			placeAwareBox.isSelected());
	}

	private void reloadComparison(){
		comparisonPanel.loadProjection();
	}


	/* ======================================================================
	 *                          Shared
	 * ====================================================================== */

	private FLEFRecord chooseIndividual(){
		final FLEFRecord[] chosen = new FLEFRecord[1];
		@SuppressWarnings("unchecked")
		final RecordSelectionDialog dialog = RecordSelectionDialog.create(this, model, (record, handler) -> chosen[0] = record,
			IndividualHandler.class);
		dialog.setVisible(true);
		return chosen[0];
	}

	private void saveSvg(final TemporalProjectionModel projection, final String defaultName){
		final JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(I18N.t("dialog.historical_timeline.svg.title"));
		chooser.setFileFilter(new FileNameExtensionFilter(I18N.t("file.filter.svg"), "svg"));
		chooser.setSelectedFile(new File(defaultName));
		if(chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return;

		File target = chooser.getSelectedFile();
		if(!target.getName().toLowerCase(Locale.ROOT).endsWith(".svg"))
			target = new File(target.getParentFile(), target.getName() + ".svg");
		if(target.exists() && JOptionPane.showConfirmDialog(this, I18N.t("confirmation.overwrite.export.message"),
				I18N.t("confirmation.overwrite.export.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
				!= JOptionPane.YES_OPTION)
			return;

		try{
			Files.writeString(target.toPath(), TemporalSvgWriter.write(projection), StandardCharsets.UTF_8);
		}
		catch(final IOException e){
			MessageDialogs.show(this, I18N.tf("error.csv.export", target.getAbsolutePath(), e.getMessage()),
				I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);
			return;
		}
		SavedFileNotice.show(this, I18N.t("dialog.historical_timeline.svg.title"),
			I18N.tf("text.historical.svg.saved", target.getAbsolutePath()), target);
	}


	/** An individual of the comparison. */
	private record IndividualItem(String id, String text){

		@Override
		public String toString(){
			return text;
		}

	}

	/** The date of a row: the text of the date, ordered by the first day the date can be. */
	private record DateCell(String text, long sortKey) implements Comparable<DateCell>{

		@Override
		public int compareTo(final DateCell other){
			return Long.compare(sortKey, other.sortKey);
		}

		@Override
		public String toString(){
			return text;
		}

	}

	private record TimelineRow(HistoricalTimeline.Item item, DateCell date){

		String kind(){
			return I18N.t("text.historical.kind." + item.kind().name().toLowerCase(Locale.ROOT));
		}

		String scope(){
			return (item.scope() != null? I18N.t(item.scope().key()): StringUtils.EMPTY);
		}

		/** @return	The impact of the context impact that links the item to the individual ("explains"), "✓" if it has no type. */
		String impact(){
			if(!item.isLinked())
				return StringUtils.EMPTY;

			return (item.impact().isEmpty()? "✓": I18N.t("enum.context_impact.type." + item.impact(), item.impact()));
		}

	}


	/** Shades the historic events and the cultural norms, more if a context impact links them to the individual. */
	private final class RowRenderer extends DefaultTableCellRenderer{

		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
				final boolean hasFocus, final int row, final int column){
			final Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			final TimelineRow timelineRow = tableModel.rows.get(table.convertRowIndexToModel(row));
			final boolean linked = timelineRow.item().isLinked();
			if(!isSelected)
				component.setBackground(timelineRow.item().isContext()? (linked? LINKED_ROW: CONTEXT_ROW): table.getBackground());
			component.setFont(component.getFont()
				.deriveFont(timelineRow.item().isContext()? (linked? Font.BOLD: Font.ITALIC): Font.PLAIN));
			return component;
		}

	}


	private static final class TimelineTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {I18N.t("column.date"), I18N.t("column.age"), I18N.t("column.kind"), I18N.t("column.title"),
			I18N.t("column.description"), I18N.t("column.place"), I18N.t("column.scope"), I18N.t("column.impact")};

		private final List<TimelineRow> rows = new ArrayList<>();

		void setRows(final List<TimelineRow> rows){
			this.rows.clear();
			this.rows.addAll(rows);
			fireTableDataChanged();
		}

		/** @return	The rows, as tab-separated lines (the titles first), for the clipboard. */
		String toText(){
			final StringBuilder sb = new StringBuilder(String.join("\t", COLUMNS))
				.append(StringUtils.LF);
			for(final TimelineRow row : rows){
				for(int column = 0; column < COLUMNS.length; column ++)
					sb.append(column > 0? "\t": StringUtils.EMPTY)
						.append(valueOf(row, column));
				sb.append(StringUtils.LF);
			}
			return sb.toString();
		}

		private static Object valueOf(final TimelineRow row, final int column){
			final HistoricalTimeline.Item item = row.item();
			return switch(column){
				case 0 -> row.date();
				case 1 -> (item.age() != null? item.age(): StringUtils.EMPTY);
				case 2 -> row.kind();
				case 3 -> item.label();
				case 4 -> item.detail();
				case 5 -> item.place();
				case 6 -> row.scope();
				case 7 -> row.impact();
				default -> StringUtils.EMPTY;
			};
		}

		@Override
		public int getRowCount(){
			return rows.size();
		}

		@Override
		public int getColumnCount(){
			return COLUMNS.length;
		}

		@Override
		public String getColumnName(final int column){
			return COLUMNS[column];
		}

		@Override
		public Class<?> getColumnClass(final int column){
			return (column == 0? DateCell.class: (column == 1? Integer.class: String.class));
		}

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex){
			final Object value = valueOf(rows.get(rowIndex), columnIndex);
			return (value instanceof String text && columnIndex == 1 && text.isEmpty()? null: value);
		}

	}

}
