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
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ScrollableContainerHost;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
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
 * Reusable panel for editing a SOURCE_CITATION structure.
 * <p>
 * Structure:
 * <pre>
 * SOURCE_CITATION :=
 *   n SOURCE @<XREF:SOURCE>@    {1:1}
 *     +1 SEARCH_OUTCOME <SEARCH_OUTCOME>    {0:1}
 *     +1 SEARCH_SCOPE <SEARCH_SCOPE_DESCRIPTION>    {0:1}
 *     +1 SEARCH_DATE <DATE_STRUCTURE>    {0:1}
 *     +1 LOCATION <WHERE_WITHIN_SOURCE>    {0:1}
 *     +1 ROLE <ROLE_IN_EVENT>    {0:1}
 *     +1 CROP <CROP_COORDINATES>    {1:1}
 *     +1 NOTE @<XREF:NOTE>@    {0:M}
 *     +1 CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}
 * </pre>
 */
public class SourceCitationPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -8036724779224867360L;


	private final FLEFModel model;
	private final Component parent;

	// ========== SOURCE (1:1) ==========
	private final JTextField sourceDisplayField = new JTextField(20);
	private final JButton browseSourceBtn = new JButton("Browse...");
	private final JButton clearSourceBtn = new JButton("Clear");
	private String selectedSourceId;

	// ========== SEARCH_OUTCOME (0:1) ==========
	private final JComboBox<String> outcomeCombo = new JComboBox<>(new String[]{
		"", "found", "not_found", "partially_found", "unreadable", "destroyed"
	});

	// ========== SEARCH_SCOPE (0:1) ==========
	private final JTextField scopeField = new JTextField(20);

	// ========== SEARCH_DATE (0:1) - using DateStructurePanel ==========
	private final DatePanel searchDatePanel;

	// ========== LOCATION (0:1) ==========
	private final JTextField locationField = new JTextField(20);

	// ========== ROLE (0:1) ==========
	private final JTextField roleField = new JTextField(15);

	// ========== CROP (1:1) ==========
	private final JTextField cropField = new JTextField(20);

	// ========== NOTE (0:M) ==========
	private final DefaultListModel<String> noteModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	// ========== CREDIBILITY (0:1) ==========
	private final JComboBox<String> credibilityCombo = new JComboBox<>(new String[]{"", "0", "1", "2", "3"});

	// ========== Handlers ==========
	private final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler("SOURCE");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");

	/**
	 * Creates a new SourceCitationPanel.
	 *
	 * @param model  the FLEF model
	 * @param parent the parent component (for showing dialogs)
	 */
	public SourceCitationPanel(FLEFModel model, Dialog parent){
		this.model = model;
		this.parent = parent;
		this.searchDatePanel = new DatePanel(model, parent);
		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]5[]5[]5[]5[]5[]5[]5[]"));
		setBorder(new TitledBorder("Source Citation"));

		noteList.setVisibleRowCount(4);

		// ===== SOURCE (1:1) =====
		add(new JLabel("Source:"), "align label");
		sourceDisplayField.setEditable(false);
		sourceDisplayField.setBackground(UIManager.getColor("TextField.background"));
		JPanel sourcePanel = new JPanel(new BorderLayout(5, 5));
		sourcePanel.add(sourceDisplayField, BorderLayout.CENTER);
		JPanel sourceBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		sourceBtnPanel.add(browseSourceBtn);
		sourceBtnPanel.add(clearSourceBtn);
		sourcePanel.add(sourceBtnPanel, BorderLayout.EAST);
		add(sourcePanel, "growx,wrap");

		browseSourceBtn.addActionListener(e -> browseSource());
		clearSourceBtn.addActionListener(e -> {
			selectedSourceId = null;
			sourceDisplayField.setText("");
		});

		// ===== SEARCH_OUTCOME (0:1) =====
		add(new JLabel("Search Outcome:"), "align label");
		add(outcomeCombo, "growx,wrap");

		// ===== SEARCH_SCOPE (0:1) =====
		add(new JLabel("Search Scope:"), "align label");
		add(scopeField, "growx,wrap");

		// ===== SEARCH_DATE (0:1) - using DateStructurePanel =====
		add(new JLabel("Search Date:"), "align label,top");
		add(searchDatePanel, "growx,wrap");

		// ===== LOCATION (0:1) =====
		add(new JLabel("Location:"), "align label");
		add(locationField, "growx,wrap");

		// ===== ROLE (0:1) =====
		add(new JLabel("Role:"), "align label");
		add(roleField, "growx,wrap");

		// ===== CROP (1:1) =====
		add(new JLabel("Crop:"), "align label");
		add(cropField, "growx,wrap");

		// ===== NOTE (0:M) =====
		add(new JLabel("Notes:"), "align label,top");
		JPanel notePanel = createNotePanel();
		add(notePanel, "growx,wrap");

		// ===== CREDIBILITY (0:1) =====
		add(new JLabel("Credibility:"), "align label");
		add(credibilityCombo, "growx");
	}

	// ==================== Note Panel ====================

	private JPanel createNotePanel(){
		JPanel panel = new JPanel(new BorderLayout(3, 3));

		JScrollPane scrollPane = createScrollPane(noteList);
		scrollPane.setPreferredSize(new Dimension(200, 60));
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

	private JScrollPane createScrollPane(final JList<?> list){
		JScrollPane scrollPane = new JScrollPane(new ScrollableContainerHost(list,
			ScrollableContainerHost.ScrollType.VERTICAL));
		scrollPane.setPreferredSize(list.getPreferredScrollableViewportSize());
		return scrollPane;
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
		JDialog dialog = noteHandler.createEditDialog((parent instanceof Frame? (Frame)parent: null), model, rec);
		dialog.setVisible(true);
		String newDisplay = getNoteDisplayName(id);
		noteDisplayMap.put(id, newDisplay);
		noteModel.set(idx, newDisplay);
	}

	private void deleteNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(parent, "Remove this note reference?", "Confirm", JOptionPane.YES_NO_OPTION);
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
		JDialog dialog = noteHandler.createNewDialog((parent instanceof Frame? (Frame)parent: null), model);
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

	// ==================== Source methods ====================

	private void browseSource(){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(parent, "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			(parent instanceof Frame? (Frame)parent: null),
			model, sourceHandler, selectedId -> {
			if(selectedId != null){
				selectedSourceId = selectedId;
				FLEFRecord rec = model.getRecordById(selectedId);
				if(rec != null){
					sourceDisplayField.setText(sourceHandler.getDisplayName(rec));
				}
				else{
					sourceDisplayField.setText(selectedId);
				}
			}
		}
		);
		dialog.setVisible(true);
	}

	// ==================== Public API ====================

	/**
	 * Loads data from a SOURCE_CITATION FLEFRecord.
	 *
	 * @param citationRecord the SOURCE_CITATION record (may be null)
	 */
	public void loadFromRecord(FLEFRecord citationRecord){
		// Clear all fields
		selectedSourceId = null;
		sourceDisplayField.setText("");
		outcomeCombo.setSelectedItem("");
		scopeField.setText("");
		searchDatePanel.clear();
		locationField.setText("");
		roleField.setText("");
		cropField.setText("");
		noteModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
		credibilityCombo.setSelectedItem("");

		if(citationRecord == null){
			return;
		}

		// SOURCE (1:1)
		String sourceId = citationRecord.getValue();
		if(sourceId != null && !sourceId.isEmpty()){
			selectedSourceId = sourceId;
			FLEFRecord rec = model.getRecordById(sourceId);
			if(rec != null && sourceHandler != null){
				sourceDisplayField.setText(sourceHandler.getDisplayName(rec));
			}
			else{
				sourceDisplayField.setText(sourceId);
			}
		}

		// SEARCH_OUTCOME (0:1)
		String outcome = FLEFRecordUtils.getChildValue(citationRecord, "SEARCH_OUTCOME");
		outcomeCombo.setSelectedItem(outcome != null? outcome: "");

		// SEARCH_SCOPE (0:1)
		scopeField.setText(FLEFRecordUtils.getChildValue(citationRecord, "SEARCH_SCOPE"));

		// SEARCH_DATE (0:1) - use DateStructurePanel
		FLEFRecord dateRecord = FLEFRecordUtils.findChild(citationRecord, "SEARCH_DATE");
		searchDatePanel.loadFromRecord(dateRecord);

		// LOCATION (0:1)
		locationField.setText(FLEFRecordUtils.getChildValue(citationRecord, "LOCATION"));

		// ROLE (0:1)
		roleField.setText(FLEFRecordUtils.getChildValue(citationRecord, "ROLE"));

		// CROP (1:1)
		cropField.setText(FLEFRecordUtils.getChildValue(citationRecord, "CROP"));

		// NOTE (0:M)
		for(FLEFRecord child : citationRecord.getChildren()){
			if("NOTE".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteModel.addElement(display);
			}
		}

		// CREDIBILITY (0:1)
		String credibility = FLEFRecordUtils.getChildValue(citationRecord, "CREDIBILITY");
		credibilityCombo.setSelectedItem(credibility != null? credibility: "");
	}

	/**
	 * Saves data to a SOURCE_CITATION FLEFRecord.
	 * The record will be created if null.
	 *
	 * @param citationRecord the SOURCE_CITATION record to save into (may be null)
	 * @return the saved record (new if null was passed), or null if validation fails
	 */
	public FLEFRecord saveToRecord(FLEFRecord citationRecord){
		// Validate required fields before saving
		if(!validateRequiredFields()){
			return null;
		}

		if(citationRecord == null){
			citationRecord = new FLEFRecord();
			citationRecord.setLevel(1);
			citationRecord.setTag("SOURCE_CITATION");
		}

		// Clear existing children
		citationRecord.getChildren().clear();

		// SOURCE (1:1) - required
		if(selectedSourceId != null && !selectedSourceId.isEmpty()){
			citationRecord.setValue(selectedSourceId);
		}

		// SEARCH_OUTCOME (0:1)
		String outcome = (String)outcomeCombo.getSelectedItem();
		if(outcome != null && !outcome.isEmpty()){
			FLEFRecordUtils.updateChildValue(citationRecord, "SEARCH_OUTCOME", outcome);
		}

		// SEARCH_SCOPE (0:1)
		String scope = scopeField.getText().trim();
		if(!scope.isEmpty()){
			FLEFRecordUtils.updateChildValue(citationRecord, "SEARCH_SCOPE", scope);
		}

		// SEARCH_DATE (0:1) - use DateStructurePanel
		if(searchDatePanel.hasData()){
			FLEFRecord dateRecord = searchDatePanel.saveToRecord(null);
			if(dateRecord != null){
				dateRecord.setLevel(2);
				dateRecord.setTag("SEARCH_DATE");
				citationRecord.addChild(dateRecord);
			}
		}

		// LOCATION (0:1)
		String location = locationField.getText().trim();
		if(!location.isEmpty()){
			FLEFRecordUtils.updateChildValue(citationRecord, "LOCATION", location);
		}

		// ROLE (0:1)
		String role = roleField.getText().trim();
		if(!role.isEmpty()){
			FLEFRecordUtils.updateChildValue(citationRecord, "ROLE", role);
		}

		// CROP (1:1) - required
		String crop = cropField.getText().trim();
		if(!crop.isEmpty()){
			FLEFRecordUtils.updateChildValue(citationRecord, "CROP", crop);
		}

		// NOTE (0:M)
		for(String id : noteIds){
			FLEFRecordUtils.addChild(citationRecord, "NOTE", 2, id);
		}

		// CREDIBILITY (0:1)
		String credibility = (String)credibilityCombo.getSelectedItem();
		if(credibility != null && !credibility.isEmpty()){
			FLEFRecordUtils.updateChildValue(citationRecord, "CREDIBILITY", credibility);
		}

		return citationRecord;
	}

	/**
	 * Validates that required fields (SOURCE and CROP) are filled.
	 *
	 * @return true if all required fields are filled
	 */
	public boolean validateRequiredFields(){
		if(selectedSourceId == null || selectedSourceId.isEmpty()){
			JOptionPane.showMessageDialog(parent,
				"SOURCE is required for a citation.\n" +
					"Please select a source record.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if(cropField.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(parent,
				"CROP is required for a citation.\n" +
					"Please enter crop coordinates.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			cropField.requestFocusInWindow();
			return false;
		}
		return true;
	}

	/**
	 * Checks if the source citation has any data.
	 *
	 * @return true if any field has data
	 */
	public boolean hasData(){
		return (selectedSourceId != null && !selectedSourceId.isEmpty()) ||
			outcomeCombo.getSelectedItem() != null &&
				!((String)outcomeCombo.getSelectedItem()).isEmpty() ||
			!scopeField.getText().trim().isEmpty() ||
			searchDatePanel.hasData() ||
			!locationField.getText().trim().isEmpty() ||
			!roleField.getText().trim().isEmpty() ||
			!cropField.getText().trim().isEmpty() ||
			!noteModel.isEmpty() ||
			credibilityCombo.getSelectedItem() != null &&
				!((String)credibilityCombo.getSelectedItem()).isEmpty();
	}

	/**
	 * Clears all fields in the panel.
	 */
	public void clear(){
		selectedSourceId = null;
		sourceDisplayField.setText("");
		outcomeCombo.setSelectedItem("");
		scopeField.setText("");
		searchDatePanel.clear();
		locationField.setText("");
		roleField.setText("");
		cropField.setText("");
		noteModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
		credibilityCombo.setSelectedItem("");
	}

}
