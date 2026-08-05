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
package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import java.awt.Component;
import java.io.Serial;


/**
 * Reusable panel for editing a TRANSCRIBED_TEXT structure.
 * <p>
 * Structure:
 * <pre>
 * TRANSCRIBED_TEXT :=
 *   n PHONETIC <PHONETIC_SYSTEM>    {0:1}
 *     +1 VALUE <PHONETIC_NAME_PIECE>    {1:1}
 *   n TRANSCRIPTION <TRANSCRIPTION_SYSTEM>    {0:1}
 *     +1 TYPE <TRANSCRIPTION_TYPE>    {0:1}
 *     +1 VALUE <TRANSCRIPTION_NAME_PIECE>    {1:1}
 * </pre>
 */
public class TranscribedTextPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -6313838382210691780L;


	private static final String TAG_PHONETIC = "PHONETIC";
	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_TRANSCRIPTION = "TRANSCRIPTION";
	private static final String TAG_TYPE = "TYPE";


	private final Component parent;

	private final JTextField phoneticSystemField = new JTextField(15);
	private final JTextField phoneticValueField = new JTextField(20);

	private final JTextField transcriptionSystemField = new JTextField(15);
	private final JComboBox<String> transcriptionTypeCombo = new JComboBox<>(new String[]{
		StringUtils.EMPTY, "romanized", "anglicized", "cyrillized", "francized",
		"gairaigized", "latinized"
	});
	private final JTextField transcriptionValueField = new JTextField(20);

	/**
	 * Creates a new TranscribedTextPanel.
	 *
	 * @param parent the parent component (for showing dialogs)
	 */
	public TranscribedTextPanel(Component parent){
		this.parent = parent;
		initComponents();
	}


	private void initComponents(){
		setLayout(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]5[]5[]"));
		setBorder(new TitledBorder("Trasncribed Text"));

		add(new JLabel("Phonetic System:"), "align label");
		add(phoneticSystemField, "growx,wrap");

		add(new JLabel("Phonetic Value:"), "align label");
		add(phoneticValueField, "growx,wrap");

		add(new JSeparator(), "span 2,growx,wrap");

		add(new JLabel("Transcription System:"), "align label");
		add(transcriptionSystemField, "growx,wrap");

		add(new JLabel("Transcription Type:"), "align label");
		add(transcriptionTypeCombo, "growx,wrap");

		add(new JLabel("Transcription Value:"), "align label");
		add(transcriptionValueField, "growx");
	}


	/**
	 * Loads data from a TRANSCRIBED_TEXT FLEFRecord.
	 *
	 * @param transRecord the TRANSCRIBED_TEXT record (may be null)
	 */
	public void loadFromRecord(FLEFRecord transRecord){
		// Clear all fields
		phoneticSystemField.setText(StringUtils.EMPTY);
		phoneticValueField.setText(StringUtils.EMPTY);
		transcriptionSystemField.setText(StringUtils.EMPTY);
		transcriptionTypeCombo.setSelectedItem(StringUtils.EMPTY);
		transcriptionValueField.setText(StringUtils.EMPTY);

		if(transRecord == null){
			return;
		}

		// PHONETIC (0:1)
		FLEFRecord phonetic = FLEFRecordHelper.findChild(transRecord, "PHONETIC");
		if(phonetic != null){
			phoneticSystemField.setText(phonetic.getValue());
			FLEFRecord value = FLEFRecordHelper.findChild(phonetic, "VALUE");
			if(value != null){
				phoneticValueField.setText(value.getValue());
			}
		}

		// TRANSCRIPTION (0:1)
		FLEFRecord transcription = FLEFRecordHelper.findChild(transRecord, "TRANSCRIPTION");
		if(transcription != null){
			transcriptionSystemField.setText(transcription.getValue());
			FLEFRecord type = FLEFRecordHelper.findChild(transcription, "TYPE");
			if(type != null){
				transcriptionTypeCombo.setSelectedItem(type.getValue());
			}
			FLEFRecord value = FLEFRecordHelper.findChild(transcription, "VALUE");
			if(value != null){
				transcriptionValueField.setText(value.getValue());
			}
		}
	}

	/**
	 * Saves data to a TRANSCRIBED_TEXT FLEFRecord.
	 * The record will be created if null.
	 *
	 * @param transRecord the TRANSCRIBED_TEXT record to save into (may be null)
	 * @return the saved record (new if null was passed), or null if validation fails
	 */
	public FLEFRecord saveToRecord(FLEFRecord transRecord){
		// Validate before saving
		if(!validateData()){
			return null;
		}

		if(transRecord == null){
			transRecord = FLEFRecord.createChild("TRANSCRIBED_TEXT");
		}

		// Clear existing children
		FLEFRecordHelper.removeAllChildren(transRecord);

		String phoneticSys = phoneticSystemField.getText().trim();
		String phoneticVal = phoneticValueField.getText().trim();

		if(!phoneticSys.isEmpty() || !phoneticVal.isEmpty()){
			FLEFRecord phonetic = FLEFRecord.createChildWithValue(TAG_PHONETIC, phoneticSys);
			transRecord.addChild(phonetic);

			// VALUE - required if PHONETIC exists
			if(!phoneticVal.isEmpty()){
				FLEFRecord value = FLEFRecord.createChildWithValue(TAG_VALUE, phoneticVal);
				phonetic.addChild(value);
			}
		}

		String transSys = transcriptionSystemField.getText().trim();
		String transType = (String)transcriptionTypeCombo.getSelectedItem();
		String transVal = transcriptionValueField.getText().trim();

		if(!transSys.isEmpty() || !transVal.isEmpty()){
			FLEFRecord transcription = FLEFRecord.createChildWithValue(TAG_TRANSCRIPTION, transSys);
			transRecord.addChild(transcription);

			// TYPE (0:1)
			if(transType != null && !transType.isEmpty()){
				FLEFRecord type = FLEFRecord.createChildWithValue(TAG_TYPE, transType);
				transcription.addChild(type);
			}

			// VALUE - required if TRANSCRIPTION exists
			if(!transVal.isEmpty()){
				FLEFRecord value = FLEFRecord.createChildWithValue(TAG_VALUE, transVal);
				transcription.addChild(value);
			}
		}

		return transRecord;
	}

	/**
	 * Validates that required fields (VALUE under PHONETIC and VALUE under TRANSCRIPTION)
	 * are filled if their parent sections are present.
	 *
	 * @return true if valid, false otherwise
	 */
	public boolean validateData(){
		// If no data at all, validation passes
		if(!hasData()){
			return true;
		}

		// Check PHONETIC: if SYSTEM is filled, VALUE must be filled
		String phoneticSys = phoneticSystemField.getText().trim();
		String phoneticVal = phoneticValueField.getText().trim();
		if(!phoneticSys.isEmpty() && phoneticVal.isEmpty()){
			JOptionPane.showMessageDialog(parent,
				"Phonetic VALUE is required when PHONETIC system is provided.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			phoneticValueField.requestFocusInWindow();

			return false;
		}

		// Check TRANSCRIPTION: if SYSTEM is filled, VALUE must be filled
		String transSys = transcriptionSystemField.getText().trim();
		String transVal = transcriptionValueField.getText().trim();
		if(!transSys.isEmpty() && transVal.isEmpty()){
			JOptionPane.showMessageDialog(parent,
				"Transcription VALUE is required when TRANSCRIPTION system is provided.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			transcriptionValueField.requestFocusInWindow();

			return false;
		}

		return true;
	}

	/**
	 * Checks if the transcribed text has any data.
	 *
	 * @return true if any field has data
	 */
	public boolean hasData(){
		return !StringUtils.isEmpty(phoneticSystemField.getText()) ||
			!StringUtils.isEmpty(phoneticValueField.getText()) ||
			!StringUtils.isEmpty(transcriptionSystemField.getText()) ||
			(transcriptionTypeCombo.getSelectedItem() != null &&
				!((String)transcriptionTypeCombo.getSelectedItem()).isEmpty()) ||
			!StringUtils.isEmpty(transcriptionValueField.getText());
	}

	/**
	 * Clears all fields in the panel.
	 */
	public void clear(){
		phoneticSystemField.setText(StringUtils.EMPTY);
		phoneticValueField.setText(StringUtils.EMPTY);
		transcriptionSystemField.setText(StringUtils.EMPTY);
		transcriptionTypeCombo.setSelectedItem(StringUtils.EMPTY);
		transcriptionValueField.setText(StringUtils.EMPTY);
	}


	public String getPhoneticSystem(){
		return phoneticSystemField.getText().trim();
	}

	public String getPhoneticValue(){
		return phoneticValueField.getText().trim();
	}

	public String getTranscriptionSystem(){
		return transcriptionSystemField.getText().trim();
	}

	public String getTranscriptionType(){
		return (String)transcriptionTypeCombo.getSelectedItem();
	}

	public String getTranscriptionValue(){
		return transcriptionValueField.getText().trim();
	}

}
