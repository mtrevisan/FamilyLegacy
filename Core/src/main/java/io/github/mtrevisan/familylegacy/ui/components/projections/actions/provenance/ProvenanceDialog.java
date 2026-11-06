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
package io.github.mtrevisan.familylegacy.ui.components.projections.actions.provenance;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.sources.ProvenanceService;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.RowTableModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Shows where an assertion comes from: one row for each citation, with the source, the locator, the stance (a contradicting citation is
 * in red), the extract, the evidence qualifiers (as text) and the repository. It has two forms:
 * <ul>
 *   <li>{@link #forRecord(Window, FLEFModel, String)}: the citations of a record; for an individual, a check box adds the ones of its
 *       events, attributes and relationships;</li>
 *   <li>{@link #forSource(Window, FLEFModel, String)}: everything that cites a source.</li>
 * </ul>
 * A row can be opened (double-click or "Open record") to fix the citation on the spot, or its source can ("Open source"); the list is
 * read again afterwards.
 */
public final class ProvenanceDialog extends JDialog{

	private static final int COLUMN_STANCE = 3;

	private final FLEFModel model;
	private final String targetId;
	private final boolean forSource;

	private final JCheckBox related = new JCheckBox(I18N.t("checkbox.provenance.related"));
	private final JLabel summary = new JLabel(StringUtils.SPACE);
	private final JTable table = new JTable();
	private final JButton openRecord = new JButton(I18N.t("button.open_record"));
	private final JButton openSource = new JButton(I18N.t("button.open_source"));
	private List<ProvenanceService.Row> rows = List.of();


	/** The citations of a record. */
	static ProvenanceDialog forRecord(final Window owner, final FLEFModel model, final String recordId){
		return new ProvenanceDialog(owner, model, recordId, false);
	}

	/** Everything that cites a source. */
	static ProvenanceDialog forSource(final Window owner, final FLEFModel model, final String sourceId){
		return new ProvenanceDialog(owner, model, sourceId, true);
	}


	private ProvenanceDialog(final Window owner, final FLEFModel model, final String targetId, final boolean forSource){
		super(owner, titleOf(model, targetId, forSource), ModalityType.APPLICATION_MODAL);
		ToolDialogs.installEscapeToClose(this);
		this.model = model;
		this.targetId = targetId;
		this.forSource = forSource;

		table.setRowHeight(22);
		table.setAutoCreateRowSorter(true);
		table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel()
			.addListSelectionListener(e -> updateButtons());
		table.addMouseListener(new java.awt.event.MouseAdapter(){
			@Override
			public void mouseClicked(final java.awt.event.MouseEvent e){
				if(e.getClickCount() == 2 && javax.swing.SwingUtilities.isLeftMouseButton(e))
					openSelectedRecord();
			}
		});

		final FLEFRecord target = model.getRecordById(targetId);
		related.setVisible(!forSource && IndividualHandler.isOfType(target));
		related.setToolTipText(I18N.t("checkbox.provenance.related.tooltip"));
		related.addActionListener(e -> reload());

		openRecord.addActionListener(e -> openSelectedRecord());
		openSource.addActionListener(e -> openSelectedSource());

		final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		top.add(summary);
		top.add(related);

		final JScrollPane scroll = new JScrollPane(table);
		scroll.setPreferredSize(new Dimension(980, 420));

		setLayout(new BorderLayout(8, 8));
		add(top, BorderLayout.NORTH);
		add(scroll, BorderLayout.CENTER);
		add(ToolDialogs.createFooter(StringUtils.EMPTY, openRecord, openSource,
			ToolDialogs.createCopyButton(() -> ((RowTableModel)table.getModel()).toText()), ToolDialogs.createCloseButton(this)),
			BorderLayout.SOUTH);

		reload();

		pack();
		setLocationRelativeTo(owner);
	}


	private static String titleOf(final FLEFModel model, final String targetId, final boolean forSource){
		final FLEFRecord target = model.getRecordById(targetId);
		final String name = (target != null? RecordDisplay.describe(target, model): targetId);
		return I18N.tf((forSource? "dialog.provenance.title.source": "dialog.provenance.title.record"), name);
	}

	/** Reads the model again: it is called after a record has been edited, so that the list is what the file is now. */
	private void reload(){
		final FLEFRecord target = model.getRecordById(targetId);
		if(forSource)
			rows = ProvenanceService.ofSource(model, targetId);
		else if(related.isVisible() && related.isSelected())
			rows = ProvenanceService.ofIndividual(model, targetId);
		else
			rows = ProvenanceService.ofRecord(model, target);

		final List<Object[]> tableRows = new ArrayList<>(rows.size());
		for(final ProvenanceService.Row row : rows)
			tableRows.add(new Object[]{typeLabel(row.ownerType()) + ": " + row.ownerText(), sourceText(row), row.locator(),
				row.stance(), row.extractText(), row.evidence(), row.repositories()});
		table.setModel(new RowTableModel(new String[]{I18N.t("column.provenance.owner"), I18N.t("column.source"),
			I18N.t("column.provenance.locator"), I18N.t("column.provenance.stance"), I18N.t("column.provenance.extract"),
			I18N.t("column.provenance.evidence"), I18N.t("column.provenance.repository")}, tableRows));
		final int[] widths = {260, 220, 120, 80, 220, 140, 150};
		for(int column = 0; column < widths.length; column ++)
			table.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
		table.getColumnModel().getColumn(COLUMN_STANCE).setCellRenderer(new StanceRenderer());

		summary.setText(summaryOf(rows, forSource));
		updateButtons();
	}

	/** @return	The line above the table: how many citations, how they stand, how many are assessed; or why there are none. */
	public static String summaryOf(final List<ProvenanceService.Row> rows, final boolean forSource){
		if(rows.isEmpty())
			return I18N.t(forSource? "text.provenance.empty.source": "text.provenance.empty.record");

		int contradicting = 0;
		int assessed = 0;
		for(final ProvenanceService.Row row : rows){
			if(row.contradicts())
				contradicting ++;
			if(row.isAssessed())
				assessed ++;
		}
		return I18N.tf("text.provenance.summary", rows.size(), rows.size() - contradicting, contradicting, assessed);
	}

	private static String sourceText(final ProvenanceService.Row row){
		return (row.sourceTitle().isEmpty()? StringUtils.defaultString(row.sourceId()): row.sourceTitle());
	}

	/** The type of a record as the user reads it ("individual attribute"), not as it is written in the file. */
	private static String typeLabel(final String tag){
		return I18N.t("confirmation.exist_record." + tag.toLowerCase(Locale.ROOT), tag);
	}


	private ProvenanceService.Row selectedRow(){
		final int viewRow = table.getSelectedRow();
		return (viewRow >= 0? rows.get(table.convertRowIndexToModel(viewRow)): null);
	}

	private void updateButtons(){
		final ProvenanceService.Row row = selectedRow();
		openRecord.setEnabled(row != null);
		openSource.setEnabled(row != null && row.sourceId() != null && model.hasRecord(row.sourceId()));
	}

	private void openSelectedRecord(){
		final ProvenanceService.Row row = selectedRow();
		if(row == null)
			return;

		ToolDialogs.openRecordEditor(this, model, model.getRecordById(row.ownerId()));
		reload();
	}

	private void openSelectedSource(){
		final ProvenanceService.Row row = selectedRow();
		if(row == null || row.sourceId() == null)
			return;

		ToolDialogs.openRecordEditor(this, model, model.getRecordById(row.sourceId()));
		reload();
	}


	/** Shows a contradicting citation in red. */
	private static final class StanceRenderer extends DefaultTableCellRenderer{

		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
				final boolean hasFocus, final int row, final int column){
			final Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			final boolean contradicting = SourceCitationReader.STANCE_CONTRADICTS.equals(value);
			component.setForeground(contradicting && !isSelected? ToolDialogs.ERROR_COLOR: (isSelected? table.getSelectionForeground(): table.getForeground()));
			return component;
		}

	}

}
