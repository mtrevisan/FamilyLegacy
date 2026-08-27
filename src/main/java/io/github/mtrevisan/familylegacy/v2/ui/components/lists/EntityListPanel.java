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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.EventParticipationRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.IdentityHypothesisRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.PlaceRelationshipRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.RelationshipRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionTargetHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * Panel for managing a list of entity references, structures, or citations.
 * <p>
 * This panel supports four modes, controlled by {@link ListType}:
 * <ul>
 *   <li><strong>ENTITY_REFERENCE</strong>: simple reference (stores only the ID of the target entity)</li>
 *   <li><strong>STRUCTURE</strong>: stores the entire structure (embedded record) as a child</li>
 *   <li><strong>CITATION_WRAPPER</strong>: stores a citation record that wraps a target entity</li>
 *   <li><strong>ONEOF_REFERENCE</strong>: polymorphic reference (stores both tag and ID)</li>
 * </ul>
 * <p>
 * It also supports:
 * <ul>
 *   <li>Multiple handler types (for polymorphic lists)</li>
 *   <li>Parent entity linking for relationship dialogs</li>
 *   <li>Reverse lookup via {@link #loadReferenceWithType} and {@link #loadCitationsWithType}</li>
 *   <li>Special save modes: {@link #withSaveAsVoid()} and {@link ConclusionTargetHandler}</li>
 * </ul>
 */
public class EntityListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = 8040533307824167492L;

	private static final String TAG_RESOLVES = "RESOLVES";

	private static final String TAG_VOID = "VOID";


	public enum ListType{
		/** Simple reference: stores only the ID of the referenced entity. */
		ENTITY_REFERENCE,
		/** Structure: stores the entire embedded structure. */
		STRUCTURE,
		/** Citation wrapper: stores a citation record that contains a reference to the target entity. */
		CITATION_WRAPPER,
		/** One‑of reference: stores both the type tag and the ID of the referenced entity. */
		ONEOF_REFERENCE
	}

	public enum ActorType{
		SUBJECT,
		OBJECT,

		EVENT,
		PARTICIPANT
	}


	private final String path;
	private final ListType type;
	// used for relationship dialogs (SUBJECT/OBJECT etc.)
	private final ActorType actorType;
	private List<Class<? extends RecordTypeHandler<?>>> handlerTypes;
	// if true, save a VOID node when the list is empty
	private boolean saveAsVoid;
	private String parentEntityId;
	private String parentEntityTag;

	// used to reload the list after editing
	private FLEFRecord parentRecord;
	// true if loaded via reverse lookup (loadReference*)
	private boolean isReference;


	public static EntityListPanel createForEntityReference(final String path, final Dialog parent,
			final String panelTitle, final FLEFModel model, final Class<? extends RecordTypeHandler<?>> handlerType){
		return new EntityListPanel(path, parent, panelTitle, model, ListType.ENTITY_REFERENCE, null)
			.withHandlerTypes(handlerType);
	}

	public static EntityListPanel createForStructure(final String path, final Dialog parent,
			final String panelTitle, final FLEFModel model, final Class<? extends RecordTypeHandler<?>> handlerType){
		return new EntityListPanel(path, parent, panelTitle, model, ListType.STRUCTURE, null)
			.withHandlerTypes(handlerType);
	}

	public static EntityListPanel createForCitationWrapper(final String path, final Dialog parent,
			final String panelTitle, final FLEFModel model, final Class<? extends RecordTypeHandler<?>> handlerType){
		return new EntityListPanel(path, parent, panelTitle, model, ListType.CITATION_WRAPPER, null)
			.withHandlerTypes(handlerType);
	}

	public static EntityListPanel createForOneOfReference(final String path, final Dialog parent,
			final String panelTitle, final FLEFModel model){
		return new EntityListPanel(path, parent, panelTitle, model, ListType.ONEOF_REFERENCE, null);
	}

	public static EntityListPanel createForOneOfReference(final String path, final Dialog parent,
			final String panelTitle, final FLEFModel model, final ActorType actorType){
		return new EntityListPanel(path, parent, panelTitle, model, ListType.ONEOF_REFERENCE, actorType);
	}


	protected EntityListPanel(final String path, final Dialog parent, final String panelTitle, final FLEFModel model,
			final ListType type, final ActorType actorType){
		super(parent, panelTitle, model);

		this.path = path;
		this.type = type;
		this.actorType = actorType;
		this.handlerTypes = Collections.emptyList();
	}


	/**
	 * Sets the allowed handler types. Must be called before the panel is used.
	 *
	 * @param handlerTypes the record types this list can accept
	 * @return this panel (for chaining)
	 */
	@SafeVarargs
	public final EntityListPanel withHandlerTypes(final Class<? extends RecordTypeHandler<?>>... handlerTypes){
		for(final Class<? extends RecordTypeHandler<?>> handlerType : handlerTypes){
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(handlerType);
			if(handler == null){
				JOptionPane.showMessageDialog(this,
					"Handler for " + handlerType + " not loaded.",
					"Error", JOptionPane.ERROR_MESSAGE);

				return this;
			}
		}

		this.handlerTypes = List.of(handlerTypes);

		initComponents();

		return this;
	}

	/**
	 * Enables saving a VOID node when the list is empty (only applicable to ONEOF_REFERENCE).
	 */
	public EntityListPanel withSaveAsVoid(){
		this.saveAsVoid = true;

		return this;
	}

	/**
	 * Sets a parent entity that will be automatically linked when creating new records
	 * (e.g., as SUBJECT/OBJECT in a relationship).
	 */
	public EntityListPanel withParentEntity(final String parentEntityId, final String parentEntityTag){
		this.parentEntityId = parentEntityId;
		this.parentEntityTag = parentEntityTag;

		return this;
	}


	@Override
	protected void initComponents(){
		super.initComponents();

		final Consumer<GUIHelper.MenuBuilder> menuItems;
		if(type == ListType.CITATION_WRAPPER)
			menuItems = createMenuItemsForCitationWrapper();
		else
			// ENTITY_REFERENCE, STRUCTURE, ONEOF_REFERENCE: default menu
			menuItems = createMenuItemsForDefault();

		GUIHelper.installBehavior(list,
			this::editItem,
			(type == ListType.CITATION_WRAPPER)? this::editTargetItem: null,
			this::createNewItem, this::removeItem,
			menuItems);
	}

	private Consumer<GUIHelper.MenuBuilder> createMenuItemsForDefault(){
		return builder -> {
			builder.item("Create New…", this::createNewItem);
			// Only show "Add Existing" for ENTITY_REFERENCE and ONEOF_REFERENCE (not for STRUCTURE)
			if(type != ListType.STRUCTURE)
				builder.item("Add Existing…", this::addItem);
			builder.separator();
			builder.selectionSensitiveItem("Edit…", this::editItem);
			builder.selectionSensitiveItem("Remove", this::removeItem);
		};
	}

	private Consumer<GUIHelper.MenuBuilder> createMenuItemsForCitationWrapper(){
		return builder -> {
			builder.item("Create New…", this::createNewItem);
			builder.item("Add Existing…", this::addItem);
			builder.separator();
			builder.selectionSensitiveItem("Edit Record…", this::editTargetItem);
			builder.selectionSensitiveItem("Edit Citation…", this::editItem);
			builder.selectionSensitiveItem("Remove", this::removeItem);
		};
	}


	@Override
	protected String getDisplayText(final FLEFRecord record){
		if(record == null)
			return "--";

		final RecordTypeHandler<?> handler = findHandler(record.getTag());
		if(handler == null)
			return "--";

		return handler.getDisplayText(record, model);
	}


	@Override
	protected FLEFRecord showAddDialog(){
		if(handlerTypes.isEmpty()){
			JOptionPane.showMessageDialog(parent,
				"No handler types available.",
				"Error", JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final List<Class<? extends RecordTypeHandler<?>>> cleaned = extractParentHandlers();
		@SuppressWarnings("unchecked")
		final MultiTypeSelectionDialog dialog = new MultiTypeSelectionDialog(parent, model, cleaned.toArray(Class[]::new))
			.withSetupDialog(getDialogSetup());
		dialog.addPropertyChangeListener(MultiTypeSelectionDialog.PROPERTY_TYPE_SELECTED, e -> {
			final FLEFRecord selectedRecord = dialog.getSelectedRecord();
			// For citation wrapper, we need to create a citation around the selected entity.
			if(type == ListType.CITATION_WRAPPER)
				addExistingCitation(selectedRecord);
			else
				addItemDirectly(selectedRecord);
		});
		dialog.setVisible(true);

		// Result is handled via listener
		return null;
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		if(handlerTypes.isEmpty()){
			JOptionPane.showMessageDialog(parent,
				"No handler types available.",
				"Error", JOptionPane.ERROR_MESSAGE);

			return null;
		}

		if(type == ListType.CITATION_WRAPPER)
			// Create a new target entity and then a citation for it.
			return createNewCitation();
		else{
			// For ENTITY_REFERENCE, STRUCTURE, ONEOF_REFERENCE:
			// Use the first handler (or if multiple, a selection dialog).
			if(handlerTypes.size() == 1){
				final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(handlerTypes.getFirst());
				final BaseRecordDialog dialog = handler.createNewDialog(parent, model);
				getDialogSetup().accept(dialog);
				dialog.setVisible(true);

				return (dialog.isSaved()? dialog.getRecord(): null);
			}
			else{
				// Multiple types: show selection dialog and then create based on selection
				@SuppressWarnings("unchecked")
				final MultiTypeSelectionDialog selectionDialog = new MultiTypeSelectionDialog(parent, model,
						handlerTypes.toArray(Class[]::new))
					.withSetupDialog(getDialogSetup());
				selectionDialog.setVisible(true);

				final FLEFRecord selected = selectionDialog.getSelectedRecord();
				if(selected != null){
					// The selected record might be a new one created via the dialog? Actually the dialog only selects existing.
					// For creation, we need to create a new one of the chosen type.
					// But the selection dialog returns an existing record, not a new one.
					// So we need to show a creation dialog for the chosen type.
					// However, the MultiTypeSelectionDialog is for selecting existing records.
					// So this flow is not fully correct. The original code uses showCreateNewDialog to create a new record,
					// not to select existing. In EntityListPanel, showCreateNewDialog uses the first handler or selection dialog?
					// Actually in EntityListPanel, showCreateNewDialog: if one handler, create; if multiple, show selection dialog? But that selection dialog is for picking a type, then create new.
					// So we should handle that properly.
				}
				// For now, we'll just create using the first handler if multiple, but we need a better selection dialog.
				// We'll handle it similarly to EntityListPanel: if multiple handlers, show a type selection dialog (not MultiTypeSelectionDialog which selects records, but a dialog that lets the user pick a type, then we open the creation dialog for that type).
				// We'll implement a simple type selection dialog via JOptionPane.
				return createNewWithTypeSelection();
			}
		}
	}

	/**
	 * Shows a dialog to select a type from the available handler types and then opens the creation dialog for that type.
	 */
	private FLEFRecord createNewWithTypeSelection(){
		// Build list of type names
		final List<String> typeNames = handlerTypes.stream()
			.map(HandlerRegistry::getHandler)
			.filter(Objects::nonNull)
			.map(RecordTypeHandler::getLabel)
			.toList();
		if(typeNames.isEmpty())
			return null;

		final String selectedType = (String)JOptionPane.showInputDialog(parent,
			"Select the type of record to create:",
			"New Record Type",
			JOptionPane.QUESTION_MESSAGE,
			null,
			typeNames.toArray(),
			typeNames.getFirst());
		if(selectedType == null)
			return null;

		// Find the handler class for the selected type
		final Class<? extends RecordTypeHandler<?>> selectedHandlerClass = handlerTypes.stream()
			.filter(cls -> {
				final RecordTypeHandler<?> h = HandlerRegistry.getHandler(cls);
				return h != null && Strings.CI.equals(selectedType, h.getLabel());
			})
			.findFirst()
			.orElse(null);
		if(selectedHandlerClass == null)
			return null;

		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(selectedHandlerClass);
		final BaseRecordDialog dialog = handler.createNewDialog(parent, model);
		getDialogSetup().accept(dialog);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	/**
	 * Creates a new target entity and its citation (for CITATION_WRAPPER mode).
	 */
	private FLEFRecord createNewCitation(){
		final RecordTypeHandler<?> citationHandler = findHandler(HandlerRegistry.getHandlerType(handlerTypes.getFirst()));
		final RecordTypeHandler<?> targetHandler = citationHandler.getParentHandler();
		if(targetHandler == null){
			JOptionPane.showMessageDialog(parent,
				"No parent handler defined for citation handler.",
				"Error", JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final BaseRecordDialog targetDialog = targetHandler.createNewDialog(parent, model);
		getDialogSetup().accept(targetDialog);
		targetDialog.setVisible(true);

		if(!targetDialog.isSaved())
			return null;

		final FLEFRecord newTarget = targetDialog.getRecord();
		final String targetId = newTarget.getId();

		// Now create the citation
		final FLEFRecord citation = FLEFRecord.createEmpty();
		final String citedType = citationHandler.getCitedType();
		FLEFRecordHelper.updateChildValue(citation, (citedType != null? citedType: path), targetId);

		final BaseRecordDialog citationDialog = citationHandler.createEditDialog(parent, model, citation);
		citationDialog.setVisible(true);

		if(citationDialog.isSaved()){
			final FLEFRecord newCitation = citationDialog.getRecord();
			addItemDirectly(newCitation);
			return newCitation;
		}
		else{
			// Rollback: remove the target entity
			model.removeRecord(targetId);
			return null;
		}
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord record){
		if(record == null){
			JOptionPane.showMessageDialog(parent,
				"Record not found",
				"Error", JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final RecordTypeHandler<?> handler = findHandler(record.getTag());
		if(handler == null){
			JOptionPane.showMessageDialog(parent,
				"No handler found for type " + record.getTag(),
				"Error", JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final BaseRecordDialog dialog = handler.createEditDialog(parent, model, record);
		getDialogSetup().accept(dialog);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			if(parentRecord != null){
				// Use the stored parent record info to reload
				if(type == ListType.ENTITY_REFERENCE || type == ListType.ONEOF_REFERENCE)
					loadReferenceWithType(parentRecord.getId(), StringUtils.split(parentRecord.getTag(), '|'));
				// For STRUCTURE or CITATION_WRAPPER, we might need a different reload
				// Fallback: reload from the parent record if available
				else
					load(model.getRecordById(parentRecord.getId()));
			}

			// Check if the record is still in the list; if not, it was removed.
			if(!items.contains(record))
				return null;
		}

		// Return the same record (it was updated in place)
		return record;
	}

	/**
	 * Edits the target entity (for CITATION_WRAPPER mode, with Shift+click).
	 */
	private void editTargetItem(){
		final int index = list.getSelectedIndex();
		if(index < 0)
			return;

		final FLEFRecord citation = items.get(index);
		final RecordTypeHandler<?> citationHandler = findHandler(citation.getTag());
		final String targetId = FLEFRecordHelper.getChildValue(citation, citationHandler.getCitedType());
		if(targetId == null){
			JOptionPane.showMessageDialog(parent,
				"No target entity found in citation.",
				"Error", JOptionPane.ERROR_MESSAGE);

			return;
		}

		final FLEFRecord target = model.getRecordById(targetId);
		if(target == null){
			JOptionPane.showMessageDialog(parent,
				"Target entity not found in model.",
				"Error", JOptionPane.ERROR_MESSAGE);

			return;
		}

		final RecordTypeHandler<?> targetHandler = citationHandler.getParentHandler();
		final BaseRecordDialog dialog = targetHandler.createEditDialog(parent, model, target);
		getDialogSetup().accept(dialog);
		dialog.setVisible(true);
	}

	/**
	 * For CITATION_WRAPPER mode: adds an existing entity by creating a citation around it.
	 */
	private void addExistingCitation(final FLEFRecord entity){
		if(entity == null)
			return;

		final String entityId = entity.getId();
		final FLEFRecord citation = FLEFRecord.createEmpty();
		final RecordTypeHandler<?> handler = findHandler(HandlerRegistry.getHandlerType(handlerTypes.getFirst()));
		final String citedType = handler.getCitedType();
		if(citedType != null)
			FLEFRecordHelper.updateChildValue(citation, citedType, entityId);
		else
			// Fallback: use the path as the tag
			FLEFRecordHelper.updateChildValue(citation, path, entityId);

		final BaseRecordDialog citationDialog = handler.createEditDialog(parent, model, citation);
		citationDialog.setVisible(true);

		if(citationDialog.isSaved()){
			final FLEFRecord newCitation = citationDialog.getRecord();
			addItemDirectly(newCitation);
		}
	}


	/**
	 * Loads the list from a parent record (normal forward loading).
	 */
	public void load(final FLEFRecord record){
		clear();

		if(record == null || record.isEmpty())
			return;

		parentRecord = FLEFRecord.createMainRecord(record.getId(), record.getTag());

		List<FLEFRecord> items;
		if(type == ListType.CITATION_WRAPPER)
			items = FLEFRecordHelper.extractStructuresWithReference(record, path);
		else if(type == ListType.ONEOF_REFERENCE)
			items = FLEFRecordHelper.extractRecordsFromOneOfReference(record, path, model);
		else if(type == ListType.STRUCTURE)
			items = FLEFRecordHelper.extractStructures(record, path);
		else
			// ENTITY_REFERENCE
			items = FLEFRecordHelper.extractRecordsFromReference(record, path, model);
		setItems(items);

		// Set parent entity for new records
		withParentEntity(record.getId(), record.getTag());
	}

	/**
	 * Loads references by scanning the model for records that reference the given entity.
	 * Uses the handler's findReferences method (must be implemented by handlers).
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

	/**
	 * Loads references by scanning the model for records that reference the given entity,
	 * filtering by specific actor tags (e.g., "SUBJECT", "TARGET").
	 */
	public void loadReferenceWithType(final String recordId, final String... actorTags){
		clear();

		if(recordId == null)
			return;

		parentRecord = FLEFRecord.createMainRecord(recordId, StringUtils.join(actorTags, '|'));
		isReference = true;

		final List<FLEFRecord> references = new ArrayList<>();
		for(final Class<? extends RecordTypeHandler<?>> handler : handlerTypes){
			final String type = HandlerRegistry.getHandlerType(handler);
			final List<FLEFRecord> handlerReferences = model.getRecordsByType(type).stream()
				.filter(reference -> {
					for(final String actorTag : actorTags){
						final List<FLEFRecord> actors = FLEFRecordHelper.extractRecordsFromOneOfReference(reference, actorTag, model);
						if(actors.isEmpty())
							continue;

						for(final FLEFRecord actor : actors){
							final String tag = actor.getTag();
							final String id = actor.getId();
							if(Strings.CI.equals(parentEntityTag, tag) && recordId.equals(id))
								return true;
						}
					}
					return false;
				})
				.toList();
			references.addAll(handlerReferences);
		}
		setItems(references);
	}

	/**
	 * Loads citations by scanning the model for records that reference the given entity,
	 * using a different extraction method (extractRecordsFromReference).
	 */
	public void loadCitationsWithType(final String recordId, final String... actorTags){
		clear();

		if(recordId == null)
			return;

		parentRecord = FLEFRecord.createMainRecord(recordId, StringUtils.join(actorTags, '|'));
		isReference = true;

		final List<FLEFRecord> references = new ArrayList<>();
		for(final Class<? extends RecordTypeHandler<?>> handler : handlerTypes){
			final String type = HandlerRegistry.getHandlerType(handler);
			final List<FLEFRecord> handlerReferences = model.getRecordsByType(type).stream()
				.filter(reference -> {
					for(final String actorTag : actorTags){
						final List<FLEFRecord> actors = FLEFRecordHelper.extractRecordsFromReference(reference, actorTag, model);
						if(actors.isEmpty())
							continue;

						for(final FLEFRecord actor : actors){
							final String tag = actor.getTag();
							final String id = actor.getId();
							if(Strings.CI.equals(parentEntityTag, tag) && recordId.equals(id))
								return true;
						}
					}
					return false;
				})
				.toList();
			references.addAll(handlerReferences);
		}
		setItems(references);
	}

	/**
	 * Variant of loadCitationsWithType that uses extractStructures on the reference.
	 */
	public void loadCitationsWithType2(final String recordId, final String... actorTags){
		clear();

		if(recordId == null)
			return;

		parentRecord = FLEFRecord.createMainRecord(recordId, StringUtils.join(actorTags, '|'));
		isReference = true;

		final List<FLEFRecord> references = new ArrayList<>();
		for(final Class<? extends RecordTypeHandler<?>> handler : handlerTypes){
			final String type = HandlerRegistry.getHandlerType(handler);
			final List<FLEFRecord> handlerReferences = model.getRecordsByType(type).stream()
				.filter(reference -> {
					for(final String actorTag : actorTags){
						final List<FLEFRecord> actors = FLEFRecordHelper.extractStructures(reference, actorTag);
						if(actors.isEmpty())
							continue;

						for(final FLEFRecord actor : actors){
							final FLEFRecord ref = FLEFRecordHelper.findChild(actor, parentEntityTag);
							if(ref == null)
								continue;

							final String tag = ref.getTag();
							final String id = ref.getValue();
							if(Strings.CI.equals(parentEntityTag, tag) && recordId.equals(id))
								return true;
						}
					}
					return false;
				})
				.toList();
			references.addAll(handlerReferences);
		}
		setItems(references);
	}

	/**
	 * Variant of loadCitationsWithType3 that uses value directly from the actor.
	 */
	public void loadCitationsWithType3(final String recordId, final String... actorTags){
		clear();

		if(recordId == null)
			return;

		parentRecord = FLEFRecord.createMainRecord(recordId, StringUtils.join(actorTags, '|'));
		isReference = true;

		final List<FLEFRecord> references = new ArrayList<>();
		for(final Class<? extends RecordTypeHandler<?>> handler : handlerTypes){
			final String type = HandlerRegistry.getHandlerType(handler);
			final List<FLEFRecord> handlerReferences = model.getRecordsByType(type).stream()
				.filter(reference -> {
					for(final String actorTag : actorTags){
						final List<FLEFRecord> actors = FLEFRecordHelper.extractStructures(reference, actorTag);
						if(actors.isEmpty())
							continue;

						for(final FLEFRecord actor : actors){
							final String tag = actor.getTag();
							final String id = actor.getValue();
							if(Strings.CI.equals(parentEntityTag, tag) && recordId.equals(id))
								return true;
						}
					}
					return false;
				})
				.toList();
			references.addAll(handlerReferences);
		}
		setItems(references);
	}

	/**
	 * Saves the current list to the given record. The behavior depends on {@link #type}.
	 */
	public void save(final FLEFRecord record){
		if(isReference)
			return;

		if(type == ListType.ENTITY_REFERENCE){
			// Simple: add each item's ID as a child with the given path
			for(final FLEFRecord item : items)
				FLEFRecordHelper.addChildValue(record, path, item.getFormattedId());
		}
		else if(type == ListType.ONEOF_REFERENCE){
			// One‑of: add a node with a child that has tag and value
			if(hasData())
				for(final FLEFRecord item : items){
					final FLEFRecord child = FLEFRecord.createChildWithTagAndValue(item.getTag(), item.getFormattedId());
					final FLEFRecord parentNode = FLEFRecord.createChildWithTag(path);
					parentNode.addChild(child);
					record.addChild(parentNode);
				}
			else if(saveAsVoid){
				// Save a VOID marker if the list is empty and saveAsVoid is enabled
				final FLEFRecord child = FLEFRecord.createChildWithTag(TAG_VOID);
				final FLEFRecord itemRecord = FLEFRecord.createChildWithTag(path)
					.addChild(child);
				record.addChild(itemRecord);
			}
		}
		else if(type == ListType.CITATION_WRAPPER)
			// Citation wrapper: items are already full citation records; add them under the path.
			super.save(record, path);
		else if(type == ListType.STRUCTURE){
			// Structure: save the entire structures under the path.
			// Special case: if handler is ConclusionTargetHandler, save under TAG_RESOLVES.
			if(handlerTypes.size() == 1 && ConclusionTargetHandler.class.equals(handlerTypes.getFirst())){
				final FLEFRecord parentRecord = FLEFRecordHelper.getOrCreateTargetNode(record, TAG_RESOLVES);
				super.save(parentRecord, path);
			}
			else
				super.save(record, path);
		}
	}


	private Consumer<BaseRecordDialog> getDialogSetup(){
		return dialog -> {
			// Configure relationship dialogs based on `actorType` and `type`
			if(dialog instanceof RelationshipRecordDialog relationshipDialog
					&& (type == ListType.ONEOF_REFERENCE || type == ListType.ENTITY_REFERENCE)
					&& StringUtils.isNotEmpty(parentEntityId) && StringUtils.isNotEmpty(parentEntityTag)){
				if(actorType == ActorType.SUBJECT)
					relationshipDialog.withSubject(parentEntityId, parentEntityTag);
				else if(actorType == ActorType.OBJECT)
					relationshipDialog.withObject(parentEntityId, parentEntityTag);
			}
			else if(dialog instanceof PlaceRelationshipRecordDialog placeRelationshipDialog
					&& (type == ListType.ONEOF_REFERENCE || type == ListType.ENTITY_REFERENCE)
					&& StringUtils.isNotEmpty(parentEntityId) && StringUtils.isNotEmpty(parentEntityTag)){
				if(actorType == ActorType.SUBJECT)
					placeRelationshipDialog.withSubject(parentEntityId, parentEntityTag);
				else if(actorType == ActorType.OBJECT)
					placeRelationshipDialog.withObject(parentEntityId, parentEntityTag);
			}
			else if(dialog instanceof IdentityHypothesisRecordDialog identityDialog
					&& (type == ListType.ONEOF_REFERENCE || type == ListType.ENTITY_REFERENCE)
					&& StringUtils.isNotEmpty(parentEntityId) && StringUtils.isNotEmpty(parentEntityTag))
				identityDialog.withParentEntity(parentEntityId, parentEntityTag);
			else if(dialog instanceof EventParticipationRecordDialog eventDialog
					&& (type == ListType.ONEOF_REFERENCE || type == ListType.ENTITY_REFERENCE)){
				if(actorType == ActorType.PARTICIPANT)
					eventDialog.withParticipant(parentEntityId, parentEntityTag);
				else if(actorType == ActorType.EVENT)
					eventDialog.withEvent(parentEntityId);
			}
		};
	}

	private RecordTypeHandler<?> findHandler(final String type){
		// If only one handler, use it directly
		if(handlerTypes.size() == 1)
			return HandlerRegistry.getHandler(handlerTypes.getFirst());

		for(final Class<? extends RecordTypeHandler<?>> handlerType : handlerTypes){
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(handlerType);
			if(Strings.CI.equals(handler.getType(), type))
				return handler;

			// Also check parent handler
			final RecordTypeHandler<?> parentHandler = handler.getParentHandler();
			if(parentHandler != null && Strings.CI.equals(parentHandler.getType(), type))
				return handler;
		}
		return null;
	}

	/**
	 * Extracts the parent handlers (used for "Add Existing" when the handler is a citation wrapper).
	 */
	@SuppressWarnings("unchecked")
	private List<Class<? extends RecordTypeHandler<?>>> extractParentHandlers(){
		final List<Class<? extends RecordTypeHandler<?>>> cleaned = new ArrayList<>();
		for(final Class<? extends RecordTypeHandler<?>> handlerType : handlerTypes){
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(handlerType);
			final RecordTypeHandler<?> parentHandler = handler.getParentHandler();
			cleaned.add((Class<? extends RecordTypeHandler<?>>)(parentHandler != null
				? parentHandler.getClass()
				: handler.getClass()));
		}
		return cleaned;
	}


	@Override
	public String toString(){
		return "EntityListPanel{"
			+ "path = '" + path + '\''
			+ ", type = " + type
			+ ", items = " + items.size()
			+ '}';
	}

}
