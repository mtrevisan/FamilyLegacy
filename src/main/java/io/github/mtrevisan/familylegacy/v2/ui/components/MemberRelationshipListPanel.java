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
package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.MultiTypeSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogstodo._NoteListEditorDialog;
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


/**
 * Panel that displays only member relationships (group ↔ Individual).
 * Provides extra actions: "Add Existing", "Create New", "Edit Member Individual", "Notes".
 */
public class MemberRelationshipListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = 4913602704327077030L;


	private static final String TAG_RELATIONSHIP = "RELATIONSHIP";
	private static final String TAG_SUBJECT = "SUBJECT";
	private static final String TAG_OBJECT = "OBJECT";
	private static final String TAG_ROLE = "ROLE";
	private static final String TAG_INDIVIDUAL = "INDIVIDUAL";


	static{
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new RelationshipHandler());
	}


	private final String groupId;
	private RelationshipListPanel generalPanel;

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
			if(obj != null)
				display.append(individualHandler.getDisplayText(obj, model));
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
		final MultiTypeSelectionDialog dialog = new MultiTypeSelectionDialog(parent, model,
			IndividualHandler.TYPE,
			(handlerType, selectedRecord) -> result[0] = selectedRecord.getValue()
		);
		dialog.setVisible(true);

		final String individualId = result[0];
		if(individualId == null)
			return null;

		if(groupId == null){
			JOptionPane.showMessageDialog(parent, "Group ID not available.", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		//TODO
//		final RelationshipRecordDialog relDialog = RelationshipRecordDialog(parent, model, null, groupId,
//			individualId);
//		relDialog.setVisible(true);
//
//		final FLEFRecord rel = (relDialog.isSaved()? relDialog.getCitationRecord(): null);
//		if(rel != null && generalPanel != null && !generalPanel.getItems().contains(rel))
//			generalPanel.addItemDirectly(rel);
//		return rel;
		return null;
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

		if(groupId == null){
			JOptionPane.showMessageDialog(parent, "Group ID not available.", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		//TODO
//		final RelationshipRecordDialog relDialog = new RelationshipRecordDialog(parent, model, null, groupId,
//			newIndividualId);
//		relDialog.setVisible(true);
//
//		final FLEFRecord rel = relDialog.isSaved()? relDialog.getCitationRecord(): null;
//		if(rel != null && generalPanel != null && !generalPanel.getItems().contains(rel))
//			generalPanel.addItemDirectly(rel);
//		return rel;
		return null;
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, "Member Relationship not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		//TODO
//		final RelationshipRecordDialog dialog = (RelationshipRecordDialog)relationshipHandler.createEditDialog(parent, model,
//			existing);
//		dialog.setVisible(true);
//		return (dialog.isSaved()? dialog.getCitationRecord(): existing);
		return null;
	}

	private void editMemberIndividual(){
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
}
