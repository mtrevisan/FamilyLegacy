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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.BasicNoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ContactListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;


/* ONGOING */
/**
 * Dialog for editing the {@code HEADER} singleton structure according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * struct Header {
 *   protocol: struct {
 *     name: Text
 *     version: SemVer
 *   }
 *   source: struct {
 *     system_id: Text
 *     name?: Text
 *     version?: SemVer
 *     corporate?: Text
 *   }
 *   date: Date
 *   copyright?: Text
 *   submitter?: struct {
 *     name: Text
 *     contact*: ContactStructure
 *     note*: Text
 *   }
 *   scope?: Text
 * }
 * </pre>
 */
public class HeaderDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 8685753096364050900L;


	private static final String DOT = ".";
	private static final String TAG_NAME = "NAME";
	private static final String TAG_VERSION = "VERSION";

	private static final String TAG_PROTOCOL = "PROTOCOL";
	private static final String TAG_PROTOCOL_NAME = TAG_PROTOCOL + DOT + TAG_NAME;
	private static final String TAG_PROTOCOL_VERSION = TAG_PROTOCOL + DOT + TAG_VERSION;
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_SOURCE_SYSTEM_ID = TAG_SOURCE + DOT + "SYSTEM_ID";
	private static final String TAG_SOURCE_NAME = TAG_SOURCE + DOT + TAG_NAME;
	private static final String TAG_SOURCE_VERSION = TAG_SOURCE + DOT + TAG_VERSION;
	private static final String TAG_SOURCE_CORPORATE = TAG_SOURCE + DOT + "CORPORATE";
	private static final String TAG_DATE = "DATE";
	private static final String TAG_COPYRIGHT = "COPYRIGHT";
	private static final String TAG_SUBMITTER = "SUBMITTER";
	private static final String TAG_SUBMITTER_NAME = TAG_SUBMITTER + DOT + TAG_NAME;
	private static final String TAG_SUBMITTER_CONTACT = TAG_SUBMITTER + DOT + "CONTACT";
	private static final String TAG_SUBMITTER_NOTE = TAG_SUBMITTER + DOT + "NOTE";
	private static final String TAG_SCOPE = "SCOPE";


	private Dialog parent;

	private final FLEFModel model;
	private final FLEFRecord headerRecord;

	private final BindingManager bindingManager = new BindingManager();

	private final BoundTextField protocolNameField;
	private final BoundTextField protocolVersionField;

	private final BoundTextField sourceSystemIdField;
	private final BoundTextField sourceNameField;
	private final BoundTextField sourceVersionField;
	private final BoundTextField sourceCorporateField;

	private final BoundTextField dateField;

	private final BoundTextArea copyrightArea;

	private final BoundTextField submitterNameField;
	private final ContactListPanel submitterContactListPanel;
	private final BasicNoteListPanel submitterNotePanel;

	private final BoundTextArea scopeArea;


	public HeaderDialog(final Dialog parent, final FLEFModel model){
		super(parent, "Header", ModalityType.APPLICATION_MODAL);

		this.parent = parent;

		this.model = model;
		headerRecord = model.getHeader();

		protocolNameField = new BoundTextField(TAG_PROTOCOL_NAME, 20);
		protocolVersionField = new BoundTextField(TAG_PROTOCOL_VERSION, 20);
		sourceSystemIdField = new BoundTextField(TAG_SOURCE_SYSTEM_ID, 20);
		sourceNameField = new BoundTextField(TAG_SOURCE_NAME, 20);
		sourceVersionField = new BoundTextField(TAG_SOURCE_VERSION, 20);
		sourceCorporateField = new BoundTextField(TAG_SOURCE_CORPORATE, 20);
		dateField = new BoundTextField(TAG_DATE, 20);
		copyrightArea = new BoundTextArea(TAG_COPYRIGHT, 3, 25);
		submitterNameField = new BoundTextField(TAG_SUBMITTER_NAME, 20);
		submitterContactListPanel = new ContactListPanel(TAG_SUBMITTER_CONTACT, this, model);
		submitterNotePanel = new BasicNoteListPanel(TAG_SUBMITTER_NOTE, this, "Notes");
		scopeArea = new BoundTextArea(TAG_SCOPE, 3, 25);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(protocolNameField);
		bindingManager.bind(protocolVersionField);
		bindingManager.bind(sourceSystemIdField);
		bindingManager.bind(sourceNameField);
		bindingManager.bind(sourceVersionField);
		bindingManager.bind(sourceCorporateField);
		bindingManager.bind(dateField);
		bindingManager.bind(copyrightArea);
		bindingManager.bind(submitterNameField);
		bindingManager.bind(scopeArea);

		protocolNameField.setText("Family LEgacy Format");
		protocolVersionField.setText("0.1.1");

		sourceSystemIdField.setText("FamilyLegacy");
		sourceNameField.setText("FL");
		sourceVersionField.setText("0.1");
		sourceCorporateField.setText("(c) Mauro Trevisan");
		dateField.setEnabled(false);

		setLayout(new MigLayout("ins 10,fillx", "[grow]"));

		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("Submitter", createSubmitterPanel());
		add(tabbedPane, BorderLayout.CENTER);

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}


	private JPanel createMainPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10", "[right]rel[grow]", "[]15[]15[]"));

		// date
		panel.add(new JLabel("Date:"), "align label");
		panel.add(dateField, "growx,wrap");

		// copyright
		panel.add(new JLabel("Copyright:"), "align label,top");
		panel.add(copyrightArea, "growx,wrap");

		// scope
		panel.add(new JLabel("Scope:"), "align label,top");
		panel.add(scopeArea, "growx");

		return panel;
	}

	private JPanel createSubmitterPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10", "[right]rel[grow]", "[]15[]15[]"));

		// name
		panel.add(new JLabel("Name:"), "align label,top");
		panel.add(submitterNameField, "growx,wrap");

		// contact
		panel.add(submitterContactListPanel, "span 2,growx,wrap");

		// note
		panel.add(submitterNotePanel, "span 2,growx,wrap");

		return panel;
	}

	private JPanel createSubmitterNotePanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));

