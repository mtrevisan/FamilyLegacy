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
package io.github.mtrevisan.familylegacy.ui.components.fields;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.bindings.BindingsHelper;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.ui.bindings.MenuBuilder;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.awt.Window;
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

	public static final String PROPERTY_ENTITY_CHANGED = "entity-changed";


	public enum EntityType{
		// record from reference
		ENTITY_REFERENCE,
		// record from oneof reference
		ONEOF_REFERENCE,
		// structure with reference
		CITATION_WRAPPER
	}


	private final Window parent;

	private final FLEFModel model;

	private final EntityType type;
	private List<? extends RecordTypeHandler<?>> handlers;

	private FLEFRecord entityRef;


	/**
	 * @param path	the path used for binding (it may be {@code null} if binding is handled externally
	 * @param parent	the parent dialog
	 * @param model	the FLEF model
	 * @return a new instance
	 */
	public static EntityField createForRecordFromOneofReference(final String path, final Window parent,
			final FLEFModel model){
		return new EntityField(path, parent, model,
			EntityType.ONEOF_REFERENCE);
	}

	public static EntityField createForRecordFromReference(final String path, final Window parent,
			final FLEFModel model, final Class<? extends RecordTypeHandler<?>> handlerType){
		return new EntityField(path, parent, model, EntityType.ENTITY_REFERENCE)
			.withHandlerTypes(handlerType);
	}

	public static EntityField createForStructureWithReference(final String path, final Window parent,
			final FLEFModel model, final Class<? extends RecordTypeHandler<?>> handlerType){
		return new EntityField(path, parent, model, EntityType.CITATION_WRAPPER)
			.withHandlerTypes(handlerType);
	}


	private EntityField(final String path, final Window parent, final FLEFModel model, final EntityType type){
		super(path);

		this.parent = parent;

		this.model = model;

		handlers = Collections.emptyList();
		this.type = type;
	}


	private void initComponents(){
		final Consumer<MenuBuilder> menuItems = (type == EntityType.CITATION_WRAPPER
			? createMenuItemsForCitationWrapper()
			: createMenuItemsForDefault());
		BindingsHelper.installBehavior(this,
			this::editItem, (type == EntityType.CITATION_WRAPPER? this::editTargetItem: null),
			null, null,
			menuItems);
	}

	private Consumer<MenuBuilder> createMenuItemsForDefault(){
		return builder -> {
			builder.item(I18N.t("popupmenu.set"), this::addItem);
			builder.separator();
			builder.selectionSensitiveItem(I18N.t("popupmenu.edit"), this::editItem);
			builder.selectionSensitiveItem(I18N.t("popupmenu.clear"), this::clear);
		};
	}

	private Consumer<MenuBuilder> createMenuItemsForCitationWrapper(){
		return builder -> {
			if(handlers.size() == 1)
				builder.item(I18N.t("popupmenu.create_new"), this::createNewItem);
			builder.item(I18N.t("popupmenu.set"), this::addItem);
			builder.separator();
			builder.selectionSensitiveItem(I18N.t("popupmenu.edit.record"), this::editTargetItem);
			builder.selectionSensitiveItem(I18N.t("popupmenu.edit.citation"), this::editItem);
			builder.selectionSensitiveItem(I18N.t("popupmenu.clear"), this::clear);
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
				else{
					final String entityRefTag = entityRef.getTag();
					final RecordTypeHandler<?> entityRefHandler = HandlerRegistry.getHandler(entityRefTag);
					value = entityRefHandler.getDisplayText(entityRef, model);
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

		this.handlers = resolveHandlers(handlerTypes);

		initComponents();

		addPropertyChangeListener(PROPERTY_ENTITY_CHANGED, e -> updateDisplay());

		return this;
	}

	/**
	 * Changes the record types a field already set up accepts, e.g. when the other field of a dialog restricts them (the target of
	 * a relationship of a group can only be a group).
	 * <p>
	 * Unlike {@link #withHandlerTypes(Class[])} it does not set the field up again: the behavior (mouse, keys, popup menu) and the
	 * listeners are installed once, and the handler types are read each time they are needed. It is not available to the fields that
	 * wrap a citation, whose popup menu depends on how many types they accept.
	 *
	 * @param handlerTypes	the record types this field accepts from now on
	 * @return	this field
	 * @throws IllegalStateException	if the field is not set up yet, or it wraps a citation
	 */
	@SafeVarargs
	public final EntityField restrictHandlerTypes(final Class<? extends RecordTypeHandler<?>>... handlerTypes){
		if(handlers.isEmpty())
			throw new IllegalStateException("The handler types must be assigned with withHandlerTypes() first");
		if(type == EntityType.CITATION_WRAPPER)
			throw new IllegalStateException("The handler types of a field that wraps a citation cannot be changed");

		handlers = resolveHandlers(handlerTypes);

		return this;
	}

	private static List<RecordTypeHandler<?>> resolveHandlers(final Class<? extends RecordTypeHandler<?>>[] handlerTypes){
		return Arrays.stream(handlerTypes)
			.<RecordTypeHandler<?>>map(HandlerRegistry::getHandler)
			.toList();
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
		if(record != null){
			final boolean match = isInHandlerTypes(record);
			if(match)
				entityRef = record;
		}
		else
			entityRef = null;

		firePropertyChange(PROPERTY_ENTITY_CHANGED, null, null);
	}

	private boolean isInHandlerTypes(final FLEFRecord record){
		if(record == null)
			return false;

		final String recordTag = record.getTag();
		for(final RecordTypeHandler<?> handler : handlers)
			if(handler.getType().equalsIgnoreCase(recordTag))
				return true;
		return false;
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
			if(!entities.isEmpty())
				entity = entities.get(index);
			else if(isInHandlerTypes(record))
				entity = record;
		}
		else if(type == EntityType.CITATION_WRAPPER)
			entity = FLEFRecordHelper.extractStructureWithReference(record, path);
		setEntity(entity);
	}

	/**
	 * Saves the current entity into the target record, appending it at the given path.
	 * <p>
	 * Existing children at the path are NOT removed, because list panels call this method once per item on the same
	 * target. For a singular field, the caller MUST pass a target that does not already contain the path.
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
				final FLEFRecord parentNode = (StringUtils.isNotEmpty(path)
					? FLEFRecord.createChildWithTag(path).addChild(FLEFRecord.createChildWithTagAndValue(tag, id))
					: FLEFRecord.createChildWithTagAndValue(tag, id)
				);
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
	}

	private void addItem(){
		final List<Class<? extends RecordTypeHandler<?>>> cleaned = extractParentHandlers();
		// Dialog of a record created from within the selection dialog (if any): it tells a newly created record, which
		// must be discarded when the citation is cancelled, from an existing one, which must be kept
		final RecordDialog[] creationDialog = new RecordDialog[1];
		@SuppressWarnings("unchecked")
		final RecordSelectionDialog dialog = RecordSelectionDialog.createWithAllowRecordCreation(parent, model,
			(record, selectedHandler) -> {
				if(type == EntityType.CITATION_WRAPPER){
					final boolean newlyCreated = (creationDialog[0] != null && creationDialog[0].getRecord() == record);

					// The selection dialog works on the cited type: the citation itself is edited by the citation handler
					final RecordTypeHandler<?> citationHandler = Objects.requireNonNullElse(
						findHandler(selectedHandler.getType()), selectedHandler);

					final String selectedRecordId = record.getId();
					final FLEFRecord citation = FLEFRecord.createChildWithTag(getCitationTag(citationHandler));
					FLEFRecordHelper.updateChildValue(citation, path, selectedRecordId);

					final RecordDialog citationDialog = citationHandler.createEditDialog(parent, model, citation);
					citationDialog.setVisible(true);

					if(citationDialog.isSaved()){
						final FLEFRecord newCitationRecord = citationDialog.getRecord();
						setEntity(newCitationRecord);
					}
					else if(newlyCreated)
						model.removeRecord(selectedRecordId);
				}
				else
					setEntity(record);
			},
			cleaned.toArray(Class[]::new));
		dialog.withSetupDialog(createdDialog -> creationDialog[0] = createdDialog);
		dialog.setVisible(true);
	}

	/**
	 * Returns the tag of the citation wrapper: the tag of the current citation, if any, otherwise the type of the
	 * citation handler.
	 */
	private String getCitationTag(final RecordTypeHandler<?> citationHandler){
		return (entityRef != null && StringUtils.isNotEmpty(entityRef.getTag())
			? entityRef.getTag()
			: citationHandler.getType());
	}

	private void editItem(){
		if(!hasData()){
			addItem();

			return;
		}

		final RecordTypeHandler<?> handler = findHandler(entityRef.getTag());
		final RecordDialog dialog = handler.createEditDialog(parent, model, entityRef);
		dialog.setVisible(true);

		if(dialog.isSaved())
			// The record may have changed; refresh the display
			firePropertyChange(PROPERTY_ENTITY_CHANGED, null, null);
	}

	/**
	 * Creates a new cited record (e.g. a place) and adds a citation for it.
	 * <p>
	 * When the field is empty and accepts more than one citation type, the type cannot be decided here, so the
	 * selection dialog is opened instead.
	 */
	private FLEFRecord createNewItem(){
		final RecordTypeHandler<?> handler = (hasData()
			? findHandler(entityRef.getTag())
			: (handlers.size() == 1? handlers.getFirst(): null));
		if(handler == null){
			addItem();

			return null;
		}

		final RecordTypeHandler<?> parentHandler = handler.getParentHandler();
		final RecordDialog dialog = parentHandler.createNewDialog(parent, model);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			final FLEFRecord newRecord = dialog.getRecord();
			if(newRecord != null){
				final String newRecordId = newRecord.getId();
				final FLEFRecord citation = FLEFRecord.createChildWithTag(getCitationTag(handler));
				FLEFRecordHelper.updateChildValue(citation, path, newRecordId);

				final RecordDialog citationDialog = handler.createEditDialog(parent, model, citation);
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
		final RecordDialog dialog = citedRecordHandler.createEditDialog(parent, model, citedRecord);
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
