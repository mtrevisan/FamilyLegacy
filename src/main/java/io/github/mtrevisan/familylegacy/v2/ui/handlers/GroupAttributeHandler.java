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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs._GroupAttributeDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


/**
 * Handler for {@code GROUP_ATTRIBUTE_RECORD} entities according to FLEF 0.1.0.
 * <p>
 * This handler provides the necessary operations for managing group attribute records:
 * creation, editing, display name generation, and type identification.
 * <p>
 * Structure:
 * <pre>
 * GROUP_ATTRIBUTE_RECORD :=
 * n @<XREF:GROUP_ATTRIBUTE>@ GROUP_ATTRIBUTE    {1:1}
 *   +1 GROUP @<XREF:GROUP>@    {1:1}
 *   +1 TYPE [ RESIDENCE | CHILDREN_COUNT | SOCIAL_CLASS | <ATTRIBUTE_TYPE> ]    {1:1}
 *   +1 VALUE <TEXT>    {0:1}
 *   +1 <<DATE_STRUCTURE>>    {0:1}
 *   +1 VALID_FROM    {0:1}
 *     +2 <<DATE_STRUCTURE>>    {1:1}
 *   +1 VALID_TO    {0:1}
 *     +2 <<DATE_STRUCTURE>>    {1:1}
 *   +1 <<PLACE_STRUCTURE>>    {0:1}
 *   +1 <<SOURCE_CITATION>>    {0:M}
 *   +1 <<EVIDENCE_QUALIFIERS>>    {0:1}
 *   +1 <<RESTRICTION_STRUCTURE>>    {0:1}
 *   +1 <<CONCLUSION_STRUCTURE>>    {0:1}
 *   +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class GroupAttributeHandler implements RecordTypeHandler<_GroupAttributeDialog>{

	/** The record type identifier for group attributes. */
	public static final String TYPE = "GROUP_ATTRIBUTE";


	@Override
	public boolean isTopLevelEntity(){
		return false;
	}

	@Override
	public String getLabel(){
		return "Group Attribute";
	}

	@Override
	public String getType(){
		return TYPE;
	}

	@Override
	public String getIDPrefix(){
		return null;
	}

	/**
	 * Creates a new group attribute dialog for creating a new attribute record.
	 *
	 * @param parent the parent frame
	 * @param model  the FLEF model
	 * @return a new {@code GroupAttributeDialog} in create mode
	 */
	@Override
	public _GroupAttributeDialog createNewDialog(Dialog parent, FLEFModel model){
		return _GroupAttributeDialog.createNew(parent, model);
	}

	/**
	 * Creates a new group attribute dialog for editing an existing attribute record.
	 *
	 * @param parent the parent frame
	 * @param model  the FLEF model
	 * @param record the group attribute record to edit
	 * @return a new {@code GroupAttributeDialog} in edit mode
	 */
	@Override
	public _GroupAttributeDialog createEditDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		return _GroupAttributeDialog.createEdit(parent, model, record);
	}

	/**
	 * Returns a display name for the given group attribute record.
	 * <p>
	 * The display name is composed of the attribute type and the associated group name.
	 * Format: {@code "TYPE (GroupName)"} or {@code "TYPE (ID)"} if the group name
	 * is not available.
	 *
	 * @param record the group attribute record
	 * @return a human-readable display name
	 */
	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		if(record == null){
			return StringUtils.EMPTY;
		}

		// Get the attribute type
		String type = FLEFRecordHelper.getChildValue(record, "TYPE");
		String typeDisplay = (type != null && !type.isEmpty())? type: "?";

		// Get the associated group
		String groupId = FLEFRecordHelper.getChildValue(record, "GROUP");
		String groupDisplay = groupId;

		//FIXME
//		if(groupId != null){
//			// Try to resolve the group and get its display name
//			FLEFModel model = FLEFModel.getInstance(); // Assumes a singleton or a way to get the model
//			if(model != null){
//				FLEFRecord group = model.getRecordById(groupId);
//				if(group != null){
//					// Use the group's display name from its handler
//					RecordTypeHandler<?> handler = HandlerRegistry.getHandler(GroupHandler.TYPE);
//					groupDisplay = handler.getDisplayName(group);
//				}
//			}
//		}

		return typeDisplay + " (" + groupDisplay + ")";
	}

}
