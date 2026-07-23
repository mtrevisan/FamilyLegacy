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

import io.github.mtrevisan.familylegacy.v2.io.FLEFFile;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.ContactStructurePanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Dialog for editing a {@code REPOSITORY_RECORD} according to FLEF 0.1.0.
 * <p>
 * Structure:
 * <pre>
 * REPOSITORY_RECORD :=
 * n @<XREF:REPOSITORY>@ REPOSITORY    {1:1}
 *   +1 <<NAME_STRUCTURE>>    {1:M}
 *   +1 CUSTODIAN @<XREF:INDIVIDUAL>@    {0:1}
 *   +1 <<PLACE_STRUCTURE>>    {0:1}
 *   +1 <<CONTACT_STRUCTURE>>    {0:M}
 *   +1 NOTE @<XREF:NOTE>@    {0:M}
 *   +1 <<RESTRICTION_STRUCTURE>>    {0:1}
 *   +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class RepositoryDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 3053114409506763765L;


	// Handlers
	static{
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new NoteHandler());
	}

	// UI components: Names {1:M}
	private final DefaultListModel<String> nameListModel = new DefaultListModel<>();
	private final JList<String> nameList = new JList<>(nameListModel);
	private final List<FLEFRecord> nameRecords = new ArrayList<>();

	// UI components: Custodian & Place
	private final JTextField custodianDisplayField = new JTextField(20);
	private final JButton custodianBrowseBtn = new JButton("Browse...");
	private final JButton custodianEditBtn = new JButton("Edit...");
	private final JButton custodianClearBtn = new JButton("Clear");
	private String selectedCustodianId;

	private final JTextField placeDisplayField = new JTextField(20);
	private final JButton placeBrowseBtn = new JButton("Browse...");
	private final JButton placeEditBtn = new JButton("Edit...");
	private final JButton placeClearBtn = new JButton("Clear");
	private FLEFRecord placeStructureRecord;

	// Contacts
	private final DefaultListModel<String> contactListModel = new DefaultListModel<>();
	private final JList<String> contactList = new JList<>(contactListModel);
	private final List<FLEFRecord> contactRecords = new ArrayList<>();

	// Notes
	private final DefaultListModel<String> noteListModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteListModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	// Panels
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]5[]"));
	private final ModificationPanel modificationPanel;

	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");


	public static RepositoryDialog createNew(final Frame parent, final FLEFModel model){
		return new RepositoryDialog(parent, model, null);
	}

	public static RepositoryDialog createEdit(final Frame parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new RepositoryDialog(parent, model, record);
	}


	private RepositoryDialog(final Frame parent, final FLEFModel model, final FLEFRecord record){
		super(parent, buildTitle(model, record), model, record);

		modificationPanel = new ModificationPanel(this);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	private static String buildTitle(final FLEFModel model, final FLEFRecord record){
		return (record == null
			? "New Repository"
			: "Edit Repository - " + record.getId());
	}

	@Override
	protected void initComponents(){
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());

		final JPanel modificationContainer = new JPanel(new MigLayout("top", "[grow]", "[grow]"));
		modificationContainer.add(modificationPanel, "grow");
		tabbedPane.addTab("Modification", modificationContainer);

		setLayout(new MigLayout("fillx,top"));
		add(tabbedPane, "growx");

		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		saveButton.addActionListener(e -> save());
		cancelButton.addActionListener(e -> dispose());
	}

	private JPanel createMainPanel(){
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// NAME_STRUCTURE {1:M}
		mainPanel.add(createNamesPanel(), "span 2,growx,wrap");

		// CUSTODIAN
		mainPanel.add(new JLabel("Custodian:"), "align label");
		custodianDisplayField.setEditable(false);
		custodianDisplayField.setBackground(UIManager.getColor("TextField.background"));
		JPanel indPanel = new JPanel(new BorderLayout(5, 5));
		indPanel.add(custodianDisplayField, BorderLayout.CENTER);
		JPanel indBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		indBtnPanel.add(custodianBrowseBtn);
		indBtnPanel.add(custodianEditBtn);
		indBtnPanel.add(custodianClearBtn);
		indPanel.add(indBtnPanel, BorderLayout.EAST);
		mainPanel.add(indPanel, "growx,wrap");

		// PLACE
		mainPanel.add(new JLabel("Place:"), "align label");
		placeDisplayField.setEditable(false);
		placeDisplayField.setBackground(UIManager.getColor("TextField.background"));
		JPanel placePanel = new JPanel(new BorderLayout(5, 5));
		placePanel.add(placeDisplayField, BorderLayout.CENTER);
		JPanel placeBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		placeBtnPanel.add(placeBrowseBtn);
		placeBtnPanel.add(placeEditBtn);
		placeBtnPanel.add(placeClearBtn);
		placePanel.add(placeBtnPanel, BorderLayout.EAST);
		mainPanel.add(placePanel, "growx");

		// Listeners
		custodianBrowseBtn.addActionListener(e -> browseIndividual());
		custodianEditBtn.addActionListener(e -> editIndividual());
		custodianClearBtn.addActionListener(e -> {
			selectedCustodianId = null;
			custodianDisplayField.setText(StringUtils.EMPTY);
		});
		placeBrowseBtn.addActionListener(e -> browsePlace());
		placeEditBtn.addActionListener(e -> editPlace());
		placeClearBtn.addActionListener(e -> {
			placeStructureRecord = null;
			placeDisplayField.setText(StringUtils.EMPTY);
		});

		return mainPanel;
	}


	private JPanel createNamesPanel(){
		final JPanel panel = new JPanel(new MigLayout("fillx,top"));
		panel.setBorder(new TitledBorder("Names*"));

		nameList.setVisibleRowCount(3);
		nameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		GUIHelper.installBehavior(nameList,
			() -> nameList.getSelectedIndex() >= 0,
			this::editName,
			this::addName,
			this::removeName,
			builder -> {
				builder.item("Add Name...", this::addName);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editName);
				builder.selectionSensitiveItem("Remove", this::removeName);
			});

		final JScrollPane scrollPane = GUIHelper.createScrollPane(nameList);
		panel.add(scrollPane, "growx,wrap");

		return panel;
	}

	private void addName(){
		final NameStructureDialog dialog = new NameStructureDialog(this, model, null);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			final FLEFRecord nameRecord = dialog.getNameRecord();
			nameRecords.add(nameRecord);
			nameListModel.addElement(getNameDisplay(nameRecord));
		}
	}

	private void editName(){
		final int idx = nameList.getSelectedIndex();
		if(idx == -1)
			return;

		final FLEFRecord existing = nameRecords.get(idx);
		final NameStructureDialog dialog = new NameStructureDialog(this, model, existing);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			final FLEFRecord updated = dialog.getNameRecord();
			nameRecords.set(idx, updated);
			nameListModel.set(idx, getNameDisplay(updated));
		}
	}

	private String getNameDisplay(final FLEFRecord nameRecord){
		if(nameRecord == null)
			return "[empty]";

		final FLEFRecord valueRec = FLEFRecordUtils.findChild(nameRecord, "VALUE");
		final String text = valueRec != null ? valueRec.getValue() : null;
		final String type = FLEFRecordUtils.getChildValue(nameRecord, "TYPE");

		final StringBuilder sb = new StringBuilder();
		if(StringUtils.isNotBlank(text))
			sb.append(text);
		else
			sb.append("[unnamed]");

		if(StringUtils.isNotBlank(type))
			sb.append(" (").append(type).append(")");
		return sb.toString();
	}

	private void removeName(){
		int idx = nameList.getSelectedIndex();
		if(idx == -1){
			return;
		}
		if(!showConfirm("Confirm", "Remove this name?")){
			return;
		}
		nameRecords.remove(idx);
		nameListModel.remove(idx);
	}

	private void showNameEditDialog(FLEFRecord existing, int index){
		JDialog dialog = new JDialog(this, existing == null ? "Add Name" : "Edit Name", true);
		dialog.setLayout(new BorderLayout(10, 10));

		JPanel formPanel = new JPanel(new MigLayout("ins 10, fillx, top", "[right]rel[grow]", "[]5[]5[]"));

		JTextField textValField = new JTextField(20);
		JComboBox<String> typeCombo = new JComboBox<>(new String[]{"", "official", "colonial", "indigenous"});
		typeCombo.setEditable(true);
		JTextField localeField = new JTextField(10);

		if(existing != null){
			FLEFRecord valRec = FLEFRecordUtils.findChild(existing, "VALUE");
			if(valRec != null){
				textValField.setText(valRec.getValue());
				FLEFRecord locRec = FLEFRecordUtils.findChild(valRec, "LOCALE");
				if(locRec != null){
					localeField.setText(locRec.getValue());
				}
			}
			FLEFRecord typeRec = FLEFRecordUtils.findChild(existing, "TYPE");
			if(typeRec != null){
				typeCombo.setSelectedItem(typeRec.getValue());
			}
		}

		formPanel.add(new JLabel("Name Value*:"), "align label");
		formPanel.add(textValField, "growx, wrap");

		formPanel.add(new JLabel("Type:"), "align label");
		formPanel.add(typeCombo, "growx, wrap");

		formPanel.add(new JLabel("Locale (BCP 47):"), "align label");
		formPanel.add(localeField, "growx, wrap");

		dialog.add(formPanel, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		dialog.add(btnPanel, BorderLayout.SOUTH);

		okBtn.addActionListener(e -> {
			String nameVal = textValField.getText().trim();
			if(nameVal.isEmpty()){
				JOptionPane.showMessageDialog(dialog, "Name Value is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			FLEFRecord nameRec = (existing != null) ? existing : new FLEFRecord();
			FLEFRecordUtils.removeAllChildren(nameRec);

			nameRec.setLevel(1);
			nameRec.setTag("NAME");

			// VALUE
			FLEFRecord valRec = new FLEFRecord();
			valRec.setLevel(2);
			valRec.setTag("VALUE");
			valRec.setValue(nameVal);

			String locVal = localeField.getText().trim();
			if(!locVal.isEmpty()){
				FLEFRecord locRec = new FLEFRecord();
				locRec.setLevel(3);
				locRec.setTag("LOCALE");
				locRec.setValue(locVal);
				valRec.addChild(locRec);
			}
			nameRec.addChild(valRec);

			// TYPE
			String typeVal = ((String)typeCombo.getSelectedItem()).trim();
			if(!typeVal.isEmpty()){
				FLEFRecord typeRec = new FLEFRecord();
				typeRec.setLevel(2);
				typeRec.setTag("TYPE");
				typeRec.setValue(typeVal);
				nameRec.addChild(typeRec);
			}

			if(existing == null){
				nameRecords.add(nameRec);
				nameListModel.addElement(getNameDisplay(nameRec));
			}
			else{
				nameRecords.set(index, nameRec);
				nameListModel.set(index, getNameDisplay(nameRec));
			}

			dialog.dispose();
		});

		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setMinimumSize(new Dimension(400, 200));
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}

	// --- REFERENCES PANEL ---

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 5,fillx,top,wrap 1", "[grow]", "[]5[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		panel.add(createContactsPanel(), "growx");
		panel.add(createNotesPanel(), "growx");

		return panel;
	}

	private JPanel createContactsPanel(){
		final JPanel panel = new JPanel(new MigLayout("fillx,top"));
		panel.setBorder(new TitledBorder("Contact"));

		contactList.setVisibleRowCount(4);
		contactList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		GUIHelper.installBehavior(contactList,
			() -> contactList.getSelectedIndex() >= 0,
			this::editContact,
			this::addContact,
			this::removeContact,
			builder -> {
				builder.item("New...", this::newContact);
				builder.item("Add Existing...", this::addContact);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editContact);
				builder.selectionSensitiveItem("Remove", this::removeContact);
			});

		final JScrollPane scrollPane = GUIHelper.createScrollPane(contactList);
		panel.add(scrollPane, "growx,wrap");

		return panel;
	}

	private JPanel createNotesPanel(){
		final JPanel panel = new JPanel(new MigLayout("fillx,top"));
		panel.setBorder(new TitledBorder("Note"));

		noteList.setVisibleRowCount(4);
		noteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		GUIHelper.installBehavior(noteList,
			() -> noteList.getSelectedIndex() >= 0,
			this::editNote,
			this::addNote,
			this::removeNote,
			builder -> {
				builder.item("New...", this::newNote);
				builder.item("Add Existing...", this::addNote);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editNote);
				builder.selectionSensitiveItem("Remove", this::removeNote);
			});

		final JScrollPane scrollPane = GUIHelper.createScrollPane(noteList);
		panel.add(scrollPane, "growx,wrap");

		return panel;
	}

	// --- HANDLERS & BROWSE ---

	private void browseIndividual(){
		final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler(IndividualHandler.TYPE);
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, individualHandler, selectedId -> {
			if(selectedId != null){
				selectedCustodianId = selectedId;
				updateIndividualDisplay();
			}
		});
		dialog.setVisible(true);
	}

	private void editIndividual(){
		final IndividualDialog dialog = IndividualDialog.createEdit(getParentFrame(), model, placeStructureRecord);
		dialog.setVisible(true);

		updateIndividualDisplay();
	}

	private void updateIndividualDisplay(){
		if(selectedCustodianId != null && !selectedCustodianId.isEmpty()){
			final FLEFRecord rec = model.getRecordById(selectedCustodianId);
			final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler(IndividualHandler.TYPE);
			if(rec != null)
				custodianDisplayField.setText(individualHandler.getDisplayName(rec));
			else
				custodianDisplayField.setText(selectedCustodianId);
		}
	}

	private void browsePlace(){
		final RecordTypeHandler<?> placeHandler = HandlerRegistry.getHandler(PlaceHandler.TYPE);
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, placeHandler, selectedId -> {
			if(selectedId != null){
				placeStructureRecord = model.getRecordById(selectedId);
				updatePlaceDisplay();
			}
		});
		dialog.setVisible(true);
	}

	private void editPlace(){
		final PlaceStructureDialog dialog = new PlaceStructureDialog(this, model, placeStructureRecord);
		dialog.setVisible(true);

		if(dialog.isSaved())
			updatePlaceDisplay();
	}

	private void updatePlaceDisplay(){
		if(placeStructureRecord != null && placeStructureRecord.getValue() != null){
			final String id = placeStructureRecord.getValue();
			final FLEFRecord rec = model.getRecordById(id);
			final RecordTypeHandler<?> placeHandler = HandlerRegistry.getHandler(PlaceHandler.TYPE);
			if(rec != null)
				placeDisplayField.setText(placeHandler.getDisplayName(rec));
			else
				placeDisplayField.setText(id);
		}
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
			if(!sb.isEmpty()){
				sb.append(" (");
			}
			sb.append(type);
			if(!sb.isEmpty() && !sb.toString().endsWith("(")){
				sb.append(")");
			}
		}
		if(callerId != null && !callerId.isEmpty()){
			if(!sb.isEmpty()){
				sb.append(" - ");
			}
			sb.append(callerId);
		}
		if(sb.isEmpty()){
			sb.append("[empty]");
		}
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
		if(idx == -1){
			return;
		}
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

	private void removeContact(){
		int idx = contactList.getSelectedIndex();
		if(idx == -1){
			return;
		}
		if(!showConfirm("Confirm", "Remove this contact?")){
			return;
		}
		contactRecords.remove(idx);
		contactListModel.remove(idx);
	}

	private void newContact(){
		addContact();
	}

	private String getNoteDisplayName(String id){
		final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return noteHandler.getDisplayName(rec);
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
		final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, noteHandler, selectedId -> {
			if(selectedId != null && !noteIds.contains(selectedId)){
				noteIds.add(selectedId);
				String display = getNoteDisplayName(selectedId);
				noteDisplayMap.put(selectedId, display);
				noteListModel.addElement(display);
			}
		});
		dialog.setVisible(true);
	}

	private void editNote(){
		final int idx = noteList.getSelectedIndex();
		if(idx == -1){
			return;
		}

		final String id = noteIds.get(idx);
		final FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(this, "Note not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);
		final JDialog dialog = noteHandler.createEditDialog(getParentFrame(), model, rec);
		dialog.setVisible(true);

		final String newDisplay = getNoteDisplayName(id);
		noteDisplayMap.put(id, newDisplay);
		noteListModel.set(idx, newDisplay);
	}

	private void removeNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1){
			return;
		}
		if(!showConfirm("Confirm", "Remove this note reference?")){
			return;
		}
		String removedId = noteIds.remove(idx);
		noteDisplayMap.remove(removedId);
		noteListModel.remove(idx);
	}

	private void newNote(){
		final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);
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


	@Override
	protected void loadData(){
		// 1. NAME_STRUCTURE {1:M}
		nameRecords.clear();
		nameListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("NAME".equals(child.getTag())){
				nameRecords.add(child);
				nameListModel.addElement(getNameDisplay(child));
			}
		}

		// 2. CUSTODIAN
		String indId = FLEFRecordUtils.getChildValue(record, "CUSTODIAN");
		if(indId != null && !indId.isEmpty()){
			selectedCustodianId = indId;
			updateIndividualDisplay();
		}

		// 3. PLACE_STRUCTURE
		placeStructureRecord = FLEFRecordUtils.findChild(record, "PLACE");
		updatePlaceDisplay();

		// 4. CONTACT_STRUCTURE
		loadContacts();

		// 5. NOTE
		loadNotes();

		// 6. MODIFICATION
		modificationPanel.loadFromRecord(record);
	}

	@Override
	protected boolean validateData(){
		if(nameRecords.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"At least one NAME structure is required ({1:M}).",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			// Ensure the tab containing valueArea is visible
			tabbedPane.setSelectedComponent(mainPanel);

			return false;
		}

		return true;
	}

	@Override
	protected void saveRecord(){
		FLEFRecordUtils.removeAllChildren(record);

		// 1. NAME
		for(final FLEFRecord nameRec : nameRecords){
			nameRec.setLevel(1);
			nameRec.setTag("NAME");
			record.addChild(nameRec);
		}

		// 2. CUSTODIAN
		if(StringUtils.isNotBlank(selectedCustodianId))
			FLEFRecordUtils.addChild(record, "CUSTODIAN", FLEFRecordUtils.formatXRef(selectedCustodianId));

		// 3. PLACE
		FLEFRecordUtils.removeChildren(record, "PLACE");
		if(placeStructureRecord != null && placeStructureRecord.getValue() != null){
			placeStructureRecord.setLevel(1);
			placeStructureRecord.setTag("PLACE");
			record.addChild(placeStructureRecord);
		}

		// 4. CONTACT
		for(final FLEFRecord contact : contactRecords){
			contact.setLevel(1);
			contact.setTag("CONTACT");
			record.addChild(contact);
		}

		// 5. NOTE
		for(final String id : noteIds)
			FLEFRecordUtils.addChild(record, "NOTE", FLEFRecordUtils.formatXRef(id));

		// 6. MODIFICATION
		modificationPanel.saveToRecord(record);

		if(isNew){
			model.addRecord(record);
		}

//TODO to be removed
FLEFFile.print(model);
//		dispose();
	}


	@Override
	protected FLEFRecord createNewRecord(){
		return FLEFRecord.createMainRecord(generateNewId(), RepositoryHandler.TYPE);
	}

	@Override
	protected String generateNewId(){
		return FLEFRecordUtils.generateNewId(model, RepositoryHandler.TYPE, RepositoryHandler.ID_PREFIX);
	}


	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			RepositoryDialog dialog = RepositoryDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
