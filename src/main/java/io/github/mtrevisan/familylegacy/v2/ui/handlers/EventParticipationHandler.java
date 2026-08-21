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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.EventParticipationRecordDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


public class EventParticipationHandler extends AbstractRecordTypeHandler<EventParticipationRecordDialog>{

	public static final String TYPE = "EVENT_PARTICIPATION";
	public static final String ID_PREFIX = "EP";

	private static final String TAG_PARTICIPANT = "PARTICIPANT";
	private static final String TAG_ROLE = "ROLE";
	private static final String TAG_EVENT = "EVENT";


	private final RecordTypeHandler<?> eventHandler = HandlerRegistry.getHandler(EventHandler.class);


	@Override
	public String getLabel(){
		return "Event Participation";
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
		if(record == null)
			return "--";

		final StringBuilder sb = new StringBuilder();

		String participantText = null;
		final FLEFRecord participant = FLEFRecordHelper.extractRecordsFromOneOfReference(record, TAG_PARTICIPANT, model)
			.getFirst();
		if(participant != null){
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(participant.getTag());
			if(handler != null)
				participantText = handler.getDisplayText(participant, model);
		}
		sb.append(StringUtils.isNotEmpty(participantText)? participantText: "Unknown Participant");

		final String role = FLEFRecordHelper.getChildValue(record, TAG_ROLE);
		if(StringUtils.isNotEmpty(role))
			sb.append(" (as ")
				.append(role)
				.append(')');

		final String eventRef = FLEFRecordHelper.getChildValue(record, TAG_EVENT);
		if(StringUtils.isNotEmpty(eventRef)){
			final FLEFRecord eventRecord = model.getRecordById(eventRef);
			if(eventRecord != null)
				sb.append(" in ")
					.append(eventHandler.getDisplayText(eventRecord, model));
		}

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
	public EventParticipationRecordDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return EventParticipationRecordDialog.createNew(parent, model);
	}

	@Override
	public EventParticipationRecordDialog createEditDialog(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return EventParticipationRecordDialog.createEdit(parent, model, record);
	}

}
