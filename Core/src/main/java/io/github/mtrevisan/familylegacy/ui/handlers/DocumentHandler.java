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
package io.github.mtrevisan.familylegacy.ui.handlers;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.DocumentReader;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.DocumentRecordDialog;
import io.github.mtrevisan.familylegacy.ui.helpers.FileHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import java.awt.Window;


/**
 * Handler for DOCUMENT records.
 */
public class DocumentHandler extends AbstractRecordTypeHandler<DocumentRecordDialog>{

	public static final String TYPE = "DOCUMENT";
	public static final String ID_PREFIX = "D";


	private static final class SingletonHelper{
		private static final DocumentHandler INSTANCE = new DocumentHandler();
	}


	public static DocumentHandler getInstance(){
		return SingletonHelper.INSTANCE;
	}


	@Override
	public String getLabel(){
		return I18N.t("confirmation.exist.record.document");
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
		String uri = DocumentReader.extractUri(record);
		if(uri == null){
			// it's a citation, extract URI from true record
			final String documentId = record.getValue();
			final FLEFRecord document = model.getRecordById(documentId);
			uri = DocumentReader.extractUri(document);
		}

		final StringBuilder sb = new StringBuilder();
		if(StringUtils.isNotEmpty(uri))
			sb.append(FileHelper.getFilename(uri));
		final String id = record.getId();
		if(StringUtils.isNotEmpty(id)){
			if(!sb.isEmpty())
				sb.append(StringUtils.SPACE);
			sb.append('[')
				.append(id)
				.append(']');
		}
		return sb.toString();
	}

	@Override
	public DocumentRecordDialog createNewDialog(final Window parent, final FLEFModel model){
		return DocumentRecordDialog.createNew(parent, model);
	}

	@Override
	public DocumentRecordDialog createEditDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		return DocumentRecordDialog.createEdit(parent, model, record);
	}

}
