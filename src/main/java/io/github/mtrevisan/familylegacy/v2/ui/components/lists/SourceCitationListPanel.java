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
package io.github.mtrevisan.familylegacy.v2.ui.components.lists;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.SourceCitationDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.SourceRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;

import javax.swing.JDialog;
import java.awt.Dialog;
import java.io.Serial;


/* DONE */
public class SourceCitationListPanel extends AbstractCitationListPanel{

	@Serial
	private static final long serialVersionUID = -764509672344287269L;


	private static final String TAG_SOURCE = "SOURCE";


	static{
		HandlerRegistry.register(new SourceCitationHandler());
		HandlerRegistry.register(new SourceHandler());
	}


	public SourceCitationListPanel(final String path, final Dialog parent, final String panelTitle,
			final FLEFModel model){
		super(path, parent, panelTitle, model, TAG_SOURCE, SourceHandler.TYPE);
	}


	@Override
	protected boolean isDialogSaved(final JDialog dialog){
		if(dialog instanceof SourceRecordDialog sourceDialog)
			return sourceDialog.isSaved();
		if(dialog instanceof SourceCitationDialog citationDialog)
			return citationDialog.isSaved();
		return false;
	}

	@Override
	protected FLEFRecord getRecordFromDialog(final JDialog dialog){
		if(dialog instanceof SourceRecordDialog sourceDialog)
			return sourceDialog.getRecord();
		if(dialog instanceof SourceCitationDialog citationDialog)
			return citationDialog.getRecord();
		return null;
	}

	@Override
	protected JDialog createCitationEditDialog(final FLEFRecord citation){
		final RecordTypeHandler<?> sourceCitationHandler = HandlerRegistry.getHandler(SourceCitationHandler.TYPE);
		return sourceCitationHandler.createEditDialog(parent, model, citation);
	}

	@Override
	protected JDialog createTargetEditDialog(final FLEFRecord entity){
		final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
		return sourceHandler.createEditDialog(parent, model, entity);
	}

	public boolean hasData(){
		return !isEmpty();
	}

}
