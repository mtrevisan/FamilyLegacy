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
 * Reusable panel for editing a GROUP_CITATION structure.
 * <p>
 * Structure:
 * <pre>
 * GROUP_CITATION :=
 *   n GROUP @<XREF:GROUP>@    {1:1}
 *     +1 ROLE <ROLE_IN_GROUP>    {0:1}
 *     +1 NOTE @<XREF:NOTE>@    {0:M}
 *     +1 CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}
 * </pre>
 */
public class GroupCitationPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -6484848805034159063L;


	private final FLEFModel model;
	private final Component parent;

	// ========== GROUP (1:1) ==========
	private final JTextField groupDisplayField = new JTextField(20);
	private final JButton browseGroupBtn = new JButton("Browse...");
	private final JButton clearGroupBtn = new JButton("Clear");
	private String selectedGroupId;

	// ========== ROLE (0:1) ==========
	private final JTextField roleField = new JTextField(20);

	// ========== NOTE (0:M) ==========
	private final DefaultListModel<String> noteModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	// ========== CREDIBILITY (0:1) ==========
	private final JComboBox<String> credibilityCombo = new JComboBox<>(new String[]{"", "0", "1", "2", "3"});

	// ========== Handlers ==========
	private final RecordTypeHandler<?> groupHandler = HandlerRegistry.getHandler("GROUP");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");

	/**
	 * Creates a new GroupCitationPanel.
	 *
	 * @param model  the FLEF model
	 * @param parent the parent component (for showing dialogs)
	 */
	public GroupCitationPanel(FLEFModel model, Component parent){
		this.model = model;
		this.parent = parent;
		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]5[]5[]5[]"));
		setBorder(new TitledBorder("Group Citation"));

		noteList.setVisibleRowCount(4);

		// ===== GROUP (1:1) =====
		add(new JLabel("Group:"), "align label");
		groupDisplayField.setEditable(false);
		groupDisplayField.setBackground(UIManager.getColor("TextField.background"));
		JPanel groupPanel = new JPanel(new BorderLayout(5, 5));
		groupPanel.add(groupDisplayField, BorderLayout.CENTER);
		JPanel groupBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		groupBtnPanel.add(browseGroupBtn);
		groupBtnPanel.add(clearGroupBtn);
		groupPanel.add(groupBtnPanel, BorderLayout.EAST);
		add(groupPanel, "growx,wrap");

		browseGroupBtn.addActionListener(e -> browseGroup());
		clearGroupBtn.addActionListener(e -> {
			selectedGroupId = null;
			groupDisplayField.setText("");
		});

		// ===== ROLE (0:1) =====
		add(new JLabel("Role:"), "align label");
		add(roleField, "growx,wrap");

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

	// ==================== Group methods ====================

	private void browseGroup(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			(parent instanceof Frame? (Frame)parent: null),
			model, groupHandler, selectedId -> {
			if(selectedId != null){
				selectedGroupId = selectedId;
				FLEFRecord rec = model.getRecordById(selectedId);
				if(rec != null){
					groupDisplayField.setText(groupHandler.getDisplayName(rec));
				}
				else{
					groupDisplayField.setText(selectedId);
				}
			}
		}
		);
		dialog.setVisible(true);
	}

	// ==================== Note methods ====================

	private String getNoteDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return noteHandler.getDisplayName(rec);
		}
		return id;
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
	 * Loads data from a GROUP_CITATION FLEFRecord.
	 *
	 * @param citationRecord the GROUP_CITATION record (may be null)
	 */
	public void loadFromRecord(FLEFRecord citationRecord){
		// Clear all fields
		selectedGroupId = null;
		groupDisplayField.setText("");
		roleField.setText("");
		noteModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
		credibilityCombo.setSelectedItem("");

		if(citationRecord == null){
			return;
		}

		// GROUP (1:1)
		String groupId = citationRecord.getValue();
		if(groupId != null && !groupId.isEmpty()){
			selectedGroupId = groupId;
			FLEFRecord rec = model.getRecordById(groupId);
			if(rec != null){
				groupDisplayField.setText(groupHandler.getDisplayName(rec));
			}
			else{
				groupDisplayField.setText(groupId);
			}
		}

		// ROLE (0:1)
		roleField.setText(FLEFRecordUtils.getChildValue(citationRecord, "ROLE"));

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
	 * Saves data to a GROUP_CITATION FLEFRecord.
	 * The record will be created if null.
	 *
	 * @param citationRecord the GROUP_CITATION record to save into (may be null)
	 * @return the saved record (new if null was passed), or null if validation fails
	 */
	public FLEFRecord saveToRecord(FLEFRecord citationRecord){
		// Validate before saving
		if(!validateRequiredFields()){
			return null;
		}

		if(citationRecord == null){
			citationRecord = new FLEFRecord();
			citationRecord.setTag("GROUP_CITATION");
		}

		// Clear existing children
		FLEFRecordUtils.removeAllChildren(citationRecord);

		// GROUP (1:1) - required
		if(selectedGroupId != null && !selectedGroupId.isEmpty()){
			citationRecord.setValue(selectedGroupId);
		}

		// ROLE (0:1)
		String role = roleField.getText().trim();
		FLEFRecordUtils.updateChildValue(citationRecord, "ROLE", role);

		// NOTE (0:M)
		for(String id : noteIds){
			FLEFRecordUtils.addChild(citationRecord, "NOTE", id);
		}

		// CREDIBILITY (0:1)
		String credibility = (String)credibilityCombo.getSelectedItem();
		FLEFRecordUtils.updateChildValue(citationRecord, "CREDIBILITY", credibility);

		return citationRecord;
	}

	/**
	 * Validates that the required field (GROUP) is filled.
	 *
	 * @return true if valid, false otherwise
	 */
	public boolean validateRequiredFields(){
		// If no data at all, validation passes (citation can be empty)
		if(!hasData()){
			return true;
		}

		// GROUP (1:1) - required if citation has data
		if(selectedGroupId == null || selectedGroupId.isEmpty()){
			JOptionPane.showMessageDialog(parent,
				"GROUP is required for a group citation.\n" +
					"Please select a group record.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	/**
	 * Checks if the group citation has any data.
	 *
	 * @return true if any field has data
	 */
	public boolean hasData(){
		return (selectedGroupId != null && !selectedGroupId.isEmpty()) ||
			!roleField.getText().trim().isEmpty() ||
			!noteModel.isEmpty() ||
			(credibilityCombo.getSelectedItem() != null &&
				!((String)credibilityCombo.getSelectedItem()).isEmpty());
	}

	/**
	 * Clears all fields in the panel.
	 */
	public void clear(){
		selectedGroupId = null;
		groupDisplayField.setText("");
		roleField.setText("");
		noteModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
		credibilityCombo.setSelectedItem("");
	}

}
