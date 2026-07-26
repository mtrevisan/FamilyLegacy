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
import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.ContactStructurePanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


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

	// Notes (using NoteListPanel)
	private final NoteListPanel notePanel;

	// Panels
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]5[]"));
	private final ModificationPanel modificationPanel;

	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	// ==================== Constructors ====================

	public static RepositoryDialog createNew(final Frame parent, final FLEFModel model){
		return new RepositoryDialog(parent, model, null);
	}

	public static RepositoryDialog createEdit(final Frame parent, final FLEFModel model, final FLEFRecord record){
		if(record == null){
			throw new IllegalArgumentException("Record cannot be null");
		}
		return new RepositoryDialog(parent, model, record);
	}

	private RepositoryDialog(final Frame parent, final FLEFModel model, final FLEFRecord record){
		super(parent, buildTitle(model, record), model, record);

		modificationPanel = new ModificationPanel(this);
		notePanel = new NoteListPanel(model, this);

		initComponents();
		loadData();
		pack();
		setLocationRelativeTo(parent);
	}

	private static String buildTitle(final FLEFModel model, final FLEFRecord record){
		return (record == null? "New Repository": "Edit Repository - " + record.getId());
	}

	// ==================== UI Initialization ====================

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
		final JPanel indPanel = new JPanel(new BorderLayout(5, 5));
		indPanel.add(custodianDisplayField, BorderLayout.CENTER);
		final JPanel indBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		indBtnPanel.add(custodianBrowseBtn);
		indBtnPanel.add(custodianEditBtn);
		indBtnPanel.add(custodianClearBtn);
		indPanel.add(indBtnPanel, BorderLayout.EAST);
		mainPanel.add(indPanel, "growx,wrap");

		// PLACE
		mainPanel.add(new JLabel("Place:"), "align label");
		placeDisplayField.setEditable(false);
		placeDisplayField.setBackground(UIManager.getColor("TextField.background"));
		final JPanel placePanel = new JPanel(new BorderLayout(5, 5));
		placePanel.add(placeDisplayField, BorderLayout.CENTER);
		final JPanel placeBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
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

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 5,fillx,top,wrap 1", "[grow]", "[]5[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		panel.add(createContactsPanel(), "growx");

		// Notes (using NoteListPanel)
		panel.add(notePanel, "growx");

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

	// ==================== Name Management ====================

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
		if(idx == -1){
			return;
		}

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
		if(nameRecord == null){
			return "[empty]";
		}

		final FLEFRecord valueRec = FLEFRecordUtils.findChild(nameRecord, "VALUE");
		final String text = valueRec != null? valueRec.getValue(): null;
		final String type = FLEFRecordUtils.getChildValue(nameRecord, "TYPE");

		final StringBuilder sb = new StringBuilder();
		if(StringUtils.isNotBlank(text)){
			sb.append(text);
		}
		else{
			sb.append("[unnamed]");
		}

		if(StringUtils.isNotBlank(type)){
			sb.append(" (").append(type).append(")");
		}
		return sb.toString();
	}

	private void removeName(){
		final int idx = nameList.getSelectedIndex();
		if(idx == -1){
			return;
		}
		if(!showConfirm("Confirm", "Remove this name?")){
			return;
		}
		nameRecords.remove(idx);
		nameListModel.remove(idx);
	}

	// ==================== Custodian & Place ====================

	private void browseIndividual(){
		final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler(IndividualHandler.TYPE);
		final GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			GUIHelper.getParentFrame(this), model, individualHandler, selectedId -> {
			if(selectedId != null){
				selectedCustodianId = selectedId;
				updateIndividualDisplay();
			}
		});
		dialog.setVisible(true);
	}

	private void editIndividual(){
		final IndividualDialog dialog = IndividualDialog.createEdit(GUIHelper.getParentFrame(this), model, placeStructureRecord);
		dialog.setVisible(true);
		updateIndividualDisplay();
	}

	private void updateIndividualDisplay(){
		if(selectedCustodianId != null && !selectedCustodianId.isEmpty()){
			final FLEFRecord rec = model.getRecordById(selectedCustodianId);
			final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler(IndividualHandler.TYPE);
			if(rec != null){
				custodianDisplayField.setText(individualHandler.getDisplayName(rec));
			}
			else{
				custodianDisplayField.setText(selectedCustodianId);
			}
		}
	}

	private void browsePlace(){
		final RecordTypeHandler<?> placeHandler = HandlerRegistry.getHandler(PlaceHandler.TYPE);
		final GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			GUIHelper.getParentFrame(this), model, placeHandler, selectedId -> {
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
		if(dialog.isSaved()){
			updatePlaceDisplay();
		}
	}

	private void updatePlaceDisplay(){
		if(placeStructureRecord != null && placeStructureRecord.getValue() != null){
			final String id = placeStructureRecord.getValue();
			final FLEFRecord rec = model.getRecordById(id);
			final RecordTypeHandler<?> placeHandler = HandlerRegistry.getHandler(PlaceHandler.TYPE);
			if(rec != null){
				placeDisplayField.setText(placeHandler.getDisplayName(rec));
			}
			else{
				placeDisplayField.setText(id);
			}
		}
	}

	// ==================== Contact Management ====================

	private String getContactDisplay(final FLEFRecord contact){
		final String address = contact.getValue();
		final String type = FLEFRecordUtils.getChildValue(contact, "TYPE");
		final String callerId = FLEFRecordUtils.getChildValue(contact, "CALLER_ID");
		final StringBuilder sb = new StringBuilder();
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
		for(final FLEFRecord child : record.getChildren()){
			if("CONTACT".equals(child.getTag())){
				contactRecords.add(child);
				contactListModel.addElement(getContactDisplay(child));
			}
		}
	}

	private void addContact(){
		final ContactStructurePanel panel = new ContactStructurePanel(model, this);
		final JDialog dialog = new JDialog(this, "Add Contact", true);
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
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);

		if(result[0] != null){
			contactRecords.add(result[0]);
			contactListModel.addElement(getContactDisplay(result[0]));
		}
	}

	private void editContact(){
		final int idx = contactList.getSelectedIndex();
		if(idx == -1){
			return;
		}
		final FLEFRecord existing = contactRecords.get(idx);

		final ContactStructurePanel panel = new ContactStructurePanel(model, this);
		panel.loadFromRecord(existing);

		final JDialog dialog = new JDialog(this, "Edit Contact", true);
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
		dialog.setMinimumSize(new Dimension(500, 450));
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);

		if(result[0] != null){
			contactRecords.set(idx, result[0]);
			contactListModel.set(idx, getContactDisplay(result[0]));
		}
	}

	private void removeContact(){
		final int idx = contactList.getSelectedIndex();
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

	// ==================== Load / Save ====================

	@Override
	protected void loadData(){
		// 1. NAME_STRUCTURE {1:M}
		nameRecords.clear();
		nameListModel.clear();
		for(final FLEFRecord child : record.getChildren()){
			if("NAME".equals(child.getTag())){
				nameRecords.add(child);
				nameListModel.addElement(getNameDisplay(child));
			}
		}

		// 2. CUSTODIAN
		final String indId = FLEFRecordUtils.getChildValue(record, "CUSTODIAN");
		if(indId != null && !indId.isEmpty()){
			selectedCustodianId = indId;
			updateIndividualDisplay();
		}

		// 3. PLACE_STRUCTURE
		placeStructureRecord = FLEFRecordUtils.findChild(record, "PLACE");
		updatePlaceDisplay();

		// 4. CONTACT_STRUCTURE
		loadContacts();

		// 5. NOTE (using NoteListPanel)
		final List<String> noteIds = new ArrayList<>();
		for(final FLEFRecord child : record.getChildren()){
			if("NOTE".equals(child.getTag()) && child.getValue() != null){
				noteIds.add(child.getValue());
			}
		}
		notePanel.loadFromNoteIds(noteIds);

		// 6. MODIFICATION
		modificationPanel.loadFromRecord(record);
	}

	@Override
	protected boolean validateData(){
		if(nameRecords.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"At least one NAME structure is required ({1:M}).",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
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
			nameRec.setTag("NAME");
			record.addChild(nameRec);
		}

		// 2. CUSTODIAN
		if(StringUtils.isNotBlank(selectedCustodianId)){
			FLEFRecordUtils.addChild(record, "CUSTODIAN", FLEFRecordUtils.formatXRef(selectedCustodianId));
		}

		// 3. PLACE
		FLEFRecordUtils.removeChildren(record, "PLACE");
		if(placeStructureRecord != null && placeStructureRecord.getValue() != null){
			placeStructureRecord.setTag("PLACE");
			record.addChild(placeStructureRecord);
		}

		// 4. CONTACT
		for(final FLEFRecord contact : contactRecords){
			contact.setTag("CONTACT");
			record.addChild(contact);
		}

		// 5. NOTE
		for(final String id : notePanel.getNoteIds()){
			FLEFRecordUtils.addChild(record, "NOTE", FLEFRecordUtils.formatXRef(id));
		}

		// 6. MODIFICATION
		modificationPanel.saveToRecord(record);

		if(isNew){
			model.addRecord(record);
		}

		// TODO to be removed
		FLEFFile.print(model);
		// dispose();
	}

	@Override
	protected FLEFRecord createNewRecord(){
		return FLEFRecord.createMainRecord(generateNewId(), RepositoryHandler.TYPE);
	}

	@Override
	protected String generateNewId(){
		return FLEFRecordUtils.generateNewId(model, RepositoryHandler.TYPE, RepositoryHandler.ID_PREFIX);
	}

	// ==================== Main for testing ====================

	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){
		}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final RepositoryDialog dialog = RepositoryDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
