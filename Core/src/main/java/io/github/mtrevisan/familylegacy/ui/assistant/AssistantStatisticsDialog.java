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

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantStatistics.Event;

import javax.swing.JButton;
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
import java.util.List;


/**
 * The page of the statistics of the assistant: for each kind of finding, how many were listed, how many were acted on (and how high in the
 * list they were), opened, ignored, put aside or shown again. It is what the numbers of the assistants are calibrated on: the page can be
 * copied as text (to give it to somebody) and the counts can be started again. They hold nothing about the research.
 */
public final class AssistantStatisticsDialog{

	private AssistantStatisticsDialog(){}


	/** Shows the page. */
	public static void show(final Window owner, final ResearchAssistantService service){
		JOptionPane.showMessageDialog(owner, panel(service.statistics()), I18N.t("dialog.assistant_statistics.title"),
			JOptionPane.PLAIN_MESSAGE);
	}

	/** @return the page, that reads the counts of the statistics when it is built and again when they are started again */
	static JPanel panel(final AssistantStatistics statistics){
		final DefaultTableModel model = new DefaultTableModel(columns(), 0){
			@Override
			public boolean isCellEditable(final int row, final int column){
				return false;
			}
		};
		final JTable table = new JTable(model);
		table.setAutoCreateRowSorter(true);
		table.getTableHeader().setReorderingAllowed(false);
		final JLabel summary = new JLabel();
		final Runnable fill = () -> {
			model.setRowCount(0);
			for(final AssistantStatistics.Row row : statistics.rows())
				model.addRow(rowOf(row));
			summary.setText(summary(statistics, model.getRowCount()));
		};
		fill.run();

		final JButton copy = new JButton(I18N.t("dialog.assistant_statistics.copy"));
		copy.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
			.setContents(new StringSelection(statistics.export()), null));
		final JButton reset = new JButton(I18N.t("dialog.assistant_statistics.reset"));
		reset.addActionListener(e -> {
			if(JOptionPane.showConfirmDialog(reset, I18N.t("dialog.assistant_statistics.reset.confirm"),
					I18N.t("dialog.assistant_statistics.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION){
				statistics.reset();
				fill.run();
			}
		});

		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttons.add(copy);
		buttons.add(reset);
		final JPanel panel = new JPanel(new BorderLayout(0, 6));
		final JScrollPane scroll = new JScrollPane(table);
		scroll.setPreferredSize(new Dimension(820, 320));
		panel.add(summary, BorderLayout.NORTH);
		panel.add(scroll, BorderLayout.CENTER);
		panel.add(buttons, BorderLayout.SOUTH);
		return panel;
	}

	private static String[] columns(){
		return new String[]{I18N.t("dialog.assistant_statistics.kind"), I18N.t("dialog.assistant_statistics.shown"),
			I18N.t("dialog.assistant_statistics.acted"), I18N.t("dialog.assistant_statistics.acted_share"),
			I18N.t("dialog.assistant_statistics.position"), I18N.t("dialog.assistant_statistics.opened"),
			I18N.t("dialog.assistant_statistics.viewed"), I18N.t("dialog.assistant_statistics.ignored"),
			I18N.t("dialog.assistant_statistics.snoozed"), I18N.t("dialog.assistant_statistics.restored")};
	}

	/** @return the cells of a row: numbers are numbers, so that the table sorts them as such */
	private static Object[] rowOf(final AssistantStatistics.Row row){
		return new Object[]{I18N.t("assistant.kind." + row.kind(), row.kind()), row.count(Event.SHOWN), row.count(Event.ACTED),
			(row.actedShare() >= 0.? Math.round(row.actedShare() * 1_000.) / 10.: null),
			(row.averagePosition() >= 0.? Math.round(row.averagePosition() * 10.) / 10.: null), row.count(Event.OPENED),
			row.count(Event.VIEWED), row.ignored(), row.count(Event.SNOOZED), row.count(Event.RESTORED)};
	}

	private static String summary(final AssistantStatistics statistics, final int kinds){
		final String enabled = I18N.t(statistics.isEnabled()? "dialog.assistant_statistics.on": "dialog.assistant_statistics.off");
		return "<html><body style='width: 780px'>" + I18N.tf("dialog.assistant_statistics.summary", enabled, kinds, statistics.numberOfSets(),
			AssistantStatistics.file()) + "</body></html>";
	}

	/** @return the names of the columns, to look at what the page shows */
	static List<String> columnNames(final JPanel panel){
		final JScrollPane scroll = (JScrollPane)panel.getComponent(1);
		final JTable table = (JTable)scroll.getViewport().getView();
		return java.util.stream.IntStream.range(0, table.getColumnCount())
			.mapToObj(table::getColumnName)
			.toList();
	}

}
