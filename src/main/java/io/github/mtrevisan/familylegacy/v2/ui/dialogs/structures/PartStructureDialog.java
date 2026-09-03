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
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.TextValueVariantListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PartHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.Serial;


/**
 * Structure:
 * <pre>
 * struct {
 *     type: enum {
 *       given, generation,
 *       patronymic, matronymic, kunya,
 *       family, family_nickname, lineage, house, clan, tribal, caste,
 *       toponymic,
 *       title, occupational, prefix, suffix,
 *       nickname, regnal, religious, posthumous
 *     } | Text
 *     value: Text
 *     variant*: TextValueVariant
 *   }
 * </pre>
 */
public class PartStructureDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 3227495851403391698L;


	private static final String TAG_PART = "PART";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_VARIANT = "VARIANT";


	private final RecordDialogComponents components;

	private final JPanel propertiesPanel;

	private final BoundComboBox<String> typeCombo;
	private final BoundTextField valueField;
	private final TextValueVariantListPanel variantPanel;


	public static PartStructureDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, PartStructureDialog::new);
	}

	public static PartStructureDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, PartStructureDialog::new);
	}


	private PartStructureDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, PartHandler.class);

		propertiesPanel = GUIHelper.createLabelFieldPanel(0, "[]10[]15[]");

		typeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{
			StringUtils.EMPTY,
			"given", "generation",
			"patronymic", "matronymic", "kunya",
			"family", "family_nickname", "lineage", "house", "clan", "tribal", "caste",
			"toponymic",
			"title", "occupational", "prefix", "suffix",
			"nickname", "regnal", "religious", "posthumous"
		});
		typeCombo.setEditable(true);
		valueField = new BoundTextField(TAG_VALUE);
		variantPanel = new TextValueVariantListPanel(TAG_VARIANT, this, "Variant", model);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.build();

		components.bind(typeCombo);
		components.bind(valueField);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// type
		GUIHelper.addLabeledComponent(propertiesPanel, "Part Type*:", typeCombo);

		// value
		GUIHelper.addLabeledComponent(propertiesPanel, "Value*:", valueField);

		// variant
		GUIHelper.addComponent(propertiesPanel, variantPanel);

		return propertiesPanel;
	}


	@Override
	protected void loadData(){
		components.load(record);

		variantPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(valueField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Value cannot be empty.",
				tabbedPane, propertiesPanel, valueField);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		components.save(record);

		variantPanel.save(record);
	}


	public static void main(final String[] args){
		GUIHelper.launch(PartStructureDialog::createNew);
	}

}
