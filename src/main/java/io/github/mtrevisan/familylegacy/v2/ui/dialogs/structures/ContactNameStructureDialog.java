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
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.TextValueVariantListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContactNameHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.Serial;


/**
 * Dialog for editing a {@code NAME_STRUCTURE} according to FLEF 0.1.2.
 * <p>
 * Structure:
 * <pre>
 * struct NameStructure {
 *   value: Text
 *   variant*: TextValueVariant
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): value, variant
 */
public class ContactNameStructureDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -6832652809158028331L;


	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_VARIANT = "VARIANT";


	private final RecordDialogComponents components;

	private final JPanel propertiesPanel;

	private final BoundTextField valueField;
	private final TextValueVariantListPanel variantPanel;


	public static ContactNameStructureDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, ContactNameStructureDialog::new);
	}

	public static ContactNameStructureDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, ContactNameStructureDialog::new);
	}


	private ContactNameStructureDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, ContactNameHandler.class);

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]");

		valueField = new BoundTextField(TAG_VALUE);
		variantPanel = new TextValueVariantListPanel(TAG_VARIANT, this, "Variant", model);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.build();

		components.bind(valueField);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// value
		GUIHelper.addLabeledComponent(propertiesPanel, "Name Value*:", valueField);

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
				"Name value cannot be empty.",
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
		GUIHelper.launch(ContactNameStructureDialog::createNew);
	}

}
