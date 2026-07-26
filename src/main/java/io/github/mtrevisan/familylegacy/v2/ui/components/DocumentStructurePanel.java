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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ScrollableContainerHost;
import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
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
import javax.swing.JTextArea;
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
 * Reusable panel for editing a DOCUMENT_STRUCTURE.
 * <p>
 * Structure:
 * <pre>
 * DOCUMENT_STRUCTURE :=
 *   n FILE <DOCUMENT_FILE_REFERENCE>    {1:1}
 *     +1 SPHERICAL Y    {0:1}
 *     +1 MAPPING <MAPPING_TYPE>    {0:1}
 *     +1 DESCRIPTION <DOCUMENT_DESCRIPTION>    {0:1}
 *     +1 EXTRACT <TEXTED_TEXT_FROM_SOURCE>    {0:1}
 *       +2 TYPE <EXTRACT_TYPE>    {0:1}
 *       +2 LOCALE <EXTRACT_LOCALE_CODE>    {0:1}
 *     +1 RESTRICTION <confidential>    {0:1}
 *     +1 NOTE @<XREF:NOTE>@    {0:M}
 * </pre>
 */
public class DocumentStructurePanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -7512372301406529923L;


	private final FLEFModel model;
	private final Component parent;

	// ========== FILE (1:1) ==========
	private final JTextField fileField = new JTextField(30);

	// ========== SPHERICAL (0:1) ==========
	private final JCheckBox sphericalCheckBox = new JCheckBox("Spherical");

	// ========== MAPPING (0:1) ==========
	private final JComboBox<String> mappingCombo = new JComboBox<>(new String[]{
		"", "spherical_UV", "cylindrical_equirectangular_horizontal",
		"cylindrical_equirectangular_vertical"
	});

	// ========== DESCRIPTION (0:1) ==========
	private final JTextField descriptionField = new JTextField(30);

	// ========== EXTRACT (0:1) ==========
	private final JTextArea extractArea = new JTextArea(3, 20);
	private final JComboBox<String> extractTypeCombo = new JComboBox<>(new String[]{"", "transcript", "extract", "abstract"});
	private final JTextField extractLocaleField = new JTextField(10);

	// ========== RESTRICTION (0:1) ==========
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	// ========== NOTE (0:M) ==========
	private final DefaultListModel<String> noteModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	// ========== Handlers ==========
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");

	/**
	 * Creates a new DocumentStructurePanel.
	 *
	 * @param model  the FLEF model
	 * @param parent the parent component (for showing dialogs)
	 */
	public DocumentStructurePanel(FLEFModel model, Component parent){
		this.model = model;
		this.parent = parent;
		initComponents();
	}

	private void initComponents(){
		setLayout(new BorderLayout(5, 5));

		noteList.setVisibleRowCount(4);

		JTabbedPane tabbedPane = new JTabbedPane();

		// ===== Basic tab =====
		JPanel basicPanel = createBasicPanel();
		tabbedPane.addTab("Basic", basicPanel);

		// ===== Extract tab =====
		JPanel extractPanel = createExtractPanel();
		tabbedPane.addTab("Extract", extractPanel);

		// ===== Notes tab =====
		JPanel notesPanel = createNotesPanel();
		tabbedPane.addTab("Notes", notesPanel);

		add(tabbedPane, BorderLayout.CENTER);
	}

	// ==================== Basic Tab ====================

	private JPanel createBasicPanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]5[]5[]5[]5[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// FILE (1:1)
		panel.add(new JLabel("File:"), "align label");
		panel.add(fileField, "growx,wrap");

		// SPHERICAL (0:1)
		panel.add(sphericalCheckBox, "span 2,wrap");

		// MAPPING (0:1)
		panel.add(new JLabel("Mapping:"), "align label");
		panel.add(mappingCombo, "growx,wrap");

		// DESCRIPTION (0:1)
		panel.add(new JLabel("Description:"), "align label");
		panel.add(descriptionField, "growx,wrap");

		// RESTRICTION (0:1)
		panel.add(restrictionCheckBox, "span 2,wrap");

		return panel;
	}

	// ==================== Extract Tab ====================

	private JPanel createExtractPanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]5[]5[]"));
		panel.setBorder(new TitledBorder("Extract"));

		// EXTRACT text area
		panel.add(new JLabel("Extract Text:"), "align label,top");
		JScrollPane scrollPane = GUIHelper.createScrollPane(extractArea);
		panel.add(scrollPane, "growx,wrap");

		// EXTRACT -> TYPE (0:1)
		panel.add(new JLabel("Extract Type:"), "align label");
		panel.add(extractTypeCombo, "growx,wrap");

		// EXTRACT -> LOCALE (0:1)
		panel.add(new JLabel("Locale:"), "align label");
		panel.add(extractLocaleField, "growx");

		return panel;
	}

	// ==================== Notes Tab ====================

	private JPanel createNotesPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder("Note References"));

		JScrollPane scrollPane = GUIHelper.createScrollPane(noteList);
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

	// ==================== Note methods ====================

	private String getNoteDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return noteHandler.getDisplayName(rec);
		}
		return id;
	}

	private void loadNotes(){
		noteModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
		// Notes are loaded from the parent DOCUMENT_STRUCTURE record
		// This will be handled in loadFromRecord
	}

	private void addNote(){
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
	 * Loads data from a DOCUMENT_STRUCTURE FLEFRecord.
	 *
	 * @param documentRecord the DOCUMENT_STRUCTURE record (may be null)
	 */
	public void loadFromRecord(FLEFRecord documentRecord){
		clear();

		if(documentRecord == null){
			return;
		}

		// FILE (1:1)
		fileField.setText(documentRecord.getValue());

		// SPHERICAL (0:1)
		String spherical = FLEFRecordUtils.getChildValue(documentRecord, "SPHERICAL");
		sphericalCheckBox.setSelected("Y".equals(spherical));

		// MAPPING (0:1)
		String mapping = FLEFRecordUtils.getChildValue(documentRecord, "MAPPING");
		mappingCombo.setSelectedItem(mapping != null? mapping: "");

		// DESCRIPTION (0:1)
		descriptionField.setText(FLEFRecordUtils.getChildValue(documentRecord, "DESCRIPTION"));

		// EXTRACT (0:1)
		FLEFRecord extract = FLEFRecordUtils.findChild(documentRecord, "EXTRACT");
		if(extract != null){
			extractArea.setText(extract.getValue());
			String extractType = FLEFRecordUtils.getChildValue(extract, "TYPE");
			extractTypeCombo.setSelectedItem(extractType != null? extractType: "");
			String extractLocale = FLEFRecordUtils.getChildValue(extract, "LOCALE");
			extractLocaleField.setText(extractLocale != null? extractLocale: "");
		}

		// RESTRICTION (0:1)
		String restriction = FLEFRecordUtils.getChildValue(documentRecord, "RESTRICTION");
		restrictionCheckBox.setSelected("confidential".equals(restriction));

		// NOTE (0:M)
		noteModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
		for(FLEFRecord child : documentRecord.getChildren()){
			if("NOTE".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteModel.addElement(display);
			}
		}
	}

	/**
	 * Saves data to a DOCUMENT_STRUCTURE FLEFRecord.
	 * The record will be created if null.
	 *
	 * @param documentRecord the DOCUMENT_STRUCTURE record to save into (may be null)
	 * @return the saved record (new if null was passed), or null if validation fails
	 */
	public FLEFRecord saveToRecord(FLEFRecord documentRecord){
		// Validate before saving
		if(!validateRequiredFields()){
			return null;
		}

		if(documentRecord == null){
			documentRecord = new FLEFRecord();
			documentRecord.setTag("DOCUMENT_STRUCTURE");
		}

		// Clear existing children
		FLEFRecordUtils.removeAllChildren(documentRecord);

		// FILE (1:1) - required
		String file = fileField.getText().trim();
		if(!file.isEmpty()){
			documentRecord.setValue(file);
		}

		// SPHERICAL (0:1)
		if(sphericalCheckBox.isSelected()){
			FLEFRecordUtils.updateChildValue(documentRecord, "SPHERICAL", "Y");
		}

		// MAPPING (0:1)
		String mapping = (String)mappingCombo.getSelectedItem();
		FLEFRecordUtils.updateChildValue(documentRecord, "MAPPING", mapping);

		// DESCRIPTION (0:1)
		String description = descriptionField.getText().trim();
		FLEFRecordUtils.updateChildValue(documentRecord, "DESCRIPTION", description);

		// EXTRACT (0:1) with its children
		String extractText = extractArea.getText().trim();
		if(!extractText.isEmpty()){
			FLEFRecord extract = new FLEFRecord();
			extract.setTag("EXTRACT");
			extract.setValue(extractText);
			documentRecord.addChild(extract);

			String extractType = (String)extractTypeCombo.getSelectedItem();
			FLEFRecordUtils.updateChildValue(extract, "TYPE", extractType);
			String extractLocale = extractLocaleField.getText().trim();
			FLEFRecordUtils.updateChildValue(extract, "LOCALE", extractLocale);
		}

		// RESTRICTION (0:1)
		String restriction = restrictionCheckBox.isSelected()? "confidential": null;
		FLEFRecordUtils.updateChildValue(documentRecord, "RESTRICTION", restriction);

		// NOTE (0:M)
		for(String id : noteIds){
			FLEFRecordUtils.addChild(documentRecord, "NOTE", id);
		}

		return documentRecord;
	}

	/**
	 * Validates that required fields (FILE) are filled.
	 *
	 * @return true if valid, false otherwise
	 */
	public boolean validateRequiredFields(){
		// If no data at all, validation passes (document can be empty)
		if(!hasData()){
			return true;
		}

		// FILE (1:1) - required if document has data
		if(fileField.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(parent,
				"FILE is required for a DOCUMENT_STRUCTURE.\n" +
					"Please enter a file reference.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			fileField.requestFocusInWindow();
			return false;
		}
		return true;
	}

	/**
	 * Checks if the document structure has any data.
	 *
	 * @return true if any field has data
	 */
	public boolean hasData(){
		return !fileField.getText().trim().isEmpty() ||
			sphericalCheckBox.isSelected() ||
			(mappingCombo.getSelectedItem() != null &&
				!((String)mappingCombo.getSelectedItem()).isEmpty()) ||
			!descriptionField.getText().trim().isEmpty() ||
			!extractArea.getText().trim().isEmpty() ||
			restrictionCheckBox.isSelected() ||
			!noteModel.isEmpty();
	}

	/**
	 * Clears all fields in the panel.
	 */
	public void clear(){
		fileField.setText("");
		sphericalCheckBox.setSelected(false);
		mappingCombo.setSelectedItem("");
		descriptionField.setText("");
		extractArea.setText("");
		extractTypeCombo.setSelectedItem("");
		extractLocaleField.setText("");
		restrictionCheckBox.setSelected(false);
		noteModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
	}

}
