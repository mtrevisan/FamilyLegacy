package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.NoteRecordDialog;

import java.awt.Dialog;


public class NoteHandler implements RecordTypeHandler<NoteRecordDialog>{

	public static final String TYPE = "NOTE";
	public static final String ID_PREFIX = "N";

	private static final String TAG_VALUE = "VALUE";


	@Override
	public String getLabel(){
		return "Note";
	}

	@Override
	public String getType(){
		return TYPE	;
	}

	@Override
	public String getIDPrefix(){
		return ID_PREFIX;
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		String value = FLEFRecordHelper.getChildValue(record, TAG_VALUE);
		String id = record.getId();
		if(value != null && !value.isEmpty()){
			// Truncate long notes
			if(value.length() > 50){
				value = value.substring(0, 50) + "...";
			}
			return value + " (" + id + ")";
		}
		return id;
	}

	@Override
	public NoteRecordDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return NoteRecordDialog.createNew(parent, model);
	}

	@Override
	public NoteRecordDialog createEditDialog(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return NoteRecordDialog.createEdit(parent, model, record);
	}

}
