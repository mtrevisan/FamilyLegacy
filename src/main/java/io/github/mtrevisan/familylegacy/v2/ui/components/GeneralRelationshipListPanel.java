package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogstodo._RelationshipDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.List;


/**
 * Panel that displays all relationship records belonging to a group.
 * Supports creating, editing and removing arbitrary relationships.
 */
public class GeneralRelationshipListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = 8165048140355496463L;


	private static final String TAG_RELATIONSHIP = "RELATIONSHIP";
	private static final String TAG_OBJECT = "OBJECT";
	private static final String TAG_ROLE = "ROLE";


	static{
		HandlerRegistry.register(new RelationshipHandler());
	}


	private final String path;

	private final RecordTypeHandler<?> relationshipHandler;


	public GeneralRelationshipListPanel(final String path, final Dialog parent, final FLEFModel model){
		super(parent, "Relationships", model);

		this.path = path;

		relationshipHandler = HandlerRegistry.getHandler(RelationshipHandler.TYPE);
	}


	@Override
	protected void initComponents(){
		super.initComponents();

		GUIHelper.installBehavior(list,
			this::editItem,
			this::createNewItem,
			this::removeItem,
			builder -> {
				builder.item("Create New...", this::createNewItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			}
		);
	}

	@Override
	protected String getDisplay(final FLEFRecord relationship){
		if(relationship != null)
			return relationshipHandler.getDisplayText(relationship, model);

		return "--";
	}

	@Override
	protected FLEFRecord showAddDialog(){
		final FLEFRecord[] result = {null};
		final GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parent, model, relationshipHandler, selectedId -> {
				final FLEFRecord relationship = model.getRecordById(selectedId);
				if(relationship != null && !items.contains(relationship))
					result[0] = relationship;
			}
		);
		dialog.setVisible(true);

		return result[0];
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		final _RelationshipDialog dialog = (_RelationshipDialog)relationshipHandler.createNewDialog(parent, model);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getCitationRecord(): null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, "Relationship not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final _RelationshipDialog dialog = (_RelationshipDialog)relationshipHandler.createEditDialog(parent, model, existing);
		dialog.setVisible(true);

		// Return the same record (it was updated in place)
		return existing;
	}

	public void load(final FLEFRecord record){
		final List<FLEFRecord> relationships = FLEFRecordHelper.findChildren(record, TAG_RELATIONSHIP);
		setItems(relationships);
	}

	public void save(final FLEFRecord record){
		FLEFRecordHelper.removeChildren(record, TAG_RELATIONSHIP);

		for(final FLEFRecord relationship : getItems()){
			relationship.setTag(TAG_RELATIONSHIP);
			record.addChild(relationship);
		}
	}

}
