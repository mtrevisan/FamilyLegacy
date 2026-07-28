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

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.ContactStructurePanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Dialog for editing the HEADER structure according to FLEF 0.0.9.
 * <p>
 * Structure:
 * <pre>
 * HEADER :=
 *   n HEADER    {1:1}
 *     +1 PROTOCOL <PROTOCOL_NAME>    {1:1}
 *       +2 NAME <NAME_OF_PROTOCOL>    {1:1}
 *       +2 VERSION <VERSION_NUMBER>    {1:1}
 *     +1 SOURCE <APPROVED_SYSTEM_ID>    {1:1}
 *       +2 NAME <NAME_OF_PRODUCT>    {0:1}
 *       +2 VERSION <VERSION_NUMBER>    {0:1}
 *       +2 CORPORATE <NAME_OF_BUSINESS>    {0:1}
 *     +1 DATE <CREATION_DATE>    {1:1}
 *     +1 COPYRIGHT <COPYRIGHT_SOURCE_DATA>    {0:1}
 *     +1 SUBMITTER    {1:1}
 *       +2 NAME <SUBMITTER_NAME>    {1:1}
 *       +2 PLACE    {0:1}
 *         +3 ADDRESS <ADDRESS_LINE>    {0:1}
 *           +4 HIERARCHY <ADDRESS_HIERARCHY>    {0:1}
 *         +3 MAP    {0:1}
 *           +4 LATITUDE <PLACE_LATITUDE>    {1:1}
 *           +4 LONGITUDE <PLACE_LONGITUDE>    {1:1}
 *         +3 NOTE <PLACE_NOTE_STRUCTURE>    {0:M}
 *       +2 <<CONTACT_STRUCTURE>>    {0:M}
 *       +2 NOTE <SUBMITTER_NOTE_STRUCTURE>    {0:M}
 *     +1 NOTE <CONTENT_DESCRIPTION>    {0:1}
 * </pre>
 */
