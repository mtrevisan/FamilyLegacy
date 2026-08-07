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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GroupAttributeRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/* DONE */
/**
 * Panel for managing a list of {@code GROUP_ATTRIBUTE} references according to FLEF 0.1.1.
 */
public class GroupAttributeListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = -7728901182938867416L;


	private static final String TAG_GROUP_ATTRIBUTE = "GROUP_ATTRIBUTE";


	static{
		HandlerRegistry.register(new GroupAttributeHandler());
	}


	private final String path;

	private final RecordTypeHandler<?> groupAttributeHandler = HandlerRegistry.getHandler(GroupAttributeHandler.TYPE);


	/**
	 * Constructs a GroupAttributeListPanel without a border.
	 *
	 * @param parent the parent dialog
	 * @param model        the FLEF model
	 */
	public GroupAttributeListPanel(final String path, final Dialog parent, final FLEFModel model){
		this(path, parent, "Group Attributes", model);
	}

	/**
	 * Constructs a GroupAttributeListPanel with a titled border.
	 *
	 * @param parent the parent dialog
	 * @param borderTitle  the border title, or {@code null} for no border
	 * @param model        the FLEF model
	 */
	public GroupAttributeListPanel(final String path, final Dialog parent, final String borderTitle,
			final FLEFModel model){
		super(parent, borderTitle, model);

		this.path = path;
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
			});
	}

	@Override
	protected String getDisplay(final FLEFRecord groupAttribute){
		if(groupAttribute != null)
			return groupAttributeHandler.getDisplayText(groupAttribute, model);
		return "--";
	}

	@Override
	protected FLEFRecord showAddDialog(){
		final FLEFRecord[] result = {null};
		final GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parent, model, groupAttributeHandler, selectedItem -> result[0] = selectedItem
		);
		dialog.setVisible(true);

		return result[0];
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		final GroupAttributeRecordDialog newGroupAttributeDialog = (GroupAttributeRecordDialog)groupAttributeHandler.createNewDialog(parent, model);
		newGroupAttributeDialog.setVisible(true);

		FLEFRecord newGroupAttribute = null;
		if(newGroupAttributeDialog.isSaved())
			newGroupAttribute = newGroupAttributeDialog.getRecord();
		return newGroupAttribute;
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, "Source Citation not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final JDialog dialog = GroupAttributeRecordDialog.createEdit(parent, model, existing);
		dialog.setVisible(true);

		// Return the same record (it was updated in place)
		return existing;
	}

	public void load(final FLEFRecord record){
		clear();

		if(record == null)
			return;

		final List<FLEFRecord> groupAttributes = FLEFRecordHelper.findChildren(record, path);
		final List<FLEFRecord> sources = new ArrayList<>();
		for(final FLEFRecord groupAttribute : groupAttributes){
			final String sourceId = findRecordGroupAttributeId(groupAttribute);
			if(sourceId != null){
				final FLEFRecord source = model.getRecordById(sourceId);
				sources.add(source);
			}
		}
		setItems(sources);
	}

	public String findRecordGroupAttributeId(final FLEFRecord groupAttribute){
		String id = null;
		for(final FLEFRecord child : groupAttribute.getChildren())
			if(TAG_GROUP_ATTRIBUTE.equals(child.getTag()))
				id = XRefHelper.extractXRef(child.getValue());
		return id;
	}

	public void save(final FLEFRecord record){
		FLEFRecordHelper.removeChildren(record, path);

		for(final FLEFRecord attribute : getItems())
			FLEFRecordHelper.addChild(record, path, attribute.getFormattedId());
	}

}
