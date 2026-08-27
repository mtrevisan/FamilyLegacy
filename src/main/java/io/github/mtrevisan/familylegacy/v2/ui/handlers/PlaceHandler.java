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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.PlaceRecordDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


/**
 * Handler for PLACE records.
 */
public class PlaceHandler extends AbstractRecordTypeHandler<PlaceRecordDialog>{

	public static final String TYPE = "PLACE";
	public static final String ID_PREFIX = "P";

	private static final String DOT = ".";

	private static final String TAG_NAME = "NAME";
	private static final String TAG_TEXT = "TEXT";
	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_NAME_TEXT_VALUE = TAG_NAME + DOT + TAG_TEXT + DOT + TAG_VALUE;


	@Override
	public String getLabel(){
		return "Place";
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
		final String value = FLEFRecordHelper.getChildValue(record, TAG_NAME_TEXT_VALUE);
		if(StringUtils.isNotEmpty(value)){
			final String id = record.getId();
			return value + " [" + id + "]";
		}
		return "[" + record.getId() + "]";
	}

	@Override
	public PlaceRecordDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return PlaceRecordDialog.createNew(parent, model);
	}

	@Override
	public PlaceRecordDialog createEditDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return PlaceRecordDialog.createEdit(parent, model, record);
	}

}
