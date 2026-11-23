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
package io.github.mtrevisan.familylegacy.ui.tools.sources;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.reports.DocumentationCoverage;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.RowTableModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;


/**
 * The documentation coverage dashboard, one tab for each question:
 * <ul>
 *   <li><b>By record type</b>: how much of the individuals, families, events, places and media is documented; selecting a type lists
 *   the records without a citation;</li>
 *   <li><b>Hotspots</b>: the places and the kinds of event where most events lack a citation;</li>
 *   <li><b>Branches</b>: the lines of the family from the least documented.</li>
 * </ul>
 * A double-click opens the record to fix the problem on the spot; the analysis is made again afterwards.
 */
final class DocumentationCoverageDialog extends JDialog{

	/** A list of tens of thousands of records is of no use on the screen: the first ones are shown, the rest is a matter of fixing and reloading. */
	static final int MAX_LISTED = 2000;

	private final FLEFModel model;

	private final JTabbedPane tabs = new JTabbedPane();

	/** The records of the rows of the tables that open one, by table (the sorting is off: a row of the table is a row of the model). */
	private final List<FLEFRecord> placeRecords = new ArrayList<>();
	private final List<FLEFRecord> branchRecords = new ArrayList<>();
	private final List<FLEFRecord> listedRecords = new ArrayList<>();

	private DocumentationCoverage coverage;
	private DocumentationCoverage.Kind selectedKind;
	private JTable kindTable;
	private JTable listTable;
	private JLabel listTitle;


	DocumentationCoverageDialog(final Window owner, final FLEFModel model){
		super(owner, I18N.t("dialog.documentation_coverage.title"), ModalityType.APPLICATION_MODAL);

		ToolDialogs.installEscapeToClose(this);

		this.model = model;

		tabs.setPreferredSize(new Dimension(820, 520));

		setLayout(new BorderLayout(8, 8));
		add(tabs, BorderLayout.CENTER);

		reload();

		pack();
		setLocationRelativeTo(owner);
	}


	/** Reads the model again and builds the tabs, keeping the one in view (and the type whose records are listed). */
	private void reload(){
		coverage = DocumentationCoverage.analyze(model);

		final int selectedTab = Math.max(tabs.getSelectedIndex(), 0);
		tabs.removeAll();
		placeRecords.clear();
		branchRecords.clear();

		tabs.addTab(I18N.t("tab.title.coverage_kinds"), createKindsTab());
		tabs.addTab(I18N.t("tab.title.coverage_hotspots"), createHotspotsTab());
		tabs.addTab(I18N.t("tab.title.coverage_branches"), createBranchesTab());
		tabs.setSelectedIndex(Math.min(selectedTab, tabs.getTabCount() - 1));
	}

	private String overallText(){
		final int total = coverage.byKind().stream()
			.mapToInt(DocumentationCoverage.KindCoverage::total)
			.sum();
		if(total == 0)
			return I18N.t("text.documentation_coverage.overall.empty");

		return I18N.tf("text.documentation_coverage.overall", percent(coverage.overallPercentage()), total - coverage.totalUndocumented(),
			total);
	}

	static String percent(final double value){
		return String.format(Locale.ROOT, "%.1f %%", value);
	}

	static String kindLabel(final DocumentationCoverage.Kind kind){
		return I18N.t("text.documentation_coverage.kind." + kind.name().toLowerCase(Locale.ROOT));
	}

	/** The type of an event as the user reads it ("birth"); the types that are not known are shown as written. */
	static String eventTypeLabel(final String type){
		return (type.isEmpty()? "-": I18N.t("enum.event.type." + type, type));
	}


	/* ======================================================================
	 *                          By record type
	 * ====================================================================== */

