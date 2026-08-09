package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogstodo._ResearchLogRecordDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


/**
 * Handler for ResearchLogRecord.
 */
public class ResearchLogHandler implements RecordTypeHandler<_ResearchLogRecordDialog>{

	public static final String TYPE = "research_log";

	@Override
	public String getType(){
		return TYPE;
	}

	@Override
	public String getIdPrefix(){
		return "";
	}

	@Override
	public String getLabel(){
		return "Research Log";
	}

	@Override
	public String getDisplayText(FLEFRecord record, FLEFModel model){
		if(record == null) return "--";
		String action = FLEFRecordHelper.getChildValue(record, "ACTION");
		String date = FLEFRecordHelper.getChildValue(record, "DATE");
		if(StringUtils.isNotEmpty(action)){
			String display = action.length() > 40? action.substring(0, 37) + "...": action;
			if(StringUtils.isNotEmpty(date)){
				display += " (" + date + ")";
			}
			return display;
		}
		return record.getId() != null? record.getId(): "(unnamed)";
	}

	@Override
	public _ResearchLogRecordDialog createNewDialog(Dialog parent, FLEFModel model){
		return _ResearchLogRecordDialog.createNew(parent, model);
	}

	@Override
	public _ResearchLogRecordDialog createEditDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		return _ResearchLogRecordDialog.createEdit(parent, model, record);
	}

}
