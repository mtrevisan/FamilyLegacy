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

import io.github.mtrevisan.familylegacy.ui.components.projections.actions.provenance.EvidenceTraceDialog;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.reports.ConflictConfidence;
import io.github.mtrevisan.familylegacy.reports.ConflictDashboard;
import io.github.mtrevisan.familylegacy.reports.ConflictFilter;
import io.github.mtrevisan.familylegacy.reports.ConflictFinder;
import io.github.mtrevisan.familylegacy.reports.ConflictRecordType;
import io.github.mtrevisan.familylegacy.reports.ReportConflict;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.RowTableModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.ProgressDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.ConclusionRecordDialog;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;


/**
 * The conflict dashboard: everything the research has in conflict or still open, in one list to work on: dates and places that do not
 * agree, conflicting relationships, attributes and names, identity hypotheses nobody has judged and conclusions that decide nothing yet.
 * <p>
 * It can be filtered by the confidence of the assertions, by the kind of record and to what is still unresolved. Selecting an entry shows
 * the competing assertions with their confidence, the citations that support and the ones that contradict them, and the one that a
 * conclusion prefers. From there the resolution tools are one click away: the record, the evidence trace (<i>Explain</i>) and the
 * conclusion that settles the conflict (a new draft, or the one that exists). The dashboard never changes the file by itself; after
 * anything is edited it is read again.
 */
