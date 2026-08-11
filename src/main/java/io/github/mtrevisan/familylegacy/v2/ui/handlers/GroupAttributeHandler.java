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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GroupAttributeRecordDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


/* DONE */
/**
 * Handler for {@code GROUP_ATTRIBUTE_RECORD} entities according to FLEF 0.1.1.
 */
public class GroupAttributeHandler implements RecordTypeHandler<GroupAttributeRecordDialog>{

	public static final String TYPE = "GROUP_ATTRIBUTE";
	public static final String ID_PREFIX = "GA";

	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_VALUE = "VALUE";


	@Override
	public String getLabel(){
		return "Group Attribute";
	}

	@Override
	public String getType(){
		return TYPE;
	}

	@Override
	public String getIdPrefix(){
		return ID_PREFIX;
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		if(record == null)
			return "--";

		final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
		final String value = FLEFRecordHelper.getChildValue(record, TAG_VALUE);
		final StringBuilder sb = new StringBuilder();
		if(type != null)
			sb.append('(')
				.append(type)
				.append(')');
		if(type != null && StringUtils.isNotEmpty(value))
			sb.append(StringUtils.SPACE);
		if(StringUtils.isNotEmpty(value))
			sb.append(value);
		return sb.toString();
	}

	@Override
	public GroupAttributeRecordDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return GroupAttributeRecordDialog.createNew(parent, model);
	}

	@Override
	public GroupAttributeRecordDialog createEditDialog(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return GroupAttributeRecordDialog.createEdit(parent, model, record);
	}

}
