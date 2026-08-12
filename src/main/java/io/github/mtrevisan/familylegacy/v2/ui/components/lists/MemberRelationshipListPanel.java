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
import java.util.stream.Collectors;


/* ONGOING */
/**
 * Panel for managing membership relationships (group_member) of a genealogical entity.
 * <p>
 * This panel handles the creation and management of relationships between
 * an actor (either an Individual or a Group) and the opposite entity type.
 * <ul>
 *   <li>If actor is a {@code Group}, the members are {@code Individual}s.</li>
 *   <li>If actor is an {@code Individual}, the members are {@code Group}s.</li>
 * </ul>
 * The actor is identified by its ID and its handler type passed in the constructor.
 */
public class MemberRelationshipListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = 4913602704327077030L;


	private static final String TAG_RELATIONSHIP = "RELATIONSHIP";
	private static final String TAG_SUBJECT = "SUBJECT";
	private static final String TAG_OBJECT = "OBJECT";


	static{
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new RelationshipHandler());
	}


	private final String actorId;
	private final String actorHandlerType;

	private final RecordTypeHandler<?> relationshipHandler;

	// The type of the member (the opposite of the actor)
	private final String memberHandlerType;
	private final RecordTypeHandler<?> memberHandler;


	/**
	 * Constructs a MemberRelationshipListPanel.
	 *
	 * @param parent	the parent dialog
	 * @param model	the FLEF model
	 * @param actorId	the ID of the actor (individual or group)
	 * @param actorHandlerType	the handler type of the actor
	 */
	public MemberRelationshipListPanel(final Dialog parent, final String panelTitle, final FLEFModel model,
			final String actorId, final String actorHandlerType){
		super(parent, panelTitle, model);

		this.actorId = actorId;
		this.actorHandlerType = actorHandlerType;
		this.relationshipHandler = HandlerRegistry.getHandler(RelationshipHandler.TYPE);

		// Determine the member type: if actor is Individual, members are Group; if actor is Group, members are Individual.
		if(IndividualHandler.TYPE.equals(actorHandlerType)){
			this.memberHandlerType = GroupHandler.TYPE;
			this.memberHandler = HandlerRegistry.getHandler(GroupHandler.TYPE);
		}
		else if(GroupHandler.TYPE.equals(actorHandlerType)){
			this.memberHandlerType = IndividualHandler.TYPE;
			this.memberHandler = HandlerRegistry.getHandler(IndividualHandler.TYPE);
		}
		else
			throw new IllegalArgumentException("Unsupported actor handler type: " + actorHandlerType);
	}

	@Override
	protected void initComponents(){
		super.initComponents();

		GUIHelper.installBehavior(list,
			this::editItem,
			this::createNewItem,
			this::removeItem,
			builder -> {
				builder.item("Create New…", this::createNewItem);
				builder.item("Add Existing…", this::addItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit Relationship…", this::editItem);
				builder.selectionSensitiveItem("Edit Member…", this::editMemberItem);
				builder.selectionSensitiveItem("Notes…", this::showNotes);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			}
		);
	}

	@Override
	protected String getDisplayText(final FLEFRecord relationship){
		if(relationship != null)
			return relationshipHandler.getDisplayText(relationship, model);

		return "--";
	}

	/**
	 * Checks whether the given relationship is a membership relationship
	 * involving the actor and an entity of the member type.
	 *
	 * @param relationship	the relationship to check
	 * @return {@code true} if it is a membership relationship
	 */
	private boolean isMembershipRelationship(final FLEFRecord relationship){
		if(actorId == null)
			return false;

		final String subjectId = FLEFRecordHelper.getChildValue(relationship, TAG_SUBJECT);
		final String objectId = FLEFRecordHelper.getChildValue(relationship, TAG_OBJECT);

		// The actor must be either the subject or the object
		final boolean actorIsSubject = actorId.equals(subjectId);
		final boolean actorIsObject = actorId.equals(objectId);
		if(!actorIsSubject && !actorIsObject)
			return false;

		// The other participant must be of the member type
		final String otherId = (actorIsSubject? objectId: subjectId);
		if(otherId == null)
			return false;

		final FLEFRecord other = model.getRecordById(otherId);
		return (other != null && memberHandlerType.equals(other.getTag()));
	}


	/**
	 * Creates a new member entity (Individual or Group) and a membership relationship
	 * with the actor.
	 *
	 * @return the new relationship record, or {@code null} if cancelled
	 */
	@Override
	protected FLEFRecord showCreateNewDialog(){
		if(actorId == null){
			JOptionPane.showMessageDialog(parent, "Actor ID not available.", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		// 1. Track existing member entities to identify the newly created one
		final Set<String> before = new HashSet<>();
		for(final FLEFRecord rec : model.getRecordsByType(memberHandlerType)){
			final String id = rec.getId();
			if(id != null)
				before.add(id);
		}

		// 2. Create a new member entity using the member handler
		final BaseRecordDialog createDialog = memberHandler.createNewDialog(parent, model);
		createDialog.setVisible(true);
		if(!createDialog.isSaved())
			return null;

		// 3. Find the newly created member ID
		String newMemberId = null;
		for(final FLEFRecord rec : model.getRecordsByType(memberHandlerType)){
			final String id = rec.getId();
			if(id != null && !before.contains(id)){
				newMemberId = id;
				break;
			}
		}
		if(newMemberId == null)
			return null;

		// 4. Create the membership relationship
		final RelationshipRecordDialog relDialog = (RelationshipRecordDialog)relationshipHandler.createNewDialog(parent, model);
		configureRelationship(relDialog, newMemberId);
		relDialog.setVisible(true);

		if(relDialog.isSaved())
			return relDialog.getRecord();

		// Rollback: remove the newly created member entity
		model.removeRecord(newMemberId);
		return null;
	}

	/**
	 * Adds an existing member entity to the actor.
	 *
	 * @return the new relationship record, or {@code null} if cancelled
	 */
	@Override
	protected FLEFRecord showAddDialog(){
		if(actorId == null){
			JOptionPane.showMessageDialog(parent, "Actor ID not available.", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		// 1. Select an existing member entity
		final String[] result = {null};
		final MultiTypeSelectionDialog dialog = new MultiTypeSelectionDialog(
			parent, model, memberHandlerType, (handlerType, selectedRecord) -> {
			if(selectedRecord != null)
				result[0] = selectedRecord.getId();
		});
		dialog.setVisible(true);

		final String memberId = result[0];
		if(memberId == null)
			return null;

		// 2. Create a relationship between the actor and the selected member
		final RelationshipRecordDialog relDialog = (RelationshipRecordDialog)relationshipHandler.createNewDialog(parent, model);
		configureRelationship(relDialog, memberId);
		relDialog.setVisible(true);

		return (relDialog.isSaved()? relDialog.getRecord(): null);
	}

	/**
	 * Configures the relationship dialog with the correct subject/object.
	 *
	 * @param relDialog	the relationship dialog
	 * @param memberId	the ID of the member entity
	 */
	private void configureRelationship(final RelationshipRecordDialog relDialog, final String memberId){
		// The subject must be the member, the object must be the actor (group)
		// According to the protocol, group_member: Individual -> Group
		if(IndividualHandler.TYPE.equals(memberHandlerType)){
			// member is Individual, actor is Group → subject = Individual, object = Group
			relDialog.withSubject(memberId, memberHandlerType);
			relDialog.withObject(actorId, actorHandlerType);
		}
		else{
			// member is Group, actor is Individual → subject = Individual, object = Group
			// (or we could allow both directions, but protocol defines group_member as Individual -> Group)
			relDialog.withSubject(actorId, actorHandlerType);
			relDialog.withObject(memberId, memberHandlerType);
		}
	}


	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord record){
		if(record == null){
			JOptionPane.showMessageDialog(parent, "Relationship not found.", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final BaseRecordDialog dialog = relationshipHandler.createEditDialog(parent, model, record);
		dialog.setVisible(true);

		return (dialog.isSaved()? record: null);
	}

	/**
	 * Edits the member entity (Individual or Group) that is linked to the actor
	 * via the selected relationship.
	 */
	private void editMemberItem(){
		final int idx = list.getSelectedIndex();
		if(idx == -1)
			return;

		final FLEFRecord relationship = items.get(idx);
		if(relationship == null || actorId == null)
			return;

		final String subjectId = FLEFRecordHelper.getChildValue(relationship, TAG_SUBJECT);
		final String objectId = FLEFRecordHelper.getChildValue(relationship, TAG_OBJECT);
		final String otherId = (actorId.equals(subjectId)? objectId: subjectId);
		if(otherId == null)
			return;

		final FLEFRecord member = model.getRecordById(otherId);
		if(member == null){
			JOptionPane.showMessageDialog(parent, "Member not found: " + otherId, "Error",
				JOptionPane.ERROR_MESSAGE);

			return;
		}

		final JDialog editDialog = memberHandler.createEditDialog(parent, model, member);
		editDialog.setVisible(true);

		// Refresh the display
		final int pos = list.getSelectedIndex();
		if(pos != -1)
			listModel.set(pos, getDisplayText(relationship));
	}

	/**
	 * Shows the notes editor for the selected relationship.
	 */
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
				listModel.set(pos, getDisplayText(relationship));
		}
	}


	/**
	 * Loads all relationship children from the given record.
	 *
	 * @param record	the record containing relationships
	 */
	public void load(final FLEFRecord record){
		clear();

		if(record == null)
			return;

		final List<FLEFRecord> relationships = FLEFRecordHelper.findChildren(record, TAG_RELATIONSHIP);
		// Filter only membership relationships involving this actor
		final List<FLEFRecord> filtered = relationships.stream()
			.filter(this::isMembershipRelationship)
			.collect(Collectors.toList());
		setItems(filtered);
	}

	/**
	 * Saves the current relationship items as children of the given record.
	 *
	 * @param record	the record to save into
	 */
	public void save(final FLEFRecord record){
		// Remove all existing RELATIONSHIP children to avoid duplicates
		FLEFRecordHelper.removeChildren(record, TAG_RELATIONSHIP);

		for(final FLEFRecord rel : getItems()){
			rel.setTag(TAG_RELATIONSHIP);
			record.addChild(rel);
		}
	}

	/**
	 * Loads relationships where the actor is either the subject or the object,
	 * without needing a parent record.
	 *
	 * @param recordId	the ID of the actor
	 */
	public void loadReference(final String recordId){
		clear();

		if(recordId == null)
			return;

		// Scan all relationships in the model to find those involving the actor
		final List<FLEFRecord> allRels = model.getRecordsByType(RelationshipHandler.TYPE);
		final List<FLEFRecord> filtered = allRels.stream()
			.filter(rel -> {
				final String subjectId = FLEFRecordHelper.getChildValue(rel, TAG_SUBJECT);
				final String objectId = FLEFRecordHelper.getChildValue(rel, TAG_OBJECT);
				return recordId.equals(subjectId) || recordId.equals(objectId);
			})
			.filter(this::isMembershipRelationship)
			.collect(Collectors.toList());
		setItems(filtered);
	}

}
