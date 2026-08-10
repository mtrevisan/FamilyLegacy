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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.AssociationStructureDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


public class AssociationHandler implements RecordTypeHandler<AssociationStructureDialog>{

	public static final String TYPE = "ASSOCIATION";

	private static final String TAG_TARGET = "TARGET";
	private static final String TAG_NAME = "NAME";


	@Override
	public boolean isTopLevelEntity(){
		return false;
	}

	@Override
	public String getLabel(){
		return "Cause";
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

		final FLEFRecord target = FLEFRecordHelper.findChild(record, TAG_TARGET);
		final String name = FLEFRecordHelper.getChildValue(record, TAG_NAME);

		if(StringUtils.isNotEmpty(name))
			return (StringUtils.isNotEmpty(name)? name: "[VOID]");

		final FLEFRecord child = target.getChildren().getFirst();
		final String handlerType = child.getTag();
		final String xref = child.getValue();
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(handlerType);
		final FLEFRecord targetRecord = model.getRecordById(xref);
		return handler.getDisplayText(targetRecord, model);
	}

	@Override
	public AssociationStructureDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return AssociationStructureDialog.createNew(parent, model);
	}

	@Override
	public AssociationStructureDialog createEditDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return AssociationStructureDialog.createEdit(parent, model, record);
	}

}
