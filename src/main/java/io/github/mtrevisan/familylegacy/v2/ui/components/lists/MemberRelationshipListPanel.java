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
package io.github.mtrevisan.familylegacy.v2.ui.components.lists;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.MultiTypeSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.RelationshipRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogstodo._NoteListEditorDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
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


/* ONGOING */
/**
 * Panel for managing member relationships (membership) of a group.
 * <p>
 * This panel handles the creation and management of relationships between
 * a group (identified by {@code groupId}) and individuals.
 * <p>
 * <strong>Note:</strong> This panel works in conjunction with a {@link RelationshipListPanel}
 * to keep the general relationship list synchronized. When a member relationship
 * is added or removed, it is automatically reflected in the general panel if set.
 */
public class MemberRelationshipListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = 4913602704327077030L;


	private static final String TAG_RELATIONSHIP = "RELATIONSHIP";
	private static final String TAG_SUBJECT = "SUBJECT";
	private static final String TAG_OBJECT = "OBJECT";
	private static final String TAG_INDIVIDUAL = "INDIVIDUAL";


	static{
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new RelationshipHandler());
	}


	private final String groupId;

	private final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler(IndividualHandler.TYPE);
	private final RecordTypeHandler<?> relationshipHandler = HandlerRegistry.getHandler(RelationshipHandler.TYPE);


	public MemberRelationshipListPanel(final Dialog parent, final FLEFModel model, final String groupId){
		super(parent, "Members", model);

		this.groupId = groupId;
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
				builder.selectionSensitiveItem("Edit Relationship...", this::editItem);
				builder.selectionSensitiveItem("Edit Member...", this::editMemberItem);
				builder.selectionSensitiveItem("Notes...", this::showNotes);
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

	private boolean isMemberRelationship(final FLEFRecord relationship){
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

	/**
	 * Creates a new individual and a relationship to the group.
	 *
	 * @return the new relationship record, or {@code null} if cancelled
	 */
	@Override
	protected FLEFRecord showCreateNewDialog(){
		if(groupId == null){
			JOptionPane.showMessageDialog(parent, "Group ID not available.", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		// 1. Track existing individuals to identify the newly created one
		final Set<String> before = new HashSet<>();
		for(final FLEFRecord rec : model.getRecordsByType(TAG_INDIVIDUAL)){
			final String id = rec.getId();
			if(id != null)
				before.add(id);
		}

		// 2. Create a new individual
		final JDialog createDialog = individualHandler.createNewDialog(parent, model);
		createDialog.setVisible(true);

		// 3. Find the newly created individual ID
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

		// 4. Create a relationship between the group and the new individual
		final RelationshipRecordDialog relDialog = (RelationshipRecordDialog)relationshipHandler.createNewDialog(parent, model);
		relDialog.setSubject(groupId, GroupHandler.TYPE);
		//TODO group vs individual
//		relDialog.setObject(newIndividualId, IndividualHandler.TYPE);
		relDialog.setVisible(true);

		if(relDialog.isSaved())
			return relDialog.getRecord();

		// Rollback: remove the newly created individual
		model.removeRecord(newIndividualId);
		return null;
	}

	/**
	 * Adds an existing individual as a member of the group.
	 *
	 * @return the new relationship record, or {@code null} if cancelled
	 */
	@Override
	protected FLEFRecord showAddDialog(){
		if(groupId == null){
			JOptionPane.showMessageDialog(parent, "Group ID not available.", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		// 1. Select an existing individual
		final String[] result = {null};
		final MultiTypeSelectionDialog dialog = new MultiTypeSelectionDialog(parent, model,
			IndividualHandler.TYPE,
			(handlerType, selectedRecord) -> result[0] = selectedRecord.getValue()
		);
		dialog.setVisible(true);

		final String individualId = result[0];
		if(individualId == null)
			return null;

		// 2. Create a relationship between the group and the selected individual
		final RelationshipRecordDialog relDialog = (RelationshipRecordDialog)relationshipHandler.createNewDialog(parent, model);
		relDialog.setSubject(groupId, GroupHandler.TYPE);
		//TODO group vs individual
//		relDialog.setObject(newIndividualId, IndividualHandler.TYPE);
		relDialog.setVisible(true);

		return (relDialog.isSaved()? relDialog.getRecord(): null);
	}

	/**
	 * Edits an existing relationship.
	 *
	 * @param existing the existing relationship to edit
	 * @return the updated relationship, or {@code null} if cancelled
	 */
	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, relationshipHandler.getLabel() + " not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final BaseRecordDialog dialog = relationshipHandler.createEditDialog(parent, model, existing);
		dialog.setVisible(true);

		// Return the same record (it was updated in place)
		return existing;
	}

	/**
	 * Edits the individual that is the target of the selected relationship.
	 * The individual is the one that is not the group itself.
	 */
	private void editMemberItem(){
		final int idx = list.getSelectedIndex();
		if(idx == -1)
			return;

		final FLEFRecord relationship = items.get(idx);
		if(relationship == null)
			return;

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

		// Update the display in the list
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
			// Update the display in the list
			final int pos = list.getSelectedIndex();
			if(pos != -1)
				listModel.set(pos, getDisplay(relationship));
		}
	}

	public void load(final FLEFRecord record){
		clear();

		if(record == null)
			return;

		final List<FLEFRecord> relationships = FLEFRecordHelper.findChildren(record, TAG_RELATIONSHIP);
		setItems(relationships);
	}

	public void save(final FLEFRecord record){
		FLEFRecordHelper.removeChildren(record, TAG_RELATIONSHIP);

		record.addChildrenWithTag(TAG_RELATIONSHIP, getItems());
	}

	public boolean hasData(){
		return !isEmpty();
	}

}
