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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs.structures;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.TextValueVariantHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.io.Serial;


/**
 * Dialog for editing a {@code TEXT_VALUE_VARIANT} according to FLEF 0.1.2.
 * <p>
 * Structure:
 * <pre>
 * TextValueVariant = oneof {
 *   phonetic: struct {
 *     system: Text
 *     value: Text
 *   }
 *   transcription: struct {
 *     system: enum {
 *       rōmaji, hepburn, kunreishiki, nihonshiki,
 *       pinyin, wadegiles,
 *       bgn_pcgn,
 *       iso9,
 *       ala_lc,
 *       dmg,
 *       buckwalter,
 *       iso233,
 *       iso259,
 *       iast,
 *       iso15919, hunterian,
 *       mccune_reischauer, revised_korean,
 *       scientific
 *     } | Text
 *     type?: enum {
 *       romanized, latinized, anglicized, francized, germanized, italianized, hispanicized, lusitanized, cyrillized,
 *       arabized, hebraized, hellenized, gairaigized, modernized, normalized
 *     } | Text
 *     value: Text
 *   }
 * }
 * </pre>
 */
public class TextValueVariantStructureDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -4887775439277994973L;


	private static final String DOT = ".";

	private static final String TAG_PHONETIC = "PHONETIC";
	private static final String TAG_TRANSCRIPTION = "TRANSCRIPTION";
	private static final String TAG_SYSTEM = "SYSTEM";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_PHONETIC_SYSTEM = TAG_PHONETIC + DOT + TAG_SYSTEM;
	private static final String TAG_PHONETIC_VALUE = TAG_PHONETIC + DOT + TAG_VALUE;
	private static final String TAG_TRANSCRIPTION_SYSTEM = TAG_TRANSCRIPTION + DOT + TAG_SYSTEM;
	private static final String TAG_TRANSCRIPTION_TYPE = TAG_TRANSCRIPTION + DOT + TAG_TYPE;
	private static final String TAG_TRANSCRIPTION_VALUE = TAG_TRANSCRIPTION + DOT + TAG_VALUE;


	private final RecordDialogComponents components;

	private final JPanel propertiesPanel;

	private final JRadioButton phoneticRadio = new JRadioButton("Phonetic", true);
	private final JRadioButton transcriptionRadio = new JRadioButton("Transcription");
	private final BoundTextField phoneticSystemField;
	private final BoundComboBox<String> transcriptionSystemCombo;
	private final BoundComboBox<String> typeCombo;
	private final BoundTextField valueField;


	public static TextValueVariantStructureDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, TextValueVariantStructureDialog::new);
	}

	public static TextValueVariantStructureDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, TextValueVariantStructureDialog::new);
	}


	private TextValueVariantStructureDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, TextValueVariantHandler.class);

		propertiesPanel = GUIHelper.createLabelFieldPanel(0, "[]15[]5[]5[]5[]");

		phoneticSystemField = new BoundTextField(TAG_PHONETIC_SYSTEM);
		phoneticSystemField.setToolTipText("e.g., 'ipa', 'rōmaji', 'pinyin', 'wadegiles'");
		transcriptionSystemCombo = new BoundComboBox<>(TAG_TRANSCRIPTION_SYSTEM, new String[]{
			StringUtils.EMPTY,
			"rōmaji", "hepburn", "kunreishiki", "nihonshiki",
			"pinyin", "wadegiles",
			"bgn_pcgn",
			"iso9",
			"ala_lc",
			"dmg",
			"buckwalter",
			"iso233",
			"iso259",
			"iast",
			"iso15919", "hunterian",
			"mccune_reischauer", "revised_korean",
			"scientific"
		});
		transcriptionSystemCombo.setEditable(true);
		typeCombo = new BoundComboBox<>(TAG_TRANSCRIPTION_TYPE, new String[]{
			StringUtils.EMPTY,
			"romanized", "latinized", "anglicized", "francized", "germanized", "italianized", "hispanicized",
			"lusitanized", "cyrillized", "arabized", "hebraized", "hellenized", "gairaigized", "modernized", "normalized"
		});
		typeCombo.setEditable(true);
		valueField = new BoundTextField(TAG_VALUE);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.build();

		components.bind(phoneticSystemField);
		components.bind(transcriptionSystemCombo);
		components.bind(typeCombo);
		components.bind(valueField);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		final ButtonGroup group = new ButtonGroup();
		group.add(phoneticRadio);
		group.add(transcriptionRadio);

		final JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		radioPanel.add(phoneticRadio);
		radioPanel.add(transcriptionRadio);

		GUIHelper.addLabeledComponent(propertiesPanel, "Variant Kind:", radioPanel);

		propertiesPanel.add(new JLabel("System*:"), "align label");
		propertiesPanel.add(phoneticSystemField, "growx,wrap");
		propertiesPanel.add(transcriptionSystemCombo, "growx,wrap");

		GUIHelper.addLabeledComponent(propertiesPanel, "Type:", typeCombo);

		GUIHelper.addLabeledComponent(propertiesPanel, "Value*:", valueField);

		phoneticRadio.addActionListener(e -> updateFieldsState());
		transcriptionRadio.addActionListener(e -> updateFieldsState());

		updateFieldsState();

		return propertiesPanel;
	}


	@Override
	protected void loadData(){
		final FLEFRecord phonetic = record.getTheOnlyChild(TAG_PHONETIC);
		final FLEFRecord transcription = record.getTheOnlyChild(TAG_TRANSCRIPTION);
		if(phonetic != null)
			valueField.setPath(TAG_PHONETIC_VALUE);
		else if(transcription != null)
			valueField.setPath(TAG_TRANSCRIPTION_VALUE);

		components.load(record);

		if(phonetic != null)
			phoneticRadio.setSelected(true);
		else if(transcription != null)
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
		GUIHelper.setComponentVisible(typeCombo, isTranscription);

		pack();
	}

	@Override
	protected boolean validData(){
		if(phoneticRadio.isSelected() && phoneticSystemField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"System is required.",
				tabbedPane, propertiesPanel, phoneticSystemField);

			return false;
		}

		if(transcriptionRadio.isSelected() && !transcriptionSystemCombo.isValued()){
			GUIHelper.showValidationErrorAndFocus(this,
				"System cannot be empty.",
				tabbedPane, propertiesPanel, transcriptionSystemCombo);

			return false;
		}

		if(valueField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Value is required.",
				tabbedPane, propertiesPanel, valueField);

			return false;
		}

		return true;
	}

	@Override
	public void saveData(){
		if(phoneticRadio.isSelected()){
			record.setTag(TAG_PHONETIC);
			valueField.setPath(TAG_PHONETIC_VALUE);
		}
		else if(transcriptionRadio.isSelected()){
			record.setTag(TAG_TRANSCRIPTION);
			valueField.setPath(TAG_TRANSCRIPTION_VALUE);
		}

		components.save(record);
	}


	public static void main(final String[] args){
		GUIHelper.launch(TextValueVariantStructureDialog::createNew);
	}

}
