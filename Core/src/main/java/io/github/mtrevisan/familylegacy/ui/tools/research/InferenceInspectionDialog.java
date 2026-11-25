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
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.research.InferenceInspection;
import io.github.mtrevisan.familylegacy.research.InferenceInspectionText;
import io.github.mtrevisan.familylegacy.research.ResearchHelper;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.util.List;


/**
 * Shows, for the conclusion picked from the list, the evidence it rests on, the reasoning chain, the confidence, the alternative
 * interpretations and the conflicting evidence: why, how and on which evidence (see {@link InferenceInspection}). Nothing is hidden
 * in a score: every line is read from the records of the conclusion and of the assertions it resolves.
 */
final class InferenceInspectionDialog extends JDialog{

	private final FLEFModel model;
	private final List<FLEFRecord> conclusions;
	private final JList<String> list;
	private final JTextArea report = new JTextArea();
	private final JButton openRecord = new JButton(I18N.t("button.open_record"));


	/**
	 * @param owner        the window the dialog belongs to
	 * @param model        the model
	 * @param conclusionId the conclusion to show first, or {@code null} for the first one
	 */
	InferenceInspectionDialog(final Window owner, final FLEFModel model, final String conclusionId){
		super(owner, I18N.t("dialog.inference.title"), ModalityType.APPLICATION_MODAL);

		ToolDialogs.installEscapeToClose(this);

		this.model = model;
		conclusions = ResearchHelper.listConclusions(model);

		final DefaultListModel<String> entries = new DefaultListModel<>();
		int selected = 0;
		for(int i = 0; i < conclusions.size(); i ++){
			final FLEFRecord conclusion = conclusions.get(i);
			final String issue = ResearchHelper.conclusionIssue(conclusion);
			entries.addElement(conclusion.getId() + (issue != null && !issue.isBlank()? " - " + issue: ""));
			if(conclusion.getId().equals(conclusionId))
				selected = i;
		}
		list = new JList<>(entries);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(e -> {
			if(!e.getValueIsAdjusting())
				refresh();
		});

		report.setEditable(false);
		report.setLineWrap(true);
		report.setWrapStyleWord(true);
		report.setFont(new Font(Font.MONOSPACED, Font.PLAIN, report.getFont().getSize()));
		report.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

		final JScrollPane listPane = new JScrollPane(list);
		listPane.setPreferredSize(new Dimension(220, 400));
		final JScrollPane reportPane = new JScrollPane(report);
		reportPane.setPreferredSize(new Dimension(640, 400));

		openRecord.addActionListener(e -> {
			if(list.getSelectedIndex() >= 0){
				ToolDialogs.openRecordEditor(this, model, conclusions.get(list.getSelectedIndex()));

				refresh();
			}
		});

		setLayout(new BorderLayout(8, 8));
		add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPane, reportPane), BorderLayout.CENTER);
		add(ToolDialogs.createFooter("", openRecord, ToolDialogs.createCopyButton(report)), BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(owner);

		if(!conclusions.isEmpty())
			list.setSelectedIndex(selected);
		else
			refresh();
	}


	/** The text shown for the selected conclusion: made again from the model every time, so it is what it is now. */
	String reportText(){
		return report.getText();
	}

	private void refresh(){
		final int index = list.getSelectedIndex();
		openRecord.setEnabled(index >= 0);
		if(index < 0){
			report.setText(I18N.t("text.inference.empty"));
			return;
		}

		report.setText(InferenceInspectionText.render(InferenceInspection.inspect(model, conclusions.get(index))));
		report.setCaretPosition(0);
	}

}