	private JPanel createKindsTab(){
		final List<Object[]> rows = new ArrayList<>();
		for(final DocumentationCoverage.KindCoverage entry : coverage.byKind()){
			rows.add(new Object[]{kindLabel(entry.kind()), entry.total(), entry.undocumented().size(), percent(entry.percentage())});
		}
		kindTable = createTable(new String[]{I18N.t("column.record_type"), I18N.t("column.total"), I18N.t("column.uncited"),
			I18N.t("column.coverage")}, rows, null);
		rightAlign(kindTable, 1, 2, 3);
		kindTable.getSelectionModel()
			.addListSelectionListener(e -> {
				if(!e.getValueIsAdjusting())
					showListOfSelectedKind();
			});

		listTitle = new JLabel(I18N.t("text.documentation_coverage.list.hint"));
		listTitle.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
		listTable = createTable(new String[]{I18N.t("column.id"), I18N.t("column.record")}, List.of(), this::openListed);
		final JPanel list = new JPanel(new BorderLayout());
		list.add(listTitle, BorderLayout.NORTH);
		list.add(new JScrollPane(listTable), BorderLayout.CENTER);

		final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(kindTable), list);
		split.setResizeWeight(0.3);
		split.setBorder(null);

		final boolean complete = (coverage.totalUndocumented() == 0);
		final JPanel tab = ToolDialogs.createVerdictPanel(complete, overallText());
		tab.add(split, BorderLayout.CENTER);

