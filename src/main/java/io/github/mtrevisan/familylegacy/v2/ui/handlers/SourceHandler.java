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

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.SourceDialog;

import java.awt.Dialog;


/**
 * Handler for SOURCE records.
 */
public class SourceHandler implements RecordTypeHandler<SourceDialog>{

	public static final String TYPE = "SOURCE";
	public static final String ID_PREFIX = "S";


	@Override
	public String getLabel(){
		return "Source";
	}

	@Override
	public String getType(){
		return TYPE;
	}

	@Override
	public String getIDPrefix(){
		return ID_PREFIX;
	}

	@Override
	public String getDisplayText(FLEFRecord record){
		String title = FLEFRecordUtils.getChildValue(record, "TITLE.VALUE");
		String id = record.getId();
		if(title != null && !title.isEmpty()){
			return title + " (" + id + ")";
		}
		return id;
	}

	@Override
	public SourceDialog createEditDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		return SourceDialog.createEdit(parent, model, record);
	}

	@Override
	public SourceDialog createNewDialog(Dialog parent, FLEFModel model){
		return SourceDialog.createNew(parent, model);
	}

}
