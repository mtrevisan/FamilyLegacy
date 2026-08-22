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
import java.io.Serial;
import java.util.List;


/**
 * Panel for managing citation lists for specific entity record types.
 * <p>
 * This panel handles citations that reference an entity (e.g., SOURCE or REPOSITORY)
 * with additional citation metadata. It supports:
 * <ul>
 *   <li>Creating a new entity and its citation together</li>
 *   <li>Adding an existing citation</li>
 *   <li>Editing the citation metadata</li>
 *   <li>Editing the target entity</li>
 *   <li>Removing a citation</li>
 * </ul>
 */
public class EntityCitationListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = -764509672344287269L;


	private final String path;

	private final RecordTypeHandler<?> recordHandler;


	/**
	 * Constructs a CitationListPanel with all necessary behavior.
	 *
	 * @param path	the record path where citations are stored
	 * @param parent	the parent dialog
	 * @param panelTitle	the border title
	 * @param model	the FLEF model
	 * @param recordHandlerType	the type of the target entity handler
	 */
	public EntityCitationListPanel(final String path, final Dialog parent,
			final String panelTitle, final FLEFModel model, final Class<? extends RecordTypeHandler<?>> recordHandlerType){
		super(parent, panelTitle, model);

		this.path = path;

		recordHandler = HandlerRegistry.getHandler(recordHandlerType);
	}


	@Override
	protected void initComponents(){
		super.initComponents();

		GUIHelper.installBehavior(list,
			this::editItem, this::editTargetItem,
			this::createNewItem, this::removeItem,
			builder -> {
				builder.item("Create New…", this::createNewItem);
				builder.item("Add Existing…", this::addItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit Record…", this::editTargetItem);
				builder.selectionSensitiveItem("Edit Citation…", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			}
		);
	}


	@Override
	protected String getDisplayText(final FLEFRecord record){
		return recordHandler.getDisplayText(record, model);
	}

	@Override
	protected FLEFRecord showAddDialog(){
		final FLEFRecord[] result = {null};
		final MultiTypeSelectionDialog dialog = new MultiTypeSelectionDialog(parent, model, null,
			recordHandler.getHandlerClass());
		dialog.addPropertyChangeListener(MultiTypeSelectionDialog.PROPERTY_TYPE_SELECTED, e -> {
			final FLEFRecord selectedRecord = dialog.getSelectedRecord();
			final String selectedId = selectedRecord.getValue();
			final FLEFRecord citation = model.getRecordById(selectedId);
			if(citation != null && !items.contains(citation)){
				final String entityId = findTargetEntityId(citation);
				final FLEFRecord entity = model.getRecordById(entityId);
				if(entity != null)
					result[0] = entity;
			}
		});
		dialog.setVisible(true);

		return result[0];
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		final RecordTypeHandler<?> parentRecordHandler = recordHandler.getParentHandler();
		final JDialog newEntityDialog = parentRecordHandler.createNewDialog(parent, model);
		newEntityDialog.setVisible(true);

		FLEFRecord newCitation = null;
		if(isDialogSaved(newEntityDialog)){
			final FLEFRecord newEntity = getRecordFromDialog(newEntityDialog);
			if(newEntity != null){
				final String newEntityId = newEntity.getId();
				final FLEFRecord citation = FLEFRecord.createEmpty();
				FLEFRecordHelper.updateChildValue(citation, parentRecordHandler.getType(), newEntityId);

				final JDialog citationDialog = createCitationEditDialog(citation);
				citationDialog.setVisible(true);

				if(isDialogSaved(citationDialog))
					newCitation = getRecordFromDialog(citationDialog);
				else
					model.removeRecord(newEntityId);
			}
		}
		return newCitation;
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord record){
		if(record == null){
			JOptionPane.showMessageDialog(parent, recordHandler.getLabel() + " not found",
				"Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final JDialog dialog = createCitationEditDialog(record);
		dialog.setVisible(true);

		return record;
	}

	public final void editTargetItem(){
		final int idx = list.getSelectedIndex();
		if(idx == -1)
			return;

		final FLEFRecord citation = items.get(idx);
		final RecordTypeHandler<?> parentRecordHandler = recordHandler.getParentHandler();
		final String recordId = FLEFRecordHelper.getChildValue(citation, parentRecordHandler.getType());
		final FLEFRecord entity = model.getRecordById(recordId);
		final JDialog dialog = createTargetEditDialog(entity);
		dialog.setVisible(true);

		if(isDialogSaved(dialog))
			listModel.setElementAt(getDisplayText(entity), idx);
	}

	public void load(final FLEFRecord record){
		clear();

		if(record == null || record.isEmpty())
			return;

		final List<FLEFRecord> citations = FLEFRecordHelper.extractStructuresWithReference(record, path);
		setItems(citations);
	}

	/**
	 * Finds the target entity ID referenced by a citation record.
	 *
	 * @param citation	the citation record
	 * @return the entity ID, or {@code null} if not found
	 */
	private String findTargetEntityId(final FLEFRecord citation){
		if(citation == null)
			return null;

		for(final FLEFRecord child : citation.getChildren())
			if(recordHandler.getType().equals(child.getTag()))
				return child.getValue();
		return null;
	}

	public void save(final FLEFRecord record){
		super.save(record, path);
	}

	public boolean isDialogSaved(final JDialog dialog){
		return ((BaseRecordDialog)dialog).isSaved();
	}

	public FLEFRecord getRecordFromDialog(final JDialog dialog){
		return ((BaseRecordDialog)dialog).getRecord();
	}

	public JDialog createCitationEditDialog(final FLEFRecord citation){
		return recordHandler.createEditDialog(parent, model, citation);
	}

	public JDialog createTargetEditDialog(final FLEFRecord entity){
		final RecordTypeHandler<?> parentRecordHandler = recordHandler.getParentHandler();
		return parentRecordHandler.createEditDialog(parent, model, entity);
	}

}
