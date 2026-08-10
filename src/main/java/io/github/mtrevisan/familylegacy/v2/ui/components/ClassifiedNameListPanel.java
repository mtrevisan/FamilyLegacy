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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.ClassifiedNameDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ClassifiedNameHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.List;


/* DONE */
/**
 * According to FLEF 0.1.1.
 * Structure:
 * <pre>
 * name+: ClassifiedName
 * </pre>
 * and
 * <pre>
 * name*: ClassifiedName
 * </pre>
 */
public class ClassifiedNameListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = -922034547054981789L;


	private static final String TAG_TYPE = "TYPE";


	static{
		HandlerRegistry.register(new ClassifiedNameHandler());
	}


	private final String path;


	public ClassifiedNameListPanel(final String path, final Dialog parent, final String panelTitle,
			final FLEFModel model){
		super(parent, panelTitle, model);

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
	protected String getDisplay(final FLEFRecord name){
		String value = FLEFRecordHelper.getChildValue(name, ClassifiedNameDialog.TAG_VALUE);
		if(value != null && !value.isEmpty()){
			// Truncate long names
			if(value.length() > 50)
				value = value.substring(0, 50) + "...";

			final String type = FLEFRecordHelper.getChildValue(name, TAG_TYPE);
			if(type != null && !type.isEmpty())
				value += " (" +  type + ")";

			return value;
		}
		return "--";
	}

	@Override
	protected FLEFRecord showAddDialog(){
		return null;
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		final RecordTypeHandler<?> classifiedNameHandler = HandlerRegistry.getHandler(ClassifiedNameHandler.TYPE);
		final ClassifiedNameDialog dialog = (ClassifiedNameDialog)classifiedNameHandler.createNewDialog(parent, model);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, "Name not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final RecordTypeHandler<?> classifiedNameHandler = HandlerRegistry.getHandler(ClassifiedNameHandler.TYPE);
		final ClassifiedNameDialog dialog = (ClassifiedNameDialog)classifiedNameHandler.createEditDialog(parent, model, existing);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	public void load(final FLEFRecord record){
		clear();

		if(record == null)
			return;

		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(record, path);
		setItems(names);
	}

	public void save(final FLEFRecord record){
		super.save(record, path);
	}

	public boolean hasData(){
		return !isEmpty();
	}

}
