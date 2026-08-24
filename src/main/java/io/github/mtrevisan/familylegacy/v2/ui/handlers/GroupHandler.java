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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.GroupRecordDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


public class GroupHandler extends AbstractRecordTypeHandler<GroupRecordDialog>{

	public static final String TYPE = "GROUP";
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
	public String getIdPrefix(){
		return ID_PREFIX;
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		if(record == null)
			return "--";

		// Locate the first populated NAME structure
		final String name = FLEFRecordHelper.getChildValue(record, "NAME.TEXT.VALUE");

		final String type = FLEFRecordHelper.getChildValue(record, "TYPE");

		final String id = record.getId();
		final StringBuilder sb = new StringBuilder();
		if(StringUtils.isNotEmpty(name))
			sb.append(name);
		if(StringUtils.isNotEmpty(type)){
			if(!sb.isEmpty())
				sb.append(' ');
			sb.append('(')
				.append(type)
				.append(')');
		}
		if(!sb.isEmpty())
			sb.append(' ');
		sb.append('[')
			.append(id)
			.append(']');

		return sb.toString();
	}

	@Override
	public GroupRecordDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return GroupRecordDialog.createNew(parent, model);
	}

	@Override
	public GroupRecordDialog createEditDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return GroupRecordDialog.createEdit(parent, model, record);
	}

}
