package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.DateDialog;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.Dialog;
import java.awt.Font;


/**
 * A compact panel for displaying and editing a {@code DATE_STRUCTURE} as defined
 * in the FLEF protocol.
 * <p>
 * The panel expects a {@code DATE} record (the wrapper) which contains one of:
 * {@code VALUE}, {@code BOUNDED}, or {@code SPANNING} as the date value,
 * plus optional {@code SOURCE_CITATION} and {@code EVIDENCE_QUALIFIERS}.
 * </p>
 * <p>
 * This panel shows a summary of the date and provides "Edit..." and "Clear" buttons.
 * The full editing is delegated to {@link DateDialog}.
 * </p>
 */
public class DateFieldPanel extends JPanel{

	private final FLEFModel model;
	private final Dialog parentDialog;
	private final String label;

	private FLEFRecord dateRecord; // the DATE node (wrapper)
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
	 * Extracts a human-readable summary from a DATE node.
	 * The DATE node can contain:
	 * - VALUE (with ISO/CENTURY/DECADE + optional APPROXIMATE)
	 * - BOUNDED (with NOT_BEFORE/NOT_AFTER, each containing a date)
	 * - SPANNING (with FROM/TO, each containing a date)
	 */
	public static String extractDateSummary(final FLEFRecord dateNode){
		if(dateNode == null)
			return StringUtils.EMPTY;

		// Check for POINT
		final FLEFRecord value = FLEFRecordUtils.findChild(dateNode, "POINT");
		if(value != null)
			return extractSingleDate(value);

		// Check for BOUNDED
		final FLEFRecord bounded = FLEFRecordUtils.findChild(dateNode, "BOUNDED");
		if(bounded != null)
			return extractBoundedSummary(bounded);

		// Check for SPANNING
		final FLEFRecord spanning = FLEFRecordUtils.findChild(dateNode, "SPANNING");
		if(spanning != null)
			return extractSpanningSummary(spanning);

		return "[invalid]";
	}

	/**
	 * Extracts a single date string from a node that may contain ISO, CENTURY, or DECADE
	 * and optional APPROXIMATE.
	 */
	private static String extractSingleDate(final FLEFRecord node){
		final StringBuilder dateStr = new StringBuilder(extractDateValue(node));
		if(dateStr.isEmpty())
			return "[empty]";

		// Check for APPROXIMATE (direct child of the node)
		final FLEFRecord approx = FLEFRecordUtils.findChild(node, "APPROXIMATE");
		if(approx != null){
			final String basis = FLEFRecordUtils.getChildValue(approx, "BASIS");
			final String margin = FLEFRecordUtils.getChildValue(approx, "MARGIN");
			if(basis != null || margin != null){
				dateStr.append(" (approx");
				if(basis != null)
					dateStr.append(" basis: ").append(basis);
				if(margin != null)
					dateStr.append(" margin: ").append(margin);
				dateStr.append(")");
			}
			else
				dateStr.append(" (approx)");
		}
		return dateStr.toString();
	}

	/**
	 * Extracts the actual date value from FULL_DATE, CENTURY, or DECADE (including CALENDAR).
	 */
	private static String extractDateValue(final FLEFRecord parent){
		final FLEFRecord fullDate = FLEFRecordUtils.findChild(parent, "FULL_DATE");
		if(fullDate != null && fullDate.getValue() != null){
			final String calendar = FLEFRecordUtils.getChildValue(fullDate, "CALENDAR");
			return fullDate.getValue() + (calendar != null? " (" + calendar + ")": StringUtils.EMPTY);
		}

		final FLEFRecord decade = FLEFRecordUtils.findChild(parent, "DECADE");
		if(decade != null && decade.getValue() != null){
			final String calendar = FLEFRecordUtils.getChildValue(decade, "CALENDAR");
			return decade.getValue() + "s" + (calendar != null? " (" + calendar + ")": StringUtils.EMPTY);
		}

		final FLEFRecord century = FLEFRecordUtils.findChild(parent, "CENTURY");
		if(century != null && century.getValue() != null){
			final String part = FLEFRecordUtils.getChildValue(century, "PART");
			final String calendar = FLEFRecordUtils.getChildValue(century, "CALENDAR");
			String centuryStr = century.getValue() + "th century";
			if(part != null)
				centuryStr += " (" + part + ")";
			if(calendar != null)
				centuryStr += " (" + calendar + ")";
			return centuryStr;
		}

		return StringUtils.EMPTY;
	}

	private static String extractBoundedSummary(final FLEFRecord boundedNode){
		final String notBefore = extractBoundEndpoint(boundedNode, "NOT_BEFORE");
		final String notAfter = extractBoundEndpoint(boundedNode, "NOT_AFTER");
		if(!notBefore.isEmpty() && !notAfter.isEmpty())
			return "between " + notBefore + " and " + notAfter;
		if(!notBefore.isEmpty())
			return "after " + notBefore;
		if(!notAfter.isEmpty())
			return "before " + notAfter;
		return "[bounded]";
	}

	private static String extractSpanningSummary(final FLEFRecord spanningNode){
		final String from = extractBoundEndpoint(spanningNode, "FROM");
		final String to = extractBoundEndpoint(spanningNode, "TO");
		if(!from.isEmpty() && !to.isEmpty())
			return "from " + from + " to " + to;
		if(!from.isEmpty())
			return "from " + from;
		if(!to.isEmpty())
			return "until " + to;
		return "[spanning]";
	}

	/**
	 * Extracts the date string from a bound endpoint node (NOT_BEFORE, NOT_AFTER, FROM, TO).
	 * The endpoint node contains ISO/CENTURY/DECADE and optional APPROXIMATE.
	 */
	private static String extractBoundEndpoint(FLEFRecord parent, String childTag){
		FLEFRecord child = FLEFRecordUtils.findChild(parent, childTag);
		if(child == null) return StringUtils.EMPTY;
		return extractSingleDate(child);
	}


	/**
	 * Loads a DATE record into this panel.
	 *
	 * @param record the DATE record (wrapper), or null
	 */
	public void loadFromRecord(FLEFRecord record){
		this.dateRecord = record;

		updateSummary();
	}

	/**
	 * Returns the current DATE record.
	 *
	 * @return the DATE record, or null if no data
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
		// Delegate full validation to DatePanel (which knows the structure)
		DatePanel tempPanel = new DatePanel(parentDialog, model);
		tempPanel.load(dateRecord);
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
