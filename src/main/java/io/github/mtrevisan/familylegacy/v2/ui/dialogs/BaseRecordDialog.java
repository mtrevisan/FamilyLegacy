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

import io.github.mtrevisan.familylegacy.v2.io.FLEFWriter;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.List;


/**
 * Abstract base class for all record editing dialogs.
 * Provides common functionality and utility methods.
 */
public abstract class BaseRecordDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 6460878052412992481L;


	protected final RecordTypeHandler<?> handler;
	protected final FLEFModel model;
	protected final FLEFRecord record;
	protected final boolean isNew;
	protected boolean isSaved;


	protected BaseRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record,
			final RecordTypeHandler<?> handler){
		super(parent, buildTitle(handler, record), true);

		this.handler = handler;
		this.model = model;
		this.record = (record != null? record: createNewRecord());
		this.isNew = (record == null);
	}

	private static String buildTitle(final RecordTypeHandler<?> handler, final FLEFRecord record){
		final String label = handler.getLabel();
		if(record == null)
			return "New " + label;

		String id = record.getId();
		if(id == null)
			// in case of a citation
			id = XRefHelper.extractXRef(FLEFRecordHelper.getChildValue(record, handler.getCitedType()));
		return "Edit " + label + " (" + id + ")";
	}


	protected abstract void loadData();

	/**
	 * Public save method that performs validation and then saves.
	 * Called by the Save button.
	 */
	public final void save(){
		if(validData()){
			record.clear();

			saveData();

			if(isNew && handler.isTopLevelEntity())
				model.addRecord(record);
			isSaved = true;

// TODO to be removed
System.out.println(FLEFWriter.create().writeToString(model));
//		dispose();
		}
	}

	/**
	 * Validates the data before saving.
	 * Subclasses must implement this method to check required fields.
	 *
	 * @return	Whether the data is valid.
	 */
	protected abstract boolean validData();

	/**
	 * Saves the record data to the model.
	 * Subclasses must call validateData() at the beginning of this method.
	 */
	protected abstract void saveData();

	private FLEFRecord createNewRecord(){
		if(!handler.isTopLevelEntity())
			return FLEFRecord.createEmpty();

		return FLEFRecord.createMainRecord(generateNewId(), handler.getType());
	}

	private String generateNewId(){
		return XRefHelper.generateNewId(model, handler.getType(), handler.getIdPrefix());
	}


	protected String getChildValue(final String tag){
		return FLEFRecordHelper.getChildValue(record, tag);
	}

	protected String getChildValue(final FLEFRecord parent, final String tag){
		return FLEFRecordHelper.getChildValue(parent, tag);
	}

	protected FLEFRecord findChild(final String tag){
		return FLEFRecordHelper.findChild(record, tag);
	}

	protected FLEFRecord findChild(final FLEFRecord parent, final String tag){
		return FLEFRecordHelper.findChild(parent, tag);
	}

	protected List<FLEFRecord> findChildren(final String tag){
		return FLEFRecordHelper.findChildren(record, tag);
	}

	protected List<FLEFRecord> findChildren(final FLEFRecord parent, final String tag){
		return FLEFRecordHelper.findChildren(parent, tag);
	}

	protected String getChildValuesAsString(final String tag){
		return FLEFRecordHelper.getChildValuesAsString(record, tag);
	}

	protected String getChildValuesAsString(final FLEFRecord parent, final String tag){
		return FLEFRecordHelper.getChildValuesAsString(parent, tag);
	}

	protected void updateChildValue(final String tag, final String value){
		FLEFRecordHelper.updateChildValue(record, tag, value);
	}

	protected void updateChildValue(final FLEFRecord parent, final String tag, final String value){
		FLEFRecordHelper.updateChildValue(parent, tag, value);
	}

	protected void addChild(final String tag, final String value){
		FLEFRecordHelper.addChild(record, tag, value);
	}

	protected void addChild(final FLEFRecord parent, final String tag, final String value){
		FLEFRecordHelper.addChild(parent, tag, value);
	}


	protected void removeChildren(final String tag){
		FLEFRecordHelper.removeChildren(record, tag);
	}

	protected void removeChildren(final FLEFRecord parent, final String tag){
		FLEFRecordHelper.removeChildren(parent, tag);
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

	public boolean isSaved(){
		return isSaved;
	}

	public FLEFRecord getRecord(){
		return (!record.isEmpty()? record: null);
	}

}
