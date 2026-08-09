package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/**
 * Panel for managing a list of association structures in ResearchStatusRecord.
 * Each association has a target (Xref or VOID) and an optional name.
 * <p>
 * Structure:
 * <pre>
 * association: struct {
 *   target: XrefOrVoid&lt;LocalID&gt;
 *   name?: Text
 * }
 * </pre>
 */
public class ResearchAssociationListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = 5201318669653810012L;

	private static final String TAG_TARGET = "TARGET";
	private static final String TAG_NAME = "NAME";
	private static final String VOID_MARKER = "VOID";

	private final Dialog parentDialog;

	public ResearchAssociationListPanel(Dialog parentDialog, FLEFModel model){
		super(parentDialog, "Associations", model);
		this.parentDialog = parentDialog;
	}

//	@Override
//	protected void buildMenu(GUIHelper.MenuBuilder builder){
//		builder.item("Add Association...", this::createNewItem);
//		builder.separator();
//		builder.selectionSensitiveItem("Edit...", this::editItem);
//		builder.selectionSensitiveItem("Remove", this::removeItem);
//	}

	@Override
	protected String getDisplay(FLEFRecord association){
		if(association == null) return "--";
		String target = FLEFRecordHelper.getChildValue(association, TAG_TARGET);
		String name = FLEFRecordHelper.getChildValue(association, TAG_NAME);

		StringBuilder sb = new StringBuilder();
		if(VOID_MARKER.equals(target)){
			sb.append("[VOID]");
			if(StringUtils.isNotEmpty(name)){
				sb.append(" ").append(name);
			}
		}
		else if(StringUtils.isNotEmpty(target)){
			FLEFRecord rec = model.getRecordById(target);
			if(rec != null){
				sb.append(rec.getTag()).append(": ").append(target);
			}
			else{
				sb.append(target);
			}
		}
		else{
			sb.append("(empty)");
		}
		return sb.toString();
	}

	@Override
	protected FLEFRecord showAddDialog(){
		// For associations, "Add" is the same as "Create New"
		return showCreateNewDialog();
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		return showAssociationDialog(null);
	}

	@Override
	protected FLEFRecord showEditDialog(FLEFRecord existing){
		if(existing == null) return null;
		return showAssociationDialog(existing);
	}

	/**
	 * Shows a dialog to create or edit an association entry.
	 */
	private FLEFRecord showAssociationDialog(FLEFRecord initial){
		JDialog dialog = new JDialog(parentDialog, initial == null? "Add Association": "Edit Association", true);
		dialog.setLayout(new MigLayout("ins 10,fillx", "[right]rel[grow]", "[]5[]5[]"));

		// Target type (Record or VOID)
		JComboBox<String> targetTypeCombo = new JComboBox<>(new String[]{"Record", "VOID"});
		targetTypeCombo.setToolTipText("Select 'Record' to reference an existing record, or 'VOID' for external reference");

		// Target selection field
		JTextField targetField = new JTextField(20);
		targetField.setEditable(false);
		JButton selectButton = new JButton("...");
		selectButton.setEnabled(true);

		// Name field
		JTextField nameField = new JTextField(20);
		nameField.setToolTipText("Optional name for VOID references");

		// If editing, load existing data
		String existingTarget = null;
		String existingName = null;
		if(initial != null){
			existingTarget = FLEFRecordHelper.getChildValue(initial, TAG_TARGET);
			existingName = FLEFRecordHelper.getChildValue(initial, TAG_NAME);
			if(StringUtils.isNotEmpty(existingTarget)){
				if(VOID_MARKER.equals(existingTarget)){
					targetTypeCombo.setSelectedItem("VOID");
					targetField.setText("[VOID]");
					selectButton.setEnabled(false);
				}
				else{
					targetTypeCombo.setSelectedItem("Record");
					FLEFRecord rec = model.getRecordById(existingTarget);
					targetField.setText(rec != null? rec.getTag() + ": " + existingTarget: existingTarget);
					selectButton.setEnabled(true);
				}
			}
			if(StringUtils.isNotEmpty(existingName)){
				nameField.setText(existingName);
			}
		}

		// Listeners
		targetTypeCombo.addActionListener(e -> {
			boolean isVoid = "VOID".equals(targetTypeCombo.getSelectedItem());
			selectButton.setEnabled(!isVoid);
			if(isVoid){
				targetField.setText("[VOID]");
			}
			else{
				targetField.setText("");
			}
		});

		selectButton.addActionListener(e -> {
			// Open a generic selection dialog for any record type
			// For simplicity, we'll use a MultiTypeSelectionDialog with all record types
			// Or we could restrict to a specific set. For now, we'll use a simple approach.
//			GenericSelectionDialog<?> selDialog = new GenericSelectionDialog<>(
//				dialog, model,
//				// We need a handler; for simplicity we'll use a generic approach
//				// that shows all records. This could be improved.
//				new RecordTypeHandler<JDialog>(){
//					@Override
//					public String getType(){
//						return null;
//					}
//
//					@Override
//					public String getIdPrefix(){
//						return "";
//					}
//
//					@Override
//					public String getLabel(){
//						return "Target Record";
//					}
//
//					@Override
//					public String getDisplayText(FLEFRecord rec, FLEFModel m){
//						return rec.getTag() + ": " + rec.getId();
//					}
//
//					@Override
//					public JDialog createNewDialog(Dialog parent, FLEFModel m){
//						return null;
//					}
//
//					@Override
//					public JDialog createEditDialog(Dialog parent, FLEFModel m, FLEFRecord rec){
//						return null;
//					}
//				},
//				selectedRecord -> {
//					if(selectedRecord != null){
//						targetField.setText(selectedRecord.getTag() + ": " + selectedRecord.getId());
//						targetField.putClientProperty("selectedId", selectedRecord.getId());
//					}
//				},
//				() -> model.getRecords() // Load all records
//			);
//			selDialog.setVisible(true);
		});

		dialog.add(new JLabel("Target Type:"), "align label");
		dialog.add(targetTypeCombo, "growx,wrap");
		dialog.add(new JLabel("Target:"), "align label");
		JPanel targetPanel = new JPanel(new MigLayout("ins 0,fillx", "[grow][shrink 0]"));
		targetPanel.add(targetField, "growx");
		targetPanel.add(selectButton, "width 30!");
		dialog.add(targetPanel, "growx,wrap");
		dialog.add(new JLabel("Name (optional):"), "align label");
		dialog.add(nameField, "growx,wrap");

		// Buttons
		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		dialog.add(btnPanel, "span 2,growx");

		final FLEFRecord[] result = {null};
		okBtn.addActionListener(e -> {
			String targetValue;
			boolean isVoid = "VOID".equals(targetTypeCombo.getSelectedItem());
			if(isVoid){
				targetValue = VOID_MARKER;
			}
			else{
				String selectedId = (String)targetField.getClientProperty("selectedId");
				if(StringUtils.isEmpty(selectedId)){
					JOptionPane.showMessageDialog(dialog,
						"Please select a target record.",
						"Validation Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				targetValue = XRefHelper.formatXRef(selectedId);
			}

			String name = nameField.getText().trim();

			FLEFRecord association = (initial != null)? initial: FLEFRecord.createEmpty();
			association.setTag("ASSOCIATION");
			FLEFRecordHelper.updateChildValue(association, TAG_TARGET, targetValue);
			if(StringUtils.isNotEmpty(name)){
				FLEFRecordHelper.updateChildValue(association, TAG_NAME, name);
			}
			else{
				FLEFRecordHelper.removeChildren(association, TAG_NAME);
			}

			result[0] = association;
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(parentDialog);
		dialog.setVisible(true);

		return result[0];
	}

	/**
	 * Loads all ASSOCIATION children from the given record.
	 */
	public void load(FLEFRecord record){
		List<FLEFRecord> associations = new ArrayList<>();
		for(FLEFRecord child : record.getChildren()){
			if("ASSOCIATION".equals(child.getTag())){
				associations.add(child);
			}
		}
		setItems(associations);
	}

	/**
	 * Saves the current list of associations as children of the given record.
	 */
	public void save(FLEFRecord record){
		FLEFRecordHelper.removeChildren(record, "ASSOCIATION");
		for(FLEFRecord association : getItems()){
			association.setTag("ASSOCIATION");
			record.addChild(association);
		}
	}

	/**
	 * Returns whether there is at least one association.
	 */
	public boolean hasData(){
		return !items.isEmpty();
	}

}
