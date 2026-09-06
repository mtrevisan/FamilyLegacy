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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.PlaceRelationshipRecordDialog;

import java.awt.Dialog;


public class PlaceRelationshipHandler extends AbstractRecordTypeHandler<PlaceRelationshipRecordDialog>{

	public static final String TYPE = "PLACE_RELATIONSHIP";
	public static final String ID_PREFIX = "PR";


	private static final String TAG_SUBJECT = "SUBJECT";
	private static final String TAG_TARGET = "TARGET";
	private static final String TAG_TYPE = "TYPE";


	private static final class SingletonHelper{
		private static final PlaceRelationshipHandler INSTANCE = new PlaceRelationshipHandler();

	}


	public static PlaceRelationshipHandler getInstance(){
		return PlaceRelationshipHandler.SingletonHelper.INSTANCE;
	}


	@Override
	public String getLabel(){
		return "Place Relationship";
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
		final FLEFRecord subject = FLEFRecordHelper.extractRecordsFromOneOfReference(record, TAG_SUBJECT, model)
			.getFirst();
		String subjectDisplayText = "--";
		if(subject != null){
			final RecordTypeHandler<?> subjectHandler = HandlerRegistry.getHandler(subject.getTag());
			subjectDisplayText = subjectHandler.getDisplayText(subject, model);
		}

		final FLEFRecord target = FLEFRecordHelper.extractRecordsFromOneOfReference(record, TAG_TARGET, model)
			.getFirst();
		String targetDisplayText = "--";
		if(target != null){
			final RecordTypeHandler<?> targetHandler = HandlerRegistry.getHandler(target.getTag());
			targetDisplayText = targetHandler.getDisplayText(target, model);
		}

		final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);

		final String id = record.getId();

		return subjectDisplayText + " is related to " + targetDisplayText + " as " + type + " [" + id + ']';
	}

	@Override
	public PlaceRelationshipRecordDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return PlaceRelationshipRecordDialog.createNew(parent, model);
	}

	@Override
	public PlaceRelationshipRecordDialog createEditDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return PlaceRelationshipRecordDialog.createEdit(parent, model, record);
	}

}
