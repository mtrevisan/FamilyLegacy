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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GroupDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


/**
 * Handler for {@code GROUP_RECORD} entities according to FLEF 0.1.0.
 * <p>
 * This handler provides the necessary operations for managing group records:
 * creation, editing, display name generation, and type identification.
 * <p>
 * Structure:
 * <pre>
 * GROUP_RECORD :=
 * n @<XREF:GROUP>@ GROUP    {1:1}
 *   +1 <<NAME_STRUCTURE>>    {0:M}
 *   +1 TYPE <GROUP_TYPE>    {0:1}
 *   +1 CULTURAL_NORM @<XREF:CULTURAL_NORM>@    {0:M}
 *   +1 NOTE @<XREF:NOTE>@    {0:M}
 *   +1 <<SOURCE_CITATION>>    {0:M}
 *   +1 PREFERRED_IMAGE <RESOURCE_URI>    {0:1}
 *     +2 CROP <CROP_COORDINATES>    {0:1}
 *   +1 <<RESTRICTION_STRUCTURE>>    {0:1}
 *   +1 <<CONCLUSION_STRUCTURE>>    {0:M}
 *   +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class GroupHandler implements RecordTypeHandler<GroupDialog>{

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
	public GroupDialog createNewDialog(Dialog parent, FLEFModel model){
		return GroupDialog.createNew(parent, model);
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
	public GroupDialog createEditDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		return GroupDialog.createEdit(parent, model, record);
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
	public String getDisplayText(FLEFRecord record){
		if(record == null){
			return StringUtils.EMPTY;
		}

		// Find NAME_STRUCTURE -> TEXT_VALUE -> VALUE
		FLEFRecord nameStruct = FLEFRecordUtils.findChild(record, "NAME_STRUCTURE");
		if(nameStruct != null){
			FLEFRecord textValue = FLEFRecordUtils.findChild(nameStruct, "TEXT_VALUE");
			if(textValue != null){
				String value = FLEFRecordUtils.getChildValue(textValue, "VALUE");
				if(value != null && !value.isEmpty()){
					return value;
				}
			}
		}

		// Fallback to the record ID
		return record.getId();
	}

}
