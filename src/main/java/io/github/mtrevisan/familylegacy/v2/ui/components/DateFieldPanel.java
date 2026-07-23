package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.DateDialog;
import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;


/**
 * A compact panel for displaying and editing a {@code DATE_STRUCTURE}.
 * <p>
 * This panel shows a summary of the date and provides an "Edit..." button
 * that opens a {@link DateDialog} for full editing.
 */
public class DateFieldPanel extends JPanel{

	private final FLEFModel model;
	private final Dialog parentDialog;
	private final String label;

	private FLEFRecord dateRecord;
	private final JTextField summaryField = new JTextField();
	private final JButton editButton = new JButton("Edit...");
	private final JButton clearButton = new JButton("Clear");

	/**
	 * Constructs a new DateFieldPanel.
	 *
	 * @param model  the FLEF model
	 * @param parent the parent dialog
	 * @param label  the label to display (e.g., "Valid From")
	 */
	public DateFieldPanel(FLEFModel model, Dialog parent, String label){
		this.model = model;
		this.parentDialog = parent;
		this.label = label;
		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout("ins 0, fillx", "[right]rel[grow][][][]"));
		setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

		JLabel labelComp = new JLabel(label + ":");
		labelComp.setFont(labelComp.getFont().deriveFont(Font.BOLD));
		add(labelComp, "align label");

		summaryField.setEditable(false);
		summaryField.setBackground(UIManager.getColor("TextField.background"));
		add(summaryField, "growx");

		add(editButton, "gap 5");
		add(clearButton);

		editButton.addActionListener(e -> editDate());
		clearButton.addActionListener(e -> clearDate());

		updateSummary();
	}

	private void editDate(){
		FLEFRecord result = DateDialog.showDateDialog(parentDialog, model, "Edit " + label, dateRecord);
		if(result != null){
			dateRecord = result;
			updateSummary();
		}
	}

	private void clearDate(){
		if(dateRecord != null && !(JOptionPane.showConfirmDialog(parentDialog,
			"Clear " + label + "?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)){
			return;
		}
		dateRecord = null;
		updateSummary();
	}

	private void updateSummary(){
		if(dateRecord == null || dateRecord.getChildren().isEmpty()){
			summaryField.setText("[not set]");
			clearButton.setEnabled(false);
		}
		else{
			summaryField.setText(extractDateSummary(dateRecord));
			clearButton.setEnabled(true);
		}
	}

	/**
	 * Extracts a human-readable summary from a DATE_STRUCTURE.
	 */
	private String extractDateSummary(FLEFRecord dateStruct){
		if(dateStruct == null) return "[not set]";

		// DATE_STRUCTURE -> DATE -> DATE_VALUE
		FLEFRecord date = FLEFRecordUtils.findChild(dateStruct, "DATE");
		if(date == null){
			// For backward compatibility with older format, try direct child
			FLEFRecord dateValue = FLEFRecordUtils.findChild(dateStruct, "DATE_VALUE");
			if(dateValue != null){
				return extractDateValueSummary(dateValue);
			}
			return "[invalid]";
		}

		FLEFRecord dateValue = FLEFRecordUtils.findChild(date, "DATE_VALUE");
		if(dateValue == null) return "[invalid]";

		return extractDateValueSummary(dateValue);
	}

	private String extractDateValueSummary(FLEFRecord dateValue){
		// Check VALUE
		FLEFRecord value = FLEFRecordUtils.findChild(dateValue, "VALUE");
		if(value != null){
			FLEFRecord qualified = FLEFRecordUtils.findChild(value, "QUALIFIED_DATE");
			if(qualified != null){
				return extractSingleDateSummary(qualified);
			}
		}

		// Check BOUNDED
		FLEFRecord bounded = FLEFRecordUtils.findChild(dateValue, "BOUNDED");
		if(bounded != null){
			String before = extractDateFromChild(bounded, "NOT_BEFORE");
			String after = extractDateFromChild(bounded, "NOT_AFTER");
			if(!before.isEmpty() && !after.isEmpty()){
				return "between " + before + " and " + after;
			}
			else if(!before.isEmpty()){
				return "after " + before;
			}
			else if(!after.isEmpty()){
				return "before " + after;
			}
			return "[bounded]";
		}

		// Check SPANNING
		FLEFRecord spanning = FLEFRecordUtils.findChild(dateValue, "SPANNING");
		if(spanning != null){
			String from = extractDateFromChild(spanning, "FROM");
			String to = extractDateFromChild(spanning, "TO");
			if(!from.isEmpty() && !to.isEmpty()){
				return "from " + from + " to " + to;
			}
			else if(!from.isEmpty()){
				return "from " + from;
			}
			else if(!to.isEmpty()){
				return "until " + to;
			}
			return "[spanning]";
		}

		return "[invalid]";
	}

	private String extractDateFromChild(FLEFRecord parent, String childTag){
		FLEFRecord child = FLEFRecordUtils.findChild(parent, childTag);
		if(child == null) return "";
		FLEFRecord qualified = FLEFRecordUtils.findChild(child, "QUALIFIED_DATE");
		if(qualified != null){
			return extractSingleDateSummary(qualified);
		}
		return "";
	}

	private String extractSingleDateSummary(FLEFRecord qualified){
		FLEFRecord single = FLEFRecordUtils.findChild(qualified, "SINGLE_DATE");
		if(single == null) return "";

		FLEFRecord iso = FLEFRecordUtils.findChild(single, "ISO");
		if(iso != null && iso.getValue() != null){
			return iso.getValue();
		}

		FLEFRecord century = FLEFRecordUtils.findChild(single, "CENTURY");
		if(century != null && century.getValue() != null){
			String centuryStr = century.getValue();
			FLEFRecord part = FLEFRecordUtils.findChild(century, "PART");
			if(part != null && part.getValue() != null){
				return centuryStr + "th century (" + part.getValue() + ")";
			}
			return centuryStr + "th century";
		}

		FLEFRecord decade = FLEFRecordUtils.findChild(single, "DECADE");
		if(decade != null && decade.getValue() != null){
			return decade.getValue() + "s";
		}

		return "";
	}

	// ==================== Public API ====================

	/**
	 * Loads a DATE_STRUCTURE record into this panel.
	 *
	 * @param record the DATE_STRUCTURE record, or null
	 */
	public void loadFromRecord(FLEFRecord record){
		this.dateRecord = record;
		updateSummary();
	}

	/**
	 * Saves the current date to a DATE_STRUCTURE record.
	 *
	 * @return the DATE_STRUCTURE record, or null if no data
	 */
	public FLEFRecord saveToRecord(){
		return dateRecord;
	}

	/**
	 * Checks whether this panel has a date set.
	 *
	 * @return true if a date is set, false otherwise
	 */
	public boolean hasData(){
		return dateRecord != null && !dateRecord.getChildren().isEmpty();
	}

	/**
	 * Validates the date (if present).
	 *
	 * @return true if valid or empty, false otherwise
	 */
	public boolean validateRequiredFields(){
		if(!hasData()){
			return true;
		}
		// Validate the date structure
		DatePanel tempPanel = new DatePanel(model, parentDialog);
		tempPanel.loadFromRecord(dateRecord);
		return tempPanel.validateRequiredFields();
	}

	/**
	 * Clears the date.
	 */
	public void clear(){
		dateRecord = null;
		updateSummary();
	}

}
