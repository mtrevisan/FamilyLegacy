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
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
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


	private final String path;
	private final RelationType relationType;

	private final RecordTypeHandler<?> handler;

	private String parentEntityId;
	private String parentEntityPath;


	/**
	 * Creates a panel in record mode (stores only the ID of the referenced entity).
	 *
	 * @param path	the path where references are stored
	 * @param parent	the parent dialog
	 * @param panelTitle	the border title
	 * @param model	the FLEF model
	 * @param handlerType	the type of the entity handler
	 * @return a new EntityReferenceListPanel in record mode
	 */
	public static <T extends Class<? extends RecordTypeHandler<?>>> EntityReferenceListPanel createForRecord(
			final String path, final Dialog parent, final String panelTitle, final FLEFModel model,
			final T handlerType){
		return new EntityReferenceListPanel(path, parent, panelTitle, model, handlerType,
			RelationType.RECORD);
	}

	/**
	 * Creates a panel in structure mode (stores the entire entity record).
	 *
	 * @param path	the path where references are stored
	 * @param parent	the parent dialog
	 * @param panelTitle	the border title
	 * @param model	the FLEF model
	 * @param handlerClass	the type of the entity handler
	 * @return a new EntityReferenceListPanel in structure mode
	 */
	public static <T extends Class<? extends RecordTypeHandler<?>>> EntityReferenceListPanel createForStructure(
			final String path, final Dialog parent, final String panelTitle, final FLEFModel model,
			final T handlerClass){
		return new EntityReferenceListPanel(path, parent, panelTitle, model, handlerClass,
			RelationType.STRUCTURE);
	}


	/**
	 * Constructs an EntityReferenceListPanel.
	 *
	 * @param path	the path where references are stored
	 * @param parent	the parent dialog
	 * @param panelTitle	the border title
	 * @param model	the FLEF model
	 * @param handlerClass	the type of the entity handler
	 * @param relationType	{@code true} for record mode (store only ID), {@code false} for structure mode
	 */
	protected <T extends Class<? extends RecordTypeHandler<?>>> EntityReferenceListPanel(final String path,
			final Dialog parent, final String panelTitle, final FLEFModel model, final T handlerClass,
			final RelationType relationType){
		super(parent, panelTitle, model);

		this.path = path;
		this.relationType = relationType;
		this.handler = HandlerRegistry.getHandler(handlerClass);
	}

	/**
	 * Sets the parent entity for new records created by this panel.
	 * This is used to automatically link new records to a parent entity.
	 *
	 * @param parentEntityId	the ID of the parent entity
	 * @param parentEntityPath	the handler type of the parent entity
	 * @return this panel instance (for method chaining)
	 */
	public EntityReferenceListPanel withParentEntity(final String parentEntityId, final String parentEntityPath){
		this.parentEntityId = parentEntityId;
		this.parentEntityPath = parentEntityPath;

		return this;
	}


	@Override
	protected void initComponents(){
		super.initComponents();

		final Consumer<GUIHelper.MenuBuilder> menuItems = (relationType == RelationType.RECORD
			? createMenuItemsForRecord()
			: createMenuItemsForStructure());
		GUIHelper.installBehavior(list,
			this::editItem,
			this::createNewItem,
			this::removeItem,
			menuItems
		);
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

	@Override
	protected String getDisplayText(final FLEFRecord record){
		if(record != null)
			return handler.getDisplayText(record, model);

		return "--";
	}

	@Override
	protected FLEFRecord showAddDialog(){
		final FLEFRecord[] result = {null};
		@SuppressWarnings("unchecked")
		final MultiTypeSelectionDialog dialog = new MultiTypeSelectionDialog(parent, model,
			(Class<? extends RecordTypeHandler<?>>)handler.getClass());
		dialog.addPropertyChangeListener(MultiTypeSelectionDialog.PROPERTY_TYPE_SELECTED, e -> {
			final FLEFRecord selectedRecord = dialog.getSelectedRecord();
			result[0] = selectedRecord;
		});
		dialog.setVisible(true);

		return result[0];
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		final BaseRecordDialog dialog = handler.createNewDialog(parent, model);
		if(dialog instanceof RelationshipRecordDialog relationshipDialog
				&& relationType == RelationType.RECORD
				&& StringUtils.isNotEmpty(parentEntityId) && StringUtils.isNotEmpty(parentEntityPath))
			relationshipDialog.withSubject(parentEntityId, parentEntityPath);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	@Override
	protected FLEFRecord showEditDialog(FLEFRecord record){
		if(record == null){
			JOptionPane.showMessageDialog(parent, handler.getLabel() + " not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final String xref = record.getValue();
		if(XRefHelper.isReference(xref))
			record  = model.getRecordById(xref);

		final BaseRecordDialog dialog = handler.createEditDialog(parent, model, record);
		if(dialog instanceof RelationshipRecordDialog relationshipDialog
				&& StringUtils.isNotEmpty(parentEntityId) && StringUtils.isNotEmpty(parentEntityPath))
			relationshipDialog.withSubject(parentEntityId, parentEntityPath);
		dialog.setVisible(true);

		// Return the same record (it was updated in place)
		return record;
	}


	/**
	 * Loads entities from the given record.
	 *
	 * @param record	the record containing the entities
	 */
	public void load(final FLEFRecord record){
		clear();

		final List<FLEFRecord> entities = handler.extractEntities(record, path);
		setItems(entities);
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

		final List<FLEFRecord> references = handler.findReferences(model, recordId, parentEntityPath);
		setItems(references);
	}

	public void loadReferenceWithType(final String recordId, final String... actorTags){
		clear();

		if(recordId == null)
			return;

		final List<FLEFRecord> references = model.getRecordsByType(handler.getType()).stream()
			.filter(reference -> {
				final List<FLEFRecord> actors = FLEFRecordHelper.findChildren(reference, actorTags);
				for(final FLEFRecord actor : actors){
					final FLEFRecord entity = actor.getTheOnlyChild();
					final FLEFRecord parentRecord = (entity != null? entity: actor);
					final String tag = parentRecord.getTag();
					final String value = parentRecord.getValue();
					return (Objects.equals(parentEntityPath, tag) && recordId.equals(XRefHelper.extractXRef(value)));
				}
				return false;
			})
			.toList();
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
		if(relationType == RelationType.RECORD){
			FLEFRecordHelper.removeChildren(record, path);

			for(final FLEFRecord item : getItems())
				FLEFRecordHelper.addChild(record, path, item.getFormattedId());
		}
		else if(ConclusionTargetHandler.class.equals(handler.getClass())){
			final FLEFRecord parentRecord = FLEFRecordHelper.getOrCreateTargetNode(record, TAG_RESOLVES);
			super.save(parentRecord, path);
		}
		else
			super.save(record, path);
	}

	public void saveReferences(final FLEFRecord record){
		FLEFRecordHelper.removeChildren(record, path);

		for(final FLEFRecord item : getItems())
			FLEFRecordHelper.addChild(record, path, item.getFormattedId());
	}

}
