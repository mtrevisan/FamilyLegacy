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
package io.github.mtrevisan.familylegacy.ui.tools.files.importation;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.importer.ImportIssue;
import io.github.mtrevisan.familylegacy.importer.ImportReport;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;


/**
 * Shows, after an import, the important information that could not be kept (see {@link ImportReport}): a summary by reason and the list
 * of the items. The whole list can be copied or saved as plain text.
 */
public final class ImportReportDialog extends JDialog{

	private final ImportReport report;


	private ImportReportDialog(final Window owner, final ImportReport report){
		super(owner, I18N.t("dialog.import.report.title"), ModalityType.APPLICATION_MODAL);

		GUIHelper.installEscapeToClose(this);

		this.report = report;

		final JLabel intro = new JLabel("<html><body style='width: 560px'>" + escapeHtml(I18N.t("dialog.import.report.intro"))
			+ "</body></html>");
		intro.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));

		final DefaultTableModel summaryModel = readOnlyModel(I18N.t("dialog.import.report.column.reason"),
			I18N.t("dialog.import.report.column.count"));
		for(final Map.Entry<String, Integer> entry : report.countsByReason().entrySet())
			summaryModel.addRow(new Object[]{I18N.t(entry.getKey()), entry.getValue()});
		final JTable summary = new JTable(summaryModel);
		summary.getColumnModel().getColumn(1).setMaxWidth(80);
		final JScrollPane summaryScroll = new JScrollPane(summary);
		summaryScroll.setBorder(BorderFactory.createTitledBorder(I18N.t("dialog.import.report.summary")));
		summaryScroll.setPreferredSize(new Dimension(640, 150));

		final DefaultTableModel detailModel = readOnlyModel(I18N.t("dialog.import.report.column.reason"),
			I18N.t("dialog.import.report.column.item"), I18N.t("dialog.import.report.column.where"));
		for(final ImportIssue issue : report.issues())
			detailModel.addRow(new Object[]{I18N.t(issue.reasonKey()), issue.item(), issue.where()});
		final JTable details = new JTable(detailModel);
		details.setAutoCreateRowSorter(true);
		final JScrollPane detailScroll = new JScrollPane(details);
		detailScroll.setBorder(BorderFactory.createTitledBorder(I18N.t("dialog.import.report.details")));
		detailScroll.setPreferredSize(new Dimension(640, 260));

		final JPanel center = new JPanel(new BorderLayout(4, 4));
		center.add(summaryScroll, BorderLayout.NORTH);
		center.add(detailScroll, BorderLayout.CENTER);
		if(report.overflow() > 0)
			center.add(new JLabel(I18N.tf("dialog.import.report.overflow", report.overflow())), BorderLayout.SOUTH);
		center.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

		final JButton copy = new JButton(I18N.t("dialog.import.report.copy"));
		copy.addActionListener(e -> {
			final StringSelection selection = new StringSelection(toText(report));
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
		});
		final JButton save = new JButton(I18N.t("dialog.import.report.save"));
		save.addActionListener(e -> saveAsText());
		final JButton close = new JButton(I18N.t("button.close"));
		close.addActionListener(e -> dispose());

		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(copy);
		buttons.add(save);
		buttons.add(close);

		setLayout(new BorderLayout(4, 4));
		add(intro, BorderLayout.NORTH);
		add(center, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(owner);
	}


	/** Shows the (modal) dialog; it does nothing if the report is empty. */
	public static void show(final Window owner, final ImportReport report){
		if(report.isEmpty())
			return;

		new ImportReportDialog(owner, report).setVisible(true);
	}

	/** Builds the dialog without showing it (for tests). */
	static ImportReportDialog create(final Window owner, final ImportReport report){
		return new ImportReportDialog(owner, report);
	}


	/** @return	The whole report as plain text: a header, then one line per issue ({@code reason<TAB>item<TAB>where}). */
	static String toText(final ImportReport report){
		final StringBuilder sb = new StringBuilder(I18N.t("dialog.import.report.text.header")).append(StringUtils.LF);
		for(final ImportIssue issue : report.issues())
			sb.append(I18N.t(issue.reasonKey())).append('\t').append(issue.item()).append('\t').append(issue.where())
				.append(StringUtils.LF);
		if(report.overflow() > 0)
			sb.append(I18N.tf("dialog.import.report.text.overflow", report.overflow())).append(StringUtils.LF);
		return sb.toString();
	}

	private void saveAsText(){
		final JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(I18N.t("dialog.import.report.save"));
		chooser.setSelectedFile(new File("import-report.txt"));
		if(chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return;

		try{
			Files.writeString(chooser.getSelectedFile().toPath(), toText(report), StandardCharsets.UTF_8);
		}
		catch(final IOException ioe){
			JOptionPane.showMessageDialog(this, I18N.t("dialog.import.report.save.error") + StringUtils.LF + ioe.getMessage(),
				I18N.t("dialog.import.report.title"), JOptionPane.ERROR_MESSAGE);
		}
	}

	private static DefaultTableModel readOnlyModel(final String... columns){
		return new DefaultTableModel(columns, 0){
			@Override
			public boolean isCellEditable(final int row, final int column){
				return false;
			}
		};
	}

	private static String escapeHtml(final String text){
		return StringUtils.replaceEach(text, new String[]{"&", "<", ">"}, new String[]{"&amp;", "&lt;", "&gt;"});
	}

}
