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
package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GroupRecordDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


public class GroupHandler implements RecordTypeHandler<GroupRecordDialog>{

	/** The record type identifier for groups. */
	public static final String TYPE = "GROUP";
	/** The ID prefix used for generating new group IDs (e.g., {@code G}). */
	public static final String ID_PREFIX = "G";


	@Override
	public String getLabel(){
		return "Group";
	}

	@Override
	public String getType(){
		return TYPE;
	}

	@Override
	public String getIDPrefix(){
		return ID_PREFIX;
	}

	/**
	 * Creates a new group dialog for creating a new group record.
	 *
	 * @param parent the parent frame
	 * @param model  the FLEF model
	 * @return a new {@code GroupDialog} in create mode
	 */
	@Override
	public GroupRecordDialog createNewDialog(Dialog parent, FLEFModel model){
		return GroupRecordDialog.createNew(parent, model);
	}

	/**
	 * Creates a new group dialog for editing an existing group record.
	 *
	 * @param parent the parent frame
	 * @param model  the FLEF model
	 * @param record the group record to edit
	 * @return a new {@code GroupDialog} in edit mode
	 */
	@Override
	public GroupRecordDialog createEditDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		return GroupRecordDialog.createEdit(parent, model, record);
	}

	/**
	 * Returns a display name for the given group record.
	 * <p>
	 * The display name is the first {@code VALUE} found inside
	 * {@code NAME_STRUCTURE} → {@code TEXT_VALUE}. If no name is available,
	 * the record ID is returned.
	 *
	 * @param record the group record
	 * @return a human-readable display name
	 */
	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		if(record == null)
			return StringUtils.EMPTY;

		// Find NAME_STRUCTURE -> TEXT_VALUE -> VALUE
		FLEFRecord nameStruct = FLEFRecordHelper.findChild(record, "NAME_STRUCTURE");
		if(nameStruct != null){
			FLEFRecord textValue = FLEFRecordHelper.findChild(nameStruct, "TEXT_VALUE");
			if(textValue != null){
				String value = FLEFRecordHelper.getChildValue(textValue, "VALUE");
				if(value != null && !value.isEmpty()){
					return value;
				}
			}
		}

		// Fallback to the record ID
		return record.getId();
	}

}