//		submitterNoteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//		submitterNoteList.addMouseListener(new MouseAdapter(){
//			@Override
//			public void mouseClicked(MouseEvent e){
//				if(e.getClickCount() == 2){
//					editSubmitterNote();
//				}
//			}
//		});
//		JScrollPane scrollPane = GUIHelper.createScrollPane(submitterNoteList);
//		panel.add(scrollPane, BorderLayout.CENTER);
//
//		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
//		JButton addBtn = new JButton("Add");
//		JButton newBtn = new JButton("New");
//		JButton editBtn = new JButton("Edit");
//		JButton deleteBtn = new JButton("Delete");
//		btnPanel.add(addBtn);
//		btnPanel.add(newBtn);
//		btnPanel.add(editBtn);
//		btnPanel.add(deleteBtn);
//		panel.add(btnPanel, BorderLayout.SOUTH);
//
//		submitterNoteList.addListSelectionListener(e -> {
//			boolean selected = submitterNoteList.getSelectedIndex() != -1;
//			editBtn.setEnabled(selected);
//			deleteBtn.setEnabled(selected);
//		});
//		editBtn.setEnabled(false);
//		deleteBtn.setEnabled(false);
//
//		addBtn.addActionListener(e -> addSubmitterNote());
//		newBtn.addActionListener(e -> createNewSubmitterNote());
//		editBtn.addActionListener(e -> editSubmitterNote());
//		deleteBtn.addActionListener(e -> deleteSubmitterNote());

		return panel;
	}


	private String getContactDisplay(FLEFRecord contact){
//		String address = contact.getValue();
//		String type = FLEFRecordHelper.getChildValue(contact, "TYPE");
//		String name = FLEFRecordHelper.getChildValue(contact, "NAME");
//		StringBuilder sb = new StringBuilder();
//		if(address != null && !address.isEmpty()){
//			sb.append(address);
//		}
//		if(type != null && !type.isEmpty()){
//			if(!sb.isEmpty())
//				sb.append(" (");
//			sb.append(type);
//			if(!sb.isEmpty() && !sb.toString().endsWith("("))
//				sb.append(")");
//		}
//		if(name != null && !name.isEmpty()){
//			if(!sb.isEmpty())
//				sb.append(" - ");
//			sb.append(name);
//		}
//		if(sb.isEmpty())
//			sb.append("[empty]");
//		return sb.toString();
		return "";
	}

	private void loadContacts(){
//		contactListModel.clear();
//		contactRecords.clear();
//		FLEFRecord submitter = FLEFRecordHelper.findChild(headerRecord, "SUBMITTER");
//		if(submitter != null){
//			for(FLEFRecord child : submitter.getChildren()){
//				if("CONTACT".equals(child.getTag())){
//					contactRecords.add(child);
//					contactListModel.addElement(getContactDisplay(child));
//				}
//			}
//		}
	}

	private void addContact(){
//		ContactStructurePanel panel = new ContactStructurePanel(this, model);
//		JDialog dialog = new JDialog(this, "Add Contact", true);
//		dialog.setLayout(new BorderLayout(10, 10));
//		dialog.add(panel, BorderLayout.CENTER);
//
//		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
//		JButton okBtn = new JButton("OK");
//		JButton cancelBtn = new JButton("Cancel");
//		btnPanel.add(okBtn);
//		btnPanel.add(cancelBtn);
//		dialog.add(btnPanel, BorderLayout.SOUTH);
//
//		final FLEFRecord[] result = {null};
//		okBtn.addActionListener(e -> {
//			if(panel.validateData()){
//				FLEFRecord contact = panel.saveToRecord(null);
//				if(contact != null){
//					contact.setTag("CONTACT");
//					result[0] = contact;
//					dialog.dispose();
//				}
//			}
//		});
//		cancelBtn.addActionListener(e -> dialog.dispose());
//
//		dialog.pack();
//		dialog.setLocationRelativeTo(this);
//		dialog.setVisible(true);
//
//		if(result[0] != null){
//			contactRecords.add(result[0]);
//			contactListModel.addElement(getContactDisplay(result[0]));
//		}
	}

	private void editContact(){
//		int idx = contactList.getSelectedIndex();
//		if(idx == -1)
//			return;
//		FLEFRecord existing = contactRecords.get(idx);
//
//		ContactStructurePanel panel = new ContactStructurePanel(this, model);
//		panel.loadFromRecord(existing);
//
//		JDialog dialog = new JDialog(this, "Edit Contact", true);
//		dialog.setLayout(new BorderLayout(10, 10));
//		dialog.add(panel, BorderLayout.CENTER);
//
//		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
//		JButton okBtn = new JButton("OK");
//		JButton cancelBtn = new JButton("Cancel");
//		btnPanel.add(okBtn);
//		btnPanel.add(cancelBtn);
//		dialog.add(btnPanel, BorderLayout.SOUTH);
//
//		final FLEFRecord[] result = {null};
//		okBtn.addActionListener(e -> {
//			if(panel.validateData()){
//				FLEFRecord contact = panel.saveToRecord(existing);
//				if(contact != null){
//					contact.setTag("CONTACT");
//					result[0] = contact;
//					dialog.dispose();
//				}
//			}
//		});
//		cancelBtn.addActionListener(e -> dialog.dispose());
//
//		dialog.pack();
//		dialog.setLocationRelativeTo(this);
//		dialog.setVisible(true);
//
//		if(result[0] != null){
//			contactRecords.set(idx, result[0]);
//			contactListModel.set(idx, getContactDisplay(result[0]));
//		}
	}

	private void deleteContact(){
//		int idx = contactList.getSelectedIndex();
//		if(idx == -1)
//			return;
//		if(!showConfirm("Confirm", "Remove this contact?"))
//			return;
//		contactRecords.remove(idx);
//		contactListModel.remove(idx);
	}


	private String getNoteDisplayName(String id){
//		FLEFRecord rec = model.getRecordById(id);
//		if(rec != null){
//			return noteHandler.getDisplayText(rec, model);
//		}
//		return id;
		return null;
	}

	private void loadSubmitterNotes(){
//		submitterNoteListModel.clear();
//		submitterNoteIds.clear();
//		submitterNoteDisplayMap.clear();
//		FLEFRecord submitter = FLEFRecordHelper.findChild(headerRecord, "SUBMITTER");
//		if(submitter != null){
//			for(FLEFRecord child : submitter.getChildren()){
//				if("NOTE".equals(child.getTag()) && child.getValue() != null){
//					String id = child.getValue();
//					submitterNoteIds.add(id);
//					String display = getNoteDisplayName(id);
//					submitterNoteDisplayMap.put(id, display);
//					submitterNoteListModel.addElement(display);
//				}
//			}
//		}
	}

	private void addSubmitterNote(){
//		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
//			parent, model, noteHandler, selectedItem -> {
//			final String selectedId = selectedItem.getValue();
//			if(selectedId != null && !submitterNoteIds.contains(selectedId)){
//				submitterNoteIds.add(selectedId);
//				String display = getNoteDisplayName(selectedId);
//				submitterNoteDisplayMap.put(selectedId, display);
//				submitterNoteListModel.addElement(display);
//			}
//		}
//		);
//		dialog.setVisible(true);
	}

	private void editSubmitterNote(){
//		int idx = submitterNoteList.getSelectedIndex();
//		if(idx == -1)
//			return;
//		String id = submitterNoteIds.get(idx);
//		FLEFRecord rec = model.getRecordById(id);
//		if(rec == null){
//			JOptionPane.showMessageDialog(this, "Note not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
//			return;
//		}
//		JDialog dialog = noteHandler.createEditDialog(parent, model, rec);
//		dialog.setVisible(true);
//
//		String newDisplay = getNoteDisplayName(id);
//		submitterNoteDisplayMap.put(id, newDisplay);
//		submitterNoteListModel.set(idx, newDisplay);
	}

	private void deleteSubmitterNote(){
//		int idx = submitterNoteList.getSelectedIndex();
//		if(idx == -1)
//			return;
//		if(!showConfirm("Confirm", "Remove this note reference?"))
//			return;
//		String removedId = submitterNoteIds.remove(idx);
//		submitterNoteDisplayMap.remove(removedId);
//		submitterNoteListModel.remove(idx);
	}

	private void createNewSubmitterNote(){
//		Set<String> before = new HashSet<>(submitterNoteIds);
//		JDialog dialog = noteHandler.createNewDialog(parent, model);
//		dialog.setVisible(true);
//
//		for(FLEFRecord rec : model.getRecordsByType("NOTE")){
//			String id = rec.getId();
//			if(id != null && !before.contains(id) && !submitterNoteIds.contains(id)){
//				submitterNoteIds.add(id);
//				String display = getNoteDisplayName(id);
//				submitterNoteDisplayMap.put(id, display);
//				submitterNoteListModel.addElement(display);
//				break;
//			}
//		}
	}


	private boolean showConfirm(String title, String message){
		return JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	}

	private void loadData(){
		String date = FLEFRecordHelper.getChildValuesAsString(headerRecord, TAG_DATE);
		if(StringUtils.isBlank(date))
			date = "--";
		dateField.setText(date);

		bindingManager.load(headerRecord);

		submitterContactListPanel.load(headerRecord);
		submitterNotePanel.load(headerRecord);
	}

	private void save(){
		FLEFRecordHelper.removeAllChildren(headerRecord);

		bindingManager.save(headerRecord);

		submitterContactListPanel.save(headerRecord);
		submitterNotePanel.save(headerRecord);

		dispose();
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final HeaderDialog dialog = new HeaderDialog(null, model);
			dialog.setVisible(true);
		});
	}

}
