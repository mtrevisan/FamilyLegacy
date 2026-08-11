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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.util.List;
import java.util.function.Consumer;


/* DONE */
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

	protected final String path;
	private final boolean isRecord;

	private final RecordTypeHandler<?> handler;

	private String parentEntityId;
	private String parentEntityHandlerType;


	/**
	 * Creates a panel in record mode (stores only the ID of the referenced entity).
	 *
	 * @param path         the path where references are stored
	 * @param parent       the parent dialog
	 * @param panelTitle   the border title
	 * @param model        the FLEF model
	 * @param handlerType  the type of the entity handler
	 * @return a new EntityReferenceListPanel in record mode
	 */
	public static EntityReferenceListPanel createForRecord(final String path, final Dialog parent,
			final String panelTitle, final FLEFModel model, final String handlerType){
		return new EntityReferenceListPanel(path, parent, panelTitle, model, handlerType, true);
	}

	/**
	 * Creates a panel in structure mode (stores the entire entity record).
	 *
	 * @param path         the path where references are stored
	 * @param parent       the parent dialog
	 * @param panelTitle   the border title
	 * @param model        the FLEF model
	 * @param handlerType  the type of the entity handler
	 * @return a new EntityReferenceListPanel in structure mode
	 */
	public static EntityReferenceListPanel createForStructure(final String path, final Dialog parent,
			final String panelTitle, final FLEFModel model, final String handlerType){
		return new EntityReferenceListPanel(path, parent, panelTitle, model, handlerType, false);
	}


	/**
	 * Constructs an EntityReferenceListPanel.
	 *
	 * @param path         the path where references are stored
	 * @param parent       the parent dialog
	 * @param panelTitle   the border title
	 * @param model        the FLEF model
	 * @param handlerType  the type of the entity handler
	 * @param isRecord     {@code true} for record mode (store only ID), {@code false} for structure mode
	 */
	protected EntityReferenceListPanel(final String path, final Dialog parent, final String panelTitle,
			final FLEFModel model, final String handlerType, final boolean isRecord){
		super(parent, panelTitle, model);

		this.path = path;
		this.isRecord = isRecord;

		handler = HandlerRegistry.getHandler(handlerType);
	}

	/**
	 * Sets the parent entity for new records created by this panel.
	 * This is used to automatically link new records to a parent entity.
	 *
	 * @param parentEntityId        the ID of the parent entity
	 * @param parentEntityHandlerType the handler type of the parent entity
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

		final Consumer<GUIHelper.MenuBuilder> menuItems = (isRecord
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
			builder.item("Create New...", this::createNewItem);
			builder.item("Add Existing...", this::addItem);
			builder.separator();
			builder.selectionSensitiveItem("Edit...", this::editItem);
			builder.selectionSensitiveItem("Remove", this::removeItem);
		};
	}

	private Consumer<GUIHelper.MenuBuilder> createMenuItemsForStructure(){
		final Consumer<GUIHelper.MenuBuilder> menuItems;
		menuItems = builder -> {
			builder.item("Create New...", this::createNewItem);
			builder.separator();
			builder.selectionSensitiveItem("Edit...", this::editItem);
			builder.selectionSensitiveItem("Remove", this::removeItem);
		};
		return menuItems;
	}

	@Override
	protected String getDisplay(final FLEFRecord record){
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
		if(isRecord && StringUtils.isNotEmpty(parentEntityId) && StringUtils.isNotEmpty(parentEntityHandlerType))
			dialog.withParentEntity(parentEntityId, parentEntityHandlerType);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, handler.getLabel() + " not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final JDialog dialog = handler.createEditDialog(parent, model, existing);
		dialog.setVisible(true);

		// Return the same record (it was updated in place)
		return existing;
	}


	/**
	 * Loads entities from the given record.
	 *
	 * @param record the record containing the entities
	 */
	public void load(final FLEFRecord record){
		clear();

		if(record == null)
			return;

		final List<FLEFRecord> entities = FLEFRecordHelper.findChildren(record, path);
		setItems(entities);
	}

	/**
	 * Saves the current entities to the given record.
	 * <p>
	 * In record mode, saves only the IDs of the entities.
	 * In structure mode, saves the entire entity records.
	 *
	 * @param record the record to save to
	 */
	public void save(final FLEFRecord record){
		if(isRecord){
			FLEFRecordHelper.removeChildren(record, path);

			for(final FLEFRecord item : getItems())
				FLEFRecordHelper.addChild(record, path, item.getFormattedId());
		}
		else
			super.save(record, path);
	}

	public boolean hasData(){
		return !isEmpty();
	}

}
