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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.PartStructureDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


public class PartHandler implements RecordTypeHandler<PartStructureDialog>{

	public static final String TYPE = "PART";

	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_PHONETIC = "PHONETIC";
	private static final String TAG_TRANSCRIPTION = "TRANSCRIPTION";


	@Override
	public boolean isTopLevelEntity(){
		return false;
	}

	@Override
	public String getLabel(){
		return "Part";
	}

	@Override
	public String getType(){
		return TYPE	;
	}

	@Override
	public String getIdPrefix(){
		return null;
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		if(record == null)
			return "--";

		final String type = FLEFRecordHelper.getChildValue(record, "TYPE");
		final String value = FLEFRecordHelper.getChildValue(record, TAG_VALUE);

		final StringBuilder sb = new StringBuilder();
		if(StringUtils.isNotEmpty(type))
			sb.append("[")
				.append(type)
				.append("] ");
		if(StringUtils.isNotEmpty(value)){
			String val = value;
			if(val.length() > 50)
				val = val.substring(0, 50) + "...";
			sb.append(val);
		}
		else
			sb.append("--");

		// If it is a low-level element with no meaningful ID, the variant count is shown
		int variantCount = 0;
		for(final FLEFRecord child : record.getChildren())
			if(TAG_PHONETIC.equals(child.getTag()) || TAG_TRANSCRIPTION.equals(child.getTag()))
				variantCount++;

		if(variantCount > 0)
			sb.append(" (")
				.append(variantCount)
				.append(" variant")
				.append(variantCount > 1? "s": StringUtils.EMPTY)
				.append(")");
		else if(StringUtils.isNotEmpty(record.getId())){
			sb.append(" (")
				.append(record.getId())
				.append(")");
		}

		return sb.toString();
	}

	@Override
	public PartStructureDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return PartStructureDialog.createNew(parent, model);
	}

	@Override
	public PartStructureDialog createEditDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return PartStructureDialog.createEdit(parent, model, record);
	}

}
