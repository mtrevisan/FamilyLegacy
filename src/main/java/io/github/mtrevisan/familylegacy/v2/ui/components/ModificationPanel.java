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
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/**
 * Reusable panel for editing a MODIFICATION_STRUCTURE.
 * <p>
 * Structure:
 * <pre>
 * MODIFICATION_STRUCTURE :=
 *   n CREATION    {1:1}
 *     +1 DATE <CREATION_DATE>    {1:1}
 *   n UPDATE    {0:M}
 *     +1 DATE <UPDATE_DATE>    {1:1}
 *     +1 NOTE <TEXT>    {0:1}
 */
public class ModificationPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 7707110647553098799L;


	private final Component parent;

	// CREATION (1:1)
	private final JTextField creationDateField = new JTextField(15);

	// UPDATE (0:M)
	private final DefaultListModel<UpdateRecord> updateModel = new DefaultListModel<>();
	private final JList<UpdateRecord> updateList = new JList<>(updateModel);
	private final List<UpdateRecord> updateRecords = new ArrayList<>();

	/**
	 * Represents a single UPDATE record.
	 */
	public static class UpdateRecord{
		public String date;
		public String noteText;

		public UpdateRecord(String date, String noteText){
			this.date = date;
			this.noteText = noteText;
		}

		@Override
		public String toString(){
			if(date != null && !date.isEmpty()){
				return date + (noteText != null && !noteText.isEmpty()? " (note: " + noteText + ")": "");
			}
			return "(empty)";
		}
	}

	/**
	 * Creates a new ModificationPanel.
	 *
	 * @param model  the FLEF model (kept for compatibility, but not used for note references)
	 * @param parent the parent component (for showing dialogs)
	 */
	public ModificationPanel(FLEFModel model, Component parent){
		this.parent = parent;
		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]10[]"));
		setBorder(new TitledBorder("Modification"));

		// CREATION (1:1)
		add(new JLabel("Creation Date:"), "align label");
		add(creationDateField, "growx,wrap");

		// UPDATE (0:M)
		add(new JLabel("Updates:"), "align label,top");

		JPanel updatePanel = createModificationPanel();
		add(updatePanel, "growx");
	}

	private JPanel createModificationPanel(){
		JPanel panel = new JPanel(new BorderLayout(3, 3));

		updateList.setVisibleRowCount(4);
		JScrollPane scrollPane = new JScrollPane(updateList);
		scrollPane.setPreferredSize(updateList.getPreferredScrollableViewportSize());
		panel.add(scrollPane, BorderLayout.CENTER);

		// ----- Menu contestuale -----
		JPopupMenu popup = new JPopupMenu();
		JMenuItem addItem = new JMenuItem("Add");
		JMenuItem editItem = new JMenuItem("Edit");
		JMenuItem deleteItem = new JMenuItem("Delete");
		popup.add(addItem);
		popup.addSeparator();
		popup.add(editItem);
		popup.add(deleteItem);

		// Mouse listener per popup (click destro) e doppio click
		updateList.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(MouseEvent e){
				if(e.isPopupTrigger()){
					int index = updateList.locationToIndex(e.getPoint());
					if(index != -1 && !updateList.isSelectedIndex(index)){
						updateList.setSelectedIndex(index);
					}
					popup.show(updateList, e.getX(), e.getY());
				}
			}

			@Override
			public void mouseReleased(MouseEvent e){
				if(e.isPopupTrigger()){
					int index = updateList.locationToIndex(e.getPoint());
					if(index != -1 && !updateList.isSelectedIndex(index)){
						updateList.setSelectedIndex(index);
					}
					popup.show(updateList, e.getX(), e.getY());
				}
			}

			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editUpdate();
				}
			}
		});

		// Scorciatoie da tastiera: Ins per Add, Canc per Delete
		updateList.addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(KeyEvent e){
				if(e.getKeyCode() == KeyEvent.VK_INSERT){
					addUpdate();
					e.consume();
				}
				else if(e.getKeyCode() == KeyEvent.VK_DELETE){
					removeUpdate();
					e.consume();
				}
			}
		});

		// Abilitazione/disabilitazione delle voci del menu in base alla selezione
		updateList.addListSelectionListener(e -> {
			boolean selected = updateList.getSelectedIndex() != -1;
			editItem.setEnabled(selected);
			deleteItem.setEnabled(selected);
		});
		editItem.setEnabled(false);
		deleteItem.setEnabled(false);

		// Azioni del menu contestuale
		addItem.addActionListener(e -> addUpdate());
		editItem.addActionListener(e -> editUpdate());
		deleteItem.addActionListener(e -> removeUpdate());

		return panel;
	}

	// ==================== UPDATE methods ====================

	private void addUpdate(){
		UpdateRecord newRec = showUpdateDialog(null);
		if(newRec != null){
			updateRecords.add(newRec);
			updateModel.addElement(newRec);
		}
	}

	private void editUpdate(){
		int idx = updateList.getSelectedIndex();
		if(idx == -1)
			return;
		UpdateRecord current = updateRecords.get(idx);
		UpdateRecord updated = showUpdateDialog(current);
		if(updated != null){
			updateRecords.set(idx, updated);
			updateModel.set(idx, updated);
		}
	}

	private void removeUpdate(){
		int idx = updateList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(
			parent,
			"Remove this update?",
			"Confirm",
			JOptionPane.YES_NO_OPTION
		);
		if(confirm == JOptionPane.YES_OPTION){
			updateRecords.remove(idx);
			updateModel.remove(idx);
		}
	}

	/**
	 * Shows a sub-dialog to edit an Update record.
	 * The NOTE is a simple text field (0:1).
	 */
	private UpdateRecord showUpdateDialog(UpdateRecord existing){
		JDialog dialog = new JDialog(
			(Frame)SwingUtilities.getWindowAncestor(parent),
			existing == null? "Add Update": "Edit Update",
			true
		);
		dialog.setLayout(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]10[]"));
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		JTextField dateField = new JTextField(15);
		JTextField noteField = new JTextField(20);

		if(existing != null){
			dateField.setText(existing.date);
			noteField.setText(existing.noteText != null? existing.noteText: "");
		}

		dialog.add(new JLabel("Date:"), "align label");
		dialog.add(dateField, "growx,wrap");

		dialog.add(new JLabel("Note (optional):"), "align label");
		dialog.add(noteField, "growx,wrap");

		JPanel btnPanelBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		btnPanelBottom.add(okBtn);
		btnPanelBottom.add(cancelBtn);
		dialog.add(btnPanelBottom, "span 2,growx");

		final UpdateRecord[] result = {null};
		okBtn.addActionListener(e -> {
			String date = dateField.getText().trim();
			if(date.isEmpty()){
				JOptionPane.showMessageDialog(dialog, "Date is required for an update.", "Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			String note = noteField.getText().trim();
			result[0] = new UpdateRecord(date, note.isEmpty()? null: note);
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setMinimumSize(new Dimension(450, 180));
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);

		return result[0];
	}

	// ==================== Public API ====================

	/**
	 * Loads the modification data from a FLEFRecord.
	 *
	 * @param record the record that may contain a MODIFICATION child
	 */
	public void loadFromRecord(FLEFRecord record){
		// Clear current data
		creationDateField.setText("");
		updateModel.clear();
		updateRecords.clear();

		if(record == null){
			return;
		}

		FLEFRecord mod = FLEFRecordUtils.findChild(record, "MODIFICATION");
		if(mod == null){
			return;
		}

		// CREATION (1:1)
		FLEFRecord creation = FLEFRecordUtils.findChild(mod, "CREATION");
		if(creation != null){
			creationDateField.setText(FLEFRecordUtils.getChildValue(creation, "DATE"));
		}

		// UPDATE (0:M)
		for(FLEFRecord child : mod.getChildren()){
			if("UPDATE".equals(child.getTag())){
				String date = FLEFRecordUtils.getChildValue(child, "DATE");
				String note = FLEFRecordUtils.getChildValue(child, "NOTE");
				if(date != null){
					UpdateRecord rec = new UpdateRecord(date, note);
					updateRecords.add(rec);
					updateModel.addElement(rec);
				}
			}
		}
	}

	/**
	 * Saves the modification data into a FLEFRecord.
	 * The MODIFICATION child will be created or updated.
	 *
	 * @param record the record to save into
	 */
	public void saveToRecord(FLEFRecord record){
		if(record == null){
			return;
		}

		// Validate before saving
		if(!validateRequiredFields()){
			return;
		}

		// Remove existing MODIFICATION child
		FLEFRecordUtils.removeChildren(record, "MODIFICATION");

		String creationDate = creationDateField.getText().trim();

		// Only create MODIFICATION if there is at least CREATION or some UPDATE
		if(creationDate.isEmpty() && updateRecords.isEmpty()){
			return;
		}

		FLEFRecord mod = new FLEFRecord();
		mod.setLevel(1);
		mod.setTag("MODIFICATION");
		record.addChild(mod);

		// CREATION (1:1) - required if MODIFICATION is used
		if(!creationDate.isEmpty()){
			FLEFRecord creation = new FLEFRecord();
			creation.setLevel(2);
			creation.setTag("CREATION");
			mod.addChild(creation);
			FLEFRecordUtils.updateChildValue(creation, "DATE", creationDate);
		}

		// UPDATE (0:M)
		for(UpdateRecord upd : updateRecords){
			if(upd.date != null && !upd.date.isEmpty()){
				FLEFRecord updateRec = new FLEFRecord();
				updateRec.setLevel(2);
				updateRec.setTag("UPDATE");
				mod.addChild(updateRec);
				FLEFRecordUtils.updateChildValue(updateRec, "DATE", upd.date);
				if(upd.noteText != null && !upd.noteText.isEmpty()){
					FLEFRecordUtils.updateChildValue(updateRec, "NOTE", upd.noteText);
				}
			}
		}
	}

	/**
	 * Validates that required fields (CREATION_DATE) are filled
	 * if the modification has any data.
	 *
	 * @return true if valid, false otherwise
	 */
	public boolean validateRequiredFields(){
		// If no data at all, validation passes (modification can be empty)
		if(!hasData()){
			return true;
		}

		// CREATION_DATE (1:1) - required if modification has data
		if(creationDateField.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(parent,
				"Creation Date is required when MODIFICATION is used.\n" +
					"Please enter a date for CREATION.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			creationDateField.requestFocusInWindow();
			return false;
		}

		return true;
	}

	/**
	 * Checks if the modification has any data.
	 *
	 * @return true if any field has data
	 */
	public boolean hasData(){
		return !creationDateField.getText().trim().isEmpty() || !updateRecords.isEmpty();
	}

	/**
	 * Clears all fields in the panel.
	 */
	public void clear(){
		creationDateField.setText("");
		updateModel.clear();
		updateRecords.clear();
	}

}
