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
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;


/**
 * Abstract base class for all record editing dialogs.
 * Provides common functionality and utility methods.
 * <p>
 * IMPORTANT: Subclasses MUST call initComponents() and loadData()
 * in their constructors after calling super().
 */
public abstract class BaseRecordDialog extends JDialog{

	protected final FLEFModel model;
	protected final FLEFRecord record;
	protected final boolean isNew;

	/**
	 * Constructor for editing an existing record.
	 * Subclasses must call initComponents() and loadData() after this.
	 */
	protected BaseRecordDialog(Frame parent, FLEFModel model, FLEFRecord record, String title){
		super(parent, title, true);
		this.model = model;
		this.record = record;
		this.isNew = false;
	}

	/**
	 * Constructor for creating a new record.
	 * Subclasses must call initComponents() and loadData() after this.
	 */
	protected BaseRecordDialog(Frame parent, FLEFModel model, String title){
		super(parent, title, true);
		this.model = model;
		this.record = createNewRecord();
		this.isNew = true;
	}

	// ==================== Abstract methods ====================

	/**
	 * Initializes the UI components. Must be implemented by subclasses.
	 * Called by the subclass constructor after super().
	 */
	protected abstract void initComponents();

	/**
	 * Loads data from the record into the UI. Must be implemented by subclasses.
	 * Called by the subclass constructor after initComponents().
	 */
	protected abstract void loadData();

	/**
	 * Saves the record data to the model. Must be implemented by subclasses.
	 * Called when the user clicks the Save button.
	 */
	protected abstract void saveRecord();

	/**
	 * Creates a new empty record for this dialog type.
	 * Used when creating a new record.
	 *
	 * @return a new empty FLEFRecord
	 */
	protected abstract FLEFRecord createNewRecord();

	/**
	 * Generates a new unique ID for this record type.
	 *
	 * @return a new unique ID
	 */
	protected abstract String generateNewId();

	// ==================== Utility methods (delegated to FLEFRecordUtils) ====================

	/**
	 * Finds the value of the first child with the given tag.
	 */
	protected String getChildValue(String tag){
		return FLEFRecordUtils.getChildValue(record, tag);
	}

	/**
	 * Finds the value of the first child with the given tag in a specific parent.
	 */
	protected String getChildValue(FLEFRecord parent, String tag){
		return FLEFRecordUtils.getChildValue(parent, tag);
	}

	/**
	 * Finds the first child with the given tag.
	 */
	protected FLEFRecord findChild(String tag){
		return FLEFRecordUtils.findChild(record, tag);
	}

	/**
	 * Finds the first child with the given tag in a specific parent.
	 */
	protected FLEFRecord findChild(FLEFRecord parent, String tag){
		return FLEFRecordUtils.findChild(parent, tag);
	}

	/**
	 * Finds all children with the given tag.
	 */
	protected List<FLEFRecord> findChildren(String tag){
		return FLEFRecordUtils.findChildren(record, tag);
	}

	/**
	 * Finds all children with the given tag in a specific parent.
	 */
	protected List<FLEFRecord> findChildren(FLEFRecord parent, String tag){
		return FLEFRecordUtils.findChildren(parent, tag);
	}

	/**
	 * Collects values of all children with the given tag as a comma-separated string.
	 */
	protected String getChildValuesAsString(String tag){
		return FLEFRecordUtils.getChildValuesAsString(record, tag);
	}

	/**
	 * Collects values of all children with the given tag in a specific parent as a comma-separated string.
	 */
	protected String getChildValuesAsString(FLEFRecord parent, String tag){
		return FLEFRecordUtils.getChildValuesAsString(parent, tag);
	}

	/**
	 * Updates or creates a child with the given tag and value.
	 */
	protected void updateChildValue(String tag, String value){
		FLEFRecordUtils.updateChildValue(record, tag, value);
	}

	/**
	 * Updates or creates a child with the given tag and value in a specific parent.
	 */
	protected void updateChildValue(FLEFRecord parent, String tag, String value){
		FLEFRecordUtils.updateChildValue(parent, tag, value);
	}

	/**
	 * Adds a single child with the given tag and value.
	 */
	protected void addChild(String tag, int level, String value){
		FLEFRecordUtils.addChild(record, tag, level, value);
	}

	/**
	 * Adds a single child with the given tag and value to a specific parent.
	 */
	protected void addChild(FLEFRecord parent, String tag, int level, String value){
		FLEFRecordUtils.addChild(parent, tag, level, value);
	}

	/**
	 * Adds multiple children from a comma-separated string of values.
	 */
	protected void addChildrenFromString(String tag, String values){
		FLEFRecordUtils.addChildrenFromString(record, tag, values);
	}

	/**
	 * Adds multiple children from a comma-separated string of values to a specific parent.
	 */
	protected void addChildrenFromString(FLEFRecord parent, String tag, String values){
		FLEFRecordUtils.addChildrenFromString(parent, tag, values);
	}

	/**
	 * Removes all children with the given tag.
	 */
	protected void removeChildren(String tag){
		FLEFRecordUtils.removeChildren(record, tag);
	}

	/**
	 * Removes all children with the given tag from a specific parent.
	 */
	protected void removeChildren(FLEFRecord parent, String tag){
		FLEFRecordUtils.removeChildren(parent, tag);
	}

	// ==================== Common UI helpers ====================

	/**
	 * Helper to create a panel with a label for text areas.
	 */
	protected JPanel createTextAreaPanel(String label){
		JPanel panel = new JPanel(new BorderLayout());
		JLabel lbl = new JLabel(label);
		lbl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		panel.add(lbl, BorderLayout.NORTH);
		return panel;
	}

	/**
	 * Shows an error message dialog.
	 */
	protected void showError(String title, String message){
		JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Shows an information message dialog.
	 */
	protected void showInfo(String title, String message){
		JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Shows a confirmation dialog.
	 *
	 * @return true if the user confirmed, false otherwise
	 */
	protected boolean showConfirm(String title, String message){
		return JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	}

}
