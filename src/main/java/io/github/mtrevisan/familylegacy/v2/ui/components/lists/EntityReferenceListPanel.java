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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionTargetHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


/* ONGOING */
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
	private static final String TAG_INDIVIDUAL = "INDIVIDUAL";
	private static final String TAG_GROUP = "GROUP";


	protected enum RelationType{
		RECORD,
		STRUCTURE
	}


	private final String path;
	private final RelationType relationType;

	private final RecordTypeHandler<?> handler;

	private String parentEntityId;
	private String parentEntityHandlerType;


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
	public static EntityReferenceListPanel createForRecord(final String path, final Dialog parent,
			final String panelTitle, final FLEFModel model, final String handlerType){
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
	 * @param handlerType	the type of the entity handler
	 * @return a new EntityReferenceListPanel in structure mode
	 */
	public static EntityReferenceListPanel createForStructure(final String path, final Dialog parent,
			final String panelTitle, final FLEFModel model, final String handlerType){
		return new EntityReferenceListPanel(path, parent, panelTitle, model, handlerType,
			RelationType.STRUCTURE);
	}


	/**
	 * Constructs an EntityReferenceListPanel.
	 *
	 * @param path	the path where references are stored
	 * @param parent	the parent dialog
	 * @param panelTitle	the border title
	 * @param model	the FLEF model
	 * @param handlerType	the type of the entity handler
	 * @param relationType	{@code true} for record mode (store only ID), {@code false} for structure mode
	 */
	protected EntityReferenceListPanel(final String path, final Dialog parent, final String panelTitle,
			final FLEFModel model, final String handlerType, final RelationType relationType){
		super(parent, panelTitle, model);

		this.path = path;
		this.relationType = relationType;

		handler = HandlerRegistry.getHandler(handlerType);
	}

	/**
	 * Sets the parent entity for new records created by this panel.
	 * This is used to automatically link new records to a parent entity.
	 *
	 * @param parentEntityId	the ID of the parent entity
	 * @param parentEntityHandlerType	the handler type of the parent entity
	 * @return this panel instance (for method chaining)
	 */
	public EntityReferenceListPanel withParentEntity(final String parentEntityId, final String parentEntityHandlerType){
		this.parentEntityId = parentEntityId;
		this.parentEntityHandlerType = parentEntityHandlerType;

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
		return (record != null? handler.getDisplayText(record, model): "--");
	}

	@Override
	protected FLEFRecord showAddDialog(){
		final FLEFRecord[] result = {null};
		final MultiTypeSelectionDialog dialog = new MultiTypeSelectionDialog(parent, model,
			handler.getType(),
			(handlerType, selectedRecord) -> result[0] = selectedRecord
		);
		dialog.setVisible(true);

		return result[0];
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		final BaseRecordDialog dialog = handler.createNewDialog(parent, model);
		if(dialog instanceof RelationshipRecordDialog relationshipDialog
				&& relationType == RelationType.RECORD
				&& StringUtils.isNotEmpty(parentEntityId) && StringUtils.isNotEmpty(parentEntityHandlerType))
			relationshipDialog.withSubject(parentEntityId, parentEntityHandlerType);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord record){
		if(record == null){
			JOptionPane.showMessageDialog(parent, handler.getLabel() + " not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final BaseRecordDialog dialog = handler.createEditDialog(parent, model, record);
		if(dialog instanceof RelationshipRecordDialog relationshipDialog
				&& StringUtils.isNotEmpty(parentEntityId) && StringUtils.isNotEmpty(parentEntityHandlerType))
			relationshipDialog.withSubject(parentEntityId, parentEntityHandlerType);
		dialog.setVisible(true);

		// Return the same record (it was updated in place)
		return record;
	}


	//TODO refactor
	/**
	 * Loads entities from the given record.
	 *
	 * @param record	the record containing the entities
	 */
	public void load(final FLEFRecord record){
		clear();

		if(record == null)
			return;

		if(ConclusionTargetHandler.TYPE.equals(handler.getType())){
			final List<FLEFRecord> resolves = FLEFRecordHelper.findChildren(record, path);
			final List<FLEFRecord> entities = new ArrayList<>(resolves.size());
			for(final FLEFRecord resolve : resolves)
				entities.add(resolve.getChildren().getFirst());
			setItems(entities);
		}
		else{
			final List<FLEFRecord> entities = FLEFRecordHelper.findChildren(record, path);
			setItems(entities);
		}
	}

	//TODO refactor
	/**
	 * Loads entities from the given record.
	 *
	 * @param recordId	the record containing the entities
	 */
	public void loadReference(final String recordId){
		clear();

		if(recordId == null)
			return;

		if(ConclusionTargetHandler.TYPE.equals(handler.getType())){
			final List<FLEFRecord> conclusions = model.getRecordsByType(ConclusionHandler.TYPE).stream()
				.filter(conclusion -> {
					final List<FLEFRecord> resolves = FLEFRecordHelper.findChildren(conclusion, TAG_RESOLVES);
					for(final FLEFRecord resolve : resolves){
						final FLEFRecord resolveCitation = resolve.getChildren()
							.getFirst();
						final String resolveTag = resolveCitation.getTag();
						final String resolveXRef = XRefHelper.extractXRef(resolveCitation.getValue());
						if(resolveTag.equals(parentEntityHandlerType) && resolveXRef.equals(recordId))
							return true;
					}
					return false;
				})
				.toList();
			setItems(conclusions);
		}
		else if(IndividualAttributeHandler.TYPE.equals(handler.getType())){
			final List<FLEFRecord> individualAttributes = model.getRecordsByType(IndividualAttributeHandler.TYPE).stream()
				.filter(attribute -> {
					final List<FLEFRecord> individuals = FLEFRecordHelper.findChildren(attribute, TAG_INDIVIDUAL);
					for(final FLEFRecord individual : individuals){
						final String resolveTag = individual.getTag();
						final String resolveXRef = XRefHelper.extractXRef(individual.getValue());
						if(resolveTag.equals(parentEntityHandlerType) && resolveXRef.equals(recordId))
							return true;

						break;
					}
					return false;
				})
				.toList();
			setItems(individualAttributes);
		}
		else if(GroupAttributeHandler.TYPE.equals(handler.getType())){
			final List<FLEFRecord> groupAttributes = model.getRecordsByType(GroupAttributeHandler.TYPE).stream()
				.filter(attribute -> {
					final List<FLEFRecord> groups = FLEFRecordHelper.findChildren(attribute, TAG_GROUP);
					for(final FLEFRecord group : groups){
						final String resolveTag = group.getTag();
						final String resolveXRef = XRefHelper.extractXRef(group.getValue());
						if(resolveTag.equals(parentEntityHandlerType) && resolveXRef.equals(recordId))
							return true;

						break;
					}
					return false;
				})
				.toList();
			setItems(groupAttributes);
		}
		else if(RelationshipHandler.TYPE.equals(handler.getType())){
			final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
			setItems(relationships);
		}
	}

	//TODO refactor
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
		else if(ConclusionTargetHandler.TYPE.equals(handler.getType())){
			final FLEFRecord parentRecord = FLEFRecordHelper.getOrCreateTargetNode(record, TAG_RESOLVES);
			super.save(parentRecord, path);
		}
		else
			super.save(record, path);
	}

}
