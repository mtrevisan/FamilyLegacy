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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.ResearchTaskRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


/**
 * Handler for ResearchTaskRecord.
 */
public class ResearchTaskHandler extends AbstractRecordTypeHandler<ResearchTaskRecordDialog>{

	public static final String TYPE = "RESEARCH_TASK";
	public static final String ID_PREFIX = "RT";


	private static final class SingletonHelper{
		private static final ResearchTaskHandler INSTANCE = new ResearchTaskHandler();

	}


	public static ResearchTaskHandler getInstance(){
		return ResearchTaskHandler.SingletonHelper.INSTANCE;
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
	public String getLabel(){
		return "Research Task";
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		if(record == null)
			return "--";

		String description = FLEFRecordHelper.getChildValue(record, "DESCRIPTION");
		String status = FLEFRecordHelper.getChildValue(record, "STATUS");
		if(StringUtils.isNotEmpty(description)){
			String display = GUIHelper.limitTextLength(description);
			if(StringUtils.isNotEmpty(status))
				display += " [" + status + "]";
			return display;
		}
		return record.getId() != null? record.getId(): "(unnamed)";
	}

	@Override
	public ResearchTaskRecordDialog createNewDialog(Dialog parent, FLEFModel model){
		return ResearchTaskRecordDialog.createNew(parent, model);
	}

	@Override
	public ResearchTaskRecordDialog createEditDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		return ResearchTaskRecordDialog.createEdit(parent, model, record);
	}

}
