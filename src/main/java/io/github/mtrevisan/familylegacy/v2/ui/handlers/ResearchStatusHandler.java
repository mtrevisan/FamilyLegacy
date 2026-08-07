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
import io.github.mtrevisan.familylegacy.v2.ui.dialogstodo._ResearchStatusDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


/**
 * Handler for RESEARCH_STATUS records.
 */
public class ResearchStatusHandler implements RecordTypeHandler<_ResearchStatusDialog>{

	public static final String TYPE = "RESEARCH_STATUS";
	public static final String ID_PREFIX = "RS";

	private static final String TAG_QUESTION = "QUESTION";
	private static final String TAG_STATUS = "STATUS";


	@Override
	public String getLabel(){
		return "Research Status";
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
		// Try to get the QUESTION field as display name
		String question = FLEFRecordHelper.getChildValue(record, TAG_QUESTION);
		String status = FLEFRecordHelper.getChildValue(record, TAG_STATUS);
		String id = record.getId();

		StringBuilder sb = new StringBuilder();
		if(question != null && !question.isEmpty()){
			// Truncate long questions
			if(question.length() > 40){
				question = question.substring(0, 40) + "...";
			}
			sb.append(question);
		}

		if(status != null && !status.isEmpty()){
			if(!sb.isEmpty())
				sb.append(StringUtils.SPACE);
			sb.append("(")
				.append(status)
				.append(")");
		}

		if(sb.isEmpty()){
			sb.append("Research Status ");
		}

		sb.append(" (")
			.append(id)
			.append(")");
		return sb.toString();
	}

	@Override
	public _ResearchStatusDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return new _ResearchStatusDialog(parent, model);
	}

	@Override
	public _ResearchStatusDialog createEditDialog(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return new _ResearchStatusDialog(parent, model, record);
	}

}
