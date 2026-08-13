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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.IndividualRecordDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;
import java.util.ArrayList;
import java.util.List;


public class IndividualHandler extends AbstractRecordTypeHandler<IndividualRecordDialog>{

	public static final String TYPE = "INDIVIDUAL";
	public static final String ID_PREFIX = "I";

	private static final String TAG_NAME = "NAME";
	private static final String TAG_PART = "PART";
	private static final String TAG_VALUE = "VALUE";


	@Override
	public String getLabel(){
		return "Individual";
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
		String formattedName = null;
		for(final FLEFRecord child : record.getChildren())
			if(TAG_NAME.equals(child.getTag())){
				formattedName = buildNameFromParts(child);
				if(StringUtils.isNotEmpty(formattedName))
					break;
			}

		final String id = record.getId();
		if(StringUtils.isNotEmpty(formattedName))
			return formattedName + (StringUtils.isNotEmpty(id)? " [" + id + "]": StringUtils.EMPTY);

		// Fallback to the record ID
		return "[" + id + "]";
	}

	/**
	 * Builds the display name preserving the sequential order of PART nodes as defined in the record.
	 */
	private String buildNameFromParts(final FLEFRecord nameRecord){
		final List<String> parts = new ArrayList<>();
		for(final FLEFRecord child : nameRecord.getChildren())
			if(TAG_PART.equals(child.getTag())){
				final String value = getTextValueFromPart(child);
				if(StringUtils.isNotEmpty(value))
					parts.add(value);
			}
		return String.join(StringUtils.SPACE, parts);
	}

	/**
	 * Extracts the primary text value from a TEXT_VALUE structure inside a PART node.
	 */
	private String getTextValueFromPart(final FLEFRecord partRecord){
		for(final FLEFRecord textValueChild : partRecord.getChildren())
			if(TAG_VALUE.equals(textValueChild.getTag())){
				final String val = textValueChild.getValue();
				if(StringUtils.isNotBlank(val))
					return val.trim();
			}
		return null;
	}

	@Override
	public IndividualRecordDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return IndividualRecordDialog.createNew(parent, model);
	}

	@Override
	public IndividualRecordDialog createEditDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return IndividualRecordDialog.createEdit(parent, model, record);
	}

}
