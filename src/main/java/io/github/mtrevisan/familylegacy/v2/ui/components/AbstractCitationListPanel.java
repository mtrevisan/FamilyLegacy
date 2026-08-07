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
package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/**
 * Abstract panel managing citation lists for specific entity record types.
 */
public abstract class AbstractCitationListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = 7700117494674811261L;


	private final String path;
	private final String tag;

	private final RecordTypeHandler<?> targetHandler;


	/**
	 * Constructs an AbstractCitationListPanel with panel configurations.
	 *
	 * @param path        the record path
	 * @param parent      the parent dialog
	 * @param borderTitle the border title
	 * @param model       the FLEF model
	 * @param tag         the tag identifier
	 * @param handlerType the type of the target handler
	 */
	public AbstractCitationListPanel(final String path, final Dialog parent, final String borderTitle,
			final FLEFModel model, final String tag, final String handlerType){
		super(parent, borderTitle, model);

		this.path = path;
		this.tag = tag;

		targetHandler = HandlerRegistry.getHandler(handlerType);
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
				builder.selectionSensitiveItem("Edit...", this::editTargetEntity);
				builder.selectionSensitiveItem("Edit Citation...", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			});
	}

	@Override
	protected String getDisplay(final FLEFRecord citation){
		final String entityId = findTargetEntityId(citation);
		if(entityId != null){
			final FLEFRecord entity = model.getRecordById(entityId);
			if(entity != null)
				return targetHandler.getDisplayText(entity, model);
			return entityId;
		}
		return "--";
	}

	public String findTargetEntityId(final FLEFRecord citation){
		if(citation == null)
			return null;

		for(final FLEFRecord child : citation.getChildren())
			if(tag.equals(child.getTag()))
				return XRefHelper.extractXRef(child.getValue());
		return null;
	}

	@Override
	protected FLEFRecord showAddDialog(){
		final FLEFRecord[] result = {null};
		final GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parent, model, targetHandler, selectedItem -> {
				final String selectedId = selectedItem.getValue();
				final FLEFRecord citation = model.getRecordById(selectedId);
				if(citation != null && !items.contains(citation)){
					final String entityId = findTargetEntityId(citation);
					final FLEFRecord entity = model.getRecordById(entityId);
					if(entity != null)
						result[0] = entity;
				}
			}
		);
		dialog.setVisible(true);
		return result[0];
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		final JDialog newEntityDialog = targetHandler.createNewDialog(parent, model);
		newEntityDialog.setVisible(true);

		FLEFRecord newCitation = null;
		if(isDialogSaved(newEntityDialog)){
			final FLEFRecord newEntity = getRecordFromDialog(newEntityDialog);
			if(newEntity != null){
				final String newEntityId = newEntity.getId();
				final FLEFRecord citation = FLEFRecord.createEmpty();
				FLEFRecordHelper.updateChildValue(citation, tag, XRefHelper.formatXRef(newEntityId));

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
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, targetHandler.getLabel() + " Citation not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final JDialog dialog = createCitationEditDialog(existing);
		dialog.setVisible(true);
		return existing;
	}

	public final void editTargetEntity(){
		final int idx = list.getSelectedIndex();
		if(idx == -1)
			return;

		final FLEFRecord citation = items.get(idx);
		final String entityId = findTargetEntityId(citation);
		if(entityId != null){
			final FLEFRecord entity = model.getRecordById(entityId);
			final JDialog dialog = createTargetEditDialog(entity);
			dialog.setVisible(true);

			if(isDialogSaved(dialog))
				listModel.setElementAt(getDisplay(citation), idx);
		}
	}

	public void load(final FLEFRecord record){
		clear();

		if(record == null)
			return;

		final List<FLEFRecord> citations = FLEFRecordHelper.findChildren(record, path);
		final List<FLEFRecord> entities = new ArrayList<>();
		for(final FLEFRecord citation : citations){
			final String entityId = findTargetEntityId(citation);
			if(entityId != null){
				final FLEFRecord entity = model.getRecordById(entityId);
				if(entity != null)
					entities.add(entity);
			}
		}
		setItems(entities);
	}

	public void save(final FLEFRecord record){
		super.save(record, path);
	}

	protected abstract boolean isDialogSaved(JDialog dialog);

	protected abstract FLEFRecord getRecordFromDialog(JDialog dialog);

	protected abstract JDialog createCitationEditDialog(FLEFRecord citation);

	protected abstract JDialog createTargetEditDialog(FLEFRecord entity);

}
