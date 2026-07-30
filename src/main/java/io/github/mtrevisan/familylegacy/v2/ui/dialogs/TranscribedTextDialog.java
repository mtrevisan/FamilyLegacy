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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.Serial;


/**
 * Dialog for editing a {@code TRANSCRIBED_TEXT} structure according to FLEF 0.0.9.
 * <p>
 * Structure:
 * <pre>
 * n PHONETIC <PHONETIC_SYSTEM>    {0:1}
 *   +1 VALUE <PHONETIC_NAME_PIECE>    {1:1}
 * n TRANSCRIPTION <TRANSCRIPTION_SYSTEM>    {0:1}
 *   +1 TYPE <TRANSCRIPTION_TYPE>    {0:1}
 *   +1 VALUE <TRANSCRIPTION_NAME_PIECE>    {1:1}
 * </pre>
 */
public class TranscribedTextDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = -2491081033037415929L;


	private final FLEFRecord transRecord;
	private boolean saved = false;

	private final JTextField phoneticSystemField = new JTextField(15);
	private final JTextField phoneticValueField = new JTextField(20);
	private final JTextField transcriptionSystemField = new JTextField(15);
	private final JTextField transcriptionTypeField = new JTextField(15);
	private final JTextField transcriptionValueField = new JTextField(20);

	private final JButton saveButton = new JButton("OK");
	private final JButton cancelButton = new JButton("Cancel");

	public TranscribedTextDialog(JDialog parent, FLEFRecord existing){
		super(parent, "Edit Transcribed Text", true);

		this.transRecord = existing != null? existing: FLEFRecord.createEmpty();
		initComponents();
		if(existing != null) loadData();
		pack();
		setMinimumSize(new Dimension(400, 300));
		setLocationRelativeTo(parent);
	}

	private void initComponents(){
		setLayout(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]"));

		// Phonetic section
		add(new JLabel("Phonetic System:"), "align label");
		add(phoneticSystemField, "growx,wrap");
		add(new JLabel("Phonetic Value:"), "align label");
		add(phoneticValueField, "growx,wrap");

		// Separator
		add(new JSeparator(), "span 2,growx,wrap");

		// Transcription section
		add(new JLabel("Transcription System:"), "align label");
		add(transcriptionSystemField, "growx,wrap");
		add(new JLabel("Transcription Type:"), "align label");
		add(transcriptionTypeField, "growx,wrap");
		add(new JLabel("Transcription Value:"), "align label");
		add(transcriptionValueField, "growx,wrap");

		// Buttons
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, "span 2,growx");

		saveButton.addActionListener(e -> {
			saveData();
			saved = true;
			dispose();
		});
		cancelButton.addActionListener(e -> dispose());
	}

	private void loadData(){
		// PHONETIC
		FLEFRecord phonetic = FLEFRecordUtils.findChild(transRecord, "PHONETIC");
		if(phonetic != null){
			phoneticSystemField.setText(phonetic.getValue());
			FLEFRecord value = FLEFRecordUtils.findChild(phonetic, "VALUE");
			if(value != null){
				phoneticValueField.setText(value.getValue());
			}
		}

		// TRANSCRIPTION
		FLEFRecord transcription = FLEFRecordUtils.findChild(transRecord, "TRANSCRIPTION");
		if(transcription != null){
			transcriptionSystemField.setText(transcription.getValue());
			FLEFRecord type = FLEFRecordUtils.findChild(transcription, "TYPE");
			if(type != null){
				transcriptionTypeField.setText(type.getValue());
			}
			FLEFRecord value = FLEFRecordUtils.findChild(transcription, "VALUE");
			if(value != null){
				transcriptionValueField.setText(value.getValue());
			}
		}
	}

	private void saveData(){
		FLEFRecordUtils.removeAllChildren(transRecord);

		// PHONETIC
		String phoneticSys = phoneticSystemField.getText().trim();
		String phoneticVal = phoneticValueField.getText().trim();
		if(!phoneticSys.isEmpty() || !phoneticVal.isEmpty()){
			FLEFRecord phonetic = FLEFRecord.createChildWithValue("PHONETIC", phoneticSys);
			transRecord.addChild(phonetic);
			if(!phoneticVal.isEmpty()){
				FLEFRecord value = FLEFRecord.createChildWithValue("VALUE", phoneticVal);
				phonetic.addChild(value);
			}
		}

		// TRANSCRIPTION
		String transSys = transcriptionSystemField.getText().trim();
		String transType = transcriptionTypeField.getText().trim();
		String transVal = transcriptionValueField.getText().trim();
		if(!transSys.isEmpty() || !transType.isEmpty() || !transVal.isEmpty()){
			FLEFRecord transcription = FLEFRecord.createChildWithValue("TRANSCRIPTION", transSys);
			transRecord.addChild(transcription);
			if(!transType.isEmpty()){
				FLEFRecord type = FLEFRecord.createChildWithValue("TYPE", transType);
				transcription.addChild(type);
			}
			if(!transVal.isEmpty()){
				FLEFRecord value = FLEFRecord.createChild("VALUE");
				value.setValue(transVal);
				transcription.addChild(value);
			}
		}
	}

	public boolean isSaved(){
		return saved;
	}

	public FLEFRecord getTranscribedTextRecord(){
		return transRecord;
	}

}
