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
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ScrollableContainerHost;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/**
 * Panel for editing a {@code MODIFICATION_STRUCTURE} according to FLEF 0.1.0.
 * <p>
 * Structure:
 * <pre>
 * MODIFICATION_STRUCTURE :=
 * n CREATION    {1:1}
 *   +1 DATE <DATE>    {1:1}
 * n UPDATE    {0:M}
 *   +1 DATE <DATE>    {1:1}
 *   +1 COMMENT <TEXT>    {0:1}
 * </pre>
 */
public class ModificationPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -8538135290834556766L;

	private final FLEFModel model;
	private final Dialog parentDialog;

	// Creation fields
	private final JTextField creationDateField = new JTextField(15);
	private final JTextArea creationCommentArea = new JTextArea(2, 30);

	// Update entries
	private final DefaultListModel<UpdateEntry> updateModel = new DefaultListModel<>();
	private final JList<UpdateEntry> updateList = new JList<>(updateModel);
	private final List<UpdateEntry> updateEntries = new ArrayList<>();

	/**
	 * Internal representation of an UPDATE entry.
	 */
	private static class UpdateEntry{
		private final String date;
		private final String comment;

		UpdateEntry(String date, String comment){
			this.date = date != null? date: "";
			this.comment = comment != null? comment: "";
		}

		@Override
		public String toString(){
			StringBuilder sb = new StringBuilder(date);
			if(!comment.isEmpty()){
				sb.append(": ").append(comment);
			}
			if(sb.length() > 60){
				return sb.substring(0, 57) + "...";
			}
			return sb.toString();
		}
	}

	/**
	 * Constructs a new ModificationPanel.
	 *
	 * @param model  the FLEF model
	 * @param parent the parent dialog (used for showing message dialogs)
	 */
	public ModificationPanel(FLEFModel model, Dialog parent){
		this.model = model;
		this.parentDialog = parent;
		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout("ins 10, fillx, wrap 2", "[right]rel[grow]", "[]10[]10[]"));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// CREATION section
		JPanel creationPanel = new JPanel(new MigLayout("ins 0, fillx", "[right]rel[grow]", "[]5[]"));
		creationPanel.setBorder(new TitledBorder("Creation"));

		creationPanel.add(new JLabel("Date:"), "align label");
		creationPanel.add(creationDateField, "growx,wrap");

		creationPanel.add(new JLabel("Comment:"), "align label,top");
		creationCommentArea.setLineWrap(true);
		creationCommentArea.setWrapStyleWord(true);
		JScrollPane commentScroll = new JScrollPane(creationCommentArea);
		commentScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		commentScroll.setPreferredSize(new Dimension(300, 40));
		creationPanel.add(commentScroll, "growx,wrap");

		add(creationPanel, "span 2,growx");

		// UPDATES section
		JPanel updatePanel = new JPanel(new MigLayout("ins 0, fillx", "[grow]", ""));
		updatePanel.setBorder(new TitledBorder("Updates"));

		updateList.setVisibleRowCount(3);
		updateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Install popup behaviour (no buttons!)
		GUIHelper.installBehaviour(updateList,
			() -> updateList.getSelectedIndex() >= 0,
			this::editUpdate,                    // double‑click → edit
			this::addUpdate,                     // INSERT key → add
			this::removeUpdate,                  // DELETE key → remove
			builder -> {
				builder.item("Add Update...", this::addUpdate);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editUpdate);
				builder.selectionSensitiveItem("Remove", this::removeUpdate);
			});

		JScrollPane scrollPane = new JScrollPane(new ScrollableContainerHost(updateList,
			ScrollableContainerHost.ScrollType.VERTICAL));
		scrollPane.setPreferredSize(updateList.getPreferredScrollableViewportSize());
		updatePanel.add(scrollPane, "growx,wrap");

		add(updatePanel, "span 2,growx");
	}

	// ==================== Update Management ====================

	private void addUpdate(){
		UpdateEntry newEntry = showUpdateDialog(null);
		if(newEntry != null){
			updateEntries.add(newEntry);
			updateModel.addElement(newEntry);
		}
	}

	private void editUpdate(){
		int idx = updateList.getSelectedIndex();
		if(idx == -1) return;

		UpdateEntry current = updateEntries.get(idx);
		UpdateEntry updated = showUpdateDialog(current);
		if(updated != null){
			updateEntries.set(idx, updated);
			updateModel.set(idx, updated);
		}
	}

	private void removeUpdate(){
		int idx = updateList.getSelectedIndex();
		if(idx == -1) return;

		int confirm = JOptionPane.showConfirmDialog(parentDialog,
			"Remove this update?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			updateEntries.remove(idx);
			updateModel.remove(idx);
		}
	}

	/**
	 * Shows a dialog to add or edit an update entry.
	 *
	 * @param initial the existing entry, or {@code null} for a new one
	 * @return the updated entry, or {@code null} if cancelled
	 */
	private UpdateEntry showUpdateDialog(UpdateEntry initial){
		JDialog dialog = new JDialog(parentDialog, initial == null? "Add Update": "Edit Update", true);
		dialog.setLayout(new MigLayout("ins 10, fillx", "[right]rel[grow]", "[]10[]"));

		// DATE (required)
		JTextField dateField = new JTextField(15);
		if(initial != null){
			dateField.setText(initial.date);
		}
		dateField.setToolTipText("ISO 8601 date (e.g., 2026-07-18)");

		dialog.add(new JLabel("Date:"), "align label");
		dialog.add(dateField, "growx,wrap");

		// COMMENT (optional) - multi-line text area
		JTextArea commentArea = new JTextArea(3, 25);
		commentArea.setLineWrap(true);
		commentArea.setWrapStyleWord(true);
		if(initial != null){
			commentArea.setText(initial.comment);
		}
		JScrollPane commentScroll = new JScrollPane(commentArea);
		commentScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		commentScroll.setPreferredSize(new Dimension(300, 60));

		dialog.add(new JLabel("Comment:"), "align label,top");
		dialog.add(commentScroll, "growx,wrap");

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		dialog.add(btnPanel, "span 2,growx");

		final UpdateEntry[] result = {null};
		okBtn.addActionListener(e -> {
			String date = dateField.getText().trim();
			if(date.isEmpty()){
				JOptionPane.showMessageDialog(dialog, "Date cannot be empty.",
					"Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if(!date.matches("\\d{4}-\\d{2}-\\d{2}")){
				JOptionPane.showMessageDialog(dialog, "Date must be in ISO 8601 format (YYYY-MM-DD).",
					"Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			String comment = commentArea.getText().trim();
			result[0] = new UpdateEntry(date, comment);
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(parentDialog);
		dialog.setVisible(true);
		return result[0];
	}

	// ==================== Load / Save ====================

	/**
	 * Loads data from a record's MODIFICATION_STRUCTURE into the panel.
	 *
	 * @param record the record containing the MODIFICATION_STRUCTURE
	 */
	public void loadFromRecord(FLEFRecord record){
		clear();

		// Find CREATION
		FLEFRecord creation = FLEFRecordUtils.findChild(record, "CREATION");
		if(creation != null){
			String date = FLEFRecordUtils.getChildValue(creation, "DATE");
			creationDateField.setText(date != null? date: "");

			// Load creation comment if present (non-standard, but we keep it)
			String comment = FLEFRecordUtils.getChildValue(creation, "COMMENT");
			creationCommentArea.setText(comment != null? comment: "");
		}

		// Find UPDATE entries
		updateEntries.clear();
		updateModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("UPDATE".equals(child.getTag())){
				String date = FLEFRecordUtils.getChildValue(child, "DATE");
				String comment = FLEFRecordUtils.getChildValue(child, "COMMENT");
				UpdateEntry entry = new UpdateEntry(date, comment);
				updateEntries.add(entry);
				updateModel.addElement(entry);
			}
		}
	}

	/**
	 * Saves the panel data into a record's MODIFICATION_STRUCTURE.
	 *
	 * @param record the record to save into
	 */
	public void saveToRecord(FLEFRecord record){
		// Remove existing CREATION and UPDATE children
		FLEFRecordUtils.removeChildren(record, "CREATION");
		FLEFRecordUtils.removeChildren(record, "UPDATE");

		// CREATION (required)
		String creationDate = creationDateField.getText().trim();
		if(!creationDate.isEmpty()){
			FLEFRecord creation = FLEFRecord.createChild(1, "CREATION");
			FLEFRecord date = FLEFRecord.createChildWithValue(2, "DATE", creationDate);
			creation.addChild(date);

			// Save creation comment if present
			String creationComment = creationCommentArea.getText().trim();
			if(!creationComment.isEmpty()){
				FLEFRecord comment = FLEFRecord.createChildWithValue(2, "COMMENT", creationComment);
				creation.addChild(comment);
			}

			record.addChild(creation);
		}

		// UPDATE entries
		for(UpdateEntry entry : updateEntries){
			FLEFRecord update = FLEFRecord.createChild(1, "UPDATE");
			if(entry.date != null && !entry.date.isEmpty()){
				FLEFRecord date = FLEFRecord.createChildWithValue(2, "DATE", entry.date);
				update.addChild(date);
			}
			if(entry.comment != null && !entry.comment.isEmpty()){
				FLEFRecord comment = FLEFRecord.createChildWithValue(2, "COMMENT", entry.comment);
				update.addChild(comment);
			}
			record.addChild(update);
		}
	}

	public void clear(){
		creationDateField.setText("");
		creationCommentArea.setText("");
		updateEntries.clear();
		updateModel.clear();
	}

	/**
	 * Checks whether the panel has any data.
	 *
	 * @return {@code true} if CREATION date is present, otherwise {@code false}
	 */
	public boolean hasData(){
		return !creationDateField.getText().trim().isEmpty();
	}

	/**
	 * Validates required fields.
	 *
	 * @return {@code true} if CREATION date is present and valid, otherwise {@code false}
	 */
	public boolean validateRequiredFields(){
		String creationDate = creationDateField.getText().trim();
		if(creationDate.isEmpty()){
			JOptionPane.showMessageDialog(parentDialog,
				"CREATION date is required.\nPlease add a CREATION date.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		if(!creationDate.matches("\\d{4}-\\d{2}-\\d{2}")){
			JOptionPane.showMessageDialog(parentDialog,
				"CREATION date must be in ISO 8601 format (YYYY-MM-DD).",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		// Validate each update date if present
		for(UpdateEntry entry : updateEntries){
			if(!entry.date.matches("\\d{4}-\\d{2}-\\d{2}")){
				JOptionPane.showMessageDialog(parentDialog,
					"Update date must be in ISO 8601 format (YYYY-MM-DD).",
					"Validation Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}

		return true;
	}

}
