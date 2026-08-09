package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogstodo._GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/**
 * Panel for managing a list of ConclusionTarget entries (resolves*).
 * Each target is stored as a child record with tag = target type (e.g., "EVENT")
 * and value = Xref (formatted with @).
 */
public class ConclusionTargetListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = -2686169799110029329L;

	// Allowed target types (tags)
	private static final List<String> TARGET_TYPES = List.of(
		EventHandler.TYPE,
		RelationshipHandler.TYPE,
		IndividualAttributeHandler.TYPE,
		GroupAttributeHandler.TYPE,
		//TODO
//		IdentityHypothesisHandler.TYPE
		null
	);

	private final Dialog parentDialog;

	public ConclusionTargetListPanel(Dialog parentDialog, FLEFModel model){
		super(parentDialog, "Resolves", model);
		this.parentDialog = parentDialog;
	}

	//TODO
//	@Override
	protected void buildMenu(GUIHelper.MenuBuilder builder){
		builder.item("Add Target...", this::addItem);
		builder.separator();
		builder.selectionSensitiveItem("Edit...", this::editItem);
		builder.selectionSensitiveItem("Remove", this::removeItem);
	}

	@Override
	public String getDisplay(FLEFRecord target){
		if(target == null) return "--";
		String type = target.getTag();
		String ref = target.getValue();
		if(StringUtils.isEmpty(ref)) return "[empty]";
		FLEFRecord rec = model.getRecordById(ref);
		if(rec == null) return ref;
		RecordTypeHandler<?> handler = HandlerRegistry.getHandler(type);
		if(handler == null) return ref;
		return handler.getDisplayText(rec, model);
	}

	@Override
	protected FLEFRecord showAddDialog(){
		// Let user choose target type and then select a record
		TargetSelectionDialog dialog = new TargetSelectionDialog(parentDialog, model);
		dialog.setVisible(true);
		if(!dialog.isConfirmed()) return null;
		String type = dialog.getSelectedType();
		String id = dialog.getSelectedId();
		if(StringUtils.isEmpty(type) || StringUtils.isEmpty(id)) return null;
		FLEFRecord target = FLEFRecord.createChildWithValue(type, XRefHelper.formatXRef(id));
		return target;
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		// Same as add for now; we don't create new targets separately
		return showAddDialog();
	}

	@Override
	protected FLEFRecord showEditDialog(FLEFRecord existing){
		if(existing == null) return null;
		// For simplicity, we can allow editing by re-selecting
		// But we could also open the target record's edit dialog
		String type = existing.getTag();
		String ref = existing.getValue();
		if(StringUtils.isEmpty(ref)) return null;
		FLEFRecord rec = model.getRecordById(ref);
		if(rec == null) return null;
		RecordTypeHandler<?> handler = HandlerRegistry.getHandler(type);
		if(handler == null) return null;
		JDialog editDialog = handler.createEditDialog(parentDialog, model, rec);
		editDialog.setVisible(true);
		// The record may have changed; refresh display
		return existing;
	}

	public void load(FLEFRecord record){
		List<FLEFRecord> targets = new ArrayList<>();
		for(FLEFRecord child : record.getChildren()){
			if(TARGET_TYPES.contains(child.getTag())){
				targets.add(child);
			}
		}
		setItems(targets);
	}

	public void save(FLEFRecord record){
		// Remove existing target children
		for(String type : TARGET_TYPES){
			FLEFRecordHelper.removeChildren(record, type);
		}
		// Add each target as a child
		for(FLEFRecord target : getItems()){
			record.addChild(target);
		}
	}

	/**
	 * Returns the list of Xref IDs of the targets.
	 */
	public List<String> getTargetIds(){
		List<String> ids = new ArrayList<>();
		for(FLEFRecord target : items){
			String ref = target.getValue();
			if(StringUtils.isNotEmpty(ref)){
				ids.add(XRefHelper.extractXRef(ref));
			}
		}
		return ids;
	}

	// Inner dialog for selecting target type and record
	private static class TargetSelectionDialog extends JDialog{
		private final FLEFModel model;
		private final JComboBox<String> typeCombo;
		private boolean confirmed = false;
		private String selectedType;
		private String selectedId;

		public TargetSelectionDialog(Dialog parent, FLEFModel model){
			super(parent, "Add Conclusion Target", ModalityType.APPLICATION_MODAL);
			this.model = model;
			typeCombo = new JComboBox<>(TARGET_TYPES.toArray(new String[0]));


			initComponents();

			pack();

			setLocationRelativeTo(parent);
		}

		private void initComponents(){
			setLayout(new MigLayout("ins 10,fillx", "[grow]"));
			add(new JLabel("Target Type:"), "wrap");
			add(typeCombo, "growx,wrap");

			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			JButton selectBtn = new JButton("Select...");
			JButton cancelBtn = new JButton("Cancel");
			buttonPanel.add(selectBtn);
			buttonPanel.add(cancelBtn);
			add(buttonPanel, "growx");

			selectBtn.addActionListener(e -> selectTarget());
			cancelBtn.addActionListener(e -> dispose());
		}

		private void selectTarget(){
			String type = (String)typeCombo.getSelectedItem();
			if(StringUtils.isEmpty(type)) return;
			RecordTypeHandler<?> handler = HandlerRegistry.getHandler(type);
			if(handler == null) return;
			_GenericSelectionDialog<?> dialog = new _GenericSelectionDialog<>(
				this, model, handler, selectedRecord -> {
				if(selectedRecord != null){
					selectedType = type;
					selectedId = selectedRecord.getId();
					confirmed = true;
					dispose();
				}
			});
			dialog.setVisible(true);
		}

		public boolean isConfirmed(){
			return confirmed;
		}

		public String getSelectedType(){
			return selectedType;
		}

		public String getSelectedId(){
			return selectedId;
		}
	}

}
