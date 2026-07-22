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
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.io.Serial;


/**
 * Panel for editing a {@code RESTRICTION_STRUCTURE} according to FLEF 0.1.0.
 * <p>
 * Structure:
 * <pre>
 * RESTRICTION_STRUCTURE :=
 * n RESTRICTION    {1:1}
 *   +1 LEVEL <RESTRICTION_LEVEL>    {1:1}
 *   +1 RATIONALE <TEXT>    {0:1}
 *   +1 EXPIRES <DATE>    {0:1}
 * </pre>
 * <p>
 * Known LEVEL values: {@code 'public'}, {@code 'restricted'}, {@code 'confidential'}.
 *
 * @see <a href="https://en.wikipedia.org/wiki/ISO_8601">ISO 8601 Date Format</a>
 */
public class RestrictionPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -8538135290834556765L;

	// UI components
	private final JComboBox<String> levelCombo = new JComboBox<>(new String[]{"", "public", "restricted", "confidential"});

	// RATIONALE - multi-line text area with scroll
	private final JTextArea rationaleArea = new JTextArea(3, 30);
	private final JScrollPane rationaleScrollPane = new JScrollPane(rationaleArea);

	private final JTextField expiresField = new JTextField(15);

	// Parent dialog for showing error messages
	private final Dialog parentDialog;

	/**
	 * Constructs a new RestrictionPanel.
	 *
	 * @param parent the parent dialog (used for showing message dialogs)
	 */
	public RestrictionPanel(Dialog parent){
		this.parentDialog = parent;
		initComponents();
	}

	/**
	 * Constructs a new RestrictionPanel without a parent dialog.
	 */
	public RestrictionPanel(){
		this.parentDialog = null;
		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout("ins 10, fillx, top", "[right]rel[grow]", "[]10[]10[]"));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// LEVEL (required)
		add(new JLabel("Level:"), "align label");
		add(levelCombo, "growx,wrap");

		// RATIONALE (optional) - multi-line text area
		add(new JLabel("Rationale:"), "align label,top");
		rationaleArea.setLineWrap(true);
		rationaleArea.setWrapStyleWord(true);
		rationaleArea.setToolTipText("e.g., 'Living individual', 'Repository license forbids redistribution'");
		rationaleScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		rationaleScrollPane.setPreferredSize(new Dimension(300, 60));
		add(rationaleScrollPane, "growx,wrap");

		// EXPIRES (optional)
		add(new JLabel("Expires:"), "align label");
		expiresField.setToolTipText("ISO 8601 date (e.g., 2030-12-31)");
		add(expiresField, "growx");
	}

	// ==================== Data Loading ====================

	/**
	 * Loads data from a RESTRICTION record into the panel.
	 *
	 * @param restrictionRecord the RESTRICTION record, or {@code null}
	 */
	public void loadFromRecord(FLEFRecord restrictionRecord){
		if(restrictionRecord == null){
			clear();
			return;
		}

		String level = FLEFRecordUtils.getChildValue(restrictionRecord, "LEVEL");
		levelCombo.setSelectedItem(level != null? level: "");

		String rationale = FLEFRecordUtils.getChildValue(restrictionRecord, "RATIONALE");
		rationaleArea.setText(rationale != null? rationale: "");

		String expires = FLEFRecordUtils.getChildValue(restrictionRecord, "EXPIRES");
		expiresField.setText(expires != null? expires: "");
	}

	// ==================== Data Saving ====================

	/**
	 * Saves the panel data into a RESTRICTION record.
	 * <p>
	 * If the panel has no data (i.e., LEVEL is empty), returns {@code null}.
	 *
	 * @param targetRecord an existing RESTRICTION record to update, or {@code null} to create a new one
	 * @return the updated or new RESTRICTION record, or {@code null} if no data
	 */
	public FLEFRecord saveToRecord(FLEFRecord targetRecord){
		if(!hasData()){
			return null;
		}

		FLEFRecord record = targetRecord != null? targetRecord: new FLEFRecord();
		// Level and tag will be set by the caller (e.g., GroupDialog)

		// LEVEL (required)
		String level = (String)levelCombo.getSelectedItem();
		if(level != null && !level.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "LEVEL", level);
		}
		else{
			FLEFRecordUtils.removeChildren(record, "LEVEL");
		}

		// RATIONALE (optional)
		String rationale = rationaleArea.getText().trim();
		if(!rationale.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "RATIONALE", rationale);
		}
		else{
			FLEFRecordUtils.removeChildren(record, "RATIONALE");
		}

		// EXPIRES (optional)
		String expires = expiresField.getText().trim();
		if(!expires.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "EXPIRES", expires);
		}
		else{
			FLEFRecordUtils.removeChildren(record, "EXPIRES");
		}

		return record;
	}

	// ==================== Validation ====================

	/**
	 * Checks whether the panel has any data (i.e., LEVEL is selected).
	 *
	 * @return {@code true} if LEVEL is selected, otherwise {@code false}
	 */
	public boolean hasData(){
		String level = (String)levelCombo.getSelectedItem();
		return level != null && !level.isEmpty();
	}

	/**
	 * Validates the required fields and the format of the EXPIRES date.
	 *
	 * @return {@code true} if LEVEL is selected and EXPIRES (if present) is a valid ISO 8601 date,
	 * otherwise {@code false}
	 */
	public boolean validateRequiredFields(){
		String level = (String)levelCombo.getSelectedItem();
		if(level == null || level.isEmpty()){
			showError("Restriction LEVEL is required.");
			return false;
		}

		// Validate EXPIRES format if present
		String expires = expiresField.getText().trim();
		if(!expires.isEmpty()){
			// Simple ISO 8601 date validation (YYYY-MM-DD)
			if(!expires.matches("\\d{4}-\\d{2}-\\d{2}")){
				showError("EXPIRES must be in ISO 8601 date format (YYYY-MM-DD).");
				return false;
			}
		}

		return true;
	}

	// ==================== Utility methods ====================

	/**
	 * Clears all fields (sets LEVEL to empty, clears RATIONALE and EXPIRES).
	 */
	public void clear(){
		levelCombo.setSelectedIndex(0);
		rationaleArea.setText("");
		expiresField.setText("");
	}

	/**
	 * Returns the selected restriction level.
	 *
	 * @return the selected level, or {@code null} if none selected
	 */
	public String getSelectedLevel(){
		String level = (String)levelCombo.getSelectedItem();
		return (level != null && !level.isEmpty())? level: null;
	}

	// ==================== Private helpers ====================

	/**
	 * Shows an error message dialog if a parent dialog is available.
	 *
	 * @param message the error message
	 */
	private void showError(String message){
		if(parentDialog != null){
			JOptionPane.showMessageDialog(parentDialog, message, "Validation Error", JOptionPane.ERROR_MESSAGE);
		}
		else{
			System.err.println("Validation Error: " + message);
		}
	}

}
