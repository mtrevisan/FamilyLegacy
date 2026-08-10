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
import io.github.mtrevisan.familylegacy.v2.ui.dialogstodo._ConclusionRecordDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


public class ConclusionHandler implements RecordTypeHandler<_ConclusionRecordDialog>{

	public static final String TYPE = "CONCLUSION";
	public static final String ID_PREFIX = "CC";

	private static final String TAG_VALUE = "VALUE";


	@Override
	public String getLabel(){
		return "Conclusion";
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
		if(record == null) return "--";
		String context = FLEFRecordHelper.getChildValue(record, "CONTEXT");
		String proofStatus = FLEFRecordHelper.getChildValue(record, "PROOF_STATUS");
		if(StringUtils.isNotEmpty(context)){
			String display = context.length() > 40? context.substring(0, 37) + "...": context;
			if(StringUtils.isNotEmpty(proofStatus)){
				display += " [" + proofStatus + "]";
			}
			return display;
		}
		return record.getId() != null? record.getId(): "(unnamed)";
	}

	@Override
	public _ConclusionRecordDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return _ConclusionRecordDialog.createNew(parent, model);
	}

	@Override
	public _ConclusionRecordDialog createEditDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return _ConclusionRecordDialog.createEdit(parent, model, record);
	}

}
