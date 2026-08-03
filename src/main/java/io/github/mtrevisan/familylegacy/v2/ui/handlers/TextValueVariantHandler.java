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


public class TextValueVariantHandler implements RecordTypeHandler<TextValueVariantDialog>{

	/** The record type identifier for groups. */
	public static final String TYPE = "TEXT_VALUE_VARIANT";
	/** The ID prefix used for generating new group IDs (e.g., {@code NS}). */
	public static final String ID_PREFIX = "TVV";


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
	public String getIDPrefix(){
		return ID_PREFIX;
	}

	/**
	 * Creates a new group dialog for creating a new group record.
	 *
	 * @param parent the parent frame
	 * @param model  the FLEF model
	 * @return a new {@code TextValueVariantDialog} in create mode
	 */
	@Override
	public TextValueVariantDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return TextValueVariantDialog.createNew(parent, model);
	}

	/**
	 * Creates a new group dialog for editing an existing group record.
	 *
	 * @param parent the parent frame
	 * @param model  the FLEF model
	 * @param record the group record to edit
	 * @return a new {@code TextValueVariantDialog} in edit mode
	 */
	@Override
	public TextValueVariantDialog createEditDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return TextValueVariantDialog.createEdit(parent, model, record);
	}

	/**
	 * Returns a display name for the given text value variant structure.
	 *
	 * @param record the text value variant structure record (phonetic or transcription)
	 * @param model  the FLEF model
	 * @return a human-readable display name
	 */
	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		if(record == null)
			return StringUtils.EMPTY;

		FLEFRecord variant = record.findChild(TAG_PHONETIC);
		if(variant != null){
			final String system = FLEFRecordHelper.getChildValue(variant, TAG_SYSTEM);
			final String value = FLEFRecordHelper.getChildValue(variant, TAG_VALUE);

			final StringBuilder details = new StringBuilder();
			if(StringUtils.isNotEmpty(system))
				details.append(system);
			if(!details.isEmpty())
				return String.format("%s [%s: %s]", value, TAG_PHONETIC, details);
			return String.format("%s [%s]", value, TAG_PHONETIC);
		}
		variant = record.findChild(TAG_TRANSCRIPTION);
		if(variant != null){
			final String system = FLEFRecordHelper.getChildValue(variant, TAG_SYSTEM);
			final String type = FLEFRecordHelper.getChildValue(variant, TAG_TYPE);
			final String value = FLEFRecordHelper.getChildValue(variant, TAG_VALUE);

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
		return record.getId();
	}

}
