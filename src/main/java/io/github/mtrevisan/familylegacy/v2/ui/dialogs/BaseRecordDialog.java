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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.io.Serial;
import java.util.List;


/**
 * Abstract base class for all record editing dialogs.
 * Provides common functionality and utility methods.
 */
public abstract class BaseRecordDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 6460878052412992481L;


	protected final FLEFModel model;
	protected final FLEFRecord record;
	protected final boolean isNew;


	protected BaseRecordDialog(final Frame parent, final String title, final FLEFModel model, final FLEFRecord record){
		super(parent, title, true);

		this.model = model;
		this.record = (record != null? record: createNewRecord());
		this.isNew = (record == null);
	}

	// ==================== Abstract methods ====================

	protected abstract void initComponents();

	protected abstract void loadData();

	/**
	 * Validates the data before saving.
	 * Subclasses must implement this method to check required fields.
	 *
	 * @return	Whether the data is valid.
	 */
	protected abstract boolean validateData();

	/**
	 * Saves the record data to the model.
	 * Subclasses must call validateData() at the beginning of this method.
	 */
	protected abstract void saveRecord();

	protected abstract FLEFRecord createNewRecord();

	protected abstract String generateNewId();

	/**
	 * Public save method that performs validation and then saves.
	 * Called by the Save button.
	 */
	public final void save(){
		if(validateData())
			saveRecord();
	}

	// ==================== Utility methods ====================

	protected String getChildValue(final String tag){
		return FLEFRecordUtils.getChildValue(record, tag);
	}

	protected String getChildValue(final FLEFRecord parent, final String tag){
		return FLEFRecordUtils.getChildValue(parent, tag);
	}

	protected FLEFRecord findChild(final String tag){
		return FLEFRecordUtils.findChild(record, tag);
	}

	protected FLEFRecord findChild(final FLEFRecord parent, final String tag){
		return FLEFRecordUtils.findChild(parent, tag);
	}

	protected List<FLEFRecord> findChildren(final String tag){
		return FLEFRecordUtils.findChildren(record, tag);
	}

	protected List<FLEFRecord> findChildren(final FLEFRecord parent, final String tag){
		return FLEFRecordUtils.findChildren(parent, tag);
	}

	protected String getChildValuesAsString(final String tag){
		return FLEFRecordUtils.getChildValuesAsString(record, tag);
	}

	protected String getChildValuesAsString(final FLEFRecord parent, final String tag){
		return FLEFRecordUtils.getChildValuesAsString(parent, tag);
	}

	protected void updateChildValue(final String tag, final String value){
		FLEFRecordUtils.updateChildValue(record, tag, value);
	}

	protected void updateChildValue(final FLEFRecord parent, final String tag, final String value){
		FLEFRecordUtils.updateChildValue(parent, tag, value);
	}

	protected void addChild(final String tag, final String value){
		FLEFRecordUtils.addChild(record, tag, value);
	}

	protected void addChild(final FLEFRecord parent, final String tag, final String value){
		FLEFRecordUtils.addChild(parent, tag, value);
	}


	protected void removeChildren(final String tag){
		FLEFRecordUtils.removeChildren(record, tag);
	}

	protected void removeChildren(final FLEFRecord parent, final String tag){
		FLEFRecordUtils.removeChildren(parent, tag);
	}


	protected JPanel createTextAreaPanel(final String label){
		final JPanel panel = new JPanel(new BorderLayout());
		final JLabel lbl = new JLabel(label);
		lbl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		panel.add(lbl, BorderLayout.NORTH);
		return panel;
	}


	protected void showError(final String title, final String message){
		JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
	}

	protected void showInfo(final String title, final String message){
		JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
	}

	protected boolean showConfirm(final String title, final String message){
		final int selectedOption = JOptionPane.showConfirmDialog(this, message, title,
			JOptionPane.YES_NO_OPTION);
		return (selectedOption == JOptionPane.YES_OPTION);
	}

}
