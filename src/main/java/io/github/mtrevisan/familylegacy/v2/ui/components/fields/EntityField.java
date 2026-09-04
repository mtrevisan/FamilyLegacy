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
package io.github.mtrevisan.familylegacy.v2.ui.components.fields;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.MultiTypeSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * A text field that displays and manages an entity reference.
 * It extends {@link BoundTextField} and adds behavior for selecting,
 * editing, and clearing an entity record via a popup menu.
 */
public class EntityField extends BoundTextField{

	@Serial
	private static final long serialVersionUID = -8333332073516970045L;


	public static final String PROPERTY_ENTITY_CHANGED = "entity-changed";


	private static final String TAG_VOID = "VOID";


	public enum EntityType{
		// record from reference
		ENTITY_REFERENCE,
		// record from oneof reference
		ONEOF_REFERENCE,
		// structure with reference
		CITATION_WRAPPER
	}


	private final Dialog parent;

	private final FLEFModel model;

	private final EntityType type;
	private List<? extends RecordTypeHandler<?>> handlers;
	private boolean saveAsVoid;

	private FLEFRecord entityRef;


	/**
	 * @param path	the path used for binding (it may be {@code null} if binding is handled externally
	 * @param parent	the parent dialog
	 * @param model	the FLEF model
	 * @return a new instance
	 */
	public static EntityField createForRecordFromOneofReference(final String path, final Dialog parent,
			final FLEFModel model){
		return new EntityField(path, parent, model,
			EntityType.ONEOF_REFERENCE);
	}

	public static EntityField createForRecordFromReference(final String path, final Dialog parent,
			final FLEFModel model,
			final Class<? extends RecordTypeHandler<?>> handlerType){
		return new EntityField(path, parent, model,
				EntityType.ENTITY_REFERENCE)
			.withHandlerTypes(handlerType);
	}

	public static EntityField createForStructureWithReference(final String path, final Dialog parent,
			final FLEFModel model,
			final Class<? extends RecordTypeHandler<?>> handlerType){
		return new EntityField(path, parent, model,
				EntityType.CITATION_WRAPPER)
			.withHandlerTypes(handlerType);
	}


	private EntityField(final String path, final Dialog parent, final FLEFModel model, final EntityType type){
		super(path);

		this.parent = parent;

		this.model = model;

		handlers = Collections.emptyList();
		this.type = type;
	}


	private void initComponents(){
		final Consumer<GUIHelper.MenuBuilder> menuItems = (type == EntityType.CITATION_WRAPPER
			? createMenuItemsForCitationWrapper()
			: createMenuItemsForDefault());
		GUIHelper.installBehavior(this,
			this::editItem, (type == EntityType.CITATION_WRAPPER? this::editTargetItem: null),
			null, null,
			menuItems);
	}

	private Consumer<GUIHelper.MenuBuilder> createMenuItemsForDefault(){
		return builder -> {
			builder.item("Set…", this::addItem);
			builder.separator();
			builder.selectionSensitiveItem("Edit…", this::editItem);
			builder.selectionSensitiveItem("Clear", this::clear);
		};
	}

	private Consumer<GUIHelper.MenuBuilder> createMenuItemsForCitationWrapper(){
		return builder -> {
			if(handlers.size() == 1)
				builder.item("Create New…", this::createNewItem);
			builder.item("Set…", this::addItem);
			builder.separator();
			builder.selectionSensitiveItem("Edit Record…", this::editTargetItem);
			builder.selectionSensitiveItem("Edit Citation…", this::editItem);
			builder.selectionSensitiveItem("Clear", this::clear);
		};
	}

	private void updateDisplay(){
		String value = null;
		if(entityRef != null && !entityRef.isEmpty()){
			if(type == EntityType.CITATION_WRAPPER){
				final RecordTypeHandler<?> handler = findHandler(entityRef.getTag());
				value = handler.getDisplayText(entityRef, model);
			}
			else{
				final FLEFRecord entity = model.getRecordById(entityRef.getId());
				if(entity != null){
					final RecordTypeHandler<?> handler = findHandler(entity.getTag());
					value = handler.getDisplayText(entity, model);
				}
			}
		}
		setText(value);
	}

