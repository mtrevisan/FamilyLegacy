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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.RelationshipRecordDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;
import java.util.List;


public class RelationshipHandler extends AbstractRecordTypeHandler<RelationshipRecordDialog>{

	public static final String TYPE = "RELATIONSHIP";
	public static final String ID_PREFIX = "RL";

	private static final String TAG_SUBJECT = "SUBJECT";
	private static final String TAG_OBJECT = "OBJECT";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_ROLE = "ROLE";


	@Override
	public String getLabel(){
		return "Relationship";
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
	public List<FLEFRecord> findReferences(final FLEFModel model, final String recordId,
			final String parentEntityType){
		return model.getRecordsByType(TYPE);
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

		final FLEFRecord object = FLEFRecordHelper.extractRecordsFromOneOfReference(record, TAG_OBJECT, model)
			.getFirst();
		String objectDisplayText = "--";
		if(object != null){
			final RecordTypeHandler<?> objectHandler = HandlerRegistry.getHandler(object.getTag());
			objectDisplayText = objectHandler.getDisplayText(object, model);
		}

		final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
		final String role = FLEFRecordHelper.getChildValue(record, TAG_ROLE);

		final String id = record.getId();

		final StringBuilder sb = new StringBuilder();
		sb.append(subjectDisplayText);
		if(StringUtils.isNotBlank(role))
			sb.append(" is ")
				.append(role)
				.append(" w.r.t. ");
		else
			sb.append(" is related to ");
		sb.append(objectDisplayText)
			.append(" as ")
			.append(type)
			.append(" [")
			.append(id)
			.append(']');
		return sb.toString();
	}

	@Override
	public RelationshipRecordDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return RelationshipRecordDialog.createNew(parent, model);
	}

	@Override
	public RelationshipRecordDialog createEditDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return RelationshipRecordDialog.createEdit(parent, model, record);
	}

}
