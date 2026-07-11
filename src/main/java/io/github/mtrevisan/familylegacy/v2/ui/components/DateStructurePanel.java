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
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.io.Serial;


/**
 * Reusable panel for editing a DATE_STRUCTURE.
 * <p>
 * Structure:
 * <pre>
 * DATE_STRUCTURE :=
 *   n DATE <ENTRY_RECORDING_DATE>    {1:1}
 *     +1 CALENDAR @<XREF:CALENDAR>@    {0:1}
 *     +1 ORIGINAL_TEXT <ORIGINAL_INPUT_TEXT>    {0:1}
 *     +1 CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}
 * </pre>
 */
public class DateStructurePanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -3898832505950815095L;


	private final FLEFModel model;
	private final Component parent;

	// ========== DATE (1:1) ==========
	private final JTextField dateField = new JTextField(15);

	// ========== CALENDAR (0:1) ==========
	private final JTextField calendarDisplayField = new JTextField(15);
	private final JButton browseCalendarBtn = new JButton("Browse...");
	private final JButton clearCalendarBtn = new JButton("Clear");
	private String selectedCalendarId;

	// ========== ORIGINAL_TEXT (0:1) ==========
	private final JTextField originalTextField = new JTextField(20);

	// ========== CREDIBILITY (0:1) ==========
	private final JComboBox<String> credibilityCombo = new JComboBox<>(new String[]{"", "0", "1", "2", "3"});

	// ========== Handlers ==========
	private final RecordTypeHandler<?> calendarHandler = HandlerRegistry.getHandler("CALENDAR");


	public DateStructurePanel(FLEFModel model, Component parent){
		this.model = model;
		this.parent = parent;
		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]5[]5[]5[]"));
		setBorder(new TitledBorder("Date"));

		// ===== DATE (1:1) =====
		add(new JLabel("Date:"), "align label");
		add(dateField, "growx,wrap");

		// ===== CALENDAR (0:1) =====
		add(new JLabel("Calendar:"), "align label");
		calendarDisplayField.setEditable(false);
		calendarDisplayField.setBackground(UIManager.getColor("TextField.background"));
		JPanel calendarPanel = new JPanel(new BorderLayout(5, 5));
		calendarPanel.add(calendarDisplayField, BorderLayout.CENTER);
		JPanel calBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		calBtnPanel.add(browseCalendarBtn);
		calBtnPanel.add(clearCalendarBtn);
		calendarPanel.add(calBtnPanel, BorderLayout.EAST);
		add(calendarPanel, "growx,wrap");

		browseCalendarBtn.addActionListener(e -> browseCalendar());
		clearCalendarBtn.addActionListener(e -> {
			selectedCalendarId = null;
			calendarDisplayField.setText("");
		});

		// ===== ORIGINAL_TEXT (0:1) =====
		add(new JLabel("Original Text:"), "align label");
		add(originalTextField, "growx,wrap");

		// ===== CREDIBILITY (0:1) =====
		add(new JLabel("Credibility:"), "align label");
		add(credibilityCombo, "growx");
	}

	// ==================== Calendar methods ====================

	private void browseCalendar(){
		if(calendarHandler == null){
			JOptionPane.showMessageDialog(parent, "Calendar handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			(parent instanceof Frame? (Frame)parent: null),
			model, calendarHandler, selectedId -> {
			if(selectedId != null){
				selectedCalendarId = selectedId;
				FLEFRecord rec = model.getRecordById(selectedId);
				if(rec != null){
					calendarDisplayField.setText(calendarHandler.getDisplayName(rec));
				}
				else{
					calendarDisplayField.setText(selectedId);
				}
			}
		}
		);
		dialog.setVisible(true);
	}

	// ==================== Public API ====================

	/**
	 * Loads data from a DATE FLEFRecord.
	 *
	 * @param dateRecord the DATE record (may be null)
	 */
	public void loadFromRecord(FLEFRecord dateRecord){
		// Clear all fields
		dateField.setText("");
		selectedCalendarId = null;
		calendarDisplayField.setText("");
		originalTextField.setText("");
		credibilityCombo.setSelectedItem("");

		if(dateRecord == null){
			return;
		}

		// DATE (1:1)
		dateField.setText(dateRecord.getValue());

		// CALENDAR (0:1)
		String calendarId = FLEFRecordUtils.getChildValue(dateRecord, "CALENDAR");
		if(calendarId != null && !calendarId.isEmpty()){
			selectedCalendarId = calendarId;
			FLEFRecord rec = model.getRecordById(calendarId);
			if(rec != null && calendarHandler != null){
				calendarDisplayField.setText(calendarHandler.getDisplayName(rec));
			}
			else{
				calendarDisplayField.setText(calendarId);
			}
		}

		// ORIGINAL_TEXT (0:1)
		originalTextField.setText(FLEFRecordUtils.getChildValue(dateRecord, "ORIGINAL_TEXT"));

		// CREDIBILITY (0:1)
		String credibility = FLEFRecordUtils.getChildValue(dateRecord, "CREDIBILITY");
		credibilityCombo.setSelectedItem(credibility != null? credibility: "");
	}

	/**
	 * Saves data to a DATE FLEFRecord.
	 * The record will be created if null.
	 *
	 * @param dateRecord the DATE record to save into (may be null)
	 * @return the saved record (new if null was passed), or null if validation fails
	 */
	public FLEFRecord saveToRecord(FLEFRecord dateRecord){
		// Validate before saving
		if(!validateRequiredFields()){
			return null;
		}

		if(dateRecord == null){
			dateRecord = new FLEFRecord();
			dateRecord.setLevel(1);
			dateRecord.setTag("DATE");
		}

		// Clear existing children
		dateRecord.getChildren().clear();

		// DATE (1:1) - required
		String date = dateField.getText().trim();
		if(!date.isEmpty()){
			dateRecord.setValue(date);
		}

		// CALENDAR (0:1)
		if(selectedCalendarId != null && !selectedCalendarId.isEmpty()){
			FLEFRecordUtils.updateChildValue(dateRecord, "CALENDAR", selectedCalendarId);
		}

		// ORIGINAL_TEXT (0:1)
		String originalText = originalTextField.getText().trim();
		if(!originalText.isEmpty()){
			FLEFRecordUtils.updateChildValue(dateRecord, "ORIGINAL_TEXT", originalText);
		}

		// CREDIBILITY (0:1)
		String credibility = (String)credibilityCombo.getSelectedItem();
		if(credibility != null && !credibility.isEmpty()){
			FLEFRecordUtils.updateChildValue(dateRecord, "CREDIBILITY", credibility);
		}

		return dateRecord;
	}

	/**
	 * Validates that the required field (DATE) is filled.
	 *
	 * @return true if valid, false otherwise
	 */
	public boolean validateRequiredFields(){
		if(dateField.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(parent,
				"DATE is required for a DATE_STRUCTURE.\n" +
					"Please enter a date.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			dateField.requestFocusInWindow();
			return false;
		}
		return true;
	}

	/**
	 * Checks if the date structure has any data.
	 *
	 * @return true if any field has data
	 */
	public boolean hasData(){
		return !dateField.getText().trim().isEmpty() ||
			(selectedCalendarId != null && !selectedCalendarId.isEmpty()) ||
			!originalTextField.getText().trim().isEmpty() ||
			(credibilityCombo.getSelectedItem() != null &&
				!((String)credibilityCombo.getSelectedItem()).isEmpty());
	}

	/**
	 * Clears all fields in the panel.
	 */
	public void clear(){
		dateField.setText("");
		selectedCalendarId = null;
		calendarDisplayField.setText("");
		originalTextField.setText("");
		credibilityCombo.setSelectedItem("");
	}

	/**
	 * Sets the date value.
	 *
	 * @param date the date string
	 */
	public void setDate(String date){
		dateField.setText(date);
	}

	/**
	 * Gets the date value.
	 *
	 * @return the date string
	 */
	public String getDate(){
		return dateField.getText().trim();
	}

}
