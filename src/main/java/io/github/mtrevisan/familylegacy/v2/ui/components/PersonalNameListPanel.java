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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.PersonalNameStructureDialog;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.List;


public class PersonalNameListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = 5393675791860301264L;


	private static final String TAG_PART = "PART";
	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_TYPE = "TYPE";


	private final String path;


	public PersonalNameListPanel(final String path, final Dialog parent, final FLEFModel model){
		super(parent, "Personal Names*", model);

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
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			});
	}

	@Override
	protected String getDisplay(final FLEFRecord personalName){
		if(personalName == null)
			return "--";

		final List<FLEFRecord> parts = FLEFRecordHelper.findChildren(personalName, TAG_PART);
		final StringBuilder fullName = new StringBuilder();

		for(final FLEFRecord part : parts){
			final String val = FLEFRecordHelper.getChildValue(part, TAG_VALUE);
			if(val != null && !val.isBlank()){
				if(!fullName.isEmpty())
					fullName.append(StringUtils.SPACE);
				fullName.append(val.trim());
			}
		}

		String result = fullName.toString();
		if(result.isBlank())
			return "--";

		if(result.length() > 50)
			result = result.substring(0, 50) + "...";
		final String type = FLEFRecordHelper.getChildValue(personalName, TAG_TYPE);
		if(type != null && !type.isBlank())
			result += " (" + type + ")";

		return result;
	}

	@Override
	protected FLEFRecord showAddDialog(){
		return null;
	}

	/**
	 * Creates a new personal name and adds it to the list.
	 */
	@Override
	protected FLEFRecord showCreateNewDialog(){
		final PersonalNameStructureDialog dialog = PersonalNameStructureDialog.createNew(parent, model);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, "Personal Name not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final PersonalNameStructureDialog dialog = PersonalNameStructureDialog.createEdit(parent, model, existing);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	public void load(final FLEFRecord record){
		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(record, path);
		setItems(names);
	}

	public void save(final FLEFRecord record){
		FLEFRecordHelper.removeChildren(record, path);

		for(final FLEFRecord name : getItems()){
			final FLEFRecord child = FLEFRecordHelper.getOrCreateTargetNode(record, path);
			for(final FLEFRecord grandchild : name.getChildren())
				child.addChild(grandchild);
		}
	}

	public boolean hasData(){
		return !isEmpty();
	}

}
