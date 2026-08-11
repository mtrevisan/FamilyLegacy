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

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.util.List;


/* DONE */
public class EntityReferenceListPanel extends AbstractListPanel<FLEFRecord>{

	private final String path;

	private final RecordTypeHandler<?> handler;

	private String parentEntityId;
	private String parentEntityHandlerType;


	public EntityReferenceListPanel(final String path, final Dialog parent, final String panelTitle,
			final FLEFModel model, final String handlerType){
		super(parent, panelTitle, model);

		this.path = path;

		handler = HandlerRegistry.getHandler(handlerType);
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
				builder.selectionSensitiveItem("Remove", this::removeItem);
			}
		);
	}

	@Override
	protected String getDisplay(final FLEFRecord record){
		if(record != null)
			return handler.getDisplayText(record, model);

		return "--";
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
		final BaseRecordDialog dialog = handler.createNewDialog(parent, model)
			.withParentEntity(parentEntityId, parentEntityHandlerType);
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

	public EntityReferenceListPanel withParentEntity(final String parentEntityId, final String parentEntityHandlerType){
		this.parentEntityId = parentEntityId;
		this.parentEntityHandlerType = parentEntityHandlerType;

		return this;
	}

	public void load(final FLEFRecord record){
		clear();

		if(record == null)
			return;

		final List<FLEFRecord> entities = FLEFRecordHelper.findChildren(record, path);
		setItems(entities);
	}

	public void saveReferences(final FLEFRecord record){
		FLEFRecordHelper.removeChildren(record, path);

		for(final FLEFRecord item : getItems())
			FLEFRecordHelper.addChild(record, path, item.getFormattedId());
	}

	public boolean hasData(){
		return !isEmpty();
	}

}
