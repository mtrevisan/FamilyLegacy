package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogstodo._GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.Dialog;
import java.io.Serial;


/**
 * Component for selecting a target with XrefOrVoid support.
 * Used in ResearchLogRecord for the 'target' field.
 */
public class ResearchLogTargetField extends JPanel{

	@Serial
	private static final long serialVersionUID = -6358544279103513588L;

	private static final String VOID_MARKER = "VOID";

	private final Dialog parent;
	private final FLEFModel model;
	private final RecordTypeHandler<?> handler;

	private final JTextField displayField;
	private final JButton selectButton;
	private final JComboBox<String> typeCombo;

	private String targetType; // "RECORD" or "VOID"
	private FLEFRecord targetRecord;

	public ResearchLogTargetField(Dialog parent, FLEFModel model, RecordTypeHandler<?> handler){
		super(new MigLayout("ins 0,fillx", "[shrink 0][grow][shrink 0]"));
		this.parent = parent;
		this.model = model;
		this.handler = handler;

		typeCombo = new JComboBox<>(new String[]{"Record", "VOID"});
		displayField = new JTextField(20);
		displayField.setEditable(false);
		selectButton = new JButton("...");


		initComponents();
	}

	private void initComponents(){
		typeCombo.addActionListener(e -> updateState());

		selectButton.addActionListener(e -> selectRecord());

		// Install popup behavior on the display field
		GUIHelper.installBehavior(displayField,
			this::edit,
			null,
			null,
			builder -> {
				builder.item("Set Target...", this::selectRecord);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::edit);
				builder.selectionSensitiveItem("Clear", this::clear2);
			});

		add(typeCombo, "width 80!");
		add(displayField, "growx");
		add(selectButton, "width 30!");

		updateState();
		updateDisplay();
	}

	private void updateState(){
		boolean isVoid = "VOID".equals(typeCombo.getSelectedItem());
		selectButton.setEnabled(!isVoid);
		if(isVoid){
			displayField.setText("[VOID]");
			targetRecord = null;
		}
		else if(targetRecord == null){
			displayField.setText("(not set)");
		}
		else{
			displayField.setText(handler.getDisplayText(targetRecord, model));
		}
		revalidate();
		repaint();
	}

	private void selectRecord(){
		if("VOID".equals(typeCombo.getSelectedItem())){
			return;
		}
		_GenericSelectionDialog<?> dialog = new _GenericSelectionDialog<>(
			parent, model, handler, selectedRecord -> {
			if(selectedRecord != null){
				targetRecord = selectedRecord;
				updateDisplay();
			}
		});
		dialog.setVisible(true);
	}

	private void edit(){
		if(targetRecord == null){
			selectRecord();
			return;
		}
		JDialog editDialog = handler.createEditDialog(parent, model, targetRecord);
		editDialog.setVisible(true);
		updateDisplay();
	}

	private void clear2(){
		targetRecord = null;
		displayField.setText("(not set)");
		displayField.setForeground(UIManager.getColor("Label.disabledForeground"));
		targetType = null;
	}

	private void updateDisplay(){
		if(targetRecord != null){
			displayField.setText(handler.getDisplayText(targetRecord, model));
			displayField.setForeground(UIManager.getColor("TextField.foreground"));
			targetType = "RECORD";
		}
		else if("VOID".equals(typeCombo.getSelectedItem())){
			displayField.setText("[VOID]");
			displayField.setForeground(UIManager.getColor("Label.disabledForeground"));
			targetType = "VOID";
		}
		else{
			displayField.setText("(not set)");
			displayField.setForeground(UIManager.getColor("Label.disabledForeground"));
			targetType = null;
		}
	}

	public void setTarget(String type, FLEFRecord record){
		this.targetType = type;
		this.targetRecord = record;
		if("VOID".equals(type)){
			typeCombo.setSelectedItem("VOID");
		}
		else if(record != null){
			typeCombo.setSelectedItem("Record");
		}
		updateDisplay();
	}

	public void clear(){
		setTarget(null, null);
	}

	public boolean hasData(){
		return targetRecord != null || "VOID".equals(targetType);
	}

	public String getTargetType(){
		return targetType;
	}

	public FLEFRecord getTargetRecord(){
		return targetRecord;
	}

	public String getTargetValue(){
		if("VOID".equals(targetType)){
			return VOID_MARKER;
		}
		if(targetRecord != null){
			return XRefHelper.formatXRef(targetRecord.getId());
		}
		return null;
	}

	public void load(FLEFRecord record, String path){
		clear();
		if(record == null) return;
		String value = FLEFRecordHelper.getChildValue(record, path);
		if(StringUtils.isEmpty(value)) return;

		if(VOID_MARKER.equals(value)){
			setTarget("VOID", null);
		}
		else{
			FLEFRecord rec = model.getRecordById(value);
			if(rec != null){
				setTarget("RECORD", rec);
			}
		}
	}

	public void save(FLEFRecord record, String path){
		FLEFRecordHelper.removeChildren(record, path);
		if(hasData()){
			String value = getTargetValue();
			if(value != null){
				FLEFRecordHelper.updateChildValue(record, path, value);
			}
		}
	}

}
