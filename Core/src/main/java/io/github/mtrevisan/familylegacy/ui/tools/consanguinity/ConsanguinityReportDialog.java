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
package io.github.mtrevisan.familylegacy.ui.tools.consanguinity;

import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityReport;
import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityReportText;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.util.function.Function;


/**
 * Modal dialog that shows the consanguinity report: the couples whose partners are related and the inbred individuals, with the
 * coefficients of Wright. It is read-only: it changes neither the model nor the views.
 */
public class ConsanguinityReportDialog extends JDialog{

	private static final Font MONO_FONT = new Font("Monospaced", Font.PLAIN, 12);

	private final ConsanguinityReport report;
	private final Function<String, String> names;


	/**
	 * @param owner  the parent window; may be {@code null}
	 * @param report the report to show
	 * @param names  the name to show of an individual
	 */
	public ConsanguinityReportDialog(final Window owner, final ConsanguinityReport report, final Function<String, String> names){
		super(owner, I18N.t("dialog.consanguinity_report.title"), ModalityType.APPLICATION_MODAL);
		ToolDialogs.installEscapeToClose(this);

		if(report == null)
			throw new IllegalArgumentException("Report must not be null");

		this.report = report;
		this.names = names;

		initComponents();

		pack();
		setMinimumSize(new Dimension(700, 460));
		setLocationRelativeTo(owner);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}


	private void initComponents(){
		final JPanel main = new JPanel(new MigLayout("ins 12,fill", "[grow]", "[]8[grow]12[]"));

		final JLabel header = new JLabel(buildHeaderText());
		header.setFont(header.getFont().deriveFont(Font.BOLD));
		main.add(header, "growx,wrap");

		final JTextArea area = new JTextArea(ConsanguinityReportText.format(report, names));
		area.setEditable(false);
		area.setFont(MONO_FONT);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setCaretPosition(0);
		area.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		final JScrollPane scroll = new JScrollPane(area);
		main.add(scroll, "grow,push,wrap");

		final JButton closeButton = new JButton(I18N.t("button.close"));
		closeButton.addActionListener(e -> dispose());
		final JPanel footer = new JPanel(new MigLayout("ins 0", "[grow][]", "[]"));
		footer.add(new JLabel(), "growx");
		footer.add(closeButton);
		main.add(footer, "growx");

		setLayout(new BorderLayout());
		add(main, BorderLayout.CENTER);

		getRootPane().setDefaultButton(closeButton);
	}

	private String buildHeaderText(){
		return I18N.tf("dialog.consanguinity_report.header", String.valueOf(report.unions().size()),
			String.valueOf(report.inbredIndividuals().size()));
	}

}
