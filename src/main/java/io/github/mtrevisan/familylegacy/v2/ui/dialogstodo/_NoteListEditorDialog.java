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
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A modal dialog for editing the list of notes attached to any record
 * that has children with tag {@code NOTE}. The dialog allows the user
 * to add existing {@code NOTE} records, create new ones, edit them,
 * or remove them.
 * <p>
 * This class follows the same design pattern as {@code GroupDialog}
 * for managing a list of referenced entities.
 */
public class _NoteListEditorDialog extends JDialog{

	private final FLEFModel model;
	private final FLEFRecord ownerRecord;
	private final NoteHandler noteHandler;

	// UI components
	private final DefaultListModel<String> noteListModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteListModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	private boolean saved = false;

	/**
	 * Creates a new note list editor dialog.
	 *
	 * @param parent      the parent frame (or dialog) for modality
	 * @param model       the FLEF model
	 * @param ownerRecord the record whose notes are being edited
	 */
	public _NoteListEditorDialog(Dialog parent, FLEFModel model, FLEFRecord ownerRecord){
		super(parent, "Notes for " + ownerRecord.getId(), true);
		this.model = model;
		this.ownerRecord = ownerRecord;
		this.noteHandler = new NoteHandler();


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		setLayout(new MigLayout("ins 10,fillx,top"));

		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx"));

		JPanel listPanel = createNoteListPanel();
		panel.add(listPanel, "growx,wrap");

		add(panel, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		buttonPanel.add(okBtn);
		buttonPanel.add(cancelBtn);
		add(buttonPanel, BorderLayout.SOUTH);

		okBtn.addActionListener(e -> {
			save();
			saved = true;
			dispose();
		});
		cancelBtn.addActionListener(e -> dispose());
	}

	private JPanel createNoteListPanel(){
		final JPanel panel = new JPanel(new MigLayout("fillx"));
		panel.setBorder(new TitledBorder("Notes"));
		noteList.setVisibleRowCount(4);
		noteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		GUIHelper.installBehavior(noteList,
			this::editNote,
			this::createNewNote,
			this::deleteNote,
			builder -> {
				builder.item("Create New...", this::createNewNote);
				builder.item("Add Existing...", this::addExistingNote);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editNote);
				builder.selectionSensitiveItem("Clear", this::deleteNote);
			}
		);

		panel.add(noteList, "growx,wrap");
		return panel;
	}


	private void loadData(){
		noteIds.clear();
		noteListModel.clear();
		noteDisplayMap.clear();

		// Collect all NOTE children of the owner record
		for(FLEFRecord child : ownerRecord.getChildren()){
			if("NOTE".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteListModel.addElement(display);
			}
		}
	}

	private String getNoteDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return noteHandler.getDisplayText(rec, model);
		}
		return id;
	}

	// ----- Actions -----

	private void createNewNote(){
		Set<String> before = new HashSet<>(noteIds);
		JDialog dialog = noteHandler.createNewDialog(this, model);
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

	private void addExistingNote(){
		_GenericSelectionDialog<?> dialog = new _GenericSelectionDialog<>(
			this, model, noteHandler, selectedItem -> {
			if(selectedItem != null){
				final String selectedId = selectedItem.getValue();
				if(!noteIds.contains(selectedId)){
					noteIds.add(selectedId);
					String display = getNoteDisplayName(selectedId);
					noteDisplayMap.put(selectedId, display);
					noteListModel.addElement(display);
				}
			}
		});
		dialog.setVisible(true);
	}

	private void editNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1) return;
		String id = noteIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null) return;
		JDialog dialog = noteHandler.createEditDialog(this, model, rec);
		dialog.setVisible(true);

		String newDisplay = getNoteDisplayName(id);
		noteDisplayMap.put(id, newDisplay);
		noteListModel.set(idx, newDisplay);
	}

	private void deleteNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1) return;
		if(JOptionPane.showConfirmDialog(this, "Remove this note?", "Confirm",
			JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
		String removedId = noteIds.remove(idx);
		noteDisplayMap.remove(removedId);
		noteListModel.remove(idx);
	}

	private void save(){
		// Remove all existing NOTE children from the owner record
		List<FLEFRecord> toRemove = new ArrayList<>();
		for(FLEFRecord child : ownerRecord.getChildren()){
			if("NOTE".equals(child.getTag())){
				toRemove.add(child);
			}
		}
		toRemove.forEach(ownerRecord::removeChild);

		// Add the current note list as new children
		for(String id : noteIds)
			FLEFRecordHelper.updateChildValue(ownerRecord, "NOTE", XRefHelper.formatXRef(id));
	}

	/**
	 * Returns {@code true} if the user pressed OK and the changes were saved.
	 */
	public boolean isSaved(){
		return saved;
	}

}