public class HeaderDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 8685753096364050900L;


	private Dialog parent;

	private final FLEFModel model;
	private final FLEFRecord headerRecord;
	private boolean saved = false;

	private final JTextField protocolNameField = new JTextField(15);
	private final JTextField protocolVersionField = new JTextField(15);

	private final JTextField sourceIdField = new JTextField(20);
	private final JTextField sourceNameField = new JTextField(20);
	private final JTextField sourceVersionField = new JTextField(15);
	private final JTextField sourceCorporateField = new JTextField(20);

	private final JTextField dateField = new JTextField(20);

	private final JTextArea copyrightArea = new JTextArea(3, 30);

	private final JTextField submitterNameField = new JTextField(30);

	private final JTextField submitterAddressField = new JTextField(30);
	private final JTextField submitterHierarchyField = new JTextField(30);
	private final JTextField submitterLatitudeField = new JTextField(15);
	private final JTextField submitterLongitudeField = new JTextField(15);
	private final JTextArea submitterPlaceNoteArea = new JTextArea(2, 20);

	private final DefaultListModel<String> contactListModel = new DefaultListModel<>();
	private final JList<String> contactList = new JList<>(contactListModel);
	private final List<FLEFRecord> contactRecords = new ArrayList<>();

	private final DefaultListModel<String> submitterNoteListModel = new DefaultListModel<>();
	private final JList<String> submitterNoteList = new JList<>(submitterNoteListModel);
	private final List<String> submitterNoteIds = new ArrayList<>();
	private final Map<String, String> submitterNoteDisplayMap = new HashMap<>();

	private final JTextArea headerNoteArea = new JTextArea(3, 30);

	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");


	public HeaderDialog(Dialog parent, FLEFModel model, FLEFRecord headerRecord){
		super(parent, "Edit Header", true);

		this.parent = parent;

		this.model = model;
		this.headerRecord = headerRecord != null? headerRecord: new FLEFRecord();
		initComponents();
		loadData();
		setMinimumSize(new Dimension(850, 800));
		pack();
		setLocationRelativeTo(parent);
	}

	public HeaderDialog(Dialog parent, FLEFModel model){
		this(parent, model, null);
	}

	private void initComponents(){
		setLayout(new BorderLayout(10, 10));

		JTabbedPane tabbedPane = new JTabbedPane();

		// --- Main tab ---
		tabbedPane.addTab("Main", createMainPanel());

		// --- Submitter tab ---
		tabbedPane.addTab("Submitter", createSubmitterPanel());

		add(tabbedPane, BorderLayout.CENTER);

		// --- Button panel ---
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		saveButton.addActionListener(e -> save());
		cancelButton.addActionListener(e -> dispose());
	}


	private JPanel createMainPanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]10[]10[]10[]10[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// --- PROTOCOL ---
		panel.add(new JLabel("Protocol Name*:"), "align label");
		panel.add(protocolNameField, "growx,wrap");

		panel.add(new JLabel("Protocol Version*:"), "align label");
		panel.add(protocolVersionField, "growx,wrap");

		// --- SOURCE ---
		panel.add(new JLabel("Source ID*:"), "align label");
		panel.add(sourceIdField, "growx,wrap");

		panel.add(new JLabel("Source Name:"), "align label");
		panel.add(sourceNameField, "growx,wrap");

		panel.add(new JLabel("Source Version:"), "align label");
		panel.add(sourceVersionField, "growx,wrap");

		panel.add(new JLabel("Source Corporate:"), "align label");
		panel.add(sourceCorporateField, "growx,wrap");

		// --- DATE ---
		panel.add(new JLabel("Date*:"), "align label");
		panel.add(dateField, "growx,wrap");

		// --- COPYRIGHT ---
		panel.add(new JLabel("Copyright:"), "align label,top");
		JScrollPane copyScroll = GUIHelper.createScrollPane(copyrightArea);
		panel.add(copyScroll, "growx,wrap");

		// --- HEADER NOTE ---
		panel.add(new JLabel("Content Description:"), "align label,top");
		JScrollPane noteScroll = GUIHelper.createScrollPane(headerNoteArea);
		panel.add(noteScroll, "growx");

		return panel;
	}

	private JPanel createSubmitterPanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]10[]10[]10[]10[]10[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// --- SUBMITTER NAME ---
		panel.add(new JLabel("Submitter Name*:"), "align label");
		panel.add(submitterNameField, "growx,wrap");

		// --- SUBMITTER PLACE ---
		panel.add(new JLabel("Place Address:"), "align label");
		panel.add(submitterAddressField, "growx,wrap");

		panel.add(new JLabel("  Hierarchy:"), "align label");
		panel.add(submitterHierarchyField, "growx,wrap");

		panel.add(new JLabel("  Latitude:"), "align label");
		panel.add(submitterLatitudeField, "growx,wrap");

		panel.add(new JLabel("  Longitude:"), "align label");
		panel.add(submitterLongitudeField, "growx,wrap");

		panel.add(new JLabel("  Place Note:"), "align label,top");
		JScrollPane placeNoteScroll = GUIHelper.createScrollPane(submitterPlaceNoteArea);
		panel.add(placeNoteScroll, "growx,wrap");

		// --- CONTACT_STRUCTURE (0:M) ---
		panel.add(new JLabel("Contacts (0:M):"), "align label,top");
		JPanel contactPanel = createContactPanel();
		panel.add(contactPanel, "growx,wrap");

		// --- SUBMITTER NOTE (0:M) ---
		panel.add(new JLabel("Submitter Notes:"), "align label,top");
		JPanel notePanel = createSubmitterNotePanel();
		panel.add(notePanel, "growx");

		return panel;
	}

	private JPanel createContactPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));

		contactList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		contactList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editContact();
				}
			}
		});
		JScrollPane scrollPane = GUIHelper.createScrollPane(contactList);
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(editBtn);
		btnPanel.add(deleteBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		contactList.addListSelectionListener(e -> {
			boolean selected = contactList.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addContact());
		editBtn.addActionListener(e -> editContact());
		deleteBtn.addActionListener(e -> deleteContact());

		return panel;
	}

	private JPanel createSubmitterNotePanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));

		submitterNoteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		submitterNoteList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editSubmitterNote();
				}
			}
		});
		JScrollPane scrollPane = GUIHelper.createScrollPane(submitterNoteList);
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton newBtn = new JButton("New");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(newBtn);
		btnPanel.add(editBtn);
		btnPanel.add(deleteBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		submitterNoteList.addListSelectionListener(e -> {
			boolean selected = submitterNoteList.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addSubmitterNote());
		newBtn.addActionListener(e -> createNewSubmitterNote());
		editBtn.addActionListener(e -> editSubmitterNote());
		deleteBtn.addActionListener(e -> deleteSubmitterNote());

		return panel;
	}


	private String getContactDisplay(FLEFRecord contact){
		String address = contact.getValue();
		String type = FLEFRecordUtils.getChildValue(contact, "TYPE");
		String callerId = FLEFRecordUtils.getChildValue(contact, "CALLER_ID");
		StringBuilder sb = new StringBuilder();
		if(address != null && !address.isEmpty()){
			sb.append(address);
		}
		if(type != null && !type.isEmpty()){
			if(!sb.isEmpty())
				sb.append(" (");
			sb.append(type);
			if(!sb.isEmpty() && !sb.toString().endsWith("("))
				sb.append(")");
		}
		if(callerId != null && !callerId.isEmpty()){
			if(!sb.isEmpty())
				sb.append(" - ");
			sb.append(callerId);
		}
		if(sb.isEmpty())
			sb.append("[empty]");
		return sb.toString();
	}

	private void loadContacts(){
		contactListModel.clear();
		contactRecords.clear();
		FLEFRecord submitter = FLEFRecordUtils.findChild(headerRecord, "SUBMITTER");
		if(submitter != null){
			for(FLEFRecord child : submitter.getChildren()){
				if("CONTACT".equals(child.getTag())){
					contactRecords.add(child);
					contactListModel.addElement(getContactDisplay(child));
				}
			}
		}
	}

	private void addContact(){
		ContactStructurePanel panel = new ContactStructurePanel(model, this);
		JDialog dialog = new JDialog(this, "Add Contact", true);
		dialog.setLayout(new BorderLayout(10, 10));
		dialog.add(panel, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		dialog.add(btnPanel, BorderLayout.SOUTH);

		final FLEFRecord[] result = {null};
		okBtn.addActionListener(e -> {
			if(panel.validateRequiredFields()){
				FLEFRecord contact = panel.saveToRecord(null);
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
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);

		if(result[0] != null){
			contactRecords.add(result[0]);
			contactListModel.addElement(getContactDisplay(result[0]));
		}
	}

	private void editContact(){
		int idx = contactList.getSelectedIndex();
		if(idx == -1)
			return;
		FLEFRecord existing = contactRecords.get(idx);

		ContactStructurePanel panel = new ContactStructurePanel(model, this);
		panel.loadFromRecord(existing);

		JDialog dialog = new JDialog(this, "Edit Contact", true);
		dialog.setLayout(new BorderLayout(10, 10));
		dialog.add(panel, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		dialog.add(btnPanel, BorderLayout.SOUTH);

		final FLEFRecord[] result = {null};
		okBtn.addActionListener(e -> {
			if(panel.validateRequiredFields()){
				FLEFRecord contact = panel.saveToRecord(existing);
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
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);

		if(result[0] != null){
			contactRecords.set(idx, result[0]);
			contactListModel.set(idx, getContactDisplay(result[0]));
		}
	}

	private void deleteContact(){
		int idx = contactList.getSelectedIndex();
		if(idx == -1)
			return;
		if(!showConfirm("Confirm", "Remove this contact?"))
			return;
		contactRecords.remove(idx);
		contactListModel.remove(idx);
	}


	private String getNoteDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return noteHandler.getDisplayText(rec);
		}
		return id;
	}

	private void loadSubmitterNotes(){
		submitterNoteListModel.clear();
		submitterNoteIds.clear();
		submitterNoteDisplayMap.clear();
		FLEFRecord submitter = FLEFRecordUtils.findChild(headerRecord, "SUBMITTER");
		if(submitter != null){
			for(FLEFRecord child : submitter.getChildren()){
				if("NOTE".equals(child.getTag()) && child.getValue() != null){
					String id = child.getValue();
					submitterNoteIds.add(id);
					String display = getNoteDisplayName(id);
					submitterNoteDisplayMap.put(id, display);
					submitterNoteListModel.addElement(display);
				}
			}
		}
	}

	private void addSubmitterNote(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parent, model, noteHandler, selectedId -> {
			if(selectedId != null && !submitterNoteIds.contains(selectedId)){
				submitterNoteIds.add(selectedId);
				String display = getNoteDisplayName(selectedId);
				submitterNoteDisplayMap.put(selectedId, display);
				submitterNoteListModel.addElement(display);
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editSubmitterNote(){
		int idx = submitterNoteList.getSelectedIndex();
		if(idx == -1)
			return;
		String id = submitterNoteIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(this, "Note not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = noteHandler.createEditDialog(parent, model, rec);
		dialog.setVisible(true);
		String newDisplay = getNoteDisplayName(id);
		submitterNoteDisplayMap.put(id, newDisplay);
		submitterNoteListModel.set(idx, newDisplay);
	}

	private void deleteSubmitterNote(){
		int idx = submitterNoteList.getSelectedIndex();
		if(idx == -1)
			return;
		if(!showConfirm("Confirm", "Remove this note reference?"))
			return;
		String removedId = submitterNoteIds.remove(idx);
		submitterNoteDisplayMap.remove(removedId);
		submitterNoteListModel.remove(idx);
	}

	private void createNewSubmitterNote(){
		Set<String> before = new HashSet<>(submitterNoteIds);
		JDialog dialog = noteHandler.createNewDialog(parent, model);
		dialog.setVisible(true);
		for(FLEFRecord rec : model.getRecordsByType("NOTE")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !submitterNoteIds.contains(id)){
				submitterNoteIds.add(id);
				String display = getNoteDisplayName(id);
				submitterNoteDisplayMap.put(id, display);
				submitterNoteListModel.addElement(display);
				break;
			}
		}
	}


	private boolean showConfirm(String title, String message){
		return JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	}


	private void loadData(){
		// --- PROTOCOL ---
		FLEFRecord protocol = FLEFRecordUtils.findChild(headerRecord, "PROTOCOL");
		if(protocol != null){
			protocolNameField.setText(FLEFRecordUtils.getChildValue(protocol, "NAME"));
			protocolVersionField.setText(FLEFRecordUtils.getChildValue(protocol, "VERSION"));
		}

		// --- SOURCE ---
		FLEFRecord source = FLEFRecordUtils.findChild(headerRecord, "SOURCE");
		if(source != null){
			sourceIdField.setText(source.getValue());
			sourceNameField.setText(FLEFRecordUtils.getChildValue(source, "NAME"));
			sourceVersionField.setText(FLEFRecordUtils.getChildValue(source, "VERSION"));
			sourceCorporateField.setText(FLEFRecordUtils.getChildValue(source, "CORPORATE"));
		}

		// --- DATE ---
		dateField.setText(FLEFRecordUtils.getChildValue(headerRecord, "DATE"));

		// --- COPYRIGHT ---
		copyrightArea.setText(FLEFRecordUtils.getChildValue(headerRecord, "COPYRIGHT"));

		// --- HEADER NOTE ---
		headerNoteArea.setText(FLEFRecordUtils.getChildValue(headerRecord, "NOTE"));

		// --- SUBMITTER ---
		FLEFRecord submitter = FLEFRecordUtils.findChild(headerRecord, "SUBMITTER");
		if(submitter != null){
			// SUBMITTER NAME
			submitterNameField.setText(FLEFRecordUtils.getChildValue(submitter, "NAME"));

			// SUBMITTER PLACE
			FLEFRecord place = FLEFRecordUtils.findChild(submitter, "PLACE");
			if(place != null){
				submitterAddressField.setText(FLEFRecordUtils.getChildValue(place, "ADDRESS"));
				submitterHierarchyField.setText(FLEFRecordUtils.getChildValue(place, "HIERARCHY"));
				FLEFRecord map = FLEFRecordUtils.findChild(place, "MAP");
				if(map != null){
					submitterLatitudeField.setText(FLEFRecordUtils.getChildValue(map, "LATITUDE"));
					submitterLongitudeField.setText(FLEFRecordUtils.getChildValue(map, "LONGITUDE"));
				}
				// Place notes (simplified: as text)
				StringBuilder notes = new StringBuilder();
				for(FLEFRecord note : place.getChildren()){
					if("NOTE".equals(note.getTag()) && note.getValue() != null){
						if(!notes.isEmpty())
							notes.append("\n");
						notes.append(note.getValue());
					}
				}
				submitterPlaceNoteArea.setText(notes.toString());
			}

			// CONTACT_STRUCTURE (0:M)
			loadContacts();

			// SUBMITTER NOTE (0:M)
			loadSubmitterNotes();
		}
	}


	private boolean validateData(){
		// PROTOCOL NAME
		if(protocolNameField.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(this,
				"Protocol Name is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			protocolNameField.requestFocusInWindow();
			return false;
		}

		// PROTOCOL VERSION
		if(protocolVersionField.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(this,
				"Protocol Version is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			protocolVersionField.requestFocusInWindow();
			return false;
		}

		// SOURCE ID
		if(sourceIdField.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(this,
				"Source ID is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			sourceIdField.requestFocusInWindow();
			return false;
		}

		// DATE
		if(dateField.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(this,
				"Date is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			dateField.requestFocusInWindow();
			return false;
		}

		// SUBMITTER NAME
		if(submitterNameField.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(this,
				"Submitter Name is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			submitterNameField.requestFocusInWindow();
			return false;
		}

		return true;
	}


	private void save(){
		if(!validateData()){
			return;
		}

		// Clear existing children
		FLEFRecordUtils.removeAllChildren(headerRecord);

		// --- PROTOCOL ---
		FLEFRecord protocol = new FLEFRecord();
		protocol.setTag("PROTOCOL");
		headerRecord.addChild(protocol);
		FLEFRecordUtils.updateChildValue(protocol, "NAME", protocolNameField.getText().trim());
		FLEFRecordUtils.updateChildValue(protocol, "VERSION", protocolVersionField.getText().trim());

		// --- SOURCE ---
		FLEFRecord source = new FLEFRecord();
		source.setTag("SOURCE");
		source.setValue(sourceIdField.getText().trim());
		headerRecord.addChild(source);
		FLEFRecordUtils.updateChildValue(source, "NAME", sourceNameField.getText().trim());
		FLEFRecordUtils.updateChildValue(source, "VERSION", sourceVersionField.getText().trim());
		FLEFRecordUtils.updateChildValue(source, "CORPORATE", sourceCorporateField.getText().trim());

		// --- DATE ---
		FLEFRecordUtils.updateChildValue(headerRecord, "DATE", dateField.getText().trim());

		// --- COPYRIGHT ---
		String copyright = copyrightArea.getText().trim();
		FLEFRecordUtils.updateChildValue(headerRecord, "COPYRIGHT", copyright);

		// --- HEADER NOTE ---
		String headerNote = headerNoteArea.getText().trim();
		FLEFRecordUtils.updateChildValue(headerRecord, "NOTE", headerNote);

		// --- SUBMITTER ---
		FLEFRecord submitter = new FLEFRecord();
		submitter.setTag("SUBMITTER");
		headerRecord.addChild(submitter);

		// SUBMITTER NAME
		FLEFRecordUtils.updateChildValue(submitter, "NAME", submitterNameField.getText().trim());

		// SUBMITTER PLACE
		String address = submitterAddressField.getText().trim();
		String hierarchy = submitterHierarchyField.getText().trim();
		String lat = submitterLatitudeField.getText().trim();
		String lon = submitterLongitudeField.getText().trim();
		String placeNote = submitterPlaceNoteArea.getText().trim();

		if(!address.isEmpty() || !hierarchy.isEmpty() || !lat.isEmpty() || !lon.isEmpty() || !placeNote.isEmpty()){
			FLEFRecord place = new FLEFRecord();
			place.setTag("PLACE");
			submitter.addChild(place);

			FLEFRecordUtils.updateChildValue(place, "ADDRESS", address);
			FLEFRecordUtils.updateChildValue(place, "HIERARCHY", hierarchy);
			if(!lat.isEmpty() && !lon.isEmpty()){
				FLEFRecord map = new FLEFRecord();
				map.setTag("MAP");
				place.addChild(map);
				FLEFRecordUtils.updateChildValue(map, "LATITUDE", lat);
				FLEFRecordUtils.updateChildValue(map, "LONGITUDE", lon);
			}
			if(!placeNote.isEmpty()){
				FLEFRecord note = new FLEFRecord();
				note.setTag("NOTE");
				note.setValue(placeNote);
				place.addChild(note);
			}
		}

		// CONTACT_STRUCTURE (0:M)
		for(FLEFRecord contact : contactRecords){
			contact.setTag("CONTACT");
			submitter.addChild(contact);
		}

		// SUBMITTER NOTE (0:M)
		for(String id : submitterNoteIds){
			FLEFRecordUtils.addChild(submitter, "NOTE", id);
		}

		saved = true;
		dispose();
	}

	public boolean isSaved(){
		return saved;
	}

	public FLEFRecord getHeaderRecord(){
		return headerRecord;
	}


	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();
		HandlerRegistry.register(new NoteHandler());

		// Crea un header di esempio
		FLEFRecord header = new FLEFRecord();
		header.setTag("HEADER");

		FLEFRecord protocol = new FLEFRecord();
		protocol.setTag("PROTOCOL");
		header.addChild(protocol);
		FLEFRecordUtils.updateChildValue(protocol, "NAME", "FLEF");
		FLEFRecordUtils.updateChildValue(protocol, "VERSION", "0.0.9");

		FLEFRecord source = new FLEFRecord();
		source.setTag("SOURCE");
		source.setValue("MyApp");
		header.addChild(source);
		FLEFRecordUtils.updateChildValue(source, "NAME", "My Application");
		FLEFRecordUtils.updateChildValue(source, "VERSION", "1.0");

		FLEFRecordUtils.updateChildValue(header, "DATE", "2026-07-11");

		FLEFRecord submitter = new FLEFRecord();
		submitter.setTag("SUBMITTER");
		header.addChild(submitter);
		FLEFRecordUtils.updateChildValue(submitter, "NAME", "Mauro Trevisan");

		model.setHeader(header);

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Header Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("Edit Header");
			btn.addActionListener(e -> {
				HeaderDialog dialog = new HeaderDialog(null, model, header);
				dialog.setVisible(true);
				if(dialog.isSaved()){
					System.out.println("Header saved.");
				}
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
