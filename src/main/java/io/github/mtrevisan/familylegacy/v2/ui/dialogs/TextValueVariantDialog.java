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
 *     system: enum {
 *       romaji, hepburn, kunreishiki, nihonshiki,
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
		return createNew(parent, model, TextValueVariantDialog::new);
	}

	public static TextValueVariantDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, TextValueVariantDialog::new);
	}


	private TextValueVariantDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(VariantHandler.TYPE));

		phoneticSystemField = new BoundTextField(TAG_SYSTEM, 15);
		phoneticSystemField.setToolTipText("e.g., 'ipa', 'romaji', 'pinyin', 'wadegiles'");
		transcriptionSystemCombo = new BoundComboBox<>(TAG_SYSTEM, new String[]{
			StringUtils.EMPTY,
			"romaji", "hepburn", "kunreishiki", "nihonshiki",
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
		typeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{
			StringUtils.EMPTY,
			"romanized", "latinized", "anglicized", "francized", "germanized", "italianized", "hispanicized",
			"lusitanized", "cyrillized", "arabized", "hebraized", "hellenized", "gairaigized", "modernized", "normalized"
		});
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


		setLayout(new MigLayout("ins 10,fillx,hidemode 3", "[right]rel[grow]", "[]15[]5[]5[]5[]"));

		final ButtonGroup group = new ButtonGroup();
		group.add(phoneticRadio);
		group.add(transcriptionRadio);

		final JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		radioPanel.add(phoneticRadio);
		radioPanel.add(transcriptionRadio);

		add(new JLabel("Variant Kind:"), "align label");
		add(radioPanel, "growx,wrap");

		add(systemLabel, "align label");
		add(phoneticSystemField, "growx,wrap");
		add(transcriptionSystemCombo, "growx,wrap");

		add(typeLabel, "align label");
		add(typeCombo, "growx,wrap");

		add(valueLabel, "align label");
		add(valueField, "growx,wrap");

		phoneticRadio.addActionListener(e -> updateFieldsState());
		transcriptionRadio.addActionListener(e -> updateFieldsState());

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);

		updateFieldsState();
	}


	@Override
	protected void loadData(){
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
			GUIHelper.showValidationErrorAndFocus(this,
				"System is required.",
				null, null, phoneticSystemField);

			return false;
		}

		if(transcriptionRadio.isSelected() && !transcriptionSystemCombo.isValued()){
			GUIHelper.showValidationErrorAndFocus(this,
				"System cannot be empty.",
				null, null, transcriptionSystemCombo);

			return false;
		}

		if(valueField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Value is required.",
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


	public static void main(final String[] args){
		GUIHelper.launch(TextValueVariantDialog::createNew);
	}

}
