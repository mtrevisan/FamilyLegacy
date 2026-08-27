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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.structures.ContactStructureDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


public class ContactHandler extends AbstractRecordTypeHandler<ContactStructureDialog>{

	public static final String TYPE = "CONTACT";

	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_NAME = "NAME";


	@Override
	public boolean isTopLevelEntity(){
		return false;
	}

	@Override
	public String getLabel(){
		return "Contact";
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
		final String value = FLEFRecordHelper.getChildValue(record, TAG_VALUE);
		final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
		final String name = FLEFRecordHelper.getChildValue(record, TAG_NAME);
		final StringBuilder sb = new StringBuilder();
		if(StringUtils.isNotBlank(name))
			sb.append(name)
				.append(':')
				.append(StringUtils.SPACE);
		if(StringUtils.isNotBlank(value))
			sb.append(value);
		if(StringUtils.isNotBlank(type)){
			if(!sb.isEmpty())
				sb.append(StringUtils.SPACE);
			sb.append('(')
				.append(type)
				.append(')');
		}
		return sb.toString();
	}

	@Override
	public ContactStructureDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return ContactStructureDialog.createNew(parent, model);
	}

	@Override
	public ContactStructureDialog createEditDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return ContactStructureDialog.createEdit(parent, model, record);
	}

}
