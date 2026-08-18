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
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
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
import java.io.Serial;


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

	public static final String FULL_DATE = "FULL_DATE";
	@Serial
	private static final long serialVersionUID = -1546493806724946504L;


	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_START_YEAR = "START_YEAR";
	private static final String TAG_POINT = "POINT";
	private static final String TAG_BOUNDED = "BOUNDED";
	private static final String TAG_SPANNING = "SPANNING";
	private static final String TAG_SINGLE_DATE = "SINGLE_DATE";
	private static final String TAG_APPROXIMATE = "APPROXIMATE";
	private static final String TAG_BASIS = "BASIS";
	private static final String TAG_MARGIN = "MARGIN";
	private static final String TAG_CALENDAR = "CALENDAR";
	private static final String TAG_DECADE = "DECADE";
	private static final String TAG_CENTURY = "CENTURY";
	private static final String TAG_ORDINAL = "ORDINAL";
	private static final String TAG_PART = "PART";
	private static final String TAG_NOT_BEFORE = "NOT_BEFORE";
	private static final String TAG_NOT_AFTER = "NOT_AFTER";
	private static final String TAG_FROM = "FROM";
	private static final String TAG_TO = "TO";


	private final FLEFModel model;
	private final Dialog parent;
	private final String label;

	private FLEFRecord dateRecord;
	private final JTextField summaryField = new JTextField(null);
	private final JButton editButton = new JButton("Edit…");
	private final JButton clearButton = new JButton("Clear");


	/**
	 * Constructs a new DateFieldPanel.
	 *
	 * @param parent	the parent dialog
	 * @param label	the label to display (e.g., "Valid From")
	 * @param model	the FLEF model
	 */
	public DateFieldPanel(Dialog parent, String label, FLEFModel model){
		this.model = model;
		this.parent = parent;

		this.label = label;


		initComponents();
	}


	private void initComponents(){
		setLayout(new MigLayout("ins 0,fillx", "[right]rel[grow][][][]"));
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
		FLEFRecord result = DateDialog.showDateDialog(parent, model, "Edit " + label, dateRecord);
		if(result != null){
			dateRecord = result;
			updateSummary();
		}
	}

	private void clearDate(){
		if(dateRecord != null && !(JOptionPane.showConfirmDialog(parent,
			"Clear " + label + "?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION))
			return;

		dateRecord = null;
		updateSummary();
	}

	private void updateSummary(){
		if(dateRecord == null || dateRecord.getChildren().isEmpty()){
			summaryField.setText("[not set]");
			clearButton.setEnabled(false);
		}
		else{
			summaryField.setText(getDateValueDisplayText(dateRecord));
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
	public static String getDateValueDisplayText(final FLEFRecord dateNode){
		if(dateNode == null)
			return StringUtils.EMPTY;

		final FLEFRecord dateValueNode = dateNode.getTheOnlyChild(TAG_VALUE);

		// Check for POINT
		final FLEFRecord value = FLEFRecordHelper.findChild(dateValueNode, TAG_POINT);
		if(value != null)
			return getQualifiedDateDisplayText(value);

		// Check for BOUNDED
		final FLEFRecord bounded = FLEFRecordHelper.findChild(dateValueNode, TAG_BOUNDED);
		if(bounded != null)
			return getBoundedDisplayText(bounded);

		// Check for SPANNING
		final FLEFRecord spanning = FLEFRecordHelper.findChild(dateValueNode, TAG_SPANNING);
		if(spanning != null)
			return getSpanningDisplayText(spanning);

		return "[invalid]";
	}

	/**
	 * Extracts a single date string from a node that may contain ISO, CENTURY, or DECADE
	 * and optional APPROXIMATE.
	 */
	private static String getQualifiedDateDisplayText(final FLEFRecord node){
		final FLEFRecord singleDate = FLEFRecordHelper.findChild(node, TAG_SINGLE_DATE);
		final StringBuilder dateStr = new StringBuilder(getSingleDateDisplayText(singleDate));
		if(dateStr.isEmpty())
			return "[empty]";

		// Check for APPROXIMATE (direct child of the node)
		final FLEFRecord approx = FLEFRecordHelper.findChild(node, TAG_APPROXIMATE);
		if(approx != null){
			final String basis = FLEFRecordHelper.getChildValue(approx, TAG_BASIS);
			final String margin = FLEFRecordHelper.getChildValue(approx, TAG_MARGIN);
			if(basis != null || margin != null){
				dateStr.append(" (approx.");
				if(basis != null)
					dateStr.append(" basis: ")
						.append(basis);
				if(margin != null)
					dateStr.append(" margin: ")
						.append(margin);
				dateStr.append(')');
			}
			else
				dateStr.append(" (approx.)");
		}
		return dateStr.toString();
	}

	/**
	 * Extracts the actual date value from FULL_DATE, CENTURY, or DECADE (including CALENDAR).
	 */
	private static String getSingleDateDisplayText(final FLEFRecord parent){
		final FLEFRecord fullDate = parent.getTheOnlyChild(FULL_DATE);
		if(fullDate != null){
			final String value = FLEFRecordHelper.getChildValue(fullDate, TAG_VALUE);
			final String calendar = FLEFRecordHelper.getChildValue(fullDate, TAG_CALENDAR);
			return value + (calendar != null? " (" + calendar + ")": StringUtils.EMPTY);
		}

		final FLEFRecord decade = parent.getTheOnlyChild(TAG_DECADE);
		if(decade != null){
			final String startYear = FLEFRecordHelper.getChildValue(decade, TAG_START_YEAR);
			final String calendar = FLEFRecordHelper.getChildValue(decade, TAG_CALENDAR);
			return startYear + "s" + (calendar != null? " (" + calendar + ")": StringUtils.EMPTY);
		}

		final FLEFRecord century = parent.getTheOnlyChild(TAG_CENTURY);
		if(century != null){
			final String ordinal = FLEFRecordHelper.getChildValue(century, TAG_ORDINAL);
			final String part = FLEFRecordHelper.getChildValue(century, TAG_PART);
			final String calendar = FLEFRecordHelper.getChildValue(century, TAG_CALENDAR);
			String centuryStr = ordinal + "th century";
			if(part != null)
				centuryStr += " (" + part + ")";
			if(calendar != null)
				centuryStr += " (" + calendar + ")";
			return centuryStr;
		}

		return StringUtils.EMPTY;
	}

	private static String getBoundedDisplayText(final FLEFRecord boundedNode){
		final String notBefore = getQualifiedDateDisplayText(boundedNode, TAG_NOT_BEFORE);
		final String notAfter = getQualifiedDateDisplayText(boundedNode, TAG_NOT_AFTER);
		if(!notBefore.isEmpty() && !notAfter.isEmpty())
			return "between " + notBefore + " and " + notAfter;
		if(!notBefore.isEmpty())
			return "after " + notBefore;
		if(!notAfter.isEmpty())
			return "before " + notAfter;
		return "[bounded]";
	}

	private static String getSpanningDisplayText(final FLEFRecord spanningNode){
		final String from = getQualifiedDateDisplayText(spanningNode, TAG_FROM);
		final String to = getQualifiedDateDisplayText(spanningNode, TAG_TO);
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
	private static String getQualifiedDateDisplayText(final FLEFRecord parent, final String childTag){
		final FLEFRecord child = parent.getTheOnlyChild(childTag);
		return getQualifiedDateDisplayText(child);
	}


	/**
	 * Loads a DATE record into this panel.
	 *
	 * @param record	the DATE record (wrapper), or null
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
		return (dateRecord != null && !dateRecord.getChildren().isEmpty());
	}

	/**
	 * Validates the date (if present).
	 *
	 * @return true if valid or empty, false otherwise
	 */
	public boolean validateData(){
		if(!hasData())
			return true;

		// Delegate full validation to DatePanel (which knows the structure)
		final DateStructurePanel tempPanel = new DateStructurePanel(parent, model);
		tempPanel.load(dateRecord);
		return tempPanel.validateData();
	}

	/**
	 * Clears the date.
	 */
	public void clear(){
		dateRecord = null;

		updateSummary();
	}

}
