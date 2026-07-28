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

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
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
 * Reusable panel for editing a REPOSITORY_CITATION structure.
 * <p>
 * Structure:
 * <pre>
 * REPOSITORY_CITATION :=
 *   n REPOSITORY @<XREF:REPOSITORY>@    {1:1}
 *     +1 LOCATION <WHERE_WITHIN_REPOSITORY>    {0:1}
 *     +1 NOTE @<XREF:NOTE>@    {0:M}
 * </pre>
 */
public class RepositoryCitationPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -2445624581367265605L;


	private final FLEFModel model;
	private final Component parent;

	private final JTextField repositoryDisplayField = new JTextField(20);
	private final JButton browseRepoBtn = new JButton("Browse...");
	private final JButton clearRepoBtn = new JButton("Clear");
	private String selectedRepositoryId;

	private final JTextField locationField = new JTextField(30);

	private final DefaultListModel<String> noteModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	private final RecordTypeHandler<?> repositoryHandler = HandlerRegistry.getHandler("REPOSITORY");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");

	/**
	 * Creates a new RepositoryCitationPanel.
	 *
	 * @param model  the FLEF model
	 * @param parent the parent component (for showing dialogs)
	 */
	public RepositoryCitationPanel(FLEFModel model, Component parent){
		this.model = model;
		this.parent = parent;
		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]5[]5[]"));
		setBorder(new TitledBorder("Repository Citation"));

		noteList.setVisibleRowCount(4);

		add(new JLabel("Repository:"), "align label");
		repositoryDisplayField.setEditable(false);
		repositoryDisplayField.setBackground(UIManager.getColor("TextField.background"));
		JPanel repoPanel = new JPanel(new BorderLayout(5, 5));
		repoPanel.add(repositoryDisplayField, BorderLayout.CENTER);
		JPanel repoBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		repoBtnPanel.add(browseRepoBtn);
		repoBtnPanel.add(clearRepoBtn);
		repoPanel.add(repoBtnPanel, BorderLayout.EAST);
		add(repoPanel, "growx,wrap");

		browseRepoBtn.addActionListener(e -> browseRepository());
		clearRepoBtn.addActionListener(e -> {
			selectedRepositoryId = null;
			repositoryDisplayField.setText(StringUtils.EMPTY);
		});

		add(new JLabel("Location:"), "align label");
		add(locationField, "growx,wrap");

		add(new JLabel("Notes:"), "align label,top");
		JPanel notePanel = createNotePanel();
		add(notePanel, "growx");
	}


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


	private void browseRepository(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			(parent instanceof Dialog? (Dialog)parent: null),
			model, repositoryHandler, selectedId -> {
			if(selectedId != null){
				selectedRepositoryId = selectedId;
				FLEFRecord rec = model.getRecordById(selectedId);
				if(rec != null){
					repositoryDisplayField.setText(repositoryHandler.getDisplayText(rec));
				}
				else{
					repositoryDisplayField.setText(selectedId);
				}
			}
		}
		);
		dialog.setVisible(true);
	}


	private String getNoteDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return noteHandler.getDisplayText(rec);
		}
		return id;
	}

	private void addNote(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			(parent instanceof Dialog? (Dialog)parent: null),
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
			(parent instanceof Dialog? (Dialog)parent: null),
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
			(parent instanceof Dialog? (Dialog)parent: null),
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


	/**
	 * Loads data from a REPOSITORY_CITATION FLEFRecord.
	 *
	 * @param citationRecord the REPOSITORY_CITATION record (may be null)
	 */
	public void loadFromRecord(FLEFRecord citationRecord){
		// Clear all fields
		selectedRepositoryId = null;
		repositoryDisplayField.setText(StringUtils.EMPTY);
		locationField.setText(StringUtils.EMPTY);
		noteModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();

		if(citationRecord == null){
			return;
		}

		// REPOSITORY
		String repoId = citationRecord.getValue();
		if(repoId != null && !repoId.isEmpty()){
			selectedRepositoryId = repoId;
			FLEFRecord rec = model.getRecordById(repoId);
			if(rec != null){
				repositoryDisplayField.setText(repositoryHandler.getDisplayText(rec));
			}
			else{
				repositoryDisplayField.setText(repoId);
			}
		}

		// LOCATION (0:1)
		locationField.setText(FLEFRecordUtils.getChildValue(citationRecord, "LOCATION"));

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
	}

	/**
	 * Saves data to a REPOSITORY_CITATION FLEFRecord.
	 * The record will be created if null.
	 *
	 * @param citationRecord the REPOSITORY_CITATION record to save into (may be null)
	 * @return the saved record (new if null was passed), or null if validation fails
	 */
	public FLEFRecord saveToRecord(FLEFRecord citationRecord){
		// Validate before saving
		if(!validateRequiredFields()){
			return null;
		}

		if(citationRecord == null){
			citationRecord = FLEFRecord.createChild("REPOSITORY_CITATION");
		}

		// Clear existing children
		FLEFRecordUtils.removeAllChildren(citationRecord);

		// REPOSITORY - required
		if(selectedRepositoryId != null && !selectedRepositoryId.isEmpty()){
			citationRecord.setValue(selectedRepositoryId);
		}

		// LOCATION (0:1)
		String location = locationField.getText().trim();
		FLEFRecordUtils.updateChildValue(citationRecord, "LOCATION", location);

		// NOTE (0:M)
		for(String id : noteIds){
			FLEFRecordUtils.addChild(citationRecord, "NOTE", id);
		}

		return citationRecord;
	}

	/**
	 * Validates that the required field (REPOSITORY) is filled.
	 *
	 * @return true if valid, false otherwise
	 */
	public boolean validateRequiredFields(){
		// If no data at all, validation passes (citation can be empty)
		if(!hasData()){
			return true;
		}

		// REPOSITORY - required if citation has data
		if(selectedRepositoryId == null || selectedRepositoryId.isEmpty()){
			JOptionPane.showMessageDialog(parent,
				"REPOSITORY is required for a repository citation.\n" +
					"Please select a repository record.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	/**
	 * Checks if the repository citation has any data.
	 *
	 * @return true if any field has data
	 */
	public boolean hasData(){
		return (selectedRepositoryId != null && !selectedRepositoryId.isEmpty()) ||
			!locationField.getText().trim().isEmpty() ||
			!noteModel.isEmpty();
	}

	/**
	 * Clears all fields in the panel.
	 */
	public void clear(){
		selectedRepositoryId = null;
		repositoryDisplayField.setText(StringUtils.EMPTY);
		locationField.setText(StringUtils.EMPTY);
		noteModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
	}

}
