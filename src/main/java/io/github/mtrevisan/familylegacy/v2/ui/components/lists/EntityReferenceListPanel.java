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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.EventParticipationRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.IdentityHypothesisRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.MultiTypeSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.RelationshipRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionTargetHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * Panel for managing a list of entity references (records or structures).
 * <p>
 * This panel supports two modes:
 * <ul>
 *   <li><strong>Record mode</strong>: stores only the ID of the referenced entity</li>
 *   <li><strong>Structure mode</strong>: stores the entire entity record</li>
 * </ul>
 * It also supports setting a parent entity for new records via {@link #withParentEntity(String, String)}.
 */
public class EntityReferenceListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = -2938815950958721359L;


	private static final String TAG_RESOLVES = "RESOLVES";


	protected enum RelationType{
		RECORD,
		STRUCTURE
	}

	public enum ActorType{
		SUBJECT,
		OBJECT,

		EVENT,
		PARTICIPANT
	}


	private final String path;
	private final RelationType relationType;
	private final ActorType actorType;

	private List<Class<? extends RecordTypeHandler<?>>> handlerTypes;

	private FLEFRecord parentRecord;

	private String parentEntityId;
	private String parentEntityTag;


	/**
	 * Creates a panel in record mode (stores only the ID of the referenced entity).
	 *
	 * @param path	the path where references are stored
	 * @param parent	the parent dialog
	 * @param panelTitle	the border title
	 * @param model	the FLEF model
	 * @return a new EntityReferenceListPanel in record mode
	 */
	public static EntityReferenceListPanel createForRecord(final String path, final Dialog parent,
			final String panelTitle, final FLEFModel model){
		return createForRecord(path, parent, panelTitle, model, null);
	}

	public static EntityReferenceListPanel createForRecord(final String path, final Dialog parent,
			final String panelTitle, final FLEFModel model, final ActorType actorType){
		return new EntityReferenceListPanel(path, parent, panelTitle, model, RelationType.RECORD, actorType);
	}

	/**
	 * Creates a panel in structure mode (stores the entire entity record).
	 *
	 * @param path	the path where references are stored
	 * @param parent	the parent dialog
	 * @param panelTitle	the border title
	 * @param model	the FLEF model
	 * @return a new EntityReferenceListPanel in structure mode
	 */
	public static EntityReferenceListPanel createForStructure(final String path, final Dialog parent,
			final String panelTitle, final FLEFModel model){
		return createForStructure(path, parent, panelTitle, model, null);
	}

	public static EntityReferenceListPanel createForStructure(final String path, final Dialog parent,
			final String panelTitle, final FLEFModel model, final ActorType actorType){
		return new EntityReferenceListPanel(path, parent, panelTitle, model, RelationType.STRUCTURE, actorType);
	}


	/**
	 * Constructs an EntityReferenceListPanel.
	 *
	 * @param path	the path where references are stored
	 * @param parent	the parent dialog
	 * @param panelTitle	the border title
	 * @param model	the FLEF model
	 * @param relationType	{@code true} for record mode (store only ID), {@code false} for structure mode
	 */
	protected EntityReferenceListPanel(final String path, final Dialog parent, final String panelTitle,
			final FLEFModel model, final RelationType relationType, final ActorType actorType){
		super(parent, panelTitle, model);

		this.path = path;
		this.relationType = relationType;
		this.actorType = actorType;

		handlerTypes = Collections.emptyList();


		final Consumer<GUIHelper.MenuBuilder> menuItems = (relationType == RelationType.RECORD
			? createMenuItemsForRecord()
			: createMenuItemsForStructure());
		GUIHelper.installBehavior(list,
			this::editItem, null,
			this::createNewItem, this::removeItem,
			menuItems
		);
	}

	/**
	 * Sets the parent entity for new records created by this panel.
	 * This is used to automatically link new records to a parent entity.
	 *
	 * @param parentEntityId	the ID of the parent entity
	 * @param parentEntityTag	the handler type of the parent entity
	 * @return this panel instance (for method chaining)
	 */
	public EntityReferenceListPanel withParentEntity(final String parentEntityId, final String parentEntityTag){
		this.parentEntityId = parentEntityId;
		this.parentEntityTag = parentEntityTag;

		return this;
	}


	private Consumer<GUIHelper.MenuBuilder> createMenuItemsForRecord(){
		return builder -> {
			builder.item("Create New…", this::createNewItem);
			builder.item("Add Existing…", this::addItem);
			builder.separator();
			builder.selectionSensitiveItem("Edit…", this::editItem);
			builder.selectionSensitiveItem("Remove", this::removeItem);
		};
	}

	private Consumer<GUIHelper.MenuBuilder> createMenuItemsForStructure(){
		return builder -> {
			builder.item("Create New…", this::createNewItem);
			builder.separator();
			builder.selectionSensitiveItem("Edit…", this::editItem);
			builder.selectionSensitiveItem("Remove", this::removeItem);
		};
	}

	/**
	 * Sets the allowed handler types for the participant.
	 *
	 * @param handlerTypes the record types this field can accept
	 */
	@SafeVarargs
	public final EntityReferenceListPanel withHandlerTypes(final Class<? extends RecordTypeHandler<?>>... handlerTypes){
		for(final Class<? extends RecordTypeHandler<?>> handlerType : handlerTypes){
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(handlerType);
			if(handler == null){
				JOptionPane.showMessageDialog(this, "Handler for " + handlerType
						+ " not loaded.",
					"Error", JOptionPane.ERROR_MESSAGE);

				return this;
			}
		}

		this.handlerTypes = List.of(handlerTypes);

		return this;
	}

	@Override
	protected String getDisplayText(final FLEFRecord record){
		if(record != null){
			final RecordTypeHandler<?> handler = findHandler(record.getTag());
			return handler.getDisplayText(record, model);
		}

		return "--";
	}

	@Override
	protected FLEFRecord showAddDialog(){
		final FLEFRecord[] result = {null};
		final MultiTypeSelectionDialog dialog = new MultiTypeSelectionDialog(parent, model,
				handlerTypes.toArray(Class[]::new))
			.withSetupDialog(getDialogSetup());
		dialog.addPropertyChangeListener(MultiTypeSelectionDialog.PROPERTY_TYPE_SELECTED, e -> {
			final FLEFRecord selectedRecord = dialog.getSelectedRecord();
			result[0] = selectedRecord;
		});
		dialog.setVisible(true);

		return result[0];
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		if(!handlerTypes.isEmpty()){
			if(handlerTypes.size() == 1){
				// Single type: show creation dialog
				final RecordTypeHandler<?> selectedHandler = HandlerRegistry.getHandler(handlerTypes.getFirst());
				final BaseRecordDialog dialog = selectedHandler.createNewDialog(parent, model);
				getDialogSetup().accept(dialog);
				dialog.setVisible(true);

				return (dialog.isSaved()? dialog.getRecord(): null);
			}

			// Multiple types: show selection dialog
			final MultiTypeSelectionDialog selectionDialog = new MultiTypeSelectionDialog(parent, model,
					handlerTypes.toArray(Class[]::new))
				.withSetupDialog(getDialogSetup());
			selectionDialog.setVisible(true);

			return selectionDialog.getSelectedRecord();
		}

		return null;
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord record){
		final RecordTypeHandler<?> handler = findHandler(record.getTag());
		if(record == null){
			JOptionPane.showMessageDialog(parent, handler.getLabel() + " not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final BaseRecordDialog dialog = handler.createEditDialog(parent, model, record);
		getDialogSetup().accept(dialog);
		dialog.setVisible(true);

		if(dialog.isSaved() && parentRecord != null){
			if(relationType == RelationType.RECORD)
				loadReferenceWithType(parentRecord.getId(), StringUtils.split(parentRecord.getTag(), '|'));
			else
				load(model.getRecordById(parentRecord.getId()));

			if(!items.contains(record))
				// record was removed
				return null;
		}

		// return the same record (it was updated in place)
		return record;
	}

	private Consumer<BaseRecordDialog> getDialogSetup(){
		return recordDialog -> {
			if(recordDialog instanceof RelationshipRecordDialog relationshipDialog
					&& relationType == RelationType.RECORD
					&& StringUtils.isNotEmpty(parentEntityId) && StringUtils.isNotEmpty(parentEntityTag)){
				if(actorType == ActorType.SUBJECT)
					relationshipDialog.withSubject(parentEntityId, parentEntityTag);
				else if(actorType == ActorType.OBJECT)
					relationshipDialog.withObject(parentEntityId, parentEntityTag);
			}
			else if(recordDialog instanceof IdentityHypothesisRecordDialog identityHypothesisDialog
					&& relationType == RelationType.RECORD
					&& StringUtils.isNotEmpty(parentEntityId) && StringUtils.isNotEmpty(parentEntityTag))
				identityHypothesisDialog.withParentEntity(parentEntityId, parentEntityTag);
			else if(recordDialog instanceof EventParticipationRecordDialog eventParticipationRecordDialog){
				if(actorType == ActorType.PARTICIPANT)
					eventParticipationRecordDialog.withParticipant(parentEntityId, parentEntityTag);
				else if(actorType == ActorType.EVENT)
					eventParticipationRecordDialog.withEvent(parentEntityId);
			}
		};
	}


	/**
	 * Loads entities from the given record.
	 *
	 * @param record	the record containing the entities
	 */
	public void load(final FLEFRecord record){
		clear();

		parentRecord = FLEFRecord.createMainRecord(record.getId(), record.getTag());

		List<FLEFRecord> entities = Collections.emptyList();
		if(relationType == RelationType.RECORD)
			entities = FLEFRecordHelper.extractRecordsFromOneOfReference(record, path, model);
		else if(relationType == RelationType.STRUCTURE)
			entities = FLEFRecordHelper.extractStructures(record, path);
		if(!entities.isEmpty())
			setItems(entities);

		withParentEntity(record.getId(), record.getTag());
	}

	/**
	 * Loads entities from the given record.
	 *
	 * @param recordId	the record containing the entities
	 */
	public void loadReference(final String recordId){
		clear();

		if(recordId == null)
			return;

		parentRecord = FLEFRecord.createMainRecord(recordId, null);
		isReference = true;

		final List<FLEFRecord> references = new ArrayList<>();
		for(final Class<? extends RecordTypeHandler<?>> handlerType : handlerTypes){
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(handlerType);
			references.addAll(handler.findReferences(model, recordId, parentEntityTag));
		}
		setItems(references);
	}

	public void loadReferenceWithType(final String recordId, final String... actorTags){
		clear();

		if(recordId == null)
			return;

		parentRecord = FLEFRecord.createMainRecord(recordId, StringUtils.join(actorTags, '|'));
		isReference = true;

		final List<FLEFRecord> references = new ArrayList<>();
		for(final Class<? extends RecordTypeHandler<?>> handler : handlerTypes)
			references.addAll(model.getRecordsByType(HandlerRegistry.getHandlerType(handler)).stream()
				.filter(reference -> {
					for(final String actorTag : actorTags){
						final List<FLEFRecord> actors = FLEFRecordHelper.extractRecordsFromOneOfReference(reference, actorTag, model);
						if(actors.isEmpty())
							return false;

						for(final FLEFRecord actor : actors){
							final String tag = actor.getTag();
							final String id = actor.getId();
							if(Objects.equals(parentEntityTag, tag) && recordId.equals(id))
								return true;
						}
					}
					return false;
				})
				.toList());
		setItems(references);
	}

	public void loadCitationsWithType(final String recordId, final String... actorTags){
		clear();

		if(recordId == null)
			return;

		parentRecord = FLEFRecord.createMainRecord(recordId, StringUtils.join(actorTags, '|'));
		isReference = true;

		final List<FLEFRecord> references = new ArrayList<>();
		for(final Class<? extends RecordTypeHandler<?>> handler : handlerTypes)
				references.addAll(model.getRecordsByType(HandlerRegistry.getHandlerType(handler)).stream()
				.filter(reference -> {
					for(final String actorTag : actorTags){
						final List<FLEFRecord> actors = FLEFRecordHelper.extractRecordsFromReference(reference, actorTag, model);
						if(actors.isEmpty())
							return false;

						for(final FLEFRecord actor : actors){
							final String tag = actor.getTag();
							final String id = actor.getId();
							if(Objects.equals(parentEntityTag, tag) && recordId.equals(id))
								return true;
						}
					}
					return false;
				})
				.toList());
		setItems(references);
	}

	/**
	 * Saves the current entities to the given record.
	 * <p>
	 * In record mode, saves only the IDs of the entities.
	 * In structure mode, saves the entire entity records.
	 *
	 * @param record	the record to save to
	 */
	public void save(final FLEFRecord record){
		if(isReference)
			return;

		if(relationType == RelationType.RECORD)
			for(final FLEFRecord item : getItems())
				FLEFRecordHelper.addChild(record, path, item.getFormattedId());
		else if(handlerTypes.size() == 1 && ConclusionTargetHandler.class.equals(handlerTypes.getFirst())){
			final FLEFRecord parentRecord = FLEFRecordHelper.getOrCreateTargetNode(record, TAG_RESOLVES);
			super.save(parentRecord, path);
		}
		else
			super.save(record, path);
	}

	public void saveReferences(final FLEFRecord record){
		if(isReference)
			return;

		for(final FLEFRecord item : getItems())
			FLEFRecordHelper.addChild(record, path, item.getFormattedId());
	}

	private RecordTypeHandler<?> findHandler(final String type){
		if(handlerTypes.size() == 1)
			return HandlerRegistry.getHandler(handlerTypes.getFirst());

		for(final Class<? extends RecordTypeHandler<?>> headerType : handlerTypes){
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(headerType);
			if(handler.getType().equalsIgnoreCase(type))
				return handler;
		}
		return null;
	}

}
