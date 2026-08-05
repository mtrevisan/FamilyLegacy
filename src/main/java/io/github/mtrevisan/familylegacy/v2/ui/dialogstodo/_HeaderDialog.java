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
package io.github.mtrevisan.familylegacy.v2.ui.dialogstodo;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.ContactStructurePanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

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
public class _HeaderDialog extends JDialog{

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

	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);


	public _HeaderDialog(Dialog parent, FLEFModel model, FLEFRecord headerRecord){
		super(parent, "Edit Header", true);

		this.parent = parent;

		this.model = model;
		this.headerRecord = headerRecord != null? headerRecord: FLEFRecord.createEmpty();
		initComponents();
		loadData();
		pack();
		setLocationRelativeTo(parent);
	}

	public _HeaderDialog(Dialog parent, FLEFModel model){
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
		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}


	private JPanel createMainPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10", "[right]rel[grow]", "[]10[]10[]10[]10[]"));

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
		JPanel panel = new JPanel(new MigLayout("ins 10", "[right]rel[grow]", "[]10[]10[]10[]10[]10[]"));

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
		String type = FLEFRecordHelper.getChildValue(contact, "TYPE");
		String name = FLEFRecordHelper.getChildValue(contact, "NAME");
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
		if(name != null && !name.isEmpty()){
			if(!sb.isEmpty())
				sb.append(" - ");
			sb.append(name);
		}
		if(sb.isEmpty())
			sb.append("[empty]");
		return sb.toString();
	}

	private void loadContacts(){
		contactListModel.clear();
		contactRecords.clear();
		FLEFRecord submitter = FLEFRecordHelper.findChild(headerRecord, "SUBMITTER");
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
		ContactStructurePanel panel = new ContactStructurePanel(this, model);
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
			if(panel.validateData()){
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

		ContactStructurePanel panel = new ContactStructurePanel(this, model);
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
			if(panel.validateData()){
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
			return noteHandler.getDisplayText(rec, model);
		}
		return id;
	}

	private void loadSubmitterNotes(){
		submitterNoteListModel.clear();
		submitterNoteIds.clear();
		submitterNoteDisplayMap.clear();
		FLEFRecord submitter = FLEFRecordHelper.findChild(headerRecord, "SUBMITTER");
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
		FLEFRecord protocol = FLEFRecordHelper.findChild(headerRecord, "PROTOCOL");
		if(protocol != null){
			protocolNameField.setText(FLEFRecordHelper.getChildValue(protocol, "NAME"));
			protocolVersionField.setText(FLEFRecordHelper.getChildValue(protocol, "VERSION"));
		}

		// --- SOURCE ---
		FLEFRecord source = FLEFRecordHelper.findChild(headerRecord, "SOURCE");
		if(source != null){
			sourceIdField.setText(source.getValue());
			sourceNameField.setText(FLEFRecordHelper.getChildValue(source, "NAME"));
			sourceVersionField.setText(FLEFRecordHelper.getChildValue(source, "VERSION"));
			sourceCorporateField.setText(FLEFRecordHelper.getChildValue(source, "CORPORATE"));
		}

		// --- DATE ---
		dateField.setText(FLEFRecordHelper.getChildValue(headerRecord, "DATE"));

		// --- COPYRIGHT ---
		copyrightArea.setText(FLEFRecordHelper.getChildValue(headerRecord, "COPYRIGHT"));

		// --- HEADER NOTE ---
		headerNoteArea.setText(FLEFRecordHelper.getChildValue(headerRecord, "NOTE"));

		// --- SUBMITTER ---
		FLEFRecord submitter = FLEFRecordHelper.findChild(headerRecord, "SUBMITTER");
		if(submitter != null){
			// SUBMITTER NAME
			submitterNameField.setText(FLEFRecordHelper.getChildValue(submitter, "NAME"));

			// SUBMITTER PLACE
			FLEFRecord place = FLEFRecordHelper.findChild(submitter, "PLACE");
			if(place != null){
				submitterAddressField.setText(FLEFRecordHelper.getChildValue(place, "ADDRESS"));
				submitterHierarchyField.setText(FLEFRecordHelper.getChildValue(place, "HIERARCHY"));
				FLEFRecord map = FLEFRecordHelper.findChild(place, "MAP");
				if(map != null){
					submitterLatitudeField.setText(FLEFRecordHelper.getChildValue(map, "LATITUDE"));
					submitterLongitudeField.setText(FLEFRecordHelper.getChildValue(map, "LONGITUDE"));
				}
				// Place notes (simplified: as text)
				StringBuilder notes = new StringBuilder();
				for(FLEFRecord note : place.getChildren()){
					if("NOTE".equals(note.getTag()) && note.getValue() != null){
						if(!notes.isEmpty())
							notes.append(StringUtils.LF);
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
		if(StringUtils.isEmpty(protocolNameField.getText())){
			JOptionPane.showMessageDialog(this,
				"Protocol Name is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			protocolNameField.requestFocusInWindow();

			return false;
		}

		// PROTOCOL VERSION
		if(StringUtils.isEmpty(protocolVersionField.getText())){
			JOptionPane.showMessageDialog(this,
				"Protocol Version is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			protocolVersionField.requestFocusInWindow();

			return false;
		}

		// SOURCE ID
		if(StringUtils.isEmpty(sourceIdField.getText())){
			JOptionPane.showMessageDialog(this,
				"Source ID is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			sourceIdField.requestFocusInWindow();

			return false;
		}

		// DATE
		if(StringUtils.isEmpty(dateField.getText())){
			JOptionPane.showMessageDialog(this,
				"Date is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			dateField.requestFocusInWindow();

			return false;
		}

		// SUBMITTER NAME
		if(StringUtils.isEmpty(submitterNameField.getText())){
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
		FLEFRecordHelper.removeAllChildren(headerRecord);

		// --- PROTOCOL ---
		FLEFRecord protocol = FLEFRecord.createChild("PROTOCOL");
		headerRecord.addChild(protocol);
		FLEFRecordHelper.updateChildValue(protocol, "NAME", protocolNameField.getText().trim());
		FLEFRecordHelper.updateChildValue(protocol, "VERSION", protocolVersionField.getText().trim());

		// --- SOURCE ---
		FLEFRecord source = FLEFRecord.createChildWithValue("SOURCE", sourceIdField.getText().trim());
		headerRecord.addChild(source);
		FLEFRecordHelper.updateChildValue(source, "NAME", sourceNameField.getText().trim());
		FLEFRecordHelper.updateChildValue(source, "VERSION", sourceVersionField.getText().trim());
		FLEFRecordHelper.updateChildValue(source, "CORPORATE", sourceCorporateField.getText().trim());

		// --- DATE ---
		FLEFRecordHelper.updateChildValue(headerRecord, "DATE", dateField.getText().trim());

		// --- COPYRIGHT ---
		String copyright = copyrightArea.getText().trim();
		FLEFRecordHelper.updateChildValue(headerRecord, "COPYRIGHT", copyright);

		// --- HEADER NOTE ---
		String headerNote = headerNoteArea.getText().trim();
		FLEFRecordHelper.updateChildValue(headerRecord, "NOTE", headerNote);

		// --- SUBMITTER ---
		FLEFRecord submitter = FLEFRecord.createChild("SUBMITTER");
		headerRecord.addChild(submitter);

		// SUBMITTER NAME
		FLEFRecordHelper.updateChildValue(submitter, "NAME", submitterNameField.getText().trim());

		// SUBMITTER PLACE
		String address = submitterAddressField.getText().trim();
		String hierarchy = submitterHierarchyField.getText().trim();
		String lat = submitterLatitudeField.getText().trim();
		String lon = submitterLongitudeField.getText().trim();
		String placeNote = submitterPlaceNoteArea.getText().trim();

		if(!address.isEmpty() || !hierarchy.isEmpty() || !lat.isEmpty() || !lon.isEmpty() || !placeNote.isEmpty()){
			FLEFRecord place = FLEFRecord.createChild("PLACE");
			submitter.addChild(place);

			FLEFRecordHelper.updateChildValue(place, "ADDRESS", address);
			FLEFRecordHelper.updateChildValue(place, "HIERARCHY", hierarchy);
			if(!lat.isEmpty() && !lon.isEmpty()){
				FLEFRecord map = FLEFRecord.createChild("MAP");
				place.addChild(map);
				FLEFRecordHelper.updateChildValue(map, "LATITUDE", lat);
				FLEFRecordHelper.updateChildValue(map, "LONGITUDE", lon);
			}
			if(!placeNote.isEmpty())
				FLEFRecordHelper.updateChildValue(place, "NOTE", placeNote);
		}

		// CONTACT_STRUCTURE (0:M)
		for(FLEFRecord contact : contactRecords){
			contact.setTag("CONTACT");
			submitter.addChild(contact);
		}

		// SUBMITTER NOTE (0:M)
		for(String id : submitterNoteIds){
			FLEFRecordHelper.addChild(submitter, "NOTE", id);
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
		catch(Exception ignored){}

		FLEFModel model = new FLEFModel();
		HandlerRegistry.register(new NoteHandler());

		// Crea un header di esempio
		FLEFRecord header = FLEFRecord.createChild("HEADER");

		FLEFRecord protocol = FLEFRecord.createChild("PROTOCOL");
		header.addChild(protocol);
		FLEFRecordHelper.updateChildValue(protocol, "NAME", "FLEF");
		FLEFRecordHelper.updateChildValue(protocol, "VERSION", "0.0.9");

		FLEFRecord source = FLEFRecord.createChildWithValue("SOURCE", "MyApp");
		header.addChild(source);
		FLEFRecordHelper.updateChildValue(source, "NAME", "My Application");
		FLEFRecordHelper.updateChildValue(source, "VERSION", "1.0");

		FLEFRecordHelper.updateChildValue(header, "DATE", "2026-07-11");

		FLEFRecord submitter = FLEFRecord.createChild("SUBMITTER");
		header.addChild(submitter);
		FLEFRecordHelper.updateChildValue(submitter, "NAME", "Mauro Trevisan");

		model.setHeader(header);

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Header Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("Edit Header");
			btn.addActionListener(e -> {
				_HeaderDialog dialog = new _HeaderDialog(null, model, header);
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
