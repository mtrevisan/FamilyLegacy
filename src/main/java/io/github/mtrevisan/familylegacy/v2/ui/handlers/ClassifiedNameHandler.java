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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.ClassifiedNameDialog;

import java.awt.Dialog;


/**
 * Handler for {@code CLASSIFIED_NAME_STRUCTURE} entities according to FLEF 0.1.1.
 * <p>
 * This handler provides the necessary operations for managing name structures:
 * creation, editing, display name generation, and type identification.
 * <p>
 * Structure:
 * <pre>
 * struct ClassifiedName {
 *   type?: enum { official, colonial, indigenous } | Text
 *   text: NameStructure
 * }
 * </pre>
 */
public class ClassifiedNameHandler implements RecordTypeHandler<ClassifiedNameDialog>{

	/** The record type identifier for groups. */
	public static final String TYPE = "CLASSIFIED_NAME";

	private static final String TAG_TEXT = "TEXT";
	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_TYPE = "TYPE";


	@Override
	public boolean isTopLevelEntity(){
		return false;
	}

	@Override
	public String getLabel(){
		return "Name Structure";
	}

	@Override
	public String getType(){
		return TYPE;
	}

	@Override
	public String getIdPrefix(){
		return null;
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		if(record == null)
			return "--";

		final FLEFRecord textValue = FLEFRecordHelper.findChild(record, TAG_TEXT);
		if(textValue != null){
			String value = FLEFRecordHelper.getChildValue(textValue, TAG_VALUE);
			if(value != null && !value.isEmpty()){
				// Truncate long names
				if(value.length() > 50)
					value = value.substring(0, 50) + "…";

				final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
				if(type != null && !type.isEmpty())
					value += " (" + type + ")";

				return value;
			}
		}

		return "[" + record.getId() + "]";
	}

	@Override
	public ClassifiedNameDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return ClassifiedNameDialog.createNew(parent, model);
	}

	@Override
	public ClassifiedNameDialog createEditDialog(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return ClassifiedNameDialog.createEdit(parent, model, record);
	}

}
