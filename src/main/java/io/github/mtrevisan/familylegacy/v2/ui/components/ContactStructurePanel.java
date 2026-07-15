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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.TranscribedTextDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Component;
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
 * Reusable panel for editing a CONTACT_STRUCTURE.
 * <p>
 * Structure:
 * <pre>
 * CONTACT_STRUCTURE :=
 *   n CONTACT <CONTACT_ADDRESS>    {1:1}
 *     +1 TYPE <CONTACT_TYPE>    {0:1}
 *     +1 CALLER_ID <CALLED_ID_VALUE>    {0:1}
 *       +2 <<TRANSCRIBED_TEXT>>    {0:M}
 *     +1 NOTE @<XREF:NOTE>@    {0:M}
 *     +1 RESTRICTION <confidential>    {0:1}
 *     +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class ContactStructurePanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -4578408607463890901L;


	private final FLEFModel model;
	private final Component parent;

	// ========== CONTACT (1:1) ==========
	private final JTextField contactField = new JTextField(30);

	// ========== TYPE (0:1) ==========
	private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{
		"", "work", "home", "blog", "personal", "social", "mobile", "fax"
	});

	// ========== CALLER_ID (0:1) ==========
	private final JTextField callerIdField = new JTextField(20);

	// ========== CALLER_ID -> TRANSCRIBED_TEXT (0:M) ==========
	private final DefaultListModel<String> transcriptionModel = new DefaultListModel<>();
	private final JList<String> transcriptionList = new JList<>(transcriptionModel);
	private final List<FLEFRecord> transcriptionRecords = new ArrayList<>();

	// ========== NOTE (0:M) ==========
	private final DefaultListModel<String> noteModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	// ========== RESTRICTION (0:1) ==========
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	// ========== MODIFICATION_STRUCTURE (1:1) ==========
	private final ModificationPanel modificationPanel;

	// ========== Handlers ==========
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");

	/**
	 * Creates a new ContactStructurePanel.
	 *
	 * @param model  the FLEF model
	 * @param parent the parent component (for showing dialogs)
	 */
	public ContactStructurePanel(FLEFModel model, Component parent){
		this.model = model;
		this.parent = parent;
		this.modificationPanel = new ModificationPanel(model, parent);
		initComponents();
	}

	private void initComponents(){
		setLayout(new BorderLayout(5, 5));

		transcriptionList.setVisibleRowCount(4);
		noteList.setVisibleRowCount(4);

		JTabbedPane tabbedPane = new JTabbedPane();

		// ===== Basic tab =====
		JPanel basicPanel = createBasicPanel();
		tabbedPane.addTab("Basic", basicPanel);

		// ===== Caller ID Transcriptions tab =====
		JPanel transcriptionsPanel = createTranscriptionsPanel();
		tabbedPane.addTab("Transcriptions", transcriptionsPanel);

		// ===== Notes tab =====
		JPanel notesPanel = createNotesPanel();
		tabbedPane.addTab("Notes", notesPanel);

		// ===== Modification tab =====
		tabbedPane.addTab("Modification", modificationPanel);

		add(tabbedPane, BorderLayout.CENTER);
	}

	// ==================== Basic Tab ====================

	private JPanel createBasicPanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]5[]5[]5[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// CONTACT (1:1)
		panel.add(new JLabel("Contact:"), "align label");
		panel.add(contactField, "growx,wrap");

		// TYPE (0:1)
		panel.add(new JLabel("Type:"), "align label");
		panel.add(typeCombo, "growx,wrap");

		// CALLER_ID (0:1)
		panel.add(new JLabel("Caller ID:"), "align label");
		panel.add(callerIdField, "growx,wrap");

		// RESTRICTION (0:1)
		panel.add(restrictionCheckBox, "span 2,wrap");

		return panel;
	}

	// ==================== Transcriptions Tab ====================

	private JPanel createTranscriptionsPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder("Caller ID Transcriptions"));

		JScrollPane scrollPane = new JScrollPane(transcriptionList);
		scrollPane.setPreferredSize(new Dimension(200, 100));
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(editBtn);
		btnPanel.add(deleteBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		transcriptionList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editTranscription();
				}
			}
		});
		transcriptionList.addListSelectionListener(e -> {
			boolean selected = (transcriptionList.getSelectedIndex() != -1);
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addTranscription());
		editBtn.addActionListener(e -> editTranscription());
		deleteBtn.addActionListener(e -> deleteTranscription());

		return panel;
	}

	// ==================== Notes Tab ====================

	private JPanel createNotesPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder("Note References"));

		JScrollPane scrollPane = new JScrollPane(noteList);
		scrollPane.setPreferredSize(new Dimension(200, 70));
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

		noteList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editNote();
				}
			}
		});
		noteList.addListSelectionListener(e -> {
			boolean selected = (noteList.getSelectedIndex() != -1);
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

	// ==================== Transcriptions methods ====================

	private String buildTranscriptionDisplay(FLEFRecord transRecord){
		String phonetic = FLEFRecordUtils.getChildValue(transRecord, "PHONETIC");
		String transcription = FLEFRecordUtils.getChildValue(transRecord, "TRANSCRIPTION");
		StringBuilder sb = new StringBuilder();
		if(phonetic != null) sb.append("phonetic: ").append(phonetic);
		if(transcription != null){
			if(!sb.isEmpty())
				sb.append(" | ");
			sb.append("transcription: ")
				.append(transcription);
		}
		if(sb.isEmpty())
			sb.append("[empty]");
		return sb.toString();
	}

	private void loadTranscriptions(){
		transcriptionModel.clear();
		transcriptionRecords.clear();
		// Find the CALLER_ID child and load its TRANSCRIBED_TEXT children
		FLEFRecord callerIdRecord = findCallerIdRecord();
		if(callerIdRecord != null){
			for(FLEFRecord child : callerIdRecord.getChildren()){
				if("TRANSCRIBED_TEXT".equals(child.getTag())){
					transcriptionRecords.add(child);
					transcriptionModel.addElement(buildTranscriptionDisplay(child));
				}
			}
		}
	}

	private FLEFRecord findCallerIdRecord(){
		// This is a placeholder - the parent record will have CONTACT -> CALLER_ID
		// We need to be passed the parent record to find it
		return null;
	}

	private FLEFRecord findOrCreateCallerIdRecord(FLEFRecord parent){
		if(parent == null)
			return null;
		// Find CALLER_ID under the parent (CONTACT)
		FLEFRecord callerId = FLEFRecordUtils.findChild(parent, "CALLER_ID");
		if(callerId == null){
			callerId = FLEFRecord.createChildWithValue(2, "CALLER_ID", callerIdField.getText().trim());
			parent.addChild(callerId);
		}
		return callerId;
	}

	private void addTranscription(){
		TranscribedTextDialog dialog = new TranscribedTextDialog(
			(parent instanceof JDialog? (JDialog)parent: null),
			null
		);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord transRecord = dialog.getTranscribedTextRecord();
			if(transRecord != null){
				// We need the parent record to add it to CALLER_ID
				// This will be handled in saveToRecord
				transcriptionRecords.add(transRecord);
				transcriptionModel.addElement(buildTranscriptionDisplay(transRecord));
			}
		}
	}

	private void editTranscription(){
		int idx = transcriptionList.getSelectedIndex();
		if(idx == -1)
			return;
		FLEFRecord transRecord = transcriptionRecords.get(idx);
		TranscribedTextDialog dialog = new TranscribedTextDialog(
			(parent instanceof JDialog? (JDialog)parent: null),
			transRecord
		);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			transcriptionModel.set(idx, buildTranscriptionDisplay(transRecord));
		}
	}

	private void deleteTranscription(){
		int idx = transcriptionList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(parent,
			"Remove this transcription?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			transcriptionRecords.remove(idx);
			transcriptionModel.remove(idx);
		}
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
		noteModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
		// Notes are loaded from the parent CONTACT record
		// This will be handled in loadFromRecord
	}

	private void addNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(parent, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			(parent instanceof Frame? (Frame)parent: null),
			model, noteHandler, selectedId -> {
			if(selectedId != null && !noteIds.contains(selectedId)){
				noteIds.add(selectedId);
				String display = getNoteDisplayName(selectedId);
				noteDisplayMap.put(selectedId, display);
				noteModel.addElement(display);
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
			JOptionPane.showMessageDialog(parent, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(parent, "Note not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = noteHandler.createEditDialog(
			(parent instanceof Frame? (Frame)parent: null),
			model, rec
		);
		dialog.setVisible(true);
		String newDisplay = getNoteDisplayName(id);
		noteDisplayMap.put(id, newDisplay);
		noteModel.set(idx, newDisplay);
	}

	private void deleteNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(parent,
			"Remove this note reference?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String removedId = noteIds.remove(idx);
			noteDisplayMap.remove(removedId);
			noteModel.remove(idx);
		}
	}

	private void createNewNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(parent, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>(noteIds);
		JDialog dialog = noteHandler.createNewDialog(
			(parent instanceof Frame? (Frame)parent: null),
			model
		);
		dialog.setVisible(true);
		for(FLEFRecord rec : model.getRecordsByType("NOTE")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !noteIds.contains(id)){
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteModel.addElement(display);
				break;
			}
		}
	}

	// ==================== Public API ====================

	/**
	 * Loads data from a CONTACT FLEFRecord.
	 *
	 * @param contactRecord the CONTACT record (may be null)
	 */
	public void loadFromRecord(FLEFRecord contactRecord){
		clear();

		if(contactRecord == null){
			return;
		}

		// CONTACT (1:1)
		contactField.setText(contactRecord.getValue());

		// TYPE (0:1)
		String type = FLEFRecordUtils.getChildValue(contactRecord, "TYPE");
		typeCombo.setSelectedItem(type != null? type: "");

		// CALLER_ID (0:1)
		FLEFRecord callerIdRecord = FLEFRecordUtils.findChild(contactRecord, "CALLER_ID");
		if(callerIdRecord != null){
			callerIdField.setText(callerIdRecord.getValue());

			// CALLER_ID -> TRANSCRIBED_TEXT (0:M)
			transcriptionModel.clear();
			transcriptionRecords.clear();
			for(FLEFRecord child : callerIdRecord.getChildren()){
				if("TRANSCRIBED_TEXT".equals(child.getTag())){
					transcriptionRecords.add(child);
					transcriptionModel.addElement(buildTranscriptionDisplay(child));
				}
			}
		}

		// NOTE (0:M)
		noteModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
		for(FLEFRecord child : contactRecord.getChildren()){
			if("NOTE".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteModel.addElement(display);
			}
		}

		// RESTRICTION (0:1)
		String restriction = FLEFRecordUtils.getChildValue(contactRecord, "RESTRICTION");
		restrictionCheckBox.setSelected("confidential".equals(restriction));

		// MODIFICATION_STRUCTURE (1:1)
		modificationPanel.loadFromRecord(contactRecord);
	}

	/**
	 * Saves data to a CONTACT FLEFRecord.
	 * The record will be created if null.
	 *
	 * @param contactRecord the CONTACT record to save into (may be null)
	 * @return the saved record (new if null was passed), or null if validation fails
	 */
	public FLEFRecord saveToRecord(FLEFRecord contactRecord){
		// Validate before saving
		if(!validateRequiredFields()){
			return null;
		}

		if(contactRecord == null){
			contactRecord = new FLEFRecord();
			contactRecord.setLevel(1);
			contactRecord.setTag("CONTACT");
		}

		// Clear existing children
		contactRecord.getChildren().clear();

		// CONTACT (1:1) - required
		String contact = contactField.getText().trim();
		if(!contact.isEmpty()){
			contactRecord.setValue(contact);
		}

		// TYPE (0:1)
		String type = (String)typeCombo.getSelectedItem();
		if(type != null && !type.isEmpty()){
			FLEFRecordUtils.updateChildValue(contactRecord, "TYPE", type);
		}

		// CALLER_ID (0:1) with its TRANSCRIBED_TEXT children
		String callerId = callerIdField.getText().trim();
		if(!callerId.isEmpty() || !transcriptionRecords.isEmpty()){
			FLEFRecord callerIdRecord = FLEFRecord.createChildWithValue(1, "CALLER_ID", callerId);
			contactRecord.addChild(callerIdRecord);

			for(FLEFRecord transRecord : transcriptionRecords){
				// Ensure the transcription has the correct level
				transRecord.setLevel(2);
				transRecord.setTag("TRANSCRIBED_TEXT");
				callerIdRecord.addChild(transRecord);
			}
		}

		// NOTE (0:M)
		for(String id : noteIds){
			FLEFRecordUtils.addChild(contactRecord, "NOTE", 1, id);
		}

		// RESTRICTION (0:1)
		String restriction = restrictionCheckBox.isSelected()? "confidential": null;
		if(restriction != null){
			FLEFRecordUtils.updateChildValue(contactRecord, "RESTRICTION", restriction);
		}

		// MODIFICATION_STRUCTURE (1:1)
		modificationPanel.saveToRecord(contactRecord);

		return contactRecord;
	}

	/**
	 * Validates that required fields (CONTACT) are filled.
	 *
	 * @return true if valid, false otherwise
	 */
	public boolean validateRequiredFields(){
		// If no data at all, validation passes (contact can be empty)
		if(!hasData()){
			return true;
		}

		// CONTACT (1:1) - required if contact has data
		if(contactField.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(parent,
				"CONTACT is required.\n" +
					"Please enter a contact address.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			contactField.requestFocusInWindow();
			return false;
		}

		// MODIFICATION_STRUCTURE (1:1) - required if contact has data
		return modificationPanel.validateRequiredFields();
	}

	/**
	 * Checks if the contact structure has any data.
	 *
	 * @return true if any field has data
	 */
	public boolean hasData(){
		return !contactField.getText().trim().isEmpty() ||
			(typeCombo.getSelectedItem() != null &&
				!((String)typeCombo.getSelectedItem()).isEmpty()) ||
			!callerIdField.getText().trim().isEmpty() ||
			!transcriptionModel.isEmpty() ||
			!noteModel.isEmpty() ||
			restrictionCheckBox.isSelected() ||
			modificationPanel.hasData();
	}

	/**
	 * Clears all fields in the panel.
	 */
	public void clear(){
		contactField.setText("");
		typeCombo.setSelectedItem("");
		callerIdField.setText("");
		transcriptionModel.clear();
		transcriptionRecords.clear();
		noteModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
		restrictionCheckBox.setSelected(false);
		modificationPanel.clear();
	}

}
