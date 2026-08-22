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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.ResearchActivityRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


/**
 * Handler for ResearchActivityRecord.
 */
public class ResearchActivityHandler extends AbstractRecordTypeHandler<ResearchActivityRecordDialog>{

	public static final String TYPE = "RESEARCH_ACTIVITY";
	public static final String ID_PREFIX = "RA";

	private static final String TAG_ACTION = "ACTION";
	private static final String TAG_ACTIVITY_TYPE = "ACTIVITY_TYPE";


	@Override
	public String getType(){
		return TYPE;
	}

	@Override
	public String getIdPrefix(){
		return ID_PREFIX;
	}

	@Override
	public String getLabel(){
		return "Research Activity";
	}

	@Override
	public String getDisplayText(FLEFRecord record, FLEFModel model){
		if(record == null)
			return "--";

		final String action = FLEFRecordHelper.getChildValue(record, TAG_ACTION);
		final String type = FLEFRecordHelper.getChildValue(record, TAG_ACTIVITY_TYPE);
		if(StringUtils.isNotEmpty(action)){
			String display = GUIHelper.limitTextLength(action);
			if(StringUtils.isNotEmpty(type))
				display += " [" + type + "]";
			return display;
		}
		return record.getId() != null? record.getId(): "(unnamed)";
	}

	@Override
	public ResearchActivityRecordDialog createNewDialog(Dialog parent, FLEFModel model){
		return ResearchActivityRecordDialog.createNew(parent, model);
	}

	@Override
	public ResearchActivityRecordDialog createEditDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		return ResearchActivityRecordDialog.createEdit(parent, model, record);
	}

}
