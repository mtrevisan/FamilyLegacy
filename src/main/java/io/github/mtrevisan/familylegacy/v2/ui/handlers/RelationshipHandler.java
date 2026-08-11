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


/* ONGOING */
public class RelationshipHandler implements RecordTypeHandler<RelationshipRecordDialog>{

	public static final String TYPE = "RELATIONSHIP";
	public static final String ID_PREFIX = "RL";

	private static final String TAG_SUBJECT = "SUBJECT";
	private static final String TAG_OBJECT = "OBJECT";
	private static final String TAG_ROLE = "ROLE";


	static{
		HandlerRegistry.register(new IndividualHandler());
	}


	private final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler(IndividualHandler.TYPE);


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
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final FLEFRecord subjectCitation = FLEFRecordHelper.findChild(record, TAG_SUBJECT)
			.getChildren()
			.getFirst();
		final FLEFRecord subjectRecord = model.getRecordById(subjectCitation.getValue());
		final String subjectTag = subjectRecord
			.getTag();
		final RecordTypeHandler<?> subjectHandler = HandlerRegistry.getHandler(subjectTag);
		final String subjectDisplayText = subjectHandler.getDisplayText(subjectRecord, model);

		final FLEFRecord objectCitation = FLEFRecordHelper.findChild(record, TAG_OBJECT)
			.getChildren()
			.getFirst();
		final FLEFRecord objectRecord = model.getRecordById(objectCitation.getValue());
		final String objectTag = objectRecord
			.getTag();
		final RecordTypeHandler<?> objectHandler = HandlerRegistry.getHandler(objectTag);
		final String objectDisplayText = objectHandler.getDisplayText(objectRecord, model);

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
			.append(" [")
			.append(id)
			.append("]");
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
