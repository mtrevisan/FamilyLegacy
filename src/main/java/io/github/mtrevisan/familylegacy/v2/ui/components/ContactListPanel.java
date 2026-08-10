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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.ContactStructureDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.List;


public class ContactListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = 2648904511050688880L;


	static{
		HandlerRegistry.register(new ContactHandler());
	}


	private final String path;

	private final RecordTypeHandler<?> contactHandler = HandlerRegistry.getHandler(ContactHandler.TYPE);


	public ContactListPanel(final String path, final Dialog parent, final FLEFModel model){
		this(path, parent, "Contacts", model);
	}

	public ContactListPanel(final String path, final Dialog parent, final String borderTitle, final FLEFModel model){
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
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			});
	}

	@Override
	protected String getDisplay(FLEFRecord contact){
		if(contact != null)
			return contactHandler.getDisplayText(contact, model);

		return "--";
	}

	@Override
	protected FLEFRecord showAddDialog(){
		return null;
	}

	/**
	 * Creates a new contact and adds it to the list.
	 */
	@Override
	protected FLEFRecord showCreateNewDialog(){
		final RecordTypeHandler<?> contactHandler = HandlerRegistry.getHandler(ContactHandler.TYPE);
		final ContactStructureDialog dialog = (ContactStructureDialog)contactHandler.createNewDialog(parent, model);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, "Contact not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final RecordTypeHandler<?> contactHandler = HandlerRegistry.getHandler(ContactHandler.TYPE);
		final ContactStructureDialog dialog = (ContactStructureDialog)contactHandler.createEditDialog(parent, model, existing);
		dialog.setVisible(true);

		// Return the same record (it was updated in place)
		return existing;
	}

	public void load(final FLEFRecord record){
		clear();

		if(record == null)
			return;

		final List<FLEFRecord> contacts = FLEFRecordHelper.findChildren(record, path);
		setItems(contacts);
	}

	public void save(final FLEFRecord record){
		super.save(record, path);
	}

	public boolean hasData(){
		return !isEmpty();
	}

}
