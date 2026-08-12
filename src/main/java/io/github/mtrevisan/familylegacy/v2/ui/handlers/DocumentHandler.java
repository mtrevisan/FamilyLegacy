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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.DocumentRecordDialog;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


/**
 * Handler for DOCUMENT records.
 */
public class DocumentHandler implements RecordTypeHandler<DocumentRecordDialog>{

	public static final String TYPE = "DOCUMENT";
	public static final String ID_PREFIX = "D";

	private static final String TAG_FILE = "FILE";


	@Override
	public String getLabel(){
		return "Document";
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
		String uri = FLEFRecordHelper.getChildValue(record, TAG_FILE);
		if(uri == null){
			// it's a citation, extract URI from true record
			final String documentId = record.getValue();
			final FLEFRecord document = model.getRecordById(documentId);
			uri = FLEFRecordHelper.getChildValue(document, TAG_FILE);
		}

		final StringBuilder sb = new StringBuilder();
		if(StringUtils.isNotEmpty(uri))
			sb.append(FilenameUtils.getBaseName(uri));
		final String id = record.getId();
		if(StringUtils.isNotEmpty(id)){
			if(sb.isEmpty())
				sb.append(StringUtils.SPACE);
			sb.append('[')
				.append(id)
				.append(']');
		}
		return sb.toString();
	}

	@Override
	public DocumentRecordDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return DocumentRecordDialog.createNew(parent, model);
	}

	@Override
	public DocumentRecordDialog createEditDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return DocumentRecordDialog.createEdit(parent, model, record);
	}

}
