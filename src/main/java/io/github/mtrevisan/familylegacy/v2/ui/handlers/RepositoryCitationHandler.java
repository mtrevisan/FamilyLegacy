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
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.RepositoryCitationDialog;

import java.awt.Dialog;


/**
 * Handler for {@code REPOSITORY_CITATION} entities according to FLEF 0.1.1.
 */
public class RepositoryCitationHandler implements RecordTypeHandler<RepositoryCitationDialog>{

	/** The record type identifier for groups. */
	public static final String TYPE = "REPOSITORY_CITATION";
	public static final String CITED_TYPE = "REPOSITORY";

	private static final String TAG_REPOSITORY = "REPOSITORY";


	static{
		HandlerRegistry.register(new RepositoryHandler());
	}


	private final RecordTypeHandler<?> repositoryHandle = HandlerRegistry.getHandler(RepositoryHandler.TYPE);


	@Override
	public boolean isTopLevelEntity(){
		return false;
	}

	@Override
	public String getLabel(){
		return "Repository Citation";
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
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		if(record == null)
			return "--";

		final String xref = XRefHelper.extractXRef(FLEFRecordHelper.getChildValuesAsString(record, TAG_REPOSITORY));
		final FLEFRecord repository = model.getRecordById(xref);
		return repositoryHandle.getDisplayText(repository, model);
	}

	@Override
	public RepositoryCitationDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return RepositoryCitationDialog.createNew(parent, model);
	}

	@Override
	public RepositoryCitationDialog createEditDialog(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return RepositoryCitationDialog.createEdit(parent, model, record);
	}

}
