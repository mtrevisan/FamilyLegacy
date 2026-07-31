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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.io.Serial;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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


	private final BindingManager bindingManager = new BindingManager();

	// UI components
	// Creation fields
	private String creationDate;
	private final BoundTextArea creationCommentArea;

	private final Dialog parentDialog;


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
			this.date = StringUtils.defaultString(date);
			this.comment = StringUtils.defaultString(comment);
		}

		@Override
		public String toString(){
			StringBuilder sb = new StringBuilder(date);
			if(!comment.isEmpty())
				sb.append(": ").append(comment);
			if(sb.length() > 60)
				return sb.substring(0, 57) + "...";
			return sb.toString();
		}
	}


	/**
	 * Constructs a new ModificationPanel.
	 *
	 * @param parent the parent dialog (used for showing message dialogs)
	 */
	public ModificationPanel(final Dialog parent){
		this.parentDialog = parent;

		// Initialize bound components
		creationCommentArea = new BoundTextArea("CREATION.COMMENT", 2, 30);

		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout("ins 10,fillx", "[grow]"));

		bindingManager.bind(creationCommentArea);

		// CREATION section
		creationCommentArea.setLineWrap(true);
		creationCommentArea.setWrapStyleWord(true);
		final JPanel creationPanel = new JPanel(new MigLayout("fillx", "[grow]"));
		creationPanel.setBorder(new TitledBorder("Creation Comment"));
		final JScrollPane commentScroll = GUIHelper.createScrollPane(creationCommentArea);
		creationPanel.add(commentScroll, "growx");
		add(creationPanel, "growx,wrap");

		// UPDATES section
		final JPanel updatePanel = new JPanel(new MigLayout("fillx", "[grow]"));
		updatePanel.setBorder(new TitledBorder("Updates"));

		updateList.setVisibleRowCount(3);
		updateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Install popup behavior (no buttons!)
		GUIHelper.installBehavior(updateList,
			() -> updateList.getSelectedIndex() >= 0,
			this::editUpdate,                    // double‑click → edit
			this::createNewUpdate,                     // INSERT key → add
			this::removeUpdate,                  // DELETE key → remove
			builder -> {
				builder.item("Create New...", this::createNewUpdate);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editUpdate);
				builder.selectionSensitiveItem("Remove", this::removeUpdate);
			});

		final JScrollPane scrollPane = GUIHelper.createScrollPane(updateList);
		updatePanel.add(scrollPane, "growx,wrap");

		add(updatePanel, "growx");
	}


	private void createNewUpdate(){
		final UpdateEntry newEntry = showUpdateDialog(null);
		if(newEntry != null){
			updateEntries.add(newEntry);
			updateModel.addElement(newEntry);
		}
	}

	private void editUpdate(){
		final int idx = updateList.getSelectedIndex();
		if(idx == -1)
			return;

		final UpdateEntry current = updateEntries.get(idx);
		final UpdateEntry updated = showUpdateDialog(current);
		if(updated != null){
			updateEntries.set(idx, updated);
			updateModel.set(idx, updated);
		}
	}

	private void removeUpdate(){
		final int idx = updateList.getSelectedIndex();
		if(idx == -1)
			return;

		final int confirm = JOptionPane.showConfirmDialog(parentDialog,
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
	private UpdateEntry showUpdateDialog(final UpdateEntry initial){
		final JDialog dialog = new JDialog(parentDialog, initial == null? "Add Update": "Edit Update", true);
		dialog.setLayout(new MigLayout("ins 10, fillx", "[grow]", "[]10[]"));

		// COMMENT
		final BoundTextArea commentArea = new BoundTextArea("UPDATE.COMMENT", 3, 25);
		commentArea.setLineWrap(true);
		commentArea.setWrapStyleWord(true);
		if(initial != null)
			commentArea.setText(initial.comment);
		final JScrollPane commentScroll = GUIHelper.createScrollPane(commentArea);

		dialog.add(commentScroll, "growx,wrap");

		final JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		final JButton okBtn = new JButton("OK");
		final JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		dialog.add(btnPanel, "growx");

		final UpdateEntry[] result = {null};
		okBtn.addActionListener(e -> {
			final String date = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
			final String comment = commentArea.getText()
				.trim();
			result[0] = new UpdateEntry(date, comment);
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(parentDialog);
		dialog.setVisible(true);

		return result[0];
	}


	/**
	 * Loads data from a record's MODIFICATION_STRUCTURE into the panel.
	 *
	 * @param record the record containing the MODIFICATION_STRUCTURE
	 */
	public void load(final FLEFRecord record){
		clear();

		// ---- Load bound simple fields ----
		bindingManager.load(record);

		// Find CREATION
		final FLEFRecord creation = FLEFRecordHelper.findChild(record, "CREATION");
		if(creation != null){
			creationDate = FLEFRecordHelper.getChildValue(creation, "DATE");

			// Load creation comment if present (non-standard, but we keep it)
			final String comment = FLEFRecordHelper.getChildValue(creation, "COMMENT");
			creationCommentArea.setText(StringUtils.defaultString(comment));
		}

		// Find UPDATE entries
		updateEntries.clear();
		updateModel.clear();
		for(final FLEFRecord child : record.getChildren())
			if("UPDATE".equals(child.getTag())){
				final String date = FLEFRecordHelper.getChildValue(child, "DATE");
				final String comment = FLEFRecordHelper.getChildValue(child, "COMMENT");
				final UpdateEntry entry = new UpdateEntry(date, comment);
				updateEntries.add(entry);
				updateModel.addElement(entry);
			}
	}

	/**
	 * Saves the panel data into the parent's record.
	 *
	 * @param targetRecord the record to save into
	 */
	public void save(final FLEFRecord targetRecord){
		final FLEFRecord record = (targetRecord != null? targetRecord: FLEFRecord.createEmpty());

		// Remove existing children
		FLEFRecordHelper.removeChildren(record, "CREATION");
		FLEFRecordHelper.removeChildren(record, "UPDATE");

		// CREATION
		if(creationDate == null || creationDate.isEmpty())
			creationDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
		FLEFRecordHelper.addChild(record, "CREATION.DATE", creationDate);

		// ---- Save bound simple fields ----
		bindingManager.save(record);

		// Save creation comment if present
		final String creationComment = creationCommentArea.getText()
			.trim();
		FLEFRecordHelper.addChild(record, "CREATION.COMMENT", creationComment);

		// UPDATE entries
		for(int i = 0, length = updateEntries.size(); i < length; i ++){
			final UpdateEntry entry = updateEntries.get(i);
			FLEFRecordHelper.addChild(record, "UPDATE[" + i + "].DATE", entry.date);
			FLEFRecordHelper.addChild(record, "UPDATE[" + i + "].COMMENT", entry.comment);
		}
	}

	public void clear(){
		creationCommentArea.setText(StringUtils.EMPTY);
		updateEntries.clear();
		updateModel.clear();
	}

}
