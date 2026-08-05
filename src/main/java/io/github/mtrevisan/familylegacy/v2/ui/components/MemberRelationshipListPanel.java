package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogstodo._NoteListEditorDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogstodo._RelationshipDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;


/**
 * Panel that displays only member relationships (group ↔ Individual).
 * Provides extra actions: "Add Existing", "Create New", "Edit Member Individual", "Notes".
 */
public class MemberRelationshipListPanel extends AbstractListPanel2{

	@Serial
	private static final long serialVersionUID = 4913602704327077030L;


	private static final String TAG_RELATIONSHIP = "RELATIONSHIP";
	private static final String TAG_SUBJECT = "SUBJECT";
	private static final String TAG_OBJECT = "OBJECT";
	private static final String TAG_ROLE = "ROLE";
	private static final String TAG_INDIVIDUAL = "INDIVIDUAL";


	static{
		HandlerRegistry.register(new RelationshipHandler());
	}


	private final Supplier<String> groupIdSupplier;
	private GeneralRelationshipListPanel generalPanel;

	private final RecordTypeHandler<?> individualHandler;
	private final RecordTypeHandler<?> relationshipHandler;


	public MemberRelationshipListPanel(final Dialog parent, final FLEFModel model,
			final Supplier<String> groupIdSupplier){
		super(parent, "Members", model);

		this.groupIdSupplier = groupIdSupplier;

		this.individualHandler = HandlerRegistry.getHandler(IndividualHandler.TYPE);
		this.relationshipHandler = HandlerRegistry.getHandler(RelationshipHandler.TYPE);
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
				builder.item("Add Existing...", this::addItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editItem);
				builder.selectionSensitiveItem("Edit Member Individual...", this::editMemberIndividual);
				builder.selectionSensitiveItem("Notes...", this::showNotes);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			}
		);
	}

	@Override
	protected String getDisplay(final FLEFRecord relationship){
		if(relationship == null)
			return "--";

		final String objectId = FLEFRecordHelper.getChildValue(relationship, TAG_OBJECT);
		final String role = FLEFRecordHelper.getChildValue(relationship, TAG_ROLE);
		final StringBuilder display = new StringBuilder();
		if(objectId != null){
			final FLEFRecord obj = model.getRecordById(objectId);
			if(obj != null){
				final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(IndividualHandler.TYPE);
				display.append(handler.getDisplayText(obj, model));
			}
			else
				display.append(objectId);
		}
		else
			display.append("?");
		if(role != null && !role.isEmpty())
			display.append(" [").append(role).append("]");
		return display.toString();
	}

	private boolean isMemberRelationship(final FLEFRecord relationship){
		final String groupId = groupIdSupplier.get();
		if(groupId == null)
			return false;

		final String subjectId = FLEFRecordHelper.getChildValue(relationship, TAG_SUBJECT);
		final String objectId = FLEFRecordHelper.getChildValue(relationship, TAG_OBJECT);
		final boolean groupIsSubject = groupId.equals(subjectId);
		final boolean groupIsObject = groupId.equals(objectId);
		if(!groupIsSubject && !groupIsObject)
			return false;

		final String otherId = groupIsSubject? objectId: subjectId;
		if(otherId == null)
			return false;

		final FLEFRecord other = model.getRecordById(otherId);
		return (other != null && TAG_INDIVIDUAL.equals(other.getTag()));
	}

	//FIXME
