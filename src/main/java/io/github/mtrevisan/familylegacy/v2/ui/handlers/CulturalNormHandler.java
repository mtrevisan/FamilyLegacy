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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.CulturalNormDialog;

import java.awt.Dialog;


/**
 * Handler for CULTURAL_NORM records.
 */
public class CulturalNormHandler implements RecordTypeHandler<CulturalNormDialog>{

	public static final String TYPE = "CULTURAL_NORM";
	public static final String ID_PREFIX = "CN";


	@Override
	public String getLabel(){
		return "Cultural Norm";
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
		String title = FLEFRecordHelper.getChildValue(record, "TITLE");
		String id = record.getId();
		if(title != null && !title.isEmpty()){
			return title + " (" + id + ")";
		}
		return id;
	}

	@Override
	public CulturalNormDialog createEditDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		return CulturalNormDialog.createEdit(parent, model, record);
	}

	@Override
	public CulturalNormDialog createNewDialog(Dialog parent, FLEFModel model){
		return CulturalNormDialog.createNew(parent, model);
	}

}