public final class ConflictDashboardDialog extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(ConflictDashboardDialog.class);

	private static String[] entryColumns(){
		return new String[]{I18N.t("column.conflict.status"), I18N.t("column.conflict.category"), I18N.t("column.conflict.subject"),
			I18N.t("column.conflict.problem"), I18N.t("column.conflict.assertions")};
	}

	private static String[] assertionColumns(){
		return new String[]{I18N.t("column.id"), I18N.t("column.conflict.assertion"), I18N.t("column.place"),
			I18N.t("column.conflict.confidence"), I18N.t("column.conflict.supporting"), I18N.t("column.conflict.contradicting"),
			I18N.t("column.conflict.preferred")};
	}

	private final FLEFModel model;

	private final JComboBox<ConflictFilter> statusFilter = enumCombo(ConflictFilter.values(), "enum.conflict_filter.");
	private final JComboBox<ConflictConfidence> confidenceFilter = enumCombo(ConflictConfidence.values(), "enum.conflict_confidence.");
	private final JComboBox<ConflictRecordType> typeFilter = enumCombo(ConflictRecordType.values(), "enum.conflict_record_type.");
	private final JLabel verdict = new JLabel(StringUtils.SPACE);
	private final JTable entryTable = new JTable();
	private final JTable assertionTable = new JTable();
	private final JButton openRecord = new JButton(I18N.t("button.open_record"));
	private final JButton explain = new JButton(I18N.t("button.explain"));
	private final JButton resolve = new JButton(I18N.t("button.conflict.create_conclusion"));

	private ConflictDashboard dashboard;
	/** The entries of the rows of the table (the sorting is off: a row of the table is a row of the list). */
	private List<ConflictDashboard.Entry> listed = List.of();


	/** Reads the model and shows the dashboard (see {@link #open} to do it with a progress bar). */
	public ConflictDashboardDialog(final Window owner, final FLEFModel model){
		this(owner, model, ConflictDashboard.analyze(model, ConflictFinder.provided()));
	}

	private ConflictDashboardDialog(final Window owner, final FLEFModel model, final ConflictDashboard dashboard){
		super(owner, I18N.t("dialog.conflict_dashboard.title"), ModalityType.APPLICATION_MODAL);

		ToolDialogs.installEscapeToClose(this);

		this.model = model;
		this.dashboard = dashboard;

		for(final JTable table : List.of(entryTable, assertionTable)){
			table.setRowHeight(22);
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		entryTable.getSelectionModel()
			.addListSelectionListener(e -> {
				if(!e.getValueIsAdjusting())
					showSelected();
			});
		entryTable.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && selectedEntry() != null)
					resolveSelected();
			}
		});
		assertionTable.getSelectionModel()
			.addListSelectionListener(e -> updateButtons());

		statusFilter.addActionListener(e -> showEntries());
		confidenceFilter.addActionListener(e -> showEntries());
		typeFilter.addActionListener(e -> showEntries());

		openRecord.addActionListener(e -> openSelectedRecord());
		explain.addActionListener(e -> explainSelected());
		resolve.addActionListener(e -> resolveSelected());
		final JButton close = new JButton(I18N.t("button.close"));
		close.addActionListener(e -> dispose());

		final JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
		filters.add(new JLabel(I18N.t("label.conflict_filter")));
		filters.add(statusFilter);
		filters.add(new JLabel(I18N.t("label.conflict_confidence")));
		filters.add(confidenceFilter);
		filters.add(new JLabel(I18N.t("label.conflict_record_type")));
		filters.add(typeFilter);

		verdict.setBorder(BorderFactory.createEmptyBorder(6, 8, 2, 8));
		final JPanel north = new JPanel(new BorderLayout());
		north.add(verdict, BorderLayout.NORTH);
		north.add(filters, BorderLayout.CENTER);

		final JScrollPane entryScroll = new JScrollPane(entryTable);
		entryScroll.setPreferredSize(new Dimension(860, 240));
		final JScrollPane assertionScroll = new JScrollPane(assertionTable);
		assertionScroll.setPreferredSize(new Dimension(860, 160));
		assertionScroll.setBorder(BorderFactory.createTitledBorder(I18N.t("border.conflict.assertions")));
		final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, entryScroll, assertionScroll);
		split.setResizeWeight(0.6);
		split.setBorder(null);

		setLayout(new BorderLayout(8, 8));
		add(north, BorderLayout.NORTH);
		add(split, BorderLayout.CENTER);
		add(ToolDialogs.createFooter(StringUtils.SPACE, openRecord, explain, resolve, close), BorderLayout.SOUTH);

		showEntries();

		pack();
		setLocationRelativeTo(owner);
	}


	/**
	 * Shows the dashboard. Reading the conflicts of a big file takes a while, so it is done in the background with a progress dialog,
	 * and the dashboard is shown when it is ready.
	 */
	public static void open(final Window owner, final FLEFModel model){
		analyze(owner, model, dashboard -> new ConflictDashboardDialog(owner, model, dashboard)
			.setVisible(true));
	}

	private static void analyze(final Window owner, final FLEFModel model, final Consumer<ConflictDashboard> whenReady){
		final String title = I18N.t("dialog.conflict_dashboard.title");
		final ProgressDialog progress = new ProgressDialog(owner, title, I18N.t("progress.conflict_dashboard.computing"));

		final SwingWorker<ConflictDashboard, Void> worker = new SwingWorker<>(){
			@Override
			protected ConflictDashboard doInBackground(){
				return ConflictDashboard.analyze(model, ConflictFinder.provided());
			}

			@Override
			protected void done(){
				progress.close();
				try{
					whenReady.accept(get());
				}
				catch(final InterruptedException ie){
					Thread.currentThread().interrupt();
				}
				catch(final ExecutionException ee){
					fail(owner, title, (ee.getCause() != null? ee.getCause(): ee));
				}
				catch(final RuntimeException | Error ex){
					fail(owner, title, ex);
				}
			}
		};
		worker.execute();
		//modal: it blocks the caller until done() disposes the dialog
		progress.setVisible(true);
	}

	private static void fail(final Window owner, final String title, final Throwable cause){
		LOGGER.error("Conflict dashboard failed", cause);
		MessageDialogs.show(owner, I18N.t("dialog.conflict_dashboard.error") + StringUtils.LF
			+ (cause.getMessage() != null? cause.getMessage(): cause.toString()), title, JOptionPane.ERROR_MESSAGE);
	}

	/** Reads the model again after something was edited, keeping the filters (and the entry in view, if it is still there). */
	private void reload(){
		final ConflictDashboard.Entry selected = selectedEntry();
		final String selectedConclusion = (selected != null? selected.conclusionId(): null);
		final String selectedFirst = (selected != null && !selected.assertions().isEmpty()? selected.assertions().getFirst().recordId(): null);

		analyze(this, model, refreshed -> {
			dashboard = refreshed;
			showEntries();
			for(int row = 0; row < listed.size(); row ++){
				final ConflictDashboard.Entry entry = listed.get(row);
				if(selected != null && (selectedConclusion != null && selectedConclusion.equals(entry.conclusionId())
						|| selectedFirst != null && !entry.assertions().isEmpty()
						&& selectedFirst.equals(entry.assertions().getFirst().recordId()))){
					entryTable.setRowSelectionInterval(row, row);
					break;
				}
			}
		});
	}


	/* ======================================================================
	 *                          The list
	 * ====================================================================== */

	private void showEntries(){
		listed = dashboard.filter((ConflictFilter)statusFilter.getSelectedItem(), (ConflictConfidence)confidenceFilter.getSelectedItem(),
			(ConflictRecordType)typeFilter.getSelectedItem());

		final List<Object[]> rows = new ArrayList<>(listed.size());
		for(final ConflictDashboard.Entry entry : listed)
			rows.add(new Object[]{I18N.t(entry.resolved()? "text.conflict_dashboard.status.settled": "text.conflict_dashboard.status.unresolved"),
				I18N.t("text.conflict_dashboard.category." + entry.category().key()), subject(entry), problem(entry), entry.assertions().size()});
		entryTable.setModel(new RowTableModel(entryColumns(), rows));
		//the sorter of a table whose model is replaced would stay bound to the first one and show no row
		entryTable.setAutoCreateRowSorter(false);
		entryTable.setRowSorter(null);
		entryTable.getColumnModel().getColumn(0).setPreferredWidth(90);
		entryTable.getColumnModel().getColumn(1).setPreferredWidth(120);
		entryTable.getColumnModel().getColumn(2).setPreferredWidth(260);
		entryTable.getColumnModel().getColumn(3).setPreferredWidth(300);
		entryTable.getColumnModel().getColumn(4).setPreferredWidth(80);

		final int total = dashboard.entries().size();
		final int unresolved = dashboard.unresolved();
		if(total == 0){
			verdict.setText(I18N.t("text.conflict_dashboard.none"));
			verdict.setForeground(ToolDialogs.OK_COLOR);
		}
		else{
			verdict.setText(I18N.tf("text.conflict_dashboard.summary", total, unresolved, total - unresolved, categoryCounts()));
			verdict.setForeground(unresolved == 0? ToolDialogs.OK_COLOR: ToolDialogs.WARN_COLOR);
		}
		showSelected();
	}

	/** How many entries each category has, as one line (the empty categories are left out). */
	private String categoryCounts(){
		final List<String> parts = new ArrayList<>();
		for(final ConflictDashboard.Category category : ConflictDashboard.Category.values()){
			final int count = dashboard.count(category);
			if(count > 0)
				parts.add(I18N.t("text.conflict_dashboard.category." + category.key()) + " " + count);
		}
		return String.join(", ", parts);
	}

	private String subject(final ConflictDashboard.Entry entry){
		if(entry.conclusion() != null)
			return RecordDisplay.describe(entry.conclusion(), model);

		final List<String> names = new ArrayList<>(entry.personIds().size());
		for(final String personId : entry.personIds())
			names.add(display(personId));
		return String.join("; ", names);
	}

	private static String problem(final ConflictDashboard.Entry entry){
		if(entry.conflict() == null)
			return I18N.t("text.conflict_dashboard.problem.open_conclusion");

		final String kind = I18N.t("text.conflict_dashboard.kind." + entry.conflict().kind().key());
		return kind + ": " + I18N.t("text.conflict_dashboard.problem." + entry.conflict().problem().key());
	}

	private String display(final String recordId){
		if(recordId == null)
			return StringUtils.EMPTY;

		final FLEFRecord record = model.getRecordById(recordId);
		return (record != null? RecordDisplay.describe(record, model): recordId);
	}

	private ConflictDashboard.Entry selectedEntry(){
		final int row = entryTable.getSelectedRow();
		return (row >= 0 && row < listed.size()? listed.get(row): null);
	}

	/** @return	The assertion selected in the lower table, {@code null} if none. */
	private ReportConflict.Assertion selectedAssertion(){
		final ConflictDashboard.Entry entry = selectedEntry();
		final int row = assertionTable.getSelectedRow();
		return (entry != null && row >= 0 && row < entry.assertions().size()? entry.assertions().get(row): null);
	}


	/* ======================================================================
	 *                          The detail
	 * ====================================================================== */

	private void showSelected(){
		final ConflictDashboard.Entry entry = selectedEntry();
		final List<Object[]> rows = new ArrayList<>();
		if(entry != null){
			final String preferredId = (entry.conflict() != null && entry.conflict().settlement() != null
				? entry.conflict().settlement().preferredId(): null);
			for(final ReportConflict.Assertion assertion : entry.assertions()){
				final int[] citations = ConflictDashboard.citations(model, assertion);
				rows.add(new Object[]{assertion.recordId(),
					(StringUtils.isNotBlank(assertion.value())? assertion.value(): display(assertion.recordId())),
					display(assertion.placeId()),
					(assertion.confidence() != null? I18N.t("enum.conflict_confidence." + assertion.confidence().toLowerCase(Locale.ROOT),
						assertion.confidence()): "-"),
					citations[0], citations[1],
					(assertion.recordId().equals(preferredId)? I18N.t("text.conflict_dashboard.preferred"): StringUtils.EMPTY)});
			}
		}
		assertionTable.setModel(new RowTableModel(assertionColumns(), rows));
		assertionTable.setAutoCreateRowSorter(false);
		assertionTable.setRowSorter(null);
		assertionTable.getColumnModel().getColumn(1).setPreferredWidth(260);
		updateButtons();
	}

	private void updateButtons(){
		final ConflictDashboard.Entry entry = selectedEntry();
		openRecord.setEnabled(entry != null);
		explain.setEnabled(entry != null && entry.conflict() != null);
		final boolean existing = (entry != null && entry.conclusionId() != null);
		resolve.setText(I18N.t(existing? "button.conflict.open_conclusion": "button.conflict.create_conclusion"));
		resolve.setEnabled(entry != null && (existing || entry.canBeConcluded()));
	}


	/* ======================================================================
	 *                          The resolution tools
	 * ====================================================================== */

	/** Opens the selected assertion, or, if none is, the individual (or the conclusion) of the entry. */
	private void openSelectedRecord(){
		final ConflictDashboard.Entry entry = selectedEntry();
		if(entry == null)
			return;

		final ReportConflict.Assertion assertion = selectedAssertion();
		final String id = (assertion != null? assertion.recordId(): (entry.conclusion() != null? entry.conclusion().getId()
			: (entry.personIds().isEmpty()? null: entry.personIds().getFirst())));
		final FLEFRecord record = (id != null? model.getRecordById(id): null);
		if(record == null)
			return;

		ToolDialogs.openRecordEditor(this, model, record);
		reload();
	}

	/** "Explain This" on the selected assertion (the first one if none is selected): the evidence for and against it. */
	private void explainSelected(){
		final ConflictDashboard.Entry entry = selectedEntry();
		if(entry == null || entry.assertions().isEmpty())
			return;

		final ReportConflict.Assertion assertion = selectedAssertion();
		EvidenceTraceDialog.open(this, model, (assertion != null? assertion: entry.assertions().getFirst()).recordId());
		reload();
	}

	/** Opens the conclusion that deals with the entry, or starts the one that would settle the conflict. */
	private void resolveSelected(){
		final ConflictDashboard.Entry entry = selectedEntry();
		if(entry == null)
			return;

		final String conclusionId = entry.conclusionId();
		final FLEFRecord conclusion = (conclusionId != null? model.getRecordById(conclusionId): null);
		if(conclusion != null)
			ToolDialogs.openRecordEditor(this, model, conclusion);
		else if(entry.canBeConcluded()){
			final ConclusionRecordDialog dialog = ConclusionRecordDialog.createNew(this, model)
				.withDraft(null, draftNarrative(entry));
			for(final ReportConflict.Assertion assertion : entry.assertions()){
				final FLEFRecord record = model.getRecordById(assertion.recordId());
				if(record != null)
					dialog.withResolvedTarget(record, entry.issue());
			}
			dialog.setVisible(true);
		}
		else
			return;

		reload();
	}

	/** The description of the conflict, as the first text of the reasoning of the conclusion. */
	private String draftNarrative(final ConflictDashboard.Entry entry){
		final List<String> assertions = new ArrayList<>(entry.assertions().size());
		for(final ReportConflict.Assertion assertion : entry.assertions())
			assertions.add(assertion.recordId() + (StringUtils.isNotBlank(assertion.value())? " (" + assertion.value() + ")": StringUtils.EMPTY));
		return I18N.tf("text.conflict_dashboard.draft", subject(entry), problem(entry), String.join("; ", assertions));
	}


	private static <E extends Enum<E>> JComboBox<E> enumCombo(final E[] values, final String prefix){
		final JComboBox<E> combo = new JComboBox<>(values);
		combo.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean selected,
					final boolean focus){
				return super.getListCellRendererComponent(list, (value != null
					? I18N.t(prefix + ((Enum<?>)value).name().toLowerCase(Locale.ROOT)): StringUtils.EMPTY), index, selected, focus);
			}
		});
		return combo;
	}

}
