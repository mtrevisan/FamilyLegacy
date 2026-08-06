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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.VariantHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.io.Serial;


/* DONE */
/**
 * Dialog for editing a {@code TEXT_VALUE_VARIANT} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * TextValueVariant = oneof {
 *   phonetic: struct {
 *     system: Text
 *     value: Text
 *   }
 *   transcription: struct {
 *     system: enum { romaji, pinyin, wadegiles } | Text
 *     type?: enum { romanized, anglicized, cyrillized, francized, gairaigized, latinized } | Text
 *     value: Text
 *   }
 * }
 * </pre>
 */
public class TextValueVariantDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -4887775439277994973L;


	private static final String TAG_PHONETIC = "PHONETIC";
	private static final String TAG_TRANSCRIPTION = "TRANSCRIPTION";
	private static final String TAG_SYSTEM = "SYSTEM";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_VALUE = "VALUE";


	static{
		HandlerRegistry.register(new VariantHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final JRadioButton phoneticRadio = new JRadioButton("Phonetic", true);
	private final JRadioButton transcriptionRadio = new JRadioButton("Transcription");
	private final JLabel systemLabel = new JLabel("System*:");
	private final BoundTextField phoneticSystemField;
	private final BoundComboBox<String> transcriptionSystemCombo;
	private final JLabel typeLabel = new JLabel("Type:");
	private final BoundComboBox<String> typeCombo;
	private final JLabel valueLabel = new JLabel("Value*:");
	private final BoundTextField valueField;


	public static TextValueVariantDialog createNew(final Dialog parent, final FLEFModel model){
		return new TextValueVariantDialog(parent, model, null);
	}

	public static TextValueVariantDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return new TextValueVariantDialog(parent, model, record);
	}


	private TextValueVariantDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(VariantHandler.TYPE));

		phoneticSystemField = new BoundTextField(TAG_SYSTEM, 15);
		phoneticSystemField.setToolTipText("e.g., 'ipa', 'romaji', 'pinyin', 'wadegiles'");
		transcriptionSystemCombo = new BoundComboBox<>(TAG_SYSTEM,
			new String[]{StringUtils.EMPTY, "romaji", "pinyin", "wadegiles"});
		transcriptionSystemCombo.setEditable(true);
		typeCombo = new BoundComboBox<>(TAG_TYPE,
			new String[]{StringUtils.EMPTY, "romanized", "anglicized", "cyrillized", "francized", "gairaigized", "latinized"});
		typeCombo.setEditable(true);
		valueField = new BoundTextField(TAG_VALUE, 20);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(phoneticSystemField);
		bindingManager.bind(transcriptionSystemCombo);
		bindingManager.bind(typeCombo);
		bindingManager.bind(valueField);

		final ButtonGroup group = new ButtonGroup();
		group.add(phoneticRadio);
		group.add(transcriptionRadio);

		final JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		radioPanel.add(phoneticRadio);
		radioPanel.add(transcriptionRadio);

		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,hidemode 3", "[right]rel[grow]", "[]5[]5[]5[]"));
		panel.add(new JLabel("Variant Kind:"), "align label");
		panel.add(radioPanel, "growx,wrap");

		panel.add(systemLabel, "align label");
		panel.add(phoneticSystemField, "growx,wrap");
		panel.add(transcriptionSystemCombo, "growx,wrap");

		panel.add(typeLabel, "align label");
		panel.add(typeCombo, "growx,wrap");

		panel.add(valueLabel, "align label");
		panel.add(valueField, "growx,wrap");

		phoneticRadio.addActionListener(e -> updateFieldsState());
		transcriptionRadio.addActionListener(e -> updateFieldsState());

		final JButton okBtn = new JButton("OK");
		final JButton cancelBtn = new JButton("Cancel");
		okBtn.addActionListener(e -> save());
		cancelBtn.addActionListener(e -> dispose());

		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(okBtn);
		buttonPanel.add(cancelBtn);

		setLayout(new MigLayout("ins 10,fillx,top"));
		add(panel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);

		updateFieldsState();
	}

	@Override
	public void loadData(){
		bindingManager.load(record);

		String tag = record.getTag();
		if(TAG_PHONETIC.equals(tag))
			phoneticRadio.setSelected(true);
		else if(TAG_TRANSCRIPTION.equals(tag))
			transcriptionRadio.setSelected(true);

		updateFieldsState();
	}

	private void updateFieldsState(){
		final boolean isPhonetic = phoneticRadio.isSelected();
		final boolean isTranscription = transcriptionRadio.isSelected();

		// Toggle visibility for System components
		phoneticSystemField.setVisible(isPhonetic);
		transcriptionSystemCombo.setVisible(isTranscription);

		// Toggle visibility for Type components
		typeLabel.setVisible(isTranscription);
		typeCombo.setVisible(isTranscription);

		pack();
	}

	@Override
	protected boolean validData(){
		if(phoneticRadio.isSelected() && phoneticSystemField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this, "System field is required.",
				null, null, phoneticSystemField);

			return false;
		}

		if(transcriptionRadio.isSelected() && transcriptionSystemCombo.getSelectedIndex() < 0){
			GUIHelper.showValidationErrorAndFocus(this, "System field is required.",
				null, null, transcriptionSystemCombo);

			return false;
		}

		if(valueField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this, "Value field is required.",
				null, null, valueField);

			return false;
		}

		return true;
	}

	@Override
	public void saveData(){
		if(phoneticRadio.isSelected())
			record.setTag(TAG_PHONETIC);
		else if(transcriptionRadio.isSelected())
			record.setTag(TAG_TRANSCRIPTION);
		bindingManager.save(record);
	}

}
