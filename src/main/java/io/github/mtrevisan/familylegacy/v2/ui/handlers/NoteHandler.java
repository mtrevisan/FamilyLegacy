package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.NoteDialog;

import java.awt.Frame;


public class NoteHandler implements RecordTypeHandler<NoteDialog>{

	public static final String TYPE = "NOTE";
	public static final String ID_PREFIX = "N";


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
	public String getDisplayName(FLEFRecord record){
		String value = FLEFRecordUtils.getChildValue(record, "VALUE");
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
	public NoteDialog createEditDialog(Frame parent, FLEFModel model, FLEFRecord record){
		return NoteDialog.createEdit(parent, model, record);
	}

	@Override
	public NoteDialog createNewDialog(Frame parent, FLEFModel model){
		return NoteDialog.createNew(parent, model);
	}

}