	/**
	 * Sets the allowed handler types. Must be called before the panel is used.
	 *
	 * @param handlerTypes the record types this field can accept
	 */
	@SafeVarargs
	public final EntityField withHandlerTypes(final Class<? extends RecordTypeHandler<?>>... handlerTypes){
		assert this.handlers.isEmpty(): "Cannot assign handler type more than one time";

		for(final Class<? extends RecordTypeHandler<?>> handlerType : handlerTypes){
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(handlerType);
			if(handler == null){
				JOptionPane.showMessageDialog(this,
					"Handler for " + handlerType + " not loaded.",
					"Error", JOptionPane.ERROR_MESSAGE);

				return this;
			}
		}

		this.handlers = Arrays.stream(handlerTypes)
			.map(HandlerRegistry::getHandler)
			.toList();

		initComponents();

		addPropertyChangeListener(PROPERTY_ENTITY_CHANGED, e -> updateDisplay());

		return this;
	}

	public EntityField withSaveAsVoid(){
		saveAsVoid = true;

		return this;
	}


	/**
	 * Returns the selected entity record.
	 *
	 * @return the entity record, or {@code null} if none
	 */
	public FLEFRecord getEntity(){
		return entityRef;
	}

	/**
	 * Sets the current entity and updates the display.
	 *
	 * @param record	the entity record (it may be {@code null})
	 */
	public void setEntity(final FLEFRecord record){
		entityRef = record;

		firePropertyChange(PROPERTY_ENTITY_CHANGED, null, null);
	}

	/**
	 * Clears the current selection.
	 */
	public void clear(){
		setEntity(null);
	}

	/**
	 * Returns whether an entity is selected.
	 *
	 * @return	Whether a non-empty entity is set.
	 */
	public boolean hasData(){
		return (entityRef != null && !entityRef.isEmpty());
	}


	public void load(final FLEFRecord record){
		load(record, 0);
	}

	/**
	 * Loads the entity from a given target record.
	 *
	 * @param record the record containing the reference
	 */
	public void load(final FLEFRecord record, final int index){
		clear();

		if(record == null || record.isEmpty())
			return;

		FLEFRecord entity = null;
		if(type == EntityType.ENTITY_REFERENCE)
			entity = FLEFRecordHelper.extractRecordFromReference(record, path, model);
		else if(type == EntityType.ONEOF_REFERENCE){
			final List<FLEFRecord> entities = FLEFRecordHelper.extractRecordsFromOneOfReference(record, path, model);
			entity = (!entities.isEmpty()? entities.get(index): record);
		}
		else if(type == EntityType.CITATION_WRAPPER)
			entity = FLEFRecordHelper.extractStructureWithReference(record, path);
		setEntity(entity);
	}

	/**
	 * Saves the current entity into the target record.
	 * Removes any existing child with the given path and adds a new one.
	 *
	 * @param targetRecord	the record to save into
	 */
	public void saveReferences(final FLEFRecord targetRecord){
		if(hasData()){
			if(type == EntityType.ENTITY_REFERENCE){
				final String id = entityRef.getFormattedId();
				final FLEFRecord parentNode = FLEFRecord.createChildWithTag(path)
					.setValue(id);
				targetRecord.addChild(parentNode);
			}
			else if(type == EntityType.ONEOF_REFERENCE){
				final String tag = entityRef.getTag();
				final String id = entityRef.getFormattedId();
				final FLEFRecord parentNode = FLEFRecord.createChildWithTag(path)
					.addChild(FLEFRecord.createChildWithTagAndValue(tag, id));
				targetRecord.addChild(parentNode);
			}
			else if(type == EntityType.CITATION_WRAPPER){
				final List<FLEFRecord> recordItems = entityRef.getChildren();
				if(StringUtils.isEmpty(path)){
					targetRecord.addChildren(recordItems);

					return;
				}

				final FLEFRecord parent = FLEFRecordHelper.getOrCreateTargetNode(targetRecord, path);
				parent.addChildren(recordItems);
			}
		}
		else if(saveAsVoid){
			final FLEFRecord child = FLEFRecord.createChildWithTag(TAG_VOID);
			final FLEFRecord itemRecord = FLEFRecord.createChildWithTag(path)
				.addChild(child);
			targetRecord.addChild(itemRecord);
		}
	}

	private void addItem(){
		if(handlers.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"Empty handler types.\nCannot show dialog.",
				"Error", JOptionPane.ERROR_MESSAGE);

			return;
		}

