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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.TextValueVariantDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


public class VariantHandler extends AbstractRecordTypeHandler<TextValueVariantDialog>{

	public static final String TYPE = "TEXT_VALUE_VARIANT";

	private static final String TAG_PHONETIC = "PHONETIC";
	private static final String TAG_TRANSCRIPTION = "TRANSCRIPTION";
	private static final String TAG_SYSTEM = "SYSTEM";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_VALUE = "VALUE";


	@Override
	public boolean isTopLevelEntity(){
		return false;
	}

	@Override
	public String getLabel(){
		return "Text Value Variant";
	}

	@Override
	public String getType(){
		return TYPE;
	}

	@Override
	public String getIdPrefix(){
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		if(record == null)
			return "--";

		String tag = record.getTag();
		if(TAG_PHONETIC.equals(tag)){
			final String system = FLEFRecordHelper.getChildValue(record, TAG_SYSTEM);
			final String value = FLEFRecordHelper.getChildValue(record, TAG_VALUE);

			final StringBuilder details = new StringBuilder();
			if(StringUtils.isNotEmpty(system))
				details.append(system);
			if(!details.isEmpty())
				return String.format("%s [%s: %s]", value, TAG_PHONETIC, details);
			return String.format("%s [%s]", value, TAG_PHONETIC);
		}
		else if(TAG_TRANSCRIPTION.equals(tag)){
			final String system = FLEFRecordHelper.getChildValue(record, TAG_SYSTEM);
			final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
			final String value = FLEFRecordHelper.getChildValue(record, TAG_VALUE);

			final StringBuilder details = new StringBuilder();
			if(StringUtils.isNotEmpty(system))
				details.append(system);
			if(StringUtils.isNotEmpty(type)){
				if(!details.isEmpty())
					details.append(", ");
				details.append(type);
			}
			if(!details.isEmpty())
				return String.format("%s [%s: %s]", value, TAG_TRANSCRIPTION, details);
			return String.format("%s [%s]", value, TAG_TRANSCRIPTION);
		}

		// Fallback to the record ID
		return "[" + record.getId() + "]";
	}

	@Override
	public TextValueVariantDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return TextValueVariantDialog.createNew(parent, model);
	}

	@Override
	public TextValueVariantDialog createEditDialog(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return TextValueVariantDialog.createEdit(parent, model, record);
	}

}
