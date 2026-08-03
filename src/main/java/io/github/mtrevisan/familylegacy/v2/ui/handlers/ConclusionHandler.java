package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.ConclusionDialog;

import java.awt.Dialog;


public class ConclusionHandler implements RecordTypeHandler<ConclusionDialog>{

	public static final String TYPE = "CONCLUSION";
	public static final String ID_PREFIX = "N";

	private static final String TAG_VALUE = "VALUE";


	@Override
	public String getLabel(){
		return "Conclusion";
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
			// Truncate long conclusions
			if(value.length() > 50){
				value = value.substring(0, 50) + "...";
			}
			return value + " (" + id + ")";
		}
		return id;
	}

	@Override
	public ConclusionDialog createEditDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		return ConclusionDialog.createEdit(parent, model, record);
	}

	@Override
	public ConclusionDialog createNewDialog(Dialog parent, FLEFModel model){
		return ConclusionDialog.createNew(parent, model);
	}

}
