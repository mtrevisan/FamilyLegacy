package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.Serial;


public class ContactListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = 2648904511050688880L;


	public ContactListPanel(FLEFModel model, Dialog parentDialog){
		super(parentDialog, "Contact", model);
	}

	@Override
	protected String getDisplay(FLEFRecord contact){
		String address = contact.getValue();
		String type = contact.getTag();
		return (address != null ? address : StringUtils.EMPTY) + (type != null ? " (" + type + ")" : StringUtils.EMPTY);
	}

	@Override
	protected FLEFRecord showAddDialog(){
		final ContactStructurePanel panel = new ContactStructurePanel(model, parentDialog);
		final JDialog dialog = new JDialog(parentDialog, "Add Contact", Dialog.ModalityType.APPLICATION_MODAL);
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
			if(panel.validateRequiredFields()){
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
		dialog.setMinimumSize(new Dimension(500, 450));
		dialog.setLocationRelativeTo(parentDialog);
		dialog.setVisible(true);

		return result[0];
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord item){
		if(item == null){
			return null;
		}

		final ContactStructurePanel panel = new ContactStructurePanel(model, parentDialog);
		panel.loadFromRecord(item);

		final JDialog dialog = new JDialog(parentDialog, "Edit Contact", Dialog.ModalityType.APPLICATION_MODAL);
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
			if(panel.validateRequiredFields()){
				final FLEFRecord contact = panel.saveToRecord(item);
				if(contact != null){
					contact.setTag("CONTACT");
					result[0] = contact;
					dialog.dispose();
				}
			}
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setMinimumSize(new Dimension(500, 450));
		dialog.setLocationRelativeTo(parentDialog);
		dialog.setVisible(true);

		return result[0];
	}

}
