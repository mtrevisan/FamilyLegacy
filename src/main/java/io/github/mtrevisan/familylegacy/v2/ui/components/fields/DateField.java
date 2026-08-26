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
package io.github.mtrevisan.familylegacy.v2.ui.components.fields;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.structures.DateStructureDialog;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dialog;
import java.io.Serial;


//TODO ParticipantField?
/**
 * Component for selecting and displaying dates.
 */
public class DateField extends JPanel{

	@Serial
	private static final long serialVersionUID = 4495716172290856838L;


	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_START_YEAR = "START_YEAR";
	private static final String TAG_POINT = "POINT";
	private static final String TAG_BOUNDED = "BOUNDED";
	private static final String TAG_SPANNING = "SPANNING";
	private static final String TAG_APPROXIMATE = "APPROXIMATE";
	private static final String TAG_BASIS = "BASIS";
	private static final String TAG_MARGIN = "MARGIN";
	private static final String TAG_CALENDAR = "CALENDAR";
	private static final String TAG_FULL_DATE = "FULL_DATE";
	private static final String TAG_DECADE = "DECADE";
	private static final String TAG_CENTURY = "CENTURY";
	private static final String TAG_ORDINAL = "ORDINAL";
	private static final String TAG_PART = "PART";
	private static final String TAG_NOT_BEFORE = "NOT_BEFORE";
	private static final String TAG_NOT_AFTER = "NOT_AFTER";
	private static final String TAG_FROM = "FROM";
	private static final String TAG_TO = "TO";


	private final Dialog parent;
	private final String dialogTitle;

	private final String path;
	private final FLEFModel model;

	private FLEFRecord record;

	private final JTextField displayField = new JTextField(null);


	public static DateField create(final Dialog parent, final String dialogTitle, final FLEFModel model){
		return new DateField(null, parent, dialogTitle, model);
	}

	public static DateField createWithWrapperTag(final String path, final Dialog parent, final String dialogTitle,
			final FLEFModel model){
		return new DateField(path, parent, dialogTitle, model);
	}


	private DateField(final String path, final Dialog parent, final String dialogTitle, final FLEFModel model){
		super(new MigLayout("ins 0,fillx", "[grow]"));

		this.parent = parent;
		this.dialogTitle = dialogTitle;

		this.path = path;
		this.model = model;


		initComponents();
	}


	private void initComponents(){
		setupField(displayField,
			this::createNew,
			this::edit,
			this::clear
		);

		add(displayField, "growx");
	}

	private void setupField(final JTextField field,
			final Runnable newAction, final Runnable editAction, final Runnable clearAction){
		GUIHelper.installBehavior(field,
			editAction, null,
			null, null,
			builder -> {
				builder.item("Set…", newAction);
				builder.separator();
				builder.selectionSensitiveItem("Edit…", editAction);
				builder.selectionSensitiveItem("Clear", clearAction);
			}
		);

		updateDisplay();
	}

	/**
	 * Updates the underlying record and automatically refreshes the display.
	 */
	public void setRecord(final FLEFRecord record){
		this.record = record;

		updateDisplay();
	}

	public void clear(){
		setRecord(null);
	}

	public boolean hasData(){
		return (record != null && record.hasData());
	}

	public void load(final FLEFRecord record){
		clear();

		if(record == null || record.isEmpty())
			return;

		final FLEFRecord child = FLEFRecordHelper.findChild(record, path);
		setRecord(child);
	}

	public void save(final FLEFRecord targetRecord){
		if(record != null){
			final FLEFRecord targetNode = FLEFRecordHelper.getOrCreateTargetNode(targetRecord, path);
			targetNode.addChildren(record.getChildren());
		}
	}

	private void createNew(){
		final DateStructureDialog dialog = DateStructureDialog.createNew(parent, model, dialogTitle);
		dialog.setVisible(true);

		if(dialog.isSaved())
			setRecord(dialog.getRecord());
	}

	private void edit(){
		if(!hasData()){
			createNew();

			return;
		}

		final DateStructureDialog dialog = DateStructureDialog.createEdit(parent, model, dialogTitle, record);
		dialog.setVisible(true);

		if(dialog.isSaved())
			// Only necessary here if changes are in-place
			updateDisplay();
	}

	private void updateDisplay(){
		GUIHelper.updateDisplay(displayField,
			this::hasData,
			() -> getDateValueDisplayText(record));
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
			return getDateDisplayText(value);

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
	 * Extracts a single date string from a node that may contain FULL_DATE, DECADE, or CENTURY
	 * and optional APPROXIMATE.
	 */
	private static String getDateDisplayText(final FLEFRecord node){
		final StringBuilder dateStr = new StringBuilder(getSingleDateDisplayText(node));
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
	 * Extracts the actual date value from FULL_DATE, DECADE, or CENTURY (including CALENDAR).
	 */
	private static String getSingleDateDisplayText(final FLEFRecord parent){
		final FLEFRecord fullDate = parent.getTheOnlyChild(TAG_FULL_DATE);
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
		final FLEFRecord notBeforeRecord = boundedNode.getTheOnlyChild(TAG_NOT_BEFORE);
		final String notBefore = getSingleDateDisplayText(notBeforeRecord);
		final FLEFRecord notAfterRecord = boundedNode.getTheOnlyChild(TAG_NOT_AFTER);
		final String notAfter = getSingleDateDisplayText(notAfterRecord);
		if(!notBefore.isEmpty() && !notAfter.isEmpty())
			return "between " + notBefore + " and " + notAfter;
		if(!notBefore.isEmpty())
			return "after " + notBefore;
		if(!notAfter.isEmpty())
			return "before " + notAfter;
		return "[bounded]";
	}

	private static String getSpanningDisplayText(final FLEFRecord spanningNode){
		final FLEFRecord fromRecord = spanningNode.getTheOnlyChild(TAG_FROM);
		final String from = getSingleDateDisplayText(fromRecord);
		final FLEFRecord toRecord = spanningNode.getTheOnlyChild(TAG_TO);
		final String to = getSingleDateDisplayText(toRecord);
		if(!from.isEmpty() && !to.isEmpty())
			return "from " + from + " to " + to;
		if(!from.isEmpty())
			return "from " + from;
		if(!to.isEmpty())
			return "until " + to;
		return "[spanning]";
	}


	@Override
	public String toString(){
		final StringBuilder sb = new StringBuilder();
		sb.append("value: ");
		final String text = GUIHelper.getText(displayField.getText());
		sb.append(text != null? (text.isEmpty()? "''": text): "<null>")
			.append(", path: ")
			.append(path);
		return sb.toString();
	}

}
