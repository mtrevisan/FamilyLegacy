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
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.research.GpsProofStatement;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;


/**
 * The proof statement of a research question (or of a conclusion), written from what the file holds (see {@link GpsProofStatement}).
 * The text can be edited before it is copied, or kept as a note of the research question (a research note called "Proof statement",
 * that is replaced when the statement is saved again): the researcher completes and corrects it, nothing is written without asking.
 */
final class GpsProofStatementDialog extends JDialog{

	private final FLEFModel model;
	/** The question the statement is kept with, {@code null} if the subject is a conclusion that answers none. */
	private final FLEFRecord question;
	private final JTextArea text = new JTextArea(26, 90);


	GpsProofStatementDialog(final Window owner, final FLEFModel model, final FLEFRecord subject){
		super(owner, I18N.t("dialog.gps.statement.title"), ModalityType.APPLICATION_MODAL);

		ToolDialogs.installEscapeToClose(this);

		this.model = model;
		final GpsWorkflow.Assessment assessment = GpsWorkflow.assess(model, subject);
		question = (assessment.questions().isEmpty()? null: assessment.questions().getFirst());

		text.setLineWrap(true);
		text.setWrapStyleWord(true);
		text.setText(GpsProofStatement.render(model, assessment));
		text.setCaretPosition(0);

		final JButton save = new JButton(I18N.t("button.gps.save_statement"));
		save.setEnabled(question != null && ResearchQuestionReader.isOfType(question));
		save.addActionListener(e -> save());
		final JButton close = new JButton(I18N.t("button.close"));
		close.addActionListener(e -> dispose());

		setLayout(new BorderLayout(8, 8));
		final JScrollPane scroll = new JScrollPane(text);
		scroll.setPreferredSize(new Dimension(820, 520));
		add(scroll, BorderLayout.CENTER);
		add(ToolDialogs.createFooter(" ", save, ToolDialogs.createCopyButton(text), close), BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(owner);
	}


	/** Keeps the text as the research note "Proof statement" of the question, replacing the one a previous save made. */
	private void save(){
		final String title = I18N.t("text.gps.statement.note_title");
		FLEFRecord note = null;
		for(final FLEFRecord candidate : FLEFRecordHelper.findChildren(question, NoteReader.TAG_NOTE))
			if(title.equals(FLEFRecordHelper.getChildValue(candidate, NoteReader.TAG_TITLE)) && NoteReader.isResearch(candidate)){
				note = candidate;
				break;
			}

		if(note == null){
			question.addChild(FLEFRecord.createChildWithTag(NoteReader.TAG_NOTE)
				.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TITLE, title))
				.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_KIND, NoteReader.ENUM_KIND_RESEARCH))
				.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TEXT, text.getText()))
				.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_MIME, "text/markdown")));
		}
		else{
			FLEFRecordHelper.updateChildValue(note, NoteReader.TAG_TEXT, text.getText());
			FLEFRecordHelper.updateChildValue(note, NoteReader.TAG_MIME, "text/markdown");
		}
		MessageDialogs.show(this, I18N.t("text.gps.statement.saved"), getTitle(), JOptionPane.INFORMATION_MESSAGE);
	}

}
