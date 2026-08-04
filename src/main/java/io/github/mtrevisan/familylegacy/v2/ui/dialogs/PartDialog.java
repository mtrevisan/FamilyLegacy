package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.VariantListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PartHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;


/**
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
public class PartDialog extends BaseRecordDialog{

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


	public static PartDialog createNew(final Dialog parent, final FLEFModel model){
		return new PartDialog(parent, model, null);
	}

	public static PartDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new PartDialog(parent, model, record);
	}


	private PartDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
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
		valueField = new BoundTextField(TAG_VALUE, 25);
		variantPanel = new VariantListPanel(TAG_VARIANT, this, model);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	private void initComponents(){
		bindingManager.bind(typeCombo);
		bindingManager.bind(valueField);


		setLayout(new MigLayout("ins 10, fillx", "[right]rel[grow]", "[]5[]5[]"));

		add(new JLabel("Part Type:"), "align label");
		add(typeCombo, "growx,wrap");

		add(new JLabel("Value:"), "align label");
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
		typeCombo.setSelectedItem(type != null ? type : StringUtils.EMPTY);

		final String val = FLEFRecordHelper.getChildValue(record, TAG_VALUE);
		valueField.setText(StringUtils.defaultString(val));

		variantPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(StringUtils.isBlank(valueField.getText())){
			GUIHelper.showValidationErrorAndFocus(this, "Value cannot be empty.",
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

}
