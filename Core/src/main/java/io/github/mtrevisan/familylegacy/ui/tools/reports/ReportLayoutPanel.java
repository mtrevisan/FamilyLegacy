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
package io.github.mtrevisan.familylegacy.ui.tools.reports;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.reports.ReportLayout;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Component;
import java.util.Locale;


/**
 * The layout of the pages of a report (paper, margin, title at the top of the pages, footnote, text under the title): it is shared by
 * every type of report and has an effect on the formats that have pages (PDF, Word).
 */
public final class ReportLayoutPanel extends JPanel{

	/** The margins offered, in millimetres: 0 is the one of the format. */
	private static final Integer[] MARGINS = {0, 15, 20, 25, 30, 35, 40};

	private final JComboBox<ReportLayout.PaperSize> paper = new JComboBox<>(ReportLayout.PaperSize.values());
	private final JComboBox<Integer> margin = new JComboBox<>(MARGINS);
	private final JCheckBox pageHeadings = new JCheckBox(I18N.t("checkbox.report.page_headings"));
	private final JTextField footnote = new JTextField();
	private final JTextArea coverText = new JTextArea(3, 20);


	public ReportLayoutPanel(final ReportLayout initial){
		super(new MigLayout("ins 4 8 8 8,wrap 2,fillx", "[][grow,fill]"));
		setBorder(BorderFactory.createTitledBorder(I18N.t("border.report.layout")));
		setToolTipText(I18N.t("tooltip.report.layout"));

		paper.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus){
				final Object text = (value instanceof ReportLayout.PaperSize size
					? I18N.t("enum.paper_size." + size.name().toLowerCase(Locale.ROOT)): value);
				return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
			}
		});
		margin.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus){
				final Object text = (Integer.valueOf(0).equals(value)
					? I18N.t("text.report.margin.default"): I18N.tf("text.report.margin.mm", value));
				return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
			}
		});
		margin.setToolTipText(I18N.t("tooltip.report.margin"));
		pageHeadings.setToolTipText(I18N.t("tooltip.report.page_headings"));
		footnote.setToolTipText(I18N.t("tooltip.report.footnote"));
		coverText.setToolTipText(I18N.t("tooltip.report.cover_text"));
		coverText.setLineWrap(true);
		coverText.setWrapStyleWord(true);

		add(new JLabel(I18N.l("label.report.paper")));
		add(paper);
		add(new JLabel(I18N.l("label.report.margin")));
		add(margin);
		add(pageHeadings, "span 2");
		add(new JLabel(I18N.l("label.report.footnote")));
		add(footnote);
		add(new JLabel(I18N.l("label.report.cover_text")), "top");
		add(new JScrollPane(coverText));

		reset(initial);
	}


	/** Shows the layout. */
	public void reset(final ReportLayout layout){
		paper.setSelectedItem(layout.paper());
		margin.setSelectedItem(layout.marginMm());
		pageHeadings.setSelected(layout.pageHeadings());
		footnote.setText(layout.footnote());
		coverText.setText(layout.coverText());
	}

	/** @return	The layout the user chose. */
	public ReportLayout toLayout(){
		return new ReportLayout((ReportLayout.PaperSize)paper.getSelectedItem(),
			(margin.getSelectedItem() instanceof final Integer value? value: 0), pageHeadings.isSelected(), footnote.getText(),
			coverText.getText());
	}

}
