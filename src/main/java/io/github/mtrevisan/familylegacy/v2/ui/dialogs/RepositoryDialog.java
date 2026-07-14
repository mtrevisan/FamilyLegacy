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
import io.github.mtrevisan.familylegacy.v2.ui.components.ContactStructurePanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CalendarHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.FamilyHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
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
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
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
 * Dialog for editing a REPOSITORY_RECORD according to FLEF 0.0.9.
 * <p>
 * Structure:
 * <pre>
 * REPOSITORY_RECORD :=
 *   n @<XREF:REPOSITORY>@ REPOSITORY    {1:1}
 *     +1 NAME <NAME_OF_REPOSITORY>    {1:1}
 *     +1 INDIVIDUAL @<XREF:INDIVIDUAL>@    {0:1}
 *     +1 PLACE @<XREF:PLACE>@    {0:1}
 *     +1 <<CONTACT_STRUCTURE>>    {0:M}
 *     +1 NOTE @<XREF:NOTE>@    {0:M}
 *     +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class RepositoryDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 3053114409506763765L;


	static{
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new RepositoryHandler());
		HandlerRegistry.register(new CalendarHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new CulturalNormHandler());
		HandlerRegistry.register(new FamilyHandler());
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new EventHandler());
	}

	// ========== Basic fields ==========
	private final JTextField idField = new JTextField(10);
	private final JTextField nameField = new JTextField(30);

	// ========== INDIVIDUAL (0:1) ==========
	private final JTextField individualDisplayField = new JTextField(20);
	private final JButton individualBrowseBtn = new JButton("Browse...");
	private final JButton individualClearBtn = new JButton("Clear");
	private String selectedIndividualId;

	// ========== PLACE (0:1) ==========
	private final JTextField placeDisplayField = new JTextField(20);
	private final JButton placeBrowseBtn = new JButton("Browse...");
	private final JButton placeClearBtn = new JButton("Clear");
	private String selectedPlaceId;

	// ========== CONTACT_STRUCTURE (0:M) ==========
	private final DefaultListModel<String> contactListModel = new DefaultListModel<>();
	private final JList<String> contactList = new JList<>(contactListModel);
	private final List<FLEFRecord> contactRecords = new ArrayList<>();

	// ========== NOTE (0:M) ==========
	private final DefaultListModel<String> noteListModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteListModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	// ========== MODIFICATION (1:1) ==========
	private final ModificationPanel modificationPanel;

	// ========== Buttons ==========
	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	// ========== Handlers ==========
	private final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler("INDIVIDUAL");
	private final RecordTypeHandler<?> placeHandler = HandlerRegistry.getHandler("PLACE");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");

	// ==================== Constructors ====================
	public RepositoryDialog(Frame parent, FLEFModel model, FLEFRecord record){
		super(parent, "Edit Repository", model, record);

		this.modificationPanel = new ModificationPanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(900, 750));
		pack();
		setLocationRelativeTo(parent);
	}

	public RepositoryDialog(Frame parent, FLEFModel model){
		super(parent, "New Repository", model, null);

		this.modificationPanel = new ModificationPanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(900, 750));
		pack();
		setLocationRelativeTo(parent);
	}

	// ==================== UI Initialization ====================
	@Override
	protected void initComponents(){
		setLayout(new BorderLayout(10, 10));

		JTabbedPane tabbedPane = new JTabbedPane();

		// --- Basic tab ---
		tabbedPane.addTab("Basic", createBasicPanel());

		// --- Contacts tab ---
		tabbedPane.addTab("Contacts", createContactsPanel());

		// --- Notes tab ---
		tabbedPane.addTab("Notes", createNotesPanel());

		// --- Modification tab ---
		tabbedPane.addTab("Modification", modificationPanel);

		add(tabbedPane, BorderLayout.CENTER);

		// --- Button panel ---
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		saveButton.addActionListener(e -> save());
		cancelButton.addActionListener(e -> dispose());
	}

	// ==================== Panel factories ====================

	private JPanel createBasicPanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]10[]10[]10[]10[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// ID (read-only)
		idField.setEditable(false);
		idField.setText(record.getId());
		panel.add(new JLabel("ID:"), "align label");
		panel.add(idField, "growx,wrap");

		// NAME (1:1) - marked with an asterisk
		panel.add(new JLabel("Name*:"), "align label");
		panel.add(nameField, "growx,wrap");

		// INDIVIDUAL (0:1)
		panel.add(new JLabel("Individual:"), "align label");
		individualDisplayField.setEditable(false);
		individualDisplayField.setBackground(UIManager.getColor("TextField.background"));
		JPanel indPanel = new JPanel(new BorderLayout(5, 5));
		indPanel.add(individualDisplayField, BorderLayout.CENTER);
		JPanel indBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		indBtnPanel.add(individualBrowseBtn);
		indBtnPanel.add(individualClearBtn);
		indPanel.add(indBtnPanel, BorderLayout.EAST);
		panel.add(indPanel, "growx,wrap");

		// PLACE (0:1)
		panel.add(new JLabel("Place:"), "align label");
		placeDisplayField.setEditable(false);
		placeDisplayField.setBackground(UIManager.getColor("TextField.background"));
		JPanel placePanel = new JPanel(new BorderLayout(5, 5));
		placePanel.add(placeDisplayField, BorderLayout.CENTER);
		JPanel placeBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		placeBtnPanel.add(placeBrowseBtn);
		placeBtnPanel.add(placeClearBtn);
		placePanel.add(placeBtnPanel, BorderLayout.EAST);
		panel.add(placePanel, "growx");

		// Listeners
		individualBrowseBtn.addActionListener(e -> browseIndividual());
		individualClearBtn.addActionListener(e -> {
			selectedIndividualId = null;
			individualDisplayField.setText("");
		});
		placeBrowseBtn.addActionListener(e -> browsePlace());
		placeClearBtn.addActionListener(e -> {
			selectedPlaceId = null;
			placeDisplayField.setText("");
		});

		return panel;
	}

	private JPanel createContactsPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder("Contact"));

		contactList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		contactList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editContact();
				}
			}
		});
		JScrollPane scrollPane = new JScrollPane(contactList);
		scrollPane.setPreferredSize(new Dimension(200, 100));
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton newBtn = new JButton("New Contact");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(newBtn);
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
		newBtn.addActionListener(e -> createNewContact());
		editBtn.addActionListener(e -> editContact());
		deleteBtn.addActionListener(e -> deleteContact());

		return panel;
	}

	private JPanel createNotesPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder("Note"));

		noteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		noteList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editNote();
				}
			}
		});
		JScrollPane scrollPane = new JScrollPane(noteList);
		scrollPane.setPreferredSize(new Dimension(200, 80));
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

		noteList.addListSelectionListener(e -> {
			boolean selected = noteList.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addNote());
		newBtn.addActionListener(e -> createNewNote());
		editBtn.addActionListener(e -> editNote());
		deleteBtn.addActionListener(e -> deleteNote());

		return panel;
	}

	// ==================== Individual methods ====================

	private void browseIndividual(){
		if(individualHandler == null){
			JOptionPane.showMessageDialog(this, "Individual handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, individualHandler, selectedId -> {
			if(selectedId != null){
				selectedIndividualId = selectedId;
				FLEFRecord rec = model.getRecordById(selectedId);
				if(rec != null){
					individualDisplayField.setText(individualHandler.getDisplayName(rec));
				}
				else{
					individualDisplayField.setText(selectedId);
				}
			}
		}
		);
		dialog.setVisible(true);
	}

	// ==================== Place methods ====================

	private void browsePlace(){
		if(placeHandler == null){
			JOptionPane.showMessageDialog(this, "Place handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, placeHandler, selectedId -> {
			if(selectedId != null){
				selectedPlaceId = selectedId;
				FLEFRecord rec = model.getRecordById(selectedId);
				if(rec != null){
					placeDisplayField.setText(placeHandler.getDisplayName(rec));
				}
				else{
					placeDisplayField.setText(selectedId);
				}
			}
		}
		);
		dialog.setVisible(true);
	}

	// ==================== Contact methods ====================

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
		for(FLEFRecord child : record.getChildren()){
			if("CONTACT".equals(child.getTag())){
				contactRecords.add(child);
				contactListModel.addElement(getContactDisplay(child));
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
					contact.setLevel(1);
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
					contact.setLevel(1);
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

	private void createNewContact(){
		addContact();
	}

	// ==================== Note methods ====================

	private String getNoteDisplayName(String id){
		if(noteHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null){
				return noteHandler.getDisplayName(rec);
			}
		}
		return id;
	}

	private void loadNotes(){
		noteListModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
		for(FLEFRecord child : record.getChildren()){
			if("NOTE".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteListModel.addElement(display);
			}
		}
	}

	private void addNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(this, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, noteHandler, selectedId -> {
			if(selectedId != null && !noteIds.contains(selectedId)){
				noteIds.add(selectedId);
				String display = getNoteDisplayName(selectedId);
				noteDisplayMap.put(selectedId, display);
				noteListModel.addElement(display);
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1)
			return;
		String id = noteIds.get(idx);
		if(noteHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(this, "Note not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = noteHandler.createEditDialog(getParentFrame(), model, rec);
		dialog.setVisible(true);
		String newDisplay = getNoteDisplayName(id);
		noteDisplayMap.put(id, newDisplay);
		noteListModel.set(idx, newDisplay);
	}

	private void deleteNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1)
			return;
		if(!showConfirm("Confirm", "Remove this note reference?"))
			return;
		String removedId = noteIds.remove(idx);
		noteDisplayMap.remove(removedId);
		noteListModel.remove(idx);
	}

	private void createNewNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(this, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>(noteIds);
		JDialog dialog = noteHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
		for(FLEFRecord rec : model.getRecordsByType("NOTE")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !noteIds.contains(id)){
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteListModel.addElement(display);
				break;
			}
		}
	}

	// ==================== Load Data ====================

	@Override
	protected void loadData(){
		idField.setText(record.getId());

		// NAME (1:1)
		nameField.setText(FLEFRecordUtils.getChildValue(record, "NAME"));

		// INDIVIDUAL (0:1)
		String indId = FLEFRecordUtils.getChildValue(record, "INDIVIDUAL");
		if(indId != null && !indId.isEmpty()){
			selectedIndividualId = indId;
			FLEFRecord rec = model.getRecordById(indId);
			if(rec != null && individualHandler != null){
				individualDisplayField.setText(individualHandler.getDisplayName(rec));
			}
			else{
				individualDisplayField.setText(indId);
			}
		}

		// PLACE (0:1)
		String placeId = FLEFRecordUtils.getChildValue(record, "PLACE");
		if(placeId != null && !placeId.isEmpty()){
			selectedPlaceId = placeId;
			FLEFRecord rec = model.getRecordById(placeId);
			if(rec != null && placeHandler != null){
				placeDisplayField.setText(placeHandler.getDisplayName(rec));
			}
			else{
				placeDisplayField.setText(placeId);
			}
		}

		// CONTACT_STRUCTURE (0:M)
		loadContacts();

		// NOTE (0:M)
		loadNotes();

		// MODIFICATION (1:1)
		modificationPanel.loadFromRecord(record);
	}

	// ==================== Validation ====================

	@Override
	protected boolean validateData(){
		// NAME (1:1) - required
		if(nameField.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(this,
				"NAME is required.\nPlease enter a repository name.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			nameField.requestFocusInWindow();
			return false;
		}

		// MODIFICATION_STRUCTURE (1:1) - required
		if(!modificationPanel.hasData()){
			JOptionPane.showMessageDialog(this,
				"Modification is required.\nPlease add a CREATION date.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return modificationPanel.validateRequiredFields();
	}

	// ==================== Save ====================

	@Override
	protected void saveRecord(){
		// Validation is already done by save() before calling this method
		record.getChildren().clear();

		// NAME (1:1)
		String name = nameField.getText().trim();
		if(!name.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "NAME", name);
		}

		// INDIVIDUAL (0:1)
		if(selectedIndividualId != null && !selectedIndividualId.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "INDIVIDUAL", selectedIndividualId);
		}

		// PLACE (0:1)
		if(selectedPlaceId != null && !selectedPlaceId.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "PLACE", selectedPlaceId);
		}

		// CONTACT_STRUCTURE (0:M)
		for(FLEFRecord contact : contactRecords){
			contact.setLevel(1);
			contact.setTag("CONTACT");
			record.addChild(contact);
		}

		// NOTE (0:M)
		for(String id : noteIds){
			FLEFRecordUtils.addChild(record, "NOTE", 1, id);
		}

		// MODIFICATION (1:1)
		modificationPanel.saveToRecord(record);

		if(isNew){
			model.addRecord(record);
		}
		dispose();
	}

	// ==================== Overrides ====================

	@Override
	protected FLEFRecord createNewRecord(){
		FLEFRecord newRecord = new FLEFRecord();
		newRecord.setType("REPOSITORY");
		newRecord.setId(generateNewId());
		return newRecord;
	}

	@Override
	protected String generateNewId(){
		return FLEFRecordUtils.generateNewId(model, "REPOSITORY", "R");
	}

	private Frame getParentFrame(){
		Container parent = getParent();
		while(parent != null && !(parent instanceof Frame)){
			parent = parent.getParent();
		}
		return (Frame)parent;
	}

	// ==================== Main per test ====================

	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();

		// Aggiungi un individuo di esempio
		FLEFRecord ind = new FLEFRecord();
		ind.setId("I1");
		ind.setType("INDIVIDUAL");
		FLEFRecord name = new FLEFRecord();
		name.setLevel(1);
		name.setTag("NAME");
		FLEFRecord given = new FLEFRecord();
		given.setLevel(2);
		given.setTag("INDIVIDUAL_NAME");
		given.setValue("John");
		name.addChild(given);
		FLEFRecord family = new FLEFRecord();
		family.setLevel(2);
		family.setTag("FAMILY_NAME");
		family.setValue("Doe");
		name.addChild(family);
		ind.addChild(name);
		model.addRecord(ind);

		// Aggiungi un place di esempio
		FLEFRecord place = new FLEFRecord();
		place.setId("P1");
		place.setType("PLACE");
		FLEFRecord placeName = new FLEFRecord();
		placeName.setLevel(1);
		placeName.setTag("NAME");
		placeName.setValue("Rome");
		place.addChild(placeName);
		model.addRecord(place);

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Repository Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New Repository");
			btn.addActionListener(e -> {
				RepositoryDialog dialog = new RepositoryDialog(frame, model);
				dialog.setVisible(true);
				System.out.println("Repository saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
