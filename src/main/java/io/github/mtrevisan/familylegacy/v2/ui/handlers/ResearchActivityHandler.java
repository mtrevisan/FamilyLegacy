package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.ResearchActivityRecordDialog;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;


/**
 * Handler for ResearchActivityRecord.
 */
public class ResearchActivityHandler implements RecordTypeHandler<ResearchActivityRecordDialog>{

	public static final String TYPE = "RESEARCH_ACTIVITY";
	public static final String ID_PREFIX = "RA";

	private static final String TAG_ACTION = "ACTION";
	private static final String TAG_ACTIVITY_TYPE = "ACTIVITY_TYPE";
	private static final String TAG_DATE = "DATE";


	@Override
	public String getType(){
		return TYPE;
	}

	@Override
	public String getIdPrefix(){
		return ID_PREFIX;
	}

	@Override
	public String getLabel(){
		return "Research Activity";
	}

	@Override
	public String getDisplayText(FLEFRecord record, FLEFModel model){
		if(record == null) return "--";
		String action = FLEFRecordHelper.getChildValue(record, TAG_ACTION);
		String type = FLEFRecordHelper.getChildValue(record, TAG_ACTIVITY_TYPE);
		String date = FLEFRecordHelper.getChildValue(record, TAG_DATE);
		if(StringUtils.isNotEmpty(action)){
			String display = action.length() > 40? action.substring(0, 37) + "...": action;
			if(StringUtils.isNotEmpty(type)){
				display += " [" + type + "]";
			}
			if(StringUtils.isNotEmpty(date)){
				display += " (" + date + ")";
			}
			return display;
		}
		return record.getId() != null? record.getId(): "(unnamed)";
	}

	@Override
	public ResearchActivityRecordDialog createNewDialog(Dialog parent, FLEFModel model){
		return ResearchActivityRecordDialog.createNew(parent, model);
	}

	@Override
	public ResearchActivityRecordDialog createEditDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		return ResearchActivityRecordDialog.createEdit(parent, model, record);
	}

}