		//what was listed stays listed after an edit
		final int index = (selectedKind != null? selectedKind.ordinal(): -1);
		if(index >= 0)
			kindTable.setRowSelectionInterval(index, index);
		return tab;
	}

	private void showListOfSelectedKind(){
		final int row = kindTable.getSelectedRow();
		listedRecords.clear();
		if(row < 0){
			selectedKind = null;
			listTitle.setText(I18N.t("text.documentation_coverage.list.hint"));
			listTable.setModel(new RowTableModel(new String[]{I18N.t("column.id"), I18N.t("column.record")}, List.of()));
			return;
		}

		final DocumentationCoverage.KindCoverage entry = coverage.byKind().get(row);
		selectedKind = entry.kind();

		final List<FLEFRecord> undocumented = entry.undocumented();
		final int shown = Math.min(undocumented.size(), MAX_LISTED);
		final List<Object[]> rows = new ArrayList<>(shown);
		for(int i = 0; i < shown; i ++){
			final FLEFRecord record = undocumented.get(i);
			listedRecords.add(record);
			rows.add(new Object[]{record.getId(), RecordDisplay.describe(record, model)});
		}
		listTable.setModel(new RowTableModel(new String[]{I18N.t("column.id"), I18N.t("column.record")}, rows));
		listTable.getColumnModel().getColumn(0).setPreferredWidth(110);
		listTable.getColumnModel().getColumn(1).setPreferredWidth(600);

		String title = I18N.tf("text.documentation_coverage.list.title", kindLabel(entry.kind()), undocumented.size());
		if(undocumented.size() > shown)
			title += StringUtils.SPACE + I18N.tf("text.documentation_coverage.list.capped", shown, undocumented.size());
		if(entry.kind() == DocumentationCoverage.Kind.FAMILY)
			title += StringUtils.SPACE + I18N.t("text.documentation_coverage.family.note");
		listTitle.setText(title);
	}

	private void openListed(final int row){
		open(listedRecords, row);
	}


	/* ======================================================================
	 *                          Hotspots
	 * ====================================================================== */

	private JPanel createHotspotsTab(){
		final boolean none = coverage.placeHotspots().isEmpty() && coverage.eventTypeHotspots().isEmpty();
		final JPanel tab = ToolDialogs.createVerdictPanel(none, I18N.t(none
			? "text.documentation_coverage.hotspots.none"
			: "text.documentation_coverage.hotspots.note"));
		if(none)
			return tab;

		final List<Object[]> placeRows = new ArrayList<>();
		for(final DocumentationCoverage.PlaceHotspot hotspot : coverage.placeHotspots()){
			placeRows.add(new Object[]{RecordDisplay.describe(hotspot.place(), model), hotspot.undocumented(), hotspot.total(),
				percent(100. * (hotspot.total() - hotspot.undocumented()) / hotspot.total())});
			placeRecords.add(hotspot.place());
		}
		final JTable places = createTable(new String[]{I18N.t("column.place"), I18N.t("column.uncited"), I18N.t("column.events"),
			I18N.t("column.coverage")}, placeRows, row -> open(placeRecords, row));
		rightAlign(places, 1, 2, 3);
		places.getColumnModel().getColumn(0).setPreferredWidth(420);

		final List<Object[]> typeRows = new ArrayList<>();
		for(final DocumentationCoverage.EventTypeHotspot hotspot : coverage.eventTypeHotspots())
			typeRows.add(new Object[]{eventTypeLabel(hotspot.type()), hotspot.undocumented(), hotspot.total(),
				percent(100. * (hotspot.total() - hotspot.undocumented()) / hotspot.total())});
		final JTable types = createTable(new String[]{I18N.t("column.event"), I18N.t("column.uncited"), I18N.t("column.events"),
			I18N.t("column.coverage")}, typeRows, null);
		rightAlign(types, 1, 2, 3);
		types.getColumnModel().getColumn(0).setPreferredWidth(420);

		final JScrollPane typeScroll = new JScrollPane(types);
		typeScroll.setBorder(BorderFactory.createTitledBorder(I18N.t("border.event_types")));
		final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(places), typeScroll);
		split.setResizeWeight(0.6);
		split.setBorder(null);
		tab.add(split, BorderLayout.CENTER);
		return tab;
	}


	/* ======================================================================
	 *                          Branches
	 * ====================================================================== */

	private JPanel createBranchesTab(){
		final boolean none = coverage.branches().isEmpty();
		final JPanel tab = ToolDialogs.createHeaderPanel(I18N.t(none
			? "text.documentation_coverage.branches.none"
			: "text.documentation_coverage.branches.note"), null);
		if(none)
			return tab;

		final List<Object[]> rows = new ArrayList<>();
		for(final DocumentationCoverage.Branch branch : coverage.branches()){
			rows.add(new Object[]{RecordDisplay.describe(branch.founder(), model), branch.size(), branch.documented(),
				percent(branch.percentage())});
			branchRecords.add(branch.founder());
		}
		final JTable table = createTable(new String[]{I18N.t("column.founder"), I18N.t("column.individuals"), I18N.t("column.documented"),
			I18N.t("column.coverage")}, rows, row -> open(branchRecords, row));
		rightAlign(table, 1, 2, 3);
		table.getColumnModel().getColumn(0).setPreferredWidth(420);
		tab.add(new JScrollPane(table), BorderLayout.CENTER);
		return tab;
	}


	/* ======================================================================
	 *                          Tables
	 * ====================================================================== */

	/** The rows are already in the order that matters (the worst first), so the table is not sortable and a row is a row of the model. */
	private static JTable createTable(final String[] columns, final List<Object[]> rows, final IntConsumer onOpen){
		final JTable table = ToolDialogs.createTable(new RowTableModel(columns, rows), onOpen);
		table.setAutoCreateRowSorter(false);
		//the sorter that the factory installed would stay bound to the first model, and a table whose model is replaced would show no row
		table.setRowSorter(null);
		return table;
	}

	private static void rightAlign(final JTable table, final int... columns){
		final DefaultTableCellRenderer right = new DefaultTableCellRenderer();
		right.setHorizontalAlignment(SwingConstants.RIGHT);
		for(final int column : columns)
			table.getColumnModel().getColumn(column).setCellRenderer(right);
	}

	private void open(final List<FLEFRecord> records, final int row){
		if(row < 0 || row >= records.size())
			return;

		ToolDialogs.openRecordEditor(this, model, records.get(row));

		reload();
	}

}
