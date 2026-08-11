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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.PersonalNameStructureDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;
import java.util.List;


/**
 * Handler for {@code PERSONAL_NAME_STRUCTURE} entities according to FLEF 0.1.1.
 * <p>
 * This handler provides the necessary operations for managing name structures:
 * creation, editing, display name generation, and type identification.
 * <p>
 * Structure:
 * <pre>
 * ???
 * </pre>
 */
public class PersonalNameHandler implements RecordTypeHandler<PersonalNameStructureDialog>{

	/** The record type identifier for groups. */
	public static final String TYPE = "PERSONAL_NAME_STRUCTURE";
	public static final String CITED_TYPE = "PERSONAL_NAME";

	private static final String TAG_PART = "PART";
	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_TYPE = "TYPE";


	@Override
	public boolean isTopLevelEntity(){
		return false;
	}

	@Override
	public String getLabel(){
		return "Personal Name Structure";
	}

	@Override
	public String getType(){
		return TYPE;
	}

	@Override
	public String getCitedType(){
		return (!isTopLevelEntity()? CITED_TYPE: null);
	}

	@Override
	public String getIdPrefix(){
		return null;
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		if(record == null)
			return "--";

		final List<FLEFRecord> parts = FLEFRecordHelper.findChildren(record, TAG_PART);
		final StringBuilder fullName = new StringBuilder();

		for(final FLEFRecord part : parts){
			final String val = FLEFRecordHelper.getChildValue(part, TAG_VALUE);
			if(val != null && !val.isBlank()){
				if(!fullName.isEmpty())
					fullName.append(StringUtils.SPACE);
				fullName.append(val);
			}
		}

		String result = fullName.toString();
		if(result.isBlank())
			return "[" + record.getId() + "]";

		if(result.length() > 50)
			result = result.substring(0, 50) + "...";
		final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
		if(type != null && !type.isBlank())
			result += " (" + type + ")";

		return result;
	}

	@Override
	public PersonalNameStructureDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return PersonalNameStructureDialog.createNew(parent, model);
	}

	@Override
	public PersonalNameStructureDialog createEditDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return PersonalNameStructureDialog.createEdit(parent, model, record);
	}

}
