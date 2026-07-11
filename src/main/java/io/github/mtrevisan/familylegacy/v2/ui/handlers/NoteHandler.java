package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.NoteDialog;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;

import java.awt.Frame;


public class NoteHandler implements RecordTypeHandler<NoteDialog>{

	@Override
	public String getLabel(){
		return "Note";
	}

	@Override
	public String getType(){
		return "NOTE";
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
		return new NoteDialog(parent, model, record);
	}

	@Override
	public NoteDialog createNewDialog(Frame parent, FLEFModel model){
		return new NoteDialog(parent, model);
	}

}
