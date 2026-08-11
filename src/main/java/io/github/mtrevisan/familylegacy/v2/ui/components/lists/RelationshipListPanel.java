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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.RelationshipRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.List;


/* ONGOING set withParentEntity on subject */
/**
 * Panel for managing a list of relationships.
 */
public class RelationshipListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = 8165048140355496463L;


	private static final String TAG_RELATIONSHIP = "RELATIONSHIP";


	static{
		HandlerRegistry.register(new RelationshipHandler());
	}


	private final String path;

	private String subjectEntityId;
	private String subjectEntityHandlerType;

	private final RecordTypeHandler<?> relationshipHandler = HandlerRegistry.getHandler(RelationshipHandler.TYPE);


	public RelationshipListPanel(final String path, final Dialog parent, final String panelTitle, final FLEFModel model){
		super(parent, panelTitle, model);

		this.path = path;
	}

	/**
	 * Sets the subject entity for new records created by this panel.
	 * This is used to automatically link new records to a subject entity.
	 *
	 * @param subjectEntityId        the ID of the subject entity
	 * @param subjectEntityHandlerType the handler type of the subject entity
	 * @return this panel instance (for method chaining)
	 */
	public RelationshipListPanel withSubject(final String subjectEntityId, final String subjectEntityHandlerType){
		this.subjectEntityId = subjectEntityId;
		this.subjectEntityHandlerType = subjectEntityHandlerType;

		return this;
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
	protected String getDisplay(final FLEFRecord relationship){
		if(relationship != null)
			return relationshipHandler.getDisplayText(relationship, model);

		return "--";
	}

	@Override
	protected FLEFRecord showAddDialog(){
		final FLEFRecord[] result = {null};
		final MultiTypeSelectionDialog dialog = new MultiTypeSelectionDialog(parent, model,
			RelationshipHandler.TYPE,
			(handlerType, selectedRecord) -> result[0] = selectedRecord
		);
		dialog.setVisible(true);

		return result[0];
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		final BaseRecordDialog dialog = relationshipHandler.createNewDialog(parent, model);
		if(StringUtils.isNotEmpty(subjectEntityId) && StringUtils.isNotEmpty(subjectEntityHandlerType))
			dialog.withParentEntity(subjectEntityId, subjectEntityHandlerType);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, relationshipHandler.getLabel() + " not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final RelationshipRecordDialog dialog = (RelationshipRecordDialog)relationshipHandler.createEditDialog(parent, model, existing);
		dialog.setVisible(true);

		// Return the same record (it was updated in place)
		return existing;
	}

	public void load(final FLEFRecord record){
		clear();

		if(record == null)
			return;

		final List<FLEFRecord> relationships = FLEFRecordHelper.findChildren(record, TAG_RELATIONSHIP);
		setItems(relationships);
	}

	public void save(final FLEFRecord record){
		super.save(record, path);
	}

	public boolean hasData(){
		return !isEmpty();
	}

}
