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
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.io.Serial;
import java.util.List;


public class ContactListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = 2648904511050688880L;


	private final String path;


	public ContactListPanel(final String path, Dialog parent, FLEFModel model){
		super(parent, "Contact", model);

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
		String address = contact.getValue();
		String type = contact.getTag();
		return (address != null
			? address
			: StringUtils.EMPTY) + (type != null? " (" + type + ")": StringUtils.EMPTY);
	}

	@Override
	protected FLEFRecord showAddDialog(){
		final ContactStructurePanel panel = new ContactStructurePanel(parent, model);
		final JDialog dialog = new JDialog(parent, "Add Contact", Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setLayout(new BorderLayout(10, 10));
		dialog.add(panel, BorderLayout.CENTER);

		final JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		final JButton okBtn = new JButton("OK");
		final JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		dialog.add(btnPanel, BorderLayout.SOUTH);

		final FLEFRecord[] result = {null};
		okBtn.addActionListener(e -> {
			if(panel.validateData()){
				final FLEFRecord contact = panel.saveToRecord(null);
				if(contact != null){
					contact.setTag("CONTACT");
					result[0] = contact;
					dialog.dispose();
				}
			}
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);

		return result[0];
	}

	/**
	 * Creates a new contact and adds it to the list.
	 */
	@Override
	protected FLEFRecord showCreateNewDialog(){
		//TODO
//		final ContactDialog dialog = new ContactDialog(parent, model, null);
//		dialog.setVisible(true);
//
//		return dialog.getRecord();
		return null;
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, "Contact not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final ContactStructurePanel panel = new ContactStructurePanel(parent, model);
		panel.loadFromRecord(existing);

		final JDialog dialog = new JDialog(parent, "Edit Contact", Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setLayout(new BorderLayout(10, 10));
		dialog.add(panel, BorderLayout.CENTER);

		final JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		final JButton okBtn = new JButton("OK");
		final JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		dialog.add(btnPanel, BorderLayout.SOUTH);

		final FLEFRecord[] result = {null};
		okBtn.addActionListener(e -> {
			if(panel.validateData()){
				final FLEFRecord contact = panel.saveToRecord(existing);
				if(contact != null){
					contact.setTag("CONTACT");
					result[0] = contact;
					dialog.dispose();
				}
			}
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);

		return result[0];
	}

	public void load(final FLEFRecord record){
		clear();

		if(record == null)
			return;

		final List<FLEFRecord> contacts = FLEFRecordHelper.findChildren(record, "CONTACT");
		setItems(contacts);
	}

	public void save(final FLEFRecord record){
		super.save(record, path);
	}

	public boolean hasData(){
		return !isEmpty();
	}

}