		final List<Class<? extends RecordTypeHandler<?>>> cleaned = extractParentHandlers();
		@SuppressWarnings("unchecked")
		final MultiTypeSelectionDialog dialog = new MultiTypeSelectionDialog(parent, model,
			cleaned.toArray(Class[]::new));
		dialog.addPropertyChangeListener(MultiTypeSelectionDialog.PROPERTY_TYPE_SELECTED, e -> {
			final FLEFRecord selectedRecord = dialog.getSelectedRecord();
			if(type == EntityType.CITATION_WRAPPER){
				final String selectedRecordId = selectedRecord.getId();
				final String entityRefTag = entityRef.getTag();
				final FLEFRecord citation = FLEFRecord.createChildWithTag(entityRefTag);
				FLEFRecordHelper.updateChildValue(citation, path, selectedRecordId);

				final RecordTypeHandler<?> handler = findHandler(entityRefTag);
				final BaseRecordDialog citationDialog = handler.createEditDialog(parent, model, citation);
				citationDialog.setVisible(true);

				if(citationDialog.isSaved()){
					final FLEFRecord newCitationRecord = citationDialog.getRecord();
					setEntity(newCitationRecord);
				}
				else
					model.removeRecord(selectedRecordId);
			}
			else
				setEntity(selectedRecord);
		});
		dialog.setVisible(true);
	}

	private void editItem(){
		if(!hasData()){
			addItem();

			return;
		}

		final RecordTypeHandler<?> handler = findHandler(entityRef.getTag());
		final BaseRecordDialog dialog = handler.createEditDialog(parent, model, entityRef);
		dialog.setVisible(true);

		if(dialog.isSaved())
			// The record may have changed; refresh the display
			firePropertyChange(PROPERTY_ENTITY_CHANGED, null, null);
	}

	/**
	 * Creates a new place and adds a citation for it.
	 */
	private FLEFRecord createNewItem(){
		final RecordTypeHandler<?> handler = findHandler(entityRef.getTag());
		final RecordTypeHandler<?> parentHandler = handler.getParentHandler();
		final BaseRecordDialog dialog = parentHandler.createNewDialog(parent, model);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			final FLEFRecord newRecord = dialog.getRecord();
			if(newRecord != null){
				final String newRecordId = newRecord.getId();
				final FLEFRecord citation = FLEFRecord.createEmpty();
				FLEFRecordHelper.updateChildValue(citation, path, newRecordId);

				final BaseRecordDialog citationDialog = handler.createEditDialog(parent, model, citation);
				citationDialog.setVisible(true);

				if(citationDialog.isSaved()){
					final FLEFRecord newCitationRecord = citationDialog.getRecord();
					setEntity(newCitationRecord);

					return newCitationRecord;
				}
				else
					model.removeRecord(newRecordId);
			}
		}

		return null;
	}

	private void editTargetItem(){
		if(!hasData()){
			createNewItem();

			return;
		}

		final RecordTypeHandler<?> handler = findHandler(entityRef.getTag());
		final String citedRecordId = FLEFRecordHelper.getChildValue(entityRef, handler.getCitedType());
		final FLEFRecord citedRecord = model.getRecordById(citedRecordId);
		final RecordTypeHandler<?> citedRecordHandler = HandlerRegistry.getHandler(citedRecord.getTag());
		final BaseRecordDialog dialog = citedRecordHandler.createEditDialog(parent, model, citedRecord);
		dialog.setVisible(true);

		if(dialog.isSaved())
			// Only needed here because the reference to 'record' doesn't change, but the internal data does.
			updateDisplay();
	}

	private RecordTypeHandler<?> findHandler(final String type){
		for(final RecordTypeHandler<?> handler : handlers){
			if(Strings.CI.equals(handler.getType(), type))
				return handler;

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
		final List<Class<? extends RecordTypeHandler<?>>> cleaned = new ArrayList<>(handlers.size());
		for(final RecordTypeHandler<?> handler : handlers){
			final RecordTypeHandler<?> parentHandler = handler.getParentHandler();
			cleaned.add((Class<? extends RecordTypeHandler<?>>)(parentHandler != null
				? parentHandler.getClass()
				: handler.getClass()));
		}
		return cleaned;
	}


	@Override
	public boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final EntityField other = (EntityField)obj;
		return Objects.equals(entityRef, other.entityRef);
	}

	@Override
	public int hashCode(){
		return Objects.hashCode(entityRef);
	}

	@Override
	public String toString(){
		final StringBuilder sb = new StringBuilder(super.toString());
		if(entityRef != null && !entityRef.isEmpty())
			sb.append(", entity: ")
				.append(entityRef);
		return sb.toString();
	}

}
