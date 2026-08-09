package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


/**
 * Panel for editing the search_scope structure:
 * <pre>
 * search_scope: struct {
 *   type: enum { entire_source, index_only, partial_source, selected_entries }
 *   detail?: Text
 * }
 * </pre>
 */
public class SearchScopePanel extends JPanel{

	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_DETAIL = "DETAIL";

	private final JComboBox<String> typeCombo;
	private final JTextField detailField;

	public SearchScopePanel(){
		setLayout(new MigLayout("ins 0,fillx", "[shrink 0][grow]"));

		typeCombo = new JComboBox<>(new String[]{
			"",
			"entire_source",
			"index_only",
			"partial_source",
			"selected_entries"
		});
		typeCombo.setToolTipText("Type of search scope");

		detailField = new JTextField(20);
		detailField.setToolTipText("Optional detail about the search scope");

		add(new JLabel("Type:"), "align label");
		add(typeCombo, "growx,wrap");
		add(new JLabel("Detail:"), "align label");
		add(detailField, "growx");
	}

	public void load(FLEFRecord record){
		String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
		typeCombo.setSelectedItem(StringUtils.defaultString(type));
		String detail = FLEFRecordHelper.getChildValue(record, TAG_DETAIL);
		detailField.setText(StringUtils.defaultString(detail));
	}

	public void save(FLEFRecord record){
		FLEFRecordHelper.removeChildren(record, TAG_TYPE);
		FLEFRecordHelper.removeChildren(record, TAG_DETAIL);

		String type = (String)typeCombo.getSelectedItem();
		if(StringUtils.isNotEmpty(type)){
			FLEFRecordHelper.updateChildValue(record, TAG_TYPE, type);
		}
		String detail = detailField.getText().trim();
		if(StringUtils.isNotEmpty(detail)){
			FLEFRecordHelper.updateChildValue(record, TAG_DETAIL, detail);
		}
	}

	public void clear(){
		typeCombo.setSelectedIndex(0);
		detailField.setText("");
	}

	public boolean hasData(){
		return StringUtils.isNotEmpty((String)typeCombo.getSelectedItem())
			|| StringUtils.isNotEmpty(detailField.getText().trim());
	}

	public boolean validateData(){
		return true;
	}

}
