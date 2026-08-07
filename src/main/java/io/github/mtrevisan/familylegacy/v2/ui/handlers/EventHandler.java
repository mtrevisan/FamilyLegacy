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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.EventRecordDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


public class EventHandler implements RecordTypeHandler<EventRecordDialog>{

	public static final String TYPE = "EVENT";
	public static final String ID_PREFIX = "E";


	@Override
	public String getLabel(){
		return "Event";
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
		String type = getChildValue(record, "TYPE");
		String date = getChildValue(record, "DATE");
		String id = record.getId();
		StringBuilder sb = new StringBuilder();
		if(type != null)
			sb.append(type);
		if(date != null){
			if(!sb.isEmpty())
				sb.append(StringUtils.SPACE);
			sb.append("(")
				.append(date)
				.append(")");
		}
		if(sb.isEmpty())
			sb.append("Event");
		sb.append(" (")
			.append(id)
			.append(")");
		return sb.toString();
	}

	private String getChildValue(FLEFRecord parent, String tag){
		for(FLEFRecord child : parent.getChildren()){
			if(tag.equals(child.getTag())){
				return child.getValue();
			}
		}
		return null;
	}

	@Override
	public EventRecordDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return EventRecordDialog.createNew(parent, model);
	}

	@Override
	public EventRecordDialog createEditDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return EventRecordDialog.createEdit(parent, model, record);
	}

}