//	@Override
//	public void setItems(final List<FLEFRecord> items){
//		final List<FLEFRecord> members = new ArrayList<>();
//		for(final FLEFRecord rel : items)
//			if(isMemberRelationship(rel))
//				members.add(rel);
//		super.setItems(members);
//	}

	@Override
	protected FLEFRecord showAddDialog(){
		final String[] result = {null};
		final GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parent, model, individualHandler, id -> result[0] = id);
		dialog.setVisible(true);

		final String individualId = result[0];
		if(individualId == null)
			return null;

		final String groupId = groupIdSupplier.get();
		if(groupId == null){
			JOptionPane.showMessageDialog(parent, "Group ID not available.", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final _RelationshipDialog relDialog = new _RelationshipDialog(parent, model, null, groupId,
			individualId);
		relDialog.setVisible(true);

		final FLEFRecord rel = (relDialog.isSaved()? relDialog.getCitationRecord(): null);
		if(rel != null && generalPanel != null && !generalPanel.getItems().contains(rel))
			generalPanel.addItemDirectly(rel);
		return rel;
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		final Set<String> before = new HashSet<>();
		for(final FLEFRecord rec : model.getRecordsByType(TAG_INDIVIDUAL)){
			final String id = rec.getId();
			if(id != null)
				before.add(id);
		}

		final JDialog createDialog = individualHandler.createNewDialog(parent, model);
		createDialog.setVisible(true);

		String newIndividualId = null;
		for(final FLEFRecord rec : model.getRecordsByType(TAG_INDIVIDUAL)){
			final String id = rec.getId();
			if(id != null && !before.contains(id)){
				newIndividualId = id;

				break;
			}
		}
		if(newIndividualId == null)
			return null;

		final String groupId = groupIdSupplier.get();
		if(groupId == null){
			JOptionPane.showMessageDialog(parent, "Group ID not available.", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final _RelationshipDialog relDialog = new _RelationshipDialog(parent, model, null, groupId,
			newIndividualId);
		relDialog.setVisible(true);

		final FLEFRecord rel = relDialog.isSaved()? relDialog.getCitationRecord(): null;
		if(rel != null && generalPanel != null && !generalPanel.getItems().contains(rel))
			generalPanel.addItemDirectly(rel);
		return rel;
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, "Member Relationship not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final _RelationshipDialog dialog = (_RelationshipDialog)relationshipHandler.createEditDialog(parent, model,
			existing);
		dialog.setVisible(true);
		return (dialog.isSaved()? dialog.getCitationRecord(): existing);
	}

	private void editMemberIndividual(){
		final int idx = list.getSelectedIndex();
		if(idx == -1)
			return;

		final FLEFRecord relationship = items.get(idx);
		if(relationship == null)
			return;

		final String groupId = groupIdSupplier.get();
		if(groupId == null)
			return;

		final String subjectId = FLEFRecordHelper.getChildValue(relationship, TAG_SUBJECT);
		final String objectId = FLEFRecordHelper.getChildValue(relationship, TAG_OBJECT);
		final String otherId = groupId.equals(subjectId)? objectId: subjectId;
		if(otherId == null)
			return;

		final FLEFRecord individual = model.getRecordById(otherId);
		if(individual == null){
			JOptionPane.showMessageDialog(parent, "Individual not found: " + otherId, "Error",
				JOptionPane.ERROR_MESSAGE);

			return;
		}

		final JDialog editDialog = individualHandler.createEditDialog(parent, model, individual);
		editDialog.setVisible(true);

		final int pos = list.getSelectedIndex();
		if(pos != -1)
			listModel.set(pos, getDisplay(relationship));
	}

	private void showNotes(){
		final int idx = list.getSelectedIndex();
		if(idx == -1)
			return;

		final FLEFRecord relationship = items.get(idx);
		if(relationship == null)
			return;

		final _NoteListEditorDialog dialog = new _NoteListEditorDialog(parent, model, relationship);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			final int pos = list.getSelectedIndex();
			if(pos != -1)
				listModel.set(pos, getDisplay(relationship));
		}
	}

	public void load(final FLEFRecord record){
		final List<FLEFRecord> relationships = FLEFRecordHelper.findChildren(record, TAG_RELATIONSHIP);
		setItems(relationships);
	}

	public void save(final FLEFRecord groupRecord){
		FLEFRecordHelper.removeChildren(groupRecord, TAG_RELATIONSHIP);

		for(final FLEFRecord rel : getItems()){
			rel.setTag(TAG_RELATIONSHIP);
			groupRecord.addChild(rel);
		}
	}
}
