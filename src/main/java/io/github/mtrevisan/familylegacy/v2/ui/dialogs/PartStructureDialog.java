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
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.VariantListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PartHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;


/* DONE */
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


	static{
		HandlerRegistry.register(new PartHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final BoundComboBox<String> typeCombo;
	private final BoundTextField valueField;
	private final VariantListPanel variantPanel;


	public static PartStructureDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, PartStructureDialog::new);
	}

	public static PartStructureDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, PartStructureDialog::new);
	}


	private PartStructureDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(PartHandler.TYPE));

		setTitle(record == null? "Add Part": "Edit Part");

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
		valueField = new BoundTextField(TAG_VALUE, 25);
		variantPanel = new VariantListPanel(TAG_VARIANT, this, "Variant", model);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(typeCombo);
		bindingManager.bind(valueField);


		setLayout(new MigLayout("ins 10, fillx", "[right]rel[grow]", "[]5[]10[]"));

		add(new JLabel("Part Type*:"), "align label");
		add(typeCombo, "growx,wrap");

		add(new JLabel("Value*:"), "align label");
		add(valueField, "growx,wrap");

		add(variantPanel, "span 2,growx,wrap");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}


	@Override
	protected void loadData(){
		bindingManager.load(record);

		final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
		typeCombo.setSelectedItem(type != null? type: StringUtils.EMPTY);

		final String value = FLEFRecordHelper.getChildValue(record, TAG_VALUE);
		valueField.setText(value);

		variantPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(valueField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Value cannot be empty.",
				null, null, valueField);

			return false;
		}
		return true;
	}

	@Override
	protected void saveData(){
		record.setTag(TAG_PART);

		bindingManager.save(record);

		variantPanel.save(record);
	}


	public static void main(final String[] args){
		GUIHelper.launch(PartStructureDialog::createNew);
	}

}
